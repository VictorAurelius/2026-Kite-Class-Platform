package com.kiteclass.core.module.parent.dto;

import java.time.Instant;

/**
 * Read-only notification projection exposed to a parent for one of their
 * linked children.
 *
 * <p>Phase 1B v1 stub: there is no parent-targeted notifications table
 * yet — the cross-cutting notification engine lands in Wave 18a Bucket B
 * (GAP-063b). The endpoint returns an empty page until that engine writes
 * to a child-scoped store. The DTO is published now so the FE
 * notifications drawer can be wired against the eventual contract.
 *
 * @param notificationId  primary key (opaque to UI; useful for cache key)
 * @param studentId       child's id (always matches the path parameter)
 * @param title           notification title (max 100 chars)
 * @param body            notification body (max 500 chars)
 * @param sentAt          server-side timestamp when the notification was
 *                        emitted to the parent's queue
 * @param readAt          {@code null} until the parent marks it read
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
public record ParentNotificationFacetResponse(
        Long notificationId,
        Long studentId,
        String title,
        String body,
        Instant sentAt,
        Instant readAt
) {
}
