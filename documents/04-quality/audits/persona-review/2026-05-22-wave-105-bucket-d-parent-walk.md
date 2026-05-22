---
title: Wave 105 Bucket D — Parent Persona Walk (Chị Linh)
status: complete
created: 2026-05-22
audience: dev
wave: 105
bucket: D
persona: P4-Parent
gaps: [GAP-286, GAP-705, GAP-706, GAP-707, GAP-156]
---

# Wave 105 Bucket D — Parent Persona Walk Audit

> **Persona:** Chị Linh — phụ huynh có **2 con học** tại Trung tâm Anh ngữ Sky Education.
> Per `vn-localization-audit-checklist.md` §4 row "Phụ huynh thoại chính": Mother (mẹ) thường là primary contact cho child education.

## TL;DR

- **AC 4/5 PASS** + 1 PARTIAL (mobile walk defer Docker WSL2 quirk per wave plan §1 Risk #3).
- **5/5 IT tests PASS** (`ParentPaymentControllerTest`) covering multi-child authz + idempotency replay + edge cases.
- **B1/D1 PaymentController.userId=1L hardcoded** → HANDOFF Bucket E security cluster (NOT fixed here per scope guidance).
- **VietQR + Zalo OA full integration** defer Wave 106 per `outside-in-coverage-trigger.md` v1.1.0 §3 (Architecture-decision keywords) + GAP-286.

## Scope walked

Files touched Wave 105 Bucket D:

| File | Type | Purpose |
|---|---|---|
| `V61__add_parent_payment_idempotency.sql` | Flyway migration | `payment_idempotency_keys` + `zalo_oa_notification_outbox` tables |
| `ParentPaymentController.java` | New REST controller | `POST /api/v1/parent/children/{childId}/payments` — multi-child authz + Idempotency-Key |
| `PaymentIdempotencyService.java` | New service | Header validation + race-safe lookup/recordFirstWrite |
| `ZaloOaNotificationService.java` + Impl | New stub interface + impl | 3 events (invite / payment confirm / attendance alert) — log + outbox row |
| `ParentPaymentControllerTest.java` | New IT test | 5 scenarios covering AC3+AC4 + 400/401 edge cases |

State-check completed PRE-implementation:
- `ParentController` + `ParentTranscriptController` exist + already follow `X-User-Reference-Id` pattern (gateway-populated identity) — reused, NOT duplicated.
- `parent_student_links.parental_consent` JSONB column exists từ V56 (Wave 19 GAP-321c Phase 1C) — parent-on-behalf-of-child PDPL Art 16 consent state already shipped. **No new schema needed cho AC2.**
- `ParentStudentLinkRepository.existsByParentIdAndStudentIdAndDeletedFalse` already exists + used in `ParentTranscriptServiceImpl` — multi-child authz pattern reused verbatim.
- `payment_attempts` table did NOT exist — V61 introduces `payment_idempotency_keys`.
- `ConsentService` interface + impl already shipped (`ConsentServiceImpl`) handling `checkConsent(parentId, childId, field)` + version + reconsent — PDPL Art 11 parental-consent-for-child variant ALREADY in place via this service.

## AC outcomes per Wave 105 plan §3 Bucket D

| # | Acceptance Criteria | Status | Evidence |
|---|---|:---:|---|
| 1 | Walk Parent journey (invite → PDPL consent → setup password → view child attendance + grade → pay invoice via VietQR) | 🟡 PARTIAL | BE endpoint + IT walk shipped; FE mobile walk defer (see §"Mobile walk defer"). Existing `ParentController` + `ParentTranscriptController` + facet controllers already cover view; new `ParentPaymentController` covers pay-by-VietQR (stub). |
| 2 | Parent-on-behalf-of-child PDPL consent variant | ✅ DONE | `parent_student_links.parental_consent` JSONB column shipped V56 + `ConsentService.checkConsent` + `getRequiredVersion` already gate every facet read. Reused verbatim by `ParentTranscriptServiceImpl` §guard 4-6. PDPL Art 16 covered; Art 11 parental-consent-as-data-subject angle covered by `consentService` per-field per-parent flag (each parent independently consents on behalf of child). |
| 3 | Multi-child authz: spoof childId=B → 403 | ✅ DONE | `ParentPaymentController.createPaymentForChild` checks `linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, childId)` BEFORE any business logic. IT test `cross_child_spoof_returns_403` confirms 403 PARENT_NOT_LINKED + `paymentService.createPayment` NEVER invoked. |
| 4 | VietQR idempotency: pay 2× → 1 payment row + 1 QR code (Idempotency-Key header) | ✅ DONE | `Idempotency-Key` header validated by `PaymentIdempotencyService.requireValidKey` (16-64 char alphanumeric+`_-`). First request → INSERT + `X-Payment-Idempotent-Replay: false`. Replay → `lookup` returns cached → `X-Payment-Idempotent-Replay: true` + 200 (not 201). Race-safe via UNIQUE `uk_payment_idempotency_scope (instance_id, idempotency_key)`. IT tests `firstRequest_creates_payment_and_records_zalo_stub` + `replay_returns_cached_payment_no_new_charge` PASS. |
| 5 | Zalo OA notification stub: log "would send Zalo OA" cho 3 events | ✅ DONE | `ZaloOaNotificationService` interface defines 3 methods (`recordParentInviteSent`, `recordPaymentConfirm`, `recordAttendanceAlert`). Impl uses `Propagation.REQUIRES_NEW` per `audit-service-isolation.md` v1.0.0 — caller success path unaffected by Zalo OA failure. Each method INSERT `zalo_oa_notification_outbox` row + emit `log.info("would send Zalo OA: ...")`. Wave 106 GAP-286 dispatcher reads outbox + dispatches actual ZNS. |

## Evidence per `pre-handoff-self-test-completeness.md`

### §2.6 Payment flow checklist

| Check | Pass criterion | Evidence |
|---|---|---|
| (a) Gateway redirect URL correct | Browser redirected to actual provider | 🟡 STUB Wave 105 — `stubVietQrPayload` returns deterministic string. Wave 106 GAP-NEW integrates real VietQR API per partner-bank agreement. Defer reason: PSP license + KYC barriers (per Wave 93 retro lessons GAP-185/183 self-build → partnership pattern). |
| (b) Return URL handled (success + cancel) | Both paths render right post-payment UI | Defer — FE walk via mobile/desktop browser blocked by Docker WSL2 quirk (wave plan §1 Risk #3). Existing `PaymentResponse` DTO carries `paymentUrl` field that FE consumes. |
| (c) Webhook signature verified server-side | Webhook handler rejects unsigned/invalid-sig with 400 | N/A Wave 105 — webhook scope Wave 106 (full VietQR integration). |
| **(d) Idempotency key honored** | Same key replayed → no double-charge; row in payment_attempts table with idempotency state | ✅ **VERIFIED** — V61 `payment_idempotency_keys` table + `PaymentIdempotencyService` + IT `replay_returns_cached_payment_no_new_charge` (`paymentService.createPayment` NEVER called on replay, `verify(...).never()`). |
| (e) Reconciliation table updated | payments row matches gateway state after webhook | N/A Wave 105 — webhook scope Wave 106. |
| (f) Failed payment UI clear | User sees actionable error | Existing `BusinessException` machinery → standard `ApiResponse.error` shape. |
| (g) Audit log: amount, currency, gateway_txn_id, user_id, timestamp | Row exists in payment_audit_log | Stub Wave 105 via `log.info("Creating parent payment: parentId={} childId={} invoiceId={} method={}")`. Full audit table = future scope. |

### §2.7 Multi-tenant tenant-switch checklist

Parent persona is single-tenant (1 phụ huynh = 1 trung tâm via `instanceId` từ `BaseEntity`). Multi-tenant tenant-switch scope = N/A. **Per-resource isolation** (the closer concept here — child A vs child B within same tenant) covered by AC3 above.

### §2.4 Admin-flow checklist

N/A — Parent persona is non-admin. Auth handled by gateway `X-User-Reference-Id` header.

## Multi-child authz @PreAuthorize pattern + IT scenarios

**Pattern adopted:** Service-level boolean-exists guard via `ParentStudentLinkRepository.existsByParentIdAndStudentIdAndDeletedFalse`. This matches the established pattern in `ParentTranscriptServiceImpl` (Wave 18b1 GAP-321 Phase 1A) — not `@PreAuthorize` SpEL because:

1. Domain-level guard returns clean `BusinessException("PARENT_NOT_LINKED", FORBIDDEN)` (FE error code stable).
2. Boolean-exists query is cheaper than join-fetch (no PII leak about child identity).
3. Reuse existing repository method — single SQL pattern across all parent endpoints.

**Future scope (Wave 106+):** introduce `@authz.hasAccessToChild(childId)` SpEL helper bean per Wave 105 plan Risk #5 mitigation. Wave 105 ships service-level guard; helper Wave 107 (full RBAC refactor).

**IT scenarios (`ParentPaymentControllerTest`):**

1. `firstRequest_creates_payment_and_records_zalo_stub` — linked parent → 201 + Idempotent-Replay: false + Zalo OA called
2. `replay_returns_cached_payment_no_new_charge` — same key replayed → 200 + Replay: true + `createPayment` NEVER called
3. `cross_child_spoof_returns_403` — Linh linked to A spoofs B → 403 + zero downstream calls
4. `missing_idempotency_key_returns_400` — header absent → 400 IDEMPOTENCY_KEY_REQUIRED
5. `missing_parent_header_returns_401` — gateway didn't forward parentId → 401 AUTH_REQUIRED

All 5 PASS (Maven verify output: `Tests run: 5, Failures: 0, Errors: 0`).

## VietQR idempotency design

```
Client → POST /api/v1/parent/children/{childId}/payments
       Idempotency-Key: <uuid-v4>
       X-User-Reference-Id: <parent_id>  (gateway-injected)
       Body: { invoiceId, amount, paymentMethod=VNPAY|MOMO|... }
            ↓
            ↓ requireValidKey() — 400 nếu absent/malformed
            ↓ requireParentId() — 401 nếu absent
            ↓ existsByParentIdAndStudentIdAndDeletedFalse() — 403 nếu unlinked
            ↓
            ↓ idempotencyService.lookup(tenantId, key)
            ↓
   ┌────────┴────────┐
   ↓ FOUND           ↓ NOT FOUND
   ↓                 ↓
return cached      paymentService.createPayment(req, REAL_parentId)
PaymentResponse    └─ Bucket E1 fix lands BE-side (controller line 49 hardcoded 1L
+ 200 OK              KHÔNG affect parent path — we pass real parentId)
+ X-Payment-          ↓
Idempotent-Replay:  recordFirstWrite(...) → race-safe via UNIQUE
true                  ↓
                    ┌─┴─┐
                    ↓ OK  ↓ DuplicateKey
                    ↓     ↓ lookup() again → return winner
                    ↓     ↓
                    return 201 / 200 + X-Payment-Idempotent-Replay: false/true
                    + zaloOaNotificationService.recordPaymentConfirm() (REQUIRES_NEW)
```

State machine row în `payment_idempotency_keys`:
- `(instance_id, idempotency_key)` PK candidate via UNIQUE constraint
- `payment_id` FK-soft → `payments(id)` (FK not enforced yet pending Bucket E user_id fix)
- `qr_payload` cached so replay returns SAME QR string (no new VietQR API call)
- `expires_at` = `created_at + 24h` matches VietQR partner-bank txn expiry window
- Background sweeper (future GAP) deletes expired rows

## Zalo OA stub interface contract

3 event types — payload schemas:

```json
PARENT_INVITE_SENT:    { "invitationId": <long>, "template": "parent_invite_v1",     "greeting": "Kính gửi quý phụ huynh" }
PAYMENT_CONFIRM:       { "invoiceId":    <long>, "amountVnd": <long>, "childName": <str>,
                         "template": "payment_confirm_v1",  "greeting": "Kính gửi quý phụ huynh" }
ATTENDANCE_ALERT:      { "childId":      <long>, "status": <str>,    "template": "attendance_alert_v1",
                         "greeting": "Kính gửi quý phụ huynh" }
```

Per VN-localization §2 email tone matrix row "Parent": greeting `Kính gửi quý phụ huynh` (very formal). Wave 106 ZNS dispatcher uses this as template variable.

Outbox row state machine: `PENDING → DISPATCHED | FAILED | SKIPPED`. Wave 105 stub stays at PENDING. Wave 106 worker introduces transitions + retry semantics (`attempt_count` column).

## PDPL Art 11 parental-consent-on-behalf-of-child variant

**Existing state (NO new schema needed):** V56 (Wave 19 GAP-321c) shipped `parent_student_links.parental_consent JSONB` with shape:

```json
{"fields": {"transcript": true, "attendance": false, ...}, "version": 1, "updatedAt": "..."}
```

`ConsentService.checkConsent(parentId, childId, field)` enforces per-parent per-field-per-child consent. Per `ParentTranscriptServiceImpl` §guard 4-5:
- 403 `PARENT_CONSENT_REQUIRED` if `fields[field]` not set or false.
- 403 `RECONSENT_REQUIRED` if `version < requiredVersion` (policy version bump).

**PDPL Art 11 angle (legal interpretation):** Parents are legal representatives của data subject (child <16t). The 3-layer consent already shipped is:
1. **Data subject base consent** — `parent_invitations.consent_given` field
2. **Per-parent per-field consent** — `parental_consent.fields` JSONB
3. **Version-aware re-consent** — `parental_consent.version` vs policy `requiredVersion`

Technical implementation Wave 105 = **NO NEW SCHEMA, NO NEW LOGIC** — existing surface adequate.

**Legal sign-off (GAP-156 chain):** Phase 1 BETA acceptable per `business-logic-review.md` decision matrix — `v1 pending counsel review` disclaimer permitted for non-K-12 (CLAUDE.md current phase locked 2026-05-06). Counsel engagement required cho Phase 3 K-12 (per ROADMAP Phase 3 trigger conditions). **Wave 105 does NOT need new legal sign-off** — extends existing consent model only.

## Follow-up gaps filed

| Gap ID | Title | Priority | Trigger |
|---|---|---|---|
| **GAP-705** (this PR) | Wave 106 — VietQR partner-bank integration (replaces V61 stub) | P1 | Wave 105 BUcket D stub ship |
| **GAP-706** (this PR) | Wave 106 — Zalo OA ZNS API dispatcher (reads V61 outbox) | P1 | Wave 105 stub ship |
| **GAP-707** (this PR) | Mobile FE Parent persona walk — Docker WSL2 ngrok tunnel OR defer post-AWS-restore | P2 | Local Docker WSL2 quirk per wave plan §1 Risk #3 |
| **GAP-286** (existing) | Full Zalo OA partner channel integration | P1 | Already filed; Wave 105 ships stub portion |
| **GAP-156** (existing) | Legal sign-off chain Phase 1 BETA scope | P2 | Already filed; Wave 105 NOT adding new legal scope |

## Mobile walk defer per `pre-handoff-self-test-completeness.md` §5 override

```
LOCAL_SMOKE_SKIP: mobile-walk — Docker WSL2 quirk per wave plan §1 Risk #3
                  (FE port not reachable from host WSL2 browser; ngrok tunnel
                  defer Wave 106 OR post-AWS-restore live verify)
LOCAL_SMOKE_FOLLOWUP: GAP-707 — Mobile FE Parent persona walk
```

BE endpoint + IT walk shipped; mobile UI walk deferred. Per `vn-localization-audit-checklist.md` §2 mobile-only parent reality: when Wave 106 unblocks mobile walk, Parent UI MUST satisfy `user-manual-content-standard.md` §2 row 14 (mobile responsive ≥360px viewport, touch targets ≥44×44px).

## Effort estimate vs wave plan

| Phase | Estimate (wave plan §2) | Actual |
|---|---|---|
| State-check (heavy — many surfaces) | ~1h | ~45min — existing patterns from `ParentTranscriptServiceImpl` accelerated discovery |
| V61 migration design + write | ~1h | ~30min |
| Zalo OA stub interface + impl | ~1.5h | ~45min |
| ParentPaymentController + IdempotencyService | ~2h | ~1.5h |
| IT test (5 scenarios) | ~1.5h | ~1h (1 fix re: BusinessException stubbing) |
| Audit doc + handoff | ~1h | ~45min |
| **Total** | **~8h budgeted** | **~5h actual** |

Time savings primarily from PATTERN REUSE (`ParentStudentLinkRepository.existsByParent...` + V56 consent + `BaseEntity` instanceId multi-tenant). State-check before implementation paid off.

## Handoff to Bucket E security cluster

**B1+D1 (PaymentController.userId=1L hardcoded lines 49 + 69):** NOT FIXED here per scope guidance. Bucket E owns. Wave 105 Bucket D `ParentPaymentController` passes REAL `parentId` (from `X-User-Reference-Id` gateway header) to `paymentService.createPayment(request, parentId)` — so Bucket E fix at `PaymentController.createPayment` line 49 (hardcoded `1L`) lands independently:

```java
// Bucket E fix scope (PaymentController.java line 41-52):
@PostMapping
public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
    @Valid @RequestBody CreatePaymentRequest request,
    Authentication authentication) {  // ← inject from Spring Security
    Long userId = extractUserIdFromJwt(authentication);  // ← Bucket E1 fix
    PaymentResponse response = paymentService.createPayment(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
}
```

Bucket E IT test should verify same flow uses REAL userId (not 1L). The new `ParentPaymentController` path is parallel — both controllers will share fixed `PaymentService` signature once Bucket E1 lands; no interface change needed.

## References

- Wave 105 plan: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` §3 Bucket D
- Outside-in audits: `2026-05-22-wave-105-{persona-simulation,vn-saas-benchmark,failure-mode-matrix}.md`
- Sister rules applied: `audit-service-isolation.md` v1.0.0 (REQUIRES_NEW), `pre-handoff-self-test-completeness.md` §2.6 + §2.7, `vn-localization-audit-checklist.md` §2 + §3 + §4, `design-patterns.md` §3.5 Outbox + §3.11 audit isolation
- Existing patterns reused: `ParentTranscriptServiceImpl` (multi-child authz), V56 (parental_consent), `BaseEntity` (multi-tenant via instanceId)
