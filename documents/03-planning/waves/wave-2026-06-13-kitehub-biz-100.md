---
title: Wave kitehub-biz-100 — KiteHub subscription-lifecycle gap-closure
status: draft
created: 2026-06-13
updated: 2026-06-13
waves: [kitehub-biz-100]
tag_primary: kitehub-biz
tags_secondary: [subscription, trial, lifecycle]
gaps: [GAP-1079, GAP-1080, GAP-1016, GAP-1017, GAP-1018, GAP-1095, GAP-1096, GAP-192, GAP-1026, GAP-1024, GAP-1002, GAP-1064, GAP-1044]
---

# Wave kitehub-biz-100 — KiteHub subscription-lifecycle gap-closure

**Goal:** Đóng đủ các gap tồn đọng hoàn thiện các nghiệp vụ KiteHub subscription-lifecycle ĐÃ TỒN TẠI (batch trial check, trial→paid migration, renewal/cancel/downgrade, off-boarding) + verify end-to-end (G2 walk) → KiteHub business logic đạt quality gate.
**Trigger:** User direction 2026-06-13 — chốt nghiệp vụ KiteHub trước branding-100. "Đã có authen + nâng/hạ gói; còn batch check trial + migrate data → invest đủ gap tồn đọng."
**Estimated wall-clock:** TBD sau outside-in audit (3 agents) + bucket finalize.

> **Design-first finding (per `design-first-investigation-order.md`):** batch trial check (`TrialExpirationChecker` cron `0 0 8`), subscription expiry (`SubscriptionExpirationChecker`), data retention (`DataRetentionScheduler`), trial→paid migration (`TrialToPaidService` + `MigrationStateMachine` + `MigrationScheduler` + retry + idempotency + outbox + webhook) — TẤT CẢ ĐÃ CÓ code. Scope wave = **đóng edge-case gaps + verify**, KHÔNG phải build greenfield.

---

## 1. Brainstorm

### 1.1 Inside-out (dev brainstorm — 6 cụm gap tồn đọng)

| Cụm | Gaps | Nội dung |
|---|---|---|
| A. Create/Read correctness | GAP-1079 (P1), GAP-1080 (P2) | GET active sub 400→404; POST subscriptions không idempotent (dup PENDING) |
| B. Renewal/Cancel/Downgrade hardening | GAP-1016 (P1·85%), GAP-1017 (P1·85%), GAP-1018 (P2) | manual renewal miễn phí + reactivate; cancel chưa suspend instance; renewal bỏ qua ANNUALLY + pending downgrade + idempotency + downgrade-FREE |
| C. Trial→Paid / tier-sync | GAP-192 (P0·50% phase-1.5), GAP-1095 (P2), GAP-1096 (P3) | zero-downtime migration design; convert/activate path không sync `instances.tier` |
| D. Off-boarding / domain lifecycle | GAP-1026 (P1), GAP-1024 (P1) | purge non-deleted 200 FAILED + retention warning lệch ngày; domain verification state machine kẹt PENDING |
| E. Provisioning | GAP-1002 (P1·85%) | grading_scales NULL fallback + seed new-tenant |
| F. Test infra | GAP-1064 (P2), GAP-1044 (P2) | SpringBoot IT subscription fail boot (H2 thiếu set_config RLS); stale ITs chưa auth-migrate |

### 1.2 Outside-in (audit — per `outside-in-coverage-trigger.md` §3, user chọn "cả 3 song song" 2026-06-13)

**Status:** ✅ DONE — 3 Opus agent xong 2026-06-13. Report:
- ✅ Persona simulation (11 findings) → `audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md`
- ✅ External SaaS benchmark (11 findings) → `...-benchmark.md`
- ✅ Failure-mode matrix (11 findings, code file:line) → `...-failure-mode.md`

**NET-NEW (outside-in tìm, không có trong 6 cụm inside-out) → 20 gap GAP-1253..1272 (user chọn scope "Core + P2 enrichment"):**

