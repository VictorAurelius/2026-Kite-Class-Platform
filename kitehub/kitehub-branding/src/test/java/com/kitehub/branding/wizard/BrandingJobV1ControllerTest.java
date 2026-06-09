package com.kitehub.branding.wizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.service.BrandingJobService;
import com.kitehub.branding.wizard.dto.BrandingJobResponse;
import com.kitehub.branding.wizard.quality.BrandColoursDeriver;
import com.kitehub.branding.wizard.service.MockProvisioningService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
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

    @Mock
    private BrandingJobService brandingJobService;

    @Mock
    private MockProvisioningService mockProvisioningService;

    private final BrandColoursDeriver coloursDeriver = new BrandColoursDeriver();

    private BrandingJobV1Controller controller;

    private BrandingJob job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        controller = new BrandingJobV1Controller(
                jobRepository, coloursDeriver, brandingJobService, mockProvisioningService);
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

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("GAP-390-A: tenantId populated from MDC when request carries tenant context")
    void tenantIdSourcedFromMdc() {
        String tenantId = "550e8400-e29b-41d4-a716-446655440000";
        MDC.put("tenantId", tenantId);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getJob(jobId);

        BrandingJobResponse body = (BrandingJobResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.tenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("GAP-390-A: tenantId is null when MDC has no tenant context (system jobs)")
    void tenantIdNullWhenMdcEmpty() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = controller.getJob(jobId);

        BrandingJobResponse body = (BrandingJobResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.tenantId()).isNull();
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
