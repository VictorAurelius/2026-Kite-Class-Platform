package com.kitehub.subscription.auth.passwordreset;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Password-reset orchestration (GAP-548 / Wave 79 Bucket C).
 *
 * <p>Two-step flow paired with gateway rate-limit shipped Wave 78 (PR #1354):</p>
 * <ol>
 *   <li>{@code request(email)} — issue opaque URL-safe token, persist on User row,
 *       send link via {@link EmailSenderService}. Returns silently regardless of
 *       email existence to prevent enumeration.</li>
 *   <li>{@code confirm(token, newPassword)} — validate token + TTL, single-use
 *       update of password hash, clear token columns in same transaction.</li>
 * </ol>
 *
 * <p>Token TTL configured via {@code kitehub.auth.password-reset.token-ttl-minutes}
 * (default 60). Token entropy: 256-bit random URL-safe base64 (43 chars).</p>
 *
 * <p>Schema source-of-truth: {@code documents/01-business/kitehub/password-reset/}
 * (to be filed alongside follow-up gap — Bucket C ships the BE controller; 3-layer
 * business docs are GAP-548 Phase 1.1 follow-up per gap §"Proposed Fix").</p>
 *
 * @since Wave 79 — GAP-548
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;          // 256-bit entropy
    private static final int MIN_PASSWORD_LENGTH = 12;  // matches pre-launch-auth-hardening-checklist §2.3

    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom rng = new SecureRandom();

    @Value("${kitehub.email-verification.base-url:http://localhost:3001}")
    private String resetBaseUrl;

    @Value("${kitehub.auth.password-reset.token-ttl-minutes:60}")
    private long tokenTtlMinutes;

    /**
     * Issue a password-reset token for the given email if a user exists.
     *
     * <p>Returns silently whether or not a user matched — caller (controller)
     * always responds 202 to mitigate email enumeration.</p>
     */
    @Transactional
    public void request(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            log.info("Password-reset requested for non-existent email — silent no-op");
            return;
        }
        User user = userOpt.get();
        String token = generateToken();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpires(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        userRepository.save(user);

        String resetUrl = resetBaseUrl + "/reset-password?token=" + token;
        log.info("[EMAIL] Password-reset link for {}: <redacted>", user.getEmail());
        emailSenderService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    /**
     * Confirm a previously-issued token + apply new password.
     *
     * @throws PasswordResetTokenInvalidException token unknown / expired / already used
     * @throws WeakPasswordException new password fails complexity policy
     */
    @Transactional
    public void confirm(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new PasswordResetTokenInvalidException("token missing");
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        User user = userRepository.findByPasswordResetToken(token)
            .orElseThrow(() -> new PasswordResetTokenInvalidException("token invalid"));

        LocalDateTime expires = user.getPasswordResetTokenExpires();
        if (expires == null || expires.isBefore(LocalDateTime.now())) {
            // Clear stale token to keep table tidy even though we reject the request.
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpires(null);
            userRepository.save(user);
            throw new PasswordResetTokenInvalidException("token expired");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Single-use: clear token columns alongside password update.
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpires(null);
        // Also unlock if locked (admin-friendly recovery).
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("Password reset confirmed for: {}", user.getEmail());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        rng.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Token unknown, expired, or already consumed. Maps to HTTP 400. */
    public static class PasswordResetTokenInvalidException extends RuntimeException {
        public PasswordResetTokenInvalidException(String message) {
            super(message);
        }
    }

    /** New password fails complexity policy. Maps to HTTP 400. */
    public static class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String message) {
            super(message);
        }
    }
}
