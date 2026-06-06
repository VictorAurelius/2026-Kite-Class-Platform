# Pre-Walk Persona Simulation — KH-8 Off-boarding + Data Retention (PDPL) + Consent

**Ngày:** 2026-06-06
**Flow:** KH-8 (Consent v1/v2 + DSAR + Off-boarding/Retention/Purge)
**Service:** kitehub-subscription qua gateway `:9000`
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md`
**Loại:** Prediction-only (KHÔNG fix) — predict failure modes TRƯỚC khi walk local Docker stack.

---

## TL;DR — 2 cờ bắt buộc trả lời trước khi walk

### (i) Consent/DSAR POST có PUBLIC-whitelist tại gateway không?

**KHÔNG.** Gateway `JwtAuthenticationGatewayFilter.isPublicPath()` (dòng 76-84) whitelist CHỈ: `/api/auth/`, `/api/v1/auth/`, `/api/v1/staff-invitations/by-token/`, `*/accept`, `/api/platform/webhooks/`, `/actuator/health`, `/docs/`, `/fallback/`.
→ **KHÔNG có `/api/v1/consent/**` và `/api/v1/dsar/**`.**

**Hệ quả:** visitor không có JWT gọi `POST /api/v1/consent/record` hoặc `POST /api/v1/dsar/request` QUA GATEWAY → **401 walk-blocker**, dù controller + SecurityConfig của subscription cố ý permitAll. Đây đúng class sự cố Wave meta-6 Bug #16 (gateway public-path miss) mà chính file filter này ghi chú.

### (ii) IDOR posture 3 sub-flow

| Sub-flow | Posture | Verdict |
|---|---|---|
| **Consent v1** (`/api/v1/consent/{visitorId}` GET + `/revoke`) | KHÔNG `@PreAuthorize`, permitAll — bất kỳ ai biết `visitorId` đều GET/revoke được | ⚠️ MEDIUM (giảm nhẹ nhờ UUIDv4 không đoán được) |
| **Consent v2** (`/api/v1/consent/v2/**`) | `@consentAuthz.canAccessUser` so principal (X-User-Id verbatim từ gateway) vs `userId` body/path + platform-admin override | ✅ SECURE — đóng đúng class KH-5/6/7 |
| **DSAR** (`/api/v1/dsar/{ticketId}` GET) | Public-by-design nhưng response đã redact (không email/nationalId/resolution); ticketId là UUID | ✅ LOW (rò rỉ PII tối thiểu) |

---

## Failure modes (xếp hạng confidence × impact)

### 1. [HIGH — WALK-BLOCKER] Gateway không whitelist consent v1 + DSAR public POST → 401
- **(a) Where:** `kitehub-gateway/.../filter/JwtAuthenticationGatewayFilter.java:76-84` (`isPublicPath`)
- **(b) Symptom:** No-JWT visitor `POST :9000/api/v1/consent/record` hoặc `POST :9000/api/v1/dsar/request` → **HTTP 401** tại gateway, request không bao giờ đến subscription. Walk "visitor ghi consent trước khi login" và "submit DSAR ẩn danh" đều fail.
- **(c) Pre-walk check:**
  ```bash
  grep -nE "consent|dsar" kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java
  # Expect: 0 match trong isPublicPath() → confirm blocker
  curl -i -X POST http://localhost:9000/api/v1/consent/record -H 'Content-Type: application/json' -d '{"visitorId":"...","granted":{...}}'
  # Expect (bug): 401. Workaround walk: gọi thẳng subscription :8080 bypass gateway.
  ```

### 2. [HIGH — A01 Broken Access Control] InstanceController không có @PreAuthorize → bất kỳ user nào purge/delete/list-all instances
- **(a) Where:** `kitehub-subscription/.../controller/InstanceController.java` (toàn bộ method: `listInstances`, `getInstanceById`, `deleteInstance`, `extendTrial`, `purgeInstance:241`) — KHÔNG method nào có `@PreAuthorize`. Gateway route `platform-instances` (application.yml:154-162) chỉ có CircuitBreaker, KHÔNG role predicate. SecurityConfig rơi vào `anyRequest().authenticated()` (dòng 158).
- **(b) Symptom:** JWT của 1 TENANT_USER bất kỳ → `GET :9000/api/platform/instances` liệt kê TẤT CẢ instance (cross-tenant enumeration), `DELETE .../{id}/purge` xoá vĩnh viễn instance của tenant khác, `DELETE .../{id}` soft-delete. Javadoc ghi "admin only" nhưng không enforce. Cùng class IDOR/broken-access KH-5/6/7.
- **(c) Pre-walk check:**
  ```bash
  grep -nE "PreAuthorize|hasRole|hasAuthority" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/InstanceController.java
  # Expect: 0 match → confirm hole
  grep -n "platform-instances" -A6 kitehub/kitehub-gateway/src/main/resources/application.yml  # no role predicate
  # Walk: login as TENANT_USER → curl GET /api/platform/instances → expect 403 (đúng) nhưng sẽ thấy 200 + full list (bug)
  ```

### 3. [HIGH] DSAR mâu thuẫn design vs config: "public" nhưng SecurityConfig = authenticated()
- **(a) Where:** `kitehub-subscription/.../config/SecurityConfig.java:156` `.requestMatchers("/api/v1/dsar/**").authenticated()` — TRÁI với controller javadoc + V26 migration comment ("both endpoints unauthenticated by design").
- **(b) Symptom:** Kể cả nếu gateway whitelist (bug #1), DSAR vẫn 401 tại subscription vì yêu cầu JWT. DSAR submitter (ex-user / never-signed-up) không thể submit. Double-blocked.
- **(c) Pre-walk check:**
  ```bash
  grep -n "dsar" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java
  # Expect: authenticated() (bug) thay vì permitAll — đối chiếu V26 comment "both endpoints public"
  ```

### 4. [MEDIUM] Consent v1 IDOR — bất kỳ ai biết visitorId đều GET/revoke consent người khác
- **(a) Where:** `consent/controller/ConsentController.java:79,89` — không `@PreAuthorize`; SecurityConfig:122-125 permitAll GET `/api/v1/consent/*` + POST `/api/v1/consent/*/revoke`.
- **(b) Symptom:** Biết/đoán/leak `visitorId` (LocalStorage `kite_visitor_id`, có thể lộ qua URL/log/shared device) → GET trạng thái consent (rò privacy posture) hoặc revoke (phá consent người khác). UUIDv4 122-bit nên brute-force không khả thi; rủi ro là leak-then-abuse.
- **(c) Pre-walk check:**
  ```bash
  # Walk: tạo consent với visitorId A → từ session khác (no auth) curl POST /api/v1/consent/{A}/revoke → expect 200 (xác nhận ai cũng revoke được)
  ```
  Đánh giá: chấp nhận được cho cookie-consent ẩn danh, nhưng ghi nhận để quyết định rate-limit/binding.

### 5. [MEDIUM] Purge instance chưa DELETED trả 200 + status=FAILED thay vì 4xx
- **(a) Where:** `service/InstancePurgeService.java:63-68` (`purgeInstance` guard) + `adminPurge:93`. Khi `status != DELETED` → trả `PurgeResult{status=FAILED, errorMessage}` với HTTP 200.
- **(b) Symptom:** `DELETE .../{id}/purge` trên instance ACTIVE → HTTP **200 OK** body `{status:FAILED}`. Walker mong đợi 409 Conflict. Semantic mismatch dễ gây hiểu nhầm "purge thành công". Cần check purge instance không tồn tại (`findById` empty → exception hay FAILED?).
- **(c) Pre-walk check:**
  ```bash
  sed -n '55,110p' kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstancePurgeService.java
  # Walk: purge instance ACTIVE → quan sát HTTP code (200 vs 409) + purge UUID random → 200/FAILED vs 404
  ```

### 6. [MEDIUM] Cảnh báo retention dùng so khớp ngày CHÍNH XÁC → cron downtime = bỏ sót cảnh báo im lặng
- **(a) Where:** `service/DataRetentionService.java:187-198` (`shouldSendWarning`: `daysSuspended == firstWarningDay || == secondWarningDay`) + dòng 136-137 (`daysUntilExpiry == 1`).
- **(b) Symptom:** Trial retention=7 → cảnh báo chỉ bắn ở đúng day 3 và day 5. Nếu cron (daily) lỡ 1 ngày (downtime/stop-stack) → user KHÔNG bao giờ nhận cảnh báo, rồi instance bị xoá đột ngột (vi phạm tinh thần PDPL "thông báo trước"). Final warning `== 1` cũng vậy.
- **(c) Pre-walk check:**
  ```bash
  # Đọc logic; walk khó simulate (cần >3 ngày). Verify bằng unit reasoning + check cron schedule.
  grep -rn "Scheduled\|cron" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/**/*Cron*.java
  ```

### 7. [MEDIUM] Consent v2 không phục vụ ẩn danh — userId @NotNull, canAccessUser(null)=deny
- **(a) Where:** `ImmutableConsentController.java:137` (`@NotNull Long userId`) + `ConsentAuthorizationBean.java:65-69` (null → deny). DB cho phép user_id NULL (visitor) nhưng DTO bắt buộc.
- **(b) Symptom:** Walk thử v2 cho visitor chưa login → 400 (validation) hoặc 403 (authz). By design (v2 = post-login) nhưng dễ gây nhầm khi walk; phải dùng v1 cho pre-login.
- **(c) Pre-walk check:** Walk v2 với JWT hợp lệ + `body.userId == X-User-Id`; KHÔNG thử v2 không login.

### 8. [LOW-MED] INET binding consent_record_immutable.ip_address — phải chạy Postgres+Flyway thật (không ddl-auto)
- **(a) Where:** `V56__create_consent_record_immutable.sql` (`ip_address INET NOT NULL`); entity map String. Đã có `ConsentRecordImmutablePostgresIT.java` (Testcontainers).
- **(b) Symptom:** Nếu local stack dùng ddl-auto/H2-mode thì INET bị che (giống RCA 2026-05-16 LoginAuditLog.ip). Trên Postgres+Flyway thật, binding String→INET cần cast đúng; nếu sai → 500 khi `POST /consent/v2/record`. Lưu ý memory `kiteclass-core IT ddl-auto masks migration drift`.
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "\d consent_record_immutable" | grep ip_address  # expect inet
  curl -i POST .../consent/v2/record -d '{"userId":1,"granted":{"analytics":true},"ipAddress":"203.0.113.7"}'  # expect 201 không 500
  ```

