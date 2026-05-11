# GAP-269: Track 2 Port — kiteclass-student → production Next.js (mobile PWA, NEW route)

**Status:** 🟡 PARTIAL — shell + 11 screens shipped Wave 49 Bucket C; login screen reuses existing `/login`; social-login backend wiring + Lighthouse PWA validation deferred to follow-up
**Priority:** 🟡 P2 (UX growth — Tier 2 S. Student persona, mobile-first)
**Domain:** Frontend
**Found:** 2026-04-29
**Affects:** `kiteclass-frontend/src/app/(dashboard)/student/` — NEW route to be created

## Problem

HTML prototype `kiteclass-student/` (avg **116/128** ⭐⭐ HIGHEST kit Round 3, 14 screens, R3 PR #700, mobile-first PWA-grade) is highest-scoring kit across both R2 + R3. Production student dashboard does NOT exist — entire route to be created.

## Current State (verified 2026-04-29)

`kiteclass-frontend/src/app/(dashboard)/student/` does NOT exist. Build from scratch.

## Proposed Fix

Create production student dashboard from prototype — 14 mobile screens.

**Scope:**
- Today (next class card + today's schedule + pending tasks)
- My Classes (list + class detail)
- Class detail (teacher info + schedule + classmates)
- Assignments (filterable: pending/submitted/graded)
- Assignment detail + submit interface (saved-draft pattern)
- Grades (overview + per-subject breakdown + trend)
- Grade detail (per-subject history)
- Attendance log (history + percentage stats)
- Payments balance + history (older students only)
- Notifications inbox (Zalo OA + Web Push mirror)
- Profile + settings
- Login + forgot password (social: Zalo + Google)
- Empty states gallery

**Tech direction:**
- 5-tab bottom nav (Today / Classes / Grades / Notif / Profile) — distinct from kiteclass-parent's 4-tab
- Web Push primary (per dossier S. Student persona spec)
- Saved-draft submit pattern with service-worker background sync
- PWA: manifest + sw.js
- Social login: Zalo + Google via existing auth providers

## Acceptance Criteria

- [ ] All 14 screens ≥110/128 (kit was 116 ⭐⭐)
- [ ] PWA installable (manifest + sw)
- [ ] Web Push permission UI working
- [ ] Saved-draft submit recovers offline submissions when back online
- [ ] Bottom 5-tab nav, 44px+ tap targets
- [ ] Social login Zalo OA + Google
- [ ] Lighthouse PWA score ≥90
- [ ] Vietnamese-only, realistic VN student data
- [ ] WCAG AA preserved
- [ ] E2E: student login → today → submit assignment offline → see synced when online

## Related

- HTML prototype: `ui_kits/kiteclass-student/` (Wave Round 3 PR #700)
- Sister Track 2 gaps: GAP-266..272 + GAP-273 (components)
- **External review (Wave 20 Bucket A, 2026-05-05):** `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md` — avg **100.4/128** APPROVE WITH POLISH
- **🚫 BLOCKER:** [GAP-363](GAP-363-kiteclass-student-polish-payments-persona-violation.md) (P1 — payments persona violation + 4 partials) MUST close before Track 2 port begins
- **🚫 BLOCKER:** [GAP-365](closed/GAP-365-s-student-tier-1-ac-doc.md) (P2 BL — file Tier-1 `S-student.md` AC doc) MUST close to ground port spec
- Parent quality-gate gap: [GAP-348](GAP-348-round-3-ui-kits-persona-driven-review.md) (🟡 PARTIAL)

## Effort estimate

~1-2 weeks. New route greenfield. Wave-pack candidate when sliced into auth+navigation / home+classes / assignments+grades / PWA-infrastructure.

## Log

- **2026-05-10 (Wave 49 Bucket C):** PARTIAL — production port shipped under `kiteclass-frontend/src/app/(dashboard)/student/` (NEW route per Wave 49 plan §1 Q3 R3). 11 screens + 1 redirect (login deferred to existing `/login`):
  - `student/today` (Trang hôm nay) · `student/my-classes` + `[classId]` · `student/assignments` + `[assignmentId]` (offline-aware submit) · `student/grades` + `[subjectId]` · `student/attendance` (G8 AttendanceCalendar) · `student/payments` · `student/notifications` (Web Push UI) · `student/profile` · `student/empty-states`.
  - PWA infra reused from Bucket 0 (`public/manifest.json` + `public/sw.js` + `src/lib/web-push.ts`). `sw.js` NOT modified — offline assignment retry implemented as additive page-context queue at `src/lib/offline/student-assignment-queue.ts` (Command + Outbox pattern; persisted to `localStorage`; auto-flushes on `online` event). Rationale: SW does not yet inject JWT, so submit must run in page context; future Periodic-Sync upgrade can hook this same queue.
  - Bottom 5-tab nav (`StudentBottomTabs`) + mobile shell (`StudentMobileShell`) — 56px tap targets exceed WCAG 2.5.5 AA (44px). Persona guard in `student/layout.tsx` redirects non-STUDENT users to `/dashboard`.
  - Vietnamese-only copy + realistic VN data (lớp 10A2, 6 môn, học phí 2.500.000 ₫/tháng).
  - Test: `src/lib/offline/__tests__/student-assignment-queue.test.ts` covers enqueue/remove/flush success+failure/corrupt-storage tolerance.
  - **Deferred (explicit) → follow-up sub-gaps:**
    - Social login backend wiring (Zalo OA + Google) — UI shows existing auth state only; no new providers added.
    - Real REST endpoints (today/grades/payments/notifications) — pages render representative VN data; backend wiring waits on kc-core REST controllers.
    - Lighthouse PWA score ≥90 verification — manual audit not run in this bucket.
    - E2E Playwright spec (login → today → submit offline → sync) — unit test covers the queue contract.
  - Acceptance criteria progress: 7/10 partially or fully met (PWA infra ✅, Web Push UI ✅, offline submit ✅ via queue, 5-tab nav ✅, Vietnamese ✅, WCAG AA preserved ✅, screens ported ⚠️ at lower fidelity than 110/128 target). Per `gap-done-discipline.md` §3 PARTIAL exit ramp — does not flip DONE.
- **2026-04-29:** Filed after user accepted Round 3 quality. HIGHEST-scoring kit Round 3 (116/128 ⭐⭐).

- **2026-05-11 (Wave 53 Phase 4 milestone audit — UI /128 ✅ DONE-eligible):** Bucket A static-analysis audit (PR #1106) confirmed avg 116.4/128 (range 114-118) — ALL screens ≥105/128 baseline. Per Wave 53 plan §7 + `gap-done-discipline.md` §2: UI-dimension AC verified; gap stays 🟡 PARTIAL pending remaining deferred sub-gaps (Lighthouse PWA / E2E spec / etc. tracked in their own follow-up gaps). When those close, this gap eligible PARTIAL → DONE flip via cascade.
