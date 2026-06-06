package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.auth.entity.AuthCredential;
import com.kiteclass.core.module.auth.repository.AuthCredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Provisions {@link AuthCredential} rows for tenant-scoped roles (Wave auth-1,
 * Option B). Called from the flows that establish a domain entity + its login:
 * parent-invitation redeem (PARENT), teacher create (TEACHER), student provision
 * (STUDENT — later).
 *
 * <p>Runs inside the caller's transaction (provisioning is atomic with the domain
 * row — a parent must not exist without a credential, and vice versa). NOT an
 * audit side-effect, so default propagation (REQUIRED) is correct.
 *
 * <p><b>1 email = 1 tenant (Phase 1 BETA limitation — GAP-1011, Option A):</b>
 * {@code auth_credentials.email} is GLOBALLY unique (V89 {@code uk_auth_credentials_email}).
 * Login lookup is pre-auth (no tenant context) so the row must be reachable by
 * email alone. Consequence: the same email address cannot be a credential in two
 * tenants. Provisioning an email that already belongs to a DIFFERENT instance is
 * REJECTED (409) rather than silently returning the wrong-tenant credential — that
 * would mint a token with the wrong {@code tenantId}. A future multi-tenant email
 * design would switch the lookup to (email + tenant-slug) with a composite unique.
 */
@Slf4j
@Service
public class AuthCredentialProvisioningService {

    public static final String ROLE_PARENT = "PARENT";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    /** Email already owned by a credential in a different tenant (GAP-1011). */
    public static final String ERR_EMAIL_CROSS_TENANT = "AUTH_EMAIL_CROSS_TENANT";
    /** Email already owned by a different domain entity (GAP-1013a). */
    public static final String ERR_CREDENTIAL_ENTITY_MISMATCH = "AUTH_CREDENTIAL_ENTITY_MISMATCH";

    private final AuthCredentialRepository credentialRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthCredentialProvisioningService(AuthCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    /**
     * Create a login credential for a freshly-provisioned domain entity.
     *
     * <p>Idempotent on email within the SAME tenant: if a credential already exists
     * for {@code email} (e.g., a parent redeeming a second child's invite, or
     * re-provisioning), the existing credential is kept unchanged — the original
     * password wins. Password is NOT rotated here.
     *
     * @return the credential (existing or newly created)
     * @throws BusinessException 409 {@code AUTH_EMAIL_CROSS_TENANT} if the email
     *         already belongs to a credential in a different tenant (GAP-1011)
     */
    public AuthCredential provision(String entityType, Long entityId, String email,
                                    UUID instanceId, String rawPassword) {
        String normalisedEmail = email.trim();
        return credentialRepository.findByEmailIgnoreCase(normalisedEmail)
                .map(existing -> {
                    rejectCrossTenant(existing, instanceId, normalisedEmail);
                    log.info("Credential already exists for {} {} (email={}) — keeping existing",
                            entityType, entityId, normalisedEmail);
                    return existing;
                })
                .orElseGet(() -> {
                    AuthCredential credential = AuthCredential.builder()
                            .userUuid(UUID.randomUUID())
                            .entityType(entityType)
                            .entityId(entityId)
                            .email(normalisedEmail)
                            .passwordHash(passwordEncoder.encode(rawPassword))
                            .instanceId(instanceId)
                            .enabled(true)
                            .createdAt(Instant.now())
                            .build();
                    AuthCredential saved = credentialRepository.save(credential);
                    log.info("Provisioned credential id={} for {} entityId={} tenant={}",
                            saved.getId(), entityType, entityId, instanceId);
                    return saved;
                });
    }

    public AuthCredential provisionParent(Long parentId, String email, UUID instanceId, String rawPassword) {
        return provision(ROLE_PARENT, parentId, email, instanceId, rawPassword);
    }

    /**
     * Set or reset the login password for a domain entity (admin action).
     *
     * <p>Unlike {@link #provision} (idempotent first-time, used by self-redeem),
     * this UPSERTS — an existing credential has its password rotated. Used for
     * admin-driven teacher/student credential management (Hướng B).
     *
     * @throws BusinessException 409 {@code AUTH_EMAIL_CROSS_TENANT} if the email
     *         belongs to a credential in a different tenant (GAP-1011);
     *         409 {@code AUTH_CREDENTIAL_ENTITY_MISMATCH} if the email belongs to a
     *         credential for a different (entityType, entityId) — prevents an admin
     *         silently rotating, e.g., a PARENT credential when setting a TEACHER
     *         password for the same email (GAP-1013a, no cross-entity rotation).
     */
    public AuthCredential setPassword(String entityType, Long entityId, String email,
                                      UUID instanceId, String rawPassword) {
        String normalisedEmail = email.trim();
        AuthCredential credential = credentialRepository.findByEmailIgnoreCase(normalisedEmail)
                .map(existing -> {
                    rejectCrossTenant(existing, instanceId, normalisedEmail);
                    if (!existing.getEntityType().equals(entityType)
                            || !existing.getEntityId().equals(entityId)) {
                        log.warn("Credential entity mismatch for email={}: existing {}#{} vs requested {}#{}",
                                normalisedEmail, existing.getEntityType(), existing.getEntityId(),
                                entityType, entityId);
                        throw new BusinessException(ERR_CREDENTIAL_ENTITY_MISMATCH, HttpStatus.CONFLICT);
                    }
                    return existing;
                })
                .orElseGet(() -> AuthCredential.builder()
                        .userUuid(UUID.randomUUID())
                        .entityType(entityType)
                        .entityId(entityId)
                        .email(normalisedEmail)
                        .instanceId(instanceId)
                        .enabled(true)
                        .createdAt(Instant.now())
                        .build());
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setUpdatedAt(Instant.now());
        AuthCredential saved = credentialRepository.save(credential);
        log.info("Set password for {} entityId={} tenant={} (credId={})",
                entityType, entityId, instanceId, saved.getId());
        return saved;
    }

    /**
     * Disable (soft-revoke) the login credential owned by a domain entity when that
     * entity is deactivated / soft-deleted (Wave auth-2, GAP-1013b). Idempotent +
     * no-op when no credential exists (e.g., admin never provisioned a login).
     *
     * @param entityType PARENT / TEACHER / STUDENT
     * @param entityId   the kiteclass domain row id whose login should be revoked
     */
    public void disableCredential(String entityType, Long entityId) {
        credentialRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .filter(AuthCredential::isEnabled)
                .ifPresent(credential -> {
                    credential.setEnabled(false);
                    credential.setUpdatedAt(Instant.now());
                    credentialRepository.save(credential);
                    log.info("Disabled login credential id={} for {} entityId={} (entity deactivated)",
                            credential.getId(), entityType, entityId);
                });
    }

    private void rejectCrossTenant(AuthCredential existing, UUID instanceId, String email) {
        if (!existing.getInstanceId().equals(instanceId)) {
            log.warn("Cross-tenant credential collision for email={}: existing tenant={} vs requested tenant={}",
                    email, existing.getInstanceId(), instanceId);
            throw new BusinessException(ERR_EMAIL_CROSS_TENANT, HttpStatus.CONFLICT);
        }
    }
}
