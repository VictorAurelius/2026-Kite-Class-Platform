---
title: Wave g2-blockers-1 — clear OPEN P1 flow-blockers before G2 human batch
wave: 1
tag_primary: g2-blockers
tags_secondary: [flow-campaign, p1, beta-prep]
counter: 1
date_launch: 2026-06-07
status: draft
gaps: [GAP-1028, GAP-1029, GAP-1020, GAP-1021, GAP-1016, GAP-1017, GAP-1004, GAP-1005, GAP-1000, GAP-1002]
---

# Wave g2-blockers-1 — clear OPEN P1 flow-blockers before G2 human batch

**Goal:** Đóng 10 OPEN P1 gap đang chặn G2 human-walk trên 5 flow (KH-9 / KH-6 / KH-5 / KC-7 / KC-6) — không gap nào cần SePay key, không gap nào AWS-gated.
**Trigger:** Flow Verification Campaign đang ở reorder G1→G3→G2 ([[project_flow_campaign_g1_first_then_g2]]). 17/22 flow đã `🔄 walk-pass-pending-human` với G3 ✅, nhưng mỗi flow còn OPEN P1 bug mà human G2 sẽ vấp. User directive 2026-06-07: pick gaps không cần SePay, tạo wave, ưu tiên gap chặn G2.
**Estimated wall-clock:** ~5-7h agent work (5 bucket parallel), longest-bucket ~90min (KH-6 branding job/SSE).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phục vụ Flow Verification Campaign §4 — 5 flow đã pass G1+G3, OPEN P1 residual là "G2 blocker" cuối. Personas: Owner (KH-5 lifecycle, KH-6 branding, KC-7 invoice, KC-6 grade), Platform Admin (KH-9 console).

**Inside-out source pull (per `inside-out-completeness-trigger.md`):**
- **ROADMAP / campaign §4:** mỗi flow row liệt kê OPEN P1 blocker — đây là canonical queue cho wave này.
- **gap-status.csv phase-1-beta:** 23 PARTIAL + 5 OPEN P0; OPEN P1 cluster (1000/1002/1004/1005/1016/1017/1020/1021/1028/1029) = scope wave này.
- **Loại trừ rõ ràng:** SePay GAP-975/976 (cần `SEPAY_API_KEY`), AWS-gated GAP-952/793/502/608/533/567/566/572/117/756 (cần `start-stack.sh`), KC-1 provisioning cluster GAP-945/946/948 (provisionInfrastructure real-impl = large kiteclass-core task, riêng wave), GAP-982 academic-year orphan (user re-scope own wave), GAP-1024 KH-7 domain (verification state machine cần cert/Cloudflare — partial AWS-gated).

**Q2 (trade-offs):**
- *Tại sao 5 bucket, không gộp 10 gap thành ít bucket hơn?* — 5 flow nằm trên 3 service (kitehub-subscription, kitehub-branding, kiteclass-core). Gộp theo flow giữ disjoint + 1 agent = 1 flow domain = re-walk G3 dễ.
- *Tại sao defer KC-1 provisioning?* — GAP-946 provisionInfrastructure stub→real = large standalone task, không phải walk-blocker dạng P1 nhỏ; xứng đáng wave riêng.
- *Tại sao defer KH-7/KH-8 còn-lại?* — GAP-1024 verification state machine ceiling local (cần cert/gateway/Cloudflare = AWS); GAP-1026 (KH-8) fully-local nhưng để wave-2 giữ wave này ≤5 bucket.
- *Rejected: 1 mega-bucket per service* — vi phạm "1 bucket = 1 disjoint scope" + khó re-walk G3 độc lập.

