package com.kiteclass.core.module.marketing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables marketing-module configuration properties so the package's
 * {@code @ConfigurationProperties} beans (e.g. {@link LandingPageSafetyProperties})
 * bind without each declaring {@code @EnableConfigurationProperties}.
 *
 * @since wave-thesis-5 (GAP-827)
 */
@Configuration
@EnableConfigurationProperties(LandingPageSafetyProperties.class)
public class MarketingConfiguration {
}
