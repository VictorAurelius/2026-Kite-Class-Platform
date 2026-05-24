---
title: Wave beta-readiness-3 — Idempotency completion + Test flake fix + Authz test re-enable + Email pipeline cluster
wave: 3
waves: [beta-readiness-3]
tag_primary: beta-readiness
tags_secondary: [idempotency-completion, test-infra-fix, authz-test-coverage, email-pipeline, phase-1-closure]
counter: 3
created: 2026-05-24
date_launch: 2026-05-25
status: draft
audience: dev
gaps:
  - GAP-734
  - GAP-735
  - GAP-732
  - GAP-606
  - GAP-610
  - GAP-611
---

# Wave beta-readiness-3 — Idempotency completion + Test flake fix + Authz test re-enable + Email pipeline cluster

**Mục tiêu:** Đóng 3 follow-up gap chính từ Wave beta-readiness-2 (GAP-732/734/735) + dọn cluster email beta-signup pipeline (GAP-606/610/611) — tổng 6 Phase 1 BETA P0 đóng trong 4 bucket disjoint.

**Khởi sự:** Wave beta-readiness-2 ship 4/4 bucket (9 PR merged); 4 follow-up gap filed (GAP-732/733/734/735); cluster email beta-signup pipeline GAP-606/610/611 từ Wave 90 walkthrough còn block beta invite flow.

**Thời gian ước tính:** ~10-12h (1-2 phiên) — 4 bucket scope, disjoint at file level.

**Tag scheme:** per `.claude/rules/wave-tag-numbering-convention.md` §2 — `beta-readiness-3` counter 3, descriptor `idempotency-completion-test-flake-email-pipeline`.

---

## 1. Brainstorm (5-10 phút)

**Q1 (đối tượng phục vụ):**
- Phase 1 BETA gate — 6 P0 blocker còn lại trong cluster Wave beta-readiness-2 follow-up + email pipeline
- Personas affected: P2 Owner (signup idempotency + invite-staff), Beta Invitee anonymous (beta-signup pipeline GAP-610/611), P1 Solo Teacher (authz test coverage)
- CI gate hygiene (GAP-735) = developer experience — không trực tiếp persona-facing nhưng compound across mọi PR

**Q2 (giải pháp đã xét và loại):**
- ❌ Defer GAP-735 sang dedicated test-infra wave riêng → CI gate đang block mọi `kiteclass-core` code PR với AUDIT_OVERRIDE; ship trong wave này = unblock force-multiplier
- ❌ Bundle GAP-606/610/611 với GAP-608 (IAM ses:SendEmail) → GAP-608 cần AWS access (block GAP-612); ship 3 code-only gap trong wave này, GAP-608 wait restore
- ❌ Skip GAP-732 (test re-enable) → production fix Wave beta-readiness-2 đã ship; thiếu test coverage = compound technical debt
- ✅ **4-bucket parallel wave** — A idempotency-completion + B test-infra-fix + C authz-test-coverage + D email-pipeline

**Q3 (rủi ro):**
- Bucket A (GAP-734): port `common/idempotency/` từ `kiteclass-core` sang `kitehub-subscription` — quyết định Option A (shared lib kitehub-platform) vs Option B (duplicate). Option B faster nhưng debt; Option A cleaner nhưng cross-module refactor. Default Option B nếu agent chọn fast path.
- Bucket B (GAP-735): test pollution fix có thể vẫn flaky sau patch nếu root cause sai (multi-tenant context bleed vs transaction rollback). Option A `@Transactional + @Rollback` first; nếu vẫn fail → Option B `@DirtiesContext`
- Bucket C (GAP-732): test fixture work needs teacher + course + class via mockMvc — sister của A01-U02 + A01-U04 existing tests. Effort theo precedent.
- Bucket D (GAP-606/610/611): 3 sub-issue khác nhau cùng email/beta-signup flow:
  - GAP-606 — template missing `admin-new-login-alert.html` (Wave 90 hotfix)
  - GAP-610 — RLS suspect `findByInviteToken` empty cho valid token (need RLS policy investigation)
  - GAP-611 — POST `/api/v1/auth/request-beta-access` route 404 (gateway routing hoặc security shadow Wave 89 JWT filter regression)
  Cả 3 cluster theo "beta invite pipeline" nhưng có thể disjoint enough cho 1 bucket nếu mỗi sub-fix nhỏ. Pivot to Bucket D1/D2/D3 nếu effort blow up.

---

## 2. Task Breakdown