**Q3 (risks):**
- *Same-service parallel (A+C cùng kitehub-subscription; D+E cùng kiteclass-core):* file-disjoint (audit/ vs service/; payment/ vs grade/) → worktree isolation an toàn git; rebuild churn chấp nhận (merge sequential).
- *GAP-1028 audit-log 500 có IT PASS nhưng live 500* — agent PHẢI investigate version/sort/count discrepancy TRƯỚC khi sửa Specification (per gap note); empirical state-check first per `release-fix-retry-budget.md` §3.5.
- *Post-fix re-walk:* mỗi bucket P1 từ audit/RST walk → MUST re-walk affected scope qua gateway :9000 trước DONE flip (per `pre-handoff-self-test-completeness.md` §3). G3-parity re-verify, không chỉ unit test.
- *Cross-layer (KH-6 GAP-1021 SSE/FE):* scope bucket B ở BE (job approve endpoint + SSE token-in-query); FE EventSource wiring = follow-up note, KHÔNG trong bucket → tránh cross-layer foundation overhead.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Flow | Owner | Effort | Disjoint? |
|--------|--------|------|-------|--------|-----------|
| A | GAP-1028 + GAP-1029 | KH-9 admin console | bg-agent (Opus) | ~75min | ✅ `kitehub-subscription/.../audit/` + admin controllers |
| B | GAP-1020 + GAP-1021 | KH-6 AI branding | bg-agent (Opus) | ~90min | ✅ `kitehub-branding/` |
| C | GAP-1016 + GAP-1017 | KH-5 subscription lifecycle | bg-agent (Opus) | ~60min | ✅ `kitehub-subscription/.../service/Subscription*` |
| D | GAP-1004 + GAP-1005 | KC-7 invoice/payment | bg-agent (Opus) | ~60min | ✅ `kiteclass-core/.../module/payment` + `/module/invoice` |
| E | GAP-1000 + GAP-1002 | KC-6 grade | bg-agent (Opus) | ~60min | ✅ `kiteclass-core/.../module/grade` |

**Disjoint check:** A↔C cùng service kitehub-subscription nhưng package khác (`audit/` + admin controllers vs `service/SubscriptionService` + `SubscriptionRenewalService`) — 0 file chung. D↔E cùng service kiteclass-core nhưng module khác (`payment/record` + `invoice` vs `grade`) — 0 file chung. B độc lập service kitehub-branding. Worktree isolation per agent.

---

## 3. Scope (compact schema)

**Stake tier:** MEDIUM-HIGH (financial integrity KH-5/KC-7 + admin A01 KH-9 + RLS/IDOR KH-6) → model: **Opus 4.7 full** (per `agent-model-opus-default.md`).
**Cross-layer?** NO (mọi bucket scope BE-only; KH-6 SSE/FE wiring deferred follow-up) → skip Bucket 0 Foundation.

