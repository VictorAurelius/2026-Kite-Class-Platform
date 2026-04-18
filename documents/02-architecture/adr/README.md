# Architecture Decision Records (ADRs)

Lightweight records của significant architecture decisions.

## When to write ADR

- Choosing between ≥2 viable alternatives (database, pattern, framework)
- Design affects >1 service/module
- Decision hard to reverse (schema, API contract, integration)
- Cross-cutting concerns (security, performance, deployment)

## When NOT to write

- Trivial implementation details
- Easy-to-reverse choices
- Already covered by existing rules/patterns

## Format

Use `_TEMPLATE.md`. Follow Michael Nygard's format (Context → Decision → Consequences).

## Index

| ADR | Title | Status | Date |
|-----|-------|:------:|------|
| [001](ADR-001-k12-data-model.md) | K-12 Multi-Subject Data Model | ACCEPTED | 2026-04-14 |
| [002](ADR-002-academic-year-structure.md) | Academic Year + Semester Structure | ACCEPTED | 2026-04-14 |
| [003](ADR-003-role-hierarchy.md) | Hierarchical Role-Based Access Control | ACCEPTED | 2026-04-14 |
| [004](ADR-004-instance-lifecycle.md) | Frontend Instance Provisioning Lifecycle | ACCEPTED | 2026-04-14 |
| [005](ADR-005-resource-classification.md) | Resource Classification Pipeline | ACCEPTED | 2026-04-14 |
| [006](ADR-006-ai-agent-orchestration.md) | AI Agent Orchestration (Analyzer → Planner → Executor) | ACCEPTED | 2026-04-14 |
| [007](ADR-007-outbox-pattern-for-events.md) | Outbox Pattern for Reliable Event Publishing | ACCEPTED | 2026-04-14 |
| [008](ADR-008-resilience-for-ai-calls.md) | Resilience for External AI Calls (Circuit Breaker + Bulkhead + Retry) | ACCEPTED | 2026-04-14 |
| [009](ADR-009-branding-package-api.md) | Branding Package Composite API | ACCEPTED | 2026-04-14 |
| [010](ADR-010-content-moderation-policy.md) | Content Moderation Policy (Staged Review) | ACCEPTED | 2026-04-14 |
| [011](ADR-011-defense-in-depth-security.md) | Defense-in-Depth Security (Validators + Output-Encoders + CSP) | ACCEPTED | 2026-04-14 |
| [012](ADR-012-dmca-trademark-workflow.md) | DMCA / Trademark Workflow | ACCEPTED | 2026-04-14 |
| [013](ADR-013-data-retention-classification.md) | Data Retention Classification (GDPR + VN Compliance) | ACCEPTED | 2026-04-14 |
| [014](ADR-014-async-jobs-queue-over-batch.md) | Async Jobs Queue (RabbitMQ) over Batch Framework | ACCEPTED | 2026-04-18 |
| [015](ADR-015-aws-agent-plugins-evaluation.md) | AWS Agent Plugins Evaluation — Defer Adoption | ACCEPTED | 2026-04-18 |

## Naming

`ADR-NNN-short-kebab-title.md` — 3-digit sequential.

## Status Transitions

```
PROPOSED → ACCEPTED → (time passes) → DEPRECATED → SUPERSEDED
```

Never edit accepted ADRs — create new ADR that supersedes.

## Log

- **2026-04-18:** Added ADR-015 (AWS Agent Plugins defer, GAP-103).
- **2026-04-18:** Index backfilled (was showing only 5/13). Added ADR-014 capturing RabbitMQ-over-Batch decision retroactively (GAP-102 ADR kickoff).
- **2026-04-14:** ADRs 001-013 created (initial architecture documentation sweep).
