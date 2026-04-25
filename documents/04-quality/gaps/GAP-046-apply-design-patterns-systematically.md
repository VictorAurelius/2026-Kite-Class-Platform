# GAP-046: Apply Design Patterns Systematically

**Status:** 🟡 PARTIAL — rules + infrastructure shipped (catalog doc + `.claude/rules/design-patterns.md` + Outbox infra in `kiteclass-core/common/outbox/`); audit skill + hotspot refactors remain
**Branch:** wave/01-foundation (Wave 6 candidate for completion)
**Priority:** 🟠 P1 (foundation quality — affects all implementations); meta-boosted to top of GA-blocker queue per `meta-gap-priority.md`
**Domain:** Architecture / Engineering
**Detected:** 2026-04-14 (user raised)
**Related Docs:**
- `documents/02-architecture/ai-branding-design-patterns.md` (catalog)
- `.claude/rules/design-patterns.md` (rules + anti-pattern list + checklist)

## Current State (verified 2026-04-25)

| AC item | Status | Evidence |
|---------|:------:|----------|
| Pattern catalog doc | ✅ DONE | `documents/02-architecture/ai-branding-design-patterns.md` |
| `.claude/rules/design-patterns.md` rule + checklist | ✅ DONE | 7.6K rule with 17 patterns + anti-pattern BANNED list (§3) + PR review checklist (§4) |
| Outbox Pattern infra | ✅ DONE | `kiteclass-core/common/outbox/` — 7 classes (OutboxEvent / OutboxStatus / OutboxEventRepository / OutboxEventWriter / OutboxEventPublisher / EventDispatcher / LoggingEventDispatcher) + branding integration (`BrandingEventPublisher`) |
| Code review checklist references patterns | ✅ DONE | `.claude/rules/design-patterns.md` §4 + `.claude/skills/core/two-stage-code-review.md` |
| Each GAP-007..015 implementation references pattern | ⚠️ Cross-cutting — partially done, needs verification per gap | Audit-driven sweep needed |
| Quality audit "Patterns applied correctly?" section | ❌ NOT DONE | `grep "Patterns applied" .claude/skills/quality-audit` returns 0 matches |
| Refactor existing anti-patterns (God Service, primitive obsession) | ❌ NOT DONE | TrialToPaidService 546 LOC > 500 threshold (per design-patterns.md §3.1); InstanceService 496 LOC at threshold; ~95 status-switch sites across services pending State Pattern triage |
| Training | N/A | Solo-dev mode |

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

- [x] Pattern catalog doc published (`ai-branding-design-patterns.md`)
- [x] Code review checklist includes pattern verification (`design-patterns.md` §4 + `two-stage-code-review.md`)
- [x] Developer guidelines updated (`.claude/rules/design-patterns.md` shipped 2026-04-14; `ai-branding-guidelines.md` §10 references it)
- [x] Outbox infrastructure (`kiteclass-core/common/outbox/` — 7 classes)
- [ ] Each GAP-007..015 implementation references relevant pattern (cross-cutting audit needed)
- [ ] Refactor plan cho existing anti-patterns — TrialToPaidService 546 LOC + InstanceService 496 LOC + ~95 status-switch sites
- [ ] Quality audit section: "Patterns applied correctly?" (skill currently missing this category)
- [ ] N/A Training (solo-dev mode)

## Dependencies

- Informs all other GAPs (cross-cutting)
- Requires team alignment on patterns (architecture review)

## Log

- 2026-04-25 — **Status 🟡 PLANNED → 🟡 PARTIAL** after state-check (per `audit-to-gap-pipeline.md` Step 2.5 + `feedback_gap_state_check_required.md`). Rules + infrastructure already shipped (catalog doc, design-patterns.md rule with §3 BANNED anti-patterns + §4 PR checklist, Outbox pattern infra in kiteclass-core). Remaining: (1) extend `quality-audit` skill OR create dedicated `design-pattern-audit` skill scoring God-Service threshold + status-switch density + primitive obsession; (2) audit-driven refactor of 1-2 hotspots (TrialToPaidService 546 LOC, InstanceService 496 LOC, status-switch lifecycle entities). Wave 6 plan sketched in session brainstorm 2026-04-25 (6 sub-PRs, M-L effort).
- 2026-04-14 — User raised: should apply design patterns for optimization
