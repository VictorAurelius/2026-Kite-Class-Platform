# Admin v1 — Use Cases

**Domain:** Admin v1 endpoints (GAP-638 — Wave 97 Bucket B1 3-layer foundation)
**Source-of-truth controllers:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/`
**Last verified:** 2026-05-18 (Wave 97 Bucket B1)

---

## UC-ADMIN-V1-001 — Liệt kê tenant instances

**Use case:** Platform admin xem danh sách mọi tenant instance trên platform (paginated)

**Actor:** `PLATFORM_ADMIN` (single role per BR-ADMIN-V1-001)

**Trigger:** Admin mở `/admin/instances` UI hoặc gọi endpoint trực tiếp

**Endpoint:** `GET /api/v1/admin/instances?page={N}&size={M}`

**Business rule:** BR-ADMIN-V1-001 (role), BR-ADMIN-V1-002 (pagination)

**Happy path:**
1. Admin authenticated (JWT có role `PLATFORM_ADMIN`)
2. Request gửi `GET /api/v1/admin/instances?page=0&size=50`
3. Gateway forward request + X-User-Roles header
4. `AdminInstancesController.listInstances()` được Spring Security AOP intercept
5. `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` check role PASS
6. Service query `InstanceRepository.findAll(pageable)` với pagination
7. Response 200 OK + `Page<InstanceSummary>` với content + totalElements + totalPages

**Error branches:**
- Non-admin role → HTTP 403 + AccessDeniedException (per BR-ADMIN-V1-001)
- Pagination size vượt max → clamp xuống 200 + log warn (per BR-ADMIN-V1-002)
- DB connection fail → HTTP 500 + log error + retry idempotent

**FE behavior:** Admin dashboard render table với pagination controls; click row navigate UC-ADMIN-V1-002.

---

## UC-ADMIN-V1-002 — Xem chi tiết một tenant instance

**Use case:** Admin xem chi tiết tenant instance theo ID (instance metadata + subscription state + lifecycle history)

**Actor:** `PLATFORM_ADMIN`

**Trigger:** Click instance row trong list UI hoặc gọi endpoint trực tiếp với UUID

**Endpoint:** `GET /api/v1/admin/instances/{id}`

**Business rule:** BR-ADMIN-V1-001

**Happy path:**
1. Admin authenticated
2. Request `GET /api/v1/admin/instances/550e8400-e29b-41d4-a716-446655440000`
3. `@PreAuthorize` check role PASS
4. Service query `InstanceRepository.findById(uuid)` + join với Subscription
5. Response 200 OK + InstanceDetail (instance fields + subscription fields + tier + status)

**Error branches:**
- Non-admin role → HTTP 403
- Invalid UUID format → HTTP 400 + INVALID_ID error code
- Instance not found → HTTP 404 + INSTANCE_NOT_FOUND
- Deleted instance (soft delete) → return data với `deleted=true` flag (admin có quyền view archived)

**FE behavior:** Detail page với 4 sections: instance metadata / subscription state / lifecycle timeline / quick actions (deprecated → only show, không edit).

---

## UC-ADMIN-V1-003 — Liệt kê pending payments

**Use case:** Admin xem danh sách payments đang chờ xác nhận manual (bank transfer chưa reconcile)

**Actor:** `PLATFORM_ADMIN`

**Trigger:** Admin mở `/admin/payments/pending` UI cho daily reconciliation workflow

**Endpoint:** `GET /api/v1/admin/payments/pending?page={N}&size={M}`

**Business rule:** BR-ADMIN-V1-001, BR-ADMIN-V1-002

**Happy path:**
1. Admin authenticated
2. Request gửi với pagination params
3. `@PreAuthorize` check PASS
4. Service query `PaymentRepository.findByStatusAndPendingTrue(pageable)` filter Bank Transfer + pending
5. Response 200 OK + `Page<PaymentSummary>` với content showing amount + tenant + days_pending

**Error branches:**
- Non-admin role → HTTP 403
- Pagination size > max → clamp
- DB connection fail → 500

**FE behavior:** Table với badge highlight payments quá hạn 3-day; row click navigate đến manual confirm UI (separate write endpoint, không trong scope domain này).

---

## UC-ADMIN-V1-004 — Xem payments summary aggregated

**Use case:** Admin xem aggregate summary cho payments (total amount + count + currency breakdown)

**Actor:** `PLATFORM_ADMIN`

**Trigger:** Admin mở dashboard widget hoặc gọi endpoint trực tiếp

**Endpoint:** `GET /api/v1/admin/payments/summary`

**Business rule:** BR-ADMIN-V1-001

**Current implementation:** Returns `Map<String, Object>` với keys `totalAmount`, `totalCount`, `currency`, `periodStart`, `periodEnd`. **DEFER B2:** Refactor → typed `PaymentsSummaryResponse` record per Wave 97 Bucket B2 follow-up.

**Happy path:**
1. Admin authenticated
2. Request `GET /api/v1/admin/payments/summary`
3. `@PreAuthorize` check PASS
4. Service aggregate query (SUM amount + COUNT rows + GROUP BY currency)
5. Response 200 OK + summary Map

**Error branches:**
- Non-admin role → HTTP 403
- Empty result (no payments) → return `{totalAmount: 0, totalCount: 0, currency: "VND"}` không HTTP 404

**FE behavior:** Dashboard card hiển thị 3 metric: total amount (VND format) + total transactions + currency badge.

---

## UC-ADMIN-V1-005 — Liệt kê revenue records

**Use case:** Admin xem danh sách revenue records (per-month aggregated subscription income)

**Actor:** `PLATFORM_ADMIN`

**Trigger:** Admin mở `/admin/revenue` UI cho monthly financial review

**Endpoint:** `GET /api/v1/admin/revenue?page={N}&size={M}`

**Business rule:** BR-ADMIN-V1-001, BR-ADMIN-V1-002

**Happy path:**
1. Admin authenticated
2. Request với pagination
3. `@PreAuthorize` check PASS
4. Service query `RevenueRecordRepository.findAll(pageable)` ordered by period DESC (mới nhất trước)
5. Response 200 OK + `Page<RevenueRecord>`

**Error branches:**
- Non-admin role → HTTP 403
- Pagination size > max → clamp
- DB fail → 500

**FE behavior:** Table sorted by period DESC + monthly bar chart visualization above.

---

## UC-ADMIN-V1-006 — Xem revenue summary aggregated

**Use case:** Admin xem aggregate summary revenue (total YTD + MoM growth + currency)

**Actor:** `PLATFORM_ADMIN`

**Trigger:** Admin mở dashboard hoặc gọi endpoint trực tiếp

**Endpoint:** `GET /api/v1/admin/revenue/summary`

**Business rule:** BR-ADMIN-V1-001

**Current implementation:** Returns `Map<String, Object>` với keys. **DEFER B2:** Refactor → typed `RevenueSummaryResponse` record + `RevenuePeriod` enum (DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY) per Wave 97 Bucket B2.

**Happy path:**
1. Admin authenticated
2. Request `GET /api/v1/admin/revenue/summary`
3. `@PreAuthorize` check PASS
4. Service aggregate query (SUM revenue YTD + previous-period delta)
5. Response 200 OK + summary Map

**Error branches:**
- Non-admin role → HTTP 403
- Empty result → return 0-baseline response không HTTP 404

**FE behavior:** Dashboard hero card với YTD revenue + sparkline + MoM growth percentage badge.

---

## Cross-cutting concerns

| Concern | Implementation |
|---|---|
| Auth | Gateway-forwarded JWT → SecurityConfig XUserRolesHeaderFilter → Spring Security context |
| Authorization | `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` class-level per BR-ADMIN-V1-001 |
| Pagination | Spring Data `Pageable` + `Page<T>` response per BR-ADMIN-V1-002 |
| Rate limit | Gateway-enforced 30 req/min/admin per config `kitehub.admin.api-v1.rate-limit-per-minute` |
| Audit log | Auto-emit `admin-audit` event per service `AuditLogService.record()` (cross-domain — xem `admin-audit/use-cases.md` UC-ADMIN-AUDIT-001..005) |
| Error format | Standard error envelope `{code, message, requestId, timestamp}` per `roles/api-contract.md` |

---

## Related

- [`rules.md`](./rules.md) — BR-ADMIN-V1-001..003
- [`api-contract.md`](./api-contract.md) — endpoint contract chi tiết
- Sister domain [`../admin-audit/`](../admin-audit/) — audit log infrastructure
- Wave 97 Bucket A (GAP-637) — @PreAuthorize implementation PR #1540
- Wave 97 Bucket B1 (this PR) — 3-layer docs foundation
- Wave 97 Bucket B2 — DTO refactor + legacy @Deprecated (defer next session)
