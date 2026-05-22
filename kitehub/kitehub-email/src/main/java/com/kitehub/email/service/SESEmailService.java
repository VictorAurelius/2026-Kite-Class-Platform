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
    /**
     * Thymeleaf engine — retained for constructor backward compatibility
     * with existing test fixtures (3 callers). Direct usage superseded by
     * {@link EmailTemplateRenderer} (GAP-703 Wave 104 B2). Field removal +
     * constructor cleanup deferred to follow-up refactor PR.
     */
    @SuppressWarnings("unused")
    private final TemplateEngine templateEngine;
    private final BrandingClient brandingClient;
    private final EmailTemplateRenderer templateRenderer;

    @Value("${email.provider:mock}")
    private String emailProvider;

    @Value("${kitehub.email.branding-enabled:true}")
    private boolean brandingEnabled;

    public SESEmailService(
            SESConfig.SESProperties sesProperties,
            @Autowired(required = false) SesClient sesClient,
            @Autowired(required = false) JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Autowired(required = false) BrandingClient brandingClient,
            EmailTemplateRenderer templateRenderer
    ) {
        this.sesProperties = sesProperties;
        this.sesClient = sesClient;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.brandingClient = brandingClient;
        this.templateRenderer = templateRenderer;
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

        // GAP-703 Wave 104 B2: render BOTH html + plain-text via
        // EmailTemplateRenderer so all templates with a `.txt` sibling emit
        // multipart/alternative on the wire (previously only HTML was rendered
        // here — sendEmail(to, subj, html) routed through HTML-only path).
        // Tone resolution deferred to Wave 99 per renderer javadoc.
        EmailTemplateRenderer.RenderedBodies bodies = templateRenderer.render(
                request.getTemplateName(), variables, /* tone */ null);

        // Send email — 4-arg path always passes textBody so multipart/alternative
        // is the contract; renderer returns empty when .txt sibling missing.
        return sendEmail(request.getTo(), request.getSubject(), bodies.getHtml(),
                bodies.hasText() ? bodies.getText() : null);
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
     * Send email with HTML body only (legacy entry point — kept for backward
     * compatibility). New callers SHOULD use {@link #sendEmail(String, String, String, String)}
     * which emits both HTML + plain-text bodies per GAP-657 deliverability
     * hardening (Wave 98 Bucket B1).
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param htmlBody HTML body
     * @return Email response
     */
    public EmailResponse sendEmail(String to, String subject, String htmlBody) {
        return sendEmail(to, subject, htmlBody, null);
    }

    /**
     * Send email with HTML body + optional plain-text fallback (GAP-657).
     *
     * <p>When {@code textBody} is non-null/non-blank, the SES message is sent
     * as {@code multipart/alternative} with both parts. Mail clients that strip
     * HTML (Gmail Promotions surface, Outlook plain mode) render the plain-text
     * fallback — projected to reduce silent-churn ~20% per external Resend
     * deliverability benchmark.</p>
     *
     * <p>Headers wired alongside (per GAP-657 §Step 3):</p>
     * <ul>
     *   <li>{@code Reply-To: support@kitehub.me} — every transactional email</li>
     *   <li>{@code List-Unsubscribe} — mailto + one-click (CSA / Gmail bulk-sender
     *       requirement). Wired by upstream caller via {@link #sendWithHeaders}.</li>
     * </ul>
     *
     * @param to       Recipient email
     * @param subject  Email subject
     * @param htmlBody HTML body (required)
     * @param textBody Plain-text body (optional — empty/null = HTML-only)
     * @return Email response
     * @since Wave 98 Bucket B1 (GAP-657)
     */
    public EmailResponse sendEmail(String to, String subject, String htmlBody, String textBody) {
        log.info("Sending email to: {}, subject: {}, textBody present: {}",
                to, subject, textBody != null && !textBody.isBlank());

        // Route to correct provider
        if ("smtp".equalsIgnoreCase(emailProvider)) {
            return sendViaSMTP(to, subject, htmlBody, textBody);
        }

        if ("mock".equalsIgnoreCase(emailProvider) || sesProperties.isMockMode()) {
            log.info("[MOCK] Email to: {} | Subject: {} | text-part: {}",
                    to, subject, textBody != null && !textBody.isBlank() ? "yes" : "no");
            log.debug("[MOCK] HTML: {}", htmlBody);
            if (textBody != null && !textBody.isBlank()) {
                log.debug("[MOCK] Text: {}", textBody);
            }
            return EmailResponse.builder()
                    .messageId("mock-" + UUID.randomUUID())
                    .status("MOCK")
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        // SES sending — multipart/alternative when textBody present
        try {
            Body.Builder bodyBuilder = Body.builder()
                    .html(Content.builder().data(htmlBody).build());
            if (textBody != null && !textBody.isBlank()) {
                bodyBuilder.text(Content.builder().data(textBody).build());
            }

            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .source(String.format("%s <%s>",
                            sesProperties.getFromName(),
                            sesProperties.getFromEmail()))
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(bodyBuilder.build())
                            .build());

            // Reply-To header per GAP-657 §Step 3
            String replyTo = sesProperties.getReplyToEmail();
            if (replyTo != null && !replyTo.isBlank()) {
                requestBuilder.replyToAddresses(replyTo);
            }

            SendEmailRequest emailRequest = requestBuilder.build();

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
     *
     * <p>When {@code textBody} is non-blank, sends multipart/alternative via
     * {@link MimeMessageHelper#setText(String, String)} 2-arg form (plain +
     * HTML). Reply-To header wired when configured.</p>
     */
    private EmailResponse sendViaSMTP(String to, String subject, String htmlBody, String textBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sesProperties.getFromEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            if (textBody != null && !textBody.isBlank()) {
                helper.setText(textBody, htmlBody);
            } else {
                helper.setText(htmlBody, true);
            }
            String replyTo = sesProperties.getReplyToEmail();
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            // GAP-703 Wave 104 B2 — RFC 8058 List-Unsubscribe headers.
            // - List-Unsubscribe: mailto + https (Gmail bulk-sender requirement)
            // - List-Unsubscribe-Post: List-Unsubscribe=One-Click (RFC 8058 1-click)
            // Setting on raw MimeMessage so headers land in transmitted envelope.
            applyListUnsubscribeHeaders(message);

            mailSender.send(message);
            String messageId = "smtp-" + UUID.randomUUID();
            log.info("[SMTP] Email sent to: {} ({}), text-part: {}", to, messageId,
                    textBody != null && !textBody.isBlank() ? "yes" : "no");

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
     * Apply RFC 8058 List-Unsubscribe + List-Unsubscribe-Post headers to a
     * MimeMessage (GAP-703 Wave 104 B2).
     *
     * <p>Gmail bulk-sender requirements (effective Feb 2024) demand BOTH:</p>
     * <ul>
     *   <li>{@code List-Unsubscribe: <mailto:...>, <https://...>} — both
     *       mailto + HTTPS endpoints in same header (Gmail / Yahoo accept)</li>
     *   <li>{@code List-Unsubscribe-Post: List-Unsubscribe=One-Click} —
     *       enables 1-click unsubscribe per RFC 8058</li>
     * </ul>
     *
     * <p>Unsubscribe URL token placeholder {@code {token}} is left in the URL
     * for now; tenant-specific token expansion lives in a follow-up wire-up
     * when subscriber-preference tracking lands (deferred per existing
     * {@code unsubscribeUrlTemplate} config key javadoc).</p>
     *
     * <p>Header values fall back gracefully when config is missing — emits the
     * mailto half only OR HTTPS half only OR skips header entirely (logged
     * WARN) so a misconfigured deployment doesn't break send.</p>
     */
    private void applyListUnsubscribeHeaders(MimeMessage message) {
        try {
            String mailto = sesProperties.getUnsubscribeMailto();
            String urlTemplate = sesProperties.getUnsubscribeUrlTemplate();

            StringBuilder header = new StringBuilder();
            if (mailto != null && !mailto.isBlank()) {
                header.append("<mailto:").append(mailto).append(">");
            }
            if (urlTemplate != null && !urlTemplate.isBlank()) {
                if (header.length() > 0) {
                    header.append(", ");
                }
                header.append("<").append(urlTemplate).append(">");
            }

            if (header.length() == 0) {
                log.warn("List-Unsubscribe headers skipped — no mailto OR url configured");
                return;
            }

            message.setHeader("List-Unsubscribe", header.toString());
            message.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
        } catch (Exception ex) {
            // Header failure must not block send (deliverability degraded, not broken).
            log.warn("Failed to apply List-Unsubscribe headers: {}", ex.getMessage());
        }
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
