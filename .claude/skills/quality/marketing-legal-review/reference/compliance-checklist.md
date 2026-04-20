# Marketing + Legal Compliance Checklist

Per-jurisdiction checklist + required clauses per doc type. Vietnam is primary (mandatory); GDPR/CAN-SPAM apply extraterritorially.

**Legend:** MUST = legal mandate; SHOULD = strong risk mitigation; NICE = best practice.

---

## PART 1 — Vietnam jurisdiction (PRIMARY)

### 1.1 Nghị định 13/2023 — Personal Data Protection Decree (PDPL)

Effective 2023-07-01. Applies to any processing of Vietnamese citizens' personal data OR processing happening on Vietnamese territory.

| # | Requirement | Doc types affected | Level |
|---|-------------|-------------------|-------|
| VN-PDPL-1 | Explicit opt-in consent (no pre-checked boxes, no consent-by-continuing) | Privacy, consent UI, signup | MUST |
| VN-PDPL-2 | Granular consent per processing purpose | Privacy, consent UI | MUST |
| VN-PDPL-3 | Right to withdraw consent easily | Privacy, settings page | MUST |
| VN-PDPL-4 | Data Protection Officer (DPO) appointed if >10,000 records/yr | Internal, Privacy § | MUST at scale |
| VN-PDPL-5 | 72-hour breach notification to Ministry of Public Security | Internal runbook, Privacy § | MUST |
| VN-PDPL-6 | Cross-border transfer requires prior approval | Privacy, DPA § | MUST if applicable |
| VN-PDPL-7 | Impact assessment before deploying new data-processing feature | Internal, Privacy § | MUST |
| VN-PDPL-8 | Right to access, correct, delete, portability | Privacy, settings | MUST |
| VN-PDPL-9 | Children's data (under 16) — parental consent required | Privacy, signup | MUST if applicable |
| VN-PDPL-10 | Cite decree by number in Privacy Policy | Privacy | MUST |

### 1.2 Luật Quảng cáo 2012 + Nghị định 91/2020 — Advertising Law

| # | Requirement | Doc types affected | Level |
|---|-------------|-------------------|-------|
| VN-ADV-1 | `[QC]` prefix in subject for promotional email/SMS | Email, SMS | MUST |
| VN-ADV-2 | Sender identity (company legal name) in body | Marketing emails | MUST |
| VN-ADV-3 | 1-click opt-out mechanism | Marketing emails/SMS | MUST |
| VN-ADV-4 | No sending to numbers on Do-Not-Call registry | SMS runtime | MUST |
| VN-ADV-5 | Claims must be substantiated (no "tốt nhất Việt Nam" without proof) | Landing, pricing | MUST |
| VN-ADV-6 | Comparative advertising naming competitors — BANNED | Marketing copy | MUST |
| VN-ADV-7 | Health/education claims require certification | Landing, blog | MUST if applicable |
| VN-ADV-8 | Promotional pricing must show regular price | Pricing page | MUST |

### 1.3 Luật Bảo vệ Người tiêu dùng 2023 — Consumer Protection Law

Effective 2024-07-01. Applies when tenant is end-consumer (small schools, individual tutors — NOT large enterprise).

| # | Requirement | Doc types affected | Level |
|---|-------------|-------------------|-------|
| VN-CON-1 | 30-day advance notice for material TOS changes | TOS, notification workflow | MUST |
| VN-CON-2 | Unilateral cancellation right if consumer disagrees with change | TOS | MUST |
| VN-CON-3 | Full refund pro-rated on cancellation for unused service | TOS, refund policy | MUST |
| VN-CON-4 | Clear, Vietnamese-language terms (no English-only TOS for VN users) | TOS, all docs | MUST |
| VN-CON-5 | Dispute resolution — VN courts jurisdiction for VN consumers (arbitration clause unenforceable against consumer) | TOS | MUST |
| VN-CON-6 | Standard-form contract requires explicit acknowledgment per clause category | Signup flow | SHOULD |

### 1.4 Luật Giao dịch Điện tử 2005 — Electronic Transactions

