/* eslint-disable */
/**
 * kiteclass-teacher — Wave 1.6 ADD-ON · React reference implementation
 *
 * Persona: Teacher (homeroom GVCN + subject) · Tier 2 KC user
 * Stack: Next.js 15 (App Router) + Tailwind + shadcn + Radix + lucide
 * NO Framer Motion (per dossier 09-tech-constraints — KC stack restriction).
 *
 * Production targets:
 *   /classes/[id]/attendance        (G2 Roster pattern)
 *   /classes/[id]/grades            (NEW route — gradebook)
 *   /classes/[id]/schedule          (NEW route — week-view)
 *   /attendance/reports             (heaviest screen redesign — was 417 LOC)
 *   /settings/teacher               (profile + payroll + notifications)
 */

import * as React from 'react';
import {
  Bell, Calendar, CheckCircle, Lock, Save, Pencil, Plus, ChevronLeft, ChevronRight,
  AlertCircle, AlertTriangle, AlertOctagon, Users, History, ShieldCheck, Info,
  TrendingUp, MessageCircle, BarChart3, Calculator, Settings, Send, Upload,
  Download, FileSpreadsheet, Link2, UserPlus, Printer, Loader, Keyboard,
  CalendarPlus, CalendarX, X, Check, Clock, FileText, QrCode, Zap, RefreshCw,
  HardDrive, Sun, Moon, ChevronDown, MoreHorizontal, Dot, CircleDot,
} from 'lucide-react';

// ---------------------------------------------------------------------------
// Mock data — use real names per VN UX standard
// ---------------------------------------------------------------------------
export const TEACHER = {
  id: 'usr-tch-001',
  fullName: 'Trần Thu Hà',
  honorific: 'Cô',
  email: 'tranthuha@thpt-nguyentruongto.edu.vn',
  phone: '0912345678',
  taxCode: '8001234567', // MST 10 digits
  birthDate: '1985-03-12',
  initials: 'TH',
  homeroomClass: { id: '10A2', name: 'Lớp 10A2 - Toán nâng cao' },
  subjectClasses: [
    { id: '10A1', name: 'Lớp 10A1 - Toán', enrollmentCount: 28 },
    { id: '11B1', name: 'Lớp 11B1 - Văn', enrollmentCount: 22 },
  ],
  payroll: { hourlyRate: 200000, commissionPct: 15, currency: 'VND' },
  notifications: {
    zaloOA_absences: true,
    email_weeklyReport: true,
    sms_emergency: true,
    email_parentMessage: false,
  },
};

export const STUDENTS_10A2 = [
  { stt: 1, code: '10A2-001', fullName: 'Bùi Thị Anh', monthlyAbsences: 0 },
  { stt: 2, code: '10A2-002', fullName: 'Đặng Văn Bảo', monthlyAbsences: 1 },
  { stt: 3, code: '10A2-003', fullName: 'Hoàng Thị Cẩm', monthlyAbsences: 2 },
  { stt: 4, code: '10A2-004', fullName: 'Lê Minh Đức', monthlyAbsences: 3 },
  { stt: 5, code: '10A2-005', fullName: 'Nguyễn Văn An', monthlyAbsences: 0 },
  { stt: 6, code: '10A2-006', fullName: 'Phạm Thị Hương', monthlyAbsences: 0 },
  { stt: 7, code: '10A2-007', fullName: 'Trần Quang Huy', monthlyAbsences: 5 },
  { stt: 8, code: '10A2-008', fullName: 'Vũ Thị Mai', monthlyAbsences: 0 },
  { stt: 9, code: '10A2-009', fullName: 'Bùi Văn Nam', monthlyAbsences: 1 },
  { stt: 10, code: '10A2-010', fullName: 'Đỗ Thị Ngọc', monthlyAbsences: 0 },
  { stt: 11, code: '10A2-011', fullName: 'Hà Quang Minh', monthlyAbsences: 2 },
  { stt: 12, code: '10A2-012', fullName: 'Lý Thanh Tùng', monthlyAbsences: 0 },
  { stt: 13, code: '10A2-013', fullName: 'Mai Thị Linh', monthlyAbsences: 1 },
  { stt: 14, code: '10A2-014', fullName: 'Ngô Văn Thắng', monthlyAbsences: 0 },
  { stt: 15, code: '10A2-015', fullName: 'Phan Thị Quỳnh', monthlyAbsences: 0 },
  { stt: 16, code: '10A2-016', fullName: 'Tô Minh Khôi', monthlyAbsences: 0 },
  { stt: 17, code: '10A2-017', fullName: 'Trịnh Thị Vân', monthlyAbsences: 1 },
  { stt: 18, code: '10A2-018', fullName: 'Vũ Quang Hải', monthlyAbsences: 0 },
  { stt: 19, code: '10A2-019', fullName: 'Bùi Thị Yến', monthlyAbsences: 0 },
  { stt: 20, code: '10A2-020', fullName: 'Cao Văn Phúc', monthlyAbsences: 0 },
  { stt: 21, code: '10A2-021', fullName: 'Đặng Thị Hồng', monthlyAbsences: 2 },
  { stt: 22, code: '10A2-022', fullName: 'Hồ Quang Huy', monthlyAbsences: 0 },
  { stt: 23, code: '10A2-023', fullName: 'Lê Thị Hằng', monthlyAbsences: 0 },
  { stt: 24, code: '10A2-024', fullName: 'Nguyễn Quốc Đạt', monthlyAbsences: 1 },
  { stt: 25, code: '10A2-025', fullName: 'Phạm Văn Tiến', monthlyAbsences: 0 },
];

