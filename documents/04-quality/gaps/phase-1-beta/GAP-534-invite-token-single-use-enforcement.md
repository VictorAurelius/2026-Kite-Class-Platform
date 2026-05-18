# GAP-534: Invite token single-use enforcement + audit log

**Status:** 🟡 PARTIAL — Wave 77 Bucket D code+tests shipped; live verify gated on next deploy (Flyway V39 + service)
**Priority:** 🔴 P0 — BLOCKING Phase 1 BETA invite (security foundation)
**Domain:** Backend
**Found:** 2026-05-14 (Wave 77 — outside-in audit: failure-mode matrix F1)
**Affects:** All beta personas — invite link forwarded/leaked allows slot hijack
**Phase:** phase-1-beta

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Invite token domain | `kitehub/kitehub-subscription/src/main/java/**/invite/*` | 🟡 verify-at-spawn — likely exists with multi-use semantics from Wave 45 |
| Beta access flow | Wave 45 closure (GAP-372) | 🟢 DONE |
| `used_at` column / single-use flag | DB schema | ❌ verify-at-spawn |
| Audit log entry on reuse attempt | (anywhere) | ❌ missing |
| GAP-530 email flow verify | `GAP-530-email-flow-end-to-end-verify.md` | 🔵 OPEN — covers happy-path; does NOT cover reuse/leak |

**Grep commands run (deferred to Bucket D agent — see wave plan §4 verify-at-spawn rows):**
```bash
grep -rln "InviteToken\|inviteToken" kitehub/kitehub-subscription/src/main/java/
ls kitehub/kitehub-subscription/src/main/resources/db/migration/ | grep -iE "invite|token"
```

## Problem

Wave 77 outside-in audit (2026-05-14) — failure-mode matrix F1 (P0): **invite email forwarded → 2nd recipient validates token → beta slot hijack**. Current invite token flow (per Wave 45 GAP-372 closure) likely lacks:

1. Single-use enforcement — same token validates twice OK currently
2. Audit log entry on reuse attempt — no signal to admin
3. Device-binding option — token usable from any IP/UA

Failure-mode reasoning: solo beta cohort = small (5-20 tenants Phase 1 BETA Linear cohort playbook). One leak / one share → poisoned cohort + admin manual recovery burden.

Scope note: F1 matrix flagged device-binding as desirable but DEFER to Phase 2 — complexity > benefit cho Phase 1 BETA solo-dev scope. Wave 77 ships single-use + audit log; device-binding tracked separately when (if) needed.

## Proposed Fix

1. **Schema:**
   - `V{N}__invite_token_single_use.sql` — add `used_at TIMESTAMP NULL` column to `invite_tokens` (or equivalent table)
   - Backfill `used_at = signup_completed_at` for existing tokens (or NULL if not signed up)
2. **Service:**
   - `InviteTokenService.validateAndConsume(token)` — atomic UPDATE WHERE `used_at IS NULL`; row-count==1 = success; row-count==0 = 409 Conflict
3. **Audit log:**
   - Reuse attempt logs `WARN invite_token_reuse_attempt token=<hashed> ip=<ip> user_agent=<ua>`
   - Increment Prometheus counter `kitehub_invite_token_reuse_attempts_total`
4. **API behavior:**
   - 1st use → 200 + signup flow
   - 2nd use → 409 Conflict + error message "Link đã được sử dụng. Vui lòng liên hệ support."

## Acceptance Criteria

- [x] DB migration V39 adds `used_at` + `consumed_ip` + `consumed_user_agent` columns (Wave 77 Bucket D — Flyway checksum-immutable per GAP-493 retro; partial index on `used_at` non-null)
- [x] `InviteTokenService.validateAndConsume` enforces single-use atomically via repository `@Modifying` UPDATE WHERE `used_at IS NULL`; row-count==0 → throws `InviteTokenAlreadyUsedException` (controller maps to 409)
- [x] Audit log entry on reuse attempt — `log.warn("invite_token_reuse_attempt token=... attempt_ip=... attempt_user_agent=...")` per `logs-format-standard.md`
- [x] Prometheus counter `kitehub_invite_token_reuse_attempts_total` exposed via Micrometer
- [x] User-facing error message Vietnamese: "Link đã được sử dụng. Vui lòng liên hệ support."
- [x] Existing tokens (if any unconsumed) remain valid — `used_at IS NULL` for all backfilled rows
- [x] Unit tests cover first-consume / reuse-attempt / not-found / wrong-status / expired (6 tests pass)
- [ ] **Live verify post-deploy:** real reuse attempt via curl → 409 + audit log entry + counter increment (deferred — Flyway apply via deploy workflow)

## Related

- **Sibling Wave 77 outside-in:** GAP-533 (deliverability), GAP-535 (slug normalize), GAP-536 (idempotency)
- **Upstream context:** GAP-372 (beta access flow Wave 45)
- **Downstream:** GAP-530 (email flow verify — must cover both happy-path AND single-use enforcement)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-14-77-beta-invite-launch-foundation.md` Bucket D
- **Outside-in audit source:** Wave 77 failure-mode matrix F1 (2026-05-14)

## Log

- **2026-05-14** — Wave 77 Bucket D code shipped. Files: `V39__invite_token_single_use.sql` (3 cột + partial index) + `BetaAccessRequest.java` (used_at/consumed_ip/consumed_user_agent fields) + `BetaAccessRequestRepository.consumeInviteToken` (atomic `@Modifying` UPDATE) + `InviteTokenService.java` (lifecycle gates + atomic consume + audit log + counter) + `InviteTokenAlreadyUsedException.java` + 6 unit tests pass. Status → PARTIAL: code+tests DONE, Flyway migration apply + live verify deferred to deploy workflow per `pre-handoff-self-test-completeness.md` §2.3 (production-equivalent verify out of scope this PR).
- **2026-05-14** — Initial write-up. Wave 77 outside-in failure-mode matrix F1 surfaced. Stub in wave plan PR; full execution → Bucket D.
