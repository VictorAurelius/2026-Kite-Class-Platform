# GAP-1348: 15 stale remote feature branch — repo hygiene cleanup

**Status:** 🟡 PARTIAL
**Priority:** 🟢 P3
**Domain:** DevOps
**Found:** 2026-06-14 (Quality full audit, AUDIT-2026-06-14-quality-full)
**Updated:** 2026-06-15 — count already down 15→7 (prior sessions); remaining 7 classified, 0 safely deletable
**Affects:** remote `origin` feature/wave branches

## Problem

`git branch -r | grep -v main` đếm **15 remote branch** còn tồn (không tính main/HEAD). Quality audit Cat 5 (CI/CD) sub-check "0 stale branches (2pt)" không đạt — branch của wave/PR đã merge không được prune → ls/branch listing nhiễu, nhầm lẫn nhánh active vs đã-merge, tăng rủi ro multi-session checkout sai nhánh (per `worktree-only-branch-work` + `multi-session-concurrency-coordination`).

## Root Cause

Squash-merge không tự xóa remote branch; `prune-merged-worktrees.sh` (GAP-690) còn thiếu detect `[gone]` + worktree-agent-* → branch merged tích lũy.

## Proposed Fix

(a) Liệt kê 15 branch, xác định branch đã merge vào main (`git branch -r --merged main`) vs branch active; (b) xóa branch merged qua `git push origin --delete <branch>`; (c) hoàn thiện GAP-690 tooling để auto-prune định kỳ. Hygiene-only — không ảnh hưởng code.

## Acceptance Criteria

- [x] Branches phân loại merged vs active — done (table below). Count already dropped 15→7 (prior audit-fix sessions pruned their merged branches).
- [~] Branch merged-vào-main đã xóa khỏi remote — **0 deleted**: the only `--merged origin/main` branch is `archive/kiteclass-gateway-pre-removal-2026-05-18`, an intentional archive snapshot (kept, not stale). All other 6 are active or unmerged — none meet the safe-delete bar (merged + no open PR + not archive + not in-flight).
- [~] Remote branch count (non-main) ≤5 hoặc document active — count = 7: 5 are active (open PRs), 1 intentional archive, 1 unmerged feature. All documented below; full FIFO auto-prune remains GAP-690.

## Resolution (2026-06-15) — PARTIAL

`git fetch --prune` then classified all 7 remaining non-main remote branches. **0 deleted** — none safely deletable per the conservative criteria (merged into main + no open PR + not an `archive/*` keep + not `fix/audit-*` / in-flight):

| Branch | Merged into main? | Open PR | Verdict |
|---|---|---|---|
| `archive/kiteclass-gateway-pre-removal-2026-05-18` | ✅ yes | — | **KEEP** — intentional archive snapshot (ADR kiteclass-gateway removal); not stale |
| `dependabot/maven/kiteclass/kiteclass-core/all-deps-3212e766b4` | no | #2418 | KEEP — open PR |
| `dependabot/maven/kitehub/all-deps-3212e766b4` | no | #2417 | KEEP — open PR |
| `dependabot/npm_and_yarn/kiteclass/kiteclass-frontend/all-deps-71db8f0fb8` | no | #2419 | KEEP — open PR |
| `dependabot/npm_and_yarn/kitehub/kitehub-frontend/all-deps-71db8f0fb8` | no | #2420 | KEEP — open PR |
| `feature/branding-100-prewalk-fix-csp` | no | — | LEAVE — not in `--merged` set (possibly squash-merged leftover OR in-flight); not safe to delete blindly |
| `fix/audit-fixB-storage-2026-06-14` | no | #2416 | KEEP — sibling audit-fix in-flight THIS session (explicitly excluded) |

The audit's "15 stale" figure was satisfied by prior sessions (15→7). The residual 7 are all legitimately retained today. The real durable fix (auto-prune squash-merged + `[gone]` branches on a cadence) is GAP-690 (`prune-merged-worktrees.sh`) — kept PARTIAL pending that tooling + an owner decision on `feature/branding-100-prewalk-fix-csp`.

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 5)
- Tooling: GAP-690 (prune-merged-worktrees.sh `[gone]` detection)
- Rules: `worktree-only-branch-work.md`, `multi-session-concurrency-coordination.md`
