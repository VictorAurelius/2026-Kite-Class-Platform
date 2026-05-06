# G12 Bulk Actions Bar — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G12-bulk-actions-bar/spec.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G12-bulk-actions-bar/spec.md) + 5 root state HTML files (`default.html`, `selecting.html`, `bulk-confirm.html`, `action-running.html`, `action-done.html`).
**Component gap:** G12 per `dossier/04-component-gaps.md` §G12.
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — closes G12 portion (Track 2 Phase 3 final).
**Wave:** 29 Bucket D (12th of 12 G* + D1 — final port; **closes Track 2 Phase 3 G* roster**).
**Replaces:** Missing bulk-action affordance in production tables (KC `/students`, KC `/teachers`, KH `/admin/instances`, KH `/admin/payments`).

---

## What this PR ships

- `<BulkActionsBar>` React component covering the 5 spec'd states (default / selecting / bulk-confirm / action-running / action-done) via a closed `BulkAction` enum API + a controlled `<ConfirmDialog>` integration for the destructive `DELETE` action.
- `BulkAction` closed enum union (`'EXPORT_CSV' | 'ARCHIVE' | 'ASSIGN' | 'DELETE'`) — narrowed from the open `Array<{id,label,icon,variant}>` shape in the HTML proto so callsites get TS exhaustiveness + centralised i18n + uniform destructive-confirm wiring.
- `BulkActionsBarProps` shape: `selectedCount` + `onAction` + optional `sticky` (`'top' | 'bottom' | 'none'`) + optional `disabled` + optional `onClearSelection` + optional `lang`.
- `SelectedCount` type alias kept for callsites that prefer to be explicit.
- **Cross-component re-use:** the destructive `Xóa` action consumes the D1 `<ConfirmDialog>` shipped Wave 28 Bucket E (PR #859) — first time a G* component composes a D* dialog inside `@kite/shared-ui`. Imported via relative path AND re-exported from this module. Identity is preserved (`G12.ConfirmDialog === D1.ConfirmDialog`) — no copy-paste drift; same proof-of-concept pattern as G10 → G6 `formatVNCurrency` in Wave 28 Bucket D.
- TypeScript types exported on the public `@kite/shared-ui` API: `BulkActionsBar`, `BulkActionsBarProps`, `BulkAction`, `SelectedCount`.
- Vitest coverage: 14 RTL tests covering all 5 states + 4 action callbacks + the destructive-confirm flow (open / confirm / cancel) + sticky positioning (`top`/`bottom`) + cross-component re-use identity + clear-selection optional callback + default `lang="vi"`.

## State / step mapping

`BulkAction` enum + state mapping (visual treatment + Vietnamese label):

| Action | Variant | Icon | Label (VN) | Confirm? |
|--------|---------|------|------------|----------|
| `EXPORT_CSV` | default | `⬇` | `Xuất CSV` | No |
| `ARCHIVE` | default | `📦` | `Lưu trữ` | No |
| `ASSIGN` | default | `👥` | `Phân lớp` | No |
| `DELETE` | destructive | `🗑` | `Xóa` | **Yes** — opens D1 ConfirmDialog |

Page-level state mapping (HTML-proto-state → component-API rendering):

| HTML proto state | Component API rendering |
|------------------|------------------------|
| `default.html` (no selection) | `selectedCount={0}` → buttons disabled; chip shows `Đã chọn 0`. Hide bar at call-site if you want the slide-out behaviour. |
| `selecting.html` (5 selected) | `selectedCount={5}` → buttons enabled; chip shows `Đã chọn 5`; sticky-bottom positioning. |
| `bulk-confirm.html` (modal open) | User clicked `Xóa` → `<ConfirmDialog>` open with VN copy `Xác nhận xóa hàng loạt`. |
| `action-running.html` (in progress) | `disabled={true}` while parent awaits server work — all 4 buttons disabled, clear-selection still enabled. |
| `action-done.html` (toast + bar hidden) | Parent unmounts the bar after server work + shows its own toast. Component is presentational only. |

The `bulk-confirm` / `running` / `done` states from `spec.md` are split between the bar (confirm flow) and the parent page (running/done are parent-owned).

## Vietnamese formatting

- Count chip copy: `Đã chọn N` (per agent prompt; the HTML proto's `N mục đã chọn` is alternate phrasing — the binding contract for this PR is the agent prompt).
- Action labels per agent prompt: `Xuất CSV` / `Lưu trữ` / `Phân lớp` / `Xóa`.  The HTML proto uses `Xuất Excel` + `Chuyển lớp`; the enum-API approach lets either copy ship via `ACTION_LABELS` if a host app overrides later.
- ConfirmDialog VN copy:
  - Title: `Xác nhận xóa hàng loạt`
  - Description: `Bạn có chắc chắn muốn xóa các mục đã chọn? Hành động này không thể hoàn tác.`
  - Confirm CTA: `Xác nhận xóa` (NOT `Yes` / `Confirm`)
  - Cancel CTA: `Hủy`
- Region landmark: `Thanh thao tác hàng loạt`.
- Clear-selection a11y label: `Bỏ chọn tất cả`.

## Accessibility (WCAG AA)

- Bar wrapper carries `role="region"` + `aria-label="Thanh thao tác hàng loạt"` + `aria-live="polite"` so screen readers announce selection-count changes.
- Count chip carries `role="status"` so SR users hear `Đã chọn N` on selection change.
- All action buttons have visible focus rings (Tailwind `focus-visible:ring-2`).
- Destructive `Xóa` button uses red contrast ≥4.9:1 verified in HTML proto (`selecting.html` HTML comment block).
- The clear-selection button has `aria-label="Bỏ chọn tất cả"`; the `×` glyph is `aria-hidden`.
- D1 ConfirmDialog handles its own focus trap + `role="alertdialog"` + Escape-to-close (Radix native).
- Action button glyphs (`⬇` / `📦` / `👥` / `🗑`) are `aria-hidden` decorative; meaning conveyed through the button's text label in Vietnamese.

## Cross-component utility re-use

This component is the FIRST G* component to compose a D* dialog (the previous proof-of-concept was G10 importing the G6 `formatVNCurrency` helper):

```ts
// inside BulkActionsBar.tsx
import { ConfirmDialog } from '../D1-confirm-dialog';
export { ConfirmDialog } from '../D1-confirm-dialog';
```

Why relative path (not `@kite/shared-ui`):
- The package's `main` is `./src/index.ts`, so `@kite/shared-ui` from inside the package would be a circular module reference.
- Relative path resolves to the same source file, preserves identity, and keeps the import graph acyclic.

A test (`it('cross-component ConfirmDialog re-use ...')`) asserts identity (`G12.ConfirmDialog === D1.ConfirmDialog`) so future refactors that accidentally re-implement the dialog are caught.

## What this PR does NOT ship (deferred)

- Wiring into production routes (`kiteclass-frontend/src/app/students/page.tsx`, `kitehub-frontend/.../admin/instances/page.tsx`, etc.) — host-app concern in a follow-up PR.
- TanStack Table `enableRowSelection` integration — host-app concern (the bar is presentational; the parent owns the selection state).
- Cross-page select-all affordance (`select-all-cross-page.html` proto) — deferred follow-up; the agent-prompt's enum-API does not yet support a "select all 247 across pages" mode. Tracked separately.
- Async progress bar inside the bar (`action-running.html` — `Đang xử lý 3/5…`) — handled by parent page (which renders progress alongside the bar with `disabled={true}` set on the bar). The presentational-only design matches the rest of `@kite/shared-ui`.
- Toast on `action-done.html` (`Đã xóa 5 học sinh` + `[Hoàn tác]` undo) — host-app concern.
- Per-action permission gating (e.g., `ARCHIVE` only for owners) — caller filters which actions to render via `disabled` (v2 will accept an `actionsAllowed: BulkAction[]` prop if needed).
- Open-shape `actions: Array<{id,label,icon,variant,requiresConfirm}>` per the HTML proto — intentionally narrowed to a closed enum for this port; can be widened later if the proto's open shape proves needed.

## Acceptance criteria status (mapping to GAP-273 AC)

- [x] Component ported with TypeScript types
- [x] `spec.md` mirror committed
- [x] Unit tests per state + props edge cases (14 tests; 5 states × N + 4 action callbacks + destructive confirm flow + sticky positioning + cross-component re-use + clear-selection optional + default lang)
- [x] D1 ConfirmDialog re-used (proof-of-concept for cross-component DIALOG sharing inside `@kite/shared-ui`)
- [x] Vietnamese-only labels
- [x] All 12 G* components ported — 12/12 in `@kite/shared-ui` after this PR (Track 2 Phase 3 G* roster CLOSES with G12).
- [ ] Storybook / `/dev/components/` route — out of scope for this PR
- [ ] Production usage ≥107/128 verification — needs host-app wiring + UI review run
- [ ] Visual regression baseline — captured separately under post-wave audit

GAP-273 closes the G12 acceptance line; the remaining open AC items above are tracked in follow-up gaps.
