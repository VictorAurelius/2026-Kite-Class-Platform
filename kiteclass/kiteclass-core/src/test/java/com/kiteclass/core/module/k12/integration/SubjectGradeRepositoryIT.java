package com.kiteclass.core.module.k12.integration;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.academicyear.entity.Semester;
import com.kiteclass.core.module.academicyear.entity.SemesterType;
import com.kiteclass.core.module.academicyear.repository.AcademicYearRepository;
import com.kiteclass.core.module.academicyear.repository.SemesterRepository;
import com.kiteclass.core.module.k12.entity.HomeroomClass;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.entity.SubjectSection;
import com.kiteclass.core.module.k12.enums.SubjectGradeStatus;
import com.kiteclass.core.module.k12.enums.SubjectGradeType;
import com.kiteclass.core.module.k12.repository.HomeroomClassRepository;
import com.kiteclass.core.module.k12.repository.SubjectGradeRepository;
import com.kiteclass.core.module.k12.repository.SubjectSectionRepository;
import com.kiteclass.core.testutil.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-323c Phase 1C v1 — Verifies the new {@link SubjectGradeRepository}
 * status / type queries return only matching rows and that V55 backward-compat
 * defaults are honored on insert.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class SubjectGradeRepositoryIT extends IntegrationTestBase {

    @Autowired
    private SubjectGradeRepository repository;

    @Autowired
    private SubjectSectionRepository subjectSectionRepository;

    @Autowired
    private HomeroomClassRepository homeroomClassRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    void findBySubjectSectionIdAndStatusAndDeletedFalse_returnsOnlyMatchingStatus() {
        TestFixture fx = setupFixture();

        // 2 DRAFT, 1 REVIEWED, 1 PUBLISHED for same subject section
        save(fx, 100L, SubjectGradeType.TX, SubjectGradeStatus.DRAFT, "8.0");
        save(fx, 101L, SubjectGradeType.TX, SubjectGradeStatus.DRAFT, "7.5");
        save(fx, 102L, SubjectGradeType.GK, SubjectGradeStatus.REVIEWED, "9.0");
        save(fx, 103L, SubjectGradeType.CK, SubjectGradeStatus.PUBLISHED, "8.5");
        em.flush();
        em.clear();

        List<SubjectGrade> drafts = repository.findBySubjectSectionIdAndStatusAndDeletedFalse(
                fx.section.getId(), SubjectGradeStatus.DRAFT);
        List<SubjectGrade> reviewed = repository.findBySubjectSectionIdAndStatusAndDeletedFalse(
                fx.section.getId(), SubjectGradeStatus.REVIEWED);
        List<SubjectGrade> published = repository.findBySubjectSectionIdAndStatusAndDeletedFalse(
                fx.section.getId(), SubjectGradeStatus.PUBLISHED);

        assertThat(drafts).hasSize(2);
        assertThat(drafts).allMatch(g -> g.getStatus() == SubjectGradeStatus.DRAFT);
        assertThat(reviewed).hasSize(1);
        assertThat(reviewed.get(0).getStatus()).isEqualTo(SubjectGradeStatus.REVIEWED);
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getStatus()).isEqualTo(SubjectGradeStatus.PUBLISHED);
    }

    @Test
    void findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse_filtersByType() {
        TestFixture fx = setupFixture();

        save(fx, 200L, SubjectGradeType.TX, SubjectGradeStatus.DRAFT, "8.0");
        save(fx, 200L, SubjectGradeType.TX, SubjectGradeStatus.DRAFT, "7.0");
        save(fx, 200L, SubjectGradeType.GK, SubjectGradeStatus.DRAFT, "9.0");
        save(fx, 200L, SubjectGradeType.CK, SubjectGradeStatus.DRAFT, "8.5");
        em.flush();
        em.clear();

        List<SubjectGrade> tx = repository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        200L, fx.section.getId(), fx.semester.getId(), SubjectGradeType.TX);
        List<SubjectGrade> gk = repository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        200L, fx.section.getId(), fx.semester.getId(), SubjectGradeType.GK);
        List<SubjectGrade> ck = repository
                .findByStudentIdAndSubjectSectionIdAndSemesterIdAndTypeAndDeletedFalse(
                        200L, fx.section.getId(), fx.semester.getId(), SubjectGradeType.CK);

        assertThat(tx).hasSize(2);
        assertThat(tx).allMatch(g -> g.getType() == SubjectGradeType.TX);
        assertThat(gk).hasSize(1);
        assertThat(ck).hasSize(1);
    }

    @Test
    void v55DefaultsHonored_whenStatusAndTypeNotSetOnInsert() {
        TestFixture fx = setupFixture();

        // Insert without setting status/type/weight — DB defaults must apply
        SubjectGrade g = SubjectGrade.builder()
                .studentId(300L)
                .subjectSection(fx.section)
                .semester(fx.semester)
                .regularScore(new BigDecimal("8.0"))
                .midtermScore(new BigDecimal("7.5"))
                .finalScore(new BigDecimal("9.0"))
                .build();
        g.setInstanceId(fx.instanceId);
        g.setDeleted(false);
        repository.saveAndFlush(g);
        em.clear();

        SubjectGrade reloaded = repository.findById(g.getId()).orElseThrow();
        assertThat(reloaded.getStatus())
                .as("V55 default must be DRAFT for backward compat")
                .isEqualTo(SubjectGradeStatus.DRAFT);
        assertThat(reloaded.getType())
                .as("V55 default must be TX for backward compat")
                .isEqualTo(SubjectGradeType.TX);
        assertThat(reloaded.getWeight())
                .as("V55 default must be 1.0 for backward compat")
                .isEqualByComparingTo("1.0");
    }

    // ------------------------- helpers -------------------------

    private TestFixture setupFixture() {
        TestFixture fx = new TestFixture();
        fx.instanceId = UUID.randomUUID();

        AcademicYear year = AcademicYear.builder()
                .name("AY-323c-" + System.nanoTime())
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 6, 30))
                .status(AcademicYearStatus.CURRENT)
                .build();
        year.setInstanceId(fx.instanceId);
        year.setDeleted(false);
        fx.year = academicYearRepository.save(year);

        Semester sem = Semester.builder()
                .academicYear(fx.year)
                .type(SemesterType.HK1)
                .name("HK1")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2027, 1, 31))
                .build();
        sem.setInstanceId(fx.instanceId);
        sem.setDeleted(false);
        fx.semester = semesterRepository.save(sem);

        HomeroomClass hrc = HomeroomClass.builder()
                .academicYear(fx.year)
                .grade("10")
                .section("A1")
                .capacity(40)
                .currentEnrolled(0)
                .build();
        hrc.setInstanceId(fx.instanceId);
        hrc.setDeleted(false);
        fx.homeroom = homeroomClassRepository.save(hrc);

        SubjectSection section = SubjectSection.builder()
                .homeroomClass(fx.homeroom)
                .courseId(1001L)
                .weeklyHours(4)
                .build();
        section.setInstanceId(fx.instanceId);
        section.setDeleted(false);
        fx.section = subjectSectionRepository.save(section);

        return fx;
    }

    private void save(TestFixture fx, Long studentId, SubjectGradeType type,
                      SubjectGradeStatus status, String score) {
        SubjectGrade g = SubjectGrade.builder()
                .studentId(studentId)
                .subjectSection(fx.section)
                .semester(fx.semester)
                .type(type)
                .status(status)
                .weight(switch (type) {
                    case TX -> new BigDecimal("1.0");
                    case GK -> new BigDecimal("2.0");
                    case CK -> new BigDecimal("3.0");
                })
                .build();
        switch (type) {
            case TX -> g.setRegularScore(new BigDecimal(score));
            case GK -> g.setMidtermScore(new BigDecimal(score));
            case CK -> g.setFinalScore(new BigDecimal(score));
        }
        g.setInstanceId(fx.instanceId);
        g.setDeleted(false);
        repository.save(g);
    }

    private static class TestFixture {
        UUID instanceId;
        AcademicYear year;
        Semester semester;
        HomeroomClass homeroom;
        SubjectSection section;
    }
}
