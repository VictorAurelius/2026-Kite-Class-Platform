# AI Branding — Developer Guidelines

**Priority:** 🟠 MANDATORY
**Version:** 1.1.0
**Created:** 2026-04-14
**Last-Reviewed:** 2026-04-26
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; backfill of frontmatter happens this same edit per §3 backfill-on-next-edit policy)
**Applies to:** Every PR touching `kitehub-branding/**` (Java + frontend) or AI provider config

How to implement AI Branding features correctly. **Key feature của dự án** — phải tuân thủ nghiêm.

Reference: `documents/02-architecture/ai-branding-v2-redesign.md`

## Core Principle

> **KiteClass là SaaS giáo dục, KHÔNG phải AI creative platform.**
> Target: **Best possible branded instance**, KHÔNG phải user creativity.

Mọi design decision phải align với triết lý này.

---

## 1. Resource Classification (MANDATORY)

Mọi branding resource PHẢI được phân loại thành 1 trong 3 categories:

```java
public enum ResourceCategory {
  STATIC,    // User-uploaded or system default (no compute)
  TEMPLATE,  // SVG template + brand params (fast compose, near-zero cost)
  FULL_AI    // AI-generated (heavy, async, expensive)
}
```

### Rules

- ✅ **Template-first**: default route qua TEMPLATE path (~80% requests)
- ✅ **AI only when necessary**: FULL_AI chỉ khi template không match hoặc user Enterprise yêu cầu custom
- ❌ **Never skip classification**: mọi request phải qua `ResourceRoutingService.classify()`
- ❌ **Never hardcode category**: phải dynamic dựa trên context (tier, uploaded assets, template availability)

---

## 2. User Prompt Constraints

### 2.1 Free-form prompt is BANNED (except Enterprise opt-in)

```tsx
// ❌ WRONG — free-form prompt field
<textarea placeholder="Describe your banner..." />

// ✅ CORRECT — constrained presets
<RadioGroup options={['Professional', 'Friendly', 'Energetic', 'Luxurious']} />
<TemplatePicker templates={filteredTemplates} />
```

### 2.2 Always provide 6+ template previews

User chọn visual, KHÔNG describe bằng text.

### 2.3 Backend composes fixed prompts

```java
// AI prompt internally constructed từ user choices:
String prompt = String.format(
  "Generate banner for %s education center, %s tone, " +
  "matching template %s, brand colors %s",
  audience, tone, templateId, colors
);
// User never writes AI prompt directly
```

### 2.4 Enterprise Advanced Mode

Cho phép free prompt CHỈ khi:
- User tier = ENTERPRISE
- Explicit opt-in qua settings toggle
- Show disclaimer về unpredictable output
- Fallback template nếu AI fail quality gate

---

## 3. Workflow Pattern: Agent Orchestration

### 3.1 Required pattern

```
AnalyzerService → PlannerService → PlanExecutor
```

- ❌ **KHÔNG direct AIClient.generate()** trong controller
- ✅ Luôn đi qua 3 layers: analyze context → plan steps → execute với fallback

### 3.2 Each Step must implement interface

```java
public interface Step {
  String name();
  void execute(StepContext ctx);
  default boolean hasFallback() { return false; }
  default void fallback(StepContext ctx) { throw new UnsupportedOperationException(); }
}
```

### 3.3 Heavy tasks async

- ❌ **KHÔNG sync HTTP call Ollama cho image generation** (2-5 phút blocking)
- ✅ Enqueue vào RabbitMQ `ai.generate.{tier}` queue
- ✅ Return jobId ngay, FE polls hoặc SSE subscribe

---

## 4. UI/UX Rules

### 4.1 Wizard pattern (required cho new tenant)

Provisioning wizard 6 steps:
1. Welcome + info
2. Upload logo (optional)
3. Choose audience
4. Choose tone
5. Choose template (6 previews)
6. Preview + approve per resource

### 4.2 Preview before commit (MANDATORY)

- ❌ **KHÔNG auto-deploy sau generate**
- ✅ Show live preview (iframe) với generated assets
- ✅ User approve từng resource (logo, colors, banner, hero) riêng lẻ

### 4.3 Regenerate limits (MANDATORY)

| Tier | Regenerate/session |
|------|-------------------|
| FREE | 3 |
| PRO | 10 |
| PREMIUM | 30 |
| ENTERPRISE | Unlimited |

Counter PHẢI visible. Nếu hết quota → disabled button + upgrade CTA.

### 4.4 Anti-patterns — NEVER DO

| ❌ Don't | ✅ Do |
|---------|------|
| "Describe banner in 50 words" text field | 6 template previews + tone selector |
| Single "Generate" → black box output | Step-by-step wizard with reasoning |
| All-or-nothing approve | Per-resource approve |
| Unlimited regenerates | Tier-based counter visible |
| Hide preview until deploy | Show preview each step |
| Mystery AI | Explain why template matches user choices |

---

## 5. Quality Gate (MANDATORY before DEPLOY)

