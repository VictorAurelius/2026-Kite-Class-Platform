---
audience: dev
---

# GAP-868 — `/end-session` skill META formalization (additional scaffold items Wave 12+)

**Status:** 🟡 PARTIAL 20% (skill v1.1.0 đã ship cơ bản tại Wave 87 — handoff template + lock archive + docs-sync 5-target verify; META formalization gap này filed Wave local-doable-11 Bucket D để track 3 scaffold items follow-up Wave 12+)
**Priority:** 🟠 P1 META
**Domain:** Meta / Workflow
**Created:** 2026-06-02 (Wave local-doable-6 retro — pattern: session-end ad-hoc cycle consistently missed sync targets pre v1.1.0; v1.1.0 closed missing-handoff but 3 scaffold areas chưa solidify)
**Affects:** Every session-end moment; reproducibility + completeness của handoff doc quality; symmetry với `/start-session` orchestration depth
**Phase:** phase-1-beta

## Problem

`.claude/skills/workflow/end-session/SKILL.md` v1.1.0 (shipped pre-Wave-local-doable-11) đã cover:
- Step 0 docs-sync 5-target verify per `session-end-context-check.md` §4.5
- Step 1 lock detect
- Step 2 build summary
- Step 2.5 auto-write handoff note (template `reference/handoff-template.md`)
- Step 2.6 wave-history.jsonl append
- Step 3 archive lock
- Step 4 1-line summary VN
- Step 5 docs-only PR + propose `/clear`

Nhưng 3 scaffold areas chưa solidify (per Wave local-doable-6 retro observations):

1. **Handoff template improvements** — `reference/handoff-template.md` 6-section template chưa được iterate dựa trên usage data; thiếu placeholder coverage cho edge cases (multi-wave session, agent-only session không có user direction, partial wave close mid-session). Wave 12+ cần audit ≥10 real handoff notes shipped + refine template.
2. **Post-merge sync check sub-step** — `post-merge-sync-completeness.md` §2 4-target framework chưa fully wired vào `/end-session` Step 0 (chỉ 5-target verify per `session-end-context-check.md` §4.5 = list-only check). Wave 12+ cần build dedicated `scripts/check-post-merge-sync.sh` mirror `check-gap-status-csv.sh` pattern + invoke từ Step 0 mandatory.
3. **Context-budget recalibration sub-step** — `context-budget-mandate.md` §6.3 detector `scripts/check-context-budget.sh` đã ship (active CI gate) nhưng `/end-session` chưa surface context-budget delta session này (vd "session start 50k → session end 95k = +45k delta — recalibrate trước /clear"). Wave 12+ cần add Step 4.5 invoking `check-context-budget.sh --session-delta` + emit summary line.

Gap này KHÔNG re-implement skill (already v1.1.0); chỉ formalize Wave 12+ work scope cho 3 scaffold extensions trên + track via gap file (not chat memory only).

## Root Cause

Asymmetry historical:
- `/start-session` skill canonicalized 2026-04-20 (GAP-193 Phase 1) với `collect-state.sh` orchestration + multiple data sources
- `/end-session` Phase 2 GAP-193 shipped 2026-04-20 chỉ lock-archive + 1-line summary; v1.1.0 (~2026-05-19 Wave 100) added docs-sync 5-target + handoff per `session-end-context-check.md` v1.1.0 §4.5 mandate

3 scaffold gaps trên chưa file gap formal (chỉ exist trong retro chat) → drift risk; Wave 12+ session pickup khả năng miss without canonical gap tracking.

## Proposed Fix (Phase 1 = này wave; Phase 2+ = Wave 12+)

### Phase 1 (Wave local-doable-11 Bucket D — này PR)

- File gap GAP-868 với 3 scaffold items + AC + cross-link end-session SKILL.md v1.1.0
- CSV row added với status PARTIAL 20% (scaffold filed; impl deferred)
- SKILL.md v1.1.0 extended với inline `<!-- TODO Wave 12+ -->` markers tại 3 scaffold points để future readers thấy planned extensions
- Skills index entry already exists (line 102) — no change needed
- Audit completeness ≥10 handoff notes — defer Wave 12+ post real-data collection

