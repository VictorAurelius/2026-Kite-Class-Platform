# GAP-610 — `GET /api/v1/auth/beta-signup/validate` returns TOKEN_NOT_FOUND cho valid token (RLS suspect)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-17 (Wave 90 walkthrough — FE deep-link page 404)
**Affects:** Toàn bộ beta signup deep-link flow — invitee không thể proceed dù email + token đúng

## Problem

DB row tồn tại đúng:
```
id=7, email=mvann1207@gmail.com, status=APPROVED,
invite_token=98446443-e5cc-43e9-9498-6799d460d2db,
approved_at=2026-05-17 16:36:08, invite_sent_at=2026-05-17 16:36:08
```

Token chưa expired (TTL 24h, query 5 phút sau approve).

Curl evidence:
```bash
$ curl -s "https://api.kitehub.me/api/v1/auth/beta-signup/validate?token=98446443-e5cc-43e9-9498-6799d460d2db"
{"valid":false,"email":null,"name":null,"orgName":null,"persona":null,"errorCode":"TOKEN_NOT_FOUND"}
HTTP: 404
```

`BetaAccessService.validateToken(UUID)` calls `repository.findByInviteToken(token)` → empty Optional. Return TOKEN_NOT_FOUND.

## Root cause hypothesis (need verify — AWS suspended, blocked)

### Hypothesis 1 (most likely): RLS policy blocks anonymous query

`beta_access_request` table likely has Row-Level Security policy similar pattern to `email_sent_log`:
```sql
USING (COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
    OR instance_id = current_setting('app.current_tenant_id', true)::uuid)
```

Public anonymous endpoint (no JWT, no tenant context) → no SET LOCAL `app.*` settings → RLS hides ALL rows → findByInviteToken returns empty.

### Hypothesis 2: Token UUID encoding mismatch

`invite_token` column type might be `uuid` but JPA send as `varchar` or vice versa. PostgreSQL strict on UUID format match.

### Hypothesis 3: Query method signature mismatch

`repository.findByInviteToken(UUID token)` — if JPA inferring SQL `WHERE invite_token = ?1::varchar` instead of `::uuid`, no match.

## Verify steps (resume when AWS restored)

```bash
# 1. Check table RLS policies
SSM → psql → \d beta_access_request → check Policies section
psql → SELECT polname, polcmd, pg_get_expr(polqual, polrelid) FROM pg_policy WHERE polrelid = 'beta_access_request'::regclass;

# 2. Query as admin role bypass RLS
psql → SET ROLE postgres; SELECT id, invite_token FROM beta_access_request WHERE invite_token = '98446443-e5cc-43e9-9498-6799d460d2db';

# 3. Check JPA query SQL
docker logs kitehub-subscription | grep "select.*beta_access_request" | tail -5
# Look for parameter binding type
```

## Production impact

🔴 100% beta signup deep-link flow broken. NO invitee can complete signup via email link (when email infra fixed per GAP-605/606/608).

## Proposed Fix

### If Hypothesis 1 (RLS):
- Either drop RLS on beta_access_request (public access required for unauthenticated signup), OR
- Add public-bypass policy: `USING (status IN ('PENDING', 'APPROVED') AND invite_token_expiry > NOW())` — allows public lookup of active invite tokens only
- Per `security-audit/SKILL.md` A01 — RLS must not break legitimate unauthenticated flows

### If Hypothesis 2/3 (UUID encoding):
- Add explicit `@Type(type = "pg-uuid")` annotation on entity field
- OR query method signature `findByInviteToken(@Param("token") UUID token)` with explicit `@Query("SELECT b FROM BetaAccessRequest b WHERE b.inviteToken = :token")`

## Acceptance Criteria

- [ ] Root cause identified (verify hypothesis 1/2/3)
- [ ] Fix applied + deployed staging.N
- [ ] Curl validate with known-good token → returns `{valid:true, email, name, orgName, persona}` HTTP 200
- [ ] FE `/beta-signup?token=<UUID>` page loads + pre-fills form correctly
- [ ] Integration test cover: public endpoint + RLS bypass scenario
- [ ] Companion gaps GAP-611 ID instances/payments/revenue 404 if same RLS root cause

## Related

- GAP-611 (sister — `POST /api/v1/auth/beta-signup` 404 — likely same root cause class)
- `security-audit/SKILL.md` A01 — RLS scope
- BetaAccessController.java + BetaAccessRequestRepository.java
- Wave 90 walkthrough evidence — DB row confirmed via direct psql `findByInviteToken` returns empty via JPA

## Log

- **2026-05-17:** Gap filed during Wave 90 walkthrough. FE deep-link returns "Token không hợp lệ hoặc đã hết hạn" mặc dù token tồn tại đúng. AWS account suspended mid-investigation; Hypothesis 1 (RLS) priority because identical pattern observed in `email_sent_log` policy (verified Wave 90 \d query).
