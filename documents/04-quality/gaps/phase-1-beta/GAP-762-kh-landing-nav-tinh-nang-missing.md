---
audience: dev
---

# GAP-762 — KH landing nav thiếu "Tính năng" trong top nav

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-05-27 (Wave 106 RST Mảng A1 walkthrough)
**Affects:** KH landing `/` anonymous browsing — discoverability features
**Phase:** phase-1-beta

## Problem

Plan Đợt 106 §3 §3 mô tả A1 expected nav: **bộ ba (Tính năng / Bảng giá / Liên hệ)**. RST walkthrough phát hiện:

| Nav item | Found | Verdict |
|---|---|---|
| Tính năng | ❌ KHÔNG có trong top nav OR anywhere on landing (`grep -ci "tính năng\|features\|tinh-nang"` returns 0) | FAIL |
| Bảng giá | ✅ Top nav `<a href="/pricing">Bảng giá</a>` | PASS |
| Liên hệ | ⚠️ Có nhưng ở footer `<h4>Liên hệ</h4>` không phải top nav | xem GAP-763 |

DOM nav structure hiện tại: `<nav>Bảng giá / Đăng nhập</nav>` — 2 mục, thiếu Tính năng.

## Root Cause

Cần investigate: (a) intentional simplification trong nav design (anonymous user focus on Pricing + CTA login) HOẶC (b) miss khi build landing component.

## Proposed Fix (defer Đợt 107 — không chặn luồng walk)

Option A: Thêm `<a href="/features">Tính năng</a>` vào top nav + tạo route `/features` page.
Option B: Drop expected nav "Tính năng" khỏi plan Đợt 106 §3 (scope-decision intentional).

User-Q: Phase 1 BETA có cần dedicated /features page không? Hay landing hero + section đã đủ feature visibility?

## Acceptance Criteria

- [ ] Decision logged: keep simplified nav OR add Tính năng
- [ ] Nếu add: `/features` page render với Vietnamese narrative + VN sample
- [ ] Nếu drop: update plan Đợt 106 §3 expected nav (bộ-đôi thay vì bộ-ba)

## Related

- Wave 106 plan §3 row A1 — `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`
- Wave 106 RST walk evidence: `/tmp/wave-106-A1.html` baseline
- Sister: GAP-763 (Liên hệ placement)
