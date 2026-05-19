# GAP-663: Wave 98 PreferencesController zero integration tests + cookie httpOnly drift

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (test coverage + cookie semantic drift)
**Found:** 2026-05-19 (Wave 98 post-closure audit suite — GAP-661 API Contract /100 audit)
**Affects:** kitehub-subscription `PreferencesController` / new endpoint `POST /api/v1/preferences/dismiss-banner-state` / banner dismiss user flow

## Problem

Wave 98 Bucket B0 ship `PreferencesController.dismissBannerState()` new endpoint với new DTO + new cookie response — KHÔNG có integration test cho HTTP routing + cookie attribute verification. Bucket B0 PR #1548 ship `.java` controller + api-contract doc nhưng zero `*IT.java` test class.

Audit GAP-661 surfaced 2 sub-issues:

1. **Zero integration tests** — new endpoint untested at HTTP layer. Unit tests with `@Mock PreferencesService` insufficient — bypass actual `@RestController` routing, JWT validation, security config, cookie serialization.
2. **Cookie httpOnly semantic drift** — doc `api-contract.md` describes cookie as `HttpOnly` (security default expected); actual code `ResponseCookie.from(...).httpOnly(false)` (intentional — FE JS reads cookie to update UI state without page reload). Mismatch confuses reviewer + downstream consumer.

Impact: regression risk khi controller refactored → no test catches HTTP 500 / wrong cookie attributes / JWT mismatch. Doc misleading về security model (looks more secure than actual).

Per `postgres-specific-type-testcontainers.md` adjacent pattern (new entity → mandatory IT). Per `pre-handoff-self-test-completeness.md` §2.1 — auth-gated user-flow verify required.

## Root Cause

Bucket B0 scope intentionally compressed (PREREQ bucket blocking B5) — agent prioritized FE Coordinator + Playwright spec, deferred BE IT to "later". No follow-up filed → orphan. Cookie httpOnly drift = agent doc-vs-code reconciliation skipped (FE design intent valid, but doc should reflect reality with rationale comment).

## Proposed Fix

### Step 1: PreferencesControllerIT.java (kitehub-subscription)

```java
@SpringBootTest
@AutoConfigureMockMvc
class PreferencesControllerIT {
    @Test void dismissBannerState_returns_204_and_sets_cookie() {
        mockMvc.perform(post("/api/v1/preferences/dismiss-banner-state")
                .header("Authorization", "Bearer " + validJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().exists("banner_dismissed_until"))
            .andExpect(cookie().httpOnly("banner_dismissed_until", false))  // FE-readable per design
            .andExpect(cookie().maxAge("banner_dismissed_until", 7 * 24 * 3600));
    }

    @Test void dismissBannerState_returns_401_without_jwt() {
        mockMvc.perform(post("/api/v1/preferences/dismiss-banner-state"))
            .andExpect(status().isUnauthorized());
    }

    @Test void dismissBannerState_idempotent_replay() {
        // POST twice → both 204, cookie maxAge resets
    }
}
```

### Step 2: Update api-contract.md cookie semantic

Edit `documents/01-business/kitehub/preferences/api-contract.md` §Response → cookie section:

```markdown
| Cookie | Value | HttpOnly | Why |
|---|---|---|---|
| banner_dismissed_until | ISO timestamp | **false** | FE JS reads to render dismissed state without page reload (per Wave 98 B0 design — banner is non-sensitive UX state) |
```

Add comment: `<!-- Intentional non-HttpOnly: banner state is FE display preference, not auth/PII -->`

### Step 3: Verify no other consumers expect HttpOnly

`grep -rn "banner_dismissed_until" kitehub/ kiteclass/ --include="*.ts" --include="*.tsx"` — confirm FE reads cookie via `document.cookie` (correct expectation).

## Acceptance Criteria

- [ ] `PreferencesControllerIT.java` created với ≥3 test methods (happy path / unauthorized / idempotent replay)
- [ ] `mvn verify -P strict-warnings` PASS kitehub-subscription
- [ ] api-contract.md cookie section reflects actual code semantic (HttpOnly=false intentional)
- [ ] FE grep confirms consumers expect non-HttpOnly cookie
- [ ] Audit refresh in next post-wave audit suite reflects fix

## Related

- **Parent audit:** `documents/04-quality/audits/api-contract/2026-05-19-wave-98-new-contracts.md`
- **Sibling P0:** GAP-662 EmailController URL drift (3-way drift family)
- **Rule:** `pre-handoff-self-test-completeness.md` §2.1 auth-gated flow verify
- **Rule:** `audit-to-gap-pipeline.md` §2.5 state-check at filing time
