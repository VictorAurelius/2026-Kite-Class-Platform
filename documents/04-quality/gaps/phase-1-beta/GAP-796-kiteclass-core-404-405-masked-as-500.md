---
audience: dev
---

# GAP-796 — kiteclass-core mask 404/405 thành `500 SYSTEM_INTERNAL_ERROR`

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-28 (Full regression RST walk — owner-ops + teacher-parent-student clusters độc lập confirm)
**Phase:** phase-1-beta
**Affects:** /classes, /teacher/dashboard, /parent, /attendance, /grades, /invoices, /payments base paths (route-mismatch bị che dưới vỏ "internal error")

## Problem

kiteclass-core `GlobalExceptionHandler` nuốt `NoHandlerFoundException` (route không tồn tại → đáng lẽ 404) + `HttpRequestMethodNotSupportedException` (method sai → đáng lẽ 405) → trả `HTTP 500 SYSTEM_INTERNAL_ERROR`.

Hệ quả: debug khó — endpoint-missing/method-mismatch bị disguise thành "system error", làm walk/test hiểu nhầm là server crash thay vì route chưa implement. Cả 2 RST agent (owner-ops + teacher-parent-student) độc lập gặp pattern này khi probe base paths.

## Root Cause (cần investigate)

`GlobalExceptionHandler` trong kiteclass-core có catch-all `@ExceptionHandler(Exception.class)` → 500 mà KHÔNG có handler riêng cho `NoHandlerFoundException` + `HttpRequestMethodNotSupportedException` (Spring throw 2 exception này cho 404/405). Cần thêm `throw-exception-if-no-handler-found: true` + dedicated handlers trả đúng status.

## Proposed Fix

- Thêm `@ExceptionHandler(NoHandlerFoundException.class)` → 404
- Thêm `@ExceptionHandler(HttpRequestMethodNotSupportedException.class)` → 405
- Set `spring.mvc.throw-exception-if-no-handler-found=true` + `spring.web.resources.add-mappings=false` (nếu chưa) để NoHandlerFoundException fire
- Đảm bảo catch-all `Exception.class` handler đứng SAU (ordering) 2 handler trên

## Acceptance Criteria

- [ ] GET route không tồn tại → HTTP 404 (không 500)
- [ ] Sai HTTP method trên route hợp lệ → HTTP 405 (không 500)
- [ ] Genuine internal error vẫn → 500 SYSTEM_INTERNAL_ERROR
- [ ] E2E/IT spec assert 404/405 cho 2 case trên

## Related

- Index: `documents/04-quality/audits/rst-html/2026-05-28-full-regression/INDEX.md`
- Cluster findings: `owner-ops.md` (P1 bug #2), `teacher-parent-student.md` (P1 bug #2)
- **GAP-570** (closed, "incomplete") — Spring 500 instead of 404 POST static not-found; GAP-796 = recurrence/extension cho kiteclass-core NoHandlerFound + method-not-supported
- **GAP-571** (closed, "pre-existing") — validation endpoints 500 instead 400; same exception-handler-completeness class

## Log

- **2026-05-28:** Filed từ Full regression RST walk per `audit-to-gap-pipeline.md` §3. State-check (§2.5): related-but-different scope vs GAP-570 (POST static not-found, closed incomplete) + GAP-571 (validation 400, closed). GAP-796 covers NoHandlerFoundException + HttpRequestMethodNotSupportedException trong kiteclass-core GlobalExceptionHandler — uncovered surface. 2 RST agent độc lập confirm.
