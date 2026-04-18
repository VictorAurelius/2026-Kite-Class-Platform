package com.kitehub.email.service;

import com.kitehub.email.client.BrandingClient;
import com.kitehub.email.config.SESConfig;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.dto.TenantBranding;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
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
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final BrandingClient brandingClient;

    @Value("${email.provider:mock}")
    private String emailProvider;

    @Value("${kitehub.email.branding-enabled:true}")
    private boolean brandingEnabled;

    public SESEmailService(
            SESConfig.SESProperties sesProperties,
            @Autowired(required = false) SesClient sesClient,
            @Autowired(required = false) JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Autowired(required = false) BrandingClient brandingClient
    ) {
        this.sesProperties = sesProperties;
        this.sesClient = sesClient;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.brandingClient = brandingClient;
    }

    /**
     * Send email using template.
     *
     * @param request Email request with template
     * @return Email response with message ID
     */
    public EmailResponse sendTemplatedEmail(EmailRequest request) {
        log.info("Sending templated email to: {}, template: {}", request.getTo(), request.getTemplateName());

        // Merge variables with tenant branding (GAP-021 Wave 4)
        Map<String, Object> variables = enrichWithBranding(request);

        // Render template with Thymeleaf
        String htmlBody = renderTemplate(request.getTemplateName(), variables);

        // Send email
        return sendEmail(request.getTo(), request.getSubject(), htmlBody);
    }

    /**
     * Merge request variables with tenant branding. Always injects {@code branding}
     * (either real or default) so templates can safely reference {@code branding.*}.
     */
    private Map<String, Object> enrichWithBranding(EmailRequest request) {
        Map<String, Object> variables = request.getVariables() != null
                ? new HashMap<>(request.getVariables())
                : new HashMap<>();

        TenantBranding branding;
        if (brandingEnabled && brandingClient != null
                && (request.getInstanceId() != null || request.getTenantId() != null)) {
            branding = brandingClient.fetchBranding(request.getInstanceId(), request.getTenantId());
        } else {
            branding = TenantBranding.defaultBranding();
        }

        variables.putIfAbsent("branding", branding);
        return variables;
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

        // Route to correct provider
        if ("smtp".equalsIgnoreCase(emailProvider)) {
            return sendViaSMTP(to, subject, htmlBody);
        }

        if ("mock".equalsIgnoreCase(emailProvider) || sesProperties.isMockMode()) {
            log.info("[MOCK] Email to: {} | Subject: {}", to, subject);
            log.debug("[MOCK] Body: {}", htmlBody);
            return EmailResponse.builder()
                    .messageId("mock-" + UUID.randomUUID())
                    .status("MOCK")
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        // SES sending
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
     * Send email via SMTP (MailHog for local, real SMTP for production).
     */
    private EmailResponse sendViaSMTP(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sesProperties.getFromEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            String messageId = "smtp-" + UUID.randomUUID();
            log.info("[SMTP] Email sent to: {} ({})", to, messageId);

            return EmailResponse.builder()
                    .messageId(messageId)
                    .status("SENT")
                    .sentAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("[SMTP] Failed to send email to {}: {}", to, e.getMessage());
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
