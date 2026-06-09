# GAP-1091: AI Branding section hiển thị trial/locked dù tier PREMIUM — `MOCK_QUOTA` hardcode + advanced-settings sai `instanceId`

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-09 (state-check tier-entitlement UI session)
**Affects:** `kitehub-frontend` `(customer)/branding/page.tsx` (quota widget) + `(customer)/settings/branding/advanced/page.tsx` (advanced gate) — KH-6 AI Branding flow

## Problem

Hai call-site trong AI Branding hiển thị trial/locked dù subscription PREMIUM. Thuộc cluster bug "PREMIUM shows trial UI" (cùng cluster GAP-1090).

1. **(3a) Branding hub quota widget hardcode** — `(customer)/branding/page.tsx:55` `const MOCK_QUOTA = { used:3, limit:10, tier:'PRO' }` → line 179/201/209 luôn render "gói PRO" + CTA "Nâng cấp PREMIUM" vĩnh viễn, KHÔNG đọc field tier thật từ subscription/branding API.

2. **(3b) Advanced-mode branding gate truyền SAI id** — `(customer)/settings/branding/advanced/page.tsx:36` gọi `useBrandingTier(user?.id)` — truyền OWNER id làm `instanceId` → `useActiveSubscription(user.id)` gọi `/subscriptions/instance/{user.id}/active` → 404 (owner id ≠ instance id) → tier fallback `'FREE'` → render locked "Tính năng chỉ dành cho ENTERPRISE — Gói hiện tại (FREE)" (line 111-123) **dù subscription PREMIUM/ENTERPRISE**. Khác với wizard `TemplateStep` (line 185 truyền đúng `instanceId`).

3. **Hook logic ĐÚNG, bug chỉ ở call-site** — `use-branding-tier.ts:84` đọc `subscription.tier` chính xác. Bug nằm ở 2 call-site (hardcode + wrong-id), KHÔNG phải logic của hook.

## Root Cause

- (3a) Quota widget được scaffold với mock data và chưa wire vào API thật → hardcode `'PRO'` + CTA upgrade vĩnh viễn.
- (3b) Call-site nhầm `user.id` (owner) với `instanceId` khi gọi `useBrandingTier` → subscription lookup 404 → fallback FREE → gate khoá nhầm. Wizard `TemplateStep` đã truyền đúng `instanceId` → confirm đây là lỗi call-site cục bộ, không phải hook.

## Proposed Fix

1. Thay `MOCK_QUOTA` trong `branding/page.tsx` bằng `useBrandingTier(instanceId)` thật (real quota từ subscription/branding API).
2. Sửa `advanced/page.tsx:36` truyền `instanceId` (resolve từ owner's instances) thay vì `user?.id`.

## Acceptance Criteria

- [x] Branding hub quota đọc tier thật (PREMIUM → không hiện CTA "Nâng cấp PREMIUM")
- [x] Advanced branding gate truyền `instanceId` đúng → không còn locked-as-FREE (tier label hiển thị đúng PREMIUM; 404 fallback hết)
- [x] Không còn hardcode `tier:'PRO'` trong branding hub

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 + pre-handoff-self-test-completeness §3)

**Source:** state-check tier-entitlement UI fix session 2026-06-09. Verify trên local Docker stack (`:3001` kitehub-frontend), tenant `test-8` PREMIUM (owner `g2test-an-8@example.com`). Headless browser Playwright — assertion PASS.

**Browser (Playwright headless `:3001`, login `g2test-an-8@example.com`):**
- (3a) `/branding` hub: **KHÔNG** còn CTA "Nâng cấp PREMIUM" + hiển thị "GÓI PREMIUM" — `MOCK_QUOTA` hardcode `tier:'PRO'` đã gỡ, wire `useBrandingTier(instanceId)` thật.
- (3b) `/settings/branding/advanced`: HTTP **200** (hết 404 fallback) + hiển thị tier **(PREMIUM)** thay vì sai **(FREE)** — call-site truyền `instanceId` đúng thay vì `user.id`.

**Build:** `pnpm --filter kitehub-frontend build` PASS 89/89.

**Code shipped (working tree session 2026-06-09):** FE `branding/page.tsx` (gỡ `MOCK_QUOTA`, wire `useBrandingTier(instanceId)` thật) + `settings/branding/advanced/page.tsx` (truyền `instanceId` đúng).

**Nuance — KHÔNG over-claim (per `ai-branding-guidelines` §2.4):** Advanced branding mode là **ENTERPRISE-only**. Fix này chỉ sửa **tier label sai** ((FREE)→(PREMIUM)) + hết 404 fallback; **toggle advanced vẫn ẩn với tier PREMIUM** — đây là design ĐÚNG (cần ENTERPRISE để bật advanced). AC #2 "không locked-as-FREE" đạt: gate không còn khoá nhầm vì lookup 404; tier hiển thị đúng. Full advanced unlock cho ENTERPRISE là scope riêng (không thuộc gap này).

## Related

- Discovered in: state-check tier-entitlement UI session 2026-06-09 (2 Opus agent state-check)
- Cluster sibling: GAP-1090 (root cause `instances.tier` không sync — cùng cluster "PREMIUM shows trial UI")
- Branding tier: GAP-1020 (branding tier RLS, server-side không client-trust)

## Log

- **2026-06-09** — 🟢 DONE. Fix shipped tier-UI session: (3a) `branding/page.tsx` gỡ `MOCK_QUOTA` hardcode `tier:'PRO'` → wire `useBrandingTier(instanceId)` thật (hết CTA "Nâng cấp PREMIUM"); (3b) `settings/branding/advanced/page.tsx:36` truyền `instanceId` đúng thay vì `user.id` (hết 404 → tier label hiển thị PREMIUM thay vì FREE). Verify per `## Walk evidence` (Playwright headless `:3001` tenant test-8 PREMIUM + build 89/89). **Nuance:** advanced toggle vẫn ENTERPRISE-only per `ai-branding-guidelines` §2.4 — fix chỉ sửa tier-label-sai + 404, KHÔNG bật advanced cho PREMIUM (đúng design). Status flip per `gap-done-discipline.md` §2 — 3 AC verified; `git mv` → `phase-1-beta/closed/` + CSV row sync per `gap-folder-organization.md` v2.0.0 + `post-merge-sync-completeness.md`.
