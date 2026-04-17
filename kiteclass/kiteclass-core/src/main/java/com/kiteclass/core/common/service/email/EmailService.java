package com.kiteclass.core.common.service.email;

/**
 * Service interface for sending emails.
 *
 * <p>This service provides methods for sending various types of emails:
 * transactional emails, notifications, marketing emails, etc.
 *
 * <p>Implementation can use SMTP, external email services (SendGrid, AWS SES),
 * or mock/log for testing environments.
 *
 * @author KiteClass Team
 * @since 2.17
 */
public interface EmailService {

    /**
     * Sends a simple text email.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param body email body (plain text)
     */
    void sendSimpleEmail(String to, String subject, String body);

    /**
     * Sends an HTML email.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param htmlBody email body (HTML content)
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);

    /**
     * Sends an email using a template.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param templateName template name (without extension)
     * @param variables template variables
     */
    void sendTemplateEmail(String to, String subject, String templateName,
                          java.util.Map<String, Object> variables);

    /**
     * Sends notification email to admin/teacher.
     *
     * <p>Used for BR-MKT-003: Contact message notifications.
     *
     * @param recipientEmail admin/teacher email
     * @param senderName contact sender name
     * @param senderEmail contact sender email
     * @param subject contact subject
     * @param message contact message
     */
    void sendContactNotification(String recipientEmail, String senderName,
                                 String senderEmail, String subject, String message);

    /**
     * Sends confirmation email to lead.
     *
     * <p>Used for BR-MKT-004: Lead creation confirmation.
     *
     * @param leadEmail lead email address
     * @param leadName lead name
     */
    void sendLeadConfirmation(String leadEmail, String leadName);
}
