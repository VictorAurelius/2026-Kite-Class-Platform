---
title: AWS Activate Founder Application — Resubmission Log
date: 2026-05-11
time: 01:19 ICT (UTC+7)
status: resubmitted, pending approval
gap: GAP-412 (parent) + GAP-459 (Phase 4 closure) + GAP-460 (brand pivot context preserved)
supersedes: 2026-05-09-submission.md (denied 2026-05-10)
---

# AWS Activate Founder — Resubmission 2026-05-11

## Trigger

Original submission 2026-05-09 17:19 ICT denied 2026-05-10 với reason "Your website cannot be accessed or fails to load". Root causes identified + fixed:
1. **SSR bailout** — `LandingShell.tsx` `next/dynamic({ ssr: false })` → bot không-JS thấy `<div>Đang tải trang chủ…</div>` fallback
2. **Canonical URL** trỏ `kitehub.vn` (DNS NXDOMAIN) — 21 hardcoded `.vn` references trong codebase

GAP-459 Phase 1+2+3 shipped (PR #1086 d64d4f7c, 2026-05-10):
- ✅ Centralize SITE_URL constant `kitehub-frontend/src/lib/site-config.ts` → 21 refs swapped `.vn` → `.me`
- ✅ `LandingShellSSR.tsx` server component renders SSR hero + CTA + value prop + nav + footer (real content visible without JS)
- ✅ `pnpm test --run` 649/649 pass; `pnpm build` succeeds; built HTML 22KB với hero copy + 0 `.vn` refs + 0 "Đang tải" spinner
- ✅ Production deploy verified `https://kitehub.me` accessible với hero content

## Submission details

| Field | Value |
|---|---|
| **Tier** | Activate Founder ($1,000 USD credits) — same tier as original |
| **URL used** | https://aws.amazon.com/startups/credits/ (resubmit flow per AWS dashboard) |
| **AWS account ID** | 906286017800 |
| **Resubmission timestamp** | 2026-05-11 01:19 ICT (UTC+7) |
| **Approval lead time** | 7-10 business days typical (per AWS Activate official guide 2026) |
| **Calendar reminder** | 2026-05-25 (D+14, set via Google Calendar MCP) |

## Form fields submitted (carried forward from 2026-05-09)

Same form values as original submission, ngoại trừ description text refreshed nhấn fix:

| Field | Value |
|---|---|
| Company name | KiteClass |
| City | Ha Noi |
| Country | VN |
| Industry | Education |
| Founded date | 2026-01-01 |
| Number of employees | 1 - 10 |
| Founder | Yes |
| Job title | Software Engineer |
| Customer type | Education (Ed Tech / EdTech) |
| Primary target customers | Businesses (B2B) |
| Product stage | Ideating/building initial product |
| Customer interaction | Web browser |
| AI/ML usage | Yes |
| Funding round | No Funding |
| Valuation | No Valuation |
| % AWS spend | 75 - 99% |
| Annual marketing spend | < $250,000 |
| Planned product launch | 2026-07-01 |
| Most recent funding date | (blank — bootstrapped) |
| AWS years | 0 - 2 yrs |
| AWS top-3 ways helps | Building scalable architecture / Reduce infrastructure costs / Validating product concept |
| **Website** | **https://kitehub.me** (verified accessible 2026-05-10 post-GAP-459 deploy) |
| LinkedIn | https://www.linkedin.com/in/victoraurelius/ |

## Description text submitted (refreshed, 247 chars, ≤250 budget)

> Vietnamese-first multi-tenant SaaS for ~30k VN education centers — students, attendance, grades, parents, payments. PDPL-compliant, MoET-aware. Replaces Excel/Zalo. Live at kitehub.me. Solo dev, pre-revenue. AWS Singapore Phase 1 BETA May 2026.

**Δ vs 2026-05-09:** Added "**Live at kitehub.me**" — direct counter denial reason "website cannot be accessed". Compressed "30,000" → "30k" + "managing" → em-dash + drop "launch" để fit 250 char budget.

## Pitch deck attached

- Source: `documents/00-brd/kite-pitch-deck.md` (unchanged from 2026-05-09)
- Rendered PDF: `kite-pitch-deck.pdf` (95KB, 3 pages, A4)
- Render command: `npx -y md-to-pdf <source.md> --pdf-options '{"format":"A4","margin":{"top":"20mm","bottom":"20mm","left":"20mm","right":"20mm"}}' --launch-options '{"args":["--no-sandbox"]}'`

## Next actions

1. **D+14 (2026-05-25)** — Calendar reminder fires; check approval status
2. **If approved:**
   - Screenshot https://console.aws.amazon.com/billing/home#/credits
   - Save as `2026-MM-DD-approval.png` cùng folder này
   - Flip GAP-412 status PARTIAL → 🟢 DONE
   - Resume EC2 + RDS từ cost-save (Tier 3 cutover unblocked per `release-1-tier-3-cutover.md`)
3. **If denied lần 2:**
   - Read denial reason
   - **If same "website" reason** → escalate AWS Support contact form (per `2026-05-09-submission.md` §"If no response D+21" precedent)
   - **If new reason** → evaluate per-reason fix
   - **Last resort** → Path C cost reduction (per `aws-activate-credit-policy.md` §4.3) OR personal-fund $293 buffer per §4.5
4. **If no response D+21 (2026-06-01):**
   - Escalate qua AWS Support contact form

## Compliance check

- ✅ `aws-activate-credit-policy.md` §2 Application Checklist met (account active + pitch deck + AWS account ID)
- ✅ `agent-action-bias.md` §3 row 2 — agent helped với drafting/PDF rendering, user submitted (form cần GitHub OAuth + identity verify)
- ✅ `gap-done-discipline.md` §3 PARTIAL exit ramp — resubmission shipped, approval pending → status stays PARTIAL với explicit pending criterion
- ✅ GAP-459 root-cause fixes verified production via `curl https://kitehub.me` (Phase 4 deploy verify implicit)

## References

- `documents/05-guides/deploy/aws-activate-confirmation/2026-05-09-submission.md` — original submission log (superseded by this file)
- `documents/04-quality/gaps/GAP-412-aws-activate-founders-pack-application.md` — parent gap (status update needed)
- `documents/04-quality/gaps/closed/GAP-459-activate-resubmit-prep.md` — Phase 4 user-action partial-DONE
- `documents/04-quality/audits/aws-verification/2026-05-11-actual-cost-vs-estimate.md` — cash burn analysis ($293 personal-fund risk if denied)
- `documents/05-guides/deploy/aws-activate-credit-policy.md` v1.1 — updated 2026-05-11 với actual numbers
