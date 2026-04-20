---
name: GAP-176 — UI/UX Pro Max skill integration
description: Adopt nextlevelbuilder/ui-ux-pro-max-skill (161 industry rules, 67 styles, 161 palettes) to make UI audit objective + upgrade design quality
type: gap
---

# GAP-176: UI/UX Pro Max Skill Integration

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (feature + meta skill upgrade)
**Domain:** UI/UX / Skills / Frontend
**Found:** 2026-04-20 (reference-repos-and-starter-kit-coverage report)
**Affects:** UI audit skill scoring, tenant-facing UI quality, FE design decisions across both frontends

## Problem

Current ui-review skill scores UI /128 per-screen across 5 dimensions (Technical, Heuristics, Visual, Friendliness, WCAG) but is **output validator** — không phải **design advisor**. Reviewer nói "Visual Aesthetics: 18/28" mà không có framework lý giải TẠI SAO hoặc NÊN LÀM GÌ để nâng điểm.

Reference repo `nextlevelbuilder/ui-ux-pro-max-skill` (community-vetted, multi-star) cung cấp:
- 161 industry-specific reasoning rules (education, healthcare, finance...)
- 67 UI styles catalog (Glassmorphism, Brutalism, Bento, AI-Native...)
- 161 color palettes mapped 1:1 với product types
- 57 font pairings + Google Fonts imports
- 99 UX guidelines + anti-patterns (e.g., "banking → avoid AI purple/pink")
- Pre-delivery checklist (7 items)

Plan doc đã tồn tại (`documents/03-planning/plans/plan-ui-ux-design-system-integration.md`, 2026-04-18) với 3-PR execution outline, nhưng không tracked như gap và không có wave assignment.

## Root Cause

Plan doc được tạo standalone mà không convert thành gap + integrate vào master plan. Kết quả: plan sitting unreviewed 2+ ngày, risk bị quên.

## Proposed Fix

**Approach: Hybrid (per plan doc §Approach):**
- Install uipro-cli globally (community distribution, 161 rules as-is)
- Internal upgrade to ui-review skill to reference external rules when scoring
- Add Vietnamese education context layer (MOET colors, VN typography, K-12 patterns)

**3-PR execution (per plan doc §3-PR Execution):**
1. PR 1 — Install + integrate (1-2 giờ): uipro-cli setup + `.claude/skills/ui-ux-pro-max/` vendored + playbook `documents/05-guides/ui-design-system-setup.md`
2. PR 2 — ui-review skill upgrade (2-3 giờ): reference external rules for scoring, dimension explainers, "design advice" section per score
3. PR 3 — Vietnamese context layer (1-2 giờ): MOET MOET-blue variant, VN typography pairings, K-12 UX patterns

**Sequencing:** Blocked on Part C Sprint 2 (GAP-127 FE code-splitting) completion. Need clean FE baseline trước khi adopt new design reasoning.

**Wave integration:** Merge as sub-scope 6c của Wave 6 UI Polish (per master plan update this PR).

## Acceptance Criteria

- [ ] uipro-cli installed + vendored skill folder committed
- [ ] Playbook in `documents/05-guides/ui-design-system-setup.md`
- [ ] ui-review skill references external rules in scoring rubric
- [ ] Vietnamese context layer documented (MOET palette, VN fonts, K-12 patterns)
- [ ] 3 example screens re-scored with new advisor output showing "why X/Y" reasoning
- [ ] Plan doc `plan-ui-ux-design-system-integration.md` updated to reference this gap

## Related

- Plan doc: `documents/03-planning/plans/plan-ui-ux-design-system-integration.md`
- Parent wave: Wave 6 UI Polish (master plan §Wave 6)
- Dependency: Part C Sprint 2 (GAP-127 FE code-splitting)
- Reference: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
- Reference report: `documents/03-planning/analyses/reference-repos-and-starter-kit-coverage-2026-04-20.md`
