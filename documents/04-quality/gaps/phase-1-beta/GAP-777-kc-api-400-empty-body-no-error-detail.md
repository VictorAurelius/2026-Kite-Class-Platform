---
audience: dev
---

# GAP-777 — KC API 400 Bad Request returns empty body (no error detail)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B5-B11 probe)
**Affects:** Mọi endpoint `/api/v1/*` (KC) — FE error-message rendering
**Phase:** phase-1-beta

## Problem

Reproduce với owner.test JWT (Owner role nhưng `tenant_id IS NULL` trong DB):

```bash
curl -i -H "Authorization: Bearer $OWNER_TOKEN" http://localhost:9000/api/v1/students
# HTTP/1.1 400 Bad Request
# Vary: Origin
# Vary: Access-Control-Request-Method
# Vary: Access-Control-Request-Headers
# content-length: 0    ← empty body, no error detail
```

Same pattern across 19 endpoints probed (students/teachers/courses/classes/attendance/billing/payments/reports/settings/etc.).

FE consumer khi gặp 400 empty body:
- Không có error message để render
- Phải hiển thị generic "Lỗi không xác định" → user confused
- Debug session: dev phải tail BE logs để hiểu vì sao 400

Đối chiếu với existing KH endpoints: `/api/auth/login` 400 returns RFC 7807 Problem Details JSON với `detail` field:
```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Invalid email or password","instance":"/api/auth/login"}
```

→ Inconsistency: KH side has RFC 7807; KC side returns empty.

## Root Cause (verified 2026-06-02, fix-time state-check per audit-to-gap-pipeline.md §2.8)

State-check empirical tại code level (gap age 6 ngày, không chạy live stack — owner.test fixture cần stack up):

1. `GlobalExceptionHandler` (`@RestControllerAdvice`) **đã có** structured `ErrorResponse` JSON cho hầu hết exception (`BusinessException`, `EntityNotFoundException`, `DuplicateResourceException`, `ValidationException`, `MethodArgumentNotValidException`, `IllegalArgumentException`, `HttpMessageNotReadableException`, `NoHandlerFound` 404, `MethodNotSupported` 405, catch-all 500). Wave 106 GAP-796 (#1946) + GAP-611 (#1827) đã wire phần lớn.
2. `TenantFilterInterceptor.preHandle` KHÔNG trả 400 — chỉ set context + `return true` (không có X-Tenant-Id → context không set, request đi tiếp).
3. **Root cause chính**: `TenantNotSetException extends RuntimeException` — KHÔNG có `@ResponseStatus`, KHÔNG có dedicated `@ExceptionHandler` trong `GlobalExceptionHandler` → rơi vào catch-all `handleUnexpectedException(Exception.class)` → trả **500** (KHÔNG phải 400) + message generic `SYSTEM_INTERNAL_ERROR` (KHÔNG có lý do "thiếu tenant"). Đây là semantic-wrong path khi owner.test (`tenant_id IS NULL`) hit endpoint tenant-scoped → service gọi `TenantContext.getCurrentTenant()` → throw → error vô dụng cho FE.

Lưu ý: gap symptom mô tả "400 content-length:0" trên `localhost:9000` (gateway port). Đường chính xác sinh "400 empty body" (gateway/nginx layer vs core 500) chưa xác minh được vì cần stack up + owner.test JWT. Phần đã fix có thẩm quyền tại code level là semantic-wrong `TenantNotSetException` → 500/generic. `kiteclass-gateway` đã decommission (0 java files, chỉ target/ leftover).

## Proposed Fix

1. Verify KC `GlobalExceptionHandler` (hoặc `@ControllerAdvice`) handles `BadRequestException` + missing tenant với Problem Details JSON response
2. Tenant resolver filter (nếu return 400 sớm): trả structured JSON `{"detail":"Tenant context missing — please complete onboarding wizard","status":400}` thay vì empty
3. FE consumer test: error toast render đúng tiếng Việt từ `detail` field
4. Integration test: 400 response có `application/problem+json` content-type + non-empty body

## Acceptance Criteria

- [x] KC error responses include structured JSON body (`code` + `message`) — `TenantNotSetException` giờ trả 400 `TENANT_NOT_SET` thay vì 500/generic; mọi exception class khác đã có structured `ErrorResponse` (verified code-level + 11/11 `GlobalExceptionHandlerTest` PASS)
- [ ] FE catches structured error + renders Vietnamese error message (per `vn-localization-audit-checklist.md` §2) — FE-side change, defer follow-up
- [ ] Live verify: owner.test (`tenant_id IS NULL`) hit `/api/v1/students` trả 400 + non-empty body trên running stack (5+ endpoint samples) — cần stack up + owner.test JWT; deferred (gap age fix-time state-check không chạy live)
- [ ] (Optional) Full RFC 7807 `application/problem+json` content-type — hiện dùng project `ErrorResponse` shape (code/message/path/timestamp); RFC 7807 alignment là scope riêng nếu cần parity với KH side

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 — code-level, live deferred)

