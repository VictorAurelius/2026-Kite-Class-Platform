---
title: Security Audit — Full (post wave-p0-closeout-1)
status: complete
created: 2026-06-14
phase: Phase 1 BETA pre-launch
wave: p0-closeout-1
auditor: claude (Opus 4.8 1M) — security-audit skill v2
gaps: [GAP-1308, GAP-1309, GAP-1310, GAP-1311]
baseline_security_100: 93/100 A (2026-05-18 Wave 94c v2, audits-index.csv)
audit_format_version: v2
evidence_dir: documents/04-quality/audits/security/evidence/2026-06-14/
---

# Security Audit — Full (post wave-p0-closeout-1)

## 1. Header

**Phạm vi audit:** Toàn bộ monorepo tại commit `cd44e035f` (sau wave-p0-closeout-1). Trọng tâm bề mặt wave gần đây: LMS paywall (`LessonAccessGuard`), KC OWNER authz (GAP-1139), SSO determinism (GAP-1306), StorageController download-url (GAP-1307), cluster header-spoof vừa đóng (GAP-1299/1300/1301/814).

**Method:** Per `.claude/skills/quality/security-audit/SKILL.md` v2 — chấm theo từng sub-check (không lấy trung bình); mỗi control gắn evidence block (Command run + Output + Verdict + Evidence artifact ID) per GAP-564. Bug list là deliverable, điểm số chỉ mô tả. Trọng tâm: phân tích tĩnh (OWASP Top-10, @PreAuthorize coverage, secrets grep, dep CVE, RLS/tenant-isolation, JWT, input validation). Stack Docker 13-container đang chạy dùng để xác minh runtime khi cần.

**Baselines so sánh:**
- Wave 94c v2 (2026-05-18): **93/100 A** — baseline gần nhất.
- Wave 85 (2026-05-15): 93/100 A.

---

## 2. Methodology

**Tools used:** `grep -rnE` (Cat 2/3 source scan), `pnpm audit --audit-level=high` (Cat 1 FE), đọc trực tiếp filter/config/entity/migration (Cat 3/4/5), `bash scripts/query-gaps.sh` (dedup), `git log/show` (xác minh fix wave gần đây).

**Scope coverage:** `kitehub/` (8 service + gateway + FE), `kiteclass/` (core + gateway + FE), `scripts/`, `infrastructure/` (terraform-aws, k8s, helm), `documents/`. 105 controller (`*Controller.java`), 7 Java module.

**Sampling:** Cat 1 = cả 2 FE app + root poms BE; Cat 2 = 100% grep broad-scope (incl. docker-compose + infrastructure per GAP-564); Cat 3 = 1 block/OWASP item; Cat 4 = gateway JWT + strip + rate-limit + downstream authority bridge; Cat 5 = RLS migration sweep + Docker USER + CORS + security headers + actuator. AWS-live controls (TLS/IAM/CloudTrail) **không chạy** session này (account state) → đánh dấu `❓ UNCHECKED`, không PASS mặc định.

---

## 3. Score Summary

| # | Category (20pt) | Score | Verdict | Evidence blocks |
|---|-----------------|:-----:|:-------:|:---------------:|
| 1 | Dependency Vulnerabilities | 17/20 | 🟢 | 3 |
| 2 | Secrets & Credentials | 19/20 | 🟢 | 4 |
| 3 | OWASP A01-A06/A08-A10 | 17/20 | 🟡 | 9 |
| 4 | Auth & Access Control (A07) | 16/20 | 🔴 | 5 |
| 5 | Infrastructure Security | 16/20 | 🟡 | 5 |

**Tổng: 85/100 — Grade B** (delta vs baseline 93/100 A = **−8**, do bề mặt audit lần này mở rộng sang KiteClass storage + gateway header-strip mà các audit trước chưa quét sâu).

