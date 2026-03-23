package com.kitehub.admin.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for Admin Service.
 *
 * @since 1.1.0
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "KiteHub Admin Service API",
        version = "1.0.0",
        description = "Admin portal APIs for platform management, analytics, and payment administration",
        contact = @Contact(name = "KiteHub Team")
    )
)
public class OpenApiConfig {
}
