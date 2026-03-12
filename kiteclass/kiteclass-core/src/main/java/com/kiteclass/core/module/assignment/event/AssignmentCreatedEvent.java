package com.kiteclass.core.module.assignment.event;

import com.kiteclass.core.module.assignment.entity.Assignment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new assignment is created.
 *
 * <p>Triggers auto-creation of grade components for all enrolled students
 * in the assignment's class. This ensures each student has a grade entry
 * for the assignment, initialized with score = 0 (not submitted).
 *
 * <p>Event flow:
 * <ol>
 *   <li>Teacher creates assignment → AssignmentService saves to DB</li>
 *   <li>AssignmentService publishes this event</li>
 *   <li>GradeEventListener receives event</li>
 *   <li>Listener creates GradeComponent for each enrolled student</li>
 * </ol>
 *
 * @author KiteClass Team
 * @since 2.15
 */
@Getter
public class AssignmentCreatedEvent extends ApplicationEvent {

    private final Assignment assignment;
    private final Long classId;

    /**
     * Creates a new assignment created event.
     *
     * @param source the component that published the event (typically AssignmentService)
     * @param assignment the created assignment entity
     */
    public AssignmentCreatedEvent(Object source, Assignment assignment) {
        super(source);
        this.assignment = assignment;
        this.classId = assignment.getClassId();
    }
}
