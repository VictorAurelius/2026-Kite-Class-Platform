---
title: Wave GAP-122 — 12 platform-critical alerts (single-gap parallel build)
status: active
created: 2026-04-28
updated: 2026-04-28
gaps: [GAP-122]
slices: 3
parallel_agents: 3
---

# GAP-122 — Single-gap parallel wave

Single-gap focus per `pr-next-session-single-gap-handoff.md`, but internally sliced into 3 disjoint slices for parallel agent execution per `feedback_wave_plan_before_serial_prs.md`.

**Branch:** `feat/single-gap-122-platform-alerts`
**PR title:** `feat(ops): GAP-122 — 12 platform-critical alerts + runbook stubs + alerting-standards`

---

## Alert Manifest (interface — frozen, agents must NOT change names)

12 new alerts. Group name: `kitehub-platform-alerts`. Append as NEW group to:
- `kitehub/docker/prometheus/alert-rules.yml`
- `infrastructure/helm/kitehub/templates/prometheusrule.yaml`

KiteClass docker alert-rules.yml: NOT modified in this wave (these are platform-level alerts; kiteclass per-tenant alerts deferred to follow-up gap if needed). Document this split in `alerting-standards.md`.

### Critical (4 — page on-call)

| # | Alert name | Runbook filename | Severity | Metric | Status |
|---|------------|------------------|----------|--------|--------|
| 1 | `MultiTenantDataLeak` | `multi-tenant-data-leak.md` | critical | `increase(tenant_isolation_violations_total[5m]) > 0` for 1m | METRIC PENDING (custom counter — emit from `BaseEntity` tenant filter when violation detected) |
| 2 | `CertExpiryImminent` | `cert-expiry-imminent.md` | critical | `(probe_ssl_earliest_cert_expiry - time()) / 86400 < 14` for 10m | METRIC PENDING (requires `blackbox_exporter` deployment) |
| 3 | `BackupJobFailure` | `backup-job-failure.md` | critical | `time() - kite_backup_last_success_timestamp_seconds > 90000` for 5m (>25h since last success) | METRIC PENDING (custom pushgateway from cron — see GAP-117 backup runbook) |
| 4 | `FlywayMigrationFailure` | `flyway_migrations` gauge has `state="failed"` series → `flyway_migrations{state="failed"} > 0` for 1m | critical | (Spring Boot Actuator exposes per-state series via `flywayInitializer`) | METRIC PARTIAL (Spring Actuator default) |

(Alert 4 runbook filename: `flyway-migration-failure.md`)

### Warning (8 — Slack)

| # | Alert name | Runbook filename | Severity | Metric | Status |
|---|------------|------------------|----------|--------|--------|
| 5 | `AIProviderHighFailureRate` | `ai-provider-high-failure-rate.md` | warning | `rate(ai_provider_requests_total{outcome="failure"}[5m]) / clamp_min(rate(ai_provider_requests_total[5m]), 1) > 0.1` for 5m | METRIC PENDING (custom counter — emit from `OllamaClient`/AI Adapter per `ai-branding-guidelines.md` §3) |
| 6 | `EmailQueueDLQGrowing` | `email-queue-dlq-growing.md` | warning | `rabbitmq_queue_messages_ready{queue=~".*[\\.\\-](dlq\|dead)"} > 10` for 10m | METRIC depends on `rabbitmq_exporter` (already aspirational in existing alerts — same pattern) |
| 7 | `SubscriptionWebhookFailure` | `subscription-webhook-failure.md` | warning | `(sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/webhooks/.*",status=~"5.."}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/webhooks/.*"}[5m])), 1)) > 0.05` for 5m | METRIC AVAILABLE (HTTP) |
| 8 | `TenantProvisioningFailure` | `tenant-provisioning-failure.md` | warning | `(sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/admin/provisioning.*",status=~"5.."}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{uri=~"/api/v1/admin/provisioning.*"}[5m])), 1)) > 0.05` for 5m | METRIC AVAILABLE (HTTP) |
| 9 | `BrandingQualityGateFailRate` | `branding-quality-gate-fail-rate.md` | warning | `rate(branding_quality_gate_total{result="failed"}[10m]) / clamp_min(rate(branding_quality_gate_total[10m]), 1) > 0.2` for 10m | METRIC PENDING (custom counter — emit from `InstanceQualityReviewer` per `ai-branding-guidelines.md` §5) |
| 10 | `JwtAuthFailureSpike` | `jwt-auth-failure-spike.md` | warning | `sum(rate(http_server_requests_seconds_count{status="401"}[5m])) > 10` for 5m | METRIC AVAILABLE (HTTP 401) |
| 11 | `RedisEvictionRate` | `redis-eviction-rate.md` | warning | `rate(redis_evicted_keys_total[1m]) > 1000` for 5m | METRIC PENDING (requires `redis_exporter`) |
| 12 | `RateLimitBreachSpike` | `rate-limit-breach-spike.md` | warning | `sum(rate(http_server_requests_seconds_count{status="429"}[5m])) > 5` for 5m | METRIC AVAILABLE (HTTP 429) |

