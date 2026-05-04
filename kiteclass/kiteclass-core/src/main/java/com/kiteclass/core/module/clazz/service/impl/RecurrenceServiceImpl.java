package com.kiteclass.core.module.clazz.service.impl;

import com.kiteclass.core.common.constant.SessionStatus;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto.IcalDay;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.service.RecurrenceService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link RecurrenceService} (GAP-290 Wave 18a).
 *
 * <p>Strategy: lean handwritten WEEKLY-only generator — no external library.
 * Trade-off vs. ical4j: scope is narrow (WEEKLY + by_day + until + exclude_dates,
 * no COUNT/INTERVAL/BYMONTHDAY), so a focused {@link DayOfWeek}-based loop is
 * simpler, has zero new transitive dependencies, avoids the GAP-203 CVE-pin
 * burden, and is trivially auditable. If future phases need MONTHLY/YEARLY or
 * BYSETPOS rules, swap implementation behind this interface (Strategy pattern
 * per design-patterns.md §1.1) — caller code stays stable.
 *
 * <p>Pure recurrence math — no DB, no transactions. Caller (ClassService) owns
 * persistence + state-machine for edit.
 *
 * @since GAP-290 (Wave 18a)
 */
@Service
public class RecurrenceServiceImpl implements RecurrenceService {

    @Override
    public List<Occurrence> planOccurrences(LocalDate start, RecurrenceRuleDto rule) {
        validate(start, rule);

        Set<DayOfWeek> targetDays = EnumSet.noneOf(DayOfWeek.class);
        for (IcalDay d : rule.byDay()) {
            targetDays.add(d.toDayOfWeek());
        }

        Set<LocalDate> excludes = rule.excludeDates() == null
                ? Set.of()
                : rule.excludeDates();

        List<Occurrence> result = new ArrayList<>();
        LocalDate current = start;
        LocalDate until = rule.until();

        // Defensive cap to prevent runaway loops if validation upstream is bypassed.
        // 10 years × 7 days × ~52 weeks = ~3650 iterations max; well under any sane class span.
        long safetyCap = java.time.temporal.ChronoUnit.DAYS.between(start, until) + 1;
        if (safetyCap > 3700) {
            throw new ValidationException("RECURRENCE_RANGE_TOO_LARGE", new Object[]{safetyCap});
        }

        while (!current.isAfter(until)) {
            if (targetDays.contains(current.getDayOfWeek()) && !excludes.contains(current)) {
                result.add(new Occurrence(current, rule.startTime(), rule.endTime()));
            }
            current = current.plusDays(1);
        }
        return result;
    }

    @Override
    public List<ClassSession> buildSessions(Long classId, LocalDate start,
                                            RecurrenceRuleDto rule, int sessionNumberOffset) {
        List<Occurrence> occurrences = planOccurrences(start, rule);
        List<ClassSession> sessions = new ArrayList<>(occurrences.size());
        int n = sessionNumberOffset;
        for (Occurrence occ : occurrences) {
            n++;
            sessions.add(ClassSession.builder()
                    .classId(classId)
                    .sessionNumber(n)
                    .sessionDate(occ.date())
                    .startTime(occ.startTime())
                    .endTime(occ.endTime())
                    .status(SessionStatus.SCHEDULED)
                    .attendanceTaken(false)
                    .build());
        }
        return sessions;
    }

    private static void validate(LocalDate start, RecurrenceRuleDto rule) {
        if (rule == null) {
            throw new ValidationException("RECURRENCE_REQUIRED", new Object[0]);
        }
        if (rule.endTime() == null || rule.startTime() == null
                || !rule.endTime().isAfter(rule.startTime())) {
            throw new ValidationException("RECURRENCE_INVALID_TIME", new Object[0]);
        }
        if (rule.until() == null || rule.until().isBefore(start)) {
            throw new ValidationException("RECURRENCE_INVALID_RANGE", new Object[0]);
        }
        if (rule.byDay() == null || rule.byDay().isEmpty()) {
            throw new ValidationException("RECURRENCE_NO_DAYS", new Object[0]);
        }
    }

    /**
     * A planned recurrence occurrence — date + time-of-day pair. Equivalent to
     * a future {@link ClassSession}'s scheduling fields, without the entity overhead.
     *
     * @param date      calendar date the session is scheduled for
     * @param startTime start of session
     * @param endTime   end of session
     */
    public record Occurrence(
            LocalDate date,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {
    }
}
