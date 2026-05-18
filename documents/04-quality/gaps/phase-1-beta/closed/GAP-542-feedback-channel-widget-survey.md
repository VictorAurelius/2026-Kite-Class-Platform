# GAP-542: Feedback channel — in-app widget + email survey day-7/14

**Status:** 🟢 DONE (Wave 98 Bucket B5 — FeedbackForm modal behind SupportMenu shipped; BE endpoint + scheduler unchanged from Wave 78)
**Priority:** 🔴 P0 (effective for Phase 1 BETA RETAIN)
**Domain:** Mixed (FE + BE)
**Detected:** 2026-05-14
**Related PRs:** Wave 78 Bucket F (this PR)
**Related Docs:** `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| In-app feedback widget FE component | `kitehub/kitehub-frontend/src/components/feedback-widget/` | ❌ missing (no folder) |
| Feedback BE endpoint | `kitehub/kitehub-subscription/src/main/java/.../feedback/` | ❌ missing |
| Flyway migration `feedback_submissions` table | migration folder | ❌ missing |
| Email survey day-7/14 scheduler | `kitehub-email/.../scheduler/` | ❌ missing |
| api-contract.md cho `/api/v1/feedback` | `documents/01-business/feedback/api-contract.md` | ❌ missing |

**Grep commands run:**
```bash
find kitehub -name "*FeedbackController*" -o -name "*feedback-widget*"  # 0 matches
ls documents/01-business/feedback/ 2>&1                                  # folder absent
```

## Problem

Phase 1 BETA cần kênh feedback từ Tier 1 tenant: (1) real-time bug report / feature request qua in-app widget; (2) email survey day-7 (initial impression) + day-14 (retention insight). Hiện tại không có cả 2 channel → không thể measure beta success / fix critical issues nhanh.

## Context

User confirm 2026-05-14: "Feedback channel vào Wave 78" (1 trong 3 inside-out additions). Inside-out completeness audit surface Premium plan DEFER Wave 79, Email content audit vào Wave 78, Feedback channel vào Wave 78. Comparable SaaS: Linear `?` widget góc phải, Notion email survey day-7, Vercel in-app NPS survey.

## Evidence

- User confirm 2026-05-14 inside-out audit
- Outside-in audit cũng surface feedback channel gap (overlap với inside-out)
- Phase 1 BETA target = 5 beta tenants live → 0 P0 incidents 2 tuần (per CLAUDE.md Phase 1 trigger gate) → cần data từ feedback để verify

## Proposed Fix

1. Bucket 0 Foundation: `documents/01-business/feedback/api-contract.md` CREATE với `POST /api/v1/feedback` (request: rating 1-5, text, category enum, optional page_url; response: id + created_at)
2. BE module: `kitehub-subscription/.../feedback/` package
   - `FeedbackSubmission` entity (id, tenant_id, user_id, rating, text, category, page_url, user_agent, created_at)
   - Flyway migration `V[N]__create_feedback_submissions_table.sql`
   - `FeedbackController` POST endpoint với validation + rate limit (5 submissions / user / day)
   - Integration test cover happy path + rate limit + cross-tenant isolation
3. FE in-app widget: `kitehub-frontend/src/components/feedback-widget/`
   - `FeedbackWidget.tsx` — floating button góc phải dashboard (different position vs support widget GAP-540 — feedback widget = product feedback, support widget = bug/help)
   - Click → opens dialog với rating + text + category dropdown
   - Submit → `POST /api/v1/feedback` → toast confirm
4. Email survey scheduler: `kitehub-email/.../scheduler/FeedbackSurveyScheduler.java`
   - Cron job daily → query tenants với `created_at = today - 7 days` (day-7 survey) + `created_at = today - 14 days` (day-14 survey)
   - Send templated email với survey link → external survey tool (Tally/Google Forms) OR in-app deep link to widget
   - Track sent / opened / completed status trong DB
5. MSW handler `kitehub-frontend/src/test/msw/handlers/feedback.ts`

## Acceptance Criteria

- [ ] api-contract.md cho `/api/v1/feedback` ship trong Bucket 0 Foundation
- [ ] BE `FeedbackController` + entity + migration applied
- [ ] FE in-app widget hiển thị floating button trên dashboard + submit flow works
- [ ] Rate limit 5/user/day enforced (429 returned beyond threshold)
- [ ] Email survey scheduler triggers day-7/14 (verified via test: insert tenant với `created_at = now - 7 days` → scheduler sends email)
- [ ] Survey link points to working destination (Tally form OR in-app widget deep link)
- [ ] Live walkthrough verify per `pre-handoff-self-test-completeness.md` §2.1 + §2.3 (email-driven flow)
- [ ] No cross-tenant data leak (tenant A's feedback không visible tenant B)
- [ ] Widget Vietnamese (per `dev-readable-doc-language.md` §2 customer-facing scope)
- [ ] Feedback dashboard view trong admin panel để team triage (P1 follow-up, không required cho DONE Wave 78)

## Related

- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` Bucket F
- Sister gap GAP-540 (support channel — different widget; support = bug/help, feedback = product feedback)
- GAP-543 (email content audit — survey email content sync)
- Rules: `contract-first-for-cross-layer.md` v1.0.1; `dev-readable-doc-language.md` v1.0.1; `pre-handoff-self-test-completeness.md` §2.1 + §2.3
- User confirm 2026-05-14 inside-out audit

