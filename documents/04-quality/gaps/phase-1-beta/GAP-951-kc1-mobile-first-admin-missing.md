# GAP-951: Mobile-first admin missing — 50%+ VN admin dùng phone

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Admin layout) — persona P3 (Owner non-tech 50+, smartphone-first)
**Defer-to:** After Wave flow-kh3 finish

## Problem

Per persona simulation 2026-05-14 Wave 79: 50%+ VN edu admin dùng phone. `kiteclass-frontend` admin layout chưa có evidence breakpoint <768px tested. Bác Hùng mở `kc-...kitehub.me/admin` trên Samsung A05 → sidebar nav cover 80% screen → settings form input đè nhau → bỏ cuộc. Surfaced: persona Finding 3.3.

## Proposed Fix

Audit + responsive refactor admin layout: collapsible sidebar <768px, form fields stack vertically, touch target ≥44px. Playwright config thêm mobile viewport (iPhone SE, Galaxy A05).

## Acceptance Criteria

- [ ] `grep -rn "md:\|lg:\|sm:" kiteclass/kiteclass-frontend/src/app --include="*.tsx" \| wc -l` returns ≥50 (breakpoint usage)
- [ ] Playwright spec `mobile-admin.spec.ts` PASS trên 375×667 viewport
- [ ] Manual walk Samsung A05 emulation: sidebar collapsed by default, settings form usable

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 3.3
- Sister: persona simulation 2026-05-14 Wave 79
- Flow Verification Campaign §4 row KC-1
