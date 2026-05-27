---
audience: dev
---

# GAP-766 — KH `/accessibility` route HTTP 404 (plan A3 expected)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-05-27 (Wave 106 RST Mảng A3)
**Affects:** KH anonymous browsing — WCAG accessibility statement page missing
**Phase:** phase-1-beta

## Problem

Plan Đợt 106 §3 row A3 expects: "Trang chính sách — **Điều khoản / Quyền riêng tư / Câu hỏi tiếp cận** từ chân trang".

Probe routes:
- `/legal/terms` → HTTP 200 ✅ (Điều khoản dịch vụ)
- `/legal/privacy` → HTTP 200 ✅ (Chính sách quyền riêng tư)
- `/accessibility` → HTTP 404 ❌ ("Câu hỏi tiếp cận" missing)
- `/legal/accessibility` → HTTP 404 ❌

WCAG AA / PDPL compliance angle: tenant-facing platform thường có accessibility statement page declaring level of conformance + contact channel cho accessibility issues.

## Root Cause

Route `/accessibility` (hoặc equivalent VN-slug) chưa được tạo. Footer cũng KHÔNG có link "Tiếp cận" / "Accessibility" — `grep` returns 0.

## Proposed Fix (defer Đợt 107)

Tạo `kitehub-frontend/src/app/legal/accessibility/page.tsx` (hoặc `/accessibility/`) với content:
- WCAG 2.1 AA conformance statement (Phase 1 BETA partial)
- Known accessibility issues list
- Contact email `accessibility@kitehub.me` (hoặc `support@kitehub.me`)
- Last reviewed date

Footer add `<a href="/legal/accessibility">Câu hỏi tiếp cận</a>` link.

E2E spec paired: assert HTTP 200 + title contains "tiếp cận" / "accessibility".

## Acceptance Criteria

- [ ] Route `/legal/accessibility` HTTP 200
- [ ] Page content Vietnamese narrative + WCAG AA statement
- [ ] Footer link visible
- [ ] E2E spec regression-guard

## Related

- Wave 106 plan §3 row A3
- Rule: `user-manual-content-standard.md` §2 row 11 (WCAG AA mandate)
- Rule: `e2e-rst-test-layer-boundary.md` §3 RST→E2E mandate
