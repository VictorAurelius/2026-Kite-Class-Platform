# GAP-1308: Gateway default-filters không strip X-User-Roles → role-spoof privilege escalation qua gateway

**Status:** 🔵 OPEN
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

- [ ] `X-User-Roles` + `X-User-Email` có trong gateway `default-filters` RemoveRequestHeader.
- [ ] Request gửi `X-User-Roles: OWNER` không token → core/sub KHÔNG cấp authority OWNER (403 trên endpoint role-gated).
- [ ] Request token hợp lệ → X-User-Roles luôn = role từ claim verify (client value bị ghi đè/strip).
- [ ] Regression test (CI-bound) ở gateway và/hoặc downstream filter: forged X-User-Roles bị từ chối; token-derived role được tôn trọng.

## Related

- Discovered in: AUDIT-2026-06-14-security-full F-001 (EVIDENCE-2026-06-14-AUTH-001). Reserved gap-ID per `multi-session-concurrency-coordination.md`.
- GAP-814 (X-Tenant-Id strip, P0 DONE) — precedent cùng class, chưa phủ X-User-Roles.
- GAP-825 (tenant-isolation hardening, OPEN) — defense-in-depth bổ trợ.
- GAP-1310 (X-User-Email strip, P2) — sibling cùng evidence.
- `cross-flow-bug-class-sweep.md` — strip-pattern cần sweep mọi inject-header (X-User-Id/Tenant/Reference/Tier/Roles/Email).
