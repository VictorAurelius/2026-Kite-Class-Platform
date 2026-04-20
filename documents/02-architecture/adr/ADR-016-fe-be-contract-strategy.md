# ADR-016: FE↔BE Contract Strategy — OpenAPI Schema Diff (Phase 1), Pact Deferred

**Status:** ACCEPTED (Phase 1 scope)
**Date:** 2026-04-20
**Deciders:** Tech Lead + Architect + FE Lead
**Reviewers:** QA Lead
**Related Gap:** GAP-198 (consumer-side), GAP-090 (producer-side — DONE)

## Context

GAP-090 shipped producer-side contract tests (`InstanceApiContractTest.java` in `kitehub-subscription`) — BE guarantees its responses match advertised schema.

Consumer-side is missing: MSW mocks in `kiteclass-frontend` + `kitehub-frontend` currently simulate BE responses, but nothing verifies those mocks match real BE schemas. Drift scenario:
1. BE renames `responseDto.studentName` → `responseDto.fullName`
2. MSW handler still returns old `studentName` key
3. FE unit tests pass (consume mock)
4. Integration breaks in staging/prod — no earlier signal

`output-review-mandate.md` §3 currently lists API contracts as ⚠️ PARTIAL for this reason.

### Forces at play

- **Low operational burden** — small team; Pact broker adds infra cost
- **Existing investment** — producer side MockMvc already validates BE schema; doubling with Pact would be redundant at BE layer
- **MSW mocks are FE-authored** — drift risk is "FE mock lags BE schema update", not "BE breaks contract"
- **CI speed matters** — adding a heavy contract test suite to every PR slows velocity
- **OpenAPI spec availability** — all KiteHub services + gateway publish OpenAPI via Springdoc; some endpoints may lag

## Decision

**Phase 1: Lightweight OpenAPI schema diff** (12-week horizon)

1. Each frontend (`kiteclass-frontend`, `kitehub-frontend`) exports its MSW handler responses as JSON Schema fixtures at test time
2. CI job downloads the live OpenAPI spec from each KiteHub service (+ gateway) at build time
3. Schema-diff tool (e.g. `openapi-diff`, `oasdiff`) compares FE mock shape vs BE advertised schema
4. Breaking shape changes → red build; additive changes → warning
5. `api-contract-audit` skill reads the diff report into its /100 score (new subcategory 5)

**Phase 2: Pact or Spring Cloud Contract evaluation** (revisit after Phase 1 in production ≥1 quarter)

If Phase 1 catches ≥80% of drift cases → remain on schema diff. If gaps emerge (e.g. semantic contracts like "status must be one of {A,B,C}" not captured by type alone), escalate to Pact.

## Consequences

### Positive
- ✅ Catches shape drift before staging
- ✅ Reuses existing OpenAPI investment (no new artifact)
- ✅ No broker infrastructure needed (unlike Pact)
- ✅ FE authors remain in control of mocks; diff is the safety net
- ✅ api-contract-audit skill becomes data-driven (no manual check)
- ✅ CI impact minimal — diff runs in seconds

### Negative
- ❌ Schema diff does NOT catch semantic contracts (value constraints, required-if rules)
- ❌ Requires every MSW handler to be introspectable (see Phase 1 implementation notes)
- ❌ OpenAPI spec must stay fresh — stale spec hides drift
- ❌ Gateway composite endpoints (aggregations) need special handling

### Neutral
- Phase 2 re-evaluation in Q3 2026 or when scale warrants
- New CI dependency on `oasdiff` or similar tool
- `api-contract-audit` skill scoring rubric updated to /100 with contract-diff subcategory

## Alternatives Considered

### Alternative A: Full Pact (consumer-driven contracts + broker)
**Pros:**
- Industry standard for consumer-driven contract testing
- Captures semantic expectations ("status ∈ {PENDING, ACTIVE}")
- Versioned contracts with broker-mediated compatibility

