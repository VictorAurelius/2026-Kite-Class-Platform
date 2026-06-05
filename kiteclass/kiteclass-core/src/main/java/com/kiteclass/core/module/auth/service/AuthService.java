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
                .filter(AuthCredential::isEnabled)
                .filter(c -> passwordEncoder.matches(request.password(), c.getPasswordHash()))
                .orElseThrow(() -> {
                    log.info("Login failed for email={} (uniform 401)", request.email());
                    return new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
                });

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
}
