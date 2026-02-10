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
    // R2DBC transaction manager is auto-configured by Spring Boot
    // TestContainersConfiguration provides @Primary transaction manager for tests
}
