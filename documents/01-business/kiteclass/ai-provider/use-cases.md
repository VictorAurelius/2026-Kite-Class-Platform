# AI Provider — Use Cases

### UC-AI-01: Analyze Brand Context
- **Actor:** Planner / Analyzer services (Wave 3 Sub-PR 3.5)
- **Input:** logo bytes + audience + tone + context map
- **Steps:**
  1. Caller constructs `AnalysisRequest`
  2. Calls `resilientAIClient.analyze(req)`
  3. ResilientAIClient invokes Resilience4j wrapper → delegate provider
  4. Provider runs multimodal analysis → returns `AnalysisResult`
- **Happy path:** returns palette + typography + mood tags
- **Fallback path:** Circuit open / retries exhausted → `AnalysisResult.templateOnly()` signals downstream TEMPLATE-only routing

### UC-AI-02: Generate Branded Asset
- **Actor:** AIResourceHandler (Wave 3 Sub-PR 3.3) or PlanExecutor Step
- **Input:** composed prompt + resourceType + dimensions
- **Steps:**
  1. Caller constructs `GenerationRequest`
  2. Calls `resilientAIClient.generate(req)`
  3. Provider generates image → returns `GenerationResult` with URL or bytes
- **Fallback path:** returns `GenerationResult.templateFallback()` → caller routes to template handler instead

### UC-AI-03: Circuit Breaker Trip
- **Actor:** System (during incident)
- **Preconditions:** 50%+ of last 20 calls failed
- **Steps:**
  1. Circuit transitions CLOSED → OPEN
  2. Next 30s of calls hit fallback directly (no provider invocation)
  3. After 30s, 3 probe calls allowed (HALF_OPEN)
  4. All 3 succeed → CLOSED; any fail → OPEN
- **Postcondition:** Provider protected from overload; tenant sees template-only branding until recovery

### UC-AI-04: Swap Provider via Profile
- **Actor:** DevOps / deployment
- **Steps:**
  1. Set `SPRING_PROFILES_ACTIVE=ai-live` (enables OllamaAIClient) or leave unset (MockAIClient)
  2. Restart service
  3. Resilient wrapper auto-picks new delegate via `@Qualifier("baseAIClient")`
- **Postcondition:** Different provider active without domain code changes

### UC-AI-05: Tests Use Mock
- **Actor:** Developer / CI
- **Steps:**
  1. Default profile (no `ai-live`) selects `MockAIClient`
  2. Deterministic fixture data returned — no network I/O, no cost
  3. Tests assert on known output values
- **Postcondition:** Test suite fast + reliable

## Log
- 2026-04-14 — Initial UCs
