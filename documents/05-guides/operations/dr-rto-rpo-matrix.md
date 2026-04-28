# DR — RTO / RPO Matrix

**Last Updated:** 2026-04-28
**Status:** Active
**Owner:** SRE / DevOps lead (solo-dev mode: @nguyenvankiet)
**Closes:** Part of GAP-119 — Platform-Wide DR Runbook + RTO/RPO Matrix
**Parent doc:** [`../disaster-recovery-plan.md`](../disaster-recovery-plan.md)

---

## 1. Definitions

- **RTO (Recovery Time Objective)** — Wall-clock time from disaster declaration → service restored. Lower = better, but cost grows non-linearly.
- **RPO (Recovery Point Objective)** — Maximum acceptable data loss, measured backwards from disaster moment. RPO 15min = "we accept losing up to last 15 min of writes". RPO 0 = "no data loss at all" (synchronous replication).

These targets apply to **production** environment. Dev/staging have no RTO/RPO commitments — restore is best-effort there.

---

## 2. Component Matrix

| # | Component | RTO | RPO | Recovery mode | Backup source | Restore reference |
|--:|-----------|----:|----:|---------------|---------------|-------------------|
| 1 | **kitehub-subscription DB** (Postgres) | **1h** | **15min** | RDS PITR (point-in-time recovery) | RDS automated backups + WAL shipping | [`../restore-procedure.md`](../restore-procedure.md) Scenario A (GAP-117) |
| 2 | **kiteclass tenant DBs** (Postgres, multi-tenant) | **2h** | **1h** | pg_dump → fresh DB | `DatabaseBackupScheduler` (GAP-093) → S3 hourly | [`../restore-procedure.md`](../restore-procedure.md) Scenario B (GAP-117) |
| 3 | **MinIO / S3 assets** (logos, banners, hero, templates) | **4h** | **24h** | S3 versioning + cross-region replication | S3 versioning + replication per [GAP-118](../../04-quality/gaps/GAP-118-minio-backup-strategy.md) (`infrastructure/terraform-aws/s3-ecr.tf`) | [`../restore-procedure.md`](../restore-procedure.md) Scenario C (GAP-117 + GAP-118) |
| 4 | **RabbitMQ queues** (events + AI jobs) | **10min** | **5min** | Durable queues + mirrored across nodes | Message persistence + mirrored-queue policy | Restart broker; in-flight messages persist |
| 5 | **Redis sessions** | **5min** | **N/A** (forced re-login) | Drop sessions + redirect to login | None (intentionally ephemeral) | Restart Redis; users re-auth |
| 6 | **AI artifacts / branding-domain** | **4h** | **24h** | Per [GAP-030](../../04-quality/gaps/GAP-030-disaster-recovery-ai-branding.md) | Same S3 versioning (GAP-118) + branding tables in subscription DB | Re-compose template path OR regenerate via AI |
| 7 | **Outbox tables** (per-module domain outbox) | Same as parent DB | Same as parent DB | Restored when parent DB restored | Part of DB backup | N/A (table is part of DB restore) |
| 8 | **Static frontend assets** (CDN + S3) | **30min** | **N/A** (deployable from git) | Re-deploy from CI artifact | Git + Docker registry (ECR) | Re-run last successful CI deploy job |

---

## 3. Rationale per row

### Row 1 — kitehub-subscription DB (RTO 1h / RPO 15min)

This DB holds billing, subscription state, tenant lifecycle, payment events. **Most critical** because losing 15+ min of payment data = direct financial impact.

- RDS PITR gives RPO ~5min (WAL shipping interval) but we set 15min as target accounting for detection + decision time
- Multi-AZ enabled in prod (Terraform) — automatic failover handles single-AZ loss in <2min, no human decision needed for that case
- RTO 1h = time to detect, declare, run PITR (~20min for restore command + verification per GAP-117 Scenario A)

### Row 2 — kiteclass tenant DBs (RTO 2h / RPO 1h)

Per-tenant schemas hold attendance, grades, class data. Tighter RTO than assets because **academic operations are time-sensitive** (a class running NOW needs attendance NOW).

- pg_dump runs hourly via `DatabaseBackupScheduler` (GAP-093) — RPO bounded by that interval
- RTO 2h reflects fresh-DB restore path which is slower than PITR (no WAL replay)
- For especially large tenants (>10GB DB) RTO may exceed 2h — flagged as known limitation, will need RDS PITR upgrade if/when single tenant exceeds threshold

### Row 3 — MinIO / S3 assets (RTO 4h / RPO 24h)

Logos, banners, hero images, AI-generated assets. Slower targets because:

- Recoverable: TEMPLATE category can be re-composed (cheap) per `ai-branding-guidelines.md` §1
- Re-generatable: FULL_AI category can be re-generated (more expensive but bounded)
- Only STATIC user-uploaded assets are truly irreplaceable → these benefit from S3 versioning (per [GAP-118](../../04-quality/gaps/GAP-118-minio-backup-strategy.md))

S3 cross-region replication per [GAP-118](../../04-quality/gaps/GAP-118-minio-backup-strategy.md) `infrastructure/terraform-aws/s3-ecr.tf` provides geographic redundancy. RPO 24h is conservative — actual replication is near-real-time but we don't commit tighter SLA without monitoring proof.

### Row 4 — RabbitMQ queues (RTO 10min / RPO 5min)