| # | Requirement | Doc types affected | Level |
|---|-------------|-------------------|-------|
| VN-ET-1 | Click-through accepted as valid electronic signature | Signup | MUST met by design |
| VN-ET-2 | High-value contracts (>VND 50M) need certified digital signature | Enterprise DPA | MUST |
| VN-ET-3 | Audit trail of acceptance preserved (timestamp + IP) | Signup, tenant_agreement | MUST |

### 1.5 Luật Giáo dục 2019 — Education Law (sector-specific)

KiteClass serves education — extra sector rules:

| # | Requirement | Doc types affected | Level |
|---|-------------|-------------------|-------|
| VN-EDU-1 | Tuition/fee disclosure transparency | Marketing pricing, TOS | MUST |
| VN-EDU-2 | Student data minimization (only what serves education) | Privacy | MUST |
| VN-EDU-3 | Student records retention per MOET regulation | Privacy, retention policy | MUST |

---

## PART 2 — GDPR (EU jurisdiction, extraterritorial)

Applies if ANY user accessed from EU OR marketing targets EU. Conservative default: apply to all marketing + all privacy docs.

| # | Requirement | Doc types affected | Level |
|---|-------------|-------------------|-------|
| EU-GDPR-1 | Legal basis stated per processing purpose (consent/contract/legitimate interest) | Privacy | MUST |
| EU-GDPR-2 | Right of access (Art. 15) | Privacy, settings | MUST |
| EU-GDPR-3 | Right to erasure (Art. 17) — 30-day response | Privacy, settings | MUST |
| EU-GDPR-4 | Right to data portability (Art. 20) — machine-readable export | Privacy, settings | MUST |
| EU-GDPR-5 | Right to object to processing (Art. 21) | Privacy, settings | MUST |
| EU-GDPR-6 | DPO contact published | Privacy | MUST at scale |
| EU-GDPR-7 | 72-hour breach notification to supervisory authority | Internal runbook | MUST |
| EU-GDPR-8 | Data Protection Impact Assessment (DPIA) for high-risk processing | Internal | MUST if applicable |
| EU-GDPR-9 | Cross-border transfer mechanism (SCCs, adequacy decision) | DPA | MUST |
| EU-GDPR-10 | Consent freely given, specific, informed, unambiguous | Privacy, signup | MUST |

---

## PART 3 — United States (CAN-SPAM, CCPA/CPRA)

Applies if sending email to US recipients OR California residents access service.

| # | Requirement | Doc types affected | Level |
|---|-------------|-------------------|-------|
| US-SPAM-1 | Accurate "From" and subject line | Email headers | MUST |
| US-SPAM-2 | Physical postal address in every commercial email | Email footer | MUST |
| US-SPAM-3 | Clear opt-out mechanism + honor within 10 business days | Email, unsubscribe flow | MUST |
| US-SPAM-4 | Identify message as advertisement if promotional | Email header/body | MUST |
| US-CCPA-1 | "Do Not Sell My Personal Information" link (if applicable) | Privacy, footer | MUST if CA users |
| US-CCPA-2 | Right to know + delete + opt-out of sale | Privacy | MUST if CA users |

---

## PART 4 — Required clauses per doc type

### Terms of Service (TOS)

| Clause | Required? | Notes |
|--------|:---------:|-------|
| Parties | MUST | "Between KiteClass (Công ty TNHH KiteClass, MSDN ...) and Customer" |
| Service description | MUST | What is KiteClass, what's included |
| Acceptance | MUST | Click-through binding |
| User obligations | MUST | AUP reference |
| Payment terms | MUST | Billing cycle, late fees, refund policy |
| Subscription auto-renewal | MUST | Clear opt-out before renewal date |
| Term + termination | MUST | Notice period, grounds for termination |
| Data ownership | MUST | Customer owns their data; KiteClass processes per DPA |
| IP rights | MUST | KiteClass owns platform; Customer owns content |
| Warranties + disclaimers | MUST | "As is" with Vietnam Consumer Protection Law minima |
| Liability cap | MUST | Cannot waive statutory minima for VN consumers |
| Indemnification | SHOULD | Mutual, capped |
| Governing law + jurisdiction | MUST | Vietnam law; Hanoi/HCMC courts for consumer disputes |
| Dispute resolution | MUST | Negotiation → mediation → court (arbitration only for non-consumer) |
| Modification notice | MUST | 30-day for material, immediate for non-material |
| Force majeure | SHOULD | Pandemic, war, government action |
| Severability | MUST | Invalid clause doesn't void rest |
| Entire agreement | MUST | This TOS + DPA + SLA supersede prior |

