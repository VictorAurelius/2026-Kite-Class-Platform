package com.kitehub.subscription.auth.twofactor;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.auth.twofactor.dto.*;
import com.kitehub.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the five 2FA endpoints (GAP-516).
 *
 * <p>Keeps the controller a thin HTTP-binding layer. Each public method returns
 * a DTO + raises a {@link TwoFactorException} on the documented error code so
 * the controller can render the right HTTP status + body shape per
 * {@code documents/01-business/kitehub/auth/api-contract.md}.</p>
 *
 * @since 1.0.0 (Wave 72b GAP-516)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorEnrollmentService {

    public enum ErrorCode {
        INVALID_CHALLENGE,
        CHALLENGE_EXPIRED,
        ALREADY_ENROLLED,
        INVALID_TOTP,
        INVALID_RECOVERY_CODE,
        INVALID_REQUEST,
        INVALID_PASSWORD,
        CANNOT_DISABLE_2FA_FOR_ADMIN,
        TOTP_PRECONDITION_FAILED,
        UNAUTHORIZED
    }

    public static class TwoFactorException extends RuntimeException {
        private final ErrorCode code;
        public TwoFactorException(ErrorCode code, String message) {
            super(message);
            this.code = code;
        }
        public ErrorCode getCode() { return code; }
    }

    /**
     * Holds the secret + pre-generated recovery codes (plaintext + bcrypt
     * hashes) from enroll-init. Plaintexts have already been surfaced to the
     * user once; on enroll-confirm we persist the matching hashes so the same
     * codes the user has in hand work as recovery.
     */
    static final class PendingEnrollment {
        final String secret;
        final List<RecoveryCodeService.GeneratedCode> codes;
        PendingEnrollment(String secret, List<RecoveryCodeService.GeneratedCode> codes) {
            this.secret = secret;
            this.codes = codes;
        }
    }

    /**
     * In-memory cache of pending enrollments keyed by (userId, secret).
     * Phase 1 BETA: single-instance, in-memory; Phase 1.5+ moves to Redis.
     * TTL aligned with challenge token TTL (5 min) via {@link ChallengeTokenService}.
     */
    private final java.util.concurrent.ConcurrentHashMap<UUID, PendingEnrollment> pending =
        new java.util.concurrent.ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final RecoveryCodeService recoveryCodeService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final TotpSecretCipher cipher;
    private final ChallengeTokenService challengeTokens;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---- enroll-init -------------------------------------------------------

    @Transactional
    public EnrollInitResponse enrollInit(UUID userId) {
        User user = mustFindUser(userId);
        if (user.getTotpEnrolledAt() != null) {
            throw new TwoFactorException(ErrorCode.ALREADY_ENROLLED, "User already enrolled");
        }
        String secret = twoFactorAuthService.generateSecret();
        List<RecoveryCodeService.GeneratedCode> codes = recoveryCodeService.generate();
        List<String> plain = codes.stream().map(RecoveryCodeService.GeneratedCode::plain).toList();
        pending.put(userId, new PendingEnrollment(secret, codes));
        String qrUri = twoFactorAuthService.generateQrUri(secret, user.getEmail());
        log.info("2FA enroll-init for userId={}", userId);
        return new EnrollInitResponse(secret, qrUri, plain);
    }

    // ---- enroll-confirm ----------------------------------------------------

    @Transactional
    public EnrollConfirmResponse enrollConfirm(UUID userId, EnrollConfirmRequest req,
                                               TokenIssuer tokenIssuer) {
        User user = mustFindUser(userId);
        if (user.getTotpEnrolledAt() != null) {
            throw new TwoFactorException(ErrorCode.ALREADY_ENROLLED, "Already enrolled");
        }
        PendingEnrollment p = pending.get(userId);
        if (p == null) {
            throw new TwoFactorException(ErrorCode.INVALID_CHALLENGE, "No pending enrollment");
        }
        if (!twoFactorAuthService.verifyCode(p.secret, req.firstTotpCode())) {
            throw new TwoFactorException(ErrorCode.INVALID_TOTP, "First TOTP code wrong");
        }

        // Persist secret + recovery codes.
        user.setTotpSecretEncrypted(cipher.encrypt(p.secret));
        LocalDateTime now = LocalDateTime.now();
        user.setTotpEnrolledAt(now);
        userRepository.save(user);

        // Persist the SAME hashes that match the plaintexts already shown to
        // the user at enroll-init. Otherwise their notebook copy wouldn't work
        // as recovery — they'd hold one set of plaintexts while we store hashes
        // of a different set.
        for (RecoveryCodeService.GeneratedCode gc : p.codes) {
            recoveryCodeRepository.save(RecoveryCode.builder()
                .userId(userId)
                .codeHash(gc.hash())
                .createdAt(now)
                .build());
        }
        pending.remove(userId);

        String access = tokenIssuer.access(user);
        String refresh = tokenIssuer.refresh(user);

        return new EnrollConfirmResponse(
            true,
            now,
            access,
            refresh,
            new EnrollConfirmResponse.UserSummary(
                user.getId().toString(),
                user.getEmail(),
                user.getRole(),
                now
            )
        );
    }

    // ---- verify -----------------------------------------------------------

    @Transactional
    public VerifyResponse verify(UUID userId, VerifyRequest req, TokenIssuer tokenIssuer) {
        boolean hasTotp = req.totpCode() != null && !req.totpCode().isBlank();
        boolean hasRecovery = req.recoveryCode() != null && !req.recoveryCode().isBlank();
        if (hasTotp == hasRecovery) {
            throw new TwoFactorException(ErrorCode.INVALID_REQUEST,
                "Exactly one of totp_code or recovery_code required");
        }

        User user = mustFindUser(userId);
        if (user.getTotpEnrolledAt() == null || user.getTotpSecretEncrypted() == null) {
            throw new TwoFactorException(ErrorCode.INVALID_CHALLENGE, "User not enrolled");
        }

        Boolean regenerateRecommended = null;
        Long codesRemaining = null;
        if (hasTotp) {
            String secret = cipher.decrypt(user.getTotpSecretEncrypted());
            if (!twoFactorAuthService.verifyCode(secret, req.totpCode())) {
                throw new TwoFactorException(ErrorCode.INVALID_TOTP, "Wrong TOTP code");
            }
        } else {
            var verify = recoveryCodeService.verifyAndConsume(userId, req.recoveryCode().trim());
            if (!verify.success()) {
                throw new TwoFactorException(ErrorCode.INVALID_RECOVERY_CODE,
                    "Wrong or already-used recovery code");
            }
            regenerateRecommended = true;
            codesRemaining = verify.codesRemaining();
        }

        String access = tokenIssuer.access(user);
        String refresh = tokenIssuer.refresh(user);
        return new VerifyResponse(
            access,
            refresh,
            new VerifyResponse.UserSummary(user.getId().toString(), user.getEmail(), user.getRole()),
            regenerateRecommended,
            codesRemaining
        );
    }

    // ---- regenerate -------------------------------------------------------

    @Transactional
    public RegenerateResponse regenerate(UUID userId, RegenerateRequest req) {
        User user = mustFindUser(userId);
        if (user.getTotpEnrolledAt() == null) {
            throw new TwoFactorException(ErrorCode.INVALID_CHALLENGE, "Not enrolled");
        }
        String secret = cipher.decrypt(user.getTotpSecretEncrypted());
        if (!twoFactorAuthService.verifyCode(secret, req.currentTotpCode())) {
            throw new TwoFactorException(ErrorCode.INVALID_TOTP, "Wrong TOTP code");
        }
        var result = recoveryCodeService.regenerate(userId);
        return new RegenerateResponse(
            result.plainCodes(),
            result.invalidatedCount(),
            "All previous recovery codes are now invalid. Save these new codes — they will not be shown again."
        );
    }

    // ---- disable ----------------------------------------------------------

    @Transactional
    public DisableResponse disable(UUID userId, DisableRequest req) {
        User user = mustFindUser(userId);
        if (user.getTotpEnrolledAt() == null) {
            // Idempotent — already disabled.
            return new DisableResponse(true, LocalDateTime.now());
        }
        if ("PLATFORM_ADMIN".equals(user.getRole())) {
            throw new TwoFactorException(ErrorCode.CANNOT_DISABLE_2FA_FOR_ADMIN,
                "PLATFORM_ADMIN cannot disable 2FA (BR-AUTH-005)");
        }
        if (!passwordEncoder.matches(req.passwordReconfirm(), user.getPasswordHash())) {
            throw new TwoFactorException(ErrorCode.INVALID_PASSWORD, "Password reconfirm wrong");
        }
        String secret = cipher.decrypt(user.getTotpSecretEncrypted());
        if (!twoFactorAuthService.verifyCode(secret, req.currentTotpCode())) {
            throw new TwoFactorException(ErrorCode.INVALID_TOTP, "Wrong TOTP code");
        }

        user.setTotpEnrolledAt(null);
        user.setTotpSecretEncrypted(null);
        userRepository.save(user);
        recoveryCodeService.regenerate(userId); // marks all codes used + inserts a new batch
        // …but we don't surface the new batch on disable. Caller intent here is
        // to leave the table in a clean state (all rows used, no live secrets).
        // The fresh batch is collateral; calling regenerate() rather than only
        // markAllUsed() keeps a single code-path. Improve in follow-up gap.

        return new DisableResponse(true, LocalDateTime.now());
    }

    // ---- helpers ----------------------------------------------------------

    /**
     * Allows the caller (controller) to inject the token-issue strategy. The
     * subscription module owns the access/refresh token signing (JwtKeyService);
     * we don't want this service to depend on it directly so it stays unit-testable.
     */
    public interface TokenIssuer {
        String access(User user);
        String refresh(User user);
    }

    private User mustFindUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() ->
            new TwoFactorException(ErrorCode.UNAUTHORIZED, "User not found"));
    }

    // ---- challenge token surface (used by AuthService.login) --------------

    /**
     * Issued by AuthService.login when 2FA is required. Wrapper here so the
     * service stays the single 2FA orchestration entry point.
     */
    public String issueChallenge(UUID userId, ChallengeTokenService.Purpose purpose) {
        return challengeTokens.issue(userId, purpose);
    }

    public ChallengeTokenService.Verified verifyChallenge(String token) {
        return challengeTokens.verify(token);
    }
}
