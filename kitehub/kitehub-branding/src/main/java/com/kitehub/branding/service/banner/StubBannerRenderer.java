package com.kitehub.branding.service.banner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default {@link BannerRenderer} — the <b>render seam stub</b> (GAP-1117).
 *
 * <p>This implementation deliberately does NOT rasterise the composed HTML. It
 * logs that a real banner HTML was composed and returns {@code null}, signalling
 * the caller ({@code AIBrandingProcessor}) to fall back to the uploaded logo /
 * template placeholder for the banner image while still persisting the composed
 * HTML for transparency.</p>
 *
 * <p><b>What is real vs stubbed:</b> the HTML composition is fully real
 * (deterministic, crisp Vietnamese). Only the HTML → image rasterisation is
 * stubbed, awaiting the Playwright-runtime decision. Wire a Playwright sidecar /
 * Node renderer impl with higher {@code @Order}/{@code @Primary} to activate the
 * real WebP path — no pipeline change required.</p>
 *
 * @since GAP-1117
 */
@Slf4j
@Component
public class StubBannerRenderer implements BannerRenderer {

    @Override
    public String render(BannerComposition composition, UUID instanceId) {
        log.info("[banner][stub] composed banner HTML ({}x{}, {} chars) for instance {} — "
                        + "Playwright rasteriser not wired; caller falls back to logo/placeholder",
                composition.width(), composition.height(),
                composition.html() == null ? 0 : composition.html().length(), instanceId);
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
