# K-12 Multi-Subject Model — Use Cases

### UC-K12-01: Create Homeroom Class
- **Actor:** School Admin
- **Precondition:** Academic year exists; tenant has K-12 flag enabled
- **Steps:**
  1. Admin selects academic year, inputs grade "10", section "A1", capacity 40, GVCN
  2. System: verify academic year exists
  3. System: check uniqueness (year, grade, section)
  4. System: create HomeroomClass
- **Postcondition:** Homeroom class "10A1" created, capacity=40, currentEnrolled=0

### UC-K12-02: Assign Homeroom Teacher (GVCN)
- **Actor:** School Admin
- **Steps:**
  1. Admin selects HomeroomClass → "Đổi GVCN"
  2. Selects teacher from ACTIVE list
  3. System: update homeroomTeacherId

### UC-K12-03: Enroll Student in HomeroomClass
- **Actor:** Admin (bulk via GAP-051 import hoặc manual)
- **Preconditions:** HomeroomClass has capacity
- **Steps:**
  1. System: check hasCapacity()
  2. System: increment currentEnrolled
  3. System: auto-create StudentEnrollment for every SubjectSection of this HRC (future enhancement)
- **Error:** 409 if capacity full

### UC-K12-04: Record Subject Grade
- **Actor:** Subject Teacher
- **Steps:**
  1. Teacher selects SubjectSection → Semester → Student
  2. Inputs: điểm TX (regular), điểm GK (midterm), điểm CK (final)
  3. System: validate 0.0-10.0
  4. System: auto-compute average = (TX×1 + GK×2 + CK×3)/6
  5. System: auto-derive letter grade

### UC-K12-05: View Student Transcript for Semester
- **Actor:** Student / Parent / Teacher
- **Steps:**
  1. System: query all SubjectGrades for (student, semester)
  2. Display table: subject | TX | GK | CK | TB môn | Xếp loại
  3. Calculate overall semester average (weighted by curriculum)

## Log
- 2026-04-14 — Initial UCs
