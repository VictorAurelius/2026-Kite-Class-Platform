package com.kiteclass.core.module.parent.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Message published to the shared {@code email.exchange} / {@code email.send}
 * queue to trigger delivery of a parent-invitation email.
 *
 * <p>Shape matches the generic {@code EmailEvent} consumed by the KiteHub
 * email service (see {@code EmailServiceClient}) so that the same worker can
 * process both platform-side (trial/welcome) and tenant-side (parent-invite)
 * emails. The template {@code parent-invitation} is resolved by the email
 * service at send time.
 *
 * <p>This is a plain {@code record} (no JPA, no Lombok) — Jackson
 * deserialises it via the auto-discovered canonical constructor.
 *
 * @param instanceId   tenant id for audit / rate-limiting (nullable only if
 *                     the email originates outside tenant scope — not the
 *                     case here)
 * @param to           recipient email address
 * @param subject      pre-rendered subject line
 * @param templateName always {@code "parent-invitation"} for this event
 * @param variables    template variables (parentEmail, studentName, schoolName,
 *                     redeemUrl, expiresAt, inviterName)
 * @param emailType    {@code "parent-invitation"} — used by the idempotency
 *                     dedup check
 * @param sentAt       publish timestamp (for observability)
 * @since 2.14.0
 */
public record ParentInvitationEmailEvent(
        UUID instanceId,
        String to,
        String subject,
        String templateName,
        Map<String, Object> variables,
        String emailType,
        Instant sentAt
) {

    /**
     * Jackson entry point — kept explicit so that the constructor argument
     * names are stable regardless of how the compiler renders record
     * accessors in bytecode.
     */
    @JsonCreator
    public ParentInvitationEmailEvent(
            @JsonProperty("instanceId") UUID instanceId,
            @JsonProperty("to") String to,
            @JsonProperty("subject") String subject,
            @JsonProperty("templateName") String templateName,
            @JsonProperty("variables") Map<String, Object> variables,
            @JsonProperty("emailType") String emailType,
            @JsonProperty("sentAt") Instant sentAt) {
        this.instanceId = instanceId;
        this.to = to;
        this.subject = subject;
        this.templateName = templateName;
        this.variables = variables;
        this.emailType = emailType;
        this.sentAt = sentAt;
    }
}
