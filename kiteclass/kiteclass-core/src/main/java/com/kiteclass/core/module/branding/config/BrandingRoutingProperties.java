package com.kiteclass.core.module.branding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for branding resource routing (GAP-106, ADR-005).
 *
 * <p>Binds to the {@code branding.routing} prefix in {@code application.yml}.
 * Externalizes the template-first routing flag and the FULL_AI share alert
 * threshold that were previously only documented in
 * {@code documents/01-business/kiteclass/resource-classification/rules.md}
 * but missing from configuration (see GAP-106).
 *
 * <p>Rules governed:
 * <ul>
 *   <li>BR-RES-005 — Template-first routing: ≥80% of requests should resolve
 *       to STATIC or TEMPLATE. Disabling {@link #templateFirst} is reserved
 *       for debug / load-test scenarios where teams want the AI path
 *       exercised directly.</li>
 *   <li>{@link #maxAiRatio} — alert threshold for the
 *       {@code branding.routing.ai_ratio} Micrometer gauge emitted by
 *       {@link com.kiteclass.core.module.branding.service.ResourceRoutingService}.</li>
 * </ul>
 *
 * @since 3.21.0 (GAP-106, Wave 9-D)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "branding.routing")
public class BrandingRoutingProperties {

    /**
     * When {@code true} (default), the classifier chain enforces the template-first
     * philosophy — {@code TemplateMatchClassifier} runs before {@code AIFallback}.
     * Setting to {@code false} bypasses the template preference, letting the
     * AI fallback win early. Intended for debugging and load tests only.
     */
    private boolean templateFirst = true;

    /**
     * Metric alert threshold: maximum allowed share of requests routed to
     * {@code FULL_AI}. Expressed as a fraction (0.20 = 20%). When the
     * Micrometer gauge exceeds this value, a Grafana / Prometheus alert fires.
     * Default 0.20 matches BR-RES-005 (≤20% FULL_AI share).
     */
    private double maxAiRatio = 0.20;
}
