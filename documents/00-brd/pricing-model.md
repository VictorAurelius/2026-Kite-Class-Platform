# Pricing Model — Tier Definition + Monetization

**Status:** skeleton
**Created:** 2026-04-29
**Updated:** 2026-04-29
**Owner:** PM + Finance
**Reviewer:** Business Lead + Tech Lead
**Related Gap:** [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) (content creation tracking)

---

## 1. Scope / Context

TODO: Mô tả 1 đoạn về pricing strategy cho KiteHub (subscription) + KiteClass (per-tenant). 4 tiers core: **Free / Pro / Premium / Enterprise**. Mục tiêu: align tier capacity với persona scale (P1 → Free/Pro, P2 → Pro/Premium, P3/P5 → Premium/Enterprise, P4 chains → Enterprise). Pricing PHẢI compliance với `compliance-scope.md` §6 (transparency) + §7 (VAT, e-invoice).

**Currency:** VND primary; USD secondary (cho international tenants P9).

---

## 2. Tier Comparison Matrix

| Feature / Limit | FREE | BASIC | PREMIUM | ENTERPRISE |
|-----------------|:----:|:---:|:-------:|:----------:|
| **Monthly price (VND)** | 0 | TODO (~500K?) | TODO (~2M?) | Custom (TODO) |
| **Monthly price (USD ref)** | 0 | TODO | TODO | Custom |
| **Annual discount** | — | TODO 15-20% | TODO 20-25% | Negotiated |
| **Max active students** | TODO 50 | TODO 200 | TODO 1000 | Unlimited |
| **Max active teachers** | TODO 1 | TODO 5 | TODO 25 | Unlimited |
| **Max classes** | TODO 5 | TODO 20 | TODO 100 | Unlimited |
| **Storage (MinIO)** | TODO 1GB | TODO 10GB | TODO 50GB | Unlimited (fair use) |
| **AI Branding regenerates / session** | 3 | 10 | 30 | Unlimited |
| **AI input token cap (per request)** | 2,000 | 4,000 (BASIC) → TODO map BASIC↔FREE | 8,000 | 16,000 or unlimited |
| **AI inference quota (req/day)** | TODO 10 | TODO 100 | TODO 500 | TODO 2000+ |
| **Custom domain** | ❌ | ❌ | ✅ | ✅ |
| **AI Branding access** | Templates only (STATIC) | Templates + Composed (TEMPLATE) | Full AI generation | Full AI + Advanced Mode |
| **Branding wizard** | Basic 3-step | Full 6-step | Full 6-step | Full + Custom prompt |
| **Bulk import (xlsx)** | ❌ | ❌ | ✅ | ✅ |
| **Parent portal** | ❌ | ❌ | ✅ (P5) | ✅ |
| **Multi-branch (P4)** | ❌ | ❌ | ❌ | ✅ |
| **API access** | ❌ | Read-only | Read+Write | Full + webhooks |
| **SSO/SAML** | ❌ | ❌ | ❌ | ✅ |
| **SLA uptime target** | None | 99.5% (link `nfr-catalog.md`) | 99.9% | 99.95% custom |
| **Support channel** | Community | Email (48h) | Email (24h) | Dedicated CS + phone |
| **Onboarding** | Self-serve | Self-serve | Guided (1 session) | White-glove |

**Note (resolved 2026-06-11, GAP-1098):** tier naming canonical = FREE/BASIC/PREMIUM/ENTERPRISE per `PricingTier.java` — tên cũ PRO đã map → BASIC toàn bộ doc này; khớp `.claude/rules/ai-branding-guidelines.md` §2.5.

---

## 3. Persona → Tier Mapping

Reference: [`personas-catalog.md`](personas-catalog.md)

| Persona | Recommended Tier | Reason |
|---------|:----------------:|--------|
| P1 Solo Teacher | FREE → BASIC upgrade | Trial low risk, upgrade khi >50 students |
| P2 Small Center | BASIC → PREMIUM | 20-100 students cần ≥BASIC |
| P3 Medium Center | PREMIUM | 100-500 students + role-based access |
| P4 Chain/Franchise | ENTERPRISE | Multi-branch only available ENT |
| P5 K-12 School | PREMIUM (small) → ENTERPRISE | Parent portal, bulk import gated |
| P7 Corporate | ENTERPRISE | SCORM, SSO needed |
| P8 Online Creator | BASIC/PREMIUM | depends on student count |
| P9 International School | ENTERPRISE | Multi-curriculum, premium pricing tolerance |

---

## 4. AI Cost Metering

AI is the highest variable cost. Tier limits MUST cap effective AI spend per tenant.

### 4.1 Metering dimensions
- Input tokens per request (capped per `ai-branding-guidelines.md` §2.5)
- Requests per day per tenant
- Concurrent AI jobs (link Bulkhead in `ai-branding-guidelines.md` §11.4.4)
- Storage of AI artifacts (MinIO)