### 9. [LOW] X-User-Id forgery — đã mitigated nhưng phụ thuộc thứ tự filter
- **(a) Where:** gateway `application.yml:771-776` `default-filters: RemoveRequestHeader=X-User-Id` (+ Tenant/Email/Reference). JWT filter inject SAU strip (javadoc:38-56 cảnh báo ordering fragility).
- **(b) Symptom:** Nếu RemoveRequestHeader bị gỡ HOẶC JWT filter chạy TRƯỚC strip → client forge `X-User-Id: <victim>` → bypass toàn bộ defense consent v2 (IDOR). Hiện tại config đúng.
- **(c) Pre-walk check:**
  ```bash
  curl -i POST :9000/api/v1/consent/v2/record -H 'Authorization: Bearer <userA-jwt>' -H 'X-User-Id: 999' -d '{"userId":999,...}'
  # Expect 403 (forged header bị strip, principal=userA != 999) — nếu 201 thì forgery thủng
  ```

### 10. [LOW — POSITIVE] Consent v2 immutability + withdraw = INSERT mới (A09 tamper-proof) ✅
- **(a) Where:** `ConsentService.withdrawConsent:118-128` → `recordConsent` → INSERT; `V56` RLS policy `no_update`/`no_delete` (USING false).
- **(b) Symptom kỳ vọng:** withdraw tạo ROW MỚI (analytics+marketing=false), KHÔNG sửa row cũ; hash chain nối tiếp. PDPL Art 11 đạt.
- **(c) Pre-walk check:**
  ```bash
  # record → withdraw → GET /consent/v2/{userId} → expect ≥2 rows, row mới granted.analytics=false, chainValid=true
  docker exec kite-postgres psql -U kitehub -d kitehub -c "UPDATE consent_record_immutable SET current_hash='x' WHERE id=1"  # expect RLS reject (0 rows / error)
  ```

