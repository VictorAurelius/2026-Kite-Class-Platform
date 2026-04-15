package com.kiteclass.core.module.legal.service;

import lombok.Value;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of a proactive trademark scan.
 *
 * <p>Value object — {@link #isClear()} is the primary predicate; when {@code false},
 * {@link #getHits()} lists the offending keywords for audit / user messaging.
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.3, GAP-042)
 */
@Value
public class TrademarkCheckResult {

    boolean clear;
    List<String> hits;

    public static TrademarkCheckResult clear() {
        return new TrademarkCheckResult(true, Collections.emptyList());
    }

    public static TrademarkCheckResult flagged(List<String> hits) {
        return new TrademarkCheckResult(false, List.copyOf(hits));
    }
}
