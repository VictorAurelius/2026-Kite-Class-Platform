package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentConductFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level branch-coverage test for {@link ParentConductFacetServiceImpl}.
 *
 * <p>The fan-in audit invariant (linked → audit row, unlinked → no row) is
 * already covered by {@code ParentReadAuditLogIntegrationTest}; this class
 * fills the 401 + 400 branches that integration test does not exercise.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentConductFacetServiceImpl branch coverage")
class ParentConductFacetServiceImplTest {

    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private ParentReadAuditLogService auditLogService;

    private ParentConductFacetServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new ParentConductFacetServiceImpl(linkRepository, auditLogService);
    }

    @Test
    @DisplayName("null parentId → 401 AUTH_REQUIRED, no audit row")
    void nullParent_returns401() {
        assertThatThrownBy(() -> service.getConductForChild(null, CHILD_ID, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("null childId → 400 BAD_REQUEST, no audit row")
    void nullChild_returns400() {
        assertThatThrownBy(() -> service.getConductForChild(PARENT_ID, null, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    /**
     * BR-PARENT-FACET-CONDUCT-001 (foundation) + BR-PARENT-FACET-CONDUCT-002
     * (remainder): unlinked parent rejected with 403 PARENT_FACET_FORBIDDEN
     * BEFORE any data is touched. The boolean form of the link query is used
     * so a non-linked caller never reaches downstream sensitive sources.
     */
    @Test
    @DisplayName("BR-CONDUCT-001: unlinked parent → 403 PARENT_FACET_FORBIDDEN, no audit row")
    void unlinkedParent_returns403_noAudit() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    /**
     * BR-PARENT-FACET-CONDUCT-002: with no `Incident.visibilityScope` field
     * shipped in this wave (state-check 2026-05-04 confirmed 0 matches), the
     * facet stays a v1 stub returning empty list. Stub-stay is the SAFE
     * default per PDPL Decree 13/2023 Art 16 (children's data minimization)
     * — surfacing unverified `REPORTED` incidents to parents without a
     * vetted visibility filter risks leaking unfounded accusations. Real
     * wiring follows in GAP-321b.1-conduct-incident-visibility once the
     * visibility column lands.
     */
    @Test
    @DisplayName("BR-CONDUCT-002: linked parent → empty list (v1 stub) + audit row emitted")
    void linkedParent_returnsEmpty_auditEmitted() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);

        List<ParentConductFacetResponse> result =
                service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026");

        assertThat(result).isEmpty();
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.CONDUCT);
    }

    /**
     * BR-PARENT-FACET-CONDUCT-002 visibility-scope edge case: even when an
     * underlying STAFF_ONLY incident exists (future schema), the parent
     * facet MUST NOT surface it. The current v1 stub satisfies this
     * trivially — empty list contains no STAFF_ONLY rows. This test is the
     * negative regression contract: the day GAP-321b.1 lands real wiring,
     * this test must continue to pass (i.e., STAFF_ONLY incidents must be
     * filtered out at the JPQL level by the future visibility predicate).
     */
    @Test
    @DisplayName("BR-CONDUCT-002 edge: STAFF_ONLY-equivalent incidents must NOT appear in parent results")
    void staffOnlyIncidentEquivalent_notExposedToParent() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);

        List<ParentConductFacetResponse> result =
                service.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026");

        // Stub returns []; the assertion is the regression contract that
        // when GAP-321b.1 lands and starts hitting `Incident`, STAFF_ONLY
        // rows must remain absent from the response.
        assertThat(result).noneMatch(r -> "STAFF_ONLY".equals(r.rating()));
        assertThat(result).noneMatch(r -> r.remark() != null && r.remark().contains("STAFF"));
    }
}
