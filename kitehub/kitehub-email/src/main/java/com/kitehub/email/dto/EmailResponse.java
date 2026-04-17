package com.kitehub.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Email send response.
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

    /**
     * Email message ID from SES.
     */
    private String messageId;

    /**
     * Email status (SENT, FAILED, MOCK).
     */
    private String status;

    /**
     * Sent timestamp.
     */
    private LocalDateTime sentAt;

    /**
     * Error message if failed.
     */
    private String errorMessage;
}
