package com.kiteclass.core.module.legal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for proactive trademark scaffold (ADR-012 Track 1).
 *
 * <p>Loaded from {@code application.yml} under {@code legal.trademark} prefix. Seed list only —
 * tenant-level overrides and USPTO-API integration are deferred to a later wave.
 *
 * <pre>
 * legal:
 *   trademark:
 *     banned-keywords:
 *       - "Nike"
 *       - "Adidas"
 * </pre>
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "legal.trademark")
public class TrademarkProperties {

    /** Seed list of banned keywords; case-insensitive match at scan time. */
    private List<String> bannedKeywords = new ArrayList<>();
}
