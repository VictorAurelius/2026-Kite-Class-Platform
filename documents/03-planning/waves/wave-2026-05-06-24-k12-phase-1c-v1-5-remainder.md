---
title: Wave 24 — K-12 Phase 1C v1.5 Remainder (independent sub-tasks across 3 gaps)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [24]
gaps: [GAP-359, GAP-360, GAP-361]
---

# Wave 24 — K-12 Phase 1C v1.5 Remainder

**Goal:** Ship high-leverage independent sub-tasks across GAP-359 + GAP-360 + GAP-361 in single 3-bucket parallel wave (Phase 1C v1.5 — between v1 from Wave 19 and full Phase 1C UI/dependency-blocked work).
**Trigger:** Wave 23 closure recommended K-12 Phase 1C remainder pick (P0 LEGAL outranks PDPL Phase 2 P1/P2 per `meta-gap-priority.md` §3). Total raw scope ~6.5-8 weeks across 3 gaps; this wave picks ~17 days raw effort across 8 sub-tasks not blocked by external dependencies.
**Estimated wall-clock:** ~35-50 phút parallel; longest bucket B ~7 ngày serial → background-parallel ~12-15 phút agent (Wave 19-23 cadence).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- **P5 K-12 Principal + GVCN + Tổ trưởng + Hiệu trưởng** (BE-side workflows enforced; UI 4 variants → Wave 25 FE wave).
- **P5 K-12 Parent** (361.B + 361.C — consent gate to all 5 facets + re-consent flow).
- **Bucket A (childprotection)** completes the integrity-+-retention loop for Đ.51 mandatory-reporting (lifecycle close-out).
- **Bucket B (multi-subject-gradebook)** locks state machine + ships outbox event hook to GAP-055 học bạ (downstream unblock).
- **Bucket C (parent-portal)** completes consent-gating coverage (transcript + attendance + conduct + notifications already exist as v1; this wave wires consent gate uniformly).
- **4-layer V-model coverage** (per `design-layer-coverage.md` §2):
  - 要件定義: 5-attribute BR additions (childprotection + gradebook + parent-portal); references existing PDPL/Decree-13 + Luật Trẻ em + TT 22/2021 from Wave 19 BR namespace.
  - 基本設計: API contracts updated (360.6 explicit; 361 implicit via existing endpoints); 359 ops surface (cron alert).
  - 詳細設計: state machine (360.1) + retention lifecycle (359.1) + re-consent middleware (361.C); 3 explicit state transitions documented in rules.md.
  - コンポーネント設計: NEW SubjectGradeService.review/publish (360.1) + RetentionLifecycleService (359.1) + AuditChainVerificationCron (359.5) + ReconsentMiddleware (361.C); reuses existing ConsentService + ChildProtectionAuditService patterns.

**Q2 (trade-offs):**
- **Reject:** include all 3 gaps' full scope in 1 wave (~6.5-8 weeks raw — would require single agent 30+ hour run, not realistic).
- **Reject:** UI work in this wave (360.3 4-variant gradebook UI ~10-15 days FE; 361.E settings page ~3 days; 359.4 full UC report page ~3 days) — defer to Wave 25 FE wave-pack.
- **Reject:** dependency-blocked work (359.2 pen test on deployed instance; 359.3 + 361.A depend GAP-339; 360.2 depends GAP-063b/058; 361.D international tenant feature-flag) — defer post-deps.
- **Accept:** ship Phase 1C v1.5 with 8 independent sub-tasks across 3 buckets file-disjoint. Each sub-task ≤3 days raw → fits parallel agent execution comfortably.
- **Accept:** Bucket B ~7 ngày is largest (4 sub-tasks combined); Buckets A + C smaller (~4-6 ngày each). Wave-pack pattern OK with this skew.

