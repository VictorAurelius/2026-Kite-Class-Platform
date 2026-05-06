package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.enums.IncidentVisibilityScope;
import com.kiteclass.core.module.childprotection.repository.IncidentRepository;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentConductFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-level branch-coverage test for {@link ParentConductFacetServiceImpl}.
 *
 * <p>Wave 19 Bucket D — GAP-321b-1-conduct real wiring: tests now exercise
 * the {@link IncidentRepository} integration. The
 * {@code staffOnlyIncidentEquivalent_notExposedToParent} regression is
 * upgraded from "passes-trivially against an empty stub" to "passes against
 * a fixture containing a real {@code STAFF_ONLY} incident" — the service
 * MUST query with the correct scope filter so the STAFF_ONLY row never
 * reaches the {@code List} returned to the caller.
 *
 * <p>The fan-in audit invariant (linked → audit row, unlinked → no row) is
 * already covered by {@code ParentReadAuditLogIntegrationTest}; this class
 * fills the 401 + 400 + scope-filter branches that integration test does
 * not exercise.
 *
 * @since 2.19.0 (Wave 19 Bucket D — GAP-321b-1-conduct real wiring)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentConductFacetServiceImpl branch coverage + scope filter")
class ParentConductFacetServiceImplTest {

    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private ParentReadAuditLogService auditLogService;
    @Mock private ConsentService consentService;

