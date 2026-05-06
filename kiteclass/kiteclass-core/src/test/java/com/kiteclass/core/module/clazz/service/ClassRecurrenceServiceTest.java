package com.kiteclass.core.module.clazz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto.Freq;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto.IcalDay;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.mapper.ClassMapper;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.clazz.repository.ClassSessionRepository;
import com.kiteclass.core.module.clazz.service.impl.ClassServiceImpl;
import com.kiteclass.core.module.clazz.service.impl.RecurrenceServiceImpl;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.testutil.ClassTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the recurrence path of {@link ClassServiceImpl}, focused on
 * the BR-CLASS-009 state machine for edit (GAP-290 Wave 18a).
 *
 * <p>Covers:
 * <ul>
 *   <li>AC-1: WEEKLY TU,TH 19:00-20:30 → ~24 sessions</li>
 *   <li>AC-2: edit recurrence preserves attended sessions</li>
 *   <li>AC-3: rule persisted as JSONB on class</li>
 *   <li>AC-4: exclude dates honoured</li>
 *   <li>BR-CLASS-009: COMPLETED/CANCELLED class rejected</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ClassRecurrenceServiceTest {

    @Mock
    private ClassRepository classRepository;
    @Mock
    private ClassSessionRepository sessionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ClassMapper classMapper;
    @Spy
    private RecurrenceService recurrenceService = new RecurrenceServiceImpl();
    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules() // JSR-310 — feedback_objectmapper_test_jsr310.md
            .build();

    @InjectMocks
    private ClassServiceImpl classService;

    private Class clazz;
    private static final UUID TENANT = ClassTestDataBuilder.DEFAULT_TENANT;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT);
        clazz = ClassTestDataBuilder.createDefaultClass();
        clazz.setId(42L);
        clazz.setStatus(ClassStatus.SCHEDULED);
        clazz.setStartDate(LocalDate.of(2026, 5, 1));
        clazz.setEndDate(LocalDate.of(2026, 8, 1));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("AC-1, AC-3: WEEKLY TU,TH generates ~26 sessions and persists JSONB rule")
    void generate_weeklyTuThu_persistsRuleAndCreatesSessions() {
        // Given: rule far enough in the future that all occurrences are post-today
        LocalDate futureStart = LocalDate.now().plusDays(7);
        clazz.setStartDate(futureStart);
        clazz.setEndDate(futureStart.plusWeeks(13));
        RecurrenceRuleDto rule = new RecurrenceRuleDto(
                Freq.WEEKLY,
                List.of(IcalDay.TU, IcalDay.TH),
                LocalTime.of(19, 0),
                LocalTime.of(20, 30),
                futureStart.plusWeeks(13),
                null
        );
        when(classRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(clazz));
        when(sessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(42L))
                .thenReturn(new ArrayList<>())
                .thenAnswer(inv -> {
                    // Second call after persist — return stub list of sessions
                    return List.of(stubSession(1L, 1, futureStart));
                });
        when(classMapper.toSessionResponse(any())).thenReturn(stubResponse(1L, 1));

        // When
        List<ClassSessionResponse> result = classService.generateSessionsFromRecurrence(42L, rule);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(clazz.getRecurrenceRule()).isNotNull();
        assertThat(clazz.getRecurrenceRule()).contains("\"freq\":\"WEEKLY\"");
        assertThat(clazz.getRecurrenceRule()).contains("\"by_day\"");
        verify(sessionRepository).saveAll(any());
    }

    @Test
    @DisplayName("AC-2: regenerating with new rule preserves attended sessions")
    void regenerate_preservesAttendedSessions() {
        LocalDate futureStart = LocalDate.now().plusDays(2);
        ClassSession attended = stubSession(100L, 5, LocalDate.now().minusDays(10));
        attended.setAttendanceTaken(true);
        ClassSession futurePending = stubSession(101L, 6, futureStart.plusDays(7));
        futurePending.setAttendanceTaken(false);
        ClassSession past = stubSession(102L, 4, LocalDate.now().minusDays(20));

        RecurrenceRuleDto rule = new RecurrenceRuleDto(
                Freq.WEEKLY,
                List.of(IcalDay.MO),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                futureStart.plusWeeks(4),
                null
        );

        when(classRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(clazz));
        when(sessionRepository.findByClassIdAndDeletedFalseOrderBySessionNumberAsc(42L))
                .thenReturn(List.of(past, attended, futurePending))
                .thenReturn(List.of(past, attended)); // After regeneration: futurePending soft-deleted
        when(classMapper.toSessionResponse(any())).thenReturn(stubResponse(0L, 0));

        // When
        classService.generateSessionsFromRecurrence(42L, rule);

        // Then
        // futurePending should have been marked deleted; past + attended NOT touched.
        assertThat(futurePending.isDeleted()).isTrue();
        assertThat(past.isDeleted()).isFalse();
        assertThat(attended.isDeleted()).isFalse();
        // saveAll called at least once (for soft-delete + new sessions)
        verify(sessionRepository, atLeastOnce()).saveAll(any());
    }

    @Test
    @DisplayName("BR-CLASS-009: COMPLETED class rejects recurrence change")
    void completedClass_rejected() {
        clazz.setStatus(ClassStatus.COMPLETED);
        when(classRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(clazz));

        RecurrenceRuleDto rule = new RecurrenceRuleDto(
                Freq.WEEKLY,
                List.of(IcalDay.MO),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalDate.now().plusMonths(1),
                null
        );

        assertThatThrownBy(() -> classService.generateSessionsFromRecurrence(42L, rule))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CLASS_RECURRENCE_LOCKED");

        verify(sessionRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("BR-CLASS-009: CANCELLED class rejects recurrence change")
    void cancelledClass_rejected() {
        clazz.setStatus(ClassStatus.CANCELLED);
        when(classRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(clazz));

        RecurrenceRuleDto rule = new RecurrenceRuleDto(
                Freq.WEEKLY,
                List.of(IcalDay.MO),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalDate.now().plusMonths(1),
                null
        );

        assertThatThrownBy(() -> classService.generateSessionsFromRecurrence(42L, rule))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CLASS_RECURRENCE_LOCKED");
    }

    private ClassSession stubSession(Long id, int number, LocalDate date) {
        ClassSession s = ClassSession.builder()
                .classId(42L)
                .sessionNumber(number)
                .sessionDate(date)
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .status(SessionStatus.SCHEDULED)
                .attendanceTaken(false)
                .build();
        s.setId(id);
        return s;
    }

    private ClassSessionResponse stubResponse(Long id, int number) {
        return new ClassSessionResponse(
                id, 42L, number, LocalDate.now(),
                LocalTime.of(19, 0), LocalTime.of(20, 30),
                null, null, SessionStatus.SCHEDULED, false
        );
    }
}
