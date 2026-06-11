---
paths:
  - ".claude/session-locks/**"
  - ".claude/worktrees/**"
---

# Worktree-Only Branch Work — không checkout nhánh trong main working tree

**Priority:** 🟠 MANDATORY — multi-session filesystem-safety governance
**Version:** 1.1.1
**Created:** 2026-06-09
**Last-Reviewed:** 2026-06-11
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.0 MINOR self-approve per `rule-change-process.md` §5; tightens §3 → worktree PHẢI sibling-outside-repo (`../kite-wt-<slug>`), bans in-repo `.claude/worktrees/` (nested `.claude/` → harness auto-load DUPLICATE rules). + PreToolUse detector §8.4 per §6.5 Enforcement Parity. No constraint loosening (narrows 2 path options → 1 safe). Details §11 Log + `_examples/worktree-only-branch-work-examples.md`. v1.0.0: new rule, built-in enforcement, 2026-06-09 main-tree checkout-swap incident; sister `multi-session-concurrency-coordination.md` (GAP-1114).)
**Applies to:** Mọi thao tác cần làm việc trên một nhánh ≠ nhánh hiện tại của main working tree (feature/wave/fix/PR branch). Out-of-scope: file-restore `git checkout -- <path>`, read-only inspection nhánh khác (`git show`/`git log origin/<branch>`/`git diff`), harness agent-worktree mechanism (đã isolated sẵn).

---

## 1. The Rule

> **KHÔNG `git checkout <branch>` / `git switch <branch>` để chuyển main working tree sang nhánh khác. Mọi công việc trên một nhánh PHẢI thực hiện trong git worktree riêng (`git worktree add`). Main working tree giữ nguyên nhánh hiện tại — 1 nhánh khác = 1 worktree khác.**

Main working tree (`/home/kitedev/projects/2026-Kite-Class-Platform`) là **shared filesystem state** giữa nhiều phiên Claude + agent chạy đồng thời (solo-dev multi-session pattern per `feedback_multi_session_concurrency.md`). `git checkout` đổi nhánh tại đó = yank HEAD + working files dưới chân mọi phiên/agent khác đang dùng cùng thư mục → đè dirty edits chưa commit, đổi context build/test giữa chừng. `git worktree add` cô lập mỗi nhánh vào 1 thư mục riêng → song song không xung đột.

Sister rule cùng họ concurrency, khác boundary:
- `multi-session-concurrency-coordination.md` (GAP-1114) — reserve gap-ID block + branch off main + additive-resolve CSV (boundary: định danh gap/nhánh)
- **This rule** — không switch main tree, worktree per nhánh (boundary: cô lập filesystem)

---

## 2. Trigger pattern — khi nào rule fire

| Tình huống | Fire? | Hành động đúng |
|---|---|---|
| Cần sửa/CI-fix một PR branch có sẵn | ✅ YES | `git worktree add <path> <branch>` |
| Bắt đầu việc mới trên nhánh mới | ✅ YES | `git worktree add -b <new-branch> <path> origin/main` |
| Cần build/test một nhánh khác nhánh hiện tại | ✅ YES | worktree riêng |
| Restore 1 file từ nhánh/commit khác (`git checkout -- f` / `git checkout <sha> -- f`) | ❌ NO | OK — không switch branch |
| Xem nội dung nhánh khác (`git show origin/x:f`, `git log origin/x`) | ❌ NO | read-only, không cần checkout |
| Harness tự tạo agent worktree (`isolation: "worktree"`) | ❌ NO | đã isolated, không phải checkout main tree |

Rule **KHÔNG** fire khi: thao tác read-only, file-restore, hoặc cơ chế agent-worktree của harness.

---

## 3. Required action — worktree workflow

Worktree PHẢI đặt **SIBLING ngoài repo** (`../kite-wt-<slug>`) — KHÔNG đặt in-repo `.claude/worktrees/` (xem §3.1 hazard).

```bash
# 1) Làm trên PR/nhánh có sẵn:
git worktree add ../kite-wt-<slug> <existing-branch>
# 2) Bắt đầu nhánh mới off main:
git worktree add -b <new-branch> ../kite-wt-<slug> origin/main
# 3) Làm việc bằng đường dẫn tuyệt đối tới worktree (per feedback_worktree_absolute_path_contamination.md):
#    edit/commit/push trong ../kite-wt-<slug>/...
# 4) Dọn sau khi merge:
git worktree remove ../kite-wt-<slug>
```

- Worktree path: **sibling `../kite-wt-<slug>` (OUTSIDE repo root) MANDATORY**. In-repo `.claude/worktrees/wt-<slug>` BANNED per §3.1.
- Edit/commit/push **trong** worktree path — luôn dùng đường dẫn tuyệt đối, KHÔNG `cd` lẫn lộn giữa worktree và main tree (per `feedback_worktree_absolute_path_contamination.md`).
- Sau merge: `git worktree remove ../kite-wt-<slug>` để tránh husk tích tụ (per `post-wave-cleanup.md`).

