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
