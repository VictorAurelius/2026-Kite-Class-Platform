package com.kitehub.email.service;

import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;

/**
 * Provider-agnostic email send contract — the producer-facing surface that the
 * general email channel exposes regardless of which backend (AWS SES, Resend,
 * SMTP/MailHog) is active.
 *
 * <p><strong>Why this interface exists (GAP-788):</strong> {@code NotificationChannel}
 * defines the future preference-aware {@code send(recipient, message, ctx)} surface,
 * but the Phase 1 producers ({@code EmailController}, {@code EmailEventListener})
 * call the richer, template-aware methods {@link #sendTemplatedEmail(EmailRequest)}
 * and {@link #sendEmail(String, String, String)}. Those methods previously lived
 * only on the concrete {@link SESEmailService}, so both producers injected the
 * concrete SES bean — which made {@link ResendEmailService} a dead bean even when
 * {@code email.provider=resend} (production). This interface lifts those producer
 * methods to a shared contract so an {@link EmailProviderRouter} can select the
 * active provider by config (Strategy Pattern per design-patterns.md §1.1 / §2).</p>
 *
 * <p>Implementations: {@link SESEmailService} (smtp / ses / mock branching) and
 * {@link ResendEmailService} (Resend HTTP API). The {@link EmailProviderRouter}
 * is the {@code @Primary} {@link com.kitehub.email.api.NotificationChannel} that
 * delegates to the {@code email.provider}-selected implementation.</p>
 *
 * @since GAP-788 (Wave phase2-beta — production email-provider routing)
 */
public interface EmailSender {

    /**
     * Send a templated email — renders the named template (HTML + optional
     * plain-text sibling) with branding enrichment, then dispatches via the
     * provider backend.
     *
     * @param request email request carrying recipient, subject, template name,
     *                variables, and optional tenant/instance for branding
     * @return send result envelope — never {@code null}
     */
    EmailResponse sendTemplatedEmail(EmailRequest request);

    /**
     * Send a plain HTML email (no template rendering).
     *
     * @param to       recipient email
     * @param subject  email subject
     * @param htmlBody HTML body
     * @return send result envelope — never {@code null}
     */
    EmailResponse sendEmail(String to, String subject, String htmlBody);
}
