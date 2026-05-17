# GAP-614: Wave 91 Bucket D V60 RLS migration not found in codebase

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-18 (Wave 91 post-batch1 ops-readiness audit OPS-W91-010)
**Affects:** Beta signup BE bugs GAP-610/611 (Wave 91 Bucket D) — possibly unresolved if migration not shipped

## Problem

Wave 91 plan §3 Bucket D nêu rõ: ship NEW Flyway migration `V60__beta_access_request_public_bypass_rls.sql` để cho phép anonymous endpoints query APPROVED + not-expired beta access tokens (per Hypothesis 1 RLS). Ops-readiness audit 2026-05-18 §3.5 Cat 5 row "Wave 91 Bucket D" surfaced:

```bash
find kitehub/kitehub-subscription/src/main/resources/db/migration -name "V60*"
# → 0 results
ls kitehub/kitehub-subscription/src/main/resources/db/migration/ | tail -10
# → highest V-prefix is V52__login_audit_ip_varchar.sql
```

V60 RLS migration NOT FOUND trong codebase. 3 scenarios khả thi:
1. **Bucket D PR chưa merge** — Wave 91 plan §8 Log entry chưa update; status PARTIAL chưa flipped DONE
2. **Migration renamed differently** — e.g., shipped as `V53__` hoặc `V61__` không match plan glob; cần grep semantic content `CREATE POLICY.*beta_access_public`
3. **Bucket D PARTIAL execution** — only controller/service code shipped, migration deferred

Audit-level verdict: 1 P1 follow-up (OPS-W91-010 → this gap GAP-614).

## Root Cause

Cần investigate — 3 hypotheses trong §Problem. Wave 91 plan §8 Log entry only documented planning + Q2 trade-offs; chưa có closure log enumerating actual Bucket D shipped artifacts.

## Proposed Fix

### Phase 1: Verify state (~15min)

```bash
# Hypothesis 1: file with different V-prefix
find kitehub/kitehub-subscription/src/main/resources/db/migration -name "V*beta*rls*" -o -name "V*beta_access_public*"
grep -rl "beta_access_public_token_lookup\|beta_access_public_bypass" kitehub/kitehub-subscription/src/main/resources/db/migration/

# Hypothesis 2: search any beta_access RLS migration
grep -rl "CREATE POLICY.*beta_access" kitehub/kitehub-subscription/src/main/resources/db/migration/

# Hypothesis 3: check Bucket D PR status
gh pr list --search "Wave 91 Bucket D" --state all
git log --all --oneline --grep="GAP-610\|GAP-611\|V60.*beta" | head -10
```

### Phase 2: Outcome-based action

| Verify outcome | Action |
|---|---|
| Migration shipped as different name | Update Wave 91 plan §8 Log + this gap → DONE |
| Migration not shipped, Bucket D PR pending | Wait for Bucket D PR merge, then re-verify; this gap stays OPEN |
| Migration deferred from Bucket D | File GAP-615 for migration; downgrade this to PARTIAL after Bucket D PR DONE flip rationalized |

### Phase 3: If migration truly missing (worst case)

Ship V60 (or next available V-prefix) migration per Wave 91 plan §3 Bucket D §"Implementation Hypothesis-driven":

```sql
ALTER TABLE beta_access_request ENABLE ROW LEVEL SECURITY;
-- Existing tenant_isolation policy preserved
CREATE POLICY beta_access_public_token_lookup ON beta_access_request
  FOR SELECT
  USING (status = 'APPROVED' AND invite_token_expiry > NOW());
```

Per `pre-launch-owasp-rest-hardening-checklist.md` §2.1 RLS defense-in-depth — scope tight to active tokens only (NOT all rows).

## Acceptance Criteria

- [ ] Phase 1 verify-state commands run; output documented trong this gap §Log
- [ ] Outcome-based action per Phase 2 matrix executed
- [ ] Wave 91 Bucket D status synchronized (DONE rationale OR PARTIAL with follow-up filed per `gap-done-discipline.md` §3)
- [ ] If RLS migration genuinely missing: ship migration in Wave 92 Bucket A; live verify POST /api/v1/auth/beta-signup → 200 + tenant created (was 404 Wave 90)
- [ ] Update GAP-610/611 status per verify outcome

## Related

- Wave 91 plan: `documents/03-planning/waves/wave-2026-05-18-91-production-restore-email-infra-beta-signup.md` §3 Bucket D
- Related gaps: GAP-610 (RLS hypothesis) + GAP-611 (gateway route hypothesis) — both Wave 91 Bucket D scope
- Audit that surfaced this: `documents/04-quality/audits/ops-readiness/2026-05-18-wave-91-post-batch1-ops-readiness.md` §3.5 row "Wave 91 Bucket D"
- Rule: `audit-to-gap-pipeline.md` §3 gap-from-audit naming + `pre-launch-owasp-rest-hardening-checklist.md` §2.1 RLS defense-in-depth
- Migration list: highest V-prefix verified = `V52__login_audit_ip_varchar.sql` (2026-05-18 grep)

## Log

- **2026-05-18:** Gap filed by Wave 91 post-batch1 ops-readiness audit (OPS-W91-010). Bucket D V60 RLS migration claim trong Wave 91 plan §3 KHÔNG verify được trong codebase (`find -name "V60*"` returns 0; highest V-prefix = V52). 3 hypotheses (different name / PR pending / PARTIAL execution) cần verify Phase 1. Wave 92 queue.
