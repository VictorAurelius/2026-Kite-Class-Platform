package com.kitehub.branding.wizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.BrandingJobResponse;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandingJobV1Controller")
class BrandingJobV1ControllerTest {

    @Mock
    private BrandingJobRepository jobRepository;

    private final BrandColoursDeriver coloursDeriver = new BrandColoursDeriver();

    private BrandingJobV1Controller controller;

    private BrandingJob job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        controller = new BrandingJobV1Controller(jobRepository, coloursDeriver);
        jobId = UUID.randomUUID();
        job = new BrandingJob();
        job.setId(jobId);
        job.setInstanceId(UUID.randomUUID());
        job.setOrganizationName("Trường ABC");
        job.setLanguage("vi");
        job.setStatus(JobStatus.COMPLETED);
        job.setLogoUrl("https://cdn.example.com/logo.png");
        job.setProgress(100);
        job.setRetryCount(2);
        job.setQueuedAt(LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("returns BrandingJobResponse with brandColors populated")
    void returnsBrandColors() throws Exception {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getJob(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BrandingJobResponse body = (BrandingJobResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.jobId()).isEqualTo(jobId);
        assertThat(body.brandColors()).isNotNull();
        assertThat(body.brandColors().primary()).matches("^#[0-9A-Fa-f]{6}$");
        assertThat(body.previewUrl()).isEqualTo("/api/v1/branding/jobs/" + jobId + "/preview");
        // Maps queue COMPLETED → contract DEPLOYED
        assertThat(body.status()).isEqualTo("DEPLOYED");

        // Round-trip JSON contains brandColors
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = mapper.writeValueAsString(body);
        assertThat(json).contains("\"brandColors\"");
        assertThat(json).contains("\"primary\"");
    }

    @Test
    @DisplayName("returns 404 with JOB_NOT_FOUND when job missing")
    void returns404WhenMissing() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getJob(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "JOB_NOT_FOUND");
    }
}
