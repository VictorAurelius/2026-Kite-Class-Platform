'use client';

import { useRef, useEffect, KeyboardEvent, ClipboardEvent, ChangeEvent } from 'react';

/**
 * TotpInput — 6 single-digit boxes with auto-focus-next + paste handling.
 *
 * Wave 72b Bucket B (GAP-516 FE half) per
 * `documents/01-business/kitehub/auth/api-contract.md` POST /api/auth/2fa/verify
 * + POST /api/auth/2fa/enroll-confirm.
 *
 * UX rules:
 * - 6 individual numeric inputs (one per digit) — mobile keyboards show numeric pad
 * - Typing a digit auto-advances focus to next box
 * - Backspace on empty box jumps back + clears prior digit
 * - Paste of "123456" fills all 6 boxes from cursor position
 * - Non-digit input rejected
 * - Disabled state preserves values, blocks edits (used while submit in-flight)
 *
 * Controlled component — parent owns the `value` (6-char string, may be partial)
 * + receives `onChange` callbacks on each edit and `onComplete` when all 6 digits filled.
 *
 * @author KiteHub Team
 * @since Wave 72b Bucket B (GAP-516)
 */

export interface TotpInputProps {
  /** Current value as a string (0–6 digits). Parent owns state. */
  value: string;
  /** Called on every change; receives new 0–6 digit string. */
  onChange: (value: string) => void;
  /** Called when value becomes exactly 6 digits (useful for auto-submit). */
  onComplete?: (value: string) => void;
  /** Disable all boxes (e.g., while submit pending). */
  disabled?: boolean;
  /** Optional aria-label for the whole group. */
  'aria-label'?: string;
  /** Auto-focus first box on mount. Default true. */
  autoFocus?: boolean;
}

const NUM_DIGITS = 6;

export function TotpInput({
  value,
  onChange,
  onComplete,
  disabled = false,
  'aria-label': ariaLabel = 'Mã TOTP 6 số',
  autoFocus = true,
}: TotpInputProps) {
  const inputRefs = useRef<Array<HTMLInputElement | null>>([]);

  // Normalize value to exactly NUM_DIGITS chars (pad with empty)
  const digits = value.padEnd(NUM_DIGITS, ' ').slice(0, NUM_DIGITS).split('');

  useEffect(() => {
    if (autoFocus && !disabled && inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, [autoFocus, disabled]);

  // Fire onComplete when value is exactly 6 digits
  useEffect(() => {
    if (value.length === NUM_DIGITS && /^\d{6}$/.test(value) && onComplete) {
      onComplete(value);
    }
  }, [value, onComplete]);

  const setDigitAt = (index: number, newDigit: string) => {
    const arr = digits.map((d) => (d === ' ' ? '' : d));
    arr[index] = newDigit;
    const trimmed = arr.join('').replace(/\s+/g, '');
    onChange(trimmed);
  };

  const handleChange = (index: number) => (e: ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    // Only allow single digit. If user types multi-char (shouldn't happen given maxLength=1),
    // take the last char.
    const digit = raw.replace(/\D/g, '').slice(-1);
    if (!digit && raw !== '') return; // rejected non-digit

    setDigitAt(index, digit);

    // Auto-advance to next box
    if (digit && index < NUM_DIGITS - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number) => (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      const currentDigit = digits[index];
      if (!currentDigit || currentDigit === ' ') {
        // Empty box: jump back + clear prior
        if (index > 0) {
          e.preventDefault();
          setDigitAt(index - 1, '');
          inputRefs.current[index - 1]?.focus();
        }
      } else {
        // Box has content: clear current (default behavior also clears, but we handle setDigitAt)
        e.preventDefault();
        setDigitAt(index, '');
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      e.preventDefault();
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < NUM_DIGITS - 1) {
      e.preventDefault();
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (index: number) => (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, NUM_DIGITS);
    if (!pasted) return;

    const arr = digits.map((d) => (d === ' ' ? '' : d));
    let cursor = index;
    for (const ch of pasted) {
      if (cursor >= NUM_DIGITS) break;
      arr[cursor] = ch;
      cursor += 1;
    }
    const filled = arr.join('').replace(/\s+/g, '');
    onChange(filled);

    // Focus the next empty box (or last)
    const nextFocus = Math.min(cursor, NUM_DIGITS - 1);
    inputRefs.current[nextFocus]?.focus();
  };

  return (
    <div role="group" aria-label={ariaLabel} className="flex gap-2 justify-center">
      {Array.from({ length: NUM_DIGITS }).map((_, index) => (
        <input
          key={index}
          ref={(el) => {
            inputRefs.current[index] = el;
          }}
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={1}
          pattern="\d{1}"
          aria-label={`Chữ số ${index + 1} của 6`}
          value={digits[index] === ' ' ? '' : digits[index]}
          onChange={handleChange(index)}
          onKeyDown={handleKeyDown(index)}
          onPaste={handlePaste(index)}
          disabled={disabled}
          className="w-12 h-14 text-center text-xl font-mono rounded-xl border bg-background focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-colors disabled:opacity-50"
        />
      ))}
    </div>
  );
}
