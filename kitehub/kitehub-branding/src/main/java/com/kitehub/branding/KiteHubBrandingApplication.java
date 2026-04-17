package com.kitehub.branding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application for KiteHub Branding Service.
 * AI-powered branding generation using OpenAI GPT-4 Vision and DALL-E 3.
 * <p>
 * Features:
 * - RabbitMQ job queue for async processing
 * - JPA for BrandingJob persistence
 * - OpenAI API integration
 * - S3 asset storage
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class KiteHubBrandingApplication {

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KiteHubBrandingApplication.class, args);
    }
}
