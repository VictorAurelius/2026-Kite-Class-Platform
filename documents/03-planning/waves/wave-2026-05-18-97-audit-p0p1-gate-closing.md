---
title: Wave 97 — Audit P0+P1 gate-closing (Wave 94c follow-up)
status: draft
created: 2026-05-18
updated: 2026-05-18
waves: [97]
gaps: [GAP-637, GAP-638, GAP-639, GAP-640, GAP-642, GAP-644]
---

# Wave 97 — Audit P0+P1 gate-closing (Wave 94c follow-up)

**Goal:** Fix 6 audit-surfaced gaps từ Wave 94c (API/Business/Security audit suite) để đẩy Phase 1 BETA gate scores: API contract 79→≥85, Business logic 70→78+, lock Security 93 + close audit P0 admin auth hole (OWASP A01).

**Trigger:** Wave 94c GAP-619 audit suite shipped 2026-05-18 với 3 audit findings categorize-able thành 1 wave gate-closing cluster (1 P0 + 4 P1 + 1 P2 disjoint scope).

**Estimated wall-clock:** ~3-4h agent work, longest-bucket ~75min (Bucket C 3-layer docs creation).

---

## 1. Brainstorm (5-10 min)

**Q1 Alignment:** Phục vụ Phase 1 BETA gate trigger criteria (per CLAUDE.md): Quality audit /100 ≥80 + 0 P0 incidents 2 tuần. Wave 94c findings: API 79 FAIL + Business 70 + 3 P2 security findings cần close trước beta tenant invite. Personas: Platform Admin (Mai) — admin v1 controllers security exposure; Solo Dev — Living Docs sync rule enforcement (GAP-640 META P1 force-multiplier).

**Inside-out sources consulted (per `inside-out-completeness-trigger.md` §3):**
- **Source 1 ROADMAP §🚀:** Phase 1 BETA P0 active 46 (25 PARTIAL); audit findings priority queue.
- **Source 2 `inside-out-queue.md`:** Premium plan / Feedback channel / Email content / OCR / QR — all Phase 1.5+ scope không relevant Wave 97.
- **Source 3 `query-gaps.sh P0/P1 phase-1-beta`:** 6 chosen gaps tất cả mới filed 2026-05-18 từ Wave 94c audit; no canonical drift.
- **Source 4 outside-in:** Wave 94c (today) đã chạy outside-in audit 5 categories — `outside-in-coverage-trigger.md` §4 exception "≤30 ngày" áp dụng, skip re-audit.

**Q2 Trade-offs:**
- 4 buckets disjoint scope = 4 parallel background agents (~30-75min each) thay vì serial (~4h+)
- Alternative considered: bundle GAP-641 Admin Revenue scaffold (Wave 35 carry P1) — REJECTED, scope nằm trong Bucket B (api-contract) sẽ surface lại; defer Wave 98+
- Alternative considered: include GAP-643 sessionStorage XSS — REJECTED, phase-1.5-paid scope theo CSV; Phase 1 BETA gate không cần
- Cross-layer check: tất cả 4 buckets backend-only hoặc docs-only → KHÔNG cần Bucket 0 Foundation per `contract-first-for-cross-layer.md` (FE consumer không tồn tại cho admin endpoints — FE admin UI Wave 92 scaffold-only)

**Q3 Risks:**
- Bucket A `@PreAuthorize` annotation có thể fail existing tests nếu test fixtures thiếu role mock → MockMvc test suite phải sửa song song
- Bucket B api-contract.md mới tạo có thể drift với typed DTOs nếu Bucket A controllers change signature mid-wave → spawn order Bucket A trước (1 turn), sau đó B/C/D parallel
- Bucket C admin-audit domain 3-layer docs hoàn toàn missing → cost cao hơn ước tính (75min); fallback: ship rules.md + use-cases.md đủ, api-contract.md defer Wave 98 nếu blocked
- Bucket D V54 JSONB IT cần Testcontainers Postgres → kiểm tra dev có Docker chạy; CI pipeline OK theo `postgres-specific-type-testcontainers.md` rule

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-637 P0 admin v1 @PreAuthorize + 403 tests | bg-agent | ~45min | ✅ kitehub-admin/controller/ |
| B | GAP-638 P1 admin v1 api-contract.md + typed DTOs | bg-agent | ~60min | ✅ documents/01-business/kitehub/admin/ + kitehub-admin/dto/ |
| C | GAP-639 + GAP-640 P1 Living Docs sync | bg-agent | ~75min | ✅ documents/01-business/kitehub/{beta-access,admin-audit}/ |
| D | GAP-642 P1 V54 JSONB IT + GAP-644 P2 scheduler drift metric | bg-agent | ~45min | ✅ kitehub-subscription/src/test/ + .../scheduler/ |

Disjoint check: 4 buckets không cross-touch file nào. Bucket A controllers ≠ Bucket B docs ≠ Bucket C rules.md ≠ Bucket D scheduler/IT.

---

## 3. Scope (compact schema)

