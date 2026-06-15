---
title: Session Handoff — RBAC G2 + audit fix-wave + cross-flow sweeps + KC-3 walk
date: 2026-06-15
scope: RBAC G2 close · 3 cross-flow sweeps · KC-3 academic walk (recurrence) · 11 PR · 7 bug fix
context_at_close: 96% (1M Opus) — forced handoff per session-end-context-check §3
---

# Session Handoff 2026-06-15 (late PM)

## Scope shipped (11 PR merged, #2438–#2447 + 2 handoff)

| PR | Content |
|---|---|
| #2438 | hardcode/mock state-check audit + GAP-1410 umbrella |
| #2439 | RBAC G2 fixes + **P0 GAP-1413** nil-UUID tenant resolver |
| #2441 | RBAC assign-role searchable user picker (UX) |
| #2442 | gate `/admin/roles` read-only (GAP-1417 — user_roles disconnected from authz, Phase-3 defer) |
| #2443 | KC-3 cluster: recurrence camelCase→snake_case contract drift (GAP-1418) + LMS-DRAFT guard + discoverability; **GAP-1419** cross-flow @JsonProperty snake_case sweep |
| #2444 | branding cross-tenant IDOR (GAP-1420, GAP-1019 sweep miss — regenerate + mintSseToken) |
| #2445 | cache-500 (GAP-1421) — `CacheConfig` `NON_FINAL→EVERYTHING` (cached record DTOs had no root @class) |
| #2446 | landing `getOrCreateDefault` template_type NULL → 500 (GAP-1422) |
| #2440/#2436 | session handoff docs |

## KC-3 — DONE (functional + G2★ human walk)
Full chain verified LIVE end-to-end (direct-to-core `:8088` + user browser walk):
- course-detail 200 (cache fix) → classes-list 200 (landing fix) → class-create no-date **400** / with-date **201** (date fix) → `generate-from-recurrence` snake_case **200 + 17 sessions auto-gen**.
- User confirmed G2★ browser walk "tạo lớp mới" PASS.

## Open PRs (pickup #1)
- **#2447** (class-date required validation, GAP-1423) — `MERGEABLE/UNSTABLE`, only **Code Quality Analysis** pending (0 fail; Test Core Service + ClassControllerTest 16/16 + CourseClassCrudOwnerIT 5/5 passed). **Merge when green** (`gh pr merge 2447 --squash`, no --admin) → then `git worktree remove ../kite-wt-clsdate`. (A ScheduleWakeup was set ~14:03 to auto-merge; if context cleared, do manually.)

## Pickup (in order)
1. Merge #2447 when CI green; cleanup worktree `../kite-wt-clsdate`.
2. **Flip campaign KC-3 row → `🟢 THÔNG (local)`** in `documents/03-planning/roadmap/flow-verification-campaign.md` §4 (G1+G2★ both PASS now; was `🔄 walk-pass-pending-human`).
3. 3 cross-flow sweeps done this session (URL-contract ✅ clean / header-injection ✅ closed +2 IDOR fix / @JsonProperty contract-drift ✅ only recurrence). No follow-up sweep pending.

## Cross-flow sweeps (this session)
- **FE↔BE @JsonProperty snake_case** (GAP-1419): 28 hits/13 files; only RecurrenceRuleDto drifted (fixed); 2FA snake-correct; rest exempt. CI detector HONEST-deferred (recurrence 1).
- **Header-injection**: gateway strips+reinjects all identity headers (X-Tenant/User-Id/Reference-Id/Roles/Email/Tier) — closed; X-Instance-Id bound only in BrandingJobController → swept siblings → fixed GAP-1420.
- **BE→FE URL contract**: detector PASS 4/4.

## Background / survives /clear
- Docker stack UP + healthy (16 containers). **kiteclass-core + kiteclass-frontend rebuilt this session** with ALL fixes (cache EVERYTHING + landing template_type + class-date @NotNull + RBAC + KC-3 recurrence). Verified live.
- Demo tenant `sky-education-074901` (owner `owner+074901@skyedu.vn`/`SkyEdu@2026`, teacher `an.nguyen+074901@skyedu.vn`/`Teacher@2026`, student `mai.pham+074901@gmail.com`/`Student@2026`). Seeded: 1 landing_pages row (template_type=organization, manual seed workaround for the demo) + course 26 + classes (incl class 28 with 17 recurrence sessions from the verify).
- Worktree `.claude/worktrees/agent-a3116…` (parent-fix GAP-1411 from prior session — KEEP until consolidated, NOT pushed).

## Known issues / notes
- **landing `getOrCreateDefault` non-commit churn** (out-of-scope, not erroring): called from `TenantAwareDataSourceInterceptor` per-request; its `save()` may not commit in that context → re-creates each request (perf churn, no 500 after GAP-1422 fix). Demo unblocked via persisted seed row. Candidate follow-up: move landing default-create out of the datasource interceptor.
- **GAP-1417** (user_roles ↔ authz disconnect) OPEN P2 — Phase-3 wiring deferred per GAP-1119; `/admin/roles` gated read-only for beta.
- **Cache format changed** NON_FINAL→EVERYTHING — stale Redis entries flushed on deploy; fresh prod cache starts empty (no issue).

## Anchors
gap-status.csv has GAP-1410..1423 (1417 OPEN, rest DONE/PARTIAL). All on main except #2447 (class-date) pending merge.
