package com.kiteclass.core.module.k12.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ClassScheduleSlot — structured weekly schedule slot for a SubjectSection.
 *
 * <p>Phase 1 of GAP-099: replaces free-form {@code SubjectSection.schedule} text with typed
 * slots supporting query-ability, conflict detection, and iCal export (future phases).
 *
 * <p>Phase 1 scope (this entity): data model + migration + repository.
 * Phase 2 (future wave): CRUD API, conflict detection, attendance session generator, iCal feed.
 * Phase 3 (future wave): weekly grid UI, data conversion from free-form.
 *
 * <p>Business rules:
 * <ul>
 *   <li>BR-CSS-001: Belongs to exactly 1 SubjectSection</li>
 *   <li>BR-CSS-002: end_time must be after start_time (DB CHECK)</li>
 *   <li>BR-CSS-003: effective_until NULL = indefinite; if set, must be >= effective_from (DB CHECK)</li>
 *   <li>BR-CSS-004: day_of_week restricted to java.time.DayOfWeek values (DB CHECK)</li>
 * </ul>
 *
 * @since GAP-099 Phase 1
 */
@Entity
@Table(
        name = "class_schedule_slots",
        indexes = {
                @Index(name = "idx_schedule_slot_section_day", columnList = "subject_section_id,day_of_week,deleted"),
                @Index(name = "idx_schedule_slot_instance", columnList = "instance_id"),
                @Index(name = "idx_schedule_slot_effective", columnList = "effective_from,effective_until")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassScheduleSlot extends BaseEntity {

    @NotNull
    @Column(name = "subject_section_id", nullable = false)
    private Long subjectSectionId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10, nullable = false)
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * NULL = slot is active indefinitely.
     * Set to a date when schedule changes mid-year (create new slot with effective_from = next day).
     */
    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    /**
     * Free-text note for exceptions (e.g., "Skip week 5 for exam"). Kept free-form intentionally;
     * structured exception handling deferred to Phase 2.
     */
    @Column(name = "recurrence_note", length = 500)
    private String recurrenceNote;

    /**
     * Check if this slot is active on a given date.
     *
     * @param date the date to check
     * @return true if effective_from <= date AND (effective_until IS NULL OR date <= effective_until)
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveUntil == null || !date.isAfter(effectiveUntil);
    }

    /**
     * Duration of the slot in minutes.
     *
     * @return minutes between startTime and endTime
     */
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
}
