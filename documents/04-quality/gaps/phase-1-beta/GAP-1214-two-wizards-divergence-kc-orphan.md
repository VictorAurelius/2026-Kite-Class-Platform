# GAP-1214: 2 wizard branding lệch nhau — KH 7-bước ADR-037 (canonical) vs KC 6-bước orphan + preview about:blank

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (branding-100 persona + failure-mode audits)
**Affects:** `kiteclass-frontend/components/branding/wizard/` (orphan) + `kitehub-frontend (customer)/branding/wizard` (canonical)

## Problem

Hai wizard tồn tại song song: KH 7 bước theo ADR-037 (mode/portrait/banner/SSE) vs KC 6 bước input-collector cũ — KC `PreviewStep.tsx:51` iframe `src="about:blank"` (deploy mù, P0 UX). Mọi fix phải làm 2 lần hoặc trôi (đúng class GAP-1208/1212).

## Proposed Fix

Chốt canonical = KH wizard (per failure-mode audit); KC route → embed/redirect sang canonical hoặc port; retire FSM orphan. Bucket B wave branding-100; kit GAP-1212 design cho bộ bước unified.

## Acceptance Criteria

- [ ] 1 wizard canonical duy nhất phục vụ cả 2 entry
- [ ] Không còn preview about:blank
- [ ] Bộ bước theo kit redesign (output-first)

## Related

- Audits persona F1/F2 + failure-mode; GAP-1212 (kit), GAP-1147/1134
