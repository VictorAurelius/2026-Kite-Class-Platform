# Failure-Mode Matrix Audit — KiteHub Subscription-Lifecycle / Trial→Paid Migration

**Ngày:** 2026-06-13
**Phương pháp:** Outside-in 3-axis failure matrix (`simulation-gap-finder.md` §5 stress-test) — [lifecycle STATE] × [ACTOR action] × [FAULT]
**Phạm vi:** `kitehub-subscription` migration state machine + scheduler cluster + subscription/renewal/retention services
**Investigation order (per `design-first-investigation-order.md`):** DESIGN (trial-to-paid + trial-lifecycle + subscription-billing rules.md + ADR-004) → GAPS (`gap-status.csv` dedup) → CODE (grounded, file:line cited)
**Auditor:** outside-in failure-mode agent (NET-NEW vs inside-out 6-cluster dedup)

> Mục tiêu: liệt kê failure mode / edge case / race condition mà inside-out brainstorm bỏ sót. Mỗi cell: failure → hệ quả → guard exists? (code-ref) → gap level + fix.

---

## 1. Tóm tắt — 11 findings (8 NET-NEW, 3 overlap)

| # | Title | Severity | Matrix cell | Guard | Dedup |
|---|---|:---:|---|:---:|---|
| FM-01 | T2P-08 pessimistic lock KHÔNG tồn tại — concurrent upgrade double-migrate | P1 | TRIAL × owner upgrade ×2 đồng thời × concurrent-upgrade | ❌ unguarded | **NET-NEW** |
| FM-02 | `MigrationRetryRunner` `@Transactional` inert (non-bean + self-invocation) — atomicity invariant vỡ | P1 | MIGRATING × scheduler tick × retry-exhausted/partial | ❌ unguarded | **NET-NEW** |
| FM-03 | Retry partial-success → double `convertTrialToSubscription` trên instance đã ACTIVE | P2 | MIGRATING × scheduler tick × retry after partial flip | ⚠️ partial (phụ thuộc convert idempotency) | **NET-NEW** |
| FM-04 | Tier desync trên ROLLBACK — `instances.tier` không restore khi REVERSED | P1 | ACTIVE × webhook reversal × payment-reversed-mid-window | ❌ unguarded | **NET-NEW** (sister GAP-1090/1095/1096) |
| FM-05 | Tier desync trên expiry/suspend/cancel — `instances.tier` kẹt paid-tier | P2 | grace→SUSPENDED × scheduler tick / owner cancel × tier-desync | ❌ unguarded | **NET-NEW** |
| FM-06 | Trial expires during MIGRATING — `findExpiredTrials` không lọc `migration_phase` → suspend giữa chừng | P1 | TRIAL(mid-migrate) × scheduler tick × trial-expires-during-MIGRATING | ❌ unguarded | **NET-NEW** |
| FM-07 | Scheduler chạy trên ≥2 replica — không leader-election (cron schedulers 0 lock) | P2 | mọi STATE × scheduler tick × runs-on-2-leaders | ⚠️ partial (in-JVM AtomicBoolean chỉ MigrationScheduler) | **NET-NEW** (dep GAP-123 HPA) |
| FM-08 | Retention clock = `updatedAt` (không phải `suspendedAt`) — bất kỳ update reset đồng hồ purge | P2 | SUSPENDED × scheduler tick × retention-purge-race | ❌ unguarded | **NET-NEW** (partial overlap GAP-1026) |
| FM-09 | Reversal-window tính từ `migrationCompletedAt` ≠ T2P-04 "after PAYMENT_CAPTURED" | P3 | COMPLETED × webhook reversal × clock-origin drift | ⚠️ doc-vs-code drift | **NET-NEW** |
| FM-10 | Idempotency persist TOCTOU + UNIQUE-violation poisons txn → loser nhận 500 thay 202 | P2 | TRIAL × owner upgrade dup-key × idempotency-collision | ⚠️ partial (UNIQUE chống dup row, sai error semantics) | **NET-NEW** |
| FM-11 | Migration webhook không có timestamp/nonce → replay window | P3 | ACTIVE × webhook forged/replay × webhook-replay | ⚠️ partial (HMAC ok + phase-idempotency mitigate) | overlap GAP-039 |

