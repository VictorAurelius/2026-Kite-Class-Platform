---
title: Wave flow-kc6 — Grade entry + report card + gradebook
status: active
created: 2026-06-05
updated: 2026-06-05
waves: [flow-kc6]
tag_primary: flow
tags_secondary: [kc6, grade, gradebook, transcript, report-card, kiteclass, campaign]
counter: 6
campaign: flow-verification-campaign
gaps: []
---

# Wave flow-kc6 — Grade entry + report card + gradebook

**Goal:** Walk end-to-end flow KC-6 (Teacher nhập grade components → calculate → finalize → transcript/report card → gradebook statistics) trên stack production-equivalent, đạt **G1 PASS**. Đứng sau KC-4 (enrollment) — grade gắn vào enrollment/student trong lớp.

**Trigger:** KC-6 unblocked sau KC-4 (student enrolled) + KC-5 (attendance G1 PASS). Campaign chain: KC-4 → {KC-5 attendance, KC-6 grade, KC-7 invoice}.

## 1. Brainstorm

**State-check (2026-06-05):** KC-6 = kiteclass-core flow. Endpoints CONFIRMED tồn tại:
- ✅ `GradeController` @ `/api/v1/grades` — POST `/initialize`, GET `/{id}` + `/student/{id}/class/{id}` + `/student/{id}` + `/class/{id}`, components CRUD (POST/PUT/DELETE `/components[/{id}]`), POST `/{id}/calculate` + `/{id}/finalize` + `/{id}/unfinalize`, transcripts (POST `/transcripts/generate` + GET `/transcripts/student/{id}[/semester/{s}]`), GET `/class/{id}/statistics`.
- ✅ `SubjectGradeController` (K12 multi-subject) — secondary K12-only.
- ✅ `ReportController` — report card.
- 2 business domains: `grade-assignment` + `multi-subject-gradebook`.

**Pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` (BẮT BUỘC):** Opus agent spawned 2026-06-05 → artifact `audits/persona-review/2026-06-05-pre-walk-kc6-grade.md`. Likely failure modes: weights sum ≠ 100% / calculate trước khi đủ components / finalize state machine (edit finalized? double-finalize?) / weighted calc rounding + div-by-zero / cross-tenant grade by-id (GAP-983) / authz missing on grade write (như KC-5 single-mark) / transcript generate khi chưa finalize.

**⚠️ KC-5 lesson (P0 GAP-996):** kiteclass-core IT dùng `ddl-auto=create-drop` (Flyway off) → mù schema↔entity drift. KC-5 attendance write CHƯA BAO GIỜ chạy trên Flyway schema dù 12 IT green. **Pre-walk agent PHẢI check grade table schema↔entity drift proactively** (NOT NULL unmapped / CHECK enum mismatch / stale unique). GAP-875 (grading-scales-entity-migration-drift) đã CLOSED — verify thực sự fixed hay scaffold-closed (live schema = ground truth).

**Isolation context (post GAP-983):** grade by-id + by-student + by-class lookups → cross-tenant re-walk verify.

**Blocker:** none known. Cần enrollment (KC-4) + class (KC-3) fixture; grade gắn student↔class.

## 2. Task Breakdown

| Bucket | Scope | Owner | Walk class |
|---|---|---|---|
| 0 (Pre-walk) | Opus persona sim agent → ≥5 FMs + schema-drift check per `pre-walk-persona-simulation-mandate.md` §3 | Coordinator | n/a (DONE — agent spawned) |
| A (Walk) | Coordinator G1 walk: initialize grade → add components (weights) → enter scores → calculate → finalize (+ state machine + authz + cross-tenant) | Coordinator | user-facing ✅ |
| B (Walk) | Transcript/report card generate + gradebook statistics | Coordinator | user-facing ✅ |
| C (Fix) | Batch-fix high-confidence pre-walk findings + walk bugs (catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4) | agent/coordinator | — |
| D (G2 handoff) | G2 recipe MD per `g2-handoff-md-mandate.md` khi G1 PASS | Coordinator | — |

K12 multi-subject gradebook (`SubjectGradeController`) secondary — defer G2/Phase 3 K12.

## 3. Scope

Full §3 expansion at walk-time (after pre-walk agent returns). Skeleton:
- **BE (kiteclass-core):** `module/grade/**` (GradeController + service + Grade/GradeComponent/Transcript entities + repos + mapper + DTOs) + `module/report/**`.
- **Verify target:** initialize grade cho enrollment (sky class 14) → add components (sum 100%) → enter scores → calculate weighted → finalize → transcript generate → statistics.
- **Schema drift (KC-5 lesson):** check grade/grade_component/transcript tables NOT NULL + CHECK vs entity.
- **Isolation (post GAP-983):** cross-tenant grade by-id → reject.
- **Dependency:** KC-3 (class 14) + KC-4 (enrollment 32) fixtures.

## 4. State-Check Evidence

Verified 2026-06-05 (grep, no `| head`):

| Symbol | Verify command | Verdict |
|---|---|---|
| `GradeController` @ /api/v1/grades | `grep -rn "RequestMapping\|Mapping" grade/controller/GradeController.java` | ✅ 16 endpoints (initialize + components + calculate + finalize + transcripts + statistics) |
| `ReportController` | `find ... report/controller/ReportController.java` | ✅ exists |
| grade domain docs (grade-assignment + multi-subject-gradebook) | `find documents/01-business -ipath "*grad*"` | ✅ both 3-layer |

Detailed DTO + service logic + entity↔schema = read at walk-time + pre-walk agent.

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — coordinator walk | Claude | initialize + components(weights) + calculate + finalize state machine + transcript + statistics; authz + cross-tenant; production-equivalent (schema drift checked) | ⬜ |
| G2 — human walk | User | Per G2 recipe MD (Bucket D) — grade entry + report card via UI | ⬜ |
| G3 — production parity | User | Post AWS restore — multi-tenant grade isolation + K12 multi-subject | ⬜ |

## 6. Agent Spawn Pattern

_(n/a — flow-walk wave: Bucket 0 = 1 Opus pre-walk persona-sim agent (background); G1 = coordinator manual walk on local stack. No parallel bucket-agent fan-out. Fix-agents spawned ad-hoc per finding per `agent-model-opus-default.md`.)_

## 7. Closure Protocol

1. Catalog walk findings → file gaps inline per `discovery-to-gap-inline-filing.md` §3.
2. Batch-fix high-confidence per `feature-ship-runtime-walk-mandate.md` §3.4.
3. Re-walk affected scope per `pre-handoff-self-test-completeness.md` §3.
4. G2 recipe MD per `g2-handoff-md-mandate.md`.
5. Flip campaign §4 KC-6 row → 🔄 walk-pass-pending-human.
6. wave-history.jsonl append; CSV + ROADMAP sync per `post-merge-sync-completeness.md`.

## 9. Pre-walk findings (2026-06-05) — `audits/persona-review/2026-06-05-pre-walk-kc6-grade.md`

12 failure modes (4 HIGH / 6 MEDIUM / 2 LOW). **Schema-drift check (KC-5 lesson): YES — `grading_scales`.**

**Contract surprises (quan trọng cho walk):**
- ⚠️ **calculate + finalize SẼ 404** `GRADING_SCALE_NOT_FOUND` vì `grading_scales` RỖNG (count=0) + không seed → **blocker #1**. V88 seed 8 default scales fix.
- ⚠️ **`grading_scales` schema drift** (4 cột legacy `grade`/`min_percentage`/`max_percentage`/`gpa` NOT NULL no-default entity không map) = **GAP-875 scaffold-close**. V88 DROP NOT NULL.
- ⚠️ **Thứ tự walk happy-path bắt buộc:** seed scale → POST `/initialize` (studentId+classId qua @RequestParam) → POST `/components` (weights=100%) → POST `/{id}/calculate` → POST `/{id}/finalize` (body `teacherId`=MAIN_TEACHER) → POST `/transcripts/generate`.
- ⚠️ **"Report card" KHÔNG ở ReportController** (chỉ /revenue + /attendance ADMIN). Transcript = `/api/v1/grades/transcripts/*`.
- ⚠️ **initialize dùng @RequestParam** studentId+classId; finalize/calculate dùng `{id}`=gradeId.

**HIGH bugs batch-fix TRƯỚC walk:**
- **GAP-998** P0 — grading_scales empty + legacy NOT-NULL drift → V88 (seed + DROP NOT NULL).
- **GAP-999** P1 — grade write/calc/read no @PreAuthorize (OWASP A01) → hasAccessToGrade helper + annotations (fix-agent).
- **GAP-1000** P1 — finalize teacherId self-asserted body + ADMIN blocked → OPEN (GAP-999 covers cross-tenant/non-teacher; teacherId-from-JWT = follow-up).
- **GAP-1001** P2 — transcript no semester filter + credit hardcode 3.0 → OPEN defer.

MEDIUM spot-check walk: weights chưa validate ở calculate (#5) / addComponent instance_id (#6, verify BaseEntity @PrePersist) / component_ref_id NULL dup (#7) / cross-tenant grade GAP-983 re-walk (#8). LOW: transcript studentName null (#11) / K12 SubjectGradeController secondary (#12).

## 8. Log

- **2026-06-05 (plan ship):** Filed sau KC-5 G1 PASS. State-check confirmed grade endpoints exist. Pre-walk persona sim (Opus) với schema-drift check mandate (KC-5 lesson). 12 FMs (4 HIGH), schema-drift YES (grading_scales).
- **2026-06-05 (batch-fix + walk):** GAP-998 (V88 seed+drift) + GAP-999 (authz, fix-agent 11 endpoints + 2 helpers, 76 tests green). **V88 W-1:** seed instance_id NULL → FAIL (NOT NULL + tenantFilter/RLS kill NULL-default by design) → revised per-tenant seed (GAP-1002 P1 design follow-up). G1 walk PASS.

## 10. G1 Outcome (2026-06-05)

**G1 ✅ PASS** (production-equivalent walk, post V88 + grade authz):

| # | Scenario | Result |
|---|---|---|
| W1-3 | initialize → add components (MIDTERM 40% + FINAL 60%) → calculate | finalScore **88.0 / B+ / gpa 3.3** ✅ (was 404 GRADING_SCALE_NOT_FOUND) |
| W4 | finalize (MAIN_TEACHER teacherId 3) | isFinalized=true ✅ |
| W5 | transcript generate | 201 ✅ |
| W6/W6b | calculate no-user / unfinalize wrong-user (GAP-999 authz OWASP A01) | 403 ✅ |
| W7/W7b | GET grade by-id khanh / sky (GAP-983 re-walk) | 404 / 200 ✅ isolation holds |
| W8 | class statistics | 200 ✅ |

MEDIUM #6 (addComponent instance_id) → 201 (BaseEntity @PrePersist fills — không phải bug).

**GAP-998 + GAP-999 → DONE** (walk-verified). **OPEN follow-ups:** GAP-1000 P1 (finalize teacherId spoof + ADMIN blocked), GAP-1001 P2 (transcript semester/credit/studentName), GAP-1002 P1 (NULL-default unreachable + new-tenant provisioning). K12 SubjectGradeController secondary — defer.

**Walk fixtures (dev DB):** grade id=25 (student 4 class 14) finalized B+; teacher_classes (3,14,MAIN_TEACHER from KC-5); grading_scales seeded per-tenant.

G2 handoff: [`2026-06-05-g2-recipe-kc6-grade.md`](../../05-guides/operations/2026-06-05-g2-recipe-kc6-grade.md). Campaign §4: KC-6 → 🔄 walk-pass-pending-human.
