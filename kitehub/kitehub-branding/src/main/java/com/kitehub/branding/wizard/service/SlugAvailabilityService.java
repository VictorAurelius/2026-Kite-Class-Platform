package com.kitehub.branding.wizard.service;

import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.SlugAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        boolean taken = isTaken(slug);
        if (!taken) {
            return new SlugAvailabilityResponse(true, List.of());
        }
        return new SlugAvailabilityResponse(false, suggest(slug));
    }

    private boolean isTaken(String slug) {
        String normalized = slug.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(normalized)) {
            return true;
        }
        // Best-effort proxy: scan branding_jobs.organization_name for a
        // slug-normalized match. Cross-service check against
        // frontend_instances tracked as a follow-up gap (see PR body).
        return brandingJobRepository.findAll().stream()
                .map(j -> normalize(j.getOrganizationName()))
                .anyMatch(normalized::equals);
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

    /** Normalize a name into a slug — lowercase, hyphenate, strip non-allowed. */
    private static String normalize(String name) {
        if (name == null) return "";
        String lower = name.toLowerCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder(lower.length());
        boolean prevHyphen = false;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
                prevHyphen = false;
            } else if (!prevHyphen && sb.length() > 0) {
                sb.append('-');
                prevHyphen = true;
            }
        }
        // Trim trailing hyphen.
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
