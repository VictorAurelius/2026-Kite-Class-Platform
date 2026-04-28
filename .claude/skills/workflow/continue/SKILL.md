---
name: continue
description: "Dùng khi user runs `/continue` hoặc nói 'tiếp tục', 'pick next task', 'làm tiếp', 'next priority'. Reads plan documents in priority order (P0 first), applies wave-eligibility pre-flight (Step 0 — ≥3 disjoint sub-tasks → wave + parallel agents), then executes via Superpowers methodology."
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
- **Living docs gate**: `./.claude/skills/workflow/session-docs-check/scripts/check-docs.sh` — bắt status flip thiếu, ROADMAP entry thiếu, skill index chưa update, rule edit thiếu Version+Log
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

---

## Gotchas

- **Step 0 wave-eligibility is BLOCKING, not advisory** — if 3/3 questions YES, you MUST stop `/continue` and switch to wave plan + parallel agents; ignoring this cost 90min serial vs ~30min parallel in GAP-229 incident (memory `feedback_wave_plan_before_serial_prs.md`)
- **Wave plan PHẢI qua PR, không push trực tiếp main** — even when "agents need plan ngay", route the wave plan through a normal PR — pushing directly to main violates CLAUDE.md (incident 2026-04-26 wave-7-perf, memory `feedback_wave_plan_through_pr.md`)
- **Step 3.5 living-docs gate is the latest enforcement** — `scripts/check-docs.sh` Rule 13 catches premature `🟢 DONE` flips with deferred work; failure here is a HARD block per `gap-done-discipline.md`, not a warn
- **`@Scheduled profiles=...` requires `-Dspring-boot.run.profiles=dev`** — env vars alone don't activate Spring profiles for the dev runner; `dev-start.sh` must pass the JVM property (memory `feedback_dev_profile_schema_workaround.md`, GAP-244)
- **MCP check at session start is mandatory** — read `claude mcp list` early; `start-session` collector emits "MCP servers: N/M" — skipping this defaults you to CLI for the whole session and burns ~5min on polling loops (memory `feedback_mcp_check_at_session_start.md`)
- **Don't pick GAP-006 / AI Branding inference work without verifying Ollama + Docker stack up first** — WSL2 CPU-only is too slow for 9B A/B test; pre-flight gate is non-negotiable (memory `feedback_gap006_infra_blocker.md`)
