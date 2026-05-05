package com.kiteclass.core.module.childprotection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request body for {@code POST /api/v1/incidents/{id}/mandatory-report-ack}
 * (BR-CHILD-PROTECT-006, GAP-322c Phase 1C v1).
 *
 * <p>Submitted by the safeguarding officer after they have actually
 * reported the case to Tổng đài 111 + công an địa phương per
 * Luật Trẻ em 2016 Đ.51 (≤24h).
 *
 * @param referenceNumber external reference number issued by Tổng đài 111 / công an
 * @param reportedAt      wall-clock instant the external report was filed
 * @param notes           optional officer note (non-sensitive — encrypted notes stay on Incident.description)
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
public record MandatoryReportAckRequest(
        @NotBlank
        @Size(max = 128)
        String referenceNumber,

        @NotNull
        Instant reportedAt,

        @Size(max = 500)
        String notes) {
}
