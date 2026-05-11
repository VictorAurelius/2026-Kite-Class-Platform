# GAP-321: Parent Portal v1 — LEGAL MANDATE (Luật Giáo dục 2019 Đ.83)

**Status:** 🟡 PARTIAL — Phase 1 (GAP-052a identity + invitation MVP) SHIPPED Wave 2; Phase 2 K-12 LEGAL scope (this gap) NOT started
**Priority:** 🔴 P0 LEGAL
**Domain:** Backend + Frontend + Compliance
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 K-12 persona review)
**Revised:** 2026-05-04 (per GAP-345 state-check audit — initial filing mis-classified as fully greenfield)
**Related PRs:** Wave 2 GAP-052a (parent identity + invitation MVP)
**Related Docs:**
- `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` Finding 1
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-COMM-001..005, AC-OPS-009
- `documents/00-brd/persona-criteria/secondary/parent-in-P5.md` (84 legal citations)
- Existing GAP-052 (parent portal umbrella) — GAP-052a Phase 1 shipped Wave 2; this gap is K-12 LEGAL build-on
- GAP-345 (state-check audit revising this gap)

## Current State (verified 2026-05-04 per GAP-345)

### ✅ SHIPPED Wave 2 (GAP-052a Phase 1)

| Piece | File / Path | Notes |
|-------|-------------|-------|
| `Parent` entity | `kiteclass-core/module/parent/entity/Parent.java` | email + phone + name + relationship + status; since 2.14.0 |
| `ParentStudentLink` (M-to-M with metadata) | `kiteclass-core/module/parent/entity/ParentStudentLink.java` | PRIMARY/SECONDARY linkType; UK constraint prevents duplicate edges |
| `ParentInvitation` token-based onboarding | `kiteclass-core/module/parent/entity/ParentInvitation.java` | 24h TTL; PENDING/REDEEMED/EXPIRED/REVOKED |
| `ParentInvitationService` | `kiteclass-core/module/parent/service/impl/ParentInvitationServiceImpl.java` | invitation creation + redemption logic |
| `ParentService` | `kiteclass-core/module/parent/service/ParentService.java` | CRUD on Parent profile |
| Migration | `kiteclass-core/db/migration/V42__create_parent_portal_schema.sql` | 3 tables: parents, parent_student_links, parent_invitations; multi-tenant (instance_id), audit, soft-delete, optimistic-lock |
| Sibling dedup | V42 `uk_parent_student UNIQUE (parent_id, student_id)` | per-link metadata preserved |
| Gateway PARENT user type | `Gateway UserType.PARENT` | identity links via `users.reference_id = parents.id` |
| `ParentPortalConfiguration` + `ParentPortalProperties` | `kiteclass-core/module/parent/config/` | Spring config exists |

V42 migration comment: "Messaging, fee payment, attendance / grade widgets follow in Wave 5 — this migration is deliberately minimal." → This gap (GAP-321) IS that follow-on, scoped specifically for K-12 LEGAL mandate.

### ⚠️ Phase 1A in-flight discovery (2026-05-04 Wave 18b1)

GAP-345 state-check missed an existing Wave 2 FE skeleton: `kiteclass/kiteclass-frontend/src/app/(dashboard)/parent/page.tsx` (3.14.0 — Wave 2 GAP-052a, 159 LOC) renders a basic children list using inline `apiClient` fetch (no React Query, no transcript drill-in). Phase 1A REPLACES this skeleton with a hooks-based version + adds the `/parent/transcript/[childId]` route. Anti-pattern recurrence: 4th-time `head`-truncation issue per `feedback_audit_grep_scope.md` (state-check grep would catch if `find … -iname "parent*"` walked all `(dashboard)/` subpaths).

### ❌ MISSING (this gap's actual scope)

| Piece | Status |
|-------|--------|
| Parent dashboard FE (`/parent` route in `kiteclass-frontend`) | ⚠️ Wave 2 skeleton existed; Phase 1A REPLACES with hooks-based version |
| Multi-children selector | ❌ |
| 6 facet drill-down pages: transcript / điểm danh / học phí / hạnh kiểm / notifications / kỷ luật | ❌ |
| Bulk import xlsx with `Tên Cha, SĐT Cha, Email Cha, Tên Mẹ, SĐT Mẹ, Email Mẹ` columns | ❌ depends GAP-325 |
| Zalo OTP login flow | ❌ Gateway uses email password currently |
| Per-read audit log (parent-side data view audit trail) | ❌ BaseEntity audit exists for writes but no parent-read log |
| PDPL Decree 13 Art 16 children-data parental consent flag tracking | ❌ |
| Phase 2 — Write actions (complaints GAP-339, RSVP GAP-338, absence excuse) | ❌ |
| Phase 3 — Multi-channel notification | ❌ depends GAP-063 |

