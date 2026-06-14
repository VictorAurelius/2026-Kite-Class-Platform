# GAP-1348: 15 stale remote feature branch — repo hygiene cleanup

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** DevOps
**Found:** 2026-06-14 (Quality full audit, AUDIT-2026-06-14-quality-full)
**Affects:** remote `origin` feature/wave branches

## Problem

`git branch -r | grep -v main` đếm **15 remote branch** còn tồn (không tính main/HEAD). Quality audit Cat 5 (CI/CD) sub-check "0 stale branches (2pt)" không đạt — branch của wave/PR đã merge không được prune → ls/branch listing nhiễu, nhầm lẫn nhánh active vs đã-merge, tăng rủi ro multi-session checkout sai nhánh (per `worktree-only-branch-work` + `multi-session-concurrency-coordination`).

## Root Cause

Squash-merge không tự xóa remote branch; `prune-merged-worktrees.sh` (GAP-690) còn thiếu detect `[gone]` + worktree-agent-* → branch merged tích lũy.

## Proposed Fix

(a) Liệt kê 15 branch, xác định branch đã merge vào main (`git branch -r --merged main`) vs branch active; (b) xóa branch merged qua `git push origin --delete <branch>`; (c) hoàn thiện GAP-690 tooling để auto-prune định kỳ. Hygiene-only — không ảnh hưởng code.

## Acceptance Criteria

- [ ] 15 branch phân loại merged vs active
- [ ] Branch merged-vào-main đã xóa khỏi remote
- [ ] Remote branch count (non-main) ≤5 hoặc tài liệu hóa branch active còn lại

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 5)
- Tooling: GAP-690 (prune-merged-worktrees.sh `[gone]` detection)
- Rules: `worktree-only-branch-work.md`, `multi-session-concurrency-coordination.md`