### 11. [LOW] DSAR GET redaction đúng; submit POST không ràng buộc ownership (by design)
- **(a) Where:** `dsar/dto/DsarResponse.java` chỉ phơi ticketId/rightType/status/sla/timestamps — ẩn email/nationalId/resolution. `DsarController.submitDsar` không bind người gửi (xác minh out-of-band).
- **(b) Symptom:** GET ticket người khác (đoán UUID) chỉ thấy metadata — rò rỉ tối thiểu. Submit thay danh người khác có thể (spam) — chấp nhận theo design, có honeypot field.
- **(c) Pre-walk check:** GET ticket UUID hợp lệ → xác nhận body KHÔNG có email/nationalId.

### 12. [LOW] Purge cascade — kiểm tra orphan subscriptions/payments/branding
- **(a) Where:** `InstancePurgeService.adminPurge` (Operation javadoc InstanceController:233 "Removes database, backups, email logs, publishes cross-service cleanup event").
- **(b) Symptom:** Nếu purge không cascade hoặc cross-service event không phát → orphan rows (subscriptions/payments/branding) sau khi instance bị purge. Verify event thực sự publish (outbox).
- **(c) Pre-walk check:**
  ```bash
  sed -n '90,200p' kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstancePurgeService.java
  # Walk: purge DELETED instance → check subscriptions/payments rows cho instance đó còn không + outbox/rabbit event
  ```