8 NET-NEW (FM-01..06, FM-08, FM-10) + 3 overlap/dep (FM-07 dep GAP-123, FM-09 doc-drift, FM-11 → GAP-039).

---

## 2. Ma trận 3 trục (state × actor × fault)

| State | Actor action | Fault | Failure | Hệ quả | Guard exists? (code-ref) | Finding |
|---|---|---|---|---|---|---|
| TRIAL | owner upgrade ×2 concurrent | concurrent upgrade | 2 request cùng load instance phase=NONE, cùng pass `assertCanStartMigration`, cùng INITIATE | 2 migration in-flight / 2 payment / double-charge | ❌ **không có** `@Lock(PESSIMISTIC_WRITE)`; `TrialToPaidService.loadInstance` dùng `findById` (L362-364); `InstanceRepository` 0 method có `@Lock` (verified grep) — T2P-08 vi phạm | FM-01 |
| MIGRATING | scheduler tick | retry exhausted/partial | `MigrationRetryRunner.executeMigrationInternal` `@Transactional` (L92) nhưng class `new`-instantiated (`TrialToPaidService` ctor L79) → KHÔNG phải Spring bean → annotation inert; thêm self-invocation L67 | MIGRATING-save + convert + COMPLETED-save không cùng 1 txn → invariant "status ACTIVE chỉ flip atomic với phase COMPLETED" (rules §3) vỡ | ❌ no transactional boundary on retry path | FM-02 |
| MIGRATING | scheduler tick | retry after partial flip | `resetToPaymentCapturedForRetry` (L128-139) force-reset phase=PAYMENT_CAPTURED dù attempt trước đã flip status=ACTIVE | retry gọi lại `convertTrialToSubscription` trên instance đã ACTIVE → double subscription nếu convert không idempotent | ⚠️ phụ thuộc `TrialService.convertTrialToSubscription` idempotency (chưa verify) | FM-03 |
| ACTIVE | webhook payment-reversed | reversal mid-window | `TrialToPaidService.rollback` (L237-285) set status=TRIAL nhưng KHÔNG gọi `instance.setTier(...)` để restore | `instances.tier` kẹt PREMIUM sau revert; pool-size/custom-domain/retention vẫn PREMIUM cho non-payer; `branding.refresh.required` emit với tier stale (L271-274) | ❌ SUB-21 sync-site list (`applyPendingUpgrade` + `processRenewal`) KHÔNG gồm rollback | FM-04 |
| grace→SUSPENDED | scheduler tick / owner cancel | tier desync | `suspendExpiredSubscription` (L178-214) + `suspendCancelledExpired` (L226-242) + `cancelSubscription` immediate (L311-318) set status=SUSPENDED, KHÔNG reset `instances.tier` | `DataRetentionService.getRetentionDays` (L40-47) đọc `instance.tier` → suspended PREMIUM instance hưởng retention dài hơn FREE + pool sized PREMIUM | ❌ SUB-21 không cover suspend/expire path | FM-05 |
| TRIAL (mid-migrate) | scheduler tick (8AM) | trial-expires-during-MIGRATING | `findExpiredTrials` (`InstanceRepository` L35-37): `status='TRIAL' AND trialExpiresAt<now` — KHÔNG lọc `migration_phase` | instance phase=PAYMENT_CAPTURED/MIGRATING bị `suspendExpiredTrial` → SUSPENDED giữa migration → `MigrationScheduler` vẫn pick up (phase=PAYMENT_CAPTURED) → convert chạy trên SUSPENDED instance | ❌ T2P-05 rescue-window chỉ check ở `initiateUpgrade` (`MigrationStateMachine.assertWithinRescueWindowOrStillTrial` L59-70), KHÔNG ở scheduler | FM-06 |
| mọi STATE | scheduler tick | runs-on-2-leaders | `MigrationScheduler` in-JVM `AtomicBoolean` (L40,48) chỉ chống overlap cùng JVM; javadoc tự nhận "runs on the leader container only" nhưng KHÔNG có leader-election; `TrialExpirationChecker`/`SubscriptionExpirationChecker`/`DataRetentionScheduler` cron 0 lock | ≥2 replica (Phase 2 HPA GAP-123) → double trial-suspend, double retention-purge, double migration pickup | ⚠️ partial — AtomicBoolean chỉ 1 scheduler; 3 cron scheduler 0 lock | FM-07 |
| SUSPENDED | scheduler tick (3AM) | retention-purge clock | `DataRetentionService` (L70,127) dùng `instance.getUpdatedAt()` làm "suspendedAt" | mọi update SUSPENDED row (tier-sync, admin edit, re-save) reset `daysSuspended` → đồng hồ purge restart → data giữ quá hạn / warning sai ngày | ❌ không có cột `suspendedAt` riêng | FM-08 |
| COMPLETED | webhook reversal | clock-origin drift | `isWithinReversalWindow` (`MigrationStateMachine` L72-79) tính từ `migrationCompletedAt`; T2P-04 rule ghi "24h after PAYMENT_CAPTURED" | nếu scheduler delay giữa capture→complete, cửa sổ 24h bắt từ complete (rộng hơn rule) | ⚠️ doc-vs-code drift (rules §3 invariant ghi completedAt; rule table T2P-04 ghi PAYMENT_CAPTURED) | FM-09 |
| TRIAL | owner upgrade dup-key | idempotency collision | `MigrationIdempotencyKeyService.persist` (L76-98) check-then-insert (TOCTOU); entity có `@UniqueConstraint` (L48); persist nằm trong txn của `initiateUpgrade` | concurrent same-key: loser INSERT → ConstraintViolation poisons `initiateUpgrade` txn → migration đã NONE→PAYMENT_PENDING rollback → loser nhận 500 thay 202; HOẶC dup arrive sau phase=PAYMENT_PENDING trước persist commit → `assertCanStartMigration` throw MIGRATION_IN_FLIGHT thay vì replay cached | ⚠️ UNIQUE chống dup row nhưng error semantics sai | FM-10 |
| ACTIVE | webhook forged/replay | webhook replay | `MigrationWebhookVerifier.verify` (L47-69) HMAC-SHA256 constant-time (tốt) nhưng KHÔNG có timestamp/nonce check | webhook capture hợp lệ replay vô hạn; phase-idempotency (`handlePaymentCaptured` L158-161, `handlePaymentReversed` L353-355) hấp thụ phần lớn; reversal replay sau re-upgrade có thể re-trigger rollback | ⚠️ partial — HMAC + phase-idempotency mitigate; thiếu freshness | FM-11 |

