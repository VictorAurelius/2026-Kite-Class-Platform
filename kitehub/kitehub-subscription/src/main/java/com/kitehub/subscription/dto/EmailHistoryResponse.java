package com.kitehub.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for email history log entries.
 * Used by admin email monitoring endpoints.
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailHistoryResponse {

    private UUID id;
    private UUID instanceId;
    private String emailType;
    private String recipient;
    private LocalDateTime sentAt;

    /**
     * Derived status: SUCCESS or FAILED.
     * FAILED is determined by emailType containing ":FAILED" suffix
     * (recorded by DLQ consumer after retry exhaustion).
     */
    private String status;
}
