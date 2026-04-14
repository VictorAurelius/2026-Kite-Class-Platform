package com.kiteclass.core.module.k12;

import com.kiteclass.core.module.k12.entity.HomeroomClass;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for K-12 entity domain methods.
 */
class K12EntityTest {

    @Test
    void homeroomClass_getFullName_combines_grade_and_section() {
        HomeroomClass hrc = HomeroomClass.builder()
                .grade("10")
                .section("A1")
                .build();

        assertThat(hrc.getFullName()).isEqualTo("10A1");
    }

    @Test
    void homeroomClass_hasCapacity_when_not_full() {
        HomeroomClass hrc = HomeroomClass.builder()
                .capacity(40)
                .currentEnrolled(20)
                .build();

        assertThat(hrc.hasCapacity()).isTrue();
    }

    @Test
    void homeroomClass_no_capacity_when_full() {
        HomeroomClass hrc = HomeroomClass.builder()
                .capacity(40)
                .currentEnrolled(40)
                .build();

        assertThat(hrc.hasCapacity()).isFalse();
    }

    @Test
    void subjectGrade_computeAverage_weighted_formula() {
        SubjectGrade grade = SubjectGrade.builder()
                .regularScore(new BigDecimal("8.0"))
                .midtermScore(new BigDecimal("7.5"))
                .finalScore(new BigDecimal("9.0"))
                .build();

        grade.computeAverage();

        // (8.0*1 + 7.5*2 + 9.0*3) / 6 = (8 + 15 + 27) / 6 = 50 / 6 ≈ 8.33
        assertThat(grade.getAverage()).isEqualByComparingTo(new BigDecimal("8.33"));
        assertThat(grade.getLetterGrade()).isEqualTo("Giỏi");  // >= 8
    }

    @Test
    void subjectGrade_letterGrade_Kha() {
        SubjectGrade grade = SubjectGrade.builder()
                .regularScore(new BigDecimal("7.0"))
                .midtermScore(new BigDecimal("6.5"))
                .finalScore(new BigDecimal("7.0"))
                .build();
        grade.computeAverage();

        // (7 + 13 + 21) / 6 = 6.83
        assertThat(grade.getAverage()).isEqualByComparingTo(new BigDecimal("6.83"));
        assertThat(grade.getLetterGrade()).isEqualTo("Khá");  // >= 6.5
    }

    @Test
    void subjectGrade_letterGrade_TB() {
        SubjectGrade grade = SubjectGrade.builder()
                .regularScore(new BigDecimal("5.0"))
                .midtermScore(new BigDecimal("5.5"))
                .finalScore(new BigDecimal("5.0"))
                .build();
        grade.computeAverage();

        // (5 + 11 + 15) / 6 = 5.17
        assertThat(grade.getLetterGrade()).isEqualTo("Trung bình");
    }

    @Test
    void subjectGrade_letterGrade_Yeu() {
        SubjectGrade grade = SubjectGrade.builder()
                .regularScore(new BigDecimal("4.0"))
                .midtermScore(new BigDecimal("3.5"))
                .finalScore(new BigDecimal("4.0"))
                .build();
        grade.computeAverage();

        // (4 + 7 + 12) / 6 = 3.83
        assertThat(grade.getLetterGrade()).isEqualTo("Yếu");
    }

    @Test
    void subjectGrade_null_scores_result_in_null_average() {
        SubjectGrade grade = SubjectGrade.builder()
                .regularScore(new BigDecimal("8.0"))
                // midterm + final missing
                .build();

        grade.computeAverage();

        assertThat(grade.getAverage()).isNull();
        assertThat(grade.getLetterGrade()).isNull();
    }

    @Test
    void deriveLetterGrade_boundary_cases() {
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("10.0"))).isEqualTo("Giỏi");
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("8.0"))).isEqualTo("Giỏi");
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("7.99"))).isEqualTo("Khá");
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("6.5"))).isEqualTo("Khá");
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("6.49"))).isEqualTo("Trung bình");
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("5.0"))).isEqualTo("Trung bình");
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("4.99"))).isEqualTo("Yếu");
        assertThat(SubjectGrade.deriveLetterGrade(new BigDecimal("0.0"))).isEqualTo("Yếu");
        assertThat(SubjectGrade.deriveLetterGrade(null)).isNull();
    }
}
