# GAP-1001: generateTranscript không lọc semester + credit hardcode 3.0 + studentName null

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (business — KC-6)
**Found:** 2026-06-05 (Wave flow-kc6 pre-walk, MEDIUM #9/#11)
**Affects:** `GradeServiceImpl.generateTranscript` + `GradeMapper.toTranscriptResponse`

## Problem

1. **Không lọc semester:** `generateTranscript` dùng `findFinalizedGradesByStudentId(studentId)` (TẤT CẢ finalized grades) → transcript "Spring 2026" gộp cả grade kỳ trước → GPA sai.
2. **Credit hardcode `BigDecimal.valueOf(3.0)`** mỗi course → totalCredits = #course × 3, không phản ánh credit thật.
3. **`extractAcademicYear`** parse chữ cuối semester string → "Học kỳ 1" → academicYear=1 (sai); semester VN không năm → null → `uk_transcripts_student_semester (student, NULL, NULL)` cho phép trùng (NULL distinct).
4. **studentName/studentEmail null** trong `toTranscriptResponse` (mapper ignore, không fetch tên thật) → report card "Bảng điểm của: (trống)".

## Proposed Fix

Lọc finalized grades theo semester/academic_year truyền vào; lấy credit từ course (không hardcode); fix `extractAcademicYear` (hoặc require semester format chuẩn); populate studentName từ student lookup. Defer (transcript secondary, GPA correctness Phase 1.5).

## Acceptance Criteria
- [ ] transcript chỉ gồm grade đúng semester
- [ ] credit từ course thật
- [ ] studentName hiển thị đúng

## Related
- Discovered in: Wave flow-kc6 pre-walk 2026-06-05 (FM #9/#11)
- Authz transcript covered by GAP-999

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-1.5-paid (notes 'GPA correctness Phase 1.5').
- **2026-06-05 (Wave flow-kc6):** Filed — defer (transcript secondary; GPA correctness Phase 1.5).
