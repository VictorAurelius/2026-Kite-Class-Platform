package com.kitehub.subscription.dsar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kitehub.subscription.dsar.entity.DsarRightType;
import com.kitehub.subscription.dsar.entity.DsarStatus;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public-safe response payload for DSAR endpoints.
 *
 * <p>Redacts internal BIGSERIAL {@code id}, raw {@code requesterEmail},
 * {@code nationalIdLast4}, and any DPO {@code resolution} notes. Public
 * GET-by-uuid only returns ticket metadata and current status; full content
 * lives behind DPO callback per BR-PDPL-DSAR-003.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DsarResponse {

    @JsonProperty("ticketId")
    private UUID ticketId;

    @JsonProperty("rightType")
    private DsarRightType rightType;

    @JsonProperty("status")
    private DsarStatus status;

    @JsonProperty("slaDeadline")
    private OffsetDateTime slaDeadline;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("resolvedAt")
    private OffsetDateTime resolvedAt;

    public static DsarResponse from(DsarTicket ticket) {
        return DsarResponse.builder()
                .ticketId(ticket.getTicketUuid())
                .rightType(ticket.getRightType())
                .status(ticket.getStatus())
                .slaDeadline(ticket.getSlaDeadline())
                .createdAt(ticket.getCreatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }
}
