package com.kiteclass.core.module.student.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cursor-paginated wrapper for {@link StudentNotificationItem} (GAP-269b).
 *
 * <p>{@code nextCursor} is null when no further page is available; the FE
 * stops calling once it sees a null. Cursor opaqueness is intentional —
 * callers must not interpret the value.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentNotificationFeedResponse {

    private List<StudentNotificationItem> items;
    private String nextCursor;
}
