package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.*;
import com.kitehub.subscription.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Handles user registration and login.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new instance with owner account.
     *
     * @param request registration request
     * @return registration response with tokens
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with email and password.
     *
     * @param request login request
     * @return login response with tokens
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token request
     * @return new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Verify email address using token from verification email.
     *
     * @param token Verification token
     * @return Login response with tokens (user is now verified)
     */
    @PostMapping("/verify-email")
    public ResponseEntity<LoginResponse> verifyEmail(@RequestParam String token) {
        LoginResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Resend verification email.
     *
     * @param request Contains email address
     * @return Success message
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<java.util.Map<String, Object>> resendVerification(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        authService.resendVerification(email);
        return ResponseEntity.ok(java.util.Map.of("message", "Email xác nhận đã được gửi lại"));
    }

    /**
     * Update user profile.
     */
    @PutMapping("/profile")
    public ResponseEntity<java.util.Map<String, Object>> updateProfile(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");
        String phone = request.get("phone");
        authService.updateProfile(email, name, phone);
        return ResponseEntity.ok(java.util.Map.of("message", "Profile updated"));
    }

    /**
     * Change password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<java.util.Map<String, Object>> changePassword(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        authService.changePassword(email, currentPassword, newPassword);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed"));
    }
}
