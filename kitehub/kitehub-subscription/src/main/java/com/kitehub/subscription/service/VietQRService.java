package com.kitehub.subscription.service;

import com.kitehub.subscription.dto.vietqr.VietQRRequest;
import com.kitehub.subscription.dto.vietqr.VietQRResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;

    @Value("${payment.vietqr.bank-code:VCB}")
    private String bankCode;

    @Value("${payment.vietqr.account-number:1234567890}")
    private String accountNumber;

    @Value("${payment.vietqr.account-name:CONG TY KITECLASS}")
    private String accountName;

    @Value("${payment.vietqr.api-url:https://api.vietqr.io/v2/generate}")
    private String apiUrl;

    /**
     * Wave flow-kh3 Finding #4: default flipped to {@code true} to match the
     * {@code application.yml} top-level binding ({@code payment.vietqr.mock-mode}) and to
     * make the safe-by-default behaviour explicit in the source. Production overrides via
     * compose env ({@code PAYMENT_MOCK_MODE=false}) once a real VietQR API key is provisioned.
     */
    @Value("${payment.vietqr.mock-mode:true}")
    private boolean mockMode;

    @Value("${payment.vietqr.api-key:#{null}}")
    private String apiKey;

    @Value("${payment.vietqr.template:compact}")
    private String defaultTemplate;

    /**
     * Generate VietQR code for payment.
     *
     * @param paymentId Payment UUID
     * @param amountVnd Payment amount in VND
     * @param subscriptionId Subscription ID for payment content
     * @return QR code URL
     */
    public String generateQRCode(UUID paymentId, Long amountVnd, UUID subscriptionId) {
        log.info("Generating VietQR code for payment: {} (amount: {} VND)", paymentId, amountVnd);

        if (mockMode) {
            log.info("[MOCK] Returning mock QR code URL for payment: {}", paymentId);
            return String.format("https://placehold.co/300x300/4CAF50/white?text=MOCK+QR%%0A%s%%0A%d+VND",
                paymentId.toString().substring(0, 8), amountVnd);
        }

        String paymentContent = generatePaymentContent(subscriptionId);

        try {
            // Build VietQR API request
            VietQRRequest request = VietQRRequest.builder()
                .acqId(bankCode)
                .accountNo(accountNumber)
                .accountName(accountName)
                .amount(amountVnd)
                .addInfo(paymentContent)
                .template(defaultTemplate)
                .build();

            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-client-id", apiKey);
            }

            HttpEntity<VietQRRequest> httpEntity = new HttpEntity<>(request, headers);

            // Call VietQR API
            log.debug("Calling VietQR API: POST {}", apiUrl);
            ResponseEntity<VietQRResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                httpEntity,
                VietQRResponse.class
            );

            // Handle response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                VietQRResponse vietQRResponse = response.getBody();

                if ("00".equals(vietQRResponse.getCode()) && vietQRResponse.getData() != null) {
                    String qrDataUrl = vietQRResponse.getData().getQrDataUrl();
                    log.info("Successfully generated VietQR code: {}", qrDataUrl);
                    return qrDataUrl;
                } else {
                    log.error("VietQR API returned error: {} - {}",
                        vietQRResponse.getCode(), vietQRResponse.getDescription());
                    throw new RuntimeException("Failed to generate QR code: " + vietQRResponse.getDescription());
                }
            } else {
                log.error("VietQR API returned unexpected status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to generate QR code: unexpected response");
            }

        } catch (RestClientException e) {
            log.error("Failed to call VietQR API", e);

            // Fallback to public image URL if API fails
            String fallbackUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-%s.jpg?amount=%d&addInfo=%s",
                bankCode,
                accountNumber,
                defaultTemplate,
                amountVnd,
                paymentContent
            );

            log.warn("Using fallback QR URL: {}", fallbackUrl);
            return fallbackUrl;
        }
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
     *
     * <p><strong>IMPORTANT:</strong> Automated payment verification requires integration
     * with bank APIs (e.g., Vietcombank API, TPBank API) which need:
     * <ul>
     *   <li>Bank API credentials and authentication</li>
     *   <li>Webhook setup for real-time notifications</li>
     *   <li>Transaction polling mechanisms</li>
     * </ul>
     *
     * <p>Current implementation returns false (manual verification required).
     * For production use, implement one of:
     * <ol>
     *   <li>Bank API integration (requires credentials)</li>
     *   <li>Payment gateway webhook (e.g., VNPay, MoMo)</li>
     *   <li>Admin dashboard for manual verification</li>
     * </ol>
     *
     * @param transactionId Bank transaction ID
     * @param expectedAmount Expected payment amount in VND
     * @param expectedContent Expected payment content/description
     * @return true if payment is verified, false if verification failed or not implemented
     */
    public boolean verifyPayment(String transactionId, Long expectedAmount, String expectedContent) {
        log.info("Verifying payment transaction: {} (amount: {}, content: {})",
            transactionId, expectedAmount, expectedContent);

        try {
            // Bank API integration would go here
            // Example (pseudocode):
            // BankTransaction transaction = bankApiClient.getTransaction(transactionId);
            // return transaction.getAmount().equals(expectedAmount) &&
            //        transaction.getContent().contains(expectedContent) &&
            //        transaction.getStatus().equals("SUCCESS");

            log.warn("Automated payment verification not implemented - requires bank API integration");
            log.info("Manual verification required for transaction: {}", transactionId);

            // Return false to indicate manual verification is needed
            // Admin should verify payment manually via bank statement
            return false;

        } catch (Exception e) {
            log.error("Error during payment verification for transaction: {}", transactionId, e);
            return false;
        }
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

    public String getBankCode() {
        return bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }
}
