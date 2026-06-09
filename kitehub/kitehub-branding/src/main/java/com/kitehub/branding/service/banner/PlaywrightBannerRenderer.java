package com.kitehub.branding.service.banner;

import com.kitehub.branding.service.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Real {@link BannerRenderer} — rasterises the composed banner HTML to WebP via
 * the {@code kitehub-banner-renderer} Playwright sidecar, then stores it and
 * returns its URL (GAP-1135 / GAP-1112).
 *
 * <p>Resolves the deployment decision the seam deferred: Chromium lives in a
 * dedicated Node sidecar, not in this JVM image. This bean POSTs the composed
 * HTML to {@code branding.banner.renderer-url} and uploads the returned WebP via
 * {@link S3StorageService}.</p>
 *
 * <p><b>Activation:</b> {@code @Primary} so it wins over {@link StubBannerRenderer}
 * whenever both beans exist. When {@code branding.banner.renderer-url} is blank
 * (production default until the sidecar is deployed there), this renderer behaves
 * exactly like the stub — logs + returns {@code null} so the caller falls back to
 * the uploaded logo / template placeholder. Setting {@code BANNER_RENDERER_URL}
 * (local compose) activates the real path. No pipeline change required.</p>
 *
 * @since GAP-1135
 */
@Slf4j
@Primary
@Component
public class PlaywrightBannerRenderer implements BannerRenderer {

    private final S3StorageService storageService;
    private final WebClient webClient;
    private final String rendererUrl;
    private final int timeoutSeconds;
    private final boolean enabled;

    @Autowired
    public PlaywrightBannerRenderer(
            @Value("${branding.banner.renderer-url:}") String rendererUrl,
            @Value("${branding.banner.renderer-timeout-seconds:30}") int timeoutSeconds,
            S3StorageService storageService,
            WebClient.Builder webClientBuilder) {
        this.rendererUrl = rendererUrl == null ? "" : rendererUrl.trim();
        this.timeoutSeconds = timeoutSeconds;
        this.storageService = storageService;
        this.enabled = !this.rendererUrl.isBlank();
        // 8 MB ceiling: a 1200x630@2x WebP is ~50-200KB, but allow headroom.
        this.webClient = enabled
                ? webClientBuilder.clone()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                    .build()
                : null;
        log.info("PlaywrightBannerRenderer initialized: enabled={}, url={}",
                enabled, enabled ? this.rendererUrl : "(unset → placeholder fallback)");
    }

    /** Test seam — inject a pre-built WebClient (e.g. stub ExchangeFunction). */
    PlaywrightBannerRenderer(String rendererUrl, int timeoutSeconds,
                             S3StorageService storageService, WebClient webClient) {
        this.rendererUrl = rendererUrl == null ? "" : rendererUrl.trim();
        this.timeoutSeconds = timeoutSeconds;
        this.storageService = storageService;
        this.webClient = webClient;
        this.enabled = !this.rendererUrl.isBlank();
    }

    @Override
    public String render(BannerComposition composition, UUID instanceId) {
        if (!enabled || composition == null || composition.html() == null) {
            log.info("[banner][playwright] renderer-url not set or empty composition — "
                    + "placeholder fallback for instance {}", instanceId);
            return null;
        }
        try {
            byte[] webp = webClient.post()
                    .uri(rendererUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "html", composition.html(),
                            "width", composition.width(),
                            "height", composition.height()))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (webp == null || webp.length == 0) {
                log.warn("[banner][playwright] sidecar returned empty body — placeholder fallback");
                return null;
            }

            String path = "banners/" + instanceId + "/" + UUID.randomUUID() + ".webp";
            storageService.uploadAsset(new ByteArrayInputStream(webp), path, "image/webp", webp.length);
            String url = storageService.getPresignedAssetUrl(path);
            log.info("[banner][playwright] rendered {} bytes → {} ({}x{})",
                    webp.length, path, composition.width(), composition.height());
            return url;
        } catch (Exception ex) {
            // Seam contract: any failure → null so caller falls back to placeholder.
            log.warn("[banner][playwright] render failed ({}) — placeholder fallback",
                    ex.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }
}
