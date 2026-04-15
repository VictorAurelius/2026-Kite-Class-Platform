package com.kiteclass.core.module.retention;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @since 3.23.0 (Wave 4 Sub-PR 4.4)
 */
@Repository
public interface DeletionRequestRepository extends JpaRepository<DeletionRequest, Long> {

    Optional<DeletionRequest> findFirstByUserIdAndStatusAndDeletedFalse(
            Long userId, DeletionStatus status);

    List<DeletionRequest> findByStatusAndGraceEndsAtBeforeAndDeletedFalse(
            DeletionStatus status, Instant cutoff);
}
