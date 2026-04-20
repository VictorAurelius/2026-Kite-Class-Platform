# GAP-049: Business Logic Correctness Review (not just implementation)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — process scope per `meta-gap-priority.md`)
**Domain:** Product / Business / Governance
**Detected:** 2026-04-14 (user raised)
**Scope clarification (2026-04-20):** This gap tracks **REVIEW PROCESS** (governance, cadence, stakeholder sign-off). Content creation (5 BRD docs) tracked by **GAP-150**. Persona review execution tracked by **GAP-152**. Persona AC framework tracked by **GAP-151**.
**Related:**
- `.claude/rules/output-review-mandate.md` (master)
- `.claude/rules/meta-gap-priority.md` §3 — business-logic tier
- GAP-048 (general output review violations)
- GAP-150 (BRD docs content creation — content scope)
- GAP-151 (persona AC template — framework scope)
- GAP-152 (persona review execution — execution scope)

## Problem

Dự án review business logic **implementation** (code match design) nhưng KHÔNG review business logic **correctness** (design đúng nghiệp vụ/thị trường không).

### Implementation review (có)
- `two-stage-code-review` → code match spec
- `business-gap-check` → code match business doc
- Living Docs rule → doc + code đồng bộ

### Correctness review (THIẾU)
- ❌ Ai validate business rules đúng với thị trường?
- ❌ Ai sign-off "trial 14 ngày" là số hợp lý?
- ❌ Ai check compliance với VN law?
- ❌ Có A/B test rules không?
- ❌ Stakeholder approval process?

## Evidence

### Examples của rules CHƯA được review đúng correctness

| Rule (hiện tại) | Câu hỏi chưa answer |
|----------------|---------------------|
| Trial 14 days | Thị trường VN chuẩn? 7d? 30d? Data từ đâu? |
| AI FREE: 3 calls/day | Đủ cho tenant trải nghiệm? Conversion impact? |
| AI PREMIUM: 50/day | Competitive analysis? Quá rẻ/quá đắt? |
| Rate limit tier pricing | Financial model justify? Burst vs sustained? |
| Late fee % (nếu có) | VN consumer protection law compliant? |
| Refund period | Defined? Dispute resolution? |
| Student min age | Legal age for contracts in VN? MOE rules? |
| Teacher qualifications | MOE requirements? VN education standards? |
| Data retention | GDPR + VN cybersecurity law (Law No. 24/2018/QH14)? |
| Dispute resolution | Process defined? ADR vs court? |
| Invoice numbering format | Tax authority (TCT) format compliance? |
| Teacher commission % | Financial model? Competitive? |

### Real risk scenarios

1. **Trial too short:** Tenant sign up, don't complete wizard, trial expire → churn → no chance to convert
2. **AI quota too low:** Tenant can't test feature fully in FREE → no conversion signal
3. **Non-compliance fines:** VN law requires data localization, some business rules might conflict
4. **Tax errors:** Invoice format doesn't match TCT spec → tax audit issue
5. **Legal disputes:** Undefined refund policy → chargeback, reputation damage

## Proposed Fix

### 1. Business Review Process

Tạo `.claude/rules/business-logic-review.md`:

```markdown
Every business rule MUST have:
- [ ] Source: where decision came from (data, competitor, law, gut)
- [ ] Rationale: why this value
- [ ] Reviewer: product owner / business stakeholder sign-off
- [ ] Compliance check: VN law + industry regulations
- [ ] Review cadence: quarterly / event-driven

New rule requires 3 approvals:
- Product Owner (strategic fit)
- Business Stakeholder (market fit)
- Legal (compliance)
```

### 2. Business Rule Metadata

Extend `rules.md` template với metadata:

```markdown
### AIB-01: FREE daily AI limit
- **Value:** 3 requests/day
- **Source:** Comparable with competitor X (5/day), but lower to drive upgrade
- **Rationale:** Enough to try feature, not enough for real use
- **Sign-offs:**
  - Product: @ownerName (2026-04-01)
  - Business: @bizName (2026-04-01)
  - Legal: N/A
- **Last reviewed:** 2026-04-01
- **Next review:** 2026-07-01 (quarterly)
- **A/B test:** No
- **Metrics tracked:** conversion rate by daily limit
```

### 3. Business Requirements Document (BRD)

Create `documents/00-brd/` folder cho high-level requirements:

```
00-brd/
├── MASTER-BRD.md          # Overall product strategy
├── pricing-strategy.md    # Why these tiers, prices, quotas
├── trial-strategy.md      # Trial mechanics with data
├── compliance-checklist.md # VN law, GDPR, MOE, TCT
└── market-analysis/       # Competitor comparisons
```

Every BRD doc reviewed by:
- CEO / Product lead
- Business stakeholder (center owner rep?)
- Legal counsel

