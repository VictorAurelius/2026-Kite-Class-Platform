# GAP-1111: slug-availability check dùng sai source-of-truth (branding_jobs.organization_name vs instances.subdomain)

**Status:** 🟢 DONE 2026-06-10
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-10 (investigation "subdomain đã dùng chưa có check thật không" — user-flagged)
**Affects:** `kitehub-branding` `SlugAvailabilityService` + `GET /api/v1/branding/slug-availability`

## Problem

Endpoint `GET /api/v1/branding/slug-availability` (`BrandingWizardController:66`, sub-GAP-272i) — FE wizard gọi để báo user "subdomain còn trống / đã dùng" TRƯỚC khi submit. Nhưng `SlugAvailabilityService.isTaken()` (`SlugAvailabilityService.java:80-94`) check "đã dùng" dựa trên:
1. Reserved-words filter (admin/api/billing/…)
2. Format regex (3-63 chars, lowercase + hyphen)
3. **`brandingJobRepository.existsByOrganizationNameLowercased(normalized)`** — tức bảng `branding_jobs.organization_name`

⟹ **KHÔNG** check `instances.subdomain` — nguồn canonical THẬT của subdomain tenant.

Canonical subdomain uniqueness sống ở:
- DB: `instances.subdomain VARCHAR(50) UNIQUE NOT NULL` (`V1__create_instances_table.sql:4`)
- App: `InstanceService.existsBySubdomainAndDeletedFalse(...)` (lines 162/225/354) — throw "Subdomain already exists" lúc create/register.

**Hệ quả divergence:** subdomain có thể "còn trống" theo wizard (không có branding_job org-name khớp) NHƯNG đã bị tenant khác claim trong `instances` → user pick xong, tới bước create thì fail ở DB UNIQUE (UX khó hiểu), HOẶC ngược lại. Javadoc `SlugAvailabilityService:19` tự nhận "source of truth for 'taken' is **currently**: reserved-words + organization_name" — interim implementation.

## Proposed Fix

1. `SlugAvailabilityService.isTaken()` check `instanceRepository.existsBySubdomainAndDeletedFalse(slug)` làm source-of-truth chính (giữ reserved-words + format), HOẶC inject 1 shared `SubdomainAvailabilityPort` để cả wizard + InstanceService dùng chung 1 nguồn.
2. Reconcile: nếu slug≠subdomain (org-name → slug → subdomain có transform), document mapping rõ + check đúng bảng cuối (instances.subdomain).
3. IT: subdomain đã claim trong instances → wizard slug-availability trả `available:false`.
4. Cross-service: kitehub-branding cần đọc instances (kitehub-subscription) — qua API/shared-read hoặc event (tránh cross-module direct repo per design-patterns).

## Acceptance Criteria

- [x] slug-availability check phản ánh đúng `instances.subdomain` (canonical) — `SlugAvailabilityService.isTaken` reads `instances.subdomain` via JdbcTemplate trên shared `kitehub` DB (code shipped #2279)
- [x] IT: subdomain taken-in-instances → wizard báo unavailable — `SlugAvailabilityInstancesTest` (Testcontainers Postgres) 5/5 PASS
- [x] Reserved-words + format checks giữ nguyên
- [x] Mapping documented — slug = subdomain (lowercase, `LOWER()` match); không transform

## Related

- Discovered in: investigation 2026-06-10 (PR #2279 G2 recipe Q&A — frontendUrl/subdomain check mechanism)
- Canonical: `InstanceService.existsBySubdomainAndDeletedFalse` + `instances.subdomain` UNIQUE
- Divergent: `SlugAvailabilityService.isTaken` → `branding_jobs.organization_name`
- Note: AI Branding DEPLOY flow `frontendUrl` = placeholder (MockProvisioningService, no check) — separate concern; per-tenant landing render gated GAP-811/1077; domain drift .vn/.com GAP-813.

## Log

- **2026-06-10 (DONE):** Testcontainers IT `SlugAvailabilityInstancesTest` 5/5 PASS (subdomain taken→unavailable, free→available, soft-deleted→available, case-insensitive, reserved-word short-circuit). Cross-service read = JdbcTemplate native query trên shared `kitehub` Postgres DB (branding + subscription cùng DB) — clean, không import JPA entity service khác (per `design-patterns.md`). Code core đã ship #2279; IT đóng AC cuối. Wave branding-fix-2026-06-10 (agent af9cb327, SHA 434ceee4).
- **2026-06-10:** Filed từ investigation user-flagged "check subdomain đã dùng chưa có chính xác không". Phát hiện wizard slug-availability check `branding_jobs.organization_name` thay vì `instances.subdomain` canonical → 2 nguồn sự thật lệch. Per `discovery-to-gap-inline-filing.md`.
