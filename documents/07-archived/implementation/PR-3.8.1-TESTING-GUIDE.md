# PR 3.8.1: Testing Guide

This guide provides instructions for completing Tasks #17 (Component Tests) and #18 (E2E Tests).

---

## Task #17: Component Tests

### Setup

Tests should be placed in `src/components/attendance/__tests__/` directory.

### Testing Framework

- **Vitest** - Test runner
- **Testing Library** - React component testing
- **Coverage Target**: 80%+

### Example: AttendanceStatsOverview Test

Create `src/components/attendance/__tests__/attendance-stats-overview.test.tsx`:

```typescript
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AttendanceStatsOverview } from '../attendance-stats-overview';

describe('AttendanceStatsOverview', () => {
  const mockStats = {
    targetId: 1,
    targetType: 'STUDENT' as const,
    totalSessions: 40,
    presentCount: 34,
    absentCount: 3,
    lateCount: 2,
    excusedCount: 1,
    makeupCount: 0,
    attendanceRate: 85.0,
  };

  it('renders stats correctly', () => {
    render(<AttendanceStatsOverview stats={mockStats} />);

    expect(screen.getByText('85.0%')).toBeInTheDocument();
    expect(screen.getByText('40')).toBeInTheDocument(); // total sessions
    expect(screen.getByText('34')).toBeInTheDocument(); // present
  });

  it('shows progress bar when showProgress is true', () => {
    render(<AttendanceStatsOverview stats={mockStats} showProgress={true} />);

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('hides progress bar when showProgress is false', () => {
    render(<AttendanceStatsOverview stats={mockStats} showProgress={false} />);

    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('applies correct color based on attendance rate', () => {
    const { rerender } = render(
      <AttendanceStatsOverview stats={{ ...mockStats, attendanceRate: 95 }} />
    );
    expect(screen.getByText('95.0%')).toHaveClass('text-green-600');

    rerender(
      <AttendanceStatsOverview stats={{ ...mockStats, attendanceRate: 80 }} />
    );
    expect(screen.getByText('80.0%')).toHaveClass('text-yellow-600');

    rerender(
      <AttendanceStatsOverview stats={{ ...mockStats, attendanceRate: 60 }} />
    );
    expect(screen.getByText('60.0%')).toHaveClass('text-red-600');
  });
});
```

### Test Files to Create

1. **attendance-stats-overview.test.tsx**
   - Test rendering with valid stats
   - Test progress bar visibility
   - Test color coding based on rate
   - Test compact vs default variant

2. **pending-attendance-badge.test.tsx**
   - Test badge renders with count
   - Test badge hidden when count is 0
   - Test compact variant
   - Test icon visibility

3. **enhanced-attendance-calendar.test.tsx**
   - Test calendar grid renders
   - Test month navigation
   - Test date click handler
   - Test status filter
   - Test tooltips
   - Test empty state

4. **today-classes-widget.test.tsx**
   - Test renders session list
   - Test pending badge count
   - Test loading state
   - Test empty state
   - Test navigation links

5. **attendance-detail-dialog.test.tsx**
   - Test dialog opens/closes
   - Test records display
   - Test stats summary
   - Test grouping by session

6. **attendance-history-table.test.tsx**
   - Test table renders data
   - Test empty state
   - Test loading state
   - Test pagination
   - Test column rendering

7. **class-stats-table.test.tsx**
   - Test table renders
   - Test sorting
   - Test summary cards
   - Test empty state

8. **attendance-trends-chart.test.tsx**
   - Test SVG renders
   - Test data points
   - Test empty state
   - Test axis labels

### Running Tests

```bash
cd kiteclass/kiteclass-frontend
pnpm test
```

### Coverage Report

```bash
pnpm test:coverage
```

---

## Task #18: E2E Tests

### Setup

Create `e2e/attendance-enhancements.spec.ts` in the frontend directory.

### Testing Framework

- **Playwright** - E2E testing

### Example: E2E Test Suite

