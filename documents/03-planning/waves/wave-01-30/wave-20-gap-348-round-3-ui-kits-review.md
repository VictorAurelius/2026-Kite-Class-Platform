---
title: Wave 20 — GAP-348 Round 3 UI Kits Persona-Driven Review
status: complete
created: 2026-05-05
updated: 2026-05-05
waves: [20]
gaps: [GAP-348]
---

# Wave 20 — GAP-348 Round 3 UI Kits Persona-Driven Review

**Goal:** External `/128` review of Round 3 kits (`kiteclass-student` 13 screens + `kitehub-admin` 12 screens) — quality gate before Track 2 Phase 2 production port.
**Trigger:** Round 3 merged 2026-04-29 with agent self-report only (student avg 116/128 ⭐⭐, admin avg 107.2/128). Per `feedback_audit_calibration.md` self-audit overstates 15-20pts vs external. Track 2 Phase 2 (GAP-269 student port + GAP-271 admin port) blocks on external review per GAP-348 AC last bullet.
**Estimated wall-clock:** ~3-4h agent work, longest-bucket ~90 min (parallel A+B), coordinator synthesis Part C ~30 min.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- **S. Student persona** — kiteclass-student kit serves Tier-1 Student (mobile-first, 13 screens: today, classes, assignments, grades, attendance, payments, notifications, profile, login, empty-states + 3 detail views). No top-level `S-student.md` AC doc, but secondary docs `student-in-P2.md`/`student-in-P3.md`/`student-in-P5.md` exist. Wave 20 surfaces gap if Tier-1 doc needed (Part C → potential follow-up gap).
- **P5 K-12 School Principal persona** — kitehub-admin kit serves Tier-1 P5 (12 screens: dashboard, academic-calendar, fees, conduct, multi-class-roster, parent-comms, report-cards, school-profile, teacher-management, bulk-import, login, empty-states). Cross-link with `P2-small-center-round-1-2026-05-04.md` (admin overlaps P2 + P5 personas).

**Q2 (trade-offs):**
- **Alternative considered:** Skip external review, trust agent self-report → REJECTED per `feedback_audit_calibration.md` (16-20pt overstatement) + GAP-348 P1 quality-gate framing.
- **Alternative considered:** Single-agent serial review (A → B → C) → REJECTED per `feedback_wave_plan_before_serial_prs.md` (3 disjoint sub-tasks = wave-pack candidate; 90 min serial vs ~45 min parallel).
- **Alternative considered:** Include Track 2 port execution in same wave → REJECTED — review must close BEFORE port (per AC: "follow-up kit polish gap filed BEFORE Track 2 port for that kit can start").

**Q3 (risks):**
- **R1 — Calibration drift:** External agent may also overstate vs human reviewer. Mitigation: bucket prompt enforces explicit `/128` rubric per dimension (visual / VN UX / mobile-PWA / states / a11y / consistency / dark-mode / persona-fit) with citing evidence; no aggregate-only scoring.
- **R2 — Persona mapping incomplete:** S. Student has no Tier-1 doc. Mitigation: Bucket A uses `secondary/student-in-P2.md` as proxy + Part C files follow-up gap if needed.
- **R3 — Kit-level gap files spawned at scale:** if external avg < 95 floor, multiple polish gaps may be needed. Mitigation: Part C batches per-kit (1 gap per kit, not per-screen) with screen-level table inside.
- **R4 — Worktree contamination from parallel agents:** Mitigation: per `agent-background-spawn-default.md` + `feedback_worktree_absolute_path_contamination.md`, agents use RELATIVE paths only, separate worktree dirs.

---

## 2. Task Breakdown

| Bucket | Gap part | Owner | Effort | Disjoint? |
|--------|----------|-------|--------|-----------|
| A | GAP-348 Part A — kiteclass-student review | bg-agent | ~75 min | ✅ writes only `audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md` |
| B | GAP-348 Part B — kitehub-admin review | bg-agent | ~75 min | ✅ writes only `audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md` |
| C | GAP-348 Part C — synthesis + README + persona gap | coordinator (this session, after A+B merge) | ~30 min | sequential — depends on A+B output |

Disjoint check: Bucket A writes only to `kiteclass-student-review.md`; Bucket B writes only to `kitehub-admin-review.md`. Zero file overlap. Coordinator (Part C) updates 2 different files (`ui_kits/README.md` + GAP-348 status) after both merge.

---

## 3. Scope (per bucket)

### Bucket A — kiteclass-student external review

