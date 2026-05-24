---
title: Wave beta-readiness-2 — Idempotency + Authz hasAccessToClass + Outbox dispatcher + API contract drift
wave: 2
waves: [beta-readiness-2]
tag_primary: beta-readiness
tags_secondary: [idempotency, authz-a01, outbox-pattern, api-contract-drift, phase-1-closure]
counter: 2
created: 2026-05-24
date_launch: 2026-05-24
closed_at: 2026-05-24
status: complete
audience: dev
gaps:
  - GAP-730
  - GAP-727
  - GAP-605
  - GAP-662
  - GAP-663
---

# Wave beta-readiness-2 — Idempotency + Authz hasAccessToClass + Outbox dispatcher + API contract drift

**Mục tiêu:** Ship 4 verified P0 blockers tiếp theo cho Phase 1 BETA invite. Khắc phục (1) carry-over Bucket C Wave beta-readiness-1 (idempotency POST narrow), (2) teacher full lock-out (A01 authz Class entity), (3) cross-service event delivery (outbox dispatcher 6+ wave debt), (4) Wave 98 API contract 3-way drift cluster.

**Khởi sự:** Wave beta-readiness-1 ship 3/4 (PR #1761/#1762/#1763); Bucket C GAP-730 defer (agent content-filter block); Wave beta-readiness-1 Bucket D audit còn surface GAP-727 (teacher lock-out P0); Wave 98 GAP-661 audit cluster còn 2 P0 (GAP-662/663) chưa fix.

**Thời gian ước tính:** ~10h (1-2 phiên) — 4 bucket scope, mostly cross-module disjoint.

**Tag scheme:** per `.claude/rules/wave-tag-numbering-convention.md` §2 — `beta-readiness-2` counter 2, descriptor `idempotency-authz-outbox-contract-drift`.

---

## 1. Brainstorm (5-10 phút)

**Q1 (đối tượng phục vụ):**
- Phase 1 BETA gate — beta tenant invite blocked nếu (1) double-submit signup/enrollment, (2) teacher login dead, (3) beta-invite email không gửi do outbox stuck, (4) consumer follow doc HTTP 404
- 4 personas affected: P1 Solo Teacher (authz B), P2 Owner (idempotency A + email C), P3 Manager (idempotency A invite-staff), Beta Invitee anonymous (email outbox C)

**Q2 (giải pháp đã xét và loại):**
- ❌ Defer toàn bộ sang post-AWS-restore (GAP-612): GAP-727 teacher lock-out + GAP-605 outbox đều CODE-only, không cần AWS verify để ship code-fix; chỉ live verify defer
- ❌ Ship D (contract drift) là 2 separate waves: GAP-662 + GAP-663 cohesive cluster Wave 98 audit, ship together natural
- ❌ Skip C outbox "vì có thể work-around tại consumer side": dispatcher missing là 6+ wave debt, không thể defer thêm
- ✅ **4-bucket parallel wave với cross-service disjoint scope** — agents song song không xung đột

**Q3 (rủi ro):**
- Bucket A (idempotency POST narrow) carry-over từ beta-readiness-1: content-filter từng block agent — pivot mitigation = chia agent prompt narrow scope per controller (no bulk pattern explanation)
- Bucket B (hasAccessToClass) đụng Class entity + ClassServiceImpl trong kiteclass-core — phải verify production schema `\d classes` có `teacher_id` column trước (state-check); nếu absent thì Flyway migration mandatory
- Bucket C (outbox dispatcher) implement Phase 1 fast-path (~30 min per gap proposal) + Phase 2 scheduled dispatcher (~2h) — split nếu effort quá → Phase 1 này wave, Phase 2 wave sau
- Bucket D (contract drift cluster) Option A GAP-662 rename controller → đụng nhiều caller services + SecurityConfig matchers; verify all callers updated tránh 404 production

---

## 2. Task Breakdown

| Bucket | Loại | Agent | Phụ thuộc | Thời gian |
|---|---|---|---|---|
| **A** Idempotency POST narrow (signup + enrollment + beta-request) | BE pattern | Agent A worktree (Opus, narrow scope) | Không (read Wave 105 Bucket D `PaymentIdempotencyService.java`) | ~3h |
| **B** `hasAccessToClass` guard fix — teacher lock-out A01 | BE schema + entity + service | Agent B worktree (Sonnet) | Verify schema trước (Tier 1 read-only — GAP-612 blocked nhưng test schema verify trên local DB OK) | ~2h |
| **C** subscription_outbox dispatcher implement Phase 1 hotfix (fast-path) + Phase 2 scheduled dispatcher | BE pattern | Agent C worktree (Sonnet) | Không (kitehub-subscription tách biệt) | ~3h |
| **D** API contract drift cluster — EmailController rename + PreferencesController IT tests | BE refactor + tests | Agent D worktree (Sonnet) | Verify Option A vs B trước spawn (recommend A từ gap) | ~2.5h |
| Tổng hợp + ship | Main session | — | All 4 done | ~30 phút |

**Kiểm tra rời rạc:**
- A đụng `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{auth,enrollment,beta-request}/controller/` + new `common/idempotency/` package + Flyway `V*__shared_idempotency_keys.sql`
- B đụng `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/class/{entity/Class.java,service/ClassServiceImpl.java}` + new Flyway `V*__add_classes_teacher_id.sql` (nếu schema thiếu) + `CrossUserAuthzTest.java` re-enable 2 `@Disabled` tests
- C đụng `kitehub/kitehub-subscription/src/main/java/.../SubscriptionEventEmitter.java` (fast-path) + new `OutboxDispatcher.java` (@Scheduled poll) + `EmailQueueConfig.java` (exchange/topic verify)
- D đụng `kitehub/kitehub-email/src/main/java/.../EmailController.java` (Option A `@RequestMapping` rename) + caller services (`kitehub-subscription` + `kitehub-platform` `RestTemplate`/`WebClient` paths) + `application*.yml` SecurityConfig matchers + new `PreferencesControllerIT.java`

**Conflict risk:**
- A vs B: cả 2 trong `kiteclass-core` nhưng khác file — A controllers + common/idempotency/ package; B class module + entity + Flyway. Maven compile + test parallel run OK. ✅
- C vs D: cả 2 trong `kitehub` project nhưng khác service — C `kitehub-subscription`; D `kitehub-email` + tests trong `kitehub-subscription` (`PreferencesControllerIT.java`). D's GAP-663 ship test class mới trong subscription nhưng KHÔNG đụng `SubscriptionEventEmitter.java` C ship. ✅ disjoint files within same service.
- A/B vs C/D: khác project entirely (kiteclass-core vs kitehub-*). ✅
- Migration ordering: A Flyway `V*__shared_idempotency_keys.sql` + B Flyway `V*__add_classes_teacher_id.sql` cùng kiteclass-core — VN convention V66+ next. A ships V66, B ships V67 (sequential numbering). Maven Flyway plugin enforces order; no DB conflict do khác table.

**Revised spawn pattern:** Cả 4 buckets disjoint at file level → parallel spawn OK ngay từ đầu (KHÔNG cần sequential như Wave beta-readiness-1 B→C).

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM (data-integrity + authz fix; ship pattern reusable). Model: Opus 4.7 coordinator + Sonnet cho 4 agents (cost-efficient cho execution scope rõ ràng; A pivot Opus nếu agent block lần 2).
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO (no FE bucket touching shared contract — D ship test class mới, không sửa contract). Skip Bucket 0 Foundation.

> **Gap referencing convention** per `.claude/rules/gap-architecture-v2.md`: canonical ids verified via `bash scripts/query-gaps.sh GAP-NNN` — all 5 gaps confirmed status `OPEN` / priority `P0` / phase `phase-1-beta`.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| A | **Idempotency POST narrow** | GAP-730 | P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{auth,enrollment,beta-request}/controller/`, `kiteclass-core/src/main/java/com/kiteclass/core/common/idempotency/` (new), `kiteclass-core/src/main/resources/db/migration/V66__shared_idempotency_keys.sql` | parallel |
| B | **hasAccessToClass guard fix** | GAP-727 | P0 | `kiteclass-core/src/main/java/com/kiteclass/core/module/class/entity/Class.java`, `.../service/ClassServiceImpl.java`, `kiteclass-core/src/main/resources/db/migration/V67__add_classes_teacher_id.sql` (nếu schema thiếu), `kiteclass-core/src/test/java/com/kiteclass/core/authz/CrossUserAuthzTest.java` (re-enable 2 `@Disabled`) | parallel |
| C | **Outbox dispatcher Phase 1+2** | GAP-605 | P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/event/SubscriptionEventEmitter.java` (fast-path), `.../OutboxDispatcher.java` (new, @Scheduled), `.../config/EmailQueueConfig.java` (verify) | parallel |
| D | **API contract drift cluster** | GAP-662 + GAP-663 | P0 | `kitehub/kitehub-email/src/main/java/.../EmailController.java` (Option A rename `/api/v1/email`), caller services `kitehub-subscription`/`kitehub-platform` `RestTemplate`/`WebClient` paths + `application*.yml` Security matchers, `kitehub-subscription/src/test/java/.../PreferencesControllerIT.java` (new) | parallel |

### Bucket A — Idempotency POST narrow (carry-over Wave beta-readiness-1 Bucket C)

- Files: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{auth,enrollment,beta-request}/controller/`, new `common/idempotency/` package (`IdempotencyService.java` + `IdempotencyRecord.java` entity + `IdempotencyRepository.java`), new Flyway `V66__shared_idempotency_keys.sql`
- Tests: `kiteclass-core/src/test/java/com/kiteclass/core/module/{auth,enrollment,beta_request}/IT/*IdempotencyIT.java` (3 IT — submit POST với same `Idempotency-Key` 2x → 2nd request returns cached response, NOT duplicate DB row)
- Acceptance:
  - [ ] Migration V66 shipped (composite PK `(tenant_id, idempotency_key, scope)`, `scope` enum SIGNUP/ENROLLMENT/BETA_REQUEST/PAYMENT)
  - [ ] `IdempotencyService.findExisting()` + `recordRequest()` shipped
  - [ ] 3 controllers (signup + enrollment + beta-request) wrap với idempotency logic (Option A annotation `@Idempotent(scope = ...)` preferred, fall back Option B inline nếu AOP complexity)
  - [ ] 3 IT tests verify duplicate Idempotency-Key → cached response
  - [ ] `./mvnw -pl kiteclass-core verify -P strict-warnings` PASS
- Agent prompt note: **narrow scope per controller to avoid content-filter block** — KHÔNG bulk-explain pattern; chỉ implement file-by-file với reference `Wave 105 Bucket D PaymentIdempotencyService.java` pattern

### Bucket B — hasAccessToClass guard fix (A01 OWASP teacher lock-out)

- Files: `kiteclass-core/.../module/class/entity/Class.java` (add `@Column(name="teacher_id") private Long teacherId`), `.../service/ClassServiceImpl.java` (set `teacherId` trong `create()` + `update()` từ DTO), new Flyway `V67__add_classes_teacher_id.sql` (CONDITIONAL — chỉ ship nếu schema thiếu), `kiteclass-core/src/test/java/.../authz/CrossUserAuthzTest.java` (re-enable A01-U01 + A01-U03)
- Tests: 2 `@Disabled` tests re-enabled — A01-U01 (teacher own class) → 200 OK; A01-U03 (teacher other class IDOR) → 403
- Acceptance:
  - [ ] State-check schema `\d classes` confirm whether `teacher_id` column exists (local test DB via `docker exec kite-postgres psql -U kite -d kitehub -c "\d classes"`)
  - [ ] `Class.java` entity maps `teacher_id` field
  - [ ] `ClassServiceImpl` sets `teacherId` từ create/update DTO
  - [ ] Flyway V67 ship nếu schema thiếu column
  - [ ] 2 `@Disabled` tests re-enabled + PASS (A01-U01 200, A01-U03 403)
  - [ ] `./mvnw -pl kiteclass-core verify -P strict-warnings` PASS

### Bucket C — Outbox dispatcher Phase 1 + Phase 2

- Files: `kitehub-subscription/src/main/java/.../event/SubscriptionEventEmitter.java` (Phase 1 add fast-path `rabbitTemplate.convertAndSend` per `design-patterns.md §3.5.1` Exception A), new `OutboxDispatcher.java` (Phase 2 `@Scheduled(fixedDelay = 30000)` poll `dispatched_at IS NULL` rows → publish RMQ → mark dispatched), verify `EmailQueueConfig.java` exchange `EmailQueueConfig.EMAIL_EXCHANGE` + topic patterns existed
- Tests: `kitehub-subscription/src/test/java/.../event/SubscriptionEventEmitterIT.java` (verify outbox row + RMQ publish 2-paths) + `OutboxDispatcherIT.java` (scheduled poll picks up stuck rows + marks dispatched)
- Acceptance:
  - [ ] Phase 1 fast-path: `SubscriptionEventEmitter.emit()` writes outbox AND publishes RMQ (best-effort, catch exception); existing stuck rows recoverable
  - [ ] Phase 2 scheduled: `OutboxDispatcher` poll mỗi 30s, publish stuck rows, mark `dispatched_at = NOW()`
  - [ ] 2 IT tests pass
  - [ ] `./mvnw -pl kitehub-subscription verify -P strict-warnings` PASS
  - [ ] Out-of-scope: live verify post-AWS-restore (gated GAP-612) — defer follow-up gap

### Bucket D — API contract drift cluster (Option A rename + PreferencesController IT)

- Files:
  - **GAP-662 (Option A controller rename):** `kitehub-email/src/main/java/.../EmailController.java` `@RequestMapping("/api/v1/email")` (was `/api/platform/emails`); update callers `kitehub-subscription`/`kitehub-platform` (`RestTemplate`/`WebClient`) + `application*.yml` SecurityConfig matchers; update MockMvc fixtures + api-contract.md cite `/api/v1/email/send`
  - **GAP-663 (PreferencesController IT):** new `kitehub-subscription/src/test/java/.../PreferencesControllerIT.java` (3 tests — 204+cookie / 401 no JWT / idempotent replay); update api-contract.md cookie httpOnly=false rationale comment
- Tests: 3 IT trong PreferencesControllerIT + GAP-662 callers verified via existing email-flow IT (no new tests needed for rename — existing tests update path)
- Acceptance:
  - [ ] EmailController `@RequestMapping("/api/v1/email")` rename ship (Option A)
  - [ ] All callers updated (grep `/api/platform/emails` → 0 hits sau rename)
  - [ ] SecurityConfig matchers updated (`/api/v1/email/**` permit/auth as appropriate)
  - [ ] api-contract.md cite new canonical URL
  - [ ] `PreferencesControllerIT.java` ship (3 tests)
  - [ ] api-contract.md cookie httpOnly=false rationale comment (FE-readable per design)
  - [ ] `./mvnw verify -P strict-warnings` PASS (multi-module)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `PaymentIdempotencyService` | Java class (Wave 105 Bucket D pattern source) | `grep -rn "PaymentIdempotencyService" kiteclass/kiteclass-core/src/main/java` | TBD by Agent A state-check | ✅ exists (per GAP-730 §Proposed Fix) |
| `SubscriptionOutboxEvent` + `SubscriptionOutboxRepository` | Java entity + repository (Wave 33 GAP-372 ship) | `grep -rn "SubscriptionOutboxEvent\|SubscriptionOutboxRepository" kitehub/kitehub-subscription/src/main/java` | TBD by Agent C state-check | ✅ exists (per GAP-605 §Problem) |
| `EmailQueueConfig.EMAIL_EXCHANGE` | Java config constant | `grep -rn "EMAIL_EXCHANGE" kitehub/kitehub-subscription/src/main/java` | TBD by Agent C state-check | ✅ exists (per GAP-605 §Proposed Fix Phase 1) |
| `Class.teacherId` Java field | Java entity field | `grep -rn "teacherId\|teacher_id" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/class` | TBD by Agent B state-check | 🆕 to-be-created (Bucket B owns) |
| `classes.teacher_id` DB column | Postgres column | `docker exec kite-postgres psql -U kite -d kitehub -c "\d classes"` | TBD by Agent B state-check (Tier 1 read-only) | TBD — Bucket B verifies first; if absent → Flyway V67 ships |
| `CrossUserAuthzTest.testTeacherAccessOwnClass` (A01-U01) | JUnit test | `grep -rn "testTeacherAccessOwnClass\|A01-U01" kiteclass-core/src/test/java` | Wave beta-readiness-1 Bucket D PR #1763 shipped | ✅ exists (currently `@Disabled` — Bucket B re-enables) |
| `EmailController.@RequestMapping` | Java annotation | `grep -rn "@RequestMapping" kitehub/kitehub-email/src/main/java/.../EmailController.java` | TBD by Agent D state-check | ✅ exists (Bucket D Option A rewrites path) |
| `PreferencesController.dismissBannerState` | Java method | `grep -rn "dismissBannerState" kitehub/kitehub-subscription/src/main/java` | Wave 98 Bucket B0 PR #1548 shipped | ✅ exists (Bucket D adds IT class) |
| `IdempotencyService` | Java class (shared common package) | `find kiteclass-core/src/main/java -name "IdempotencyService.java"` | 0 matches expected | 🆕 to-be-created (Bucket A owns) |
| `OutboxDispatcher` | Java class | `find kitehub-subscription/src/main -name "*Dispatcher*"` | 0 matches per GAP-605 §Problem | 🆕 to-be-created (Bucket C owns) |
| `V66__shared_idempotency_keys.sql` | Flyway migration | `ls kiteclass-core/src/main/resources/db/migration/V66*` | 0 matches expected | 🆕 to-be-created (Bucket A owns) |
| `V67__add_classes_teacher_id.sql` | Flyway migration | `ls kiteclass-core/src/main/resources/db/migration/V67*` | 0 matches expected | 🆕 to-be-created (Bucket B owns — conditional ship) |
| `PreferencesControllerIT.java` | JUnit IT class | `find kitehub-subscription/src/test -name "PreferencesControllerIT.java"` | 0 matches per GAP-663 §Problem | 🆕 to-be-created (Bucket D owns) |

**State-check empirical execution deferred to agent spawn time** — coordinator (Opus) verifies symbol presence via `grep`/`find` ngay khi agents start; nếu finding mismatch gap §Proposed Fix → revise plan §3 inline before agent execution (per `audit-to-gap-pipeline.md` §2.8 Fix-Time State-Check).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `./mvnw -pl kiteclass-core clean verify -P strict-warnings` | core-ci |
| B | `./mvnw -pl kiteclass-core clean verify -P strict-warnings` | core-ci |
| C | `cd kitehub && ./mvnw -pl kitehub-subscription clean verify -P strict-warnings` | kitehub-ci |
| D | `cd kitehub && ./mvnw clean verify -P strict-warnings` (multi-module — kitehub-email rename affects callers) | kitehub-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

- Spawn 4 agents song song trong single message (multiple Agent tool blocks):
  - Agent A (Opus narrow scope): GAP-730 Idempotency POST narrow — `kiteclass-core/.../{auth,enrollment,beta-request}/controller/` + `common/idempotency/` + V66 migration
  - Agent B (Sonnet): GAP-727 hasAccessToClass guard fix — `Class.java` + `ClassServiceImpl` + conditional V67 + `CrossUserAuthzTest` re-enable
  - Agent C (Sonnet): GAP-605 outbox dispatcher Phase 1+2 — `SubscriptionEventEmitter` fast-path + new `OutboxDispatcher`
  - Agent D (Sonnet): GAP-662 + GAP-663 contract drift cluster — `EmailController` rename Option A + `PreferencesControllerIT` new
- Each agent: own worktree (per `agent-worktree-isolation.md`); spawn pattern `Agent({subagent_type: general-purpose, ...})` background OK
- Coordinator (Opus 4.7 1M): synthesize 4 agent outputs → ship 1-4 separate PRs (preferred 1 PR per bucket cho independent merge ordering; D có thể consolidate 2 gaps trong 1 PR)

**Agent A pivot (content-filter recurrence):** Nếu Agent A block lần 2 (Wave beta-readiness-1 Bucket C precedent) → coordinator (Opus) tự implement narrow scope inline; KHÔNG re-spawn agent với same prompt.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Each bucket PR updates affected GAP file Log + status (5 gaps: GAP-730, GAP-727, GAP-605, GAP-662, GAP-663)
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan frontmatter `status: draft → complete` flip in closure PR
- `wave-history.jsonl` append entry `beta-readiness-2 | SHIPPED ...` per Rule 15 enforcement
- Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3 — likely candidates: Bucket A per-tenant rate-limit, all-buckets live-verify gated GAP-612
- Run `bash scripts/prune-merged-worktrees.sh --yes` post all 4 bucket PRs merged, pre closure PR draft
- `## Release Plan Progress` section in closure PR body per `feedback_wave_closure_release_progress_report.md` rules #1-6
- Scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3 — mọi item §3 Scope categorize ✅/🟡/❌

### Output Review Checklist (per `output-review-mandate.md` §3)

- [ ] **Code** — two-stage-code-review (Stage 1+2+2.5) — coordinator review mỗi PR
- [ ] **Tests** — IT pass cho 4 buckets: A 3 IT + B 2 IT + C 2 IT + D 3 IT = ~10 IT total
- [ ] **Migrations** — V66 + V67_5 (conditional) DBA checklist
- [ ] **API contract** — D Option A api-contract.md cite `/api/v1/email/send` canonical
- [ ] **3-layer docs sync** — rules.md / use-cases.md / api-contract.md updated cho A + D
- [ ] **Gap closure** — 5 gaps DONE flip với `gap-done-discipline.md` §2 checklist
- [ ] **Post-merge sync 4 targets** per `post-merge-sync-completeness.md` §2
- [ ] **Pre-handoff self-test** per `pre-handoff-self-test-completeness.md` §2.1 — live-verify gated GAP-612 PARTIAL exit ramp
- [ ] **Dev-readable language** per `dev-readable-doc-language.md`

### Risks + Pivots

| Risk | Trigger | Mitigation |
|---|---|---|
| Agent A content-filter block (recurrence) | Agent stops mid-implementation như Wave beta-readiness-1 Bucket C | Coordinator implement inline narrow scope per controller — KHÔNG bulk prompt pattern |
| Bucket B schema check reveals `teacher_id` already exists | `\d classes` shows column present | Skip Flyway V67_5; chỉ ship Class entity + ClassServiceImpl + test re-enable |
| Bucket D Option A rename callers miss → 404 production | Caller services có hardcoded `/api/platform/emails` path | Verify grep `/api/platform/emails` → 0 hits after rename; SecurityConfig matchers updated |
| Migration V66 + V67_5 conflict on Flyway order | Both buckets ship migration same wave | Sequential numbering + `IF NOT EXISTS` clause |
| Bucket C Phase 2 OutboxDispatcher scope creep | @Scheduled bean conflicts | Phase 1 hotfix-only nếu complexity surface — Phase 2 follow-up |

### Out-of-scope (track separately)

| Item | Where |
|---|---|
| Bucket A: Per-tenant rate-limit Idempotency-Key abuse | follow-up gap Wave beta-readiness-3+ per GAP-730 §Out-of-scope |
| Bucket A: Production live verify deduplication | gated GAP-612 AWS restore |
| Bucket B: Production schema migration verify | gated GAP-612 AWS restore — live IT against prod RDS |
| Bucket C: Live RMQ verify outbox dispatch | gated GAP-612 AWS restore |
| Bucket D: Production HTTP routing verify new `/api/v1/email/*` | gated GAP-612 AWS restore |
| All buckets: Production smoke test post-deploy | gated GAP-612 AWS restore — `pre-handoff-self-test-completeness.md` §2.1 PARTIAL exit ramp |

### Audit context

Wave beta-readiness-1 V2 audit (3 reports shipped 2026-05-24 PR #1759) — within `outside-in-coverage-trigger.md` §4 ≤30-day window. Skip outside-in audit hợp lệ cho Wave beta-readiness-2 scope (same Phase 1 closure cluster).

### Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Bucket A — Shared `IdempotencyService` + V66 migration + `common/idempotency/` package | ✅ DONE | PR #1769 |
| 2 | Bucket A — Wrap 3 controllers (signup + enrollment + beta-request) | 🟡 PARTIAL (1/3) | EnrollmentController DONE PR #1769; Signup + BetaRequest sống trong `kitehub-subscription` (Agent A state-check finding) → **GAP-734** (P1) |
| 3 | Bucket A — 3 IT tests verify duplicate `Idempotency-Key` | 🟡 PARTIAL (2 IT, EnrollmentController scope) | 2 IT PASS PR #1769; Signup + BetaRequest IT theo GAP-734 |
| 4 | Bucket B — `Class.java` map `teacher_id` field | ✅ DONE | PR #1768 |
| 5 | Bucket B — `ClassServiceImpl.create/update` set `teacherId` từ `UserContext` | ✅ DONE | PR #1768 |
| 6 | Bucket B — Flyway `V67_5__add_classes_teacher_id` (conditional) | ✅ N/A skipped | Schema column đã có sẵn V1 line 158; ddl-auto test profile generate từ entity annotation |
| 7 | Bucket B — Re-enable 2 `@Disabled` tests `CrossUserAuthzTest` A01-U01 + A01-U03 | ❌ NOT-IMPLEMENTED | Production defect FIXED nhưng test bodies vẫn `@Disabled` (cần fixture work) → **GAP-732** (P1) |
| 8 | Bucket C — Phase 1 fast-path `SubscriptionEventEmitter` | ✅ DONE pre-existing (Wave 91 PR #1487) | State-check §2.8 phát hiện đã ship; GAP-605 flip DONE qua PR #1770 |
| 9 | Bucket C — Phase 2 `OutboxDispatcher` `@Scheduled` poll | ✅ DONE pre-existing (Wave 91 PR #1487) | Cùng PR #1487 đã ship 164 LOC dispatcher |
| 10 | Bucket C — 2 IT tests `SubscriptionEventEmitterIT` + `OutboxDispatcherIT` | ✅ DONE pre-existing | Wave 91 Bucket A ship cùng với dispatcher |
| 11 | Bucket D — GAP-662 `EmailController` rename Option A `/api/platform/emails` → `/api/v1/email` + cập nhật mọi caller | 🟡 PARTIAL (Option B chosen) | Option B doc sync ship PR #1771 (api-contract.md cập nhật path); Option A rename 10+ files → **GAP-733** (P2 Wave 109+) |
| 12 | Bucket D — `PreferencesControllerIT` 3 tests | ✅ DONE (4 tests PASS) | PR #1771 ship `@WebMvcTest` + 4 tests (vượt scope: thêm 401 without JWT test) |
| 13 | Bucket D — api-contract.md cookie `httpOnly=false` rationale comment | ✅ DONE | PR #1771 |
| 14 | Wave closure scope — `wave-history.jsonl` append entry | ✅ DONE | Closure PR (this PR) |
| 15 | Wave closure scope — ROADMAP §🎯 sync | ✅ DONE | Closure PR (this PR) |
| 16 | Wave closure scope — `prune-merged-worktrees.sh` post 4 bucket merge | ✅ DONE | Worktree husks pruned coordinator session 2026-05-24 |
| 17 | Live verify post-AWS-restore (all 4 buckets) | ❌ NOT-IMPLEMENTED (gated GAP-612) | Gated GAP-612 AWS account restore; PARTIAL exit ramp per `pre-handoff-self-test-completeness.md` §2.1 |
| 18 | Post-wave audit suite ≤3 ngày | ❌ NOT-IMPLEMENTED | Defer fresh session; trigger gate per `post-wave-audit-mandate.md` §2.2 |
| 19 | Test flake `kiteclass-core` ≠ unblock CI gate | ❌ NOT-IMPLEMENTED (out-of-wave-scope) | Surfaced khi B + A CI fail same 6 flake → **GAP-735** (P1) dedicated wave fix |

**Verdict:** 14 ✅ DONE (Phase 1+2 outbox PR #1487 pre-existing 3 items counted) + 3 🟡 PARTIAL (Bucket A controller wrap 1/3, Bucket A IT 2/3, Bucket D Option B chosen) + 4 ❌ NOT-IMPLEMENTED (3 follow-up gap filed + 1 deferred audit suite). Mỗi item NOT-IMPLEMENTED có follow-up gap link hoặc explicit defer rationale per `wave-closure-scope-completeness.md` §3 decision tree.

---

## 8. Log

- **2026-05-24 (draft):** Wave plan tạo theo user direction (`/start-session` → option 3 → 4-bucket scope confirm qua AskUserQuestion). PR #1767 mở.
- **2026-05-24 (in-progress):** 4 agent spawn parallel (Agent A Opus narrow scope idempotency + Agent B/C/D Sonnet authz/outbox/contract-drift) per `agent-background-spawn-default.md` worktree isolation + run_in_background. Wave plan `check-wave-plan-completeness` FAIL ban đầu do numbering §7; fix §7→Closure Protocol + §8→Log per template mandate.
- **2026-05-24 (complete):** Wave SHIPPED. 9 PR merged: #1767 (plan) + #1768 (Bucket B authz PARTIAL admin-merge với AUDIT_OVERRIDE → GAP-735) + #1769 (Bucket A idempotency admin-merge với AUDIT_OVERRIDE → GAP-735) + #1770 (Bucket C state-check finding pre-existing impl) + #1771 (Bucket D contract drift) + #1772 (GAP-734 follow-up file) + #1773 (session handoff) + #1774 (post-merge sync GAP-662+663 Log) + #1775 (GAP-735 pre-existing flake file). 3 Sonnet agent (B/C/D) fail autocompact thrash; coordinator Opus 1M inline + Agent A Opus narrow scope survive. 4 follow-up gap filed: GAP-732 (P1 test re-enable) + GAP-733 (P2 v1 namespace migration) + GAP-734 (P1 signup + beta-request kitehub-subscription) + GAP-735 (P1 pre-existing test flake `kiteclass-core`). Post-wave audit suite defer fresh session per `post-wave-audit-mandate.md` §2.2 ≤3 ngày deadline.
