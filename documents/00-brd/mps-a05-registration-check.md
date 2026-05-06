# MPS A05 Registration Check + Procedure — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — pre-emptive monitoring procedure + registration intent + collection-of-artifacts checklist; Phase 2 actual registration upon crossing 100k subscriber threshold)
**Owner:** [`@nguyenvankiet` acting DPO](dpo-designation.md) (solo-dev, 2026-05-06)
**Reviewer:** Legal counsel với VN PDPL expertise — queued [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md) (Phase 2)
**Last-Updated:** 2026-05-06
**Tracking:** GAP-353d (Wave 26 Bucket B, PDPL Phase 2 close-out) → 100k subscriber threshold event-driven actual filing
**Legal basis:** **Nghị định 13/2023/NĐ-CP Điều 28(1)** (registration mandate cho processing >100k subjects OR sensitive data at scale); Điều 28(2-4) (registration content + procedure); Luật An ninh mạng 2018 (cybersecurity oversight); Luật Trẻ em 2016 (sensitive child data classification triggering Art 28)
**Languages:** Vietnamese (canonical, this file). English translation Phase 2.
**Cross-cuts:** [`dpo-designation.md`](dpo-designation.md) §3.4 MPS A05 Liaison scope; [`dpia.md`](dpia.md) (DPIA = required submission artifact); [`privacy-policy.md`](privacy-policy.md) §2 + §13 + §14; [`compliance-scope.md`](compliance-scope.md); [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md); [`child-protection-policy.md`](child-protection-policy.md)

---

## 1. Purpose + Trigger Conditions

### 1.1 Purpose

Establish pre-emptive monitoring procedure + registration trigger criteria + artifacts checklist cho **Cục An ninh mạng và Phòng chống tội phạm sử dụng công nghệ cao (A05) — Bộ Công an** registration per **Nghị định 13/2023/NĐ-CP Điều 28(1)**.

### 1.2 Registration trigger criteria (Decree 13/2023 Art 28(1))

A05 registration MANDATORY when ANY of:

1. **>100k subjects** whose PII is processed
2. **Sensitive data at scale** processing — including:
   - Health data
   - Biometric data
   - Child data (Luật Trẻ em 2016 — heightened sensitivity per PDPL Art 16)
   - Financial data
   - Religious belief / political opinion / sexual orientation / criminal records
3. **Cross-border transfer** of PII (per PDPL Art 22)
4. **Automated decision-making** with legal/significant effect

**KiteClass MVP current state (2026-05-06):** Solo-dev mode with <<100k subjects (estimated <100 subjects MVP pre-launch). Registration NOT yet triggered. However:

- **Activity #1 child data** (per [`dpia.md`](dpia.md) §4 high-risk activities) WILL trigger criterion 2 once K-12 onboarding launches Phase 3
- **Activity #2 payment data** triggers criterion 2 (financial sensitive) at any scale per strict reading of Art 28(1)(b) — counsel review needed Phase 2
- **Approaching 100k subscribers** triggers criterion 1 — pre-emptive monitoring (§3 below)

### 1.3 Out of scope (this document)

- Actual A05 form filling (deferred to trigger event; counsel involvement required)
- A05 fee schedule + submission portal mechanics (Phase 2 — verify với A05 portal directly)
- Post-registration audit response procedure (separate runbook upon first audit request)
- Cross-border transfer specific notification (separate procedure per PDPL Art 22 — to be filed if vendor processing PII outside VN)

---

## 2. Registration Intent + Responsible Party

### 2.1 Registration intent

KiteHub/KiteClass platform commits to **proactive A05 registration** within the SLA defined by Decree 13/2023 Art 28(2) upon crossing any trigger criterion in §1.2 above. Pre-emptive registration considered for:

- Crossing 50k subscriber threshold (P1 trigger) — counsel-advised early registration even before mandatory threshold
- K-12 onboarding Phase 3 (child sensitive data trigger regardless of count)
- Payment processing strict reading (counsel review Phase 2 — if Art 28(1)(b) financial-data interpretation requires registration at any scale, register pre-launch)

### 2.2 Responsible party

**Primary responsible party:** [`@nguyenvankiet` acting DPO](dpo-designation.md) per Decree 13/2023 Art 28(1)(c) — DPO designation is the registration submitter.

**Solo-dev acting status (2026-05-06):** Solo-dev wears DPO hat per `business-logic-review.md` §2.3 solo-dev exemption with explicit role declaration. Formal counsel co-submission queued via [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md) Phase 2.

**Phase 2 transition:** Upon crossing 50k subscribers OR K-12 launch trigger:
- Engage formal legal counsel với VN PDPL expertise (per [`dpo-designation.md`](dpo-designation.md) §7 Phase 2 Open Items)
- Counsel co-signs A05 submission
- Establish formal DPO contract (when team grows beyond solo)

