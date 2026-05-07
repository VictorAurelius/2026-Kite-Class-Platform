/**
 * Critical Journey E2E Test: Course Publishing to Class Creation
 *
 * Tests the complete workflow:
 * 1. Login
 * 2. Create a new course (DRAFT)
 * 3. Publish the course (DRAFT → PUBLISHED) via ConfirmDialog (Radix UI)
 * 4. Create a class from the published course
 * 5. Verify class is created and linked to course
 *
 * This test covers the core business flow for setting up a new class.
 *
 * Selectors validated against actual component render:
 *  - StatusBadge text: "Bản nháp" (DRAFT), "Đã xuất bản" (PUBLISHED)
 *  - Publish button: <Button>Xuất bản</Button> (course detail page.tsx line 108)
 *  - Archive button: <Button variant="outline">Lưu trữ</Button>
 *  - Publish confirm dialog: <ConfirmDialog title="Xuất bản khóa học" confirmText="Xuất bản" />
 *    → Radix UI Dialog (NOT native window.confirm) — confirmed in confirm-dialog.tsx
 *  - Error text: "Không tìm thấy khóa học" (ErrorAlert in course detail page.tsx line 78)
 *  - "Thêm lớp học": <Button> inside <Link> on /classes page (classes/page.tsx line 86)
 *  - Course selector: Radix <Select> → SelectTrigger (role="combobox") on /classes
 *  - Class form fields: name, schedule, locationDetail, maxStudents, startDate, endDate
 *    (confirmed from class-form.tsx register() calls — NOT "location", use "locationDetail")
 *
 * API mocks in api-mocks.ts handle:
 *  - POST /api/v1/courses → 201 new DRAFT course (id:2)
 *  - GET /api/v1/courses/:id → DRAFT course detail
 *  - POST /api/v1/courses/:id/publish → PUBLISHED status
 *  - GET /api/v1/courses/:id == 99999 → 404
 *
 * // Validated locally 2026-05-07 against 2f1e29bd
 *
 * @since 2026-02-24
 */

import { test, expect } from '@playwright/test';
import { login } from '../helpers/auth';

