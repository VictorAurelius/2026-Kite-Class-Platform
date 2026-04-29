# Compliance Scope — VN Legal Framework Mapping

**Status:** skeleton
**Created:** 2026-04-29
**Updated:** 2026-04-29
**Owner:** Legal + PM
**Reviewer:** Tech Lead + External Counsel
**Related Gap:** [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) (content creation tracking)

---

## 1. Scope / Context

TODO: Mô tả 1 đoạn — danh sách legal frameworks tại Việt Nam mà KiteHub + KiteClass phải compliance. SaaS giáo dục → giao thoa giữa data protection (PDPL), education law (MoET), labor law (HR/payroll), tax law (e-invoice TCT), cybersecurity (Luật ANM), consumer protection (B2B + B2C). Doc này là **mapping checklist**, không phải legal opinion — external counsel sign-off bắt buộc trước GA.

**Severity legend:**
- 🔴 BLOCKING — không tuân thủ = không launch được
- 🟠 MANDATORY — vi phạm có hình phạt admin/financial
- 🟡 ADVISORY — best practice + reputation risk

---

## 2. Personal Data Protection Law (Luật Bảo vệ Dữ liệu Cá nhân — PDPL 2023)

**Reference:** Nghị định 13/2023/NĐ-CP (effective 2023-07-01); Law 91/2025/QH15 PDPL (TODO verify date)
**Severity:** 🔴 BLOCKING
**Owner:** Legal + Tech Lead

### 2.1 Data inventory required
TODO:
- Liệt kê các loại personal data thu thập (name, phone, email, CCCD, date of birth, parent contact, student grades…)
- Phân loại "sensitive personal data" (per NĐ-13 §3.4: health, biometric, sexual orientation, criminal history, financial transactions, location)
- Map theo persona (P1 Solo Teacher / P5 K-12 School student → minor data special handling)

### 2.2 Lawful basis per processing activity
TODO: For each activity, document lawful basis (consent / contract / legal obligation / vital interest / public interest / legitimate interest):
- Student enrollment data
- Parent communication
- Payment processing
- AI branding logo analysis (`.claude/rules/ai-branding-guidelines.md` §9 references this)

### 2.3 Data subject rights (Quyền chủ thể dữ liệu)
TODO: Implement endpoints + UI cho:
- Access (truy cập)
- Rectification (cải chính)
- Erasure / Right to be forgotten (xóa)
- Restriction (hạn chế xử lý)
- Portability (tính di chuyển)
- Object (phản đối)

### 2.4 Cross-border data transfer
TODO: 
- AI inference: local Ollama (in-VN) vs OpenAI (US) — Impact Assessment (đánh giá tác động) bắt buộc khi gửi data ra nước ngoài
- Backup/DR strategy ảnh hưởng location

### 2.5 Breach notification
TODO: ≤72h theo NĐ-13 §23 — runbook in `documents/05-guides/incident-response/`

### 2.6 Consent management
TODO: separate consent records per processing purpose; minor (<16) requires parent consent (P5 K-12)

---

## 3. Education Law / MoET Circulars (Luật Giáo dục + Bộ GD&ĐT)

**Reference:** Luật Giáo dục 43/2019/QH14; Thông tư MoET liên quan transcript, học bạ điện tử, báo cáo
**Severity:** 🟠 MANDATORY (cho P5 K-12, P9 international school personas)
**Owner:** PM + School-domain SME

### 3.1 Báo cáo MoET
TODO: K-12 schools phải báo cáo lên Sở GD&ĐT — KiteClass phải export đúng format

### 3.2 Học bạ điện tử (Electronic transcript)
TODO: Thông tư 27/2020/TT-BGDĐT đánh giá học sinh — format bảng điểm chuẩn
TODO: ND về chữ ký số / dấu nhà trường on transcripts

### 3.3 Curriculum standards
TODO: subject codes, grade scales (10-point system), conduct grades (hạnh kiểm) per Thông tư MoET

### 3.4 Academic year structure
TODO: Khung kế hoạch năm học VN; align với GAP-053 (academic year + semester)

### 3.5 Student transfer (chuyển trường)
TODO: Required documents + process per MoET regulation

---

## 4. Cybersecurity Law (Luật An ninh mạng — 24/2018/QH14)

**Reference:** Luật ANM 24/2018/QH14; Nghị định 53/2022/NĐ-CP (data localization)
**Severity:** 🔴 BLOCKING (data localization for VN users)
**Owner:** Tech Lead + Legal

### 4.1 Data localization (NĐ-53)
TODO: Data của user VN PHẢI store tại VN ≥24 tháng — server placement (AWS Singapore vs OCI VN/Bangkok vs FPT Cloud) decision matrix

### 4.2 Co-operation with authorities
TODO: process khi nhận yêu cầu từ cơ quan có thẩm quyền (cung cấp dữ liệu user)

### 4.3 Content moderation
TODO: AI Branding `ContentModerationService` (per `.claude/rules/ai-branding-guidelines.md` §11.4.3) phải align Luật ANM Article 8 (banned content)

