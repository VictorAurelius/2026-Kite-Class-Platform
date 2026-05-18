# ADR-020: Vendor Client Package Naming — Accept `client/` as Adapter Convention

**Status:** ACCEPTED
**Date:** 2026-04-26
**Deciders:** @nguyenvankiet (solo-dev)
**Related Gap(s):** GAP-046 (apply design patterns systematically)
**Wave:** 6 closure (Sub-PR 6.5)

## Context

`design-patterns.md` Mandatory Patterns table requires "**Adapter + ACL**" for "External API vendor-specific" code. The convention literally names the pattern *Adapter*, suggesting the package should be `adapter/`.

**Reality on the ground (2026-04-26 Wave 6 audit baseline):** All vendor-API isolation code in this codebase lives in `client/` packages, not `adapter/`:

```
kitehub-branding/.../client/OllamaClient.java         (Ollama adapter, but named "Client")
kitehub-subscription/.../client/EmailServiceClient.java (HTTP-side calls, RabbitMQ-side bypass)
kiteclass-gateway/.../client/...                       (downstream service clients — historical; service removed Wave 96 per ADR-032)
```

Sub-PR 6.1 design-pattern-audit Cat 4 (Vendor Leak) flagged `OllamaClient` because the calibrated grep pattern only excluded `/adapter/` paths. This was flagged as a **false-positive** during baseline review — the class genuinely isolates Ollama types from the domain layer; the package name is the only "wrong" thing.

Two ways to fix the false-positive:
- **Option 1:** Rename `client/` → `adapter/` everywhere (~5-10 files moved across modules)
- **Option 2:** Update detector to accept `/client/` as adapter convention; document the convention

## Decision

**We accept `/client/` as the adapter convention.** No package rename. Detector calibrated to skip `/client/` paths in Cat 4 vendor-leak detection (Sub-PR 6.4).

## Consequences

### Positive
- **Zero code churn** — no package moves, no import path changes across 5+ files in 3 modules
- **Honors existing convention** — every existing service that wraps an external API was named `*Client` (Ollama, EmailService, historical gateway clients). Renaming creates a temporary inconsistency until all are migrated, while accepting the convention is consistent immediately
- **Detector now accurate** — Sub-PR 6.4 calibration baked the `/client/` allowance into `anti-pattern-detectors.md`; future audits won't re-flag this
- **Aligns with Spring ecosystem** — Spring's `@FeignClient`, `RestClient`, `WebClient`, `RestTemplate` all use "Client" naming for vendor API wrappers. Java/Spring devs read `OllamaClient` correctly without translation

### Negative
- **Naming asymmetry vs `design-patterns.md`** — the rule talks about "Adapter pattern" but the codebase uses "Client" packages. Mitigated by:
  - Adding cross-reference note in `design-patterns.md` Mandatory Patterns table next time it's edited (deferred to next rule update; not blocking)
  - This ADR serves as the canonical reference for "client = adapter in our project"
- **New devs** may briefly look for `adapter/` packages before finding `client/` — single-file orientation cost

### Neutral
- The pattern *itself* (Adapter / Anti-Corruption Layer) still applies — vendor types stay inside `client/` packages, domain types cross the boundary. Only the package name differs from textbook GoF naming.

## Alternatives considered

### Option 1 — Rename `client/` → `adapter/`
Rejected because:
- 5+ files across 3 modules would need package moves
- Every consumer's import statement updates (~15-30 import lines)
- Temporary inconsistency window (multi-PR migration) exposes risk of partial-state confusion
- No architectural benefit — the isolation is identical regardless of package name
- Spring ecosystem precedent (`@FeignClient`, `*Client`) makes "Client" idiomatic for the role

### Option 3 — Hybrid (rename only for new vendor types, leave existing)
Rejected because:
- Worst of both worlds — convention drift over time
- Future audits would need to track which is which
- Violates principle of one canonical convention per project

## Implementation

Already complete (Sub-PR 6.4):
- `anti-pattern-detectors.md` Cat 4: `grep -vE '/(adapter|client|client/external|integration/ollama)/'`
- Calibration note documents the architectural choice
- Sub-PR 6.5 audit re-run: Cat 4 score 16 → 20 (no false-positives)

No code changes required by this ADR.

## Future review

Re-evaluate this decision if:
- Spring releases a new dominant naming convention that shifts ecosystem consensus
- Project grows multi-team and a new team independently chooses `adapter/` (sign of convention drift)
- A new code-intelligence skill (e.g., GitNexus per GAP-221) gives stronger enforcement options

## Related

- Rule: `.claude/rules/design-patterns.md` Mandatory Patterns table (External API → Adapter + ACL)
- Skill calibration: `.claude/skills/quality/design-pattern-audit/reference/anti-pattern-detectors.md` Cat 4
- Audit: `documents/04-quality/audits/design-patterns/audit-2026-04-26-closure.md` §Cat 4 Verdict
- Wave: Wave 6 plan §2.2 closure checklist (this ADR closes the deferred decision)
