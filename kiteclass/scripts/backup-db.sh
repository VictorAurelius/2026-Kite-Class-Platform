#!/bin/bash
# KiteClass Database Backup Script
# Usage: ./scripts/backup-db.sh [output-dir]
# Runs pg_dump, compresses, and optionally uploads to S3/MinIO
#
# Schedule via cron: 0 2 * * * /path/to/backup-db.sh /backups

set -euo pipefail

BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-kiteclass_dev}"
DB_USER="${POSTGRES_USER:-kiteclass}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
BACKUP_FILE="$BACKUP_DIR/kiteclass_${TIMESTAMP}.sql.gz"

echo "=== KiteClass Database Backup ==="
echo "Timestamp: $TIMESTAMP"
echo "Database:  $DB_NAME@$DB_HOST:$DB_PORT"
echo "Output:    $BACKUP_FILE"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Run pg_dump with compression
echo "Starting backup..."
PGPASSWORD="${POSTGRES_PASSWORD:-kiteclass123}" pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --format=custom \
  --compress=9 \
  --verbose \
  -f "$BACKUP_FILE" 2>&1 | tail -5

# Verify backup
if [ -f "$BACKUP_FILE" ]; then
  SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
  echo "✅ Backup created: $BACKUP_FILE ($SIZE)"
else
  echo "❌ Backup failed!"
  exit 1
fi

# Upload to MinIO/S3 if mc (MinIO client) is available
if command -v mc &> /dev/null; then
  MINIO_BUCKET="${MINIO_BACKUP_BUCKET:-kiteclass-backups}"
  echo "Uploading to MinIO: $MINIO_BUCKET..."
  mc cp "$BACKUP_FILE" "myminio/$MINIO_BUCKET/$(basename "$BACKUP_FILE")" 2>/dev/null && \
    echo "✅ Uploaded to MinIO" || \
    echo "⚠️  MinIO upload failed (backup still saved locally)"
fi

# Cleanup old backups
echo "Cleaning up backups older than $RETENTION_DAYS days..."
DELETED=$(find "$BACKUP_DIR" -name "kiteclass_*.sql.gz" -mtime +$RETENTION_DAYS -delete -print | wc -l)
echo "Removed $DELETED old backup(s)"

echo "=== Backup complete ==="
