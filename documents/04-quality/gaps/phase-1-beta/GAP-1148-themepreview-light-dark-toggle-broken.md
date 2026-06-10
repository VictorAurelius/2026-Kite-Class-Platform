# GAP-1148: Nút sáng/tối (ThemePreview G11) không bật được ở Step 7

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend (`@kite/shared-ui` ThemePreview)
**Found:** 2026-06-10 (Wizard Step 7 G2 browser-walk — PR #2289)
**Affects:** `ThemePreview` component (shared-ui) rendered ở Step 7

## Problem

G2 feedback #6: "nút sáng tối không bật được". `<ThemePreview brandColors initialMode="light" />` (G11) có toggle light/dark nhưng click không đổi mode trên màn hình.

Pre-existing component bug (ThemePreview ở `@kite/shared-ui`, không phải code enhancement wave-wizard-step7).

## Proposed Fix

State-check `ThemePreview` trong `@kite/shared-ui`: toggle handler có wire + cập nhật class/CSS var dark không? (cross-ref `design-source-implementation-parity.md` §3.2 runtime click-verify — affordance wired ≠ working).

## Acceptance Criteria

- [ ] Click toggle → ThemePreview đổi light↔dark thật (browser verify).

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10)
- GAP-272k (live brand colors for ThemePreview, phase-2)
