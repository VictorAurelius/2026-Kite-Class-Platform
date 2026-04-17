# PR 3.8: Frontend Testing & Coverage - Implementation Status

**Created:** 2026-02-22
**Branch:** `feature/PR-3.8-frontend-testing`
**Target:** Achieve ≥80% test coverage for ALL frontend PRs (3.1-3.7)
**Current Status:** 🟡 IN PROGRESS (Part 1/4 Complete)

---

## 🎯 Objective

Viết tests cho TẤT CẢ components/pages đã implement từ PR 3.1 đến PR 3.7:
- ✅ PR 3.1: Infrastructure (types, api-client)
- ✅ PR 3.2: Shared Components
- ✅ PR 3.3: Auth Pages
- ✅ PR 3.4: Student Management
- ✅ PR 3.5: Teacher Management
- ✅ PR 3.6: Course Management
- ✅ PR 3.7: Class Management

---

## 📊 Progress Overview

| Category | Total Items | Completed | Remaining | Progress |
|----------|------------|-----------|-----------|----------|
| **Test Infrastructure** | 5 | 5 | 0 | ✅ 100% |
| **Component Tests** | 12 | 2 | 10 | 🟡 17% |
| **Form Tests** | 4 | 1 | 3 | 🟡 25% |
| **Hook Integration Tests** | 4 | 1 | 3 | 🟡 25% |
| **API Client Tests** | 4 | 0 | 4 | ⏳ 0% |
| **Utility Tests** | 2 | 0 | 2 | ⏳ 0% |
| **TOTAL** | **31** | **9** | **22** | **🟡 29%** |

**Estimated Test Count:**
- Current: ~28 tests
- Target: ~120-150 tests
- Coverage: ~20% → Target: ≥80%

---

## ✅ COMPLETED (Part 1 - Commit e0b4045)

### Test Infrastructure ✅
1. ✅ **MSW Setup** (`src/mocks/`)
   - `handlers.ts`: Mock API responses cho Students, Teachers, Courses, Classes
   - `server.ts`: MSW server setup for Node.js tests
   - All CRUD operations + lifecycle actions mocked

2. ✅ **Test Utils** (`src/test/`)
   - `setup.ts`: Global test setup with MSW, router mocks, browser API mocks
   - `utils.tsx`: Custom render with QueryClient provider
   - `README.md`: Test documentation

3. ✅ **Vitest Config**
   - Coverage thresholds: 80% (lines, functions, statements), 75% (branches)
   - Exclude: ui components (shadcn), types, test files
   - Provider: v8 coverage

4. ✅ **Next.js Mocks**
   - `useRouter()` mock with push, replace, back, prefetch
   - `usePathname()` mock
   - `useSearchParams()` mock

5. ✅ **Skill Documentation**
   - `.claude/skills/frontend-testing-requirements.md`: Comprehensive testing standards

### Component Tests ✅
1. ✅ **DataTable** (8 tests)
   - Render with data
   - Empty state
   - Column headers
   - Pagination controls
   - Disable previous/next buttons
   - Custom cell rendering

2. ✅ **SearchInput** (4 tests)
   - Render input
   - Debounced search
   - Clear button
   - Custom placeholder

### Form Tests ✅
1. ✅ **StudentForm** (6 tests)
   - Render all fields (Vietnamese labels)
   - Required field validation
   - Email format validation
   - Submit valid data
   - Disable during submission
   - Pre-fill in edit mode

### Hook Integration Tests ✅
1. ✅ **use-students** (9 tests)
   - List query: success + error handling
   - Single query: by ID + disabled when ID=0
   - Create mutation: success + validation errors
   - Update mutation: success
   - Delete mutation: success + error handling

---

## 🔨 TODO: Part 2 - Form Tests

### 1. TeacherForm Tests (8 tests)
**File:** `src/components/forms/__tests__/teacher-form.test.tsx`

```typescript
describe('TeacherForm', () => {
  // Rendering
  - should render all required fields (tên giáo viên, email, phone, specialization)

  // Validation
  - should validate required fields on submit
  - should validate email format
  - should validate phone format (Vietnamese)

  // Submission
  - should submit valid data

  // States
  - should disable submit during submission
  - should pre-fill data in edit mode
  - should show status selector in edit mode
});
```

**Vietnamese Labels:**
- Tên giáo viên
- Email
- Số điện thoại
- Chuyên môn
- Trạng thái (ACTIVE/ON_LEAVE/TERMINATED)

### 2. CourseForm Tests (10 tests)
**File:** `src/components/forms/__tests__/course-form.test.tsx`

