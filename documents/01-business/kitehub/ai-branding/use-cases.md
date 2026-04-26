# AI Branding — Use Cases

### UC-AIB-01: Phân tích Logo (AI)
- **Actor:** Owner (đã xác thực)
- **Precondition:** Instance có tier, không vượt daily limit
- **Steps:**
  1. FE: AI Branding page, upload logo URL + org name
  2. System: check rate limit theo tier (AIB-01 đến AIB-04)
  3. System: nếu vượt limit → 429 Too Many Requests
  4. System: gọi AI provider phân tích logo → trả về brand identity
  5. System: record usage (AIB-07)
- **Postcondition:** Logo analysis result returned
- **Errors:**
  - 429: rate limit exceeded → "Daily AI request limit exceeded. Limit: 3/day (FREE tier)"
- **FE Behavior:** Hiển thị remaining requests (e.g., "2 requests remaining today")

### UC-AIB-02: Generate Hero Image (AI)
- **Actor:** Owner
- **Precondition:** Không vượt daily limit
- **Steps:**
  1. FE: nhập org name, theme, colors
  2. System: rate limit check
  3. System: generate hero banner via DALL-E / Ollama vision
  4. System: record usage
  5. System: trả về imageUrl
- **Errors:**
  - 429: rate limit exceeded

### UC-AIB-03: Generate Marketing Text (AI)
- **Actor:** Owner
- **Precondition:** Không vượt daily limit
- **Steps:**
  1. FE: nhập org name, theme, target audience
  2. System: rate limit check
  3. System: generate marketing copy via GPT-4 / Ollama text
  4. System: record usage
  5. System: trả về text
- **Errors:**
  - 429: rate limit exceeded

### UC-AIB-04: Generate Full Theme (AI)
- **Actor:** Owner
- **Precondition:** Đã có LogoAnalysis result
- **Steps:**
  1. FE: submit LogoAnalysis object
  2. System: rate limit check
  3. System: generate ThemeConfig (colors, typography, spacing, layout)
  4. System: record usage
  5. System: trả về ThemeConfig JSON
- **Postcondition:** Complete theme ready for KiteClass frontend

### UC-AIB-05: Browse Template Gallery (Không cần AI)
- **Actor:** Owner (bất kỳ tier)
- **Steps:**
  1. FE: GET /api/platform/branding/templates?category=education
  2. System: trả về active templates filtered by category
  3. User: chọn template
  4. FE: GET /api/platform/branding/templates/{id}

### UC-AIB-06: Apply Template (Instant, Không cần AI)
- **Actor:** Owner
- **Steps:**
  1. FE: POST /api/platform/branding/templates/{id}/apply với X-Instance-Id header
  2. System: find template, return themeConfig JSON
  3. FE: apply theme config ngay lập tức (< 1s)
- **Postcondition:** Instance theme updated
- **Errors:**
  - 404: template not found

---

## v2 Use Cases (Waves 2-4 — kiteclass-core implementation)

### UC-AIB-07: Tenant Provisioning Saga (event-driven)
- **Actor:** System (consumer of `TenantCreatedEvent` from KiteHub onboarding)
- **Precondition:** Wizard UI đã thu thập `tenantId`, `slug`, `audience`, `tone`; tenant đã trả tiền hoặc đang TRIAL
- **Steps:**
  1. KiteHub publish `TenantCreatedEvent { tenantId, slug, audience, tone }` qua RabbitMQ
  2. `TenantProvisioningSaga.provision(event)` consume event
  3. `lifecycle.initiate(tenantId, slug)` → tạo `FrontendInstance` (NOT_STARTED → INITIALIZING); outbox event `instance.initializing`
  4. `provisionInfrastructure()` (placeholder hiện tại — DB schema, MinIO bucket, DNS sẽ delegate sang Infrastructure service)
  5. `lifecycle.markInfrastructureReady(id)` → INITIALIZING → GENERATING; outbox `instance.generating`
  6. `runBrandingPlan()`: `AnalyzerService.analyze(audience, tone)` → `PlannerService.plan()` → `PlanExecutor.execute(plan, ctx)` chạy 4 Steps (`ExtractPaletteStep`, `PickTemplateStep`, `QualityReviewStep`, `PublishPackageStep`)
  7. Last Step trigger `lifecycle.markBrandingCompleted(id, frontendUrl)` → DEPLOYED; outbox `instance.deployed`
