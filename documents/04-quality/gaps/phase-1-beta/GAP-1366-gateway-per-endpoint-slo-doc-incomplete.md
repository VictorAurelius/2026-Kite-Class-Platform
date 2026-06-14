# GAP-1366: Gateway per-endpoint-class SLO documentation chưa đầy đủ

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 2.4)
**Affects:** `documents/02-architecture/` (SLO doc), gateway routing

## Problem

Sub-check 2.4 (gateway response-time SLO per endpoint-class documented) chỉ PARTIAL — chưa có doc SLO phân lớp endpoint (read vs write vs heavy-gen vs auth). GAP-135 (PARTIAL) là precedent SLO target chưa hoàn chỉnh.

Không có SLO per-class → không có baseline để alert / không xác định endpoint nào "chậm bất thường". Cần tài liệu hóa target P50/P95/P99 theo lớp endpoint.

## Proposed Fix

Tạo/hoàn thiện `documents/02-architecture/slo.md`: bảng SLO per endpoint-class (read <500ms P95, write <1s P95, heavy-gen async, auth <300ms P95). Link từ gateway config + prometheus alert rules.

## Acceptance Criteria

- [ ] `slo.md` có bảng SLO per endpoint-class
- [ ] Prometheus alert rule reference SLO threshold
- [ ] Gateway routing doc link tới SLO

## Related

- Discovered in: 2026-06-14 performance audit (F-010)
- GAP-135 (PARTIAL) — SLO target precedent
