package com.kiteclass.core.common.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Logging-based email service implementation.
 *
 * <p>This implementation logs email content instead of actually sending emails.
 * Useful for development and testing environments where SMTP is not configured.
 *
 * <p>To use real email sending, replace this with SmtpEmailService or
 * ExternalEmailService (SendGrid, AWS SES, etc.) and configure SMTP settings.
 *
 * @author KiteClass Team
 * @since 2.17
 */
@Service
@Slf4j
public class LoggingEmailService implements EmailService {

    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        log.info("📧 [EMAIL] Sending simple email");
        log.info("   To: {}", to);
        log.info("   Subject: {}", subject);
        log.info("   Body: {}", body);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("📧 [EMAIL] Sending HTML email");
        log.info("   To: {}", to);
        log.info("   Subject: {}", subject);
        log.info("   HTML Body: {}", htmlBody);
    }

    @Override
    public void sendTemplateEmail(String to, String subject, String templateName,
                                  Map<String, Object> variables) {
        log.info("📧 [EMAIL] Sending template email");
        log.info("   To: {}", to);
        log.info("   Subject: {}", subject);
        log.info("   Template: {}", templateName);
        log.info("   Variables: {}", variables);
    }

    @Override
    public void sendContactNotification(String recipientEmail, String senderName,
                                        String senderEmail, String subject, String message) {
        log.info("📧 [CONTACT NOTIFICATION] Sending to teacher/admin");
        log.info("   Recipient: {}", recipientEmail);
        log.info("   From: {} <{}>", senderName, senderEmail);
        log.info("   Subject: {}", subject);
        log.info("   Message: {}", message);

        // In production, this would send actual email to teacher/admin
        String emailBody = String.format(
                "You have received a new contact message.\n\n" +
                "From: %s <%s>\n" +
                "Subject: %s\n\n" +
                "Message:\n%s",
                senderName, senderEmail, subject, message
        );

        sendSimpleEmail(recipientEmail, "New Contact Message: " + subject, emailBody);
    }

    @Override
    public void sendLeadConfirmation(String leadEmail, String leadName) {
        log.info("📧 [LEAD CONFIRMATION] Sending to lead");
        log.info("   To: {} <{}>", leadName, leadEmail);

        // In production, this would send actual confirmation email
        String emailBody = String.format(
                "Dear %s,\n\n" +
                "Thank you for your interest in KiteClass!\n\n" +
                "We have received your information and our team will contact you soon.\n\n" +
                "Best regards,\n" +
                "KiteClass Team",
                leadName
        );

        sendSimpleEmail(leadEmail, "Thank you for your interest - KiteClass", emailBody);
    }
}
