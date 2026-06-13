package com.kitehub.subscription.controller;

import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.billing.dto.DowngradePreviewResponse;
import com.kitehub.subscription.billing.dto.PendingPaymentStatusResponse;
import com.kitehub.subscription.billing.dto.ReactivateResponse;
import com.kitehub.subscription.billing.service.OwnerBillingService;
import com.kitehub.subscription.service.SubscriptionRenewalService;
import com.kitehub.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the 3 owner-facing billing endpoints added to {@link SubscriptionController}
 * (GAP-1257-BE / GAP-1261 / GAP-1263-BE). Calls the controller methods directly with a matching
 * {@code X-Tenant-Id} header so the static {@code TenantOwnershipGuard} ownership check passes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionController owner-billing endpoints")
class SubscriptionControllerTest {

    @Mock private SubscriptionService subscriptionService;
    @Mock private SubscriptionRenewalService renewalService;
    @Mock private OwnerBillingService ownerBillingService;

    @InjectMocks private SubscriptionController controller;

    private UUID instanceId;
    private String tenantHeader;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        tenantHeader = instanceId.toString(); // owner of this instance — passes ownership guard
    }

    @Test
    @DisplayName("GAP-1257-BE: pending-payment-status delegates to OwnerBillingService")
    void getPendingPaymentStatus_delegates() {
        PendingPaymentStatusResponse stub = PendingPaymentStatusResponse.builder()
            .hasPendingPayment(true).build();
        when(ownerBillingService.getPendingPaymentStatus(instanceId)).thenReturn(stub);

        ResponseEntity<PendingPaymentStatusResponse> resp =
            controller.getPendingPaymentStatus(tenantHeader, instanceId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(stub);
        verify(ownerBillingService).getPendingPaymentStatus(instanceId);
    }

    @Test
    @DisplayName("GAP-1261: downgrade-preview delegates with the requested target tier")
    void getDowngradePreview_delegates() {
        DowngradePreviewResponse stub = DowngradePreviewResponse.builder()
            .currentTier(PricingTier.PREMIUM).targetTier(PricingTier.BASIC).build();
        when(ownerBillingService.getDowngradePreview(instanceId, PricingTier.BASIC)).thenReturn(stub);

        ResponseEntity<DowngradePreviewResponse> resp =
            controller.getDowngradePreview(tenantHeader, instanceId, PricingTier.BASIC);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(stub);
        verify(ownerBillingService).getDowngradePreview(instanceId, PricingTier.BASIC);
    }

    @Test
    @DisplayName("GAP-1263-BE: reactivate delegates to OwnerBillingService")
    void reactivate_delegates() {
        ReactivateResponse stub = ReactivateResponse.builder()
            .instanceId(instanceId)
            .outcome(ReactivateResponse.Outcome.PAYMENT_REQUIRED)
            .build();
        when(ownerBillingService.reactivate(instanceId)).thenReturn(stub);

        ResponseEntity<ReactivateResponse> resp = controller.reactivate(tenantHeader, instanceId);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(stub);
        verify(ownerBillingService).reactivate(instanceId);
    }
}
