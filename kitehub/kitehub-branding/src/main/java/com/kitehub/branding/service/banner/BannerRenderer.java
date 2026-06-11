package com.kitehub.branding.service.banner;

import java.util.UUID;

/**
 * Render seam (Strategy) for turning a composed {@link BannerComposition} HTML
 * into a rasterised image URL — the TEMPLATE-mode banner (GAP-1135).
 *
 * <p>The reference rasteriser is {@code compose-sky-demo-banner.mjs}
 * (HTML → headless Chromium/Playwright → WebP). Running Playwright inside the
 * JVM service is a deployment decision deferred to a follow-up; this interface
 * is the clean seam so a Playwright sidecar (or a Node renderer microservice)
 * can be wired without touching the generation pipeline.</p>
 *
 * <p>The default in-process implementation is {@link StubBannerRenderer}, which
 * does NOT rasterise (returns {@code null}); the caller then falls back to the
 * uploaded logo / template placeholder. The composed HTML is still persisted so
 * the work is real + inspectable.</p>
 *
 * @since GAP-1135
 */
public interface BannerRenderer {

    /**
     * Rasterise the composed HTML to an image and return its (storage/CDN) URL.
     *
     * @param composition composed HTML + dimensions
     * @param instanceId  owning instance (object-key namespacing)
     * @return rendered image URL, or {@code null} when no rasteriser is wired
     *         (caller falls back to a deterministic placeholder)
     */
    String render(BannerComposition composition, UUID instanceId);

    /** Whether this renderer actually rasterises (true) or is a stub seam (false). */
    boolean isAvailable();
}
