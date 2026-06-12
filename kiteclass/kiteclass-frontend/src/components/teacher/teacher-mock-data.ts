/**
 * Mock data for the kc-teacher production port (Wave 49 Bucket B / GAP-268).
 *
 * Vietnamese-only realistic VN teacher dataset. Sourced from the
 * `kiteclass-teacher` HTML kit (Round 2, R2 PR #674) — same names + classes
 * + grades to keep parity with the prototype while real backend wiring
 * lands in follow-up gaps.
 *
 * Real backend wiring is intentionally deferred — for the period-attendance
 * route the live `attendance-period` API is already wired (Wave 18b2). The
 * homepage / overview / gradebook / schedule / reports / settings screens
 * use these in-memory fixtures until follow-up backend gaps land.
 */

import type {
  ClassSession,
  StudentRecord,
} from '@kite/shared-ui';

export type TeacherClassQueue = {
  id: string;
  className: string;
  subject: string;
  periodNo: number;
  startTime: string;
  endTime: string;
  room: string;
  totalStudents: number;
  attendanceMarked: boolean;
  presentCount: number;
  absentCount: number;
};

export const TEACHER_PROFILE = {
  fullName: 'Cô Trần Thu Hà',
  shortName: 'Cô Hà',
  initials: 'TH',
  homeroomClass: 'Lớp 10A2',
  subject: 'Toán nâng cao',
  email: 'ha.tran@kitehub.me',
  phone: '0912 345 678',
  schoolYear: '2026-2027',
  semester: 'Học kỳ I',
};

export const TEACHER_TODAY_QUEUE: TeacherClassQueue[] = [
  {
    id: 'cls-10a2-math',
    className: 'Lớp 10A2',
    subject: 'Toán nâng cao',
    periodNo: 2,
    startTime: '08:00',
    endTime: '09:30',
    room: 'A201',
    totalStudents: 25,
    attendanceMarked: true,
    presentCount: 22,
    absentCount: 3,
  },
  {
    id: 'cls-10a2-tutorial',
    className: 'Lớp 10A2',
    subject: 'Phụ đạo Toán',
    periodNo: 4,
    startTime: '10:30',
    endTime: '11:15',
    room: 'A201',
    totalStudents: 25,
    attendanceMarked: false,
    presentCount: 0,
    absentCount: 0,
  },
  {
    id: 'cls-11b1-math',
    className: 'Lớp 11B1',
    subject: 'Toán cơ bản',
    periodNo: 6,
    startTime: '14:00',
    endTime: '15:30',
    room: 'B305',
    totalStudents: 28,
    attendanceMarked: false,
    presentCount: 0,
    absentCount: 0,
  },
  {
    id: 'cls-12c1-math',
    className: 'Lớp 12C1',
    subject: 'Toán ôn thi',
    periodNo: 8,
    startTime: '16:00',
    endTime: '17:30',
    room: 'A105',
    totalStudents: 30,
    attendanceMarked: false,
    presentCount: 0,
    absentCount: 0,
  },
];

export const TEACHER_SAMPLE_CLASS_SESSION: ClassSession = {
  id: 'sess-10a2-math-2026-04-15',
  className: 'Lớp 10A2 - Toán nâng cao',
  sessionNumber: 12,
  date: new Date('2026-04-15T08:00:00+07:00'),
  durationMinutes: 90,
  teacherName: TEACHER_PROFILE.fullName,
};

const VN_NAMES = [
  'Bùi Thị Anh',
  'Đặng Văn Bảo',
  'Hoàng Thị Cẩm',
  'Lê Đức Duy',
  'Nguyễn Thị Hương',
  'Phạm Văn Khang',
  'Trần Thị Linh',
  'Vũ Quốc Minh',
  'Đỗ Thị Ngọc',
  'Hồ Văn Phát',
  'Lý Thị Quyên',
  'Mai Văn Sơn',
  'Nguyễn Thị Tâm',
  'Phan Văn Tuấn',
  'Trần Thị Uyên',
  'Vũ Thị Vy',
  'Bùi Văn Xuân',
  'Đặng Thị Yến',
  'Hoàng Văn An',
  'Lê Thị Bích',
  'Nguyễn Văn Cường',
  'Phạm Thị Diệu',
  'Trần Văn Đạt',
  'Vũ Thị Hà',
  'Đỗ Văn Khánh',
];