### Phase 2 (Wave 12+ — concrete implementations)

Per `meta-gap-priority.md` §3 force-multiplier (META P1):

1. **Handoff template iteration** — audit ≥10 shipped handoff notes (`documents/03-planning/session-handoffs/`) → categorize template gaps → refine `reference/handoff-template.md` (target: 4 new placeholders covering multi-wave / agent-only / partial-close / blocker-cascade cases)
2. **`scripts/check-post-merge-sync.sh`** — bash script wrap 4-target `post-merge-sync-completeness.md` §2 verify (CSV / ROADMAP / wave-history / MEMORY) + emit PASS/FAIL/WARN; invoke từ SKILL.md Step 0 mandatory
3. **Context-budget delta surface** — extend `scripts/check-context-budget.sh` với `--session-delta` flag (read transcript file size start vs end) + invoke từ SKILL.md Step 4.5 + emit summary line "Context budget X% → Y% (+Δ tokens)"

### Phase 3 (Future — optional)

- Cron/hook integration (auto-invoke `/end-session` skill when context ≥85% per `session-end-context-check.md` §3 threshold table) — defer post Phase 2 stable

## Acceptance Criteria

### Phase 1 (this wave Bucket D)

- [ ] Gap GAP-868 filed phase-1-beta with §Problem + §Proposed Fix + §AC sections + cross-links
- [ ] CSV row added với status PARTIAL 20% completion_pct=20
- [ ] SKILL.md v1.1.0 extended với 3 inline `<!-- TODO Wave 12+ GAP-868 -->` markers tại Step 0 / Step 2.5 / Step 4.5 candidate positions
- [ ] Cross-link GAP-868 ↔ session-end-context-check.md §4.5 + start-session/SKILL.md + GAP-193 documented in §Related

### Phase 2 (Wave 12+ candidate — defer)

- [ ] ≥10 handoff notes audited + categorized template gaps
- [ ] `reference/handoff-template.md` refined với ≥4 new edge-case placeholders
- [ ] `scripts/check-post-merge-sync.sh` shipped + 4-target verify + self-test fixtures
- [ ] SKILL.md Step 0 invokes `check-post-merge-sync.sh` mandatory
- [ ] `scripts/check-context-budget.sh --session-delta` flag implemented
- [ ] SKILL.md Step 4.5 invokes context-budget delta surface
- [ ] All Phase 2 changes verified via test fixtures cross-session

### Phase 3 (Future — optional)

- [ ] Cron/hook auto-invoke pattern documented + opt-in mechanism

## Related

- `.claude/skills/workflow/end-session/SKILL.md` v1.1.0 — current skill state extending
- `.claude/skills/workflow/start-session/SKILL.md` — symmetric counterpart (Phase 1 of GAP-193)
- `.claude/rules/session-end-context-check.md` v1.1.0 §4.5 — 5-target docs-sync mandate (Step 0 source)
- `.claude/rules/post-merge-sync-completeness.md` §2 — 4-target framework (Phase 2 sub-script target)
- `.claude/rules/context-budget-mandate.md` v1.1.0 §6.3 — `check-context-budget.sh` detector (Phase 2 `--session-delta` extension target)
- `.claude/rules/meta-gap-priority.md` §3 — META P1 force-multiplier reasoning
- `GAP-193` — parent gap (Phase 1 + Phase 2 end-session lock-archive closed by Wave Meta Phase-2 Cleanup)
- Wave plan `documents/03-planning/waves/wave-2026-06-02-local-doable-11-zalo-sms-infra.md` Bucket D — filed scope reference

## Log

- **2026-06-02:** Gap filed Wave local-doable-11 Bucket D — Phase 1 scaffold (gap file + CSV row + SKILL.md TODO markers). META formalization track 3 scaffold extensions (handoff template iteration + post-merge sync sub-script + context-budget delta surface) deferred Wave 12+. Status PARTIAL 20% per `gap-done-discipline.md` §3 PARTIAL exit ramp — main scaffold filed, concrete Phase 2 implementations follow-up wave.
