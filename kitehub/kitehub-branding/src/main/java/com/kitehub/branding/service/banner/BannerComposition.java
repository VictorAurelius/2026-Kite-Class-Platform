package com.kitehub.branding.service.banner;

/**
 * The composed, deterministic HTML for a TEMPLATE-mode banner plus its target
 * dimensions (GAP-1117). Produced by {@link BannerHtmlComposer}; consumed by a
 * {@link BannerRenderer} that rasterises it to an image (WebP) URL.
 *
 * <p>The HTML is the <em>real</em> artifact of the TEMPLATE path — crisp
 * Vietnamese, deterministic, $0 — mirroring the layout of
 * {@code kiteclass-frontend/scripts/compose-sky-demo-banner.mjs}.</p>
 *
 * @param html   full self-contained HTML document (fonts via CDN, images via URL/data-URI)
 * @param width  target render width in pixels
 * @param height target render height in pixels
 * @since GAP-1117
 */
public record BannerComposition(String html, int width, int height) {
}
