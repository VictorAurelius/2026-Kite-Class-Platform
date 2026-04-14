# Academic Year — Use Cases

### UC-ACYR-01: Create Academic Year
- **Actor:** Admin (school / university tenant)
- **Precondition:** Name doesn't exist in tenant
- **Steps:**
  1. FE: Admin panel → Academic Calendar → New
  2. Input: name "2026-2027", startDate 2026-09-05, endDate 2027-06-15
  3. System: validate name unique (BR-ACYR-001) + dates (BR-ACYR-002)
  4. System: create AcademicYear với status UPCOMING
  5. System: auto-seed VN national holidays (BR-ACYR-006)
- **Postcondition:** Year created với 6+ holidays
- **Errors:**
  - 400 if name exists: "Năm học '2026-2027' đã tồn tại"
  - 400 if endDate ≤ startDate

### UC-ACYR-02: Set Current Academic Year
- **Actor:** Admin
- **Precondition:** Year exists và status = UPCOMING
- **Steps:**
  1. Admin navigates to year detail → "Đặt làm năm học hiện tại"
  2. System: find existing CURRENT (if any)
  3. System: demote existing CURRENT → COMPLETED (BR-ACYR-003)
  4. System: promote selected → CURRENT
- **Postcondition:** Exactly 1 year is CURRENT in tenant

### UC-ACYR-03: View Current Year + Semester Progress
- **Actor:** Admin / Teacher / Student
- **Steps:**
  1. System: Query AcademicYear WHERE status = CURRENT
  2. Return year name + current semester (based on today's date)
  3. Display: "HK1 năm học 2026-2027 — Tuần 8/18"
- **FE Behavior:** Banner at top of dashboard shows current period

### UC-ACYR-04: Add Custom Holiday
- **Actor:** Admin
- **Steps:**
  1. Admin navigates to year detail → Holidays tab → Add
  2. Input: name "Ngày thành lập trường", dates
  3. System: validate dates within academic year range (BR-HLD-002)
  4. System: create Holiday với type = SCHOOL
- **Postcondition:** Custom holiday added to calendar

### UC-ACYR-05: Year-End Rollover
- **Actor:** Admin (manual trigger) or Scheduler (automatic at endDate)
- **Precondition:** Current year's endDate reached
- **Steps:**
  1. Finalize all semester grades (ref GAP-055)
  2. Generate report cards
  3. Process promotion/retention (ref GAP-061)
  4. Transition CURRENT → COMPLETED
  5. Prepare next UPCOMING year (if exists)
- **Postcondition:** Year closed, ready for new year

## Log
- 2026-04-14 — Initial UCs (GAP-053)
