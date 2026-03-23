package com.kitehub.branding.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for Branding Service.
 *
 * @since 1.1.0
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "KiteHub Branding Service API",
        version = "1.0.0",
        description = "AI branding, asset storage, content generation, and branding job management APIs",
        contact = @Contact(name = "KiteHub Team")
    )
)
public class OpenApiConfig {
}
