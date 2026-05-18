# GAP-614: Wave 91 Bucket D V60 RLS migration not found in codebase

**Status:** 🟢 DONE 2026-05-18 — false-positive audit finding; Bucket D verdict = migration NOT needed (RLS hypothesis REJECTED via static analysis per `audit-to-gap-pipeline.md` §2.8 fix-time state-check)
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-18 (Wave 91 post-batch1 ops-readiness audit OPS-W91-010)
**Affects:** Beta signup BE bugs GAP-610/611 (Wave 91 Bucket D) — RESOLVED: GAP-610/611 stay PARTIAL với root-cause hypothesis "data state mismatch (~70%)" hoặc "image promotion drift (~10%)" per PR #1495 deep-investigation, KHÔNG phải missing migration

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

- [x] Phase 1 verify-state commands run; output documented trong this gap §Log
- [x] Outcome-based action per Phase 2 matrix executed — outcome = "Migration NOT needed (hypothesis rejected)"
- [x] Wave 91 Bucket D status synchronized — PR #1490 body + PR #1495 audit document rationale
- [x] RLS migration NOT genuinely missing (condition FALSE — hypothesis rejected via static analysis); migration ship sẽ vi phạm `pre-mutation-state-check.md` §3.5 (speculative apply không evidence)
- [x] GAP-610/611 status updated qua PR #1495 (stay PARTIAL với deep-investigation hypothesis ranking; root cause likely data-state mismatch / image promotion drift — chờ Coordinator F live verify post AWS restore GAP-612)

## Related

- Wave 91 plan: `documents/03-planning/waves/wave-2026-05-18-91-production-restore-email-infra-beta-signup.md` §3 Bucket D
- Related gaps: GAP-610 (RLS hypothesis) + GAP-611 (gateway route hypothesis) — both Wave 91 Bucket D scope
- Audit that surfaced this: `documents/04-quality/audits/ops-readiness/2026-05-18-wave-91-post-batch1-ops-readiness.md` §3.5 row "Wave 91 Bucket D"
- Rule: `audit-to-gap-pipeline.md` §3 gap-from-audit naming + `pre-launch-owasp-rest-hardening-checklist.md` §2.1 RLS defense-in-depth
- Migration list: highest V-prefix verified = `V52__login_audit_ip_varchar.sql` (2026-05-18 grep)

## Log

- **2026-05-18 (DONE flip — false-positive audit finding resolved):** Phase 1 verify-state commands run (session post /clear /start-session 02:19 UTC). 3 hypothesis verdicts:
  - **H1: Migration shipped as different name** → ❌ FALSE. `find -name "V*beta*rls*"`, `grep "beta_access_public_token_lookup\|beta_access_public_bypass"`, `grep "CREATE POLICY.*beta_access"` đều trả 0 kết quả. Highest V-prefix vẫn là V52.
  - **H2: Bucket D PR chưa merge** → ❌ FALSE. PR #1490 `fix(wave-91 bucket D): beta signup BE — defensive hardening + JWT filter regression tests (GAP-610+611)` đã MERGED 2026-05-17 18:05 UTC. PR #1495 `audit(wave-91-D): deep investigation GAP-610+611 hypothesis verdicts` đã MERGED 2026-05-17 18:56 UTC.
  - **H3: Bucket D PARTIAL — code shipped, migration deferred** → ⚠️ NEAR but framed sai. Actual: Bucket D **chủ động KHÔNG ship V60 migration** sau khi static code analysis bác bỏ RLS hypothesis. PR #1490 body §Phase 1 ghi: *"V34 enables RLS chỉ trên `instance_id`-keyed tables ... `beta_access_request` KHÔNG có RLS policy nào"* → hypothesis 1 (RLS blocks anonymous query) REJECTED. PR #1495 deep audit xác nhận lại verdict REJECTED (strong) + đề xuất root cause khác = "data state mismatch (~70%)" hoặc "image promotion drift (~10%)" — cần Coordinator F debug live post AWS restore.
- **Verdict:** GAP-614 = audit-filed expectation MISMATCH với actual Bucket D scope. Ops-readiness audit OPS-W91-010 trông vào sự vắng mặt của V60 mà không cross-reference Bucket D verdict + PR #1495 → false-positive. Wave 91 plan §3 Bucket D `V60__beta_access_request_public_bypass_rls.sql` là **implementation hypothesis-driven candidate** không phải mandatory output; Bucket D đã apply `audit-to-gap-pipeline.md` §2.8 fix-time state-check + bác bỏ hypothesis đúng cách → KHÔNG ship migration là đúng. Ship V60 RLS speculative sẽ vi phạm `pre-mutation-state-check.md` §3.5.
- **Lesson for ops-readiness audit:** before file gap "claimed deliverable X missing", cross-reference Bucket-D-execution PR body + companion audit PRs trong cùng wave để xem hypothesis có được bác bỏ không. Audit OPS-W91-010 sẽ tránh được false-positive này nếu đọc PR #1495 body trước khi file gap.
- **Status flip:** OPEN → DONE. All AC satisfied or N/A (AC4 conditional FALSE). Per `gap-done-discipline.md` §2 + `post-merge-sync-completeness.md` §2 — flip docs synced với `gap-status.csv` row trong cùng PR.
- **Investigation artifact:** finding cross-referenced từ `documents/04-quality/audits/aws-verification/2026-05-18-fe-runtime-state-and-cve-gate-investigation.md` (same session).
- **2026-05-18 (gap filed):** Filed by Wave 91 post-batch1 ops-readiness audit (OPS-W91-010). Bucket D V60 RLS migration claim trong Wave 91 plan §3 KHÔNG verify được trong codebase (`find -name "V60*"` returns 0; highest V-prefix = V52). 3 hypotheses (different name / PR pending / PARTIAL execution) cần verify Phase 1. Wave 92 queue.
