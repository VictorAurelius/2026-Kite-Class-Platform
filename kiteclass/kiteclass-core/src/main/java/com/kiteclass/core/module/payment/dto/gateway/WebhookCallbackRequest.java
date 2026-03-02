package com.kiteclass.core.module.payment.dto.gateway;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for webhook callback from payment gateway.
 *
 * @since 1.0.0
 */
@Data
@Builder
public class WebhookCallbackRequest {
    private String transactionId;
    private String gatewayTransactionId;
    private BigDecimal amount;
    private String status;
    private String signature;
    private Map<String, String> rawParams;
}
