package com.kiteclass.core.module.k12.repository;

import com.kiteclass.core.module.k12.entity.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @since 3.15.0 (GAP-054)
 */
@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

    Optional<Curriculum> findByGradeAndDeletedFalse(String grade);

    boolean existsByGradeAndDeletedFalse(String grade);
}
