# Beta Data Reset Policy

**Status:** Active (Phase 1 BETA)
**Created:** 2026-05-14
**Last-Updated:** 2026-05-14
**Owner:** Platform Ops
**Applies to:** Mọi tenant đang ở Phase 1 BETA (kitehub-platform — kitehub.me)
**Replaces:** vague "dữ liệu có thể bị reset" wording trong `BetaDisclaimerBanner.tsx` (Wave 78 GAP-539)

---

## 1. Mục đích

Tài liệu này định nghĩa **chính xác** khi nào, ai, và như thế nào dữ liệu của tenant BETA có thể bị reset. Người dùng beta có thể tham khảo doc này (link từ banner cảnh báo trong dashboard) để hiểu rõ rủi ro trước khi nhập dữ liệu thực tế.

Per outside-in audit Persona 1 (Chị Hằng — P2 Center Owner): banner "dữ liệu có thể bị reset" thiếu specificity → tạo anxiety thay vì calm. Doc này close GAP-560.

---

## 2. Cam kết của KiteHub trong giai đoạn BETA

### 2.1 KHÔNG tự ý reset dữ liệu của tenant

- KiteHub **không** lên lịch reset tự động hằng tuần/hằng tháng cho dữ liệu tenant production-equivalent.
- Mọi reset đều có nguyên nhân được công bố **trước** (xem §3 Reset Triggers).

### 2.2 Báo trước tối thiểu 7 ngày

- Mọi reset có kế hoạch được thông báo qua **email + dashboard banner** ít nhất **7 ngày** trước khi thực hiện.
- Trong trường hợp khẩn cấp (security incident, vi phạm PDPL), tối thiểu **24 giờ** trước khi reset, hoặc đồng thời với incident notification nếu phải thực hiện ngay.

### 2.3 Backup trước reset

- Trước mọi reset có kế hoạch, KiteHub tạo snapshot Postgres + MinIO của tenant.
- Snapshot được giữ **30 ngày** sau reset; tenant có thể yêu cầu restore tới snapshot này qua `support@kitehub.me`.

---

## 3. Reset Triggers — khi nào reset CÓ THỂ xảy ra

| Loại reset | Tần suất | Báo trước | Phạm vi | Backup |
|---|---|---|---|---|
| **Migration breaking change** (vd: schema rewrite cho persona expansion) | Tối đa 1 lần / Phase 1 BETA | 7 ngày email + 14 ngày dashboard banner | Toàn bộ tenant data | ✅ 30 ngày |
| **Pre-GA cutover** (chuyển từ BETA → v1.0.0 GA — chuyển sandbox sang production) | 1 lần duy nhất khi exit BETA | 14 ngày email + 21 ngày dashboard banner + opt-in carry-forward form | Sandbox tenant data | ✅ Tenant tự chọn carry-forward hoặc reset; backup 90 ngày |
| **Security incident** (compromised data, PDPL vi phạm cần cleanse) | Khi cần (hi vọng 0 lần) | Tối thiểu 24h trước, hoặc đồng thời incident notice | Affected tenants only | ✅ Pre-incident snapshot 90 ngày |
| **Tenant tự reset** (qua Settings → Data → Reset Account) | On-demand | Confirmation modal 2-bước | Tenant của chính user | ❌ Tenant chọn xoá vĩnh viễn |
| **Test data only** (tenant tự đánh dấu `is_beta_demo_data=true` trong onboarding) | Khi tenant click "Xoá dữ liệu mẫu" | Confirmation modal | Sample/demo rows only | ❌ |

---

## 4. KHÔNG bao giờ reset

Các bảng/dữ liệu sau **KHÔNG bao giờ** bị reset trong giai đoạn BETA:

- `tenant` row (tenant identity, slug, subscription metadata)
- `subscription` row (gói + billing history)
- `payment` row (lịch sử giao dịch — yêu cầu pháp lý)
- `audit_log` row (compliance trail — PDPL + Cybersecurity 2018)
- `user` row của Owner persona (tránh khoá tài khoản chính)

Lý do: các bảng này thuộc **identity layer**, reset chúng sẽ phá break tenant identity + vi phạm yêu cầu lưu trữ pháp lý (audit log + payment record tối thiểu 5 năm theo Luật Kế toán + Luật Phòng chống rửa tiền).

---

## 5. Quy trình restore từ snapshot