---

## 3. Findings chi tiết (NET-NEW ưu tiên)

### FM-01 (P1, NET-NEW) — T2P-08 pessimistic lock missing
**Cell:** TRIAL × owner upgrade ×2 đồng thời × concurrent-upgrade.
Rule T2P-08 mandate "Concurrent in-flight migrations per instance = 1 (pessimistic lock on instance_id during MIGRATING) — `@Lock(PESSIMISTIC_WRITE)`". Verified: `InstanceRepository` (kitehub-subscription) KHÔNG có method nào annotate `@Lock`/`PESSIMISTIC`; `TrialToPaidService.loadInstance` (L362-364) dùng `instanceRepository.findById`. `initiateUpgrade` (L92-142) `@Transactional` nhưng read không-lock → 2 request đồng thời cùng đọc phase=NONE, cùng `assertCanStartMigration` pass (`MigrationStateMachine` L46-57), cùng transition → 2 migration. Idempotency-key chỉ dedupe **cùng key** (L99-106) — 2 thiết bị / 2 key khác nhau bypass hoàn toàn.
**Hệ quả:** double migration phase machine, double payment-pending row, potential double-charge khi 2 admin confirm.
**Fix:** thêm `@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("... where id=:id") Optional<Instance> findByIdForUpdate(UUID id)` + dùng trong `loadInstance` cho mutating paths (initiateUpgrade/handlePaymentCaptured/executeMigration/rollback). Phase 1 BETA low-concurrency giảm xác suất nhưng manual-confirm path vẫn race.

