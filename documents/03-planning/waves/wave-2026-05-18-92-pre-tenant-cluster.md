---
title: Wave 92 — Pre-Tenant Cluster + Audit Follow-ups
status: draft
created: 2026-05-18
updated: 2026-05-18
waves: [92]
gaps: [GAP-521, GAP-514, GAP-432, GAP-600, GAP-599]
---

# Wave 92 — Pre-Tenant Cluster + Audit Follow-ups

**Goal:** Hoàn thiện hardening Phase 1 BETA pre-tenant (admin audit log enrichment + auth resilience + cleanup beta_request abort) + đóng các meta long-term P2/P3 follow-ups (uptime monitoring + DR plan + AWS health dashboard) để chuẩn bị invite 3-5 beta tenant đầu tiên sau khi Wave 91 ship.

**Trigger:** Wave 91 ship 5/6 PRs offline (admin + email + signup unblocked); Coordinator F vẫn BLOCKED bởi GAP-612 AWS suspension. Wave 92 = scope offline-safe (mọi bucket KHÔNG cần AWS active) để spawn parallel song song khi AWS active hoặc trước. Pre-tenant cluster (GAP-525 / GAP-514 / GAP-524 / GAP-515 / GAP-521) đã shrink: GAP-524 + GAP-515 DONE (Wave 72b/78); GAP-525 PARTIAL 85% (user-action only, không Claude scope); GAP-514 PARTIAL 90% (live smoke gated Coordinator F); GAP-521 PARTIAL 70% real work còn lại. Cộng thêm sub-finding Wave 90 (admin /instances /payments /revenue 404) + Wave 90 backlog (GAP-600 abort cleanup + GAP-599 JWT tab collide) + queue Manual split + 3 long-term observability gaps.

**Estimated wall-clock:** ~6-8h agent work × 5 buckets parallel ≈ longest bucket ~2-3h. Coordinator merge + closure +30min.

**OFFLINE-SAFE design:** Mọi bucket touch code/docs/rules — KHÔNG cần AWS active. Live smoke deferred Wave 91 Coordinator F (GAP-514 live verify) hoặc Wave 93 nếu cần.

---

## 1. Brainstorm

### Q1: Inside-out + outside-in completeness per `inside-out-completeness-trigger.md` §3

**Inside-out source 1 — ROADMAP §🚀 Next Action (Wave 91 closed-out scope):**
- Pre-tenant cluster GAP-525/514/524/515/521 (queued kể từ Wave 87, defer mỗi Wave 88+) — RECHECK status:
  - GAP-524 DONE 2026-05-14 (Wave 72b Bucket E) — SKIP
  - GAP-515 DONE 2026-05-14 (Wave 78 Bucket C) — SKIP
  - GAP-525 PARTIAL 85% (15% = user execute rotation, không Claude scope) — defer (user-action)
  - GAP-514 PARTIAL 90% (1 AC live smoke gated Wave 91 Coordinator F deploy) — defer (Coordinator F)
  - GAP-521 PARTIAL 70% (real BE enrichment work còn lại) — INCLUDE Wave 92
- 3 long-term P2/P3 follow-ups (Wave 92 candidate): uptime monitoring external / DR plan / AWS health dashboard daily check — INCLUDE as Bucket E meta scope
- Sub-finding Wave 90: admin `/instances` `/payments` `/revenue` 404 (route map missing) — INCLUDE Wave 92 Bucket F
- GAP-601 ops-readiness audit deferred deadline 2026-05-20 — RECHECK: nếu parallel Agent A close = SKIP (avoid duplicate); nếu chưa = INCLUDE

**Inside-out source 2 — `documents/03-planning/inside-out-queue.md` (5 items):**
- Premium plan → Phase 1.5+ (n/a Wave 92)
- Feedback channel — consumed Wave 78 (n/a)
- Email content audit — consumed Wave 78 (n/a)
- User manual VN — consumed Wave 79 (n/a)
- **Manual split professional vs end-user** — Wave 88+ candidate, doc work, INCLUDE Wave 92 Bucket D (mảng rule-level — codify `professional-manual-content-standard.md` sister rule)

