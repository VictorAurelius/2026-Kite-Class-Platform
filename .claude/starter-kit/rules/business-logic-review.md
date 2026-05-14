# Business Logic Correctness Review

**Priority:** 🔴 CRITICAL — every business rule (constraint, threshold, pricing tier, compliance check) MUST pass review before merge
**Version:** 1.0.0
**Created:** 2026-04-29
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every per-domain `documents/01-business/*/rules.md` file (currently 45 across <tenant-product> + <backend-product>) AND every code constant encoding business value (timeouts, limits, thresholds, prices, tiers, quotas, periods)
**Closes:** Phase 1 of GAP-049 (rule file shipped + matrix-row flip); Phase 2 audit + stakeholder sign-offs tracked in GAP-156

---

## 1. The Rule

> **Every business rule MUST document 5 attributes: Source, Rationale, Reviewer, Compliance check, Review cadence.**
> A rule without these is unreviewed business logic — its value is a guess, its compliance status is unknown, and its review timeline is "never."

`output-review-mandate.md` §3 splits *implementation review* (does the code match the doc?) from *correctness review* (does the doc reflect what the market + law actually require?). This rule fills the correctness gap. Implementation can be perfect while the underlying rule is wrong: "trial 14 days" is implemented exactly, but if the market converts at 21 days the rule is the bug.

This rule does NOT replace `audit-to-gap-pipeline.md` (which routes audit findings to gap files), nor `rule-change-process.md` (which governs `.claude/rules/**` edits). It governs **business values** — the substantive content of `documents/01-business/*/rules.md` and the constants those values flow into in code.

---

## 2. Five required attributes

Every business rule entry in `rules.md` (or every business-value constant in code) MUST document these. If any attribute is missing, the rule is not review-ready.

### 2.1 Source

Where did the value come from? Pick one or more:

| Source category | Example | Documentation requirement |
|-----------------|---------|--------------------------|
| **Data** | "Internal A/B test conversion data, Q1 2026" | Link to dashboard / report |
| **Competitor analysis** | "Hotmart 7-day trial; the project goes 14d to differentiate" | Name the competitor + cite the public source |
| **VN law / regulation** | "Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023) Art 23 — 30-day retention min" | Statute number + article |
| **Stakeholder interview** | "PM interview with 3 education-center owners, 2026-04-15" | Date + interview notes pointer |
| **Industry standard** | "MoET education-center licensing guidelines" | Standard reference |
| **Informed gut** | "First-best estimate; A/B test scheduled Q3 2026" | Must explicitly say "informed gut" + flag for re-review within 1 quarter |

**"No source" is not a source.** A rule with no documented source defaults to `informed gut` and inherits its quarterly re-review obligation.

### 2.2 Rationale

*Why this exact value, not adjacent values?* Answer the question a reviewer will ask:

> "Why 14 days, not 7 or 30?"

Concrete rationale formats:
- **Asymmetric cost:** "7 days too short — onboarding takes 5d on avg; 30 days too long — competitor Hotmart shows steep churn after 21d."
- **Anchored to data:** "Median time-to-value in Q1 was 9d; trial = 1.5× median per industry heuristic."
- **Anchored to law:** "PDPL minimum is 30d; we ship 30d to comply, not more (storage cost)."
- **Tiered:** "FREE = 3/day to enable evaluation; PRO = 30/day = ~3-4 daily users; PREMIUM = 100/day = small school usage."

Banned rationale: "Standard," "It seemed right," "Engineering chose this." If those are the only reasons, document `informed gut` per §2.1 and flag for stakeholder review.

### 2.3 Reviewer

**Who signed off?** Each business rule has a designated reviewer role. Self-approval by the rule's author is BANNED for business rules (this is the single most important difference vs `rule-change-process.md`, which allows solo-dev self-approve for `.claude/rules/`).

| Rule scope | Required reviewer role(s) |
|-----------|---------------------------|
| Pricing, tier quotas, billing terms | Product Owner + Business Stakeholder |
| Trial mechanics, conversion funnels | Product Owner |
| Refund / dispute / late-fee | Product Owner + Legal counsel |
| Data retention, PII handling, consent | Legal counsel + Compliance |
| Tax / invoice / financial docs | Legal counsel + Tax advisor |
| Education-specific (student age, teacher qual) | Product Owner + Education domain expert |
| Pure-engineering thresholds (timeout, retry count) — BUSINESS impact only when user-facing | Tech Lead suffices if no user-visible business impact |

