package com.kitehub.subscription.client;

import com.kitehub.subscription.repository.EmailSentLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.kitehub.platform.domain.entity.EmailSentLog;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailServiceClient idempotency.
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

    private EmailServiceClient emailServiceClient;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        emailServiceClient = new EmailServiceClient(restTemplate, emailSentLogRepository);
        instanceId = UUID.randomUUID();
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

            // Should not call RestTemplate
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
            // Should not save new log
            verify(emailSentLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should send and log when email not yet sent today")
        void shouldSendAndLogWhenNotYetSentToday() {
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

            // Should call RestTemplate
            verify(restTemplate).postForEntity(anyString(), any(), eq(EmailServiceClient.EmailResponse.class));

            // Should save log
            ArgumentCaptor<EmailSentLog> captor = ArgumentCaptor.forClass(EmailSentLog.class);
            verify(emailSentLogRepository).save(captor.capture());

            EmailSentLog saved = captor.getValue();
            assertThat(saved.getInstanceId()).isEqualTo(instanceId);
            assertThat(saved.getEmailType()).isEqualTo("trial-warning");
            assertThat(saved.getRecipient()).isEqualTo("test@example.com");
            assertThat(saved.getSentAt()).isNotNull();
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

        @Test
        @DisplayName("Should support sending without instanceId (null)")
        void shouldSupportSendingWithoutInstanceId() {
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                eq(null), eq("trial-warning"), eq("test@example.com"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

            EmailServiceClient.EmailResponse mockResponse = new EmailServiceClient.EmailResponse();
            mockResponse.setSuccess(true);
            when(restTemplate.postForEntity(anyString(), any(), eq(EmailServiceClient.EmailResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

            // Use the overload without instanceId
            emailServiceClient.sendTrialExpirationWarning(
                "test@example.com", "Test Org", 3);

            verify(restTemplate).postForEntity(anyString(), any(), eq(EmailServiceClient.EmailResponse.class));
        }
    }
}