---

## Tổng kết cho walker

| # | Mức | Bản chất | Hành động walk |
|---|---|---|---|
| 1 | HIGH | Gateway chặn consent v1 + DSAR public (401) | Bypass gateway gọi thẳng :8080 HOẶC fix whitelist trước |
| 2 | HIGH | InstanceController thiếu @PreAuthorize (A01 cross-tenant purge/list) | Walk với TENANT_USER JWT → xác nhận list-all/purge thủng |
| 3 | HIGH | DSAR SecurityConfig=authenticated() trái design public | Xác nhận 401 tại subscription |
| 4 | MED | Consent v1 IDOR revoke-by-visitorId | Xác nhận revoke không cần auth |
| 5 | MED | Purge non-DELETED → 200/FAILED thay vì 409 | Quan sát HTTP code |
| 6 | MED | Retention warning exact-day fragility | Đọc logic (khó walk thời gian) |
| 7 | MED | Consent v2 không phục vụ ẩn danh | Dùng v1 cho pre-login |
| 8 | LOW-MED | INET binding phải Postgres+Flyway thật | psql `\d` + 201 round-trip |
| 9 | LOW | X-User-Id forgery (đã mitigated) | curl forge header → expect 403 |
| 10 | LOW✅ | v2 immutability/withdraw đúng | record→withdraw→2 rows |
| 11 | LOW | DSAR redaction đúng | GET → no PII |
| 12 | LOW | Purge cascade orphan | check rows + event |

**Walk-blocker chính:** #1 (gateway whitelist) + #3 (DSAR authenticated) — public sub-flow KHÔNG đi qua gateway được. **Lỗ nghiêm trọng nhất:** #2 (InstanceController broken access — purge/list-all cross-tenant bởi bất kỳ user đăng nhập).
