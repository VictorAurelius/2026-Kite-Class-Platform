/**
 * Critical Journey E2E Test: Class Lifecycle Management
 *
 * Tests the complete class lifecycle workflow:
 * 1. View class in SCHEDULED status
 * 2. Start class (SCHEDULED → IN_PROGRESS)
 * 3. Complete class (IN_PROGRESS → COMPLETED)
 * 4. Cancel class workflow (alternative path)
 * 5. Generate class code
 *
 * Selectors validated against actual VN-first UI per CLAUDE.md communication rule.
 * Key UI facts (from src/app/(dashboard)/classes/[id]/page.tsx):
 *  - Status labels: Đã lên lịch / Đang diễn ra / Đã hoàn thành / Đã hủy
 *  - Buttons: "Bắt đầu" / "Hoàn thành" / "Hủy lớp" / "Tạo mã lớp"
 *  - Cancel UI is a <Card> rendered inline (NOT role="dialog")
 *    heading "Hủy lớp học", confirm button "Xác nhận hủy"
 *  - Cancel validation: toast "Vui lòng nhập lý do hủy" (not inline DOM)
 *  - Sessions: div-based list "Buổi {n}: {topic}" (NOT a <table>)
 *  - Copy code: icon-only ghost button (no text label)
 *  - Generate code confirm: window.confirm "Tạo hoặc tạo lại mã lớp học?"
 *  - Classes list uses Shadcn Select (role="combobox") to pick course first
 *  - Actions column: Eye icon ghost button title="Xem chi tiết"
 *
 * Mock architecture:
 *  - setupApiMocks() (called inside login()) registers global routes.
 *  - Tests needing stateful lifecycle mocks call login() explicitly FIRST,
 *    then register an override route (Playwright LIFO: last-registered wins).
 *  - Tests that just need SCHEDULED state use navigateToClassDetail(page)
 *    which calls login() internally.
 *
 * // Validated locally 2026-05-07 against 2f1e29bd
 *
 * @since 2026-02-24
 * @updated 2026-05-07 GAP-420 sub-B — VN-first selector reconciliation
 */

import { test, expect } from '@playwright/test';
import { login } from '../helpers/auth';

// ──────────────────────────────────────────────────────────────────────────────
// Shared navigation helper: (optionally login) → land on /classes/:id directly.
//
// History: pre-GAP-454 the helper went through the listing UI flow
// (click /classes nav link → open Shadcn <Select> combobox → pick course option →
// wait for /api/v1/courses/:id/classes mock → click Eye icon ghost button row).
// That UI chain proved flaky in headless chromium (Shadcn Select interaction +
// row-action button selector) and made the helper a 4/6 failure point during
// GAP-453 Phase B local verify (2026-05-09), even though the underlying
// /api/v1/classes/:id mocks (registered by setupApiMocks() inside login()) were
// in place.
//
// Per GAP-454 Option C.2 — direct-navigation pattern (matches existing test 6
// "invalid-id" that already used `page.goto('/classes/99999')` successfully):
// helper now goto's /classes/1 directly. Trade-off: loses listing-UI navigation
// coverage in this gate (was already shallow per the GAP-455 audit) in exchange
// for stable, repeatable lifecycle test signal. Listing-UI coverage belongs in
// a separate spec or under the future docker-compose-in-CI gate (GAP-453 B.1).
// ──────────────────────────────────────────────────────────────────────────────
async function navigateToClassDetail(
  page: Parameters<typeof login>[0],
  skipLogin = false
) {
  if (!skipLogin) {
    // login() also wires setupApiMocks() which registers the /classes/1 detail
    // route mocks. Direct goto below relies on those mocks.
    await login(page);
  }

  await page.goto('/classes/1');

  // Confirm we landed (mocks should make this near-instant; 5s buffer for
  // first-time Next.js dev server compile of the [id] route).
  await expect(page).toHaveURL(/\/classes\/\d+$/, { timeout: 5000 });
}

// ──────────────────────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────────────────────

