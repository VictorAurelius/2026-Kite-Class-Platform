---
audience: dev
date: 2026-06-05
session-theme: Pre-walk persona simulation — KC-2 Staff invitation + RBAC
flow: KC-2 (Owner mời staff → email token → accept → STAFF account → RBAC)
method: read-only static analysis (no stack run, no code edit)
mandate: .claude/rules/pre-walk-persona-simulation-mandate.md §1 + §3
predicted_failure_modes: 9
known_blockers_referenced: GAP-886, GAP-893, GAP-784
prior_walk: 2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md (17 bugs, kiteclass-core impl — DIFFERENT module)
---

# Pre-walk persona simulation — KC-2 Staff invitation + RBAC (2026-06-05)

## Tóm tắt điều hành (đọc trước)

Flow KC-2 trong codebase hiện tại được implement ở **`kitehub-subscription`** (KHÔNG phải `kiteclass-core` như prior-walk Wave meta-6). Đây là bản rewrite UUID-native, và **2 P0 lớn nhất của prior-walk đã được fix**:
- Prior Bug #14 (email không gửi) → ĐÃ CÓ: `StaffInvitationController.dispatchInviteEmail()` gọi `emailServiceClient.sendInviteStaffEmail()` thật (line 271-288 + EmailServiceClient:799).
- Prior Bug #17 (accept không tạo user) → ĐÃ CÓ: `accept()` tạo `User` row + hash password + role STAFF (line 234-242).

Tuy nhiên static analysis phát hiện **9 failure mode mới**, trong đó **3 P0** vẫn block happy path hoặc gây cross-tenant RBAC hole. Cao nhất:
1. **FM-1 (P0):** `User` mới tạo lúc accept KHÔNG có tenant/instance link → STAFF không thuộc trung tâm nào → RBAC + tenant isolation hỏng.
2. **FM-2 (P0):** Role-literal mismatch 3 lớp (`STAFF` seed vs `TENANT_STAFF`/`TENANT_OWNER` SecurityConfig vs `OWNER`/`ADMIN` controller `@PreAuthorize`) — instance cụ thể của GAP-893.
3. **FM-3 (P0):** `requireUser()` trả `null` khi gateway không forward `X-User-Id`, nhưng cột `invited_by NOT NULL` (V45:19) → INSERT fail → Owner mời staff nhận 500.

## Scope inspected

**Backend (`kitehub-subscription`):**
- `src/main/java/com/kitehub/subscription/staff/controller/StaffInvitationController.java` (449 dòng — đọc full)
- `src/main/java/com/kitehub/subscription/staff/service/StaffInvitationService.java` (207 dòng — đọc full)
- `src/main/java/com/kitehub/subscription/staff/entity/StaffInvitation.java` (UUID-native, line 41-84)
- `src/main/java/com/kitehub/subscription/config/SecurityConfig.java` (line 60-149 — test chain permitAll vs prod chain default-deny)
- `src/main/resources/db/migration/V45__create_staff_invitations.sql` (line 14-50 — `invited_by NOT NULL`)
- `src/main/java/com/kitehub/subscription/client/EmailServiceClient.java` (line 799-825 — `sendInviteStaffEmail` RestTemplate-based)
- `kitehub-platform/.../domain/entity/User.java` (line 21-111 — UUID id, String role, KHÔNG có tenant_id/instance_id)

**Gateway (`kitehub-gateway`):**
- `filter/JwtAuthenticationGatewayFilter.java` (line 218-242 — public allowlist by-token + /accept)
- `filter/TenantHeaderGuardFilter.java` (line 160-161)
- `src/main/resources/application.yml` (line 627-672 — route `staff-invitations-public-token` order)

**Frontend (`kitehub-frontend`):**
- `src/app/(admin)/admin/staff/invite/page.tsx` (185 dòng — Owner invite form, role hard-fixed STAFF)
- `src/app/(public)/staff/accept-invite/page.tsx` (283 dòng — invitee accept + set password, Suspense-wrapped)
- `src/lib/api/endpoints.ts` (line 118-124 — staffInvitations config, khớp BE)

