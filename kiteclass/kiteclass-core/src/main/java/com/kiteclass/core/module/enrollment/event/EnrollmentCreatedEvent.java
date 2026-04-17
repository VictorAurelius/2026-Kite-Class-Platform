package com.kiteclass.core.module.enrollment.event;

import com.kiteclass.core.module.enrollment.entity.Enrollment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new enrollment is created.
 *
 * <p>This event triggers auto-creation of invoice for the enrollment.
 * Consumed by {@code InvoiceEventListener} in the Invoice Module.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Getter
public class EnrollmentCreatedEvent extends ApplicationEvent {

    private final Enrollment enrollment;

    /**
     * Creates a new EnrollmentCreatedEvent.
     *
     * @param source the object that published the event (usually the service)
     * @param enrollment the enrollment that was created
     */
    public EnrollmentCreatedEvent(Object source, Enrollment enrollment) {
        super(source);
        this.enrollment = enrollment;
    }
}
