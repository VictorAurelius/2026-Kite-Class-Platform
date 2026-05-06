package com.kiteclass.core.module.k12.service;

import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.exception.IllegalGradeTransitionException;

/**
 * State Pattern (per {@code design-patterns.md} §3.3) — exclusive mutator
 * surface for {@link SubjectGrade#getStatus()} transitions per BR-GRADEBOOK-003.
 *
 * <p>{@link SubjectGrade#setStatus} is package-private; every status change
 * MUST flow through this interface so transition validity and audit fields
 * (reviewedBy, publishedAt) stay consistent. Direct {@code setStatus} from
 * other packages is enforced banned by an ArchUnit test
 * ({@code SubjectGradeArchitectureTest}).
 *
 * <p>Allowed transitions:
 * <ul>
 *   <li>{@code DRAFT → REVIEWED} via {@link #review(Long, Long)}</li>
 *   <li>{@code REVIEWED → PUBLISHED} via {@link #publish(Long, Long)}</li>
 *   <li>{@code REVIEWED → DRAFT} (revert by Tổ trưởng) via
 *       {@link #revertToDraft(Long, Long)}</li>
 * </ul>
 *
 * <p>Reference: BR-GRADEBOOK-003 + BR-GRADEBOOK-006 in
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/rules.md}
 * and UC-GRADEBOOK-* in {@code use-cases.md}.
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.1)
 */
public interface SubjectGradeService {

    /**
     * Submit a {@code DRAFT} grade for Tổ trưởng review. The status remains
     * {@code DRAFT} (the entry-state) but the grade is flagged as ready by
     * downstream notification (out of scope here — GAP-063b).
     *
     * <p>For Phase 1C v1.5 this is a no-op marker: the action exists for the
     * api-contract but no status transition happens until Tổ trưởng actually
     * approves via {@link #review(Long, Long)}. The grade must currently be
     * {@code DRAFT}; calling on REVIEWED/PUBLISHED throws
     * {@link IllegalGradeTransitionException}.
     *
     * @param gradeId     SubjectGrade primary key
     * @param submitterId user (GV bộ môn) submitting for review
     * @return id of the grade (echo) for client confirmation
     */
    Long submitForReview(Long gradeId, Long submitterId);

    /**
     * Tổ trưởng marks a {@code DRAFT} grade as {@code REVIEWED}.
     *
     * @throws IllegalGradeTransitionException when the grade is not DRAFT
     */
    SubjectGrade review(Long gradeId, Long reviewerId);

    /**
     * Hiệu trưởng publishes a {@code REVIEWED} grade — terminal state per
     * BR-GRADEBOOK-003. Sets {@code publishedAt} and triggers the học bạ
     * Outbox event when this is the last grade for the (student, academicYear).
     *
     * @throws IllegalGradeTransitionException when the grade is not REVIEWED
     */
    SubjectGrade publish(Long gradeId, Long publisherId);

    /**
     * Tổ trưởng reverts a {@code REVIEWED} grade back to {@code DRAFT} (e.g.
     * after spotting an error pre-publish). Clears the {@code reviewedBy}
     * marker.
     *
     * @throws IllegalGradeTransitionException when the grade is not REVIEWED
     */
    SubjectGrade revertToDraft(Long gradeId, Long reviewerId);
}
