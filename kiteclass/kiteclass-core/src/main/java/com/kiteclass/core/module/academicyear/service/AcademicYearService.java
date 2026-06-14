package com.kiteclass.core.module.academicyear.service;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.academicyear.entity.Holiday;
import com.kiteclass.core.module.academicyear.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * AcademicYearService — Aggregate Root service per DDD (ADR-002).
 *
 * <p>Business operations:
 * <ul>
 *   <li>Create academic year with auto-seeded VN holidays</li>
 *   <li>Transition status (UPCOMING → CURRENT → COMPLETED)</li>
 *   <li>Enforce "only 1 CURRENT at a time" (BR-ACYR-003)</li>
 *   <li>Find current year for tenant</li>
 * </ul>
 *
 * <p>Multi-tenant isolation automatic via Hibernate filter.
 *
 * @since 3.15.0 (GAP-053)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final VnHolidayProvider vnHolidayProvider;

    /**
     * Create new academic year + auto-seed VN national holidays.
     *
     * @param name       e.g., "2026-2027"
     * @param startDate  typically early September
     * @param endDate    typically mid-June
     * @return created year with seeded holidays
     * @throws IllegalArgumentException if name exists or dates invalid
     */
    @Transactional
    public AcademicYear createAcademicYear(String name, LocalDate startDate, LocalDate endDate) {
        if (academicYearRepository.existsByNameAndDeletedFalse(name)) {
            throw new IllegalArgumentException("Academic year with name '" + name + "' already exists");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        AcademicYear academicYear = AcademicYear.builder()
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .status(AcademicYearStatus.UPCOMING)
                .build();

        // Auto-seed VN national holidays
        List<Holiday> holidays = vnHolidayProvider.generateForAcademicYear(academicYear);
        academicYear.getHolidays().addAll(holidays);

        AcademicYear saved = academicYearRepository.save(academicYear);
        log.info("Created academic year '{}' with {} VN holidays", name, holidays.size());
        return saved;
    }

    /**
     * Transition academic year to CURRENT status.
     *
     * <p>BR-ACYR-003: Only 1 CURRENT at a time.
     * Auto-transitions previous CURRENT → COMPLETED.
     */
    @Transactional
    public AcademicYear setCurrent(Long academicYearId) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found"));

        // Find existing CURRENT (if any) and demote to COMPLETED
        academicYearRepository.findFirstByStatusAndDeletedFalse(AcademicYearStatus.CURRENT)
                .ifPresent(existing -> {
                    log.info("Transitioning existing CURRENT year '{}' to COMPLETED", existing.getName());
                    existing.setStatus(AcademicYearStatus.COMPLETED);
                    academicYearRepository.save(existing);
                });

        year.setStatus(AcademicYearStatus.CURRENT);
        return academicYearRepository.save(year);
    }

    /**
     * Get the CURRENT academic year for tenant.
     */
    public Optional<AcademicYear> getCurrent() {
        return academicYearRepository.findFirstByStatusAndDeletedFalse(AcademicYearStatus.CURRENT);
    }

    /**
     * Get academic year by ID.
     */
    public Optional<AcademicYear> getById(Long id) {
        return academicYearRepository.findById(id);
    }

    /** GAP-1362: defensive hard cap — academic years accumulate ~1/year but never load unbounded. */
    static final int LIST_ALL_MAX = 200;

    /**
     * List recent academic years (newest first), bounded to {@link #LIST_ALL_MAX}.
     *
     * <p>GAP-1362: previously {@code findAll()} materialised every row unbounded. The set is
     * small (one per academic year) but bounding it removes the unbounded-pattern cliff;
     * callers needing full pagination should add a {@code Pageable} overload.
     */
    public List<AcademicYear> listAll() {
        return academicYearRepository
                .findAll(PageRequest.of(0, LIST_ALL_MAX, Sort.by(Sort.Direction.DESC, "startDate")))
                .getContent();
    }

    /**
     * Check if given date falls on a holiday within current year.
     *
     * <p>GAP-134: uses {@link AcademicYearRepository#findFirstByStatusWithHolidays}
     * so that the year + its holidays arrive in a single SELECT — the plain
     * {@code getCurrent()} variant triggers an extra query the first time the
     * lazy {@code holidays} collection is touched, which is every call.
     */
    public boolean isHoliday(LocalDate date) {
        return academicYearRepository
                .findFirstByStatusWithHolidays(AcademicYearStatus.CURRENT)
                .map(year -> year.getHolidays().stream()
                        .anyMatch(h -> h.contains(date)))
                .orElse(false);
    }
}
