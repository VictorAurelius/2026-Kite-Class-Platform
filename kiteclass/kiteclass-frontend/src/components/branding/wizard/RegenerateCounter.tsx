'use client';

import type { Tier } from './types';

interface Props {
  tier: Tier;
  used: number;
  limit: number;
}

/**
 * Per-tier regenerate quota display.
 * FREE=3 · PRO=10 · PREMIUM=30 · ENTERPRISE=∞
 *
 * @since Wave 3 Sub-PR 3.7
 */
export function RegenerateCounter({ tier, used, limit }: Props) {
  const unlimited = !Number.isFinite(limit);
  const remaining = unlimited ? Number.POSITIVE_INFINITY : Math.max(0, limit - used);
  const blocked = !unlimited && remaining === 0;

  return (
    <div
      aria-live="polite"
      className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-sm ${
        blocked ? 'border-destructive text-destructive' : 'border-border text-muted-foreground'
      }`}
    >
      <span className="font-medium">{tier}</span>
      <span>·</span>
      {unlimited ? (
        <span>Regenerate: không giới hạn</span>
      ) : (
        <span>
          Regenerate: <strong>{remaining}</strong>/{limit} còn lại
        </span>
      )}
    </div>
  );
}