export function buildSampleStudents(): StudentRecord[] {
  return VN_NAMES.map((fullName, idx) => ({
    id: `stu-${idx + 1}`,
    fullName,
    studentCode: `HS${String(idx + 1).padStart(4, '0')}`,
    currentRate: 0.85 + ((idx * 7) % 13) / 100, // 0.85..0.97
    status: null,
  }));
}

export type ScheduleEvent = {
  id: string;
  className: string;
  subject: string;
  weekday: number; // 1=Mon..7=Sun
  startTime: string;
  endTime: string;
  room: string;
  teacherName?: string;
};

export const TEACHER_WEEK_SCHEDULE: ScheduleEvent[] = [
  { id: 'sl-1', className: 'Lớp 10A2', subject: 'Toán nâng cao', weekday: 1, startTime: '08:00', endTime: '09:30', room: 'A201' },
  { id: 'sl-2', className: 'Lớp 11B1', subject: 'Toán cơ bản', weekday: 1, startTime: '14:00', endTime: '15:30', room: 'B305' },
  { id: 'sl-3', className: 'Lớp 10A2', subject: 'Toán nâng cao', weekday: 2, startTime: '08:00', endTime: '09:30', room: 'A201' },
  { id: 'sl-4', className: 'Lớp 12C1', subject: 'Toán ôn thi', weekday: 2, startTime: '16:00', endTime: '17:30', room: 'A105' },
  { id: 'sl-5', className: 'Lớp 10A2', subject: 'Toán nâng cao', weekday: 4, startTime: '08:00', endTime: '09:30', room: 'A201' },
  { id: 'sl-6', className: 'Lớp 11B1', subject: 'Toán cơ bản', weekday: 4, startTime: '14:00', endTime: '15:30', room: 'B305' },
  { id: 'sl-7', className: 'Lớp 12C1', subject: 'Toán ôn thi', weekday: 5, startTime: '16:00', endTime: '17:30', room: 'A105' },
  { id: 'sl-8', className: 'Lớp 10A2', subject: 'Phụ đạo Toán', weekday: 6, startTime: '09:00', endTime: '10:30', room: 'A201' },
];

export type ReportRow = {
  classId: string;
  className: string;
  subject: string;
  totalStudents: number;
  averageGrade: number; // 0..10 VN scale
  excellentCount: number; // ≥ 9
  goodCount: number; // 7..< 9
  averageCount: number; // 5..< 7
  weakCount: number; // < 5
  attendanceRate: number; // 0..1
};

export const TEACHER_REPORT_ROWS: ReportRow[] = [
  {
    classId: 'cls-10a2',
    className: 'Lớp 10A2',
    subject: 'Toán nâng cao',
    totalStudents: 25,
    averageGrade: 8.2,
    excellentCount: 8,
    goodCount: 13,
    averageCount: 3,
    weakCount: 1,
    attendanceRate: 0.95,
  },
  {
    classId: 'cls-11b1',
    className: 'Lớp 11B1',
    subject: 'Toán cơ bản',
    totalStudents: 28,
    averageGrade: 7.4,
    excellentCount: 4,
    goodCount: 15,
    averageCount: 7,
    weakCount: 2,
    attendanceRate: 0.92,
  },
  {
    classId: 'cls-12c1',
    className: 'Lớp 12C1',
    subject: 'Toán ôn thi',
    totalStudents: 30,
    averageGrade: 7.8,
    excellentCount: 6,
    goodCount: 17,
    averageCount: 6,
    weakCount: 1,
    attendanceRate: 0.93,
  },
];

export const PAYROLL_SAMPLE = {
  month: 'Tháng 04 / 2026',
  baseSalary: 12_000_000,
  periodsThisMonth: 64,
  ratePerPeriod: 250_000,
  bonus: 1_500_000,
  deductions: 600_000,
  total: 12_000_000 + 64 * 250_000 + 1_500_000 - 600_000,
};
