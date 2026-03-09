# Phase 2: Teachers Module Tests - Implementation Guide

**Status:** 📋 READY TO START
**Prerequisite:** Phase 1 completed (72% passing rate)
**Estimated Time:** 2-3 hours
**Expected Output:** ~20 integration tests for Teachers module

## Overview

Phase 2 implements integration tests for the Teachers module by reusing patterns established in Phase 1. Focus on List and Create pages only (skip Detail/Edit due to async params issue).

## Quick Start Checklist

### Pre-flight Checks
- [ ] Phase 1 summary reviewed (`phase-1-students-tests-summary.md`)
- [ ] Working directory: `kiteclass/kiteclass-frontend`
- [ ] Branch: Create new branch `feature/phase-2-teachers-tests`
- [ ] Test environment: `pnpm test` runs successfully

### Implementation Steps
1. [ ] Create test directory structure
2. [ ] Copy and adapt students tests
3. [ ] Update MSW handlers with teacher data
4. [ ] Run tests and fix issues
5. [ ] Document results
6. [ ] Commit and create PR

## File Structure

Create these files:

```
src/app/(dashboard)/teachers/
├── __tests__/
│   └── teachers-list.integration.test.tsx  (10 tests)
└── new/__tests__/
    └── teachers-new.integration.test.tsx   (9 tests)
```

**Note:** Skip `[id]` and `[id]/edit` tests (async params issue from Phase 1)

## Step-by-Step Implementation

### Step 1: Setup Test Files (15 min)

#### 1.1 Create Directories
```bash
cd kiteclass/kiteclass-frontend
mkdir -p src/app/\(dashboard\)/teachers/__tests__
mkdir -p src/app/\(dashboard\)/teachers/new/__tests__
```

#### 1.2 Copy Base Test Files
```bash
# Copy students list test as template
cp src/app/\(dashboard\)/students/__tests__/students-list.integration.test.tsx \
   src/app/\(dashboard\)/teachers/__tests__/teachers-list.integration.test.tsx

# Copy students new test as template
cp src/app/\(dashboard\)/students/new/__tests__/students-new.integration.test.tsx \
   src/app/\(dashboard\)/teachers/new/__tests__/teachers-new.integration.test.tsx
```

### Step 2: Update Teachers List Tests (30 min)

#### 2.1 Search and Replace

In `teachers-list.integration.test.tsx`, replace:
- `StudentsPage` → `TeachersPage`
- `students` → `teachers` (lowercase)
- `Students` → `Teachers` (capitalized)
- `Học viên` → `Giảng viên`
- `học viên` → `giảng viên`
- Import path: `../page` (same structure)

#### 2.2 Update Mock Data

Replace student mock data with teacher data:
```typescript
// Example teacher data for tests
{
  id: 1,
  name: 'Nguyễn Thị Giáo',
  email: 'giao.nguyen@kiteclass.local',
  phone: '0901234567',
  specialization: 'Toán học',
  status: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}
```

#### 2.3 Update Test Assertions

Key differences from students:
- Check `specialization` field instead of `dateOfBirth`
- Status options: `ACTIVE`, `INACTIVE`, `ON_LEAVE` (different from students)
- Search includes specialization field

### Step 3: Update Teachers Create Tests (30 min)

#### 3.1 Update Form Fields

Teachers form has different fields than students:
```typescript
// Teacher form labels (check actual TeacherForm component)
const TEACHER_FORM_LABELS = {
  name: /tên giảng viên/i,
  email: /email/i,
  phone: /số điện thoại/i,
  specialization: /chuyên môn/i,
  // ... other fields
};
```

#### 3.2 Update Validation Tests

Check `TeacherForm` schema for actual validation messages:
```typescript
// Expected validation errors (verify in actual schema)
- name: "Tên không được để trống"
- email: "Email không hợp lệ"
- specialization: "Chuyên môn là bắt buộc" (if required)
```

#### 3.3 Update Success Scenarios

Toast messages for teachers:
- Create: "Đã tạo giảng viên mới"
- Update: "Đã cập nhật thông tin giảng viên"
- Delete: "Đã xóa giảng viên"

(Verify these in `use-teachers.ts` hook)

### Step 4: Update MSW Handlers (15 min)

#### 4.1 Add Teacher Mock Handlers

