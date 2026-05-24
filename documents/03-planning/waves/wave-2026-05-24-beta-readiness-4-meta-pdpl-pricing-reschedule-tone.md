---
title: Wave beta-readiness-4 — META env-coverage + PDPL consent API + Pricing PER_HOUR + Reschedule + Email tone cross-cut
wave: 4
waves: [beta-readiness-4]
tag_primary: beta-readiness
tags_secondary: [meta-env-coverage, pdpl-consent-api, pricing-per-hour, reschedule-class, email-tone-matrix, phase-1-beta]
counter: 4
created: 2026-05-24
date_launch: 2026-05-24
status: draft
audience: dev
gaps:
  - GAP-508
  - GAP-353b
  - GAP-292
  - GAP-292b
  - GAP-291
  - GAP-NEW-email-tone-matrix
  - ADR-027
  - ADR-028
  - ADR-029
  - ADR-030
---

# Wave beta-readiness-4 — META env-coverage + PDPL consent API + Pricing PER_HOUR + Reschedule + Email tone cross-cut

**Mục tiêu:** Ship 5 buckets đóng META P0 (GAP-508 Phase 2 + Phase 3) + Compliance P1 (GAP-353b consent API + hash chain) + 2 Business-Logic P0 greenfield (GAP-292 pricing + GAP-292b payment recording paired; GAP-291 reschedule + email fallback) + 1 META cross-cut force-multiplier (email tone matrix Mustache helper) cho Phase 1 BETA exit gate ≥ 80.

