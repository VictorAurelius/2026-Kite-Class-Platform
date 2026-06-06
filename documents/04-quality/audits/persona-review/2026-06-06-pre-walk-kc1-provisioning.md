---
audience: dev
flow: KC-1 tenant-provisioning saga (beta signup → KiteClass tenant ready)
date: 2026-06-06
type: pre-walk-persona-simulation
wave: provisioning-1
---

# Pre-walk persona simulation — KC-1 tenant-provisioning saga

**Per `.claude/rules/pre-walk-persona-simulation-mandate.md` §3.** Read-only static trace của 8-bucket saga vừa merge (Wave provisioning-1). Mục tiêu: surface failure modes TRƯỚC khi coordinator/user walk local Docker stack, để batch-fix high-confidence findings trước → walk chỉ bắt residual.

## Personas đã walk (mental simulation)

1. **Invitee/Owner** — redeem beta invite → `registerFromBetaInvite` → kỳ vọng KC tenant ready → login → cấu hình TenantSettings (Năm học/timezone).
2. **PLATFORM_ADMIN** — thấy 1 tenant FAILED/stuck → bấm "Retry provisioning"; sau đó DELETE tenant (PDPL cascade).

## ⚠️ Tiền đề bắt buộc — STALE IMAGE

Container `kiteclass-core` + `kitehub-subscription` đang chạy là build **~6h trước khi 8 bucket merge** → toàn bộ saga keystone (consumer + publisher + audit + retry) KHÔNG có trong image đang chạy. Walk hiện tại = test code CŨ (orphan saga), KHÔNG phải code đã merge.

```bash
bash kitehub/scripts/rebuild.sh kitehub-subscription
bash kitehub/scripts/rebuild.sh kiteclass-core   # hoặc tên service tương ứng trong docker-compose.kitehub.yml
# verify: docker ps --format '{{.Names}}\t{{.CreatedAt}}' | grep -E 'subscription|kiteclass-core'
```
Không rebuild = walk vô nghĩa (kiteclass-core trước Wave này không có `@RabbitListener` → tenant.created rơi vào void).

---

## Failure modes (12)

### 1. [HIGH] Subscription `Instance.status` kẹt PENDING vĩnh viễn — không có DEPLOYED callback (GAP-945 follow-up)
- **(a) Where:** `kitehub-subscription InstanceService.java:217` (`createTrialInstance` → `setStatus(InstanceStatus.PENDING)`); KHÔNG có consumer nào ở subscription flip status. Chỉ có 2 `@RabbitListener`: `TenantDeployedEventConsumer` (chỉ gửi email) + `EmailConsumer`. `tenant.deployed` consumer KHÔNG update `Instance.status`.
- **(b) Symptom:** Sau khi saga kiteclass-core provision xong + `FrontendInstance` → DEPLOYED, subscription `Instance` vẫn PENDING. Owner login thấy tenant "đang chờ"; admin tenant list hiển thị PENDING dù KC đã sẵn sàng. Beta-invite còn bỏ qua `verifyEmailAndActivate` (line 238 check `status != PENDING`) nên không có đường nào flip PENDING → ACTIVE.
- **(c) Pre-walk check:**
  ```bash
  grep -n "setStatus(InstanceStatus" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstanceService.java
  grep -rn "@RabbitListener" kitehub/kitehub-subscription/src/main/java --include=*.java | grep -v /test/
  # psql sau signup: SELECT subdomain, status FROM instances ORDER BY created_at DESC LIMIT 1;  → kỳ vọng (sai) PENDING
  ```

