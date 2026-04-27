# AI Branding v2 — Redesign Document

**Trạng thái:** 🟢 SHIPPED (Waves 2-4) — design adopted with module-location deviation noted in §0
**Ngày:** 2026-04-14 (initial), 2026-04-26 (GAP-234 sync)
**Supersedes:** `documents/03-planning/implementation/ai-local-implementation-plan.md` (phần architecture)
**Current state:** 4/10 baseline (2026-04-14) → ~7/10 after Waves 2-4 ship; quality-audit baseline 62/100 captured 2026-04-26 per `ai-branding-guidelines.md` §11.4.

---

## 0. Implementation Reality Note (added 2026-04-26 / GAP-234)

This document was written 2026-04-14 assuming AI Branding workflow code would land in `kitehub-branding/`. Implementation diverged in Waves 2-4:

| Layer | Original spec | Shipped location |
|-------|--------------|------------------|
| Workflow engine (Analyzer/Planner/Executor + Steps) | `kitehub-branding/` | **`kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/workflow/`** |
| Resource handlers (Static/Template/AI/Fallback) | `kitehub-branding/` | **`kiteclass-core/module/branding/handler/`** |
| `FrontendInstance` entity + lifecycle | `kitehub-branding/` | **`kiteclass-core/module/instance/`** |
| `InstanceQualityReviewer` + 5 quality checks | `kitehub-branding/` | **`kiteclass-core/module/quality/`** |
| `ContentModerationService` | `kitehub-branding/` | **`kiteclass-core/module/moderation/`** |
| `TenantProvisioningSaga` | `kitehub-branding/` | **`kiteclass-core/module/provisioning/`** |
| RabbitMQ queue topology + worker pool + AIQueueDispatcher | `kitehub-branding/` | `kitehub-branding/queue/` (unchanged) |
| Tier queues `ai.request.{enterprise,pro,free}` + DLQs | `kitehub-branding/` | `kitehub-branding/config/AIQueueConfig` (unchanged) |

**Class renames** (sections §3.3 below show original names):

| §3.3 spec | Shipped class |
|-----------|---------------|
| `BrandingAnalyzer` | `AnalyzerService` |
| `BrandingPlanner` | `PlannerService` |
| `BrandingExecutor` | `PlanExecutor` |

**Why the deviation:** branding lives next to tenant data it themes — co-location simplifies multi-tenant isolation, transactional outbox writes (V33), and avoids cross-service joins between `kitehub-*` and `kiteclass_shared`. `kitehub-branding` retains queue dispatch + worker pool because tier scheduling is a cross-tenant concern owned by the SaaS layer.

DB migrations actually shipped to KiteClass: V31 `frontend_instances`, V32 `branding_resources`, V33 `outbox_events`, V34 `rebrand_approvals`, V35 `audit_log`, V36 `moderation_queue`, V37 `dmca_takedown_requests`, V38 `deletion_requests`, V39 `quality_reports`, V40 `branding`, V43 `branding_versions`, V45 index on `branding_resources(instance_id, deleted)`.

Architecture/diagram drift between this doc and shipped code surfaced in GAP-234 (2026-04-26) and was fixed by syncing diagrams + appending this §0. Class examples in §3.3 still show the original names; treat them as design intent, with the shipped class names canonical.

---

AI Branding là **key feature** của KiteHub. Tài liệu này redesign architecture theo best practice, cover 4 vấn đề chính user đã chỉ ra:

1. Capacity planning — độ chịu tải cho N users
2. Resource classification pipeline (static / template / full-AI)
3. AI Agent workflow vs direct generator
4. Frontend instance provisioning lifecycle

---

## 1. Assessment: Current State (4/10)

### Có sẵn ✅
- `AIBrandingService` — logo analysis, hero generation, marketing copy
- `BrandingJob` entity + `JobStatus` enum (5 states: QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED)
- RabbitMQ queue `branding-jobs` + DLQ
- Rate limit per tier per day
- S3/MinIO storage + CDN URL pattern
- Dual AI provider (OpenAI + Ollama)

### Thiếu ❌
- Phân loại resources (static / template / full-AI)
- Workflow orchestration / AI Agent planner
- State machine lifecycle đầy đủ cho provisioning
- Webhook/event notification → kiteclass-frontend
- Composite endpoint "full branding package"
- Capacity plan rõ ràng
- Test integration KiteClass sau khi resources publish

---

## 2. Resource Classification Pipeline (best practice)

### 2.1 Ba lớp resource

