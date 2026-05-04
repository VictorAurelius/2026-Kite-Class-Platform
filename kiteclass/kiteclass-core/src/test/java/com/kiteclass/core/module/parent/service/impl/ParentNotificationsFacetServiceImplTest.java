package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
