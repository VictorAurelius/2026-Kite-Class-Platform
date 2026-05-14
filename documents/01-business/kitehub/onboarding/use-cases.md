# Onboarding Progress — Use Cases

**Domain:** Tenant onboarding checklist tracking
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)

> **Bucket 0 stub status:** use-cases dưới là stub form (≥1 đầy đủ happy + error branch, ≥1 placeholder). Bucket B (GAP-538) sẽ enrich thêm theo final FE/BE implementation.

---

## UC-ONBOARD-001 — Tenant lần đầu vào dashboard, fetch checklist state

**Actor:** Authenticated tenant user (owner hoặc admin role thuộc tenant).
**Trigger:** Sau khi đăng nhập, FE redirect tới `/dashboard` hoặc `/onboarding` → component mount → fetch checklist.
**Endpoint:** `GET /api/v1/onboarding-progress`

### Happy path

1. FE gắn JWT token vào `Authorization: Bearer <token>` header.
2. FE call `GET /api/v1/onboarding-progress`.
3. BE extract `tenantId` từ JWT claim.
4. BE query `onboarding_progress` table by `tenant_id`.
5. Nếu row chưa tồn tại → BE lazy-init row (5 step, tất cả `completed=false`, `lastUpdatedAt=now()`) trong cùng transaction.
6. BE compute `completionPercent` = `completedSteps / totalSteps * 100`.
7. BE return `200 OK` với `OnboardingProgressResponse` body.
8. FE render checklist UI 5 steps + progress bar.

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 1 | JWT missing/expired | 401 | `UNAUTHENTICATED` | Redirect `/login` |
| 3 | JWT không có `tenantId` claim | 403 | `TENANT_CONTEXT_MISSING` | Banner "Vui lòng chọn workspace"; redirect tenant picker |

### FE behavior notes

- Lần fetch đầu sau login → render skeleton loader đến khi response về.
- Cache response trong 30s (TanStack Query staleTime); auto-refetch khi user PUT step.

---

## UC-ONBOARD-002 — Tenant hoàn thành 1 bước, persist completion

**Actor:** Authenticated tenant user.
**Trigger:** User click checkbox / button "Hoàn tất bước này" trong UI checklist.
**Endpoint:** `PUT /api/v1/onboarding-progress`

### Happy path

1. FE optimistic-update checklist UI (mark step complete locally).
2. FE call `PUT /api/v1/onboarding-progress` với body `{ stepId: "PROFILE_SETUP", completed: true }`.
3. BE validate `stepId` thuộc enum `OnboardingStepId`.
4. BE update row `onboarding_progress` cho `tenantId` đang active.
5. Nếu `stepId=IMPORT_DATA` AND `completed=true` AND `tenant.metadata.is_beta_demo_data=true` → emit `onboarding.demo-data.requested` outbox event (BR-ONBOARD-002).
6. Nếu sau update `completionPercent` đạt 100% lần đầu → emit `onboarding.completed` outbox event (BR-ONBOARD-003).
7. BE return `200 OK` với state mới (full response shape giống GET).
8. FE reconcile optimistic state với server response.

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 3 | `stepId` không thuộc enum | 400 | `ONBOARDING_INVALID_STEP_ID` | Inline error; rollback optimistic state |
| 2 | Body malformed | 400 | `ONBOARDING_INVALID_PAYLOAD` | Toast error "Yêu cầu không hợp lệ" |
| 1 | JWT missing/expired | 401 | `UNAUTHENTICATED` | Redirect `/login` |
| (gw) | Rate limit (>60 req/min/tenant) | 429 | `RATE_LIMITED` | Toast "Vui lòng thử lại sau"; disable button 30s |

### FE behavior notes

- Optimistic UI update CHỈ cho `completed=true` (less risky than uncheck). Uncheck = full round-trip để confirm.
- Khi nhận `onboarding.completed` event (Bucket F push channel), FE show celebration modal (confetti hoặc đơn giản).

---

## (UC-ONBOARD-003 placeholder) — Admin tracks tenant onboarding completion across cohort

*Placeholder for Wave 79+ analytics scope. Out of Wave 78 scope.*
