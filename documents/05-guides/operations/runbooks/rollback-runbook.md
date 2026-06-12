# Rollback Runbook — Production deploy reversal

**Last Updated:** 2026-05-06
**Owner:** Solo-dev coordinator (future on-call)
**Reviewer:** @nguyenvankiet
**Closes:** GAP-378
**Source-of-truth:** [`.claude/rules/release-deploy-standard.md`](../../../../.claude/rules/release-deploy-standard.md) §3.4 MAJOR per-bump-type checklist
**Cross-link:** [`documents/03-planning/roadmap/release-1-deploy-plan.md`](../../../03-planning/roadmap/release-1-deploy-plan.md) §5

---

## What this runbook covers

A complete, executable procedure to revert a production release of KiteHub + KiteClass when one or more rollback triggers (§1) fires within 24h of deploy. The runbook is written to be followed top-to-bottom under incident pressure — every command has a verify step, every decision point names an owner, every artifact (status page, email, smoke test) has a template.

This runbook is paired with the abbreviated rollback summary in `release-1-deploy-plan.md` §5 — that section gives the 8-step "TL;DR"; **this file is the detailed source of truth**. If they conflict, this file wins; release-1-deploy-plan.md should be updated to match.

> ⚠️ **Verify deploy infrastructure version BEFORE running any command.** Phase 1 BETA + 1.5 PAID run on Docker-compose (Oracle Cloud VM); future K8s migration runs on Helm. Both paths are documented — pick the one that matches what was deployed. Mixing them = data loss risk.

---

## 1. When to rollback (triggers)

Initiate rollback IF **any** of the following is observed within 24h of deploy:

| Trigger | Threshold | Source |
|---|---|---|
| Critical bug affecting tenants | >10% of active tenants in first 24h | Tenant complaints + status page reports |
| Database corruption / data loss | Any confirmed instance | Backup-job alert + manual verify |
| Authentication completely broken | Login success rate <50% over 5 min | `JWTAuthFailureSpike` alert + smoke test fail |
| Payment processor failures | >50% of new transactions failed | `SubscriptionWebhookFailure` alert + webhook logs |
| Performance degradation | P95 latency > 2× baseline sustained 15 min | `HighResponseTime` alert + Grafana |
| Multi-tenant data leak | Any single confirmed cross-tenant query | `MultiTenantDataLeak` alert (P0 security) |
| Service unreachable | Any service `up == 0` for >5 min after restart attempt | `ServiceDown` alert |

**One trigger is sufficient.** Do NOT wait for a second confirmation if a P0 trigger fires — rollback fast, investigate later.

---

## 2. Decision authority

| Mode | Decision-maker | Approver |
|---|---|---|
| **Solo-dev (current)** | Coordinator (also executor) | Self — log decision in incident timeline |
| **Future on-call** | On-call engineer | Tech lead (asynchronous OK if P0) |
| **Production cutover (Phase 3 K-12)** | On-call engineer | Tech lead + DPO (if PII involved) |

The decision-maker MUST log the trigger that fired, the timestamp, and the chosen rollback path (Helm vs Docker-compose) in the incident channel + status page incident before executing §4 Step 1.

---

## 3. Pre-rollback checklist (T+0 → T+5 min)

Before touching production, complete these in order:

- [ ] **Confirm the trigger** matches §1 — verify the alert payload + manual reproduction (1 minute max)
- [ ] **Notify status page** — create incident at status.kitehub.vn (template §6.1)
- [ ] **Notify tenants by email** — "Maintenance for ~30 minutes" (template §6.3)
- [ ] **Take a fresh backup snapshot** — DB + MinIO uploads (do not skip; needed for forensics)
  ```bash
  # Postgres logical dump (Docker-compose path)
  docker exec kite-postgres pg_dumpall -U postgres > /opt/kite/backups/pre-rollback-$(date -u +%Y%m%dT%H%M%SZ).sql
  # MinIO snapshot (rclone or mc mirror to S3 cold)
  mc mirror minio/kitehub-assets s3-cold/kitehub-assets-pre-rollback-$(date -u +%Y%m%dT%H%M%SZ)
  ```
