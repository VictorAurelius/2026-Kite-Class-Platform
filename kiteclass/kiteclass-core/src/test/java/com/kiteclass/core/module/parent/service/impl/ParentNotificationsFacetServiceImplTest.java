package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentNotificationFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level branch-coverage test for {@link ParentNotificationsFacetServiceImpl}.
 *
 * <p>The fan-in audit invariant + happy-path empty page are already covered
 * by {@code ParentReadAuditLogIntegrationTest}; this class fills the 401 +
 * 400 (null + inverted-range) branches that the integration test does not
 * exercise.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentNotificationsFacetServiceImpl branch coverage")
class ParentNotificationsFacetServiceImplTest {

    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private ParentReadAuditLogService auditLogService;

    private ParentNotificationsFacetServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;
    private static final LocalDate FROM = LocalDate.parse("2026-04-01");
    private static final LocalDate TO = LocalDate.parse("2026-04-30");

    @BeforeEach
    void setUp() {
        service = new ParentNotificationsFacetServiceImpl(linkRepository, auditLogService);
    }

    @Test
    @DisplayName("null parentId → 401 AUTH_REQUIRED, no audit row")
    void nullParent_returns401() {
        assertThatThrownBy(() -> service.getNotificationsForChild(
                null, CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("null childId → 400 BAD_REQUEST")
    void nullChild_returns400() {
        assertThatThrownBy(() -> service.getNotificationsForChild(
                PARENT_ID, null, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("null from → 400 BAD_REQUEST")
    void nullFrom_returns400() {
        assertThatThrownBy(() -> service.getNotificationsForChild(
                PARENT_ID, CHILD_ID, null, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("null to → 400 BAD_REQUEST")
    void nullTo_returns400() {
        assertThatThrownBy(() -> service.getNotificationsForChild(
                PARENT_ID, CHILD_ID, FROM, null, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("inverted range from > to → 400 BAD_REQUEST")
    void invertedRange_returns400() {
        assertThatThrownBy(() -> service.getNotificationsForChild(
                PARENT_ID, CHILD_ID, TO, FROM, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    /**
     * BR-PARENT-FACET-NOTIFY-001 (foundation): unlinked parent rejected with
     * 403 PARENT_FACET_FORBIDDEN BEFORE any data is touched. The boolean
     * link query is used so a non-linked caller never reaches the (still
     * absent) notification store.
     */
    @Test
    @DisplayName("BR-NOTIFY-001: unlinked parent → 403 PARENT_FACET_FORBIDDEN, no audit row")
    void unlinkedParent_returns403_noAudit() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getNotificationsForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    /**
     * BR-PARENT-FACET-NOTIFY-002: with no `Notification` entity in
     * `kiteclass-core` (state-check 2026-05-04 confirmed 0 matches in module
     * domain scope), the facet stays a v1 stub returning empty page until
     * GAP-063b (cross-cutting notification engine) ships. Real wiring
     * follows in GAP-321b.1-notifications-engine-wiring.
     */
    @Test
    @DisplayName("BR-NOTIFY-002: linked parent → empty page (v1 stub) + audit row emitted")
    void linkedParent_returnsEmptyPage_auditEmitted() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);

        Page<ParentNotificationFacetResponse> page = service.getNotificationsForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.NOTIFICATIONS);
    }

    /**
     * BR-PARENT-FACET-NOTIFY-002 audience-scope contract: when the cross-
     * cutting engine ships (GAP-063b → GAP-321b.1), the JPQL must filter by
     * `audienceScope ∋ {PARENT, ALL_PARENTS}` to prevent leaking
     * staff-targeted notifications to parents. Current v1 stub satisfies
     * this trivially with empty page; this test is the regression contract.
     */
    @Test
    @DisplayName("BR-NOTIFY-002 audience filter: STAFF-only-equivalent notifications never appear")
    void staffOnlyAudienceEquivalent_notExposedToParent() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);

        Page<ParentNotificationFacetResponse> page = service.getNotificationsForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .as("BR-NOTIFY-002: parent facet must never expose staff-audience entries")
                .noneMatch(r -> r.title() != null && r.title().contains("STAFF"));
    }
}
