/**
 * E2E tests for staff invitation flow (Wave 80 Bucket B, GAP-561b).
 *
 * Covers Owner invite → recipient accept loop using Playwright route mocks.
 * Production verify (real BE + email) is tracked by smoke-email-actuator.sh.
 *
 * @since Wave 80 — GAP-561b
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth } from './utils/test-helpers';

const INVITATION_ROW = {
  id: 'invitation-fixture-001',
  tenantId: 'tenant-aaaa-0000-0000-0000-000000000001',
  email: 'staff.new@example.edu.vn',
  fullName: 'Nguyễn Văn Mẫu',
  role: 'STAFF',
  status: 'PENDING',
  invitedBy: 'owner-uuid-00001',
  createdAt: '2026-05-14T09:00:00Z',
  expiresAt: '2026-05-22T09:00:00Z',
  acceptedAt: null,
};

test.describe('Staff invitation flow (GAP-561b)', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'OWNER');
  });

  test('Owner sees the invite CTA + can submit form → list updates', async ({ page }) => {
    let invitationCreated = false;

    await page.route('**/api/v1/staff-invitations', async (route) => {
      if (route.request().method() === 'POST') {
        invitationCreated = true;
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(INVITATION_ROW),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(invitationCreated ? [INVITATION_ROW] : []),
        });
      }
    });

    await page.goto('/admin/staff');
    await expect(page.getByText('Quản lý nhân viên')).toBeVisible();
    await expect(page.getByText('Chưa có lời mời nào')).toBeVisible();

    await page.getByTestId('invite-staff-cta').click();
    await expect(page).toHaveURL(/\/admin\/staff\/invite$/);

    await page.getByTestId('invite-email-input').fill('staff.new@example.edu.vn');
    await page.getByTestId('invite-full-name-input').fill('Nguyễn Văn Mẫu');
    await page.getByTestId('invite-submit').click();

    await expect(page).toHaveURL(/\/admin\/staff/);
    await expect(page.getByText('staff.new@example.edu.vn')).toBeVisible();
    await expect(page.getByText('Đang chờ')).toBeVisible();
  });

  test('Public accept-invite landing — happy path', async ({ page }) => {
    const token = 'invite-jwt-token-fixture-default';

    await page.route(`**/api/v1/staff-invitations/by-token/${token}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(INVITATION_ROW),
      });
    });

    await page.route(`**/api/v1/staff-invitations/${token}/accept`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          userId: 'user-created-001',
          tenantId: INVITATION_ROW.tenantId,
          email: INVITATION_ROW.email,
          fullName: INVITATION_ROW.fullName,
          role: 'STAFF',
        }),
      });
    });

    await page.goto(`/staff/accept-invite?token=${token}`);
    await expect(page.getByText('Chấp nhận lời mời')).toBeVisible();
    await expect(page.getByText(INVITATION_ROW.email)).toBeVisible();

    await page.getByTestId('accept-password').fill('StrongPass123Aa');
    await page.getByTestId('accept-password-confirm').fill('StrongPass123Aa');
    await page.getByTestId('accept-submit').click();

    await expect(page.getByText('Tham gia thành công')).toBeVisible();
    await expect(page.getByTestId('accept-success-login')).toBeVisible();
  });

  test('Accept-invite — expired token shows actionable error', async ({ page }) => {
    const token = 'expired-token';

    await page.route(`**/api/v1/staff-invitations/by-token/${token}`, async (route) => {
      await route.fulfill({
        status: 410,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'INVITATION_EXPIRED' }),
      });
    });

    await page.goto(`/staff/accept-invite?token=${token}`);
    await expect(page.getByText('Lời mời đã hết hạn')).toBeVisible();
  });

  test('Accept-invite — weak password rejected client-side', async ({ page }) => {
    const token = 'weak-pw-token';

    await page.route(`**/api/v1/staff-invitations/by-token/${token}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(INVITATION_ROW),
      });
    });

    await page.goto(`/staff/accept-invite?token=${token}`);
    await page.getByTestId('accept-password').fill('short');
    await page.getByTestId('accept-password-confirm').fill('short');
    await page.getByTestId('accept-submit').click();
    await expect(page.getByText(/ít nhất 12 ký tự/)).toBeVisible();
  });
});