### FM-02 (P1, NET-NEW) — MigrationRetryRunner @Transactional inert
**Cell:** MIGRATING × scheduler tick × retry path.
`MigrationRetryRunner` được tạo bằng `new MigrationRetryRunner(...)` trong `TrialToPaidService` constructor (L79-81) → KHÔNG phải Spring bean → KHÔNG có proxy → `@Transactional` trên `executeMigrationInternal` (L92) + `resetToPaymentCapturedForRetry` (L128) **hoàn toàn inert**. Thêm nữa `executeMigrationWithRetry` (L51) gọi `executeMigrationInternal` qua self-invocation (L67) — kể cả nếu là bean cũng bypass proxy. Hệ quả: `instanceRepository.save(MIGRATING)` (L101) → `convertTrialToSubscription` (L103, đây là @Transactional bean riêng → atomic nội bộ) → `save(COMPLETED)` (L116) chạy KHÔNG trong 1 transaction bao quanh.
**Hệ quả:** invariant rules §3 "status TRIAL→ACTIVE chỉ flip atomic với phase=COMPLETED" vỡ — nếu `save(COMPLETED)` (L116) ném sau khi convert đã commit → instance ACTIVE nhưng phase ≠ COMPLETED.
**Fix:** biến `MigrationRetryRunner` thành Spring `@Component` (inject thay vì `new`) + tách `executeMigrationInternal` sang bean khác để proxy fire, HOẶC wrap toàn bộ flip trong `TransactionTemplate.execute(...)` programmatic.

### FM-03 (P2, NET-NEW) — Retry partial-success double-convert
**Cell:** MIGRATING × scheduler tick × retry after partial flip.
`resetToPaymentCapturedForRetry` (L128-139) force-reset `migrationPhase=PAYMENT_CAPTURED` bypass state-machine guard (comment L134-135 thừa nhận). Nếu attempt trước: convert đã flip status=ACTIVE (committed) nhưng COMPLETED-transition/save ném → retry reset PAYMENT_CAPTURED → `executeMigrationInternal` gọi lại `convertTrialToSubscription(instanceId)` (L103) trên instance đã ACTIVE.
**Hệ quả:** nếu convert không idempotent trên ACTIVE → duplicate subscription row / double billing handoff.
**Fix:** trước retry kiểm tra `instance.status`; nếu đã ACTIVE → chỉ resume COMPLETED-transition (skip convert). Verify `TrialService.convertTrialToSubscription` no-op khi status đã ACTIVE.

### FM-04 (P1, NET-NEW) — Tier desync trên ROLLBACK
**Cell:** ACTIVE × webhook payment-reversed × within-24h-window.
`rollback` (L237-285): set `status=TRIAL` (L257), `subscriptionExpiresAt=null` (L258) nhưng KHÔNG `instance.setTier(trialTier)`. SUB-21 liệt kê `instances.tier` là load-bearing (pool size `MultiTenantDataSourceConfig`, custom-domain `DomainService`, retention `DataRetentionService`) và sync-site CHỈ gồm `applyPendingUpgrade` + `processRenewal`. Rollback path không restore tier. `branding.refresh.required` (L271-274) emit với `instance.getTier()` = PREMIUM stale → branding service refresh template PREMIUM cho instance đã revert. Rollback matrix (rules §6) ghi "AI budget restored to trial level" — code 0 chạm budget; downstream consume event với tier stale.
**Hệ quả:** non-paying reverted trial giữ PREMIUM pool + custom-domain eligibility + retention window dài; branding/budget desync.
**Fix:** trong rollback set `instance.setTier(FREE)` (trial entitlement) + emit event với tier đúng; thêm rollback vào SUB-21 sync-site list.

### FM-05 (P2, NET-NEW) — Tier desync trên expiry/suspend/cancel
**Cell:** grace→SUSPENDED × scheduler tick / owner cancel × tier-desync.
`SubscriptionRenewalService.suspendExpiredSubscription` (L178-214) + `suspendCancelledExpired` (L226-242) + `SubscriptionService.cancelSubscription` immediate (L311-318) đều set status=SUSPENDED, KHÔNG reset `instances.tier`. `DataRetentionService` (L40-47, L67-68, L124-125) đọc `instance.tier` cho retention window → suspended PREMIUM instance hưởng retention dài hơn FREE + connection-pool sized PREMIUM (waste).
**Hệ quả:** non-paying suspended instance giữ PREMIUM-tier retention + pool; desync không document.
**Fix:** quyết định policy — nếu suspend nên revert tier về FREE (đồng bộ SUB-21) hoặc document chủ đích "retention theo last-paid-tier" + decouple pool sizing khỏi tier cho SUSPENDED.

