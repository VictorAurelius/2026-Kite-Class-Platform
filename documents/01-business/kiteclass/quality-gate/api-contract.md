# Quality Gate — API Contract

> Internal Java SPI. No REST endpoints in Sub-PR 4.5.

## QualityCheck interface

```java
public interface QualityCheck {
    String name();
    Result run(FrontendInstance instance);

    @Value @Builder
    class Result {
        String checkName;
        int score;          // 0–100
        boolean passed;
        String detail;      // null on pass

        static Result pass(String name, int score);
        static Result fail(String name, int score, String detail);
    }
}
```

Built-in implementations:
| Bean | Name |
|------|------|
| `ContrastQualityCheck` | `wcag-contrast` |
| `CssVarsQualityCheck` | `css-vars-applied` |
| `AssetUrlsQualityCheck` | `asset-urls-reachable` |
| `VisualRegressionQualityCheck` | `visual-regression` |
| `LogoPlacementQualityCheck` | `logo-placement` |

## InstanceQualityReviewer

```java
@Transactional
QualityReport review(Long instanceId);
```

- Runs all `QualityCheck` beans; aggregates arithmetic mean
- Persists `QualityReport` + writes `AuditLog`
- Returns the saved report (caller checks `getPassed()`)

Config: `quality-gate.pass-threshold` (default 70).

## QualityReviewStep (Step interface — agent workflow)

```java
String name() → "quality-review";
void execute(StepContext) throws StepException;
boolean hasFallback() → false;
```

- Slots between `pick-template` and `publish-package` in `PlannerService`.
- On failure throws `StepException` with report id + score; saga compensation handles FAILED transition.

## Schema

See `rules.md` §5 reference checks + V39 migration for the `quality_reports` table.

## Log
- 2026-04-14 — Initial contract
