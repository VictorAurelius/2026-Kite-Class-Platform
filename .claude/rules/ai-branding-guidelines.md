---
paths:
  - "kitehub/kitehub-branding/**"
---

# AI Branding — Developer Guidelines

**Priority:** 🟠 MANDATORY
**Version:** 1.3.0
**Created:** 2026-04-14
**Last-Reviewed:** 2026-06-09
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.2.2 PATCH self-approve per `rule-change-process.md` §5; tier-name drift sweep §4.3 + §11.4 stale `PRO` → canonical `BASIC` (PricingTier enum không có tier PRO), align với SUB-22 entitlement matrix — value-preserving, no constraint change. v1.2.1 (kept): PATCH self-approve per §5; thêm `paths:` frontmatter cho path-scoped auto-load qua Wave 73 Bucket A5 — không thay đổi scope rule, chỉ defer-load khi không có file trong `kitehub/kitehub-branding/**` ở context. v1.2.0 (kept): MINOR self-approve per §5; new §2.5 input cap rule paired với `AIInputCapService` + tests + business rules.md BR-INPUT-CAP-001..007 in same PR per §6.5 Enforcement Parity Mandate)
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

#### FULL_AI banner image-gen tier-gate (GAP-1137, 2026-06-10)

Tách bạch với free-prompt ở trên: banner render có 2 mode (per ADR-037 Amendment + SUB-22 matrix):
- **TEMPLATE** (HTML+Gemini→Playwright, $0) — mặc định MỌI tier.
- **FULL_AI** (GPT-5.5 image-gen, có phí) — **chỉ PREMIUM + ENTERPRISE** (user decision 2026-06-10, nới từ ENTERPRISE-only):
  - **PREMIUM** = giới hạn cost quota riêng (`ai.rate-limit.fullai-premium-per-month`, mặc định 5/tháng); hết quota → fallback TEMPLATE.
  - **ENTERPRISE** = unlimited.
  - **FREE / BASIC** = không eligible (TEMPLATE only).
- Enforce: `GenerationMode.forTier(tier)` (eligibility) + `FullAiQuotaService.canUseFullAi(instanceId, tier)` (PREMIUM monthly cap) + input-cap §2.5 mọi callsite + cost metric `ai.fullai.call{tier,outcome}` (Micrometer) + CircuitBreaker fallback TEMPLATE.

### 2.5 Input prompt token cap (GAP-258, MANDATORY)

Mọi callsite vào AI provider PHẢI đi qua `AIInputCapService#checkInputSize(tier, userInputs...)` TRƯỚC khi gọi `AIBrandingService` / `OpenAIClient` / `OllamaClient`.

**Tier defaults** (estimated tokens — chars/4 heuristic, configurable via `ai.input.*` keys; `-1` = unlimited):

| Tier | Cap |
|------|-----|
| FREE / TRIAL | 2000 |
| BASIC | 4000 |
| PREMIUM | 8000 |
| ENTERPRISE | 16000 (or `-1` for unlimited) |

**Rules:**
- ❌ KHÔNG bypass — kể cả Enterprise Advanced Mode (§2.4) phải qua cap (cap có thể đặt `-1` thay vì bỏ check)
- ❌ KHÔNG record usage trước khi check cap → reject path không tốn quota request-count
- ✅ Reject response: HTTP 400 + `{error: "AI_INPUT_TOO_LONG", estimatedTokens, maxTokens, tier}`
- ✅ Counter `ai.input.token.rejection{tier}` phải emit mỗi rejection (pattern alert tương tự `RateLimitBreachSpike`)
- ✅ Unknown tier → fallback FREE cap (fail-safe)

**Why:** rate-limit theo request count alone không bound được cost. 1 request × 100k tokens cost gấp 50× của 1 request × 2k tokens, nhưng cùng tốn 1 slot. Cap tại entry-point chặn cost-attack DDoS nhắm vào surface AI.

**Reference:** `BR-INPUT-CAP-001..007` trong `documents/01-business/kiteclass/ai-agent-workflow/rules.md`. Real BPE tokenizer (tiktoken-java) tracked separately — heuristic chars/4 đủ cho v1 vì over-estimate Vietnamese (fail-safe theo cost).

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
| BASIC | 10 |
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
- Tier rate-limit FREE 3 / BASIC 10 / PREMIUM 30 still enforced

#### 11.4.5 Tier-specific governance
- FREE tier template-first routing intact (≥80% requests STATIC/TEMPLATE)
- BASIC tier regenerate counter visible + decremented
- PREMIUM tier additional template variants accessible
- ENTERPRISE Advanced Mode toggle + free-prompt opt-in still gated by `ai.enterprise.advancedModeEnabled` flag
- Free-form prompt BANNED for FREE/BASIC/PREMIUM (per §2.1)

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

