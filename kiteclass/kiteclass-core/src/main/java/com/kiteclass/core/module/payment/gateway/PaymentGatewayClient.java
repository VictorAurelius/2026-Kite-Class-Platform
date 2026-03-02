package com.kiteclass.core.module.payment.gateway;

import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment gateway client interface.
 * Defines contract for integrating with payment gateways (VNPay, MoMo, ZaloPay).
 *
 * @since 1.0.0
 */
public interface PaymentGatewayClient {

    /**
     * Initiates a payment and returns payment URL/QR code.
     *
     * @param request payment gateway request
     * @return payment initiation response with URL and expiry
     */
    PaymentInitiationResponse initiatePayment(PaymentGatewayRequest request);

    /**
     * Verifies webhook signature for security.
     *
     * @param params webhook parameters
     * @param signature signature from gateway
     * @return true if signature is valid
     */
    boolean verifySignature(Map<String, String> params, String signature);

    /**
     * Queries payment status from gateway.
     *
     * @param transactionId transaction ID
     * @return current payment status
     */
    PaymentStatus queryPaymentStatus(String transactionId);

    /**
     * Processes refund for a completed payment.
     *
     * @param transactionId transaction ID
     * @param amount refund amount
     */
    void processRefund(String transactionId, BigDecimal amount);
}
