# Data Protection Impact Assessment (DPIA) — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — DPIA framework + processing inventory skeleton + risk matrix template; Phase 2 full risk assessment per processing activity backfilled at 50k subscriber threshold trigger)
**Owner:** [`@nguyenvankiet` acting DPO](dpo-designation.md) (solo-dev, 2026-05-06)
**Reviewer:** Legal counsel với VN PDPL expertise — queued [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md) (Phase 2)
**Last-Updated:** 2026-05-06
**Tracking:** GAP-353d (Wave 26 Bucket B, PDPL Phase 2 close-out) → 50k-subscriber-trigger event-driven full backfill
**Legal basis:** **Nghị định 13/2023/NĐ-CP** Điều 24 (DPIA mandate cho high-risk processing); Điều 25 (DPIA contents); Điều 26 (DPIA review cycle); Luật Trẻ em 2016 (heightened DPIA cho child data)
**Languages:** Vietnamese (canonical, this file). English translation Phase 2.
**Cross-cuts:** [`dpo-designation.md`](dpo-designation.md) (DPO oversight); [`mps-a05-registration-check.md`](mps-a05-registration-check.md) (DPIA artifact required for A05 submission); [`privacy-policy.md`](privacy-policy.md) §13 Security Measures; [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md); [`child-protection-policy.md`](child-protection-policy.md) (heightened risk for K-12 minor data)

---

## 1. Purpose + Scope

### 1.1 Purpose

Document Data Protection Impact Assessment (DPIA) framework cho KiteHub/KiteClass platform per **Nghị định 13/2023/NĐ-CP Điều 24-26**. DPIA mandatory cho:

- High-risk processing (large-scale, systematic monitoring, sensitive PII categories)
- Processing of child data (Luật Trẻ em 2016 + PDPL Art 16)
- Cross-border data transfer
- New processing activities khi added to platform
- Processing crossing 100k subjects threshold (sensitive-data-at-scale per PDPL Art 28)

### 1.2 Scope (this document)

Phase 1 SKELETON — establishes:
- §2 Processing inventory (skeleton table 6+ rows: account / payment / consent / DSAR / child protection / audit logs)
- §3 Risk assessment template (5×5 probability × impact matrix + mitigation controls inventory placeholder)
- §4 High-risk activities placeholder list + inclusion criteria
- §5 Annual review cadence + event-driven triggers

**Phase 2 backfill triggers** (per [GAP-353d](../04-quality/gaps/GAP-353d-dpia-decree-13-art-24-30-docs.md) §"Why P2"):
- Crossing 50k subscribers (50% of 100k threshold) → bump to P1, full risk assessment per processing activity
- Crossing 90k subscribers (90% threshold) → bump to P0 legal hard-deadline
- K-12 onboarding Phase 3 → child-data DPIA mandatory before launch
- Decree 13/2023 amendment introducing additional DPIA requirements

### 1.3 Out of scope (this document)

- Per-activity full risk scoring (deferred Phase 2 50k-trigger)
- Specific mitigation control evidence files (cross-link future runbooks)
- A05 registration submission (separate doc [`mps-a05-registration-check.md`](mps-a05-registration-check.md))
- Detailed technical security controls (cross-link [`privacy-policy.md`](privacy-policy.md) §13)

---

## 2. Processing Inventory (Skeleton)

Inventory of personal data processing activities. Each row = 1 distinct processing activity. Phase 1 SKELETON populates 6 high-priority activities; Phase 2 backfill expands to full inventory upon 50k subscriber trigger.