| Bucket | Loại | Agent | Phụ thuộc | Thời gian |
|---|---|---|---|---|
| **A** Idempotency completion kitehub-subscription (GAP-734) | BE pattern port | Agent A worktree (Opus narrow scope vì Sonnet thrash precedent Wave br-2) | Đọc Wave beta-readiness-2 Bucket A `IdempotencyService` (PR #1769) | ~3h |
| **B** Test flake fix kiteclass-core (GAP-735) | BE test infra | Agent B worktree (Opus — broad refactor across 3 IT classes) | Không | ~3h |
| **C** CrossUserAuthzTest re-enable (GAP-732) | BE test fixture | Agent C worktree (Sonnet — narrow scope OK) | Không (đọc A01-U02 + A01-U04 precedent patterns) | ~2h |
| **D** Email beta-signup pipeline cluster (GAP-606 + GAP-610 + GAP-611) | BE template + RLS + routing | Agent D worktree (Opus — 3 sub-issue domain spread) | Đọc Wave 90 walkthrough audit report | ~3-4h |
| Tổng hợp + ship | Main session | — | All 4 done | ~30 phút |

**Kiểm tra rời rạc:**
- A đụng `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/{auth,beta_access}/controller/` + `kitehub-subscription/src/main/java/.../common/idempotency/` (port hoặc duplicate) + `kitehub-subscription/src/test/.../auth/SignupIdempotencyIT.java` + `kitehub-subscription/src/test/.../beta_access/BetaAccessIdempotencyIT.java`
- B đụng `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/{integration/EnrollmentIntegrationTest.java, integration/InvoiceFlowIntegrationTest.java, module/course/service/CourseSecurityTest.java}` — thêm `@Transactional + @Rollback` hoặc `@DirtiesContext` per class
- C đụng `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/integration/CrossUserAuthzTest.java` (2 method body — A01-U01 negative + A01-U03 positive) + có thể `kiteclass-core/src/test/java/.../testutil/TestDataBuilder.java` (helper `createTestClass()` nếu cần extract)
- D đụng `kitehub/kitehub-email/src/main/resources/templates/email/admin-new-login-alert.html` (GAP-606 new template) + `kitehub-platform/src/main/java/.../beta_access/` controller/security/RLS investigation (GAP-610/611)

**Conflict risk:**
- A vs D: cả 2 chạm `kitehub-subscription` nhưng KHÁC FILE — A đụng auth + beta_access controller (existing files); D đụng kitehub-platform beta_access (different module). Chỉ B vs C trong kiteclass-core/src/test — different files. ✅
- B vs C: cả 2 trong `kiteclass-core/src/test` — DIFFERENT FILES (B touches 3 IT classes Enrollment/Invoice/CourseSecurity; C touches CrossUserAuthzTest only). ✅
- A vs B/C: khác project entirely (kitehub-subscription vs kiteclass-core). ✅
- D vs A: A wraps `AuthController.register` + `BetaAccessController.requestBetaAccess` (idempotency) trong kitehub-subscription; D fixes `BetaAccessController` route 404 (GAP-611) — cùng controller possible conflict. **Mitigation:** Bucket D ship first (fix routing) → Bucket A ship sau (wrap idempotency). Sequential A-after-D nếu conflict; otherwise parallel.

**Revised spawn pattern:** D first (~30 min priority gate cho GAP-611 routing fix) → A/B/C parallel khi D's controller route stabilize. Hoặc full parallel nếu pre-spawn state-check verify D's BetaAccessController fix không đụng line A wraps.

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM (follow-up completion + test infra; ship pattern reusable). Model: Opus 4.7 coordinator + Opus cho A/B/D agents (Sonnet thrash precedent Wave br-2) + Sonnet C narrow scope.
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO (D không touch FE; tất cả buckets BE-only). Skip Bucket 0 Foundation.

> **Gap referencing convention** per `.claude/rules/gap-architecture-v2.md`: 6 gap ids verified via `bash scripts/query-gaps.sh GAP-NNN` — all OPEN/PARTIAL Phase 1 BETA P0/P1.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| A | **Idempotency completion kitehub-subscription** | GAP-734 | P1 | `kitehub-subscription/src/main/java/.../auth/controller/`, `.../beta_access/controller/`, `.../common/idempotency/` (port hoặc duplicate), `kitehub-subscription/src/test/.../{auth,beta_access}/*IdempotencyIT.java` | parallel (sau D nếu conflict BetaAccessController) |
| B | **Test flake fix kiteclass-core** | GAP-735 | P1 | `kiteclass-core/src/test/java/com/kiteclass/core/integration/EnrollmentIntegrationTest.java`, `.../integration/InvoiceFlowIntegrationTest.java`, `.../module/course/service/CourseSecurityTest.java` (thêm `@Transactional + @Rollback` Option A, fallback `@DirtiesContext` Option B) | parallel |
| C | **CrossUserAuthzTest re-enable** | GAP-732 | P1 | `kiteclass-core/src/test/java/com/kiteclass/core/integration/CrossUserAuthzTest.java` (re-enable A01-U01 + A01-U03 với fixture work), optional `kiteclass-core/src/test/.../testutil/TestDataBuilder.java` (helper extract) | parallel |
| D | **Email beta-signup pipeline cluster** | GAP-606 + GAP-610 + GAP-611 | P0 | `kitehub-email/src/main/resources/templates/email/admin-new-login-alert.html` (new), `kitehub-platform/src/main/java/.../beta_access/` (GAP-610 RLS + GAP-611 routing investigation), `kitehub-gateway` config nếu routing issue | **FIRST** (block A nếu BetaAccessController conflict) |

### Bucket A — Idempotency completion kitehub-subscription (GAP-734)

- Files:
  - **Option B (default, faster):** Duplicate `common/idempotency/` package từ kiteclass-core sang `kitehub-subscription/src/main/java/.../common/idempotency/` (4 file: `IdempotencyScope.java` + `IdempotencyRecord.java` + `IdempotencyRecordId.java` + `IdempotencyService.java`)
  - **Option A (cleaner, slower):** Move to `kitehub-platform` shared lib JAR — both kiteclass-core + kitehub-subscription import; refactor existing kiteclass-core imports
  - Wrap `AuthController.register` (signup) + `BetaAccessController.requestBetaAccess` (beta-request) với inline idempotency logic (Option B pattern từ Wave beta-readiness-2 Bucket A PR #1769)
  - Reuse table `idempotency_keys` — kitehub-subscription connects same DB instance; query `WHERE scope IN ('SIGNUP', 'BETA_REQUEST')` (enum value đã reserved Wave beta-readiness-2)
- Tests: 2 IT `SignupIdempotencyIT.java` + `BetaAccessIdempotencyIT.java` — submit POST same Idempotency-Key 2x → cached response, NOT duplicate `User` row / beta_signup row
- Acceptance:
  - [ ] `common/idempotency/` available trong kitehub-subscription (Option B duplicate OR Option A shared lib)
  - [ ] `AuthController.register` + `BetaAccessController.requestBetaAccess` wrap với inline idempotency
  - [ ] 2 IT tests PASS
  - [ ] `mvn -pl kitehub-subscription verify -P strict-warnings` PASS

### Bucket B — Test flake fix kiteclass-core (GAP-735)

- Files: 3 IT classes thêm `@Transactional + @Rollback(true)` (Option A preferred — Spring rolls back DB state per test):
  - `kiteclass-core/src/test/java/com/kiteclass/core/integration/EnrollmentIntegrationTest.java` (1 failure deterministic)
  - `kiteclass-core/src/test/java/com/kiteclass/core/integration/InvoiceFlowIntegrationTest.java` (1 failure deterministic)
  - `kiteclass-core/src/test/java/com/kiteclass/core/module/course/service/CourseSecurityTest.java` (4 failures suite-pollution only)
- Option B fallback (nếu Option A insufficient): `@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)` — expensive ~20-30s/class context reload, guaranteed isolation
- Acceptance:
  - [ ] `EnrollmentIntegrationTest` → 14/14 PASS locally + CI suite
  - [ ] `InvoiceFlowIntegrationTest` → 5/5 PASS locally + CI suite
  - [ ] `CourseSecurityTest` → 15/15 PASS in full suite CI (not just isolated)
  - [ ] `mvn -pl kiteclass-core verify -P strict-warnings` PASS clean
  - [ ] Remove AUDIT_OVERRIDE trailers from future code PR touching kiteclass-core
- **Force-multiplier impact:** unblock mọi `kiteclass-core` code PR từ flaky CI gate

### Bucket C — CrossUserAuthzTest re-enable (GAP-732)

- Files: `kiteclass-core/src/test/java/com/kiteclass/core/integration/CrossUserAuthzTest.java` — re-enable 2 `@Disabled` tests A01-U01 (negative IDOR) + A01-U03 (positive owner)
- Test fixture pattern (đọc A01-U02 + A01-U04 precedent):
  ```java
  Long teacher1Id = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
  Long teacher2Id = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
  Long courseId = testDataBuilder.createTestCourse(mockMvc, objectMapper, tenantId, teacher1Id);
  // POST class với X-User-Id=teacher1 → teacher_id=teacher1 (per Wave br-2 Bucket B fix)
  String classJson = "{\"name\":\"Test Class 5A1\",\"maxStudents\":30}";
  Long classId = /* extract from POST response */;
  Long sessionId = /* create session via existing fixture */;
  // A01-U01: teacher2 GET → 403
  // A01-U03: teacher1 GET → non-Spring-Security 403 pattern (A01-U04 precedent)
  ```
- Optional: extract `createTestClass()` helper vào `TestDataBuilder` nếu pattern needed broader
- Acceptance:
  - [ ] A01-U01 test body shipped + PASS (negative IDOR)
  - [ ] A01-U03 test body shipped + PASS (positive owner)
  - [ ] Remove `@Disabled` annotation từ cả 2
  - [ ] `mvn -pl kiteclass-core verify -P strict-warnings` PASS

### Bucket D — Email beta-signup pipeline cluster (GAP-606 + GAP-610 + GAP-611)

**D1 — GAP-606 template missing (~30 min):**
- Files: new `kitehub-email/src/main/resources/templates/email/admin-new-login-alert.html` (Thymeleaf template)
- Producer side đã emit event (Wave 88 cutover); consumer side template missing → kitehub-email HTTP 500 + RMQ infinite retry → ~10 retries/sec wasted
- Content scope: admin login alert email (recipient `recipientName`, login fingerprint, IP, time, link "report this if not you")
- Acceptance: template render via existing `EmailTemplateRenderer` PASS unit test; producer event stops 500 loop

**D2 — GAP-610 RLS suspect (~1.5-2h investigation + fix):**
- Files: `kitehub-platform/src/main/java/.../beta_access/repository/BetaSignupRepository.java` + `db/migration/` RLS policy SQL
- Hypothesis 1: RLS `public anonymous` blocked `findByInviteToken` cho anonymous user — GET beta-signup validate là PUBLIC endpoint (no JWT)
- Hypothesis 2: tenant_id RLS filter applied without context → empty
- Investigation: check existing RLS policy on `beta_signups` table (V58/V59 RLS migrations); add bypass cho anonymous validate endpoint hoặc remove tenant_id filter cho this query
- Acceptance: GET `/api/v1/auth/validate-beta-token?token=<valid>` → 200 + token data (not TOKEN_NOT_FOUND)

**D3 — GAP-611 POST route 404 (~1h investigation + fix):**
- Hypothesis 1: Wave 89 JWT filter regression — public endpoint catch-all bị JWT filter strip Authorization header → security shadow → 404
- Hypothesis 2: gateway routing path mismatch
- Files: `kitehub-gateway/src/main/resources/application.yml` routing pattern + `kitehub-platform/.../beta_access/controller/BetaAccessController.java` `@PostMapping` annotation + `kitehub-platform/.../config/SecurityConfig.java` matchers
- Investigation: trace gateway → service → security chain via logs; verify `/api/v1/auth/request-beta-access` permitAll matcher present
- Acceptance: POST `/api/v1/auth/request-beta-access` với valid body → 201/200 (not 404)

- Acceptance (combined Bucket D):
  - [ ] GAP-606 admin-new-login-alert.html template ships + render PASS
  - [ ] GAP-610 RLS fix shipped — `findByInviteToken` returns row cho valid token
  - [ ] GAP-611 POST route 404 fixed — endpoint accessible từ FE
  - [ ] 3 IT/unit tests verify per sub-issue
  - [ ] `mvn verify -P strict-warnings` PASS multi-module

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

**Pre-spawn state-check deferred to coordinator** (next session). Mỗi symbol-shaped reference trong §3 sẽ verify before agent execution:

| Symbol | Type | Verification command | Expected Verdict |
|---|---|---|---|
| `IdempotencyService` (kiteclass-core) | Java class | `find kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/idempotency -type f` | ✅ exists (Wave br-2 Bucket A PR #1769) |
| `AuthController.register` | Java method | `find kitehub/kitehub-subscription/src/main/java -name "AuthController.java"` + grep `register` | ✅ exists (state-check confirm path) |
| `BetaAccessController.requestBetaAccess` | Java method | `find kitehub/kitehub-subscription/src/main/java -name "BetaAccessController.java"` + grep | ✅ exists |
| 3 IT classes for Bucket B | Java test classes | `find kiteclass/kiteclass-core/src/test/java -name "EnrollmentIntegrationTest.java" -o -name "InvoiceFlowIntegrationTest.java" -o -name "CourseSecurityTest.java"` | ✅ exists |
| `CrossUserAuthzTest.testTeacherAccessOwnClass` | JUnit test | `grep "testTeacherAccess\|teacher2_cannotGet" kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/integration/CrossUserAuthzTest.java` | ✅ exists `@Disabled` (Wave br-2 Bucket B update message) |
| `admin-new-login-alert.html` template | Thymeleaf file | `find kitehub/kitehub-email/src/main/resources/templates/email -name "admin-new-login-alert.html"` | 🆕 to-be-created (Bucket D1) |
| `BetaSignupRepository.findByInviteToken` | JPA method | `grep "findByInviteToken" kitehub/kitehub-platform/src/main/java` | ✅ exists (RLS bug surface) |
| `idempotency_keys` table | DB table | `grep "CREATE TABLE idempotency_keys" kiteclass-core/src/main/resources/db/migration/V66*` | ✅ exists (Wave br-2 V66) |

State-check empirical execution deferred per `audit-to-gap-pipeline.md` §2.8 — coordinator verify ngay khi spawn agent, revise §3 nếu finding mismatch.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify -P strict-warnings` | kitehub-ci |
| B | `cd kiteclass/kiteclass-core && ./mvnw clean verify -P strict-warnings` (cả 3 IT class PASS) | core-ci |
| C | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest=CrossUserAuthzTest -P strict-warnings` | core-ci |
| D | `cd kitehub && ./mvnw clean verify -P strict-warnings` (multi-module — kitehub-email + kitehub-platform + kitehub-gateway) | kitehub-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

**Spawn pattern revised (D-first):**

1. **First wave spawn:** Agent D (Opus, ~3-4h) — fixes GAP-611 BetaAccessController routing FIRST (block A nếu conflict)
2. **D's BetaAccessController fix merged hoặc state-check confirm no conflict** → Spawn A + B + C parallel (3 agents)

Alternative full-parallel (nếu state-check verify D's GAP-611 fix scope không đụng line A wraps `requestBetaAccess`):
- Single message spawn 4 agent (A Opus + B Opus + C Sonnet + D Opus)
- Each agent own worktree per `agent-worktree-isolation.md`
- `Agent({subagent_type: general-purpose, isolation: worktree, run_in_background: true})`
- Coordinator (Opus 4.7 1M) synthesize 4 agent outputs → ship 1-4 PR

**Agent model selection (lesson Wave br-2):**
- Sonnet 200k → fail autocompact thrash trong repo này (3/3 fail Wave br-2)
- Opus 1M → survive (Wave br-2 Agent A Opus narrow scope worked + coordinator inline)
- **Default Opus cho A/B/D (broad scope), Sonnet C OK (narrow single-file)**

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Mỗi bucket PR cập nhật affected GAP file Log + status (6 gap: GAP-732, GAP-734, GAP-735, GAP-606, GAP-610, GAP-611)
- ROADMAP §🚀 Next Action update trong closure PR
- Wave plan frontmatter `status: draft → complete` flip trong closure PR
- `wave-history.jsonl` append entry `beta-readiness-3 | SHIPPED ...` per Rule 15
- Sub-gap filed cho mọi deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3 — likely candidates: Bucket D live verify gated GAP-612
- Chạy `bash scripts/prune-merged-worktrees.sh --yes` sau all bucket PR merged, trước closure PR draft
- `## Release Plan Progress` section trong closure PR body per `feedback_wave_closure_release_progress_report.md`
- Scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3 — mọi item §3 Scope categorize ✅/🟡/❌

### Output Review Checklist (per `output-review-mandate.md` §3)

- [ ] **Code** — two-stage-code-review mỗi PR
- [ ] **Tests** — IT pass cho 4 buckets: A 2 IT + B (3 existing IT class fix) + C 2 IT re-enable + D 3 IT/unit ≈ 10 IT total
- [ ] **Migrations** — không có new migration (Bucket A reuse V66 table; Bucket D2 có thể thêm RLS policy migration nếu cần)
- [ ] **API contract** — D2 + D3 verify api-contract.md `documents/01-business/kitehub/beta-access/api-contract.md` match endpoint paths
- [ ] **3-layer docs sync** — rules.md / use-cases.md / api-contract.md cập nhật cho A (signup + beta-request idempotency scope) + D (beta-signup pipeline error semantics)
- [ ] **Gap closure** — 6 gap DONE flip với `gap-done-discipline.md` §2 checklist
- [ ] **Post-merge sync 4 targets** per `post-merge-sync-completeness.md` §2
- [ ] **Pre-handoff self-test** per `pre-handoff-self-test-completeness.md` §2.1 — live-verify gated GAP-612 PARTIAL exit ramp
- [ ] **Dev-readable language** per `dev-readable-doc-language.md`

### Risks + Pivots

| Risk | Trigger | Mitigation |
|---|---|---|
| Sonnet agent fail autocompact thrash (recurrence Wave br-2 pattern) | Agent stops mid-implementation với "Autocompact is thrashing" | Default Opus cho A/B/D từ đầu; Sonnet chỉ C narrow scope. Coordinator inline pivot nếu agent block. |
| Bucket A conflict với Bucket D BetaAccessController | D fixes routing, A wraps idempotency on same controller | Sequential spawn D-first, A-after; HOẶC state-check trước spawn parallel |
| Bucket B `@Transactional + @Rollback` không fix CourseSecurityTest suite-pollution | Tests vẫn fail trong full suite sau Option A patch | Fallback Option B `@DirtiesContext` (expensive 20-30s/class) — accept slower CI |
| Bucket D2 RLS investigation rabbit hole | RLS policy reading SQL + production schema verify cần >2h | Time-box D2 ≤2h; nếu over, file follow-up gap + ship D1 + D3 only |
| GAP-610/611 live verify blocked GAP-612 | Cannot verify post-fix on production | Document PARTIAL exit ramp per `pre-handoff-self-test-completeness.md` §2.1; ship code fix + IT/unit tests as proof |
| 6 gap closure CSV sync conflict (Wave br-2 pattern recurrence) | Multiple bucket PRs update CSV → merge conflict | Coordinator merge sequential (D → A → B → C order); auto-resolve via append-only pattern |

### Out-of-scope (track separately)

| Item | Where |
|---|---|
| GAP-608 EC2 IAM ses:SendEmail permission | Gated GAP-612 AWS restore (need IAM modify) — defer until restore |
| Bucket A Option A shared lib `kitehub-platform` (vs Option B duplicate) | Option B default cho speed; Option A nếu next refactor wave consolidate |
| All buckets: Production live verify post-deploy | Gated GAP-612 AWS restore — `pre-handoff-self-test-completeness.md` §2.1 PARTIAL exit ramp |
| GAP-732 helper extract `createTestClass()` to TestDataBuilder (broader use) | Optional within Bucket C — defer nếu narrow scope sufficient |
| Per-tenant rate-limit Idempotency-Key abuse (DDoS) | follow-up gap Wave beta-readiness-4+ per GAP-730 §Out-of-scope |

### Audit context

Wave beta-readiness-1 V2 audit (3 reports shipped 2026-05-24 PR #1759) — within `outside-in-coverage-trigger.md` §4 ≤30-day window. Skip outside-in audit hợp lệ cho Wave beta-readiness-3 scope (same Phase 1 closure cluster).

**Post-wave Wave beta-readiness-2 audit suite** (deadline 2026-05-27 per `post-wave-audit-mandate.md` §2.2) SHOULD ship BEFORE Wave beta-readiness-3 execution để surface any audit findings impact scope. Coordinator session sau verify audit shipped → execute wave.

---

## 8. Log

- **2026-05-24 (draft):** Wave plan drafted end-of-session theo user direction "draft wave tiếp theo để session sau thực hiện". Scope = 4 bucket follow-up Wave beta-readiness-2: A GAP-734 idempotency completion kitehub-subscription + B GAP-735 test flake fix + C GAP-732 authz test re-enable + D GAP-606/610/611 email beta-signup pipeline cluster. Outside-in audit skip per `outside-in-coverage-trigger.md` §4 (Wave br-1 V2 audit within 30-day window). Inside-out 3-source pull: ROADMAP §🚀 (4 follow-up gap GAP-732/733/734/735) + email cluster GAP-606/610/611 from Wave 90 walkthrough còn block beta invite pipeline. State-check execution deferred to coordinator next session per `audit-to-gap-pipeline.md` §2.8. Spawn sequence revised D-first (block A conflict potential trên BetaAccessController).
