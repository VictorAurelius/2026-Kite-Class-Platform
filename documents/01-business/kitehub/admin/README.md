# admin — KiteHub Platform Admin v1 endpoints

**Last updated:** 2026-05-18
**Domain:** Admin v1 API endpoints — instance/payment/revenue management cho `PLATFORM_ADMIN`
**Source-of-truth controllers:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/Admin{Instances,Payments,Revenue}Controller.java`

---

## Mục đích

Domain `admin` quản lý các endpoint quản trị platform-level cho role `PLATFORM_ADMIN` — bao gồm read-only operations về tenant instances, payment transactions, và revenue aggregations. Đây là layer admin v1 hiện đại (`/api/v1/admin/*`), thay thế legacy layer `/api/platform/admin/*` (deprecated, sunset planned).

Khác biệt với sister domain `admin-audit`:
- `admin/` — endpoint operations (đọc dữ liệu admin cần để vận hành)
- `admin-audit/` — audit log infrastructure (ghi nhận admin actions per PDPL Art 11)

---

## Cấu trúc thư mục

| File | Mục đích |
|---|---|
| `README.md` | Tổng quan domain (file này) |
| `rules.md` | Business rules (BR-ADMIN-V1-001..003) — auth + role + rate limit constraints |
| `use-cases.md` | Use cases (UC-ADMIN-V1-001..006) — actor flows per 6 endpoints |
| `api-contract.md` | API contract — 6 v1 endpoints + legacy deprecation policy + DTO references |

---

## Phạm vi

- **Thuộc domain này:** 6 admin v1 endpoints (`GET /api/v1/admin/{instances,payments,revenue}/*`), legacy deprecation policy, role guard contract
- **Không thuộc domain này:**
  - Audit logging (xem `admin-audit/`)
  - Authentication flow (xem `auth/`)
  - Subscription business logic (xem `subscription-billing/`)
  - Instance lifecycle management (xem `instance-provisioning/`)

---

## API surface (6 endpoints v1)

Per `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/`:

| Controller | Endpoint | Method |
|---|---|---|
| `AdminInstancesController` | `GET /api/v1/admin/instances` | List paginated |
| `AdminInstancesController` | `GET /api/v1/admin/instances/{id}` | Get by ID |
| `AdminPaymentsController` | `GET /api/v1/admin/payments/pending` | List pending payments |
| `AdminPaymentsController` | `GET /api/v1/admin/payments/summary` | Aggregated summary |
| `AdminRevenueController` | `GET /api/v1/admin/revenue` | Revenue list paginated |
| `AdminRevenueController` | `GET /api/v1/admin/revenue/summary` | Revenue summary |

Mỗi controller có class-level `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` per Wave 97 Bucket A (GAP-637).

---

## Archive policy

Domain admin v1 docs maintained khi:
- Thay đổi controller signature
- Thêm/bớt admin endpoint
- Thay đổi role/permission model
- Thay đổi deprecation status của legacy v0 endpoints

Quarterly review per `output-review-mandate.md`.
