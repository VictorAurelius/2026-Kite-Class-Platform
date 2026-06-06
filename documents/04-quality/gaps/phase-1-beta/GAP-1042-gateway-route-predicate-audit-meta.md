# GAP-1042: META — Gateway route-predicate audit (broad predicates shadow/expose wrong service, 3 recurrences)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META — force-multiplier per `meta-gap-priority.md` §3)
**Domain:** Backend (gateway) — meta/systemic
**Found:** 2026-06-06 (KC-12 G1 walk — 3rd recurrence triggered meta filing)
**Affects:** `kitehub-gateway` route table (application.yml) — systemic

## Problem

3 routing-collision/exposure bug đã surface trong campaign Flow Verification, tất cả cùng **root: gateway route predicate quá rộng** — predicate bắt nhiều path hơn service sở hữu → shadow hoặc expose nhầm:

| Gap | Route (over-broad predicate) | Hậu quả |
|---|---|---|
| **GAP-1031** | `platform-email` `Path=/api/platform/emails/**` (:295) | Expose internal email API ra public (anon arbitrary send) |
| **GAP-1034** | `kitehub-branding-v1` `Path=/api/v1/branding/**` (:593) | Shadow 3/5 kiteclass branding controller → login default theme |
| **GAP-1041** | `kitehub-admin-v1` `Path=/api/v1/admin/**` (:551) | Shadow kiteclass payroll → 404 |

Mỗi route declare predicate catch-all `/**` cho prefix mà service KHÔNG own toàn bộ. Các path kiteclass-core dùng cùng prefix (`/api/v1/branding/public`, `/api/v1/admin/payroll`) bị shadow. Các internal-only path (`/api/platform/emails`) bị expose. Đây là systemic — sẽ tái diễn cho mọi prefix kiteclass + kitehub chia sẻ.

## Root Cause

Gateway route table thiếu **predicate discipline**: route nên match đúng paths service own, không catch-all prefix. Hiện có một số precedent đúng (`kitehub-admin-beta-requests-v1` :525 + `kitehub-admin-impersonate` :539 declare TRƯỚC catch-all) nhưng không áp dụng nhất quán.

## Proposed Fix (systemic audit)

1. **Audit toàn bộ gateway route table** — list mọi route với predicate `Path=.../**` catch-all; với mỗi cái, verify service đích thực sự own TẤT CẢ paths dưới prefix.
2. **Narrow predicates** — đổi catch-all sang explicit path-list cho route mà prefix chia sẻ giữa services (vd kitehub-admin-v1 → list dashboard/instances/audit-logs; thêm kiteclass routes cho payroll/branding-public/versions).
3. **Route-ordering invariant** — explicit narrow routes PHẢI precede catch-all (đã có comment pattern :524 "Must precede kitehub-admin-v1 catch-all" — formalize).
4. **CI guard (optional)** — script verify mỗi `/api/v1/**` + `/api/platform/**` path trong codebase (grep @RequestMapping) route tới đúng service.
5. **Cross-flow sweep** — sau audit, re-walk các flow đã verify (KH-10/KC-10/KC-12) confirm routing fix không regress.

## Acceptance Criteria

- [ ] Audit artifact: mọi catch-all route + service-ownership verify
- [ ] Mọi kiteclass-core path dưới shared prefix (`/api/v1/admin/payroll`, `/api/v1/branding/{public,versions,package}`) route đúng kiteclass-core
- [ ] Internal-only path (`/api/platform/emails`) không expose public (GAP-1031)
- [ ] Route-ordering invariant documented + (optional) CI guard
- [ ] GAP-1031 + GAP-1034 + GAP-1041 closed bởi systemic fix

## Related

- Discovered in: KC-12 G1 walk (Wave flow-kc12) — 3rd recurrence trigger
- Children: GAP-1031 (KH-10) + GAP-1034 (KC-10) + GAP-1041 (KC-12) — concrete instances
- `meta-gap-priority.md` §3 — META P1 force-multiplier (fix gateway route discipline 1 lần → eliminate routing-collision class)
- `cross-flow-bug-class-sweep.md` — recurring class warrants systemic fix
