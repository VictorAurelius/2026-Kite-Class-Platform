package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.auth.dto.LoginRequest;
import com.kiteclass.core.module.auth.dto.LoginResponse;
import com.kiteclass.core.module.auth.entity.AuthCredential;
import com.kiteclass.core.module.auth.repository.AuthCredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KC-native login (Wave auth-1, Option B) for PARENT/TEACHER/STUDENT.
 *
 * <p>Authenticates against {@code auth_credentials} (BCrypt) and mints an HS512 JWT
 * the gateway forwards as identity headers (referenceId → X-User-Reference-Id),
 * unblocking the parent/student portals that consume reference-id authz (GAP-798).
 */
@Slf4j
@Service
public class AuthService {

    /**
     * A real BCrypt hash of a throwaway string. On unknown-email login we still run
     * {@code passwordEncoder.matches} against this so the response time matches the
     * found-email path — flattens the timing side-channel that would otherwise let an
     * attacker enumerate which emails exist (Wave auth-2, GAP-1013f).
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO3Z3oCk0jKqQHcCv3Vp0fwQ0aJ5z3Qm";

    private final AuthCredentialRepository credentialRepository;
    private final AuthTokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthCredentialRepository credentialRepository, AuthTokenService tokenService) {
        this.credentialRepository = credentialRepository;
        this.tokenService = tokenService;
    }

    /**
     * Verify email + password and issue an access token.
     *
     * @throws BusinessException 401 {@code INVALID_CREDENTIALS} on any failure
     *         (unknown email / disabled / wrong password) — uniform message avoids
     *         user-enumeration.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AuthCredential credential = credentialRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElse(null);

        // GAP-1013f: run a dummy BCrypt compare when the email is unknown so the
        // request takes the same time as a found-email wrong-password attempt
        // (constant-time enumeration defense). enabled + password checked together
        // so all three failure modes return the SAME uniform 401.
        boolean ok = credential != null
                && credential.isEnabled()
                && passwordEncoder.matches(request.password(), credential.getPasswordHash());
        if (credential == null) {
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
        }

        if (!ok) {
            // GAP-1013d: mask email in the failure log (PII per logs-format-standard §3.1).
            log.info("Login failed for email={} (uniform 401)", maskEmail(request.email()));
            throw new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }

        String token = tokenService.mintAccessToken(credential);
        log.info("Login OK role={} referenceId={} tenant={}",
                credential.getEntityType(), credential.getEntityId(), credential.getInstanceId());

        return new LoginResponse(
                token,
                "Bearer",
                tokenService.accessTtlSeconds(),
                credential.getEntityType(),
                credential.getEntityId(),
                credential.getInstanceId().toString());
    }

    /**
     * Mask an email for logging: first char + {@code ***@} + domain
     * (per {@code logs-format-standard.md} §3.1). {@code null}/blank → {@code "***"}.
     */
    static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email.charAt(0) + "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
