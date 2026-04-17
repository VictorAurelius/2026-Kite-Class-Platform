package com.kiteclass.core.module.storage.constant;

import lombok.Getter;

/**
 * Status values for uploaded files.
 *
 * <p>Tracks the lifecycle of an uploaded file:
 * <ul>
 *   <li>PENDING: Presigned URL generated, waiting for actual upload (30min TTL)</li>
 *   <li>CONFIRMED: File successfully uploaded to S3</li>
 *   <li>EXPIRED: PENDING upload exceeded TTL (30 minutes)</li>
 *   <li>DELETED: File soft deleted (scheduled for S3 cleanup)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Getter
public enum StorageStatus {

    PENDING("Đang chờ", "Presigned URL generated, waiting for upload"),
    CONFIRMED("Đã xác nhận", "File successfully uploaded"),
    EXPIRED("Đã hết hạn", "Upload window expired"),
    DELETED("Đã xóa", "File marked for deletion");

    private final String displayNameVi;
    private final String description;

    StorageStatus(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }

    /**
     * Returns badge class for UI styling.
     *
     * @return Tailwind CSS classes
     */
    public String getBadgeClass() {
        return switch (this) {
            case PENDING -> "bg-yellow-100 text-yellow-800";
            case CONFIRMED -> "bg-green-100 text-green-800";
            case EXPIRED -> "bg-gray-100 text-gray-800";
            case DELETED -> "bg-red-100 text-red-800";
        };
    }
}
