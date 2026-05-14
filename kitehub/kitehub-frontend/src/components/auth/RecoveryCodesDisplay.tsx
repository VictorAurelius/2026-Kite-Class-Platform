'use client';

import { useState } from 'react';
import { Copy, Download, Printer, Check } from 'lucide-react';

/**
 * RecoveryCodesDisplay — renders 10 single-use recovery codes in mono font
 * with copy-all + download + print actions.
 *
 * Wave 72b Bucket B (GAP-516 FE half) per
 * `documents/01-business/kitehub/auth/api-contract.md` POST /api/auth/2fa/enroll-init
 * response shape — `recovery_codes: string[10]` shown ONCE.
 *
 * UX rules:
 * - Codes rendered in monospace grid (8 chars per code per BR-AUTH-007 alphabet)
 * - "Copy tất cả" copies all 10 codes newline-separated, with brief "✓ Đã copy" affordance
 * - "Tải xuống" generates a `.txt` blob (filename `kitehub-recovery-codes-<date>.txt`)
 * - "In" triggers `window.print()` with print-friendly styles
 * - Codes are display-only (cannot edit) — parent owns the source list
 *
 * Critical security UX: the codes are shown ONCE per `enroll-init` response —
 * user MUST save before navigating away. Parent gates "Tôi đã lưu mã" confirm
 * button on whether copy/download/print was triggered (optional enforcement).
 *
 * @author KiteHub Team
 * @since Wave 72b Bucket B (GAP-516)
 */

export interface RecoveryCodesDisplayProps {
  codes: string[];
  /** Optional callback after user copies/downloads/prints — used for UX gating. */
  onSaveAction?: (action: 'copy' | 'download' | 'print') => void;
  /** Optional heading override (default Vietnamese). */
  heading?: string;
}

export function RecoveryCodesDisplay({
  codes,
  onSaveAction,
  heading = 'Mã khôi phục',
}: RecoveryCodesDisplayProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    const text = codes.join('\n');
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
      onSaveAction?.('copy');
    } catch {
      // Fallback if clipboard API blocked — surface inline so user can manual-select
      // eslint-disable-next-line no-alert
      alert('Trình duyệt chặn copy tự động. Vui lòng chọn và copy thủ công các mã bên dưới.');
    }
  };

  const handleDownload = () => {
    const text = codes.join('\n');
    const today = new Date().toISOString().slice(0, 10);
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `kitehub-recovery-codes-${today}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    onSaveAction?.('download');
  };

  const handlePrint = () => {
    onSaveAction?.('print');
    window.print();
  };

  return (
    <div className="rounded-2xl border bg-card p-6 shadow-sm">
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <h3 className="text-lg font-semibold">{heading}</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Lưu 10 mã khôi phục — chỉ hiển thị một lần duy nhất. Mỗi mã chỉ dùng được 1 lần để đăng nhập khi mất thiết bị TOTP.
          </p>
        </div>
      </div>

      <ul
        className="grid grid-cols-2 gap-2 rounded-xl bg-muted/30 p-4 font-mono text-sm print:bg-white print:border print:text-base"
        aria-label="Danh sách mã khôi phục"
      >
        {codes.map((code, idx) => (
          <li
            key={idx}
            className="flex items-center gap-2 rounded-lg bg-background px-3 py-2 select-all"
          >
            <span className="text-xs text-muted-foreground w-5 shrink-0">{idx + 1}.</span>
            <span className="font-mono tracking-wider">{code}</span>
          </li>
        ))}
      </ul>

      <div className="mt-4 flex flex-wrap gap-2 print:hidden">
        <button
          type="button"
          onClick={handleCopy}
          className="inline-flex items-center gap-2 rounded-lg border bg-background px-3 py-2 text-sm font-medium hover:bg-accent transition-colors"
          aria-label="Copy tất cả mã khôi phục"
        >
          {copied ? (
            <>
              <Check className="h-4 w-4 text-green-600" />
              Đã copy
            </>
          ) : (
            <>
              <Copy className="h-4 w-4" />
              Copy tất cả
            </>
          )}
        </button>

        <button
          type="button"
          onClick={handleDownload}
          className="inline-flex items-center gap-2 rounded-lg border bg-background px-3 py-2 text-sm font-medium hover:bg-accent transition-colors"
          aria-label="Tải xuống danh sách mã khôi phục"
        >
          <Download className="h-4 w-4" />
          Tải xuống
        </button>

        <button
          type="button"
          onClick={handlePrint}
          className="inline-flex items-center gap-2 rounded-lg border bg-background px-3 py-2 text-sm font-medium hover:bg-accent transition-colors"
          aria-label="In mã khôi phục"
        >
          <Printer className="h-4 w-4" />
          In
        </button>
      </div>

      <p className="mt-4 rounded-lg bg-amber-50 dark:bg-amber-950/20 p-3 text-xs text-amber-700 dark:text-amber-400">
        <strong>Quan trọng:</strong> Lưu mã ở nơi an toàn (password manager, két sắt). Sau khi rời trang này, mã sẽ KHÔNG hiển thị lại. Mỗi mã chỉ dùng được 1 lần.
      </p>
    </div>
  );
}
