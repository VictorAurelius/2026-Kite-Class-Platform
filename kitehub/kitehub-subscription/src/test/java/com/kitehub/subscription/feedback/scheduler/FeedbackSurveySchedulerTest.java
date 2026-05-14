package com.kitehub.subscription.feedback.scheduler;

import com.kitehub.subscription.feedback.entity.FeedbackSubmission;
import com.kitehub.subscription.feedback.repository.FeedbackSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeedbackSurveyScheduler (GAP-542 Wave 78 Bucket F).
 *
 * Coverage:
 *  - runDailyDigest invokes day-7 + day-14 windows
 *  - sendDigestForOffsetDays counts processed recipients
 *  - disabled flag skips run
 *  - empty result handled gracefully
 *
 * @since Wave 78 Bucket F
 */
@ExtendWith(MockitoExtension.class)
class FeedbackSurveySchedulerTest {

    @Mock
    private FeedbackSubmissionRepository repository;

    @Test
    void shouldSkipWhenDisabled() {
        FeedbackSurveyScheduler scheduler = new FeedbackSurveyScheduler(repository, false);
        scheduler.runDailyDigest();
        verify(repository, never()).findSubmissionsWithEmailInWindow(
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class));
    }

    @Test
    void shouldQueryBothWindowsWhenEnabled() {
        FeedbackSurveyScheduler scheduler = new FeedbackSurveyScheduler(repository, true);
        when(repository.findSubmissionsWithEmailInWindow(
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        scheduler.runDailyDigest();

        // Called once for day-7 window + once for day-14 window
        verify(repository, times(2)).findSubmissionsWithEmailInWindow(
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class));
    }

    @Test
    void shouldCountSentWhenRecipientsFound() {
        FeedbackSurveyScheduler scheduler = new FeedbackSurveyScheduler(repository, true);

        FeedbackSubmission row1 = FeedbackSubmission.builder()
                .publicId(UUID.randomUUID())
                .rating((short) 4)
                .email("a@kitehub.me")
                .category("USABILITY")
                .status("RECEIVED")
                .createdAt(OffsetDateTime.now().minusDays(7))
                .updatedAt(OffsetDateTime.now())
                .build();
        FeedbackSubmission row2 = FeedbackSubmission.builder()
                .publicId(UUID.randomUUID())
                .rating((short) 5)
                .email("b@kitehub.me")
                .category("GENERAL")
                .status("RECEIVED")
                .createdAt(OffsetDateTime.now().minusDays(7))
                .updatedAt(OffsetDateTime.now())
                .build();

        when(repository.findSubmissionsWithEmailInWindow(
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(row1, row2));

        int count = scheduler.sendDigestForOffsetDays(7, "day-7");
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldHandleEmptyResult() {
        FeedbackSurveyScheduler scheduler = new FeedbackSurveyScheduler(repository, true);
        when(repository.findSubmissionsWithEmailInWindow(
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        int count = scheduler.sendDigestForOffsetDays(14, "day-14");
        assertThat(count).isZero();
        verify(repository).findSubmissionsWithEmailInWindow(
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class));
    }
}
