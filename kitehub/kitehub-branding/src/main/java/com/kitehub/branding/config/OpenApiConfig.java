package com.kitehub.branding.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for Branding Service.
 *
 * <p>Explicit bean name ({@code "brandingOpenApiConfig"}) — see GAP-147 for
 * why every {@code OpenApiConfig} in the KiteHub multi-module build declares
 * an explicit name instead of relying on the default {@code "openApiConfig"}.
 *
 * @since 1.1.0
 */
@Configuration("brandingOpenApiConfig")
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
