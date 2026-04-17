package com.kiteclass.core.common.config;

import com.kiteclass.core.common.context.TenantContext;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

/**
 * Custom cache key generator that includes tenant ID for multi-tenant isolation.
 *
 * <p>This generator ensures that cached data is isolated per tenant by prefixing
 * cache keys with the current tenant ID from {@link TenantContext}.
 *
 * <p>Key format: {@code "tenantId:methodParams"}
 * <p>Example: {@code "123e4567-e89b-12d3-a456-426614174000:[42]"}
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @Cacheable(value = "students", keyGenerator = "multiTenantKeyGenerator")
 * public StudentResponse getStudentById(Long id) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @author KiteClass Team
 * @since 2.14.1
 */
@Component("multiTenantKeyGenerator")
public class MultiTenantKeyGenerator implements KeyGenerator {

    /**
     * Generates cache key with tenant ID prefix.
     *
     * @param target the target instance
     * @param method the method being called
     * @param params the method parameters
     * @return cache key in format "tenantId:params"
     */
    @Override
    public Object generate(Object target, Method method, Object... params) {
        // Check if tenant context is set (e.g., background jobs may not have tenant context)
        UUID tenantId = TenantContext.isSet() ? TenantContext.getCurrentTenant() : null;

        // If no tenant context (e.g., background jobs), use "global" prefix
        String tenantPrefix = (tenantId != null) ? tenantId.toString() : "global";

        // Combine tenant ID with method parameters
        return tenantPrefix + ":" + Arrays.toString(params);
    }
}
