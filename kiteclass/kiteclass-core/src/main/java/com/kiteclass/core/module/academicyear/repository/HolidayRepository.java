package com.kiteclass.core.module.academicyear.repository;

import com.kiteclass.core.module.academicyear.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Holiday entity.
 *
 * @since 3.15.0 (GAP-053)
 */
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByAcademicYearIdAndDeletedFalseOrderByStartDate(Long academicYearId);

    List<Holiday> findByAcademicYearIdAndStartDateBetweenAndDeletedFalse(
            Long academicYearId, LocalDate from, LocalDate to
    );
}
