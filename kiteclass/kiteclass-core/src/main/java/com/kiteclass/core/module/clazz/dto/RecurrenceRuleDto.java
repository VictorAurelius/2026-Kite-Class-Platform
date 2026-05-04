package com.kiteclass.core.module.clazz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * RecurrenceRule DTO — RFC 5545 RRULE subset for recurring class sessions (GAP-290).
 *
 * <p>Persisted as JSONB on {@code classes.recurrence_rule} column.
 * Schema (v1, snake_case for storage compatibility):
 * <pre>
 * {
 *   "freq": "WEEKLY",
 *   "by_day": ["TU", "TH"],
 *   "start_time": "19:00",
 *   "end_time": "20:30",
 *   "until": "2026-08-01",
 *   "exclude_dates": ["2026-06-15"]
 * }
 * </pre>
 *
 * <p>Phase 1 scope (Wave 18a / GAP-290): only {@link Freq#WEEKLY}. {@code DAILY},
 * {@code MONTHLY}, {@code YEARLY} reserved for future phases.
 *
 * <p>Business rules:
 * <ul>
 *   <li>BR-CLASS-009: {@code freq=WEEKLY} only (Phase 1)</li>
 *   <li>BR-CLASS-009: {@code by_day} non-empty, values from {@link IcalDay} 2-letter codes</li>
 *   <li>BR-CLASS-009: {@code end_time} strictly after {@code start_time}</li>
 *   <li>BR-CLASS-009: {@code until} required (no infinite recurrence in Phase 1)</li>
 *   <li>BR-CLASS-009: {@code exclude_dates} optional, deduped/sorted by service</li>
 * </ul>
 *
 * @param freq         recurrence frequency (WEEKLY only in Phase 1)
 * @param byDay        2-letter iCal day codes (MO, TU, WE, TH, FR, SA, SU)
 * @param startTime    session start time (Asia/Ho_Chi_Minh interpreted by FE)
 * @param endTime      session end time (must be after startTime)
 * @param until        last calendar date to consider for generation (inclusive)
 * @param excludeDates optional dates skipped (holidays, breaks); may be null/empty
 * @since GAP-290 (Wave 18a)
 */
public record RecurrenceRuleDto(

        @NotNull(message = "Tần suất lặp lại không được để trống")
        Freq freq,

        @NotEmpty(message = "Phải chọn ít nhất 1 ngày trong tuần")
        @JsonProperty("by_day")
        List<IcalDay> byDay,

        @NotNull(message = "Giờ bắt đầu không được để trống")
        @JsonProperty("start_time")
        LocalTime startTime,

        @NotNull(message = "Giờ kết thúc không được để trống")
        @JsonProperty("end_time")
        LocalTime endTime,

        @NotNull(message = "Ngày kết thúc lặp không được để trống")
        LocalDate until,

        @JsonProperty("exclude_dates")
        Set<LocalDate> excludeDates
) {

    /**
     * Recurrence frequency (Phase 1: WEEKLY only).
     */
    public enum Freq {
        WEEKLY
    }

    /**
     * iCal-style 2-letter day codes per RFC 5545 §3.3.10.
     */
    public enum IcalDay {
        MO(DayOfWeek.MONDAY),
        TU(DayOfWeek.TUESDAY),
        WE(DayOfWeek.WEDNESDAY),
        TH(DayOfWeek.THURSDAY),
        FR(DayOfWeek.FRIDAY),
        SA(DayOfWeek.SATURDAY),
        SU(DayOfWeek.SUNDAY);

        private final DayOfWeek dayOfWeek;

        IcalDay(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        /**
         * @return matching java.time DayOfWeek
         */
        public DayOfWeek toDayOfWeek() {
            return dayOfWeek;
        }

        /**
         * Maps a {@link DayOfWeek} to its iCal code.
         *
         * @param day java.time DayOfWeek
         * @return matching IcalDay
         */
        public static IcalDay fromDayOfWeek(DayOfWeek day) {
            for (IcalDay d : values()) {
                if (d.dayOfWeek == day) {
                    return d;
                }
            }
            throw new IllegalArgumentException("Unknown day: " + day);
        }
    }
}
