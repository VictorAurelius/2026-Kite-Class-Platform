package com.kiteclass.gateway.service;

import reactor.core.publisher.Mono;

/**
 * Service for sending emails.
 *
 * <p>Provides reactive email sending with Thymeleaf templates.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
public interface EmailService {

    /**
     * Send password reset email to user.
     *
     * @param to recipient email address
     * @param userName recipient name
     * @param resetToken password reset token
     * @return Mono of Void when email is sent
     */
    Mono<Void> sendPasswordResetEmail(String to, String userName, String resetToken);

    /**
     * Send welcome email to new user.
     *
     * @param to recipient email address
     * @param userName recipient name
     * @return Mono of Void when email is sent
     */
    Mono<Void> sendWelcomeEmail(String to, String userName);

    /**
     * Send account locked notification email.
     *
     * @param to recipient email address
     * @param userName recipient name
     * @param lockDurationMinutes how long the account is locked for
     * @return Mono of Void when email is sent
     */
    Mono<Void> sendAccountLockedEmail(String to, String userName, long lockDurationMinutes);
}
