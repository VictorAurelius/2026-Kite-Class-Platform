# Pre-Walk Persona Simulation — KH-10 Notification / Email / Feedback / Support

**Flow:** KH-10 — tenant user gửi feedback (anonymous + authenticated) → quản lý notification preferences → PLATFORM_ADMIN xem/điều khiển email (history/stats/config/trigger) → SupportMenu (help / Zalo / email / feedback / beta-status). Email side-effect verify qua MailHog.
**Date:** 2026-06-06
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md` (prediction-only, KHÔNG fix).
**Stack:** gateway `:9000` (JWT HS512 → strip client headers + inject `X-User-Id` + `X-User-Roles` + `X-Tenant-Id`) → kitehub-subscription (`/api/v1/feedback`, `/api/v1/notification-preferences`, `/api/platform/admin/emails`) + kitehub-email (`/api/platform/emails/send`). MailHog cho email side-effect.

---

## Câu trả lời 2 câu hỏi headline

### (i) Gateway role bridge cho các path KH-10 — CÓ, KHÔNG phải KC-7 dead-deny ✅

kitehub-subscription `SecurityConfig.java:169` register `XUserRolesHeaderFilter` (inner static class L192-215) `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`. Filter split `X-User-Roles` trên dấu phẩy + prefix `ROLE_` → `@EnableMethodSecurity` (L61) làm `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` trên `AdminEmailController:49` resolve đúng. Chuỗi role giống KH-9 (PASS). Đây KHÔNG phải KC-7 GAP-1003 (24 endpoint dead-deny vì thiếu bridge) — subscription CÓ bridge.

```
gateway JwtAuthenticationGatewayFilter → inject X-User-Roles: PLATFORM_ADMIN
  → XUserRolesHeaderFilter split(",") + "ROLE_" prefix → ROLE_PLATFORM_ADMIN
    → @PreAuthorize("hasRole('PLATFORM_ADMIN')") ✅ MATCH
