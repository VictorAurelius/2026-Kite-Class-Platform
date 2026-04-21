package com.kitehub.email.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for Email Service.
 *
 * <p>Explicit bean name ({@code "emailOpenApiConfig"}) — see GAP-147 for why
 * every {@code OpenApiConfig} in the KiteHub multi-module build declares an
 * explicit name instead of relying on the default {@code "openApiConfig"}.
 *
 * @since 1.1.0
 */
@Configuration("emailOpenApiConfig")
@OpenAPIDefinition(
    info = @Info(
        title = "KiteHub Email Service API",
        version = "1.0.0",
        description = "Email notification service with SMTP/SES support and Thymeleaf templates",
        contact = @Contact(name = "KiteHub Team")
    )
)
public class OpenApiConfig {
}
