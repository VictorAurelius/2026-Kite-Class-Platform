package com.kitehub.subscription.config;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MultiTenantDataSourceConfig.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MultiTenantDataSourceConfig Unit Tests")
class MultiTenantDataSourceConfigTest {

    @Mock
    private InstanceRepository instanceRepository;

    @InjectMocks
    private MultiTenantDataSourceConfig dataSourceConfig;

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
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5433/kiteclass_test");
        instance.setDatabaseUsername("kiteclass_test_user");
        instance.setDatabasePassword("test_password");
    }

    @Test
    @DisplayName("Should throw exception when instance not found")
    void shouldThrowExceptionWhenInstanceNotFound() {
        // Given
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dataSourceConfig.getDataSource(instanceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Instance not found");
    }

    @Test
    @DisplayName("Should throw exception when database not provisioned")
    void shouldThrowExceptionWhenDatabaseNotProvisioned() {
        // Given
        instance.setDatabaseUrl(null);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When & Then
        assertThatThrownBy(() -> dataSourceConfig.getDataSource(instanceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Database not provisioned");
    }

    @Test
    @DisplayName("Should throw exception when database URL is empty")
    void shouldThrowExceptionWhenDatabaseUrlEmpty() {
        // Given
        instance.setDatabaseUrl("");
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        // When & Then
        assertThatThrownBy(() -> dataSourceConfig.getDataSource(instanceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Database not provisioned");
    }

    @Test
    @DisplayName("Should close DataSource successfully")
    void shouldCloseDataSourceSuccessfully() {
        // Given - No setup needed

        // When
        dataSourceConfig.closeDataSource(instanceId);

        // Then
        // Should complete without exception even if DataSource doesn't exist
        assertThat(dataSourceConfig.getActivePoolCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return zero active pools initially")
    void shouldReturnZeroActivePoolsInitially() {
        // When
        int activePoolCount = dataSourceConfig.getActivePoolCount();

        // Then
        assertThat(activePoolCount).isZero();
    }

    @Test
    @DisplayName("Should close all DataSources")
    void shouldCloseAllDataSources() {
        // When
        dataSourceConfig.closeAllDataSources();

        // Then
        assertThat(dataSourceConfig.getActivePoolCount()).isZero();
    }
}
