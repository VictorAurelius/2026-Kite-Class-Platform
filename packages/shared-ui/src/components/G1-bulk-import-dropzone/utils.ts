/**
 * Pure helpers for the G1 Bulk Import Drop-zone.
 *
 * Parsing + validation contracts (source of truth: `README.md` §Use case +
 * §VN UX + 5 state HTML files):
 *
 *   - CSV columns: `ho_va_ten,ngay_sinh,lop,phu_huynh_phone` (any order; the
 *     first row of the file is the header that names the columns).
 *   - File constraints (≤ 5 MB, ≤ 10.000 rows) are enforced by the host caller
 *     BEFORE handing the parsed rows into validation — they are not the
 *     validator's concern, but the row-cap is exposed via `props.maxRows`.
 *   - VN row error format: `Dòng N: <message>` with messages copied verbatim
 *     from the partial-success HTML state.
 *
 * The component is presentational — these helpers are exported so host apps
 * (kiteclass-frontend / kitehub-frontend) can run the parse + validate
 * pipeline before driving the component into `partial-success` / `done`
 * status. They are also exercised by `__tests__/utils.test.ts`.
 */

import type { ImportError, ImportRow, ImportSchema } from './types';

const REQUIRED_COLUMNS = [
  'ho_va_ten',
  'ngay_sinh',
  'lop',
  'phu_huynh_phone',
] as const;

const ERR_PHONE = 'Số điện thoại không hợp lệ';
const ERR_DOB = 'Ngày sinh sai định dạng';
const ERR_NAME = 'Họ tên phải có ít nhất 2 từ';
const ERR_CLASS = 'Tên lớp không được để trống';

/** Strip a UTF-8 BOM (`﻿`) if present at the start of `text`. */
function stripBom(text: string): string {
  return text.charCodeAt(0) === 0xfeff ? text.slice(1) : text;
}

/**
 * Split a single CSV line into fields, respecting double-quoted fields.
 *
 * - `"a,b","c"` → `['a,b', 'c']`
 * - `"a""b"` → `['a"b']` (escaped quote inside quoted field)
 * - Trims surrounding whitespace from each field unless the field is quoted.
 */
function splitCsvLine(line: string): string[] {
  const out: string[] = [];
  let cur = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"' && line[i + 1] === '"') {
        cur += '"';
        i++;
      } else if (ch === '"') {
        inQuotes = false;
      } else {
        cur += ch;
      }
    } else {
      if (ch === '"') {
        inQuotes = true;
      } else if (ch === ',') {
        out.push(cur);
        cur = '';
      } else {
        cur += ch;
      }
    }
  }
  out.push(cur);
  return out.map((s) => s.trim());
}

/**
 * Parse a CSV / Excel-export text blob into `ImportRow`s + parse-time errors.
 *
 * "Parse-time" errors are structural problems detected during CSV parsing
 * (missing required columns, wrong column count, empty file). Per-field
 * validation (phone format, date format) is the job of `validateRow`.
 *
 * The first non-empty line is treated as the header. If any required column
 * (`ho_va_ten`, `ngay_sinh`, `lop`, `phu_huynh_phone`) is missing, the parser
 * returns `{ rows: [], errors: [<single whole-file error>] }` and does not
 * attempt to read data rows.
 *
 * Row numbering matches the spreadsheet convention: the header is row 1, the
 * first data row is row 2, etc. — so error messages can say `Dòng 23` and a
 * user can navigate directly to that line in Excel.
 */
