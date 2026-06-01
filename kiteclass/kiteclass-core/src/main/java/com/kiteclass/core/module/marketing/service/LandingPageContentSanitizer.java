package com.kiteclass.core.module.marketing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiteclass.core.module.marketing.entity.LandingPage;

/**
 * Sanitizes tenant-supplied landing page content on the write path (defense-in-depth).
 *
 * <p>GAP-827: {@link com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest}
 * flows through MapStruct {@code updateEntity} (raw copy) into the entity, then persists.
 * React JSX auto-escapes XSS at the {@code .tsx} render path, BUT the same text is reused on
 * surfaces that do NOT auto-escape:
 * <ul>
 *   <li>{@code JsonLd.tsx} ({@code dangerouslySetInnerHTML} for schema.org JSON-LD)</li>
 *   <li>transactional email templates</li>
 *   <li>PDF certificate generation</li>
 *   <li>{@code <meta>} / Open Graph tags</li>
 * </ul>
 * Sanitizing on write closes the stored-XSS hole regardless of render surface.
 *
 * <p>Sanitization MUST preserve Vietnamese diacritics raw (NFC) per
 * {@code .claude/rules/vn-localization-audit-checklist.md} §5 — Jsoup {@code Safelist.none()}
 * strips HTML markup while leaving Unicode text intact (NOT {@code HtmlUtils.htmlEscape}
 * single-arg, which corrupts {@code â/ê/ô} into numeric entities — see Wave 106 GAP-764).
 *
 * @since wave-thesis-5 (GAP-827)
 */
public interface LandingPageContentSanitizer {

    /**
     * Sanitizes every tenant-controlled field on the entity in place after the MapStruct
     * partial update. Idempotent — re-sanitizing already-clean content is a no-op.
     *
     * @param entity the landing page entity to sanitize (post-{@code updateEntity})
     */
    void sanitize(LandingPage entity);

    /**
     * Sanitizes a single plain-text field: strips ALL HTML/script markup, NFC-normalizes
     * Vietnamese diacritics, trims. Returns null for null input.
     *
     * @param raw untrusted plain-text input
     * @return sanitized plain text; null if input null; empty string if input was all-markup
     */
    String sanitizeText(String raw);

    /**
     * Recursively sanitizes every string value in a JSONB tree (object values + array
     * elements). Object KEYS are left unchanged (controlled by code, not tenant).
     * Numeric / boolean / null nodes pass through unchanged.
     *
     * @param node untrusted JSON tree from tenant
     * @return sanitized JSON tree (new tree; input not mutated); null if input null
     */
    JsonNode sanitizeJson(JsonNode node);

    /**
     * Validates a tenant-supplied image/asset URL against the configured host allowlist.
     *
     * <p>Rejects non-https schemes ({@code javascript:}, {@code data:}, {@code http:}) and
     * off-allowlist hosts (blocks off-origin {@code .svg} script vectors + {@code .html}
     * disguised as {@code .png}). Returns the original URL when valid; null/blank passes
     * through (optional field); throws when an invalid non-blank URL is supplied so the
     * caller surfaces HTTP 400 instead of silently persisting a malicious URL.
     *
     * @param url tenant-supplied image URL (heroImageUrl / logoUrl)
     * @return the URL unchanged when valid (or null/blank)
     * @throws com.kiteclass.core.common.exception.ValidationException when a non-blank URL is
     *         off-allowlist or non-https
     */
    String validateImageUrl(String url);
}
