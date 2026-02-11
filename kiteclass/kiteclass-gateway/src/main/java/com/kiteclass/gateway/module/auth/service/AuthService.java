package com.kiteclass.gateway.module.auth.service;

import com.kiteclass.gateway.module.auth.dto.ForgotPasswordRequest;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.LoginResponse;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.dto.RegisterStudentRequest;
import com.kiteclass.gateway.module.auth.dto.RegisterResponse;
import com.kiteclass.gateway.module.auth.dto.ResetPasswordRequest;
import com.kiteclass.gateway.module.auth.dto.request.RegisterRequest;
import com.kiteclass.gateway.module.auth.dto.response.AuthResponse;
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

    /**
     * Register a new user account (simplified).
     *
     * <p>Basic registration flow for testing and simple use cases:
     * <ol>
     *   <li>Validate password policy</li>
     *   <li>Check email doesn't exist</li>
     *   <li>Hash password with BCrypt</li>
     *   <li>Create User in Gateway</li>
     *   <li>Generate JWT tokens</li>
     * </ol>
     *
     * @param request basic registration request (email, password, name)
     * @return auth response with tokens and user ID
     * @since 1.1.0
     */
    Mono<AuthResponse> register(RegisterRequest request);

    /**
     * Register a new student account.
     *
     * <p>Registration flow (Saga pattern):
     * <ol>
     *   <li>Validate email doesn't exist in Gateway</li>
     *   <li>Create User in Gateway with hashed password</li>
     *   <li>Call Core service to create Student profile</li>
     *   <li>Update User.referenceId with Student.id</li>
     *   <li>Generate JWT tokens</li>
     *   <li>Rollback User if Core call fails</li>
     * </ol>
     *
     * @param request student registration request
     * @param tenantId tenant ID from X-Tenant-Id header
     * @return registration response with tokens
     * @since 1.8.0
     */
    Mono<RegisterResponse> registerStudent(RegisterStudentRequest request, String tenantId);
}
