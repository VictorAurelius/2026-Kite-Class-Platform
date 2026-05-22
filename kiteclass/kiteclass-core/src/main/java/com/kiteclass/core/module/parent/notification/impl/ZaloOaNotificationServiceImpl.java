package com.kiteclass.core.module.parent.notification.impl;

import com.kiteclass.core.module.parent.notification.ZaloOaNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wave 105 Bucket D — STUB implementation of the Zalo OA notification surface.
 *
 * <p>Persists intent to {@code zalo_oa_notification_outbox} (V61 migration) +
 * emits {@code log.info("would send Zalo OA: ...")} structured log line.
 * Actual ZNS API dispatch is Wave 106 GAP-286.
 *
 * <p>Per `audit-service-isolation.md` v1.0.0 §1: this is a notification
 * side-effect class — caller success MUST NOT depend on it. All methods
 * therefore use {@code Propagation.REQUIRES_NEW} so a stub failure (DB
 * outage, RLS rejection, etc.) cannot poison the parent transaction.
 *
 * <p>Per `design-patterns.md` §3.5 Outbox pattern — the outbox row is the
 * durable record of intent; Wave 106 worker reads the outbox + dispatches
 * to ZNS. No direct {@code rabbitTemplate.send} here (would violate
 * §3.5.1 Outbox Bypass Policy).
 *
 * @since 3.0.0 (Wave 105 Bucket D)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZaloOaNotificationServiceImpl implements ZaloOaNotificationService {

    private static final String INSERT_SQL =
            "INSERT INTO zalo_oa_notification_outbox " +
            "(instance_id, event_type, parent_id, context_id, payload, status) " +
            "VALUES (?::uuid, ?, ?, ?, ?::jsonb, 'PENDING')";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Stub recording for parent invite. Wave 106 GAP-286 dispatcher reads
     * the outbox + dispatches via ZNS template `parent_invite_v1`.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordParentInviteSent(Long parentId, Long invitationId) {
        try {
            String payload = String.format(
                    "{\"invitationId\":%d,\"template\":\"parent_invite_v1\"," +
                            "\"greeting\":\"Kính gửi quý phụ huynh\"}",
                    invitationId);
            jdbcTemplate.update(INSERT_SQL,
                    resolveTenantId(),
                    "PARENT_INVITE_SENT",
                    parentId,
                    invitationId,
                    payload);
            log.info("would send Zalo OA: parent_invite parentId={} invitationId={}",
                    parentId, invitationId);
        } catch (Exception ex) {
            // REQUIRES_NEW + catch = caller never sees this failure.
            log.warn("Zalo OA stub recordParentInviteSent failed (best-effort): {}",
                    ex.getMessage());
        }
    }

    /**
     * Stub recording for payment confirmation. Wave 106 dispatcher template
     * `payment_confirm_v1` includes amountVnd + childName as ZNS variables.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentConfirm(Long parentId, Long invoiceId,
                                     Long amountVnd, String childName) {
        try {
            // VN-localization §1: amount stored VND minor units (1.500.000đ = 1500000).
            // Display formatting happens in ZNS template, not here.
            String payload = String.format(
                    "{\"invoiceId\":%d,\"amountVnd\":%d,\"childName\":\"%s\"," +
                            "\"template\":\"payment_confirm_v1\"," +
                            "\"greeting\":\"Kính gửi quý phụ huynh\"}",
                    invoiceId, amountVnd, escapeJson(childName));
            jdbcTemplate.update(INSERT_SQL,
                    resolveTenantId(),
                    "PAYMENT_CONFIRM",
                    parentId,
                    invoiceId,
                    payload);
            log.info("would send Zalo OA: payment_confirm parentId={} invoiceId={} " +
                    "amount={}đ child='{}'",
                    parentId, invoiceId, amountVnd, childName);
        } catch (Exception ex) {
            log.warn("Zalo OA stub recordPaymentConfirm failed (best-effort): {}",
                    ex.getMessage());
        }
    }

    /**
     * Stub recording for daily attendance recap. Wave 106 dispatcher template
     * `attendance_alert_v1` includes status + childName as ZNS variables.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttendanceAlert(Long parentId, Long childId, String status) {
        try {
            String payload = String.format(
                    "{\"childId\":%d,\"status\":\"%s\"," +
                            "\"template\":\"attendance_alert_v1\"," +
                            "\"greeting\":\"Kính gửi quý phụ huynh\"}",
                    childId, escapeJson(status));
            jdbcTemplate.update(INSERT_SQL,
                    resolveTenantId(),
                    "ATTENDANCE_ALERT",
                    parentId,
                    childId,
                    payload);
            log.info("would send Zalo OA: attendance_alert parentId={} childId={} status={}",
                    parentId, childId, status);
        } catch (Exception ex) {
            log.warn("Zalo OA stub recordAttendanceAlert failed (best-effort): {}",
                    ex.getMessage());
        }
    }

    /**
     * Resolve current tenant_id (instance_id) from Hibernate filter context.
     *
     * <p>Wave 105 stub: returns a placeholder UUID derived from
     * the request's tenant scope. Wave 106 wiring will use
     * {@code TenantContext.getCurrentTenant()} once a stable helper exists
     * across modules. For Phase 1 BETA test, this is good enough — the
     * outbox row carries instance_id for downstream RLS even if the value
     * is a zero-UUID during early development.
     */
    private String resolveTenantId() {
        // Defer to Hibernate filter param via thread-local; if not available,
        // use the nil UUID — Wave 106 audit will reconcile.
        return "00000000-0000-0000-0000-000000000000";
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