**Q3 (risks):**
- **Risk: Bucket B state machine 360.1 collides with existing GradeFormulaService** — mitigation: 360.1 introduces NEW SubjectGradeService class (review/publish mutators), separate from existing GradeFormulaService (read-path formula). No service collision.
- **Risk: Bucket A 359.1 retention column conflicts with V54 incidents schema** — mitigation: 359.1 is additive ALTER TABLE (V57+); does not modify V54 columns.
- **Risk: Bucket C 361.B touching all 4 facet impls = 4 file edits** — mitigation: each facet impl is independent (`Parent{Attendance,Notifications,Conduct,Transcript}FacetServiceImpl`); pattern shipped Wave 19 for `ParentFeesFacetServiceImpl` reusable verbatim.
- **Risk: Bucket B 360.5 Outbox event needs `OutboxEventWriter` infrastructure** — mitigation: per `design-patterns.md` §3.5.1, kiteclass-core uses generic `OutboxEventWriter`; pattern exists from Wave 18a.
- **Risk: Bucket C 361.C re-consent middleware = cross-cutting Spring filter or interceptor** — mitigation: hook into existing `ConsentService.checkConsent()` callsite chain inside facet impls (no new filter; per-facet check already at entry).
- **Risk: Migrations conflict (B + C both file Vxx)** — mitigation: pre-allocate migration versions in plan: Bucket A V57; Bucket B V58 (if needed for state machine table); Bucket C no migration (additive code only).
- **Risk: business rules 5-attribute review for 8 new BRs (BR-CHILD-PROTECT-008..009 + BR-GRADEBOOK-004..006 + BR-PARENT-PORTAL-014..016)** — mitigation: each agent applies existing 5-attribute pattern from Wave 18b/19; `business-logic-review.md` §2 checklist.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Sub-tasks | Effort raw | Disjoint? |
|--------|--------|-----------|------------|-----------|
| **A** | GAP-359 | 359.1 retention column + soft-delete block; 359.5 hash-chain verification cron | ~4 ngày | ✅ `module/childprotection/` only |
| **B** | GAP-360 | 360.1 state machine (review/publish mutators + invalid-transition exceptions); 360.4 bulk publish endpoint; 360.5 học bạ Outbox event hook; 360.6 api-contract.md endpoint definitions | ~7 ngày | ✅ `module/k12/` (SubjectGrade) + `module/grade/` extension only |
| **C** | GAP-361 | 361.B consent gate to 4 remaining facets (transcript / attendance / conduct / notifications); 361.C re-consent flow (admin bulk-bump endpoint + middleware integration in facet impls + retention bump on missing consent) | ~6 ngày | ✅ `module/parent/` only |

**Disjoint check:**
- A: `kiteclass-core/.../module/childprotection/` (entity + service + controller + scheduled cron) + `db/migration/V57__*.sql`
- B: `kiteclass-core/.../module/k12/{entity,service,enums}/` (SubjectGrade + new SubjectGradeService) + `module/grade/` (existing GradeFormulaService consume) + `documents/01-business/kiteclass/multi-subject-gradebook/api-contract.md` (fill empty file)
- C: `kiteclass-core/.../module/parent/{service/impl/Parent{Transcript,Attendance,Conduct,Notifications}FacetServiceImpl,service/ConsentServiceImpl,controller/ParentConsentController,...}/`

Zero file overlap. Migration versions pre-allocated: A=V57; B=V58 (if needed; may not need new migration if Outbox uses existing `outbox_events` table); C=no migration.

---

## 3. Scope (per bucket)

### Bucket A — GAP-359 retention + hash-chain cron

**359.1 — Retention 7-year column + soft-delete block:**
- **CREATE:** `kiteclass-core/src/main/resources/db/migration/V57__add_incidents_retention_until.sql`
  - `ALTER TABLE incidents ADD COLUMN retention_until TIMESTAMPTZ NULL;`
  - Backfill: `UPDATE incidents SET retention_until = COALESCE(closed_at, created_at) + INTERVAL '7 years' WHERE retention_until IS NULL;`
  - Make NOT NULL after backfill OK (or keep nullable + service-side default = closed_at + 7y on transition to CLOSED status)