### FM-06 (P1, NET-NEW) — Trial expires during MIGRATING
**Cell:** TRIAL(mid-migrate) × scheduler tick (8AM) × trial-expires-during-MIGRATING.
`findExpiredTrials` (`InstanceRepository` L35-37) chỉ lọc `status='TRIAL' AND trialExpiresAt<now AND deleted=false` — KHÔNG lọc `migration_phase`. Instance đang PAYMENT_CAPTURED/MIGRATING vẫn `status=TRIAL` (flip sang ACTIVE chỉ ở COMPLETED). `TrialExpirationChecker.checkExpiredTrials` (L40-75) gọi `suspendExpiredTrial` → SUSPENDED. Sau đó `MigrationScheduler.tick` (L47-71) vẫn `findInstancesReadyForMigration` (phase=PAYMENT_CAPTURED, L341-343) → `executeMigrationWithRetry` chạy convert trên SUSPENDED instance.
**Hệ quả:** paying-converting tenant bị suspend giữa chừng; state machine conflict (SUSPENDED + MIGRATING).
**Fix:** `findExpiredTrials` thêm `AND (migration_phase IS NULL OR migration_phase = 'NONE')`; HOẶC `suspendExpiredTrial` skip khi phase ∉ {NONE}. T2P-05 rescue-window guard cần mở rộng sang scheduler boundary.

### FM-08 (P2, NET-NEW) — Retention clock từ updatedAt
**Cell:** SUSPENDED × scheduler tick (3AM) × retention-purge clock-reset.
`DataRetentionService.processRetentionWarnings` (L70) + `processExpiredRetention` (L127) dùng `instance.getUpdatedAt()` làm mốc "suspendedAt". `updatedAt` đổi theo MỌI update row (tier-sync, admin edit, bất kỳ `save`). Một SUSPENDED instance bị chạm field → `daysSuspended` reset → đồng hồ purge khởi động lại.
**Hệ quả:** data giữ quá hạn retention (PDPL determinism risk) HOẶC warning gửi sai ngày. PDPL pre-deletion notice không đáng tin cậy.
**Fix:** thêm cột `suspended_at TIMESTAMP` set một lần khi `suspend()`, đọc nó thay `updatedAt`. (Partial overlap GAP-1026 vốn cover warning exact-day cron-skip, nhưng KHÔNG cover root-cause updatedAt-as-clock.)

### FM-10 (P2, NET-NEW) — Idempotency persist TOCTOU
**Cell:** TRIAL × owner upgrade dup-key × idempotency-collision.
`MigrationIdempotencyKeyService.persist` (L76-98) check-then-insert; entity `MigrationIdempotencyKey` có `@UniqueConstraint` (L48). persist nằm trong cùng `@Transactional` của `initiateUpgrade` (L137-139). Concurrent same-key: loser INSERT → DataIntegrityViolation poisons `initiateUpgrade` txn → migration đã transition NONE→PAYMENT_PENDING rollback → loser nhận 500. Ngoài ra dup arrive SAU khi phase=PAYMENT_PENDING nhưng TRƯỚC persist commit: `findExisting` (L101) empty → `assertCanStartMigration` (L110) throw MIGRATION_IN_FLIGHT thay vì replay cached 202 (vi phạm api-contract "duplicate within 10 min returns original 202").
**Fix:** catch DataIntegrityViolation trong persist → re-read cached row return; HOẶC dùng `INSERT ... ON CONFLICT DO NOTHING` + re-query; move idempotency persist trước state transition.

### FM-07 (P2, NET-NEW, dep GAP-123) — Scheduler multi-replica
**Cell:** mọi STATE × scheduler tick × runs-on-2-leaders.
`MigrationScheduler` (L40,48) chỉ có in-JVM `AtomicBoolean`; javadoc L24-27 tự nhận thiếu cluster-wide lock ("follow-up gap"). 3 cron scheduler (`TrialExpirationChecker` L40, `SubscriptionExpirationChecker` L46/L88, `DataRetentionScheduler` L28) hoàn toàn 0 lock. Phase 1 single-replica → latent. Phase 2 HPA (GAP-123) → mọi replica fire → double trial-suspend / double retention-purge / double migration pickup.
**Fix:** ShedLock (DB-backed) hoặc leader-election trước khi bật HPA. File gap gắn dependency GAP-123/GAP-479 (EKS).

