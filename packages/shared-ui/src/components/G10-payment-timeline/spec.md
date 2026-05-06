# G10 Payment Status Timeline — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G10-payment-timeline/README.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G10-payment-timeline/README.md) + 5 state HTML files under `states/`.
**Component gap:** G10 per `dossier/04-component-gaps.md` §G10.
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — stays 🟡 PARTIAL after this port.
**Wave:** 28 Bucket D (5th of 12 G* — first cross-component re-use of a shared util).
**Replaces:** Implicit status chips in KH `/admin/payments` table + KC `/billing/[id]`.

---

## What this PR ships

- `<PaymentStatusTimeline>` React component covering the 5 spec'd states (`pending`, `paid`, `partial-paid`, `overdue`, `refunded`).
- `PaymentTimelineStep` granular event union (`CREATED` / `PAYMENT_PENDING` / `PAYMENT_RECEIVED` / `CONFIRMED` / `COMPLETED` / `FAILED` / `REFUNDED`) — backend-event-shaped, NOT page-level state.
- `TimelineEvent` shape: `step` + `at` + optional `note` / `actor` / `amount` / `status` — one event per `<li>` in the timeline `<ol>`.
- **Cross-component re-use proof-of-concept:** `formatVNCurrency` is imported from sibling `G6-invoice-detail/utils` via relative path AND re-exported from this module. Identity is preserved (`G10.formatVNCurrency === G6.formatVNCurrency`) — no copy-paste drift; first time a G* component re-uses another G*'s helper.
- TypeScript types exported on the public `@kite/shared-ui` API: `PaymentStatusTimeline`, `PaymentStatusTimelineProps`, `PaymentTimelineState`, `PaymentTimelineStep`, `TimelineEvent`, `TimelineEventStatus`.
- Vitest coverage: 10 RTL tests covering all 5 states + canonical step ordering + `aria-current="step"` behaviour + datetime format + cross-component re-use + default `lang="vi"`.

## State / step mapping

`PaymentTimelineState` (page-level pill + body colour story):

| State | Pill copy | Pill colour | Banner | Default current step |
|-------|-----------|-------------|--------|----------------------|
| `pending` | `Chờ thanh toán` | warning | none | `PAYMENT_PENDING` |
| `paid` | `Đã thanh toán` | success | none | `COMPLETED` |
| `partial-paid` | `Trả góp` | info | none | `PAYMENT_RECEIVED` |
| `overdue` | `Quá hạn` | destructive | `role="alert"` overdue banner | `FAILED` |
| `refunded` | `Đã hoàn tiền` | warning | none | `REFUNDED` |

Caller can override the derived current step via `currentStep` prop when backend semantics require it.

`PaymentTimelineStep` (per-event icon + colour) — canonical lifecycle order via `STEP_ORDER`:

| Step | Icon | Default visual | Vietnamese label |
|------|------|----------------|------------------|
| `CREATED` | `✓` | success | `Đã phát hành hóa đơn` |
| `PAYMENT_PENDING` | `⏳` | warning (current state) / muted (future) | `Chờ thanh toán` |
| `PAYMENT_RECEIVED` | `✓` | success | `Đã nhận thanh toán` |
| `CONFIRMED` | `✓` | success | `Đã xác nhận` |
| `COMPLETED` | `✓` | success | `Hoàn thành` |
| `FAILED` | `✗` | destructive (always, regardless of position) | `Thất bại` |
| `REFUNDED` | `💸` | warning | `Đã hoàn tiền` |

## Vietnamese formatting

- Currency: `1.500.000đ` / `0đ` / `−200.000đ` — re-uses G6 `formatVNCurrency` (U+2212 minus, lowercase Latin `đ`, no space).
- Datetime: `dd/MM/yyyy HH:mm` (e.g. `14/10/2026 19:42`) — UTC accessors so test fixtures + production match across timezones; matches HTML proto exactly.
- Vietnamese-only labels copied verbatim from `README.md` + `states/*.html`.

## Accessibility (WCAG AA)

- Status pill carries `role="status"` + `aria-label` with the pill copy so screen readers announce state.
- Overdue banner carries `role="alert"` + `aria-live="polite"`.
- Timeline rendered as semantic `<ol aria-label="Timeline trạng thái thanh toán">` with one `<li>` per event.
- Current event carries `aria-current="step"` on its icon span (the step icon itself is `aria-hidden`, the title text + label-via-aria is the source of meaning — colour is NOT the only signal).
- Step icon glyphs (`✓` / `⏳` / `✗` / `💸`) are `aria-hidden` decorative; meaning conveyed through the event title text in Vietnamese.
- Connector lines between events are `aria-hidden` decorative.
- Time elements use `dateTime={event.at.toISOString()}` so AT can read the ISO form if needed.
- Future events get `opacity-50` for visual de-emphasis but the text remains in the DOM (not hidden).

## Cross-component utility re-use

This component is the FIRST G* component to import another G*'s helper:

```ts
// inside PaymentStatusTimeline.tsx
import { formatVNCurrency } from '../G6-invoice-detail/utils';
export { formatVNCurrency } from '../G6-invoice-detail/utils';
```

Why relative path (not `@kite/shared-ui`):
- The package's `main` is `./src/index.ts`, so `@kite/shared-ui` from inside the package would be a circular module reference.
- Relative path resolves to the same source file, preserves identity, and keeps the import graph acyclic.

A test (`it('cross-component formatVNCurrency re-use ...')`) asserts identity (`G10.formatVNCurrency === G6.formatVNCurrency`) so future refactors that accidentally re-implement the helper are caught.

## What this PR does NOT ship (deferred)

- Remaining 7 G* components (G1, G3, G4, G8, G9, G11, G12) — separate buckets / waves under [GAP-273](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md).
- Wiring into production routes (`kiteclass-frontend/src/app/billing/[id]/page.tsx`, `kitehub-frontend/.../admin/payments/[id]/page.tsx`) — host-app concern in a follow-up PR.
- Horizontal-on-desktop / vertical-on-mobile auto-toggle via media query — the `orientation` prop accepts `'horizontal'` as a structural hook, but the responsive auto-toggle is not in v1 (the HTML proto in `states/*.html` is vertical-only).
- Per-step CTAs (`Thanh toán ngay` / `Tải biên lai` / `Liên hệ hỗ trợ`) — caller renders these alongside the timeline; this component is presentational only.
- Reminder schedule sub-card (`Lịch nhắc nhở tự động: còn 3 ngày...`) — separate sub-component (deferred).
- Late-fee itemisation panel — handled by G6 `<InvoiceDetail>` (this component shows the timeline, not the invoice line items).
- `PARTIAL_PAID` progress bar (`60% progress`) — basic pill copy + amount-paid-so-far in the event note only in v1; full progress sub-component deferred.

## Acceptance criteria status (mapping to GAP-273 AC)

- [x] Component ported with TypeScript types
- [x] `spec.md` mirror committed
- [x] Unit tests per state + props edge cases (10 tests; 5 states × 1 + step ordering + aria-current + datetime + cross-component re-use + default lang)
- [x] G6 VN currency helper re-used (proof-of-concept for cross-component utils inside `@kite/shared-ui`)
- [x] Vietnamese-only labels
- [ ] All 12 components ported — 5/12 in this PR (Wave 27 shipped 4; this Wave 28 Bucket D ships G10); 7 remaining
- [ ] Storybook / `/dev/components/` route — out of scope for this PR
- [ ] Production usage ≥105/128 verification — needs host-app wiring + UI review run
- [ ] Visual regression baseline — captured separately under post-wave audit

GAP-273 stays 🟡 PARTIAL.
