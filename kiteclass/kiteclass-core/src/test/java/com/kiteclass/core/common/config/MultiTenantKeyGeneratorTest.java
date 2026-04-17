package com.kiteclass.core.common.config;

import com.kiteclass.core.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultiTenantKeyGenerator}.
 *
 * @author KiteClass Team
 * @since 2.14.1
 */
class MultiTenantKeyGeneratorTest {

    private final MultiTenantKeyGenerator keyGenerator = new MultiTenantKeyGenerator();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should generate key with tenant ID prefix")
    void shouldGenerateKeyWithTenantIdPrefix() throws NoSuchMethodException {
        // Given
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        Method method = String.class.getMethod("valueOf", Object.class);
        Object[] params = {42L};

        // When
        Object cacheKey = keyGenerator.generate(this, method, params);

        // Then
        assertThat(cacheKey).isNotNull();
        assertThat(cacheKey.toString()).startsWith(tenantId.toString() + ":");
        assertThat(cacheKey.toString()).contains("[42]");
    }

    @Test
    @DisplayName("Should generate different keys for different tenants with same params")
    void shouldGenerateDifferentKeysForDifferentTenants() throws NoSuchMethodException {
        // Given
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        Method method = String.class.getMethod("valueOf", Object.class);
        Object[] params = {42L};

        // When - Tenant 1
        TenantContext.setCurrentTenant(tenant1);
        Object key1 = keyGenerator.generate(this, method, params);

        // When - Tenant 2
        TenantContext.clear();
        TenantContext.setCurrentTenant(tenant2);
        Object key2 = keyGenerator.generate(this, method, params);

        // Then
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.toString()).startsWith(tenant1.toString());
        assertThat(key2.toString()).startsWith(tenant2.toString());
    }

    @Test
    @DisplayName("Should generate same key for same tenant and same params")
    void shouldGenerateSameKeyForSameTenantAndSameParams() throws NoSuchMethodException {
        // Given
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        Method method = String.class.getMethod("valueOf", Object.class);
        Object[] params = {42L};

        // When
        Object key1 = keyGenerator.generate(this, method, params);
        Object key2 = keyGenerator.generate(this, method, params);

        // Then
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("Should generate different keys for same tenant with different params")
    void shouldGenerateDifferentKeysForDifferentParams() throws NoSuchMethodException {
        // Given
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        Method method = String.class.getMethod("valueOf", Object.class);
        Object[] params1 = {42L};
        Object[] params2 = {99L};

        // When
        Object key1 = keyGenerator.generate(this, method, params1);
        Object key2 = keyGenerator.generate(this, method, params2);

        // Then
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.toString()).contains("[42]");
        assertThat(key2.toString()).contains("[99]");
    }

    @Test
    @DisplayName("Should handle no tenant context (background jobs)")
    void shouldHandleNoTenantContext() throws NoSuchMethodException {
        // Given - No tenant context set
        Method method = String.class.getMethod("valueOf", Object.class);
        Object[] params = {42L};

        // When
        Object cacheKey = keyGenerator.generate(this, method, params);

        // Then
        assertThat(cacheKey).isNotNull();
        assertThat(cacheKey.toString()).startsWith("global:");
        assertThat(cacheKey.toString()).contains("[42]");
    }

    @Test
    @DisplayName("Should handle multiple parameters")
    void shouldHandleMultipleParameters() throws NoSuchMethodException {
        // Given
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        Method method = String.class.getMethod("valueOf", Object.class);
        Object[] params = {42L, "test", true};

        // When
        Object cacheKey = keyGenerator.generate(this, method, params);

        // Then
        assertThat(cacheKey.toString()).startsWith(tenantId.toString() + ":");
        assertThat(cacheKey.toString()).contains("42");
        assertThat(cacheKey.toString()).contains("test");
        assertThat(cacheKey.toString()).contains("true");
    }
}
