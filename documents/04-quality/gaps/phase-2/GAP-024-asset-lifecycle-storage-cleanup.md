# GAP-024: Asset Lifecycle & Storage Cleanup

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / Storage
**Detected:** 2026-04-14 (simulation)

## Problem

AI-generated assets tích lũy không giới hạn trong MinIO:

- ❌ Old AI assets không được cleanup khi tenant regenerate
- ❌ Orphan files (tenant deleted nhưng assets còn)
- ❌ Không storage quota per tenant
- ❌ Không archival policy (hot → warm → cold storage)
- ❌ MinIO size grow unbounded → infrastructure cost

**Cost impact:** Với 1000 tenants × 5 rebrands × 5 assets × 500KB = ~12GB chỉ assets chính thức. Thêm AI-gen generations (rejected) → có thể 10x nữa.

## Proposed Fix

### 1. Asset Lifecycle States

```java
public enum AssetLifecycle {
  ACTIVE,      // Currently used by DEPLOYED instance
  PREVIOUS,    // Previous version, kept for rollback (7 days)
  ARCHIVED,    // Old, moved to cold storage (30+ days)
  DELETED      // Marked for deletion (grace period)
}

@Entity
public class BrandingAssetLifecycle {
  Long id;
  String assetUrl;
  String tenantId;
  AssetLifecycle status;
  Timestamp createdAt, lastAccessedAt;
  Timestamp archivedAt, deletedAt;
}
```

### 2. Cleanup Policies

```java
@Scheduled(cron = "0 0 2 * * *") // Daily 2am
public class AssetCleanupScheduler {

  // Move PREVIOUS → ARCHIVED after 7 days
  public void archiveOldAssets() {
    var oldAssets = lifecycleRepo.findByStatusAndOlderThan(PREVIOUS, 7);
    for (var asset : oldAssets) {
      moveToArchiveBucket(asset);
      asset.setStatus(ARCHIVED);
    }
  }

  // Delete ARCHIVED after 90 days (unless tenant DELETED extension)
  public void deleteArchived() {
    var expired = lifecycleRepo.findByStatusAndOlderThan(ARCHIVED, 90);
    for (var asset : expired) {
      s3.delete(asset.getUrl());
      asset.setStatus(DELETED);
    }
  }

  // Delete orphans (tenant deleted)
  public void cleanupOrphans() {
    var orphans = lifecycleRepo.findByTenantDeletedAndOlderThan(30);
    s3.bulkDelete(orphans);
  }
}
```

### 3. Storage Quota per Tenant

```java
public class StorageQuotaPolicy {
  FREE:       500MB   (can store ~1000 small assets)
  PRO:        5GB
  PREMIUM:    50GB
  ENTERPRISE: Unlimited + billed per GB over fair-use
}
```

Check quota trước khi save new asset → reject nếu exceeded.

### 4. Archive Strategy

Hot (MinIO standard) → Warm (MinIO IA) → Cold (S3 Glacier Deep Archive)
- Latency tradeoff vs cost
- User request archive → restore takes hours but cheap storage

### 5. Admin UI

- Dashboard: storage usage per tenant
- Top 20 tenants by storage
- Cleanup history + status

## Acceptance Criteria

- [ ] AssetLifecycle enum + entity
- [ ] 3 cleanup schedulers (archive, delete, orphans)
- [ ] Storage quota enforcement per tier
- [ ] Admin storage dashboard
- [ ] GDPR: tenant deletion → all assets purged within 30 days
- [ ] Metrics: storage used per tier, cleanup rate

## Dependencies

- GAP-007 (resource classification) — identify which assets cleanup
- MinIO / S3 tiering configuration

## Log

- 2026-04-14 — Storage cost sustainability concern
