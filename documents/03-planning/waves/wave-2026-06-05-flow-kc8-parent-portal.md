---
title: Wave flow-kc8 — Parent portal (child grade/attendance/fees/conduct facets)
status: active
created: 2026-06-05
updated: 2026-06-05
waves: [flow-kc8]
tag_primary: flow
tags_secondary: [kc8, parent-portal, facet, idor, consent, pdpl, kiteclass, campaign]
counter: 8
campaign: flow-verification-campaign
gaps: []
---

# Wave flow-kc8 — Parent portal

**Goal:** Walk end-to-end flow KC-8 (Phụ huynh đăng nhập xem dữ liệu con — children list + transcript + attendance + fees + conduct facet; consent gate + IDOR guard) trên stack production-equivalent, đạt **G1 PASS**. Đứng sau KC-4/5/6/7 (parent đọc dữ liệu con đã enroll + có điểm danh/điểm/hóa đơn).

**Trigger:** KC-8 unblocked sau KC-4/5/6/7 G1 PASS 2026-06-05. Campaign chain priority 9 (core tier cuối). Parent facet tiêu thụ attendance/grade/invoice/incident repos làm input.

## 1. Brainstorm

**State-check (2026-06-05, 2 Explore agent map):** KC-8 = kiteclass-core `module/parent/`. **Backend chín** — 11 controller CONFIRMED tồn tại (grep, no partial-impl risk cho read facets):
- ✅ `ParentController` @ `/api/v1/parent` — GET `/me`, GET `/me/children`.
- ✅ `ParentTranscriptController` — GET `/children/{childId}/transcript` (`@PreAuthorize hasAccessToChild`).
- ✅ `ParentAttendanceFacetController` — GET `/children/{childId}/attendance?from&to` paged (`@PreAuthorize hasAccessToChild`).
- ✅ `ParentFeesFacetController` — GET `/children/{childId}/fees?from&to` paged (`@PreAuthorize hasAccessToChild`).
- ✅ `ParentConductFacetController` — GET `/children/{childId}/conduct?period` (`@PreAuthorize hasAccessToChild`).
- ⚠️ `ParentNotificationsFacetController` — GET `/children/{childId}/notifications` — **THIẾU `@PreAuthorize`** (stub trả empty, service-layer guard only).
- ⚠️ `ParentPaymentController` — POST `/children/{childId}/payments` (Idempotency-Key) — **THIẾU `@PreAuthorize`** (inline link-check, VietQR stub Wave 106).
- ✅ `ParentConsentController` — GET/PUT `/consent?childId`.

**IDOR guard 2 lớp:** `@PreAuthorize("@authz.hasAccessToChild(#childId)")` (`AuthorizationBean.hasAccessToChild` — admin bypass + `UserContext.getCurrentReferenceId()` + link check, GAP-798 wired) + service-layer `existsByParentIdAndStudentIdAndDeletedFalse` → 403 `PARENT_NOT_LINKED`/`PARENT_FACET_FORBIDDEN`. Reference bridge: `parents.id == users.reference_id == X-User-Reference-Id`.

**Consent gate (PDPL):** mỗi facet read qua `ConsentService` — `PARENT_CONSENT_REQUIRED` (per-field) + `RECONSENT_REQUIRED` (version stale). Consent đọc `parental_consent` JSONB trên `parent_student_links`; required version từ config.

**Pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` (BẮT BUỘC — phụ huynh xem dữ liệu con):** Opus agent spawned 2026-06-05 → artifact `documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc8-parent-portal.md`. Likely failure modes: consent fields rỗng → facet block PARENT_CONSENT_REQUIRED (walk happy-path bị chặn) / cross-parent IDOR đổi childId → reject đúng không / notifications+payment thiếu @PreAuthorize → IDOR lọt service-layer / re-consent version stale message / pagination from/to sai → 400 vs 500 / audit log ghi nhầm consent-blocked read thành success / cross-tenant parent đọc child tenant khác.

**Walk fixtures (dev DB `kiteclass_shared`):** parent 1 (`parent1@test.com`, ref-id 1) → child 1; parent 2 (`parent2@test.com`, ref-id 2) → child 2; cùng instance `aaaabbbb-…0001`. Data: invoices 3 rows (fees facet walkable), attendance_period + transcripts 0 rows (empty-page 200 hoặc seed). Consent `{"fields":{},"version":1}` cả 2 link.

**Blocker:** none known. FE wiring partial (chỉ `/me` + transcript wired; attendance/grades/billing pages còn mock) → FE-wiring là G2/defer scope, G1 walk focus BE facet (giống KC-5/6/7 BE-first pattern).

## 2. Task Breakdown

| Bucket | Scope | Owner | Walk class |
|---|---|---|---|
| 0 (Pre-walk) | Opus persona sim agent → ≥5 failure modes per `pre-walk-persona-simulation-mandate.md` §3 | Coordinator | n/a (DONE — agent spawned) |
| A (Fix) | Batch-fix HIGH-confidence pre-walk findings (2 thiếu @PreAuthorize + consent/audit findings) per `feature-ship-runtime-walk-mandate.md` §3.4 + cross-flow sweep IDOR per `cross-flow-bug-class-sweep.md` | agent/coordinator | — |
| B (Walk) | Coordinator G1 walk: parent login → GET /me/children → GET facet (transcript/attendance/fees/conduct) happy + consent-block + IDOR cross-parent + cross-tenant + pagination sad path | Coordinator | user-facing ✅ pre-walk required |
| C (Walk) | Notifications/payment authz walk: cross-parent POST payment / GET notifications → verify service-layer guard holds despite missing @PreAuthorize (or fixed in A) | Coordinator | user-facing ✅ |
| D (G2 handoff) | G2 recipe MD per `g2-handoff-md-mandate.md` khi G1 PASS | Coordinator | — |

**Stub scope note:** notifications facet (empty — GAP-063b), payment VietQR (stub Wave 106), conduct severity projection — G1 walk verify guard + contract shape; full data defer. FE wiring (attendance/grades/billing mock) defer G2/Phase 1.5.

## 3. Scope

Full §3 expansion happens at walk-time (after pre-walk agent returns). Skeleton:
- **BE (kiteclass-core):** `module/parent/**` (11 controllers + facet service impls + ParentStudentLink entity + ConsentService + ParentReadAuditLogService + AuthorizationBean.hasAccessToChild + DTOs).
- **Verify target:** parent 1 → GET /me/children = [child 1]; GET transcript/attendance/fees/conduct child 1 → 200 (consent-permitting); GET child 2 (cross-parent) → 403; consent rỗng → PARENT_CONSENT_REQUIRED; pagination sai → 400.
- **Isolation:** cross-parent (parent 1 ↛ child 2) + cross-tenant (parent instance A ↛ child instance B) → 403/404.
- **Dependency:** KC-4 enroll + KC-5 attendance + KC-6 grade + KC-7 invoice data (parent reads downstream). Invoices có data; attendance/transcript cần seed cho happy read.

## 4. State-Check Evidence

Verified 2026-06-05 (2 Explore agent, grep, no `| head`):

| Symbol | Verdict |
|---|---|
| 11 controllers @ `module/parent/` | ✅ exist (ParentController/Transcript/Attendance/Fees/Conduct/Notifications/Payment/Consent/Complaint/Invitation/InternalParent) |
| `@authz.hasAccessToChild` IDOR guard | ✅ `AuthorizationBean.hasAccessToChild` + service `existsByParentIdAndStudentIdAndDeletedFalse` |
| `ParentStudentLink` entity + V42 schema | ✅ `parent_student_links` tenant-scoped (instance_id NOT NULL) + parental_consent JSONB (V56) |
| ConsentService + ParentReadAuditLogService | ✅ per-facet consent gate + audit row |
| Notifications + Payment controller @PreAuthorize | ⚠️ MISSING — pre-walk batch-fix candidate (Bucket A) |

Detailed facet DTO + consent field logic + audit semantics = read at walk-time (Bucket B) + pre-walk agent.

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — coordinator walk | Claude | /me/children + facet read happy + consent-block + IDOR cross-parent 403 + cross-tenant reject + pagination sad path; notifications/payment authz; production-equivalent stack | ⬜ |
| G2 — human walk | User | Per G2 recipe MD (Bucket D) — parent xem dữ liệu con via UI (transcript wired; attendance/fees note mock) | ⬜ |
| G3 — production parity | User | Post AWS restore — multi-tenant parent isolation + consent enforcement + audit log | ⬜ |

## 6. Agent Spawn Pattern

_(flow-walk wave: Bucket 0 = 1 Opus pre-walk persona-sim agent (background); G1 = coordinator manual walk. Fix-agents spawned ad-hoc per finding per `agent-model-opus-default.md`.)_

## 7. Closure Protocol

1. Catalog walk findings → file gaps inline per `discovery-to-gap-inline-filing.md` §3.
2. Batch-fix high-confidence (pre-walk + walk) per `feature-ship-runtime-walk-mandate.md` §3.4.
3. Re-walk affected scope per `pre-handoff-self-test-completeness.md` §3.
4. G2 recipe MD per `g2-handoff-md-mandate.md` (Bucket D).
5. Flip campaign §4 KC-8 row → 🔄 walk-pass-pending-human.
6. wave-history.jsonl append; frontmatter draft → active.
7. CSV + ROADMAP sync per `post-merge-sync-completeness.md`.

## 8. Log

- **2026-06-05 (plan ship):** Filed sau KC-4/5/6/7 G1 PASS. 2 Explore agent map: KC-8 BE chín (11 controllers + 2-layer IDOR + PDPL consent + audit + real delegation), FE partial (transcript wired; attendance/fees/billing mock). KC-9 contract-first stub → user chốt build wave riêng sau KC-8. Pre-walk persona sim agent (Opus) spawned. Fixtures: parent 1→child 1, parent 2→child 2 same instance; invoices 3 rows; consent fields empty. 2 @PreAuthorize missing (notifications + payment) = pre-walk batch-fix candidate.

## 9. Pre-walk findings (2026-06-05) — `audits/persona-review/2026-06-05-pre-walk-kc8-parent-portal.md`

Opus agent → 10 failure mode (3 HIGH / 5 MEDIUM / 2 LOW). **Batch-fixed TRƯỚC walk:**
- **#1 (HIGH):** Missing `from`/`to` param → 500 thay vì 400. Fix: `MissingServletRequestParameterException` handler trong `GlobalExceptionHandler` → 400 `PARAM_MISSING`.
- **#3 (HIGH consistency):** Notifications + Payment controller thiếu `@PreAuthorize`. Fix: thêm `@PreAuthorize("@authz.hasAccessToChild(#childId)")` cả 2 (cross-flow sweep: 6/6 controller giờ guarded).

**Verified during walk:** #6 (consent first-login 403 everywhere — đúng PDPL, FE phải branch CTA), #8 (gateway no role-gate → GAP-1007 defer P2), #4 (payment no consent → GAP-1008 defer P3), #5 (audit-after-consent ordering đúng), #9 (cross-tenant via tenant filter), #2/#10 (FE contract + size cap minor).

## 10. G1 Outcome (2026-06-05)

**G1 ✅ PASS** (production-equivalent walk, post 2 rebuild — pre-walk fixes + GAP-1006 fees fix):

| # | Scenario | Result |
|---|---|---|
| W1 | GET /me/children (parent 1) | 200 [child 1] ✅ |
| W2 | Missing from/to param (fix#1) | 400 PARAM_MISSING ✅ |
| W3 | Consent empty → read | 403 PARENT_CONSENT_REQUIRED ✅ |
| W4 | PUT /consent grant | 200 (version→2) ✅ |
| W5a | Attendance (consent) | 200 empty ✅ |
| W5b | Fees (consent) | 200 (2 invoices) ✅ **GAP-1006 fixed** |
| W6 | IDOR fees child2 | 403 ACCESS_DENIED ✅ |
| W7a/b | IDOR notifications/payment child2 (fix#3) | 403 ACCESS_DENIED ✅ |
| W7c | Payment own child (valid body) | passes authz → business ✅ |
| W8/8b | Transcript / conduct | 200 ✅ |
| W9 | childId=abc | 400 PARAM_TYPE_MISMATCH ✅ |
| W10 | No X-User-Reference-Id | 401 AUTH_REQUIRED ✅ |
| W11 | Inverted range | 400 BAD_REQUEST ✅ |

**Walk-discovered:** GAP-1006 P1 (fees MultipleBagFetchException 500 — fixed + re-walk PASS). **Filed defer:** GAP-1007 P2 (role-collision IDOR) + GAP-1008 P3 (payment consent asymmetry).

**Test note:** `mvnw test` 8 IT context-load errors xác định **preexisting** (clean-code stash reproduce — MapStruct `AssignmentMapper` không generate trong isolated test compile; GAP-735 class). My changes boot OK trong container (full Spring context) + walk PASS = G1 evidence.

**Walk fixtures (dev DB):** parent 1 (ref-id 1) → child 1 (consent granted version 2); parent 2 → child 2; child 1 = 2 invoices INV-TEST-001/002. FE wiring attendance/fees/billing mock → defer Phase 1.5. G2 handoff: [`2026-06-05-g2-recipe-kc8-parent-portal.md`](../../05-guides/operations/2026-06-05-g2-recipe-kc8-parent-portal.md). Campaign §4: KC-8 → 🔄 walk-pass-pending-human.

## Scope-Completeness Reconciliation

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Bucket 0 pre-walk persona sim (≥5 FM) | ✅ DONE | artifact shipped (10 FM) |
| 2 | Bucket A batch-fix pre-walk HIGH | ✅ DONE | fix#1 + fix#3 (2 @PreAuthorize) |
| 3 | Bucket B walk facets + consent + IDOR | ✅ DONE | W1-W11 PASS |
| 4 | Bucket C notifications/payment authz | ✅ DONE | W7a/b 403 |
| 5 | Walk-discovered fees 500 | ✅ DONE | GAP-1006 fixed + re-walk |
| 6 | Bucket D G2 recipe MD | ✅ DONE | shipped |
| 7 | Role-collision IDOR defense-in-depth | ❌ NOT-IMPL (defer) | GAP-1007 P2 |
| 8 | Payment consent gate | ❌ NOT-IMPL (defer) | GAP-1008 P3 |
| 9 | FE wiring attendance/fees/billing | ❌ NOT-IMPL (defer) | Phase 1.5 (FE mock current) |
| 10 | G3 production parity (gateway chain) | ⛔ **DEFERRED Phase 2** | GAP-725 + GAP-798b — parent LOGIN is Phase 2 (see §11) |

## 11. G3 production-parity finding (2026-06-05) — Phase-2-gated

**G3 attempt (mint JWT → gateway :9000, no manual headers):** empirically blocked at tenant resolution (`TokenService.resolveTenantIdForRole` only issues `tenantId` claim for OWNER — PARENT/TEACHER/STUDENT return null per line 86 "until their auth paths land"). **Code-level definitive:** gateway injects NO `X-User-Reference-Id` (grep all gateway filters = 0); `TokenService` issues NO `referenceId` claim.

**Verdict: NOT a new bug — tracked + deliberately deferred Phase 2.**
- **GAP-725** (P1, Phase 2): parent/teacher/student auth path architectural gap — those roles can't login in Phase 1 (KH PlatformRole = OWNER/STAFF/PLATFORM_ADMIN only). Decision: Hướng B (teacher email+pass) + Hướng C (parent/student invite+OTP).
- **GAP-798b** (P1, OPEN): reference_id producer side + gateway X-User-Reference-Id forward — BLOCKED on parent login-wiring; deliberately NOT built (unverifiable security = trust-pass anti-pattern). Consumer-side authz bridge (G1 tested) shipped GAP-798.

**KC-8 Phase-1 scope is correct:** parent portal BE facets are production-ready (consumer-side authz + consent + IDOR + audit verified G1 direct-core); the production ACCESS path (parent login → JWT reference_id → gateway inject) is Phase 2 by deliberate architecture. G3 gateway-parity unblocks when GAP-725/GAP-798b land Phase 2. Same Phase-2 gate applies to KC-9 student portal.
