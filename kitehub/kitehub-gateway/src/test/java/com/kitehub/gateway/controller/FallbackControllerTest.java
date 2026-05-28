package com.kitehub.gateway.controller;

import com.kitehub.gateway.client.BrandingClient;
import com.kitehub.gateway.client.GatewayBranding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FallbackControllerTest {

    @Mock
    private BrandingClient brandingClient;

    private FallbackController controller;
    private ErrorPageRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ErrorPageRenderer();
        controller = new FallbackController(brandingClient, renderer);
    }

    @Test
    void subscriptionFallback_returnsBrandedHtml_withTenantColors() {
        GatewayBranding branding = GatewayBranding.builder()
                .displayName("ABC Center")
                .logoUrl("https://cdn.test/logo.png")
                .primaryColor("#112233")
                .secondaryColor("#445566")
                .build();
        when(brandingClient.fetch("tenant-abc")).thenReturn(branding);

        ResponseEntity<String> response = controller.subscriptionFallback("tenant-abc").block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("#112233"); // tenant primary color
        assertThat(body).contains("ABC Center");
        assertThat(body).contains("https://cdn.test/logo.png");
        assertThat(body).contains("Subscription service");
        verify(brandingClient).fetch("tenant-abc");
    }

    @Test
    void subscriptionFallback_falls_backToDefaultBranding_whenFetchFails() {
        when(brandingClient.fetch(null)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<String> response = controller.subscriptionFallback(null).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        String body = response.getBody();
        assertThat(body).isNotNull();
        // Default palette still rendered — no crash bubble through.
        assertThat(body).contains("#3B82F6");
    }

    @Test
    void notFound_returns_404_withBranding() {
        when(brandingClient.fetch("tenant-x"))
                .thenReturn(GatewayBranding.builder()
                        .displayName("X-Center")
                        .logoUrl("")
                        .primaryColor("#ABCDEF")
                        .secondaryColor("#123456")
                        .build());

        ResponseEntity<String> response = controller.notFound("tenant-x").block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("#ABCDEF").contains("X-Center");
    }

    @Test
    void serverError_returns_500_withBranding() {
        when(brandingClient.fetch("tenant-y"))
                .thenReturn(GatewayBranding.defaults());

        ResponseEntity<String> response = controller.serverError("tenant-y").block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("500");
    }
}