### Annotation contract (every alert MUST have)

```yaml
annotations:
  summary: "<short — 1 line, may use {{ $labels.X }}>"
  description: "<longer — what's broken + impact>"
  runbook_url: "/documents/05-guides/operations/runbooks/<filename>.md"
```

`runbook_url` MUST be present + non-empty (CI gate Slice A enforces).

---

## Slices (parallel — agents must NOT touch each other's files)

### Slice A — CI gate
**Files (own exclusively):**
- `scripts/check-alert-runbook-url.py` (NEW — Python script using PyYAML)
- `scripts/fixtures/alert-runbook-url/` (4 fixtures already committed by parent: `good-with-runbook.yml`, `bad-missing-runbook.yml`, `bad-empty-runbook.yml`, `good-helm-template.yml`)
- `.github/workflows/script-quality.yml` (ADD new job `alert-runbook-url`; do not modify existing jobs; add path filters for alert YAML files + script)

**Deliverable:** Self-tests pass against the 4 fixtures (1 PASS, 2 FAIL, 1 PASS-with-helm-tolerated). CI workflow job runs the check + self-test.

### Slice B — Alerts
**Files (own exclusively):**
- `kitehub/docker/prometheus/alert-rules.yml` (APPEND new group `kitehub-platform-alerts` at end; do not edit existing `kitehub-service-alerts` group)
- `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (APPEND new group `kitehub-platform-alerts` inside `spec.groups`; do not edit existing groups)

**Deliverable:** All 12 alerts implemented with exact names + runbook_url paths from manifest. YAML validates via `python3 -c "import yaml; yaml.safe_load(open('<file>'))"`. Helm file: per-line escape `{{ "{{ $labels.X }}" }}` like existing alerts.

### Slice C — Runbooks + standards
**Files (own exclusively):**
- `documents/05-guides/operations/runbooks/multi-tenant-data-leak.md` (NEW)
- `documents/05-guides/operations/runbooks/cert-expiry-imminent.md` (NEW)
- `documents/05-guides/operations/runbooks/backup-job-failure.md` (NEW)
- `documents/05-guides/operations/runbooks/flyway-migration-failure.md` (NEW)
- `documents/05-guides/operations/runbooks/ai-provider-high-failure-rate.md` (NEW)
- `documents/05-guides/operations/runbooks/email-queue-dlq-growing.md` (NEW)
- `documents/05-guides/operations/runbooks/subscription-webhook-failure.md` (NEW)
- `documents/05-guides/operations/runbooks/tenant-provisioning-failure.md` (NEW)
- `documents/05-guides/operations/runbooks/branding-quality-gate-fail-rate.md` (NEW)
- `documents/05-guides/operations/runbooks/jwt-auth-failure-spike.md` (NEW)
- `documents/05-guides/operations/runbooks/redis-eviction-rate.md` (NEW)
- `documents/05-guides/operations/runbooks/rate-limit-breach-spike.md` (NEW)
- `documents/05-guides/operations/runbooks/README.md` (UPDATE index — append the 12 new entries; do not remove existing 7)
- `documents/05-guides/alerting-standards.md` (NEW — severity rules, runbook_url contract, metric-pending pattern, kitehub vs kiteclass split)

**Deliverable:** 12 runbooks following GAP-121 template (sections: What this alert means / Immediate checks / Likely causes / Mitigation / When to escalate / Related). Each runbook ≥40 lines, project-specific (reference real services, queues, memory feedback files where applicable). `alerting-standards.md` describes severity classification + runbook_url requirement + metric-pending pattern + ownership matrix.

---

## Coordination

- Parent commits manifest + fixtures BEFORE agents spawn → agents see frozen interface.
- Agents work in worktree-isolated branches off `feat/single-gap-122-platform-alerts`.
- After agents complete, parent merges each worktree branch into feature branch sequentially.
- Final verify on parent: YAML validate + run check-alert-runbook-url.py against 3 alert files → expect 12 alerts present, all with runbook_url, all matching manifest filenames.

## Rollback

If any slice fails, parent can drop that slice's commits and ship reduced PR (e.g., A+B without C → mark gap PARTIAL until runbooks added in follow-up).

## Log

- 2026-04-28 — Wave plan created. 3 slices, 3 parallel agents. Manifest frozen.
