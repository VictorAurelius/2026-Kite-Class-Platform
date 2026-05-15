package com.kitehub.subscription.auth.passwordreset;

import com.kitehub.subscription.auth.passwordreset.PasswordResetService.PasswordResetTokenInvalidException;
import com.kitehub.subscription.auth.passwordreset.PasswordResetService.WeakPasswordException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Password-reset REST surface paired with gateway rate-limit shipped Wave 78
 * (PR #1354 — closes GAP-548).
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/auth/password-reset-request} — issue token + email link;
 *       always responds 202 to prevent email enumeration.</li>
 *   <li>{@code POST /api/auth/password-reset-confirm} — validate token + apply
 *       new password.</li>
 * </ul>
 *
 * <p>Gateway rate-limit (email-keyed, 1/sec, burst 2) protects request path;
 * confirm path piggy-backs on JWT auth chain in a future iteration once we add
 * step-up auth — Phase 1 BETA confirm path is gated only by token entropy
 * (256-bit) + TTL.</p>
 *
 * @since Wave 79 — GAP-548
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Password reset endpoints")
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/password-reset-request")
    @Operation(summary = "Request password-reset link (always 202 to prevent enumeration)")
    public ResponseEntity<Map<String, Object>> request(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.getEmail());
        // Constant response shape regardless of whether email matched.
        return ResponseEntity.accepted().body(Map.of(
            "message", "If the email exists, a reset link has been sent."
        ));
    }

    @PostMapping("/password-reset-confirm")
    @Operation(summary = "Confirm reset token and apply new password")
    public ResponseEntity<Map<String, Object>> confirm(@Valid @RequestBody PasswordResetConfirm request) {
        passwordResetService.confirm(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset."));
    }

    // ── Exception handlers (problem+json error envelope) ──

    @ExceptionHandler(PasswordResetTokenInvalidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidToken(PasswordResetTokenInvalidException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        body.setProperty("error", "PASSWORD_RESET_TOKEN_INVALID");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ProblemDetail> handleWeakPassword(WeakPasswordException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        body.setProperty("error", "PASSWORD_RESET_WEAK_PASSWORD");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse("invalid payload");
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        body.setProperty("error", "PASSWORD_RESET_INVALID_PAYLOAD");
        return ResponseEntity.badRequest().body(body);
    }

    // ── DTOs ──

    public static class PasswordResetRequest {
        @NotBlank
        @Email
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class PasswordResetConfirm {
        @NotBlank
        private String token;

        @NotBlank
        private String newPassword;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
