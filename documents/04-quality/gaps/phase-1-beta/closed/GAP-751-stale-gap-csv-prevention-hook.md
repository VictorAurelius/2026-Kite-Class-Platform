---
title: Stale-gap CSV prevention — auto-detect hook + pre-wave-plan check
status: 🔵 OPEN
priority: 🟠 P1
domain: Meta
phase: phase-1-beta
created: 2026-05-26
---

# GAP-751: Stale-gap CSV prevention — PostMerge auto-detect hook + pre-wave-plan stale-check

**Status:** 🟢 DONE 100%
**Priority:** 🟠 P1 — META P0 force-multiplier per `meta-gap-priority.md` §3
**Domain:** Meta (Hook + Skill governance)
**Found:** 2026-05-26 (Wave br-7 closure retro — 4/5 buckets state-check phát hiện code đã shipped Wave 5 Sub-PR 5.6b era)
**Affects:** Mọi wave subsequent — eliminate ~5-10 wasted agent spawns per quarter (~5-10h/quarter token cost)

## Problem

Pattern recurring trong Wave br-7 closure:
- 4/5 buckets (GAP-215/216/217/218) state-check phát hiện code ĐÃ shipped Wave 5 Sub-PR 5.6b era (~30 ngày trước)
- Gap CSV vẫn OPEN P0 → wave plan spawn agents → agents discover stale → flip CSV inline → waste agent spawn cost
- Pattern recurrence ≥4 lần trong 1 wave = systemic governance gap

Root causes:
1. Không có **commit-time auto-detect**: PR merge code touching gap scope không tự liên kết CSV flip
2. `audit-to-gap-pipeline.md` §2.8 state-check **AT FIX TIME** = reactive (đã waste spawn)
3. `gap-done-discipline.md` §2 enforce **AT CLOSURE PR** (manual)
4. Wave plan spawn KHÔNG có pre-spawn stale-check mandatory step

## Root Cause

Multiple existing rules cover RELATED scope but không cover prevention direction:
- `audit-to-gap-pipeline.md` §2.5/§2.6/§2.7/§2.8 cover filing/planning/decision-doc/fix-time state-check
- `gap-done-discipline.md` covers closure mechanics
- `post-merge-sync-completeness.md` §2 mandates CSV sync khi status flip — but không trigger auto-flip
- Missing: **commit→CSV auto-link mechanism** (Option A) + **wave-plan pre-spawn stale-check** (Option B)

## Proposed Fix

User chốt 2026-05-26: **A+B combined approach** (highest ROI).

### Option A — PostMerge hook auto-detect (~3-4h build)

1. Extend `audit-gate.py` hook OR new `.husky/post-merge` hook:
   - Scan PR body for patterns `Closes: GAP-NNN`, `Refs: GAP-NNN`, `Resolves: GAP-NNN`
   - For each matched GAP-NNN:
     - Read CSV row → if status != DONE, propose flip
     - Auto-flip CSV: OPEN/PARTIAL → DONE 100% + update `last_verified` date
     - Append closure Log entry to gap markdown file
     - git mv to `phase-{X}/closed/`
   - Emit PR comment: "Auto-flipped GAP-NNN per body marker"
2. Extend `.github/PULL_REQUEST_TEMPLATE.md` Output Review Checklist:
   - New row: `- [ ] **Closes GAP-NNN(s)** — list each gap closed by this PR using "Closes: GAP-NNN" syntax in PR body`
3. Cross-reference `gap-done-discipline.md` §2 — hook satisfies criterion 1 (AC checkbox flip) automatically; criterion 5 (verification artifact) still manual

### Option B — Pre-wave-plan stale-check mandatory step (~1h build)

1. Extend `.claude/skills/quality/wave-pack-planner/SKILL.md` (or reference) — add new section "Step N: Pre-spawn stale-check":
   - Before spawn agents, batch state-check all gap-IDs in wave scope
   - For each gap-ID, grep CSV vs code paths matching gap §Problem
   - If code paths exist → flag as "potentially stale" → coordinator inline flip BEFORE spawn (waste prevention)
2. Cross-reference `audit-to-gap-pipeline.md` §2.8 — wave-plan-time state-check complements fix-time state-check
3. Pair với pre-spawn worked self-test on Wave audit-stale-sweep (#7)

## Acceptance Criteria

- [ ] PostMerge hook implementation in `.claude/hooks/` OR `.husky/post-merge` scans PR body markers
- [ ] Hook auto-flips CSV + appends Log entry + git mv to closed/
- [ ] PR template checkbox added per Option A step 2
- [ ] `wave-pack-planner` SKILL.md adds Step "Pre-spawn stale-check"
- [ ] Worked self-test on Wave audit-stale-sweep (#7) — apply Option B mandatory step demonstrate fires correctly
- [ ] Detector self-test: synthetic PR with `Closes: GAP-NNN` body → CSV flip verified
- [ ] Documentation update: `gap-done-discipline.md` Related section cross-link to GAP-751

## Related

- **`audit-to-gap-pipeline.md`** §2.5/§2.6/§2.7/§2.8 — sister state-check pipeline; this gap extends to commit-time auto-detect direction
- **`gap-done-discipline.md`** §2 — closure mechanics; Option A automates criterion 1+5
- **`post-merge-sync-completeness.md`** §2 — 4-target sync; Option A automates CSV target
- **`gap-architecture-v2.md`** §3 — CSV canonical; this gap reinforces CSV as auto-derived source of truth
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier classification
- **Wave audit-stale-sweep (Task #7)** — companion wave; Option B applies trong sweep self-test
- **Wave br-7 closure** (this session 2026-05-26) — incident triggering this gap; 4/5 buckets stale recurrence

## Log

- **2026-05-26 (filed):** Filed per Wave br-7 closure retro 2026-05-26 — 4/5 buckets (GAP-215/216/217/218) state-check phát hiện code đã shipped Wave 5 Sub-PR 5.6b era ~30 ngày trước; pattern recurrence ≥4 lần trong 1 wave = systemic. User chốt A+B combined approach per cost-benefit (eliminate ~5-10 wasted agent spawns per quarter = ~5-10h/quarter token cost saved; one-time build ~5h). Queued companion với Wave audit-stale-sweep (Task #7) next session priority.

- **2026-05-26 (Wave meta-6 closure):** Flipped DONE 100% — A+B combined shipped. Option A audit-gate.py auto_close_referenced_gaps() function + PR template + 10/10 unit tests in PR #1849 (merged b0180294). Option B wave-pack-planner SKILL.md Step 4.7 Pre-spawn stale-check in PR #1848 (merged 6247a439). Filing in PR #1847 (merged bde8179e). LIVE TEST: bg-agent local hook fired during PR #1847 merge — verified working. CSV row updated + file moved to phase-1-beta/closed/ per `gap-folder-organization.md` v2.0.0 §3.3.
