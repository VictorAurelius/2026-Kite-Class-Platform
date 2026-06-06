---
title: Ops-Readiness Audit — Wave auth-1 (KC-native login)
status: complete
created: 2026-06-06
phase: Phase 1 BETA
wave: auth-1
commit: 2b01ac93
scope: KC-native login surface (AuthTokenService / AuthService / V89 / gateway kc-tenant-auth route / production parity)
audit_level_verdict: PARTIAL (no fresh P0; 3 P1 block production-readiness of the new surface)
score: 71/100 (C)
---

# Ops-Readiness Audit — Wave auth-1 (KC-native login, Option B)

Phạm vi: bề mặt đăng nhập KC-native pull-forward (PR #2186, commit `2b01ac93`) — `AuthTokenService`
(HS512 JWT mint), `AuthService` (BCrypt verify + uniform 401), migration `V89__create_auth_credentials.sql`,
gateway route `kc-tenant-auth`, và production-parity chain (`fetch-secrets.sh` + `secrets.tf` +
`docker-compose.production.yml`).

Per `audit-skill-rubric-ops-readiness-audit.md` §4 bug-finding-primacy: **bug list trước, score sau**.

---

## 1. Bug list (mọi FAIL, severity + evidence)

### 🟠 P1 — chặn production-readiness của bề mặt auth-1

**P1-1 — Gateway route `kc-tenant-auth` thiếu `RequestRateLimiter`.**
`kitehub/kitehub-gateway/src/main/resources/application.yml:726-734` — route mới chỉ có `CircuitBreaker`,
KHÔNG có `RequestRateLimiter`. Mọi route auth khác (vd `application.yml:41-45`, GAP-514 OWASP A07 hardening)
đều có rate limiter (`replenishRate 3 / burstCapacity 5`). `/api/v1/tenant-auth/login` là endpoint PUBLIC
(no auth) mint JWT — không throttle = brute-force / credential-stuffing không bị chặn ở gateway.
Kết hợp với P1-2 (no lockout ở service) → bề mặt login mới không có lớp chống dò mật khẩu nào ngoài
độ chậm của BCrypt.
→ Fix: thêm `RequestRateLimiter` (pattern GAP-514, replenishRate 3 burst 5) vào filters của route `kc-tenant-auth`.

**P1-2 — `kiteclass-core` KHÔNG có trong `docker-compose.production.yml` → auth-1 surface không có đường deploy production.**
`docker-compose.production.yml:7-8` — "KC stack (kiteclass-core + gateway + frontend) deferred to Phase 7
polish wave per GAP-444". Toàn bộ code auth-1 (`AuthController` / `AuthTokenService` / `AuthService`) nằm
trong `kiteclass-core` — service này KHÔNG được khai báo trong production compose. Nghĩa là feature mới chỉ
walk-verified ở local stack (`kitehub/docker-compose.kitehub.yml`), chưa có artifact deploy production.
`JWT_SECRET` đã được `fetch-secrets.sh` ghi vào `/etc/kite/.env` (line 63, 170) NHƯNG không có consumer
nào trên prod. Per `local-fix-production-parity-check.md` §2 row 1: thay đổi env local compose phải có
production-equivalent surface cùng PR HOẶC follow-up gap có deadline. Commit message đã ghi nhận
("Production parity: kiteclass-core prod JWT_SECRET via fetch-secrets.sh — follow-up") → là PARTIAL có ý thức.
→ Fix: thêm service `kiteclass-core` vào `docker-compose.production.yml` (env_file `/etc/kite/.env` passthrough
đã cover JWT_SECRET) HOẶC file follow-up gap với AC + deadline rõ ràng + dependency (GAP-444 Phase 7).

**P1-3 — `PARENT_PORTAL_ENABLED` mặc định `false` ở production.**
`kiteclass-core/src/main/resources/application.yml:318` → `enabled: ${PARENT_PORTAL_ENABLED:false}`.
Local compose override `:-true` (`kitehub/docker-compose.kitehub.yml:673`) NHƯNG biến này KHÔNG có trong
`fetch-secrets.sh` cũng KHÔNG trong `docker-compose.production.yml`. Trên production sẽ default `false` →
parent portal bị gate off → toàn bộ luồng parent login auth-1 không hoạt động dù có deploy. Đây là item
"handoff-known pending" trong task #2 — xác nhận VẪN PENDING.
→ Fix: thêm `PARENT_PORTAL_ENABLED=true` vào production env source (compose `environment:` block — public
feature flag, dùng mechanism §4.1 của `production-env-config-registry.md`, không phải secret).

### 🟡 P2 — security/observability posture (không chặn deploy nhưng cần đóng)

**P2-1 — `AuthService` log email plaintext khi login fail.**
`AuthService.java:48` → `log.info("Login failed for email={} (uniform 401)", request.email())`. Email là PII
per `logs-format-standard.md` §2.4 + §3.1 (cần mask `a***@domain.com`). PII scrubber (GAP-116) chưa
implement (deferred Wave 7) → email ghi raw vào log. auth-1 thêm 1 site PII-log mới đóng góp vào gap nền tảng.
Mặt tích cực: password + JWT KHÔNG bao giờ bị log (✅), login OK chỉ log role/referenceId/tenantId (không PII).
→ Fix: bỏ email khỏi message HOẶC mask thủ công cho tới khi GAP-116 scrubber active.

**P2-2 — Không có account lockout / failed-attempt counter trong `AuthService`.**
`AuthService.login()` chỉ verify BCrypt + uniform 401, không đếm số lần fail / không khoá tài khoản. Chống
brute-force hiện chỉ dựa độ chậm BCrypt. Phụ thuộc gateway rate limit (xem P1-1 — hiện cũng thiếu).
→ Fix: rely on gateway RequestRateLimiter (P1-1) làm lớp 1; cân nhắc lockout per-credential (Phase 1.5).

**P2-3 — `V89 auth_credentials` global `UNIQUE(email)` + RLS-disabled = cross-tenant blast radius.**
`V89__create_auth_credentials.sql:20` `uk_auth_credentials_email UNIQUE (email)` là global → 1 email KHÔNG thể
thuộc 2 tenant (1 phụ huynh có con ở 2 trường dùng cùng email sẽ kẹt). Bảng RLS-disabled (có rationale đúng:
lookup pre-auth, chưa set tenant GUC) NHƯNG là bảng global high-value: nếu bị đọc trộm (SQLi/broad read) thì
lộ credential hash của MỌI tenant, thay vì per-tenant RLS giới hạn blast radius. Hash là BCrypt nên rủi ro
giảm; posture chấp nhận được cho use case lookup pre-auth nhưng cần ghi nhận.
→ Note: cân nhắc `UNIQUE(email, instance_id)` nếu cần same-email cross-tenant; document blast-radius posture.

### 🟢 P3 — minor / doc drift

**P3-1 — `secrets.tf` jwt-secret description nói "HS256" nhưng thực tế HS512.**
`infrastructure/terraform-aws/secrets.tf:47` description `"JWT signing secret (HS256)"`. AuthTokenService +
gateway dùng HS512. `random_password.jwt length=64 special=false` = đúng 64 bytes = 512 bits = mức tối thiểu
HS512, KHÔNG có margin (check `< 64` → 64 vừa pass). Length OK nhưng zero buffer.
→ Fix: sửa description → HS512; cân nhắc length 88 cho margin.

---

## 2. Production-parity verification (task #2 — handoff-known pending item)

Item handoff: "kiteclass-core prod JWT_SECRET + PARENT_PORTAL_ENABLED via fetch-secrets.sh + IaC".

| Thành phần | Trạng thái | Bằng chứng |
|---|---|---|
| `JWT_SECRET` trong AWS Secrets Manager IaC | ✅ DONE | `secrets.tf:45-55` `aws_secretsmanager_secret.jwt` (length=64) |
| `fetch-secrets.sh` fetch + ghi `/etc/kite/.env` | ✅ DONE | `fetch-secrets.sh:63` fetch + `:170` write `JWT_SECRET=${JWT_SECRET}` |
| `kiteclass-core` consume JWT_SECRET ở production | ❌ PENDING | service không có trong `docker-compose.production.yml` (P1-2) |
| `PARENT_PORTAL_ENABLED` production override | ❌ PENDING | không trong fetch-secrets.sh / production compose; default false (P1-3) |

**Kết luận task #2:** chain bí mật JWT (Secrets Manager → fetch-secrets → /etc/kite/.env) ĐÃ XONG ở mức
provisioning, nhưng service tiêu thụ (`kiteclass-core` prod deploy) + feature flag `PARENT_PORTAL_ENABLED`
VẪN PENDING. Item handoff là **PARTIAL**, không phải DONE.

---

## 3. Per-category score (per `audit-skill-rubric-ops-readiness-audit.md` §2)

| # | Category | Score | Lý do chính |
|---|---|:---:|---|
| 1 | Monitoring & Observability | 15/20 | App log có (login OK/fail); awslogs driver cover kh-services nhưng kc-core không deploy prod → không có observability của bề mặt auth mới; không có custom auth metric (success/fail rate). |
| 2 | Logging Standards | 14/20 | ✅ password/JWT không bao giờ log, uniform 401; ❌ email PII log site mới (P2-1); JSON encoder + scrubber platform-deferred (GAP-114/116). |
| 3 | Backup & Recovery | 16/20 | `auth_credentials` kế thừa RDS automated backup của kiteclass schema; không có gap auth-1-specific; chưa verify được vì kc-core chưa deploy prod. |
| 4 | Alerting | 14/20 | Không có alert login-failure-spike / brute-force; kết hợp P1-1 (no rate limit) + P2-2 (no lockout) = bề mặt auth mới không có cảnh báo dò mật khẩu. |
| 5 | Deployment Pipeline | 12/20 | 🔴 Dominant: kc-core không có production deploy artifact (P1-2); PARENT_PORTAL_ENABLED chưa override prod (P1-3); local compose OK nhưng production parity thiếu. |
| | **Total** | **71/100 (C)** | |

**Audit-level verdict: PARTIAL.** Không có fresh P0 trong code auth-1 (password/JWT không lộ, uniform 401,
gateway strip anti-spoof X-User-Reference-Id, RLS-disable có rationale). 3 P1 chặn việc bề mặt auth-1 đạt
production-readiness — tất cả là documented follow-up (feature pull-forward + Phase-gated), không phải
"broken production". Khắc phục 3 P1 trước khi feature reach prod.

---

## 4. Điểm tích cực (verified)

- `AuthTokenService` fail-fast tại boot nếu `JWT_SECRET < 64 bytes` (HS512 guard) — tốt cho catch sớm.
- Password + JWT token KHÔNG bao giờ bị log.
- Uniform 401 `INVALID_CREDENTIALS` — chống user-enumeration (unknown email / disabled / wrong pwd cùng message).
- Gateway `default-filters RemoveRequestHeader=X-User-Reference-Id` — anti-spoof, gateway là authority duy nhất set ref-id.
- V89 KHÔNG seed known-password row (production hygiene); credential provision runtime.
- JWT_SECRET provisioning chain (Secrets Manager IaC → fetch-secrets → /etc/kite/.env) hoàn chỉnh + length đủ.
- V89 RLS-disable có rationale rõ ràng inline (pre-auth lookup, row IS tenant-binding source).

---

## 5. Recommendations (thứ tự ưu tiên)

1. **P1-1**: thêm `RequestRateLimiter` vào gateway route `kc-tenant-auth` (pattern GAP-514).
2. **P1-2 + P1-3**: hoặc ship `kiteclass-core` service + `PARENT_PORTAL_ENABLED=true` vào
   `docker-compose.production.yml`, hoặc file follow-up gap với AC + deadline + dependency GAP-444 Phase 7
   (per `local-fix-production-parity-check.md` §3.2).
3. **P2-1**: bỏ/mask email khỏi log fail của `AuthService` cho tới khi GAP-116 scrubber active.
4. **P3-1**: sửa description secrets.tf HS256 → HS512.
5. **P2-3**: document blast-radius posture của global auth_credentials; cân nhắc composite unique nếu cần.

---

## 6. References

- Commit: `2b01ac93` (PR #2186)
- Files: `kiteclass-core/.../auth/service/{AuthTokenService,AuthService,AuthCredentialProvisioningService}.java`,
  `V89__create_auth_credentials.sql`, `kitehub-gateway/.../application.yml:726`, `scripts/fetch-secrets.sh`,
  `infrastructure/terraform-aws/secrets.tf`, `docker-compose.production.yml`, `kitehub/docker-compose.kitehub.yml`
- Rules applied: `audit-skill-rubric-ops-readiness-audit.md`, `local-fix-production-parity-check.md`,
  `production-env-config-registry.md`, `logs-format-standard.md`, `pre-launch-auth-hardening-checklist.md`
- Related gaps: GAP-444 (KC stack prod deploy Phase 7), GAP-725/798b (auth-1 scope), GAP-514 (gateway rate limit), GAP-116 (PII scrubber)
