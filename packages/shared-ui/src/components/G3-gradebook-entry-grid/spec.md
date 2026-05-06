# G3 Gradebook Entry Grid — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G3-gradebook-entry-grid/README.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G3-gradebook-entry-grid/README.md)
**Component gap:** G3 per [`dossier/04-component-gaps.md`](../../../../../documents/02-architecture/design-system/dossier/04-component-gaps.md) §G3
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — stays 🟡 PARTIAL after this port (5/12 G* components shipped post-Wave 28 Bucket A).
**Wave:** 28 Bucket A — first solo-bucket addition after Wave 27's G2/G5/G6/G7 quartet.
**Replaces:** KC `/classes/[id]/grades` (currently no production route — greenfield).

---

## What this PR ships

- `<GradebookEntryGrid>` React component covering 7 lifecycle states (`loading`, `empty`, `default`, `editing`, `saving`, `saved`, `error`).
- Per-cell save indicator: idle / editing / dirty / saving / saved / error — caller owns the `cellStatuses[]` and the grid renders the indicator (`aria-busy`, error inline, data-cell-state).
- `validateGrade(input: string) → { valid, value?, error? }` — VN 10-point scale validator (0..10, 1 decimal max, comma decimal accepted).
- `parseExcelPaste(text: string) → GradebookCell[]` — TSV row parser for clipboard imports.
- TypeScript types exported on the public `@kite/shared-ui` API: `GradebookEntryGrid`, `GradebookEntryGridProps`, `GradebookSession`, `GradebookStudent`, `GradeColumn`, `GradeValue`, `GradebookCell`, `GradebookCellStatus`, plus `validateGrade` / `parseExcelPaste`.
- Vitest coverage: 21 utility tests + 14 component state-render tests.

## Status / state mapping

| `state` prop | Header | Save bar | Body | Banner |
|---|---|---|---|---|
| `loading` | hidden | hidden | skeleton (~6 rows) | none |
| `empty` | hidden | hidden | empty CTA "Chưa có cột điểm nào" + "Thêm cột điểm" | none |
| `default` | shown | hidden | full grid, cells editable | none |
| `editing` | shown | shown (`Lưu (Ctrl+S)`, dirty count) | full grid, cells editable | none |
| `saving` | shown | shown (Save button disabled `Đang lưu`) | full grid, cells locked | none |
| `saved` | shown | hidden | full grid, cells locked | green `role="status"` banner with Zalo OA notice |
| `error` | shown | shown | full grid, cells editable | red `role="alert"` banner + `Thử lại` retry |

## VN UX (verbatim from HTML spec + README §VN UX)

- **Class title format:** `Lớp 10A2 — Toán nâng cao`
- **Term line:** `Học kỳ I · 2026-2027`
- **Column headers:** `KT 15'`, `KT 1 tiết`, `Giữa kỳ`, `Cuối kỳ` + Hệ số 1/2/2/3
- **TBM column:** `TBM` / `Tự tính` (auto-computed, weighted-average per Thông tư 22/2021/TT-BGDĐT)
- **Save copy:** `Lưu (Ctrl+S)` / `Đang lưu` / `Hủy` / `Thử lại`
- **Saved banner:** "Đã lưu thành công N điểm. Phụ huynh sẽ nhận thông báo qua Zalo OA trong 5 phút tới."
- **Validation copy:**
  - `Điểm phải trong khoảng 0-10` (range)
  - `Điểm phải là số` (non-numeric)
  - `Tối đa 1 chữ số thập phân` (decimal places — VN teacher convention rejects "9.25")
- **Range hint** on focused cell: `Thang điểm 0–10`
- **Save bar count:** `N thay đổi`

## VN 10-point scale rules (validator contract)

Implemented in `validateGrade`:

- Range `0..10` inclusive; out-of-range → `Điểm phải trong khoảng 0-10`.
- **Max 1 decimal place** — VN convention per `validation-error.html` rules card. "9.25" fails with `Tối đa 1 chữ số thập phân`.
- Both `7.5` (period) and `7,5` (Vietnamese comma decimal) accepted.
- Empty / whitespace returns `{ valid: true, value: undefined }` so the grid can render `—`.

> **Note on prompt vs source-of-truth:** the agent prompt referenced ".25 step (so valid: 0, 0.25, 0.5, ..., 10)". The HTML spec (`states/cell-editing.html` line 92: `step="0.1"`) and README §VN UX ("1 decimal max — Vietnamese teacher convention") are unambiguous: 1 decimal max, period. We follow the spec. If the .25 step is later required for a different audience (e.g. K-12 sub-stream), file a follow-up gap.

## Excel paste contract

Implemented in `parseExcelPaste`:

- Splits clipboard text on `\n` / `\r\n` (Windows Excel friendly).
- Per row, splits on `\t` and uses **column 0** = student MST, **column 1** = grade. Extra columns ignored.
- Skips empty / whitespace-only rows.
- Skips rows with fewer than 2 columns (malformed).
- Trims whitespace around studentCode + rawValue.
- Component intercepts paste events on grade cells when the clipboard text contains `\t` OR newlines (multi-cell paste). Single-value pastes pass through to the input as normal.

## Accessibility

- `<table>` with `<thead data-sticky="true">` so screen readers + sticky-aware scroll work.
- Each input cell: `aria-label="<column> cho <học sinh>"` so audio context is preserved.
- Errors set `aria-invalid="true"` + `aria-describedby` linking inline error text (matches `validation-error.html` markup).
- In-flight cells set `aria-busy="true"`.
- Range hint announced as part of the input's neighbouring text on focus (visible to AT users).
- Save bar is a `role="region"` landmark.
- Reduced-motion honoured (no slide / shake animation in production port — error state uses border + text only).

## What this PR does NOT ship (deferred)

- Remaining 7 G* components (G1, G4, G8..G12) — separate Wave 28+ buckets.
- Wiring into a production route `kiteclass-frontend/src/app/classes/[id]/grades/page.tsx` — host-app concern.
- TBM computed-color tier (Xuất sắc/Giỏi/Khá/TB/Yếu) — basic decimal display only in v1.
- Distribution footer card (Vietnamese grade tiers histogram).
- "Quy tắc nhập điểm" rules card (validation reference panel) — appears on `validation-error.html` but is informational/static, deferred to host page.
- "Hoàn tác" undo affordance — caller-owned (caller can re-render a previous state snapshot).
- "Lịch sử thay đổi gần đây" history panel — separate component (D1/D2 family in dossier).
- Auto-save countdown (`tự động lưu sau 30 giây`) — host-app concern; component exposes `state` enum + callbacks.
- TBM auto-recalc on cell change — current implementation uses live computed value from `student.grades` per render; if caller mutates the grades object after `onCellChange`, the TBM updates automatically.

## Acceptance criteria status (mapping to GAP-273 AC)

- [x] Component ported with TypeScript types
- [x] `spec.md` mirror committed
- [x] Unit tests per state + cell editing (35 tests total — 21 utils + 14 component)
- [x] G3 VN 10pt validator + Excel paste parser exported and tested
- [x] Vietnamese-only labels (verbatim from spec HTML protos)
- [ ] All 12 components ported — 5/12 in production after this PR (G2/G3/G5/G6/G7); 7 remaining (G1/G4/G8..G12)
- [ ] Storybook / `/dev/components/` route — out of scope for this PR
- [ ] Production usage ≥105/128 verification — needs host-app wiring + UI review run
- [ ] Visual regression baseline — captured separately under post-wave audit

GAP-273 stays 🟡 PARTIAL.
