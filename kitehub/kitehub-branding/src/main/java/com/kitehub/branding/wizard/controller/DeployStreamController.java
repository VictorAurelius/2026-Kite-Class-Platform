package com.kitehub.branding.wizard.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE deploy-stream endpoint (Wave 34 sub-GAP-272e).
 *
 * <p>Streams lifecycle progress + log events for the AI Branding wizard's
 * "deploy" step at {@code GET /api/v1/branding/jobs/{jobId}/deploy-stream}.
 *
 * <p><b>v0 implementation:</b> emitters are driven by a scheduled poller
 * (every 2s) that watches {@link BrandingJob} status transitions. When the
 * RabbitMQ {@code branding.deploy.*} topic exchange is wired, swap the poller
 * for a queue listener — the SSE emit surface stays unchanged. Heartbeats
 * fire every 30s while the job is in flight to keep proxies + LBs friendly.
 *
 * <p>Event types mirror api-contract.md verbatim:
 * {@code log | progress | state-change | complete | error | heartbeat}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branding/jobs")
@RequiredArgsConstructor
@Tag(name = "AI Branding Wizard SSE", description = "Live deploy progress stream")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-c", "controller", "branding-deploy-stream"})
public class DeployStreamController {

    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L; // 10 min

    /** All active emitters per jobId — multiple subscribers per job allowed. */
    private final Map<UUID, List<EmitterEntry>> emitters = new ConcurrentHashMap<>();

    private final BrandingJobRepository brandingJobRepository;

    @GetMapping(value = "/{jobId}/deploy-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable UUID jobId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            HttpServletRequest request) {

        Optional<BrandingJob> jobOpt = brandingJobRepository.findById(jobId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        if (jobOpt.isEmpty()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("errorCode", "JOB_NOT_FOUND",
                                "message", "job not found",
                                "retryable", false)));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
            return emitter;
        }

        BrandingJob job = jobOpt.get();
        EmitterEntry entry = new EmitterEntry(emitter, job.getStatus());
        emitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(entry);

        emitter.onCompletion(() -> removeEmitter(jobId, entry));
        emitter.onTimeout(() -> removeEmitter(jobId, entry));
        emitter.onError(ex -> removeEmitter(jobId, entry));

        // Send initial state-change so subscribers see current status immediately.
        try {
            Map<String, Object> initial = new LinkedHashMap<>();
            initial.put("from", null);
            initial.put("to", job.getStatus().name());
            initial.put("ts", Instant.now().toString());
            emitter.send(SseEmitter.event().name("state-change").data(initial));

            if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
                emitTerminal(emitter, job);
                emitter.complete();
                removeEmitter(jobId, entry);
            }
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            removeEmitter(jobId, entry);
        }

        return emitter;
    }

    /**
     * Poll for status changes + emit updates. Replaceable by a RabbitMQ
     * listener once the {@code branding.deploy.*} exchange is wired.
     */
    @Scheduled(fixedDelayString = "${kitehub.branding.deploy-stream.poll-ms:2000}")
    public void pollJobStates() {
        if (emitters.isEmpty()) return;
        for (Map.Entry<UUID, List<EmitterEntry>> entry : emitters.entrySet()) {
            UUID jobId = entry.getKey();
            BrandingJob job = brandingJobRepository.findById(jobId).orElse(null);
            if (job == null) continue;

            for (EmitterEntry e : entry.getValue()) {
                try {
                    if (e.lastStatus != job.getStatus()) {
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("from", e.lastStatus.name());
                        body.put("to", job.getStatus().name());
                        body.put("ts", Instant.now().toString());
                        e.emitter.send(SseEmitter.event().name("state-change").data(body));
                        e.lastStatus = job.getStatus();
                    }

                    if (job.getProgress() != null) {
                        Map<String, Object> prog = new LinkedHashMap<>();
                        prog.put("step", job.getCurrentStep() != null ? job.getCurrentStep() : "GENERATE");
                        prog.put("percent", job.getProgress());
                        e.emitter.send(SseEmitter.event().name("progress").data(prog));
                    }

                    if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
                        emitTerminal(e.emitter, job);
                        e.emitter.complete();
                    }
                } catch (IOException ex) {
                    e.emitter.completeWithError(ex);
                }
            }
        }
    }

    /** Heartbeat ~30s for keepalive on idle streams. */
    @Scheduled(fixedDelayString = "${kitehub.branding.deploy-stream.heartbeat-ms:30000}")
    public void heartbeat() {
        if (emitters.isEmpty()) return;
        for (List<EmitterEntry> list : emitters.values()) {
            for (EmitterEntry e : list) {
                try {
                    e.emitter.send(SseEmitter.event().name("heartbeat").data(new HashMap<>()));
                } catch (IOException ex) {
                    e.emitter.completeWithError(ex);
                }
            }
        }
    }

    private void emitTerminal(SseEmitter emitter, BrandingJob job) throws IOException {
        if (job.getStatus() == JobStatus.COMPLETED) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jobId", job.getId().toString());
            body.put("finalStatus", "DEPLOYED");
            body.put("ts", Instant.now().toString());
            emitter.send(SseEmitter.event().name("complete").data(body));
        } else if (job.getStatus() == JobStatus.FAILED) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("errorCode", "JOB_FAILED");
            body.put("message", job.getErrorMessage() != null ? job.getErrorMessage() : "job failed");
            body.put("retryable", true);
            emitter.send(SseEmitter.event().name("error").data(body));
        }
    }

    private void removeEmitter(UUID jobId, EmitterEntry entry) {
        List<EmitterEntry> list = emitters.get(jobId);
        if (list != null) {
            list.remove(entry);
            if (list.isEmpty()) emitters.remove(jobId);
        }
    }

    /** Tracks per-subscriber state — used to detect status transitions in poller. */
    static final class EmitterEntry {
        final SseEmitter emitter;
        volatile JobStatus lastStatus;

        EmitterEntry(SseEmitter emitter, JobStatus initial) {
            this.emitter = emitter;
            this.lastStatus = initial;
        }
    }

    /** Visible for tests — count of active emitters for a given job. */
    int activeEmitterCount(UUID jobId) {
        List<EmitterEntry> list = emitters.get(jobId);
        return list == null ? 0 : list.size();
    }
}
