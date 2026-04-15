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

## Log
- 2026-04-14 — Initial UCs
