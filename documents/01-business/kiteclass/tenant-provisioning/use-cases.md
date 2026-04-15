# Tenant Provisioning — Use Cases

### UC-PROV-01: Happy Path Provisioning
- **Actor:** KiteHub onboarding service (publishes TenantCreatedEvent)
- **Sequence:**
  1. Saga receives TenantCreatedEvent(tenantId, slug, audience, tone)
  2. `lifecycle.initiate` → instance id=N, status=INITIALIZING
  3. `provisionInfrastructure` (stub logs; future: real infra calls)
  4. `lifecycle.markInfrastructureReady(N)` → status=GENERATING
  5. `analyzer.analyze(request)` → AnalysisResult (or templateOnly fallback)
  6. `planner.plan(analysis)` → Plan (3 steps: extract-palette, pick-template, publish-package)
  7. `executor.execute(plan, ctx)` — last Step transitions instance to DEPLOYED and evicts package cache
- **Postcondition:** instance DEPLOYED; outbox events emitted end-to-end

### UC-PROV-02: Initiate Fails (Slug Conflict)
- **Trigger:** `lifecycle.initiate` throws because slug already used
- **Result:** Saga rethrows; NO instance row exists, so no compensation needed
- **Caller:** sees 400 from API layer

### UC-PROV-03: Plan Step Fails Without Fallback
- **Trigger:** PlanExecutor rethrows StepException (e.g. PublishPackageStep failure)
- **Steps:**
  1. Saga catches StepException
  2. Calls `lifecycle.markFailed(instanceId, stepException.getMessage())`
  3. Rethrows — caller sees failure + instance row shows FAILED with reason
- **Notes:** instance stays in DB for retry (BR-INST-003)

### UC-PROV-04: Analyzer Throws Unexpected Runtime
- **Trigger:** ResilientAIClient fallback has a bug; analyzer itself throws
- **Steps:**
  1. Saga catches RuntimeException
  2. Calls `markFailed` with exception message
  3. Rethrows
- **Operational:** alert on `instance.failed` events with unexpected messages

### UC-PROV-05: Retry After Failure
- **Actor:** Admin / scheduled retry (future)
- **Preconditions:** instance.status=FAILED, retryCount < MAX_RETRIES
- **Steps:**
  1. Caller invokes `lifecycle.retry(instanceId)` → status=INITIALIZING (via existing lifecycle)
  2. Saga re-invoked with the SAME TenantCreatedEvent parameters (stored elsewhere or reconstructed)
  3. Flow proceeds as UC-PROV-01 from step 3 onward
- **Notes:** explicit retry wiring lands in follow-up (scheduled job needs parameter persistence)

### UC-PROV-06: Templates Only (AI Provider Down)
- **Trigger:** ResilientAIClient circuit breaker open
- **Steps:**
  1. Analyzer returns `AnalysisResult.templateOnly()`
  2. Planner still produces Plan (description flags it)
  3. Steps detect templateOnly and skip AI-heavy work
  4. Instance still reaches DEPLOYED via template-only path
- **Postcondition:** tenant sees branded instance; FULL_AI generation retry can happen later via `rebrand`

## Log
- 2026-04-14 — Initial UCs