> Gap referencing per `gap-architecture-v2.md` — canonical id từ `gap-status.csv`. Query `bash scripts/query-gaps.sh <id>` xác nhận status/priority trước reference.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** KH-9 admin audit | GAP-1028, GAP-1029 | 🟠 P1 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/audit/**` + admin controllers + migration | parallel |
| 2 | **B** KH-6 branding | GAP-1020, GAP-1021 | 🟠 P1 | `kitehub/kitehub-branding/src/main/**` | parallel |
| 3 | **C** KH-5 lifecycle | GAP-1016, GAP-1017 | 🟠 P1 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/Subscription*` + controller | parallel |
| 4 | **D** KC-7 invoice | GAP-1004, GAP-1005 | 🟠 P1 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/**` + `/module/invoice/controller/**` | parallel |
| 5 | **E** KC-6 grade | GAP-1000, GAP-1002 | 🟠 P1 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/grade/**` | parallel |

### Bucket A — KH-9 admin audit-log fix + completeness
- Files: `kitehub-subscription/.../audit/AdminAuditLogRepository.java` + `AdminAuditAspect.java` + admin instance/beta controllers + new Flyway migration (table reconcile).
- **GAP-1028:** GET `/api/v1/admin/audit-logs` default load → 500 `could not determine data type of parameter $5`. **Investigate FIRST** (per gap note): IT `AdminAuditLogSearchPostgresIT:77` all-null PASS on Testcontainers PG16 nhưng live 500 — kiểm tra version/sort/count path trước khi sửa `search()` Specification null-LocalDateTime binding.
- **GAP-1029:** (1) suspend/activate thiếu `@Auditable` → no `admin_audit_log` row (A09). (2) DB drift: `admin_audit_log` (V36 singular, entity dùng) + `admin_audit_logs` (V50 plural, immutability/RLS target) → reconcile canonical table.
- Tests: IT search default-load + suspend/activate audit-row assertion.
- Acceptance: audit-log default GET 200 + suspend/activate ghi audit row + 1 canonical table.

### Bucket B — KH-6 branding RLS GUC + job approve/apply + SSE
- Files: `kitehub-branding/.../service/BrandingJobService.java` + `BrandingJobV1Controller.java` + RLS GUC filter + SSE auth.
- **GAP-1020:** (1) `branding_jobs` RLS bật nhưng service không set `app.current_tenant_id` GUC → RLS giả. (2) `X-Subscription-Tier` client-sent → resolve tier server-side, không trust header.
- **GAP-1021:** (1) job generate assets nhưng thiếu endpoint approve/apply persist thành instance active theme → wizard dead-end; thêm job approve endpoint. (2) SSE preview/deploy header-auth không tương thích EventSource → 401; SSE token-in-query.
- Tests: RLS isolation IT (set GUC) + tier server-resolve unit + job approve persist IT.
- Acceptance: branding job approve→apply persists theme + RLS thật + tier server-side. **FE EventSource wiring = follow-up note (không trong bucket).**

### Bucket C — KH-5 manual renewal payment + cancel suspend
- Files: `kitehub-subscription/.../service/SubscriptionService.java` + `SubscriptionRenewalService.java` + controller.
- **GAP-1016:** POST `/renew` (manualRenewal) +1mo + reactivate instance KHÔNG tạo payment row → revenue leak + bypass VietQR gate. `processRenewal` tạo payment, `manualRenewal` không. Fix = tạo payment record trong manualRenewal.
- **GAP-1017:** DELETE sub → status CANCELLED nhưng instance vẫn ACTIVE; `cancelSubscription` không chạm Instance, scheduler bỏ qua CANCELLED → Owner huỷ vẫn dùng vô hạn. Fix = suspend instance khi cancel.
- Tests: unit renewal-creates-payment + cancel-suspends-instance.
- Acceptance: manualRenewal tạo payment + cancel → instance SUSPENDED.

### Bucket D — KC-7 invoice overpayment clamp + InvoiceController authz
- Files: `kiteclass-core/.../module/payment/record/{controller,service/impl,dto}/**` + `module/invoice/controller/InvoiceController.java`.
- **GAP-1004:** record 4M trên invoice 3.5M → 201 + `balance_due -500000` + status PAID. Thiếu upper-bound validation `RecordPaymentRequest`/`PaymentRecordServiceImpl`. Idempotency không enforce DB-side (2 calls same `Idempotency-Key` → 2 rows). Fix = clamp/reject overpayment + DB-side idempotency.
- **GAP-1005:** `InvoiceController` GET/mark-paid/cancel/adjustments có 0 `@PreAuthorize` → 200 không cần role (RLS giữ tenant scope nhưng no role gate trên financial mutations). Fix = `@PreAuthorize` role gate.
- Tests: overpayment reject IT + idempotency dup-key IT + InvoiceController 403 negative-authz IT.
- Acceptance: overpayment 400/clamp + idempotent payment + financial mutations role-gated.

### Bucket E — KC-6 grade finalize teacherId + grading-scale provisioning
- Files: `kiteclass-core/.../module/grade/{controller,service,repository}/**` + `common/security/AuthorizationBean.java` + grading-scale provisioning hook.
- **GAP-1000:** finalize teacherId self-asserted; `GAP-999` `@PreAuthorize` cover cross-tenant/non-teacher nhưng teacherId-from-JWT + ADMIN bypass = follow-up. Fix = teacherId từ JWT + ADMIN bypass path.
- **GAP-1002:** `instance_id NOT NULL` + tenantFilter/RLS kill IS-NULL default grading_scale; V88 backfill existing tenants per-tenant nhưng new-tenant provisioning hook thiếu. Fix = provisioning hook seed grading_scale per-tenant.
- Tests: finalize teacherId-from-JWT unit + ADMIN bypass + new-tenant grading-scale seed IT.
- Acceptance: finalize dùng JWT teacherId + ADMIN bypass + new tenant có grading_scale.

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `AdminAuditLogRepository` | Java repo | `grep -rln AdminAuditLogRepository kitehub/kitehub-subscription/src/main/java` | `.../audit/AdminAuditLogRepository.java` | ✅ exists |
| `AdminAuditAspect` / `@Auditable` | Java aspect | `grep -rln "AdminAuditAspect\|@Auditable" .../subscription/src/main/java` | `.../audit/AdminAuditAspect.java` | ✅ exists |
| `admin_audit_log` + `admin_audit_logs` | Migrations (V36/V50 drift) | per GAP-1029 note (V36 singular + V50 plural) | documented in gap | ✅ exists (drift to reconcile) |
| `SubscriptionService` / `manualRenewal` / `cancelSubscription` | Java service | `grep -rln "manualRenewal\|cancelSubscription" .../subscription/src/main/java` | `SubscriptionService.java` + `SubscriptionRenewalService.java` | ✅ exists |
| `BrandingJobService` / `BrandingJobV1Controller` | Java | `grep -rln "class BrandingJob" kitehub/kitehub-branding/src/main` | `.../service/BrandingJobService.java` + `wizard/BrandingJobV1Controller.java` | ✅ exists |
| `branding_jobs` RLS GUC `app.current_tenant_id` | Migration/config | per GAP-1020 note (RLS enabled, GUC unset) | documented in gap | ✅ exists (GUC wiring missing — bucket B) |
| `PaymentRecordServiceImpl` / `RecordPaymentRequest` | Java | `grep -rln PaymentRecordServiceImpl kiteclass/kiteclass-core/src/main/java` | `.../module/payment/record/service/impl/...` | ✅ exists |
| `InvoiceController` | Java controller | `grep -rln "class InvoiceController" kiteclass-core/.../java` | `.../module/invoice/controller/InvoiceController.java` | ✅ exists |
| `GradeController.finalizeGrade` | Java endpoint | `grep -n finalize .../grade/controller/GradeController.java` | `:175 @PostMapping("/{id}/finalize")` | ✅ exists |
| `GradingScaleRepository` | Java repo | `grep -rln GradingScaleRepository kiteclass-core/.../java` | `.../module/grade/repository/GradingScaleRepository.java` | ✅ exists |

### 4.1 Bucket-Completion Check (per `audit-to-gap-pipeline.md` §2.6.1)

| Bucket | Gap | completion_pct (CSV) | Residual | Verdict |
|--------|-----|:--------------------:|----------|---------|
| A | GAP-1028 | 0 | full fix (investigate-then-fix audit-log 500) | 🆕 Greenfield-fix |
| A | GAP-1029 | 0 | @Auditable suspend/activate + table reconcile | 🆕 Greenfield-fix |
| B | GAP-1020 | 0 | GUC wiring + tier server-resolve | 🆕 Greenfield-fix |
| B | GAP-1021 | 0 | job approve endpoint + SSE token-in-query (BE) | 🆕 Greenfield-fix |
| C | GAP-1016 | 0 | payment row in manualRenewal | 🆕 Greenfield-fix |
| C | GAP-1017 | 0 | suspend instance on cancel | 🆕 Greenfield-fix |
| D | GAP-1004 | 0 | overpayment clamp + DB idempotency | 🆕 Greenfield-fix |
| D | GAP-1005 | 0 | @PreAuthorize InvoiceController | 🆕 Greenfield-fix |
| E | GAP-1000 | 0 | teacherId-from-JWT + ADMIN bypass | 🆕 Greenfield-fix |
| E | GAP-1002 | 0 | grading_scale provisioning hook | 🆕 Greenfield-fix |

Tất cả 10 gap completion_pct=0 (OPEN) — không có ⚠️ Already-shipped. Expected P0/P1 outcome: 10 P1 → DONE (sau post-fix re-walk G3-parity). Phase 1 BETA P0 count KHÔNG đổi (đây là P1 cluster).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate | Post-fix re-walk (per `pre-handoff-self-test-completeness.md` §3) |
|--------|---------------------|---------|------------------------------------------------------------------|
| A | `./mvnw -pl kitehub-subscription verify -Dcheckstyle.skip=true` | kitehub-ci | Re-walk KH-9 audit-log GET + suspend/activate via gateway :9000 |
| B | `./mvnw -pl kitehub-branding verify -Dcheckstyle.skip=true` | kitehub-ci | Re-walk KH-6 job approve→apply + RLS isolation via :9000 |
| C | `./mvnw -pl kitehub-subscription verify -Dcheckstyle.skip=true` | kitehub-ci | Re-walk KH-5 renew→payment + cancel→suspend via :9000 |
| D | `./mvnw -pl kiteclass-core verify -Dcheckstyle.skip=true` | core-ci | Re-walk KC-7 overpayment reject + InvoiceController 403 via :9000 |
| E | `./mvnw -pl kiteclass-core verify -Dcheckstyle.skip=true` | core-ci | Re-walk KC-6 finalize JWT-teacherId + new-tenant grading-scale via :9000 |

> Heavy verify scripts (`./mvnw verify`) chạy `run_in_background` per `docs-only-pr-no-block-wait.md` §5.5.
> DONE flip CHỈ sau re-walk PASS (không chỉ unit test) — `feature-ship-runtime-walk-mandate.md` §1 + `pre-handoff-self-test-completeness.md` §3.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `agent-model-opus-default.md`:
- 5 buckets spawn `run_in_background: true` + `model: "opus"` + `isolation: worktree`
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge sequential sau khi tất cả background complete; same-service pairs (A+C, D+E) merge tuần tự để tránh rebuild race
- Mỗi bucket: catalog-then-batch-fix per `feature-ship-runtime-walk-mandate.md` §3.4

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-wave-cleanup.md`:
- Mỗi bucket PR update GAP file Log + status (DONE sau re-walk PASS; PARTIAL nếu re-walk lòi residual + follow-up gap)
- **Scope-Completeness Reconciliation table** trong closure PR (10 gap × verdict ✅/🟡/❌)
- ROADMAP §🎯 + campaign §4 row update (flow OPEN P1 → resolved)
- wave-history.jsonl append (tag_primary=g2-blockers, counter=1)
- `bash scripts/prune-merged-worktrees.sh --yes` trước closure PR
- `## Release Plan Progress` section trong closure PR

---

## 8. Log

- **2026-06-07** (draft): Plan created. User directive "pick gaps không cần SePay, tạo wave, ưu tiên gap chặn G2". 10 OPEN P1 across 5 flow (KH-9/KH-6/KH-5/KC-7/KC-6), tất cả G1+G3 ✅ pending G2 human. Loại trừ SePay (975/976) + AWS-gated cluster + KC-1 provisioning (riêng wave) + GAP-1024 (cert/Cloudflare gated) + GAP-982 (user re-scope). 5 bucket disjoint, all symbols state-checked ✅ exists.