- **Postcondition:** Instance ở DEPLOYED; FE có thể fetch package qua UC-AIB-11
- **Errors:**
  - `StepException` từ pipeline → `compensate(reason)` gọi `lifecycle.markFailed` (BR-LIFE-005) → FAILED state
  - Mọi RuntimeException khác cũng compensate; failed transition KHÔNG rollback các transitions trước (saga là multi-txn cố ý)
- **FE Behavior:** Tenant onboarding screen poll `GET /api/v1/instances/{id}` để track status; hiển thị spinner + tiến độ theo `status`

### UC-AIB-08: Quality Gate Review
- **Actor:** System (`QualityReviewStep` trong pipeline UC-AIB-07)
- **Precondition:** Instance đang ở GENERATING hoặc REGENERATING; package đã build xong (theme + assets)
- **Steps:**
  1. `InstanceQualityReviewer.review(instanceId)` lấy `FrontendInstance`
  2. Chạy 5 `QualityCheck` song song: `wcag-contrast`, `css-vars-applied`, `asset-urls-reachable`, `visual-regression`, `logo-placement`
  3. Tính average score + flag `passed = score ≥ pass-threshold` (BR-QUALITY-001 — default 70)
  4. Build `QualityReport { score, passed, issues, perCheckScores }` + persist
  5. Audit log: `quality.review.passed` hoặc `quality.review.failed` với `{instanceId, score, reason}`
- **Postcondition:**
  - Pass → caller (`PublishPackageStep`) tiếp tục → DEPLOYED
  - Fail → saga catch + `markFailed("score X < threshold 70")` → FAILED
- **Errors:** Instance không tồn tại → `IllegalArgumentException`
- **FE Behavior:** Score thấp + reason hiển thị trong instance detail (ops dashboard); end-user không thấy chi tiết
- **Lưu ý (per ai-branding-guidelines.md §11.4):** Hiện tại scaffold mode — real WCAG/visual-regression/ML scoring tracked GAP-226/227/228, ship Wave 8+

### UC-AIB-09: Rebrand Request (DEPLOYED → REGENERATING → DEPLOYED)
- **Actor:** Owner (instance đang DEPLOYED)
- **Precondition:** Tenant tier KHÔNG phải Enterprise (Enterprise đi qua UC-AIB-10), instance status = DEPLOYED
- **Steps:**
  1. FE: POST `/api/v1/instances/{id}/rebrand`
  2. `lifecycle.rebrand(id)` validate transition DEPLOYED → REGENERATING; tăng `lastRegenerateAt`
  3. Outbox event `instance.regenerating` → BrandingPackage cache evict (BR-PKG-003)
  4. Pipeline Steps chạy lại (giống UC-AIB-07 từ step 6); kết thúc bằng `markBrandingCompleted` (REGENERATING → DEPLOYED, `brandingVersion++`)
- **Postcondition:** `brandingVersion` tăng → ETag thay đổi → FE re-fetch package
- **Errors:**
  - Transition không hợp lệ (e.g. instance đang FAILED) → `IllegalStateException` (HTTP 409)
- **FE Behavior:** Show loading trạng thái REGENERATING; cache automatic invalidate qua SSE / poll `GET /api/v1/instances/{id}`

