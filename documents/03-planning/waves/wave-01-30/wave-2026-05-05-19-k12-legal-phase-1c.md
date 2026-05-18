---
title: Wave 19 — K-12 LEGAL Trio Phase 1C + Parent Portal Phase 1B Remainder
status: complete
created: 2026-05-05
updated: 2026-05-05
shipped: 2026-05-05
prs: [793, 792, 794, 796]
waves: [19]
gaps: [GAP-322c, GAP-323c, GAP-321c, GAP-321b-1-conduct]
---

# Wave 19 — K-12 LEGAL Trio Phase 1C + Parent Portal Phase 1B Remainder

**Goal:** Ship Phase 1C v1 of K-12 LEGAL trio (322c child protection mandatory reporting + audit-log foundation, 323c SubjectGrade extension + GradeFormulaService backend, 321c PDPL granular consent + 1 write action) plus close Wave 18b3 follow-up GAP-321b-1-conduct (Incident.visibilityScope schema + BR-CHILD-PROTECT-005 + ParentConductFacetServiceImpl real wiring).

**Trigger:** Wave 18b3 SHIPPED 2026-05-04 with 3 K-12 LEGAL Phase 1B gaps PARTIAL + 3 sub-gaps filed. ROADMAP §🚀 Next Action recommended Phase 1C planning post Meta-P0 (GAP-356 SHIPPED PR #788). Wave 19 is first downstream consumer of `audit-to-gap-pipeline.md` v1.2.0 §2.6 wave-plan pre-flight state-check protocol.

**Estimated wall-clock:** ~2-3h agent work, longest-bucket ~60-90min (Bucket B SubjectGrade + GradeFormulaService backend largest).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** P5 K-12 persona — Phase 1A SHIPPED Wave 18b1, Phase 1B SHIPPED Wave 18b2/18b3, Phase 1C is mandate completion (TT 22/2021 grading, Đ.51 mandatory reporting, PDPL granular consent). 4 buckets disjoint at module level (childprotection / k12.attendance / parent.consent / parent.conduct).

**Q2 (trade-offs):** Single wave (4 buckets) vs split 19a (LEGAL P0 trio) + 19b (parent-portal P1). Decision: single wave with **Phase 1C v1 scope** per bucket — defer multi-week features (state machine UI, 4-level escalation, pen test, full 4 write actions, i18n) to Phase 1C remainder follow-up gaps. Pattern matches Wave 18b1/b2/b3 PARTIAL exit-ramp via `gap-done-discipline.md` §3.

**Q3 (risks):**
- (R1) `Incident` entity already encrypted via Phase 1A AES-256; adding fields (`visibilityScope`, `retentionUntil`) must preserve backward compat. **Mitigation:** additive migrations + non-null DEFAULTs.
- (R2) `SubjectGrade` extension + State Pattern coupled — Bucket B may grow. **Mitigation:** scope to entity extension + GradeFormulaService unit-tested; UI/state machine deferred.
- (R3) PDPL granular consent JSONB column shape — schema breaking later if wrong. **Mitigation:** start with simple `{"fields":{}, "version":N, "updatedAt":...}`; iterate via additive migrations.
- (R4) Bucket D needs `Incident.visibilityScope` column AND `BR-CHILD-PROTECT-005` rule — same artifacts Bucket A needs. **Mitigation:** Bucket A creates `IncidentVisibilityScope` enum + V54a migration; Bucket D consumes and adds JPQL wiring. Avoid race via ordering: Bucket A merges first.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| **A** | GAP-322c v1 (mandatory reporting banner + audit-log foundation) | bg-agent-22c | ~60min | ✅ kiteclass-core/.../childprotection/** |
| **B** | GAP-323c v1 (SubjectGrade extension + GradeFormulaService backend) | bg-agent-23c | ~60-90min | ✅ kiteclass-core/.../k12/** |
| **C** | GAP-321c v1 (PDPL granular consent JSONB + ConsentService + 1 write action) | bg-agent-21c | ~45-60min | ✅ kiteclass-core/.../parent/consent + new /complaints scope |
| **D** | GAP-321b-1-conduct (`Incident.visibilityScope` + BR-CHILD-PROTECT-005 + ParentConductFacetServiceImpl real wiring) | bg-agent-21b1c | ~45min | ⚠️ overlap with Bucket A on `Incident` entity → ordering: A merges first |

Disjoint check: Buckets A/B/C/D touch separate top-level modules (childprotection / k12 / parent.consent / parent.conduct). Bucket D consumes Bucket A's `IncidentVisibilityScope` enum — coordinator merges A→D sequentially after both complete.

---

## 3. Scope (per bucket — Phase 1C v1)

### Bucket A — GAP-322c v1: Mandatory reporting banner + audit-log foundation

- **Files (write):**
  - `kiteclass-core/.../childprotection/enums/IncidentVisibilityScope.java` (NEW enum: `PARENT_VISIBLE`, `PUBLIC`, `STAFF_ONLY`, `RESTRICTED`)
  - `kiteclass-core/.../childprotection/entity/ChildProtectionAuditLog.java` (NEW entity)
  - `kiteclass-core/.../childprotection/repository/ChildProtectionAuditLogRepository.java`
  - `kiteclass-core/.../childprotection/service/ChildProtectionAuditService.java` + `Impl` (hash-chain compute + append)
  - `kiteclass-core/.../childprotection/controller/IncidentReportingController.java` (banner-ack endpoint `POST /api/v1/incidents/{id}/mandatory-report-ack`)
  - `kiteclass-core/.../childprotection/listener/IncidentTransitionListener.java` (fire audit log on Incident transition to `severity=CRITICAL` AND `category IN (ABUSE, GROOMING)`)
  - `kiteclass-core/src/main/resources/db/migration/V54__add_incident_visibility_scope_and_audit_log.sql` (adds `visibility_scope` to `incidents` + creates `child_protection_audit_log` table with hash-chain columns)
  - `kiteclass-frontend/.../safeguarding/IncidentBanner.tsx` (mandatory-reporting banner component)
  - `documents/01-business/kiteclass/child-protection/rules.md` (BR-CHILD-PROTECT-005 visibility scope + BR-CHILD-PROTECT-006 mandatory reporting + BR-CHILD-PROTECT-007 audit log)
  - `documents/01-business/kiteclass/child-protection/use-cases.md` (UC-INCIDENT-CRITICAL-REPORT)

- **Tests:**
  - Unit: `ChildProtectionAuditServiceImplTest` (hash-chain compute, append, integrity)
  - IT: `IncidentReportingControllerIT` (banner-ack endpoint + RBAC)
  - FE: `IncidentBanner.test.tsx`

- **AC subset (full AC in GAP-322c):**
  - V54 migration adds `visibility_scope` + `child_protection_audit_log` table backward-compat
  - `IncidentVisibilityScope` enum 4 values
  - `BR-CHILD-PROTECT-005..007` authored with 5-attribute frontmatter per `business-logic-review.md`
  - Banner triggers on CRITICAL+abuse-category transition (IT proves)
  - Hash-chain audit log append-only (admin DELETE denied at DB grant level)
  - `IncidentTransitionListener` fires audit-log entry on transition

- **Out-of-scope (defer to GAP-322c follow-up):** 7-year retention enforcement (V<N+1>), pen test, 4-level complaint escalation, full UC-INCIDENT-CRITICAL-REPORT page UI.

### Bucket B — GAP-323c v1: SubjectGrade extension + GradeFormulaService backend

- **Files (write):**
  - `kiteclass-core/.../k12/entity/SubjectGrade.java` (extend with `type`, `weight`, `status`, `reviewedBy`, `publishedAt`)
  - `kiteclass-core/.../k12/enums/SubjectGradeType.java` (NEW: `TX`, `GK`, `CK`)
  - `kiteclass-core/.../k12/enums/SubjectGradeStatus.java` (NEW: `DRAFT`, `REVIEWED`, `PUBLISHED`)
  - `kiteclass-core/.../k12/service/GradeFormulaService.java` + `Impl` (computeDTBmHK + computeDTBmCN, Strategy Pattern stub)
  - `kiteclass-core/.../k12/repository/SubjectGradeRepository.java` (queries by status)
  - `kiteclass-core/src/main/resources/db/migration/V55__extend_subject_grades_for_tt22.sql` (additive: type/weight/status/reviewed_by/published_at)
  - `documents/01-business/kiteclass/multi-subject-gradebook/rules.md` (NEW — 5-attribute frontmatter; cite TT 22/2021 + TT 32/2018)
  - `documents/01-business/kiteclass/multi-subject-gradebook/use-cases.md` (NEW)
  - `documents/01-business/kiteclass/multi-subject-gradebook/api-contract.md` (NEW)

- **Tests:**
  - Unit: `GradeFormulaServiceImplTest` (≥5 edge cases: zero TX, missing GK, decimal precision HALF_EVEN scale=1, weighted mix, full average)
  - IT: `SubjectGradeRepositoryIT` (status queries return only matching rows)

- **AC subset:**
  - V55 migration backward compat
  - GradeFormulaService implements ĐTBmHK + ĐTBmCN with HALF_EVEN scale=1
  - Strategy Pattern interface (1 default impl, room for TT amendment swap)
  - Business docs `multi-subject-gradebook/` 3-layer with 5-attribute frontmatter
  - Sonar coverage ≥80% on new Service code

- **Out-of-scope (defer to GAP-323c follow-up):** State machine + Tổ trưởng approval workflow (depends GAP-063b notification), multi-subject gradebook UI, học bạ generation hook, bulk publish.

### Bucket C — GAP-321c v1: PDPL granular consent JSONB + ConsentService + 1 write action

- **Files (write):**
  - `kiteclass-core/.../parent/entity/ParentStudentLink.java` (add `parentalConsent` JSONB field via `@JdbcTypeCode(SqlTypes.JSON)` per memory `feedback_jpa_jsonb_jdbctypecode.md`)
  - `kiteclass-core/.../parent/service/ConsentService.java` + `Impl` (checkConsent(parentId, childId, field) — gate facet API; getConsentVersion; bumpConsent)
  - `kiteclass-core/.../parent/controller/ParentConsentController.java` (settings page endpoint `GET/PUT /api/v1/parent/consent`)
  - `kiteclass-core/.../parent/controller/ParentComplaintController.java` (`POST /api/v1/parent/complaints` v1 — write to existing complaint table or queue if GAP-339 not yet)
  - `kiteclass-core/src/main/resources/db/migration/V56__add_parental_consent_to_parent_student_links.sql` (additive JSONB column + default `{"fields":{},"version":1,"updatedAt":null}`)
  - `documents/01-business/kiteclass/parent-portal/rules.md` (BR-PARENT-PORTAL-011 PDPL consent + BR-PARENT-PORTAL-012 consent versioning)

- **Tests:**
  - Unit: `ConsentServiceImplTest` (gate matrix per field per consent state)
  - IT: `ParentConsentControllerIT` (settings GET/PUT + 403 when consent missing)
  - IT: `ParentComplaintControllerIT` (write + scope guard + 403 cross-tenant)

- **AC subset:**
  - V56 migration backward compat
  - `ConsentService.checkConsent()` gates facet APIs (test: SECONDARY parent without consent for `fees` returns 403)
  - PUT settings updates per-field flag + bumps version
  - 1 write action (complaints) wired with scope guard + audit log
  - BR-PARENT-PORTAL-011 + 012 5-attribute frontmatter

- **Out-of-scope (defer to GAP-321c follow-up):** 3 remaining write actions (conduct-confirm, meeting RSVP, absence-excuse upload), i18n EN/zh-CN, settings page UI, re-consent flow on policy bump.

### Bucket D — GAP-321b-1-conduct: ParentConductFacetServiceImpl real wiring

- **Files (write):**
  - `kiteclass-core/.../parent/service/impl/ParentConductFacetServiceImpl.java` (replace v1 stub with JPQL `SELECT i FROM Incident i WHERE i.subjectStudentId = :childId AND i.visibilityScope IN (PARENT_VISIBLE, PUBLIC) AND i.deleted = false` + `@EntityGraph` for nested decryption paths)
  - `kiteclass-core/.../parent/repository/ParentConductFacetRepository.java` (NEW or extend ParentRepository)
  - `kiteclass-core/src/test/java/.../parent/service/impl/ParentConductFacetServiceImplTest.java` (flip `staffOnlyIncidentEquivalent_notExposedToParent` from passing-trivially to passing-against-real-data)
  - `kiteclass-core/src/test/java/.../parent/integration/ParentConductFacetEntityGraphIT.java` (mirror `ParentFeesFacetEntityGraphIT`, asserts `assertSelectCount ≤3`)
  - `documents/01-business/kiteclass/parent-portal/rules.md` §13.4 (flip BR-PARENT-FACET-CONDUCT-002 from stub-stay → real-wiring; cite BR-CHILD-PROTECT-005 from Bucket A)

- **Depends on Bucket A:** consumes `IncidentVisibilityScope` enum + V54 migration + BR-CHILD-PROTECT-005. Coordinator merges A first.

- **Tests:**
  - Flipped `staffOnlyIncidentEquivalent_notExposedToParent` test (real STAFF_ONLY fixture, must not appear)
  - `ParentConductFacetEntityGraphIT` (`assertSelectCount ≤3`)

- **AC subset (full AC in GAP-321b-1-conduct):**
  - `ParentConductFacetServiceImpl` returns real data filtered by visibility scope
  - Flipped regression test passes against real fixture
  - N+1 protection `assertSelectCount ≤3`
  - Sonar coverage ≥80% on changed Service code

- **Out-of-scope:** ADR for hạnh kiểm storage (Incident extension vs new `conduct_record`) — keep Incident extension this wave; ADR can land separately if storage decision revisited.

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Evidence | Verdict |
|--------|------|--------------|----------|---------|
| `Incident` | Java entity | `grep -rn "class Incident\b" kiteclass/kiteclass-core/src/main/java` | 1 match `kiteclass-core/.../childprotection/entity/Incident.java:76` | ✅ exists |
| `IncidentCategory` | Java enum | `grep -rn "enum IncidentCategory" kiteclass/kiteclass-core/src/main/java` | 1 match `.../childprotection/enums/IncidentCategory.java:14` | ✅ exists |
| `Incident.visibilityScope` | Java field | `grep -rn "visibilityScope\|visibility_scope" kiteclass/kiteclass-core/src/main` | 0 matches in src code (only test javadoc as "no" reference) | 🆕 to-be-created (Bucket A) |
| `BR-CHILD-PROTECT-005` | Business rule | `grep -rn "BR-CHILD-PROTECT-005" documents/01-business/kiteclass/child-protection/` | 0 matches | 🆕 to-be-created (Bucket A — consumed by Bucket D) |
| `V54__add_incident_visibility_scope_and_audit_log.sql` | Flyway migration | `ls kiteclass/kiteclass-core/src/main/resources/db/migration/V54*` | 0 matches (last is V53) | 🆕 to-be-created (Bucket A) |
| `SubjectGrade` | Java entity | `grep -rln "class SubjectGrade\b" kiteclass/kiteclass-core/src/main/java` | 1 match `.../k12/entity/SubjectGrade.java` | ✅ exists |
| `GradeFormulaService` | Java service | `grep -rln "GradeFormulaService" kiteclass/kiteclass-core/src/main/java` | 0 matches | 🆕 to-be-created (Bucket B) |
| `V55__extend_subject_grades_for_tt22.sql` | Flyway migration | `ls .../V55*` | 0 matches | 🆕 to-be-created (Bucket B) |
| `ParentStudentLink` | Java entity | `grep -rn "class ParentStudentLink\b" kiteclass/kiteclass-core/src/main/java` | 1 match `.../parent/entity/ParentStudentLink.java:53` | ✅ exists |
| `V42__create_parent_portal_schema.sql` | Flyway migration | `ls .../V42*` | 1 file `V42__create_parent_portal_schema.sql` | ✅ exists |
| `ParentStudentLink.parentalConsent` | Java field | `grep -rn "parental_consent\|parentalConsent" kiteclass/kiteclass-core/src/main` | 0 matches | 🆕 to-be-created (Bucket C) |
| `ConsentService` | Java service | `grep -rln "ConsentService" kiteclass/kiteclass-core/src/main/java` | 0 matches | 🆕 to-be-created (Bucket C) |
| `V56__add_parental_consent_to_parent_student_links.sql` | Flyway migration | `ls .../V56*` | 0 matches | 🆕 to-be-created (Bucket C) |
| `ParentConductFacetServiceImpl` | Java service | `grep -rln "ParentConductFacetServiceImpl" kiteclass/kiteclass-core/src/main/java` | 1 match `.../parent/service/impl/ParentConductFacetServiceImpl.java` | ✅ exists |
| `BR-PARENT-FACET-CONDUCT-002` | Business rule | `grep -rn "BR-PARENT-FACET-CONDUCT-002" documents/01-business/kiteclass/parent-portal/` | known to exist from Wave 18b3 (stub-stay marker) | ✅ exists |

Banned shortcuts verified absent:
- No `| head` truncation on grep/find
- No "agent will check at execution"-style aspirational refs without 🆕 flag
- All forward-looking refs marked `🆕 to-be-created` with owning Bucket

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `./mvnw -pl kiteclass-core clean verify -Dcheckstyle.skip=true` + `pnpm -F kiteclass-frontend test:unit` (for IncidentBanner) | core-ci + frontend-ci |
| B | `./mvnw -pl kiteclass-core clean verify -Dcheckstyle.skip=true` | core-ci |
| C | `./mvnw -pl kiteclass-core clean verify -Dcheckstyle.skip=true` | core-ci |
| D | `./mvnw -pl kiteclass-core clean verify -Dcheckstyle.skip=true` | core-ci |

All buckets: jacoco ≥80% on new code; existing test suites green.

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `feedback_worktree_absolute_path_contamination.md`:

- **All 4 buckets** spawned via Agent tool with:
  - `subagent_type: "general-purpose"`
  - `isolation: "worktree"` (parallel-safe)
  - `run_in_background: true` (NON-NEGOTIABLE — default per `agent-background-spawn-default.md` §1)
  - **RELATIVE paths** in agent prompts (per `feedback_worktree_absolute_path_contamination.md` — agents MUST NOT cd or use absolute repo paths; worktree is `.` to them)
- **Coordinator** (this session):
  - Wait for completion notifications (no polling, no sleep)
  - Order merges sequentially: **A → D → B → C** (D depends on A; B and C are independent of each other and of D)
  - On any agent's CI failure: triage, NOT compound — do not auto-merge; surface to user
- **Per-agent prompt structure** (briefing must be self-contained per Agent tool guidance):
  - Goal: 1 sentence
  - Scope (relative paths only, copy from §3 above)
  - State-Check Evidence row references (so agent knows which symbols already exist)
  - AC subset (copy from §3)
  - Local verify command (copy from §5)
  - Closure protocol: per `gap-done-discipline.md` §3 — flip Status to 🟡 PARTIAL with explicit out-of-scope follow-up gaps for deferred items; do NOT flip to 🟢 DONE
  - Branch + PR title format

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md`:

- **Each bucket PR** updates affected GAP file Log + status (🔵 OPEN → 🟡 PARTIAL with Phase 1C remainder follow-up gaps filed)
- **Each bucket PR** updates `documents/01-business/kiteclass/{domain}/rules.md` + `use-cases.md` per Living Docs rule (CLAUDE.md)
- **Closure PR (after all 4 merge):** flips this wave plan `status: draft` → `complete` + appends `wave-history.jsonl` (Rule 15 enforced) + ROADMAP §🚀 Next Action retro entry
- **Sub-gaps filed** for any deferral; PARTIAL exit-ramp text MUST cite follow-up gap number per `gap-done-discipline.md` §3

---

## 8. Log

- **2026-05-05** (draft): Plan created. First downstream consumer of `audit-to-gap-pipeline.md` v1.2.0 §2.6 wave-plan pre-flight protocol shipped Meta-PR #788. State-Check Evidence section verified 15 symbols (5 ✅ exist, 10 🆕 to-be-created with owning bucket). Coordinator opting for **single wave (4 buckets)** vs split 19a/19b based on disjoint module check + Phase 1C v1 scope-cut; Phase 1C remainder follow-up gaps filed by each bucket per `gap-done-discipline.md` §3.
