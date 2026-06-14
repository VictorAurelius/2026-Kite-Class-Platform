# Cross-Product SSO (KiteHub → KiteClass) — Use Cases

**Domain:** Cross-product SSO (ADR-040 Option A)
**Source-of-truth code:** `SsoController.java` + `SsoCodeService.java` (kitehub-subscription)
**Last verified:** 2026-06-14 (GAP-1332)

Xem `rules.md` cho business rules (BR-SSO-*) và `api-contract.md` cho endpoint contract.

---

## UC-SSO-001 — Owner/Staff vào KiteClass owner-shell không đăng nhập lại (happy path)

**Actor:** Owner / Staff (đã đăng nhập KiteHub `:3001`)
**Tiền điều kiện:** Có access token KH hợp lệ (Bearer trong session FE).

**Các bước:**
1. Owner bấm nút "Mở quản lý trường" trên dashboard KiteHub.
2. KH FE gọi `POST /api/v1/auth/sso/issue-code` kèm `Authorization: Bearer <KH-JWT>`.
3. BE validate token (HS512 + expiry + type=access), mint one-time code (TTL ≤60s), trả `{ code, expiresIn }`.
4. KH FE redirect browser tới `:3000/sso/callback?code=<code>`.
5. KC FE (route `/sso/callback`) gọi `POST /api/v1/auth/sso/exchange` với body `{ code }` (qua gateway `:9000`).
6. BE consume code nguyên tử (GETDEL), re-mint access + refresh token, trả `LoginResponse`.
7. KC FE lưu token; gateway verify JWT + inject `X-Tenant-Id` cho các request sau → KC session thiết lập.
8. Redirect tới `/dashboard` (owner-shell).

**Kết quả:** Owner ở trong KiteClass owner-shell mà không nhập lại mật khẩu.
**Rules:** BR-SSO-001, BR-SSO-002, BR-SSO-005, BR-SSO-009.

---

## UC-SSO-002 — Code không hợp lệ / hết hạn / đã dùng (sad path)

**Actor:** KiteClass FE (callback) hoặc kẻ tấn công replay.
**Các bước:**
1. Gọi `exchange` với code đã consume (replay), hết TTL (>60s), hoặc sai.
2. `consumeCode` trả empty (GETDEL không thấy key).
3. BE trả **401** `{ "error": "SSO_UNAUTHORIZED", "message": "Mã SSO không hợp lệ hoặc đã hết hạn" }`.

**Kết quả:** Không thiết lập session. **Rules:** BR-SSO-002, BR-SSO-003.

---

## UC-SSO-003 — issue-code với token thiếu/sai/refresh (sad path)

**Actor:** Client gọi issue-code không đúng.
**Các bước:**
1. Gọi `issue-code` thiếu header `Authorization`, sai prefix Bearer, token sai chữ ký/hết hạn, hoặc là **refresh token** (không phải access).
2. BE reject với **401** + message tiếng Việt tương ứng (xem api-contract.md error table).

**Kết quả:** Không phát code. **Rules:** BR-SSO-006.

---

## UC-SSO-004 — Chống CSRF tại exchange (sad path)

**Actor:** Trang web bên thứ ba auto-submit `<form>` tới `exchange`.
**Các bước:**
1. Forged form gửi `Content-Type: application/x-www-form-urlencoded` (hoặc multipart/text-plain).
2. `consumes = application/json` không khớp → Spring trả **415 Unsupported Media Type** trước khi vào handler.

**Kết quả:** Request giả bị chặn. **Rules:** BR-SSO-007.

---

## UC-SSO-005 — Redis outage (fail-loud)

**Actor:** Owner/Staff khi Redis (`kite-redis`) gặp sự cố.
**Các bước:**
1. `issue-code` / `exchange` gọi Redis → lỗi (KHÔNG fail-open).
2. SSO convenience path tạm không khả dụng.
3. Owner/Staff dùng **fallback**: login KC-native dual-path tại `:3000` bằng credential KH (re-enter password) — ADR-040 §Beta unblock.

**Kết quả:** Không mint session không verify được; có đường dự phòng. **Rules:** BR-SSO-008.
