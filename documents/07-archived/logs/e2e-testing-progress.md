# E2E Testing Progress Report

**Last Updated**: 2026-02-26 (added V4.1 notes)

> ⭐ **V4.1 Update**: Sẽ thêm E2E tests cho Guest Pages (landing → course catalog → trial lesson → contact form → lead conversion). Tham khảo `03-frontend-prs.md` PR 3.12.

**Ngày cập nhật**: 2026-02-24
**Trạng thái**: Tạm dừng E2E, chuyển sang Integration Tests

## Tóm tắt

E2E testing với Playwright đã giúp phát hiện và sửa nhiều issues quan trọng, nhưng tốn quá nhiều thời gian để maintain và debug. Quyết định chuyển sang **Integration Tests** (Vitest + RTL + MSW) như kế hoạch gốc để coverage nhanh hơn và reliable hơn.

---

## Kết quả E2E Tests

### Auth Module ✅ HOÀN THÀNH
**File**: `e2e/auth.spec.ts`
**Kết quả**: **10/11 tests passing** (91% pass rate)

#### Passing Tests (10)
1. ✅ Should show login form with all fields
2. ✅ Should show validation errors for empty form
3. ✅ Should show validation error for invalid email
4. ✅ Should show error for invalid credentials
5. ✅ Should login successfully with valid credentials
6. ✅ Should redirect authenticated user from login to dashboard
7. ✅ Should persist auth state after page refresh
8. ✅ Should logout successfully
9. ✅ Should clear auth state after logout
10. ✅ Should redirect to login after logout

#### Failing Test (1)
- ❌ Should show server error message (timeout issue)

#### Issues Resolved
1. **CORS Configuration** (kiteclass-gateway)
   - Added WebFlux CORS filter for cookie credentials
   - Enabled `allowCredentials: true` + specific origins
   - Fixed: `gateway/src/main/java/com/kiteclass/gateway/config/CorsConfig.java`

2. **Zustand Persist Hydration Race**
   - Dashboard layout checked auth before localStorage rehydration
   - Fixed: Added `isHydrated` state to wait for Zustand
   - File: `src/app/(dashboard)/layout.tsx`

---

### Students Module ⚠️ CHƯA HOÀN THÀNH
**File**: `e2e/students.spec.ts`
**Kết quả**: **1/20 tests passing** (5% pass rate)

#### Passing Test (1)
- ✅ Should view student detail page

#### Failing Tests (19)
**Detail Page (4 tests)**
- ❌ Should display student information correctly
- ❌ Should have edit button on detail page
- ❌ Should have delete button on detail page
- ❌ Should show error when student not found

**Edit Page (11 tests)**
- ❌ Should navigate to edit page from detail
- ❌ Should load existing student data in form
- ❌ Should update student successfully
- ❌ Should validate required fields on edit
- ❌ Should validate email format on edit
- ❌ Should handle duplicate email error on edit
- ❌ Should handle server error on edit
- ❌ Should disable submit button while updating
- ❌ Should show error when student not found on edit
- ❌ Should have cancel button to go back
- ❌ Should show loading spinner while loading student

**Delete & Navigation (4 tests)**
- ❌ Should delete student from detail page with confirmation
- ❌ Should cancel delete when confirmation rejected
- ❌ Should navigate from list to detail to edit and back
- ❌ Should have breadcrumb navigation

#### Root Causes
1. **Element Selectors Mismatch**: E2E tests expect text/elements not matching actual implementation
2. **Page Structure Changes**: Detail/edit pages may have different structure than expected
3. **Timing Issues**: Some elements not visible within timeout
4. **Dialog Handling**: Delete confirmation dialogs have timing problems

---

## Thời gian đã tiêu tốn

### Session 1: CORS Debugging
- **Thời lượng**: ~2-3 giờ
- **Kết quả**: Fixed CORS để auth tests pass (10/11)

### Session 2: Students E2E Debugging
- **Thời lượng**: ~2-3 giờ
- **Kết quả**:
  - Fixed Zustand hydration race condition
  - Fixed table navigation pattern
  - Chỉ 1/20 tests passing
  - 19 tests còn lại cần sửa element selectors

### Tổng thời gian E2E
**~4-6 giờ** để có 11/31 tests passing (35% pass rate)

**Vấn đề**: E2E tests quá brittle (dễ bị break khi UI thay đổi), chậm (test phải start server + browser), khó debug.

---

## Quyết định: Pivot sang Integration Tests

### Tại sao Integration Tests tốt hơn?

#### 1. **Tốc độ nhanh hơn 10-100x**
- E2E: 30-60 giây cho 20 tests (Playwright + server + browser)
- Integration: 1-5 giây cho 20 tests (Vitest + JSDOM + MSW)

#### 2. **Ít brittle hơn**
- E2E: Break khi thay đổi CSS class, text, layout
- Integration: Only break khi thay đổi behavior thực sự

