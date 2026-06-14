# GAP-1308: Gateway default-filters không strip X-User-Roles → role-spoof privilege escalation qua gateway

**Status:** 🟡 PARTIAL (code+config+CI-config-test DONE; runtime forged-header→403 walk pending)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-14 (security full audit post wave-p0-closeout-1 — AUDIT-2026-06-14-security-full, F-001)
**Affects:** `kitehub-gateway` (edge duy nhất cho cả KH + KC) + downstream `kiteclass-core` + `kitehub-subscription`

## Problem

Gateway `default-filters` (`kitehub/kitehub-gateway/src/main/resources/application.yml:965-973`) strip 4 identity header client-supplied — `X-Tenant-Id`, `X-User-Id`, `X-User-Reference-Id`, `X-Subscription-Tier` — và re-inject từ claim JWT đã verify (pattern anti-spoof đúng, GAP-814/1020). NHƯNG **bỏ sót `X-User-Roles`** (và `X-User-Email`).

`JwtAuthenticationGatewayFilter` chỉ set `X-User-Roles` khi `role != null` (L219-221); với request **không có Bearer token** thì filter **pass-through** (L159-161: `if (token == null) return chain.filter(exchange);`) — KHÔNG 401, KHÔNG set X-User-Roles. Downstream `kiteclass-core/.../config/GatewayHeaderAuthenticationFilter.java:69-90` dựng Spring authority TRỰC TIẾP từ header `X-User-Roles`; `kitehub-subscription` `XUserRolesHeaderFilter` (GAP-706/783) cũng vậy. KC `SecurityConfig.java:52-53` = `.anyRequest().permitAll()` ở URL layer → authz CHỈ ở method-layer `@PreAuthorize`.

**Hệ quả (priv-esc qua chính gateway, không phải direct-to-core):**
```
curl -H 'X-User-Roles: OWNER' https://<gateway>/api/v1/<role-only-gated-endpoint>
→ default-filters strip X-User-Id/X-Tenant-Id/X-User-Reference-Id/X-Subscription-Tier
→ JwtAuthenticationGatewayFilter: no token → pass-through (no 401, no X-User-Roles set)
→ core GatewayHeaderAuthenticationFilter: forged X-User-Roles=OWNER → cấp ROLE_OWNER (principal "gateway-user")
→ @PreAuthorize("hasAnyRole('ADMIN','OWNER')") PASS
```

**Mitigating factors:** endpoint tenant-scoped còn cần X-Tenant-Id (đã strip → TENANT_CONTEXT_MISSING); endpoint per-resource dùng X-User-Reference-Id (đã strip). Subset khai thác = endpoint role-only-gated, không bind tenant/resource. **Không** được mitigate bởi network-isolation (GAP-825) vì lỗ hổng ở chính gateway forward header client.

Cùng class GAP-814 (X-Tenant-Id strip, P0 DONE) — fix đó lẽ ra phải thêm X-User-Roles cùng lúc.

## Proposed Fix

1. Thêm `- RemoveRequestHeader=X-User-Roles` (và `- RemoveRequestHeader=X-User-Email`) vào gateway `default-filters`.
2. JwtAuthenticationGatewayFilter set `X-User-Roles` **vô điều kiện** trên token hợp lệ — khi claim `role` thiếu thì set rỗng/none-role (least-privilege), không để client value sống sót.
3. Cân nhắc 401 cho request tokenless tới path non-public (thay vì pass-through) — hiện pass-through để optionally-authed hoạt động; cần đảm bảo không kèm header authority giả.

## Acceptance Criteria

- [x] `X-User-Roles` + `X-User-Email` có trong gateway `default-filters` RemoveRequestHeader. (`application.yml` — 2 dòng RemoveRequestHeader thêm sau X-Subscription-Tier).
- [ ] Request gửi `X-User-Roles: OWNER` không token → core/sub KHÔNG cấp authority OWNER (403 trên endpoint role-gated). ⏳ **runtime walk pending** — cần boot gateway + downstream stub, gửi forged header, assert 403. Cơ chế strip đạt được điều này nhưng chưa walk e2e trên stack sống trong session này.
- [x] Request token hợp lệ → X-User-Roles luôn = role từ claim verify (client value bị strip + JWT filter re-inject ở Order `LOWEST_PRECEDENCE-2`, sau strip — cùng pattern đã proven cho X-User-Id/X-Subscription-Tier).
- [x] Regression test (CI-bound) ở gateway: `GatewayRoutesIntegrationTest.defaultFiltersStripAllClientIdentityHeaders` assert default-filters chứa cả 6 RemoveRequestHeader (Roles/Email mới + 4 twin). Assertion hành vi forged-header→403 thuộc runtime walk ở AC #2.

## Resolution (PR fix/audit-fixA-gw-2026-06-14, 2026-06-15)

Code-fix PARTIAL — cơ chế anti-spoof đã hoàn tất ở tầng gateway config + CI test; còn chờ runtime walk e2e cho assertion 403.

- **Fix:** thêm `- RemoveRequestHeader=X-User-Roles` (+ `X-User-Email` cho GAP-1310) vào `kitehub/kitehub-gateway/src/main/resources/application.yml` `default-filters`, ngay sau `X-Subscription-Tier`. Đây chính là twin còn thiếu của GAP-814 (X-Tenant-Id).
- **Vì sao chỉ cần strip:** `JwtAuthenticationGatewayFilter` chạy ở Order `Ordered.LOWEST_PRECEDENCE-2` (GAP-916) — SAU khi default-filters strip (Order ~0). Strip xoá client value trước; filter chỉ re-inject `X-User-Roles` khi claim `role != null`. Request tokenless / role-absent → downstream KHÔNG nhận X-User-Roles = least privilege (không cấp authority). Không cần đổi filter conditional injection.
- **Test:** `GatewayRoutesIntegrationTest.defaultFiltersStripAllClientIdentityHeaders` (CI-bound `*Test`, YAML-text assertion — pattern repo dùng cho default-filters verify). Gateway module `verify -P strict-warnings` BUILD SUCCESS (75 tests, 0 fail).
- **Còn lại (PARTIAL):** boot gateway + KC/KH stack, `curl -H 'X-User-Roles: OWNER'` tới endpoint role-gated không token → assert 403. Cần khi stack chạy.

## Related

- Discovered in: AUDIT-2026-06-14-security-full F-001 (EVIDENCE-2026-06-14-AUTH-001). Reserved gap-ID per `multi-session-concurrency-coordination.md`.
- GAP-814 (X-Tenant-Id strip, P0 DONE) — precedent cùng class, chưa phủ X-User-Roles.
- GAP-825 (tenant-isolation hardening, OPEN) — defense-in-depth bổ trợ.
- GAP-1310 (X-User-Email strip, P2) — sibling cùng evidence.
- `cross-flow-bug-class-sweep.md` — strip-pattern cần sweep mọi inject-header (X-User-Id/Tenant/Reference/Tier/Roles/Email).
