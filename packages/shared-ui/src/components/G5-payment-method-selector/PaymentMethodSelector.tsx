'use client';

/**
 * PaymentMethodSelector — VN multi-gateway payment-method picker.
 *
 * Wave 27 Bucket C scope = the `method-selecting` state of the spec's state
 * machine. Renders a single-pick radio-group of methods, with optional badges
 * for "Popular" / "Redirect" and full WCAG AA semantics. Other states
 * (loading-qr, qr-displayed, expired, success, failure-retry) defer to a
 * follow-up bucket — see `spec.md` for full lifecycle.
 *
 * Behaviour:
 *  - Single-pick semantics: only one radio checked at a time (native HTML).
 *  - Disabled options render muted and are excluded from arrow-key cycling
 *    (native radio-group behaviour — disabled radios are non-tabbable AND
 *    skipped by arrow keys).
 *  - Selected state visible at WCAG AA contrast via border + background tint
 *    (theme tokens `border-primary` + `bg-primary/5`, contrast pre-validated
 *    in upstream theme audits).
 *  - Vietnamese labels: copy-pasted verbatim from
 *    `documents/02-architecture/design-system/ui_kits/components/G5-payment-method-selector/default.html`.
 *
 * No new deps — uses only React + native HTML radios. No icon library; badges
 * are text chips.
 */

import type React from 'react';
import { useId } from 'react';
import type { PaymentMethodSelectorProps } from './types';

const DEFAULT_ARIA_LABEL = 'Phương thức thanh toán';
const DEFAULT_NAME = 'payment-method';
const POPULAR_LABEL = 'Phổ biến';
const REDIRECT_LABEL = 'Chuyển hướng';

export function PaymentMethodSelector(
  props: PaymentMethodSelectorProps,
): React.JSX.Element {
  const {
    options,
    selectedMethod,
    onChange,
    name = DEFAULT_NAME,
    ariaLabel = DEFAULT_ARIA_LABEL,
    id,
  } = props;

  const reactId = useId();
  const groupId = id ?? `pmselect-${reactId}`;

  return (
    <div
      role="radiogroup"
      aria-label={ariaLabel}
      id={groupId}
      data-testid="payment-method-selector"
      className="flex flex-col gap-2"
    >
      {options.map((opt) => {
        const checked = selectedMethod === opt.id;
        const optionId = `${groupId}-${opt.id}`;
        return (
          // WCAG: contrast ratio target ≥ 4.5:1 for body text.
          // Theme tokens (`border`, `bg-card`, `text-foreground`,
          // `border-primary`, `bg-primary/5`) are pre-validated upstream;
          // selection state visible via colored border + tinted background
          // (NOT color alone — checked radio dot is also a non-color signal).
          <label
            key={opt.id}
            htmlFor={optionId}
            className={[
              'flex cursor-pointer items-start gap-3 rounded-lg border bg-card p-3 transition-colors',
              checked
                ? 'border-primary bg-primary/5'
                : 'border-border hover:bg-muted/40',
              opt.disabled
                ? 'cursor-not-allowed opacity-60 hover:bg-card'
                : '',
            ]
              .filter(Boolean)
              .join(' ')}
            data-testid={`payment-method-option-${opt.id}`}
            data-checked={checked ? 'true' : 'false'}
            data-disabled={opt.disabled ? 'true' : 'false'}
          >
            <input
              id={optionId}
              type="radio"
              name={name}
              value={opt.id}
              checked={checked}
              disabled={opt.disabled}
              onChange={() => {
                // onChange fires only on transition to checked. React-controlled
                // radios won't re-fire when clicking the already-checked option.
                if (!checked && !opt.disabled) {
                  onChange(opt.id);
                }
              }}
              className="mt-1 h-4 w-4 shrink-0 cursor-pointer accent-primary disabled:cursor-not-allowed"
              // Use aria-label so the radio's accessible name is exactly the
              // payment-method label ("VNPay" / "Ví MoMo" / etc.), independent
              // of nested badge / description spans inside the visual <label>.
              aria-label={opt.label}
              aria-describedby={
                opt.description ? `${optionId}-desc` : undefined
              }
            />
            <span className="flex min-w-0 flex-col gap-1">
              <span className="flex flex-wrap items-center gap-2 text-sm font-medium text-foreground">
                {opt.label}
                {opt.popular && (
                  <span
                    className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary"
                    data-testid={`payment-method-badge-popular-${opt.id}`}
                  >
                    {POPULAR_LABEL}
                  </span>
                )}
                {opt.redirect && (
                  <span
                    className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground"
                    data-testid={`payment-method-badge-redirect-${opt.id}`}
                  >
                    {REDIRECT_LABEL}
                  </span>
                )}
              </span>
              {opt.description && (
                <span
                  id={`${optionId}-desc`}
                  className="text-xs leading-relaxed text-muted-foreground"
                >
                  {opt.description}
                </span>
              )}
            </span>
          </label>
        );
      })}
    </div>
  );
}

export default PaymentMethodSelector;
