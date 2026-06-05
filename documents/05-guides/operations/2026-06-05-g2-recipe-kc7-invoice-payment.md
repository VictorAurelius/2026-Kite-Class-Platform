---
title: G2 Human Test Recipe — KC-7 Invoice → Payment record → Reconcile
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign G2 handoff cho KC-7 — enrollment sinh invoice → record payment (tiền mặt/chuyển khoản) → reconcile status (SENT→PARTIAL→PAID)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc7-invoice-payment.md
  - documents/04-quality/gaps/phase-1-beta/closed/GAP-1003-kiteclass-core-gateway-role-authority-bridge-missing.md
---

# G2 Human Test Recipe — KC-7 Invoice → Payment → Reconcile

## Mục tiêu

Owner/giáo viên ghi nhận thanh toán học phí (tiền mặt/chuyển khoản/QR) trên invoice của học sinh → invoice tự chuyển trạng thái `SENT → PARTIAL → PAID` theo số tiền đã trả. Verify fix G1: **GAP-1003 P0** (record-payment trước đây 403 ACCESS_DENIED với MỌI role vì kiteclass-core thiếu filter bridge `X-User-Roles` → Spring authority; nay đã có `GatewayHeaderAuthenticationFilter`).

**Prereq:**
- Stack local UP: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8088/actuator/health` → 200.
- kiteclass-core đã rebuild với GAP-1003 fix (image `kiteclass-core:latest`).
- Data sẵn (tenant `sky-education` `0edaee10-...`): invoices linked enrollment đã sinh sẵn — dùng **invoice 9/10/11/12/13** (status `SENT`, `amount_paid=0`, mỗi invoice có 1 invoice_item = subtotal). ⚠️ Invoice 28/15/14 đã bị G1 walk tiêu thụ (PAID / over-paid / có 2 payment).

**Thời lượng:** ~12 phút.

## ⚠️ Lưu ý quan trọng (contract surprises)

- **record-payment cần `X-User-Roles`** (OWNER/ADMIN/TEACHER/PLATFORM_ADMIN). Thiếu/sai role → **403 ACCESS_DENIED** (đúng — GAP-1003 fix làm `hasAnyRole` hoạt động). Trước fix: 403 với mọi role.
- **invoice tự chuyển status qua `@PreUpdate`** (`Invoice.calculateTotals`): `amount_paid` đủ `total` → `PAID`; một phần → `PARTIAL`. `balance_due` là generated column = `total - amount_paid`.
- **Reconcile = ghi đủ tiền qua record-payment** (status tự lên PAID). KHÔNG cần gọi mark-paid riêng cho happy path.
- **GAP-879 dual-system:** record ghi bảng `payment_records` (V69) + cập nhật `invoices.amount_paid`. `markInvoiceAsPaid` là đường độc lập (ghi đè) — KHÔNG dùng trong recipe này.
- **Endpoint:** `POST /api/v1/invoices/{id}/record-payment` body `{"method":"CASH|BANK_TRANSFER|QR|...","amount":<số>}`; header `Idempotency-Key` optional (⚠️ chưa enforce DB-side — GAP-1004).

## Setup

- Browser + DevTools Network (filter `invoices`).
- DB verify: `docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tA -c "..."`.
- Biến: `SKY=0edaee10-2d13-44be-9151-12b78b7c5fd4`, `UID=11111111-1111-1111-1111-111111111111`. Header chung: `-H X-Tenant-Id:$SKY -H "X-User-Roles: OWNER" -H X-User-Id:$UID -H Content-Type:application/json`.

## Các bước

### Bước 1 — Xem invoice chưa thanh toán

- **Hành động:** Mở danh sách hóa đơn → chọn invoice 9 (học sinh, học phí).
- **✅ Kỳ vọng:** HTTP 200, `status=SENT`, `amountPaid=0`, `balanceDue=total`.
- **🔍 Verify (curl):** `GET "/api/v1/invoices/9"` → 200. (⚠️ endpoint này KHÔNG cần role — GAP-1005 defense-in-depth gap; RLS vẫn chặn cross-tenant.)
- **🔍 DB:** `SELECT id,total,amount_paid,status,balance_due FROM invoices WHERE id=9;`

### Bước 2 — Ghi nhận thanh toán một phần (tiền mặt)

- **Hành động:** Nhấn "Ghi nhận thanh toán" → chọn Tiền mặt → nhập 1 phần (vd 500.000đ) → lưu.
- **✅ Kỳ vọng:** HTTP **201** (trước GAP-1003 fix: **403**). Invoice → `status=PARTIAL`, `amount_paid=500000`, `balance_due=total-500000`. **total KHÔNG bị về 0** (invoice có items, `@PreUpdate` recompute đúng).
- **⚠️ Sad path (role gate — GAP-1003 verify):** gửi cùng request KHÔNG có `X-User-Roles` → **403 ACCESS_DENIED**.
- **🔍 Verify:** `POST "/api/v1/invoices/9/record-payment" -d '{"method":"CASH","amount":500000}'` → 201; `SELECT total,amount_paid,status,balance_due FROM invoices WHERE id=9;` → status PARTIAL.

### Bước 3 — Ghi nhận phần còn lại → reconcile PAID

- **Hành động:** Ghi nhận tiếp số còn lại (chuyển khoản) cho đủ `total`.
- **✅ Kỳ vọng:** HTTP 201; invoice → `status=PAID`, `amount_paid=total`, `balance_due=0` (tự reconcile qua `@PreUpdate`).
- **🔍 Verify:** `POST "/api/v1/invoices/9/record-payment" -d '{"method":"BANK_TRANSFER","amount":<balance còn lại>}'` → 201; invoice status PAID.

### Bước 4 — Xem lịch sử thanh toán

- **Hành động:** Mở tab "Lịch sử thanh toán" của invoice.
- **✅ Kỳ vọng:** 2 dòng (CASH + BANK_TRANSFER), tổng = total. ⚠️ `recordedBy` hiện hardcode 1 (GAP-526) — tên người thu chưa đúng.
- **🔍 Verify:** `GET "/api/v1/invoices/9/payment-records"` → 200, 2 records.

## Sad path quick checks (tổng hợp)

- **Role gate (GAP-1003):** record-payment thiếu `X-User-Roles` → **403**. (Đây là điểm verify chính của G1 fix.)
- **Over-payment (GAP-1004 đã biết):** dùng invoice khác (vd 10), record amount > total → hiện **201** + `balance_due` âm (chưa clamp). Quan sát — đã có GAP-1004.
- **Idempotency (GAP-1004 đã biết):** POST 2 lần cùng `Idempotency-Key` → tạo **2** payment_records (chưa enforce). Quan sát.
- **Cách ly tenant (RLS):** `GET /api/v1/invoices/1` (invoice tenant khác) với header `X-Tenant-Id:$SKY` → **404** (RLS chặn). ✅
- **InvoiceController authz (GAP-1005 đã biết):** `GET /api/v1/invoices/9` không cần role → 200 (defense-in-depth gap; financial mutation mark-paid/cancel cũng chưa gate).

## Báo kết quả

**Khi G2 xong, báo lại 1 trong 4:**
- ✅ **FULL PASS** → Claude xác nhận KC-7 G1+G2, chờ G3.
- ⚠️ **MOSTLY PASS** với cosmetic (over-payment/idempotency/recordedBy) → đã có GAP-1004 + GAP-526.
- 🔴 **BLOCKING** (record-payment 403 dù có role / status không reconcile / total về 0) → catalog + fix loop.
- ❓ **UNCLEAR** → ping screenshot + Network tab.

## Troubleshooting + G3 preview

| Triệu chứng | Fix nhanh |
|---|---|
| record-payment 403 dù có `X-User-Roles` | kiteclass-core chưa rebuild với GAP-1003 fix — `bash kitehub/scripts/rebuild.sh kiteclass-core` |
| record-payment 403 (đúng — thiếu role) | Thêm header `X-User-Roles: OWNER` (gateway thật forward role từ JWT) |
| total về 0 sau record-payment | invoice không có invoice_items → `@PreUpdate` zero-out (bug #1 — chưa xảy ra cho invoice có items; nếu gặp → catalog) |
| status không chuyển PARTIAL/PAID | kiểm `amount_paid` vs `total` trong DB; `@PreUpdate updateStatus` chỉ chạy khi save |
| invoice không tồn tại (404) | dùng invoice 9-13 (SENT); 28/15/14 đã tiêu thụ ở G1 |

**G3 (production parity, post AWS restore):** multi-tenant payment isolation thật + over-payment clamp (GAP-1004) + InvoiceController @PreAuthorize (GAP-1005) + idempotency enforce DB-side + QR/webhook reconcile (Phase 1.5 GAP-625/636) — chưa walk.
