package com.kiteclass.core.module.academicyear.entity;

import com.kiteclass.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Academic year entity — top-level organizing structure for K-12 schools + universities.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-ACYR-001: Name unique per tenant (e.g., "2026-2027")</li>
 *   <li>BR-ACYR-002: endDate > startDate</li>
 *   <li>BR-ACYR-003: Only 1 CURRENT year per tenant at any time</li>
 *   <li>BR-ACYR-004: Contains 1+ semesters (HK1, HK2, SUMMER)</li>
 *   <li>BR-ACYR-005: Holidays scoped to this academic year</li>
 * </ul>
 *
 * <p>Aggregate Root per DDD pattern (ADR-002).
 *
 * @author KiteClass Team
 * @since 3.15.0 (GAP-053)
 */
@Entity
@Table(
        name = "academic_years",
        indexes = {
                @Index(name = "idx_academic_years_tenant_name", columnList = "instance_id,name", unique = true),
                @Index(name = "idx_academic_years_status", columnList = "status"),
                @Index(name = "idx_academic_years_instance_id", columnList = "instance_id"),
                @Index(name = "idx_academic_years_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicYear extends BaseEntity {

    /**
     * Name of the academic year (e.g., "2026-2027", "AY 2026").
     * Unique per tenant.
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * Start date (typically early September in VN — khai giảng).
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * End date (typically mid-June in VN).
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Current status (UPCOMING, CURRENT, COMPLETED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AcademicYearStatus status = AcademicYearStatus.UPCOMING;

    /**
     * Semesters within this academic year (HK1, HK2, optionally SUMMER).
     * Cascade persist + remove.
     */
    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Semester> semesters = new ArrayList<>();

    /**
     * Holidays in this academic year (national + school-specific).
     */
    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Holiday> holidays = new ArrayList<>();

    /**
     * Convenience: check if this year is currently active.
     */
    public boolean isCurrent() {
        return status == AcademicYearStatus.CURRENT;
    }

    /**
     * Convenience: check if given date falls within this year.
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
