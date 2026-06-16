---
title: G2 Human Test Recipe — KC-2 Staff invitation + RBAC
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign G2 handoff cho luồng KC-2 (Owner mời staff → accept → RBAC)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-05-flow-kc2-staff-invitation-rbac.md
---

# G2 Recipe — KC-2 Staff invitation + RBAC

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn (Owner) tự test chuỗi: mời nhân viên → nhận email → invitee accept + đặt password → STAFF login → xác nhận STAFF có quyền hạn đúng (thuộc tenant, không truy cập được endpoint owner-only).

**Prereq:** Stack UP (kitehub-subscription rebuilt 2026-06-05 với fix FM-1/GAP-981). Tenant `sky-education`, Owner `owner@skyedu.vn` / `SkyEdu@2026`.

**Thời lượng:** ~10-12 phút.

## 2. Setup

- Browser + DevTools → Network tab (filter `staff`).
- MailHog UI: `http://localhost:8025` (xem email mời).
- (Tùy chọn) verify DB:
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub \
    -c "SELECT email, status, tenant_id FROM staff_invitations ORDER BY created_at DESC LIMIT 3;"
  ```

## 3. Các bước

### Bước 1 — Owner login + mở trang mời nhân viên
- **Hành động:** `http://localhost:3001` → login `owner@skyedu.vn` / `SkyEdu@2026` → vào `/admin/staff/invite` (hoặc menu "Nhân viên" → "Mời").
- **✅ Kỳ vọng:** Form mời hiển thị; trường role là **read-only "Nhân viên trung tâm (STAFF)"** (không dropdown — GAP-784).

### Bước 2 — Gửi lời mời
- **Hành động:** Nhập email (vd `nhanvien1@skyedu.vn`) + họ tên → Gửi.
- **✅ Kỳ vọng:** Toast thành công; Network `POST /api/v1/staff-invitations` → **HTTP 201**.
- **⚠️ Sad path:** Họ tên 1 ký tự → 400 `INVALID_FULL_NAME`. Email đã mời (PENDING) → 409 (idempotency).

### Bước 3 — Nhận email
- **Hành động:** Mở MailHog `http://localhost:8025` → email "Bạn được mời tham gia...".
- **✅ Kỳ vọng:** Email tới đúng địa chỉ + có link accept.
- **⚠️ Lưu ý local:** Link trong email trỏ `https://kitehub.me/staff/accept-invite?token=...` (prod domain). **Local: thay `https://kitehub.me` bằng `http://localhost:3001`**, giữ nguyên `?token=...`.

### Bước 4 — Invitee accept + đặt password
- **Hành động:** Mở link accept (đã đổi sang localhost) → đặt password (≥12 ký tự) → Submit.
- **✅ Kỳ vọng:** `GET /by-token/{token}` → 200; `POST /{token}/accept` → 200 + màn success + CTA login.

### Bước 5 — STAFF login + verify RBAC
- **Hành động:** Login `nhanvien1@skyedu.vn` + password vừa đặt.
- **✅ Kỳ vọng:** Login OK; STAFF vào được dashboard tenant (thuộc sky-education — FM-1 fix).
- **🔍 Verify (quan trọng):** STAFF KHÔNG mời được staff khác (menu mời ẩn hoặc 403). Nếu STAFF gọi `POST /api/v1/staff-invitations` → **403 Forbidden** (RBAC enforce).

## 4. Sad path quick checks
- Họ tên rỗng/1 ký tự → 400 INVALID_FULL_NAME.
- Token hết hạn (>7 ngày) hoặc đã dùng → 410 GONE.
- STAFF → owner-only endpoint → 403 (không phải 201).

## 5. Báo kết quả
Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** → Claude flip campaign → ✅ G1+G2 chờ G3.
- ⚠️ **MOSTLY PASS** (cosmetic, vd email subject/domain) → catalog gap polish.
- 🔴 **BLOCKING** (STAFF không thuộc tenant / mời được staff khác / accept fail) → catalog + fix loop.
- ❓ **UNCLEAR** → ping kèm screenshot + Network error.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|---|---|
| Email link 404 (kitehub.me) | Thay domain sang `http://localhost:3001`, giữ `?token=` |
| STAFF login JWT thiếu tenantId | kitehub-subscription chưa rebuild fix GAP-981 → `docker-compose build kitehub-subscription` |
| STAFF mời được staff khác (không 403) | RBAC chưa enforce → báo blocker |

**G3 production parity (Phase tiếp):** staff-invitation schema migrate sạch RDS + email gửi thật SES (link prod-domain đúng khi prod) + gateway JWT→header + RBAC enforce đúng tenant scope. Gated GAP-612 (AWS).


---

## 🔄 Re-walk update 2026-06-16 (agent headless browser-walk, nip.io)

- **Verdict:** ⚠️ mostly-pass (RBAC solid 3 lớp, 0 blocker).\n- **FM-1 FIXED:** email accept link nay trỏ `http://localhost:3001` (env KITEHUB_STAFF_INVITATION_BASE_URL, PR #2456).\n- **Idempotency:** re-invite PENDING trả **201** (auto-revoke), KHÔNG phải 409 như recipe cũ ghi.\n- **Credential:** owner@skyedu.vn/SkyEdu@2026 (PREMIUM owns sky-education). CẤM trap `owner.sky@test.vn` (403 TENANT_CONTEXT_MISSING).\n- Platform-side `:3001` (không nip.io).\n- Defer: STAFF dashboard owner-widget 403 (GAP-1459 Phase 2), /docs 404 (GAP-1460), email org stub Phase 1.5.
