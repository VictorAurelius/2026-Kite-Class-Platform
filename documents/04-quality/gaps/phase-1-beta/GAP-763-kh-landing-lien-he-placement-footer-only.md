---
audience: dev
---

# GAP-763 — KH landing "Liên hệ" chỉ ở footer, không có top nav

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-05-27 (Wave 106 RST Mảng A1)
**Affects:** KH landing `/` anonymous browsing — discoverability contact
**Phase:** phase-1-beta

## Problem

Plan §3 row A1 expects "Liên hệ" trong nav set (bộ ba Tính năng / Bảng giá / Liên hệ). Reality:

- Top nav: KHÔNG có "Liên hệ" link
- Footer: `<h4 class="mb-4 text-sm font-semibold">Liên hệ</h4>` — section title trong footer columns, không phải nav link

Anonymous user muốn contact phải scroll xuống footer. Affects:
- Solo Teacher cần hỏi pricing → tốn 1 scroll
- P2 Owner cần demo → cùng

## Proposed Fix (defer Đợt 107)

Option A: Thêm `<a href="/contact">Liên hệ</a>` vào top nav (nếu confirmed route exists).
Option B: Reframe expected nav as "Bảng giá / Đăng nhập / Yêu cầu Beta" trong plan Đợt 106 (scope intentional — beta phase prioritizes beta-request flow over generic contact).

## Acceptance Criteria

- [ ] Decision: add top nav OR reframe plan expectation
- [ ] Nếu add: `/contact` page render Vietnamese + email `support@kitehub.me` + Zalo OA placeholder

## Related

- Sister: GAP-762 (Tính năng missing)
- Wave 106 plan §3 row A1
