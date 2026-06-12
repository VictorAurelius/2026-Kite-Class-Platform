/**
 * Playwright mock API routes for screenshot capture.
 * Intercepts all /api/v1/* requests and returns realistic mock data.
 */

import type { Page, Route } from '@playwright/test';

const API_BASE = 'http://localhost:8080';

// Helper: wrap response in API envelope
function apiResponse(data: unknown) {
  return { success: true, data, timestamp: new Date().toISOString() };
}

function paginated<T>(content: T[], total?: number) {
  const t = total ?? content.length;
  return {
    content,
    totalElements: t,
    totalPages: Math.ceil(t / 20),
    size: 20,
    number: 0,
    first: true,
    last: true,
  };
}

// ── Mock Data ──

const STUDENTS = [
  { id: 1, name: 'Nguyễn Văn An', fullName: 'Nguyễn Văn An', email: 'an.nguyen@gmail.com', phone: '0901234567', dateOfBirth: '2008-03-15', gender: 'MALE', address: '12 Nguyễn Huệ, Q.1, TP.HCM', status: 'ACTIVE', createdAt: '2026-02-01T08:00:00Z', updatedAt: '2026-03-10T10:00:00Z' },
  { id: 2, name: 'Trần Thị Bích', fullName: 'Trần Thị Bích', email: 'bich.tran@gmail.com', phone: '0912345678', dateOfBirth: '2007-07-22', gender: 'FEMALE', address: '45 Lê Lợi, Q.3, TP.HCM', status: 'ACTIVE', createdAt: '2026-02-05T09:00:00Z', updatedAt: '2026-03-12T14:00:00Z' },
  { id: 3, name: 'Lê Hoàng Cường', fullName: 'Lê Hoàng Cường', email: 'cuong.le@gmail.com', phone: '0923456789', dateOfBirth: '2009-11-08', gender: 'MALE', address: '78 Trần Hưng Đạo, Q.5, TP.HCM', status: 'ACTIVE', createdAt: '2026-02-10T10:00:00Z', updatedAt: '2026-03-15T08:00:00Z' },
  { id: 4, name: 'Phạm Minh Dung', fullName: 'Phạm Minh Dung', email: 'dung.pham@gmail.com', phone: '0934567890', dateOfBirth: '2008-05-30', gender: 'FEMALE', address: '23 Hai Bà Trưng, Q.1, TP.HCM', status: 'INACTIVE', createdAt: '2026-01-15T07:00:00Z', updatedAt: '2026-02-28T16:00:00Z' },
  { id: 5, name: 'Võ Thanh Hùng', fullName: 'Võ Thanh Hùng', email: 'hung.vo@gmail.com', phone: '0945678901', dateOfBirth: '2007-09-12', gender: 'MALE', address: '56 Pasteur, Q.3, TP.HCM', status: 'ACTIVE', createdAt: '2026-03-01T08:30:00Z', updatedAt: '2026-04-01T09:00:00Z' },
];

