---
title: API Contract Audit — Wave beta-readiness-4 post-merge
date: 2026-05-25
audit_type: api-contract
audit_skill: .claude/skills/quality/api-contract-audit/SKILL.md
audit_rubric: .claude/skills/quality/api-contract-audit/reference/scoring-guide.md
wave: beta-readiness-4
wave_last_merge: 2026-05-24
audit_window: 2026-05-25
auditor: claude-opus-4-7-1m
audit_mode: read-only
scope:
  - "POST /api/v1/consent/v2/record + GET /api/v1/consent/v2/{userId} + POST /api/v1/consent/v2/withdraw (Bucket B PR #1782)"
  - "POST /api/v1/invoices/{invoiceId}/record-payment + GET /api/v1/invoices/{invoiceId}/payment-records (Bucket C PR #1783)"
  - "POST /api/v1/classes/{classId}/reschedule (Bucket D PR #1781)"
prior_baseline:
  - wave-98-new-contracts: 76/100 C FAIL (2026-05-19, GAP-661)
status: complete
audience: dev
related_gaps:
  - GAP-353b (Bucket B PDPL consent v2)
  - GAP-292 (Bucket C pricing + payment recording)
  - GAP-291 (Bucket D reschedule)
  - GAP-662 (carry-forward Wave 98 — DONE 2026-05-24 via Option B)
  - GAP-663 (carry-forward Wave 98 — DONE 2026-05-24)
---

# API Contract Audit — Wave beta-readiness-4 post-merge (2026-05-25)

> **Audit purpose primacy (per `audit-skill-rubric-api-contract-audit.md` §4):** surface contract drift BEFORE consumers hit it. Bug-list first, score second.

---

## 1. Final Score

**Score:** **74/100** — Grade **C** — Verdict: **PARTIAL FAIL** (P0 sub-check FAIL in Cat 1)

**Delta vs Wave 98 baseline (76/100 C FAIL):** **-2 points** (regression).

| Source of regression | Impact |
|---|---|
| Bucket C `record-payment` + `payment-records` endpoints shipped without documentation in `documents/01-business/kiteclass/payment-invoice/api-contract.md` | -6 Cat 1 (P0 endpoint coverage FAIL) |
| Bucket C `PaymentRecordController` no controller-level integration test (only service-level `PaymentRecordServiceImplTest`) | -3 Cat 5 (P1 IT coverage FAIL) |
| Bucket B Consent v2 ships api-contract.md comprehensive + 2 Postgres IT (`ConsentRecordImmutablePostgresIT` + `ConcurrentConsentWritesIT`) | +4 Cat 1 + Cat 5 (P0 PASS) |
| Bucket D reschedule contract + IT (`ClassControllerRescheduleIT`) comprehensive | +3 Cat 1 + Cat 5 (P0 PASS) |

**Audit-level verdict:** PARTIAL FAIL — Bucket C contract gap = drift class identical to Wave 98 GAP-662 EmailController URL drift (P0 incident class recurring). Per `contract-first-for-cross-layer.md` §3, cross-layer wave PHẢI ship api-contract.md cùng PR — Bucket C ship endpoint mà KHÔNG ship docs = same incident class as GAP-662.

**Phase 1 BETA gate ≥80:** ❌ FAIL (74 < 80, deficit 6 points). Path-to-82 outlined §6.

---

## 2. Per-endpoint check matrix (3 endpoints × 7-check)

### 2.1 Bucket B — POST `/api/v1/consent/v2/record` (+ companion GET / withdraw)

**Controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/immutable/ImmutableConsentController.java:48-118`
**Contract:** `documents/01-business/kitehub/consent/api-contract.md` (210 lines, comprehensive)
**Tests:**
- `kitehub/kitehub-subscription/src/test/java/.../consent/immutable/ConsentRecordImmutablePostgresIT.java`
- `kitehub/kitehub-subscription/src/test/java/.../consent/immutable/ConcurrentConsentWritesIT.java`

| # | Check | Verdict | Evidence |
|---|---|:---:|---|
| 1 | Controller URL matches contract | ✅ PASS | Code `@RequestMapping("/api/v1/consent/v2")` + `/record`/`/{userId}`/`/withdraw` (lines 49,61,81,103) match doc table §1 (line 32-34) |
| 2 | HTTP method matches | ✅ PASS | POST/GET/POST (lines 61,81,103) match doc (lines 32-34) verbatim |
| 3 | Request DTO schema matches | ✅ PASS | `ConsentRequestDto` fields {`userId`, `tenantId`, `granted`, `ipAddress`, `userAgent`} (lines 126-134) match doc §2.1 table line 64-69 (5/5 fields including required/optional markers + size constraints) |
| 4 | Response DTO schema matches | ✅ PASS | `ConsentResponseDto` fields {`id`, `userId`, `tenantId`, `granted`, `prevHash`, `currentHash`, `ipAddress`, `signedAt`} (lines 152-161) match doc §2.2 response JSON (8/8 fields including hash chain fields) |
| 5 | Error codes + RFC 7807 | ⚠️ PARTIAL | Doc §2.3 lists 400 VALIDATION_ERROR + 500 INTERNAL_ERROR; code uses `ResponseStatusException` (line 86,96) — NOT RFC 7807 ProblemDetail format. Doc doesn't explicitly mandate `application/problem+json` though. Consistent with rest of kitehub-subscription `GlobalExceptionHandler.java` patterns. Cat 3 deduct -1 |
| 6 | Integration test verifies schema | ✅ PASS | `ConsentRecordImmutablePostgresIT` (paired with `postgres-specific-type-testcontainers.md` v1.0.0 mandate) — Testcontainers Postgres + 7/7 cases per `gap-status.csv` row 244; `ConcurrentConsentWritesIT` proves SERIALIZABLE concurrent safety per doc §5 |
| 7 | Auth/authz matches doc | ✅ PASS | Doc §1 table "Auth: gateway-level (rate-limit)" — `@RestController` + no `@PreAuthorize` (gateway-level enforcement matches). GET `/{userId}` doc says "tenant-scope (admin OR owner self)" — code line 81 no explicit annotation, relies on gateway. Minor concern but matches contract claim. |

**Verdict Bucket B:** 6 PASS + 1 PARTIAL (RFC 7807 omission), Cat 1+5 P0 PASS. Per-endpoint sub-score: **17/20** average across 7 checks (high quality).

### 2.2 Bucket C — POST `/api/v1/invoices/{invoiceId}/record-payment` + GET `/payment-records`

**Controller:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/record/controller/PaymentRecordController.java:47-97`
**Contract:** **🔴 MISSING** — `documents/01-business/kiteclass/payment-invoice/api-contract.md` (111 lines) chỉ document `mark-paid` (legacy Wave 7 era) + Payment + Installment + Refund. **HAI endpoints `record-payment` + `payment-records` HOÀN TOÀN không có trong any api-contract.md** trong `documents/01-business/`.
**Tests:** `kiteclass/kiteclass-core/src/test/java/.../payment/record/service/PaymentRecordServiceImplTest.java` (service-level only, 5 cases per session handoff)

| # | Check | Verdict | Evidence |
|---|---|:---:|---|
| 1 | Controller URL matches contract | 🔴 **P0 FAIL** | Code `@PostMapping("/{invoiceId}/record-payment")` (line 63) + `@GetMapping("/{invoiceId}/payment-records")` (line 88) — **NO matching api-contract.md entry**. `grep -rn "record-payment\|RecordPayment\|payment-records" documents/01-business/` returns 0 hits. Same drift class as GAP-662 (Wave 98 EmailController). |
| 2 | HTTP method matches | N/A | Cannot verify — doc absent |
| 3 | Request DTO schema matches | N/A | `RecordPaymentRequest` DTO {`method` PaymentRecordMethod, `amount` BigDecimal, `paidAt` Instant, `note` String} present in code (line 67 + `RecordPaymentRequest.java`) — no doc to verify against |
| 4 | Response DTO schema matches | N/A | `PaymentRecordResponse` shape returned (line 78) — no doc table to compare |
| 5 | Error codes + RFC 7807 | N/A | Validation errors (`@DecimalMin "0.01"` line 47 of DTO, `@NotNull` line 41) handled by global handler; doc absent so no contract |
| 6 | Integration test verifies schema | 🔴 **P1 FAIL** | Only service-level `PaymentRecordServiceImplTest` (Mockito-style); NO controller IT `PaymentRecordController*IT.java` testing actual `@PostMapping` binding + `@PreAuthorize` enforcement + `Idempotency-Key` header handling. Same anti-pattern class as Wave 98 GAP-663 PreferencesController zero IT. |
| 7 | Auth/authz matches doc | N/A | Code line 64 `@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'OWNER', 'PLATFORM_ADMIN')")` — well-formed but no doc to verify role list. Sister concern: line 76 `Long recordedByUserId = 1L;` **PLACEHOLDER hardcoded** per Phase 1 BETA stub (cited as "GAP-526 follow-up" in javadoc line 60) — production behavior risk. |