```typescript
describe('CourseForm', () => {
  // Rendering
  - should render all required fields (tên khóa học, mã khóa học, price)

  // Validation
  - should validate required fields
  - should validate price as positive number
  - should validate code format (uppercase, no spaces)

  // Submission
  - should submit valid data
  - should format price to VND

  // Edit Mode States
  - should disable submit during submission
  - should pre-fill data in edit mode
  - should lock fields when status=PUBLISHED
  - should lock fields when status=ARCHIVED
});
```

**Vietnamese Labels:**
- Tên khóa học
- Mã khóa học
- Mô tả
- Giá (VND)

### 3. ClassForm Tests (10 tests)
**File:** `src/components/forms/__tests__/class-form.test.tsx`

```typescript
describe('ClassForm', () => {
  // Rendering
  - should render all required fields (tên lớp, sĩ số, địa điểm)

  // Validation
  - should validate required fields
  - should validate maxStudents >= 1
  - should validate date range (endDate >= startDate)

  // Submission
  - should submit valid data

  // Status-based field locking
  - should allow all fields in DRAFT status
  - should allow all fields in SCHEDULED status
  - should restrict fields in IN_PROGRESS status (only description, locationDetail)
  - should make all fields read-only in COMPLETED status
  - should make all fields read-only in CANCELLED status

  // States
  - should disable submit during submission
});
```

**Vietnamese Labels:**
- Tên lớp học
- Sĩ số tối đa
- Lịch học
- Loại địa điểm (Trực tiếp / Trực tuyến)
- Chi tiết địa điểm

---

## 🔨 TODO: Part 3 - Hook Integration Tests

### 1. use-teachers Hooks (9 tests)
**File:** `src/hooks/__tests__/use-teachers.test.tsx`

```typescript
describe('useTeachers Hooks', () => {
  describe('useTeachers (list)', () => {
    - should fetch teachers list successfully
    - should handle API errors gracefully
  });

  describe('useTeacher (single)', () => {
    - should fetch single teacher by ID
    - should not fetch when ID is 0
  });

  describe('useCreateTeacher', () => {
    - should create new teacher successfully
    - should handle validation errors (email exists)
  });

  describe('useUpdateTeacher', () => {
    - should update teacher successfully
  });

  describe('useDeleteTeacher', () => {
    - should delete teacher successfully
    - should handle delete errors
  });
});
```

### 2. use-courses Hooks (11 tests)
**File:** `src/hooks/__tests__/use-courses.test.tsx`

```typescript
describe('useCourses Hooks', () => {
  describe('useCourses (list)', () => {
    - should fetch courses list successfully
    - should handle API errors
  });

  describe('useCourse (single)', () => {
    - should fetch single course by ID
    - should not fetch when ID is 0
  });

  describe('useCreateCourse', () => {
    - should create new course successfully (status=DRAFT)
    - should handle validation errors
  });

  describe('useUpdateCourse', () => {
    - should update course successfully
  });

  describe('useDeleteCourse', () => {
    - should delete course successfully
  });

  // Lifecycle actions
  describe('usePublishCourse', () => {
    - should publish course (DRAFT → PUBLISHED)
  });

  describe('useArchiveCourse', () => {
    - should archive course (PUBLISHED → ARCHIVED)
  });
});
```

### 3. use-classes Hooks (14 tests)
**File:** `src/hooks/__tests__/use-classes.test.tsx`

```typescript
describe('useClasses Hooks', () => {
  describe('useClasses (list)', () => {
    - should fetch classes by course ID successfully
    - should handle API errors
  });

  describe('useClass (single)', () => {
    - should fetch single class by ID
    - should not fetch when ID is 0
  });

  describe('useClassSessions', () => {
    - should fetch sessions for a class
  });

  describe('useCreateClass', () => {
    - should create new class successfully
    - should handle validation errors
  });

  describe('useUpdateClass', () => {
    - should update class successfully
  });

  describe('useDeleteClass', () => {
    - should delete class successfully (SCHEDULED with 0 enrolled)
  });

  // Lifecycle actions
  describe('useStartClass', () => {
    - should start class (SCHEDULED → IN_PROGRESS)
  });

  describe('useCompleteClass', () => {
    - should complete class (IN_PROGRESS → COMPLETED)
  });

  describe('useCancelClass', () => {
    - should cancel class with reason
  });

  // Other actions
  describe('useGenerateClassCode', () => {
    - should generate enrollment code
  });

  describe('useCreateSchedule', () => {
    - should create schedule and sessions
  });
});
```

---

