/**
 * AdvancedModeDisclaimer — explicit consent modal before enabling Advanced Mode.
 *
 * Wave 32 Bucket D — Direction C 6-step refactor.
 *
 * Per `ai-branding-guidelines.md` §2.4: ENTERPRISE Advanced Mode opt-in MUST
 * include an explicit disclaimer about unpredictable output before the toggle
 * is ON.
 *
 * Behavior:
 *   - Modal renders the §2.4-mandated disclaimer + acceptance checkbox.
 *   - Confirm button is DISABLED until the checkbox is checked.
 *   - On confirm the parent flips the Advanced Mode toggle ON and persists.
 *   - Cancel keeps the toggle OFF (the safe default).
 *
 * Test surface (matches `__tests__/AdvancedModeDisclaimer.test.tsx`):
 *   - Modal renders all 5 disclaimer bullets
 *   - Confirm button disabled by default
 *   - Confirm enables only after checkbox checked
 *   - Cancel handler fires onCancel + does NOT call onConfirm
 */

'use client';

import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { AlertTriangle, Zap } from 'lucide-react';

export interface AdvancedModeDisclaimerProps {
  /** Whether the modal is open. Caller controls open state. */
  open: boolean;
  /** Open-change handler — fires on dismiss / cancel / confirm. */
  onOpenChange: (open: boolean) => void;
  /** Click handler when user confirms (only enabled after checkbox checked). */
  onConfirm: () => void;
  /** Click handler when user cancels (keeps toggle OFF). */
  onCancel: () => void;
}

const DISCLAIMER_BULLETS: readonly string[] = [
  'Output AI có thể không match brand guideline mặc định',
  'Quality gate vẫn chạy — score <70 sẽ fallback về template gốc',
  'Mỗi prompt qua 3-stage moderation pipeline (PII / brand-safety / hate-speech)',
  'Audit log lưu 90 ngày — đội kỹ thuật + legal có thể xem',
  'Cap 16.000 tokens per request (cấu hình lại trong Settings)',
];

export function AdvancedModeDisclaimer(props: AdvancedModeDisclaimerProps) {
  const { open, onOpenChange, onConfirm, onCancel } = props;
  const [accepted, setAccepted] = useState(false);

  // Reset acceptance state when modal closes — prevents leaky cross-open state.
  useEffect(() => {
    if (!open) setAccepted(false);
  }, [open]);

  const handleConfirm = () => {
    if (!accepted) return; // Guard — should also be disabled by UI
    onConfirm();
  };

  const handleCancel = () => {
    onCancel();
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="sm:max-w-lg"
        data-testid="advanced-mode-disclaimer-modal"
      >
        <DialogHeader>
          <div className="flex items-start gap-3 mb-2">
            <div
              className="w-12 h-12 rounded-full grid place-items-center shrink-0"
              style={{ background: 'hsl(38 92% 50% / 0.15)' }}
            >
              <AlertTriangle className="w-6 h-6" style={{ color: 'hsl(38 92% 50%)' }} />
            </div>
            <div>
              <DialogTitle>Xác nhận bật Advanced Mode</DialogTitle>
              <DialogDescription>
                Tính năng dành cho ENTERPRISE — đọc kỹ trước khi bật
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-3 text-sm">
          <p className="text-muted-foreground">
            Advanced Mode mở khoá <strong>free-form prompt 200 ký tự</strong> ở Bước 5 wizard.
            AI có thể tạo output không khớp brand guideline thông thường.
          </p>

          <div
            className="p-3 rounded-md text-xs"
            style={{
              background: 'hsl(38 92% 50% / 0.08)',
              border: '1px solid hsl(38 92% 50% / 0.3)',
            }}
            data-testid="advanced-mode-disclaimer-bullets"
          >
            <p className="font-semibold mb-1.5" style={{ color: 'hsl(38 92% 40%)' }}>
              Khi BẬT, bạn chấp nhận:
            </p>
            <ul className="space-y-1 text-muted-foreground">
              {DISCLAIMER_BULLETS.map((bullet, idx) => (
                <li
                  key={idx}
                  className="flex items-start gap-2"
                  data-testid={`advanced-mode-disclaimer-bullet-${idx}`}
                >
                  <span
                    aria-hidden="true"
                    className="block w-1 h-1 rounded-full bg-current mt-1.5 shrink-0"
                  />
                  <span>{bullet}</span>
                </li>
              ))}
            </ul>
          </div>

          <label className="flex items-start gap-2 cursor-pointer">
            <Checkbox
              checked={accepted}
              onCheckedChange={(checked) => setAccepted(checked === true)}
              className="mt-1"
              data-testid="advanced-mode-disclaimer-checkbox"
            />
            <span className="text-sm">
              Tôi hiểu các rủi ro trên và đồng ý chịu trách nhiệm với output từ Advanced Mode
            </span>
          </label>
        </div>

        <DialogFooter className="sm:justify-stretch gap-2">
          <Button
            type="button"
            variant="secondary"
            className="flex-1"
            onClick={handleCancel}
            data-testid="advanced-mode-disclaimer-cancel-button"
          >
            Huỷ — giữ wizard
          </Button>
          <Button
            type="button"
            className="flex-1"
            disabled={!accepted}
            onClick={handleConfirm}
            data-testid="advanced-mode-disclaimer-confirm-button"
          >
            <Zap className="w-4 h-4" />
            Bật Advanced Mode
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
