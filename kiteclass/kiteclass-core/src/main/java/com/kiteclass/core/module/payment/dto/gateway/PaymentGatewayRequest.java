package com.kiteclass.core.module.payment.dto.gateway;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for initiating payment with gateway.
 *
 * @since 1.0.0
 */
@Data
@Builder
public class PaymentGatewayRequest {
    private String transactionId;
    private BigDecimal amount;
    private String orderInfo;
    private String returnUrl;
    private String notifyUrl;
    private String ipAddress;
    private Map<String, String> extraParams;
}
