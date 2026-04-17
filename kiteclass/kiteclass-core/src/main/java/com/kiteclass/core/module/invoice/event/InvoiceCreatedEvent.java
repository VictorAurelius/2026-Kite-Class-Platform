package com.kiteclass.core.module.invoice.event;

import com.kiteclass.core.module.invoice.entity.Invoice;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new invoice is created.
 *
 * <p>This event can be consumed by the future Payment Module
 * to trigger payment workflows or notifications.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Getter
public class InvoiceCreatedEvent extends ApplicationEvent {

    private final Invoice invoice;

    /**
     * Creates a new InvoiceCreatedEvent.
     *
     * @param source the object that published the event (usually the service)
     * @param invoice the invoice that was created
     */
    public InvoiceCreatedEvent(Object source, Invoice invoice) {
        super(source);
        this.invoice = invoice;
    }
}
