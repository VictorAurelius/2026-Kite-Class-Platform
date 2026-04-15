package com.kiteclass.core.module.legal.service;

import com.kiteclass.core.module.legal.config.TrademarkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Proactive trademark scaffold (ADR-012 Track 1).
 *
 * <p>Runs a cheap case-insensitive keyword match against the seed list configured in
 * {@link TrademarkProperties}. Intended for wiring into the Wave 3 async
 * {@code GenerateLogoStep} (Sub-PR 3.5) so logo prompts / generated text are screened before
 * publication. When that step lands, call {@link #checkTextKeywords(String)} against the
 * tenant-supplied name / tagline / prompt; on {@link TrademarkCheckResult#isClear() non-clear}
 * results, route the resource to the TEMPLATE category fallback per ADR-012.
 *
 * <p>Real fuzzy-matching and USPTO-API integration are deferred; the scaffold is intentionally
 * minimal so it stays correct while the richer list is curated.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrademarkCheckService {

    private final TrademarkProperties properties;

    /**
     * Scans {@code text} for seed banned keywords (case-insensitive substring match).
     *
     * @param text arbitrary user-supplied text (instance name, tagline, prompt snippet)
     * @return clear result when no hit, otherwise flagged with the matched keywords
     */
    public TrademarkCheckResult checkTextKeywords(String text) {
        if (text == null || text.isBlank()) {
            return TrademarkCheckResult.clear();
        }
        List<String> banned = properties.getBannedKeywords();
        if (banned == null || banned.isEmpty()) {
            return TrademarkCheckResult.clear();
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        for (String keyword : banned) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                hits.add(keyword);
            }
        }
        if (hits.isEmpty()) {
            return TrademarkCheckResult.clear();
        }
        log.info("[trademark] flagged text contains banned keywords: {}", hits);
        return TrademarkCheckResult.flagged(hits);
    }
}
