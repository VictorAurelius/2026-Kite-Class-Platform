---
name: marketing-legal-review
description: "Use when PR touches marketing copy (landing pages, blog, pricing page, brochure), legal docs (Terms of Service / TOS, Privacy Policy, Data Processing Agreement / DPA, SLA), or when user says 'review legal', 'review marketing', 'update TOS', 'privacy policy', 'kiểm tra pháp lý', 'xem điều khoản', 'điều khoản sử dụng'. Establishes legal counsel sign-off, dated versioning, compliance checklist for Vietnam (Nghị định 13/2023 PDPL, Luật Quảng cáo, Luật Bảo vệ Người tiêu dùng) + extraterritorial (GDPR, CAN-SPAM). Required before ship per output-review-mandate §5.8."
user-invocable: true
---

# Marketing + Legal Docs Review

Review gate cho customer-facing marketing copy và legal documents. Closes GAP-174 + output-review-mandate §5.8 VIOLATION #5.

## When to run

- PR adds/modifies:
  - **Marketing copy** — landing pages, `/pricing`, `/features`, blog posts, brochure PDFs, email campaigns (non-transactional)
  - **Legal docs** — TOS (Terms of Service / Điều khoản Sử dụng), Privacy Policy (Chính sách Bảo mật), DPA (Data Processing Agreement / Thỏa thuận Xử lý Dữ liệu), SLA (Service Level Agreement), Cookie Policy, AUP (Acceptable Use Policy)
  - **Consent surfaces** — signup checkboxes, cookie banner, data export forms
- Pricing change (values or model — flat vs per-seat vs usage-based)
- New data-processing feature that expands PII collection
- Expansion to new jurisdiction (new country)

## Reviewer matrix

| Doc class | Engineering | Marketing/Brand | Legal Counsel | Sign-off format |
|-----------|:-----------:|:---------------:|:-------------:|-----------------|
| Landing/pricing copy | ✅ | ✅ | optional | Dated PR approval |
| Blog / content marketing | optional | ✅ | optional | Dated PR approval |
| TOS / Privacy / DPA / SLA | ✅ | ✅ | ✅ **MANDATORY** | Signed PDF in `documents/legal/signed/` |
| Cookie Policy | ✅ | optional | ✅ MANDATORY | Dated PR approval + Legal email confirmation |
| Consent UI copy | ✅ | ✅ | ✅ MANDATORY | Dated PR approval |

**Legal counsel engagement:** If external counsel not yet retained, mark gap + defer publication. NEVER ship legal docs to real customers without counsel review — only marketing/exploratory "coming soon" placeholders are acceptable pre-counsel.

## Process

1. **Classify doc** — marketing copy vs legal doc (see matrix). Different path.
2. **Version + date** — every legal doc gets `documents/legal/{type}-vX.Y-YYYY-MM-DD.md`. Never edit in place.
3. **Run compliance checklist** — open `reference/compliance-checklist.md` — tick every applicable row per jurisdiction (Vietnam primary; GDPR if EU users).
4. **Follow workflow** — see `reference/workflow.md` for step-by-step:
   - Drafting → Internal review → Legal counsel review → Dated signature → Publish → Archive previous version
5. **Tenant notification** — if TOS/Privacy/DPA changes materially, tenants must receive **30-day notice** + opportunity to opt-out per Vietnamese Consumer Protection Law + GDPR Art. 7.
6. **Version pointers** — update `documents/legal/index.md` with: effective date, superseded version, changelog summary, transition period.
7. **Record review** — PR comment: "Legal review PASS. Counsel sign-off: {name, firm, date}. Effective: YYYY-MM-DD. Previous version archived to `07-archived/legal-{type}-vX.Y-YYYY-MM-DD.md`. Tenants notified: {N} via {channel} on {date}."

## Skill Contents

- `reference/workflow.md` — detailed workflow (drafting → counsel review → publish → tenant notification → archive)
- `reference/compliance-checklist.md` — compliance matrix per jurisdiction (VN primary, GDPR/CAN-SPAM secondary) + required clauses per doc type

## Gotchas

- **"Tenant agreed to v1.3 on 2026-01-15":** that tenant must ALWAYS see v1.3 when viewing TOS, even when v2.0 is current for new signups. Requires per-tenant `agreedVersion` field in subscription/account table — file gap if not implemented. Never force-migrate tenants silently.
- **Nghị định 13/2023 (PDPL — Vietnam Personal Data Protection Decree, effective 2023-07-01):** stricter than GDPR on some points. Requires: explicit consent (opt-in, not pre-checked), DPO appointment if processing >10,000 records/yr, cross-border transfer approval from Ministry of Public Security, 72-hr breach notification. Every privacy doc MUST cite this decree by number.
- **Luật Quảng cáo (Vietnam Advertising Law 2012 + Nghị định 91/2020):** marketing emails/SMS with `[QC]` subject prefix; pricing claims must be substantiated; comparative advertising to named competitors BANNED (can only reference general market).
- **Luật Bảo vệ Người tiêu dùng (Vietnam Consumer Protection Law 2023, effective 2024-07-01):** applies when tenant is treated as end-consumer (small schools, individual tutors). 30-day notice for material TOS changes; unilateral cancellation right if tenant disagrees.
- **GDPR applicability:** triggered if ANY user accessed from EU (including travel) OR marketing targets EU. Conservative assumption: apply GDPR to all marketing surfaces. Privacy Policy must have GDPR + PDPL section side-by-side.
- **Pricing copy claims:** "Miễn phí vĩnh viễn" / "Free forever" claims legally bind — requires legal review EVEN on marketing pages. "Starting at X" requires substantiation with actual SKU price. "Trusted by N+ schools" requires verifiable source.
- **Defamation + competitor comparisons:** naming competitors in negative light = defamation risk under Vietnam civil code. Compare against "the market average" or anonymized benchmarks only.
- **Template copy — watch for "we" vs "you":** TOS says "You agree to..." — pronoun clarity matters for enforceability. "The Company" vs "KiteClass" vs tenant's legal entity all have different legal weight; pick one and stick per doc.
- **Electronic signatures:** Vietnam recognizes digital signatures (Luật Giao dịch Điện tử 2005) but requires specific cert authority for high-value contracts (>VND 50M). For DPA with Enterprise customers, use DocuSign or certified VN provider (VNPT-CA, Viettel-CA).

## Related

- Rule: `.claude/rules/output-review-mandate.md` §5.8
- Sibling skill: `.claude/skills/quality/email-template-review/SKILL.md` (email templates cite privacy/unsubscribe URLs that come from here)
- Gap: `documents/04-quality/gaps/GAP-174-marketing-legal-review.md`
- Pre-GA blocker per gap — do not ship TOS/Privacy to real customers before counsel sign-off.