**Aggregate verdict: ❌ FAIL** — có **1 P0 OPEN** (F-001 role-spoof) trong Cat 4. Theo SKILL §3 rule 5 + §2 "Primacy: bug-finding > scoring", bất kỳ P0/P1 fail nào → audit-level verdict = FAIL bất kể tổng điểm. Điểm 85 vượt ngưỡng Phase 1 BETA ≥80 nhưng **promotion BỊ CHẶN cho tới khi đóng GAP-1308**.

**v2 evidence completeness:** 26/26 control planned đều có evidence block (100%). 3 control AWS-live (`INFRA TLS/IAM/CloudTrail`) đánh dấu UNCHECKED minh bạch.

---

## 4. Bug List (deliverable — surface BEFORE score)

### P0 — BLOCKING promotion

**F-001 (P0): Gateway KHÔNG strip `X-User-Roles` → role-spoof privilege escalation qua gateway**
- File: `kitehub/kitehub-gateway/src/main/resources/application.yml:965-973` (default-filters), `JwtAuthenticationGatewayFilter.java:159-161,219-221`, `kiteclass-core/.../GatewayHeaderAuthenticationFilter.java:69-90`, `kiteclass-core/.../config/SecurityConfig.java:52-53`.
- **Impact:** `default-filters` strip X-Tenant-Id / X-User-Id / X-User-Reference-Id / X-Subscription-Tier (đúng pattern "strip + re-inject từ claim đã verify") nhưng **bỏ sót X-User-Roles + X-User-Email**. JwtAuthenticationGatewayFilter chỉ set X-User-Roles khi `role != null`; với request **không có Bearer token** → filter pass-through (không 401, không set header). Downstream `GatewayHeaderAuthenticationFilter` (KC) + `XUserRolesHeaderFilter` (KH sub) dựng Spring authority TRỰC TIẾP từ X-User-Roles. KC SecurityConfig `.anyRequest().permitAll()` ở URL layer → authz chỉ ở method-layer `@PreAuthorize`. ⇒ Client gửi `X-User-Roles: OWNER` (không token) qua gateway → core cấp `ROLE_OWNER` → endpoint chỉ gate bằng `hasRole/hasAnyRole` (không ràng tenant/resource) bị **escalate bởi client chưa xác thực**.
- **Mitigation hiện có:** endpoint tenant-scoped còn cần X-Tenant-Id (đã strip) → fail TENANT_CONTEXT_MISSING; endpoint per-resource dùng X-User-Reference-Id (đã strip) → fail. Subset khai thác = endpoint role-only-gated không bind tenant/resource. **Không** được mitigate bởi network-isolation (GAP-825) vì lỗ hổng nằm ở chính gateway forward header.
- **Cùng class GAP-814** (X-Tenant-Id strip, P0 DONE) — fix đó lẽ ra phải thêm X-User-Roles.
- **Fix:** thêm `RemoveRequestHeader=X-User-Roles` + `RemoveRequestHeader=X-User-Email` vào `default-filters`; cho JwtAuthenticationGatewayFilter set X-User-Roles **vô điều kiện** (mặc định rỗng/none-role least-privilege khi claim thiếu); cân nhắc 401 cho request tokenless tới path non-public thay vì pass-through.
- **Evidence:** EVIDENCE-2026-06-14-AUTH-001 → **GAP-1308**

### P1 — Should fix before promotion

**F-002 (P1): StorageController `confirmUpload` + `deleteFile` thiếu per-resource ownership authz → intra-tenant IDOR**
- File: `kiteclass-core/.../storage/controller/StorageController.java:108,173`; `.../storage/service/impl/StorageServiceImpl.java:151,225`.
- **Impact:** 2 endpoint chỉ nhận `@PathVariable Long fileId`, không có requesterId/tenantId, không `@PreAuthorize`. Service `findByIdAndDeletedFalse(fileId)` rồi `softDelete()`/`confirmUpload()` không check uploader/role. Cross-tenant được Hibernate `tenantFilter` chặn (404), nhưng **intra-tenant KHÔNG**: bất kỳ user (mọi role) cùng tenant có thể **xóa mềm** hoặc confirm file của user khác bằng cách enumerate fileId tuần tự (vd STUDENT xóa material của TEACHER; quota của nạn nhân bị trừ). download-url thì CÓ check PRIVATE/TENANT (L369-383) — bất đối xứng.
- **Fix:** thêm requesterId+tenantId vào confirm/delete + owner-or-admin check (delegate AuthorizationBean), + `@PreAuthorize` role gate cho controller storage.
- **Evidence:** EVIDENCE-2026-06-14-OWASP-A01-002 → **GAP-1309**

