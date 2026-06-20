---
audience: mixed
---

# Compliance Checklist — 7 Vietnamese Laws × Business Rules

**Created:** 2026-06-21
**Maintainer:** @nguyenvankiet (acting Compliance + Legal scout, solo-dev)
**Scope:** Actionable per-law compliance checklist cho mọi `documents/01-business/**/rules.md` business rule. Layer thực thi (obligation + posture) bổ sung cho `compliance-scope.md` (layer chiến lược).
**Status:** ⚠️ v1 self-assessed — formal legal counsel review **chưa engaged** (GAP-156 AC-D, Phase 2 counsel). Mọi verdict "Compliant" dưới đây là solo-dev best-effort, KHÔNG phải legal opinion.
**Closes:** GAP-156 AC-C (Build `documents/00-brd/compliance-checklist.md` — 7 VN laws)
**Related:** `.claude/rules/business-logic-review.md` §2.4 · `compliance-scope.md` (GAP-150 chiến lược) · `dpia.md` · `data-retention-deletion-policy.md` · `privacy-policy.md`

---

## 1. Mục đích + cách dùng

`compliance-scope.md` trả lời "luật nào áp dụng cho KiteHub ở mức chiến lược". File này trả lời câu hỏi thực thi: **với MỖI luật, business rule nào chạm tới nó, nghĩa vụ cụ thể là gì, và posture hiện tại ra sao.**

Mỗi khi tạo/sửa một business rule trong `rules.md` (per `business-logic-review.md` §2.4 attribute "Compliance check"), tác giả:
1. Tra §3 bên dưới → xác định luật nào áp dụng cho domain đó.
2. Gán posture cho rule: **N/A** (không chạm vùng điều chỉnh) / **Considered** (chạm nhưng không kích nghĩa vụ cụ thể) / **Compliant** (chạm + dẫn nghĩa vụ + nêu cách thỏa) / **GAP** (chạm + nghĩa vụ chưa thỏa → file gap).
3. Ghi posture vào attribute "Compliance check" của rule.

International (GDPR/CCPA) **out of scope** tới khi KiteClass mở rộng ngoài VN — theo `compliance-scope.md`.

---

## 2. 7 luật VN — bảng tổng quan

| # | Luật | Số hiệu / Văn bản | Hiệu lực | Vùng điều chỉnh chính | KiteHub touchpoint |
|---|---|---|---|---|---|
| L1 | **Luật Bảo vệ Dữ liệu Cá nhân (PDPL)** | Nghị định 13/2023/NĐ-CP (PDPD) | 2023-07-01 | PII, consent, retention, cross-border transfer | Mọi domain xử lý dữ liệu user/student/parent |
| L2 | **Luật Quản lý Thuế + Hóa đơn điện tử** | Luật 38/2019/QH14 + NĐ 123/2020/NĐ-CP | 2022-07-01 (e-invoice bắt buộc) | Hóa đơn, chứng từ tài chính | payment-invoice, subscription-billing, payroll |
| L3 | **Luật Bảo vệ Quyền lợi Người tiêu dùng** | Luật 19/2023/QH15 | 2024-07-01 | Refund, giá hiển thị, quảng cáo, hợp đồng mẫu | course-pricing, pricing-model, refund-dispute, marketing |
| L4 | **Bộ luật Lao động** | Bộ luật 45/2019/QH14 | 2021-01-01 | Hợp đồng lao động, thù lao | teacher, payroll, staff-invitation |
| L5 | **Luật Giáo dục + quy định Bộ GD&ĐT (MoET)** | Luật 43/2019/QH14 + thông tư MoET | 2020-07-01 | Độ tuổi học sinh, điều kiện giáo viên, cấp phép trung tâm | academic-year, teacher, k12-model, role-hierarchy |
| L6 | **Luật An ninh mạng + bản địa hóa dữ liệu** | Luật 24/2018/QH14 + NĐ 53/2022/NĐ-CP | 2019-01-01 | Lưu trữ dữ liệu, mã hóa, localization | storage, security-foundation, multi-tenancy, data-retention |
| L7 | **Luật Giao dịch điện tử** | Luật 20/2023/QH15 | 2024-07-01 | Hợp đồng điện tử, chữ ký số | tenant-auth, consent, billing-terms, terms-of-service |

---

## 3. Checklist theo từng luật

> Posture verdict dưới đây là **self-assessed v1** (solo-dev). Cột "Evidence/Gap" trỏ tới artifact hiện có hoặc gap cần mở.

### L1 — PDPL (Nghị định 13/2023/NĐ-CP)