## 🔨 TODO: Part 4 - Remaining Components & Utilities

### Shared Components Tests

#### 1. StatusBadge (4 tests)
**File:** `src/components/common/__tests__/StatusBadge.test.tsx`

```typescript
- should render with default variant
- should render with success variant (green)
- should render with error variant (red)
- should render with custom className
```

#### 2. LoadingSpinner (3 tests)
**File:** `src/components/common/__tests__/LoadingSpinner.test.tsx`

```typescript
- should render spinner
- should render with different sizes (sm, md, lg)
- should render with custom className
```

#### 3. ErrorAlert (4 tests)
**File:** `src/components/common/__tests__/ErrorAlert.test.tsx`

```typescript
- should render with title and message
- should render with default title if not provided
- should render close button
- should call onClose when close clicked
```

### Form Components Tests

#### 4. FormInput (6 tests)
**File:** `src/components/forms/__tests__/form-input.test.tsx`

```typescript
- should render input with label
- should show required asterisk
- should display error message
- should disable input
- should pass through HTML input props
- should support different types (text, email, password, number)
```

#### 5. FormSelect (5 tests)
**File:** `src/components/forms/__tests__/form-select.test.tsx`

```typescript
- should render select with options
- should show required asterisk
- should display error message
- should disable select
- should support default value
```

#### 6. FormTextarea (5 tests)
**File:** `src/components/forms/__tests__/form-textarea.test.tsx`

```typescript
- should render textarea with label
- should show required asterisk
- should display error message
- should disable textarea
- should support rows prop
```

### API Client Tests

#### 7. students API (5 tests)
**File:** `src/lib/api/__tests__/students.test.ts`

```typescript
- should call GET /api/v1/students with params
- should call GET /api/v1/students/:id
- should call POST /api/v1/students
- should call PUT /api/v1/students/:id
- should call DELETE /api/v1/students/:id
```

#### 8. teachers API (5 tests)
**File:** `src/lib/api/__tests__/teachers.test.ts`

```typescript
// Same pattern as students
```

#### 9. courses API (7 tests)
**File:** `src/lib/api/__tests__/courses.test.ts`

```typescript
// CRUD + publish + archive
```

#### 10. classes API (11 tests)
**File:** `src/lib/api/__tests__/classes.test.ts`

```typescript
// CRUD + start + complete + cancel + generateCode + createSchedule + getSessions
```

### Utility Tests

#### 11. formatDate (4 tests)
**File:** `src/lib/__tests__/utils.test.ts`

```typescript
- should format ISO date to Vietnamese locale (DD/MM/YYYY)
- should handle invalid date string
- should format with different separators
- should handle null/undefined
```

#### 12. formatDateTime (4 tests)
**File:** `src/lib/__tests__/utils.test.ts`

```typescript
- should format ISO datetime to Vietnamese locale
- should include time in HH:mm format
- should handle invalid datetime string
- should handle null/undefined
```

---

## 📝 Testing Checklist

### Before Starting Part 2-4:

- [x] MSW handlers có đầy đủ endpoints
- [x] Test utils có AllTheProviders với QueryClient
- [x] Router mocks hoạt động
- [ ] Chạy `pnpm test` - verify Part 1 tests pass
- [ ] Check coverage hiện tại: `pnpm test:coverage`

### For Each Test File:

- [ ] Import đúng utilities từ `@/test/utils`
- [ ] Sử dụng `render` thay vì RTL's render (có provider)
- [ ] Mock API responses với MSW khi cần
- [ ] Test cả tiếng Việt và tiếng Anh labels
- [ ] Test happy path + error cases + edge cases
- [ ] Test user interactions (click, type, submit)
- [ ] Test loading states
- [ ] Test disabled states
- [ ] Clean up sau mỗi test (afterEach)

### Before Committing:

- [ ] All tests pass: `pnpm test`
- [ ] No console errors in test output
- [ ] No `.only` or `.skip` without justification
- [ ] Coverage meets thresholds: `pnpm test:coverage`
- [ ] TypeScript: `pnpm tsc --noEmit`
- [ ] ESLint: `pnpm lint`

---

## 🎯 Coverage Targets

```typescript
// vitest.config.ts - Current thresholds
thresholds: {
  lines: 80,       // Must achieve ≥80%
  functions: 80,   // Must achieve ≥80%
  branches: 75,    // Must achieve ≥75%
  statements: 80,  // Must achieve ≥80%
}
```

