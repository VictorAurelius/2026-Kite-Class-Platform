package com.kitehub.email.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.email.config.EmailQueueConsumerConfig;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.service.SESEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Consumes transactional {@code EmailEvent} messages from the {@code email.send}
 * queue and dispatches them through {@link SESEmailService}.
 *
 * <p><strong>Why (GAP-787 / GAP-702):</strong> kitehub-subscription's
 * {@code EmailServiceClient.publishToQueue(...)} publishes every transactional email
 * (welcome, trial, beta-invite, staff-invite, DSAR, etc.) to {@code email.exchange}
 * with routing key {@code email.send} when {@code kitehub.email.use-queue=true} (prod
 * default). Until now NO kitehub-email consumer existed for that queue, so in queue mode
 * every transactional email was silently dropped. The Wave meta-6 Bucket A walk surfaced
 * this for the staff-invite flow (Bug #14); Wave 103 verify surfaced the same for
 * beta-invite. This listener restores delivery for the entire transactional pipeline.</p>
 *
 * <p><strong>Wire-format handling:</strong> two producer paths publish to
 * {@code email.send}, each with a slightly different on-the-wire shape (both carry the
 * same logical {@code EmailEvent}):</p>
 * <ol>
 *   <li><strong>Fast-path</strong> ({@code EmailServiceClient.publishToQueue}) —
 *       {@code rabbitTemplate.convertAndSend(exchange, routingKey, emailEvent)} where the
 *       arg is an {@code EmailEvent} object → JSON object on the wire.</li>
 *   <li><strong>Outbox dispatcher</strong> ({@code SubscriptionOutboxDispatcher}) —
 *       {@code convertAndSend(exchange, topic, payloadString)} where the arg is the
 *       already-serialized {@code EmailEvent} JSON String → a JSON string literal on the
 *       wire (double-encoded).</li>
 * </ol>
 * The listener consumes the raw body as {@code String} and normalizes both shapes via
 * {@link ObjectMapper}: a leading {@code "} signals a JSON-string-wrapping-JSON, which is
 * unwrapped once before parsing the inner object.
 *
 * <p><strong>Idempotency / reliability:</strong> producer-side dedup
 * ({@code EmailServiceClient.alreadySentToday}) + outbox {@code dispatchedAt} guard the
 * at-most-once intent; the queue + DLQ ({@link EmailQueueConsumerConfig}) provide
 * at-least-once retry. Render/send failures throw so the listener container routes the
 * message to the DLQ after retry exhaustion (poison messages don't infinite-loop, per
 * GAP-607 producer-side retry advice).</p>
 *
 * <p>Enabled by default; disabled in tests / RabbitMQ-less environments via
 * {@code kitehub.email.use-queue=false}.</p>
 *
 * @since Wave phase2-beta (GAP-787)
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "kitehub.email.use-queue",
        havingValue = "true",
        matchIfMissing = true)
public class EmailEventListener {

    private final SESEmailService sesEmailService;
    private final ObjectMapper objectMapper;

    public EmailEventListener(SESEmailService sesEmailService, ObjectMapper objectMapper) {
        this.sesEmailService = sesEmailService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle one {@code EmailEvent} message: parse → map to {@link EmailRequest} →
     * render + send the templated email.
     *
     * <p>Accepts the body as {@code String} so both producer wire-shapes (raw JSON object
     * vs JSON-string-wrapping-JSON) deserialize through one code path. A render/send
     * failure is rethrown so the container's DLQ-on-reject semantics apply.</p>
     *
     * @param rawBody the message body (JSON object OR JSON string literal)
     */
    @RabbitListener(queues = EmailQueueConsumerConfig.EMAIL_QUEUE)
    public void onEmailEvent(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            log.warn("email.send received empty body — skipping");
            return;
        }

        EmailEventPayload event;
        try {
            event = parse(rawBody);
        } catch (Exception ex) {
            // Malformed payload is not retryable — log + drop (return) so it does NOT
            // poison the queue. Reject-and-requeue would loop forever on bad JSON.
            log.error("email.send payload unparseable — dropping. raw='{}' err={}",
                    truncate(rawBody), ex.getMessage());
            return;
        }

        if (event.to == null || event.to.isBlank()) {
            log.warn("email.send event missing recipient — skipping (templateName={})",
                    event.templateName);
            return;
        }
        if (event.templateName == null || event.templateName.isBlank()) {
            log.warn("email.send event missing templateName — skipping (to={})", event.to);
            return;
        }

        log.info("Dispatching queued email: type={} template={} to={}",
                event.emailType, event.templateName, event.to);

        EmailRequest request = EmailRequest.builder()
                .to(event.to)
                .subject(event.subject)
                .templateName(event.templateName)
                .variables(event.variables == null ? new HashMap<>() : event.variables)
                .build();

        // Throw on failure → container routes to DLQ after retry exhaustion.
        sesEmailService.sendTemplatedEmail(request);
        log.info("Queued email dispatched OK: type={} to={}", event.emailType, event.to);
    }

    /**
     * Normalize both producer wire-shapes into an {@link EmailEventPayload}.
     *
     * <p>If the body starts with a {@code "} it is a JSON string literal wrapping the
     * actual JSON (outbox dispatcher path) — read it as a String first, then parse the
     * inner object. Otherwise parse the body directly as the event object (fast-path).</p>
     */
    private EmailEventPayload parse(String rawBody) throws Exception {
        String trimmed = rawBody.trim();
        String json = trimmed.startsWith("\"")
                ? objectMapper.readValue(trimmed, String.class) // unwrap double-encoded payload
                : trimmed;
        JsonNode node = objectMapper.readTree(json);

        EmailEventPayload payload = new EmailEventPayload();
        payload.to = text(node, "to");
        payload.subject = text(node, "subject");
        payload.templateName = text(node, "templateName");
        payload.emailType = text(node, "emailType");

        JsonNode vars = node.get("variables");
        if (vars != null && vars.isObject()) {
            Map<String, Object> map = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = vars.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                map.put(e.getKey(), unwrapScalar(e.getValue()));
            }
            payload.variables = map;
        }
        return payload;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    /** Keep template variables as plain Java scalars / strings for Thymeleaf. */
    private static Object unwrapScalar(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isBoolean()) {
            return v.asBoolean();
        }
        if (v.isNumber()) {
            return v.numberValue();
        }
        if (v.isTextual()) {
            return v.asText();
        }
        // Objects/arrays kept as their JSON text — templates rarely use nested vars here.
        return v.toString();
    }

    private static String truncate(String s) {
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }

    /** Local mirror of the producer-side {@code EmailEvent} (avoids a cross-module dep). */
    private static final class EmailEventPayload {
        private String to;
        private String subject;
        private String templateName;
        private String emailType;
        private Map<String, Object> variables;
    }
}
