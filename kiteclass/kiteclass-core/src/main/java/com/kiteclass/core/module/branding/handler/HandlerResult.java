package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.branding.entity.BrandingResource;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import lombok.Builder;
import lombok.Value;

/**
 * Outcome of a {@link ResourceHandler#handle} call.
 *
 * <p>Three states:
 * <ul>
 *   <li>{@code READY} — resource already persisted + asset URL available</li>
 *   <li>{@code PENDING} — heavy job enqueued; {@code jobId} tracks async completion</li>
 *   <li>{@code FALLBACK} — handler couldn't produce; routing should move to fallback chain link</li>
 * </ul>
 *
 * @since 3.19.0 (Wave 3 Sub-PR 3.3)
 */
@Value
@Builder
public class HandlerResult {

    public enum Status { READY, PENDING, FALLBACK }

    Status status;
    ResourceCategory category;
    BrandingResource resource;
    String jobId;
    String message;

    public static HandlerResult ready(ResourceCategory category, BrandingResource resource) {
        return HandlerResult.builder()
                .status(Status.READY).category(category).resource(resource).build();
    }

    public static HandlerResult pending(ResourceCategory category, String jobId) {
        return HandlerResult.builder()
                .status(Status.PENDING).category(category).jobId(jobId).build();
    }

    public static HandlerResult fallback(String message) {
        return HandlerResult.builder()
                .status(Status.FALLBACK).message(message).build();
    }
}