#### 3. **Dễ debug hơn**
- E2E: Phải xem screenshot, trace, network logs
- Integration: Console.log + Jest matchers + IDE debugging

#### 4. **Coverage tốt hơn**
- E2E: Khó test edge cases (network errors, race conditions)
- Integration: Dễ mock API responses, test error states

### Kế hoạch gốc (từ plan file)

**Automated Testing Plan for KiteClass Dashboard Pages**

**Strategy**: Focus on **Integration Tests** (Vitest + RTL + MSW) as primary approach, with selective E2E tests for critical user journeys only.

**Timeline**: 3-4 tuần (phased by module)
**Deliverable**: 120-140 integration tests covering all dashboard pages

#### Test Pyramid
```
       /\
      /  \  E2E Tests (3-5 critical flows)
     /____\
    /      \  Integration Tests (120-140 tests) ← PRIMARY FOCUS
   /________\
  /          \  Unit Tests (152 tests) ✅ DONE
 /____________\
```

---

## Kế hoạch tiếp theo

### Phase 1: Students Module Integration Tests (Tuần 1)

**Files to Create**:
1. `/src/test/page-test-utils.tsx` - Shared test helpers
2. `/src/app/(dashboard)/students/__tests__/students-list.integration.test.tsx`
3. `/src/app/(dashboard)/students/new/__tests__/students-new.integration.test.tsx`
4. `/src/app/(dashboard)/students/[id]/__tests__/student-detail.integration.test.tsx`
5. `/src/app/(dashboard)/students/[id]/edit/__tests__/student-edit.integration.test.tsx`

**Test Scenarios**: 25-30 tests
- List page: Load data, search, pagination, delete, error handling (8-10 tests)
- Create page: Form validation, successful creation, API errors (5-6 tests)
- Detail page: Load data, display, navigation, 404 handling (6-7 tests)
- Edit page: Pre-fill form, update, validation, errors (5-6 tests)

**Deliverable**: ~400-500 LOC, 25-30 tests, 1-2 ngày

---

### Phase 2: Teachers Module (Tuần 1-2)
- Same pattern as Students
- Additional: Status management (ACTIVE, INACTIVE, ON_LEAVE)
- **25-30 tests**, ~400-500 LOC

### Phase 3: Courses Module (Tuần 2-3)
- Lifecycle actions: Publish, Archive
- Field locking based on status
- **30-35 tests**, ~500-600 LOC

### Phase 4: Classes Module (Tuần 3-4)
- Course selector, lifecycle, sessions
- Most complex module
- **35-40 tests**, ~600-700 LOC

### Phase 5: E2E Critical Journeys (Tuần 4) - SAU CÙNG
**Chỉ 3 critical flows**:
1. Student enrollment flow (Login → Create Student → Verify in list)
2. Course publish to class (Create Course → Publish → Create Class)
3. Class lifecycle (Create → Start → Complete)

**3 tests**, ~200-300 LOC

---

## Lessons Learned

### E2E Testing
✅ **Tốt cho**:
- Phát hiện integration issues (CORS, auth session)
- Verify critical user journeys
- Catch real browser-specific bugs

❌ **Không tốt cho**:
- Comprehensive page testing (quá chậm)
- Edge cases và error states (khó setup)
- Rapid development cycle (break quá dễ)

### Best Practice
1. **Viết Integration Tests trước** (fast feedback loop)
2. **Chỉ viết E2E cho critical flows** (3-5 flows)
3. **Không viết E2E cho mọi page** (waste of time)

---

## Tài liệu tham khảo

### Existing Test Files (đã có)
- `/src/test/utils.tsx` - Custom render with providers
- `/src/mocks/handlers.ts` - MSW handlers (all endpoints)
- `/src/components/forms/__tests__/*.test.tsx` - Form test patterns
- `/src/hooks/__tests__/*.test.tsx` - Hook test patterns

### Plan File
`/home/vkiet/.claude/plans/rustling-plotting-shore.md` - Full testing plan

---

## Action Items

- [x] Document E2E testing progress
- [x] Commit E2E changes (auth helpers, hydration fix)
- [ ] Create `/src/test/page-test-utils.tsx` shared helpers
- [ ] Implement Phase 1: Students Integration Tests (25-30 tests)
- [ ] Implement Phase 2: Teachers Integration Tests (25-30 tests)
- [ ] Implement Phase 3: Courses Integration Tests (30-35 tests)
- [ ] Implement Phase 4: Classes Integration Tests (35-40 tests)
- [ ] Implement Phase 5: E2E Critical Journeys (3 tests)

---

**Tổng kết**: E2E testing đã giúp fix CORS và auth session issues (quan trọng!), nhưng không phải cách hiệu quả để test toàn bộ dashboard pages. Chuyển sang Integration Tests để coverage nhanh hơn, reliable hơn, và dễ maintain hơn.
