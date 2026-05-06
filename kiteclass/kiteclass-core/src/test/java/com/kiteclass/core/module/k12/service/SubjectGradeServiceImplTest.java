package com.kiteclass.core.module.k12.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.exception.IllegalGradeTransitionException;
import com.kiteclass.core.module.k12.listener.SubjectGradeAllPublishedListener;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SubjectGradeServiceImpl} — BR-GRADEBOOK-003 state
 * machine + §360.5 học bạ trigger contract.
 *
 * <p>Covered:
 * <ol>
 *   <li>review: DRAFT → REVIEWED succeeds, sets reviewedBy</li>
 *   <li>review: REVIEWED throws IllegalGradeTransitionException (idempotency)</li>
 *   <li>review: PUBLISHED throws IllegalGradeTransitionException (terminal state)</li>
 *   <li>publish: REVIEWED → PUBLISHED succeeds, sets publishedAt, invokes listener</li>
 *   <li>publish: DRAFT throws (cannot skip REVIEWED)</li>
 *   <li>publish: PUBLISHED throws (terminal — no re-publish)</li>
 *   <li>publish: listener fired only on success path</li>
 *   <li>revertToDraft: REVIEWED → DRAFT succeeds, clears reviewedBy</li>
 *   <li>submitForReview: DRAFT permitted, returns id</li>
 *   <li>submitForReview: REVIEWED throws</li>
 *   <li>load: missing id → BusinessException(GRADE_NOT_FOUND)</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SubjectGradeServiceImplTest {

    @Mock
    private SubjectGradeRepository repository;

    @Mock
    private SubjectGradeAllPublishedListener listener;

    @InjectMocks
    private SubjectGradeServiceImpl service;

    private static final Long GRADE_ID = 100L;
    private static final Long REVIEWER_ID = 11L;
    private static final Long PUBLISHER_ID = 22L;
    private static final Long SUBMITTER_ID = 33L;

    private SubjectGrade grade;

    @BeforeEach
    void setUp() {
        grade = SubjectGrade.builder()
                .studentId(900L)
                .status(SubjectGradeStatus.DRAFT)
                .build();
        grade.setId(GRADE_ID);
    }

    @Test
    void review_DRAFT_succeeds() {
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));
        when(repository.save(any(SubjectGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectGrade result = service.review(GRADE_ID, REVIEWER_ID);

        assertThat(result.getStatus()).isEqualTo(SubjectGradeStatus.REVIEWED);
        assertThat(result.getReviewedBy()).isEqualTo(REVIEWER_ID);
        verify(repository).save(grade);
        verifyNoInteractions(listener);
    }

    @Test
    void review_REVIEWED_throwsInvalidTransition() {
        grade.setStatus(SubjectGradeStatus.REVIEWED);
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.review(GRADE_ID, REVIEWER_ID))
                .isInstanceOf(IllegalGradeTransitionException.class)
                .extracting("code").isEqualTo(IllegalGradeTransitionException.ERROR_CODE);
        verify(repository, never()).save(any(SubjectGrade.class));
    }

    @Test
    void review_PUBLISHED_throwsInvalidTransition() {
        grade.setStatus(SubjectGradeStatus.PUBLISHED);
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.review(GRADE_ID, REVIEWER_ID))
                .isInstanceOf(IllegalGradeTransitionException.class);
        verify(repository, never()).save(any(SubjectGrade.class));
    }

    @Test
    void publish_REVIEWED_succeeds_andFiresListener() {
        grade.setStatus(SubjectGradeStatus.REVIEWED);
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));
        when(repository.save(any(SubjectGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectGrade result = service.publish(GRADE_ID, PUBLISHER_ID);

        assertThat(result.getStatus()).isEqualTo(SubjectGradeStatus.PUBLISHED);
        assertThat(result.getPublishedAt()).isNotNull();
        verify(repository).save(grade);
        verify(listener, times(1)).onPublish(grade);
    }

    @Test
    void publish_DRAFT_throwsInvalidTransition() {
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.publish(GRADE_ID, PUBLISHER_ID))
                .isInstanceOf(IllegalGradeTransitionException.class);
        verify(repository, never()).save(any(SubjectGrade.class));
        verifyNoInteractions(listener);
    }

    @Test
    void publish_PUBLISHED_throwsInvalidTransition() {
        grade.setStatus(SubjectGradeStatus.PUBLISHED);
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.publish(GRADE_ID, PUBLISHER_ID))
                .isInstanceOf(IllegalGradeTransitionException.class);
        verify(repository, never()).save(any(SubjectGrade.class));
        verifyNoInteractions(listener);
    }

    @Test
    void revertToDraft_REVIEWED_succeeds_clearsReviewedBy() {
        grade.setStatus(SubjectGradeStatus.REVIEWED);
        grade.setReviewedBy(REVIEWER_ID);
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));
        when(repository.save(any(SubjectGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectGrade result = service.revertToDraft(GRADE_ID, REVIEWER_ID);

        assertThat(result.getStatus()).isEqualTo(SubjectGradeStatus.DRAFT);
        assertThat(result.getReviewedBy()).isNull();
        verifyNoInteractions(listener);
    }

    @Test
    void revertToDraft_DRAFT_throwsInvalidTransition() {
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.revertToDraft(GRADE_ID, REVIEWER_ID))
                .isInstanceOf(IllegalGradeTransitionException.class);
    }

    @Test
    void submitForReview_DRAFT_returnsId() {
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));

        Long result = service.submitForReview(GRADE_ID, SUBMITTER_ID);

        assertThat(result).isEqualTo(GRADE_ID);
        verify(repository, never()).save(any(SubjectGrade.class));
    }

    @Test
    void submitForReview_REVIEWED_throwsInvalidTransition() {
        grade.setStatus(SubjectGradeStatus.REVIEWED);
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.of(grade));

        assertThatThrownBy(() -> service.submitForReview(GRADE_ID, SUBMITTER_ID))
                .isInstanceOf(IllegalGradeTransitionException.class);
    }

    @Test
    void load_missingId_throwsGradeNotFound() {
        when(repository.findByIdAndDeletedFalse(GRADE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(GRADE_ID, REVIEWER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("GRADE_NOT_FOUND");
    }
}