**Inside-out source 3 — CSV query `bash scripts/query-gaps.sh "" OPEN phase-1-beta` (29 OPEN + 30 PARTIAL):**
- GAP-599 P0 Frontend JWT tab collide — INCLUDE Bucket B (auth resilience pair với GAP-514)
- GAP-600 P1 Backend beta_request abort cleanup — INCLUDE Bucket C (cleanup pair với GAP-521 audit)
- GAP-432 P1 findAll bounded — INCLUDE Bucket B (BE hardening pair)
- GAP-481 P1 Gateway path routing 404 — sub-finding overlap? Cross-check Bucket F (admin endpoints 404) — KEEP separate (admin = controller missing, gateway = routing config)
- GAP-579/580 P1 (soft-delete + email idempotency) — defer Wave 93 (not pre-tenant blocking)
- GAP-595..598 P1/P2 landing CTA + invite mgmt + grade window — defer Wave 93 (post-tenant feedback driven)

**Outside-in decision per `outside-in-coverage-trigger.md`:**
- Wave 92 scope = **MIXED user-facing + backend hardening**. Bucket C (GAP-521 admin audit log + GAP-600 abort cleanup) = backend internal (PDPL angle nhưng không user-facing flow mới). Bucket F (admin endpoints 404) = admin-persona user-facing. Bucket D (Manual split rule) = doc governance.
- Per §4 exception row "Wave scope 100% internal / refactor / tech debt" + "User-facing scope user đã trải qua outside-in audit gần đây ≤ 30 ngày" — Wave 90 walkthrough (real persona Nguyễn Thùy Dương DG Edu 2026-05-17) là outside-in evidence áp dụng được cho admin scope (Bucket F). User manual scope (Bucket D) đã có Wave 79 Bucket F1 outside-in audit + user-manual-content-standard.md v1.0.0 → reuse.
- **DECISION: outside-in audit SKIPPED** với rationale documented. Backend Buckets A/B/C = internal hardening, no new user flow; Bucket D = extends existing rule pattern; Bucket E = meta-ops; Bucket F = admin-persona đã covered Wave 90 walkthrough. Outside-in spawn agents không add marginal coverage > cost.

### Q2: Trade-offs considered

- **Mega-bucket combining GAP-521 + GAP-600 (same admin_audit_logs table)** — REJECTED: 2 disjoint gaps với riêng AC; mega-bucket = harder review + slower merge. Keep separate.
- **Skip Bucket E meta long-term observability (defer Wave 93+)** — REJECTED: 3 follow-ups (uptime / DR / AWS health) đã queue ROADMAP từ Wave 90, file gaps now để materialize backlog + avoid silent loss per `gap-done-discipline.md` §3 PARTIAL exit ramp pattern.
- **Skip Bucket F admin endpoints 404 (defer until tenant onboarded)** — REJECTED: P1 blocker cho Platform Admin persona post-launch (3 missing endpoints = visible 404 trong admin nav); ship trước first beta tenant.
- **Spawn agents NOW (before AWS active)** — ACCEPTED: All 5 buckets offline-safe; spawn parallel song song với Wave 91 Coordinator F user-action wait (24-72h AWS reply).
- **Include GAP-601 ops-readiness audit** — DEFERRED decision: nếu parallel Agent A (working on GAP-601 close trong session này) ship trước Wave 92 plan PR mở → SKIP; nếu chưa → include Bucket E sub-item. Reviewer check at PR creation time.
- **Include Manual split queue item (Bucket D)** — ACCEPTED: doc-rule work, scope hợp lý ~2h, codify `professional-manual-content-standard.md` v1.0.0 sister rule. Defer execution của professional manual content cho Wave 93+.

### Q3: Risks + recovery

