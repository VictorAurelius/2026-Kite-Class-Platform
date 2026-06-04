---
title: Wave flow-kh3-2 — SePay Free integration + 10.000đ beta override
status: draft
created: 2026-06-04
updated: 2026-06-04
wave: 2
tag_primary: flow-kh3
tags_secondary: [payment, beta-prep, sepay]
counter: 2
date_launch: 2026-06-04
gaps: [GAP-944, GAP-974, GAP-975, GAP-976, GAP-977]
---

# Wave flow-kh3-2 — SePay Free integration + 10.000đ beta override

**Goal:** Wire SePay Free webhook + dynamic VietQR + 10.000đ symbolic amount → KH-3 G2 walk-ready với real bank flow.
**Trigger:** KH-3 G1 ✅ PASS production-equivalent (mock mode) + 3-agent payment audit consensus (Pattern A SePay 120k/tháng winner) + user direction "G2 phải có SePay + symbolic amount". Cost 0đ (SePay Free 50tx/tháng covers Phase 1 BETA 5-15 tenants).
**Estimated wall-clock:** ~4-5 ngày dev, longest bucket ~1.5 ngày.

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

### Bucket A — Dynamic VietQR + txn_ref (GAP-975)

- Files: `PaymentService.createPendingPayment` + `VietQRService` (txn_ref gen + URL build)
- Tests: `PaymentServiceTest.createPendingPayment_generatesUniqueTxnRef` + `_buildsDynamicVietQRUrl`
- Acceptance: `txn_ref = "KH3SUB" + paymentId[0:8].upper()` (≥8-char unique) + VietQR URL embed amount + memo per benchmark §3.2 pattern
- Cross-layer FE consumption: `Payment.qrCodeUrl` returned in POST response — FE renders QR + amount + memo display

### Bucket B — Webhook + HMAC + idempotency (GAP-976)

- Files: `PaymentWebhookController` NEW + `PaymentService.processWebhook` NEW
- Tests: `PaymentWebhookControllerTest` (HMAC valid/invalid/replay/amount-mismatch/memo-collision) + IT với mock SePay payload
- Acceptance:
  - HMAC SHA-256 verify against `kitehub.payment.sepay.webhook-secret`
  - Idempotency: `transaction_id` UNIQUE constraint + early-return if seen
  - Cross-tenant memo collision guard: `findByTxnRef` exact-match (NOT substring greedy per failure-mode audit)
  - On match: flip Payment COMPLETED + call SubscriptionService.applyPendingUpgrade (per existing state machine)
- BE controller signature match api-contract.md (Bucket 0)

### Bucket C — Subscription-activated email (GAP-974)

- Files: `subscription-activated.hbs` (kitehub-email templates) + `SubscriptionService.applyPendingUpgrade` outbox enqueue
- Tests: `SubscriptionServiceTest.applyPendingUpgrade_emitsActivationEmail` + integration via subscription_outbox
- Acceptance:
  - Template Vietnamese narrative + English identifiers per `dev-readable-doc-language.md` §4
  - Variables: tenantName, tier, expiresAt, supportUrl
  - Subject pattern `[KiteHub] Gói {tier} đã kích hoạt`
  - Email arrives MailHog post `applyPendingUpgrade`

### Bucket D — FE WebSocket + beta banner (GAP-977)

- Files: `(auth)/billing/page.tsx` + `lib/api/payments.ts` (WS hook) + `components/billing/BetaModeBanner.tsx`
- Tests: Vitest unit tests + MSW handler for WS mock
- Acceptance:
  - Billing page POST subscription → render QR (from `Payment.qrCodeUrl`) + amount display + memo highlight + WS subscribe `/topic/payments/{paymentId}`
  - On `paymentCompleted` event → toast "✅ Đã nhận thanh toán" + redirect /dashboard
  - Timeout 30s no event → fallback "Chờ xác nhận thanh toán... bạn sẽ nhận email khi xong" + email link
  - Beta banner (when `BETA_PAYMENT_OVERRIDE=true` flag env): "🧪 Bạn đang ở chế độ Beta — số tiền chuyển là 10.000đ tượng trưng. Khi vào production sẽ là 599.000đ/tháng."
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

## 4. State-Check Evidence

| Symbol | Type | Verification | Evidence | Verdict |
|---|---|---|---|---|
| `PaymentService.createPendingPayment` | Java method | `grep -n "createPendingPayment" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/PaymentService.java` | exists line ~144 | ✅ exists |
| `VietQRService` | Java service | `find kitehub/kitehub-subscription -name "VietQRService.java"` | exists | ✅ exists |
| `SubscriptionService.applyPendingUpgrade` | Java method | `grep -n "applyPendingUpgrade" .../SubscriptionService.java` | exists | ✅ exists |
| `subscription_outbox` | DB table | `docker exec kite-postgres psql -U kitehub -d kitehub -c '\d subscription_outbox'` | exists (verified this session) | ✅ exists |
| `PaymentWebhookController` | Java controller | `find kitehub -name "PaymentWebhookController*"` | 0 hits | 🆕 to-be-created (Bucket B) |
| `subscription-activated.hbs` | Email template | `find kitehub/kitehub-email -name "subscription-activated*"` | 0 hits | 🆕 to-be-created (Bucket C) |
| `BetaModeBanner.tsx` | FE component | `find kitehub/kitehub-frontend -name "BetaModeBanner*"` | 0 hits | 🆕 to-be-created (Bucket D) |
| `documents/01-business/kitehub/subscription-billing/api-contract.md` | API contract | `ls documents/01-business/kitehub/subscription-billing/api-contract.md` | exists | ✅ exists (UPDATE in Bucket 0) |
| `kitehub.payment.sepay.*` config keys | Config | `grep -rn "sepay" kitehub/kitehub-subscription/src/main/resources` | 0 hits | 🆕 to-be-created (Bucket 0 + B) |
| `Payment.txnRef` field | Java entity | `grep -n "txnRef\|txn_ref" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/entity/Payment.java` | TBD — verify before Bucket A spawn | TBD |

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

- **2026-06-04 (draft):** Plan created post-KH-3 G1 ✅ re-walk + 3-agent payment audit consensus + user direction "SePay Free + 10.000đ symbolic". Outside-in audit eligibility: SKIP per `outside-in-coverage-trigger.md` §4 row 4 (audit ≤30 ngày, 3-agent done this session). Per `inside-out-completeness-trigger.md`: ROADMAP §🚀 + audit findings + AskUserQuestion explicit (user chose Recommended option 1). Cost estimate: 0đ SePay Free + 4-5 ngày dev. Wave naming per `wave-tag-numbering-convention.md` v1.0.0 — tag_primary=flow-kh3, counter=2.
