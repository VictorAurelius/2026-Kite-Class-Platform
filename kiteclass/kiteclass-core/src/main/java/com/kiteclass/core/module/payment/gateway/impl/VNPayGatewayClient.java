package com.kiteclass.core.module.payment.gateway.impl;

import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.gateway.PaymentGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VNPay payment gateway client implementation.
 * Uses HMAC SHA512 for signature verification.
 *
 * @since 1.0.0
 */
@Component("vnpayGatewayClient")
@RequiredArgsConstructor
@Slf4j
public class VNPayGatewayClient implements PaymentGatewayClient {

    @Value("${payment.vnpay.api-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String apiUrl;

    @Value("${payment.vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${payment.vnpay.hash-secret:}")
    private String hashSecret;

    @Override
    public PaymentInitiationResponse initiatePayment(PaymentGatewayRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(request.getAmount().longValue() * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", request.getTransactionId());
        params.put("vnp_OrderInfo", request.getOrderInfo());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", request.getReturnUrl());
        params.put("vnp_IpAddr", request.getIpAddress() != null ? request.getIpAddress() : "127.0.0.1");
        params.put("vnp_CreateDate",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        // Generate signature
        String signData = buildSignData(params);
        String signature = hmacSHA512(hashSecret, signData);
        params.put("vnp_SecureHash", signature);

        String paymentUrl = apiUrl + "?" + buildQueryString(params);

        log.info("Generated VNPay payment URL for transaction {}", request.getTransactionId());

        return PaymentInitiationResponse.builder()
            .paymentUrl(paymentUrl)
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .build();
    }

    @Override
    public boolean verifySignature(Map<String, String> params, String signature) {
        Map<String, String> signParams = new HashMap<>(params);
        signParams.remove("vnp_SecureHash");
        signParams.remove("vnp_SecureHashType");

        String signData = buildSignData(signParams);
        String calculatedSignature = hmacSHA512(hashSecret, signData);

        boolean valid = calculatedSignature.equals(signature);
        log.debug("VNPay signature verification: {}", valid ? "VALID" : "INVALID");

        return valid;
    }

    @Override
    public PaymentStatus queryPaymentStatus(String transactionId) {
        // VNPay API call to query transaction status
        // For now, return PENDING (to be implemented with actual API call)
        log.warn("VNPay queryPaymentStatus not implemented yet");
        return PaymentStatus.PENDING;
    }

    @Override
    public void processRefund(String transactionId, BigDecimal amount) {
        // VNPay refund API call
        // To be implemented with actual API call
        log.warn("VNPay processRefund not implemented yet for transaction {}", transactionId);
        throw new UnsupportedOperationException("VNPay refund not implemented yet");
    }

    /**
     * Builds sign data string from parameters (sorted by key).
     *
     * @param params parameters map
     * @return sign data string
     */
    private String buildSignData(Map<String, String> params) {
        return params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
    }

    /**
     * Builds query string from parameters.
     *
     * @param params parameters map
     * @return query string
     */
    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
    }

    /**
     * Generates HMAC SHA512 signature.
     *
     * @param key secret key
     * @param data data to sign
     * @return hex-encoded signature
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(secretKey);
            byte[] hash = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Failed to generate HMAC SHA512 signature", e);
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    /**
     * Converts byte array to hex string.
     *
     * @param bytes byte array
     * @return hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
