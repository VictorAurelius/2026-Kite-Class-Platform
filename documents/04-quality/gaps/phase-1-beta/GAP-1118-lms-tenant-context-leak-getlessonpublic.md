# GAP-1118: LMS getLessonPublic rò TenantContext cross-tenant trên pooled thread

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Backend (security/multi-tenant — LMS)
**Found:** 2026-06-10 (outside-in audit 3-lens FE LMS wave — failure-mode F10)
**Affects:** `kiteclass-core` `LmsServiceImpl.findLessonWithTenantContext` (dòng ~478-491) + `getLessonPublic`

## Problem

`findLessonWithTenantContext` (dùng bởi `getLessonPublic` — guest access) gọi `TenantContext.setCurrentTenant(lesson.getInstanceId())` khi context chưa set, nhưng KHÔNG clear/restore. Vì `TenantContext` là `ThreadLocal` và thread được pool (Tomcat), instanceId mà guest resolve sẽ **rò sang request kế tiếp trên cùng thread** → request sau (có thể của tenant khác hoặc guest khác) chạy với tenant context sai → nguy cơ cross-tenant data exposure qua RLS.

Đối chiếu: `getCourseStructurePublic` (dòng 76-96) đã làm đúng — capture `previousTenant`, set trong try, restore/clear trong finally. Riêng `getLessonPublic` thiếu try/finally này.

## Root Cause

`findLessonWithTenantContext` chỉ SET context (cần active cho re-query + `buildLessonDetailResponse` lookup resources) nhưng không có ai restore. Caller `getLessonPublic` không wrap try/finally.

## Proposed Fix

Wrap `getLessonPublic` trong try/finally (mirror `getCourseStructurePublic` dòng 76-96): capture `previousTenant = isSet() ? getCurrentTenant() : null` trước, set qua `findLessonWithTenantContext` trong try, restore previous (hoặc `clear()` nếu guest) trong finally. Restore phải ở mức `getLessonPublic` (không trong `findLessonWithTenantContext`) vì `buildLessonDetailResponse` lookup resources vẫn cần tenant active.

## Acceptance Criteria
- [x] Guest (no tenant) gọi `getLessonPublic` → sau khi xong, `TenantContext.isSet()` == false (đã clear)
- [x] Caller có tenant sẵn → sau khi xong, tenant gốc được restore nguyên vẹn
- [x] Unit test phủ cả 2 case (`LmsServiceTest`)
- [ ] Runtime verify (2 request liên tiếp khác tenant trên cùng worker) trước DONE flip

## Related
- Audit report: `documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md` (F10)
- Sister fix cùng PR: GAP-1115, GAP-1116, GAP-1117
- Cross-flow sweep: `getCourseStructurePublic` (đã đúng), `TenantCreatedEventConsumer` + `RetentionLifecycleServiceImpl` (đã có try/finally — EXEMPT)

## Log

- **2026-06-10 (LMS BE security wave):** Fix shipped — `getLessonPublic` wrap try/finally capture+restore `previousTenant` (clear cho guest). `findLessonWithTenantContext` thêm javadoc note "caller MUST restore". Unit test 2-case PASS (`LmsServiceTest` guest-cleared + preExisting-restored) + `@AfterEach TenantContext.clear()` hygiene. Status 🟡 PARTIAL ~85% — code + test PASS; **residual:** runtime verify thread-leak (2 request liên tiếp khác tenant) trước DONE flip.
