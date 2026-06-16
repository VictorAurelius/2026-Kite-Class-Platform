# GAP-1439: Anonymous DSAR bị 401 — SecurityConfig khóa /api/v1/dsar/** authenticated() mâu thuẫn controller "unauthenticated by design"

**Status:** 🔵 OPEN
**Priority:** 🔴 P1
**Domain:** Backend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-8)
**Affects:** KH-8 — `kitehub-subscription/.../config/SecurityConfig.java:172` vs `DsarController.java:34-38`

## Problem
Discovered Phase-2 browser walk KH-8. `SecurityConfig.java:172` đặt `.requestMatchers("/api/v1/dsar/**").authenticated()`, mâu thuẫn javadoc `DsarController` ("unauthenticated by design") + form legal public anonymous. Anonymous DSAR request → 401.

## Proposed Fix
`permitAll` cho `POST /api/v1/dsar/request` + `GET /api/v1/dsar/{ticketId}` (mirror consent `permitAll` lines 120-125), giữ phần còn lại default-deny. Identity verify out-of-band qua `national_id_last4` + DPO callback (đúng controller design) + honeypot + gateway rate-limit. Nếu CHỦ Ý yêu cầu auth → phải sửa controller javadoc + FE form (bỏ intake anonymous name/email/CCCD).

## Acceptance Criteria
- [ ] POST /api/v1/dsar/request anonymous → 201 (không 401)
- [ ] GET /api/v1/dsar/{ticketId} anonymous tra cứu được ticket của chính mình
- [ ] Phần còn lại /api/v1/dsar/** giữ default-deny

## Related
- Discovered in: Phase-2 browser walk (flow KH-8), 2026-06-16
- Cùng cluster FE: GAP-1438