| # | Processing activity | Data category | Purpose | Legal basis (PDPL Art 11/13) | Retention | Cross-border? | Sensitive? |
|---|---|---|---|---|---|---|---|
| 1 | **Account management** (signup / login / profile) | Identity (name, email, phone) + Auth credentials (hashed password, MFA tokens) | Provide platform access; authentication | Performance of contract (Art 11.1.b) | Account lifetime + 36mo retention per DR-03 | ❌ No | ⚠️ Auth credentials hashed |
| 2 | **Payment processing** | Identity + Financial (card last 4, transaction history, invoice records) | Process subscription payments; tax compliance (TT 78/2021/TT-BTC e-invoice) | Performance of contract (Art 11.1.b) + Legal obligation (Art 11.1.c) | Active subscription + 5 years (TCT mandate) | ⚠️ Payment processor (Stripe/VNPay) — DPA required | ⚠️ Financial data |
| 3 | **Consent records** (cookie consent, marketing opt-in) | Consent metadata (timestamp, IP, user agent, consent version) | Compliance demonstration per PDPL Art 11 + cookie banner (per [GAP-353a](../04-quality/gaps/GAP-353-pdpl-cookie-consent-banner-marketing-kits.md) Wave 23) | Compliance with legal obligation (Art 11.1.c) | 36mo per BR-PDPL-CONSENT-003 | ❌ No | ❌ No |
| 4 | **DSAR ticket processing** | Identity + National ID last 4 + DSAR request content | Comply with PDPL Art 14 right to access/rectify/erase | Compliance with legal obligation (Art 11.1.c) | 36mo per [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) | ❌ No | ⚠️ National ID partial |
| 5 | **Child protection** (K-12 student data, parental consent) | Identity (minor) + Parental consent records + Educational performance | Provide K-12 education service per Luật Giáo dục 2019 | Performance of contract (Art 11.1.b) + Parental consent (Art 16) | Active enrollment + 36mo post-graduation | ❌ No | 🔴 **HIGH — child data** |
| 6 | **Audit logs** (access logs, admin actions, payment events) | Identity (user ID, tenant ID) + Action metadata | Security + compliance audit + breach forensics per `logs-format-standard.md` | Legitimate interest (Art 11.1.f) — security obligation per Art 27 | 24mo (sensitive logs) → 7 years (auth + financial per ND-13/2023) | ❌ No | ⚠️ Aggregated PII |

