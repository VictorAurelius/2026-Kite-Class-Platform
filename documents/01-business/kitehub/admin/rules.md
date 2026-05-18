# Admin v1 — Business Rules

**Domain:** Admin v1 endpoints (GAP-638 — Wave 97 Bucket B1 3-layer foundation)
**Source-of-truth controllers:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/`
**Last verified:** 2026-05-18 (Wave 97 Bucket B1 — admin docs only; DTO refactor defer B2)

---

## Config keys

| Key | Default | Mô tả |
|---|---|---|
| `kitehub.admin.api-v1.rate-limit-per-minute` | `30` | Số request tối đa per admin user trên endpoints v1 |
| `kitehub.admin.legacy.deprecation-sunset-date` | `2026-09-30T23:59:59Z` | Sunset date cho legacy `/api/platform/admin/*` endpoints |
| `kitehub.admin.page-size.default` | `50` | Page size mặc định cho list endpoints |
| `kitehub.admin.page-size.max` | `200` | Page size tối đa cho phép trong query param |

---

## BR-ADMIN-V1-001 — Mọi endpoint v1 require PLATFORM_ADMIN role

**Rule:** Mọi endpoint dưới `/api/v1/admin/**` PHẢI enforce role check `PLATFORM_ADMIN` tại class-level via `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`. Request từ role khác (TENANT_USER, TEACHER, MANAGER, etc.) → HTTP 403 Forbidden + AccessDeniedException.

**Source:** OWASP A01 Broken Access Control + `pre-launch-owasp-rest-hardening-checklist.md` §2.1 + GAP-637 fix Wave 97 Bucket A

**Rationale:** Defense-in-depth: gateway routing layer chỉ verify JWT signature, không enforce role claim cụ thể. Nếu attacker có valid JWT (bất kỳ role nào) + biết endpoint URL → có thể truy cập admin data. Class-level annotation đảm bảo mọi method trong controller đều check role nhất quán.

**Reviewer:** @nguyenvankiet (solo-dev Wave 97 Bucket B1)

**Compliance check:** Code review verify mọi `Admin*Controller` trong `kitehub-admin/src/main/java/com/kitehub/admin/controller/` có annotation; `Admin*ControllerSecurityTest` test 403 cho TENANT_USER + TEACHER roles (per Wave 97 Bucket A GAP-637 PR #1540).

**Review cadence:** Mỗi khi thêm admin controller mới hoặc khi `pre-launch-owasp-rest-hardening-checklist.md` cập nhật.

---

## BR-ADMIN-V1-002 — Pagination + filter mandatory cho list endpoints

**Rule:** List endpoints (`GET /api/v1/admin/instances`, `GET /api/v1/admin/payments/pending`, `GET /api/v1/admin/revenue`) PHẢI support:
- Query param `page` (default `0`, 0-indexed)
- Query param `size` (default `kitehub.admin.page-size.default=50`, max `kitehub.admin.page-size.max=200`)
- Response format Spring `Page<T>` với `content` + `totalElements` + `totalPages` + `page` + `size`

**Source:** GAP-432 bounded queries (Wave 92 Bucket B) — chống unbounded query risk gây OOM khi tenant scale.

**Rationale:** Admin có thể có 100+ tenant instances + 10k+ payments trong production. Unbounded `findAll()` → load all rows vào memory → OOM. Pagination mandatory + max size cap = defense.

**Reviewer:** @nguyenvankiet (solo-dev Wave 97 Bucket B1)

**Compliance check:** Unit test verify endpoint với `size=99999` request được clamp xuống `kitehub.admin.page-size.max`; integration test verify response shape conform `Page<T>` format.

**Review cadence:** Mỗi khi thêm list endpoint mới.

---

## BR-ADMIN-V1-003 — Legacy /api/platform/admin/* phải @Deprecated + emit Sunset header

**Rule:** Mọi controller dưới `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/platform/` (legacy v0 layer) PHẢI:
1. Mark `@Deprecated(since = "v1", forRemoval = true)` tại class level
2. Emit HTTP response header `Sunset: Sat, 30 Sep 2026 23:59:59 GMT` (per config key `kitehub.admin.legacy.deprecation-sunset-date`)
3. Emit response header `Link: </api/v1/admin/{equivalent}>; rel="successor-version"` chỉ đến endpoint v1 tương đương

**Source:** RFC 8594 (Sunset HTTP Header) + IETF deprecation best practice.

**Rationale:** FE clients có thể vẫn dùng legacy endpoints sau khi v1 ship. Không có deprecation signal → silent breakage khi legacy removed. Sunset header + Link header cho phép FE detect + plan migration trước sunset date.

**Reviewer:** @nguyenvankiet (solo-dev Wave 97 Bucket B1)

**Compliance check:** Integration test verify legacy endpoint response chứa `Sunset` + `Link` headers; log warn count requests đến legacy endpoint sau sunset date.

**Review cadence:** Trigger khi v1 endpoint mới ship + khi sunset date approaches (T-30, T-7, T-0).

**Note:** Wave 97 Bucket B1 surface rule này nhưng KHÔNG implement legacy deprecation — defer B2 cùng DTO refactor.

---

## Related rules + sister docs

- `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1 — @PreAuthorize mandate
- `.claude/rules/contract-first-for-cross-layer.md` §3 — code+doc cùng PR
- [`use-cases.md`](./use-cases.md) — UC-ADMIN-V1-001..006 actor flows
- [`api-contract.md`](./api-contract.md) — endpoint contract
- Sister domain `admin-audit/` — audit log infrastructure (GAP-640 Wave 97 Bucket C)
- Wave 97 Bucket A (GAP-637) — @PreAuthorize implementation