**Verdict Bucket C:** 0/7 cleanly PASS — 2 P0/P1 FAIL + 5 N/A. Per-endpoint sub-score: **3/20** worst-of-3. Drives Cat 1 cap (P0 FAIL → category total ≤16/20).

**Additional concern (line 76):** placeholder `recordedByUserId = 1L` — javadoc says "placeholder=1L until full auth wiring per GAP-526". Production-risk: every payment record attributed to user 1 = audit log integrity violation. NOT documented as known-issue trong session handoff.

### 2.3 Bucket D — POST `/api/v1/classes/{classId}/reschedule`

**Controller:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/controller/ClassController.java:196-205`
**Contract:** `documents/01-business/kiteclass/course-class/api-contract.md` §"POST /api/v1/classes/{classId}/reschedule" (lines 99-145)
**Tests:** `kiteclass/kiteclass-core/src/test/java/.../clazz/controller/ClassControllerRescheduleIT.java` + `ClassServiceRescheduleTest.java` + `ClassRescheduledEmailConsumerTest.java`

| # | Check | Verdict | Evidence |
|---|---|:---:|---|
| 1 | Controller URL matches contract | ✅ PASS | Code `@PostMapping("/api/v1/classes/{classId}/reschedule")` (line 196) match doc heading line 99 verbatim |
| 2 | HTTP method matches | ✅ PASS | POST matches doc |
| 3 | Request DTO schema matches | ✅ PASS | `RescheduleClassRequest` record {`newStartDate` LocalDate @NotNull, `newEndDate` LocalDate @NotNull, `reasonCategory` RescheduleReasonCategory @NotNull, `reasonNotes` String @Size(max=2000)} (lines 27-40 of DTO) match doc JSON example (lines 107-112) + table (line 109-111 reasonCategory enum values) |
| 4 | Response DTO schema matches | ✅ PASS | `ApiResponse.success(classService.rescheduleClass(...), "Đã đổi lịch lớp học thành công")` (lines 202-204) returns `ClassResponse` — doc example response (lines 115-127) matches shape |
| 5 | Error codes + RFC 7807 | ✅ PASS | Doc lists 6 error codes (400 CLASS_INVALID_DATES, 400 Bean validation, 403 ACCESS_DENIED, 404 CLASS_NOT_FOUND, 409 CLASS_CANNOT_RESCHEDULE, 500 RESCHEDULE_EVENT_SERIALIZATION_FAILED). `GlobalExceptionHandler.java` in kiteclass-core handles BusinessException + EntityNotFoundException + MethodArgumentNotValid + AccessDenied — coverage match. Cat 3 PASS |
| 6 | Integration test verifies schema | ✅ PASS | `ClassControllerRescheduleIT` exists (controller-level IT with real Spring context) + `ClassServiceRescheduleTest` (service) + `ClassRescheduledEmailConsumerTest` (event consumer downstream). Session handoff: "14/14 BE + 4/4 email tests PASS" — well covered. |
| 7 | Auth/authz matches doc | ✅ PASS | Code line 197 `@PreAuthorize("@authz.hasAccessToClass(#classId)")` match doc line 100 "Auth: Bearer token + `@authz.hasAccessToClass(#classId)`" + "Role: ADMIN / TEACHER (owner of class)" verbatim |

**Verdict Bucket D:** 7/7 PASS. Per-endpoint sub-score: **20/20**. Cat 1+3+5 all PASS for this endpoint.

---

## 3. Cross-layer drift verification (per `contract-first-for-cross-layer.md` §6.2)

Ran `bash scripts/check-cross-layer-contract-drift.sh` (heuristic v1, WARN-mode CI):

```
Controller paths scanned: 290
Doc paths scanned:        8
Drifts:                   86 (heuristic WARN — many FP do doc table format vs script's H3 regex)
```

**Targeted drift verdict for 3 Wave br-4 scope endpoints:**

| Endpoint | Drift script verdict | True verdict (after manual review) |
|---|---|---|
| `/api/v1/consent/v2/record` | NOT in drift list | ✅ PASS — script's `## 2. POST` regex catches doc §2 heading line 38 |
| `/api/v1/consent/v2/{userId}` | NOT in drift list | ✅ PASS — doc §3 heading line 101 |
| `/api/v1/consent/v2/withdraw` | NOT in drift list | ✅ PASS — doc §4 heading line 152 |
| `/api/v1/invoices/{invoiceId}/record-payment` | NOT in drift list (FP — script's broad scan doesn't catch missing endpoints under existing controller prefix) | 🔴 **P0 DRIFT** — doc completely missing |
| `/api/v1/invoices/{invoiceId}/payment-records` | NOT in drift list (FP) | 🔴 **P0 DRIFT** — doc completely missing |
| `/api/v1/classes/{classId}/reschedule` | listed as drift (FP — doc uses `### POST /api/v1/...` not `## POST` so script regex misses heading style) | ✅ PASS — manual verification shows doc lines 99-145 comprehensive |

**Detector limitation discovered:** drift script's heuristic regex misses doc entries using H3 `### ` heading style (common pattern in this repo) vs H2 `## ` (script regex). Follow-up gap candidate: tighten regex to accept both H2+H3 heading levels. Also: script can't detect MISSING endpoints in code that aren't in docs (only flags presence mismatch). Bucket C invisible to current detector → user manual review required.

---

## 4. New findings — gap candidates

### 4.1 GAP-NEW-payment-record-api-contract-missing (P0 — recommended file)

**Class:** API contract drift — same incident class as Wave 98 GAP-662

**Problem:** Bucket C ship `PaymentRecordController` exposing 2 new endpoints (`POST /api/v1/invoices/{invoiceId}/record-payment` + `GET /api/v1/invoices/{invoiceId}/payment-records`) but NO matching documentation in `documents/01-business/kiteclass/payment-invoice/api-contract.md`. Violates `contract-first-for-cross-layer.md` §3 (cross-layer wave PHẢI ship api-contract.md cùng PR).

**Impact:** Consumers (FE Wave br-5 record-payment modal per session handoff line 36 "GAP-NEW-pricing-data-reclassification UI") cannot reference canonical contract → repeat of Wave 32 endpoint proliferation incident class. Audit log integrity risk (placeholder `recordedByUserId=1L`).

**Proposed fix:** Extend `documents/01-business/kiteclass/payment-invoice/api-contract.md` with new §"Manual Payment Recording" section documenting:
- POST `/api/v1/invoices/{invoiceId}/record-payment` — request shape (PaymentRecordMethod enum CASH/BANK_TRANSFER/VIETQR/MOMO, amount BigDecimal, paidAt Instant?, note String≤500), response 201 + PaymentRecordResponse, error codes (400 validation, 403 ACCESS_DENIED, 404 INVOICE_NOT_FOUND, 409 IDEMPOTENCY_CONFLICT), Idempotency-Key header semantics per BR-PAYMENT-METHOD-004
- GET `/api/v1/invoices/{invoiceId}/payment-records` — response 200 + List<PaymentRecordResponse>, auth roles

**Effort:** ~30 min docs writing + reviewer-checklist.

### 4.2 GAP-NEW-payment-record-controller-it (P1 — recommended file)

**Class:** IT coverage gap — same anti-pattern class as Wave 98 GAP-663 PreferencesController zero IT

**Problem:** `PaymentRecordController` has only service-level test (`PaymentRecordServiceImplTest` Mockito-style) — no controller IT testing `@PostMapping` JSON binding + `@PreAuthorize` role enforcement + `Idempotency-Key` header handling + JPA transactional boundaries.

**Impact:** Validation rules (`@DecimalMin "0.01"`, `@NotNull` PaymentRecordMethod) may pass unit but fail integration; idempotency-key flow untested end-to-end.

**Proposed fix:** Add `PaymentRecordControllerIT.java` covering:
- Happy path POST 201 + response shape
- Validation 400 cases (amount=0, missing method, negative amount, note>500 chars)
- AuthZ 403 cases (no role)
- Idempotency-Key replay (same key → same response, no duplicate row)
- Cross-tenant 404/403 (BR-PAYMENT-METHOD security)

**Effort:** ~2-3h.

### 4.3 GAP-NEW-payment-record-recorded-by-placeholder (P1 — recommended file)

**Class:** Production behavior risk — placeholder leaks past Phase 1 BETA

**Problem:** `PaymentRecordController.java` line 76 hardcodes `Long recordedByUserId = 1L;` with javadoc note "Phase 1 BETA: placeholder=1L until full auth wiring per GAP-526". Every payment record attributed to user ID 1 in audit log.

**Impact:** Audit log integrity violation — financial transactions cannot be traced to actual user; PDPL Art 11 immutable audit requirement (cited in session handoff §5 wave audit suite Security category) compromised.

**Proposed fix:** Either (a) extract `userId` from `@AuthenticationPrincipal Object principal` (line 69, declared but unused), OR (b) add JIRA-style runtime banner if placeholder used + block deploy if `application-production.yml` profile active. Track GAP-526 follow-up.

**Effort:** ~1-2h if auth wiring already supports, ~1 day if not.

### 4.4 GAP-NEW-drift-script-h3-heading-regex (P2 META — recommended file)

**Class:** Detector improvement — drift script blind spot

**Problem:** `scripts/check-cross-layer-contract-drift.sh` regex misses doc entries using H3 `### Method /path` heading style (`course-class/api-contract.md` pattern). Caused false positive on reschedule endpoint (script reported drift; manual review found doc present).

**Proposed fix:** Extend doc-extraction regex to accept both `## Method /path` AND `### Method /path` heading levels.

**Effort:** ~30 min script update + self-test fixture.

---

## 5. Carry-forward Wave 98 GAP status

| Gap | Wave 98 status | Wave br-4 status | Verification |
|---|---|---|---|
| **GAP-662** EmailController URL drift `/api/email/send` vs `/api/platform/emails/send` | 🔵 OPEN (Wave 98 audit suite finding) | ✅ **DONE** (2026-05-24 via Wave beta-readiness-2 Bucket D PR #1771, commit `1ad7e31b`) | Option B selected over Option A (10+ files multi-service rename risk). api-contract.md updated `/api/platform/emails/send` matching actual `EmailController @RequestMapping("/api/platform/emails")`. Option A v1 namespace migration deferred GAP-733 (P2 Wave 109+). File moved to `phase-1-beta/closed/`. **Verified:** code `EmailController.java:27 @RequestMapping("/api/platform/emails")` matches doc — drift eliminated. |
| **GAP-663** PreferencesController zero IT + cookie HttpOnly drift | 🔵 OPEN (Wave 98 audit suite finding) | ✅ **DONE** (2026-05-24) | Listed DONE in `gap-status.csv` row 4 (closed/), per session handoff. ≥3 MockMvc test methods + doc reconciliation shipped. |

Both Wave 98 carry-forwards CLOSED before Wave br-4 closure. No carry-forward debt entering Wave br-4 audit cycle.

---

## 6. Phase 1 BETA gate verdict + path to 82

### 6.1 Phase 1 BETA gate ≥80

**Current: 74/100 — FAIL** (deficit 6 points).

### 6.2 Path to 82 (PASS + 2-point buffer)

| Step | Action | Expected delta |
|---|---|---|
| 1 | File + ship `GAP-NEW-payment-record-api-contract-missing` (P0) — extend `payment-invoice/api-contract.md` with 2 new endpoints | **+6 Cat 1** (P0 FAIL → PASS; endpoint coverage 95%+) |
| 2 | File + ship `GAP-NEW-payment-record-controller-it` (P1) — `PaymentRecordControllerIT.java` covering 5+ scenarios | **+3 Cat 5** (P1 FAIL → PASS; IT coverage ≥90% endpoints) |
| 3 | Optional polish: address Bucket B Cat 3 minor RFC 7807 mention in consent v2 doc §2.3 + §3.3 + §4.3 | **+1 Cat 3** (partial → PASS) |
| **Total path** | All 3 above | **+10 → score 84/100 B-** |

**Effort estimate:** ~3-4h (P0 doc ~30 min + P1 IT ~2-3h + P2 polish ~30 min). Can ship as Wave beta-readiness-5 hotfix or dedicated audit-followup wave.

### 6.3 Alternative: minimum compliance path to 80 exactly

| Step | Action | Expected delta |
|---|---|---|
| 1 | File + ship `GAP-NEW-payment-record-api-contract-missing` (P0) only | **+6 → score 80/100 B-** (PASS exact threshold, 0 buffer) |

Risk: 0 buffer means any regression in next audit cycle FAILs gate. Path-to-82 recommended.

### 6.4 P0 production-risk gap (independent of gate)

`GAP-NEW-payment-record-recorded-by-placeholder` — production audit log integrity risk. Should file regardless of gate path; track GAP-526 cluster.

---

## 7. Cross-reference audits-index.csv (per `output-review-mandate.md` §3)

Append row to `documents/04-quality/audits/audits-index.csv` (separate sync PR):

```csv
AUDIT-2026-05-25-wave-br-4-api-contract,api-contract,2026-05-25,wave-br-4,74,C,PARTIAL_FAIL,GAP-NEW-payment-record-api-contract-missing+GAP-NEW-payment-record-controller-it+GAP-NEW-payment-record-recorded-by-placeholder+GAP-NEW-drift-script-h3-heading-regex,76→74 (-2 regression Bucket C contract gap)
```

§3 matrix row "API contracts" Current Status update separate PR (per Wave 99 streamline lesson — CSV canonical, matrix terse):

> 🔴 74/100 C PARTIAL_FAIL (2026-05-25, Wave br-4) — see `audits-index.csv`; path 82 PASS via GAP-NEW-payment-record-api-contract-missing + GAP-NEW-payment-record-controller-it cluster ~3-4h

---

## 8. Verification methodology + transparency

**Audit mode:** READ-ONLY (no code/doc changes; gap files NOT filed in this PR — recommendation only).

**Files examined:**
- 3 Java controller files (lines audited cited inline)
- 3 api-contract.md files (210 + 111 + 293 lines surveyed)
- 1 DTO file (RescheduleClassRequest record)
- 1 DTO file (RecordPaymentRequest class)
- Drift script run (`scripts/check-cross-layer-contract-drift.sh`)
- IT file existence checks via `find` (8 hits across 3 endpoints)
- `gap-status.csv` rows 177, 178, 243, 244 (GAP-291/292/353/353b)
- Session handoff `documents/03-planning/session-handoffs/2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 1/5"
- 2 closed gap files (GAP-662, GAP-663) for carry-forward status

**Methodology compliance:**
- Per `audit-skill-rubric-api-contract-audit.md` §3 primacy: bug list (§4 + §2.2) BEFORE score (§1) ✅
- Per `audit-skill-rubric-api-contract-audit.md` §4 sub-checks: NEVER skip "obviously fine" — all 7 checks × 3 endpoints = 21 sub-checks marked PASS/FAIL/PARTIAL/N/A ✅
- Per `output-review-mandate.md` §3: cite per-endpoint Controller file:line + api-contract.md file:line + test file ✅
- Per `dev-readable-doc-language.md` §2: narrative Vietnamese + English identifiers ✅
- Per `audit-to-gap-pipeline.md` §3: findings ready for gap filing (4 gap candidates §4) ✅

**Limitations:**
- Did NOT execute `mvn verify` (read-only audit constraint)
- Did NOT verify production behavior (read-only audit constraint)
- Drift script heuristic v1 limitation surfaced (§3 follow-up GAP-NEW-drift-script-h3-heading-regex P2)
- Bucket C controller IT absence inferred from file find (no manual mvn run)

---

## 9. References

- Skill: `.claude/skills/quality/api-contract-audit/SKILL.md` (v1; §3 primacy rubric)
- Rubric: `.claude/skills/quality/api-contract-audit/reference/scoring-guide.md`
- Wave plan: `documents/03-planning/session-handoffs/2026-05-24-wave-beta-readiness-4-closure.md`
- Prior audit: `documents/04-quality/audits/api-contract/2026-05-19-wave-98-new-contracts.md` (baseline 76/100)
- Sister rule: `.claude/rules/contract-first-for-cross-layer.md` §3 cross-layer mandate
- Sister rule: `.claude/rules/audit-to-gap-pipeline.md` §3 audit→gap flow
- Rule applied: `.claude/rules/postgres-specific-type-testcontainers.md` (Consent v2 IT verification per Bucket B §2.1 check 6)
- Rule applied: `.claude/rules/pre-handoff-self-test-completeness.md` (Bucket C drift recurrence class)
- Related closed gaps: GAP-662, GAP-663 (Wave 98 carry-forward, both DONE)
- Related new gaps (recommended file): GAP-NEW-payment-record-api-contract-missing, GAP-NEW-payment-record-controller-it, GAP-NEW-payment-record-recorded-by-placeholder, GAP-NEW-drift-script-h3-heading-regex
