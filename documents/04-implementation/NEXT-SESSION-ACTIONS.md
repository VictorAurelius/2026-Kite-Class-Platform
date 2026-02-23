# 🎯 Next Session Action Plan

**Created:** 2026-02-22
**For:** Session tiếp theo sau khi PR 3.8 Part 1 complete
**Current Branch:** `feature/PR-3.8-frontend-testing`
**Last Commit:** e0b4045 (Part 1: Test Infrastructure)

---

## ⚡ QUICK START

```bash
# 1. Verify branch
git branch --show-current
# Should show: feature/PR-3.8-frontend-testing

# 2. Check current test status
cd kiteclass/kiteclass-frontend
~/.local/share/pnpm/pnpm test

# 3. Check coverage
~/.local/share/pnpm/pnpm test:coverage

# 4. Continue with Part 2
```

---

## 📋 IMMEDIATE ACTIONS (Priority Order)

### 1. Verify Part 1 Tests Pass ✅
**Command:**
```bash
cd /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend
~/.local/share/pnpm/pnpm test
```

**Expected:**
- 28 tests passing (example.test + DataTable + SearchInput + StudentForm + use-students)
- No failures

**If tests fail:**
- Check error messages
- Fix issues in test files
- Re-commit fixes

### 2. Check Current Coverage 📊
**Command:**
```bash
~/.local/share/pnpm/pnpm test:coverage
```

**Expected:**
- Lines: ~15-25%
- Functions: ~15-25%
- Target: ≥80%

### 3. Start Part 2: Form Tests 🧪

Create 3 test files với **28 tests total**:

#### A. TeacherForm Tests (8 tests)
**File:** `src/components/forms/__tests__/teacher-form.test.tsx`

**Copy pattern từ:** `student-form.test.tsx`

**Tests cần viết:**
```typescript
describe('TeacherForm', () => {
  // 1. Rendering
  it('should render all required fields', () => {
    // Tên giáo viên, Email, Số điện thoại, Chuyên môn
  });

  // 2. Validation (3 tests)
  it('should validate required fields on submit', async () => {});
  it('should validate email format', async () => {});
  it('should validate phone format', async () => {});

  // 3. Submission (1 test)
  it('should submit valid form data', async () => {});

  // 4. States (3 tests)
  it('should disable submit button when submitting', () => {});
  it('should pre-fill form in edit mode', () => {});
  it('should show status selector in edit mode', () => {
    // ACTIVE, ON_LEAVE, TERMINATED
  });
});
```

**Vietnamese Labels to use:**
- `Tên giáo viên`
- `Email`
- `Số điện thoại`
- `Chuyên môn`
- `Trạng thái`

#### B. CourseForm Tests (10 tests)
**File:** `src/components/forms/__tests__/course-form.test.tsx`

```typescript
describe('CourseForm', () => {
  // 1. Rendering
  it('should render all required fields', () => {
    // Tên khóa học, Mã khóa học, Giá
  });

  // 2. Validation (4 tests)
  it('should validate required fields', async () => {});
  it('should validate price as positive number', async () => {});
  it('should validate code format', async () => {
    // Uppercase, no spaces
  });
  it('should format price to VND', async () => {});

  // 3. Submission (1 test)
  it('should submit valid data', async () => {});

  // 4. Edit Mode States (4 tests)
  it('should disable submit during submission', () => {});
  it('should pre-fill data in edit mode', () => {});
  it('should lock fields when status=PUBLISHED', () => {});
  it('should lock fields when status=ARCHIVED', () => {});
});
```

**Vietnamese Labels:**
- `Tên khóa học`
- `Mã khóa học`
- `Mô tả`
- `Giá`
- Status: `DRAFT`, `PUBLISHED`, `ARCHIVED`

#### C. ClassForm Tests (10 tests)
**File:** `src/components/forms/__tests__/class-form.test.tsx`

