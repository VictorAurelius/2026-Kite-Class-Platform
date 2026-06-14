# GAP-1362: AcademicYearService.listAll() + AssetUrlsQualityCheck.run() unbounded findAll

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 1.1)
**Affects:** `kiteclass-core .../academicyear/service/AcademicYearService.java:114`, `.../quality/check/AssetUrlsQualityCheck.java:35`

## Problem

Hai internal caller dùng `findAll()` load-all-into-memory:
- `AcademicYearService.listAll():114` — `academicYearRepository.findAll()`. Javadoc nói "paginated in real controller" nhưng service method tự nó unbounded.
- `AssetUrlsQualityCheck.run():35` — `repository.findAll()` trên `BrandingResource` rồi stream filter/count (internal quality check, RLS-scoped per-instance).

Bảng nhỏ/internal → severity P2 (không catastrophic). Nhưng vẫn là pattern unbounded — academic-year tích lũy qua năm, branding-resource per-tenant có thể nhiều. Nên thêm bound/limit phòng vệ.

## Proposed Fix

- AcademicYearService.listAll(): trả Pageable hoặc giới hạn theo tenant + status (chỉ cần CURRENT/recent).
- AssetUrlsQualityCheck: stream qua page/cursor hoặc count-query thay vì load-all.

## Acceptance Criteria

- [ ] AcademicYearService không `findAll()` unbounded (Pageable hoặc filtered query)
- [ ] AssetUrlsQualityCheck dùng count/paged query thay full materialization

## Related

- Discovered in: 2026-06-14 performance audit (F-007)
- GAP-432 (DONE) — precedent findAll bounded
