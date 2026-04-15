# Quality Gate — Use Cases

### UC-QG-01: Passing Review
- **Actor:** PlanExecutor at `QualityReviewStep`
- **Steps:**
  1. Step invokes `InstanceQualityReviewer.review(instanceId)`
  2. Reviewer runs all `QualityCheck` beans, aggregates arithmetic mean
  3. score ≥ threshold → passed=true
  4. Report persisted + AuditLog `quality.review.passed`
  5. Context gets `quality-report-id` + `quality-score`; step returns normally
- **Result:** pipeline continues to `PublishPackageStep` → DEPLOYED

### UC-QG-02: Failing Review
- **Trigger:** score < threshold
- **Steps:**
  1. Reviewer still persists report (passed=false) + audit `quality.review.failed`
  2. Step throws StepException with report id + score
  3. PlanExecutor emits `ai.plan.failed` (no fallback on QualityReviewStep)
  4. TenantProvisioningSaga compensation: `lifecycle.markFailed(id, reason)`
- **Result:** instance FAILED; retry path available via existing lifecycle

### UC-QG-03: Admin Review Inspection
- **Actor:** Ops dashboard (future GAP-067 Admin Console)
- **Query:** `QualityReportRepository.findByTargetInstanceIdOrderByCreatedAtDesc(instanceId)`
- **Purpose:** see history of reviews + per-check scores

### UC-QG-04: Calibration (Future)
- **Actor:** Quality lead
- **Scope:** adjust `quality-gate.pass-threshold` based on observed failure rate
- **Metrics:** `QualityReportRepository.countByPassedAndDeletedFalse(true/false)` over time
- **Wave:** GAP-029 calibration (deferred)

### UC-QG-05: Re-review After Rebrand
- **Trigger:** tenant triggers rebrand → new plan runs → QualityReviewStep runs → NEW report row
- **Invariant:** reports are append-only (BR-QG-008); historical records preserved

## Log
- 2026-04-14 — Initial UCs