In `src/mocks/handlers.ts`, add teachers endpoints following students pattern:

```typescript
// Teachers API - List
http.get(`${BASE_URL}/api/v1/teachers`, ({ request }) => {
  const url = new URL(request.url);
  const page = parseInt(url.searchParams.get('page') || '0');
  const size = parseInt(url.searchParams.get('size') || '20');

  return HttpResponse.json({
    success: true,
    data: {
      content: [
        {
          id: 1,
          name: 'Nguyễn Thị Giáo',
          email: 'giao.nguyen@kiteclass.local',
          phone: '0901234567',
          specialization: 'Toán học',
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
        {
          id: 2,
          name: 'Trần Văn Học',
          email: 'hoc.tran@kiteclass.local',
          phone: '0901234568',
          specialization: 'Văn học',
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ],
      totalElements: 2,
      totalPages: 1,
      size,
      number: page,
    },
  });
}),

// Teachers API - Create
http.post(`${BASE_URL}/api/v1/teachers`, async ({ request }) => {
  const body = await request.json() as Record<string, unknown>;
  return HttpResponse.json({
    success: true,
    data: {
      id: 3,
      ...body,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
  }, { status: 201 });
}),

// Teachers API - Delete
http.delete(`${BASE_URL}/api/v1/teachers/:id`, () => {
  return HttpResponse.json({ success: true }, { status: 204 });
}),
```

**Important:** Maintain `{success: true, data: {...}}` wrapper format!

### Step 5: Run and Fix Tests (45 min)

#### 5.1 Run Tests
```bash
pnpm test src/app/\(dashboard\)/teachers --run
```

#### 5.2 Common Issues and Fixes

Based on Phase 1 learnings:

**Issue 1: Form field labels don't match**
- **Solution:** Check actual labels in `TeacherForm` component
- Update test selectors to match actual labels

**Issue 2: Toast messages timeout**
- **Solution:** Verify toast text in `use-teachers.ts` hook
- Match exact toast messages in tests

**Issue 3: Navigation mock missing usePathname**
- **Solution:** Already fixed in Phase 1, just ensure mock includes:
  ```typescript
  vi.mock('next/navigation', () => ({
    useRouter: vi.fn(),
    usePathname: vi.fn(() => '/teachers/new'),
  }));
  ```

**Issue 4: Delete button not found**
- **Solution:** Use icon button selector from Phase 1:
  ```typescript
  const allButtons = screen.getAllByRole('button');
  const iconButtons = allButtons.filter(btn => !btn.textContent);
  const firstDeleteButton = iconButtons[2]; // 3rd icon button
  ```

#### 5.3 Expected Test Results

Target: **~15-18 passing tests** (similar to Phase 1)
- List tests: 7-8 passing (skip pagination if problematic)
- Create tests: 8-10 passing

### Step 6: Document Results (15 min)

Create `phase-2-teachers-tests-summary.md`:

```markdown
# Phase 2: Teachers Module Tests - Summary

**Date:** [Current Date]
**Status:** ✅ COMPLETED
**Total Tests:** [X tests] ([Y] passing, [Z] failing, 20 skipped)

## Test Results

### ✅ Passing Tests ([Y]/[X])
- List [details]
- Create [details]

### ❌ Failing Tests ([Z]/[X])
- [List with reasons]

## Learnings
- [What worked well]
- [What was different from students]
- [Issues encountered]

## Next Steps
- Ready for Phase 3 (Courses)
```

### Step 7: Commit and PR (10 min)

```bash
# Add all changes
git add -A

# Commit
git commit -m "test(frontend): add teachers integration tests

Changes:
- Add teachers-list integration tests (X)
- Add teachers-new integration tests (Y)
- Update MSW handlers for teachers API
- Document Phase 2 results
"

# Push and create PR
git push origin feature/phase-2-teachers-tests
gh pr create --title "Phase 2: Teachers Module Integration Tests" \
  --body "Implements integration tests for Teachers module following Phase 1 patterns"
```

## Reference Files

### Must Read Before Starting
1. `phase-1-students-tests-summary.md` - Patterns and learnings
2. `src/test/page-test-utils.tsx` - Shared test helpers
3. `src/app/(dashboard)/students/__tests__/students-list.integration.test.tsx` - Template for list tests
4. `src/app/(dashboard)/students/new/__tests__/students-new.integration.test.tsx` - Template for create tests

