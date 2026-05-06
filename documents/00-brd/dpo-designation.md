# Data Protection Officer (DPO) Designation — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — designation skeleton; Phase 2 formal counsel review via [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md))
**Owner:** `@nguyenvankiet` (acting Legal scout + Compliance, solo-dev)
**Reviewer:** Legal counsel với VN PDPL expertise (queued for Phase 3 K-12 launch — GAP-156)
**Last-Updated:** 2026-05-06
**Tracking:** GAP-353d (Wave 26 Bucket B, PDPL Phase 2 close-out) → [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md) (Phase 2 counsel sign-off)
**Legal basis:** **Nghị định 13/2023/NĐ-CP** Điều 27 (DPO mandatory cho tổ chức xử lý dữ liệu nhạy cảm hoặc dữ liệu trẻ em); Điều 28 (DPO functions); Luật An ninh mạng 2018; Luật Trẻ em 2016 Điều 33-37 (child data protection)
**Languages:** Vietnamese (canonical, this file). English translation Phase 2.
**Cross-cuts:** [`privacy-policy.md`](privacy-policy.md) §2 DPO field; [`dpia.md`](dpia.md) (DPO oversees DPIA process); [`mps-a05-registration-check.md`](mps-a05-registration-check.md) (DPO is responsible party for MPS A05 submissions); [`child-protection-policy.md`](child-protection-policy.md) (DPO escalation channel)

---

## 1. Five-attribute frontmatter (per `business-logic-review.md` §2)

- **Source:** Decree 13/2023/NĐ-CP Art 27-28 (DPO mandate for organizations processing sensitive PII OR child data); Luật Trẻ em 2016 (K-12 minor data classified sensitive — Art 6 + Art 33). KiteClass MVP processes child data (Tier 1 K-12 persona P5 deferred Phase 3, but P3 medium-center may onboard schools với student under-16 trong Phase 2). DPO designation pre-MVP launch = good practice + market-ready compliance posture.
- **Rationale:** Pre-launch DPO designation prevents post-launch scramble khi crossing first thresholds (K-12 onboarding / 100k subscribers). Solo-dev can act as DPO with role-declaration per `business-logic-review.md` §2.3 solo-dev exemption; formal counsel review queued GAP-156. Self-designation acceptable IF + ONLY IF the role is explicitly worn + follow-up obligation attached.
- **Reviewer:** `@nguyenvankiet` (acting Legal scout + Compliance + DPO, solo-dev, 2026-05-06). Formal legal counsel + DPO independence review queued — see [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md) acceptance criteria item.
- **Compliance check:** **Compliant skeleton** — Decree 13/2023 Art 27 (DPO designation) + Art 28 (DPO functions) addressed at skeleton level. Full compliance pending Phase 2 (formal contracted DPO when team grows beyond solo OR when crossing 100k subscriber threshold per [GAP-353d](../04-quality/gaps/GAP-353d-dpia-decree-13-art-24-30-docs.md) §"Why P2").
- **Review cadence:** Annual + event-driven on (a) team growth beyond solo-dev, (b) crossing 50k subscriber threshold (P1 trigger), (c) crossing 90k subscriber threshold (P0 hard-deadline), (d) Decree 13/2023 amendment, (e) K-12 LEGAL trio merge (Phase 3 trigger). **Next review:** 2027-05-06 OR earlier upon any trigger.

---

## 2. Designated DPO

**Tên đầy đủ:** Nguyễn Văn Kiệt
**GitHub handle:** `@nguyenvankiet`
**Email contact:** `vannkite@outlook.com` (interim — `dpo@TODO` upon legal entity registration completion Phase 2)
**Vai trò chính (chính/phụ):** Solo-dev acting Legal scout + Compliance + DPO (multiple hats per solo-dev exemption)
**Tuyên bố vai trò (role declaration):** Solo-dev mode 2026-05-06 — same person wears Product Owner + Engineering + Legal-scout + DPO hats. Khi đội mở rộng beyond solo-dev, role tách biệt phải áp dụng và DPO independence (§4 dưới đây) phải được formally established với separate reporting line.

