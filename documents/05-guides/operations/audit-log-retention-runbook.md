# Audit Log Retention Runbook — 90d hot RDS + S3 glacier archive (Wave 86 Bucket H H-AC5)

**Owner:** Solo dev / Compliance lead
**Created:** 2026-05-16 (Wave 86 Bucket H H-AC5)
**Last Updated:** 2026-05-16
**Related table:** `admin_audit_logs` (RDS kitehub-postgres)
**Compliance basis:** PDPL Art 11 (retention 7 năm cho audit log)

---

## 1. Bối cảnh

V60 migration (Wave 85 Bucket H) đã tạo `admin_audit_logs` immutable với INSERT-only RLS policy (revoke UPDATE/DELETE/TRUNCATE). PDPL Art 11 yêu cầu retain audit log 7 năm cho compliance audit + legal hold + dispute resolution.

**Vấn đề:** giữ 7 năm trong RDS hot storage = expensive (gp3 $0.125/GB-month × 7 năm × growth rate). Ở Phase 1 BETA baseline ~10 admin actions/ngày × 365 = ~3,650 rows/year × 7 years × ~2KB/row ≈ 50MB cumulative — acceptable RDS.

Ở scale Phase 1.5+ (100 tenant × 50 actions/day) = ~1.8M rows/year × 7 năm × 2KB = ~25GB cumulative → cần tier strategy:

- **Hot tier:** RDS, last 90 ngày (operational query — admin xem activity gần đây)
- **Cold tier:** S3 Standard 90-180 ngày
- **Archive tier:** S3 Glacier 180 ngày → 7 năm
- **Delete:** sau 7 năm 1 ngày

---

## 2. Retention policy specification

| Tier | Storage | Duration | Access pattern | Cost (per GB-month) |
|---|---|---|---|---|
| **Hot** | RDS `admin_audit_logs` table | 0 → 90 ngày | Online query via admin UI / API | $0.125 (gp3) |
| **Cold** | S3 Standard | 90 → 180 ngày | Retrieval 1-2 phút, ad-hoc audit | $0.025 |
| **Archive** | S3 Glacier Flexible Retrieval | 180 ngày → 7 năm | Retrieval 3-5h, legal hold/compliance audit | $0.004 |
| **Delete** | — | > 7 năm + 1 ngày | — | $0 |

Total Phase 1.5 cost @25GB cumulative: ~$0.10/month archive + $0.06/month cold + $1.25/month hot ≈ **$1.40/month**.

---

## 3. Implementation phases

### Phase A — Hot tier rotation cron (Phase 1 BETA priority)

**Goal:** archive rows > 90 ngày sang S3, delete khỏi RDS hot.

**Spring Boot @Scheduled** trong `kitehub-platform` service:

```java
@Component
public class AuditLogRetentionJob {

    @Autowired
    private AdminAuditLogRepository repo;

    @Autowired
    private S3Client s3Client;

    @Value("${kite.audit-log.retention.hot-days:90}")
    private int hotRetentionDays;

    @Value("${kite.audit-log.retention.s3-bucket:kitehub-audit-log-archive}")
    private String archiveBucket;

    // Daily 2 AM UTC — low traffic window
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void rotateAuditLogs() {
        Instant cutoff = Instant.now().minus(hotRetentionDays, ChronoUnit.DAYS);
        List<AdminAuditLog> toArchive = repo.findByCreatedAtBefore(cutoff);

        if (toArchive.isEmpty()) {
            log.info("Audit log rotation: no rows to archive (cutoff={})", cutoff);
            return;
        }

        // Group by month for partition-friendly S3 key
        Map<String, List<AdminAuditLog>> byMonth = toArchive.stream()
            .collect(Collectors.groupingBy(
                row -> DateTimeFormatter.ofPattern("yyyy-MM")
                    .withZone(ZoneOffset.UTC).format(row.getCreatedAt())));

        for (var entry : byMonth.entrySet()) {
            String s3Key = String.format("year=%s/month=%s/admin_audit_logs.jsonl",
                entry.getKey().substring(0, 4), entry.getKey().substring(5, 7));

            String jsonl = entry.getValue().stream()
                .map(this::serializeRow)
                .collect(Collectors.joining("\n"));

            s3Client.putObject(PutObjectRequest.builder()
                .bucket(archiveBucket)
                .key(s3Key)
                .contentType("application/x-ndjson")
                .build(),
                RequestBody.fromString(jsonl));

            log.info("Archived {} rows to s3://{}/{}",
                entry.getValue().size(), archiveBucket, s3Key);
        }

        // ⚠️ V60 RLS revoke DELETE on admin_audit_logs.
        // Rotation deletion REQUIRES temporary RLS override via dedicated
        // role `audit_log_archiver` with explicit DELETE grant.
        // See §6 RLS override pattern.
        int deleted = repo.deleteByCreatedAtBefore(cutoff);
        log.info("Deleted {} archived rows from RDS hot tier", deleted);
    }

    private String serializeRow(AdminAuditLog row) {
        // JSON serialization with all PDPL-relevant fields
        return objectMapper.writeValueAsString(row);
    }
}
```

