package com.kitehub.branding.wizard.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DeployStreamController")
class DeployStreamControllerTest {

    private BrandingJobRepository jobRepo;
    private DeployStreamController controller;

    @BeforeEach
    void setUp() {
        jobRepo = mock(BrandingJobRepository.class);
        controller = new DeployStreamController(jobRepo);
    }

    @Test
    @DisplayName("stream — non-existent job returns emitter that completes with error event")
    void streamMissingJob() {
        UUID jobId = UUID.randomUUID();
        when(jobRepo.findById(jobId)).thenReturn(Optional.empty());

        SseEmitter emitter = controller.stream(jobId, null, null);

        assertThat(emitter).isNotNull();
        // After stream() returns the missing-job branch, no entry should be tracked.
        assertThat(controller.activeEmitterCount(jobId)).isZero();
    }

    @Test
    @DisplayName("stream — in-flight job registers an active emitter")
    void streamInFlight() {
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, JobStatus.PROCESSING);
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));

        SseEmitter emitter = controller.stream(jobId, null, null);

        assertThat(emitter).isNotNull();
        assertThat(controller.activeEmitterCount(jobId)).isEqualTo(1);
    }

    @Test
    @DisplayName("stream — terminal job completes immediately + no lingering emitter")
    void streamTerminalJob() {
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, JobStatus.COMPLETED);
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));

        SseEmitter emitter = controller.stream(jobId, null, null);

        assertThat(emitter).isNotNull();
        // Terminal jobs are immediately completed and removed from the registry.
        assertThat(controller.activeEmitterCount(jobId)).isZero();
    }

    @Test
    @DisplayName("heartbeat — no active emitters → safe no-op")
    void heartbeatNoEmitters() {
        controller.heartbeat();
        // Should not throw — silent no-op when registry is empty.
    }

    @Test
    @DisplayName("pollJobStates — no active emitters → safe no-op")
    void pollNoEmitters() {
        controller.pollJobStates();
    }

    private BrandingJob makeJob(UUID id, JobStatus status) {
        BrandingJob job = new BrandingJob();
        job.setId(id);
        job.setInstanceId(UUID.randomUUID());
        job.setStatus(status);
        job.setProgress(50);
        job.setCurrentStep("GENERATE");
        job.setOrganizationName("Test");
        job.setLanguage("vi");
        return job;
    }
}
