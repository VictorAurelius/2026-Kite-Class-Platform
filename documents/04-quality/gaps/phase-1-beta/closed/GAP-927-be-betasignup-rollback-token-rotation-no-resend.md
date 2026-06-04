# GAP-927: BetaAccessService.rollbackSignup rotates invite_token without resending email → invitee retry broken

**Status:** 🟢 DONE 2026-06-04 — Option A shipped Wave flow-kh1; empirical re-walk pending
**Priority:** 🔴 P0 (production-blocker — invitee permanently locked out after any post-completeBetaSignup provisioning failure; recovery requires admin DB inspection or full re-approve cycle)
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh1 G2 walk — invitee g2test-an-4 hit 409 subdomain conflict on first submit, retried with new subdomain, got 404 INVALID_TOKEN because BE silently rotated the token and never told them)
**Affects:**
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java` lines 617-631 (`rollbackSignup`)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` lines 145-156 (catch blocks calling `rollbackSignup`)

## Problem

`BetaAccessService.rollbackSignup(id)` is called by `BetaAccessController` whenever `authService.registerFromBetaInvite(...)` fails after `completeBetaSignup` has already marked the request `SIGNED_UP`. The two failure paths are:

1. **409 — subdomain or email already taken** (`IllegalArgumentException` from registerFromBetaInvite) → controller line 148-149: `rollbackSignup(saved.getId())` + return `ResponseEntity.status(CONFLICT).build()` (empty body)
2. **500 — generic provisioning failure** (`RuntimeException`) → controller line 154-155: `rollbackSignup(saved.getId())` + return `ResponseEntity.status(INTERNAL_SERVER_ERROR).build()`

`rollbackSignup` (BetaAccessService.java:617-631) does this:

```java
entity.setStatus(BetaAccessRequestStatus.APPROVED);
entity.setInviteToken(UUID.randomUUID());                       // ← ROTATES TOKEN
entity.setInviteTokenExpiry(OffsetDateTime.now().plusHours(INVITE_TOKEN_TTL_HOURS));
repository.save(entity);
log.warn("Beta signup rolled back to APPROVED: id={} email={}", id, entity.getEmail());
```

The invitee's browser URL still references the **old** token (`/beta-signup?token=<old-uuid>`). On retry, BE `validateToken` returns `INVALID_TOKEN` because the row's `invite_token` no longer matches → controller returns 404 → invitee thinks their token expired (compounded by the FE bug in GAP-926).

**Empirical reproduction, Wave flow-kh1 G2 walk 2026-06-04 04:39 UTC:**

| Step | URL token | BE state | Outcome |
|---|---|---|---|
| 1. Approve g2test-an-4 (req id=31) | `ef4826b4-2a81-4f31-969c-a03e388fcdfd` | row id=31, status=APPROVED, token=`ef4826b4...` | Invite email delivered ✅ |
| 2. Click invite, submit with subdomain `g2test-an` | same | First call: completeBetaSignup flips status SIGNED_UP. Second call: registerFromBetaInvite throws (subdomain taken by id=29 g2test-an-1). Controller catches → rollbackSignup → status=APPROVED + new token `9ad8ebb8-fc78-44b1-b6da-aa3b5db4830a` | 409 (empty body) returned, but invitee still has old token |
| 3. Retry with subdomain `g2test-an-4` | still `ef4826b4...` (stale) | DB row has `invite_token=9ad8ebb8...`; validateToken on old uuid → no match → INVALID_TOKEN | 404 returned ❌ invitee can never recover from #2 |

Verified by direct SQL probe at 04:42 UTC:

```
 id | email                   | status   | invite_token
 31 | g2test-an-4@example.com | APPROVED | 9ad8ebb8-fc78-44b1-b6da-aa3b5db4830a
```

Original intent of the token rotation is sound (prevent replay if someone abuses the conflict path), but the contract is broken because the new token is never surfaced to the invitee in any form — no email, no FE response field, no UI hint.

## Root Cause