- [ ] **Identify previous working version** — `git tag -l 'v*' --sort=-v:refname | head -3` then pick the last GREEN tag (one preceding the broken release)
- [ ] **Identify rollback SQL file** — every Flyway migration shipped in the broken release MUST have a paired `Rxxx__rollback.sql` prepared pre-deploy (per `release-1-deploy-plan.md` §2.1 checklist). If no rollback SQL exists for a schema-breaking change → §5.5 "Data corruption recovery" path applies (slower, requires `pg_restore`)
- [ ] **Page secondary engineer** (if available) — second pair of eyes catches typos in `helm rollback` / `docker-compose pull` revisions

If any pre-flight item fails, **STOP and escalate**. Do not proceed with rollback against an un-snapshotted DB.

---

## 4. Rollback steps (detailed, 7 steps)

### Step 1: Application rollback

Pick **one** path based on which infra was deployed.

**Path A — Helm (Kubernetes, future K8s migration):**

```bash
# Identify previous successful revision
helm history kitehub --namespace kitehub | tail -10
# Pick the revision number (R) of the last DEPLOYED-status entry preceding broken
helm rollback kitehub <R> --namespace kitehub --wait --timeout 10m

# Same for kiteclass-instances if separately released
helm history kiteclass --namespace kiteclass-instances | tail -10
helm rollback kiteclass <R> --namespace kiteclass-instances --wait --timeout 10m
```

Verify:
```bash
kubectl rollout status deploy -n kitehub --timeout=300s
kubectl get pods -n kitehub --field-selector=status.phase!=Running
# Expect: empty output (no broken pods)
```

**Path B — Docker-compose (Oracle Cloud VM, current Phase 1 BETA + 1.5 PAID):**

```bash
# SSH to Oracle VM
ssh ubuntu@<oracle-vm-ip>
cd /opt/kite

# Stop the broken release (Docker-compose names per CLAUDE.md naming convention)
docker-compose -f kitehub/docker-compose.kitehub.yml down
# If kiteclass-frontend deployed separately:
docker-compose -f kitehub/docker-compose.oracle-frontend.yml down

# Switch git to previous tag
git fetch --tags
git checkout v0.9.0-beta  # or whichever tag from §3 step "Identify previous working version"

# Pull pre-built images (faster than rebuild)
docker-compose -f kitehub/docker-compose.kitehub.yml pull
docker-compose -f kitehub/docker-compose.oracle-frontend.yml pull

# Bring up
docker-compose -f kitehub/docker-compose.kitehub.yml up -d
docker-compose -f kitehub/docker-compose.oracle-frontend.yml up -d
```

Verify:
```bash
docker-compose -f kitehub/docker-compose.kitehub.yml ps
# All services should show State=Up (healthy)
docker logs kite-gateway --tail 50 | grep -E 'Started|Tomcat started'
```

### Step 2: Database rollback (only if release shipped Flyway migrations)

Skip this step if the broken release did NOT include any new `V[N]__*.sql` files. Check via:
```bash
git diff <previous-tag>..<broken-tag> -- '**/db/migration/V*.sql' | grep '^+++'
```

If migrations DID ship, identify which versions need reverting:
```bash
docker exec kite-postgres psql -U postgres -d kitehub \
  -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 10;"
```

Apply the paired rollback SQL prepared pre-deploy (per `release-1-deploy-plan.md` §2.1 "Rollback SQL prepared (last DDL revert)"):
```bash
# Per-service: subscription, branding, email, admin, kiteclass-core, kiteclass-instance-template
docker exec -i kite-postgres psql -U postgres -d kitehub < /opt/kite/rollback-sql/V<N>__rollback.sql
docker exec -i kite-postgres psql -U postgres -d kiteclass < /opt/kite/rollback-sql/V<N>_kc__rollback.sql
```

Then mark the migration as reverted in Flyway history:
```sql
DELETE FROM flyway_schema_history WHERE version = '<N>';
```

> ⚠️ If no rollback SQL was prepared (process miss) → jump to §5.5 "Data corruption recovery — pg_restore from snapshot" — slower (5-15 min) but safe.

### Step 3: DNS rollback (only if §4 Step 1 swapped IPs / endpoints)

