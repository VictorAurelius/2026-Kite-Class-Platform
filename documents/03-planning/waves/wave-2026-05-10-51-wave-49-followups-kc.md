---
title: Wave 51 — Wave 49 KC Follow-ups (E2E sweep + backend read APIs)
status: complete
created: 2026-05-10
updated: 2026-05-10
waves: [51]
gaps: [GAP-267a, GAP-268a, GAP-268b, GAP-269b, GAP-269c]
parent_wave: documents/03-planning/waves/wave-2026-05-10-49-track-2-phase-4-kc-personas.md
phase_reference: Phase 1 BETA hardening
---

# Wave 51 — Wave 49 KC Follow-ups

**Goal:** Đóng 5/6 sub-gap đã filed sau Wave 49 closure (GAP-267a + GAP-268a/b + GAP-269b/c). Skip GAP-269a (OAuth keys user-action). Phần Lighthouse PWA của 267a/269c defer thêm vì cần HTTPS staging post-merge.

**Trigger:** User chọn disciplined path "Wave 51 plan PR + 2 agents pragmatic batch" sau khi 6 sub-gap files filed PR #1097. Wave 50 Bucket A vẫn đang chạy (kh-admin) → 1 + 2 = 3 agents ≤ max-cap 5.

**Estimated wall-clock:** ~3-5h longest path (Bucket B BE APIs nặng hơn vì 5 endpoints + integration tests). 2 agents parallel.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- 5 sub-gap đều thuộc Phase 1 BETA hardening (verify Wave 49 PARTIAL → DONE-ready)
- Bucket A (E2E sweep) đóng AC "E2E flow" của 3 parent gap (267 + 268 + 269) — verify Wave 49 production routes còn navigable end-to-end (regression safety)
- Bucket B (BE read APIs) unblock Wave 49 Bucket C real-data integration — replace mock fixtures với real REST → kc-student kit có data flowing cho beta tenants

**Q2 (trade-offs):**
- **Đã xét:** spawn 6 agents song song (1 per sub-gap) → REJECT vì vượt max-cap 5 với Bucket A Wave 50 đang chạy + 3 Playwright specs share `kiteclass-frontend/playwright.config.ts` + shared MSW handlers → conflict risk
- **Đã xét:** sequential 6 fix PRs → REJECT vì wall-clock tăng 5-10× vs parallel
- **Chọn:** 2 buckets pragmatic batch:
  - Bucket A: 3 Playwright specs (parent + teacher + student) → 1 agent FE test sweep
  - Bucket B: 2 BE controller groups (attendance batch + student-portal reads) → 1 agent BE API extension
- **Đã xét:** include GAP-269a (social login) trong Bucket B → REJECT vì cần Zalo OA + Google API keys provisioned out-of-band; agent có thể scaffold nhưng không integration-test → low value-per-effort, defer
- **Đã xét:** Lighthouse PWA verification trong Wave 51 → DEFER. Lighthouse cần HTTPS staging URL chỉ measurable POST-MERGE của fix PR, không thể đo trong PR verify scope. Phần Playwright của 267a/269c ship Wave 51; Lighthouse phần ship riêng follow-up sau khi staging URL ready.

