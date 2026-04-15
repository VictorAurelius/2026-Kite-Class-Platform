package com.kiteclass.core.module.retention;

/**
 * Retention classification bucket per ADR-013.
 *
 * <ul>
 *   <li>{@link #PURGE_ON_REQUEST} — PII with no legal retention requirement (e.g. uploaded
 *       logos, AI-generated assets, branding history). 7-day grace → hard delete.</li>
 *   <li>{@link #PURGE_DELAYED} — Non-PII useful for short-term ops (sessions, caches,
 *       queue messages). Already TTL'd; no action needed on account delete.</li>
 *   <li>{@link #RETAIN_WITH_PSEUDO} — PII under legal retention (VN tax law 10y invoices,
 *       audit logs 2y, moderation 5y). Pseudonymize PII fields but keep row.</li>
 *   <li>{@link #RETAIN_LEGAL_HOLD} — Data under active legal proceeding (DMCA disputes,
 *       incident evidence). Retained until hold lifts; queued post-lift.</li>
 * </ul>
 *
 * @since 3.23.0 (Wave 4 Sub-PR 4.4, ADR-013)
 */
public enum RetentionBucket {
    PURGE_ON_REQUEST,
    PURGE_DELAYED,
    RETAIN_WITH_PSEUDO,
    RETAIN_LEGAL_HOLD
}
