# G1 — Bulk Import Drop-zone + Job Tracker

**Component gap:** G1 per `dossier/04-component-gaps.md`
**Flow ref:** `dossier/05-business-flows.md` Flow #2 (Bulk import)
**Used by:** KC `/students` (Nhập danh sách button) → modal/full-page wizard
**Persona:** P3 Medium Center Admin / P5 K-12 Principal

---

## Use case

Admin import **500 students/day** trong tuần tuyển sinh. CSV format:
```
ho_va_ten,ngay_sinh,lop,phu_huynh_phone
Nguyễn Văn An,15/08/2015,Lớp 6A1,0901234567
...
```

Constraints: ≤ 5 MB, ≤ 10.000 dòng, batch insert 500/txn.

---

## States (5)

| State | File | Score /128 | What it shows |
|-------|------|:----------:|---------------|
| Idle | `states/idle.html` | 108 | Empty drop-zone awaiting file + sample CTA + format hint |
| Drag over | `states/drag-over.html` | 105 | Primary border + filled bg + pulse ring + filename preview |
| Parsing | `states/parsing.html` | 107 | Progress bar 62%, step list, ETA "12 giây", cancel CTA |
| Partial success | `states/partial-success.html` | 110 | 487/500 valid, 13 errors panel, download errors xlsx |
| Done | `states/done.html` | 111 | Hero check + 3-stat grid + 3 next-step CTAs |
| **Average** | — | **108.2** | ✓ ≥105 target |
| **Min** | — | **105** | ✓ ≥95 floor |

---

## VN UX

- Sample download: `Tải file mẫu (.xlsx)`
- Errors localized: `Dòng 23: Số điện thoại không hợp lệ`, `Dòng 47: Ngày sinh sai định dạng`
- Class names: `Lớp 6A1`, `Lớp 10A2` (never "Class 1")
- Phone format: 10 digits starting with 0 — `0901 234 567`
- Date format: `dd/MM/yyyy` — `15/08/2015`
- Names: `Nguyễn Văn An`, `Trần Thị Mai`, `Lê Hoàng Long`

---

## Accessibility

- Drop-zone keyboard reachable via `<input type="file">` inside `<label>`
- `role="status" aria-live="polite"` on parsing + summary banners
- `role="progressbar"` with `aria-valuenow/min/max` on progress bar
- Focus ring 2px primary on drop-zone + buttons
- Color-not-only: error rows use icon + text + colored badge
- All chips have icon + glyph + sr-only label

---

## Contrast measurements (WCAG AA)

| Combo | Ratio | Pass |
|-------|------:|:----:|
| Body fg on bg-card | 17.9:1 | AAA |
| Muted-fg on bg-card | 4.7:1 | AA |
| Primary KC blue on white | 4.6:1 | AA |
| Success on green-100 | 4.5:1 | AA |
| Warning amber on amber-50 | 5.2:1 | AA |
| Destructive on red-50 | 4.9:1 | AA |

---

## i18n keys

```
import.title
import.dropzone.idle.cta
import.dropzone.hint
import.dropzone.drag.active
import.dropzone.drag.release
import.sample.download
import.constraint.maxRows
import.constraint.maxSize
import.format.required
import.parsing.title
import.parsing.progress
import.parsing.cancel
import.parsing.eta
import.partial.title
import.partial.errors.heading
import.partial.errors.row
import.partial.action.fix
import.partial.action.commitOk
import.partial.action.downloadErrors
import.done.title
import.done.summary
import.done.next.viewList
import.done.next.assignClass
import.done.next.invoiceParents
```

---

## Reference

- shadcn `Dialog` for modal containing drop-zone
- shadcn `Progress` (or native progressbar) for parsing
- React Dropzone library for actual implementation
- TanStack Query for upload mutation
- Backend reference: GAP-137 P0 (backend complete, FE entry was missing — this closes the design-system side)