- Stack-up: KHÔNG chạy (fix-time state-check tại code level, gap age 6 ngày)
- Test: `GlobalExceptionHandlerTest` 11/11 PASS (Tests run: 11, Failures: 0, Errors: 0) — gồm 2 test mới:
  - `handleTenantNotSet_shouldReturn400WithStructuredBody`: 400 + code `TENANT_NOT_SET` + non-empty message + path + timestamp
  - `handleTenantNotSet_shouldFallBackToCodeWhenMessageMissing`: code làm fallback message khi bundle thiếu key → body vẫn non-empty
- Message bundle: `messages.properties` + `messages_vi.properties` đã có key `TENANT_NOT_SET` (en + vi) → i18n ready
- Live owner.test walk: DEFER (cần running stack + owner.test JWT) — `FEATURE_SHIP_WALK_DEFER` semantics

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** Custom exception `extends RuntimeException`, không `@ResponseStatus` + không dedicated `@ExceptionHandler` → rơi catch-all 500/generic thay vì structured client-error.

**Sweep command:**
```bash
for f in kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/exception/*Exception.java; do
  name=$(basename "$f" .java)
  grep -c "@ExceptionHandler($name" .../GlobalExceptionHandler.java
done
```

**Sites found (6 custom exceptions):**

| Exception | Verdict | Reason |
|---|---|---|
| `TenantNotSetException` | **FIX** | extends RuntimeException, no handler → 500/generic. Fixed this PR → 400 structured |
| `BusinessException` | EXEMPT | Has dedicated handler (`ex.getStatus()` + structured body) |
| `EntityNotFoundException` | EXEMPT | Dedicated handler → 404 structured |
| `DuplicateResourceException` | EXEMPT | Dedicated handler → 409 structured |
| `ValidationException` | EXEMPT | Dedicated handler → 400 structured |
| `PermissionDeniedException` | EXEMPT | `extends BusinessException` với `HttpStatus.FORBIDDEN` → handled transitively (403 structured) |

**Decision:** FIXED 1 (TenantNotSetException), EXEMPT 5. Sweep complete — TenantNotSetException là exception duy nhất trong bug class.

## Related

- Wave 106 RST B5-B11 probe evidence
- Sister KH RFC 7807 surface (per Wave 83 post-wave API contract audit 82/100)
- `api-contract.md` audit standard per `output-review-mandate.md` §3
- Wave 106 GAP-796 (#1946) + GAP-611 (#1827) — prior structured-error wiring

## Log

- **2026-06-02** (PARTIAL): Fix-time state-check per `audit-to-gap-pipeline.md` §2.8 — gap diagnostic "handler chưa wire RFC 7807" partly stale (GAP-796 + GAP-611 đã wire phần lớn structured-error). Root cause thực = `TenantNotSetException` rơi catch-all 500/generic. Fix: thêm `@ExceptionHandler(TenantNotSetException.class)` → 400 + structured JSON `TENANT_NOT_SET` (i18n message bundle đã có key). TDD: 2 test mới, `GlobalExceptionHandlerTest` 11/11 PASS. Cross-flow sweep: 1 FIX + 5 EXEMPT (PermissionDeniedException covered transitively). PARTIAL vì: (a) live owner.test walk trên running stack chưa chạy (gap age fix-time), (b) FE rendering AC = FE-side scope, (c) gateway/nginx-layer "400 empty body" path chưa xác minh live (gateway decommissioned, symptom có thể từ nginx). Code PR — no self-merge.
