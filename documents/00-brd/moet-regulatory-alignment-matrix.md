# MoET Regulatory Alignment Matrix — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** PM + School-domain SME
**Reviewer:** Legal counsel + Education domain expert + Tech Lead
**Legal basis:** **Luật Giáo dục 2019 (43/2019/QH14) (Education Law — L5)**; **Thông tư Bộ GD&ĐT (MoET circulars)** — Thông tư 27/2020/TT-BGDĐT (đánh giá học sinh / học bạ điện tử), các thông tư báo cáo + chương trình GDPT 2018
**Related:** [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.5 VN-EDU-1/2/3 · [`compliance-scope.md`](compliance-scope.md) §3 (Education Law/MoET) + §8 (K-12) · ADR-001 (k12-data-model) · ADR-002 (academic-year-structure) · [`academic-year-curriculum-structure-policy.md`](academic-year-curriculum-structure-policy.md) · [`child-protection-policy.md`](child-protection-policy.md) · [`data-export-portability-policy.md`](data-export-portability-policy.md)

---

## 1. Phạm vi & mục đích

Tài liệu này là **ma trận đối chiếu** giữa yêu cầu quy định của Bộ GD&ĐT (MoET) / Luật Giáo dục và tính năng tương ứng trong KiteClass, kèm trạng thái coverage. Mục tiêu: làm rõ K-12 compliance gap trước khi triển khai cho trường (P5 persona — phần lớn **defer Phase 3** sau khi engage counsel).

Skeleton Phase 1: cấu trúc ma trận + liệt kê requirement chính + đánh dấu coverage status; nội dung chi tiết (format thông tư cụ thể, mapping field) cần School SME + counsel Phase 3.

> Bối cảnh phase: phần lớn yêu cầu MoET chỉ áp dụng cho **P5 K-12 School** — không nằm trong Phase 1/2 scope (P1/P2/P3 center). Tài liệu này chuẩn bị trước cho Phase 3 trigger.

---

## 2. Regulatory Alignment Matrix

Ký hiệu coverage: ✅ covered · 🟡 partial · ❌ not covered · ⏸️ defer (Phase 3) · n/a không áp dụng phase hiện tại.

| # | Yêu cầu MoET / Luật GD | Căn cứ | KiteClass feature/coverage | Status | Phase |
|:--:|---|---|---|:--:|:--:|
| M1 | Học bạ điện tử (electronic transcript) đúng format | Thông tư 27/2020/TT-BGDĐT | Grade + transcript domain (ref ADR-019 doc-gen) | ⏸️ defer | P3 |
| M2 | Đánh giá học sinh (thang điểm 10, nhận xét, hạnh kiểm) | Thông tư 27/2020 | Grade domain (`01-business/grade`) | 🟡 partial | P3 |
| M3 | Báo cáo lên Sở GD&ĐT đúng định dạng | Luật GD + thông tư báo cáo | Reporting/export module | ⏸️ defer | P3 |
| M4 | Cấu trúc năm học / học kỳ chuẩn VN | Khung kế hoạch năm học | Academic year (ref ADR-002) — xem [`academic-year-curriculum-structure-policy.md`](academic-year-curriculum-structure-policy.md) | 🟡 partial | P1-P3 |
| M5 | Mã môn học + chương trình GDPT 2018 | Chương trình GDPT 2018 | Curriculum/subject model | ⏸️ defer | P3 |
| M6 | Chữ ký số / dấu nhà trường trên học bạ | ND chữ ký số + thông tư | Digital signature on documents | ❌ not covered | P3 |
| M7 | Chuyển trường (student transfer) — hồ sơ + quy trình | Quy định MoET | Student transfer flow | ❌ not covered | P3 |
| M8 | Minh bạch học phí (tuition disclosure) | VN-EDU-1 + Luật BVNTD | Pricing/billing (ref [`billing-terms.md`](billing-terms.md)) | 🟡 partial | P1 |
| M9 | Tối thiểu hóa dữ liệu học sinh (chỉ phục vụ giáo dục) | VN-EDU-2 + PDPL Art 6 | Data classification (ref [`data-classification-policy.md`](data-classification-policy.md)) | 🟡 partial | P1-P3 |
| M10 | Lưu trữ hồ sơ học sinh theo quy định MoET | VN-EDU-3 | Retention (ref [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) — edu-records 5y) | 🟡 partial | P1-P3 |
| M11 | Bảo vệ trẻ em / an toàn học đường | Luật Trẻ em 2016 (cross-cut) | [`child-protection-policy.md`](child-protection-policy.md) | 🟡 partial | P3 |

> TBD (Phase 3 — needs School SME + counsel input): xác nhận đầy đủ danh sách thông tư áp dụng; mapping field cụ thể từng yêu cầu → schema; format báo cáo Sở GD&ĐT.

---

## 3. Coverage Summary

- **Phase 1 (center):** M8/M9/M10 partial — center cũng cần minh bạch học phí + minimization + retention, nhưng KHÔNG cần học bạ điện tử/báo cáo Sở.
- **Phase 3 (K-12):** M1-M7 + M11 kích hoạt đầy đủ — đây là rào cản chính cho triển khai trường.

> TBD (Phase 3): điểm coverage tổng hợp /100 cho K-12 readiness; gap list ưu tiên trước launch trường đầu tiên.

---

## 4. Phase 3 Readiness Triggers

Trước khi triển khai cho trường K-12 đầu tiên, các mục ❌/⏸️ phải chuyển ✅:
1. M1/M2 học bạ điện tử + đánh giá đúng Thông tư 27/2020.
2. M6 chữ ký số/dấu trường trên transcript.
3. M3 báo cáo Sở GD&ĐT.
4. M11 child protection (điều kiện tiên quyết — [`child-protection-policy.md`](child-protection-policy.md)).

> TBD (Phase 3 — needs counsel): legal sign-off rằng coverage đủ để không vi phạm Luật GD khi vận hành trường.

---

## 5. Tuân thủ pháp lý (Compliance)

- **Education Law L5 (`compliance-scope.md` §3):** học bạ, báo cáo, chương trình, năm học, chuyển trường.
- **VN-EDU-1/2/3 (`compliance-checklist.md` §1.5):** minh bạch học phí, minimization, retention hồ sơ học sinh.
- **K-12 cross-cut (`compliance-scope.md` §8):** child online safety + child protection — xem M11.

---

## 6. Dependencies / References

- ADR-001 (k12-data-model), ADR-002 (academic-year-structure), ADR-019 (document-generation)
- BRD: [`academic-year-curriculum-structure-policy.md`](academic-year-curriculum-structure-policy.md), [`compliance-scope.md`](compliance-scope.md) §3/§8, [`child-protection-policy.md`](child-protection-policy.md), [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md), [`data-export-portability-policy.md`](data-export-portability-policy.md), [`billing-terms.md`](billing-terms.md)
- Checklist: [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.5
- Consumer: `01-business/grade`, `01-business/student` (rules.md implement)

---

## 7. Out of Scope (this skeleton)

- Mapping field cụ thể từng thông tư → entity schema (Phase 3 — School SME)
- Format báo cáo Sở GD&ĐT (Phase 3 — SME)
- Chữ ký số / dấu trường implementation (Phase 3 — engineering + legal)

---

## 8. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P1 batch). 11-row MoET alignment matrix (M1-M11) với coverage status + phase mapping; phần lớn K-12 requirement đánh dấu defer Phase 3. Field-level mapping + thông tư format marked TBD (Phase 3, needs School SME + counsel). Cites Education Law L5 + VN-EDU-1/2/3.
