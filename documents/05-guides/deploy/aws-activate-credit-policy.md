# AWS Activate Founders Pack — Credit Allocation Policy + Spend-Down Strategy

**Status:** ACCEPTED — Phase 1 BETA chốt 2026-05-07
**Last-Reviewed:** 2026-05-07
**Reviewer:** @nguyenvankiet (solo-dev, acting CTO + acting Founder)
**Closes:** GAP-412 (AWS Activate application + credit policy doc)
**Related:** GAP-411 (sizing matrix — Yr1 cost target), GAP-413 (cost monitoring — credit depletion alarm), ADR-025 (AWS Singapore), `release-1-plan-2026.md`

---

## 1. Mục đích

AWS Activate Founders Pack là free credit program cho early-stage startup, self-applied (không cần investor verification). Phase 1 BETA target: $1,000 credit cover ~13.9 tháng of $72/mo Architecture B, hoặc ~21 tháng nếu actual cost stay ~$48/mo. Document này định nghĩa:

1. Application checklist + timeline
2. Credit allocation rules (priority spend categories)
3. Spend-down strategy theo tháng
4. Transition plan post-credit (revenue-funded OR Activate Investor upgrade)

---

## 2. Application Checklist

**URL:** https://aws.amazon.com/startups/credits/ (rebrand 2024-2025; cũ `https://aws.amazon.com/activate/founders-pack` → 404 từ ~2025)
**Tier name (current):** "Activate Founder" (singular) — $1,000 USD credits, no Org ID required
**Eligibility 2026-05-07:**
- Pre-revenue startup (KiteClass status)
- Có pitch deck (1-page minimum) — see `documents/00-brd/kite-pitch-deck.md`
- AWS account active
- KHÔNG yêu cầu investor verification cho $1k tier
- KHÔNG eligible nếu đã được Activate trước đó cho cùng company

**Submission fields (per AWS Activate form 2026-05-07):**

| Field | Value |
|---|---|
| Company name | KiteClass |
| Company website | (TBD post-deploy v0.9.0-beta) |
| Founded date | 2026 (current year) |
| Description (100-200 words) | Concept summary — see `kite-pitch-deck.md` §1-2 |
| AWS account ID | (from `aws sts get-caller-identity` post-account-prep GAP-394) |
| AWS account email | vannkite@outlook.com |
| Funding stage | Pre-revenue / Bootstrapping |
| Industry | EdTech / SaaS |
| Use case | Multi-tenant SaaS platform — education centers in Vietnam |
| Estimated monthly AWS spend | $72 (per Architecture B) |
| Anticipated growth | 5-10 tenants Phase 1 BETA → 100 Phase 1.5 → 500 Phase 2 |

**Approval timeline:** Typical 1-2 tuần (per AWS Activate FAQ 2026-05-07 cite). Some applications expedited 3-5 days.

**Confirmation artifact:** Email "Welcome to AWS Activate" + credit applied screenshot tại `https://console.aws.amazon.com/billing/home#/credits`. Save screenshot tới `documents/05-guides/deploy/aws-activate-confirmation/2026-MM-DD-approval.png` (post-approval).

---

## 3. Credit Allocation Rules

**Priority order (highest = use credit first):**

| Priority | Category | Rationale | Phase 1 BETA $/mo |
|---|---|---|---|
| 1 | Production EC2 (compute) | Highest cost item per `sizing-matrix.md` §3.1 (89% of total) | $15 (Yr1) → $45 (Yr2) |
| 2 | RDS Postgres | Critical infra; Free Tier ends Yr2 | $0 Yr1 → $13 Yr2 |
| 3 | Data transfer egress | Variable, hard to optimize early | $5/mo |
| 4 | ALB / NLB | Required cho TLS + routing | $18/mo |
| 5 | CloudWatch (log + metrics) | Observability mandate per `logs-format-standard.md` | $0 Yr1 → $5 Yr2 |
| 6 | S3 + ECR + Secrets Manager | Storage + secrets | ~$3/mo |

**KHÔNG dùng credit cho:**
- Experimental / learning workloads (e.g., personal AWS tutorials)
- Sandbox environments không phục vụ Phase 1 BETA
- AWS Marketplace third-party software (credit không cover Marketplace charges per Activate ToS)
- Domain registration via Route 53 (domain registration phí riêng KHÔNG covered)

---

## 4. Spend-Down Strategy

### 4.1 Months 1-3 (Phase 1 BETA invite)

- Target burn: ~$48-72/mo actual
- Credit consumed: ~$216 (3 × $72) worst-case OR ~$144 (3 × $48) best-case
- Remaining: ~$784-856
- Activity: 5-10 tenants live, monitor cost vs estimate

