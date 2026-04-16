# GAP-083: Gap Triage & Sprint Assignment Process

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Project Management / Quality
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** 80+ gap files in documents/04-quality/gaps/

## Problem

`audit-to-gap-pipeline.md` defines: audit → gap file → fix PR. Nhưng thiếu bước giữa: **triage** — ai quyết định gap nào fix trước? Gom vào wave nào? Sprint nào?

Hiện tại ROADMAP.md có epics/sprints nhưng gap assignment là ad-hoc. Khi audit tạo 5-10 gaps mới, chúng pile up vì không có triage process.

## Proposed Fix

1. Tạo skill `workflow/gap-triage/SKILL.md`:
   - Input: list gaps (new or backlog)
   - Process: score impact × effort → rank → group by domain → assign sprint
   - Output: updated ROADMAP.md với gap assignments
2. Triage matrix:
   - Impact: Blocks production (5) → Blocks feature (3) → Inconvenience (1)
   - Effort: Quick fix <2h (1) → Half day (3) → Multi-day (5)
   - Priority = Impact / Effort (higher = fix first)
3. Run after mỗi audit hoặc khi backlog > 10 unassigned gaps

## Acceptance Criteria

- [ ] Skill file tồn tại
- [ ] All 80+ existing gaps có sprint assignment trong ROADMAP.md
- [ ] New gaps auto-triaged within 1 session of creation
