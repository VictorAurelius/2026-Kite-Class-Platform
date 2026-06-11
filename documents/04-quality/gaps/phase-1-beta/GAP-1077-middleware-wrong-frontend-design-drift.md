---
id: GAP-1077
title: Host→tenant middleware build SAI frontend (kitehub-frontend) vs design (kiteclass-frontend) — GAP-811 reconcile
status: PARTIAL
priority: P1
phase: phase-1-beta
domain: Frontend
created: 2026-06-08
last_verified: 2026-06-11
---

# GAP-1077 — Host→tenant middleware sai frontend (design↔code drift)

## Problem

Design↔code drift về vị trí FE middleware host→tenant, phát hiện khi reconcile recipe G2 GAP-811 (2026-06-08, user-flagged "3001 là KH mà?").

**Design canonical** (`documents/02-architecture/tenant-domain-landing-architecture.md`):
- Tenant landing render FE = **`kiteclass-frontend`** (diagram dòng 26 + dòng 50).
- GAP-811 = "FE `middleware.ts` host→tenant" scope rõ là **kiteclass-frontend** (dòng 98).
- KiteHub apex `kitehub.me` = "marketing site, **no tenant context**" (middleware comment tự ghi).

**Code thực tế:**
- Middleware `src/middleware.ts` được build ở **`kitehub-frontend`** (`:3001`, Wave tenant-domain-1 Bucket C, comment tag "GAP-811"), resolve `sky.kitehub.me` → BE `by-subdomain`.
- **`kiteclass-frontend` (`:3000`) KHÔNG có `middleware.ts`** (verify `find kiteclass/kiteclass-frontend/src -name middleware.ts` → 0 file) — chỉ có `useTenantFromUrl.ts` client-hook (trả slug, không UUID, không tới SSR).
- Domain mismatch: design `*.kiteclass.com` vs code `*.kitehub.me` (liên quan brand-pivot deferred, memory `feedback_brand_pivot_kiteclass_me_dual_brand`).

**Hậu quả:**
- GAP-811 (deliverable đúng = kiteclass-frontend middleware) **VẪN CHƯA LÀM** — bị tưởng "shipped ở kitehub-frontend PARTIAL" (handoff cũ + recipe G2 mis-scope).
- Middleware ở kitehub-frontend = drift: KiteHub platform không cần per-tenant middleware theo design (apex = generic marketing).

## Root Cause

Implement GAP-811 đặt middleware nhầm vào `kitehub-frontend` thay vì `kiteclass-frontend` (design). Có thể do: (a) brand-pivot deferred → domain hiện là `kitehub.me` → tưởng landing thuộc kitehub-frontend; (b) không đọc design trước khi implement (design-first violation). Session sau + recipe G2 kế thừa nhầm.

## Proposed Fix

**User chốt 2026-06-08 (AskUserQuestion):** middleware THUỘC **kiteclass-frontend (theo design)**.

1. Implement `kiteclass-frontend/src/middleware.ts` host→tenant per GAP-811 Approach A (đọc Host → BE resolve UUID → inject `x-tenant-id` cho SSR landing).
2. Quyết định số phận middleware `kitehub-frontend/src/middleware.ts`: remove (nếu KiteHub apex thuần marketing không cần) HOẶC repurpose (nếu KiteHub có per-tenant preview surface riêng — cần design làm rõ, xem option 3 AskUserQuestion).
3. Reconcile domain: `kitehub.me` ad-interim vs `kiteclass.com` design — align hoặc cập nhật design doc.
4. GAP-811 chỉ flip DONE khi middleware đúng ở kiteclass-frontend + walk production-accurate (nip.io subdomain Host per `g1-browser-walk-before-flip` §3.1).

## Acceptance Criteria

- [x] `kiteclass-frontend/src/middleware.ts` tồn tại + resolve Host→tenantId UUID + inject `x-tenant-id` cho SSR — shipped (commit c1b09c88); SSR consumers `(public)/{page,layout}.tsx` đọc header.
- [x] Quyết định + thực thi số phận `kitehub-frontend/src/middleware.ts` (remove/repurpose) — REMOVED; `find kitehub/kitehub-frontend/src -name middleware.ts` → 0 file (verify 2026-06-09). Move clean.
- [x] Design doc `tenant-domain-landing-architecture.md` + GAP-811 + recipe G2 nhất quán (cùng FE + cùng domain story) — design doc + middleware code + tests đều `kiteclass-frontend` + `*.kiteclass.com`. (G2 recipe sync = coordinator handoff.)
- [ ] Walk production-accurate qua kiteclass-frontend (nip.io subdomain) → landing render đúng tenant branding — **live walk deferred to coordinator G2**.

## Log

- 2026-06-11 — **Status sync OPEN → PARTIAL (state-check).** 3/4 AC `[x]` DONE (middleware port ✅, kitehub-frontend m/w removed ✅, design-doc consistency ✅); chỉ còn `[ ]` nip.io subdomain production-accurate walk (= G2★ landing-100). OPEN under-sold reality (code đã ship 06-09) → PARTIAL chính xác. KHÔNG flip DONE — chờ nip.io subdomain walk per `g1-browser-walk-before-flip` §3.1+§3.2 (cấm `?tenant=` làm bằng chứng; landing-100 G1 dùng `?tenant=` probe = bằng chứng giả cho access-mode). Walk này = đúng phần G2★ pending của landing-100 (campaign §1 G2★-absorbs-G3-functional, 2026-06-11). Per `gap-architecture-v2.md` §3 + `audit-to-gap-pipeline.md` §2.8 fix-time state-check.
- 2026-06-09 — **Move verified CLEAN.** `kiteclass-frontend/src/middleware.ts` exists (host→tenant, GAP-1077/811 tag in comments); `kitehub-frontend/src/middleware.ts` REMOVED (`find kitehub/kitehub-frontend/src -name middleware.ts` → 0 file). Design (`tenant-domain-landing-architecture.md`) + middleware code + tests all consistent on `kiteclass-frontend` + `*.kiteclass.com`. 41/41 FE unit tests PASS + `pnpm --filter kiteclass-frontend build` exit 0. 3/4 AC met; remaining = live production-accurate walk (coordinator G2) + DONE flip.

## Related

- Discovered in: reconcile recipe G2 GAP-811 scope 2026-06-08 (user-flagged "3001 là KH")
- [[GAP-811]] — FE middleware host→tenant (design = kiteclass-frontend; gap này track drift của implementation)
- Design: `documents/02-architecture/tenant-domain-landing-architecture.md` dòng 26/50/98
- Rule: `design-first-investigation-order` (test/implement theo design trước, không theo code), `g1-browser-walk-before-flip` §3.1 (production-accurate Host, đúng FE)
- Brand context: memory `feedback_brand_pivot_kiteclass_me_dual_brand` (kitehub.me ad-interim)
