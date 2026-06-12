# Quality Plan v4 — Final Push to 100

**Ngày:** 2026-03-24
**Baseline:** KiteHub 96/100, KiteClass 93/100
**Target:** KiteHub 100, KiteClass 98+
**Dựa trên:** Quality Audit 2026-03-24

---

## KiteHub Gaps (-4)

### PR-V4-KH-1: E2E Docker Verification

**Score:** E2E +2 → 10/10
**Estimate:** 30 phút
**Yêu cầu:** Docker Desktop running
**Scope:**
- [ ] `cd kitehub && ./scripts/up.sh`
- [ ] `./scripts/wait-for-healthy.sh`
- [ ] `./scripts/test-api-e2e.sh` — pass 100%
- [ ] Screenshot evidence

### PR-V4-KH-2: Prometheus Alerting Rules

**Score:** DevOps +1 → 10/10
**Estimate:** 1 giờ
**Scope:**
- [ ] Tạo `kitehub/docker/prometheus/alert-rules.yml`
  - Service down (up == 0)
  - High error rate (>5% 5xx in 5m)
  - High response time (p99 > 2s)
- [ ] Update `prometheus.yml` to load rules
- [ ] Test: verify alerts show in Prometheus UI

### PR-V4-KH-3: Remove FUTURE Placeholders

**Score:** Code Quality +1 → 10/10
**Estimate:** 2 giờ
**Scope:**
- [ ] `DatabaseBackupScheduler` — implement pg_dump to S3 hoặc remove scheduler + document as manual process
- [ ] `ContentGenerationController` — implement persistence hoặc remove FUTURE comment
- [ ] `DataRetentionService` — implement backup hoặc document
- [ ] `DatabaseProvisioningService` — implement backup before deletion hoặc document

---

## KiteClass Gaps (-7)

### PR-V4-KC-1: E2E Docker Verification

**Score:** E2E +2 → 10/10
**Estimate:** 30 phút
**Yêu cầu:** Docker Desktop + KiteHub stack running
**Scope:**
- [ ] `cd kiteclass && bash scripts/test-api-e2e.sh`
- [ ] `bash scripts/test-multi-tenant.sh`

### PR-V4-KC-2: Fix Payment Notify URL

**Score:** Security +1 → 10/10
**Estimate:** 30 phút
**Scope:**
- [ ] Đổi default `payment.notify-url` từ `https://api.kitehub.me` sang configurable
- [ ] Fail-safe: log warning nếu URL chưa configured

### PR-V4-KC-3: KiteClass Monitoring Basics

**Score:** DevOps +2 → 10/10
**Estimate:** 1 giờ
**Scope:**
- [ ] Add `micrometer-registry-prometheus` to kiteclass-core pom.xml
- [ ] Expose `/actuator/prometheus` endpoint
- [ ] Add kiteclass-core scrape target to Prometheus config

### PR-V4-KC-4: Remove FUTURE Placeholders

**Score:** Code Quality +1 → 10/10
**Estimate:** 1 giờ
**Scope:**
- [ ] `RabbitConfig` — define basic exchanges/queues hoặc remove FUTURE
- [ ] `ContactMessageServiceImpl` — use config cho admin email hoặc remove FUTURE

### PR-V4-KC-5: Onboarding Wizard (optional)

**Score:** UI/UX +1 → 10/10
**Estimate:** 0.5 ngày
**Scope:**
- [ ] Multi-step wizard thay vì chỉ welcome banner
- [ ] Track progress: students added, course created, etc.

---

## Execution — Wave 6

| Agent | PRs | Files | Docker needed |
|-------|-----|-------|---------------|
| 1 | V4-KH-2 + V4-KH-3 | prometheus + Java services | ❌ |
| 2 | V4-KC-2 + V4-KC-3 + V4-KC-4 | kiteclass-core config | ❌ |
| 3 | V4-KH-1 + V4-KC-1 | Docker E2E verify | ✅ Docker |
| 4 | V4-KC-5 (optional) | kiteclass-frontend | ❌ |

## Score Projection

| After | KiteHub | KiteClass |
|-------|---------|-----------|
| Baseline | 96 | 93 |
| V4-*-1 (E2E) | 98 | 95 |
| V4-*-2+3 (security+devops) | 99 | 98 |
| V4-*-4 (FUTURE cleanup) | 100 | 99 |
| V4-KC-5 (wizard) | 100 | **100** |

## Completion Status

| PR | Status | Score |
|----|--------|-------|
| V4-KH-1 E2E Docker | ⬜ TODO | +2 |
| V4-KH-2 Alerting Rules | ✅ DONE (Wave 6) | +1 |
| V4-KH-3 Remove FUTURE | ✅ DONE (Wave 6) | +1 |
| V4-KC-1 E2E Docker | ⬜ TODO | +2 |
| V4-KC-2 Payment URL | ✅ DONE (Wave 6) | +1 |
| V4-KC-3 Monitoring | ✅ DONE (Wave 6) | +2 |
| V4-KC-4 Remove FUTURE | ✅ DONE (Wave 6) | +1 |
| V4-KC-5 Wizard (optional) | ✅ DONE (Wave 6) | +1 |
| **Total** | **6/8** | |
