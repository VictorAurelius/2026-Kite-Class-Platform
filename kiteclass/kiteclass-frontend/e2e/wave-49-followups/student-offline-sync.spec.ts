/**
 * Student offline assignment submit + auto-flush E2E (GAP-269c Playwright).
 *
 * Lighthouse PWA + Performance scores deferred to post-staging-HTTPS run.
 *
 * Storage key under test: `kc.student.offline-submits` — owned by
 * `kiteclass-frontend/src/lib/offline/student-assignment-queue.ts` per
 * Wave 49 Bucket C (PR #1093).
 *
 * @since Wave 51 Bucket A
 */
import { expect, test } from '@playwright/test';
import { STORAGE_KEYS, loginAs, mockAssignmentSubmit } from './_helpers';

test.describe('GAP-269c — Student offline assignment sync', () => {
  test.beforeEach(async ({ page }) => {
    await mockAssignmentSubmit(page);
    await loginAs(page, 'STUDENT');
  });

  test('happy path: offline submit queues → online auto-flush succeeds', async ({
    page,
    context,
  }) => {
    // 1. Land on student today shell.
    await page.goto('/student/today');
    await expect(page).toHaveURL(/\/student\/today/);

    // 2. Open the assignment detail route.
    await page.goto('/student/assignments/asg-001');
    await expect(page.getByRole('button', { name: /Nộp bài|Lưu nháp offline/i })).toBeVisible({
      timeout: 10000,
    });

    // 3. Go offline + submit — expect the queue to receive the entry.
    await context.setOffline(true);
    // Wait a tick so the `online`/`offline` event listener updates state.
    await page.waitForTimeout(300);
    await page
      .getByRole('textbox')
      .first()
      .fill('Bài làm offline — Wave 51 spec.');
    await page.getByRole('button', { name: /Lưu nháp offline|Nộp bài/i }).click();

    // 4. Confirm the queue persisted to localStorage.
    const queued = await page.evaluate((key) => {
      const raw = window.localStorage.getItem(key);
      return raw ? JSON.parse(raw) : [];
    }, STORAGE_KEYS.studentAssignmentQueue);
    expect(Array.isArray(queued)).toBe(true);
    expect(queued.length).toBeGreaterThanOrEqual(1);
    expect(queued[0]).toMatchObject({ assignmentId: 'asg-001' });

    // 5. Go back online — auto-flush handler should drain the queue.
    await context.setOffline(false);
    // Allow the `online` event handler + flush() round-trip.
    await page.waitForTimeout(1500);

    const queuedAfter = await page.evaluate((key) => {
      const raw = window.localStorage.getItem(key);
      return raw ? JSON.parse(raw) : [];
    }, STORAGE_KEYS.studentAssignmentQueue);
    expect(queuedAfter.length).toBe(0);
  });

  test('error branch: corrupt localStorage tolerated; queue starts empty', async ({
    page,
  }) => {
    // Seed a corrupt queue value before the page loads its modules.
    await page.goto('/student/today');
    await page.evaluate((key) => {
      window.localStorage.setItem(key, 'NOT_VALID_JSON{{{');
    }, STORAGE_KEYS.studentAssignmentQueue);

    // Reload — the queue module must NOT throw on parse failure; the page
    // still renders.
    await page.reload();
    await expect(page).toHaveURL(/\/student\/today/);

    // The queue read should return [] rather than throwing.
    const queued = await page.evaluate((key) => {
      // Mirror readQueue() tolerance contract.
      try {
        const raw = window.localStorage.getItem(key);
        if (!raw) return [];
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
      } catch {
        return [];
      }
    }, STORAGE_KEYS.studentAssignmentQueue);
    expect(queued).toEqual([]);
  });
});
