---
title: Session Prompt — Fix Meta Gaps First (skills/rules/workflow)
status: ready-to-use
created: 2026-04-20
updated: 2026-04-20
purpose: Self-contained prompt for new Claude Code session to execute meta-gap fixes before feature gaps
---

# Session Prompt — Fix Meta Gaps First

Áp dụng `.claude/rules/meta-gap-priority.md` — meta gaps (skills/rules/workflow) ưu tiên cao nhất vì force multiplier cho mọi output downstream. Fix meta trước feature.

---

## 1. Ready-to-use Prompt (copy-paste vào session mới)

```
Đọc documents/03-planning/plans/prompt-meta-gaps-first-2026-04-20.md §3 Meta-Fix
Execution Order và thực hiện tuần tự. Mỗi wave dùng parallel-agent pattern với
isolation:worktree theo feedback_parallel_agent_strategy.md hard rules. Dừng sau
khi wave đang làm merged thành công. Tuân thủ toàn bộ constraints trong §4.

Bắt đầu từ: {WAVE_ID}

Ở đâu {WAVE_ID} là một trong:
- wave-9-ga-meta    → GAP-011, 014, 016, 005 (4 gaps P0)
- wave-8b-output    → GAP-170, 171, 172, 173, 174, 175 (6 gaps)
- partc-sprint-0    → GAP-149 audit skill grep scope (1 gap)
- wave-6-6c         → GAP-176 ui-ux-pro-max integration (1 gap)
- wave-8-meta       → GAP-046, 049, 050 (3 meta-portion of Wave 8)
- wave-5-exec       → GAP-047 document generation execution (plan PR #361 merged)
- all-sequential    → chạy tất cả theo thứ tự §3 dưới

Nếu prompt này không kèm {WAVE_ID}, ask user chọn wave nào bắt đầu.
```

---

## 2. Context tóm tắt (new session đọc trước)

**Dự án:** KiteClass + KiteHub SaaS giáo dục

**Trạng thái 2026-04-20:**
- Backlog: 155 gaps (59 closed, ~93 open)
- Calibrated quality: 77/100 C+ (honest baseline, không phải 95/100 self-estimate cũ)
- Specialist audit scores: business 72 C, perf 64 D, ops 49 F, UI KC 81/KH 59 /128
- Master plan 12 waves merged PR #382 (close all 93 open gaps, ~2-3 months)
- Part C score recovery plan merged PR #381 (14 gaps, 4 sprints)
- Wave 5 document generation plan merged PR #361 (GAP-047)
- 26 PRs merged hôm nay 2026-04-20

**Rules ưu tiên đọc:**
- `.claude/rules/meta-gap-priority.md` — WHY meta first
- `.claude/rules/audit-to-gap-pipeline.md` — gap closure process
- `.claude/rules/post-wave-audit-mandate.md` — audit cadence
- `.claude/rules/mcp-first-with-fallback.md` — MCP preferred
- `.claude/rules/output-review-mandate.md` — review standards
- `CLAUDE.md` — Vietnamese, no Co-Authored-By, Superpowers methodology

**Memory bắt buộc đọc:**
- `feedback_parallel_agent_strategy.md` — 9 hard rules (pre-assigned GAP ranges, parent owns shared files, worktree paths explicit, silent crash respawn, cwd drift fix, 5-agent cap)
- `feedback_audit_calibration.md` — self-audit overstates 15-20 pts
- `feedback_audit_grep_scope.md` — audit skills include `-core` submodules

---

## 3. Meta-Fix Execution Order

Sequencing theo force-multiplier impact + dependency chain. Làm tuần tự HOẶC parallel khi không xung đột.

### 3.1 `partc-sprint-0` (quick win, 2-3h)

**Goal:** Fix audit skill grep scope — prevent false positives như GAP-107.

**Gap:** GAP-149 (new — chưa có file) — business-logic-audit skill grep scope too narrow.

**Scope:**
- Update `.claude/skills/quality/business-logic-audit/SKILL.md` grep scope section:
  - OLD: `grep -r "pattern" kitehub/ kiteclass/`
  - NEW: `grep -rE "pattern" --include="*.java"` (project root)
- Verify other audit skills (performance, api-contract, ops-readiness) with similar pattern
- Create GAP-149 file (reserved number) với Problem/Fix/AC

**PR:** `fix(gap): GAP-149 — audit skill grep scope fix`

**1 agent, no parallel needed. 1 PR.**

