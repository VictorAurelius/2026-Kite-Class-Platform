# GAP-345: Wave 17 K-12 LEGAL Trio State-Check Audit

**Status:** 🟡 PARTIAL (this PR ships audit + 3 gap revisions; downstream fixes via revised gaps)
**Priority:** 🔴 P0 META — meta-gap that must close before Wave 18b plan can ship
**Domain:** Meta / Governance
**Detected:** 2026-05-04 (Wave 18b plan brainstorm — `audit-to-gap-pipeline.md` Step 2.5)
**Reviewer-Approver:** @nguyenvankiet (solo-dev — meta gap, self-approve per `audit-to-gap-pipeline.md`)

---

## Problem

Wave 17 Persona Review Round 1 (PR #748) filed GAP-321 + GAP-322 + GAP-323 (the K-12 LEGAL trio) for Stage 1 K-12 deployment. ROADMAP §🚀 Next Action 2026-05-04 recommends Wave 18b kicks off these 3 gaps.

When Wave 18b plan brainstorm started 2026-05-04, state-check (per `audit-to-gap-pipeline.md` Step 2.5) revealed **2 of 3 gaps mis-classify current code state** — claiming "fully greenfield" or "missing" for infrastructure that already shipped in earlier waves.

This is the **3rd recurrence** of the GAP-190 / GAP-197 anti-pattern (filed 2026-04-20 without state-check, required follow-up rewrite PR #396). Memory `feedback_gap_state_check_required.md` exists; rule `audit-to-gap-pipeline.md` Step 2.5 enforces; pre-existing precedent should have caught this.

If Wave 18b plan ships against these inaccurate gaps, agents will:
1. Try to "create from scratch" what already exists (GAP-321 Parent entity, GAP-323 SubjectSection / SubjectGrade)
2. Surface contradictions late (after PR opened)
3. Require rewrite PRs (precedent: GAP-190/197 → PR #396)

## Evidence

### Finding 1 — GAP-321 Parent Portal mis-classifies as "fully greenfield"

**Gap claim** (line 14-35):
> Parent role distinguished from generic user — ❌ missing — flat student/teacher/admin model
> Parent-student relationship entity — ❌ missing
> ...
> Result: no parent-portal scaffolding in code; GAP-052 file exists but no implementation.

**Actual state (verified 2026-05-04):**

| Piece claimed missing | Actually exists |
|----------------------|------------------|
| `Parent` entity | ✅ `kiteclass-core/module/parent/entity/Parent.java` since 2.14.0 (Wave 2 GAP-052a) |
| Parent-student relationship | ✅ `ParentStudentLink.java` with PRIMARY/SECONDARY linkType + UK constraint |
| Token-based parent invitation | ✅ `ParentInvitation.java` + `ParentInvitationService` |
| Migration | ✅ `V42__create_parent_portal_schema.sql` shipped Wave 2 — 3 tables (parents, parent_student_links, parent_invitations) |
| Multi-tenant isolation | ✅ V42 includes `instance_id` per BaseEntity |
| Sibling dedup | ✅ V42 `uk_parent_student UNIQUE (parent_id, student_id)` |
| Parent role in Gateway | ✅ `Gateway UserType.PARENT` exists + Parent.java javadoc references it |

**What's actually missing (GAP-321 should reflect):**
- Parent dashboard FE (no `/parent` route in `kiteclass-frontend`)
- 6 drill-down pages (transcript, attendance, fees, conduct, notifications, kỷ luật)
- Bulk import xlsx with Cha/Mẹ columns (depends GAP-325)
- Zalo OTP login flow
- Audit log per parent-side data view (BaseEntity audit exists, per-read log doesn't)
- PDPL Decree 13 Art 16 children-data parental consent flag tracking
- Phase 2 write actions (complaints, RSVP, absence excuse)
- Phase 3 multi-channel notification (depends GAP-063)

**Gap migration comment hint missed by author:** V42 explicitly says "Messaging, fee payment, attendance / grade widgets follow in Wave 5 — this migration is deliberately minimal." → GAP-321 IS that follow-on, not a from-scratch greenfield.

### Finding 2 — GAP-322 Child Protection state-check ambiguous (false-positive)

**Gap claim** (line 31-32):
> Result: zero matches — fully greenfield.

**Actual state (verified 2026-05-04):**

| Piece | Status | Note |
|-------|--------|------|
| `module/legal/` | ⚠️ EXISTS but unrelated | DMCA + Trademark only (`DmcaTakedownRequest.java`, `DmcaService.java`, `TrademarkCheckService.java`) — NOT child protection workflow |
| `module/moderation/` | ⚠️ EXISTS — content needs verification | Likely content moderation (AI), separate concern |
| `module/storage/` | ⚠️ has `child.protection` text-grep hit | Verified: false-positive — generic comment, not workflow |
| Safeguarding officer role | ❌ MISSING | confirmed |
| Encrypted incident entity | ❌ MISSING | confirmed |
| Staff vetting (LLTP) workflow | ❌ MISSING | confirmed |
| Mandatory reporting (Đ.51) | ❌ MISSING | confirmed |

**Verdict:** GAP-322 "fully greenfield" claim is technically correct for the workflow itself, but misleading because `module/legal/` exists with a confusable name. Gap should clarify: "legal module exists but contains DMCA/Trademark IP-protection, NOT child-protection — this gap is greenfield for safeguarding workflow specifically."

### Finding 3 — GAP-323 Period Attendance mis-classifies multi-subject + TT 22/2021 formula

**Gap claim** (lines 19-23):
> `Subject` entity multi-class — ⚠️ partial — single course-class link
> TT 22/2021 weighted formula (TX×1 + GK×2 + CK×3) — ❌ missing
> Gradebook UI for 12-15 môn / HS — ❌ missing K-12 layout

**Actual state (verified 2026-05-04):**

| Piece claimed missing | Actually exists |
|----------------------|------------------|
| Multi-subject infrastructure | ✅ `SubjectSection.java` (Lớp bộ môn) — HomeroomClass + Course + Teacher + schedule + weeklyHours since 3.15.0 (GAP-054) |
| Per-class subject grade | ✅ `SubjectGrade.java` — điểm của 1 HS cho 1 môn trong 1 học kỳ |
| TT 22/2021 weighted formula | ✅ `SubjectGrade.java` javadoc: "Average = (regular × 1 + midterm × 2 + final × 3) / 6" — IS TT 22/2021 |
| Curriculum entity | ✅ `Curriculum.java` exists |
| HomeroomClass | ✅ `HomeroomClass.java` exists |
| ClassScheduleSlot (structured weekly schedule) | ✅ `ClassScheduleSlot.java` GAP-099 Phase 1 with javadoc "Phase 2 future: iCal feed + attendance session generator" |
| Period dimension on Attendance | ❌ MISSING (still per-day model — gap correctly identifies this as primary blocker) |
| Tổ trưởng approval state machine | ❌ MISSING | confirmed |
| Mobile UI điểm danh ≤2 min | ❌ MISSING | confirmed |
| Multi-subject gradebook UI | ❌ MISSING | confirmed (data layer exists, FE doesn't) |

**Verdict:** GAP-323 is PARTIAL — the data model multi-subject scaffolding exists (Phase 1 of GAP-054 + ClassScheduleSlot from GAP-099), but the period-attendance dimension AND the FE gradebook UI AND the formula service (GradeFormulaService class) are missing. Gap should reframe as "extend existing multi-subject infrastructure with period dimension + formula service + UI", not "build from scratch".

## Root Cause

State-check in Wave 17 Bucket D agent prompt was insufficient. Agent ran 3 grep commands (lines 30-33 of GAP-321), used `head` truncation, and concluded "no implementation" without verifying:
1. Whether truncated output hid existing implementations
2. Whether existing infrastructure under different names (k12 module names "SubjectSection" not "Subject"; "ParentStudentLink" not "GuardianLink")
3. Whether prior Wave migrations already shipped the schema

Per memory `feedback_audit_grep_scope.md` — `head` truncation causes false-negatives. Per `feedback_gap_state_check_required.md` — must grep MULTIPLE patterns + read entity files + check migrations directory.

## Proposed Fix (this PR)

This PR ships:
1. **GAP-345 file** (this) — meta-gap documenting state-check audit findings
2. **GAP-321 revision** — Status 🔵 OPEN → 🟡 PARTIAL; replace "Current State" table with accurate version; reframe Proposed Fix as "build on Wave 2 GAP-052a Phase 1 foundation, add Phase 2-4 K-12 LEGAL scope"
3. **GAP-322 revision** — Add "Current State (verified 2026-05-04)" clarifying `module/legal/` is DMCA/Trademark (NOT confusable with child-protection workflow); workflow itself confirmed greenfield
4. **GAP-323 revision** — Status 🔵 OPEN → 🟡 PARTIAL; replace "Current State" table; reframe Proposed Fix as "extend existing GAP-054 + GAP-099 multi-subject + schedule infrastructure with period dimension + formula service + UI"

This PR does NOT ship code. Wave 18b plan is unblocked AFTER this PR merges + gap files reflect accurate state.

## Acceptance Criteria

- [ ] GAP-345 file created (this) with audit findings + verification commands
- [ ] GAP-321 revised: Status PARTIAL + accurate Current State + reframed Proposed Fix
- [ ] GAP-322 revised: Current State note clarifying legal module ≠ child protection
- [ ] GAP-323 revised: Status PARTIAL + accurate Current State + reframed Proposed Fix
- [ ] All 3 revised gap files cite this GAP-345 in their Log entries
- [ ] PR ships docs-only (no code, no migrations)
- [ ] Memory entry filed if non-obvious lesson surfaces (likely: extend `feedback_gap_state_check_required.md` with grep-head truncation note)

## Related

- **Caused by:** Wave 17 Bucket D agent (P5 K-12 review) insufficient state-check during gap filing
- **Anti-pattern recurrence:** GAP-190 (SEO) + GAP-197 (attendance calendar) — same pattern 2026-04-20, required PR #396 rewrite
- **Rule:** `audit-to-gap-pipeline.md` Step 2.5 (state-check before file)
- **Memory:** `feedback_gap_state_check_required.md`, `feedback_audit_grep_scope.md`
- **Blocks:** Wave 18b plan PR (cannot draft without accurate gap state)
- **Unblocks (after merge):** Wave 18b plan PR drafting

## Log

- **2026-05-04** — Filed during Wave 18b plan brainstorm. State-check found 2 of 3 K-12 LEGAL trio gaps mis-classify "greenfield" — Parent portal Phase 1 (GAP-052a) shipped Wave 2 with V42 migration + 3 entities + service; multi-subject infrastructure (SubjectSection + SubjectGrade w/ TT 22/2021 formula in javadoc) shipped GAP-054 Phase 1; ClassScheduleSlot shipped GAP-099 Phase 1. 3rd recurrence of state-check anti-pattern (precedent GAP-190/197 2026-04-20).