If the broken release came with a DNS cutover (e.g., new prod IP, new CDN edge), revert via Cloudflare API:

```bash
# Find DNS record ID
curl -s -X GET "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/dns_records?type=A&name=kitehub.vn" \
  -H "Authorization: Bearer $CF_API_TOKEN" | jq '.result[].id'

# Revert
curl -s -X PATCH "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/dns_records/<record-id>" \
  -H "Authorization: Bearer $CF_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"content\": \"<previous-stable-IP>\", \"ttl\": 300}"

# Wait for propagation (Cloudflare TTL 5 min)
for host in kitehub.vn kitehub.me api.kitehub.vn; do
  dig "$host" +short
done
# Re-check until output matches previous-stable-IP
```

If the rollback path keeps the same DNS pointing → SKIP this step (most Phase 1 BETA hotfixes do not involve DNS changes).

### Step 4: Cache invalidation

```bash
# Cloudflare cache purge (entire zone)
curl -s -X POST "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/purge_cache" \
  -H "Authorization: Bearer $CF_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"purge_everything": true}'

# Application-layer: Redis cache for branding + sessions + rate-limit
docker exec kite-redis redis-cli FLUSHDB
# Or selective by key prefix:
docker exec kite-redis redis-cli --scan --pattern "branding:*" | xargs -r docker exec -i kite-redis redis-cli DEL

# Browser side: bump SW version via deploy (already happens with §4 Step 1 image swap)
```

### Step 5: Smoke test the rolled-back version

Cross-link forward to GAP-377 (smoke test automation) — until that ships, use the existing baseline `scripts/smoke-test.sh`:

```bash
./scripts/smoke-test.sh https://api.kitehub.vn
./scripts/smoke-test.sh https://api.kitehub.me
# Exit 0 = all pass; exit 1 = at least one FAIL → rollback FAILED, escalate to §7
# Exit 2 = WARN only, document warnings, decide case-by-case
```

If smoke test exits non-zero, **DO NOT** announce "service restored." Move to §7 Recovery and consider §5.5 data restore.

### Step 6: Communicate restoration to tenants

- [ ] **Status page** — update incident: investigating → identified → monitoring → resolved (template §6.2)
- [ ] **Email tenants** — "Service restored to v<previous>; investigation ongoing" (template §6.4)
- [ ] **Beta tenants only** — additional email if beta SLA was breached (template §6.5)

### Step 7: Post-incident review (within 48h)

Per `output-review-mandate.md` §6 + `release-deploy-standard.md` §4.3:

- [ ] Schedule retro within 48h of incident closure
- [ ] Write incident report at `documents/04-quality/incidents/YYYY-MM-DD-<slug>.md`
- [ ] Root cause analysis (Five Whys minimum)
- [ ] Action items + owners + due dates
- [ ] Update this runbook if new failure mode surfaced
- [ ] If incident reveals a coverage gap in rules/skills → run `incident-to-rule-pipeline.md` 5-stage

---

## 5. Per-component rollback specifics

### 5.1 Frontend (Next.js — kiteclass-frontend, kitehub-frontend)

- Static assets are content-hashed at build time (e.g. `_next/static/<hash>.js`). Old visitors with cached HTML pointing to old hashes are unaffected by rollback.
- Service Worker (PWA): bumping container image version automatically registers a new SW; clients pick up on next page load.
- Hard-flush concerns: only if the broken release touched `pwa.js` cache scope; in that case append a cache-bust query to `/manifest.webmanifest` for 24h.
- Edge cache: covered by §4 Step 4 Cloudflare purge.

### 5.2 Backend (Spring Boot — kitehub-* + kiteclass-core)

- **Multi-instance rolling rollback:** Helm `helm rollback --wait` handles pod-by-pod; Docker-compose `up -d` does parallel restart (acceptable for Phase 1 BETA scale).
- **DB connection pool drain:** HikariCP `maximumPoolSize=20` per service. On shutdown, Spring sends `SIGTERM` → `gracefulShutdown.timeout=10s` (per `release-deploy-standard.md` §3.1 Twelve-Factor Disposability). In-flight HTTP requests get up to 10s to complete.
- **JVM warmup:** rolled-back pods will see a P95 spike for ~60s; expected, not a re-rollback trigger.
- **Migration drift risk:** if the broken release bumped Hibernate validation strictness (e.g. dialect change), the rolled-back JAR may itself fail to validate against schema state. Verify boot logs: `docker logs kitehub-subscription | grep -E 'Hibernate|Schema-validation'`.