### 2. [HIGH] Audit TENANT_PROVISIONED bị drop âm thầm — FK fail trên owner chưa commit (GAP-949 — đúng như risk flag)
- **(a) Where:** `AuthService.java:299` gọi `recordTenantProvisionedAudit` BÊN TRONG parent `@Transactional registerFromBetaInvite`. `TenantAuditService.recordTenantProvisioned` (`:82`) là `REQUIRES_NEW` → suspend parent txn, mở txn MỚI. `adminUserId(ownerId)` với FK `admin_user_id → users(id)` NOT NULL. Owner `userRepository.save(user)` (line 279) chưa commit (parent txn còn mở) → dưới READ COMMITTED txn audit KHÔNG thấy user row → FK violation → catch nuốt (`:101-105`).
- **(b) Symptom:** Happy-path signup chạy "thành công" nhưng bảng `admin_audit_log` KHÔNG có row TENANT_PROVISIONED. Log warn `recordTenantProvisioned failed`. PDPL Art 11 / OWASP A09 trail rỗng.
- **(c) Pre-walk check:**
  ```bash
  grep -n "REQUIRES_NEW\|adminUserId(ownerId)\|admin_user_id" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/audit/TenantAuditService.java
  # sau signup: SELECT count(*) FROM admin_audit_log WHERE action='TENANT_PROVISIONED';  → kỳ vọng (sai) 0
  # docker logs kitehub-subscription | grep "recordTenantProvisioned failed"
  ```
  Fix hướng: chuyển audit sang `TransactionSynchronization` AFTER_COMMIT (như plan §7.1 follow-up ghi), HOẶC bỏ FK NOT NULL trên admin_user_id cho self-service rows.

### 3. [HIGH] Admin force-retry KHÔNG bao giờ retry — slug collision (GAP-953 — tệ hơn risk flag)
- **(a) Where:** `AdminTenantProvisioningService.java:72` re-publish `tenant.created` (tenantId=subscription Instance UUID, slug=subdomain). kiteclass `TenantCreatedEventConsumer` → `saga.provision` → `InstanceLifecycleService.initiate` (`:44-47`) gọi `existsBySlugAndDeletedFalse(slug)`. `FrontendInstance` cũ vẫn tồn tại (FAILED, `deleted=false`) → returns TRUE → `throw IllegalArgumentException("Slug already in use")`. Saga `provision` (`:77-81`) rethrow KHÔNG compensate; consumer (`:74-79`) catch + ACK.
- **(b) Symptom:** Admin bấm "Retry provisioning" → API trả 200 (re-publish OK) nhưng kiteclass log ERROR `Slug already in use` rồi nuốt. `FrontendInstance` vẫn FAILED. `InstanceLifecycleService.retry()` (FAILED→INITIALIZING, retryCount++) là DEAD CODE từ admin path — KHÔNG ai gọi. Javadoc `AdminTenantProvisioningService:21` claim "(→ InstanceLifecycleService.retry() path)" SAI.
- **(c) Pre-walk check:**
  ```bash
  grep -n "existsBySlugAndDeletedFalse\|throw new IllegalArgumentException" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/instance/service/InstanceLifecycleService.java
  grep -rn "lifecycle.retry(\|\.retry(instanceId" kiteclass/kiteclass-core/src/main/java --include=*.java | grep -v /test/   # ai gọi retry()?
  ```
  Fix hướng: admin retry phải gọi đường `lifecycle.retry(failedInstanceId)` trực tiếp (hoặc consumer detect existing FAILED instance theo tenantSlug → retry thay vì initiate).

### 4. [HIGH] Walk chạy image cũ nếu không rebuild (vận hành)
- **(a) Where:** docker containers `kitehub-subscription` + `kiteclass-core` (~6h, pre-merge).
- **(b) Symptom:** tenant.created publish nhưng kiteclass-core image cũ không có consumer → tenant không bao giờ provision → walk kết luận sai "saga đứt".
- **(c) Pre-walk check:** xem khối "Tiền đề bắt buộc — STALE IMAGE" ở trên (`bash scripts/check-stale-images.sh` nếu có).