| Cụm outside-in | Gaps | Pri | Bản chất |
|---|---|:--:|---|
| **G3. Migration atomicity** (data-integrity THẬT) | 1253 (pessimistic lock), 1254 (retry @Tx inert), 1255 (suspend mid-migrate), 1271 (idempotency TOCTOU) | P1/P2 | BUG trong T2P machinery — double-payment, retry không nguyên tử, suspend instance đang migrate |
| **C-ext. Tier-desync sweep** | 1256 (rollback+suspend/cancel/expiry không reset tier) | P1 | extends GAP-1090/1095/1096 |
| **G1. Dunning + visibility** (manual VietQR) | 1257 (chờ-confirm vô hình), 1258 (auto-renew mismatch), 1259 (pending TTL+grace dunning), 1260 (involuntary-churn spec), 1270 (trial cadence+extension) | P1/P2 | mô hình VietQR thủ công vô hình với owner |
| **G2. Downgrade transparency** | 1261 (over-cap data-loss warning), 1262 (prorated breakdown) | P1/P2 | hạ tier mất data không cảnh báo |
| **G6. Win-back** | 1263 (fraud vs voluntary + reactivate + outreach) | P1 | tombstone chặn KH quay lại |
| **D-ext. Retention** | 1264 (suspended_at determinism + paid retention + messaging) | P2 | extends GAP-1026; PDPL clock risk |
| **G7. Notification channel** | 1265 (Zalo/in-app fallback) | P2 | extends GAP-063 |
| **G9. Receipt/portal** | 1266 (biên nhận non-VAT), 1267 (billing portal), 1268 (cancel export+undo), 1269 (tier recommender) | P2 | enrichment self-serve |
| **Doc-drift / Phase-2 defer** | 1272 (reversal-window doc-drift; ref FM-9 leader-election + FM-11 webhook nonce Phase 2) | P3 | |

**Đã defer đúng (KHÔNG vào wave):** VAT e-invoice (GAP-185/634), refund engine (GAP-183→SOP), MoMo/VNPay gateway, scheduler leader-election (dep GAP-123/479 Phase 2), webhook nonce (GAP-039 Phase 2), late-cancel (GAP-295).

### 1.3 Migrate-data scope (user chốt 2026-06-13)

= **Trial→Paid migration** (cụm C). KHÔNG bao gồm import-data tenant onboarding (nghiệp vụ mới, defer).

---

## 2. Task Breakdown (FINAL — 8 bucket, ownership-by-file để disjoint)

**Resolution overlap SubscriptionService:** tách theo FILE OWNERSHIP, không theo cụm. Bucket 0 (merge-first) gom shared schema + `InstanceRepository` + tier-sync helper + contract → BE-1/BE-2/BE-3 sau đó disjoint theo service-file (Subscription vs TrialToPaid vs Scheduler).

| # | Bucket | Gaps | Files chính (RELATIVE) | Disjoint | Spawn |
|:-:|--------|------|------------------------|:--:|:--:|
| **0** | **Foundation** (schema+repo+contract+rules) | 1253(@Lock repo), 1255(query), 1256(tier-sync helper SUB-21), 1264(suspended_at col) | `InstanceRepository.java` + Flyway `V*.sql` + `subscription-billing/{rules,api-contract}.md` + `trial-to-paid-migration/*` doc | merge-first | **MERGE FIRST** |
| 1 | BE-1 SubscriptionService lifecycle | 1079, 1080, 1016, 1017, 1018, 1096, 1260 + 1256(sub-paths via helper) | `SubscriptionService.java` + `SubscriptionRenewalService.java` + `SubscriptionController` + `GlobalExceptionHandler` | ✅ owns SubscriptionService | Batch 1 |
| 2 | BE-2 Trial→Paid atomicity | 1253(usage), 1254, 1095, 1271, 1272, 192(design) + 1256(rollback) | `TrialToPaidService.java` + `service/migration/*` + `MigrationRetryRunner` + `MigrationIdempotencyKeyService` + `MigrationScheduler` | ✅ owns TrialToPaid pkg | Batch 1 |
| 3 | BE-3 Dunning + retention scheduler | 1259, 1264(logic), 1270, 1026 | `SubscriptionExpirationChecker` + `DataRetentionScheduler` + `DataRetentionService` + `TrialExpirationChecker`(warning) | ✅ scheduler pkg | Batch 1 |
| 4 | BE-4 Notification/email/receipt | 1257(BE), 1265, 1266, 1263(BE outreach) | email templates + notification service + outbox consumers | ✅ email/notify | Batch 1 |
| 5 | BE-5 Domain + provisioning | 1024, 1002 | `DomainService` + `TenantProvisioningSaga` + `DefaultGradingScaleProvisioner` | ✅ disjoint | Batch 1 |
| 6 | TEST infra | 1064, 1044 | `*IT` config + Testcontainers/H2 RLS interceptor | ✅ test-only | Batch 1 |
| 7 | FE-1 Billing/subscription FE | 1079(consume), 1257(wait), 1258(label), 1261(downgrade warn), 1262(proration), 1267(portal), 1268(cancel wizard), 1269(recommender), 1263(FE) | `kitehub-frontend/src/app/**/billing` + subscription components | ✅ FE (consume contract Bucket 0) | Batch 2 |