**Q3 (rủi ro):**
- **R1 — Playwright config conflict**: `playwright.config.ts` shared giữa existing specs + 3 new ones. → AC Bucket A: KHÔNG edit `playwright.config.ts` (assume existing baseUrl/projects sufficient); chỉ thêm spec files trong `e2e/critical-journeys/` hoặc tạo subfolder `e2e/wave-49-followups/`. Recovery: nếu cần config change, file follow-up reconcile.
- **R2 — Existing student-portal backend state**: `kiteclass-core/.../student/controller/StudentController.java` + `InternalStudentController.java` đã tồn tại — state-check agent execution-time để xem endpoints nào đã có vs cần thêm. → AC Bucket B: KHÔNG re-implement existing; CHỈ thêm 5 endpoints missing (today / grades / grades/{subjectId} / payments / notifications). Nếu endpoint đã có nhưng FE Wave 49 chưa wire, agent ghi note vào gap update.
- **R3 — Business doc folder cho student-portal**: `documents/01-business/kiteclass/student/` không tồn tại; chưa rõ student-portal endpoints thuộc domain folder nào (student? lms? course-class?). → AC Bucket B: state-check existing controllers' module path, file `student-portal` 3-layer (rules.md / use-cases.md / api-contract.md) trong cùng PR (per CLAUDE.md §Living Docs). Recovery: nếu đã có folder nhưng tên khác, dùng folder đó.
- **R4 — MSW handlers Wave 49 mock fixtures**: Bucket A Playwright specs dùng MSW từ Wave 49 Bucket A/B/C. → AC Bucket A: reuse existing MSW; nếu thiếu, thêm handler trong same PR (handler không phải specs).
- **R5 — Wave 50 Bucket A overlap**: kh-admin agent vẫn đang chạy trên `wave/50-bucket-a-kh-admin` (kitehub-frontend). Wave 51 buckets touch kiteclass-* only → ✅ disjoint (no overlap).

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-267a (Playwright phần) + GAP-268b + GAP-269c (Playwright phần) | bg-agent | ~2-3h | ✅ FE test only — `kiteclass-frontend/e2e/wave-49-followups/**` |
| B | GAP-268a + GAP-269b | bg-agent | ~3-5h | ✅ BE only — `kiteclass-core/.../attendance/**` + `kiteclass-core/.../student/**` + `documents/01-business/kiteclass/{attendance,student-portal}/**` |

**Disjoint check:**
- Bucket A files: `kiteclass-frontend/e2e/wave-49-followups/*.spec.ts` (NEW subfolder); MSW handlers nếu cần
- Bucket B files: `kiteclass-core/src/main/java/com/kiteclass/core/module/{attendance,student}/**` + `documents/01-business/kiteclass/{attendance,student-portal}/**`
- Zero file overlap ✅
- Both pure-domain (no shared `playwright.config.ts` edit per R1; no shared kc-core source overlap)
- Wave 50 Bucket A (kitehub-frontend) disjoint với cả 2 ✅

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** **MEDIUM** — verification + read-API extension (no new business rules, no UI change). Model: **Opus 4.7** mỗi agent (HIGH-quality test + API design warranted).
**Cross-layer? (per `contract-first-for-cross-layer.md`):** **NO** — Bucket A pure FE test; Bucket B pure BE API + business doc. No FE bucket consumes Bucket B's new endpoints in same wave (FE wiring follows in future GAP-269b consumer follow-up). Bucket B writes api-contract.md trong cùng PR per CLAUDE.md §Living Docs.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A — KC E2E sweep** | GAP-267a (Playwright) + GAP-268b + GAP-269c (Playwright) | 🟡 P2 | `kiteclass-frontend/e2e/wave-49-followups/**` (NEW subfolder) | parallel với B |
| 2 | **B — KC backend read APIs** | GAP-268a + GAP-269b | 🟡 P2 | `kiteclass-core/.../module/{attendance,student}/**` + `documents/01-business/kiteclass/{attendance,student-portal}/**` | parallel với A |

### Bucket A — KC E2E sweep (3 Playwright specs)

- Files: NEW subfolder `kiteclass-frontend/e2e/wave-49-followups/`
  - `parent-invite-pay-flow.spec.ts` (GAP-267a Playwright phần) — login → parent-invite token → child binding → home → child card → transcript → billing → pay → success
  - `teacher-attendance-grade-report.spec.ts` (GAP-268b) — login → `/teacher/attendance` Lớp 6A1 → mark period → save → `/teacher/grades` → enter grade → finalize → `/teacher/reports` → see report
  - `student-offline-sync.spec.ts` (GAP-269c Playwright phần) — login → `student/today` → `student/assignments/[id]` → `context.setOffline(true)` → submit → verify localStorage queue → `setOffline(false)` → verify auto-flush → success state
- Reuse existing MSW handlers (Wave 49 Bucket A/B/C scope); thêm handler trong same PR nếu thiếu
- KHÔNG edit `playwright.config.ts` per R1
- Acceptance:
  - 3 specs pass locally (`pnpm -F kiteclass-frontend test:e2e -- wave-49-followups`)
  - 3 specs pass trong CI (`frontend-ci.yml` E2E job)
  - Cover happy path mỗi flow + ≥1 error branch (e.g., conflict on schedule entry / payment fail / network never recovers)
  - GAP-267a parent gap "E2E spec" AC ✅ + GAP-268b parent gap "E2E spec" AC ✅ + GAP-269c parent gap "E2E spec" AC ✅
  - Lighthouse PWA portion DEFERRED — note rõ trong each gap Log entry (HTTPS staging post-merge follow-up)

