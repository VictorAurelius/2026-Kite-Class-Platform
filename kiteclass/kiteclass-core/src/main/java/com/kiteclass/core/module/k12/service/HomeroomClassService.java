package com.kiteclass.core.module.k12.service;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.repository.AcademicYearRepository;
import com.kiteclass.core.module.k12.entity.HomeroomClass;
import com.kiteclass.core.module.k12.repository.HomeroomClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * HomeroomClassService — manages lớp chính cho K-12 schools.
 *
 * <p>Aggregate root per ADR-001.
 *
 * @since 3.15.0 (GAP-054)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HomeroomClassService {

    private final HomeroomClassRepository homeroomClassRepository;
    private final AcademicYearRepository academicYearRepository;

    /**
     * Create homeroom class within academic year.
     *
     * @throws IllegalArgumentException if (year, grade, section) already exists
     *                                  or academic year not found
     */
    @Transactional
    public HomeroomClass create(Long academicYearId, String grade, String section,
                                 Integer capacity, Long homeroomTeacherId) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found"));

        if (homeroomClassRepository.existsByAcademicYearIdAndGradeAndSectionAndDeletedFalse(
                academicYearId, grade, section)) {
            throw new IllegalArgumentException(
                    "Homeroom class " + grade + section + " already exists in " + year.getName());
        }

        HomeroomClass hrc = HomeroomClass.builder()
                .academicYear(year)
                .grade(grade)
                .section(section)
                .capacity(capacity != null ? capacity : 40)
                .homeroomTeacherId(homeroomTeacherId)
                .currentEnrolled(0)
                .build();

        HomeroomClass saved = homeroomClassRepository.save(hrc);
        log.info("Created homeroom class {} in year {}", saved.getFullName(), year.getName());
        return saved;
    }

    /**
     * List all homeroom classes in academic year.
     */
    public List<HomeroomClass> listByAcademicYear(Long academicYearId) {
        return homeroomClassRepository.findByAcademicYearIdAndDeletedFalse(academicYearId);
    }

    /**
     * Get by ID.
     */
    public Optional<HomeroomClass> getById(Long id) {
        return homeroomClassRepository.findById(id);
    }

    /**
     * Assign or change homeroom teacher (GVCN).
     */
    @Transactional
    public HomeroomClass assignHomeroomTeacher(Long homeroomClassId, Long teacherId) {
        HomeroomClass hrc = homeroomClassRepository.findById(homeroomClassId)
                .orElseThrow(() -> new IllegalArgumentException("Homeroom class not found"));
        hrc.setHomeroomTeacherId(teacherId);
        return homeroomClassRepository.save(hrc);
    }

    /**
     * Increment enrolled count when student added.
     * @throws IllegalStateException if capacity full
     */
    @Transactional
    public HomeroomClass enrollStudent(Long homeroomClassId) {
        HomeroomClass hrc = homeroomClassRepository.findById(homeroomClassId)
                .orElseThrow(() -> new IllegalArgumentException("Homeroom class not found"));
        if (!hrc.hasCapacity()) {
            throw new IllegalStateException("Homeroom class " + hrc.getFullName() + " is at full capacity");
        }
        hrc.setCurrentEnrolled(hrc.getCurrentEnrolled() + 1);
        return homeroomClassRepository.save(hrc);
    }

    /**
     * Decrement enrolled count when student removed.
     */
    @Transactional
    public HomeroomClass unenrollStudent(Long homeroomClassId) {
        HomeroomClass hrc = homeroomClassRepository.findById(homeroomClassId)
                .orElseThrow(() -> new IllegalArgumentException("Homeroom class not found"));
        if (hrc.getCurrentEnrolled() > 0) {
            hrc.setCurrentEnrolled(hrc.getCurrentEnrolled() - 1);
        }
        return homeroomClassRepository.save(hrc);
    }
}
