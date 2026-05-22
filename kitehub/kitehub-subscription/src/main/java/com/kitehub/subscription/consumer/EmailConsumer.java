package com.kitehub.subscription.consumer;

import com.kitehub.platform.domain.entity.EmailSentLog;
import com.kitehub.subscription.config.EmailQueueConfig;
import com.kitehub.subscription.dto.EmailEvent;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * RabbitMQ consumer for email send events.
 * <p>
 * Listens to email.send queue, sends the actual HTTP request to email service.
 * On failure, exception is thrown so RabbitMQ retries (3 attempts with backoff).
 * After all retries exhausted, message goes to email.dlq.
 * <p>
 * DLQ listener logs failed emails and records them as FAILED in EmailSentLog.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kitehub.email.use-queue", havingValue = "true", matchIfMissing = true)
public class EmailConsumer {

    private final RestTemplate restTemplate;
    private final EmailSentLogRepository emailSentLogRepository;

    @Value("${email.service.url:http://kitehub-email:8080}")
    private String emailServiceUrl;

    /**
     * Process email send event from queue.
     * Sends HTTP POST to email service. Throws on failure to trigger retry.
     *
     * @param event email event from queue
     */
    @RabbitListener(queues = EmailQueueConfig.EMAIL_QUEUE)
    public void handleEmailEvent(EmailEvent event) {
        log.info("Processing email event: type={}, to={}", event.getEmailType(), event.getTo());

        String url = emailServiceUrl + "/api/platform/emails/send";

        try {
            // Build the request matching email service API
            Map<String, Object> request = Map.of(
                    "to", event.getTo(),
                    "subject", event.getSubject(),
                    "templateName", event.getTemplateName(),
                    "variables", event.getVariables()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Email sent successfully: type={}, to={}", event.getEmailType(), event.getTo());
            } else {
                log.warn("Email service returned non-2xx status: {} for type={}, to={}",
                        response.getStatusCode(), event.getEmailType(), event.getTo());
                throw new RuntimeException("Email service returned " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email via HTTP: type={}, to={}, error={}",
                    event.getEmailType(), event.getTo(), e.getMessage());
            // Re-throw to trigger RabbitMQ retry
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle emails that failed after all retry attempts.
     * Logs the failure and records it in EmailSentLog for tracking.
     *
     * @param event failed email event from DLQ
     */
    @RabbitListener(queues = EmailQueueConfig.EMAIL_DLQ)
    public void handleFailedEmail(EmailEvent event) {
        log.error("Email permanently failed after retries: type={}, to={}, instanceId={}",
                event.getEmailType(), event.getTo(), event.getInstanceId());

        // Record failure in email log for audit trail
        try {
            emailSentLogRepository.save(EmailSentLog.builder()
                    .instanceId(event.getInstanceId())
                    .emailType(event.getEmailType() + ":FAILED")
                    .recipient(event.getTo())
                    .sentAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to record email failure in log: {}", e.getMessage());
        }
    }
}
