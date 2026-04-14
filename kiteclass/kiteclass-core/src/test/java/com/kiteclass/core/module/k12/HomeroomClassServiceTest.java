package com.kiteclass.core.module.k12;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.repository.AcademicYearRepository;
import com.kiteclass.core.module.k12.entity.HomeroomClass;
import com.kiteclass.core.module.k12.repository.HomeroomClassRepository;
import com.kiteclass.core.module.k12.service.HomeroomClassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeroomClassServiceTest {

    @Mock
    private HomeroomClassRepository homeroomClassRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @InjectMocks
    private HomeroomClassService service;

    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        academicYear = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .build();
    }

    @Test
    void create_succeeds_with_valid_input() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(homeroomClassRepository.existsByAcademicYearIdAndGradeAndSectionAndDeletedFalse(1L, "10", "A1"))
                .thenReturn(false);
        when(homeroomClassRepository.save(any(HomeroomClass.class))).thenAnswer(inv -> inv.getArgument(0));

        HomeroomClass result = service.create(1L, "10", "A1", 40, 5L);

        assertThat(result.getFullName()).isEqualTo("10A1");
        assertThat(result.getCapacity()).isEqualTo(40);
        assertThat(result.getHomeroomTeacherId()).isEqualTo(5L);
        assertThat(result.getCurrentEnrolled()).isZero();
    }

    @Test
    void create_uses_default_capacity_when_null() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(homeroomClassRepository.existsByAcademicYearIdAndGradeAndSectionAndDeletedFalse(any(), any(), any()))
                .thenReturn(false);
        when(homeroomClassRepository.save(any(HomeroomClass.class))).thenAnswer(inv -> inv.getArgument(0));

        HomeroomClass result = service.create(1L, "10", "A1", null, null);

        assertThat(result.getCapacity()).isEqualTo(40);
    }

    @Test
    void create_throws_when_academic_year_not_found() {
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(999L, "10", "A1", 40, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void create_throws_when_duplicate() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(homeroomClassRepository.existsByAcademicYearIdAndGradeAndSectionAndDeletedFalse(1L, "10", "A1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, "10", "A1", 40, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void enrollStudent_increments_count() {
        HomeroomClass hrc = HomeroomClass.builder()
                .grade("10").section("A1")
                .capacity(40).currentEnrolled(20)
                .build();
        when(homeroomClassRepository.findById(1L)).thenReturn(Optional.of(hrc));
        when(homeroomClassRepository.save(any(HomeroomClass.class))).thenAnswer(inv -> inv.getArgument(0));

        HomeroomClass result = service.enrollStudent(1L);

        assertThat(result.getCurrentEnrolled()).isEqualTo(21);
    }

    @Test
    void enrollStudent_throws_when_full() {
        HomeroomClass hrc = HomeroomClass.builder()
                .grade("10").section("A1")
                .capacity(40).currentEnrolled(40)
                .build();
        when(homeroomClassRepository.findById(1L)).thenReturn(Optional.of(hrc));

        assertThatThrownBy(() -> service.enrollStudent(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full capacity");
    }

    @Test
    void unenrollStudent_decrements_count() {
        HomeroomClass hrc = HomeroomClass.builder()
                .grade("10").section("A1")
                .capacity(40).currentEnrolled(20)
                .build();
        when(homeroomClassRepository.findById(1L)).thenReturn(Optional.of(hrc));
        when(homeroomClassRepository.save(any(HomeroomClass.class))).thenAnswer(inv -> inv.getArgument(0));

        HomeroomClass result = service.unenrollStudent(1L);

        assertThat(result.getCurrentEnrolled()).isEqualTo(19);
    }

    @Test
    void unenrollStudent_stays_zero_when_already_zero() {
        HomeroomClass hrc = HomeroomClass.builder()
                .grade("10").section("A1")
                .capacity(40).currentEnrolled(0)
                .build();
        when(homeroomClassRepository.findById(1L)).thenReturn(Optional.of(hrc));
        when(homeroomClassRepository.save(any(HomeroomClass.class))).thenAnswer(inv -> inv.getArgument(0));

        HomeroomClass result = service.unenrollStudent(1L);

        assertThat(result.getCurrentEnrolled()).isZero();
    }

    @Test
    void assignHomeroomTeacher_updates_teacher_id() {
        HomeroomClass hrc = HomeroomClass.builder()
                .grade("10").section("A1")
                .homeroomTeacherId(5L)
                .build();
        when(homeroomClassRepository.findById(1L)).thenReturn(Optional.of(hrc));
        when(homeroomClassRepository.save(any(HomeroomClass.class))).thenAnswer(inv -> inv.getArgument(0));

        HomeroomClass result = service.assignHomeroomTeacher(1L, 10L);

        assertThat(result.getHomeroomTeacherId()).isEqualTo(10L);
    }
}
