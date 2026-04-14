# Academic Year — API Contract

**Base path:** `/api/v1/academic-years`
**Auth:** Bearer token + `X-Tenant-Id` header

## POST /api/v1/academic-years
**Use case:** UC-ACYR-01
**Request:**
```json
{
  "name": "2026-2027",
  "startDate": "2026-09-05",
  "endDate": "2027-06-15"
}
```
**Response 201:**
```json
{
  "id": 1,
  "name": "2026-2027",
  "startDate": "2026-09-05",
  "endDate": "2027-06-15",
  "status": "UPCOMING",
  "holidaysCount": 8
}
```
**Errors:**
- 400 `ACADEMIC_YEAR_NAME_EXISTS`
- 400 `INVALID_DATE_RANGE`

---

## POST /api/v1/academic-years/{id}/set-current
**Use case:** UC-ACYR-02
**Response 200:** updated year with status=CURRENT

---

## GET /api/v1/academic-years/current
**Use case:** UC-ACYR-03
**Response 200:**
```json
{
  "id": 1,
  "name": "2026-2027",
  "startDate": "2026-09-05",
  "endDate": "2027-06-15",
  "status": "CURRENT",
  "currentSemester": {
    "id": 2,
    "type": "HK1",
    "weekNumber": 8,
    "totalWeeks": 18
  }
}
```
**Errors:**
- 404 `NO_CURRENT_YEAR` nếu chưa có CURRENT

---

## GET /api/v1/academic-years/{id}
Standard fetch.

---

## GET /api/v1/academic-years
**Query params:** `?status=UPCOMING|CURRENT|COMPLETED&page=0&size=20`
Returns paginated list.

---

## GET /api/v1/academic-years/{id}/holidays
**Response 200:** List holidays ordered by startDate.

---

## POST /api/v1/academic-years/{id}/holidays
**Use case:** UC-ACYR-04
**Request:**
```json
{
  "name": "Ngày thành lập trường",
  "startDate": "2026-10-15",
  "endDate": "2026-10-15",
  "type": "SCHOOL"
}
```

## Log
- 2026-04-14 — Initial API contract
