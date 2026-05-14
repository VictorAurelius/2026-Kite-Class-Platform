package com.kitehub.subscription.feedback.repository;

import com.kitehub.subscription.feedback.entity.FeedbackSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Spring Data repository for {@link FeedbackSubmission} (GAP-542 Wave 78).
 *
 * <p>Two access paths:</p>
 * <ol>
 *   <li><b>Admin queue:</b> {@code findByStatusOrderByCreatedAtDesc} via index
 *       {@code idx_feedback_submissions_status_created}.</li>
 *   <li><b>Email survey scheduler:</b>
 *       {@code findSubmissionsWithEmailInWindow} for day-7/14 digest — pulls
 *       distinct email + min(rating) + count(*) within a creation window.</li>
 * </ol>
 *
 * @since Wave 78 — GAP-542
 */
@Repository
public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, Long> {

    Page<FeedbackSubmission> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByCreatedAtAfter(OffsetDateTime since);

    /**
     * Email-survey scheduler query: submissions in a given creation window
     * that have an email attached (anonymous submits are skipped — no recipient).
     *
     * <p>Returns up to {@code limit} rows; scheduler batches digest emails in
     * single round-trip per day. Per BR-FEEDBACK-002 — only one digest per
     * recipient per window.</p>
     */
    @Query("""
            SELECT f FROM FeedbackSubmission f
             WHERE f.email IS NOT NULL
               AND f.createdAt BETWEEN :from AND :to
             ORDER BY f.createdAt ASC
            """)
    List<FeedbackSubmission> findSubmissionsWithEmailInWindow(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
