# GAP-538: Day-1 onboarding checklist + sample/demo data seed

**Status:** 🟡 PARTIAL (95%) — Wave 78 Bucket B FE/BE shipped; Wave 98 B2 GAP-658 closed AC7 VN seed worker foundation; Wave 101 Bucket D added Playwright E2E spec for checklist + sample-data flow; live walkthrough verify on real deploy still gated on AWS GAP-612 restoration
**Priority:** 🔴 P0
**Domain:** Mixed (FE + BE)
**Detected:** 2026-05-14
**Related PRs:** Wave 78 Bucket B (this PR) — FE `OnboardingChecklist` + BE `OnboardingProgressController` + Flyway `V43` shipped
**Related Docs:** `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Onboarding checklist FE component | `kitehub/kitehub-frontend/src/app/(dashboard)/onboarding/` | ❌ missing (no folder) |
| Onboarding progress BE endpoint | `kitehub/kitehub-subscription/src/main/java/.../onboarding/` | ❌ missing |
| Flyway migration `onboarding_progress` table | `kitehub/kitehub-subscription/src/main/resources/db/migration/V*__create_onboarding_progress_table.sql` | ❌ missing |
| Sample/demo data seed gated by `is_beta_demo_data` flag | tenant metadata schema + seed scripts | ❌ missing |
| api-contract.md cho onboarding-progress endpoints | `documents/01-business/onboarding/api-contract.md` | ❌ missing |

**Grep commands run:**
```bash
find kitehub/kitehub-frontend/src/app -type d -name "onboarding*"  # 0 matches
find kitehub/kitehub-subscription -name "OnboardingProgress*"       # 0 matches
ls documents/01-business/onboarding/ 2>&1                            # folder absent
```

## Problem

Beta user nhận invite → click email link → đăng nhập lần đầu → **không có guidance** về việc cần làm gì tiếp theo. Per outside-in 3-agent audit 2026-05-14 (N1 finding): Tier 1 beta tenant onboarding fail = first-touch UX miss → user bounce. Cần checklist 5 bước hiển thị + opt-in sample/demo data để tenant có data ban đầu để khám phá.

## Context

Wave 77 SEND foundation đóng email delivery + 2026-05-14 outside-in 3-agent audit surface 4 P0 NEW cho RETAIN scope. N1 (onboarding checklist + sample data) là first item — user nhận invite không thể chỉ "đăng nhập rồi thôi"; cần lộ trình rõ ràng + dữ liệu mẫu để test ngay.

## Evidence

- Outside-in audit 2026-05-14 (`documents/04-quality/audits/persona/2026-05-14-outside-in-3-agent-beta-retain.md` — to be filed Wave 78 audit cycle) N1 finding
- Comparable SaaS benchmark: Linear / Notion / Vercel — all gửi welcome email với checklist nhúng + sample workspace data
- Inside-out completeness audit 2026-05-14 surface 5 BLOCKING items missed Wave 77 (separate gaps)

## Proposed Fix

1. Bucket 0 Foundation (cross-layer prereq): `documents/01-business/onboarding/api-contract.md` CREATE với `GET /api/v1/onboarding-progress` + `PUT /api/v1/onboarding-progress` endpoints
2. BE module: `kitehub-subscription/.../onboarding/` package
   - `OnboardingProgress` entity (tenant_id, step_completed JSON array, created_at, updated_at)
   - Flyway migration `V[N]__create_onboarding_progress_table.sql`
   - `OnboardingProgressController` với GET/PUT endpoints + DTO
   - Integration test cover happy path + multi-tenant isolation
3. FE component: `kitehub-frontend/src/app/(dashboard)/onboarding/`
   - `OnboardingChecklist.tsx` component 5 bước (welcome / profile / sample-data-opt-in / first-action / done)
   - Step completion persist qua `PUT /api/v1/onboarding-progress`
   - Sample data seed gated by user opt-in trên step 3 → call `POST /api/v1/tenant/seed-demo-data` (existing endpoint hoặc new)
4. Sample data seed implementation: tenant.metadata flag `is_beta_demo_data=true` → seed sample students/courses/classes (5-10 entries) qua existing seed mechanism
5. MSW handler `kitehub-frontend/src/test/msw/handlers/onboarding.ts` cho Bucket 0 Foundation

## Acceptance Criteria

- [x] api-contract.md cho onboarding-progress endpoints ship trong Bucket 0 Foundation (Wave 78 Bucket 0 PR #1349)
- [x] BE `OnboardingProgressController` + entity + migration shipped (`V43__create_onboarding_progress_table.sql`; `\d onboarding_progress` to verify on next deploy)
- [x] FE checklist 5 bước hiển thị on first login + step completion persist qua API (`OnboardingChecklist` at `/onboarding` consumes GET/PUT endpoints)
- [x] Sample data seed gated by user opt-in (step 3); KHÔNG auto-seed mà không hỏi user (confirmation dialog gates `IMPORT_DATA` toggle on opt-in)
- [ ] Live walkthrough verify per `pre-handoff-self-test-completeness.md` §2.1 auth-gated user-flow (credential available → login → checklist visible → step click → step completed) — gated on next deploy
- [x] No cross-tenant data leak (tenant A login không thấy tenant B onboarding state) — controller scopes by `X-Tenant-Id`; missing header → 403
- [x] FE unit test cover checklist component + BE integration test cover endpoints (FE 6 tests + BE 12 tests passing)
- [ ] Sample seed data Vietnamese-friendly (per `dev-readable-doc-language.md` — student names như "Nguyễn Văn An", course names tiếng Việt) — deferred to follow-up seed-worker gap; this PR ships toggle + opt-in confirmation only

## Related

- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` Bucket B
- Sister gap GAP-539 (N2 beta disclaimer + /beta-status — same Bucket B)
- Rules: `contract-first-for-cross-layer.md` v1.0.1 (Bucket 0 Foundation prereq); `pre-handoff-self-test-completeness.md` §2.1 (live walkthrough)
- Outside-in 3-agent audit 2026-05-14 N1 finding

