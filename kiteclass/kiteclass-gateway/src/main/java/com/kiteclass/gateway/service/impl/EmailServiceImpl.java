package com.kiteclass.gateway.service.impl;

import com.kiteclass.gateway.config.EmailProperties;
import com.kiteclass.gateway.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of email service using JavaMailSender and Thymeleaf.
 *
 * <p>Wraps blocking mail operations in reactive Mono for compatibility with reactive stack.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailProperties emailProperties;

    @Override
    public Mono<Void> sendPasswordResetEmail(String to, String userName, String resetToken) {
        log.info("Sending password reset email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("resetLink", emailProperties.getBaseUrl() + "/reset-password?token=" + resetToken);
        variables.put("expirationMinutes", emailProperties.getResetTokenExpiration() / 60000);

        return sendEmail(to, "Đặt lại mật khẩu KiteClass", "password-reset", variables)
                .doOnSuccess(v -> log.info("Password reset email sent successfully to: {}", to))
                .doOnError(e -> log.error("Failed to send password reset email to: {}", to, e));
    }

    @Override
    public Mono<Void> sendWelcomeEmail(String to, String userName) {
        log.info("Sending welcome email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("loginUrl", emailProperties.getBaseUrl() + "/login");

        return sendEmail(to, "Chào mừng đến với KiteClass", "welcome", variables)
                .doOnSuccess(v -> log.info("Welcome email sent successfully to: {}", to))
                .doOnError(e -> log.error("Failed to send welcome email to: {}", to, e));
    }

    @Override
    public Mono<Void> sendAccountLockedEmail(String to, String userName, long lockDurationMinutes) {
        log.info("Sending account locked email to: {}", to);

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userName);
        variables.put("lockDurationMinutes", lockDurationMinutes);
        variables.put("supportEmail", "support@kiteclass.com");

        return sendEmail(to, "Tài khoản KiteClass của bạn đã bị khóa", "account-locked", variables)
                .doOnSuccess(v -> log.info("Account locked email sent successfully to: {}", to))
                .doOnError(e -> log.error("Failed to send account locked email to: {}", to, e));
    }

    /**
     * Send email using Thymeleaf template.
     *
     * <p>This method wraps the blocking JavaMailSender operation in a Mono
     * and executes it on a bounded elastic scheduler to avoid blocking the reactive pipeline.
     *
     * @param to recipient email
     * @param subject email subject
     * @param templateName Thymeleaf template name (without .html extension)
     * @param variables template variables
     * @return Mono of Void when email is sent
     */
    private Mono<Void> sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        return Mono.fromRunnable(() -> {
            try {
                // Process Thymeleaf template
                Context context = new Context();
                context.setVariables(variables);
                String htmlContent = templateEngine.process("email/" + templateName, context);

                // Create and send email
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(emailProperties.getFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);

            } catch (MessagingException e) {
                throw new RuntimeException("Failed to send email", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // Execute blocking operation on bounded elastic scheduler
        .then();
    }
}
