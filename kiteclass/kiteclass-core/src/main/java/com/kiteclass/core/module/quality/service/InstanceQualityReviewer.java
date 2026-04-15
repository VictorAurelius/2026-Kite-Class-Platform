package com.kiteclass.core.module.quality.service;

import com.kiteclass.core.common.audit.AuditLogWriter;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.quality.check.QualityCheck;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.entity.QualityReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates {@link QualityCheck} results into a {@link QualityReport} and persists it.
 *
 * <p>Per ai-branding-guidelines.md §5: runs all registered checks, computes weighted
 * average score, gates DEPLOY when {@code score < pass-threshold} (default 70).
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5, GAP-012)
 */
@Service
@Slf4j
public class InstanceQualityReviewer {

    private static final String AGGREGATE_TYPE = "QualityReport";

    private final List<QualityCheck> checks;
    private final QualityReportRepository reportRepository;
    private final FrontendInstanceRepository instanceRepository;
    private final AuditLogWriter auditLog;
    private final int passThreshold;

    public InstanceQualityReviewer(
            List<QualityCheck> checks,
            QualityReportRepository reportRepository,
            FrontendInstanceRepository instanceRepository,
            AuditLogWriter auditLog,
            @Value("${quality-gate.pass-threshold:70}") int passThreshold) {
        this.checks = checks;
        this.reportRepository = reportRepository;
        this.instanceRepository = instanceRepository;
        this.auditLog = auditLog;
        this.passThreshold = passThreshold;
    }

    @Transactional
    public QualityReport review(Long instanceId) {
        FrontendInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "FrontendInstance not found: id=" + instanceId));

        Map<String, QualityCheck.Result> byName = checks.stream()
                .collect(Collectors.toMap(QualityCheck::name, c -> c.run(instance)));

        int averageScore = (int) Math.round(
                byName.values().stream().mapToInt(QualityCheck.Result::getScore).average().orElse(0));
        boolean passed = averageScore >= passThreshold;
        String issuesJson = renderIssues(byName);

        QualityReport report = QualityReport.builder()
                .targetInstanceId(instanceId)
                .brandingVersion(instance.getBrandingVersion())
                .score(averageScore)
                .passed(passed)
                .issues(issuesJson)
                .contrastScore(scoreOf(byName, "wcag-contrast"))
                .cssVarsScore(scoreOf(byName, "css-vars-applied"))
                .assetUrlsScore(scoreOf(byName, "asset-urls-reachable"))
                .visualRegressionScore(scoreOf(byName, "visual-regression"))
                .logoPlacementScore(scoreOf(byName, "logo-placement"))
                .build();
        QualityReport saved = reportRepository.save(report);

        auditLog.record(AuditLogWriter.AuditLogEvent.builder()
                .actionType(passed ? "quality.review.passed" : "quality.review.failed")
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saved.getId()))
                .payload("{\"instanceId\":" + instanceId + ",\"score\":" + averageScore + "}")
                .reason(passed ? null : "score " + averageScore + " < threshold " + passThreshold)
                .build());

        log.info("[quality-gate] id={} score={} passed={}", instanceId, averageScore, passed);
        return saved;
    }

    private static Integer scoreOf(Map<String, QualityCheck.Result> byName, String key) {
        QualityCheck.Result r = byName.get(key);
        return r == null ? null : r.getScore();
    }

    private static String renderIssues(Map<String, QualityCheck.Result> byName) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (QualityCheck.Result r : byName.values()) {
            if (r.isPassed()) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            sb.append("{\"check\":\"").append(escape(r.getCheckName()))
                    .append("\",\"score\":").append(r.getScore())
                    .append(",\"detail\":\"").append(escape(r.getDetail())).append("\"}");
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escape(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
