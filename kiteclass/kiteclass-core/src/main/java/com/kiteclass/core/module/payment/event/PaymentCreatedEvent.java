package com.kiteclass.core.module.payment.event;

import com.kiteclass.core.module.payment.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a payment is created.
 *
 * @since 1.0.0
 */
@Getter
public class PaymentCreatedEvent extends ApplicationEvent {

    private final Payment payment;

    public PaymentCreatedEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }
}