const TEACHERS = [
  { id: 1, name: 'Nguyễn Thị Mai Lan', email: 'lan.nguyen@kitehub.me', phoneNumber: '0901111222', specialization: 'Tiếng Anh', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 2, name: 'Trần Văn Minh', email: 'minh.tran@kitehub.me', phoneNumber: '0902222333', specialization: 'Toán học', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 3, name: 'Lê Thị Hồng', email: 'hong.le@kitehub.me', phoneNumber: '0903333444', specialization: 'Vật lý', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
];

const COURSES = [
  { id: 1, name: 'Tiếng Anh Giao Tiếp B1', code: 'ENG-B1-001', description: 'Khóa học tiếng Anh giao tiếp cho người mới bắt đầu. Tập trung phát âm, ngữ pháp cơ bản và hội thoại hàng ngày.', price: 3000000, durationWeeks: 12, totalSessions: 24, status: 'PUBLISHED', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 2, name: 'Toán Nâng Cao Lớp 10', code: 'MATH-10-ADV', description: 'Chương trình toán nâng cao cho học sinh THPT, bao gồm đại số, hình học và giải tích.', price: 2500000, durationWeeks: 16, totalSessions: 32, status: 'PUBLISHED', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 3, name: 'Lập Trình Python Cơ Bản', code: 'CS-PY-001', description: 'Nhập môn lập trình Python cho người chưa có kinh nghiệm.', price: 3500000, durationWeeks: 8, totalSessions: 16, status: 'DRAFT', createdAt: '2026-02-01T00:00:00Z', updatedAt: '2026-03-01T00:00:00Z' },
];

const CLASSES = [
  { id: 1, courseId: 1, name: 'Lớp Tiếng Anh Sáng T2-T4-T6', classCode: 'ENG-B1-S246', schedule: 'Thứ 2, 4, 6: 08:00-10:00', locationType: 'IN_PERSON', locationDetail: 'Phòng A101', startDate: '2026-03-01', endDate: '2026-06-30', maxStudents: 30, currentEnrolled: 22, status: 'IN_PROGRESS', createdAt: '2026-01-15T00:00:00Z', updatedAt: '2026-03-01T00:00:00Z' },
  { id: 2, courseId: 1, name: 'Lớp Tiếng Anh Tối T3-T5', classCode: 'ENG-B1-T35', schedule: 'Thứ 3, 5: 18:30-20:30', locationType: 'ONLINE', locationDetail: 'Google Meet', startDate: '2026-03-15', endDate: '2026-07-15', maxStudents: 25, currentEnrolled: 18, status: 'SCHEDULED', createdAt: '2026-02-01T00:00:00Z', updatedAt: '2026-02-01T00:00:00Z' },
  { id: 3, courseId: 2, name: 'Lớp Toán NC Chiều T2-T4', classCode: 'MATH-10-C24', schedule: 'Thứ 2, 4: 14:00-16:00', locationType: 'IN_PERSON', locationDetail: 'Phòng B203', startDate: '2026-03-01', endDate: '2026-08-30', maxStudents: 20, currentEnrolled: 15, status: 'IN_PROGRESS', createdAt: '2026-01-20T00:00:00Z', updatedAt: '2026-03-01T00:00:00Z' },
];

const INVOICES = [
  { id: 1, invoiceNumber: 'INV-2026-001', studentId: 1, subtotal: 3000000, discount: 300000, total: 2700000, amountPaid: 2700000, balanceDue: 0, status: 'PAID', issueDate: '2026-02-15', dueDate: '2026-03-15', periodStart: '2026-03-01', periodEnd: '2026-06-30', items: [{ id: 1, description: 'Tiếng Anh Giao Tiếp B1 — Lớp Sáng', quantity: 1, amount: 3000000 }], adjustments: [], createdAt: '2026-02-15T00:00:00Z' },
  { id: 2, invoiceNumber: 'INV-2026-002', studentId: 2, subtotal: 2500000, discount: 0, total: 2500000, amountPaid: 1000000, balanceDue: 1500000, status: 'PENDING', issueDate: '2026-03-01', dueDate: '2026-04-01', periodStart: '2026-03-01', periodEnd: '2026-08-30', items: [{ id: 2, description: 'Toán Nâng Cao Lớp 10 — Lớp Chiều', quantity: 1, amount: 2500000 }], adjustments: [], createdAt: '2026-03-01T00:00:00Z' },
  { id: 3, invoiceNumber: 'INV-2026-003', studentId: 3, subtotal: 3000000, discount: 0, total: 3000000, amountPaid: 0, balanceDue: 3000000, status: 'OVERDUE', issueDate: '2026-02-01', dueDate: '2026-03-01', periodStart: '2026-03-01', periodEnd: '2026-06-30', items: [{ id: 3, description: 'Tiếng Anh Giao Tiếp B1', quantity: 1, amount: 3000000 }], adjustments: [], createdAt: '2026-02-01T00:00:00Z' },
];

const PAYMENTS = [
  { id: 1, paymentNumber: 'PAY-2026-001', invoiceId: 1, amount: 2700000, paymentMethod: 'BANK_TRANSFER', paymentStatus: 'COMPLETED', initiatedAt: '2026-03-10T14:30:00Z' },
  { id: 2, paymentNumber: 'PAY-2026-002', invoiceId: 2, amount: 1000000, paymentMethod: 'CASH', paymentStatus: 'COMPLETED', initiatedAt: '2026-03-15T10:00:00Z' },
];

const BRANDING = {
  displayName: 'KiteClass Academy',
  tagline: 'Nâng tầm tri thức — Vươn xa tương lai',
  logoUrl: '',
  primaryColor: '#2563eb',
  secondaryColor: '#1e40af',
  accentColor: '#f59e0b',
  contactEmail: 'info@kitehub.me',
  contactPhone: '028 1234 5678',
  address: '123 Nguyễn Văn Linh, Q.7, TP. Hồ Chí Minh',
  facebookUrl: 'https://facebook.com/kiteclass',
  zaloUrl: 'kiteclass',
  websiteUrl: 'https://kitehub.me',
};

const PREFERENCES = {
  language: 'VI',
  timezone: 'Asia/Ho_Chi_Minh',
  theme: 'LIGHT',
  notificationPreferences: {
    email: true, push: true, sms: false,
    enrollmentUpdates: true, paymentReminders: true, classReminders: true, attendanceAlerts: true,
  },
};

const SESSIONS = [
  { id: 1, classId: 1, sessionNumber: 1, topic: 'Giới thiệu & Alphabet', sessionDate: '2026-03-03', startTime: '08:00', endTime: '10:00', status: 'COMPLETED' },
  { id: 2, classId: 1, sessionNumber: 2, topic: 'Greetings & Self-introduction', sessionDate: '2026-03-05', startTime: '08:00', endTime: '10:00', status: 'COMPLETED' },
  { id: 3, classId: 1, sessionNumber: 3, topic: 'Numbers & Counting', sessionDate: '2026-03-07', startTime: '08:00', endTime: '10:00', status: 'SCHEDULED' },
];

const ENROLLMENTS = [
  { id: 1, studentId: 1, classId: 1, status: 'ACTIVE', enrolledAt: '2026-02-20T00:00:00Z' },
  { id: 2, studentId: 2, classId: 1, status: 'ACTIVE', enrolledAt: '2026-02-22T00:00:00Z' },
  { id: 3, studentId: 3, classId: 1, status: 'ACTIVE', enrolledAt: '2026-02-25T00:00:00Z' },
  { id: 4, studentId: 5, classId: 1, status: 'ACTIVE', enrolledAt: '2026-03-01T00:00:00Z' },
  { id: 5, studentId: 1, classId: 3, status: 'ACTIVE', enrolledAt: '2026-02-20T00:00:00Z' },
];

// ── Route Setup ──

export async function setupMockApi(page: Page) {
  await page.route(`${API_BASE}/api/v1/**`, async (route: Route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();

    // Auth
    if (path === '/api/v1/auth/refresh') {
      return route.fulfill({ json: apiResponse({ accessToken: 'mock-refreshed-token' }) });
    }

    // Students
    if (method === 'GET' && /^\/api\/v1\/students$/.test(path)) {
      return route.fulfill({ json: apiResponse(paginated(STUDENTS, 24)) });
    }
    if (method === 'GET' && /^\/api\/v1\/students\/\d+$/.test(path)) {
      const id = parseInt(path.split('/').pop()!);
      const student = STUDENTS.find(s => s.id === id) || STUDENTS[0];
      return route.fulfill({ json: apiResponse({ ...student, id }) });
    }

    // Teachers
    if (method === 'GET' && /^\/api\/v1\/teachers$/.test(path)) {
      return route.fulfill({ json: apiResponse(paginated(TEACHERS, 8)) });
    }
    if (method === 'GET' && /^\/api\/v1\/teachers\/\d+$/.test(path)) {
      const id = parseInt(path.split('/').pop()!);
      const teacher = TEACHERS.find(t => t.id === id) || TEACHERS[0];
      return route.fulfill({ json: apiResponse({ ...teacher, id }) });
    }

    // Courses
    if (method === 'GET' && /^\/api\/v1\/courses$/.test(path)) {
      return route.fulfill({ json: apiResponse(paginated(COURSES, 6)) });
    }
    if (method === 'GET' && /^\/api\/v1\/courses\/\d+$/.test(path)) {
      const id = parseInt(path.split('/').pop()!);
      const course = COURSES.find(c => c.id === id) || COURSES[0];
      return route.fulfill({ json: apiResponse({ ...course, id }) });
    }

    // Classes (under courses)
    if (method === 'GET' && /^\/api\/v1\/courses\/\d+\/classes$/.test(path)) {
      const courseId = parseInt(path.split('/')[4]);
      const filtered = CLASSES.filter(c => c.courseId === courseId);
      return route.fulfill({ json: apiResponse(paginated(filtered.length ? filtered : CLASSES)) });
    }

    // Classes (direct)
    if (method === 'GET' && /^\/api\/v1\/classes$/.test(path)) {
      return route.fulfill({ json: apiResponse(paginated(CLASSES, 5)) });
    }
    if (method === 'GET' && /^\/api\/v1\/classes\/\d+\/sessions$/.test(path)) {
      return route.fulfill({ json: apiResponse(SESSIONS) });
    }
    if (method === 'GET' && /^\/api\/v1\/classes\/\d+$/.test(path)) {
      const id = parseInt(path.split('/').pop()!);
      const cls = CLASSES.find(c => c.id === id) || CLASSES[0];
      return route.fulfill({ json: apiResponse({ ...cls, id }) });
    }

    // Invoices
    if (method === 'GET' && /^\/api\/v1\/invoices$/.test(path)) {
      return route.fulfill({ json: apiResponse(paginated(INVOICES)) });
    }
    if (method === 'GET' && /^\/api\/v1\/invoices\/\d+$/.test(path)) {
      const id = parseInt(path.split('/').pop()!);
      const inv = INVOICES.find(i => i.id === id) || INVOICES[0];
      return route.fulfill({ json: apiResponse({ ...inv, id }) });
    }

    // Payments
    if (method === 'GET' && /^\/api\/v1\/payments\/invoice\/\d+$/.test(path)) {
      const invId = parseInt(path.split('/').pop()!);
      return route.fulfill({ json: apiResponse(PAYMENTS.filter(p => p.invoiceId === invId)) });
    }

    // Enrollments
    if (method === 'GET' && /^\/api\/v1\/enrollments\/class\/\d+/.test(path)) {
      return route.fulfill({ json: apiResponse(ENROLLMENTS) });
    }

    // Attendance stats
    if (method === 'GET' && /^\/api\/v1\/attendance\/stats\/system$/.test(path)) {
      return route.fulfill({ json: apiResponse({ totalClasses: 3, totalSessions: 48, overallAttendanceRate: 87.5, absentCount: 12, presentCount: 84, lateCount: 6, excusedCount: 3 }) });
    }
    if (method === 'GET' && /^\/api\/v1\/attendance\/stats\/class\/\d+$/.test(path)) {
      return route.fulfill({ json: apiResponse({ totalSessions: 16, presentCount: 14, absentCount: 1, lateCount: 1, excusedCount: 0, attendanceRate: 87.5 }) });
    }
    if (method === 'GET' && /^\/api\/v1\/attendance\/trends$/.test(path)) {
      return route.fulfill({ json: apiResponse([]) });
    }

    // Branding
    if (method === 'GET' && path === '/api/v1/settings/branding') {
      return route.fulfill({ json: apiResponse(BRANDING) });
    }
    if (method === 'GET' && path === '/api/v1/branding') {
      return route.fulfill({ json: apiResponse(BRANDING) });
    }

    // Preferences
    if (method === 'GET' && path === '/api/v1/settings/preferences') {
      return route.fulfill({ json: apiResponse(PREFERENCES) });
    }
    if (method === 'GET' && path === '/api/v1/preferences') {
      return route.fulfill({ json: apiResponse(PREFERENCES) });
    }

    // Feature detection
    if (method === 'GET' && path === '/api/v1/features') {
      return route.fulfill({ json: apiResponse({ captcha: false, oauth: false }) });
    }

    // Fallback: return empty success for unhandled GET
    if (method === 'GET') {
      return route.fulfill({ json: apiResponse(null) });
    }

    // POST/PUT/PATCH/DELETE — return success
    return route.fulfill({ json: apiResponse({ id: 1 }) });
  });
}