**Status:** Acting designation — pending (a) legal entity registration (formal company name + business registration ID), (b) Phase 2 counsel sign-off via [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md), (c) team growth threshold (when first non-author dev joins → DPO independence formalization).

**Effective date:** 2026-05-06 (skeleton effective immediately for compliance audit purposes; full formal designation upon Phase 2 sign-off).

---

## 3. Scope of DPO Authority (per Decree 13/2023 Art 28)

DPO chịu trách nhiệm giám sát + thực thi compliance trong các phạm vi sau:

### 3.1 Data subject rights enforcement
- Receive + process Data Subject Access Requests (DSAR) per PDPL Art 14 — SLA 20 ngày (extendable +10 ngày with notice).
- Channels: web form `/legal/data-rights` (per [GAP-353c](../04-quality/gaps/GAP-353c-dsar-self-service-form.md) Wave 26 Bucket A) + email backup `dpo@TODO`.
- Right types covered: ACCESS / RECTIFICATION / ERASURE / PORTABILITY / RESTRICT / OBJECT (PDPL Art 9-15).
- Escalation: SLA breach → DPO must notify data subject + log to AuditLog within 24h.

### 3.2 Breach notification + incident response
- Receive incident reports from engineering on-call within 24h of detection.
- Assess breach severity per PDPL Art 23 (72h notification window to A05 + affected subjects).
- Owner of breach notification template + decision authority on what constitutes "high risk" triggering subject notification.
- Cross-link [`privacy-policy.md`](privacy-policy.md) §14 Breach Notification.

### 3.3 DPIA oversight (per Decree 13/2023 Art 24-26)
- Maintain processing inventory (per [`dpia.md`](dpia.md) §2).
- Conduct DPIA when (a) new processing activity added, (b) existing activity material change, (c) annual review cadence, (d) crossing 50k subscriber threshold (full inventory backfill).
- Sign-off authority on residual risk acceptance (HIGH residual risk requires legal counsel co-sign).

### 3.4 MPS A05 liaison (per Decree 13/2023 Art 28)
- Responsible party for MPS A05 (Cục An ninh mạng và Phòng chống tội phạm sử dụng công nghệ cao — Bộ Công an) registration upon crossing 100k subjects threshold OR sensitive-data-at-scale trigger.
- Pre-emptive monitoring at 90k subscribers (90% threshold) per [`mps-a05-registration-check.md`](mps-a05-registration-check.md) §3.
- Submit + maintain registration records; respond to A05 audits.

### 3.5 Compliance training + culture
- Annual privacy + security training cho mọi staff có quyền truy cập dữ liệu khách hàng (Phase 2 — currently solo-dev: self-training via legal updates + counsel sync quarterly).
- Maintain training log + completion evidence.
- Cross-cut với [`acceptable-use-policy.md`](acceptable-use-policy.md) staff conduct standards.

### 3.6 Vendor + sub-processor due diligence
- Maintain registry of third-party processors (per [`privacy-policy.md`](privacy-policy.md) §7 Data Sharing).
- Sign Data Processing Agreement (DPA) với mỗi vendor processing PII.
- Annual DPA review; vendor breach response coordination.

### 3.7 Child data protection (per Luật Trẻ em 2016 + PDPL Art 16)
- Oversight cho all under-16 student data processing (cross-link [`child-protection-policy.md`](child-protection-policy.md)).
- Parental consent verification process.
- Escalation channel cho child safeguarding incidents.
- Quarterly tabletop exercise với legal counsel (Phase 2).

---

## 4. Communication Channels

| Channel | Purpose | SLA |
|---|---|---|
| `dpo@TODO` (Phase 2) | Public DPO contact email | 20 ngày response per PDPL Art 14 |
| Web form `/legal/data-rights` (per GAP-353c) | DSAR self-service intake | 20 ngày SLA + auto-acknowledge within 1h |
| Internal Slack/email cho engineering | Breach reports + incident escalation | 1h response |
| `legal@TODO` (Phase 2) | Backup channel + non-DPO legal queries | 5 business days |
| Quarterly tabletop sync | DPO + tech lead + on-call coordinator | Scheduled per `child-protection-policy.md` cadence |

