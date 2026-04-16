package com.kitehub.subscription.repository;

import com.kitehub.subscription.domain.BackupRecord;
import com.kitehub.subscription.domain.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BackupRecord entity.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, UUID> {

    /**
     * Find all backups for an instance, ordered by most recent first.
     *
     * @param instanceId instance UUID
     * @return list of backup records
     */
    List<BackupRecord> findByInstanceIdOrderByCreatedAtDesc(UUID instanceId);

    /**
     * Find the latest backup with a specific status for an instance.
     *
     * @param instanceId instance UUID
     * @param status backup status
     * @return optional backup record
     */
    Optional<BackupRecord> findTopByInstanceIdAndStatusOrderByCreatedAtDesc(
        UUID instanceId, BackupStatus status);

    /**
     * Check if any backup with a given status exists for an instance.
     * Useful for GAP-094 purge logic to verify backup before deletion.
     *
     * @param instanceId instance UUID
     * @param status backup status
     * @return true if at least one backup exists
     */
    boolean existsByInstanceIdAndStatus(UUID instanceId, BackupStatus status);

    /**
     * Find all backup records for an instance (used during purge cleanup).
     *
     * @param instanceId instance UUID
     * @return list of backup records
     */
    List<BackupRecord> findByInstanceId(UUID instanceId);

    /**
     * Count total backups for an instance (for retention policy).
     *
     * @param instanceId instance UUID
     * @return number of backups
     */
    long countByInstanceId(UUID instanceId);
}