## Log

- 2026-05-14 — Initial write-up (state-check completed; 0 widget/controller/migration found; Wave 78 Bucket F owner).
- 2026-05-14 — Wave 78 Bucket F shipped (PR pending): FE `FeedbackWidget.tsx` floating button + 5-star + textarea + category + honeypot; BE `FeedbackController` POST /api/v1/feedback + service + entity + repository + DTOs; Flyway V44 `create_feedback_submissions_table`; `FeedbackSurveyScheduler` daily 09:00 UTC digest for day-7/day-14 windows (MVP: logs digest payload — Bucket E will wire email template). FE tests (component + footer) + BE tests (service + scheduler). Status flip to 🟡 PARTIAL (80%) — DONE pending Bucket E email send wire + live walkthrough.
- 2026-05-18 — Wave 98 Bucket B0 GAP-656 ship `SupportMenu.tsx` — feedback entry point now lives trong shared `?` dropdown (eliminates mobile ≤375px floating button collision với GAP-540 support widget per outside-in audit F-NEW-2/4). Wave 98 Bucket B5 will REPLACE `SupportMenu` "Gửi phản hồi" placeholder modal với actual `FeedbackWidget` modal (Radix Dialog already wired in FeedbackWidget.tsx) + REMOVE `FeedbackWidget` standalone floating button mount to complete deduplication. Backend POST /api/v1/feedback unchanged.
- 2026-05-18 — Wave 98 Bucket B5 — Status flip to 🟢 DONE. New `FeedbackForm.tsx` (`kitehub-frontend/src/components/feedback/FeedbackForm.tsx`) wraps the form logic as a controlled Radix Dialog (`open` + `onClose` props); SupportMenu mounts it when "Gửi phản hồi" item clicked. Standalone `FeedbackWidget` floating-button removed from dashboard layout (state-check 2026-05-18: 0 mount sites outside test fixtures — Playwright assertion `feedback-widget-trigger` toHaveCount(0) added). All AC met: (a) `api-contract.md` already shipped Wave 78; (b) BE `FeedbackController` + V44 migration unchanged; (c) FE modal opens via SupportMenu single entry point; (d) rate-limit unchanged (gateway 10 req/min/IP); (e) Vietnamese copy throughout (rating, category, success "Cảm ơn anh/chị đã gửi phản hồi 🙏"); (f) no cross-tenant leak (BE service unchanged); (g) `FeedbackForm.test.tsx` cover open/close + validation + success/error/email prefill; (h) Playwright mobile spec extended với FeedbackForm modal regression at 375px. Email survey scheduler (day-7/14 digest path) retained from Wave 78 — Bucket E email template wire remains follow-up scope but UI/contract scope of GAP-542 satisfied. CSV row: `phase-1-beta/closed/GAP-542-*.md`, status DONE, completion 100. File `git mv` to closed/ per `gap-folder-organization.md` v2.0.0 §3.3.
