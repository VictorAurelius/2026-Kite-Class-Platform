package com.kitehub.branding.wizard.preview;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import com.kitehub.branding.wizard.dto.BrandColours;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreviewController")
class PreviewControllerTest {

    @Mock
    private BrandingJobRepository jobRepository;

    private final BrandColoursDeriver coloursDeriver = new BrandColoursDeriver();

    private PreviewController controller;

    private BrandingJob job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        controller = new PreviewController(jobRepository, coloursDeriver);
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
        job.setQueuedAt(LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("returns HTML with iframe-safe headers")
    void returnsIframeSafeHeaders() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<String> response = controller.getPreview(jobId, job.getInstanceId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(response.getHeaders().getFirst("Content-Security-Policy"))
                .contains("frame-ancestors 'self'");
        assertThat(response.getHeaders().getFirst("Cache-Control"))
                .contains("private");
        assertThat(response.getHeaders().getContentType().toString())
                .contains("text/html");
    }

    @Test
    @DisplayName("renders organization name + brand colours in HTML body")
    void rendersBrandData() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<String> response = controller.getPreview(jobId, job.getInstanceId().toString());

        String html = response.getBody();
        BrandColours colours = coloursDeriver.derive(job);
        assertThat(html).isNotNull();
        assertThat(html).contains("Trường ABC");
        assertThat(html).contains(colours.primary());
        assertThat(html).contains(colours.secondary());
        assertThat(html).contains("data-testid=\"preview-hero\"");
    }

    @Test
    @DisplayName("escapes hostile organization names")
    void escapesXss() {
        job.setOrganizationName("<script>alert(1)</script>");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<String> response = controller.getPreview(jobId, job.getInstanceId().toString());

        String html = response.getBody();
        assertThat(html).doesNotContain("<script>alert(1)</script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("returns 404 HTML when job not found")
    void returns404Html() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.getPreview(jobId, job.getInstanceId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(response.getBody()).contains("Preview unavailable");
    }
}
