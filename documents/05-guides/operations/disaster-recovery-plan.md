# Disaster Recovery Plan — Platform-Wide

**Last Updated:** 2026-04-28
**Status:** Active
**Owner:** SRE / DevOps lead (solo-dev mode: @nguyenvankiet)
**Closes:** GAP-119 — Platform-Wide DR Runbook + RTO/RPO Matrix
**Related:** [`incident-response-runbook.md`](incident-response-runbook.md) · [`rollback-procedure.md`](rollback-procedure.md) · [`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md) · [`operations/dr-rto-rpo-matrix.md`](operations/dr-rto-rpo-matrix.md)

---

## 1. Purpose & Scope

Disaster Recovery (DR) là **platform-wide** plan để khôi phục toàn bộ Kite Platform (KiteHub + KiteClass) sau các sự cố nghiêm trọng:

- Region/AZ failure
- Database loss / corruption
- Ransomware / mass data tampering
- Mass tenant provisioning failure
- Critical external dependency downtime (>2h)

### DR vs Incident vs Rollback

| Document | Scope | Trigger | Time-frame |
|----------|-------|---------|-----------|
| [`incident-response-runbook.md`](incident-response-runbook.md) | Operational incidents (single service down, slow API) | Alert / user report | Minutes to hours, **mitigate then resume** |
| [`rollback-procedure.md`](rollback-procedure.md) | Bad deploy needs reverting | Deploy-related regression | Minutes, **revert to known-good** |
| **THIS DOCUMENT (DR plan)** | Multi-component / data-loss / catastrophic | Region down, DB lost, ransomware, …  | Hours to days, **rebuild from backups** |

DR overlaps incident-response — for same trigger you start in incident-response, escalate to DR if scope exceeds normal mitigation. DR Coordinator (§3) decides escalation.

### Out-of-scope here

- AI Branding domain-specific recovery → [GAP-030](../04-quality/gaps/GAP-030-disaster-recovery-ai-branding.md). This document **extends** GAP-030 (does not replace) — when AI subsystem is the only impacted scope, GAP-030 runbook supersedes.
- Backup strategy details (S3 versioning, MinIO replication) → [GAP-118](../04-quality/gaps/GAP-118-minio-backup-strategy.md) + Terraform sources
- Restore step-by-step (RDS PITR, pg_dump → fresh DB) → [`restore-procedure.md`](restore-procedure.md) (GAP-117). Each scenario below **references** that runbook rather than duplicating commands.

---

## 2. RTO / RPO Targets (summary)

Full matrix in [`operations/dr-rto-rpo-matrix.md`](operations/dr-rto-rpo-matrix.md). Quick lookup:

| Component | RTO | RPO | Recovery mode |
|-----------|----:|----:|---------------|
| kitehub-subscription DB | 1h | 15min | RDS PITR |
| kiteclass tenant DBs | 2h | 1h | pg_dump + S3 |
| MinIO / S3 assets | 4h | 24h | S3 versioning + cross-region replication (GAP-118) |
| RabbitMQ queues | 10min | 5min | Durable + mirrored queues |
| Redis sessions | 5min | N/A | Drop sessions, redirect to login |
| AI artifacts (branding) | 4h | 24h | Per [GAP-030](../04-quality/gaps/GAP-030-disaster-recovery-ai-branding.md) |

**Reading the matrix:**
- **RTO (Recovery Time Objective)** — wall-clock từ khi tuyên bố disaster đến khi service restored.
- **RPO (Recovery Point Objective)** — tối đa data có thể chấp nhận mất, tính ngược từ thời điểm disaster.

These targets reflect **solo-dev / early-stage SaaS**. As tenants grow + paid SLA contracts arrive, tighter targets land in a re-baselined matrix (tracked in `dr-rto-rpo-matrix.md` §6 review cadence).

---

## 3. DR Coordinator Role

### Responsibility

DR Coordinator owns the disaster end-to-end:

1. **Declare disaster** — escalate from incident → DR (criteria below)
2. **Assemble responders** — ping on-call, exec, legal if needed
3. **Drive scenario runbook** (§5) — single source of "what step are we on"
4. **Communicate** — internal updates every 30min, tenant comms within SLA, regulator within legal-required window
5. **Decide trade-offs** — partial restore vs full, rollback vs forward-fix, paid-tenant priority order
6. **Post-DR retrospective** within 7 days

### Solo-dev mode

In current solo-dev mode (2026-04-28), the **DR Coordinator role is held by @nguyenvankiet (project owner)**. As team grows, role rotates — solo-dev mode is documented limitation, not the steady-state design.

When team scales:
- Primary DR Coordinator: SRE lead
- Backup: Tech lead
- Rotation: 1 quarter; both must complete tabletop exercise (§7) before serving

### Escalation criteria (when to declare disaster)

Declare DR (not incident) when ANY of:

- **Multi-service** P0 incident lasting >30 min with no path to mitigation
- Confirmed **data loss** beyond auto-backup window (e.g. backup also corrupted)
- **Region/AZ failure** affecting >50% of services
- **Confirmed ransomware** or hostile data tampering
- **Compliance trigger** — incident touches PII obligations under PDPL / financial obligations under ND-13/2023/NĐ-CP and reporting deadlines apply

### Escalation path

```
1. On-call engineer detects incident → triages per incident-response-runbook.md §3
   ↓ (escalation criteria met)
2. DR Coordinator declared (self-declared by on-call OR by team lead reviewing pager)
   ↓ Coordinator pages:
3. Tech lead + secondary on-call (always)
   ↓ Coordinator decides whether to also page:
4. CTO (if revenue-impacting >2h or compliance trigger)
   ↓
5. Legal counsel (if PII / regulator notification clock starts)
   ↓
6. PR / Comms lead (if public-facing comms required)
```

Solo-dev mode collapses 1–6 onto one person; the path documents future steady state and is the trigger list to consult even alone.

---

## 4. Communication Templates

### 4.1 Internal — DR declared (Slack `#incidents` or equivalent)

```
🚨 DISASTER DECLARED — [scenario codename, e.g. S2-DB-CRASH]
COORDINATOR: @[name]
DECLARED AT: [HH:MM UTC]
SCOPE: [components affected]
ESTIMATED RTO: [hh:mm based on §5 scenario]
NEXT UPDATE: [time, max 30 min]
RUNBOOK: documents/05-guides/disaster-recovery-plan.md §5.[N]
```

### 4.2 Internal — status update (every 30 min while DR active)

```
DR UPDATE — [scenario] [HH:MM]
PROGRESS: [step N of M complete; current step description]
BLOCKERS: [none / specific blocker]
REVISED RTO: [hh:mm]
NEXT UPDATE: [time]
```

### 4.3 Tenant-facing email (P0/P1, >30 min outage)

```
Subject: [Kite Platform] Sự cố hệ thống — đang khôi phục

Kính gửi quý trường,

Hệ thống Kite Platform hiện đang gặp sự cố [bắt đầu lúc HH:MM].

Phạm vi ảnh hưởng:
- [Tính năng/dịch vụ bị ảnh hưởng]
- [Số lượng người dùng/trường ước tính]

Nguyên nhân (sơ bộ):
[1-2 câu, không kỹ thuật, không đổ lỗi vendor cụ thể nếu chưa xác định]

Tình trạng khôi phục:
- Đội ngũ kỹ thuật đang xử lý
- Dự kiến khôi phục: [HH:MM cùng ngày | trong vòng X giờ]
- Cập nhật tiếp theo: [HH:MM]

Dữ liệu của quý trường:
[Có thể bị ảnh hưởng X giờ gần nhất | An toàn, không bị mất dữ liệu]

Chúng tôi sẽ gửi email cập nhật mỗi giờ cho đến khi sự cố được khắc phục.

Trân trọng,
Đội Vận hành Kite Platform
support@kiteplatform.example
```

English variant: same structure, header `[Kite Platform] Service Disruption — Recovery in Progress`.

### 4.4 Regulator notice (PDPL / data-loss disclosure)

PDPL (Nghị định 13/2023/NĐ-CP) yêu cầu thông báo trong **72 giờ** kể từ khi phát hiện sự cố data breach gây ảnh hưởng đến dữ liệu cá nhân.

```
KÍNH GỬI: Cục An toàn thông tin / Bộ Công an (theo §47 Nghị định 13/2023/NĐ-CP)
TỪ: Kite Platform (đăng ký xử lý dữ liệu cá nhân số [XXX])
NGÀY: [DD/MM/YYYY HH:MM]
LOẠI THÔNG BÁO: Sự cố lộ/mất dữ liệu cá nhân

1. Thời điểm phát hiện sự cố: [HH:MM DD/MM/YYYY UTC+7]
2. Loại sự cố: [Lộ thông tin cá nhân / Mất dữ liệu / Truy cập trái phép]
3. Phạm vi dữ liệu bị ảnh hưởng:
   - Loại dữ liệu: [họ tên, email, SĐT, …]
   - Số lượng chủ thể dữ liệu ước tính: [N]
   - Số lượng tổ chức (trường học) bị ảnh hưởng: [M]
4. Nguyên nhân:
   [Sơ bộ — chi tiết kỹ thuật, không đổ lỗi vendor đến khi xác minh]
5. Biện pháp đã thực hiện:
   - [Bước 1, 2, 3 …]
6. Kế hoạch khắc phục:
   - [Cụ thể, có deadline]
7. Đầu mối liên hệ:
   - DPO: [tên, email, SĐT]
   - DR Coordinator: [tên, email, SĐT]

Kính báo cáo,
[Người ký — đại diện pháp lý của Kite Platform]
```

**Quan trọng:** Trước khi gửi, **luôn** review với legal counsel. Solo-dev mode → chuẩn bị template sẵn nhưng việc gửi cần tham vấn luật sư trước.

### 4.5 Tenant-facing — DR resolved

```
Subject: [Kite Platform] Sự cố đã được khắc phục — Báo cáo sơ bộ

Kính gửi quý trường,

Sự cố ngày [DD/MM/YYYY] đã được khắc phục lúc [HH:MM].

Tổng thời gian gián đoạn: [hh:mm]
Dữ liệu mất (nếu có): [Không có | X giờ giao dịch trong khoảng HH:MM-HH:MM, đang khôi phục]
Hành động cần thiết từ quý trường: [Không có | Đăng nhập lại | Kiểm tra dữ liệu khoảng thời gian ABC]

Báo cáo sự cố chi tiết (postmortem) sẽ được gửi trong vòng 7 ngày.

Chúng tôi xin lỗi về bất tiện này và cam kết liên tục cải thiện độ ổn định của hệ thống.

Trân trọng,
Đội Vận hành Kite Platform
```

---

## 5. Scenario Runbooks

Mỗi scenario có cùng cấu trúc: **Triggers → Detection → Decision → Steps → Verification → Comms checkpoints**. Where steps overlap with existing runbooks, this document **references** rather than duplicates.

### S1 — Region failure (AWS ap-southeast-1 down)

**Triggers:**
- AWS health dashboard reports degradation/outage in ap-southeast-1
- All services report 503 / unreachable simultaneously
- RDS endpoint unreachable from any healthy app pod

**Detection:**
- Synthetic monitoring fails across all endpoints
- CloudWatch alarms region-wide
- Status page (statuspage.io / equivalent) shows AWS issue

**Decision tree:**
- **AWS reports <2h ETA** → wait, comms tenants, no failover (failover cost > expected outage)
- **AWS reports >2h ETA OR no ETA** → declare DR S1, begin failover to us-east-1 secondary

**Steps (failover path):**

1. **DR Coordinator declares S1**, posts §4.1 internal comms.
2. **Verify secondary region readiness:**
   - S3 replicated bucket accessible (GAP-118 cross-region replication on)
   - RDS read-replica in us-east-1 healthy (verify `aws rds describe-db-instances --region us-east-1`)
   - Terraform state for us-east-1 stack present (`terraform -chdir=infrastructure/terraform-aws-us-east-1 show`)
3. **Promote read-replica to primary** in us-east-1:
   ```bash
   aws rds promote-read-replica --db-instance-identifier kitehub-prod-replica --region us-east-1
   ```
4. **Update DNS** (Route53) — switch CNAME for `api.kiteplatform.example` from ap-southeast-1 ALB to us-east-1 ALB. TTL pre-set to 60s to support fast cutover.
5. **Spin up app stack** in us-east-1:
   - If pre-warmed: `kubectl scale deployment --all --replicas=N -n kitehub` (target region)
   - If cold: `terraform apply` from us-east-1 stack (~30-45 min)
6. **Verify** per §5 verification block.
7. **Tenant comms** (§4.3) — sent at T+15min after declaration, T+1h, then on resolution.
8. **Failback plan** — when ap-southeast-1 returns: schedule maintenance window, reverse promotion (us-east-1 → primary, ap-southeast-1 → replica), DNS swap back. Failback ≠ urgent — can wait for low-traffic window.

**Verification:**
- All health endpoints `/actuator/health` return UP from us-east-1 pods
- Sample tenant login works end-to-end (synthetic test)
- DB writes succeed, replication to (recovered) ap-southeast-1 catching up

**Comms checkpoints:**
- T+0: §4.1 internal
- T+15min: §4.3 tenant email (Vietnamese + English)
- Every 30min: §4.2 internal
- T+resolution: §4.5 tenant email

**Estimated RTO:** 2–4 hours (assuming pre-warmed us-east-1 stack). Cold stack: 4–8 hours.

---

### S2 — Database crash (RDS instance lost)

**Triggers:**
- RDS instance status = `failed` or `incompatible-restore`
- All app pods report `connection refused` to DB endpoint
- CloudWatch DBLoad metric → 0 abruptly

**Detection:**
- Monitoring alert: DB connection pool exhausted across all services
- `/actuator/health` shows DB component DOWN platform-wide

**Decision tree:**
- **Single AZ issue, multi-AZ replica available** → automatic AWS failover (~60–90s), monitor only
- **No multi-AZ replica OR replica also down** → declare DR S2, follow [`restore-procedure.md`](restore-procedure.md) (GAP-117)

**Steps:**

1. DR Coordinator declares S2.
2. Determine restore source per RTO/RPO target:
   - Within RPO 15min for kitehub-subscription? → **RDS PITR** (point-in-time restore) — see [`restore-procedure.md`](restore-procedure.md) Scenario A
   - Beyond PITR window OR PITR not available? → **pg_dump → fresh DB** — see [`restore-procedure.md`](restore-procedure.md) Scenario B
3. **Stop write traffic:** scale app deployments to 0 OR enable read-only flag (prevents inconsistent state during restore window).
4. **Execute restore** per Scenario A/B. Coordinator stays out of the technical loop, owns comms + decisions only.
5. **Validate restored DB** with `scripts/verify-restore.sh` (GAP-117).
6. **Cut traffic over:** update app config endpoint, scale apps back up, monitor.
7. **Reconcile** — RPO window of lost transactions documented; if any payment/billing transactions affected, manual reconciliation per finance runbook (separate concern, tracked GAP-049 lineage).

**Verification:**
- `verify-restore.sh` passes (schema + row counts + FK integrity)
- Smoke test: tenant login + create attendance record + read back
- Outbox table queue depth recovers (events fire as app reconnects)

**Comms checkpoints:**
- T+0: §4.1 internal
- T+30min: §4.3 tenant email (highlight "data through HH:MM is safe; transactions in window HH:MM–HH:MM may need re-entry")
- Every 30min: §4.2 internal
- T+resolution: §4.5 tenant email
- T+72h max: PDPL §4.4 if PII data lost

**Estimated RTO:**
- PITR path: 1–2 hours
- pg_dump path: 2–4 hours (depends on data volume)

---

### S3 — Ransomware / hostile data tampering

**Triggers:**
- Files renamed/encrypted with attacker-known extensions across MinIO/S3
- Unfamiliar tables / dropped tables in DB schema
- Ransom note delivered (email / DB row / file)
- Auth logs show admin-level commands from untrusted source IPs
- Backup deletion attempts logged in S3 access logs

**Detection:**
- Monitoring alert: mass file mtime change in MinIO/S3
- DB integrity check fails (ArchUnit or schema-diff regression)
- Manual report from team / customer

**Decision tree (CRITICAL — wrong choice can compound damage):**
- **Active intrusion confirmed** → DO NOT immediately restore. First isolate.
- **Restored backup confirmed clean (versioning/replication intact)** → restore path
- **Backup also encrypted** → escalate legal counsel; pay-or-no-pay decision is exec + legal, NOT engineering

**Steps:**

1. **DR Coordinator declares S3.** Pages tech lead + legal counsel + CTO (criteria from §3 — compliance trigger).
2. **Isolate, before restore:**
   - Revoke all admin AWS keys, rotate ALL secrets via Vault / Sealed Secrets
   - Disable all SSO / federated logins
   - Shut down public ingress (Cloudflare WAF block-all, OR scale gateway to 0)
   - Snapshot affected systems FOR FORENSICS (do not delete attacker traces — needed for legal + insurance)
3. **Identify intrusion vector:**
   - Audit logs (CloudTrail, VPC flow logs, app audit logs)
   - Recently merged PRs / dependency changes (supply-chain attack vector)
4. **Verify backup integrity:**
   - S3 versioning preserved → previous versions usable (GAP-118)
   - Cross-region replica clean → restore source
   - If both compromised → STOP, legal decision required
5. **Plan restore window** with legal:
   - Notify regulators per §4.4 (PDPL 72h clock starts AT detection, not at decision)
   - Notify tenants per §4.3 (transparent about scope; legal may require specific wording)
6. **Restore** per [`restore-procedure.md`](restore-procedure.md) — DB + MinIO assets from versions before earliest known compromise timestamp.
7. **Hardening before re-opening ingress:**
   - All credentials rotated
   - WAF rules tightened
   - 2FA enforced platform-wide if not already
   - Re-deploy from known-good Docker image (verify image SHA against pre-incident registry)
8. **Re-open ingress in stages:** internal team → admin tenants → all tenants.
9. **Postmortem with legal sign-off** within 14 days; incident report shared with insurance carrier.

**Verification:**
- DB schema diff against pre-incident known-good = 0 differences
- File integrity scan (sha256 of templates against pre-incident manifest)
- Audit logs show no remaining attacker session
- Sample tenant data integrity verified by tenant (request confirmation)

**Comms checkpoints:**
- T+0: §4.1 internal (legal CC'd)
- T+1h: §4.3 tenant email (transparent, legal-reviewed wording)
- T+24h: progress update to tenants
- T+72h max: §4.4 regulator notice (PDPL deadline)
- T+resolution: §4.5 tenant email + commitment to postmortem
- T+14d: postmortem published to tenants + regulator follow-up

**Estimated RTO:** 1–7 days (depends on forensics depth + legal timeline). Restore itself is 4–8h; legal/forensics drive total.

---

### S4 — Mass tenant provisioning failure

**Triggers:**
- Provisioning success rate <50% over 30-min window
- Multiple tenants stuck in `INITIALIZING` / `GENERATING` lifecycle state >2h
- AI Branding service circuit-breaker open + queue backlog >100

**Detection:**
- Monitoring: provisioning success-rate dashboard
- Support tickets spike from new tenants
- KiteHub admin dashboard shows `kitehub-subscription` provisioning queue depth

**Decision tree:**
- **Underlying service issue (RabbitMQ, AI provider, DB)** → first treat as upstream incident, drain the queue once fixed
- **Mass corruption of provisioning DB** → escalate to S2-DB-CRASH path
- **AI provider down >2h** → degrade to template-only provisioning (per `ai-branding-guidelines.md` §5 quality gate fallback) → S5

**Steps:**

1. DR Coordinator declares S4.
2. **Stop new provisioning:**
   - Set feature flag `provisioning.enabled=false` (or scale subscription consumer to 0)
   - New signups see "temporarily paused" page
3. **Triage stuck tenants:**
   - Query `instance_lifecycle` table for stuck states
   - Categorize: AI failed, DB write failed, queue lost
4. **Recover by category:**
   - **AI failed** → fall back to STATIC + DEFAULT template (per `ai-branding-guidelines.md` §1 ResourceCategory.STATIC); mark for re-generation when AI healthy
   - **DB write failed** → re-run `InstanceLifecycleService.retry(instanceId)` per stuck tenant
   - **Queue lost** (RabbitMQ event missing) → republish from outbox table (per outbox pattern, §3.5.1 of `design-patterns.md`)
5. **Verify each tenant** reaches `DEPLOYED` state OR explicitly `FAILED` with error logged.
6. **Re-enable provisioning** when stuck queue cleared + upstream healthy.
7. **Tenant comms:** affected tenants get individual recovery email (template adapts §4.5).

**Verification:**
- Provisioning success rate back to >95% baseline
- No tenants stuck >24h
- Audit trail in `instance_lifecycle_history` table consistent

**Comms checkpoints:**
- T+0: §4.1 internal
- T+30min: per-tenant email to affected tenants (NOT mass email — only those stuck)
- Resolution: per-tenant email confirming successful provisioning

**Estimated RTO:** 2–6 hours.

---

### S5 — Critical AI provider down >2h

**Triggers:**
- Ollama / OpenAI / Bedrock circuit-breaker open >2h
- AI provider status page shows incident
- Bulkhead saturation, all retries exhausted

**Detection:**
- Resilience4j metrics: circuit-breaker `ai` state = OPEN sustained
- AI job queue depth growing despite consumers running
- Support tickets re: branding stuck

**Decision tree:**
- **<2h** → ride it out, retry queue handles it
- **2–24h** → degrade to TEMPLATE-only mode (§3 below)
- **>24h** → consider provider swap (Ollama → OpenAI fallback if licensed; or pause new provisioning)

**Steps:**

1. DR Coordinator declares S5 (lighter-touch DR — typically no data loss, just degraded service).
2. **Activate template-only fallback:**
   - Set config `ai.enabled=false` OR force ResourceRoutingService to default TEMPLATE category
   - Existing tenants: branding from cache continues to work
   - New provisioning: TEMPLATE path only (per `ai-branding-guidelines.md` §1 — "Template-first")
3. **Communicate degraded mode** internally + to tenants currently mid-provisioning.
4. **Queue AI requests** for retry (do not drop — RabbitMQ persists)
5. **Monitor provider** status; when restored, drain queue at controlled rate to avoid stampede.
6. **Catch-up regeneration:** tenants who provisioned in TEMPLATE-only mode get notification offering AI regeneration (one-click, per their tier quota).

**Verification:**
- Template-only provisioning succeeds for new signups
- Queue drains at expected rate when AI restored
- No tenant stuck in BROKEN state

**Comms checkpoints:**
- T+0: §4.1 internal
- T+30min: status banner in admin dashboard (no mass tenant email needed unless >6h)
- T+6h: §4.3 tenant email if still degraded
- Resolution: internal §4.2 only (tenants experience graceful degradation, not outage)

**Estimated RTO:** N/A for service availability (degraded continues); RTO for full AI feature parity = depends on provider.

---

## 6. Integration into Deploy Go/No-Go

Per `gap-done-discipline.md` §2 — adding a new "Operational Readiness" item:

The [`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md) "Operational Readiness" section should add:

```markdown
- [ ] DR plan reviewed within last 90 days (`disaster-recovery-plan.md` §7 review log)
- [ ] DR Coordinator role assigned + reachable for deploy window
- [ ] If deploy touches critical path (DB schema, infrastructure): RTO/RPO matrix re-validated
```

This document does NOT edit `deploy-go-nogo-checklist.md` directly — that's a follow-up task in next routine update of that file. Cross-link added in §1 instead so reviewer of this doc knows the integration point.

---

## 7. Quarterly DR Exercise

### Schedule (proposed, to be confirmed)

| Quarter | Scenario tested | Status |
|---------|----------------|--------|
| Q3 2026 (Jul-Sep) | S2 — DB crash via PITR (low-risk first exercise) | **Proposed — TBC** |
| Q4 2026 (Oct-Dec) | S5 — AI provider down (degradation test) | **Proposed — TBC** |
| Q1 2027 (Jan-Mar) | S4 — Mass provisioning failure | Future |
| Q2 2027 (Apr-Jun) | S1 — Region failover (most complex; do after team familiar with simpler scenarios) | Future |
| Annual | S3 — Ransomware tabletop (no actual destruction; walk through legal/comms) | Future |

**Solo-dev caveat:** schedule above is **proposed** for when team grows. In current solo-dev mode, exercises are tabletop (walk-through) only; live exercises require backup operator to staff incident response while exerciser works. First Q3 2026 exercise specifically scheduled to validate this plan with real `restore-procedure.md` (GAP-117) commands against staging DB.

### Exercise format

1. Coordinator picks scenario; doesn't tell rest of team specifics
2. Inject failure (delete staging DB, kill region in test env, simulate ransomware on isolated bucket)
3. Team responds per this runbook **without** opening anything other than this doc + linked runbooks
4. Coordinator times each step, notes friction points
5. Postmortem within 7 days; runbook updated

### Pass criteria

- RTO target hit within 1.5× target (e.g. 1h target → 90min OK; >90min triggers process improvement)
- All comms templates worked without on-the-fly editing
- No undocumented decision points encountered

### Review log (this plan)

| Date | Reviewed by | Changes |
|------|------------|---------|
| 2026-04-28 | @nguyenvankiet | Initial creation (closes GAP-119) |

Next review due: **2026-07-28** (90-day rolling).

---

## 8. Pre-Disaster Hygiene (always-on)

For DR to work, these things must be true at all times. Not part of "during disaster" but listed here as the precondition list:

- [ ] Backups running daily (GAP-093 DONE — `DatabaseBackupScheduler`)
- [ ] Backup restore drilled monthly (GAP-117 — `restore-drill.yml` CI workflow)
- [ ] S3 versioning + cross-region replication enabled (GAP-118 — Terraform)
- [ ] MinIO versioning enabled in dev (GAP-118 — docker-compose)
- [ ] All cross-service events go through outbox (per `design-patterns.md` §3.5.1)
- [ ] RabbitMQ queues durable + mirrored
- [ ] RDS multi-AZ enabled in prod (Terraform check)
- [ ] DR plan reviewed quarterly (§7 log)
- [ ] All team members know who DR Coordinator is THIS QUARTER
- [ ] Comms templates (§4) accessible without VPN / without prod systems being up

If any of the above is FALSE, DR effectiveness compromised — file a P0/P1 incident on the gap.

---

## 9. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Improvise during DR ("I think we usually …") | Follow scenario runbook step-by-step |
| Restore before isolating in S3 (ransomware) | Isolate first, forensics, then restore from clean version |
| Skip comms because "it'll be fixed in 10 min" | If declared DR, comms per §4 are mandatory |
| Restore from latest backup without verifying integrity | `verify-restore.sh` before cut-over |
| Failback to original region under load | Failback in maintenance window, not under traffic |
| Combine DR exercise with real deploy | Quarterly exercise on dedicated day, no concurrent changes |
| Treat AI degradation as full DR | S5 is degraded mode, not full DR — don't over-page |
| Skip postmortem because "we recovered" | Every declared DR gets postmortem within 7 days |

---

## 10. Related

- **Gaps:** [GAP-117](../04-quality/gaps/GAP-117-restore-drill-test.md) (restore drill), [GAP-118](../04-quality/gaps/GAP-118-minio-backup-strategy.md) (MinIO backup), [GAP-030](../04-quality/gaps/GAP-030-disaster-recovery-ai-branding.md) (AI-scope DR — extended by this plan), [GAP-119](../04-quality/gaps/GAP-119-platform-dr-runbook.md) (this plan)
- **Audit:** [`ops-readiness-audit-2026-04-19.md`](../04-quality/audits/ops/ops-readiness-audit-2026-04-19.md) §6
- **Sibling runbooks:** [`incident-response-runbook.md`](incident-response-runbook.md), [`rollback-procedure.md`](rollback-procedure.md), [`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md), [`restore-procedure.md`](restore-procedure.md) (GAP-117)
- **Matrix:** [`operations/dr-rto-rpo-matrix.md`](operations/dr-rto-rpo-matrix.md)
- **Rules:** [`docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md), [`output-review-mandate.md`](../../.claude/rules/output-review-mandate.md), [`design-patterns.md`](../../.claude/rules/design-patterns.md) §3.5.1 (outbox for reliable comms during DR)

---

## 11. Log

- **2026-04-28** — Plan created. Closes GAP-119. Solo-dev DR Coordinator role documented with steady-state path for team scaling. Q3 2026 first quarterly exercise proposed (S2 PITR via staging). Cross-references GAP-117 (`restore-procedure.md`) and GAP-118 (S3 versioning) — does not duplicate their procedure detail.
