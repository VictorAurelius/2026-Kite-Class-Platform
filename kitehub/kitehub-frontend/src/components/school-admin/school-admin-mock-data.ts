/**
 * Mock data for K-12 School Principal scope.
 *
 * Wave 50 Bucket A (GAP-271): kh-admin K-12 Principal port.
 * Source: documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/*.html
 *
 * Realistic VN K-12 reference: Trường THCS Nguyễn Du
 * - 1.247 học sinh, 25 lớp (6 khối), 62 giáo viên
 * - Năm học 2026–2027, Học kỳ 1, Tuần 12/18
 * - Hiệu trưởng: Cô Trần Thị Lan
 *
 * @since Wave 50 Bucket A (Phase 4 Track 2 production port)
 */

export const SCHOOL_PROFILE = {
  name: 'Trường THCS Nguyễn Du',
  shortCode: 'NGUYEN_DU_HCM',
  district: 'Quận 5, TP.HCM',
  principalName: 'Cô Trần Thị Lan',
  principalInitials: 'TL',
  principalRole: 'Hiệu trưởng',
  academicYear: '2026–2027',
  semester: 'Học kỳ 1',
  weekNumber: 12,
  totalWeeks: 18,
  totalStudents: 1247,
  totalClasses: 25,
  totalTeachers: 62,
  totalGrades: 6,
} as const;

export interface KpiCard {
  label: string;
  value: string;
  delta: string;
  deltaDirection: 'up' | 'down' | 'flat';
  sub: string;
}

export const DASHBOARD_KPIS: KpiCard[] = [
  { label: 'Tổng học sinh', value: '1.247', delta: '+47 vs năm trước', deltaDirection: 'up', sub: '25 lớp · 6 khối' },
  { label: 'Tỷ lệ đi học', value: '94,3%', delta: '+1,2 điểm', deltaDirection: 'up', sub: '7 ngày qua · TB toàn trường' },
  { label: 'Thu học phí', value: '87,3%', delta: 'Mục tiêu 95%', deltaDirection: 'flat', sub: '158 hồ sơ trễ hạn' },
  { label: 'Hạnh kiểm cần xử lý', value: '7', delta: '-3 vs tuần trước', deltaDirection: 'down', sub: '2 ca leo thang phụ huynh' },
  { label: 'Giáo viên', value: '62', delta: '+4 mới tháng này', deltaDirection: 'up', sub: '3 GV nghỉ phép hôm nay' },
  { label: 'SLA phản hồi PH', value: '2,3h', delta: 'Mục tiêu <4h', deltaDirection: 'up', sub: '12 yêu cầu chờ xử lý' },
];

export interface PendingTask {
  id: string;
  urgency: 'urgent' | 'high' | 'normal';
  title: string;
  sub: string;
  pillLabel: string;
  pillKind: 'danger' | 'warning' | 'info';
  slaLabel: string;
  slaKind: 'bad' | 'warn' | 'ok';
  ctaLabel: string;
  ctaHref: string;
  avatarText: string;
}

