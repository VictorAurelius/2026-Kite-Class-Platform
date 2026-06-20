package com.kitehub.subscription.auth.otp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kitehub.subscription.auth.otp.OtpService.InvalidPhoneException;
import com.kitehub.subscription.auth.otp.OtpService.OtpRequestResult;
import com.kitehub.subscription.auth.otp.OtpService.OtpVerifyResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

/**
 * Phone-OTP REST surface for mobile signup (GAP-286). Mirrors
 * {@link com.kitehub.subscription.auth.passwordreset.PasswordResetController}
 * (RFC 7807 {@code ProblemDetail} error envelope + nested validated DTOs).
 *
 * <p>Endpoints (public — {@code /api/v1/auth/**} is {@code permitAll} in SecurityConfig):</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/signup/request-otp} — mint + (mock) dispatch an OTP;
 *       200 on success, 429 when rate-limited, 400 on invalid phone.</li>
 *   <li>{@code POST /api/v1/auth/signup/verify-otp} — verify the OTP; 200 with a
 *       short-lived signup token, 400 with a {@code reason} when verification fails.</li>
 * </ul>
 *
 * @since GAP-286 (mobile signup OTP)
 */
@RestController
@RequestMapping("/api/v1/auth/signup")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Mobile signup phone-OTP endpoints")
@Slf4j
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/request-otp")
    @Operation(summary = "Request a phone OTP for mobile signup (MOCK delivery in Phase 1)")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        OtpRequestResult result = otpService.requestOtp(request.phone(), request.channel());
        if (result.rateLimited()) {
            ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Too many OTP requests for this phone");
            body.setProperty("error", "RATE_LIMITED");
            body.setProperty("retryAfterSeconds", result.retryAfterSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(result.retryAfterSeconds()))
                .body(body);
        }
        return ResponseEntity.ok(new RequestOtpResponse(
            result.requestId(), result.channel(), result.expiresInSeconds(), result.mock()));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify a phone OTP and issue a short-lived signup token")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpVerifyResult result = otpService.verifyOtp(request.phone(), request.code());
        if (result.verified()) {
            return ResponseEntity.ok(new VerifyOtpResponse(true, result.signupToken(), null));
        }
        return ResponseEntity.badRequest()
            .body(new VerifyOtpResponse(false, null, result.reason().name()));
    }

    // ── Exception handlers (problem+json error envelope) ──

    @ExceptionHandler(InvalidPhoneException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPhone(InvalidPhoneException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        body.setProperty("error", "OTP_INVALID_PHONE");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        var fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null
            ? "invalid payload"
            : fieldError.getField() + ": " + fieldError.getDefaultMessage();
        String error = (fieldError != null && "phone".equals(fieldError.getField()))
            ? "OTP_INVALID_PHONE" : "OTP_INVALID_PAYLOAD";
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        body.setProperty("error", error);
        return ResponseEntity.badRequest().body(body);
    }

    // ── DTOs (records mirroring passwordreset DTO style + bean-validation) ──

    public record RequestOtpRequest(
        @NotBlank
        @Pattern(regexp = OtpService.PHONE_REGEX, message = "phone must match ^0\\d{9,10}$")
        String phone,
        // Optional; null/blank → defaults to ZALO in the service.
        String channel) {
    }

    public record VerifyOtpRequest(
        @NotBlank
        @Pattern(regexp = OtpService.PHONE_REGEX, message = "phone must match ^0\\d{9,10}$")
        String phone,
        @NotBlank
        String code) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RequestOtpResponse(String requestId, String channel, long expiresInSeconds, boolean mock) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VerifyOtpResponse(boolean verified, String signupToken, String reason) {
    }
}