### 3.1 Vì sao sibling-outside-repo (KHÔNG in-repo `.claude/worktrees/`)

Worktree là bản copy đầy đủ repo (gồm `CLAUDE.md` + `.claude/rules/**`). Worktree IN-REPO → `.claude/` worktree thành **nested project-config** dưới main → harness auto-load THÊM CLAUDE.md + always-load rules của worktree CHỒNG bản main → **DUPLICATE** (context ~2×). Sibling ngoài repo root KHÔNG nested → load 1 bộ. `gitignore` KHÔNG cứu (chỉ git tracking, không ảnh hưởng harness path-based auto-load). Chi tiết self-test: `_examples/worktree-only-branch-work-examples.md`.

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| `git checkout feature/x` trong main tree để sửa PR | `git worktree add ../kite-wt-x feature/x` |
| `git checkout -b wave/new` trong main tree để bắt đầu việc | `git worktree add -b wave/new ../kite-wt-new origin/main` |
| `git switch <branch>` đổi main tree | worktree riêng |
| **Worktree đặt in-repo `.claude/worktrees/wt-<slug>/`** (dù gitignored) | **Sibling `../kite-wt-<slug>` ngoài repo** — in-repo khiến `.claude/` worktree nested → harness auto-load DUPLICATE rules + CLAUDE.md (§3.1) |
| "Chỉ checkout nhanh rồi checkout lại" (giữa phiên đa-luồng) | Phiên khác có thể đang dùng main tree giữa 2 lần checkout → vẫn worktree |
| Reuse main tree cho nhiều nhánh tuần tự | 1 nhánh = 1 worktree, song song |
| Commit lên nhánh phiên khác vừa checkout vào main tree | Worktree của riêng mình, off main |

---

## 5. Override mechanism

Genuine exception (chắc chắn solo, KHÔNG có phiên/agent song song nào — vd máy cá nhân 1 terminal duy nhất, hoặc thao tác recovery cần main tree ở nhánh cụ thể):

