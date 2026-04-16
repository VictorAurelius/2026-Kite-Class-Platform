# GAP-086: Incident Response Runbook

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (trước production)
**Domain:** Operations / SRE
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** Production operations

## Problem

Không có documented process cho: "Production is down. What do I do?"

Khi deploy production, cần: detection → triage → communication → fix/rollback → post-mortem. Hiện tại không có bất kỳ document nào cho flow này.

## Proposed Fix

1. Tạo `documents/05-guides/incident-response.md`:
   - Severity matrix: P1 (data loss) → P2 (service down) → P3 (degraded) → P4 (cosmetic)
   - SLA per severity: P1 = 15min response, P2 = 1h, P3 = 4h
   - Escalation path
   - Communication template (who to notify, how)
2. Tạo `documents/05-guides/post-mortem-template.md`:
   - Timeline, root cause, impact, fix, prevention
3. Tạo skill `devops/incident-response/SKILL.md`:
   - Quick runbook: check health → check logs → identify service → rollback or hotfix
   - Links to monitoring dashboards

## Acceptance Criteria

- [ ] Incident response doc exists
- [ ] Post-mortem template exists
- [ ] Severity matrix defined with SLAs
- [ ] At least 1 practice drill documented