**Phase 2 backfill candidates** (deferred — to be added at 50k subscriber threshold OR per processing activity addition):
- Branding asset generation (AI Branding flow per `ai-branding-guidelines.md`)
- Notification dispatch (email + push)
- Analytics + telemetry
- Backup snapshots + DR
- Vendor sub-processor data sharing (Stripe, VNPay, Cloudflare, etc.)
- Cross-tenant aggregation (anonymized / pseudonymized)
- Search index data (PostgreSQL full-text)
- Cache layer (Redis — TTL'd PII)

**Cross-link:** [`privacy-policy.md`](privacy-policy.md) §4 Data Categories Matrix and §5 Processing Purposes for canonical mapping.

---

## 3. Risk Assessment Template (5×5 Matrix)

### 3.1 Probability × Impact matrix

| Probability \ Impact | 1 — Negligible | 2 — Minor | 3 — Moderate | 4 — Major | 5 — Severe |
|---|:---:|:---:|:---:|:---:|:---:|
| **5 — Almost certain** | Med | High | High | Critical | Critical |
| **4 — Likely** | Low | Med | High | High | Critical |
| **3 — Possible** | Low | Med | Med | High | High |
| **2 — Unlikely** | Low | Low | Med | Med | High |
| **1 — Rare** | Negligible | Low | Low | Med | Med |

### 3.2 Probability scale

- **5 — Almost certain:** Reasonable to expect this in <1 month
- **4 — Likely:** Expected within 1-6 months
- **3 — Possible:** Expected within 6-18 months
- **2 — Unlikely:** Possible but not expected within 2 years
- **1 — Rare:** Very unlikely to occur (<5% probability over 2-year horizon)

### 3.3 Impact scale

- **5 — Severe:** Massive PII breach (>10k subjects); regulatory action; criminal liability; business existential
- **4 — Major:** Significant breach (>1k subjects); regulatory fine; reputational damage major
- **3 — Moderate:** Localized breach (>100 subjects); regulator notification required; operational disruption
- **2 — Minor:** Single-tenant data exposure; internal incident; mitigatable
- **1 — Negligible:** Near-miss; no PII exposure; pure operational disruption

### 3.4 Residual risk decision rules

- **Critical (red):** UNACCEPTABLE — must reduce via additional controls before activity proceeds
- **High (orange):** Requires DPO sign-off + legal counsel co-sign for residual acceptance
- **Medium (yellow):** Acceptable with DPO sign-off + documented compensating controls
- **Low (green):** Acceptable; ongoing monitoring sufficient
- **Negligible (white):** Acceptable; periodic review only

### 3.5 Mitigation Controls Inventory (Placeholder)

Phase 1 SKELETON list of control families. Phase 2 backfill maps specific controls to specific risks per activity.

| Control family | Examples | Cross-link |
|---|---|---|
| **Encryption** | TLS 1.3 in transit; AES-256 at rest; PostgreSQL TDE; MinIO SSE | [`privacy-policy.md`](privacy-policy.md) §13 |
| **Access Control** | RBAC (admin/teacher/parent/student/accountant); least privilege; multi-tenant DB isolation | [`privacy-policy.md`](privacy-policy.md) §13 |
| **Audit Logging** | Sensitive access logged; 24mo retention; tamper-evident | `.claude/rules/logs-format-standard.md` |
| **Authentication** | MFA available; password hashing (Argon2/bcrypt); session timeout | TBD security audit follow-up |
| **Data Minimization** | Collect only required fields; pseudonymization for analytics | [`privacy-policy.md`](privacy-policy.md) §4 |
| **Retention enforcement** | Automated deletion at retention expiry; deletion runbook | [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) |
| **Backup + Recovery** | Encrypted backups; periodic restore drills; DR runbook | TBD DR/Backup wave |
| **Vendor management** | DPA per processor; annual review; incident notification | [`privacy-policy.md`](privacy-policy.md) §7 |
| **Training** | Annual privacy + security training for staff | [`dpo-designation.md`](dpo-designation.md) §3.5 |
| **Incident response** | 24h DPO escalation; 72h regulator notification; tabletop drills | [`privacy-policy.md`](privacy-policy.md) §14 |
| **Consent management** | Explicit opt-in cookie banner; per-purpose granularity; withdrawal | Per Wave 23 consent banner + Wave 25 server consent API |
| **Child data heightened controls** | Parental consent verification; reduced data collection; safeguarding escalation | [`child-protection-policy.md`](child-protection-policy.md) |

---

## 4. High-Risk Activities — Inclusion Criteria + Placeholder List

### 4.1 Inclusion criteria (per Decree 13/2023 Art 24)

A processing activity is HIGH-RISK and requires DPIA when ANY of:

1. **Sensitive PII categories** processed (health, biometric, child data, financial, location, sexual orientation, religious belief)
2. **Large-scale processing** (≥100k subjects OR ≥10% of relevant population)
3. **Systematic monitoring** (continuous surveillance, behavioral profiling)
4. **Automated decision-making** with legal/significant effect (AI-driven decisions)
5. **Innovative technology** (AI/ML on PII, blockchain, biometric matching)
6. **Vulnerable subjects** (children, elderly, employees in power-imbalance)
7. **Cross-border transfer** to non-adequacy jurisdictions
8. **Combining datasets** that wouldn't be linked otherwise
9. **Public-facing data publication**

### 4.2 Placeholder high-risk activities list (Phase 1)

Phase 1 identifies 4 activities meeting HIGH-RISK criteria; Phase 2 conducts full DPIA per activity.

| # | Activity | High-risk criteria triggered | Phase 2 DPIA owner |
|---|---|---|---|
| 1 | **Child K-12 student data processing** | (1) Sensitive PII child data; (6) Vulnerable subjects | DPO + counsel (mandatory before Phase 3 K-12 launch) |
| 2 | **Payment + financial data processing** | (1) Sensitive PII financial; (7) Vendor cross-border (Stripe/VNPay) | DPO + Tax advisor |
| 3 | **AI Branding asset generation** | (4) Automated decision-making; (5) Innovative AI tech; (1) Brand IP processing | DPO + AI/ML lead |
| 4 | **Aggregated tenant analytics** (Phase 2+ feature) | (8) Combining datasets; potential re-identification risk | DPO + Data lead |

### 4.3 Activities NOT high-risk (Phase 1 assessment)

Account management, basic auth, audit logs (per §2 inventory) are routine processing — DPO oversight via standard cadence sufficient; no full DPIA per activity required at MVP scale.

**Caveat:** scaling factor — when crossing 100k subjects, Activity 1-4 above (and possibly account management) become full-DPIA-required regardless of routine status.

---

## 5. Review Cadence + Event-Driven Triggers

### 5.1 Annual review

DPO conducts full DPIA review annually:
- Verify processing inventory accuracy
- Re-score residual risks for high-risk activities
- Update mitigation controls inventory
- Identify new processing activities added during year
- Sign-off + log to `documents/04-quality/audits/dpia/YYYY.md` (Phase 2 audit folder)

### 5.2 Event-driven triggers (per Decree 13/2023 Art 26)

Immediate DPIA review fires on:

| Trigger | Affected activities | Owner | SLA |
|---|---|---|---|
| **New processing activity added** | New activity row | DPO + activity owner | Before activity goes live |
| **Existing activity material change** | Affected activity | DPO | Before change deployed |
| **Decree 13/2023 amendment** | All activities | DPO + counsel | Within 30 days of decree publication |
| **Crossing 50k subscribers** (P1 trigger per [GAP-353d](../04-quality/gaps/GAP-353d-dpia-decree-13-art-24-30-docs.md)) | Full inventory backfill | DPO | Within 60 days of crossing |
| **Crossing 90k subscribers** (P0 hard-deadline) | Pre-A05 registration prep | DPO + counsel | Within 30 days |
| **Crossing 100k subscribers** (legal trigger) | A05 registration mandatory | DPO + counsel | Within 30 days per Art 28 |
| **K-12 onboarding Phase 3** | Activity #1 child data full DPIA | DPO + counsel | Before first K-12 tenant |
| **Breach incident ≥ Major impact** | Breach-affected activity | DPO + counsel | Within 30 days of incident closure |
| **Vendor / sub-processor change** | Affected activity | DPO | Before vendor cutover |
| **Regulator request** (A05 audit) | All activities | DPO | Per request SLA |

### 5.3 Sign-off + retention

DPIA sign-off MUST be logged with:
- DPIA version number + date
- DPO signature (digital — git commit + signed `Reviewer: @nguyenvankiet (acting DPO, solo-dev)` per `business-logic-review.md` §2.3)
- Counsel co-sign (Phase 2 +)
- A05 acknowledgment (when applicable per Art 28)

DPIA records retained per [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) — minimum 7 years (regulatory + audit forensics).

---

## 6. Phase 2 Open Items (consolidated)

- [ ] Full risk assessment per processing activity (deferred to 50k subscriber trigger per gap §"Why P2")
- [ ] Specific mitigation control evidence files (link to runbooks)
- [ ] Vendor sub-processor full inventory + DPA registry
- [ ] Cross-border transfer impact assessment (Stripe/VNPay/Cloudflare)
- [ ] Child data full DPIA (mandatory before Phase 3 K-12 launch)
- [ ] AI Branding DPIA (mandatory before AI Branding production rollout per GAP-225 cluster)
- [ ] Annual DPIA audit cadence operationalized (first run 2027-05-06)
- [ ] EN translation
- [ ] A05 acknowledgment process documented (when crossing 100k threshold)

---

## 7. Cross-References

- **DPO oversight:** [`dpo-designation.md`](dpo-designation.md) §3.3 DPIA Oversight scope
- **MPS A05 registration:** [`mps-a05-registration-check.md`](mps-a05-registration-check.md) (DPIA = required artifact for A05 submission)
- **Privacy Policy security:** [`privacy-policy.md`](privacy-policy.md) §13 Security Measures (mitigation control evidence)
- **Data retention:** [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) (per-activity retention rationale)
- **Child protection heightened controls:** [`child-protection-policy.md`](child-protection-policy.md)
- **Compliance scope mapping:** [`compliance-scope.md`](compliance-scope.md) §VN PDPL row + §Luật Trẻ em row
- **Logs standard:** `.claude/rules/logs-format-standard.md` (audit logging mitigation control)
- **Business correctness review:** `.claude/rules/business-logic-review.md` §2 5-attribute mandate (this DPIA satisfies for DPO-designated rules)

---

## 8. Log

- **2026-05-06** (Phase 1 skeleton): Initial DPIA framework + processing inventory skeleton (6 rows) + 5×5 risk matrix template + mitigation controls placeholder + 4 placeholder high-risk activities + cadence/triggers documented. Wave 26 Bucket B closing GAP-353d. Decree 13/2023 Art 24-26 addressed at skeleton level. Solo-dev acting DPO sign-off; Phase 2 counsel review queued GAP-156. Full risk assessment per processing activity deferred to 50k subscriber threshold (P1 trigger). Effective immediately for compliance audit purposes; full operationalization upon Phase 2 sign-off.