**Disjoint check:** BE-1/2/3/4/5 + TEST không share file sau khi Bucket 0 gom `InstanceRepository` + tier-helper + schema. Rủi ro còn lại: BE-3 scheduler có thể gọi method `SubscriptionService` mà BE-1 đổi signature → rebase BE-3 trên BE-1 nếu xảy ra (worktree isolation).

---

## 3. Scope + spawn batching

**Stake tier:** HIGH (revenue path + multi-tenant data lifecycle + data-integrity bugs) → model: **Opus full** (per `agent-model-opus-default.md`).
**Cross-layer?** YES → **Bucket 0 = MERGE FIRST** (contract `subscription-billing/api-contract.md` + schema), per `contract-first-for-cross-layer.md`. FE-1 (Batch 2) consume contract Bucket 0.

**Spawn batching (per `feedback_parallel_agent_strategy` max-5 concurrent + `agent-concurrency-budget-inline-hybrid`):**
- **Bucket 0** merge-first (1 agent HOẶC coordinator inline — gom schema/repo/contract; reserve Flyway versions).
- **Batch 1** (5 agent parallel): BE-1, BE-2, BE-3, BE-5, TEST.
- **Batch 2** (2 agent): BE-4 (notify, sau BE-1/2 events stable), FE-1 (sau BE-1 merge contract). Coordinator inline-fill idle.
- Sau mỗi bucket: G2 walk (KH-3 nâng/hạ gói + KH-5 renewal/cancel) per `feature-ship-runtime-walk-mandate.md` + `g1-browser-walk-before-flip.md`.

**Flyway versions cần reserve (Bucket 0):** suspended_at column + migration_phase index (+ tier backfill nếu cần). → reserve khi spawn Bucket 0 (multi-session lock).

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

- ✅ Design-first ladder chạy 2026-06-13: rules.md (trial-lifecycle/subscription-billing/trial-to-paid-migration) + ADR-004/039 + gap-status.csv query + code existence verify (scheduler/T2P machinery confirmed exist).
- ⏳ Per-gap state-check (re-confirm status/AC) tại spawn-time mỗi bucket.

---

## 5. Quality-target gate (per `wave-closure-scope-completeness.md` §2.5 — "-100" = quality target)

Wave KHÔNG flip COMPLETE cho tới khi (không kể phase label):
- [ ] 0 gap OPEN/PARTIAL thuộc scope wave (trừ defer trailer user-approved)
- [ ] KH-3 + KH-5 G2 walk PASS (per `g1-browser-walk-before-flip.md` + `feature-ship-runtime-walk-mandate.md`)
- [ ] Business-logic audit re-score subscription domain (path-to-gate)
- Exception: gap PENDING external-blocked (vd GAP-192 phase-1.5 design-only) → defer trailer.

---

## 6. Bucket agent briefs (compact — spawn sau Bucket 0 merge)

Mọi agent: Opus, worktree-isolated, branch `wave/kitehub-biz-100-<bucket>`, RELATIVE paths, conventional commit (no Co-Authored-By), KHÔNG đụng `gaps/`+CSV, reference gap IDs, mở PR base main (coordinator verify+merge), per-bucket G2 walk sau merge. Dùng helper `InstanceTierSyncService` + repo `findByIdForUpdate`/`findExpiredTrials`(đã guard) + col `suspended_at` + contract SUB-23..26/TR-08 từ Bucket 0.