**Solo-dev mode (current state):** the project author wears multiple hats (PO + dev + sometimes legal scout). Solo-dev sign-off is acceptable IF and ONLY IF the Reviewer field documents which hat is being worn AND a follow-up obligation is attached:

```
Reviewer: @nguyenvankiet (acting Product Owner, solo-dev, 2026-04-29). Legal review queued — see GAP-156.
```

When team grows beyond solo, this exemption disappears. The expectation is that `informed gut` rules get formally re-reviewed by the appropriate role within 90 days of team growth.

### 2.4 Compliance check

Does this rule touch a regulated area? List which Vietnamese law(s) apply or N/A.

| Domain | VN law to consider | When applicable |
|--------|--------------------|-----------------|
| Personal data, consent, retention | **PDPL 2023** (Personal Data Protection Law, effective 2026-07-01) | Any user PII, cookies, analytics |
| Tax invoices, financial records | **Luật Quản lý Thuế 2019** + **Nghị định 123/2020/NĐ-CP** (e-invoice) | Any invoice, receipt, financial doc |
| Consumer rights, refunds, advertising | **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023** | Any pricing display, refund policy, marketing claim |
| Labor, contractor terms | **Bộ luật Lao động 2019** | Teacher contracts, tutor commission |
| Education sector | **Luật Giáo dục 2019** + **MoET regulations** | Student age limits, teacher qualifications, school licensing |
| Cybersecurity, data localization | **Luật An ninh mạng 2018** (Law 24/2018/QH14) + **Nghị định 53/2022/NĐ-CP** | Data storage location, encryption |
| Electronic transactions, e-signatures | **Luật Giao dịch điện tử 2023** | Online contracts, e-signed agreements |

For each rule, state: **N/A** (no regulated area touched), **Considered** (regulated area touched, no specific obligation triggered), or **Compliant** (regulated area touched + specific obligation cited + how rule satisfies).

International compliance (GDPR, CCPA) is out of scope until the project expands beyond Vietnam — flag in `documents/00-brd/compliance-scope.md` (GAP-150 deliverable).

### 2.5 Review cadence

When does this rule get re-reviewed?

| Cadence | When |
|---------|------|
| **Quarterly** | Default for all business rules. Reviews scheduled in `documents/04-quality/audits/business-correctness/` |
| **Event-driven** | Whenever specified trigger fires: competitor pricing change, regulation amendment, market expansion, ≥5% conversion-rate movement |
| **Annual** | Stable rules with very low drift risk (e.g., MoET-mandated student age limits) |
| **Continuous (A/B)** | Rules under live experiment — must reference experiment ID + concluding-date |

Every rule has at minimum a `Next review: YYYY-MM-DD` date. Rules with `Next review` past today's date are out of compliance with this standard.

---

## 3. What counts as a business rule

In scope (THIS rule applies):

- **Pricing & quotas:** trial duration, tier prices, AI quotas, regenerate limits, rate limits visible to users
- **Time periods:** trial length, refund window, data retention, dispute resolution period, payment grace period
- **Identity & access:** student minimum age, teacher qualification requirements, parent vs student permission boundaries
- **Financial:** late fee %, teacher commission %, transaction fee %, currency rounding rule, invoice numbering format
- **Compliance:** data retention period, PII handling, consent collection, audit-log retention
- **Domain-specific:** attendance policy thresholds, grade calculation rules, class size limits, dispute escalation paths
- **Marketing claims:** advertised conversion rates, promised SLAs, money-back guarantees

Out of scope (NOT business rules — different review process):

- Code style, lint rules, file naming → `skill-conventions.md`, project lint configs
- Tooling versions (Node, Java, Postgres) → `02-architecture/adr/`
- Test framework choice, CI gate thresholds → DevOps/SRE review
- Internal API timeouts that don't affect user-visible behavior → engineering judgment
- Library upgrade decisions → Dependabot + security audit pipeline

