# GAP-1359: InstanceController.list() unbounded findAll() (no Pageable)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 1.1/2.2)
**Affects:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/instance/controller/InstanceController.java:67-72`

## Problem

`InstanceController.list()` (GET `/`) khi `status == null` gọi `repository.findAll()` rồi map toàn bộ sang `List<InstanceResponse>` — KHÔNG có `Pageable`. Endpoint trả unbounded list `FrontendInstance`.

Bảng `FrontendInstance` (platform-level) tăng theo số trường/instance. Hiện nhỏ nhưng là performance cliff post-launch khi số instance tăng — toàn bộ load vào memory + serialize trong 1 response. Đối chiếu các endpoint user-data hot-path (attendance/enrollment/invoice) đều đã `Page<>`.

## Proposed Fix

Thêm `Pageable` param + trả `Page<InstanceResponse>` (hoặc cursor pagination). Nếu thực sự small-set có chủ đích → document exemption + hard cap (`PageRequest.of(0, 200)`).

## Acceptance Criteria

- [x] `InstanceController.list()` nhận `Pageable` HOẶC có hard cap + documented exemption
- [x] Không còn `repository.findAll()` không bounded trong path này

## Resolution (2026-06-15, branch fix/audit-fixE-perf-2026-06-14)

- `status == null` branch giờ gọi `repository.findAll(PageRequest.of(0, 500, Sort id DESC))`
  thay cho `findAll()` unbounded — hard cap `INSTANCE_LIST_MAX = 500` + documented exemption.
- Giữ nguyên response shape `List<InstanceResponse>` (JSON array) — FE consumers
  (`branding-wizard-api.ts`) tiêu thụ array, nên KHÔNG đổi sang `Page<>` envelope (tránh break caller).
- Test: `InstanceControllerTest.list_returns_bounded_page_as_array` (verify `findAll(Pageable)`
  được gọi, `findAll()` no-arg KHÔNG). Full kiteclass-core surefire 1763 tests PASS.

## Related

- Discovered in: 2026-06-14 performance audit (F-003)
- GAP-432 (DONE) — precedent findAll bounded (site khác)
