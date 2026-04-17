package com.kitehub.subscription.controller;

import com.kitehub.subscription.config.DataRetentionConfig;
import com.kitehub.subscription.config.SubscriptionConfig;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.dto.PublicConfigResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PublicConfigController.
 * Verifies that the controller correctly maps config values into the response DTO.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PublicConfigControllerTest {

    @Mock
    private TrialConfig trialConfig;

    @Mock
    private SubscriptionConfig subscriptionConfig;

    @Mock
    private DataRetentionConfig dataRetentionConfig;

    @InjectMocks
    private PublicConfigController controller;

    @BeforeEach
    void setUp() {
        when(trialConfig.getDurationDays()).thenReturn(14);
        when(trialConfig.getMaxPerOwner()).thenReturn(1);
        when(subscriptionConfig.getGracePeriodDays()).thenReturn(3);
        when(dataRetentionConfig.getTrial()).thenReturn(7);
        when(dataRetentionConfig.getFree()).thenReturn(7);
        when(dataRetentionConfig.getBasic()).thenReturn(30);
        when(dataRetentionConfig.getPremium()).thenReturn(60);
        when(dataRetentionConfig.getEnterprise()).thenReturn(90);
    }

    @Test
    void shouldReturnOkStatus() {
        // When
        ResponseEntity<PublicConfigResponse> response = controller.getPublicConfig();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnTrialConfig() {
        // When
        ResponseEntity<PublicConfigResponse> response = controller.getPublicConfig();

        // Then
        PublicConfigResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTrialDays()).isEqualTo(14);
        assertThat(body.getTrialMaxPerOwner()).isEqualTo(1);
    }

    @Test
    void shouldReturnGracePeriod() {
        // When
        ResponseEntity<PublicConfigResponse> response = controller.getPublicConfig();

        // Then
        PublicConfigResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getGracePeriodDays()).isEqualTo(3);
    }

    @Test
    void shouldReturnRetentionDaysPerTier() {
        // When
        ResponseEntity<PublicConfigResponse> response = controller.getPublicConfig();

        // Then
        PublicConfigResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getRetentionDays())
            .containsEntry("trial", 7)
            .containsEntry("free", 7)
            .containsEntry("basic", 30)
            .containsEntry("premium", 60)
            .containsEntry("enterprise", 90)
            .hasSize(5);
    }

    @Test
    void shouldReflectCustomConfigValues() {
        // Given - override with custom values
        when(trialConfig.getDurationDays()).thenReturn(30);
        when(trialConfig.getMaxPerOwner()).thenReturn(3);
        when(subscriptionConfig.getGracePeriodDays()).thenReturn(7);
        when(dataRetentionConfig.getTrial()).thenReturn(14);

        // When
        ResponseEntity<PublicConfigResponse> response = controller.getPublicConfig();

        // Then
        PublicConfigResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTrialDays()).isEqualTo(30);
        assertThat(body.getTrialMaxPerOwner()).isEqualTo(3);
        assertThat(body.getGracePeriodDays()).isEqualTo(7);
        assertThat(body.getRetentionDays()).containsEntry("trial", 14);
    }
}
