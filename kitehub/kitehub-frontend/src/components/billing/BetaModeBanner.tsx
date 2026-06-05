'use client';

/**
 * Beta payment-mode banner (Wave flow-kh3-2, GAP-977).
 *
 * Rendered only when {@code NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE === 'true'} (mirrors
 * the backend {@code kitehub.payment.beta-mode.enabled} flag). Tells the owner the
 * displayed transfer amount is the symbolic 10.000đ used during Phase 1 BETA.
 */
export function BetaModeBanner() {
  if (process.env.NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE !== 'true') {
    return null;
  }

  return (
    <div
      className="rounded-lg border border-amber-400/60 bg-amber-50 p-4"
      data-testid="beta-mode-banner"
    >
      <p className="text-sm text-amber-800">
        🧪 Bạn đang ở chế độ Beta — số tiền chuyển là{' '}
        <strong>10.000đ tượng trưng</strong>. Khi vào production sẽ là{' '}
        <strong>599.000đ/tháng</strong>.
      </p>
    </div>
  );
}
