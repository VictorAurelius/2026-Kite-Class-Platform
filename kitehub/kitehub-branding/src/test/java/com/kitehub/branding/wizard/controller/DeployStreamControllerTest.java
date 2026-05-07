package com.kitehub.branding.wizard.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
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

    // -----------------------------------------------------------------------
    // GAP-390-B: SSE event payload assertions
    //
    // Strategy: use Mockito.mockConstruction to intercept SseEmitter creation
    // inside the controller and capture every SseEventBuilder passed to send().
    // We then introspect the captured builders to verify event NAMES match
    // the contract vocabulary (state-change | progress | complete | error |
    // heartbeat | log) and that payload Map fields shape correctly.
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GAP-390-B: in-flight stream emits initial 'state-change' with from/to/ts shape")
    void emitsInitialStateChangeEvent() throws Exception {
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, JobStatus.PROCESSING);
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));

        try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class)) {

            controller.stream(jobId, null, null);

            // Initial state-change event must be emitted exactly once.
            assertThat(mocked.constructed()).hasSize(1);
            SseEmitter constructedEmitter = mocked.constructed().get(0);
            ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
            verify(constructedEmitter, atLeastOnce()).send(captor.capture());

            // Render every captured event to its raw SSE wire form for assertion.
            List<String> payloads = renderAll(captor.getAllValues());
            assertThat(payloads).isNotEmpty();
            // SseEventBuilder.data(Map) without an explicit media type renders the map via
            // toString() — assertions match the {key=value, ...} representation.
            assertThat(payloads.get(0))
                    .as("first emitted event must be the initial state-change snapshot")
                    .contains("event:state-change")
                    .contains("to=PROCESSING")
                    .contains("from=null")
                    .contains("ts=");
        }
    }

    @Test
    @DisplayName("GAP-390-B: terminal COMPLETED job emits state-change + 'complete' with finalStatus DEPLOYED")
    void emitsCompleteEventForTerminalJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, JobStatus.COMPLETED);
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));

        try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class)) {
            controller.stream(jobId, null, null);

            assertThat(mocked.constructed()).hasSize(1);
            SseEmitter constructedEmitter = mocked.constructed().get(0);
            ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
            verify(constructedEmitter, atLeastOnce()).send(captor.capture());

            List<String> payloads = renderAll(captor.getAllValues());
            // Expect the controller to send (a) initial state-change, (b) terminal complete.
            assertThat(payloads).anyMatch(p -> p.contains("event:state-change"));
            assertThat(payloads).anyMatch(p ->
                    p.contains("event:complete")
                            && p.contains("finalStatus=DEPLOYED")
                            && p.contains("jobId=" + jobId));
        }
    }

    @Test
    @DisplayName("GAP-390-B: terminal FAILED job emits 'error' event with errorCode + retryable flag")
    void emitsErrorEventForFailedJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        BrandingJob job = makeJob(jobId, JobStatus.FAILED);
        job.setErrorMessage("downstream provider unavailable");
        when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));

        try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class)) {
            controller.stream(jobId, null, null);

            assertThat(mocked.constructed()).hasSize(1);
            SseEmitter constructedEmitter = mocked.constructed().get(0);
            ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
            verify(constructedEmitter, atLeastOnce()).send(captor.capture());

            List<String> payloads = renderAll(captor.getAllValues());
            assertThat(payloads).anyMatch(p ->
                    p.contains("event:error")
                            && p.contains("errorCode=JOB_FAILED")
                            && p.contains("retryable=true")
                            && p.contains("downstream provider unavailable"));
        }
    }

    @Test
    @DisplayName("GAP-390-B: missing-job stream emits 'error' with JOB_NOT_FOUND + retryable false")
    void emitsErrorEventForMissingJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(jobRepo.findById(jobId)).thenReturn(Optional.empty());

        try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class)) {
            controller.stream(jobId, null, null);

            assertThat(mocked.constructed()).hasSize(1);
            SseEmitter constructedEmitter = mocked.constructed().get(0);
            ArgumentCaptor<SseEventBuilder> captor = ArgumentCaptor.forClass(SseEventBuilder.class);
            verify(constructedEmitter, atLeastOnce()).send(captor.capture());

            List<String> payloads = renderAll(captor.getAllValues());
            assertThat(payloads).anyMatch(p ->
                    p.contains("event:error")
                            && p.contains("errorCode=JOB_NOT_FOUND")
                            && p.contains("retryable=false"));
        }
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

    /**
     * Materialise each captured {@link SseEventBuilder} into its raw wire form
     * by walking the builder's {@code build()} output. The wire form contains
     * {@code event:<name>\n} + {@code data:<json>\n\n} which is sufficient for
     * substring-based assertions on event names + payload field shapes.
     */
    private List<String> renderAll(List<SseEventBuilder> builders) {
        List<String> out = new ArrayList<>();
        for (SseEventBuilder b : builders) {
            StringBuilder sb = new StringBuilder();
            b.build().forEach(set -> sb.append(set.getData()));
            out.add(sb.toString());
        }
        return out;
    }
}
