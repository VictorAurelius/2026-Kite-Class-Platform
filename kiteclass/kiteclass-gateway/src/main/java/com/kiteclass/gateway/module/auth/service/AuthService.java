package com.kiteclass.gateway.module.auth.service;

import com.kiteclass.gateway.module.auth.dto.ForgotPasswordRequest;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.LoginResponse;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.dto.ResetPasswordRequest;
import reactor.core.publisher.Mono;

/**
 * Service interface for authentication operations.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * Authenticate user with email and password.
     *
     * @param request login request
     * @return login response with tokens
     */
    Mono<LoginResponse> login(LoginRequest request);

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token request
     * @return new login response with tokens
     */
    Mono<LoginResponse> refreshToken(RefreshTokenRequest request);

    /**
     * Logout user by invalidating refresh token.
     *
     * @param refreshToken refresh token to invalidate
     * @return Mono of Void
     */
    Mono<Void> logout(String refreshToken);

    /**
     * Send password reset email.
     *
     * @param request forgot password request
     * @return Mono of Void
     */
    Mono<Void> forgotPassword(ForgotPasswordRequest request);

    /**
     * Reset password with token.
     *
     * @param request reset password request
     * @return Mono of Void
     */
    Mono<Void> resetPassword(ResetPasswordRequest request);
}
