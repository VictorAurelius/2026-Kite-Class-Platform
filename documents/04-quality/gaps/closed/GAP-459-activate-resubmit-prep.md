# GAP-459: AWS Activate denial — fix kitehub.me SSR bailout + canonical URL `.vn` → `.me`

**Status:** 🟢 DONE 2026-05-11 — Phase 1+2+3 shipped 2026-05-10 (PR #1086) + Phase 4 user-action complete: production verified accessible 2026-05-10 + AWS Activate resubmitted 2026-05-11 01:19 ICT (`documents/05-guides/deploy/aws-activate-confirmation/2026-05-11-resubmission.md`) + Calendar reminder D+14 (2026-05-25) set; gap closes regardless of approval outcome (resubmission action complete; approval = parent GAP-412 dependency)
**Priority:** 🟠 P1 — block AWS Activate $1k credit (compute cover Phase 1 BETA ~10 tháng)
**Domain:** Frontend (kitehub-frontend) / SEO / DevOps
**Found:** 2026-05-10 (AWS Activate denial email — "Your website cannot be accessed or fails to load")
**Affects:** `kitehub-frontend/src/app/layout.tsx`, `app/sitemap.ts`, `app/robots.ts`, `app/(public)/{LandingShell,LandingClient,pricing/page,blog/page,blog/[slug]/page}.tsx`, `components/seo/schemas.ts`, `components/layout/PublicLayout.tsx`, AWS Activate resubmit

---

## Problem

AWS Activate Founder application (submitted 2026-05-09 17:19 ICT, GAP-412) **DENIED** với reason:
> Your website cannot be accessed or fails to load. Please resolve and resubmit your application.

Curl `https://kitehub.me` returns HTTP 200 + Next.js HTML, NHƯNG có 2 issues khiến reviewer bot/headless thấy "fails to load":

### Root cause 1 — SSR bailout, JS-only content

`kitehub-frontend/src/app/(public)/LandingShell.tsx:14-21` dùng `next/dynamic({ ssr: false })` để code-split framer-motion (GAP-127 Wave 7-Perf). Tradeoff:
- Bot/reviewer KHÔNG execute JS chỉ thấy fallback `<div>Đang tải trang chủ…</div>`
- Activate review tool có thể là headless không-JS hoặc rate-limit JS execution → resolve → "site fails to load"

HTML response chứa marker:
```html
<template data-dgst="BAILOUT_TO_CLIENT_SIDE_RENDERING"></template>
<div class="min-h-screen flex items-center justify-center bg-background">
  <div class="text-muted-foreground text-sm">Đang tải trang chủ…</div>
</div>
```

### Root cause 2 — Canonical URL trỏ domain KHÔNG tồn tại

16 hardcoded `kitehub.vn` references trong codebase:
```
app/layout.tsx:15,22,27               metadataBase + canonical + og:url
app/sitemap.ts:8,15,16,17,19,20       6 sitemap URLs
app/robots.ts:10                       sitemap URL
app/(public)/pricing/page.tsx:12,18    canonical + og:url
app/(public)/blog/page.tsx:14          og:url
app/(public)/blog/[slug]/page.tsx:30,35  canonical + og:url
app/(public)/LandingClient.tsx:430     JSON-LD url
components/seo/schemas.ts:11,45        SITE_URL + email
components/layout/PublicLayout.tsx:81  footer email
components/layout/__tests__/PublicLayout.test.tsx:95  test assertion
```

Reviewer follow `<link rel="canonical" href="https://kitehub.vn"/>` → DNS NXDOMAIN → "site fails to load".

JSON-LD Organization + WebSite schemas cũng trỏ `kitehub.vn` (5 chỗ trong schemas.ts + LandingClient.tsx) — bot crawler hit dead link.

## Background

Per GAP-458 decision 2026-05-09, domain Release 1 = `kitehub.me` (Free path GitHub Student Pack). Codebase được scaffold từ pre-decision era khi default assumed `.vn`. Domain decision update các runbook + docs nhưng FE code-level URL refs chưa sync.

## Proposed Fix

### Phase 1 — Canonical URL `.vn` → `.me` (~30-60 phút)

Centralize SITE_URL constant trong `components/seo/schemas.ts:11` → all imports use `SITE_URL`. Specifically:

| File | Edit |
|---|---|
| `components/seo/schemas.ts:11` | `const SITE_URL = 'https://kitehub.me'` (was `.vn`) |
| `components/seo/schemas.ts:45` | `email: 'support@kitehub.me'` |
| `app/layout.tsx:15,22,27` | Import + use `SITE_URL` (3 refs) |
| `app/sitemap.ts:8,15-20` | Import + use `SITE_URL` (6 refs) |
| `app/robots.ts:10` | Import + use `SITE_URL` |
| `app/(public)/pricing/page.tsx:12,18` | Import + use `SITE_URL` |
| `app/(public)/blog/page.tsx:14` | Import + use `SITE_URL` |
| `app/(public)/blog/[slug]/page.tsx:30,35` | Import + use `SITE_URL` |
| `app/(public)/LandingClient.tsx:430` | Hardcode `'https://kitehub.me'` (client comp avoid SSR-only import) |
| `components/layout/PublicLayout.tsx:81` | `support@kitehub.me` |
| `components/layout/__tests__/PublicLayout.test.tsx:95` | Test assertion `kitehub.me` |

### Phase 2 — SSR bailout fix (~1-2h)

Tạo `LandingShellSSR.tsx` server component render top-fold static (hero + CTA + tagline + value prop bullets) BÊN CẠNH lazy `LandingClient`:

```tsx
// page.tsx (server component)
import LandingShellSSR from './LandingShellSSR';
import LandingShell from './LandingShell';

export default function HomePage() {
  return (
    <>
      <JsonLd ... />
      {/* SSR hero — bot/reviewer sees real content */}
      <LandingShellSSR />
      {/* Below-fold animations lazy load */}
      <LandingShell />
    </>
  );
}
```

`LandingShellSSR.tsx` renders:
- `<h1>` với product tagline (Vietnamese)
- Hero CTA buttons (`Đăng ký dùng thử` + `Xem bảng giá`)
- 3-bullet value prop static text
- Footer-style links (pricing, blog, login, register)

→ Bot không-JS thấy real content. User vẫn được first-paint instant + animations hydrate sau qua `LandingClient`.

### Phase 3 — Tests + verify (~30 phút)

```bash
cd kitehub/kitehub-frontend
pnpm test --run                         # PublicLayout test cập nhật .me
pnpm build                              # Verify SSR works
pnpm dev &
curl -s http://localhost:3001 | grep -E "Đang tải|kitehub\.vn"
# Expected: 0 matches (no bailout text, no .vn refs)
```

### Phase 4 — Deploy + resubmit Activate (~30 phút)

1. Merge PR → Vercel auto-deploy ~3 phút
2. Verify production:
   ```bash
   curl -s https://kitehub.me | grep -cE "Đang tải|kitehub\.vn"
   # Expected: 0
   ```
3. Resubmit Activate `https://aws.amazon.com/startups/credits/`
4. Update Calendar reminder (2 tuần wait)

## Acceptance Criteria

- [x] Phase 1: 21 `.vn` refs → `.me` via centralized `lib/site-config.ts` SITE_URL constant (+5 vs gap-doc estimate of 16 — added DPO email in `legal/data-rights/page.tsx` + LandingClient JSON-LD + 2 layout og:url variants caught by grep sweep)
- [x] Phase 2: `LandingShellSSR.tsx` ships SSR hero + CTA + value prop + nav + footer; wired into `LandingShell` `loading` prop so initial HTML contains real content (verified via `.next/server/app/index.html` — hero in body not template)
- [x] Phase 3: `pnpm test --run` 649/649 pass; `pnpm build` succeeds; built HTML 22KB with hero copy + 0 `.vn` refs + 0 "Đang tải" spinner text
- [x] Phase 4: Production verify `kitehub.me` shows hero content via curl (no "Đang tải"); zero `.vn` refs in HTML — **verified 2026-05-10 post-PR-#1086 deploy**
- [x] Resubmit AWS Activate với same form data + updated submission log — **2026-05-11 01:19 ICT, log `documents/05-guides/deploy/aws-activate-confirmation/2026-05-11-resubmission.md`**
- [x] Calendar reminder 2 tuần wait (D+14 từ resubmit date) — **2026-05-25 set via Google Calendar MCP**

## Compliance

- ✅ CLAUDE.md tiếng Việt mandate — fix preserves Vietnamese content
- ✅ `release-deploy-standard.md` §3.1 — pre-release artifact (frontend SEO)
- ✅ `agent-action-bias.md` — fix là agent-do (FE code edit), resubmit là user-action (form submit)
- ⚠️ GAP-127 Wave 7-Perf trade-off — code-splitting framer-motion preserved (LandingShell `ssr: false` retained); SSR shell mới chỉ render top-fold static (~5KB additional bundle) — KHÔNG ship framer-motion eagerly

## Related

- Parent: GAP-412 (AWS Activate Founder application — submitted 2026-05-09 → DENIED 2026-05-10)
- Sibling: GAP-458 (kitehub.me domain decision — established `.me` choice)
- GAP-127 (Wave 7-Perf code-splitting LandingShell — pattern preserved)
- `documents/05-guides/deploy/aws-activate-confirmation/2026-05-09-submission.md` — original submission log
- `documents/05-guides/deploy/release-1-tier-3-cutover.md` — Tier 3 block trên Activate approval (cost cover)
- ROADMAP §🚀 row 6 — domain Tier 1/2/3 + Activate dependency

## Log

- **2026-05-10** Filed in response to AWS Activate denial email. Curl audit confirmed 2 root causes: SSR bailout (JS-only fallback "Đang tải trang chủ…") + canonical URL trỏ `.vn` (DNS NXDOMAIN). User chose Recommended fix path (full session ship). Fix branch `fix/activate-resubmit-prep-gap-459`.
- **2026-05-10 (later)** Phases 1+2+3 shipped single-PR (wave-pack analysis: 2 disjoint code buckets < 3 threshold → serial single-PR per `feedback_wave_plan_before_serial_prs.md`). Implementation: created `kitehub-frontend/src/lib/site-config.ts` exporting `SITE_URL`/`SUPPORT_EMAIL`/`DPO_EMAIL`; centralized 21 `.vn` refs across 9 files via TypeScript imports; created `LandingShellSSR.tsx` server-friendly above-fold component (hero + 2 CTAs + 3 value props + nav + footer); wired it into `LandingShell` `loading` prop so Next.js renders real content in initial HTML for `ssr: false` dynamic import. Verify: `pnpm test --run` 649/649 pass, `pnpm build` succeeds, built `index.html` contains hero copy in body (not template) + 0 `.vn` refs + 0 "Đang tải" spinner. BAILOUT marker still emitted by Next.js as empty `<template>` element (expected — doesn't hide content). Phase 4 (production curl verify + Activate resubmit + calendar reminder) deferred to post-merge user-action per `gap-done-discipline.md` §3 PARTIAL exit ramp.
- **2026-05-11 — Phase 4 user-action complete + gap closure**: User confirmed resubmit AWS Activate Founder $1k tier 01:19 ICT từ rejected dashboard (Path 1 standard re-submission flow worked; KHÔNG cần Path 2 Contact us escalation). Form values carried forward từ 2026-05-09 submission; description text refreshed nhấn "Live at kitehub.me" (247 chars, ≤250 budget) để counter denial reason. Pitch deck attached unchanged. Calendar reminder 2026-05-25 set via Google Calendar MCP per `agent-action-bias.md` §3 (agent-do tooling actions). Status flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 — all 6 AC checked. Approval outcome = GAP-412 parent dependency; this gap (resubmission prep work) is complete regardless of approval verdict. Per `gap-done-discipline.md` §3 framing: "DONE = work complete," not "outcome favorable" — GAP-412 tracks outcome separately.
