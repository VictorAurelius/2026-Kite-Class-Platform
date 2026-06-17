/**
 * Attendance report XLSX export (GAP-1478).
 *
 * Builds an Excel (.xlsx) workbook from already-fetched attendance data,
 * supporting four export criteria the user can pick from:
 *   - 'detail'   Chi tiết điểm danh   — one row per attendance record
 *   - 'session'  Theo buổi học        — aggregated per session
 *   - 'student'  Theo học sinh        — aggregated per student
 *   - 'summary'  Tổng hợp lớp         — class-level KPI summary
 *
 * Each selected criterion becomes its own sheet in a single workbook, so the
 * user can export one criterion or all four at once. Logic lives here (not in
 * the page) so it stays unit-testable and keeps the page slim.
 *
 * Reuses the SheetJS `xlsx` library (XLSX.utils.aoa_to_sheet + book_new +
 * book_append_sheet) — the same dependency the bulk-import feature relies on
 * for spreadsheet handling.
 *
 * Bundle-budget note (GAP-1478): `xlsx` (SheetJS) is heavy and MUST NOT land in
 * the report route's First Load JS. So this module only `import type`s xlsx
 * (erased at build time) — the runtime library is pulled lazily via
 * `await import('xlsx')` inside {@link exportAttendanceXlsx}, which only fires
 * when the user clicks "Export". `buildAttendanceWorkbook` takes the loaded
 * `xlsx` module as a parameter so it stays synchronous + unit-testable.
 *
 * @author KiteClass Team
 * @since 2.7.0 (wave-flow-kc3, GAP-1478)
 */

import type * as XLSX from 'xlsx';
import { AttendanceStatusLabels, type AttendanceStatus } from '@/types/attendance';

/**
 * Minimal attendance record shape needed for export. Structurally compatible
 * with {@link import('@/types/attendance').Attendance} so the page can pass
 * `attendanceData.content` straight through.
 */
export interface AttendanceExportRecord {
  studentName: string;
  sessionId: number;
  sessionNumber?: number;
  status: AttendanceStatus;
  markedDate: string; // ISO datetime
  notes?: string;
  pointsAwarded: number;
}

/** Per-student aggregated counts (matches the page's `studentStats` shape). */
export interface AttendanceStudentStat {
  studentName: string;
  total: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  makeup: number;
}

/** Class-level aggregated counts (matches the page's `stats` shape). */
export interface AttendanceClassStats {
  total: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  makeup: number;
}

/** Per-session aggregated counts. */
export interface AttendanceSessionStat {
  sessionNumber: number | string;
  date: string; // vi-VN formatted
  total: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  makeup: number;
}

/** Supported export criteria. */
export type AttendanceExportCriterion = 'detail' | 'session' | 'student' | 'summary';

/** Stable emit order for sheets regardless of user pick order. */
export const ATTENDANCE_CRITERION_ORDER: AttendanceExportCriterion[] = [
  'detail',
  'session',
  'student',
  'summary',
];

/**
 * Inputs for building the workbook. `stats`/`studentStats` are optional — when
 * omitted (e.g. in tests) they are computed from `records`.
 */
export interface AttendanceExportInput {
  className: string;
  records: AttendanceExportRecord[];
  stats?: AttendanceClassStats;
  studentStats?: AttendanceStudentStat[];
}

/** Vietnamese sheet names (≤31 chars, no Excel-banned chars). */
export const ATTENDANCE_SHEET_NAMES: Record<AttendanceExportCriterion, string> = {
  detail: 'Chi tiết',
  session: 'Theo buổi học',
  student: 'Theo học sinh',
  summary: 'Tổng hợp lớp',
};

/** Human label per criterion for the picker UI. */
export const ATTENDANCE_CRITERION_LABELS: Record<AttendanceExportCriterion, string> = {
  detail: 'Chi tiết điểm danh',
  session: 'Theo buổi học',
  student: 'Theo học sinh',
  summary: 'Tổng hợp lớp',
};

type StatusKey = 'present' | 'absent' | 'late' | 'excused' | 'makeup';