export const PENDING_TASKS: PendingTask[] = [
  {
    id: 't1',
    urgency: 'urgent',
    title: 'Phụ huynh Nguyễn Quang Huy yêu cầu gặp hiệu trưởng',
    sub: 'Lớp 8A2 · Sự việc gây gổ ngày 27/04 · Đã chuyển từ GVCN cô Phạm Thị Yến',
    pillLabel: 'Cấp bách',
    pillKind: 'danger',
    slaLabel: 'SLA: 18h trễ',
    slaKind: 'bad',
    ctaLabel: 'Xử lý',
    ctaHref: '/school-admin/conduct',
    avatarText: 'PH',
  },
  {
    id: 't2',
    urgency: 'high',
    title: 'Phê duyệt học bạ HK1 chuẩn MoET — 25 lớp',
    sub: 'Đã có 22/25 lớp hoàn tất nhập điểm · Hạn cuối: 05/05/2026',
    pillLabel: 'Quan trọng',
    pillKind: 'warning',
    slaLabel: 'Còn 6 ngày',
    slaKind: 'warn',
    ctaLabel: 'Xem',
    ctaHref: '/school-admin/report-cards',
    avatarText: 'HB',
  },
  {
    id: 't3',
    urgency: 'high',
    title: 'Gửi nhắc thu học phí HK1 — đợt 2',
    sub: '158 phụ huynh chưa đóng · Tổng tồn 568.400.000đ · Mẫu Zalo OA đã sẵn',
    pillLabel: 'Tồn lâu',
    pillKind: 'warning',
    slaLabel: 'Trễ TB 12 ngày',
    slaKind: 'warn',
    ctaLabel: 'Soạn nhắc',
    ctaHref: '/school-admin/fees',
    avatarText: 'HP',
  },
  {
    id: 't4',
    urgency: 'normal',
    title: '12 yêu cầu phụ huynh chưa phản hồi',
    sub: 'Trong đó 3 ca quá 4h SLA · 9 ca trong giờ',
    pillLabel: 'Hàng đợi',
    pillKind: 'info',
    slaLabel: 'TB 2h 18p',
    slaKind: 'ok',
    ctaLabel: 'Xem hàng đợi',
    ctaHref: '/school-admin/parent-comms',
    avatarText: '12',
  },
];

export interface GradeBlock {
  name: string;
  classCount: number;
  studentCount: number;
  fillPct: number;
}

export const GRADE_BLOCKS: GradeBlock[] = [
  { name: 'Khối 6', classCount: 5, studentCount: 218, fillPct: 96 },
  { name: 'Khối 7', classCount: 5, studentCount: 215, fillPct: 95 },
  { name: 'Khối 8', classCount: 5, studentCount: 204, fillPct: 92 },
  { name: 'Khối 9', classCount: 5, studentCount: 196, fillPct: 88 },
  { name: 'Khối liên cấp', classCount: 5, studentCount: 414, fillPct: 100 },
];

export interface RecentEnrollment {
  date: string;
  fullName: string;
  initials: string;
  classCode: string;
  parent: string;
  phone: string;
  feeStatus: 'paid' | 'pending' | 'overdue';
  studentStatus: 'active' | 'pending';
}

export const RECENT_ENROLLMENTS: RecentEnrollment[] = [
  { date: '29/04/2026', fullName: 'Phạm Thị Mai', initials: 'PM', classCode: '6A1', parent: 'Phạm Văn Hùng', phone: '0901 234 567', feeStatus: 'paid', studentStatus: 'active' },
  { date: '28/04/2026', fullName: 'Nguyễn Tuấn Khang', initials: 'NT', classCode: '9A3', parent: 'Nguyễn Văn Đức', phone: '0987 654 321', feeStatus: 'pending', studentStatus: 'pending' },
  { date: '27/04/2026', fullName: 'Lê Thị Bích Ngọc', initials: 'LT', classCode: '7A2', parent: 'Lê Minh Tuấn', phone: '0912 345 678', feeStatus: 'paid', studentStatus: 'active' },
  { date: '26/04/2026', fullName: 'Trần Quốc Bảo', initials: 'TQ', classCode: '8A1', parent: 'Trần Thị Hương', phone: '0905 678 901', feeStatus: 'paid', studentStatus: 'active' },
  { date: '25/04/2026', fullName: 'Vũ Hoài Anh', initials: 'VH', classCode: '6A2', parent: 'Vũ Đức Long', phone: '0938 222 111', feeStatus: 'overdue', studentStatus: 'pending' },
];

export interface Teacher {
  id: string;
  fullName: string;
  initials: string;
  subject: string;
  homeroom?: string;
  email: string;
  phone: string;
  role: 'GVCN' | 'GV bộ môn' | 'Tổ trưởng' | 'Phó hiệu trưởng';
  status: 'active' | 'leave' | 'pending';
  classCount: number;
}