```typescript
describe('ClassForm', () => {
  // 1. Rendering
  it('should render all required fields', () => {
    // Tên lớp học, Sĩ số tối đa, Loại địa điểm
  });

  // 2. Validation (3 tests)
  it('should validate required fields', async () => {});
  it('should validate maxStudents >= 1', async () => {});
  it('should validate date range', async () => {
    // endDate >= startDate
  });

  // 3. Submission (1 test)
  it('should submit valid data', async () => {});

  // 4. Status-based Field Locking (5 tests)
  it('should allow all fields in DRAFT status', () => {});
  it('should allow all fields in SCHEDULED status', () => {});
  it('should restrict fields in IN_PROGRESS status', () => {
    // Only description, locationDetail editable
  });
  it('should make all fields read-only in COMPLETED', () => {});
  it('should make all fields read-only in CANCELLED', () => {});

  // 5. States (1 test)
  it('should disable submit during submission', () => {});
});
```

**Vietnamese Labels:**
- `Tên lớp học`
- `Sĩ số tối đa`
- `Lịch học`
- `Loại địa điểm` (Trực tiếp / Trực tuyến)
- `Chi tiết địa điểm`

### 4. Commit Part 2 ✅

**After all 28 tests pass:**
```bash
git add kiteclass/kiteclass-frontend/src/components/forms/__tests__/
git commit -m "test(frontend): PR 3.8 - Part 2: Form Tests

Complete form testing for Teacher, Course, and Class forms.

## Tests Added (28 tests)
- TeacherForm (8 tests): Validation, submission, edit mode, status selector
- CourseForm (10 tests): Validation, price format, status-based locking
- ClassForm (10 tests): Validation, date range, status-based field locking

## Coverage Impact
- Part 1: ~20%
- Part 2: ~35-40% (estimated)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

### 5. Start Part 3: Hook Integration Tests 🔗

Create 3 test files với **34 tests total**:

#### A. use-teachers.test.tsx (9 tests)
**Pattern:** Copy từ `use-students.test.tsx`

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

  describe('useCreateTeacher (mutation)', () => {
    - should create new teacher successfully
    - should handle validation errors
  });

  describe('useUpdateTeacher (mutation)', () => {
    - should update teacher successfully
  });

  describe('useDeleteTeacher (mutation)', () => {
    - should delete teacher successfully
    - should handle delete errors
  });
});
```

#### B. use-courses.test.tsx (11 tests)
**Additional:** Lifecycle actions (publish, archive)

```typescript
describe('useCourses Hooks', () => {
  // Standard CRUD (7 tests - same as teachers)

  // Lifecycle actions (4 tests)
  describe('usePublishCourse', () => {
    - should publish course (DRAFT → PUBLISHED)
    - should handle error when already published
  });

  describe('useArchiveCourse', () => {
    - should archive course (PUBLISHED → ARCHIVED)
    - should handle error when already archived
  });
});
```

#### C. use-classes.test.tsx (14 tests)
**Most complex:** Start, Complete, Cancel, GenerateCode, CreateSchedule

```typescript
describe('useClasses Hooks', () => {
  // Standard CRUD (7 tests)

  // Lifecycle actions (7 tests)
  describe('useStartClass', () => {
    - should start class (SCHEDULED → IN_PROGRESS)
  });

  describe('useCompleteClass', () => {
    - should complete class (IN_PROGRESS → COMPLETED)
  });

  describe('useCancelClass', () => {
    - should cancel class with reason
    - should require cancellation reason
  });

  describe('useGenerateClassCode', () => {
    - should generate enrollment code
  });

  describe('useCreateSchedule', () => {
    - should create schedule and sessions
  });

  describe('useClassSessions', () => {
    - should fetch sessions for a class
  });
});
```

### 6. Commit Part 3 ✅

