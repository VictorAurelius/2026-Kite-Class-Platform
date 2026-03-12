package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.DatabaseCredentials;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for DatabaseProvisioningService.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseProvisioningService Unit Tests")
class DatabaseProvisioningServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private DatabaseProvisioningService provisioningService;

    private UUID instanceId;
    private Instance instance;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();

        instance = new Instance();
        instance.setId(instanceId);
        instance.setSubdomain("test-org");
        instance.setOrganizationName("Test Organization");
        instance.setOwnerId(UUID.randomUUID());
        instance.setTier(PricingTier.BASIC);

        // Set master database properties via reflection
        ReflectionTestUtils.setField(provisioningService, "masterHost", "localhost");
        ReflectionTestUtils.setField(provisioningService, "masterPort", "5433");

        // Configure encryption mock behavior (lenient to avoid strict stubbing issues)
        // Encrypt: add "ENCRYPTED_" prefix
        lenient().when(encryptionService.encrypt(anyString()))
            .thenAnswer(invocation -> "ENCRYPTED_" + invocation.getArgument(0));

        // Decrypt: remove "ENCRYPTED_" prefix
        lenient().when(encryptionService.decrypt(anyString()))
            .thenAnswer(invocation -> {
                String encrypted = invocation.getArgument(0);
                return encrypted.startsWith("ENCRYPTED_")
                    ? encrypted.substring("ENCRYPTED_".length())
                    : encrypted;
            });
    }

    @Test
    @DisplayName("Should provision database successfully")
    void shouldProvisionDatabaseSuccessfully() {
        // Given
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        DatabaseCredentials credentials = provisioningService.provisionDatabase(instanceId);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.getDatabaseUrl()).contains("jdbc:postgresql://localhost:5433/kiteclass_");
        assertThat(credentials.getUsername()).startsWith("kiteclass_");
        assertThat(credentials.getPassword()).isNotEmpty();

        // Verify instance was updated
        ArgumentCaptor<Instance> instanceCaptor = ArgumentCaptor.forClass(Instance.class);
        verify(instanceRepository).save(instanceCaptor.capture());

        Instance savedInstance = instanceCaptor.getValue();
        assertThat(savedInstance.getDatabaseUrl()).isNotNull();
        assertThat(savedInstance.getDatabaseUsername()).isNotNull();
        assertThat(savedInstance.getDatabasePassword()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when instance not found")
    void shouldThrowExceptionWhenInstanceNotFound() {
        // Given
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> provisioningService.provisionDatabase(instanceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Instance not found");

        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return existing credentials when database already provisioned")
    void shouldReturnExistingCredentialsWhenAlreadyProvisioned() {
        // Given
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5433/kiteclass_abc123");
        instance.setDatabaseUsername("kiteclass_abc123_user");
        instance.setDatabasePassword("ENCRYPTED_existing_password"); // Stored encrypted

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        DatabaseCredentials credentials = provisioningService.provisionDatabase(instanceId);

        // Then
        assertThat(credentials.getDatabaseUrl()).isEqualTo("jdbc:postgresql://localhost:5433/kiteclass_abc123");
        assertThat(credentials.getUsername()).isEqualTo("kiteclass_abc123_user");
        assertThat(credentials.getPassword()).isEqualTo("existing_password"); // Decrypted

        // Should not save again
        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate unique database name")
    void shouldGenerateUniqueDatabaseName() {
        // Given
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenReturn(instance);

        // When
        DatabaseCredentials credentials = provisioningService.provisionDatabase(instanceId);

        // Then
        String dbName = credentials.getDatabaseUrl().split("/")[credentials.getDatabaseUrl().split("/").length - 1];
        assertThat(dbName).startsWith("kiteclass_");
        assertThat(dbName).hasSize(18); // "kiteclass_" (10) + 8 chars UUID
    }

    @Test
    @DisplayName("Should check database health")
    void shouldCheckDatabaseHealth() {
        // Given
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5433/kiteclass_abc123");
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        boolean isHealthy = provisioningService.checkDatabaseHealth(instanceId);

        // Then
        assertThat(isHealthy).isTrue(); // Simulated health check
    }

    @Test
    @DisplayName("Should return false when database not provisioned")
    void shouldReturnFalseWhenDatabaseNotProvisioned() {
        // Given
        instance.setDatabaseUrl(null);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        boolean isHealthy = provisioningService.checkDatabaseHealth(instanceId);

        // Then
        assertThat(isHealthy).isFalse();
    }

    @Test
    @DisplayName("Should delete database successfully")
    void shouldDeleteDatabaseSuccessfully() {
        // Given
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5433/kiteclass_abc123");
        instance.setDatabaseUsername("kiteclass_abc123_user");
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        provisioningService.deleteDatabase(instanceId);

        // Then
        // Should complete without exception (actual deletion is mocked)
        verify(instanceRepository).findById(instanceId);
    }

    @Test
    @DisplayName("Should handle deletion of instance with no database")
    void shouldHandleDeletionOfInstanceWithNoDatabase() {
        // Given
        instance.setDatabaseUrl(null);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When
        provisioningService.deleteDatabase(instanceId);

        // Then
        // Should complete without exception
        verify(instanceRepository).findById(instanceId);
    }
}
