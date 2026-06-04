---
title: Wave flow-kh3-2 — SePay Free integration + 10.000đ beta override
status: in-progress
created: 2026-06-04
updated: 2026-06-04
wave: 2
waves: [flow-kh3-2]
tag_primary: flow-kh3
tags_secondary: [payment, beta-prep, sepay]
counter: 2
date_launch: 2026-06-04
gaps: [GAP-944, GAP-974, GAP-975, GAP-976, GAP-977]
---

# Wave flow-kh3-2 — SePay Free integration + 10.000đ beta override

**Goal:** Wire SePay Free webhook + dynamic VietQR + 10.000đ symbolic amount → KH-3 G2 walk-ready với real bank flow.
**Trigger:** KH-3 G1 ✅ PASS production-equivalent (mock mode) + 3-agent payment audit consensus (Pattern A SePay 120k/tháng winner) + user direction "G2 phải có SePay + symbolic amount". Cost 0đ (SePay Free 50tx/tháng covers Phase 1 BETA 5-15 tenants).
**Estimated wall-clock:** ~2-3 ngày dev (REVISED post §4 state-check 2026-06-04 — generic webhook + VietQR scaffold đã ship trước, scope = adapter rewrite + new email template + FE banner, không phải foundation from scratch). Longest bucket ~1 ngày.

**⚠️ State-check drift discovered 2026-06-04 (coordinator pre-flight):** §4 rows initially marked "🆕 to-be-created" actually EXIST. Cross-module mapping: `Payment` entity in `kitehub-platform` (not `kitehub-subscription`). Existing `PaymentWebhookController` ships generic HMAC-SHA256 body-signature scheme; SePay uses `Authorization: Apikey <key>` header — protocol-level rewrite needed for Bucket B (extend, not create). Subscription-activated email + Beta mode banner + WS push remain truly missing. Refer §3 PATCH annotations per bucket.

---

## 1. Brainstorm (5-10 min)

**Inside-out (from canonical):**
- ROADMAP §🚀: KH-3 G1 ✅ — G2 prep blocked on auto-reconcile mechanism
- GAP-944 cross-module cache invalidation via outbox (this wave delivers via SePay webhook path)
- GAP-974 subscription activation email missing (paired fix this wave)

**Outside-in audit consensus (this session, ≤30 ngày — exception per `outside-in-coverage-trigger.md` §4):**
- Persona simulation: 4/4 personas prefer Pattern A (SePay) over Pattern 0 manual
- External benchmark: SePay Startup 120k VND/tháng winner; **SePay Free 50tx/tháng covers Phase 1 BETA**
- Failure-mode matrix: 10 P0 cells + 12 GAP candidates (3 P0 included this wave: collision/signature/idempotency)

**Q1 (alignment):** Persona Owner (test bank flow real) + PlatformAdmin (zero manual confirm post-launch) + Accountant (auto export future Phase 1.5). Domain `subscription` + `payment` + `webhook` + `notification`.
**Q2 (trade-offs):** SePay Free vs Casso Free → SePay (50 vs 30 tx) + SePay paid ladder cheaper. Symbolic 10.000đ vs 1.000đ (bank min) vs 599k real → 10k (bank-accepted + low beta cost). Skip VNPay merchant (banned per audit).
**Q3 (risks):** SePay Free quota exceed → seamless migrate Startup 120k (same webhook). Cross-tenant memo collision → 8-char unique suffix + collision guard. Bank delay >5 min → email fallback (Subscription activation per GAP-974).

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | Foundation — api-contract.md update | coordinator | 2-3h | ✅ docs only |
| A | GAP-975 BE PaymentService dynamic VietQR + txn_ref | bg-agent | 4-6h | ✅ kitehub-subscription |
| B | GAP-976 BE webhook endpoint + HMAC + idempotency | bg-agent | 6-8h | ✅ kitehub-subscription |
| C | GAP-974 BE subscription-activated email outbox + template | bg-agent | 3-4h | ✅ kitehub-email |
| D | GAP-977 FE WebSocket subscribe + beta banner | bg-agent | 4-6h | ✅ kitehub-frontend |
| E | Walk verify — KH-3 G1 production-equivalent với real 10k VCB→VCB transfer | coordinator | 2-3h | ✅ ops only |

