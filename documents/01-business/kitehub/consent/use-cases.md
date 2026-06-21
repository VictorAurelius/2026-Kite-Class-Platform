# Use Cases — Consent v2 (Immutable + hash chain)

**Wave:** loop round 3 (GAP-1516 — 3-layer completeness)
**Service:** `kitehub-subscription`
**Status:** ⚠️ PARTIAL — mô tả runtime use-cases hiện có; counsel formal review queued Phase 2 (GAP-156 AC-D)
**Source rules:** [`rules.md`](rules.md) → canonical `BR-PDPL-CONSENT-001..004` trong [`../marketing/rules.md`](../marketing/rules.md)
**API:** [`api-contract.md`](api-contract.md) — `/api/v1/consent/v2/*` (3 endpoints)

---

## Phạm vi

Consent v2 = **immutable append-only hash-chain** capture cho consent SAU khi user đăng nhập (authenticated `userId`). Khác v1 (`marketing/` visitor_id, upsert-in-place pre-login banner) — xem [`api-contract.md`](api-contract.md) §"Distinction từ v1 path". Co-exist không conflict.

Actors: **Authenticated user** (ghi/rút consent của chính mình) · **Tenant admin / owner** (xem lịch sử consent của user trong tenant cho audit).

---

## UC-CONSENT-V2-01 — Ghi nhận consent (post-login)

**Actor:** Authenticated user (hoặc admin capture flow).
**Tiền điều kiện:** User đã đăng nhập; có `userId`; (tùy chọn) `tenantId`.
**Trigger:** User chọn consent categories (analytics/marketing) sau login, hoặc admin explicit capture.

**Steps:**
1. FE gửi `POST /api/v1/consent/v2/record` với `{userId, tenantId, granted:{essential,analytics,marketing}}` (BR-PDPL-CONSENT-001: server **coerce** `essential=true` bất kể input).
2. Server INSERT row mới vào `consent_record_immutable`; tính `current_hash = SHA-256(prev_hash || canonical(...))`, `prev_hash` = `current_hash` của row mới nhất cùng `userId` (NULL = chain head) — BR-PDPL-CONSENT-003 (schema versioning qua JSONB `granted`).
3. Auto-fill `ipAddress` (từ `X-Forwarded-For` → `RemoteAddr`) + `userAgent` (header) phục vụ non-repudiation.
4. Trả `201 Created` + record (gồm `currentHash`, `signedAt`).

**Kết quả:** consent state mới được ghi append-only; chain liên kết với record trước.

**Errors:** `400 VALIDATION_ERROR` (thiếu `userId` / `granted` rỗng / `ipAddress` rỗng sau auto-fill) · `500 INTERNAL_ERROR` (DB down / hash compute fail — cực hiếm).

**FE behavior:** hiển thị xác nhận "Đã lưu tùy chọn quyền riêng tư"; nếu 400 → highlight field thiếu; KHÔNG cho phép tắt essential (locked-on per BR-PDPL-CONSENT-001).

---

## UC-CONSENT-V2-02 — Xem lịch sử consent + xác thực chuỗi

**Actor:** Authenticated user (chính mình) HOẶC tenant admin/owner (trong tenant scope).
**Tiền điều kiện:** Tồn tại ≥1 consent record cho `userId`; caller có quyền (admin OR owner self).
**Trigger:** User xem trang "Quyền riêng tư của tôi", hoặc admin audit consent.

**Steps:**
1. FE gửi `GET /api/v1/consent/v2/{userId}`.
2. Server đọc toàn bộ history (oldest→newest) + **validate hash chain integrity tại read time**.
3. Trả `200 OK` với `{userId, chainValid:true, records:[...]}`.

**Kết quả:** caller thấy đầy đủ lịch sử consent + bằng chứng chuỗi không bị giả mạo.

**Errors:** `404 NOT_FOUND` (chưa có record cho `userId`) · `500 CHAIN_INTEGRITY_VIOLATION` (validator phát hiện tampering → cần manual audit).

**FE behavior:** render timeline các lần consent (granted categories + `signedAt`); badge "Chuỗi hợp lệ ✓"; nếu 500 integrity → cảnh báo + escalate support (không tự sửa).

---

## UC-CONSENT-V2-03 — Rút lại consent (withdraw)

**Actor:** Authenticated user.
**Tiền điều kiện:** User đã đăng nhập; có `userId`.
**Trigger:** User bấm "Rút lại đồng ý" trong trang quyền riêng tư.

**Steps:**
1. FE gửi `POST /api/v1/consent/v2/withdraw` với `{userId, tenantId}`.
2. Server INSERT row mới với `granted={essential:true, analytics:false, marketing:false}` (BR-PDPL-CONSENT-004: rút dễ như cho — single-call, mirror grant flow; PDPL NĐ13/2023 Điều 14).
3. Trả `201 Created`; row mới nhất phản ánh trạng thái đã rút.

**Kết quả:** consent bị rút qua record mới (KHÔNG flip row cũ — append-only giữ audit trail).

**Errors:** `400 VALIDATION_ERROR` (thiếu `userId`).

**FE behavior:** nút "Rút lại đồng ý" hiển thị ngang tầm nút "Đồng ý" (BR-PDPL-CONSENT-004 — không chôn sâu); xác nhận "Đã rút lại đồng ý".

---

## Concurrency + integrity (tham chiếu)

SERIALIZABLE + REQUIRES_NEW per insert + retry loop (3 lần, backoff 50→100→200ms); 2 thread cùng `userId` → Postgres serialization conflict (40001) → retry → chain không fork (IT proof `ConcurrentConsentWritesIT`). Chi tiết [`api-contract.md`](api-contract.md) §5.

## Related

- [`rules.md`](rules.md) — BR-PDPL-CONSENT-001..004 (canonical `../marketing/rules.md`)
- [`api-contract.md`](api-contract.md) — 3 endpoints + concurrency model
- `../../../02-architecture/adr/ADR-034-cookie-consent-vendor.md`

## Log

- 2026-06-21 — use-cases.md created (GAP-1516 — 3-layer completeness; consent đã có rules.md + api-contract.md, thiếu use-cases.md). 3 UC (record / view-history / withdraw) grounded trong api-contract `/api/v1/consent/v2/*` + BR-PDPL-CONSENT-001..004. Counsel formal review Phase 2 (GAP-156 AC-D).
