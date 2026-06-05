# GAP-988: Bulk-import non-XLSX/malformed upload → HTTP 500 thay vì 400/413/415

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (file-upload validation — KC-4)
**Found:** 2026-06-05 (Wave flow-kc4 pre-walk persona simulation)
**Affects:** `BulkImportController` `/api/v1/students/bulk-import/{preview,commit}` + bulk-import service `parseSafely`

## Problem

Bulk-import parser dùng Apache POI `XSSFWorkbook` (XLSX-only). Khi upload file KHÔNG phải XLSX (CSV, ảnh đổi tên, file rỗng, oversized, MIME spoof), POI ném **RuntimeException** mà `parseSafely` chỉ catch `IOException` → exception propagate → `GlobalExceptionHandler` generic handler trả **HTTP 500** thay vì 400 (bad format) / 415 (unsupported media) / 413 (too large). Thiếu MIME/extension pre-check trước parse. User upload nhầm CSV (phổ biến vì UI/docs nói "CSV") → 500 khó hiểu.

## Proposed Fix

(1) MIME + extension pre-check (`.xlsx` + `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) → 415 nếu sai. (2) Wrap POI runtime exceptions trong parse → `ValidationException` → 400. (3) Size limit → 413. (4) Đồng bộ docs/UI: rõ "file XLSX" + cung cấp `/template` download.

## Acceptance Criteria
- [ ] Upload CSV/ảnh/file rỗng → 400/415 (not 500)
- [ ] Upload oversized → 413
- [ ] Upload XLSX hợp lệ → 200 preview
- [ ] IT cover non-XLSX → 4xx

## Related
- Discovered in: Wave flow-kc4 pre-walk 2026-06-05 (`audits/persona-review/2026-06-05-pre-walk-kc4-enrollment-bulk-import.md` FM #1+#2)
- File-upload checklist: `pre-handoff-self-test-completeness.md` §2.5
