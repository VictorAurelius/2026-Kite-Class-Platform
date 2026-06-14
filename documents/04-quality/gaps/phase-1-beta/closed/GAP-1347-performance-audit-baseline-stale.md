# GAP-1347: Performance audit baseline stale ~30 ngày — refresh due

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Quality full audit, AUDIT-2026-06-14-quality-full)
**Resolved:** 2026-06-15 (baseline refreshed by 2026-06-14 performance full audit)
**Affects:** `documents/04-quality/audits/performance/**` + `audits-index.csv`

## Problem

Specialist Performance audit gần nhất = **2026-05-15 (Wave 85) 86/100 B+** — đã ~30 ngày. Từ đó tới nay surface backend phình đáng kể: LMS module (LessonAccessGuard + paywall), subscription lifecycle (kitehub-biz-100: dunning/grace/T2P state machine + scheduler), SSO + impersonation, RBAC role-shell. Quality audit Cat 7 hiện carry-forward điểm Performance cũ mà chưa đo lại — có thể che query N+1 mới, missing index trên bảng mới, hoặc scheduler/cron load chưa đánh giá. Các sibling audit khác (security/business-logic/api-contract) đã refresh 2026-06-14; performance là baseline duy nhất chưa refresh trong cụm.

## Root Cause

Performance audit không nằm trong scope wave-p0-closeout-1 audit suite; cadence per `post-wave-audit-mandate` chưa trigger refresh cho domain này.

## Proposed Fix

Chạy `/performance-audit` full refresh tập trung surface mới: LMS paywall query path, subscription scheduler + T2P migration batch, SSO exchange, RBAC guard overhead. Đo DB query (N+1, index coverage), API latency, bundle size FE (ui-kits/landing growth), cache hit. Ghi delta vs Wave 85 baseline 86/100.

## Acceptance Criteria

- [x] Performance audit report 2026-06-xx tồn tại với score + delta vs Wave 85 86/100 — `documents/04-quality/audits/performance/2026-06-14-performance-full-audit.md` = **82/100 B PARTIAL PASS, delta −4 vs Wave 85 86**.
- [x] Surface mới (LMS + subscription scheduler + SSO + RBAC) được đánh giá explicit — audit covered new surface (5-category per-check rubric; DB 17 / API 16 / FE-bundle 17 / Caching 17 / Resources 15).
- [x] Findings filed gap riêng; audits-index.csv có row mới — 12 findings GAP-1356..1367 filed; `AUDIT-2026-06-14-performance-full` row present in `audits-index.csv`.

## Resolution (2026-06-15)

Closeable — the 2026-06-14 performance full audit already refreshed the baseline that this gap flagged as stale: `AUDIT-2026-06-14-performance-full` = 82/100 B (delta −4 vs Wave 85 86/100), report at `documents/04-quality/audits/performance/2026-06-14-performance-full-audit.md`, with 12 findings (GAP-1356..1367) covering the new LMS / subscription-scheduler / SSO / RBAC surface. Baseline is now current; no further action — this gap is a hygiene/cadence tracker that the refresh satisfied. (Several of its child findings — GAP-1357/1358/1365/1366/1367 — are resolved/PARTIAL'd in this same PR.)

## Related

- Discovered in: `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (Cat 7 / specialized)
- Last baseline: Wave 85 `documents/04-quality/audits/performance/` (86/100, 2026-05-15)
- Skill: `.claude/skills/quality/performance-audit/SKILL.md`
