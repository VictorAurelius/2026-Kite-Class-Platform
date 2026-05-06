'use client';

/**
 * AdvancedModeDisclaimer — Wave 32 Bucket D (GAP-272)
 *
 * Modal shown before enabling Advanced Mode (ENTERPRISE only — §2.4).
 * Confirm button disabled until checkbox is checked.
 *
 * Per ai-branding-guidelines.md §2.4:
 *   - Show disclaimer about unpredictable output
 *   - Require explicit opt-in checkbox
 *   - Fallback template used if AI fails quality gate
 */

import React, { useState } from 'react';
import { AlertTriangle, X } from 'lucide-react';
import { Button } from '@/components/ui/button';

export interface AdvancedModeDisclaimerProps {
  /** Called when user confirms and enables Advanced Mode. */
  onConfirm: () => void;
  /** Called when user cancels or closes. */
  onCancel: () => void;
  /** Whether the modal is visible. */
  open: boolean;
}

export function AdvancedModeDisclaimer({
  open,
  onConfirm,
  onCancel,
}: AdvancedModeDisclaimerProps) {
  const [checked, setChecked] = useState(false);

  if (!open) return null;

  function handleConfirm() {
    if (!checked) return;
    onConfirm();
    setChecked(false);
  }

  function handleCancel() {
    setChecked(false);
    onCancel();
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-label="Cảnh báo: Chế độ Advanced Mode"
    >
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4 overflow-hidden">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 p-6 pb-4 border-b">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-amber-100 p-2">
              <AlertTriangle className="w-5 h-5 text-amber-600" aria-hidden />
            </div>
            <h2 className="text-lg font-bold">Bật Advanced Mode</h2>
          </div>
          <button
            type="button"
            onClick={handleCancel}
            className="text-muted-foreground hover:text-foreground transition-colors"
            aria-label="Đóng"
          >
            <X className="w-5 h-5" aria-hidden />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-4">
          <div className="rounded-lg bg-amber-50 border border-amber-200 p-4 text-sm text-amber-900 space-y-2">
            <p className="font-semibold">Chế độ nâng cao cho phép:</p>
            <ul className="list-disc list-inside space-y-1 text-amber-800">
              <li>Nhập custom prompt tối đa 200 ký tự</li>
              <li>AI tự do hơn trong việc sáng tạo thiết kế</li>
            </ul>
          </div>

          <div className="text-sm text-muted-foreground space-y-2">
            <p>
              Lưu ý: Prompt tự do có thể tạo ra kết quả <strong>không nhất quán</strong> với
              bản sắc thương hiệu của bạn. Wizard 6 bước được thiết kế để đảm bảo chất lượng
              cao nhất — Advanced Mode là dành cho người dùng có kinh nghiệm.
            </p>
            <p>
              Nếu AI không đạt Quality Gate (&lt;70/100), hệ thống sẽ tự động fallback về
              template mặc định.
            </p>
          </div>

          {/* Required checkbox */}
          <label className="flex items-start gap-3 cursor-pointer group">
            <input
              type="checkbox"
              checked={checked}
              onChange={(e) => setChecked(e.target.checked)}
              className="mt-0.5 h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary"
              aria-required="true"
            />
            <span className="text-sm font-medium select-none group-hover:text-foreground text-foreground/80">
              Tôi hiểu AI có thể tạo ra kết quả không nhất quán và đồng ý sử dụng
              Advanced Mode theo trách nhiệm của mình.
            </span>
          </label>
        </div>

        {/* Footer */}
        <div className="flex gap-3 p-6 pt-0">
          <Button
            variant="outline"
            className="flex-1"
            onClick={handleCancel}
          >
            Huỷ
          </Button>
          <Button
            className="flex-1"
            disabled={!checked}
            onClick={handleConfirm}
            aria-disabled={!checked}
          >
            Bật Advanced Mode
          </Button>
        </div>
      </div>
    </div>
  );
}
