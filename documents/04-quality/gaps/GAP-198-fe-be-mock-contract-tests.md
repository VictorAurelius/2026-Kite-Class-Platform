# GAP-198: FE ↔ BE Decoupled Mock Contract Tests

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (meta tier — hardens contract audit)
**Domain:** Meta / Testing / Frontend / Backend
**Found:** 2026-04-20 (action-1 §10 + §15.I)
**Wave:** Wave 8b (meta)
**Affects:** FE↔BE contract safety across all kiteclass-frontend + kitehub-frontend routes

## Problem

MSW mocks in the frontend currently simulate BE responses, but there is no **contract guarantee** that mocks match real BE schemas:

- Mocks drift from real API (BE renames field → FE tests still pass on old mock → prod break)
- GAP-090 (API Contract Tests) is DONE but focused on OpenAPI validation at BE, not consumer-driven from FE mocks
- `output-review-mandate.md` currently marks API contracts as ⚠️ PARTIAL because of this gap
- User ask (action-1 line 302): "FE phải có đủ bộ mock API cho BE"

## Context

Complements GAP-090 (producer-side) with consumer-side guarantee. Pact or Spring Cloud Contract candidate. Not a GA-blocker but protects against silent regressions.

## Proposed Fix

1. **Tool decision (ADR)** — Pact (broker infra needed) vs Spring Cloud Contract (Spring-native) vs contract-tests via OpenAPI schema diff (lightweight)
2. **MSW → contract generation**
   - Each MSW handler emits a contract fixture JSON
   - CI job runs contract verification against BE running in test mode
   - Violations = red build
3. **Scope** — start with high-traffic endpoints (auth, class list, attendance, grade)
4. **Integration with api-contract-audit skill** — skill reads contract report into audit /100
5. **Update `output-review-mandate.md`** — API contracts ✅ DONE once implemented

## Acceptance Criteria

### Phase 1 — Pilot
- [ ] ADR with tool decision
- [ ] 3 pilot endpoints with contract tests (login, class list, attendance submit)
- [ ] CI fails on contract violation
- [ ] api-contract-audit skill reads contract report

### Phase 2 — Expansion
- [ ] All kiteclass-frontend routes covered
- [ ] All kitehub-frontend routes covered
- [ ] `output-review-mandate.md` status updated to ✅ DONE

## Related

- action-1 §10 + §15.I
- GAP-090 API contract tests (producer-side, DONE)
- `output-review-mandate.md` (⚠️ PARTIAL entry for API contracts)
- `.claude/skills/quality/api-contract-audit/SKILL.md`
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P2)

## Log

- 2026-04-20 — Created from action-1 §15.I.
