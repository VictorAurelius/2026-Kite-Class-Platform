package com.kitehub.email.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for Email Service.
 *
 * @since 1.1.0
 */
@Configuration
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
