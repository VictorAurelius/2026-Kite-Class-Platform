# GAP-030: Disaster Recovery for AI Branding

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / Reliability
**Detected:** 2026-04-14 (simulation)

## Problem

Không có disaster recovery plan cho AI branding:

- ❌ Ollama worker crash mid-job → job lost? quota refunded?
- ❌ MinIO corrupt → assets unrecoverable?
- ❌ Postgres branding tables corrupt → rebuild from?
- ❌ RabbitMQ queue lost → in-flight jobs stuck?
- ❌ Không có RTO (Recovery Time Objective) / RPO (Recovery Point Objective)
- ❌ Không backup strategy rõ ràng

## Proposed Fix

### 1. RTO/RPO Targets

| Component | RTO | RPO |
|-----------|-----|-----|
| kitehub-branding service | 5 min | 0 (stateless) |
| Postgres branding tables | 30 min | 1 hour (hourly backup) |
| MinIO assets | 2 hours | 24 hours (daily backup) |
| RabbitMQ queues | 10 min | 5 min (durable msgs) |

### 2. Job Failure Recovery

```java
@Component
public class BrandingJobRecovery {

  // Detect stuck jobs (PROCESSING > timeout)
  @Scheduled(fixedDelay = 60000) // 1 min
  public void recoverStuckJobs() {
    var stuck = jobRepo.findStuckProcessing(Duration.ofMinutes(10));
    for (var job : stuck) {
      // Check worker alive
      if (workerDead(job.workerId)) {
        job.setStatus(FAILED);
        job.setRetryCount(job.getRetryCount() + 1);
        if (job.getRetryCount() < 3) {
          requeue(job);
        } else {
          markAbandoned(job);
          refundAIQuota(job);  // Give user quota back
          notifyUser(job);
        }
      }
    }
  }
}
```

### 3. Asset Corruption Recovery

```java
public class AssetRecoveryService {
  public void recoverCorruptedAssets(String tenantId) {
    var brokenAssets = scanBrokenAssets(tenantId);

    for (var asset : brokenAssets) {
      if (asset.category == TEMPLATE) {
        // Re-compose from template + brand params (fast)
        templateService.recompose(asset);
      } else if (asset.category == FULL_AI) {
        // Re-generate via AI (mark user to approve)
        aiService.regenerate(asset, notifyUser: true);
      } else if (asset.category == STATIC) {
        // Restore from backup
        backupService.restore(asset);
      }
    }
  }
}
```

### 4. Backup Strategy

```yaml
postgres:
  full-backup: daily 2am UTC
  incremental: hourly
  retention: 30 days
  storage: S3 separate region

minio:
  snapshot: daily 3am UTC
  versioning: enabled (7 days)
  cross-region-replication: enabled

rabbitmq:
  queue-mode: lazy (durable)
  message-persistence: enabled
  mirrored-queues: 3 nodes
```

### 5. Graceful Degradation

When AI service down:
- Fall back to TEMPLATE path (works without AI)
- Show banner "AI temporarily unavailable, template mode active"
- Queue AI requests for retry when restored
- Don't block user — allow them continue with templates

### 6. Incident Runbooks

Create runbooks for common scenarios:
- `runbooks/ollama-worker-down.md`
- `runbooks/minio-bucket-corruption.md`
- `runbooks/rabbitmq-queue-full.md`
- `runbooks/branding-service-slow.md`

### 7. Chaos Engineering

Quarterly tests:
- Kill Ollama workers → verify recovery
- Delete test tenant assets → verify regeneration
- Fill RabbitMQ queue → verify backpressure

### 8. Monitoring Integration (with GAP-019)

Alerts cho:
- Job failure rate > 5%
- Stuck jobs detected
- Asset integrity check fail
- Backup job failure

## Acceptance Criteria

- [ ] RTO/RPO documented + reviewed
- [ ] Stuck job recovery scheduler
- [ ] Asset recovery service (3 paths)
- [ ] Backup strategy implemented for all 3 stores
- [ ] Graceful degradation when AI down
- [ ] Runbooks cho 4+ scenarios
- [ ] Quarterly chaos engineering tests
- [ ] Incident response time measured

## Dependencies

- GAP-019 (monitoring/alerting)
- GAP-023 (admin tools for manual recovery)

## Log

- 2026-04-14 — Operational resilience gap identified
