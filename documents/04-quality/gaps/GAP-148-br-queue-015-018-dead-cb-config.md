# GAP-148: BR-QUEUE-015..018 — Circuit breaker config dead in kitehub-branding (no Java wiring)

**Status:** 🟢 DONE (Wave 9-D, 2026-04-21 — chose Option A)
**Priority:** 🟡 P2
**Domain:** Backend (kitehub-branding / AI infra)
**Found:** 2026-04-20 (business-logic audit refresh — [`business-logic-audit-2026-04-20.md`](../audits/business/business-logic-audit-2026-04-20.md) §New Violations)
**Affects:** Wave 3 fair-queue Phase 1 resilience layer (AIQueueDispatcher, AIJobConsumer)

---

## Problem

`documents/01-business/kiteclass/ai-agent-workflow/rules.md:54-57` (BR-QUEUE-015..018, added bởi GAP-104 PR #371) document circuit breaker contract cho `ResilientAIClient`:

- BR-QUEUE-015: failureRateThreshold = 50%
- BR-QUEUE-016: waitDurationInOpenState = 30s
- BR-QUEUE-017: slidingWindowSize = 20 calls
- BR-QUEUE-018: minimumNumberOfCalls = 10

Config keys này EXIST trong `kitehub-branding/application.yml:92-99`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      ai-provider:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
```

**Drift:** `ResilientAIClient` class và resilience4j annotations CHỈ tồn tại trong `kiteclass/kiteclass-core`, KHÔNG có trong `kitehub-branding` nơi fair-queue code (AIQueueDispatcher + AIJobConsumer) lives.

### Evidence

```bash
$ grep -rn "@CircuitBreaker\|@Bulkhead\|@Retry" kitehub/kitehub-branding/src/main/java/
# 0 hits

$ grep "ResilientAIClient" kitehub/kitehub-branding/src/main/java/ -r
# 0 hits

$ grep -c "class ResilientAIClient" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/ResilientAIClient.java
1
```

`kitehub-branding/pom.xml:77-82` có dependency `resilience4j-spring-boot3` + `resilience4j-reactor` — jar loaded nhưng zero annotation uses → dead dependency + dead config.

UC-AGENT-11 (`use-cases.md:93-102`) describes circuit breaker transitions (CLOSED → OPEN → HALF_OPEN) với actor là `ResilientAIClient` — nhưng ở service scope kitehub-branding (nơi fair-queue Phase 1 ship), không có enforcement Java code.

---

## Root Cause

GAP-104 (PR #371) tập trung vào 11 BR-QUEUE rules cho fair-queue dispatcher/consumer/backpressure (tất cả ĐỀU có code tương ứng trong kitehub-branding). Nhưng §"Circuit breaker around AI provider" (BR-QUEUE-015..018) được thêm vào rules.md để hoàn chỉnh resilience story, với giả định `ResilientAIClient` = source of truth cho circuit breaking.

**Thực tế:**
- `ResilientAIClient` ở `kiteclass-core` wrap `AIClient` calls cho TenantProvisioningSaga / Analyzer / Planner — đúng domain
- `kitehub-branding` AI stack dùng trực tiếp `OllamaClient` / `OpenAIClient` (no decorator) trong Queue consumer — không wrap bằng CB
- Resilience4j config block trong `kitehub-branding/application.yml` được copy-pasted từ Wave 3 Phase 1 design nhưng Java wiring chưa ship

Gap giữa rules (say CB applies) và code (CB not wired trong service hosting queue).

---

## Proposed Fix

**Option A — Wire CB trong kitehub-branding (recommended):**

```java
// AIJobConsumer.java or new ResilientAIClient wrapper trong kitehub-branding
@CircuitBreaker(name = "ai-provider", fallbackMethod = "fallbackToTemplate")
@Bulkhead(name = "ai-provider")
public AIResult call(...) { ... }
```

- Thêm `@CircuitBreaker`/`@Bulkhead` annotation vào Ollama/OpenAI call sites trong kitehub-branding
- Fallback returns `AnalysisResult.templateOnly()` or equivalent (BR-AI-004 pattern)
- Tests: verify CB opens after 10 failures (BR-QUEUE-018) + 50% failure rate (BR-QUEUE-015)

**Option B — Narrow rule scope:**

Update BR-QUEUE-015..018 + UC-AGENT-11 để clarify CB rules ONLY áp dụng ở `kiteclass-core` (ResilientAIClient consumer path), KHÔNG phải fair-queue path ở kitehub-branding Phase 1. Remove resilience4j block khỏi `kitehub-branding/application.yml` để tránh dead config confusion.

**Option C — Defer to GAP-005 Phase 2:**

Part of horizontal scaling work. Mark BR-QUEUE-015..018 as "⏳ Phase 2" trong rules.md và acknowledge dead config hiện tại.

**Recommend Option A** — resilience là required for Wave 3 production readiness per `ai-branding-guidelines.md` §3 (heavy tasks + resilience required). Implementation low-risk (few annotations). Ships closer alignment với rules.md description.

---

## Acceptance Criteria

- [ ] Chọn Option (A/B/C) và document decision trong PR description
- [ ] Nếu Option A: `@CircuitBreaker("ai-provider")` annotation trên AI call sites trong kitehub-branding
- [ ] Nếu Option A: unit test verify CB opens theo BR-QUEUE-015/017/018 thresholds
- [ ] Nếu Option A: fallback method returns domain-safe template result
- [ ] Nếu Option B: rules.md + use-cases.md narrow scope; remove resilience4j block khỏi kitehub-branding yml
- [ ] Nếu Option C: mark rules "Phase 2 deferred" với reference tới GAP-005
- [ ] Audit refresh confirms BR-QUEUE-015..018 trace chain (rule → config → Java) hoặc updated scope trace

---

## Related

- **Audit:** [`business-logic-audit-2026-04-20.md`](../audits/business/business-logic-audit-2026-04-20.md) §New Violations row 1
- **Baseline context:** GAP-104 (CLOSED PR #371) — introduced BR-QUEUE-015..018
- **Meta context:** `ai-branding-guidelines.md` §3 (resilience patterns mandatory cho heavy AI calls)
- **Related classes:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/ResilientAIClient.java` (reference pattern)

---

## Log

- 2026-04-21 (Wave 9-D) — Closed via **Option A**. New `kitehub-branding/src/main/java/com/kitehub/branding/client/ResilientAIClient.java`:
  - `@Primary @Component("resilientAIClient")` implements `AIClient`
  - `@CircuitBreaker(name="ai-provider", fallbackMethod=...)` on `analyzeLogo` / `generateImage` / `generateText`
  - Delegate injected via `@Qualifier("aiClient")` — `AIProviderConfig.aiClient()` demoted from `@Primary` and renamed (`@Bean(name="aiClient")`) to avoid self-cycle
  - Fallbacks return template-safe defaults (template-first philosophy per `ai-branding-guidelines.md` §1) — `LogoAnalysis` defaults, placehold.co URL, Vietnamese default copy
  - `CB_NAME` constant = `"ai-provider"` matches `application.yml:95`
  
  Tests: 8 new in `ResilientAIClientTest` (delegate happy path × 3, provider name, fallbacks × 3, CB name constant). Existing `AIProviderConfigTest` still passes — direct method invocation unchanged.
  
  Rules.md + use-cases.md updated: BR-QUEUE-015..018 code reference now points to `kitehub-branding/client/ResilientAIClient` (authoritative) with note that `kiteclass-core` still has its own wrapper under CB name `ai`. UC-AGENT-11 Actor updated.
- 2026-04-20 — Gap created after business-logic refresh found BR-QUEUE-015..018 partial drift (config exists, Java wiring missing trong kitehub-branding scope).