### 5. [MEDIUM] tenant.created publish 2 lần (fast-path + outbox dispatcher) → duplicate consume → ERROR noise mỗi signup
- **(a) Where:** `SubscriptionEventEmitter.emit` (`:86` save outbox row) + (`:99-111` fast-path `rabbitTemplate.send`). `SubscriptionOutboxDispatcher:101` `@Scheduled` poll → re-send (`:143`). Fast-path KHÔNG mark row published → dispatcher gửi lại cùng event.
- **(b) Symptom:** Mỗi beta signup → tenant.created delivered ≥2 lần. Lần 1 provision OK; lần 2 → saga.initiate slug collision (#3) → ERROR `Slug already in use` (swallowed). Provisioning idempotent-by-accident nhưng log đầy ERROR gây nhiễu triage.
- **(c) Pre-walk check:**
  ```bash
  grep -n "setPublishedAt\|markPublished\|findBy.*[Pp]ublished\|published" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/SubscriptionOutboxDispatcher.java
  # docker logs kiteclass-core | grep "Slug already in use" | wc -l   → kỳ vọng ≥1 mỗi signup
  ```

### 6. [MEDIUM] `provisionInfrastructure` chỉ là stub — tenant DEPLOYED nhưng không có infra thật (GAP-946 follow-up)
- **(a) Where:** `TenantProvisioningSaga.java:105-108` — chỉ `log.info(...)`, không tạo DB schema / MinIO bucket / DNS record cho tenant.
- **(b) Symptom:** Saga đạt DEPLOYED + gửi email "tenant ready", nhưng Owner click vào tenant → không có instance KiteClass thực chạy (no schema/bucket/subdomain). Persona "vào trường của tôi" fail ở tầng app.
- **(c) Pre-walk check:** `grep -n "provisionInfrastructure" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/provisioning/TenantProvisioningSaga.java` — xác nhận vẫn stub. Là PARTIAL đã biết, nhưng persona-visible → cần set kỳ vọng walk.

### 7. [MEDIUM] tenant.deployed publish vô điều kiện — email "ready" có thể gửi sớm
- **(a) Where:** `TenantCreatedEventConsumer.java:73` gọi `tenantReadyNotifier.notifyDeployed(...)` ngay sau `saga.provision` trả về (không throw). DEPLOYED transition thực sự do `PublishPackageStep.java:40` (`markBrandingCompleted`) — bước cuối của branding plan. Nếu plan hoàn thành mà KHÔNG chạy PublishPackageStep, `provision()` vẫn return bình thường → email gửi trong khi `FrontendInstance` còn GENERATING.
- **(b) Symptom:** Email "tenant của bạn đã sẵn sàng" tới hộp thư trong khi instance chưa DEPLOYED.
- **(c) Pre-walk check:**
  ```bash
  grep -rn "PublishPackageStep\|markBrandingCompleted" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/ --include=*.java
  # sau signup: SELECT status FROM frontend_instances ORDER BY id DESC LIMIT 1;  → kỳ vọng DEPLOYED (nếu GENERATING = bug)
  ```

### 8. [MEDIUM] Audit retry FK fail nếu PLATFORM_ADMIN không ở bảng `users` của subscription
- **(a) Where:** `TenantAuditService.recordTenantRetryRequested` (`:154`) `adminUserId(adminUserId)` FK `admin_user_id → users(id)`. `adminUserId` lấy từ gateway `X-User-Id`. Nếu PLATFORM_ADMIN sống ở identity store khác (kitehub-platform admin ≠ subscription `users`) → FK violation → catch nuốt (`:171-175`).
- **(b) Symptom:** Admin retry → audit row TENANT_PROVISIONING_RETRY_TRIGGERED không ghi (silent). Cùng lớp lỗi #2 nhưng cho admin principal.
- **(c) Pre-walk check:** `psql: SELECT id FROM users WHERE id = '<X-User-Id của admin>';` — nếu rỗng → FK sẽ fail.

### 9. [MEDIUM] AI provider mặc định `gemini` (không phải mock) cho walk
- **(a) Where:** `kiteclass-core application.yml:201` `primary: ${AI_PROVIDER_PRIMARY:gemini}`. `GeminiAIClient` (`@ConditionalOnProperty ... havingValue=gemini`) hiện là Phase-1 scaffold trả mock-shaped result (`GeminiAIClient.java:22,76`), và `ResilientAIClient` có CircuitBreaker+fallback, `MockAIClient` là default-no-profile. Nên KHÔNG hard-block, nhưng provider routing không deterministic giữa các môi trường.
- **(b) Symptom:** Branding plan chạy nhưng đường provider khác kỳ vọng; nếu sau này GeminiAIClient gọi API thật → cần key → fallback template.
- **(c) Pre-walk check:** set `AI_PROVIDER_PRIMARY=mock` cho kiteclass-core trước walk để branding plan deterministic. `grep -n "AI_PROVIDER_PRIMARY" kitehub/docker-compose.kitehub.yml` (hiện chưa set → mặc định gemini).

### 10. [LOW] tenant.created KHÔNG có DLQ — poison payload drop câm
- **(a) Where:** `TenantCreatedEventConsumer.java:54-58,74-79` swallow + return cho cả malformed JSON LẪN saga-fail (no requeue, no DLQ). Chỉ `tenant.deployed` có DLQ (`EmailQueueConfig:197-208`). `RabbitConfig.tenantCreatedQueue()` không khai báo `x-dead-letter`.
- **(b) Symptom:** Một tenant.created hỏng (hoặc saga fail) biến mất không dấu vết, không alert → tenant không bao giờ provision mà không ai biết.
- **(c) Pre-walk check:** `grep -n "x-dead-letter\|tenantCreatedQueue" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/config/RabbitConfig.java`.

### 11. [LOW] Email tenant-ready — template/nội dung có thể placeholder
- **(a) Where:** `TenantDeployedEventConsumer.java:90` → `EmailServiceClient.sendTenantReadyEmail`. Plan §7.1 ghi follow-up "Resend template + MailHog live walk".
- **(b) Symptom:** Email tới MailHog nhưng subject/body có thể là placeholder VN chưa hoàn thiện.
- **(c) Pre-walk check:** `curl -s http://localhost:8025/api/v2/messages | jq '.items[0].Content.Headers.Subject'` sau signup (MailHog). Verify recipient = `instance.contactEmail`.

### 12. [LOW] `ProvisioningStuckSweep` @Scheduled có thể race với instance vừa INITIALIZING
- **(a) Where:** `kiteclass-core .../provisioning/ProvisioningStuckSweep.java` (@Scheduled). Nếu ngưỡng stuck quá ngắn so với thời gian branding plan chạy, sweep có thể mark FAILED một instance đang provision bình thường.
- **(b) Symptom:** Instance mới signup bị sweep flip FAILED giữa chừng → walk thấy FAILED bất thường.
- **(c) Pre-walk check:** `grep -n "fixedDelay\|cron\|stuck\|threshold\|Duration\|minutes" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/provisioning/ProvisioningStuckSweep.java` — xác nhận ngưỡng >> thời gian provision.

---

## Recommended pre-walk batch fix (sort confidence × impact)

**HIGH — fix TRƯỚC walk:**
- **#4 Rebuild 2 image** (subscription + kiteclass-core) — bắt buộc tuyệt đối, nếu không cả walk vô nghĩa.
- **#2 GAP-949 audit FK** — chuyển recordTenantProvisioned sang AFTER_COMMIT hook HOẶC nới FK. Nếu không fix, ít nhất xác nhận trước để walk không hiểu nhầm "audit hoạt động".
- **#3 GAP-953 retry** — feature force-retry hiện broken (slug collision). Fix: admin retry gọi `lifecycle.retry()` đường FAILED→INITIALIZING thay vì re-publish tenant.created → initiate. Nếu chưa fix, persona PLATFORM_ADMIN "Retry" sẽ luôn fail.
- **#1 GAP-945 status callback** — cân nhắc thêm consumer subscription nghe `instance.deployed` (kiteclass đã emit qua outbox `:88`) để flip Instance PENDING→ACTIVE. Nếu chưa fix, set kỳ vọng walk: status PENDING là đã biết.

**MEDIUM — spot-check (Read+grep+psql) trong walk:**
- #5 đếm log "Slug already in use" mỗi signup; #6 xác nhận stub infra (set kỳ vọng); #7 kiểm `frontend_instances.status`=DEPLOYED; #8 kiểm admin tồn tại trong `users`; #9 set `AI_PROVIDER_PRIMARY=mock`.

**LOW — để walk bắt:**
- #10 DLQ tenant.created; #11 email template; #12 sweep race.

---

## Đối chiếu với per-gap risk flags (input)

| Risk flag (input) | Kết quả trace |
|---|---|
| GAP-945 callback flip Instance→DEPLOYED | ✅ XÁC NHẬN — không có consumer nào flip; Instance kẹt PENDING (createTrialInstance:217 + 2 listener chỉ email) |
| GAP-949 FK fail dưới READ COMMITTED | ✅ XÁC NHẬN ĐÚNG CHÍNH XÁC — REQUIRES_NEW + owner chưa commit + FK NOT NULL → audit drop câm |
| GAP-953 re-publish idempotent hay duplicate/throw | ⚠️ TỆ HƠN FLAG — không chỉ throw: đường `lifecycle.retry()` UNREACHABLE; admin retry luôn fail slug-collision; javadoc claim "(→ retry() path)" SAI |
| GAP-952 stuck-sweep chạy local | ✅ chạy local; thêm nuance #12 race với instance mới INITIALIZING |
