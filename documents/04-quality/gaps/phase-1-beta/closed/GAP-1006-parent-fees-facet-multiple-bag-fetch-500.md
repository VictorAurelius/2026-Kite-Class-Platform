# GAP-1006: Parent fees facet 500 — MultipleBagFetchException (items + adjustments)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-05 (Wave flow-kc8 KC-8 parent portal G1 walk — W5b)
**Affects:** `kiteclass-core` `InvoiceRepository.findByStudentIdAndDueDateRange` + `Invoice` entity + `ParentFeesFacetServiceImpl`

## Problem

Khi parent đọc học phí của con — `GET /api/v1/parent/children/{childId}/fees` — backend trả **HTTP 500** `SYSTEM_INTERNAL_ERROR`. Stack trace (walk log 2026-06-05):

```
org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch
multiple bags: [Invoice.adjustments, Invoice.items]
  at ParentFeesFacetServiceImpl.getFeesForChild(ParentFeesFacetServiceImpl.java:103)
```

Root cause: `InvoiceRepository.findByStudentIdAndDueDateRange` dùng `@EntityGraph(attributePaths = {"items", "adjustments"})` trên paginated query (`Page<Invoice>`). Cả `items` lẫn `adjustments` đều là `List` (bag) — Hibernate KHÔNG fetch đồng thời 2 bag được. Mọi call method này crash bất kể có data hay không. IT mù vì test không gọi đúng path parent-fees-facet này (only 1 multi-bag entity-graph trong toàn repo — line 200; line 50/61 single-bag OK).

Phụ bệnh: ngay cả khi fetch 1 bag, `Page` + collection JOIN FETCH → Hibernate in-memory pagination (HHH000104).

## Proposed Fix (SHIPPED Wave flow-kc8)

Bỏ `@EntityGraph(attributePaths = {"items", "adjustments"})` khỏi `findByStudentIdAndDueDateRange` + thêm `@BatchSize(size = 20)` lên cả `items` + `adjustments` trên `Invoice` entity. Collections batch-load lazily trong `@Transactional(readOnly=true)` service → tránh MultipleBagFetch + tránh in-memory pagination + tránh N+1.

## Acceptance Criteria

- [x] `GET /children/{childId}/fees` (consent granted) → 200 với invoice items + adjustments đầy đủ.
- [x] Re-walk W5b PASS post-rebuild.
- [x] Không regression query fees khác (line 50/61 single-bag unaffected).

## Related

- Discovered in: Wave flow-kc8 KC-8 parent portal G1 walk (W5b)
- Pattern: cross-flow sweep — chỉ 1 site multi-bag trong InvoiceRepository (line 200)
- Sister: `pre-handoff-self-test-completeness.md` §2.6 fees facet

## Log

- **2026-06-05 (DONE):** Fix shipped Wave flow-kc8 — bỏ `@EntityGraph` multi-bag line 200 + `@BatchSize(20)` lên `Invoice.items` + `Invoice.adjustments`. Re-walk W5b post-rebuild: `GET /children/1/fees` → 200 với 2 invoices (INV-TEST-001/002). Regression W2 (400 PARAM_MISSING) + W6 (403 IDOR) hold. 8 IT context-load errors trong `mvnw test` xác định preexisting (clean-code stash reproduce — MapStruct AssignmentMapper không generate trong isolated test compile, không liên quan fix này). Walk-verified per `feature-ship-runtime-walk-mandate.md` §3.4.
