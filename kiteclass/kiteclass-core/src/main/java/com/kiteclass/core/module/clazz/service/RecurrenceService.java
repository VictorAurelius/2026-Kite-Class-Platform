package com.kiteclass.core.module.clazz.service;

import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto;
import com.kiteclass.core.module.clazz.entity.ClassSession;
import com.kiteclass.core.module.clazz.service.impl.RecurrenceServiceImpl.Occurrence;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for generating {@link ClassSession} occurrences from an RFC 5545
 * RRULE-subset {@link RecurrenceRuleDto}.
 *
 * <p>GAP-290 (Wave 18a) — Phase 1 supports {@code freq=WEEKLY} with multi-day
 * {@code by_day}, {@code start_time}/{@code end_time}, mandatory {@code until}, and
 * optional {@code exclude_dates}. Other frequencies (DAILY/MONTHLY/YEARLY) and
 * COUNT/INTERVAL/BYMONTHDAY are deferred to a future phase.
 *
 * <p>Implementation is intentionally pure — no DB access, no Spring beans —
 * to keep the recurrence math testable as a unit.
 *
 * <p>State-machine for edit (per BR-CLASS-009):
 * <ul>
 *   <li>Future sessions (date >= today AND attendanceTaken == false) → may be regenerated</li>
 *   <li>Past or attended sessions (date &lt; today OR attendanceTaken == true) → preserved</li>
 * </ul>
 *
 * @see RecurrenceRuleDto
 * @since GAP-290 (Wave 18a)
 */
public interface RecurrenceService {

    /**
     * Plans the list of occurrences (date-only) that match the rule between
     * {@code start} (inclusive) and {@code rule.until()} (inclusive).
     *
     * @param start  first calendar date considered (typically Class.startDate)
     * @param rule   recurrence rule
     * @return ordered list of occurrences (ascending date), excluding any in
     *         {@code rule.excludeDates()}
     * @throws com.kiteclass.core.common.exception.ValidationException
     *         when rule fails Phase 1 validation (end &lt;= start time, until &lt; start)
     */
    List<Occurrence> planOccurrences(LocalDate start, RecurrenceRuleDto rule);

    /**
     * Builds {@link ClassSession} entities for {@link #planOccurrences} results.
     *
     * @param classId             owner class
     * @param start               first date considered
     * @param rule                recurrence rule
     * @param sessionNumberOffset existing max session number; first new session = offset+1
     * @return ordered list of unsaved {@code ClassSession} (caller persists)
     */
    List<ClassSession> buildSessions(Long classId, LocalDate start,
                                     RecurrenceRuleDto rule, int sessionNumberOffset);
}