| Risk | Bucket | Recovery |
|---|---|---|
| Wave 91 Coordinator F deploy chưa run khi Wave 92 mở → GAP-514 live smoke conflict | B | Bucket B chỉ touch test code + config (no deploy needed); live smoke deferred Wave 91 F sequence — không overlap |
| Bucket A (GAP-521) + Bucket C (GAP-600) cùng touch `admin_audit_logs` table → merge conflict | A/C | Disjoint columns: A = enrich existing rows (extra metadata); C = new beta_request abort event rows. Different code paths (admin actions vs beta-signup). |
| Bucket E (3 long-term gaps) chỉ file gaps (no code) → low value | E | Material value = backlog visibility + ROADMAP entries; matches `audit-to-gap-pipeline.md` Step 5 mandate (every audit finding → gap → ROADMAP) |
| Bucket F (admin endpoints 404) overlap với GAP-481 (gateway routing 404) | F | Disjoint: GAP-481 = gateway route config; Bucket F = controller methods missing. Cross-reference but separate fix. |
| Bucket D Manual split rule conflicts với existing `user-manual-content-standard.md` | D | Sister rule pattern: extend §"Applies to" exclusion; mirror §2 15-item checklist for professional scope; cross-link `dev-readable-doc-language.md` §3 acceptable English (technical token broader cho professional audience) |
| GAP-601 duplicate work với parallel Agent A | E | Reviewer checks `documents/04-quality/gaps/GAP-601-*.md` Status field at Wave 92 PR creation; if DONE → drop từ Bucket E scope |
| Wave 92 scope creep (5 buckets → 7) | All | 5-bucket lean discipline; Manual split sub-rule (Bucket D) defer execution content; observability gaps (Bucket E) file only |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-521 (admin audit log enrich) | bg-agent (Sonnet) | ~120min | ✅ `kitehub-subscription` admin module + audit table enrichment |
| B | GAP-432 + GAP-599 (BE bounded findAll + FE JWT tab collide) | bg-agent (Opus) | ~150min | ✅ disjoint paths — BE service paginate + FE auth storage key namespacing |
| C | GAP-600 (beta_request abort cleanup) | bg-agent (Sonnet) | ~90min | ✅ `kitehub-subscription` beta module — cleanup scheduled job |
| D | Manual split rule + Bucket F (admin /instances /payments /revenue 404) | bg-agent (Sonnet) | ~120min | ⚠️ Mixed scope — rule + controller. Split nếu effort >2h: D1 = rule only, D2 = controllers |
| E | Meta long-term observability (3 P2/P3 follow-up gaps) + verify GAP-601 status | bg-agent (Sonnet) | ~45min | ✅ documents/04-quality/gaps/ only — file new gaps + ROADMAP update |

**Disjoint check:**
- A (admin_audit_logs enrich) ≠ C (beta_request_abort_log NEW table) — different domains
- B BE (findAll bound) ≠ B FE (JWT storage key) — different services + languages
- D rule (codify `professional-manual-content-standard.md`) ≠ D controllers (`AdminInstancesController/PaymentsController/RevenueController`) — different layers
- E gaps-only file — no code overlap
- All 5 buckets can spawn parallel

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM — Wave 92 = hardening + meta scope, không cross-cutting architecture. Bucket B (Opus) bao gồm Performance + Security multi-language coordination → cao hơn. Buckets A/C/D/E → Sonnet đủ.

**Cross-layer? (per `contract-first-for-cross-layer.md` §2):** NO — Bucket B FE/BE chạm different services + ZERO endpoint contract changes (FE = client-side storage key refactor; BE = repository layer paginate). Bucket F admin controllers consume existing `api-contract.md` from Wave 79+ admin scope. Skip Bucket 0 Foundation.

> **Gap referencing convention** (per `.claude/rules/gap-architecture-v2.md`): use canonical id từ `gap-status.csv`. Verify gap state via `bash scripts/query-gaps.sh <prefix>` trước khi bucket agents start.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** Admin audit log enrich | GAP-521 | 🟠 P1 | `kitehub-subscription/.../admin/audit/` + Flyway migration | parallel batch 1 |
| 2 | **B** BE findAll bounded + FE JWT tab collide | GAP-432 + GAP-599 | 🟠 P1 + 🔴 P0 | `kitehub-{subscription,branding,admin}/.../repository/` + `kitehub-frontend/src/lib/auth/` | parallel batch 1 |
| 3 | **C** beta_request abort cleanup | GAP-600 | 🟠 P1 | `kitehub-subscription/.../beta/scheduler/` + Flyway migration | parallel batch 1 |
| 4 | **D** Manual split rule + admin endpoints 404 | Manual split queue item + admin sub-finding | 🟠 P1 | `.claude/rules/professional-manual-content-standard.md` (NEW) + `kitehub-admin/.../controller/` | parallel batch 1 |
| 5 | **E** Meta observability backlog gaps + GAP-601 verify | 3 NEW gaps + GAP-601 status check | 🟡 P2 | `documents/04-quality/gaps/GAP-XXX-*.md` + ROADMAP | parallel batch 1 |