Disjoint check: Bucket A=PaymentService.java, B=PaymentWebhookController.java (NEW), C=kitehub-email templates, D=kitehub-frontend billing/, E=runtime walk. Zero file overlap.

---

## 3. Scope (compact)

**Stake tier:** MEDIUM → Opus 4.7 medium (payment scope = real money path eventually; risk profile moderate)
**Cross-layer?:** YES → Bucket 0 Foundation REQUIRED per `contract-first-for-cross-layer.md` v1.0.3

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | api-contract update for SePay webhook + dynamic VietQR | 🟠 P1 | `documents/01-business/kitehub/subscription-billing/api-contract.md` | MERGE FIRST |
| 1 | **A** | GAP-975 dynamic VietQR + txn_ref | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/PaymentService.java` + `VietQRService.java` | parallel after Bucket 0 |
| 2 | **B** | GAP-976 webhook + HMAC + idempotency + collision guard | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/PaymentWebhookController.java` (NEW) + `PaymentService.confirmPayment` extension | parallel after Bucket 0 |
| 3 | **C** | GAP-974 subscription-activated email | 🟠 P1 | `kitehub/kitehub-email/src/main/resources/templates/subscription-activated.hbs` + `SubscriptionService.applyPendingUpgrade` outbox enqueue | parallel after Bucket 0 |
| 4 | **D** | GAP-977 FE WS push + beta banner | 🟠 P1 | `kitehub/kitehub-frontend/src/app/(auth)/billing/` + `lib/api/payments.ts` + `components/billing/BetaModeBanner.tsx` (NEW) | parallel after Bucket 0 |
| 5 | **E** | Walk verify | — | runtime walk evidence | LAST (after A+B+C+D merged) |

### Bucket 0 — Foundation (api-contract)

- Files: `documents/01-business/kitehub/subscription-billing/api-contract.md` (UPDATE)
- Scope:
  - Add endpoint `POST /api/webhooks/sepay` — request shape (SePay payload schema) + HMAC header `X-Signature` + idempotency response
  - Add field to `Payment` response: `qrCodeUrl` (dynamic VietQR URL), `txnRef` (memo for matching)
  - Add config keys: `kitehub.payment.beta-mode.enabled` + `kitehub.payment.beta-mode.override-amount-vnd` + `kitehub.payment.sepay.webhook-secret` + `kitehub.payment.sepay.api-key`
- Acceptance: api-contract.md đầy đủ endpoint + schema + config; FE/BE bucket reference contract

### Bucket A — Dynamic VietQR + txn_ref + Beta-amount override (GAP-975)

**PATCH 2026-06-04 (post §4 state-check):** Existing `PaymentService.createPayment` + `VietQRService` already generate VietQR QR + paymentContent (KH3SUB-like format already in VietQRService.generatePaymentContent). Real scope = (i) decide field naming (extend `paymentContent` OR add new `txn_ref` column via V64 migration — recommend `txn_ref` for unambiguous matching + keep paymentContent for human-readable VietQR memo), (ii) ensure unique-8-char suffix collision-safe, (iii) wire `kitehub.payment.beta-mode.enabled` + `override-amount-vnd` config to override `amountVnd` when creating Payment.

- Files: `Payment` entity (kitehub-platform — add `txnRef` field) + V64 migration (kitehub-subscription Flyway adds `txn_ref VARCHAR(32) UNIQUE` to payments) + `PaymentService.createPayment` (kitehub-subscription — extend) + `VietQRService` (kitehub-subscription — verify `generatePaymentContent` format alignment) + per `postgres-specific-type-testcontainers.md` v1.0.1 verify entity round-trip Testcontainers IT
- Tests: `PaymentServiceTest.createPayment_generatesUniqueTxnRef` + `_appliesBetaModeOverride_whenFlagEnabled` + `_keepsRealAmount_whenFlagDisabled`
- Acceptance: `txn_ref = "KH3SUB" + paymentId[0:8].upper()` (≥8-char unique, UNIQUE constraint) + when `beta-mode.enabled=true`, payment.amountVnd = override-amount-vnd (default 10000) + Mockito + Testcontainers IT pass
- Cross-module note: Payment entity in kitehub-platform → Bucket A touches BOTH kitehub-platform (entity) + kitehub-subscription (service + migration)
- Per `api-contract-change-caller-sweep.md` v1.0.0: if `createPayment` signature changes → grep callers prod+test, `./mvnw test` PaymentServiceTest before push