The litmus test: **would a non-engineer stakeholder (PM, business owner, legal counsel) reasonably challenge or sign off on this value?** If yes → business rule. If no → engineering decision.

---

## 4. Examples — GOOD vs BAD

### 4.1 BAD — bare constant in code, no documentation

```java
public class TrialConfig {
  public static final int TRIAL_DAYS = 14;  // ❌ no Source, no Rationale, no Reviewer
}
```

`rules.md` entry that mirrors it:
```markdown
### TR-01: Trial duration
- Value: 14 days
```

This rule fails all five §2 attributes. A reviewer cannot tell if 14 days is justified, who blessed it, or when to revisit.

### 4.2 GOOD — documented in rules.md with code reference

```markdown
### TR-01: Trial duration

- **Value:** 14 days (config key: `<backend-product>.trial.duration-days`)
- **Source:** Competitor analysis (Hotmart 7d, Teachable 30d, mid-market 14d) + informed gut (no internal A/B yet)
- **Rationale:** 7d too short — onboarding wizard + first class scheduling avg 5-6d in pilot data; 30d delays revenue + opens churn cliff. 14d = 2× onboarding window + buffer.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-04-29). Legal review N/A — trial mechanics not regulated.
- **Compliance check:** N/A — no PDPL / Consumer Protection trigger (free trial, no commitment, no auto-renewal yet).
- **Review cadence:** Quarterly. **Next review:** 2026-07-29. Event triggers: competitor pricing change, ≥5% trial→paid conversion movement, internal A/B concludes.
- **Code reference:** `<subscription-service>/src/main/java/com/kite/hub/subscription/config/TrialConfig.java`
- **A/B test:** No (planned Q3 2026 — see roadmap)
```

Every §2 attribute is satisfied. The rule is reviewable, traceable, and on a clock.

### 4.3 GOOD — compliance-driven rule

```markdown
### DR-03: Personal data retention period

- **Value:** 36 months after account deletion (config key: `<backend-product>.privacy.retention-months`)
- **Source:** PDPL 2023 Art 23 (minimum 24 months for service-related personal data) + Consumer Protection Law dispute window 24mo → ship 36mo for safety margin
- **Rationale:** PDPL minimum 24mo. Consumer disputes can be filed within 24mo of last transaction. 36mo covers both + buffer for late-arriving disputes. Storage cost negligible vs legal exposure.
- **Reviewer:** @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-04-29). Formal legal counsel review queued — GAP-156 acceptance criteria item.
- **Compliance check:** **Compliant** — PDPL 2023 Art 23 (≥24mo); Consumer Protection Law Art 12 (dispute window 24mo); shipped 36mo.
- **Review cadence:** Annual + event-driven on PDPL amendment. **Next review:** 2027-04-29 OR within 30 days of any PDPL implementing-decree publication.
```

---

## 5. Audit cadence

Three layers of audit run on this rule:

### 5.1 Per-PR (entry gate)

Pre-merge PR review checklist (`output-review-mandate.md` §6.2):

- [ ] **Business rule changes documented** per `business-logic-review.md` (5 attributes present in rules.md diff)

A PR that introduces a new business rule constant (`TRIAL_DAYS`, `MAX_REGENS_PER_DAY`, `LATE_FEE_PCT`, etc.) without the matching `rules.md` entry — or with an entry missing any of the 5 attributes — fails this gate.

### 5.2 Quarterly batch audit

Run `quality-audit` skill category "Business Logic Correctness" once per quarter against all per-domain `rules.md` files. Output report saved to `documents/04-quality/audits/business-correctness/YYYY-Q#.md`. Tracks:

- % of rules with all 5 attributes present (target: 100% over time, baseline TBD by GAP-156)
- % of rules past `Next review` date (target: 0%)
- New rules added in quarter — were they reviewed at entry?
- Rules with `informed gut` Source — re-review backlog

GAP-156 schedules the first quarterly audit (Q3 2026) and surfaces the baseline gap.

### 5.3 Event-driven re-review

Triggers immediate re-review of one or more rules:

