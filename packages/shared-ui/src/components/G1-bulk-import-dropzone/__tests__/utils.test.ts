/**
 * Pure-logic tests for the G1 Bulk Import helpers.
 *
 * Validation contract (source of truth: README §Use case + §VN UX +
 * `partial-success.html` error rows):
 *  - Phone: `/^0\d{9,10}$/` after whitespace stripping (10–11 digits leading 0).
 *  - DOB: `dd/MM/yyyy` with calendar round-trip (Feb 30 invalid).
 *  - Name: ≥ 2 whitespace-separated tokens.
 *  - Class: non-empty.
 *
 * Parser contract:
 *  - Strips UTF-8 BOM.
 *  - Handles `\n` and `\r\n`.
 *  - Handles quoted fields with commas inside + `""` escape.
 *  - Reports missing required columns as a single whole-file error.
 *  - 1-based row numbers (header is row 1, first data row is row 2).
 */

import { describe, expect, it } from 'vitest';
import { parseCSV, validateRow } from '../utils';
import type { ImportRow } from '../types';

describe('parseCSV', () => {
  it('strips UTF-8 BOM at start of file', () => {
    const text =
      '﻿ho_va_ten,ngay_sinh,lop,phu_huynh_phone\nNguyễn Văn An,15/08/2015,Lớp 6A1,0901234567\n';
    const { rows, errors } = parseCSV(text);
    expect(errors).toHaveLength(0);
    expect(rows).toHaveLength(1);
    expect(rows[0]?.ho_va_ten).toBe('Nguyễn Văn An');
  });

  it('parses Vietnamese names with diacritics', () => {
    const text =
      'ho_va_ten,ngay_sinh,lop,phu_huynh_phone\nTrần Thị Mỹ Hạnh,01/01/2014,Lớp 7B,0987654321\n';
    const { rows } = parseCSV(text);
    expect(rows[0]?.ho_va_ten).toBe('Trần Thị Mỹ Hạnh');
  });

  it('handles quoted field with embedded comma', () => {
    const text =
      'ho_va_ten,ngay_sinh,lop,phu_huynh_phone\n"Nguyễn, Văn An",15/08/2015,Lớp 6A1,0901234567\n';
    const { rows } = parseCSV(text);
    expect(rows[0]?.ho_va_ten).toBe('Nguyễn, Văn An');
  });

  it('handles `""` escape inside a quoted field', () => {
    const text =
      'ho_va_ten,ngay_sinh,lop,phu_huynh_phone\n"He said ""hi""",15/08/2015,Lớp 6A1,0901234567\n';
    const { rows } = parseCSV(text);
    expect(rows[0]?.ho_va_ten).toBe('He said "hi"');
  });

  it('skips empty rows in the middle of the file', () => {
    const text =
      'ho_va_ten,ngay_sinh,lop,phu_huynh_phone\nNguyễn Văn An,15/08/2015,Lớp 6A1,0901234567\n\nTrần Thị Mai,02/02/2014,Lớp 7B,0987654321\n';
    const { rows } = parseCSV(text);
    expect(rows).toHaveLength(2);
  });

  it('uses 1-based row numbers matching the spreadsheet', () => {
    const text =
      'ho_va_ten,ngay_sinh,lop,phu_huynh_phone\nNguyễn Văn An,15/08/2015,Lớp 6A1,0901234567\nTrần Thị Mai,02/02/2014,Lớp 7B,0987654321\n';
    const { rows } = parseCSV(text);
    // Header is line 1; first data row is line 2; second data row is line 3.
    expect(rows[0]?.row).toBe(2);
    expect(rows[1]?.row).toBe(3);
  });

  it('reports missing required columns as a single whole-file error', () => {
    const text =
      'ho_va_ten,ngay_sinh,lop\nNguyễn Văn An,15/08/2015,Lớp 6A1\n';
    const { rows, errors } = parseCSV(text);
    expect(rows).toHaveLength(0);
    expect(errors).toHaveLength(1);
    expect(errors[0]?.message).toMatch(/phu_huynh_phone/);
  });

  it('returns empty-file error for empty / whitespace input', () => {
    expect(parseCSV('').errors).toHaveLength(1);
    expect(parseCSV('   \n  \r\n').errors).toHaveLength(1);
  });

  it('handles `\\r\\n` line endings (Windows)', () => {
    const text =
      'ho_va_ten,ngay_sinh,lop,phu_huynh_phone\r\nNguyễn Văn An,15/08/2015,Lớp 6A1,0901234567\r\n';
    const { rows } = parseCSV(text);
    expect(rows).toHaveLength(1);
    expect(rows[0]?.ho_va_ten).toBe('Nguyễn Văn An');
  });

  it('tolerates header column reordering', () => {
    const text =
      'phu_huynh_phone,lop,ngay_sinh,ho_va_ten\n0901234567,Lớp 6A1,15/08/2015,Nguyễn Văn An\n';
    const { rows } = parseCSV(text);
    expect(rows[0]?.ho_va_ten).toBe('Nguyễn Văn An');
    expect(rows[0]?.phu_huynh_phone).toBe('0901234567');
  });
});

