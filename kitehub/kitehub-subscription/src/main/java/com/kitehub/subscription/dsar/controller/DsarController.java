package com.kitehub.subscription.dsar.controller;

import com.kitehub.subscription.dsar.dto.DsarRequest;
import com.kitehub.subscription.dsar.dto.DsarResponse;
import com.kitehub.subscription.dsar.entity.DsarTicket;
import com.kitehub.subscription.dsar.service.DsarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Public REST controller for self-service DSAR (Data Subject Access Request) flow.
 *
 * <p>Endpoints (per
 * {@code documents/01-business/kitehub/marketing/api-contract.md}):</p>
 * <ul>
 *   <li>{@code POST /api/v1/dsar/request} — accept a fresh DSAR submission</li>
 *   <li>{@code GET  /api/v1/dsar/{ticketId}} — return redacted public-safe status</li>
 * </ul>
 *
 * <p>Both endpoints are unauthenticated by design — DSAR submitters are not
 * logged-in users by definition. Identity verification happens out-of-band via
 * national_id_last4 + DPO callback (BR-PDPL-DSAR-003). Rate-limit + abuse
 * mitigation lives at the gateway and via the honeypot field on
 * {@link DsarRequest}.</p>
 *
 * @since Wave 26 Bucket A — GAP-353c
 */
@RestController
@RequestMapping("/api/v1/dsar")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DSAR", description = "Self-service Data Subject Access Request intake (GAP-353c Wave 26)")
public class DsarController {

    private final DsarService dsarService;

    @Operation(summary = "Submit a DSAR request",
               description = "Public unauthenticated endpoint. Creates a PENDING ticket with a 20-day SLA deadline (PDPL Art 14 / Decree 13/2023 Art 19). Returns ticket UUID for status lookup.")
    @PostMapping("/request")
    public ResponseEntity<DsarResponse> submitDsar(@Valid @RequestBody DsarRequest request) {
        try {
            DsarTicket saved = dsarService.submitRequest(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(DsarResponse.from(saved));
        } catch (IllegalArgumentException ex) {
            log.warn("DSAR submission rejected: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Get DSAR ticket status by UUID",
               description = "Public unauthenticated endpoint. Returns redacted ticket state (status + SLA + timestamps). DPO resolution notes + raw PII never exposed; full content available only via DPO callback.")
    @GetMapping("/{ticketId}")
    public ResponseEntity<DsarResponse> getTicket(@PathVariable("ticketId") UUID ticketId) {
        Optional<DsarTicket> ticket = dsarService.getTicket(ticketId);
        return ticket.map(t -> ResponseEntity.ok(DsarResponse.from(t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