**Public-facing publication of DPO contact:** [`privacy-policy.md`](privacy-policy.md) §2 (after Phase 2 entity registration); [`personas-catalog.md`](personas-catalog.md) cross-reference (skip — internal only).

---

## 5. Independence Guarantees (per Decree 13/2023 Art 28)

DPO PHẢI có quyền độc lập trong các quyết định compliance — KHÔNG bị chỉ đạo bởi business interest khi quyết định mâu thuẫn pháp lý.

### 5.1 Solo-dev mode (current 2026-05-06)
Solo-dev = same person wears all hats. Independence is structurally limited but functionally achievable through:
- **Documented decision log:** every DPO call logged trong gap files / commit messages với explicit "acting DPO hat" framing.
- **Counsel queue:** quarterly review of DPO decisions với external counsel (Phase 2 GAP-156).
- **Override discipline:** business decisions overriding DPO recommendations MUST be logged via `BUSINESS_RULE_OVERRIDE` trailer per `business-logic-review.md` §8 mechanism.

### 5.2 Post-team-growth (Phase 2+)
When team grows beyond solo, DPO independence MUST be formally established:
- Separate reporting line (DPO does NOT report to engineering manager OR product manager; reports to CEO / board / external counsel).
- Cannot be terminated for performing DPO duties (whistleblower protection extension).
- Budget authority for compliance training + tooling.
- Right to escalate directly to A05 if internal process fails.

### 5.3 Conflicts of interest declaration
Solo-dev mode current state: NO conflicts declared (single party). Phase 2: explicit declaration required before each DPIA sign-off + MPS A05 submission.

---

## 6. Reporting Line

### 6.1 Internal reporting (current solo-dev)
DPO reports to:
- Self (solo-dev) — documented decisions logged
- External counsel (Phase 2 quarterly sync)

### 6.2 Internal reporting (Phase 2+)
DPO reports to:
- Board / CEO (formal annual review)
- Audit committee (if established)
- Engineering org chart: dotted-line only (NO direct supervision by engineering management)

### 6.3 External reporting (regulatory)
DPO reports to:
- A05 Bộ Công an (per Decree 13/2023 Art 28(1)(c)) — registration + breach notifications
- MPS / police investigative requests (per Luật An ninh mạng 2018)
- Legal counsel (privileged communication)

---

## 7. Phase 2 Open Items (consolidated)

- [ ] Legal entity registration → formal DPO contract + email `dpo@<domain>` + business registration ID published
- [ ] Phase 2 counsel sign-off per [GAP-156](../04-quality/gaps/GAP-156-business-rules-compliance-audit.md)
- [ ] Independence formalization upon first team hire beyond solo-dev
- [ ] Annual training program established (when team >1)
- [ ] DPO contracted with VN PDPL expertise (when crossing 50k subscribers OR K-12 onboarding Phase 3)
- [ ] DPA template registry (when first vendor processing PII added)
- [ ] EN translation of DPO designation for international procurement audits

---

## 8. Cross-References

- **Privacy Policy:** [`privacy-policy.md`](privacy-policy.md) §2 Data Protection Officer field
- **DPIA framework:** [`dpia.md`](dpia.md) (DPO oversees DPIA process)
- **MPS A05 procedure:** [`mps-a05-registration-check.md`](mps-a05-registration-check.md) (DPO = responsible party)
- **Data Retention:** [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) (DPO approves retention exceptions)
- **Child Protection:** [`child-protection-policy.md`](child-protection-policy.md) (DPO oversight)
- **Compliance Scope:** [`compliance-scope.md`](compliance-scope.md) §VN PDPL row
- **Breach Notification ops:** [`privacy-policy.md`](privacy-policy.md) §14 + future incident-response runbook

---

## 9. Log

- **2026-05-06** (Phase 1 skeleton): Initial designation skeleton created via Wave 26 Bucket B closing GAP-353d. Solo-dev acting DPO declaration `@nguyenvankiet` per `business-logic-review.md` §2.3 solo-dev exemption with explicit role declaration + Phase 2 counsel review queued GAP-156. Decree 13/2023 Art 27-28 addressed at skeleton level. Compliance scope: K-12 trigger (Phase 3) + 100k subscriber trigger (P0 hard-deadline). Effective date 2026-05-06.