| # | Nghĩa vụ | Domains áp dụng | Posture (self-assessed) | Evidence / Gap |
|---|---|---|---|---|
| L1.1 | Thu thập consent có cơ sở pháp lý, ghi nhận time + version | consent, cookie-consent, onboarding | ⚠️ Considered | `kitehub/consent/rules.md`, `cookie-consent/rules.md`; cần verify version-stamping đủ |
| L1.2 | Retention tối thiểu + xóa khi hết mục đích | data-retention (×2), off-boarding | ⚠️ Considered | `data-retention-deletion-policy.md`; cần xác nhận con số tháng dẫn điều khoản PDPD |
| L1.3 | Cross-border transfer assessment (nếu dữ liệu rời VN) | storage, multi-tenancy | ❓ GAP | AWS Singapore (ap-southeast-1) = cross-border → cần DPIA mục transfer (xem `dpia.md`) |
| L1.4 | DPO designation + DPIA cho high-risk processing | (toàn hệ thống) | ⚠️ Considered | `dpo-designation.md`, `dpia.md` đã có (skeleton); cần legal review |
| L1.5 | Quyền chủ thể dữ liệu (truy cập, xóa, đính chính, rút consent) | parent-portal, student-portal, off-boarding | ❓ GAP | DSAR flow tồn tại 1 phần; cần map đủ 6 quyền PDPD Art 9 |
| L1.6 | MPS A05 registration (đăng ký xử lý dữ liệu với Bộ CA) | (toàn hệ thống) | ⚠️ Considered | `mps-a05-registration-check.md` đã có; thực thi đăng ký = real-user-action |

### L2 — Quản lý Thuế + Hóa đơn điện tử (NĐ 123/2020)

| # | Nghĩa vụ | Domains | Posture | Evidence / Gap |
|---|---|---|---|---|
| L2.1 | Hóa đơn điện tử hợp lệ (mã CQT, định dạng XML chuẩn) | payment-invoice, subscription-billing | ❓ GAP | Hiện sinh invoice nội bộ; chưa tích hợp nhà cung cấp HĐĐT (MISA/Viettel) — Phase 2 partnership (xem GAP-185) |
| L2.2 | Lưu chứng từ tài chính ≥10 năm | payment-record, payroll | ⚠️ Considered | Retention cho financial record cần tách khỏi retention PII (10 năm vs 36 tháng) |
| L2.3 | Đánh số hóa đơn tuần tự không trùng | payment-invoice | ⚠️ Considered | `payment-invoice/rules.md` invoice numbering — cần verify chuẩn NĐ 123 |

### L3 — Bảo vệ Quyền lợi Người tiêu dùng (Luật 19/2023)

| # | Nghĩa vụ | Domains | Posture | Evidence / Gap |
|---|---|---|---|---|
| L3.1 | Chính sách hoàn tiền minh bạch, công bố trước | course-pricing, pricing-model, refund-dispute | ⚠️ Considered | `refund-dispute-resolution-policy.md` (skeleton, GAP-183 chờ legal); engine = manual SOP (non-PSP) |
| L3.2 | Hiển thị giá đầy đủ (gồm thuế), không gây nhầm | pricing-model, marketing | ⚠️ Considered | Cần verify giá hiển thị VND gồm/không gồm VAT rõ ràng |
| L3.3 | Hợp đồng mẫu không có điều khoản bất lợi vô lý | terms-of-service, billing-terms | ⚠️ Considered | `terms-of-service.md` + `billing-terms.md` skeleton; cần legal review điều khoản mẫu |
| L3.4 | Quảng cáo không sai sự thật (SLA, cam kết) | marketing (×2) | ⚠️ Considered | Marketing claims cần khớp NFR thực tế (`nfr-catalog.md`) |

### L4 — Bộ luật Lao động (Bộ luật 45/2019)

| # | Nghĩa vụ | Domains | Posture | Evidence / Gap |
|---|---|---|---|---|
| L4.1 | Hợp đồng giáo viên/cộng tác viên hợp lệ | teacher, staff-invitation | ⚠️ Considered | Tenant tự quản hợp đồng; KiteHub = nền tảng → posture phần lớn N/A (tenant-side liability) |
| L4.2 | Tính thù lao/hoa hồng minh bạch | payroll | ⚠️ Considered | `payroll/rules.md` commission %; cần dẫn cơ sở khi là rule mặc định |

### L5 — Luật Giáo dục + MoET (Luật 43/2019)

