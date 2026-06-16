# GAP-1439: Anonymous DSAR bị 401 — SecurityConfig khóa /api/v1/dsar/** authenticated() mâu thuẫn controller "unauthenticated by design"

**Status:** 🟢 DONE
**Priority:** 🔴 P1
**Domain:** Backend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-8)
**Affects:** KH-8 — `kitehub-subscription/.../config/SecurityConfig.java:172` vs `DsarController.java:34-38`

## Problem
Discovered Phase-2 browser walk KH-8. `SecurityConfig.java:172` đặt `.requestMatchers("/api/v1/dsar/**").authenticated()`, mâu thuẫn javadoc `DsarController` ("unauthenticated by design") + form legal public anonymous. Anonymous DSAR request → 401.

## Proposed Fix
`permitAll` cho `POST /api/v1/dsar/request` + `GET /api/v1/dsar/{ticketId}` (mirror consent `permitAll` lines 120-125), giữ phần còn lại default-deny. Identity verify out-of-band qua `national_id_last4` + DPO callback (đúng controller design) + honeypot + gateway rate-limit. Nếu CHỦ Ý yêu cầu auth → phải sửa controller javadoc + FE form (bỏ intake anonymous name/email/CCCD).

## Acceptance Criteria
- [x] POST /api/v1/dsar/request anonymous → 201 (không 401)
- [x] GET /api/v1/dsar/{ticketId} anonymous tra cứu được ticket của chính mình
- [x] Phần còn lại /api/v1/dsar/** giữ default-deny

## Fix (fix implemented, pending re-walk — Phase-3 Bucket A, branch fix/phase3-bucketA-kh8-dsar)
- `SecurityConfig.java:172` bỏ blanket `.requestMatchers("/api/v1/dsar/**").authenticated()`. Thay bằng HttpMethod-specific permitAll mirror consent carve-out (lines 120-125):
  - `POST /api/v1/dsar/request` → permitAll (anonymous submit, identity verify out-of-band per BR-PDPL-DSAR-003)
  - `GET /api/v1/dsar/*` → permitAll (1-segment ticket lookup; response redacted public-safe per controller javadoc)
  - Mọi path dsar khác (2-segment future admin export, POST non-/request) → rơi xuống `anyRequest().authenticated()` default-deny.
- `SecurityConfigTest.java` `TestSecurityChain` stub sync (in-sync mandate); CsvSource matrix cập nhật: `GET /api/v1/dsar/*` → 404 (pass-auth), `GET /api/v1/dsar/admin/export` (2-segment) → 401. 3 @Test mới (anonymous POST /request NOT-401, GET {ticketId} NOT-401, POST /{id} stays-401).
- Gateway KHÔNG cần đổi: `JwtAuthenticationGatewayFilter` pass-through request không-JWT (line 159-160); 401 chỉ khi Bearer token hiện diện và invalid. DSAR anonymous form không gửi token.
- Test: `SecurityConfigTest` 29/29 PASS (`./mvnw -pl kitehub-subscription test -Dtest=SecurityConfigTest`).
- Verify còn lại: G2 browser walk thật end-to-end (FE → gateway → subscription → DB row PENDING) — pending human re-walk.

## Related
- Discovered in: Phase-2 browser walk (flow KH-8), 2026-06-16
- Cùng cluster FE: GAP-1438 (fixed same PR)
- Pattern mirror: consent anonymous carve-out GAP-794 (SecurityConfig lines 120-125)