export const TEACHERS: Teacher[] = [
  { id: 'gv001', fullName: 'Phạm Thị Yến', initials: 'PY', subject: 'Toán', homeroom: '8A2', email: 'yen.pham@nguyendu.edu.vn', phone: '0901 111 222', role: 'GVCN', status: 'active', classCount: 4 },
  { id: 'gv002', fullName: 'Nguyễn Văn Hải', initials: 'NH', subject: 'Văn', homeroom: '9A1', email: 'hai.nguyen@nguyendu.edu.vn', phone: '0902 222 333', role: 'GVCN', status: 'active', classCount: 5 },
  { id: 'gv003', fullName: 'Lê Thị Hồng', initials: 'LH', subject: 'Anh', email: 'hong.le@nguyendu.edu.vn', phone: '0903 333 444', role: 'GV bộ môn', status: 'active', classCount: 8 },
  { id: 'gv004', fullName: 'Trần Văn Minh', initials: 'TM', subject: 'Lý', homeroom: '7A3', email: 'minh.tran@nguyendu.edu.vn', phone: '0904 444 555', role: 'GVCN', status: 'leave', classCount: 3 },
  { id: 'gv005', fullName: 'Vũ Thị Lan', initials: 'VL', subject: 'Hoá', email: 'lan.vu@nguyendu.edu.vn', phone: '0905 555 666', role: 'Tổ trưởng', status: 'active', classCount: 6 },
  { id: 'gv006', fullName: 'Đỗ Quang Huy', initials: 'DH', subject: 'Sử', email: 'huy.do@nguyendu.edu.vn', phone: '0906 666 777', role: 'GV bộ môn', status: 'active', classCount: 5 },
  { id: 'gv007', fullName: 'Hoàng Thị Mai', initials: 'HM', subject: 'Địa', homeroom: '6A1', email: 'mai.hoang@nguyendu.edu.vn', phone: '0907 777 888', role: 'GVCN', status: 'active', classCount: 4 },
  { id: 'gv008', fullName: 'Bùi Văn Tuấn', initials: 'BT', subject: 'Sinh', email: 'tuan.bui@nguyendu.edu.vn', phone: '0908 888 999', role: 'GV bộ môn', status: 'active', classCount: 7 },
];

export interface ClassRow {
  code: string;
  homeroomTeacher: string;
  studentCount: number;
  attendanceRate: number;
}

// Generate 25 classes (5 per grade block)
export const CLASSES: ClassRow[] = (() => {
  const grades = ['6', '7', '8', '9'];
  const rows: ClassRow[] = [];
  const pickTeacher = (idx: number): string => TEACHERS[idx % TEACHERS.length]?.fullName ?? 'GVCN';
  let teacherIdx = 0;
  for (const g of grades) {
    for (let i = 1; i <= 5; i++) {
      rows.push({
        code: `${g}A${i}`,
        homeroomTeacher: pickTeacher(teacherIdx),
        studentCount: 40 + ((i + Number(g)) % 8),
        attendanceRate: 88 + ((i * 3 + Number(g)) % 10),
      });
      teacherIdx++;
    }
  }
  // 5 liên cấp
  for (let i = 1; i <= 5; i++) {
    rows.push({
      code: `LC${i}`,
      homeroomTeacher: pickTeacher(teacherIdx + i),
      studentCount: 80 + (i * 3),
      attendanceRate: 92 + (i % 5),
    });
  }
  return rows;
})();

export const SUBJECTS = ['Toán', 'Văn', 'Anh', 'Lý', 'Hoá', 'Sử', 'Địa', 'Sinh', 'GDCD'] as const;

export interface ParentCommRow {
  id: string;
  parentName: string;
  studentName: string;
  classCode: string;
  topic: string;
  channel: 'Zalo OA' | 'Email' | 'Hotline' | 'In-app';
  receivedAt: string;
  slaHoursLeft: number;
  status: 'pending' | 'in_progress' | 'escalated' | 'resolved';
  assignedTo: string;
}

