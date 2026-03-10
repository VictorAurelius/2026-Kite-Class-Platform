package com.kitehub.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for VietQR payment integration.
 * Generates QR codes for bank transfers.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VietQRService {

    @Value("${payment.vietqr.bank-code:VCB}")
    private String bankCode;

    @Value("${payment.vietqr.account-number:1234567890}")
    private String accountNumber;

    @Value("${payment.vietqr.account-name:CONG TY KITECLASS}")
    private String accountName;

    @Value("${payment.vietqr.api-url:https://api.vietqr.io/v2/generate}")
    private String apiUrl;

    /**
     * Generate VietQR code for payment.
     * TODO: Integrate with real VietQR API when credentials are available.
     *
     * @param paymentId Payment UUID
     * @param amountVnd Payment amount in VND
     * @param subscriptionId Subscription ID for payment content
     * @return QR code URL
     */
    public String generateQRCode(UUID paymentId, Long amountVnd, UUID subscriptionId) {
        log.info("Generating VietQR code for payment: {} (amount: {} VND)", paymentId, amountVnd);

        String paymentContent = generatePaymentContent(subscriptionId);

        // TODO: Call real VietQR API
        // For MVP: Return mock QR code URL
        String mockQrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-%s.jpg?amount=%d&addInfo=%s",
            bankCode,
            accountNumber,
            "compact",
            amountVnd,
            paymentContent
        );

        log.info("Generated QR code URL: {}", mockQrUrl);
        return mockQrUrl;
    }

    /**
     * Generate payment content/description.
     * Format: "KITECLASS {subscription_id_short}"
     *
     * @param subscriptionId Subscription UUID
     * @return Payment content
     */
    public String generatePaymentContent(UUID subscriptionId) {
        String shortId = subscriptionId.toString().substring(0, 8).toUpperCase();
        return "KITECLASS " + shortId;
    }

    /**
     * Verify payment transaction.
     * TODO: Integrate with bank API to verify transaction.
     *
     * @param transactionId Bank transaction ID
     * @param expectedAmount Expected payment amount
     * @param expectedContent Expected payment content
     * @return true if payment is verified
     */
    public boolean verifyPayment(String transactionId, Long expectedAmount, String expectedContent) {
        log.info("Verifying payment transaction: {} (amount: {}, content: {})",
            transactionId, expectedAmount, expectedContent);

        // TODO: Query bank API to verify transaction
        // For MVP: Mock verification (always returns true for testing)
        log.warn("Payment verification not implemented - using mock (returns true)");
        return true;
    }

    /**
     * Get bank information for manual transfer.
     *
     * @return Bank info string
     */
    public String getBankInfo() {
        return String.format(
            "Bank: %s\nAccount: %s\nName: %s",
            bankCode,
            accountNumber,
            accountName
        );
    }
}
