# GAP-1009: Auth-1 business-doc completeness — tenant-auth 3-layer + portal Option B sync

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-06 (Wave auth-1 post-wave audit suite — business-logic 64/100 + api-contract 85/100)
**Affects:** `documents/01-business/kiteclass/` (no `tenant-auth` domain) + `parent-portal/` + `student-portal/` 3-layer docs

## Problem

KC-native login (Wave auth-1 PR #2186) shipped code without same-PR business docs (Living Docs violation). Surfaced by 2 audits:

1. **No `kiteclass/tenant-auth` 3-layer doc domain at all** — `POST /api/v1/tenant-auth/login` + `POST /api/v1/teachers/{id}/credentials` are undocumented public/admin endpoints; entity_type CHECK / BCrypt / HS512 claims / uniform-401 / TTL 12h / anti-spoof header (`X-User-Reference-Id` strip+reinject) exist only in code.
2. **parent-portal + student-portal docs describe superseded Option A** (Gateway `users` table reference_id) — Option B mints `referenceId` from `auth_credentials.entity_id` (claim-based, no cross-service population). Stale: `parent-portal/rules.md:33,90,181` + `api-contract.md:16,266` + `use-cases.md:71,105,217`.
3. **BR-PARENT-004 still says `PARENT_PORTAL_ENABLED=false`** but Bucket B flipped default true (PDPL gate bypass undocumented).

## Proposed Fix

(1) Create `documents/01-business/kiteclass/tenant-auth/{rules,use-cases,api-contract}.md` — BR-AUTH-xxx (entity_type/BCrypt/HS512/uniform-401/TTL) + both endpoints (request/response shape, 401 INVALID_CREDENTIALS / 400 / 403 / 404 error tables) + anti-spoof header contract.
(2) Update parent-portal + student-portal docs Option A → Option B (BR-PARENT-007 / AUTH-001 / PORTAL-002; referenceId = auth_credentials.entity_id claim).
(3) Update BR-PARENT-004 PARENT_PORTAL_ENABLED flip + document PDPL decision.

## Acceptance Criteria

- [ ] `kiteclass/tenant-auth/` 3-layer docs exist (CI `three-layer-completeness` PASS)
- [ ] Both new endpoints documented in api-contract.md with error tables
- [ ] parent/student-portal docs reflect Option B (no Option A `users` table refs)
- [ ] BR-PARENT-004 reflects PARENT_PORTAL_ENABLED=true + PDPL decision noted

## Related

- Audit reports: `documents/04-quality/audits/business-logic/2026-06-06-wave-auth-1-business-logic.md` + `../api-contract/2026-06-06-wave-auth-1-api-contract.md`
- Wave auth-1 PR #2186 (2b01ac93); GAP-725 + GAP-798b (auth-1 partials)
