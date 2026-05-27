---
audience: dev
---

# GAP-777 — KC API 400 Bad Request returns empty body (no error detail)

**Status:** 🔵 OPEN
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

## Root Cause

Suy đoán: KC global exception handler chưa được wire RFC 7807 Problem Details support. Hoặc tenant-resolver filter trả 400 trước khi reach handler. Cần investigate `kiteclass-core/.../config/GlobalExceptionHandler.java` + tenant filter.

## Proposed Fix

1. Verify KC `GlobalExceptionHandler` (hoặc `@ControllerAdvice`) handles `BadRequestException` + missing tenant với Problem Details JSON response
2. Tenant resolver filter (nếu return 400 sớm): trả structured JSON `{"detail":"Tenant context missing — please complete onboarding wizard","status":400}` thay vì empty
3. FE consumer test: error toast render đúng tiếng Việt từ `detail` field
4. Integration test: 400 response có `application/problem+json` content-type + non-empty body

## Acceptance Criteria

- [ ] KC 400 responses include RFC 7807 Problem Details JSON với `detail` field
- [ ] FE catches `detail` + renders Vietnamese error message (per `vn-localization-audit-checklist.md` §2)
- [ ] Integration test: `expect(response.body.detail).toBeTruthy()` across 5+ endpoint samples

## Related

- Wave 106 RST B5-B11 probe evidence
- Sister KH RFC 7807 surface (per Wave 83 post-wave API contract audit 82/100)
- `api-contract.md` audit standard per `output-review-mandate.md` §3