### Bucket A — Admin Audit Log Enrichment (GAP-521)

- Files:
  - EDIT: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/admin/audit/AdminAuditLogService.java` — add enrichment fields per GAP-521 §"Proposed Fix Phase 2" (request_id, target_resource_type, target_resource_id, before_state JSONB, after_state JSONB)
  - NEW Flyway: `kitehub/kitehub-subscription/src/main/resources/db/migration/V61__admin_audit_log_enrichment.sql` — ALTER TABLE add columns + index
  - EDIT: existing admin action sites (BetaAccessService.approve/reject, password reset endpoints) — populate enrichment fields
  - NEW: `kitehub-subscription/src/test/.../AdminAuditLogEnrichmentTest.java`
- Pre-implementation state-check (mandatory in agent prompt):
  ```bash
  # Verify GAP-521 Wave 72a Bucket B baseline
  grep -rn "AdminAuditLogService\|admin_audit_logs" kitehub/kitehub-subscription/src/main --include="*.java"
  # Existing Flyway migration tier
  ls kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql | tail -10
  ```
- Tests: integration test verify enrichment fields persist + JSONB query lookup works
- Acceptance:
  - [ ] V61 migration ships + adds 4 enrichment columns
  - [ ] Existing admin action sites populate enrichment fields
  - [ ] Integration test pass (enrichment + JSONB query)
  - [ ] `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` pass

### Bucket B — BE findAll Bounded + FE JWT Tab Collide (GAP-432 + GAP-599)

- Files:
  - EDIT BE: 3 service `findAll()` sites per GAP-432 §"Proposed Fix" — convert to Pageable
  - NEW BE tests: pagination boundary tests
  - EDIT FE: `kitehub-frontend/src/lib/auth/jwt-storage.ts` (or equivalent) — add tab-scoped storage key per GAP-599 §"Proposed Fix"
  - NEW FE tests: 2-tab simulation test (Playwright OR jsdom multi-window)
- Pre-implementation state-check:
  ```bash
  # Locate unbounded findAll
  grep -rn "findAll()" kitehub/{kitehub-subscription,kitehub-admin,kitehub-branding}/src/main --include="*.java"
  # FE JWT storage current pattern
  grep -rn "localStorage\.setItem\|jwt\|access_token" kitehub/kitehub-frontend/src/lib --include="*.ts" --include="*.tsx"
  ```
- Tests:
  - BE: pagination test asserts `Pageable` respected + default page size sensible (≤50)
  - FE: open 2 tabs different domain → JWT keys không collide; logout 1 tab không affect tab khác
- Acceptance:
  - [ ] 3 service findAll → Pageable shipped
  - [ ] FE JWT storage key tab-scoped (sessionStorage hoặc namespaced localStorage)
  - [ ] BE pagination tests pass
  - [ ] FE 2-tab simulation test pass
  - [ ] `cd kitehub && ./mvnw verify -P strict-warnings` + `cd kitehub-frontend && pnpm test --run && pnpm build` pass

### Bucket C — beta_request Abort Cleanup (GAP-600)

- Files:
  - NEW: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/scheduler/BetaRequestAbortCleanupScheduler.java` — `@Scheduled` job sweep stale PENDING beta_request rows
  - NEW Flyway: `kitehub/kitehub-subscription/src/main/resources/db/migration/V62__beta_request_abort_cleanup_index.sql` — index for cleanup query performance
  - EDIT: `application.yml` — `beta.cleanup.stale-threshold-hours: 24` + `beta.cleanup.poll-cron: "0 0 */6 * * *"`
  - NEW: integration test
