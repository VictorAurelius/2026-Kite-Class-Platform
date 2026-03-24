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
