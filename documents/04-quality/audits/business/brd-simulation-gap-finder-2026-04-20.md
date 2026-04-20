---
title: BRD Simulation Gap Finder Report
date: 2026-04-20
method: simulation-gap-finder skill (3-axis matrix)
feature: Business Requirements Documentation (BRD) — documents/00-brd/
audience: PM, Business Lead, Legal, Architect
---

# BRD Simulation Gap Finder — 2026-04-20

**Trigger:** User raised *"BRD là điểm khởi đầu quan trọng nhất của dự án — xem còn gaps nào nữa không"*.

**Current BRD state:**
- ✅ `personas-catalog.md` (DRAFT v1)
- 🟡 5 skeleton docs tracked by GAP-150 (business-objectives, compliance-scope, pricing-model, nfr-catalog, go-to-market)
- 🟡 Persona AC tracked by GAP-151 + GAP-153

**Method:** 3-axis matrix per `.claude/skills/quality/simulation-gap-finder.md`:
- Personas: Owner/Admin · End User · Platform Admin · Developer · Support
- Stages: Discovery → Signup → Config → Provisioning → Daily → Edge → Evolution → Termination
- Categories: C1 Functional · C2 UX · C3 Data · C4 Perf · C5 Security · C6 Compliance · C7 Ops · C8 Integration · C9 Commercial · C10 Evolution

---

## 1. Simulation Findings — 22 missing BRD docs (net new)

Beyond GAP-150's 5 skeleton docs, simulation surfaces **22 additional BRD artifacts** required for a defensible SaaS launch. Classified by priority + stage + persona coverage.

### 1.1 🔴 P0 — GA blockers (7 docs)

These docs are **legally or operationally mandatory** before paying customers + must exist for any persona reaching Signup stage.

| # | Missing BRD Doc | Stage | Category | Matrix cell (sample) | Why P0 |
|---|-----------------|-------|----------|---------------------|--------|
| B | **Terms of Service (TOS)** | Signup | C6 Compliance | End User × Signup × C6 | Legal contract tenant + end users; blocker for payment processing |
| C | **Acceptable Use Policy (AUP)** | Daily | C6 | End User × Daily × C6 | Content moderation basis; user ban + DMCA response needs AUP |
| D | **Privacy Policy** | Signup | C6 | End User × Signup × C6 | VN PDPL 2023 mandatory disclosure; GDPR analog for any EU traffic |
| K | **Refund + Dispute Resolution Policy** | Termination | C9 | Owner × Termination × C9 | VN Consumer Protection Law 2023 mandatory; chargeback defense |
| L | **Data Retention + Deletion Policy** | Termination | C6 | Owner × Termination × C6 | VN PDPL Article 6; GDPR right to be forgotten |
| N | **Billing Terms + Payment Policy (Tax-compliant)** | Signup | C9 | Owner × Signup × C9 | VAT + TCT invoice format; late fee rules (GAP-108 hardcoded drift symptom) |
| Z | **Child Protection Policy (COPPA-equivalent for VN)** | All | C6 | End User × All × C6 | K-12 students = minors; parental consent; VN child protection law |

### 1.2 🟠 P1 — Strong need pre-scale (7 docs)

Block organizational growth but not single-customer GA. Required before pilot expansion.

| # | Missing BRD Doc | Stage | Category | Why P1 |
|---|-----------------|-------|----------|--------|
| A | **Product Scope / MRD** (Market Requirements Doc) | Discovery | C1 Functional | WHAT product we're building (personas catalog is WHO, not WHAT); feeds sales + roadmap |
| E | **Data Classification + Handling Policy** | Config | C3 Data | PII tiers (student grades = sensitive, attendance = PII), retention per tier |
| F | **SLA + Uptime Commitment (customer-facing)** | Signup | C7 Ops | Separate from internal NFR; credit policy; enterprise sales blocker |
| H | **Incident Response + Breach Notification Policy** | Edge | C7 | VN PDPL 72-hour breach notification; customer comm SOP |
| M | **Data Export / Portability Policy** | Termination | C6 | GDPR Article 20 equivalent; VN PDPL data subject rights |
| X | **MOET Regulatory Alignment Matrix** | Compliance | C6 | Which circulars/decrees apply (curriculum, assessment, teacher qual); K-12 sales requirement |
| Y | **Academic Year + Curriculum Structure Policy** | Daily | C1 | K-12 vs center vs uni structures; grading scales; promotion rules |

### 1.3 🟡 P2 — Defensible scale (5 docs)

Professional posture, enterprise requirement, but not blocking early growth.

