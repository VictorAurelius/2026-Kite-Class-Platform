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

## Naming

`ADR-NNN-short-kebab-title.md` — 3-digit sequential.

## Status Transitions

```
PROPOSED → ACCEPTED → (time passes) → DEPRECATED → SUPERSEDED
```

Never edit accepted ADRs — create new ADR that supersedes.
