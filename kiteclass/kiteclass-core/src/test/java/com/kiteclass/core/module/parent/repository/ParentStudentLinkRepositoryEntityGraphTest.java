package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.common.constant.ParentLinkType;
import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import com.kiteclass.core.testutil.IntegrationTestBase;
import com.kiteclass.core.testutil.StudentTestDataBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-134 (Wave 9.5) — Verifies that the symmetric
 * {@link ParentStudentLinkRepository#findByStudentIdWithParent} method prefetches
 * the {@code parent} side of the join in a single SELECT, mirroring the existing
 * {@code findByParentIdWithStudent} which was added prior to this expansion.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class ParentStudentLinkRepositoryEntityGraphTest extends IntegrationTestBase {

    @Autowired
    private ParentStudentLinkRepository linkRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findByStudentIdWithParent_runsSingleSelect_whenParentAccessed() {
        Parent parent = parentRepository.save(Parent.builder()
                .email("gap134-link-parent@example.com")
                .fullName("GAP134 Parent")
                .relationship(ParentRelationship.GUARDIAN)
                .status(ParentStatus.ACTIVE)
                .build());

        Student student = StudentTestDataBuilder.createDefaultStudent();
        student.setId(null);
        student.setEmail("gap134-link-student@example.com");
        Student savedStudent = studentRepository.save(student);

        ParentStudentLink link = ParentStudentLink.builder()
                .parent(parent)
                .student(savedStudent)
                .linkType(ParentLinkType.PRIMARY)
                .build();
        link.setDeleted(false);
        linkRepository.save(link);

        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        List<ParentStudentLink> results =
                linkRepository.findByStudentIdWithParent(savedStudent.getId());
        assertThat(results).hasSize(1);
        ParentStudentLink loaded = results.get(0);
        assertThat(Hibernate.isInitialized(loaded.getParent()))
                .as("parent must be initialised via JOIN FETCH")
                .isTrue();
        assertThat(loaded.getParent().getEmail())
                .isEqualTo("gap134-link-parent@example.com");

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("findByStudentIdWithParent must emit a single SELECT — got %d",
                        selectCount)
                .isEqualTo(1L);
    }
}
