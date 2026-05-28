package com.kitehub.email.service;

import com.kitehub.email.api.NotificationChannel;
import com.kitehub.email.api.NotificationContext;
import com.kitehub.email.api.NotificationSendResult;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Provider-selecting email channel — the {@code @Primary}
 * {@link NotificationChannel} (and {@link EmailSender}) that all producers
 * inject. Routes every send through the backend named by the {@code email.provider}
 * property (Strategy Pattern per design-patterns.md §1.1 / §2 — "Multiple
 * implementations swap via config").
 *
 * <h2>Why this exists (GAP-788 — P0 production email-delivery defect)</h2>
 *
 * <p>Production runs {@code EMAIL_PROVIDER=resend} (docker-compose.production.yml,
 * Cloudflare DNS provisions Resend SPF+DKIM), but before this router the send path
 * never reached {@link ResendEmailService}:</p>
 * <ul>
 *   <li>{@link SESEmailService#sendEmail} routed {@code smtp}→SMTP, {@code mock}→log,
 *       <em>else</em>→AWS SES — there was NO {@code resend} branch, so
 *       {@code provider=resend} silently fell through to SES (whose DKIM is not set
 *       up in prod → reject / spam).</li>
 *   <li>{@link ResendEmailService} (conditional on {@code email.provider=resend})
 *       was a DEAD bean: both {@code EmailController} and {@code EmailEventListener}
 *       injected the concrete {@link SESEmailService}, never the interface, so Resend
 *       was never invoked.</li>
 * </ul>
 *
 * <p>This router fixes both halves: it reads {@code email.provider} and delegates to
 * {@link ResendEmailService} when {@code resend}, otherwise to {@link SESEmailService}
 * (which keeps its internal {@code smtp}/{@code ses}/{@code mock} branching). Consumers
 * inject the {@code @Primary} {@link NotificationChannel}/{@link EmailSender} — never
 * the concrete SES bean.</p>
 *
 * <h2>Bean-ambiguity reasoning</h2>
 *
 * <p>{@link SESEmailService} is always a bean (handles smtp/ses/mock); making it
 * conditional would need four mutually-exclusive conditions and break local
 * {@code smtp} wiring, so it stays unconditional. {@link ResendEmailService} stays
 * {@code @ConditionalOnProperty(email.provider=resend)} — present ONLY for that one
 * value — and is resolved here via {@link ObjectProvider} so its absence (provider in
 * {ses, smtp, mock}) is null-safe rather than a startup failure. Three concrete beans
 * implement {@link NotificationChannel} when {@code resend} is active (SES, Resend,
 * this router), so {@code @Primary} on the router gives an unambiguous injection target
 * for the {@link NotificationChannel} interface. The router holds the concrete
 * {@link SESEmailService} + {@code ObjectProvider<ResendEmailService>} (NOT a generic
 * {@link NotificationChannel}/{@link EmailSender}) — so it cannot self-inject and there
 * is no recursion.</p>
 *
 * @since GAP-788 (Wave phase2-beta — production email-provider routing)
 */
@Slf4j
@Service
@Primary
public class EmailProviderRouter implements NotificationChannel, EmailSender {

    private static final String PROVIDER_RESEND = "resend";

    private final SESEmailService sesEmailService;
    private final ObjectProvider<ResendEmailService> resendEmailServiceProvider;

    @Value("${email.provider:mock}")
    private String emailProvider;

    public EmailProviderRouter(
            SESEmailService sesEmailService,
            ObjectProvider<ResendEmailService> resendEmailServiceProvider) {
        this.sesEmailService = sesEmailService;
        this.resendEmailServiceProvider = resendEmailServiceProvider;
    }

    /**
     * Resolve the active {@link EmailSender} backend for the configured provider.
     *
     * <p>When {@code email.provider=resend} the {@link ResendEmailService} bean MUST
     * be present (its {@code @ConditionalOnProperty} matches the same value). If it is
     * somehow absent (misconfiguration), we log a warning and fall back to SES rather
     * than fail the send.</p>
     */
    private EmailSender active() {
        if (PROVIDER_RESEND.equalsIgnoreCase(emailProvider)) {
            ResendEmailService resend = resendEmailServiceProvider.getIfAvailable();
            if (resend != null) {
                return resend;
            }
            log.warn("email.provider=resend but ResendEmailService bean unavailable — "
                    + "falling back to SESEmailService");
        }
        return sesEmailService;
    }

    // ---- EmailSender surface (producer-facing) ----

    @Override
    public EmailResponse sendTemplatedEmail(EmailRequest request) {
        return active().sendTemplatedEmail(request);
    }

    @Override
    public EmailResponse sendEmail(String to, String subject, String htmlBody) {
        return active().sendEmail(to, subject, htmlBody);
    }

    // ---- NotificationChannel surface (future preference-aware dispatcher) ----

    @Override
    public NotificationSendResult send(String recipient, String message, NotificationContext ctx) {
        EmailSender backend = active();
        // Both general channels also implement NotificationChannel; delegate the
        // channel-agnostic surface to the selected backend.
        return ((NotificationChannel) backend).send(recipient, message, ctx);
    }

    @Override
    public String channelName() {
        return SESEmailService.CHANNEL_NAME;
    }
}
