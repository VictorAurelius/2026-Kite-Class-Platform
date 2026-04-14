package com.kiteclass.core.module.academicyear.entity;

/**
 * Type of holiday in academic calendar.
 *
 * <ul>
 *   <li>NATIONAL: VN national holidays (Tết, 30/4, 2/9, etc.)</li>
 *   <li>SCHOOL: School-specific (e.g., founding anniversary)</li>
 *   <li>RELIGIOUS: Religious observances (if tenant specifies)</li>
 * </ul>
 *
 * @since 3.15.0 (GAP-053, ADR-002)
 */
public enum HolidayType {
    NATIONAL,
    SCHOOL,
    RELIGIOUS
}
