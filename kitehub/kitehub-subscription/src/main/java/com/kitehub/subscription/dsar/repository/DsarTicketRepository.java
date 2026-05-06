package com.kitehub.subscription.dsar.repository;

import com.kitehub.subscription.dsar.entity.DsarStatus;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DsarTicket}.
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@Repository
public interface DsarTicketRepository extends JpaRepository<DsarTicket, Long> {

    Optional<DsarTicket> findByTicketUuid(UUID ticketUuid);

    /**
     * Tickets whose SLA deadline has passed and which are still open. Driven by
     * {@code SlaTimerCron} (daily 04:00).
     */
    List<DsarTicket> findBySlaDeadlineBeforeAndStatusIn(
            OffsetDateTime cutoff, List<DsarStatus> statuses);
}
