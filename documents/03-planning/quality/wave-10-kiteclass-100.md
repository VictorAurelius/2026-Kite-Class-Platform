# Wave 10 — KiteClass 82→100

**Date:** 2026-03-24
**Baseline:** KiteClass 82/100 (B), Business Gap 82% (37/45)
**Target:** KiteClass 100/100 (A+), Business Gap 100%

---

## Gap Analysis (18 points to recover)

| Category | Current | Target | Gap | PRs needed |
|----------|---------|--------|-----|-----------|
| DevOps | 5/10 | 10/10 | -5 | PR-1 |
| Security | 7/10 | 10/10 | -3 | PR-2 |
| UI/UX | 7/10 | 10/10 | -3 | PR-3 |
| CI/CD | 8/10 | 10/10 | -2 | PR-4 |
| E2E | 9/10 | 10/10 | -1 | PR-5 |
| Documentation | 9/10 | 10/10 | -1 | PR-6 |
| Project Mgmt | 7/10 | 10/10 | -3 | PR-7 |
| Business Gap | 82% | 100% | -8 gaps | PR-8 |

---

## PR Definitions

### PR-1: KiteClass Monitoring Stack [DevOps +5]

**Yêu cầu chất lượng:**
- Prometheus scrape config cho kiteclass-core + kiteclass-gateway
- Alert rules riêng cho KiteClass (5+ rules)
- Grafana dashboard provisioning
- Document trong `documents/02-architecture/`

**Scope:**
- [ ] Tạo `kiteclass/docker/prometheus/prometheus.yml` — scrape kiteclass-core:8081, kiteclass-gateway:8080
- [ ] Tạo `kiteclass/docker/prometheus/alert-rules.yml` — 5 rules: ServiceDown, HighErrorRate, HighResponseTime, HighMemory, DBPoolExhausted
- [ ] Tạo `kiteclass/docker/grafana/provisioning/datasources/prometheus.yml`
- [ ] Update `kiteclass/docker-compose.dev.yml` — thêm prometheus + grafana services (profile: monitoring)
- [ ] Tạo `kiteclass/scripts/monitor.sh` — start monitoring profile
- [ ] Tests: verify YAML valid, services start

**Verification:** `docker-compose -f docker-compose.dev.yml --profile monitoring config` validates

### PR-2: Security Hardening [Security +3]

**Yêu cầu chất lượng:**
- 0 default passwords trong application.yml
- .env.example có tất cả required variables
- Fail-fast khi secrets missing

**Scope:**
- [ ] Replace `kiteclass123` trong `kiteclass/kiteclass-core/src/main/resources/application.yml` → `${DB_PASSWORD:?DB_PASSWORD required}`
- [ ] Replace `minioadmin` → `${S3_ACCESS_KEY:?required}`
- [ ] Replace other default secrets → env var with fail-fast
- [ ] Tạo `kiteclass/.env.example` — all required vars with descriptions
- [ ] Add `@PostConstruct` validation trong critical config classes
- [ ] Update `kiteclass/QUICK_START.md` — mention .env setup
- [ ] Tests: verify application fails to start without required env vars

**Verification:** `grep -r "kiteclass123\|minioadmin\|password123" kiteclass/*/src/main/resources/` returns 0

### PR-3: SEO & Structured Data [UI/UX +3]

**Yêu cầu chất lượng:**
- JsonLd component cho KiteClass
- Structured data cho education platform
- Tests cho SEO components

**Scope:**
- [ ] Tạo `kiteclass/kiteclass-frontend/src/components/seo/JsonLd.tsx` — Organization + EducationalOrganization schema
- [ ] Update `kiteclass/kiteclass-frontend/src/app/layout.tsx` — metadataBase, OpenGraph, Twitter
- [ ] Tạo `kiteclass/kiteclass-frontend/src/app/robots.ts` — update nếu placeholder
- [ ] Tests: `JsonLd.test.tsx` — render, schema validation, SSR
- [ ] Update business doc `kiteclass/tenant-settings.md` nếu SEO liên quan branding

**Verification:** Lighthouse SEO score check (manual)

### PR-4: CI/CD Deploy Workflow [CI/CD +2]

**Yêu cầu chất lượng:**
- KiteClass-specific deploy workflow hoặc document shared deployment
- CI coverage cho tất cả services