### 4.2 Months 4-9 (Phase 1.5 PAID early)

- Target burn: ~$115/mo actual (Architecture A upgrade)
- Credit consumed: ~$690 (6 × $115)
- Remaining: ~$94-166 → trigger Alarm B (<20%)
- Activity: 30 paying tenants @ $5/mo = $150 revenue → covers Architecture A burn
- **Decision point month 9:** Transition off credit OR file Activate Investor ($5k upgrade)

### 4.3 Months 10+ (post-credit)

**Path A (revenue covers):**
- ≥30 paying tenants → revenue ≥$150/mo → ≥ Architecture A burn
- Continue without new credit; reserve cash buffer 3 tháng runway

**Path B (Activate Investor upgrade):**
- Apply $5k Activate Investor pack (requires venture investor verification — solo-dev mode = unlikely 2026)
- Defer per CLAUDE.md decision context (no investor engaged Phase 1)

**Path C (cost reduction emergency):**
- Hibernate non-critical services
- Downsize Architecture A → back to B
- Reduce Phase 1.5 feature scope

### 4.4 Math: 13.9 tháng coverage

$1000 credit ÷ $72/mo target = 13.9 tháng. Phase 1 BETA 9-12 tuần = 2-3 tháng. Phase 1.5 early 4-6 tuần = ~1.5 tháng → cumulative ~3.5-4.5 tháng. Revenue should kick in by month 4-5 → credit lasts well into Phase 1.5 với buffer cho cost overrun OR scope expansion.

---

## 5. Monitoring + Reporting

- **Alarm B (per GAP-413 §2.2):** Credit balance < 20% → email
- **Monthly cost report (per GAP-414):** Document credit balance trend + monthly burn rate trong `documents/04-quality/cost-reports/YYYY-MM.md`
- **Quarterly review:** Re-evaluate spend-down strategy vs actual; adjust per Path A/B/C decision

---

## 6. Constraints + Risks

### 6.1 Activate ToS constraints (cite 2026-05-07)

- Credit valid 2 năm từ approval date
- Non-transferable
- Cannot stack với other AWS promotional credits
- Apply automatically tới billable usage (cannot select per-service)
- Cannot use cho AWS Marketplace third-party

### 6.2 Risks

| Risk | Mitigation |
|---|---|
| Application denied | Re-submit với enhanced pitch deck (per `kite-pitch-deck.md`); typical denial reason = incomplete description |
| Approval delayed >2 tuần | Phase 1 BETA deploy still proceed (Architecture B Yr1 ~$48 actual without credit ~$144 over 3 tháng — manageable cash burn solo-dev) |
| Credit exhausted before revenue | Path C cost reduction; defer non-critical Phase 1.5 features |
| ToS change mid-Phase | Quarterly re-read Activate ToS; alarm on AWS announcement RSS |

---

## 7. Acceptance Criteria mapping

| GAP-412 AC | Status |
|---|---|
| Application submitted (screenshot confirmation) | ⏳ Pending — submission post account-prep (GAP-394) |
| Pitch deck draft `documents/00-brd/kite-pitch-deck.md` (1-page minimum) | ✅ Created same wave (GAP-412 Bucket E) |
| Credit approval received → balance reflected billing dashboard | ⏳ Pending approval — typical 1-2 tuần |
| Document `documents/05-guides/deploy/aws-activate-credit-policy.md` (usage rules) | ✅ this file |
| Budget alarm wired (GAP-413 dependency) | ⏳ Tracked GAP-413 §2.2 — Terraform provisioning GAP-395 |

**Status flip:** GAP-412 → 🟡 PARTIAL (policy + pitch deck ship; submission + approval = post-deploy human action per GAP-381 Phase 2 BANNED for agent).

---

## 8. References

- GAP-411 — sizing matrix
- GAP-413 — cost monitoring (Alarm B credit depletion)
- GAP-381 — agent deploy role boundaries (human submits Activate application)
- GAP-394 — account prep (creates AWS account ID for Activate form)
- ADR-025 — AWS Singapore
- `documents/00-brd/kite-pitch-deck.md` — 1-page pitch deck
- AWS Activate landing: https://aws.amazon.com/activate/
- AWS Startups credits (current Activate apply page): https://aws.amazon.com/startups/credits/ (rebrand 2024-2025)
- AWS Activate Founder package ($1,000 self-apply, no Org ID required) — sub-page của startups/credits
- `feedback_release_1_first_session_priority.md` — MVP-first philosophy

---

## 9. Log

- **2026-05-07** — Initial credit allocation policy + spend-down strategy. 3-path transition plan (revenue / Investor upgrade / cost reduction). Closes GAP-412 acceptance criterion partial (policy + pitch deck); submission + approval pending human action post-deploy.
