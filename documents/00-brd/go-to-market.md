# Go-to-Market Plan

**Status:** skeleton
**Created:** 2026-04-29
**Updated:** 2026-04-29
**Owner:** PM
**Reviewer:** Business Lead + Marketing
**Related Gap:** [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) (content creation tracking)

---

## 1. Scope / Context

TODO: Mô tả 1 đoạn — GTM plan định nghĩa thứ tự đi vào thị trường, persona ưu tiên, pilot strategy, sales funnel. Vietnam-first market (`personas-catalog.md` §"Role-Play Assumption"). Hai sản phẩm khác channel: KiteHub bán B2B (tenant onboarding), KiteClass instance UX phục vụ users của tenant. GTM tập trung KiteHub acquisition.

---

## 2. Target Persona Priority

Reference: [`personas-catalog.md`](personas-catalog.md) Tier 1.

### Phase 1 — Beachhead (Months 0-6 post-GA)

Target **2 personas** để focus engineering + sales:

| Priority | Persona | Why beachhead |
|:--------:|---------|---------------|
| 🥇 #1 | TODO (proposal: P2 Small Tutoring Center) | Easy onboarding, owner-decision (no committee), pricing tolerance moderate, lots of them in VN |
| 🥈 #2 | TODO (proposal: P5 K-12 School pilot) | High ACV, lighthouse references, but long sales cycle — need 2-3 paid cases first |

Defer Tier 1 P1 (Solo Teacher) + P3 (Medium Center) until product-market fit shown trên P2/P5.

### Phase 2 — Expansion (Months 6-18)

TODO: roll out to remaining Tier 1 + Tier 2:
- P1 Solo Teacher (self-serve channel)
- P3 Medium Center (sales-assisted)
- P4 Chain (enterprise sales)
- P9 International School (premium channel)

### Phase 3 — Adjacencies (Months 18+)

TODO: P7 Corporate Training, P8 Online Creator (re-evaluate fit)

---

## 3. Pilot Strategy

### 3.1 First-10 Pilot Customers (Months 0-3 post-GA)

**Objective:** 10 paying customers across beachhead personas → produce 3 lighthouse case studies.

TODO:
- Discount: 50% off Year 1 (link `pricing-model.md` §5.4)
- Enhanced support: dedicated CS contact, weekly check-in
- Co-marketing: case-study + logo permission required (in MSA)
- Eligibility: must commit to 6-month minimum + provide testimonial

### 3.2 Pilot success criteria
TODO:
- ≥7/10 active 6 months post-onboarding
- NPS ≥40 from pilot cohort
- 3 detailed case studies published
- ≥1 K-12 school + ≥3 tutoring centers + ≥2 medium centers

### 3.3 Pilot risk mitigation
TODO:
- Weekly stakeholder review with PM
- Escalation path: any P1 issue → CTO direct
- Exit clause if SLA breached

---

## 4. Acquisition Channels

### 4.1 Inbound (organic)
TODO:
- SEO: blog (link `kitehub-frontend` blog MDX), keyword research
- Content marketing: K-12 admin pain-point articles, Vietnamese
- Webinar: "Quản lý trung tâm số hóa" mỗi quý

### 4.2 Outbound (sales-driven)
TODO:
- Direct outreach: P5 K-12 schools via Sở GD&ĐT relationships
- Conference presence: education tech expos VN
- Cold email cadence: P3 medium centers

### 4.3 Partnerships
TODO:
- Education NGOs (refer non-profit to Free/discounted tier)
- Existing edtech complementary tools (referral program — `pricing-model.md` §5.5)
- Bank/payment partners (VNPay, MoMo, ZaloPay) — bundled offer

### 4.4 Self-serve (low-touch)
TODO:
- P1 Solo Teacher channel: signup → trial → conversion
- Optimize onboarding flow (link `trial-to-paid-conversion.md`)

---

## 5. Sales Funnel + Stages

| Stage | Definition | Target conversion | Owner |
|-------|-----------|:-----------------:|-------|
| **Awareness** | Visited site or read content | TODO 100% baseline | Marketing |
| **Interest (MQL)** | Signup for trial / contacted sales | TODO 5% of awareness | Marketing |
| **Trial active** | Used product ≥3 sessions in trial | TODO 60% of MQL | Product + CS |
| **Sales Qualified (SQL)** | Spoke with sales OR self-serve trial completed | TODO 40% of trial | Sales |
| **Proposal sent** | (Enterprise/Premium) | TODO 70% of SQL | Sales |
| **Closed-won** | Contract signed + first payment | TODO 25% of SQL self-serve, 40% of SQL sales-led | Sales |
| **Activated** | Tenant DEPLOYED + first user logs in | TODO 90% of won | CS |
| **Retained M3** | Active 3 months in | TODO 80% of activated | CS |

