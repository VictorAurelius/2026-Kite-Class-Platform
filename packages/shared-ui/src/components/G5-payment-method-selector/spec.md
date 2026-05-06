# G5 — PaymentMethodSelector (port to `@kite/shared-ui`)

**Wave:** 27 Bucket C
**Dossier source:** [`documents/02-architecture/design-system/ui_kits/components/G5-payment-method-selector/spec.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G5-payment-method-selector/spec.md)
**Component gap:** G5 per `dossier/04-component-gaps.md` §G5
**Used by:** KC `/billing/[id]/pay`, KH `/billing/upgrade`
**Persona:** Student / Parent / P2 Owner (paying user)

## Scope shipped (this PR)

The `method-selecting` state of the dossier state machine: a single-pick
radio-group of VN payment gateways with optional badges for "Phổ biến" and
"Chuyển hướng" markers. Vietnamese labels copy-pasted verbatim from the
HTML proto's `default.html`.

**Implemented:**
- 5 gateway IDs: `VNPAY | MOMO | ZALOPAY | BANK | CASH` (verbatim from spec)
- VN labels: `VNPay`, `Ví MoMo`, `ZaloPay`, `Chuyển khoản ngân hàng`, `Tiền mặt tại trung tâm`
- Single-pick radio-group semantics (native HTML; arrow keys cycle, space selects)
- Disabled-method support: muted visual + non-interactive + skipped in keyboard nav
- Selected state visible at WCAG AA via border + background tint (NOT color-only —
  native radio dot is the non-color signal)
- Optional `popular` badge ("Phổ biến") and `redirect` badge ("Chuyển hướng")
- Custom `name` + `ariaLabel` props for tenant-specific embedding

## Out of scope (deferred to follow-up sub-bucket)

- `loading-qr` state (spinner while QR generates)
- `qr-displayed` state (200×200 QR + countdown timer + "Đã thanh toán xong")
- `expired` state (15-minute QR timeout + regenerate CTA)
- `success` state (confetti + receipt download)
- `failure-retry` state (gateway error + retry CTA)
- Amount confirmation card + invoice metadata display
- Bank-info copy-paste UI for `BANK` method (account number + transfer code)
- Trust strip (`✓ Bảo mật bởi VNPay/MoMo`) and legal-footer text
- Currency formatting helper (`1.500.000đ` lowercase đ + dot separator)

These belong to a richer composite (e.g. `<PaymentFlow>`) that consumes this
selector. The selector is the atomic radio-group primitive — kept narrow on
purpose so KC and KH can compose differently.

## Note on `BANK_TRANSFER` / `QR` discrepancy

The Wave 27 Bucket C briefing prompt mentioned a 6-method `PaymentMethod` enum
including `BANK_TRANSFER` and `QR`. The dossier `spec.md` (source of truth)
defines 5 methods: `VNPAY | MOMO | ZALOPAY | BANK | CASH`. Per the
`audit-to-gap-pipeline.md` §2.5 state-check rule, the dossier wins — `QR` is a
state reached via MoMo/ZaloPay, not a separate method. We use `BANK` (not
`BANK_TRANSFER`) per spec verbatim.

## Props

```ts
type PaymentMethodSelectorProps = {
  options: PaymentMethodOption[];   // ordered: VNPay → MoMo → ZaloPay → Bank → Cash typical
  selectedMethod?: PaymentMethod;   // undefined = none picked yet (controlled)
  onChange: (method: PaymentMethod) => void;
  name?: string;                    // default 'payment-method'
  ariaLabel?: string;               // default 'Phương thức thanh toán'
  id?: string;                      // optional radiogroup container id
};

type PaymentMethodOption = {
  id: PaymentMethod;
  label: string;                    // VN label, verbatim from dossier
  description?: string;
  disabled?: boolean;
  popular?: boolean;                // renders "Phổ biến" chip
  redirect?: boolean;               // renders "Chuyển hướng" chip
};
```

## Accessibility

- `role="radiogroup"` + `aria-label="Phương thức thanh toán"` (overridable)
- Native `<input type="radio">` per option → standard arrow-key cycling, space
  to select, tab into group
- Disabled options receive native `disabled` attr → automatically skipped by
  arrow-key cycling (browser-managed)
- `aria-describedby` on the radio when option has a description, pointing to
  the description span id
- Focus visible via `accent-primary` (theme token) on the radio dot + the
  card's primary border on selection
- Color contrast: theme tokens `text-foreground` on `bg-card` ≥ 17.9:1 (AAA),
  `text-muted-foreground` on `bg-card` ≥ 4.7:1 (AA)
- No color-only signals — selection state communicated via border colour AND
  the radio dot AND data-checked attribute

## Test coverage

`__tests__/PaymentMethodSelector.test.tsx` — 9 tests:

1. Renders radiogroup with all 5 VN payment methods
2. VN labels match spec verbatim
3. Single-pick semantics (only `selectedMethod` is checked)
4. Click on unselected fires `onChange` with new id
5. Click on already-selected does NOT re-fire `onChange`
6. Disabled option click does NOT fire `onChange`
7. Disabled option excluded from keyboard arrow cycling
8. Popular + redirect badges render when flagged
9. Custom `name` + `ariaLabel` props apply

## Files

- [`PaymentMethodSelector.tsx`](./PaymentMethodSelector.tsx) — component
- [`types.ts`](./types.ts) — `PaymentMethod`, `PaymentMethodOption`, props
- [`index.tsx`](./index.tsx) — barrel re-exports
- [`__tests__/PaymentMethodSelector.test.tsx`](./__tests__/PaymentMethodSelector.test.tsx) — vitest + RTL