```bash
git add kiteclass/kiteclass-frontend/src/hooks/__tests__/
git commit -m "test(frontend): PR 3.8 - Part 3: Hook Integration Tests

Complete integration testing for all React Query hooks.

## Tests Added (34 tests)
- use-teachers (9 tests): CRUD operations with API mocking
- use-courses (11 tests): CRUD + Publish/Archive lifecycle
- use-classes (14 tests): CRUD + Start/Complete/Cancel + Code generation

## Coverage Impact
- Part 2: ~40%
- Part 3: ~60-65% (estimated)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## 🎯 Success Criteria for Part 2 & 3

### Part 2 Complete When:
- [ ] All 28 form tests pass
- [ ] TeacherForm: 8/8 tests ✅
- [ ] CourseForm: 10/10 tests ✅
- [ ] ClassForm: 10/10 tests ✅
- [ ] Coverage increases to ~35-40%
- [ ] `pnpm test` - 0 failures
- [ ] `pnpm tsc --noEmit` - 0 errors
- [ ] `pnpm lint` - 0 warnings

### Part 3 Complete When:
- [ ] All 34 hook tests pass
- [ ] use-teachers: 9/9 tests ✅
- [ ] use-courses: 11/11 tests ✅
- [ ] use-classes: 14/14 tests ✅
- [ ] Coverage increases to ~60-65%
- [ ] All API mocks working correctly
- [ ] `pnpm test` - 0 failures

---

## 📝 Important Notes

### Vietnamese Label Patterns
**Always use flexible regex để match cả tiếng Việt và Anh:**

```typescript
// Good ✅
screen.getByLabelText(/tên học viên|name/i)
screen.getByLabelText(/email/i)
screen.getByLabelText(/số điện thoại|phone/i)

// Bad ❌
screen.getByLabelText('Name')  // Sẽ fail nếu label là tiếng Việt
```

### Test Pattern to Follow

```typescript
describe('ComponentName', () => {
  // 1. Rendering tests
  it('should render...', () => {
    render(<Component />);
    expect(screen.getByText(...)).toBeInTheDocument();
  });

  // 2. User interaction tests
  it('should handle click', async () => {
    const onClick = vi.fn();
    render(<Component onClick={onClick} />);

    await userEvent.click(screen.getByRole('button'));

    expect(onClick).toHaveBeenCalled();
  });

  // 3. Validation tests
  it('should validate...', async () => {
    render(<Form onSubmit={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(screen.getByText(/error message/i)).toBeInTheDocument();
    });
  });

  // 4. API integration tests (for hooks)
  it('should fetch data', async () => {
    const { result } = renderHook(() => useData(), { wrapper: AllTheProviders });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toHaveLength(2);
  });
});
```

### MSW Mock Pattern

```typescript
// Override handler in test
server.use(
  http.post(`${BASE_URL}/api/v1/students`, () => {
    return HttpResponse.json(
      { success: false, message: 'Email already exists' },
      { status: 400 }
    );
  })
);
```

---

## 🚨 Common Pitfalls to Avoid

1. ❌ **Không mock Next.js router** → Error: "invariant expected app router to be mounted"
   - ✅ Fixed in `src/test/setup.ts`

2. ❌ **Dùng fake timers với userEvent** → Tests timeout
   - ✅ Use `vi.waitFor()` instead

3. ❌ **Hard-code English labels** → Tests fail với Vietnamese UI
   - ✅ Use regex: `/vietnamese|english/i`

4. ❌ **Không clean up MSW handlers** → Tests affect each other
   - ✅ `afterEach(() => server.resetHandlers())`

5. ❌ **Không dùng AllTheProviders** → React Query errors
   - ✅ Use `render` from `@/test/utils`

---

## 📊 Expected Progress After Part 2 & 3

```
Part 1 (Current):  28 tests,  ~20% coverage ✅
Part 2 (Forms):    +28 tests, ~35-40% coverage
Part 3 (Hooks):    +34 tests, ~60-65% coverage
---------------------------------------------------
Total so far:      90 tests,  ~60-65% coverage

Remaining (Part 4): ~30-40 tests to reach 80%
```

---

## 🔗 Quick Links

**Status Document:** `documents/04-implementation/PR-3.8-TESTING-STATUS.md`
**Skill Reference:** `.claude/skills/frontend-testing-requirements.md`
**Test Examples:** `src/components/forms/__tests__/student-form.test.tsx`

**Commands:**
```bash
# Run tests
~/.local/share/pnpm/pnpm test

# Run with coverage
~/.local/share/pnpm/pnpm test:coverage

# Run specific test file
~/.local/share/pnpm/pnpm test src/components/forms/__tests__/teacher-form.test.tsx

# TypeScript check
~/.local/share/pnpm/pnpm tsc --noEmit

# ESLint
~/.local/share/pnpm/pnpm lint
```

---

**Last Updated:** 2026-02-22
**Ready to Continue:** ✅ YES
**Next Action:** Start Part 2 - TeacherForm tests
