# G4 — Class Schedule Manager

**Component gap:** G4 per `dossier/04-component-gaps.md`
**Used by:** KC `/classes/[id]/schedule` (new route)
**Persona:** Principal / Owner / Teacher

---

## Use case

Principal sets up **Lớp 6A1 weekly schedule** for the school year. Three subjects (Toán/Văn/Anh) × three teachers × recurring rule (Mon/Wed/Fri 14:00-15:30). Vietnamese week starts Monday. Holiday auto-overlay (Tết, 30/4, 2/9).

---

## States (5)

| State | File | Score /128 |
|-------|------|:----------:|
| Empty | `states/empty.html` | 103 |
| Single class | `states/single-class.html` | 106 |
| Recurring edit | `states/recurring-edit.html` | 110 |
| Conflict warning | `states/conflict-warning.html` | 108 |
| Saved | `states/saved.html` | 109 |
| **Average** | — | **107.2** |
| **Min** | — | **103** |

---

## VN UX

- Week starts Monday (T2 = first column, NOT Sunday)
- Day labels: T2/T3/T4/T5/T6/T7/CN (short pills) or "Thứ Hai" etc. (full)
- Weekend cols (T7, CN) shown in info-blue, slightly muted bg
- Date format: `dd/MM/yyyy` short, `Thứ Hai, 09/04/2026` long
- Time 24h `HH:mm` — `14:00 – 15:30`
- Holiday auto-overlay listed: `Tết (12-19/02), Giỗ tổ Hùng Vương (29/04), 30/4 - 01/05, Quốc khánh (02/09)`
- Class names: `Toán nâng cao`, `Văn 6`, `Anh văn` (never "Math 1")

---

## Accessibility

- Day toggle: `role="group"` with `aria-pressed` per day button
- Conflict alert: `role="alert"`, color-not-only (icon + text + dashed border)
- All interactive elements ≥44×44 (day buttons, time inputs)
- Focus ring 2px primary on all form inputs
- Reduced-motion: no animations on conflict highlight
- Color-not-only: subject blocks have icon glyph + text (not just bg color)

---

## Contrast measurements (WCAG AA)

| Combo | Ratio | Pass |
|-------|------:|:----:|
| Body fg on bg-card | 17.9:1 | AAA |
| Primary chip on white | 4.6:1 | AA |
| Destructive on red-50 (conflict) | 4.9:1 | AA |
| Warning amber on amber-50 (holiday) | 5.2:1 | AA |
| Info on white (weekend) | 4.6:1 | AA |
| Accent on white | 4.6:1 | AA |

---

## i18n keys

```
schedule.title
schedule.empty.title
schedule.empty.cta
schedule.empty.preset
schedule.single.title
schedule.single.recur
schedule.single.edit
schedule.recur.title
schedule.recur.days
schedule.recur.until
schedule.recur.preview
schedule.conflict.title
schedule.conflict.detail
schedule.conflict.resolve
schedule.saved.title
schedule.saved.summary
schedule.saved.export
```
