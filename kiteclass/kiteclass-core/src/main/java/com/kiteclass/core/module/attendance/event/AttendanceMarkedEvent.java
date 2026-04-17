package com.kiteclass.core.module.attendance.event;

import com.kiteclass.core.module.attendance.entity.Attendance;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Event published when attendance is marked for a session.
 *
 * <p>This event is published after attendance records are successfully saved
 * to the database. Future modules (e.g., Grade Module) can listen to this event
 * to trigger related operations such as attendance-based grade calculations.
 *
 * <p>Published within the same transaction as the attendance save operation,
 * following the Payment Module event publishing pattern.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Getter
public class AttendanceMarkedEvent extends ApplicationEvent {

    /**
     * List of attendance records that were marked.
     * Never null or empty.
     */
    private final List<Attendance> attendances;

    /**
     * Session ID for which attendance was marked.
     */
    private final Long sessionId;

    /**
     * Teacher ID who marked the attendance.
     */
    private final Long markedBy;

    /**
     * Constructs a new AttendanceMarkedEvent.
     *
     * @param source the object on which the event initially occurred (never null)
     * @param attendances the list of attendance records that were marked
     * @param sessionId the session ID for which attendance was marked
     * @param markedBy the teacher ID who marked the attendance
     */
    public AttendanceMarkedEvent(Object source, List<Attendance> attendances,
                                Long sessionId, Long markedBy) {
        super(source);
        this.attendances = List.copyOf(attendances); // Defensive copy
        this.sessionId = sessionId;
        this.markedBy = markedBy;
    }
}
