---
status: active
audience: legal-tenant-facing
last-updated: 2026-05-26
wave: beta-prep-1-bucket-A
---

# Legal Documents — KiteHub Phase 1 BETA

Tài liệu pháp lý tenant-facing cho Phase 1 BETA, áp dụng quy định PDPL 2023 + Nghị định 13/2023/NĐ-CP (Decree 13).

> ⚠️ **Phase 1 BETA disclaimer:** Mọi tài liệu trong folder này là **v1 pending counsel review** — chưa qua luật sư chuyên ngành dữ liệu cá nhân. Risk tolerance Moderate per `documents/03-planning/roadmap/release-1-plan-2026.md` (solo-dev mode, no legal counsel engaged Phase 1). Phase 2 sẽ engage counsel cho counsel-reviewed legal docs.

## Nội dung

| File | Audience | Mô tả |
|---|---|---|
| [`privacy-notice.md`](privacy-notice.md) | Tenant chuẩn bị đăng ký + đã đăng ký | Thông báo bảo mật dữ liệu cá nhân — danh mục dữ liệu thu thập, thời gian lưu trữ, quyền của chủ thể dữ liệu, contact DPO |
| [`terms-of-service.md`](terms-of-service.md) | Tenant chuẩn bị đăng ký | Điều khoản sử dụng dịch vụ — phạm vi dịch vụ, nghĩa vụ tenant, billing, chấm dứt, governing law |
| [`data-retention-policy.md`](data-retention-policy.md) | Tenant + dev nội bộ | Chính sách lưu trữ dữ liệu — per-domain retention period theo PDPL Art 11 + Luật Kế toán |

## Cross-link

- **Compliance source:** `documents/04-quality/compliance/pdpl-pre-launch-checklist.md`
- **Cookie consent (related, separate domain):** `documents/01-business/cookie-consent/`
- **Breach response runbook:** `documents/05-guides/operations/breach-notification-sop.md`
- **Wave context:** Wave beta-prep-1 Bucket A (PDPL compliance-min 4 items, PDPL hard deadline 2026-07-01)

## VN-localization

Tất cả tài liệu trong folder này tuân theo `.claude/rules/vn-localization-audit-checklist.md` §2 4-section checklist:
- §1 VND currency + date format (`Thứ Hai, 26/05/2026` + `1.500.000đ`)
- §2 Vietnamese label (greeting persona-appropriate)
- §3 VN sample data (`Trung tâm Anh ngữ Sky Education`, `Trần Thị Hồng`)
- §4 VN cultural awareness (Zalo communication, niên khóa 9-5, Mon-Sat)

Per `.claude/rules/dev-readable-doc-language.md` §2: narrative tiếng Việt, identifier code/path English natural.