### FM-09 (P3, NET-NEW) — Reversal-window clock origin drift
`MigrationStateMachine.isWithinReversalWindow` (L72-79) tính deadline từ `migrationCompletedAt`. Rule table T2P-04 ghi "24 hours after PAYMENT_CAPTURED"; rules §3 invariant lại ghi reversal đo từ completedAt. Doc-vs-code inconsistency. Phase 4a sync-MVP capture→complete gần như tức thì nên drift nhỏ; async worker (delay ≤5s/tick) vẫn rộng hơn rule wording.
**Fix:** reconcile rule T2P-04 wording ↔ code (chọn PAYMENT_CAPTURED hoặc COMPLETED làm canonical), cập nhật rules.md hoặc code cho khớp.

### FM-11 (P3, overlap GAP-039) — Webhook replay
`MigrationWebhookVerifier` (L47-69) HMAC-SHA256 constant-time đúng chuẩn nhưng raw-body, không timestamp/nonce → captured valid webhook replay vô hạn. Phase-idempotency (`handlePaymentCaptured` L158, `handlePaymentReversed` L353) hấp thụ phần lớn. Reversal replay sau khi tenant re-upgrade (ACTIVE lại) có thể re-trigger rollback. Maps to existing **GAP-039** (Webhook Reliability — Retry/Idempotency/Event Versioning, Phase 2). Khuyến nghị: thêm timestamp tolerance + nonce store khi GAP-039 thực thi.

---

## 4. Dedup vs inside-out 6 clusters + existing gaps

| Inside-out cluster | Coverage | Overlap với findings |
|---|---|---|
| A) GAP-1079/1080 | GET active 400-vs-404 + POST create dup PENDING | KHÔNG trùng FM — FM-10 là **migration** idempotency-key race (khác `POST /subscriptions` create idempotency của 1080) |
| B) GAP-1016/1017/1018 | manual renewal gate / cancel→suspend / renewal hardening | FM-05 (tier desync on suspend) liên quan 1017 nhưng 1017 chỉ cover status-propagation, KHÔNG cover tier-desync → FM-05 NET-NEW |
| C) GAP-192/1095/1096 | migration design / convert-no-tier-sync / activate-dead-code | FM-04 (rollback tier desync) + FM-01/02/03 (lock/retry) KHÔNG nằm trong 192/1095/1096 — đó là tier-sync trên convert/activate, không phải rollback/lock/retry-atomicity |
| D) GAP-1026/1024 | offboarding retention robustness / domain verification SM | FM-08 partial overlap GAP-1026 (warning exact-day) nhưng root-cause `updatedAt`-as-clock là NET-NEW; FM-06/07 không trùng |
| E) GAP-1002 | grading_scales NULL-default + new-tenant seed | KHÔNG trùng (KC domain) |
| F) GAP-1064/1044 | H2 IT boot fail / stale IT auth-migrate | Test-infra, KHÔNG trùng FM business-logic |

**Verified absent trong gap-status.csv 2026-06-13:** FM-01 (pessimistic lock), FM-02 (retry @Transactional inert), FM-04 (rollback tier desync), FM-05 (suspend tier desync), FM-06 (trial-expires-mid-MIGRATING `findExpiredTrials` filter), FM-08 (retention `updatedAt` clock) — genuinely new. FM-07 dep GAP-123/479. FM-09 doc-drift. FM-10 distinct từ GAP-1080. FM-11 → GAP-039.

---

## 5. Pre-Flight checklist (`simulation-gap-finder.md`)

- [x] Edge/Error stage simulated (≥2 cells/persona): concurrent, retry-partial, mid-migration suspend, multi-replica
- [x] Termination stage (rollback/cancel/expire/purge) simulated — FM-04/05/08
- [x] Concurrency + malice (webhook replay FM-11, concurrent upgrade FM-01)
- [x] Each finding cross-checked against `gap-status.csv` (§4) — no duplicate filed
- [x] Code-grounded: mọi guard verdict cite file:line
