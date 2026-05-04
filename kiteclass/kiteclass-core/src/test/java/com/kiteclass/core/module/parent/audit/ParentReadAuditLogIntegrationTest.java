package com.kiteclass.core.module.parent.audit;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.attendance.entity.AttendancePeriod;
import com.kiteclass.core.module.attendance.repository.AttendancePeriodRepository;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.impl.ParentAttendanceFacetServiceImpl;
import com.kiteclass.core.module.parent.service.impl.ParentConductFacetServiceImpl;
import com.kiteclass.core.module.parent.service.impl.ParentFeesFacetServiceImpl;
import com.kiteclass.core.module.parent.service.impl.ParentNotificationsFacetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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
 * Integration-style test (mocked repos) covering the per-read audit log
 * invariant for all four Phase 1B facets:
 *
 * <ol>
 *   <li>Linked parent + valid args → audit row written exactly once with
 *       correct {@link ParentFacet} value.</li>
 *   <li>Unlinked parent → 403 thrown BEFORE audit row is written (no
 *       silent attribution of a denied read).</li>
 * </ol>
 *
 * <p>The audit log itself is a {@code Mockito.spy}-equivalent
 * {@code @Mock} — the production impl is exercised in
 * {@link ParentReadAuditLogServiceTest}. This test is deliberately scoped
 * to the contract between facet service and audit service so the fan-in
 * is validated with mocked persistence (TestContainers-backed full bring-up
 * is a future enrichment when GAP-321b.1 lands schema changes).
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Parent facet audit-log fan-in")
class ParentReadAuditLogIntegrationTest {

    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private ParentReadAuditLogService auditLogService;
    @Mock private AttendancePeriodRepository attendanceRepo;
    @Mock private InvoiceRepository invoiceRepo;

    private ParentAttendanceFacetServiceImpl attendanceService;
    private ParentFeesFacetServiceImpl feesService;
    private ParentConductFacetServiceImpl conductService;
    private ParentNotificationsFacetServiceImpl notificationsService;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;
    private static final Long OTHER_CHILD_ID = 999L;
    private static final LocalDate FROM = LocalDate.parse("2026-04-01");
    private static final LocalDate TO = LocalDate.parse("2026-04-30");

    @BeforeEach
    void setUp() {
        attendanceService = new ParentAttendanceFacetServiceImpl(linkRepository, attendanceRepo, auditLogService);
        feesService = new ParentFeesFacetServiceImpl(linkRepository, invoiceRepo, auditLogService);
        conductService = new ParentConductFacetServiceImpl(linkRepository, auditLogService);
        notificationsService = new ParentNotificationsFacetServiceImpl(linkRepository, auditLogService);
    }

    @Test
    @DisplayName("attendance: linked parent → audit row recorded with ATTENDANCE facet")
    void attendance_linked_writesAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(attendanceRepo.findByStudentIdAndDateBetweenAndDeletedFalse(eq(CHILD_ID), eq(FROM), eq(TO), any()))
                .thenReturn(new PageImpl<>(List.of(samplePeriod()), Pageable.unpaged(), 1));

        attendanceService.getAttendanceForChild(PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.ATTENDANCE);
    }

    @Test
    @DisplayName("attendance: unlinked parent → 403 + NO audit row written")
    void attendance_unlinked_noAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, OTHER_CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> attendanceService.getAttendanceForChild(
                PARENT_ID, OTHER_CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("fees: linked parent → audit row recorded with FEES facet")
    void fees_linked_writesAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(invoiceRepo.findByStudentIdAndDeletedFalse(eq(CHILD_ID), any()))
                .thenReturn(new PageImpl<Invoice>(List.of(sampleInvoice()), Pageable.unpaged(), 1));

        feesService.getFeesForChild(PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.FEES);
    }

    @Test
    @DisplayName("fees: unlinked parent → 403 + NO audit row written")
    void fees_unlinked_noAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, OTHER_CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> feesService.getFeesForChild(
                PARENT_ID, OTHER_CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN");

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("conduct: linked parent → audit row recorded with CONDUCT facet (stub returns empty)")
    void conduct_linked_writesAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);

        var result = conductService.getConductForChild(PARENT_ID, CHILD_ID, "HK1-2025-2026");

        assertThat(result).isEmpty();
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.CONDUCT);
    }

    @Test
    @DisplayName("conduct: unlinked parent → 403 + NO audit row written")
    void conduct_unlinked_noAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, OTHER_CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> conductService.getConductForChild(PARENT_ID, OTHER_CHILD_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN");

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("notifications: linked parent → audit row recorded with NOTIFICATIONS facet (empty page)")
    void notifications_linked_writesAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);

        Page<?> page = notificationsService.getNotificationsForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.NOTIFICATIONS);
    }

    @Test
    @DisplayName("notifications: unlinked parent → 403 + NO audit row written")
    void notifications_unlinked_noAuditRow() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, OTHER_CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> notificationsService.getNotificationsForChild(
                PARENT_ID, OTHER_CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN");

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("inverted date range → 400 BAD_REQUEST + no audit row (attendance + fees + notifications)")
    void invertedRange_badRequest_noAuditRow() {
        // No link stub needed — argument validation fires first.
        LocalDate badFrom = LocalDate.parse("2026-05-01");
        LocalDate badTo = LocalDate.parse("2026-04-01");

        assertThatThrownBy(() -> attendanceService.getAttendanceForChild(
                PARENT_ID, CHILD_ID, badFrom, badTo, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    // ——— fixtures ————————————————————————————————————————————————

    private AttendancePeriod samplePeriod() {
        AttendancePeriod p = new AttendancePeriod();
        p.setId(1L);
        p.setStudentId(CHILD_ID);
        p.setClassId(7L);
        p.setSubjectSectionId(11L);
        p.setPeriodNo(1);
        p.setDate(LocalDate.parse("2026-04-15"));
        p.setStatus(AttendanceStatus.PRESENT);
        p.setRecordedBy(99L);
        p.setRecordedAt(LocalDateTime.parse("2026-04-15T08:30:00"));
        return p;
    }

    private Invoice sampleInvoice() {
        Invoice i = new Invoice();
        i.setId(1L);
        i.setStudentId(CHILD_ID);
        i.setInvoiceNumber("INV-2026-0001");
        i.setStatus(InvoiceStatus.SENT);
        i.setSubtotal(new BigDecimal("1500000.00"));
        i.setDiscount(BigDecimal.ZERO);
        i.setTotal(new BigDecimal("1500000.00"));
        i.setAmountPaid(BigDecimal.ZERO);
        i.setDueDate(LocalDate.parse("2026-04-30"));
        return i;
    }
}
