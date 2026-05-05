package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.parent.audit.ParentFacet;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.dto.ParentFeeFacetResponse;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Unit-level branch-coverage test for {@link ParentFeesFacetServiceImpl}.
 *
 * <p>Covers the scope-guard rejects (401/400) and the {@code toResponse}
 * mapping including the {@code status != null ? .name() : null} ternary
 * which is otherwise uncovered.
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentFeesFacetServiceImpl branch coverage")
class ParentFeesFacetServiceImplTest {

    @Mock private ParentStudentLinkRepository linkRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ParentReadAuditLogService auditLogService;
    @Mock private ConsentService consentService;

    private ParentFeesFacetServiceImpl service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;
    private static final LocalDate FROM = LocalDate.parse("2026-04-01");
    private static final LocalDate TO = LocalDate.parse("2026-04-30");

    @BeforeEach
    void setUp() {
        service = new ParentFeesFacetServiceImpl(
                linkRepository, invoiceRepository, auditLogService, consentService);
    }

    @Test
    @DisplayName("null parentId → 401 AUTH_REQUIRED, no audit row")
    void nullParent_returns401() {
        assertThatThrownBy(() -> service.getFeesForChild(
                null, CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(auditLogService, never()).logRead(any(), any(), any());
    }

    @Test
    @DisplayName("null childId → 400 BAD_REQUEST")
    void nullChild_returns400() {
        assertThatThrownBy(() -> service.getFeesForChild(
                PARENT_ID, null, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("null from → 400 BAD_REQUEST")
    void nullFrom_returns400() {
        assertThatThrownBy(() -> service.getFeesForChild(
                PARENT_ID, CHILD_ID, null, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("null to → 400 BAD_REQUEST")
    void nullTo_returns400() {
        assertThatThrownBy(() -> service.getFeesForChild(
                PARENT_ID, CHILD_ID, FROM, null, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("inverted range from > to → 400 BAD_REQUEST")
    void invertedRange_returns400() {
        assertThatThrownBy(() -> service.getFeesForChild(
                PARENT_ID, CHILD_ID, TO, FROM, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("happy path: invoice with non-null status maps status name into DTO")
    void happyPath_mapsStatusName() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "fees"))
                .thenReturn(true);
        Invoice invoice = sampleInvoice(InvoiceStatus.SENT);
        when(invoiceRepository.findByStudentIdAndDueDateRange(eq(CHILD_ID), eq(FROM), eq(TO), any()))
                .thenReturn(new PageImpl<>(List.of(invoice)));

        Page<ParentFeeFacetResponse> result = service.getFeesForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        ParentFeeFacetResponse dto = result.getContent().get(0);
        assertThat(dto.invoiceNumber()).isEqualTo("INV-2026-0001");
        assertThat(dto.status()).isEqualTo("SENT");
        assertThat(dto.studentId()).isEqualTo(CHILD_ID);
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.FEES);
    }

    @Test
    @DisplayName("happy path: invoice with null status maps to null in DTO (covers ternary false branch)")
    void happyPath_nullStatusMapsToNull() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "fees"))
                .thenReturn(true);
        Invoice invoice = sampleInvoice(null);
        when(invoiceRepository.findByStudentIdAndDueDateRange(eq(CHILD_ID), eq(FROM), eq(TO), any()))
                .thenReturn(new PageImpl<>(List.of(invoice)));

        Page<ParentFeeFacetResponse> result = service.getFeesForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isNull();
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.FEES);
    }

    /**
     * BR-PARENT-FACET-FEES-002: real query MUST narrow by date range. The
     * stub used {@code findByStudentIdAndDeletedFalse} (no range arg);
     * remainder switches to {@code findByStudentIdAndDueDateRange(child, from, to, pageable)}.
     */
    @Test
    @DisplayName("BR-FEES-002: service narrows by dueDate range — stub-fallback path is gone")
    void realWiring_callsRangeNarrowingRepoMethod() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "fees"))
                .thenReturn(true);
        when(invoiceRepository.findByStudentIdAndDueDateRange(
                eq(CHILD_ID), eq(FROM), eq(TO), any()))
                .thenReturn(new PageImpl<>(List.of(sampleInvoice(InvoiceStatus.SENT))));

        Page<ParentFeeFacetResponse> page = service.getFeesForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        verify(invoiceRepository, times(1)).findByStudentIdAndDueDateRange(
                eq(CHILD_ID), eq(FROM), eq(TO), any());
        // Old stub path must no longer be reached.
        verify(invoiceRepository, never()).findByStudentIdAndDeletedFalse(any(), any());
    }

    @Test
    @DisplayName("BR-FEES-002: empty page when no invoice in range — audit row still emitted")
    void realWiring_emptyRange_auditStillEmitted() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "fees"))
                .thenReturn(true);
        when(invoiceRepository.findByStudentIdAndDueDateRange(
                eq(CHILD_ID), eq(FROM), eq(TO), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ParentFeeFacetResponse> page = service.getFeesForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        verify(auditLogService, times(1)).logRead(PARENT_ID, CHILD_ID, ParentFacet.FEES);
    }

    private Invoice sampleInvoice(InvoiceStatus status) {
        Invoice i = new Invoice();
        i.setId(1L);
        i.setStudentId(CHILD_ID);
        i.setInvoiceNumber("INV-2026-0001");
        i.setStatus(status);
        i.setSubtotal(new BigDecimal("1500000.00"));
        i.setDiscount(BigDecimal.ZERO);
        i.setTotal(new BigDecimal("1500000.00"));
        i.setAmountPaid(BigDecimal.ZERO);
        i.setDueDate(LocalDate.parse("2026-04-30"));
        return i;
    }
}
