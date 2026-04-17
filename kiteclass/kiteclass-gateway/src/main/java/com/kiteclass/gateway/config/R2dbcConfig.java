package com.kiteclass.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import io.r2dbc.spi.ConnectionFactory;

/**
 * R2DBC configuration for reactive database access.
 *
 * <p>Enables:
 * <ul>
 *   <li>R2DBC auditing for createdAt/updatedAt fields</li>
 *   <li>Repositories are auto-detected by Spring Boot</li>
 *   <li>R2DBC transaction manager marked as @Primary (FlywayDataSource creates another TX manager)</li>
 * </ul>
 *
 * <p>Note: @EnableR2dbcRepositories is not needed - Spring Boot auto-configuration
 * handles repository scanning automatically.
 *
 * <p>This configuration is only active in non-test profiles. For tests,
 * {@link TestContainersConfiguration} provides the transaction manager.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Configuration
@EnableR2dbcAuditing
@Profile("!test")
public class R2dbcConfig {

    /**
     * R2DBC Transaction Manager marked as @Primary.
     *
     * <p>This is required because FlywayConfig creates a JDBC DataSource which triggers
     * auto-configuration of a DataSourceTransactionManager. We need to mark R2DBC's
     * transaction manager as @Primary so Spring knows which one to use by default.
     *
     * @param connectionFactory R2DBC connection factory
     * @return ReactiveTransactionManager
     */
    @Bean
    @Primary
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