```
git commit -m "...
WORKTREE_ONLY_OVERRIDE: <reason — e.g. 'single-session confirmed, no parallel locks/agents; recovery checkout main'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review. Mặc định khi nghi ngờ có phiên song song → worktree (cost worktree ~200ms < cost đè công việc phiên khác).

---

## 6. Worked self-test

Moved to `_examples/worktree-only-branch-work-examples.md` (deferred-load per `context-budget-mandate.md` §3.2 — keeps always-load body under byte ceiling). Covers: v1.0.0 main-tree checkout-swap (2026-06-09), v1.1.0 duplicate-rule-load (2026-06-11), v1.1.1 detector fixture. Both fire correctly on originating incidents — PASS.

---

## 7. Tier: hook-covered + path-scoped (per `context-budget-mandate.md` §3.3)

v1.1.1 chuyển từ always-load → **hook-covered tier**: enforcement thật là PreToolUse detector `check_worktree_in_repo` (§8.4) chạy mọi `git worktree add` bất kể rule có trong context hay không — deterministic, real-time, không phụ thuộc always-load. Rule body giờ là documentation, path-scoped (`paths:` frontmatter loads khi thao tác `.claude/session-locks/**` / `.claude/worktrees/**` — multi-session/worktree context).

Lý do đổi: (a) always-load set chạm hard-ceiling 300k bytes (`check-context-budget.sh` FAIL) — v1.0.0 §7 always-load justification viết khi CHƯA có hook; (b) hook giờ cover worktree-add (case chính); (c) checkout-swap case (v1.0.0) còn self-detection §8.1 + sister `multi-session-concurrency-coordination.md` (path-scoped `.claude/session-locks/**`, cùng surface). Re-evaluate nếu checkout-swap recurrence ≥2 → thêm checkout-detector vào hook (§8.4).

---

## 8. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 8.1 Self-detection (in-turn, active now)
Trước khi chạy `git checkout <branch>` / `git switch <branch>` qua Bash:
- Đây có phải switch main tree sang nhánh khác để làm việc? Nếu YES → STOP, dùng `git worktree add`.
- File-restore (`git checkout -- f`) / read-only → OK.

### 8.2 Reviewer-checklist (active now)
Khi review PR:
- [ ] Công việc nhánh này phát sinh từ worktree (không phải checkout main tree)?
- [ ] Không có dấu hiệu commit nhầm lên nhánh phiên khác (author/branch drift)?

### 8.3 Memory auto-load (paired same-PR)
`feedback_worktree_only_no_checkout.md` reminds self-detection checklist tại session start.

### 8.4 Detector

- **Worktree-path detector (SHIPPED v1.1.1, `pre-tool-guard.py` `check_worktree_in_repo`):** PreToolUse Bash hook BLOCK khi `git worktree add` với path in-repo (`.claude/worktrees/` hoặc absolute path dưới repo root); ALLOW sibling `../kite-wt-<slug>`. Deterministic — chặn ngay tại moment tạo worktree (earliest point). Self-test fixture `_examples/worktree-only-branch-work-examples.md`.
- **Checkout-vs-file-restore detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1):** phân biệt `git checkout <branch>` (banned) vs `git checkout -- <file>` (OK) vs `-b` vs detached-sha cần arg-parse, FP risk trung bình; recurrence 1 (2026-06-10). Self-detection §8.1 + reviewer-checklist §8.2 + memory §8.3 đủ; revisit khi recurrence ≥2.

### 8.5 Override — per §5.

---

## 9. Atomic-unique-bar check (per `rule-change-process.md` §5.1)

✅ Atomic (1 concept: worktree per nhánh, không switch main tree) · ✅ Unique (`multi-session-concurrency-coordination.md` = gap-ID/branch reservation; `feedback_worktree_absolute_path_contamination.md` = in-worktree path hygiene; không rule nào cấm checkout main tree / mandate sibling) · ✅ Widely applicable · ✅ Body discipline ≤2 conjunction.

---

## 10. Relationship to other rules

- **`multi-session-concurrency-coordination.md`** (GAP-1114) — sister concurrency (gap-ID/branch reservation); rule này = filesystem-isolation layer.
- **`feedback_worktree_absolute_path_contamination.md`** + **`feedback_worktree_only_no_checkout.md`** (memory) — in-worktree path hygiene + self-detection reminder.
- **`context-budget-mandate.md`** §3.3 — hook-covered tier (PreToolUse detector §8.4 = real-time enforcement).
- **`post-wave-cleanup.md`** — `git worktree remove` sau merge. **`incident-to-rule-pipeline.md`** + **`rule-change-process.md`** §6.5 + **`output-review-mandate.md`** §3 — pipeline + enforcement-parity + matrix row.

---

## 11. Log

- **2026-06-11 (v1.1.0+v1.1.1):** MINOR — §3 mandate sibling-outside-repo (`../kite-wt-<slug>`); BAN in-repo `.claude/worktrees/` (§3.1 hazard + §4 banned row). v1.1.1 ships PreToolUse detector `check_worktree_in_repo` in `pre-tool-guard.py` (§8.4) — BLOCK `git worktree add` in-repo path at creation moment; worked self-tests moved to `_examples/worktree-only-branch-work-examples.md`. Triggered by user-flagged 2026-06-11 duplicate-rule-load: CSP fix phiên dùng in-repo `.claude/worktrees/wt-csp/` → harness auto-load DUPLICATE CLAUDE.md + ~30 rules (main + nested worktree) → context ~2×. Root cause: v1.0.0 §3 offered in-repo + sibling as equal options; in-repo `.claude/` becomes nested project-config (gitignore doesn't help — only git tracking, not harness path-based auto-load). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged) → Classify ✓ (`context-budget-mandate.md` covers always-load size, not worktree-nested-config dup) → Rule+Enforce ✓ (§3 + detector + this PR dogfoods sibling `../kite-wt-meta`) → Self-Test ✓ (_examples) → Retro Log ✓. META P1 force-multiplier. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — narrows 2 options → 1; no loosening; existing in-repo worktrees cleanup-on-remove; prospective).
- **2026-06-09 (v1.0.0):** Rule created at user direction (session local 2026-06-10 GMT+7 = 2026-06-09 UTC; frontmatter dates use UTC per CI `check-rule-frontmatter.sh` `Last-Reviewed ≤ today` gate; branch names embed `2026-06-10` as literal identifiers) "thêm rule là bây giờ sẽ không checkout mà chỉ làm việc trên worktrees thôi". Triggered by same-session incident: main working tree bị một phiên song song `git checkout` swap từ `wave/branding-fix-2026-06-10` (e7444b45) sang `feature/wizard-redesign-gaps-2026-06-10` (91352f74) giữa lúc phiên này đang điều tra 5 open PR — concrete clobber-risk evidence. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged + live incident observed) → Classify ✓ (no existing rule cấm checkout main tree; `multi-session-concurrency-coordination.md` covers gap-ID/branch reservation; `feedback_worktree_absolute_path_contamination.md` covers in-worktree path hygiene) → Rule+Enforce ✓ (this file + self-detection §8.1 + reviewer-checklist §8.2 + memory paired + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5) → Self-Test ✓ (§6 — rule fires đúng trên chính incident; counterfactual main tree ổn định) → Retro Log ✓. META P1 force-multiplier per `meta-gap-priority.md` §3 — mọi multi-branch work subsequent dùng worktree, eliminate main-tree-swap clobber class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint; no constraint loosening; existing checkouts grandfathered; applies prospectively từ this PR forward). Always-load count 15→16 (OK, <18 WARN). Detector (§8.4) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 1, arg-parse FP risk); self-detection + reviewer-checklist + memory + self-test sufficient cho v1.0.0.
