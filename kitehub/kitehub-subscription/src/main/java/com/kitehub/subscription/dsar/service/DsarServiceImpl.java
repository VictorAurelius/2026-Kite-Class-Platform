package com.kitehub.subscription.dsar.service;

import com.kitehub.subscription.dsar.dto.DsarRequest;
import com.kitehub.subscription.dsar.entity.DsarStatus;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import com.kitehub.subscription.dsar.repository.DsarTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link DsarService} implementation.
 *
 * <p>Persists the ticket with a server-assigned UUID and a 20-day SLA deadline
 * (BR-PDPL-DSAR-002). DPO notification currently scaffolded as a structured log
 * line — the {@code kitehub-email} async API is not yet exposed cross-module,
 * so a follow-up gap (`GAP-353c-followup-dpo-email-notification`) tracks the
 * proper integration.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DsarServiceImpl implements DsarService {

    private final DsarTicketRepository repository;

    @Override
    @Transactional
    public DsarTicket submitRequest(DsarRequest request) {
        if (request.getCompanyWebsite() != null && !request.getCompanyWebsite().isBlank()) {
            // Honeypot tripped — bot submission. Per BR-PDPL-DSAR-005 reject silently
            // with a generic IllegalArgumentException so callers see HTTP 400 without
            // leaking the detection mechanism.
            log.warn("DSAR submission rejected: honeypot field populated (likely bot)");
            throw new IllegalArgumentException("invalid request");
        }

        DsarTicket ticket = DsarTicket.builder()
                .rightType(request.getRightType())
                .requesterEmail(request.getRequesterEmail())
                .requesterName(request.getRequesterName())
                .nationalIdLast4(request.getNationalIdLast4())
                .scope(request.getScope())
                .reason(request.getReason())
                .status(DsarStatus.PENDING)
                .build();

        DsarTicket saved = repository.save(ticket);
        notifyDpo(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DsarTicket> getTicket(UUID ticketUuid) {
        return repository.findByTicketUuid(ticketUuid);
    }

    /**
     * Emit a DPO-notification audit line. Currently scaffolded — to be replaced
     * with a {@code kitehub-email}-backed async dispatch in
     * {@code GAP-353c-followup-dpo-email-notification}.
     *
     * <p>PII-light by design: email is logged structured (scrubber masks per
     * {@code logs-format-standard.md} §3.1); no national_id fragment is emitted.</p>
     */
    private void notifyDpo(DsarTicket ticket) {
        log.info(
                "dsar.ticket.created event=dsar.ticket.created ticketId={} rightType={} requesterEmail={} slaDeadline={}",
                ticket.getTicketUuid(),
                ticket.getRightType(),
                ticket.getRequesterEmail(),
                ticket.getSlaDeadline());
    }
}
