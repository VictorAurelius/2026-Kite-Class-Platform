# Data Export / Portability Policy — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** Engineering Lead + DPO
**Reviewer:** Legal counsel + Tech Lead + DPO
**Legal basis:** **Nghị định 13/2023/NĐ-CP (PDPL — L1)** — quyền chủ thể dữ liệu: tính di chuyển dữ liệu (data portability) + quyền truy cập/cung cấp bản sao dữ liệu cá nhân (PDPL Art 9/14 — quyền của chủ thể dữ liệu); cross-ref GDPR Art 20 (right to data portability, extraterritorial)
**Related:** [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.1 VN-PDPL-8 + PART 2 EU-GDPR-4 · [`compliance-scope.md`](compliance-scope.md) §2.3 (data subject rights) · [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) §6 (offboarding export) + §7 (erasure) · [`privacy-policy.md`](privacy-policy.md) · [`data-classification-policy.md`](data-classification-policy.md)

---

## 1. Phạm vi & mục đích

Tài liệu này định nghĩa **quyền export + tính di chuyển dữ liệu** (data export / portability) cho cả 2 cấp độ:

- **Tenant-level export:** trung tâm/trường rút toàn bộ dữ liệu của mình (khi offboard hoặc theo yêu cầu).
- **Subject-level export:** cá nhân (teacher/parent/student) yêu cầu bản sao dữ liệu cá nhân của mình theo PDPL.

Nguyên tắc: dữ liệu thuộc về khách hàng (TOS data-ownership); nền tảng là processor. Export phải ở định dạng **machine-readable** + có cấu trúc + đầy đủ scope đã consent.

Skeleton Phase 1: khung quyền + scope + định dạng + SLA structure. Schema export cụ thể + giới hạn rate cần engineering + legal sign-off Phase 2.

> Lưu ý quan hệ với retention: export là điều kiện thường gặp ở offboarding ([`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) §6) — phải cung cấp export TRƯỚC khi xóa.

---

## 2. Quyền export (Export rights)

### 2.1 Subject-level (PDPL data subject right)

Theo **PDPL Art 9/14** (VN-PDPL-8 — quyền access + portability), chủ thể dữ liệu có quyền:
- Yêu cầu **bản sao** dữ liệu cá nhân của mình (access).
- Yêu cầu **chuyển** dữ liệu sang định dạng có cấu trúc, đọc được bằng máy (portability).

Kênh tiếp nhận: in-app self-service form + email DPO (xem [`privacy-policy.md`](privacy-policy.md) rights-exercise channel).

### 2.2 Tenant-level (data ownership)

Tenant (Owner/Admin) có quyền export toàn bộ dữ liệu tenant — student records, courses, classes, attendance, grades, invoices — khi:
- Chủ động (in-app export tool).
- Offboarding (cross-ref [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) §6 bước 3 "Data export").

> TBD (Phase 2 — needs product input): scope chính xác tenant export (có bao gồm AI-generated assets? communication logs? audit logs?).

---

## 3. Định dạng & Scope (Formats & scope)

### 3.1 Định dạng

| Loại dữ liệu | Định dạng export | Ghi chú |
|---|---|---|
| Structured records (student, grade, attendance) | CSV / JSON | machine-readable, UTF-8 BOM |
| Documents (transcripts, invoices) | PDF | ref ADR-019 document-generation |
| Bulk tenant archive | ZIP (CSV/JSON + PDF + manifest) | có manifest mô tả schema |

> TBD (Phase 2 — needs engineering input): JSON schema chuẩn cho từng entity; format học bạ điện tử K-12 (ref MoET Thông tư 27/2020 — xem [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md)).

### 3.2 Scope export theo classification

Áp dụng [`data-classification-policy.md`](data-classification-policy.md):
- Subject export chỉ chứa dữ liệu **của chính subject đó** (no cross-subject leak).
- Tenant export giới hạn trong tenant (no cross-tenant leak — verify isolation).
- T4 sensitive/child: export chỉ cho parent/guardian đã verify (minor), không cho minor tự export sensitive.

---

## 4. Request SLA (thời hạn xử lý yêu cầu)

| Loại request | SLA xử lý | Ghi chú |
|---|:---:|---|
| Subject access/portability | TBD ≤30 ngày | theo PDPL (thường 30 ngày) |
| Tenant self-service export | tức thời / async job | enqueue → notify khi sẵn sàng |
| Offboarding export | trong grace window | trước hard-delete |

Async job pattern: enqueue → generate → signed download URL có TTL (xem [`data-classification-policy.md`](data-classification-policy.md) — T3/T4 export link phải có hạn + audit).

> TBD (Phase 2 — needs legal input): xác nhận SLA chính xác PDPL cho subject request; rate-limit chống lạm dụng export (DoS qua mass export).

### 4.1 Identity verification

Subject request phải **xác minh danh tính** trước khi export (PDPL Art 14 yêu cầu) — tránh export nhầm cho kẻ mạo danh.

---

## 5. Quan hệ với Retention (interplay)

- Export là **bước bắt buộc trước erasure/offboarding** — cung cấp cơ hội rút dữ liệu trước khi xóa.
- Sau khi hard-delete ([`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) §4), dữ liệu không còn export được — phải export trong grace window.
- Audit log mọi export (who/what/when/scope) — immutable, theo §8 retention policy.

> TBD (Phase 2): runbook export tích hợp vào tenant-offboarding + subject-erasure runbooks (ref retention §6/§7).

---

## 6. Tuân thủ pháp lý (Compliance)

- **PDPL L1 (`compliance-scope.md` §2.3 data subject rights):** quyền access + portability — VN-PDPL-8. 🟠 MANDATORY.
- **GDPR Art 20 (extraterritorial):** EU-GDPR-4 right to data portability — machine-readable export, nếu có user EU.
- **MoET (L5):** export học bạ phải đúng format Thông tư 27/2020 (K-12, Phase 3) — xem [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md).

---

## 7. Dependencies / References

- BRD: [`data-retention-deletion-policy.md`](data-retention-deletion-policy.md) §6/§7, [`privacy-policy.md`](privacy-policy.md), [`data-classification-policy.md`](data-classification-policy.md), [`compliance-scope.md`](compliance-scope.md) §2.3, [`moet-regulatory-alignment-matrix.md`](moet-regulatory-alignment-matrix.md)
- Checklist: [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.1 VN-PDPL-8, PART 2 EU-GDPR-4
- ADR-019 (document-generation-architecture)

---

## 8. Out of Scope (this skeleton)

- JSON/CSV export schema per entity (Phase 2 — engineering)
- Export rate-limit / anti-abuse design (Phase 2 — engineering + security)
- K-12 học bạ export format (Phase 3 — MoET alignment)

---

## 9. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P1 batch). Export rights (subject + tenant) + formats + SLA + retention-interplay structure complete; export schemas + rate-limits + exact SLA marked TBD (Phase 2, needs engineering + legal). Cites PDPL L1 (data subject rights, VN-PDPL-8) + GDPR Art 20.