### 4. Quarterly Business Rule Review

Schedule recurring meeting:
- Review all business rules
- Check for drift (reality vs assumption)
- Update based on data:
  - Trial length vs conversion rate
  - Quotas vs usage patterns
  - Pricing vs competitive shifts
- Legal compliance re-check

### 5. A/B Testing Framework (reuse GAP-044)

For key business rules, run A/B tests:
- Trial length: 7 vs 14 vs 30 days
- AI quota: 3 vs 5 vs 10 / day
- Upgrade prompts timing

Decisions data-driven, not gut-feel.

### 6. Update business-gap-check skill

Add new section (v1.4):

```markdown
#### 2.10 Business Logic CORRECTNESS (not just implementation)

| Check | Method | Target |
|-------|--------|--------|
| Each rule has source documented | Check rules.md metadata | 100% |
| Each rule has stakeholder sign-off | Check reviewers in metadata | 100% |
| Rules reviewed quarterly | Check last-reviewed dates | ≤90d |
| VN law compliance checklist | documents/00-brd/compliance-checklist.md | Current |
| A/B test data for key rules | Check analytics | Rolled-up monthly |
| Pricing model justified | BRD pricing-strategy.md | Up-to-date |
```

### 7. Compliance Checklist

Create `documents/00-brd/compliance-checklist.md`:

Vietnam specific:
- [ ] Law on Cybersecurity (24/2018/QH14) — data localization
- [ ] Law on Electronic Transactions — e-signatures
- [ ] Consumer Protection Law — refunds, disputes
- [ ] Education Law — MOE regulations for education centers
- [ ] Tax Law — invoice format, VAT handling
- [ ] Labor Code — teacher contracts
- [ ] PDPA/GDPR — personal data protection

International (if scale):
- [ ] GDPR (EU users)
- [ ] CCPA (US California)

## Acceptance Criteria

### Scope: REVIEW PROCESS (this gap)

- [ ] `.claude/rules/business-logic-review.md` created — review process doc
- [ ] `documents/00-brd/` folder exists (✅ done 2026-04-14, PR #349 via GAP-101)
- [ ] All existing rules in `01-business/` updated với metadata (source, sign-offs) — can be bulk PR after stakeholder session
- [ ] Quarterly review cadence documented (rule or ADR) — meeting schedule is operational, not deliverable
- [ ] business-gap-check v1.4 includes correctness checks
- [ ] Legal counsel engagement process defined (who initiates, what scope)

### Delegated to sibling gaps (scope split 2026-04-20)

- ~~`documents/00-brd/` BRD docs content~~ → **GAP-150** (business-objectives, compliance-scope, pricing-model, nfr-catalog, go-to-market skeletons)
- ~~Pricing strategy document written~~ → **GAP-150** Phase 2
- ~~Trial strategy document written~~ → deferred (covered by pricing-model.md in GAP-150)
- ~~Compliance checklist reviewed by lawyer~~ → **GAP-150** `compliance-scope.md` + stakeholder engagement (operational)
- ~~A/B testing framework~~ → deferred to post-GA (not blocking)
- Persona review process framework → **GAP-050**
- Persona AC template + per-persona AC → **GAP-151**
- Persona review execution → **GAP-152**

## Risks of NOT fixing

- **Low conversion:** Wrong trial length/quota = lose users
- **Legal fines:** Non-compliance with VN laws
- **Reputation:** Dispute handling undefined = customer complaints go public
- **Tax audit:** Invoice format non-compliant = TCT issues
- **Churn:** Business rules don't match market = users leave

## Execution Plan

**Phase 1 (P0 — month 1):**
- Legal counsel engagement cho VN law compliance
- Compliance checklist với sign-off
- BRD folder + MASTER-BRD

**Phase 2 (P1 — month 2):**
- Metadata migration cho all existing rules.md
- Pricing + trial strategy docs
- Quarterly review meeting setup

**Phase 3 (P2 — month 3):**
- A/B testing framework (reuse GAP-044)
- Analytics integration
- First quarterly review

## Dependencies

- GAP-042 (legal/IP) — shares legal counsel engagement
- GAP-044 (feature flags) — A/B testing infrastructure
- GAP-048 (output review mandate) — this is specific instance

## References

- VN Law references (full list in compliance-checklist.md)
- Competitor analysis (market-analysis/)
- Master rule: `.claude/rules/output-review-mandate.md`

## Log

- 2026-04-20 — Scope split for clarity: this gap = REVIEW PROCESS only. Content creation → GAP-150. Persona AC → GAP-151. Persona review execution → GAP-152. AC section rewritten to reflect split. Execution Plan Phase 1 BRD folder deliverable marked done (GAP-101); content creation moved to GAP-150.
- 2026-04-14 — User raised: "business logic có review không?" — revealed correctness review missing (only implementation reviewed)