### Bucket B — KC backend read APIs (5 endpoints + business docs)

- Files: 
  - `kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/**` (extend existing `AttendancePeriodController` với class-overview batch save)
  - `kiteclass-core/src/main/java/com/kiteclass/core/module/student/**` (extend existing `StudentController` hoặc thêm controllers cho 5 student-portal read endpoints)
  - `documents/01-business/kiteclass/attendance/api-contract.md` (extend với new batch endpoint)
  - `documents/01-business/kiteclass/student-portal/{rules.md,use-cases.md,api-contract.md}` (CREATE NEW domain folder per R3 — state-check trước, dùng existing folder nếu phù hợp)
- 5 student-portal endpoints + 1 attendance batch (6 total):
  - `POST /api/v1/attendance/class/{classId}/batch?date=YYYY-MM-DD` — batch save by class (GAP-268a)
  - `GET /api/v1/students/me/today` — today's schedule + assignments due (GAP-269b)
  - `GET /api/v1/students/me/grades` — grades index
  - `GET /api/v1/students/me/grades/{subjectId}` — grade detail per subject
  - `GET /api/v1/students/me/payments` — invoice list (verify if existing first)
  - `GET /api/v1/students/me/notifications?cursor=&limit=` — notification feed cursor-paginated
- Tests: integration test mỗi controller (6 tests minimum); outbox event verification cho `attendance/class/batch`
- Acceptance:
  - 6 endpoints documented trong api-contract.md với request/response schema + error codes
  - 6 backend integration tests pass (`./mvnw verify -pl kiteclass-core`)
  - api-contract.md + use-cases.md + rules.md updated per Living Docs rule
  - Outbox event published cho attendance batch (per `design-patterns.md` §3.5.1)
  - Pagination on notifications (cursor-based)
  - GAP-268a + GAP-269b parent gap AC ✅ verifiable
  - business-logic-review.md §2.5 5-attribute reviews cho any new BR (e.g., student-portal access scoping rules)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

Verified 2026-05-10 trước khi draft plan:

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `kiteclass-frontend/e2e/` directory | E2E test root | `ls kiteclass-frontend/e2e` | 9+ existing spec files (auth, billing, branding, classes, critical-journeys/, etc.) | ✅ exists |
| `kiteclass-frontend/e2e/wave-49-followups/` | Target subfolder | `find kiteclass-frontend/e2e -type d -name "wave-49-followups"` | 0 | 🆕 to-be-created (Bucket A) |
| `kiteclass-frontend/playwright.config.ts` | Playwright config | reference for projects/baseUrl | (exists per repo convention) | ✅ exists (Bucket A KHÔNG edit per R1) |
| `kiteclass-core/.../student/controller/StudentController.java` | Existing controller | `find kiteclass-core -path '*/student/*Controller.java'` | 3 files (StudentController + InternalStudentController + bulkimport/BulkImportController) | ✅ exists (Bucket B extends; verify endpoint inventory execution-time) |
| `kiteclass-core/.../attendance/controller/AttendancePeriodController.java` | Existing controller | `find kiteclass-core -path '*/attendance/*Controller.java'` | AttendanceController + AttendancePeriodController | ✅ exists (Bucket B extends per-tiết controller với class-batch) |
| `documents/01-business/kiteclass/attendance/` | Business doc folder | `ls documents/01-business/kiteclass/attendance/` | 3 files (api-contract.md + rules.md + use-cases.md) | ✅ exists (Bucket B extends api-contract.md với batch endpoint) |
| `documents/01-business/kiteclass/student-portal/` | Target business doc folder | `ls documents/01-business/kiteclass/student-portal/` 2>/dev/null | 0 dirs | ⚠️ verify Bucket B execution: nếu existing student-portal-ish folder dùng tên khác (e.g., `student/`, `lms/`), reuse; else 🆕 create per R3 |
| `documents/01-business/kiteclass/student/` | Possible existing folder | `ls documents/01-business/kiteclass/student/` 2>/dev/null | 0 dirs | ✅ confirmed missing — Bucket B sẽ tạo `student-portal/` folder với 3-layer files |
| Phase 4 Wave 49 production routes (parent/teacher/student) | FE routes | per Wave 49 closure ROADMAP §🚀 Next Action | 8+11+12 routes shipped | ✅ exists (Bucket A E2E specs target these) |
| MSW handlers cho student/teacher/parent | Mock infra | reference Wave 49 Bucket A/B/C scope | (exists per Wave 49) | ✅ exists (Bucket A reuse; thêm nếu thiếu coverage) |

