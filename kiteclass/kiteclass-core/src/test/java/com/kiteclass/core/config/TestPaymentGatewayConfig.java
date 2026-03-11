package com.kiteclass.core.config;

import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.gateway.PaymentGatewayClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration for Payment Gateway clients.
 * Provides mock implementations for integration tests.
 *
 * @author KiteClass Team
 * @since 2.15
 */
@TestConfiguration
public class TestPaymentGatewayConfig {

    /**
     * Mock VNPAY gateway client for tests.
     * Always returns successful payment initiation and valid signatures.
     */
    @Bean
    @Primary
    public PaymentGatewayClient vnpayGatewayClient() {
        PaymentGatewayClient mock = mock(PaymentGatewayClient.class);

        // Mock initiatePayment - returns payment URL
        when(mock.initiatePayment(any(PaymentGatewayRequest.class))).thenAnswer(invocation -> {
            PaymentGatewayRequest request = invocation.getArgument(0);
            return PaymentInitiationResponse.builder()
                    .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=" + request.getTransactionId())
                    .qrCodeUrl("https://img.vietqr.io/test/" + request.getTransactionId())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
        });

        // Mock verifySignature - always returns true for tests
        when(mock.verifySignature(any(), anyString())).thenReturn(true);

        // Mock queryPaymentStatus - returns PENDING by default
        when(mock.queryPaymentStatus(anyString())).thenReturn(PaymentStatus.PENDING);

        return mock;
    }

    /**
     * Mock MoMo gateway client for tests.
     * Always returns successful payment initiation and valid signatures.
     */
    @Bean
    @Primary
    public PaymentGatewayClient momoGatewayClient() {
        PaymentGatewayClient mock = mock(PaymentGatewayClient.class);

        // Mock initiatePayment
        when(mock.initiatePayment(any(PaymentGatewayRequest.class))).thenAnswer(invocation -> {
            PaymentGatewayRequest request = invocation.getArgument(0);
            return PaymentInitiationResponse.builder()
                    .paymentUrl("https://test-payment.momo.vn/gw_payment/transactionProcessor?partnerCode=TEST&orderId=" + request.getTransactionId())
                    .qrCodeUrl("https://test-payment.momo.vn/qr/" + request.getTransactionId())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
        });

        // Mock verifySignature - always returns true for tests
        when(mock.verifySignature(any(), anyString())).thenReturn(true);

        // Mock queryPaymentStatus
        when(mock.queryPaymentStatus(anyString())).thenReturn(PaymentStatus.PENDING);

        return mock;
    }

    /**
     * Mock ZaloPay gateway client for tests.
     * Always returns successful payment initiation and valid signatures.
     */
    @Bean
    @Primary
    public PaymentGatewayClient zalopayGatewayClient() {
        PaymentGatewayClient mock = mock(PaymentGatewayClient.class);

        // Mock initiatePayment
        when(mock.initiatePayment(any(PaymentGatewayRequest.class))).thenAnswer(invocation -> {
            PaymentGatewayRequest request = invocation.getArgument(0);
            return PaymentInitiationResponse.builder()
                    .paymentUrl("https://sb-openapi.zalopay.vn/v2/create?app_trans_id=" + request.getTransactionId())
                    .qrCodeUrl("https://qr.zalopay.vn/" + request.getTransactionId())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
        });

        // Mock verifySignature - always returns true for tests
        when(mock.verifySignature(any(), anyString())).thenReturn(true);

        // Mock queryPaymentStatus
        when(mock.queryPaymentStatus(anyString())).thenReturn(PaymentStatus.PENDING);

        return mock;
    }
}
