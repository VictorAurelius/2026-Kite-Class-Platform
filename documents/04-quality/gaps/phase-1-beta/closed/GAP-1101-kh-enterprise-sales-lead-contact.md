# GAP-1101: KiteHub PLATFORM sales-lead /contact flow — Enterprise "Liên hệ" CTA

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Mixed (Backend + Gateway + Frontend)
**Found:** 2026-06-09 (G2 walk pricing/billing — Enterprise CTA 404 + hardcode wrong-domain alert)
**Affects:** kitehub-frontend `(public)/pricing` + `components/billing`, kitehub-subscription, kitehub-gateway
**Completion:** 100%

## Problem

CTA "Liên hệ" của gói Enterprise trên KiteHub PLATFORM bị hỏng ở 2 nơi:

1. **404 dead-link** — `kitehub-frontend/src/app/(public)/pricing/PricingContent.tsx:172`
   trỏ Enterprise CTA tới `/contact`, nhưng route `/contact` KHÔNG tồn tại trong
   kitehub-frontend → HTTP 404. Khách hàng tiềm năng (chủ trung tâm) bấm "Liên hệ"
   → trang trắng 404.

2. **Hardcode sai domain** — `kitehub-frontend/src/components/billing/TierSelector.tsx:20`
   dùng `alert('Vui lòng liên hệ sales@kiteclass.com ...')` = (a) domain SAI
   (`kiteclass.com`; canonical là `kitehub.me`; không có mailbox `sales@`), (b) UX tệ
   (alert box). Sister site `PlanComparison.tsx:30` cũng `mailto:sales@kiteclass.com`.

**Ranh giới sản phẩm (lý do gap này tồn tại — KH ≠ KC):** đây là KiteHub PLATFORM
sales (chủ trung tâm → KiteHub sales về gói Enterprise SaaS). KHÔNG tái dùng
`leads`/`contact_messages` BE đã có trong kiteclass-core — đó là KC TENANT marketing
(học sinh → trung tâm), domain KHÁC. Build sales-lead MỚI phía KH (per
`.claude/rules/kitehub-kiteclass-boundary.md` §2: KH = kitehub-frontend `:3001`,
`/api/platform/*`).

## Proposed Fix

Full-stack KH-side sales-lead:

- **BE (kitehub-subscription)** — theo precedent BetaAccessRequest + FeedbackSubmission
  (public unauthenticated POST-and-persist): entity `SalesLead` + Flyway V69 +
  `CreateSalesLeadRequest`/`SalesLeadResponse` DTO + `SalesLeadService` +
  `SalesLeadRepository` + `SalesLeadController` (`POST /api/platform/sales-leads`,
  PUBLIC, bean-validation + honeypot spam guard + XSS regex `[^<>&]` giữ dấu tiếng Việt).
- **Gateway** — route public `POST /api/platform/sales-leads` → kitehub-subscription
  (rate-limit 2/5 IP, mirror request-beta-access) + whitelist trong
  `JwtAuthenticationGatewayFilter.isPublicPath`.
- **FE (kitehub-frontend)** — `(public)/contact/page.tsx` (đọc `?plan` server-side,
  tránh Suspense bailout) + `ContactForm.tsx` (form VN: Họ tên / Email / SĐT / Tên
  trung tâm / Nội dung). Pre-fill `planInterest='ENTERPRISE'` khi `?plan=enterprise`.
- **Fix 2 CTA + sweep** — PricingContent `/contact?plan=enterprise`; TierSelector +
  PlanComparison `router.push('/contact?plan=enterprise')` (xoá `sales@kiteclass.com`).

## Acceptance Criteria

- [x] `POST /api/platform/sales-leads` PUBLIC (no JWT) — gateway route + filter whitelist + SecurityConfig permitAll
- [x] Entity-Migration-Mapper triad atomic: `SalesLead` + V69 migration + DTO (per design-patterns.md §3.12)
- [x] BE validation: @NotBlank/@Email/phone pattern + honeypot @Size(max=0) + XSS `[^<>&]` (giữ dấu tiếng Việt per vn-localization §5)
- [x] FE `/contact` route render (server-side searchParams → no Suspense bailout)
- [x] 2 CTA fix: PricingContent `/contact?plan=enterprise` + TierSelector navigate (no alert / no kiteclass.com)
- [x] Cross-flow sweep: PlanComparison sister `mailto:sales@kiteclass.com` fixed
- [x] Static verify: BE compile + tests 7/7, gateway YAML+compile, FE build exit 0, vitest 16/16
- [x] Runtime walk (coordinator gate): re-walk PASS 2026-06-10 — POST 201 + DB row + sad-paths 400 (xem §Runtime-walk evidence)

## Runtime-walk evidence (per feature-ship-runtime-walk-mandate.md §3)

**Walk bug caught (runtime-only — static verify miss):** subscription crash on boot `Not a managed type: class ...saleslead.entity.SalesLead`. `SalesLead` entity package thiếu trong `KitehubSubscriptionApplication` `@EntityScan` explicit basePackages list → `UnsatisfiedDependencyException` → app `Application run failed` → gateway 503 fallback. Flyway V69 chạy TRƯỚC khi context fail nên `sales_leads` table vẫn tạo (đánh lừa static check). Fix: add `com.kitehub.subscription.saleslead.entity` vào `@EntityScan`. Sweep mọi `@Entity` package vs `@EntityScan` list → 0 package khác bị sót.

| Bước | Kết quả |
|---|---|
| subscription rebuild + boot | ✅ healthy 9×3s, "Started KitehubSubscriptionApplication" (no UnsatisfiedDependency) |
| `POST /api/platform/sales-leads` (PUBLIC, no JWT) | ✅ HTTP 201 `{id, ENTERPRISE, status:NEW}` |
| DB `SELECT * FROM sales_leads` | ✅ row `1 / Trần Thị Hồng / Trung tâm Anh ngữ Sky Education / ENTERPRISE / NEW` |
| sad: honeypot filled | ✅ 400 |
| sad: bad email | ✅ 400 |
| FE `GET :3001/contact` | ✅ 200 |

## Related

- Discovered in: branch `agent/gap-1101-kh-sales-lead-contact`
- Rule: `.claude/rules/kitehub-kiteclass-boundary.md` §2 (KH ≠ KC product boundary)
- Rule: `.claude/rules/cross-flow-bug-class-sweep.md` (sister wrong-domain sweep)
- Precedent: BetaAccessRequest (GAP-372) + FeedbackSubmission (GAP-542) public-submit pattern
