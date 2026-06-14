package com.kiteclass.core.module.academicyear;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.academicyear.entity.Holiday;
import com.kiteclass.core.module.academicyear.entity.HolidayType;
import com.kiteclass.core.module.academicyear.repository.AcademicYearRepository;
import com.kiteclass.core.module.academicyear.service.AcademicYearService;
import com.kiteclass.core.module.academicyear.service.VnHolidayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AcademicYearService (aggregate root service).
 */
@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private VnHolidayProvider vnHolidayProvider;

    @InjectMocks
    private AcademicYearService service;

    private AcademicYear validYear;

    @BeforeEach
    void setUp() {
        validYear = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 6, 15))
                .status(AcademicYearStatus.UPCOMING)
                .build();
    }

    @Test
    void createAcademicYear_with_valid_input_succeeds_with_seeded_holidays() {
        when(academicYearRepository.existsByNameAndDeletedFalse("2026-2027")).thenReturn(false);
        Holiday mockHoliday = Holiday.builder()
                .name("Tết Dương lịch")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .type(HolidayType.NATIONAL)
                .build();
        when(vnHolidayProvider.generateForAcademicYear(any())).thenReturn(List.of(mockHoliday));
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

        AcademicYear result = service.createAcademicYear(
                "2026-2027",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2027, 6, 15));

        assertThat(result.getName()).isEqualTo("2026-2027");
        assertThat(result.getStatus()).isEqualTo(AcademicYearStatus.UPCOMING);
        assertThat(result.getHolidays()).hasSize(1);
        verify(academicYearRepository).save(any(AcademicYear.class));
    }

    @Test
    void createAcademicYear_throws_when_name_exists() {
        when(academicYearRepository.existsByNameAndDeletedFalse("2026-2027")).thenReturn(true);

        assertThatThrownBy(() -> service.createAcademicYear(
                "2026-2027",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2027, 6, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createAcademicYear_throws_when_endDate_not_after_startDate() {
        when(academicYearRepository.existsByNameAndDeletedFalse("bad-year")).thenReturn(false);

        assertThatThrownBy(() -> service.createAcademicYear(
                "bad-year",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 5)))  // same date
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must be after startDate");
    }

    @Test
    void setCurrent_demotes_existing_current_then_promotes() {
        AcademicYear existing = AcademicYear.builder()
                .name("2025-2026")
                .status(AcademicYearStatus.CURRENT)
                .startDate(LocalDate.of(2025, 9, 5))
                .endDate(LocalDate.of(2026, 6, 15))
                .build();

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(validYear));
        when(academicYearRepository.findFirstByStatusAndDeletedFalse(AcademicYearStatus.CURRENT))
                .thenReturn(Optional.of(existing));
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

        AcademicYear result = service.setCurrent(1L);

        assertThat(result.getStatus()).isEqualTo(AcademicYearStatus.CURRENT);
        assertThat(existing.getStatus()).isEqualTo(AcademicYearStatus.COMPLETED);
    }

    @Test
    void setCurrent_works_when_no_existing_current() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(validYear));
        when(academicYearRepository.findFirstByStatusAndDeletedFalse(AcademicYearStatus.CURRENT))
                .thenReturn(Optional.empty());
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

        AcademicYear result = service.setCurrent(1L);

        assertThat(result.getStatus()).isEqualTo(AcademicYearStatus.CURRENT);
    }

    @Test
    void setCurrent_throws_when_year_not_found() {
        when(academicYearRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setCurrent(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getCurrent_returns_current_year_when_exists() {
        validYear.setStatus(AcademicYearStatus.CURRENT);
        when(academicYearRepository.findFirstByStatusAndDeletedFalse(AcademicYearStatus.CURRENT))
                .thenReturn(Optional.of(validYear));

        Optional<AcademicYear> result = service.getCurrent();

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AcademicYearStatus.CURRENT);
    }

    @Test
    void getCurrent_returns_empty_when_no_current() {
        when(academicYearRepository.findFirstByStatusAndDeletedFalse(AcademicYearStatus.CURRENT))
                .thenReturn(Optional.empty());

        assertThat(service.getCurrent()).isEmpty();
    }

    @Test
    void isHoliday_returns_true_when_date_within_holiday_range() {
        Holiday tet = Holiday.builder()
                .name("Tết Nguyên đán")
                .startDate(LocalDate.of(2027, 2, 6))
                .endDate(LocalDate.of(2027, 2, 12))
                .type(HolidayType.NATIONAL)
                .build();
        validYear.setStatus(AcademicYearStatus.CURRENT);
        validYear.getHolidays().add(tet);

        // GAP-134 (Wave 9.5): isHoliday routes via findFirstByStatusWithHolidays
        // to avoid N+1 on the lazy holidays collection.
        when(academicYearRepository.findFirstByStatusWithHolidays(AcademicYearStatus.CURRENT))
                .thenReturn(Optional.of(validYear));

        assertThat(service.isHoliday(LocalDate.of(2027, 2, 8))).isTrue();
        assertThat(service.isHoliday(LocalDate.of(2027, 3, 1))).isFalse();
    }

    @Test
    void isHoliday_returns_false_when_no_current_year() {
        when(academicYearRepository.findFirstByStatusWithHolidays(AcademicYearStatus.CURRENT))
                .thenReturn(Optional.empty());

        assertThat(service.isHoliday(LocalDate.of(2027, 1, 1))).isFalse();
    }

    @Test
    void getById_and_listAll_delegate_to_repository() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(validYear));
        // GAP-1362: listAll() is now bounded — delegates to findAll(Pageable), never the
        // unbounded findAll().
        when(academicYearRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(validYear)));

        assertThat(service.getById(1L)).isPresent();
        assertThat(service.listAll()).hasSize(1);
        verify(academicYearRepository).findAll(any(Pageable.class));
    }
}
