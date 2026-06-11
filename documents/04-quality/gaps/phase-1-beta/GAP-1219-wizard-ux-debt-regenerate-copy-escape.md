# GAP-1219: Wizard UX debt — regenerate no-op mid-wizard + logo over-promise copy + thiếu escape-ramp Welcome

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-11 (branding-100 persona audit F3/F4/F10)
**Affects:** KH wizard (RegenerateCounter, LogoStep copy, WelcomeStep)

## Problem

(1) Nút "Tạo lại" hiển thị/trừ counter cả khi CHƯA generate gì (no-op vô nghĩa); (2) copy bước Logo hứa "AI tự tạo logo" vượt khả năng hiện tại; (3) Welcome không có escape-ramp "dùng mặc định, làm sau" ngay từ đầu (GAP-287 chỉ defaults từ Logo trở đi).

## Proposed Fix

Gom 3 fix nhỏ UX vào bucket E/F wave branding-100 (cùng đợt reorder output-first).

## Acceptance Criteria

- [ ] Regenerate chỉ active sau lần generate đầu
- [ ] Copy logo khớp năng lực thật
- [ ] Welcome có lối "bỏ qua — dùng mặc định"

## Related

- Persona F3/F4/F10; benchmark khuyến nghị #4/#6