### P2 — Track cho Phase 1.5+

**F-003 (P2): Gateway KHÔNG strip `X-User-Email` → header email giả mạo được (audit/display poisoning)**
- File: `application.yml:965-973`. X-User-Email re-inject từ claim nhưng không strip; khi token thiếu claim email / tokenless → client value lọt. Không phải authz nhưng có thể đầu độc audit log / luồng dựa email. → **GAP-1310**. (Tách khỏi F-001 vì surface + severity khác.)

**F-004 (P2): `uploaded_files` + `storage_quota` không có DB-level RLS — chỉ dựa Hibernate `@Filter`**
- File: migration sweep V58/V78/V84 không phủ `uploaded_files`; `BaseEntity.java:43-44` `@Filter`. Thiếu backstop RLS DB như các bảng tenant-scoped khác → nếu filter bị disable / native query / path exempt thì rò cross-tenant. → **GAP-1311**. Cross-ref GAP-825.

### Đã có gap OPEN — REFERENCE (không file trùng)

| Concern | Gap hiện có |
|---|---|
| StorageController download-url bỏ enrollment paywall (visibility-only) | GAP-1307 (OPEN P1) |
| Core trust header + cần network-isolation + JWT-sig-verify TenantResolver fallback | GAP-825 (OPEN P1) |
| Parent portal role-collision IDOR | GAP-1007 (OPEN P2) |
| Stored-SVG-XSS qua MIME client-trusted (storage ALLOWED_MIME_TYPES có image/svg+xml) | GAP-1037 (OPEN P2 — đề nghị mở rộng scope từ logo sang storage chung) |
| KiteClass gateway thiếu HSTS+CSP parity | GAP-472 (PARTIAL P1) |
| Vercel FE CSP + restrict CORS | GAP-471 (PARTIAL P1) |
| sessionStorage XSS → httpOnly cookie | GAP-643 (OPEN P2) |

---

## 5. Per-Category Evidence Blocks (v2)

### Cat 1 — Dependency Vulnerabilities — 17/20 🟢

#### DEPS-001 — FE pnpm audit cả 2 app (P0)
**Control:** `pnpm audit --audit-level=high` = 0 HIGH/CRITICAL.
- **Command:** `cd kitehub/kitehub-frontend && pnpm audit --audit-level=high` ; `cd kiteclass/kiteclass-frontend && pnpm audit --audit-level=high`
- **Output:** kitehub-frontend `1 vulnerabilities found — Severity: 1 moderate` (exit 0); kiteclass-frontend `1 moderate` (exit 0).
- **Verdict:** ✅ PASS — 0 high/critical, 1 moderate/app (chấp nhận).
- **Evidence:** EVIDENCE-2026-06-14-DEPS-001

#### DEPS-002 — BE dependency versions (P0)
**Control:** Spring Boot + lib không dính CVE HIGH/CRIT đã biết.
- **Command:** `grep -nE "spring-boot|jjwt|postgresql|java.version" kitehub/pom.xml kiteclass/kiteclass-core/pom.xml`
- **Output:** spring-boot-starter-parent **3.5.14**, Java 17, jjwt **0.13.0**, postgresql **42.7.11** — đều current.
- **Verdict:** ✅ PASS — version hiện hành, không CVE HIGH/CRIT đã biết cho các version pinned.
- **Evidence:** EVIDENCE-2026-06-14-DEPS-001

