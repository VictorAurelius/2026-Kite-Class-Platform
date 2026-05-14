# GAP-540: Beta support channel discoverability (support@ + chat widget + footer)

**Status:** 🟡 PARTIAL (80% — Wave 78 Bucket F shipped footer mailto: + Help + beta-status; paid chat widget vendor Wave 79)
**Priority:** 🔴 P0
**Domain:** Frontend
**Detected:** 2026-05-14
**Related PRs:** (Wave 78 plan PR pending)
**Related Docs:** `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Footer support@ link visibility | `kitehub/kitehub-frontend/src/components/layout/footer.tsx` | partial — footer có thể có nhưng support@ link chưa rõ |
| Chat widget embed (Crisp/Tawk.to OR mailto: MVP) | `kitehub/kitehub-frontend/src/components/support/` | ❌ missing |
| Help/FAQ link trong dashboard nav | `kitehub/kitehub-frontend/src/components/layout/sidebar.tsx` | ❌ missing (Help link) |
| support@kitehub.me email address active | DNS MX + email forwarding | ⚠️ need verify (GAP-525 credential rotation may have touched) |
| api-contract.md cho support-tickets endpoint (nếu in-house) | `documents/01-business/support/api-contract.md` | ❌ missing |

**Grep commands run:**
```bash
find kitehub/kitehub-frontend/src/components -path "*footer*" -o -path "*support*"  # check existing
grep -rn "support@kitehub" kitehub/kitehub-frontend/src 2>&1 | head -20
ls documents/01-business/support/ 2>&1  # folder absent
```

## Problem

Beta user gặp bug hoặc câu hỏi → **không biết liên hệ ai/như thế nào**. Footer hiện tại không hiển thị support@kitehub.me rõ ràng; không có chat widget; không có Help/FAQ link trong dashboard nav. Per outside-in audit 2026-05-14 (N7 finding): support channel discoverability = retention critical signal — beta user bị stuck không có lối liên hệ → bounce.

## Context

Outside-in 3-agent audit 2026-05-14 N7 finding. Comparable SaaS: Stripe support widget góc phải dashboard; Linear cmd-K → ask AI; Vercel footer support link + status link. Wave 78 RETAIN scope cần discoverable support.

## Evidence

- Outside-in audit 2026-05-14 N7 finding
- Inside-out audit miss vì developer perspective: dev không cần support (đã có repo access + GitHub)
- Beta target tenant = real user — support channel = trust signal

## Proposed Fix

1. Bucket 0 Foundation (cross-layer prereq nếu support ticket in-house): `documents/01-business/support/api-contract.md` CREATE
   - Decision: in-house `POST /api/v1/support-tickets` (NEW endpoint Wave 78) OR external (Crisp/Tawk.to embed only, không có BE)
   - Recommend MVP: external chat widget embed + mailto: fallback; in-house ticket → Wave 79+ scope
2. Footer component update: `kitehub-frontend/src/components/layout/footer.tsx`
   - Hiển thị `support@kitehub.me` link (mailto: anchor)
   - Status page link `/beta-status` (sync với GAP-539)
   - "Help & Documentation" link → `/help` route (MVP placeholder OR external docs URL)
3. Chat widget MVP: `kitehub-frontend/src/components/support/support-widget.tsx`
   - Floating button góc phải-dưới dashboard (z-index above content)
   - Click → opens mailto:support@kitehub.me?subject=... HOẶC external Crisp/Tawk.to embed
   - Configurable via env var `NEXT_PUBLIC_CHAT_WIDGET_URL`
4. Dashboard sidebar nav: thêm "Help & Support" item dưới Settings
5. Support email forwarding verify: `support@kitehub.me` → forward đến team inbox (Cloudflare Email Routing OR AWS SES receive)

## Acceptance Criteria

- [ ] Footer hiển thị support@kitehub.me + /beta-status link + Help link (mọi page)
- [ ] Chat widget hiển thị floating button trên dashboard layout
- [ ] mailto: anchor populates subject="Beta support — [tenant_slug]" để team triage faster
- [ ] Help link nav trong dashboard sidebar (visible mọi role)
- [ ] support@kitehub.me forwarding verify (gửi test email → nhận inbox)
- [ ] FE component unit test cover footer link visibility + widget mount
- [ ] Accessibility: support widget không trap keyboard; ARIA labels present
- [ ] Live walkthrough verify per `pre-handoff-self-test-completeness.md` §2.1 — user clicks support → email client opens với prefilled subject
- [ ] Documentation: support contact rationale trong `documents/05-guides/operations/beta-invite-flow.md` (GAP-480 sister gap)

## Related

- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` Bucket F
- Sister gap GAP-542 (feedback widget — different scope: feedback widget = product feedback, support widget = bug/help)
- GAP-539 (beta disclaimer banner mention support@)
- GAP-480 (beta invite flow doc — includes support contact section)
- Rules: `dev-readable-doc-language.md` v1.0.1 (Vietnamese support copy); `agent-action-bias.md` (default to command/CLI for email forwarding setup)
- Outside-in 3-agent audit 2026-05-14 N7 finding

## Log

- 2026-05-14 — Initial write-up (state-check completed; footer partial / chat widget + Help nav missing; Wave 78 Bucket F owner).
- 2026-05-14 — Wave 78 Bucket F shipped (PR pending): `Footer.tsx` extracted from `PublicLayout.tsx` with new "Hỗ trợ" section — support@kitehub.me mailto: link + Trung tâm trợ giúp `/help` link + Trạng thái Beta `/beta-status` link + Privacy + Terms in Contact column. Tests cover all 4 testid'd links. Status flip to 🟡 PARTIAL (80%) — paid chat widget vendor (Crisp/Tawk.to) deferred Wave 79 per plan §1 Q4 MVP decision.
