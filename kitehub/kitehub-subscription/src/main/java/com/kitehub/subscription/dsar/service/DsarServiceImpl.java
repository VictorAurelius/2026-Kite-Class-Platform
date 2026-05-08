package com.kitehub.subscription.dsar.service;

import com.kitehub.subscription.client.EmailServiceClient;
import com.kitehub.subscription.dsar.dto.DsarRequest;
import com.kitehub.subscription.dsar.entity.DsarStatus;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import com.kitehub.subscription.dsar.repository.DsarTicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link DsarService} implementation.
 *
 * <p>Persists the ticket with a server-assigned UUID and a 20-day SLA deadline
 * (BR-PDPL-DSAR-002). On submit, dispatches two emails via {@link EmailServiceClient}
 * (outbox-first per `design-patterns.md` §3.5.1 Exception A — outbox is the
 * reliability net): a DPO inbox alert (`dsar-new-ticket-dpo`) and a requester
 * acknowledgement (`dsar-acknowledgement-requester`). The structured
 * `dsar.ticket.created` log line is preserved as a defense-in-depth audit
 * fallback per GAP-353c-followup-dpo-email-notification AC line 41 — even if
 * the email path fails, the ticket creation event is captured by aggregation.</p>
 *
 * <p>Email dispatch failure does <strong>not</strong> roll back the ticket
 * transaction: the ticket save commits before {@code notifyDpo}; each dispatch
 * is wrapped in a try/catch envelope so that mailroom outage is recovered by
 * the outbox replay loop, never by aborting the data-subject-rights request.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 * @since Wave 48 Bucket A — GAP-353c-followup-dpo-email-notification (email integration)
 */
@Service
@Slf4j
public class DsarServiceImpl implements DsarService {

    private final DsarTicketRepository repository;
    private final EmailServiceClient emailServiceClient;
    private final String dpoEmail;

    public DsarServiceImpl(DsarTicketRepository repository,
                           EmailServiceClient emailServiceClient,
                           @Value("${kitehub.dsar.dpo-email:dpo@kitehub.vn}") String dpoEmail) {
        this.repository = repository;
        this.emailServiceClient = emailServiceClient;
        this.dpoEmail = dpoEmail;
    }

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
     * Emit the DPO-notification audit log line + dispatch DPO alert + requester
     * acknowledgement emails.
     *
     * <p>Audit log line runs FIRST as defense-in-depth fallback (per gap AC line 41):
     * if downstream broker is unavailable AND the outbox replay also fails, log
     * aggregation still captures the ticket creation event so DPO can grep on a
     * daily basis. Email dispatches follow.</p>
     *
     * <p>Each dispatch is independently try/catch'd — a failure in the DPO push
     * does not block the requester acknowledgement, and neither failure rolls
     * back the ticket transaction (the ticket has already been persisted by
     * {@link #submitRequest}).</p>
     *
     * <p>PII-light by design: email is logged structured (scrubber masks per
     * {@code logs-format-standard.md} §3.1); no national_id fragment is emitted.</p>
     */
    private void notifyDpo(DsarTicket ticket) {
        // Defense-in-depth audit fallback per GAP-353c-followup AC line 41.
        log.info(
                "dsar.ticket.created event=dsar.ticket.created ticketId={} rightType={} requesterEmail={} slaDeadline={}",
                ticket.getTicketUuid(),
                ticket.getRightType(),
                ticket.getRequesterEmail(),
                ticket.getSlaDeadline());

        // DPO push notification (outbox is the reliability net).
        try {
            emailServiceClient.sendDsarNewTicketDpoEmail(
                    ticket.getTicketUuid(),
                    dpoEmail,
                    ticket.getRightType().name(),
                    ticket.getRequesterEmail(),
                    ticket.getSlaDeadline());
        } catch (Exception e) {
            log.warn("DPO email dispatch failed for ticket {} - outbox is the reliability net: {}",
                    ticket.getTicketUuid(), e.getMessage());
        }

        // Requester acknowledgement (outbox is the reliability net).
        try {
            emailServiceClient.sendDsarAcknowledgementEmail(
                    ticket.getTicketUuid(),
                    ticket.getRequesterEmail(),
                    ticket.getRequesterName(),
                    ticket.getRightType().name(),
                    ticket.getSlaDeadline());
        } catch (Exception e) {
            log.warn("Requester acknowledgement dispatch failed for ticket {}: {}",
                    ticket.getTicketUuid(), e.getMessage());
        }
    }
}
