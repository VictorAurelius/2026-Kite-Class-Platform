package com.kiteclass.core.module.clazz.service;

import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto.Freq;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto.IcalDay;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.service.impl.RecurrenceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RecurrenceServiceImpl}. GAP-290 Wave 18a.
 *
 * <p>Covers AC-1..6 from GAP-290:
 * <ul>
 *   <li>AC-1: WEEKLY TU,TH 19:00-20:30 until 2026-08-01 → expected session count</li>
 *   <li>AC-4: exclude dates supported</li>
 *   <li>AC-5: multi-day weekly (TU+TH)</li>
 *   <li>AC-6: edge cases (1 session, 100 sessions, leap year)</li>
 * </ul>
 *
 * <p>Pure planning tests — no DB, no Spring context. State machine for edit
 * (AC-2 preserve attended sessions) tested in {@link ClassRecurrenceServiceTest}.
 */
class RecurrenceServiceTest {

    private final RecurrenceService service = new RecurrenceServiceImpl();

    @Nested
    @DisplayName("AC-1, AC-5: Multi-day WEEKLY generation")
    class GenerateOccurrencesTests {

        @Test
        @DisplayName("WEEKLY TU,TH 19:00-20:30 from 2026-05-01 until 2026-08-01 → ~24 sessions")
        void weeklyTuesdayThursday_generatesExpectedCount() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.TU, IcalDay.TH),
                    LocalTime.of(19, 0),
                    LocalTime.of(20, 30),
                    LocalDate.of(2026, 8, 1),
                    null
            );

            List<RecurrenceServiceImpl.Occurrence> occurrences =
                    service.planOccurrences(LocalDate.of(2026, 5, 1), rule);

            // From 2026-05-01 (FRI) to 2026-08-01 (SAT) inclusive:
            // 13 Tuesdays + 13 Thursdays = 26 occurrences
            assertThat(occurrences).hasSize(26);
            // First should be Tuesday 2026-05-05
            assertThat(occurrences.get(0).date()).isEqualTo(LocalDate.of(2026, 5, 5));
            assertThat(occurrences.get(0).date().getDayOfWeek().getValue()).isEqualTo(2); // TUESDAY
        }

        @Test
        @DisplayName("WEEKLY TU only with 1-week range → 1 session")
        void singleSession_oneWeek() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.TU),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0),
                    LocalDate.of(2026, 5, 6), // Wed — should include only TU 05-05
                    null
            );

            List<RecurrenceServiceImpl.Occurrence> occurrences =
                    service.planOccurrences(LocalDate.of(2026, 5, 5), rule);

            assertThat(occurrences).hasSize(1);
            assertThat(occurrences.get(0).date()).isEqualTo(LocalDate.of(2026, 5, 5));
        }

        @Test
        @DisplayName("AC-6 edge: 100+ sessions over 2 years")
        void hundredSessions_twoYears() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.MO),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 0),
                    LocalDate.of(2028, 1, 3), // ~104 weeks
                    null
            );

            List<RecurrenceServiceImpl.Occurrence> occurrences =
                    service.planOccurrences(LocalDate.of(2026, 1, 5), rule); // Mon

            // 2026-01-05 (Mon) to 2028-01-03 (Mon) = 105 Mondays
            assertThat(occurrences).hasSize(105);
            // All occurrences must be Monday
            assertThat(occurrences).allMatch(o -> o.date().getDayOfWeek().getValue() == 1);
        }

        @Test
        @DisplayName("AC-6 edge: leap year — Feb 29, 2028 (Tuesday)")
        void leapYear_feb29Tuesday_included() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.TU),
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 0),
                    LocalDate.of(2028, 3, 7),
                    null
            );

            List<RecurrenceServiceImpl.Occurrence> occurrences =
                    service.planOccurrences(LocalDate.of(2028, 2, 22), rule);

            assertThat(occurrences).extracting(RecurrenceServiceImpl.Occurrence::date)
                    .contains(LocalDate.of(2028, 2, 29));
        }
    }

    @Nested
    @DisplayName("AC-4: Exclude dates")
    class ExcludeDatesTests {

        @Test
        @DisplayName("Exclude dates skipped from generation")
        void excludeDates_areSkipped() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.MO),
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    LocalDate.of(2026, 5, 25),
                    Set.of(LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 18))
            );

            List<RecurrenceServiceImpl.Occurrence> occurrences =
                    service.planOccurrences(LocalDate.of(2026, 5, 4), rule);

            // Mondays in range: 05-04, 05-11, 05-18, 05-25 → 4 candidates
            // After excludes (05-11, 05-18): 2 remain
            assertThat(occurrences).hasSize(2);
            assertThat(occurrences).extracting(RecurrenceServiceImpl.Occurrence::date)
                    .containsExactly(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 25));
        }

        @Test
        @DisplayName("Empty exclude_dates set is handled like null")
        void emptyExcludeDates_acceptsAll() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.MO),
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    LocalDate.of(2026, 5, 11),
                    Set.of()
            );

            List<RecurrenceServiceImpl.Occurrence> occurrences =
                    service.planOccurrences(LocalDate.of(2026, 5, 4), rule);

            assertThat(occurrences).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Validation: Phase 1 constraints")
    class ValidationTests {

        @Test
        @DisplayName("Reject when end_time <= start_time")
        void invalidTimeOrder_rejected() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.MO),
                    LocalTime.of(20, 0),
                    LocalTime.of(19, 0),
                    LocalDate.of(2026, 6, 1),
                    null
            );

            assertThatThrownBy(() -> service.planOccurrences(LocalDate.of(2026, 5, 1), rule))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("RECURRENCE_INVALID_TIME");
        }

        @Test
        @DisplayName("Reject when until < startDate")
        void untilBeforeStart_rejected() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.MO),
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    LocalDate.of(2026, 4, 1),
                    null
            );

            assertThatThrownBy(() -> service.planOccurrences(LocalDate.of(2026, 5, 1), rule))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("RECURRENCE_INVALID_RANGE");
        }
    }

    @Nested
    @DisplayName("buildSessions — converts occurrences into ClassSession entities")
    class BuildSessionsTests {

        @Test
        @DisplayName("Sessions numbered sequentially — verified count")
        void sessions_correctCountAndNumbers() {
            RecurrenceRuleDto rule = new RecurrenceRuleDto(
                    Freq.WEEKLY,
                    List.of(IcalDay.TU, IcalDay.TH),
                    LocalTime.of(19, 0),
                    LocalTime.of(20, 30),
                    LocalDate.of(2026, 5, 14), // Thu inclusive
                    null
            );

            List<ClassSession> sessions = service.buildSessions(
                    42L, LocalDate.of(2026, 5, 5), rule, 10
            );

            // TU 05-05, TH 05-07, TU 05-12, TH 05-14 = 4
            assertThat(sessions).hasSize(4);
            assertThat(sessions.get(0).getSessionNumber()).isEqualTo(11);
            assertThat(sessions.get(0).getClassId()).isEqualTo(42L);
            assertThat(sessions.get(0).getSessionDate()).isEqualTo(LocalDate.of(2026, 5, 5));
            assertThat(sessions.get(0).getStartTime()).isEqualTo(LocalTime.of(19, 0));
            assertThat(sessions.get(0).getEndTime()).isEqualTo(LocalTime.of(20, 30));
            assertThat(sessions.get(3).getSessionNumber()).isEqualTo(14);
        }
    }
}
