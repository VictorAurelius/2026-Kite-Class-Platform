package com.kiteclass.core.module.childprotection.exception;

import com.kiteclass.core.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a caller attempts to soft-delete a child-protection
 * {@code Incident} whose {@code retention_until} timestamp is in the future.
 *
 * <p>Per BR-CHILD-PROTECT-008 (Phase 1C v1.5, GAP-359 sub-task 359.1) once an
 * incident transitions to {@code CLOSED} the row enters a 7-year mandatory
 * retention window. Soft-delete is BLOCKED at the service layer until the
 * window expires; the {@code RetentionLifecycleService} cron then secure-deletes
 * the row and appends an audit-log entry recording the lifecycle transition.
 *
 * <p>Compliance: PDPL Decree 13/2023/NĐ-CP Art 16 + Luật Trẻ em 2016 Đ.51
 * follow-through + BLHS Đ.147 statute-of-limitations alignment.
 *
 * <p>HTTP status: 409 Conflict — the resource exists but its lifecycle state
 * forbids the requested operation.
 *
 * @since 5.x (Wave 24 Bucket A — GAP-359 sub-task 359.1)
 */
public class RetentionWindowActiveException extends BusinessException {

    /**
     * Creates an exception describing an active retention window.
     *
     * <p>Use the {@code (String errorCode, Object... args)} ctor of
     * {@link BusinessException}. Pass {@code (Object) id} to force varargs
     * resolution per the deprecated-ctor overload-resolution gotcha.
     *
     * @param errorCode  message-source key, typically
     *                   {@code "INCIDENT_RETENTION_WINDOW_ACTIVE"}
     * @param args       message-format arguments — usually
     *                   {@code (Object) retentionUntilIso} so the user-facing
     *                   message names the date the row becomes deletable
     */
    public RetentionWindowActiveException(String errorCode, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, args);
    }
}
