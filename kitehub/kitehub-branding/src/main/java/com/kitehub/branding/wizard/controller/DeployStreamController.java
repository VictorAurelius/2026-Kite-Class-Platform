package com.kitehub.branding.wizard.controller;

import com.kitehub.branding.domain.entity.BrandingJob;
import com.kitehub.branding.domain.enums.JobStatus;
import com.kitehub.branding.repository.BrandingJobRepository;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@Tag(name = "AI Branding Wizard SSE", description = "Live deploy progress stream")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-c", "controller", "branding-deploy-stream"})
public class DeployStreamController {

    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L; // 10 min

    /**
     * Wave 36 GAP-393-B — backpressure cap on concurrent subscribers per job.
     * Defends against a single misbehaving client opening unbounded streams.
     * Configurable via {@code kitehub.branding.deploy-stream.max-emitters-per-job}.
     */
    private final int maxEmittersPerJob;

    /** All active emitters per jobId — multiple subscribers per job allowed up to {@link #maxEmittersPerJob}. */
    private final Map<UUID, List<EmitterEntry>> emitters = new ConcurrentHashMap<>();

    private final BrandingJobRepository brandingJobRepository;

    /** GAP-1021 — mints the short-lived token a browser EventSource carries via ?access_token. */
    private final com.kitehub.branding.wizard.sse.SseTokenService sseTokenService;

    /**
     * GAP-1108 / G1 walk 2026-06-12 — nguồn {@code frontendUrl} cho event {@code complete}:
     * marker {@code deploy-completed} (metadata.frontendUrl) do MockProvisioningService ghi.
     * Nullable (test seams cũ không inject) → complete event bỏ qua frontendUrl khi vắng.
     */
    private final com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository lifecycleEventRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public DeployStreamController(
            BrandingJobRepository brandingJobRepository,
            com.kitehub.branding.wizard.sse.SseTokenService sseTokenService,
            com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository lifecycleEventRepository,
            @org.springframework.beans.factory.annotation.Value(
                    "${kitehub.branding.deploy-stream.max-emitters-per-job:20}") int maxEmittersPerJob) {
        this.brandingJobRepository = brandingJobRepository;
        this.sseTokenService = sseTokenService;
        this.lifecycleEventRepository = lifecycleEventRepository;
        this.maxEmittersPerJob = maxEmittersPerJob;
    }

    /** Test seam — preserve no-arg-equivalent constructor for unit tests. */
    public DeployStreamController(BrandingJobRepository brandingJobRepository) {
        this(brandingJobRepository,
                new com.kitehub.branding.wizard.sse.SseTokenService("test-sse-secret", 120),
                null,
                20);
    }

