package com.kitehub.branding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Main application for KiteHub Branding Service.
 * AI-powered branding generation using OpenAI GPT-4 Vision and DALL-E 3.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class KiteHubBrandingApplication {

    public static void main(String[] args) {
        SpringApplication.run(KiteHubBrandingApplication.class, args);
    }
}