```

**Caveat (FM-7):** `AdminEmailController` CHỈ chấp nhận `PLATFORM_ADMIN`, KHÔNG có alias `ADMIN`. Seed phải đúng (KH-9 FM-4 recurrence).

### (ii) Feedback anonymous-safe — CÓ, defense vững ✅

`/api/v1/feedback` + `/api/v1/feedback/**` explicit `permitAll()` (`SecurityConfig:128-129`). Controller (`FeedbackController:68-74`) trả `userId=null` cho anonymousUser. Defense 2 lớp: (a) DTO bean-validation `FeedbackSubmissionRequest` — `@NotNull @Min(1) @Max(5)` rating + `@Size(5,2000)` comment + `@Pattern` category whitelist + `@Size(max=0)` honeypot bot-trap; (b) `FeedbackService:59-77` re-validate category whitelist + comment length + rating runtime (GAP-555 config-driven). Anonymous POST → 201 + DB row với `tenant_id`/`user_id` null. Vững.

---

## Ranked failure modes (confidence × impact)

### FM-1 🔴 `POST /api/platform/emails/send` (kitehub-email) KHÔNG có auth — bất kỳ caller nào gửi email tùy ý (spam / spoof)
- **(a) Where:** `kitehub-email/.../controller/EmailController.java` — KHÔNG có `@PreAuthorize`. kitehub-email KHÔNG có `SecurityConfig`/`SecurityFilterChain`/`spring-boot-starter-security` (grep 0 hit trong toàn module). Javadoc L87 tự nhận "should only be called by other KiteHub services... In production, use service-to-service authentication" — NHƯNG không enforce gì. Gateway route `platform-email` (`application.yml:295-300`) chỉ có CircuitBreaker filter, không role gate.
- **(b) Symptom:** Nếu gateway forward `/api/platform/emails/**` cho authenticated user bất kỳ (hoặc tệ hơn — anonymous nếu path không trong gateway auth-required list) → caller POST `/api/platform/emails/send` với `to` + `htmlBody` tùy ý → email thật gửi tới MailHog/SES. Abuse: spam, phishing từ domain KiteHub, email-bomb. A01 Broken Access Control + A09.
- **(c) Pre-walk check:**
  ```bash
  # 1) Verify gateway có require JWT cho path này không (tìm public allowlist)
  grep -rnE "platform/emails|public.*path|permitAll|secured" kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/ | grep -iE "email|public" | head
  # 2) Thử gửi với token tenant-thường (KHÔNG admin):
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:9000/api/platform/emails/send \
    -H "Authorization: Bearer $TENANT_USER_JWT" -H "Content-Type: application/json" \
    -d '{"to":"victim@test.vn","subject":"x","htmlBody":"<b>spam</b>"}'
  # Expect HIGH-risk nếu 200; an toàn nếu 401/403
  ```

### FM-2 🟠 Email stats `failedToday` vô nghĩa + bảng `email_logs`(V5) orphan vs `email_sent_log`(V11)
- **(a) Where:** `EmailAdminService.getEmailStats()` tính `failedToday = countByEmailTypeContainingAndSentAtBetween(...)` — bảng `email_sent_log` (V11) CHỈ có cột `id/instance_id/email_type/recipient/sent_at`, KHÔNG có cột `status`/`error`/`failed`. `EmailSentLog` entity (cross-module `com.kitehub.platform.domain.entity`) khớp V11 (no drift → không 500). Nhưng "failures" được derive bằng `email_type LIKE '%<substring>%'`, không phải failure thật → log này chỉ ghi SEND THÀNH CÔNG (idempotency log) → `failedToday` luôn 0 hoặc đếm sai theo tên type. Ngoài ra tồn tại 2 bảng email: `V5__create_email_logs_table` (email_logs) + `V11__create_email_sent_log` (email_sent_log) — service chỉ đọc email_sent_log; email_logs có thể orphan.
- **(b) Symptom:** Admin email dashboard `/stats` render `failedToday` luôn ≈ 0 (misleading — admin tưởng 0 lỗi). `/history` + `/stats` KHÔNG 500 (schema khớp), nhưng dữ liệu sai nghĩa. Walker đừng nhầm "0 failures" là bug routing.
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "\d email_sent_log" -c "\dt email*"
  grep -nE "countByEmailTypeContaining|failedToday|status" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/repository/EmailSentLogRepository.java
  ```

### FM-3 🟠 `notification-preferences` 401 nếu gateway thiếu `X-User-Roles` (filter cần CẢ HAI header)
- **(a) Where:** `/api/v1/notification-preferences/**` KHÔNG trong public allowlist → rơi vào `.anyRequest().authenticated()` (`SecurityConfig:158`). Auth chỉ được set bởi `XUserRolesHeaderFilter:200` khi **CẢ** `X-User-Id` AND `X-User-Roles` non-blank. Controller (`NotificationPreferenceController:50`) lại đọc trực tiếp `@RequestHeader("X-User-Id") UUID userId`.
- **(b) Symptom:** Nếu gateway inject `X-User-Id` nhưng `X-User-Roles` rỗng (user role rỗng / token thiếu claim role) → filter KHÔNG set auth → 401 dù user đã đăng nhập. Nếu `X-User-Id` thiếu hẳn → `MissingRequestHeader` → 400. Nếu `X-User-Id` không phải UUID hợp lệ → `MethodArgumentTypeMismatchException` → handler L77 trả 400 với `errorCode: INVALID_NOTIFICATION_TYPE` (SAI nghĩa — đây là user-id lỗi không phải type lỗi).
- **(c) Pre-walk check:**
  ```bash
  # Decode JWT của seed user → verify claim "role" non-empty
  # Walk: login → GET /api/v1/notification-preferences (DevTools verify X-User-Roles forwarded)
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:9000/api/v1/notification-preferences \
    -H "Authorization: Bearer $USER_JWT"  # Expect 200
  ```

### FM-4 🟠 Notification-pref / feedback đọc header `X-User-Id`/`X-Tenant-Id` trực tiếp — IDOR nếu bypass gateway
- **(a) Where:** `NotificationPreferenceController:50,59` dùng `@RequestHeader("X-User-Id")` làm khóa scope (service `list(userId)`/`update(userId,...)`) — KHÔNG đọc từ `SecurityContext`. `FeedbackController:56` đọc `X-Tenant-Id` header trực tiếp. Gateway CÓ strip client header (`JwtAuthenticationGatewayFilter:38` `RemoveRequestHeader=X-User-Id` + `TenantHeaderGuardFilter` re-inject từ JWT verified) → spoof bị chặn TẠI gateway. NHƯNG service KHÔNG có defense-in-depth: nếu request tới subscription trực tiếp (port expose / internal network / SSRF) → client tự set `X-User-Id` → đọc/ghi preference của user khác (cross-tenant IDOR — recurrence GAP-1015/1019/1023).
- **(b) Symptom:** Trong walk qua gateway: an toàn (gateway strip). Risk class: service tin tưởng header tuyệt đối, không cross-check `X-User-Id` header vs `SecurityContext.authentication.name` (mà filter cũng set = userId). Lệch nhau không được phát hiện.
- **(c) Pre-walk check:**
  ```bash
  # Verify gateway strip client-supplied X-User-Id (curl trực tiếp service, không qua gateway)
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/api/v1/notification-preferences \
    -H "X-User-Id: 00000000-0000-0000-0000-000000000099" -H "X-User-Roles: TENANT_USER"
  # Nếu subscription port KHÔNG expose ra ngoài → IDOR chỉ exploit nếu attacker vào internal net
  grep -nE "SecurityContextHolder|getAuthentication|X-User-Id" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/notification/controller/NotificationPreferenceController.java
  ```

### FM-7 🟠 Seed admin role phải `PLATFORM_ADMIN` (alias `ADMIN` → 403 toàn bộ admin email console)
- **(a) Where:** `AdminEmailController:49` `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` literal-only. Không chấp nhận `ADMIN`.
- **(b) Symptom:** Nếu seed user role legacy `ADMIN` HOẶC walker login sai admin → login OK nhưng MỌI `/api/platform/admin/emails/**` trả 403 (silent walk-blocker — login thành công gây nhầm).
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT email, role FROM users WHERE role IN ('PLATFORM_ADMIN','ADMIN');"
  ```
  Verify đúng 1 row `PLATFORM_ADMIN` + biết email + password.

### FM-5 🟡 GET notification-preferences khi chưa có row → synthesize defaults (KHÔNG 500) ✅
- **(a) Where:** `NotificationPreferenceService.list():45-67` — "synthesizes defaults for types with no row" (BR-NOTIF-005/006 default-on EMAIL). Không cần seed DB.
- **(b) Symptom:** Lần GET đầu (chưa upsert) → trả full table với default EMAIL on. KHÔNG 500. Walker verify list đầy đủ enum types.
- **(c) Pre-walk check:** Walk: login user mới → GET notification-preferences → expect full list, default channels = [EMAIL].

### FM-6 🟡 Tắt mandatory notification type → 400 (không 500) ✅
- **(a) Where:** `NotificationPreferenceService.update():91-92` — `type.isMandatory() && !contains(EMAIL)` → throw `MandatoryTypeCannotBeDisabledException` → controller `handleMandatory():68` → 400 `MANDATORY_TYPE_...`.
- **(b) Symptom:** PATCH mandatory type bỏ EMAIL → 400 (đúng), không 500. Verify message tiếng Việt "Loại thông báo bắt buộc không thể tắt."
- **(c) Pre-walk check:** Walk: PATCH `/{mandatoryType}` với enabledChannels rỗng → expect 400. PATCH type không hợp lệ (vd `/FOO`) → 400 `INVALID_NOTIFICATION_TYPE`.

### FM-8 🟡 AdminEmail `PUT /config` chỉ đổi in-memory + nhận Map toggle không whitelist
- **(a) Where:** `AdminEmailController.updateConfig():117-124` javadoc "Updates in-memory only; to persist, update application.yml". Nhận `Map<String,Boolean> toggles` raw — không validate key thuộc tập email type hợp lệ.
- **(b) Symptom:** Admin tắt 1 email type → có hiệu lực tới khi service restart → revert (gây nhầm "config không lưu"). Map key tùy ý được accept (không crash nhưng no-op cho key lạ). Documented behavior, không phải crash.
- **(c) Pre-walk check:** Walk: PUT config toggle 1 type → GET config verify đổi → (nếu restart được) verify revert. Note đây là known limitation.

### FM-9 🟡 AdminEmail `POST /trigger` — type không hợp lệ → cần 400 (verify không 500); side-effect thật tới MailHog
- **(a) Where:** `EmailAdminService.triggerEmail():115-140` switch 13 case hardcoded + `default → IllegalArgumentException("Unknown email type")`. Instance không tồn tại → `EntityNotFoundException`. AdminEmailController KHÔNG có local `@ExceptionHandler` → phụ thuộc global handler subscription map IllegalArgument→400 + EntityNotFound→404. `/trigger` gọi `emailServiceClient.send*` → tới kitehub-email → MailHog thật.
- **(b) Symptom:** Trigger `welcome` cho instance hợp lệ → 200 + email tới MailHog. Trigger type lạ → expect 400 (verify global handler map đúng, không leak 500). Trigger instance không tồn tại → 404. Idempotency check (`IllegalStateException` L110) nếu đã gửi hôm nay → verify map 409 hay 400.
- **(c) Pre-walk check:**
  ```bash
  grep -rnE "IllegalArgumentException|EntityNotFoundException|IllegalStateException|@ExceptionHandler|@RestControllerAdvice" \
    kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/**/Global*Handler*.java 2>/dev/null | head
  # Walk: trigger welcome → check MailHog http://localhost:8025/api/v2/messages
  ```

### FM-10 🟡 Feedback honeypot + double-validation → expect 400 cho input xấu (không 500) ✅
- **(a) Where:** DTO `FeedbackSubmissionRequest` — `@Size(max=0) honeypot` (bot-trap) + `@Pattern` category whitelist + `@Min/@Max` rating + `@Size(5,2000)` comment. Service `FeedbackService:59-77` re-validate category/length/rating runtime (config-driven, IllegalArgument).
- **(b) Symptom:** POST honeypot non-empty → 400 (bean validation). POST category `INVALID` → 400. POST rating 6 / comment 2 ký tự → 400. POST hợp lệ anonymous → 201 + DB row `tenant_id`/`user_id` null. Verify không 500.
- **(c) Pre-walk check:** Walk: submit feedback hợp lệ → 201 → `psql -c "SELECT rating,category,tenant_id,user_id,status FROM feedback_submissions ORDER BY created_at DESC LIMIT 3"` (expect status RECEIVED). Submit honeypot non-empty → 400.

### FM-11 🟢 Feedback rate-limit (10 req/min/IP) enforced TẠI gateway, không ở service
- **(a) Where:** `application.yml:460-463` comment + api-contract.md §Rate limits. Service không có rate-limit riêng.
- **(b) Symptom:** Nếu gateway rate-limit (RequestRateLimiter / Redis) không config trong local stack → unlimited feedback submit (spam DB). Informational — walker không cần block.
- **(c) Pre-walk check:** `grep -nE "RequestRateLimiter|redis-rate-limiter|replenishRate" kitehub/kitehub-gateway/src/main/resources/application.yml | head` quanh route `kitehub-feedback-v1`.

### FM-12 🟢 SupportMenu pure FE (no BE endpoint) — Zalo OA placeholder + route check
- **(a) Where:** `components/support/SupportMenu.tsx` — 5 item: help route (Link persona-aware), `mailto:support@kitehub.me`, Zalo OA (`ZALO_OA_ID = process.env.NEXT_PUBLIC_KITEHUB_ZALO_OA_ID ?? 'kitehub'` — L51 placeholder), feedback dialog (`FeedbackForm` Radix), `/beta-status`. Không gọi BE endpoint nào trực tiếp (feedback đi qua FeedbackForm → `/api/v1/feedback`).
- **(b) Symptom:** Nếu `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` chưa set → click Zalo mở `https://zalo.me/kitehub` (placeholder sai, không phải OA thật). `/beta-status` route tồn tại (`app/(public)/beta-status/page.tsx` verified ✅). Feedback dialog mount khi `!onFeedbackClick`.
- **(c) Pre-walk check:** Walk: mở SupportMenu (nút `?` floating bottom-right) → verify 5 item render → click "Gửi phản hồi" → FeedbackForm dialog mở → submit → 201. Click "Trạng thái beta" → `/beta-status` render. Note Zalo placeholder nếu env chưa set.

