# Pending Folder — Legal/Compliance Gaps Deferred

**Created:** 2026-05-11
**Owner:** @nguyenvankiet (solo-dev)
**Status:** Active deferral folder

## Mục đích

Folder này chứa các gap **đã được hoãn vô thời hạn** theo quyết định scope-cut của solo-dev — KiteClass chưa cần đạt mức tuân thủ pháp lý cao trong giai đoạn này. Đây không phải `closed/` (đã hoàn thành) hay backlog active (sẽ làm sớm) — đây là **out-of-scope chính thức** cho Phase 1 BETA + Phase 1.5 PAID, sẽ revisit khi đủ điều kiện.

## Phạm vi (28 gaps, 5 nhóm luật)

### 1. PDPL — Nghị định 13/2023/NĐ-CP (7 gaps)
Bảo vệ dữ liệu cá nhân — banner consent, privacy policy, retention, DSAR, audit log.
- GAP-182 Privacy Policy
- GAP-184 Data Retention + Deletion
- GAP-301 Tenant Data Export Bundle (DSAR)
- GAP-321c Parent Portal Phase 1C — PDPL consent
- GAP-353 PDPL Cookie Consent Banner
- GAP-353b Server Consent API
- GAP-353b-followup Multi-device + Audit Chain

### 2. Luật Trẻ em 2016 (5 gaps)
Bảo vệ trẻ em — child protection workflow, vetting, mandatory reporting.
- GAP-186 Child Protection Policy
- GAP-322 Child Protection Workflow
- GAP-322b Phase 1B — Vetting + MinIO RBAC
- GAP-322c Phase 1C — Mandatory Reporting + Hash Audit
- GAP-359 Phase 1C Remainder

### 3. Luật Giáo dục 2019 + MOET (8 gaps)
Quy định giáo dục — parent portal mandate, MOET licensing, school records.
- GAP-321 Parent Portal v1 LEGAL MANDATE
- GAP-326 MOET School License Verification
- GAP-327 MOET Subject Taxonomy Seed
- GAP-336 MOET Financial Report TT 107/200
- GAP-340 MOET Inter-school Transfer API
- GAP-341 Phổ cập Escalation Mandatory
- GAP-343 Học bạ + Bằng tốt nghiệp Sealed PDF
- GAP-361 Parent Portal Phase 1C Remainder

### 4. Tax + Lao động + Lưu trữ (5 gaps)
Thuế, BHXH/BHYT/TNCN, lưu trữ.
- GAP-185 Billing Terms + VAT/TCT
- GAP-306 Teacher Commission BHXH/BHYT/TNCN
- GAP-317 Staff Offboard Wizard
- GAP-319 WORM Audit Log 10-Year Tax
- GAP-344 School Closure 30y Archive (Luật Lưu trữ)

### 5. Legal audit + sổ đầu bài (4 gaps)
- GAP-156 Business Rules Compliance Audit (Phase 2 legal counsel sign-off)
- GAP-333 Sổ đầu bài Digital
- GAP-345 Wave 17 K-12 LEGAL Trio State-Check Audit
- GAP-335 Public/Private School Fee Compliance

## Rationale (lý do hoãn)

1. **Solo-dev mode** — chưa engage legal counsel, không đủ năng lực self-audit toàn bộ compliance.
2. **Target user Phase 1 BETA** = P1 Solo Teacher + P2 Small Center, không có học sinh K-12 nên Luật Trẻ em + Luật Giáo dục không trigger.
3. **No live commercial tenants** yet — PDPL Article 9 consent requirement chưa apply (chưa có data subject thực sự).
4. **Beta period** = invite-only, không phải public commercial service → coverage pháp lý chưa critical.
5. **Risk tolerance Moderate** chốt 2026-05-06 — chấp nhận "v1 pending counsel review" disclaimer.

## Acknowledged risks (rủi ro đã ghi nhận)

| Rủi ro | Mức độ | Mitigation |
|---|---|---|
| **PDPL Nghị định 13/2023 hạn 2026-07-01** | 🔴 CAO | Beta tenants được brief "v1 pending counsel review"; không thu thập dữ liệu cá nhân ngoài tối thiểu cần thiết cho dịch vụ |
| Luật Trẻ em vi phạm hình sự nếu có học sinh K-12 | 🟠 TRUNG | Không onboard tenant K-12 trong Phase 1 BETA + Phase 1.5 PAID (gating ở provisioning) |
| Phạt hành chính Nghị định 13/2023 | 🟡 THẤP | Mức phạt khả thi self-fund nếu trigger; chưa có precedent enforcement với SaaS giáo dục solo-dev |
| Mất uy tín / mất tenant nếu vi phạm công khai | 🟡 THẤP | Beta cohort được informed consent về limitations |

## Re-evaluation triggers (khi nào revisit)

Move khỏi `pending/` về active backlog khi MỘT trong các điều kiện:
1. **Legal counsel engaged** (tối thiểu 1 luật sư VN có kinh nghiệm PDPL/giáo dục SaaS)
2. **First commercial tenant signing** — bắt đầu thu thập dữ liệu thật → PDPL trigger
3. **First K-12 tenant request** — Luật Trẻ em + Luật Giáo dục trigger
4. **Regulator inquiry** — Bộ Công an A05 / Bộ GD&ĐT contact
5. **Phase 2 ramp** đạt — public paid launch khả thi → cần compliance trước expansion
6. **Phase 3 K-12 schools** scope trigger (post-counsel)

## Khi revisit

1. Move file từ `pending/` về top-level `documents/04-quality/gaps/`
2. Update CSV row: `filename` bỏ prefix `pending/`, `status` trả về `OPEN` hoặc `PARTIAL` cho phù hợp
3. Log entry trong gap file ghi "Returned to active backlog YYYY-MM-DD because <trigger>"
4. Update ROADMAP §🚀 Next Action

## Liên kết quy tắc

- `CLAUDE.md` §CURRENT PHASE — Phase 1 BETA decision context (chốt 2026-05-06)
- `.claude/rules/business-logic-review.md` §2.4 — VN compliance checklist
- `.claude/rules/gap-architecture-v2.md` — CSV canonical (status=PENDING enum cho gaps trong folder này)
- `documents/03-planning/roadmap/release-1-plan-2026.md` §6.4 — defer rationale

