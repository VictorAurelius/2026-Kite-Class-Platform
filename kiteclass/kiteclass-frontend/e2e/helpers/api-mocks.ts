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
  // Students API - use glob patterns
  await page.route('**/api/v1/students*', async (route) => {
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

  // Classes API (nested under courses)
  await page.route('**/api/v1/courses/*/classes*', async (route) => {
    const method = route.request().method();

    if (method === 'POST') {
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
}
