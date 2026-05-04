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
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
