# Data Retention — API Contract

> Data Retention được xử lý hoàn toàn tự động bởi DataRetentionScheduler.
> Không có API endpoint trực tiếp cho retention management.
> Thông tin retention được trả về qua Instance API:

## GET /api/platform/instances/{id}
**Relevant retention fields trong InstanceResponse:**
```json
{
  "id": "uuid",
  "status": "SUSPENDED",
  "tier": "BASIC",
  "deleted": false,
  "updatedAt": "2026-03-01T00:00:00Z",
  "retentionDays": 30,
  "retentionExpiresAt": "2026-03-31T00:00:00Z"
}
```

> **Note:** `retentionDays` và `retentionExpiresAt` là derived fields tính từ tier và updatedAt.
> Reactivation được thực hiện qua Subscription API (tạo subscription mới).

---

## Note on Automated Use Cases (no HTTP endpoint)

| Use Case | Trigger | Description |
|----------|---------|-------------|
| UC-RET-01 | `DataRetentionScheduler` (daily) | Check SUSPENDED instances past retention period → mark for deletion |
| UC-RET-02 | `DataRetentionScheduler` (daily) | Soft-delete instances that have exceeded retention window |
| UC-RET-03 | `DataRetentionScheduler` (daily) | Hard-purge data after retention period fully elapsed |

These use cases run automatically — no HTTP endpoint. Monitor via instance status fields above.

---

## Hard Purge API

> Admin endpoint để xóa vĩnh viễn instance đã DELETED, bao gồm toàn bộ tài nguyên liên quan.

### DELETE /api/platform/instances/{id}/purge
**Use case:** UC-RET-04
**Auth:** Platform Admin
**Path params:** `id` — Instance UUID

**Precondition:**
- Instance phải có status = `DELETED`
- Phải có ít nhất 1 backup với status = `COMPLETED` (safety gate)

**Response 200:**
```json
{
  "instanceId": "uuid",
  "subdomain": "truong-abc",
  "status": "SUCCESS",
  "databaseDropped": true,
  "backupFilesDeleted": 3,
  "emailLogsDeleted": 0,
  "brandingCleanupPublished": true,
  "errorMessage": null,
  "purgedAt": "2026-04-15T03:15:00"
}
```

**PurgeStatus values:**

| Status | Mô tả |
|--------|-------|
| `SUCCESS` | Purge thành công, tất cả tài nguyên đã xóa |
| `SKIPPED_NO_BACKUP` | Không có backup COMPLETED → bỏ qua, không xóa |
| `FAILED` | Lỗi trong quá trình purge |

**Errors:**
- 404: Instance not found
- 200 với `status: FAILED`: Instance không ở trạng thái DELETED (errorMessage chứa status hiện tại)
- 200 với `status: SKIPPED_NO_BACKUP`: Không tìm thấy backup COMPLETED — purge bị từ chối

**Purge steps (khi SUCCESS):**
1. Verify backup exists (COMPLETED)
2. Drop PostgreSQL database
3. Delete all backup files từ S3, mark BackupRecord status = DELETED
4. Delete email sent logs
5. Publish RabbitMQ event (`instance.purge.exchange`, fanout) cho cross-service cleanup (branding, etc.)
6. Set instance status = `PURGED`

> **Note:** Sau purge, instance status = `PURGED`. Đây là trạng thái cuối cùng, không thể recover.
