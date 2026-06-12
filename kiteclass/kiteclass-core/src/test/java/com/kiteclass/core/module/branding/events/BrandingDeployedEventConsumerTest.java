package com.kiteclass.core.module.branding.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.marketing.service.LandingPageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BrandingDeployedEventConsumer} (GAP-1213).
 *
 * <p>Verifies: valid payload → {@code applyDeployedBranding} with parsed theme + TenantContext
 * cleared; malformed/empty-tenant payload swallowed (no service call); apply failure ACKed
 * (no throw) + TenantContext still cleared.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandingDeployedEventConsumer")
class BrandingDeployedEventConsumerTest {

    @Mock
    private LandingPageService landingPageService;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private BrandingDeployedEventConsumer consumer() {
        return new BrandingDeployedEventConsumer(objectMapper, landingPageService);
    }

    @Test
    @DisplayName("valid payload applies the theme + clears tenant context")
    void validPayloadApplies() {
        UUID tenant = UUID.randomUUID();
        when(landingPageService.applyDeployedBranding(eq(tenant), eq("#112233"), eq("#445566"),
                eq("https://cdn/logo.svg"), eq(2))).thenReturn(true);
        String json = "{\"tenantId\":\"" + tenant + "\",\"slug\":\"acme\","
                + "\"frontendUrl\":\"https://acme.kiteclass.vn\",\"primaryColor\":\"#112233\","
                + "\"secondaryColor\":\"#445566\",\"accentColor\":\"#778899\","
                + "\"logoUrl\":\"https://cdn/logo.svg\",\"brandingVersion\":2,"
                + "\"deployedAt\":\"2026-06-12T00:00:00Z\"}";

        consumer().handlePayload(json);

        verify(landingPageService).applyDeployedBranding(tenant, "#112233", "#445566",
                "https://cdn/logo.svg", 2);
        assertThat(TenantContext.isSet()).isFalse();
    }

    @Test
    @DisplayName("malformed JSON is swallowed — no service call, no throw")
    void malformedSwallowed() {
        assertThatCode(() -> consumer().handlePayload("{not-json")).doesNotThrowAnyException();
        verifyNoInteractions(landingPageService);
    }

    @Test
    @DisplayName("missing tenantId is dropped — no service call")
    void missingTenantDropped() {
        consumer().handlePayload("{\"slug\":\"acme\",\"primaryColor\":\"#112233\"}");
        verifyNoInteractions(landingPageService);
    }

    @Test
    @DisplayName("apply failure is ACKed (no throw) + tenant context cleared")
    void applyFailureAcked() {
        UUID tenant = UUID.randomUUID();
        when(landingPageService.applyDeployedBranding(eq(tenant), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("db down"));
        String json = "{\"tenantId\":\"" + tenant + "\",\"brandingVersion\":1}";

        assertThatCode(() -> consumer().handlePayload(json)).doesNotThrowAnyException();
        assertThat(TenantContext.isSet()).isFalse();
    }
}