export const ATTENDANCE_CODES = {
  P: { label: 'Có mặt', color: 'green-600', icon: 'check' },
  V: { label: 'Vắng có phép', color: 'blue-500', icon: 'file-text' },
  M: { label: 'Vắng không phép', color: 'red-500', icon: 'x' },
  L: { label: 'Đi trễ', color: 'amber-500', icon: 'clock' },
  S: { label: 'Bệnh', color: 'purple-500', icon: 'heart' },
};

// VN MoET grade classification thresholds
export function classifyGrade(avg) {
  if (avg >= 9) return { key: 'xuatsac', label: 'Xuất sắc' };
  if (avg >= 8) return { key: 'gioi', label: 'Giỏi' };
  if (avg >= 6.5) return { key: 'kha', label: 'Khá' };
  if (avg >= 5) return { key: 'trungbinh', label: 'Trung bình' };
  return { key: 'yeu', label: 'Yếu' };
}

// Grade weights per MoET HK regulation
export const GRADE_WEIGHTS = { test1: 0.15, test2: 0.15, midterm: 0.30, final: 0.40 };

export function computeGradeAverage({ test1, test2, midterm, final: f }) {
  const w = GRADE_WEIGHTS;
  return Math.round((test1 * w.test1 + test2 * w.test2 + midterm * w.midterm + f * w.final) * 100) / 100;
}

// Late penalty: 10%/day, max 50%
export const LATE_PENALTY_PCT_PER_DAY = 0.10;
export const LATE_PENALTY_MAX = 0.50;

export function applyLatePenalty(score, daysLate) {
  if (daysLate <= 0) return score;
  const penalty = Math.min(daysLate * LATE_PENALTY_PCT_PER_DAY, LATE_PENALTY_MAX);
  return Math.round(score * (1 - penalty) * 100) / 100;
}

// ---------------------------------------------------------------------------
// AttendanceToggle — 4-way P/V/M/L button group
// ---------------------------------------------------------------------------
export function AttendanceToggle({ studentId, value, onChange, disabled }) {
  const codes = ['P', 'V', 'M', 'L'];
  return (
    <div role="radiogroup" aria-label={`Trạng thái học sinh ${studentId}`} className="flex items-center justify-center gap-1">
      {codes.map((code) => (
        <button
          key={code}
          type="button"
          disabled={disabled}
          onClick={() => onChange?.(code)}
          aria-pressed={value === code}
          className={`att-toggle ${value === code ? `active-${code.toLowerCase()}` : ''}`}
          aria-label={ATTENDANCE_CODES[code].label}
        >
          {code}
        </button>
      ))}
    </div>
  );
}

