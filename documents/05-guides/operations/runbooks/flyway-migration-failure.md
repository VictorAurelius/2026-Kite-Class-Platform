# Runbook: Flyway Migration Failure

**Alert:** `FlywayMigrationFailure`
**Severity:** critical
**Last updated:** 2026-04-28

## What does this alert mean?

A service started up and Flyway either rejected applying a migration (checksum mismatch, syntax error, missing dependency) OR Hibernate's `validate` schema check failed because applied migrations don't match entity model. The alert fires on `kite_flyway_migration_failed_total > 0` (counter incremented at boot) OR via service `up == 0` correlated with a recent deploy. **A failed migration blocks the entire service from booting** — every replica that restarts will keep crashlooping until either the migration is fixed forward or rolled back. Production deploys are gated on this alert via the Go/No-go checklist.

## Note

> If the metric `kite_flyway_migration_failed_total` is not yet emitted (Spring Boot Flyway integration doesn't expose Micrometer counter by default), the alert may surface as `ServiceDown` correlated with deploy timestamp. A small `@EventListener(FlywayMigrationFailedEvent.class)` bean must register the counter; track instrumentation under follow-up gap.

## Immediate checks (0-5 min)

1. **Identify the failing migration:**
   ```bash
   kubectl logs -n kitehub deploy/<service> --tail=300 \
     | grep -E 'Flyway|FlywayException|Migration failed|checksum|ValidationFail' -A 10
   # Look for "Migration V##__name.sql failed" — note the version + reason
   ```
2. **Cross-check `flyway_schema_history` table:**
   ```bash
   docker exec kite-postgres psql -U postgres -d kitehub -c \
     "SELECT installed_rank, version, description, type, success, installed_on \
      FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;"
   # Failed migrations have success=false; do NOT manually delete those rows blindly.
   ```
3. **Inspect the migration file in source:**
   ```bash
   ls kitehub/<service>/src/main/resources/db/migration/ | tail -10
   git log --oneline -- kitehub/<service>/src/main/resources/db/migration/V## | head -5
   ```
4. **Pre-deploy state check** — was this service healthy before the latest release? `gh run list --workflow=docker-build-push.yml --limit 5`

## Likely causes

- **Schema validation drift (V29+ created_by column)** → see `feedback_dev_profile_schema_workaround.md`. V29+ migrations declare `created_by VARCHAR(100)` but `BaseEntity.createdBy` is `Long` — Hibernate boot-time validation rejects schema. **Fix:** in PROD, **never** bypass with `ddl-auto: create-drop`. Roll back the service version (see Mitigation), file a fix-forward migration. GAP-244 tracks the canonical fix.
- **JSONB column missing `@JdbcTypeCode(SqlTypes.JSON)`** → see `feedback_jpa_jsonb_jdbctypecode.md`. `@Column(columnDefinition = "jsonb")` without `@JdbcTypeCode` causes Postgres to reject VARCHAR bind on the very first INSERT after migration applies. **Fix:** annotate entity, add a smoke insert in startup health check.
- **Checksum mismatch** — a migration file was edited *after* it was applied to some environment. Flyway hashes file at apply-time and refuses re-apply with new content. **Fix:** never edit a previously-applied migration; create a new V##+1 migration that adjusts. Use `flyway repair` only with explicit DBA approval.
- **Missing Postgres extension** — migration uses `CREATE EXTENSION IF NOT EXISTS pgcrypto` but the DB user lacks SUPERUSER. **Fix:** pre-create extensions in cluster bootstrap, drop them from per-service migrations.
- **Out-of-order migration** — two services share the same DB and shipped overlapping V## versions. **Fix:** namespace prefixes per service (e.g. `V01.001__subscription_init.sql` vs `V01.001__branding_init.sql`); enable `outOfOrder=true` in `application.yml` only after audit.
- **Migration takes longer than liveness probe timeout** — large `ALTER TABLE ... ADD COLUMN` on multi-million-row table; pod killed mid-migration leaves DB in inconsistent state. **Fix:** set Flyway connection liveness timeout and pod startup probe `failureThreshold` accordingly.

## Mitigation

```bash
# 1. ROLL BACK the service to last known good version (do NOT attempt schema rewrite under pressure)
kubectl rollout undo deployment/<service> -n kitehub
# Verify rollback
kubectl rollout status deployment/<service> -n kitehub --timeout=120s

# 2. Capture the failure for forensics BEFORE any flyway repair
kubectl logs -n kitehub deploy/<service> --previous --tail=500 > /tmp/migration-fail-$(date +%s).log
docker exec kite-postgres pg_dump -U postgres -t flyway_schema_history kitehub \
  > /tmp/flyway-history-$(date +%s).sql

# 3. If failed migration row blocks redeploy AFTER source fix is shipped:
#    flyway repair removes failed rows; ONLY run with DBA approval and after fix is in source
docker exec kite-postgres psql -U postgres -d kitehub -c \
  "DELETE FROM flyway_schema_history WHERE success = false;"
# Then re-deploy with the fixed migration; Flyway re-applies clean.

# 4. NEVER use ddl-auto: create-drop or update in production — see feedback file
```

After mitigation, run the next deploy through staging first AND verify on a fresh equivalent environment per `gap-done-discipline.md` §6. Add a regression test in CI that asserts `flyway info` returns zero `failed` migrations after boot.

## When to escalate

- Rollback fails OR rollback target also has the same failed migration → escalate to platform lead; potentially restore from backup (see [`backup-job-failure.md`](./backup-job-failure.md))
- Multi-service deploy chain affected (kitehub-subscription + kitehub-billing migrations both fail) → invoke DR runbook
- Schema validation error mentioning `wrong column type` → escalate to GAP-244 owner; do NOT attempt schema rewrite during incident

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Memory: `feedback_dev_profile_schema_workaround.md`, `feedback_jpa_jsonb_jdbctypecode.md`, `feedback_thymeleaf_ognl_pin.md`
- Related runbooks: [`service-down.md`](./service-down.md), [`backup-job-failure.md`](./backup-job-failure.md), [`../../rollback-procedure.md`](../../rollback-procedure.md)