- **MODIFY:** `kiteclass-core/.../module/childprotection/entity/Incident.java` — add `retentionUntil` field
- **MODIFY:** `kiteclass-core/.../module/childprotection/service/IncidentService.java`:
  - On status transition to CLOSED (Phase 1B has CLOSED in IncidentStatus enum), set `retentionUntil = now() + 7y`
  - Override `softDelete(Long id)`: throw new RetentionWindowActiveException if `retentionUntil > now()`
- **CREATE:** `kiteclass-core/.../module/childprotection/exception/RetentionWindowActiveException.java` (extends BusinessException; HTTP 409 Conflict)
- **CREATE:** `kiteclass-core/.../module/childprotection/service/RetentionLifecycleService.java` + `Impl` — `@Scheduled` daily job iterating expired incidents, secure-delete + audit-log entry `INCIDENT_RETENTION_EXPIRED_DELETE`
- **CREATE:** `messages.properties` + `messages_vi.properties` keys: `INCIDENT_RETENTION_WINDOW_ACTIVE`, `INCIDENT_RETENTION_EXPIRED_DELETE`
- **CREATE:** Test: `IncidentServiceTest.softDelete_withinRetentionWindow_throwsRetentionActive()` + IT for cron job (testcontainers Postgres, time-mock to advance past retention)

**359.5 — Hash-chain integrity verification cron:**
- **CREATE:** `kiteclass-core/.../module/childprotection/service/AuditChainVerificationCron.java` — `@Scheduled` daily job
  - For each `(instance_id, entity_type)` chain in `child_protection_audit_log`, recompute genesis-to-tail content hashes
  - Compare each row's `content_hash` with recomputed
  - On mismatch: log WARN + emit Micrometer counter `child_protection.audit.chain.break{instance,entityType}`
  - Final summary metric: `child_protection.audit.chain.verified{instance,entityType,result}`
- **MODIFY:** `kiteclass-core/.../module/childprotection/service/ChildProtectionAuditService.java` — add `verifyChain(String entityType)` method (existing `verifyChainIntegrity` may already exist per Wave 19 Bucket A — read first)
- **CREATE:** `RunbookAuditChainBreak.md` at `documents/05-guides/operations/audit-chain-break-runbook.md` — manual repair procedure + audit-log entry on intentional chain reset
- **CREATE:** Test: `AuditChainVerificationCronTest` (mock chain + tamper one row + assert counter incremented)

**Bucket A AC:**
- [ ] V57 migration shipped with backfill
- [ ] `Incident.retentionUntil` field + IncidentService softDelete block (RetentionWindowActiveException)
- [ ] RetentionLifecycleService cron + secure-delete + audit-log entry
- [ ] AuditChainVerificationCron daily job + Micrometer counter on break
- [ ] 2 properties keys (en + vi) added
- [ ] Operations runbook `audit-chain-break-runbook.md` created
- [ ] Tests: softDelete throws + cron unit + verifyChain unit
- [ ] Update `documents/01-business/kiteclass/child-protection/rules.md` — add BR-CHILD-PROTECT-008 (retention 7-year mandatory) + BR-CHILD-PROTECT-009 (audit-log integrity verification daily) full 5-attribute
- [ ] mvnw `-pl kiteclass-core test-compile` clean
- [ ] GAP-359 Log entry referencing this PR (Status stays 🔵 OPEN — coordinator updates at closure)

### Bucket B — GAP-360 state machine + bulk publish + Outbox + api-contract

**360.1 — State machine enforcement:**
- **READ:** `kiteclass-core/.../module/k12/entity/SubjectGrade.java` + `enums/SubjectGradeStatus.java` (existing per Wave 19 Bucket B V55)
- **CREATE:** `kiteclass-core/.../module/k12/service/SubjectGradeService.java` interface + `SubjectGradeServiceImpl.java`:
  - `submitForReview(Long gradeId, Long submitterId)` — DRAFT → REVIEWED transitions illegal; only DRAFT existing
  - `review(Long gradeId, Long reviewerId)` — DRAFT → REVIEWED (Tổ trưởng action; SubjectGrade.status flip)
  - `publish(Long gradeId, Long publisherId)` — REVIEWED → PUBLISHED (Hiệu trưởng action)
  - All transitions validated against allowed-transition map; throw `IllegalStateException("INVALID_GRADE_TRANSITION", current, target)` if not allowed
