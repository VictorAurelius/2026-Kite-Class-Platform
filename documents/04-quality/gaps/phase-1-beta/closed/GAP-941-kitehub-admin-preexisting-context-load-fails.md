# GAP-941: kitehub-admin preexisting Spring context-load test failures block strict-warnings CI

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 PR #2150 merge surfaced cross-module ambiguous mapping)
**Closed:** 2026-06-04 (this PR — removed 3 deprecated payment endpoints from `AdminController` in kitehub-admin; ownership transferred to new `AdminPaymentController` in kitehub-subscription per PR #2150 UC-SUB-07)
**Affects:** Every PR touching `kitehub/kitehub-admin/**` OR transitively `kitehub-subscription/**` — Test KiteHub Admin Service strict-warnings job FAILs (context-load error blocks 8 tests at boot). `ADMIN_MERGE_OVERRIDE` trailer accumulation 5+ PRs.

## Problem

Trên `main` HEAD post-PR #2150 (`51564d82 feat(wave-flow-kh3-pre-walk-be): admin payment controller`), kitehub-admin Spring context fails to bootstrap → 8 tests error:

```
Caused by: BeanCreationException: Error creating bean with name 'requestMappingHandlerMapping':
  Ambiguous mapping. Cannot map 'adminPaymentController' method
    com.kitehub.subscription.controller.admin.AdminPaymentController#confirm(UUID, AdminConfirmPaymentRequest)
  to {POST [/api/platform/admin/payments/{id}/confirm]}:
  There is already 'adminController' bean method
    com.kitehub.admin.controller.AdminController#confirmPayment(UUID, ConfirmPaymentRequest) mapped.
```

8 tests affected:
- `com.kitehub.admin.controller.AdminControllerTest` — 7 tests (full `@SpringBootTest`; payload-agnostic, all fail at context load)
- `com.kitehub.admin.KiteHubAdminApplicationTest.contextLoads` — 1 test

PR #2150 (Wave flow-kh3 pre-walk BE) shipped a new `AdminPaymentController` in kitehub-subscription module mapping the same 3 endpoints (`/api/platform/admin/payments/{pending,{id}/confirm,{id}/reject}`) that the legacy `AdminController` in kitehub-admin already owned. kitehub-admin pulls kitehub-subscription as a Maven dependency → both controllers load in the same Spring context → ambiguous mapping → context bootstrap fails → every `@SpringBootTest` in kitehub-admin errors.

Tiền lệ: GAP-735 (kiteclass-core Spring context-load — đóng Wave meta-3) + GAP-937 (kitehub-subscription Mockito UnnecessaryStubbing — đóng PR #2155). Same class repeats trên kitehub-admin lần này.

## Root Cause

PR #2150 intentionally took over the manual VietQR payment-confirm path because it now drives `SubscriptionService.applyPendingUpgrade` inside the same transaction (UC-SUB-07, co-locating controller with service eliminates cross-module hop). The new `AdminPaymentController` was correct to ship — but PR #2150 did NOT remove the now-duplicate methods from the deprecated `AdminController` in kitehub-admin. The deprecation javadoc (`@Deprecated since v1, forRemoval=true, sunset 2026-09-30, GAP-654`) implies eventual removal, but Spring's `RequestMappingHandlerMapping` cannot tolerate same-path duplicates at runtime regardless of deprecation status.

Net: 3 endpoints in `AdminController` (`getPendingPayments` + `confirmPayment` + `rejectPayment`) had to migrate out 4 months ahead of the broader `/api/platform/admin` prefix sunset because PR #2150 shipped the replacement.

## Proposed Fix

Remove the 3 overlapping payment endpoint methods + accompanying `PaymentService` injection + unused imports/DTOs from `AdminController` in kitehub-admin. Leave the other deprecated endpoints (instances/dashboard/revenue) in place — they're still serving sunset traffic per GAP-654 until 2026-09-30. The new `AdminPaymentController` in kitehub-subscription continues to serve `/api/platform/admin/payments/**`; frontend mocks (`test-helpers.ts:217`, `mock-api-routes.ts:493`) require no change because the path is preserved.

DTOs `kitehub-admin/dto/ConfirmPaymentRequest.java` + `RejectPaymentRequest.java` become orphan but are not strictly causing failure — leaving them in place (dead-code cleanup is out of this gap's scope; strict-warnings does not flag unused classes, only unused imports/locals).

## Acceptance Criteria

- [x] `cd kitehub && ./mvnw -pl kitehub-admin verify -P strict-warnings` exits 0 — verified locally: `Tests run: 63, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`
- [x] Both originally-failing tests pass (`AdminControllerTest` 7 tests + `KiteHubAdminApplicationTest.contextLoads`) — confirmed in the 63 total above; `KiteHubAdminApplicationTest in 3.664 seconds` PASS
- [x] CI job "Test KiteHub Admin Service (strict-warnings — GAP-245)" green on this PR without `ADMIN_MERGE_OVERRIDE` trailer

## Log

- **2026-06-04** Fix shipped via this PR. Investigation per `release-fix-retry-budget.md` §3.5: ran `./mvnw -pl kitehub-admin test -Dtest='AdminControllerTest,KiteHubAdminApplicationTest'` against fresh `origin/main`; surfaced cross-module ambiguous mapping (NOT a stale mock issue as initially hypothesized — every one of the 8 errors shared the same Spring context-load failure). Root cause = PR #2150 took over the path without removing the deprecated owner. Fix touched ONLY `kitehub-admin/.../AdminController.java`:
  - Removed `import com.kitehub.admin.dto.ConfirmPaymentRequest` + `RejectPaymentRequest`
  - Removed `import com.kitehub.subscription.dto.PaymentResponse` + `com.kitehub.subscription.service.PaymentService`
  - Removed unused `jakarta.validation.Valid` + `PostMapping` + `RequestBody` imports
  - Removed `private final PaymentService paymentService;` field
  - Removed 3 endpoint methods (`getPendingPayments`, `confirmPayment`, `rejectPayment`) + their `SubscriptionDataChangedEvent` publishers
  - Replaced the `// ==================== PAYMENT ADMIN APIs ====================` block with an explanatory comment pointing at `AdminPaymentController` in kitehub-subscription as the new canonical owner + cross-link to GAP-941 + PR #2150 + GAP-654 sunset context.
  - Production code touched = 1 file (`AdminController.java`); test code = 0 files (the failing tests passed once the context loaded — they were never broken at the test level).
  - Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3: grepped `@RequestMapping("/api*` across kitehub-admin/platform/branding/email — surfaced 4 controllers under `/api/v1/branding/jobs` (all in kitehub-branding, distinct sub-paths — EXEMPT, same-module routing). No other cross-module duplicates.

## Related

- Triggered by: PR #2150 (`51564d82 feat(wave-flow-kh3-pre-walk-be): admin payment controller + VietQR mock-mode default`)
- Tiền lệ same class: GAP-735 (kiteclass-core, đóng Wave meta-3); GAP-937 (kitehub-subscription Mockito, đóng PR #2155 this session)
- Related path-ownership transition: GAP-654 (`/api/platform/admin/*` → `/api/v1/admin/*` sunset 2026-09-30); GAP-938 (dead `AdminApiKeyInterceptor` removal, PR #2152)
- Replacement controller: `com.kitehub.subscription.controller.admin.AdminPaymentController` (Wave flow-kh3, UC-SUB-07)
- Rule cite: `.claude/rules/admin-merge-discipline.md` v1.0.3 §4 `ADMIN_MERGE_OVERRIDE` pattern (this PR breaks the override-trailer cycle — no longer needed for kitehub-admin)
- Rule cite: `.claude/rules/release-fix-retry-budget.md` §3.5 investigation phase mandate (applied — empirical config + Spring stack trace read before drafting fix)
- Rule cite: `.claude/rules/cross-flow-bug-class-sweep.md` §3 cross-flow sweep evidence (executed — no sister duplicates outside kitehub-branding intra-module)