### 5.3 AI Branding (kitehub-branding)

- **Cached generated assets:** keep them. Rollback does NOT invalidate `branding-job-cache:*` Redis keys or MinIO `branding/` bucket — these are tenant-owned outputs from successful past generations.
- **In-flight AI jobs (status `PENDING`/`GENERATING`):** the rabbitmq queue `ai.generate.{tier}` will be drained by the rolled-back consumer. Jobs older than 10 min get marked `FAILED` via `BrandingJobReaper` cron; affected tenants receive a "regenerate available" email (template §6.6).
- **Quality gate compatibility:** if the rolled-back version uses a different `InstanceQualityReviewer` schema, manually re-evaluate any `DEPLOYING` instances by triggering `POST /api/v1/branding/{instanceId}/quality-gate/recheck`.

### 5.4 Email transactional (kitehub-email)

- **In-flight emails (queued in `emails.send` exchange):** let them complete — the rolled-back consumer is fully backwards-compatible with v0.9.0-beta payload schema. Verify queue depth: `docker exec kite-rabbitmq rabbitmqctl list_queues name messages | grep emails`.
- **DLQ monitoring:** watch `EmailQueueDLQGrowing` alert for 30 min post-rollback. Spike >10 messages → check `email-queue-dlq-growing.md` runbook.
- **Provider failover:** if rollback was triggered by `SendGridProviderHighFailureRate` and the rolled-back version still uses SendGrid, manually flip provider via `kitehub.email.provider=ses` config + service restart.

### 5.5 Payment processor (kitehub-subscription webhooks)

- **In-flight transactions:** every webhook handler is idempotent (per `kitehub-subscription/src/main/java/.../webhooks/PaymentWebhookHandler.java` deduplication via `idempotency_key`). Re-deliveries from VNPay/Stripe after rollback are safe.
- **Stuck transactions** (status `PROCESSING` >5 min): manual triage via `subscription-webhook-failure.md` runbook + manual refund flow if needed.
- **Audit log:** every status change emits to `payment.audit` exchange (Outbox pattern); rollback does not lose audit trail.

### 5.6 Database — pg_restore path (data corruption recovery)

If §4 Step 2 cannot be completed (no rollback SQL, or schema rollback corrupted data):

```bash
# 1. Stop all app services FIRST (prevent writes during restore)
docker-compose -f kitehub/docker-compose.kitehub.yml stop kitehub-subscription kitehub-branding \
  kitehub-email kitehub-admin kite-gateway kiteclass-core

# 2. Drop + restore (DESTRUCTIVE — verify backup file before running)
docker exec -i kite-postgres psql -U postgres -c "DROP DATABASE kitehub WITH (FORCE);"
docker exec -i kite-postgres psql -U postgres -c "CREATE DATABASE kitehub;"
docker exec -i kite-postgres pg_restore -U postgres -d kitehub --clean --if-exists \
  < /opt/kite/backups/pre-rollback-<timestamp>.sql

# 3. Restart services
docker-compose -f kitehub/docker-compose.kitehub.yml up -d

# 4. Verify Flyway state
docker exec kite-postgres psql -U postgres -d kitehub \
  -c "SELECT MAX(version) FROM flyway_schema_history;"
# Expect: matches the previous-tag's expected migration version
```

Allow 5-15 min for restore on production-sized DB. Smoke test (§4 Step 5) MUST pass before announcing restore.

---

## 6. Communication templates

### 6.1 Status page — incident created (T+0)

```
Status: Investigating
Affected components: api.kitehub.vn, app.kitehub.me
Started: <UTC timestamp>
We are investigating reports of <symptom>. Updates every 15 minutes.
```

### 6.2 Status page — rollback in progress / resolved

