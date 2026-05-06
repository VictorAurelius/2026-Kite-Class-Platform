package com.kitehub.subscription.dsar.cron;

import com.kitehub.subscription.dsar.entity.DsarStatus;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import com.kitehub.subscription.dsar.repository.DsarTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Daily DSAR SLA-deadline check.
 *
 * <p>Runs 04:00 — one hour after {@code ConsentRetentionCron} (03:00) and
 * {@code DataRetentionScheduler} so the heavier retention pass finishes first.
 * Logs an ERROR per overdue ticket — operators / Grafana alert on
 * {@code dsar.sla.breach} pattern; the cron itself does not auto-escalate
 * status (DPO triage stays human-in-the-loop per BR-PDPL-DSAR-002).</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaTimerCron {

    private final DsarTicketRepository repository;

    /** Daily 04:00 — after retention crons. */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(readOnly = true)
    public void flagOverdueTickets() {
        OffsetDateTime now = OffsetDateTime.now();
        List<DsarTicket> overdue = repository.findBySlaDeadlineBeforeAndStatusIn(
                now,
                List.of(DsarStatus.PENDING, DsarStatus.IN_REVIEW));
        if (overdue.isEmpty()) {
            log.info("DSAR SLA timer: no overdue tickets at {}", now);
            return;
        }
        log.error("dsar.sla.breach event=dsar.sla.breach overdueCount={}", overdue.size());
        for (DsarTicket ticket : overdue) {
            log.error("dsar.sla.breach ticketId={} rightType={} status={} slaDeadline={} createdAt={}",
                    ticket.getTicketUuid(),
                    ticket.getRightType(),
                    ticket.getStatus(),
                    ticket.getSlaDeadline(),
                    ticket.getCreatedAt());
        }
    }
}
