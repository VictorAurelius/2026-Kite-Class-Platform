/**
 * Tests for the attendance XLSX export library (GAP-1478).
 *
 * Asserts workbook structure (sheet names + selected cells) and the
 * aggregation helpers, without touching the browser download path.
 *
 * @author KiteClass Team
 * @since 2.7.0 (wave-flow-kc3, GAP-1478)
 */

import { describe, it, expect } from 'vitest';
import * as XLSX from 'xlsx';
import { AttendanceStatus } from '@/types/attendance';
import {
  buildAttendanceWorkbook,
  computeClassStats,
  computeStudentStats,
  computeSessionStats,
  attendanceExportFilename,
  ATTENDANCE_SHEET_NAMES,
  type AttendanceExportRecord,
  type AttendanceExportInput,
} from '../attendance-export';

const records: AttendanceExportRecord[] = [
  {
    studentName: 'Trần Thị Hồng',
    sessionId: 101,
    sessionNumber: 1,
    status: AttendanceStatus.PRESENT,
    markedDate: '2026-05-04T09:00:00Z',
    notes: 'Đúng giờ',
    pointsAwarded: 10,
  },
  {
    studentName: 'Nguyễn Văn An',
    sessionId: 101,
    sessionNumber: 1,
    status: AttendanceStatus.ABSENT,
    markedDate: '2026-05-04T09:00:00Z',
    pointsAwarded: 0,
  },
  {
    studentName: 'Trần Thị Hồng',
    sessionId: 102,
    sessionNumber: 2,
    status: AttendanceStatus.LATE,
    markedDate: '2026-05-06T09:00:00Z',
    pointsAwarded: 5,
  },
  {
    studentName: 'Nguyễn Văn An',
    sessionId: 102,
    sessionNumber: 2,
    status: AttendanceStatus.PRESENT,
    markedDate: '2026-05-06T09:00:00Z',
    pointsAwarded: 10,
  },
];

const input: AttendanceExportInput = {
  className: 'Lớp Anh ngữ 5A1',
  records,
};

const cell = (ws: XLSX.WorkSheet, ref: string) => ws[ref]?.v;

describe('computeClassStats', () => {
  it('aggregates totals across statuses', () => {
    expect(computeClassStats(records)).toEqual({
      total: 4,
      present: 2,
      absent: 1,
      late: 1,
      excused: 0,
      makeup: 0,
    });
  });

  it('returns zeros for empty input', () => {
    expect(computeClassStats([])).toEqual({
      total: 0,
      present: 0,
      absent: 0,
      late: 0,
      excused: 0,
      makeup: 0,
    });
  });
});

describe('computeStudentStats', () => {
  it('groups one entry per student with correct counts', () => {
    const stats = computeStudentStats(records);
    expect(stats).toHaveLength(2);
    const hong = stats.find((s) => s.studentName === 'Trần Thị Hồng');
    expect(hong).toMatchObject({ total: 2, present: 1, late: 1, absent: 0 });
    const an = stats.find((s) => s.studentName === 'Nguyễn Văn An');
    expect(an).toMatchObject({ total: 2, present: 1, absent: 1, late: 0 });
  });
});

describe('computeSessionStats', () => {
  it('groups by session and orders by session number', () => {
    const sessions = computeSessionStats(records);
    expect(sessions).toHaveLength(2);
    expect(sessions[0]!.sessionNumber).toBe(1);
    expect(sessions[0]).toMatchObject({ total: 2, present: 1, absent: 1 });
    expect(sessions[1]!.sessionNumber).toBe(2);
    expect(sessions[1]).toMatchObject({ total: 2, present: 1, late: 1 });
  });
});

