'use client';

import type { WizardEvent } from './types';

interface Props {
  send: (e: WizardEvent) => void;
  /** Optional override label (default Vietnamese "Sử dụng mặc định"). */
  label?: string;
  /** Disable when user already customised — caller decides hide vs disable. */
  disabled?: boolean;
}

/**
 * Escape ramp button cho branding wizard (GAP-287 / AC-ONBOARD-002).
 *
 * Khi click: dispatch `USE_DEFAULTS` event → reducer fill unset inputs với defaults
 * + transition thẳng tới `submitting`. AI branding pipeline sẽ chạy với cấu hình mặc định,
 * user có thể quay lại Settings → Branding để re-run wizard chi tiết sau.
 *
 * Visible ở mỗi step từ logo trở đi (welcome cần segment chọn trước để default phù hợp,
 * tuy nhiên defaults.segment='OTHER' đảm bảo skip an toàn nếu user click trước khi pick).
 */
export function UseDefaultsButton({ send, label, disabled }: Props) {
  return (
    <button
      type="button"
      onClick={() => send({ type: 'USE_DEFAULTS' })}
      disabled={disabled}
      data-testid="use-defaults-button"
      className="rounded-md border border-dashed border-muted-foreground/40 bg-muted/20 px-4 py-2 text-sm text-muted-foreground transition hover:bg-muted/40 disabled:opacity-40"
    >
      ⏭️ {label ?? 'Sử dụng mặc định'}
    </button>
  );
}
