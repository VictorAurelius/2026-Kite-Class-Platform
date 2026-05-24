# GAP-662: Wave 98 EmailController URL drift — code `/api/platform/emails/send` vs doc `/api/email/send`

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (API contract 3-way drift)
**Found:** 2026-05-19 (Wave 98 post-closure audit suite — GAP-661 API Contract /100 audit)
**Affects:** kitehub-email service / 5 critical email types routing / beta invite + verify + password-reset + welcome + staff-invite

## Problem

Wave 98 Bucket B1 ship `documents/01-business/kitehub/email/api-contract.md` declaring endpoint path `/api/email/send` — nhưng actual controller code routes via `/api/platform/emails/send` (legacy platform path). 3-way drift surfaced bởi GAP-661 API Contract audit:

- **Doc** (api-contract.md §Endpoint): `POST /api/email/send`
- **Code** (`EmailController.java`): `@RequestMapping("/api/platform/emails")` → effective path `/api/platform/emails/send`
- **Test** (MockMvc fixture): tests against actual code path, NOT documented path

Impact: any consumer following api-contract.md sẽ HTTP 404 khi POST `/api/email/send`. 5 critical email types (beta-invite + verify-email + password-reset + welcome + staff-invite) may not route correctly if upstream caller uses documented URL.

Per `contract-first-for-cross-layer.md` §3 — cross-layer drift detected; code + docs + tests must reconcile to ONE canonical URL.

## Root Cause

B1 agent shipped api-contract.md scaffolding fresh per Living Docs mandate nhưng KHÔNG verify controller path existing trong code (skip `audit-to-gap-pipeline.md` §2.5 state-check at filing time). Controller path `/api/platform/emails/*` predates Wave 98 (legacy from earlier email service iteration); should have been documented as-is OR controller renamed to match new doc.

## Proposed Fix

### Option A (preferred): Rename controller to match doc

```java
// EmailController.java
@RequestMapping("/api/v1/email")   // was: /api/platform/emails
public class EmailController { ... }
```

Update:
- `EmailController.java` `@RequestMapping`
- All callers in kitehub-subscription / kitehub-platform / other services using `RestTemplate`/`WebClient` referencing old path
- `application*.yml` if path hardcoded for SecurityConfig matchers
- MockMvc test fixtures match new path
- Doc cite `/api/v1/email/send` for consistency với admin v1 namespace pattern

### Option B (fast): Update doc to match code

Edit `documents/01-business/kitehub/email/api-contract.md` §Endpoint → `/api/platform/emails/send`. Cite legacy path origin + plan migration to `/api/v1/email/*` Wave 100+ namespace cleanup.

**Recommend Option A** — `/api/platform/*` legacy ambiguous (was Wave 35-ish controller naming experiment); v1 namespace pattern adopted by admin Wave 97 → email should follow same pattern.

## Acceptance Criteria

- [ ] Pick Option A or Option B with documented rationale
- [ ] Code + docs + tests all reference ONE canonical URL
- [ ] Grep audit: zero references to dropped path remain (per `audit-to-gap-pipeline.md` §2.7 decision-doc code-sync)
- [ ] Integration test exercises documented URL end-to-end
- [ ] `output-review-mandate.md` §3 API Contract row reflects fix in next refresh

## Related

- **Parent audit:** `documents/04-quality/audits/api-contract/2026-05-19-wave-98-new-contracts.md`
- **Carry-forward:** GAP-637 admin v1 @PreAuthorize + GAP-638 6 endpoints undocumented (Wave 92 carry, similar 3-way pattern)
- **Rule:** `contract-first-for-cross-layer.md` §3 cross-layer drift
- **Rule:** `audit-to-gap-pipeline.md` §2.5 state-check at filing time (B1 agent missed)