### 2.3 Submission channel

- **Primary:** A05 official portal (Phase 2 — verify URL + e-signature requirements directly)
- **Backup:** Postal submission to A05 office (Phase 2 — verify address: Cục An ninh mạng và Phòng chống tội phạm sử dụng công nghệ cao, Bộ Công an, Hà Nội)
- **Contact intermediary:** Legal counsel may act as authorized agent (Phase 2 — power of attorney established)

---

## 3. Pre-Emptive Monitoring Procedure

Subscription growth threshold check + auto-flag mechanism prevents surprise threshold crossing.

### 3.1 Monitoring metric

**Subscriber count** = total active KiteHub subscriptions × per-subscription PII subjects (estimated).

| Subscription tier | Estimated PII subjects per subscription | Notes |
|---|:---:|---|
| FREE / TRIAL | 5-10 | Owner + 1-2 admin + sample students/teachers |
| BASIC | 50-200 | Small tutoring center |
| PREMIUM | 200-1000 | Medium center |
| ENTERPRISE / K-12 | 1000-10000 | School-scale |

**Aggregate subjects estimate:** sum across all active subscriptions = total PII subjects in system.

### 3.2 Threshold flags

| Threshold | % of 100k mandate | Priority bump | Action |
|---|:---:|:---:|---|
| **50k aggregate subjects** | 50% | P2 → P1 | DPO sync với counsel; full DPIA backfill per [`dpia.md`](dpia.md) §5.2; counsel-advised pre-emptive A05 registration consideration |
| **75k aggregate subjects** | 75% | P1 → P1+ | Active A05 form preparation; counsel engaged formally; collect §4 artifacts |
| **90k aggregate subjects** | 90% | P1+ → P0 | Submit A05 registration WITHIN 30 days; do NOT wait for 100k crossing — registration must complete BEFORE crossing per Decree 13/2023 Art 28(2) SLA |
| **100k aggregate subjects** | 100% | LEGAL HARD-DEADLINE | If not yet registered → emergency override per `business-logic-review.md` §8; counsel-led emergency submission within Art 28(2) SLA (TBD per counsel — typically 30 days) |
| **K-12 first tenant onboarding** | N/A (criterion 2 child data) | Special trigger | Submit A05 registration BEFORE first K-12 tenant data ingest; child data triggers regardless of count |

### 3.3 Monitoring implementation

**Phase 1 (current — solo-dev MVP scale):**
- Manual monthly check via subscription dashboard / DB query
- DPO logs current count to `documents/04-quality/audits/dpia/YYYY-Q#.md` (Phase 2 audit folder)
- Threshold cross-check at each wave merge per `post-wave-audit-mandate.md`

**Phase 2 (after first 1k subscribers):**
- Automated counter in subscription service emit metric `subscription.aggregate_subjects.estimated`
- Grafana dashboard alert at 50k / 75k / 90k thresholds
- DPO + on-call notification on threshold crossing
- Cross-link [`dpia.md`](dpia.md) §5.2 event-driven triggers

**Phase 3 (mature):**
- Quarterly external audit verifying counter accuracy
- Subscription analytics correlate với actual data subject count (not just subscription count)

### 3.4 False-positive guard

Aggregate subject count estimate uses per-tier averages; actual count may vary. Pre-emptive registration at 90k = 10% buffer covers estimation error. If actual >100k discovered post-fact (counter error / sudden tenant onboarding spike), emergency procedure:
- DPO + counsel notified within 24h
- Emergency A05 submission within 7 days
- Override trailer logged per `business-logic-review.md` §8

---

## 4. Registration Submission — Required Artifacts Checklist

When trigger fires (per §3.2), submit the following artifacts to A05:

### 4.1 Core artifacts (Decree 13/2023 Art 28(2))

- [ ] **Registration form** (A05 official template — Phase 2 verify current version)
- [ ] **DPO designation document** → [`dpo-designation.md`](dpo-designation.md) (post Phase 2 counsel sign-off)
- [ ] **DPIA full version** → [`dpia.md`](dpia.md) (post 50k full backfill OR per-activity DPIA for child / payment / AI Branding activities)
- [ ] **Processing inventory** → [`dpia.md`](dpia.md) §2 (post Phase 2 full backfill)
- [ ] **Privacy Policy** → [`privacy-policy.md`](privacy-policy.md) (post Phase 2 legal counsel sign-off + EN translation)
- [ ] **Data retention policy** → [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) (post Phase 2 sign-off)
- [ ] **Security measures statement** → [`privacy-policy.md`](privacy-policy.md) §13 + cross-link to security audit reports
- [ ] **Breach notification procedure** → [`privacy-policy.md`](privacy-policy.md) §14 + future incident-response runbook

