/**
 * MSW Request Handlers - Mock API responses for testing
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { http, HttpResponse } from 'msw';

import { aiBrandingHandlers } from './ai-branding-handlers';
import { tenantHandlers } from './tenant-handlers';

// Mock base URL
const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const kiteClassCoreHandlers = [
  // ==================== Auth API ====================
  http.post(`${BASE_URL}/api/v1/auth/login`, async ({ request }) => {
    const body = await request.json() as { email: string; password: string };

    if (body.email === 'test@example.com' && body.password === 'password123') {
      return HttpResponse.json({
        success: true,
        data: {
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          user: {
            id: 1,
            email: 'test@example.com',
            role: 'OWNER',
          },
        },
      });
    }

    return HttpResponse.json(
      { success: false, message: 'Invalid credentials' },
      { status: 401 }
    );
  }),

  // ==================== Students API ====================
  http.get(`${BASE_URL}/api/v1/students`, ({ request }) => {
    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get('page') || '0');
    const size = parseInt(url.searchParams.get('size') || '20');

    return HttpResponse.json({
      success: true,
      data: {
        content: [
          {
            id: 1,
            name: 'Nguyễn Văn A',
            email: 'nguyenvana@gmail.com',
            phone: '0901234567',
            dateOfBirth: '2005-01-15',
            gender: 'MALE',
            address: '123 Đường ABC, TP.HCM',
            status: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          {
            id: 2,
            name: 'Trần Thị B',
            email: 'tranthib@gmail.com',
            phone: '0901234568',
            dateOfBirth: '2005-02-20',
            gender: 'FEMALE',
            address: '456 Đường DEF, Hà Nội',
            status: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        totalElements: 2,
        totalPages: 1,
        size,
        number: page,
      },
    });
  }),

  http.get(`${BASE_URL}/api/v1/students/:id`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        name: 'Nguyễn Văn A',
        email: 'nguyenvana@gmail.com',
        phone: '0901234567',
        dateOfBirth: '2005-01-15',
        gender: 'MALE',
        address: '123 Đường ABC, TP.HCM',
        status: 'ACTIVE',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });
  }),

  http.post(`${BASE_URL}/api/v1/students`, async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: 3,
        ...body,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.put(`${BASE_URL}/api/v1/students/:id`, async ({ params, request }) => {
    const { id } = params;
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        ...body,
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.delete(`${BASE_URL}/api/v1/students/:id`, () => {
    return HttpResponse.json({ success: true });
  }),

  // ==================== Bulk Import API (GAP-137 / Wave 60 Bucket B) ====================
  // Mirrors BE contract:
  //   POST /api/v1/students/bulk-import/preview
  //   POST /api/v1/students/bulk-import/commit
  //   POST /api/v1/students/bulk-import/jobs/{id}/errors
  // File name conventions for test triggers:
  //   - filename contains "empty"    → 0 rows
  //   - filename contains "errors"   → 5 rows, 2 errors
  //   - filename contains "fail"     → HTTP 500 (server error)
  //   - filename contains "invalid"  → HTTP 400 (parse error)
  //   - default                       → 10 rows, 0 errors
  http.post(`${BASE_URL}/api/v1/students/bulk-import/preview`, async ({ request }) => {
    // jsdom + MSW lose File.name after multipart parse; rely on X-File-Name
    // header that the FE attaches alongside the body.
    const name = (request.headers.get('x-file-name') || '').toLowerCase();

    if (name.includes('fail')) {
      return HttpResponse.json(
        { success: false, message: 'Lỗi máy chủ nội bộ' },
        { status: 500 },
      );
    }
    if (name.includes('invalid')) {
      return HttpResponse.json(
        {
          success: false,
          errorCode: 'BULK_IMPORT_PARSE',
          message: 'Tệp không phải xlsx hợp lệ',
        },
        { status: 400 },
      );
    }
    if (name.includes('empty')) {
      return HttpResponse.json({
        success: true,
        data: { jobId: null, totalRows: 0, successCount: 0, errorCount: 0, errors: [] },
      });
    }
    if (name.includes('errors')) {
      return HttpResponse.json({
        success: true,
        data: {
          jobId: null,
          totalRows: 5,
          successCount: 3,
          errorCount: 2,
          errors: [
            { rowNumber: 2, field: 'email', message: 'Email không hợp lệ' },
            { rowNumber: 4, field: 'phone', message: 'Số điện thoại không đúng định dạng VN' },
          ],
        },
      });
    }
    return HttpResponse.json({
      success: true,
      data: { jobId: null, totalRows: 10, successCount: 10, errorCount: 0, errors: [] },
    });
  }),

  http.post(`${BASE_URL}/api/v1/students/bulk-import/commit`, async ({ request }) => {
    // jsdom + MSW lose File.name after multipart parse; rely on X-File-Name
    // header that the FE attaches alongside the body.
    const name = (request.headers.get('x-file-name') || '').toLowerCase();

    if (name.includes('fail')) {
      return HttpResponse.json(
        { success: false, message: 'Lỗi máy chủ nội bộ' },
        { status: 500 },
      );
    }
    if (name.includes('errors')) {
      return HttpResponse.json(
        {
          success: true,
          data: {
            jobId: 42,
            totalRows: 5,
            successCount: 3,
            errorCount: 2,
            errors: [
              { rowNumber: 2, field: 'email', message: 'Email không hợp lệ' },
              { rowNumber: 4, field: 'phone', message: 'Số điện thoại không đúng định dạng VN' },
            ],
          },
        },
        { status: 201 },
      );
    }
    return HttpResponse.json(
      {
        success: true,
        data: {
          jobId: 7,
          totalRows: 10,
          successCount: 10,
          errorCount: 0,
          errors: [],
        },
      },
      { status: 201 },
    );
  }),

  http.post(`${BASE_URL}/api/v1/students/bulk-import/jobs/:id/errors`, () => {
    const blob = new Blob(['mock-xlsx-bytes'], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    return new HttpResponse(blob, {
      status: 200,
      headers: {
        'Content-Type':
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'Content-Disposition': 'attachment; filename="bulk-import-errors.xlsx"',
      },
    });
  }),

  // ==================== Teachers API ====================
  http.get(`${BASE_URL}/api/v1/teachers`, ({ request }) => {
    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get('page') || '0');
    const size = parseInt(url.searchParams.get('size') || '20');

    return HttpResponse.json({
      success: true,
      data: {
        content: [
          {
            id: 1,
            name: 'Nguyễn Thị Giáo',
            email: 'giao.nguyen@kiteclass.local',
            phoneNumber: '0901234567',
            specialization: 'Toán học',
            status: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          {
            id: 2,
            name: 'Trần Văn Học',
            email: 'hoc.tran@kiteclass.local',
            phoneNumber: '0901234568',
            specialization: 'Văn học',
            status: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        totalElements: 2,
        totalPages: 1,
        size,
        number: page,
      },
    });
  }),

  http.get(`${BASE_URL}/api/v1/teachers/:id`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        name: 'Nguyễn Thị Giáo',
        email: 'giao.nguyen@kiteclass.local',
        phoneNumber: '0901234567',
        specialization: 'Toán học',
        status: 'ACTIVE',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });
  }),

  http.post(`${BASE_URL}/api/v1/teachers`, async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: 2,
        ...body,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.put(`${BASE_URL}/api/v1/teachers/:id`, async ({ params, request }) => {
    const { id } = params;
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        ...body,
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.delete(`${BASE_URL}/api/v1/teachers/:id`, () => {
    return HttpResponse.json({ success: true });
  }),

  // ==================== Courses API ====================
  http.get(`${BASE_URL}/api/v1/courses`, ({ request }) => {
    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get('page') || '0');
    const size = parseInt(url.searchParams.get('size') || '20');

    return HttpResponse.json({
      success: true,
      data: {
        content: [
          {
            id: 1,
            name: 'Tiếng Anh Giao Tiếp Cơ Bản',
            code: 'ENG-B1-001',
            description: 'Khóa học tiếng Anh giao tiếp cho người mới bắt đầu',
            price: 3000000,
            durationWeeks: 12,
            totalSessions: 24,
            status: 'PUBLISHED',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          {
            id: 2,
            name: 'Toán Học Nâng Cao',
            code: 'MATH-A1-001',
            description: 'Khóa học toán nâng cao cho học sinh THPT',
            price: 2500000,
            durationWeeks: 10,
            totalSessions: 20,
            status: 'DRAFT',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        totalElements: 2,
        totalPages: 1,
        size,
        number: page,
      },
    });
  }),

  http.get(`${BASE_URL}/api/v1/courses/:id`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        name: 'Tiếng Anh Giao Tiếp Cơ Bản',
        code: 'ENG-B1-001',
        description: 'Khóa học tiếng Anh giao tiếp cho người mới bắt đầu',
        price: 3000000,
        durationWeeks: 12,
        totalSessions: 24,
        status: 'PUBLISHED',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });
  }),

  http.post(`${BASE_URL}/api/v1/courses`, async ({ request }) => {
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: 2,
        ...body,
        status: 'DRAFT',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.patch(`${BASE_URL}/api/v1/courses/:id`, async ({ params, request }) => {
    const { id } = params;
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        ...body,
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.delete(`${BASE_URL}/api/v1/courses/:id`, () => {
    return HttpResponse.json({ success: true });
  }),

  http.post(`${BASE_URL}/api/v1/courses/:id/publish`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        status: 'PUBLISHED',
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.post(`${BASE_URL}/api/v1/courses/:id/archive`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        status: 'ARCHIVED',
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  // ==================== Classes API ====================
  http.get(`${BASE_URL}/api/v1/courses/:courseId/classes`, ({ params }) => {
    const { courseId } = params;
    return HttpResponse.json({
      success: true,
      data: {
        content: [
          {
            id: 1,
            courseId: Number(courseId),
            name: 'Lớp Tiếng Anh Buổi Sáng',
            classCode: 'ENG-B1-SANG',
            schedule: 'Thứ 2, 4, 6: 08:00-10:00',
            locationType: 'IN_PERSON',
            locationDetail: 'Phòng A101',
            startDate: '2026-03-01',
            endDate: '2026-06-30',
            maxStudents: 30,
            currentEnrolled: 15,
            status: 'SCHEDULED',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          {
            id: 2,
            courseId: Number(courseId),
            name: 'Lớp Toán Buổi Chiều',
            classCode: 'MATH-A1-CHIEU',
            schedule: 'Thứ 3, 5, 7: 14:00-16:00',
            locationType: 'ONLINE',
            locationDetail: 'Zoom Meeting',
            startDate: '2026-03-15',
            endDate: '2026-05-15',
            maxStudents: 25,
            currentEnrolled: 20,
            status: 'IN_PROGRESS',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        totalElements: 2,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });
  }),

  http.get(`${BASE_URL}/api/v1/classes/:id`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        courseId: 1,
        name: 'Lớp Tiếng Anh Buổi Sáng',
        classCode: 'ENG-B1-SANG',
        description: 'Lớp học buổi sáng dành cho người đi làm',
        schedule: 'Thứ 2, 4, 6: 08:00-10:00',
        locationType: 'IN_PERSON',
        locationDetail: 'Phòng A101',
        startDate: '2026-03-01',
        endDate: '2026-06-30',
        maxStudents: 30,
        currentEnrolled: 15,
        status: 'SCHEDULED',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    });
  }),

  http.post(`${BASE_URL}/api/v1/courses/:courseId/classes`, async ({ params, request }) => {
    const { courseId } = params;
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: 2,
        courseId: Number(courseId),
        ...body,
        status: 'DRAFT',
        currentEnrolled: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.patch(`${BASE_URL}/api/v1/classes/:id`, async ({ params, request }) => {
    const { id } = params;
    const body = await request.json() as Record<string, unknown>;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        ...body,
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.delete(`${BASE_URL}/api/v1/classes/:id`, () => {
    return HttpResponse.json({ success: true });
  }),

  http.post(`${BASE_URL}/api/v1/classes/:id/start`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        status: 'IN_PROGRESS',
        startedAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.post(`${BASE_URL}/api/v1/classes/:id/complete`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        status: 'COMPLETED',
        completedAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.post(`${BASE_URL}/api/v1/classes/:id/cancel`, async ({ params, request }) => {
    const { id } = params;
    const body = await request.json() as { reason: string };
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        status: 'CANCELLED',
        cancelReason: body.reason,
        cancelledAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    });
  }),

  http.get(`${BASE_URL}/api/v1/classes/:id/sessions`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: [
        {
          id: 1,
          classId: Number(id),
          sessionNumber: 1,
          topic: 'Introduction',
          sessionDate: '2024-03-01',
          startTime: '08:00',
          endTime: '10:00',
          status: 'SCHEDULED',
        },
      ],
    });
  }),
];

export const handlers = [...kiteClassCoreHandlers, ...aiBrandingHandlers, ...tenantHandlers];
