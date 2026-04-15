package com.kiteclass.core.module.instance.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @since 3.21.0 (Wave 3 Sub-PR 3.5, GAP-070)
 */
@Repository
public interface RebrandApprovalRepository extends JpaRepository<RebrandApproval, Long> {

    Optional<RebrandApproval> findFirstByTargetInstanceIdAndStatusAndDeletedFalse(
            Long targetInstanceId, ApprovalStatus status);

    List<RebrandApproval> findByStatusAndExpiresAtBeforeAndDeletedFalse(
            ApprovalStatus status, Instant cutoff);
}