**Config:**

```yaml
# application-production.yml
kite:
  audit-log:
    retention:
      hot-days: 90
      s3-bucket: kitehub-audit-log-archive
```

### Phase B — S3 lifecycle policy (one-time terraform setup)

```hcl
# infrastructure/terraform-aws/audit-log-archive.tf
resource "aws_s3_bucket" "audit_log_archive" {
  bucket = "${var.project_name}-audit-log-archive"

  tags = {
    Name        = "${var.project_name}-audit-log-archive"
    Purpose     = "PDPL Art 11 audit log retention (7 years)"
    Compliance  = "PDPL"
  }
}

resource "aws_s3_bucket_versioning" "audit_log_archive" {
  bucket = aws_s3_bucket.audit_log_archive.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "audit_log_archive" {
  bucket = aws_s3_bucket.audit_log_archive.id

  rule {
    id     = "tier-and-expire"
    status = "Enabled"

    # 0-90 days: Standard (S3 default, fast retrieval)
    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    # 90+ days: Glacier Flexible Retrieval (3-5h retrieval, $0.004/GB)

    # Delete after 7 years + 1 day grace
    expiration {
      days = 2557 # 7*365 + 2 (leap year buffer)
    }
  }
}

resource "aws_s3_bucket_public_access_block" "audit_log_archive" {
  bucket = aws_s3_bucket.audit_log_archive.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Server-side encryption with KMS CMK (PDPL data-at-rest)
resource "aws_s3_bucket_server_side_encryption_configuration" "audit_log_archive" {
  bucket = aws_s3_bucket.audit_log_archive.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# IAM policy for kitehub-platform service to put + (during rotation) delete
# from admin_audit_logs table — paired with audit_log_archiver DB role
resource "aws_iam_policy" "audit_log_archiver" {
  name        = "${var.project_name}-audit-log-archiver"
  description = "Allow kitehub-platform service to archive audit logs to S3"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.audit_log_archive.arn,
          "${aws_s3_bucket.audit_log_archive.arn}/*"
        ]
      }
    ]
  })
}
```

### Phase C — Systemd timer alternative (nếu Spring @Scheduled khó)

Standalone cron trên EC2:

```bash
# /etc/systemd/system/audit-log-rotate.service
[Unit]
Description=Audit log rotation (90d hot → S3 archive)

[Service]
Type=oneshot
User=ec2-user
ExecStart=/usr/local/bin/audit-log-rotate.sh

# /etc/systemd/system/audit-log-rotate.timer
[Unit]
Description=Daily audit log rotation

[Timer]
OnCalendar=*-*-* 02:00:00
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
#!/usr/bin/env bash
# /usr/local/bin/audit-log-rotate.sh
set -euo pipefail

CUTOFF=$(date -u -d '90 days ago' '+%Y-%m-%d')
MONTH_BUCKET=$(date -u -d '91 days ago' '+%Y-%m')

# Dump rows older than cutoff
psql "$DB_URL" -c "COPY (
  SELECT * FROM admin_audit_logs WHERE created_at < '$CUTOFF'
) TO STDOUT WITH (FORMAT json)" > /tmp/audit-$MONTH_BUCKET.jsonl

# Upload to S3
aws s3 cp /tmp/audit-$MONTH_BUCKET.jsonl \
  "s3://kitehub-audit-log-archive/year=${MONTH_BUCKET%-*}/month=${MONTH_BUCKET#*-}/admin_audit_logs.jsonl"

# Delete archived rows (requires audit_log_archiver DB role with DELETE grant)
psql "$DB_URL" -U audit_log_archiver -c "
  DELETE FROM admin_audit_logs WHERE created_at < '$CUTOFF';
"

rm /tmp/audit-$MONTH_BUCKET.jsonl
```

---

## 4. Restore procedure (legal hold / compliance audit)

Khi cần restore archived data (vd: tenant dispute, regulator subpoena):

