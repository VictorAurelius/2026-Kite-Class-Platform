package com.kiteclass.core.module.payment.event;

import com.kiteclass.core.module.payment.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a payment is completed successfully.
 * Triggers invoice update and receipt generation.
 *
 * @since 1.0.0
 */
@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final Payment payment;

    public PaymentCompletedEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }
}
