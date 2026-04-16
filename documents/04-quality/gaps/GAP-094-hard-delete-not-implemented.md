# GAP-094: Hard Delete (Data Purge) Not Implemented

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** KiteHub / Data Retention / Operations
**Found:** 2026-04-16 (SaaS business logic audit)
**Affects:** Storage growth, GDPR compliance

## Problem

`DataRetentionService.processExpiredRetention()` chỉ thực hiện soft delete:
```java
instance.softDelete(); // Sets status=DELETED, deletedAt=now
instanceRepository.save(instance);
```

Không có code cho:
- DROP DATABASE (instance-specific DB)
- DROP ROLE (DB user)
- Delete MinIO/S3 files (branding assets, uploads)
- Purge RabbitMQ queues

Kết quả: dữ liệu "deleted" vẫn chiếm storage vĩnh viễn.

## Proposed Fix

```java
// After soft delete + retention expiry:
void hardPurge(Instance instance) {
    // 1. Verify backup exists (GAP-093 prerequisite)
    backupService.verifyBackupExists(instance.getId());
    
    // 2. Drop tenant database
    databaseService.dropTenantDatabase(instance.getDatabaseUrl());
    
    // 3. Remove storage files
    storageService.deleteTenantBucket(instance.getId());
    
    // 4. Clean RabbitMQ resources
    messagingService.removeTenantExchange(instance.getId());
    
    // 5. Mark as PURGED (final state)
    instance.setStatus(InstanceStatus.PURGED);
}
```

**Prerequisite:** GAP-093 (backup must work) trước khi implement hard delete.

## Acceptance Criteria

- [ ] Hard purge runs sau retention period hết
- [ ] Database, files, messaging resources cleaned
- [ ] Backup verified trước khi purge
- [ ] Audit log giữ lại (instance ID, purge date, backup location)
- [ ] GDPR: personal data fully removed
