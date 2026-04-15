# AI Agent Workflow — Business Rules

**Domain:** ai-agent-workflow
**Source:** GAP-008, Wave 3 Sub-PR 3.5, ADR-006

## Rules

### Step contract
| ID | Rule |
|----|------|
| BR-STEP-001 | Step.execute() MUST throw StepException on failure (no silent returns) |
| BR-STEP-002 | Step.name() is unique across the deployed catalogue — used in logs, metrics, outbox events |
| BR-STEP-003 | Step reads/writes StepContext via explicit constants (e.g. KEY_PALETTE) documented in javadoc |
| BR-STEP-004 | Heavy work (image gen) MUST enqueue async (ai-branding-guidelines.md §3.3); execute() returns promptly |
| BR-STEP-005 | fallback() runs only when hasFallback() returns true; caller catches second failure as saga abort |

### PlanExecutor (Saga orchestration)
| ID | Rule |
|----|------|
| BR-EXEC-001 | Steps run strictly in declared order — no parallelism within a single plan |
| BR-EXEC-002 | On StepException + hasFallback → call fallback(), continue |
| BR-EXEC-003 | On StepException + no-fallback → emit ai.plan.failed + rethrow (aborts plan) |
| BR-EXEC-004 | Outbox events emitted: ai.plan.{started,completed,failed}, ai.step.{completed,fallback} |
| BR-EXEC-005 | Entire execute() runs in one @Transactional — all outbox rows commit atomically with domain changes |

### Analyzer / Planner
| ID | Rule |
|----|------|
| BR-AGENT-001 | Analyzer MUST go through ResilientAIClient (auto-primary) — CB + Bulkhead + Retry applied |
| BR-AGENT-002 | Planner honors AnalysisResult.templateOnly — plans produced regardless, Steps handle the template-only path |
| BR-AGENT-003 | Planner output is deterministic given same AnalysisResult (no random step selection) |

## Event catalogue

| Event | Trigger |
|-------|---------|
| ai.plan.started | PlanExecutor enters execute() |
| ai.step.completed | Step execute() returned normally |
| ai.step.fallback | Step execute() threw, fallback() recovered |
| ai.plan.completed | All steps done |
| ai.plan.failed | Step threw without fallback OR fallback also failed |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| (Resilience4j inherits from ai-provider config) | — | CB/Bulkhead/Retry for AI calls |

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.5, ADR-006)
