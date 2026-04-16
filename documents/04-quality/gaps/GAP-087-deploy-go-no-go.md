# GAP-087: Deploy Go/No-Go Decision Checklist

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (trước production)
**Domain:** Operations / Release Management
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** Production deployments

## Problem

`ops-readiness-audit` đánh giá infrastructure readiness nhưng không tạo deploy decision. Không có checklist: "Có nên deploy version này không?"

Thiếu: audit scores pass? Tests green? No open incidents? Rollback plan ready? Team available?

## Proposed Fix

1. Tạo skill `devops/deploy-checklist/SKILL.md`:
   - [ ] All audit scores ≥ threshold (quality /100 ≥ 85, UI /128 ≥ 70 lowest)
   - [ ] CI green on main (0 failing workflows)
   - [ ] No P0/P1 open gaps blocking this release
   - [ ] Rollback procedure documented + tested
   - [ ] Monitoring dashboards accessible
   - [ ] On-call person identified
   - [ ] Database backup taken
   - [ ] Feature flags configured (critical features off by default)
   - [ ] Communication sent (maintenance window if needed)
   - Decision: GO / NO-GO with reasons

## Acceptance Criteria

- [ ] Skill file tồn tại
- [ ] Checklist executed before first production deploy
- [ ] Decision documented with sign-off
