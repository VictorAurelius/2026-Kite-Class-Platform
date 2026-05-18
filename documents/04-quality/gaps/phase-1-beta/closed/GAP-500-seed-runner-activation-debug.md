# GAP-500: ProductionSeedRunner không activate dù env present — Wave 67 blocker

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (Wave 67 entry — GAP-376 production seed)
**Domain:** Backend / DevOps
**Found:** 2026-05-13 (session retry #5 surfaced)
**Affects:** GAP-376 production data seed execution path

## Problem

Sau 5 retries fixing distinct issues, container env IS verified set (`KITE_SEED_MODE=production`, `KITE_SEED_ADMIN_EMAIL=admin@kitehub.me`) NHƯNG `ProductionSeedRunner` không activate. Zero log lines: no `starting`, no `skipped`, no `finished`.

## 5-retry log + distinct root causes

| Retry | Root cause | Fix PR | Status |
|---|---|---|---|
| 1 | Script absent on EC2 — chicken-and-egg | PR #1242 (git pull pre-call) | ✅ Fixed |
| 2 | AWS CLI `--parameters` shorthand can't escape nested quotes | PR #1243 (JSON heredoc) | ✅ Fixed |
| 3 | `docker compose restart` không reload env_file | PR #1244 (`up -d --force-recreate`) | ✅ Fixed |
| 4 | Compose variable substitution empty (`${KITE_VERSION}` etc.) | PR #1245 (`--env-file /etc/kite/.env`) | ✅ Fixed |
| 5 | **Env present in container, runner không activate** | UNKNOWN | ❌ Open |

## Verified facts (run #5 — workflow run 25780599618)

- `aws ssm exec ... printenv KITE_SEED_MODE` → `production` ✅
- `aws ssm exec ... printenv KITE_SEED_ADMIN_EMAIL` → `admin@kitehub.me` ✅
- Spring Boot startup logs present (Tomcat, Hibernate, HikariPool, Flyway up-to-date)
- `application.yml` không có hardcoded `kite.seed.mode` override
- ALB target healthy, `https://api.kitehub.me/actuator/health` → HTTP 200
- ZERO `ProductionSeedRunner` log lines (grep `-i seed` returned 0 in container logs)
- `Users in DB: 1` (AuthService log) — admin chưa exist với email `admin@kitehub.me` (need verify)

## Hypothesis (cần investigate fresh session)

### H1: Spring `@ConfigurationProperties` binding silently fails
- Env var `KITE_SEED_MODE` should map to `kite.seed.mode` via Spring relaxed binding
- Possibly Spring's `RelaxedPropertyResolver` doesn't kick in for some reason
- Test: enable DEBUG for `org.springframework.boot.context.properties` package

### H2: ApplicationRunner phase never fires
- Spring Boot startup may fail silently AFTER context loaded but BEFORE ApplicationRunner phase
- Maybe a `CommandLineRunner` / `ApplicationRunner` earlier throws
- Test: check if `Started KitehubSubscriptionApplication in X seconds` log appears

### H3: Logback config suppresses INFO for `com.kitehub.subscription.seed`
- Production logback may have logger-level filter
- Test: `docker exec ... cat /app/BOOT-INF/classes/logback-spring.xml`

### H4: SeedProperties bean not created (component scan miss)
- Less likely — class under `com.kitehub.subscription.seed.*` should scan
- Test: list created beans via actuator `/actuator/beans` (need actuator endpoint exposed)

## Proposed Redesign (next session)

Per `release-fix-retry-budget.md` §3 STOP-AND-REDESIGN, **don't patch retry #6** — redesign:

### Path A: Spring binding debug (30 min)
- Enable `DEBUG` logger for `org.springframework.boot.context.properties` + `com.kitehub.subscription.seed`
- Re-trigger seed workflow
- Surface root cause from binding logs
- Fix accordingly (may be single-line)

### Path B (PREFERRED): Bypass runner via Flyway migration (1 hour)
- New `V35__production_seed.sql` migration
- INSERT admin user with bcrypt hash (computed via Flyway placeholder `${admin_password_hash}`)
- INSERT system_config baseline + tenant id=0
- ON CONFLICT DO NOTHING (idempotent)
- Pass hash via env: `FLYWAY_PLACEHOLDERS_ADMIN_PASSWORD_HASH=$(python3 -c 'import bcrypt; ...')`
- Pros: deterministic, no Spring runtime dependency, Flyway-versioned
- Cons: re-implements logic, drift risk

### Path C: REST trigger endpoint (45 min)
- Add `POST /actuator/seed/run` to ProductionSeedRunner (admin-only auth)
- CI workflow `curl -X POST` post-deploy
- Pros: explicit invocation, no ApplicationRunner uncertainty
- Cons: code change + auth layer

### Path D: One-shot CLI mode (1.5 hour)
- Refactor `ProductionSeedRunner` to `SpringApplication.exit()` after run
- Add `--seed-mode=production` arg parser
- CI runs `java -jar app.jar --seed-mode=production` in one-shot container
- Pros: clean architecture, future-proof recovery scenarios
- Cons: most invasive

## Acceptance Criteria

- [ ] Root cause of retry #5 identified (Spring binding / lifecycle / logback / bean scan)
- [ ] Wave 67 seed succeeds — admin@kitehub.me user + system_config + tenant 0 in DB
- [ ] `gh workflow run seed-production.yml` (or equivalent) completes ~3-5 min
- [ ] GAP-376 + GAP-499 flip DONE

## Related

- Parent: GAP-376 (production data seed)
- Sister: GAP-499 (Wave 67 prerequisites — code/docs/secret prep)
- Rule violations log:
  - `release-fix-retry-budget.md` §3 STOP-AND-REDESIGN triggered at retry #5
  - `pre-mutation-state-check.md` §1.5 satisfied each retry but new bugs surfaced
- 5 fix PRs: #1242, #1243, #1244, #1245, plus current state where #5 unresolved

## Log

- **2026-05-13:** Filed after 5-retry budget exhausted on Wave 67 seed flow. Container env verified set but runner doesn't log activation. Multiple distinct root causes already fixed (script absent → CLI parse → restart-vs-recreate → compose var substitution). Retry #5 represents fundamentally different class — Spring lifecycle/binding issue, not infra plumbing. Per `release-fix-retry-budget.md` §3 pivot matrix, STOP-AND-REDESIGN per Path A/B/C/D evaluation next session.
