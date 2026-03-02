package com.kiteclass.core.module.payment.event;

import com.kiteclass.core.module.payment.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a payment is refunded.
 * Triggers invoice amount update.
 *
 * @author KiteClass Team
 * @since 2.8.1
 */
@Getter
public class PaymentRefundedEvent extends ApplicationEvent {

    private final Payment payment;

    public PaymentRefundedEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }
}
