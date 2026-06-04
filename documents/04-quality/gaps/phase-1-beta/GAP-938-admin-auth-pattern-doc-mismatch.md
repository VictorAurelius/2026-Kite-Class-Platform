# GAP-938: Admin auth pattern doc-vs-code mismatch (`X-Admin-Key` interceptor vs `X-User-Roles` gateway forward)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 G1 walk — AdminPaymentController gọi qua gateway HTTP 401 dù dev-mode AdminApiKeyInterceptor cho phép)
**Affects:** Mọi admin endpoint trong `kitehub-subscription` + tài liệu G2 recipe + bất kỳ admin client nào dùng X-Admin-Key thay vì JWT qua gateway

## Problem

`kitehub-subscription` có 2 cơ chế auth chồng nhau cho `/api/platform/admin/**`:

1. **AdminApiKeyInterceptor** (`config/AdminApiKeyInterceptor.java:24-46`): kiểm tra header `X-Admin-Key` so với env `kitehub.admin.api-key`; nếu key rỗng → "dev mode allow all". Javadoc nói "In production, set ADMIN_API_KEY env var."
2. **SecurityConfig + XUserRolesHeaderFilter**: Spring Security đọc header `X-User-Id` + `X-User-Roles` (gateway forward), tạo `UsernamePasswordAuthenticationToken` với `ROLE_<role>`, default-deny `anyRequest().authenticated()` per GAP-552 (Wave 79). Method-level `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` khi có annotation.

Mismatch: AdminPaymentController (Wave flow-kh3 PR #2150) KHÔNG có `@PreAuthorize` (Agent A theo pattern `AdminMigrationController` — không method-level annotation). Spring Security default-deny vẫn yêu cầu authenticated user. AdminApiKeyInterceptor chạy SAU Spring Security filter → đến lúc interceptor "allow all" thì request đã bị Spring chặn 401.

Walk evidence 2026-06-04 G1:
- `curl -X POST :9000/api/platform/admin/payments/<id>/confirm -d '{}' ` → HTTP 401 (gateway forward không có Authorization header)
- `curl -X POST :8081/... ` (bypass gateway) cũng 401
- `curl -X POST :8081/... -H "X-User-Id: ..." -H "X-User-Roles: PLATFORM_ADMIN" -d '{}'` → HTTP 200 ✅ (mô phỏng gateway header)

→ Đường thực sự xác thực admin là JWT có `role=PLATFORM_ADMIN` đi qua gateway, KHÔNG phải X-Admin-Key. AdminApiKeyInterceptor là code dead-end / hiểu nhầm legacy.

## Root Cause

Wave 79 GAP-552 default-deny migration đổi `anyRequest().permitAll()` → `anyRequest().authenticated()`. AdminApiKeyInterceptor được viết trước thay đổi đó, giả định Spring Security cho phép request đi qua tới interceptor. Sau Wave 79, interceptor trở thành no-op trong path admin (vì JWT chain block trước).

Documentation drift: AdminApiKeyInterceptor javadoc + `kitehub.admin.api-key` config không có warning rằng cơ chế đã supersede bởi JWT/PLATFORM_ADMIN role.

## Proposed Fix

Option A (đề xuất — minimal): Xóa AdminApiKeyInterceptor hoàn toàn + cập nhật doc:
1. Delete `AdminApiKeyInterceptor.java` + WebMvc registration của nó (grep `addInterceptor`).
2. Remove `kitehub.admin.api-key` từ `application.yml` + docker-compose env nếu có.
3. Update `api-contract.md` mọi endpoint admin: ghi rõ "yêu cầu JWT với role PLATFORM_ADMIN qua gateway; gateway forward `X-User-Id` + `X-User-Roles` headers".
4. Audit mọi AdminController hiện có (AdminMigrationController, AdminPaymentController, …) → thêm explicit `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` annotation cho mỗi method.

Option B: Giữ AdminApiKeyInterceptor như fallback emergency path nhưng:
1. Whitelist admin paths trong SecurityConfig (permitAll cho admin path thì interceptor mới chạy được).
2. Set strong default ADMIN_API_KEY trong production env.
3. Risk: 2 cơ chế auth chồng = surface attack tăng.

Option A clean hơn cho Phase 1 BETA.

## Acceptance Criteria

- [ ] AdminApiKeyInterceptor xóa khỏi codebase HOẶC documented rõ là supersede + path-scoped chỉ áp dụng cho whitelist không bao gồm admin.
- [ ] Mỗi admin controller (`AdminPaymentController`, `AdminMigrationController`, các admin khác) có `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` method-level.
- [ ] `api-contract.md` mọi admin endpoint ghi auth = JWT PLATFORM_ADMIN role qua gateway (không X-Admin-Key).
- [ ] G2 recipe documents/05-guides/operations/2026-06-04-g2-recipe-kh3-subscription.md cập nhật bước admin confirm dùng JWT của admin user, không X-Admin-Key.
- [ ] CI grep detector check không có @RestController admin nào thiếu @PreAuthorize (defer follow-up gap nếu chưa làm v1).

## Related

- Tiền lệ: GAP-552 (default-deny migration Wave 79)
- Tiền lệ: GAP-384 (admin endpoints unauthenticated despite javadoc — SecurityConfig javadoc ghi)
- Pattern reference: `AdminMigrationController` (Wave 35) — cùng pattern không-annotation mà Agent A của Wave flow-kh3 copy
- Discovered via: Wave flow-kh3 G1 walk 2026-06-04 (PR #2150 AdminPaymentController shipped pre-walk)
- Rule cite: `.claude/rules/audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync (Wave 79 SecurityConfig change → docs+controllers PHẢI sync — gap này là 1 stale ref còn sót)