- **2026-06-09** (v1.3.0): MINOR — added §2.4 sub-section "FULL_AI banner image-gen tier-gate (GAP-1137)" codifying user decision 2026-06-10: banner FULL_AI (GPT-5.5, có phí) nới từ ENTERPRISE-only → **PREMIUM (limited monthly cost quota, mặc định 5/tháng) + ENTERPRISE (unlimited)**; FREE/BASIC = TEMPLATE-only. Enforce qua `GenerationMode.forTier` (eligibility) + `FullAiQuotaService` (PREMIUM cap) + input-cap §2.5 + cost metric `ai.fullai.call{tier,outcome}` + CircuitBreaker fallback TEMPLATE. Paired same-PR (GAP-1137) với: code (`GenerationMode`/`FullAiQuotaService`/`AIBrandingProcessor`/`AIRateLimitConfig`/`DistributedRateLimiter` + tests) + `application.yml` (premium-per-day 50→30 canonical SUB-22 + fullai-premium-per-month=5) + SUB-22 matrix AI-mode column (`subscription-billing/rules.md`) + ADR-037 amendment sync per `rule-change-process.md` §6.5 Enforcement Parity Mandate. MINOR per §4 — adds FULL_AI banner tier-gate spec (new constraint, additive; free-prompt §2.4 ENTERPRISE-only unchanged). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — codifies user pricing/tier decision; no constraint loosening; existing mock-generation grandfathered; gate applies prospectively khi real AI wired GAP-1135).
- **2026-06-09** (v1.2.2): PATCH — tier-name drift sweep (tier-enforcement wave). Đổi 4 instance stale `PRO` → `BASIC` cho đúng canonical `PricingTier` enum (`FREE/BASIC/PREMIUM/ENTERPRISE` — không có tier `PRO`): §4.3 regen table `PRO 10` → `BASIC 10`; §11.4.4 `FREE 3 / PRO 10 / PREMIUM 30` → `FREE 3 / BASIC 10 / PREMIUM 30`; §11.4.5 `PRO tier regenerate counter` → `BASIC tier regenerate counter`; §11.4.5 `BANNED for FREE/PRO/PREMIUM` → `FREE/BASIC/PREMIUM`. Value-preserving (tier có regen=10 = BASIC per SUB-22 matrix; PREMIUM giữ 30). Align với `subscription-billing/rules.md` SUB-22 entitlement matrix (canonical) + §2.5 input cap (đã đúng FREE/BASIC/PREMIUM/ENTERPRISE). Per `cross-flow-bug-class-sweep.md` — sweep evidence trong PR body. PATCH per `rule-change-process.md` §4 (correction of stale label → clarification, no constraint change; enforcement reality unchanged — BASIC tier vẫn = 10 regen). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — stale-name correction, no constraint loosening).
- **2026-05-14** (v1.2.1): PATCH — thêm `paths: ["kitehub/kitehub-branding/**"]` frontmatter qua Wave 73 Bucket A5 (path-scope 6 design/wave/AI rules). Per Anthropic native `paths:` mechanism (https://code.claude.com/docs/en/memory), rule giờ chỉ auto-load khi Claude đọc file trong `kitehub/kitehub-branding/**`. Không thay đổi rule content/scope; reduces base context auto-load per Wave 73 Meta Context Optimization plan. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — additive frontmatter, no constraint change).
- **2026-04-28** (v1.2.0): MINOR — added §2.5 Input prompt token cap (GAP-258). Tier-aware caps (FREE 2000 / BASIC 4000 / PREMIUM 8000 / ENTERPRISE 16000 tokens; configurable via `ai.input.*`; `-1` = unlimited). Defends against cost-attack DDoS where small request count carries oversized prompts. Paired same-PR with `AIInputCapService` + `PromptTokenEstimator` + tier-aware `AIInputCapConfig` + Micrometer counter `ai.input.token.rejection{tier}` + 13 unit tests + 3 IT + business rules `BR-INPUT-CAP-001..007` per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint, no constraint loosening). Motivation: 2026-04-28 article state-check ("Những lỗi 'chết người' khi build AI backend (Phần 2) — Không rate limit") surfaced that `OpenAIClient` capped output tokens only; per-day request-count cap alone does not bound input cost.
- **2026-04-26** (v1.1.0): MINOR — added §11.4 Migration test checklist subsection (5 sub-sections × 20 points; mandatory `/ai-branding-quality-gate` skill run; baseline 62/100 captured 2026-04-26). Backfilled mandatory frontmatter (Version, Created, Last-Reviewed, Reviewer-Approver, Applies-to) per `rule-change-process.md` §3 backfill-on-next-edit policy. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — paired with skill creation + audit-gate rule + baseline audit in same Sub-PR 223.1). Closes part of GAP-223 Sub-PR 223.1 (Option C). Motivation: AI behavior changes (model upgrade, prompt rewrite) shipped Wave 4 với scaffold-only verification; GAP-006 Gemma 4 9B migration cannot ship without migration checklist (§11.4) + skill + audit-gate trigger. Real WCAG/vrg/ML automation deferred to GAP-226/227/228.
- **2026-04-14** (v1.0.0): Rule established sau AI Branding redesign v2.