### 4.4 Local domain registration
TODO: nếu KiteHub provision custom domain `.vn` cho tenants → Bộ TT&TT registration requirements

---

## 5. Labor Code (Bộ luật Lao động 2019 — 45/2019/QH14)

**Severity:** 🟠 MANDATORY (cho payroll feature P3/P5/P4 — GAP-057, GAP-062)
**Owner:** PM + HR/Finance SME

### 5.1 Employee data classification
TODO: teacher/staff personal data + tax info handling

### 5.2 Payroll calculation rules
TODO: 
- Minimum wage zones (Vùng I/II/III/IV)
- Overtime rates (150% thường, 200% nghỉ tuần, 300% lễ Tết)
- BHXH/BHYT/BHTN deductions

### 5.3 Working time + leave
TODO: 48h/tuần chuẩn, leave policies, link với attendance feature

### 5.4 Contract types
TODO: HĐLĐ xác định/không xác định thời hạn implications cho teacher onboarding flow

---

## 6. Consumer Protection Law (Luật Bảo vệ Quyền lợi NTD — 19/2023/QH15)

**Severity:** 🟠 MANDATORY
**Owner:** Legal + PM

### 6.1 Pricing transparency
TODO: hiển thị đầy đủ giá, VAT, phí phát sinh per Luật BVNTD §6 (link `pricing-model.md`)

### 6.2 Refund + cancellation
TODO: cooling-off period cho B2B SaaS subscription; refund policy clear

### 6.3 Terms of service
TODO: TOS published, accessible, language-clear (per GAP-154 umbrella)

### 6.4 Dispute resolution
TODO: support channel + escalation; small-claims process

### 6.5 Advertising claims
TODO: Marketing copy compliance (link `.claude/skills/quality/marketing-legal-review/`)

---

## 7. Tax Law / E-Invoice (Luật Thuế + Thông tư TCT)

**Reference:** Nghị định 123/2020/NĐ-CP; Thông tư 78/2021/TT-BTC e-invoice
**Severity:** 🔴 BLOCKING (cho payment + invoice features)
**Owner:** Finance + Tech Lead

### 7.1 E-invoice format
TODO: TCT-compliant XML format, digital signature, sequential numbering

### 7.2 VAT handling
TODO: 0% education services exemption per Luật Thuế GTGT — confirm with tax counsel

### 7.3 Invoice issuance flow
TODO: connect với GAP-047 generated PDFs

### 7.4 TIN (Mã số thuế) collection
TODO: B2B tenants — collect MST as part of onboarding

---

## 8. Industry-Specific (K-12 School compliance)

**Severity:** 🟠 MANDATORY for P5 persona
**Owner:** PM + School SME

### 8.1 Child Online Safety
TODO: minor (<16) data handling, parental consent flow, no behavioral ads to minors

### 8.2 School fee collection regulation
TODO: Quy định thu học phí trường công vs tư; transparency requirements

### 8.3 School year schedule + holidays
TODO: VN public holidays (Tết Nguyên Đán, Quốc Khánh 2/9, ...) hard-coded in `personas-catalog.md` notes

---

## 9. Operational Compliance (recurring obligations)

| Obligation | Frequency | Owner | TODO |
|-----------|:---------:|-------|------|
| Data Protection Impact Assessment (DPIA) | Annual + per major change | Legal + Tech | TODO scope |
| Penetration test | Annual | Security | TODO vendor |
| MoET reporting | Per academic period | PM + School SME | TODO format |
| Tax filing support | Quarterly | Finance | TODO automation |
| Privacy policy review | Annual | Legal | TODO version control |

---

## 10. Dependencies / References

- BRD: [`personas-catalog.md`](personas-catalog.md) — P5 K-12, P9 International, P10 Special Ed have unique compliance scope
- BRD: [`pricing-model.md`](pricing-model.md) — refund + tax compliance
- Rule: [`.claude/rules/logs-format-standard.md`](../../.claude/rules/logs-format-standard.md) §3 PII Scrubbing — implements PDPL §2.6 consent + scrub
- Rule: [`.claude/rules/ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §9 Security & Privacy
- Skill: [`.claude/skills/quality/marketing-legal-review/`](../../.claude/skills/quality/marketing-legal-review/) — VN PDPL/Advertising/Consumer-protection-primary checklist
- Gap: GAP-154 (BRD scope expansion umbrella — TOS, Privacy Policy, AUP, Refund, Data Retention, Child Protection details)

---

## 11. Out of Scope (this skeleton)

- Filling regulatory text (Phase 2 — needs external counsel review)
- Compliance audit run (operational)
- DPIA artifact (per-feature, separate gap)
- Specific MoET form templates (P5 SME)

---

## 12. Log

- 2026-04-29 — Skeleton created (GAP-150 Phase 1). Mapped 7 primary frameworks (PDPL, MoET, Cybersecurity, Labor, Consumer Protection, Tax, K-12) + operational compliance table. Content fill requires external counsel — Phase 2 GAP-155.