**Exclude from Coverage:**
- `src/**/*.test.{ts,tsx}` - Test files
- `src/**/*.spec.{ts,tsx}` - Spec files
- `src/types/**` - Type definitions
- `src/test/**` - Test utilities
- `src/components/ui/**` - Shadcn components (external library)

---

## 🚀 Implementation Plan

### Session Timeline

**Session 1 (Current - 2026-02-22):**
- ✅ Part 1: Test Infrastructure & Initial Tests (DONE - commit e0b4045)

**Session 2 (Next):**
- Part 2: Form Tests (TeacherForm, CourseForm, ClassForm)
- Estimated: 28 tests, 2-3 hours

**Session 3:**
- Part 3: Hook Integration Tests (use-teachers, use-courses, use-classes)
- Estimated: 34 tests, 2-3 hours

**Session 4:**
- Part 4: Shared Components + API Clients + Utilities
- Estimated: 50 tests, 3-4 hours

**Session 5:**
- Coverage verification
- Fix any gaps to reach ≥80%
- Documentation updates
- PR creation with `gh pr create`

---

## 📋 Git Hooks & CI Requirements

### Pre-commit Checks
**File:** `.claude/scripts/pre-commit-check.sh`

Need to add:
```bash
# Frontend test check
if [ -n "$FRONTEND_FILES" ]; then
  echo "Running frontend tests..."
  cd kiteclass/kiteclass-frontend
  pnpm test --run

  if [ $? -ne 0 ]; then
    echo "❌ Frontend tests failed"
    exit 1
  fi
fi
```

### CI Workflow
**File:** `.github/workflows/frontend-ci.yml`

Should include:
```yaml
- name: Run tests with coverage
  run: pnpm test:coverage

- name: Check coverage thresholds
  run: |
    # Vitest will fail if coverage < thresholds
    echo "Coverage thresholds enforced by vitest.config.ts"
```

---

## 🔍 Known Issues & Solutions

### Issue 1: useRouter() in hooks
**Problem:** Hooks using `useRouter()` fail with "invariant expected app router to be mounted"

**Solution:** ✅ Fixed in `src/test/setup.ts`
```typescript
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), ... }),
}));
```

### Issue 2: Debounced search tests timeout
**Problem:** Tests using fake timers cause timeout

**Solution:** ✅ Use `vi.waitFor()` instead of fake timers
```typescript
await vi.waitFor(() => {
  expect(onSearch).toHaveBeenCalled();
}, { timeout: 1000 });
```

### Issue 3: Vietnamese labels not matching
**Problem:** Tests fail because labels are in Vietnamese

**Solution:** ✅ Use flexible regex: `/tên học viên|name/i`

---

## 📚 References

### Skills to Follow:
- `.claude/skills/frontend-testing-requirements.md` - Testing standards (NEW)
- `.claude/skills/kiteclass-frontend-testing-patterns.md` - Test patterns
- `.claude/skills/frontend-code-quality.md` - Code quality checklist

### External Docs:
- [Vitest](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [MSW](https://mswjs.io/)
- [Testing Library User Event](https://testing-library.com/docs/user-event/intro/)

---

## ✅ Definition of Done

PR 3.8 is complete when:

1. **Test Coverage:**
   - [ ] Lines ≥80%
   - [ ] Functions ≥80%
   - [ ] Branches ≥75%
   - [ ] Statements ≥80%

2. **Test Quality:**
   - [ ] All components have tests
   - [ ] All hooks have integration tests
   - [ ] All API clients have tests
   - [ ] All utilities have tests
   - [ ] Happy path + error cases + edge cases covered
   - [ ] User interactions tested

3. **Quality Checks:**
   - [ ] All tests pass: `pnpm test`
   - [ ] Coverage verified: `pnpm test:coverage`
   - [ ] TypeScript: `pnpm tsc --noEmit` ✅
   - [ ] ESLint: `pnpm lint` ✅
   - [ ] No console errors in tests
   - [ ] No `.only` or `.skip` without justification

4. **CI/CD:**
   - [ ] Frontend CI workflow passes
   - [ ] Git hooks updated to run tests
   - [ ] Coverage enforced in CI

5. **Documentation:**
   - [ ] All test files have clear describe blocks
   - [ ] Complex tests have comments
   - [ ] README updated if needed

6. **PR:**
   - [ ] Branch merged to main
   - [ ] Squash commit with detailed message
   - [ ] CURRENT-STATUS.md updated
   - [ ] Coverage reports generated

---

**Current Branch:** `feature/PR-3.8-frontend-testing`
**Next Action:** Continue with Part 2 - Form Tests
**Last Updated:** 2026-02-22
**Progress:** Part 1/4 Complete (29%)
