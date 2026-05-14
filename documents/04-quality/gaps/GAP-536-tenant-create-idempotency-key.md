# GAP-536: POST /tenants idempotency key — prevent double-submit orphan tenants

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — BLOCKING Phase 1 BETA invite (P1 Solo teacher slow 3G common in VN)
**Domain:** Backend
**Found:** 2026-05-14 (Wave 77 — outside-in audit: failure-mode matrix F3)
**Affects:** P1 Solo teacher (mobile-first, slow 3G) — accidental double-tap → 2 tenant rows orphan billing
**Phase:** phase-1-beta

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Tenant create endpoint | `kitehub/kitehub-subscription/src/main/java/**/tenant/TenantController.java` | 🟡 verify-at-spawn |
| Idempotency-Key header support | (anywhere) | ❌ likely missing |
| `idempotency_keys` table | DB schema | ❌ missing |
| FE submit-button debounce | `kitehub-frontend/src/...signup form` | 🟡 verify-at-spawn — may exist partial |

## Problem

Wave 77 outside-in audit (2026-05-14) — failure-mode matrix F3 (P0): **P1 Solo teacher trên 3G chậm nhấn "Tạo trường" 2 lần (form không disable button + network slow)** → 2 POST /tenants requests → 2 tenant rows tạo trong DB race condition → 1 orphan tenant (no admin assigned, no billing).

Recovery cost: admin manual cleanup; user confusion ("tại sao 2 trường?"). Per VN connectivity context: 3G + mobile-first phổ biến cho gia sư cá nhân.

Standard fix: `Idempotency-Key` header (Stripe pattern) — request với same key returns cached response.

## Proposed Fix

1. **Schema:**
   - `V{N+1}__idempotency_keys.sql` — new table:
     ```sql
     CREATE TABLE idempotency_keys (
       key VARCHAR(128) PRIMARY KEY,
       endpoint VARCHAR(64) NOT NULL,
       request_hash VARCHAR(64) NOT NULL,
       response_status INT NOT NULL,
       response_body TEXT NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       INDEX idx_created_at (created_at)
     );
     ```
   - TTL: 24h cleanup job (or scheduled task)
2. **Middleware / Interceptor:**
   - Before POST `/tenants` handler:
     - Read `Idempotency-Key` header (required for mutation endpoints)
     - If key exists in `idempotency_keys` table → return cached `response_status` + `response_body`
     - If key missing → reject 400 `idempotency_key_required` (or auto-generate for forward-compat)
   - After handler success → cache response
3. **FE update:**
   - Frontend generates UUID v4 idempotency key per submit-attempt
   - Header `Idempotency-Key: <uuid>` on POST /tenants
   - Submit button disabled after 1st click (defense-in-depth — even if key works)
4. **Tests:**
   - 1st POST với key X → 201
   - 2nd POST với key X (same body) → 201 (same response, no new row)
   - 2nd POST với key X (DIFFERENT body) → 422 idempotency conflict

## Acceptance Criteria

- [ ] DB migration V{N+1} adds `idempotency_keys` table + Flyway checksum clean per GAP-493 preflight
- [ ] POST `/tenants` honors `Idempotency-Key` header (cached replay returns same response 200/201)
- [ ] FE generates UUID v4 per submit, sends header, disables submit button after click
- [ ] Integration test: 2 sequential POSTs same key → 1 tenant row created (verify DB count)
- [ ] Different body + same key → 422 (Stripe semantics)
- [ ] Cleanup job removes idempotency_keys >24h old (cron OR scheduled task)

## Related

- **Sibling Wave 77 outside-in:** GAP-533, GAP-534, GAP-535
- **Related:** GAP-535 (slug normalize — same controller surface)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-14-77-beta-invite-launch-foundation.md` Bucket D
- **Outside-in audit source:** Wave 77 failure-mode matrix F3 (2026-05-14)

## Log

- **2026-05-14** — Initial write-up. Wave 77 outside-in failure-mode matrix F3 surfaced. Stub in wave plan PR; full execution → Bucket D.