```
Status: Identified → Monitoring → Resolved
Update <UTC timestamp>: Rolling back to previous stable version v<X.Y.Z>. ETA 30 minutes.
Update <UTC timestamp>: Rollback complete. All services restored. Continuing to monitor.
Update <UTC timestamp>: Resolved. Post-incident report will be published within 48 hours.
```

### 6.3 Email tenants — maintenance window (T+0)

**Subject (VN):** `[KiteHub] Bảo trì khẩn cấp — dịch vụ tạm gián đoạn ~30 phút`
**Subject (EN):** `[KiteHub] Emergency maintenance — service interruption ~30 minutes`

```
Tiếng Việt:

Kính gửi quý đối tác,

Chúng tôi đang thực hiện bảo trì khẩn cấp để khắc phục sự cố vừa phát sinh. Dịch vụ KiteHub + KiteClass có thể bị gián đoạn trong khoảng 30 phút từ <thời gian bắt đầu>.

Trạng thái cập nhật: https://status.kitehub.vn
Hỗ trợ khẩn cấp: support@kitehub.vn

Trân trọng,
Đội ngũ KiteHub

---

English:

Dear partners,

We are performing emergency maintenance to address a service issue. KiteHub + KiteClass services may be interrupted for approximately 30 minutes starting at <start time>.

Status updates: https://status.kitehub.vn
Emergency support: support@kitehub.vn

Best regards,
KiteHub Team
```

### 6.4 Email tenants — service restored

**Subject (VN):** `[KiteHub] Dịch vụ đã khôi phục — phiên bản v<X.Y.Z>`
**Subject (EN):** `[KiteHub] Service restored — version v<X.Y.Z>`

```
Tiếng Việt:

Kính gửi quý đối tác,

Dịch vụ KiteHub + KiteClass đã được khôi phục về phiên bản ổn định v<X.Y.Z>. Chúng tôi đang điều tra nguyên nhân và sẽ công bố báo cáo trong vòng 48 giờ.

Nếu quý đối tác vẫn gặp vấn đề, vui lòng liên hệ support@kitehub.vn hoặc kiểm tra https://status.kitehub.vn

Xin lỗi vì sự bất tiện.

Đội ngũ KiteHub

---

English:

Dear partners,

KiteHub + KiteClass services have been restored to stable version v<X.Y.Z>. We are investigating the root cause and will publish a post-incident report within 48 hours.

If you continue to experience issues, please contact support@kitehub.vn or check https://status.kitehub.vn

We apologize for the inconvenience.

KiteHub Team
```

### 6.5 Email beta tenants — beta period extension (only if beta SLA breached)

```
Tiếng Việt:

Kính gửi quý đối tác beta,

Do sự cố vừa qua trong giai đoạn beta, chúng tôi gia hạn thời gian beta thêm <N> ngày miễn phí. Quý đối tác không cần thực hiện hành động nào — gia hạn được áp dụng tự động.

Cảm ơn sự kiên nhẫn của quý đối tác trong giai đoạn beta của chúng tôi.

Đội ngũ KiteHub
```

### 6.6 Email AI Branding tenants — regeneration available

```
Tiếng Việt:

Kính gửi quý đối tác,

Một số tác vụ tạo branding bằng AI đã bị ảnh hưởng trong sự cố vừa qua. Quý đối tác có thể tạo lại miễn phí (không tính vào hạn mức tháng) tại Dashboard → Branding → Tạo lại.

Đội ngũ KiteHub
```

---

## 7. Validation post-rollback

Run this checklist within 30 min of §4 Step 6 completion:

- [ ] All public marketing pages load (kitehub.vn, kitehub.me) — HTTP 200
- [ ] Tenant login works (3 sample tenants minimum, mix of beta + paid)
- [ ] Existing data accessible (sample query: tenant dashboard shows historic enrollments)
- [ ] Smoke test passes (§4 Step 5 exit 0)
- [ ] Status page shows GREEN (resolved)
- [ ] Tenant complaints subsiding (support@kitehub.vn inbox monitored 2h)
- [ ] Grafana dashboards back to baseline (P95 latency < 2× pre-rollback baseline)
- [ ] No `ServiceDown` / `HighErrorRate` / `MultiTenantDataLeak` alerts active
- [ ] Beta tenants notified (if applicable per §6.5)
- [ ] Incident timeline written (raw `documents/04-quality/incidents/YYYY-MM-DD-<slug>.md` draft)

