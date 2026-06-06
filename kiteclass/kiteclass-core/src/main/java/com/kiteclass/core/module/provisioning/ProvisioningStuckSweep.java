package com.kiteclass.core.module.provisioning;

import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * ProvisioningStuckSweep — periodic safety net for tenant frontend instances stuck mid-provisioning
 * (GAP-952, sister to {@link TenantProvisioningSaga} compensation alert).
 *
 * <p>If a provisioning saga dies before reaching DEPLOYED <em>and</em> its compensation also fails
 * (markFailed never runs — e.g. DB connection lost), the instance row stays in
 * {@link FrontendInstanceStatus#INITIALIZING} or {@link FrontendInstanceStatus#GENERATING}
 * indefinitely. BR-PROV-005 previously only logged this, so admins discovered orphans months later.
 * This sweep scans for such rows beyond a configurable age threshold (default 10 min), transitions
 * them to FAILED so an admin can retry (Bucket E), and emits a metric + structured alert token so a
 * CloudWatch alarm fires SNS.
 *
 * <p>Runs every 5 minutes by default (configurable via {@code kiteclass.provisioning.stuck-sweep.cron}).
 * Threshold via {@code kiteclass.provisioning.stuck-threshold-minutes} (default 10).
 *
 * <p>NOTE: this stack has no CloudWatch meter registry; the SNS alarm is driven by the structured
 * log token {@code TENANT_PROVISIONING_STUCK} via a CloudWatch Logs metric filter — see
 * {@code infrastructure/terraform-aws/cloudwatch-provisioning-alarms.tf}. The Micrometer counter
 * here serves in-app / Prometheus (/actuator) observability.
 *
 * @since Wave provisioning-1 Bucket D — GAP-952
 */
@Component
@Slf4j
public class ProvisioningStuckSweep {

    /**
     * Micrometer counter — stuck-sweep outcomes. Tagged {@code result=swept|sweep_failed}.
     */
    public static final String METRIC_STUCK = "tenant_provisioning.stuck";

    /**
     * Structured log token matched by the CloudWatch Logs metric filter that backs the SNS alarm.
     * Keep verbatim — changing it silently breaks the alarm (GAP-952).
     */
    public static final String ALERT_STUCK = "TENANT_PROVISIONING_STUCK";

    /** Statuses considered "in flight" — an instance lingering here past the threshold is stuck. */
    private static final List<FrontendInstanceStatus> IN_FLIGHT = List.of(
            FrontendInstanceStatus.INITIALIZING,
            FrontendInstanceStatus.GENERATING);

    private final FrontendInstanceRepository repository;
    private final InstanceLifecycleService lifecycle;
    private final MeterRegistry meterRegistry;
    private final Duration stuckThreshold;

    public ProvisioningStuckSweep(
            FrontendInstanceRepository repository,
            InstanceLifecycleService lifecycle,
            MeterRegistry meterRegistry,
            @Value("${kiteclass.provisioning.stuck-threshold-minutes:10}") long stuckThresholdMinutes) {
        this.repository = repository;
        this.lifecycle = lifecycle;
        this.meterRegistry = meterRegistry;
        this.stuckThreshold = Duration.ofMinutes(stuckThresholdMinutes);
    }

    /**
     * Scheduled entry point — runs every 5 minutes by default (see the cron expression on the
     * annotation below). Must NEVER propagate — relies on logging + Micrometer/CloudWatch alerting.
     */
    @Scheduled(cron = "${kiteclass.provisioning.stuck-sweep.cron:0 */5 * * * *}")
    public void scheduledSweep() {
        try {
            sweepStuckInstances();
        } catch (Exception ex) {
            log.error("[stuck-sweep] sweep run failed", ex);
        }
    }

    /**
     * Scan in-flight instances older than the threshold, mark them FAILED, and alert.
     * Returns the number of instances marked FAILED so tests/callers can assert deterministically.
     *
     * @return number of instances transitioned to FAILED by this sweep
     */
    public int sweepStuckInstances() {
        Instant cutoff = Instant.now().minus(stuckThreshold);
        int swept = 0;

        for (FrontendInstanceStatus status : IN_FLIGHT) {
            List<FrontendInstance> candidates = repository.findByStatusAndDeletedFalse(status);
            for (FrontendInstance instance : candidates) {
                Instant enteredAt = enteredStateAt(instance);
                if (enteredAt == null) {
                    // Anomalous: transitionTo always stamps the timestamp. Skip rather than risk
                    // failing a brand-new row whose age we cannot determine.
                    log.warn("[stuck-sweep] id={} status={} has null entered-state timestamp; skipping",
                            instance.getId(), status);
                    continue;
                }
                if (enteredAt.isBefore(cutoff)) {
                    if (markStuckFailed(instance, status, enteredAt)) {
                        swept++;
                    }
                }
            }
        }

        if (swept > 0) {
            log.info("[stuck-sweep] complete: {} stuck instance(s) marked FAILED (threshold={}min)",
                    swept, stuckThreshold.toMinutes());
        } else {
            log.debug("[stuck-sweep] complete: no stuck instances (threshold={}min)",
                    stuckThreshold.toMinutes());
        }
        return swept;
    }

    private boolean markStuckFailed(
            FrontendInstance instance, FrontendInstanceStatus status, Instant enteredAt) {
        String reason = String.format(
                "stuck in %s > %d min (provisioning-stuck-sweep auto-fail)",
                status, stuckThreshold.toMinutes());
        try {
            lifecycle.markFailed(instance.getId(), reason);
            counter("swept");
            // Structured alert token → CloudWatch Logs metric filter → SNS alarm (GAP-952).
            log.error("[stuck-sweep] {} id={} status={} enteredAt={} threshold={}min — auto-marked FAILED; "
                            + "admin retry required",
                    ALERT_STUCK, instance.getId(), status, enteredAt, stuckThreshold.toMinutes());
            return true;
        } catch (RuntimeException ex) {
            counter("sweep_failed");
            // Even the sweep's markFailed failed — instance remains stuck; still alert.
            log.error("[stuck-sweep] {} id={} status={} enteredAt={} — markFailed failed: {}; "
                            + "instance remains stuck",
                    ALERT_STUCK, instance.getId(), status, enteredAt, ex.getMessage(), ex);
            return false;
        }
    }

    private void counter(String result) {
        Counter.builder(METRIC_STUCK)
                .description("Tenant provisioning stuck-sweep outcomes")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    private static Instant enteredStateAt(FrontendInstance instance) {
        return instance.getStatus() == FrontendInstanceStatus.INITIALIZING
                ? instance.getInitializingAt()
                : instance.getGeneratingAt();
    }
}
