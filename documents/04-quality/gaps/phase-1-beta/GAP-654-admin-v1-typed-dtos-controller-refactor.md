# GAP-654: Admin v1 typed DTOs + controller refactor + legacy @Deprecated

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (API contract typed responses + deprecation)
**Detected:** 2026-05-18 (Wave 97 closure orphan-cleanup — splits GAP-638 deferred portion)
**Parent:** [GAP-638](closed/GAP-638-admin-v1-api-contract-docs-typed-dtos.md) PARTIAL 30% Wave 97 Bucket B1

## Current State (verified 2026-05-18)

| Piece | Path | Status |
|---|---|---|
| admin/ 3-layer docs foundation | `documents/01-business/kitehub/admin/{README,rules,use-cases,api-contract}.md` | ✅ shipped Wave 97 Bucket B1 (PR #1543) |
| 3 controllers @PreAuthorize | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/` | ✅ shipped Wave 97 Bucket A (PR #1540) |
| Typed DTOs (PaymentsSummaryResponse + RevenueSummaryResponse + RevenuePeriod enum) | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/dto/` | ❌ missing — controllers return `Map<String, Object>` |
| Controllers refactor Map → typed DTO | `Admin{Payments,Revenue}Controller` getXxxSummary() methods | ❌ pending |
| Legacy `/api/platform/admin/*` `@Deprecated(since="v1", forRemoval=true)` | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/platform/` | ❌ not annotated |
| Sunset HTTP header interceptor | `kitehub-admin/src/main/java/com/kitehub/admin/api/platform/` | ❌ not implemented |
| OpenAPI spec auto-sync (from typed DTO annotations) | `kitehub/kitehub-admin/src/main/resources/openapi/` | ❌ pending B2 typed DTOs |

## Problem

Wave 97 Bucket B intended ship full GAP-638 scope (docs + DTOs + legacy deprecation) trong 1 wave. Per best-practice escalation (context-thrashing risk after bg-agent fail 2x), scope split into B1 (docs only — shipped) + B2/B3 (DTOs + deprecation — this gap).

GAP-638 closed PARTIAL 30% với reference "DEFER B2/B3 next session". Per `gap-done-discipline.md` §3 PARTIAL exit ramp + `wave-closure-scope-completeness.md` §3 — deferred portion MUST have follow-up gap. This file fixes orphan.

## Proposed Fix

### Step 1: Typed DTO records

`kitehub/kitehub-admin/src/main/java/com/kitehub/admin/dto/`:
- `PaymentsSummaryResponse.java` — record (totalAmount + totalCount + currency + periodStart + periodEnd + pendingCount + completedCount)
- `RevenueSummaryResponse.java` — record (ytdRevenueVnd + currentMonthRevenueVnd + previousMonthRevenueVnd + growthPercentage + currency + period + asOfDate)
- `RevenuePeriod.java` — enum (DAILY / WEEKLY / MONTHLY / QUARTERLY / YEARLY)

### Step 2: Controller refactor Map → DTO

Edit:
- `AdminPaymentsController.getPaymentsSummary()` — change return `ResponseEntity<Map<String, Object>>` → `ResponseEntity<PaymentsSummaryResponse>`
- `AdminRevenueController.getRevenueSummary()` — change return type + accept `@RequestParam(defaultValue="MONTHLY") RevenuePeriod period`

### Step 3: Legacy `/api/platform/admin/*` deprecation

Edit controllers under `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/platform/`:
- Add `@Deprecated(since = "v1", forRemoval = true)` class-level
- Implement `SunsetHeaderInterceptor` adding `Sunset: Sat, 30 Sep 2026 23:59:59 GMT` + `Link: </api/v1/admin/{equivalent}>; rel="successor-version"` response headers

### Step 4: OpenAPI spec sync

Verify springdoc-openapi auto-generates schema từ typed DTO annotations (no manual file edit needed if `springdoc.api-docs.enabled=true`).

### Step 5: Update GAP-638 reference + close GAP-654

After this gap DONE → GAP-638 review remaining AC:
- AC2 typed DTOs ✅ (Step 1)
- AC3 controllers return typed ✅ (Step 2)
- AC4 legacy @Deprecated ✅ (Step 3)
- AC5 OpenAPI auto-sync ✅ (Step 4)
- AC6 pre-handoff §2.x — still BLOCKED by GAP-612 AWS suspension

→ GAP-638 PARTIAL 30% → PARTIAL 90% (only AC6 AWS-blocked). Flip DONE khi GAP-612 unblock + live verify PASS.

## Acceptance Criteria

- [ ] 3 typed DTO/enum files created trong `kitehub-admin/.../dto/`
- [ ] 2 controllers refactor return type Map → typed DTO
- [ ] Legacy controllers `@Deprecated(since="v1", forRemoval=true)` + Sunset interceptor
- [ ] `cd kitehub && ./mvnw -pl kitehub-admin verify -P strict-warnings` PASS
- [ ] OpenAPI spec auto-generates correct schema cho 2 typed responses
- [ ] GAP-638 reference Status updated (PARTIAL 30 → 90%)

## Effort estimate

~1-1.5 wave bucket scope (small): 5-7 file edits + mvn verify. Wave 98 candidate.

## Related

- **Parent gap:** [GAP-638](closed/GAP-638-admin-v1-api-contract-docs-typed-dtos.md) PARTIAL 30% Wave 97 Bucket B1 PR #1543
- **Sister gaps:** [GAP-637](closed/GAP-637-admin-v1-controllers-preauthorize-missing.md) (same controllers, security focus)
- **Future spec:** `documents/01-business/kitehub/admin/api-contract.md` §"Future typed DTO target (B2)" sections — pre-defined record shapes ready to copy
- **Rules:**
  - `.claude/rules/contract-first-for-cross-layer.md` §3
  - RFC 8594 (Sunset HTTP Header)

## Log

- **2026-05-18 (created):** Filed per `gap-done-discipline.md` §3 + `wave-closure-scope-completeness.md` §3 — orphan-cleanup for GAP-638 B2/B3 deferred portion (Wave 97 closure compliance fix).
