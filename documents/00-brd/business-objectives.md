# Business Objectives — KiteHub + KiteClass

**Status:** skeleton
**Created:** 2026-04-29
**Updated:** 2026-04-29
**Owner:** PM
**Reviewer:** Tech Lead
**Related Gap:** [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) (content creation tracking)

---

## 1. Scope / Context

TODO: Mô tả 1 đoạn về scope của BRD Objectives — KiteHub (SaaS lifecycle management) + KiteClass (multi-tenant education delivery). Đối tượng audience: PM, Business Lead, Stakeholders. Quan hệ với `personas-catalog.md` (audience), `pricing-model.md` (monetization KPIs), `nfr-catalog.md` (technical KPIs feeding business KPIs).

---

## 2. North-Star Metric

TODO: define single primary KPI driving cả 2 sản phẩm.

Candidate options (chọn 1, tham khảo `personas-catalog.md` Tier 1):
- **Active Tenants × Avg Active Students** (proxy cho platform health)
- **Monthly Recurring Revenue (MRR)** — đơn giản tài chính
- **Net Revenue Retention (NRR)** — long-term tenant health

Decision: TODO (cần stakeholder input)

---

## 3. OKRs Template (per quarter)

Mỗi quý đặt 3-5 Objectives, mỗi Objective 2-4 Key Results.

### Template

```markdown
### Objective Q{N} {YYYY}: <Outcome statement, ambitious nhưng đo được>

- **KR1:** <Quantitative target, baseline → goal, deadline>
- **KR2:** ...
- **KR3:** ...
```

### Q2 2026 (PLACEHOLDER — cần PM fill)

#### Objective 1: Achieve product-market fit với Tier 1 personas
- **KR1:** TODO — số tenant active P1/P2/P3/P5 (baseline: 0, goal: ?)
- **KR2:** TODO — NPS từ pilot tenants (baseline: ?, goal: ≥40)
- **KR3:** TODO — % feature coverage cho P5 K-12 từ 30% → 70%

#### Objective 2: Establish operational reliability
- **KR1:** TODO — uptime % (link với `nfr-catalog.md`)
- **KR2:** TODO — Mean time to recovery (MTTR)

#### Objective 3: TODO

---

## 4. Success Metrics (Operating KPIs)

KPIs theo dõi tuần/tháng. Khác OKRs (đặt mục tiêu) — đây là health dashboard.

### 4.1 Acquisition
- **New tenants/month**: TODO target (baseline: ?)
- **Trial → Paid conversion %**: TODO (link `trial-to-paid-conversion.md`)
- **CAC (Customer Acquisition Cost)**: TODO

### 4.2 Engagement
- **DAU / MAU ratio**: TODO target (proxy cho stickiness)
- **Avg sessions/tenant/week**: TODO
- **Feature adoption rate per tier (Free/Pro/Premium/Enterprise)**: TODO

### 4.3 Retention
- **Monthly churn % per tier**: TODO targets
- **Cohort retention M1/M3/M6**: TODO

### 4.4 Revenue
- **MRR**: TODO target trajectory
- **ARPU per tier**: TODO (link `pricing-model.md`)
- **NRR**: TODO target ≥110% (industry benchmark SaaS)

### 4.5 Platform health (technical KPIs feeding business)
- TODO — link to `nfr-catalog.md` SLA/Performance budgets
- AI cost per active tenant (cost-control feedback) — TODO

---

## 5. Decision-Making Framework

TODO: Khi conflict giữa metrics (ví dụ growth vs retention), thứ tự ưu tiên:

1. TODO (e.g. retention > growth nếu churn >5%/tháng)
2. TODO
3. TODO

---

## 6. Review Cadence

| Cadence | Output | Owner |
|---------|--------|-------|
| Weekly | KPI dashboard snapshot | PM |
| Monthly | OKR check-in (mid-quarter pulse) | PM + Tech Lead |
| Quarterly | OKR retro + next quarter plan | PM + Business Lead + Stakeholders |
| Annually | North-Star review (still right metric?) | All |

---

## 7. Dependencies / References

- BRD: [`personas-catalog.md`](personas-catalog.md) — target audience driving objectives
- BRD: [`pricing-model.md`](pricing-model.md) — revenue KPIs
- BRD: [`nfr-catalog.md`](nfr-catalog.md) — operational KPIs
- BRD: [`go-to-market.md`](go-to-market.md) — acquisition KPIs
- Consumer domains: `01-business/*` — per-domain rules.md derive from objectives
- External: industry SaaS benchmarks (TODO link) cho NRR/churn calibration

---

## 8. Out of Scope (this skeleton)

- Filling actual KPI numbers (Phase 2 — needs stakeholder + finance data)
- OKR sign-off (requires Business Lead + leadership)
- North-Star final selection (requires stakeholder workshop)

---

## 9. Log

- 2026-04-29 — Skeleton created (GAP-150 Phase 1 Wave Business Correctness). Section structure complete; TODOs mark stakeholder input needed for content fill (Phase 2 GAP-155).
