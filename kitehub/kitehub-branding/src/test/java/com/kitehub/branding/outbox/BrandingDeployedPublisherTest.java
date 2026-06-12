package com.kitehub.branding.outbox;

import com.kitehub.branding.config.RabbitMQConfig;
import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.wizard.dto.BrandColours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BrandingDeployedPublisher} (GAP-1213 — branding.deployed propagation).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandingDeployedPublisher")
class BrandingDeployedPublisherTest {

    @Mock
    private BrandingEventEmitter eventEmitter;

    @Mock
    private BrandingInstanceStateRepository stateRepository;

    private BrandingColoursFixture fixture() {
        return new BrandingColoursFixture();
    }

    static class BrandingColoursFixture {
        final BrandColours colours = new BrandColours(
                "#112233", "#445566", "#778899", "#0F172A", "#FFFFFF", BrandColours.Source.TEMPLATE);
    }

    @Test
    @DisplayName("emits branding.deployed to the branding.events topic exchange with theme payload")
    void emitsDeployedEvent() {
        BrandingDeployedPublisher publisher = new BrandingDeployedPublisher(eventEmitter, stateRepository);
        UUID instanceId = UUID.randomUUID();
        when(stateRepository.findById(instanceId)).thenReturn(Optional.of(
                BrandingInstanceState.builder().instanceId(instanceId).brandingVersion(3).build()));

        publisher.publishDeployed(instanceId, "acme", "https://acme.kitehub.me",
                fixture().colours, "https://cdn/logo.svg");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventEmitter).emit(
                eq(instanceId), eq(instanceId), eq("branding.deployed"),
                eq(RabbitMQConfig.BRANDING_EVENTS_EXCHANGE),
                eq(RabbitMQConfig.BRANDING_DEPLOYED_ROUTING_KEY),
                payload.capture());

        assertThat(payload.getValue()).isInstanceOf(BrandingDeployedEvent.class);
        BrandingDeployedEvent event = (BrandingDeployedEvent) payload.getValue();
        assertThat(event.tenantId()).isEqualTo(instanceId.toString());
        assertThat(event.slug()).isEqualTo("acme");
        assertThat(event.frontendUrl()).isEqualTo("https://acme.kitehub.me");
        assertThat(event.primaryColor()).isEqualTo("#112233");
        assertThat(event.secondaryColor()).isEqualTo("#445566");
        assertThat(event.accentColor()).isEqualTo("#778899");
        assertThat(event.logoUrl()).isEqualTo("https://cdn/logo.svg");
        assertThat(event.brandingVersion()).isEqualTo(3);
        assertThat(event.deployedAt()).isNotBlank();
    }

    @Test
    @DisplayName("defaults brandingVersion to 1 when no instance-state row exists")
    void defaultsVersionWhenStateMissing() {
        BrandingDeployedPublisher publisher = new BrandingDeployedPublisher(eventEmitter, stateRepository);
        UUID instanceId = UUID.randomUUID();
        when(stateRepository.findById(instanceId)).thenReturn(Optional.empty());

        publisher.publishDeployed(instanceId, "acme", "https://acme.kitehub.me",
                fixture().colours, null);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventEmitter).emit(any(), any(), any(), any(), any(), payload.capture());
        assertThat(((BrandingDeployedEvent) payload.getValue()).brandingVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("null instanceId is a no-op (never emits)")
    void nullInstanceIsNoOp() {
        BrandingDeployedPublisher publisher = new BrandingDeployedPublisher(eventEmitter, stateRepository);
        lenient().when(stateRepository.findById(any())).thenReturn(Optional.empty());

        publisher.publishDeployed(null, "acme", "url", fixture().colours, null);

        verify(eventEmitter, org.mockito.Mockito.never())
                .emit(any(), any(), any(), any(), any(), any());
    }
}
