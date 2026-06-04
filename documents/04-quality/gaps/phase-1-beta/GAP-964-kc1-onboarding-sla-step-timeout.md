# GAP-964: Onboarding wizard async — no SLA / step timeout / resumption email

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Onboarding wizard) — trial-clock waste
**Defer-to:** After Wave flow-kh3 finish

## Problem

Onboarding wizard reads `OnboardingProgress.steps`. KHÔNG có timeout nếu user >24h idle. Trial clock starts at instance creation → user might lose days. Plus không có resumption email sau N hours idle. Surfaced: matrix A5×E3×EC5.

## Proposed Fix

Wire `OnboardingProgress.last_activity_at` + scheduled job sweep `onboarding_progress` stuck >24h → send resumption email "Bạn còn 1 bước nữa để hoàn tất...". Trial clock pause-during-onboarding option (business decision).

## Acceptance Criteria

- [ ] `psql -c "SELECT id, created_at, completion_pct FROM onboarding_progress WHERE created_at < NOW() - INTERVAL '2 days' AND completion_pct < 100"` post-fix returns 0 stuck rows
- [ ] Resumption email sent at 24h + 48h idle thresholds
- [ ] Trial pause-during-onboarding decision documented (rules.md update)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md A5×E3×EC5
- Sister: GAP-531 (init handoff PARTIAL 45%) — parent scope, GAP-950 (onboarding wizard)
- Flow Verification Campaign §4 row KC-1