- **BE-1 SubscriptionService lifecycle** — files `SubscriptionService.java` + `SubscriptionRenewalService.java` + `SubscriptionController` + `GlobalExceptionHandler`. Gaps: 1079 (getActive→404 dedicated NotFoundException), 1080 (create idempotency guard PENDING+409), 1016 (manualRenewal PENDING payment), 1017 (cancel suspend instance immediate/end-cycle), 1018 (renewal ANNUALLY + apply pending downgrade + idempotent + cấm downgrade-FREE), 1096 (activateSubscription dead-code: remove HOẶC setTier), 1260 (involuntary-churn end-of-grace suspend per SUB-24), 1256-sub (wire syncInstanceTier ở applyPendingUpgrade+processRenewal). Verify: `./mvnw -pl kitehub-subscription test`.
- **BE-2 Trial→Paid atomicity** — files `TrialToPaidService.java` + `service/migration/*` + `MigrationRetryRunner` + `MigrationIdempotencyKeyService` + `MigrationScheduler`. Gaps: 1253 (dùng findByIdForUpdate ở mutating paths), 1254 (MigrationRetryRunner @Component/TransactionTemplate + check status trước retry), 1095 (convertTrialToSubscription thêm tier param + setTier + sweep callers), 1271 (persist TOCTOU catch DataIntegrityViolation→cached 202), 1272 (reconcile reversal-window clock-origin doc↔code), 192 (design doc zero-downtime — narrative), 1256-rollback (wire syncInstanceTier ở rollback:257). Verify: migration unit + retry tests.
- **BE-3 Dunning + retention scheduler** — files `SubscriptionExpirationChecker` + `DataRetentionScheduler` + `DataRetentionService` + `TrialExpirationChecker`(warning cadence). Gaps: 1259 (pending TTL auto-EXPIRED + grace dunning reminder per SUB-23), 1264 (DataRetentionService dùng suspended_at thay updated_at + paid post-suspend retention window), 1270 (trial cadence 5-7 touch + extension per TR-08), 1026 (purge non-deleted→409 + retention warning range-based + sent flag). Verify: scheduler tests.
- **BE-5 Domain + provisioning** — files `DomainService` + `TenantProvisioningSaga` + `DefaultGradingScaleProvisioner`. Gaps: 1024 (domain verification state machine: CERT_PROVISIONING + timeout→FAILED + idempotent verify), 1002 (grading_scales NULL fallback + seed new-tenant idempotent). Verify: domain + provisioning tests.
- **TEST infra** — files `*IT` config + Testcontainers/H2 RLS interceptor. Gaps: 1064 (H2 SpringBoot IT fail boot SET_CONFIG → migrate Testcontainers HOẶC skip interceptor in test), 1044 (stale ITs auth-migrate SUB-20 + @PreAuthorize drift + decide *IT CI strategy). Verify: ITs actually run green.

**Batch 2 (sau BE-1 merge):**
- **BE-4 Notification/email/receipt** — email templates + notification service + outbox consumers. Gaps: 1257-BE (pending-payment status notify), 1265 (Zalo/in-app channel fallback per GAP-063), 1266 (biên nhận non-VAT sau confirm), 1263-BE (win-back outreach email).
- **FE-1 Billing/subscription FE** — `kitehub-frontend/src/app/**/billing` + components, consume contract Bucket 0. Gaps: 1079-FE (handle 404), 1257-FE (màn "đang chờ xác nhận"+SLA), 1258 (relabel auto-renew/default-off), 1261 (downgrade impact warning+confirm), 1262 (prorated breakdown), 1267 (billing portal history/invoice/receipt), 1268 (cancel wizard export+undo-30d), 1269 (tier recommender + ENTERPRISE contact), 1263-FE (reactivate UI). Verify: `pnpm -F ... test --run && build` (per `fe-build-local-verify.md`).

---

## 7. Log

- **2026-06-13:** Plan skeleton created (inside-out 6 cụm + outside-in 3-agent pending). Per `outside-in-coverage-trigger.md` §3 — outside-in findings merge vào §1.2 TRƯỚC khi lock + PR.
- **2026-06-13:** Outside-in 3-agent DONE (persona+benchmark+failure-mode, 33 findings). Plan locked: §1.2 filled, 20 NET-NEW gap GAP-1253..1272 reserved + filed, §2 8-bucket file-ownership split, §3 spawn batching. Goal-set "complete wave kitehub-biz-100" → executing: Bucket 0 Foundation spawned (branch b0-foundation, V73 suspended_at + @Lock repo + query guard + tier-sync helper + SUB-23..26/TR-08 contract). Batch 1 briefs §6 ready.
