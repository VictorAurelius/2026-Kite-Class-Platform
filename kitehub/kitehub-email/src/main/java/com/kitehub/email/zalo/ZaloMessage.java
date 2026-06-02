package com.kitehub.email.zalo;

import lombok.Builder;
import lombok.Value;

/**
 * Domain-level Zalo OA outbound message envelope.
 *
 * <p>Neutral domain type per design-patterns.md §3.10 (Leaky Abstraction) — does
 * NOT leak any vendor-specific raw payload shape (no {@code ZaloRawRequest} or
 * Zalo SDK type). When the live {@code ZaloOAHttpClient} ships (Wave 12+
 * follow-up requiring verified Zalo OA business account), it will translate
 * this domain type into the vendor's HTTP body inside the adapter layer.</p>
 *
 * <p><strong>Phase 1 scaffold scope (Wave local-doable-11 Bucket B):</strong>
 * mock-only — fields cover the minimum needed to verify the contract surface
 * (recipient + body + optional template id). Live Zalo OA template/quick-reply
 * variants will be added as additional fields when the live adapter ships.</p>
 *
 * @since Wave local-doable-11 Bucket B (GAP-063 Phase 1 scaffold)
 */
@Value
@Builder
public class ZaloMessage {

    /**
     * Free-text message body. Required for plain-text sends; may be {@code null}
     * when {@link #templateId} is set (template-rendered body).
     */
    String body;

    /**
     * Optional Zalo OA template id. Only used by the live adapter; mock ignores
     * but echoes back so callers can assert routing decisions.
     */
    String templateId;

    /**
     * Optional locale hint (e.g., {@code "vi"}, {@code "en"}) — adapter chooses
     * the matching template variant when live.
     */
    String locale;
}
