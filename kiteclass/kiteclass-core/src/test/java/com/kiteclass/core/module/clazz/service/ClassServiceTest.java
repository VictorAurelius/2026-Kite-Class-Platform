package com.kiteclass.core.module.clazz.service;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.module.clazz.dto.ClassCodeResponse;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.dto.CreateScheduleRequest;
import com.kiteclass.core.module.clazz.dto.UpdateClassRequest;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.mapper.ClassMapper;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.clazz.service.impl.ClassServiceImpl;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.testutil.ClassTestDataBuilder;
import com.kiteclass.core.testutil.CourseTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClassServiceImpl}.
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

        @Mock
        private ClassRepository classRepository;
        @Mock
        private ClassSessionRepository sessionRepository;
        @Mock
        private CourseRepository courseRepository;
        @Mock
        private ClassMapper classMapper;

        @InjectMocks
        private ClassServiceImpl classService;

        private Class defaultClass;
        private Course defaultCourse;
        private ClassResponse defaultResponse;
        private static final UUID TENANT_ID = ClassTestDataBuilder.DEFAULT_TENANT;

        @BeforeEach
        void setUp() {
                TenantContext.setCurrentTenant(TENANT_ID);
                defaultClass = ClassTestDataBuilder.createDefaultClass();
                defaultCourse = CourseTestDataBuilder.createDefaultCourse();
                defaultResponse = buildClassResponse(defaultClass);
        }

        @AfterEach
        void tearDown() {
                TenantContext.clear();
        }

        // =========================================================================
        // createClass
        // =========================================================================

        @Test
        void createClass_shouldCreateSuccessfully() {
                // Given
                CreateClassRequest request = ClassTestDataBuilder.createDefaultCreateRequest();
                when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultCourse));
                when(classRepository.existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(
                                any(), any(), any())).thenReturn(false);
                when(classMapper.toEntity(request)).thenReturn(defaultClass);
                when(classRepository.save(any())).thenReturn(defaultClass);
                when(classMapper.toResponse(defaultClass)).thenReturn(defaultResponse);

                // When
                ClassResponse result = classService.createClass(1L, request);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.name()).isEqualTo("English B1 - Evening Class");
                verify(classRepository).save(any(Class.class));
        }

        @Test
        void createClass_shouldThrow_whenCourseNotFound() {
                when(courseRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> classService.createClass(999L,
                                ClassTestDataBuilder.createDefaultCreateRequest()))
                                .isInstanceOf(EntityNotFoundException.class)
                                .hasMessageContaining("COURSE_NOT_FOUND");
        }

        @Test
        void createClass_shouldThrow_whenCourseArchived() {
                defaultCourse.setStatus(CourseStatus.ARCHIVED);
                when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultCourse));

                assertThatThrownBy(() -> classService.createClass(1L,
                                ClassTestDataBuilder.createDefaultCreateRequest()))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_COURSE_ARCHIVED");
        }

        @Test
        void createClass_shouldThrow_whenNameAlreadyExists() {
                // classMapper.toEntity() is NOT called when name check throws first
                when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultCourse));
                when(classRepository.existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(
                                any(), any(), any())).thenReturn(true);

                assertThatThrownBy(() -> classService.createClass(1L,
                                ClassTestDataBuilder.createDefaultCreateRequest()))
                                .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        void createClass_shouldThrow_whenEndDateBeforeStartDate() {
                CreateClassRequest badRequest = new CreateClassRequest(
                                "Test Class", null, null, null, null,
                                LocalDate.of(2026, 5, 1),
                                LocalDate.of(2026, 3, 1), // endDate < startDate
                                20);
                // classMapper.toEntity() is NOT called — validateDates() throws first
                when(courseRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultCourse));
                when(classRepository.existsByNameAndCourseIdAndInstanceIdAndDeletedFalse(
                                any(), any(), any())).thenReturn(false);

                assertThatThrownBy(() -> classService.createClass(1L, badRequest))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_INVALID_DATES");
        }

        // =========================================================================
        // updateClass
        // =========================================================================

        @Test
        void updateClass_shouldUpdateDescription_whenInProgress() {
                Class inProgressClass = ClassTestDataBuilder.createClassWithStatus(ClassStatus.IN_PROGRESS);
                UpdateClassRequest request = new UpdateClassRequest(
                                null, "Updated description", null, null, null, null, null, null);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inProgressClass));
                when(classRepository.save(any())).thenReturn(inProgressClass);
                when(classMapper.toResponse(any())).thenReturn(defaultResponse);

                ClassResponse result = classService.updateClass(1L, request);

                assertThat(result).isNotNull();
                verify(classRepository).save(any());
        }

        @Test
        void updateClass_shouldThrow_whenScheduleChanged_forInProgressClass() {
                Class inProgressClass = ClassTestDataBuilder.createClassWithStatus(ClassStatus.IN_PROGRESS);
                UpdateClassRequest request = new UpdateClassRequest(
                                null, null, "New Schedule", null, null, null, null, null);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inProgressClass));

                assertThatThrownBy(() -> classService.updateClass(1L, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_SCHEDULE_LOCKED");
        }

        @Test
        void updateClass_shouldThrow_whenCompleted() {
                Class completedClass = ClassTestDataBuilder.createClassWithStatus(ClassStatus.COMPLETED);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(completedClass));

                assertThatThrownBy(() -> classService.updateClass(1L,
                                ClassTestDataBuilder.createDefaultUpdateRequest()))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_READ_ONLY");
        }

        @Test
        void updateClass_shouldThrow_whenReducingMaxStudentsBelowEnrolled() {
                defaultClass.setCurrentEnrolled(15);
                defaultClass.setMaxStudents(20);
                UpdateClassRequest request = new UpdateClassRequest(
                                null, null, null, null, null, null, null, 10); // reduce to 10 < 15
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));

                assertThatThrownBy(() -> classService.updateClass(1L, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_CAPACITY_VIOLATION");
        }

        // =========================================================================
        // Lifecycle transitions
        // =========================================================================

        @Test
        void startClass_shouldTransitionToInProgress() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(classRepository.save(any())).thenReturn(defaultClass);
                when(classMapper.toResponse(any())).thenReturn(defaultResponse);

                classService.startClass(1L);

                verify(classRepository).save(argThat(c -> c.getStatus() == ClassStatus.IN_PROGRESS
                                && c.getStartedAt() != null));
        }

        @Test
        void startClass_shouldThrow_whenAlreadyInProgress() {
                Class inProgress = ClassTestDataBuilder.createClassWithStatus(ClassStatus.IN_PROGRESS);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inProgress));

                assertThatThrownBy(() -> classService.startClass(1L))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_CANNOT_START");
        }

        @Test
        void completeClass_shouldTransitionToCompleted() {
                Class inProgress = ClassTestDataBuilder.createClassWithStatus(ClassStatus.IN_PROGRESS);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inProgress));
                when(classRepository.save(any())).thenReturn(inProgress);
                when(classMapper.toResponse(any())).thenReturn(defaultResponse);

                classService.completeClass(1L);

                verify(classRepository).save(argThat(c -> c.getStatus() == ClassStatus.COMPLETED
                                && c.getCompletedAt() != null));
        }

        @Test
        void completeClass_shouldThrow_whenNotInProgress() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));

                assertThatThrownBy(() -> classService.completeClass(1L))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_CANNOT_COMPLETE");
        }

        @Test
        void cancelClass_shouldTransitionToCancelled_fromScheduled() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(classRepository.save(any())).thenReturn(defaultClass);
                when(classMapper.toResponse(any())).thenReturn(defaultResponse);

                classService.cancelClass(1L, ClassTestDataBuilder.createCancelRequest());

                verify(classRepository).save(argThat(c -> c.getStatus() == ClassStatus.CANCELLED
                                && c.getCancelledAt() != null));
        }

        @Test
        void cancelClass_shouldThrow_whenCompleted() {
                Class completed = ClassTestDataBuilder.createClassWithStatus(ClassStatus.COMPLETED);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(completed));

                assertThatThrownBy(() -> classService.cancelClass(1L,
                                ClassTestDataBuilder.createCancelRequest()))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_CANNOT_CANCEL");
        }

        // =========================================================================
        // deleteClass
        // =========================================================================

        @Test
        void deleteClass_shouldSoftDelete_whenScheduledWithNoStudents() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(classRepository.save(any())).thenReturn(defaultClass);

                classService.deleteClass(1L);

                verify(sessionRepository).softDeleteByClassId(1L);
                verify(classRepository).save(argThat(Class::isDeleted));
        }

        @Test
        void deleteClass_shouldThrow_whenHasStudents() {
                Class classWithStudents = ClassTestDataBuilder.createClassWithEnrolledStudents(5);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(classWithStudents));

                assertThatThrownBy(() -> classService.deleteClass(1L))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_HAS_STUDENTS");
        }

        @Test
        void deleteClass_shouldThrow_whenNotScheduled() {
                Class inProgress = ClassTestDataBuilder.createClassWithStatus(ClassStatus.IN_PROGRESS);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inProgress));

                assertThatThrownBy(() -> classService.deleteClass(1L))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_CANNOT_DELETE");
        }

        // =========================================================================
        // generateClassCode
        // =========================================================================

        @Test
        void generateClassCode_shouldGenerateUniqueCode_whenNoCustomCode() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(classRepository.existsByClassCodeAndDeletedFalse(any())).thenReturn(false);
                when(classRepository.save(any())).thenReturn(defaultClass);

                ClassCodeResponse result = classService.generateClassCode(1L,
                                ClassTestDataBuilder.createCodeRequest());

                assertThat(result.classCode()).isNotNull();
                assertThat(result.classCode()).hasSize(8);
                verify(classRepository).save(argThat(c -> c.getClassCode() != null));
        }

        @Test
        void generateClassCode_shouldUseCustomCode_whenProvided() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(classRepository.existsByClassCodeAndDeletedFalse("MYCODE01")).thenReturn(false);
                when(classRepository.save(any())).thenReturn(defaultClass);

                ClassCodeResponse result = classService.generateClassCode(1L,
                                ClassTestDataBuilder.createCustomCodeRequest("MYCODE01"));

                assertThat(result.classCode()).isEqualTo("MYCODE01");
        }

        @Test
        void generateClassCode_shouldThrow_whenCustomCodeAlreadyTaken() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(classRepository.existsByClassCodeAndDeletedFalse("TAKEN123")).thenReturn(true);

                assertThatThrownBy(() -> classService.generateClassCode(1L,
                                ClassTestDataBuilder.createCustomCodeRequest("TAKEN123")))
                                .isInstanceOf(DuplicateResourceException.class);
        }

        // =========================================================================
        // createSchedule
        // =========================================================================

        @Test
        void createSchedule_shouldGenerateSessions_forDateRange() {
                // Class: 2026-03-02 (Mon) to 2026-03-16 (Mon) — 3 weeks
                defaultClass.setStartDate(LocalDate.of(2026, 3, 2));
                defaultClass.setEndDate(LocalDate.of(2026, 3, 16));
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(sessionRepository.findMaxSessionNumberByClassId(1L)).thenReturn(0);
                when(sessionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
                when(classMapper.toSessionResponse(any())).thenAnswer(inv -> {
                        ClassSession s = inv.getArgument(0);
                        return new ClassSessionResponse(s.getId(), s.getClassId(), s.getSessionNumber(),
                                        s.getSessionDate(), s.getStartTime(), s.getEndTime(), null, null,
                                        s.getStatus(), s.getAttendanceTaken());
                });

                // Mon-Wed-Fri: 2026-03-02(Mon) to 2026-03-16(Mon)
                // Week1: 3/2, 3/4, 3/6 | Week2: 3/9, 3/11, 3/13 | Extra: 3/16 = 7 sessions
                CreateScheduleRequest req = new CreateScheduleRequest(
                                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                                LocalTime.of(18, 0), LocalTime.of(20, 0));

                List<ClassSessionResponse> sessions = classService.createSchedule(1L, req);

                assertThat(sessions).hasSize(7); // 2 full weeks (6) + Mon 3/16 (1)
        }

        @Test
        void createSchedule_shouldThrow_whenNoDates() {
                defaultClass.setStartDate(null);
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));

                assertThatThrownBy(() -> classService.createSchedule(1L,
                                ClassTestDataBuilder.createScheduleRequest()))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_NO_DATES");
        }

        @Test
        void createSchedule_shouldThrow_whenEndTimeBeforeStartTime() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                CreateScheduleRequest badTime = new CreateScheduleRequest(
                                List.of(DayOfWeek.MONDAY), LocalTime.of(20, 0), LocalTime.of(18, 0));

                assertThatThrownBy(() -> classService.createSchedule(1L, badTime))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("CLASS_INVALID_TIME");
        }

        // =========================================================================
        // getClass / listClasses
        // =========================================================================

        @Test
        void getClass_shouldReturn_whenFound() {
                when(classRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(defaultClass));
                when(classMapper.toResponse(defaultClass)).thenReturn(defaultResponse);

                ClassResponse result = classService.getClass(1L);
                assertThat(result).isNotNull();
        }

        @Test
        void getClass_shouldThrow_whenNotFound() {
                when(classRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> classService.getClass(999L))
                                .isInstanceOf(EntityNotFoundException.class)
                                .hasMessageContaining("CLASS_NOT_FOUND");
        }

        @Test
        void listClasses_shouldReturnPaginatedResults() {
                Page<Class> page = new PageImpl<>(List.of(defaultClass));
                when(classRepository.findByCourseIdAndDeletedFalse(eq(1L), any(Pageable.class)))
                                .thenReturn(page);
                when(classMapper.toResponse(any())).thenReturn(defaultResponse);

                PageResponse<ClassResponse> result = classService.listClasses(1L, 0, 20);

                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getTotalElements()).isEqualTo(1);
        }

        // =========================================================================
        // Helpers
        // =========================================================================

        private ClassResponse buildClassResponse(Class clazz) {
                return new ClassResponse(
                                clazz.getId(), clazz.getCourseId(), clazz.getName(), clazz.getDescription(),
                                clazz.getSchedule(), clazz.getLocationType(), clazz.getLocationDetail(),
                                clazz.getStartDate(), clazz.getEndDate(), clazz.getMaxStudents(),
                                clazz.getCurrentEnrolled(), clazz.getClassCode(), clazz.getCodeExpiresAt(),
                                clazz.getStatus(), clazz.getStartedAt(), clazz.getCompletedAt(),
                                clazz.getCancelledAt(), clazz.getCreatedAt(), clazz.getUpdatedAt());
        }
}
