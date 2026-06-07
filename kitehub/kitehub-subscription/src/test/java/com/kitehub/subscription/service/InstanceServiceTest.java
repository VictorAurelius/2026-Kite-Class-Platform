package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.DatabaseCredentials;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.RegisterInstanceRequest;
import com.kitehub.subscription.dto.RegisterInstanceResponse;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private com.kitehub.subscription.config.MultiTenantDataSourceConfig dataSourceConfig;

    @Mock
    private TokenService tokenService;

    @Mock
    private TrialConfig trialConfig;

    @Mock
    private com.kitehub.subscription.client.EmailServiceClient emailServiceClient;

    @Mock
    private com.kitehub.subscription.tenant.TenantSlugNormalizer tenantSlugNormalizer;

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

        // Mock trial config
        lenient().when(trialConfig.getDurationDays()).thenReturn(14);

        // Mock database provisioning service to return test credentials (lenient for tests that don't use it)
        DatabaseCredentials mockCredentials = DatabaseCredentials.builder()
            .databaseUrl("jdbc:postgresql://localhost:5433/kiteclass_test")
            .username("kiteclass_test_user")
            .password("test_password")
            .build();
        lenient().when(databaseProvisioningService.provisionDatabase(any(UUID.class)))
            .thenReturn(mockCredentials);

        // GAP-823 Wave local-doable-9 Bucket B: stub TenantSlugNormalizer so
        // generateUniqueSlug() returns a non-empty slug (default Mock returns null
        // → IllegalArgumentException). Tests that don't exercise slug logic still need
        // this stub because createTrialInstance/createPendingInstance/registerInstance
        // all now call generateUniqueSlug. Stub returns the organization name lowercased
        // + space→dash as a deterministic shortcut (real normalizer covered by IT).
        lenient().when(tenantSlugNormalizer.normalize(anyString())).thenAnswer(inv -> {
            String input = inv.getArgument(0);
            return input == null ? "" : input.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+", "").replaceAll("-+$", "");
        });
        lenient().when(tenantSlugNormalizer.withCollisionSuffix(anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(inv -> inv.getArgument(0) + "-" + inv.getArgument(1));
        // Default: no slug collision. Tests can override per-case.
        lenient().when(instanceRepository.existsBySlugAndDeletedFalse(anyString())).thenReturn(false);
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
    @DisplayName("GAP-946: DB provisioning failure propagates (no silent pending-cred swallow)")
    void shouldPropagateWhenDatabaseProvisioningFails() {
        // Given — subdomain free, instance saves, but provisionDatabase throws (prod
        // lifecycleEnabled real failure). Pre-GAP-946 this was swallowed leaving a row
        // with databaseUrl='pending'; now it must propagate so @Transactional rolls back.
        when(instanceRepository.existsBySubdomainAndDeletedFalse(validRequest.getSubdomain())).thenReturn(false);
        Instance savedInstance = new Instance();
        savedInstance.setId(UUID.randomUUID());
        when(instanceRepository.save(any(Instance.class))).thenReturn(savedInstance);
        when(databaseProvisioningService.provisionDatabase(any(UUID.class)))
                .thenThrow(new IllegalStateException("RDS CREATE DATABASE failed"));

        // When / Then — exception propagates (not swallowed)
        assertThatThrownBy(() -> instanceService.createTrialInstance(validRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RDS CREATE DATABASE failed");
    }

    @Test
    @DisplayName("GAP-946 fail-loud: provisioning silent no-op (databaseUrl still 'pending') aborts creation")
    void shouldFailLoudWhenDatabaseUrlStillPendingAfterProvision() {
        // Given — subdomain free, instance saves, provisionDatabase returns WITHOUT throwing
        // but leaves databaseUrl on the 'pending' placeholder (a silent no-op / early-return
        // bug in the provisioning service). Defense-in-depth assertDatabaseProvisioned() must
        // catch this and throw so @Transactional rolls back — no half-provisioned tenant row.
        when(instanceRepository.existsBySubdomainAndDeletedFalse(validRequest.getSubdomain())).thenReturn(false);
        Instance savedInstance = new Instance();
        savedInstance.setId(UUID.randomUUID());
        savedInstance.setDatabaseUrl("pending"); // provisionDatabase (mocked) did NOT update it
        when(instanceRepository.save(any(Instance.class))).thenReturn(savedInstance);
        // databaseProvisioningService.provisionDatabase stubbed lenient in setUp() — returns
        // credentials without throwing and without mutating savedInstance (the no-op scenario).

        // When / Then — fail-loud assertion fires
        assertThatThrownBy(() -> instanceService.createTrialInstance(validRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Database provisioning did not complete");
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

    @Test
    @DisplayName("updateInstance should persist notification preferences (GAP-098)")
    void updateInstance_persistsNotificationPreferences() {
        // Given
        UUID id = UUID.randomUUID();
        Instance instance = new Instance();
        instance.setId(id);
        instance.setSubdomain("test");
        instance.setOrganizationName("Test");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setDatabaseUrl("test");
        instance.setDatabaseUsername("test");
        instance.setDatabasePassword("test");
        instance.setEmailNotifications(true);
        instance.setTrialReminders(true);
        instance.setDeleted(false);

        com.kitehub.subscription.dto.UpdateInstanceRequest request =
            com.kitehub.subscription.dto.UpdateInstanceRequest.builder()
                .emailNotifications(false)
                .trialReminders(false)
                .build();

        when(instanceRepository.findById(id)).thenReturn(java.util.Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        InstanceResponse response = instanceService.updateInstance(id, request);

        // Then
        assertThat(response.getEmailNotifications()).isFalse();
        assertThat(response.getTrialReminders()).isFalse();

        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(captor.capture());
        assertThat(captor.getValue().isEmailNotifications()).isFalse();
        assertThat(captor.getValue().isTrialReminders()).isFalse();
    }

    @Test
    @DisplayName("updateInstance should not change preferences when fields are null (GAP-098)")
    void updateInstance_nullFieldsPreserveExistingPreferences() {
        // Given
        UUID id = UUID.randomUUID();
        Instance instance = new Instance();
        instance.setId(id);
        instance.setSubdomain("test2");
        instance.setOrganizationName("Test2");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);
        instance.setStatus(InstanceStatus.ACTIVE);
        instance.setDatabaseUrl("test");
        instance.setDatabaseUsername("test");
        instance.setDatabasePassword("test");
        instance.setEmailNotifications(false); // already opted out
        instance.setTrialReminders(true);
        instance.setDeleted(false);

        com.kitehub.subscription.dto.UpdateInstanceRequest request =
            com.kitehub.subscription.dto.UpdateInstanceRequest.builder()
                .organizationName("Only Rename")
                .build();

        when(instanceRepository.findById(id)).thenReturn(java.util.Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        InstanceResponse response = instanceService.updateInstance(id, request);

        // Then — preferences unchanged
        assertThat(response.getEmailNotifications()).isFalse();
        assertThat(response.getTrialReminders()).isTrue();
    }

    @Nested
    @DisplayName("registerInstance — re-trial prevention (TR-07)")
    class RegisterInstanceReTrial {

        private RegisterInstanceRequest validRegisterRequest;

        @BeforeEach
        void setUpRegisterRequest() {
            validRegisterRequest = RegisterInstanceRequest.builder()
                .subdomain("new-school")
                .organizationName("New School")
                .ownerEmail("owner@example.com")
                .ownerPassword("password123")
                .build();
        }

        @Test
        @DisplayName("should reject re-trial when email already used trial")
        void registerInstance_shouldRejectReTrial() {
            // Given: email already used trial
            when(instanceRepository.existsBySubdomainAndDeletedFalse("new-school"))
                .thenReturn(false);
            when(instanceRepository.existsByContactEmailAndDeletedFalse("owner@example.com"))
                .thenReturn(false);
            when(instanceRepository.existsByContactEmailAndTrialStartedAtIsNotNull("owner@example.com"))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> instanceService.registerInstance(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chỉ được dùng thử 1 lần");

            verify(instanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow first trial registration")
        void registerInstance_shouldAllowFirstTrial() {
            // Given: email never used trial
            when(instanceRepository.existsBySubdomainAndDeletedFalse("new-school"))
                .thenReturn(false);
            when(instanceRepository.existsByContactEmailAndDeletedFalse("owner@example.com"))
                .thenReturn(false);
            when(instanceRepository.existsByContactEmailAndTrialStartedAtIsNotNull("owner@example.com"))
                .thenReturn(false);

            Instance savedInstance = new Instance();
            savedInstance.setId(UUID.randomUUID());
            savedInstance.setSubdomain("new-school");
            savedInstance.setOrganizationName("New School");
            savedInstance.setOwnerId(UUID.randomUUID());
            savedInstance.setContactEmail("owner@example.com");
            savedInstance.setTier(PricingTier.FREE);
            savedInstance.setStatus(InstanceStatus.TRIAL);
            savedInstance.setTrialStartedAt(LocalDateTime.now());
            savedInstance.setTrialExpiresAt(LocalDateTime.now().plusDays(14));
            // GAP-946: reflect post-provision reality — provisionDatabase moves databaseUrl
            // off the "pending" placeholder; assertDatabaseProvisioned() now enforces this.
            savedInstance.setDatabaseUrl("jdbc:postgresql://localhost:5433/kiteclass_new_school");
            savedInstance.setDatabaseUsername("kiteclass_new_school_user");
            savedInstance.setDatabasePassword("encrypted_pass");
            savedInstance.setCreatedAt(LocalDateTime.now());
            savedInstance.setUpdatedAt(LocalDateTime.now());

            when(instanceRepository.save(any(Instance.class))).thenReturn(savedInstance);
            when(tokenService.generateAccessToken(any(UUID.class), anyString(), anyString()))
                .thenReturn("mock-access-token");
            when(tokenService.generateRefreshToken(any(UUID.class)))
                .thenReturn("mock-refresh-token");

            // When
            RegisterInstanceResponse response = instanceService.registerInstance(validRegisterRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
            assertThat(response.getInstance().getSubdomain()).isEqualTo("new-school");

            verify(instanceRepository).save(any(Instance.class));
        }
    }

    @Nested
    @DisplayName("markProvisioned (GAP-945 — tenant.deployed PENDING → TRIAL)")
    class MarkProvisioned {

        @Test
        void markProvisioned_pendingInstance_transitionsToTrial() {
            UUID id = UUID.randomUUID();
            Instance pending = new Instance();
            pending.setId(id);
            pending.setStatus(InstanceStatus.PENDING);
            when(instanceRepository.findById(id)).thenReturn(java.util.Optional.of(pending));

            instanceService.markProvisioned(id);

            assertThat(pending.getStatus()).isEqualTo(InstanceStatus.TRIAL);
            assertThat(pending.getTrialStartedAt()).isNotNull();
            assertThat(pending.getTrialExpiresAt()).isNotNull();
            verify(instanceRepository).save(pending);
        }

        @Test
        void markProvisioned_alreadyTrial_isNoOp() {
            UUID id = UUID.randomUUID();
            Instance trial = new Instance();
            trial.setId(id);
            trial.setStatus(InstanceStatus.TRIAL);
            when(instanceRepository.findById(id)).thenReturn(java.util.Optional.of(trial));

            instanceService.markProvisioned(id);

            assertThat(trial.getStatus()).isEqualTo(InstanceStatus.TRIAL);
            verify(instanceRepository, never()).save(any(Instance.class));
        }

        @Test
        void markProvisioned_unknownInstance_doesNotThrow() {
            UUID id = UUID.randomUUID();
            when(instanceRepository.findById(id)).thenReturn(java.util.Optional.empty());

            org.assertj.core.api.Assertions.assertThatCode(() -> instanceService.markProvisioned(id))
                .doesNotThrowAnyException();

            verify(instanceRepository, never()).save(any(Instance.class));
        }
    }
}
