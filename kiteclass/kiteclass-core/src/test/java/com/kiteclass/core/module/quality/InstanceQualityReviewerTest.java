package com.kiteclass.core.module.quality;

import com.kiteclass.core.common.audit.AuditLogWriter;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.quality.check.QualityCheck;
import com.kiteclass.core.module.quality.entity.QualityReport;
import com.kiteclass.core.module.quality.entity.QualityReportRepository;
import com.kiteclass.core.module.quality.service.InstanceQualityReviewer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstanceQualityReviewerTest {

    private FrontendInstance instance(long id) {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme")
                .status(FrontendInstanceStatus.GENERATING)
                .retryCount(0).brandingVersion(3).build();
        i.setId(id);
        return i;
    }

    private InstanceQualityReviewer reviewer(List<QualityCheck> checks, int threshold,
                                             FrontendInstanceRepository instanceRepo,
                                             QualityReportRepository reportRepo,
                                             AuditLogWriter audit) {
        return new InstanceQualityReviewer(checks, reportRepo, instanceRepo, audit, threshold);
    }

    private QualityCheck stub(String name, int score, boolean pass) {
        QualityCheck c = mock(QualityCheck.class);
        when(c.name()).thenReturn(name);
        QualityCheck.Result r = pass
                ? QualityCheck.Result.pass(name, score)
                : QualityCheck.Result.fail(name, score, name + " failed");
        when(c.run(any())).thenReturn(r);
        return c;
    }

    @Test
    void review_passing_scores_produce_passed_report() {
        FrontendInstanceRepository instanceRepo = mock(FrontendInstanceRepository.class);
        QualityReportRepository reportRepo = mock(QualityReportRepository.class);
        AuditLogWriter audit = mock(AuditLogWriter.class);

        when(instanceRepo.findById(42L)).thenReturn(Optional.of(instance(42L)));
        when(reportRepo.save(any(QualityReport.class))).thenAnswer(inv -> inv.getArgument(0));

        InstanceQualityReviewer r = reviewer(List.of(
                stub("wcag-contrast", 85, true),
                stub("logo-placement", 95, true)
        ), 70, instanceRepo, reportRepo, audit);

        QualityReport report = r.review(42L);

        assertThat(report.getScore()).isEqualTo(90);
        assertThat(report.getPassed()).isTrue();
        assertThat(report.getIssues()).isEqualTo("[]");
        verify(audit).record(any());
    }

    @Test
    void review_below_threshold_produces_failed_report_with_issues() {
        FrontendInstanceRepository instanceRepo = mock(FrontendInstanceRepository.class);
        QualityReportRepository reportRepo = mock(QualityReportRepository.class);
        AuditLogWriter audit = mock(AuditLogWriter.class);

        when(instanceRepo.findById(42L)).thenReturn(Optional.of(instance(42L)));
        when(reportRepo.save(any(QualityReport.class))).thenAnswer(inv -> inv.getArgument(0));

        InstanceQualityReviewer r = reviewer(List.of(
                stub("wcag-contrast", 40, false),
                stub("logo-placement", 50, false)
        ), 70, instanceRepo, reportRepo, audit);

        QualityReport report = r.review(42L);

        assertThat(report.getScore()).isEqualTo(45);
        assertThat(report.getPassed()).isFalse();
        assertThat(report.getIssues()).contains("wcag-contrast").contains("logo-placement");
    }

    @Test
    void review_missing_instance_throws() {
        FrontendInstanceRepository instanceRepo = mock(FrontendInstanceRepository.class);
        when(instanceRepo.findById(999L)).thenReturn(Optional.empty());

        InstanceQualityReviewer r = reviewer(List.of(), 70,
                instanceRepo, mock(QualityReportRepository.class), mock(AuditLogWriter.class));

        assertThatThrownBy(() -> r.review(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void review_records_per_check_scores_into_columns() {
        FrontendInstanceRepository instanceRepo = mock(FrontendInstanceRepository.class);
        QualityReportRepository reportRepo = mock(QualityReportRepository.class);
        when(instanceRepo.findById(42L)).thenReturn(Optional.of(instance(42L)));
        when(reportRepo.save(any(QualityReport.class))).thenAnswer(inv -> inv.getArgument(0));

        InstanceQualityReviewer r = reviewer(List.of(
                stub("wcag-contrast", 85, true),
                stub("css-vars-applied", 80, true),
                stub("asset-urls-reachable", 90, true),
                stub("visual-regression", 85, true),
                stub("logo-placement", 95, true)
        ), 70, instanceRepo, reportRepo, mock(AuditLogWriter.class));

        QualityReport report = r.review(42L);

        assertThat(report.getContrastScore()).isEqualTo(85);
        assertThat(report.getCssVarsScore()).isEqualTo(80);
        assertThat(report.getAssetUrlsScore()).isEqualTo(90);
        assertThat(report.getVisualRegressionScore()).isEqualTo(85);
        assertThat(report.getLogoPlacementScore()).isEqualTo(95);
    }
}
