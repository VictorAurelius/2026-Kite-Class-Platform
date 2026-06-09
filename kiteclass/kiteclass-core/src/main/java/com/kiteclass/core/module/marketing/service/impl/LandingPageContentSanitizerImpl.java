package com.kiteclass.core.module.marketing.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.marketing.config.LandingPageSafetyProperties;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.service.LandingPageContentSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Jsoup-backed {@link LandingPageContentSanitizer} (GAP-827).
 *
 * <p>Text sanitization uses {@link Safelist#none()} (strip ALL HTML/script markup → plain
 * text) with UTF-8 output charset so Vietnamese diacritics survive raw (NOT escaped to
 * numeric entities — see {@code vn-localization-audit-checklist.md} §5 / Wave 106 GAP-764).
 * NFC normalization applied so combining-form input is stored as precomposed VN chars.
 *
 * @since wave-thesis-5 (GAP-827)
 */
@Slf4j
@Component
public class LandingPageContentSanitizerImpl implements LandingPageContentSanitizer {

    /** No tags allowed → output is plain text with markup stripped. */
    private static final Safelist PLAIN_TEXT = Safelist.none();

    /** Output settings: UTF-8 + no pretty-print → VN diacritics raw, not entity-escaped. */
    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings()
            .charset("UTF-8")
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false);

    private final ObjectMapper objectMapper;
    private final LandingPageSafetyProperties properties;

    public LandingPageContentSanitizerImpl(ObjectMapper objectMapper,
                                           LandingPageSafetyProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void sanitize(LandingPage entity) {
        if (entity == null) {
            return;
        }
        // Plain-text fields
        entity.setCenterName(sanitizeText(entity.getCenterName()));
        entity.setHeroTitle(sanitizeText(entity.getHeroTitle()));
        entity.setHeroSubtitle(sanitizeText(entity.getHeroSubtitle()));
        entity.setTeacherBio(sanitizeText(entity.getTeacherBio()));
        entity.setTagline(sanitizeText(entity.getTagline()));
        entity.setAboutText(sanitizeText(entity.getAboutText()));
        entity.setAddress(sanitizeText(entity.getAddress()));
        entity.setTemplateType(sanitizeText(entity.getTemplateType()));

        // Image / asset URLs — scheme + host allowlist (throws on invalid)
        entity.setHeroImageUrl(validateImageUrl(entity.getHeroImageUrl()));
        entity.setLogoUrl(validateImageUrl(entity.getLogoUrl()));

        // JSONB structured sections — recursive string-value sanitize
        entity.setTeachers(sanitizeJson(entity.getTeachers()));
        entity.setPrograms(sanitizeJson(entity.getPrograms()));
        entity.setPricingTiers(sanitizeJson(entity.getPricingTiers()));
        entity.setTestimonials(sanitizeJson(entity.getTestimonials()));
        entity.setFaqs(sanitizeJson(entity.getFaqs()));
        entity.setStats(sanitizeJson(entity.getStats()));

        // landing-100 F-sections (GAP-1083) — same recursive string-value sanitize.
        entity.setProblemSolution(sanitizeJson(entity.getProblemSolution()));
        entity.setHowItWorks(sanitizeJson(entity.getHowItWorks()));
        entity.setTrustStrip(sanitizeJson(entity.getTrustStrip()));

        // Note: contactEmail / contactPhone / *Url social + zaloUrl fields already constrained
        // by @Email/@Pattern/@Size on the DTO+entity; they carry no free-text XSS surface.
    }

    @Override
    public String sanitizeText(String raw) {
        if (raw == null) {
            return null;
        }
        // Strip ALL markup; UTF-8 charset keeps VN diacritics raw (not numeric entities).
        String cleaned = Jsoup.clean(raw, "", PLAIN_TEXT, OUTPUT);
        // Jsoup escapes the 5 XHTML entities (& < > " ') even with Safelist.none() — unescape
        // back to plain text so stored value is human-readable plain text, then NFC normalize.
        String unescaped = org.jsoup.parser.Parser.unescapeEntities(cleaned, false);
        String normalized = Normalizer.normalize(unescaped, Normalizer.Form.NFC);
        return normalized.trim();
    }

    @Override
    public JsonNode sanitizeJson(JsonNode node) {
        if (node == null) {
            return null;
        }
        return sanitizeNode(node.deepCopy());
    }

    private JsonNode sanitizeNode(JsonNode node) {
        if (node.isTextual()) {
            return objectMapper.getNodeFactory().textNode(sanitizeText(node.asText()));
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            // Snapshot field names first to avoid concurrent-modification while re-setting.
            java.util.List<String> fieldNames = new java.util.ArrayList<>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (String name : fieldNames) {
                obj.set(name, sanitizeNode(obj.get(name)));
            }
            return obj;
        }
        if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, sanitizeNode(arr.get(i)));
            }
            return arr;
        }
        // numbers / booleans / null pass through unchanged
        return node;
    }

    @Override
    public String validateImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        final URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException ex) {
            throw new ValidationException("landing.image.url.invalid", url);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if (host.isEmpty()) {
            // javascript:, data:, mailto:, relative, or opaque URI → no host → reject
            throw new ValidationException("landing.image.url.invalid", url);
        }

        boolean hostAllowed = properties.getAllowedImageHosts().stream()
                .map(h -> h.toLowerCase(Locale.ROOT))
                .anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));

        if (!hostAllowed) {
            throw new ValidationException("landing.image.url.host", host);
        }

        boolean devHost = host.equals("localhost") || host.equals("minio")
                || host.equals("kite-minio") || host.endsWith(".minio");
        boolean schemeOk = scheme.equals("https") || (devHost && scheme.equals("http"));
        if (!schemeOk) {
            throw new ValidationException("landing.image.url.scheme", scheme);
        }

        return url;
    }
}
