# Gateway carve-out runtime walk (C1-C5) — G3 production-parity verify (GAP-1042/1049)

**Ngày:** 2026-06-07
**Loại:** Runtime walk evidence — production-parity (gateway :9000, JWT→header, Postgres+Flyway+RLS)
**Trigger:** P3 G3 production-parity prerequisite per `g3-production-parity-plan.md` — verify 5 carve-out route fixes (#2228, GAP-1042/1049) chạm đúng service runtime, không chỉ config-level.
**Post-fix re-walk per** `pre-handoff-self-test-completeness.md` §3 (source audit = `2026-06-07-gateway-route-predicate-audit.md`).

## Stack-up evidence

- 4 backend service rebuilt từ main HEAD `7d75a9cd` + healthy: kite-gateway (carve-outs #2228), kitehub-subscription, kiteclass-core, kitehub-branding — all `fresh` per `check-stale-images.sh`.
- Auth chain: HS512 JWT minted với gateway `JWT_SECRET` (82 chars), claims `sub`/`role`/`email`/`tenantId` → gateway inject `X-User-Id`/`X-User-Roles`/`X-User-Email`/`X-Tenant-Id`. Verified: bare call → 401/400, valid token → reaches service.

## Walk evidence per carve-out (gateway :9000)

| # | Route | Verb | Token | HTTP | Service reached (signal) | Verdict |
|---|---|---|---|---|---|---|
| C1 | `/api/platform/admin/payments/pending` | GET | PLATFORM_ADMIN | **200** | subscription `AdminPaymentController` — returned real PENDING VietQR payment row | ✅ routed correct (pre-fix: 404 admin) |
| C2 | `/api/v1/preferences/dismiss-banner-state` | POST | OWNER+tenantA | **400** | subscription `PreferencesController` — RFC7807 "bannerKey: must not be blank" (validation reached) | ✅ routed correct (pre-fix: 404 kiteclass) |
| C3 | `/api/v1/admin/parent/consent/bulk-bump` | POST | ADMIN+tenantA | **400** | kiteclass-core `ParentConsentAdminController` — core error shape `fieldErrors{reason,newVersion}` (validation reached) | ✅ routed correct (pre-fix: 404 admin) |
| C4 | `/api/v1/payments/webhook/{vnpay\|momo}` | GET/POST | none (public) | 405 then **500** | kiteclass-core `PaymentWebhookController` — 405 (wrong verb) + 500 (reached handler, `payment_webhook_logs.instance_id` NOT NULL violation) | ✅ routed correct + TenantResolver-skip works (pre-fix: 400 "Cannot resolve tenant"); ⚠️ discovery → GAP-1051 |
| C5 | `/api/v1/parent-invitations/redeem/{token}` | POST | none (public) | **400** | kiteclass-core `ParentInvitationController` redeem — validation "Mật khẩu là bắt buộc / Tên là bắt buộc" (reached) | ✅ routed correct + TenantResolver-skip works (pre-fix: 400 tenant) |

**Key signal interpretation:** distinguishing "reached correct service" from "404-from-wrong-service" / "400-gateway-TenantResolver" via (a) response body error-shape (subscription RFC7807 `about:blank` vs kiteclass-core `{success,code,fieldErrors}`), (b) actual data (C1 200), (c) controller-level validation messages, (d) confirmed C4 500 in `kiteclass-core` logs (PSQLException not-null `payment_webhook_logs.instance_id`).

## Verdict

**All 5 carve-out routes (C1-C5) route to the correct backend service at runtime.** GAP-1042 (META gateway route-predicate audit) + GAP-1049 (gateway routing collisions cluster) → routing fix verified production-parity → **DONE**.

## Discovery (per `discovery-to-gap-inline-filing.md`)

- **GAP-1051** P3: `PaymentWebhookController` returns 500 (NOT NULL `payment_webhook_logs.instance_id`) on public webhook with no server-side tenant resolution. Phase 1.5 payment (momo/zalopay = "not implemented yet" stub; vnpay GET same root). Should resolve tenant from txn-ref OR return graceful 400/202. Routing not affected.

## Sister scope spot-check (per `pre-handoff-self-test-completeness.md` §3.2)

- C1 reached subscription with live data (no regression on platform-admin catch-all — AdminController `/dashboard` etc still owned by admin).
- C3/C5 TenantResolver-skip did not break the `/api/v1/**` catch-all (instance-apis) — both carve-outs precede catch-all in declaration order, confirmed by reaching core not 400.
