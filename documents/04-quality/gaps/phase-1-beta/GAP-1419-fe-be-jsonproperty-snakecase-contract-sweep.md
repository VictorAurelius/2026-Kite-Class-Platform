# GAP-1419: Cross-flow sweep — FE camelCase POST bodies vs BE @JsonProperty snake_case DTOs

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed (FE↔BE contract)
**Found:** 2026-06-15 (GAP-1418 root cause — recurrence by_day/start_time null)
**Affects:** any FE API client POST/PUT body → kiteclass-core / kitehub request DTO that uses explicit `@JsonProperty("snake_case")`

## Problem

GAP-1418 found `RecurrenceRuleDto` binds snake_case via `@JsonProperty` while the FE sent camelCase → silent null → 400. This is a **statically-detectable class** (per `cross-flow-bug-class-sweep.md` §4.1): any request DTO with `@JsonProperty("x_y")` whose FE caller serializes a camelCase object has the same latent drift. The recurrence endpoint was caught only by a runtime walk; sister endpoints may be unverified.

## Proposed Fix

1. Grep BE request DTOs for field-level `@JsonProperty("..._...")` (snake_case) on `record`/class params bound from `@RequestBody`.
2. For each, cross-check the FE caller's POST/PUT body key casing (camelCase object vs explicit snake_case map).
3. Fix drifting callers (map to snake_case at the API-client layer, like GAP-1418).
4. Consider a persistent CI detector (FE body keys vs BE @JsonProperty) per `cross-flow-bug-class-sweep.md` §4.1 — statically detectable → manual grep insufficient long-term.

## Acceptance Criteria

- [ ] BE @JsonProperty snake_case request-DTO inventory produced.
- [ ] Each cross-checked against FE caller; drifting ones fixed + tested.
- [ ] Detector decision logged (ship CI check OR HONEST-defer with rationale).

## Related

- Parent: GAP-1418 (recurrence drift, fixed)
- Rule: `cross-flow-bug-class-sweep.md` §4.1 (statically-detectable → persistent detector)
- Sister detectors: GAP-802 BE↔FE URL contract (`check-be-fe-url-contract.sh`)