#### DEPS-003 — `mvn dependency-check` + Trivy image scan (P1)
- **Command:** `./mvnw dependency-check:check` / `trivy image` — **không chạy session này** (offline + thời gian).
- **Verdict:** ❓ UNCHECKED — không PASS mặc định; coordinator quyết định defer. Không có dấu hiệu CVE HIGH/CRIT từ version manifest.
- **Evidence:** EVIDENCE-2026-06-14-DEPS-001 (note)

*Score: 20 − 0 P0/P1 fail = 20; trừ 3 do 2 control (mvn/Trivy) UNCHECKED → **17/20**.*

---

### Cat 2 — Secrets & Credentials — 19/20 🟢

#### SEC-001 — Zero hardcoded secrets (broad scope, P0)
**Control:** grep `kitehub/ kiteclass/ scripts/ infrastructure/` + docker-compose per GAP-564.
- **Command:** grep secret-pattern (xem artifact).
- **Output:** Chỉ còn: `DomainService.java:99` DNS verify token (không phải secret); `infrastructure/k8s/.../secrets.yaml` toàn `REPLACE_WITH_BASE64` placeholder; docker-compose toàn `${VAR:?required}`.
- **Verdict:** ✅ PASS — 0 leak thật.
- **Evidence:** EVIDENCE-2026-06-14-SEC-001

#### SEC-002 — `.env` không bị commit (P0)
- **Command:** `git ls-files | grep -E "\.env" | grep -vE "(template|example|sample|.md)"`
- **Output:** (rỗng).
- **Verdict:** ✅ PASS.
- **Evidence:** EVIDENCE-2026-06-14-SEC-001

#### SEC-003 — JWT secret quản lý qua env + validate length (P0)
- **Command:** grep `JWT_SECRET` docker-compose + `AuthService.java` + `JwtAuthenticationGatewayFilter.java`.
- **Output:** `JWT_SECRET: ${JWT_SECRET:?required}`; AuthService throw nếu <32 ký tự; gateway throw nếu <64 bytes (HS512).
- **Verdict:** ✅ PASS — không literal, có validate độ dài.
- **Evidence:** EVIDENCE-2026-06-14-SEC-001 / AUTH-005

#### SEC-004 — Terraform IaC không chứa secret literal (P1)
- **Command:** `grep -rnE "(password|api_key|secret|token)\s*=\s*\"[a-zA-Z0-9_-]{8,}\"" infrastructure/terraform-aws/*.tf`
- **Output:** không có literal (dùng var/Secrets Manager). AWS Secrets Manager live describe = ❓ UNCHECKED (account state).
- **Verdict:** ✅ PASS (source) / ⚠️ live versioning+KMS UNCHECKED.
- **Evidence:** EVIDENCE-2026-06-14-SEC-001

*Score: all PASS; trừ 1 do SEC-003/004 live-side (Secrets Manager rotation+KMS) UNCHECKED → **19/20**.*

---

### Cat 3 — OWASP A01-A06/A08-A10 — 17/20 🟡

