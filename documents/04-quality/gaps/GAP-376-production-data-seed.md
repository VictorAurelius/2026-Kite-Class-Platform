# GAP-376: Production Data Seed (Admin User + System Config)

**Status:** 🟡 PARTIAL
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

- [x] `ProductionSeedRunner` class implemented + idempotent (Wave 33 PR #895)
- [x] Admin user seed (password from secrets manager — see GAP-379) — runner wired, password injection via `SEED_ADMIN_PASSWORD` env (Wave 33)
- [x] System config records seed (Wave 33 — V27 baseline)
- [x] Instance zero seed (Wave 33 — platform tenant id=0)
- [x] Currencies + locales seed (Wave 33 — VND + vi via system_config)
- [x] Beta capacity config (Wave 33 — system_config.beta.max_tenants)
- [x] Run-as-CLI documented (Wave 33 — `scripts/seed-production.sh --help`; Wave 61 — runbook §3)
- [x] Smoke test: empty DB → run seed → verify all records present (Wave 61 — `STOP_WHEN_IDLE_E2E=1` cycle audit in `scripts/smoke-test.sh`)
- [x] Idempotent re-run test (run twice → no duplicates) (Wave 33 — ON CONFLICT semantics, integration test in `ProductionSeedRunnerIntegrationTest`)
- [x] Production deploy runbook updated with seed step (Wave 61 — `documents/05-guides/deploy/production-seed-runbook.md`)
- [ ] **Real production seed execution** (USER-EXECUTED first cutover step — per `agent-aws-access.md` Tier 3)

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

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. BLOCKING cho Phase 1 BETA first deploy.
- **2026-05-07:** Wave 33 Bucket A shipped (PR #895 — `ProductionSeedRunner` Spring Boot ApplicationRunner + `SeedProperties` @ConfigurationProperties + `SystemConfigSeedDao` + V27 idempotent INSERT-ON-CONFLICT-DO-NOTHING + `scripts/seed-production.sh` wrapper với --dry-run mode + 14 new tests, 407/407 module pass). Status 🔵 OPEN → 🟡 PARTIAL — runner + migration + script shipped, **production execution = user-executed step** (run `scripts/seed-production.sh` với `SEED_ADMIN_PASSWORD` env on first deploy). Seeds `admin@kitehub.vn` PLATFORM_ADMIN + system_config baseline (default_tier=FREE, currency=VND, locale=vi) + platform tenant id=0; NO demo content (separate optional script).
- **2026-05-11:** Wave 61 Bucket C shipped — (1) `documents/05-guides/deploy/production-seed-runbook.md` covering when-to-run + prerequisites + step-by-step §3.1-§3.7 + recovery §4 + rollback §5; (2) `scripts/smoke-test.sh` extended with `STOP_WHEN_IDLE_E2E=1` cycle audit (seed dry-run + ≤25min envelope + optional `STOP_WHEN_IDLE_STATE_FILE` JSONL log); (3) all agent-shippable AC ticked. Per `gap-done-discipline.md` §3 PARTIAL exit ramp — Status stays 🟡 PARTIAL pending real production seed execution (user-action first cutover step per `agent-aws-access.md` Tier 3 mutation ban). Forward-references Wave 61 Bucket D (`scripts/aws/start-stack.sh` + `stack-on-demand-runbook.md`) for stack resume/stop wrapper.
