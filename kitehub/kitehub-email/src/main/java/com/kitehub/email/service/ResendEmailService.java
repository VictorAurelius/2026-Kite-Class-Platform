package com.kitehub.email.service;

import com.kitehub.email.api.NotificationChannel;
import com.kitehub.email.api.NotificationContext;
import com.kitehub.email.api.NotificationSendResult;
import com.kitehub.email.client.BrandingClient;
import com.kitehub.email.config.SESConfig;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.dto.TenantBranding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Resend HTTP API email sending service (GAP-657 — Wave 98 Bucket B1 stub).
 *
 * <p>Sister implementation of {@link SESEmailService} for the Resend provider
 * (ADR-025 Stream A). Activated when {@code email.provider=resend}. Wires both
 * {@code html} + {@code text} bodies in the POST payload (multipart/alternative
 * equivalent) plus {@code Reply-To} + {@code List-Unsubscribe} headers per
 * GAP-657 §Step 3.</p>
 *
 * <p><b>Wave 98 status:</b> stub implementation — wires payload shape but
 * activated only when the Resend secret + RestTemplate bean are configured
 * (see {@code application.yml resend.api-key}). Full integration with
 * Resend's official SDK (resend-java) deferred to follow-up gap; HTTP-level
 * call via {@link RestTemplate} sufficient for v1.0.0 deliverability
 * hardening.</p>
 *
 * @since Wave 98 Bucket B1 (GAP-657)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "resend")
public class ResendEmailService implements NotificationChannel, EmailSender {

    /**
     * Channel identifier — matches {@link SESEmailService#CHANNEL_NAME} so
     * upstream routing remains channel-agnostic; provider dispatch happens
     * at the @ConditionalOnProperty boundary.
     */
    public static final String CHANNEL_NAME = "EMAIL";

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final SESConfig.SESProperties sesProperties;
    private final RestTemplate restTemplate;
    private final EmailTemplateRenderer templateRenderer;
    private final BrandingClient brandingClient;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:no-reply@kitehub.me}")
    private String resendFromEmail;

    @Value("${resend.from-name:KiteHub}")
    private String resendFromName;

    @Value("${kitehub.email.branding-enabled:true}")
    private boolean brandingEnabled;

    public ResendEmailService(
            SESConfig.SESProperties sesProperties,
            RestTemplate restTemplate,
            EmailTemplateRenderer templateRenderer,
            @Autowired(required = false) BrandingClient brandingClient) {
        this.sesProperties = sesProperties;
        this.restTemplate = restTemplate;
        this.templateRenderer = templateRenderer;
        this.brandingClient = brandingClient;
    }

    /**
     * Send a templated email via Resend — renders the named template (HTML +
     * optional plain-text sibling) with branding enrichment, then dispatches
     * through the Resend HTTP API. Mirrors {@link SESEmailService#sendTemplatedEmail}
     * so provider routing (GAP-788) keeps templated delivery working when
     * {@code email.provider=resend}.
     *
     * @param request email request with template name + variables
     * @return send result envelope
     */
    @Override
    public EmailResponse sendTemplatedEmail(EmailRequest request) {
        log.info("Sending templated Resend email to: {}, template: {}",
                request.getTo(), request.getTemplateName());

        Map<String, Object> variables = enrichWithBranding(request);
        EmailTemplateRenderer.RenderedBodies bodies = templateRenderer.render(
                request.getTemplateName(), variables, /* tone */ null);

        return sendEmail(request.getTo(), request.getSubject(), bodies.getHtml(),
                bodies.hasText() ? bodies.getText() : null);
    }

    /**
     * Merge request variables with tenant branding. Always injects {@code branding}
     * (real or default) so templates can safely reference {@code branding.*}.
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
     * Send via Resend HTTP API with both HTML + plain-text bodies + headers
     * (GAP-657 §Step 2 + §Step 3).
     *
     * <p>If {@code resend.api-key} is not set OR blank, returns MOCK response
     * — supports local dev without Resend credentials.</p>
     *
     * @param to       Recipient email
     * @param subject  Email subject
     * @param htmlBody HTML body (required)
     * @param textBody Plain-text body (optional)
     * @return EmailResponse envelope
     */
    public EmailResponse sendEmail(String to, String subject, String htmlBody, String textBody) {
        log.info("Sending Resend email to: {}, subject: {}, text-part: {}",
                to, subject, textBody != null && !textBody.isBlank());

        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("[RESEND_MOCK] api-key not configured — returning MOCK response");
            return EmailResponse.builder()
                    .messageId("resend-mock-" + UUID.randomUUID())
                    .status("MOCK")
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", String.format("%s <%s>", resendFromName, resendFromEmail));
        payload.put("to", to);
        payload.put("subject", subject);
        payload.put("html", htmlBody);
        if (textBody != null && !textBody.isBlank()) {
            payload.put("text", textBody);
        }

        // Reply-To per GAP-657 §Step 3
        String replyTo = sesProperties.getReplyToEmail();
        if (replyTo != null && !replyTo.isBlank()) {
            payload.put("reply_to", replyTo);
        }

        // List-Unsubscribe headers (mailto + one-click) per GAP-657 §Step 3.
        // password-reset templates SHOULD pass {@code suppressUnsubscribe=true}
        // to skip — they are essential security mail.
        Map<String, String> headers = new HashMap<>();
        String unsubMailto = sesProperties.getUnsubscribeMailto();
        if (unsubMailto != null && !unsubMailto.isBlank()) {
            headers.put("List-Unsubscribe",
                    String.format("<mailto:%s>, <%s>", unsubMailto,
                            sesProperties.getUnsubscribeUrlTemplate()));
            headers.put("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
        }
        if (!headers.isEmpty()) {
            payload.put("headers", headers);
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(resendApiKey);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, httpHeaders);
            @SuppressWarnings({"unchecked", "rawtypes"})
            ResponseEntity<Map<String, Object>> response =
                    (ResponseEntity<Map<String, Object>>) (ResponseEntity) restTemplate.postForEntity(
                            RESEND_API_URL, entity, Map.class);

            String messageId = response.getBody() != null
                    ? String.valueOf(response.getBody().get("id"))
                    : "resend-" + UUID.randomUUID();

            log.info("Resend email sent. Message ID: {}", messageId);
            return EmailResponse.builder()
                    .messageId(messageId)
                    .status("SENT")
                    .sentAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Resend send failed: {}", e.getMessage(), e);
            return EmailResponse.builder()
                    .messageId(null)
                    .status("FAILED")
                    .sentAt(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Convenience overload without plain-text body (HTML-only) —
     * {@link EmailSender} surface.
     */
    @Override
    public EmailResponse sendEmail(String to, String subject, String htmlBody) {
        return sendEmail(to, subject, htmlBody, null);
    }

    // ---- NotificationChannel surface ----

    @Override
    public NotificationSendResult send(String recipient, String message, NotificationContext ctx) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be null or blank");
        }
        NotificationContext context = ctx != null ? ctx : NotificationContext.builder().build();
        String subject = context.getSubject() != null ? context.getSubject() : "";

        EmailResponse response = sendEmail(recipient, subject, message, null);

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

    @Override
    public String channelName() {
        return CHANNEL_NAME;
    }
}
