---
audience: mixed
audit_type: api-contract
scope: Wave auth-1 (KC-native login) — PR #2186, commit 2b01ac93
date: 2026-06-06
score: 85/100
grade: B
verdict: PARTIAL FAIL (Cat 1 endpoint coverage — 2 undocumented endpoints)
---

# API-Contract Audit — Wave auth-1 (KC-native login)

**Phạm vi:** Code merged trong PR #2186 (commit `2b01ac93`) — KC-native login Option B (GAP-725) + gateway `X-User-Reference-Id` propagation. Đối chiếu endpoint trong code với `documents/01-business/**/api-contract.md`.

**Phương pháp:** `.claude/skills/quality/api-contract-audit/SKILL.md` — bug-finding > scoring (§3 primacy). Liệt kê mọi endpoint drift trước, score sau.

---

## 1. Bug list (mismatch-first per §3 primacy)

### 🟠 P1 — `POST /api/v1/tenant-auth/login` KHÔNG có api-contract.md (undocumented public auth endpoint)

- **Code:** `AuthController.java:33` `@PostMapping("/login")` dưới `@RequestMapping("/api/v1/tenant-auth")` (`AuthController.java:26`).
- **Drift:** KHÔNG tồn tại domain doc `documents/01-business/kiteclass/auth/` hay `tenant-auth/` — toàn bộ 3-layer (`rules.md` + `use-cases.md` + `api-contract.md`) vắng mặt. Đây là endpoint **public auth** (gateway whitelist `/api/v1/auth/**` + route `kc-tenant-auth` no-filter) — security-sensitive + FE consumer (parent/student/teacher login) build trực tiếp lên nó.
- **Request/Response (code):** `LoginRequest{email(@Email,@NotBlank), password(@NotBlank)}` → `ApiResponse<LoginResponse{accessToken, tokenType, expiresInSeconds, role, referenceId, tenantId}>`, HTTP 200.
- **Error semantics (code):** uniform 401 `INVALID_CREDENTIALS` (no user-enumeration — `AuthService.java:49`) + 400 bean-validation. KHÔNG có error table trong doc.
- **Proposed fix:** Tạo 3-layer doc `documents/01-business/kiteclass/auth/` (hoặc `tenant-auth/`): BR cho lookup pre-auth global-email, UC-AUTH-01 login flow, api-contract endpoint + request/response shape + error codes (401 INVALID_CREDENTIALS, 400 validation) + 12h TTL note.

### 🟠 P1 — `POST /api/v1/teachers/{id}/credentials` KHÔNG có trong teacher/api-contract.md

- **Code:** `TeacherController.java:71` `@PostMapping("/{id}/credentials")` + `@PreAuthorize("hasAnyRole('OWNER','ADMIN','PRINCIPAL')")`.
- **Drift:** `documents/01-business/kiteclass/teacher/api-contract.md` liệt kê POST/GET/PUT/DELETE `/teachers` + `/teachers/search` + `/classes/{classId}/teachers` + `/internal/teachers` — nhưng KHÔNG có `/{id}/credentials`. Endpoint admin-provisioning mới (set/reset teacher login password) hoàn toàn vắng mặt.
- **Request/Response (code):** `SetPasswordRequest{password(@Size 8-100, @Pattern letter+digit+symbol)}` → `ApiResponse<Void>` message `"Đặt mật khẩu giáo viên thành công"`, HTTP 200.
- **Proposed fix:** Thêm section `### POST /api/v1/teachers/{id}/credentials` vào `teacher/api-contract.md` — auth `OWNER/ADMIN/PRINCIPAL`, request shape (password rule), 200 success, 403 non-admin, 400 weak password, 404 teacher not found.

### 🟡 P2 — `X-User-Reference-Id` header SOURCE drift (parent/student-portal api-contract.md)

- **Doc:** `parent-portal/api-contract.md:16` mô tả `X-User-Reference-Id` source = "Gateway (`users.reference_id` cho `userType=PARENT`)"; line 266 tương tự.
- **Drift:** Wave auth-1 Option B **đổi producer** — `referenceId` giờ mint trực tiếp từ `auth_credentials.entity_id` bởi `AuthTokenService.java:70` (KC-native token), KHÔNG còn qua cross-service `users.reference_id` population (commit message xác nhận: "Option B simplifies GAP-798b... no cross-service users.reference_id population"). Doc description đã stale.
- **Proposed fix:** Cập nhật source description: "Gateway re-inject từ `referenceId` claim của KC-native token (= `auth_credentials.entity_id`)" cho cả parent-portal + student-portal api-contract.md.

### 🟡 P2 — Gateway anti-spoof header contract (`X-User-Reference-Id`) chưa documented

- **Code:** Gateway `default-filters: RemoveRequestHeader=X-User-Reference-Id` (application.yml) strip client value + `JwtAuthenticationGatewayFilter.java:200-207` re-inject verified claim. Đây là contract bảo mật quan trọng (anti-spoof, giống `X-User-Id`).
- **Drift:** Không api-contract.md nào ghi behavior strip+re-inject + "client-supplied → bị bỏ". Reader/consumer không biết header này gateway-only-trusted.
- **Proposed fix:** Thêm note vào parent/student-portal api-contract Headers table: "client-supplied bị strip; chỉ gateway-injected từ verified JWT claim".

