# AI Agent Workflow — Use Cases

### UC-AGENT-01: Full Branding Pipeline (Happy Path)
- **Actor:** TenantProvisioningSaga (Sub-PR 3.6) / scheduled rebrand (GAP-072)
- **Steps:**
  1. Build StepContext(instanceId, tenantId)
  2. AnalyzerService.analyze(request) → AnalysisResult
  3. ctx.setAnalysis(result)
  4. PlannerService.plan(analysis) → Plan
  5. PlanExecutor.execute(plan, ctx) runs Steps sequentially
  6. Each Step reads/writes context keys; final step transitions lifecycle to DEPLOYED
- **Postcondition:** Instance DEPLOYED; branding package cache evicted; outbox events emitted

### UC-AGENT-02: Step Recovers via Fallback
- **Actor:** PlanExecutor
- **Trigger:** Step.execute() throws StepException; Step.hasFallback() = true
- **Steps:**
  1. PlanExecutor catches StepException
  2. Calls step.fallback(ctx)
  3. Records {name}[fallback] in executedSteps
  4. Emits ai.step.fallback event
  5. Continues to next step

### UC-AGENT-03: Plan Aborts (No Fallback)
- **Actor:** PlanExecutor
- **Trigger:** Step.execute() throws, hasFallback=false OR fallback itself throws
- **Steps:**
  1. PlanExecutor emits ai.plan.failed with reason
  2. Rethrows StepException to caller
- **Caller responsibility:** call InstanceLifecycleService.markFailed() — saga compensation

### UC-AGENT-04: Template-Only Path
- **Actor:** AnalyzerService via ResilientAIClient fallback
- **Trigger:** Circuit breaker open OR AI provider returned unusable result
- **Steps:**
  1. ResilientAIClient returns AnalysisResult.templateOnly()
  2. Planner still produces the full Plan
  3. Individual Steps detect templateOnly and skip AI-heavy work (e.g. GenerateLogoStep goes directly to template handler)
- **Result:** Branding succeeds via template route; no user-visible failure

