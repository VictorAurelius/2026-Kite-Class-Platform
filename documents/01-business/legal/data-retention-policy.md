---
status: active
audience: tenant-dev
last-updated: 2026-05-26
version: v0.9.0-beta (Phase 1 BETA)
wave: beta-prep-1-bucket-A
gaps: [GAP-PDPL-COMPLIANCE-MIN]
---

# Chính sách lưu trữ dữ liệu (Data Retention Policy)

> ⚠️ **Phase 1 BETA v1 pending counsel review** — bản đầu để đáp ứng PDPL deadline 2026-07-01. Phase 2 counsel-review.

**Cập nhật lần cuối:** Thứ Ba, 26/05/2026 · Phiên bản KiteHub: v0.9.0-beta · Đọc khoảng **3 phút**

---

## TL;DR

KiteHub lưu trữ dữ liệu theo từng loại với thời hạn cụ thể, tuân thủ:

- **PDPL 2023 + Nghị định 13/2023/NĐ-CP** (Decree 13) — bảo vệ dữ liệu cá nhân
- **Luật Kế toán 2015 Art 41** — hồ sơ tài chính 10 năm
- **Luật An ninh mạng 2018 + Nghị định 53/2022/NĐ-CP** — audit log 5 năm

Hết thời hạn → dữ liệu xóa bất hồi quy (irreversible) theo schedule purge job hàng tuần.

---

## 1. Bảng thời hạn lưu trữ per-domain

| Loại dữ liệu | Thời hạn lưu | Cơ sở pháp lý | Hành động sau hết hạn |
|---|---|---|---|
| **Tài khoản người dùng** (sau khi deactivate) | 7 năm | Luật Kế toán 2015 Art 41 (liên quan hóa đơn) | Xóa bất hồi quy (anonymize log refs) |
| **Audit log** (login, admin action, immutable consent) | 5 năm | PDPL Art 11 + ND-53/2022 Art 26 (audit log) | Xóa bất hồi quy (table `admin_audit_log` + `login_audit_log` + `consent_record_immutable`) |
| **Hồ sơ kế toán + hóa đơn** | 10 năm | Luật Kế toán 2015 Art 41 | Lưu offline (S3 Glacier cold tier); cấp lại theo yêu cầu cơ quan thuế |
| **Đồng ý marketing** | 1 năm sau ngày đồng ý hoặc rút lại đồng ý | PDPL Art 11 (đồng ý phải tái xác nhận định kỳ) | Re-prompt user đồng ý mới hoặc xóa |
| **Cookie consent** | 12 tháng | Cookiebot/Osano industry standard + PDPL Art 11 | Re-show banner khi user truy cập sau 12 tháng |
| **Dữ liệu học sinh + phụ huynh** (Tenant Data Controller) | Theo chính sách Tenant; mặc định 7 năm sau khi tenant chấm dứt | Tenant chịu trách nhiệm; KiteHub backup default 7 năm | Theo yêu cầu Tenant; mặc định xóa sau 7 năm |
| **Backup database** (full snapshot) | 30 ngày | Disaster recovery best practice | Rotation xóa snapshot > 30 ngày |
| **Application log** (DEBUG/TRACE) | 24 giờ | Logs format standard (`logs-format-standard.md`) | Auto-purge khỏi log aggregator |
| **Application log** (INFO/WARN/ERROR) | 30 ngày | Logs format standard hot tier | Rotation warm tier 8-30 ngày, cold tier 31-180 ngày, xóa > 180 ngày |
| **PII trong log** (sau khi scrub) | N/A — không lưu trữ plaintext | PII scrubber `logs-format-standard.md` §3 | Scrub at source, không thể recover |
| **Email transactional log** (sent/delivered metadata) | 1 năm | Audit + bounce tracking | Xóa sau 1 năm; nội dung email không lưu trữ |
| **File upload** (avatar, document) | Trong khi tài khoản active + 90 ngày grace | Liên kết với tài khoản | Xóa khi tài khoản deactivate + 90 ngày |
| **Tenant CSV export** (request từ Tenant) | 7 ngày sau khi tạo | Tenant download window | Auto-purge CSV export sau 7 ngày |
| **Khóa tài khoản (failed login attempt) state** | 24 giờ | Account lockout policy `pre-launch-auth-hardening-checklist.md` §2.2 | Auto-reset counter sau 24 giờ |

## 2. Quy trình xóa dữ liệu (Purge process)

### 2.1 Schedule purge job

KiteHub vận hành purge job hàng tuần (mỗi Chủ Nhật 02:00 GMT+7) quét các bảng:

```
- users (deactivated > 7 năm) → DELETE
- admin_audit_log (created_at > 5 năm) → DELETE
- login_audit_log (created_at > 5 năm) → DELETE
- email_logs (sent_at > 1 năm) → DELETE
- consent_records (expired > 12 tháng + 30d grace) → DELETE
- cookie_consents (expired > 12 tháng) → DELETE
- minio storage (orphan files > 90 ngày) → DELETE
- temp_csv_exports (created_at > 7 ngày) → DELETE
```

### 2.2 User-initiated xóa (Right to be forgotten — PDPL Art 11)

Khi user yêu cầu xóa qua [support@kitehub.me](mailto:support@kitehub.me):

1. **Xác minh danh tính:** confirm qua email đã đăng ký
2. **Đánh giá scope:** xác định loại dữ liệu user yêu cầu xóa
3. **Soft-delete trong 24 giờ:** user account `deleted_at` flag, dữ liệu cá nhân ẩn khỏi UI
4. **Hard-delete trong 30 ngày:** dữ liệu cá nhân xóa thật khỏi DB (PII fields anonymize)
5. **Giữ lại:** audit log + hồ sơ kế toán theo §1 (không thể xóa do nghĩa vụ pháp lý)
6. **Confirm với user:** email confirmation khi hoàn tất

### 2.3 Tenant chấm dứt dịch vụ

Khi tenant chấm dứt:

1. **30 ngày grace export window:** Tenant download CSV qua [support@kitehub.me](mailto:support@kitehub.me)
2. **90 ngày grace reactivation window:** Tenant có thể đăng ký lại để khôi phục workspace
3. **Sau 90 ngày:** workspace xóa khỏi production (chuyển archive cold tier S3 Glacier)
4. **Sau 7 năm archive:** xóa bất hồi quy

## 3. Ngoại lệ giữ dữ liệu lâu hơn

KiteHub giữ dữ liệu lâu hơn §1 trong các trường hợp:

- **Legal hold:** đang tranh chấp pháp lý đang diễn ra
- **Investigation:** đang điều tra sự cố an ninh
- **Cơ quan nhà nước yêu cầu:** Bộ Công an A05, Cơ quan thuế, Tòa án có thẩm quyền
- **Anonymized data:** dữ liệu đã anonymize (không thể nhận dạng cá nhân) giữ vô thời hạn cho mục đích thống kê/cải thiện sản phẩm

## 4. Quyền của user theo PDPL Art 11

User có thể yêu cầu:

- **Truy cập** danh mục dữ liệu cá nhân của mình — phản hồi trong 30 ngày
- **Chỉnh sửa** thông tin sai
- **Xóa** dữ liệu (right to be forgotten) — trong 30 ngày, trừ ngoại lệ §3
- **Hạn chế xử lý** một số loại dữ liệu
- **Chuyển dữ liệu** dạng CSV trong 30 ngày
- **Rút lại đồng ý** marketing/analytics bất kỳ lúc nào

Liên hệ: [support@kitehub.me](mailto:support@kitehub.me)

## 5. Tham chiếu

- `documents/01-business/legal/privacy-notice.md` — Thông báo bảo mật chi tiết
- `documents/01-business/legal/terms-of-service.md` — Điều khoản sử dụng
- `documents/05-guides/operations/breach-notification-sop.md` — Sự cố an ninh dữ liệu
- `documents/05-guides/operations/audit-log-retention-runbook.md` — Audit log retention chi tiết
- `documents/01-business/cookie-consent/rules.md` — Cookie consent BR-COOKIE-001 to BR-COOKIE-004
- `.claude/rules/logs-format-standard.md` §4 — Application log retention tier policy
- **PDPL 2023:** https://thuvienphapluat.vn/van-ban/Cong-nghe-thong-tin/Luat-Bao-ve-du-lieu-ca-nhan-2023-562196.aspx
- **Nghị định 13/2023/NĐ-CP:** Decree triển khai PDPL
- **Luật Kế toán 2015:** https://thuvienphapluat.vn/van-ban/Ke-toan-Kiem-toan/Luat-ke-toan-2015-298757.aspx

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 🐛 Báo lỗi tài liệu này: [support@kitehub.me?subject=Lỗi tại /data-retention](mailto:support@kitehub.me?subject=Lỗi%20tại%20/data-retention)

**Phiên bản:** v0.9.0-beta v1 pending counsel review · **Cập nhật:** Thứ Ba, 26/05/2026 · **Wave:** beta-prep-1 Bucket A
