# Kite Design System — Context Dossier for Claude Design

**Purpose:** Context bundle for handing back to Claude Design (claude.ai/design) so Round 2+ produces designs aligned with real project requirements — not just CSS/TSX surface mining.

**Last Updated:** 2026-04-29

**Status:** Round 1 bundle (`/tmp/anthropic-design/kite-design-system/`) shipped foundation tokens + 6 UI kits but missed personas, business rules, VN-specific UX, screen inventory, and quality bar. This dossier closes that gap.

---

## How to use

1. **Upload all 10 dossier files + `prompts.md` to Claude Design as context** (single batch — Claude Design will navigate them as needed).
2. **Paste the Round 2 opening prompt** from `prompts.md`.
3. Claude Design produces Round 2 deliverables (kiteclass-teacher / kiteclass-parent / ai-branding-wizard-v2 / component spec set / mobile-tech-decision).
4. Review. If quality bar met (per `06-quality-bar.md` + `10-acceptance-criteria.md`), port to production codebase via Track 2 (separate gap files filed after scope is known).
5. If not met, paste targeted Round 3+ prompts from `prompts.md`.

---

## Index

| # | File | Purpose | Source |
|:-:|------|---------|--------|
| — | `README.md` | This file — entry point + index | hand-written |
| 1 | `01-personas.md` | 5 BRD personas (Solo Teacher / Center Owner / Medium Center Admin / K–12 Principal / Student / Parent) — JTBD, pain points, devices, tech literacy, usage time | `documents/00-brd/personas-catalog.md` v1 + use-cases mining |
| 2 | `02-vietnamese-ux-musts.md` | Vietnam-specific UX patterns: currency, date, phone, address, attendance code, grade scale, học bạ format, MoMo/VNPay/ZaloPay buttons, Zalo OA cards, SMS OTP, CCCD/CMND | `documents/01-business/*/rules.md` + GAP-055 |
| 3 | `03-screen-inventory.md` | 63-route inventory (39 KC + 24 KH) with priority, current UI score `/128`, mock-data status, business rule reference | UI audit baseline 2026-04-19 |
| 4 | `04-component-gaps.md` | 12 components missing from Round 1 bundle but needed in production: bulk-import, gradebook entry, schedule manager, payment-method-selector, parent invite, attendance calendar, instance lifecycle, theme panel | code grep + GAP-137/139/056/009 |
| 5 | `05-business-flows.md` | 10 critical user flows with actor / trigger / steps / success / failure / duration: tenant signup, bulk import, attendance daily, grade entry, payment, parent view, trial → upgrade, AI rebrand, enrollment, K–12 sync | `01-business/*/use-cases.md` |
| 6 | `06-quality-bar.md` | UI audit `/128` rubric (4 dim × 4 = 16 per screen) + WCAG AA contrast 4.5:1 + perf budget (First Load JS <250KB) + dark mode mandatory + i18n bilingual | `.claude/skills/quality/ui-review/SKILL.md` + `meta-gap-priority.md` |
| 7 | `07-existing-pain-points.md` | Top 10 lowest-scoring screens from current audit + specific issues: KH branding/billing 33/128, KC settings 74/128, parent dashboard MVP only, blog 404 English fallback | `documents/04-quality/audits/ui-review/2026-04-19/` |
| 8 | `08-direction-decisions.md` | 4 hard decisions: (a) Direction B priority, (b) Direction D = web responsive (not native app), (c) Direction A = marketing track, (d) Direction C = integrate into wizard 6-step | session 2026-04-29 |
| 9 | `09-tech-constraints.md` | Stack lock: Next.js 15.5, React 19.2, Tailwind 3.4, shadcn/ui, Radix UI, Framer Motion (KH only), TanStack Query, MSW v2 (KC test), pnpm, Vitest, Playwright | `package.json` of both frontends |
| 10 | `10-acceptance-criteria.md` | Per-screen AC: before/after screenshot, score `/128` ≥ 100, WCAG AA, mobile 320/768/1440 viewports, dark mode, loading/empty/error states, VN copy, no Lorem ipsum | `ui-review/SKILL.md` |
| — | `prompts.md` | Prompt library for Claude Design: opening Round 2 prompt + 4 direction deep-dive prompts (B / C / D pivoted / A) + per-deliverable acceptance prompt | hand-written |

---

## Glossary (for Claude Design context)

- **KiteHub** = SaaS control plane (port 4701, sky blue + orange) — used by center owners to manage their tenant instance, billing, AI branding.
- **KiteClass** = multi-tenant education app (port 4700, blue base, per-tenant theme overlay) — used by teachers, students, parents.
- **Tenant** = a trung tâm (education center / tutoring business / K-12 school) — gets own subdomain + theme.
- **AI Branding** = KiteHub feature that auto-generates a tenant's theme (colors, logo, banners) via 6-step wizard.
- **Provisioning** = lifecycle from `NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED` (with `FAILED` and `REGENERATING` branches).
- **Wave** = a parallel-agent shipping pattern; not relevant to design but mentioned in some references.
- **persona codes:** P1 Solo Teacher, P2 Center Owner, P3 Medium Center Admin, P5 K–12 Principal (P4 was dropped). Plus Student + Parent as secondary.
- **VN MoET** = Ministry of Education and Training (Vietnam) — sets official report card format, attendance code requirements, grade scale 0-10.

---

## Out of scope for this dossier

- Production code migration plan — that's Track 2, gap files filed separately after Round 2 deliverables exist.
- Backend API changes — design produces FE specs; BE follows.
- Mobile native app design — pivoted to **web responsive + PWA-grade** per `08-direction-decisions.md`. Native app deferred until post-PMF.

---

## Maintenance

When persona BRD evolves, screen inventory shifts, or quality bar tightens, update the relevant file + bump dossier version line at top. Re-upload to Claude Design before next round.
