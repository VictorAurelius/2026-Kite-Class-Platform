package com.kiteclass.gateway.config;

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
 * @author KiteClass Team
 * @since 1.0.0
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.kiteclass.gateway")
@EnableR2dbcAuditing
public class R2dbcConfig {
}
