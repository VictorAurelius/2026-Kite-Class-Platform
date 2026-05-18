# Admin v1 — API Contract

**Domain:** Admin v1 endpoints (GAP-638 — Wave 97 Bucket B1 3-layer foundation)
**Source-of-truth controllers:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/`
**Last verified:** 2026-05-18 (Wave 97 Bucket B1 — docs only; DTO refactor defer B2)

---

## Common conventions

- **Auth:** Mọi endpoint require `Authorization: Bearer <JWT>` với role `PLATFORM_ADMIN` claim (per BR-ADMIN-V1-001)
- **Authorization:** Class-level `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` enforce role tại controller boundary (shipped Wave 97 Bucket A GAP-637)
- **Rate limit:** 30 req/min/admin per `kitehub.admin.api-v1.rate-limit-per-minute` config (gateway-enforced)
- **Pagination:** List endpoints support `?page={N}&size={M}` per BR-ADMIN-V1-002; max size = 200
- **Response format:** JSON Content-Type `application/json`; standard error envelope `{code, message, requestId, timestamp}` cho 4xx/5xx
- **Audit:** Mọi request auto-emit audit log row qua `AuditLogService.record()` (xem `admin-audit/api-contract.md`)

---

## Endpoint 1: GET /api/v1/admin/instances

**Use case:** UC-ADMIN-V1-001 — Liệt kê tenant instances paginated
**Controller:** `AdminInstancesController.listInstances()`

**Query parameters:**
| Param | Type | Required | Default | Mô tả |
|---|---|---|---|---|
| `page` | int | No | `0` | 0-indexed |
| `size` | int | No | `50` | Max 200 (clamped per BR-ADMIN-V1-002) |
| `sort` | string | No | `createdAt,desc` | Spring Data sort param |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "tenantSlug": "trung-tam-sky",
      "organizationName": "Trung tâm Anh ngữ Sky Education",
      "ownerId": "660e8400-e29b-41d4-a716-446655440000",
      "ownerEmail": "owner@sky-edu.example",
      "tier": "PREMIUM",
      "status": "ACTIVE",
      "createdAt": "2026-04-15T08:30:00Z",
      "verticalType": "CENTER"
    }
  ],
  "totalElements": 142,
  "totalPages": 3,
  "page": 0,
  "size": 50
}
```

**Error codes:**
| HTTP | Code | Khi nào |
|---|---|---|
| `400` | `INVALID_PAGE_PARAM` | `page` negative hoặc non-integer |
| `403` | `FORBIDDEN` | Role không phải PLATFORM_ADMIN |
| `429` | `RATE_LIMITED` | Vượt 30 req/min |
| `500` | `INTERNAL_ERROR` | DB connection fail |

---

## Endpoint 2: GET /api/v1/admin/instances/{id}

**Use case:** UC-ADMIN-V1-002 — Xem chi tiết một tenant instance
**Controller:** `AdminInstancesController.getInstanceById()`

**Path parameters:**
| Param | Type | Required | Mô tả |
|---|---|---|---|
| `id` | UUID | Yes | Instance UUID (path) |

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantSlug": "trung-tam-sky",
  "organizationName": "Trung tâm Anh ngữ Sky Education",
  "subdomain": "sky",
  "customDomain": "sky.edu.vn",
  "ownerId": "660e8400-e29b-41d4-a716-446655440000",
  "ownerEmail": "owner@sky-edu.example",
  "tier": "PREMIUM",
  "status": "ACTIVE",
  "trialExpiresAt": null,
  "subscriptionExpiresAt": "2027-04-15T00:00:00Z",
  "createdAt": "2026-04-15T08:30:00Z",
  "updatedAt": "2026-05-12T10:20:00Z",
  "deleted": false,
  "migrationPhase": "COMPLETED",
  "verticalType": "CENTER"
}
```

**Error codes:**
| HTTP | Code | Khi nào |
|---|---|---|
| `400` | `INVALID_ID` | UUID format invalid |
| `403` | `FORBIDDEN` | Non-admin role |
| `404` | `INSTANCE_NOT_FOUND` | Instance không tồn tại |
| `429` | `RATE_LIMITED` | Rate limit |

---

## Endpoint 3: GET /api/v1/admin/payments/pending

**Use case:** UC-ADMIN-V1-003 — Liệt kê pending payments
**Controller:** `AdminPaymentsController.getPendingPayments()`

**Query parameters:** `page`, `size`, `sort` (per common conventions)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440000",
      "tenantId": "550e8400-e29b-41d4-a716-446655440000",
      "tenantSlug": "trung-tam-sky",
      "amountVnd": 1500000,
      "currency": "VND",
      "paymentMethod": "BANK_TRANSFER",
      "status": "PENDING",
      "createdAt": "2026-05-15T14:30:00Z",
      "daysPending": 3,
      "paymentContent": "Sky Education tháng 5/2026",
      "qrCodeUrl": "https://kitehub.me/qr/payment/770e8400..."
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "page": 0,
  "size": 50
}
```

