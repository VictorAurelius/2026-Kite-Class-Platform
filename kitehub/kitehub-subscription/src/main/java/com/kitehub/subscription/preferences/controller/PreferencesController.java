package com.kitehub.subscription.preferences.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for user preference state — dismissible banners
 * + onboarding phase tracking (Wave 98 GAP-656 UI Coordinator).
 *
 * <p>Single endpoint surface per contract
 * {@code documents/01-business/kitehub/preferences/api-contract.md}:</p>
 * <ul>
 *   <li>{@code POST /api/v1/preferences/dismiss-banner-state} — set httpOnly
 *       cookie marker for cross-tab dismissal sync</li>
 * </ul>
 *
 * <p><strong>Scope deviation:</strong> GAP-656 §Proposed Fix Step 4 originally
 * specified {@code kitehub-platform} as the controller location, but
 * {@code kitehub-platform} is a shared library JAR (domain entities + logging
 * utilities only — no REST surface). Per state-check at implementation time
 * (Bucket B0), controller lands in {@code kitehub-subscription} where sister
 * public-write controllers (FeedbackController, BetaAccessController) already
 * live. No constraint change to GAP-656 §AC.</p>
 *
 * <p><strong>Persistence:</strong> Wave 98 Phase 1 uses in-memory
 * {@link ConcurrentHashMap} for dismissed markers (per-server, lost on
 * restart). Cookie set on every request ensures client-side dismissal
 * survives. Phase 2 (Wave 99+) will persist into {@code user_preferences}
 * table when user authenticated. TODO comment marks future work.</p>
 *
 * @since Wave 98 — GAP-656
 */
@RestController
@Slf4j
public class PreferencesController {

    /** Cookie prefix synced với FE {@code useOnboardingPhase} hook. */
    private static final String COOKIE_PREFIX = "kite-banner-dismissed-";

    /** 30-day cookie expiry per GAP-656 §Proposed Fix Step 5. */
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(30);

    /**
     * In-memory dismissed state mapping (Phase 1 — per GAP-656 §Implementation notes).
     * Key: {@code userId-or-anonymous + ":" + bannerKey} for soft partitioning.
     * TODO Wave 99+: replace with user_preferences table persistence.
     */
    private final ConcurrentHashMap<String, Boolean> dismissedState = new ConcurrentHashMap<>();

    @PostMapping("/api/v1/preferences/dismiss-banner-state")
    public ResponseEntity<Void> dismissBannerState(
            @Valid @RequestBody DismissBannerStateRequest request,
            HttpServletResponse response) {

        String sanitizedKey = sanitizeBannerKey(request.bannerKey());
        String mapKey = "anonymous:" + sanitizedKey; // TODO Wave 99: derive userId từ SecurityContextHolder
        dismissedState.put(mapKey, request.dismissed());

        // Note: bannerKey already constrained to [a-z0-9-] per @Pattern;
        // sanitized to be defensive against future relaxation.
        ResponseCookie cookie = ResponseCookie.from(COOKIE_PREFIX + sanitizedKey, request.dismissed() ? "1" : "0")
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .httpOnly(false) // SET FALSE so FE useOnboardingPhase document.cookie can read marker per GAP-656 Step 5
                                  // SameSite=Lax ensures CSRF protection; future Phase 2 may flip to httpOnly với same-doc GET endpoint
                .sameSite("Lax")
                .secure(true) // production-only flag enforced via reverse proxy (HTTPS-only)
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
        log.debug("dismiss-banner-state set: key={} dismissed={}", sanitizedKey, request.dismissed());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Defensive sanitization — strip anything outside [a-z0-9-] và lowercase.
     * Hash collision with @Pattern but provides defense-in-depth nếu validation bypassed.
     */
    private static String sanitizeBannerKey(String key) {
        if (key == null) return "unknown";
        String lower = key.toLowerCase();
        StringBuilder sb = new StringBuilder(Math.min(lower.length(), 100));
        for (int i = 0; i < lower.length() && sb.length() < 100; i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-') {
                sb.append(c);
            }
        }
        return sb.isEmpty() ? "unknown" : sb.toString();
    }

    /**
     * DTO for POST /api/v1/preferences/dismiss-banner-state body.
     * Matches {@code documents/01-business/kitehub/preferences/api-contract.md}
     * field constraints.
     */
    public record DismissBannerStateRequest(
            @NotBlank
            @Size(min = 3, max = 100)
            @Pattern(regexp = "^[a-z0-9-]+$", message = "PREF_INVALID_BANNER_KEY")
            String bannerKey,

            @NotNull
            Boolean dismissed
    ) {
    }
}