Khi tenant cần restore (vd: lỡ click "Reset Account" hoặc dữ liệu bị mất do migration):

1. Email `support@kitehub.me` với subject `[BETA-RESTORE] <tenant-slug>` trong vòng **30 ngày** từ thời điểm reset.
2. Cung cấp:
   - Tenant slug
   - Email Owner đã xác thực
   - Thời điểm muốn restore tới (snapshot timestamp)
   - Lý do (audit trail)
3. Platform Ops phản hồi trong vòng **24 giờ** (working day) với plan restore.
4. Restore thực hiện trong vòng **72 giờ** từ confirm (downtime ~15-30 phút cho tenant).

Lưu ý:
- Restore **chỉ áp dụng cho tenant data** (lớp học, học viên, lịch học, branding assets). KHÔNG restore audit_log / payment / subscription (các bảng này không bị reset).
- Snapshot >30 ngày sẽ bị xoá tự động — yêu cầu restore quá hạn không khả thi.

---

## 6. Roadmap exit BETA

Phase 1 BETA dự kiến kết thúc khi đạt 3 triggers (per `documents/03-planning/roadmap/release-1-plan-2026.md`):

1. Quality audit /100 ≥ 80
2. 5 beta tenants live ≥ 2 tuần
3. 0 P0 incidents trong 2 tuần liên tiếp

Khi đạt triggers → KiteHub publish **pre-GA cutover plan** (per §3 row 2). Tenant sẽ nhận:
- Email 14 ngày trước cutover
- Dashboard banner 21 ngày trước
- Opt-in form để chọn carry-forward dữ liệu sang production hoặc reset

---

## 7. Câu hỏi thường gặp

### Q: Tôi đã nhập dữ liệu thật vào BETA. KiteHub có xoá không?
**A:** KHÔNG, trừ khi có 1 trong các trigger §3 + báo trước 7-14 ngày. Backup snapshot 30 ngày được giữ cho mọi reset có kế hoạch.

### Q: Nếu KiteHub thay đổi schema (migration breaking) thì sao?
**A:** KiteHub cam kết tránh schema-breaking migration trong BETA. Nếu bắt buộc (vd: lỗi nghiêm trọng cần fix), sẽ báo 7 ngày + backup + có thể support migration tự động cho tenant.

### Q: Dữ liệu mẫu (sample data seed) có bị reset không?
**A:** CÓ — chỉ data có flag `is_beta_demo_data=true` (do tenant tự opt-in trong Onboarding step IMPORT_DATA). Tenant có thể xoá dữ liệu mẫu này bất cứ lúc nào trong Settings.

### Q: Audit log có bị reset không?
**A:** KHÔNG. Audit log lưu vĩnh viễn (yêu cầu PDPL + Luật An ninh mạng 2018).

### Q: Tôi muốn KiteHub xoá toàn bộ dữ liệu của tôi (GDPR/PDPL right-to-erasure).
**A:** Email `privacy@kitehub.me` với subject `[PDPL-DELETE] <tenant-slug>`. Platform Ops xoá trong vòng 30 ngày theo PDPL §17.

---

## 8. Cam kết khi exit BETA → GA

Tại thời điểm chuyển sang v1.0.0 GA:
- Tenant carry-forward sẽ KHÔNG bị reset thêm (BETA reset policy này hết hiệu lực).
- Một `data-retention-policy.md` cho production sẽ thay thế doc này — dự kiến retention 7 năm cho audit_log, 5 năm cho payment, tenant data theo subscription active.

---

## 9. Liên hệ + báo cáo vấn đề

- **Restore request:** `support@kitehub.me` (subject `[BETA-RESTORE]`)
- **PDPL data deletion:** `privacy@kitehub.me`
- **Báo cáo dữ liệu bị mất bất thường:** `support@kitehub.me` + screenshot + tenant slug

KiteHub cam kết phản hồi mọi yêu cầu data-related trong vòng 24 giờ (working day).

---

## 10. Log

- **2026-05-14:** Doc created (Wave 79 Bucket D, GAP-560). Triggered by outside-in audit Persona 1 finding — banner copy "dữ liệu có thể bị reset" vague + tăng anxiety. Doc này link từ `BetaDisclaimerBanner.tsx` để tenant tham khảo specificity (reset cadence, advance notice, backup policy). Companion to `BetaDisclaimerBanner.tsx` content update.