**Error codes:** Common 400/403/429/500

---

## Endpoint 4: GET /api/v1/admin/payments/summary

**Use case:** UC-ADMIN-V1-004 — Xem payments summary aggregated
**Controller:** `AdminPaymentsController.getPaymentsSummary()`

**Current implementation:** Returns `Map<String, Object>` — **DEFER B2** refactor → typed `PaymentsSummaryResponse` record.

**Query parameters:**
| Param | Type | Required | Default | Mô tả |
|---|---|---|---|---|
| `periodStart` | ISO date | No | (30 days ago) | Inclusive |
| `periodEnd` | ISO date | No | (today) | Inclusive |

**Response (200 OK):**
```json
{
  "totalAmount": 45000000,
  "totalCount": 30,
  "currency": "VND",
  "periodStart": "2026-04-18T00:00:00Z",
  "periodEnd": "2026-05-18T23:59:59Z",
  "pendingCount": 8,
  "completedCount": 22
}
```

**Future typed DTO target (B2):**
```java
public record PaymentsSummaryResponse(
    BigDecimal totalAmount,
    int totalCount,
    String currency,
    Instant periodStart,
    Instant periodEnd,
    int pendingCount,
    int completedCount
) {}
```

**Error codes:** Common 400/403/429/500

---

## Endpoint 5: GET /api/v1/admin/revenue

**Use case:** UC-ADMIN-V1-005 — Liệt kê revenue records
**Controller:** `AdminRevenueController.getRevenue()`

**Query parameters:** `page`, `size`, `sort` (per common conventions)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440000",
      "period": "2026-05",
      "totalAmountVnd": 45000000,
      "transactionCount": 30,
      "currency": "VND",
      "subscriptionCount": 28,
      "trialConvertCount": 2,
      "createdAt": "2026-05-31T23:59:59Z"
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "page": 0,
  "size": 50
}
```

**Error codes:** Common 400/403/429/500

---

## Endpoint 6: GET /api/v1/admin/revenue/summary

**Use case:** UC-ADMIN-V1-006 — Xem revenue summary aggregated
**Controller:** `AdminRevenueController.getRevenueSummary()`

**Current implementation:** Returns `Map<String, Object>` — **DEFER B2** refactor → typed `RevenueSummaryResponse` record + `RevenuePeriod` enum.

**Query parameters:**
| Param | Type | Required | Default | Mô tả |
|---|---|---|---|---|
| `period` | enum | No | `MONTHLY` | DAILY \| WEEKLY \| MONTHLY \| QUARTERLY \| YEARLY |

**Response (200 OK):**
```json
{
  "ytdRevenueVnd": 250000000,
  "currentMonthRevenueVnd": 45000000,
  "previousMonthRevenueVnd": 38000000,
  "growthPercentage": 18.4,
  "currency": "VND",
  "period": "MONTHLY",
  "asOfDate": "2026-05-18"
}
```

**Future typed DTO target (B2):**
```java
public record RevenueSummaryResponse(
    BigDecimal ytdRevenueVnd,
    BigDecimal currentMonthRevenueVnd,
    BigDecimal previousMonthRevenueVnd,
    double growthPercentage,
    String currency,
    RevenuePeriod period,
    LocalDate asOfDate
) {}