- Pre-implementation state-check:
  ```bash
  # GAP-600 problem references
  grep -rn "beta_access_request\|BetaAccessRequest" kitehub/kitehub-subscription/src/main --include="*.java"
  # Existing scheduler patterns
  grep -rn "@Scheduled" kitehub/kitehub-subscription/src/main --include="*.java"
  ```
- Tests: integration test verify scheduled job picks up rows > threshold + marks ABORTED status (NOT delete — preserve audit trail)
- Acceptance:
  - [ ] Scheduler ships với configurable cron + threshold
  - [ ] V62 migration adds index for cleanup query
  - [ ] Integration test verifies stale row → ABORTED status
  - [ ] Unique-constraint regression test (re-submit allowed after abort)
  - [ ] `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` pass

### Bucket D — Manual Split Rule + Admin Endpoints 404 (queue item + Wave 90 sub-finding)

- Files:
  - NEW: `.claude/rules/professional-manual-content-standard.md` v1.0.0 — sister rule cho `user-manual-content-standard.md` per Manual split queue item 2026-05-17
  - EDIT: `.claude/rules/rules-index.csv` — add new rule row
  - EDIT: `.claude/rules/output-review-mandate.md` §3 — add row "Professional manual content"
  - EDIT: `documents/03-planning/inside-out-queue.md` — mark Manual split item `status: consumed (Wave 92 Bucket D)`
  - NEW: `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/AdminInstancesController.java` (or equivalent — locate via state-check)
  - NEW: `AdminPaymentsController.java` + `AdminRevenueController.java` (3 controllers cho 3 endpoints 404)
  - NEW tests: 3 controller endpoint smoke tests
- Pre-implementation state-check:
  ```bash
  # Verify admin endpoints currently 404 (sub-finding Wave 90)
  grep -rn "instances\|payments\|revenue" kitehub/kitehub-admin/src/main --include="*.java"
  # Existing admin controller pattern
  ls kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/
  # user-manual-content-standard.md baseline
  cat .claude/rules/user-manual-content-standard.md | head -50
  ```
- Tests: smoke `curl /api/v1/admin/{instances,payments,revenue} → 200 (not 404)` post-deploy
- Acceptance:
  - [ ] Rule `professional-manual-content-standard.md` v1.0.0 ships với §1-9 sections + worked self-test
  - [ ] `rules-index.csv` row added
  - [ ] `output-review-mandate.md` §3 matrix row added
  - [ ] Queue file `inside-out-queue.md` Manual split item marked consumed
  - [ ] 3 admin controller stubs ship với basic GET endpoints (paginated list, summary stats)
  - [ ] 3 controller smoke tests pass

### Bucket E — Meta Long-term Observability Backlog (3 NEW gaps + GAP-601 verify)

- Files:
  - VERIFY: `documents/04-quality/gaps/GAP-601-wave-88-ops-readiness-audit-deferred.md` Status (if DONE by parallel Agent A → SKIP; else INCLUDE follow-up)
  - NEW: `documents/04-quality/gaps/GAP-614-uptime-monitoring-external.md` — UptimeRobot / BetterStack integration P2
  - NEW: `documents/04-quality/gaps/GAP-615-disaster-recovery-plan.md` — multi-region OR backup mechanism + RTO/RPO targets P3
  - NEW: `documents/04-quality/gaps/GAP-616-aws-health-dashboard-daily-check.md` — automated AWS Service Health Dashboard scrape + alert P2
  - EDIT: `documents/04-quality/gaps/gap-status.csv` — add 3 NEW rows per `gap-architecture-v2.md` §3
  - EDIT: `documents/04-quality/gaps/ROADMAP.md` — add 3 gaps under "Long-term P2/P3 backlog" section
- Pre-implementation state-check:
  ```bash
  # Verify GAP-601 status (skip if DONE)
  bash scripts/query-gaps.sh GAP-601
  # Check for duplicate gap titles before filing
  grep -rli "uptime\|disaster recovery\|aws health" documents/04-quality/gaps/
  ```
