package com.kiteclass.core.common.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * @since 3.17.0 (Wave 3 Sub-PR 3.1)
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Fetch a batch of PENDING rows whose next_attempt_at has elapsed, oldest first.
     * Used by {@code OutboxEventPublisher} scheduler.
     */
    @Query(
            "SELECT e FROM OutboxEvent e "
                    + "WHERE e.status = com.kiteclass.core.common.outbox.OutboxStatus.PENDING "
                    + "AND e.nextAttemptAt <= :now "
                    + "AND e.deleted = false "
                    + "ORDER BY e.nextAttemptAt ASC"
    )
    List<OutboxEvent> findDispatchable(@Param("now") Instant now, Pageable pageable);

    /**
     * Count rows in each status — used for metrics / dashboard.
     */
    long countByStatusAndDeletedFalse(OutboxStatus status);
}