Create `e2e/attendance-enhancements.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test.describe('Attendance Enhancements', () => {
  test.beforeEach(async ({ page }) => {
    // Login as teacher/admin
    await page.goto('/login');
    await page.fill('input[name="email"]', 'teacher@example.com');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('Student views attendance history', async ({ page }) => {
    // Navigate to student list
    await page.goto('/students');

    // Click first student
    await page.click('[data-testid="student-row"]:first-child');

    // Click attendance tab/link
    await page.click('a[href*="/attendance"]');

    // Verify attendance page loaded
    await expect(page.locator('h1')).toContainText('Lịch sử điểm danh');

    // Verify stats cards visible
    await expect(page.locator('text=Tổng số')).toBeVisible();
    await expect(page.locator('text=Có mặt')).toBeVisible();

    // Verify calendar rendered
    await expect(page.locator('.grid-cols-7')).toBeVisible();

    // Click a calendar date with attendance
    await page.click('button:has-text("15")'); // Click day 15

    // Verify detail dialog opens
    await expect(page.locator('role=dialog')).toBeVisible();
    await expect(page.locator('text=Chi tiết điểm danh')).toBeVisible();
  });

  test('Admin views statistics and exports CSV', async ({ page }) => {
    // Navigate to admin stats
    await page.goto('/admin/attendance/stats');

    // Verify page loaded
    await expect(page.locator('h1')).toContainText('Thống kê điểm danh');

    // Verify stats cards
    await expect(page.locator('text=Tổng lớp học')).toBeVisible();
    await expect(page.locator('text=Tổng buổi học')).toBeVisible();

    // Change date range
    await page.fill('input[type="date"]:first', '2026-02-01');
    await page.fill('input[type="date"]:last', '2026-02-28');

    // Wait for data to reload
    await page.waitForTimeout(1000);

    // Click export CSV
    const downloadPromise = page.waitForEvent('download');
    await page.click('button:has-text("Xuất báo cáo CSV")');
    const download = await downloadPromise;

    // Verify download
    expect(download.suggestedFilename()).toContain('thong-ke-diem-danh');
    expect(download.suggestedFilename()).toContain('.csv');
  });

  test('Teacher marks attendance from dashboard', async ({ page }) => {
    // Navigate to teacher dashboard
    await page.goto('/teacher/dashboard');

    // Verify dashboard loaded
    await expect(page.locator('h1')).toContainText('Giáo viên');

    // Verify today's classes widget
    await expect(page.locator('text=Lớp học hôm nay')).toBeVisible();

    // Check if pending badge exists
    const pendingBadge = page.locator('text=Chưa điểm danh');
    if (await pendingBadge.isVisible()) {
      // Click "Mark Now" button for first pending class
      await page.click('button:has-text("Điểm danh")');

      // Verify navigated to attendance form
      await expect(page).toHaveURL(/\/classes\/\d+\/attendance/);
    }
  });

  test('Calendar interaction opens detail dialog', async ({ page }) => {
    // Navigate to student attendance page
    await page.goto('/students/1/attendance');

    // Wait for calendar to load
    await page.waitForSelector('.grid-cols-7');

    // Find and click a date with attendance (green background)
    const dateWithAttendance = page.locator('button.bg-green-100').first();
    await dateWithAttendance.click();

    // Verify dialog opened
    await expect(page.locator('role=dialog')).toBeVisible();

    // Verify dialog shows attendance records
    await expect(page.locator('text=Chi tiết điểm danh')).toBeVisible();
    await expect(page.locator('text=Tổng')).toBeVisible();

    // Close dialog
    await page.keyboard.press('Escape');

    // Verify dialog closed
    await expect(page.locator('role=dialog')).not.toBeVisible();
  });

  test('Responsive design works on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });

    // Navigate to admin stats
    await page.goto('/admin/attendance/stats');

    // Verify page is responsive
    await expect(page.locator('h1')).toBeVisible();

    // Verify cards stack vertically
    const cards = page.locator('[class*="grid"]').first();
    await expect(cards).toBeVisible();
  });
});
```

