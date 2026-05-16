# RDS Storage Runbook — phát hiện + xử lý storage thấp (GAP-583 H-AC2)

**Owner:** Solo dev / on-call
**Created:** 2026-05-16 (Wave 86 Bucket H H-AC2)
**Last Updated:** 2026-05-16
**Related gap:** GAP-583 P1
**Related alarm:** `kitehub-rds-storage-low` (CloudWatch)
**Related SNS topic:** `kitehub-production-alerts`

---

## 1. Bối cảnh

Phase 1 BETA chạy RDS `kitehub-postgres`:

- Instance class: `db.t3.micro`
- Allocated storage: **20 GB** gp3
- Storage autoscaling: **DISABLED** (cost saving per Wave 86 Bucket A simulation-3axis audit cell 8)
- Estimated 30-day reach: ~12 GB ở Phase 1 BETA baseline (5 tenant trials + audit logs + Flyway history)

Vì autoscale tắt, storage fill silently khi vượt threshold sẽ:
- Block ghi mới → connection refused → tenant không tạo được lớp/student
- Trigger maintenance event tự động (AWS auto-resize emergency = ~$2/GB premium)

Runbook này define detection + resize procedure để xử lý chủ động.

---

## 2. Detection (alarm fire signal)

CloudWatch alarm `kitehub-rds-storage-low` fires khi:

- Metric: `AWS/RDS / FreeStorageSpace` (DBInstanceIdentifier=`kitehub-postgres`)
- Threshold: `< 5 GB` (5,368,709,120 bytes)
- Evaluation: 2 datapoints × 5 min = 10 phút sustained
- Notification: SNS topic `kitehub-production-alerts` → support@kitehub.me + vannkite@outlook.com

**Khi nhận email:**

```
Subject: ALARM: "kitehub-rds-storage-low" in AP (Singapore)
You are receiving this email because your Amazon CloudWatch Alarm
"kitehub-rds-storage-low" in the AP (Singapore) region has entered
the ALARM state...
```

→ Mở runbook này + chạy diagnosis (§3).

---

## 3. Diagnosis

### 3.1 Verify current state

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws rds describe-db-instances \
    --db-instance-identifier kitehub-postgres \
    --query 'DBInstances[0].[AllocatedStorage,StorageType,StorageThroughput,MaxAllocatedStorage]' \
    --output table

AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws cloudwatch get-metric-statistics \
    --namespace AWS/RDS \
    --metric-name FreeStorageSpace \
    --dimensions Name=DBInstanceIdentifier,Value=kitehub-postgres \
    --start-time $(date -u -d '6 hours ago' '+%Y-%m-%dT%H:%M:%SZ') \
    --end-time $(date -u '+%Y-%m-%dT%H:%M:%SZ') \
    --period 300 \
    --statistics Average \
    --query 'Datapoints[*].[Timestamp,Average]' \
    --output table
```

Output cho biết:
- Allocated vs free hiện tại
- Trend 6h gần nhất (decay rate)

### 3.2 Identify top consumer

SSM tunnel vào postgres (per `documents/05-guides/operations/stack-on-demand-runbook.md`):

```sql
-- Top 10 table by size
SELECT
  schemaname,
  relname AS table_name,
  pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
  pg_total_relation_size(relid) AS bytes
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC
LIMIT 10;

-- Database total
SELECT
  pg_database.datname,
  pg_size_pretty(pg_database_size(pg_database.datname)) AS db_size
FROM pg_database
ORDER BY pg_database_size(pg_database.datname) DESC;

-- Largest indexes
SELECT
  schemaname,
  indexrelname AS index_name,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 10;