**Business docs:** `documents/01-business/kiteclass/staff-invitation/{rules,use-cases,api-contract}.md` + `role-hierarchy/` (3-layer present).

**Prior walk:** `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` (17 bugs — nhưng impl `kiteclass-core`, khác module với bản đang review).

---

## Predicted failure modes (9)

### FM-1: STAFF user mới KHÔNG được link vào tenant/instance — cross-tenant RBAC hole

- **(a) WHERE:** `StaffInvitationController.accept()` line 234-242 (`User.builder()...role("STAFF")...build()`) + `kitehub-platform/.../domain/entity/User.java` line 21-111 (entity KHÔNG có cột `tenant_id` / `instance_id`).
- **(b) SYMPTOM:** Invitee đặt password xong → account STAFF được tạo, nhưng row `User` không mang `tenantId` của lời mời. `accepted.getTenantId()` chỉ đi vào `AcceptStaffInvitationResponse` (line 253) + audit log — KHÔNG ghi vào User. Khi STAFF login, JWT không có tenant claim đúng → hoặc (i) STAFF thấy được trung tâm khác, hoặc (ii) STAFF không thuộc trung tâm nào → mọi tenant-scoped endpoint 403. Persona invitee: "Đăng nhập được nhưng không thấy lớp/dữ liệu trung tâm mời mình."
- **(c) PRE-WALK CHECK:**
  ```bash
  # Xác nhận User entity không có tenant scope
  grep -nE "tenant_id|instance_id|tenantId|instanceId" kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/entity/User.java
  # → kỳ vọng: 0 hit (xác nhận hole)
  # Xác nhận accept() không set tenant lên User
  sed -n '234,255p' kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/controller/StaffInvitationController.java
  ```
- **SEVERITY: P0** (cross-tenant isolation + RBAC core — không thể DONE flow KC-2 với hole này).

### FM-2: Role-literal mismatch 3 lớp — STAFF seed vs SecurityConfig vs controller @PreAuthorize (instance của GAP-893)

- **(a) WHERE:** 3 vị trí xung đột:
  - `accept()` line 238: tạo User `.role("STAFF")`.
  - `SecurityConfig.java` line 104-109: dùng literal `TENANT_OWNER`, `TENANT_STAFF`, `TENANT_USER`, `PLATFORM_ADMIN` (prefix `TENANT_`).
  - `StaffInvitationController.OWNER_AUTHZ` line 90-91: `hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')` (KHÔNG có `TENANT_OWNER`).
- **(b) SYMPTOM:** STAFF mới tạo có role `"STAFF"` (không prefix) — `hasAnyRole('TENANT_STAFF')` ở SecurityConfig sẽ KHÔNG match `STAFF` (Spring `hasRole` thêm prefix `ROLE_` → cần `ROLE_TENANT_STAFF` vs `ROLE_STAFF`). Đồng thời Owner thật có role gì? Nếu Owner seed là `TENANT_OWNER` thì `OWNER_AUTHZ` (`hasAnyRole('OWNER',...)`) KHÔNG match → Owner POST invite nhận 403 "Bạn không có quyền mời nhân viên" (FE xử lý code `FORBIDDEN` ở invite/page.tsx line 76-79). Persona Owner: "Tôi là chủ mà bấm Gửi lời mời báo không có quyền."
- **(c) PRE-WALK CHECK:**
  ```bash
  # Liệt kê mọi role literal trong subscription
  grep -rnE "\"(OWNER|STAFF|ADMIN|TENANT_OWNER|TENANT_STAFF|TENANT_USER|PLATFORM_ADMIN)\"|hasRole\(|hasAnyRole\(|\.role\(" \
    kitehub/kitehub-subscription/src/main/java kitehub/kitehub-platform/src/main/java | grep -iE "role|OWNER|STAFF|ADMIN"
  # So sánh: seed role STAFF (controller:238) vs guard TENANT_STAFF (SecurityConfig:105) vs OWNER_AUTHZ OWNER (controller:91)
  # Tìm Owner seed role thật
  grep -rnE "role.*OWNER|OWNER.*role|TENANT_OWNER" kitehub/*/src/main/resources/db/migration/*.sql scripts/seed-data.sh 2>/dev/null
  ```