```
┌─────────────────────────────────────────────────────────────┐
│                 Resource Classification                      │
├────────────┬──────────────────┬─────────────────────────────┤
│ STATIC     │ TEMPLATE         │ FULL_AI                     │
│ (no AI)    │ (scripts + AI)   │ (AI generated)              │
├────────────┼──────────────────┼─────────────────────────────┤
│ • Logo     │ • Banner         │ • Custom hero image         │
│   uploaded │   (SVG template  │   (Stable Diffusion)        │
│ • Default  │    + placeholders│ • Unique marketing visual   │
│   icons    │    + brand colors│ • Custom product photos     │
│ • Favicon  │ • Course thumb   │                             │
│ • Fonts    │ • Hero section   │                             │
├────────────┼──────────────────┼─────────────────────────────┤
│ Latency:   │ Latency:         │ Latency:                    │
│ <100ms     │ 1-3s (compose)   │ 10s-5min (depend on model)  │
│ Cost: $0   │ Cost: ~$0        │ Cost: compute heavy         │
│ Quality:   │ Quality: 85%     │ Quality: 95%                │
│ 100% det.  │ consistent       │ variable                    │
└────────────┴──────────────────┴─────────────────────────────┘
```

### 2.2 Decision tree

```
User request: "Generate hero banner"
  ↓
Does tenant uploaded custom asset?
  YES → STATIC (return uploaded asset) ✓
  NO ↓
Is there matching template for use case?
  YES → TEMPLATE (compose with brand colors + SVG→PNG) ✓
  NO ↓
User has AI quota remaining?
  YES → FULL_AI (queue, async)
  NO  → TEMPLATE fallback + prompt user to upgrade
```

### 2.3 Code structure cần thêm

```java
// NEW: Enum phân loại
public enum ResourceCategory {
  STATIC,     // Pre-uploaded hoặc system default
  TEMPLATE,   // Compose từ SVG/HTML template + brand params
  FULL_AI     // Sinh hoàn toàn bởi AI model
}

// NEW: Entity
@Entity
public class BrandingResource {
  Long id;
  String instanceId;
  ResourceType type;        // LOGO, BANNER, HERO, THUMBNAIL, FAVICON...
  ResourceCategory category; // STATIC/TEMPLATE/FULL_AI
  String storageUrl;         // MinIO S3 URL
  String templateId;         // ID của template nếu TEMPLATE
  String aiJobId;            // ID của AI job nếu FULL_AI
  ResourceStatus status;
  Map<String, Object> metadata; // brand colors used, template params, etc.
}

// NEW: Router service
@Service
public class ResourceRoutingService {
  public ResourceCategory classify(ResourceRequest req, TenantContext ctx);
  public CompletableFuture<BrandingResource> route(ResourceRequest req);
}
```

### 2.4 Best practice

✅ **Phân lớp như trên ĐỦ theo best practice** — 3 categories cover hầu hết SaaS branding platforms (Canva, Vercel, Shopify). Thêm 4th tier chỉ khi có use case như "video" hoặc "3D" trong tương lai.

---

## 3. AI Agent Workflow vs Direct Generator

### 3.1 Best practice: **Agent-first, AI-assisted**

```
User: "Tạo banner cho trang chủ trung tâm"
  ↓
AI Agent (Planner):
  Step 1: Analyze context (tenant colors, logo uploaded, target audience)
  Step 2: Decide strategy:
    - Có logo + basic colors → TEMPLATE approach
    - Specific requirements nêu trong prompt → FULL_AI
    - Generic request → TEMPLATE với variant selection
  Step 3: Execute plan (có thể multi-step):
    - Compose template A với brand colors
    - Extract dominant color from logo
    - Generate headline text (AI)
    - Combine into final image
  Step 4: Validate output (brand consistency, WCAG contrast, resolution)
  Step 5: Store + return
```

### 3.2 Tại sao Agent tốt hơn Direct?

| Aspect | Direct AI Generator | AI Agent Workflow |
|--------|---------------------|-------------------|
| Control | Low — model tự quyết | High — logic code quyết strategy |
| Consistency | Variable output | Brand-consistent |
| Cost | Luôn dùng AI (expensive) | Dùng AI khi cần |
| Latency | Luôn slow (AI call) | Fast cho template path |
| Debuggability | Black box | Step-by-step traceable |
| Fallback | Fail = return error | Template fallback khi AI fail |

### 3.3 Implementation pattern: **State Machine + Plan Executor**

> **Shipped class names** (per §0 deviation 2026-04-26):
> `BrandingAnalyzer` → **`AnalyzerService`**, `BrandingPlanner` → **`PlannerService`**, `BrandingExecutor` → **`PlanExecutor`** (this name kept).
> Location: `kiteclass-core/module/ai/workflow/`.

