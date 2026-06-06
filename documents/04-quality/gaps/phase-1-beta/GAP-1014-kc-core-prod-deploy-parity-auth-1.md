# GAP-1014: kiteclass-core production deploy path + PARENT_PORTAL_ENABLED override + secrets.tf HS512 desc (auth-1)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-06 (Wave auth-1 post-wave audit suite — ops-readiness P1×2 + P3)
**Affects:** `docker-compose.production.yml` + `scripts/fetch-secrets.sh` + `infrastructure/terraform-aws/secrets.tf`

## Problem

Production-parity gaps for the auth-1 surface (per `local-fix-production-parity-check.md`):

1. **`kiteclass-core` not in `docker-compose.production.yml`** (KC stack deferred Phase 7 / GAP-444) → the entire auth-1 surface has no production deploy path. `JWT_SECRET` is written to `/etc/kite/.env` (fetch-secrets.sh) but has no consuming service in prod compose.
2. **`PARENT_PORTAL_ENABLED` default `false` in production** — not overridden in fetch-secrets.sh / production compose → parent portal gated off even if KC deploys (handoff-known item, STILL PENDING).
3. **`secrets.tf` jwt-secret description stale "HS256"** (actually HS512); `random_password length=64` = exactly 64 bytes = HS512 minimum with zero margin.

## Proposed Fix

(1) Add `kc-core` service to `docker-compose.production.yml` (env_file passthrough covers JWT_SECRET) OR — if KC prod deploy genuinely Phase 7 — record explicit blocker dependency on GAP-444 + mark this gap deferred-with-deadline. (2) Add `PARENT_PORTAL_ENABLED=true` to production env source. (3) Fix secrets.tf description HS256→HS512; consider `length=88` for margin.

## Acceptance Criteria

- [ ] kc-core in production compose OR explicit GAP-444 blocker dependency recorded
- [ ] PARENT_PORTAL_ENABLED=true in production env source
- [ ] secrets.tf description = HS512; length margin decision recorded

## Related

- Audit report: `documents/04-quality/audits/ops-readiness/2026-06-06-wave-auth-1-ops-readiness.md`
- `local-fix-production-parity-check.md`; GAP-444 (KC stack deferral); GAP-612 (AWS restore — verify gated)
