package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.DatabaseCredentials;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for InstanceService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstanceService Unit Tests")
class InstanceServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private DatabaseProvisioningService databaseProvisioningService;

    @InjectMocks
    private InstanceService instanceService;

    private CreateInstanceRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreateInstanceRequest.builder()
            .subdomain("test-org")
            .organizationName("Test Organization")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        // Mock database provisioning service to return test credentials (lenient for tests that don't use it)
        DatabaseCredentials mockCredentials = DatabaseCredentials.builder()
            .databaseUrl("jdbc:postgresql://localhost:5433/kiteclass_test")
            .username("kiteclass_test_user")
            .password("test_password")
            .build();
        lenient().when(databaseProvisioningService.provisionDatabase(any(UUID.class)))
            .thenReturn(mockCredentials);
    }

    @Test
    @DisplayName("Should create trial instance successfully")
    void shouldCreateTrialInstanceSuccessfully() {
        // Given
        when(instanceRepository.existsBySubdomainAndDeletedFalse(validRequest.getSubdomain())).thenReturn(false);

        Instance savedInstance = new Instance();
        savedInstance.setId(UUID.randomUUID());
        savedInstance.setSubdomain(validRequest.getSubdomain());
        savedInstance.setOrganizationName(validRequest.getOrganizationName());
        savedInstance.setOwnerId(validRequest.getOwnerId());
        savedInstance.setTier(validRequest.getTier());
        savedInstance.setStatus(InstanceStatus.TRIAL);
        savedInstance.setTrialStartedAt(LocalDateTime.now());
        savedInstance.setTrialExpiresAt(LocalDateTime.now().plusDays(14));
        savedInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/test");
        savedInstance.setDatabaseUsername("test_user");
        savedInstance.setDatabasePassword("encrypted_pass");
        savedInstance.setCreatedAt(LocalDateTime.now());
        savedInstance.setUpdatedAt(LocalDateTime.now());

        when(instanceRepository.save(any(Instance.class))).thenReturn(savedInstance);

        // When
        InstanceResponse response = instanceService.createTrialInstance(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubdomain()).isEqualTo(validRequest.getSubdomain());
        assertThat(response.getOrganizationName()).isEqualTo(validRequest.getOrganizationName());
        assertThat(response.getStatus()).isEqualTo(InstanceStatus.TRIAL);
        assertThat(response.getIsOnTrial()).isTrue();

        // Verify trial dates
        ArgumentCaptor<Instance> instanceCaptor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(instanceCaptor.capture());
        Instance captured = instanceCaptor.getValue();
        assertThat(captured.getTrialStartedAt()).isNotNull();
        assertThat(captured.getTrialExpiresAt()).isNotNull();
        assertThat(captured.getTrialExpiresAt()).isAfter(captured.getTrialStartedAt());
    }

    @Test
    @DisplayName("Should throw exception when subdomain already exists")
    void shouldThrowExceptionWhenSubdomainExists() {
        // Given
        when(instanceRepository.existsBySubdomainAndDeletedFalse(validRequest.getSubdomain())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> instanceService.createTrialInstance(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Subdomain already exists");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when custom domain used with FREE tier")
    void shouldThrowExceptionWhenCustomDomainWithFreeTier() {
        // Given
        validRequest.setTier(PricingTier.FREE);
        validRequest.setCustomDomain("custom.example.com");

        when(instanceRepository.existsBySubdomainAndDeletedFalse(validRequest.getSubdomain())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> instanceService.createTrialInstance(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Custom domain is only available for PREMIUM and ENTERPRISE");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject reserved subdomain 'admin' for trial instance")
    void shouldRejectReservedSubdomainForTrialInstance() {
        // Given
        validRequest.setSubdomain("admin");

        // When & Then
        assertThatThrownBy(() -> instanceService.createTrialInstance(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject reserved subdomain 'api' for trial instance")
    void shouldRejectReservedSubdomainApiForTrialInstance() {
        // Given
        validRequest.setSubdomain("api");

        // When & Then
        assertThatThrownBy(() -> instanceService.createTrialInstance(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject reserved subdomain case-insensitively")
    void shouldRejectReservedSubdomainCaseInsensitive() {
        // Given
        validRequest.setSubdomain("Admin");

        // When & Then
        assertThatThrownBy(() -> instanceService.createTrialInstance(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate trial days left correctly")
    void shouldCalculateTrialDaysLeftCorrectly() {
        // Given
        Instance instance = new Instance();
        instance.setId(UUID.randomUUID());
        instance.setStatus(InstanceStatus.TRIAL);
        instance.setTrialStartedAt(LocalDateTime.now().minusDays(7)); // Started 7 days ago
        instance.setTrialExpiresAt(LocalDateTime.now().plusDays(7));  // 7 days left
        instance.setSubdomain("test");
        instance.setOrganizationName("Test");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);
        instance.setDatabaseUrl("test");
        instance.setDatabaseUsername("test");
        instance.setDatabasePassword("test");
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        instance.setDeleted(false);

        when(instanceRepository.findById(instance.getId())).thenReturn(java.util.Optional.of(instance));

        // When
        InstanceResponse response = instanceService.getInstanceById(instance.getId());

        // Then
        assertThat(response.getTrialDaysLeft()).isGreaterThanOrEqualTo(6).isLessThanOrEqualTo(8); // Allow 1 day tolerance for timing
    }
}
