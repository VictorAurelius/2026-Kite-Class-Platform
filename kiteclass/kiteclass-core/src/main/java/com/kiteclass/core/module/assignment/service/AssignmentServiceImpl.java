package com.kiteclass.core.module.assignment.service;

import com.kiteclass.core.common.constant.AssignmentStatus;
import com.kiteclass.core.common.constant.SubmissionStatus;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.common.security.AuthorizationBean;
import com.kiteclass.core.module.assignment.dto.request.CreateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.GradeSubmissionRequest;
import com.kiteclass.core.module.assignment.dto.request.SubmitAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.request.UpdateAssignmentRequest;
import com.kiteclass.core.module.assignment.dto.response.AssignmentResponse;
import com.kiteclass.core.module.assignment.dto.response.SubmissionResponse;
import com.kiteclass.core.module.assignment.entity.Assignment;
import com.kiteclass.core.module.assignment.entity.Submission;
import com.kiteclass.core.module.assignment.event.AssignmentCreatedEvent;
import com.kiteclass.core.module.assignment.event.AssignmentGradedEvent;
import com.kiteclass.core.module.assignment.mapper.AssignmentMapper;
import com.kiteclass.core.module.assignment.repository.AssignmentRepository;
import com.kiteclass.core.module.assignment.repository.SubmissionRepository;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of AssignmentService.
 *
 * @author KiteClass Team
 * @since 2.7.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final ClassRepository classRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final AssignmentMapper assignmentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthorizationBean authz;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, Long teacherId) {
        // 1. Validate class exists
        Class clazz = classRepository.findByIdAndDeletedFalse(request.getClassId())
            .orElseThrow(() -> new EntityNotFoundException("CLASS_NOT_FOUND", (Object) request.getClassId()));
        log.debug("Creating assignment for class: {}", clazz.getName());

        // 2. Permission check: Only MAIN_TEACHER can create assignments.
        // GAP-1301/GAP-1299: ADMIN/OWNER (tenant-admin) carry no numeric reference id
        // (teacherId == null) and have no TeacherClass row, so they bypass the per-class
        // MAIN_TEACHER check — mirrors AuthorizationBean.isAdmin(). The teacher id is the
        // token-derived reference id (X-User-Reference-Id), NOT a spoofable client header.
        if (!authz.isAdmin()) {
            TeacherClass teacherClass = teacherClassRepository
                    .findByTeacherIdAndClassId(teacherId, request.getClassId())
                    .orElseThrow(() -> new PermissionDeniedException("TEACHER_NOT_IN_CLASS"));

            if (teacherClass.getRole() != TeacherClassRole.MAIN_TEACHER) {
                throw new PermissionDeniedException("ONLY_MAIN_TEACHER_CAN_CREATE_ASSIGNMENT");
            }
        }

        // 3. Map to entity
        // created_by (audit actor UUID) is auto-populated by JPA auditing from
        // UserContext (X-User-Id) — see GAP-795. No longer stashes numeric teacherId.
        Assignment assignment = assignmentMapper.toEntity(request);
        assignment.setInstanceId(TenantContext.getCurrentTenant());

        // Set default late penalty if not provided
        if (assignment.getLatePenaltyPercent() == null) {
            assignment.setLatePenaltyPercent(BigDecimal.valueOf(10.0));
        }

        // 4. Save
        Assignment savedAssignment = assignmentRepository.save(assignment);

        log.info("Created assignment {} for class {} by teacher {}",
            savedAssignment.getId(), request.getClassId(), teacherId);

        // Publish event for grade component initialization
        eventPublisher.publishEvent(new AssignmentCreatedEvent(this, savedAssignment));

        return assignmentMapper.toResponse(savedAssignment);
    }

    @Override
    @Transactional
    public AssignmentResponse updateAssignment(Long id, UpdateAssignmentRequest request, Long teacherId) {
        // 1. Find assignment
        Assignment assignment = findAssignmentById(id);

        // 2. Permission check
        validateTeacherPermission(assignment, teacherId);

        // 3. Validate: Cannot update published/closed assignment significantly
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            if (request.getMaxScore() != null || request.getWeightPercent() != null) {
                throw new ValidationException("CANNOT_CHANGE_SCORES_AFTER_PUBLISH", new Object[0]);
            }
        }

        // 4. Update fields
        assignmentMapper.updateEntity(request, assignment);

        // 5. Save
        Assignment updatedAssignment = assignmentRepository.save(assignment);

        log.info("Updated assignment {} by teacher {}", id, teacherId);

        return assignmentMapper.toResponse(updatedAssignment);
    }

    @Override
    @Transactional
    public AssignmentResponse publishAssignment(Long id, Long teacherId) {
        // 1. Find assignment
        Assignment assignment = findAssignmentById(id);

        // 2. Permission check
        validateTeacherPermission(assignment, teacherId);

        // 3. Validate status
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new ValidationException("ASSIGNMENT_ALREADY_PUBLISHED", new Object[0]);
        }

        // 4. Publish
        assignment.publish();
        Assignment savedAssignment = assignmentRepository.save(assignment);

        log.info("Published assignment {} by teacher {}", id, teacherId);

        return assignmentMapper.toResponse(savedAssignment);
    }

    @Override
    @Transactional
    public AssignmentResponse closeAssignment(Long id, Long teacherId) {
        // 1. Find assignment
        Assignment assignment = findAssignmentById(id);

        // 2. Permission check
        validateTeacherPermission(assignment, teacherId);

        // 3. Close
        assignment.close();
        Assignment savedAssignment = assignmentRepository.save(assignment);

        log.info("Closed assignment {} by teacher {}", id, teacherId);

        return assignmentMapper.toResponse(savedAssignment);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long id, Long teacherId) {
        // 1. Find assignment
        Assignment assignment = findAssignmentById(id);

        // 2. Permission check
        validateTeacherPermission(assignment, teacherId);

        // 3. Validate: Cannot delete if has submissions
        long submissionCount = submissionRepository.countByAssignmentIdAndDeletedFalse(id);
        if (submissionCount > 0) {
            throw new ValidationException("CANNOT_DELETE_ASSIGNMENT_WITH_SUBMISSIONS", new Object[0]);
        }

        // 4. Soft delete
        assignment.setDeleted(true);
        assignmentRepository.save(assignment);

        log.info("Deleted assignment {} by teacher {}", id, teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        Assignment assignment = findAssignmentById(id);
        return assignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByClass(Long classId) {
        List<Assignment> assignments = assignmentRepository
            .findByClassIdAndDeletedFalseOrderByDueDateDesc(classId);
        return assignmentMapper.toResponseList(assignments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getPublishedAssignmentsByClass(Long classId) {
        List<Assignment> assignments = assignmentRepository
            .findByClassIdAndStatusAndDeletedFalseOrderByDueDateDesc(classId, AssignmentStatus.PUBLISHED);
        return assignmentMapper.toResponseList(assignments);
    }

    @Override
    @Transactional
    public SubmissionResponse submitAssignment(SubmitAssignmentRequest request, Long studentId) {
        // 1. Find assignment
        Assignment assignment = findAssignmentById(request.getAssignmentId());

        // 2. Validate: Assignment must accept submissions
        if (!assignment.isAcceptingSubmissions()) {
            throw new ValidationException("ASSIGNMENT_NOT_ACCEPTING_SUBMISSIONS", new Object[0]);
        }

        // 3. Check if student already submitted
        submissionRepository.findByAssignmentIdAndStudentIdAndDeletedFalse(
            request.getAssignmentId(), studentId)
            .ifPresent(existing -> {
                throw new ValidationException("STUDENT_ALREADY_SUBMITTED", new Object[0]);
            });

        // 4. Create submission
        LocalDateTime now = LocalDateTime.now();
        Submission submission = Submission.builder()
            .assignmentId(assignment.getId())
            .studentId(studentId)
            .submissionDate(now)
            .contentUrl(request.getContentUrl())
            .notes(request.getNotes())
            .status(SubmissionStatus.PENDING)
            .build();
        submission.setInstanceId(TenantContext.getCurrentTenant());

        // 5. Save
        Submission savedSubmission = submissionRepository.save(submission);

        log.info("Student {} submitted assignment {}", studentId, assignment.getId());

        return enrichSubmissionResponse(savedSubmission, assignment.getDueDate());
    }

    @Override
    @Transactional
    public SubmissionResponse gradeSubmission(Long submissionId, GradeSubmissionRequest request, Long teacherId) {
        // 1. Find submission
        Submission submission = findSubmissionById(submissionId);

        // 2. Find assignment
        Assignment assignment = findAssignmentById(submission.getAssignmentId());

        // 3. Permission check: Only MAIN_TEACHER or assigned grader
        validateTeacherPermission(assignment, teacherId);

        // 4. Validate score <= max_score
        if (request.getScore().compareTo(assignment.getMaxScore()) > 0) {
            throw new ValidationException("SCORE_EXCEEDS_MAX_SCORE",
                request.getScore(), assignment.getMaxScore());
        }

        // 5. Calculate late penalty
        BigDecimal penaltyMultiplier = assignment.calculateLatePenaltyMultiplier(submission.getSubmissionDate());

        // 6. Grade submission
        submission.grade(request.getScore(), penaltyMultiplier, teacherId, request.getFeedback());

        // 7. Save
        Submission gradedSubmission = submissionRepository.save(submission);

        // 8. Publish event for Grade Module
        Class clazz = classRepository.findByIdAndDeletedFalse(assignment.getClassId())
            .orElseThrow(() -> new EntityNotFoundException("CLASS_NOT_FOUND", (Object) assignment.getClassId()));

        eventPublisher.publishEvent(new AssignmentGradedEvent(this, gradedSubmission, clazz.getId()));

        log.info("Teacher {} graded submission {} with score {} (adjusted: {})",
            teacherId, submissionId, request.getScore(), gradedSubmission.getAdjustedScore());

        return enrichSubmissionResponse(gradedSubmission, assignment.getDueDate());
    }

    @Override
    @Transactional
    public SubmissionResponse returnSubmission(Long submissionId, Long teacherId) {
        // 1. Find submission
        Submission submission = findSubmissionById(submissionId);

        // 2. Find assignment
        Assignment assignment = findAssignmentById(submission.getAssignmentId());

        // 3. Permission check
        validateTeacherPermission(assignment, teacherId);

        // 4. Validate: Must be graded first
        if (submission.getStatus() != SubmissionStatus.GRADED) {
            throw new ValidationException("SUBMISSION_NOT_GRADED", new Object[0]);
        }

        // 5. Return to student
        submission.returnToStudent();
        Submission returnedSubmission = submissionRepository.save(submission);

        log.info("Teacher {} returned submission {} to student {}",
            teacherId, submissionId, submission.getStudentId());

        return enrichSubmissionResponse(returnedSubmission, assignment.getDueDate());
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(Long id) {
        Submission submission = findSubmissionById(id);
        Assignment assignment = findAssignmentById(submission.getAssignmentId());
        return enrichSubmissionResponse(submission, assignment.getDueDate());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId) {
        Assignment assignment = findAssignmentById(assignmentId);
        List<Submission> submissions = submissionRepository
            .findByAssignmentIdAndDeletedFalseOrderBySubmissionDateDesc(assignmentId);
        return submissions.stream()
            .map(sub -> enrichSubmissionResponse(sub, assignment.getDueDate()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getStudentSubmission(Long assignmentId, Long studentId) {
        Assignment assignment = findAssignmentById(assignmentId);
        Submission submission = submissionRepository
            .findByAssignmentIdAndStudentIdAndDeletedFalse(assignmentId, studentId)
            .orElse(null);

        if (submission == null) {
            return null;
        }

        return enrichSubmissionResponse(submission, assignment.getDueDate());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByStudent(Long studentId) {
        List<Submission> submissions = submissionRepository
            .findByStudentIdAndDeletedFalseOrderBySubmissionDateDesc(studentId);

        // For each submission, get assignment due date for late calculation
        return submissions.stream()
            .map(submission -> {
                Assignment assignment = findAssignmentById(submission.getAssignmentId());
                return enrichSubmissionResponse(submission, assignment.getDueDate());
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getPendingGradingByClass(Long classId) {
        List<Submission> submissions = submissionRepository.findPendingGradingByClass(classId);

        // For each submission, get assignment due date
        return submissions.stream()
            .map(submission -> {
                Assignment assignment = findAssignmentById(submission.getAssignmentId());
                return enrichSubmissionResponse(submission, assignment.getDueDate());
            })
            .toList();
    }

    // ==================== Helper Methods ====================

    private Assignment findAssignmentById(Long id) {
        return assignmentRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new EntityNotFoundException("ASSIGNMENT_NOT_FOUND", (Object) id));
    }

    private Submission findSubmissionById(Long id) {
        return submissionRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new EntityNotFoundException("SUBMISSION_NOT_FOUND", (Object) id));
    }

    private void validateTeacherPermission(Assignment assignment, Long teacherId) {
        // GAP-1301/GAP-1299: ADMIN/OWNER bypass the per-class MAIN_TEACHER check (no numeric
        // reference id, no TeacherClass row) — mirrors AuthorizationBean.isAdmin(). The teacher
        // id is the token-derived reference id (X-User-Reference-Id), NOT a spoofable header.
        if (authz.isAdmin()) {
            return;
        }

        TeacherClass teacherClass = teacherClassRepository
                .findByTeacherIdAndClassId(teacherId, assignment.getClassId())
                .orElseThrow(() -> new PermissionDeniedException("TEACHER_NOT_IN_CLASS"));

        if (teacherClass.getRole() != TeacherClassRole.MAIN_TEACHER) {
            throw new PermissionDeniedException("ONLY_MAIN_TEACHER_CAN_MANAGE_ASSIGNMENT");
        }
    }

    /**
     * Enrich SubmissionResponse with computed fields (isLate, penaltyApplied).
     */
    private SubmissionResponse enrichSubmissionResponse(Submission submission, LocalDateTime dueDate) {
        SubmissionResponse response = assignmentMapper.toSubmissionResponse(submission);

        // Compute isLate
        if (submission.getSubmissionDate() != null && dueDate != null) {
            response.setIsLate(submission.isLate(dueDate));
        } else {
            response.setIsLate(false);
        }

        // Compute penaltyApplied
        if (submission.getScore() != null && submission.getAdjustedScore() != null) {
            response.setPenaltyApplied(submission.getScore().subtract(submission.getAdjustedScore()));
        } else {
            response.setPenaltyApplied(BigDecimal.ZERO);
        }

        return response;
    }
}
