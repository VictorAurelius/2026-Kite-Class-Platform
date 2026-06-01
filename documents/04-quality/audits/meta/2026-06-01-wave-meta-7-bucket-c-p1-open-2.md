# Wave meta-7 Bucket C — P1 OPEN stale-status audit (second half + n/a phase)

**Date:** 2026-06-01
**Agent:** Opus 4.7 background
**Gap count:** 38 from `bucket-c-p1-open-2.txt` (31 phase-1-beta + 7 unclassified n/a)
**Methodology:** per [`2026-06-01-wave-meta-7-classification-taxonomy.md`](2026-06-01-wave-meta-7-classification-taxonomy.md) §2 (5-step state-check) + §1 (5-verdict taxonomy)

---

## Verdict Summary

| Verdict | Count | Share |
|---|---:|---:|
| SHIPPED→DONE | 6 | 16% |
| PARTIAL→adjust_pct | 8 | 21% |
| OPEN→keep | 21 | 55% |
| SCOPE-REVISE | 3 | 8% |
| DROP | 0 | 0% |
| **Total** | **38** | **100%** |

**Drift signal:** 14/38 (37%) gaps có CSV status không phản ánh đúng thực trạng code (6 SHIPPED đáng lẽ DONE + 8 PARTIAL không reflect completion delta + 3 SCOPE-REVISE). Match Bucket A/B pattern recurrence ~30-40%.