- **SEVERITY: P0** (block cả Owner-invite path lẫn STAFF-RBAC; trùng class GAP-893 nhưng manifest cụ thể ở 3 literal).

### FM-3: `invited_by NOT NULL` nhưng `requireUser()` trả null → Owner mời staff 500

- **(a) WHERE:** `StaffInvitationController.requireUser()` line 332-344 (return `null` khi `X-User-Id` header thiếu/malformed — comment line 334-337 "Allow null for legacy callers") → `create()` line 123 `UUID ownerId = requireUser(ownerHeader)` → `service.create(tenantId, ownerId, ...)` line 128 → entity `.invitedBy(invitedBy)` (StaffInvitationService line 131). Cột `invited_by UUID NOT NULL` (V45 line 19).
- **(b) SYMPTOM:** Nếu gateway KHÔNG forward `X-User-Id` (hoặc forward sai format), `ownerId = null` → JPA INSERT vào `invited_by NOT NULL` → `DataIntegrityViolationException` → 500. `@ExceptionHandler` không có handler cho exception này → ProblemDetail generic 500. Persona Owner: "Bấm Gửi lời mời → lỗi hệ thống, không rõ vì sao." Đây là sister-class của prior-walk Bug #13 (UserContext null) nhưng manifest qua DB constraint thay vì 401.
- **(c) PRE-WALK CHECK:**
  ```bash
  # Xác nhận constraint NOT NULL
  grep -nE "invited_by" kitehub/kitehub-subscription/src/main/resources/db/migration/V45__create_staff_invitations.sql
  # Xác nhận gateway forward X-User-Id cho route staff-invitations create
  grep -nE "X-User-Id|userId.*header|addRequestHeader.*User" kitehub/kitehub-gateway/src/main/resources/application.yml kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/*.java
  # → nếu gateway không set X-User-Id cho POST /staff-invitations → null → 500
  ```
- **SEVERITY: P0** (Owner happy path đầu tiên — invite — fail nếu gateway header không khớp; cần verify gateway forward thực tế lúc walk).

### FM-4: `resend()` xoay token nhưng nếu email dispatch fail → invitation mới có token mà invitee KHÔNG nhận được (sister GAP-927)

- **(a) WHERE:** `resend()` line 154-181 → `service.resend()` (StaffInvitationService line 86-99) set `tokenHash = hash(newRawToken)` + save (token cũ chết ngay) → rồi `dispatchInviteEmail()` (line 177). `dispatchInviteEmail` + `EmailServiceClient.sendInviteStaffEmail` đều best-effort try/catch nuốt exception (controller line 287 + EmailServiceClient line 821-823).
- **(b) SYMPTOM:** Owner bấm "Gửi lại lời mời" → DB rotate token mới (token cũ trong email cũ chết) → email dispatch fail (SMTP/RabbitMQ/RestTemplate down) → exception bị nuốt → controller trả 200 OK. Invitee giờ có token cũ chết + không nhận token mới → click link cũ ra 410 "Lời mời đã hết hạn". Cùng pattern GAP-927 (rollback/rotation invisible to caller). Persona Owner: "Tôi gửi lại rồi mà nhân viên vẫn báo link hết hạn."
- **(c) PRE-WALK CHECK:**
  ```bash
  # Xác nhận resend rotate token TRƯỚC dispatch + dispatch nuốt lỗi
  sed -n '85,99p' kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/service/StaffInvitationService.java
  sed -n '271,288p' kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/controller/StaffInvitationController.java
  # check-be-rollback-side-effects.sh (per pre-walk-static-audit-bundle.md) — rotation invisible class
  bash scripts/check-be-rollback-side-effects.sh 2>/dev/null
  ```
- **SEVERITY: P1** (degraded — không corrupt data nhưng invitee stuck; cần surface email-dispatch failure cho Owner).

### FM-5: `create()` cũng best-effort email — Owner thấy "đã gửi" nhưng invitee không bao giờ nhận