```java
// Step 1: Analyzer extracts structured context
// SHIPPED: com.kiteclass.core.module.ai.workflow.AnalyzerService
@Service
public class BrandingAnalyzer {
  public BrandingContext analyze(ResourceRequest req, TenantId tid);
  // Returns: colors, logo, tagline, audience, tier
}

// Step 2: Planner creates execution plan
// SHIPPED: com.kiteclass.core.module.ai.workflow.PlannerService
@Service
public class BrandingPlanner {
  public ExecutionPlan plan(BrandingContext ctx, ResourceRequest req);
  // Returns list of steps: [FetchTemplate, ComposeColors, GenerateHeadline, Render]
}

// Step 3: Executor runs plan
// SHIPPED: com.kiteclass.core.module.ai.workflow.PlanExecutor
@Service
public class PlanExecutor {
  public BrandingResource execute(ExecutionPlan plan);
  // Handles: parallel steps, retries, fallbacks, output validation
}
```

### 3.4 Khuyến nghị

✅ **Nên chuyển sang Agent workflow** — không phải AI sinh ảnh hoàn toàn mà là AI agent điều hướng tạo ảnh theo template cho 80% use cases. Pure AI generation chỉ dùng cho premium/enterprise khi user yêu cầu rõ custom.

Lợi ích:
- **Cost**: giảm 80% AI calls → tiết kiệm compute
- **Latency**: 80% resources render <3s (template) thay vì phút (AI)
- **Brand consistency**: template enforce layout + colors
- **User control**: user có thể chỉnh template, không "mông lung AI"

---

## 4. Frontend Instance Provisioning Lifecycle

### 4.1 State machine chuẩn

```
┌─────────────┐
│ NOT_STARTED │  Tenant đăng ký nhưng chưa trigger provisioning
└──────┬──────┘
       │ initiate
       ▼
┌─────────────┐
│ INITIALIZING│  Create instance shell, allocate resources
└──────┬──────┘
       │ resources ready
       ▼
┌─────────────┐
│ GENERATING  │  Running branding pipeline (analyze/plan/execute)
└──┬───────┬──┘
   │       │
   │ ok    │ fail
   ▼       ▼
┌────────┐ ┌────────┐
│DEPLOYED│ │ FAILED │  Instance live / Provisioning failed
└────┬───┘ └────┬───┘
     │          │ retry
     │          ▼
     │    ┌─────────────┐
     │    │REGENERATING │  User/admin retry
     │    └─────┬───────┘
     │          │
     │     ┌────┴────┐
     │     ▼         ▼
     │  DEPLOYED  FAILED
     │
     │ user rebrand
     ▼
┌─────────────┐
│REGENERATING │  Re-run pipeline với config mới
└──────┬──────┘
       │
       ▼
    DEPLOYED
```

### 4.2 State descriptions

| State | Ý nghĩa | Transition OK | Transition Fail |
|-------|---------|---------------|-----------------|
| `NOT_STARTED` | Tenant tạo, chưa provision | → INITIALIZING | → (never) |
| `INITIALIZING` | Khởi tạo instance (DB, storage, DNS) | → GENERATING | → FAILED |
| `GENERATING` | Pipeline đang chạy (branding resources) | → DEPLOYED | → FAILED |
| `DEPLOYED` | Instance live, FE truy cập được | → REGENERATING (user rebrand) | (stable) |
| `REGENERATING` | Tạo lại branding (keeping instance live) | → DEPLOYED | → FAILED (instance vẫn live) |
| `FAILED` | Provisioning failed | → REGENERATING (retry) | → (abandoned after N retries) |

### 4.3 Implementation

```java
@Entity
public class FrontendInstance {
  String instanceId;
  String tenantId;
  InstanceStatus status;  // enum trên
  String frontendUrl;     // null nếu chưa DEPLOYED
  Timestamp createdAt, initializingAt, generatingAt, deployedAt, failedAt;
  Integer retryCount;
  String failureReason;
}

public enum InstanceStatus {
  NOT_STARTED, INITIALIZING, GENERATING, DEPLOYED, REGENERATING, FAILED
}

@Service
public class ProvisioningOrchestrator {
  // Event-driven via RabbitMQ
  @RabbitListener("tenant.created")
  public void onTenantCreated(TenantCreatedEvent e) { ... }

  @RabbitListener("branding.completed")
  public void onBrandingReady(BrandingCompletedEvent e) { ... }
}
```

