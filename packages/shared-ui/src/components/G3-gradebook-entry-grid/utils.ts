/**
 * VN grade validation + Excel-paste parsing helpers for G3 Gradebook Entry Grid.
 *
 * VN 10-point grade scale (per `ui_kits/components/G3-gradebook-entry-grid/`
 * `validation-error.html` rules card + `README.md` §VN UX):
 *
 *   - Range: 0..10 (inclusive at both ends).
 *   - Max 1 decimal place. "8.5" ✅ but "9.25" ❌ ("Tối đa 1 chữ số thập phân").
 *   - VN decimal comma allowed: "7,5" parses as 7.5 (Vietnamese typing convention).
 *   - Empty / whitespace = valid + undefined (cell may legitimately be blank).
 *
 * Excel paste parsing (per `bulk-paste.html` modal):
 *   - TSV rows separated by newline (handles \n and \r\n).
 *   - Each row: column 0 = MST, column 1 = grade. Extra columns ignored.
 *   - Caller decides matching strategy (by MST, row index, etc.).
 */

import type { GradebookCell, ValidateGradeResult } from './types';

const ERR_RANGE = 'Điểm phải trong khoảng 0-10';
const ERR_NON_NUMERIC = 'Điểm phải là số';
const ERR_DECIMALS = 'Tối đa 1 chữ số thập phân';

/**
 * Validate a raw grade string against the VN 10-point scale.
 *
 * Returns:
 *   - `{ valid: true, value: undefined }` for empty / whitespace input.
 *   - `{ valid: true, value: number }` when input parses to a 0..10 number with
 *     at most 1 decimal place.
 *   - `{ valid: false, error: string }` otherwise. Errors are Vietnamese,
 *     copied verbatim from spec.
 */
export function validateGrade(input: string): ValidateGradeResult {
  const trimmed = input.trim();
  if (trimmed === '') {
    return { valid: true, value: undefined };
  }

  // VN convention: comma is the decimal separator. Normalize before parsing.
  const normalized = trimmed.replace(',', '.');

  // Strict numeric format: optional minus, digits, optional .digits.
  // Reject anything else (including "1e3", "+5", trailing letters).
  if (!/^-?\d+(\.\d+)?$/.test(normalized)) {
    return { valid: false, error: ERR_NON_NUMERIC };
  }

  const value = Number(normalized);
  if (!Number.isFinite(value)) {
    return { valid: false, error: ERR_NON_NUMERIC };
  }

  if (value < 0 || value > 10) {
    return { valid: false, error: ERR_RANGE };
  }

  // Decimal-places check: only one digit after the separator allowed.
  const dotIndex = normalized.indexOf('.');
  if (dotIndex !== -1) {
    const fractional = normalized.slice(dotIndex + 1);
    if (fractional.length > 1) {
      return { valid: false, error: ERR_DECIMALS };
    }
  }

  return { valid: true, value };
}

/**
 * Parse Excel-paste TSV into a list of `(studentCode, rawValue)` pairs.
 *
 * - Splits on `\n` (also handles `\r\n`).
 * - Within a row, splits on `\t` and uses columns 0 + 1.
 * - Skips rows that don't have ≥2 non-empty columns.
 * - Trims whitespace around each cell.
 * - Does NOT validate the grade — caller pipes through `validateGrade`.
 */
export function parseExcelPaste(clipboardText: string): GradebookCell[] {
  if (!clipboardText) return [];

  const lines = clipboardText.split(/\r?\n/);
  const cells: GradebookCell[] = [];

  for (const line of lines) {
    if (line.trim() === '') continue;

    const cols = line.split('\t');
    if (cols.length < 2) continue;

    const studentCode = (cols[0] ?? '').trim();
    const rawValue = (cols[1] ?? '').trim();
    if (studentCode === '' || rawValue === '') continue;

    cells.push({ studentCode, rawValue });
  }

  return cells;
}
