package com.kiteclass.core.module.document.branding;

import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.marketing.config.LandingPageSafetyProperties;
import com.kiteclass.core.module.settings.dto.response.BrandingResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sub-PR 5.5 — assembles per-tenant branding into a {@link DocumentRequest} before it hits the
 * format-specific {@link com.kiteclass.core.module.document.Generator}.
 *
 * <p>Key names stay as the generators already expect (<code>branding.primaryColor</code>,
 * <code>branding.logoUrl</code>, ...), so this class only needs to populate the data map — no
 * generator change is required.
 *
 * <h2>SSRF hardening (GAP-1040)</h2>
 *
 * <p>The merged data map flows into the Thymeleaf invoice template where
 * <code>&lt;img th:src="${brand.logoUrl}"&gt;</code> is resolved <em>server-side</em> by
 * OpenHTMLtoPDF at render time. A caller-supplied fetch-able URL there is a Server-Side Request
 * Forgery vector (e.g. <code>http://169.254.169.254/latest/meta-data/</code> or an internal host).
 * Two guards close it:
 *
 * <ol>
 *   <li><b>Server is authoritative for fetch-able URLs.</b> Any caller-supplied key whose name ends
 *       in <code>url</code> (case-insensitive — covers <code>logoUrl</code>, <code>faviconUrl</code>,
 *       <code>branding.logoUrl</code>, ...) is stripped before merge. Non-URL keys (colors,
 *       display name) keep their caller-override precedence — those are escaped via
 *       <code>th:text</code> / used only as CSS color values and carry no fetch surface.</li>
 *   <li><b>Host allowlist on the server logoUrl.</b> Even the tenant-configured branding logo is
 *       validated against {@code landing.allowed-image-hosts} (MinIO/CDN allowlist, reused from
 *       {@link LandingPageSafetyProperties}). A logoUrl whose host is outside the allowlist (or
 *       whose scheme is not https / dev-http) is dropped — the template's {@code th:if} then skips
 *       the {@code <img>} entirely, so no egress occurs.</li>
 * </ol>
 *
 * <p>Precedence: caller-provided NON-URL keys win (testing/preview overrides); server-resolved
 * fetch-able URLs always win because the caller cannot supply one.
 */
@Slf4j
@Component
public class DocumentBrandingAssembler {

    private final LandingPageSafetyProperties safetyProperties;

    public DocumentBrandingAssembler(LandingPageSafetyProperties safetyProperties) {
        this.safetyProperties = safetyProperties;
    }

    public DocumentRequest enrich(DocumentRequest request, BrandingResponse branding) {
        Map<String, Object> merged = new LinkedHashMap<>();

        // 1. Server branding first (so caller NON-URL keys can override below). Fetch-able URLs
        //    (logoUrl) are host-allowlist validated here — the server value is authoritative.
        if (branding != null) {
            putIfPresent(merged, "branding.primaryColor", branding.getPrimaryColor());
            putIfPresent(merged, "branding.secondaryColor", branding.getSecondaryColor());
            putIfPresent(merged, "branding.accentColor", branding.getAccentColor());
            putBrandingLogoIfAllowed(merged, branding.getLogoUrl());
            putIfPresent(merged, "branding.displayName", branding.getDisplayName());
        }

        // 2. Caller data wins for NON-URL keys; fetch-able URL keys are STRIPPED (SSRF guard —
        //    the caller must never inject a URL the server fetches at render time).
        boolean strippedAny = false;
        for (Map.Entry<String, Object> entry : request.data().entrySet()) {
            if (isFetchableUrlKey(entry.getKey())) {
                log.warn(
                        "Dropping caller-supplied fetch-able URL key '{}' from document data — "
                                + "server branding is authoritative (SSRF guard, GAP-1040)",
                        entry.getKey());
                strippedAny = true;
                continue;
            }
            merged.put(entry.getKey(), entry.getValue());
        }

        // No effective change → return the original instance (preserves identity for callers/tests).
        if (!strippedAny && merged.equals(request.data())) {
            return request;
        }

        return DocumentRequest.builder()
                .format(request.format())
                .templateId(request.templateId())
                .tenantId(request.tenantId())
                .data(merged)
                .build();
    }

    /**
     * A key is treated as a fetch-able URL field — and therefore never honored from caller input —
     * when its name ends in {@code url} (case-insensitive). Covers {@code logoUrl},
     * {@code faviconUrl}, {@code branding.logoUrl}, {@code imageUrl}, etc.
     */
    private static boolean isFetchableUrlKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).endsWith("url");
    }

    /**
     * Add the server-resolved branding logo only when its host passes the
     * {@code landing.allowed-image-hosts} allowlist + scheme check. Otherwise the key is omitted so
     * the template skips the {@code <img>} (no server-side egress).
     */
    private void putBrandingLogoIfAllowed(Map<String, Object> target, String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }
        if (isAllowedLogoHost(logoUrl.trim())) {
            target.put("branding.logoUrl", logoUrl);
        } else {
            log.warn(
                    "Skipping branding logoUrl '{}' — host not in allowed-image-hosts allowlist "
                            + "(SSRF guard, GAP-1040)",
                    logoUrl);
        }
    }

    /**
     * Mirrors {@code LandingPageContentSanitizerImpl#validateImageUrl} semantics (exact host match
     * or sub-domain of an allowlisted host; https everywhere, http only for dev hosts) but returns
     * a boolean instead of throwing — a bad branding logo must not 500 a document render.
     */
    private boolean isAllowedLogoHost(String url) {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if (host.isEmpty()) {
            // data:, javascript:, file:, opaque, or relative URI → no host → reject (no egress).
            return false;
        }

        boolean hostAllowed = safetyProperties.getAllowedImageHosts().stream()
                .map(h -> h.toLowerCase(Locale.ROOT))
                .anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
        if (!hostAllowed) {
            return false;
        }

        boolean devHost = host.equals("localhost") || host.equals("minio")
                || host.equals("kite-minio") || host.endsWith(".minio");
        return scheme.equals("https") || (devHost && scheme.equals("http"));
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            return;
        }
        target.put(key, value);
    }
}