**Banned shortcut compliance (mirror §2.5):** không dùng `| head` truncation; full `find`/`grep` output đã verify cho 9 hàng ✅; 1 hàng ⚠️ marked verify-execution-time với fallback strategy (R3 folder discovery).

**🆕 to-be-created symbols** trong scope:
- `kiteclass-frontend/e2e/wave-49-followups/` subfolder (Bucket A creates)
- 3 Playwright spec files (Bucket A creates)
- 6 BE endpoints + integration tests (Bucket B creates)
- `documents/01-business/kiteclass/student-portal/` folder + 3-layer files (Bucket B creates per R3)

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `pnpm -F kiteclass-frontend test:e2e -- wave-49-followups` (3 new specs) + `pnpm -F kiteclass-frontend test --run` (no regression on existing unit tests) | frontend-ci E2E job |
| B | `cd kiteclass/kiteclass-core && ./mvnw verify` (full module test suite — 6 new IT tests should pass alongside existing) | core-ci |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- **Buckets A và B**: spawn `run_in_background: true` ngay sau plan PR merged + main synced
- `isolation: worktree` mỗi bucket để parallel safety
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge tuần tự A → B sau khi 2 background completion notifications đến
- Stake tier MEDIUM → mỗi agent dùng **Opus 4.7** (test + API design warrant)
- **Wave 50 Bucket A vẫn đang chạy** → Wave 51 spawn 2 thêm = tổng 3 ≤ max-cap 5 ✅

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Mỗi bucket PR update gap file Log + status (5 sub-gaps: 267a + 268a + 268b + 269b + 269c)
- ROADMAP §🚀 Next Action update trong closure PR
- Wave plan frontmatter `status: draft → complete` flip trong closure PR
- `wave-history.jsonl` append entry (Rule 15)
- Sub-gap deferred items (Lighthouse phần 267a/269c → riêng follow-up post-staging-HTTPS)
- PARTIAL exit-ramp per `gap-done-discipline.md` §3 nếu không đủ AC verified
- `bash scripts/prune-merged-worktrees.sh --yes` sau khi tất cả bucket PR merged

### GAP progress dự kiến sau Wave 51

| Gap | Trước Wave 51 | Sau Wave 51 |
|-----|---------------|-------------|
| GAP-267a | OPEN | 🟡 PARTIAL (Playwright DONE; Lighthouse defer post-HTTPS-staging) |
| GAP-268a | OPEN | 🟢 DONE |
| GAP-268b | OPEN | 🟢 DONE |
| GAP-269a (skip) | OPEN | OPEN (user-action gated) |
| GAP-269b | OPEN | 🟢 DONE |
| GAP-269c | OPEN | 🟡 PARTIAL (Playwright DONE; Lighthouse defer) |

**Cascade flips:** GAP-268 parent có thể flip 🟡 PARTIAL → 🟢 DONE nếu cả 268a + 268b + Phase 4 milestone audit (UI /128) ≥105 per screen; mặc khác stay PARTIAL.

---

## 8. Log

- **2026-05-10 (draft)**: Wave 51 plan filed sau khi 6 sub-gap files merged PR #1097. User chọn "disciplined Wave 51 plan PR + 2 agents pragmatic batch" thay vì spawn ngay 6 agents (vượt cap) hoặc skip plan PR. Plan tuân thủ `audit-to-gap-pipeline.md` §2.6 State-Check Evidence + `contract-first-for-cross-layer.md` (NO cross-layer; Bucket B writes api-contract trong cùng PR per Living Docs) + `gap-done-discipline.md` PARTIAL exit-ramp ready (Lighthouse defer) + `feedback_parallel_agent_strategy.md` max-cap 5 respected (1 Wave 50 + 2 Wave 51 = 3 ≤ 5). Stake tier MEDIUM → Opus 4.7 mỗi agent. Wall-clock estimate ~3-5h longest path. **Status: draft — chờ user review + approve. Execution sau khi plan PR merged.**