| # | Nghĩa vụ | Domains | Posture | Evidence / Gap |
|---|---|---|---|---|
| L5.1 | Cấu trúc niên khóa/cấp học theo chuẩn MoET | academic-year, k12-model | ⚠️ Considered | ADR-002 academic-year; K-12 = Phase 3 (defer) |
| L5.2 | Điều kiện giáo viên (nếu nền tảng ràng buộc) | teacher | ✅ N/A | KiteHub không thẩm định bằng cấp giáo viên = tenant trách nhiệm |
| L5.3 | Bảo vệ trẻ em (học sinh <16) | child-protection, k12-model | ⚠️ Considered | `child-protection-policy.md` (skeleton, GAP-186); K-12 P5 Phase 3 |

### L6 — An ninh mạng + Bản địa hóa (Luật 24/2018 + NĐ 53/2022)

| # | Nghĩa vụ | Domains | Posture | Evidence / Gap |
|---|---|---|---|---|
| L6.1 | Lưu trữ dữ liệu người dùng VN (data localization) | storage, multi-tenancy | ❓ GAP | AWS Singapore — cần đánh giá NĐ 53 yêu cầu localization với loại dữ liệu nào (xem L1.3 chung gốc) |
| L6.2 | Mã hóa dữ liệu nhạy cảm (at-rest + in-transit) | security-foundation, security-hardening, storage | ⚠️ Considered | RLS + TLS có; cần xác nhận encryption-at-rest cho PII column |
| L6.3 | Audit log bất biến + retention | admin-audit, data-retention | ✅ Considered | `admin-audit/rules.md` + V60 immutable admin_audit_logs (per security audit) |

### L7 — Giao dịch điện tử (Luật 20/2023)

| # | Nghĩa vụ | Domains | Posture | Evidence / Gap |
|---|---|---|---|---|
| L7.1 | Hợp đồng điện tử có hiệu lực (accept ToS = giao kết) | terms-of-service, billing-terms, consent | ⚠️ Considered | Click-accept ToS cần ghi nhận time + version + IP để chứng minh giao kết |
| L7.2 | Chữ ký điện tử (nếu dùng) | tenant-auth | ✅ N/A | Chưa dùng chữ ký số; click-accept = giao kết hợp lệ theo Luật 20/2023 |

---

## 4. Ma trận domain → luật áp dụng (tra nhanh)

| Domain (rules.md) | Luật áp dụng |
|---|---|
| consent, cookie-consent | L1, L7 |
| data-retention (kiteclass + kitehub) | L1, L2, L6 |
| payment-invoice, payment-record | L2, L3 |
| subscription-billing, billing-terms | L2, L3, L7 |
| course-pricing, pricing-model | L3 |
| refund-dispute | L3 |
| marketing (×2) | L3 |
| teacher, staff-invitation | L4, L5 |
| payroll | L2, L4 |
| academic-year, k12-model, child-protection | L5 |
| storage, multi-tenancy, security-foundation, security-hardening | L6 |
| admin-audit | L6 |
| tenant-auth, terms-of-service | L7 |
| (domain không liệt kê) | N/A trừ khi chạm PII → mặc định L1 |

---

## 5. Trạng thái sign-off (GAP-156 AC-D — REAL-USER-ACTION)

| Sign-off role | Trạng thái |
|---|---|
| Solo-dev self-assessment (acting Compliance) | ✅ v1 done (file này) |
| Legal counsel formal review | ❌ Chưa engaged — Phase 2 counsel (GAP-156 AC-D, blocked) |
| Product Owner (pricing/refund) | ⚠️ solo-dev đội mũ PO, cần re-review khi team > solo |

**Mọi posture "Compliant"/"Considered" ở §3 là self-assessed, KHÔNG thay thế legal opinion.** Khi engage counsel (Phase 2), checklist này là input để review.

---

## 6. Cadence re-review

| Trigger | Hành động |
|---|---|
| Quý (mặc định) | Re-run baseline audit (`business-correctness/YYYY-Q#.md`) + refresh posture |
| Luật VN mới/sửa (vd nghị định hướng dẫn PDPD) | Re-review mọi rule có Compliance check chạm luật đó (L-row) |
| Mở vùng dữ liệu mới (cross-border, biometric) | DPIA + cập nhật L1/L6 |
| Engage legal counsel | Flip self-assessed → counsel-reviewed; cập nhật §5 |

**Next review:** 2026-09-21 (Q3 → Q4) HOẶC trong 30 ngày kể từ khi nghị định hướng dẫn PDPD mới công bố.

---

## 7. Log

- **2026-06-21 (v1.0):** Created — GAP-156 AC-C. 7-law actionable checklist (L1-L7) + domain→law matrix + per-law obligation posture (self-assessed solo-dev). Grounds in `business-logic-review.md` §2.4 law table + `compliance-scope.md` strategic scope. Posture verdicts v1 self-assessed; legal counsel review (AC-D) remains REAL-USER-ACTION blocked Phase 2. Author: @nguyenvankiet (acting Compliance scout, solo-dev).
