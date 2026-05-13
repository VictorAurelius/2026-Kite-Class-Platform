# GAP-497: AWS credits strategy post Activate Founders rejection ×2

**Status:** 🔵 OPEN (design / decision needed)
**Priority:** 🟡 P2 (cost optimization; not blocking Release 1 — Free Tier covers $0 net)
**Domain:** Business / DevOps / Cost
**Found:** 2026-05-12 (2nd AWS Activate Founders rejection email)
**Affects:** Phase 1 BETA + Phase 1.5 cost runway

## Problem

Two AWS Activate Founders rejections received:

1. **2026-05-10:** First rejection — "Your website cannot be accessed or fails to load" (root cause: stale `kitehub.vn` refs in FE before `kitehub.me` cutover). Fixed via GAP-459.
2. **2026-05-12:** Second rejection — generic "did not meet internal requirements." 3 criteria checked: (a) valid billing/account, (b) consistent business information, (c) connection to accounts linked to misuse.

Email không nói criteria nào cụ thể fail. Analysis dựa trên profile + 2026 startup credit guides:

| Criterion | Likely status | Risk factor |
|-----------|--------------|-------------|
| Valid billing/AWS account | ✅ OK | Account 906286017800 active, payment method set |
| Consistent business information | 🟡 Likely fail | Solo dev no legal entity (LLC/Pte Ltd); `.me` TLD (GitHub Student Pack) may be flagged as "personal"; domain only 3 days old at application time |
| Connection to accounts linked to misuse | 🟡 Possible | Personal Gmail used (per guide "never apply with personal Gmail"); previous AWS accounts under same email/IP/credit card |

Per 2026 AWS Activate Founders eligibility (cross-referenced multiple guides):
- Pre-series B startup
- Company website live + clearly articulates product
- Founded in last 10 years
- AWS account on **Paid Tier Plan** (NOT Free Tier) — likely the trigger
- Corporate email (NOT personal Gmail)
- Bootstrapped/unfunded OK for Founders tier ($1k credits)

## AWS Educate (researched — not viable for production)

AWS Educate is **learning platform**, NOT credit program:
- Free hands-on labs (Storage, Compute, Networking, Databases, Cloud Ops)
- Digital badges + entry-level job board
- Min age 13; 18+ for jobs
- NO compute/storage credits for production hosting
- NOT suitable for KiteHub MVP

→ AWS Educate is dead-end for cost strategy. Skip.

## Alternative credit programs (researched)

| Program | Credits | Eligibility | Notes for KiteHub |
|---------|---------|-------------|-------------------|
| **Microsoft for Startups Founders Hub** | $5k–$150k Azure | Self-attestation OK, no accelerator needed | Lower bar than AWS; can dual-cloud |
| **Google for Startups Cloud** | $2k–$100k | Stage-based tiers | Easier first-tier $2k |
| **AWS Activate Portfolio** | $5k–$100k | Through accelerator/VC (Y Combinator, Techstars, Antler VN, ThinkZone, Touchstone) | Solo dev → join accelerator first |
| **DigitalOcean Hatch** | $1k–$100k | Early-stage startup | Lower-spec but simpler |
| **GitHub Student Pack** | $200 DO + AWS Educate | Currently student | Already used for domain |
| **Vietnam-specific** | Varies | VietChallenge, Antler VN, ThinkZone, Touchstone Partners | Local network advantage |

## Reapply AWS Activate strategy (when ready)

Per email "rejected" — account may be flagged for ~6-12 month cooldown. Don't apply 3rd time same account immediately.

**Conditions to satisfy before reapply:**
1. Domain age >60 days (kitehub.me activated 2026-05-09 → eligible 2026-07-09+)
2. Website production-ready (Java services healthy, real landing page with product description, not "coming soon")
3. Legal entity registered (LLC, Pte Ltd, or VN business registration ~$200-1000 cost)
4. Corporate email (`founder@kitehub.me` via Cloudflare Email Routing → forward to personal)
5. AWS account moved to Paid Tier (post Free Tier exhaust 2027 OR explicit upgrade)
6. ≥5 active beta tenants (traction signal)
7. Apply via fresh AWS account if old account is flagged → use legal entity name

## Recommended path for KiteHub

**Phase 1 BETA (current — through ~Aug 2026):**
- **Free Tier covers $0 net payable** (verified Bills tab "Estimated grand total: $0.00")
- ALB $16/mo fixed but covered by Free Tier 12-month window
- Stop-when-idle pattern (Wave 61 design) keeps gross usage ~$5/mo
- **NO action needed on credits during Phase 1 BETA**

**Phase 1.5 PAID prep (~Sep-Oct 2026):**
- Apply Microsoft for Startups Founders Hub ($5k Azure) — lower bar — as backup
- Register VN business entity (Pte Ltd Singapore $500 OR VN LLC $200) → legal name on application
- Set up `founder@kitehub.me` corporate email
- Build traction page on `kitehub.me` (case studies + 5 beta tenant testimonials)

**Phase 2 PAID launch (~Q4 2026):**
- AWS Activate reapply via legal entity (fresh approach, new corporate email)
- If accelerator joined (Antler VN, ThinkZone) → Activate Portfolio $5k-$25k

**Phase 3 K-12 (post counsel + funding):**
- Activate Portfolio via VC/accelerator → $50k-$100k tier

## Acceptance Criteria (decision-only gap)

- [x] AWS Educate evaluated → NOT viable (learning-only)
- [x] Alternative programs researched (Microsoft, Google, accelerator portfolio)
- [x] Recommended phased strategy documented
- [ ] User decides: (A) Phase 1 BETA stay Free Tier no credits, (B) Apply Microsoft Founders Hub now as backup, (C) Both
- [ ] If (B) or (C): Apply Microsoft for Startups Founders Hub → track timeline
- [ ] If reapply AWS Activate (post legal entity registration): cooldown date noted, conditions list reviewed

## Out-of-scope

- Negotiating with AWS support for rejection appeal — solo dev no leverage; rejection email final
- Multi-cloud architecture (Azure + AWS) — Phase 2+ consideration
- Detailed cost projection beyond Free Tier exhaust 2027 — separate gap

## Related

- **Predecessor:** GAP-459 (kitehub.me canonical sweep — fixed 1st rejection cause)
- **Reference docs:**
  - `documents/03-planning/roadmap/release-1-plan-2026.md` (Phase progression)
  - `feedback_decision_doc_code_sync.md` memory (2026-05-09 incident)
- **Cost context:** Bills tab "Estimated grand total $0.00" verified Wave 64 session 2026-05-12

## Sources researched

- https://aws.amazon.com/education/awseducate/ (Educate — learning only)
- https://aws.amazon.com/startups/credits/ (Activate Founders apply page)
- https://aws.amazon.com/startups/learn/applying-for-aws-activate-credits-a-step-by-step-guide (step-by-step guide)
- https://aicreditmart.com/ai-credits-providers/aws-activate-founders-package-1000-credits-guide-2026/ (2026 guide)
- https://cloudvisor.co/aws-credits-for-startups/ (Cloudvisor 2026 fastest way)

## Log

- **2026-05-12:** Filed after 2nd AWS Activate rejection. AWS Educate researched + ruled out (learning platform only). Strategy: Free Tier covers Phase 1 BETA $0 net; apply Microsoft Founders Hub as backup Phase 1.5; AWS Activate reapply Phase 2 post legal entity + traction.
