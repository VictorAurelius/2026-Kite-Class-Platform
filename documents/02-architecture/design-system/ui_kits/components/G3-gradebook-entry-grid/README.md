# G3 — Gradebook Entry Grid

**Component gap:** G3 per `dossier/04-component-gaps.md`
**Used by:** KC `/classes/[id]/grades` (new route)
**Persona:** Teacher (subject)

---

## Use case

Subject teacher nhập **Toán midterm grades** cho 35 học sinh Lớp 10A2. Vietnamese 10-point scale (0-10, 1 decimal). Bulk paste from Excel supported. Auto-calc TBM theo Thông tư 22/2021/TT-BGDĐT.

---

## States (6)

| State | File | Score /128 |
|-------|------|:----------:|
| Empty | `states/empty.html` | 102 |
| Row selected | `states/row-selected.html` | 109 |
| Cell editing | `states/cell-editing.html` | 110 |
| Validation error | `states/validation-error.html` | 107 |
| Bulk paste | `states/bulk-paste.html` | 108 |
| Saved | `states/saved.html` | 110 |
| **Average** | — | **107.7** |
| **Min** | — | **102** |

Both targets met (avg ≥105, min ≥95).

---

## VN UX

- Class name: `Lớp 10A2 — Toán nâng cao`
- Column headers theo Quy chế: KT 15' (Hệ số 1), KT 1 tiết (Hệ số 2), Giữa kỳ, Cuối kỳ (Hệ số 3)
- TBM = trung bình môn học, auto-calc
- Distribution footer: Xuất sắc/Giỏi/Khá/TB/Yếu (Vietnamese grade tiers)
- Validation: 0-10 only, 1 decimal max — Vietnamese teacher convention
- Saved toast: "Phụ huynh sẽ nhận thông báo qua Zalo OA trong 5 phút tới"

---

## Accessibility

- Sticky first-col with shadow visual cue (`box-shadow: 2px 0 8px -4px`)
- Cell editing: `aria-invalid="true" aria-describedby="err-1"` linked error
- Banners use `role="alert"` (validation) and `role="status" aria-live="polite"` (saved)
- Keyboard: Tab to next student, Enter confirm, Esc cancel, Ctrl+S save
- Color-not-only: TBM tier shown via icon in saved state, not just color
- Reduced-motion: shake animation disabled

---

## Contrast measurements (WCAG AA)

| Combo | Ratio | Pass |
|-------|------:|:----:|
| Body fg on bg-card | 17.9:1 | AAA |
| Primary ring on white | 4.6:1 | AA |
| Destructive on red-50 | 4.9:1 | AA |
| Success on green-100 | 4.5:1 | AA |
| Sticky col on muted/30 | 14.8:1 | AAA |

---

## i18n keys

```
gradebook.title
gradebook.empty.title
gradebook.empty.cta
gradebook.empty.hint
gradebook.col.kt15
gradebook.col.kt1tiet
gradebook.col.giuaky
gradebook.col.cuoiky
gradebook.col.tbm
gradebook.cell.editing
gradebook.cell.range
gradebook.cell.shortcut
gradebook.validation.outOfRange
gradebook.validation.nonNumeric
gradebook.validation.fix
gradebook.paste.title
gradebook.paste.preview
gradebook.paste.confirm
gradebook.paste.skip
gradebook.saved.title
gradebook.saved.timestamp
gradebook.saved.history
```
