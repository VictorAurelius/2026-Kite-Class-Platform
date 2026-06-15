# GAP-1419: Cross-flow sweep — FE camelCase POST bodies vs BE @JsonProperty snake_case DTOs

**Status:** 🟢 DONE
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

- [x] BE @JsonProperty snake_case request-DTO inventory produced.
- [x] Each cross-checked against FE caller; drifting ones fixed + tested.
- [x] Detector decision logged (ship CI check OR HONEST-defer with rationale).

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** FE POST/PUT body sends camelCase key for a BE @RequestBody field bound to snake_case via explicit `@JsonProperty("x_y")` → BE sees null → 400.

**Grep:** `grep -rnE '@JsonProperty\("[a-z]+_[a-z_]+"\)' kiteclass/kiteclass-core/src/main/java kitehub/*/src/main/java` → 28 hits / 13 files. Sanity: no global `PropertyNamingStrategy.SNAKE_CASE` in either product → BE defaults camelCase; only annotated fields are snake → class bounded to these.

**Request-DTO sites + verdict:**

| # | DTO | FE caller | Verdict | Reason |
|---|---|---|---|---|
| 1 | `RecurrenceRuleDto` (by_day/start_time/end_time/exclude_dates) | `classesApi.generateSessionsFromRecurrence` | **FIX** | DRIFT — fixed GAP-1418 (#2443) + unit test |
| 2 | 2FA `EnrollConfirmRequest` (first_totp_code) | `2fa-setup/page.tsx:102` `{ first_totp_code }` | EXEMPT | FE already sends snake — correct |
| 3 | 2FA `VerifyRequest` (challenge_token/totp_code/recovery_code) | `2fa-challenge/page.tsx:116-122` snake keys | EXEMPT | FE already sends snake — correct |
| 4 | 2FA `DisableRequest`/`RegenerateRequest` (current_totp_code/password_reconfirm) | none (unwired) | DEFER | No FE caller yet; whoever wires must send snake — noted |
| 5 | `ZalopayCallbackRequest` | vendor webhook (Zalopay→BE) | EXEMPT | Not FE-originated |
| 6 | Response DTOs (Login/Verify/EnrollInit/EnrollConfirm/Regenerate/Disable*Response) | FE reads (destructures snake e.g. qr_uri, recovery_codes) | EXEMPT | Read-path, not request-drift; FE already reads snake |

**Decision:** Sites FIXED this class: 1 (recurrence, GAP-1418). DEFERRED: 0 active (disable/regenerate unwired — note only). EXEMPT: rest. **No other flow currently has the bug** — 2FA was authored snake-aware.

**Detector — HONEST DEFER** (per `cross-flow-bug-class-sweep.md` §4.1 + `incident-to-rule-pipeline.md` §3.1): a CI check would parse BE `@JsonProperty` snake fields + cross-reference FE caller body keys (non-trivial — needs FE call-graph + body-shape inference). Recurrence-count = 1; manual sweep above covers current universe (bounded, 13 files); 2FA clean. Revisit + build detector if a 2nd drift instance appears OR when disable/regenerate gets wired.

## Related

- Parent: GAP-1418 (recurrence drift, fixed)
- Rule: `cross-flow-bug-class-sweep.md` §4.1 (statically-detectable → persistent detector)
- Sister detectors: GAP-802 BE↔FE URL contract (`check-be-fe-url-contract.sh`)
