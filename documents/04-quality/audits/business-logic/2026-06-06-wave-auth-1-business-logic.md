---
audience: mixed
---

# Business-Logic Audit — Wave auth-1 (KC-native login, Option B)

**Ngày:** 2026-06-06
**Scope:** PR #2186 (commit `2b01ac93`) — KC-native login (PARENT/TEACHER/STUDENT) + gateway `X-User-Reference-Id` inject + teacher credential provisioning + parent provisioning qua redeem.
**Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md` + rubric `.claude/rules/audit-skill-rubric-business-logic-audit.md` (5 categories, per-check pass/fail).
**Verdict:** ⚠️ **PARTIAL FAIL** — không có P0 production-breaking, nhưng nhiều P1 drift (business docs chưa cập nhật Option B; 0 test tự động cho auth module; multi-tenant email collision).
**Score:** **64/100 (C)**

> Per rubric §4 bug-finding > scoring primacy: bug list đứng TRƯỚC score. Mỗi finding kèm `file:line` evidence + proposed fix.

---

## 1. Bug list (precedes score)

### P1 — phải fix trước khi flip GAP-725/798b → DONE

| # | Finding | Evidence | Proposed fix |
|---|---|---|---|
| P1-1 | **Toàn bộ domain KC-native login (`tenant-auth`) KHÔNG có 3-layer business doc.** `documents/01-business/kiteclass/` có 46 domain nhưng KHÔNG có `auth`/`tenant-auth`. Entity_type CHECK, BCrypt, HS512 claims, uniform-401, JWT TTL 12h đều chỉ tồn tại trong code + javadoc — không có rules.md/use-cases.md/api-contract.md. Vi phạm CLAUDE.md Living Docs + `output-review-mandate.md` §3. | `find documents/01-business/kiteclass -type d` → no `auth`; commit diff stat: 0 file dưới `documents/01-business/`. | Tạo `documents/01-business/kiteclass/tenant-auth/{rules,use-cases,api-contract}.md` với BR-AUTH-xxx cho: BCrypt verify, HS512 ≥64-byte key, claim set (sub/role/email/tenantId/referenceId/type), uniform 401 no-enumeration, entity_type ∈ {PARENT,TEACHER,STUDENT}, email globally-unique lookup, TTL 12h. |
| P1-2 | **parent-portal docs vẫn mô tả Option A (đã bị Option B thay thế).** BR-PARENT-007 / BR-PARENT-AUTH-001 / BR-PARENT-PORTAL-002 + `api-contract.md:16` + `use-cases.md:71,105,217` nói "Credential + JWT ở Gateway `users` table", "Gateway populate `X-User-Reference-Id` từ `users.reference_id`". Code Wave auth-1 chuyển sang Core `auth_credentials` + Core mint JWT + gateway inject từ JWT claim `referenceId`. | `parent-portal/rules.md:33,90,181`; code `AuthTokenService.java:70` (referenceId từ entity), `JwtAuthenticationGatewayFilter.java:200-207` (inject từ claim, không phải users table). | Cập nhật BR-PARENT-007/AUTH-001/PORTAL-002 + api-contract + use-cases sang Option B (Core auth_credentials, JWT claim-based ref-id). Đánh dấu `users.reference_id` design là superseded. |
| P1-3 | **`PARENT_PORTAL_ENABLED` default flip false→true nhưng BR-PARENT-004 vẫn ghi false.** Commit: "PARENT_PORTAL_ENABLED default true (was Wave 2 default-off gate)". rules.md vẫn nói default false + "Wave 5 sẽ flip true cho instances đã ký PDPL" → config-key value drift (rubric Cat 2 §2.2) + PDPL gate bị bỏ qua âm thầm. | `parent-portal/rules.md:30,101` (`enabled` default `false`); commit message Bucket B. | Cập nhật BR-PARENT-004 + bảng config về default true, HOẶC khôi phục default false nếu PDPL gate vẫn yêu cầu. Ghi rõ quyết định PDPL. |
| P1-4 | **0 test tự động cho auth module.** `kiteclass-core/src/test/.../module/auth/` có 0 file. Không có test cho: login happy-path, wrong-password 401, unknown-email 401, disabled 401, JWT claim shape, idempotent provision, upsert setPassword, entity_type CHECK. Chỉ walk-verified thủ công (rubric Cat 3 §3.1 P0 error-path test). | `find .../test/.../module/auth -type f | wc -l` → 0. | Thêm `AuthServiceTest` (4 branch 401 + happy), `AuthTokenServiceTest` (claim assertions + ≥64-byte guard), `AuthCredentialProvisioningServiceTest` (idempotent vs upsert), + `AuthCredentialPostgresIT` (entity_type CHECK + unique email round-trip per `postgres-specific-type-testcontainers.md`). |
| P1-5 | **Multi-tenant email collision — `auth_credentials.email` GLOBALLY unique mâu thuẫn BR-PARENT-001 (email hợp lệ ở nhiều tenant).** Cùng 1 email là parent ở tenant A và B → `provision()` idempotent-on-email trả về credential CŨ (instance_id của tenant A) → người dùng chỉ login được 1 tenant + token mang sai tenantId. | `V89:20` `uk_auth_credentials_email UNIQUE(email)`; `AuthCredentialProvisioningService.java:50-55` (findByEmailIgnoreCase, keep existing); mâu thuẫn `parent-portal/rules.md:27` BR-PARENT-001. | Quyết định + document: hoặc (a) chấp nhận giới hạn "email = 1 tenant" cho Phase 1 (cập nhật BR-PARENT-001), hoặc (b) đổi lookup pre-auth sang email+tenant-slug, unique `(instance_id, email)`. |

### P2 — nên fix trong wave kế

| # | Finding | Evidence | Proposed fix |
|---|---|---|---|
| P2-1 | **Bất đối xứng password policy giữa 2 credential path.** Parent redeem (BR-PARENT-PWD-002) yêu cầu HOA+thường+số+đặc biệt; `SetPasswordRequest` (teacher) chỉ yêu cầu chữ+số+đặc biệt (không tách HOA/thường). Cùng table `auth_credentials`, 2 policy khác nhau. | `SetPasswordRequest.java:15-17` regex `^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$` vs `parent-portal/rules.md:81`. | Thống nhất 1 password policy dùng chung cho mọi auth_credentials provisioning; document BR-AUTH-PWD-xxx. |
| P2-2 | **`setPassword` found-by-email bỏ qua entityType/entityId mismatch.** Nếu email đã thuộc credential PARENT, admin set password teacher cùng email sẽ rotate credential PARENT (giữ entityType=PARENT, entityId=parentId) thay vì tạo TEACHER. | `AuthCredentialProvisioningService.java:88-100` (chỉ set passwordHash, không reconcile entityType/entityId). | Khi found-by-email nhưng entityType/entityId khác → reject (409 conflict) hoặc raise rõ ràng. |
| P2-3 | **Gateway HS512 key-length check chỉ ≥32 bytes (sai cho HS512 — cần 64).** Comment nói "≥32 bytes (256 bits) for HS512" — nội bộ mâu thuẫn. Core fail-fast ≥64 (đúng) nên gate thực tế ở Core, nhưng gateway check yếu hơn thuật toán. | `JwtAuthenticationGatewayFilter.java:90-94` (≥32) vs `AuthTokenService.java:47` (≥64). | Sửa gateway check + comment sang ≥64 bytes cho HS512 (đồng bộ với Core). |
| P2-4 | **Không có rate-limit/lockout trên `POST /api/v1/tenant-auth/login`.** Route public, BCrypt chậm nhưng không có brute-force lockout/throttle documented. | `AuthController.java:33`; `application.yml:726` route `kc-tenant-auth` public no filter. | Document + wire gateway rate-limit cho route tenant-auth (per `pre-launch-auth-hardening-checklist.md`). |
| P2-5 | **Không có cờ disable credential khi parent/teacher bị deactivate.** `enabled` default true, không path nào set false; soft-delete domain entity không vô hiệu hoá login credential. | `AuthCredential.java:67-69`; không có setter-to-false trong provisioning. | Wire disable credential khi entity soft-deleted/INACTIVE. |

### P3 — minor / hygiene

| # | Finding | Evidence | Proposed fix |
|---|---|---|---|
| P3-1 | **Login log email plaintext.** `log.info("Login failed for email={}...", request.email())` — PII trong log, vi phạm `logs-format-standard.md` §2.4/§3 (email phải mask). | `AuthService.java:48`. | Mask email hoặc log dưới structured field qua scrubber. |
| P3-2 | **JWT thiếu `jti`/`iss`/`aud`.** Không hỗ trợ revocation/blacklist; không validate issuer/audience. Acceptable Phase 1 nhưng nên ghi nhận. | `AuthTokenService.java:65-75`. | Thêm `jti` cho revocation roadmap; document quyết định bỏ iss/aud. |
| P3-3 | **Timing side-channel user-enumeration.** Khi email không tồn tại, `.filter(passwordEncoder.matches)` không chạy BCrypt → response nhanh hơn → oracle account-exists, mặc dù message uniform. | `AuthService.java:43-50`. | Dummy BCrypt compare khi email not-found để cân bằng thời gian. |

---

## 2. Điểm CORRECT (business logic đúng — ghi nhận)

- **BCrypt:** `BCryptPasswordEncoder` default cost (10); `password_hash` VARCHAR(72) đủ cho 60-char hash. ✓
- **HS512 ≥64-byte key fail-fast:** `AuthTokenService.init()` ném `IllegalStateException` nếu secret <64 bytes. ✓ Đúng yêu cầu HS512 (512-bit key).
- **JWT claim set:** sub=userUuid, role=entityType, email, tenantId=instanceId, referenceId=entityId, type=access, iat, exp — khớp gateway forward (`X-User-Id`/`X-User-Roles`/`X-User-Email`/`X-User-Reference-Id`/`X-Tenant-Id`). ✓
- **Anti-spoof:** `default-filters RemoveRequestHeader=X-User-Reference-Id` strip client value (`application.yml:767`) + filter re-inject từ verified claim (`JwtAuthenticationGatewayFilter.java:203-206`), chỉ khi `!isChallenge`. Walk-verified forged ref-id stripped. ✓ Cùng pattern X-User-Id.
- **entity_type CHECK:** `V89:22` CHECK IN ('PARENT','TEACHER','STUDENT') khớp entity length 16 + `ROLE_*` constants. ✓
- **Tenant binding:** credential row IS tenant binding (instance_id) cho pre-auth email lookup — thiết kế đúng cho login trước khi có tenant context. ✓ RLS-exempt có rationale (`V89:4-7`).
- **Uniform 401 no-enumeration:** single `orElseThrow(INVALID_CREDENTIALS 401)` cho unknown-email/disabled/wrong-password. ✓ (timing oracle P3-3 là phần dư).
- **Transaction propagation:** provisioning chạy trong caller txn (REQUIRED) — atomic parent/teacher + credential, KHÔNG phải audit side-effect nên không cần REQUIRES_NEW (đúng `audit-service-isolation.md` scope). ✓ Login `@Transactional(readOnly=true)`. ✓
- **Public route ordering:** `kc-tenant-auth` precede `/api/v1/**` catch-all, no TenantResolver (login chưa có tenant context). ✓

---

## 3. Score breakdown (per-category /20)

| Cat | Tên | Score | Lý do chính |
|---|---|:---:|---|
| 1 | Rule Coverage | **10/20** | P1-1 (domain tenant-auth không có rules.md) + P1-2 (Option A drift) + teacher credential endpoint không có BR + orphan business logic. |
| 2 | Config Accuracy | **13/20** | P1-3 (PARENT_PORTAL_ENABLED default drift) + `kite.auth.access-token-ttl` PT12H không documented. JWT_SECRET shared được code-documented. |
| 3 | Edge Case Tests | **8/20** | P1-4 — 0 test auth module (4 error-path 401 + claim + provision/upsert đều thiếu). Chỉ walk-verified. |
| 4 | Cross-Domain Consistency | **11/20** | P1-5 (email global vs per-tenant) + P2-1 (password asymmetry) + P2-2 (setPassword cross-role) + Option B mâu thuẫn parent-portal Option A docs. |
| 5 | Stakeholder Alignment | **12/20** | Không có rules.md → thiếu Reviewer/Source/Compliance attributes; credential + PII (parent/student) chưa review PDPL. Cần human review. |
| | **TOTAL** | **64/100 (C)** | Code-level correct; mất điểm ở doc drift + thiếu test + multi-tenant edge. |

---

## 4. Ghi chú

- Wave tự khai báo PARTIAL (GAP-725/798b: teacher/student provisioning, KC-9, production parity JWT_SECRET). Nhiều P1 ở trên (docs, tests) khớp phần PARTIAL chưa hoàn thành → cần đóng trước khi flip DONE.
- Business docs KHÔNG được cập nhật cùng PR (diff stat 0 file `documents/01-business/`) → vi phạm Living Docs (cùng-PR doc+code). Đây là nguồn chính của P1-1/P1-2/P1-3.
- Không có P0 production-breaking → verdict PARTIAL FAIL (không phải hard FAIL), nhưng audit-level KHÔNG PASS cho tới khi P1 đóng.