### Running E2E Tests

```bash
cd kiteclass/kiteclass-frontend
pnpm exec playwright test e2e/attendance-enhancements.spec.ts
```

### Headed Mode (See Browser)

```bash
pnpm exec playwright test e2e/attendance-enhancements.spec.ts --headed
```

### Debug Mode

```bash
pnpm exec playwright test e2e/attendance-enhancements.spec.ts --debug
```

---

## Test Data Setup

### Mock Data for Tests

Create `src/__tests__/fixtures/attendance.ts`:

```typescript
export const mockAttendanceStats = {
  targetId: 1,
  targetType: 'STUDENT' as const,
  totalSessions: 40,
  presentCount: 34,
  absentCount: 3,
  lateCount: 2,
  excusedCount: 1,
  makeupCount: 0,
  attendanceRate: 85.0,
};

export const mockAttendanceRecords = [
  {
    id: 1,
    enrollmentId: 1,
    studentName: 'Nguyễn Văn A',
    sessionId: 1,
    sessionNumber: 1,
    status: 'PRESENT' as const,
    markedDate: '2026-03-01T09:00:00Z',
    markedBy: 1,
    markedByName: 'GV Trần B',
    notes: '',
    pointsAwarded: 10,
    createdAt: '2026-03-01T09:00:00Z',
    updatedAt: '2026-03-01T09:00:00Z',
  },
  // Add more records...
];

export const mockTodayClassSessions = [
  {
    sessionId: 1,
    sessionNumber: 5,
    classId: 1,
    className: 'Toán Lớp 10A',
    startTime: '2026-03-08T09:00:00Z',
    endTime: '2026-03-08T10:30:00Z',
    totalStudents: 30,
    attendanceMarked: false,
  },
  // Add more sessions...
];
```

---

## Coverage Goals

### Component Test Coverage

- **Statements**: 80%+
- **Branches**: 75%+
- **Functions**: 80%+
- **Lines**: 80%+

### E2E Test Coverage

- **Critical Flows**: 100% (all 4 flows)
- **User Roles**: Teacher, Admin, Student
- **Browsers**: Chrome, Firefox, Safari
- **Devices**: Desktop, Tablet, Mobile

---

## Continuous Integration

### GitHub Actions Workflow

Add to `.github/workflows/frontend-ci.yml`:

```yaml
- name: Run Frontend Tests
  run: |
    cd kiteclass/kiteclass-frontend
    pnpm test:coverage

- name: Run E2E Tests
  run: |
    cd kiteclass/kiteclass-frontend
    pnpm exec playwright test

- name: Upload Coverage
  uses: codecov/codecov-action@v3
  with:
    files: ./kiteclass/kiteclass-frontend/coverage/lcov.info
```

---

## Debugging Tips

### Component Tests

1. Use `screen.debug()` to see rendered HTML
2. Use `screen.logTestingPlaygroundURL()` to get selector suggestions
3. Use `waitFor()` for async operations
4. Use `findBy*` queries for elements that appear asynchronously

### E2E Tests

1. Use `await page.pause()` to pause execution
2. Use `--debug` flag to open inspector
3. Use `trace: 'on'` in playwright.config.ts to record traces
4. Check `test-results/` for screenshots on failure

---

## Next Steps

1. **Create Component Tests** - 8 test files
2. **Create E2E Tests** - 1 test file with 5 test cases
3. **Run Tests Locally** - Verify all pass
4. **Check Coverage** - Ensure 80%+ coverage
5. **Fix Any Failures** - Debug and resolve
6. **Commit Tests** - Add to PR

---

## Estimated Effort

- **Component Tests**: 4-6 hours
- **E2E Tests**: 2-3 hours
- **Debugging & Fixes**: 1-2 hours
- **Total**: 7-11 hours (1-2 days)

---

## Resources

- [Vitest Documentation](https://vitest.dev/)
- [Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [Playwright Documentation](https://playwright.dev/)
- [Testing Best Practices](https://kentcdodds.com/blog/common-mistakes-with-react-testing-library)
