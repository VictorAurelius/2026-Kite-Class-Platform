package com.kiteclass.gateway.module.auth.controller;

import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.module.auth.dto.ForgotPasswordRequest;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.LoginResponse;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.dto.ResetPasswordRequest;
import com.kiteclass.gateway.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for authentication operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/auth/login - User login</li>
 *   <li>POST /api/v1/auth/refresh - Refresh access token</li>
 *   <li>POST /api/v1/auth/logout - User logout</li>
 *   <li>POST /api/v1/auth/forgot-password - Request password reset</li>
 *   <li>POST /api/v1/auth/reset-password - Reset password</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    /**
     * Login with email and password.
     *
     * @param request login request
     * @return login response with JWT tokens
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token request
     * @return new login response with JWT tokens
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        return authService.refreshToken(request)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    /**
     * Logout by invalidating refresh token.
     *
     * @param request refresh token request
     * @return no content
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token")
    public Mono<ResponseEntity<Void>> logout(
            @RequestBody RefreshTokenRequest request) {

        return authService.logout(request.refreshToken())
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    /**
     * Request password reset email.
     *
     * @param request forgot password request
     * @return success message
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public Mono<ResponseEntity<ApiResponse<String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        return authService.forgotPassword(request)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success(null, "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu"))));
    }

    /**
     * Reset password with token.
     *
     * @param request reset password request
     * @return success message
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public Mono<ResponseEntity<ApiResponse<String>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        return authService.resetPassword(request)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success(null, "Đặt lại mật khẩu thành công"))));
    }
}