**Khởi sự:** Wave beta-readiness-2 ship 4/4 (PR #1767/1768/1769/1771); wave beta-readiness-3 đã claim counter 3 (idempotency-completion-test-flake-email-pipeline scope, shipped on main). 3 outside-in agents 2026-05-24 (persona simulation + VN edu benchmark + failure-mode matrix) surface 7 P0 + 10 P1 cells cross-bucket. User 3-Q decisions chốt: (a) PDPL deadline giữ 2026-07-01 per CLAUDE.md, (b) Pricing taxonomy = PER_HOUR primary, (c) Bucket C ghép GAP-292b paired.

**Thời gian ước tính:** ~14h (2-3 phiên) — 5 bucket scope, Bucket A sequence trước, B/C/D parallel, E cross-cut sau khi A code lands.

**Tag scheme:** per `.claude/rules/wave-tag-numbering-convention.md` §2 — `beta-readiness-4` counter 4, descriptor `meta-pdpl-pricing-reschedule-tone`.

---

## 1. Brainstorm (5-10 phút)

**Q1 (đối tượng phục vụ — inside-out + outside-in):**

Inside-out (dev/user-flagged gaps đã có):
- 4 P0/P1 gaps user picked Option C: GAP-508 META + GAP-353b Compliance + GAP-292/292b Business + GAP-291 Business
- Personas: P1 Solo Teacher Vy (pricing PER_HOUR + payment recording cash), P2 Center Owner Hằng (consent banner formal greeting + reschedule lớp), Parent Mai (reschedule email fallback "Kính gửi quý phụ huynh"), Platform Admin (env-coverage CI gate FP elimination)

Outside-in (3 audit agents):
- **Persona simulation** (4 personas × 4 gaps = 16 cells): 3 P0 + 5 P1 + 3 cross-cut patterns — surfaced GAP-292b payment recording missing + email fallback gap + hash chain immutability
- **VN edu SaaS benchmark** (Apollo/ILA/MISA/Cookiebot/Resend benchmark): 4 P0 + 4 P1 + 4 ADR — re-scope pricing taxonomy PER_HOUR primary + Cal.com reschedule pattern alignment + Resend VN deliverability risk
- **Failure-mode matrix** (10 classes × 8 triggers × 6 personas, picked 22 cells): 7 P0 + 10 P1 + 5 P2 — Migration V67 race + consent_record FK risk + cross-bucket business semantic gaps

Cross-cut: 4 buckets B/C/D/E đụng email templates → cần shared persona tone matrix Mustache helper (META force-multiplier per `meta-gap-priority.md` §3)

**Q2 (giải pháp đã xét và loại):**
- ❌ Ship 4 buckets không có META cross-cut (Bucket E): 3 email templates ship riêng → tone mismatch persona → Vy nhận formal greeting "Kính gửi quý" → ngược target audience. Force-multiplier rationale: 1 shared helper → 3 templates auto-comply
- ❌ Bucket B + C + D parallel ngay từ đầu không sequence Bucket A: Bucket A ship RESEND_API_KEY IaC + CI gate; Bucket D email fallback (reschedule) phụ thuộc Resend code path ready. Sequence A → (B/C/D parallel) → E
- ❌ Defer GAP-292b payment recording: persona Agent 1 P0-1 explicit — invoice generation without record-payment endpoint = Vy quay lại Excel → defeats whole purpose
- ❌ Ship GAP-353 hash chain Phase 2 sau Wave br-4: persona Agent 1 P0-3 explicit — PDPL Art 11 audit fail risk; cross-bucket Agent 3 Cell 6 lifecycle handler mandate
- ❌ Lock ClassStatus = thêm enum RESCHEDULED: Agent 3 Cell 15 breakage risk existing IT tests + GAP-291 line 28 ambiguity; chốt audit log only (preserves backward compat)
- ❌ Re-scope GAP-292 PER_SESSION enum primary: VN TT Anh ngữ market dùng PER_HOUR; user decision Q2 chốt PER_HOUR primary
- ✅ **5-bucket: A first sequence + B/C/D parallel + E cross-cut sau A code lands** — cân bằng disjoint scope + cross-bucket coordination

**Q3 (rủi ro):**
- Bucket A: AWS suspended (GAP-612) → live verify deferred; ship IaC + script code path only; status target 🟡 PARTIAL ~90% (per Agent 3 Cell 7 PARTIAL exit ramp); paired follow-up gap GAP-NEW-resend-live-verify-post-restore
- Bucket B: PDPL consent_record `user_id` FK cross-database risk (Agent 3 Cell 4) → lock soft reference (no FK); pre-merge ConsentBanner integration race (Agent 3 Cell 6) → analytics SDK lifecycle handler `gtag('consent', 'update', {analytics_storage: 'denied'})` synchronously BEFORE server API call
- Bucket C: V67 migration race với Bucket D V68 (Agent 3 Cell 1) → lock reservation table §3; existing tenant data default risk (Agent 3 Cell 12) → email Phase 1 BETA Owners pre-migration; paired rollback `R67__undo` script (Agent 3 Cell 9)
- Bucket D: ClassRescheduledEvent outbox no-consumer race (Agent 3 Cell 10) → default no-op consumer + feature flag `kite.class.reschedule.notify.enabled=false`; notification classification = OPERATIONAL bypass marketing_consented (Agent 3 Cell 13); reschedule + invoice generation interaction (Agent 3 Cell 5) → cross-bucket IT test `reschedule_preserves_invoice_period_semantics`
- Bucket E (META cross-cut): risk thấp — Mustache template helper, ship sau A/B/C/D code merge để consume

---

## 2. Task Breakdown

| Bucket | Loại | Agent | Phụ thuộc | Thời gian |
|---|---|---|---|---|
| **A** META env-coverage RESEND IaC + CI gate Phase 3 | DevOps + script | Agent A worktree (Opus, narrow scope IaC + reviewer-checklist) | Không (terraform-aws scope tách biệt) | ~2.5h |
| **B** PDPL consent API + hash chain + analytics lifecycle | BE + FE | Agent B worktree (Sonnet) | A code lands (gateway routes audit) | ~3.5h |
| **C** Pricing PER_HOUR + GAP-292b paired payment recording | BE + FE | Agent C worktree (Sonnet) | A code lands (cho FE bundle) | ~3h |
| **D** Reschedule + email fallback + reason MANDATORY | BE + FE + email template | Agent D worktree (Sonnet) | A code lands (Resend email path) | ~3h |
| **E** Email tone matrix Mustache helper + VN sample audit script (META cross-cut) | Email template + script | Agent E worktree (Sonnet, sequential after B/C/D) | B + C + D email templates ship trước | ~1.5h |
| Tổng hợp + ship | Main session | — | All 5 done | ~30 phút |

**Kiểm tra rời rạc:**
- A đụng `infrastructure/terraform-aws/{secrets.tf,iam.tf}` + `scripts/{fetch-secrets.sh,audit-env-coverage.sh}` + `.github/workflows/script-quality.yml` + `documents/02-architecture/env-vars-registry.md`
- B đụng `kitehub/kitehub-subscription/src/main/java/.../consent/` (new package) + `kitehub-subscription/src/main/resources/db/migration/V26__create_consent_record.sql` + `packages/shared-ui/src/components/ConsentBanner/useConsent.ts` (extend) + dossier banner spec + `documents/01-business/kitehub/consent/api-contract.md` (per `contract-first-for-cross-layer.md` §3 — BE+FE same bucket, contract co-located trong bucket scope)
- C đụng `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{course,invoice,payment}/` + Flyway `V67__add_pricing_model_to_courses.sql` (+ paired `R67__undo`) + `kiteclass-frontend/src/app/(dashboard)/courses/` FE radio form + business docs `BR-COURSE-PRICING-001..004` + `documents/01-business/kiteclass/course/api-contract.md` + `documents/01-business/kiteclass/payment/api-contract.md` + ADR-027
- D đụng `kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/{service/ClassServiceImpl.java,controller/ClassController.java}` + Flyway `V68__add_class_reschedule_audit.sql` (audit log only, NO enum change) + `kiteclass-frontend/src/app/(dashboard)/classes/[id]/` FE modal + email template `kitehub/kitehub-email/src/main/resources/templates/class-rescheduled.html.mustache` + `documents/01-business/kiteclass/class/api-contract.md` + ADR-030
- E đụng `kitehub-email/src/main/resources/templates/_shared/persona-tone.mustache` (new) + `scripts/audit-vn-sample-fixtures.sh` (new) + `.github/workflows/script-quality.yml` job `vn-sample-fixtures`

**Conflict risk:**
- A vs B/C/D: A trong `infrastructure/` + `scripts/` + `.github/`; B/C/D trong service code paths. ✅ disjoint
- B vs C/D: B trong `kitehub-subscription` + `shared-ui`; C/D trong `kiteclass-core` + `kiteclass-frontend`. ✅ disjoint
- C vs D: cả 2 trong `kiteclass-core` nhưng khác module — C `course/invoice/payment`; D `clazz`. Maven compile parallel OK. ⚠️ Migration ordering: C ships V67, D ships V68 (sequential reservation per §3.5)
- E vs B/C/D: E phải ship SAU B/C/D vì consume email templates ship từ B (consent email) + D (reschedule email). Sequence: B/C/D parallel → wait merge → E spawn
- Cross-bucket business semantic (Agent 3 Cell 5): reschedule mutates session date affecting invoice period — lock semantic `BR-COURSE-PRICING-004 + BR-CLASS-RESCHEDULE-002` per §3.5

**Revised spawn pattern:** Bucket A first (sequential ~30 phút wait) → B/C/D parallel spawn (~3.5h cap) → E sequential after B/C/D merge (~1.5h).

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH (Compliance + Business-Logic + META cross-cut). Model: Opus 4.7 coordinator + Opus cho Agent A (narrow IaC review reuse Opus precision) + Sonnet cho Agents B/C/D/E (execution scope rõ ràng).

**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5 + `contract-first-for-cross-layer.md` §2):** YES — Buckets B/C/D đều cross-layer (BE controller + FE consumer cùng bucket). Per `contract-first-for-cross-layer.md` §3, api-contract.md update PHẢI ship TRONG cùng bucket (BE+FE same agent ships both, contract co-located trong bucket scope) thay vì dedicated Bucket 0 Foundation (single-agent ownership obviates handoff race). Cross-bucket coordination table §3.5 below.

