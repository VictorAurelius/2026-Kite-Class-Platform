package com.kiteclass.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * R2DBC configuration for reactive database access.
 *
 * <p>Enables:
 * <ul>
 *   <li>R2DBC auditing for createdAt/updatedAt fields</li>
 *   <li>Repositories are auto-detected by Spring Boot</li>
 * </ul>
 *
 * <p>Note: @EnableR2dbcRepositories is not needed - Spring Boot auto-configuration
 * handles repository scanning automatically.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {

    /**
     * R2DBC transaction manager (primary).
     *
     * <p>Marks R2DBC transaction manager as primary when both JDBC (for Flyway)
     * and R2DBC transaction managers exist.
     *
     * @param connectionFactory R2DBC connection factory
     * @return R2DBC transaction manager
     */
    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    public org.springframework.r2dbc.connection.R2dbcTransactionManager transactionManager(
            io.r2dbc.spi.ConnectionFactory connectionFactory) {
        return new org.springframework.r2dbc.connection.R2dbcTransactionManager(connectionFactory);
    }
}