export const PARENT_COMMS: ParentCommRow[] = [
  { id: 'pc001', parentName: 'Nguyễn Quang Huy', studentName: 'Nguyễn Tuấn Khang', classCode: '8A2', topic: 'Yêu cầu gặp hiệu trưởng — sự việc 27/04', channel: 'Hotline', receivedAt: '27/04 14:20', slaHoursLeft: -18, status: 'escalated', assignedTo: 'Cô Lan (Hiệu trưởng)' },
  { id: 'pc002', parentName: 'Phạm Văn Hùng', studentName: 'Phạm Thị Mai', classCode: '6A1', topic: 'Hỏi lịch họp PHHS đầu tháng 5', channel: 'Zalo OA', receivedAt: '29/04 09:12', slaHoursLeft: 1.5, status: 'in_progress', assignedTo: 'Cô Hoàng Mai' },
  { id: 'pc003', parentName: 'Trần Thị Hương', studentName: 'Trần Quốc Bảo', classCode: '8A1', topic: 'Báo nghỉ ốm 3 ngày', channel: 'Zalo OA', receivedAt: '28/04 16:45', slaHoursLeft: 2.3, status: 'pending', assignedTo: 'GVCN' },
  { id: 'pc004', parentName: 'Lê Minh Tuấn', studentName: 'Lê Thị Bích Ngọc', classCode: '7A2', topic: 'Hỏi điểm KT giữa kỳ', channel: 'Email', receivedAt: '28/04 11:30', slaHoursLeft: 0.5, status: 'pending', assignedTo: 'GVCN' },
  { id: 'pc005', parentName: 'Vũ Đức Long', studentName: 'Vũ Hoài Anh', classCode: '6A2', topic: 'Phản ánh chất lượng bữa trưa', channel: 'In-app', receivedAt: '26/04 13:00', slaHoursLeft: -4, status: 'escalated', assignedTo: 'Tổ hậu cần' },
];

export interface ConductCase {
  id: string;
  studentName: string;
  classCode: string;
  incidentDate: string;
  category: 'gây gổ' | 'vi phạm nội quy' | 'vắng học không phép' | 'sử dụng thiết bị' | 'khác';
  severity: 'high' | 'medium' | 'low';
  step: 1 | 2 | 3 | 4 | 5;
  stepLabel: string;
  homeroom: string;
  status: 'open' | 'in_progress' | 'resolved';
}

export const CONDUCT_CASES: ConductCase[] = [
  { id: 'cc001', studentName: 'Nguyễn Tuấn Khang', classCode: '8A2', incidentDate: '27/04/2026', category: 'gây gổ', severity: 'high', step: 4, stepLabel: 'Họp hiệu trưởng + PHHS', homeroom: 'Cô Phạm Yến', status: 'in_progress' },
  { id: 'cc002', studentName: 'Trần Văn Minh', classCode: '9A1', incidentDate: '25/04/2026', category: 'vi phạm nội quy', severity: 'medium', step: 2, stepLabel: 'Cảnh cáo bằng văn bản', homeroom: 'Thầy Hải', status: 'in_progress' },
  { id: 'cc003', studentName: 'Lê Quốc Anh', classCode: '7A3', incidentDate: '23/04/2026', category: 'vắng học không phép', severity: 'low', step: 1, stepLabel: 'Nhắc nhở miệng', homeroom: 'Thầy Minh', status: 'open' },
  { id: 'cc004', studentName: 'Phạm Thị Hồng', classCode: '8A1', incidentDate: '22/04/2026', category: 'sử dụng thiết bị', severity: 'medium', step: 3, stepLabel: 'Mời PH lên trao đổi', homeroom: 'Thầy Tuấn', status: 'in_progress' },
  { id: 'cc005', studentName: 'Đỗ Văn Quang', classCode: '6A2', incidentDate: '20/04/2026', category: 'gây gổ', severity: 'high', step: 5, stepLabel: 'Đình chỉ học có thời hạn', homeroom: 'Cô Hoàng Mai', status: 'open' },
  { id: 'cc006', studentName: 'Vũ Thị Hà', classCode: '9A2', incidentDate: '19/04/2026', category: 'khác', severity: 'low', step: 1, stepLabel: 'Nhắc nhở miệng', homeroom: 'Cô Lan Vũ', status: 'resolved' },
  { id: 'cc007', studentName: 'Hoàng Quốc Việt', classCode: '7A1', incidentDate: '18/04/2026', category: 'vi phạm nội quy', severity: 'medium', step: 2, stepLabel: 'Cảnh cáo bằng văn bản', homeroom: 'Cô Phạm Yến', status: 'in_progress' },
];

