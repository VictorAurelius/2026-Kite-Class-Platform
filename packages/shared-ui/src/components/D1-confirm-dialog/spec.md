# `ConfirmDialog` — `@kite/shared-ui` (D1)

Radix-native confirmation dialog ported from existing kiteclass-frontend
implementation to `@kite/shared-ui` (Wave 28 Bucket E).

**Source spec (existing implementation):**
[`kiteclass/kiteclass-frontend/src/components/ui/confirm-dialog.tsx`](../../../../kiteclass/kiteclass-frontend/src/components/ui/confirm-dialog.tsx)

**Component gap row:**
[`dossier/04-component-gaps.md` §D1](../../../../documents/02-architecture/design-system/dossier/04-component-gaps.md)

## Why no HTML proto

Per Wave 28 Bucket E briefing — for D1 the **component IS the proto**. Spec
source-of-truth = existing kiteclass-frontend code (50 LOC, 3 callsites in
`(dashboard)/courses/[id]/page.tsx`) + dossier 04 modal-inventory note. Goal
of this PR is to port the API surface **identically** so callsites can swap
the import path with no other change.

## Why Radix Primitives directly (not shadcn `Dialog` wrapper)

The existing kiteclass-frontend implementation imports
`@/components/ui/dialog` (shadcn wrapper). That wrapper depends on:

- `@/lib/utils` (shadcn `cn()` util) — kiteclass-frontend specific path
- `@/components/ui/button` — kiteclass-frontend Button
- `lucide-react` `<X />` icon — extra dep

Adopting any of those in `@kite/shared-ui` would either (a) bloat the lib
with kiteclass-frontend specifics, or (b) duplicate the wrapper inside
shared-ui. We instead consume `@radix-ui/react-dialog` Primitives directly
— same headless WCAG semantics, zero runtime, already a transitive dep of
both KH + KC frontends. Marginal cost = 1 new workspace peer dep.

## Scope shipped (Wave 28 Bucket E)

| Aspect | Status |
|--------|--------|
| `open` / `onOpenChange` controlled state | ✅ |
| `onConfirm` callback + auto-close | ✅ |
| Title + description text props | ✅ |
| Vietnamese defaults (`Xác nhận` / `Hủy`) | ✅ |
| Custom `confirmText` / `cancelText` overrides | ✅ |
| `default` / `destructive` variant | ✅ |
| `role="alertdialog"` semantics | ✅ |
| Focus trap + Escape-to-close (Radix native) | ✅ |
| Overlay click-to-close (Radix native) | ✅ |
| WCAG AA contrast on destructive red button | ✅ (uses `bg-destructive` token) |
| Tailwind classes inlined (no `cn()` dep) | ✅ |
| ≥6 unit tests | ✅ (10 tests shipped) |

## Out of scope (callsite migration → follow-up)

- **Migrate kiteclass-frontend callsites** from local
  `@/components/ui/confirm-dialog` to `@kite/shared-ui` ConfirmDialog.
  3 known sites in `(dashboard)/courses/[id]/page.tsx` + future. Tracked
  as separate follow-up gap; coordinator files at Wave 28 closure.
- **Delete legacy** `kiteclass-frontend/src/components/ui/confirm-dialog.tsx`
  — only after migration verified.
- **kitehub-frontend adoption** — kitehub doesn't currently import
  ConfirmDialog. When kitehub onboards (admin destructive ops?), it imports
  from `@kite/shared-ui` directly — no migration needed.
- **Slot-based custom footer** (e.g., loading spinner inside Confirm
  button while async-confirm in flight). Caller can compose by hiding
  `ConfirmDialog` and rendering custom Radix Dialog instead — not in MVP.
- **Tertiary/secondary variants** beyond `default` + `destructive`. Add
  per-need.

## Public API

See [`types.ts`](./types.ts):

```tsx
import {
  ConfirmDialog,
  type ConfirmDialogProps,
  type ConfirmDialogVariant,
} from '@kite/shared-ui';

<ConfirmDialog
  open={open}
  onOpenChange={setOpen}
  onConfirm={handleDelete}
  title="Xác nhận xóa lớp học"
  description="Hành động này không thể hoàn tác."
  variant="destructive"
/>
```

The component is **controlled** — parent owns `open` state. `onConfirm`
fires before the dialog auto-closes. This mirrors the controlled shape of
`<ConsentBanner>` and `<AttendanceRoster>` in this same package and keeps
fetch / persistence concerns out of the shared lib.

## Back-compat path

Existing kiteclass-frontend implementation
(`kiteclass-frontend/src/components/ui/confirm-dialog.tsx`) **stays
UNTOUCHED** in this PR. The 3 known callsites in
`(dashboard)/courses/[id]/page.tsx` continue importing from the local
wrapper. Migration to `@kite/shared-ui` is a separate follow-up gap (filed
at Wave 28 closure) so this PR's blast radius stays minimal.

## Workspace dep added

`packages/shared-ui/package.json` gains `@radix-ui/react-dialog@^1.1.15` in
both `peerDependencies` (consumers de-dup with their existing copy) and
`devDependencies` (for shared-ui's own tests + type-check). Version
matches kiteclass-frontend's existing pin exactly.
