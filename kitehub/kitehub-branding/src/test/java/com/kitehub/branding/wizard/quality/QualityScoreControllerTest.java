package com.kitehub.branding.wizard.quality;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.quality.dto.QualityScoreResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QualityScoreController")
class QualityScoreControllerTest {

    @Mock
    private BrandingJobRepository jobRepository;

    @InjectMocks
    private QualityScoreController controller;

    private final QualityScoreAggregator aggregator = new QualityScoreAggregator();

    private BrandingJob job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        // Inject the real aggregator — score logic is part of contract verification.
        // GAP-386: aggregator's `threshold` field is @Value-injected in production;
        // unit tests bootstrapping without Spring must seed the default explicitly.
        ReflectionTestUtils.setField(aggregator, "threshold", 70);
        controller = new QualityScoreController(jobRepository, aggregator);

        jobId = UUID.randomUUID();
        job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(UUID.randomUUID());
        job.setOrganizationName("Trường ABC");
        job.setLanguage("vi");
        job.setStatus(JobStatus.COMPLETED);
        job.setLogoUrl("https://cdn.example.com/logo.png");
        job.setProgress(100);
        job.setRetryCount(0);
        job.setQueuedAt(LocalDateTime.now().minusMinutes(5));
        job.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        job.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("returns aggregated score with 5 sub-scores when job exists")
    void returnsAggregatedScore() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getQualityScore(jobId, job.getInstanceId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        QualityScoreResponse body = (QualityScoreResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.jobId()).isEqualTo(jobId.toString());
        assertThat(body.score()).isBetween(0, 100);
        assertThat(body.threshold()).isEqualTo(70);
        assertThat(body.subscores()).containsKeys(
                "contrast", "brokenLinks", "cssVarsApplied", "visualRegression", "logoPlacement");
        assertThat(body.subscores().values()).allMatch(v -> v >= 0 && v <= 100);
        assertThat(body.computedAt()).isNotNull();
    }

    @Test
    @DisplayName("returns 404 with JOB_NOT_FOUND error when job missing")
    void returns404WhenMissing() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getQualityScore(jobId, job.getInstanceId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "JOB_NOT_FOUND");
        assertThat(body).containsEntry("jobId", jobId.toString());
    }

    @Test
    @DisplayName("returns 409 QUALITY_SCORE_NOT_READY when job still QUEUED")
    void returns409WhenQueued() {
        job.setStatus(JobStatus.QUEUED);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getQualityScore(jobId, job.getInstanceId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "QUALITY_SCORE_NOT_READY");
    }

    @Test
    @DisplayName("flags logoPlacement issue when logoUrl is null")
    void flagsMissingLogo() {
        job.setLogoUrl(null);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getQualityScore(jobId, job.getInstanceId().toString());

        QualityScoreResponse body = (QualityScoreResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.subscores().get("logoPlacement")).isEqualTo(60);
        assertThat(body.issues())
                .anyMatch(i -> "logoPlacement".equals(i.check()) && "ERROR".equals(i.severity()));
    }
}