**Stake tier:** MEDIUM → model Sonnet 4.6 cho 4 bg-agents (audit-driven fixes, scope rõ ràng, không exploratory).
**Cross-layer?** NO → skip Bucket 0 Foundation. FE admin UI Wave 92 scaffold-only, không consume admin endpoints chính thức.

> Gap referencing convention per `gap-architecture-v2.md` §3 — canonical ids verified via `bash scripts/query-gaps.sh GAP-637` (etc.) trước khi reference.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-637 | 🔴 P0 | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/Admin{Instances,Payments,Revenue}Controller.java` + `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/controller/Admin*ControllerSecurityTest.java` | FIRST (controllers stable trước khi B docs) |
| 2 | **B** | GAP-638 | 🟠 P1 | `documents/01-business/kitehub/admin/api-contract.md` (NEW 3-layer foundation) + `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/dto/` (typed DTOs) | parallel after A |
| 3 | **C** | GAP-639 + GAP-640 | 🟠 P1 (META) | `documents/01-business/kitehub/beta-access/rules.md` (edit ABORTED) + `documents/01-business/kitehub/admin-audit/{rules,use-cases,api-contract}.md` (NEW 3-layer) | parallel |
| 4 | **D** | GAP-642 + GAP-644 | 🟠 P1 + 🟡 P2 | `kitehub/kitehub-subscription/src/test/java/.../it/V54JsonbIntegrationTest.java` + `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/scheduler/BetaRequestAbortCleanupScheduler.java` (CloudWatch metric) | parallel |

### Bucket A — Admin v1 @PreAuthorize + 403 MockMvc tests (GAP-637 P0)

- Files: `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/Admin{Instances,Payments,Revenue}Controller.java`
- Add: class-level `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` annotation cho 3 controllers
- Tests new: `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/controller/Admin{Instances,Payments,Revenue}ControllerSecurityTest.java` — `@WebMvcTest` + `@WithMockUser(roles={...})` test 403 cho TENANT_USER + TEACHER roles
- Acceptance: 3 controllers có class-level annotation + 6 new test methods (2 per controller × 3 controllers) PASS + ABE existing tests PASS
- Verification: `cd kitehub && ./mvnw -pl kitehub-admin verify -P strict-warnings`

### Bucket B — Admin v1 api-contract.md + typed DTOs (GAP-638 P1)

- Files NEW: `documents/01-business/kitehub/admin/{rules,use-cases,api-contract}.md` (3-layer foundation per CLAUDE.md §Business Docs 3-Layer Structure)
- Document 6 endpoints surface (3 controllers × 2 methods avg): method + path + request/response shape + error codes + role requirement
- Files NEW: `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/dto/{Instance,Payment,Revenue}Dto.java` (typed records replacing Map<String,Object>)
- Files edit: 3 admin controllers refactor return type Map → DTO
- Acceptance: api-contract.md tồn tại với 6 endpoint sections + role explicit + 3 typed DTO files + 3 controllers return DTO + tests vẫn PASS
- Verification: `cd kitehub && ./mvnw -pl kitehub-admin verify`

### Bucket C — Living Docs sync (GAP-639 + GAP-640 P1 META)

- File edit: `documents/01-business/kitehub/beta-access/rules.md` — thêm `ABORTED` enum value vào danh sách BetaAccessRequestStatus + business rationale (Wave 92 introduced status nhưng rules.md chưa sync)
- Files NEW: `documents/01-business/kitehub/admin-audit/{rules,use-cases,api-contract}.md` (3-layer foundation cho admin_audit_log domain — Wave 92 V54 enrichment landed code, docs hoàn toàn missing)
- Acceptance: beta-access/rules.md có ABORTED row + 3 new files admin-audit/ tồn tại + each file 4 sections theo `docs-folder-structure.md` template + ROADMAP §🚀 reference Living Docs gap closure
- Verification: reviewer check sections complete; CI no new failure

### Bucket D — V54 JSONB IT + scheduler CloudWatch drift metric (GAP-642 P1 + GAP-644 P2)

- Files NEW: `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/admin/audit/V54JsonbIntegrationTest.java` — Testcontainers PostgreSQL IT cho 5 JSONB columns admin_audit_log V54 enrichment per `postgres-specific-type-testcontainers.md` §3
- File edit: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/scheduler/BetaRequestAbortCleanupScheduler.java` — thêm CloudWatch custom metric emit khi scheduler chạy 0 rows cleanup (silent failure detection)
- Acceptance: V54 IT PASS với Testcontainers PostgreSQL + scheduler metric emission verified via mock `MetricsCollector`
- Verification: `cd kitehub && ./mvnw -pl kitehub-subscription verify` (IT included via Failsafe plugin)

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