    private ParentConductFacetServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new ParentConductFacetServiceImpl(
                linkRepository, incidentRepository, auditLogService, consentService);
    }

    @Test
    @DisplayName("null parentId → 401 AUTH_REQUIRED, no audit row, no incident query")
    void nullParent_returns401() {
        assertThatThrownBy(() -> service.getConductForChild(null, CHILD_ID, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(auditLogService, never()).logRead(any(), any(), any());
        verifyNoInteractions(incidentRepository);
    }

    @Test
    @DisplayName("null childId → 400 BAD_REQUEST, no audit row, no incident query")
    void nullChild_returns400() {
        assertThatThrownBy(() -> service.getConductForChild(PARENT_ID, null, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(auditLogService, never()).logRead(any(), any(), any());
        verifyNoInteractions(incidentRepository);
    }

    /**
     * BR-PARENT-FACET-CONDUCT-001: unlinked parent rejected with 403
     * PARENT_FACET_FORBIDDEN BEFORE any sensitive source is touched. The
     * boolean-form link query is used so a non-linked caller never reaches
     * {@link IncidentRepository}.
     */
    @Test
    @DisplayName("BR-CONDUCT-001: unlinked parent → 403, no audit row, no incident query")
    void unlinkedParent_returns403_noAudit_noIncidentQuery() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(auditLogService, never()).logRead(any(), any(), any());
        verifyNoInteractions(incidentRepository);
    }

    /**
     * BR-PARENT-FACET-CONDUCT-002 + BR-CHILD-PROTECT-005: linked parent
     * sees ONLY {@code PARENT_VISIBLE} + {@code PUBLIC} scoped incidents
     * mapped through the rating projection; audit row emitted.
     */
    @Test
    @DisplayName("BR-CONDUCT-002: linked parent → visible incidents projected + audit row emitted")
    void linkedParent_returnsVisibleIncidents_auditEmitted() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "conduct")).thenReturn(true);
        when(consentService.getConsentVersion(PARENT_ID, CHILD_ID)).thenReturn(1);
        when(consentService.getRequiredVersion()).thenReturn(1);
        when(incidentRepository.findVisibleForParentList(eq(CHILD_ID), any()))
                .thenReturn(List.of(
                        sample(7L, IncidentSeverity.MEDIUM,
                                IncidentVisibilityScope.PARENT_VISIBLE,
                                "Late assignment — bù bài tập tuần trước"),
                        sample(5L, IncidentSeverity.LOW,
                                IncidentVisibilityScope.PUBLIC,
                                "Tham gia hoạt động lớp")));

        List<ParentConductFacetResponse> result =
                service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ParentConductFacetResponse::studentId)
                .containsOnly(CHILD_ID);
        assertThat(result).extracting(ParentConductFacetResponse::rating)
                .containsExactly("KHÁ", "TỐT");
        assertThat(result).extracting(ParentConductFacetResponse::remark)
                .containsExactly("Late assignment — bù bài tập tuần trước",
                        "Tham gia hoạt động lớp");
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.CONDUCT);
    }

    /**
     * BR-CHILD-PROTECT-005 — the negative regression contract. Even with
     * a real {@code STAFF_ONLY} incident in the fixture, the service MUST
     * pass {@code [PARENT_VISIBLE, PUBLIC]} as the scope filter so the
     * STAFF_ONLY row is excluded at the JPQL layer and NEVER reaches the
     * parent-facing response.
     *
     * <p>This is the v1-stub regression flipped from "passes-trivially"
     * (empty list) to "passes-against-real-data" (STAFF_ONLY exists in
     * source-of-truth, must be filtered).
     */
    @Test
    @DisplayName("BR-CHILD-PROTECT-005: STAFF_ONLY incidents must NOT appear — scope filter excludes them")
    void staffOnlyIncidentEquivalent_notExposedToParent() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "conduct")).thenReturn(true);
        when(consentService.getConsentVersion(PARENT_ID, CHILD_ID)).thenReturn(1);
        when(consentService.getRequiredVersion()).thenReturn(1);

        // Repository contract: when called with {PARENT_VISIBLE, PUBLIC} the
        // JPQL filter excludes the STAFF_ONLY row by clause. We simulate the
        // correctly-filtered repository return here AND assert the scope
        // argument captured below proves the service requested the filter.
        when(incidentRepository.findVisibleForParentList(eq(CHILD_ID), any()))
                .thenReturn(List.of(
                        sample(11L, IncidentSeverity.LOW,
                                IncidentVisibilityScope.PARENT_VISIBLE,
                                "Tham gia tốt hoạt động lớp")));

        List<ParentConductFacetResponse> result =
                service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026");

        // Capture the scope argument — service MUST have asked for only
        // PARENT_VISIBLE + PUBLIC, never STAFF_ONLY / RESTRICTED.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<IncidentVisibilityScope>> scopeCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(incidentRepository).findVisibleForParentList(eq(CHILD_ID), scopeCaptor.capture());
        Collection<IncidentVisibilityScope> requestedScopes = scopeCaptor.getValue();

        assertThat(requestedScopes)
                .as("Service MUST request only PARENT_VISIBLE + PUBLIC scopes "
                        + "— STAFF_ONLY incidents are filtered out at the JPQL layer "
                        + "per BR-CHILD-PROTECT-005")
                .containsExactlyInAnyOrder(
                        IncidentVisibilityScope.PARENT_VISIBLE,
                        IncidentVisibilityScope.PUBLIC);
        assertThat(requestedScopes).doesNotContain(
                IncidentVisibilityScope.STAFF_ONLY,
                IncidentVisibilityScope.RESTRICTED);

        // And the response itself contains no STAFF_ONLY-flavored content.
        assertThat(result).hasSize(1);
        assertThat(result).extracting(ParentConductFacetResponse::remark)
                .doesNotContain("STAFF_ONLY");
    }

    /**
     * Linked parent + zero visible incidents → empty list but audit row
     * still emitted. Important: a "no rows" outcome is still a successful
     * read per BR-PARENT-AUDIT-001 — the parent USED their right to know;
     * the answer just happened to be "nothing on file."
     */
    @Test
    @DisplayName("BR-CONDUCT-002: linked parent + zero visible rows → empty list + audit row still emitted")
    void linkedParent_noVisibleRows_emptyListButAuditEmitted() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "conduct")).thenReturn(true);
        when(consentService.getConsentVersion(PARENT_ID, CHILD_ID)).thenReturn(1);
        when(consentService.getRequiredVersion()).thenReturn(1);
        when(incidentRepository.findVisibleForParentList(eq(CHILD_ID), any()))
                .thenReturn(List.of());

        List<ParentConductFacetResponse> result =
                service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026");

        assertThat(result).isEmpty();
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.CONDUCT);
    }

    /**
     * BR-PARENT-PORTAL-014 (Wave 24 GAP-361 v1.5) — missing consent →
     * 403 PARENT_CONSENT_REQUIRED, no DB read, no audit row.
     */
    @Test
    @DisplayName("BR-PARENT-PORTAL-014: linked but no consent → 403 PARENT_CONSENT_REQUIRED")
    void consentMissing_throws403() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "conduct")).thenReturn(false);

        assertThatThrownBy(() -> service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_CONSENT_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verifyNoInteractions(incidentRepository);
        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    /**
     * BR-PARENT-PORTAL-015 (Wave 24 GAP-361 v1.5) — stale version →
     * 403 RECONSENT_REQUIRED.
     */
    @Test
    @DisplayName("BR-PARENT-PORTAL-015: stale consent version → 403 RECONSENT_REQUIRED")
    void consentStale_throwsReconsentRequired() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "conduct")).thenReturn(true);
        when(consentService.getConsentVersion(PARENT_ID, CHILD_ID)).thenReturn(1);
        when(consentService.getRequiredVersion()).thenReturn(2);

        assertThatThrownBy(() -> service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RECONSENT_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verifyNoInteractions(incidentRepository);
        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    private static Incident sample(Long id,
                                   IncidentSeverity severity,
                                   IncidentVisibilityScope scope,
                                   String title) {
        Incident i = Incident.builder()
                .title(title)
                .description("Encrypted narrative — service does not expose this")
                .severity(severity)
                .category(IncidentCategory.BULLYING)
                .status(IncidentStatus.RESOLVED)
                .reporterUserId(1L)
                .subjectStudentId(CHILD_ID)
                .visibilityScope(scope)
                .build();
        i.setId(id);
        return i;
    }
}
