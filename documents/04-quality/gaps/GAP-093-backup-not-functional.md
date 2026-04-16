# GAP-093: Database Backup System Only Logs, Doesn't Actually Backup

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** KiteHub / Data Safety / Operations
**Found:** 2026-04-16 (SaaS business logic audit)
**Affects:** Data recovery for all suspended/expired instances

## Problem

`DatabaseBackupScheduler.backupInstanceDatabase()` (line 88-95) chỉ log backup path, KHÔNG thực sự chạy `pg_dump` hay upload S3:

```java
void backupInstanceDatabase(Instance instance) {
    String dbName = extractDatabaseName(instance.getDatabaseUrl());
    String backupPath = generateBackupPath(instance.getId(), dbName);
    log.info("Backup recorded for database {} -> {}", dbName, backupPath);
    // Actual S3 upload via pg_dump is deferred until cloud storage infrastructure is provisioned.
}
```

Hệ thống GỬI EMAIL cho user nói "data của bạn đã được backup" nhưng thực tế KHÔNG có backup. Nếu hard delete chạy → data mất vĩnh viễn.

## Proposed Fix

### Phase 1 (trước production):
1. Implement `pg_dump` execution cho mỗi instance database
2. Upload dump file lên S3/MinIO
3. Verify backup integrity (file size, checksum)
4. Update `BackupRecord` entity với actual path + checksum

### Phase 2 (production):
1. Restore verification: auto-test restore từ backup weekly
2. Retention policy cho backup files (90 ngày?)
3. Customer-facing backup download endpoint (self-service)

## Acceptance Criteria

- [ ] `pg_dump` thực sự chạy khi scheduler trigger
- [ ] Backup file upload lên object storage (MinIO dev, S3 prod)
- [ ] Backup integrity verified (checksum match)
- [ ] Restore tested successfully từ backup file
- [ ] Email content chính xác (chỉ nói "backup" khi thực sự có backup)
