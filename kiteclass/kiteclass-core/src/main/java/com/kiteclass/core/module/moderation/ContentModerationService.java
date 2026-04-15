package com.kiteclass.core.module.moderation;

import com.kiteclass.core.common.audit.AuditLogWriter;
import com.kiteclass.core.common.audit.AuditLogWriter.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Facade for content moderation per ADR-010 (3-stage pipeline).
 *
 * <p>Pipeline responsibilities:
 * <ul>
 *   <li><b>Stage 1</b> — stubbed NSFW/keyword pre-check (synchronous, fast). The stub is
 *       deterministic (keyword-score) so we can unit test without an ML dependency; a real
 *       classifier slots in later by replacing this method only.</li>
 *   <li><b>Stage 2</b> — on fail, recommend the template-only fallback. The service does
 *       NOT itself re-run the branding pipeline — the CALLER (e.g. PublishPackageStep) owns
 *       that decision. The result merely carries REJECTED + reason so the caller can choose
 *       to retry with TEMPLATE category only, per {@code moderation.stage2.auto-fallback-to-template}.</li>
 *   <li><b>Stage 3</b> — for borderline scores (in the hysteresis band below the reject
 *       threshold) we persist a {@link ModerationQueue} row with status NEEDS_HUMAN_REVIEW
 *       so Stage X admin UI (follow-up wave) can adjudicate.</li>
 * </ul>
 *
 * <p>EVERY call — approved, rejected, or escalated — writes one {@link com.kiteclass.core.common.audit.AuditLog}
 * row via {@link AuditLogWriter} (BR-AUDIT-001). The writer runs with
 * {@code Propagation.MANDATORY}, so this service's {@code @Transactional} boundary is what
 * commits audit rows alongside the moderation decision.
 *
 * <p>Config keys (see {@code application.yml} under {@code moderation:}):
 * <ul>
 *   <li>{@code moderation.stage1.enabled} — disable = every call APPROVED (dev/test)</li>
 *   <li>{@code moderation.stage1.nsfw-threshold} — score &gt;= threshold → REJECTED</li>
 *   <li>{@code moderation.stage2.auto-fallback-to-template} — flag forwarded to caller
 *       via logging; interpretation is caller-side.</li>
 * </ul>
 *
 * @since 3.24.0 (Wave 4 Sub-PR 4.1, GAP-018, ADR-010)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentModerationService {

    private static final String AGGREGATE_TYPE = "ModerationQueue";

    /**
     * Seeded keyword ban list. Real deployments override via config (follow-up);
     * kept as a static default so unit tests are deterministic without Spring context.
     */
    private static final List<String> DEFAULT_BANNED_KEYWORDS = List.of(
            "nsfw", "nude", "porn", "sex", "violence", "gore", "hate", "slur",
            "drug", "weapon", "terror"
    );

    private final ModerationQueueRepository queueRepository;
    private final AuditLogWriter auditLog;

    @Value("${moderation.stage1.enabled:true}")
    private boolean stage1Enabled;

    @Value("${moderation.stage1.nsfw-threshold:0.7}")
    private double nsfwThreshold;

    @Value("${moderation.stage2.auto-fallback-to-template:true}")
    private boolean autoFallbackToTemplate;

    /**
     * Run content through the 3-stage moderation pipeline.
     *
     * @param targetType logical category (e.g. {@code "branding.banner"})
     * @param targetId   caller-owned id of the item being checked
     * @param text       text to scan (may be null)
     * @param imageUrl   URL of the image to scan (may be null; currently only metadata hash
     *                   is supported by the stub — reserved for real classifier integration)
     * @return non-null result with a terminal-or-escalated status
     */
    @Transactional
    public ModerationResult check(String targetType, String targetId, String text, String imageUrl) {
        if (!stage1Enabled) {
            ModerationResult approved = ModerationResult.builder()
                    .status(ModerationStatus.APPROVED)
                    .score(0.0)
                    .flaggedKeywords(Collections.emptyList())
                    .reason("stage1.disabled")
                    .build();
            writeAudit(targetType, targetId, approved, null);
            return approved;
        }

        ModerationResult stage1 = runStage1(text, imageUrl);

        if (stage1.isApproved()) {
            writeAudit(targetType, targetId, stage1, null);
            return stage1;
        }

        if (stage1.needsHumanReview()) {
            ModerationQueue row = persistQueueRow(targetType, targetId, stage1,
                    ModerationStatus.NEEDS_HUMAN_REVIEW);
            writeAudit(targetType, targetId, stage1, row.getId());
            log.info("[moderation] queued for human review target={}:{} score={} keywords={}",
                    targetType, targetId, stage1.getScore(), stage1.getFlaggedKeywords());
            return stage1;
        }

        // REJECTED — persist row + audit; Stage 2 template-only fallback is CALLER's decision.
        ModerationQueue row = persistQueueRow(targetType, targetId, stage1,
                ModerationStatus.REJECTED);
        writeAudit(targetType, targetId, stage1, row.getId());
        log.info("[moderation] rejected target={}:{} score={} keywords={} "
                        + "recommend-template-fallback={}",
                targetType, targetId, stage1.getScore(), stage1.getFlaggedKeywords(),
                autoFallbackToTemplate);
        return stage1;
    }

    /**
     * Stage 1: deterministic keyword-based scoring stub.
     *
     * <p>Score formula — simple count-of-hits / count-of-keywords capped at 1.0. Kept
     * intentionally naive; a real classifier replaces this method without touching
     * callers or {@link ModerationResult}.
     */
    private ModerationResult runStage1(String text, String imageUrl) {
        List<String> flagged = new ArrayList<>();
        double score = 0.0;

        if (text != null && !text.isBlank()) {
            String lower = text.toLowerCase(Locale.ROOT);
            List<String> tokens = Arrays.stream(lower.split("\\W+"))
                    .filter(t -> !t.isBlank())
                    .collect(Collectors.toList());
            int hits = 0;
            for (String kw : DEFAULT_BANNED_KEYWORDS) {
                if (tokens.contains(kw)) {
                    flagged.add(kw);
                    hits++;
                }
            }
            if (!tokens.isEmpty()) {
                score = Math.min(1.0, (double) hits / Math.max(3, tokens.size()));
                // Boost score if multiple hits to push it into reject band.
                if (hits >= 2) {
                    score = Math.max(score, 0.85);
                } else if (hits == 1) {
                    score = Math.max(score, 0.55);
                }
            }
        }

        // Image hash placeholder — no-op in stub; real classifier adds to score here.
        if (imageUrl != null && !imageUrl.isBlank()) {
            log.debug("[moderation] stage1 image check (stub, no-op) url={}", imageUrl);
        }

        ModerationStatus status = classifyScore(score);
        String reason = status == ModerationStatus.APPROVED
                ? "stage1.pass"
                : status == ModerationStatus.REJECTED
                ? ("banned-keyword" + (flagged.isEmpty() ? "" : ": " + String.join(",", flagged)))
                : "borderline-score";

        return ModerationResult.builder()
                .status(status)
                .score(score)
                .flaggedKeywords(Collections.unmodifiableList(flagged))
                .reason(reason)
                .build();
    }

    /**
     * Map a Stage 1 score to a terminal-or-escalated status using the configured
     * threshold and a fixed 0.2-wide hysteresis band for human review.
     */
    private ModerationStatus classifyScore(double score) {
        double reviewBandLow = Math.max(0.0, nsfwThreshold - 0.2);
        if (score >= nsfwThreshold) {
            return ModerationStatus.REJECTED;
        }
        if (score >= reviewBandLow) {
            return ModerationStatus.NEEDS_HUMAN_REVIEW;
        }
        return ModerationStatus.APPROVED;
    }

    private ModerationQueue persistQueueRow(
            String targetType, String targetId, ModerationResult result, ModerationStatus initial) {
        ModerationQueue row = ModerationQueue.builder()
                .targetType(targetType)
                .targetId(targetId)
                .status(ModerationStatus.PENDING)
                .score(result.getScore())
                .flaggedKeywords(toJsonArray(result.getFlaggedKeywords()))
                .reason(result.getReason())
                .build();
        row.transitionTo(initial);
        return queueRepository.save(row);
    }

    private void writeAudit(String targetType, String targetId,
                            ModerationResult result, Long queueRowId) {
        String aggregateId = queueRowId != null
                ? String.valueOf(queueRowId)
                : targetType + ":" + targetId;
        auditLog.record(AuditLogEvent.builder()
                .actionType("moderation." + result.getStatus().name().toLowerCase(Locale.ROOT))
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .payload(buildPayload(targetType, targetId, result))
                .reason(result.getReason())
                .build());
    }

    private static String toJsonArray(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "[]";
        }
        return keywords.stream()
                .map(k -> "\"" + k.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String buildPayload(String targetType, String targetId, ModerationResult result) {
        return String.format(Locale.ROOT,
                "{\"targetType\":\"%s\",\"targetId\":\"%s\",\"score\":%.4f,\"keywords\":%s}",
                targetType, targetId, result.getScore(),
                toJsonArray(result.getFlaggedKeywords()));
    }
}