    /** Test seam — preserve the (repo, maxEmitters) constructor used by backpressure tests. */
    public DeployStreamController(BrandingJobRepository brandingJobRepository, int maxEmittersPerJob) {
        this(brandingJobRepository,
                new com.kitehub.branding.wizard.sse.SseTokenService("test-sse-secret", 120),
                null,
                maxEmittersPerJob);
    }

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
        List<EmitterEntry> bucket = emitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>());

        // Wave 36 GAP-393-B — backpressure cap. Reject when over per-job limit.
        if (bucket.size() >= maxEmittersPerJob) {
            log.warn("SSE backpressure: job={} already has {} emitters (max={}), rejecting subscriber",
                    jobId, bucket.size(), maxEmittersPerJob);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("errorCode", "TOO_MANY_SUBSCRIBERS",
                                "message", "subscriber cap reached for this job",
                                "retryable", true)));
                emitter.complete();
            } catch (IOException ex) {
                safeComplete(emitter, ex);
            }
            return emitter;
        }
        bucket.add(entry);

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
     * Mint a short-lived SSE access token (GAP-1021 part 2 — FM-4).
     *
     * <p>Called by an already-authenticated fetch (carrying gateway {@code X-User-*} headers)
     * BEFORE opening the {@code deploy-stream}/{@code preview} EventSource. The browser then
     * opens {@code .../deploy-stream?access_token=<token>} — EventSource can't set headers, so
     * {@code SseQueryTokenAuthFilter} verifies this token to re-establish auth for the stream.</p>
     *
     * @return {@code {token, expiresInSeconds}}, or {@code 404} if the job doesn't exist.
     */
    @PostMapping("/{jobId}/sse-token")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TEACHER','ACCOUNTANT','PLATFORM_ADMIN','ADMIN','STAFF')")
    public ResponseEntity<Map<String, Object>> mintSseToken(
            @PathVariable UUID jobId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (brandingJobRepository.findById(jobId).isEmpty()) {
            Map<String, Object> notFound = new LinkedHashMap<>();
            notFound.put("error", "JOB_NOT_FOUND");
            notFound.put("jobId", jobId.toString());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
        }
        String token = sseTokenService.mint(userId, roles, jobId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("expiresInSeconds", sseTokenService.getTtlSeconds());
        return ResponseEntity.ok(body);
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
                        removeEmitter(jobId, e);
                    }
                } catch (IOException | IllegalStateException ex) {
                    // Wave 36 GAP-393-B — backpressure: client gone, send buffer full,
                    // or emitter already completed. Previously called completeWithError
                    // but left entry in registry, causing dead emitters to pile up +
                    // every poller cycle to keep tripping the same exception.
                    log.warn("SSE poller send-failure for job={}, removing emitter: {}",
                            jobId, ex.getMessage());
                    safeComplete(e.emitter, ex);
                    removeEmitter(jobId, e);
                }
            }
        }
    }

    /** Heartbeat ~30s for keepalive on idle streams. */
    @Scheduled(fixedDelayString = "${kitehub.branding.deploy-stream.heartbeat-ms:30000}")
    public void heartbeat() {
        if (emitters.isEmpty()) return;
        for (Map.Entry<UUID, List<EmitterEntry>> entry : emitters.entrySet()) {
            UUID jobId = entry.getKey();
            for (EmitterEntry e : entry.getValue()) {
                try {
                    e.emitter.send(SseEmitter.event().name("heartbeat").data(new HashMap<>()));
                } catch (IOException | IllegalStateException ex) {
                    // Wave 36 GAP-393-B — same cleanup pattern as poller.
                    log.warn("SSE heartbeat send-failure for job={}, removing emitter: {}",
                            jobId, ex.getMessage());
                    safeComplete(e.emitter, ex);
                    removeEmitter(jobId, e);
                }
            }
        }
    }

    /** Wave 36 GAP-393-B — never let completeWithError throw twice. */
    private static void safeComplete(SseEmitter emitter, Throwable cause) {
        try {
            emitter.completeWithError(cause);
        } catch (Exception ignored) {
            // Already completed elsewhere — nothing to do.
        }
    }

    private void emitTerminal(SseEmitter emitter, BrandingJob job) throws IOException {
        if (job.getStatus() == JobStatus.COMPLETED) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("jobId", job.getId().toString());
            body.put("finalStatus", "DEPLOYED");
            body.put("ts", Instant.now().toString());
            // G1 walk 2026-06-12: FE DoneStep cần frontendUrl từ complete event — thiếu nó
            // FE fallback tự build https://{slug}.kitehub.me (sai domain local + sai slug).
            String frontendUrl = resolveFrontendUrl(job);
            if (frontendUrl != null) {
                body.put("frontendUrl", frontendUrl);
            }
            emitter.send(SseEmitter.event().name("complete").data(body));
        } else if (job.getStatus() == JobStatus.FAILED) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("errorCode", "JOB_FAILED");
            body.put("message", job.getErrorMessage() != null ? job.getErrorMessage() : "job failed");
            body.put("retryable", true);
            emitter.send(SseEmitter.event().name("error").data(body));
        }
    }

    /**
     * Đọc {@code frontendUrl} từ marker {@code deploy-completed} mới nhất của instance
     * (metadata JSON do MockProvisioningService ghi — same source GAP-1108 deploy-status).
     * Null-safe mọi nhánh: repo vắng (test seam) / marker chưa ghi / metadata hỏng → null.
     */
    private String resolveFrontendUrl(BrandingJob job) {
        if (lifecycleEventRepository == null || job.getInstanceId() == null) {
            return null;
        }
        try {
            return lifecycleEventRepository
                    .findByInstanceIdSince(job.getInstanceId(),
                            java.time.LocalDateTime.now().minusYears(5),
                            org.springframework.data.domain.PageRequest.of(0, 50))
                    .stream()
                    .filter(e -> "deploy-completed".equals(e.getEventType()))
                    .findFirst()
                    .map(e -> {
                        try {
                            com.fasterxml.jackson.databind.JsonNode meta =
                                    new com.fasterxml.jackson.databind.ObjectMapper()
                                            .readTree(e.getMetadataJson());
                            com.fasterxml.jackson.databind.JsonNode url = meta.get("frontendUrl");
                            return url != null && url.isTextual() ? url.asText() : null;
                        } catch (Exception ex) {
                            return null;
                        }
                    })
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.debug("resolveFrontendUrl failed for job {}: {}", job.getId(), ex.getMessage());
            return null;
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