describe('buildAttendanceWorkbook', () => {
  it('emits one sheet per selected criterion in stable order', () => {
    const wb = buildAttendanceWorkbook(input, ['student', 'detail', 'summary', 'session']);
    expect(wb.SheetNames).toEqual([
      ATTENDANCE_SHEET_NAMES.detail,
      ATTENDANCE_SHEET_NAMES.session,
      ATTENDANCE_SHEET_NAMES.student,
      ATTENDANCE_SHEET_NAMES.summary,
    ]);
  });

  it('falls back to all four sheets when criteria is empty', () => {
    const wb = buildAttendanceWorkbook(input, []);
    expect(wb.SheetNames).toHaveLength(4);
  });

  it('emits a single sheet when one criterion is picked', () => {
    const wb = buildAttendanceWorkbook(input, ['student']);
    expect(wb.SheetNames).toEqual([ATTENDANCE_SHEET_NAMES.student]);
  });

  it('writes Vietnamese headers + data in the detail sheet', () => {
    const wb = buildAttendanceWorkbook(input, ['detail']);
    const ws = wb.Sheets[ATTENDANCE_SHEET_NAMES.detail]!;
    expect(cell(ws, 'A1')).toBe('Học viên');
    expect(cell(ws, 'C1')).toBe('Trạng thái');
    expect(cell(ws, 'F1')).toBe('Điểm');
    // First data row: Trần Thị Hồng, session 1, Có mặt, ..., points 10
    expect(cell(ws, 'A2')).toBe('Trần Thị Hồng');
    expect(cell(ws, 'B2')).toBe(1);
    expect(cell(ws, 'C2')).toBe('Có mặt');
    expect(cell(ws, 'F2')).toBe(10);
  });

  it('writes per-student aggregates with attendance-rate cell', () => {
    const wb = buildAttendanceWorkbook(input, ['student']);
    const ws = wb.Sheets[ATTENDANCE_SHEET_NAMES.student]!;
    expect(cell(ws, 'A1')).toBe('Học viên');
    expect(cell(ws, 'H1')).toBe('Tỷ lệ có mặt');
    // Trần Thị Hồng: total 2, present 1 -> 50.0%
    expect(cell(ws, 'A2')).toBe('Trần Thị Hồng');
    expect(cell(ws, 'B2')).toBe(2);
    expect(cell(ws, 'H2')).toBe('50.0%');
  });

  it('writes the class-summary KPI block', () => {
    const wb = buildAttendanceWorkbook(input, ['summary']);
    const ws = wb.Sheets[ATTENDANCE_SHEET_NAMES.summary]!;
    expect(cell(ws, 'A1')).toBe('Báo cáo điểm danh — Tổng hợp lớp');
    expect(cell(ws, 'A2')).toBe('Lớp học');
    expect(cell(ws, 'B2')).toBe('Lớp Anh ngữ 5A1');
    // 'Tổng số lần điểm danh' row -> total 4
    expect(cell(ws, 'A6')).toBe('Tổng số lần điểm danh');
    expect(cell(ws, 'B6')).toBe(4);
  });

  it('honors precomputed stats/studentStats when provided', () => {
    const wb = buildAttendanceWorkbook(
      {
        className: 'Lớp Toán 9B',
        records,
        stats: { total: 99, present: 90, absent: 9, late: 0, excused: 0, makeup: 0 },
        studentStats: [
          { studentName: 'Phạm Thị Mai', total: 99, present: 90, absent: 9, late: 0, excused: 0, makeup: 0 },
        ],
      },
      ['summary'],
    );
    const ws = wb.Sheets[ATTENDANCE_SHEET_NAMES.summary]!;
    expect(cell(ws, 'B6')).toBe(99); // total from precomputed stats
    expect(cell(ws, 'B7')).toBe(1); // student count from precomputed studentStats
  });
});

describe('attendanceExportFilename', () => {
  it('builds a slugged ASCII filename with date', () => {
    const name = attendanceExportFilename('Lớp Anh ngữ 5A1', new Date('2026-06-17T00:00:00Z'));
    expect(name).toBe('bao-cao-diem-danh-lop-anh-ngu-5a1-2026-06-17.xlsx');
  });

  it('falls back to "lop" when class name has no ASCII content', () => {
    const name = attendanceExportFilename('', new Date('2026-06-17T00:00:00Z'));
    expect(name).toBe('bao-cao-diem-danh-lop-2026-06-17.xlsx');
  });
});