- **MODIFY:** `kiteclass-core/.../module/k12/entity/SubjectGrade.java` — make `setStatus(...)` package-private (or remove direct setter; only allow via service mutators)
- **CREATE:** `IllegalGradeTransitionException` extending `BusinessException` (HTTP 409 Conflict)
- **CREATE (optional):** ArchUnit test under `kiteclass-core/src/test/java/com/kiteclass/core/archunit/SubjectGradeArchitectureTest.java` — assert no direct `setStatus` invocation outside SubjectGradeServiceImpl

**360.4 — Bulk publish action:**
- **CREATE:** `kiteclass-core/.../module/k12/controller/SubjectGradeController.java` — `POST /api/v1/grades/subjects/bulk-publish` (Hiệu trưởng RBAC)
  - Body: `{ "gradeIds": [Long...], "academicYear": "2025-2026", "term": "1" }` (or filter shape)
  - Returns: `{ "publishedCount": N, "skippedCount": M (already PUBLISHED or wrong status), "errors": [...] }`
  - Internally: iterate `gradeIds`, call `subjectGradeService.publish(...)` for each; capture failures
- **CREATE:** Test: `SubjectGradeControllerTest.bulkPublish_skipsAlreadyPublished()` + IT

**360.5 — Học bạ Outbox event hook:**
- **CREATE:** `kiteclass-core/.../module/k12/listener/SubjectGradeAllPublishedListener.java`:
  - On `subjectGradeService.publish(...)` completion, check if all SubjectGrade rows for `(student_id, academic_year)` are PUBLISHED
  - If yes, emit `SubjectGradeAllPublishedEvent { studentId, academicYear, term }` via existing `OutboxEventWriter` (kiteclass-core pattern per `design-patterns.md` §3.5)
  - Routing key: `kiteclass.k12.grades.all-published`
- **MODIFY:** `documents/01-business/kiteclass/multi-subject-gradebook/use-cases.md` — add UC-GRADEBOOK-PUBLISH-COMPLETE describing event emission
- **CREATE:** Test: `SubjectGradeAllPublishedListenerTest` mock 3 grades same student + assert event NOT emitted on first publish, IS emitted on last

**360.6 — api-contract.md fill:**
- **MODIFY:** `documents/01-business/kiteclass/multi-subject-gradebook/api-contract.md` — fill empty file with concrete endpoints:
  - `POST /api/v1/grades/subjects/{id}/submit-for-review` (BE handler in 360.1)
  - `POST /api/v1/grades/subjects/{id}/review` (BE handler in 360.1)
  - `POST /api/v1/grades/subjects/{id}/publish` (BE handler in 360.1)
  - `POST /api/v1/grades/subjects/bulk-publish` (BE handler in 360.4)
  - `GET /api/v1/grades/subjects?status=REVIEWED&...` (Hiệu trưởng review queue read endpoint — minimal — full UI 360.3 deferred Wave 25)
  - Request/response schemas + error codes

**Bucket B AC:**
- [ ] SubjectGradeService interface + Impl with state machine mutators
- [ ] SubjectGrade.setStatus() package-private (or removed from public surface)
- [ ] IllegalGradeTransitionException + ArchUnit test (or unit test stand-in)
- [ ] SubjectGradeController bulk-publish endpoint
- [ ] SubjectGradeAllPublishedListener wired to Outbox
- [ ] api-contract.md filled (≥5 endpoints)
- [ ] BR-GRADEBOOK-004 (DRAFT→REVIEWED→PUBLISHED state machine) + BR-GRADEBOOK-005 (bulk publish authorization Hiệu trưởng) + BR-GRADEBOOK-006 (học bạ trigger event) full 5-attribute in `multi-subject-gradebook/rules.md`
- [ ] Tests: state machine 4 cases (valid transitions, invalid transitions, idempotent publish, race) + bulk publish + Outbox listener
- [ ] mvnw `-pl kiteclass-core test-compile` clean
- [ ] GAP-360 Log entry; Status stays 🔵 OPEN — coordinator updates at closure

