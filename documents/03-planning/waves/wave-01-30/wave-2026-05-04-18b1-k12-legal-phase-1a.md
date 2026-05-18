---
title: Wave 18b1 — K-12 LEGAL Trio Phase 1A skeleton wave-pack (GAP-321 + GAP-322 + GAP-323)
status: complete
created: 2026-05-04
updated: 2026-05-04
waves: [18b1]
gaps: [GAP-321, GAP-322, GAP-323]
phase: 1A
expected_outputs: 3 PRs (Phase 1A skeletons) + 6 sister gaps filed for Phase 1B/C + closure PR
actual_outputs: 5 PRs merged (#763 GAP-285 fix + #764 plan + #765 Bucket F + #766 Bucket D + #767 Bucket E) + closure PR (this) filing 6 sister gaps. Wall-clock ~3h total agent work (~1.5-2.5h longest path Bucket E encryption). 0-clarification all 3 agents. State-check addendum from Bucket D: GAP-345 missed Wave 2 inline-fetch FE skeleton — 4th GAP-190/197 head-truncation recurrence (closure PR extends audit-to-gap-pipeline.md Step 2.5).
strategy: Phase-1A wave-pack (Wave Legal-BRD precedent applied to K-12 LEGAL trio) — 3 disjoint buckets, parallel agents
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 18b1 — K-12 LEGAL Trio Phase 1A skeleton wave-pack

**Wave kickoff readiness:** 🟢 ALL preconditions met
- Wave 18a SHIPPED 2026-05-04 (PRs #756-#762)
- GAP-345 state-check audit MERGED (PR #757) → revised gap files reflect accurate state
- GAP-285 admin CI flake FIXED (PR #763) → CI clean baseline
- mosh+tmux+ntfy mobile-resilient stack from Wave 17 active

**Wall-clock estimate:** ~3h total (foundation 30min + 3 parallel agents ~1.5-2h longest path + sequential merge ~20min + closure ~30min).

**Methodology:** Phase 1A skeleton wave-pack — match precedent Wave 13/14 Legal-BRD Phase 1+1.5 (~30 min wall-clock) + Wave 18a Cross-Persona Keystones Phase 1 (~3.5h). Each bucket ships **structural foundation only**; Phase 1B (UI/workflow) + 1C (polish) deferred to Wave 18b2/b3 via sister gaps.

---

## §1 Brainstorm

### Q1 — Persona alignment (要件定義)

| Bucket | Gap | Affects | Wave 17 evidence |
|--------|-----|---------|------------------|
| D | GAP-321 Phase 1A (parent portal FE bones) | P5 K-12 (1800 PH) + tangentially P2/P3 | Luật GD 2019 Đ.83 mandate; P5 0/6 communication ACs FAIL |
| E | GAP-322 Phase 1A (incident + encryption + safeguarding role) | P5 K-12 (criminal liability) | Luật Trẻ em 2016 Đ.51 mandatory reporting; AC-EDGE-005 FAIL |
| F | GAP-323 Phase 1A (period attendance + tenant.vertical_type) | P5 K-12 (daily ops) + future P3/P4 | TT 22/2021 + TT 32/2018 MOET; AC-OPS-001..003 FAIL |

All 3 are P0 LEGAL — block K-12 deployment.

### Q2 — Trade-offs (詳細設計)

| Decision | Choice | Reason |
|---|---|---|
| Phase 1A scope cut | Skeleton only (entity + migration + read-only API + business docs); UI deferred Phase 1B | Match Legal-BRD precedent; agents fit ~2h target |
| Migration V-numbers | E=V49, F=V50, D=no migration (reuses Parent/ParentStudentLink from V42) | Reserve here; latest=V48 after Wave 18a |
| GAP-321 Phase 1A scope | 1 facet (transcript read-only) — proves end-to-end, defer 5 other facets | Lowest-risk minimum; later agents add facets independently |
| GAP-322 Incident encryption | JPA AttributeConverter with AES-256 (existing encryption.master-key config from admin tests) | Standard pattern; reuse infra |
| GAP-322 module placement | NEW `module/childprotection/` (per GAP-322 revision §Verdict — do NOT extend `module/legal/` DMCA) | Clean separation; per audit guidance |
| GAP-323 vertical_type location | Add column on `instances` table (kitehub-side) — tenant config | Multi-tenant discriminator naturally lives on instance |
| GAP-323 AttendancePeriod table | New `attendance_period` on kiteclass-core (per gap §Phase 2.1) | Don't refactor existing per-day Attendance — backward compat |

### Q3 — Risks + mitigations

| # | Risk | Mitigation |
|---|------|-----------|
| 1 | Bucket E encryption: AttributeConverter wrong approach causing data corruption | Test with @DataJpaTest + roundtrip assertion on encrypted column; reuse existing `encryption.master-key` from admin tests config |
| 2 | Bucket F tenant.vertical_type spans 2 modules (kitehub instance + kiteclass core) | Phase 1A: add on `instances` (kitehub V<N>); kiteclass reads via tenant context. Wave 18b2 wires further. |
| 3 | Bucket D depends on existing Parent + ParentStudentLink — relies on V42 from Wave 2 | State-check confirmed; agent reads `kiteclass-core/module/parent/` for entity reuse |
| 4 | Long-running agents (~1.5-2h each) → SSH SIGHUP risk | mosh+tmux+ntfy stack + commit-after-each-file mandate |
| 5 | 4-layer V-model coverage on legal scope | Each bucket explicitly maps 4 layers; legal mandates cited per `business-logic-review.md` 5-attribute |
| 6 | Multi-domain business docs creation (childprotection NEW + period-attendance NEW + parent-portal extension) | 3-layer (rules+use-cases+api-contract) per domain mandatory |

### Q4 — File-overlap analysis (HARD vs SOFT)

| File/area | D | E | F | Conflict |
|---|:-:|:-:|:-:|:-:|
| `kiteclass-core/module/parent/*` | READ | — | — | NONE (Phase 1A read-only) |
| `kiteclass-core/module/childprotection/*` (NEW) | — | WRITE | — | NONE |
| `kiteclass-core/module/attendance/*` | — | — | WRITE (extends with period) | NONE |
| `kiteclass-core/module/role/*` | — | WRITE (add SafeguardingOfficer) | — | NONE |
| `kiteclass-core/db/migration/V49-V50` | — | V49 | V50 | NONE (reserved) |
| `kitehub-subscription/db/migration/V24` | — | — | V24 (vertical_type column) | NONE |
| `kiteclass-frontend/src/app/(parent)/*` | WRITE (NEW route) | — | — | NONE |
| Business docs `01-business/kiteclass/parent-portal/*` | extend | — | — | NONE |
| Business docs `01-business/kiteclass/child-protection/*` (NEW) | — | WRITE | — | NONE |
| Business docs `01-business/kiteclass/period-attendance/*` (NEW) | — | — | WRITE | NONE |

**Verdict:** 0 HARD conflicts, 0 SOFT. Fully disjoint per `feedback_parallel_agent_strategy.md` rule #5.

---

## §2 Task Breakdown

| # | Task | Phase | Wall-clock | Owner |
|---|------|:-:|:-:|---|
| 1 | This wave plan | 1 | 30 min | Claude (parent) |
| 2 | Plan PR review + merge | 1 | 5 min | User approve |
| 3 | Spawn 3 background agents (worktree-isolated, run_in_background:true) | 2 | <5 min | Claude (parent) |
| 4d | Agent D — GAP-321 Phase 1A | 2 | ~2h | Background agent |
| 4e | Agent E — GAP-322 Phase 1A | 2 | ~2.5h | Background agent (encryption tricky) |
| 4f | Agent F — GAP-323 Phase 1A | 2 | ~2h | Background agent |
| 5 | 3 PRs CI green + sequential merge | 2 | ~20 min | Claude (parent) |
| 6 | Closure PR — file 6 sister gaps (1B + 1C per gap) + ROADMAP sync + memory | 2 | ~30 min | Claude (parent) |

---

## §3 Scope per Bucket (基本設計)

### Bucket D — GAP-321 Phase 1A: Parent portal route + transcript read-only

| Item | Value |
|------|-------|
| Branch | `wave/18b1-bucket-d-parent-portal-1a` |
| Domain | `kiteclass-core/module/parent` (extend) + `kiteclass-frontend/src/app/(parent)` (NEW) |
| BE files | `parent/controller/ParentTranscriptController.java` NEW (1 endpoint), `parent/service/ParentTranscriptService.java` NEW, `parent/dto/TranscriptResponse.java` NEW |
| FE files | `kiteclass-frontend/src/app/(parent)/page.tsx` NEW (children selector + transcript card), `(parent)/transcript/[childId]/page.tsx` NEW (single-child transcript view), `lib/api/parent.ts` NEW, `hooks/use-parent.ts` NEW |
| Tests | `ParentTranscriptControllerIT` (auth + scope test), FE component tests |
| Migration | NONE (reuses Parent/ParentStudentLink from V42 GAP-052a) |
| Business docs | `documents/01-business/kiteclass/parent-portal/{rules.md, use-cases.md, api-contract.md}` NEW (3-layer; cite Luật GD Đ.83 + PDPL Art 16) |
| Endpoint | `GET /api/v1/parent/children/{childId}/transcript` (scoped to ParentStudentLink) |
| 4-layer | 要件: Luật GD Đ.83 + AC-COMM-001 (P5) / 基本: parent dashboard mockup / 詳細: scope guard via ParentStudentLink lookup / コンポ: TranscriptView component |
| Sister gap to file at closure | **GAP-321b** (Phase 1B: 5 remaining facets — điểm danh, học phí, hạnh kiểm, notifications, kỷ luật + multi-children selector + Zalo OTP login + audit log per-read) |
| Sister gap to file at closure | **GAP-321c** (Phase 1C: PDPL parental consent flag + write actions complaints/RSVP/absence) |

### Bucket E — GAP-322 Phase 1A: Incident entity + encryption + safeguarding role

| Item | Value |
|------|-------|
| Branch | `wave/18b1-bucket-e-childprotection-1a` |
| Domain | `kiteclass-core/module/childprotection` (NEW per GAP-322 §Verdict) |
| BE files | `childprotection/entity/Incident.java` NEW (with @Convert AES-256 on description + evidence_paths fields), `childprotection/enums/{IncidentSeverity, IncidentCategory, IncidentStatus}.java`, `childprotection/repository/IncidentRepository.java`, `childprotection/service/IncidentService.java` NEW (CRUD with field-level encryption), `childprotection/converter/AesGcmAttributeConverter.java` NEW (reuses encryption.master-key) |
| Role addition | `kiteclass-core/module/role/` — add `SAFEGUARDING_OFFICER` enum value + RBAC mapping |
| Tests | `IncidentServiceTest` (CRUD with encryption roundtrip), `AesGcmAttributeConverterTest` (encrypt/decrypt invariant), `RbacIT` (safeguarding officer access scope) |
| Migration | V49 in kiteclass-core: `incidents` table with encrypted columns (BYTEA for ciphertext), severity/category/status enums, soft-delete, audit, instance_id |
| Business docs | `documents/01-business/kiteclass/child-protection/{rules.md, use-cases.md, api-contract.md}` NEW (3-layer; cite Luật Trẻ em Đ.51 + Đ.25, Decree 56/2017, PDPL Art 16; business-logic-review 5-attribute frontmatter MANDATORY for legal mandate) |
| 4-layer | 要件: Luật Trẻ em + AC-EDGE-005 / 基本: future incident submission UI mockup / 詳細: AES-256 encryption pattern + RBAC gate / コンポ: future Phase 1B incident form |
| Sister gap to file at closure | **GAP-322b** (Phase 1B: vetting workflow LLTP upload + MinIO encrypted bucket + verify queue + RBAC gate teacher access) |
| Sister gap to file at closure | **GAP-322c** (Phase 1C: mandatory reporting Đ.51 banner + hash-chained audit log + 7-year retention + Tổng đài 111 webhook) |

### Bucket F — GAP-323 Phase 1A: AttendancePeriod entity + tenant.vertical_type

| Item | Value |
|------|-------|
| Branch | `wave/18b1-bucket-f-period-attendance-1a` |
| Domain | `kiteclass-core/module/attendance` (extend) + `kitehub-subscription/.../instance` (extend tenant) |
| BE files | `attendance/entity/AttendancePeriod.java` NEW, `attendance/repository/AttendancePeriodRepository.java`, `attendance/service/AttendancePeriodService.java` NEW (read-only Phase 1A), `attendance/controller/AttendancePeriodController.java` NEW (read-only), `attendance/dto/AttendancePeriodResponse.java` NEW |
| Migration | V50 kiteclass-core: `attendance_period (id, student_id, class_id, subject_section_id, period_no, date, status, recorded_by, recorded_at, instance_id, audit cols)`. V24 kitehub-subscription: add `vertical_type` column on `instances` table (enum CENTER/K12_SCHOOL, default CENTER backward compat) |
| Tests | `AttendancePeriodServiceTest` (read-only queries), `AttendancePeriodControllerIT` |
| Business docs | `documents/01-business/kiteclass/period-attendance/{rules.md, use-cases.md, api-contract.md}` NEW (3-layer; cite TT 22/2021 + TT 32/2018; business-logic-review 5-attribute) |
| Endpoint | `GET /api/v1/attendance/periods` (filter by student/class/date range; Phase 1A read-only, no write) |
| 4-layer | 要件: TT 22/2021 Đ.7 + AC-OPS-001 / 基本: future GVCN mobile mockup / 詳細: schema design + tenant.vertical_type gate / コンポ: future Phase 1B mobile attendance grid |
| Sister gap to file at closure | **GAP-323b** (Phase 1B: write API for period attendance + GVCN mobile UI ≤2min điểm danh + daily roll-up view) |
| Sister gap to file at closure | **GAP-323c** (Phase 1C: GradeFormulaService TT 22/2021 + Tổ trưởng state machine + multi-subject gradebook UI 12-15 môn) |

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
7. NEW package added to subscription? → also update KiteHubAdminApplication scan list (per memory `feedback_admin_scan_packages_after_module_add.md` — Wave 18a Bucket B lesson)
8. FE: run `pnpm build` (not just `pnpm test`) before declaring done (per memory `feedback_agent_ts_strict_uncheckedindex.md` — Wave 18a Bucket A lesson)
9. business-logic-review.md 5-attribute frontmatter MANDATORY on rules.md (Source = legal mandate, Reviewer = solo-dev acting Legal scout, Compliance = Compliant per cited statute, Cadence = Annual + event-driven on amendment)

**Status flips:**
- GAP-321 → 🟡 PARTIAL (already partial; add Log entry for Phase 1A delivered)
- GAP-322 → 🟡 PARTIAL (was OPEN; flip to PARTIAL with Phase 1A delivered)
- GAP-323 → 🟡 PARTIAL (already partial; add Log entry for Phase 1A delivered)

---

## §5 4-Layer V-Model Coverage Per Bucket

| Layer | Bucket D (GAP-321) | Bucket E (GAP-322) | Bucket F (GAP-323) |
|-------|-------------------|-------------------|-------------------|
| 要件定義 | Luật GD Đ.83 + AC-COMM-001 (P5 review) | Luật Trẻ em Đ.51/Đ.25 + Decree 56/2017 + AC-EDGE-005 | TT 22/2021 + TT 32/2018 + AC-OPS-001..003 |
| 基本設計 | Parent dashboard mockup (future) | Incident submission flow (future) | GVCN mobile attendance grid (future) |
| 詳細設計 | ParentStudentLink scope guard | AES-256 AttributeConverter + RBAC pattern | AttendancePeriod schema + tenant.vertical_type discriminator |
| コンポ設計 | TranscriptView (Phase 1A delivers) | Incident form component (Phase 1B) | Period attendance grid (Phase 1B) |

---

## §6 Closure PR Scope

After 3 agent PRs merged:

1. Status flips:
   - GAP-321 stays 🟡 PARTIAL (now 2 Phase 1A items delivered: existing entities from V42 + new transcript facet)
   - GAP-322 → 🟡 PARTIAL (Phase 1A scaffolding delivered)
   - GAP-323 stays 🟡 PARTIAL (Phase 1A schema delivered)
2. **6 sister gaps filed** (per §3): GAP-321b, GAP-321c, GAP-322b, GAP-322c, GAP-323b, GAP-323c
3. ROADMAP §🚀 Next Action — pivot to Wave 18b2 (Phase 1B for first gap chosen by user)
4. ROADMAP §Status Snapshot entry for Wave 18b1 SHIPPED
5. Wave plan status `draft → complete` + `actual_outputs` filled
6. Memory entries if non-obvious lessons (TBD post-execution)

---

## §7 Acceptance Criteria

- [ ] Foundation PR (this) merged
- [ ] 3 agent PRs CI green + merged
- [ ] 6 sister gaps filed (Phase 1B + 1C × 3 gaps)
- [ ] Business docs 3-layer with `business-logic-review.md` 5-attribute frontmatter for legal mandate sources
- [ ] 4-layer V-model coverage matrix in each PR description
- [ ] Closure PR merged with ROADMAP sync
- [ ] Wall-clock total ≤ 4h (target ~3h; cap ~4h)

---

## §8 Out-of-scope for Wave 18b1

- All Phase 1B (UI/workflow for each gap) — Wave 18b2
- All Phase 1C (polish/integrations for each gap) — Wave 18b3
- Phase 2/3 (legal counsel content, external integrations like Tổng đài 111) — Stage 2-3 multi-quarter
- Other K-12 P0 LEGAL gaps not in trio (GAP-336 MOET financial reporting, GAP-341 phổ cập escalation) — separate waves
- Wave 18a sister gaps GAP-063b/057b execution — separate waves

---

## §9 Log

- **2026-05-04 (v0.1)** — Plan created. Per `feedback_wave_plan_through_pr.md` MUST go through PR. Builds on Wave 18a precedent (Phase-1 wave-pack pattern validated 3.5h wall-clock with 0 clarifications). Phase 1A scope-cut chosen over full Phase 1 because each gap's full Phase 1 = 3-5 days agent work (SSH SIGHUP risk too high; better split 1A skeleton + 1B/C waves). User chose to "complete Wave 18 now" 2026-05-04 → Wave 18b1 = realistic deliverable in 1 session.
