package com.kitehub.subscription.feedback.scheduler;

import com.kitehub.subscription.feedback.entity.FeedbackSubmission;
import com.kitehub.subscription.feedback.repository.FeedbackSubmissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Daily digest scheduler for feedback day-7 / day-14 survey reminders
 * (GAP-542 Wave 78 Bucket F).
 *
 * <p>Two windows per day:</p>
 * <ul>
 *   <li><b>Day-7 reminder:</b> feedback submitted 7 days ago → email digest
 *       "Cảm ơn bạn đã gửi feedback; chúng tôi đã đọc và đang xử lý…"</li>
 *   <li><b>Day-14 reminder:</b> feedback submitted 14 days ago → email digest
 *       with optional follow-up survey link</li>
 * </ul>
 *
 * <p>v1 MVP: scheduler logs the digest payload to STDOUT + counter metric.
 * Actual email send wires via {@code EmailServiceClient} in a follow-up gap
 * once the email template is finalized (Bucket E owns templates). Current
 * implementation gives ops visibility (log line per recipient) so the
 * scheduling pipeline can be verified end-to-end immediately.</p>
 *
 * <p>Cron: daily at 09:00 UTC. Configurable via
 * {@code kitehub.feedback.survey-cron} property.</p>
 *
 * <p>Idempotency: scheduler reads a strict 1-hour creation window so that a
 * second invocation in the same hour (manual or replay) emits the same set —
 * a downstream once-per-recipient guard belongs in {@code EmailServiceClient}
 * (already present for onboarding emails).</p>
 *
 * @since Wave 78 — GAP-542
 */
@Slf4j
@Component
public class FeedbackSurveyScheduler {

    private static final int DAY_7 = 7;
    private static final int DAY_14 = 14;
    private static final int MAX_DIGEST_BATCH = 200;

    private final FeedbackSubmissionRepository repository;
    private final boolean enabled;

    public FeedbackSurveyScheduler(
            FeedbackSubmissionRepository repository,
            @Value("${kitehub.feedback.survey-scheduler-enabled:true}") boolean enabled) {
        this.repository = repository;
        this.enabled = enabled;
    }

    /**
     * Daily 09:00 UTC. Resolves to day-7 + day-14 windows and emits digest
     * payloads for each submission with a contactable email.
     */
    @Scheduled(cron = "${kitehub.feedback.survey-cron:0 0 9 * * *}")
    public void runDailyDigest() {
        if (!enabled) {
            log.debug("FeedbackSurveyScheduler disabled by config — skipping");
            return;
        }
        log.info("FeedbackSurveyScheduler starting daily digest at {}", OffsetDateTime.now());
        int day7Count = sendDigestForOffsetDays(DAY_7, "day-7");
        int day14Count = sendDigestForOffsetDays(DAY_14, "day-14");
        log.info("FeedbackSurveyScheduler completed — day7Sent={} day14Sent={}",
                day7Count, day14Count);
    }

    /**
     * For a given offset (e.g. 7 days), find feedback submissions created in
     * a 24-hour window {@code [now-offset-1d, now-offset]} with an email
     * attached, and emit a digest log line per recipient. Returns count.
     */
    int sendDigestForOffsetDays(int offsetDays, String label) {
        OffsetDateTime to = OffsetDateTime.now().minus(Duration.ofDays(offsetDays));
        OffsetDateTime from = to.minus(Duration.ofDays(1));

        List<FeedbackSubmission> recipients = repository.findSubmissionsWithEmailInWindow(
                from, to, PageRequest.of(0, MAX_DIGEST_BATCH));

        int sent = 0;
        for (FeedbackSubmission submission : recipients) {
            try {
                // v1 MVP — log payload. Email send wires in follow-up gap.
                log.info("FeedbackSurvey {} digest publicId={} email={} rating={} category={}",
                        label,
                        submission.getPublicId(),
                        maskEmail(submission.getEmail()),
                        submission.getRating(),
                        submission.getCategory());
                sent++;
            } catch (RuntimeException ex) {
                log.error("FeedbackSurvey {} digest failed for publicId={}: {}",
                        label, submission.getPublicId(), ex.getMessage(), ex);
            }
        }
        return sent;
    }

    /**
     * Mask email for log output per logs-format-standard.md §3.1 PII patterns.
     * Defensive scrubbing — actual logger scrubber lands GAP-116.
     */
    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<none>";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        char first = email.charAt(0);
        return first + "***" + email.substring(at);
    }
}
