package com.kitehub.subscription.feedback.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wires {@code kitehub.feedback.*} config keys documented in
 * {@code documents/01-business/kitehub/feedback/rules.md} §Config.
 *
 * <p>Closes GAP-555 Wave 79 Bucket A — 7 unwired feedback config keys (Wave 78
 * documented but never injected via {@code @Value}). Defaults match the rules.md
 * column verbatim so production overrides land via {@code application*.yml} per
 * {@code production-env-config-registry.md} v1.1.1.</p>
 *
 * <p>This component is a domain-config aggregator — Spring instantiates it once
 * and downstream services inject {@link FeedbackConfig} instead of duplicating
 * {@code @Value} annotations. Pattern: Strategy holder per {@code design-patterns.md}
 * §2 (single owner of feedback rule values).</p>
 *
 * @since Wave 79 Bucket A — GAP-555
 */
@Component
@Getter
public class FeedbackConfig {

    private final int ratingRangeMin;
    private final int ratingRangeMax;
    private final int commentMinChars;
    private final int commentMaxChars;
    private final int publicRateLimitPerMinPerIp;
    private final int authRateLimitPerMinPerUser;
    private final String categories;

    public FeedbackConfig(
            @Value("${kitehub.feedback.rating-range-min:1}") int ratingRangeMin,
            @Value("${kitehub.feedback.rating-range-max:5}") int ratingRangeMax,
            @Value("${kitehub.feedback.comment-min-chars:5}") int commentMinChars,
            @Value("${kitehub.feedback.comment-max-chars:2000}") int commentMaxChars,
            @Value("${kitehub.feedback.public-rate-limit-per-min-per-ip:10}") int publicRateLimitPerMinPerIp,
            @Value("${kitehub.feedback.auth-rate-limit-per-min-per-user:30}") int authRateLimitPerMinPerUser,
            @Value("${kitehub.feedback.categories:BUG,USABILITY,FEATURE_REQUEST,GENERAL}") String categories
    ) {
        this.ratingRangeMin = ratingRangeMin;
        this.ratingRangeMax = ratingRangeMax;
        this.commentMinChars = commentMinChars;
        this.commentMaxChars = commentMaxChars;
        this.publicRateLimitPerMinPerIp = publicRateLimitPerMinPerIp;
        this.authRateLimitPerMinPerUser = authRateLimitPerMinPerUser;
        this.categories = categories;
    }
}
