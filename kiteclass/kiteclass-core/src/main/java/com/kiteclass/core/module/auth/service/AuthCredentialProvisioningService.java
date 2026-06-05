package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.module.auth.entity.AuthCredential;
import com.kiteclass.core.module.auth.repository.AuthCredentialRepository;
import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@Service
public class AuthCredentialProvisioningService {

    public static final String ROLE_PARENT = "PARENT";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    private final AuthCredentialRepository credentialRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthCredentialProvisioningService(AuthCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    /**
     * Create a login credential for a freshly-provisioned domain entity.
     *
     * <p>Idempotent on email: if a credential already exists for {@code email}
     * (e.g., a parent redeeming a second child's invite, or re-provisioning),
     * the existing credential is kept unchanged — the original password wins.
     * Password is NOT rotated here.
     *
     * @return the credential (existing or newly created)
     */
    public AuthCredential provision(String entityType, Long entityId, String email,
                                    UUID instanceId, String rawPassword) {
        String normalisedEmail = email.trim();
        return credentialRepository.findByEmailIgnoreCase(normalisedEmail)
                .map(existing -> {
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
}