**Cons:**
- Broker infrastructure cost (hosted Pact or self-host)
- Learning curve for team (6 devs, none with Pact experience)
- Duplicates BE-side validation already done in GAP-090
- FE-team time investment 2-3 weeks for Phase 1 pilot

**Rejected because:** over-engineering for current scale; operational cost not justified until Phase 1 proves insufficient.

### Alternative B: Spring Cloud Contract
**Pros:**
- Spring-native; no extra broker
- Groovy/YAML DSL familiar to BE team
- Generates stubs consumable by FE tests

**Cons:**
- BE-centric (contract authored by BE); flips "consumer-driven" intent
- FE would not author contracts — reduces FE ownership of what "mock match BE" means
- Stub artifacts to distribute (Nexus/Artifactory) — infra cost

**Rejected because:** model doesn't fit team split (FE authors MSW mocks, wants to own that contract surface).

### Alternative C: Manual review per-PR
**Pros:** no tooling cost

**Cons:** doesn't scale; drift happens silently between reviews; no automation signal

**Rejected because:** `output-review-mandate.md` §3 already flags this as ⚠️ PARTIAL — manual is the current state.

### Alternative D: OpenAPI schema diff (Phase 1 chosen)
See **Decision** above.

## Implementation Notes

### Phase 1 Pilot — 3 endpoints

Detailed pilot plan: [`../04-quality/contract-tests-pilot-plan.md`](../../04-quality/contract-tests-pilot-plan.md).

Chosen endpoints (high-traffic + representative):
1. `POST /api/v1/auth/login` (kiteclass-core) — authentication
2. `GET /api/v1/classes` (kiteclass-core) — paginated list
3. `POST /api/v1/attendance/submit` (kiteclass-core) — write with array payload

### MSW → fixture extraction

- Each handler gets a `.contract.json` sibling file auto-generated by a Vitest/Vite plugin at test time
- Handler example:
  ```typescript
  http.get('/api/v1/classes', () => HttpResponse.json(mockClasses))
  // extractor reads `mockClasses` shape + URL pattern → emits contract.json
  ```
- Fixtures land in `kiteclass-frontend/src/mocks/__contracts__/` (gitignored — regenerated per CI)

### BE OpenAPI spec download

- Spring Boot services expose `/v3/api-docs` via Springdoc
- CI step runs BE in test profile → curls `/v3/api-docs` → saves to `.ci/openapi/{service}.json`
- Executed once per CI run; cached across jobs

### Schema diff tool

- Primary: `oasdiff` (Go binary, fast, outputs JSON/Markdown)
- Alternative: `openapi-diff` (Java, if JVM already warm)
- Output: `.ci/contract-diff-report.md` consumed by api-contract-audit

### `api-contract-audit` skill integration

Skill reads `contract-diff-report.md`:
- Zero breaking changes → +20 points (new subcategory 5 of /100)
- Breaking changes present → subtracts per severity

Skill SKILL.md updated in Phase 1 wave to include this data source.

### Rollback plan

If CI noise too high (false positives on additive changes), gate diff behind `--contract-strict` flag, run weekly instead of per-PR. Degradation path, not full disable.

### Feature flag

No runtime flag needed — this is CI-only tooling. Toggle via CI workflow input `contract-check: true|false`.

## References

- GAP-090 API contract tests (producer-side, DONE)
- GAP-198 FE↔BE consumer-side contract tests (this ADR's gap)
- `documents/04-quality/contract-tests-pilot-plan.md`
- `.claude/skills/quality/api-contract-audit/SKILL.md`
- `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/contract/InstanceApiContractTest.java` (producer reference)
- `output-review-mandate.md` §3 (API Contracts ⚠️ PARTIAL → will promote to ✅ DONE after Phase 2 full coverage)
- External: [oasdiff](https://github.com/oasdiff/oasdiff), [Pact](https://docs.pact.io/)

## Log

- **2026-04-20:** Initial proposal + accepted for Phase 1 scope (GAP-198 Phase 1). Phase 2 evaluation window: 1 quarter post-Phase-1 landing.
