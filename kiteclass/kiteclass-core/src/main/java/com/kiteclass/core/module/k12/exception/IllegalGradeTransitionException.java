package com.kiteclass.core.module.k12.exception;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a {@code SubjectGrade} status transition violates BR-GRADEBOOK-003.
 *
 * <p>Allowed transitions per State Pattern (per {@code design-patterns.md} §3.3):
 * <ul>
 *   <li>{@link SubjectGradeStatus#DRAFT} → {@link SubjectGradeStatus#REVIEWED}</li>
 *   <li>{@link SubjectGradeStatus#REVIEWED} → {@link SubjectGradeStatus#PUBLISHED}</li>
 *   <li>{@link SubjectGradeStatus#REVIEWED} → {@link SubjectGradeStatus#DRAFT} (revert if Tổ trưởng spots an error)</li>
 * </ul>
 *
 * <p>All other transitions (e.g. DRAFT → PUBLISHED skipping REVIEWED, or any
 * transition out of PUBLISHED — terminal state) are rejected. HTTP status 409
 * Conflict signals the client request is well-formed but conflicts with the
 * current resource state.
 *
 * <p>Reference: BR-GRADEBOOK-003 in
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}.
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.1)
 */
public class IllegalGradeTransitionException extends BusinessException {

    public static final String ERROR_CODE = "INVALID_GRADE_TRANSITION";

    /**
     * @param current target's current status
     * @param target  requested status
     */
    public IllegalGradeTransitionException(SubjectGradeStatus current, SubjectGradeStatus target) {
        super(ERROR_CODE, HttpStatus.CONFLICT, current, target);
    }
}