### Actual Component Files to Check
1. `src/app/(dashboard)/teachers/page.tsx` - List page implementation
2. `src/app/(dashboard)/teachers/new/page.tsx` - Create page implementation
3. `src/components/forms/teacher-form.tsx` - Form fields and validation
4. `src/hooks/use-teachers.ts` - API hooks and toast messages
5. `src/components/tables/columns/teacher-columns.tsx` - Table column definitions

## Key Patterns from Phase 1

### 1. Test Structure
```typescript
describe('TeachersListPage Integration', () => {
  beforeEach(() => {
    window.confirm = vi.fn();
  });

  it('should [behavior]', async () => {
    render(<TeachersPage />);
    await waitFor(() => {
      expect(screen.getByText('Expected Text')).toBeInTheDocument();
    });
  });
});
```

### 2. MSW Mock Pattern
```typescript
server.use(
  http.get('*/api/v1/teachers', () => {
    return HttpResponse.json({
      success: true,  // ← Required wrapper
      data: {         // ← Data inside wrapper
        content: [...],
        totalElements: X,
        totalPages: Y,
        size: Z,
        number: N,
      },
    });
  })
);
```

### 3. Navigation Mock Pattern
```typescript
vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/current/path'),
}));

// In test setup
const mockPush = vi.fn();
vi.mocked(useRouter).mockReturnValue({
  push: mockPush,
  // ... other router methods
} as any);
```

### 4. Delete Button Selection
```typescript
// Icon buttons (no text) - for table actions
const allButtons = screen.getAllByRole('button');
const iconButtons = allButtons.filter(btn => !btn.textContent);
const deleteButton = iconButtons[2]; // View, Edit, Delete (3rd)
```

### 5. Form Field Labels
```typescript
// Always check actual component for labels
screen.getByLabelText(/tên giảng viên/i)
screen.getByLabelText(/email/i)
screen.getByLabelText(/chuyên môn/i)
```

## Known Issues from Phase 1

### Skip These Tests
❌ **Detail page tests** - Next.js 15 `use(params)` incompatible with RTL
❌ **Edit page tests** - Same async params issue
❌ **Pagination tests** - If timeout persists (MSW configuration)
❌ **"Not delete when cancelled"** - Test pollution (passes solo)

### Working Test Categories
✅ **List page**: Load, search, delete, empty state, error handling
✅ **Create page**: Render, create, validation, errors, API errors

## Troubleshooting

### Tests Won't Run
```bash
# Check you're in correct directory
pwd  # Should be: .../kiteclass/kiteclass-frontend

# Verify pnpm works
pnpm --version

# Run single test to debug
pnpm test src/app/\(dashboard\)/teachers/__tests__ -t "should load" --run
```

### Tests Timeout
- Check MSW handlers have correct format (`{success, data}` wrapper)
- Verify toast messages match actual hooks
- Ensure form labels match actual components
- Check console for React errors

### Can't Find Elements
- Use `screen.debug()` to see rendered HTML
- Check actual component for correct text/labels
- Verify data is loaded (check network mocks)

## Success Criteria

Phase 2 is complete when:
- [ ] 15+ tests passing (>70% passing rate)
- [ ] All list tests working (except pagination if problematic)
- [ ] All create tests working
- [ ] MSW handlers configured correctly
- [ ] Documentation updated
- [ ] PR created and passing CI

## Time Estimates

| Task | Time | Notes |
|------|------|-------|
| Setup files | 15 min | Copy and organize |
| List tests | 30 min | Search/replace + fixes |
| Create tests | 30 min | Update fields + validation |
| MSW handlers | 15 min | Add teacher endpoints |
| Run + fix tests | 45 min | Debug and iterate |
| Documentation | 15 min | Summary + learnings |
| Commit + PR | 10 min | Git workflow |
| **Total** | **2.5 hours** | Can be faster with experience |

## Next Steps After Phase 2

Once Phase 2 is complete:
1. Review Phase 2 results
2. Update master testing plan progress
3. Proceed to Phase 3 (Courses) - includes lifecycle tests
4. Or proceed to Phase 4 (Classes) - most complex module

---

**Questions?** Refer to Phase 1 summary or check actual component implementations.

**Good luck!** 🚀
