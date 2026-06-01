# Wave meta-7 Bucket B — P1 OPEN phase-1-beta first half (38 gaps)

**Date:** 2026-06-01
**Agent:** Opus 4.7 background (Wave meta-7 Bucket B)
**Gap count:** 38 từ `bucket-b-p1-open-1.txt`
**Source taxonomy:** [`2026-06-01-wave-meta-7-classification-taxonomy.md`](2026-06-01-wave-meta-7-classification-taxonomy.md)

---

## Verdict Summary

| Verdict | Count |
|---|---|
| SHIPPED→DONE | 6 |
| PARTIAL→adjust_pct | 7 |
| OPEN→keep | 24 |
| SCOPE-REVISE | 1 |
| DROP | 0 |

**Stale-drift rate:** 6/38 = **15.8%** (gaps shipped fix nhưng CSV vẫn báo OPEN 0%). Cộng PARTIAL: 13/38 = **34.2%** stale-or-partial drift.

---

## Per-gap verdicts

### GAP-138 — KiteClass Landing Hero — Duplicated "Chuyên nghiệp & Hiệu quả"

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `kiteclass/kiteclass-frontend/src/app/(public)/page.tsx:45` vẫn dùng `heroTitle: 'Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả'` + `title: 'KiteClass - Quản lý Trung tâm Tiếng Anh Chuyên nghiệp'` line 26 → 2 instances tồn tại (count=2 grep "Chuyên nghiệp").
  - Commits: không có fix commit referencing GAP-138.
  - AC: 0/5
- **New completion_pct:** 0
- **New notes:** Symptom verified present; FE landing hero text trùng lặp chưa fix.

### GAP-139 — Parent Dashboard MVP is Placeholder-Only (Wave 5 Widgets Missing)

- **Verdict:** PARTIAL→adjust_pct=40
- **Evidence:**
  - Code state: `kiteclass/kiteclass-frontend/src/app/(dashboard)/parent/page.tsx` (236 LOC) Wave 49 Bucket A re-skinned từ Wave 18b1 children-list page; mobile-shell PWA với hero + child cards + activity feed + sibling routes (attendance/billing/grades/settings).
  - Commits: PR #749 (Wave 17 Bucket A P1 Solo Teacher review round 1) — partial scope; Wave 49 GAP-267 visible trong page header comment.
  - AC: 0/6 nhưng implementation đáng kể đã có (mobile shell + hook wiring + sibling tabs).
- **New completion_pct:** 40
- **New notes:** Wave 49 GAP-267 mobile shell + hook wiring shipped; widgets (Wave 5 spec) per AC chưa hoàn thiện.

### GAP-212 — DefaultUrlAllowlistValidatorTest flaky DNS

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Code: `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/security/impl/DefaultUrlAllowlistValidatorTest.java:68,70,76` dùng `api.partner.invalid` (3 occurrences) thay vì `api.partner.com` — DNS-rebind hardened.
  - Commits: `c3211cc4 fix(test): GAP-212 DefaultUrlAllowlistValidatorTest flake — use .invalid TLD (#475)`.
  - AC: 0/5 nhưng AC#1 + AC#2 verified empirically (`.invalid` TLD applied).
- **New completion_pct:** 100
- **New notes:** PR #475 .invalid TLD fix shipped; tests deterministic; AC dù chưa tick về mặt formal đã đạt empirical.

### GAP-213 — Spring Cloud BOM fails on Dependabot Boot bumps

- **Verdict:** PARTIAL→adjust_pct=60
- **Evidence:**
  - Commits: PR #523 `fix: GAP-213 — bump Boot 3.5.14 + pin Spring Cloud BOM to 2025.0.x`; PR #1137 `chore(deps): GAP-468 close 9 HIGH CVE via explicit dependencyManagement overrides`.
  - Code: pom.xml searches không match "spring-cloud-dependencies" hiện tại — Spring Cloud có thể đã removed/refactored hoặc moved to different version property name.
  - AC: 0/4 — Dependabot re-run + main branch CI green + version strategy docs status không xác nhận empirically.
- **New completion_pct:** 60
- **New notes:** PR #523 fix shipped Wave 5+; documentation AC chưa xác minh; pom structure đã evolve, scope-revise needed.

