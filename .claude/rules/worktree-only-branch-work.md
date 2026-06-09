# Worktree-Only Branch Work — không checkout nhánh trong main working tree

**Priority:** 🟠 MANDATORY — multi-session filesystem-safety governance
**Version:** 1.0.0
**Created:** 2026-06-09
**Last-Reviewed:** 2026-06-09
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (self-detection + reviewer-checklist + memory auto-load + worked self-test on 2026-06-09 UTC main-tree-checkout-swap incident, session local 2026-06-10 GMT+7) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "branch switch trong shared main working tree giữa nhiều phiên đồng thời". Sister cho `multi-session-concurrency-coordination.md` (GAP-1114, gap-ID/branch reservation) tại boundary filesystem-isolation thay vì gap-ID-reservation)
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

```bash
# 1) Làm trên PR/nhánh có sẵn:
git worktree add .claude/worktrees/wt-<slug> <existing-branch>
# 2) Bắt đầu nhánh mới off main:
git worktree add -b <new-branch> .claude/worktrees/wt-<slug> origin/main
# 3) Làm việc bằng đường dẫn tuyệt đối tới worktree (per feedback_worktree_absolute_path_contamination.md):
#    edit/commit/push trong .claude/worktrees/wt-<slug>/...
# 4) Dọn sau khi merge:
git worktree remove .claude/worktrees/wt-<slug>
```

- Worktree path: `.claude/worktrees/wt-<slug>` (gitignored) HOẶC sibling `../kite-wt-<slug>`.
- Edit/commit/push **trong** worktree path — luôn dùng đường dẫn tuyệt đối, KHÔNG `cd` lẫn lộn giữa worktree và main tree (per `feedback_worktree_absolute_path_contamination.md`).
- Sau merge: `git worktree remove` để tránh husk tích tụ (per `post-wave-cleanup.md`).

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| `git checkout feature/x` trong main tree để sửa PR | `git worktree add .claude/worktrees/wt-x feature/x` |
| `git checkout -b wave/new` trong main tree để bắt đầu việc | `git worktree add -b wave/new .claude/worktrees/wt-new origin/main` |
| `git switch <branch>` đổi main tree | worktree riêng |
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

## 6. Worked self-test — main-tree checkout-swap incident (2026-06-09 UTC / 2026-06-10 GMT+7)

**Scenario:** Phiên này khởi động trên `wave/branding-fix-2026-06-10` (HEAD `e7444b45`). Giữa lúc đang điều tra 5 PR, một phiên song song chạy `git checkout feature/wizard-redesign-gaps-2026-06-10` trong **cùng main working tree** → main tree HEAD đột ngột nhảy sang `91352f74` dưới chân phiên này.

**Apply rule retroactively (counterfactual):** Phiên song song lẽ ra chạy
`git worktree add .claude/worktrees/wt-wizard feature/wizard-redesign-gaps-2026-06-10`
thay vì checkout → main tree **giữ nguyên** `wave/branding-fix` cho phiên này; cả hai phiên làm song song không đè nhau.

| Metric | Without rule | With rule |
|---|---|---|
| Main tree branch ổn định cho phiên đang chạy | ❌ bị swap giữa chừng | ✅ giữ nguyên |
| Rủi ro đè dirty edits chưa commit | CAO | ~0 (cô lập) |
| Build/test context nhất quán | ❌ đổi giữa chừng | ✅ |
| Cost | confusion + re-orient | ~200ms worktree add |

→ Rule fires đúng trên chính incident sinh ra nó. Self-test PASS ✅

---

## 7. Auto-load justification (per `context-budget-mandate.md` §3.2)

KHÔNG dùng `paths:` frontmatter — always-load. Lý do:
- **Fire tại git-decision-time, không file-read-time** — quyết định "làm trên nhánh khác" xảy ra trước khi đọc file nào; không có natural file-scope glob.
- **Path-scope sẽ miss case quan trọng** — checkout có thể xảy ra trong bất kỳ task nào (fix PR, bắt đầu wave, build nhánh khác). Scope `.claude/**` sẽ vắng mặt đúng lúc cần nhất.
- **Token cost chấp nhận được** — rule tight (~5k chars / ~1.3k tokens) × session; force-multiplier (mỗi lần tránh 1 checkout-swap = tránh đè công việc phiên khác).
- **Priority MANDATORY giữ nguyên** — §5 override cho phép solo case; always-load per §3.2 row 2.

