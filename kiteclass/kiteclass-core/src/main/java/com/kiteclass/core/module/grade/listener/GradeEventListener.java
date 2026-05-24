package com.kiteclass.core.module.grade.listener;

import com.kiteclass.core.module.assignment.event.AssignmentCreatedEvent;
import com.kiteclass.core.module.enrollment.event.EnrollmentCreatedEvent;
import com.kiteclass.core.module.grade.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for grade auto-initialization.
 *
 * <p>Listens to enrollment and assignment events to auto-create grade records.
 * Implements event-driven gradebook management to ensure students have grade
 * entries when they enroll or when new assignments are created.
 *
 * <p><strong>Handled Events:</strong>
 * <ul>
 *   <li><strong>ENROLLMENT_CREATED</strong> → Initialize grade for student in class</li>
 *   <li><strong>ASSIGNMENT_CREATED</strong> → Initialize grade components for all enrolled students</li>
 * </ul>
 *
 * <p><strong>Error Handling Philosophy:</strong>
 * Exceptions are logged but not re-thrown. Grade initialization failures should not
 * block the primary operations (enrollment creation, assignment creation). Missing
 * grades can be created manually later if needed.
 *
 * @author KiteClass Team
 * @since 2.15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GradeEventListener {

    private final GradeService gradeService;

    /**
     * Handles ENROLLMENT_CREATED event by auto-creating grade record.
     *
     * <p><strong>Event Flow:</strong>
     * <ol>
     *   <li>Student enrolls in class → ENROLLMENT_CREATED event published</li>
     *   <li>This listener receives event</li>
     *   <li>Auto-create Grade record for student in class</li>
     * </ol>
     *
     * <p><strong>Business Logic:</strong>
     * <ul>
     *   <li>Grade status = IN_PROGRESS (student actively learning)</li>
     *   <li>Pass threshold = 50% (default, can be customized per class)</li>
     *   <li>Initial scores = null (not yet graded)</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li>Exceptions are logged but not re-thrown</li>
     *   <li>Enrollment should not fail if grade creation fails</li>
     *   <li>Grade can be created manually later if needed</li>
     * </ul>
     *
     * @param event the enrollment created event
     */
    // Wave beta-readiness-1 Bucket B — capacity-race fix.
    // Changed from @EventListener + @Transactional (REQUIRED) to
    // @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW).
    //
    // Previous: listener fired INSIDE enrollment TX; GradeService re-read the Class entity
    // that the enrollment TX had locked with OPTIMISTIC_FORCE_INCREMENT. At commit time
    // Hibernate tried to bump Class.version twice → ObjectOptimisticLockingFailureException
    // propagated back through synchronous publishEvent() and rolled back the enrollment.
    //
    // Now: listener fires AFTER enrollment TX commits (AFTER_COMMIT), in its own
    // REQUIRES_NEW TX. If grade creation fails, enrollment is already committed — correct
    // per the existing "Don't throw" error-handling philosophy in this class.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        log.info("Received ENROLLMENT_CREATED event for enrollment ID: {}",
                event.getEnrollment().getId());

        try {
            gradeService.initializeGradeForEnrollment(event.getEnrollment().getId());

            log.info("Auto-created grade for enrollment {}", event.getEnrollment().getId());

        } catch (Exception e) {
            log.error("Failed to create grade for enrollment {}: {}",
                    event.getEnrollment().getId(), e.getMessage(), e);

            // Don't throw - enrollment should not fail if grade fails
            // Grade can be created manually later
        }
    }

    /**
     * Handles ASSIGNMENT_CREATED event by auto-creating grade components.
     *
     * <p><strong>Event Flow:</strong>
     * <ol>
     *   <li>Teacher creates assignment → ASSIGNMENT_CREATED event published</li>
     *   <li>This listener receives event</li>
     *   <li>Auto-create GradeComponent for each enrolled student</li>
     * </ol>
     *
     * <p><strong>Business Rules:</strong>
     * <ul>
     *   <li>Only ACTIVE enrollments receive grade components</li>
     *   <li>Component type = ASSIGNMENT</li>
     *   <li>Initial score = 0 (not submitted)</li>
     *   <li>Max score = from assignment definition</li>
     *   <li>Weight = from assignment definition</li>
     *   <li>Idempotent - skips if component already exists</li>
     * </ul>
     *
     * <p><strong>Batch Processing:</strong>
     * Creates components for all enrolled students in one transaction.
     * If individual student fails, logs error and continues with next student.
     *
     * @param event the assignment created event
     */
    @EventListener
    @Transactional
    public void onAssignmentCreated(AssignmentCreatedEvent event) {
        log.info("Received ASSIGNMENT_CREATED event for assignment ID: {}",
                event.getAssignment().getId());

        try {
            int componentsCreated = gradeService.initializeGradeComponentsForAssignment(
                    event.getAssignment().getId(),
                    event.getClassId());

            log.info("Auto-created {} grade components for assignment {}",
                    componentsCreated, event.getAssignment().getId());

        } catch (Exception e) {
            log.error("Failed to create grade components for assignment {}: {}",
                    event.getAssignment().getId(), e.getMessage(), e);

            // Don't throw - assignment creation should not fail
            // Components can be created manually later
        }
    }
}
