package com.kiteclass.core.module.payment.gateway.impl;

import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.gateway.PaymentGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ZaloPay payment gateway client stub.
 * To be implemented with actual ZaloPay API integration.
 *
 * @since 1.0.0
 */
@Component("zalopayGatewayClient")
@Slf4j
public class ZaloPayGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentInitiationResponse initiatePayment(PaymentGatewayRequest request) {
        log.warn("ZaloPay gateway integration not implemented yet");
        throw new UnsupportedOperationException("ZaloPay integration not implemented yet");
    }

    @Override
    public boolean verifySignature(Map<String, String> params, String signature) {
        log.warn("ZaloPay signature verification not implemented yet");
        throw new UnsupportedOperationException("ZaloPay integration not implemented yet");
    }

    @Override
    public PaymentStatus queryPaymentStatus(String transactionId) {
        log.warn("ZaloPay queryPaymentStatus not implemented yet");
        throw new UnsupportedOperationException("ZaloPay integration not implemented yet");
    }

    @Override
    public void processRefund(String transactionId, BigDecimal amount) {
        log.warn("ZaloPay processRefund not implemented yet");
        throw new UnsupportedOperationException("ZaloPay integration not implemented yet");
    }
}