Sau khi generating xong, PHẢI chạy automated quality review:

```java
QualityReport report = instanceQualityReviewer.review(instanceId);
if (report.score < 70) {
  // Block deploy, auto-regenerate OR mark FAILED
  lifecycleService.markFailed(instanceId, report.issues);
  return;
}
lifecycleService.deploy(instanceId);
```

**5 checks:**
1. WCAG contrast ratio ≥ 4.5:1
2. CSS variables applied (no defaults remaining)
3. No broken asset URLs (404)
4. Visual regression vs baseline ≤ 20% diff
5. Logo placement (not cropped, size appropriate)

---

## 6. Lifecycle State Machine

Frontend instance lifecycle (**không confuse với SubscriptionStatus**):

```
NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
                  ↓              ↓          ↑
                FAILED ←────── FAILED ──────┘ (retry)
```

### Rules

- ✅ State transitions via `InstanceLifecycleService` ONLY
- ✅ Invalid transitions throw `IllegalStateException`
- ✅ Each transition publish RabbitMQ event
- ❌ **KHÔNG set status trực tiếp** từ controller/service khác

---

## 7. Integration với KiteClass Frontend

### 7.1 Composite API (required)

`GET /api/v1/branding/{instanceId}/package` → return theme + assets + metadata + ETag.

FE fetch 1 lần, cache với ETag, conditional revalidate.

### 7.2 Theme injection

```tsx
// kiteclass-frontend/src/providers/BrandingProvider.tsx
useEffect(() => {
  document.documentElement.style.setProperty('--color-primary', branding.theme.primaryColor);
  // ... other vars
}, [branding]);
```

### 7.3 Event-driven updates

Khi branding update → RabbitMQ event → kiteclass-core invalidate cache → FE re-fetch via SSE.

---

## 8. Template Creation Rules

Mỗi template PHẢI pass 5 criteria trước khi commit:

1. **Brand-agnostic**: SVG placeholders, không hardcode colors
2. **WCAG AA**: contrast ≥ 4.5:1
3. **Responsive**: work từ 320px → 3840px
4. **Text safety**: 50-char Vietnamese headline không overflow
5. **Brand family consistency**: cùng "set" templates dùng cùng visual language

Chi tiết: GAP-011.

---

## 9. Security & Privacy

- ✅ Logo analysis: gọi local Ollama (không send tới OpenAI) trừ khi user consent
- ✅ AI-generated images: store local MinIO, không upload ra bên ngoài
- ✅ Rate limit per tier strict enforce
- ❌ Never log user's uploaded logo content trong plaintext logs

---

## 10. Design Patterns (MANDATORY)

Reference: `documents/02-architecture/ai-branding-design-patterns.md`

**Required patterns:**

| Component | Pattern | Why |
|-----------|---------|-----|
| AIClient | Strategy | Swap providers |
| InstanceStatus | State Machine | Enforce transitions |
| Steps | Command + Composite | Composable pipeline |
| BrandingService | Facade | Simplified API |
| External AI APIs | Adapter + ACL | Vendor isolation |
| Caching layer | Proxy | Transparent caching |
| Events | Observer + Outbox | Reliable pub-sub |
| Provisioning | Saga | Distributed txn |
| Resilience | Circuit Breaker + Bulkhead | Fault tolerance |
| Wizard FE | XState State Machine | Complex flow |

**Anti-patterns BANNED:**

- ❌ God Service (one class with 20+ methods)
- ❌ Primitive Obsession (String everywhere instead of value objects)
- ❌ Direct event publish (must use Outbox)
- ❌ Status switch/if cascades (must use State Pattern)
- ❌ Ollama/OpenAI types in domain layer (must wrap with Adapter)

**Code review checklist:**
- [ ] New AI provider added? → Strategy Pattern applied
- [ ] Status changes? → Through State Machine, not direct set
- [ ] New pipeline step? → Implements Step interface, composable
- [ ] New event? → Via Outbox, not direct publish
- [ ] External API call? → Through Adapter
- [ ] Service >15 methods? → Refactor with Facade

## 11. Testing Requirements

### 10.1 Unit tests
- Each Step class: isolated test với mock dependencies
- Classify logic: cover 4 scenarios (static/template/AI/fallback)

### 10.2 Integration tests
- Full wizard flow: wizard completion → provisioning → deploy
- Quality gate: mock <70 score → verify FAILED transition

### 10.3 E2E tests
- Playwright: new tenant signup → complete wizard → see DEPLOYED dashboard
- Visual regression: before/after branding update

### 11.4 Migration test checklist (MANDATORY for AI behavior changes)

**Khi nào áp dụng:** PR thay đổi AI behavior — model swap (Llama → Gemma, llama3.1 → llama3.2), prompt template rewrite, AI provider swap (Ollama → Bedrock), §5 Quality Reviewer logic, ContentModerationService logic. Auto-trigger qua `audit-gate.py` AUDIT_RULES rule `ai-branding-quality-gate`.

