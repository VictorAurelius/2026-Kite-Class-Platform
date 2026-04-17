package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.EmailSentLog;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.config.EmailConfigProperties;
import com.kitehub.subscription.dto.EmailConfigResponse;
import com.kitehub.subscription.dto.EmailHistoryResponse;
import com.kitehub.subscription.dto.EmailStatsResponse;
import com.kitehub.subscription.repository.EmailSentLogRepository;
import com.kitehub.subscription.repository.InstanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailAdminService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailAdminService Unit Tests")
class EmailAdminServiceTest {

    @Mock
    private EmailSentLogRepository emailSentLogRepository;

    @Mock
    private EmailConfigProperties emailConfigProperties;

    @Mock
    private EmailServiceClient emailServiceClient;

    @Mock
    private InstanceRepository instanceRepository;

    @InjectMocks
    private EmailAdminService emailAdminService;

    private UUID instanceId;
    private Instance testInstance;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        testInstance = new Instance();
        testInstance.setId(instanceId);
        testInstance.setSubdomain("test-school");
        testInstance.setOrganizationName("Test School");
        testInstance.setContactEmail("admin@test.com");
        testInstance.setStatus(InstanceStatus.ACTIVE);
        testInstance.setTier(PricingTier.BASIC);
    }

    @Nested
    @DisplayName("getEmailHistory")
    class GetEmailHistory {

        @Test
        @DisplayName("should return paginated results for all emails")
        void shouldReturnPaginatedResults() {
            EmailSentLog log = EmailSentLog.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .emailType("trial-warning")
                .recipient("admin@test.com")
                .sentAt(LocalDateTime.now())
                .build();

            Page<EmailSentLog> page = new PageImpl<>(List.of(log));
            when(emailSentLogRepository.findBySentAtBetween(any(), any(), any(Pageable.class)))
                .thenReturn(page);

            Page<EmailHistoryResponse> result = emailAdminService.getEmailHistory(
                null, null, null, null, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmailType()).isEqualTo("trial-warning");
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("should filter by instanceId and emailType")
        void shouldFilterByInstanceIdAndType() {
            EmailSentLog log = EmailSentLog.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .emailType("trial-warning")
                .recipient("admin@test.com")
                .sentAt(LocalDateTime.now())
                .build();

            Page<EmailSentLog> page = new PageImpl<>(List.of(log));
            when(emailSentLogRepository.findByInstanceIdAndEmailTypeContainingAndSentAtBetween(
                eq(instanceId), eq("trial"), any(), any(), any(Pageable.class)))
                .thenReturn(page);

            Page<EmailHistoryResponse> result = emailAdminService.getEmailHistory(
                instanceId, "trial", null, null, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should filter by instanceId only")
        void shouldFilterByInstanceIdOnly() {
            Page<EmailSentLog> page = new PageImpl<>(List.of());
            when(emailSentLogRepository.findByInstanceIdAndSentAtBetween(
                eq(instanceId), any(), any(), any(Pageable.class)))
                .thenReturn(page);

            Page<EmailHistoryResponse> result = emailAdminService.getEmailHistory(
                instanceId, null, null, null, 0, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should derive FAILED status from emailType suffix")
        void shouldDeriveFaledStatusFromSuffix() {
            EmailSentLog log = EmailSentLog.builder()
                .id(UUID.randomUUID())
                .instanceId(instanceId)
                .emailType("trial-warning:FAILED")
                .recipient("admin@test.com")
                .sentAt(LocalDateTime.now())
                .build();

            Page<EmailSentLog> page = new PageImpl<>(List.of(log));
            when(emailSentLogRepository.findBySentAtBetween(any(), any(), any(Pageable.class)))
                .thenReturn(page);

            Page<EmailHistoryResponse> result = emailAdminService.getEmailHistory(
                null, null, null, null, 0, 10);

            assertThat(result.getContent().get(0).getStatus()).isEqualTo("FAILED");
            assertThat(result.getContent().get(0).getEmailType()).isEqualTo("trial-warning");
        }
    }

    @Nested
    @DisplayName("getEmailStats")
    class GetEmailStats {

        @Test
        @DisplayName("should return aggregate statistics")
        void shouldReturnAggregateStats() {
            when(emailSentLogRepository.countBySentAtBetween(any(), any()))
                .thenReturn(42L)
                .thenReturn(200L);
            when(emailSentLogRepository.countByEmailTypeContainingAndSentAtBetween(
                eq(":FAILED"), any(), any()))
                .thenReturn(3L);
            when(emailSentLogRepository.countByEmailTypeGrouped(any(), any()))
                .thenReturn(List.of(
                    new Object[]{"trial-warning", 15L},
                    new Object[]{"renewal-reminder", 8L}
                ));

            EmailStatsResponse result = emailAdminService.getEmailStats();

            assertThat(result.getTotalSentToday()).isEqualTo(42L);
            assertThat(result.getTotalSentThisWeek()).isEqualTo(200L);
            assertThat(result.getFailedToday()).isEqualTo(3L);
            assertThat(result.getCountByType()).containsEntry("trial-warning", 15L);
            assertThat(result.getCountByType()).containsEntry("renewal-reminder", 8L);
        }
    }

    @Nested
    @DisplayName("getEmailConfig")
    class GetEmailConfig {

        @Test
        @DisplayName("should return current config state")
        void shouldReturnCurrentConfig() {
            when(emailConfigProperties.isUseQueue()).thenReturn(true);
            Map<String, Boolean> toggles = new HashMap<>();
            toggles.put("trial-warning", true);
            toggles.put("suspension-notification", false);
            when(emailConfigProperties.getTypeToggles()).thenReturn(toggles);

            EmailConfigResponse result = emailAdminService.getEmailConfig();

            assertThat(result.isQueueEnabled()).isTrue();
            assertThat(result.getEmailTypeToggles()).containsEntry("trial-warning", true);
            assertThat(result.getEmailTypeToggles()).containsEntry("suspension-notification", false);
        }
    }

    @Nested
    @DisplayName("updateEmailConfig")
    class UpdateEmailConfig {

        @Test
        @DisplayName("should update toggles in-memory")
        void shouldUpdateToggles() {
            Map<String, Boolean> existingToggles = new HashMap<>();
            existingToggles.put("trial-warning", true);
            when(emailConfigProperties.getTypeToggles()).thenReturn(existingToggles);
            when(emailConfigProperties.isUseQueue()).thenReturn(true);

            Map<String, Boolean> newToggles = Map.of("trial-warning", false, "welcome", true);

            EmailConfigResponse result = emailAdminService.updateEmailConfig(newToggles);

            // Verify toggles were updated
            assertThat(existingToggles).containsEntry("trial-warning", false);
            assertThat(existingToggles).containsEntry("welcome", true);
        }
    }

    @Nested
    @DisplayName("triggerEmail")
    class TriggerEmail {

        @BeforeEach
        void setUpIdempotency() {
            // Default: no email sent today (idempotency check passes)
            // lenient() because some tests override this stub
            lenient().when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                any(), anyString(), anyString(), any(), any())).thenReturn(false);
        }

        @Test
        @DisplayName("should reject if same email already sent today (idempotency)")
        void shouldRejectDuplicateEmail() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(testInstance));
            when(emailSentLogRepository.existsByInstanceIdAndEmailTypeAndRecipientAndSentAtBetween(
                any(), eq("trial-warning"), anyString(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> emailAdminService.triggerEmail(instanceId, "trial-warning"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already sent today");
        }

        @Test
        @DisplayName("should dispatch trial-warning via EmailServiceClient")
        void shouldDispatchTrialWarning() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(testInstance));

            emailAdminService.triggerEmail(instanceId, "trial-warning");

            verify(emailServiceClient).sendTrialExpirationWarning(
                eq(instanceId), eq("admin@test.com"), eq("Test School"), eq(1L));
        }

        @Test
        @DisplayName("should dispatch welcome email via EmailServiceClient")
        void shouldDispatchWelcomeEmail() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(testInstance));

            emailAdminService.triggerEmail(instanceId, "welcome");

            verify(emailServiceClient).sendWelcomeEmail(
                eq(instanceId), eq("admin@test.com"), eq("Test School"), eq(14), eq("N/A"));
        }

        @Test
        @DisplayName("should throw for unknown email type")
        void shouldThrowForUnknownType() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(testInstance));

            assertThatThrownBy(() -> emailAdminService.triggerEmail(instanceId, "unknown-type"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown email type");
        }

        @Test
        @DisplayName("should throw when instance not found")
        void shouldThrowWhenInstanceNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(instanceRepository.findById(unknownId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailAdminService.triggerEmail(unknownId, "trial-warning"))
                .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should dispatch trial-expired email")
        void shouldDispatchTrialExpired() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(testInstance));

            emailAdminService.triggerEmail(instanceId, "trial-expired");

            verify(emailServiceClient).sendTrialExpired(
                eq(instanceId), eq("admin@test.com"), eq("Test School"));
        }

        @Test
        @DisplayName("should dispatch data-deleted notification")
        void shouldDispatchDataDeleted() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(testInstance));

            emailAdminService.triggerEmail(instanceId, "data-deleted");

            verify(emailServiceClient).sendDataDeletedNotification(
                eq(instanceId), eq("admin@test.com"), eq("Test School"));
        }

        @Test
        @DisplayName("should dispatch trial-midpoint email")
        void shouldDispatchTrialMidpoint() {
            when(instanceRepository.findById(instanceId))
                .thenReturn(Optional.of(testInstance));

            emailAdminService.triggerEmail(instanceId, "trial-midpoint");

            verify(emailServiceClient).sendTrialMidpointEmail(
                eq(instanceId), eq("admin@test.com"), eq("test-school"));
        }
    }
}
