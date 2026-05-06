# G6 Invoice Detail — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G6-invoice-detail/spec.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G6-invoice-detail/spec.md)
**Component gap:** G6 per `dossier/04-component-gaps.md` §G6
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — stays 🟡 PARTIAL after this port.
**Wave:** 27 Bucket B (paired with G2/G5/G7 in same wave per `wave-track-2-ui-kits-port-umbrella.md`).
**Replaces:** KH `/billing/payment/[id]` baseline (33/128 🔴) + KC `/billing/[id]`.

---

## What this PR ships

- `<InvoiceDetail>` React component covering 5 spec'd states (`loading`, `default` ≡ `pending`, `paid`, `overdue`, `print-view`).
- `formatVNCurrency(amount)` — VND digit grouping + lowercase `đ` suffix + U+2212 minus for negatives.
- `formatVNTax(rate)` — accepts decimal (`0.08`) or whole-number (`8`) input → `"8%"`.
- TypeScript types exported on the public `@kite/shared-ui` API: `InvoiceDetail`, `InvoiceDetailProps`, `InvoiceLineItem`, `InvoiceTaxBreakdown`, `formatVNCurrency`, `formatVNTax`.
- Vitest coverage: 16 utility tests (currency edge cases, tax formats) + 13 component state-render tests including tax-breakdown summation.

## Status / state mapping

| `state` prop | Header | Status pill | Body | Actions |
|---|---|---|---|---|
| `loading` | hidden | hidden | skeleton (header + 3 line items + total row) | none |
| `default` | shown | derived from `invoice.status` | full | Pay-now + Download PDF + Email |
| `pending` | shown | warning pill `Chờ thanh toán` | full | Pay-now + Download PDF + Email |
| `paid` | shown | success pill `Đã thanh toán` | full + paid date | Download receipt + Email |
| `overdue` | shown + `role="alert"` banner | destructive pill `Quá hạn` | full + late-fee row (if caller adds it as `intent: 'destructive'`) | Pay-now (destructive variant) + Download |
| `print-view` | nav header hidden | shown | full + optional VN tax-invoice header (`isVATInvoice`) | none |

## Print-friendly contract

- No fixed pixel widths in main layout — all utility classes resolve to `rem` / `%` / responsive breakpoints (per spec).
- `data-print-view="true"` set on the root when `state === 'print-view'`. Host app's print stylesheet can target this attribute (`[data-print-view] { ... }`) or the test-id `invoice-detail-print-view` to apply A4 page rules without coupling to internal class names.
- VN tax invoice (`isVATInvoice: true`) renders the `HÓA ĐƠN GIÁ TRỊ GIA TĂNG` header block per Nghị định 123/2020/NĐ-CP.

## Vietnamese formatting

- Currency: `1.500.000đ` / `0đ` / `−200.000đ` (U+2212 minus, NOT ASCII hyphen).
- Date: `dd/MM/yyyy` short (UTC accessors so test fixtures + production match across timezones).
- VAT rate: `8%`, `10%`, `0%` — accepts `0.08` or `8` input.
- Status copy: `Chờ thanh toán` / `Trả góp` / `Đã thanh toán` / `Quá hạn` / `Đã hủy`.

## What this PR does NOT ship (deferred)

- Remaining 11 G* components (G1, G2, G3, G4, G5, G7..G12) — separate buckets / waves under [GAP-273](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md).
- Wiring into production routes (`kiteclass-frontend/src/app/billing/[id]/page.tsx`, `kitehub-frontend/.../billing/payment/[id]`) — host-app concern in a follow-up PR.
- PARTIAL_PAID progress sub-label (`Trả góp đợt 1/3 with progress`) — basic pill copy only in v1.
- Print stylesheet (`@page { size: A4 portrait; ... }`) — host app responsibility; the component exposes the `data-print-view` marker.

## Acceptance criteria status (mapping to GAP-273 AC)

- [x] Component ported with TypeScript types
- [x] `spec.md` mirror committed
- [x] Unit tests per state + props edge cases (29 tests)
- [x] G6 VN currency + tax format helpers exported and tested
- [x] Vietnamese-only labels
- [ ] All 12 components ported — 1/12 in this PR (Wave 27 Bucket B); 11 remaining
- [ ] Storybook / `/dev/components/` route — out of scope for this PR
- [ ] Production usage ≥105/128 verification — needs host-app wiring + UI review run
- [ ] Visual regression baseline — captured separately under post-wave audit

GAP-273 stays 🟡 PARTIAL.
