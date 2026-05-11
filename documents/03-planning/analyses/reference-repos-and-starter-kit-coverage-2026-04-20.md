---
title: Reference Repos Coverage + Starter-Kit Update Plan (2026-04-20)
status: analysis
created: 2026-04-20
updated: 2026-04-20
---

# Báo Cáo: Reference Repos Coverage + Starter-Kit Update Plan

## 1. Tóm tắt Executive

| Ref repo | Coverage hiện tại | Status | Follow-up cần |
|----------|-------------------|:------:|---------------|
| [nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) | Plan draft tồn tại (`plan-ui-ux-design-system-integration.md`), KHÔNG có gap file, KHÔNG nằm trong master plan Wave nào | ⚠️ **UNDERTRACKED** | Tạo gap + thêm vào Wave 6 UI hoặc wave mới |
| [awslabs/agent-plugins](https://github.com/awslabs/agent-plugins) | GAP-103 DONE (PR #351) + ADR-015 ACCEPTED (DEFER adoption đến Q3 2026) | ✅ **FULLY COVERED** | Không cần action — revisit Q3 2026 |
| [VictorAurelius/claude-starter-kit](https://github.com/VictorAurelius/claude-starter-kit) | KHÔNG có local copy `.claude/starter-kit/`; skill-conventions.md documentt sync workflow | ⚠️ **SOURCE, NOT CONSUMER** — project is contributor | Cần plan upstream sync sau 40+ PRs (16 ngày drift) |

---

## 2. Repo #1: ui-ux-pro-max-skill

### Coverage hiện tại

**Tồn tại:**
- 📄 Plan doc: `documents/03-planning/plans/plan-ui-ux-design-system-integration.md` (2026-04-18)
- 3-PR execution plan outlined (install uipro-cli → upgrade ui-review → integrate Vietnamese context)

**Không có:**
- ❌ Gap file (GAP-XXX) tracking
- ❌ Mention trong master plan 12 waves (PR #382)
- ❌ Mention trong Wave 6 UI plan (placeholder chỉ nói scope UI polish)
- ❌ Reference trong ROADMAP Progress Log

### Gap phân tích

| Aspect | Status |
|--------|:------:|
| Plan tồn tại | ✅ |
| Plan có frontmatter + wave assignment | ❌ |
| Gap file theo audit-to-gap-pipeline | ❌ |
| Nằm trong master plan | ❌ |
| Có PR execution | ❌ |
| Timeline rõ ràng | ❌ (chỉ nói "3 PRs, 1-2 giờ mỗi PR") |

### Khuyến nghị action

**Option A (recommended): Merge vào Wave 6 UI Polish**
- Tạo gap file GAP-176 "UI/UX Pro Max Skill Integration"
- Thêm vào Wave 6 UI scope như Sprint 6a (pre-audit of UI), Sprint 6b execution, Sprint 6c Vietnamese context
- Timeline: 3-4 ngày trong Wave 6

**Option B: Wave dedicated mới (Wave 6c)**
- Nếu Wave 6 UI scope đã đủ (8 existing gaps)
- Tách ra wave riêng sau Wave 6 khi UI baseline đã fix
- Timeline: 1 week dedicated

**Option C: Skip trong master plan 3-month — defer sang Phase 2**
- Nếu thời gian eo hẹp và priority chỉ là quality score
- Plan doc stay open, execute khi convenient

### Dependencies

- Phụ thuộc Part C Sprint 2 (GAP-127 FE code-splitting) → cần clean FE baseline trước khi refactor theo UI reasoning rules
- Có thể parallel với Wave 6 UI Polish nếu scope tách rõ

---

## 3. Repo #2: awslabs/agent-plugins

### Coverage hiện tại

✅ **Fully covered:**

| Artifact | Location | Status |
|----------|----------|:------:|
| Gap file | `documents/04-quality/gaps/closed/GAP-103-deploy-philosophy-aws-plugins-adr.md` | 🟢 DONE (PR #351) |
| ADR | `documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md` | ACCEPTED |
| Deploy doc | `documents/02-architecture/deployment-strategy.md` | Shipped |

### Quyết định đã ghi nhận

**ADR-015 ACCEPTED (2026-04-18): DEFER adoption đến Q3 2026.**

Conditions để revisit:
1. Pilot tenant #1 đã ký hợp đồng → commit production cloud (likely AWS)
2. Wave 6 (AI Billing + Observability) shipped — có Prometheus/Grafana baseline để đánh giá replacement value
3. AWS Agent Plugins pricing model công khai và predictable

**Stance hiện tại:** KHÔNG dùng plugins trong any workflow (CI/CD, agents, dev scripts).

### Khuyến nghị action

**KHÔNG cần action mới.** Revisit tự động khi Q3 2026 hoặc 3 conditions trigger.

Review checkpoint đề xuất:
- Khi Wave 10 (AI Branding Completeness) trong master plan shipped → có observability data, có thể earlier Q2 revisit
- Tracking trong ADR-015 §Revisit Schedule (nếu có); nếu không, add calendar reminder

---

## 4. Starter-Kit Status + Update Plan

### Trạng thái hiện tại

**Project này (Kite Platform):**
- ❌ KHÔNG có local copy `.claude/starter-kit/`
- ✅ Có documentation sync workflow trong `.claude/rules/skill-conventions.md` §Starter-Kit Version Management + §Remote Repo Sync
- Role: **contributor/source** of learnings → push to remote, NOT consumer importing from kit

**Remote starter-kit:**
- Repo: [VictorAurelius/claude-starter-kit](https://github.com/VictorAurelius/claude-starter-kit)
- Version: **2.2.0**
- Last commit: **2026-04-04** (16 ngày trước)
- Last features: Vibe Coding guide (#9), UI audit skill v2.1.0 (#8)

**Drift analysis:**

| Metric | Project (Kite Platform) | Starter-Kit remote | Drift |
|--------|:-----------------------:|:------------------:|:-----:|
| Last activity | 2026-04-20 (hôm nay) | 2026-04-04 | **16 days** |
| PRs since remote | 40+ | 0 new | Huge |
| Major learnings | Parallel-agent pattern (3x validated), audit calibration, audit grep scope, governance turnaround | Not propagated | **Stale** |
| Memories (feedback) | 15 memories | No mechanism in kit | **Stale** |

### Learnings cần push lên starter-kit

Dựa trên 40+ PRs + 15 memories + session hôm nay, các learnings SHOULD flow back:

#### A. Rules mới (meta governance)

| Rule file | Motivation | Path in kit |
|-----------|-----------|-------------|
| `post-wave-audit-mandate.md` | Wave merge requires audit within 3 days; hook enforces | `core/post-wave-audit-mandate.md` |
| `meta-gap-priority.md` | Meta gaps (skills/rules/workflow) before feature gaps | `core/meta-gap-priority.md` |
| `audit-to-gap-pipeline.md` | Issue → Gap → Memory → Fix PR flow | `core/audit-to-gap-pipeline.md` |
| `mcp-first-with-fallback.md` | MCP-first tool selection pattern | `core/mcp-first-with-fallback.md` |
| `output-review-mandate.md` | Every output needs review standard + process | `core/output-review-mandate.md` |
| `docs-folder-structure.md` + `planning-docs-structure.md` | README/structure rules for documents/ | `core/` |

#### B. Skills mới

| Skill file | Motivation | Path in kit |
|-----------|-----------|-------------|
| `quality/business-logic-audit/SKILL.md` | Code ↔ rules.md verification /100 | `skills/quality/` |
| `quality/performance-audit/SKILL.md` | Performance baseline /100 | `skills/quality/` |
| `quality/ops-readiness-audit/SKILL.md` | Production ops readiness /100 | `skills/quality/` |
| `quality/security-audit/SKILL.md` | OWASP + deps + secrets /100 | `skills/quality/` |
| `quality/api-contract-audit/SKILL.md` | API ↔ docs sync /100 | `skills/quality/` |
| `workflow/pr-health.md` | PR compliance scanner | `skills/workflow/` |
| `workflow/wave-completion-check.md` | Wave Level 7 audit suite gate | `skills/workflow/` |

#### C. Infrastructure

| Component | Motivation | Path in kit |
|-----------|-----------|-------------|
| `hooks/audit-gate.py` | Block non-docs-only PR without fresh audits | `hooks/` |
| Parallel-agent worktree pattern | Hard rules + respawn procedure | Updates to `starter-kit/docs/parallel-agent-guide.md` (new) |

#### D. Memories (learnings)

Memories are project-specific (personal preferences, session history) nên KHÔNG push direct. Nhưng **pattern documents extracted từ memories** có thể push:

- Audit calibration guide (from `feedback_audit_calibration.md`)
- Audit grep scope guide (from `feedback_audit_grep_scope.md`)
- Parallel-agent hard rules (from `feedback_parallel_agent_strategy.md`)

### Update plan đề xuất

**Option 1 (minimal): Single sync PR**
- Scope: copy 6 rules + 7 skills + audit-gate hook + 3 pattern docs
- Bump VERSION: 2.2.0 → **2.3.0** (MINOR — new rules/skills, not breaking)
- Effort: 2-3 giờ
- Timeline: sau Part C + Wave 5 complete (Month 1)

**Option 2 (recommended): Phased sync matching master plan**
- Sync 1 (sau Part C complete, end Month 1): Meta rules + core audits → bump 2.3.0
- Sync 2 (sau Wave 7 ops + Wave 8 governance, end Month 2): Ops audit skill + output-review skills → bump 2.4.0
- Sync 3 (cuối master plan, end Month 3): Parallel-agent pattern + hook + comprehensive upgrade → bump 3.0.0 (MAJOR — docs structure reorganization)
- Effort: 3 × 2-3 giờ = 6-9 giờ total
- Rationale: don't push untested pattern; sync only after in-project validated

**Option 3 (lazy): One big sync at end of master plan**
- Wait until ALL 12 waves complete (end Month 3)
- Single comprehensive sync → bump 3.0.0
- Risk: 3 months stale = larger drift than 16 days
- Benefit: lowest effort, all-at-once validation

**Recommended: Option 2** (phased).

### Sync process checklist

Per `.claude/rules/skill-conventions.md` §Remote Repo Sync:

```
Before sync:
1. Check remote VERSION: gh api repos/VictorAurelius/claude-starter-kit/contents/VERSION --jq '.content' | base64 -d
2. Identify files to sync (diff local rules/skills vs remote tree)
3. Clone remote: git clone git@github.com:VictorAurelius/claude-starter-kit.git /tmp/kit
4. Apply changes + bump VERSION/CHANGELOG/README (3 files must match)
5. Create PR on remote repo → review → merge

After sync:
6. Tag release on remote
7. Update project's reference to kit version (if any)
8. Document sync in project's `.claude/rules/skill-conventions.md` §Log
```

---

## 5. Master Plan Integration

Cập nhật đề xuất cho `documents/03-planning/roadmap/master-plan-all-gaps-2026-04-20.md`:

### Wave insertions

| Wave # | Name | Insert position | Gaps |
|:------:|------|-----------------|------|
| New 6d | UI/UX Pro Max Integration | After Wave 6 UI Polish (or merge into) | GAP-176 (new) |
| New 14b | Starter-Kit Upstream Sync | After Wave 14 P2 cleanup | No gap (operational) |

### Or simplified (merge into existing)

- **GAP-176** (ui-ux-pro-max integration) → add to Wave 6 UI Polish gap list, mark as sub-sprint 6c
- **Starter-kit sync** → Operational task, add §15 "Operational Tasks Parallel to Waves" in master plan with 3 sync milestones (end Month 1, 2, 3)

---

## 6. Next Actions (recommended)

1. **Create GAP-176** — UI/UX Pro Max Skill Integration (cover plan-ui-ux-design-system-integration.md)
2. **Update master plan** — add Wave 6 sub-scope for GAP-176, add §15 operational starter-kit sync schedule
3. **Schedule starter-kit Sync 1** — end of Part C + Wave 5 (Month 1 target)
4. **No action needed** — awslabs/agent-plugins (deferred per ADR-015)

---

## 7. Log

- **2026-04-20:** Report generated after user asked about coverage of 2 reference repos + starter-kit update plan post 40+ PRs.