### 4.2 Supporting artifacts

- [ ] **Business registration document** (GP-ĐKKD / Mã số doanh nghiệp) — Phase 2 legal entity registration prerequisite
- [ ] **Legal representative authorization** (if counsel submits on behalf)
- [ ] **Vendor sub-processor list** với DPA copies → [`privacy-policy.md`](privacy-policy.md) §7 (post Phase 2 vendor inventory complete)
- [ ] **Cross-border transfer assessment** (if applicable per PDPL Art 22) — separate procedure if Stripe/VNPay/Cloudflare processing PII outside VN
- [ ] **Child protection policy** → [`child-protection-policy.md`](child-protection-policy.md) (mandatory if K-12 trigger)
- [ ] **Audit logs sample + retention proof** (per `.claude/rules/logs-format-standard.md`)
- [ ] **Recent security audit report** (per `.claude/skills/quality/security-audit/SKILL.md` quarterly run)

### 4.3 Submission record retention

Post-submission, retain:
- A05 submission acknowledgment (registration number)
- Confirmation of registration acceptance
- Any A05 follow-up requests + responses
- Annual renewal records (if Decree 13/2023 implementing decree mandates renewal cadence)

Retention period: **7 years** (regulatory record per ND-13/2023/NĐ-CP + Luật Lưu trữ).

Storage: Encrypted document store với DPO access only; backup to legal counsel custody Phase 2.

---

## 5. Post-Registration Obligations

Upon successful A05 registration, ongoing obligations:

### 5.1 Annual updates
- Renewed DPIA (per [`dpia.md`](dpia.md) §5.1 annual review)
- Updated processing inventory if material changes
- Any new high-risk activities filed within 30 days of go-live

### 5.2 Material change notification (Art 28 implementing decree TBD per counsel verification)
- New processing activity addition
- Vendor sub-processor change (cross-border transfer)
- Material security incident (linked với breach notification per Art 23)
- Legal entity change

### 5.3 Audit response
- A05 may audit registration accuracy + compliance
- DPO is point of contact; counsel involved per legal-privilege protection
- Audit findings logged + remediation tracked
- Cross-link với `quality-audit` skill output

### 5.4 De-registration
- If platform shuts down OR processing activity discontinued, formal de-registration submission
- Retain registration record for 7 years post-discontinuation

---

## 6. Phase 2 Open Items (consolidated)

- [ ] Verify A05 portal URL + current registration form version (counsel)
- [ ] Confirm registration SLA per Art 28(2) implementing decree (counsel)
- [ ] Engage formal counsel với VN PDPL expertise (per [`dpo-designation.md`](dpo-designation.md) §7)
- [ ] Establish automated subscriber-count monitoring + alerts (Phase 2 infra dependent)
- [ ] Verify payment data strict-reading interpretation of Art 28(1)(b) — pre-launch registration trigger?
- [ ] EN translation of this document
- [ ] First registration submission triggered by EARLIEST of: 50k subscribers / K-12 launch / counsel-advised pre-emptive
- [ ] Annual update cadence operationalized post-first-registration
- [ ] De-registration procedure documented (defer until applicable)

---

## 7. Cross-References

- **DPO scope:** [`dpo-designation.md`](dpo-designation.md) §3.4 MPS A05 Liaison
- **DPIA artifact:** [`dpia.md`](dpia.md) (required submission artifact per §4.1)
- **Privacy Policy:** [`privacy-policy.md`](privacy-policy.md) §2 + §13 + §14
- **Data Retention:** [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md)
- **Child Protection:** [`child-protection-policy.md`](child-protection-policy.md) (heightened trigger per criterion 2)
- **Compliance Scope:** [`compliance-scope.md`](compliance-scope.md) §VN PDPL row
- **Cybersecurity Law:** Luật An ninh mạng 2018 + Decree 53/2022/NĐ-CP (data localization context)
- **Business correctness review:** `.claude/rules/business-logic-review.md` (5-attribute mandate satisfied via DPO designation chain)

---

## 8. Log

- **2026-05-06** (Phase 1 skeleton): Initial procedure + threshold monitoring + artifacts checklist created via Wave 26 Bucket B closing GAP-353d. Decree 13/2023 Art 28(1-4) addressed at skeleton level. Solo-dev acting DPO declared as responsible party per `business-logic-review.md` §2.3 solo-dev exemption + Phase 2 counsel sign-off queued via GAP-156. Pre-emptive monitoring at 50k / 75k / 90k thresholds documented; K-12 special trigger noted (child data sensitive per criterion 2). Effective immediately for compliance audit purposes; actual registration submission deferred to threshold-crossing trigger event.
