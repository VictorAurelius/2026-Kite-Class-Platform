---
title: Wave 105 Persona Walk — Outside-In Failure-Mode Matrix Audit
status: complete
audience: dev
created: 2026-05-22
phase: phase-1-beta
wave: 105
gaps: []
scope: 3-axis failure-mode matrix (X user-action × Y timing × Z data) trên Wave 105 draft Persona Walk plan — 18 scenarios audited
methodology: simulation-gap-finder skill — code review 5 controllers + matrix generation, identify edge cases beta users WILL hit
---

# Wave 105 Persona Walk — Failure-Mode Matrix Audit

## Methodology

Per `outside-in-coverage-trigger.md` v1.1.0 §3 Bước 3 — background agent generates failure scenarios qua combination 3 axis variants (X user action / Y timing / Z data) trên 4 persona buckets, code review key controllers, identify edge cases beta user WILL hit (NOT theoretical edge cases).

**3 axes:**

- **X — User action variants:** typo email, wrong subdomain, forgot password, back button, refresh, multi-tab, expired link, double-click, no JS, no cookies
- **Y — Timing variants:** network slow 3G, immediate retry, wait 5min/24h/14d, concurrent admin actions
- **Z — Data variants:** existing email/subdomain, Unicode edge, very long input, SQL injection, XSS, diacritic NFD vs NFC, phone formats, email + symbols

## Scenario matrix — 18 scenarios

| # | Persona+Action (X) | Timing (Y) | Data (Z) | Likely actual behavior | Sev | Mitigation |
|---|---|---|---|---|---|---|
| **A1** | Vy submits beta-request 2× (double-click) | Within 1s | Same email | `BetaAccessService.submitRequest` no `@Unique(email,status=PENDING)` constraint → 2 rows likely; rate-limit only kicks in 2nd request after delay | **P0** | DB unique partial index `(email) WHERE status=PENDING`; FE button debounce 1s |
| **A2** | Vy refresh page mid-multi-step | Wait 5s, page reload | Token in URL `?token=UUID` | Token valid (24h expiry), pre-fill OK — likely PASS | P2 | None — happy path |
| **A3** | Vy clicks invite link after 25h | Token >24h | Valid UUID | `validateToken` returns 404 invalid; no "request new token" CTA visible | **P1** | Add "Token expired — request new invite" CTA + endpoint |
| **A4** | Vy submits form with XSS in name | Immediate | `<script>alert(1)</script>` | `@Valid` likely passes (no length/charset cap); admin coordinator sees raw value → **stored XSS if FE renders unescaped** | **P0** | Sanitize on input; React auto-escapes BUT admin panel framework unverified |
| **A5** | Vy submits with email typo (no `@`) | Immediate | `hong.example.vn` | `@Email` validator catches → ProblemDetail 400 Vietnamese OK if `messages.properties` localized; else English leak | **P1** | Verify `messages_vi.properties` covers `jakarta.validation.constraints.Email` |
| **B1** | Hằng creates payment for invoice | Immediate after login | Valid invoice | `PaymentController.createPayment` line 49: `paymentService.createPayment(request, 1L)` — **HARDCODED userId=1L** (TODO "extracted from JWT at Gateway" never implemented) | **P0 CRIT** | Inject Authentication principal + extract userId — **BLOCKER FOR BETA** |
| **B2** | Hằng bulk-import 1000 students at once | Immediate | CSV 1000 rows | No batch size cap; likely OOM OR txn timeout RDS Free Tier; no progress indicator | **P1** | Cap at 200/batch + async job + progress endpoint |
| **B3** | Hằng + Manager approve same beta-request | Concurrent | Same id, 2 admin sessions | `BetaAccessController.approve` uses `IllegalStateException → 409` — but no optimistic lock; race may flip APPROVED twice with 2 invite tokens sent | **P1** | `@Version` field on `BetaAccessRequest` + `SELECT FOR UPDATE` |
| **B4** | Hằng uploads logo 50MB | Slow 3G | Large binary | No `multipart.max-file-size` cap verified; default 1MB Spring reject; error message English `MaxUploadSizeExceededException` | **P1** | Cap explicit 5MB + VN error message |
| **B5** | Hằng enrolls student into FULL class | Immediate | classId at capacity | `EnrollmentService.enrollStudent` claims "validates capacity" — needs verify race-condition safe; 2 concurrent enrolls likely both succeed without `SELECT FOR UPDATE` | **P0** | Optimistic lock OR pessimistic on class.currentCount |
| **C1** | Tâm accepts staff-invitation 2× (refresh) | Refresh after click | Same token | Same idempotency gap as A1 — invitation flips ACCEPTED twice, may issue 2 passwords | **P1** | Token single-use enforcement |
| **C2** | Tâm enrolls 2FA TOTP, clock skew ±90s | Immediate | TOTP code | `pre-handoff-self-test-completeness.md` §2.10 (c) mandates ±60s clock skew; 90s likely fails — Wave 104.5 verified path but boundary untested | **P1** | Test ±60s + ±90s + reject ±120s |
| **C3** | Tâm records attendance for class he's NOT assigned | Immediate | Wrong classId in URL | Tenant isolation likely PASS; per-class teacher scope `@PreAuthorize("hasAccessToClass(...)")` — A01 OWASP gap, not verified | **P0** | Per-resource authz check beyond tenant isolation |
| **C4** | Tâm uploads assignment file `script.exe` rename `.pdf` | Immediate | Magic bytes mismatch | `pre-handoff-self-test-completeness.md` §2.5 (a) MIME validation server-side — likely NOT done (Tika not visible); ClamAV definitely not | **P0** | Magic-byte check + ClamAV stub Phase 2 |
| **D1** | Linh pays invoice 2× (double-click VietQR submit) | Within 1s | Same invoiceId | `PaymentController.createPayment` hardcoded `userId=1L` (B1) + no idempotency key — 2 payment rows, possibly 2 QR codes; webhook race undefined | **P0 CRIT** | Idempotency-Key header + DB unique (invoiceId, status=PENDING) |
| **D2** | Linh's payment webhook arrives 24h late | 24h after payment | Valid signature | `PaymentWebhookController` signature verify likely OK; idempotency on webhook handler unclear — may process expired payment row | **P1** | Webhook signature TTL window + state-machine guard |
| **D3** | Linh has 2 children, views child A grade page | Immediate | childId=B in URL | Parent-child scope: `ParentTranscriptController` — must verify `parent.children.contains(childId)`; if missing → **A01 cross-child leak** | **P0** | Per-child authz check; OWASP A01 |
| **D4** | Linh consents PDPL form, browser back, re-consent | Back button + refresh | Same form | Consent persistence per PDPL Art 11 — likely `@CreatedDate` insert each time; multiple rows OK legally but UX confusion | P2 | Idempotent: 1 consent row per (parent, version) |