### 4.4 Frontend notification

Webhook/SSE khi status thay đổi:
- `INITIALIZING` → FE show "Khởi tạo instance..."
- `GENERATING` → FE show progress bar với current step
- `DEPLOYED` → FE redirect to tenant URL
- `FAILED` → FE show error + retry button

---

## 5. Capacity Planning (GAP-005 enhancement)

### 5.1 Hardware baseline

| Component | Config | Throughput |
|-----------|--------|------------|
| Oracle Cloud Always Free | ARM 4 cores, 24GB RAM | ~5-10 AI req/s text (CPU) |
| AWS g4dn.xlarge | NVIDIA T4 GPU, 16GB RAM | ~30-50 req/s text, ~1 img/30s |
| Template composer (no AI) | CPU 1 core | ~100 compose/s |

### 5.2 User scenario: 100 concurrent users (30/40/30 split)

**Assumption:**
- Premium (30%): avg 2 requests/session, mix 50% template + 50% AI
- Pro (40%): avg 1 request/session, mix 70% template + 30% AI
- Free (30%): avg 1 request/session, mix 90% template + 10% AI

**Request volume in 1 min peak:**
- Premium: 30 × 2 = 60 (30 template, 30 AI)
- Pro: 40 × 1 = 40 (28 template, 12 AI)
- Free: 30 × 1 = 30 (27 template, 3 AI)
- **Total: 130 requests/min (85 template + 45 AI)**

**Capacity check:**
- Template: 85/min × 60s = ~1.4/s → 1 CPU worker đủ ✓
- AI: 45/min → cần 1 AI req/1.3s avg
  - CPU (Oracle): 5-10 req/s → đủ ✓
  - GPU: 30-50 req/s → dư sức ✓

**Kết luận: 100 concurrent users trên Oracle Cloud Always Free = feasible** nếu template-first architecture (§2) implemented. Pure AI sẽ overload.

### 5.3 Scaling thresholds

| Concurrent users | Template share | Infrastructure |
|------------------|---------------|----------------|
| 100 | 80%+ | Oracle Cloud Free (current plan) |
| 500 | 80%+ | Oracle + 1 GPU instance |
| 1000 | 80%+ | Multi-region + CDN + 2-3 GPU instances |
| 5000+ | 80%+ | K8s HPA + spot GPU pool + queue sharding |

---

## 6. Integration với KiteClass Frontend

### 6.1 API contract chuẩn

**Composite endpoint (NEW):**
```
GET /api/v1/branding/{instanceId}/package
Response:
{
  "instanceId": "uuid",
  "status": "DEPLOYED",
  "theme": {
    "primaryColor": "#2563eb",
    "secondaryColor": "#1e40af",
    "accentColor": "#f59e0b",
    "fonts": { "heading": "Inter", "body": "Inter" }
  },
  "assets": {
    "logo": "https://cdn.../logo.png",
    "favicon": "https://cdn.../favicon.ico",
    "banner": "https://cdn.../banner.png",
    "hero": "https://cdn.../hero.png"
  },
  "metadata": {
    "generatedAt": "2026-04-14T10:00:00Z",
    "version": 3
  }
}
```

### 6.2 Cache strategy

- KiteClass FE fetch `/package` 1 lần khi load, cache localStorage với version
- Check version khi user visits → re-fetch nếu version tăng
- CDN cache headers: `Cache-Control: public, max-age=3600, stale-while-revalidate=86400`

### 6.3 Event notification

Khi branding update:
- KiteHub publish event `branding.updated` → RabbitMQ
- KiteClass core listen → invalidate cache → notify connected FE clients (SSE/WebSocket)
- FE re-fetch + apply new theme

### 6.4 Testing checklist (chưa làm)

- [ ] Generate branding via kitehub-branding
- [ ] Fetch package via kiteclass-core → return đầy đủ theme + assets
- [ ] Inject theme vào kiteclass-frontend → verify UI apply colors
- [ ] Test webhook flow: update branding → FE auto-refresh
- [ ] Load test: 100 concurrent kiteclass-frontend loading branding package

---

## 7. UX Principles & Constraints (chốt với user 2026-04-14)

**Triết lý:** KiteClass là SaaS giáo dục. Target user = "best branded instance", KHÔNG phải "AI creative freedom". Dự án KHÔNG phải nền tảng AI cho user.

### Closed-loop Guided Workflow

1. **KHÔNG free-form prompts cho thường users** (99%)
   - Wizard với presets (audience, tone, style)
   - System compose prompt cố định
   - Free prompt: chỉ ENTERPRISE opt-in, với disclaimer