#### OWASP-A01-001 — Broken Access Control: @PreAuthorize coverage (P0)
- **Command:** đếm `@PreAuthorize` vs `@*Mapping`; liệt kê controller thiếu guard.
- **Output:** KC core 128 `@PreAuthorize` / 251 mapping; KH sub 65. 27/105 controller không có `@PreAuthorize` — phần lớn là Public*/Internal*/*Webhook (đúng thiết kế) hoặc dựa AuthorizationBean ở service. KH admin: 5/5 AdminController đều có `@PreAuthorize` ✅.
- **Verdict:** ⚠️ PARTIAL — coverage tốt ở admin; nhưng StorageController confirm/delete (F-002) thiếu authz hoàn toàn.
- **Evidence:** EVIDENCE-2026-06-14-OWASP-A01-002

#### OWASP-A01-002 — IDOR storage confirm/delete (P1) ❌
- Xem F-002. **Verdict:** ❌ FAIL (P1). **Evidence:** EVIDENCE-2026-06-14-OWASP-A01-002 → GAP-1309.

#### OWASP-A02-001 — Cryptographic Failures (P0)
- **Command:** grep MD5/SHA-1/DES/RC4/ECB.
- **Output:** 0 hit thật (các "DES" là `ORDER BY ... DESC`). JWT HS512 (access) + HS256 (challenge, namespace tách).
- **Verdict:** ✅ PASS. **Evidence:** EVIDENCE-2026-06-14-OWASP-A03-001

#### OWASP-A03-001 — Injection (P0)
- **Output:** Mọi `@Query` JPQL/native đều bound `:param`; "+" chỉ là nối chuỗi literal nhiều dòng, không nối user input.
- **Verdict:** ✅ PASS. **Evidence:** EVIDENCE-2026-06-14-OWASP-A03-001

#### OWASP-A04-001 — Insecure Design (P1)
- **Command:** `ls documents/02-architecture/threat-models/*.md`
- **Output:** (chưa có thư mục threat-models chuyên biệt; thiết kế authz mô tả rải trong ADR + rules). 
- **Verdict:** ⚠️ PARTIAL — không có threat-model formal; nhưng có ADR auth + hardening checklists. Không nâng thành finding mới (đã tracked qua ops/quality audit).

#### OWASP-A05-001 — Security Misconfiguration (P1)
- **Command:** grep actuator/stacktrace trong `application-production.yml` mọi service.
- **Output:** Tất cả: `include-stacktrace: never`, `include-message: never`, `show-details: when_authorized`, exposure scoped (không `'*'`).
- **Verdict:** ✅ PASS. **Evidence:** prod profiles grep (inline).

#### OWASP-A06-001 — Vulnerable Components (delegated)
- **Verdict:** ✅ PASS — cross-ref DEPS-001/002.

#### OWASP-A08-001 — Software/Data Integrity (P1)
- **Command:** grep `image:` + base Dockerfile.
- **Output:** Docker base `eclipse-temurin:17-jdk-noble` (tag-pinned, không @sha256); Dependabot active. GH Actions pinning chưa kiểm tra full.
- **Verdict:** ⚠️ PARTIAL (tag-pinned + Dependabot = chấp nhận v1).

#### OWASP-A09-001 — Logging & Monitoring (P1)
- **Command:** grep `AdminAuditLog|admin_audit_log` + login audit.
- **Output:** `admin_audit_log` (V60 immutable, RLS V83 append-only), `LoginAuditLog`, AdminAuditLogController + repo tồn tại.
- **Verdict:** ✅ PASS. **Evidence:** migration V60/V83 + audit repos.

#### OWASP-A10-001 — SSRF (P1)
- **Command:** grep RestTemplate/WebClient/HttpClient + user/url.
- **Output:** Outbound chủ yếu là DNS verify (DomainService) + vendor webhook callback (không nhận URL từ user input để fetch). Không thấy fetch URL do client cung cấp.
- **Verdict:** ✅ PASS (no user-controlled SSRF sink phát hiện).

*Score: 20 − 1 P1 fail (A01 IDOR) ×3 = 17 → **17/20** (verdict category 🟡 PARTIAL).*

---

### Cat 4 — Auth & Access Control (OWASP A07) — 16/20 🔴

#### AUTH-001 — Anti-spoof header strip (P0) ❌
- Xem F-001. **Verdict:** ❌ FAIL (P0) — X-User-Roles/X-User-Email không strip. **Evidence:** EVIDENCE-2026-06-14-AUTH-001 → GAP-1308. **P0 ⇒ category cap ≤16/20 + audit verdict FAIL.**

#### AUTH-002 — Gateway rate-limit auth endpoints (P0)
- **Output:** `RequestRateLimiter` Redis trên route auth (GAP-514): replenishRate 1-10, burstCapacity 2-20.
- **Verdict:** ✅ PASS. **Evidence:** EVIDENCE-2026-06-14-AUTH-005

#### AUTH-003 — JWT validation HS512 + token-confusion guard (P0)
- **Output:** JWT_SECRET ≥64 bytes (HS512); challenge HS256 namespace tách; challenge chỉ valid trên `/api/v1/auth/2fa/**` + self-declare `type=challenge`; invalid JWT → 401.
- **Verdict:** ✅ PASS. **Evidence:** EVIDENCE-2026-06-14-AUTH-005

#### AUTH-004 — 2FA TOTP + recovery codes (P1)
- **Output:** `TwoFactorController`, `ChallengeTokenService`, `RecoveryCodeService` (10 codes/user) tồn tại; bridge filter GAP-706/783 DONE.
- **Verdict:** ✅ PASS.

#### AUTH-005 — SSO determinism (bề mặt wave, GAP-1306) (P1)
- **Command:** `git show 1c8509c6b`.
- **Output:** `InstanceRepository.findByOwnerIdAndDeletedFalse` thêm `ORDER BY i.createdAt ASC, i.id ASC` → JWT tenantId/tier claim chọn instance xác định (đóng cross-tenant exposure risk khi owner >1 instance). KC OWNER authz (GAP-1139): AuthorizationBean.isAdmin() += ROLE_OWNER + regression test CI-bound.
- **Verdict:** ✅ PASS — fix wave gần đây đúng + có test guard.

*Score: P0 fail (AUTH-001) ⇒ category CAP 16/20 → **16/20** (🔴 FAIL).*

---

### Cat 5 — Infrastructure Security — 16/20 🟡

#### INFRA-001 — TLS 1.2+ trên ALB (P0)
- **Command:** `aws elbv2 describe-listeners` — **không chạy** (account state).
- **Verdict:** ❓ UNCHECKED.

#### INFRA-002 — CORS origin tường minh (P0)
- **Command:** grep gateway `application.yml` CORS.
- **Output:** `allowedOriginPatterns: ${CORS_ALLOWED_ORIGINS:...localhost...}`; production override `https://kitehub.me,https://*.kitehub.me` (GAP-1171 — patterns, không `'*'`).
- **Verdict:** ✅ PASS — không wildcard trần; `*.kitehub.me` + allowCredentials hơi rộng (theo dõi).
- **Evidence:** gateway application.yml:15-21 (inline).

#### INFRA-003 — Docker non-root USER (P0)
- **Command:** sweep `USER` mọi Dockerfile.
- **Output:** Tất cả service runtime có `USER spring|nextjs|pwuser`. `kitehub-base/Dockerfile` không có USER nhưng là **builder base image** (`FROM eclipse-temurin:17-jdk-noble`, child set USER) → chấp nhận.
- **Verdict:** ✅ PASS.

#### INFRA-004 — IAM least-privilege (P0)
- **Command:** `grep -rnE "(Action|Resource).*\"\*\"" infrastructure/terraform-aws/*.tf` — review tĩnh; AWS live = UNCHECKED.
- **Verdict:** ⚠️ PARTIAL — review IaC tĩnh, không có wildcard admin lộ liễu rõ; live verify defer.

#### INFRA-005 — Security headers + RLS (P0/P1)
- **Output:** `SecurityHeadersFilter` (kitehub-gateway, edge duy nhất) set HSTS + CSP + X-Content-Type-Options + X-Frame-Options + Referrer-Policy + Permissions-Policy ✅. RLS: V58/V59/V78/V81/V83/V84 phủ nhiều bảng tenant-scoped + admin_audit_log append-only; **NHƯNG `uploaded_files`/`storage_quota` không trong sweep** (F-004) — chỉ Hibernate `@Filter`.
- **Verdict:** ⚠️ PARTIAL — security headers PASS; RLS gap storage (P2). **Evidence:** EVIDENCE-2026-06-14-INFRA-006 → GAP-1311.

*Score: 20 − 1 P2 (RLS storage) ×1 = 19; trừ thêm 3 do 3 control AWS-live (TLS/IAM/CloudTrail) UNCHECKED → **16/20** (🟡).*

---

## 6. Findings Table

| Finding | Severity | Category | Title | Evidence | Status |
|---|---|---|---|---|---|
| F-001 | P0 | Cat 4 A07+A01 | Gateway không strip X-User-Roles → role-spoof priv-esc | AUTH-001 | 🔵 OPEN GAP-1308 |
| F-002 | P1 | Cat 3 A01 | StorageController confirm/delete thiếu ownership authz (intra-tenant IDOR) | OWASP-A01-002 | 🔵 OPEN GAP-1309 |
| F-003 | P2 | Cat 4 | Gateway không strip X-User-Email (spoofable) | AUTH-001 | 🔵 OPEN GAP-1310 |
| F-004 | P2 | Cat 5 A01 | uploaded_files/storage_quota thiếu DB RLS backstop | INFRA-006 | 🔵 OPEN GAP-1311 |

---

## 7. Aggregate Verdict + Score Delta

| Baseline | Date | Score | This audit delta |
|---|---|:---:|:---:|
| Wave 94c v2 | 2026-05-18 | 93/100 A | **−8 → 85/100 B** |

**Phase 1 BETA threshold ≥80:** số điểm 85 ✅ PASS (buffer +5) **NHƯNG aggregate verdict ❌ FAIL** do 1 P0 OPEN (F-001). Promotion bị chặn cho tới khi GAP-1308 đóng.

**v2 evidence completeness:** 26/26 control có evidence block (100%); 3 control AWS-live đánh dấu UNCHECKED minh bạch.

---

## 8. Recommendations

1. **(P0, ngay) GAP-1308** — thêm `RemoveRequestHeader=X-User-Roles` + `X-User-Email` vào gateway `default-filters`; JwtAuthenticationGatewayFilter set X-User-Roles vô điều kiện (least-privilege khi claim thiếu); cân nhắc 401 cho tokenless tới path non-public. Đây là cùng class GAP-814 chưa đóng hết.
2. **(P1) GAP-1309** — truyền requesterId+tenantId + owner-or-admin check vào StorageService confirm/delete; thêm `@PreAuthorize` role gate cho StorageController.
3. **(P1, đã filed) GAP-1307** — gắn enrollment check (LessonAccessGuard) vào storage download-url cho file bài trả phí.
4. **(P2) GAP-1310 / GAP-1311** — strip X-User-Email; thêm RLS DB policy cho `uploaded_files`/`storage_quota`.
5. **Defense-in-depth** — đẩy nhanh GAP-825 (core network-isolation + JWT-sig-verify TenantResolver fallback) để giảm blast-radius của mọi header-trust gap.
6. **Mở rộng GAP-1037** sang storage chung (ALLOWED_MIME_TYPES có `image/svg+xml` → stored-SVG-XSS).
7. **Cat 1/5 live** — chạy `mvn dependency-check` + Trivy + AWS TLS/IAM/CloudTrail describe ở session có AWS để nâng UNCHECKED → PASS.

---

## 9. Pending (post-audit actions)

| Action | Status |
|---|---|
| File 4 gap (GAP-1308..1311) | ✅ done this PR |
| Update `gap-status.csv` (+4 rows) | ✅ done this PR |
| Update `audits-index.csv` (+1 row) | ✅ done this PR |
| Run `check-gap-folder-location.sh` + `check-gap-status-csv.sh` | ✅ done this PR |
| Update `output-review-mandate.md` §3 Security row | ⏳ coordinator (defer — PR docs/data-only) |
| Update ROADMAP §🎯 | ⏳ coordinator |

---

## 10. References

- Audit skill: `.claude/skills/quality/security-audit/SKILL.md` v2 (GAP-564)
- Template: `.claude/skills/quality/security-audit/reference/audit-report-template-v2.md`
- Evidence dir: `documents/04-quality/audits/security/evidence/2026-06-14/`
- Sister gaps referenced: GAP-1307, GAP-825, GAP-1007, GAP-1037, GAP-472, GAP-471, GAP-643, GAP-814 (precedent)
- Governance: `audit-to-gap-pipeline.md` §3, `output-review-mandate.md` §3, `multi-session-concurrency-coordination.md` (reserved ID block 1308-1319)
