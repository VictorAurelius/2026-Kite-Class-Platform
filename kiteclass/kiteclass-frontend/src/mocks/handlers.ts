/**
 * MSW Request Handlers - Mock API responses for testing
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { http, HttpResponse } from 'msw';

// Mock base URL
const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const handlers = [
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
            name: 'John Doe',
            email: 'john@student.com',
            phone: '0901234567',
            dateOfBirth: '2005-05-15',
            gender: 'MALE',
            address: '123 Main St',
            status: 'ACTIVE',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
          },
          {
            id: 2,
            name: 'Jane Smith',
            email: 'jane@student.com',
            phone: '0907654321',
            dateOfBirth: '2006-08-20',
            gender: 'FEMALE',
            address: '456 Oak Ave',
            status: 'ACTIVE',
            createdAt: '2024-01-02T00:00:00Z',
            updatedAt: '2024-01-02T00:00:00Z',
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
        name: 'John Doe',
        email: 'john@student.com',
        phone: '0901234567',
        dateOfBirth: '2005-05-15',
        gender: 'MALE',
        address: '123 Main St',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
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

  // ==================== Teachers API ====================
  http.get(`${BASE_URL}/api/v1/teachers`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        content: [
          {
            id: 1,
            name: 'Mr. Smith',
            email: 'smith@teacher.com',
            phone: '0911111111',
            specialization: 'Mathematics',
            status: 'ACTIVE',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });
  }),

  http.get(`${BASE_URL}/api/v1/teachers/:id`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        name: 'Mr. Smith',
        email: 'smith@teacher.com',
        phone: '0911111111',
        specialization: 'Mathematics',
        status: 'ACTIVE',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
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
  http.get(`${BASE_URL}/api/v1/courses`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        content: [
          {
            id: 1,
            name: 'Mathematics 101',
            code: 'MATH101',
            description: 'Introduction to Mathematics',
            price: 500000,
            status: 'PUBLISHED',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    });
  }),

  http.get(`${BASE_URL}/api/v1/courses/:id`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      success: true,
      data: {
        id: Number(id),
        name: 'Mathematics 101',
        code: 'MATH101',
        description: 'Introduction to Mathematics',
        price: 500000,
        status: 'PUBLISHED',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
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
            name: 'Math 101 - Morning Class',
            classCode: 'MATH101-M',
            schedule: 'Mon, Wed, Fri: 08:00-10:00',
            locationType: 'IN_PERSON',
            locationDetail: 'Room A101',
            startDate: '2024-03-01',
            endDate: '2024-06-30',
            maxStudents: 30,
            currentEnrolled: 15,
            status: 'SCHEDULED',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
          },
        ],
        totalElements: 1,
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
        name: 'Math 101 - Morning Class',
        classCode: 'MATH101-M',
        description: 'Morning section',
        schedule: 'Mon, Wed, Fri: 08:00-10:00',
        locationType: 'IN_PERSON',
        locationDetail: 'Room A101',
        startDate: '2024-03-01',
        endDate: '2024-06-30',
        maxStudents: 30,
        currentEnrolled: 15,
        status: 'SCHEDULED',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
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
