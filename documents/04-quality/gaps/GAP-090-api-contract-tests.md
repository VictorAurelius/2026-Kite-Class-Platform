# GAP-090: API Contract Tests (Consumer-Driven)

**Status:** 🟢 DONE
**Priority:** 🟠 P1 (trước production)
**Domain:** Testing / API
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** API consumers (KiteClass FE, KiteHub FE, cross-service calls)

## Problem

`output-review-mandate.md` flags API contracts as VIOLATION — no contract tests exist. `api-contract-audit` skill checks docs↔code sync nhưng không verify runtime behavior.

Developer changes DTO field name → docs updated → nhưng consuming frontend breaks at runtime vì no contract test caught it.

## Proposed Fix

1. Adopt contract testing approach (Pact hoặc Spring Cloud Contract):
   - **Producer side**: verify API responses match contract schema
   - **Consumer side**: verify frontend expectations match API contract
2. Minimum coverage:
   - KiteClass: core API → frontend consumer contract
   - KiteHub: gateway → frontend consumer contract
   - Cross-service: KiteHub → KiteClass provisioning API
3. Add to CI: contract tests run on PR that touches `*Controller.java` or `*Dto.java`
4. Breaking change = contract test failure = PR blocked

## Acceptance Criteria

- [x] Contract test framework chosen and integrated (WebMvcTest + jsonPath schema assertions)
- [x] ≥5 critical endpoints have contract tests (Instance CRUD + AdminEmail stats/config/trigger)
- [x] CI runs contract tests on API-related PRs (standard `mvn test` picks up *ContractTest)
- [x] Breaking change detected before merge (field rename/removal fails schema assertions)
