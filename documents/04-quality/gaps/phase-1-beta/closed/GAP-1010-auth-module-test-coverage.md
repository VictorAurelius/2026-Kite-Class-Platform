# GAP-1010: Auth module zero automated test coverage (auth-1)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (Wave auth-1 post-wave audit suite — business-logic P1 + api-contract P1)
**Affects:** `kiteclass-core/src/main/java/com/kiteclass/core/module/auth/**`

## Problem

KC-native login module (AuthService / AuthTokenService / AuthCredentialProvisioningService / AuthController) has **0 automated tests** — `find kiteclass-core/src/test/.../module/auth -type f → 0`. Only manual G3 walk + indirect ParentInvitationServiceTest/TeacherServiceTest coverage. No MVC contract test for the new public auth endpoints. Security-critical surface (token minting, BCrypt verify, uniform-401 no-enumeration, claim construction, provisioning upsert) is unverified by regression.

## Proposed Fix

Add: `AuthServiceTest` (happy login + 3× uniform-401 paths: email-not-found / bad-password / disabled), `AuthTokenServiceTest` (HS512 claim role/tenantId/referenceId + TTL), `AuthCredentialProvisioningServiceTest` (upsert idempotency + cross-entity guard once GAP-1013 lands), `AuthCredentialPostgresIT` (Testcontainers — real Flyway V89 schema, not ddl-auto), MVC `AuthControllerIT` (200 happy + uniform 401 + 400 validation).

## Acceptance Criteria

- [x] AuthService + AuthTokenService + Provisioning unit tests green
- [x] AuthCredentialPostgresIT on Testcontainers (Flyway schema) green
- [x] MVC contract test: 200 / uniform-401 / 400 validation
- [x] Tests run in `./mvnw test` (Core Service gate)

## Related

- Audit reports: `documents/04-quality/audits/business-logic/2026-06-06-wave-auth-1-business-logic.md` + `../api-contract/2026-06-06-wave-auth-1-api-contract.md`
- `kiteclass-core IT ddl-auto masks migration drift` (memory) — IT must use Flyway schema not ddl-auto

## Log

- **2026-06-06:** DONE 2026-06-06 PR #2193 — 5 test classes/49 tests incl AuthCredentialPostgresIT (Testcontainers Flyway V89).