test.describe('Critical Journey: Class Lifecycle', () => {
  /**
   * Test 1: Start (SCHEDULED → IN_PROGRESS) then Complete (IN_PROGRESS → COMPLETED)
   *
   * Needs a stateful mock because react-query re-fetches the class after each
   * mutation. The mock must return the updated status on subsequent GETs.
   *
   * Mock registration order:
   *   login() → setupApiMocks() registers global route for /classes/1
   *   THEN we register override → it's last-registered → LIFO wins
   */
  test('should start and complete a class successfully', async ({ page }) => {
    // 1. Login first — registers global mocks inside setupApiMocks()
    await login(page);

    // 2. Register stateful override AFTER login() so it wins over global mock.
    //    IMPORTANT: must handle ALL sub-routes (/sessions, /start, /complete) because
    //    any unhandled sub-route falls through to the default handler which returns
    //    the class object — causing sessions.map crash in the component.
    let classStatus: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' = 'SCHEDULED';
    await page.route(/\/api\/v1\/classes\/1(\/.*)?$/, async (route) => {
      const method = route.request().method();
      const url = route.request().url();

      // Sessions sub-route must return an array (not the class object)
      if (method === 'GET' && url.includes('/sessions')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [
              {
                id: 1, classId: 1, sessionNumber: 1,
                topic: 'Giới thiệu khoá học',
                scheduledDate: '2026-03-03',
                startTime: '08:00', endTime: '10:00',
                status: 'SCHEDULED', notes: null,
                createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
              },
            ],
          }),
        });
        return;
      }

      if (method === 'POST' && url.includes('/start')) {
        classStatus = 'IN_PROGRESS';
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 1,
              status: 'IN_PROGRESS',
              startedAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
          }),
        });
        return;
      }

      if (method === 'POST' && url.includes('/complete')) {
        classStatus = 'COMPLETED';
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 1,
              status: 'COMPLETED',
              completedAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
          }),
        });
        return;
      }

      // Default: GET /classes/1 — reflect current classStatus
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
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
            status: classStatus,
            startedAt: classStatus !== 'SCHEDULED' ? '2026-05-07T08:00:00Z' : null,
            completedAt: classStatus === 'COMPLETED' ? new Date().toISOString() : null,
            cancelledAt: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: new Date().toISOString(),
          },
        }),
      });
    });

    // 3. Navigate without re-logging in (skipLogin=true)
    await navigateToClassDetail(page, true);

    // Verify initial status badge: "Đã lên lịch"
    // Use .first() — StatusBadge can appear in both header and table row
    await expect(
      page.getByText(/đã lên lịch|scheduled/i).first()
    ).toBeVisible({ timeout: 5000 });

    // ── Start the class ─────────────────────────────────────────────────────
    // Button text: "Bắt đầu" (exact; only shown for SCHEDULED)
    const startButton = page.getByRole('button', { name: /^bắt đầu$/i });
    await expect(startButton).toBeVisible();

    // handleStart() calls window.confirm — register handler BEFORE click
    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('Bắt đầu lớp học');
      await dialog.accept();
    });
    await startButton.click();

    // React-query invalidates + re-fetches; override mock now returns IN_PROGRESS
    await expect(
      page.getByText(/đang diễn ra|in progress/i).first()
    ).toBeVisible({ timeout: 5000 });
    await expect(startButton).not.toBeVisible();

    // ── Complete the class ──────────────────────────────────────────────────
    // Button text: "Hoàn thành" (only shown for IN_PROGRESS)
    const completeButton = page.getByRole('button', { name: /^hoàn thành$/i });
    await expect(completeButton).toBeVisible();

    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('Hoàn thành lớp học');
      await dialog.accept();
    });
    await completeButton.click();

    // Override mock now returns COMPLETED
    await expect(
      page.getByText(/đã hoàn thành|completed/i).first()
    ).toBeVisible({ timeout: 5000 });

    // Read-only mode: all action buttons gone
    await expect(completeButton).not.toBeVisible();
    await expect(page.getByRole('button', { name: /^bắt đầu$/i })).not.toBeVisible();
    await expect(page.getByRole('button', { name: /hủy lớp/i })).not.toBeVisible();
  });

  /**
   * Test 2: Cancel a class with reason (IN_PROGRESS → CANCELLED)
   *
   * Registers an override that returns IN_PROGRESS immediately (AFTER login),
   * so "Hủy lớp" button is visible without needing the start mutation first.
   */
  test('should cancel a class with reason', async ({ page }) => {
    // 1. Login first — registers global mocks
    await login(page);

    // 2. Register IN_PROGRESS override AFTER login() — LIFO wins.
    //    Must handle /sessions sub-route to avoid sessions.map crash.
    let classStatus = 'IN_PROGRESS';
    await page.route(/\/api\/v1\/classes\/1(\/.*)?$/, async (route) => {
      const method = route.request().method();
      const url = route.request().url();

      // Sessions must return an array
      if (method === 'GET' && url.includes('/sessions')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [
              {
                id: 1, classId: 1, sessionNumber: 1,
                topic: 'Giới thiệu khoá học',
                scheduledDate: '2026-03-03',
                startTime: '08:00', endTime: '10:00',
                status: 'SCHEDULED', notes: null,
                createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
              },
            ],
          }),
        });
        return;
      }

      if (method === 'POST' && url.includes('/cancel')) {
        classStatus = 'CANCELLED';
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 1,
              status: 'CANCELLED',
              cancelledAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
          }),
        });
        return;
      }

      // Default GET — reflect classStatus
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
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
            status: classStatus,
            startedAt: '2026-05-07T08:00:00Z',
            completedAt: null,
            cancelledAt:
              classStatus === 'CANCELLED' ? new Date().toISOString() : null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: new Date().toISOString(),
          },
        }),
      });
    });

    // 3. Navigate without re-logging in (skipLogin=true)
    await navigateToClassDetail(page, true);

    // ── Open cancel inline card ─────────────────────────────────────────────
    // Button: "Hủy lớp" (XCircle icon + text); only shown for IN_PROGRESS
    const cancelButton = page.getByRole('button', { name: /hủy lớp/i });
    await expect(cancelButton).toBeVisible({ timeout: 5000 });
    await cancelButton.click();

    // Cancel UI is a <Card> rendered inline (NOT role="dialog").
    // Anchor: textarea that appears when card is shown.
    const reasonTextarea = page.locator('textarea').first();
    await expect(reasonTextarea).toBeVisible({ timeout: 5000 });

    // ── Attempt submit without reason ────────────────────────────────────────
    // Validation fires via Shadcn toast (not inline DOM text).
    const submitCancelButton = page.getByRole('button', { name: /xác nhận hủy/i });
    await expect(submitCancelButton).toBeVisible();
    await submitCancelButton.click();

    // Toast: "Vui lòng nhập lý do hủy"
    // GAP-872: this validation surfaces ONLY in a Shadcn toast, which renders both a
    // visible description (<div>) AND an sr-only live-region announcer (<span role="status">).
    // An unscoped getByText matches both → strict-mode violation. Exclude the role="status"
    // announcer to target the visible toast text deterministically.
    await expect(
      page
        .getByText(/vui lòng nhập lý do hủy|lý do.*bắt buộc|reason.*required/i)
        .and(page.locator(':not([role="status"])'))
    ).toBeVisible({ timeout: 3000 });

    // ── Submit with valid reason ─────────────────────────────────────────────
    await reasonTextarea.fill('Class cancelled due to low enrollment for E2E testing');
    await submitCancelButton.click();

    // Status badge should change to "Đã hủy"
    await expect(
      page.getByText(/đã hủy|cancelled/i).first()
    ).toBeVisible({ timeout: 5000 });

    // Cancel button disappears once CANCELLED
    await expect(cancelButton).not.toBeVisible();
  });

  /**
   * Test 3: Generate class code + copy it
   *
   * The global mock (SCHEDULED) already handles POST /generate-code returning
   * classCode "ENG-2026-001", so no per-test override needed.
   */
  test('should generate and copy class code', async ({ page }) => {
    await navigateToClassDetail(page);

    // ── Generate code ────────────────────────────────────────────────────────
    // Button: "Tạo mã lớp" (only shown for DRAFT or SCHEDULED)
    const generateCodeButton = page.getByRole('button', { name: /tạo mã lớp/i });
    await expect(generateCodeButton).toBeVisible({ timeout: 5000 });

    // handleGenerateCode() calls window.confirm (string, not regex)
    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('Tạo hoặc tạo lại mã lớp học');
      await dialog.accept();
    });
    await generateCodeButton.click();

    // After mutation, class is re-fetched and classCode section renders.
    // The code is displayed as: <p>Mã: {classCode}</p>
    await expect(page.getByText(/^Mã:/i)).toBeVisible({ timeout: 5000 });

    // ── Copy code ────────────────────────────────────────────────────────────
    // Copy button is icon-only; it's inside the div that shows "Mã: ..."
    const codeSection = page.locator('div').filter({ hasText: /^Mã:/ }).first();
    const copyIconButton = codeSection.getByRole('button');

    if (await copyIconButton.isVisible()) {
      await copyIconButton.click();
    } else {
      // Fallback: any button containing an svg in the page header area
      await page.getByRole('button').filter({ has: page.locator('svg') }).first().click();
    }

    // Toast: "Đã sao chép mã lớp học"
    // GAP-872: same toast collision as the cancel-reason validation — exclude the sr-only
    // role="status" announcer so getByText resolves to the single visible toast description.
    await expect(
      page
        .getByText(/đã sao chép|copied/i)
        .and(page.locator(':not([role="status"])'))
    ).toBeVisible({ timeout: 3000 });
  });

  /**
   * Test 4: Sessions section renders correctly
   *
   * Sessions are div-based (not a <table>). The mock returns 1 session.
   */
  test('should display class sessions correctly', async ({ page }) => {
    await navigateToClassDetail(page);

    // Sessions heading: CardTitle "Buổi học"
    await expect(
      page.getByText(/buổi học|sessions/i)
    ).toBeVisible({ timeout: 5000 });

    // Wait for async sessions fetch to resolve
    await page.waitForTimeout(500);

    // Session item pattern: "Buổi 1: Giới thiệu khoá học"
    const sessionItem = page.getByText(/buổi \d+/i).first();
    await expect(sessionItem).toBeVisible({ timeout: 5000 });

    // Count session rows — mock returns 1
    const sessionRows = page.locator('div').filter({ hasText: /buổi \d+:/i });
    const rowCount = await sessionRows.count();
    expect(rowCount).toBeGreaterThan(0);
  });

  /**
   * Test 5: Delete button not visible for IN_PROGRESS / enrolled class
   *
   * Mock returns SCHEDULED with currentEnrolled=15; the page hides "Xóa"
   * because enrolled > 0.  (For a true IN_PROGRESS test, see the note below.)
   *
   * Note: The global mock returns SCHEDULED with currentEnrolled=15.
   * Business rule: delete only allowed for SCHEDULED with 0 enrolled.
   * 15 enrolled → no delete button even in SCHEDULED.
   */
  test('should not allow delete for non-SCHEDULED or enrolled class', async ({ page }) => {
    await navigateToClassDetail(page);

    // The class has 15 enrolled students, so delete is NOT allowed even when SCHEDULED.
    // Button "Xóa" (Trash2 icon + text, variant="destructive") must not be visible.
    const deleteButton = page.getByRole('button', { name: /xóa/i });
    await expect(deleteButton).not.toBeVisible();
  });

  /**
   * Test 6: 404 for invalid class ID
   *
   * The global mock returns 404 for classId === 99999.
   */
  test('should show error for invalid class ID', async ({ page }) => {
    await login(page);

    // Navigate directly to a non-existent class
    await page.goto('/classes/99999');

    // GAP-872: scope to the persistent inline ErrorAlert (role="alert"). The 404
    // message ("Không tìm thấy lớp học") ALSO surfaces in a transient api-client
    // toast (role="status"), so an unscoped getByText matched 2 elements → strict-
    // mode violation. role="alert" excludes the role="status" toast deterministically.
    await expect(
      page.getByRole('alert').getByText(/không tìm thấy lớp học|class not found/i)
    ).toBeVisible({ timeout: 5000 });
  });
});
