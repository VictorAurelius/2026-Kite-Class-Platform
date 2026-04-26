---
name: continue
description: Xác định action ưu tiên nhất từ project plans và thực hiện theo Superpowers methodology
user-invocable: true
argument-hint: "[optional context]"
---

# /continue - Thực hiện Action Ưu Tiên Nhất

## Step 0: Wave-eligibility pre-flight (BẮT BUỘC, thêm 2026-04-26)

**Trước khi pick task đầu tiên**, hỏi 3 câu:

1. Action ưu tiên có break thành ≥3 sub-tasks không?
2. Mỗi sub-task touch disjoint files (không share `application.yml`, không share migration version, không share service class)?
3. Mỗi sub-task self-contained TDD/build cycle (không wait sub-task khác)?

**3/3 YES → wave-eligible → DỪNG `/continue`, chuyển sang wave plan + parallel agents.**

Sau đó:
- Tạo wave plan ngắn (5-10 dòng) HOẶC chạy `/quality-plan` skill nếu sau audit
- Pre-assign mỗi agent: file paths + GAP number range + migration version slot
- Single message dispatch ≤5 Agents song song với `isolation: worktree`

Reference:
- Memory `feedback_wave_plan_before_serial_prs.md` (case study GAP-229 2026-04-26: 90min serial vs ~30min parallel)
- Memory `feedback_parallel_agent_strategy.md` (9 hard rules validated 3x)
- Skill `.claude/skills/workflow/quality-plan/SKILL.md` (post-audit auto-plan)
- Skill `.claude/skills/workflow/priority-pr-planning.md` (manual priority plan)
- Doc `documents/03-planning/roadmap/parallel-execution-strategy.md`

**Nếu 1+ trả lời NO → tiếp tục serial qua Step 1 dưới.**

---

## Step 1: Xác định Priority Action

Đọc các plan documents theo thứ tự ưu tiên (🔴 P0 trước):

**Active Plans (check Completion Status section):**
1. `documents/03-planning/quality-plan-v4-final-push.md` — 8 PRs, KH 96→100 + KC 93→100
2. `documents/03-planning/roadmap/parallel-execution-strategy.md` — wave execution guide

**Completed Plans (reference only):**
- `kitehub-saas-implementation-plan.md` — 17/17 ✅
- `kiteclass-quality-improvement-plan.md` — 10/10 ✅
- `docs-and-skills-refactor-plan.md` — 3/3 ✅
- `kitehub-quality-improvement-plan-v3.md` — 6/6 ✅

**Parallel execution:** Check parallel-execution-strategy.md cho wave hiện tại.
Ưu tiên: 🔴 P0 > 🟠 P1 > 🟡 P2

**TRƯỚC KHI code:** Chạy `/pre-flight-check pr` (Layer 1).
**TRƯỚC KHI bắt đầu module mới:** Chạy `/pre-flight-check domain` (Layer 2).
**Business docs:** Check `documents/01-business/` cho rules + config keys.

Nếu tất cả plans hoàn thành:
- Check open PRs: `gh pr list --state open`
- Check CI: `gh run list --limit 5`
- Check stale branches: `git branch -r | grep -v main | wc -l`

Context bổ sung từ user: $ARGUMENTS

## Step 2: Báo cáo trước khi làm

```
## Tiếp tục: [Tên PR/Task]
**Priority**: P0/P1/P2
**Plan**: [tên document]
**Scope**: [mô tả 1-2 câu]
```

## Step 3: Thực hiện theo Superpowers

### 3.1 Quick Brainstorm
- Phân tích scope, risks, edge cases
- Xác định dependencies

### 3.2 Task Breakdown
- Chia tasks cụ thể với estimate

### 3.3 TDD (cho code changes)
- Viết tests TRƯỚC code
- Commit tests (RED phase)

### 3.4 Implementation
- Implement theo breakdown
- Commit thường xuyên

### 3.5 Verify (BẮT BUỘC trước khi push)
- Rebuild Docker nếu cần: `cd kitehub && ./scripts/rebuild.sh <service>`
- API E2E: `./scripts/test-api-e2e.sh`
- Unit tests: `JAVA_HOME=/home/vkiet/jdk/jdk-21 ./mvnw clean test -pl <module> -am`
- PHẢI tất cả pass

### 3.6 Push & PR
- `git push origin <branch>`
- `gh pr create`
- `gh pr checks <number> --watch`
- KHÔNG merge - chờ user approve

## Step 4: Update Plan
Sau khi PR tạo xong, update plan document: đánh dấu ✅, thêm PR number.

## Rules
- LUÔN giao tiếp tiếng Việt
- LUÔN tạo feature branch
- LUÔN chạy tests sau code changes
- KHÔNG merge không có approval
- KHÔNG bỏ qua brainstorm/TDD
