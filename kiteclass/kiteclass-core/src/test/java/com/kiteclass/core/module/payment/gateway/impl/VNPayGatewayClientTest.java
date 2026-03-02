package com.kiteclass.core.module.payment.gateway.impl;

import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VNPayGatewayClient}.
 * Tests signature generation and verification logic.
 *
 * @author KiteClass Team
 * @since 2.8.1
 */
@DisplayName("VNPayGatewayClient Tests")
class VNPayGatewayClientTest {

    private VNPayGatewayClient vnpayGatewayClient;

    private static final String TEST_API_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String TEST_TMN_CODE = "TEST_TMN";
    private static final String TEST_HASH_KEY = "TEST_SECRET_KEY_123456";

    @BeforeEach
    void setUp() {
        vnpayGatewayClient = new VNPayGatewayClient();
        ReflectionTestUtils.setField(vnpayGatewayClient, "apiUrl", TEST_API_URL);
        ReflectionTestUtils.setField(vnpayGatewayClient, "tmnCode", TEST_TMN_CODE);
        ReflectionTestUtils.setField(vnpayGatewayClient, "hashSecret", TEST_HASH_KEY);
    }

    @Test
    @DisplayName("Should initiate payment and return payment URL")
    void shouldInitiatePaymentSuccessfully() {
        // Arrange
        PaymentGatewayRequest request = PaymentGatewayRequest.builder()
                .transactionId("TXN1234567890")
                .amount(new BigDecimal("500000.00"))
                .orderInfo("Thanh toán hóa đơn INV-2026-000001")
                .returnUrl("http://localhost:3000/payment/return")
                .notifyUrl("http://localhost:8081/api/v1/payments/webhook/vnpay")
                .ipAddress("192.168.1.1")
                .build();

        // Act
        PaymentInitiationResponse response = vnpayGatewayClient.initiatePayment(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPaymentUrl()).isNotNull();
        assertThat(response.getPaymentUrl()).startsWith(TEST_API_URL);
        assertThat(response.getPaymentUrl()).contains("vnp_TxnRef=TXN1234567890");
        assertThat(response.getPaymentUrl()).contains("vnp_Amount=50000000"); // Amount in VND subunits
        assertThat(response.getPaymentUrl()).contains("vnp_SecureHash=");
        assertThat(response.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Should verify valid signature correctly")
    void shouldVerifyValidSignature() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN1234567890");
        params.put("vnp_Amount", "50000000");
        params.put("vnp_OrderInfo", "Test payment");
        params.put("vnp_ResponseCode", "00");

        // Generate signature using same secret
        String signData = "vnp_Amount=50000000&vnp_OrderInfo=Test payment&vnp_ResponseCode=00&vnp_TxnRef=TXN1234567890";
        String expectedSignature = hmacSHA512(TEST_HASH_KEY, signData);

        // Act
        boolean isValid = vnpayGatewayClient.verifySignature(params, expectedSignature);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid signature")
    void shouldRejectInvalidSignature() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN1234567890");
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "00");

        String invalidSignature = "invalid_signature_hash";

        // Act
        boolean isValid = vnpayGatewayClient.verifySignature(params, invalidSignature);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject tampered parameters")
    void shouldRejectTamperedParameters() {
        // Arrange
        Map<String, String> originalParams = new HashMap<>();
        originalParams.put("vnp_TxnRef", "TXN1234567890");
        originalParams.put("vnp_Amount", "50000000");
        originalParams.put("vnp_ResponseCode", "00");

        // Generate signature for original params
        String signData = "vnp_Amount=50000000&vnp_ResponseCode=00&vnp_TxnRef=TXN1234567890";
        String originalSignature = hmacSHA512(TEST_HASH_KEY, signData);

        // Tamper with amount
        Map<String, String> tamperedParams = new HashMap<>(originalParams);
        tamperedParams.put("vnp_Amount", "10000000"); // Changed amount

        // Act
        boolean isValid = vnpayGatewayClient.verifySignature(tamperedParams, originalSignature);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should ignore vnp_SecureHash parameter in signature verification")
    void shouldIgnoreSecureHashInVerification() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN1234567890");
        params.put("vnp_Amount", "50000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_SecureHash", "should_be_ignored");

        // Generate signature without vnp_SecureHash
        String signData = "vnp_Amount=50000000&vnp_ResponseCode=00&vnp_TxnRef=TXN1234567890";
        String signature = hmacSHA512(TEST_HASH_KEY, signData);

        // Act
        boolean isValid = vnpayGatewayClient.verifySignature(params, signature);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should query payment status - stub implementation")
    void shouldQueryPaymentStatus() {
        // Arrange
        String transactionId = "TXN1234567890";

        // Act
        PaymentStatus status = vnpayGatewayClient.queryPaymentStatus(transactionId);

        // Assert
        // Currently stub implementation
        assertThat(status).isNotNull();
    }

    @Test
    @DisplayName("Should process refund - stub implementation")
    void shouldProcessRefund() {
        // Arrange
        String transactionId = "TXN1234567890";
        BigDecimal amount = new BigDecimal("500000.00");

        // Act & Assert
        // Currently stub implementation - should not throw
        vnpayGatewayClient.processRefund(transactionId, amount);
    }

    /**
     * Helper method to generate HMAC SHA512 signature for testing.
     */
    private String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac sha512Hmac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA512"
            );
            sha512Hmac.init(secretKey);
            byte[] hash = sha512Hmac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
