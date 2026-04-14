# GAP-053: Academic Year + Semester Structure

**Status:** 🟢 DONE (Wave 2 Sub-PR 2.2, merged 2026-04-14)
**Branch:** wave/02-data-model
**ADR:** ADR-000
**Priority:** 🔴 P0 (K-12 blocker)
**Domain:** Backend / Product
**Detected:** 2026-04-14 (persona review)
**Persona blocked:** P5 K-12 School, P9 International School, P6 University

## Problem

K-12 và trường học Việt Nam vận hành theo **năm học (Sep → Jun)** + **2 học kỳ (HK1, HK2)**. KiteClass hiện chỉ có:
- Class (with startDate/endDate)
- Không có concept "năm học" → trường không map được logic

Kết quả: Trường cấp 3 không thể:
- Tổ chức năm học 2026-2027
- Phân biệt HK1 vs HK2
- Lên lớp / xuống lớp
- Finalize grades cuối năm
- Chuyển năm học sau

## Proposed Fix

### 1. Academic Year Entity

```java
@Entity
public class AcademicYear {
  Long id;
  String tenantId;
  String name;              // "2026-2027"
  LocalDate startDate;      // 2026-09-05 (khai giảng)
  LocalDate endDate;        // 2027-06-15
  AcademicYearStatus status; // UPCOMING, CURRENT, COMPLETED
  @OneToMany Set<Semester> semesters;
  @OneToMany Set<Holiday> holidays;
}
```

### 2. Semester Entity

```java
@Entity
public class Semester {
  Long id;
  AcademicYear academicYear;
  SemesterType type;  // HK1, HK2, SUMMER (hè)
  LocalDate startDate, endDate;
  LocalDate examStartDate, examEndDate;
  String name;  // "HK1 năm học 2026-2027"
}
```

### 3. Class ↔ Academic Year Link

```java
@Entity
public class Class {
  // Existing fields
  AcademicYear academicYear;  // NEW
  String grade;                // "Lớp 10", "Lớp 11", "Lớp 12"
  String section;              // "A1", "A2", "C1"
  // "10A1" = grade "10" + section "A1"
}
```

### 4. Holiday Calendar (VN)

```java
@Entity
public class Holiday {
  AcademicYear academicYear;
  String name;       // "Tết Nguyên đán"
  LocalDate startDate, endDate;
  HolidayType type;  // NATIONAL, SCHOOL
}
```

Pre-populate VN national holidays:
- 1/1 Tết Dương lịch
- 23-30/1 (approx) Tết Nguyên đán
- 10/3 Giỗ tổ Hùng Vương
- 30/4 + 1/5 Thống nhất + Quốc tế Lao động
- 2/9 Quốc khánh
- Plus school-specific holidays

### 5. UI Features

Admin panel:
- Create academic year
- Import holidays (Vietnamese template)
- Define semesters
- Attach classes to academic year
- Transition from one year to next

Teacher/Student view:
- Current semester banner
- Semester progress (e.g., "Tuần 12/18")
- Upcoming holidays

### 6. Year-End Rollover

```java
public void rolloverAcademicYear(Long currentYearId, Long newYearId) {
  // 1. Finalize grades in current year
  // 2. Generate report cards
  // 3. Promote students to next grade (based on grades)
  // 4. Transfer active enrollments to new year
  // 5. Archive old year data
  // 6. Start new year as CURRENT
}
```

### 7. Reports

- Mid-semester report (tháng 11 cho HK1)
- End-of-semester report (tháng 1 cho HK1)
- End-of-year report (tháng 6)

## Acceptance Criteria

- [ ] AcademicYear + Semester + Holiday entities
- [ ] DB migration với VN holiday seed
- [ ] Admin UI create/manage academic year
- [ ] Class binding to academic year
- [ ] Year-end rollover workflow
- [ ] Semester progress indicator
- [ ] Report cards timed per semester
- [ ] Integration test: create year → classes → complete → rollover

## Dependencies

- GAP-054 (multi-subject per student) — rollover affects
- GAP-055 (report card) — generated per semester
- GAP-061 (promotion logic) — rollover decides promotion

## Log

- 2026-04-14 — Persona review K-12 identified
