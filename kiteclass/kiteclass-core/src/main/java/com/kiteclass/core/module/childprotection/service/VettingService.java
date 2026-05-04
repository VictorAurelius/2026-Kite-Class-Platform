package com.kiteclass.core.module.childprotection.service;

import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Service contract for staff vetting workflow (Phase 1B foundation).
 *
 * <p>Encapsulates the state machine + AES-256 roundtrip + soft-delete.
 * RBAC enforcement is performed by {@code VettingController} (delegating to
 * the X-User-Roles header forwarded by the Gateway) — service layer assumes
 * caller already passed RBAC.
 *
 * <p>Allowed transitions (BR-VETTING-001):
 * <ul>
 *   <li>{@code PENDING → SUBMITTED}</li>
 *   <li>{@code SUBMITTED → INTERVIEW_DONE}</li>
 *   <li>{@code INTERVIEW_DONE → APPROVED}</li>
 *   <li>{@code INTERVIEW_DONE → REJECTED}</li>
 *   <li>{@code APPROVED → EXPIRED} (triggered by expiry — Phase 1B follow-up
 *       schedules a cron; manual transition allowed for tests)</li>
 * </ul>
 * Any other transition throws {@code ValidationException("VETTING_INVALID_TRANSITION")}.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
public interface VettingService {

    /**
     * Create a new vetting record in PENDING state.
     *
     * @param teacherId          FK to users.id (the teacher being vetted)
     * @param lltpNumber         optional LLTP doc identifier (encrypted at rest)
     * @param policeCheckDetails optional narrative (encrypted at rest)
     * @param expiresAt          optional expiry hint; final value set on APPROVE
     * @return persisted record
     */
    Vetting create(Long teacherId, String lltpNumber, String policeCheckDetails, Instant expiresAt);

    /** Look up a non-deleted vetting record by id. */
    Vetting findById(Long id);

    /** Find the latest active vetting record for a teacher (or empty). */
    Vetting findLatestForTeacher(Long teacherId);

    /** Paged list with optional status filter. */
    Page<Vetting> findAll(VettingStatus status, Pageable pageable);

    /**
     * Transition a vetting record from its current status to {@code target}
     * per BR-VETTING-001 state machine.
     *
     * @param id              the vetting record id
     * @param target          the requested target status
     * @param decidedByUserId user id of the safeguarding officer making the
     *                        decision (recorded in audit cols on transition
     *                        to APPROVED/REJECTED)
     * @return updated record
     */
    Vetting transition(Long id, VettingStatus target, Long decidedByUserId);

    /** Soft-delete a vetting record (BR-VETTING-005). */
    void softDelete(Long id);
}
