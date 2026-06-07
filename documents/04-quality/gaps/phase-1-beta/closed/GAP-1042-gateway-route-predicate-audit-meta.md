# GAP-1042: META — Gateway route-predicate audit (broad predicates shadow/expose wrong service, 3 recurrences)

**Status:** 🟢 DONE
**Priority:** 🟠 P1 (META — force-multiplier per `meta-gap-priority.md` §3)
**Domain:** Backend (gateway) — meta/systemic
**Found:** 2026-06-06 (KC-12 G1 walk — 3rd recurrence triggered meta filing)
**Closed:** 2026-06-07 (full systematic audit shipped + 5 carve-outs runtime-verified via :9000; HARD CI-gate wiring spun off → GAP-1052)
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

- [x] Audit artifact: mọi catch-all route + service-ownership verify — `2026-06-07-gateway-route-predicate-audit.md` (56 routes mapped, 5 collisions C1-C5 found)
- [x] Mọi kiteclass-core path dưới shared prefix (`/api/v1/admin/payroll`, `/api/v1/branding/{public,versions,package}`) route đúng kiteclass-core — verified runtime via :9000 (C3 parent-consent + branding carve-outs reach core)
- [x] Internal-only path (`/api/platform/emails`) không expose public (GAP-1031 DONE)
- [x] Route-ordering invariant documented + (optional) CI guard — invariant in audit §2 + application.yml comments; detector `audit-gateway-routes.sh` extended (#2228 BS#1 kiteclass-core scan + BS#2 TenantResolver model). HARD CI-gate wiring (WARN→FAIL) deferred → GAP-1052
- [x] GAP-1031 + GAP-1034 + GAP-1041 closed bởi systemic fix (all DONE Wave security-1/security-2)

## Related

- Discovered in: KC-12 G1 walk (Wave flow-kc12) — 3rd recurrence trigger
- Children: GAP-1031 (KH-10) + GAP-1034 (KC-10) + GAP-1041 (KC-12) — concrete instances
- `meta-gap-priority.md` §3 — META P1 force-multiplier (fix gateway route discipline 1 lần → eliminate routing-collision class)
- `cross-flow-bug-class-sweep.md` — recurring class warrants systemic fix

## Progress (Wave security-1, 2026-06-06)

**Concrete 3 collisions FIXED + verified** (the actionable findings): GAP-1031 (email expose — route removed), GAP-1034 (branding shadow — 3 explicit routes added), GAP-1041 (payroll shadow — explicit route added). All re-walk-verified live; `audit-gateway-routes.sh` back to 4-finding main baseline + extended with `INTERNAL_ONLY_PATTERNS` exemption (email). Predicate-discipline pattern applied (specific routes before catch-all, single-predicate to avoid audit parser confusion).

**REMAINING (keeps PARTIAL):** (1) full systematic audit of ALL catch-all `/**` gateway routes vs service-ownership (4 pre-existing findings remain: preferences→kiteclass + 3 admin-payments→admin — heuristic, verify real-vs-FP); (2) wire `audit-gateway-routes.sh` as HARD CI gate (currently deferred per production-env-config-registry.md §11); (3) route-ordering invariant doc. Defer to dedicated gateway-audit follow-up wave.

## Closure (P3 G3, 2026-06-07)

All REMAINING items resolved or spun off:
- (1) Full systematic audit SHIPPED — `2026-06-07-gateway-route-predicate-audit.md` mapped all 56 routes vs service-ownership; the 4 "pre-existing heuristic findings" resolved into 5 concrete collisions C1-C5 (the 4th "admin-payments" = C1; "preferences" = C2; +3 detector-blind C3/C4/C5), all carve-out fixed (#2228) + runtime-verified via :9000 (`2026-06-07-gateway-carveout-runtime-walk.md`).
- (2) HARD CI-gate wiring (WARN→FAIL) → spun off **GAP-1052** (detector already extended in #2228; only the gate-severity flip remains, low-risk follow-up).
- (3) Route-ordering invariant documented in audit §2 + inline `application.yml` carve-out comments.

Routing-collision class eliminated systemically: detector now scans kiteclass-core (BS#1) + models TenantResolver-400 (BS#2), so future shadow/expose collisions surface in CI rather than per-flow walks.
