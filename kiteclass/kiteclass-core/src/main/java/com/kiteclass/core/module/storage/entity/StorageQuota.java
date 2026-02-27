package com.kiteclass.core.module.storage.entity;

import com.kiteclass.core.module.storage.constant.StorageTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing storage quota for a tenant.
 *
 * <p>Tracks storage usage and limits per tenant:
 * <ul>
 *   <li>Quota tier (FREE, BASIC, PRO, ENTERPRISE)</li>
 *   <li>Used bytes vs quota bytes</li>
 *   <li>Last calculation timestamp</li>
 * </ul>
 *
 * <p>One quota record per tenant (instance_id is unique).
 *
 * <p>Quota is updated when:
 * <ul>
 *   <li>File upload is confirmed: +fileSize</li>
 *   <li>File is deleted: -fileSize</li>
 *   <li>Manual recalculation (via scheduled job)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Entity
@Table(name = "storage_quotas", indexes = {
    @Index(name = "idx_storage_quotas_instance_id", columnList = "instance_id"),
    @Index(name = "idx_storage_quotas_tier", columnList = "tier")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant identifier (one quota per tenant).
     */
    @Column(name = "instance_id", nullable = false, unique = true)
    private UUID instanceId;

    /**
     * Storage tier (FREE, BASIC, PRO, ENTERPRISE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    @Builder.Default
    private StorageTier tier = StorageTier.FREE;

    /**
     * Current storage usage in bytes.
     */
    @Column(name = "used_bytes", nullable = false)
    @Builder.Default
    private Long usedBytes = 0L;

    /**
     * Maximum allowed storage in bytes.
     */
    @Column(name = "quota_bytes", nullable = false)
    private Long quotaBytes;

    /**
     * Last time quota usage was recalculated.
     */
    @Column(name = "last_calculated_at", nullable = false)
    @Builder.Default
    private Instant lastCalculatedAt = Instant.now();

    /**
     * Timestamp when quota was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when quota was last updated.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Checks if adding fileSize would exceed quota.
     *
     * @param fileSize Size in bytes to check
     * @return true if quota would be exceeded
     */
    public boolean wouldExceedQuota(long fileSize) {
        return (usedBytes + fileSize) > quotaBytes;
    }

    /**
     * Returns remaining storage space in bytes.
     *
     * @return Remaining bytes (quotaBytes - usedBytes)
     */
    public long getRemainingBytes() {
        return Math.max(0, quotaBytes - usedBytes);
    }

    /**
     * Returns quota usage percentage.
     *
     * @return Usage percentage (0-100)
     */
    public double getUsagePercentage() {
        if (quotaBytes == 0) {
            return 0.0;
        }
        return (usedBytes * 100.0) / quotaBytes;
    }

    /**
     * Adds bytes to used storage.
     *
     * @param bytes Bytes to add
     */
    public void addUsage(long bytes) {
        this.usedBytes += bytes;
    }

    /**
     * Subtracts bytes from used storage.
     *
     * @param bytes Bytes to subtract
     */
    public void subtractUsage(long bytes) {
        this.usedBytes = Math.max(0, this.usedBytes - bytes);
    }

    /**
     * Sets used bytes (for quota recalculation).
     *
     * @param bytes New used bytes value
     */
    public void setUsedBytesAndRecalculate(long bytes) {
        this.usedBytes = bytes;
        this.lastCalculatedAt = Instant.now();
    }

    /**
     * Updates quota tier and limits.
     *
     * @param newTier New storage tier
     */
    public void updateTier(StorageTier newTier) {
        this.tier = newTier;
        this.quotaBytes = newTier.getQuotaBytes();
    }
}