export const CONDUCT_LADDER: { step: number; label: string; description: string }[] = [
  { step: 1, label: 'Bước 1 — Nhắc nhở', description: 'Nhắc nhở miệng từ GVCN, ghi sổ' },
  { step: 2, label: 'Bước 2 — Cảnh cáo VB', description: 'Cảnh cáo bằng văn bản, gửi PH' },
  { step: 3, label: 'Bước 3 — Mời PH', description: 'Mời phụ huynh lên trao đổi với GVCN' },
  { step: 4, label: 'Bước 4 — Họp HT + PH', description: 'Họp hiệu trưởng + phụ huynh + GVCN' },
  { step: 5, label: 'Bước 5 — Đình chỉ', description: 'Đình chỉ học có thời hạn (theo quy chế MoET)' },
];

export interface FeesRow {
  classCode: string;
  totalStudents: number;
  paidCount: number;
  pendingCount: number;
  overdueCount: number;
  collectedAmount: number;
  expectedAmount: number;
}

export const FEES_BY_CLASS: FeesRow[] = (() => {
  return CLASSES.slice(0, 12).map((c) => {
    const total = c.studentCount;
    const paid = Math.floor(total * (0.7 + Math.random() * 0.25));
    const pending = Math.floor((total - paid) * 0.6);
    const overdue = total - paid - pending;
    return {
      classCode: c.code,
      totalStudents: total,
      paidCount: paid,
      pendingCount: pending,
      overdueCount: overdue,
      collectedAmount: paid * 4500000,
      expectedAmount: total * 4500000,
    };
  });
})();

export interface CalendarEvent {
  date: string;
  type: 'holiday' | 'exam' | 'meeting' | 'moet' | 'event';
  title: string;
  description?: string;
}

export const CALENDAR_EVENTS: CalendarEvent[] = [
  { date: '02/05/2026', type: 'meeting', title: 'Họp PHHS đầu tháng 5', description: '19:00 · Hội trường tầng 3' },
  { date: '06/05/2026', type: 'exam', title: 'Kiểm tra giữa HK1 — Toán/Văn', description: '06–08/05 · Toàn trường' },
  { date: '15/05/2026', type: 'moet', title: 'Hạn nộp học bạ HK1 lên Sở GD', description: 'Bắt buộc MoET' },
  { date: '20/05/2026', type: 'holiday', title: 'Nghỉ Lễ Phật đản', description: '1 ngày' },
  { date: '01/06/2026', type: 'event', title: 'Quốc tế Thiếu nhi 1/6', description: 'Hoạt động ngoại khoá' },
  { date: '15/06/2026', type: 'exam', title: 'Kiểm tra cuối HK1', description: 'Toàn trường, theo lịch khối' },
  { date: '30/06/2026', type: 'moet', title: 'Hạn nộp báo cáo HK1', description: 'Sở GD&ĐT TP.HCM' },
];

export interface ReportCardClassStatus {
  classCode: string;
  homeroom: string;
  studentCount: number;
  enteredCount: number;
  approvedByHomeroom: boolean;
  signedByPrincipal: boolean;
  moetCompliant: boolean;
}

export const REPORT_CARD_STATUS: ReportCardClassStatus[] = CLASSES.slice(0, 25).map((c, i) => ({
  classCode: c.code,
  homeroom: c.homeroomTeacher,
  studentCount: c.studentCount,
  enteredCount: i < 22 ? c.studentCount : Math.floor(c.studentCount * 0.7),
  approvedByHomeroom: i < 20,
  signedByPrincipal: i < 18,
  moetCompliant: i < 18,
}));

export const DATA_SYNC_STATUS = [
  { label: 'Đồng bộ phòng GD', status: 'success' as const, detail: 'Mới nhất 09:14' },
  { label: 'Sao lưu hôm nay', status: 'success' as const, detail: '06:00 OK' },
  { label: 'Zalo OA', status: 'info' as const, detail: '3.241 PH theo dõi' },
];