- **(a) WHERE:** `create()` line 135-137 comment "Best-effort email dispatch — failure logged, invitation row still valid" → `dispatchInviteEmail()` nuốt mọi exception. Controller trả 201 + FE `router.push('/admin/staff?invited=1')` (invite/page.tsx line 60) bất kể email gửi được hay không.
- **(b) SYMPTOM:** Email service down → row PENDING tạo OK → Owner thấy toast "đã mời" + row xuất hiện trong list → nhưng invitee KHÔNG nhận email → flow chết im lặng. Đây chính là lại lớp prior Bug #14 ở dạng nhẹ hơn (email path tồn tại nhưng failure mode vô hình). Persona invitee: "Tôi chưa nhận được email nào." Persona Owner: "Tôi mời rồi mà sao chưa thấy nhân viên vào?"
- **(c) PRE-WALK CHECK:**
  ```bash
  # Verify MailHog nhận được email sau khi POST create (lúc walk thật)
  curl -s http://localhost:8025/api/v2/messages | python3 -c "import sys,json; d=json.load(sys.stdin); print('emails:', d.get('total',0))"
  # Verify template 'invite-staff' tồn tại trong kitehub-email
  find kitehub -path '*templates*invite-staff*' -o -name '*invite-staff*' 2>/dev/null | grep -iE "template|html|ftl|hbs"
  ```
- **SEVERITY: P1** (happy path "thành công" giả; cần verify email thực sự gửi + template tồn tại lúc walk).

### FM-6: Gateway route `/accept` allowlist dùng `endsWith("/accept")` — token chứa "/accept"? path-match an toàn nhưng raw token base64url có thể chứa ký tự lạ

- **(a) WHERE:** `JwtAuthenticationGatewayFilter.java` line 242 `(path.startsWith("/api/v1/staff-invitations/") && path.endsWith("/accept"))` + gateway route `application.yml` line 639 `Path=/api/v1/staff-invitations/by-token/**,/api/v1/staff-invitations/*/accept`. Token sinh bởi `Base64.getUrlEncoder().withoutPadding()` (StaffInvitationService line 186) → chứa `-` và `_` (URL-safe, OK) nhưng KHÔNG chứa `/`.
- **(b) SYMPTOM:** Trường hợp bình thường OK (token url-safe không có `/`). Rủi ro: nếu invitee URL-decode/double-encode sai, hoặc token bị truncate, `endsWith("/accept")` vẫn match nhưng `{token}` path-variable rỗng/sai → `findActiveByToken` trả empty → 404 `INVALID_OR_EXPIRED_TOKEN`. FE accept page (line 127-131) map sang "Lời mời không còn hiệu lực" — đúng nhưng có thể gây nhầm với token thật hết hạn. Persona invitee: "Link của tôi báo hết hạn mà tôi vừa nhận email xong."
- **(c) PRE-WALK CHECK:**
  ```bash
  # Xác nhận token charset url-safe (không có '/')
  grep -nE "getUrlEncoder|Base64|generateRawToken" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/service/StaffInvitationService.java
  # Walk: copy token thật từ DB → curl GET by-token + POST accept, verify gateway không strip
  bash scripts/check-gateway-shared-breaker.sh 2>/dev/null  # per pre-walk bundle
  ```
- **SEVERITY: P2** (edge case; happy path url-safe token an toàn — verify routing không strip token lúc walk).

### FM-7: Accept không có rate-limit / brute-force guard trên token public endpoint

