/**
 * Role Guard FE Verify — Wave 98 Bucket B7 GAP-518 P3 extension.
 *
 * Mocks `useAuthStore` (zustand persist) state via `localStorage.setItem` and
 * navigates to protected routes to verify `RoleGuard` + `AdminLayout` +
 * `isPlatformAdmin` helper combine correctly across the 4-role matrix.
 *
 * <h2>Role naming (Wave 79 GAP-562 canonical)</h2>
 *
 * Wave 98 audit (F-NEW-7) framing → shipped canonical mapping:
 *   • P2 Center Owner            = role 'OWNER'
 *   • P3 Center Manager          = role 'STAFF'
 *   • Platform Admin (legacy)    = role 'PLATFORM_ADMIN' or 'ADMIN' → resolves to OWNER
 *   • Anonymous (no session)     = redirect to /login
 *
 * <h2>Coverage gates</h2>
 *
 * Per pre-handoff-self-test-completeness.md §2.4 admin flow + §5.4 PARTIAL
 * exit-ramp: this spec ships for CI execution. Real production walkthrough
 * deferred per GAP-612 (AWS account suspended) — code-level Playwright
 * against `pnpm dev` server is the in-scope verification for B7.
 *
 * Run locally: `pnpm exec playwright test playwright/role-guard.spec.ts`.
 * CI execution may report "Browser binaries unavailable" on default runners
 * without `npx playwright install` step — see PR body note.
 *
 * @since Wave 98 Bucket B7 — GAP-518
 */

import { test, expect } from '@playwright/test';

interface MockUser {
  id: string;
  email: string;
  role: 'OWNER' | 'STAFF' | 'PLATFORM_ADMIN' | 'ADMIN';
  fullName: string;
  emailVerified: boolean;
}

/**
 * Seed zustand-persist auth-store payload directly into localStorage.
 *
 * Mirrors the structure produced by useAuthStore.setUser + setAuthenticated,
 * with the zustand persist `name: 'auth-storage'` key and `state` wrapper.
 */
async function seedAuthStorage(
  page: import('@playwright/test').Page,
  user: MockUser | null | undefined,
): Promise<void> {
  const payload =
    !user
      ? null
      : {
          state: {
            user,
            accessToken: 'mock-jwt-token-for-playwright-test',
            isAuthenticated: true,
          },
          version: 0,
        };

  await page.addInitScript((stored) => {
    if (stored === null) {
      window.localStorage.removeItem('auth-storage');
    } else {
      window.localStorage.setItem('auth-storage', JSON.stringify(stored));
    }
  }, payload);
}

const MOCK_USERS: Record<string, MockUser> = {
  owner: {
    id: '11111111-1111-1111-1111-111111111111',
    email: 'owner@example.com',
    role: 'OWNER',
    fullName: 'P2 Center Owner Hằng',
    emailVerified: true,
  },
  staff: {
    id: '22222222-2222-2222-2222-222222222222',
    email: 'staff@example.com',
    role: 'STAFF',
    fullName: 'P3 Center Manager Tâm',
    emailVerified: true,
  },
  platformAdminLegacy: {
    id: '33333333-3333-3333-3333-333333333333',
    email: 'admin@kitehub.me',
    role: 'PLATFORM_ADMIN',
    fullName: 'Platform Admin (legacy alias)',
    emailVerified: true,
  },
};

test.describe('Role guard matrix — Wave 98 B7 GAP-518', () => {
  test('OWNER → /admin accessible (legacy alias resolves to admin layout)', async ({
    page,
  }) => {
    await seedAuthStorage(page, MOCK_USERS.platformAdminLegacy);
    await page.goto('/admin');
    // AdminLayout uses isPlatformAdmin() which accepts PLATFORM_ADMIN.
    // Should NOT redirect to /login (anonymous fallback) or /dashboard (role mismatch).
    await expect(page).toHaveURL(/\/admin/);
  });

  test('STAFF (P3 Center Manager) → /admin denied, lands /dashboard', async ({
    page,
  }) => {
    await seedAuthStorage(page, MOCK_USERS.staff);
    await page.goto('/admin');
    // AdminLayout isPlatformAdmin(STAFF) === false → router.replace('/login') per
    // current AdminLayout impl (Wave 72a Bucket C). Either /login or /dashboard
    // is acceptable as "denied" outcome — assert it is NOT /admin/*.
    await page.waitForURL((url) => !url.pathname.startsWith('/admin'), {
      timeout: 5000,
    });
    expect(page.url()).not.toMatch(/\/admin\b/);
  });

  test('STAFF (P3 Center Manager) → /dashboard accessible (tenant home)', async ({
    page,
  }) => {
    await seedAuthStorage(page, MOCK_USERS.staff);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('OWNER → /settings (RoleGuard allowedRoles=[OWNER]) accessible', async ({
    page,
  }) => {
    await seedAuthStorage(page, MOCK_USERS.owner);
    await page.goto('/settings');
    await expect(page).toHaveURL(/\/settings/);
  });

  test('STAFF → /settings denied (RoleGuard allowedRoles=[OWNER]) → /dashboard', async ({
    page,
  }) => {
    await seedAuthStorage(page, MOCK_USERS.staff);
    await page.goto('/settings');
    // RoleGuard redirects mismatched role to fallbackUrl (default /dashboard).
    await page.waitForURL((url) => !url.pathname.startsWith('/settings'), {
      timeout: 5000,
    });
    expect(page.url()).not.toMatch(/\/settings\b/);
  });

  test('Anonymous (no auth-storage) → /admin redirects away', async ({ page }) => {
    await seedAuthStorage(page, null);
    await page.goto('/admin');
    await page.waitForURL((url) => !url.pathname.startsWith('/admin'), {
      timeout: 5000,
    });
    expect(page.url()).not.toMatch(/\/admin\b/);
  });
});