Durable queues + message persistence + mirrored across 3 nodes per `ai-branding-guidelines.md` §10 Strategy table.

- RTO 10min = restart broker time (durable messages survive restart)
- RPO 5min = max time between fsync to disk for in-flight, unacked messages
- Outbox pattern (per `design-patterns.md` §3.5.1) means any business-critical event is also persisted to DB before broker — defense-in-depth

### Row 5 — Redis sessions (RTO 5min / RPO N/A)

Sessions are intentionally ephemeral. On Redis loss:
- All users re-authenticate (~5min worst case for cold app + Redis restart)
- No data loss — session data is derived from DB
- Trade-off accepted: simpler ops + no Redis backup overhead, vs. minor UX (re-login) on rare events

### Row 6 — AI artifacts (RTO 4h / RPO 24h)

Tracked separately per [GAP-030](../../04-quality/gaps/GAP-030-disaster-recovery-ai-branding.md). This row in the matrix exists for completeness; full GAP-030 runbook governs detailed recovery (job recovery, asset recovery service per category, graceful degradation).

### Row 7 — Outbox tables

Outbox rows are part of the parent DB transaction (per `design-patterns.md` §3.5.1). Restoring the parent DB restores the outbox. Pending outbox rows replay automatically when app restarts — no separate restore procedure.

### Row 8 — Static frontend assets

CDN-fronted, deployed from git via CI. "Restore" = re-run CI deploy. RTO bounded by CI pipeline duration (~20-30min). RPO N/A because git is the source of truth.

---

## 4. Recovery Cost / Effort Tier

Translate RTO/RPO into operator effort:

| Tier | Components | Effort during DR |
|------|-----------|------------------|
| **Auto** | RabbitMQ (mirrored), Multi-AZ RDS failover (single-AZ loss), Static FE (CI re-run) | Monitor only |
| **Operator-driven** | RDS PITR (Row 1), pg_dump restore (Row 2) | Operator runs `restore-procedure.md` script |
| **Coordinator-driven** | Region failover (S1), Ransomware (S3) | DR Coordinator + multiple operators per scenario runbook |

---

## 5. Trade-Offs vs Cost

These targets are **early-SaaS-tier**. Tightening them costs:

| Tighter target | Cost / change required |
|---------------|------------------------|
| RPO 0 for subscription DB | Synchronous multi-AZ replica + readonly fallback during failover; +20-30% RDS cost |
| RTO 30min for region failover | Pre-warmed us-east-1 stack always running; +50% infra cost (effectively 1.5× regions) |
| RPO 5min for tenant DBs | Switch from pg_dump hourly to RDS managed (Aurora) with continuous backups; ~2× DB cost |
| RPO 1h for assets | Tighter S3 replication monitoring + alerts on replication lag; small ops cost, larger ongoing tuning |

Decisions on tightening land when:
- Tenants on paid tiers contract for tighter SLA
- A real disaster proves current targets insufficient
- Compliance requires (e.g., financial-tier tenant onboarded)

---

## 6. Review Cadence

This matrix reviewed:

- **Quarterly** — alongside DR exercise (per `disaster-recovery-plan.md` §7)
- **After every declared DR** — actual measured RTO compared to target; adjust either targets or infrastructure
- **When tenant SLA contracts change** — paid tier with tighter SLA → re-baseline relevant rows
- **When major architecture changes** — e.g., switch to Aurora, multi-region active-active, etc.

### Review log

| Date | Reviewer | Changes |
|------|---------|---------|
| 2026-04-28 | @nguyenvankiet | Initial matrix (closes GAP-119) |

Next review: **2026-07-28** (post Q3 2026 DR exercise).

---

## 7. Verification — how to know targets are realistic

Each row's target is **claim** until validated by:

1. **Drill measurement** — quarterly exercise per `disaster-recovery-plan.md` §7 measures actual RTO/RPO
2. **Production incident** — real-world data when disasters happen (and they will)
3. **Smoke test** — monthly `verify-restore.sh` (GAP-117 Phase 2 CI) confirms backups are restorable

Current state (2026-04-28): Row 1, 2, 6 targets are **claims** until first quarterly exercise (Q3 2026 proposed). Rows 4, 5, 8 targets are **likely realistic** based on standard infrastructure behavior. Row 3 target depends on GAP-118 Terraform changes landing.

This honesty matters: a published target the team can't hit is worse than no target — it creates false security. When a row's target is refuted by drill data, this matrix gets updated, NOT hidden.

---

## 8. Related

- Parent: [`../disaster-recovery-plan.md`](../disaster-recovery-plan.md)
- Restore procedures: [`../restore-procedure.md`](../restore-procedure.md) (GAP-117)
- Backup strategy: GAP-118 (`infrastructure/terraform-aws/s3-ecr.tf` + `kitehub/docker-compose.kitehub.yml` MinIO section)
- AI-domain DR: [GAP-030](../../04-quality/gaps/GAP-030-disaster-recovery-ai-branding.md)
- Audit: [`../../04-quality/audits/ops/ops-readiness-audit-2026-04-19.md`](../../04-quality/audits/ops/ops-readiness-audit-2026-04-19.md) §6

---

## 9. Log

- **2026-04-28** — Matrix created. Closes the matrix portion of GAP-119. Targets initially set as claims; first quarterly exercise (Q3 2026) will validate Rows 1, 2, 6 against measured drill data.
