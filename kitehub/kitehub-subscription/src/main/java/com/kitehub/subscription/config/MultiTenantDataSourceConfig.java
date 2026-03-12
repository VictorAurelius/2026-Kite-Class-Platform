package com.kitehub.subscription.config;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.repository.InstanceRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for multi-tenant database connection pooling.
 * Manages separate HikariCP connection pools for each KiteClass instance.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiTenantDataSourceConfig {

    private final InstanceRepository instanceRepository;
    private final com.kitehub.subscription.service.EncryptionService encryptionService;
    private final Map<UUID, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    // Connection pool size limits by pricing tier
    private static final int FREE_POOL_SIZE = 5;
    private static final int BASIC_POOL_SIZE = 10;
    private static final int PREMIUM_POOL_SIZE = 20;
    private static final int ENTERPRISE_POOL_SIZE = 50;

    /**
     * Get or create DataSource for instance.
     * Uses connection pooling with tier-based limits.
     *
     * @param instanceId UUID of the instance
     * @return DataSource for the instance
     * @throws IllegalArgumentException if instance not found or database not provisioned
     */
    public DataSource getDataSource(UUID instanceId) {
        return dataSources.computeIfAbsent(instanceId, this::createDataSource);
    }

    /**
     * Create new HikariCP DataSource for instance.
     *
     * @param instanceId UUID of the instance
     * @return Configured DataSource
     */
    private HikariDataSource createDataSource(UUID instanceId) {
        log.info("Creating DataSource for instance: {}", instanceId);

        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getDatabaseUrl() == null || instance.getDatabaseUrl().isEmpty()) {
            throw new IllegalArgumentException("Database not provisioned for instance: " + instanceId);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(instance.getDatabaseUrl());
        config.setUsername(instance.getDatabaseUsername());
        // Decrypt password for database connection
        String decryptedPassword = encryptionService.decrypt(instance.getDatabasePassword());
        config.setPassword(decryptedPassword);

        // Set pool size based on pricing tier
        int poolSize = getPoolSizeForTier(instance.getTier());
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Math.max(1, poolSize / 2)); // 50% of max as minimum idle

        // Connection timeout
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        // Pool name for monitoring
        config.setPoolName("KiteClass-" + instanceId.toString().substring(0, 8));

        // Connection test query
        config.setConnectionTestQuery("SELECT 1");

        log.info("DataSource created for instance {} with pool size: {}", instanceId, poolSize);

        return new HikariDataSource(config);
    }

    /**
     * Get connection pool size based on pricing tier.
     *
     * @param tier Pricing tier
     * @return Maximum pool size
     */
    private int getPoolSizeForTier(PricingTier tier) {
        return switch (tier) {
            case FREE -> FREE_POOL_SIZE;
            case BASIC -> BASIC_POOL_SIZE;
            case PREMIUM -> PREMIUM_POOL_SIZE;
            case ENTERPRISE -> ENTERPRISE_POOL_SIZE;
        };
    }

    /**
     * Close DataSource for instance.
     * Used when instance is deleted or suspended.
     *
     * @param instanceId UUID of the instance
     */
    public void closeDataSource(UUID instanceId) {
        HikariDataSource dataSource = dataSources.remove(instanceId);
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Closing DataSource for instance: {}", instanceId);
            dataSource.close();
        }
    }

    /**
     * Get total number of active connection pools.
     *
     * @return Number of active pools
     */
    public int getActivePoolCount() {
        return dataSources.size();
    }

    /**
     * Get total number of active connections across all pools.
     *
     * @return Total active connections
     */
    public int getTotalActiveConnections() {
        return dataSources.values().stream()
            .map(HikariDataSource::getHikariPoolMXBean)
            .mapToInt(pool -> pool.getActiveConnections())
            .sum();
    }

    /**
     * Close all DataSources.
     * Used during application shutdown.
     */
    public void closeAllDataSources() {
        log.info("Closing all DataSources ({} pools)", dataSources.size());
        dataSources.values().forEach(dataSource -> {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        });
        dataSources.clear();
    }
}
