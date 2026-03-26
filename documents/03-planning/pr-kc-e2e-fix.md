# PR Plan: KC E2E — Fix Backend API + Frontend Playwright

**Branch:** `feat/kc-e2e-fix`
**Date:** 2026-03-26
**Goal:** Chạy cả 2 loại E2E của KiteClass, fix failures, tạo PR

---

## Hai loại E2E trong phạm vi này

| Loại | Script | Cần gì | CI? |
|------|--------|--------|-----|
| **Backend API E2E** | `kiteclass/scripts/test-api-e2e.sh` | KiteHub Docker stack running | Local only |
| **Frontend Playwright E2E** | `kiteclass/scripts/test-e2e-playwright.sh` | Next.js dev server (tự start) | Xét sau |

> **KiteHub E2E**: Giữ nguyên `if: false` trong CI — không thay đổi

---

## Chiến lược Run Incremental (tiết kiệm tài nguyên)

### Nguyên tắc
- **Không run all ngay** — chạy từng spec / từng nhóm test
- **Fix từng spec rồi mới chạy tiếp** — không để failures chồng chất
- **Run all chỉ ở bước cuối** để confirm toàn bộ pass

### Lệnh từng bước (dùng script, không run raw commands)

```bash
# Backend API E2E — chạy toàn bộ (không có option filter từng section)
cd kitehub && ./scripts/up.sh            # Bước 1: Start KiteHub stack
cd kiteclass && ./scripts/test-api-e2e.sh  # Bước 2: Run API tests

# Frontend Playwright — chạy từng spec
cd kiteclass
./scripts/test-e2e-playwright.sh --spec e2e/example.spec.ts
./scripts/test-e2e-playwright.sh --spec e2e/auth.spec.ts
# ... sau khi fix từng spec ...
./scripts/test-e2e-playwright.sh             # Final: run all

# Xem report sau khi run
./scripts/test-e2e-playwright.sh --report

# Debug khi fail
./scripts/test-e2e-playwright.sh --headed --spec e2e/auth.spec.ts
```

---

## Phase 1: Backend API E2E (`test-api-e2e.sh`)

### Setup
```
cd kitehub && ./scripts/up.sh
# Đợi healthy: ./scripts/status.sh
```

### Chạy
```
cd kiteclass && ./scripts/test-api-e2e.sh
```

Script này test 7 nhóm:
1. Setup — Register test tenant qua KiteHub
2. Health Check
3. Student CRUD
4. Teacher CRUD
5. Course CRUD
6. Class CRUD
7. Multi-tenant Isolation

### Fix strategy
- Nếu fail ở CRUD: kiểm tra API response format thay đổi không
- Nếu fail ở Multi-tenant: kiểm tra tenant routing headers
- Nếu fail ở Setup: kiểm tra KiteHub gateway đang running đúng chưa

---

## Phase 2: Frontend Playwright E2E (`test-e2e-playwright.sh`)

### Thứ tự chạy từng spec (từ đơn giản → phức tạp)

| Bước | Spec | Script | Ưu tiên |
|------|------|--------|---------|
| 1 | `e2e/example.spec.ts` | `--spec e2e/example.spec.ts` | Sanity |
| 2 | `e2e/auth.spec.ts` | `--spec e2e/auth.spec.ts` | Foundation |
| 3 | `e2e/students.spec.ts` | `--spec e2e/students.spec.ts` | CRUD |
| 4 | `e2e/theme.spec.ts` | `--spec e2e/theme.spec.ts` | UI |
| 5 | `e2e/classes.spec.ts` | `--spec e2e/classes.spec.ts` | CRUD |
| 6 | `e2e/billing.spec.ts` | `--spec e2e/billing.spec.ts` | Feature |
| 7 | `e2e/branding.spec.ts` | `--spec e2e/branding.spec.ts` | Feature |
| 8 | `e2e/feature-flags.spec.ts` | `--spec e2e/feature-flags.spec.ts` | Feature |
| 9 | `e2e/attendance-enhancements.spec.ts` | `--spec e2e/attendance-enhancements.spec.ts` | Feature |
| 10 | `e2e/critical-journeys/dashboard-navigation.spec.ts` | `--spec e2e/critical-journeys/dashboard-navigation.spec.ts` | Critical |
| 11 | `e2e/critical-journeys/class-lifecycle.spec.ts` | `--spec e2e/critical-journeys/class-lifecycle.spec.ts` | Critical |
| 12 | `e2e/critical-journeys/course-to-class-flow.spec.ts` | `--spec e2e/critical-journeys/course-to-class-flow.spec.ts` | Critical |
| **Final** | **Tất cả** | `./scripts/test-e2e-playwright.sh` | Confirm |

### Quy trình mỗi spec
```
1. ./scripts/test-e2e-playwright.sh --spec <file>
2. Xem output / ./scripts/test-e2e-playwright.sh --report
3. Fix failures (selector sai, mock thiếu, assertion lỗi thời)
4. Re-run: ./scripts/test-e2e-playwright.sh --spec <file>
5. All pass → next spec
```

---

## Checklist

### Phase 1 — Backend API
- [ ] KiteHub stack started (`./scripts/up.sh`)
- [ ] `test-api-e2e.sh` — run & fix

### Phase 2 — Frontend Playwright (từng spec)
- [ ] example.spec.ts
- [ ] auth.spec.ts
- [ ] students.spec.ts
- [ ] theme.spec.ts
- [ ] classes.spec.ts
- [ ] billing.spec.ts
- [ ] branding.spec.ts
- [ ] feature-flags.spec.ts
- [ ] attendance-enhancements.spec.ts
- [ ] critical-journeys/dashboard-navigation.spec.ts
- [ ] critical-journeys/class-lifecycle.spec.ts
- [ ] critical-journeys/course-to-class-flow.spec.ts
- [ ] **Final all-pass**: `./scripts/test-e2e-playwright.sh`

### PR changes
- [ ] `kiteclass/scripts/test-e2e-playwright.sh` — script mới (đã tạo)
- [ ] `e2e/**/*.spec.ts` — fixes cho từng spec fail
- [ ] `e2e/helpers/api-mocks.ts` — thêm mock endpoints còn thiếu (nếu cần)
- [ ] `playwright.config.ts` — cập nhật nếu cần

---

## Quyết định CI

Sau khi tất cả pass local:
- Frontend Playwright tests dùng mock → **có thể enable trong CI** (`if: false` → remove)
- Backend API E2E cần Docker stack → **giữ local only**

> Quyết định cuối trình user trước khi merge