**Grep + verification commands run 2026-05-04:**
```bash
grep -rl "parent_portal\|parentPortal\|ParentPortal\|guardian" kiteclass/ --include="*.java" --include="*.tsx"
# → 10+ matches confirming Wave 2 GAP-052a shipped
ls kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/
# → 8 packages: config/controller/dto/entity/event/repository/service
ls kiteclass/kiteclass-core/src/main/resources/db/migration/ | grep -i parent
# → V42__create_parent_portal_schema.sql (111 LOC)
find kiteclass/kiteclass-frontend/src/app -type d -iname "parent*"
# → 0 matches — FE parent-portal greenfield
```
Result: Backend Phase 1 SHIPPED Wave 2 GAP-052a. Frontend greenfield. K-12 LEGAL scope (Phase 2-4) per this gap below.

## Problem

K-12 schools (P5 persona) cannot deploy without a parent portal. Luật Giáo dục 2019 Điều 83 Khoản 2 grants parents the **legal right** to view full information about their child's learning + behavior. Without this:

1. Schools deploying KiteClass for K-12 violate Vietnamese education law.
2. Parents can sue schools (and platform as data processor).
3. 6 P5 tenant ACs (AC-COMM-001..005, AC-OPS-009) + ~26 secondary parent ACs are unsatisfiable.
4. Daily operations break: GVCN cannot publish điểm số, conduct, attendance to parents.
5. Emergency communication (school closure, child safety) cannot reach 1500+ parents.

This gap **consolidates** the K-12-specific parent portal scope on top of the original GAP-052 (which was generic).

## Context

P5 K-12 review (Round 1) scored 0/6 ACs in Communication category — every AC depends on parent portal existing. Persona simulation: Trường THCS 1200 HS / 1800 PH — without portal, kế toán + GVCN + Văn thư manually phone-call PH for any update. Scale (1800 × multi-events) makes this infeasible.

**Cross-cuts:**
- Child protection (GAP-322) needs parent portal as report intake channel
- MOET financial reporting (GAP-336) for transparency — parent should see học phí breakdown
- Phổ cập escalation (GAP-341) needs parent contact channel before Phòng GD escalation
- Complaint workflow (GAP-339) needs parent submission entry point

## Evidence

- Luật Giáo dục 2019 Điều 83 Khoản 2: "Cha mẹ học sinh có quyền yêu cầu nhà trường, cơ sở giáo dục cung cấp đầy đủ thông tin về quá trình học tập, rèn luyện của con."
- Decree 13/2023 Điều 16: special protection of children's personal data — parent has consent rights
- P5 review report Finding 1: 0% communication coverage
- AC-COMM-001 marked LEGAL MANDATE in P5-k12-school.md

## Proposed Fix

### Phase 2 — Read-only portal UI build-on (Stage 1, Q3 2026)

(Phase 1 = GAP-052a SHIPPED Wave 2; reuses existing Parent + ParentStudentLink entities + V42 migration)

1. **Auth extension:** Add Zalo OTP login flow on top of existing Gateway PARENT user type (Zalo dominant in VN K-12; email/password retained as fallback).
2. **API:** `GET /api/v1/parent/children` (list scoped to ParentStudentLink with consent flags), `GET /api/v1/parent/children/{id}/transcript`, `/attendance`, `/fees`, `/conduct`, `/notifications` — all gated by ParentStudentLink + linkType (PRIMARY can see all, SECONDARY may have limited fields per parental consent).
3. **Frontend NEW:** `/parent` route in `kiteclass-frontend` with multi-children selector (queries `ParentStudentLink WHERE parent.id=current_user`) + dashboard cards per child + drill-down per facet.
4. **PDPL extension:** Add `parental_consent_granular` JSONB field to `ParentStudentLink` (which fields visible per linkType + per child age) + V<N> migration. Audit per-read access.
5. **i18n:** Vietnamese-only Phase 2; future EN/zh-CN for international schools.
6. **Audit log:** Every parent-side data view emits AuditLog entry (parent_id, entity_type, entity_id, timestamp, IP) for legal compliance evidence.

