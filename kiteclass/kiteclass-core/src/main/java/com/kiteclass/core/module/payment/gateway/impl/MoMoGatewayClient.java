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
 * MoMo payment gateway client stub.
 * To be implemented with actual MoMo API integration.
 *
 * @since 1.0.0
 */
@Component("momoGatewayClient")
@Slf4j
public class MoMoGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentInitiationResponse initiatePayment(PaymentGatewayRequest request) {
        log.warn("MoMo gateway integration not implemented yet");
        throw new UnsupportedOperationException("MoMo integration not implemented yet");
    }

    @Override
    public boolean verifySignature(Map<String, String> params, String signature) {
        log.warn("MoMo signature verification not implemented yet");
        throw new UnsupportedOperationException("MoMo integration not implemented yet");
    }

    @Override
    public PaymentStatus queryPaymentStatus(String transactionId) {
        log.warn("MoMo queryPaymentStatus not implemented yet");
        throw new UnsupportedOperationException("MoMo integration not implemented yet");
    }

    @Override
    public void processRefund(String transactionId, BigDecimal amount) {
        log.warn("MoMo processRefund not implemented yet");
        throw new UnsupportedOperationException("MoMo integration not implemented yet");
    }
}
