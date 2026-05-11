# GAP-193: Session Orchestration & Start-Session Skill

**Status:** 🟢 DONE 2026-04-29 (Phase 1 + Phase 2 closed)
**Priority:** 🟠 P1 (meta tier — quality-of-work enabler)
**Domain:** Meta / Skills / Workflow
**Found:** 2026-04-20 (action-1 §12 + §15.D)
**Wave:** Wave 8b (meta) — front of meta cluster
**Affects:** Every Claude Code session in this repo, parallel agent safety

## Problem

No structured way to start a session with correct context or detect multi-session conflicts:

- New sessions manually re-load CLAUDE.md, ROADMAP, active wave — inconsistent, lossy
- No detection that 2+ sessions are editing the same wave branch / same gap files
- No session-level lock file — risk of concurrent ROADMAP edits producing divergent state
- Context-degraded sessions (high token usage) are not flagged before critical work starts (related: GAP-199 rework audit)

User questions (action-1 lines 607–627):
- "bắt đầu session mới như thế nào"
- "nhiều session cùng lúc được không"
- "cần kiểm soát session tránh conflict"

## Context

Meta-P1 because every session pays the cost of missing context. Related rules:
- `.claude/rules/skill-conventions.md` (how to write the skill)
- `.claude/rules/output-review-mandate.md` (sessions produce outputs)

## Proposed Fix

1. **Skill**: `.claude/skills/workflow/start-session/SKILL.md`
   - Checklist: load CLAUDE.md digest + current wave doc + ROADMAP header + open PRs + failing CI
   - Output: single summary block "Active wave: X, open PRs: Y, blockers: Z"
   - Trigger phrases: "/start-session", "start new session", "what's the state?"
2. **Session lock file**: `.claude/session-locks/` directory
   - Each session creates `session-{timestamp}-{hostname}.lock` with active branch + gap IDs being edited
   - New session reads locks, warns if overlap
   - Locks cleared at session end (hook) or stale after 4h
3. **Context-degradation indicator**
   - Simple heuristic: turn count > 40 or last `/compact` > 2h ago → flag
   - Skill nudges user toward /clear + new session if critical work pending
4. **Integration** with `/continue` skill (already exists) — handoff logs

## Acceptance Criteria

### Phase 1 (DONE)
- [x] `start-session` skill in `.claude/skills/workflow/start-session/` with SKILL.md ≤100 lines
- [x] Session lock convention documented (`.claude/session-locks/README.md` + reference/context-template.md §Lock File Schema)
- [x] Overlap-detection routine spec'd (reference/context-template.md §3 Overlap Detection)
- [x] Context-degradation heuristic documented (SKILL.md §Context-degradation heuristic)
- [x] Skill registered in `.claude/skills/_README-skills-index.md`
- [x] Example runs documented (reference/context-template.md §4 Example Output Scenarios)
- [x] `scripts/collect-state.sh` smoke-tested in feat/wave-8b-E branch

### Phase 2 (DONE 2026-04-29 — Wave Meta Phase-2 Cleanup Agent A)
- [x] Hook enforcement blocking commits on locked branch from different session
- [x] Session-lock archival on session-end for retro analysis
- [x] Turn-count telemetry recorded in `PR-{N}.json` via audit-gate.py
- [x] `/end-session` skill for explicit lock release

## Out of Scope

- Actual parallel-agent coordination beyond single-user Claude Code (covered by `feedback_parallel_agent_strategy.md` memory)
- Enforcement via hook — Phase 2 after skill proves useful

## Related

- action-1 §12 + §15.D
- `.claude/rules/skill-conventions.md`
- `.claude/skills/workflow/continue/` (existing)
- Memory `feedback_parallel_agent_strategy.md`
- GAP-199 rework audit for context-degraded PRs (sister concern)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P1)

## Log

- 2026-04-20 — Created from action-1 §15.D.
- 2026-04-20 — Phase 1 CLOSED via Wave 8b Agent E. Delivered: `.claude/skills/workflow/start-session/{SKILL.md, reference/context-template.md, scripts/collect-state.sh}` + `.claude/session-locks/README.md` + skills index entry + `.gitignore` rule for lock files. Smoke test passed. Phase 2 (hook enforcement, telemetry) deferred to follow-up PR.
- **2026-04-29** — Phase 2 CLOSED via Wave Meta Phase-2 Cleanup Cluster 7 Agent A (PR #TBD). 7 deliverables shipped: (1) `.claude/hooks/session-lock-guard.py` — branch-lock enforcement (exit 1 on foreign-session conflict), auto-purges stale locks (>4h); (2) `.claude/hooks/audit-gate.py` extended with `build_session_telemetry()` + `run_session_lock_guard()` helpers, `session_telemetry` block now in every `PR-{N}.json` (session_id always; session_started_at + turn_count best-effort); (3) `.claude/skills/workflow/end-session/SKILL.md` + `scripts/end-session.sh` (helper) — Vietnamese 1-line summary per CLAUDE.md §CRITICAL; (4) `.claude/session-locks/README.md` updated with archival path `.claude/session-locks/archived/YYYY-MM-DD/` + 30-day retention; (5) `.claude/skills/_README-skills-index.md` registers `/end-session`; (6) `.claude/hooks/tests/test-session-lock-guard.sh` smoke test — 3 scenarios (foreign-active blocks; own-only passes; stale-foreign auto-purged) + verify-purge assertion = 4 PASS / 0 FAIL; (7) gap closure verified via `gap-done-discipline.md` §2 banned-phrase scan clean. Verification environment: WSL2 worktree `agent-aa85b54b` on branch `feat/wave-meta-p2-gap-193-session-lock-hook`; lint pass `ruff check` clean + `shellcheck` clean.
