package com.kiteclass.core.module.clazz.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.clazz.dto.CancelClassRequest;
import com.kiteclass.core.module.clazz.dto.ClassCodeResponse;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.dto.CreateScheduleRequest;
import com.kiteclass.core.module.clazz.dto.GenerateClassCodeRequest;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto;
import com.kiteclass.core.module.clazz.dto.RescheduleClassRequest;
import com.kiteclass.core.module.clazz.dto.UpdateClassRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Service interface for Class module operations.
 *
 * <p>Handles all class lifecycle management including CRUD, status transitions,
 * class code generation, and schedule management.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Validated
public interface ClassService {

    /**
     * Creates a new class within a course.
     *
     * @param courseId course ID (from path variable)
     * @param request  class creation data
     * @return created class response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if course not found
     * @throws com.kiteclass.core.common.exception.BusinessException       if course is ARCHIVED
     * @throws com.kiteclass.core.common.exception.DuplicateResourceException if name already exists in course
     */
    ClassResponse createClass(Long courseId, @Valid CreateClassRequest request);

    /**
     * Updates an existing class.
     *
     * @param classId class ID
     * @param request fields to update
     * @return updated class response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException  if class not found
     * @throws com.kiteclass.core.common.exception.BusinessException        if update not allowed for current status
     */
    ClassResponse updateClass(Long classId, @Valid UpdateClassRequest request);

    /**
     * Retrieves a class by ID.
     *
     * @param classId class ID
     * @return class response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if class not found
     */
    ClassResponse getClass(Long classId);

    /**
     * Lists classes for a course, paginated.
     *
     * @param courseId course ID
     * @param page     page number (0-based)
     * @param size     page size
     * @return paginated class list
     */
    PageResponse<ClassResponse> listClasses(Long courseId, int page, int size);

    /**
     * Starts a class (SCHEDULED → IN_PROGRESS).
     *
     * @param classId class ID
     * @return updated class response
     * @throws com.kiteclass.core.common.exception.BusinessException if class is not SCHEDULED
     */
    ClassResponse startClass(Long classId);

    /**
     * Completes a class (IN_PROGRESS → COMPLETED).
     *
     * @param classId class ID
     * @return updated class response
     * @throws com.kiteclass.core.common.exception.BusinessException if class is not IN_PROGRESS
     */
    ClassResponse completeClass(Long classId);

    /**
     * Cancels a class (SCHEDULED/IN_PROGRESS → CANCELLED).
     *
     * @param classId class ID
     * @param request cancellation reason
     * @return updated class response
     * @throws com.kiteclass.core.common.exception.BusinessException if class cannot be cancelled
     */
    ClassResponse cancelClass(Long classId, @Valid CancelClassRequest request);

    /**
     * Soft deletes a class.
     * Only SCHEDULED classes with 0 enrollments can be deleted.
     *
     * @param classId class ID
     * @throws com.kiteclass.core.common.exception.BusinessException if class cannot be deleted
     */
    void deleteClass(Long classId);

    /**
     * Generates or regenerates a class enrollment code.
     *
     * @param classId class ID
     * @param request optional custom code and expiry
     * @return the generated code response
     * @throws com.kiteclass.core.common.exception.DuplicateResourceException if custom code is taken
     */
    ClassCodeResponse generateClassCode(Long classId, @Valid GenerateClassCodeRequest request);

    /**
     * Creates a schedule and generates class sessions.
     *
     * @param classId class ID
     * @param request schedule configuration
     * @return list of generated sessions
     * @throws com.kiteclass.core.common.exception.BusinessException if class has no start/end dates
     */
    List<ClassSessionResponse> createSchedule(Long classId, @Valid CreateScheduleRequest request);

    /**
     * Lists all sessions for a class.
     *
     * @param classId class ID
     * @return ordered list of sessions
     */
    List<ClassSessionResponse> listSessions(Long classId);

    /**
     * Generates {@link com.kiteclass.core.module.clazz.entity.ClassSession} entries
     * from a structured RFC 5545 RRULE subset (GAP-290 Wave 18a).
     *
     * <p>Idempotent on edit per BR-CLASS-009 state machine:
     * <ul>
     *   <li>Future {@code SCHEDULED} sessions with {@code attendanceTaken=false} are
     *       soft-deleted and regenerated from the new rule.</li>
     *   <li>Past sessions ({@code sessionDate &lt; today}) are preserved untouched.</li>
     *   <li>Sessions with {@code attendanceTaken=true} are preserved regardless of date.</li>
     * </ul>
     *
     * <p>The rule is persisted as JSONB on {@code classes.recurrence_rule}.
     * Callers should ensure the class is in {@code SCHEDULED} or {@code IN_PROGRESS}
     * status (BR-CLASS-006); locked once {@code COMPLETED}/{@code CANCELLED}.
     *
     * @param classId class ID
     * @param rule    recurrence rule (Phase 1: WEEKLY only)
     * @return list of all sessions for the class after regeneration (preserved + new)
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if class not found
     * @throws com.kiteclass.core.common.exception.BusinessException       if class no longer editable
     * @throws com.kiteclass.core.common.exception.ValidationException     if rule fails Phase 1 validation
     * @since GAP-290 Wave 18a
     */
    List<ClassSessionResponse> generateSessionsFromRecurrence(Long classId,
                                                              @Valid RecurrenceRuleDto rule);

    /**
     * Reschedules a class — preserves attendance + grade history, mutates start/end dates,
     * writes audit log, publishes {@code ClassRescheduledEvent} via Outbox.
     *
     * <p>Per cross-bucket LOCKED decision §3.6 (Wave beta-readiness-4):
     * <ul>
     *   <li>NO new {@code ClassStatus.RESCHEDULED} enum (preserves backward compat)</li>
     *   <li>Audit captures: previousStartDate, previousEndDate, rescheduledByUserId,
     *       rescheduledAt, reasonCategory, reasonNotes</li>
     *   <li>Default notification consumer = no-op; email path activates only when
     *       {@code kite.class.reschedule.notify.enabled=true}</li>
     *   <li>Notification classification = OPERATIONAL (bypass marketing_consented gate)</li>
     * </ul>
     *
     * @param classId class ID
     * @param request reschedule request (newStartDate + newEndDate + reasonCategory + optional notes)
     * @return updated class response
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if class not found
     * @throws com.kiteclass.core.common.exception.ValidationException     if dates invalid or class not SCHEDULED
     * @since Wave beta-readiness-4 Bucket D (GAP-291)
     */
    ClassResponse rescheduleClass(Long classId, @Valid RescheduleClassRequest request);
}
