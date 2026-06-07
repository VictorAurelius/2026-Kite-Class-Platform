---
wave: p0-local-1
tag_primary: p0-local
tags_secondary: [db-integrity, uuid-sweep, rls]
date: 2026-06-07
status: complete
audience: dev
---

# Wave p0-local-1 — DB integrity sweep (actor UUID + RLS residual)

**Mục tiêu:** Đóng phần code-work local-verifiable còn lại trong cluster P0 DB-integrity sau khi state-check loại 952/223 (AWS/infra-gated) + xác nhận 885 gần xong.

## 1. Brainstorm (5-10 min)

**Bối cảnh:** User yêu cầu "wave P0-local". State-check (`audit-to-gap-pipeline.md` §2.6 + §2.6.1 Bucket-Completion) + survey mở rộng P0 PARTIAL backend cho thấy pool local-verifiable **mỏng** — đa số PARTIAL P0 đã ship code, residual bị chặn bởi GAP-612 (AWS restore) / vendor key (SePay) / email warm-up.

**Chỉ GAP-877 có code-work bucket-sized thật.** GAP-885 còn residual nhỏ. 952/223/975/976/610/793/502/117/530 đã loại (xem §4).

**Risk/edge:**
- Migration version collision: nhiều bucket cùng đụng 1 `db/migration` folder → Flyway version đụng. **Mitigation:** chia theo service boundary (KC vs KH), 1 migration/service/agent.
- BIGINT→UUID conversion: cột actor đang `BIGINT` chứa giá trị cũ (nếu có data) sẽ fail cast. Phase 1 BETA pre-launch, không data thật → an toàn; migration dùng DO-block `USING NULL::uuid` hoặc drop+recreate cho cột actor (audit-only, nullable).
- JPA entity field type phải đồng bộ migration (Long→UUID) nếu không runtime parse fail — đây chính là lớp lỗi GAP-877 mô tả.

**Outside-in:** SKIP per `outside-in-coverage-trigger.md` §4 — wave 100% internal scope (DB schema integrity, no user-facing change).

## 2. Task Breakdown

| Bucket | Gap | Service | Migration | Effort |
|---|---|---|---|---|
| A | GAP-877 (KC) | kiteclass-core | V94 | L |
| B | GAP-877 (KH) + GAP-885 | kitehub-subscription (+platform) | V66 | M |

## 3. Scope

### Bucket A — KC actor UUID sweep (kiteclass-core, V94)

- **Gap:** GAP-877 (KC portion) — actor user-id columns BIGINT → UUID
- **Cột (verified BIGINT 2026-06-07):** `attendance.marked_by`, `grades.finalized_by`, `submissions.graded_by`, `subject_grades.reviewed_by`, `attendance_period.recorded_by`, `payments.received_by`/`payer_id`, `payment_records.recorded_by`, `payment_idempotency_keys.user_id`, `user_roles.assigned_by`(VARCHAR), `vettings.decided_by_user_id`, `reward_redemptions.approved_by`, `audit_log.actor_user_id`, `moderation_queue.assigned_reviewer_id`, `dmca_takedown_requests.reviewer_user_id`, `deletion_requests.user_id`, `incidents.reporter_user_id`/`assigned_officer_user_id`, `child_protection_audit_log.actor_id`, `rebrand_approvals.initiator_user_id`/`approver_user_id`
- **Deliverable:** V94 migration (DO-block dynamic, audit/actor cột → UUID, nullable, `USING NULL::uuid` an toàn pre-launch) + JPA entity `@Column` Long→UUID đồng bộ + IT verify write UUID không parse-fail. **Ưu tiên compliance cluster** (child-protection actor + GDPR Art 17 audit) per gap.
- **AC:** migration V94 apply clean trên Flyway (`./mvnw -pl kiteclass-core verify`); entity update đồng bộ; IT ghi UUID vào ≥3 cột đã convert PASS.

### Bucket B — KH actor + RLS residual (kitehub-subscription V66, +platform)

- **Gap:** GAP-877 (KH BaseEntity) + GAP-885 (oauth_attempts RLS residual)
- **GAP-877 KH:** `BaseEntity.created_by/updated_by VARCHAR(100)` cross-DB vs UUID drift → **quyết định**: keep VARCHAR + document normalization (cross-DB string-ID acceptable) HOẶC sweep. Document decision trong gap §Log.
- **GAP-885:** `oauth_attempts` (kitehub-subscription, có `tenant_id`) là bảng DUY NHẤT còn thiếu RLS (V78 KC + V58 KH đã sweep 7/8). Extend RLS migration V66 cho oauth_attempts (DO-block + `tenant_isolation` policy, mirror V58 pattern).
- **AC:** V66 enable RLS + policy cho oauth_attempts; IT verify tenant isolation; BaseEntity decision documented.