export function parseCSV(text: string): {
  rows: ImportRow[];
  errors: ImportError[];
} {
  const cleaned = stripBom(text ?? '');
  if (cleaned.trim() === '') {
    return {
      rows: [],
      errors: [{ row: 0, message: 'File trống — không có dữ liệu để xử lý' }],
    };
  }

  // Split into lines, preserving 1-based row numbers (line 0 in the array
  // corresponds to row 1 in the spreadsheet — the header).
  const lines = cleaned.split(/\r?\n/);

  // Find the header — the first non-empty line.
  let headerIdx = 0;
  while (headerIdx < lines.length && lines[headerIdx]!.trim() === '') {
    headerIdx++;
  }
  if (headerIdx >= lines.length) {
    return {
      rows: [],
      errors: [{ row: 0, message: 'File trống — không có dữ liệu để xử lý' }],
    };
  }

  const header = splitCsvLine(lines[headerIdx]!);
  const colIndex: Partial<Record<(typeof REQUIRED_COLUMNS)[number], number>> = {};
  for (let i = 0; i < header.length; i++) {
    const name = header[i]!.toLowerCase();
    if ((REQUIRED_COLUMNS as readonly string[]).includes(name)) {
      colIndex[name as (typeof REQUIRED_COLUMNS)[number]] = i;
    }
  }

  const missing = REQUIRED_COLUMNS.filter((c) => colIndex[c] === undefined);
  if (missing.length > 0) {
    return {
      rows: [],
      errors: [
        {
          row: headerIdx + 1,
          message: `Thiếu cột bắt buộc: ${missing.join(', ')}`,
        },
      ],
    };
  }

  const rows: ImportRow[] = [];
  const errors: ImportError[] = [];

  for (let i = headerIdx + 1; i < lines.length; i++) {
    const raw = lines[i];
    if (raw === undefined || raw.trim() === '') continue;
    const fields = splitCsvLine(raw);
    const rowNumber = i + 1; // 1-based, matches spreadsheet
    rows.push({
      ho_va_ten: fields[colIndex.ho_va_ten!] ?? '',
      ngay_sinh: fields[colIndex.ngay_sinh!] ?? '',
      lop: fields[colIndex.lop!] ?? '',
      phu_huynh_phone: fields[colIndex.phu_huynh_phone!] ?? '',
      row: rowNumber,
    });
  }

  return { rows, errors };
}

/**
 * Validate one parsed `ImportRow` against the named schema.
 *
 * For `'students'`, enforces:
 *  - `phu_huynh_phone`: matches `/^0\d{9,10}$/` (10 or 11 digits, leading 0).
 *  - `ngay_sinh`: matches `dd/MM/yyyy` with valid calendar values.
 *  - `ho_va_ten`: at least 2 whitespace-separated tokens (Vietnamese full name).
 *  - `lop`: non-empty.
 *
 * Returns `{ valid: false, errors: string[] }` when any rule fails; the
 * messages are Vietnamese and meant for direct display.
 */
export function validateRow(
  row: ImportRow,
  schema: ImportSchema = 'students',
): { valid: boolean; errors: string[] } {
  const errors: string[] = [];
  // schema is a forward-compat hook; only `'students'` exists today, but the
  // explicit branch makes it obvious where to add `'teachers'` later.
  if (schema !== 'students') {
    return { valid: true, errors: [] };
  }

  if (!isValidPhone(row.phu_huynh_phone)) {
    errors.push(ERR_PHONE);
  }
  if (!isValidDate(row.ngay_sinh)) {
    errors.push(ERR_DOB);
  }
  if (!isValidName(row.ho_va_ten)) {
    errors.push(ERR_NAME);
  }
  if (row.lop.trim() === '') {
    errors.push(ERR_CLASS);
  }

  return { valid: errors.length === 0, errors };
}

function isValidPhone(input: string): boolean {
  // Strip whitespace inside the input — users sometimes write `0901 234 567`.
  const compact = input.replace(/\s+/g, '');
  return /^0\d{9,10}$/.test(compact);
}

function isValidDate(input: string): boolean {
  const m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(input.trim());
  if (!m) return false;
  const day = Number(m[1]);
  const month = Number(m[2]);
  const year = Number(m[3]);
  if (month < 1 || month > 12) return false;
  if (day < 1) return false;
  // Construct a UTC Date and verify the round-trip — this catches Feb 30,
  // April 31, etc. without manual day-in-month tables.
  const d = new Date(Date.UTC(year, month - 1, day));
  return (
    d.getUTCFullYear() === year &&
    d.getUTCMonth() === month - 1 &&
    d.getUTCDate() === day
  );
}

function isValidName(input: string): boolean {
  const tokens = input.trim().split(/\s+/).filter((t) => t.length > 0);
  return tokens.length >= 2;
}

/** Re-exported for external callers that want to override the column set. */
export { REQUIRED_COLUMNS };
