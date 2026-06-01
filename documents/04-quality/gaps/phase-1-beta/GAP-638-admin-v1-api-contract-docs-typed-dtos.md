# GAP-638: Admin v1 api-contract.md documentation gap + typed DTOs

**Status:** 🟡 PARTIAL 30% — Wave 97 Bucket B1 shipped 3-layer admin/ docs foundation (1/6 AC); B2+B3 typed DTOs + controller refactor + legacy @Deprecated tracked **GAP-654** (Wave 98 candidate); AC6 pre-handoff live verify blocked **GAP-612** (AWS suspension)
**Priority:** 🟠 P1
**Domain:** Backend (API contract + docs sync)
**Detected:** 2026-05-18 (Wave 92 post-wave audit suite per GAP-619)
**Related Audits:** [documents/04-quality/audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md](../audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md)

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|---|---|---|
| 6 endpoints `/api/v1/admin/{instances,payments,revenue}` | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/v1/` | ✅ shipped Wave 92 Bucket D |
| Legacy `/api/platform/admin/*` docs | `documents/01-business/kitehub/admin/api-contract.md` | ✅ shipped — covers legacy only |
| v1 endpoints docs | `documents/01-business/kitehub/admin/api-contract.md` | ❌ missing — zero hits cho `/api/v1/admin/` |
| Typed DTOs (`PaymentsSummaryResponse`, `RevenuePeriod` enum) | `kitehub-admin/src/main/java/com/kitehub/admin/api/v1/dto/` | ❌ missing — currently `Map<String, Object>` return |
| Legacy `@Deprecated` + Sunset header | `kitehub-admin/src/main/java/com/kitehub/admin/api/platform/` | ❌ missing — chưa mark legacy deprecation |

**Grep commands run:**

```bash
grep -rn "/api/v1/admin" documents/01-business/
grep -rn "PaymentsSummaryResponse\|RevenuePeriod" kitehub/kitehub-admin/src/main/java/
grep -rn "@Deprecated\|Sunset" kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/platform/
```

## Problem

Audit API Contract Wave 92 Bucket D (2026-05-18) phát hiện cross-layer drift P1:

1. **Documentation gap:** 6 endpoints `/api/v1/admin/{instances,payments,revenue}` shipped Wave 92 Bucket D nhưng **zero hits** trong `documents/01-business/kitehub/admin/api-contract.md`. Legacy docs chỉ cover `/api/platform/admin/*` endpoints. Vi phạm `contract-first-for-cross-layer.md` §3 mandate "code và doc cùng PR".

2. **Untyped DTOs:** Controllers hiện return `Map<String, Object>` cho payments summary + revenue period thay vì typed DTOs. Vi phạm `roles/api-contract.md` mandate typed contracts cho FE consumption.

3. **Legacy not deprecated:** `/api/platform/admin/*` endpoints chưa mark `@Deprecated` + thiếu `Sunset` header. FE clients có thể vẫn dùng legacy endpoints mà không có migration warning.

## Context

Wave 92 Bucket D shipped 3 admin controllers scaffold + UI consumption. Audit phát hiện docs synchronization gap — code merged mà docs không update cùng PR. Phase 1 BETA gate cho API Contract audit ≥80 cần address để đạt baseline.

## Proposed Fix

### Step 1: Document 6 endpoints trong api-contract.md

Thêm section `## Admin v1 endpoints` trong `documents/01-business/kitehub/admin/api-contract.md`:
- `GET /api/v1/admin/instances` + query params + response shape
- `GET /api/v1/admin/instances/{id}` + path param + response
- `GET /api/v1/admin/payments/summary` + query params + typed response
- `GET /api/v1/admin/payments` + pagination + filter params
- `GET /api/v1/admin/revenue/by-period` + `period` enum + response
- `GET /api/v1/admin/revenue/total` + response

### Step 2: Define typed DTOs

```java
// kitehub-admin/src/main/java/com/kitehub/admin/api/v1/dto/
public record PaymentsSummaryResponse(
    BigDecimal totalAmount,
    int totalCount,
    String currency,
    Instant periodStart,
    Instant periodEnd
) {}

public enum RevenuePeriod {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
}
```

### Step 3: Mark legacy @Deprecated + Sunset header

```java
@Deprecated(since = "v1", forRemoval = true)
@RestController
@RequestMapping("/api/platform/admin")
public class LegacyAdminController {
    // Add response header: Sunset: Sat, 30 Sep 2026 23:59:59 GMT
}
```

## Acceptance Criteria

- [ ] api-contract.md có section `## Admin v1 endpoints` document đủ 6 endpoints với request/response shapes
- [ ] 2 typed DTOs (`PaymentsSummaryResponse` record + `RevenuePeriod` enum) defined + wired vào controllers
- [ ] Controllers return typed responses thay vì `Map<String, Object>`
- [ ] Legacy `/api/platform/admin/*` controllers mark `@Deprecated(since = "v1", forRemoval = true)` + emit `Sunset` response header
- [ ] OpenAPI spec sync (auto-generated từ controller annotations)
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md` §2.x

## Related

- **Audit origin:** [documents/04-quality/audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md](../audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** [GAP-619](GAP-619-wave-92-post-wave-audit-suite.md)
- **Sister gap:** [GAP-637](GAP-637-admin-v1-controllers-preauthorize-missing.md) (same controllers, security focus)
- **Rules:**
  - `.claude/rules/contract-first-for-cross-layer.md` §3
  - CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure" (Living Docs mandate)

## Log

- 2026-06-01 — **Wave meta-8 Bucket B SCOPE-REVISE:** SCOPE-REVISE — Wave 97 Bucket B1 3-layer admin/ docs foundation shipped (README + rules BR-ADMIN-V1-001..003 + use-cases UC-001..006 + api-contract 6 endpoints); B2 typed DTOs + controller refactor + legacy @Deprecated defer next session. AC structure needs sync CSV completion_pct adjusted to 20%; gap body Status/AC reflect documented scope BEFORE Wave meta-7 audit — re-read audit artifact for current empirical reality. Source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-d-p1-partial.md`.

- **2026-05-18** — Initial write-up. Filed từ Wave 92 post-wave audit suite (GAP-619) API Contract audit finding P1. State-check confirmed `grep "/api/v1/admin" documents/01-business/` returns 0 hits — endpoints shipped Wave 92 Bucket D nhưng api-contract.md không update cùng PR. Phase 1 BETA blocker — API Contract audit gate ≥80.