2. **Curated template library** (not user-generated)
   - Brand designer tạo 30 templates khởi đầu
   - Review theo 5 tiêu chuẩn (brand-agnostic, WCAG, responsive, text safety, consistency)
   - +10 templates/quarter dựa trên analytics

3. **Preview + approve per resource**
   - User xem live preview trước commit
   - Approve logo/colors/banner/hero riêng lẻ
   - Regenerate chỉ resource không hài lòng

4. **Regenerate limits per tier**
   - FREE: 3/session, PRO: 10, PREMIUM: 30, ENTERPRISE: unlimited
   - Visible counter — khuyến khích suy nghĩ

5. **Automated quality gate sau deploy**
   - Quality score /100 (WCAG, contrast, assets, visual regression)
   - <70 → block deploy + auto-regenerate
   - ≥70 → DEPLOYED

### Post-Deploy Customization

Sau khi live, tenant vẫn có flexibility:
- Quick edits (color, logo, tagline) — không tốn AI quota
- Regenerate banner/hero — tốn AI quota
- Full rebrand — chạy lại wizard

---

## 8. Implementation Gaps (linked)

Design này break thành 8 gaps để track:

| Gap | Title | Section | Priority |
|-----|-------|---------|:--------:|
| [GAP-005](../04-quality/gaps/GAP-005-ai-queue-fair-scheduling.md) | AI queue + capacity planning | §5 | 🔴 P0 |
| [GAP-007](../04-quality/gaps/GAP-007-resource-classification-pipeline.md) | Resource classification pipeline | §2 | 🔴 P0 |
| [GAP-008](../04-quality/gaps/GAP-008-ai-agent-workflow.md) | AI Agent workflow (planner + executor) | §3 | 🟠 P1 |
| [GAP-009](../04-quality/gaps/GAP-009-instance-provisioning-lifecycle.md) | Frontend instance provisioning lifecycle | §4 | 🟠 P1 |
| [GAP-010](../04-quality/gaps/GAP-010-branding-package-api-integration.md) | Branding package API + KiteClass integration + testing | §6 | 🟠 P1 |
| [GAP-011](../04-quality/gaps/GAP-011-template-library-curation-plan.md) | Template library curation plan + review standards | §7 | 🔴 P0 |
| [GAP-012](../04-quality/gaps/GAP-012-frontend-instance-quality-review.md) | Automated frontend instance quality review | §7 | 🟠 P1 |
| [GAP-013](../04-quality/gaps/GAP-013-guided-branding-wizard-ux.md) | Guided branding wizard UX (closed-loop + flexibility) | §7 | 🟠 P1 |

## 9. Roadmap

**Phase 0 (Prerequisite — parallel):**
- **GAP-011**: Template library curation (hire designer, 30 templates) — **hard dep for Phase 1**

**Phase 1 (Foundation — 1 sprint):**
- GAP-007: Resource classification enum + router
- GAP-010: Composite API + KiteClass integration test
- GAP-013: Guided wizard UX (6-step)

**Phase 2 (Intelligence — 1 sprint):**
- GAP-008: Agent workflow (analyzer + planner + executor)
- GAP-004: Template gallery (frontend-side preview + selection)
- GAP-012: Automated instance quality review

**Phase 3 (Scale — 1-2 sprints):**
- GAP-009: Full lifecycle state machine
- GAP-005: Queue scaling + WFQ
- GAP-002: Async pipeline
- GAP-006: Gemma 4 migration
- GAP-003: Multi-tier image generation

**Target:** 7/10 → 9/10 production readiness.

---

## 9. Log

- **2026-04-26 (GAP-234)** — Sync pass after Waves 2-4 shipped. Added §0 "Implementation Reality Note" documenting module-location deviation (workflow lives in `kiteclass-core` not `kitehub-branding`) + class renames (`BrandingAnalyzer`→`AnalyzerService`, `BrandingPlanner`→`PlannerService`, `BrandingExecutor`→`PlanExecutor`). Annotated §3.3 inline. Status flipped from 🟡 DRAFT → 🟢 SHIPPED. No design content rewritten — original v1 design still intact for historical reference. Drift root cause: original spec assumed kitehub-branding ownership; Wave 2 chose kiteclass-core for tenant-data co-location + transactional outbox. Reason was not formally captured at the time; would have benefitted from an ADR (now retroactively documented here).
- 2026-04-14 — v2 redesign initiated. Based on codebase scan (4/10 readiness) + 4 user-raised design issues. Waiting user review.
