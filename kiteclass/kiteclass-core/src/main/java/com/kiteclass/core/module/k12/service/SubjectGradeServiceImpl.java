package com.kiteclass.core.module.k12.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.exception.IllegalGradeTransitionException;
import com.kiteclass.core.module.k12.listener.SubjectGradeAllPublishedListener;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State-Pattern enforcement of BR-GRADEBOOK-003 transitions
 * ({@code DRAFT → REVIEWED → PUBLISHED}, plus REVIEWED → DRAFT revert).
 *
 * <p>Outside this class, {@code SubjectGrade.setStatus} is package-private —
 * no other component may flip status directly. ArchUnit test enforces the
 * boundary; reviewer-checklist enforces during review.
 *
 * <p>{@link #publish} additionally invokes
 * {@link SubjectGradeAllPublishedListener} INSIDE the same transaction so the
 * "học bạ ready" Outbox row commits atomically with the PUBLISHED status flip.
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.1 + §360.5)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectGradeServiceImpl implements SubjectGradeService {

    private static final String GRADE_NOT_FOUND = "GRADE_NOT_FOUND";

    /**
     * Allowed transitions per BR-GRADEBOOK-003. PUBLISHED is terminal
     * (empty set = no exit allowed).
     */
    private static final Map<SubjectGradeStatus, Set<SubjectGradeStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(SubjectGradeStatus.class);
        ALLOWED_TRANSITIONS.put(SubjectGradeStatus.DRAFT,
                EnumSet.of(SubjectGradeStatus.REVIEWED));
        ALLOWED_TRANSITIONS.put(SubjectGradeStatus.REVIEWED,
                EnumSet.of(SubjectGradeStatus.PUBLISHED, SubjectGradeStatus.DRAFT));
        ALLOWED_TRANSITIONS.put(SubjectGradeStatus.PUBLISHED, EnumSet.noneOf(SubjectGradeStatus.class));
    }

    private final SubjectGradeRepository subjectGradeRepository;
    private final SubjectGradeAllPublishedListener allPublishedListener;

    @Override
    @Transactional
    public Long submitForReview(Long gradeId, Long submitterId) {
        SubjectGrade grade = loadGrade(gradeId);
        // §360.1: submit-for-review acts on DRAFT only. Already-reviewed or
        // already-published grades cannot be "re-submitted"; client must use
        // the appropriate transition (revertToDraft) first.
        if (grade.getStatus() != null && grade.getStatus() != SubjectGradeStatus.DRAFT) {
            throw new IllegalGradeTransitionException(grade.getStatus(), SubjectGradeStatus.DRAFT);
        }
        log.info("Grade {} submitted for review by user {}", gradeId, submitterId);
        return grade.getId();
    }

    @Override
    @Transactional
    public SubjectGrade review(Long gradeId, Long reviewerId) {
        SubjectGrade grade = loadGrade(gradeId);
        ensureTransitionAllowed(currentOrDraft(grade), SubjectGradeStatus.REVIEWED);
        grade.setStatus(SubjectGradeStatus.REVIEWED);
        grade.setReviewedBy(reviewerId);
        SubjectGrade saved = subjectGradeRepository.save(grade);
        log.info("Grade {} reviewed by user {}", gradeId, reviewerId);
        return saved;
    }

    @Override
    @Transactional
    public SubjectGrade publish(Long gradeId, Long publisherId) {
        SubjectGrade grade = loadGrade(gradeId);
        ensureTransitionAllowed(currentOrDraft(grade), SubjectGradeStatus.PUBLISHED);
        Instant now = Instant.now();
        grade.setStatus(SubjectGradeStatus.PUBLISHED);
        grade.setPublishedAt(now);
        SubjectGrade saved = subjectGradeRepository.save(grade);
        // Trigger học bạ event inside same txn so Outbox commits atomically.
        allPublishedListener.onPublish(saved);
        log.info("Grade {} published by user {} at {}", gradeId, publisherId, now);
        return saved;
    }

    @Override
    @Transactional
    public SubjectGrade revertToDraft(Long gradeId, Long reviewerId) {
        SubjectGrade grade = loadGrade(gradeId);
        ensureTransitionAllowed(currentOrDraft(grade), SubjectGradeStatus.DRAFT);
        grade.setStatus(SubjectGradeStatus.DRAFT);
        grade.setReviewedBy(null);
        SubjectGrade saved = subjectGradeRepository.save(grade);
        log.info("Grade {} reverted to DRAFT by user {}", gradeId, reviewerId);
        return saved;
    }

    private SubjectGrade loadGrade(Long gradeId) {
        if (gradeId == null) {
            throw new BusinessException(GRADE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return subjectGradeRepository.findByIdAndDeletedFalse(gradeId)
                .orElseThrow(() -> new BusinessException(GRADE_NOT_FOUND, HttpStatus.NOT_FOUND, gradeId));
    }

    private void ensureTransitionAllowed(SubjectGradeStatus current, SubjectGradeStatus target) {
        Set<SubjectGradeStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(
                current, EnumSet.noneOf(SubjectGradeStatus.class));
        if (!allowed.contains(target)) {
            throw new IllegalGradeTransitionException(current, target);
        }
    }

    private static SubjectGradeStatus currentOrDraft(SubjectGrade grade) {
        return grade.getStatus() == null ? SubjectGradeStatus.DRAFT : grade.getStatus();
    }
}