---

## Tóm tắt cho walker

| # | Severity | 1-dòng | Loại |
|---|---|---|---|
| FM-1 | 🔴 | `/api/platform/emails/send` (kitehub-email) KHÔNG auth — spam/spoof risk | Pre-walk curl + gateway grep |
| FM-2 | 🟠 | Email `failedToday` vô nghĩa (no status col) + email_logs V5 orphan | Semantic (không 500) |
| FM-3 | 🟠 | notification-pref 401 nếu thiếu X-User-Roles; X-User-Id sai → 400 sai nghĩa | Auth/binding |
| FM-4 | 🟠 | Header X-User-Id/X-Tenant-Id đọc trực tiếp — IDOR nếu bypass gateway | Trust-boundary |
| FM-7 | 🟠 | Seed phải `PLATFORM_ADMIN` (alias ADMIN→403 admin email) | Walk-blocker candidate |
| FM-5 | 🟡 | GET notification-pref synthesize defaults (no 500) | Validation ✅ |
| FM-6 | 🟡 | Tắt mandatory type → 400 (không 500) | State machine ✅ |
| FM-8 | 🟡 | AdminEmail PUT /config in-memory only + Map không whitelist | Known limitation |
| FM-9 | 🟡 | /trigger type lạ → verify 400 không 500; side-effect MailHog thật | Validation + side-effect |
| FM-10 | 🟡 | Feedback honeypot + double-validation → 400 input xấu | Validation ✅ |
| FM-11 | 🟢 | Feedback rate-limit ở gateway, không service | Info |
| FM-12 | 🟢 | SupportMenu pure FE; Zalo OA placeholder; /beta-status OK | Cosmetic |

**Pre-walk MUST-run trước khi mở flow:**
1. **FM-7** seed role psql — `SELECT email, role FROM users WHERE role IN ('PLATFORM_ADMIN','ADMIN')` (quyết định admin email console walk được không).
2. **FM-1** email-send auth — curl POST `/api/platform/emails/send` với token tenant-thường + grep gateway public-path cho `/api/platform/emails` (quyết định severity thực của lỗ hổng gửi email).
3. **FM-2** email schema — `psql \dt email*` + `\d email_sent_log` (xác nhận `/history`+`/stats` không 500 + hiểu `failedToday` luôn 0).

3 cái này quyết định walk có chạy được + đâu là risk thật vs cosmetic.
