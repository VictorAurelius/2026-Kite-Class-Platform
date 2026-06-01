---
audience: mixed
---

# Analytics Report — API Contract

**Domain:** analytics-report
**Source:** GAP-775
**Base path:** `/api/v1/reports`
**Auth:** `hasRole('ADMIN')` cho cả 2 endpoint (OWASP A01 role gate — BR-RPT-AUTHZ-001)
**Tenant:** `X-Tenant-Id` header bắt buộc (Hibernate tenant filter)
**Response envelope:** `ApiResponse<T>` (`{ success, data, message?, timestamp }`)

---

## GET /api/v1/reports/revenue

Báo cáo doanh thu tháng (BR-RPT-REV).

### Request

| Param | In | Type | Default | Note |
|-------|----|----|---------|------|
| `months` | query | int | 12 | Số tháng gần nhất; clamp `[1, 36]` server-side (BR-RPT-SCOPE-004) |

### Response 200

```json
{
  "success": true,
  "data": {
    "period": "month",
    "months": 12,
    "totalRevenue": 15000000,
    "points": [
      { "month": "2025-07", "amount": 0 },
      { "month": "2026-06", "amount": 1500000 }
    ]
  },
  "timestamp": "2026-06-02T00:00:00Z"
}
```

| Field | Type | Note |
|-------|------|------|
| `period` | string | luôn `"month"` (Phase 1) |
| `months` | int | cửa sổ thực tế sau clamp |
| `totalRevenue` | number (VND) | tổng amount toàn cửa sổ (BR-RPT-REV-004) |
| `points[].month` | string | ISO `YYYY-MM`, sắp xếp cũ → mới |
| `points[].amount` | number (VND) | doanh thu tháng đó (0 nếu trống) |

### Error codes

| Code | HTTP | Khi nào |
|------|------|---------|
| Access Denied | 403 | User không có role `ADMIN` (BR-RPT-AUTHZ-003) |
| (tenant missing) | 4xx/5xx | Thiếu `X-Tenant-Id` → `TenantNotSetException` |

---

## GET /api/v1/reports/attendance

Báo cáo tỷ lệ điểm danh tháng (BR-RPT-ATT).

### Request

| Param | In | Type | Default | Note |
|-------|----|----|---------|------|
| `months` | query | int | 12 | Số tháng gần nhất; clamp `[1, 36]` server-side |

### Response 200

```json
{
  "success": true,
  "data": {
    "period": "month",
    "months": 12,
    "overallPresentRate": 92.5,
    "points": [
      { "month": "2025-07", "presentCount": 0, "totalCount": 0, "presentRate": 0.0 },
      { "month": "2026-06", "presentCount": 37, "totalCount": 40, "presentRate": 92.5 }
    ]
  },
  "timestamp": "2026-06-02T00:00:00Z"
}
```

| Field | Type | Note |
|-------|------|------|
| `period` | string | luôn `"month"` |
| `months` | int | cửa sổ thực tế sau clamp |
| `overallPresentRate` | number | phần trăm `[0,100]` toàn cửa sổ, 1 chữ số (BR-RPT-ATT-005) |
| `points[].month` | string | ISO `YYYY-MM` |
| `points[].presentCount` | int | số buổi PRESENT |
| `points[].totalCount` | int | tổng số buổi điểm danh |
| `points[].presentRate` | number | PRESENT/total × 100, 1 chữ số; 0 khi total=0 (BR-RPT-ATT-004) |

### Error codes

| Code | HTTP | Khi nào |
|------|------|---------|
| Access Denied | 403 | User không có role `ADMIN` |
| (tenant missing) | 4xx/5xx | Thiếu `X-Tenant-Id` |

---

## Verification chain

| BR | UC | Endpoint | @Mapping | @Test |
|----|----|---------|---------|------|
| BR-RPT-REV-001..005 | UC-RPT-001 | `GET /reports/revenue` | `ReportController.getRevenueReport` | `ReportServiceImplTest.revenue_*` + `ReportControllerIT.revenue_*` |
| BR-RPT-ATT-001..006 | UC-RPT-002 | `GET /reports/attendance` | `ReportController.getAttendanceReport` | `ReportServiceImplTest.attendance_*` + `ReportControllerIT.attendance_*` |
| BR-RPT-AUTHZ-001..003 | UC-RPT-001/002 E1 | cả 2 | `@PreAuthorize("hasRole('ADMIN')")` | `ReportControllerIT.*_nonAdmin_denied` |
| BR-RPT-SCOPE-004 | UC-RPT-001 E3 | cả 2 | `ReportServiceImpl.clampMonths` | `ReportServiceImplTest.revenue_clampsWindow` |
