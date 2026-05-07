/**
 * E2E API Mocking Helper
 *
 * Provides mock API responses for E2E tests using Playwright route mocking.
 * Since MSW doesn't run in Playwright browser context, we use page.route() instead.
 *
 * @since 2026-02-24
 */

import { Page } from '@playwright/test';

/**
 * Setup mocks for all API endpoints used in E2E tests.
 *
 * This includes students, teachers, courses, and classes endpoints.
 *
 * @param page - Playwright page object
 */
export async function setupApiMocks(page: Page) {
  // Students API - Single student (use regex pattern for ID)
  await page.route(/\/api\/v1\/students\/\d+/, async (route) => {
    const method = route.request().method();
    const url = route.request().url();
    const studentId = parseInt(url.match(/students\/(\d+)/)?.[1] || '0');

    if (method === 'GET') {
      // Get single student
      if (studentId === 99999) {
        // Mock 404 for non-existent student
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            error: 'STUDENT_NOT_FOUND',
            message: 'Không tìm thấy học viên',
          }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: studentId,
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
          }),
        });
      }
    } else if (method === 'PUT' || method === 'PATCH') {
      // Update student
      const postData = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: studentId,
            ...postData,
            status: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: new Date().toISOString(),
          },
        }),
      });
    } else if (method === 'DELETE') {
      // Delete student
      await route.fulfill({
        status: 204,
      });
    }
  });

  // Students API - List (regex to also match URLs with query params like ?page=0&size=20)
  await page.route(/\/api\/v1\/students(\?.*)?$/, async (route) => {
    const method = route.request().method();

    if (method === 'GET') {
      // List students
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
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
            size: 20,
            number: 0,
          },
        }),
      });
    } else if (method === 'POST') {
      // Create student
      const postData = route.request().postDataJSON();
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: 3,
            ...postData,
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        }),
      });
    }
  });

  // Courses API
  await page.route('**/api/v1/courses*', async (route) => {
    const method = route.request().method();

    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [
              {
                id: 1,
                name: 'Tiếng Anh Giao Tiếp Cơ Bản',
                code: 'ENG101',
                description: 'Khóa học tiếng Anh giao tiếp cho người mới bắt đầu',
                durationWeeks: 12,
                price: 5000000,
                status: 'PUBLISHED',
                teacherId: 1,
                teacherName: 'Nguyễn Thị Giáo',
                createdAt: '2026-01-01T00:00:00Z',
                updatedAt: '2026-01-01T00:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            size: 20,
            number: 0,
          },
        }),
      });
    }
  });

  // Teachers API
  await page.route('**/api/v1/teachers*', async (route) => {
    const method = route.request().method();

    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [
              {
                id: 1,
                name: 'Nguyễn Thị Giáo',
                email: 'giao.nguyen@kiteclass.local',
                phone: '0901234567',
                specialization: 'Tiếng Anh',
                experienceYears: 5,
                status: 'ACTIVE',
                createdAt: '2026-01-01T00:00:00Z',
                updatedAt: '2026-01-01T00:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            size: 20,
            number: 0,
          },
        }),
      });
    }
  });

  // Classes API — list under course (GET returns paginated classes, POST creates)
  await page.route('**/api/v1/courses/*/classes*', async (route) => {
    const method = route.request().method();

    if (method === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [
              {
                id: 1,
                courseId: 1,
                name: 'Lớp Tiếng Anh Buổi Sáng',
                classCode: 'ENG-B1-SANG',
                description: 'Lớp học buổi sáng',
                schedule: 'Thứ 2, 4, 6: 08:00-10:00',
                locationType: 'IN_PERSON',
                locationDetail: 'Phòng A101',
                startDate: '2026-03-01',
                endDate: '2026-06-30',
                maxStudents: 30,
                currentEnrolled: 15,
                status: 'SCHEDULED',
                startedAt: null,
                completedAt: null,
                cancelledAt: null,
                createdAt: '2026-01-01T00:00:00Z',
                updatedAt: '2026-01-01T00:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            size: 20,
            number: 0,
          },
        }),
      });
    } else if (method === 'POST') {
      const postData = route.request().postDataJSON();
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: 1,
            ...postData,
            status: 'DRAFT',
            classCode: null,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        }),
      });
    }
  });

  // Classes API — individual class detail (GET, PATCH, DELETE + lifecycle sub-routes)
  await page.route(/\/api\/v1\/classes\/(\d+)(\/.*)?$/, async (route) => {
    const method = route.request().method();
    const url = route.request().url();
    const classIdMatch = url.match(/\/classes\/(\d+)/);
    const classId = parseInt(classIdMatch?.[1] || '0');

    // 404 for non-existent class
    if (classId === 99999) {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          error: 'CLASS_NOT_FOUND',
          message: 'Không tìm thấy lớp học',
        }),
      });
      return;
    }

    // Sessions sub-route
    if (method === 'GET' && url.includes('/sessions')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 1,
              classId,
              sessionNumber: 1,
              topic: 'Giới thiệu khoá học',
              scheduledDate: '2026-03-03',
              startTime: '08:00',
              endTime: '10:00',
              status: 'SCHEDULED',
              notes: null,
              createdAt: '2026-01-01T00:00:00Z',
              updatedAt: '2026-01-01T00:00:00Z',
            },
          ],
        }),
      });
      return;
    }

    // Lifecycle mutations
    if (method === 'POST' && url.includes('/start')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: classId,
            status: 'IN_PROGRESS',
            startedAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        }),
      });
      return;
    }

    if (method === 'POST' && url.includes('/complete')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: classId,
            status: 'COMPLETED',
            completedAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        }),
      });
      return;
    }

    if (method === 'POST' && url.includes('/cancel')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: classId,
            status: 'CANCELLED',
            cancelledAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        }),
      });
      return;
    }

    if (method === 'POST' && url.includes('/generate-code')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            classId,
            classCode: 'ENG-2026-001',
            expiresAt: null,
          },
        }),
      });
      return;
    }

    // Default: GET /classes/:id — return SCHEDULED class
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: classId,
          courseId: 1,
          name: 'Lớp Tiếng Anh Buổi Sáng',
          classCode: 'ENG-B1-SANG',
          description: 'Lớp học buổi sáng',
          schedule: 'Thứ 2, 4, 6: 08:00-10:00',
          locationType: 'IN_PERSON',
          locationDetail: 'Phòng A101',
          startDate: '2026-03-01',
          endDate: '2026-06-30',
          maxStudents: 30,
          currentEnrolled: 15,
          status: 'SCHEDULED',
          startedAt: null,
          completedAt: null,
          cancelledAt: null,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      }),
    });
  });
}
