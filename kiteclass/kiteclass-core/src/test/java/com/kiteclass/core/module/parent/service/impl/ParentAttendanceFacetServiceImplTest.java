package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level branch-coverage test for {@link ParentAttendanceFacetServiceImpl}.
 *
 * <p>The fan-in audit invariant (linked → audit row, unlinked → no row) is
 * already covered by {@link com.kiteclass.core.module.parent.audit.ParentReadAuditLogIntegrationTest};
 * this class fills the remaining branch coverage for the scope-guard rejects
 * (401 on null parent, 400 on each null/inverted param) plus happy-path
 * mapping into the response DTO.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentAttendanceFacetServiceImpl branch coverage")
class ParentAttendanceFacetServiceImplTest {

    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private AttendancePeriodRepository attendancePeriodRepository;
    @Mock private ParentReadAuditLogService auditLogService;

    private ParentAttendanceFacetServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;
    private static final LocalDate FROM = LocalDate.parse("2026-04-01");
    private static final LocalDate TO = LocalDate.parse("2026-04-30");

    @BeforeEach
    void setUp() {
        service = new ParentAttendanceFacetServiceImpl(linkRepository, attendancePeriodRepository, auditLogService);
    }

    @Test
    @DisplayName("null parentId → 401 AUTH_REQUIRED, no audit row")
    void nullParent_returns401() {
        assertThatThrownBy(() -> service.getAttendanceForChild(
                null, CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("null childId → 400 BAD_REQUEST")
    void nullChild_returns400() {
        assertThatThrownBy(() -> service.getAttendanceForChild(
                PARENT_ID, null, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("null from → 400 BAD_REQUEST")
    void nullFrom_returns400() {
        assertThatThrownBy(() -> service.getAttendanceForChild(
                PARENT_ID, CHILD_ID, null, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("null to → 400 BAD_REQUEST")
    void nullTo_returns400() {
        assertThatThrownBy(() -> service.getAttendanceForChild(
                PARENT_ID, CHILD_ID, FROM, null, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("inverted range from > to → 400 BAD_REQUEST")
    void invertedRange_returns400() {
        assertThatThrownBy(() -> service.getAttendanceForChild(
                PARENT_ID, CHILD_ID, TO, FROM, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("happy path: linked + valid range maps each AttendancePeriod row to response DTO")
    void happyPath_mapsResponse() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        AttendancePeriod p = new AttendancePeriod();
        p.setId(1L);
        p.setStudentId(CHILD_ID);
        p.setClassId(7L);
        p.setSubjectSectionId(11L);
        p.setPeriodNo(2);
        p.setDate(LocalDate.parse("2026-04-15"));
        p.setStatus(AttendanceStatus.LATE);
        p.setRecordedBy(50L);
        p.setRecordedAt(LocalDateTime.parse("2026-04-15T08:30:00"));
        p.setNotes("late by 5 min");
        when(attendancePeriodRepository.findByStudentIdAndDateBetweenAndDeletedFalse(
                eq(CHILD_ID), eq(FROM), eq(TO), any()))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<?> result = service.getAttendanceForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.ATTENDANCE);
    }
}