---

### 3.2 `wave-9-ga-meta` (4 P0 gaps, ~1-2 week)

**Goal:** Close 4 remaining Part A GA blockers — highest score impact per master plan.

**Gaps (4):**
- GAP-011 (P0) Template Library Curation Plan + Review Standards
- GAP-014 (P0) Wave Mock Plan Missing AI Branding Workflow
- GAP-016 (P0) Living Documents Impact Scope (3-layer sweep)
- GAP-005 (P0, IN_PROGRESS) AI queue fair scheduling Phase 2

**Parallel strategy:** 4 agents, disjoint scope:
- Agent A: GAP-011 template library
- Agent B: GAP-014 wave mock AI branding
- Agent C: GAP-016 living docs 3-layer sweep
- Agent D: GAP-005 AI queue Phase 2 continuation

**Pre-assigned GAP range for follow-ups:** GAP-177 → GAP-181 (5 slots)

**PRs:** 4 sub-PRs + 1 consolidation = 5 PRs.

---

### 3.3 `wave-8b-output` (6 meta governance gaps, ~1 week)

**Goal:** Close output-review-mandate §4 VIOLATIONS — force-multiplier cho mọi output future.

**Gaps (6, all tracked):**
- GAP-170 (P0) Gap reports review template
- GAP-171 (P0) Rules docs ADR-like process
- GAP-172 (P0) Architecture docs ADR process
- GAP-173 (P1) Email template review checklist
- GAP-174 (P1) Marketing/legal review
- GAP-175 (P2) Logs format standard

**Parallel strategy:** 3-4 agents:
- Agent A: GAP-170 + GAP-171 (meta-governance skills combined)
- Agent B: GAP-172 (architecture ADR folder + 5 retrospective ADRs)
- Agent C: GAP-173 + GAP-174 (email + marketing/legal review combined)
- Agent D: GAP-175 (logging standard rule)

**Pre-assigned follow-up range:** GAP-182 → GAP-186 (5 slots)

**PRs:** 4 sub-PRs + 1 consolidation = 5 PRs.

---

### 3.4 `wave-6-6c` (1 P1 meta skill, 2-3 days)

**Goal:** Adopt ui-ux-pro-max-skill — upgrade UI audit from validator to advisor.

**Gap:** GAP-176 (P1) ui-ux-pro-max skill integration.

**Scope:** 3-PR execution per `plan-ui-ux-design-system-integration.md`:
1. PR 1: uipro-cli install + vendor skill + playbook (1-2h)
2. PR 2: ui-review skill upgrade with external references + advice section (2-3h)
3. PR 3: Vietnamese context layer (MOET palette, VN fonts, K-12 patterns) (1-2h)

**Sequencing:** Blocked on Part C Sprint 2 (GAP-127 FE code-splitting). If Part C Sprint 2 chưa merge → defer this wave. Otherwise parallel với other meta waves OK (disjoint files).

**1-2 agents (sub-PR parallel). 3 PRs.**

---

### 3.5 `wave-8-meta` (3 meta portion of Wave 8, ~1 week)

**Goal:** Close meta portion của Wave 8 Business Governance (feature portion can wait).

**Meta gaps (3):**
- GAP-046 (P1) Apply Design Patterns Systematically — rules + ArchUnit enforcement
- GAP-049 (P0) Business Logic Correctness Review — review process
- GAP-050 (P0) Persona-Based Business Review Process — review skill

**Parallel strategy:** 3 agents:
- Agent A: GAP-046 design patterns rules + ArchUnit + checklist
- Agent B: GAP-049 + GAP-050 combined (business correctness + persona — may unify into 1 skill `persona-business-review.md`)

**Pre-assigned follow-up range:** GAP-187 → GAP-191 (5 slots)

**Feature portion of Wave 8** (GAP-001, 102, 110 non-meta) defer sang Wave 8 execution proper sau khi meta portion xong.

**PRs:** 2-3 sub-PRs + 1 consolidation = 3-4 PRs.

---

### 3.6 `wave-5-exec` (GAP-047 document generation, ~1-2 weeks)

