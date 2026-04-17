package com.kiteclass.core.module.parent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables {@link ParentPortalProperties} so that services can inject it
 * without each having to declare {@code @EnableConfigurationProperties}.
 *
 * @since 2.14.0
 */
@Configuration
@EnableConfigurationProperties(ParentPortalProperties.class)
public class ParentPortalConfiguration {
}