public enum RevenuePeriod {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
}
```

**Error codes:**
| HTTP | Code | Khi nào |
|---|---|---|
| `400` | `INVALID_PERIOD` | `period` không trong enum cho phép |
| `403` | `FORBIDDEN` | Non-admin role |
| `429` | `RATE_LIMITED` | Rate limit |

---

## Legacy deprecation policy (BR-ADMIN-V1-003)

Legacy endpoints `/api/platform/admin/*` (controllers tại `kitehub-admin/src/main/java/com/kitehub/admin/api/platform/`) **chưa marked deprecated** tại Wave 97 Bucket B1 — defer B2 cùng DTO refactor scope.

**Target B2 implementation:**
```java
@Deprecated(since = "v1", forRemoval = true)
@RestController
@RequestMapping("/api/platform/admin")
public class LegacyAdminController {
    // Interceptor adds:
    // - Sunset: Sat, 30 Sep 2026 23:59:59 GMT
    // - Link: </api/v1/admin/{equivalent}>; rel="successor-version"
}
```

**Sunset date:** `2026-09-30T23:59:59Z` per `kitehub.admin.legacy.deprecation-sunset-date` config.

**FE migration path:**
- 2026-05 (v1 ship): FE consumers MAY migrate
- 2026-06 (deprecation header shipping B2): FE warn logs nếu vẫn dùng legacy
- 2026-09-15 (T-15): Admin email reminder to remaining legacy consumers
- 2026-09-30 (sunset): Legacy endpoints return HTTP 410 Gone

---

## Cross-layer dependencies

Per `contract-first-for-cross-layer.md` §3 — contract này là source-of-truth cho:

| Layer | Consumer | Status |
|---|---|---|
| BE Java | 3 controllers in `kitehub-admin/.../controller/` | ✅ shipped Wave 92 Bucket D |
| BE Java | `@PreAuthorize` class-level annotation | ✅ shipped Wave 97 Bucket A (PR #1540) |
| BE Java | Typed DTOs (PaymentsSummaryResponse, RevenueSummaryResponse, RevenuePeriod enum) | ❌ DEFER B2 |
| BE Java | SecurityConfig X-User-Roles filter | ✅ shipped Wave 97 Bucket A |
| FE TypeScript | `kitehub-frontend/src/app/admin/**` consumer | ⚠️ scaffold-only Wave 92 (per GAP-641 carry) |
| Test | `Admin*ControllerSecurityTest` 403 cho non-admin roles | ✅ shipped Wave 97 Bucket A (6 tests) |
| Audit | Auto-emit qua `admin-audit` domain | ✅ infrastructure shipped Wave 92 + 3-layer docs Wave 97 Bucket C |

---

## Wave 97 Bucket B status

| Step | Description | Status |
|---|---|---|
| B1 | 3-layer admin/ docs foundation (README + rules + use-cases + api-contract) | ✅ this PR |
| B2 | Typed DTOs (PaymentsSummaryResponse + RevenueSummaryResponse + RevenuePeriod enum) + controller refactor Map → DTO | ⏳ DEFER next session |
| B3 | Legacy `/api/platform/admin/*` @Deprecated + Sunset header interceptor | ⏳ DEFER B2 cùng scope |
| B4 | OpenAPI spec auto-sync | ⏳ DEFER B2 |
| B5 | Pre-handoff self-test §2.x admin flow | ⏳ Block by GAP-612 AWS live verify (same as GAP-637) |

GAP-638 status → PARTIAL ~30% (1/6 AC done — docs foundation; 5/6 defer B2+B3).

---

## Related

- [`rules.md`](./rules.md) — BR-ADMIN-V1-001..003
- [`use-cases.md`](./use-cases.md) — UC-ADMIN-V1-001..006
- Sister domain [`../admin-audit/api-contract.md`](../admin-audit/api-contract.md) — audit log infrastructure
- Sister gap [GAP-637](../../../04-quality/gaps/phase-1-beta/closed/GAP-637-admin-v1-controllers-preauthorize-missing.md) — @PreAuthorize Wave 97 Bucket A
- [`.claude/rules/pre-launch-owasp-rest-hardening-checklist.md`](../../../.claude/rules/pre-launch-owasp-rest-hardening-checklist.md) §2.1
- [`.claude/rules/contract-first-for-cross-layer.md`](../../../.claude/rules/contract-first-for-cross-layer.md) §3
- RFC 8594 (Sunset HTTP Header)
