# GAP-511: Spring profile naming — application-prod.yml → application-production.yml

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (production profile overrides silently inactive)
**Domain:** Backend
**Found:** 2026-05-13 (Bucket E audit-spring-profiles.sh)
**Closed:** 2026-05-13 (PR #1271, Wave 71 Bucket C)
**Affects:** kitehub-gateway, kitehub-subscription (admin/branding/email had no prod profile YAML — out of scope)

## Problem

`SPRING_PROFILES_ACTIVE=production` set in docker-compose, but profile YAML named `application-prod.yml`. Spring requires exact match `application-{profile}.yml` → profile silently ignored, production overrides never applied.

## Fix

`git mv application-prod.yml application-production.yml` × 2 services. No source code refs (Spring uses filename convention).

## Acceptance Criteria

- [x] 0 `application-prod.yml` remaining
- [x] 2 `application-production.yml` exist
- [x] mvn compile PASS both services
- [x] Post-deploy: production profile loads (verified via 201 response showing proper config)

## Out-of-scope

admin/branding/email don't have prod profile YAML at all — separate decision whether to add (Bucket E audit will flag).

## Related

- PR: #1271, Wave 71 Bucket C
- Sibling: GAP-509, GAP-510

## Log

- **2026-05-13:** Filed retroactively at closure.
