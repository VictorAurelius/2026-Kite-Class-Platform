package com.kitehub.subscription.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for Subscription Service.
 *
 * @since 1.1.0
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "KiteHub Subscription Service API",
        version = "1.0.0",
        description = "Authentication, instance provisioning, subscription management, and payment APIs",
        contact = @Contact(name = "KiteHub Team")
    )
)
public class OpenApiConfig {
}
