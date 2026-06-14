package com.kiteclass.core.module.grade.service;

import com.kiteclass.core.common.constant.GradeStatus;
import com.kiteclass.core.common.constant.TeacherClassRole;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.PermissionDeniedException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.common.security.AuthorizationBean;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.grade.dto.request.CreateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.request.FinalizeGradeRequest;
import com.kiteclass.core.module.grade.dto.request.UpdateGradeComponentRequest;
import com.kiteclass.core.module.grade.dto.response.GradeComponentResponse;
import com.kiteclass.core.module.grade.dto.response.GradeResponse;
import com.kiteclass.core.module.grade.dto.response.GradingSummaryResponse;
import com.kiteclass.core.module.grade.dto.response.TranscriptResponse;
import com.kiteclass.core.module.grade.entity.Grade;
import com.kiteclass.core.module.grade.entity.GradeComponent;
import com.kiteclass.core.module.grade.entity.GradingScale;
import com.kiteclass.core.module.grade.entity.Transcript;
import com.kiteclass.core.module.grade.mapper.GradeMapper;
import com.kiteclass.core.module.grade.repository.GradeComponentRepository;
import com.kiteclass.core.module.grade.repository.GradeRepository;
import com.kiteclass.core.module.grade.repository.GradingScaleRepository;
import com.kiteclass.core.module.grade.repository.TranscriptRepository;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.module.teacher.entity.TeacherClass;
import com.kiteclass.core.module.teacher.repository.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of GradeService.
 *
 * @author KiteClass Team
 * @since 2.7.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final GradeComponentRepository gradeComponentRepository;
    private final GradingScaleRepository gradingScaleRepository;
    private final TranscriptRepository transcriptRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final com.kiteclass.core.module.enrollment.repository.EnrollmentRepository enrollmentRepository;
    private final com.kiteclass.core.module.assignment.repository.AssignmentRepository assignmentRepository;
    private final GradeMapper gradeMapper;
    private final AuthorizationBean authz;

    @Override
    @Transactional
    public GradeResponse initializeGrade(Long studentId, Long classId) {
        // 1. Validate student exists
        Student student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) studentId));
        log.debug("Initializing grade for student: {} ({})", student.getName(), student.getEmail());

        // 2. Validate class exists
        Class clazz = classRepository.findByIdAndDeletedFalse(classId)
                .orElseThrow(() -> new EntityNotFoundException("CLASS_NOT_FOUND", (Object) classId));
        log.debug("Initializing grade for class: {}", clazz.getName());

        // 3. Check if grade already exists (unique constraint)
        Optional<Grade> existing = gradeRepository.findByStudentIdAndClassIdAndDeletedFalse(studentId, classId);
        if (existing.isPresent()) {
            log.warn("Grade already exists for student {} in class {}", studentId, classId);
            return gradeMapper.toResponse(existing.get());
        }

        // 4. Create new grade
        Grade grade = Grade.builder()
                .studentId(studentId)
                .classId(classId)
                .status(GradeStatus.IN_PROGRESS)
                .passThreshold(BigDecimal.valueOf(50.0))
                .build();

        grade.setInstanceId(TenantContext.getCurrentTenant());

        Grade savedGrade = gradeRepository.save(grade);

        log.info("Initialized grade {} for student {} in class {}", savedGrade.getId(), studentId, classId);

        return gradeMapper.toResponse(savedGrade);
    }

    @Override
    @Transactional(readOnly = true)
    public GradeResponse getGradeById(Long id) {
        Grade grade = findGradeById(id);
        return gradeMapper.toResponse(grade);
    }

    @Override
    @Transactional(readOnly = true)
    public GradeResponse getStudentGrade(Long studentId, Long classId) {
        Grade grade = gradeRepository.findByStudentIdAndClassIdAndDeletedFalse(studentId, classId)
                .orElseThrow(() -> new EntityNotFoundException("GRADE_NOT_FOUND",
                        (Object) (studentId + "/" + classId)));

        return gradeMapper.toResponse(grade);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeResponse> getGradesByStudent(Long studentId) {
        List<Grade> grades = gradeRepository.findByStudentIdAndDeletedFalseOrderByCalculatedAtDesc(studentId);
        return gradeMapper.toResponseList(grades);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradingSummaryResponse> getGradesByClass(Long classId) {
        List<Grade> grades = gradeRepository.findByClassIdAndDeletedFalseOrderByFinalScoreDesc(classId);
        return gradeMapper.toSummaryResponseList(grades);
    }

    @Override
    @Transactional
    public GradeComponentResponse addOrUpdateComponent(CreateGradeComponentRequest request) {
        // 1. Find grade
        Grade grade = findGradeById(request.getGradeId());

        // 2. Validate: Cannot modify finalized grade
        if (grade.isFinalized()) {
            throw new ValidationException("CANNOT_MODIFY_FINALIZED_GRADE", new Object[0]);
        }

        // 3. Check if component already exists (by type and refId)
        Optional<GradeComponent> existing = gradeComponentRepository
                .findByGradeIdAndComponentTypeAndComponentRefId(
                        request.getGradeId(),
                        request.getComponentType(),
                        request.getComponentRefId()
                );

        GradeComponent component;
        if (existing.isPresent()) {
            // Update existing component
            component = existing.get();
            component.setComponentName(request.getComponentName());
            component.setScore(request.getScore());
            component.setMaxScore(request.getMaxScore());
            component.setWeightPercent(request.getWeightPercent());
            component.calculateWeightedScore();
        } else {
            // Create new component
            component = gradeMapper.toEntity(request);
            component.calculateWeightedScore();
            grade.addComponent(component);
        }

        // 4. Save
        GradeComponent savedComponent = gradeComponentRepository.save(component);

        log.info("Added/updated component {} for grade {}", savedComponent.getId(), request.getGradeId());

        return gradeMapper.toComponentResponse(savedComponent);
    }

    @Override
    @Transactional
    public GradeComponentResponse updateComponent(Long componentId, UpdateGradeComponentRequest request) {
        // 1. Find component
        GradeComponent component = gradeComponentRepository.findByIdAndDeletedFalse(componentId)
                .orElseThrow(() -> new EntityNotFoundException("GRADE_COMPONENT_NOT_FOUND", (Object) componentId));

        // 2. Validate: Cannot modify finalized grade
        if (component.getGrade().isFinalized()) {
            throw new ValidationException("CANNOT_MODIFY_FINALIZED_GRADE", new Object[0]);
        }

        // 3. Update fields (only non-null values)
        if (request.getComponentName() != null) {
            component.setComponentName(request.getComponentName());
        }
        if (request.getScore() != null) {
            component.setScore(request.getScore());
        }
        if (request.getMaxScore() != null) {
            component.setMaxScore(request.getMaxScore());
        }
        if (request.getWeightPercent() != null) {
            component.setWeightPercent(request.getWeightPercent());
        }

        // 4. Recalculate weighted score
        component.calculateWeightedScore();

        // 5. Save
        GradeComponent updatedComponent = gradeComponentRepository.save(component);

        log.info("Updated component {}", componentId);

        return gradeMapper.toComponentResponse(updatedComponent);
    }

    @Override
    @Transactional
    public void deleteComponent(Long componentId, Long teacherId) {
        // 1. Find component
        GradeComponent component = gradeComponentRepository.findByIdAndDeletedFalse(componentId)
                .orElseThrow(() -> new EntityNotFoundException("GRADE_COMPONENT_NOT_FOUND", (Object) componentId));

        // 2. Permission check
        validateTeacherPermission(component.getGrade(), teacherId);

        // 3. Validate: Cannot modify finalized grade
        if (component.getGrade().isFinalized()) {
            throw new ValidationException("CANNOT_MODIFY_FINALIZED_GRADE", new Object[0]);
        }

        // 4. Soft delete
        component.setDeleted(true);
        gradeComponentRepository.save(component);

        log.info("Deleted component {} by teacher {}", componentId, teacherId);
    }

    @Override
    @Transactional
    public GradeResponse calculateFinalScore(Long gradeId) {
        // 1. Find grade
        Grade grade = findGradeById(gradeId);

        // 2. Calculate final score from components
        BigDecimal finalScore = grade.calculateFinalScore();

        // 3. Map to letter grade and GPA
        mapGradeToLetterAndGpa(grade, finalScore);

        // 4. Save
        Grade savedGrade = gradeRepository.save(grade);

        log.info("Calculated final score {} for grade {}", finalScore, gradeId);

        return gradeMapper.toResponse(savedGrade);
    }

    @Override
    @Transactional
    public GradeResponse finalizeGrade(Long gradeId, FinalizeGradeRequest request) {
        // 1. Find grade
        Grade grade = findGradeById(gradeId);

        // GAP-1000: derive the acting teacher from the authenticated principal
        // (X-User-Reference-Id via UserContext), NOT request.getTeacherId() which is
        // client-supplied and spoofable. ADMIN bypasses the MAIN_TEACHER check (BR-GRD-007).
        Long actingTeacherId = UserContext.getCurrentReferenceId();

        // 2. Permission check: Only MAIN_TEACHER (or ADMIN) can finalize
        validateTeacherPermission(grade, actingTeacherId);

        // 3. Validate: Already finalized
        if (grade.isFinalized()) {
            throw new ValidationException("GRADE_ALREADY_FINALIZED", new Object[0]);
        }

        // 4. Validate: Weights must sum to 100%
        if (!grade.isWeightsSumValid()) {
            throw new ValidationException("GRADE_WEIGHTS_MUST_SUM_TO_100", new Object[0]);
        }

        // 5. Calculate final score if not already done
        if (grade.getFinalScore() == null) {
            grade.calculateFinalScore();
            mapGradeToLetterAndGpa(grade, grade.getFinalScore());
        }

        // 6. Set comments
        if (request.getComments() != null) {
            grade.setComments(request.getComments());
        }

        // 7. Finalize — record the authenticated actor, not the request body value
        grade.finalize(actingTeacherId);

        // 8. Save
        Grade savedGrade = gradeRepository.save(grade);

        log.info("Finalized grade {} by teacher {}", gradeId, actingTeacherId);

        return gradeMapper.toResponse(savedGrade);
    }

    @Override
    @Transactional
    public GradeResponse unfinalizeGrade(Long gradeId) {
        // 1. Find grade
        Grade grade = findGradeById(gradeId);

        // 2. Validate: Not finalized
        if (!grade.isFinalized()) {
            throw new ValidationException("GRADE_NOT_FINALIZED", new Object[0]);
        }

        // 3. Unfinalize
        grade.unfinalize();

        // 4. Save
        Grade savedGrade = gradeRepository.save(grade);

        log.info("Unfinalized grade {}", gradeId);

        return gradeMapper.toResponse(savedGrade);
    }

    @Override
    @Transactional
    public TranscriptResponse generateTranscript(Long studentId, String semester) {
        // 1. Validate student exists
        Student student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new EntityNotFoundException("STUDENT_NOT_FOUND", (Object) studentId));
        log.debug("Generating transcript for student: {} ({})", student.getName(), student.getEmail());

        // 2. Check if transcript already exists
        Optional<Transcript> existing = transcriptRepository
                .findByStudentIdAndSemesterAndDeletedFalse(studentId, semester);
        if (existing.isPresent()) {
            log.warn("Transcript already exists for student {} in semester {}", studentId, semester);
            return gradeMapper.toTranscriptResponse(existing.get());
        }

        // 3. Get all finalized grades for student
        List<Grade> finalizedGrades = gradeRepository.findFinalizedGradesByStudentId(studentId);

        if (finalizedGrades.isEmpty()) {
            throw new ValidationException("NO_FINALIZED_GRADES_FOR_STUDENT", new Object[0]);
        }

        // 4. Calculate semester GPA (assuming all grades are for this semester)
        BigDecimal totalGpaPoints = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        int passedCourses = 0;
        int failedCourses = 0;

        for (Grade grade : finalizedGrades) {
            if (grade.getGpa() != null) {
                // For simplicity, assume each course is 3.0 credits
                BigDecimal credits = BigDecimal.valueOf(3.0);
                totalGpaPoints = totalGpaPoints.add(grade.getGpa().multiply(credits));
                totalCredits = totalCredits.add(credits);

                if (grade.isPassed()) {
                    passedCourses++;
                } else {
                    failedCourses++;
                }
            }
        }

        BigDecimal semesterGpa = totalCredits.compareTo(BigDecimal.ZERO) > 0
                ? totalGpaPoints.divide(totalCredits, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. Create transcript
        Transcript transcript = Transcript.builder()
                .studentId(studentId)
                .semester(semester)
                .academicYear(extractAcademicYear(semester))
                .totalCredits(totalCredits)
                .semesterGpa(semesterGpa)
                .cumulativeGpa(semesterGpa) // Will be updated later
                .totalCourses(finalizedGrades.size())
                .passedCourses(passedCourses)
                .failedCourses(failedCourses)
                .build();

        transcript.setInstanceId(TenantContext.getCurrentTenant());

        Transcript savedTranscript = transcriptRepository.save(transcript);

        log.info("Generated transcript {} for student {} in semester {}", savedTranscript.getId(), studentId, semester);

        return gradeMapper.toTranscriptResponse(savedTranscript);
    }

    @Override
    @Transactional(readOnly = true)
    public TranscriptResponse getTranscript(Long studentId, String semester) {
        Transcript transcript = transcriptRepository.findByStudentIdAndSemesterAndDeletedFalse(studentId, semester)
                .orElseThrow(() -> new EntityNotFoundException("TRANSCRIPT_NOT_FOUND",
                        (Object) (studentId + "/" + semester)));

        return gradeMapper.toTranscriptResponse(transcript);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TranscriptResponse> getTranscriptsByStudent(Long studentId) {
        List<Transcript> transcripts = transcriptRepository
                .findByStudentIdAndDeletedFalseOrderBySemesterDesc(studentId);

        return gradeMapper.toTranscriptResponseList(transcripts);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> calculateClassStatistics(Long classId) {
        // 1. Validate class exists
        Class clazz = classRepository.findByIdAndDeletedFalse(classId)
                .orElseThrow(() -> new EntityNotFoundException("CLASS_NOT_FOUND", (Object) classId));
        log.debug("Calculating statistics for class: {}", clazz.getName());

        // 2. Get all grades for class
        List<Grade> grades = gradeRepository.findByClassIdAndDeletedFalseOrderByFinalScoreDesc(classId);

        // 3. Calculate statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", grades.size());

        long finalizedCount = gradeRepository.countFinalizedGradesByClassId(classId);
        stats.put("finalizedGrades", finalizedCount);

        Double averageScore = gradeRepository.calculateAverageFinalScoreByClassId(classId);
        stats.put("averageScore", averageScore != null ? averageScore : 0.0);

        long passedCount = grades.stream().filter(Grade::isPassed).count();
        long failedCount = grades.stream().filter(Grade::isFailed).count();
        stats.put("passedStudents", passedCount);
        stats.put("failedStudents", failedCount);

        double passRate = grades.isEmpty() ? 0.0 : (double) passedCount / grades.size() * 100;
        stats.put("passRate", passRate);

        log.info("Calculated statistics for class {}: {}", classId, stats);

        return stats;
    }

    @Override
    @Transactional
    public GradeResponse initializeGradeForEnrollment(Long enrollmentId) {
        log.debug("Initializing grade for enrollment: {}", enrollmentId);

        // 1. Fetch enrollment with student and class info
        com.kiteclass.core.module.enrollment.entity.Enrollment enrollment =
                enrollmentRepository.findByIdAndDeletedFalse(enrollmentId)
                        .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) enrollmentId));

        // 2. Delegate to existing initializeGrade (handles duplicate check)
        return initializeGrade(enrollment.getStudentId(), enrollment.getClassId());
    }

    @Override
    @Transactional
    public int initializeGradeComponentsForAssignment(Long assignmentId, Long classId) {
        log.info("Initializing grade components for assignment {} in class {}", assignmentId, classId);

        // 1. Validate assignment exists
        com.kiteclass.core.module.assignment.entity.Assignment assignment =
                assignmentRepository.findByIdAndDeletedFalse(assignmentId)
                        .orElseThrow(() -> new EntityNotFoundException("ASSIGNMENT_NOT_FOUND", (Object) assignmentId));

        // 2. Find all ACTIVE enrollments in class
        List<com.kiteclass.core.module.enrollment.entity.Enrollment> enrollments =
                enrollmentRepository.findByClassIdAndStatusAndDeletedFalse(
                        classId,
                        com.kiteclass.core.common.constant.EnrollmentStatus.ACTIVE,
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        if (enrollments.isEmpty()) {
            log.warn("No active enrollments found for class {}", classId);
            return 0;
        }

        // 3. For each enrollment, ensure Grade exists and add component
        int componentsCreated = 0;
        for (com.kiteclass.core.module.enrollment.entity.Enrollment enrollment : enrollments) {
            try {
                // 3a. Get or create grade
                Grade grade = gradeRepository
                        .findByStudentIdAndClassIdAndDeletedFalse(
                                enrollment.getStudentId(),
                                enrollment.getClassId())
                        .orElseGet(() -> {
                            Grade newGrade = Grade.builder()
                                    .studentId(enrollment.getStudentId())
                                    .classId(enrollment.getClassId())
                                    .status(com.kiteclass.core.common.constant.GradeStatus.IN_PROGRESS)
                                    .passThreshold(BigDecimal.valueOf(50.0))
                                    .build();
                            newGrade.setInstanceId(com.kiteclass.core.common.context.TenantContext.getCurrentTenant());
                            return gradeRepository.save(newGrade);
                        });

                // 3b. Check if component already exists (idempotent)
                Optional<GradeComponent> existing = gradeComponentRepository
                        .findByGradeIdAndComponentTypeAndComponentRefId(
                                grade.getId(),
                                com.kiteclass.core.common.constant.GradeComponentType.ASSIGNMENT,
                                assignmentId);

                if (existing.isEmpty()) {
                    // 3c. Create component with initial values
                    GradeComponent component = GradeComponent.builder()
                            .grade(grade)
                            .componentType(com.kiteclass.core.common.constant.GradeComponentType.ASSIGNMENT)
                            .componentName(assignment.getTitle())
                            .componentRefId(assignmentId)
                            .score(BigDecimal.ZERO)  // Not submitted yet
                            .maxScore(assignment.getMaxScore())
                            .weightPercent(assignment.getWeightPercent())
                            .build();

                    component.calculateWeightedScore();
                    component.setInstanceId(com.kiteclass.core.common.context.TenantContext.getCurrentTenant());

                    gradeComponentRepository.save(component);
                    componentsCreated++;

                    log.debug("Created grade component for student {} in assignment {}",
                            enrollment.getStudentId(), assignmentId);
                }

            } catch (Exception e) {
                log.error("Failed to create grade component for student {} in assignment {}: {}",
                        enrollment.getStudentId(), assignmentId, e.getMessage());
                // Continue with next enrollment (don't fail entire batch)
            }
        }

        log.info("Created {} grade components for assignment {}", componentsCreated, assignmentId);
        return componentsCreated;
    }

    // ==================== Helper Methods ====================

    /**
     * Find grade by ID (not deleted).
     *
     * @param id grade ID
     * @return grade entity
     * @throws EntityNotFoundException if not found
     */
    private Grade findGradeById(Long id) {
        return gradeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("GRADE_NOT_FOUND", (Object) id));
    }

    /**
     * Validate teacher permission for grade operations.
     * Only MAIN_TEACHER of the class can modify grades.
     *
     * @param grade the grade
     * @param teacherId the teacher ID
     * @throws PermissionDeniedException if teacher is not MAIN_TEACHER
     */
    private void validateTeacherPermission(Grade grade, Long teacherId) {
        // GAP-1000/GAP-1301: ADMIN / PLATFORM_ADMIN / OWNER have full access (BR-GRD-007) and
        // have no TeacherClass row, so they bypass the MAIN_TEACHER check. Uses the shared
        // AuthorizationBean.isAdmin() (now OWNER-inclusive, matching @authz.hasAccessToGrade*
        // guards at the controller) so an OWNER passing the @PreAuthorize gate is not then
        // rejected here. The teacher id is the token-derived reference id, NOT a client header.
        if (authz.isAdmin()) {
            return;
        }

        TeacherClass teacherClass = teacherClassRepository
                .findByTeacherIdAndClassId(teacherId, grade.getClassId())
                .orElseThrow(() -> new PermissionDeniedException("TEACHER_NOT_IN_CLASS"));

        if (teacherClass.getRole() != TeacherClassRole.MAIN_TEACHER) {
            throw new PermissionDeniedException("ONLY_MAIN_TEACHER_CAN_MODIFY_GRADE");
        }
    }

    /**
     * Map final score to letter grade and GPA using grading scale.
     *
     * @param grade the grade entity
     * @param finalScore the final score (0-100)
     */
    private void mapGradeToLetterAndGpa(Grade grade, BigDecimal finalScore) {
        UUID instanceId = TenantContext.getCurrentTenant();

        // Find matching grading scale
        GradingScale scale = gradingScaleRepository.findByInstanceIdAndScoreRange(instanceId, finalScore)
                .orElseGet(() -> {
                    // Fallback to default scale if tenant doesn't have custom scale
                    List<GradingScale> defaultScales = gradingScaleRepository.findDefaultGradingScales();
                    return defaultScales.stream()
                            .filter(s -> s.containsScore(finalScore))
                            .findFirst()
                            .orElseThrow(() -> new EntityNotFoundException("GRADING_SCALE_NOT_FOUND",
                                    (Object) finalScore));
                });

        // Set letter grade and GPA
        grade.setGradeMapping(scale.getLetterGrade(), scale.getGpaValue());

        log.debug("Mapped score {} to letter grade {} (GPA {})", finalScore, scale.getLetterGrade(), scale.getGpaValue());
    }

    /**
     * Extract academic year from semester string.
     * Example: "Spring 2026" → 2026
     *
     * @param semester the semester string
     * @return academic year
     */
    private Integer extractAcademicYear(String semester) {
        try {
            String[] parts = semester.split(" ");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("Could not extract academic year from semester: {}", semester);
            return null;
        }
    }
}
