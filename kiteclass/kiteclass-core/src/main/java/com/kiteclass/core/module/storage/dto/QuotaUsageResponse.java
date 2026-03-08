package com.kiteclass.core.module.storage.dto;

import com.kiteclass.core.module.storage.constant.StorageTier;

/**
 * Response DTO for storage quota usage.
 *
 * <p>Contains quota information:
 * <ul>
 *   <li>tier: Subscription tier (FREE, BASIC, PRO, ENTERPRISE)</li>
 *   <li>usedBytes: Current storage usage in bytes</li>
 *   <li>quotaBytes: Maximum allowed storage in bytes</li>
 *   <li>remainingBytes: Available storage (quotaBytes - usedBytes)</li>
 *   <li>usagePercentage: Usage percentage (0-100)</li>
 * </ul>
 *
 * @param tier             Subscription tier
 * @param usedBytes        Current usage in bytes
 * @param quotaBytes       Maximum quota in bytes
 * @param remainingBytes   Remaining storage in bytes
 * @param usagePercentage  Usage percentage
 * @author KiteClass Team
 * @since 2.10.1
 */
public record QuotaUsageResponse(
    StorageTier tier,
    Long usedBytes,
    Long quotaBytes,
    Long remainingBytes,
    Double usagePercentage
) {
}
