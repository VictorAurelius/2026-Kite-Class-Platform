# Tenant Auth (KC-native login) — Business Rules

**Domain:** KiteClass Core / Tenant Auth
**Version:** 1.0 (Wave auth-1 — KC-native login Option B, GAP-725/798)
**Updated:** 2026-06-06
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/auth/`

---

## 1. Scope

KiteClass-core tự cấp (mint) access token cho các role tenant-scoped mà KiteHub subscription **không** phát hành token: `PARENT`, `TEACHER`, `STUDENT`. Đây là "Option B" (pull-forward Phase 1 theo GAP-725) — thay thế thiết kế "Option A" cũ (credential + JWT ở Gateway `users` table, liên kết qua `users.reference_id`).

Tài liệu này phủ:
- Entity `AuthCredential` (`auth_credentials` table) — credential standalone, pre-auth lookup theo email.
- `AuthService` (BCrypt verify, uniform 401 no-enumeration) + `AuthController` (`POST /api/v1/tenant-auth/login`).
- `AuthTokenService` (HS512 JWT mint, shared `JWT_SECRET`).
- `AuthCredentialProvisioningService` (provision/upsert credential cho parent redeem + teacher/student admin set-password).
- Gateway anti-spoof `X-User-Reference-Id` (strip client header + re-inject từ verified claim).

**Không** thuộc tài liệu này: OWNER/STAFF login (KiteHub subscription, route `/api/v1/auth/**`); per-domain profile rules (xem `parent-portal/`, `student-portal/`, `teacher/`).

> **Dual-path login tại KC FE `:3000` (reconcile 2026-06-12):** OWNER/STAFF vẫn login được
> trên trang login KiteClass — KC FE (`auth.ts`) thử tenant-auth trước, 401 → fallback sang
> KH `/api/auth/login` (credential bảng `users` KiteHub). `auth_credentials` + BR-AUTH-002
> KHÔNG đổi (OWNER/STAFF vĩnh viễn không nằm trong table này). Chi tiết + credential matrix:
> `documents/02-architecture/adr/ADR-040-cross-product-sso-kh-kc.md` §Beta-unblock.

> **Quan hệ Option A → Option B:** Mọi reference tới Option A (Gateway `users` table, `users.reference_id`) trong `parent-portal/` + `student-portal/` đã được đánh dấu superseded. `referenceId` giờ = `auth_credentials.entity_id`, mint trực tiếp bởi Core, gateway chỉ re-inject từ JWT claim — không còn cross-service population.

---

## 2. Credential Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-AUTH-001 | Credential standalone, pre-auth lookup | `auth_credentials` **không** kế thừa `BaseEntity` và **không** RLS-scoped: login lookup chạy PRE-auth (chưa có `TenantContext` / GUC `app.current_tenant_id`). Row credential CHÍNH LÀ nguồn tenant binding. Lookup qua `findByEmailIgnoreCase` (`AuthCredentialRepository`). **Code:** `AuthCredential.java` (javadoc "Intentionally NOT extending BaseEntity") + `V89__create_auth_credentials.sql:4-7`. |
| BR-AUTH-002 | `entity_type` ∈ {PARENT, TEACHER, STUDENT} | DB CHECK constraint `ck_auth_credentials_entity_type CHECK (entity_type IN ('PARENT','TEACHER','STUDENT'))`. Maps tới JWT claim `role`. Constants: `AuthCredentialProvisioningService.ROLE_PARENT/ROLE_TEACHER/ROLE_STUDENT`. OWNER/STAFF KHÔNG nằm trong table này. **Code:** `V89:22`. |
| BR-AUTH-003 | `entity_id` = referenceId | `entity_id` (BIGINT) = id domain row của kiteclass (`parents.id` / `teachers.id` / `students.id`). Đây là giá trị forward thành `X-User-Reference-Id` (gateway) → tiêu thụ bởi `@authz.hasAccessToChild` (GAP-798 reference-id authz). **Code:** `AuthCredential.java` field `entityId` + `AuthTokenService.java` claim `referenceId`. |
| BR-AUTH-004 | Email globally-unique | Unique constraint `uk_auth_credentials_email UNIQUE(email)` — email lookup là pre-auth (chưa biết tenant). Giới hạn hiện tại Phase 1: 1 email = 1 credential = 1 tenant. **Lưu ý xung đột:** mâu thuẫn BR-PARENT-001 (cùng email hợp lệ ở nhiều tenant). Theo dõi follow-up P1 multi-tenant email collision — Phase 1 chấp nhận giới hạn này, quyết định lookup `(instance_id, email)` defer. **Code:** `V89:20`. |
| BR-AUTH-005 | BCrypt password hash | `BCryptPasswordEncoder` (cost mặc định 10). Cột `password_hash VARCHAR(72)` đủ cho 60-char BCrypt hash. KHÔNG dùng MD5/SHA1. **Code:** `AuthService.java` + `AuthCredentialProvisioningService.java`. |
| BR-AUTH-006 | `user_uuid` = audit identity | UUID ổn định, unique (`uk_auth_credentials_user_uuid`), forward thành `sub` / `X-User-Id` cho audit trail. Sinh tại provisioning, không đổi. **Code:** `AuthCredential.java` field `userUuid`. |
| BR-AUTH-007 | `instance_id` = tenant binding | UUID tenant, maps JWT claim `tenantId`. Vì lookup pre-auth không có tenant context, instance_id lưu trực tiếp trên credential row. **Code:** `AuthCredential.java` field `instanceId`. |
| BR-AUTH-008 | `enabled` gate | `enabled BOOLEAN DEFAULT TRUE`. Credential `enabled=false` → login trả 401 (uniform). Phase 1: chưa có path tự động set false khi entity bị deactivate/soft-delete — theo dõi follow-up P2. **Code:** `AuthCredential.java` field `enabled`, `AuthService.java` filter `AuthCredential::isEnabled`. |

---

## 3. Authentication Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-AUTH-LOGIN-001 | Uniform 401 no-enumeration | Unknown email / disabled credential / wrong password đều trả `401 INVALID_CREDENTIALS` với cùng message — không tiết lộ tài khoản tồn tại hay không. Single `orElseThrow` sau chuỗi filter. **Code:** `AuthService.java` (`findByEmailIgnoreCase().filter(enabled).filter(matches).orElseThrow(401)`). |
| BR-AUTH-LOGIN-002 | Login read-only transaction | `AuthService.login` annotated `@Transactional(readOnly = true)` — không mutate state. **Code:** `AuthService.java`. |
| BR-AUTH-LOGIN-003 | Email trim trước lookup | `request.email().trim()` trước `findByEmailIgnoreCase` — chống lỗi whitespace. **Code:** `AuthService.java`. |
| BR-AUTH-LOGIN-004 | Rate-limit pre-auth (brute-force / credential-stuffing) | Route public `kc-tenant-auth` mint token nên PHẢI rate-limit. Gateway `RequestRateLimiter` `replenishRate=3 / burstCapacity=5`, IP-keyed (`#{@ipKeyResolver}` — login pre-auth nên chưa có user key). Mirror GAP-514 auth-register rate. **Code:** `application.yml` route `kc-tenant-auth` (GAP-1012 shipped). |

---

## 4. JWT Token Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-AUTH-JWT-001 | Thuật toán HS512 | Token ký bằng HS512 — cùng thuật toán + cùng key gateway `JwtAuthenticationGatewayFilter` validate (GAP-705). **Code:** `AuthTokenService.java` (`Jwts.SIG.HS512`). |
| BR-AUTH-JWT-002 | Shared `JWT_SECRET` ≥ 64 bytes, fail-fast | Key đọc từ `jwt.secret` / env `JWT_SECRET` (cùng giá trị gateway). `@PostConstruct init()` ném `IllegalStateException` nếu secret < 64 bytes (yêu cầu key 512-bit cho HS512). **Code:** `AuthTokenService.java` `init()`. **Lưu ý:** gateway cũng enforce ≥64 bytes (`JwtAuthenticationGatewayFilter` + `TenantHeaderGuardFilter`) đồng bộ với Core — GAP-1012 DONE PR #2189. |
| BR-AUTH-JWT-003 | Claim set | `sub` = `user_uuid` (→ X-User-Id), `role` = `entity_type` (→ X-User-Roles), `email` (→ X-User-Email), `tenantId` = `instance_id` (→ resolved X-Tenant-Id qua gateway TenantResolver GAP-711), `referenceId` = `entity_id` (→ X-User-Reference-Id, GAP-798), `type` = `access`, `iat`, `exp`. **Code:** `AuthTokenService.mintAccessToken`. |
| BR-AUTH-JWT-004 | Access TTL 12h | TTL cấu hình `kite.auth.access-token-ttl` (default `PT12H` = 12 giờ). `LoginResponse.expiresInSeconds` = `accessTtl.toSeconds()` (43200). **Code:** `AuthTokenService.java` (`@Value("${kite.auth.access-token-ttl:PT12H}")`). |
| BR-AUTH-JWT-005 | Không có refresh token (Phase 1) | Chỉ access token. Không refresh/blacklist/`jti`/`iss`/`aud` ở Phase 1 — theo dõi follow-up P3 cho revocation roadmap. **Code:** `AuthTokenService.java`. |

---

## 5. Provisioning Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-AUTH-PROV-001 | Provision atomic trong caller txn | `AuthCredentialProvisioningService` chạy với propagation mặc định (REQUIRED) — credential provision atomic cùng domain row (parent không tồn tại thiếu credential và ngược lại). KHÔNG phải audit side-effect nên KHÔNG cần `REQUIRES_NEW`. **Code:** `AuthCredentialProvisioningService.java` (javadoc) + `ParentInvitationServiceImpl.redeem`. |
| BR-AUTH-PROV-002 | `provision()` idempotent-on-email | Dùng cho self-redeem (parent). Nếu credential đã tồn tại theo email (parent redeem invite con thứ 2) → GIỮ credential cũ, KHÔNG đổi password (mật khẩu gốc thắng). **Code:** `AuthCredentialProvisioningService.provision` / `provisionParent`. |
| BR-AUTH-PROV-003 | `setPassword()` upsert (rotate) | Dùng cho admin set/reset (teacher/student — Hướng B). UPSERT: credential tồn tại → rotate `password_hash` + `updated_at`; chưa tồn tại → tạo mới. **Code:** `AuthCredentialProvisioningService.setPassword`. |
| BR-AUTH-PROV-004 | Không seed credential trong migration | V89 chỉ tạo schema, KHÔNG seed — credential provision tại runtime. Test fixture thuộc `kiteclass/scripts/seed-data.sh`, không bao giờ migration production (cấm ship row known-password). **Code:** `V89:32-36`. |
| BR-AUTH-PROV-005 | Password policy (teacher/admin set) | `SetPasswordRequest`: 8–100 ký tự, regex yêu cầu chữ + số + ký tự đặc biệt (`^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$`). **Lưu ý bất đối xứng:** parent redeem (BR-PARENT-PWD-002) yêu cầu HOA+thường tách riêng — theo dõi follow-up P2 thống nhất policy chung. **Code:** `SetPasswordRequest.java`. |

---

## 6. Anti-Spoof Header Rules (Gateway)

| ID | Rule | Detail |
|----|------|--------|
| BR-AUTH-HDR-001 | Strip client-supplied `X-User-Reference-Id` | Gateway `default-filters: RemoveRequestHeader=X-User-Reference-Id` xoá mọi giá trị client gửi lên TRƯỚC khi route. Chống spoof reference-id (giống pattern `X-User-Id`). **Code:** `application.yml` `default-filters`. |
| BR-AUTH-HDR-002 | Re-inject từ verified JWT claim | `JwtAuthenticationGatewayFilter` re-inject `X-User-Reference-Id` từ claim `referenceId` của token đã verify (chỉ khi `!isChallenge` và claim non-null). Token KC-native (parent/teacher/student) mang `referenceId`; token OWNER/STAFF không có → header vắng. **Code:** `JwtAuthenticationGatewayFilter.java` (`mutated.header(HEADER_USER_REFERENCE_ID, ...)`). |
| BR-AUTH-HDR-003 | Core tin tưởng header gateway-only | Kiteclass-core đọc `X-User-Reference-Id` như identity đã verify. Client KHÔNG thể set trực tiếp (đã strip ở BR-AUTH-HDR-001). Walk-verified: forged ref-id bị strip (KC-8 G3 chain). |

---

## 7. Config Keys

| Key | Default | Env | Mô tả |
|-----|---------|-----|-------|
| `jwt.secret` | (none) | `JWT_SECRET` | HS512 signing key, ≥64 bytes, shared với gateway. Fail-fast nếu thiếu/ngắn (BR-AUTH-JWT-002). |
| `kite.auth.access-token-ttl` | `PT12H` | — | Access token TTL (BR-AUTH-JWT-004). |
| (gateway) `kc-tenant-auth` rate-limit | `replenishRate=3, burstCapacity=5` | — | Brute-force throttle IP-keyed (BR-AUTH-LOGIN-004, GAP-1012). |

---

## 8. Related

- `parent-portal/rules.md` — BR-PARENT-007 / AUTH-001 / PORTAL-002 (Option B sync), BR-PARENT-004 (PARENT_PORTAL_ENABLED).
- `student-portal/rules.md` — BR-STUDENT-PORTAL-001/002 (reference-id source Option B).
- `teacher/api-contract.md` — `POST /api/v1/teachers/{id}/credentials`.
- GAP-725 (parent/student/teacher auth pull-forward), GAP-798/798b (reference-id authz), GAP-705 (gateway HS512 shared key), GAP-711 (TenantResolver), GAP-1012 (rate-limit + HS512 keylen), GAP-1009 (3-layer docs).
- Audit: `documents/04-quality/audits/api-contract/2026-06-06-wave-auth-1-api-contract.md`, `documents/04-quality/audits/business-logic/2026-06-06-wave-auth-1-business-logic.md`.

---

## 9. Five-attribute review per `business-logic-review.md` §2

Auth/credential rule values (BCrypt cost, HS512 algorithm, token TTL, rate-limit thresholds) are **engineering security decisions**, not market-facing business values. The PII-handling dimension (login credential + email) carries a compliance overlay.

- **Source:** Engineering decision — industry security standards: BCrypt (OWASP Password Storage Cheat Sheet, cost 10), HS512 / 512-bit key (RFC 7518), uniform-401 no-enumeration (OWASP Auth Cheat Sheet), rate-limit mirror of GAP-514 auth-register. Wave auth-1 (GAP-725/798/1012).
- **Rationale:** Values chosen for security posture, not revenue — BCrypt cost 10 = standard work-factor balance; access-TTL 12h = single school-day session without refresh-token complexity (Phase 1 no refresh BR-AUTH-JWT-005); rate-limit 3/5 IP-keyed = brute-force / credential-stuffing throttle on a public mint endpoint.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-06-21). No Product-Owner/business sign-off required — pure auth-mechanism thresholds. Security/legal review of PII handling queued — GAP-156 AC-D.
- **Compliance check:** **Considered (self-assessed, counsel pending GAP-156 AC-D)** — per `documents/00-brd/compliance-checklist.md` L7 (+ security overlay): **Luật An ninh mạng 2018** (credential security — BCrypt hash BR-AUTH-005, no plaintext, no MD5/SHA1); **Nghị định 13/2023/NĐ-CP (PDPL)** (login PII = email/credential protection at rest + in transit); **Luật Giao dịch điện tử 2023** (e-identity / token-based access — click-login = valid electronic authentication, no e-signature in scope). No counsel verification yet.
- **Review cadence:** **Annual** (stable security mechanism) + event-driven on crypto-standard deprecation (BCrypt→Argon2, HS512→RS256) or PDPL implementing-decree. **Next review:** 2026-09-21 (next audit checkpoint), then Annual.