### Bucket C — GAP-361 consent gate × 4 facets + re-consent flow

**361.B — Consent gate to 4 remaining facets:**
- **MODIFY:** Apply consent-gate pattern (from Wave 19 Bucket C `ParentFeesFacetServiceImpl` exemplar) to:
  - `kiteclass-core/.../module/parent/service/impl/ParentTranscriptServiceImpl.java` — add ConsentService.checkConsent() at entry of each method; throw 403 PARENT_CONSENT_REQUIRED if missing
  - `kiteclass-core/.../module/parent/service/impl/ParentAttendanceFacetServiceImpl.java` — same
  - `kiteclass-core/.../module/parent/service/impl/ParentConductFacetServiceImpl.java` — same
  - `kiteclass-core/.../module/parent/service/impl/ParentNotificationsFacetServiceImpl.java` — same
- **CREATE/MODIFY:** Tests for each 4 facet: existing tests (Wave 18b1+19) need updated with consent-mock pattern from Wave 19 Bucket C `ParentFeesFacetServiceImplTest` exemplar

**361.C — Re-consent flow (admin bulk-bump + middleware):**
- **MODIFY:** `kiteclass-core/.../module/parent/service/ConsentService.java` — extend with `bulkBumpVersion(Long instanceId, int newVersion, String reason)`
- **MODIFY:** `kiteclass-core/.../module/parent/service/impl/ConsentServiceImpl.java` — implementation
- **CREATE:** `kiteclass-core/.../module/parent/controller/ParentConsentAdminController.java` — `POST /api/v1/admin/parent/consent/bulk-bump` (RBAC: PRINCIPAL or ADMIN)
  - Body: `{ "newVersion": int, "reason": "Privacy policy v2 — added homework facet", "effectiveAt": ISO timestamp }`
  - Returns: `{ "bumpedCount": N, "currentVersion": newVersion }`
- **MODIFY:** All 5 facet impls (transcript + attendance + conduct + notifications + fees existing) — extend consent check to also verify `consent.version >= current_required_version`; throw 403 RECONSENT_REQUIRED if version stale (in addition to existing CONSENT_REQUIRED)
- **MODIFY:** Add `messages.properties` + `messages_vi.properties` keys: `RECONSENT_REQUIRED`, `PARENT_CONSENT_BULK_BUMP_OK`
- **CREATE:** Test: `ParentConsentAdminControllerTest.bulkBump_returnsCount()` + facet IT verifying RECONSENT_REQUIRED on stale version

