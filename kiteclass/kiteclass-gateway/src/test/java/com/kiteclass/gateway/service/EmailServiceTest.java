package com.kiteclass.gateway.service;

import com.kiteclass.gateway.config.EmailProperties;
import com.kiteclass.gateway.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;

/**
 * Unit tests for {@link EmailServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        // Create a mock MimeMessage
        mimeMessage = new MimeMessage((Session) null);

        // Setup default email properties
        when(emailProperties.getFrom()).thenReturn("KiteClass <noreply@kiteclass.com>");
        when(emailProperties.getBaseUrl()).thenReturn("http://localhost:3000");
        when(emailProperties.getResetTokenExpiration()).thenReturn(3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void shouldSendPasswordResetEmail() {
        // Arrange
        String to = "user@example.com";
        String userName = "Test User";
        String resetToken = "test-reset-token-123";
        String htmlContent = "<html><body>Reset password email</body></html>";

        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        StepVerifier.create(emailService.sendPasswordResetEmail(to, userName, resetToken))
                .expectComplete()
                .verify();

        // Verify template engine was called with correct parameters
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send welcome email successfully")
    void shouldSendWelcomeEmail() {
        // Arrange
        String to = "newuser@example.com";
        String userName = "New User";
        String htmlContent = "<html><body>Welcome email</body></html>";

        when(templateEngine.process(eq("email/welcome"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        StepVerifier.create(emailService.sendWelcomeEmail(to, userName))
                .expectComplete()
                .verify();

        // Verify template engine was called with correct parameters
        verify(templateEngine).process(eq("email/welcome"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send account locked email successfully")
    void shouldSendAccountLockedEmail() {
        // Arrange
        String to = "locked@example.com";
        String userName = "Locked User";
        long lockDurationMinutes = 30;
        String htmlContent = "<html><body>Account locked email</body></html>";

        when(templateEngine.process(eq("email/account-locked"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        StepVerifier.create(emailService.sendAccountLockedEmail(to, userName, lockDurationMinutes))
                .expectComplete()
                .verify();

        // Verify template engine was called with correct parameters
        verify(templateEngine).process(eq("email/account-locked"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle email sending failure gracefully")
    void shouldHandleEmailSendingFailure() {
        // Arrange
        String to = "fail@example.com";
        String userName = "Fail User";
        String resetToken = "fail-token";

        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn("<html><body>Test</body></html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        StepVerifier.create(emailService.sendPasswordResetEmail(to, userName, resetToken))
                .expectError(RuntimeException.class)
                .verify();

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should use correct email properties")
    void shouldUseCorrectEmailProperties() {
        // Arrange
        String customFrom = "Custom Sender <custom@kiteclass.com>";
        String customBaseUrl = "https://app.kiteclass.com";
        Long customExpiration = 7200000L; // 2 hours

        when(emailProperties.getFrom()).thenReturn(customFrom);
        when(emailProperties.getBaseUrl()).thenReturn(customBaseUrl);
        when(emailProperties.getResetTokenExpiration()).thenReturn(customExpiration);

        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn("<html><body>Test</body></html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        StepVerifier.create(emailService.sendPasswordResetEmail("test@example.com", "Test", "token"))
                .expectComplete()
                .verify();

        // Verify properties were used
        verify(emailProperties, atLeastOnce()).getFrom();
        verify(emailProperties, atLeastOnce()).getBaseUrl();
        verify(emailProperties, atLeastOnce()).getResetTokenExpiration();
    }
}