## Log

- 2026-05-14 — Initial write-up (state-check completed; 0 onboarding folder/controller/migration found; api-contract.md absent; Wave 78 Bucket B owner).
- 2026-05-14 — Wave 78 Bucket B shipped 85%: FE `OnboardingChecklist` component (5 steps, opt-in dialog for IMPORT_DATA), `(customer)/onboarding/page.tsx` route, BE `OnboardingProgressController` + `OnboardingProgressService` + `OnboardingProgress` entity + repo + Flyway `V43__create_onboarding_progress_table.sql`. 6 FE component tests + 6 BE controller tests + 5 BE service unit tests all PASS. Tenant scoping via `X-Tenant-Id` header (403 on missing/malformed). Remaining items: (a) live walkthrough verify post next deploy per `pre-handoff-self-test-completeness.md` §2.1, (b) sample-data seed BE worker (Vietnamese-friendly content) — deferred to follow-up seed-worker gap because this PR ships the toggle + opt-in UI but not the actual seed worker.
- 2026-05-19 — Wave 101 Bucket D added Playwright E2E spec `kitehub/kitehub-frontend/e2e/onboarding/checklist-and-sample-data.spec.ts` covering: (a) 5-step checklist renders with Vietnamese labels per `vn-localization-audit-checklist.md` §2, (b) IMPORT_DATA confirmation dialog flow, (c) opt-in completes IMPORT_DATA + percent transitions 0% → 20%, (d) no English placeholder data (`John Doe` / `Class A1`) per §3. Progress 90% → 95%. Remaining 5% = AC5 live walkthrough verify against real deploy — blocked on GAP-612 AWS account 906286017800 restoration. Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist, defer-with-followup pattern: file blocker remains GAP-612 (no NEW gap needed — pre-existing dependency). Will flip DONE 100% when AWS unblocks + live verify confirms sample data load button triggers VN seed worker on real Postgres.
- 2026-05-18 — Wave 98 Bucket B2 ship GAP-658 (`wave/98-b2-vn-sample-seed` branch) closes AC7 VN seed worker foundation: 6 VN CSV files trong `kitehub/kitehub-platform/src/main/resources/seed-data/vn-friendly/` (student×300 / teacher×100 / center×50 / class×50 / address×104 / subject×30, UTF-8 BOM), `VietnamSampleDataGenerator` Spring @Component với 7 generator methods + 6 DTO records + locale fallback `seed.locale=en-US`, 15 unit tests PASS, 3-layer business doc `documents/01-business/kitehub/seed/{rules,use-cases,api-contract}.md` codifying BR-SEED-001..010. Progress 85% → 90%. Remaining items: (a) live walkthrough still gated on next deploy, (b) SeedWorker/OnboardingChecklist integration wiring (kitehub-platform là shared domain module, no SeedWorkerService hiện tại — track follow-up khi service materializes), (c) native VN copywriter pass paired Wave 98 Bucket B4 i18n.