## Top 5 P0 — MUST FIX Wave 105

1. **B1/D1 — PaymentController hardcoded `userId=1L`** (`/home/nguyenvankiet/projects/2026-Kite-Class-Platform/kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/controller/PaymentController.java:49,69`). Beta blocker. Comment says "extracted from JWT at Gateway" but never wired. Every Owner/Parent payment writes user_id=1. Audit trail broken. **Fix: inject Authentication principal, ~30 min.**

2. **A4 — Stored XSS** in beta-request name/orgName fields. Admin panel renders these — need React auto-escape verify + input sanitization. **Risk: admin session takeover.**

3. **A1 — Beta-request idempotency missing.** Partial unique index `(email) WHERE status=PENDING` prevents double-submit; 1 line DDL.

4. **B5 — Enrollment race on FULL class.** Needs `@Version` optimistic lock OR `SELECT FOR UPDATE`. Without it, capacity check is TOCTOU.

5. **C3/D3 — Per-resource authz (A01 OWASP).** Tenant isolation ≠ per-resource (teacher↔class, parent↔child). Add `@PreAuthorize("@authz.hasAccessTo...")`. Per `pre-launch-owasp-rest-hardening-checklist.md` §2.1 P0.

## Top 5 P1 — recommend Wave 106

- **C4** File upload magic-byte validation + ClamAV stub (per `pre-handoff-self-test-completeness.md` §2.5)
- **C2** 2FA clock skew ±60s/±90s boundary test
- **B3** Concurrent admin approve race + optimistic lock
- **B4** Multipart upload size cap + VN error i18n
- **D2** Webhook idempotency + signature TTL

## 3 systemic patterns

1. **No idempotency anywhere on POST mutations.** Beta-request, payment, enrollment, staff-invitation accept — all double-submit creates duplicates. **Root fix: introduce `Idempotency-Key` header convention + `@IdempotencyKey` interceptor; partial unique indexes per resource.**

2. **JWT principal extraction inconsistent / missing.** `PaymentController` hardcodes `1L`; comment claims gateway extraction but never landed. **Likely repeated across other controllers — grep `1L` audit needed for beta-blocker scan.**

3. **Per-resource authz (OWASP A01) untested.** Tenant isolation matrix tested (5/218 endpoints) but per-resource (teacher↔class, parent↔child) not in Wave 105 plan. **Add Bucket F: A01 spot-check on 8 endpoints across 4 personas.**

## Key files

- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/controller/PaymentController.java:49,69` (hardcoded userId=1L)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` (no idempotency on submit)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/enrollment/controller/EnrollmentController.java` (capacity race unverified)

## Verdict

Wave 105 plan as drafted catches happy-path; misses **~5 P0 + ~5 P1 edge cases** that real beta users WILL hit in week 1. Recommend adding **Bucket E: Security P0 cluster fixes** before invite go-live.
