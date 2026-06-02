/**
 * Shared helpers for Wave 49 follow-up E2E specs (Wave 51 Bucket A).
 *
 * Closes Playwright portion of GAP-267a + GAP-268b + GAP-269c. Lighthouse
 * portion deferred to dedicated post-staging-HTTPS run.
 *
 * Auth strategy: persona-aware token injection — seed localStorage with the
 * Zustand auth store + accessToken/refreshToken/tenantId before each
 * navigation, mirroring the production token shape but with the correct
 * `userType` field (PARENT / TEACHER / STUDENT). The kc-parent home page
 * routes non-PARENT users to `/dashboard`, so the userType MUST match the
 * persona under test.
 *
 * @since Wave 51 Bucket A — KC E2E sweep
 */
import type { Page, Route } from '@playwright/test';

const MOCK_TOKEN =
  'eyJhbGciOiJIUzI1NiJ9.eyJ0ZW5hbnRJZCI6IjExMTExMTExLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMSJ9.mock';
const MOCK_TENANT = '11111111-1111-1111-1111-111111111111';

type Persona = 'PARENT' | 'TEACHER' | 'STUDENT';

interface PersonaProfile {
  id: number;
  email: string;
  name: string;
  userType: Persona;
}

const PERSONA_PROFILES: Record<Persona, PersonaProfile> = {
  PARENT: {
    id: 100,
    email: 'parent@kiteclass.local',
    name: 'Phụ huynh Test',
    userType: 'PARENT',
  },
  TEACHER: {
    id: 200,
    email: 'teacher@kiteclass.local',
    name: 'Cô Lan',
    userType: 'TEACHER',
  },
  STUDENT: {
    id: 300,
    email: 'student@kiteclass.local',
    name: 'Học sinh Test',
    userType: 'STUDENT',
  },
};

/**
 * Inject auth tokens for the given persona by seeding sessionStorage at an
 * about:blank document, then navigate to the persona's landing route.
 *
 * GAP-830: tokens + auth-storage moved to sessionStorage for per-tab isolation.
 */
export async function loginAs(page: Page, persona: Persona): Promise<void> {
  const profile = PERSONA_PROFILES[persona];
  // Seed at app origin BEFORE the first SPA load so Zustand's `persist`
  // middleware sees the existing state and skips its empty default.
  await page.goto('/login');
  await page.evaluate(
    ({ profile, token, tenant }) => {
      sessionStorage.setItem('accessToken', token);
      sessionStorage.setItem('refreshToken', 'mock-refresh-token');
      sessionStorage.setItem('tenantId', tenant);
      const authStore = {
        state: {
          user: {
            id: profile.id,
            email: profile.email,
            name: profile.name,
            userType: profile.userType,
            referenceId: String(profile.id),
          },
          accessToken: token,
          refreshToken: 'mock-refresh-token',
          tenantId: tenant,
          isAuthenticated: true,
        },
        version: 0,
      };
      sessionStorage.setItem('auth-storage', JSON.stringify(authStore));
    },
    { profile, token: MOCK_TOKEN, tenant: MOCK_TENANT },
  );
}

/** Mock parent /me + /me/children endpoints with a single linked child. */
export async function mockParentApi(page: Page): Promise<void> {
  await page.route('**/api/v1/parent/me', (route: Route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { id: 100, fullName: 'Phụ huynh Test', email: 'parent@kiteclass.local' },
      }),
    }),
  );
  await page.route('**/api/v1/parent/me/children', (route: Route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: [
          {
            studentId: 501,
            fullName: 'Nguyễn Bé An',
            className: 'Lớp 10A2',
            avatarUrl: null,
            attendanceRate: 0.94,
          },
        ],
      }),
    }),
  );
  await page.route('**/api/v1/parent/children/501/transcript', (route: Route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: [
          {
            semesterId: 'sem-1',
            semesterLabel: 'Học kỳ I 2025–2026',
            gpa: 8.4,
            classification: 'Giỏi',
            entries: [
              { subject: 'Toán', score: 9.0 },
              { subject: 'Văn', score: 8.0 },
            ],
          },
        ],
      }),
    }),
  );
}

/**
 * Mock the student assignment submit endpoint. Toggle `failure` to simulate
 * a server 500 (used by the offline-error-branch path).
 */
export async function mockAssignmentSubmit(
  page: Page,
  options: { failure?: boolean } = {},
): Promise<void> {
  await page.route('**/api/student/assignments/*/submit', (route: Route) => {
    if (options.failure) {
      route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"INTERNAL"}' });
    } else {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true }),
      });
    }
  });
}

export const STORAGE_KEYS = {
  studentAssignmentQueue: 'kc.student.offline-submits',
} as const;