**Process:** chạy `/ai-branding-quality-gate` skill — manual checklist 5 sections × 20 điểm = /100. Score <70 = block migration. Output report: `documents/04-quality/audits/ai-branding/YYYY-MM-DD-<change>.md`.

#### 11.4.1 Output behavior consistency
- Generate **5 sample outputs minimum** per change (3 audience × 2 tones)
- Score each /4 against: cultural fit (VN tone), length, brand-safety, schema match
- 5/5 sample outputs ≥3/4 = pass
- A/B vs baseline (1 baseline output, 1 new output, blind compare): new ≥ baseline = pass

#### 11.4.2 Tool-calling / Schema integration
- `PlannerService.generatePlan()` returns valid `BrandingPlan` JSON (snapshotTest)
- `AnalyzerService.analyze()` returns `AnalysisResult` complete fields
- `PlanExecutor` executes returned plan without unhandled exceptions
- Step interface contracts unchanged (else migration script provided)
- Outbox events fire correctly with new payload schema

#### 11.4.3 §5 Quality Gate compatibility
- `InstanceQualityReviewer.review()` scaffold checks callable
- Mock 5/5 PASS scenario → score 100 → `markDeployed()` fires
- Mock <70 scenario → `markFailed()` fires + reason captured
- `ContentModerationService` 3-stage pipeline returns `ModerationStatus`
- Failure → AuditLog entry created

#### 11.4.4 Resilience & fallback
- CircuitBreaker fallback → `templateFallback` activates → STATIC/TEMPLATE path returns cached/default theme
- Bulkhead isolation: concurrent 4-worker test (Oracle 24GB constraint) → no thread starvation
- Retry policy unchanged or migration test added
- Timeout >2 min triggers timeout + queues for retry
- Tier rate-limit FREE 3 / PRO 10 / PREMIUM 30 still enforced

#### 11.4.5 Tier-specific governance
- FREE tier template-first routing intact (≥80% requests STATIC/TEMPLATE)
- PRO tier regenerate counter visible + decremented
- PREMIUM tier additional template variants accessible
- ENTERPRISE Advanced Mode toggle + free-prompt opt-in still gated by `ai.enterprise.advancedModeEnabled` flag
- Free-form prompt BANNED for FREE/PRO/PREMIUM (per §2.1)

#### Acceptance criteria for migration PR

- [ ] `/ai-branding-quality-gate` skill ran trên migration PR
- [ ] Report saved to `documents/04-quality/audits/ai-branding/YYYY-MM-DD-<change>.md`
- [ ] Score ≥70 (else block migration; fix issues, re-run gate)
- [ ] 5 sample outputs documented in §11.4.1
- [ ] Delta vs baseline (62/100 baseline 2026-04-26) reported
- [ ] If <85, file follow-up gaps for sub-issues <16/20

**Real automation** (WCAG measurement, visual regression diff, ML classifier scoring) requires infra not yet landed:
- Real WCAG contrast measurement → GAP-226 (Wave 8+)
- Real visual regression diff → GAP-227 (Wave 8+)
- Real ML classifier scoring → GAP-228 (Wave 8+)

Until those land, manual checklist mode is the source of truth per skill `quality/ai-branding-quality-gate/SKILL.md`.

---

## Quick Checklist for PR Reviewer

Khi review PR liên quan AI branding, check:

- [ ] Không có free-form prompt cho thường users
- [ ] Resource được classify đúng (STATIC/TEMPLATE/FULL_AI)
- [ ] Template-first routing (AI chỉ khi cần)
- [ ] Heavy tasks async qua queue
- [ ] Preview + approve per resource
- [ ] Regenerate counter hiển thị
- [ ] Quality gate trước DEPLOYED
- [ ] Event-driven lifecycle transitions
- [ ] Không bypass `InstanceLifecycleService`
- [ ] Tests: unit + integration + E2E
- [ ] Template (nếu thêm mới): pass 5 review criteria
- [ ] **Migration PR (model swap, prompt change, provider rewrite, §5 logic):** `/ai-branding-quality-gate` skill ran + report committed + score ≥70 (per §11.4)

---

## Log

- **2026-04-26** (v1.1.0): MINOR — added §11.4 Migration test checklist subsection (5 sub-sections × 20 points; mandatory `/ai-branding-quality-gate` skill run; baseline 62/100 captured 2026-04-26). Backfilled mandatory frontmatter (Version, Created, Last-Reviewed, Reviewer-Approver, Applies-to) per `rule-change-process.md` §3 backfill-on-next-edit policy. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — paired with skill creation + audit-gate rule + baseline audit in same Sub-PR 223.1). Closes part of GAP-223 Sub-PR 223.1 (Option C). Motivation: AI behavior changes (model upgrade, prompt rewrite) shipped Wave 4 với scaffold-only verification; GAP-006 Gemma 4 9B migration cannot ship without migration checklist (§11.4) + skill + audit-gate trigger. Real WCAG/vrg/ML automation deferred to GAP-226/227/228.
- **2026-04-14** (v1.0.0): Rule established sau AI Branding redesign v2.
