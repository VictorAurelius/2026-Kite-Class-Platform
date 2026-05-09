# GAP-455: KH beta-funnel E2E coverage extension — error branches + mock shape audit

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / CI / Frontend / Testing
**Found:** 2026-05-09 (GAP-453 PR #1078 user-flagged coverage audit)
**Affects:** `kitehub/kitehub-frontend/e2e/beta-funnel/*.spec.ts` (3 files, 257 lines, 5 tests)

## Problem

GAP-453 Phase B Option B.2 shipped `pnpm test:e2e:gates:ci` running 5 beta-funnel tests as PR-time gate. User-flagged audit 2026-05-09 surfaced that coverage is **happy-path-heavy + shallow vs business docs**. Gate gives signal on regression of 5 covered paths but misses ~15+ documented business scenarios.

### Coverage gap matrix (vs `documents/01-business/kitehub/beta-access/{rules,api-contract,use-cases}.md`)

| Business case | rules.md / api-contract ref | Test exists? |
|---|---|:-:|
| Submit happy path 201 | UC-BETA-001 happy | ✅ |
| FE button disabled if no consent | BR-BETA-001 FE | ✅ |
| BE rejects missing consent (400 `BETA_CONSENT_REQUIRED`) | BR-BETA-001 BE | ❌ |
| Duplicate email rejected (409 `BETA_DUPLICATE_EMAIL`) | BR-BETA-002 | ❌ |
| Honeypot filled (silent reject `BETA_HONEYPOT_FILLED`) | rules.md | ❌ |
| Invalid email format (400 `BETA_INVALID_EMAIL`) | api-contract | ❌ |
| Invalid persona (400 `BETA_INVALID_PERSONA`) | api-contract | ❌ |
| Rate-limited (429 `RATE_LIMITED`) | api-contract | ❌ |
| Admin approve happy | UC-ADMIN-APPROVE | ✅ |
| Admin reject flow | api-contract POST /reject | ❌ |
| Non-admin authenticated user (403) | BR-BETA-003 | ❌ |
| Unauthenticated (401) | api-contract | ❌ |
| Approve when not in PENDING (409) | api-contract | ❌ |
| Token validate success | UC-SIGNUP-VALIDATE | ✅ |
| Token invalid (404 `TOKEN_NOT_FOUND`) | api-contract | ⚠️ (collapsed with 409 case) |
| Token already used (409) | api-contract | ⚠️ (collapsed with 404 case) |
| Signup happy 201 | UC-SIGNUP-COMPLETE | ✅ |
| Signup with duplicate subdomain | (FE / BE) | ❌ |

**Tally:** 5 ✅ · 2 ⚠️ collapsed · 11 ❌. Coverage ~28% of documented scenarios.

### Mock shape inconsistency (from spec files)

- `request-flow.spec.ts:21` mock returns `{id: 'req-test-123', ...}` (id = STRING)
- `admin-approve.spec.ts:37` mock returns `{id: 1, ...}` (id = NUMBER)
- Real BE almost certainly returns ONE type — mocks should match.
- Risk: if FE has bug expecting one type, gate masks it because mocks satisfy whichever type FE happens to look at.

## Proposed Fix

### Phase 1 — Mock shape audit (~30 min)

1. Read `kitehub-subscription` `BetaAccessController` + DTOs to confirm BE response shapes
2. Update mock fulfillments in 3 spec files to match real BE
3. Verify still 5/5 pass after shape fix

### Phase 2 — Add error-branch tests (~1.5-2h)

Add ~5-8 new tests:

1. **request-flow** error branches (~3 tests, +50 LOC):
   - BE rejects missing consent (400) — bypass FE disabled state via direct API mock test, OR add `expect(submit).toBeDisabled` + force-click + verify error message path
   - Duplicate email 409 → verify FE shows "đã có yêu cầu pending"
   - Rate-limited 429 → verify FE shows "thử lại sau"

2. **admin-approve** new tests (~2 tests, +60 LOC):
   - Reject flow → POST /reject mock + verify "REJECTED" badge after action
   - Non-admin user redirected (mock auth as STAFF role; verify 403 / redirect to login)

3. **signup-with-claim-code** disambiguation (~2 tests, +40 LOC):
   - Token expired → 404 distinct from already-used
   - Token already used → 409 with specific Vietnamese error
   - Subdomain conflict → 409 on signup submit

### Phase 3 — Optional: persona expansion (~1h)

Pre-tenant visitor browsing legal docs / about pages — out of beta-funnel scope but related visitor flow.

## Acceptance Criteria

- [ ] Phase 1: mock shape verified vs `BetaAccessController` BE source; documented in spec file comment
- [ ] Phase 2: ≥7 new tests added covering error branches in matrix above
- [ ] Local verify chromium-only: full beta-funnel/ subset still passes 100% (now ~12-13/12-13)
- [ ] CI run on fix-PR shows expanded gate green
- [ ] Coverage matrix updated in this gap; tally moves to ≥80% of documented scenarios

## Related

- Parent: GAP-453 (E2E Phase B umbrella) — 🟡 PARTIAL after PR #1078
- Sibling: GAP-454 (KC E2E investigation) — same Wave 47 lineage
- Business docs: `documents/01-business/kitehub/beta-access/{rules,api-contract,use-cases}.md`
- Test files: `kitehub/kitehub-frontend/e2e/beta-funnel/{request-flow,admin-approve,signup-with-claim-code}.spec.ts`
- Audit lesson: per `release-fix-retry-budget.md` ship-then-extend pattern; `output-review-mandate.md` §3 honest coverage reporting

## Log

- **2026-05-09** Filed during GAP-453 PR #1078 user-flagged coverage audit. User asked "đã đánh giá code E2E tồn tại có coverage đúng và đủ chưa?" — surfaced that 5/5 tests passing ≠ adequate coverage. Decision: ship narrow gate now (some signal > zero), file this gap to extend coverage to ~80% in a follow-up PR. Mock shape inconsistency (string vs numeric id) flagged as Phase 1 quick win.
