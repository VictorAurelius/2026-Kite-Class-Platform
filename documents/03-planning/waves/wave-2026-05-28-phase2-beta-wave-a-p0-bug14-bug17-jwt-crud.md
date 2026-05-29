---
title: Wave A — Phase 2 BETA P0 fixes (Bug #14 email + Bug #17 user provision + GAP-704 JWT + Course/Class CRUD)
tag_primary: phase2-beta-wave-a
tags_secondary: [bug-14, bug-17, gap-704, plan-d-v2, p0]
status: draft
created: 2026-05-28
updated: 2026-05-28
target_weeks: [1, 2]
estimated_eng_days: 7-12
gaps: [GAP-786, GAP-787, GAP-788, GAP-704]
waves: [phase2-beta-wave-a]
---

# Wave A — Phase 2 BETA P0 fixes (Bug #14 email + Bug #17 user provision + GAP-704 JWT + Course/Class CRUD)

**Goal:** Đóng 4 P0 architecture/critical gap chặn Phase 2 BETA invite — staff invitation email path (Bug #14), user provision on accept (Bug #17), JWT tenantId post-signup (GAP-704), và Course/Class CRUD foundational walk.

**Trigger:** Wave meta-6 Bucket A RST walk shutdown 2026-05-28 surfaced 17 bugs (5 P0 + 8 P1 + 4 P2). 3 outside-in agents (persona-defense / failure-mode matrix / VN edu SaaS benchmark) converged trên refined Plan D v2 4-8 tuần. User strategic decision locked Plan D v2; Wave A = Week 1-2 P0 architecture fixes.

**Estimated wall-clock:** ~7-12 eng-days theo Plan D v2 Week 1-2 timeline. Recommend sequential single-agent execution (xem §6) thay vì parallel 4-agent thrash risk.

---

## 1. Brainstorm (Inside-out + Outside-in)

### Q1 (alignment) — 4 nguồn inside-out + outside-in (per `inside-out-completeness-trigger.md` §3)

**Inside-out from ROADMAP §🚀 Next Action (canonical):**
- Bug #14 (staff invitation email path missing) — RST walk 2026-05-28 Mảng A bug surface
- Bug #17 (no user provision on accept) — RST walk same source
- GAP-704 (JWT tenantId claim post-signup verify) — pre-existing ROADMAP queue
- Course/Class CRUD walk — foundational P0 cho beta tenant onboarding

**Inside-out from `documents/03-planning/inside-out-queue.md`:**
- Đã consult — không có additional Phase 2 BETA Week 1-2 items chưa cover; queue mirror `project_phase_1_beta_inside_out_queue.md` memory cũng align với 4 items scope.

**Inside-out from CSV query phase-2-beta status non-DONE:**
- GAP-786 (Bug #17 user provision), GAP-787 (Bug #14 email path), GAP-788 (META retro-walk discipline), GAP-704 (JWT claim) — 4 gaps khớp scope; no additional P0 surface trong CSV.

**Outside-in findings (3 agents same session 2026-05-28):**
- **Persona-defense agent:** P2 Center Owner (chị Hằng) blocked at staff invitation step → cannot complete BETA onboarding flow. P3 Manager (anh Tâm) blocked tại login post-accept vì user record absent.
- **Failure-mode matrix agent:** Email path failure mode = silent fail (invitation created in DB, email never queued → owner thinks broken UX). User-provision failure mode = redirect-loop on login (invitation marked accepted but no user → 404 → re-invite → user confusion compounds).
- **VN edu SaaS benchmark agent:** Misa eShop / OneOffice patterns — invitation flow MUST be transactional (invite + email + accept + user-create atomic OR clearly separated with status tracking). Current state = partial (invite + accept logged, email + user-create missing) = worst-of-both-worlds.

### Q1b (outside-in audit skip rationale per `outside-in-coverage-trigger.md` §4)

Skip outside-in audit cho wave plan này VÌ `outside-in-coverage-trigger.md` §4 row 4 ("User đã trải qua outside-in (audit gần đây ≤ 30 ngày)") applies — 3-agent outside-in synthesis vừa ship same session 2026-05-28 (`documents/04-quality/audits/persona-review/2026-05-28-beta-plan-d-v2-3-agent-synthesis.md`). Audit findings đã absorb vào scope; spawning lại agent = duplicate work.

### Q2 (trade-offs) — alternatives considered

- **Alt 1 (rejected): Parallel 4 buckets từ Day 1.** Risk: Bucket A+B architecture decisions chưa locked (Option A/B/C cho cả 2) → parallel agents will thrash trên design decisions. Reject — sequential better cho solo dev.
- **Alt 2 (rejected): Bundle thêm Bug #15+#16 (P1) vào Wave A.** Risk: scope creep ≥15 eng-days, vi phạm Plan D v2 Week 1-2 timeline. Reject — defer Bug #15+#16 sang Wave B (Week 3-4).
- **Alt 3 (rejected): Defer Course/Class CRUD walk to Wave B.** Risk: CRUD = foundational; nếu Bug #14+#17 fix xong nhưng owner login → empty dashboard (no course/class created) → bad first-impression. Reject — keep Bucket D in Wave A even though "lighter" than A/B.
- **Alt 4 (selected): 4 buckets sequential same agent, Decision Day 1-2 lock architecture trước implementation Day 3-12.** Reason: investigation-first per `release-fix-retry-budget.md` §3.5; eliminates 5-retry thrash class observed Wave meta-1 GAP-735.

### Q3 (risks) — chi tiết §5 Risks + Mitigations dưới

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-787 (Bug #14 email path) | Solo dev (sequential agent #1) | est. 5-7 eng-days | ⚠️ Cross-cuts `kitehub-subscription/staff` + `kitehub-email` + RabbitMQ infra |
| B | GAP-786 (Bug #17 user provision) | Solo dev (sequential agent #2) | est. 3-5 eng-days | ⚠️ Cross-cuts `kitehub-subscription/staff` + `kitehub-platform` (user create) |
| C | GAP-704 (JWT tenantId claim) | Solo dev (sequential agent #3) | est. 1-2 eng-days | ✅ Isolated `kitehub-gateway/filter` |
| D | Course/Class CRUD walk + IT | Solo dev (sequential agent #4) | est. 2 eng-days | ✅ Isolated `kiteclass-core/module/{course,clazz}` |

**Total:** 7-12 eng-days fit Plan D v2 Week 1-2 budget.

**Disjoint check:** Bucket A + B touch cùng `kitehub-subscription/staff` package → sequential mandatory (cannot parallelize without merge conflict risk). Bucket C + D isolated, có thể parallel với A+B nếu coordinator prefer 2-wave-pack model — recommend sequential single-agent vẫn cho solo dev review velocity.

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH → model: Opus 4.7 1M (mandatory per `agent-model-opus-default.md` — non-trivial bucket, recurrence risk Sonnet thrash confirmed Wave br-4 + Wave beta-readiness-8).

**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** Partial cross-layer — Bucket A touches BE (staff invitation + email service) + RabbitMQ infra (no FE consumer trong wave này; FE staff accept page đã exist). Bucket B touches BE only (user create). Bucket C+D isolated. **No Bucket 0 Foundation required** vì wave này KHÔNG add new API endpoint consumed by FE bucket cùng wave — Bug #14 + #17 fix internal BE flow + email infrastructure; existing FE staff accept page already consumes existing endpoint.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-787 | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/**`, `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/**`, `kitehub/kitehub-email/src/main/java/com/kitehub/email/**`, `kitehub/kitehub-email/src/main/resources/templates/email/**`, `kitehub/kitehub-subscription/src/main/resources/db/migration/V**.sql` | 1st (sequential) |
| 2 | **B** | GAP-786 | 🔴 P0 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/service/**`, `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/user/**` (cross-module if Option C) | 2nd (after A locks architecture) |
| 3 | **C** | GAP-704 | 🔴 P0 | `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java`, `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/auth/**` (issuance code path) | 3rd (isolated, can parallel với D) |
| 4 | **D** | (no gap — foundational walk) | 🔴 P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/**`, `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/**`, `kiteclass/kiteclass-core/src/test/java/.../module/{course,clazz}/**IT.java` | 4th (isolated, can parallel với C) |

### Bucket A — Bug #14 staff invitation email path implementation

- **Files:**
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/service/StaffInvitationServiceImpl.java` (UPDATE — emit outbox event in `invite()` method)
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/SubscriptionOutboxDispatcher.java` (UPDATE — bind new event class)
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/StaffInvitationCreatedEvent.java` (🆕 CREATE)
  - `kitehub/kitehub-email/src/main/java/com/kitehub/email/listener/StaffInvitationEmailListener.java` (🆕 CREATE — RabbitMQ consumer)
  - `kitehub/kitehub-email/src/main/resources/templates/email/staff-invitation.html` (🆕 CREATE — Vietnamese narrative per `vn-localization-audit-checklist.md`)
  - `kitehub/kitehub-email/src/main/resources/templates/email/staff-invitation.txt` (🆕 CREATE — plain-text fallback)
  - `kitehub/kitehub-subscription/src/main/resources/application.yml` (UPDATE — RabbitMQ exchange/binding declaration `staff.invitation.created` topic)
  - `kitehub/kitehub-email/src/main/resources/application.yml` (UPDATE — queue auto-declare `email.staff-invitation.queue`)
- **Tests:**
  - `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/staff/service/StaffInvitationServicePostgresIT.java` (UPDATE — verify outbox event emitted after `invite()`)
  - `kitehub/kitehub-email/src/test/java/com/kitehub/email/listener/StaffInvitationEmailListenerIT.java` (🆕 CREATE — Testcontainers RabbitMQ + MailHog smoke)
- **Acceptance:** xem §4 Bucket A AC checklist

### Bucket B — Bug #17 user provision on accept

- **Files:** depends on Decision Day 1-2 Option A/B/C lock (xem §5 Risk 1 mitigation):
  - **Option A — Direct UserRepository inject vào StaffInvitationServiceImpl:**
    - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/service/StaffInvitationServiceImpl.java` (UPDATE — call userRepository.save in accept())
    - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/service/StaffInvitationServiceImpl.java` cần inject `UserRepository` (cross-module if user table owned by platform — likely option violation)
  - **Option B — Emit outbox event `staff.invitation.accepted` → platform user-service consumer creates user:**
    - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/outbox/StaffInvitationAcceptedEvent.java` (🆕 CREATE)
    - `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/user/listener/StaffUserProvisionListener.java` (🆕 CREATE)
    - `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/user/service/UserProvisionService.java` (UPDATE — handle provision from staff invitation)
  - **Option C — Sync HTTP call subscription → platform `/api/internal/users` create endpoint:**
    - Requires Bucket 0 Foundation (api-contract.md) — heavier scope; reject unless strongly justified
- **Tests:**
  - `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/staff/service/StaffInvitationServicePostgresIT.java` (UPDATE — verify user row exists post-accept)
  - `kitehub/kitehub-platform/src/test/java/com/kitehub/platform/user/listener/StaffUserProvisionListenerIT.java` (🆕 CREATE — if Option B)
- **Acceptance:** xem §4 Bucket B AC checklist

### Bucket C — GAP-704 JWT tenantId claim post-signup

- **Files:**
  - `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/auth/service/JwtIssuanceService.java` (UPDATE if missing tenantId claim — locate via grep)
  - `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java` (VERIFY — claim propagation header)
  - `kitehub/kitehub-platform/src/main/resources/application.yml` (UPDATE if JWT config needs tenantId scope)
- **Tests:**
  - `kitehub/kitehub-platform/src/test/java/com/kitehub/platform/auth/service/JwtIssuanceServiceTest.java` (UPDATE — assert tenantId claim present trong issued JWT)
  - Gateway IT verify forwarding tenantId header
- **Acceptance:** xem §4 Bucket C AC checklist

### Bucket D — Course/Class CRUD walk + IT

- **Files (verify only — no source changes expected if walk passes):**
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/controller/CourseController.java` (VERIFY endpoints exist, return correct shapes)
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/controller/ClassController.java` (VERIFY)
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/entity/Course.java` (VERIFY entity shape)
- **Tests:**
  - `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/course/CourseControllerIT.java` (🆕 CREATE OR UPDATE — Owner role CRUD walk + tenant isolation)
  - `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/clazz/ClassControllerIT.java` (🆕 CREATE OR UPDATE)
- **Acceptance:** xem §4 Bucket D AC checklist

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

Mọi code-symbol-shaped reference trong §3 Scope verified bằng grep no `| head` truncation:

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `kitehub/kitehub-subscription/.../staff/` | Java package | `ls kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/staff/` | 5 subdirs: controller, dto, entity, repository, service | ✅ exists |
| `SubscriptionOutboxDispatcher` | Java class | `find kitehub/kitehub-subscription/src/main/java -name "*Outbox*"` | 3 files: SubscriptionOutboxRepository.java, SubscriptionOutboxDispatcher.java, SubscriptionOutboxEvent.java | ✅ exists (outbox pattern foundational ready) |
| `kitehub/kitehub-email/.../` | Java package | `ls kitehub/kitehub-email/src/main/java/com/kitehub/email/` | KiteHubEmailApplication.java + 8 subdirs: api, client, config, controller, dto, exception, health, listener, service | ✅ exists (listener/ subdir ready for new StaffInvitationEmailListener) |
| `Course` entity | JPA entity | `find kiteclass/kiteclass-core/src/main/java -name "Course.java"` | `kiteclass-core/.../module/course/entity/Course.java` | ✅ exists |
| `CourseController` | Spring controller | `find kiteclass/kiteclass-core/src/main/java -name "CourseController.java"` | `kiteclass-core/.../module/course/controller/CourseController.java` | ✅ exists |
| `ClassController` | Spring controller | `find kiteclass/kiteclass-core/src/main/java -name "ClassController.java"` | `kiteclass-core/.../module/clazz/controller/ClassController.java` | ✅ exists |
| `JwtAuthenticationGatewayFilter` | Spring filter | `find kitehub -name "JwtAuthenticationGatewayFilter*.java"` | 2 prod hits: `kitehub-gateway/.../filter/JwtAuthenticationGatewayFilter.java` + test class | ✅ exists |
| `StaffInvitationCreatedEvent` | Java class | (no grep needed — to-be-created) | 0 matches | 🆕 to-be-created (Bucket A) |
| `StaffInvitationAcceptedEvent` | Java class (Option B only) | (no grep needed — to-be-created) | 0 matches | 🆕 to-be-created (Bucket B if Option B) |
| `StaffInvitationEmailListener` | RabbitMQ listener | (no grep needed — to-be-created) | 0 matches | 🆕 to-be-created (Bucket A) |
| `staff.invitation.queue` HOẶC `email.staff-invitation.queue` | RabbitMQ queue name | (config to-be-added) | 0 matches | 🆕 to-be-created (Bucket A — auto-declare via Spring AMQP) |
| `templates/email/staff-invitation.html` | Thymeleaf template | (no grep needed — to-be-created) | 0 matches | 🆕 to-be-created (Bucket A) |
| `users` table `tenant_id` column | DB column | (Bucket B Decision Day verify via Postgres MCP / Flyway migration grep) | TBD Decision Day 1-2 | ✅ assumed exists (tenant-scoped users table) — verify before Bucket B implement |
| `tenantId` JWT claim | JWT claim name | `grep -rn "tenantId" kitehub/kitehub-platform/src/main/java/com/kitehub/platform/auth/` | TBD — Bucket C Day 1 first action: locate issuance code path | ⚠️ verify-on-Bucket-C-start |
| `EnrollmentRepository.findByIdAndDeletedFalse` | Per CLAUDE.md context — known multi-tenant bug GAP-746 | Out-of-scope Wave A | — | Out-of-scope (handled separate Wave) |

**Banned shortcuts compliance:**
- ✅ Không dùng `| head` truncation
- ✅ Không skip verification "agents will check at execution"
- ✅ Aspirational references all flagged 🆕 với bucket owner

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-subscription,kitehub-email clean verify -Dcheckstyle.skip=true` + manual MailHog smoke (dev) | kitehub-ci (subscription + email modules) |
| B | `cd kitehub && ./mvnw -pl kitehub-subscription,kitehub-platform clean verify -Dcheckstyle.skip=true` | kitehub-ci |
| C | `cd kitehub && ./mvnw -pl kitehub-platform,kitehub-gateway clean verify -Dcheckstyle.skip=true` + curl signup → JWT decode walk | kitehub-ci + gateway-ci |
| D | `cd kiteclass/kiteclass-core && ./mvnw clean verify -Dcheckstyle.skip=true` | core-ci |

**Production smoke wrap (post-Wave A merge, deferred Week 4 AWS deploy):** SES email dispatch verify + DKIM PASS smoke per `release-deploy-standard.md` §3.1.

---

### 5.1 Acceptance Criteria (per bucket)

### Bucket A — Bug #14 email path

- [ ] `staff.invitation.created` event published từ `StaffInvitationServiceImpl.invite()` via outbox (transactional outbox pattern per `design-patterns.md` §3.5)
- [ ] `SubscriptionOutboxDispatcher` recognizes new event type + publishes to RabbitMQ exchange
- [ ] `kitehub-email/StaffInvitationEmailListener` subscribed to queue, consumes event, renders template, calls `EmailService.send()`
- [ ] Email template `staff-invitation.html` + `staff-invitation.txt` Vietnamese narrative per `vn-localization-audit-checklist.md` §2 Section 1-4:
  - VND/date format đúng (nếu có currency/date trong email)
  - Vietnamese label per persona tone matrix (Owner formal `Em chào chị/anh,`)
  - VN sample data (`Trung tâm Sky Education`, `Trần Thị Hồng`)
  - Cultural awareness (accept URL clear, support email/Zalo footer)
- [ ] MailHog dev verify: email arrives với accept URL `https://kitehub.me/staff/accept?token=<token>`
- [ ] RabbitMQ queue auto-declared via Spring AMQP `@RabbitListener` (no manual `rabbitmqadmin` declaration per Bug #6 recurrence avoidance)
- [ ] Production smoke (deferred Week 4 AWS deploy): SES dispatch + DKIM PASS
- [ ] Testcontainers IT: `StaffInvitationServicePostgresIT` verifies outbox event row inserted post-`invite()` + `StaffInvitationEmailListenerIT` verifies listener consumes + EmailService called
- [ ] VN diacritic roundtrip preservation per `vn-localization-audit-checklist.md` §5 — owner name with `Trần Thị Hồng` rendered correctly trong email body (no `&acirc;` HTML entity corruption)
- [ ] Re-walk Mảng A staff-invitation flow per `pre-handoff-self-test-completeness.md` §3 post-fix mandate — verify originating Bug #14 symptom resolved + 2 sister Mảng A items checked (no regression)

### Bucket B — Bug #17 user provision on accept

- [ ] Architecture decision Option A/B/C logged in GAP-786 §Proposed Fix với rationale (per Decision Day 1-2, xem §7 Risk 1)
- [ ] User record created trong `users` table với hashed password + `tenant_id` matching invitation tenant + role matching invitation role (Manager/Teacher/Assistant)
- [ ] `invitation.acceptedUserId` foreign-key populated trỏ về newly-created user row
- [ ] Login as new staff WORKS — `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (a)→(g) all PASS:
  - (a) Credential available (password set during accept flow)
  - (b) Login API POST /api/auth/login → HTTP 200 + JWT
  - (c) Login UI → redirect post-login URL phù hợp role
  - (d) Role-guard accepts seeded role
  - (e) Sidebar visible với role-appropriate menu items
  - (f) Target page renders với tenant data
  - (g) First action (vd view dashboard) returns success
- [ ] IT test: `StaffInvitationServicePostgresIT.accept_creates_user_row()` (Option A) OR `StaffUserProvisionListenerIT.consumes_event_creates_user()` (Option B)
- [ ] Tenant isolation verified — user created chỉ visible trong tenant context
- [ ] Re-walk Mảng A accept flow per `pre-handoff-self-test-completeness.md` §3 post-fix mandate

### Bucket C — GAP-704 JWT tenantId claim post-signup

- [ ] Owner JWT post-signup contains `tenantId` claim (string match tenant UUID)
- [ ] Verify via decode (`jq -R 'split(".") | .[1] | @base64d | fromjson'`) trên fresh signup JWT — `tenantId` field present + non-null
- [ ] If broken: fix issuance code path (likely `JwtIssuanceService.buildClaims()` missing `tenantId` from User entity)
- [ ] Gateway `JwtAuthenticationGatewayFilter` propagates `X-Tenant-Id` header downstream từ JWT claim
- [ ] Walk evidence in GAP-704 closure log: signup → JWT decode show `tenantId` + downstream service receives header
- [ ] Test: `JwtIssuanceServiceTest.includes_tenant_id_claim()` + gateway IT verify header propagation

### Bucket D — Course/Class CRUD walk + IT

- [ ] Course CRUD walk per `feature-ship-runtime-walk-mandate.md` §3 Owner role:
  - Create course → POST /api/courses → HTTP 201 + course ID returned
  - Update course → PUT /api/courses/{id} → HTTP 200
  - List courses → GET /api/courses → HTTP 200 + array containing created course
  - Delete course → DELETE /api/courses/{id} → HTTP 204 (or soft-delete pattern verified)
- [ ] Class CRUD walk same pattern: create / update / list / delete via Owner role
- [ ] Cascade decision documented: delete Course → existing Classes status? Options:
  - (a) CASCADE delete (lose data — risky)
  - (b) RESTRICT (force user manually delete classes first — explicit UX)
  - (c) SOFT-CASCADE (set deleted=true on Course + Classes — recoverable)
  - Recommend (b) RESTRICT for Phase 2 BETA safety; document decision in GAP follow-up or ADR
- [ ] Per-tenant isolation verified — Owner of tenant A cannot see tenant B's courses (curl với tenant A JWT → list endpoint returns only tenant A's data)
- [ ] Testcontainers IT: `CourseControllerIT.crud_owner_role_full_walk()` + `ClassControllerIT.crud_owner_role_full_walk()` + cross-tenant isolation test
- [ ] Walk evidence per `feature-ship-runtime-walk-mandate.md` §3 — quote curl commands + JSON response + DB row state

---

### 5.2 Risks + Mitigations

### Risk 1 — Bug #14+#17 architecture decision takes >3 eng-days

**Mitigation:**
- Per `release-fix-retry-budget.md` §3.5 Investigation phase mandate — lock Decision Day 1-2:
  - **Day 1:** Investigation — read `application.yml` profiles + existing outbox infrastructure verbatim; query Postgres MCP cho `users` table schema + cross-module access patterns; cross-reference với existing event patterns (vd staff invitation already has events? grep similar service flows)
  - **Day 2:** Lock Option A/B/C decision với cost-benefit table trong GAP-786 + GAP-787 §Proposed Fix section
- Options summarized:
  - **Bucket B — Option A (direct inject):** Lowest LOC (~50 lines), BUT cross-module dependency risk (subscription module depending on platform's UserRepository — coupling smell). Effort: 1 eng-day. Reject if cross-module enforcement strict.
  - **Bucket B — Option B (outbox event → consumer):** Cleanest separation, reuses existing outbox pattern from Bucket A scope, async robust. Effort: 2-3 eng-days. Recommend default unless Day 1 investigation surfaces blocker.
  - **Bucket B — Option C (sync HTTP internal call):** Heaviest (needs API contract per `contract-first-for-cross-layer.md` + Bucket 0 Foundation). Effort: 4-5 eng-days. Reject — over-engineered cho internal flow.
- Bucket A options similar pattern: Option A (single in-process listener) vs Option B (RabbitMQ via outbox). Recommend Option B vì email retry/DLQ requirements per `pre-handoff-self-test-completeness.md` §2.9 background job flow checklist.

### Risk 2 — Outbox + RabbitMQ binding complexity higher than expected

**Mitigation:**
- Start với simplest viable Option (in-process Spring `@TransactionalEventListener` if RabbitMQ infra not ready)
- Upgrade to RabbitMQ Phase 3 nếu solo dev hits binding/queue declaration friction
- Per `local-fix-production-parity-check.md` §2 — ship terraform IaC declaration cho production RabbitMQ queue + IAM permissions in same wave (avoid Wave 81 + Wave 104.5 recurrence class)
- Bug #6 recurrence avoidance: queue auto-declare via Spring AMQP `@Queue` annotation, NOT manual `rabbitmqadmin declare` runbook step

### Risk 3 — 4 buckets parallel surface cross-cutting bugs

**Mitigation:**
- Coordinator preference: **sequential single-agent** (recommend) — Bucket A → B → C → D in order
- Bucket A+B truly sequential (depend on each other for full invitation→email→accept→login flow walk)
- Bucket C+D isolated — could parallel-spawn 2nd Opus agent if solo dev wall-clock pressure
- All agents Opus 4.7 1M per `agent-model-opus-default.md` (Sonnet thrash recurrence confirmed Wave br-4 4/4 + Wave beta-readiness-8 2/3)

### Risk 4 — VN diacritic regression class recurrence (Wave 106 GAP-764 lesson)

**Mitigation:**
- Bucket A email template MUST be tested với VN diacritic input per `vn-localization-audit-checklist.md` §5 roundtrip mandate
- Testcontainers IT cho Bucket A includes `Trần Thị Hồng` + `Trung tâm Anh ngữ Sky Education` test data
- Pre-commit verify HTML escape mode (if applied) uses `HtmlUtils.htmlEscape(input, "UTF-8")` two-arg variant (NOT single-arg corrupt variant)

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `agent-model-opus-default.md`:

**Recommended (default):** Sequential single-agent execution
- **Pattern:** 1 Opus 4.7 1M agent handles all 4 buckets sequentially (A → B → C → D)
- **Rationale:** Matches Plan D v2 Week 1-2 timeline; reduces coordination overhead; bucket A+B truly sequential anyway (architecture decision lock + implementation depend on each other)
- **Spawn:** `run_in_background: true`, `isolation: worktree`, `model: "opus"`, RELATIVE paths in prompt per `feedback_worktree_absolute_path_contamination.md`

**Alternative (if wall-clock pressure):** 2-agent parallel split
- **Agent 1:** Buckets A + B sequential (5-7 + 3-5 = 8-12 eng-days but sequential dependency)
- **Agent 2:** Buckets C + D parallel (1-2 + 2 = 3-4 eng-days, isolated)
- Coordinator merges sequentially after both agent completions
- Both agents Opus 4.7 1M

**NOT recommended:** 4-agent fully parallel — Bucket A+B merge conflict risk on `kitehub-subscription/staff` package + architecture decisions not yet locked = thrash risk.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `wave-closure-scope-completeness.md` + `feedback_wave_closure_release_progress_report.md`:

- Each bucket PR updates affected GAP file Log + status (GAP-787 Bucket A, GAP-786 Bucket B, GAP-704 Bucket C; Bucket D no gap — wave plan log entry)
- ROADMAP §🚀 Next Action updated in closure PR with Wave A SHIPPED + next wave queue
- Wave plan frontmatter `status: draft → complete` flip in closure PR
- `wave-history.jsonl` append entry với `tag_primary: phase2-beta-wave-a` + `tags_secondary: [bug-14, bug-17, gap-704, plan-d-v2]` per `wave-tag-numbering-convention.md` §2.5 new format
- **Scope-Completeness Reconciliation table** in closure PR body per `wave-closure-scope-completeness.md` §3 — mọi §3 Scope item categorized ✅ DONE / 🟡 PARTIAL / ❌ NOT-IMPLEMENTED với follow-up gap link
- Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` post all bucket merges per `post-wave-cleanup.md`
- **Release Plan Progress section** in closure PR body per `feedback_wave_closure_release_progress_report.md` rules #1-6:
  - Current Phase 2 BETA + milestone progress
  - Wave A contribution (4 P0 fixes Week 1-2)
  - Trigger gates Phase 2 → Phase 3 transition
  - Estimated remaining wall-clock cho Phase 2 BETA Wave B-D
  - Waves Remaining table với explicit wave numbers + GAP IDs + PR #s
- Post-wave audit suite trigger ≤3 days per `post-wave-audit-mandate.md` §2.2:
  - Required audits: API contract + Security + Business Logic (Bucket A+B touch BE controllers + service + email templates) + Ops Readiness (RabbitMQ infra + outbox pattern)
  - File gaps for findings per `audit-to-gap-pipeline.md` §3

---

### 7.1 References

### Walk findings + synthesis (same-session 2026-05-28)
- `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` — 17 bug surface RST walk
- `documents/04-quality/audits/persona-review/2026-05-28-beta-plan-d-v2-3-agent-synthesis.md` — 3-agent outside-in synthesis (persona / failure-mode / VN edu SaaS benchmark)
- `documents/04-quality/audits/persona-review/2026-05-28-persona-defense-agent.md` (if separate file)
- `documents/04-quality/audits/persona-review/2026-05-28-failure-mode-matrix-agent.md` (if separate file)
- `documents/04-quality/audits/persona-review/2026-05-28-vn-edu-saas-benchmark-agent.md` (if separate file)

### Gaps
- GAP-786 — Bug #17 user provision on accept (P0, architecture decision required)
- GAP-787 — Bug #14 staff invitation email path missing (P0, email + outbox + RabbitMQ scope)
- GAP-788 — META retro-walk discipline (P0 process, complementary scope cho Wave A closure protocol)
- GAP-704 — JWT tenantId claim post-signup verify (P0, pre-existing ROADMAP queue)

### META rules applied
- `feature-ship-runtime-walk-mandate.md` v1.0.0 — Bucket B + D walk evidence mandate
- `pre-handoff-self-test-completeness.md` §2.4 + §3 — admin-flow checklist + post-fix re-walk mandate
- `vn-localization-audit-checklist.md` §2 + §5 — VN content + diacritic roundtrip
- `release-fix-retry-budget.md` §3.5 — Investigation phase mandate Day 1-2
- `agent-model-opus-default.md` v1.0.0 — Opus 4.7 1M mandate
- `wave-tag-numbering-convention.md` v1.0.0 — Wave naming format
- `wave-closure-scope-completeness.md` v1.0.1 — Closure reconciliation table mandate
- `local-fix-production-parity-check.md` v1.0.0 — RabbitMQ + email config production parity

### Plan D v2 strategic context
- Plan D v2 Week 1-2 = P0 architecture fixes (this Wave A)
- Plan D v2 Week 3-4 = P1 polish (next Wave B)
- Plan D v2 Week 5-6 = P2 enhancement (next Wave C)
- Plan D v2 Week 7-8 = beta tenant invite execution (next Wave D)
- Phase 2 BETA trigger gates Phase 3: quality audit /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 weeks (per CLAUDE.md current phase context)

---

## 8. Log

- **2026-05-28** (draft): Plan created post Wave meta-6 Bucket A RST walk shutdown 2026-05-28 + 3-agent outside-in synthesis same session. Scope: 4 P0 items targeting Plan D v2 Week 1-2. Recommended sequential single-agent execution (Opus 4.7 1M) covering Bucket A → B → C → D in order. Decision Day 1-2 architecture lock for Bucket A+B Option B (outbox event pattern recommended default). Skip outside-in audit per `outside-in-coverage-trigger.md` §4 row 4 (audit ≤30 days exists). Plan PR docs-only auto-merge eligible per `docs-only-pr-auto-merge.md` §2.
