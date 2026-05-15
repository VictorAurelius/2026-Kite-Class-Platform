package com.kitehub.subscription.support.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wires {@code kitehub.support.*} config keys documented in
 * {@code documents/01-business/kitehub/support/rules.md} §Config.
 *
 * <p>Closes GAP-555 Wave 79 Bucket A — 10 unwired support config keys. Wave 78
 * shipped support DISCOVERABILITY ONLY (no controller/service yet — tracked
 * GAP-556 Bucket E for rules.md scope clarification); this config holder exists
 * so the keys are wired + grep-discoverable for the future controller PR.
 * Defaults match the rules.md column verbatim; production overrides land via
 * {@code application*.yml} per {@code production-env-config-registry.md} v1.1.1.</p>
 *
 * @since Wave 79 Bucket A — GAP-555
 */
@Component
@Getter
public class SupportConfig {

    private final int subjectMinChars;
    private final int subjectMaxChars;
    private final int bodyMinChars;
    private final int bodyMaxChars;
    private final int publicRateLimitPerMinPerIp;
    private final int authRateLimitPerMinPerUser;
    private final String categories;
    private final String priorities;
    private final String ticketNumberPrefix;
    private final int slaFirstResponseHours;

    public SupportConfig(
            @Value("${kitehub.support.subject-min-chars:5}") int subjectMinChars,
            @Value("${kitehub.support.subject-max-chars:200}") int subjectMaxChars,
            @Value("${kitehub.support.body-min-chars:10}") int bodyMinChars,
            @Value("${kitehub.support.body-max-chars:5000}") int bodyMaxChars,
            @Value("${kitehub.support.public-rate-limit-per-min-per-ip:5}") int publicRateLimitPerMinPerIp,
            @Value("${kitehub.support.auth-rate-limit-per-min-per-user:20}") int authRateLimitPerMinPerUser,
            @Value("${kitehub.support.categories:AUTH_ISSUE,BILLING,BUG,FEATURE_REQUEST,DATA_ISSUE,OTHER}") String categories,
            @Value("${kitehub.support.priorities:LOW,NORMAL,HIGH,URGENT}") String priorities,
            @Value("${kitehub.support.ticket-number-prefix:KH-}") String ticketNumberPrefix,
            @Value("${kitehub.support.sla-first-response-hours:24}") int slaFirstResponseHours
    ) {
        this.subjectMinChars = subjectMinChars;
        this.subjectMaxChars = subjectMaxChars;
        this.bodyMinChars = bodyMinChars;
        this.bodyMaxChars = bodyMaxChars;
        this.publicRateLimitPerMinPerIp = publicRateLimitPerMinPerIp;
        this.authRateLimitPerMinPerUser = authRateLimitPerMinPerUser;
        this.categories = categories;
        this.priorities = priorities;
        this.ticketNumberPrefix = ticketNumberPrefix;
        this.slaFirstResponseHours = slaFirstResponseHours;
    }
}