// ---------------------------------------------------------------------------
// AttendanceRoster — full daily attendance for 25 students
// ---------------------------------------------------------------------------
export function AttendanceRoster({ classData, students, onSave }) {
  const [marks, setMarks] = React.useState(() =>
    Object.fromEntries(students.map((s) => [s.code, 'P']))
  );
  const [savedAt, setSavedAt] = React.useState(null);
  const [isDirty, setIsDirty] = React.useState(false);

  function update(code, value) {
    setMarks((prev) => ({ ...prev, [code]: value }));
    setIsDirty(true);
  }

  function markAllPresent() {
    setMarks(Object.fromEntries(students.map((s) => [s.code, 'P'])));
    setIsDirty(true);
  }

  async function handleSave() {
    await onSave?.({ classId: classData.id, marks });
    setSavedAt(new Date());
    setIsDirty(false);
  }

  const counts = students.reduce(
    (acc, s) => ({ ...acc, [marks[s.code]]: (acc[marks[s.code]] || 0) + 1 }),
    {}
  );

  return (
    <div className="space-y-4">
      <header className="flex flex-col md:flex-row md:items-end md:justify-between gap-3">
        <div>
          <h1 className="h1 mb-1">Điểm danh hôm nay</h1>
          <p className="muted body-sm">{classData.name} · {classData.session}</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={markAllPresent} className="px-3 py-2 rounded-md bg-primary text-primary-foreground tap-target inline-flex items-center gap-2">
            <Zap className="w-4 h-4" /> Đánh tất cả Có mặt
          </button>
        </div>
      </header>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        <Stat label="Tổng số" value={students.length} />
        <Stat label="Có mặt" value={counts.P || 0} accent="att-present" prefix="P" />
        <Stat label="Có phép" value={counts.V || 0} accent="att-excused" prefix="V" />
        <Stat label="Vắng" value={counts.M || 0} accent="att-absent" prefix="M" />
        <Stat label="Trễ" value={counts.L || 0} accent="att-late" prefix="L" />
      </div>

      <div className="rounded-2xl border border-border bg-card overflow-hidden shadow-soft">
        <table className="w-full text-sm">
          <thead className="border-b border-border bg-muted/40">
            <tr className="text-left">
              <th className="px-3 py-2 sticky-col w-12">STT</th>
              <th className="px-3 py-2 sticky-col" style={{ left: 48 }}>Họ và tên</th>
              <th className="px-3 py-2 hidden md:table-cell">Mã HS</th>
              <th className="px-3 py-2 text-center">Điểm danh</th>
              <th className="px-3 py-2 hidden lg:table-cell text-right">Vắng tháng</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {students.map((s) => (
              <tr key={s.code}>
                <td className="px-3 py-2 sticky-col tabular-nums">{s.stt}</td>
                <td className="px-3 py-2 sticky-col font-medium" style={{ left: 48 }}>{s.fullName}</td>
                <td className="px-3 py-2 hidden md:table-cell muted">{s.code}</td>
                <td className="px-3 py-2">
                  <AttendanceToggle
                    studentId={s.fullName}
                    value={marks[s.code]}
                    onChange={(v) => update(s.code, v)}
                  />
                </td>
                <td className={`px-3 py-2 hidden lg:table-cell text-right tabular-nums ${s.monthlyAbsences >= 3 ? 'text-destructive' : ''}`}>
                  {s.monthlyAbsences}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="save-bar">
        <div className="text-sm flex items-center">
          {isDirty && <span className="unsaved-pulse" />}
          <span>{Object.keys(marks).length}/{students.length} đã đánh{savedAt ? ` · lưu lúc ${savedAt.toLocaleTimeString('vi-VN')}` : ''}</span>
        </div>
        <button onClick={handleSave} className="px-4 py-2 rounded-md bg-primary text-primary-foreground tap-target inline-flex items-center gap-2">
          <Save className="w-4 h-4" />Lưu điểm danh
        </button>
      </div>
    </div>
  );
}

function Stat({ label, value, accent, prefix }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-3">
      <div className="text-xs muted flex items-center gap-1">
        {prefix && <span className={`att-chip att-${prefix.toLowerCase()}`}>{prefix}</span>}
        {label}
      </div>
      <div className="text-2xl font-bold tabular-nums" style={accent ? { color: `hsl(var(--${accent}))` } : {}}>
        {value}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// GradebookTable — 25 students × 5 columns + classification footer
// ---------------------------------------------------------------------------
export function GradebookTable({ students, grades, onChange, locked, errors = {} }) {
  return (
    <div className="rounded-2xl border border-border bg-card overflow-hidden shadow-soft">
      <table className="w-full text-sm">
        <thead className="border-b border-border bg-muted/40">
          <tr className="text-left">
            <th className="px-3 py-2 sticky-col w-12">STT</th>
            <th className="px-3 py-2 sticky-col" style={{ left: 48 }}>Họ và tên</th>
            <th className="px-3 py-2 text-center">Bài 1</th>
            <th className="px-3 py-2 text-center">Bài 2</th>
            <th className="px-3 py-2 text-center">Giữa kỳ</th>
            <th className="px-3 py-2 text-center">Cuối kỳ</th>
            <th className="px-3 py-2 text-center bg-primary/5 font-semibold">Tổng kết</th>
            <th className="px-3 py-2 text-center">Xếp loại</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {students.map((s) => {
            const g = grades[s.code] || {};
            const avg = computeGradeAverage(g);
            const cls = classifyGrade(avg);
            return (
              <tr key={s.code}>
                <td className="px-3 py-2 sticky-col tabular-nums">{s.stt}</td>
                <td className="px-3 py-2 sticky-col font-medium" style={{ left: 48 }}>{s.fullName}</td>
                {['test1', 'test2', 'midterm', 'final'].map((field) => (
                  <td key={field} className="text-center">
                    {locked ? (
                      <span className="grade-cell locked inline-flex items-center justify-center font-mono">{g[field]?.toFixed(2)}</span>
                    ) : (
                      <input
                        className={`grade-cell ${errors[`${s.code}.${field}`] ? 'invalid' : ''}`}
                        type="number" min="0" max="10" step="0.25"
                        value={g[field] ?? ''}
                        onChange={(e) => onChange?.(s.code, field, parseFloat(e.target.value))}
                        aria-invalid={!!errors[`${s.code}.${field}`]}
                      />
                    )}
                  </td>
                ))}
                <td className="text-center bg-primary/5 font-mono font-semibold tabular-nums">{avg.toFixed(2)}</td>
                <td className="text-center"><span className={`grade-chip grade-${cls.key}`}>{cls.label}</span></td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ---------------------------------------------------------------------------
// PayrollSummary
// ---------------------------------------------------------------------------
export function PayrollSummary({ teacher, hoursWorked }) {
  const monthlyEarnings = teacher.payroll.hourlyRate * hoursWorked;
  return (
    <div className="grid md:grid-cols-3 gap-4">
      <div className="rounded-md border border-border p-3 bg-muted/30">
        <div className="text-xs muted">Lương theo giờ</div>
        <div className="text-2xl font-bold tabular-nums font-mono mt-1">
          {teacher.payroll.hourlyRate.toLocaleString('vi-VN')}đ
          <span className="text-sm muted font-normal">/giờ</span>
        </div>
      </div>
      <div className="rounded-md border border-border p-3 bg-muted/30">
        <div className="text-xs muted">Hoa hồng</div>
        <div className="text-2xl font-bold tabular-nums mt-1">{teacher.payroll.commissionPct}%</div>
      </div>
      <div className="rounded-md border border-border p-3 bg-primary/5">
        <div className="text-xs muted">Lương dự kiến tháng</div>
        <div className="text-2xl font-bold tabular-nums font-mono mt-1 text-primary">
          {(monthlyEarnings / 1000000).toFixed(1)}M<span className="text-sm muted font-normal">đ</span>
        </div>
        <div className="text-xs muted mt-1">{hoursWorked} giờ × {teacher.payroll.hourlyRate.toLocaleString('vi-VN')}đ</div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Default export — kit demo composition
// ---------------------------------------------------------------------------
export default function TeacherKit() {
  return (
    <div className="min-h-screen bg-background text-foreground antialiased p-6 space-y-8">
      <header className="border-b border-border pb-4">
        <h1 className="text-2xl font-bold">KiteClass · Teacher Kit (Wave 1.6 Add-on)</h1>
        <p className="muted text-sm">React reference implementation. See <code className="bg-muted px-1 rounded text-xs">index.html</code> for the click-thru HTML kit.</p>
      </header>

      <section>
        <h2 className="font-bold mb-3">1. Daily Attendance</h2>
        <AttendanceRoster
          classData={{ id: '10A2', name: TEACHER.homeroomClass.name, session: 'Thứ Hai 15/04/2026 14:00-15:30 · Phòng A201' }}
          students={STUDENTS_10A2}
          onSave={async (data) => console.log('save attendance', data)}
        />
      </section>

      <section>
        <h2 className="font-bold mb-3">2. Payroll Summary</h2>
        <PayrollSummary teacher={TEACHER} hoursWorked={72} />
      </section>
    </div>
  );
}
