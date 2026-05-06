# GAP-376: Production Data Seed (Admin User + System Config)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (Phase 1 BETA — first deploy fails without seed data)
**Domain:** Backend / DevOps
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** First production deploy success, system bootstrapping

## Problem

Empty database trên first production deploy. Cần seed:
- Admin user (root/superuser cho coordinator initial access)
- System config records (default tier definitions, default branding template, etc.)
- KiteHub instance row (tenant 0 = platform)
- System enums seed (subjects taxonomy, currencies, languages)
- Demo content (optional — sample tenant cho marketing screenshots)

## Proposed Fix

### Seed approach decision

**Option A — Flyway migration:**
- Add seed data trong `V[N]__seed_production.sql`
- Pro: versioned, repeatable
- Con: data + schema mixed; harder to update

**Option B — Spring Boot seed runner:**
- `--command=seed-production` CLI flag
- Pro: separation of concerns
- Con: extra tooling

**Option C — Idempotent seed script:**
- `scripts/seed-production.sh` chạy bất kỳ time
- Pro: simple, manual control
- Con: less automation

**Recommend Option B** — Spring Boot seed runner với env-aware (different seeds for dev/staging/prod).

### Seed data scope (Phase 1 BETA)

```yaml
admin_user:
  email: admin@kitehub.vn
  password: <vault-secret>  # rotate immediately post-deploy
  role: PLATFORM_ADMIN

system_config:
  default_tier: FREE
  default_currency: VND
  default_locale: vi
  branding_template: minimal-default

instance_zero:
  id: 0
  name: "KiteHub Platform"
  type: SYSTEM

subjects_taxonomy:
  # MOET subjects seed (Toán, Văn, Anh, ...) — per GAP-327 if applicable
  # Skip if GAP-327 not yet shipped

currencies:
  - VND (default)
  - USD (future internationalization)

beta_capacity:
  max_tenants: 20
  max_invite_pending: 50
```

### Implementation

- `kitehub-shared/src/main/java/com/kite/hub/seed/ProductionSeedRunner.java`
  - `@ConditionalOnProperty(name = "kite.seed.run", havingValue = "true")`
  - Idempotent (check existence before insert)
  - Transactional
  - Logs each entity created/skipped
- Activate via env var: `KITE_SEED_RUN=true java -jar app.jar`

### Phase 1.5 PAID expansion

When transitioning to v1.0.0:
- Migrate beta tenants to v1.0.0 (data migration)
- Update beta_capacity → unlimited
- Add new system configs (payment processor settings, eInvoice templates per GAP-185)

## Acceptance Criteria

- [ ] `ProductionSeedRunner` class implemented + idempotent
- [ ] Admin user seed (password from secrets manager — see GAP-379)
- [ ] System config records seed
- [ ] Instance zero seed
- [ ] Currencies + locales seed
- [ ] Beta capacity config
- [ ] Run-as-CLI documented
- [ ] Smoke test: empty DB → run seed → verify all records present
- [ ] Idempotent re-run test (run twice → no duplicates)
- [ ] Production deploy runbook updated with seed step

## Open decisions

- Subjects taxonomy seed (Phase 1 BETA vs Phase 3 K-12)
- Demo tenant seed (yes/no for screenshots)
- Admin user email (admin@kitehub.vn vs ops@kitehub.vn)

## Effort estimate

~1 ngày BE + ~half day testing.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md` §2.2 step 8
- Sister: GAP-379 (secrets management — admin password storage)
- Cross-cut: GAP-327 (MOET subject taxonomy seed — defer Phase 3)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. BLOCKING cho Phase 1 BETA first deploy.