function emptyCounts() {
  return { total: 0, present: 0, absent: 0, late: 0, excused: 0, makeup: 0 };
}

function statusKey(status: AttendanceStatus): StatusKey {
  return status.toLowerCase() as StatusKey;
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('vi-VN');
}

/** Guarded percentage — returns 0 when total is 0 (avoids NaN%). */
function pct(n: number, total: number): number {
  return total > 0 ? (n / total) * 100 : 0;
}

function rateCell(n: number, total: number): string {
  return `${pct(n, total).toFixed(1)}%`;
}

/** Aggregate class-level counts from raw records. */
export function computeClassStats(records: AttendanceExportRecord[]): AttendanceClassStats {
  return records.reduce((acc, r) => {
    acc.total++;
    acc[statusKey(r.status)]++;
    return acc;
  }, emptyCounts());
}

/** Aggregate per-student counts from raw records (one entry per student name). */
export function computeStudentStats(records: AttendanceExportRecord[]): AttendanceStudentStat[] {
  const byStudent = new Map<string, AttendanceStudentStat>();
  for (const r of records) {
    let entry = byStudent.get(r.studentName);
    if (!entry) {
      entry = { studentName: r.studentName, ...emptyCounts() };
      byStudent.set(r.studentName, entry);
    }
    entry.total++;
    entry[statusKey(r.status)]++;
  }
  return Array.from(byStudent.values());
}

/** Aggregate per-session counts from raw records (grouped by sessionId). */
export function computeSessionStats(records: AttendanceExportRecord[]): AttendanceSessionStat[] {
  const bySession = new Map<number, AttendanceSessionStat>();
  for (const r of records) {
    let entry = bySession.get(r.sessionId);
    if (!entry) {
      entry = {
        sessionNumber: r.sessionNumber ?? r.sessionId,
        date: formatDate(r.markedDate),
        ...emptyCounts(),
      };
      bySession.set(r.sessionId, entry);
    }
    entry.total++;
    entry[statusKey(r.status)]++;
  }
  // Stable order by session number when numeric.
  return Array.from(bySession.values()).sort(
    (a, b) => Number(a.sessionNumber) - Number(b.sessionNumber),
  );
}

/** AOA for the detail sheet (one row per record). */
export function buildDetailRows(records: AttendanceExportRecord[]): (string | number)[][] {
  const header = ['Học viên', 'Buổi học', 'Trạng thái', 'Ngày điểm danh', 'Ghi chú', 'Điểm'];
  const rows = records.map((r) => [
    r.studentName,
    r.sessionNumber ?? '',
    AttendanceStatusLabels[r.status],
    formatDate(r.markedDate),
    r.notes ?? '',
    r.pointsAwarded,
  ]);
  return [header, ...rows];
}

/** AOA for the per-session sheet. */
export function buildSessionRows(sessions: AttendanceSessionStat[]): (string | number)[][] {
  const header = [
    'Buổi học',
    'Ngày',
    'Tổng',
    'Có mặt',
    'Vắng',
    'Đi trễ',
    'Có phép',
    'Học bù',
    'Tỷ lệ có mặt',
  ];
  const rows = sessions.map((s) => [
    s.sessionNumber,
    s.date,
    s.total,
    s.present,
    s.absent,
    s.late,
    s.excused,
    s.makeup,
    rateCell(s.present, s.total),
  ]);
  return [header, ...rows];
}

/** AOA for the per-student sheet. */
export function buildStudentRows(students: AttendanceStudentStat[]): (string | number)[][] {
  const header = [
    'Học viên',
    'Tổng',
    'Có mặt',
    'Vắng',
    'Đi trễ',
    'Có phép',
    'Học bù',
    'Tỷ lệ có mặt',
  ];
  const rows = students.map((s) => [
    s.studentName,
    s.total,
    s.present,
    s.absent,
    s.late,
    s.excused,
    s.makeup,
    rateCell(s.present, s.total),
  ]);
  return [header, ...rows];
}

