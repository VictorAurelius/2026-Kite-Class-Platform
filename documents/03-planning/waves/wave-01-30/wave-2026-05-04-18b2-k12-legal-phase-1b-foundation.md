---
title: Wave 18b2 — K-12 LEGAL Trio Phase 1B foundation wave-pack (GAP-321b + GAP-322b + GAP-323b mobile)
status: complete
created: 2026-05-04
updated: 2026-05-04
waves: [18b2]
gaps: [GAP-321b, GAP-322b, GAP-323b, GAP-347]
phase: 1B-foundation
expected_outputs: 1 plan PR (this) + 3 agent PRs (Phase 1B foundation per gap) + closure PR
actual_outputs: 6 PRs merged (#769 GAP-323b backend + #770 plan + #771 Bucket A mobile UI + #772 Bucket B vetting + #773 Bucket C parent facets + closure PR this) + 1 meta-fix PR in flight (`meta/jacoco-surefire-failsafe-merge` for GAP-347). 24 follow-up tests on #773 to reach 78.2% Sonar coverage; admin-merged with GAP-347 as systemic-fix follow-up. 0-clarification all 3 agents. New memory `feedback_webmvctest_mock_reset.md` saved (Mockito mock-state leak across `@WebMvcTest` methods). Wall-clock ~2.5h total agent work.
strategy: Phase-1B foundation wave-pack — same disjoint-buckets pattern as Wave 18b1; each bucket ships one user-visible v1 surface
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 18b2 — K-12 LEGAL Trio Phase 1B Foundation Wave-Pack

**Wave kickoff readiness:** 🟢 ALL preconditions met
- Wave 18b1 SHIPPED 2026-05-04 (PR #768 closure)
- GAP-323b Phase 1B v1 backend foundation merging (PR #769 — write API + on-demand rollup + V51 CHECK; CI pending at plan time, agents will branch off main once #769 merges)
- mosh+tmux+ntfy mobile-resilient stack from Wave 17 active

**Wall-clock estimate:** ~2-3h total (foundation 30min + 3 parallel agents ~1.5-2.5h longest path + sequential merge ~20min + closure ~30min). Consistent with Wave 18b1 actual ~3h.

**Methodology:** Phase 1B foundation wave-pack — each bucket ships **one user-visible v1 surface only**, NOT full Phase 1B. Remainder of Phase 1B (offline queue, perf test, Zalo OTP, MinIO upload, etc.) deferred to Phase 1B follow-up sister PRs/gaps. Mirrors Wave 18b1 skeleton-then-iterate cadence.

---

## §1 Brainstorm

### Q1 — Persona alignment (要件定義)

| Bucket | Gap | Affects | Wave 18b1 / 17 evidence |
|--------|-----|---------|------------------------|
| A | GAP-323b mobile UI v1 (frontend tap-grid + bulk actions) | P5 K-12 daily ops | TT 22/2021 + AC-OPS-001 ≤2min target; backend foundation shipped #769 — UI is the actual user-visible blocker |
| B | GAP-322b foundation (vetting workflow service + MinIO hook + RBAC gate skeleton) | P5 K-12 (criminal liability) | Decree 56/2017 vetting; AC-EDGE-005 dependent — Phase 1A AES + safeguarding role shipped Wave 18b1 #767 |
| C | GAP-321b foundation (5 facets read-only endpoints + audit log skeleton) | P5 K-12 parent communication | Luật GD Đ.83 + PDPL Art 16; Phase 1A transcript facet shipped Wave 18b1 #766 — extend with 5 sibling facets |

All 3 are P0 LEGAL Phase 1B foundation — extend the Wave 18b1 Phase 1A skeletons.

### Q2 — Trade-offs (詳細設計)

| Decision | Choice | Reason |
|---|---|---|
| Phase 1B v1 scope cut | **Foundation only** per bucket; defer non-foundational sub-tasks to Phase 1B follow-up | Match Wave 18b1 precedent (skeleton then iterate); fits ~2-2.5h agent target |
| Agent A scope | Mobile route shell + tap-grid component + bulk actions; **defer offline queue + Playwright perf test** | UX-finicky offline + perf belong in dedicated PR after first user trial |
| Agent B scope | `VettingService` + service-level state machine + MinIO storage hook (uploadStub) + RBAC gate; **defer LLTP file upload UI + verify queue UI** | Backend foundation unblocks future UI work; UI itself is Phase 1B follow-up |
| Agent C scope | 4 read-only endpoints (attendance / fees / conduct / discipline notifications) extending parent service + per-read audit log skeleton; **defer write actions, Zalo OTP login, multi-children selector** | Read-only mirrors Wave 18b1 Bucket D pattern (transcript facet); writes need PDPL granular consent (Phase 1C) |
| Migration V-numbers | A=NONE (frontend-only), B=V52, C=V53 (audit log table) | Reserve here; latest=V51 after PR #769 |
| Branch base | `main` AFTER PR #769 merges | Otherwise migrations conflict; Bucket A is FE-only so could base earlier but cleaner to align all 3 |

### Q3 — Risks + mitigations

| # | Risk | Mitigation |
|---|------|-----------|
| 1 | Agent A FE-only — TS strict mode trap from Wave 18a Bucket A | Mandate `pnpm build` (not just `pnpm test`) per memory `feedback_agent_ts_strict_uncheckedindex.md` |
| 2 | Agent B MinIO storage hook — wrong abstraction creates rework when actual upload UI lands | Stub interface + integration smoke test; concrete impl deferred but contract pinned |
| 3 | Agent C 4 facets touch overlapping parent service — refactor risk | Each facet = separate Controller method + separate Service method; share parent module but disjoint files |
| 4 | PR #769 not yet merged at agent kickoff | **Plan PR merges first → wait for #769 CI green + merge → THEN spawn agents off main** (per `feedback_session_resume_cross_contamination.md` — never auto-merge open PRs from other sessions; here we own #769 so just wait) |
| 5 | Long-running agents (~2-2.5h each) → SSH SIGHUP risk | mosh+tmux+ntfy stack + commit-after-each-file mandate |
| 6 | 4-layer V-model coverage on Phase 1B | Each bucket maps 4 layers; persona acceptance criteria cited |

### Q4 — File-overlap analysis (HARD vs SOFT)

| File/area | A (323b) | B (322b) | C (321b) | Conflict |
|---|:-:|:-:|:-:|:-:|
| `kiteclass-frontend/src/app/(teacher)/attendance/period/*` (NEW) | WRITE | — | — | NONE |
| `kiteclass-frontend/src/components/attendance/PeriodTapGrid.tsx` (NEW) | WRITE | — | — | NONE |
| `kiteclass-frontend/src/lib/api/attendance-period.ts` (NEW) | WRITE | — | — | NONE |
| `kiteclass-core/module/childprotection/service/VettingService.java` (NEW) | — | WRITE | — | NONE |
| `kiteclass-core/module/childprotection/storage/*` (NEW) | — | WRITE | — | NONE |
| `kiteclass-core/module/parent/controller/Parent*FacetController.java` (NEW) | — | — | WRITE | NONE |
| `kiteclass-core/module/parent/service/Parent*FacetService.java` (NEW) | — | — | WRITE | NONE |
| `kiteclass-core/module/parent/audit/ParentAuditLog.java` (NEW entity) | — | — | WRITE | NONE |
| `kiteclass-core/db/migration/V52` | — | V52 | — | NONE |
| `kiteclass-core/db/migration/V53` | — | — | V53 | NONE |
| Business docs `01-business/kiteclass/period-attendance/*` | extend | — | — | NONE (already authored Wave 18b1 + #769) |
| Business docs `01-business/kiteclass/child-protection/*` | — | extend | — | NONE |
| Business docs `01-business/kiteclass/parent-portal/*` | — | — | extend | NONE |
| GAP files (`GAP-323b.md` / `322b.md` / `321b.md`) | extend | extend | extend | SOFT (each agent edits own gap file only) |

**Verdict:** 0 HARD conflicts. 0 SOFT (each agent edits its own gap file only). Fully disjoint per `feedback_parallel_agent_strategy.md` rule #5.

---

## §2 Task Breakdown

| # | Task | Phase | Wall-clock | Owner |
|---|------|:-:|:-:|---|
| 1 | This wave plan | 1 | 30 min | Claude (parent) |
| 2 | Plan PR review + merge | 1 | 5 min | User approve |
| 3 | Wait for PR #769 CI green + merge (foundation for V52/V53 migration ordering) | 1 | ~10 min | User approve / Claude monitor |
| 4 | Spawn 3 background agents (worktree-isolated, run_in_background:true) | 2 | <5 min | Claude (parent) |
| 4a | Agent A — GAP-323b mobile UI v1 (tap-grid + bulk + route shell) | 2 | ~2-2.5h | Background agent |
| 4b | Agent B — GAP-322b foundation (vetting service + MinIO hook + RBAC gate) | 2 | ~2-2.5h | Background agent |
| 4c | Agent C — GAP-321b foundation (4 read-only facets + audit log skeleton) | 2 | ~2h | Background agent |
| 5 | 3 PRs CI green + sequential merge | 2 | ~20 min | Claude (parent) |
| 6 | Closure PR — gap status updates + ROADMAP sync + memory if applicable | 2 | ~30 min | Claude (parent) |

---

## §3 Scope per Bucket (基本設計)

### Bucket A — GAP-323b Phase 1B v1 mobile UI: tap-grid + bulk actions + route shell

| Item | Value |
|------|-------|
| Branch | `wave/18b2-bucket-a-attendance-mobile-ui-v1` |
| Domain | `kiteclass-frontend/src/app/(teacher)/attendance/period/*` (NEW) |
| FE files | `(teacher)/attendance/period/[classId]/[periodNo]/[date]/page.tsx` NEW (route shell), `components/attendance/PeriodTapGrid.tsx` NEW (42 students × 4 status buttons), `components/attendance/PeriodBulkActions.tsx` NEW (mark-all-present + reset), `lib/api/attendance-period.ts` NEW (POST batch + PATCH client), `hooks/use-period-attendance.ts` NEW (TanStack Query mutation) |
| Tests | Component tests (Vitest + Testing Library) for tap-grid state transitions + bulk actions; Playwright e2e for happy-path POST batch only |
| Migration | NONE (frontend-only) |
| Business docs | Extend `documents/01-business/kiteclass/period-attendance/use-cases.md` — promote UC-PERIOD-ATT-UI-001 (tap-grid happy path) from placeholder to full UC; FE behaviour sections in W-001/W-002 |
| Deferred | Offline queue (UC-PERIOD-ATT-UI-002) + Playwright ≤2min perf assertion + multi-period quick-switch — stay placeholders |
| 4-layer | 要件: AC-OPS-001 ≤2min target / 基本: tap-grid + bulk actions UI / 詳細: TanStack mutation + optimistic update / コンポ: PeriodTapGrid + PeriodBulkActions |
| Status flip | GAP-323b stays 🟡 PARTIAL — extends Phase 1B v1 backend (#769) with v1 UI; remainder still PARTIAL |

### Bucket B — GAP-322b Phase 1B foundation: vetting service + MinIO hook + RBAC gate

| Item | Value |
|------|-------|
| Branch | `wave/18b2-bucket-b-childprotection-vetting-foundation` |
| Domain | `kiteclass-core/module/childprotection` (extend Wave 18b1 NEW module) |
| BE files | `childprotection/entity/Vetting.java` NEW (LLTP / police-check / interview status), `childprotection/enums/VettingStatus.java` NEW, `childprotection/repository/VettingRepository.java` NEW, `childprotection/service/VettingService.java` NEW (state machine with @Convert AES-256 on sensitive fields), `childprotection/storage/VettingDocumentStorage.java` NEW (MinIO interface; impl stub returning fixed URL — actual upload deferred to Phase 1B follow-up), `childprotection/storage/MinIOVettingDocumentStorageImpl.java` NEW (stub), `childprotection/controller/VettingController.java` NEW (CRUD + state-transition endpoints, NO file upload yet), `childprotection/dto/{VettingResponse, VettingCreateRequest, VettingTransitionRequest}.java` NEW |
| RBAC | Extend safeguarding-officer role (Wave 18b1 #767) with Vetting endpoint permissions; staff teachers BLOCKED from /api/v1/vettings/* without active vetting record |
| Tests | `VettingServiceTest` (state-machine + encryption roundtrip), `VettingControllerIT` (RBAC: safeguarding-officer can read/write, teacher blocked), `VettingStorageStubTest` (interface contract) |
| Migration | V52 in kiteclass-core: `vettings` table with encrypted columns (BYTEA), status enum, FK to teacher_id, soft-delete, audit, instance_id |
| Business docs | Extend `documents/01-business/kiteclass/child-protection/{rules.md, use-cases.md, api-contract.md}` — add BR-VETTING-001..005 + UC-VETTING-* + endpoints |
| Deferred | LLTP file upload UI + verify queue UI + Tổng đài 111 webhook + 7y retention enforcement (Phase 1B follow-up + Phase 1C) |
| 4-layer | 要件: Decree 56/2017 vetting + AC-EDGE-005 / 基本: Future verify-queue UI / 詳細: state machine + AES-256 + MinIO contract / コンポ: future Phase 1B verify-queue table |
| Status flip | GAP-322b 🔵 OPEN → 🟡 PARTIAL (foundation shipped) |

### Bucket C — GAP-321b Phase 1B foundation: 4 read-only facets + audit log skeleton

| Item | Value |
|------|-------|
| Branch | `wave/18b2-bucket-c-parent-portal-facets-foundation` |
| Domain | `kiteclass-core/module/parent` (extend Wave 18b1 #766 transcript facet) |
| BE files | `parent/controller/ParentAttendanceController.java` NEW (`GET /api/v1/parent/children/{childId}/attendance`), `parent/controller/ParentFeesController.java` NEW (fees facet), `parent/controller/ParentConductController.java` NEW (hạnh kiểm facet), `parent/controller/ParentNotificationsController.java` NEW (notifications facet read-only), `parent/service/Parent{Attendance,Fees,Conduct,Notifications}FacetService.java` NEW × 4 (each with ParentStudentLink scope guard mirroring Phase 1A pattern), `parent/dto/Parent{Attendance,Fees,Conduct,Notification}FacetResponse.java` NEW × 4, `parent/audit/ParentReadAuditLog.java` NEW (entity), `parent/audit/ParentReadAuditLogService.java` NEW (logs every facet read for PDPL Art 16 traceability) |
| Tests | `Parent{Attendance,Fees,Conduct,Notifications}ControllerIT` × 4 (auth + scope test mirroring transcript pattern); `ParentReadAuditLogServiceTest` (entry created on each read) |
| Migration | V53 in kiteclass-core: `parent_read_audit_log` table (parent_id, child_id, facet, read_at, instance_id, soft-delete, audit cols) |
| Business docs | Extend `documents/01-business/kiteclass/parent-portal/{rules.md, use-cases.md, api-contract.md}` — add 4 facet endpoints + audit log + BR-PARENT-AUDIT-001 (every facet read logged for PDPL traceability) |
| Deferred | Zalo OTP login, multi-children selector polish, write actions (complaints/RSVP/absence requests, granular PDPL consent — Phase 1C) |
| 4-layer | 要件: Luật GD Đ.83 + PDPL Art 16 / 基本: 5-facet parent dashboard layout (Phase 1A transcript + 4 new) / 詳細: ParentStudentLink scope guard reused × 4 + audit log per read / コンポ: future Phase 1B parent dashboard tabs UI |
| Status flip | GAP-321b 🔵 OPEN → 🟡 PARTIAL (foundation shipped) |

---

## §4 Agent Prompt Template

Use `feature-tdd-agent` template per `feedback_parallel_agent_strategy.md`. Each agent receives standard prompt structure (placeholders filled per bucket per §3 above).

**Mandates (from CLAUDE.md + memory):**
1. RELATIVE paths only (per memory `feedback_worktree_absolute_path_contamination.md`)
2. TDD: failing test FIRST, then production code
3. Commit after each file (SIGHUP risk per `feedback_agent_kill_root_cause.md`)
4. Business docs SAME PR as code (Living Docs)
5. JSONB pairs `@JdbcTypeCode(SqlTypes.JSON)` (per memory)
6. ObjectMapper `findAndRegisterModules()` in tests
7. NEW package added to subscription? → also update KiteHubAdminApplication scan list (per memory `feedback_admin_scan_packages_after_module_add.md`)
8. FE: run `pnpm build` (not just `pnpm test`) before declaring done (per memory `feedback_agent_ts_strict_uncheckedindex.md`)
9. business-logic-review.md 5-attribute frontmatter MANDATORY when extending rules.md
10. Bucket A FE-only: must run `pnpm test` + `pnpm build` (TS strict mode catches noUncheckedIndexedAccess)
11. Bucket B/C BE: must run `mvnw test -pl kiteclass/kiteclass-core -am` green
12. ALL agents: `bash .claude/skills/workflow/session-docs-check/scripts/check-docs.sh` PASS before push

**Status flips (closure PR):**
- GAP-321b 🔵 OPEN → 🟡 PARTIAL (foundation shipped; Phase 1B remainder + Phase 1C tracked)
- GAP-322b 🔵 OPEN → 🟡 PARTIAL (foundation shipped; LLTP upload UI + verify queue + 111 webhook deferred)
- GAP-323b stays 🟡 PARTIAL (UI v1 added on top of Phase 1B v1 backend; offline queue / perf test still deferred)

**Spawn discipline:**
- `run_in_background: true` (per `.claude/rules/agent-background-spawn-default.md`)
- `isolation: worktree` (per `feedback_parallel_agent_strategy.md` rule #2)
- Single message dispatch all 3 agents simultaneously (per `feedback_parallel_agent_strategy.md` rule #6)

---

## §5 4-Layer V-Model Coverage Per Bucket

| Layer | Bucket A (GAP-323b mobile) | Bucket B (GAP-322b vetting) | Bucket C (GAP-321b facets) |
|-------|----------------------------|------------------------------|------------------------------|
| 要件定義 | TT 22/2021 + AC-OPS-001 ≤2min | Decree 56/2017 vetting + AC-EDGE-005 | Luật GD Đ.83 + PDPL Art 16 |
| 基本設計 | Mobile tap-grid + bulk actions UI | Vetting CRUD UI (future) + MinIO storage contract | 5-facet parent dashboard (Phase 1A transcript + 4 new endpoints) |
| 詳細設計 | TanStack mutation + optimistic update against #769 backend | State machine + AES-256 + MinIO storage interface + RBAC gate | ParentStudentLink scope guard × 4 + audit log per read |
| コンポ設計 | PeriodTapGrid + PeriodBulkActions | Future verify-queue table (Phase 1B follow-up) | Future parent dashboard tabs UI (Phase 1B follow-up) |

---

## §6 Closure PR Scope

After 3 agent PRs merged:

1. Status flips:
   - GAP-321b 🔵 OPEN → 🟡 PARTIAL
   - GAP-322b 🔵 OPEN → 🟡 PARTIAL
   - GAP-323b stays 🟡 PARTIAL (extended with mobile v1)
2. ROADMAP §🚀 Next Action — pivot to Phase 1B follow-up (offline queue + perf test for 323b OR Phase 1C work for 321/322)
3. ROADMAP §Status Snapshot entry for Wave 18b2 SHIPPED
4. Wave plan status `draft → complete` + `actual_outputs` filled
5. Memory entries if non-obvious lessons (TBD post-execution)
6. NO new sister gaps filed unless agents discover scope drift; the existing letter-suffix gaps (b/c) cover known remainder

---

## §7 Acceptance Criteria

- [ ] Foundation PR (this) merged
- [ ] PR #769 (GAP-323b Phase 1B v1 backend) merged before agent spawn
- [ ] 3 agent PRs CI green + merged
- [ ] All 3 buckets pass session-docs-check
- [ ] Closure PR merged with gap status updates + ROADMAP sync
- [ ] No HARD merge conflicts between bucket PRs
- [ ] Wave plan status flipped `draft → complete`

---

## §8 Log

- **2026-05-04 (SHIPPED)** — Wave 18b2 closed. 3 parallel agents (Bucket A FE / B vetting / C parent facets) all 0-clarification. PR sequence: #771 ✅ merged, #772 ✅ merged, #773 admin-merged after coverage-fix push (24 unit tests added; Sonar reached 78.2% — root cause traced to JaCoCo surefire-only artifact, **filed GAP-347 meta-fix** for `pom.xml` jacoco surefire+failsafe merge). Memory `feedback_webmvctest_mock_reset.md` saved (Mockito mock-state leak across `@WebMvcTest` methods — surfaced by Bucket B). All 3 gaps now correctly 🟡 PARTIAL with explicit Phase 1B follow-up + Phase 1C deferral. Closure PR (this) flips wave plan status + files GAP-347 + updates ROADMAP §🚀 Next Action.
- **2026-05-04** — Wave 18b2 plan drafted same-day as Wave 18b1 closure + GAP-323b Phase 1B v1 backend (PR #769). Continues K-12 LEGAL trio momentum: Phase 1A skeleton (Wave 18b1) → Phase 1B v1 backend (PR #769) → Phase 1B foundation across remaining 2 gaps + UI v1 for 323b (this wave). Per `feedback_wave_plan_through_pr.md` plan ships through PR not direct push.
