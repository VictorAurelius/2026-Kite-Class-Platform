package com.kitehub.subscription.auth.twofactor;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.auth.twofactor.ChallengeTokenService.Purpose;
import com.kitehub.subscription.auth.twofactor.TwoFactorEnrollmentService.ErrorCode;
import com.kitehub.subscription.auth.twofactor.TwoFactorEnrollmentService.TwoFactorException;
import com.kitehub.subscription.auth.twofactor.dto.*;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.service.JwtKeyService;
import com.kitehub.subscription.service.TokenService;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for 2FA enrollment + verification (GAP-516 / GAP-547).
 *
 * <p>Five endpoints per
 * {@code documents/01-business/kitehub/auth-2fa/api-contract.md}:</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/2fa/enroll-init} — generate secret + recovery codes (response shown once)</li>
 *   <li>{@code POST /api/v1/auth/2fa/enroll-confirm} — verify first TOTP code, persist enrollment</li>
 *   <li>{@code POST /api/v1/auth/2fa/verify} — login-time TOTP / recovery code challenge</li>
 *   <li>{@code POST /api/v1/auth/2fa/recovery-codes/regenerate} — invalidate + reissue recovery codes</li>
 *   <li>{@code POST /api/v1/auth/2fa/disable} — non-admin opt-out</li>
 * </ul>
 *
 * <p>Versioning (Wave 79 Bucket A / GAP-547): canonical path
 * {@code /api/v1/auth/2fa/*} with backward-compat alias {@code /api/auth/2fa/*}
 * honored until 2026-06-14 (30-day deprecation window per BR-AUTH-2FA-007).
 * Each {@code @PostMapping} below lists BOTH paths so the controller answers
 * either form with identical request/response shape.</p>
 *
 * <p>Auth model: the first three endpoints accept a {@code Bearer
 * <challenge_token>} issued by {@code POST /api/auth/login} when 2FA is
 * required. {@code regenerate} / {@code disable} accept a regular access token
 * + require recent TOTP possession (re-prove freshness).</p>
 *
 * @since 1.0.0 (Wave 72b GAP-516); v1 path Wave 79 Bucket A GAP-547
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "2FA", description = "TOTP enrollment + recovery code endpoints")
@Slf4j
public class TwoFactorController {

    private final TwoFactorEnrollmentService service;
    private final UserRepository userRepository;
    private final JwtKeyService jwtKeyService;
    private final TokenService tokenService;

    // ---- token issuer (closure over JwtKeyService for unit-test isolation) -

    private final TwoFactorEnrollmentService.TokenIssuer tokenIssuer =
        new TwoFactorEnrollmentService.TokenIssuer() {
            @Override
            public String access(User user) {
                // GAP-1097: delegate to shared TokenService so 2FA-completion access
                // tokens carry tier + tenantId claims (parity with AuthService/TokenService
                // per ADR-039 + GAP-704). Eliminates the 3rd independent token builder that
                // drifted on every new claim. Same JwtKeyService signing key, 24h expiration.
                return tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
            }
            @Override
            public String refresh(User user) {
                return signRefreshToken(user, 7, ChronoUnit.DAYS);
            }
        };

    private String signRefreshToken(User user, long amount, ChronoUnit unit) {
        SecretKey key = jwtKeyService.signingKey();
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(amount, unit)))
            .signWith(key)
            .compact();
    }

    // ---- endpoints ---------------------------------------------------------

    @PostMapping({"/api/v1/auth/2fa/enroll-init", "/api/auth/2fa/enroll-init"})
    public ResponseEntity<?> enrollInit(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        UUID userId = requireChallenge(authHeader, Purpose.TWO_FACTOR_ENROLL);
        try {
            return ResponseEntity.ok(service.enrollInit(userId));
        } catch (TwoFactorException ex) {
            return errorResponse(ex);
        }
    }

    @PostMapping({"/api/v1/auth/2fa/enroll-confirm", "/api/auth/2fa/enroll-confirm"})
    public ResponseEntity<?> enrollConfirm(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @Valid @RequestBody EnrollConfirmRequest req) {
        UUID userId = requireChallenge(authHeader, Purpose.TWO_FACTOR_ENROLL);
        try {
            return ResponseEntity.ok(service.enrollConfirm(userId, req, tokenIssuer));
        } catch (TwoFactorException ex) {
            return errorResponse(ex);
        }
    }

    @PostMapping({"/api/v1/auth/2fa/verify", "/api/auth/2fa/verify"})
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyRequest req) {
        UUID userId;
        try {
            userId = service.verifyChallenge(req.challengeToken()).getUserId();
        } catch (ChallengeTokenService.ChallengeTokenException ex) {
            return challengeError(ex);
        }
        try {
            return ResponseEntity.ok(service.verify(userId, req, tokenIssuer));
        } catch (TwoFactorException ex) {
            return errorResponse(ex);
        }
    }

    @PostMapping({"/api/v1/auth/2fa/recovery-codes/regenerate", "/api/auth/2fa/recovery-codes/regenerate"})
    public ResponseEntity<?> regenerate(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @Valid @RequestBody RegenerateRequest req) {
        UUID userId = requireAccessToken(authHeader);
        try {
            return ResponseEntity.ok(service.regenerate(userId, req));
        } catch (TwoFactorException ex) {
            return errorResponse(ex);
        }
    }

    @PostMapping({"/api/v1/auth/2fa/disable", "/api/auth/2fa/disable"})
    public ResponseEntity<?> disable(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @Valid @RequestBody DisableRequest req) {
        UUID userId = requireAccessToken(authHeader);
        try {
            return ResponseEntity.ok(service.disable(userId, req));
        } catch (TwoFactorException ex) {
            return errorResponse(ex);
        }
    }

    // ---- auth helpers ------------------------------------------------------

    private UUID requireChallenge(String authHeader, Purpose required) {
        String token = stripBearer(authHeader);
        if (token == null) {
            throw new TwoFactorException(ErrorCode.INVALID_CHALLENGE, "Missing challenge token");
        }
        ChallengeTokenService.Verified verified;
        try {
            verified = service.verifyChallenge(token);
        } catch (ChallengeTokenService.ChallengeTokenException ex) {
            // Re-throw as TwoFactorException so the controller's error mapper
            // renders the right HTTP status (401 INVALID / 410 EXPIRED).
            if (ex.getReason() == ChallengeTokenService.FailureReason.EXPIRED) {
                throw new TwoFactorException(ErrorCode.CHALLENGE_EXPIRED, "Challenge expired");
            }
            throw new TwoFactorException(ErrorCode.INVALID_CHALLENGE, "Challenge invalid");
        }
        if (verified.getPurpose() != required) {
            throw new TwoFactorException(ErrorCode.INVALID_CHALLENGE, "Wrong challenge purpose");
        }
        return verified.getUserId();
    }

    private UUID requireAccessToken(String authHeader) {
        String token = stripBearer(authHeader);
        if (token == null) {
            throw new TwoFactorException(ErrorCode.UNAUTHORIZED, "Missing access token");
        }
        try {
            var claims = jwtKeyService.parse(token).getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                throw new TwoFactorException(ErrorCode.UNAUTHORIZED, "Wrong token type");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            if (userRepository.findById(userId).isEmpty()) {
                throw new TwoFactorException(ErrorCode.UNAUTHORIZED, "Unknown user");
            }
            return userId;
        } catch (TwoFactorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TwoFactorException(ErrorCode.UNAUTHORIZED, "Invalid access token");
        }
    }

    private static String stripBearer(String h) {
        if (h == null) return null;
        String trimmed = h.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return null;
    }

    // ---- error mapping -----------------------------------------------------

    @ExceptionHandler(TwoFactorException.class)
    public ResponseEntity<?> handleTwoFactor(TwoFactorException ex) {
        return errorResponse(ex);
    }

    private ResponseEntity<?> errorResponse(TwoFactorException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case INVALID_CHALLENGE, INVALID_TOTP, INVALID_RECOVERY_CODE,
                 INVALID_PASSWORD, UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case CHALLENGE_EXPIRED -> HttpStatus.GONE;
            case ALREADY_ENROLLED -> HttpStatus.CONFLICT;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case CANNOT_DISABLE_2FA_FOR_ADMIN -> HttpStatus.FORBIDDEN;
            case TOTP_PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
        };
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getCode().name());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<?> challengeError(ChallengeTokenService.ChallengeTokenException ex) {
        HttpStatus status = ex.getReason() == ChallengeTokenService.FailureReason.EXPIRED
            ? HttpStatus.GONE : HttpStatus.UNAUTHORIZED;
        String code = ex.getReason() == ChallengeTokenService.FailureReason.EXPIRED
            ? "CHALLENGE_EXPIRED" : "INVALID_CHALLENGE";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }
}
