package com.kiteclass.core.module.academicyear.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Holiday within an academic year (VN national, school-specific, religious).
 *
 * <p>Pre-populated VN national holidays on academic year creation:
 * <ul>
 *   <li>1/1 — Tết Dương lịch</li>
 *   <li>Late Jan/Early Feb — Tết Nguyên đán (7 days)</li>
 *   <li>10/3 ÂL — Giỗ tổ Hùng Vương</li>
 *   <li>30/4 + 1/5 — Thống nhất + Quốc tế Lao động</li>
 *   <li>2/9 — Quốc khánh</li>
 * </ul>
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-HLD-001: Belongs to 1 academic year</li>
 *   <li>BR-HLD-002: Date(s) within academic year period</li>
 *   <li>BR-HLD-003: endDate ≥ startDate (can be 1-day = same)</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-053, ADR-002)
 */
@Entity
@Table(
        name = "holidays",
        indexes = {
                @Index(name = "idx_holidays_year", columnList = "academic_year_id"),
                @Index(name = "idx_holidays_dates", columnList = "start_date,end_date"),
                @Index(name = "idx_holidays_instance_id", columnList = "instance_id"),
                @Index(name = "idx_holidays_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private HolidayType type = HolidayType.NATIONAL;

    @Column(name = "description", length = 500)
    private String description;

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
