# GAP-1086: Subscription email content sai — subject tiếng Anh + thiếu .txt + sai domain (Bug F)

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-09 (Wave landing-tenant-1 — KH-3 G2 SePay walk, bug F)
**Affects:** `kitehub/kitehub-subscription/.../client/EmailServiceClient.java` (`sendSubscriptionCreatedEmail`, `sendSubscriptionActivatedEmail`) + `kitehub-email/.../templates/emails/subscription-created.{html,txt}` + `subscription-activated.txt`

## Problem

KH-3 G2 walk phát hiện 3 lỗi nội dung email kích hoạt gói (`subscription-created`):

- **(a) Subject tiếng Anh:** `"Subscription đã kích hoạt - {org}"` — từ "Subscription" vi phạm VN-localization (per `dev-readable-doc-language.md` / `vn-localization-audit-checklist.md`).
- **(b) Thiếu plain-text part:** template `subscription-created.txt` không tồn tại → `EmailTemplateRenderer.renderPlainTextSibling` trả về rỗng → email HTML-only (không có multipart/alternative) → rủi ro ~20% silent churn ở Gmail/Outlook plain mode (per GAP-657/659).
- **(c) Sai domain:** `dashboardUrl = "https://kitehub.vn/dashboard"` — domain Phase 1 BETA canonical là `kitehub.me`.

Phát hiện thêm khi sửa:
- **(d) Biến template lệch:** `subscription-created.html` dùng `${price}/${startDate}/${nextRenewalDate}` nhưng sender chỉ truyền `organizationName/tier/billingCycle/dashboardUrl` → 3 dòng info-box render rỗng. `billingCycle` truyền nhưng template không dùng.
- **(e) Sister `subscription-activated`:** cùng walk, `supportUrl = "https://kitehub.vn/dashboard"` (sai domain) + thiếu `.txt`.

## Fix (shipped session 2026-06-09)

| Phần | Sửa |
|---|---|
| (a) subject | `"Gói đăng ký đã kích hoạt - {org}"` (tiếng Việt) |
| (b) thiếu .txt | Tạo `subscription-created.txt` (vars khớp sender: organizationName/tier/billingCycle/dashboardUrl) |
| (c) domain | `dashboardUrl` → `https://kitehub.me/dashboard` |
| (d) biến lệch | info-box html chỉ hiển thị `tier` + `billingCycle` (bỏ dòng render rỗng) |
| (e) sister activated | `supportUrl` → `kitehub.me`; tạo `subscription-activated.txt` (vars khớp: organizationName/tier/expiresAt/supportUrl) |

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Class 1 — domain drift trong EmailServiceClient:** grep `https://kitehub\.(vn|com)` → 12 site (9× kitehub.com: trial/renewal/suspension/retention; 3× kitehub.vn: welcome loginUrl, DSAR). **FIX** 2 site in-scope (subscription-created/activated); **DEFER → GAP-1088** 10 site còn lại (out-of-walk-scope domain-consistency sweep).

**Class 2 — html-only template thiếu .txt:** 16 template còn thiếu `.txt` (admin-new-login-alert, trial-*, data-retention-*, dsar-*, invoice, onboarding-tips, subscription-expired/renewal/suspended...). **FIX** 2 in-scope (created/activated); **DEFER → GAP-1088** 16 còn lại (= GAP-657/659 "final 20%" transitional follow-up, đã biết).

## Acceptance Criteria

- [x] Subject `subscription-created` tiếng Việt
- [x] `subscription-created.txt` + `subscription-activated.txt` tạo, vars khớp sender
- [x] `dashboardUrl`/`supportUrl` → `kitehub.me`
- [x] info-box html khớp biến truyền (không render rỗng)
- [ ] **Runtime re-walk (pending — gộp G2 re-walk):** MailHog hiển thị subject VN + có text part + link `kitehub.me` (per `pre-handoff-self-test-completeness.md` §3 / §2.3 email-flow)

## Related

- Discovered in: Wave landing-tenant-1 KH-3 G2 SePay walk (handoff Bug F)
- Sibling same walk: GAP-1085 (duplicate email)
- Deferred sweep: GAP-1088
- Plain-text coverage origin: GAP-657 / GAP-659