- **Read-only inputs:**
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/*.html` (13 screens)
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/README.md` (kit-level doc + self-report scores)
  - `.claude/skills/quality/ui-review/SKILL.md` (rubric definition `/128`)
  - `documents/04-quality/audits/ui-review/2026-04-29-wave-1-add-ons-review.md` (template — sister review report format)
  - `documents/00-brd/persona-criteria/secondary/student-in-P2.md` (S. Student persona AC proxy — gap notes no Tier-1 doc)
- **Writes (1 file):**
  - `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md`
- **Acceptance (Bucket A subset):**
  - All 13 screens scored × 8 dimensions /128 each (visual / VN UX / mobile-PWA / states / a11y / consistency / dark-mode / persona-fit)
  - Per-screen scores cite specific HTML evidence (line numbers / element IDs / VN copy samples)
  - Each screen mapped to S. Student persona journey (Today / Classes / Assignments / Grades / Notif / Profile / Payments)
  - Aggregate kit avg + delta vs self-report (e.g., "self-report 116, external 102, delta -14")
  - Kit-level findings section: WCAG measurements present? dark-mode parity? mobile 320 viewport? VN data realism?
  - Per-screen verdict column: ⭐⭐⭐⭐ excellent / ⭐⭐⭐ good / ⭐⭐ needs polish / ⭐ rebuild
  - Bottom section: list of screens scoring <95 (kit-level floor) — flagged for follow-up gap by Part C

### Bucket B — kitehub-admin external review

- **Read-only inputs:**
  - `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/*.html` (12 screens)
  - `documents/02-architecture/design-system/ui_kits/kitehub-admin/README.md`
  - `.claude/skills/quality/ui-review/SKILL.md`
  - `documents/04-quality/audits/ui-review/2026-04-29-wave-1-add-ons-review.md` (template)
  - `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` (P2 Center Owner AC mapping — admin kit serves P2)
  - `documents/00-brd/persona-criteria/P5-k12-school.md` (P5 K-12 Principal AC — primary persona)
- **Writes (1 file):**
  - `documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md`
- **Acceptance (Bucket B subset):**
  - All 12 screens scored × 8 dimensions /128
  - Per-screen scores cite specific HTML evidence
  - Each screen mapped to P2 Center Owner journey AND P5 K-12 Principal journey (admin = dual-persona)
  - Cross-reference P2 round-1 review evidence pointers (which P2 ACs the kit attempts)
  - Aggregate avg + delta vs self-report 107.2
  - Kit-level findings + per-screen verdict + <95 floor list

### Bucket C (coordinator, sequential after A+B merge)

- **Inputs:** Bucket A + B reports
- **Writes (3-4 files):**
  - `documents/02-architecture/design-system/ui_kits/README.md` (Round 3 row updated with external scores + delta)
  - `documents/04-quality/gaps/GAP-348-round-3-ui-kits-persona-driven-review.md` (status flip per `gap-done-discipline.md` §2 or §3 PARTIAL)
  - `documents/04-quality/gaps/GAP-XXX-kit-polish-*.md` (1 follow-up gap per kit if avg <105)
  - `documents/04-quality/gaps/GAP-XXX-student-tier1-ac-doc.md` (if Bucket A flags missing Tier-1 doc as blocker)
  - `documents/04-quality/gaps/ROADMAP.md` (snapshot + counts update)
  - `.claude/data/wave-history.jsonl` (Wave 20 entry per `feedback_wave_history_append_required.md` Rule N)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `documents/02-architecture/design-system/ui_kits/kiteclass-student/` | Folder | `ls -d documents/02-architecture/design-system/ui_kits/kiteclass-student` | folder exists | ✅ exists |
| `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/` | Folder + 13 HTML files | `ls .../kiteclass-student/screens/` | 13 files (assignment-detail, assignments, attendance, class-detail, empty-states, grade-detail, grades, login, my-classes, notifications, payments, profile, today) | ✅ exists (gap claimed 14, actual 13 — corrected in §3) |
| `documents/02-architecture/design-system/ui_kits/kitehub-admin/` | Folder | `ls -d ...` | folder exists | ✅ exists |
| `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/` | Folder + 12 HTML files | `ls .../kitehub-admin/screens/` | 12 files (academic-calendar, bulk-import, conduct, dashboard, empty-states, fees, login, multi-class-roster, parent-comms, report-cards, school-profile, teacher-management) | ✅ exists (gap claimed 10, actual 12 — corrected in §3) |
| `.claude/skills/quality/ui-review/SKILL.md` | Skill file | `ls .claude/skills/quality/ui-review/SKILL.md` | 8.7K file | ✅ exists |
| `documents/04-quality/audits/ui-review/2026-04-29-wave-1-add-ons-review.md` | Template review report | `ls documents/04-quality/audits/ui-review/2026-04-29-wave-1-add-ons-review.md` | 16.6K file | ✅ exists |
| `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` | P2 persona review | `ls .../P2-small-center-round-1-2026-05-04.md` | 25.6K file | ✅ exists |
| `documents/00-brd/persona-criteria/P5-k12-school.md` | P5 Tier-1 AC doc | `ls .../P5-k12-school.md` | 49.7K file | ✅ exists |
| `documents/00-brd/persona-criteria/secondary/student-in-P2.md` | S. Student secondary AC | `ls .../secondary/student-in-P2.md` | 20.8K file | ✅ exists (proxy for Tier-1; gap claim "no Tier-1 doc" confirmed — `S-student.md` not at top level) |
| `documents/00-brd/persona-criteria/S-student.md` | Tier-1 Student AC doc | `ls .../persona-criteria/S-student.md` | 0 matches at top level | ❌ absent — Part C may file follow-up gap (NOT in Wave 20 scope to create) |
| `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md` | Output review file | `ls .../2026-05-05-round-3-kiteclass-student-review.md` | 0 matches | 🆕 to-be-created (Bucket A) |
| `documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md` | Output review file | `ls .../2026-05-05-round-3-kitehub-admin-review.md` | 0 matches | 🆕 to-be-created (Bucket B) |
| `feedback_audit_calibration.md` (memory) | Calibration heuristic | auto-loaded per session | "self-audit overstates 15-20 pts" | ✅ exists (memory) |
| `gap-done-discipline.md` §2/§3 | Closure rule | `ls .claude/rules/gap-done-discipline.md` | exists | ✅ exists |

Banned shortcuts (mirror §2.5):
- `| head` truncation on grep/find — NONE used
- Skipping verification "because agents will check at execution" — NONE skipped
- Aspirational references without 🆕 flag — `S-student.md` flagged ❌ absent (NOT created this wave)

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | None (markdown-only output, no code) — coordinator reviews report completeness | None — markdown PR |
| B | None (markdown-only output) | None — markdown PR |
| C | `bash scripts/check-rule-frontmatter.sh && bash .claude/skills/workflow/session-docs-check/scripts/check-docs.sh --branch=<branch>` (closure PR includes ROADMAP + GAP files + wave-history.jsonl) | `readme-freshness` if README touched (it will be — Round 3 row update) |

No mvn/pnpm needed — pure documentation wave.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` v1.0.0:

- All buckets spawned with `run_in_background: true` (default per rule)
- `isolation: worktree` for parallel safety (separate worktree dirs)
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md` (no `/home/nguyenvankiet/...` absolute paths in prompts)
- Agent type: `general-purpose` (markdown writing + multi-file analysis; not code)
- Coordinator (this session, after A+B PRs merge) executes Part C as closure PR

**Bucket A prompt skeleton (relative paths):**
```
Read these files (relative paths from repo root):
- documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/*.html (13 screens)
- documents/02-architecture/design-system/ui_kits/kiteclass-student/README.md
- .claude/skills/quality/ui-review/SKILL.md (rubric)
- documents/04-quality/audits/ui-review/2026-04-29-wave-1-add-ons-review.md (template)
- documents/00-brd/persona-criteria/secondary/student-in-P2.md (persona)

Write ONE file: documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md
... [scoring rubric, format spec, AC list]
```

**Bucket B prompt skeleton:** parallel structure for kitehub-admin.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md`:

- Each bucket PR (A and B): markdown report only; no GAP file changes (those happen in closure)
- Closure PR (Part C):
  - GAP-348 status flip per §2/§3 (DONE if all AC verified, PARTIAL if any deferral; deferral = follow-up gap filed)
  - `ui_kits/README.md` Round 3 row updated with external scores + delta
  - ROADMAP §🚀 Next Action update (Wave 20 SHIPPED, counts adjusted)
  - `wave-history.jsonl` append (Rule 15 enforcement)
  - 1-2 follow-up gaps filed if external avg <105 per kit (per AC)
  - Optional: GAP-XXX-student-tier1-ac-doc.md filed if Bucket A flags Tier-1 absence as blocker

Per `gap-done-discipline.md` §3: if any AC genuinely deferred (e.g., Tier-1 Student AC doc creation not in scope), GAP-348 flips to 🟡 PARTIAL with explicit follow-up gap citation in Log.

---

## 8. Log

- **2026-05-05 (complete):** Wave SHIPPED — 4 PRs (#802 plan + #803 Bucket A + #805 Bucket B + #806 closure). GAP-348 → 🟡 PARTIAL per `gap-done-discipline.md` §3 (polish work deferred to follow-ups). 3 follow-up gaps filed: GAP-363 (P1 BLOCKING — kiteclass-student polish, payments persona violation), GAP-364 (P2 — kitehub-admin polish, school-profile rebuild + 5 items), GAP-365 (P2 BL — Tier-1 S-student.md AC doc). Calibration validated: Bucket A delta -15.6 in band; Bucket B delta -6.1 below band (kit's WCAG ratios + MoET citations + VN K-12 mock data justify). Track 2 ports GAP-269/271 BLOCKED until polish closes. Wall-clock ~3-4h. 29th consecutive 0-clarification wave-pack.
- **2026-05-05** (draft): Plan created. State-check passed (12 verified ✅, 1 ❌ absent flagged for Part C follow-up, 2 🆕 to-be-created by Buckets A+B). Triggered by user request post-PR #801 merge "trigger luôn GAP-348".
EOF
