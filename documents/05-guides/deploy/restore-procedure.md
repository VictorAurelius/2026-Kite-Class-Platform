# Database & Asset Restore Procedure

> **Last Updated:** 2026-04-28 | **Owner:** DevOps / SRE | **Closes (Phase 1+2):** GAP-117

Step-by-step restore procedures cho các loại data loss khác nhau. Pair với [`rollback-procedure.md`](./rollback-procedure.md) — rollback handles application/migration revert; restore handles data recovery from backup.

> **A backup you haven't restored is not a backup.** Mỗi tháng có monthly drill ([restore-drill.yml](../../.github/workflows/restore-drill.yml)) để verify backup chain còn hoạt động.

---

## 1. When to Use Which Scenario

| Scenario | Trigger | RTO target | RPO target | Section |
|----------|---------|:----------:|:----------:|---------|
| **A: RDS Point-in-Time Restore** | Production RDS, single-table corruption hoặc accidental DELETE/UPDATE trong window | 30 min | 5 min | §3 |
| **B: pg_dump → Fresh DB** | Catastrophic DB loss, regional failure, restore vào throwaway env (dev/QA), monthly drill | 60 min | 24h (last nightly dump) | §4 |
| **C: MinIO Asset Restore** | Asset bucket corruption, accidental delete | 30 min | depends on versioning | §5 (stub — see GAP-118) |

**Decision tree:**

```
Data loss detected
   ├─ Production RDS (AWS)?
   │    ├─ Yes & PITR window covers it (default 7 days) → Scenario A
   │    └─ No → Scenario B
   ├─ Local/dev/staging Postgres?
   │    └─ Scenario B (pg_dump archive on S3)
   └─ MinIO/S3 assets?
        └─ Scenario C (depends on GAP-118 versioning landing)
```

See also: [`disaster-recovery-plan.md`](./disaster-recovery-plan.md) (created by GAP-119) cho platform-wide DR strategy + RTO/RPO matrix.

---

## 2. Prerequisites & Preflight

Trước khi chạy bất kỳ scenario nào:

- [ ] **Communicate** — báo team trong `#incidents`, lock writes if production
- [ ] **Identify backup source** — confirm latest backup file location (S3 path, RDS snapshot ID)
- [ ] **Identify backup metadata** — note `backup_metadata.json` key (row counts per table — used by `verify-restore.sh`)
- [ ] **Provision target** — empty Postgres (Docker container for drills, RDS instance for production)
- [ ] **Credentials** — `PGUSER`, `PGPASSWORD`, target host accessible
- [ ] **Tools installed** — `psql` (>=14), `pg_restore`, `aws` CLI (for S3), `jq`

---

## 3. Scenario A — RDS Point-in-Time Restore

**Use when:** production RDS instance, recent (≤ retention window) data corruption.

**RTO:** ~30 min (provisioning new RDS + connection cutover)
**RPO:** 5 min (RDS continuous backup granularity)

### Steps

```bash
# 1. Identify source RDS instance + recovery target time (UTC)
SOURCE_DB="kitehub-prod-db"
TARGET_TIME="2026-04-28T03:15:00Z"   # 5 min before incident
RESTORED_DB="${SOURCE_DB}-restored-$(date +%Y%m%d-%H%M)"

# 2. Trigger PITR (creates new instance — does NOT modify source)
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier "$SOURCE_DB" \
  --target-db-instance-identifier "$RESTORED_DB" \
  --restore-time "$TARGET_TIME" \
  --db-subnet-group-name kitehub-prod-subnet \
  --vpc-security-group-ids sg-xxxxxxxx

# 3. Wait for instance available (~15-25 min for db.r6g.large)
aws rds wait db-instance-available --db-instance-identifier "$RESTORED_DB"

# 4. Validate restore (see §6 Verification)
NEW_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier "$RESTORED_DB" \
  --query 'DBInstances[0].Endpoint.Address' --output text)
bash scripts/verify-restore.sh \
  --source-host="<known-good-host>" \
  --target-host="$NEW_HOST" \
  --db=kitehub

# 5. Cutover: update Secrets Manager / Helm values to point apps at $NEW_HOST
# (See rollback-procedure.md §3 cho detailed app rollover steps)

# 6. Decommission corrupted source after grace period (24h)
aws rds delete-db-instance --db-instance-identifier "$SOURCE_DB" \
  --final-db-snapshot-identifier "${SOURCE_DB}-final-$(date +%Y%m%d)"
```

