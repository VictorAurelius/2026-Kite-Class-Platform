# Session Handoff — 2026-06-11 — Meta: worktree-dup-load fix + end-session gate

Session focus: demo-seed-1 follow-on → CSP logo fix → 2 META improvements (worktree duplicate-rule-load + end-session working-tree gate) + session-close cleanup. Worktree workflow dogfooded (sibling-outside-repo per new rule v1.1.1).

## 1. Scope shipped

| PR | Title | Status |
|---|---|---|
| #2321 | fix(kc-frontend): CSP img-src dev-override MinIO logo (GAP-1198) | ✅ MERGED |
| #2322 | rule(worktree-only) v1.1.1 + PreToolUse in-repo-worktree detector | ✅ MERGED |
| #2323 | docs(gap): GAP-1198 Log ref #2321 (drift sync) | ✅ MERGED |
| #2324 | skill(end-session) v1.2.0: working-tree clean + sync gate (Step 0a) | ✅ MERGED |
| #2325 | chore(session-close): land pr-logs + gitignore *.reserved + handoff | ⏳ this PR |

No wave-level scope (meta/fix cluster) → no wave-history.jsonl entry.

## 2. Gaps DONE / improved / NEW filed

- **GAP-1198** (NEW + DONE) — kiteclass-frontend CSP missing devImg dev-override (sister of GAP-1112). P3 Frontend. Archived `phase-1-beta/closed/`.
- No other gaps touched.

## 3. Lessons captured (session-internal, non-rule-class)

- **In-repo worktree = duplicate rule auto-load.** `.claude/worktrees/wt-*/` nested `.claude/` → harness loads CLAUDE.md + ~30 always-load rules TWICE. Now BANNED (rule v1.1.1 §3 sibling-mandate) + PreToolUse hook `check_worktree_in_repo` blocks it. Always use `../kite-wt-<slug>`.
- **always-load rule set was AT hard-ceiling (300k bytes).** Adding to an always-load rule tipped budget FAIL. Fix = retier worktree rule always-load→hook-covered+path-scoped (§3.3) → freed ~14k/session. Lesson: hook-enforced command-pattern rules should path-scope, not always-load.
- **rule→hook conversion audit:** only worktree was a clean win. admin-merge (CRITICAL + wait-CI judgment) + agent-model-opus (Explore-exception reasoning → FP over-block) NOT converted — net-negative rework. Criterion: convert only when (a) deterministic command/file trigger w/o reasoning-exception AND (b) currently always-load.
- **`--force-with-lease` stale-info recurrence:** tracking ref goes stale after amend; use explicit `--force-with-lease=<branch>:<actual-remote-sha>` (`git ls-remote` to get sha).

## 4. Stack state

- Local Docker: not started this session (CSP fix was config-only, verify-by-mirror; FE rebuild deferred — CSP report-only, no functional block).
- AWS: untouched (GAP-612 suspension context unchanged).
- Known: GAP-1198 final verify = local KC FE rebuild → console clean (deferred, report-only cosmetic).
- `pre-tool-guard.py`: 5→6 checks (added worktree-in-repo). Always-load rules: 16→15.

## 5. Pickup for next session

- No blocking carryover. Demo-seed-1 wave still has G1-full + G2-human-walk pending (GAP-1180/1190..1195 PARTIAL; demo tenant has no login account per GAP-1197 → dashboard browser walk blocked).
- Optional: GAP-1197 (seed demo login account) unblocks demo dashboard G2 walk.
- Branch: this session worked on sibling worktrees off `origin/main`; main tree on `main`, synced after #2325 merge.

## 6. Start next session

```bash
git -C /home/nguyenvankiet/projects/2026-Kite-Class-Platform fetch origin main
git -C /home/nguyenvankiet/projects/2026-Kite-Class-Platform pull --ff-only   # main tree on main, sync merges
bash .claude/skills/workflow/start-session/scripts/collect-state.sh
# New-branch work: git worktree add -b <branch> ../kite-wt-<slug> origin/main   (SIBLING, never in-repo — rule v1.1.1)
# Demo-seed-1 continue: bash scripts/query-gaps.sh GAP-1197  (login account → unblock G2 dashboard walk)
```
