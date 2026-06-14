# Cross-Product SSO (KiteHub → KiteClass) — API Contract

**Domain:** Cross-product SSO (ADR-040 Option A)
**Source-of-truth controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/SsoController.java` (`@RequestMapping("/api/v1/auth/sso")`)
**Service:** `.../service/SsoCodeService.java` (Redis one-time-code store)
**Tests:** `SsoControllerTest.java` + `SsoCodeServiceTest.java`
**Last verified:** 2026-06-14 (GAP-1332)

Cả hai endpoint thuộc namespace **kitehub-subscription** (whitelist `/api/v1/auth/**` ở gateway + `SecurityConfig permitAll`). Route FE `/sso/callback` trên KiteClass `:3000` KHÔNG phải BE endpoint — nó gọi `exchange` qua gateway.

> **Versioning:** cụm này nằm dưới `/api/v1/auth/**` (đã versioned) — khác với phần lớn surface kitehub `/api/platform/**` + `/api/auth/**` (unversioned). Xem convention tại `subscription-billing/api-contract.md` §"API versioning convention" (GAP-1338).

---

## POST /api/v1/auth/sso/issue-code

**Use case:** UC-SSO-001 / UC-SSO-003
**Auth:** `Authorization: Bearer <KH access token>` — endpoint **tự validate** (gateway coi path là public). Refresh token bị từ chối.
**Consumer:** KiteHub FE `:3001` (nút "Mở quản lý trường").

**Request:** không có body. Chỉ cần header `Authorization`.

**Response 200 OK** — `SsoIssueCodeResponse`:
```json
{
  "code": "f3aQ...Zk",
  "expiresIn": 60
}
```

| Field | Type | Mô tả |
|---|---|---|
| `code` | string | One-time opaque code (256-bit, URL-safe Base64). Single-use, consume khi exchange. |
| `expiresIn` | number (giây) | TTL còn lại của code (≤60, post-clamp `kitehub.sso.code-ttl-seconds`). |

**Error 401 Unauthorized** (envelope custom — xem §Error envelope):
```json
{ "error": "SSO_UNAUTHORIZED", "message": "<lý do tiếng Việt>" }
```

| Trường hợp | message |
|---|---|
| Thiếu header / sai prefix Bearer | `Yêu cầu Bearer token hợp lệ để phát mã SSO` |
| Token sai chữ ký / hết hạn | `Token không hợp lệ hoặc đã hết hạn` |
| Không phải access token (vd refresh) | `Chỉ access token mới được phát mã SSO` |
| Thiếu subject hợp lệ trong token | `Token thiếu subject hợp lệ` |

---

## POST /api/v1/auth/sso/exchange

**Use case:** UC-SSO-001 / UC-SSO-002 / UC-SSO-004
**Auth:** public — **code chính là credential**. Bắt buộc `Content-Type: application/json` (CSRF guard).
**Consumer:** KiteClass FE `:3000` route `/sso/callback` (gọi qua gateway `:9000`).

**Request body** — `SsoExchangeRequest`:
```json
{ "code": "f3aQ...Zk" }
```

| Field | Type | Ràng buộc |
|---|---|---|
| `code` | string | `@NotBlank`. One-time code từ `issue-code`. |

**Response 200 OK** — `LoginResponse` (KH-minted access + refresh + user):
```json
{
  "user": { "id": "uuid", "email": "owner@truong-abc.edu.vn", "name": "Nguyễn Văn A", "role": "OWNER" },
  "accessToken": "<HS512 JWT>",
  "refreshToken": "<refresh JWT>"
}
```

| Field | Type | Mô tả |
|---|---|---|
| `user.id` | UUID | userId từ tuple identity của code. |
| `user.email` | string | Email gắn vào code khi issue. |
| `user.name` | string | Resolve từ DB (`users.name`); fallback = email nếu row đã bị xoá. |
| `user.role` | string | Role gắn vào code; re-mint vào JWT mới. |
| `accessToken` | string | JWT HS512 ký bằng shared `JWT_SECRET` → gateway-validatable (ADR-039). |
| `refreshToken` | string | Refresh token KH-minted. |

**Error 401 Unauthorized** (code sai / hết hạn / đã consume):
```json
{ "error": "SSO_UNAUTHORIZED", "message": "Mã SSO không hợp lệ hoặc đã hết hạn" }
```

**Error 415 Unsupported Media Type** — request không phải `application/json` (CSRF guard BR-SSO-007).

---

## Error envelope (GAP-1337 cross-service note)

`SsoController` trả lỗi 401 dưới dạng **map custom** `{ "error", "message" }`, KHÔNG phải RFC 7807 `ProblemDetail` như đa số handler khác trong kitehub-subscription. Đây là chủ ý cục bộ (đường public auth, message tiếng Việt cho FE) nhưng tạo bất nhất envelope ở cấp service. Quyết định canonical + lộ trình thống nhất: xem `subscription-billing/api-contract.md` §"Error envelope (cross-service contract)" + GAP-1337.

## Cấu hình

| Key | Default | Ràng buộc |
|---|---|---|
| `kitehub.sso.code-ttl-seconds` | `60` | Clamp `[1..60]`; ngoài khoảng → log warn + clamp (ADR-040 ≤60s). |

## Tham chiếu

- ADR-040 (Option A), ADR-039 (precedent); GAP-1138, GAP-1305
- `rules.md` (BR-SSO-*) + `use-cases.md` (UC-SSO-*) cùng domain
