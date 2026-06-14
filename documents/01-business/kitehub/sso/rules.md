# Cross-Product SSO (KiteHub → KiteClass) — Business Rules

**Domain:** Cross-product SSO — owner/staff điều hướng từ KiteHub `:3001` sang KiteClass owner-shell `:3000` **không phải đăng nhập lại**.
**Source-of-truth code:** `kitehub/kitehub-subscription/.../controller/SsoController.java` + `.../service/SsoCodeService.java`
**Decision record:** [ADR-040 — Cross-Product SSO KiteHub→KiteClass](../../../02-architecture/adr/ADR-040-cross-product-sso-kh-kc.md) (ACCEPTED, Option A)
**Last verified:** 2026-06-14 (GAP-1332 — 3-layer doc cho cluster `/api/v1/auth/sso/**`)

---

## Bối cảnh

Theo ADR-040 + GAP-1119, **OWNER/STAFF** đăng nhập ở KiteHub (credential trong bảng `users` của KiteHub), còn nghiệp vụ quản lý trường (course/class, payroll, billing) nằm ở KiteClass owner-shell `:3000`. SSO cho phép owner/staff sau khi login KH bước thẳng vào KC mà không nhập lại mật khẩu — dùng cơ chế **redirect + one-time exchange code** (Option A), KHÔNG đặt JWT thô trên URL.

Luồng: KH FE gọi `issue-code` → nhận opaque code → redirect `:3000/sso/callback?code=...` → KC FE gọi `exchange` (qua gateway) → nhận KH-minted JWT (HS512, shared `JWT_SECRET`) → gateway validate + inject `X-Tenant-Id` → KC session thiết lập.

---

## Business Rules

| ID | Rule | Config key / nguồn |
|---|---|---|
| **BR-SSO-001** | Owner/Staff dùng one-time-code SSO để vào KiteClass owner-shell mà không đăng nhập lại. Teacher/Parent/Student KHÔNG dùng SSO (login KC-native trực tiếp). | ADR-040 §Decision |
| **BR-SSO-002** | Code là **single-use**: đọc + xoá nguyên tử bằng Redis `GETDEL` (`getAndDelete`). Lần exchange thứ hai với cùng code → reject (401). | `SsoCodeService.consumeCode` |
| **BR-SSO-003** | Code TTL tự hết hạn ≤ 60 giây. Giá trị cấu hình được **clamp về [1..60]s**; ngoài khoảng → log warn + clamp. | `kitehub.sso.code-ttl-seconds` (default `60`, max `60`) |
| **BR-SSO-004** | Code entropy 256-bit (`SecureRandom`, 32 byte), encode URL-safe Base64 (không padding). | `SsoCodeService.generateCode` |
| **BR-SSO-005** | **KHÔNG lưu JWT thô** trong store — chỉ lưu tuple identity `(userId, email, role)`. JWT được **re-mint mới** tại thời điểm exchange để claim tenant + tier phản ánh trạng thái DB hiện tại. | `SsoCodeService` javadoc |
| **BR-SSO-006** | `issue-code` yêu cầu **access token hợp lệ** của KiteHub (Bearer). Refresh token → reject. Vì gateway coi `/api/v1/auth/**` là public (không validate JWT, không inject `X-User-*`), endpoint **tự validate** chữ ký HS512 + expiry qua `JwtKeyService`. | `SsoController.issueCode` |
| **BR-SSO-007** | `exchange` bắt buộc `Content-Type: application/json` (CSRF guard). Cross-site `<form>` auto-submit chỉ tạo được `x-www-form-urlencoded`/`multipart`/`text-plain` → 415 trước khi tới handler. | `SsoController.exchange` (`consumes = application/json`) + `SsoExchangeRequest` |
| **BR-SSO-008** | Store **fail-loud** (KHÔNG fail-open như refresh-token blacklist): Redis outage → issue/exchange fail rõ ràng, SSO tạm không khả dụng. Fallback = login KC-native dual-path tại `:3000` bằng credential KH. | `SsoCodeService` javadoc + ADR-040 §Beta unblock |
| **BR-SSO-009** | JWT phát ra ở exchange ký HS512 bằng `JWT_SECRET` dùng chung → gateway `TenantHeaderGuardFilter` verify + inject header (tiền lệ ADR-039). KHÔNG quản lý key riêng cho KiteClass. | ADR-040 §Decision + ADR-039 |

---

## Verification chain

```
BR-SSO-001..009
  → UC-SSO-001..005 (use-cases.md)
  → POST /api/v1/auth/sso/issue-code  (SsoController.java:81)
  → POST /api/v1/auth/sso/exchange    (SsoController.java:129)
  → SsoControllerTest.java + SsoCodeServiceTest.java
```

## Tham chiếu

- ADR-040 — Cross-Product SSO (Option A redirect + one-time-code)
- ADR-039 — Cross-Service Subscription Tier Propagation (precedent gateway shared-secret inject)
- GAP-1138 — SSO KH→KC implementation; GAP-1305 — SSO owner-seed deterministic walk
- `use-cases.md` + `api-contract.md` (cùng domain)