### UC-AIB-10: Enterprise Rebrand Approval (2-person rule)
- **Actor:** Initiator (admin 1) → Approver (admin 2)
- **Precondition:** Instance Enterprise tier, status DEPLOYED
- **Steps:**
  1. Admin 1: `RebrandApprovalService.request(instanceId, initiatorUserId, expectedVersion, reason)`
  2. Service kiểm tra:
     - `expectedVersion` match `FrontendInstance.@Version` → nếu không match → `ConcurrentRebrandException` (HTTP 409, BR-APRV-005)
     - Không có PENDING approval khác cho cùng instance (BR-APRV-006)
  3. Tạo `RebrandApproval { status=PENDING, requestedAt=now, expiresAt=now+24h }`; outbox `rebrand.requested`
  4. Email notify Admin 2 (consumer của outbox event)
  5. Admin 2 review:
     - Approve: `service.approve(approvalId, approverUserId)`. Service verify `approverUserId != initiatorUserId` (BR-APRV-002, else 409). Status PENDING → APPROVED; outbox `rebrand.approved`. Caller controller sau đó gọi `lifecycle.rebrand(instanceId)` (UC-AIB-09 step 2-4).
     - Reject: `service.reject(approvalId, approverUserId, rejectionReason)`. PENDING → REJECTED; outbox `rebrand.rejected`. Không gọi lifecycle.
  6. Nếu `expiresAt` đến mà status vẫn PENDING → scheduler đặt EXPIRED (BR-APRV-003); outbox `rebrand.expired`
- **Postcondition:** APPROVED/REJECTED/EXPIRED là terminal (BR-APRV-001) — không mutate được nữa
- **Errors:** xem BR-APRV-002/005/006

### UC-AIB-11: Branding Package Fetch (composite + ETag)
- **Actor:** kiteclass-frontend (BrandingProvider) hoặc external integrator
- **Precondition:** Instance ở DEPLOYED
- **Steps:**
  1. FE: `GET /api/v1/branding/{instanceId}/package` với header `If-None-Match: <etag-from-cache>`
  2. `BrandingPackageController.get()` gọi `packageService.getByInstanceId()`
     - Cache hit: `CachingBrandingPackageProxy` trả ngay từ Redis
     - Cache miss: `BrandingPackageServiceImpl` build từ DB (theme + asset URLs + metadata)
  3. Server tính `etag = "W/\"v{brandingVersion}-{hashHex}\""`
  4. Match `If-None-Match`:
     - Match → 304 Not Modified, body rỗng, header `ETag`
     - No match → 200 với `BrandingPackage { instanceId, tenantId, slug, frontendUrl, brandingVersion, deployedAt, assets[] }`, header `ETag`
- **Postcondition:** FE inject CSS variables từ theme; cache theo ETag; subscribe SSE để invalidate khi `brandingVersion` thay đổi
- **Lưu ý:** Cache evict tự động khi `instance.deployed` / `instance.regenerating` outbox event fire (CachingBrandingPackageProxy event listener)

### UC-AIB-12: Public Branding Lookup (unauthenticated)
- **Actor:** Anonymous user trên login/register/reset-password page
- **Precondition:** Tenant slug hoặc UUID known
- **Steps:**
  1. FE: `GET /api/v1/branding/public?tenantId={uuid|slug}`
  2. `PublicBrandingController.get()` resolve UUID:
     - Input là UUID hợp lệ → dùng trực tiếp
     - Input là slug → query `FrontendInstanceRepository.findBySlugAndDeletedFalse` để lấy `tenantId`
  3. Lookup `Branding` entity theo `instanceId`; trả minimal payload `{ displayName, logoUrl, primaryColor, secondaryColor, accentColor, tagline }`
  4. Tenant không tồn tại / chưa có branding → trả defaults (KiteClass logo + #3B82F6/#8B5CF6/#10B981)
- **Postcondition:** Login screen render với tenant logo + colors
- **Lưu ý bảo mật:** Endpoint TUYỆT ĐỐI không leak admin-only fields (contact info, social media, internal config) — payload constrained 6 fields cố ý
