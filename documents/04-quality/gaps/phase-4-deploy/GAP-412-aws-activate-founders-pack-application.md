# GAP-412: AWS Activate Founders Pack Application ($1k credit)

**Status:** 🟡 PARTIAL — **RESUBMITTED 2026-05-11 01:19 ICT** sau khi GAP-459 root-cause fix shipped (PR #1086). Pending approval D+7-10 BD (~2026-05-21). Calendar reminder D+14 (2026-05-25) set via Google Calendar MCP. Resubmission log: `documents/05-guides/deploy/aws-activate-confirmation/2026-05-11-resubmission.md`. Original 2026-05-09 submission denied 2026-05-10 ("website cannot be accessed") — root cause GAP-459 fixed.
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Infrastructure / Cost / Business
**Found:** 2026-05-07 (Wave 37 — Layer 5)
**Affects:** Phase 1 BETA + Phase 1.5 PAID early effective $0 cost

## Problem

Architecture B = $72/mo Yr1, $89 Yr2+. Solo dev pre-revenue → cash burn ~$216 Phase 1 BETA (3mo). AWS Activate Founders Pack offers $1,000 credit cho startup self-applied → cover 13.9 tháng Phase 1.

## Proposed Fix

Apply AWS Activate Founders Pack:
1. Eligibility: pre-revenue startup, có pitch deck (ngay cả 1-page concept), không yêu cầu investor
2. URL: https://aws.amazon.com/activate/founders-pack
3. Submit: company name (KiteClass), description 100-200 từ, AWS account ID, contact email
4. Approval: typical 1-2 tuần
5. Credit applied: dashboard `aws.amazon.com/billing/home#/credits`

Optional enhancement: với investor verified → $5,000 credit (69 tháng).

Document credit usage policy:
- Reserve credit cho production EC2 (highest cost item)
- KHÔNG dùng cho experimental/learning
- Set up AWS Budgets alarm (GAP-413) khi credit <20% remaining

## Acceptance Criteria

- [x] Application submitted (screenshot confirmation) — **submitted 2026-05-09 ~17:19 ICT** (form qua https://aws.amazon.com/startups/credits/ → Activate Founder $1k tier; calendar reminder D+14 set qua MCP cho approval check 2026-05-23)
- [x] Pitch deck draft `documents/00-brd/kite-pitch-deck.md` (1-page minimum)
- [ ] Credit approval received → balance reflected billing dashboard — **🔴 DENIED 2026-05-10 — resubmit gated trên GAP-459**
- [x] Document `documents/05-guides/deploy/aws-activate-credit-policy.md` (usage rules)
- [ ] Budget alarm wired (GAP-413 dependency) — **tracked separately GAP-413 + GAP-395 Terraform**
- [ ] **GAP-459 fix shipped + production verify** (canonical .vn→.me + LandingShellSSR) — **resubmit prerequisite**
- [ ] **Resubmit Activate** sau GAP-459 ship — same form data + updated submission log

## Related

- GAP-411 (sizing matrix — credit cover Phase 1+1.5 early)
- GAP-413 (AWS Budgets alarm for credit depletion)
- GAP-381 (agent deploy role boundaries — human submits Activate)
- GAP-394 (account prep — provides AWS account ID)
- ADR-025 AWS Singapore decision
- **GAP-459** (Activate denial fix — SSR bailout + canonical URL; resubmit gate)
- **GAP-458** (kitehub.me domain — established `.me` choice making canonical URL drift surface)
- PR #1085 (Tier 3 cutover automation — built parallel với GAP-459 deferred work)

## Log

- **2026-05-07** — PARTIAL. Pitch deck + credit allocation policy shipped (Wave 37 Bucket E). Application submission + approval = human action post-deploy per GAP-381 Phase 2 BANNED-for-agent. Follow-up: track submission + approval evidence in next monthly cost report (per `documents/04-quality/cost-reports/2026-06-template.md`).
- **2026-05-09 (submission)** — Submitted Activate Founder $1k tier qua https://aws.amazon.com/startups/credits/ (rebrand từ /activate/founders-pack/). Form fields: Company KiteClass, Country VN, Industry Education, AWS account 906286017800, Founded 2026-01-01, Stage "Ideating/building initial product", Customer type Businesses (B2B), AI/ML Yes, % AWS spend 75-99%, No Funding, Marketing <$250k, Planned launch 2026-07. Pitch deck PDF (3 pages, 95KB) attached qua md-to-pdf render. Calendar reminder D+14 (2026-05-23 10:00 ICT) set qua Google Calendar MCP cho approval status check. AC #1 flipped checked. AC #3 (approval) chờ AWS review 1-2 tuần.
- **2026-05-10 (DENIED)** — AWS Activate Team email: "We are unable to process your application as it does not meet the internal requirements... Your website cannot be accessed or fails to load. Please resolve and resubmit." Curl audit kitehub.me confirmed HTTP 200 nhưng 2 root causes khiến bot/headless reviewer thấy "fails to load": (1) **SSR bailout** — `LandingShell.tsx:14-21` `next/dynamic({ssr:false})` cho framer-motion code-split (GAP-127 trade-off), bot không-JS chỉ thấy fallback `<div>Đang tải trang chủ…</div>`; (2) **Canonical URL trỏ kitehub.vn** — 16 hardcoded refs (`app/layout.tsx`/`sitemap.ts`/`robots.ts`/`schemas.ts`/`PublicLayout.tsx`/blog/pricing pages/test), domain `.vn` KHÔNG tồn tại (Release 1 dùng `.me` per GAP-458) → reviewer follow `<link rel=canonical>` → DNS NXDOMAIN. **GAP-459 filed** (`activate-resubmit-prep`) với Phase 1 (`.vn`→`.me`) + Phase 2 (LandingShellSSR server component preserve framer-motion code-split nhưng render top-fold static cho bot/SEO) + Phase 3 tests + Phase 4 resubmit. User pivot 2026-05-10 ưu tiên Tier 3 cutover automation (PR #1085) trước resubmit — 2-tuần review wait absorb được trong Phase 1 BETA Week 9-12 timeline.
