package com.kiteclass.core.module.payment.dto.gateway;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO from gateway after initiating payment.
 * Contains payment URL and QR code for user to complete payment.
 *
 * @since 1.0.0
 */
@Data
@Builder
public class PaymentInitiationResponse {
    private String paymentUrl;
    private String qrCodeUrl;
    private LocalDateTime expiresAt;
    private Map<String, String> extraData;
}