- **(a) WHERE:** `getByToken()` line 209-214 + `accept()` line 216-255 — public (gateway permitAll). Token SHA-256 32-byte (đủ entropy) nhưng KHÔNG có rate-limit ở controller; gateway comment (application.yml line 133) chỉ note rate-limit cho `/public/tenants/**`, không thấy cho `/staff-invitations/by-token`.
- **(b) SYMPTOM:** Endpoint `GET /by-token/{token}` + `POST /{token}/accept` public không rate-limit → kẻ tấn công brute-force token space. Entropy 256-bit khiến brute-force impractical (mitigant mạnh), nhưng thiếu rate-limit vẫn là OWASP A04 (Insecure Design) gap per `pre-launch-owasp-rest-hardening-checklist.md` §2.4. Persona: không observable trực tiếp — security posture gap.
- **(c) PRE-WALK CHECK:**
  ```bash
  # Verify có/không rate-limit cho by-token route ở gateway
  grep -nE "RequestRateLimiter|redis-rate-limiter|staff-invitations.*rate|by-token.*rate" kitehub/kitehub-gateway/src/main/resources/application.yml
  # → kỳ vọng: 0 hit cho by-token (xác nhận gap)
  ```
- **SEVERITY: P2** (mitigated bởi token entropy; nên thêm rate-limit trước GA per OWASP A04).

### FM-8: FE accept page — token lấy từ `useSearchParams` chỉ trong client Suspense; nếu token có ký tự cần encode → mismatch hash

- **(a) WHERE:** `accept-invite/page.tsx` line 48-50 `params.get('token')` → dùng raw trong `endpoints.staffInvitations.byToken(token)` (line 71-73) + `accept(token)` (line 118). `byToken` builds template string `` `/api/v1/staff-invitations/by-token/${token}` `` (endpoints.ts line 121) KHÔNG `encodeURIComponent`. Token base64url (`-_`) thường an toàn nhưng nếu BE đổi token charset tương lai → break.
- **(b) SYMPTOM:** Token hiện url-safe → OK. Nhưng `dispatchInviteEmail` build URL `inviteBaseUrl + "/staff/accept-invite?token=" + rawToken` (controller line 273) — nếu rawToken chứa ký tự reserved (hiện không, nhưng fragile), query param parse có thể sai. `params.get('token')` đã URL-decode 1 lần; nếu email client encode thêm → double-decode mismatch → hash không khớp → 404. Persona invitee: "Click link trong email ra trang báo lời mời không tồn tại."
- **(c) PRE-WALK CHECK:**
  ```bash
  # Verify token round-trip: raw token (DB) == token FE nhận == hash khớp
  grep -nE "encodeURIComponent|byToken|accept\(" kitehub/kitehub-frontend/src/lib/api/endpoints.ts
  # Walk: lấy inviteUrl thật từ MailHog email → mở browser → verify GET by-token 200
  ```
- **SEVERITY: P2** (happy path url-safe token OK; verify email-link → accept-page round-trip lúc walk).

### FM-9: `revokePendingForEmail` idempotency dùng `listByTenant` full-scan + so sánh email lowercase — nếu email có khoảng trắng/diacritic thì miss

- **(a) WHERE:** `create()` line 126 `revokePendingForEmail()` (line 259-269): filter `email.toLowerCase().trim().equals(i.getEmail())`. Nhưng `i.getEmail()` được lưu lúc create đã `.toLowerCase().trim()` (StaffInvitationService line 132). So sánh OK cho ASCII. FE gửi `email.trim().toLowerCase()` (invite/page.tsx line 56).
- **(b) SYMPTOM:** Edge case: nếu 2 lần invite cùng email nhưng khác casing/whitespace nhỏ, idempotency hoạt động đúng (cả 2 phía normalize). Nhưng `service.create()` còn check `findPendingByTenantAndEmail` (StaffInvitationService line 118) → nếu repo query KHÔNG normalize email giống → có thể tạo 2 PENDING cho cùng email → `INVITATION_ALREADY_PENDING` 409 không fire đúng, hoặc unique index `uq...(tenant_id, email)` (V45 line 37) chặn → 500 thay vì 409 thân thiện. Persona Owner: "Mời lại cùng email báo lỗi hệ thống thay vì 'đã có lời mời'."
- **(c) PRE-WALK CHECK:**
  ```bash
  # Verify repo query findPendingByTenantAndEmail có normalize email không
  grep -nA3 "findPendingByTenantAndEmail" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/repository/StaffInvitationRepository.java
  # Verify partial unique index trên email
  grep -nE "uq_staff_invitations|tenant_id, email|UNIQUE" kitehub/kitehub-subscription/src/main/resources/db/migration/V45__create_staff_invitations.sql
  # Walk: mời cùng 1 email 2 lần → kỳ vọng 409 message thân thiện, KHÔNG 500
  ```
