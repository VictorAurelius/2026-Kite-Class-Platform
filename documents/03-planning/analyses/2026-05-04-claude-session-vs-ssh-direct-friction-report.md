---
title: Claude-session vs SSH-direct friction report — GAP-284 hotfix retro
status: complete
created: 2026-05-04
gaps: [GAP-284, GAP-285]
---

# Claude-session vs SSH-direct — friction observed during GAP-284 hotfix

**Context:** GAP-284 (Docker pnpm workspace context mismatch) shipped 2026-05-04 via PR #737. Single-session diagnose → fix → verify → merge. Triage took longer than the underlying fix. This report captures the friction observed and recommends when to bypass Claude session for SSH-direct terminal work.

---

## 1. Friction observed (this session)

| # | Issue | Cost | Root cause |
|---|---|---|---|
| 1 | 2× foreground `docker buildx build` (~3 min each) | ~6 min cache pressure on parent context | Skill defaulted to foreground Bash instead of `run_in_background: true` + Monitor |
| 2 | Sleep+poll smoke test (`sleep 6 && curl`) | Bash tool docs explicitly forbid this; small but rule violation | Habit pattern from old `gh pr checks` polling |
| 3 | Monitor regex bug exited prematurely (matched `.` across pipe separators) | 1 wasted Monitor cycle, restart needed | Quick-write Monitor script without testing the filter |
| 4 | Dockerfile change broke `kitehub-frontend-ci.yml` Docker job (incidental coverage regression) | Required follow-up commit + push + re-monitor cycle | Fixed Dockerfile, didn't grep for ALL workflows referencing it |
| 5 | Pre-existing `AdminControllerTest.testGetRevenue` failure surfaced on PR | Triage time to confirm out-of-scope, file GAP-285 | Solo-dev mode removed `push: main` from test workflows → no main-baseline runs to confirm pre-existing |

Net: ~25-30 min triage friction on top of ~10 min real fix work.

---

## 2. Why Claude-session amplifies these issues

- **Round-trip latency:** every tool call + reply is a chat turn. CI polling, Docker builds, multi-step verifications all become Monitor + notification cycles. SSH-direct = run-and-watch in one shell.
- **Foreground bash blocks parent context cache:** 5-min cache TTL means a 3-min build burns the cache window if foregrounded. Background mode helps, but adds Monitor scripting overhead. SSH-direct: shell waits, you read output, no cache concern.
- **Rule overhead is real:** `feedback_*.md` files codify N rules. Each Bash call must be checked against scripts-rule, background-rule, Monitor-rule, etc. SSH-direct: developer judgment in the loop, no rule serialization required.
- **State-checks across turns drift:** a session takes 90+ minutes; system state (CI runs, branches, processes) evolves. Every fresh check costs a tool call + chat turn.
- **Multi-context juggling is noisy:** when a PR fix accidentally touches a sibling workflow + surfaces a pre-existing flake + needs gap filing + memory entry, the session timeline becomes 6-8 interleaved threads.

---

## 3. When SSH-direct is the right call

Switch to direct SSH terminal (skip Claude session) when ALL of:

- Task is **infra/ops-heavy** (Docker, k8s, CI, dev-stack debug)
- Verification cycle is **>30s per iteration** (build, deploy, smoke test)
- Steps are **linear + visible** (read log → adjust → re-run; no branching decisions needing context)
- Outcome is **measurable in shell** (exit code, HTTP status, file presence)

Examples this session that fit SSH-direct better:
- Local Docker build verification (the 2× `docker buildx build` calls)
- Smoke test (`docker run + curl`)
- Polling 1 specific CI job to terminal state

Examples that stayed correct in Claude session:
- Reading + writing the Dockerfile + workflow YAML edits (multi-file context, regex-precise edits)
- Filing GAP-284 / GAP-285 with proper structure + cross-references
- Drafting the PR body with full context
- Merge decision discussion

---

## 4. Recommended hybrid workflow

```
[Claude session]                          [SSH terminal]
  ↓                                          ↓
  Diagnose root cause                        ─
  Read failing log + grep code               ─
  Edit Dockerfile / workflow / configs       ─
  Stage commit                               ─
  ─                                          docker buildx build (verify)
  ─                                          docker run + curl (smoke)
  ─                                          gh pr checks <N> (poll)
  Open PR (gh pr create)                     ─
  ─                                          Watch CI in shell loop
  Decide merge                               ─
  ─                                          gh pr merge
  Update gap closure docs                    ─
  Open closure PR                            ─
```

Decision rule: anything that's a `for/while` loop or `until <state>` should run in SSH. Anything that's a state-aware decision should stay in Claude session.

---

## 5. Action items

- [x] Memory entry `feedback_local_verification_discipline.md` saved (this session, 2026-05-04) — codifies scripts/background/Monitor rules
- [ ] Add `kitehub/scripts/verify-frontend-docker.sh` helper (TODO future PR — single-Dockerfile verify command, currently no script)
- [ ] Consider documenting "when to drop into SSH" in `.claude/skills/workflow/start-session/reference/` so future sessions choose the right modality up front
- [ ] Consider grep-all-workflows audit step in any Dockerfile-change PR template (would have caught the kitehub-frontend-ci.yml regression pre-push)

---

## 6. Counter-balance — what Claude session DID well

Not all retro is negative. This session also:

- Caught the kitehub-frontend-ci.yml regression within minutes via Monitor (would have taken longer SSH-only without structured tracking)
- Filed GAP-284 + GAP-285 with proper cross-references in real-time
- Drafted PR body + commit messages that survived review
- Updated memory entries that compound across future sessions (the rule violations don't repeat)
- Verified post-merge state via Monitor while user reviewed in parallel

The friction is real but the structured artifacts produced are higher quality than ad-hoc SSH work.

---

## 7. Conclusion

Claude session is best for **decisions + artifacts**. SSH-direct is best for **verification loops + linear ops sequences**. Neither replaces the other; this session's friction came from defaulting to Claude session for ops-heavy work that should have been SSH'd through.

Next ops-heavy task: consider opening SSH terminal first, doing the verify cycles there, then returning to Claude only for artifact creation (gap files, PR body, memory updates).
