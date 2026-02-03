package com.kiteclass.gateway.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * R2DBC configuration for reactive database access.
 *
 * <p>Enables:
 * <ul>
 *   <li>R2DBC repositories</li>
 *   <li>Auditing for createdAt/updatedAt fields</li>
 * </ul>
 *
 * <p>Configured to run after R2DBC auto-configuration to ensure
 * r2dbcEntityTemplate bean is available.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.kiteclass.gateway")
@EnableR2dbcAuditing
@AutoConfigureAfter(R2dbcDataAutoConfiguration.class)
public class R2dbcConfig {
}
