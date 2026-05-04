package com.kitehub.email.service;

import com.kitehub.email.api.NotificationChannel;
import com.kitehub.email.api.NotificationContext;
import com.kitehub.email.api.NotificationSendResult;
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
 * <p>Strategy Pattern — swap notification provider via config (per
 * design-patterns.md §1.1). This is the EMAIL implementation of
 * {@link NotificationChannel} in Wave 18a Bucket B Phase 1 (GAP-063); future
 * channel adapters (Zalo / SMS / Push from GAP-063b) implement the same
 * interface and slot in via Spring autowiring.</p>
 *
 * <p>Existing producer signatures ({@link #sendEmail}, {@link #sendTemplatedEmail})
 * are preserved verbatim for backward compatibility — Phase 1 does not migrate
 * existing callers. The new {@link #send} method is the {@link NotificationChannel}
 * implementation called by future preference-aware dispatchers (Phase 2).</p>
 *
 * @since 1.0
 */
@Slf4j
@Service
public class SESEmailService implements NotificationChannel {

    /**
     * Channel identifier per BR-NOTIF-001 — matches
     * {@code NotificationChannelType.EMAIL} in kitehub-subscription.
     */
    public static final String CHANNEL_NAME = "EMAIL";

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

    // ----------------------------------------------------------------------
    // NotificationChannel implementation (Wave 18a Bucket B — GAP-063 Phase 1)
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>EMAIL channel implementation. When {@code ctx.templateName} is set,
     * routes to the templated path with branding enrichment; otherwise sends
     * the {@code message} as raw HTML body.</p>
     */
    @Override
    public NotificationSendResult send(String recipient, String message, NotificationContext ctx) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be null or blank");
        }

        // Tolerate null context per interface javadoc.
        NotificationContext context = ctx != null
                ? ctx
                : NotificationContext.builder().build();

        EmailResponse response;
        if (context.getTemplateName() != null && !context.getTemplateName().isBlank()) {
            EmailRequest request = EmailRequest.builder()
                    .to(recipient)
                    .subject(context.getSubject() != null ? context.getSubject() : "")
                    .templateName(context.getTemplateName())
                    .variables(context.getVariables())
                    .instanceId(context.getInstanceId())
                    .tenantId(context.getTenantId())
                    .build();
            response = sendTemplatedEmail(request);
        } else {
            String subject = context.getSubject() != null ? context.getSubject() : "";
            response = sendEmail(recipient, subject, message);
        }

        return toNotificationResult(response);
    }

    @Override
    public String channelName() {
        return CHANNEL_NAME;
    }

    /**
     * Map the legacy {@link EmailResponse} envelope to the channel-agnostic
     * {@link NotificationSendResult}.
     */
    private NotificationSendResult toNotificationResult(EmailResponse response) {
        NotificationSendResult.Status mapped;
        switch (response.getStatus() == null ? "" : response.getStatus()) {
            case "SENT":
                mapped = NotificationSendResult.Status.SENT;
                break;
            case "MOCK":
                mapped = NotificationSendResult.Status.MOCK;
                break;
            case "FAILED":
            default:
                mapped = NotificationSendResult.Status.FAILED;
                break;
        }
        return NotificationSendResult.builder()
                .providerMessageId(response.getMessageId())
                .status(mapped)
                .sentAt(response.getSentAt())
                .errorMessage(response.getErrorMessage())
                .channel(CHANNEL_NAME)
                .build();
    }
}
