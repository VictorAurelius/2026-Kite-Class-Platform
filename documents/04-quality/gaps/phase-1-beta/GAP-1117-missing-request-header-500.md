# GAP-1117: Thiếu required @RequestHeader (X-User-Id/X-Teacher-Id) → HTTP 500 thay vì 400

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend (error-handling — kiteclass-core, cross-flow)
**Found:** 2026-06-10 (outside-in audit 3-lens FE LMS wave — failure-mode F3)
**Affects:** `kiteclass-core` `GlobalExceptionHandler` (toàn bộ controller dùng required `@RequestHeader`, vd `LessonProgressController` X-User-Id)

## Problem

Khi client/gateway không gửi required header (vd `@RequestHeader("X-User-Id") Long userId`), Spring nem `MissingRequestHeaderException`. Handler này KHÔNG có dedicated `@ExceptionHandler` → rơi vào catch-all `handleUnexpectedException(Exception.class)` → HTTP **500** + body generic `SYSTEM_INTERNAL_ERROR`. Đây là client error (thiếu header), phải trả **400**, không phải server error 500.

Lưu ý: handler đã có `MissingServletRequestParameterException` (cho `@RequestParam`) trả 400, nhưng `MissingRequestHeaderException` là class KHÁC (subclass của `ServletRequestBindingException`, KHÔNG phải `MissingServletRequestParameterException`) nên không được cover.

## Root Cause

`GlobalExceptionHandler` thiếu `@ExceptionHandler(MissingRequestHeaderException.class)`. Catch-all `Exception.class` nuốt mọi exception chưa map → 500.

## Proposed Fix

Thêm `@ExceptionHandler(MissingRequestHeaderException.class)` vào `GlobalExceptionHandler` (đặt TRƯỚC catch-all) → trả 400 + errorCode `MISSING_HEADER` + message chứa tên header (`ex.getHeaderName()`). Fix global → cover mọi controller dùng required header (cross-flow, không chỉ LMS).

## Acceptance Criteria
- [x] GET endpoint thiếu `X-User-Id` → 400 với errorCode `MISSING_HEADER` (không phải 500)
- [x] Message chứa tên header bị thiếu
- [x] Unit test handler (`GlobalExceptionHandlerTest`)
- [ ] Runtime-walk: curl progress endpoint không gắn header → 400 (production-equivalent) trước DONE flip

## Related
- Audit report: `documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md` (F3)
- Sister fix cùng PR: GAP-1115, GAP-1116, GAP-1118
- Cùng họ với handlers GAP-777 (TenantNotSet 400) + GAP-796 (NoHandlerFound 404/405) — đều "client error không nên rơi catch-all 500"
- Cross-flow: fix global cover 32 controller dùng `@RequestHeader`

## Log

- **2026-06-10 (LMS BE security wave):** Fix shipped — thêm `handleMissingRequestHeader(MissingRequestHeaderException)` vào `GlobalExceptionHandler` trả 400 + `MISSING_HEADER` + tên header. Đặt trước catch-all. Cross-flow sweep: fix global cover mọi controller dùng required `@RequestHeader` (32 controller). Unit test PASS (`GlobalExceptionHandlerTest.handleMissingRequestHeader_shouldReturn400`). Status 🟡 PARTIAL ~90% — code + test PASS; **residual:** curl runtime verify trên stack trước DONE flip.
