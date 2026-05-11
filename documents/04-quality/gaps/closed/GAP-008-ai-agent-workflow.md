# GAP-008: AI Agent Workflow (planner + executor) thay cho direct generator

**Status:** 🟢 DONE (Wave 3 Sub-PR 3.5, merged 2026-04-14; Analyzer/Planner/PlanExecutor + 3 scaffold Steps + outbox events landed. Async Generate{Logo,Banner}Step + ComposeThemeStep deferred to 3.5b follow-up.)
**Priority:** 🟠 P1
**Domain:** AI / Backend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md` §3

## Problem

Code hiện tại: **Direct AI generator** — user request → AIClient call → result. Không có orchestration, planner, hay multi-step reasoning. Điều này gây:

- AI luôn được gọi dù không cần (tốn compute + $)
- Không consistent (AI output variable)
- Không có fallback khi AI fail
- Không debuggable (black box)

## Evidence

- `AIBrandingService.java` (line 20-106): 3 methods direct calls (`analyzeLogo`, `generateHeroImage`, `generateMarketingCopy`)
- `AIBrandingProcessor.java` (line 24-124): hardcoded 7 steps với sleep delays (MVP simulate)
- **Không có**: Agent, Workflow, Planner, PlanExecutor, Step classes
- **Không có**: decision logic "dùng template hay AI"

## Proposed Fix

### Pattern: Analyzer → Planner → Executor

```java
// Step 1: Extract structured context
@Service
public class BrandingAnalyzer {
  public BrandingContext analyze(ResourceRequest req, TenantId tid) {
    // Load: uploaded logo, existing colors, tenant tier, audience
    // Use AI vision (only for logo analysis if logo uploaded)
    return new BrandingContext(colors, logo, audience, tier);
  }
}

// Step 2: Plan strategy
@Service
public class BrandingPlanner {
  public ExecutionPlan plan(BrandingContext ctx, ResourceRequest req) {
    List<Step> steps = new ArrayList<>();

    if (req.useTemplate()) {
      steps.add(new FetchTemplateStep(req.templateId));
      steps.add(new ComposeColorsStep(ctx.colors));
      steps.add(new GenerateHeadlineStep(ctx));  // text AI
      steps.add(new RenderSVGStep());
    } else {
      steps.add(new GenerateImageAIStep(ctx, req));  // image AI
      steps.add(new PostProcessStep());
    }

    steps.add(new ValidateOutputStep());  // contrast, size, brand
    steps.add(new StoreStep());
    return new ExecutionPlan(steps);
  }
}

// Step 3: Execute plan with retry/fallback
@Service
public class PlanExecutor {
  public BrandingResource execute(ExecutionPlan plan) {
    StepContext ctx = new StepContext();
    for (Step step : plan.steps) {
      try {
        step.execute(ctx);
      } catch (Exception e) {
        if (step.hasFallback()) step.fallback(ctx);
        else throw e;
      }
    }
    return ctx.result;
  }
}
```

### Step interface

```java
public interface Step {
  String name();
  void execute(StepContext ctx);
  default boolean hasFallback() { return false; }
  default void fallback(StepContext ctx) { throw new UnsupportedOperationException(); }
}

// Ví dụ: Generate image AI step có fallback sang template
public class GenerateImageAIStep implements Step {
  public void execute(StepContext ctx) {
    ctx.image = aiClient.generateImage(ctx.prompt);
  }
  public boolean hasFallback() { return true; }
  public void fallback(StepContext ctx) {
    ctx.image = templateService.getDefault(ctx.resourceType);
  }
}
```

### Benefits table

| Criterion | Current (Direct) | Proposed (Agent) |
|-----------|------------------|------------------|
| Control | Low | High |
| Cost (80% template path) | Full AI cost | Near-zero |
| Latency (template path) | 30s-5min | <3s |
| Consistency | Variable | Brand-enforced |
| Debug | Black box | Traceable steps |
| Fallback | Error | Graceful |

## Acceptance Criteria

- [ ] `BrandingContext`, `ExecutionPlan`, `Step`, `StepContext` classes created
- [ ] `BrandingAnalyzer`, `BrandingPlanner`, `PlanExecutor` services
- [ ] 10+ concrete Step implementations (FetchTemplate, ComposeColors, GenerateAI, etc.)
- [ ] Unit tests: each step isolated, mock dependencies
- [ ] Integration test: full pipeline template path <3s, AI path <30s
- [ ] Metrics: `branding_step_duration_seconds{step}`, `branding_fallback_total{step}`
- [ ] Existing `AIBrandingService` refactored to use new pipeline

## Dependencies

- **Blocked by GAP-007** (resource classification) — planner cần category để decide
- Integrates with GAP-004 (template gallery)

## Log

- **2026-04-26** — **Governance closure tracked: [GAP-225](GAP-225-scaffolded-as-done-governance-closure-umbrella.md)** (Scaffolded-as-DONE Governance Closure Umbrella). Scaffold debt: "Async Generate{Logo,Banner}Step + ComposeThemeStep deferred to 3.5b follow-up". Status preserved 🟢 DONE for audit trail; cross-gap audit identified this gap as part of Cluster C1 (AI Agent + Async Pipeline) — needs dedicated `quality/ai-agent-review/` skill + audit-gate rule + matrix row when scheduled. No code change this PR — docs truth-up only.
- 2026-04-14 — Created from AI Branding redesign §3
