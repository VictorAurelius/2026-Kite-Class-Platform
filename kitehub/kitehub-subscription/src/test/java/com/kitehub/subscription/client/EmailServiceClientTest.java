package com.kitehub.subscription.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.subscription.config.EmailConfigProperties;
import com.kitehub.subscription.config.EmailQueueConfig;
import com.kitehub.subscription.dto.EmailEvent;
import com.kitehub.subscription.outbox.SubscriptionOutboxEvent;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.kitehub.platform.domain.entity.EmailSentLog;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailServiceClient.
 * Tests both queue mode and direct HTTP mode, plus idempotency.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceClient Unit Tests")
class EmailServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EmailSentLogRepository emailSentLogRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private EmailConfigProperties emailConfigProperties;

    @Mock
    private SubscriptionOutboxRepository outboxRepository;

    private SubscriptionEventEmitter eventEmitter;
    private ObjectMapper objectMapper;

    private EmailServiceClient emailServiceClient;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        // GAP-937 (2026-06-04): construct emitter WITH rabbitTemplate so the production
        // fast-path (`rabbitTemplate.send(exchange, routingKey, Message)` in
        // SubscriptionEventEmitter line 105) actually fires. Previously this test built
        // emitter via single-arg constructor → fast-path never ran → mock verifications
        // of rabbitTemplate.convertAndSend(...) were dead stubs / always failed.
        eventEmitter = new SubscriptionEventEmitter(outboxRepository, rabbitTemplate);
        // Spring Boot's auto-configured ObjectMapper has JSR-310 module registered;
        // findAndRegisterModules() picks the same set up for unit tests
        // (per memory: bare new ObjectMapper() drops Java 8 time types).
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        emailServiceClient = new EmailServiceClient(restTemplate, emailSentLogRepository,
            rabbitTemplate, emailConfigProperties, eventEmitter, objectMapper);
        // GAP-1414: appBaseUrl is @Value-injected at runtime (default https://kitehub.me).
        // Mockito unit tests bypass Spring, so set the canonical default explicitly here —
        // appDomain() would NPE otherwise when building per-tenant subdomain dashboard URLs.
        ReflectionTestUtils.setField(emailServiceClient, "appBaseUrl", "https://kitehub.me");
        instanceId = UUID.randomUUID();
        lenient().when(emailConfigProperties.getTypeToggles()).thenReturn(new HashMap<>());
    }

    /**
     * Decode the JSON body of an AMQP {@link Message} into an {@link EmailEvent}.
     * Production path: EmailServiceClient.publishToQueue serializes EmailEvent →
     * SubscriptionEventEmitter.emit wraps raw UTF-8 bytes in Message body
     * with Content-Type=application/json (per GAP-925 fix).
     */
    private EmailEvent decodeEmailEvent(Message msg) {
        try {
            return objectMapper.readValue(
                new String(msg.getBody(), StandardCharsets.UTF_8),
                EmailEvent.class);
        } catch (Exception e) {
            throw new AssertionError("Failed to decode EmailEvent from Message body", e);
        }
    }

    @Nested
    @DisplayName("Queue Mode (default)")
    class QueueMode {

        @BeforeEach
        void setUpQueueMode() {
            ReflectionTestUtils.setField(emailServiceClient, "useQueue", true);
        }

        @Test
        @DisplayName("Should publish to RabbitMQ queue instead of HTTP")
        void shouldPublishToQueueInsteadOfHttp() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendTrialExpirationWarning(
                instanceId, "test@example.com", "Test Org", 3);

            // GAP-937: production uses rabbitTemplate.send(exchange, routingKey, Message)
            // with raw UTF-8 JSON body — NOT convertAndSend.
            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                msgCaptor.capture());

            EmailEvent event = decodeEmailEvent(msgCaptor.getValue());
            assertThat(event.getInstanceId()).isEqualTo(instanceId);
            assertThat(event.getTo()).isEqualTo("test@example.com");
            assertThat(event.getEmailType()).isEqualTo("trial-warning");
            assertThat(event.getTemplateName()).isEqualTo("trial-expiration-warning");
            assertThat(event.getSubject()).contains("trial expires in 3 days");
            assertThat(event.getVariables()).containsEntry("organizationName", "Test Org");

            // Should NOT call HTTP
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());

            // Should still record in sent log
            verify(emailSentLogRepository).save(any(EmailSentLog.class));
        }

        @Test
        @DisplayName("Should publish welcome email to queue")
        void shouldPublishWelcomeEmailToQueue() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("welcome"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendWelcomeEmail(
                instanceId, "test@example.com", "Test Org", 14, "2026-04-30");

            // GAP-937: send(...) not convertAndSend
            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                msgCaptor.capture());

            EmailEvent event = decodeEmailEvent(msgCaptor.getValue());
            assertThat(event.getEmailType()).isEqualTo("welcome");
            assertThat(event.getTemplateName()).isEqualTo("welcome");
        }

        @Test
        @DisplayName("Should publish tenant-ready email to queue (GAP-948)")
        void shouldPublishTenantReadyEmailToQueue() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("tenant-ready"), eq("owner@acme.edu.vn"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendTenantReadyEmail(
                instanceId, "owner@acme.edu.vn", "Acme School", "acme");

            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                msgCaptor.capture());

            EmailEvent event = decodeEmailEvent(msgCaptor.getValue());
            assertThat(event.getEmailType()).isEqualTo("tenant-ready");
            assertThat(event.getTemplateName()).isEqualTo("tenant-ready");
            assertThat(event.getTo()).isEqualTo("owner@acme.edu.vn");
            assertThat(event.getSubject()).contains("sẵn sàng");
            assertThat(event.getVariables()).containsEntry("organizationName", "Acme School");
            assertThat(event.getVariables())
                .containsEntry("dashboardUrl", "https://acme.kitehub.me/dashboard");

            // Should still record in sent log (dedup tracking)
            verify(emailSentLogRepository).save(any(EmailSentLog.class));
        }

        @Test
        @DisplayName("Should skip tenant-ready email when already sent today (GAP-948)")
        void shouldSkipTenantReadyEmailWhenAlreadySentToday() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("tenant-ready"), eq("owner@acme.edu.vn"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendTenantReadyEmail(
                instanceId, "owner@acme.edu.vn", "Acme School", "acme");

            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
            verify(emailSentLogRepository, never()).save(any(EmailSentLog.class));
        }

        @Test
        @DisplayName("Should publish subscription created email to queue")
        void shouldPublishSubscriptionCreatedEmailToQueue() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("subscription-created"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendSubscriptionCreatedEmail(
                instanceId, "test@example.com", "Test Org", "PREMIUM", "MONTHLY");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(Message.class));
        }
    }

    @Nested
    @DisplayName("Direct HTTP Mode (use-queue=false)")
    class DirectMode {

        @BeforeEach
        void setUpDirectMode() {
            ReflectionTestUtils.setField(emailServiceClient, "useQueue", false);
        }

        @Test
        @DisplayName("Should send via HTTP when queue disabled")
        void shouldSendViaHttpWhenQueueDisabled() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            EmailServiceClient.EmailResponse mockResponse = new EmailServiceClient.EmailResponse();
            mockResponse.setSuccess(true);
            when(restTemplate.postForEntity(anyString(), any(), eq(EmailServiceClient.EmailResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

            emailServiceClient.sendTrialExpirationWarning(
                instanceId, "test@example.com", "Test Org", 3);

            // Should call HTTP
            verify(restTemplate).postForEntity(anyString(), any(), eq(EmailServiceClient.EmailResponse.class));

            // Should NOT publish to RabbitMQ
            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));

            // Should still record in sent log
            verify(emailSentLogRepository).save(any(EmailSentLog.class));
        }
    }

    @Nested
    @DisplayName("Email Idempotency")
    class EmailIdempotency {

        @Test
        @DisplayName("Should skip sending when email already sent today")
        void shouldSkipWhenAlreadySentToday() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendTrialExpirationWarning(
                instanceId, "test@example.com", "Test Org", 3);

            // Should not call RestTemplate or RabbitTemplate
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
            // Should not save new log
            verify(emailSentLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should check idempotency for retention warning")
        void shouldCheckIdempotencyForRetentionWarning() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("retention-warning"), eq("admin@org.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendRetentionWarning(
                instanceId, "admin@org.com", "My Org", 5);

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should check idempotency for data deleted notification")
        void shouldCheckIdempotencyForDataDeletedNotification() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("data-deleted"), eq("admin@org.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendDataDeletedNotification(
                instanceId, "admin@org.com", "My Org");

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should check idempotency for trial expired")
        void shouldCheckIdempotencyForTrialExpired() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-expired"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendTrialExpired(instanceId, "test@example.com", "Test Org");

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should check idempotency for suspension notification")
        void shouldCheckIdempotencyForSuspensionNotification() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("suspension-notification"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendSuspensionNotification(
                instanceId, "test@example.com", "Test Org");

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should check idempotency for renewal reminder")
        void shouldCheckIdempotencyForRenewalReminder() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("renewal-reminder"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendRenewalReminder(
                instanceId, "test@example.com", "Test Org", 7, "BASIC", 500000);

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }
    }

    @Nested
    @DisplayName("alreadySentToday")
    class AlreadySentToday {

        @Test
        @DisplayName("Should return true when email exists in log for today")
        void shouldReturnTrueWhenExists() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            boolean result = emailServiceClient.alreadySentToday(
                instanceId, "trial-warning", "test@example.com");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when email not in log for today")
        void shouldReturnFalseWhenNotExists() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            boolean result = emailServiceClient.alreadySentToday(
                instanceId, "trial-warning", "test@example.com");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Backward Compatibility")
    class BackwardCompatibility {

        @BeforeEach
        void setUpQueueMode() {
            ReflectionTestUtils.setField(emailServiceClient, "useQueue", true);
        }

        @Test
        @DisplayName("Should support sending without instanceId (null)")
        void shouldSupportSendingWithoutInstanceId() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(null), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            // Use the overload without instanceId
            emailServiceClient.sendTrialExpirationWarning(
                "test@example.com", "Test Org", 3);

            // GAP-937: send(...) not convertAndSend
            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                msgCaptor.capture());

            assertThat(decodeEmailEvent(msgCaptor.getValue()).getInstanceId()).isNull();
        }
    }

    @Nested
    @DisplayName("New Email Methods - SAAS-7")
    class NewEmailMethods {

        @BeforeEach
        void setUpQueueMode() {
            ReflectionTestUtils.setField(emailServiceClient, "useQueue", true);
        }

        @Test
        @DisplayName("Should check idempotency for trial midpoint email")
        void shouldCheckIdempotencyForTrialMidpointEmail() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-midpoint"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendTrialMidpointEmail(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should publish trial midpoint email to queue when not sent today")
        void shouldPublishTrialMidpointEmailWhenNotSentToday() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-midpoint"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendTrialMidpointEmail(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(Message.class));

            ArgumentCaptor<EmailSentLog> captor = ArgumentCaptor.forClass(EmailSentLog.class);
            verify(emailSentLogRepository).save(captor.capture());
            assertThat(captor.getValue().getEmailType()).isEqualTo("trial-midpoint");
        }

        @Test
        @DisplayName("Should check idempotency for onboarding tips email")
        void shouldCheckIdempotencyForOnboardingTipsEmail() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("onboarding-tips"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendOnboardingTipsEmail(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should publish onboarding tips email to queue when not sent today")
        void shouldPublishOnboardingTipsEmailWhenNotSentToday() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("onboarding-tips"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendOnboardingTipsEmail(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(Message.class));

            ArgumentCaptor<EmailSentLog> captor = ArgumentCaptor.forClass(EmailSentLog.class);
            verify(emailSentLogRepository).save(captor.capture());
            assertThat(captor.getValue().getEmailType()).isEqualTo("onboarding-tips");
        }

        @Test
        @DisplayName("Should check idempotency for subscription expired email")
        void shouldCheckIdempotencyForSubscriptionExpiredEmail() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("subscription-expired"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendSubscriptionExpiredEmail(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should publish subscription expired email to queue when not sent today")
        void shouldPublishSubscriptionExpiredEmailWhenNotSentToday() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("subscription-expired"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendSubscriptionExpiredEmail(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(Message.class));

            ArgumentCaptor<EmailSentLog> captor = ArgumentCaptor.forClass(EmailSentLog.class);
            verify(emailSentLogRepository).save(captor.capture());
            assertThat(captor.getValue().getEmailType()).isEqualTo("subscription-expired");
        }

        @Test
        @DisplayName("Should check idempotency for data retention final warning email")
        void shouldCheckIdempotencyForDataRetentionFinalWarning() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("retention-final-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

            emailServiceClient.sendDataRetentionFinalWarning(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("Should publish data retention final warning to queue when not sent today")
        void shouldPublishDataRetentionFinalWarningWhenNotSentToday() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("retention-final-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendDataRetentionFinalWarning(
                instanceId, "test@example.com", "test");

            // GAP-937: send(...) not convertAndSend
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(Message.class));

            ArgumentCaptor<EmailSentLog> captor = ArgumentCaptor.forClass(EmailSentLog.class);
            verify(emailSentLogRepository).save(captor.capture());
            assertThat(captor.getValue().getEmailType()).isEqualTo("retention-final-warning");
        }
    }

    @Nested
    @DisplayName("Exception A — outbox + best-effort fast-path (GAP-222c)")
    class OutboxFastPath {

        @BeforeEach
        void setUpQueueMode() {
            ReflectionTestUtils.setField(emailServiceClient, "useQueue", true);
        }

        @Test
        @DisplayName("writes outbox row before fast-path publish on queue mode")
        void writesOutboxRowOnQueuePublish() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendTrialExpirationWarning(
                instanceId, "test@example.com", "Test Org", 3);

            ArgumentCaptor<SubscriptionOutboxEvent> captor = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
            verify(outboxRepository).save(captor.capture());
            SubscriptionOutboxEvent saved = captor.getValue();
            assertThat(saved.getInstanceId()).isEqualTo(instanceId);
            assertThat(saved.getEventType()).isEqualTo(EmailServiceClient.EVENT_TYPE_EMAIL_QUEUED);
            assertThat(saved.getTopic()).isEqualTo(EmailQueueConfig.EMAIL_ROUTING_KEY);
            assertThat(saved.getPayload())
                .contains("test@example.com")
                .contains("trial-warning");
        }

        @Test
        @DisplayName("writes outbox row with null instanceId for system-level emails")
        void writesOutboxRowForOrphanEmail() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq((UUID) null), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            // 2-arg overload passes null instanceId
            emailServiceClient.sendTrialExpirationWarning("test@example.com", "Test Org", 3);

            ArgumentCaptor<SubscriptionOutboxEvent> captor = ArgumentCaptor.forClass(SubscriptionOutboxEvent.class);
            verify(outboxRepository).save(captor.capture());
            assertThat(captor.getValue().getInstanceId()).isNull();
        }

        @Test
        @DisplayName("swallows broker failure on direct publish — outbox row still written")
        void brokerDownDoesNotPropagate() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), anyString(), anyString(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
            // GAP-937: stub send(...) not convertAndSend — matches production path
            doThrow(new AmqpException("broker offline"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

            // Caller's outer try/catch in sendTrialExpirationWarning swallows generic
            // exceptions — but the outbox row must be written first regardless.
            emailServiceClient.sendTrialExpirationWarning(
                instanceId, "test@example.com", "Test Org", 3);

            verify(outboxRepository).save(any(SubscriptionOutboxEvent.class));
        }
    }

    @Nested
    @DisplayName("GAP-1273 — NULL-recipient guard (D1 defensive)")
    class NullRecipientGuard {

        @Test
        @DisplayName("subscription-created email with NULL recipient skips dispatch — no EmailSentLog INSERT, no publish")
        void nullRecipientSkipsDispatchAndLog() {
            // No contact_email → null recipient. dispatchEmail must skip the whole dispatch BEFORE
            // recordEmailSent (the email_sent_log.recipient NOT-NULL INSERT that rolled back the
            // paid-upgrade tier-flip in the G3 walk).
            emailServiceClient.sendSubscriptionCreatedEmail(
                instanceId, null, "Trung tâm Demo", "PREMIUM", "MONTHLY");

            verify(emailSentLogRepository, never()).save(any(EmailSentLog.class));
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }

        @Test
        @DisplayName("subscription-activated email with blank recipient skips dispatch")
        void blankRecipientSkipsDispatch() {
            emailServiceClient.sendSubscriptionActivatedEmail(
                instanceId, "   ", "Trung tâm Demo", "PREMIUM", "2026-07-14");

            verify(emailSentLogRepository, never()).save(any(EmailSentLog.class));
            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        }
    }

    @Nested
    @DisplayName("GAP-1414 — email link domain from config (not hardcoded)")
    class UrlFromConfig {

        @BeforeEach
        void setUpQueueMode() {
            ReflectionTestUtils.setField(emailServiceClient, "useQueue", true);
        }

        /**
         * Apex links (upgradeUrl, loginUrl, ...) must derive from the configured base URL —
         * proves they are NO LONGER hardcoded to kitehub.com / kitehub.vn. Overriding the
         * config field flips every link to the new domain.
         */
        @Test
        @DisplayName("apex links use the configured base URL, not a hardcoded domain")
        void apexLinksUseConfiguredBaseUrl() {
            ReflectionTestUtils.setField(emailServiceClient, "appBaseUrl", "https://staging.kitehub.test");
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendTrialExpirationWarning(
                instanceId, "test@example.com", "Test Org", 3);

            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                msgCaptor.capture());

            EmailEvent event = decodeEmailEvent(msgCaptor.getValue());
            assertThat(event.getVariables())
                .containsEntry("upgradeUrl", "https://staging.kitehub.test/pricing");
        }

        /**
         * Per-tenant subdomain dashboard URL must derive the bare host from the configured
         * base URL (scheme stripped) — {@code https://<subdomain>.<appDomain>/dashboard}.
         */
        @Test
        @DisplayName("subdomain dashboard URL derives bare host from configured base URL")
        void subdomainDashboardUrlUsesConfiguredDomain() {
            ReflectionTestUtils.setField(emailServiceClient, "appBaseUrl", "https://staging.kitehub.test");
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("tenant-ready"), eq("owner@acme.edu.vn"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendTenantReadyEmail(
                instanceId, "owner@acme.edu.vn", "Acme School", "acme");

            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                msgCaptor.capture());

            EmailEvent event = decodeEmailEvent(msgCaptor.getValue());
            assertThat(event.getVariables())
                .containsEntry("dashboardUrl", "https://acme.staging.kitehub.test/dashboard")
                .containsEntry("supportUrl", "https://staging.kitehub.test/contact");
        }

        /**
         * Canonical default (https://kitehub.me) — every customer-email link resolves to the
         * real Phase 1 BETA domain when no override is supplied (the value set in setUp matches
         * the {@code @Value} annotation default literal).
         */
        @Test
        @DisplayName("default base URL is canonical kitehub.me for apex + subdomain links")
        void defaultBaseUrlIsCanonicalKitehubMe() {
            // setUp() set appBaseUrl to the @Value default "https://kitehub.me".
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(instanceId), eq("welcome"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            emailServiceClient.sendWelcomeEmail(
                instanceId, "test@example.com", "Test Org", 14, "2026-04-30");

            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                msgCaptor.capture());

            EmailEvent event = decodeEmailEvent(msgCaptor.getValue());
            assertThat(event.getVariables())
                .containsEntry("loginUrl", "https://kitehub.me/login")
                .containsEntry("docsUrl", "https://kitehub.me/help")
                .containsEntry("unsubscribeUrl", "https://kitehub.me/unsubscribe");
        }
    }
}