### Gotchas

- PITR window default 7 days — older incidents need Scenario B
- New instance gets new endpoint; apps need restart sau khi update Secret
- Storage IOPS may differ — performance khác source first 24h (lazy-load from S3)
- Replicas NOT auto-recreated; manually recreate sau cutover

---

## 4. Scenario B — pg_dump → Fresh DB Restore

**Use when:** Local/dev restore drill, regional failure, RDS PITR window expired, restoring vào throwaway env.

**RTO:** 60 min (provisioning + restore + verify)
**RPO:** 24h (`DatabaseBackupScheduler` runs nightly — see GAP-093)

### 4.1 Locate latest backup

```bash
# Backups uploaded by DatabaseBackupScheduler (GAP-093)
S3_BUCKET="kite-backups-prod"
LATEST=$(aws s3 ls "s3://${S3_BUCKET}/postgres/" --recursive \
  | sort | tail -n 1 | awk '{print $4}')
echo "Latest backup: $LATEST"

# Download
aws s3 cp "s3://${S3_BUCKET}/${LATEST}" ./backup.sql.gz
aws s3 cp "s3://${S3_BUCKET}/${LATEST%.sql.gz}.metadata.json" ./backup.metadata.json
```

### 4.2 Provision empty target Postgres

**Production-equivalent (RDS):**

```bash
aws rds create-db-instance \
  --db-instance-identifier kitehub-restore-target \
  --db-instance-class db.r6g.large \
  --engine postgres --engine-version 16.4 \
  --master-username kitehub --master-user-password "$(openssl rand -base64 24)" \
  --allocated-storage 100
aws rds wait db-instance-available --db-instance-identifier kitehub-restore-target
```

**Local drill (Docker — what `restore-drill.yml` uses):**

```bash
docker run -d --name pg-restore-drill \
  -e POSTGRES_USER=kitehub \
  -e POSTGRES_PASSWORD=drill \
  -e POSTGRES_DB=kitehub \
  -p 5499:5432 \
  postgres:16-alpine
# Wait for ready
until docker exec pg-restore-drill pg_isready -U kitehub; do sleep 2; done
```

### 4.3 Restore

```bash
# pg_dump from GAP-093 is plain SQL gzipped
gunzip -c backup.sql.gz | \
  PGPASSWORD=drill psql -h localhost -p 5499 -U kitehub -d kitehub \
    --set ON_ERROR_STOP=on \
    -v VERBOSITY=terse \
    > restore.log 2>&1

# Confirm exit 0
echo "Restore exit: $?"
tail -20 restore.log
```

### 4.4 Verify

```bash
bash scripts/verify-restore.sh \
  --source-host=skip \
  --target-host=localhost:5499 \
  --db=kitehub \
  --metadata=./backup.metadata.json
echo "Verify exit: $?"   # 0 = PASS
```

### 4.5 Cleanup (drill only)

```bash
docker stop pg-restore-drill && docker rm pg-restore-drill
rm -f backup.sql.gz backup.metadata.json restore.log
```

---

## 5. Scenario C — MinIO / S3 Asset Restore (STUB)

> **Phase 1 stub — depends on [GAP-118](../04-quality/gaps/GAP-118-minio-s3-backup.md) landing.** Until GAP-118 implements bucket versioning + cross-region replication, manual MinIO/S3 asset restore = best effort from last `mc mirror` snapshot (if any).

### Forward-reference outline (will fill after GAP-118 ships)

1. Identify affected bucket + prefix (e.g. `tenant-{id}/branding/`)
2. Use S3/MinIO version history to list versions before incident timestamp
3. `aws s3api list-object-versions --bucket ... --prefix ...` → identify deletion markers / replaced versions
4. Restore via `aws s3api copy-object` from previous version OR cross-region replica
5. Verify object count + sample download
6. Update `BrandingService` cache (or wait for ETag-driven invalidation)

**Cross-link:** [`disaster-recovery-plan.md`](./disaster-recovery-plan.md) §MinIO/S3 (created by GAP-119) cho full DR perspective.

---

## 6. Verification Steps (all scenarios)

Use [`scripts/verify-restore.sh`](../../scripts/verify-restore.sh) — automated checks:

| Check | What it does | Pass criterion |
|-------|--------------|----------------|
| Schema match | Compare table list vs metadata expected | All expected tables present |
| Row count | `SELECT COUNT(*)` per critical table vs metadata | Within ±1% (drill noise tolerance) |
| FK integrity | Run query checking orphan child rows for top FKs | Zero orphans |
| Sample tenant read | Query 1 tenant + count related records (subscriptions, branding jobs) | Returns >0 rows, no SQL error |
| Flyway history | `flyway_schema_history` last entry success=true | TRUE |

**Manual checks beyond script:**

- [ ] Application boot against restored DB succeeds (`spring-boot:run` against new host) — log shows "Started ... in Xs"
- [ ] Sample API call works: `curl -sf $APP_URL/actuator/health` → `"status":"UP"`
- [ ] Sample tenant flow: login + read 1 protected resource (proves auth + RLS intact)
- [ ] Compare row counts vs production (if available) for top-5 tables
- [ ] Spot check 1 RabbitMQ outbox event published recently (proves event chain intact)

---

## 7. Drill Cadence

| Drill type | Frequency | Tool | Owner |
|-----------|:---------:|------|-------|
| Automated restore drill (Scenario B, fixture) | **Monthly** | [`restore-drill.yml`](../../.github/workflows/restore-drill.yml) — cronjob 1st of month | CI (auto-trigger) |
| Manual restore-from-prod-backup test | **Quarterly** | Run §4 against latest production S3 backup → throwaway RDS | DevOps lead |
| Full DR exercise (destroy staging, restore, verify app) | **Quarterly** | TBD — see GAP-117 Phase 3 (deferred) | DevOps + SRE |
| RDS PITR fire-drill | **Annually** | §3 against staging (don't test on prod!) | DevOps lead |

Phase 3 (quarterly DR exercise) deferred per `gap-done-discipline.md` §3 — requires staging env coordination + manual sign-off, separate follow-up gap.

---

## 8. Reporting & Incident Logging

Mỗi restore (drill OR real incident) phải log:

- Date + scenario used (A/B/C)
- Source backup identifier (S3 key, snapshot ID)
- Target host
- Wall-clock time (start → verify PASS)
- `verify-restore.sh` exit code + summary
- Issues hit + workarounds

**Storage:**
- **Drills:** GitHub Actions artifact retained 90 days (`restore-drill.yml`)
- **Real incidents:** New entry trong `documents/05-guides/operations/dr-rto-rpo-matrix.md` (created by GAP-119) §"Incident log"
- **Postmortem:** if real incident, file postmortem trong `documents/04-quality/postmortems/` per incident-response-runbook

---

## 9. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| "Last backup is from yesterday — should be fine" without verifying restore | Run drill monthly so verify chain stays green |
| Restore directly into production without verifying on throwaway first | Always Scenario B drill first if backup hasn't been restored in >30 days |
| Skip metadata file — "row count check is too strict" | Metadata is what makes drill detect silent corruption; never skip |
| Use `pg_restore -j N` parallel without checking target IOPS | Parallel restore can OOM small targets; default sequential for safety |
| Reuse drill target between runs | Always fresh container/instance — state from prior run hides regressions |
| Defer §6 verification "I'll check tomorrow" | Verification is the drill; without it = no proof backup is good |

---

## 10. Related

- [GAP-117](../04-quality/gaps/GAP-117-restore-drill-test.md) — this procedure closes Phase 1
- [GAP-118](../04-quality/gaps/GAP-118-minio-s3-backup.md) — MinIO/S3 backup (Scenario C dependency)
- [GAP-119](../04-quality/gaps/GAP-119-platform-dr-runbook.md) — platform-wide DR (cross-link)
- [GAP-093](../04-quality/gaps/GAP-093-database-backup.md) — backup implementation (DONE; this is the "what we're restoring")
- [`rollback-procedure.md`](./rollback-procedure.md) §4 Database Rollback — application/migration rollback (different concern)
- [`incident-response-runbook.md`](./incident-response-runbook.md) — broader incident triage
- [`scripts/verify-restore.sh`](../../scripts/verify-restore.sh) — automated verification
- [`.github/workflows/restore-drill.yml`](../../.github/workflows/restore-drill.yml) — monthly CI drill

---

## Log

- **2026-04-28** — Initial version. Phase 1+2 of GAP-117. Scenario C stubbed pending GAP-118. Phase 3 quarterly DR exercise deferred (separate follow-up).