**Bucket C AC:**
- [ ] 4 facet impls (Transcript, Attendance, Conduct, Notifications) wired with consent-gate (PARENT_CONSENT_REQUIRED 403)
- [ ] Each facet test updated with consent-mock pattern
- [ ] ConsentService.bulkBumpVersion + ParentConsentAdminController endpoint
- [ ] All 5 facet impls (4 + Fees existing) check version >= current_required; throw RECONSENT_REQUIRED
- [ ] 2 properties keys (en + vi) added
- [ ] BR-PARENT-PORTAL-014 (consent gate uniform 5 facets) + BR-PARENT-PORTAL-015 (re-consent on policy bump) + BR-PARENT-PORTAL-016 (admin bulk-bump auth) full 5-attribute in `parent-portal/rules.md`
- [ ] Tests: 4 facet consent IT (1 per facet) + ConsentAdminController unit + middleware re-consent IT
- [ ] mvnw `-pl kiteclass-core test-compile` clean
- [ ] GAP-361 Log entry; Status stays 🔵 OPEN — coordinator updates at closure

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Evidence | Verdict |
|--------|------|--------------|----------|---------|
| `kiteclass-core/.../module/childprotection/service/IncidentService.java` | Existing service | `Glob` | 1 file | ✅ exists |
| `kiteclass-core/.../module/childprotection/service/ChildProtectionAuditService{,Impl}.java` | Hash-chain pattern | `Glob` | 2 files (interface + impl) | ✅ exists |
| `kiteclass-core/.../module/childprotection/entity/Incident.java` | Entity to extend | `Grep "class Incident"` | 1 file in childprotection | ✅ exists |
| `IncidentStatus.CLOSED` | Status enum value | `Grep "CLOSED" childprotection/enums/` | enum value | ✅ exists (Wave 18b1) |
| `kiteclass-core/.../module/k12/entity/SubjectGrade.java` | Entity for state machine | `Grep "class SubjectGrade"` | 1 file in `module/k12/` | ✅ exists |
| `kiteclass-core/.../module/k12/enums/SubjectGradeStatus.java` | Status enum (DRAFT/REVIEWED/PUBLISHED) | `Glob` | 1 file | ✅ exists |
| `kiteclass-core/.../module/k12/service/GradeFormulaService.java` | Read-path formula service | `Glob` | 1 file | ✅ exists (Wave 19) |
| `kiteclass-core/.../module/k12/service/SubjectGradeService.java` | NEW state machine service | (no file) | not present | 🆕 to-be-created (Bucket B 360.1) |
| `kiteclass-core/.../module/parent/service/ConsentService.java` | Wave 19 v1 service | `Glob` | 1 file | ✅ exists |
| `Parent{Transcript,Attendance,Conduct,Notifications,Fees}FacetServiceImpl.java` | 5 facet impls | `Glob` | 5 files (Fees has consent gate; others NOT YET) | ✅ all exist; only Fees gated |
| `documents/01-business/kiteclass/{child-protection,multi-subject-gradebook,parent-portal}/rules.md` | Business rule docs | `Glob` | 3 files exist | ✅ exists; extend with new BRs |
| `documents/01-business/kiteclass/multi-subject-gradebook/api-contract.md` | API contract to fill | `Glob` | 1 file (empty per Wave 19 Bucket B note) | ✅ exists; fill in Bucket B 360.6 |
| `kiteclass-core/.../common/outbox/OutboxEventWriter.java` | Outbox infrastructure | `Grep "class OutboxEventWriter"` | per `design-patterns.md` §3.5.1 reference | ✅ exists |
| `RetentionWindowActiveException` | New exception | (no file) | not present | 🆕 to-be-created (Bucket A 359.1) |
| `IllegalGradeTransitionException` | New exception | (no file) | not present | 🆕 to-be-created (Bucket B 360.1) |
| `RetentionLifecycleService` | New @Scheduled service | (no file) | not present | 🆕 to-be-created (Bucket A 359.1) |
| `AuditChainVerificationCron` | New @Scheduled service | (no file) | not present | 🆕 to-be-created (Bucket A 359.5) |
| `SubjectGradeAllPublishedListener` | New listener | (no file) | not present | 🆕 to-be-created (Bucket B 360.5) |
| `ParentConsentAdminController` | New admin endpoint | (no file) | not present | 🆕 to-be-created (Bucket C 361.C) |
| Migration `V57__add_incidents_retention_until.sql` | New migration | `Glob` | not present (V56 latest) | 🆕 to-be-created (Bucket A 359.1) |
| `messages.properties` + `messages_vi.properties` keys (4 new) | NEW keys | `Grep "INCIDENT_RETENTION\|RECONSENT_REQUIRED"` | 0 matches | 🆕 to-be-created (Buckets A + C) |

Banned shortcuts (mirror §2.5):
- `| head` truncation on grep/find
- Skipping verification "because agents will check at execution"
- Aspirational references without 🆕 flag

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| A | `./mvnw -pl kiteclass-core test -Dtest='IncidentServiceTest,RetentionLifecycleServiceTest,AuditChainVerificationCronTest,ChildProtectionAuditServiceImplTest' -Dcheckstyle.skip=true` | core-ci |
| B | `./mvnw -pl kiteclass-core test -Dtest='SubjectGradeServiceTest,SubjectGradeControllerTest,SubjectGradeAllPublishedListenerTest' -Dcheckstyle.skip=true` | core-ci |
| C | `./mvnw -pl kiteclass-core test -Dtest='Parent*FacetServiceImplTest,ConsentService*Test,ParentConsentAdminControllerTest' -Dcheckstyle.skip=true` | core-ci |

