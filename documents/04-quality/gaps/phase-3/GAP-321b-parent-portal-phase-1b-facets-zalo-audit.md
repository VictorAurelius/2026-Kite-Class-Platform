# GAP-321b: Parent Portal Phase 1B — 5 facets + Zalo OTP + per-read audit + multi-children polish

**Status:** 🟡 PARTIAL — foundation shipped Wave 18b2 Bucket C
**Priority:** 🔴 P0 LEGAL (sister of GAP-321 Phase 1A SHIPPED Wave 18b1)
**Domain:** Backend + Frontend
**Detected:** 2026-05-04 (Wave 18b1 Bucket D closure)
**Affects:** P5 K-12 (Luật GD Đ.83 mandate full coverage); P2/P3 secondary

## Context

Phase 1A SHIPPED Wave 18b1 (PR #766): `/parent` route + `/parent/transcript/[childId]` + ParentTranscriptController scoped via ParentStudentLink + 3-layer business docs + 5-attribute frontmatter. Reused V42 GAP-052a entities; no migration needed Phase 1A.

This gap covers Phase 1B — the 5 remaining facets + identity/auth + observability foundation for legal compliance.

## Problem

Phase 1A delivered transcript only. Luật GD Đ.83 K2 mandates parents see "đầy đủ thông tin về quá trình học tập, rèn luyện của con" — needs all 6 facets. Plus VN K-12 reality requires Zalo OTP login (email/password too friction-heavy for parent demographic), per-read audit log for legal compliance, and proper multi-children selector.

## Proposed Fix

### 1B.1 — 5 remaining facets (read-only)
- `GET /api/v1/parent/children/{id}/attendance` — period attendance (uses GAP-323 P1A AttendancePeriod when K12_SCHOOL tenant; falls back to per-day for CENTER)
- `GET /api/v1/parent/children/{id}/fees` — học phí breakdown + payment history
- `GET /api/v1/parent/children/{id}/conduct` — hạnh kiểm per kỳ (uses existing Grade module)
- `GET /api/v1/parent/children/{id}/notifications` — notifications log per child
- `GET /api/v1/parent/children/{id}/discipline` — kỷ luật history (depends GAP-322 Incident type or new module)
- FE pages: 5 drill-down routes mirroring transcript pattern

### 1B.2 — Zalo OTP login flow
- Add Zalo OAuth flow on top of existing Gateway PARENT user type
- Phone-number-first registration (Zalo bound to phone)
- Email/password retained as fallback
- Test against Zalo OA sandbox

### 1B.3 — Multi-children selector polish
- Phase 1A shows children list; Phase 1B adds card layout per child + "current term summary" preview
- Sticky header with child switcher

### 1B.4 — Per-read audit log
- New table `parent_read_audit_log` (parent_id, child_id, facet, viewed_at, ip, user_agent, instance_id)
- Emit log entry on every parent-side facet API call
- Retention 5 years (financial-record class per ND-13/2023)
- Admin/safeguarding-officer can query

### 1B.5 — Bulk import xlsx (depends GAP-325)
- Excel template: `Tên Cha, SĐT Cha, Email Cha, Tên Mẹ, SĐT Mẹ, Email Mẹ`
- Sibling dedup logic
- Background processing job

## Acceptance Criteria

- [ ] All 5 facet endpoints implemented + scope-guard via ParentStudentLink (BR-PARENT-PORTAL-001 reused)
- [ ] All 5 FE drill-down pages render with i18n (vi primary)
- [ ] Zalo OTP login flow tested against sandbox (or feature-flag if sandbox unavailable)
- [ ] Per-read audit log emits on every parent API call (verified by integration test)
- [ ] Multi-children selector polish (cards + switcher)
- [ ] Bulk import xlsx (or defer to GAP-325 if scope grows)
- [ ] Business docs updated: BR-PARENT-PORTAL-002..010 + UC-PARENT-{ATTENDANCE/FEES/CONDUCT/NOTIFICATIONS/DISCIPLINE}
- [ ] Tests: per-facet IT + Zalo flow IT + audit log IT
- [ ] mvn + pnpm green; full FE strict-build green

## Estimated Effort

~2-3 weeks. Can split into:
- 321b.1: 5 facets + tests (~5-7 days)
- 321b.2: Zalo OTP integration (~3-5 days)
- 321b.3: Per-read audit log (~2 days)
- 321b.4: Multi-children polish (~1-2 days)
- 321b.5: Bulk import (~3 days, may defer to GAP-325)

## Related

- **Sister of:** GAP-321 Phase 1A (PR #766 merged 2026-05-04)
- **Depends on:** GAP-323 (period attendance for parent attendance facet), GAP-322 (Incident for discipline facet)
- **Cross-cuts:** GAP-063b (notification facet uses Phase 2 notification engine), GAP-322c (audit log pattern shared)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18b1-k12-legal-phase-1a.md`

## Log

- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Phase 1A SHIPPED (transcript facet only); Phase 2-4 deferral made explicit per `gap-done-discipline.md` §3.
- **2026-05-04** — Phase 1B foundation shipped Wave 18b2 Bucket C (this PR). Backend skeleton for 4 of 5 facets (attendance / fees / conduct / notifications — discipline deferred to GAP-321c) + per-read audit log entity + V53 migration + 5 new business rules (BR-PARENT-AUDIT-001 + BR-PARENT-FACET-{ATT,FEES,CONDUCT,NOTIFY}-001) with 5-attribute frontmatter + 3-layer docs (rules.md / use-cases.md / api-contract.md). Tests: 1 audit unit + 1 audit fan-in IT covering all 4 facets with linked + unlinked + inverted-range cases + 4 controller WebMvc IT (200 + 403). 1230/1230 mvn green. Conduct + notifications + (fees date-range narrowing) ship as v1 stubs returning empty results — concrete data sources deferred to GAP-321b.1. FE drill-down pages, Zalo OTP, multi-children polish, audit-log query surface, retention sweeper, bulk import all remain OPEN under follow-up sub-PRs (321b.1..5).
- **2026-05-04** — Wave 18b3 Bucket C (PR #781) shipped fees facet real wiring: date-range JPQL query joining `Invoice` + `Payment` filtered by `parentStudentLink.studentId` ordered by `dueDate DESC` + `@EntityGraph` for N+1 prevention + `assertSelectCount ≤3` IT + new `ParentFeesFacetEntityGraphIT` (env-gated `ENABLE_INTEGRATION_TESTS=true`) + 12 test additions (9 unit + 1 IT + 1 controller-IT + 1 audit-IT regression fix). 96/96 parent + invoice tests green. Conduct + notifications stay v1 stubs after agent state-check found `Incident.visibilityScope` field + `BR-CHILD-PROTECT-005` rule + `Notification` entity all 0 matches in codebase — honest PARTIAL exit-ramp per `gap-done-discipline.md` §3. **3 sub-gaps filed** by agent: GAP-321b.1-fees-instalment-payment-history (P2 v2 enrichment), GAP-321b.1-conduct-incident-visibility (P1 needs visibility column + Incident schema sync), GAP-321b.1-notifications-engine-wiring (P1 hard-blocked by GAP-063b notification engine). Visibility-scope edge-case tests (`staffOnlyIncidentEquivalent_notExposedToParent` + `staffOnlyAudienceEquivalent_notExposedToParent`) pass trivially today against empty stubs but become regression contracts when GAP-321b.1-conduct + GAP-321b.1-notifications land. Status stays 🟡 PARTIAL — Phase 1C scope (granular consent, write actions, EN/zh-CN i18n, cursor pagination) remains open.