/** AOA for the class-summary sheet (KPI block). */
export function buildSummaryRows(
  className: string,
  stats: AttendanceClassStats,
  studentStats: AttendanceStudentStat[],
): (string | number)[][] {
  return [
    ['Báo cáo điểm danh — Tổng hợp lớp'],
    ['Lớp học', className],
    ['Ngày xuất', new Date().toLocaleDateString('vi-VN')],
    [],
    ['Chỉ số', 'Số lượng', 'Tỷ lệ'],
    ['Tổng số lần điểm danh', stats.total, '100.0%'],
    ['Số học viên', studentStats.length, ''],
    ['Có mặt', stats.present, rateCell(stats.present, stats.total)],
    ['Vắng', stats.absent, rateCell(stats.absent, stats.total)],
    ['Đi trễ', stats.late, rateCell(stats.late, stats.total)],
    ['Có phép', stats.excused, rateCell(stats.excused, stats.total)],
    ['Học bù', stats.makeup, rateCell(stats.makeup, stats.total)],
  ];
}

/**
 * Build a workbook with one sheet per selected criterion. Criteria are emitted
 * in a stable order (detail → session → student → summary) regardless of pick
 * order. Falls back to all four when `criteria` is empty.
 *
 * The `xlsx` runtime module is passed in (not imported at module scope) so this
 * function stays synchronous + unit-testable AND so the heavy SheetJS bundle
 * never lands in the report route's First Load JS (GAP-1478). Callers obtain it
 * via `await import('xlsx')` — see {@link exportAttendanceXlsx}.
 */
export function buildAttendanceWorkbook(
  input: AttendanceExportInput,
  criteria: AttendanceExportCriterion[],
  xlsx: typeof import('xlsx'),
): XLSX.WorkBook {
  const selected = criteria.length > 0 ? criteria : ATTENDANCE_CRITERION_ORDER;
  const picked = ATTENDANCE_CRITERION_ORDER.filter((c) => selected.includes(c));

  const stats = input.stats ?? computeClassStats(input.records);
  const studentStats = input.studentStats ?? computeStudentStats(input.records);

  const wb = xlsx.utils.book_new();

  for (const criterion of picked) {
    let aoa: (string | number)[][];
    switch (criterion) {
      case 'detail':
        aoa = buildDetailRows(input.records);
        break;
      case 'session':
        aoa = buildSessionRows(computeSessionStats(input.records));
        break;
      case 'student':
        aoa = buildStudentRows(studentStats);
        break;
      case 'summary':
        aoa = buildSummaryRows(input.className, stats, studentStats);
        break;
    }
    const ws = xlsx.utils.aoa_to_sheet(aoa);
    xlsx.utils.book_append_sheet(wb, ws, ATTENDANCE_SHEET_NAMES[criterion]);
  }

  return wb;
}

/** Slugify a class name into a filename-safe ASCII fragment. */
function slugifyClassName(name: string): string {
  const ascii = name
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '') // strip combining diacritics
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D');
  return (
    ascii
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40) || 'lop'
  );
}

/** Build the export filename: bao-cao-diem-danh-<class>-<date>.xlsx */
export function attendanceExportFilename(className: string, date: Date = new Date()): string {
  const day = date.toISOString().split('T')[0];
  return `bao-cao-diem-danh-${slugifyClassName(className)}-${day}.xlsx`;
}

/**
 * Build the workbook and trigger a browser download. Uses XLSX.write(type:
 * 'array') + Blob + anchor (instead of XLSX.writeFile) so no Node `fs` path is
 * pulled into the client bundle.
 *
 * Async because the heavy `xlsx` library is loaded lazily via dynamic import at
 * click time (GAP-1478) — keeps it out of the report route's First Load JS as a
 * separate chunk.
 */
export async function exportAttendanceXlsx(
  input: AttendanceExportInput,
  criteria: AttendanceExportCriterion[],
): Promise<void> {
  const XLSX = await import('xlsx');
  const wb = buildAttendanceWorkbook(input, criteria, XLSX);
  const buffer = XLSX.write(wb, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer;
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = attendanceExportFilename(input.className);
  link.click();
  URL.revokeObjectURL(url);
}