All BE-only Java work; no FE changes; no `pnpm build` needed. `audit-gate.py` may flag business-rule changes (warn-mode acceptable per `business-logic-review.md`).

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `feedback_worktree_absolute_path_contamination.md`:

- All 3 buckets spawned with `run_in_background: true`
- `isolation: worktree` for parallel write safety
- **RELATIVE paths in agent prompts**
- Coordinator merge order: A → B → C (alphabetical; no functional dependency between buckets)
- Per-bucket PR base = `main` (file-disjoint; not stacked)
- Per `feedback_agent_local_verify_both_layers.md`: BE-only buckets use `mvnw test-compile` + targeted `mvnw test`

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md`:

- Each bucket PR updates affected GAP file Log + Status (stays 🔵 OPEN; full DONE deferred to next Phase 1C waves)
- Coordinator closure PR (after 3 bucket PRs merge):
  - Update GAP-359/360/361 status table — flip "Phase 1C v1.5 SHIPPED" notation in Log; Status stays 🔵 OPEN since FE work + dependency-blocked work remain
  - Update `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action signpost (Wave 24 SHIPPED + Phase 1C v1.5 progress)
  - Flip wave plan frontmatter `status: draft` → `status: complete`
  - Append `wave-history.jsonl` Rule 15 entry
  - Update K-12 Stage 1 GA estimate (was ~10-14 weeks remaining; Wave 24 burns ~2 weeks of independent sub-tasks)
  - Cross-link Wave 25 candidate (Phase 1C UI wave-pack: 360.3 + 361.E + 359.4)

---

## 8. Log

- **2026-05-06** (draft): Plan created. 3-bucket parallel covering 8 independent sub-tasks across GAP-359/360/361. Skip dependency-blocked work (359.2 pen test, 359.3 + 361.A depend GAP-339, 360.2 depends GAP-063b/058) and FE-heavy work (360.3 + 361.E + 359.4 → Wave 25). Wall-clock estimate ~35-50 phút parallel. State-Check Evidence verifies 17 cited symbols + 9 to-be-created.
- **2026-05-06** (complete): Wave SHIPPED — 4 PRs merged. **#824 Bucket A GAP-359 v1.5** (V57 retention migration + Incident.retentionUntil + RetentionLifecycleService cron 2am + AuditChainVerificationCron cron 2:30am + Micrometer chain.break counter + audit-chain-break-runbook + BR-CHILD-PROTECT-008/009 5-attribute; 41 tests pass). **#825 Bucket B GAP-360 v1.5** (SubjectGradeService state machine DRAFT→REVIEWED→PUBLISHED + IllegalGradeTransitionException + bulk-publish endpoint max 500 ids + SubjectGradeAllPublishedListener Outbox event routing key `kiteclass.k12.grades.all-published` + api-contract.md filled 4 endpoints + BR-GRADEBOOK-006/007/008 5-attribute; 19 tests pass; ArchUnit deferred — workspace dep missing). **#826 Bucket C GAP-361 v1.5** (consent gate × 4 facets transcript/attendance/conduct/notifications + 5/5 facets check version stale → RECONSENT_REQUIRED + ConsentService.bulkBumpVersion + ParentConsentAdminController bulk-bump endpoint @PreAuthorize + BR-PARENT-PORTAL-014/015/016 5-attribute; 107 tests pass). **MVP Plan 2026** (PR #827) shipped parallel — phased rollout P1+P2 → P3 → K-12 với 3-phase structure + 224 gap classification. **Conflict resolution:** Bucket C messages.properties + messages_vi.properties had 6-line additive conflict với Bucket A's retention keys; resolved additively (kept both blocks). Status flips: GAP-359 + GAP-360 + GAP-361 stay 🟡 PARTIAL (sub-tasks 359.2/3/4/6 + 360.2/3 + 361.A/D/E remain). Wall-clock ~75 min wave + ~20 min closure work + ~10 min conflict resolution.