| Trigger | Rules affected | Owner |
|---------|----------------|-------|
| New VN regulation published | All rules with Compliance check touching that area | Legal scout |
| Competitor public pricing change | Tier/quota/pricing rules | Product Owner |
| Internal A/B test concludes | Rule under experiment | Product Owner |
| Conversion rate moves ≥5% MoM | Trial mechanics, pricing, quotas | Product Owner + Data |
| Customer complaint/dispute pattern | Refund, dispute, fee rules | Product + Legal |

Event-driven re-reviews are tracked in the rule's Log section (Source change, Reviewer change, etc.).

---

## 6. Enforcement

### 6.1 PR template checkbox (lands same PR as this rule — `.github/pull_request_template.md`)

Already exists per `output-review-mandate.md` §6.2 ("Business docs — updated if logic changed"). This rule extends it: docs update must include all 5 §2 attributes. PR template line is updated in the same commit as this rule:

> - [ ] **Business rule changes** — if PR touches `documents/01-business/*/rules.md` OR adds/changes a business-value constant in code, the rules.md entry has all 5 attributes per `.claude/rules/business-logic-review.md` §2 (Source, Rationale, Reviewer, Compliance check, Review cadence)

Solo-dev exemption clause (`§2.3`) applies — but the Reviewer line must explicitly say which role is being worn + queue formal review via GAP-156.

### 6.2 `audit-gate.py` partial detector (PARTIAL — full detector tracked in GAP-156)

This rule ships with a **partial** detector to satisfy `rule-change-process.md` §6.5 Enforcement Parity Mandate. The partial detector runs in `audit-gate.py` AUDIT_RULES on diffs touching `documents/01-business/**/rules.md` or matching business-value constant patterns in `*.java`/`*.ts` (`*_DAYS`, `*_LIMIT`, `*_QUOTA`, `*_FEE_PCT`, `*_PERIOD`, `MAX_*`, `MIN_*`).

Detector behavior in this PR:
- **Detects** added/changed business-rule lines in `rules.md` files
- **Warns** if any of the 5 attributes (Source / Rationale / Reviewer / Compliance / Review cadence) appears missing in the changed lines
- **Does NOT block** — solo-dev mode tolerates iteration; warnings surface in PR description

Full block-mode detector (with regex per-attribute + override trailer) deferred to GAP-156 once baseline pass-rate is known. Until then, the warn-mode detector + PR template checkbox + manual reviewer step cover enforcement parity.

**Override trailer** (when block-mode lands):
```
git commit -m "...
BUSINESS_RULE_OVERRIDE: <reason and link to GAP-XXX scheduled review>"
```

### 6.3 Quarterly audit cadence (PARTIAL — first run scheduled by GAP-156)

`quality-audit` skill category "Business Logic Correctness" runs quarterly. First baseline run = GAP-156 acceptance criterion. Output: `documents/04-quality/audits/business-correctness/2026-Q3.md`.

### 6.4 Reviewer-checklist line

Every PR reviewer (or self-reviewer in solo-dev mode) confirms before merge:

> Did this PR change any business value (config constant, rules.md entry, pricing, quota, period)? If yes — are all 5 §2 attributes present, AND does the Reviewer field document the role being worn?

This line covers cases the regex-based detector misses (e.g., business value buried in a YAML config not matching the constant-name patterns).

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Hardcode `private static final int TRIAL_DAYS = 14;` with no rules.md entry | Pair every business-value constant with a rules.md entry containing all 5 attributes |
| Write "Same as competitor" without naming which competitor or data point | Cite specific competitor + public source, OR call it `informed gut` and queue re-review |
| Self-approve a business rule (Reviewer = author with no role declaration) | Document which role is worn ("acting Product Owner, solo-dev") + queue formal review |
| Mark Compliance check `N/A` without stating *why* it's N/A | Briefly state which regulated area was checked and why no obligation triggered |
| Set `Review cadence: never` or omit `Next review` date | Default cadence is Quarterly. Stable rules can document `Annual` with rationale |
| Bury business-value changes inside engineering refactor PRs | Business rule changes warrant their own PR + visible diff in rules.md |
| Reuse a rule across domains without checking applicability | Each per-domain `rules.md` declares its own values — copying across creates silent drift |
| Treat rules.md as documentation-after-the-fact | rules.md is SOURCE OF TRUTH per CLAUDE.md §Business Logic Documents 3-Layer — code follows it, not the reverse |