Verified 2026-05-18 via parallel grep/find/ls (no `| head` truncation per §2.5 hardened protocol):

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `AdminInstancesController` / `AdminPaymentsController` / `AdminRevenueController` | Java class | `find kitehub -name "Admin*Controller.java"` | 3 files in `kitehub-admin/src/main/java/com/kitehub/admin/controller/` | ✅ exists (path differs từ GAP-637 description — actual `/controller/` không phải `/api/v1/`; Bucket A agent dùng actual path) |
| `@PreAuthorize` annotation on 3 admin controllers | Spring Security | `grep -rn "@PreAuthorize" kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/` | 0 matches trên 3 admin controllers | 🆕 to-be-added (Bucket A) |
| `documents/01-business/kitehub/admin/api-contract.md` | API contract doc | `ls documents/01-business/kitehub/admin/api-contract.md` | not found | 🆕 to-be-created (Bucket B) |
| `documents/01-business/kitehub/beta-access/rules.md` | Living Doc | `ls documents/01-business/kitehub/beta-access/rules.md` | exists | ✅ exists (Bucket C edits ABORTED row) |
| `BetaAccessRequestStatus` enum + `ABORTED` value | Java enum | `grep -rn "ABORTED" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/entity/BetaAccessRequestStatus.java` | (enum file exists; ABORTED value Wave 92 introduced — verify trong Bucket C) | ✅ exists code-side (rules.md sync gap = Bucket C scope) |
| `documents/01-business/kitehub/admin-audit/` | 3-layer docs folder | `ls documents/01-business/kitehub/admin-audit/` | folder không tồn tại | 🆕 to-be-created (Bucket C — 3 files) |
| `V54__admin_audit_log_enrichment.sql` | Flyway migration | `find kitehub -name "V54*.sql"` | 1 file `kitehub-subscription/src/main/resources/db/migration/` | ✅ exists |
| `BetaRequestAbortCleanupScheduler` | Java class | `find kitehub -name "BetaRequestAbortCleanupScheduler.java"` | 1 file `kitehub-subscription/src/main/java/.../beta/scheduler/` | ✅ exists (Bucket D edits) |
| `V54JsonbIntegrationTest` | Java test | `find kitehub -name "V54Jsonb*"` | 0 matches | 🆕 to-be-created (Bucket D) |

No `| head` truncation. Aspirational references flagged 🆕 với owning bucket. GAP-637 path discrepancy noted (gap file says `api/v1/` actual is `controller/` — Bucket A agent dùng actual; coordinator update GAP-637 Current State section trong cùng PR).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-admin clean verify -P strict-warnings` | kitehub-ci |
| B | `cd kitehub && ./mvnw -pl kitehub-admin verify` + docs reviewer check 3-layer sections | kitehub-ci + docs reviewer |
| C | Reviewer check 4 sections per file + ABORTED row added + cross-link valid | docs reviewer only |
| D | `cd kitehub && ./mvnw -pl kitehub-subscription verify` (Failsafe IT included) | kitehub-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 4 buckets spawned với `run_in_background: true`
- Worktree isolation (`isolation: worktree`) cho parallel safety (per `feedback_parallel_agent_strategy.md`)
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Spawn order: **Bucket A trước (1 turn)** để controllers signature stable; **B/C/D parallel** sau Bucket A merge (B refactors A controllers return type)
- Coordinator merges sequentially A → B → C → D sau khi tất cả background completions

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `post-merge-sync-completeness.md` + `wave-closure-scope-completeness.md` + `post-wave-cleanup.md`:

- Each bucket PR updates affected GAP file Log + status + `gap-status.csv` row (per `post-merge-sync-completeness.md` Target 1)
- ROADMAP §🚀 Next Action updated trong closure PR (Target 2)
- Wave plan frontmatter `status: complete` flip trong closure PR
- `wave-history.jsonl` append trong closure PR (Target 3, Rule 15 enforcement)
- **Scope-Completeness Reconciliation table** trong closure PR body per `wave-closure-scope-completeness.md` §3 — mọi §3 item categorize ✅ DONE / 🟡 PARTIAL với gap link / ❌ NOT-IMPLEMENTED với follow-up link
- Sub-gaps filed cho any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- DONE flips: `git mv` từ `phase-1-beta/` → `phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0 §3.3
- Run `bash scripts/prune-merged-worktrees.sh --yes` sau khi tất cả bucket PRs merged
- `## Release Plan Progress` section trong closure PR body với Waves Remaining table

### Post-wave audit cadence (per `post-wave-audit-mandate.md` §2.2 — 3-day window)

Wave 97 touches `Controller.java` + `api-contract.md` + `rules.md` + Flyway IT → required audits within 3 ngày:
- API Contract /100 (target ≥85 post-fix vs 79 baseline)
- Business Logic /100 (target 78+ post Living Docs sync vs 70 baseline)
- Security /100 (regression check vs 93 baseline — GAP-637 fix should hold or improve)

Sub-wave 97b audit suite execution scheduled 2026-05-21 deadline.

---

## 8. Log

- **2026-05-18 (draft):** Plan created. Inside-out 4-source pull confirmed scope (Wave 94c follow-up). Outside-in skip per ≤30-day exception. State-check 9 symbols verified; GAP-637 path discrepancy flagged for inline correction. 4-bucket disjoint scope confirmed; spawn order A → B/C/D parallel.
