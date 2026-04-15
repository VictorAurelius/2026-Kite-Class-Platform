# AI Agent Workflow — API Contract

> Internal Java SPI (no REST endpoints — workflow is server-side orchestration).

## Step interface

```java
public interface Step {
    String name();
    void execute(StepContext context);

    default boolean hasFallback() { return false; }
    default void fallback(StepContext context) { throw new UnsupportedOperationException(); }
}
```

## StepContext

```java
class StepContext {
    Long instanceId;        // final
    String tenantId;        // final
    AnalysisResult analysis;
    Map<String, Object> attributes;  // via put/get/has
    List<String> executedSteps;      // appended by PlanExecutor
}
```

## Plan (Composite)

```java
@Value public class Plan {
    String description;
    List<Step> steps;
}
```

## Services

### AnalyzerService.analyze(AnalysisRequest) → AnalysisResult
Delegates to ResilientAIClient — on fallback returns AnalysisResult.templateOnly().

### PlannerService.plan(AnalysisResult) → Plan
Deterministic; current scaffold returns 3-step plan:
`ExtractPaletteStep → PickTemplateStep → PublishPackageStep`

### PlanExecutor.execute(Plan, StepContext)
Runs steps in declared order. Transactional. Emits outbox events. Throws StepException on unrecovered failure.

## Built-in Steps (current catalogue)

| Name | Reads (ctx) | Writes (ctx) | Fallback? |
|------|-------------|--------------|:---------:|
| extract-palette | analysis | palette | ✓ |
| pick-template | palette | template-id | ✓ |
| publish-package | template-id | — (side effects only) | — |

Async generation Steps (GenerateLogoStep / GenerateBannerStep / ComposeThemeStep) land in a follow-up Sub-PR that adds RabbitMQ queue integration.

## Outbox events

| Event | aggregateType | aggregateId |
|-------|---------------|-------------|
| ai.plan.started / completed / failed | BrandingPlan | instanceId |
| ai.step.completed / fallback | BrandingPlan | instanceId |

## Log
- 2026-04-14 — Initial contract
