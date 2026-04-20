# GAP-196: 9router Tool Evaluation

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (meta tier — investigation → ADR)
**Domain:** Meta / Architecture / Gateway
**Found:** 2026-04-20 (action-1 §15.G — user mentioned line 427)
**Wave:** Wave 8b (meta)
**Affects:** Potentially KiteHub / KiteClass API gateway architecture; decision scope TBD

## Problem

User raised `9router` tool during a session (action-1 line 427) without session-time resolution. It is unclear:
- What problem 9router solves (request routing? rate limiting? multi-tenant routing?)
- Whether it overlaps with existing kite-gateway (Spring Cloud Gateway) or sits in a different layer
- Whether it is a proprietary tool, open-source, or internal proposal
- Whether decision is still open or was informally closed

Without an evaluation + ADR, this item risks dangling indefinitely.

## Context

Investigation gap, not a feature. Output: decision doc (yes / no / revisit later).

## Proposed Fix

1. **Discovery** — identify the tool (ask user to paste link / spec) and list its claimed capabilities
2. **Comparison** — 1-page matrix vs current stack (Spring Cloud Gateway, Envoy, Kong) on: auth, rate limit, multi-tenant routing, observability, cost, maturity
3. **ADR** — `documents/02-architecture/adr/ADR-0NN-9router-evaluation.md`
   - Status: `accepted / rejected / deferred`
   - Context, decision, consequences (per MADR template)
4. **Close the loop** — mark this gap DONE with link to ADR

## Acceptance Criteria

- [ ] Clarification captured (what is 9router?)
- [ ] Comparison matrix completed
- [ ] ADR merged with explicit decision
- [ ] Gap closed with ADR link

## Related

- action-1 §15.G
- `documents/02-architecture/adr/` (target folder — see GAP-172 ADR process)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P2)

## Log

- 2026-04-20 — Created from action-1 §15.G to prevent item from dangling.