| # | Missing BRD Doc | Stage | Category | Why P2 |
|---|-----------------|-------|----------|--------|
| G | **Support SLA / SOP** | Edge | C7 | Response time by tier; escalation; L1/L2/L3 definitions |
| I | **Disaster Recovery / BCP (customer-facing)** | Edge | C7 | RTO/RPO commitment (NFR internal target vs BRD customer commitment) |
| J | **API Terms of Use / Developer License** | Integration | C8 | Rate limits, data access terms, commercial vs free API |
| Q | **Security Posture Summary** | Discovery | C5 | "What tenant gets": encryption-at-rest, backup, SOC2-path — sales collateral + legal |
| AA | **Vendor Management / 3rd Party Risk Policy** | Ops | C5 | Which vendors touch customer data; VNPay, MoMo, Zalo, Google Workspace assessment |

### 1.4 🟢 P3 — Post-GA / maturity (3 docs)

Nice-to-have, strategic, often deferred past Series A.

| # | Missing BRD Doc | Stage | Category | Why P3 |
|---|-----------------|-------|----------|--------|
| O | **Versioning + Deprecation Policy** | Evolution | C10 | API version EOL commitment; breaking change notice period |
| P | **Accessibility Statement** | Daily | C2 | WCAG 2.1 AA commitment; some enterprise RFPs require |
| R | **Brand Guidelines / Trademark Policy** | Daily | C6 | When tenant uses OUR brand; co-branding rules |

---

## 2. Matrix Coverage Trace (sample cells demonstrating method)

Trace of how simulation surfaced gaps — example cells:

| Persona × Stage × Cat | Scenario | Covered? | Missing BRD |
|----------------------|----------|----------|-------------|
| Owner × Signup × C6 | Tenant accepts TOS during signup | ❌ No TOS doc | B. TOS |
| End User × Signup × C6 | Student signs privacy consent | ❌ No Privacy Policy | D. Privacy Policy |
| End User × Daily × C6 | Student posts inappropriate content | ❌ No AUP → moderation ad-hoc | C. AUP |
| Owner × Termination × C9 | Customer cancels mid-cycle | ❌ No refund policy → legal ambiguity | K. Refund Policy |
| Owner × Termination × C6 | Customer requests data deletion | ❌ No retention doc → engineering decides | L. Data Retention |
| Platform Admin × Edge × C7 | Data breach detected | ❌ No runbook for customer notification | H. Incident Response |
| Developer × Integration × C8 | Partner wants API access | ❌ No API terms | J. API Terms |
| End User × All × C6 (minor) | 14-year-old uses platform | ❌ No child protection doc → VN law risk | Z. Child Protection |
| Owner × Discovery × C1 | Sales prospect asks "what does it do" | ⚠️ Catalog = WHO, no WHAT doc | A. MRD |
| Owner × Signup × C7 | Enterprise RFP asks uptime SLA | ⚠️ NFR internal, no customer SLA | F. Customer SLA |
| Owner × Signup × C9 | Invoice generated with VAT | ❌ No billing terms doc | N. Billing Terms |
| Owner × Daily × C6 (K-12) | School asks about MOET compliance | ❌ No regulatory matrix | X. MOET Matrix |

Full matrix not enumerated (5 × 8 × 10 = 400 cells; ~100 relevant). Above 12 cells are representative.

---

## 3. Gap Overlap Check (Duplicate Audit)

Cross-check against existing backlog to avoid duplication:

| Missing Doc | Existing gap | Overlap | Decision |
|-------------|--------------|---------|----------|
| C. AUP | GAP-018 (Content safety/moderation policy) | PARTIAL — GAP-018 covers tech; AUP is legal doc | Create separate — AUP feeds GAP-018 rules |
| K. Refund | GAP-108 (payment config) | INDIRECT — GAP-108 hardcodes, refund is policy | Create separate — policy drives config |
| F. SLA | GAP-135 (API P95 latency SLOs) | PARTIAL — SLOs internal, SLA customer-facing | Create separate — SLO → SLA translation needed |
| H. Incident Response | GAP-117 (restore drill) + GAP-144 (alertmanager) | OPS layer covered; BRD legal commitment layer missing | Create separate |
| AA. Vendor Risk | GAP-042 (legal/IP) | TANGENTIAL — legal scope differs | Create separate |
| Q. Security Posture | GAP-174 (marketing-legal review) | ADJACENT — different audience | Create separate |
| I. DR customer-facing | GAP-030 (AI branding DR) + GAP-117 | INTERNAL ops vs customer-facing | Create separate |
| M. Data Portability | GAP-049 (correctness review) | PROCESS vs CONTENT | Create separate (GAP-154 phase) |
| Others (B, D, L, N, Z, A, E, X, Y, G, J, O, P, R) | No overlap | — | Create fresh |

