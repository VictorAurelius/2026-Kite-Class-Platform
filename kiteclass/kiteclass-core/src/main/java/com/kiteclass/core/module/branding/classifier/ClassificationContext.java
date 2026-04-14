package com.kiteclass.core.module.branding.classifier;

import lombok.Builder;
import lombok.Value;

/**
 * Signals consulted by the classification chain.
 *
 * <p>Populated by {@code ResourceRoutingService} before running the chain — keeps
 * classifiers pure (no side effects / no DB calls inside each classifier).
 *
 * @since 3.16.0 (GAP-007)
 */
@Value
@Builder
public class ClassificationContext {

    boolean hasStaticAsset;

    boolean hasMatchingTemplate;

    boolean hasAIQuota;
}
