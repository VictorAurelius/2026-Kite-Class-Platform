# API Contract Audit — SaaS Data Safety (PRs #311-317)

**Date:** 2026-04-17 | **Scope:** 6 new endpoints | **Score: 82/100**

## Endpoints Audited

| # | Endpoint | Code | Doc | DTO Match | Errors Doc'd | Integration Test |
|---|----------|:----:|:---:|:---------:|:------------:|:----------------:|
| 1 | GET /admin/emails/history | YES | YES | YES | N/A | NO (unit only) |
| 2 | GET /admin/emails/stats | YES | YES | YES | N/A | NO (unit only) |
| 3 | GET /admin/emails/config | YES | YES | YES | N/A | NO (unit only) |
| 4 | PUT /admin/emails/config | YES | YES | YES | N/A | NO (unit only) |
| 5 | POST /admin/emails/trigger | YES | YES | YES | PARTIAL | NO (unit only) |
| 6 | DELETE /instances/{id}/purge | YES | YES | YES | YES | NO (unit only) |

## 1. Endpoint Coverage (code -> doc) — 20/20

All 6 endpoints exist in code AND in their respective api-contract.md files. Base paths, HTTP methods, and query params all match. No undocumented endpoints found.

## 2. Request/Response Match (DTO fields -> doc) — 18/20

- **EmailHistoryResponse**: 6 fields match doc exactly (id, instanceId, emailType, recipient, sentAt, status). OK
- **EmailStatsResponse**: 4 fields match (totalSentToday, totalSentThisWeek, failedToday, countByType). OK
- **EmailConfigResponse**: 2 fields match (queueEnabled, emailTypeToggles). OK
- **TriggerEmailRequest**: 2 fields match with correct validation (@NotNull, @NotBlank). OK
- **PurgeResult**: 9 fields match. PurgeStatus enum (SUCCESS, SKIPPED_NO_BACKUP, FAILED) matches doc. OK
- **GAP (-2):** Doc says `status` in PurgeResult response is a string value (`"SUCCESS"`), but code uses `PurgeStatus` enum. Serialization should work (Jackson defaults to enum name), but doc should clarify it's an enum, not free-form string.

## 3. Error Code Consistency — 14/20

- **DELETE /purge**: Doc says 404 for not found, 200+FAILED for wrong status, 200+SKIPPED for no backup. Code: throws `EntityNotFoundException` (404 via handler), returns 200 with status. **Match.**
- **POST /trigger**: Doc says 400 (validation), 404 (not found), **409 (idempotency — already sent today)**. Code: throws `EntityNotFoundException` (404), `IllegalArgumentException` (unknown type). **409 NOT IMPLEMENTED** — doc promises idempotency check but code has no such logic. **GAP (-4)**
- **PUT /config**: No error codes documented, none thrown in code. Acceptable for simple toggle.
- **GET endpoints**: No error codes expected for read-only. OK.
- **GAP (-2):** POST /trigger doc does not document the `IllegalArgumentException` for unknown email type (should be 400 with clear message).

## 4. Integration Test Coverage — 12/20

- **InstancePurgeServiceTest**: 8 unit tests covering SUCCESS, SKIPPED_NO_BACKUP, FAILED, not-found, adminPurge. Good coverage but **unit-level only** (Mockito mocks, no HTTP layer).
- **EmailAdminServiceTest**: 11 unit tests covering history (4 filter combos, FAILED status derivation), stats, config get/update, trigger (6 email types + unknown + not-found). Good coverage but **unit-level only**.
- **GAP (-8):** Zero `@WebMvcTest` or `@SpringBootTest` integration tests for any of the 6 new endpoints. No HTTP-layer verification (serialization, status codes, content negotiation, validation error format).

## 5. Backward Compatibility — 18/20

- All 6 endpoints are **new additions** — no existing API modified. No breaking changes.
- **GAP (-2):** No OpenAPI spec file committed (only Swagger annotations on controllers). Contract consumers have no machine-readable spec to validate against.

## Score Summary

| Category | Score | Notes |
|----------|------:|-------|
| Endpoint Coverage | 20/20 | All 6 documented |
| Request/Response Match | 18/20 | PurgeStatus enum vs string |
| Error Code Consistency | 14/20 | 409 not implemented; unknown-type error undocumented |
| Integration Test Coverage | 12/20 | Unit tests good, zero HTTP-layer tests |
| Backward Compatibility | 18/20 | New endpoints only, no OpenAPI spec |
| **TOTAL** | **82/100** | |

## Action Items

| Priority | Issue | Fix |
|----------|-------|-----|
| P1 | POST /trigger 409 idempotency check not implemented but documented | Implement check or remove from doc |
| P1 | Zero integration tests for 6 endpoints | Add @WebMvcTest for AdminEmailController + purge |
| P2 | POST /trigger unknown-type error (400) not in doc | Add to api-contract.md errors section |
| P3 | PurgeStatus enum vs string ambiguity in doc | Clarify enum values in doc |
| P3 | No OpenAPI spec file | Generate from annotations or commit openapi.yaml |
