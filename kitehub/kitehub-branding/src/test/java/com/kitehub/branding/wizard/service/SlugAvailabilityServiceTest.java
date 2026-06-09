package com.kitehub.branding.wizard.service;

import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.SlugAvailabilityResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SlugAvailabilityService} — exercises the
 * GAP-392 N+1 fix: the service must use the
 * {@code existsByOrganizationNameLowercased} derived query, NOT
 * {@code findAll()}, and must perform that lookup at most once per
 * {@code check()} call (excluding suggestion-suffix iterations).
 */
@DisplayName("SlugAvailabilityService")
class SlugAvailabilityServiceTest {

    private BrandingJobRepository jobRepo;
    private JdbcTemplate jdbcTemplate;
    private SlugAvailabilityService service;

    @BeforeEach
    void setUp() {
        jobRepo = mock(BrandingJobRepository.class);
        // GAP-1111: default mock queryForObject(...) returns null -> instances.subdomain
        // check evaluates "not taken", so existing org-name/reserved/format tests are
        // unaffected. Full instances-taken coverage is a Testcontainers IT (gap AC).
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new SlugAvailabilityService(jobRepo, jdbcTemplate);
    }

    @Test
    @DisplayName("validateFormat: rejects empty + format violations + accepts well-formed slug")
    void validateFormatChecks() {
        assertThat(service.validateFormat(null)).isNotNull();
        assertThat(service.validateFormat("")).isNotNull();
        assertThat(service.validateFormat("ab")).isNotNull(); // too short
        assertThat(service.validateFormat("-leading")).isNotNull();
        assertThat(service.validateFormat("trailing-")).isNotNull();
        assertThat(service.validateFormat("UPPER")).isNotNull();
        assertThat(service.validateFormat("acme-corp")).isNull();
    }

    @Test
    @DisplayName("check: reserved slug is taken; findAll() NEVER queried (suffix-suggestions OK to query)")
    void checkReservedShortCircuit() {
        // Suggestion suffixes are non-reserved, so they go through the repo;
        // stub them to free.
        when(jobRepo.existsByOrganizationNameLowercased(anyString())).thenReturn(false);

        SlugAvailabilityResponse resp = service.check("admin");

        assertThat(resp.available()).isFalse();
        // The reserved slug itself MUST short-circuit before hitting the repo.
        // Suggestions are allowed to query (admin-2, admin-vn, ...).
        verify(jobRepo, never()).existsByOrganizationNameLowercased("admin");
        verify(jobRepo, never()).findAll();
    }

    @Test
    @DisplayName("check: available when no matching organization name (existsByOrganizationNameLowercased=false)")
    void checkAvailableWhenNoMatch() {
        when(jobRepo.existsByOrganizationNameLowercased(anyString())).thenReturn(false);

        SlugAvailabilityResponse resp = service.check("free-slug");

        assertThat(resp.available()).isTrue();
        assertThat(resp.suggestions()).isEmpty();
        verify(jobRepo, never()).findAll(); // GAP-392: no full-table scan
    }

    @Test
    @DisplayName("check: taken when matching organization name + suggestions returned")
    void checkTakenReturnsSuggestions() {
        // The slug itself is taken; all suggestion variants are free.
        when(jobRepo.existsByOrganizationNameLowercased("acme")).thenReturn(true);
        when(jobRepo.existsByOrganizationNameLowercased("acme-2")).thenReturn(false);
        when(jobRepo.existsByOrganizationNameLowercased("acme-vn")).thenReturn(false);
        when(jobRepo.existsByOrganizationNameLowercased("acme-edu")).thenReturn(false);
        when(jobRepo.existsByOrganizationNameLowercased("acme-school")).thenReturn(false);
        when(jobRepo.existsByOrganizationNameLowercased("acme-hub")).thenReturn(false);

        SlugAvailabilityResponse resp = service.check("acme");

        assertThat(resp.available()).isFalse();
        assertThat(resp.suggestions()).containsExactly("acme-2", "acme-vn", "acme-edu", "acme-school", "acme-hub");
        verify(jobRepo, never()).findAll(); // GAP-392 guard
        verify(jobRepo, atLeastOnce()).existsByOrganizationNameLowercased(anyString());
    }

    @Test
    @DisplayName("GAP-392: large dataset would have caused findAll() N+1 — confirm not invoked")
    void noFindAllInvocation() {
        // This is the regression test: previously isTaken() called findAll()
        // and stream-mapped every row. With the V31-indexed existsByOrgname
        // path, findAll() must never run on the slug-availability hot path
        // regardless of dataset size. Stub return value is irrelevant.
        when(jobRepo.existsByOrganizationNameLowercased(anyString())).thenReturn(false);

        for (int i = 0; i < 100; i++) {
            service.check("slug-" + i);
        }

        verify(jobRepo, never()).findAll();
    }
}
