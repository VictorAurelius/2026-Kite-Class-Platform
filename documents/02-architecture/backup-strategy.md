# KiteHub Backup Strategy

## Database Backup

### Local Development
- PostgreSQL data persisted via Docker volume `kitehub-postgres-data`
- Manual backup: `docker exec kitehub-postgres pg_dump -U kitehub kitehub > backup.sql`

### Production (Oracle Cloud / AWS)

**Automated Daily Backup:**
```bash
#!/bin/bash
# /opt/kitehub/scripts/backup-db.sh
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/opt/kitehub/backups
RETENTION_DAYS=30

# Dump database
pg_dump -h localhost -U kitehub kitehub | gzip > $BACKUP_DIR/kitehub_$TIMESTAMP.sql.gz

# Upload to Object Storage (OCI or S3)
# OCI: oci os object put --bucket-name kitehub-backups --file $BACKUP_DIR/kitehub_$TIMESTAMP.sql.gz
# AWS: aws s3 cp $BACKUP_DIR/kitehub_$TIMESTAMP.sql.gz s3://kitehub-backups/

# Cleanup old backups
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
```

**Cron schedule:**
```cron
# Daily at 2 AM
0 2 * * * /opt/kitehub/scripts/backup-db.sh >> /var/log/kitehub-backup.log 2>&1
```

## Object Storage (MinIO / S3)
- Branding assets stored in `kitehub-assets` bucket
- MinIO data persisted via Docker volume `kitehub-minio-data`
- Production: Use S3/OCI Object Storage with versioning enabled

## Monitoring Data
- Prometheus data: Docker volume `kitehub-prometheus-data` (default 15d retention)
- Grafana data: Docker volume `kitehub-grafana-data`
- Not critical — can be recreated from scrape configs

## Recovery Plan

| Component | RTO | RPO | Method |
|-----------|-----|-----|--------|
| PostgreSQL | 1h | 24h | Restore from daily pg_dump |
| Object Storage | 1h | 0 | S3 versioning / cross-region replication |
| Application | 30m | 0 | Redeploy from Docker images |
| Monitoring | 1h | N/A | Recreate from provisioning configs |