- Tests: N/A (docs-only)
- Acceptance:
  - [ ] 3 NEW gap files ship với full template (Problem / Root Cause / Proposed Fix / AC / Related)
  - [ ] `gap-status.csv` 3 rows added
  - [ ] ROADMAP updated với long-term backlog section
  - [ ] GAP-601 status verified (DONE → drop scope; OPEN/PARTIAL → file follow-up)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-521-admin-audit-log.md` Status | Gap file | `bash scripts/query-gaps.sh GAP-521` | PARTIAL 70% (Wave 72a Bucket B) | ✅ exists, work remaining |
| `GAP-432` | Gap file | `bash scripts/query-gaps.sh GAP-432` | PARTIAL 50% | ✅ exists |
| `GAP-599` | Gap file | `bash scripts/query-gaps.sh GAP-599` | OPEN P0 | ✅ exists |
| `GAP-600` | Gap file | `bash scripts/query-gaps.sh GAP-600` | OPEN P1 | ✅ exists |
| `GAP-524 + GAP-515` | Gap files | `bash scripts/query-gaps.sh GAP-524; bash scripts/query-gaps.sh GAP-515` | DONE 2026-05-14 (Wave 72b + 78) | ✅ confirmed DROP từ scope |
| `GAP-525` Status | Gap file | `bash scripts/query-gaps.sh GAP-525` | PARTIAL 85% — user-action remaining | ✅ confirmed DROP từ scope (out-of-Claude) |
| `GAP-514` Status | Gap file | `bash scripts/query-gaps.sh GAP-514` | PARTIAL 90% — live smoke gated Wave 91 F | ✅ confirmed DROP từ scope (gated Coordinator F) |
| `GAP-601` Status | Gap file | `bash scripts/query-gaps.sh GAP-601` | PARTIAL P2 (verify-at-spawn — may be DONE by parallel Agent A) | ⚠️ verify-at-spawn (Bucket E) |
| `AdminAuditLogService` | Java class | `grep -rn "AdminAuditLogService" kitehub/kitehub-subscription/src/main --include="*.java"` | per Wave 72a Bucket B PR #1287 | ✅ exists (Bucket A extends) |
| `admin_audit_logs` table | DB | `grep -rn "admin_audit_logs\|create table.*audit" kitehub/kitehub-subscription/src/main/resources/db/migration/*.sql` | verify-at-spawn | ⚠️ verify-at-spawn (Bucket A) |
| `BetaAccessService` | Java class | `grep -rn "class BetaAccessService" kitehub/kitehub-subscription/src/main --include="*.java"` | per Wave 91 Bucket D | ✅ exists (Bucket C extends) |
| `kitehub-admin/.../controller/` | Java module | `ls kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/ 2>/dev/null` | verify-at-spawn — module may not exist | ⚠️ verify-at-spawn (Bucket D) |
| `professional-manual-content-standard.md` | Rule file | `ls .claude/rules/professional-manual-content-standard.md` | 0 results | 🆕 to-be-created (Bucket D) |
| `user-manual-content-standard.md` v1.0.0 | Rule file | `ls .claude/rules/user-manual-content-standard.md` | exists | ✅ exists (Bucket D references for sister-rule pattern) |
| `jwt-storage.ts` FE auth | TypeScript | `grep -rn "jwt\|access_token\|sessionStorage\|localStorage" kitehub/kitehub-frontend/src/lib/auth/ 2>/dev/null` | verify-at-spawn | ⚠️ verify-at-spawn (Bucket B) |
| 3 `findAll()` services | Java | `grep -rn "findAll()" kitehub/{kitehub-subscription,kitehub-admin,kitehub-branding}/src/main --include="*.java"` | per GAP-432 audit Wave 5 | ✅ exists (Bucket B refactor) |
| Manual split queue item | Queue file | `grep -A 5 "Manual split" documents/03-planning/inside-out-queue.md` | exists status=queued 2026-05-17 | ✅ exists (Bucket D consumes) |

**Banned shortcuts:** `| head` truncation; skipping verify-at-spawn entries (Bucket A/B/D/E); aspirational refs without 🆕 flag.

**Verify-at-spawn:** Bucket A/B/D/E agents PHẢI run grep/ls commands listed trước khi propose changes. GAP-601 status check trong Bucket E PHẢI run đầu tiên (skip-or-include decision).

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` | core-ci |
| B | `cd kitehub && ./mvnw -pl kitehub-subscription,kitehub-admin,kitehub-branding verify -P strict-warnings` + `cd kitehub/kitehub-frontend && pnpm test --run jwt-storage && pnpm build` | core-ci + frontend-ci |
| C | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` | core-ci |
| D | `cd kitehub && ./mvnw -pl kitehub-admin verify -P strict-warnings` (controllers) + `bash scripts/check-rule-frontmatter.sh` (rule file) + `bash scripts/check-rules-index-csv.sh` | core-ci + script-quality |
| E | `bash scripts/check-gap-status-csv.sh` (3 new rows valid) + manual ROADMAP review | script-quality |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

**Batch 1 (parallel, spawn AFTER this plan PR merge):** A + B + C + D + E — 5 agents `run_in_background: true`, `isolation: worktree`, RELATIVE paths per `feedback_worktree_absolute_path_contamination.md`.

⚠️ **Spawn timing:** OK to spawn all 5 agents BEFORE Wave 91 Coordinator F completes (offline-safe scope, không cần AWS). Coordinator merge sequence: A → B → C → D → E (any order safe — disjoint paths).

Coordinator F không apply tới Wave 92 (chỉ apply Wave 91 deploy + live verify). Wave 92 closure = merge all + ROADMAP sync + wave-history append + prune worktrees.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md` + `post-merge-sync-completeness.md`:

- [ ] All 5 buckets merged
- [ ] GAP-521 flip DONE (per `gap-done-discipline.md` §2 — all AC checked + integration test verified)
- [ ] GAP-432 + GAP-599 flip DONE (Bucket B)
- [ ] GAP-600 flip DONE (Bucket C)
- [ ] GAP-614/615/616 filed PLANNED (Bucket E new gaps remain OPEN until Wave 93+ fix)
- [ ] GAP-601 status sync (DONE flip if Bucket E verified Agent A closed, else PARTIAL update)
- [ ] Manual split queue item `inside-out-queue.md` marked consumed
- [ ] `gap-status.csv` rows updated per `post-merge-sync-completeness.md` §2 target 1
- [ ] ROADMAP §🚀 Next Action updated (Wave 92 SHIPPED + Wave 93 backlog: GAP-579/580 soft-delete + email idempotency + landing CTA + invite mgmt)
- [ ] Wave plan frontmatter `status: complete` flip
- [ ] `wave-history.jsonl` append
- [ ] `bash scripts/prune-merged-worktrees.sh --yes` clean
- [ ] **`## Release Plan Progress` section in closure PR body** — current Phase 1 BETA % progress + Wave 92 contribution + Waves Remaining table (strict-min v0.9.0-beta / practical v0.9.0-beta / v1.0.0 PROD)
- [ ] Handoff message: "Wave 92 ✅ ship. Pre-tenant hardening + meta observability backlog filed. Beta cohort invite ready post Wave 91 Coordinator F."

---

## 8. Log

- **2026-05-18 (draft):** Wave 92 plan drafted by background Agent B parallel với Coordinator F BLOCKED (GAP-612 AWS suspension). Scope locked via 3-source inside-out audit per `inside-out-completeness-trigger.md` §3: ROADMAP (pre-tenant cluster recheck — 2 DONE drop / 2 PARTIAL defer / 1 PARTIAL include + 3 long-term observability + admin 404 sub-finding) + inside-out-queue.md (Manual split queue item 5th) + CSV phase-1-beta non-DONE (GAP-432/599/600/481 cross-check). Outside-in audit SKIPPED per `outside-in-coverage-trigger.md` §4 exception — Wave 92 = mixed backend hardening + meta scope; admin user-facing (Bucket F) covered by Wave 90 walkthrough outside-in evidence (≤30 ngày). 5-bucket lean (A admin audit + B BE/FE auth + C beta cleanup + D rule+admin404 + E meta gaps); Bucket E GAP-601 verify-at-spawn để avoid duplicate với parallel Agent A. Cross-layer check: NOT cross-layer (no new endpoint contract). Concurrent ops check: zero AWS mutation in Wave 92 — no serialization needed. Offline-safe design: all buckets touch code/docs/rules only (Wave 91 Coordinator F unaffected).