**Goal:** Execute Wave 5 plan (PR #361 merged) — add document generation skills (PDF/Excel/Word/PPT).

**Gap:** GAP-047 (P0 meta).

**Reference:** `documents/03-planning/waves/wave-05-document-generation.md` — đã có detailed plan.

**Note:** Wave 5 có decision guide với 6 open questions — user sign-off required trước Sub-PR 5.0 start. Read `documents/03-planning/waves/wave-05-decision-guide.md` first.

**Sub-PRs:** 5.0 foundation, 5.1 PDF+Excel (P0), 5.2 Word (P1), 5.3 PPT (P2). 2-4 agents tùy phase.

---

### 3.7 `all-sequential` (grand sequence)

Chạy tuần tự:
```
partc-sprint-0 → wave-9-ga-meta → wave-8b-output → wave-8-meta
→ wave-5-exec → wave-6-6c
```

**Tổng effort:** 3-5 weeks wall-clock với parallel agents per wave.

**Score projection cuối sequence:**
- Quality 77 → ~85 B+ (meta improvements don't directly lift quality score, but unblock all feature work)
- GA blockers closed: 6/6 Part A (after Wave 9) + output-review violations 6/6 (after Wave 8b)
- Docs coverage, skill maturity, review process all governance-grade

---

## 4. Global constraints (every wave)

### 4.1 Workflow bắt buộc
1. Đọc gap file + relevant skill/rule files trước implementation
2. Superpowers methodology: Brainstorm (5-10m) → Task breakdown (5-10m) → TDD (if code) → Implementation → Self-review
3. Commit via feature branch, NEVER directly main
4. PR branch naming: `fix(gap): GAP-XXX — short desc` (for single-gap) hoặc `fix(wave-N): topic` (multi-gap)
5. Parent sequences merges; agents do NOT merge

### 4.2 Hard constraints chung
- **Vietnamese** communication (CLAUDE.md §Communication Language)
- **KHÔNG Co-Authored-By** trailer
- **KHÔNG commit trực tiếp main**
- **KHÔNG modify shared files from agent** (ROADMAP.md, output-review-mandate.md, MEMORY.md) — parent owns
- **MCP-first** per `mcp-first-with-fallback.md` (GitHub MCP connected)
- **AUDIT_OVERRIDE** PR body nếu hook block (rare — docs-only should pass)
- **Worktree cleanup** manual: `git worktree remove` + `git branch -D` + `git push origin --delete` sau mỗi wave

### 4.3 Out-of-scope chung
- Fix FEATURE gaps (waves 10/11/12/13/14) — defer
- Refactor code không thuộc gap scope — defer
- Update master plan — chỉ consolidation PR cuối wave được touch (ROADMAP/master plan)

### 4.4 Parallel-agent hard rules (from feedback_parallel_agent_strategy.md)
1. Pre-assigned GAP number ranges (no collisions)
2. Parent owns shared files
3. Brief agents with worktree paths explicitly
4. Fail-loud guards need test-profile escape
5. Sequence merges, parallelize development
6. Worktree cleanup manual
7. Silent crash + MCP disconnect → respawn (don't debug)
8. Parent cwd drift fix: `cd /home/nguyenvankiet/projects/2026-Kite-Class-Platform && git checkout main`
9. Agent cap 5 concurrent max

---

## 5. Success criteria per meta wave

**Wave 9 (GA Meta):** 4 GA blockers closed, master plan snapshot updated, quality-audit refresh after merge.

**Wave 8b (Output Review):** 6 VIOLATIONS → 0 in output-review-mandate.md §4.

**Part C Sprint 0:** `business-logic-audit` skill has broader scope; retroactive check 3 old gaps (GAP-106, 108, 110) for similar false-positives.

**Wave 6 sub-sprint 6c:** ui-ux-pro-max vendored; ui-review skill references external rules.

**Wave 8 meta:** Design pattern rules enforced via ArchUnit; persona-based review skill shipped; 10 tenant types covered.

**Wave 5 exec:** PDF + Excel + Word + PPT skills shippable + 1 concrete use case per format.

---

## 6. Recovery / rollback

Mỗi wave độc lập. Rollback wave N không ảnh hưởng wave N-1. Wave N+1 có thể pause nếu wave N fails.

Hotfix pattern: nếu meta wave introduce regression, revert consolidation PR first, fix underlying PR, re-apply consolidation.

---

## 7. Log

- **2026-04-20:** Prompt created after user request "prompt cho session mới để thực hiện fix gaps về skills, rules, workflow trước". Meta-first ordering per meta-gap-priority.md. Covers all meta gaps identified in reference-repos report + master plan waves 8, 8b, 9, Part C Sprint 0, Wave 5, Wave 6 sub-sprint 6c.
