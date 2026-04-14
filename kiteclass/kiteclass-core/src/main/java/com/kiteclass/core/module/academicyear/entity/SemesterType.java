package com.kiteclass.core.module.academicyear.entity;

/**
 * Type of semester within an academic year.
 *
 * <p>VN education context:
 * <ul>
 *   <li>HK1: Học kỳ 1 (Semester 1, Sep-Jan)</li>
 *   <li>HK2: Học kỳ 2 (Semester 2, Feb-Jun)</li>
 *   <li>SUMMER: Summer term (Jul-Aug, optional)</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-053, ADR-002)
 */
public enum SemesterType {
    HK1,
    HK2,
    SUMMER
}
