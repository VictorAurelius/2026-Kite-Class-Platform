# Resource Handlers — Use Cases

### UC-HND-01: Route Request Through Pipeline

- **Actor:** `ResourceRoutingService.route(req, ctx)` caller (PlanExecutor, etc.)
- **Steps:**
  1. Run classification chain → returns `ResourceCategory`
  2. Look up matching handler from category→handler map
  3. Call `handler.handle(req, ctx)` → `HandlerResult`
  4. If `FALLBACK` → invoke `FallbackHandler.rescue(req)`
  5. Return final `HandlerResult`
- **Outcome:** caller receives READY (resource available now) / PENDING (job enqueued) / rescue-resolved READY

### UC-HND-02: Serve Static Asset

- **Actor:** `StaticResourceHandler` (category STATIC)
- **Steps:**
  1. Query repository for existing STATIC resource of requested type
  2. Found → READY with entity
  3. Not found → FALLBACK (classifier was optimistic; rescue to default)

### UC-HND-03: Compose via Template

- **Actor:** `TemplateResourceHandler` (category TEMPLATE)
- **Steps:**
  1. Query repository for existing TEMPLATE row
  2. Found → READY (reuse composed output)
  3. Not found → PENDING with jobId `template-compose-pending` (compose engine lands in follow-up PR)

### UC-HND-04: Invoke AI Generation

- **Actor:** `AIResourceHandler` (category FULL_AI)
- **Steps:**
  1. Compose internal prompt (backend-only; free-form user prompt forbidden outside Enterprise Advanced Mode)
  2. Build `GenerationRequest` with type-specific width/height
  3. Call `resilientAIClient.generate(req)` — Circuit Breaker + Bulkhead + Retry + fallback all apply
  4. If `templateFallback=true` → return FALLBACK (routing escalates to rescue)
  5. Else → PENDING with jobId `ai-job-pending` (full async queue integration in Sub-PR 3.5)

### UC-HND-05: Rescue via Fallback

- **Actor:** `FallbackHandler.rescue`
- **Trigger:** any handler returned FALLBACK, OR no handler registered for resolved category
- **Steps:**
  1. Look up default TEMPLATE resource of requested type
  2. Found → READY with default
  3. Not found → PENDING with jobId `seed-default-template-pending` (ops must seed defaults)
- **Invariant:** FallbackHandler is the terminal — never returns FALLBACK itself

## Log
- 2026-04-14 — Initial UCs
