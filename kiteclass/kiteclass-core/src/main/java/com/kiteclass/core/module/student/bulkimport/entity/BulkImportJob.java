package com.kiteclass.core.module.student.bulkimport.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents one bulk-import attempt against the student module.
 *
 * <p>Persisted so admins can audit past imports and download the generated
 * error-report xlsx for any failed rows.
 *
 * <p>Multi-tenant: inherits {@code instanceId} from {@link BaseEntity}.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Entity
@Table(
        name = "student_bulk_import_jobs",
        indexes = {
                @Index(name = "idx_bulk_import_jobs_tenant", columnList = "instance_id"),
                @Index(name = "idx_bulk_import_jobs_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportJob extends BaseEntity {

    /** Original uploaded filename (for audit). */
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    /** Current status of the job. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BulkImportStatus status = BulkImportStatus.PENDING;

    /** Total rows detected in the uploaded file (after header). */
    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Integer totalRows = 0;

    /** Rows successfully created as students. */
    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Integer successCount = 0;

    /** Rows rejected due to validation or duplicates. */
    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private Integer errorCount = 0;

    /**
     * URL (or storage path) to the generated error-report xlsx.
     * For the MVP we generate the report on-demand from the stored errors,
     * so this column may remain null.
     */
    @Column(name = "error_report_url", length = 500)
    private String errorReportUrl;

    /** Timestamp marking job completion (success or failure). */
    @Column(name = "completed_at")
    private Instant completedAt;
}
