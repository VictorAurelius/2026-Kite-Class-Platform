package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.enums.PaymentStatus;
import com.kitehub.subscription.dto.PaymentResponse;
import com.kitehub.subscription.repository.PaymentRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-432 (Wave 41 Bucket C) regression tests for
 * {@link PaymentService#getAllPayments(PaymentStatus, Pageable)}.
 *
 * <p>Asserts the new bounded query path is wired correctly and that
 * {@code paymentRepository.findAll()} is never invoked from the admin list
 * endpoint regardless of whether a status filter is supplied.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService GAP-432 bounded query tests")
class PaymentServiceBoundedQueryTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private VietQRService vietQRService;

    @InjectMocks private PaymentService paymentService;

    private Payment samplePayment() {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setSubscriptionId(UUID.randomUUID());
        p.setAmountVnd(500_000L);
        p.setStatus(PaymentStatus.PENDING);
        return p;
    }

    @Test
    @DisplayName("getAllPayments(null, pageable) routes to findAllNotDeleted (not findAll)")
    void noStatusFilter_routesToBoundedFindAllNotDeleted() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<Payment> stub = new PageImpl<>(List.of(samplePayment()), pageable, 1L);
        when(paymentRepository.findAllNotDeleted(any(Pageable.class))).thenReturn(stub);

        Page<PaymentResponse> result = paymentService.getAllPayments(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findAllNotDeleted(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        // Critical invariant: legacy unbounded findAll() must never be reached.
        verify(paymentRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllPayments(status, pageable) routes to findByStatusNotDeleted (not findByStatus list)")
    void withStatusFilter_routesToBoundedFindByStatusNotDeleted() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<Payment> stub = new PageImpl<>(List.of(samplePayment()), pageable, 1L);
        when(paymentRepository.findByStatusNotDeleted(eq(PaymentStatus.PENDING), any(Pageable.class)))
            .thenReturn(stub);

        Page<PaymentResponse> result =
            paymentService.getAllPayments(PaymentStatus.PENDING, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(paymentRepository).findByStatusNotDeleted(eq(PaymentStatus.PENDING), any(Pageable.class));
        verify(paymentRepository, never()).findAll();
        verify(paymentRepository, Mockito.never()).findByStatus(any(PaymentStatus.class));
    }

    @Test
    @DisplayName("Empty page is returned cleanly (mirrors empty admin list)")
    void emptyPageReturnsEmptyResponses() {
        Pageable pageable = PageRequest.of(0, 50);
        when(paymentRepository.findAllNotDeleted(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        Page<PaymentResponse> result = paymentService.getAllPayments(null, pageable);
        assertThat(result.getTotalElements()).isEqualTo(0L);
        assertThat(result.getContent()).isEmpty();
    }
}
