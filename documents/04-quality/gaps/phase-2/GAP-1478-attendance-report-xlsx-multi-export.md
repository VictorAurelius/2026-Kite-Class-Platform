# GAP-1478: Báo cáo điểm danh — revamp export sang XLSX + nhiều tiêu chí xuất

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-17 (KC-3 wave-flow — UX enhancement export báo cáo điểm danh)
**Affects:** `kiteclass/kiteclass-frontend/src/app/(dashboard)/attendance/reports/page.tsx`, `.../src/lib/attendance-export.ts` (mới), `.../src/lib/__tests__/attendance-export.test.ts` (mới), `kiteclass/kiteclass-frontend/package.json`

## Problem

Trang **Báo cáo điểm danh** (`/attendance/reports`) chỉ có một nút export duy nhất xuất **CSV** (UTF-8 BOM, mỗi bản ghi 1 dòng — chỉ "chi tiết điểm danh"). Hai hạn chế:

1. **Định dạng CSV** kém thân thiện cho người dùng VN: mở bằng Excel hay bị lỗi delimiter/encoding, không có nhiều sheet, không format đẹp. User muốn **XLSX (Excel)** thật.
2. **Một tiêu chí duy nhất** (chi tiết từng bản ghi). Owner/Teacher thực tế cần xuất theo nhiều góc: theo buổi học (per session), theo học sinh (per student), tổng hợp lớp (KPI). Bản cũ không cho chọn.

## Fix (shipped, FE-side)

- **Thư viện XLSX:** thêm `xlsx` (SheetJS) vào `kiteclass-frontend/package.json` — pin bản CDN CVE-clean `https://cdn.sheetjs.com/xlsx-0.20.3/xlsx-0.20.3.tgz` (npm registry chỉ còn `xlsx@0.18.5` deprecated + dính HIGH CVE prototype-pollution/ReDoS; bản 0.20.3 đã vá). FE chưa từng dùng `xlsx` (bulk-import parse chạy backend), nên đây là dependency MỚI.
- **Lib export testable:** `src/lib/attendance-export.ts` chứa toàn bộ logic build workbook (`XLSX.utils.aoa_to_sheet` + `book_new` + `book_append_sheet`), tách khỏi page để slim + unit-test được. Dùng `XLSX.write(type:'array')` + Blob + anchor (không `writeFile`) để tránh kéo `fs` của Node vào client bundle.
- **4 tiêu chí xuất** = 4 sheet trong CÙNG 1 workbook, người dùng chọn (default cả 4):
  - **Chi tiết điểm danh** (`detail`) — mỗi bản ghi 1 dòng (Học viên / Buổi học / Trạng thái / Ngày / Ghi chú / Điểm)
  - **Theo buổi học** (`session`) — gộp theo buổi (Buổi / Ngày / Tổng / Có mặt / Vắng / Trễ / Phép / Bù / Tỷ lệ)
  - **Theo học sinh** (`student`) — gộp theo học sinh (Tên / Tổng / Có mặt / Vắng / Trễ / Phép / Bù / Tỷ lệ)
  - **Tổng hợp lớp** (`summary`) — khối KPI (tổng lần điểm danh, số học viên, breakdown từng trạng thái + tỷ lệ)
- **UX chọn tiêu chí:** nút chính **"Xuất Excel (N)"** xuất các tiêu chí đang chọn; bên cạnh là `DropdownMenu` checkbox cho phép tick/bỏ tick từng tiêu chí (default cả 4). Dùng đúng data đã fetch trên page (`stats` + `studentStats` + `attendanceData.content`) — KHÔNG refetch. Header tiếng Việt, tên file `bao-cao-diem-danh-<class-slug>-<date>.xlsx`.
- **Unit test:** `attendance-export.test.ts` (13 test) assert cấu trúc workbook (tên 4 sheet + thứ tự ổn định + cell tiêu biểu) + aggregation helper + filename slug. PASS.

## Acceptance Criteria

- [x] Export ra **XLSX** thay vì CSV (thư viện `xlsx` SheetJS, dependency resolved trong package.json).
- [x] Người dùng chọn được nhiều tiêu chí: chi tiết / theo buổi học / theo học sinh / tổng hợp lớp.
- [x] Mỗi tiêu chí = 1 sheet riêng trong cùng workbook; tái dùng data đã fetch (không refetch).
- [x] Header tiếng Việt + filename `bao-cao-diem-danh-<class>-<date>.xlsx`.
- [x] Logic ở `src/lib/attendance-export.ts` (testable) + unit test assert cấu trúc workbook.
- [x] `pnpm --filter kiteclass-frontend build` PASS + eslint 0 lỗi mới + vitest PASS.
- [ ] Human G2 walk: chọn lớp → tick tiêu chí → "Xuất Excel" → mở file Excel kiểm tra đủ sheet + số liệu đúng.

## Related

- Sibling GAP-1476 (envelope drift) + GAP-1474 (roster/attendance PENDING_PAYMENT) cùng trang `/attendance/reports` — đã fix trên main; gap này là UX enhancement (revamp export), không phụ thuộc trực tiếp.
- Discovered in: wave-flow-kc3 export revamp 2026-06-17.