`rollbackSignup` was added in Wave 45 Bucket A (GAP-372 closure follow-up #1) under the assumption that the controller would always resend an invite email on rollback. That resend was never wired. Comment on line 605-608 says *"Re-issues a fresh invite token with a new 24h expiry so the invitee can retry"* — but the freshness lives only in the DB row; the invitee has no way to learn the new value.

## Proposed Fix (pick one, ship same PR)

**Option A — keep token, drop the rotation.** Remove the `setInviteToken(UUID.randomUUID())` + `setInviteTokenExpiry(...)` lines from `rollbackSignup`. Replay risk on the rolled-back token is acceptable because (i) the conflict path already failed safely; (ii) the rate-limit + audit log catch abuse patterns. Smallest diff, lowest risk for Phase 1 BETA.

**Option B — resend the invite email on rollback.** Have `rollbackSignup` invoke `EmailServiceClient.sendBetaInviteEmail(...)` after the save, with the freshly rotated token. Keeps the rotation but closes the loop. Higher cost (touches email path, needs idempotency check to avoid double-send if multiple controller catches fire).

**Option C — return the new token in the 409 response body.** Change the controller to `return ResponseEntity.status(CONFLICT).body(new BetaSignupErrorResponse("SUBDOMAIN_TAKEN", "Subdomain đã sử dụng. Token mới: <uuid>"))` (and similar for 500). FE auto-updates the URL bar. Smallest BE-only diff but exposes the token in HTTP response — security tradeoff (logs, browser history).

Recommend **Option A** for Phase 1 BETA — minimal scope, eliminates the lock-out, defers the replay-prevention work to a follow-up gap when we have time to wire either Option B (preferred long-term) or Option C with proper signing.

## Acceptance Criteria

- [x] Pick one of Options A/B/C above; document rationale in PR description — **Option A picked** (Phase 1 BETA minimal scope; eliminates lock-out; replay risk acceptable per gap §Proposed Fix rationale)
- [x] Implementation: invitee whose first submit hit 409 (subdomain conflict) OR 500 (generic provisioning) can successfully retry with the same URL or with a clearly-communicated new URL/email — invitee URL stays valid; same token works for retry
- [x] `BetaAccessServiceTest` adds a unit case `rollbackSignup_keepsTokenUsableForRetry()` — added; verifies status flips APPROVED + invite_token UNCHANGED + invite_token_expiry UNCHANGED
- [ ] Integration test: simulate full G2 walk — approve → submit with conflict → retry with new subdomain → success
- [x] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3: confirm no other rollback path in the beta-signup chain has the same "rotate-and-forget" pattern (Wave A bug class signature: "side effect committed in step 1 not visible to retry-er in step 2") — sweep performed; see Log
- [ ] Empirical re-walk on local Docker stack: full chain start-to-finish without manual DB intervention

## Related

- Discovered in: Wave flow-kh1 G2 walk session 2026-06-04 (g2test-an-4 first submit 409 conflict, retry 404)
- Sister: GAP-926 (FE BetaSignupForm generic error message — same walk, compounds the lock-out by mis-labelling the 404 as "token expired")
- Sister: GAP-925 (subscription EmailEvent String double-encode — same walk, also surfaced from a "fix one part, miss the chain" pattern after GAP-922)
- Origin code: `BetaAccessService.java:617-631` `rollbackSignup` (GAP-372 closure follow-up #1, Wave 45 Bucket A)
- Origin controller catches: `BetaAccessController.java:145-156`
- Per `pre-handoff-self-test-completeness.md` §2.2 — public-flow validation: confirmation surface visible (here, both the FE message AND the next-step token must be visible to the invitee)
- Per `cross-flow-bug-class-sweep.md` §1 — bug class "side effect commits in step 1 not exposed to caller for step 2" — sweep candidate

## Log

- **2026-06-04 (Wave flow-kh1) — Option A shipped:**
  - **Code change:** `BetaAccessService.rollbackSignup` (lines 617-631) — dropped `entity.setInviteToken(UUID.randomUUID())` + `entity.setInviteTokenExpiry(OffsetDateTime.now().plusHours(INVITE_TOKEN_TTL_HOURS))`. KEPT: status flip APPROVED + `repository.save(entity)` + log warn line. Javadoc updated to cite GAP-927 + `design-patterns.md §3.6` (resilience: side effect must be observable to actor expected to react to it) + `cross-flow-bug-class-sweep.md §1` (bug class "side effect committed in step 1 not exposed to caller for step 2"). Imports `UUID`, `OffsetDateTime`, `INVITE_TOKEN_TTL_HOURS` retained — still used elsewhere in the file (line 424 `approveRequest` legitimate initial issuance; line 269/420/496/523 timestamps; line 311/449/540 UUID).
  - **Unit test added:** `BetaAccessServiceTest#rollbackSignup_keepsTokenUsableForRetry` — Given SIGNED_UP row with known UUID + expiry; When `rollbackSignup(31L)`; Then status flips APPROVED + `inviteToken` UNCHANGED + `inviteTokenExpiry` UNCHANGED.
  - **Cross-flow sweep (per `cross-flow-bug-class-sweep.md` §3):**
    | # | Site | Verdict | Reason |
    |---|---|---|---|
    | 1 | `BetaAccessService.approveRequest` line 424 — `entity.setInviteToken(UUID.randomUUID())` | **EXEMPT** | Initial issuance path on admin approval; the new token IS surfaced to invitee via email + claim-code emission (line 449 `eventEmitter.emit(... EVENT_TYPE_INVITE_SENT ...)`). Bug class N/A. |
    | 2 | `BetaAccessService.completeBetaSignup` lines 591-592 — `entity.setInviteToken(null)` + `entity.setInviteTokenExpiry(null)` | **EXEMPT** | Token cleared on successful signup completion; this is correct lifecycle behavior (one-time-use semantics). |
    | 3 | `AuthService.java:141` + `:380` — `UUID.randomUUID().toString()` for verification tokens | **EXEMPT** | Outside beta-signup chain; auth service handles its own token surfacing through its own flow. |
    | 4 | `ChallengeTokenService` + `StaffInvitation` + `DsarTicket` + others using `UUID.randomUUID()` | **EXEMPT** | Each has its own surfacing mechanism (returned in response body OR included in email). No "rotate-and-forget" pattern. |
    - **Decision:** Sites FIXED this PR: 1 (`rollbackSignup`); Sites DEFERRED: 0; Sites EXEMPT: ≥4 (initial issuance + token clearing on signup + unrelated subsystems all correctly surface their tokens).
  - **Verify:**
    - `./mvnw -pl kitehub-subscription -am compile` → BUILD SUCCESS (34s)
    - `./mvnw -pl kitehub-subscription test -Dtest=BetaAccessServiceTest` → Tests run: 31, Failures: 0, Errors: 0, Skipped: 0 (includes new `rollbackSignup_keepsTokenUsableForRetry` PASS).
  - **Remaining AC (deferred):** Integration test simulating full G2 walk + empirical re-walk on local Docker stack — both require live stack-up; tracked for next session (Wave flow-kh1 G2 re-walk after Docker startup) per `pre-handoff-self-test-completeness.md` §3 post-fix re-walk mandate.
