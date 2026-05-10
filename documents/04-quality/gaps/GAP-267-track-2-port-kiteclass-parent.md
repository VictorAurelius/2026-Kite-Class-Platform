# GAP-267: Track 2 Port — kiteclass-parent → production Next.js (mobile PWA)

**Status:** 🟡 PARTIAL — Wave 49 Bucket A shipped 2026-05-10 (mobile shell + 8 routes + 4 components ported); deferred items per §"Deferred (follow-up gap pending)" below
**Priority:** 🟡 P2 (UX growth — Pa. Parent persona, mobile-first 85% sessions)
**Domain:** Frontend
**Found:** 2026-04-29 (post-Round 3 user accept)
**Affects:** `kiteclass-frontend/src/app/(dashboard)/parent/` — parent portal routes

## Problem

HTML prototype `kiteclass-parent/` (avg 114/128 ⭐, 17 screens, R2 PR #670, Direction D pivot to web responsive PWA-grade NOT native) is highest-scoring R2 kit. Production parent route exists but reflects pre-R2 design (~73/128).

## Current State (verified 2026-04-29)

`kiteclass-frontend/src/app/(dashboard)/parent/` exists. Mobile responsiveness + PWA features (manifest, service worker, Web Push, Zalo OA card) NOT verified — likely missing.

## Proposed Fix

Port 17 mobile-first parent screens + add PWA infrastructure.

**Scope:**
- Parent home (children switcher + today + pending)
- Child detail (schedule + grades + attendance + payments)
- Notifications inbox (Zalo OA mirror)
- Payments (balance + history + pay action)
- Empty states (no children invited yet)
- Login + parent invite redemption flow
- PWA: `manifest.json` + service worker + Web Push permission UI
- Zalo OA primary push card (vs Web Push fallback)

**Tech direction:**
- Bottom tab nav (4 tabs: Home, Children, Payments, Profile)
- Mobile-first 320–414px primary, scale to tablet/desktop secondary
- Reuse component G7 (parent-invite redemption flow) from production component lib (port via GAP-273)

## Acceptance Criteria

- [x] **Mobile shell + bottom 4-tab nav** — 17 screens consolidated into 8 production routes (home / billing / billing/[id]/pay / billing/[id]/success / grades / grades/[subject] / attendance / attendance/[date] / settings) sharing `ParentShell` per persona-evening warm-blue theme. Tap targets ≥44px (`min-height: 44px` on `<Link>` tabs). All 17 prototype state variations (default + dark + empty + error + loading) covered through React state + theme tokens (dark via system `prefers-color-scheme`)
- [x] **PWA: `manifest.json` + service worker registered** — Wave 49 Bucket 0 (PR #1090) shipped infra; `src/app/layout.tsx` registers
- [x] **Web Push permission UI works (subscribe/unsubscribe round-trip)** — `PushNotificationCard` + `WebPushPermission` use `subscribe()` / `unsubscribe()` from `@/lib/web-push` (Bucket 0 MSW stub for `/api/push/subscribe`); UI handles 6 outcomes (subscribed / unsubscribed / denied / unsupported / not-configured / error)
- [x] **Zalo OA primary card visible above Web Push fallback** — `PushNotificationCard` renders Zalo OA card with `border-2 border-sky-500` + `CHÍNH` badge ABOVE Web Push card; uses Zalo brand `#0068FF`
- [x] **Bottom tab nav with 44px+ tap targets** — verified in `parent-shell.tsx` with `min-h-11` + `flex flex-col items-center justify-center`
- [x] **Vietnamese-only content, realistic VN data** — all UI strings VN; mock data uses `Lê Minh Tuấn`, `Lớp 10A2`, `Trường THCS-THPT EduPlus`, VN currency `4.500.000đ`, VN dates `dd/mm/yyyy`
- [x] **WCAG AA preserved** — Tailwind theme tokens ensure ≥4.5:1; status colors use brand-safe palette
- [x] **Wave 18b1 logic preserved** — `useMyChildren` + `useParentMe` hooks reused unchanged; `parent/transcript/[childId]` route untouched; both-link from new home page + grades tab
- [ ] **Lighthouse PWA score ≥90** — DEFERRED to follow-up; requires deploy/local-dev with HTTPS to measure (cannot run inside this PR's verify scope)
- [ ] **Parent invite redemption end-to-end (token → child binding)** — PARTIAL: existing `/parent-invite/[token]` flow (Wave 2) remains the redeem surface; G7 ParentInvite is sender-variant per spec.md (redeem-page surface tracked in GAP-273 follow-up)
- [ ] **E2E test (invite redemption → child detail → pay tuition)** — DEFERRED: Playwright spec not added in this PR; logical flow exists (parent-invite → home → child card → transcript / billing → pay → success). File follow-up sub-gap GAP-267a for Playwright spec

## Deferred (follow-up gap pending)

- **GAP-267a (planned)**: Playwright E2E spec covering the full parent-invite → child binding → pay tuition flow + Lighthouse PWA ≥90 measurement on staging
- **GAP-267b (planned)**: G7 ParentInvite redeem-variant (per `dossier/04-component-gaps.md` §G7 Phase 2 carve-out — see GAP-273 follow-up)
- **Backend wiring**: real `/api/push/subscribe` controller (currently MSW-mocked per Wave 49 R5). Tracked separately when push provider provisioned

## Related

- HTML prototype: `ui_kits/kiteclass-parent/`
- Component G7 (parent invite): GAP-273 batch
- Sister Track 2 gaps: GAP-266 (owner), GAP-268 (teacher), GAP-269 (student)

## Effort estimate

~2 weeks. PWA work + mobile responsiveness adds vs Track 2 baseline. Wave-pack candidate when sliced into auth+invite / home+navigation / detail-screens / PWA-infrastructure.

## Log

- **2026-05-10 (Wave 49 Bucket A — PARTIAL)**: Production Next.js port shipped. Created `kiteclass-frontend/src/components/parent/` (parent-shell + hero-metric + child-card + activity-row + push-notification-card + pwa-install-prompt + web-push-permission + parent-mock-data) + 8 routes under `(dashboard)/parent/{home,billing[+pay/success],grades[+subject],attendance[+date],settings}` consolidating the 17 prototype HTML screens. Wave 18b1 hook `useMyChildren` + transcript route preserved; Bucket 0 PWA infra (PR #1090) consumed via `subscribe()/unsubscribe()` calls; Zalo OA card primary above Web Push fallback. Added 3 unit tests + 1 mock-data contract test (parent-shell + child-card + parent-mock-data); 711/711 existing tests still green; build succeeds; lint clean for new files. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp because (1) Lighthouse PWA score ≥90 measurement requires HTTPS deploy outside PR scope, (2) Playwright E2E spec deferred to GAP-267a follow-up, (3) G7 ParentInvite redeem-variant out of scope (sender-only per spec.md). PR: wave/49-bucket-a-kc-parent.
- **2026-04-29:** Filed after user accepted Round 3 quality.

- **2026-05-11 (Wave 53 Phase 4 milestone audit — UI /128 ✅ DONE-eligible):** Bucket A static-analysis audit (PR #1106) confirmed avg 114.4/128 (range 108-121) — ALL screens ≥105/128 baseline. Per Wave 53 plan §7 + `gap-done-discipline.md` §2: UI-dimension AC verified; gap stays 🟡 PARTIAL pending remaining deferred sub-gaps (Lighthouse PWA / E2E spec / etc. tracked in their own follow-up gaps). When those close, this gap eligible PARTIAL → DONE flip via cascade.
