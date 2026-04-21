package com.kitehub.subscription.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for Subscription Service.
 *
 * <p>Explicit bean name ({@code "subscriptionOpenApiConfig"}) prevents a
 * {@link org.springframework.context.annotation.ConflictingBeanDefinitionException}
 * when a composite app (e.g. {@code kitehub-admin}) scans
 * {@code com.kitehub.subscription} and {@code com.kitehub.admin} together —
 * both previously resolved to the default bean name {@code "openApiConfig"}
 * (GAP-147).
 *
 * @since 1.1.0
 */
@Configuration("subscriptionOpenApiConfig")
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