### Phase 3 — Write actions (Stage 2, Q4 2026)

- File complaint (GAP-339)
- Confirm receipt of monthly conduct report
- RSVP parent-teacher meeting (GAP-338)
- Submit absence excuse with evidence upload

### Phase 4 — Multi-channel notification (Stage 3, Q1 2027)

- Bulk notify integration (GAP-063 Zalo + SMS + email + push)
- Read-receipt analytics
- Emergency broadcast (GAP-337) leverages this

## Acceptance Criteria

### Phase 1A (Wave 18b1 Bucket D — DELIVERED 2026-05-04)

- [x] ~~`Parent` + `ParentStudentRelationship` entities migrated~~ — DONE Wave 2 GAP-052a (V42 migration)
- [x] Parent dashboard renders linked-children selector with "Xem học bạ" CTA per child (Phase 1A — multi-card grid with linkType badge)
- [x] Transcript drill-down page renders (`/parent/transcript/[childId]` — semester cards with GPA/credits/pass/fail)
- [x] Server-side scope guard via `ParentStudentLink` boolean exists check BEFORE any data fetch (BR-PARENT-PORTAL-001 — 403 PARENT_NOT_LINKED leak-free)
- [x] Documentation: 3-layer (rules.md + use-cases.md + api-contract.md) extension per `documents/01-business/kiteclass/parent-portal/`
- [x] business-logic-review.md 5-attribute frontmatter on rules.md §11 (Source = Luật GD Đ.83 K2 + Decree 13/2023 Art 16; Compliance = Compliant; Reviewer = solo-dev acting Legal scout; formal counsel review queued GAP-321b/c)
- [x] Tests: unit (4 service + 4 controller), FE component tests (6 hook tests), all green; mvn 30 pass / pnpm build green
- [x] 4-layer V-model coverage matrix (要件: Đ.83 K2 + AC-COMM-001 / 基本: TranscriptView card pattern / 詳細: ParentStudentLink scope-guard short-circuit / コンポ: Card + Badge + Button reused from shadcn)

### Phase 1B (DEFERRED to GAP-321b)

- [ ] 5 remaining facets (attendance / fees / conduct / notifications / discipline) drill-down pages render
- [ ] Multi-children selector enriched with className + grade (currently null)
- [ ] Zalo OTP login working (test tenant + real Zalo OA sandbox)
- [ ] All parent-side reads emit audit log entry (entity, parent ID, timestamp, IP)
- [ ] Test: real PH login → see 2 children → drill into HS A 7A → see 12 môn điểm + 32/35 buổi điểm danh + học phí tháng 10 paid + hạnh kiểm "Tốt"
- [ ] Bulk import xlsx supports `Tên Cha, SĐT Cha, Email Cha, Tên Mẹ, SĐT Mẹ, Email Mẹ` columns with sibling dedup (links to GAP-325)

### Phase 1C (DEFERRED to GAP-321c)

- [ ] PDPL Decree 13/2023 Art 16 children-data special protection: parental consent flag tracked + viewable; data minimization (no fields beyond Đ.83 list)
- [ ] Phase 2 write actions: file complaint (GAP-339), RSVP parent meeting (GAP-338), absence excuse with evidence upload

## Related