### UC-AGENT-05: Extracting Palette
- **Step:** ExtractPaletteStep
- **Reads:** ctx.analysis
- **Writes:** ctx[palette] = List<String> hex colors
- **Fallback:** neutral palette (#2563EB / #1E40AF / #EFF6FF)

### UC-AGENT-06: Picking Template
- **Step:** PickTemplateStep
- **Reads:** ctx[palette] (required — throws StepException if absent)
- **Writes:** ctx[template-id]
- **Fallback:** default-template-v1

### UC-AGENT-07: Publishing Package
- **Step:** PublishPackageStep
- **Reads:** ctx[template-id] (required)
- **Side effects:** InstanceLifecycleService.markBrandingCompleted + CachingBrandingPackageProxy.evict
- **No fallback:** if this fails, saga aborts (can't deploy without lifecycle transition)

### UC-AGENT-08: Fair Dispatch — Tier-aware AI job enqueue (Wave 3 Phase 1)
- **Actor:** Branding controller / TenantProvisioningSaga gọi `AIQueueDispatcher.dispatch(priority, payload)`
- **Precondition:** `ai.queue.fair-queue-enabled=true` (BR-QUEUE-001), `payload.instanceId` set
- **Steps:**
  1. Caller resolve subscription tier → `AIJobPriority.fromTier(tier)` (BR-QUEUE-013 — null/unknown → FREE)
  2. Dispatcher set `payload.enqueuedAt = Instant.now()` (cần cho metric `ai.job.wait.time`)
  3. Dispatcher publish via direct exchange `ai.request.exchange` với routing key `ai.request.{tier}` (BR-QUEUE-014)
  4. Increment counter `ai.queue.dispatched{tier, mode=fair}`
- **Postcondition:** Job nằm trong queue tương ứng (`ai.request.enterprise/pro/free`); consumer pick theo weight (BR-QUEUE-002..004)
- **Errors:** broker unreachable → counter `ai.queue.dispatch.failed{tier}` + rethrow RuntimeException
- **Backward compat:** nếu `fair-queue-enabled=false`, dispatcher publish về legacy `branding-jobs` queue (tag `mode=legacy`)

### UC-AGENT-09: Concurrency Cap Reached → NACK + Redeliver
- **Actor:** `AIJobConsumer` (one of 3 tier listeners)
- **Trigger:** `DistributedRateLimiter.tryAcquireConcurrencySlot(instanceId, cap)` returns false (instance đã có ≥`cap` jobs in-flight)
- **Steps:**
  1. Consumer compute cap theo tier (BR-QUEUE-005..007: free=1, pro=3, enterprise=10)
  2. Acquire slot fail → log INFO + counter `ai.job.outcome{tier, outcome=concurrency_limited}`
  3. Throw `ConcurrencyLimitedException` → Rabbit NACK + redeliver theo `spring.rabbitmq.listener.simple.retry` (3 attempts, 1s → 2s → 4s) (BR-QUEUE-012)
- **Postcondition:** Job KHÔNG bị drop; sẽ retry tới khi slot available hoặc DLQ sau max-attempts
- **Why:** giới hạn cost AI provider per tenant + tránh 1 instance Enterprise tiêu hết Bulkhead

### UC-AGENT-10: Backpressure — Free Tier Degrade to Template Fallback
- **Actor:** `AIJobConsumer.consumeFree`
- **Trigger:** `BacklogInspector.enterpriseBacklog() > 50` (BR-QUEUE-011) AND incoming job tier = FREE
- **Steps:**
  1. Consumer detect backlog vượt threshold
  2. Log WARN "Free tier degraded to template fallback"
  3. Counter `ai.job.outcome{tier=free, outcome=degraded}`
  4. Skip AI processing (return) — caller's controller layer đã trả template response cho user TRƯỚC khi enqueue (degraded path = drop AI work, không user-visible failure)
- **Postcondition:** Free job marked degraded; Enterprise queue được ưu tiên flush
- **Recovery:** khi `enterpriseBacklog() ≤ 50`, free jobs xử lý normal trở lại (no manual intervention)
- **FE Behavior:** none — degraded happens silently after FE đã nhận template fallback

### UC-AGENT-11: Circuit Breaker Opens Around AI Provider
- **Actor:** `kitehub-branding/client/ResilientAIClient` (Decorator wrapping the configured Ollama/OpenAI delegate via `@Qualifier("aiClient")`)
- **Trigger:** Last 20 calls (BR-QUEUE-017) có failure rate ≥ 50% (BR-QUEUE-015), tối thiểu 10 calls (BR-QUEUE-018)
- **Steps:**
  1. Resilience4j circuit breaker `ai-provider` transition CLOSED → OPEN
  2. 30s wait duration (BR-QUEUE-016) — mọi call ngay lập tức fail-fast
  3. `ResilientAIClient` fallback methods kick in:
     - `analyzeLogo` → template-safe `LogoAnalysis` (primaryColor #2563EB, theme MODERN, `rawAnalysis` tagged "Fallback")
     - `generateImage` → `https://placehold.co/{size}/2563EB/white?text=Template`
     - `generateText` → default Vietnamese copy
  4. Sau 30s: HALF_OPEN, thử 1 call → CLOSED nếu pass, OPEN nếu fail
- **Postcondition:** Branding pipeline tiếp tục qua template path; user không thấy 5xx errors
- **Metrics:** `resilience4j.circuitbreaker.calls`, `.state` (Micrometer auto-published)
- **Scope:** `kitehub-branding` wraps via `ai-provider` CB (GAP-148); `kiteclass-core/ResilientAIClient` uses the separate `ai` CB (BR-AGENT-001) — same thresholds, different instance key, different service.

## Log
- 2026-04-21 — GAP-148 (Wave 9-D): UC-AGENT-11 Actor updated to reference `kitehub-branding/client/ResilientAIClient` (previously only kiteclass-core's wrapper existed — kitehub-branding config was dead).
- 2026-04-19 — GAP-104: thêm UC-AGENT-08 (fair dispatch), UC-AGENT-09 (concurrency cap NACK), UC-AGENT-10 (backpressure degrade), UC-AGENT-11 (circuit breaker open). Source: `AIQueueDispatcher`, `AIJobConsumer`, `BacklogInspector`, `application.yml:83-91`.
- 2026-04-14 — Initial UCs
