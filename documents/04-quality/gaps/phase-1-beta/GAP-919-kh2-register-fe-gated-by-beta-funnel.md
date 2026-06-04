# GAP-919: KH-2 register via FE bị gate sau KH-1 (beta funnel) — wave plan S1 không reflect FE production-equivalent path

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (campaign-meta — không phải bug, scope-coverage gap)
**Domain:** Frontend / Campaign meta
**Found:** 2026-06-04 (Wave flow-kh2 G2 handoff — user-flagged FE CTA mismatch)
**Affects:**
- `documents/03-planning/roadmap/flow-verification-campaign.md` §3 dependency graph
- `documents/03-planning/waves/wave-2026-06-03-flow-kh2-auth-onboarding.md` §3.2 S1 walk scope
- `kitehub-frontend/src/app/(public)/page.tsx` + `LandingClient.tsx` landing CTA
- `kitehub-frontend/src/app/(public)/pricing/PricingContent.tsx:173` register link

## Problem

Walk KH-2 G1 via BE direct `POST /api/auth/register` PASS, NHƯNG production-equivalent user-facing flow qua FE bị gate bởi Phase 1 BETA design:
- Landing CTA: **"Dùng thử miễn phí 14 ngày"** (per `LandingClient.tsx` + tests)
- Click CTA → `/register` route → HTTP 307 redirect → `/request-beta-access` (KH-1 beta funnel entry point)
- Self-service public register endpoint (`POST /api/auth/register`) **không reachable** qua FE trong Phase 1 BETA
- Chỉ beta-approved user nhận invite link mới register được qua `/beta-signup/code/<token>` (KH-1 cuối → KH-2 đầu)

Implication cho campaign:
1. **Dependency graph §3 sai/thiếu** — KH-2 register via FE depend KH-1 (beta funnel approve+invite); current graph chỉ ghi KH-2 → KH-1 (KH-1 depends on KH-2 auth)
2. **Wave plan KH-2 §3.2 S1** test BE endpoint direct — không reflect FE invite-gated production path
3. **G2 KH-2 không thể test standalone** trên FE — user phải qua KH-1 toàn bộ trước khi register form xuất hiện

Severity P2 — meta scope-coverage gap, không phải bug; BE direct register PASS confirms business logic OK. FE design intentional cho Phase 1 BETA (cấm self-service signup, chỉ beta-invite). Phase 2 mở public register sẽ giảm coupling.

## Proposed Fix

Options:
1. **Re-order campaign topology** — KH-1 (beta funnel) thành root, KH-2 register subset của KH-1 wrap-up step. Wave KH-2 ✅ G1 BE evidence vẫn valid; FE G2 transition vào wave KH-1.
2. **Tách scope KH-2** — split thành KH-2a (BE register endpoint G1 BE direct, defer FE G2 sang KH-1 closure) + KH-2b (FE register-form post-invite — phần của KH-1 §S4). Document split trong campaign §3 dependency graph.
3. **Phase 1 BETA-aware G2 recipe** — KH-2 G2 = qua beta funnel (request-beta-access → admin approve → click invite link → register form auto-fill → submit). User test thực sự là KH-1+KH-2 chained.

Recommend Option 3 cho ngắn hạn (đỡ refactor campaign), Option 1 cho dài hạn (re-architect topology Phase 2).

## Acceptance Criteria

- [ ] Campaign §3 dependency graph note: "KH-2 register via FE chỉ reachable qua KH-1 invite-gated path trong Phase 1 BETA"
- [ ] Wave plan KH-2 §3.2 S1 add note: "BE direct register OK; FE production-path requires KH-1 prerequisite (Phase 1 BETA gate)"
- [ ] G2 recipe revised cho user: full beta-funnel test (request → admin approve → invite → register-via-link) thay vì standalone register
- [ ] KH-1 wave plan (next loop) include explicit S4 "User clicks invite link → /beta-signup/code/<token> → register form prefilled → submit → POST /api/auth/register fired"
- [ ] Phase 2 follow-up: open public register (remove `/register` 307 redirect) → KH-2 standalone-able

## Related

- Discovered in: Wave flow-kh2 G2 handoff (user-flagged "Đăng ký" CTA không tồn tại, actually "Dùng thử miễn phí")
- Sister: KH-1 wave plan (chưa tạo) sẽ subsume KH-2 FE path
- BE endpoint `AuthService.register()` line 123-145 vs `registerFromBetaInvite()` line 218-238 — 2 paths cùng tạo OWNER nhưng FE chỉ expose path 2 qua invite