- **SEVERITY: P2** (idempotency mostly OK; verify re-invite cùng email trả 409 không 500 lúc walk).

---

## Summary table

| FM | Title | Layer | Severity |
|----|-------|-------|:--------:|
| FM-1 | STAFF user không link tenant/instance → cross-tenant RBAC hole | BE entity + accept() | **P0** |
| FM-2 | Role-literal mismatch 3 lớp (STAFF/TENANT_STAFF/OWNER) | BE SecurityConfig + controller | **P0** |
| FM-3 | `invited_by NOT NULL` vs `requireUser()` null → Owner invite 500 | BE controller + V45 migration | **P0** |
| FM-4 | resend rotate token trước, dispatch nuốt lỗi → invitee stuck | BE resend + email | P1 |
| FM-5 | create best-effort email → "đã gửi" giả, invitee không nhận | BE create + email | P1 |
| FM-6 | Gateway `/accept` endsWith match + token routing | Gateway route | P2 |
| FM-7 | Accept/by-token public không rate-limit (OWASP A04) | Gateway + controller | P2 |
| FM-8 | FE token round-trip (no encodeURIComponent) | FE endpoints + accept page | P2 |
| FM-9 | Idempotency re-invite cùng email → 500 thay 409 | BE create + unique index | P2 |

---

## Top-3 to verify/fix first

1. **FM-1 (P0) — Tenant scoping của STAFF user.** Đây là blocker RBAC nghiêm trọng nhất: `User` entity không có cột tenant/instance, và `accept()` không link STAFF vào trung tâm. Walk PHẢI verify: sau accept, STAFF login có thấy đúng dữ liệu trung tâm mời không (hoặc bị 403 toàn bộ). Nếu hole xác nhận → flow KC-2 KHÔNG thể DONE (cross-tenant isolation core). Pre-walk check: `grep tenant_id User.java` → 0 hit = hole.

2. **FM-3 (P0) — Owner invite 500 do `invited_by NOT NULL` + gateway header.** Đây là bước ĐẦU TIÊN của flow (Owner bấm Gửi lời mời). Nếu gateway không forward `X-User-Id` đúng format → INSERT fail 500 ngay. Walk PHẢI verify gateway thực sự set `X-User-Id` cho `POST /staff-invitations` TRƯỚC khi walk. Đây là điểm dễ chết nhất ở bước 1.

3. **FM-2 (P0) — Role-literal mismatch.** Verify role seed thực tế của Owner test fixture (`TENANT_OWNER`? `OWNER`?) khớp `OWNER_AUTHZ = hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')`. Nếu Owner seed là `TENANT_OWNER` → 403 ngay bước invite. Và STAFF seed `"STAFF"` vs guard `TENANT_STAFF` → STAFF login không vào được endpoint nào. Đây là GAP-893 manifest cụ thể; chuẩn hóa 1 role vocabulary trước khi walk.

---

## Quan hệ với known blockers + prior 17-bug walk

**GAP-886 (RBAC user_id/teacher_id BIGINT/Long vs UUID):**
- Trong `kitehub-subscription` impl này, mọi ID đã UUID-native: `User.id` UUID (User.java:24), `StaffInvitation.invitedBy`/`tenantId`/`acceptedUserId` đều UUID (StaffInvitation.java:47-73), `UserRepository extends JpaRepository<User, UUID>`. **GAP-886 KHÔNG manifest ở module này** — đây là điểm mạnh của bản rewrite. GAP-886 vẫn áp `kiteclass-core` (module prior-walk). → Walk KC-2 trên `kitehub-subscription` không bị GAP-886 chặn, nhưng FM-1 (thiếu tenant scope trên User) là vấn đề kế cận chưa được GAP-886 cover.

