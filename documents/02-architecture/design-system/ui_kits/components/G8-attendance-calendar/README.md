# G8 — Attendance Calendar (Teacher Month-View)

**Component gap:** G8 per `dossier/04-component-gaps.md`
**Used by:** KC `/attendance`, KC `/classes/[id]/attendance` (history view)
**Persona:** Teacher (homeroom)

---

## Use case

Homeroom teacher (Cô Lan, GVCN Lớp 6A1) reviews **October 2026 attendance** for the class. Scans heatmap to spot bad-attendance days, clicks a day to see who was absent, celebrates students with perfect-attendance streaks.

---

## States (4)

| State | File | Score /128 |
|-------|------|:----------:|
| Month load | `states/month-load.html` | 109 |
| Day detail | `states/day-detail.html` | 110 |
| Streak highlight | `states/streak-highlight.html` | 107 |
| Partial month | `states/partial-month.html` | 105 |
| **Average** | — | **107.8** |
| **Min** | — | **105** |

---

## VN UX

- Week starts Monday — column headers T2/T3/T4/T5/T6/T7/CN
- Weekend cells (T7, CN) shown in info-blue color
- Heatmap thresholds: ≥95% green / 80-95% amber / <80% red
- Day labels Vietnamese: `Thứ Sáu, 09/10/2026`
- Status labels: `Có mặt`, `Vắng phép`, `Vắng không phép`, `Trễ N phút`
- Streak celebration: `🏆 Chuỗi 30 ngày hoàn hảo!` + Zalo OA "gửi lời khen"
- Pending sessions (future): striped pattern + "Sắp tới"
- Mock attendance follows realistic Mon/Wed/Fri schedule

---

## Accessibility

- Calendar buttons each have `aria-label="Ngày DD/MM, tỉ lệ NN%"`
- Color-not-only: % number visible inline + icon glyph in popover
- Keyboard navigable (each day = `<button>`)
- Selected day: `aria-pressed="true"` + 3px primary ring
- Reduced-motion: glow animation on streak disabled
- Popover uses `<aside>` — semantic landmark
- 44×44 minimum on day cells (aspect-ratio + min-height implied)

---

## Contrast measurements (WCAG AA)

| Combo | Ratio | Pass |
|-------|------:|:----:|
| Body fg on bg-card | 17.9:1 | AAA |
| Success green-100 | 4.5:1 | AA |
| Warning amber-50 | 5.2:1 | AA |
| Destructive red-50 | 4.9:1 | AA |
| Info blue (weekend) | 4.6:1 | AA |
| Accent on white (streak CTA) | 4.6:1 | AA |

---

## i18n keys

```
attendance.cal.title
attendance.cal.legend
attendance.cal.heatmap
attendance.cal.day.title
attendance.cal.day.summary
attendance.cal.day.viewRoster
attendance.cal.streak.title
attendance.cal.streak.celebrate
attendance.cal.partial.title
attendance.cal.partial.upcoming
```

---

## Reference

- shadcn `calendar.tsx` is the generic primitive — this component composes it with teacher-specific overlays
- Pairs with G2 Attendance Roster (this is overview, G2 is per-day entry)
- Heatmap colors share token system with `colors_and_type.css`
