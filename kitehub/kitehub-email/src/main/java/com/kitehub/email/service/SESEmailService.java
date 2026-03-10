package com.kitehub.email.service;

import com.kitehub.email.config.SESConfig;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AWS SES email sending service.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class SESEmailService {

    private final SESConfig.SESProperties sesProperties;
    private final SesClient sesClient;
    private final TemplateEngine templateEngine;

    public SESEmailService(
            SESConfig.SESProperties sesProperties,
            @Autowired(required = false) SesClient sesClient,
            TemplateEngine templateEngine
    ) {
        this.sesProperties = sesProperties;
        this.sesClient = sesClient;
        this.templateEngine = templateEngine;
    }

    /**
     * Send email using template.
     *
     * @param request Email request with template
     * @return Email response with message ID
     */
    public EmailResponse sendTemplatedEmail(EmailRequest request) {
        log.info("Sending templated email to: {}, template: {}", request.getTo(), request.getTemplateName());

        // Render template with Thymeleaf
        String htmlBody = renderTemplate(request.getTemplateName(), request.getVariables());

        // Send email
        return sendEmail(request.getTo(), request.getSubject(), htmlBody);
    }

    /**
     * Send email with plain HTML body.
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param htmlBody HTML body
     * @return Email response
     */
    public EmailResponse sendEmail(String to, String subject, String htmlBody) {
        log.info("Sending email to: {}, subject: {}", to, subject);

        // Mock mode - don't actually send email
        if (sesProperties.isMockMode()) {
            log.info("[MOCK] Email would be sent to: {}", to);
            return EmailResponse.builder()
                    .messageId("mock-" + UUID.randomUUID())
                    .status("MOCK")
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        // Real SES sending
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(String.format("%s <%s>",
                            sesProperties.getFromName(),
                            sesProperties.getFromEmail()))
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);

            log.info("Email sent successfully. Message ID: {}", response.messageId());

            return EmailResponse.builder()
                    .messageId(response.messageId())
                    .status("SENT")
                    .sentAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);

            return EmailResponse.builder()
                    .messageId(null)
                    .status("FAILED")
                    .sentAt(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Render Thymeleaf template to HTML.
     */
    private String renderTemplate(String templateName, java.util.Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }

        return templateEngine.process("emails/" + templateName, context);
    }
}
