# GAP-1310: Gateway default-filters không strip X-User-Email → header email client giả mạo được

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (security full audit post wave-p0-closeout-1 — AUDIT-2026-06-14-security-full, F-003)
**Affects:** `kitehub-gateway` + downstream services đọc `X-User-Email`

## Problem

Giống GAP-1308 nhưng cho header `X-User-Email`: gateway `default-filters` (`application.yml:965-973`) KHÔNG strip `X-User-Email`. `JwtAuthenticationGatewayFilter` chỉ set `X-User-Email` khi claim `email != null` (L223-225); khi token thiếu claim email, hoặc request tokenless pass-through, **client-supplied X-User-Email lọt xuống downstream**.

Khác F-001 ở chỗ X-User-Email KHÔNG dùng cho authz (không build authority) → không phải priv-esc. Nhưng có thể đầu độc:
- Audit log / login-audit nếu ghi email từ header.
- Luồng nghiệp vụ dựa email (notification target, display name, attribution).

Tách khỏi GAP-1308 vì surface + severity khác (1 gap = 1 finding per `audit-to-gap-pipeline.md`).

## Proposed Fix

Thêm `- RemoveRequestHeader=X-User-Email` vào gateway `default-filters`; JwtAuthenticationGatewayFilter set X-User-Email vô điều kiện từ claim verify (rỗng khi thiếu claim). Sweep cùng GAP-1308 (cùng file, cùng pattern).

## Acceptance Criteria

- [ ] `X-User-Email` có trong gateway `default-filters` RemoveRequestHeader.
- [ ] Client-supplied X-User-Email không token / token thiếu claim → downstream nhận rỗng, không phải value client.
- [ ] Token có claim email → X-User-Email = email verify.

## Related

- Discovered in: AUDIT-2026-06-14-security-full F-003 (EVIDENCE-2026-06-14-AUTH-001). Reserved gap-ID per `multi-session-concurrency-coordination.md`.
- GAP-1308 (X-User-Roles strip, P0) — sibling cùng file/pattern; nên fix chung 1 PR.
- GAP-814 (X-Tenant-Id strip precedent).