### 4.2 Soft cap vs hard cap
TODO:
- Soft cap = warning + email at 80% quota
- Hard cap = reject HTTP 429 with upgrade CTA
- Enterprise can negotiate `-1` (unlimited) per `ai-branding-guidelines.md` §2.5

### 4.3 Overage policy
TODO:
- Free/Pro: hard cap, no overage
- Premium: hard cap default; opt-in pay-as-you-go (TODO pricing)
- Enterprise: negotiated overage rate

---

## 5. Discount Policy

### 5.1 Annual prepay
TODO: 15-20% off cho Pro/Premium; case-by-case Enterprise

### 5.2 Volume / Multi-year
TODO: Enterprise multi-year contract discount tiers

### 5.3 Education non-profit
TODO: 25-50% off cho NGO + non-profit education organizations (eligibility verification process)

### 5.4 Pilot / Beta program
TODO: Early Tier 1 customer discount (e.g. 6 months at 50% for first 10 K-12 schools — feeds `go-to-market.md`)

### 5.5 Referral
TODO: 1 month free per referred + paid tenant

---

## 6. Upgrade / Downgrade Flow

### 6.1 Upgrade
TODO: 
- Immediate effect (next billing cycle prorated)
- AI quota refreshed at upgrade timestamp
- Feature gates unlock immediately

### 6.2 Downgrade
TODO:
- Effective end of current cycle
- Data retention: warn về features sẽ lose
- Block downgrade nếu current usage > target tier limits

### 6.3 Cancellation
TODO:
- Compliance với `compliance-scope.md` §6.2 cooling-off
- Data export within X days
- Hard delete after Y days per PDPL §2.3 right-to-erasure

---

## 7. Trial Strategy

Reference: [`trial-to-paid-conversion.md`](trial-to-paid-conversion.md)

TODO:
- Trial duration: 14d / 30d?
- Trial tier: BASIC equivalent? PREMIUM with restrictions?
- Conversion friction (credit card upfront vs not)
- Trial → paid conversion KPI link `business-objectives.md` §4.1

---

## 8. Pricing Anti-Patterns (BANNED)

| ❌ Don't | ✅ Do |
|---------|-------|
| Hide AI cost behind "Premium" without quota | Always show explicit AI quota per tier |
| Charge per-student rate that scales linearly to infinity | Tier band caps; Enterprise custom |
| Free tier without student/teacher cap | Hard cap or bots will abuse |
| Surprise overage charges | Soft cap warning + opt-in only |
| Different price per region without clear basis | VND tier + USD ref; no regional surcharge |

---

## 9. Reconciliation TODOs

- [ ] Tier naming: FREE/BASIC/PREMIUM/ENTERPRISE vs FREE/BASIC/PREMIUM/ENTERPRISE (rule §2.5) — pick one
- [ ] AI Branding regenerate limits in `ai-branding-guidelines.md` §4.3 use FREE/BASIC/PREMIUM/ENTERPRISE → align
- [ ] AI input cap tier names in `ai-branding-guidelines.md` §2.5 use FREE/BASIC/PREMIUM/ENTERPRISE → align
- [ ] BR-INPUT-CAP-001..007 in `01-business/kiteclass/ai-agent-workflow/rules.md` — verify tier mapping

---

## 10. Dependencies / References

- BRD: [`personas-catalog.md`](personas-catalog.md) — persona → tier mapping
- BRD: [`business-objectives.md`](business-objectives.md) §4 ARPU + MRR KPIs
- BRD: [`compliance-scope.md`](compliance-scope.md) §6 transparency, §7 VAT/e-invoice
- BRD: [`nfr-catalog.md`](nfr-catalog.md) — SLA tier mapping
- BRD: [`trial-to-paid-conversion.md`](trial-to-paid-conversion.md) — trial flow
- Rule: [`.claude/rules/ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §2.5 input cap, §4.3 regenerate limits
- Consumer domain: `01-business/kitehub/subscription/rules.md`, `01-business/kitehub/billing/rules.md` — implement these tier rules

---

## 11. Out of Scope (this skeleton)

- Final price numbers (Phase 2 — needs Finance + market analysis)
- Currency hedging (Finance, separate gap)
- Stripe/Adyen/VNPay integration plan (architecture, separate gap)
- Tax-per-region implementation (covered by `compliance-scope.md` §7)

---

## 12. Log

- 2026-04-29 — Skeleton created (GAP-150 Phase 1). Tier matrix structure complete, all numerical TODOs require Finance + market analysis (Phase 2 GAP-155). Reconciliation TODOs flagged §9 cho tier-name mismatch giữa BRD và `ai-branding-guidelines.md`.
