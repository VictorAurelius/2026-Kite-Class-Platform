# GAP-1366: Gateway per-endpoint-class SLO documentation chưa đầy đủ

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 2.4)
**Resolved:** 2026-06-15 (branch `fix/audit-fixF-devops-2026-06-14`)
**Affects:** `documents/02-architecture/` (SLO doc), gateway routing

## Problem

Sub-check 2.4 (gateway response-time SLO per endpoint-class documented) chỉ PARTIAL — chưa có doc SLO phân lớp endpoint (read vs write vs heavy-gen vs auth). GAP-135 (PARTIAL) là precedent SLO target chưa hoàn chỉnh.

Không có SLO per-class → không có baseline để alert / không xác định endpoint nào "chậm bất thường". Cần tài liệu hóa target P50/P95/P99 theo lớp endpoint.

## Proposed Fix

Tạo/hoàn thiện `documents/02-architecture/slo.md`: bảng SLO per endpoint-class (read <500ms P95, write <1s P95, heavy-gen async, auth <300ms P95). Link từ gateway config + prometheus alert rules.

## Acceptance Criteria

- [x] `slo.md` có bảng SLO per endpoint-class — `documents/02-architecture/slo.md` §1: bảng 7 lớp (auth/interactive-read/list-read/write/heavy-gen/async/health) → Tier A–F với p50/p95/p99.
- [x] Prometheus alert rule reference SLO threshold — `slo.md` §2 map mỗi lớp → alert trong `prometheusrule.yaml` (`api-latency-slo-alerts`) + threshold + CloudWatch Alarm 6 (5xx).
- [x] Gateway routing doc link tới SLO — `service-catalog-and-auth-flow.md` §2 (Dependency Graph / routing) thêm callout trỏ `slo.md`; `02-architecture/README.md` index thêm row 9.

## Resolution (2026-06-15)

Created `documents/02-architecture/slo.md` — architecture-level per-endpoint-class SLO index that maps the audit's `read/write/heavy-gen/auth` vocabulary onto the established Tier A–F budgets (authoritative rubric stays `05-guides/monitoring/api-performance-slo.md`, which already existed but was unreferenced from architecture/gateway). slo.md §2 ties each class to its Prometheus alert (`ApiLatencyP95HighTier*`) + CloudWatch 5xx Alarm 6. Cross-linked from the gateway routing section in `service-catalog-and-auth-flow.md` + added to the `02-architecture/README.md` doc index (row 9). §4 documents the open follow-ups (partial `@Timed` tag coverage → GAP-135; load-test the budgets → GAP-1365).

## Related

- Discovered in: 2026-06-14 performance audit (F-010)
- GAP-135 (PARTIAL) — SLO target precedent