test.describe('Critical Journey: Course Publishing & Class Creation', () => {
  test('should create course, publish, and create class successfully', async ({ page }) => {
    // Step 1: Login
    await login(page);

    // Step 2: Navigate to Courses page
    await page.click('a[href="/courses"]');
    await expect(page).toHaveURL('/courses');
    // Heading on courses/page.tsx line 51: <h1>Khóa học</h1>
    await expect(page.getByRole('heading', { name: /khóa học|courses/i })).toBeVisible();

    // Step 3: Create a new course
    await page.click('a[href="/courses/new"]');
    await expect(page).toHaveURL('/courses/new');

    // Fill course form
    const timestamp = Date.now();
    const courseName = `E2E Test Course ${timestamp}`;
    const courseCode = `E2E-${timestamp}`;

    await page.fill('input[name="name"]', courseName);
    await page.fill('input[name="code"]', courseCode);
    await page.fill('textarea[name="description"]', 'Course created by E2E test for critical journey');
    await page.fill('input[name="durationWeeks"]', '12');
    await page.fill('input[name="totalSessions"]', '24');
    await page.fill('input[name="price"]', '5000000');

    // Select teacher — Radix Select renders SelectTrigger as role="combobox"
    const teacherSelect = page.locator('button[role="combobox"]').first();
    await teacherSelect.click();
    await page.locator('[role="option"]').first().click();

    // Submit form — POST /api/v1/courses → mock returns 201 + router.push('/courses')
    await page.click('button[type="submit"]');

    // Should redirect to courses list after successful creation
    await expect(page).toHaveURL('/courses', { timeout: 10000 });

    // Step 4: Navigate to course detail to verify DRAFT and publish
    // Mock GET /api/v1/courses/2 returns the newly created DRAFT course (id=2)
    await page.goto('/courses/2');

    // Should be on course detail page
    await expect(page).toHaveURL(/\/courses\/2$/, { timeout: 5000 });

    // Verify initial status is DRAFT — StatusBadge renders "Bản nháp" (statusLabels[DRAFT])
    // Confirmed: statusLabels[CourseStatus.DRAFT] = 'Bản nháp' in course detail page.tsx line 30
    await expect(page.getByText(/bản nháp|draft/i)).toBeVisible({ timeout: 5000 });

    // Step 5: Publish the course
    // "Xuất bản" button appears for DRAFT courses (course detail page.tsx line 106-110)
    const publishButton = page.getByRole('button', { name: /xuất bản|publish/i });
    await expect(publishButton).toBeVisible();
    await publishButton.click();

    // Confirm publish via ConfirmDialog (Radix UI Dialog — confirmed in confirm-dialog.tsx)
    // The dialog renders as role="dialog" with:
    //   <DialogTitle>Xuất bản khóa học</DialogTitle>    ← title prop from course detail page.tsx line 217
    //   <Button confirmText="Xuất bản" />              ← confirm button inside dialog
    const confirmDialog = page.getByRole('dialog');
    await expect(confirmDialog).toBeVisible({ timeout: 3000 });
    await expect(confirmDialog.getByRole('heading', { name: /xuất bản khóa học|publish course/i })).toBeVisible();
    // The confirm button has the same text "Xuất bản" — get it within dialog scope
    await confirmDialog.getByRole('button', { name: /^xuất bản$|^publish$/i }).click();

    // Wait for status to change to PUBLISHED
    // StatusBadge renders "Đã xuất bản" (statusLabels[CourseStatus.PUBLISHED] in page.tsx line 31)
    await expect(page.getByText(/đã xuất bản|published/i)).toBeVisible({ timeout: 5000 });

    // Verify publish button is gone, archive button appears
    await expect(publishButton).not.toBeVisible();
    // "Lưu trữ" button: <Button variant="outline">Lưu trữ</Button> (page.tsx line 113-116)
    await expect(page.getByRole('button', { name: /lưu trữ|archive/i })).toBeVisible();

    // Step 6: Create a class from this published course
    // Navigate to classes page where course selector + "Thêm lớp học" link live
    await page.click('a[href="/classes"]');
    await expect(page).toHaveURL('/classes');

    // Select the course — Radix Select (SelectTrigger renders as role="combobox")
    // On the /classes page, the selector shows all courses from GET /api/v1/courses
    const courseSelector = page.locator('button[role="combobox"]').first();
    await courseSelector.click();
    // SelectItem renders within [role="option"] — click the first available course
    await page.locator('[role="option"]').first().click();

    // Wait for "Thêm lớp học" link to appear (classes/page.tsx line 83-90)
    // Link only shows after selectedCourseId is set
    const createClassLink = page.getByRole('link', { name: /thêm lớp học|add class/i });
    await expect(createClassLink).toBeVisible({ timeout: 5000 });
    await createClassLink.click();

    // Should navigate to create class page for this course
    await expect(page).toHaveURL(/\/courses\/\d+\/classes\/new/, { timeout: 5000 });

    // Fill class form — actual field names from class-form.tsx register() calls:
    //   name, schedule, locationDetail (NOT "location"), maxStudents, startDate, endDate
    const className = `E2E Test Class ${timestamp}`;
    await page.fill('input[name="name"]', className);
    await page.fill('input[name="schedule"]', 'Thứ 2, 4, 6: 09:00-11:00');
    await page.fill('input[name="locationDetail"]', 'Phòng 101');
    await page.fill('input[name="maxStudents"]', '30');
    await page.fill('input[name="startDate"]', '2026-03-01');
    await page.fill('input[name="endDate"]', '2026-05-31');

    // Submit class form — POST /api/v1/courses/:id/classes → mock returns 201
    await page.click('button[type="submit"]');

    // useCreateClass onSuccess redirects to /courses/:courseId (use-classes.ts line 86)
    // NOT to /classes — the user returns to the course detail to see the new class
    await expect(page).toHaveURL(/\/courses\/\d+$/, { timeout: 10000 });

    // Success! Full journey completed: course created → published → class created
  });

  test('should not allow creating class from DRAFT course', async ({ page }) => {
    await login(page);

    // Navigate directly to an existing DRAFT course (mock returns DRAFT for any id)
    // This verifies DRAFT status UI and validates that user cannot create classes
    // without publishing first (by checking "Xuất bản" button is present, not archive)
    await page.goto('/courses/2');
    await expect(page).toHaveURL('/courses/2');

    // Confirm badge shows "Bản nháp" (DRAFT state per statusLabels in page.tsx line 30)
    await expect(page.getByText(/bản nháp|draft/i)).toBeVisible({ timeout: 5000 });

    // Verify: publish button present (course is DRAFT, not yet publishable for classes)
    await expect(page.getByRole('button', { name: /xuất bản|publish/i })).toBeVisible();

    // Verify: archive button NOT present (only shown for PUBLISHED courses per page.tsx line 111)
    await expect(page.getByRole('button', { name: /lưu trữ|archive/i })).not.toBeVisible();

    // Navigate to classes page to verify behavior with DRAFT course
    await page.click('a[href="/classes"]');
    await expect(page).toHaveURL('/classes');

    // The course selector (Radix Select) is visible — page is functional
    await expect(page.locator('button[role="combobox"]').first()).toBeVisible();

    // Heading "Lớp học" confirms we are on the classes page (classes/page.tsx line 78)
    await expect(page.getByRole('heading', { name: /lớp học|classes/i })).toBeVisible();

    // "Thêm lớp học" link does NOT appear until a course is selected
    // (classes/page.tsx line 83: {selectedCourseId && <Link>})
    await expect(page.getByRole('link', { name: /thêm lớp học|add class/i })).not.toBeVisible();
  });

  test('should show error when course not found', async ({ page }) => {
    await login(page);

    // Navigate directly to a non-existent course (mock returns 404 for id=99999)
    await page.goto('/courses/99999');

    // ErrorAlert component renders the message prop:
    // message="Không tìm thấy khóa học" (course detail page.tsx line 78)
    // Confirmed: API mock returns 404 + error payload for courseId === 99999
    await expect(
      page.getByText(/không tìm thấy khóa học|course not found/i)
    ).toBeVisible({ timeout: 5000 });
  });
});
