# GAP-1424: Bulk-import preview/commit trả 500 khi multipart thiếu `file` part / sai Content-Type

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-15 (KC-4 G2 re-walk — chứng nhận flow trước khi dev walk)
**Affects:** `kiteclass-core` — `POST /api/v1/students/bulk-import/{preview,commit}` (và mọi multipart upload endpoint dùng `@RequestParam MultipartFile`)

## Problem

Trong lúc re-walk KC-4 (bulk import) qua đường production-accurate, hai biến thể malformed request rơi vào catch-all `@ExceptionHandler(Exception.class)` → **HTTP 500 `SYSTEM_INTERNAL_ERROR`**:

1. **Multipart có nhưng thiếu `file` part** → `MissingServletRequestPartException: Required part 'file' is not present` → 500 (đúng phải 400).
2. **POST không có Content-Type multipart** → `HttpMediaTypeNotSupportedException` → 500 (đúng phải 415).

Cùng class với GAP-988 (bad-format → 4xx không 500) mà recipe KC-4 Bước 5 đang chứng nhận. Các sad-path đã có handler vẫn đúng: CSV → 415 `BULK_IMPORT_INVALID_FILE_TYPE`, file hỏng → 400 `BULK_IMPORT_PARSE_ERROR`, file rỗng → 400 `BULK_IMPORT_EMPTY_FILE`. Chỉ 2 biến thể "thiếu part / sai Content-Type" lọt 500.

FE thật luôn gửi multipart + file part nên dev G2 happy-path không hit; nhưng 500 (thay vì 4xx) trên malformed input là robustness gap — fix inline khi re-walk certify flow (per `small-gap-inline-fix.md`).

## Proposed Fix

Thêm 2 `@ExceptionHandler` vào `GlobalExceptionHandler` (đặt TRƯỚC catch-all, theo pattern `MaxUploadSizeExceededException` của GAP-988):
- `MissingServletRequestPartException` → 400 `MISSING_REQUEST_PART`.
- `HttpMediaTypeNotSupportedException` → 415 `UNSUPPORTED_MEDIA_TYPE`.

## Acceptance Criteria

- [x] POST preview multipart thiếu `file` part → **400 `MISSING_REQUEST_PART`** (không 500).
- [x] POST preview không Content-Type multipart → **415 `UNSUPPORTED_MEDIA_TYPE`** (không 500).
- [x] Các path đã đúng giữ nguyên: valid XLSX → 200, CSV → 415, file hỏng → 400, file rỗng → 400.

## Related

- Discovered in: KC-4 G2 re-walk session 2026-06-15 (goal "rewalk lại hết flow để dev làm G2 thuận lợi").
- Cùng class: GAP-988 (bulk-import bad-format → 4xx).
- Fix per `small-gap-inline-fix.md` (4 tiêu chí nhỏ pass) + `discovery-to-gap-inline-filing.md`.