> **Gap referencing convention** per `.claude/rules/gap-architecture-v2.md`: canonical ids verified via `bash scripts/query-gaps.sh GAP-NNN` — GAP-508 PARTIAL 75% P0 Meta, GAP-353b PENDING P1 Compliance, GAP-292 OPEN 0% P0 Mixed, GAP-291 OPEN 0% P0 Mixed. GAP-292b + GAP-NEW-email-tone-matrix = new gaps filed this Wave.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| A | **META env-coverage RESEND IaC + CI gate Phase 3** | GAP-508 | P0 | `infrastructure/terraform-aws/{secrets.tf,iam.tf}`, `scripts/{fetch-secrets.sh,audit-env-coverage.sh}`, `.github/workflows/script-quality.yml`, `documents/02-architecture/env-vars-registry.md` | first (sequential ~30 phút) |
| B | **PDPL consent API + hash chain + analytics lifecycle** | GAP-353b | P1 | `kitehub/kitehub-subscription/src/main/java/.../consent/` (new), `kitehub-subscription/src/main/resources/db/migration/V26__create_consent_record.sql`, `packages/shared-ui/src/components/ConsentBanner/useConsent.ts`, `documents/01-business/kitehub/consent/api-contract.md` (update), dossier banner spec | parallel after A |
| C | **Pricing PER_HOUR + GAP-292b payment recording paired** | GAP-292 + GAP-292b | P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{course,invoice,payment}/`, `V67__add_pricing_model_to_courses.sql` (+ `R67__undo`), `kiteclass-frontend/src/app/(dashboard)/courses/`, business docs `BR-COURSE-PRICING-001..004`, `documents/01-business/kiteclass/{course,payment}/api-contract.md`, ADR-027 | parallel after A |
| D | **Reschedule + email fallback + reason MANDATORY** | GAP-291 | P0 | `kiteclass-core/.../module/clazz/`, `V68__add_class_reschedule_audit.sql` (audit log only, NO enum), `kiteclass-frontend/src/app/(dashboard)/classes/[id]/`, `kitehub-email/src/main/resources/templates/class-rescheduled.html.mustache`, `documents/01-business/kiteclass/class/api-contract.md`, ADR-030 | parallel after A |
| E | **Email tone matrix + VN sample audit (META cross-cut)** | GAP-NEW-email-tone-matrix | P1 | `kitehub-email/src/main/resources/templates/_shared/persona-tone.mustache` (new), `scripts/audit-vn-sample-fixtures.sh` (new), `.github/workflows/script-quality.yml` job `vn-sample-fixtures` | sequential after B/C/D merge |

### 3.1 Bucket A — META env-coverage RESEND IaC + CI gate Phase 3

- Files: `infrastructure/terraform-aws/secrets.tf` (add `random_password.resend_api_key_placeholder` + `aws_secretsmanager_secret.resend_api_key` resource + `aws_secretsmanager_secret_version` với lifecycle ignore_changes pattern matching jwt-challenge-secret precedent Wave 81 GAP-509), `infrastructure/terraform-aws/iam.tf` (verify wildcard `kitehub/production/*` pattern covers — NO explicit grant edit needed), `scripts/fetch-secrets.sh` (add line `RESEND_API_KEY=$(fetch_secret resend-api-key)` + add to `/etc/kite/.env` template), `scripts/audit-env-coverage.sh` (extend ACCEPTABLE_DEFAULTS với well-commented per-row rationale; ship unit tests với fixtures known good + known bad config), `.github/workflows/script-quality.yml` (add job `env-coverage` WARN-mode initially per `incident-to-rule-pipeline.md` §3.1 tightened defer conditions), `documents/02-architecture/env-vars-registry.md` (add RESEND_API_KEY row + Wave br-4 IaC parity note)
- Acceptance:
  - [ ] Terraform secrets.tf cross-reference matrix per `pre-mutation-state-check.md` v1.2.0 §1.5 (action + resource pattern + actual resource + workflow caller + verdict)
  - [ ] Parity table per `local-fix-production-parity-check.md` v1.0.0 §3.1 (secret resource ✅ + IAM wildcard ✅ no edit + fetch-secrets.sh line ✅ + env-vars-registry row ✅)
  - [ ] `scripts/audit-env-coverage.sh` unit tests PASS
  - [ ] CI job `env-coverage` WARN-mode wired
  - [ ] Override trailer `LOCAL_FIX_PROD_PARITY_DEFER: live verify — AWS account suspended GAP-612 — follow-up GAP-NEW-resend-live-verify-post-restore`
  - [ ] Status target = 🟡 PARTIAL ~90% (live verify deferred GAP-612)
- Agent prompt note: **narrow scope — IaC + script + CI workflow only**; KHÔNG attempt AWS apply (GAP-612 blocked); reuse jwt-challenge-secret precedent Wave 81 lifecycle ignore_changes pattern

### 3.2 Bucket B — PDPL consent API + hash chain + analytics SDK lifecycle handler

- Files: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/` (new package — `ConsentRecord.java` entity + `ConsentService.java` + `ConsentController.java` + `ConsentRepository.java`), `kitehub-subscription/src/main/resources/db/migration/V26__create_consent_record.sql` (immutable schema: `consent_record(id BIGSERIAL, user_id BIGINT NULL, tenant_id BIGINT NULL, granted JSONB NOT NULL, prev_hash VARCHAR(64) NULL, current_hash VARCHAR(64) NOT NULL, ip_address INET NOT NULL, user_agent TEXT NOT NULL, signed_at TIMESTAMPTZ NOT NULL DEFAULT NOW())` + RLS policy NO UPDATE NO DELETE), `packages/shared-ui/src/components/ConsentBanner/useConsent.ts` (extend với `POST /api/v1/consent/record` call + analytics SDK lifecycle handler), `documents/01-business/kitehub/consent/api-contract.md` (update với 3 endpoints), `documents/02-architecture/dossier/14-common-components-inventory-kh.md` (add ConsentBanner spec row), `documents/04-quality/compliance/pdpl-pre-launch-checklist.md` (new — "v1 pending counsel review" disclaimer per CLAUDE.md Phase 1 BETA risk tolerance), ADR-029
- Acceptance:
  - [ ] Migration V26 immutable schema + hash chain `prev_hash → current_hash` (SHA-256)
  - [ ] `POST /api/v1/consent/record` accepts granular toggles JSON + computes current_hash + persists actor signing
  - [ ] `GET /api/v1/consent/{userId}` returns consent history với hash chain validation
  - [ ] `POST /api/v1/consent/withdraw` endpoint (PDPL Art 14 "rút lại sự đồng ý dễ dàng như cho đồng ý")
  - [ ] ConsentBanner `useConsent.ts` extends với server sync + analytics SDK lifecycle handler (revoke ≤5s effective per Agent 3 Cell 6)
  - [ ] IT test `consent_revoke_invalidates_analytics_within_5s` PASS
  - [ ] IT test `concurrent_audit_log_writes_preserve_hash_chain` PASS
  - [ ] api-contract.md updated with 3 endpoints + request/response schemas
  - [ ] Banner spec in `dossier/14-common-components-inventory-kh.md`
  - [ ] PDPL pre-launch checklist file ship (v1 disclaimer)
  - [ ] ADR-029 ship cookie consent vendor decision
  - [ ] Status target = 🟡 PARTIAL (counsel review deferred per Phase 1 BETA risk tolerance)
  - [ ] `./mvnw -pl kitehub-subscription verify -P strict-warnings` PASS

### 3.3 Bucket C — Pricing PER_HOUR + GAP-292b payment recording paired

- Files: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/entity/Course.java` (add `@Enumerated(EnumType.STRING) @Column(name="pricing_model") private PricingModel pricingModel; @Column(name="unit_price") private BigDecimal unitPrice;` — DEPRECATE `price` field), new `module/course/entity/PricingModel.java` enum (PER_HOUR, MONTHLY, COURSE_PACKAGE, FREE per user decision Q2), new `module/payment/entity/PaymentMethod.java` enum (CASH, BANK_TRANSFER, VIETQR, MOMO per user decision Q3), new `module/payment/entity/PaymentRecord.java` entity, new `module/payment/controller/PaymentRecordController.java` (`POST /api/v1/invoices/{id}/record-payment`), update `module/invoice/service/InvoiceGenerationService.java` (per-model logic: PER_HOUR = `sum(attended_sessions × session_hours × unit_price)` trong period; MONTHLY = `unit_price × N_months`; COURSE_PACKAGE = `unit_price` 1-shot; FREE = 0), Flyway `V67__add_pricing_model_to_courses.sql` (NOT NULL với default `COURSE_PACKAGE` cho existing rows) + paired `R67__undo_pricing_model.sql`, `V67b__create_payment_records.sql`, `kiteclass-frontend/src/app/(dashboard)/courses/[id]/edit/page.tsx` (radio form "Hình thức tính học phí: ⚪ Theo giờ (vd 200.000đ/giờ) ⚪ Theo tháng (vd 1.500.000đ/tháng) ⚪ Trọn gói khoá (vd 8.000.000đ/khoá) ⚪ Miễn phí"), `kiteclass-frontend/src/app/(dashboard)/invoices/[id]/page.tsx` (button "Đánh dấu đã thu" + modal radio PaymentMethod), business docs `documents/01-business/kiteclass/course/rules.md` BR-COURSE-PRICING-001..003 (5-attribute per `business-logic-review.md`) + `BR-COURSE-PRICING-004` cross-bucket reschedule semantic, `documents/01-business/kiteclass/payment/rules.md` BR-PAYMENT-METHOD-001..002, `documents/01-business/kiteclass/{course,payment}/api-contract.md` (update với CreateCourseRequest + CourseResponse + PaymentRecordRequest + PaymentRecordResponse), ADR-027 pricing taxonomy (PER_HOUR primary justified by Apollo 257-344k/giờ ILA 195-368k/giờ VN TT Anh ngữ benchmark)
- Acceptance:
  - [ ] PricingModel enum (PER_HOUR + MONTHLY + COURSE_PACKAGE + FREE) shipped
  - [ ] PaymentMethod enum (CASH + BANK_TRANSFER + VIETQR + MOMO) shipped
  - [ ] PaymentRecord entity + Repository + Controller + `POST /api/v1/invoices/{id}/record-payment` endpoint
  - [ ] InvoiceGenerationService respects all 4 pricing models
  - [ ] Migration V67 với default COURSE_PACKAGE + paired R67 undo script
  - [ ] Migration V67b create payment_records table
  - [ ] FE form supports model switch (only at course creation, not edit — preserves billing history)
  - [ ] FE invoice button "Đánh dấu đã thu" + modal radio PaymentMethod ship
  - [ ] Cross-bucket IT `RescheduleInvoicePeriodSemanticsIT` PASS (Bucket D dependency)
  - [ ] Unit tests cover 4 pricing models × invoice scenarios
  - [ ] Business docs 3-layer (rules.md + use-cases.md + api-contract.md) updated per `audit-to-gap-pipeline.md` §2.5 domain CI detector
  - [ ] ADR-027 pricing taxonomy ship
  - [ ] Pre-migration data audit script `scripts/audit-pre-pricing-model.sql` lists active courses needing reclassification (Agent 3 Cell 12)
  - [ ] Email Phase 1 BETA Owners pre-migration verification (manual step, document in PR)
  - [ ] Status target = 🟢 DONE
  - [ ] `./mvnw -pl kiteclass-core verify -P strict-warnings` PASS

### 3.4 Bucket D — Reschedule + email fallback + reason MANDATORY

- Files: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/service/ClassServiceImpl.java` (add `reschedule(classId, newStartDate, newEndDate, reasonCategory, reasonNotes)` method — preserves ClassStatus = SCHEDULED, mutates dates, writes AuditLog entry, publishes ClassRescheduledEvent via Outbox per `design-patterns.md §3.5`), `module/clazz/controller/ClassController.java` (add `POST /api/v1/classes/{id}/reschedule`), Flyway `V68__add_class_reschedule_audit.sql` (audit log columns only — NO enum change per locked decision §3.5: `rescheduled_by_user_id, rescheduled_at, previous_start_time, previous_end_time, reschedule_reason_category, reschedule_reason_notes`), `module/clazz/event/ClassRescheduledEvent.java` (Outbox payload với 5 mandatory fields per Agent 2 F3 Cal.com benchmark), new `module/clazz/event/consumer/ClassRescheduledNoOpConsumer.java` (default no-op consumer + feature flag `kite.class.reschedule.notify.enabled=false` per Agent 3 Cell 10), `kiteclass-frontend/src/app/(dashboard)/classes/[id]/page.tsx` (add "Đổi lịch" button + modal với date/time picker + reason MANDATORY dropdown preset "GV ốm/bận đột xuất | Phòng học không khả dụng | Mất điện/internet | Lễ Tết/nghỉ chính thức | Học sinh xin nghỉ tập thể | Lý do khác" + free-text notes optional per persona Agent 1 P1-2), email template `kitehub/kitehub-email/src/main/resources/templates/class-rescheduled.html.mustache` (per persona Agent 1 P0-2 mandatory; greeting `{{persona_greeting}}` from Bucket E shared helper; subject `Thông báo đổi lịch lớp {{className}} — Trung tâm {{tenantName}}`; body Vietnamese narrative per `vn-localization-audit-checklist.md` §2 — Date `{{rescheduledDate}}` format `Thứ Hai, 14/05/2026`; sample data VN-friendly: tenant `Trung tâm Anh ngữ Sky Education`, class `Lớp Anh ngữ 5A1`), `kitehub-email/src/main/java/.../EmailService.java` (add `sendClassRescheduledEmail(parents, classInfo)` method publishing to email queue), `documents/01-business/kiteclass/class/api-contract.md` (update với reschedule endpoint), ADR-030 reschedule pattern (Cal.com update-in-place + 5-field audit log + Email Phase 1 / Zalo OA Phase 2)
- Acceptance:
  - [ ] `ClassService.reschedule()` method preserves attendance + grade history (Cal.com pattern per Agent 2 F3)
  - [ ] Audit log captures 5 mandatory fields (rescheduled_by_user_id, rescheduled_at, previous_start_time, previous_end_time, reschedule_reason_category) + reason_notes optional
  - [ ] ClassRescheduledEvent published via Outbox pattern
  - [ ] Default no-op consumer registered + feature flag `kite.class.reschedule.notify.enabled=false` default
  - [ ] ClassStatus unchanged (SCHEDULED → SCHEDULED) — existing IT tests PASS unchanged
  - [ ] FE 3-click flow: Open class → "Đổi lịch" button → modal save
  - [ ] Reason MANDATORY dropdown 6 preset options + free-text notes optional
  - [ ] Email template `class-rescheduled.html.mustache` ship với persona-aware greeting (consume Bucket E helper)
  - [ ] Email fallback fires immediately khi feature flag `notify.enabled=true` (Phase 1 = email-only; Zalo OA Phase 2)
  - [ ] Notification classification = OPERATIONAL bypass marketing_consented (per Agent 3 Cell 13 locked decision)
  - [ ] Cross-bucket IT `RescheduleInvoicePeriodSemanticsIT` coordinated với Bucket C (Agent 3 Cell 5)
  - [ ] api-contract.md updated với reschedule endpoint
  - [ ] ADR-030 reschedule pattern ship
  - [ ] Status target = 🟢 DONE
  - [ ] `./mvnw -pl kiteclass-core verify -P strict-warnings` PASS

### 3.5 Bucket E — Email tone matrix Mustache helper + VN sample audit script (META cross-cut)

- Files: `kitehub/kitehub-email/src/main/resources/templates/_shared/persona-tone.mustache` (new partial — defines `{{persona_greeting}}` variable resolution: `P1_SOLO_TEACHER → "Chào em,"`, `P2_CENTER_OWNER → "Em chào chị,"`, `P3_CENTER_MANAGER → "Em chào chị/anh,"`, `PARENT → "Kính gửi quý phụ huynh,"`, `STUDENT → "Chào em,"`, default `PLATFORM_ADMIN → "Em chào anh/chị,"`), `kitehub-email/src/main/java/.../template/PersonaToneResolver.java` (Java service to inject persona variable into Mustache context based on recipient role), `scripts/audit-vn-sample-fixtures.sh` (new — grep test fixtures for English placeholder anti-patterns: `John Doe`, `Jane Doe`, `Class A1`, `Example Center`, `Lorem ipsum`, `$NN.NN` USD format → WARN-mode initially per `incident-to-rule-pipeline.md` §3.1), `.github/workflows/script-quality.yml` (add CI job `vn-sample-fixtures` WARN-mode wrapping audit script)
- Acceptance:
  - [ ] `persona-tone.mustache` partial ship với 5 persona greeting mappings (per `vn-localization-audit-checklist.md` §2 Section 2 persona tone matrix)
  - [ ] `PersonaToneResolver.java` injects `{{persona_greeting}}` variable into Mustache context
  - [ ] Bucket B + Bucket D email templates consume `{{> _shared/persona-tone}}` partial (cross-bucket integration verified post-merge)
  - [ ] `scripts/audit-vn-sample-fixtures.sh` detect English placeholder anti-patterns trong test fixtures + email previews
  - [ ] CI job `vn-sample-fixtures` WARN-mode wired
  - [ ] Unit tests PASS (5 persona × greeting matrix)
  - [ ] Status target = 🟢 DONE
  - [ ] `./mvnw -pl kitehub-email verify -P strict-warnings` PASS

### 3.6 Cross-bucket lock decisions (mandatory BEFORE agent spawn)

1. **Migration version reservation:** Bucket C = V67 (pricing) + V67b (payment_records), Bucket D = V68 (reschedule audit). Bucket B = V26 (kitehub-subscription scope separate, no race với kiteclass-core)
2. **Bucket B service:** `kitehub-subscription` (NOT new kitehub-consent service — avoid Phase 1 scope creep per Agent 3 Cell 2)
3. **consent_record `user_id`:** soft reference (no FK constraint) với index only; document inline migration comment "soft reference to user identity across services; FK omitted per multi-tenant architecture" (Agent 3 Cell 4)
4. **ClassStatus enum:** NO new RESCHEDULED enum; status STAYS SCHEDULED + audit log columns only (Agent 3 Cell 15 backward compat preservation)
5. **Reschedule notification classification:** OPERATIONAL (bypass marketing_consented gate per Agent 3 Cell 13 + PDPL operational vs marketing legal interpretation); document trong `business-logic-review.md` decision log
6. **Bucket A status target:** 🟡 PARTIAL ~90% (live verify blocked GAP-612 AWS suspension per Agent 3 Cell 7); follow-up gap GAP-NEW-resend-live-verify-post-restore tracked
7. **Outbox event default no-op consumer:** ClassRescheduledEvent ship với no-op consumer + feature flag `kite.class.reschedule.notify.enabled=false` default; outbox queue depth alert via Wave 91 wiring (Agent 3 Cell 10)
8. **Email tone matrix mandate:** Bucket B (consent emails if any) + Bucket D (class-rescheduled email) MUST consume `_shared/persona-tone.mustache` partial — Bucket E ships partial; B/D ship template consumers
9. **Cross-bucket IT tests mandatory:** `consent_revoke_invalidates_analytics_within_5s` (Bucket B) + `reschedule_preserves_invoice_period_semantics` (Bucket C + D cross) + `concurrent_audit_log_writes_preserve_hash_chain` (Bucket B)
10. **api-contract.md update per `contract-first-for-cross-layer.md`:** Each cross-layer bucket (B/C/D) updates corresponding api-contract.md TRONG bucket scope (single-agent ownership BE+FE both, contract co-located eliminates Bucket 0 Foundation handoff race)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `kiteclass-core` migration latest V66 | Flyway version baseline | `ls kiteclass/kiteclass-core/src/main/resources/db/migration/V*.sql \| sort -V \| tail -1` | `V66__shared_idempotency_keys.sql` confirmed | ✅ V66 latest; V67 next available (Bucket C reserve) |
| `kitehub-subscription` migration latest | Flyway version baseline | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql \| sort -V \| tail -1` | TBD by Agent B state-check at spawn | 🆕 V26 to-be-created (Bucket B Foundation) |
| `PricingModel` Java enum (Wave br-4 Bucket C scope) | Java enum (new) | `grep -rn "enum PricingModel" kiteclass/kiteclass-core/src/main/java` | TBD: zero hits expected (greenfield) | 🆕 to-be-created (Bucket C) |
| `PaymentMethod` Java enum (Wave br-4 Bucket C scope) | Java enum (new) | `grep -rn "enum PaymentMethod" kiteclass/kiteclass-core/src/main/java` | TBD: zero hits expected (greenfield) | 🆕 to-be-created (Bucket C — paired GAP-292b) |
| `PaymentRecord` Java entity (Wave br-4 Bucket C scope) | Java entity (new) | `grep -rn "class PaymentRecord" kiteclass/kiteclass-core/src/main/java` | TBD: zero hits expected (greenfield) | 🆕 to-be-created (Bucket C — paired GAP-292b) |
| `ClassService.reschedule()` method (Wave br-4 Bucket D scope) | Java method (new) | `grep -rn "reschedule" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/` | Verified zero hits 2026-05-24 pre-wave | 🆕 to-be-created (Bucket D — greenfield) |
| `ClassRescheduledEvent` (Outbox payload) | Java class (new) | `grep -rn "ClassRescheduledEvent" kiteclass/kiteclass-core/src/main/java` | TBD: zero hits expected | 🆕 to-be-created (Bucket D) |
| `aws_secretsmanager_secret.resend_api_key` | Terraform resource (new) | `grep -n "resend_api_key" infrastructure/terraform-aws/secrets.tf` | TBD: zero hits expected (Wave 81 jwt-challenge precedent only) | 🆕 to-be-created (Bucket A) |
| `documents/01-business/kiteclass/course/rules.md` BR-COURSE-PRICING-001..004 | Business rule docs | `grep -n "BR-COURSE-PRICING" documents/01-business/kiteclass/course/rules.md` | TBD: zero hits expected | 🆕 to-be-created (Bucket C — 5-attribute per `business-logic-review.md`) |
| `documents/01-business/kiteclass/payment/rules.md` BR-PAYMENT-METHOD-001..002 | Business rule docs | `grep -n "BR-PAYMENT-METHOD" documents/01-business/kiteclass/payment/rules.md` | TBD: zero hits expected | 🆕 to-be-created (Bucket C — paired GAP-292b) |
| `documents/01-business/kitehub/consent/api-contract.md` | API contract doc | `ls documents/01-business/kitehub/consent/api-contract.md 2>/dev/null` | Verified 2026-05-24 missing | 🆕 to-be-created (Bucket B — per `contract-first-for-cross-layer.md`) |
| `documents/01-business/kiteclass/{course,payment,class}/api-contract.md` | API contract docs | `ls documents/01-business/kiteclass/*/api-contract.md` | TBD by agents at spawn | ⚠️ partial-update (Bucket C/D extend với new endpoints) |
| `packages/shared-ui/src/components/ConsentBanner/useConsent.ts` | TS hook (existing Wave 23 Bucket BC) | `ls packages/shared-ui/src/components/ConsentBanner/useConsent.ts` | TBD by Agent B at spawn | ✅ exists (Wave 23 ship) → extend với server sync (Bucket B) |
| `kitehub-email/src/main/resources/templates/_shared/` directory | Mustache shared partials | `ls kitehub/kitehub-email/src/main/resources/templates/_shared/` | TBD by Agent E at spawn | 🆕 to-be-created (Bucket E — partial directory + persona-tone.mustache) |
| `scripts/audit-env-coverage.sh` | Bash script (existing Wave 78) | `ls scripts/audit-env-coverage.sh` | Verified exists per GAP-508 Phase 1 ship | ✅ exists → extend ACCEPTABLE_DEFAULTS (Bucket A) |
| `scripts/fetch-secrets.sh` | Bash script (existing) | `ls scripts/fetch-secrets.sh` | Verified exists per Wave 81 GAP-509 + Wave 88 | ✅ exists → add RESEND_API_KEY fetch line (Bucket A) |
| `infrastructure/terraform-aws/iam.tf` wildcard `${var.project_name}/${var.environment}/*` | IAM grant pattern | `grep -n "project_name.*environment" infrastructure/terraform-aws/iam.tf` | TBD by Agent A at spawn | ✅ exists (Wave 81 GAP-509 precedent) → NO edit (wildcard covers) |
| GAP-612 AWS account suspension | External state | `aws sts get-caller-identity --profile dev-admin 2>&1` | Verified 2026-05-24 — `(aws not authenticated)` per `start-session` collect-state | ❌ blocked → Bucket A live verify deferred per §3.6 lock decision 6 |

**Forward-looking references summary:** 14 🆕 to-be-created (all new code/schema/business rules paired with Bucket ownership) + 4 ✅ exists (existing artifacts to extend) + 1 ❌ blocked (GAP-612 AWS — deferred per §3.6 lock decision 6). State-check evidence section satisfies `audit-to-gap-pipeline.md` §2.6 mandate (every code-symbol-shaped reference verified present OR marked 🆕 with explicit creation owner).

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate | Cross-bucket IT test |
|--------|---------------------|---------|---------------------|
| A | `bash scripts/audit-env-coverage.sh` (unit tests) + `bash scripts/test/audit-env-coverage-test.sh` (fixtures) + `terraform validate` (no apply) | `script-quality.yml` job `env-coverage` WARN-mode | — |
| B | `./mvnw -pl kitehub-subscription verify -P strict-warnings` + `pnpm -F shared-ui test:run` | `core-ci.yml` job `kitehub-subscription` + `frontend-ci.yml` shared-ui | `consent_revoke_invalidates_analytics_within_5s` + `concurrent_audit_log_writes_preserve_hash_chain` |
| C | `./mvnw -pl kiteclass-core verify -P strict-warnings` + `pnpm -F kiteclass-frontend test:run` | `core-ci.yml` kiteclass-core + `frontend-ci.yml` kiteclass-frontend | `RescheduleInvoicePeriodSemanticsIT` (cross-bucket với D) |
| D | `./mvnw -pl kiteclass-core verify -P strict-warnings` + `./mvnw -pl kitehub-email verify -P strict-warnings` + `pnpm -F kiteclass-frontend test:run` | `core-ci.yml` kiteclass-core + kitehub-email + `frontend-ci.yml` kiteclass-frontend | `RescheduleInvoicePeriodSemanticsIT` (cross-bucket với C) |
| E | `./mvnw -pl kitehub-email verify -P strict-warnings` + `bash scripts/audit-vn-sample-fixtures.sh` | `core-ci.yml` kitehub-email + `script-quality.yml` job `vn-sample-fixtures` WARN-mode | Manual: Bucket B + D email templates consume `{{> _shared/persona-tone}}` partial verification |

Verification checkpoint per bucket — local `mvn verify` PASS BEFORE PR open; CI green REQUIRED BEFORE merge; cross-bucket IT tests run as part of Bucket C/D verify (single Maven test suite catches both).

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

### 6.1 Phase 1 — Bucket A first (sequential ~2.5h)

Spawn Agent A:
- `subagent_type`: `general-purpose` (or specialized devops nếu có)
- `isolation`: `worktree`
- `run_in_background`: `true` (per `agent-background-spawn-default.md` §1)
- Model: Opus 4.7 (narrow IaC scope = Opus precision reuse jwt-challenge precedent Wave 81)
- Scope: Bucket A only — terraform-aws + scripts + workflow + env-vars-registry
- Verification gate: §5 row A local + CI

**Wait Bucket A merge before Phase 2 spawn.** Bucket A code lands → unblocks Bucket B (gateway routes audit) + Bucket D (Resend email path).

### 6.2 Phase 2 — Bucket B + C + D parallel (single message, 3 agents ~3.5h cap)

Spawn 3 agents song song trong single message (multiple Agent tool blocks):
- Agent B (Sonnet, worktree): Bucket B PDPL consent API + hash chain + analytics SDK lifecycle handler
- Agent C (Sonnet, worktree): Bucket C Pricing PER_HOUR + GAP-292b paired
- Agent D (Sonnet, worktree): Bucket D Reschedule + email fallback + reason MANDATORY

Cross-bucket IT test `RescheduleInvoicePeriodSemanticsIT` ship trong Bucket C OR D (single-agent ownership; cross-bucket coordination via shared test class fully in scope cross-cut bucket).

Each agent reads §3 corresponding bucket subsection + §3.6 cross-bucket lock decisions + §5 verification gate.

**Wait B/C/D merge before Phase 3 spawn.** B/D email templates ship → unblock Bucket E consume.

### 6.3 Phase 3 — Bucket E sequential (~1.5h)

Spawn Agent E:
- `subagent_type`: `general-purpose`
- `isolation`: `worktree`
- `run_in_background`: `true`
- Model: Sonnet 4.6
- Scope: Bucket E only — Mustache helper + audit script + CI workflow job
- Verification gate: §5 row E + cross-bucket integration verify (Bucket B + D email templates consume partial)

### 6.4 Phase 4 — Closure (~30 phút)

Main session (Opus 4.7 coordinator):
- Run §7 Closure Protocol
- 4-target sync per `post-merge-sync-completeness.md` §2
- Scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3
- Audit suite scheduling per `post-wave-audit-mandate.md` §2.2 3-day cadence
- Session-handoff note creation per `session-end-context-check.md` §4.5 row 5

---

## 7. Closure Protocol (per `wave-closure-scope-completeness.md` §3)

### 7.1 Pre-flip status:draft → status:complete

1. **Scope-Completeness Reconciliation table** — verify all 5 buckets AC checked + categorize ✅/🟡/❌ per item

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Bucket A — RESEND IaC + script + CI Phase 3 | 🟡 PARTIAL ~90% | GAP-NEW-resend-live-verify-post-restore (gated GAP-612) |
| 2 | Bucket B — PDPL consent API + hash chain | 🟡 PARTIAL | GAP-NEW-counsel-pdpl-pre-launch-review (Phase 2 counsel) |
| 3 | Bucket C — Pricing PER_HOUR + GAP-292b payment | 🟢 DONE expected | — (paired follow-up GAP-NEW-pricing-data-reclassification for existing tenant migration) |
| 4 | Bucket D — Reschedule + email fallback | 🟢 DONE expected | — (paired follow-up GAP-NEW-zalo-oa-notification-integration Wave br-5+) |
| 5 | Bucket E — Email tone matrix + VN sample audit | 🟢 DONE expected | — |
| 6 | ADR-027/028/029/030 (4 ADRs paired ship) | 🟢 DONE expected | — |

2. **Post-wave audit suite trigger** (per `post-wave-audit-mandate.md` §2.2 3-day deadline) — schedule Security + Business-Logic + API contract + Ops Readiness audits within 3 ngày của closure (cadence cap ngày 2026-05-27)

3. **4-target sync** per `post-merge-sync-completeness.md` §2:
   - `gap-status.csv`: 4 status flips (GAP-508 PARTIAL→PARTIAL 90%, GAP-353b PENDING→PARTIAL, GAP-292 OPEN→DONE, GAP-291 OPEN→DONE) + 4 new rows (GAP-292b new, GAP-NEW-email-tone-matrix new, GAP-NEW-resend-live-verify-post-restore new, GAP-NEW-pricing-data-reclassification new)
   - `ROADMAP.md §🎯 Current Status Snapshot`: Wave br-4 SHIPPED entry
   - `wave-history.jsonl`: Wave br-4 outcome entry (5 buckets shipped, follow-ups filed, audit suite scheduled) — per `wave-tag-numbering-convention.md` §2.5 new format với tag_primary + counter
   - `MEMORY.md`: new memory entry `feedback_outside_in_audit_value_wave_br_4.md` (3-agent audit prevented 7 P0 + 10 P1 cells)

4. **GAP closure flips per `gap-done-discipline.md` §2:** AC checked + no banned phrases + follow-up filed for any PARTIAL deferral

5. **Session-handoff note** per `session-end-context-check.md` §4.5 row 5: `documents/03-planning/session-handoffs/2026-05-24-wave-br-4-closure.md`

6. **Post-wave cleanup** per `post-wave-cleanup.md` §2: `bash scripts/prune-merged-worktrees.sh --yes`

### 7.2 6 follow-up gaps filed at closure

| Gap | Priority | Scope | Trigger |
|---|---|---|---|
| GAP-NEW-resend-live-verify-post-restore | P1 | Live verify Resend API key delivery + DKIM/SPF/DMARC + smoke test 5 VN ISPs | GAP-612 AWS restore unblock |
| GAP-NEW-pricing-data-reclassification | P1 | Per-tenant manual reclassification UI + email all Phase 1 BETA Owners verify pricing model | Bucket C post-merge |
| GAP-NEW-zalo-oa-notification-integration | P2 | GAP-063 Phase 2 Zalo OA notification consumer subscribes ClassRescheduledEvent | Wave br-5+ |
| GAP-NEW-vat-einvoice-misa-integration | P2 | MISA MeInvoice partnership investigation per Agent 2 F5 + Wave 93 GAP-185 precedent | Wave br-5+ |
| GAP-NEW-counsel-pdpl-pre-launch-review | P1 | Legal counsel review PDPL pre-launch checklist + privacy policy text + consent banner UX | Phase 2 trigger (counsel engaged) |
| GAP-NEW-resend-vs-ses-phase-2-eval | P2 | AWS SES migration evaluation cost + VN region option ap-southeast-1 | Phase 2 cost optimization |

### 7.3 Risk register (cross-reference §1 Brainstorm Q3)

| # | Risk | Severity | Mitigation | Cross-link |
|---|---|---|---|---|
| 1 | Bucket A AWS verify blocked GAP-612 → silent IaC drift if Resend account verification fails post-restore | P0 | PARTIAL status ~90% + paired follow-up gap | Agent 3 Cell 7 |
| 2 | Migration V67 (Bucket C) race với V68 (Bucket D) khi 2 agents parallel | P0 | Hardcode reservation §3.6; cross-bucket merge dry-run pre-merge | Agent 3 Cell 1, Wave 105 precedent |
| 3 | Bucket B consent_record cross-database FK → migration fails OR silent skip | P0 | Lock soft reference no FK §3.6 | Agent 3 Cell 4 |
| 4 | Bucket C existing tenant data default COURSE_PACKAGE wrong for MONTHLY-billed courses | P1 | Pre-migration audit script + email Phase 1 BETA Owners | Agent 3 Cell 12 |
| 5 | Bucket D ClassRescheduledEvent outbox without consumer → unbounded queue grow | P1 | Default no-op consumer + feature flag | Agent 3 Cell 10 |
| 6 | Cross-bucket reschedule + invoice generation interaction → wrong invoice amount | P0 | Cross-bucket IT test mandatory; BR-COURSE-PRICING-004 + BR-CLASS-RESCHEDULE-002 | Agent 3 Cell 5 |
| 7 | Bucket B analytics SDK lifecycle handler lag on revoke → PDPL Art 13 audit fail | P0 | Synchronous gtag denied BEFORE server API call; IT test verify ≤5s | Agent 3 Cell 6 |
| 8 | Bucket B PDPL pre-launch checklist solo-dev no counsel → banned-phrase risk on DONE flip | P1 | PARTIAL exit ramp; "v1 pending counsel review" disclaimer | Agent 3 Cell 11 |
| 9 | VN sample data drift trong test fixtures + email previews → English placeholder leak | P1 | Bucket E audit script + reviewer-checklist mandatory | Persona Agent 1 Pattern 2 |
| 10 | Persona tone mismatch trong 3 email templates → Vy formal greeting defeats target | P1 | Bucket E shared Mustache helper consumed by B/D | Persona Agent 1 Pattern 1 |

---

## 8. Log

- **2026-05-24:** Wave plan drafted. 3 outside-in agents ran parallel (persona simulation + VN edu benchmark + failure-mode matrix). User 3-Q decisions chốt: (a) PDPL deadline giữ CLAUDE.md 2026-07-01, (b) Pricing taxonomy = PER_HOUR primary, (c) Bucket C ghép GAP-292b paired payment recording. Counter conflict resolved 3→4 (wave-beta-readiness-3-idempotency-completion-test-flake-email-pipeline đã claim counter 3 trên main). 5 required sections added per CI wave-plan-completeness check (§4 State-Check Evidence + §5 Verification Gates + §6 Agent Spawn Pattern + §7 Closure Protocol + §8 Log). Wave plan ship via PR per `feedback_wave_plan_through_pr.md` BEFORE agent spawn implementation.
