# `AttendanceRoster` — `@kite/shared-ui` (G2)

React/TypeScript port of the G2 prototype.

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G2-attendance-roster/spec.md`](../../../../documents/02-architecture/design-system/ui_kits/components/G2-attendance-roster/spec.md)

**Component gap row:** [`dossier/04-component-gaps.md` §G2](../../../../documents/02-architecture/design-system/dossier/04-component-gaps.md)

## Scope shipped (Wave 27 Bucket A)

| Aspect | Status |
|--------|--------|
| Roster header (class name, session #, date, duration) | ✅ |
| Per-student row: name, MST, attendance rate inline | ✅ |
| 4-button toggle group (P / V / M / L) — radiogroup ARIA | ✅ |
| Vietnamese labels copied verbatim from spec.md | ✅ |
| 7 lifecycle states: loading / empty / default / marking / saving / saved / error | ✅ |
| Sticky save bar with `N thay đổi` count | ✅ |
| Mark-all-present quick action | ✅ |
| Error banner + retry CTA | ✅ |
| WCAG AA: ≥44×44 touch targets, sr-only labels per glyph | ✅ |

## Out of scope (deferred to follow-up gaps under GAP-273)

- **Late-minutes input + excuse-note popover** — props (`lateMinutes`, `note`) accepted on data shape, but no inline editor UI yet (spec.md §VN UX Late minutes `5/10/15` quick-pills + popover).
- **Status cycle (P → V → M → L → P)** — current API takes explicit `status` per click; cycle behaviour is left to the caller (kept controlled).
- **Virtualization for ≥50 students** — not needed at MVP class sizes (~25 students typical per spec).
- **Read-only parent variant** — same component renders read-only in `state="saved"`; a dedicated parent variant can compose this in a later PR.

## Public API

See [`types.ts`](./types.ts):

```tsx
import { AttendanceRoster, type AttendanceRosterProps, type AttendanceStatus, type StudentRecord } from '@kite/shared-ui';
```

The component is **controlled** — parent owns `students`, `state`, and the `dirtyCount`. This mirrors the controlled shape of `<ConsentBanner>` and keeps fetch / persistence concerns out of the shared lib.
