/**
 * Pure-logic tests for VN gradebook validators + Excel-paste parser used by
 * G3 Gradebook Entry Grid.
 *
 * Validation contract (source of truth: HTML proto + README §VN UX +
 * `validation-error.html` rules card):
 *  - VN 10-point scale: 0..10, max 1 decimal place ("8.5 hợp lệ; 9.25 không").
 *  - Empty → `{ valid: true, value: undefined }` (cell may legitimately be empty).
 *  - VN decimal comma accepted: "7,5" parses as 7.5.
 *  - Out-of-range → "Điểm phải trong khoảng 0-10".
 *  - Non-numeric → "Điểm phải là số".
 *  - >1 decimal places → "Tối đa 1 chữ số thập phân".
 *
 * Excel-paste contract: TSV (tab-separated) within a row, newlines between
 * rows. Each row produces ONE GradebookCell — first column = student id (MST),
 * second column = grade. Trailing/empty cells skipped.
 */

import { describe, expect, it } from 'vitest';
import { validateGrade, parseExcelPaste } from '../utils';

describe('validateGrade', () => {
  it('accepts integer 0', () => {
    const r = validateGrade('0');
    expect(r.valid).toBe(true);
    expect(r.value).toBe(0);
  });

  it('accepts integer 10', () => {
    const r = validateGrade('10');
    expect(r.valid).toBe(true);
    expect(r.value).toBe(10);
  });

  it('accepts 1-decimal value 7.5 (period)', () => {
    const r = validateGrade('7.5');
    expect(r.valid).toBe(true);
    expect(r.value).toBe(7.5);
  });

  it('accepts 1-decimal value 7,5 (Vietnamese decimal comma)', () => {
    const r = validateGrade('7,5');
    expect(r.valid).toBe(true);
    expect(r.value).toBe(7.5);
  });

  it('treats empty / whitespace as valid + undefined (cell may be blank)', () => {
    expect(validateGrade('')).toEqual({ valid: true, value: undefined });
    expect(validateGrade('   ')).toEqual({ valid: true, value: undefined });
  });

  it('rejects values >10 with VN error', () => {
    const r = validateGrade('10.5');
    expect(r.valid).toBe(false);
    expect(r.error).toBe('Điểm phải trong khoảng 0-10');
  });

  it('rejects negative values with VN error', () => {
    const r = validateGrade('-1');
    expect(r.valid).toBe(false);
    expect(r.error).toBe('Điểm phải trong khoảng 0-10');
  });

  it('rejects non-numeric input with VN error', () => {
    const r = validateGrade('abc');
    expect(r.valid).toBe(false);
    expect(r.error).toBe('Điểm phải là số');
  });

  it('rejects >1 decimal places (VN teacher convention) — "9.25" invalid', () => {
    const r = validateGrade('9.25');
    expect(r.valid).toBe(false);
    expect(r.error).toBe('Tối đa 1 chữ số thập phân');
  });

  it('rejects >1 decimal places using comma notation ("9,25")', () => {
    const r = validateGrade('9,25');
    expect(r.valid).toBe(false);
    expect(r.error).toBe('Tối đa 1 chữ số thập phân');
  });

  it('accepts spec example boundary values 8.5 and 9.5', () => {
    expect(validateGrade('8.5').valid).toBe(true);
    expect(validateGrade('9.5').valid).toBe(true);
  });

  it('rejects "11" (out of range, integer)', () => {
    expect(validateGrade('11').valid).toBe(false);
  });
});

describe('parseExcelPaste', () => {
  it('parses single row "MST\\tgrade" → 1 cell', () => {
    const rows = parseExcelPaste('HS-10A2-001\t8.5');
    expect(rows).toHaveLength(1);
    expect(rows[0]!).toEqual({ studentCode: 'HS-10A2-001', rawValue: '8.5' });
  });

  it('parses multi-row TSV (newline-separated)', () => {
    const text = 'HS-10A2-001\t8.5\nHS-10A2-002\t9.0\nHS-10A2-003\t7.5';
    const rows = parseExcelPaste(text);
    expect(rows).toHaveLength(3);
    expect(rows[0]!.studentCode).toBe('HS-10A2-001');
    expect(rows[1]!.rawValue).toBe('9.0');
    expect(rows[2]!.studentCode).toBe('HS-10A2-003');
  });

  it('handles VN decimal comma in pasted grades', () => {
    const rows = parseExcelPaste('HS-10A2-001\t7,5\nHS-10A2-002\t8,0');
    expect(rows[0]!.rawValue).toBe('7,5');
    expect(rows[1]!.rawValue).toBe('8,0');
  });

  it('handles \\r\\n line endings (Windows Excel)', () => {
    const rows = parseExcelPaste('HS-10A2-001\t8.5\r\nHS-10A2-002\t9.0');
    expect(rows).toHaveLength(2);
    expect(rows[1]!.studentCode).toBe('HS-10A2-002');
  });

  it('skips empty / whitespace-only rows', () => {
    const text = 'HS-10A2-001\t8.5\n\n   \nHS-10A2-002\t9.0\n';
    const rows = parseExcelPaste(text);
    expect(rows).toHaveLength(2);
  });

  it('returns empty array for empty input', () => {
    expect(parseExcelPaste('')).toEqual([]);
    expect(parseExcelPaste('   \n\n')).toEqual([]);
  });

  it('handles mixed-column input — uses first 2 tab-separated columns', () => {
    // Excel may include extra metadata columns; we still map (col0, col1).
    const rows = parseExcelPaste('HS-10A2-001\t8.5\textra\tdata');
    expect(rows).toHaveLength(1);
    expect(rows[0]!).toEqual({ studentCode: 'HS-10A2-001', rawValue: '8.5' });
  });

  it('handles malformed row (only 1 column) — skips it', () => {
    const text = 'HS-10A2-001\nHS-10A2-002\t9.0';
    const rows = parseExcelPaste(text);
    expect(rows).toHaveLength(1);
    expect(rows[0]!.studentCode).toBe('HS-10A2-002');
  });

  it('trims whitespace in studentCode + rawValue', () => {
    const rows = parseExcelPaste('  HS-10A2-001  \t  8.5  ');
    expect(rows[0]!).toEqual({ studentCode: 'HS-10A2-001', rawValue: '8.5' });
  });
});