### 🟢 P3 — Parent redeem credential side-effect chưa phản ánh trong use-cases/api-contract

- **Code:** `POST /api/v1/parent-invitations/redeem/{token}` (ParentInvitationController) — endpoint contract KHÔNG đổi, nhưng `ParentInvitationServiceImpl.redeem` giờ provision `auth_credentials` (side-effect mới để parent login được). Endpoint đã documented (`parent-portal/api-contract.md:61`).
- **Proposed fix:** Thêm 1 dòng vào UC-PARENT-02 use-case: "redeem cũng provision login credential (auth_credentials) idempotent-on-email → parent login qua /api/v1/tenant-auth/login". Contract response shape không đổi → P3.

---

## 2. Điểm đúng (no drift — verified PASS)

| Hạng mục | Verdict |
|---|---|
| **HTTP status correctness** | ✅ login 200 (không tạo resource → không 201); teacher credentials 200 (UPSERT idempotent set-password → không 201, không trả resource URI) |
| **Uniform 401 no-enumeration** | ✅ `AuthService.java:43-50` — unknown email / disabled / wrong password đều 401 `INVALID_CREDENTIALS` |
| **Versioning** | ✅ `/api/v1/` prefix nhất quán; `tenant-auth` namespace tách sạch khỏi `/api/v1/auth/**` (KH subscription OWNER/STAFF) — không collision |
| **Error envelope consistency** | ✅ login/teacher-credentials dùng `ErrorResponse{code,message,path}` qua `GlobalExceptionHandler` — nhất quán convention kiteclass-core (lưu ý: kiteclass-core KHÔNG dùng RFC7807 ProblemDetail; ErrorResponse là chuẩn nội bộ, không phải drift) |
| **DTO shape** | ✅ `LoginRequest`/`LoginResponse`/`SetPasswordRequest` records well-formed, validation đầy đủ |
| **No breaking change** | ✅ existing teacher endpoints (POST/GET/PUT/DELETE) không đổi signature |

---

## 3. Score (per `audit-skill-rubric-api-contract-audit.md` 5 categories /20)

| # | Category | Score | Sub-check fails |
|---|----------|:-----:|-----------------|
| 1 | Endpoint Coverage | **14/20** | 2× P1 (tenant-auth/login + teachers/{id}/credentials undocumented) → 20−6 |
| 2 | Request/Response Match | **18/20** | 2× P2 (X-User-Reference-Id source drift + anti-spoof header behavior undoc) → 20−2 |
| 3 | Error Code Consistency | **18/20** | 2× P2 (error codes của 2 endpoint mới chưa documented; envelope consistent) → 20−2 |
| 4 | Versioning & Deprecation | **19/20** | 1× P2 (no changelog/version note cho endpoint mới — doc vắng mặt) → 20−1 |
| 5 | Integration Test Coverage | **16/20** | 1× P1 (no AuthControllerIT / MVC contract test cho public auth endpoint — chỉ Mockito service test + manual walk) + 1× P2 → 20−3−1 |

**TOTAL: 85/100 — Grade B**

**Audit-level verdict: PARTIAL FAIL** — không có P0 (endpoint hoạt động + secure), nhưng Cat 1 có 2 P1 undocumented endpoints. Score B phản ánh code chất lượng tốt (DTO/security/status đúng) nhưng documentation là điểm yếu: 2 endpoint hoàn toàn không doc + tenant-auth thiếu cả 3-layer doc domain.

---

## 4. Khuyến nghị ưu tiên

1. **(P1)** Tạo 3-layer doc `documents/01-business/kiteclass/auth/` (hoặc `tenant-auth/`) cho `POST /api/v1/tenant-auth/login` — vi phạm CLAUDE.md 3-layer mandate (doc + code phải cùng PR).
2. **(P1)** Bổ sung `### POST /api/v1/teachers/{id}/credentials` vào `teacher/api-contract.md`.
3. **(P1)** Thêm AuthControllerIT MVC test cho login flow (200 happy + 401 uniform + 400 validation) — per `pre-handoff-self-test-completeness.md` §2.1.
4. **(P2)** Sửa `X-User-Reference-Id` source description (parent + student portal) phản ánh Option B mint-from-credential.
5. **(P2)** Document gateway anti-spoof strip+re-inject contract.

---

## 5. References

- Commit: `2b01ac93` (PR #2186) — feat(wave-auth-1) KC-native login Option B
- Wave plan: `documents/03-planning/waves/wave-2026-06-06-auth-1-kc-native-login.md`
- Rubric: `.claude/rules/audit-skill-rubric-api-contract-audit.md`
- Liên quan: GAP-725 (parent/student/teacher auth pull-forward), GAP-798/798b (reference-id authz)
