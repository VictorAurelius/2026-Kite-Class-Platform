package com.kitehub.subscription.dsar.service;

import com.kitehub.subscription.dsar.dto.DsarRequest;
import com.kitehub.subscription.dsar.entity.DsarTicket;

import java.util.Optional;
import java.util.UUID;

/**
 * Application-layer DSAR service.
 *
 * <p>Strategy / Facade combination — wraps the persistence repository and the
 * (currently scaffolded) DPO email-notification adapter under a domain-typed
 * facade. See {@code DsarServiceImpl} for the production implementation.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
public interface DsarService {

    /**
     * Submit a fresh DSAR request.
     *
     * @param request validated DTO from the public POST endpoint
     * @return persisted ticket with assigned UUID + 20-day SLA deadline
     */
    DsarTicket submitRequest(DsarRequest request);

    /**
     * Look up ticket by public UUID. Returns redacted state — {@link DsarTicket}
     * is returned verbatim; the controller is responsible for projecting it via
     * {@code DsarResponse.from(...)} before sending to the client.
     */
    Optional<DsarTicket> getTicket(UUID ticketUuid);
}
