/**
 * VN currency + tax-rate formatting helpers for G6 Invoice Detail.
 *
 * Why hand-rolled and not raw `Intl.NumberFormat({ style: 'currency' })`:
 *   `Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })`
 *   produces `"4.500.000 ₫"` — note the U+20AB symbol and a trailing space.
 *   The HTML protos and the spec mandate lowercase Latin `đ` with NO space:
 *     `4.500.000đ`, `−200.000đ`, `0đ`.
 *   So we use `Intl.NumberFormat` only for the digit-grouping piece and
 *   append the suffix ourselves.
 *
 * Negative amounts use the typographic minus U+2212 (`−`) to match the
 * discount/late-fee rows in the spec'd HTML protos (e.g. `−200.000đ`).
 */

const VN_NUMBER_FORMAT = new Intl.NumberFormat('vi-VN', {
  maximumFractionDigits: 0,
  useGrouping: true,
});

const MINUS = '−'; // U+2212, NOT the ASCII hyphen-minus.

/**
 * Format a VND amount as `1.500.000đ` / `0đ` / `−200.000đ`.
 *
 * - Rounds to nearest integer (VND has no fractional unit).
 * - Returns `0đ` for `NaN` / non-finite inputs (defensive — never blow up
 *   the whole invoice page for one bad number).
 */
export function formatVNCurrency(amount: number): string {
  if (!Number.isFinite(amount)) {
    return '0đ';
  }
  const rounded = Math.round(amount);
  if (rounded === 0) {
    return '0đ';
  }
  if (rounded < 0) {
    return `${MINUS}${VN_NUMBER_FORMAT.format(-rounded)}đ`;
  }
  return `${VN_NUMBER_FORMAT.format(rounded)}đ`;
}

/**
 * Format a VAT rate as `8%` / `10%` / `0%`.
 *
 * Accepts either decimal (`0.08`) or whole-number (`8`) inputs:
 * - rate < 1 → treated as decimal, multiplied by 100
 * - rate ≥ 1 → treated as already a percentage
 *
 * Always rounds to nearest integer percent (VN VAT rates are whole).
 */
export function formatVNTax(rate: number): string {
  if (!Number.isFinite(rate)) {
    return '0%';
  }
  const pct = rate < 1 ? rate * 100 : rate;
  return `${Math.round(pct)}%`;
}