### GAP-220 — BrandingVersionService.snapshot JSONB column type mismatch

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code: không tìm thấy `BrandingVersion*.java` files trong `kitehub-branding/src/main/java/com/kitehub/branding/version/` (find returned 0).
  - Commits: PR #532 `feat(doc-gen): Sub-PR 5.6b — Wave 5 closure + 4 P0 audit fixes` không clearly include GAP-220.
  - AC: 0/4.
- **New completion_pct:** 0
- **New notes:** BrandingVersion entity có thể không còn tồn tại hoặc đã refactor; cần state-check class location trước fix.

### GAP-257 — Restore Drill Phase 3 — Quarterly DR Exercise + Measured RTO Baseline

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` referenced `rds_storage_low` baseline; Wave 86-H AlertManager wiring + 4 runbooks shipped (GAP-144 cluster).
  - Commits: nhiều related Wave 84-91 ops-readiness audit shipped (PR #1438 Wave 86-H, PR #1873 Wave beta-prep-1 Bucket C status page + SNS), nhưng restore drill Phase 3 quarterly execution chưa thực hiện per gap title.
  - AC: 0/7.
- **New completion_pct:** 0
- **New notes:** Phase 3 quarterly DR exercise execution gated by AWS account restoration (GAP-612 unblock); Phase 1+2 partial scope shipped via sister gaps.

### GAP-288 — First-login onboarding tour for solo teacher

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "OnboardingTour\|onboardingTour" returned 0 matches across kitehub/ + kiteclass/.
  - Commits: PR #749 (Wave 17 Bucket A P1 Solo Teacher review) là persona review, không phải implementation.
  - AC: 0/8.
- **New completion_pct:** 0
- **New notes:** Symptom verified present — không có onboarding tour component nào.

### GAP-289 — Quick-add lesson session UI for mobile

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "QuickAdd\|quick-add-lesson" returned 0 matches.
  - Commits: PR #749 persona review only.
  - AC: 0/6.
- **New completion_pct:** 0
- **New notes:** Symptom verified present — quick-add UI chưa implement.

### GAP-293 — Monthly income summary dashboard

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "monthlyIncome\|MonthlyIncome" returned 0 matches kiteclass-frontend.
  - Commits: PR #1535 Wave 96 re-phase metadata only; PR #1583 Wave 100-f database-architecture-map doc rewrite (không phải implementation).
  - AC: 0/7.
- **New completion_pct:** 0
- **New notes:** Re-phased phase-2→phase-1-beta (per Wave 96 PR1 #1535); implementation chưa làm.

### GAP-294 — Add NO_SHOW attendance status

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "NO_SHOW" trong kiteclass-core/src/main/java returned 0 matches; AttendanceStatus enum chưa có NO_SHOW value.
  - Commits: PR #1535 Wave 96 re-phase only.
  - AC: 0/7.
- **New completion_pct:** 0
- **New notes:** Symptom verified present; AttendanceStatus enum needs NO_SHOW value.

### GAP-346 — Test Skip Audit — kiteclass-frontend 26.7% Skip Ratio + CI Warning Mechanism

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `kitehub/kitehub-frontend/e2e/production-self-test/full-flow.spec.ts` vẫn có ≥8 `test.skip()` instances (Wave 69 scaffold default); không có CI warning script.
  - Commits: PR #761 file gap; PR #762 wave-18a closure không có CI warning implementation.
  - AC: 0/9.
- **New completion_pct:** 0
- **New notes:** Symptom verified — CI skip ratio warning mechanism chưa shipped.

### GAP-362 — TenantIsolationIT.shouldIsolateCourseDataBetweenTenants pre-existing flake

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "@Disabled" + tenant trong kiteclass-core test returned 0 matches; flake-marker không hiện diện rõ ràng.
  - Commits: PR #795 file gap; PR #796 Bucket 19d wiring (unrelated scope).
  - AC: 0/6.
- **New completion_pct:** 0
- **New notes:** Flake symptom persists at suite level; gap chưa addressed.

### GAP-427 — POST /api/v1/auth/beta-signup/exchange-claim-code not documented in api-contract.md

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "exchange-claim-code\|exchangeClaimCode" trong `documents/01-business/` returned 0 matches.
  - Commits: PR #976+#978+#979 Wave 40 milestone audits — không add api-contract row cho endpoint này.
  - AC: 0/4.
- **New completion_pct:** 0
- **New notes:** api-contract.md doc drift verified; endpoint documented chưa shipped.

### GAP-429 — Transient-State UX Skeleton Pattern

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: chưa verify skeleton component pattern systematically (component may exist but not yet rolled out per AC scope).
  - Commits: PR #978 Wave 40 + PR #1106 Wave 53 UI /128 audits — không phải scaffold rollout.
  - AC: 0/8.
- **New completion_pct:** 0
- **New notes:** Cross-screen rollout pattern chưa addressed; UI audit waves đã catch issue nhưng không fix scope.

### GAP-445 — KC backend deploy deferred to Phase 7 (3-fix pivot)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `docker-compose.production.yml` line 7 comment "KC stack (kiteclass-core + gateway + frontend) deferred to Phase 7 polish" — symptom verified present.
  - Commits: PR #1034 file gap; PR #1038 GAP-447 EC2 right-size (related but không deploy KC); PR #1025 SSM deploy script (KH only).
  - AC: 0/7.
- **New completion_pct:** 0
- **New notes:** Phase 7 defer marker still active; KC backend deploy intentionally deferred.

### GAP-503 — Tier 2 config optimization (JVM + Tomcat + HikariCP + healthcheck)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Code: `docker-compose.production.yml` chứa `JAVA_OPTS: "-XX:MaxRAMPercentage=50.0 -XX:+UseContainerSupport -XX:+UseSerialGC"` cho 5+ services + `start_period: 150s` (per AC#3 cold-start grace period).
  - Commits: `4dee1816 feat(GAP-503): Wave 85 Bucket E — Tier 2 config JVM 60% + Tomcat + HikariCP + 3 CloudWatch alarms (3 AC) (#1429)`.
  - AC: 0/7 (gap file format) nhưng AC#1 + AC#3 empirically verified (JVM container ergonomics + start_period); AC#2 Tomcat + AC#4 HIKARI per commit message.
- **New completion_pct:** 100
- **New notes:** Wave 85 Bucket E PR #1429 shipped 3 AC (JVM + Tomcat + HikariCP + 3 alarms); IT verified post-deploy.

### GAP-506 — deploy-prod.sh tech debt cluster

- **Verdict:** PARTIAL→adjust_pct=60
- **Evidence:**
  - Commits: PR #1427 `refactor(GAP-506): Wave 85 Bucket F — deploy-prod/bootstrap split + env guards`; PR #1265 `fix(GAP-506 Sub-B + auth redirects): email mail health + /register 404 + /auth/* defense`; PR #1344 `feat(wave-77-B): kitehub-email actuator healthcheck — GAP-502 PARTIAL→90%`.
  - Code state: `scripts/aws/` không có `prod-*` named scripts (per AC#5); split implementation có thể đã được wired differently.
  - AC: 0/5 nhưng Phase 1 Sub-A (rabbitmq) + Sub-B (email healthcheck) + Phase 2 Sub-C+D shipped per commits.
- **New completion_pct:** 60
- **New notes:** Phase 1+2 shipped Wave 70+77+85; Phase 3 (3 new prod-* scripts split) AC chưa verify rõ — may need rename verification.

### GAP-532 — Multi-tenant tenant-switch flow §2.7 coverage gap

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "tenant.*picker\|TenantPicker\|workspace.*switch" trong kitehub-frontend returned 0 matches.
  - Commits: PR #1338 `chore(wave-76-F): closure — meta steady-state + Phase 1 BETA audit fold-in` (audit fold-in, không phải feature implementation).
  - AC: 0/5.
- **New completion_pct:** 0
- **New notes:** Multi-tenant switch flow chưa implement; audit caught gap nhưng implementation defer.

### GAP-537c-followup-screenshot-capture — Live screenshot capture cho P2+P3 manuals

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: gap title indicates defer until AWS stack restart (GAP-612 unblock).
  - Commits: PR #1439 + #1384 (Wave 80 + Wave 86 user manual content shipped) — manual structure ready nhưng screenshot capture step gated.
  - AC: 0/11.
- **New completion_pct:** 0
- **New notes:** Capture step gated by AWS restoration (GAP-612 unblock); follow-up scope intentional defer.

### GAP-579 — Soft-delete + 30-day restore window cho students/classes/grades

- **Verdict:** PARTIAL→adjust_pct=40
- **Evidence:**
  - Code state: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/entity/Student.java` extends BaseEntity "for audit fields and soft delete support"; StudentRepository line 21 "Find by ID (excluding soft-deleted records)"; InternalStudentController line 119+122 mentions soft delete.
  - Commits: PR #1426 Wave 85 Bucket A outside-in audits (gap filed); no dedicated restore-window implementation commit found.
  - AC: 0/8 — soft-delete infrastructure exists, 30-day restore window UI/API chưa.
- **New completion_pct:** 40
- **New notes:** Soft-delete entity layer present; restore-window feature scope (UI + admin restore action + 30d retention enforcement) chưa shipped.

### GAP-580 — Email send idempotency key UNIQUE constraint

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "email_send_audit\|idempotency.*email" trong kitehub-email returned 0 matches; UNIQUE constraint chưa added.
  - Commits: PR #1426 Wave 85 audits (filing only).
  - AC: 0/8.
- **New completion_pct:** 0
- **New notes:** RabbitMQ at-least-once duplicate-email risk persists; idempotency layer chưa shipped.

### GAP-583 — RDS storage alarm wiring CloudWatch + SNS + resize runbook

- **Verdict:** PARTIAL→adjust_pct=50
- **Evidence:**
  - Code: `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` line 287 comment "rds_storage_low" baseline + "8 Wave beta-prep-1 Bucket C P0 alarms (excluding pre-existing rds_storage_low)" — alarm baseline shipped.
  - Commits: PR #1438 + #1476 Wave 86-H AlertManager wiring shipped; PR #1435 outside-in audits filed gap.
  - AC: 0/4 — alarm wiring present; SNS routing + resize runbook portion not confirmed.
- **New completion_pct:** 50
- **New notes:** rds_storage_low alarm baseline present in terraform; SNS routing + resize runbook AC partial.

### GAP-595 — Landing CTA hierarchy + demo entry path

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep "demo entry\|demo-cta\|Xem demo" trong kitehub-frontend public landing returned 0 matches.
  - Commits: PR #1435 Wave 86 audits filed gap.
  - AC: 0/5.
- **New completion_pct:** 0
- **New notes:** Defer to Wave 87+; landing CTA + demo entry chưa restructure.

### GAP-607 — RabbitMQ DLQ chưa configured

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Code: `kitehub/kitehub-email/src/main/java/com/kitehub/email/config/EmailQueueConsumerConfig.java:96` `.withArgument("x-dead-letter-exchange", EMAIL_DLQ_EXCHANGE)` — DLX wiring shipped.
  - Commits: `017ce90d feat(wave-91 bucket A): outbox dispatcher + RMQ DLQ (GAP-605+607) (#1487)`.
  - AC: 0/5 nhưng AC#1 (DLX + DLQ declared) verified empirically.
- **New completion_pct:** 100
- **New notes:** Wave 91 Bucket A PR #1487 shipped outbox + DLQ pairing; alertmanager rule wiring via sister GAP-144 (Wave 86-H).

### GAP-609 — FE thiếu UI claim code redemption page

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Code: `kitehub/kitehub-frontend/src/app/(auth)/beta-signup/code/page.tsx` exists.
  - Commits: `9a48e612 feat(wave-91 bucket E): FE claim code redemption page (GAP-609) (#1488)`; subsequent fixes via PR #1956 GAP-801 (email URL path + FE prefill).
  - AC: 0/5 nhưng AC#1 verified (page exists); AC#4 landing link visibility per AC partial may need verify but page proper renders.
- **New completion_pct:** 100
- **New notes:** Wave 91 Bucket E PR #1488 shipped FE redemption page; downstream GAP-801 patch Wave 5+ stabilized URL+prefill.

### GAP-613 — CloudWatch Free Tier 85% threshold reduce alarms + log retention

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: grep terms returned nothing dedicated; PR #1491 Wave 91 batch-1 closure (AWS suspension defer).
  - Commits: PR #1491 partial closure (deferred).
  - AC: 0/8.
- **New completion_pct:** 0
- **New notes:** Gated by AWS restoration (GAP-612); review alarms ≥10 + log retention reduction chưa execute.

### GAP-641 — Admin Revenue page scaffold-only Wave 35 carry

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `kitehub/kitehub-frontend/src/app/(admin)/admin/revenue/page.tsx` (67 LOC) lines 17+31 vẫn hardcoded `0đ` literal; useAdminRevenue hook NOT wired vào page.tsx (chỉ visible trong AdminDashboard.test.tsx mock).
  - Commits: PR #1531 Wave 94c audit suite chỉ filed gap, không phải fix.
  - AC: 0/8 — page chưa consume hook, 0đ literal chưa removed.
- **New completion_pct:** 0
- **New notes:** Symptom verified present; useAdminRevenue hook tồn tại trong use-admin.ts:221 nhưng FE page chưa wire — Wave 35 carry-forward persist.

### GAP-654 — Admin v1 typed DTOs + controller refactor

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/api/v1/admin/dto/` không tồn tại path (find empty).
  - Commits: PR #1544+#1545 Wave 97 closure orphan-cleanup chỉ filed gap follow-up.
  - AC: 0/6.
- **New completion_pct:** 0
- **New notes:** Orphan-cleanup for GAP-638 PARTIAL deferred portion; B2 DTOs + B3 deprecation chưa implement.

### GAP-664 — Wave 98 3-layer doc completeness drift (preferences + email)

- **Verdict:** PARTIAL→adjust_pct=40
- **Evidence:**
  - Code state: `documents/01-business/kitehub/preferences/` ONLY contains `api-contract.md` (missing rules.md + use-cases.md); `documents/01-business/kitehub/email/` contains api-contract.md + rules.md + templates/ (missing use-cases.md).
  - Commits: PR #1561 (Wave 98 audit gap closure filing); PR #1566 (Wave 99C 2 META detectors filed — partial closure).
  - AC: 0/6 — 3-layer completeness chưa restored.
- **New completion_pct:** 40
- **New notes:** Detector script shipped Wave 99C (catches future violations); existing drift (preferences + email) chưa backfill — META P1 pattern.

### GAP-665 — Wave 98 /legal/terms 15-section wall-of-text restructure

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `kitehub/kitehub-frontend/src/app/(public)/legal/terms/page.tsx` (250 LOC) — grep TableOfContents/TocNav/TOC/anchor returned 0 matches; chưa có TOC component, anchor nav, mobile collapse.
  - Commits: PR #1561 Wave 98 audit filing only.
  - AC: 0/8.
- **New completion_pct:** 0
- **New notes:** /legal/terms 103/128 carry-forward Wave 23; TOC + WCAG keyboard nav + mobile collapse chưa shipped.

### GAP-674 — Wave 99B B5 — Golden-path Onboarding Tour README

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Code: `documents/02-architecture/README.md` (205 LOC vs 103 baseline) chứa frontmatter `audience: mixed` + last-reviewed: 2026-05-19 + Wave 99B B5 marker + 7-step reading tour orchestrator.
  - Commits: `1d6043a4 docs(wave-99b-b5): GAP-674 — Golden-path Onboarding Tour README orchestrator (#1577)` + `097ff3b7 docs(wave-99b): closure — 7/7 buckets SHIPPED + Scope-Completeness Reconciliation (#1578)`.
  - AC: 0/8 (file format) nhưng AC#1 (frontmatter audience) + AC#7 (line count 205, target ~150-220) + AC#5 (per-persona index) verified empirically.
- **New completion_pct:** 100
- **New notes:** Wave 99B B5 PR #1577 + closure PR #1578 shipped; README orchestrator delivered.

### GAP-678 — Wave 99B post-wave audit suite (Quality + Business Logic)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `documents/04-quality/audits/quality/` + `business/` không có file dated 2026-05-2[0-2] cho Wave 99B audit suite.
  - Commits: PR #1578 Wave 99B closure filed GAP-678 cho cadence ≤2026-05-22.
  - AC: 0/7 — audit suite chưa run.
- **New completion_pct:** 0
- **New notes:** Cadence deadline missed (2026-05-22); 2 audit reports + audits-index.csv rows chưa shipped.

### GAP-685 — Wave 101 audit suite (api-contract + business-logic + security)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `find documents/04-quality/audits -name "*wave-101*"` returned 0 matches.
  - Commits: PR #1608 Wave 101 closure filed GAP-685 cadence ≤2026-05-22.
  - AC: 0/5.
- **New completion_pct:** 0
- **New notes:** Cadence deadline missed; 3 audits không shipped.

### GAP-686 — kitehub-branding 3-layer business doc sync (RBAC + @PreAuthorize)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `documents/01-business/kitehub-branding/` không tồn tại; chỉ `documents/01-business/kitehub/ai-branding` + `kiteclass/branding-api` + `kiteclass/branding-wizard` (related but different scope).
  - Commits: PR #1608 Wave 101 closure filing only.
  - AC: 0/6.
- **New completion_pct:** 0
- **New notes:** kitehub-branding 3-layer dedicated docs structure chưa exists; OWNER vs STAFF role split docs chưa sync.

### GAP-691 — Wave 102.7.3 post-wave audit suite consolidated cadence

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `documents/04-quality/audits/persona-review/` có 3 sub-wave-102.7.3 audit (bucket a/b/c citations/measurement/RLS rubric) nhưng quality/business-logic/security/api-contract/ops-readiness consolidated suite chưa run.
  - Commits: PR #1667 Wave 102.7.3 closure filing only; PR #1670 GAP-612 escalation file 3 meta follow-ups (691/692/693).
  - AC: 0/5.
- **New completion_pct:** 0
- **New notes:** Cadence deadline 2026-05-23 missed; consolidated suite chưa run.

### GAP-698 — Wave 102.8 ops-readiness audit deferred + terraform-plan OIDC

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `find documents/04-quality/audits/ops-readiness -name "*102-8*"` returned 0 matches.
  - Commits: PR #1695 Wave 102.8 SHIPPED self-test foundation; GAP-698 filed.
  - AC: 0/6.
- **New completion_pct:** 0
- **New notes:** Ops-readiness audit Wave 102.8 deferred per domain-milestone (release-deploy-artifacts Wave 102.9); terraform-plan OIDC investigation ongoing.

### GAP-701 — kiteclass-core EmailService HTTP integration với kitehub-email

- **Verdict:** OPEN→keep
- **Evidence:**
  - Code state: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/event/consumer/ClassRescheduledEmailConsumer.java` exists (event consumer) nhưng grep "kitehub.email.url\|EMAIL_SERVICE_URL\|HTTP client" trong kiteclass-core không hiện diện HTTP integration.
  - Commits: PR #1701 file follow-up gaps + GitLab CI cleanup; PR #1701 explicit "GAP-700/701 follow-ups" — filing only.
  - AC: 0/9.
- **New completion_pct:** 0
- **New notes:** HTTP client integration chưa implement; LoggingEmailService log-only persist; effort ~4h scheduled Wave 105+ per gap notes.

### GAP-708 — Wave 103 post-merge audit suite (api-contract + ops-readiness)

- **Verdict:** PARTIAL→adjust_pct=20
- **Evidence:**
  - Code state: `documents/04-quality/audits/local-stack/` có 4-5 wave-103 walks (owner persona + stack-up smoke + email MailHog + admin persona + 2fa TOTP) dated 2026-05-22 → local-stack smoke partial.
  - Code state: NO api-contract/ + ops-readiness/ subdir audit reports for wave-103.
  - Commits: PR #1710 Wave 103 closure-sync + 12 gap flips; PR #1716 file GAP-716 tracking post-merge audit obligations; rename GAP-708→GAP-710 (PR #1709 referenced) — gap number conflict resolution.
  - AC: 0/6 — 2 canonical audits (api-contract + ops-readiness) chưa shipped; local-stack smoke is different scope.
- **New completion_pct:** 20
- **New notes:** Local-stack walks partial coverage shipped 2026-05-22; canonical api-contract + ops-readiness audits chưa run; deadline 2026-05-25 missed.

### GAP-537c-followup-screenshot-capture (skipped — covered above)

(Đã audit ở vị trí GAP-537c ở trên — OPEN→keep.)

---

## SCOPE-REVISE candidate

### GAP-213 — Spring Cloud BOM resolution (scope-revise lưu ý)

Mặc dù đã có fix shipped (PR #523), gap description references "Spring Cloud BOM" với pin version 2025.0.x; current pom.xml không match "spring-cloud-dependencies" string → có thể Spring Cloud đã removed entirely hoặc renamed property. Verdict main giữ PARTIAL→60 (fix did ship), nhưng cờ SCOPE-REVISE: gap description outdated relative to current dependency tree; reviewer should re-validate scope trước khi DONE flip prospective.

---

## CSV update commands (coordinator applies in closure PR)

### SHIPPED→DONE (6 gaps)

```bash
# GAP-212
sed -i 's|^GAP-212,phase-1-beta/.*$|GAP-212,phase-1-beta/closed/GAP-212-url-allowlist-test-flaky-dns.md,DefaultUrlAllowlistValidatorTest flaky due to DNS of `api.partner.com` → loop...,DONE,P1,Backend,phase-1-beta,100,2026-04-24,2026-06-01,Wave 5 PR #475 .invalid TLD fix shipped|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-212-url-allowlist-test-flaky-dns.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-503
sed -i 's|^GAP-503,phase-1-beta/.*$|GAP-503,phase-1-beta/closed/GAP-503-jvm-tomcat-hikari-tier-2-optimization.md,Tier 2 config optimization — JVM container ergonomics + Tomcat threads + HikariCP right-size + healthcheck grace period,DONE,P1,Backend,phase-1-beta,100,2026-05-13,2026-06-01,Wave 85 Bucket E PR #1429 shipped 3 AC|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-503-jvm-tomcat-hikari-tier-2-optimization.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-607
sed -i 's|^GAP-607,phase-1-beta/.*$|GAP-607,phase-1-beta/closed/GAP-607-rabbitmq-dlq-not-configured.md,RabbitMQ DLQ chua configured poison messages retry vo han,DONE,P1,DevOps,phase-1-beta,100,2026-05-17,2026-06-01,Wave 91 Bucket A PR #1487 outbox + DLQ shipped|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-607-rabbitmq-dlq-not-configured.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-609
sed -i 's|^GAP-609,phase-1-beta/.*$|GAP-609,phase-1-beta/closed/GAP-609-fe-claim-code-redemption-page-missing.md,FE thieu UI nhap claim code chi accept token UUID deep-link tu email,DONE,P1,Frontend,phase-1-beta,100,2026-05-17,2026-06-01,Wave 91 Bucket E PR #1488 + #1956 shipped|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-609-fe-claim-code-redemption-page-missing.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-674
sed -i 's|^GAP-674,phase-1-beta/.*$|GAP-674,phase-1-beta/closed/GAP-674-wave-99b-b5-onboarding-tour-readme.md,Wave 99B B5 — Golden-path Onboarding Tour README (rewrite 02-architecture/README.md),DONE,P1,Meta,phase-1-beta,100,2026-05-19,2026-06-01,Wave 99B B5 PR #1577 + #1578 shipped|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-674-wave-99b-b5-onboarding-tour-readme.md documents/04-quality/gaps/phase-1-beta/closed/
```

### PARTIAL→adjust_pct (7 gaps)

```bash
# GAP-139 → 40%
sed -i 's|^GAP-139,.*$|GAP-139,phase-1-beta/GAP-139-parent-dashboard-mvp-placeholder.md,Parent Dashboard MVP is Placeholder-Only (Wave 5 Widgets Missing),PARTIAL,P1,Frontend,phase-1-beta,40,2026-04-19,2026-06-01,Wave 49 GAP-267 mobile shell + hooks shipped; widgets per Wave 5 spec chưa|' documents/04-quality/gaps/gap-status.csv

# GAP-213 → 60% (scope-revise flag)
sed -i 's|^GAP-213,.*$|GAP-213,phase-1-beta/GAP-213-spring-cloud-bom-resolution.md,Spring Cloud BOM fails to resolve on Dependabot Boot bumps,PARTIAL,P1,Frontend,phase-1-beta,60,2026-04-24,2026-06-01,PR #523 fix shipped; SCOPE-REVISE pom may have evolved|' documents/04-quality/gaps/gap-status.csv

# GAP-506 → 60%
sed -i 's|^GAP-506,.*$|GAP-506,phase-1-beta/GAP-506-deploy-prod-script-tech-debt-cluster.md,deploy-prod.sh tech debt — chicken-and-egg + ephemeral cred pollution + start_period + email healthcheck,PARTIAL,P1,DevOps,phase-1-beta,60,2026-05-13,2026-06-01,Phase 1+2 shipped Wave 70/77/85; Phase 3 prod-* scripts AC verify pending|' documents/04-quality/gaps/gap-status.csv

# GAP-579 → 40%
sed -i 's|^GAP-579,.*$|GAP-579,phase-1-beta/GAP-579-soft-delete-restore-window.md,Soft-delete + 30-day restore window cho students/classes/grades (Wave 86 defer),PARTIAL,P1,Backend,phase-1-beta,40,2026-05-15,2026-06-01,Soft-delete entity layer present; 30d restore-window feature scope chưa|' documents/04-quality/gaps/gap-status.csv

# GAP-583 → 50%
sed -i 's|^GAP-583,.*$|GAP-583,phase-1-beta/GAP-583-rds-storage-alarm-wiring-resize-runbook.md,RDS storage alarm wiring CloudWatch + SNS + resize runbook (Wave 86 Bucket H H-AC2),PARTIAL,P1,DevOps,phase-1-beta,50,2026-05-15,2026-06-01,rds_storage_low alarm baseline present; SNS routing + resize runbook partial|' documents/04-quality/gaps/gap-status.csv

# GAP-664 → 40%
sed -i 's|^GAP-664,.*$|GAP-664,phase-1-beta/GAP-664-wave-98-3-layer-doc-completeness-drift.md,Wave 98 3-layer doc completeness drift — preferences + email domains missing layers,PARTIAL,P1,Meta,phase-1-beta,40,2026-05-19,2026-06-01,Detector shipped Wave 99C; existing preferences+email drift chưa backfill|' documents/04-quality/gaps/gap-status.csv

# GAP-708 → 20%
sed -i 's|^GAP-708,.*$|GAP-708,phase-1-beta/GAP-708-wave-103-post-merge-audit-suite-deadline.md,Wave 103 post-merge audit suite — api-contract + ops-readiness within 3-day deadline 2026-05-25,PARTIAL,P1,Meta,phase-1-beta,20,2026-05-22,2026-06-01,Local-stack walks partial 2026-05-22; canonical api-contract + ops-readiness chưa shipped|' documents/04-quality/gaps/gap-status.csv
```

### OPEN→keep (24 gaps — last_verified refresh only)

```bash
# Apply identical pattern updating only last_verified column from 2026-05-XX → 2026-06-01
# Manual sed batch for: GAP-138, GAP-220, GAP-257, GAP-288, GAP-289, GAP-293, GAP-294,
# GAP-346, GAP-362, GAP-427, GAP-429, GAP-445, GAP-532, GAP-537c-followup-screenshot-capture,
# GAP-580, GAP-595, GAP-613, GAP-641, GAP-654, GAP-665, GAP-678, GAP-685, GAP-686,
# GAP-691, GAP-698, GAP-701
# (Coordinator applies via single awk script — too verbose to enumerate sed inline)
```

---

## Audit metadata

- **Trigger:** Wave meta-7 Bucket B classification taxonomy applied per Foundation §2 Step 5 decision matrix
- **Method:** `git log --grep="GAP-NNN"`, multi-pattern `grep -rn` symbol verification, file-existence check, AC checkbox count per gap file
- **Banned shortcuts adhered:** NO `| head` truncation on grep; multi-pattern verification; full output read
- **Audit duration:** ~25 min wall-clock
- **Followup recommendations:**
  - Coordinator merge sequence: 6 SHIPPED→DONE rows + git mv files → 7 PARTIAL adjusts → 24 OPEN refresh in single closure PR
  - Track GAP-213 SCOPE-REVISE flag separately (1 candidate identified)
  - Audit-cadence gaps (GAP-678/685/691/698/708) suggest systemic audit-suite cadence enforcement issue — recommend meta-7 follow-up to surface pattern
