/**
 * Mock VN parent persona data — used when the kc-core endpoints are
 * unavailable (dev mode, story-driven test runs). Vietnamese-only realistic
 * names + tenant context per CLAUDE.md.
 *
 * Wave 49 Bucket A (GAP-267). Real data wires through `useMyChildren` (Wave
 * 18b1) and the existing transcript hook; these mocks back the auxiliary
 * tabs (billing, attendance overview, grades overview) until the per-feature
 * BE endpoints land in Phase 1.5.
 */

export interface MockActivity {
  id: string;
  title: string;
  sub: string;
  trailing: string;
  variant: 'green' | 'blue' | 'amber' | 'red' | 'muted';
  iconKey: 'check' | 'shield' | 'clock' | 'message';
}

export interface MockInvoice {
  id: string;
  title: string;
  childName: string;
  amount: number;
  status: 'PENDING_PAYMENT' | 'OVERDUE' | 'PAID';
  dueDate?: string;
  paidAt?: string;
  termLabel: string;
}

export interface MockGrade {
  subject: string;
  /** VN 10-pt scale. */
  score: number;
  /** Vietnamese học lực: Giỏi / Khá / Trung bình / Yếu. */
  level: 'Giỏi' | 'Khá' | 'Trung bình' | 'Yếu';
  trend: 'up' | 'down' | 'flat';
}

export interface MockAttendanceCell {
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED' | 'NO_CLASS';
}

export const MOCK_ACTIVITIES: ReadonlyArray<MockActivity> = [
  {
    id: 'a1',
    title: 'Có điểm Toán mới: 8.5',
    sub: 'Bài kiểm tra giữa kỳ 1 · Cô Trần Thị Hương',
    trailing: '2 giờ trước',
    variant: 'green',
    iconKey: 'check',
  },
  {
    id: 'a2',
    title: 'Đi học đầy đủ tuần này',
    sub: '5/5 buổi · Tuần 14/4–20/4',
    trailing: 'Hôm qua',
    variant: 'blue',
    iconKey: 'shield',
  },
  {
    id: 'a3',
    title: 'Bài tập Văn sắp hết hạn',
    sub: 'Còn 2 ngày · Hạn 18/04/2026',
    trailing: 'Hôm nay',
    variant: 'amber',
    iconKey: 'clock',
  },
  {
    id: 'a4',
    title: 'GVCN gửi nhận xét tuần',
    sub: 'Cô Trần Thị Hương — "Con tích cực phát biểu, cần chú ý..."',
    trailing: '2 ngày trước',
    variant: 'blue',
    iconKey: 'message',
  },
];

export const MOCK_INVOICES: ReadonlyArray<MockInvoice> = [
  {
    id: 'inv-2025-q1-001',
    title: 'Học phí kỳ II năm học 2025–2026',
    childName: 'Lê Minh Tuấn',
    amount: 4_500_000,
    status: 'PENDING_PAYMENT',
    dueDate: '30/04/2026',
    termLabel: 'Kỳ II · 2025-2026',
  },
  {
    id: 'inv-2025-q1-002',
    title: 'Phí đồng phục mùa xuân',
    childName: 'Lê Minh Tuấn',
    amount: 350_000,
    status: 'OVERDUE',
    dueDate: '10/04/2026',
    termLabel: 'Phí phụ · 2025-2026',
  },
  {
    id: 'inv-2024-q2-901',
    title: 'Học phí kỳ I năm học 2025–2026',
    childName: 'Lê Minh Tuấn',
    amount: 4_500_000,
    status: 'PAID',
    paidAt: '12/09/2025',
    termLabel: 'Kỳ I · 2025-2026',
  },
];

export const MOCK_GRADES: ReadonlyArray<MockGrade> = [
  { subject: 'Toán', score: 8.5, level: 'Giỏi', trend: 'up' },
  { subject: 'Ngữ văn', score: 7.8, level: 'Khá', trend: 'flat' },
  { subject: 'Tiếng Anh', score: 8.2, level: 'Giỏi', trend: 'up' },
  { subject: 'Vật lý', score: 7.5, level: 'Khá', trend: 'down' },
  { subject: 'Hóa học', score: 8.0, level: 'Giỏi', trend: 'flat' },
  { subject: 'Sinh học', score: 7.2, level: 'Khá', trend: 'up' },
  { subject: 'Lịch sử', score: 8.4, level: 'Giỏi', trend: 'up' },
  { subject: 'Địa lý', score: 7.6, level: 'Khá', trend: 'flat' },
];

/** Format a VN currency amount as `4.500.000đ`. */
export function formatVN(amount: number): string {
  return `${amount.toLocaleString('vi-VN')}đ`;
}

/** Build 30 mock attendance cells for the current month preview. */
export function buildMockAttendance(): MockAttendanceCell[] {
  const cells: MockAttendanceCell[] = [];
  for (let day = 1; day <= 30; day++) {
    const date = `2026-04-${day.toString().padStart(2, '0')}`;
    const dow = (day + 1) % 7; // arbitrary weekday distribution
    let status: MockAttendanceCell['status'] = 'PRESENT';
    if (dow === 0 || dow === 6) status = 'NO_CLASS';
    else if (day === 7) status = 'LATE';
    else if (day === 14) status = 'EXCUSED';
    else if (day === 21) status = 'ABSENT';
    cells.push({ date, status });
  }
  return cells;
}
