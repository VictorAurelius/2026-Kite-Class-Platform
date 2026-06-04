# GAP-935: Production `email.verification.enabled` missing override — defaults to `false`

**Status:** 🟢 DONE 2026-06-04 — Wave flow-kh1 G3 parity audit fix shipped same PR (kitehub-subscription/application-production.yml `email.verification.enabled: true`)
**Priority:** 🔴 P0 (security — without verification, a leaked invite token could complete signup against any email)
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh1 G3 parity audit per `local-fix-production-parity-check.md` §2 + wave plan §G3 checklist item 3)
**Affects:** `kitehub/kitehub-subscription/src/main/resources/application-production.yml`

## Problem

`kitehub-subscription/application.yml:192` declares `email.verification.enabled: ${EMAIL_VERIFICATION_ENABLED:false}` so local dev can skip the SES round-trip. The production profile (`application-production.yml`) inherited the default and did NOT override — `EMAIL_VERIFICATION_ENABLED` was never set in production env. Production deploys would have shipped with verification disabled.

Wave flow-kh1 wave plan §G3 row 3 + KH-1 production parity require verification enabled so SES delivers the verify token + the invitee proves email control before login is allowed.

## Fix

Add explicit override in production profile:

```yaml
email:
  verification:
    enabled: ${EMAIL_VERIFICATION_ENABLED:true}
```

Default flipped to `true` in production profile; the env var is still respected if production needs to disable temporarily for incident triage.

## Acceptance Criteria

- [x] `application-production.yml` explicit `email.verification.enabled: true` (or `${EMAIL_VERIFICATION_ENABLED:true}` default)
- [x] Comment cross-references Wave flow-kh1 G3 audit + SES production config
- [ ] Live production verify: post-deploy `curl POST /api/auth/register` → registration succeeds with `emailVerified=false`; SES sends the verify email; invitee click → `emailVerified=true` flips
- [ ] Per `production-env-config-registry.md` §3.1 — add `EMAIL_VERIFICATION_ENABLED` to env-vars-registry.md

## Related

- Wave flow-kh1 wave plan §G3 row 3
- Sister: GAP-934 (Cloudflare apex DNS terraform-import follow-up — G3 row 2)
- Per `local-fix-production-parity-check.md` §2 row 2 — `application.yml` `${VAR:default}` new entry must be checked against production profile override
- Per `pre-launch-secrets-hardening-checklist.md` §2.5 — production secrets must include SES API key + verification chain working