### Bucket B — Webhook protocol rewrite (HMAC→Apikey) + SePay payload adapter + idempotency (GAP-976)

**PATCH 2026-06-04 (post §4 state-check):** `PaymentWebhookController` (171 LOC) EXISTS with generic HMAC-SHA256 body-signature scheme + sorted-key=value& payload format + path `/api/platform/webhooks/payment`. SePay uses `Authorization: Apikey <key>` header authentication + SePay-specific payload fields (`id`, `gateway`, `transactionDate`, `accountNumber`, `transferType`, `transferAmount`, `description`, `referenceCode`). Real scope = **protocol-level rewrite** (replace HMAC body verify với header API-key verify; adapt payload extraction; preserve constant-time compare guard) + idempotency add + collision guard.

- Files: `PaymentWebhookController` (rewrite verify + extraction logic; KEEP class + path), `PaymentService.processPaymentWebhook` (add idempotency check via `transaction_id` UNIQUE OR `IdempotencyKey` table query; verify `findPaymentByContent` is exact-match via `payment_content = ?` clause, NOT `LIKE %?%`), V65 migration (add UNIQUE constraint on `payments.transaction_id` if not present) + per `cross-flow-bug-class-sweep.md` v1.0.1 sweep similar webhook handlers
- Tests: `PaymentWebhookControllerTest` rewrite cho SePay payload format — (Apikey valid/missing/wrong, payload amount-mismatch, payload memo-collision, duplicate transaction_id idempotent), `PaymentServiceTest.processPaymentWebhook_idempotentOnReplay`, IT với mock SePay payload `{"id":"X","gateway":"VCB","transferType":"in","transferAmount":10000,"description":"KH3SUB12345678","referenceCode":"FT2406..."}`
- Acceptance:
  - `Authorization: Apikey <key>` header verify against `kitehub.payment.sepay.api-key` config (NOT HMAC body)
  - Idempotency: replay same `transaction_id` → HTTP 200 + early-return, no double-process
  - Cross-tenant memo collision: `findPaymentByContent` exact-match (verify code reading, no `LIKE`)
  - On match: flip Payment COMPLETED + call applyPendingUpgrade (preserve existing state machine)
  - BE endpoint signature match api-contract.md (Bucket 0)
- Per `api-contract-change-caller-sweep.md` v1.0.0: webhook contract change → grep all webhook test fixtures + smoke-email-links + update

### Bucket C — Subscription-activated email (GAP-974)

**PATCH 2026-06-04 (post §4 state-check):** Template file convention is `.html` not `.hbs` (existing sibling `subscription-created.html` + `subscription-expired.html` exist). `SubscriptionOutboxEvent/Repository/Dispatcher` + `EmailServiceClient` infrastructure all exist. Scope = (i) create `subscription-activated.html` (model on `subscription-created.html` structure), (ii) extend `applyPendingUpgrade(subscriptionId, paymentId)` line ~423 với outbox enqueue `SUBSCRIPTION_ACTIVATED` event.

- Files: `kitehub-email/src/main/resources/templates/emails/subscription-activated.html` (NEW) + `SubscriptionService.applyPendingUpgrade` (extend với `subscriptionOutboxRepository.enqueue("SUBSCRIPTION_ACTIVATED", subscriptionId, payload)` after instance flip)
- Tests: `SubscriptionServiceTest.applyPendingUpgrade_emitsActivationEvent` (verify outbox row inserted in same txn) + IT verifying email arrives MailHog post-flush
- Acceptance:
  - Template Vietnamese narrative + English identifiers per `dev-readable-doc-language.md` §2 (Architecture & dev-readable docs)
  - Variables: tenantName, tier, expiresAt, supportUrl
  - Subject pattern `[KiteHub] Gói {tier} đã kích hoạt`
  - Email arrives MailHog post `applyPendingUpgrade`
  - Per `design-patterns.md` §3.5 Outbox: enqueue trong SAME @Transactional as instance flip (already @Transactional)