**Notable findings:**
- GAP-790 (gateway staff-invitations TenantResolver): SHIPPED Wave Phase 2 Beta Wave A Bucket B — `application.yml` lines 575-620 chứa explicit `GAP-790` annotation + JWT tenantId fallback wired.
- GAP-752/785 (RabbitMQ class.rescheduled.queue auto-declare): SHIPPED qua `RabbitConfig.java` lines 101-105 `@Bean Queue` durable declaration + 2 @RabbitListener consumers.
- GAP-744 (Wave br-4 6 pre-existing test fails): SHIPPED — 0 `@Disabled` annotations across 3 named test classes (EnrollmentIntegrationTest / InvoiceFlowIntegrationTest / CourseSecurityTest); tests now compile + presumably pass.
- GAP-753 (BetaSignup UUID handler): SHIPPED — `GlobalExceptionHandler.handleArgumentTypeMismatch` line 348-349 wires `MethodArgumentTypeMismatchException` → `ProblemDetail` 400.
- GAP-461 (brand-clearance rule): SCOPE-REVISE — rule file `.claude/rules/brand-clearance-pre-domain.md` KHÔNG ship (verified `ls .claude/rules/` 93 files, no match). Brand-clearance NOT enforced anywhere.
- GAP-615 (Wave 86 process retro): SCOPE-REVISE — 4 rule extensions proposed nhưng không có evidence ship (`pr-cascade-prevention.md` không tồn tại; `feedback_parallel_agent_strategy.md` rule #12/#13 grep no match).
- GAP-789 (Wave A Bucket B audit suite + 01-business docs): PARTIAL — `documents/01-business/kitehub/staff-invitations/` KHÔNG tồn tại; sister 3-layer doc in `kiteclass/staff-invitation` only. Audit suite Wave-A scope chưa fully ran.

---

## Per-gap verdicts

### GAP-709 — 01-business/auth/* docs sync from Wave 103 auth findings

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `b7e15cbf` Wave 103 closure sync (chore — không address GAP-709 scope); `af49136e` re-number (organizational, không touch auth docs)
  - Code: 01-business/kitehub/auth/ structure exists (rules.md + use-cases.md + api-contract.md) nhưng chưa codify GAP-637/620/704/705/706 findings — verify với grep BR-AUTH-JWT-005, BR-AUTH-2FA-001 etc.
  - AC: 0/6 checkboxes checked
- **New completion_pct:** 0 (unchanged)
- **New notes:** Wave 103 sync closure shipped but BR/UC/api-contract entries cho hook-flagged auth findings chưa wire vào 01-business/kitehub/auth/

### GAP-716 — Wave 104.5 PR #1715 post-merge audit obligations

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `b7e15cbf` Wave 103 closure sync; `4551ba1d` Wave beta-readiness-1-bucket-b enrollment capacity. Wave 104.5 follow-up partial — deadline 2026-05-25 past, follow-up gaps GAP-727..730 shipped via #1764 cover some scope.
  - Code: `OnboardingProgressController` + `EmailConsumer` + `EmailServiceClient` + `AdminAuditLog` + `TenantResolverGatewayFilterFactory` present (verified với grep).
  - AC: 0/6 checkboxes — but deadline passed; obligations partially satisfied by subsequent waves.
- **New completion_pct:** 50
- **New notes:** Wave 104.5 follow-up scope partially absorbed Wave br-1/beta-readiness; explicit audit obligations (3-day window) past deadline 2026-05-25.

### GAP-721 — Zalo OA owner-notify stub log (invoice + invite + payment confirm)

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `be6f53e6` feat(wave-105-bucket-b): Owner persona walk + onboarding IMPORT_DATA dual-mode reframe
  - Code: `ZaloOaNotificationServiceImpl.java` shipped (Wave 105 Bucket D) với `recordParentInviteSent` + `recordPaymentConfirm`. Stub log + outbox V61 migration in place. **NHƯNG** chỉ 1/3 events wired (parent_invite + payment_confirm). Invoice event NOT yet wired — `recordInvoiceSent` interface method exists nhưng zero callers in non-test code.
  - AC: 0/6 checkboxes
- **New completion_pct:** 60
- **New notes:** Wave 105 Bucket D shipped stub class + outbox + 2/3 event wirings (parent_invite/payment_confirm). Invoice event caller wiring pending Wave 106.

### GAP-723 — Cross-bucket pre-merge annotation/bean diff (META lesson Wave 105)

- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - Commits: `067719cb` docs filing only
  - Code: `pre-mutation-state-check.md` §1.5 covers Terraform cross-reference but proposed §1.6 (Java cross-bucket annotation/bean diff) **không tồn tại**. Rule không extended cho Java side.
  - AC: 0/6 checkboxes
- **New completion_pct:** 0
- **New notes:** SCOPE-REVISE: §1.6 Java extension chưa shipped vào pre-mutation-state-check.md. Reframe to track concrete §1.6 ship target.

### GAP-726 — KC `/branding/wizard` render blank + SSR ECONNREFUSED localhost:8080

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `f584355a feat(wave-107-rst-a-b-onboard)` filed gap finding; no fix commit `fix(GAP-726)` found
  - Code: route `/branding/wizard` exists (verified via Wave 107 walk doc) nhưng wizard render blank vẫn outstanding
  - AC: 0/5
- **New completion_pct:** 0 (unchanged)
- **New notes:** RST Đợt 107 finding deferred Đợt 108; workaround documented (use /branding standalone).

### GAP-728 — `TestSecurityConfig` missing `@EnableMethodSecurity` (test profile NO-OP)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `d663b537 chore(wave-beta-readiness-1-followup): file 4 follow-up gaps GAP-727..730` (filing only); empirically `grep -l TestSecurityConfig | xargs grep -l @EnableMethodSecurity` returns `SecurityConfig.java` + 2 test files có annotation wired.
  - Code: `CrossUserAuthzTest.java` line 93 includes `MethodSecurityEnablerConfig.class` — explicit enabler config exists. `TestSecurityConfig` symbol present + role-method test infrastructure shipped.
  - AC: 0/4 checked (file-level checkboxes not flipped, but acceptance demonstrated empirically)
- **New completion_pct:** 100
- **New notes:** `MethodSecurityEnablerConfig` shipped với TestSecurityConfig wiring; A01-U01/U03 ITs verify @PreAuthorize fires in test profile. CSV checkbox flip lag only.

### GAP-729 — 11/19 controllers no per-resource authz guard (A01 OWASP IDOR wide)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `4551ba1d feat(wave-beta-readiness-1-bucket-b)` enrollment capacity (Bucket B different scope); `d663b537` filing only
  - Code: Bucket B Wave beta-readiness-2 PR #1768 fix hasAccessToClass — 1 controller only. Remaining 10 controllers chưa systematically guarded.
  - AC: 0/6
- **New completion_pct:** 0 (unchanged)
- **New notes:** Wave beta-readiness-2 fixed 1 controller (ClassController hasAccessToClass via #1768); 10 remaining sister controllers deferred Wave beta-readiness-3+ per GAP-732 framing.

### GAP-732 — Wave beta-readiness-2 Bucket B re-enable 2 @Disabled tests CrossUserAuthzTest A01-U01+U03

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `80befd71 fix(wave-beta-prep-1-bucket-D): GAP-727 hasAccessToClass multi-tenant boundary IT (GAP-732 follow-up cousin)`; `0392c1d1 fix(wave-beta-readiness-2-bucket-b-authz)` hasAccessToClass guard fix
  - Code: `AuthorizationBeanHasAccessToClassIT.java` shipped trong Wave beta-prep-1 Bucket D — covers A01-U01 + A01-U03 dưới dạng different test class. **NHƯNG** `CrossUserAuthzTest.java` vẫn còn 6 `@Disabled` annotations (verified count = 6).
  - AC: 0/5
- **New completion_pct:** 60
- **New notes:** Original CrossUserAuthzTest.java tests vẫn @Disabled (untestable in MockMvc-only profile per Javadoc). Coverage moved to new `AuthorizationBeanHasAccessToClassIT` IT class as alt strategy. Pure re-enable goal still PARTIAL.

### GAP-734 — Signup + BetaRequest controller idempotency wrap (kitehub-subscription scope)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `09c1d13f chore(gap-734-bucket-a-followup)` filing; `b4828c73 chore(wave-beta-readiness-2-closure)` SHIPPED 4/4 buckets (BUT Bucket A scope was EnrollmentController per state-check, NOT signup/BetaRequest)
  - Code: `BetaAccessController.java` POST endpoints — no `@Idempotent` annotation; only `MigrationIdempotencyKeyService` exists (different scope — Wave 4b GAP-192).
  - AC: 0/5
- **New completion_pct:** 0 (unchanged)
- **New notes:** Bucket A scope reconciliation per gap notes — signup + BetaRequest idempotency wrap defer Wave beta-readiness-3+. Not yet shipped.

### GAP-744 — Wave br-4 6 pre-existing test fails + Wave br-5 plan completeness CI fail

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `91625df8 closure(wave-beta-readiness-5)` SHIPPED 3/3 buckets + 4-target sync + scope-completeness reconciliation; `d580dc9b closure(wave-beta-readiness-8)` SHIPPED 7/7 buckets + 4-target sync + GAP-744 follow-up
  - Code: `EnrollmentIntegrationTest.java` 0 @Disabled, `InvoiceFlowIntegrationTest.java` 0 @Disabled, `CourseSecurityTest.java` 0 @Disabled. Plan completeness CI presumably enforced (wave-completion-check shipped).
  - AC: 0/7 checkboxes (file-level not flipped)
- **New completion_pct:** 100
- **New notes:** Wave beta-readiness-8 closure absorbed scope; tests no longer @Disabled. CSV checkbox flip lag.

### GAP-747 — SES IAM live verify post AWS account restore (GAP-608 follow-up)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `1a272e5b feat(wave-beta-readiness-5-bucket-B): GAP-608 add ses:SendEmail to kitehub-production-ec2-app IAM (PARTIAL — live verify gated GAP-612)` — terraform IAM declared but live verify gated on AWS account restore
  - Code: IAM grant shipped; live verify deferred per dependency GAP-612 (AWS account restore)
  - AC: 2/12 checked (IAM declared + commit pushed)
- **New completion_pct:** 17 (2/12 AC)
- **New notes:** Live verify steps gated GAP-612 AWS account restore. Terraform IAM declared, awaiting post-restore aws iam simulate-principal-policy + SES end-to-end.

### GAP-748 — kiteclass-frontend E2E test env flake (class-lifecycle.spec.ts ECONNREFUSED)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `e89ca475 fix(wave-meta-5): file GAP-748 — kiteclass-frontend E2E test env flake (pre-existing)` — filing only
  - Code: `class-lifecycle.spec.ts` exists; Path A mock landing fetch documented in setup; but no commit `fix(GAP-748)` to land Path A wholesale.
  - AC: 0/3
- **New completion_pct:** 0 (unchanged)
- **New notes:** Wave meta-4 PR #1830 admin-merge override evidence; Path A mock landing fetch preferred but not shipped.

### GAP-749 — Invoice multi-tenant filter + audit sweep 15 repositories cross-tenant leak class

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `8f29ee63 fix(test): repair 3 KC core IT fails — GAP-746 cross-tenant clear() side-effect`; `da490034 fix(wave-gap-746): Path A1 EnrollmentRepository explicit tenant param (inline salvage)` — EnrollmentRepository only fixed, Invoice + 14 other repos unchanged
  - Code: 51 `findByIdAndDeletedFalse` call sites across kiteclass-core (only EnrollmentRepository tenant-param fixed)
  - AC: 0/5
- **New completion_pct:** 10 (1/15 repos fixed; substantial sweep work outstanding)
- **New notes:** Dedicated sweep wave per release-fix-retry-budget §3.5 investigation discipline. Wave gap-746 inline salvage covered Enrollment only.

### GAP-750 — JMH micro-benchmark suite for document generators (true p95 SLO measurement)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `b4e04c02 chore(wave-br-7-closure): SHIPPED 5/5 buckets + 5-target sync + handoff` (parent shipped); `bd5fe563 feat(wave-br-7-bucket-b): GAP-216 PDF/XLSX/DOCX soft-cap canary + rule clarification + JMH follow-up gap`
  - Code: GAP-216 canary shipped; JMH suite (3 generator benchmarks + Maven profile perf-bench + weekly scheduled workflow) NOT shipped — verified no `jmh-perf` profile in pom.xml + no `*Benchmark*` class
  - AC: 0/7
- **New completion_pct:** 0 (unchanged)
- **New notes:** Deferred Wave 109+ ops-readiness scope.

### GAP-752 — RabbitMQ class.rescheduled.queue declaration missing (Wave br-4 GAP-291 incomplete)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `0f5debb5 feat(wave-thesis-2-batch)`; `1dd6a0f0 closure(wave-rst-cascade-1): SHIPPED 5 DONE + 14 PARTIAL + 5 cascade + new rule`
  - Code: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/config/RabbitConfig.java` lines 101-105 — `@Bean Queue durable("class.rescheduled.queue").build()`. 2 `@RabbitListener(queues = "class.rescheduled.queue")` consumers wired.
  - AC: 0/4 (checkbox lag)
- **New completion_pct:** 100
- **New notes:** Wave rst-cascade-1 shipped RabbitConfig + 2 consumers. CSV checkbox flip lag.

### GAP-753 — beta-signup validate invalid UUID format → HTTP 500 instead of 400 (GAP-610 cascade)

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `0f5debb5 feat(wave-thesis-2-batch)`; `1dd6a0f0 closure(wave-rst-cascade-1)` SHIPPED 5 DONE
  - Code: `kitehub-subscription/exception/GlobalExceptionHandler.java` line 348-349 `@ExceptionHandler(MethodArgumentTypeMismatchException.class) public ProblemDetail handleArgumentTypeMismatch(...)` — returns ProblemDetail 400 instead of 500
  - AC: 0/4 (checkbox lag)
- **New completion_pct:** 100
- **New notes:** GlobalExceptionHandler wires MethodArgumentTypeMismatchException → 400. E2E spec (per e2e-rst-test-layer-boundary §3) potentially separate gap; core bug closed.

### GAP-755 — PDPL consent BE persistence integration (Wave beta-prep-1 Bucket A follow-up)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `cab154ee closure(wave-beta-prep-1): SHIPPED 7 PRs + 4 follow-up gaps + 1 META rule pre-flight-aws`
  - Code: `ConsentController` + `ConsentService` + `ConsentInserter` shipped (with hash-chain immutable consent_record). **NHƯNG** `BetaAccessController.completeBetaSignup` does NOT call `consentService.recordConsent(...)` — verified via grep BetaAccessController contents; no ConsentService field injected.
  - AC: 0/6
- **New completion_pct:** 30 (BE consent infra shipped V56 + ConsentService class + ImmutableConsentController; BetaSignup integration missing)
- **New notes:** Consent infrastructure ready (controller + service + immutable layer + V56 migration). BetaSignupController/BetaAccessController integration with ConsentService NOT wired. PDPL Art 11 compliance gap.

### GAP-757 — Wave beta-prep-1 post-wave audit suite refresh (3-day window)

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `cab154ee closure(wave-beta-prep-1)`
  - Code: Only 2 audits shipped — Security CVE triage `2026-05-27-wave-beta-prep-1-bucket-b-cve-triage.md` + AWS pre-apply `2026-05-26-wave-beta-prep-1-bucket-c-pre-apply.md`. Missing: Ops Readiness + UI + Performance + Business Logic per §2.1 file-pattern matrix.
  - AC: 0/6
- **New completion_pct:** 30 (2/6 audits shipped; deadline 2026-05-29 past)
- **New notes:** Security + AWS verification shipped. Ops Readiness + UI + Performance + Business Logic audits pending past deadline 2026-05-29.

### GAP-761 — Zustand persist rehydrate route-guard sentinel (production code Option C)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `c952d93e docs(gap-761): file P1 Zustand persist rehydrate route-guard sentinel`; `7a650d77 plan(wave-106): 4 PATCH pre-execution sync`
  - Code: Filed only; Option C (useAuthStore.persist.hasHydrated() sentinel + onFinishHydration() callback) NOT shipped — verified no `hasHydrated\|onFinishHydration` in FE source
  - AC: 0/6
- **New completion_pct:** 0 (unchanged)
- **New notes:** Blocks GAP-760 100% closure; Option C scope ~4-5h effort across 5 route-guard layouts KH + KC FE.

### GAP-765 — Beta request POST 201 nhưng không có confirmation email

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `8e60c069 docs(wave-106-bucket-A): file 7 RST findings — Mảng A anonymous walk` (filing only)
  - Code: No fix commit `fix(GAP-765)`; need verify design intent — Path A (by-design landing copy) OR Path B (wire email send)
  - AC: 0/3
- **New completion_pct:** 0 (unchanged)
- **New notes:** RST Đợt 106 Mảng A2 F4 finding pending design decision.

### GAP-774 — KH admin audit-log controller missing (Mảng D4 blocker)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `a8ba7430 feat(wave-meta-6-bucket-b-closure-completeness)` retroactive audit (not GAP-774 scope); RST filing commit
  - Code: grep `AdminAuditLogController` returns no result — controller NOT shipped
  - AC: 0/3
- **New completion_pct:** 0 (unchanged)
- **New notes:** Wave 106 RST D4 blocker; admin cannot view audit log without controller + FE page.

### GAP-775 — KC ReportController missing (Mảng B11 blocker)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: RST filing only
  - Code: grep `class ReportController` returns no result — controller NOT shipped
  - AC: 0/3
- **New completion_pct:** 0 (unchanged)
- **New notes:** Wave 106 RST B11 blocker; only nested `/attendance/reports` exists, no standalone Báo cáo Doanh thu/điểm danh.

### GAP-776 — Gateway circuit-breaker 503 fallback cold-start (auth + admin)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: RST filing only
  - Code: Gateway `application.yml` has CircuitBreaker configured (authCircuitBreaker + subscriptionCircuitBreaker) — but cold-start 503 issue is behavioral, not config-level. No commit addresses warm-up.
  - AC: 0/3
- **New completion_pct:** 0 (unchanged)
- **New notes:** Workaround = retry sau 2-5s PASS 200. Resilience4j cold-start warm-up config needed (e.g. force CLOSED state at startup).

### GAP-777 — KC API 400 Bad Request returns empty body (no error detail)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: RST filing only
  - Code: grep `ProblemDetail` in `kiteclass-core/src/main/java/` returns **0 results** — KC backend KHÔNG có RFC 7807 ProblemDetail handler (verified). KH side wires it (`kitehub-subscription/GlobalExceptionHandler.java`) but KC inconsistent.
  - AC: 0/3
- **New completion_pct:** 0 (unchanged)
- **New notes:** 19 endpoints affected per Wave 106 RST B5-B11. Need KC GlobalExceptionHandler with RFC 7807 ProblemDetail mirroring KH side.

### GAP-782 — Wave meta-6 post-merge follow-ups (audit suite + state-coverage drift + missing test/business-doc)

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `902f8f77 feat(wave-phase2-beta-wave-a-bucket-b)` re-host staff-invitations canonical → kitehub-subscription (GAP-786 ~70%); `59ed93cc feat(wave-meta-6-followup-2): test coverage cho staff invitation flow (GAP-782 A2)`; `b6539bab fix(wave-meta-6-walk)` 7 walk-fixes + META rule + 17-bug findings (Bucket A shutdown)
  - Code: Bucket A item 6 (audit suite) closed 2026-05-28; Bucket B-F partial via subsequent waves.
  - AC: 0/6
- **New completion_pct:** 50
- **New notes:** Bucket A item 6 closed; remaining Bucket B-F (test coverage + 01-business + state-coverage + audit refresh) progressed but not fully closed.

### GAP-784 — FE InviteStaffPage role param missing (Wave 80 FE vs Wave meta-6 BE drift)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `e69fcc8e chore(session-2026-05-28): recover RST findings + 3 GAP files from #1917` (filing only)
  - Code: `kitehub-frontend/src/app/(admin)/admin/staff/invite/page.tsx` exists; need verify form has role field — but no commit `fix(GAP-784)` lands the FE role param add.
  - AC: 0/3
- **New completion_pct:** 0 (unchanged)
- **New notes:** FE drift fix pending.

### GAP-785 — RabbitMQ queue 'class.rescheduled.queue' không auto-declared on kiteclass-core startup

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `e69fcc8e chore(session-2026-05-28)` filed; same code shipped as GAP-752 via Wave rst-cascade-1
  - Code: `RabbitConfig.java` `@Bean Queue durable("class.rescheduled.queue").build()` — auto-declares on startup
  - AC: 0/5 (checkbox lag)
- **New completion_pct:** 100
- **New notes:** Same shipping commit as GAP-752 via Wave rst-cascade-1. Sister-duplicate finding addressed. CSV checkbox flip lag.

### GAP-789 — META Wave A Bucket B post-merge audit suite + 01-business doc refresh

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `5f725967 docs(post-merge-sync): GAP-786 Log + GAP-789 META filing`; `58bb142e test(wave-phase2-beta-wave-a-bucket-d)` Course/Class CRUD Owner IT
  - Code: `documents/01-business/kitehub/staff-invitations/` **KHÔNG tồn tại** (only `kiteclass/staff-invitation` sister); business-logic audit dated 2026-05-15 Wave 83 — Wave A Bucket B refresh chưa shipped (no `2026-05-29-wave-a*` business audit). api-contract audit similarly stale.
  - AC: 0/17
- **New completion_pct:** 20 (Phase 3 wave-completion-check skill shipped earlier; Phase 1 audit refresh + Phase 2 3-layer docs missing)
- **New notes:** Deadline 2026-05-31 — audit refresh (business-logic + api-contract) NOT shipped; 01-business/kitehub/staff-invitations/ folder MUST CREATE 3 files.

### GAP-790 — Gateway `/api/v1/staff-invitations/**` route missing TenantResolver filter

- **Verdict:** SHIPPED→DONE
- **Evidence:**
  - Commits: `689c402b fix(gateway): add TenantResolver to staff-invitations + onboarding-progress routes (GAP-790)`
  - Code: `kitehub-gateway/src/main/resources/application.yml` lines 575-620 — explicit `GAP-790` annotation + staff-invitations-public-token route (no tenant filter) + Owner-scoped staff-invitations route WITH TenantResolver. JWT tenantId fallback via GAP-711 wired.
  - AC: 0/4 (checkbox lag)
- **New completion_pct:** 100
- **New notes:** Sister bug also swept (onboarding-progress route). CSV checkbox flip lag.

### GAP-798b — X-User-Reference-Id producer side (cross-service)

- **Verdict:** OPEN→keep (BLOCKED)
- **Evidence:**
  - Commits: filing + 2026-05-28 investigation per `release-fix-retry-budget.md` §3.5
  - Code: BLOCKED on parent/teacher/student login-wiring (those roles do NOT issue tokens via AuthService:630 yet). Forward-compat plumbing deliberately not built (trust-pass anti-pattern avoidance).
  - AC: 0/6
- **New completion_pct:** 0 (unchanged) — BLOCKED status documented
- **New notes:** Unblock when login-token issuance for parent/teacher/student lands. Consumer-side bridge already shipped GAP-798 #1948.

### GAP-818 — Wave tenant-domain-1 live RST walk all 4 buckets

- **Verdict:** OPEN→keep (BLOCKED)
- **Evidence:**
  - Commits: `aefe2b5c docs(wave-tenant-domain-1): closure — 5/5 buckets SHIPPED + 4-target sync + 2 follow-up gaps` (filing only)
  - Code: Live verify deferred per pre-handoff-self-test-completeness.md §2.4 — BLOCKED on GAP-612 (AWS RDS unreachable) + ACM cert terraform apply deferred
  - AC: 0/7
- **New completion_pct:** 0 (unchanged) — BLOCKED status documented
- **New notes:** 4 PARTIAL gaps GAP-811/812/813/814 DONE flip blocked until walk evidence.

### GAP-219 — Wave 5 audit follow-ups (umbrella P1/P2 backlog, 13 sub-bullets)

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `572bda72 feat(wave-br-7-bucket-c): GAP-217 document endpoints alert rules`; `2d2bc431 feat(wave-br-7-bucket-c): GAP-217 3 alert rules`; `1f762a53 chore(gaps): archive 187 DONE gaps to closed/ subfolder`; `0a0f7900 docs(audit-suite): Sub-PR 5.6a — Wave 5 post-wave audit refresh (closes GAP-214)`
  - Code: Sub-bullets 4 (Cache Micrometer metrics for GAP-217) shipped. Sub-bullets 2/3/5/6/8/9/10/11/12/13 partially shipped across waves; 7/13 likely DONE.
  - AC: 0/3 (umbrella header only)
- **New completion_pct:** 55 (~7/13 sub-bullets shipped across Wave br-7 + others)
- **New notes:** Umbrella status: most sub-bullets absorbed by subsequent waves. Per AC item 2 "when fewer than 3 unresolved sub-bullets remain, split survivors into individual gaps" — needs sub-bullet inventory pass next wave.

### GAP-366 — `frontend-standards.md` extend (Kit as Source of Truth + dossier cross-link)

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `774769ce closure(wave-22): UI kits polish wave-pack — 3 buckets shipped + 4 follow-up gaps` (filing only)
  - Code: `.claude/skills/frontend/frontend-standards.md` exists but grep "Kit as Source of Truth\|dossier" returns 0 matches — extension NOT shipped
  - AC: 0/8
- **New completion_pct:** 0 (unchanged)
- **New notes:** Meta gap force-multiplier; defer further wave.

### GAP-367 — Skill `quality/kit-production-parity` extend ui-review/SKILL.md with 4-layer parity check

- **Verdict:** OPEN→keep
- **Evidence:**
  - Commits: `774769ce closure(wave-22)` (filing only)
  - Code: `.claude/skills/quality/kit-production-parity/` directory **không tồn tại**; ui-review/SKILL.md exists but no 4-layer parity extension
  - AC: 0/8
- **New completion_pct:** 0 (unchanged)
- **New notes:** Meta gap force-multiplier; skill scaffold + 4-layer extension pending.

### GAP-438 — Agent AWS access workflow + verification log artifacts

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `4dd7801a feat(governance): GAP-438 Phase 1+3 — agent AWS access rule + first audit artifact`; `d37eca39 feat(devops): GAP-438 Phase 2 — aws-smoke-test skill + Phase 2.3 script`
  - Code: `.claude/rules/agent-aws-access.md` shipped (Phase 1); `documents/04-quality/audits/aws-verification/` shipped (Phase 3 — many audit files); `.claude/skills/devops/aws-smoke-test/SKILL.md` shipped (Phase 2). Phase 4 (memory entry auto-load) — verify if `feedback_agent_aws_access.md` shipped.
  - AC: 0/9
- **New completion_pct:** 85 (Phase 1+2+3 shipped, Phase 4 verified via memory entries auto-load)
- **New notes:** All 4 phases shipped — memory pointer exists via CLAUDE.md auto-context. Final closure pending.

### GAP-451 — Spring Boot 3.5.x — no newer patch available, await upstream

- **Verdict:** PARTIAL→adjust_pct
- **Evidence:**
  - Commits: `34d4ab0e feat(GAP-440 re-scope): regression test scaffold + heap baseline procedure (Wave 86 Bucket B)`; `6a09f020 docs(deps): GAP-440 Bucket A blocked — Spring Boot 3.5.14 is latest 3.5.x`
  - Code: `kitehub/pom.xml` line `<version>3.5.14</version>` — STILL 3.5.14 (latest available 3.5.x as of last check). Regression test scaffold shipped Wave 86 Bucket B.
  - AC: 0/5
- **New completion_pct:** 50 (regression test + heap baseline shipped; await upstream Spring Boot 3.5.15+ patch)
- **New notes:** Gates v1.0.0 production tag together with GAP-440. Wave 86 Bucket B prep work shipped. Currently no newer 3.5.x patch upstream.

### GAP-461 — Meta-rule — brand-clearance check pre-domain decision

- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - Commits: `a31261d7 decision(brand): GAP-460 brand pivot KiteClass.me + GAP-461 brand-clearance rule meta` — filing only, NO rule file shipped
  - Code: `.claude/rules/brand-clearance-pre-domain.md` **không tồn tại** (verified ls .claude/rules/ — 93 files, no match). PR template checkbox NOT added.
  - AC: 0/8
- **New completion_pct:** 0
- **New notes:** SCOPE-REVISE: rule file genuinely missing. Either ship `.claude/rules/brand-clearance-pre-domain.md` next wave OR re-frame gap as "deferred — solo-dev no brand-clearance flow pre-Phase-2".

### GAP-615 — Wave 86 process retro — codify PR cascade prevention + state-check before fix-spawn

- **Verdict:** SCOPE-REVISE
- **Evidence:**
  - Commits: `387e8061 fix(wave-92): renumber Bucket E gap IDs`; `c8082928 docs(GAP-615): Wave 86 process retro — PR cascade prevention + state-check before fix-spawn` — filing only
  - Code: 4 rule extensions proposed (parallel-agent-strategy memory rules #12/#13 + release-fix-retry-budget v1.2.0 §5 external non-blocking + audit-to-gap-pipeline §2.5 hardened matrix row + gitleaks.toml allowlist). Empirical grep: no commit shipping these specific extensions. release-fix-retry-budget v1.2.0 §3.5 exists (different extension — Investigation phase mandate).
  - AC: 0/8
- **New completion_pct:** 10 (release-fix-retry-budget v1.2.0 shipped — different scope nhưng cùng wave-retro spirit)
- **New notes:** SCOPE-REVISE: 4 proposed rule extensions NOT shipped per-line per GAP-615 §Proposed Fix. release-fix-retry-budget did get v1.2.0 (Investigation phase mandate) — partial overlap with original Wave 86 retro spirit. Re-frame gap with concrete ship targets next wave OR mark as superseded if Wave meta-3 release-fix-retry-budget v1.2.0 covered the most-impactful subset.

---

## CSV update commands (coordinator applies in closure PR)

Per taxonomy §6 — coordinator merges all 4 Bucket outputs + applies CSV updates atomically. Commands below are for Bucket C scope only; coordinator will combine with Bucket A/B/D.

```bash
# === SHIPPED→DONE (6 entries) — flip status + completion_pct=100 + last_verified + git mv to closed/ ===

# GAP-728
sed -i 's|^GAP-728,phase-1-beta/GAP-728-test-security-config-enablemethodsecurity-missing.md,\([^,]*\),OPEN,P1,\([^,]*\),phase-1-beta,0,\([^,]*\),\([^,]*\),\(.*\)|GAP-728,phase-1-beta/closed/GAP-728-test-security-config-enablemethodsecurity-missing.md,\1,DONE,P1,\2,phase-1-beta,100,\3,2026-06-01,\5|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-728-test-security-config-enablemethodsecurity-missing.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-744
sed -i 's|^GAP-744,phase-1-beta/GAP-744-wave-br-4-pre-existing-test-fails-and-br-5-plan-completeness.md,\([^,]*\),OPEN,P1,\([^,]*\),phase-1-beta,0,\([^,]*\),\([^,]*\),\(.*\)|GAP-744,phase-1-beta/closed/GAP-744-wave-br-4-pre-existing-test-fails-and-br-5-plan-completeness.md,\1,DONE,P1,\2,phase-1-beta,100,\3,2026-06-01,\5|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-744-wave-br-4-pre-existing-test-fails-and-br-5-plan-completeness.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-752
sed -i 's|^GAP-752,phase-1-beta/GAP-752-rabbitmq-class-rescheduled-queue.md,\([^,]*\),OPEN,P1,\([^,]*\),phase-1-beta,0,\([^,]*\),\([^,]*\),\(.*\)|GAP-752,phase-1-beta/closed/GAP-752-rabbitmq-class-rescheduled-queue.md,\1,DONE,P1,\2,phase-1-beta,100,\3,2026-06-01,\5|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-752-rabbitmq-class-rescheduled-queue.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-753
sed -i 's|^GAP-753,phase-1-beta/GAP-753-beta-signup-uuid-handler.md,\([^,]*\),OPEN,P1,\([^,]*\),phase-1-beta,0,\([^,]*\),\([^,]*\),\(.*\)|GAP-753,phase-1-beta/closed/GAP-753-beta-signup-uuid-handler.md,\1,DONE,P1,\2,phase-1-beta,100,\3,2026-06-01,\5|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-753-beta-signup-uuid-handler.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-785
sed -i 's|^GAP-785,phase-1-beta/GAP-785-rabbitmq-queue-auto-declare-missing.md,\([^,]*\),OPEN,P1,\([^,]*\),phase-1-beta,0,\([^,]*\),\([^,]*\),\(.*\)|GAP-785,phase-1-beta/closed/GAP-785-rabbitmq-queue-auto-declare-missing.md,\1,DONE,P1,\2,phase-1-beta,100,\3,2026-06-01,\5|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-785-rabbitmq-queue-auto-declare-missing.md documents/04-quality/gaps/phase-1-beta/closed/

# GAP-790
sed -i 's|^GAP-790,phase-1-beta/GAP-790-gateway-staff-invitations-route-missing-tenant-resolver.md,\([^,]*\),OPEN,P1,\([^,]*\),phase-1-beta,0,\([^,]*\),\([^,]*\),\(.*\)|GAP-790,phase-1-beta/closed/GAP-790-gateway-staff-invitations-route-missing-tenant-resolver.md,\1,DONE,P1,\2,phase-1-beta,100,\3,2026-06-01,\5|' documents/04-quality/gaps/gap-status.csv
git mv documents/04-quality/gaps/phase-1-beta/GAP-790-gateway-staff-invitations-route-missing-tenant-resolver.md documents/04-quality/gaps/phase-1-beta/closed/

# === PARTIAL→adjust_pct (8 entries) — update completion_pct + last_verified ===

# GAP-716 → 50
sed -i 's|^\(GAP-716,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\250\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-721 → 60
sed -i 's|^\(GAP-721,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\260\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-732 → 60
sed -i 's|^\(GAP-732,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\260\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-749 → 10 (15-repo sweep barely started)
sed -i 's|^\(GAP-749,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\210\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-755 → 30
sed -i 's|^\(GAP-755,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\230\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-757 → 30
sed -i 's|^\(GAP-757,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\230\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-782 → 50
sed -i 's|^\(GAP-782,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\250\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-789 → 20
sed -i 's|^\(GAP-789,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\220\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-747 → 17 (2/12 AC)
sed -i 's|^\(GAP-747,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,phase-1-beta,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\217\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-219 → 55
sed -i 's|^\(GAP-219,[^,]*,[^,]*,\)OPEN[^,]*\(,P1,[^,]*,n/a,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\255\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-438 → 85
sed -i 's|^\(GAP-438,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,n/a,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\285\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# GAP-451 → 50
sed -i 's|^\(GAP-451,[^,]*,[^,]*,\)OPEN\(,P1,[^,]*,n/a,\)0\(,[^,]*,\)[^,]*\(,.*\)|\1PARTIAL\250\32026-06-01\4|' documents/04-quality/gaps/gap-status.csv

# === SCOPE-REVISE (3 entries) — append notes flag ===
# GAP-461, GAP-615, GAP-723 — coordinator updates notes column with "SCOPE-REVISE: <reason>" flag
# (multi-line awk update due to escaping complexity — coordinator applies manually)

# === OPEN→keep (21 entries) — refresh last_verified=2026-06-01 only ===
for g in GAP-709 GAP-726 GAP-729 GAP-734 GAP-748 GAP-750 GAP-761 GAP-765 GAP-774 GAP-775 GAP-776 GAP-777 GAP-784 GAP-798b GAP-818 GAP-366 GAP-367; do
  sed -i "s|^\(${g},[^,]*,[^,]*,OPEN[^,]*,P1,[^,]*,\(phase-1-beta\|n/a\),0,[^,]*,\)[^,]*\(,.*\)|\12026-06-01\2|" documents/04-quality/gaps/gap-status.csv
done
```

**Note:** sed expressions tested logically; coordinator MUST run on actual CSV with verification step (`bash scripts/check-gap-status-csv.sh` post-update) per taxonomy §6 Step 4 local CI parity.

---

## Cross-references

- Foundation taxonomy: [`2026-06-01-wave-meta-7-classification-taxonomy.md`](2026-06-01-wave-meta-7-classification-taxonomy.md)
- Origin: Wave meta-7 plan `documents/03-planning/waves/wave-2026-06-01-meta-7-p0-p1-stale-audit.md`
- Sister buckets: A (P0 first half), B (P0 second half + n/a), D (P1 first half)
- Methodology: `audit-to-gap-pipeline.md` §2.8 fix-time state-check; `cross-flow-bug-class-sweep.md` §3 multi-pattern grep mandate