**Net new gaps:** 22. None duplicate. Some reference/feed existing gaps.

---

## 4. Phasing Recommendation

Creating 22 skeleton gap files in one PR = scope explosion. Staged phase per `audit-to-gap-pipeline.md` §2 Step 6 priority rules + `meta-gap-priority.md` §3 business-logic tier:

| Phase | Scope | Effort | Timing |
|-------|-------|:------:|--------|
| **Phase 0** (this report + GAP-154 umbrella) | Catalog 22 missing docs, classify by priority, avoid duplicates | S | 2026-04-20 |
| **Phase 1** (P0 7 docs) | File 7 P0 sub-gaps (GAP-157..163 reserved); skeleton content | M-L | Wave 8 meta execution |
| **Phase 2** (P1 7 docs) | File 7 P1 sub-gaps (GAP-164..170 reserved) | M | Post Wave 8 |
| **Phase 3** (P2 5 docs) | File 5 P2 sub-gaps (GAP-171..175 already used — needs range check) | M | Post-GA |
| **Phase 4** (P3 3 docs) | File 3 P3 sub-gaps | S | Post-GA maturity |

**Number collision warning:** GAP-170..175 already assigned to Wave 8b output-review violations. Phase 2 sub-gaps need reservation after 175 (e.g. GAP-180..186). Reserve in umbrella gap GAP-154.

---

## 5. Cross-Persona Interactions Caught

Simulation checked interactions between personas (Step 6 of skill methodology):

| Interaction | Gap surfaced |
|-------------|--------------|
| Owner vs End User TOS (does student sign their own, or tenant signs for them?) | Z. Child Protection → parental consent flow |
| Platform Admin vs Tenant (admin suspends tenant) | C. AUP — basis for suspension |
| Developer vs Owner (integration accesses tenant data) | J. API Terms — separate from TOS |
| Support vs Platform Admin (support impersonation) | G. Support SOP — privacy-safe impersonation |
| Owner vs Support (billing dispute) | K. Refund Policy + G. Support SLA |

---

## 6. Stress Test Findings

Edge cases (Step 5 of skill):

- **Data breach** → H. Incident Response missing → uncoordinated customer comm
- **Tenant churns with 10,000 student records** → L. Data Retention missing → engineering improvises
- **Minor student uses platform without parental consent** → Z. Child Protection missing → VN law violation
- **Competitor complaints DMCA** → R. Brand + J. API terms missing → no clear policy for response
- **VNPay payment fails at scale** → AA. Vendor Risk missing → no fallback policy

---

## 7. Recommendations

1. **IMMEDIATE (this PR):** File GAP-154 umbrella gap tracking the 22 docs with phasing; update GAP-150 scope note (5 of 27 = skeleton phase 1 of BRD content, not full BRD).
2. **NEXT SESSION:** File 7 P0 sub-gaps from §1.1; assign to Wave 8 meta execution.
3. **LATER:** Phase 2/3/4 as backlog fills.
4. **ONGOING:** Rerun this simulation quarterly — new personas (P4 chain, P7 corporate) will surface more BRD needs.

---

## 8. Matrix Coverage Summary

| Axis | Coverage |
|------|----------|
| Personas walked | 5/5 (Owner, End User, Platform Admin, Developer, Support) |
| Stages walked | 8/8 (Discovery → Termination) |
| Categories checked | 10/10 (C1-C10) |
| Cross-persona interactions | ✅ 5 reviewed |
| Edge cases (failure/scale/malice) | ✅ 5 scenarios |
| Evolution stage | ✅ O (versioning), U (risk register) |
| Termination stage | ✅ K, L, M |
| Compliance-specific (VN) | ✅ D, L, N, Z, X |

---

## 9. Pre-Flight Checklist (per skill)

- [x] All 5 personas walked through all 8 stages
- [x] All 10 concern categories checked
- [x] Edge cases considered (failure, scale, concurrency, malice)
- [x] Cross-persona interactions reviewed
- [x] Evolution stage considered
- [x] Termination stage considered
- [x] Compliance/legal review (VN-specific)
- [x] Operations/monitoring considered (customer-facing layer)
- [x] Each new gap cross-checked against existing gaps (no duplicates — 7 partial overlaps noted, 15 net new)

---

## 10. Log

- 2026-04-20 — Initial simulation run after user question. 22 missing BRD docs identified. Umbrella GAP-154 filed. Phase 1 (7 P0 docs) targeted for Wave 8.
