# GAP-054: Multi-Subject per Student (K-12 Model)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (K-12 blocker)
**Domain:** Backend / Data Model
**Detected:** 2026-04-14 (persona review)
**Persona blocked:** P5 K-12 School, P9 International School

## Problem

Current data model:
```
Student → Enrollment → Class → Course (1 môn học)
```

→ Implies 1 student enrolled in 1 class of 1 course.

**Reality của trường K-12:**
- 1 học sinh học **cùng lúc 12+ môn** (Toán, Văn, Anh, Lý, Hóa, Sinh, Sử, Địa, GDCD, Công nghệ, Tin học, Thể dục)
- Each subject có teacher riêng
- Attendance chung (theo class), nhưng grade riêng từng môn
- Curriculum structure: same class (e.g., 10A1) attends multiple subject classes

## Current Model Issues

```
If student "Nguyễn Văn A" in class "10A1":
  - Enrollment for class 10A1 → có 1 course
  - But really student studies 12 subjects
  - How to grade 12 subjects separately?
  - How to assign 12 teachers?
```

Platform hiện tại **assume 1 enrollment = 1 course** (trung tâm model). Phải revise cho K-12.

## Proposed Fix

### 1. Split Class Structure

```
Homeroom Class (Lớp chính)
├── Contains students (e.g., 30 students in "10A1")
├── Has homeroom teacher (GVCN — GAP-056)
└── Parent class for all subject sections

Subject Section (Lớp bộ môn)
├── Maps: (HomeroomClass) × (Subject/Course)
├── Example: "10A1 - Toán" — students of 10A1 learning Toán
├── Has subject teacher
├── Has schedule slots
└── Own grade records
```

### 2. Entity Model

```java
@Entity
public class HomeroomClass {
  Long id;
  AcademicYear academicYear;
  String grade;        // "10"
  String section;      // "A1"
  Teacher homeroomTeacher;  // GAP-056
  Integer capacity;
}

@Entity
public class StudentEnrollment {  // existing
  Student student;
  HomeroomClass homeroomClass;    // Not course directly
  AcademicYear academicYear;
  EnrollmentStatus status;
}

@Entity
public class SubjectSection {
  HomeroomClass homeroomClass;
  Course subject;
  Teacher teacher;
  String schedule;  // "T2,T4,T6 07:00-07:45"
  Integer weeklyHours;
}

@Entity
public class SubjectGrade {  // per subject per student
  Student student;
  SubjectSection section;
  Semester semester;
  BigDecimal midtermScore;
  BigDecimal finalScore;
  BigDecimal regularScore;  // điểm thường xuyên
  BigDecimal average;       // computed
  LetterGrade letterGrade;
}
```

### 3. Curriculum Definition

```java
@Entity
public class Curriculum {
  String grade;  // "10"
  @OneToMany Set<Course> subjects;  // 12 môn
  Integer totalSubjects;
}

// Pre-populated VN curriculum per grade
Grade 10:
  - Toán (4 tiết/tuần)
  - Ngữ Văn (4 tiết/tuần)
  - Ngoại ngữ (3 tiết/tuần)
  - Vật lý (2 tiết/tuần)
  - ... etc.
```

### 4. Attendance

Attendance recorded twice:
- **Homeroom class:** morning/afternoon attendance (có mặt tại trường)
- **Subject section:** per-class attendance (có đi học môn đó không)

### 5. Grade Calculation

Semester grade per student:
```
Final average = weighted avg of 12 subjects
Per subject: (điểm TX × 1 + midterm × 2 + final × 3) / 6
```

Configurable weights per curriculum.

### 6. Timetable View

Student sees timetable:
```
T2:
  7:00-7:45 Toán (phòng 201, thầy An)
  7:50-8:35 Ngữ Văn (phòng 201, cô Bình)
  8:40-9:25 Tiếng Anh (phòng 305, cô Cẩm)
...
```

Generated from SubjectSection schedules.

## Acceptance Criteria

- [ ] Data model split: HomeroomClass + SubjectSection
- [ ] Curriculum definitions cho VN grades (1-12)
- [ ] Student enrolled in HomeroomClass → auto-enrolled in all SubjectSections of that class
- [ ] Attendance dual tracking (homeroom + per-subject)
- [ ] Grade tracking per subject per semester
- [ ] Timetable generation from subject schedules
- [ ] Report card aggregates 12+ subjects (GAP-055)
- [ ] Migration: existing tenants using center model không bị break
- [ ] Integration test: K-12 scenario (30 students × 12 subjects × semester)

## Dependencies

- GAP-053 (academic year) — parent structure
- GAP-055 (report card) — consumer
- GAP-056 (homeroom teacher)

## Log

- 2026-04-14 — Persona review K-12 identified — critical data model mismatch
