# GAP-1337: Cross-service error envelope inconsistency — kiteclass ErrorResponse vs kitehub RFC 7807 ProblemDetail

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (API-contract full audit, AUDIT-2026-06-14-api-contract-full)
**Affects:** `GlobalExceptionHandler` (kitehub-subscription + kiteclass-core) + api-contract.md error sections

## Problem

Hai service chính trả error body theo 2 shape khác nhau (Cat 3.3 FAIL P1):

- **kitehub-subscription** dùng RFC 7807 `ProblemDetail` (`application/problem+json`) — `GlobalExceptionHandler.java:9,44` (`ProblemDetail.forStatusAndDetail`).
- **kiteclass-core** dùng custom `ErrorResponse` DTO (`code`/`message`/`path`/`fieldErrors`) — `GlobalExceptionHandler.java:3,48` (`ErrorResponse.of`).

api-contract.md không mô tả thống nhất envelope nào áp dụng cho service nào → consumer (FE error-handling, mobile, partner) phải parse 2 format khác nhau tùy service, không có contract rõ. Khác biệt cũng tăng rủi ro FE generic-catch (đã thấy ở GAP-926 class).

## Root Cause

2 service phát triển độc lập, chọn error-envelope khác nhau; không có cross-service error-contract standard.

## Proposed Fix

(1) Quyết định canonical envelope (đề xuất RFC 7807 ProblemDetail cho cả 2, hoặc document rõ ràng mỗi service dùng gì + lý do). (2) Nếu giữ 2 shape → document envelope trong mỗi api-contract.md (section "Error envelope" + ví dụ JSON). (3) Cân nhắc ADR cho error-contract standard.

## Acceptance Criteria

- [ ] Canonical error envelope quyết định (ADR hoặc rules.md) — 1 shape, hoặc 2 shape documented rõ per-service
- [ ] api-contract.md (cả kitehub + kiteclass) có section "Error envelope" mô tả body schema
- [ ] FE error-handling thống nhất parse theo contract documented

## Resolution

🟡 PARTIAL (2026-06-15, branch `fix/audit-fixC-apidocs-2026-06-14`). DOCUMENT canonical + drift (code migration = breaking change → defer):
- `kitehub/subscription-billing/api-contract.md` §"Error envelope (cross-service contract)": kitehub = RFC 7807 `ProblemDetail` (`application/problem+json`); kiteclass = custom `ErrorResponse` (`code/message/path/fieldErrors`); ví dụ JSON cả 2 + ngoại lệ `SsoController` (map custom `{error,message}`).
- `kiteclass/tenant-auth/api-contract.md` §4.5: shape `ErrorResponse` cho FE parse.
- `kitehub/sso/api-contract.md` §Error envelope: ghi nhận ngoại lệ SsoController.
- Canonical target = RFC 7807 cho cả 2 service.

Plan (defer — wave riêng): migrate kiteclass `ErrorResponse` → `ProblemDetail` + SsoController map → ProblemDetail; FE adapt đồng thời (breaking cho FE error-handling đang parse `code/message/fieldErrors`); cân nhắc ADR error-contract standard.

AC #2 ✅ (api-contract.md cả 2 service + SSO có section Error envelope); AC #1 PARTIAL (canonical decided + recorded, code chưa unify); AC #3 deferred (FE parse vẫn per-service). PARTIAL vì code-level unification chưa thực hiện (chỉ document + decision).

## Related

- Discovered in: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` B5
- Rubric: `audit-skill-rubric-api-contract-audit.md` §2.3 check 3.3
- Related: GAP-926 (FE generic catch class)
