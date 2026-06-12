# GAP-075: Developer Sandbox Tenant Environment

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Developer Experience / Integration
**Detected:** 2026-04-14 (simulation-gap-finder on Wave 3 scope)
**Matrix cell:** Developer × Configuration × C8 Integration

## Problem

3rd-party developers tích hợp KiteClass APIs (GAP-010 package API, GAP-038 SDK) **không có sandbox environment**. Options hiện tại:

- Test trên production tenant → consumes real AI quota + may leak test data to real users
- Test local `docker compose up` → không reflect production behavior (mock AI, no real webhook delivery)
- Test with trial tenant → trial expires, dev loses test data

Industry standard:
- Stripe: `sk_test_*` keys + fake cards + test webhooks
- Twilio: `test credentials` + free test numbers
- Anthropic: dedicated eval environment

KiteClass: nothing.

## Evidence

- Settings page không có "API test mode" toggle
- Docs (when GAP-038 land) sẽ reference `api.kitehub.me` — real prod
- Không có `X-KiteClass-Sandbox: true` header support
- Webhook retry / delivery (GAP-039) tested against production

## Proposed Fix

### 1. Sandbox tenant concept

```
Tenant.sandboxMode: boolean

When sandboxMode=true:
  - AI calls use MockAIClient (returns fixture data, 0 cost)
  - Outgoing emails sent to tenant owner only (not end users)
  - Webhook deliveries use developer-configured URL, support test-trigger endpoint
  - Payment gateway uses sandbox mode (Stripe test keys, MoMo sandbox)
  - Billing invoices marked [SANDBOX] + not sent to real customers
  - Branding pipeline returns fixture assets (no actual AI generation)
  - Banner in admin UI: "⚠️ SANDBOX TENANT — all actions are simulated"
```

### 2. Free sandbox tenants per developer

Each developer account gets 1 free sandbox tenant upon signup + ability to create up to 3 more.

### 3. Test data fixtures

Seed sandbox tenant with:
- 50 sample students
- 5 sample classes across 3 subjects
- 1 month attendance history
- 10 sample invoices (paid / unpaid / refunded)
- Pre-generated branding (skip AI to save setup time)

### 4. Dev-facing API

```
POST /api/v1/dev/sandbox-tenants
  → creates sandbox tenant with fixture data, returns credentials

POST /api/v1/dev/sandbox-tenants/{id}/reset
  → wipes + re-seeds fixture data

POST /api/v1/dev/webhook-test-deliver
  → triggers fake webhook event to developer URL for testing
```

### 5. Documentation

`/developers` section (ties to GAP-038):
- Quickstart: get sandbox credentials in 2 minutes
- Example code in JS / Python / Java / Go / PHP
- Webhook test tool (inline)
- Rate limit: sandbox is 100x less strict

## Acceptance Criteria

- [ ] `sandbox_mode` column on tenants + migration
- [ ] MockAIClient routing when sandboxMode=true
- [ ] Email interception in sandbox mode (send only to owner)
- [ ] Payment gateway test-mode routing
- [ ] `/dev/sandbox-tenants` CRUD endpoints
- [ ] Fixture data seeding job
- [ ] Admin UI banner for sandbox tenants
- [ ] Developer docs quickstart page
- [ ] E2E: developer signs up → creates sandbox → calls API → webhook fires
- [ ] Rate limit 100x standard for sandbox
- [ ] Audit: sandbox actions NOT in tenant's production audit log

## Dependencies

- GAP-038 (Developer API docs + SDK) — sandbox is prerequisite
- GAP-039 (webhook reliability) — test delivery endpoint
- GAP-017 (AI usage billing) — skip billing in sandbox

## Target Wave

**Wave 9 Developer Experience** (Sprint 8+) — nice-to-have, post-GA.

Does NOT block Wave 3.

## Log

- 2026-04-14 — Detected via simulation-gap-finder (Developer × Config, sandbox missing)
