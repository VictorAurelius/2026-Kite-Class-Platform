package com.kiteclass.core.module.parent.notification;

/**
 * Wave 105 Bucket D STUB — Zalo OA channel notification surface.
 *
 * <p>Phase 1 BETA scope = log-only. The 3 events below are persisted to
 * {@code zalo_oa_notification_outbox} (V61 migration) and emitted as
 * {@code log.info("would send Zalo OA: ...")} so observability is in place
 * before the actual ZNS (Zalo Notification Service) integration ships in
 * Wave 106 (GAP-286 full integration).
 *
 * <p>Rationale for stub-first per VN edu SaaS benchmark (Wave 105 outside-in
 * audit `2026-05-22-wave-105-vn-saas-benchmark.md`): 3/3 competitors
 * (DotB / EduSpace / CloudClass) prioritize Zalo OA parent channel because
 * Vietnamese parents prefer Zalo group chat over email for daily school
 * communication (per `vn-localization-audit-checklist.md` Section 4 row
 * "Phụ huynh communication style"). KiteHub email-only Phase 1 BETA is
 * industry outlier; stub locks in the contract NOW so:
 *
 * <ul>
 *   <li>Wave 106 dispatcher swap is mechanical (read outbox → call ZNS API)</li>
 *   <li>Wave 105 BE code paths emit events at correct decision points
 *       (no retrofit needed once ZNS lands)</li>
 *   <li>Audit log captures intent NOW so Phase 1 BETA tenants can confirm
 *       which events SHOULD be Zalo-delivered when they migrate</li>
 * </ul>
 *
 * <p><strong>Important:</strong> stub is fire-and-forget per
 * `audit-service-isolation.md`. Caller success path MUST NOT depend on this
 * service. Implementation uses {@code Propagation.REQUIRES_NEW} so any
 * persistence failure cannot poison the parent transaction (e.g., payment
 * confirm txn must commit even if Zalo OA outbox insert fails).
 *
 * <p>Per `vn-localization-audit-checklist.md` Section 2 email tone matrix:
 * Parent persona uses very-formal greeting {@code Kính gửi quý phụ huynh}.
 * Wave 106 dispatcher applies this tone in ZNS template variables.
 *
 * @since 3.0.0 (Wave 105 Bucket D — Parent persona walk)
 * @see com.kiteclass.core.module.parent.notification.impl.ZaloOaNotificationServiceImpl
 */
public interface ZaloOaNotificationService {

    /**
     * Records intent to notify parent that a child-attendance invitation
     * (parent_invitation) has been sent — sister channel to email invite.
     *
     * <p>Wave 105 effect: {@code log.info("would send Zalo OA: parent invite ...")} +
     * insert row into {@code zalo_oa_notification_outbox} with
     * {@code event_type=PARENT_INVITE_SENT}, {@code context_id=invitationId}.
     *
     * @param parentId     parent recipient (linked to Zalo OA token via Wave 106 GAP-286)
     * @param invitationId reference back to the {@code parent_invitations} row
     */
    void recordParentInviteSent(Long parentId, Long invitationId);

    /**
     * Records intent to notify parent that a payment has been confirmed.
     *
     * <p>Wave 105 effect: log + outbox insert with {@code event_type=PAYMENT_CONFIRM},
     * {@code context_id=invoiceId}, payload includes {@code amount} (VND minor units)
     * and {@code childName} (per VN-localization §3 — VN-friendly sample data).
     *
     * @param parentId   parent recipient
     * @param invoiceId  reference back to {@code invoices} row that was paid
     * @param amountVnd  amount paid in VND minor units (1.500.000đ = 1500000 BIGINT)
     * @param childName  child name for the human-readable template variable
     */
    void recordPaymentConfirm(Long parentId, Long invoiceId, Long amountVnd, String childName);

    /**
     * Records intent to notify parent about a child's attendance event today
     * (typically end-of-day recap, future scope = real-time absent alert).
     *
     * <p>Wave 105 effect: log + outbox insert with {@code event_type=ATTENDANCE_ALERT},
     * {@code context_id=childId}.
     *
     * @param parentId    parent recipient
     * @param childId     reference back to {@code students} row
     * @param status      attendance status enum value (PRESENT / ABSENT / LATE / EXCUSED)
     */
    void recordAttendanceAlert(Long parentId, Long childId, String status);
}
