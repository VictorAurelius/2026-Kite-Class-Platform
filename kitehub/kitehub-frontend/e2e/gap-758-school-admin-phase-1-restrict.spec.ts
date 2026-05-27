/**
 * GAP-758 — KH /school-admin Phase 1 BETA restrict regression guard.
 *
 * Verify all authenticated personas (OWNER / STAFF / ADMIN / PLATFORM_ADMIN)
 * bị bounce khỏi `/school-admin/*` routes trong Phase 1 BETA — SCHOOL_ADMIN
 * role chưa exist trong KH PlatformRole enum (per CLAUDE.md "Phase 3
 * (+8-12 tuần post-counsel): + K-12 P5").
 *
 * Per `.claude/rules/e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion
 * mandate: UI exposure audit 2026-05-27 found 5 `/school-admin/*` routes
 * accessible by all authenticated personas via AUTH-only layout. Fix
 * GAP-758 Option A → pair với E2E spec same PR.
 *
 * Mirrors pattern in `role-guard.spec.ts` (Wave 80 Bucket C GAP-562b).
 *
 * @since Wave 758 — GAP-758
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth } from './utils/test-helpers';

const SCHOOL_ADMIN_PATHS = [
  '/school-admin/bulk-import',
  '/school-admin/report-cards',
  '/school-admin/teacher-management',
  '/school-admin/parent-comms',
  '/school-admin/conduct',
] as const;

const ROLES_PHASE_1_BETA = ['OWNER', 'STAFF', 'ADMIN', 'PLATFORM_ADMIN'] as const;

test.describe('GAP-758 KH /school-admin Phase 1 BETA route-restrict', () => {
  for (const role of ROLES_PHASE_1_BETA) {
    test.describe(`role ${role}`, () => {
      test.beforeEach(async ({ page }) => {
        await clearBrowserStorage(page);
        await setupMockAuth(page, role);
      });

      for (const path of SCHOOL_ADMIN_PATHS) {
        test(`${role} hitting ${path} → bounces /dashboard (Phase 1 BETA restrict)`, async ({ page }) => {
          await page.goto(path);
          // Layout schedules router.replace('/dashboard'); allow up to 3s redirect.
          await expect(page).toHaveURL(/\/dashboard$/, { timeout: 3000 });
        });
      }
    });
  }
});
