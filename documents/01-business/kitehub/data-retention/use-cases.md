# Data Retention — Use Cases

### UC-RET-01: Data Retention Warning (Tự động)
- **Actor:** System (DataRetentionScheduler — daily 3 AM)
- **Precondition:** Instance SUSPENDED và chưa deleted
- **Steps:**
  1. System: query tất cả SUSPENDED, non-deleted instances
  2. System: với mỗi instance, get retention days theo tier (RET-01 đến RET-05)
  3. System: tính daysSuspended = now - updatedAt
  4. System: nếu daysSuspended = retentionDays * 50% → gửi retention-warning (RET-07)
  5. System: nếu daysSuspended = retentionDays * 80% → gửi retention-warning (RET-08)
  6. System: idempotency check trước khi gửi
- **Postcondition:** Email cảnh báo đã gửi
- **FE Behavior:** N/A (background)

### UC-RET-02: Data Deletion (Tự động)
- **Actor:** System (DataRetentionScheduler — daily 3 AM)
- **Precondition:** Instance SUSPENDED, retentionExpiry đã qua
- **Steps:**
  1. System: query SUSPENDED instances
  2. System: retentionExpiry = updatedAt + retentionDays
  3. System: nếu 1 ngày trước expiry → gửi retention-final-warning (RET-09)
  4. System: nếu now > retentionExpiry:
     a. Set status = DELETED
     b. Soft delete (deleted flag = true)
     c. Gửi data-deleted email (RET-10)
- **Postcondition:** Instance DELETED, data không thể recover

### UC-RET-03: Reactivation trước khi hết retention
- **Actor:** Owner (instance đang SUSPENDED)
- **Precondition:** Instance SUSPENDED, retention period chưa hết
- **Steps:**
  1. Owner: thanh toán để reactivate
  2. System: create new subscription
  3. System: instance status → ACTIVE
- **Postcondition:** Instance ACTIVE, data được giữ lại
- **Errors:**
  - 400: instance đã DELETED (retention đã hết)

### UC-RET-04: Admin Hard Purge
- **Actor:** Platform Admin
- **Precondition:** Instance status = DELETED, ít nhất 1 backup có status = COMPLETED
- **Steps:**
  1. Admin: chọn instance cần purge từ admin panel
  2. Admin: gọi `DELETE /api/platform/instances/{id}/purge`
  3. System: verify instance status = DELETED (nếu không → FAILED)
  4. System: verify tồn tại ít nhất 1 backup COMPLETED (nếu không → SKIPPED_NO_BACKUP)
  5. System: drop PostgreSQL database
  6. System: delete tất cả backup files từ S3, mark BackupRecord status = DELETED
  7. System: delete email sent logs cho instance
  8. System: publish RabbitMQ event (`instance.purge.exchange`, fanout) cho cross-service cleanup
  9. System: set instance status = PURGED
- **Postcondition:** Instance status = PURGED, tất cả tài nguyên đã bị xóa vĩnh viễn
- **Errors:**
  - Instance không ở status DELETED → PurgeStatus.FAILED, errorMessage chứa status hiện tại
  - Không có backup COMPLETED → PurgeStatus.SKIPPED_NO_BACKUP, instance không bị xóa
  - Lỗi khi drop DB hoặc delete S3 → tiếp tục cleanup các bước còn lại, log error
- **FE Behavior:** Admin nhận PurgeResult với chi tiết (databaseDropped, backupFilesDeleted, etc.)

### UC-RET-05: Automated Weekly Purge
- **Actor:** System (DatabaseBackupScheduler — Sunday 3:00 AM)
- **Trigger:** Cron `0 0 3 * * SUN`
- **Precondition:** Tồn tại instances có status = DELETED và updatedAt > 30 ngày trước
- **Steps:**
  1. System: query instances eligible cho purge (DELETED > 30 ngày) via `findPurgeEligible()`
  2. System: với mỗi instance eligible:
     a. Gọi `purgeInstance(instanceId)`
     b. Verify backup COMPLETED (nếu không có → SKIPPED, log warning, tiếp tục instance tiếp theo)
     c. Nếu có backup → executePurge (drop DB, delete S3, delete email logs, publish event, set PURGED)
  3. System: log summary (purged count, skipped count, failed count, duration)
- **Postcondition:** Instances eligible đã được purge (trừ những instance không có backup)
- **FE Behavior:** N/A (background job)
- **Note:** Instances bị skip (no backup) sẽ được retry ở lần chạy tiếp theo nếu backup đã tạo
