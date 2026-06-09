# GAP-1102: Student bulk-import .xlsx template download

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Mixed (Backend + Frontend + Docs)
**Phase:** phase-1-beta
**Completion:** 85
**Found:** 2026-06-10
**Affects:** `kiteclass-core` bulk-import module + `kiteclass-frontend` `/admin/bulk-import`

## Problem

Luồng bulk-import học viên đã có upload → preview → commit → tải báo cáo lỗi, NHƯNG thiếu một template trắng tải về. User phải tự đoán đúng tên cột (`name | email | phone | date_of_birth | gender | address | note`) + định dạng (SĐT `0xxxxxxxxx`, ngày `dd/MM/yyyy`, gender `MALE/FEMALE`) → dễ đặt sai header → file bị từ chối hoặc lỗi hàng loạt khi parse. Không có cách nào để user biết chính xác cấu trúc file cần điền trước khi upload.

## Proposed Fix

Thêm endpoint `GET /api/v1/students/bulk-import/template` (tenant-agnostic/static, không cần `X-Tenant-Id`) sinh template xlsx trắng bằng Apache POI:
- Sheet `HocVien`: header row = 7 cột canonical (bold + frozen) + 2 dòng ví dụ VN hợp lệ.
- Sheet `HuongDan`: hướng dẫn từng cột (bắt buộc/tùy chọn + định dạng) theo BR-BI-010..015.

Header lấy từ `XlsxParser.COL_*` constants để không drift. FE thêm nút "Tải template mẫu (.xlsx)" đặt TRƯỚC khu vực chọn tệp + helper line "Chưa biết định dạng? Tải template mẫu rồi điền theo." Round-trip guarantee: template parse sạch qua `XlsxParser` + 2 dòng ví dụ VALID qua `RowValidator` (0 lỗi) để user copy theo được.

## Acceptance Criteria

- [x] BE endpoint `GET /api/v1/students/bulk-import/template` → 200 + xlsx attachment `mau-import-hoc-vien.xlsx` (no `X-Tenant-Id`)
- [x] `XlsxTemplateGenerator` round-trips qua `XlsxParser` (parse sạch, ≥2 example rows, name+email resolved)
- [x] 2 example rows VALID qua `RowValidator` (0 lỗi)
- [x] BE tests GREEN: `XlsxTemplateGeneratorTest` (4) + `BulkImportControllerTest` (2) + `StudentBulkImportServiceTest` swept (10)
- [x] FE: `bulkImportApi.downloadTemplate()` blob + nút "Tải template mẫu (.xlsx)" trên `/admin/bulk-import` + FE test GREEN (8/8)
- [x] Docs 3-layer: BR-BI-007 (rules.md) + GET /template (api-contract.md) + UC-BI-04 (use-cases.md)
- [ ] Runtime-walk (browser :3000 → click "Tải template mẫu" → mở file → kiểm tra header + sheet HuongDan + điền + upload lại OK) — **pending coordinator gate**

## Related

- Reference canonical headers: `XlsxParser.COL_NAME .. COL_NOTE` (`name | email | phone | date_of_birth | gender | address | note`)
- Sister flow: GAP-051 (bulk-import MVP) + GAP-137 (FE surface) + GAP-109 (3-layer docs)
- Code: `XlsxTemplateGenerator` + `BulkImportController.downloadTemplate` + `bulk-import.ts` `downloadTemplate`
