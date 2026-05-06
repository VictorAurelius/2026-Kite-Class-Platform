# `AttendanceCalendar` — `@kite/shared-ui` (G8)

React/TypeScript port of the G8 prototype.

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G8-attendance-calendar/README.md`](../../../../documents/02-architecture/design-system/ui_kits/components/G8-attendance-calendar/README.md)

**Component gap row:** [`dossier/04-component-gaps.md` §G8](../../../../documents/02-architecture/design-system/dossier/04-component-gaps.md)

## Scope shipped (Wave 28 Bucket C)

| Aspect | Status |
|--------|--------|
| Vietnamese month header (`Tháng MM/YYYY`) | ✅ |
| Mon-first weekday header (T2/T3/T4/T5/T6/T7/CN) | ✅ |
| 6-status colour-coded day cells (PRESENT / ABSENT / LATE / EXCUSED / NO_CLASS / FUTURE) | ✅ |
| Glyph + sr-only label per status (colour is never the only signal) | ✅ |
| Status legend (Vietnamese) | ✅ |
| Click → `onSelectDay(dayOfMonth)` | ✅ |
| Selected-day `aria-pressed="true"` + primary ring | ✅ |
| Arrow-key navigation (←/→ ±1, ↑/↓ ±7) | ✅ |
| Editable mode: space-bar cycles status (PRESENT → ABSENT → LATE → EXCUSED) | ✅ |
| 30-day rolling streak indicator (`Chuỗi N ngày`) when `streak.count > 0` | ✅ |
| `streak.deferred === true` hides the chip (PARTIAL escape hatch) | ✅ |
| Pure utility `calculateStreak()` exported alongside | ✅ |
| Responsive grid (mobile stacks vertically; sm+ uses 7-column grid) | ✅ |
| WCAG AA: ≥44×44 touch target on each day cell | ✅ |
| `prefers-reduced-motion`: no animation, opacity-only transitions | ✅ |

## Out of scope (deferred to follow-up gaps under GAP-273)

- **Day-detail popover** — selecting a day fires `onSelectDay` only; the popover layout from `states/day-detail.html` (per-student attendance list, summary chips) belongs to the consumer page, not this primitive.
- **Heatmap variant** (% per day with green/amber/red thresholds) — current scope is per-day **status**, not aggregate **rate**. The heatmap variant from `states/month-load.html` can be composed by passing computed status (PRESENT for ≥95%, LATE for 80-95%, ABSENT for <80%) plus the `label` field for the inline `%`.
- **Streak celebration card** (`states/streak-highlight.html`) — gamification UI (trophy badge, "Gửi lời khen qua Zalo OA" CTA) is consumer-page concern; component exposes the streak count only.
- **Month navigation chrome** (prev/next buttons, "Tháng 10/2026" pill) — consumer owns nav. Component renders one month, controlled.
- **Holiday markers / partial-month indicator** — `states/partial-month.html` overlays are deferred.

## Public API

See [`types.ts`](./types.ts):

```tsx
import {
  AttendanceCalendar,
  calculateStreak,
  type AttendanceCalendarProps,
  type AttendanceDayStatus,
  type CalendarDay,
  type MonthCalendarData,
  type StreakInfo,
} from '@kite/shared-ui';
```

The component is **controlled** — parent owns `month` and `selectedDay`. This mirrors the controlled shape of `<AttendanceRoster>` and keeps fetch / persistence concerns out of the shared lib.

## Streak utility

```ts
const streak = calculateStreak(['PRESENT', 'PRESENT', 'ABSENT', 'PRESENT']);
//                              → { count: 2 }   (longest PRESENT run)
```

- Window: trailing 30 days (older days ignored even if they form longer runs).
- Output: `{ count: number, deferred?: boolean }`.
- Pure / O(n) / no allocations.

## Vietnamese-first

- Month label: `Tháng {1..12}/{YYYY}`
- Weekday header: T2 / T3 / T4 / T5 / T6 / T7 / CN (Mon-first)
- Status legend: `Có mặt`, `Vắng không phép`, `Đi trễ`, `Vắng có phép`
- Streak chip: `Chuỗi N ngày`
- ARIA: `Ngày DD/MM, <status label>`
