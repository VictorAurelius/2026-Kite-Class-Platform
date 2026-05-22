---
id: GAP-722
title: VietQR live payment integration Phase 1.5+ unblock plan — merchant verification + production API key
status: OPEN
priority: P2
phase: phase-1.5-paid
audience: dev
found: 2026-05-22
last_verified: 2026-05-22
completion_pct: 0
related: [GAP-720, GAP-721, GAP-612]
---

# GAP-722 — VietQR live payment integration Phase 1.5+ unblock plan

## Problem

Wave 105 Bucket B Owner persona walk (per `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md` §1 Step 8) verified `VietQRService.java` code path exists với `payment.vietqr.mock-mode` flag (default `false` — call real API). Local dev: mock mode OK. Production cutover BLOCKED bởi:

1. **Merchant bank account verification** — KiteHub cần register merchant với VietQR provider (Napas, MoMo, hoặc bank-direct integration)
2. **Production API key** — VietQR API key (production tier — bank verification + rate limit tier)
3. **VietQR `payment.vietqr.account-number` config** — currently default `1234567890` (placeholder); needs real KiteHub bank account
4. **Compliance audit** — VN State Bank (SBV) merchant compliance (KYC + AML) cho payment processing

**Persona impact (Hằng — Owner):**
- Phase 1 BETA: Hằng KHÔNG bị charge real money (mock mode default trong invoice flow)
- Phase 1.5+ PAID release: Hằng pay subscription qua VietQR → MUST have live integration
- Hệ quả: Phase 1 BETA → Phase 1.5+ cutover phụ thuộc unblock plan này

## Root cause

Original design Wave 4+ shipped VietQR service code path nhưng deferred production cutover deps. Per wave plan §11: "VietQR live payment integration deferred Phase 1.5+ — mock locally OK; document deferral".

GAP-612 (AWS account suspended) compounds: production cutover blocked anyway until AWS restored.

## Proposed Fix

### Phase 1 (this gap — unblock plan documentation)

Document concrete unblock dependencies + sequence in runbook:

1. **AWS account restoration** (GAP-612) — production endpoint reachable
2. **VietQR merchant registration** — choose vendor (Napas vs MoMo vs bank-direct), submit KYC docs, wait approval (~2-4 weeks)
3. **Production API key issuance** — receive key + secret, store via AWS Secrets Manager per `production-env-config-registry.md`
4. **Config update** — `payment.vietqr.account-number` + `account-name` + `bank-code` set to real KiteHub merchant account
5. **SBV compliance audit** — KYC + AML procedural confirmation
6. **Smoke test** — `VietQRServiceTimeoutTest` + new live IT against vendor sandbox endpoint
7. **Cutover** — flip `payment.vietqr.mock-mode=false` in production application.yml + restart subscription service

### Phase 2 (Phase 1.5+ — execute, out-of-scope this gap)

Concrete execution work tracked via dedicated release plan `documents/03-planning/roadmap/release-1.5-paid-plan.md` (TBD).

## Acceptance Criteria

- [ ] Runbook `documents/05-guides/deploy/vietqr-live-integration-runbook.md` created với 7-step unblock sequence
- [ ] `documents/01-business/kitehub/subscription-billing/rules.md` adds BR-BILLING-NEW "VietQR live cutover dependency chain"
- [ ] Phase 1.5 release plan `release-1.5-paid-plan.md` includes VietQR cutover bucket
- [ ] Wave 105 wave plan §11 cross-link added: "VietQR live → GAP-722 unblock plan"

## Related

- Persona walk: `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md` §1 Step 8
- AWS dep: GAP-612 (account 906286017800 suspended)
- Sister gaps Wave 105 Bucket B: GAP-720 (multi-branch), GAP-721 (Zalo OA stub)
- Code: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/VietQRService.java`
- Tests: `VietQRServiceTest`, `VietQRServiceTimeoutTest`
- Wave plan defer: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` §Open Items "AWS production cutover"
- VN compliance per `business-logic-review.md` §2.4 — Luật Quản lý Thuế 2019 + Nghị định 123/2020/NĐ-CP

## Log

- **2026-05-22:** Gap filed. Wave 105 Bucket B Owner persona walk verified mock-mode local PASS; production cutover deferred Phase 1.5+ deps documented here.