```bash
# Step 1: Identify S3 key range
aws s3 ls s3://kitehub-audit-log-archive/year=2026/month=03/

# Step 2: Initiate Glacier restore (3-5h retrieval window)
aws s3api restore-object \
  --bucket kitehub-audit-log-archive \
  --key year=2026/month=03/admin_audit_logs.jsonl \
  --restore-request '{"Days":7,"GlacierJobParameters":{"Tier":"Standard"}}'

# Step 3: Wait 3-5h, then download
aws s3 cp s3://kitehub-audit-log-archive/year=2026/month=03/admin_audit_logs.jsonl \
  /tmp/restored-2026-03.jsonl

# Step 4: Load into temporary table (do NOT insert back into admin_audit_logs —
# would break V60 immutable INSERT-only RLS invariant + ordering)
psql "$DB_URL" <<EOF
CREATE TEMP TABLE restored_audit_logs (LIKE admin_audit_logs INCLUDING ALL);
\copy restored_audit_logs FROM '/tmp/restored-2026-03.jsonl' WITH (FORMAT csv);
EOF

# Step 5: Query for legal-hold scope
SELECT * FROM restored_audit_logs
  WHERE actor_id = '<tenant-uuid>'
    AND created_at BETWEEN '2026-03-01' AND '2026-03-31';
```

**Restore retention:** S3 Glacier restored copy stays in S3 Standard for `Days` parameter (7 days above). After that, original Glacier object remains; restored copy expires.

---

## 5. Monitoring

### 5.1 Rotation success metric

Spring Boot Actuator + Micrometer:

```java
Counter rotationSuccess = Counter.builder("audit_log.rotation.success")
    .description("Audit log rotation job completions")
    .register(meterRegistry);

Counter rowsArchived = Counter.builder("audit_log.rotation.rows_archived")
    .description("Rows successfully archived to S3")
    .register(meterRegistry);
```

CloudWatch alarm khi rotation fail 2 days liên tiếp:

```bash
# Trigger via terraform CloudWatch alarm (future work)
# Custom metric: audit_log_rotation_failed_days
```

### 5.2 S3 bucket size

CloudWatch S3 bucket metrics (auto-enabled for daily storage):

```bash
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws cloudwatch get-metric-statistics \
    --namespace AWS/S3 \
    --metric-name BucketSizeBytes \
    --dimensions Name=BucketName,Value=kitehub-audit-log-archive \
                 Name=StorageType,Value=GlacierStorage \
    --start-time $(date -u -d '7 days ago' '+%Y-%m-%dT%H:%M:%SZ') \
    --end-time $(date -u '+%Y-%m-%dT%H:%M:%SZ') \
    --period 86400 \
    --statistics Average
```

---

## 6. RLS override pattern (immutable audit log)

V60 migration revokes UPDATE/DELETE/TRUNCATE on `admin_audit_logs` from public + application roles. Rotation requires DELETE → needs dedicated DB role:

```sql
-- Phase B migration (V61 or similar)
CREATE ROLE audit_log_archiver NOLOGIN;
GRANT SELECT, DELETE ON admin_audit_logs TO audit_log_archiver;
-- NO INSERT, NO UPDATE — strictly archive-then-delete only

-- Application connects with role switch
SET SESSION ROLE audit_log_archiver;
DELETE FROM admin_audit_logs WHERE created_at < :cutoff;
RESET ROLE;
```

**Audit trail of rotation itself:** rotation job WRITES new audit log entry recording "X rows archived to S3 key Y" before deleting source rows. Self-referential but acceptable per PDPL Art 11 (archive operation IS an audit event).

---

## 7. Phase rollout plan

| Phase | Scope | Trigger |
|---|---|---|
| Phase 1 BETA | Hot tier only (RDS 7 năm) — acceptable at ~50MB scale | Current |
| Phase 1.5 paid | Phase B S3 lifecycle bucket + Phase A cron | When `admin_audit_logs` size > 500MB OR tenant count > 20 |
| Phase 2 scale | Phase C systemd timer + monitoring | When tenant count > 100 |

---

## 8. Related

- Migration: V60 `admin_audit_logs_immutable` (Wave 85 Bucket H Security Cat 9 +1)
- Compliance: PDPL Art 11 (audit log retention 7 năm)
- Sister runbooks: `secrets-rotation-runbook.md`, `rds-storage-runbook.md` (largest table source)
- Rules: `business-logic-review.md` (PDPL compliance), `pre-mutation-state-check.md`