KPIs feed [`business-objectives.md`](business-objectives.md) §4.1.

---

## 6. Pricing Positioning

Reference: [`pricing-model.md`](pricing-model.md)

TODO:
- Anchor positioning: "Most affordable VN-built education SaaS — built for VN K-12 + tutoring market"
- Differentiation vs international competitors (Google Classroom, Microsoft Teams Education, Moodle):
  - VN-localized features (Tết calendar, Zalo integration, MoET reporting, MST/e-invoice)
  - VN-language support
  - Local data residency (per `compliance-scope.md` §4.1 NĐ-53)
- Differentiation vs VN competitors: TODO (research scope)

---

## 7. Launch Timeline (placeholder)

| Month | Milestone | Gate |
|-------|-----------|------|
| M-3 (pre-GA) | TODO Wave-N closure: GA blockers cleared | All P0 gaps DONE |
| M-2 | TODO Beta program: 5 selected pilot prospects | NFR baselines green |
| M-1 | TODO Compliance sign-off | `compliance-scope.md` §1-7 reviewed |
| M0 (GA) | TODO Public launch | Pilot 10 contracts signed |
| M+1 | TODO Case study #1 published | Pilot retention ≥80% M1 |
| M+3 | TODO Pilot completion | NPS measured, 3 case studies |
| M+6 | TODO Phase 2 expansion start | NRR baseline measured |

Gate signoffs: PM + Tech Lead + Legal.

---

## 8. Marketing Assets Required (skeleton list)

TODO produce per launch:
- [ ] Landing page (kitehub-frontend) — current state, optimize
- [ ] Pricing page (clear, transparent per `compliance-scope.md` §6.1)
- [ ] Demo video (1 per Tier 1 persona, 60-90s VN)
- [ ] Case study templates (post-pilot)
- [ ] Comparison table (vs Google Classroom, vs Moodle)
- [ ] FAQ (compliance, security, pricing)
- [ ] Blog series: 12 K-12 admin pain-point articles (1/month)
- [ ] Sales deck (Enterprise sales)
- [ ] One-pager flyer (event distribution)
- [ ] Email nurture sequence (trial drip)

Reference: [`.claude/skills/quality/marketing-legal-review/`](../../.claude/skills/quality/marketing-legal-review/) trước khi publish.

---

## 9. Risks + Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|:----------:|:------:|------------|
| K-12 sales cycle longer than 6 months | High | High | Beachhead với P2 first, K-12 pilot parallel as Phase 2 prep |
| Compliance gap blocks P5 launch | Medium | Critical | `compliance-scope.md` §3 MoET sign-off pre-GA |
| AI cost overrun (free abuse) | Medium | High | Hard tier caps + soft warnings (`pricing-model.md` §4) |
| International competitor enters VN | Medium | Medium | Differentiate VN-localized + lower price |
| TODO add risks | — | — | — |

---

## 10. Dependencies / References

- BRD: [`personas-catalog.md`](personas-catalog.md) — Tier 1 priority decisions
- BRD: [`pricing-model.md`](pricing-model.md) §5.4 — Pilot discount, §5.5 — Referral
- BRD: [`business-objectives.md`](business-objectives.md) §4.1 — Acquisition KPIs feed funnel
- BRD: [`compliance-scope.md`](compliance-scope.md) §3 MoET, §6 Consumer Protection
- BRD: [`nfr-catalog.md`](nfr-catalog.md) — SLA promises in sales motion
- BRD: [`trial-to-paid-conversion.md`](trial-to-paid-conversion.md) — funnel detail
- Skill: [`.claude/skills/quality/marketing-legal-review/`](../../.claude/skills/quality/marketing-legal-review/) — gate per asset
- Consumer domain: `01-business/kitehub/subscription/` — onboarding flow implementation

---

## 11. Out of Scope (this skeleton)

- Final pilot customer list (Phase 2 — Sales)
- Marketing budget (Finance + leadership)
- Competitive analysis report (Marketing research, separate work)
- Specific case-study text (post-pilot only)
- International launch (Year 2+)

---

## 12. Log

- 2026-04-29 — Skeleton created (GAP-150 Phase 1). Phased GTM (Beachhead → Expansion → Adjacencies), funnel stages with placeholder conversion targets, launch timeline gates. Persona priority TODO requires Business Lead workshop (Phase 2 GAP-155).
