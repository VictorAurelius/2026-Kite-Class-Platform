# GAP-046: Apply Design Patterns Systematically

**Status:** 🟡 PLANNED (Wave 1 Sprint 0)
**Branch:** wave/01-foundation
**Priority:** 🟠 P1 (foundation quality — affects all implementations)
**Domain:** Architecture / Engineering
**Detected:** 2026-04-14 (user raised)
**Related Docs:**
- `documents/02-architecture/ai-branding-design-patterns.md` (catalog)

## Problem

Current v2 design sử dụng patterns implicit but **không systematic**:

- AIClient có interface nhưng chưa explicit Strategy Pattern
- FrontendInstanceStatus transitions scattered if/switch thay vì State Pattern
- Steps pipeline chưa Command + Composite
- AIBrandingService là "God Service" (anti-pattern)
- Không có Outbox cho reliable events
- Không Saga cho distributed provisioning
- Không Circuit Breaker/Bulkhead cho resilience

→ Code sẽ khó maintain, test, extend khi codebase grow.

## Proposed Fix

Áp dụng 17 patterns documented trong pattern catalog, theo 5 phases:

### Phase 1 — Foundation (P1):
1. **Strategy Pattern** — AIClient interface (mostly done, formalize)
2. **State Pattern** — InstanceStateMachine (refactor status transitions)
3. **Command Pattern** — Steps as Command objects
4. **Facade Pattern** — BrandingFacade thay cho God Service

### Phase 2 — Resilience (P1):
5. **Circuit Breaker + Bulkhead** — Resilience4j integration
6. **Outbox Pattern** — Reliable event publishing
7. **Chain of Responsibility** — Resource routing chain

### Phase 3 — Enterprise (P2):
8. **Saga Pattern** — Distributed provisioning txn
9. **Repository + Aggregate** — DDD domain model
10. **Adapter + ACL** — Isolate external AI vendors

### Phase 4 — Frontend (P1):
11. **XState State Machine** — Wizard flow
12. **Compound Components** — Flexible wizard composition

### Phase 5 — Migration (P0 when ramp up):
13. **Strangler Fig** — v1 → v2 transition

### Optional advanced:
14. **Event Sourcing** — Version history (alternative to snapshots)
15. **CQRS** — Read/write separation (if scale issues)
16. **Decorator** — Step cross-cutting concerns
17. **Builder** — Complex object construction

## Benefits

- ✅ **Testability** — Mock interfaces instead of integration tests
- ✅ **Replaceability** — Swap AI provider/template engine without big refactor
- ✅ **Clarity** — Standard vocabulary across team
- ✅ **Scalability** — Proven solutions for distributed systems
- ✅ **Reliability** — Outbox + Saga + Circuit Breaker = robust
- ✅ **Maintainability** — Change locality (one concept = one place)

## Anti-Patterns to Remove

Current code smells:
- ❌ God Service: AIBrandingService + AIBrandingProcessor (break into smaller)
- ❌ Primitive Obsession: String colors (use ThemeColor value object)
- ❌ Leaky Abstraction: Ollama-specific types in domain (wrap with adapter)
- ❌ Direct RabbitMQ publish (use Outbox)
- ❌ Status transitions via if/switch (use State Pattern)

## Implementation Guidelines

**Do:**
- Document pattern choice trong javadoc (e.g., `// Strategy Pattern`)
- Use standard names (AIClientStrategy, InstanceState, Step, BrandingFacade)
- Test each pattern component in isolation
- Follow SOLID principles

**Don't:**
- Over-engineer — don't apply patterns just to apply them
- Use patterns for trivial cases (YAGNI)
- Mix patterns poorly (e.g., Singleton + Strategy → hard to swap)

## Acceptance Criteria

- [ ] Pattern catalog doc published (`ai-branding-design-patterns.md`) ✓ Done
- [ ] Each GAP-007..015 implementation references relevant pattern
- [ ] Code review checklist includes pattern verification
- [ ] Developer guidelines updated (`.claude/rules/ai-branding-guidelines.md`)
- [ ] Refactor plan cho existing anti-patterns (God Service, primitive obsession)
- [ ] Training: team review pattern catalog together
- [ ] Quality audit section: "Patterns applied correctly?"

## Dependencies

- Informs all other GAPs (cross-cutting)
- Requires team alignment on patterns (architecture review)

## Log

- 2026-04-14 — User raised: should apply design patterns for optimization
