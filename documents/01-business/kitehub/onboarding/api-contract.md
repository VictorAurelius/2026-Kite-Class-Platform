# Onboarding Progress — API Contract

**Domain:** Tenant onboarding checklist tracking (Wave 78 — GAP-538)
**Source-of-truth controller:** (planned) `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/onboarding/controller/OnboardingProgressController.java`
**Last verified:** 2026-05-14 (Wave 78 Bucket 0 Foundation)

This contract là source-of-truth cross-layer cho Wave 78 Bucket B, consumed by:
- FE Bucket B (GAP-538) — Onboarding checklist component (`/onboarding/*`) reads/writes step state
- BE Bucket B (GAP-538) — `OnboardingProgressController` + `OnboardingProgress` entity + Flyway migration `V[N]__create_onboarding_progress_table.sql`
- MSW handler `kitehub-frontend/src/test/msw/handlers/onboarding.ts` (this PR — Bucket 0)

---

## Domain model

`OnboardingProgress` is **per-tenant** state (one row per tenant). Each tenant có một checklist gồm `N` bước cứng (hard-coded enum trong BE) — FE render dynamic dựa trên `steps[]` array trả về. `completion_percent` = `completed_count / total_count * 100` (server-computed).

**Step IDs (Phase 1 BETA, hardcoded enum):**

| Step ID | Tên hiển thị (vi) | Mô tả ngắn |
|---------|-------------------|-----------|
| `PROFILE_SETUP` | Hoàn tất hồ sơ tenant | Logo + tên + persona xác nhận |
| `INVITE_TEAM` | Mời thành viên đầu tiên | Add ≥1 user khác hoặc skip |
| `IMPORT_DATA` | Nhập dữ liệu mẫu | Opt-in sample/demo data seed (gated bởi `is_beta_demo_data` flag) |
| `CREATE_FIRST_CLASS` | Tạo lớp học đầu tiên | Test core feature KiteClass |
| `EXPLORE_FEATURES` | Khám phá tính năng | Tour modal hoặc skip |

Server-side enum `OnboardingStepId` quản lý whitelist; client KHÔNG được gửi `stepId` ngoài enum.

---

## Endpoints

### GET /api/v1/onboarding-progress

**Use case:** UC-ONBOARD-001 — Tenant lần đầu vào dashboard, FE fetch checklist state để render
**Auth:** Bearer JWT (any authenticated user thuộc tenant). Trả checklist của tenant active trên JWT context.

**Request:** no body, no query params.

**Response 200 OK (`OnboardingProgressResponse`):**
```json
{
  "tenantId": "tenant-uuid-v4",
  "completionPercent": 40,
  "totalSteps": 5,
  "completedSteps": 2,
  "lastUpdatedAt": "2026-05-14T08:30:00Z",
  "steps": [
    {
      "stepId": "PROFILE_SETUP",
      "completed": true,
      "completedAt": "2026-05-14T07:00:00Z"
    },
    {
      "stepId": "INVITE_TEAM",
      "completed": true,
      "completedAt": "2026-05-14T08:30:00Z"
    },
    {
      "stepId": "IMPORT_DATA",
      "completed": false,
      "completedAt": null
    },
    {
      "stepId": "CREATE_FIRST_CLASS",
      "completed": false,
      "completedAt": null
    },
    {
      "stepId": "EXPLORE_FEATURES",
      "completed": false,
      "completedAt": null
    }
  ]
}
```

**Special case — first call (no row yet):**
- BE auto-creates a default row trên tenant first GET (lazy init). Tất cả `completed=false`, `lastUpdatedAt=now()`.
- Response shape giống happy path nhưng `completionPercent=0`, `completedSteps=0`.

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 401 | `UNAUTHENTICATED` | Missing/invalid JWT |
| 403 | `TENANT_CONTEXT_MISSING` | JWT không có `tenantId` claim |

---

### PUT /api/v1/onboarding-progress

**Use case:** UC-ONBOARD-002 — Tenant hoàn thành 1 bước, FE PUT để persist
**Auth:** Bearer JWT. Update tenant active trên JWT context. **KHÔNG hỗ trợ cross-tenant update.**

**Request body (`OnboardingProgressUpdateCommand`):**
```json
{
  "stepId": "PROFILE_SETUP",
  "completed": true
}
```

**Field constraints:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `stepId` | enum string | yes | MUST be in `OnboardingStepId` enum (xem domain model trên) |
| `completed` | boolean | yes | `true` hoặc `false`. `false` cho phép "uncheck" (undo) |

**Response 200 OK (`OnboardingProgressResponse`):** giống GET response, reflect state SAU update. `lastUpdatedAt = now()`.

**Idempotency:** PUT cùng `stepId` + `completed` value KHÔNG đổi state (no-op). `lastUpdatedAt` chỉ update khi giá trị thực sự thay đổi.

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `ONBOARDING_INVALID_STEP_ID` | `stepId` không thuộc enum |
| 400 | `ONBOARDING_INVALID_PAYLOAD` | Body malformed / missing required fields |
| 401 | `UNAUTHENTICATED` | Missing/invalid JWT |
| 403 | `TENANT_CONTEXT_MISSING` | JWT không có `tenantId` claim |

---

## Rate limits

- GET: không rate-limit (idempotent read).
- PUT: 60 req/min/tenant (per-tenant bucket tại gateway). Vượt → 429 `RATE_LIMITED`.

---

## Side effects

- PUT khi `completed=true` lần đầu cho `stepId=IMPORT_DATA` AND `tenant.metadata.is_beta_demo_data=true` → emit `onboarding.demo-data.requested` event qua outbox (per `design-patterns.md` §3.5). Bucket B owns sample-data seed implementation; contract chỉ document side-effect.
- Khi `completionPercent` đạt 100% lần đầu → emit `onboarding.completed` event qua outbox để retention pipeline (email day-7 survey trigger).

---

## Related

- BR-ONBOARD-001..003: `documents/01-business/kitehub/onboarding/rules.md`
- UC-ONBOARD-001..002: `documents/01-business/kitehub/onboarding/use-cases.md`
- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
- Source migration (planned): `kitehub/kitehub-subscription/src/main/resources/db/migration/V[N]__create_onboarding_progress_table.sql`
