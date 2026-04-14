package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.SubjectSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @since 3.15.0 (GAP-054)
 */
@Repository
public interface SubjectSectionRepository extends JpaRepository<SubjectSection, Long> {

    List<SubjectSection> findByHomeroomClassIdAndDeletedFalse(Long homeroomClassId);

    Optional<SubjectSection> findByHomeroomClassIdAndCourseIdAndDeletedFalse(
            Long homeroomClassId, Long courseId);

    boolean existsByHomeroomClassIdAndCourseIdAndDeletedFalse(Long homeroomClassId, Long courseId);

    List<SubjectSection> findByTeacherIdAndDeletedFalse(Long teacherId);
}
