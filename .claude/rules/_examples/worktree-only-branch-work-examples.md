# worktree-only-branch-work — Worked self-tests

Companion to `.claude/rules/worktree-only-branch-work.md` §6. Body moved here (deferred-load) per `context-budget-mandate.md` §3.2 — keeps the always-load rule body under the always-load byte ceiling.

---

## Self-test v1.0.0 — main-tree checkout-swap incident (2026-06-09 UTC / 2026-06-10 GMT+7)

**Scenario:** Phiên này khởi động trên `wave/branding-fix-2026-06-10` (HEAD `e7444b45`). Giữa lúc đang điều tra 5 PR, một phiên song song chạy `git checkout feature/wizard-redesign-gaps-2026-06-10` trong **cùng main working tree** → main tree HEAD đột ngột nhảy sang `91352f74` dưới chân phiên này.

**Apply rule retroactively (counterfactual):** Phiên song song lẽ ra chạy
`git worktree add ../kite-wt-wizard feature/wizard-redesign-gaps-2026-06-10`
thay vì checkout → main tree **giữ nguyên** `wave/branding-fix` cho phiên này; cả hai phiên làm song song không đè nhau.

| Metric | Without rule | With rule |
|---|---|---|
| Main tree branch ổn định cho phiên đang chạy | ❌ bị swap giữa chừng | ✅ giữ nguyên |
| Rủi ro đè dirty edits chưa commit | CAO | ~0 (cô lập) |
| Build/test context nhất quán | ❌ đổi giữa chừng | ✅ |
| Cost | confusion + re-orient | ~200ms worktree add |

→ Rule fires đúng trên chính incident sinh ra nó. Self-test PASS ✅

---

## Self-test v1.1.0 — duplicate-rule-load incident (2026-06-11)

**Scenario:** Phiên CSP fix tạo worktree IN-REPO `.claude/worktrees/wt-csp/`. Khi đọc/sửa `wt-csp/kiteclass/kiteclass-frontend/next.config.js`, harness auto-load THÊM `wt-csp/.claude/CLAUDE.md` + ~30 `wt-csp/.claude/rules/*.md` — chồng lên bản main tree đã load. User flag: "claude sẽ load duplicate rule" + screenshot 30 file `Loaded .claude/worktrees/wt-csp/.claude/rules/...`.

**Apply rule v1.1.0 retroactively (counterfactual):** Tạo worktree SIBLING `../kite-wt-csp` (ngoài repo root) thay vì in-repo → `.claude/` worktree KHÔNG nested dưới main → harness load 1 bộ rules theo cây file đang thao tác, KHÔNG chồng main → 0 duplicate.

| Metric | In-repo `.claude/worktrees/` | Sibling `../kite-wt-` |
|---|---|---|
| Bộ rules auto-load | 2× (main + worktree) | 1× |
| CLAUDE.md auto-load | 2× | 1× |
| Context phình | ~2× rule footprint | baseline |
| Cost | context budget regression | ~0 |

→ Rule v1.1.0 fires đúng trên chính incident sinh ra nó (PR v1.1.0 dùng sibling `../kite-wt-meta` → 0 duplicate). Self-test PASS ✅

### v1.1.1 detector self-test — `pre-tool-guard.py` worktree-path check

Synthetic fixture — feed các lệnh vào detector, expect verdict:

| Command | Expect |
|---|---|
| `git worktree add .claude/worktrees/wt-x feat/x` | 🛑 BLOCK (in-repo) |
| `git worktree add -b new .claude/worktrees/wt-new origin/main` | 🛑 BLOCK (in-repo `-b` form) |
| `git worktree add /home/nguyenvankiet/projects/2026-Kite-Class-Platform/tmp-wt feat/x` | 🛑 BLOCK (absolute path inside repo root) |
| `git worktree add ../kite-wt-x feat/x` | ✅ ALLOW (sibling outside repo) |
| `git worktree add -b new ../kite-wt-new origin/main` | ✅ ALLOW (sibling `-b` form) |
| `git worktree remove ../kite-wt-x` | ✅ ALLOW (not `add`) |
| `git worktree list` | ✅ ALLOW (not `add`) |

Detector fires correctly on the 2026-06-11 originating in-repo pattern (block) + allows the corrected sibling pattern. Self-test PASS ✅
