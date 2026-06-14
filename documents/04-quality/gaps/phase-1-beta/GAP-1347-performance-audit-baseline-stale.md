# GAP-1347: Performance audit baseline stale ~30 ngày — refresh due

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Quality full audit, AUDIT-2026-06-14-quality-full)
**Affects:** `documents/04-quality/audits/performance/**` + `audits-index.csv`

## Problem

Specialist Performance audit gần nhất = **2026-05-15 (Wave 85) 86/100 B+** — đã ~30 ngày. Từ đó tới nay surface backend phình đáng kể: LMS module (LessonAccessGuard + paywall), subscription lifecycle (kitehub-biz-100: dunning/grace/T2P state machine + scheduler), SSO + impersonation, RBAC role-shell. Quality audit Cat 7 hiện carry-forward điểm Performance cũ mà chưa đo lại — có thể che query N+1 mới, missing index trên bảng mới, hoặc scheduler/cron load chưa đánh giá. Các sibling audit khác (security/business-logic/api-contract) đã refresh 2026-06-14; performance là baseline duy nhất chưa refresh trong cụm.

## Root Cause

Performance audit không nằm trong scope wave-p0-closeout-1 audit suite; cadence per `post-wave-audit-mandate` chưa trigger refresh cho domain này.

## Proposed Fix

Chạy `/performance-audit` full refresh tập trung surface mới: LMS paywall query path, subscription scheduler + T2P migration batch, SSO exchange, RBAC guard overhead. Đo DB query (N+1, index coverage), API latency, bundle size FE (ui-kits/landing growth), cache hit. Ghi delta vs Wave 85 baseline 86/100.

## Acceptance Criteria

- [ ] Performance audit report 2026-06-xx tồn tại với score + delta vs Wave 85 86/100
- [ ] Surface mới (LMS + subscription scheduler + SSO + RBAC) được đánh giá explicit
- [ ] Findings (nếu có) filed gap riêng; audits-index.csv có row mới

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 7 / specialized)
- Last baseline: Wave 85 `documents/04-quality/audits/performance/` (86/100, 2026-05-15)
- Skill: `.claude/skills/quality/performance-audit/SKILL.md`
