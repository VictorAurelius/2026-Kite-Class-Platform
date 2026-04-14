package com.kiteclass.core.module.branding.classifier;

import com.kiteclass.core.module.branding.entity.ResourceType;
import lombok.Builder;
import lombok.Value;

/**
 * Input to the classification chain.
 *
 * <p>Immutable value object — no domain mutation during classification.
 *
 * @since 3.16.0 (GAP-007)
 */
@Value
@Builder
public class ResourceRequest {

    ResourceType type;

    /**
     * True if the tenant explicitly asked for a custom (AI) generation rather than template.
     */
    boolean customRequested;
}
