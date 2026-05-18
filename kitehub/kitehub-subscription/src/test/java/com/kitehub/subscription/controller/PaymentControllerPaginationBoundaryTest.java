package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-432 (Wave 92 Bucket B) — pagination boundary tests for
 * {@link PaymentController#getAllPayments(com.kitehub.platform.domain.enums.PaymentStatus, int, int)}.
 *
 * <p>Wave 41 PR #1000 introduced the bounded path; this test class hardens
 * the per-request safety net by exercising the page-size sanitiser at its
 * 4 documented edges:</p>
 *
 * <ol>
 *   <li>Page index below zero — must clamp to {@code 0}.</li>
 *   <li>Page size below 1 — must clamp to {@code 1}.</li>
 *   <li>Page size above {@code MAX_PAGE_SIZE} (200) — must clamp to 200.</li>
 *   <li>Page index beyond available data — returns empty page (no crash).</li>
 * </ol>
 *
 * <p>These boundaries are the runtime contract that prevents a client from
 * re-introducing the unbounded scan symptom GAP-432 originally surfaced.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController GAP-432 pagination boundary tests")
class PaymentControllerPaginationBoundaryTest {

    @Mock private PaymentService paymentService;

    @InjectMocks private PaymentController paymentController;

    private Page<PaymentResponse> emptyStub(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0L);
    }

    @Test
    @DisplayName("size > MAX_PAGE_SIZE (200) is capped at 200")
    void sizeAboveMaxIsCappedAt200() {
        when(paymentService.getAllPayments(any(), any(Pageable.class)))
            .thenAnswer(inv -> emptyStub(inv.getArgument(1)));

        ResponseEntity<Page<PaymentResponse>> response =
            paymentController.getAllPayments(null, 0, 9999);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getAllPayments(any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("size < 1 is clamped up to 1")
    void sizeBelowOneIsClampedToOne() {
        when(paymentService.getAllPayments(any(), any(Pageable.class)))
            .thenAnswer(inv -> emptyStub(inv.getArgument(1)));

        paymentController.getAllPayments(null, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getAllPayments(any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("page index < 0 is clamped up to 0")
    void pageIndexBelowZeroIsClampedToZero() {
        when(paymentService.getAllPayments(any(), any(Pageable.class)))
            .thenAnswer(inv -> emptyStub(inv.getArgument(1)));

        paymentController.getAllPayments(null, -5, 50);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getAllPayments(any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("page index beyond available data returns empty content (no crash)")
    void pageIndexBeyondDataReturnsEmpty() {
        Pageable pageable = PageRequest.of(999, 50);
        when(paymentService.getAllPayments(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        ResponseEntity<Page<PaymentResponse>> response =
            paymentController.getAllPayments(null, 999, 50);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("page size exactly MAX_PAGE_SIZE (200) is accepted untouched")
    void pageSizeAtMaxIsKept() {
        when(paymentService.getAllPayments(any(), any(Pageable.class)))
            .thenAnswer(inv -> emptyStub(inv.getArgument(1)));

        paymentController.getAllPayments(null, 0, 200);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getAllPayments(any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(200);
    }
}
