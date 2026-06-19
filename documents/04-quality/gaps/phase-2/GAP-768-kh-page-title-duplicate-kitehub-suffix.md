---
audience: dev
---

# GAP-768 — KH page title duplicate "| KiteHub | KiteHub" suffix

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-05-27 (Wave 106 RST Mảng A3)
**Affects:** KH legal pages — SEO + cosmetic
**Phase:** phase-1-beta

## Problem

Title element trên policy pages có duplicate "| KiteHub" suffix:

| Path | Title (cosmetic bug) |
|---|---|
| `/legal/terms` | `Điều khoản dịch vụ \| KiteHub \| KiteHub` |
| `/legal/privacy` | `Chính sách quyền riêng tư \| KiteHub \| KiteHub` |

Expected: `Điều khoản dịch vụ | KiteHub` (single suffix).

## Root Cause

Next.js `metadata.title.template = "%s | KiteHub"` áp dụng twice — once trong page-level metadata + once trong root layout template, hoặc page metadata title đã include "| KiteHub" rồi rồi template wrap thêm.

Check:
- `kitehub-frontend/src/app/legal/terms/page.tsx` metadata
- `kitehub-frontend/src/app/legal/privacy/page.tsx` metadata
- `kitehub-frontend/src/app/layout.tsx` metadata title template

## Proposed Fix (defer Đợt 107 — cosmetic)

Option A: Remove "| KiteHub" từ page metadata title, để layout template thêm.
Option B: Remove template wrap trong layout, để page set explicit title.

Per Next.js convention, Option A standard.

## Acceptance Criteria

- [ ] `<title>Điều khoản dịch vụ | KiteHub</title>` (single suffix)
- [ ] Sister fix cho `/legal/privacy` + other pages affected
- [ ] No E2E spec required per `e2e-rst-test-layer-boundary.md` §2.2 (cosmetic/visual scope = RST + visual regression, deferred Phase 1.5+)

## Related

- Wave 106 plan §3 row A3
- Rule: `e2e-rst-test-layer-boundary.md` §2.2 owns table (cosmetic = RST exempt from E2E mandate)
