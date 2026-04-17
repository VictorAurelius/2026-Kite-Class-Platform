package com.kiteclass.core.module.assignment.event;

import com.kiteclass.core.module.assignment.entity.Submission;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when an assignment submission is graded.
 * This event triggers Grade Module to update grade components.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Getter
public class AssignmentGradedEvent extends ApplicationEvent {

    private final Submission submission;
    private final Long assignmentId;
    private final Long studentId;
    private final Long classId;

    public AssignmentGradedEvent(Object source, Submission submission, Long classId) {
        super(source);
        this.submission = submission;
        this.assignmentId = submission.getAssignmentId();
        this.studentId = submission.getStudentId();
        this.classId = classId;
    }
}