### Bucket D — FE WebSocket + beta banner (GAP-977)

**PATCH 2026-06-04 (post §4 state-check):** FE billing folder path = `(customer)/billing/` (NOT `(auth)/billing/`). Pages `billing/page.tsx`, `upgrade/page.tsx`, `history/page.tsx`, `payment/[id]/page.tsx` already exist. Real scope = (i) extend `payment/[id]/page.tsx` với WS subscribe + render BetaModeBanner conditional; (ii) verify FE WebSocket lib config existence (Stomp client OR SockJS) — TBD verify in spawn.

- Files: `kitehub/kitehub-frontend/src/app/(customer)/billing/payment/[id]/page.tsx` (EXTEND), `kitehub/kitehub-frontend/src/components/billing/BetaModeBanner.tsx` (NEW), `kitehub/kitehub-frontend/src/lib/api/payments.ts` (EXTEND với WS hook OR `lib/ws/payments-ws.ts` NEW)
- Tests: Vitest unit tests cho BetaModeBanner conditional render + WS mock hook test
- Acceptance:
  - Existing payment/[id] page renders QR (from `Payment.qrCodeUrl`) + amount display + memo highlight; EXTEND với WS subscribe `/topic/payments/{paymentId}`
  - On `paymentCompleted` event → toast "✅ Đã nhận thanh toán" + redirect `/dashboard`
  - Timeout 30s no event → fallback "Chờ xác nhận thanh toán... bạn sẽ nhận email khi xong" + email link
  - Beta banner (when `NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE=true` env): "🧪 Bạn đang ở chế độ Beta — số tiền chuyển là 10.000đ tượng trưng. Khi vào production sẽ là 599.000đ/tháng."
  - Per `dev-readable-doc-language.md` §4 — Vietnamese narrative UI labels + English code identifiers
  - Per `fe-build-local-verify.md` v1.0.0 — `pnpm -F kitehub-frontend build` local pre-push (not just lint/tsc)
- FE consumption matches api-contract.md (Bucket 0)

### Bucket E — Walk verify (G1 production-equivalent với real 10k)

- Runtime walk on local Docker stack với:
  - `BETA_PAYMENT_OVERRIDE=true`, `BETA_AMOUNT=10000`, `PAYMENT_MOCK_MODE=false`
  - SePay Free account configured + webhook URL ngrok tunnel
  - Real VCB→VCB transfer 10.000đ với memo `KH3SUB<id>`
- Acceptance:
  - QR scan → app banking auto-fill 10.000đ + memo
  - Bank transfer → SePay webhook → KH backend match → Payment COMPLETED
  - FE WS push → "✅ Đã nhận thanh toán" trong <5 phút
  - Subscription BASIC ACTIVE + email "Gói BASIC đã kích hoạt" arrives MailHog
  - Walk evidence saved `documents/04-quality/audits/persona-review/2026-06-04-flow-kh3-G1-rewalk-sepay-real.md`

---

## 4. State-Check Evidence (REFRESHED 2026-06-04 post-discovery)