```

**Common Phase 1 BETA culprits:**

| Bảng | Lý do tăng | Mitigation |
|------|-----------|-----------|
| `admin_audit_logs` | PDPL Art 11 retention (V60 immutable) | Apply audit-log retention policy — `audit-log-retention-runbook.md` |
| `flyway_schema_history` | Quá nhiều migration repeat | Acceptable; chỉ ~MB scale |
| `oauth2_authorization` | Spring Auth Server token persist | Cleanup expired tokens cron |
| `lesson_attendance_record` | Tenant active ghi nhiều | Acceptable growth; resize |

---

## 4. Resize procedure

### 4.1 Path A — Manual scale up (recommended Phase 1 BETA)

**Bước 1:** Update `infrastructure/terraform-aws/rds.tf`:

```hcl
resource "aws_db_instance" "main" {
  # ... existing fields ...
  allocated_storage     = 30  # was 20, bump +10GB
  max_allocated_storage = 30  # tắt autoscale, giới hạn hard
  # ...
}
```

**Bước 2:** Commit + tạo PR.

**Bước 3:** Trigger terraform apply qua workflow_dispatch (per `release-deploy-standard.md` §9):

```bash
gh workflow run terraform-apply.yml --ref main \
  -f confirm=APPLY -f dry_run=true -f version=main
```

Verify plan: `1 to change` (storage 20→30, no replace).

**Bước 4:** Apply:

```bash
gh workflow run terraform-apply.yml --ref main \
  -f confirm=APPLY -f dry_run=false -f version=main
```

AWS in-place storage modify takes ~5-10 phút (no downtime cho gp3).

**Bước 5:** Verify alarm clear:

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws cloudwatch describe-alarms \
    --alarm-names kitehub-rds-storage-low \
    --query 'MetricAlarms[0].StateValue'
```

Expected: `OK` trong 10-15 phút.

**Cost impact:** +10GB gp3 = ~$1.25/month (gp3 $0.125/GB-month Singapore region).

### 4.2 Path B — Enable storage autoscaling (Phase 1.5+ recommended)

**Khi nào dùng:** Phase 1.5 paid tenants → unpredictable growth → autoscale buffer reasonable.

```hcl
resource "aws_db_instance" "main" {
  # ...
  allocated_storage     = 20
  max_allocated_storage = 100  # autoscale up to 100GB
  # ...
}
```

Apply như Path A. AWS sẽ tự bump allocated_storage khi free space < 10% (default trigger). Mỗi auto-scale event = +10GB step.

**Trade-off:** không control được khi nào scale; cost grow silently.

### 4.3 Path C — Emergency manual override (alarm CRITICAL, business blocked)

Nếu cần immediate (CI block / tenant tạo lớp fail):

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws rds modify-db-instance \
    --db-instance-identifier kitehub-postgres \
    --allocated-storage 30 \
    --apply-immediately
```

⚠️ **Cảnh báo:** bypass terraform → state drift. PHẢI commit + apply terraform `rds.tf` change đồng bộ trong vòng 24h để eliminate drift. Per `pre-mutation-state-check.md`.

---

## 5. Recovery validation

Sau resize:

1. **Alarm state:** `aws cloudwatch describe-alarms --alarm-names kitehub-rds-storage-low --query 'MetricAlarms[0].StateValue'` → `OK`
2. **Free space:** verify ≥ 8GB free (40%+ headroom for 30GB allocated)
3. **App health:** smoke test backend API tạo dữ liệu mới (`/api/v1/health` + create test record)
4. **Email confirmation:** SNS topic gửi OK email khi alarm transition ALARM → OK

---

## 6. Prevention — schedule review

Monthly review query (manual until automation lands):

```sql
-- DB total + table growth rate
SELECT
  current_database() AS db,
  pg_size_pretty(pg_database_size(current_database())) AS size,
  now() AS reported_at;
```

Track trong audit log spreadsheet (per `cohort-retention-tracking.md` Phase 1 BETA tracking pattern).

**Trigger resize sớm khi:**
- Used > 50% (10GB used / 20GB allocated) — Path A trong 1 tuần
- Used > 70% (14GB used) — Path A trong 24h
- Used > 80% (16GB used) — Path C emergency

---

## 7. Related

- Gap: GAP-583 P1 (Wave 86 Bucket H H-AC2)
- Alarm: `kitehub-rds-storage-low` (terraform: `infrastructure/terraform-aws/production-alerts.tf`)
- SNS topic: `kitehub-production-alerts`
- Sister runbooks:
  - `stack-on-demand-runbook.md` — start/stop RDS for SSH tunnel
  - `disaster-recovery-plan.md` — RPO/RTO matrix
  - `audit-log-retention-runbook.md` — PDPL Art 11 retention (largest table source)
- Rules: `pre-mutation-state-check.md`, `release-deploy-standard.md` §9, `deployment-naming-convention.md` (operations/ recurring)
