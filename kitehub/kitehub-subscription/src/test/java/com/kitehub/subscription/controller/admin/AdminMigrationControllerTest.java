package com.kitehub.subscription.controller.admin;

import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.MigrationPhase;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.ForceConvertRequest;
import com.kitehub.subscription.dto.RollbackRequest;
import com.kitehub.subscription.dto.RollbackResponse;
import com.kitehub.subscription.dto.UpgradeRequest;
import com.kitehub.subscription.dto.UpgradeResponse;
import com.kitehub.subscription.exception.MigrationException;
import com.kitehub.subscription.service.TrialToPaidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminMigrationController} (GAP-192 Phase 4b-i).
 *
 * <p>Admin authentication is enforced by Spring Security {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")}
 * on each handler (GAP-938, Wave flow-kh3). Authorization is verified at the Spring Security filter
 * chain level — these pure Mockito tests instantiate the controller directly and therefore bypass
 * the security chain, focusing instead on the controller-level contract (payload mapping, service
 * calls, exception propagation).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMigrationController")
class AdminMigrationControllerTest {

    @Mock
    private TrialToPaidService trialToPaidService;

    private AdminMigrationController controller;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        controller = new AdminMigrationController(trialToPaidService);
        instanceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("force-convert happy path: builds UpgradeRequest with sentinel paymentMethodId + returns 202")
    void forceConvertHappy() {
        UpgradeResponse expected = UpgradeResponse.builder()
            .instanceId(instanceId)
            .migrationPhase(MigrationPhase.PAYMENT_CAPTURED)
            .startedAt(LocalDateTime.now())
            .estimatedCompletionSeconds(5)
            .pollUrl("/poll")
            .build();
        when(trialToPaidService.forceConvert(eq(instanceId), any(UpgradeRequest.class),
            eq("INV-42"), eq("Bank verified")))
            .thenReturn(expected);

        ForceConvertRequest req = ForceConvertRequest.builder()
            .tier(PricingTier.ENTERPRISE)
            .billingCycle("ANNUAL")
            .invoiceRef("INV-42")
            .reason("Bank verified")
            .build();
        ResponseEntity<UpgradeResponse> resp = controller.forceConvert(instanceId, req);

        assertThat(resp.getStatusCode().value()).isEqualTo(202);
        assertThat(resp.getBody().getMigrationPhase()).isEqualTo(MigrationPhase.PAYMENT_CAPTURED);

        ArgumentCaptor<UpgradeRequest> cap = ArgumentCaptor.forClass(UpgradeRequest.class);
        verify(trialToPaidService).forceConvert(eq(instanceId), cap.capture(),
            eq("INV-42"), eq("Bank verified"));
        assertThat(cap.getValue().getTier()).isEqualTo(PricingTier.ENTERPRISE);
        assertThat(cap.getValue().getPaymentMethodId()).contains("admin-force-convert");
        assertThat(cap.getValue().getIdempotencyKey()).isEqualTo("admin:INV-42");
    }

    @Test
    @DisplayName("force-convert propagates MIGRATION_IN_FLIGHT through GlobalExceptionHandler")
    void forceConvertInFlight() {
        when(trialToPaidService.forceConvert(any(), any(), any(), any()))
            .thenThrow(new MigrationException(
                MigrationException.Code.MIGRATION_IN_FLIGHT, "already running"));

        ForceConvertRequest req = ForceConvertRequest.builder()
            .tier(PricingTier.PREMIUM)
            .billingCycle("MONTHLY")
            .invoiceRef("INV-1")
            .reason("ops")
            .build();

        assertThatThrownBy(() -> controller.forceConvert(instanceId, req))
            .isInstanceOf(MigrationException.class)
            .extracting(e -> ((MigrationException) e).getCode())
            .isEqualTo(MigrationException.Code.MIGRATION_IN_FLIGHT);
    }

    @Test
    @DisplayName("rollback within window → 200 with new status TRIAL")
    void rollbackHappy() {
        RollbackResponse expected = RollbackResponse.builder()
            .instanceId(instanceId)
            .migrationPhase(MigrationPhase.REVERSED)
            .rolledBackAt(LocalDateTime.now())
            .newStatus(InstanceStatus.TRIAL)
            .trialExpiresAt(LocalDateTime.now().plusDays(3))
            .build();
        when(trialToPaidService.rollback(eq(instanceId), eq("reason-x"))).thenReturn(expected);

        RollbackRequest req = RollbackRequest.builder().reason("reason-x").build();
        ResponseEntity<RollbackResponse> resp = controller.rollback(instanceId, req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().getNewStatus()).isEqualTo(InstanceStatus.TRIAL);
        assertThat(resp.getBody().getMigrationPhase()).isEqualTo(MigrationPhase.REVERSED);
    }

    @Test
    @DisplayName("rollback outside window → REVERSAL_WINDOW_EXPIRED (maps to 410 via GlobalExceptionHandler)")
    void rollbackOutsideWindow() {
        doThrow(new MigrationException(
            MigrationException.Code.REVERSAL_WINDOW_EXPIRED, "24h elapsed"))
            .when(trialToPaidService).rollback(any(), any());

        RollbackRequest req = RollbackRequest.builder().reason("late").build();
        assertThatThrownBy(() -> controller.rollback(instanceId, req))
            .isInstanceOf(MigrationException.class)
            .extracting(e -> ((MigrationException) e).getCode())
            .isEqualTo(MigrationException.Code.REVERSAL_WINDOW_EXPIRED);
    }
}
