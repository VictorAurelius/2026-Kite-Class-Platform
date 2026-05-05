package com.kiteclass.core.module.parent.service.impl;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.parent.audit.ParentReadAuditLogService;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ConsentService;
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
import static org.mockito.Mockito.when;

/**
 * End-to-end gate proof: facet service rejects with 403
 * {@code PARENT_CONSENT_REQUIRED} when {@link ConsentService#checkConsent}
 * returns false — even when the parent IS linked to the child.
 *
 * <p>This is the "fees gated end-to-end" AC item from GAP-321c Phase 1C v1.
 * Pairs with {@link ConsentServiceImplTest} (which proves the gate matrix
 * directly) by proving the wiring inside the facet impl.
 *
 * <p>Per memory {@code feedback_webmvctest_mock_reset.md}: this test class
 * uses the standard Mockito extension (not WebMvcTest), so per-method mock
 * reset is automatic — no @BeforeEach reset needed.
 *
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParentFeesFacetServiceImpl — consent gate IT")
class ParentFeesFacetConsentGateIT {

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
    @DisplayName("BR-PARENT-PORTAL-011: linked parent without fees-consent → 403 PARENT_CONSENT_REQUIRED")
    void linkedParentNoConsent_throws403() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(true);
        when(consentService.checkConsent(PARENT_ID, CHILD_ID, "fees"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getFeesForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_CONSENT_REQUIRED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        // Audit row MUST NOT be emitted for denied reads (mirrors the
        // BR-PARENT-FACET-*-001 pattern from Phase 1B foundation).
        verify(auditLogService, never()).logRead(any(), any(), any());
        // Invoice store MUST NOT be touched.
        verify(invoiceRepository, never())
                .findByStudentIdAndDueDateRange(any(), any(), any(), any());
    }

    @Test
    @DisplayName("BR-PARENT-PORTAL-011: scope-guard fails BEFORE consent check (denied=403 PARENT_FACET_FORBIDDEN)")
    void scopeGuardFails_doesNotCheckConsent() {
        when(linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(PARENT_ID, CHILD_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getFeesForChild(
                PARENT_ID, CHILD_ID, FROM, TO, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PARENT_FACET_FORBIDDEN");

        verify(consentService, never()).checkConsent(any(), any(), any());
    }
}
