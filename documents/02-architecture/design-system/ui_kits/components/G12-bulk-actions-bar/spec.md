# G12 — Student List with Bulk Actions Bar

**Component gap:** G12 per `dossier/04-component-gaps.md` §G12
**Used by screens:** KC `/students`, KC `/teachers`, KH `/admin/instances`, KH `/admin/payments`
**Persona:** Owner / Admin (P3 Medium Center Admin) — desktop dense, tablet supported

---

## Purpose

Sticky bottom bar that appears when ≥1 row is selected in a data table. Pattern is missing from repo (existing tables have no bulk-action affordance). This component spec is the **bar pattern** + the **multi-select table** that consumes it.

Demos cover:
- `default.html` — table with no selection (bar hidden)
- `selecting.html` — 5 rows selected, bar visible with 4 actions
- `bulk-confirm.html` — destructive confirmation modal
- `action-running.html` — progress bar during async batch operation
- `action-done.html` — toast success + bar hidden
- `select-all-cross-page.html` — "select all 247 across pages" affordance

---

## Props (TypeScript-ish)

```ts
interface BulkActionsBarProps {
  selectedCount: number;          // 5
  totalCount: number;             // 247 (across all pages)
  pageCount: number;              // 5 pages × 50/page
  selectedAcrossPages?: boolean;  // false → only current page; true → "247 đã chọn"
  actions: Array<{
    id: string;
    label: string;            // "Xuất Excel", "Lưu trữ", "Chuyển lớp", "Xóa"
    icon: string;             // lucide icon name
    variant: "default" | "destructive";
    requiresConfirm?: boolean;
  }>;
  state: "default" | "selecting" | "confirm" | "running" | "done";
  onAction: (actionId: string) => void;
  onClearSelection: () => void;
  onSelectAllCrossPage: () => void;
}
```

---

## States

| File | State | UI |
|------|-------|-----|
| `default.html` | No rows selected | Table header has master checkbox; rows have per-row checkbox; bar hidden |
| `selecting.html` | 5 rows selected | Sticky bottom bar: "5 mục đã chọn" + 4 action buttons + "Bỏ chọn" |
| `bulk-confirm.html` | Destructive action requested | Modal dialog: "Bạn có chắc muốn xóa 5 học sinh?" + "Hành động không thể hoàn tác" + Hủy/Xác nhận |
| `action-running.html` | Async batch in progress | Bar shows progress: "Đang xử lý 3/5…" + cancel button |
| `action-done.html` | Toast notification | Toast "Đã xóa 5 học sinh" top-right; bar hidden; 2 rows visibly removed |
| `select-all-cross-page.html` | Master checkbox engaged | Selecting all 50 on this page → bar prompts "5 mục trên trang này đã chọn — chọn tất cả 247 mục?" |

---

## Vietnamese UX considerations

Per `dossier/02-vietnamese-ux-musts.md`:

- Bar copy: `5 mục đã chọn` (NOT `5 selected`)
- Action buttons (Vietnamese):
  - `Xuất Excel` (Export CSV) — lucide `Download`
  - `Lưu trữ` (Archive) — lucide `Archive`
  - `Chuyển lớp` (Assign to class) — lucide `Users`
  - `Xóa` (Delete) — lucide `Trash-2`, destructive variant
- Confirm dialog title: `Xóa 5 học sinh khỏi hệ thống?`
- Confirm dialog body: `Hành động này không thể hoàn tác. Dữ liệu điểm danh và bảng điểm liên quan sẽ được lưu trữ ẩn.`
- Confirm CTA: `Xác nhận xóa` (NOT `Yes`/`Confirm` English)
- Progress copy: `Đang xử lý 3/5…` with elapsed time `2 giây`
- Toast on done: `Đã xóa 5 học sinh` + `[Hoàn tác]` action (5s grace period — undoable archive)
- Cross-page-select prompt: `5 mục trên trang này đã chọn. [Chọn tất cả 247 mục]?`
- Vietnamese name surname-first sort (per `dossier/02` §1) — list is sorted by surname not given name
- Empty state copy: `Chưa có học sinh nào. [Nhập danh sách từ Excel] hoặc [Thêm thủ công]` (per dossier/04 §G12)
- Pagination: `Trang 1 / 5 · 50 mục/trang` (NOT `Page 1 of 5`)
- Use `bạn` informal you in confirm dialogs

---

## Accessibility

- Master checkbox `aria-label="Chọn tất cả trong trang"` + `aria-checked="mixed"` for partial state
- Bar `role="region" aria-label="Thanh thao tác hàng loạt"` `aria-live="polite"` (announces count change)
- Bar focus management: when bar appears, first action button receives focus on Cmd-K-style keyboard activation
- Confirm dialog: focus trap, ESC to close, primary action focused by default (NO — destructive default focused on cancel per UX best practice)
- Progress bar: `role="progressbar" aria-valuenow="3" aria-valuemax="5"` + sr-only "đang xử lý 3 trên 5"
- Cross-page prompt: `role="status"` + Tab navigation to "Chọn tất cả 247"

---

## Component reuse

- shadcn `Checkbox` (with mixed/indeterminate state)
- shadcn `Dialog` for bulk-confirm
- shadcn `Toast` (KC `toaster`) for action-done with `[Hoàn tác]` action
- shadcn `Progress` for action-running bar
- TanStack Table with `enableRowSelection`
- lucide icons: `Download`, `Archive`, `Users`, `Trash-2`, `X`, `Loader2`, `CheckCircle2`, `Undo2`
- `clsx` for conditional bar visibility

---

## Performance signals

- Bar mounted to DOM but `translate-y-full` when hidden — animated in/out, no remount
- Cross-page select uses `selectAllPagesQuery` (TanStack Query mutation), not loading 247 IDs into client state
- Progress polling (or SSE) for batch ops > 5s
- Toast undoable for 5 seconds (TanStack Query optimistic update with rollback)