### Out-of-scope (đã loại qua state-check — không phải bucket)
- GAP-952 (CloudWatch live-apply + fault-injection) → AWS-gated GAP-612
- GAP-223 (Gemma migration) → Ollama GPU infra-gated
- GAP-975/976 SePay live verify → vendor key trống local
- GAP-610/793 live verify → AWS-gated (code+IT đã DONE)
- GAP-502/117/530 → deploy/DR/Resend-warmup gated

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6 + §2.6.1)

| Symbol / gap | Grep/verify command | Verdict (§2.6.1) |
|---|---|---|
| GAP-877 actor cols BIGINT | `grep -rhiE "marked_by\|graded_by\|..." KC/db/migration/V*.sql` → 10/10 vẫn BIGINT; V79 còn *thêm* `reviewed_by/assigned_by/approved_by BIGINT` sau V73 | 🔨 **Delta REAL** — code work tồn tại |
| GAP-885 KC trio | V78__rls_sweep.sql DO-block đã ENABLE RLS + CREATE POLICY cho `landing_pages/idempotency_keys/payment_records/payment_idempotency_keys` | ⚠️ **Already-shipped** — KC done |
| GAP-885 KH 4/5 | V58__rls_sweep_kh.sql covers `onboarding_progress/staff_invitations/staff_invitation_audit_log/impersonation_audit_log` | ⚠️ **Already-shipped** |
| GAP-885 oauth_attempts | NOT in V58 KH sweep; bảng ở kitehub-subscription, có tenant_id | 🔨 **Delta** — residual thật (1 bảng) |
| GAP-952 residual | gap §Current State: app-level (cron+metric+13 tests) shipped Wave provisioning-1; residual = CloudWatch live-apply + fault-injection | ⚠️ **Already-shipped** local; residual AWS-gated → DROP |
| GAP-223 residual | Sub-PR 223.1 governance scaffold shipped 2026-04-26; residual = GAP-006 Gemma (Ollama GPU) | ⚠️ **Already-shipped** gov; residual infra-gated → DROP |
| GAP-975/976 | tất cả AC `[x]` (txnRef/beta-amount/Apikey/idempotency/tests); residual = SePay live | ⚠️ code DONE; vendor-gated → DROP |
| GAP-610/793 | code+IT+88 tests `[x]`; residual = live verify | ⚠️ code DONE; AWS-gated → DROP |
| KC next migration | `ls KC/db/migration` → latest V93 | 🆕 V94 to-be-created (Bucket A owns) |
| KH next migration | `ls kitehub-subscription/db/migration` → latest V65 | 🆕 V66 to-be-created (Bucket B owns) |

## 5. Verification Gates (per bucket)

- Bucket A: `cd kiteclass/kiteclass-core && ./mvnw verify -P strict-warnings` PASS (Flyway V94 apply + ≥3 actor-write IT GREEN trên Testcontainers Postgres).
- Bucket B: `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` PASS (V66 RLS apply + oauth_attempts tenant-isolation IT GREEN); BaseEntity decision trong gap §Log.
- Production-equivalent: Flyway replay trên Testcontainers (KHÔNG ddl-auto) per `project_kiteclass_core_it_ddl_auto_masks_migration_drift` — bắt buộc IT chạy trên Flyway schema thật.

## 6. Agent Spawn Pattern

- 2 agent Opus (`model: opus` per `agent-model-opus-default.md`), worktree-isolated, parallel (service boundary disjoint → no migration collision).
- Bucket A → kiteclass-core; Bucket B → kitehub-subscription/platform.
- `run_in_background: true` cho heavy `mvnw verify` per `docs-only-pr-no-block-wait.md` §5.5.

## 7. Closure Protocol

- Mỗi bucket → 1 PR (squash). CI gate + local verify per `admin-merge-discipline.md`.
- GAP-877 → DONE khi KC+KH cả 2 bucket merge + IT verify; GAP-885 → DONE khi oauth_attempts RLS + IT PASS.
- Sync 4 target per `post-merge-sync-completeness.md`: gap-status.csv + ROADMAP + wave-history.jsonl + MEMORY.md.
- Per `feature-ship-runtime-walk-mandate.md`: DB-integrity = internal, không user-facing flow → IT verify đủ (không cần RST walk).

## 8. Log

- **2026-06-07 (draft):** Wave plan tạo sau 2-vòng state-check. Vòng 1: cluster đề xuất ban đầu (877/885/952/223) — §2.6.1 loại 952 (AWS-gated) + 223 (infra-gated). Vòng 2: survey mở rộng P0 PARTIAL backend (610/793/975/976/502/117/530) — tất cả code đã ship, residual AWS/vendor-gated. Kết luận: pool local-verifiable mỏng; anchor = GAP-877 (KC+KH actor sweep) + GAP-885 oauth_attempts residual. 2 bucket parallel-safe theo service boundary.