---

## 8. Override mechanism

For genuine emergencies (production incident driving immediate rule tweak, regulator-mandated change with same-day deadline) where the standard 5-attribute review cannot complete in-PR:

```
git commit -m "fix(billing): emergency adjust late-fee % per regulator notice 2026-XX-XX

BUSINESS_RULE_OVERRIDE: Regulatory deadline 2026-XX-XX same-day; full 5-attribute review queued in GAP-XXX (target: 2026-XX-XX+7days)"
```

Trailer requirements:
1. Reason cited (incident, regulator, etc.)
2. Follow-up gap link with concrete review-completion date
3. Quarterly retro reviews override frequency — patterns of overrides trigger meta-review of this rule

Override does NOT exempt the rule from the 5 attributes long-term. It exempts only the *timing* of full review. The follow-up gap MUST close within the named window.

---

## 9. Relationship to other rules

- **`output-review-mandate.md`** §3 — this rule fills the matrix-row "Business logic CORRECTNESS" (row flipped from ❌ VIOLATION (GAP-049) → ⚠️ PARTIAL — rule shipped 2026-04-29; audit + sign-offs → GAP-156). This rule is the **standard**; quarterly audits are the **process**; signed-off rules.md entries are the **evidence**.
- **`rule-change-process.md`** — meta-process for editing files under `.claude/rules/`. Separate scope: that rule governs the meta-governance docs themselves; *this* rule governs the business values inside `documents/01-business/`. Both rules use 5-attribute frontmatter, but for different artifact classes.
- **`meta-gap-priority.md`** §3 — Business-Logic tier (priority 2nd, between Meta and Feature). Gaps that touch `documents/01-business/*/rules.md` or `documents/00-brd/**` get Business-Logic-tier ordering. This rule is the standard those gaps measure against.
- **`audit-to-gap-pipeline.md`** — quarterly business-rules audit produces findings; findings flow through that pipeline (Issue → Gap Check → Gap File → Memory → Fix PR). Don't fix audit findings directly in the audit PR.
- **`gap-done-discipline.md`** — when closing GAP-049 (this rule's parent), Phase 1 closure must follow §3 PARTIAL exit ramp because Phase 2 (audit + sign-offs) hasn't shipped. GAP-049 stays 🟡 PARTIAL until GAP-156 lands.
- **`incident-to-rule-pipeline.md`** — if a production incident reveals a business rule was wrong (e.g., trial too short caused mass churn), the incident → rule pipeline applies; the *fix* lands here as a rules.md entry change with full 5-attribute review.
- **CLAUDE.md §Business Logic Documents 3-Layer** — that section establishes rules.md/use-cases.md/api-contract.md as the 3-file structure per domain. This rule defines the *content quality bar* for the rules.md layer specifically.

---

## 10. Log

- **2026-04-29 (v1.0.0):** Rule created. Phase 1 of GAP-049 closure (rule file shipped + `output-review-mandate.md` §3 matrix-row flip from ❌ VIOLATION → ⚠️ PARTIAL). Phase 2 (audit execution against existing 45 per-domain `rules.md` files + stakeholder sign-offs against representative sample) tracked in GAP-156. Reviewer: @nguyenvankiet (solo-dev, acting Product Owner + Legal scout) self-approve per `rule-change-process.md` §5 — new rule with built-in enforcement (PR template checkbox + `audit-gate.py` partial warn-mode detector + quarterly cadence schedule + reviewer-checklist line); no constraint loosening for prior work. Existing rules in `documents/01-business/` are grandfathered (no retroactive 5-attribute audit until GAP-156 quarterly run). Self-approve permitted because §3 review-process VIOLATION existed without standard at all — this rule establishes the standard solo-dev would otherwise route through. Motivation: `output-review-mandate.md` §4 VIOLATION row "Business logic CORRECTNESS" was the last unaddressed CRITICAL violation; GAP-049 sliced into Phase 1 (this rule, Wave Business Correctness 2026-04-29) + Phase 2 (audit + sign-offs, GAP-156) per `gap-done-discipline.md` §3 PARTIAL exit ramp.
