package com.kiteclass.core.module.student.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Notification feed item for the kc-student notifications screen (GAP-269b).
 *
 * <p>Cursor-paginated feed shape: {@link Instant#toEpochMilli()} of
 * {@code createdAt} backing the cursor in v1.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentNotificationItem {

    private Long id;
    private String category; // GRADE_PUBLISHED / ATTENDANCE_FLAG / PAYMENT_DUE / GENERIC / ...
    private String title;
    private String body;
    private Instant createdAt;
    private boolean read;
}