function row(over: Partial<ImportRow> = {}): ImportRow {
  return {
    ho_va_ten: 'Nguyễn Văn An',
    ngay_sinh: '15/08/2015',
    lop: 'Lớp 6A1',
    phu_huynh_phone: '0901234567',
    row: 2,
    ...over,
  };
}

describe('validateRow (schema=students)', () => {
  it('accepts a fully valid row', () => {
    const r = validateRow(row(), 'students');
    expect(r).toEqual({ valid: true, errors: [] });
  });

  it('accepts 11-digit phone (legacy 09 prefix length)', () => {
    const r = validateRow(row({ phu_huynh_phone: '09012345678' }), 'students');
    expect(r.valid).toBe(true);
  });

  it('strips whitespace inside phone before validating', () => {
    const r = validateRow(
      row({ phu_huynh_phone: '0901 234 567' }),
      'students',
    );
    expect(r.valid).toBe(true);
  });

  it('rejects phone too short (matches partial-success.html row 23 case)', () => {
    const r = validateRow(row({ phu_huynh_phone: '091234' }), 'students');
    expect(r.valid).toBe(false);
    expect(r.errors).toContain('Số điện thoại không hợp lệ');
  });

  it('rejects phone with no leading 0', () => {
    const r = validateRow(
      row({ phu_huynh_phone: '1901234567' }),
      'students',
    );
    expect(r.valid).toBe(false);
    expect(r.errors).toContain('Số điện thoại không hợp lệ');
  });

  it('rejects DOB with wrong separator (matches partial-success.html row 47 case)', () => {
    const r = validateRow(row({ ngay_sinh: '15-08-2015' }), 'students');
    expect(r.valid).toBe(false);
    expect(r.errors).toContain('Ngày sinh sai định dạng');
  });

  it('rejects DOB Feb 30 (impossible calendar date)', () => {
    const r = validateRow(row({ ngay_sinh: '30/02/2015' }), 'students');
    expect(r.valid).toBe(false);
    expect(r.errors).toContain('Ngày sinh sai định dạng');
  });

  it('rejects single-token name (matches partial-success.html row 112 case)', () => {
    const r = validateRow(row({ ho_va_ten: 'Phạm' }), 'students');
    expect(r.valid).toBe(false);
    expect(r.errors).toContain('Họ tên phải có ít nhất 2 từ');
  });

  it('rejects empty class', () => {
    const r = validateRow(row({ lop: '   ' }), 'students');
    expect(r.valid).toBe(false);
    expect(r.errors).toContain('Tên lớp không được để trống');
  });

  it('reports multiple errors when multiple fields invalid', () => {
    const r = validateRow(
      row({ ho_va_ten: 'X', ngay_sinh: 'bad', phu_huynh_phone: 'bad' }),
      'students',
    );
    expect(r.valid).toBe(false);
    expect(r.errors.length).toBeGreaterThanOrEqual(3);
  });
});