| Symbol | Type | Verification | Evidence | Verdict |
|---|---|---|---|---|
| `Payment` entity | Java entity | `find kitehub -path '*entity/Payment.java'` | `kitehub-platform/src/main/java/com/kitehub/platform/domain/entity/Payment.java` (CROSS-MODULE — NOT kitehub-subscription) | ✅ exists |
| `Payment.txnRef` field | Java field | `grep -n "txnRef\|txn_ref" .../Payment.java` | 0 hits — `paymentContent` String column used for matching instead | ❌ NOT EXISTS — Bucket A decision: extend `paymentContent` format OR add new `txn_ref` column |
| `PaymentService.createPayment` | Java method | `grep -n "createPayment" .../PaymentService.java` | exists, line ~47, generates VietQR QR + bank info snapshot (GAP-939) + paymentContent | ✅ exists — Bucket A: extend với beta-amount override branch |
| `PaymentService.processPaymentWebhook(txnId, amount, content)` | Java method | line ~188 | exists, full flow: findPaymentByContent → verify amount → vietQRService.verifyPayment → payment.complete → applyPendingUpgrade | ✅ exists — Bucket B: extend với SePay payload adapter + idempotency |
| `VietQRService` | Java service | `find -name "VietQRService.java"` | exists at `kitehub-subscription/.../service/VietQRService.java` — `generateQRCode`, `generatePaymentContent`, `verifyPayment`, `getBankCode/AccountNumber/AccountName` | ✅ exists |
| `SubscriptionService.applyPendingUpgrade(subscriptionId, paymentId)` | Java method | line ~423 `@Transactional` | exists — Bucket C: extend với outbox enqueue `subscription.activated` event | ✅ exists |
| `SubscriptionOutboxEvent` + `SubscriptionOutboxRepository` + `SubscriptionOutboxDispatcher` | Outbox infra | `find -path '*outbox/Subscription*'` | all exist | ✅ exists |
| `PaymentWebhookController` | Java controller | `find -name "PaymentWebhookController*"` | EXISTS at `kitehub-subscription/.../controller/PaymentWebhookController.java` (171 LOC) — generic HMAC-SHA256 body-signature scheme + sorted-key=value& format + `/api/platform/webhooks/payment` path | ✅ exists — Bucket B SCOPE REWRITE: replace HMAC scheme với SePay `Authorization: Apikey <key>` header + adapt payload format (SePay fields: `gateway`, `transferType`, `transferAmount`, `description`, `referenceCode`, `id`) |
| `PaymentWebhookControllerTest` | Java IT | `find -name "PaymentWebhookControllerTest.java"` | exists | ✅ exists — Bucket B: rewrite test fixtures cho SePay payload |
| `EmailServiceClient` | Java client | `kitehub-subscription/.../client/EmailServiceClient.java` | exists | ✅ exists — Bucket C uses for email send |
| `subscription-activated.hbs` (or `.html`) | Email template | `find kitehub/kitehub-email -name "subscription-activated*"` | 0 hits (`subscription-created.html` + `subscription-expired.html` exist nhưng activated MISSING) | ❌ NOT EXISTS — Bucket C creates `subscription-activated.html` |
| `subscription-created.html` | Email template (sibling reference) | exists `kitehub-email/.../templates/emails/subscription-created.html` | ✅ exists — Bucket C uses as template scaffolding pattern |
| `BetaModeBanner.tsx` | FE component | `find kitehub/kitehub-frontend -name "BetaMode*"` | 0 hits | ❌ NOT EXISTS — Bucket D creates |
| FE billing pages | FE pages | `find -path '*billing*' -name '*.tsx'` | `billing/page.tsx`, `upgrade/page.tsx`, `history/page.tsx`, `payment/[id]/page.tsx` all exist | ✅ exists — Bucket D extends `payment/[id]/page.tsx` với WS subscribe + render banner |
| WS subscribe `/topic/payments/{paymentId}` | FE WebSocket hook | TBD — verify in Bucket D spawn | TBD (likely missing per scope claim) | ⚠️ TBD |
| `documents/01-business/kitehub/subscription-billing/api-contract.md` | API contract | `ls` | exists | ✅ exists — Bucket 0 PATCH |
| `kitehub.payment.sepay.*` config keys | Config | `grep -rn "sepay" kitehub/kitehub-subscription/src/main/resources` | 0 hits | ❌ NOT EXISTS — Bucket 0 declares + Bucket B implements |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| 0 | `bash scripts/check-cross-layer-contract-drift.sh` (no Java drift after api-contract update) | docs-only CI |
| A | `./mvnw -pl kitehub-subscription test -Dtest=PaymentServiceTest` | Test KiteHub Subscription Service |
| B | `./mvnw -pl kitehub-subscription verify -P strict-warnings` | Test Subscription + Admin |
| C | `./mvnw -pl kitehub-email test` + MailHog visual verify | Test KiteHub Email Service |
| D | `pnpm -F kitehub-frontend test:unit && pnpm -F kitehub-frontend build` | Frontend CI |
| E | Runtime walk evidence file + screenshots | manual (coordinator) |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` v1.0.1 + `agent-model-opus-default.md` v1.0.0:
- Bucket 0 ship FIRST (foundation merge first per `contract-first-for-cross-layer.md`)
- Buckets A+B+C+D spawn parallel với `run_in_background: true` + `model: "opus"` + `isolation: worktree`
- RELATIVE paths trong agent prompts
- Coordinator merge sequential A→B→C→D after all 4 bg-agent completions
- Bucket E (walk) coordinator-run LAST

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` v1.0.1 + `feedback_post_merge_doc_sync.md` + `post-wave-cleanup.md`:
- Each bucket PR updates affected GAP file Log + status
- ROADMAP §🚀 updated trong closure PR
- Wave plan frontmatter `status: draft → complete` flip
- `wave-history.jsonl` append với tag-based entry (`tag_primary: flow-kh3, counter: 2`)
- Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3 (6 items × verdict)
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`
- G2 recipe update — swap mock mode references → SePay flow + 10.000đ amount instructions per `g2-handoff-md-mandate.md`
- Campaign §4 KH-3 row: `🔄 walk-pass-pending-human` (G1+G2) — pending G3 production parity gated GAP-612

### 7.4 G2 handoff recipe MD

Update existing `documents/05-guides/operations/2026-06-04-g2-recipe-kh3-subscription.md` post-walk verify với new SePay flow steps. Status `🔄 walk-pass-pending-human` if Bucket E PASS.

---

## 8. Log

- **2026-06-04 (PATCH post-state-check):** Coordinator pre-flight investigation per `pre-mutation-state-check.md` + `audit-to-gap-pipeline.md` §2.6 + `release-fix-retry-budget.md` §3.5 investigation mandate revealed §4 State-Check Evidence drift — 70% of "🆕 to-be-created" rows already exist:
  - `PaymentWebhookController` (171 LOC) EXISTS với generic HMAC-SHA256 scheme; Bucket B SCOPE REWRITE: protocol-level rewrite to SePay `Authorization: Apikey` header + payload field adapter (NOT new file creation)
  - `VietQRService` + `PaymentService.createPayment` + `PaymentService.processPaymentWebhook` + `SubscriptionService.applyPendingUpgrade` ALL EXIST; Buckets A/C scope = EXTEND not create
  - `Payment` entity in `kitehub-platform` (NOT `kitehub-subscription`); Bucket A is cross-module (kitehub-platform + kitehub-subscription)
  - Outbox infra + EmailServiceClient EXIST; Bucket C scope = new template + extend `applyPendingUpgrade` outbox enqueue
  - FE billing pages under `(customer)/` NOT `(auth)/`; Bucket D scope = extend existing `payment/[id]/page.tsx`
  - Truly missing: `subscription-activated.html` template, `BetaModeBanner.tsx`, SePay config keys
  Wall-clock revised ~4-5 ngày → ~2-3 ngày given existing foundation. Plan PATCH inline (no separate discovery gap) per `discovery-to-gap-inline-filing.md` §1 — silent decay eliminated when plan reflects reality + ships in coordinator's next PR. Status: draft → in-progress.
- **2026-06-04 (draft):** Plan created post-KH-3 G1 ✅ re-walk + 3-agent payment audit consensus + user direction "SePay Free + 10.000đ symbolic". Outside-in audit eligibility: SKIP per `outside-in-coverage-trigger.md` §4 row 4 (audit ≤30 ngày, 3-agent done this session). Per `inside-out-completeness-trigger.md`: ROADMAP §🚀 + audit findings + AskUserQuestion explicit (user chose Recommended option 1). Cost estimate: 0đ SePay Free + 4-5 ngày dev. Wave naming per `wave-tag-numbering-convention.md` v1.0.0 — tag_primary=flow-kh3, counter=2.