- **Supersedes scope of:** GAP-052 (original parent portal stub) — close GAP-052 once this lands
- **Blocks:** GAP-322 (child protection — needs parent intake), GAP-337 (emergency broadcast), GAP-338 (parent meeting), GAP-339 (complaint), GAP-321 secondary ACs
- **Depends on:** GAP-325 (parent-student bulk import linking)
- **Cross-cuts:** GAP-063 (Zalo channel), GAP-184 (data retention 5y), GAP-186 (child protection policy)
- **Audit-to-gap-pipeline.md** Step 2.5 state-check: complete (no pre-existing implementation; greenfield)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-persona-review-round-1.md` Bucket D
- **business-logic-review.md** Source: Luật Giáo dục 2019 Đ.83 Khoản 2; Compliance: Compliant per Đ.83 + Decree 13/2023 Art 16; Cadence: Annual + event-driven on Đ.83 amendment

## Log

- **2026-05-04 (Phase 1A delivered — Wave 18b1 Bucket D)** — Status stays 🟡 PARTIAL. Phase 1A skeleton merged: transcript read-only facet on top of Wave 2 GAP-052a foundation. Shipped:
  - **BE NEW:** `ParentTranscriptController.java` + `ParentTranscriptService.java` + `ParentTranscriptServiceImpl.java` + `TranscriptResponse.java` (record). Endpoint `GET /api/v1/parent/children/{childId}/transcript` scoped via `ParentStudentLinkRepository.existsByParentIdAndStudentIdAndDeletedFalse` (BR-PARENT-PORTAL-001 leak-free guard — 403 PARENT_NOT_LINKED short-circuits BEFORE any transcript fetch).
  - **FE REPLACED:** `(dashboard)/parent/page.tsx` (Wave 2 GAP-052a inline-fetch skeleton) → React Query hooks pattern (`useParentMe`, `useMyChildren`, `useChildTranscript`) with "Xem học bạ" CTA per child card. **FE NEW:** `(dashboard)/parent/transcript/[childId]/page.tsx` (semester cards: GPA/credits/pass/fail). **FE NEW:** `types/parent.ts` + `lib/api/parent.ts` + `hooks/use-parent.ts`.
  - **Business docs (3-layer extension):** `documents/01-business/kiteclass/parent-portal/{rules,use-cases,api-contract}.md` extended with Phase 1A K-12 LEGAL section. Rules.md §11 has `business-logic-review.md` 5-attribute frontmatter (Source = Luật GD 2019 Đ.83 K2 + PDPL Decree 13/2023 Art 16; Reviewer = solo-dev acting Legal scout, formal review queued GAP-321b/c; Compliance = Compliant; Cadence = Annual + event-driven). 9 rules BR-PARENT-PORTAL-001..009 (3 deferred to GAP-321b/c).
  - **Tests:** `ParentTranscriptServiceTest` (4 tests: happy + 403 scope guard + null-args + empty-list — asserts `verify(transcriptRepository, never()).find(...)` for leak-free); `ParentTranscriptControllerTest` (4 tests: 200/401/403/empty); `use-parent.test.tsx` (6 tests: 200 happy + 500 + 403 PARENT_NOT_LINKED + 2× disabled-when-no-id). Total 14 new tests, all green. mvn test 30 passing 0 failures; `pnpm build` green (`/parent` 2.39 kB, `/parent/transcript/[childId]` 1.78 kB).
  - **DEFERRED to GAP-321b (Phase 1B):** 5 other facets (điểm danh / học phí / hạnh kiểm / notifications / kỷ luật), multi-children selector polish (className/grade enrichment), Zalo OTP login flow, per-read audit log (BR-PARENT-PORTAL-008).
  - **DEFERRED to GAP-321c (Phase 1C):** PDPL granular parental-consent flag (BR-PARENT-PORTAL-009), Phase 2 write actions (complaints GAP-339, RSVP GAP-338, absence excuse).
- **2026-05-04 (revision per GAP-345 state-check audit)** — Status flipped 🔵 OPEN → 🟡 PARTIAL. Initial filing claimed "fully greenfield" but Wave 18b plan brainstorm 2026-05-04 found Wave 2 GAP-052a Phase 1 SHIPPED: `Parent.java` + `ParentStudentLink.java` + `ParentInvitation.java` entities + `ParentInvitationService` + V42 migration (3 tables) + Gateway PARENT user type. V42 migration comment explicitly says "Messaging, fee payment, attendance / grade widgets follow in Wave 5 — this migration is deliberately minimal" — confirming THIS gap IS that follow-on. Revised to PARTIAL with accurate Current State + reframed Proposed Fix (Phase 2-4 build-on, not from-scratch). Anti-pattern recurrence (3rd time after GAP-190/197 2026-04-20) — `feedback_audit_grep_scope.md` head-truncation cause.
- **2026-05-04 (initial filing)** — Filed during Wave 17 Bucket D P5 K-12 persona review. State-check ran 3 grep commands with `head` truncation (insufficient per `feedback_audit_grep_scope.md`); concluded "greenfield" incorrectly.