**GAP-893 (users.role no CHECK constraint + ADMIN vs PLATFORM_ADMIN drift):**
- Xác nhận trực tiếp: `User.role` là `@Column String role` KHÔNG CHECK constraint (User.java:37-38). **FM-2 là instance cụ thể của GAP-893** — drift mở rộng thành 3 vocabulary (`STAFF`/`OWNER`/`ADMIN` ở controller vs `TENANT_STAFF`/`TENANT_OWNER`/`PLATFORM_ADMIN` ở SecurityConfig). GAP-893 nên được mở rộng scope để cover staff-invitation role literal, hoặc FM-2 file gap con link GAP-893.

**GAP-784 (FE InviteStaffPage role affordance vs BE 2-role MVP):**
- **GAP-784 ĐÃ được fix trong bản FE hiện tại.** `invite/page.tsx` line 145-157 hiển thị role read-only "Nhân viên trung tâm (STAFF)" + KHÔNG gửi role param (line 55-58 chỉ gửi `{email, fullName}`). Comment line 48-54 ghi rõ "GAP-784 confirmed BE does NOT accept a role param, so we do NOT send one". → FE đã đúng 2-role MVP; KHÔNG còn role dropdown gây hiểu nhầm. GAP-784 nên flip DONE nếu chưa.

**Prior 17-bug walk (Wave meta-6 Bucket A, 2026-05-28 — `kiteclass-core`):**
- **Khác module hoàn toàn.** Bản review hôm nay là `kitehub-subscription` (UUID-native rewrite). Đối chiếu các P0 cũ:
  - Prior Bug #14 (email không gửi) → **FIXED** ở đây (FM-5 chỉ là failure-mode visibility, không phải missing path).
  - Prior Bug #17 (accept không tạo user) → **FIXED** ở đây (accept tạo User thật).
  - Prior Bug #8 (`@PreAuthorize` không fire do `anyRequest().permitAll()`) → **MOSTLY FIXED**: prod chain `kitehub-subscription` dùng `anyRequest().authenticated()` default-deny (SecurityConfig javadoc line 52-53) + `@EnableMethodSecurity` (line 61). NHƯNG `test` profile chain vẫn `anyRequest().permitAll()` (line 76) → CẢNH BÁO: test không verify được `@PreAuthorize` thật → liên quan trust-pass anti-pattern (audit PASS nhưng prod-guard chưa walk). FM-2 nhấn vào việc literal mismatch khiến guard không match kể cả khi fire.
  - Prior Bug #16 (gateway reject public-but-tenant-scoped) → **FIXED**: gateway có route `staff-invitations-public-token` allowlist by-token + /accept (application.yml line 636-639, JwtAuthenticationGatewayFilter line 241-242). FM-6 chỉ là edge-case verify, không phải missing.
  - Prior Bug #13 (UserContext Long null → 401) → **MUTATED thành FM-3**: ở đây null `invited_by` → DB constraint 500 thay vì 401, nhưng cùng root cause (gateway header forwarding chưa chắc chắn).

**Kết luận đối chiếu:** Bản `kitehub-subscription` đã đóng phần lớn P0 của prior-walk, nhưng phát sinh 3 P0 mới (tenant scoping, role literal, invited_by null) mà walk KC-2 cần verify TRƯỚC. 5/9 FM (FM-1/2/3/4/5) có khả năng surface trong happy-path walk; chạy 3 pre-walk check ở Top-3 sẽ catch 3 P0 trước khi tốn 1 vòng walk.

## Pre-walk static audit bundle (per pre-walk-static-audit-bundle.md)

Đề xuất chạy trước khi walk KC-2:
- `bash scripts/check-be-rollback-side-effects.sh` — bắt FM-4 (resend rotation invisible).
- `bash scripts/check-gateway-shared-breaker.sh` — liên quan FM-6 (gateway route).
- `bash scripts/check-fe-bare-catch.sh` — kiểm tra invite/accept page catch blocks (invite page line 61-86 switch theo error code — KHÔNG bare catch, OK; accept page line 123-140 tương tự OK).
- `bash scripts/check-stale-images.sh` — đảm bảo image `kitehub-subscription` + `kitehub-gateway` + `kitehub-frontend` không stale trước walk.
