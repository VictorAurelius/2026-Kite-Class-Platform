# GAP-231: API Contract Drift — payment-invoice domain

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Business-Logic — wrong API contract = wrong product per `meta-gap-priority.md` §3)
**Domain:** Backend / API contract documentation
**Found:** 2026-04-26 (post-wave-7 API contract audit, score 42/100 F)
**Affects:** Payment integrators (VNPay/MoMo/ZaloPay webhook consumers), tenant finance staff, support team, frontend payment screens

## Problem

Post-wave-7 API audit flagged **payment-invoice** as the worst-affected domain by raw endpoint count. Verified counts via grep on `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{payment,invoice}/`:

| Controller | Method-level mappings | Class base path |
|---|---:|---|
| `PaymentController` | 9 | `/api/v1/payments` |
| `PaymentWebhookController` | 3 | `/api/v1/payments/webhook` (vnpay GET, momo POST, zalopay POST) |
| `InvoiceController` | 10 | `/api/v1/invoices` |
| `RefundRequestController` | 5 | `/api/v1/refund-requests` |
| `InstallmentPlanController` | 5 | `/api/v1/installment-plans` |
| **Total** | **32** | — |

(Audit said "23 endpoints, 0 documented". Verified count is 32 — audit undercounted; likely missed InstallmentPlan + webhooks. Doc count was also misreported as 0; spot-check shows ~22–25 endpoints listed in `documents/01-business/kiteclass/payment-invoice/api-contract.md`. Drift is therefore PARTIAL not total — but gaps remain in DTO accuracy, auth headers, error code coverage, and webhook verification flow.)

Sample endpoints verified to exist in code:
- `POST /api/v1/payments` (`PaymentController:41`)
- `POST /api/v1/payments/{id}/refund` (`PaymentController:141`)
- `GET /api/v1/payments/webhook/vnpay` (`PaymentWebhookController:40`)
- `POST /api/v1/invoices/{id}/late-fees` (`InvoiceController:105`)
- `POST /api/v1/refund-requests/{id}/process` (`RefundRequestController:109`)

## Current State (verified 2026-04-26)

`documents/01-business/kiteclass/payment-invoice/api-contract.md` — **EXISTS**, 3.3K, lists ~25 endpoints in `### POST /api/v1/...` headings. Stub-quality: per-endpoint sections are 1–4 lines with description but inconsistent on:
- Auth header requirements (Bearer JWT, X-Tenant-Id) — not specified per endpoint
- Error code matrix — partial (a few `404` + `409` notes; many endpoints missing error block)
- Request DTO field validation rules (required, regex, range)
- Response DTO field-by-field schema (no DTO tables for most endpoints)
- Webhook signature verification flow (vnpay/momo/zalopay) — undocumented
- UC cross-references (`UC-PAY-XX`, `UC-INV-XX`) absent

`rules.md` (3.3K) and `use-cases.md` (6.2K) exist — UC IDs likely defined; api-contract.md needs to back-reference them.

## Root Cause

Wave 5–7 added payment + invoice + installment + refund-request controllers iteratively. Each PR shipped controller + (sometimes) added one heading to api-contract.md, but never the full schema/auth/error block. Living-docs SLA from `output-review-mandate.md` was not enforced per-PR for this domain. Audit-gate hook does not currently validate `@Mapping` count vs `### ` heading count in api-contract.md.

## Proposed Fix

1. List all 32 method-level mappings from the 5 controllers (use grep output as inventory).
2. For each endpoint, add or expand its `### {VERB} {path}` section in `documents/01-business/kiteclass/payment-invoice/api-contract.md` with:
   - 1-line summary + UC reference (`UC-PAY-XX` from use-cases.md)
   - Auth: `Bearer JWT (role: ADMIN|FINANCE|STUDENT|TEACHER)`, `X-Tenant-Id: {slug}` (or `none` for public webhooks)
   - Request DTO table (field, type, required, validation) — match real Java DTO field-for-field
   - Response DTO table (field, type, description) — match real Java response class
   - Error codes: `400 VALIDATION_ERROR`, `401 UNAUTHENTICATED`, `403 FORBIDDEN`, `404 NOT_FOUND`, `409 CONFLICT_*`, `500 INTERNAL`, plus domain-specific (`PAYMENT_GATEWAY_TIMEOUT`, `WEBHOOK_SIGNATURE_INVALID`)
3. Document webhook verification flow: signature header name, HMAC algorithm, timestamp tolerance, idempotency key.
4. Document refund + installment lifecycle as state-transition table linked from `rules.md`.
5. Run `/api-contract-audit` skill — target ≥85/100 for this domain post-fix.

## Acceptance Criteria

- [ ] All 32 endpoints documented in `documents/01-business/kiteclass/payment-invoice/api-contract.md`
- [ ] Each endpoint has UC reference (existing or new in `use-cases.md`)
- [ ] Auth requirements specified per endpoint (header + role)
- [ ] Error codes specified per endpoint (≥3 standard + domain-specific)
- [ ] Request/response DTO schemas match real Java DTO classes (verified field-for-field)
- [ ] Webhook signature verification flow documented for VNPay/MoMo/ZaloPay
- [ ] Refund + installment state machines linked from rules.md
- [ ] `/api-contract-audit` re-run scores ≥85 for this domain

## Related

- Audit: `documents/04-quality/audits/api/api-contract-audit-2026-04-26-post-wave7.md`
- Rule: `.claude/rules/audit-to-gap-pipeline.md` Step 3 + Step 2.5 state-check
- Rule: `.claude/rules/output-review-mandate.md` §3 matrix (API contracts row)
- Living Documents: `CLAUDE.md` §"CRITICAL: Living Documents"
- Sibling gaps: GAP-232 (attendance), GAP-233 (student-enrollment)

## Log

- 2026-04-26 — Filed during post-wave-7 audit retrospective. Source: API contract audit found 13 domains with thin/missing contracts; this gap covers payment-invoice (top-1 severity by endpoint count: 32 verified, audit said 23). State-check found api-contract.md DOES exist with ~25 endpoint headings — drift is PARTIAL (depth/accuracy) not total absence; gap scope adjusted to depth-completion per `feedback_audit_calibration.md` (audit overstates by raw count; trust delta vs reality).
