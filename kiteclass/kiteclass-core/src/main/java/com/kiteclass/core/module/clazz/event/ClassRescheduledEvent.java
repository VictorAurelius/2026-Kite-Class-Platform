package com.kiteclass.core.module.clazz.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Outbox event payload published when a class is rescheduled.
 *
 * <p>Per cross-bucket LOCKED decision §3.6 (Wave beta-readiness-4):
 * <ul>
 *   <li>Default consumer: {@code ClassRescheduledNoOpConsumer} (logs only)</li>
 *   <li>Email consumer: {@code ClassRescheduledEmailConsumer} activates when
 *       {@code kite.class.reschedule.notify.enabled=true}</li>
 *   <li>Notification classification = OPERATIONAL (bypass marketing_consented gate)</li>
 * </ul>
 *
 * <p>Routing key: {@code class.rescheduled} via Outbox dispatcher.
 *
 * @param classId            ID of the rescheduled class
 * @param tenantId           Tenant (institution) ID owning the class
 * @param tenantName         Human-readable tenant name (for email subject lines)
 * @param className          Human-readable class name
 * @param previousStartDate  startDate captured BEFORE reschedule (audit)
 * @param newStartDate       startDate AFTER reschedule
 * @param previousEndDate    endDate captured BEFORE reschedule (audit)
 * @param newEndDate         endDate AFTER reschedule
 * @param rescheduledByUserId User who triggered the reschedule
 * @param rescheduledAt      Timestamp of the reschedule operation
 * @param reasonCategory     RescheduleReasonCategory enum name (e.g., "GV_OM_BAN_DOT_XUAT")
 * @param reasonNotes        Optional free-text notes (may be null/empty)
 * @param enrolledStudentIds Student IDs currently enrolled in the class
 * @param parentUserIds      Parent user IDs to notify (joined via parent-student links)
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
public record ClassRescheduledEvent(
        Long classId,
        String tenantId,
        String tenantName,
        String className,
        LocalDate previousStartDate,
        LocalDate newStartDate,
        LocalDate previousEndDate,
        LocalDate newEndDate,
        Long rescheduledByUserId,
        Instant rescheduledAt,
        String reasonCategory,
        String reasonNotes,
        List<Long> enrolledStudentIds,
        List<Long> parentUserIds
) {
}