If any item fails, **re-open the incident** on the status page and escalate to secondary engineer.

---

## 8. Recovery flow (post-rollback, before re-deploy)

Once §7 validation is GREEN, the original broken release is parked. To eventually ship the intended changes:

1. **Investigate root cause** — read logs, traces, audit trail. Use `quality/systematic-debugging` skill (4-phase debugging).
2. **Fix in branch off rolled-back tag** — `git checkout v<previous>; git checkout -b fix/<slug>`. Do NOT branch off the broken tag.
3. **Test thoroughly on staging** — extra acceptance criteria beyond original PR (specifically reproduce the failure mode that caused rollback).
4. **Re-deploy with incremental validation** — feature-flagged if possible per `release-deploy-standard.md` §3.3 MINOR checklist; canary 10% → 50% → 100% over 2h if tooling permits.
5. **Document learnings in retro** — update this runbook §1 if a new trigger surfaced; update `release-1-deploy-plan.md` §2.1 pre-deploy checklist if a new pre-flight item is needed.
6. **Close the original incident** report (`documents/04-quality/incidents/YYYY-MM-DD-<slug>.md` Status → RESOLVED).

---

## 9. Smoke test integration (forward reference to GAP-377)

The §4 Step 5 smoke verification depends on a robust automated suite. Today the project ships `scripts/smoke-test.sh` (basic gateway + health endpoints). GAP-377 (sister gap, Phase 1 BETA + 1.5 PAID) tracks the expansion to the full 15+ assertion suite required by `release-deploy-standard.md` §3.1 Operational Excellence. When GAP-377 lands, this runbook §4 Step 5 will be amended to call the new entrypoint (`./scripts/smoke-test.sh --suite=full`) — track that update via paired-PR per `rule-change-process.md` §6.5.

Until GAP-377 lands: §4 Step 5 uses the existing baseline `scripts/smoke-test.sh` + manual spot-check of 3 sample tenants per §7 validation list.

---

## 10. Cross-references

- Parent plan: [`documents/03-planning/roadmap/release-1-deploy-plan.md`](../../../03-planning/roadmap/release-1-deploy-plan.md) §5 (high-level rollback summary)
- Sister runbook: [`deployment-procedures.md`](deployment-procedures.md) (legacy 2026-03-10 deploy + rollback procedures, K8s-only)
- Sister runbook: [`service-down.md`](service-down.md) (when rollback trigger is `ServiceDown`)
- Sister runbook: [`flyway-migration-failure.md`](flyway-migration-failure.md) (when rollback trigger is migration failure)
- Sister runbook: [`backup-job-failure.md`](backup-job-failure.md) (pre-rollback snapshot blocked by backup job failure)
- Sister gap: GAP-377 (smoke test automation — referenced in §4 Step 5 + §9)
- Standards reference: [`.claude/rules/release-deploy-standard.md`](../../../../.claude/rules/release-deploy-standard.md) §3.4 MAJOR per-bump-type checklist + §4.3 post-deploy + `release-1-deploy-plan.md` §5 inline summary
- Output review: [`.claude/rules/output-review-mandate.md`](../../../../.claude/rules/output-review-mandate.md) §6 (post-incident review process)
- Incident-to-rule pipeline: [`.claude/rules/incident-to-rule-pipeline.md`](../../../../.claude/rules/incident-to-rule-pipeline.md) (if incident surfaces coverage gap)

---

## 11. Log

- **2026-05-06:** Runbook created — Wave 25 Bucket C, closes GAP-378. ~7-step procedure + per-component specifics (FE/BE/AI Branding/Email/Payment/DB pg_restore) + 6 communication templates (status page + email VN/EN + beta extension + AI regeneration) + validation checklist + recovery flow. Documents both Helm (future K8s) + Docker-compose (current Oracle Cloud Phase 1 BETA + 1.5 PAID) paths. Smoke test integration cross-links sister GAP-377 (forward reference). Standards reference §3.4 MAJOR per-bump-type checklist of `release-deploy-standard.md`.