Re-evaluate nếu: (a) PreToolUse hook detect `git checkout <branch>` khả dụng, (b) >5 false-positive/quarter, (c) chuyển sang single-session-only workflow vĩnh viễn.

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

### 8.4 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1)
- **Complexity:** PreToolUse hook phải phân biệt `git checkout <branch>` (banned) vs `git checkout -- <file>` (OK) vs `git checkout -b` vs detached-sha — cần arg-parse, FP risk trung bình.
- **Recurrence:** 1 (2026-06-10).
- **Decision:** self-detection §8.1 + reviewer-checklist §8.2 + memory §8.3 + worked self-test §6 đủ cho v1.0.0; revisit detector (extend `pre-tool-guard.py`) khi recurrence ≥2 OR sau khi multi-session campaign hiện tại đóng.

### 8.5 Override — per §5.

---

## 9. Atomic-unique-bar check (per `rule-change-process.md` §5.1)

- ✅ **Atomic:** single concept = không switch main tree, worktree per nhánh.
- ✅ **Unique:** `multi-session-concurrency-coordination.md` covers gap-ID/branch reservation; `feedback_worktree_absolute_path_contamination.md` covers path hygiene TRONG worktree; KHÔNG rule nào cấm checkout main tree.
- ✅ **Widely applicable:** mọi multi-branch work khi có phiên song song.
- ✅ **Body discipline:** §1 The Rule ≤2 conjunction.

---

## 10. Relationship to other rules

- **`multi-session-concurrency-coordination.md`** (GAP-1114) — sister concurrency rule; reserve gap-ID/branch. Rule này thêm filesystem-isolation layer.
- **`feedback_worktree_absolute_path_contamination.md`** (memory) — path hygiene khi đã ở trong worktree; rule này quyết định DÙNG worktree.
- **`post-wave-cleanup.md`** — `git worktree remove` sau merge để tránh husk.
- **`agent-action-bias.md`** — do-it-yourself; rule này: do-it-in-worktree.
- **`incident-to-rule-pipeline.md`** — rule này = direct output 2026-06-10 main-tree-swap incident qua 5-stage.
- **`rule-change-process.md`** §6.5 — rule + self-detection + reviewer-checklist + memory + worked self-test + rules-index.csv row + output-review-mandate §3 row all same PR.
- **`feedback_worktree_only_no_checkout.md`** (memory, paired same-PR).

---

## 11. Log

- **2026-06-09 (v1.0.0):** Rule created at user direction (session local 2026-06-10 GMT+7 = 2026-06-09 UTC; frontmatter dates use UTC per CI `check-rule-frontmatter.sh` `Last-Reviewed ≤ today` gate; branch names embed `2026-06-10` as literal identifiers) "thêm rule là bây giờ sẽ không checkout mà chỉ làm việc trên worktrees thôi". Triggered by same-session incident: main working tree bị một phiên song song `git checkout` swap từ `wave/branding-fix-2026-06-10` (e7444b45) sang `feature/wizard-redesign-gaps-2026-06-10` (91352f74) giữa lúc phiên này đang điều tra 5 open PR — concrete clobber-risk evidence. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged + live incident observed) → Classify ✓ (no existing rule cấm checkout main tree; `multi-session-concurrency-coordination.md` covers gap-ID/branch reservation; `feedback_worktree_absolute_path_contamination.md` covers in-worktree path hygiene) → Rule+Enforce ✓ (this file + self-detection §8.1 + reviewer-checklist §8.2 + memory paired + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5) → Self-Test ✓ (§6 — rule fires đúng trên chính incident; counterfactual main tree ổn định) → Retro Log ✓. META P1 force-multiplier per `meta-gap-priority.md` §3 — mọi multi-branch work subsequent dùng worktree, eliminate main-tree-swap clobber class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint; no constraint loosening; existing checkouts grandfathered; applies prospectively từ this PR forward). Always-load count 15→16 (OK, <18 WARN). Detector (§8.4) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 1, arg-parse FP risk); self-detection + reviewer-checklist + memory + self-test sufficient cho v1.0.0.
