# Pre-Walk Persona Simulation — KH-6 AI Branding Wizard (generate → apply → approval)

**Date:** 2026-06-06
**Flow:** KH-6 — Owner dùng AI Branding wizard để generate brand assets → preview → apply → (approval)
**Service:** `kitehub-branding` (qua gateway `:9000`, JWT HS512 inject `X-User-Id` + `X-User-Roles`)
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md`
**Mode:** Prediction-only (KHÔNG fix). Source of truth = code đã đọc.

---

## Tóm tắt điều hành (ranked)

12 failure modes dự đoán, xếp theo confidence × walk-blocker impact. Điểm mấu chốt khác kỳ vọng ban đầu:

- **AI provider KHÔNG phải walk-blocker** (xem FM-10): default `OPENAI_API_KEY=sk-mock-key-for-local-testing` → `OpenAIClient.isMockMode()=true` → `generate-image`/`generate-text`/`analyze-logo` trả mock data (placehold.co URL + VN sample copy) KHÔNG gọi API thật. Generate step chạy được local.
- **Walk-blocker thật sự là tầng header/auth/tenant-scope** quanh gateway ↔ branding, KHÔNG phải AI.

---

## Failure modes (ranked)

### FM-1 — 🔴 P0 (high conf, WALK-BLOCKER + IDOR): `X-Instance-Id` không được gateway inject, nhưng `createJob` bắt buộc + là tenant-scope key client-controlled

- **(a) Where:** `controller/BrandingJobController.java:74-87` (`createJob`, `@RequestHeader("X-Instance-Id")` required=true, KHÔNG default); gateway `application.yml:771-776` `default-filters` chỉ strip `X-Tenant-Id` / `X-User-Id` / `X-User-Reference-Id`; `JwtAuthenticationGatewayFilter` chỉ inject `X-User-Id` / `X-User-Roles` / `X-User-Email`. KHÔNG có ai inject hay strip `X-Instance-Id`.
- **(b) Symptom:** (i) Nếu FE không tự gắn `X-Instance-Id` → `POST /api/platform/branding/jobs` trả **HTTP 400** (missing required header). (ii) Vì `X-Instance-Id` do client cung cấp (không bind từ JWT), Owner A set `X-Instance-Id=<instance của Owner B>` → tạo/list/read jobs của tenant khác. `@PreAuthorize(hasAnyRole('OWNER'))` chỉ check ROLE chứ không check ownership → **IDOR** (giống KH-5 FM-1).
- **(c) Pre-walk check:**
  ```bash
  grep -n "X-Instance-Id" kitehub/kitehub-gateway/src/main/resources/application.yml   # expect: 0 inject/strip
  grep -rn "X-Instance-Id" kitehub/kitehub-frontend/src --include=*.ts --include=*.tsx # FE có tự gắn không?
  # walk: curl -X POST gw:9000/api/platform/branding/jobs (JWT only, no X-Instance-Id) → 400?
  ```

### FM-2 — 🔴 P0 (high conf, WALK-BLOCKER): `X-User-Roles` literal mismatch → 401/403 trên mọi branding endpoint

- **(a) Where:** `config/SecurityConfig.java` — default-deny `.authenticated()` cho `/api/platform/branding/**` + `/api/v1/branding/**`; `XUserRolesHeaderFilter` build `ROLE_<role>` từ `X-User-Roles` (prefix `ROLE_` nếu chưa có). `@PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")` trên write endpoints.
- **(b) Symptom:** Nếu gateway gửi `X-User-Roles` rỗng/thiếu → filter không set auth → **401**. Nếu role literal trong JWT khác `OWNER` (vd `TENANT_OWNER`, hoặc đã `ROLE_` sẵn → double-prefix `ROLE_ROLE_OWNER`) → `hasAnyRole('OWNER')` fail → **403** dù đăng nhập đúng Owner. Đây là lớp role-literal-mismatch (class KC-5/KH-5).
- **(c) Pre-walk check:**
  ```bash
  # JWT của Owner đẻ ra X-User-Roles gì? decode token, hoặc:
  grep -rn "X-User-Roles\|setRoles\|roles" kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java
  # walk: login Owner → curl gw:9000/api/v1/branding/regenerate-quota → 200 hay 401/403?
  ```

### FM-3 — 🔴 P0 (med-high conf): RLS bật trên `branding_jobs` nhưng branding service KHÔNG set GUC `app.current_tenant_id`

- **(a) Where:** `V34__enable_rls_tenant_scoped_tables.sql` — `branding_jobs` nằm trong `instance_id_tables`, policy `tenant_isolation USING (instance_id = current_setting('app.current_tenant_id'))`. Grep branding service: **KHÔNG có** code nào set `app.current_tenant_id` / `set_config` / TenantContext (0 hit).
- **(b) Symptom:** Policy là `ENABLE` (non-FORCE) → table owner/superuser BYPASS RLS. Nếu DB role `kitehub` SỞ HỮU bảng (local dev điển hình) → RLS bị bypass → list/insert chạy (nhưng IDOR app-layer FM-1 vẫn còn). Nếu role bị hạn chế (prod) hoặc không own bảng → mọi SELECT filter `instance_id = NULL` → **list rỗng**; INSERT `WITH CHECK` fail → **500 / silent empty**.
- **(c) Pre-walk check:**
  ```bash
  # local: kitehub có own branding_jobs / bypass RLS không?
  PGPASSWORD=kitehub_dev_password psql -h localhost -p 5433 -U kitehub -d kitehub -c \
    "SELECT relrowsecurity, relforcerowsecurity, relowner::regrole FROM pg_class WHERE relname='branding_jobs';"
  PGPASSWORD=kitehub_dev_password psql -h localhost -p 5433 -U kitehub -d kitehub -c \
    "SELECT rolname, rolbypassrls FROM pg_roles WHERE rolname='kitehub';"
  ```

### FM-4 — 🟠 P1 (high conf, WALK-BLOCKER cho preview/deploy step): SSE `preview` + `deploy-stream` auth — EventSource không gửi được Authorization/X-User header → gateway 401

- **(a) Where:** `wizard/preview/PreviewController.java:55` (`GET /{jobId}/preview`, text/html) + `wizard/controller/DeployStreamController.java` (`GET /{jobId}/deploy-stream`, SSE). Cả hai dưới `/api/v1/branding/jobs/**` → `.authenticated()` (FM-2). KHÔNG có cơ chế đặc biệt cho token qua query param.
- **(b) Symptom:** Browser `EventSource`/iframe không set custom header → gateway JWT filter từ chối → **401**, preview iframe trắng / deploy-stream không connect → wizard "preview before commit" và "live deploy progress" không hiển thị.
- **(c) Pre-walk check:**
  ```bash
  grep -rn "token\|access_token\|query\|param" kitehub/kitehub-frontend/src --include=*.tsx | grep -i "preview\|deploy-stream\|EventSource"
  # walk: mở deploy-stream qua DevTools Network → status 401?
  ```

### FM-5 — 🟠 P1 (high conf): Job đứng `QUEUED` mãi nếu outbox relay/dispatcher KHÔNG chạy

- **(a) Where:** `service/BrandingJobService.createJob` — chỉ `save(QUEUED)` + `outboxEmitter.emit("branding.job.queued", ...)`. Việc xử lý thật ở `queue/BrandingJobConsumer.processJob` (`@RabbitListener(BRANDING_QUEUE)` → PROCESSING → `brandingProcessor.processJob` → COMPLETED). Cầu nối = outbox relay phải publish row → `BRANDING_QUEUE`.
- **(b) Symptom:** Nếu outbox dispatcher worker không chạy / RabbitMQ không up → message không bao giờ tới `BRANDING_QUEUE` → job kẹt **QUEUED**, `progress=0`, `GET /{id}/assets` trả null, deploy-stream poller (2s) không bao giờ thấy COMPLETED. (Lưu ý: `AIJobConsumer` cho tier-queue `ai.request.*` hiện "logs only — real AI work next wave" — đường này CHƯA xử lý; chỉ `BrandingJobConsumer` xử lý thật.)
- **(c) Pre-walk check:**
  ```bash
  grep -rn "OutboxDispatcher\|@Scheduled\|outbox" kitehub/kitehub-branding/src/main/java/com/kitehub/branding/outbox
  # RabbitMQ admin: queue BRANDING_QUEUE có message tồn → consumer pick? (http://localhost:15672)
  ```

### FM-6 — 🟠 P1 (high conf): `X-Subscription-Tier` client-controlled → bypass rate-limit + regenerate-quota

- **(a) Where:** `AIBrandingController` (`checkRateLimit`/`recordUsage` theo `X-Subscription-Tier`), `BrandingWizardController.getQuota/regenerate` (quota theo `X-Subscription-Tier`). Header này KHÔNG được gateway inject từ JWT và KHÔNG nằm trong default-filters strip.
- **(b) Symptom:** Client gửi `X-Subscription-Tier: ENTERPRISE` → unlimited regenerate + né AI rate-limit (FREE 3/PRO 10/...). Cost/abuse bypass.
- **(c) Pre-walk check:**
  ```bash
  grep -n "X-Subscription-Tier" kitehub/kitehub-gateway/src/main/resources/application.yml   # expect 0 inject/strip
  # walk: curl ... -H "X-Subscription-Tier: ENTERPRISE" /api/v1/branding/regenerate-quota → unlimited?
  ```

### FM-7 — 🟠 P1 (med conf): Thiếu endpoint approval/apply cho branding JOB → bước "apply to instance" dead-end

- **(a) Where:** Grep `approve|apply|publish|activate` toàn branding → chỉ `TemplateGalleryController.java:81 POST /{id}/apply` (apply TEMPLATE, không phải job/theme). `generate-theme` trả `ThemeConfig` JSON nhưng KHÔNG có endpoint nào persist nó thành active theme của instance. Không có job-approval endpoint.
- **(b) Symptom:** Walk tới bước "apply generated theme" / "approve" → không tìm thấy nút/endpoint trong branding service. Apply thật có thể nằm ở `kiteclass-core` branding package → cần verify, nếu không thì flow KH-6 dead-end ở bước apply/approval.
- **(c) Pre-walk check:**
  ```bash
  grep -rn "apply\|activate\|active.*theme\|setTheme" kiteclass/kiteclass-core/src/main/java --include=*.java | grep -i brand
  ```

### FM-8 — 🟠 P1 (med conf): Boot ordering — branding `ddl-auto=validate` chống lại schema do kitehub-subscription Flyway tạo

- **(a) Where:** `application.yml:18-19` `ddl-auto: validate`; branding KHÔNG có migration riêng (0 file SQL). Bảng `branding_jobs`/`branding_templates`/... do `kitehub-subscription` Flyway (V4/V8/...) tạo trên CÙNG DB `kitehub`.
- **(b) Symptom:** Nếu branding boot TRƯỚC khi subscription chạy Flyway → `validate` thấy thiếu bảng → branding **fail startup** → toàn bộ KH-6 không khả dụng. Cross-service schema coupling ẩn.
- **(c) Pre-walk check:**
  ```bash
  PGPASSWORD=kitehub_dev_password psql -h localhost -p 5433 -U kitehub -d kitehub -c "\dt branding_*"
  docker ps --format '{{.Names}} {{.Status}}' | grep -E "branding|subscription"   # cả hai healthy?
  ```

### FM-9 — 🟡 P2 (high conf): `generate-image` trả URL ngoài (placehold.co) → preview ảnh vỡ nếu Docker local không có egress

- **(a) Where:** `client/OpenAIClient.java:~100` mock path → `Mono.just("https://placehold.co/1792x1024/...")`.
- **(b) Symptom:** Preview iframe/img load placehold.co; nếu container/host không ra internet → ảnh **broken** (cosmetic, không chặn flow nhưng làm walk hiểu nhầm "generate fail").
- **(c) Pre-walk check:** `curl -sI https://placehold.co/100x100 | head -1` (có 200 không?).

### FM-10 — 🟡 P2 (med conf) — ⚑ AI-PROVIDER AVAILABILITY (đã giảm cấp): default mock OK, NHƯNG env override sẽ vỡ

- **(a) Where:** `application.yml:88` `ai.provider: ${AI_PROVIDER:openai}`, `:144` `openai.api.key: ${OPENAI_API_KEY:sk-mock-key-for-local-testing}`. `OllamaClient` gọi WebClient tới `ollama` host.
- **(b) Symptom:** Mặc định = mock → generate chạy. NHƯNG nếu `.env`/compose set `AI_PROVIDER=ollama` (Ollama không up) → generate-* **timeout/500** (Resilience4j circuit `ai-provider` mở → fallback). Nếu set `OPENAI_API_KEY` thật mà không có credit → 401/timeout từ OpenAI. **Đây là "AI provider gate" prompt cảnh báo — thực tế đã được mock-default che, nhưng phải xác nhận env compose.**
- **(c) Pre-walk check:**
  ```bash
  grep -rn "AI_PROVIDER\|OPENAI_API_KEY\|OLLAMA" kitehub/docker-compose.kitehub.yml .env 2>/dev/null
  docker exec kitehub-branding env | grep -E "AI_PROVIDER|OPENAI_API_KEY"   # mock hay real?
  ```

### FM-11 — 🟡 P2 (med conf): `regenerate` bắt buộc `Idempotency-Key` header → 400 nếu FE quên gắn

- **(a) Where:** `BrandingWizardController.java` `regenerate` — thiếu `Idempotency-Key` → 400 `MISSING_IDEMPOTENCY_KEY`; thiếu `X-Instance-Id` → 400 `MISSING_INSTANCE_ID`.
- **(b) Symptom:** Bấm "Regenerate" trên UI mà FE không tự sinh Idempotency-Key → **400**, user thấy lỗi mơ hồ.
- **(c) Pre-walk check:** `grep -rn "Idempotency-Key" kitehub/kitehub-frontend/src` (FE có gắn không?).

### FM-12 — 🟡 P2 (low-med conf): `BrandingJob` entity (plain `@Data`, không BaseEntity/@Version) vs migration có `deleted NOT NULL` + V59 optimistic-lock

- **(a) Where:** `domain/entity/BrandingJob.java` không có field `deleted`/`version`/`created_by`; `V4` có `deleted BOOLEAN NOT NULL DEFAULT FALSE`; `V59__optimistic_lock_check_coverage.sql` có thể thêm `version`. `application.yml` comment "BaseEntity uses @Version" — nhưng BrandingJob KHÔNG extend BaseEntity.
- **(b) Symptom:** Insert dựa vào DB DEFAULT cho `deleted` (OK). Nếu có cột NOT NULL không default mà entity không map (vd `version` NOT NULL no default) → INSERT **500**. `ddl-auto=validate` không bắt extra columns nên drift này im lặng tới runtime.
- **(c) Pre-walk check:**
  ```bash
  PGPASSWORD=kitehub_dev_password psql -h localhost -p 5433 -U kitehub -d kitehub -c \
    "SELECT column_name, is_nullable, column_default FROM information_schema.columns WHERE table_name='branding_jobs' ORDER BY ordinal_position;"
  ```

---

## Khuyến nghị thứ tự pre-walk

Chạy trước khi walk: FM-2 (auth role) → FM-1 (X-Instance-Id) → FM-3 (RLS/GUC) → FM-8 (schema/boot) → FM-10 (AI env). Nếu 5 cái này xanh thì generate step chạy; FM-4/FM-5 quyết định preview + deploy hiển thị; FM-6/FM-11/FM-12 là edge trong lúc walk.