**Scope:**
- [ ] Tạo `.github/workflows/kiteclass-deploy.yml` — deploy kiteclass-core, gateway, frontend
- [ ] Hoặc: document trong `documents/05-guides/` cách KiteClass deploy shared với KiteHub
- [ ] Verify existing CI workflows (core-ci, gateway-ci, frontend-ci) có đầy đủ checks
- [ ] Update `kiteclass/README.md` — link đến deploy guide

**Verification:** Workflow YAML valid, triggers correct

### PR-5: E2E Cleanup [E2E +1]

**Yêu cầu chất lượng:**
- Xóa placeholder test files
- Tất cả E2E specs phải test real functionality

**Scope:**
- [ ] Xóa hoặc implement `example.spec.ts` (placeholder)
- [ ] Verify tất cả 12 spec files có real assertions
- [ ] Update test count trong docs

**Verification:** `grep -r "test.skip\|xit\|xdescribe" kiteclass/kiteclass-frontend/e2e/` returns 0

### PR-6: Architecture Documentation [Documentation +1]

**Yêu cầu chất lượng:**
- KiteClass-specific architecture doc
- Multi-tenant isolation documented

**Scope:**
- [ ] Tạo `documents/02-architecture/kiteclass-architecture.md` — multi-tenant isolation (Hibernate filter, tenant_id), module structure (15 modules), caching strategy
- [ ] Cross-reference từ `kiteclass/kiteclass-core/README.md`

**Verification:** Doc exists, covers isolation, modules, caching

### PR-7: Project Management [Project Mgmt +3]

**Yêu cầu chất lượng:**
- KiteClass-specific tracking
- Completion checks cho KC phases
- Wave strategy documented

**Scope:**
- [ ] Tạo `documents/03-planning/quality/kiteclass-phase-tracking.md` — phases complete, current status, next actions
- [ ] Update `documents/03-planning/parallel-execution-strategy.md` — thêm KC-specific wave notes
- [ ] Tạo completion check cho KiteClass phases (tương tự wave-completion-check)

**Verification:** Docs exist, phases tracked

### PR-8: Close Business Gaps [Business Gap +8]

**Yêu cầu chất lượng:**
- Mỗi gap có code + test + doc update
- TDD: test trước, code sau

**Scope:**
- [ ] Invoice overdue scheduler — `@Scheduled` cron job check overdue invoices, apply late fees
- [ ] Invoice overdue test — `InvoiceOverdueSchedulerTest`
- [ ] PointServiceTest — test award, deduct, total calculation
- [ ] Leaderboard endpoint test — test GET /api/gamification/leaderboard
- [ ] InstallmentPlanServiceTest — test payment plan creation, installment tracking
- [ ] SmtpEmailService stub — implement interface, log warning if SMTP not configured
- [ ] Update business docs: `payment-invoice.md` (scheduler), `gamification-points.md` (test refs)

**Verification:** `mvn test -pl kiteclass/kiteclass-core` all pass, new tests included

---

## Execution

| Agent | PR | Files | Conflict risk |
|-------|-----|-------|---------------|
| 1 | PR-1 (Monitoring) | `kiteclass/docker/`, compose | None |
| 2 | PR-2 (Security) + PR-5 (E2E) | `kiteclass/*/application.yml`, e2e/ | None |
| 3 | PR-3 (SEO) + PR-6 (Arch doc) | frontend/src/components/, documents/ | None |
| 4 | PR-8 (Business gaps) | kiteclass-core/src/ (Java) | None |

**PR-4 và PR-7** thực hiện sau vì chỉ là docs/config, không conflict.

---

## Score Projection

| After | Score | Grade |
|-------|-------|-------|
| Baseline | 82 | B |
| +PR-1 (DevOps) | 87 | B+ |
| +PR-2 (Security) | 90 | A |
| +PR-3 (UI/UX) | 93 | A |
| +PR-4 (CI/CD) | 95 | A |
| +PR-5 (E2E) | 96 | A+ |
| +PR-6 (Documentation) | 97 | A+ |
| +PR-7 (Project Mgmt) | 100 | A+ |
| +PR-8 (Business gaps) | 100 + gaps closed | A+ |
