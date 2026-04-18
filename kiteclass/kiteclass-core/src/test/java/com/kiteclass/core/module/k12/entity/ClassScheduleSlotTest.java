package com.kiteclass.core.module.k12.entity;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests cho ClassScheduleSlot entity behavior (GAP-099 Phase 1).
 *
 * <p>Scope: isActiveOn() + getDurationMinutes() logic. DB-level constraints (time order,
 * date order, day_of_week enum) tested trong future integration tests.
 *
 * @since GAP-099 Phase 1
 */
class ClassScheduleSlotTest {

    @Test
    void isActiveOn_returnsTrueForDateWithinRange() {
        ClassScheduleSlot slot = ClassScheduleSlot.builder()
                .subjectSectionId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 30))
                .effectiveFrom(LocalDate.of(2026, 9, 1))
                .effectiveUntil(LocalDate.of(2027, 5, 31))
                .build();

        assertThat(slot.isActiveOn(LocalDate.of(2026, 9, 1))).isTrue();
        assertThat(slot.isActiveOn(LocalDate.of(2027, 1, 15))).isTrue();
        assertThat(slot.isActiveOn(LocalDate.of(2027, 5, 31))).isTrue();
    }

    @Test
    void isActiveOn_returnsFalseBeforeEffectiveFrom() {
        ClassScheduleSlot slot = ClassScheduleSlot.builder()
                .subjectSectionId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 30))
                .effectiveFrom(LocalDate.of(2026, 9, 1))
                .build();

        assertThat(slot.isActiveOn(LocalDate.of(2026, 8, 31))).isFalse();
    }

    @Test
    void isActiveOn_returnsFalseAfterEffectiveUntil() {
        ClassScheduleSlot slot = ClassScheduleSlot.builder()
                .subjectSectionId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 30))
                .effectiveFrom(LocalDate.of(2026, 9, 1))
                .effectiveUntil(LocalDate.of(2027, 5, 31))
                .build();

        assertThat(slot.isActiveOn(LocalDate.of(2027, 6, 1))).isFalse();
    }

    @Test
    void isActiveOn_nullEffectiveUntilMeansIndefinite() {
        ClassScheduleSlot slot = ClassScheduleSlot.builder()
                .subjectSectionId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 30))
                .effectiveFrom(LocalDate.of(2026, 9, 1))
                .effectiveUntil(null)
                .build();

        assertThat(slot.isActiveOn(LocalDate.of(2030, 1, 1))).isTrue();
        assertThat(slot.isActiveOn(LocalDate.of(2040, 12, 31))).isTrue();
    }

    @Test
    void getDurationMinutes_computesCorrectly() {
        ClassScheduleSlot slot = ClassScheduleSlot.builder()
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 30))
                .build();

        assertThat(slot.getDurationMinutes()).isEqualTo(90);
    }

    @Test
    void getDurationMinutes_handlesExactHour() {
        ClassScheduleSlot slot = ClassScheduleSlot.builder()
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .build();

        assertThat(slot.getDurationMinutes()).isEqualTo(60);
    }
}
