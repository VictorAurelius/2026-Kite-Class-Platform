package com.kiteclass.core.module.branding.handler;

import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.entity.ResourceCategory;

/**
 * Strategy per {@link ResourceCategory} — produces (or enqueues) a branding asset.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link StaticResourceHandler} — STATIC (tenant-uploaded asset)</li>
 *   <li>{@link TemplateResourceHandler} — TEMPLATE (compose SVG + params)</li>
 *   <li>{@link AIResourceHandler} — FULL_AI (enqueue to job queue)</li>
 *   <li>{@link FallbackHandler} — emergency default (catch-all)</li>
 * </ul>
 *
 * <p>Per {@code ResourceRoutingService}, the classifier chain picks a category, then
 * the service selects the matching handler via {@link #supports()} and calls
 * {@link #handle}.
 *
 * @since 3.19.0 (Wave 3 Sub-PR 3.3)
 */
public interface ResourceHandler {

    ResourceCategory supports();

    HandlerResult handle(ResourceRequest request, ClassificationContext context);
}
