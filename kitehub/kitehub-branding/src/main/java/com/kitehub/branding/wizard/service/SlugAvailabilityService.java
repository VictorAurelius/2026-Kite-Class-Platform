package com.kitehub.branding.wizard.service;

import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.SlugAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Slug-availability service for the AI Branding wizard (Wave 34 sub-GAP-272i).
 *
 * <p>Replaces the Wave 32 v1 inline {@code MOCK_TAKEN_SLUGS} stub. Source of
 * truth for "taken" is currently:
 * <ol>
 *   <li>Reserved-words filter (admin/api/billing/etc — never available)</li>
 *   <li>Existing {@code BrandingJob.organizationName} slug-normalized — best
 *       effort proxy until kiteclass-core's {@code frontend_instances} is
 *       reachable from kitehub-branding (cross-service slug check tracked as
 *       a follow-up; see PR body).</li>
 * </ol>
 *
 * <p>Format rules per api-contract.md: 3–63 chars, lowercase alphanumeric +
 * hyphens, no leading/trailing hyphen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlugAvailabilityService {

    static final Pattern SLUG_FORMAT =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$");

    /** Reserved labels — never available. Mirrors common platform conventions. */
    static final Set<String> RESERVED = Set.of(
            "admin", "api", "auth", "billing", "blog", "console", "dashboard",
            "docs", "help", "internal", "kite", "kiteclass", "kitehub", "login",
            "mail", "platform", "public", "root", "signup", "status", "support",
            "system", "test", "www");

    private static final int MAX_SUGGESTIONS = 5;

    private final BrandingJobRepository brandingJobRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Validate the slug format. Returns null when valid; otherwise a short
     * error message identifying the violation.
     */
    public String validateFormat(String slug) {
        if (slug == null || slug.isBlank()) {
            return "slug is required";
        }
        if (!SLUG_FORMAT.matcher(slug).matches()) {
            return "slug must be 3–63 chars, lowercase alphanumeric + hyphens, "
                    + "no leading/trailing hyphen";
        }
        return null;
    }

    /**
     * Check availability of {@code slug}. Always returns 5 suggestions when
     * unavailable, empty list otherwise.
     */
    public SlugAvailabilityResponse check(String slug) {
        return check(slug, null);
    }

    /**
     * G1 walk 2026-06-12 (re-brand UX): owner đang re-brand tenant hiện hữu PHẢI được
     * giữ chính subdomain của mình — exempt own instance khỏi "taken". {@code ownInstanceId}
     * = gateway-trusted X-Tenant-Id của caller (null cho anonymous/new-tenant flow).
     */
    public SlugAvailabilityResponse check(String slug, java.util.UUID ownInstanceId) {
        boolean taken = isTaken(slug, ownInstanceId);
        if (!taken) {
            return new SlugAvailabilityResponse(true, List.of());
        }
        return new SlugAvailabilityResponse(false, suggest(slug));
    }

    private boolean isTaken(String slug) {
        return isTaken(slug, null);
    }

    private boolean isTaken(String slug, java.util.UUID ownInstanceId) {
        String normalized = slug.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(normalized)) {
            return true;
        }
        // GAP-1111: instances.subdomain is the CANONICAL signup subdomain — DB
        // UNIQUE, claimed by kitehub-subscription on tenant create. branding
        // shares the same /kitehub datasource, so read it directly as the
        // authoritative "taken" signal. (The branding_jobs.organization_name
        // check below stays as a secondary best-effort proxy. A cross-DB check
        // against kiteclass-core frontend_instances is a further follow-up.)
        Boolean subdomainTaken = ownInstanceId == null
                ? jdbcTemplate.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM instances "
                                + "WHERE LOWER(subdomain) = ? AND deleted = false)",
                        Boolean.class, normalized)
                : jdbcTemplate.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM instances "
                                + "WHERE LOWER(subdomain) = ? AND deleted = false AND id <> ?)",
                        Boolean.class, normalized, ownInstanceId);
        if (Boolean.TRUE.equals(subdomainTaken)) {
            return true;
        }
        // GAP-392: replaced previous {@code findAll().stream().anyMatch(...)}
        // N+1 anti-pattern with a derived-query existence check that uses the
        // V31 functional index {@code idx_branding_job_org_name_lower}.
        //
        // Semantic note: prior code applied {@link #normalize} (hyphenation +
        // non-alphanumeric stripping) to every {@code organization_name} row
        // before comparing — which cannot be expressed in a SQL functional
        // index without pg-trgm or a stored normalised column. This check
        // therefore matches when {@code LOWER(organization_name) == slug}.
        // Edge cases such as {@code "Acme Corp" → "acme-corp"} no longer
        // match against the raw organisation name; tracked as a follow-up
        // gap once a {@code slug_normalised} column is added.
        return brandingJobRepository.existsByOrganizationNameLowercased(normalized);
    }

    /**
     * Build up to {@link #MAX_SUGGESTIONS} alternates by appending common
     * suffixes (-2, -vn, -edu, -school, -hub) and skipping any that are
     * themselves taken/invalid.
     */
    List<String> suggest(String slug) {
        List<String> candidates = new ArrayList<>();
        String[] suffixes = {"-2", "-vn", "-edu", "-school", "-hub"};
        for (String s : suffixes) {
            String c = slug + s;
            if (validateFormat(c) == null && !isTaken(c)) {
                candidates.add(c);
                if (candidates.size() >= MAX_SUGGESTIONS) break;
            }
        }
        return candidates;
    }
}
