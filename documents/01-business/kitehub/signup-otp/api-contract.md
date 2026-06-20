# Signup OTP — API Contract

**Domain:** KiteHub auth / mobile signup · Module: `kitehub-subscription`
**Related:** `rules.md` · `use-cases.md` · GAP-286
**Status:** 🟡 Phase 1 — mock delivery. Base path `/api/v1/auth/signup`.

> Verification chain: BR-OTP-* (`rules.md`) → UC-OTP-* (`use-cases.md`) → endpoint dưới → `@PostMapping` → `OtpServiceTest` / `OtpControllerTest`.
>
> **Error envelope:** request-otp lỗi 400/429 dùng RFC 7807 **ProblemDetail** (`type`/`title`/`status`/`detail` + extension field như `error`/`retryAfterSeconds`); verify-otp 400 trả domain body `{ verified:false, reason }`. Endpoints `permitAll` cho `/api/v1/auth/**` (pre-auth signup, no tenant header).

---

## POST /api/v1/auth/signup/request-otp

Sinh + giao mã OTP cho 1 số điện thoại VN (UC-OTP-01).

**Request:**
```json
{ "phone": "0901234567", "channel": "ZALO" }
```
- `phone` (required) — VN format `^0\d{9,10}$` (BR-OTP-005)
- `channel` (optional, default `ZALO`) — `ZALO` | `SMS` (Phase 1 mock cả hai)

**Response 200:**
```json
{ "requestId": "uuid", "channel": "ZALO", "expiresInSeconds": 300, "mock": true }
```

**Errors:**
| HTTP | body | Khi |
|---|---|---|
| 400 | `{ "error": "INVALID_PHONE" }` | Sai định dạng số (BR-OTP-005) |
| 429 | `{ "error": "RATE_LIMITED", "retryAfterSeconds": N }` | >3 request/15 phút/số (BR-OTP-003) |

---

## POST /api/v1/auth/signup/verify-otp

Xác thực mã + cấp `signupToken` (UC-OTP-02).

**Request:**
```json
{ "phone": "0901234567", "code": "123456" }
```

**Response 200:**
```json
{ "verified": true, "signupToken": "<opaque, TTL 600s>" }
```
- `signupToken` — chứng minh sở hữu số cho bước tạo tenant (BR-OTP-007, TTL 10 phút)

**Errors:**
| HTTP | body | Khi |
|---|---|---|
| 400 | `{ "verified": false, "reason": "INVALID_CODE" }` | Mã sai (còn lượt thử) |
| 400 | `{ "verified": false, "reason": "EXPIRED" }` | Mã hết hạn TTL (BR-OTP-002) |
| 400 | `{ "verified": false, "reason": "TOO_MANY_ATTEMPTS" }` | >5 lần sai/mã (BR-OTP-004) |

---

## Phase 2 (out of scope)

- `POST /api/v1/auth/signup/create-tenant` (tiêu thụ `signupToken` → provision TRIAL) — TBD GAP-286 fast-provisioning.
- Live ZNS/SMS delivery + cost telemetry header — TBD GAP-063 Phase 2 (vendor-gated).