### Privacy Policy

| Clause | Required? | Notes |
|--------|:---------:|-------|
| Controller identity | MUST | KiteClass full legal name + address + contact |
| DPO contact | MUST at scale | Name or role + email |
| Data categories collected | MUST | PII, usage, analytics, logs — specific list |
| Purposes of processing | MUST | Service delivery, billing, support, analytics (each with legal basis) |
| Legal basis per purpose | MUST (GDPR) | Consent / contract / legitimate interest / legal obligation |
| Data sources | MUST | Direct from user, cookies, third-parties |
| Retention period | MUST | Specific (e.g., "30 days post-termination") |
| Sharing + recipients | MUST | Sub-processors named + SCCs linked |
| Cross-border transfers | MUST | Destinations + safeguards (SCCs, adequacy) |
| User rights | MUST | Access, correct, delete, portability, object, withdraw consent |
| Rights exercise channel | MUST | Email + self-service + response SLA |
| Cookies + tracking | MUST | Link to Cookie Policy + banner mechanism |
| Children's data | MUST if applicable | Parental consent mechanism |
| Automated decision-making | MUST if applicable | Explanation + opt-out |
| Breach notification commitment | MUST | 72-hour to authority + affected users |
| Changes notification | MUST | How and when |
| Decree/law references | MUST | Cite Nghị định 13/2023 by number; GDPR articles |

### DPA (for Enterprise / B2B customers)

| Clause | Required? |
|--------|:---------:|
| Subject matter + duration | MUST |
| Nature + purpose of processing | MUST |
| Type of personal data + categories of data subjects | MUST |
| Controller (Customer) obligations | MUST |
| Processor (KiteClass) obligations | MUST |
| Sub-processor list + notification for changes | MUST |
| Technical + organizational measures (Annex II) | MUST |
| Data subject rights handling | MUST |
| Audit rights | MUST |
| Cross-border transfer mechanisms (SCCs 2021) | MUST if applicable |
| Breach notification timing + contents | MUST |
| Return/deletion at end of service | MUST |
| Liability + indemnification | MUST |

### Cookie Policy

| Clause | Required? |
|--------|:---------:|
| Cookie categories (necessary, functional, analytics, marketing) | MUST |
| Per-category list of actual cookies + purposes + duration | MUST |
| Third-party cookies disclosed (GA, ad platforms) | MUST |
| How to opt-out per category | MUST |
| Consent banner reference | MUST |

### SLA (Service Level Agreement)

| Clause | Required? |
|--------|:---------:|
| Uptime target (e.g., 99.5%) | MUST |
| Measurement methodology | MUST |
| Exclusions (scheduled maintenance, force majeure) | MUST |
| Service credits formula | SHOULD |
| Claim process | MUST |
| Reporting cadence | SHOULD |

### Acceptable Use Policy (AUP)

| Clause | Required? |
|--------|:---------:|
| Prohibited content (illegal, harmful, IP infringement) | MUST |
| Prohibited activities (spam, scraping, reverse eng) | MUST |
| Enforcement mechanisms (warning, suspension, termination) | MUST |
| Reporting channel | MUST |
| Cooperation with law enforcement | MUST |

---

## PART 5 — Consolidated pre-publish gate

Before publishing any legal doc, verify ALL:

- [ ] Applicable jurisdiction rows above ticked
- [ ] Required clauses per doc type present
- [ ] External counsel sign-off (PDF in `documents/legal/opinions/`)
- [ ] Executive signature (PDF in `documents/legal/signed/`)
- [ ] Version + effective date in frontmatter
- [ ] Previous version pointer + archive path
- [ ] Tenant notification plan + effective date set ≥30 days from publish
- [ ] Per-tenant migration plan for material changes
- [ ] Monitoring: error rate on consent page, support ticket volume for questions

---

## Log

- 2026-04-20 — Initial version, skill created by Wave 8b Agent C (GAP-174). Counsel engagement not yet complete — this checklist represents compliance research, NOT legal advice. Engage retained counsel before pre-GA to validate.
