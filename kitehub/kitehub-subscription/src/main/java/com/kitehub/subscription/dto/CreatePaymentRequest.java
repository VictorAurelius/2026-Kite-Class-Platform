package com.kitehub.subscription.dto;

import com.kitehub.platform.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for creating a payment.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Data
@Builder
public class CreatePaymentRequest {

    @NotNull(message = "Subscription ID is required")
    private UUID subscriptionId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amountVnd;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
