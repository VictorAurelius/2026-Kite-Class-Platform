# GAP-427: POST /api/v1/auth/beta-signup/exchange-claim-code not documented in api-contract.md

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** KiteHub — kitehub-subscription / beta-access
**Found:** 2026-05-08 (API Contract audit Wave 40 Bucket F)
**Affects:** FE developers integrating beta signup 2FA flow; `documents/01-business/kitehub/beta-access/api-contract.md`

## Problem

`POST /api/v1/auth/beta-signup/exchange-claim-code` exists in `BetaAccessController.java:85` (Wave 36 GAP-388-B) but is absent from `documents/01-business/kitehub/beta-access/api-contract.md`.

The endpoint is the 2FA gate for beta signup: FE submits a 6-digit claim code received via email, and the endpoint returns an `inviteToken` UUID + pre-fill data. The FE must call this endpoint FIRST before calling `POST /beta-signup`.

Without this in api-contract.md, the FE integration docs describe only `POST /beta-signup` (direct token redemption) and miss the 2FA step entirely, forcing FE devs to read controller source directly.

## Root Cause

Wave 36 GAP-388-B was a BE-only hotfix adding the claim-code 2FA mechanic. It did not include a FE consumer, so the `contract-first-for-cross-layer.md` cross-layer check did not fire. The api-contract.md was not updated in the same PR.

## Current State (verified 2026-05-08)

| Artifact | Status |
|---|---|
| `BetaAccessController.java:85` `@PostMapping("/api/v1/auth/beta-signup/exchange-claim-code")` | ✅ exists |
| `BetaClaimCodeExchangeCommand.java` (DTO: `{ claimCode }`) | ✅ exists |
| `BetaClaimCodeExchangeResponse.java` (DTO: `{ valid, email, name, persona, inviteToken, expiresAt, status }`) | ✅ exists |
| `BetaAccessServiceTest:383-430` — 3 service tests (`happyPath`, `wrongCode`, `expired`) | ✅ exists |
| `BetaAccessControllerTest` — controller test for exchange-claim-code path | ❌ missing |
| `documents/01-business/kitehub/beta-access/api-contract.md` — endpoint entry | ❌ missing |

## Proposed Fix

Update `documents/01-business/kitehub/beta-access/api-contract.md` to add:

### POST /api/v1/auth/beta-signup/exchange-claim-code

**Use case:** UC-BETA-007 — Submit 6-digit claim code to obtain invite token (2FA gate)
**Auth:** Public unauthenticated.
**Flow:** Call this endpoint AFTER `/request-beta-access` and AFTER receiving the 6-digit code via email. The returned `inviteToken` is then passed to `POST /beta-signup`.

**Request body (`BetaClaimCodeExchangeCommand`):**
```json
{ "claimCode": "123456" }
```

**Response 200 OK (`BetaClaimCodeExchangeResponse`):**
```json
{
  "valid": true,
  "email": "owner@example.edu.vn",
  "name": "Nguyễn Văn A",
  "persona": "P2_CENTER_OWNER",
  "inviteToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresAt": "2026-05-09T10:23:00Z",
  "status": "APPROVED"
}
```

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 404 | `CODE_NOT_FOUND` | Claim code not found in system |
| 404 | `CODE_EXPIRED` | Claim code expired (>24h window) |
| 400 | `BETA_INVALID_CODE_FORMAT` | Code not 6-digit numeric format |

Also add a controller-layer test in `BetaAccessControllerTest` covering this path.

## Acceptance Criteria

- [ ] `documents/01-business/kitehub/beta-access/api-contract.md` contains `POST /api/v1/auth/beta-signup/exchange-claim-code` with request + response schema + error codes
- [ ] Flow context documented: FE calls exchange-claim-code before beta-signup
- [ ] `BetaAccessControllerTest` covers the exchange-claim-code controller path (happy path + CODE_NOT_FOUND + CODE_EXPIRED)
- [ ] api-contract.md `Last verified` date updated

## Related

- Controller: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java:85`
- DTOs: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/BetaClaimCodeExchangeCommand.java` + `BetaClaimCodeExchangeResponse.java`
- Service tests: `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/beta/service/BetaAccessServiceTest.java:383-430`
- Audit report: `documents/04-quality/audits/api-contract/2026-05-08-wave-40-milestone.md`
- Business docs: `documents/01-business/kitehub/beta-access/api-contract.md`
- Rule violated: `.claude/rules/contract-first-for-cross-layer.md`

## Log

- **2026-05-08:** Filed by Wave 40 Bucket F API Contract audit. BetaAccessController.java:85 endpoint confirmed present in code + service tests; absent from api-contract.md. Wave 36 GAP-388-B BE-only hotfix escaped cross-layer contract check. Target wave: Wave 41 cluster.
