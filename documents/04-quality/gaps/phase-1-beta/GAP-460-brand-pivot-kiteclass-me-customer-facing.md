# GAP-460: Brand pivot — KiteClass.me as customer-facing brand (KiteHub stays internal)

**Status:** 🟡 PLANNED — decision recorded; execution **deferred to Phase 1.5 or later release** (2026-05-10 user-flagged: "hiện tại không có công sức để rebrand"). Current Phase 1 BETA tiếp tục dùng `kitehub.me` + KiteHub customer branding ad-interim.
**Priority:** 🟡 P2 (decision logged; no immediate Phase 1 BETA blocker since invite-only mode minimal SEO exposure; revisit Phase 1.5 PAID launch pre-public-marketing)
**Domain:** Strategic / brand / business correctness
**Found:** 2026-05-10 (user surfaced collision search "kitehub" Google → kitehub.eu existing brand)
**Affects:** entire customer-facing brand surface, domain strategy, trademark filing, AWS Activate resubmit context

## Problem

User found 2026-05-10 that searching "kitehub" on Google surfaces `kitehub.eu` first → **brand collision**. Investigation revealed:

| Brand | Sector | Status |
|-------|--------|--------|
| **KiteHub a.s.** (kitehub.eu) | Water sports SaaS (booking/management for kitesurfing schools EU) | Czech joint-stock company, ~20 years operation |
| **Kite® / Kite Suite** ([kiteassessments.org](https://kiteassessments.org/)) | Education K-12 assessment platform | Registered trademark of University of Kansas (USPTO) |
| **kiteclasses.org** | Sport (kiteboarding lessons) | International Kiteboarding Association |
| **Kite (Kerala)** | Government IT for education infra (India) | State agency |
| **Our project (pre-decision)** | Education K-12 + center management SaaS (Vietnam) | Phase 1 BETA pre-launch |

Cluster of "Kite-*" brands in adjacent verticals creates:
1. **SEO collision** (permanent globally — "kitehub" search returns competitors)
2. **Trademark dispute risk** if expand to US/EU education sector (KU Kite® register Class 41+9 + KiteHub.eu use Class 42 SaaS)
3. **AWS Activate Founder denial 2026-05-10** ("Your website cannot be accessed") = surface symptom of bigger brand-clearance gap

## Decision (this gap)

**Path B' — KiteClass for customer + KiteHub stays internal**

| Layer | Brand | Customer-visible? |
|-------|-------|:------:|
| Public marketing + signup + onboarding + customer dashboard | "KiteClass" | ✅ YES |
| Tenant subdomain pattern | `tenant.kiteclass.me` | ✅ YES |
| Email templates (sender, footer, support) | "KiteClass" | ✅ YES |
| Logo + favicon + brand assets | "KiteClass" | ✅ YES |
| User-facing docs (`documents/05-guides/**`) | "KiteClass" | ✅ YES |
| Code repo names (`kitehub-frontend`, `kitehub-subscription`, `kitehub-platform`) | "KiteHub" / "KiteClass" | ❌ internal only |
| Docker container prefixes (`kite-*`, `kitehub-*`, `kiteclass-*`) | mixed | ❌ internal infra |
| AWS resource names (`kitehub-postgres`, `kitehub-alb`) | "KiteHub" | ❌ internal infra |
| Architecture docs (`documents/02-architecture/**`) | "KiteHub" / "KiteClass" terminology | ❌ dev-only |
| Internal admin URLs (`admin.kitehub.me`) | "KiteHub" | ❌ founder/dev only |
| GAP files + wave plans + memory + rules | "KiteHub" / "KiteClass" | ❌ internal record |

## 5-attribute review (per `business-logic-review.md` §2)

### 1. Source

- **Trademark search 2026-05-10:**
  - Google search "kitehub" — surfaces kitehub.eu first (water sports SaaS, Czech)
  - Google search "kiteclass" — no clear single-mark TM holder; "kiteclasses" (plural, ICA kiteboarding) different vertical
  - EUIPO eSearch — no specific KiteHub TM registration found in initial search; needs deeper verification
  - USPTO TESS — Kite® registered by University of Kansas (Class 9 software + Class 41 testing services for K-12 assessment)
  - NOIP Vietnam — pending search (user-action; expected: no prior KiteClass mark in VN K-12 education)
- **Domain availability check 2026-05-10:** RDAP query `errorCode: 404 Object not found` for `kiteclass.me` → confirmed available
- **Existing brand investments:**
  - GAP-458 (2026-05-09) chose `kitehub.me` via GitHub Student Pack Free path (~$0 year 1)
  - PR #1084 + #1085 shipped Tier 2 + Tier 3 automation (Cloudflare Origin Cert 15-year for `kitehub.me + *.kitehub.me`, Vercel apex bind, Email Routing)
  - AWS Activate $1k credit DENIED 2026-05-10 ("Your website cannot be accessed") — GAP-459 fix planned but blocked by brand decision

### 2. Rationale

Why "KiteClass" not other alternatives:
- **Compound mark "KiteClass"** ≠ "KiteHub" — different word, different commercial impression (precedent: "Apple" vs "Apple Records" coexisted; "Delta Airlines" vs "Delta Faucet" coexist)
- **"KiteClass"** has natural meaning in education context (class as in classroom + management of classes) — doesn't require explanation
- **Preserves "Kite" emotional anchor** for founder while differentiating commercial mark
- **Single-brand customer experience** (no "KiteHub vs KiteClass" confusion at signup) — customer remembers ONE name
- **Geographic-bound trademark defense** = use in Vietnam K-12 education ≠ KU Kite® US K-12 assessment ≠ KiteHub.eu EU water sports
- **Class 41 sub-class differentiation** = K-12 multi-tenant management ≠ standardized testing services ≠ sports instruction

Why `.me` TLD:
- **Free year 1** via GitHub Student Pack Namecheap (same channel as GAP-458) — $0 incremental cost vs ~$20-30/yr for `.vn`
- **Operational consistency** — reuse Cloudflare/Vercel workflow already proven via PR #1084/#1085
- **Avoid VN-registrar KYC pain** (Mat Bao/PA Vietnam slower workflow)
- **TLD ≠ trademark defense** — defensibility comes from MARK + market locale + NOIP filing, not TLD
- **Defensive `.vn` deferred** to Phase 1.5 if needed (~$20/yr)

Why dual-brand strategy (KiteClass public + KiteHub internal):
- **Code/infra rename cost prohibitive** (~4 weeks dev) → not justified when brand exposure is fixable at Surface only
- **Industry precedent:** Alphabet → Google; Meta → Facebook; PBC → Substack — internal entity ≠ consumer brand
- **Trademark exposure narrowed** to one mark ("KiteClass") for filing + defense focus
- **Founder/dev mental model preserved** ("KiteHub manages instances; KiteClass is the multi-tenant product") — architectural distinction stays useful internally

### 3. Reviewer

- **Acting Product Owner + Legal scout (solo-dev mode, 2026-05-10):** @nguyenvankiet
- **Formal Legal counsel review:** queued (not engaged Phase 1 BETA per CLAUDE.md decision context locked 2026-05-06; engage Phase 3 K-12 trigger per `release-1-plan-2026.md`)
- **Risk acceptance:** moderate (Phase 1 BETA disclaimer "v1 pending counsel review" applies)

### 4. Compliance check

- **Vietnamese trademark (NOIP):** filing planned Class 41 (Education services - K-12 management) + Class 42 (SaaS technology services); filing fee ~$200; processing ~6 months but priority date secured immediately
- **PDPL 2023:** N/A for trademark itself (PDPL governs personal data); brand decision doesn't affect PDPL compliance
- **Consumer Protection Law 2023:** brand transparency adequate (single consistent name across all customer touchpoints)
- **Cybersecurity Law 2018 + Decree 53/2022:** no impact (locale-bound trademark filing in VN aligns with data localization signals)
- **EU GDPR / EUIPO:** out-of-scope Phase 1 BETA (no EU customers); revisit Phase 2 if expansion
- **USPTO:** out-of-scope Phase 1 BETA; revisit Phase 1.5 PAID + Phase 2 international expansion

### 5. Review cadence

- **Quarterly review** of trademark filing status (NOIP processing, opposition window if filed publicly)
- **Event-driven re-review** triggers:
  - KU Kite® or KiteHub.eu file VN trademark (Madrid Protocol extension)
  - Customer confusion incident (e.g., support ticket mentions "wrong KiteHub")
  - Phase 1.5 PAID launch (re-evaluate global TM strategy)
  - Phase 2 international expansion (US/EU TM filing)
- **Next review:** 2026-08-10 (3 months) OR upon trigger event

## Acceptance Criteria

### Phase 1 — Decision + Domain (this gap)
- [x] Brand decision documented (Path B' kiteclass.me)
- [x] Trademark search summary documented (sources cited)
- [x] Supersedes GAP-458 + reframes GAP-459 noted
- [x] 5-attribute review per `business-logic-review.md` §2 complete
- [ ] User-action: claim `kiteclass.me` via Namecheap GitHub Student Pack (~5 min, $0 year 1)
- [ ] User-action: NOIP trademark search "KiteClass" Class 41 + 42 online tool (~10 min, free)
- [ ] User-action: NOIP filing if available (~$200, online portal, priority date secured immediately)

### Phase 2 — Customer surface rebrand (deferred to Wave 52)
- [ ] Customer-facing UI text "KiteHub" → "KiteClass" in `kitehub-frontend/src/app/(public|auth|customer)/**`
- [ ] Email templates rebrand (sender display + footer + support email)
- [ ] Logo SVG + favicon update if displays "KiteHub"
- [ ] Vercel apex bind `kiteclass.me` (Tier 1 cutover)
- [ ] Cloudflare Origin Cert generate for `kiteclass.me + *.kiteclass.me` (Tier 2)
- [ ] Email DKIM/SPF/DMARC re-setup for kiteclass.me (similar GAP-370 SES domain)
- [ ] Tenant subdomain pattern `tenant.kiteclass.me` (replace `tenant.kitehub.me`)
- [ ] User-facing docs in `documents/05-guides/**` rebrand
- [ ] End-to-end customer journey test new domain

### Phase 3 — AWS Activate resubmit (deferred to post-Phase 2)
- [ ] AWS Activate Founder application resubmit with KiteClass branding context + live `kiteclass.me` URL

### What stays unchanged (zero touch)
- [x] Code repo names: `kitehub-frontend`, `kitehub-subscription`, `kitehub-platform`, etc.
- [x] Docker container prefixes: `kite-*`, `kitehub-*`, `kiteclass-*`
- [x] AWS resource names: `kitehub-postgres`, `kitehub-alb`, etc.
- [x] Internal `documents/02-architecture/**` (KiteHub/KiteClass distinction is dev terminology)
- [x] `kitehub.me` retained for admin/founder/dev access
- [x] `.claude/rules/**`, `MEMORY.md`, gap files, wave plans (historical record)

## Related

- Supersedes: [GAP-458](GAP-458-domain-procurement-release-1.md) (`kitehub.me` customer domain decision)
- Reframes: [GAP-459](GAP-459-kitehub-vn-to-me-sweep.md) (sweep scope changes from `.vn` → `.me` to `kitehub-*` → `kiteclass-*` for customer surfaces only)
- Cross-link: [GAP-461](GAP-461-rule-brand-clearance-pre-domain.md) (meta-rule to prevent recurrence)
- Wave 52 plan: customer surface rebrand sweep (file post-merge of this gap)

## Log

- **2026-05-10**: Decision filed after user-flagged collision (`kitehub.eu` Google search). Per `incident-to-rule-pipeline.md` 5-stage applied: Stage 1 Detect ✅ (user-flagged); Stage 2 Classify ✅ (no existing rule covers brand-clearance pre-domain — meta-rule needed = GAP-461); Stage 3 Rule+Enforce → GAP-461 codifies brand-clearance protocol; Stage 4 Self-test ✅ (rule §6 worked example would have caught KiteHub.eu collision pre-GAP-458 if applied 2026-05-09); Stage 5 Retro Log ✅ (this entry). User chose Path B' kiteclass.me primary (free year 1 via Student Pack, geographic-bound trademark defense via NOIP filing + market locale, dual-brand strategy preserves internal architecture). 5-attribute review per `business-logic-review.md` §2 complete; reviewer = solo-dev acting Product Owner + Legal scout; formal counsel review queued Phase 3 K-12 trigger. Phase 2 customer surface rebrand scoped to Wave 52. AWS Activate resubmit deferred to post-Phase 2 with new branding context.
- **2026-05-10 (deferral)**: Status flipped 🔴 P0 BLOCKING → 🟡 PLANNED P2 per user-flagged "hiện tại không có công sức để rebrand". Rationale: Phase 1 BETA invite-only mode has minimal SEO/brand exposure (no public marketing, no organic traffic gates); SEO collision + trademark dispute risk thresholds are Phase 1.5 PAID launch problems, not Phase 1 BETA blockers. Brand DECISION remains valid + recorded; EXECUTION deferred. Phase 1 BETA continues with existing `kitehub.me` + KiteHub customer branding ad-interim. Re-review trigger: (a) Phase 1.5 PAID public launch ≥30 days out; (b) any customer confusion incident; (c) KU Kite® / KiteHub.eu file VN trademark via Madrid Protocol; (d) AWS Activate resubmit timing pressure. User-action `kiteclass.me` claim + NOIP filing also deferred (no rush; defensive registrations can happen any time).
