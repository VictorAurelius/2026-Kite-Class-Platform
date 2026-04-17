package com.kitehub.subscription.client;

import com.kitehub.subscription.config.EmailConfigProperties;
import com.kitehub.subscription.config.EmailQueueConfig;
import com.kitehub.subscription.dto.EmailEvent;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.kitehub.platform.domain.entity.EmailSentLog;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private EmailServiceClient emailServiceClient;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        emailServiceClient = new EmailServiceClient(restTemplate, emailSentLogRepository, rabbitTemplate, emailConfigProperties);
        instanceId = UUID.randomUUID();
        lenient().when(emailConfigProperties.getTypeToggles()).thenReturn(new HashMap<>());
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

            // Should publish to RabbitMQ
            ArgumentCaptor<EmailEvent> eventCaptor = ArgumentCaptor.forClass(EmailEvent.class);
            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                eventCaptor.capture());

            EmailEvent event = eventCaptor.getValue();
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

            ArgumentCaptor<EmailEvent> eventCaptor = ArgumentCaptor.forClass(EmailEvent.class);
            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                eventCaptor.capture());

            EmailEvent event = eventCaptor.getValue();
            assertThat(event.getEmailType()).isEqualTo("welcome");
            assertThat(event.getTemplateName()).isEqualTo("welcome");
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

            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(EmailEvent.class));
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
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));

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
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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

            // Should publish to queue (default mode)
            ArgumentCaptor<EmailEvent> eventCaptor = ArgumentCaptor.forClass(EmailEvent.class);
            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                eventCaptor.capture());

            assertThat(eventCaptor.getValue().getInstanceId()).isNull();
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

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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

            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(EmailEvent.class));

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

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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

            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(EmailEvent.class));

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

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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

            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(EmailEvent.class));

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

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
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

            verify(rabbitTemplate).convertAndSend(
                eq(EmailQueueConfig.EMAIL_EXCHANGE),
                eq(EmailQueueConfig.EMAIL_ROUTING_KEY),
                any(EmailEvent.class));

            ArgumentCaptor<EmailSentLog> captor = ArgumentCaptor.forClass(EmailSentLog.class);
            verify(emailSentLogRepository).save(captor.capture());
            assertThat(captor.getValue().getEmailType()).isEqualTo("retention-final-warning");
        }
    }
}
