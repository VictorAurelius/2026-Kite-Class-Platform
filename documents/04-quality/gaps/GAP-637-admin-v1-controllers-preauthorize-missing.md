# GAP-637: Admin v1 controllers thiếu @PreAuthorize + 403 tests

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (Security)
**Detected:** 2026-05-18 (Wave 92 post-wave audit suite per GAP-619)
**Related Audits:** [documents/04-quality/audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md](../audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md)

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|---|---|---|
| `AdminInstancesController` | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/v1/AdminInstancesController.java` | 🟡 partial — controller tồn tại nhưng thiếu class-level `@PreAuthorize` |
| `AdminPaymentsController` | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/v1/AdminPaymentsController.java` | 🟡 partial — thiếu `@PreAuthorize` |
| `AdminRevenueController` | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/v1/AdminRevenueController.java` | 🟡 partial — thiếu `@PreAuthorize` |
| 403 MockMvc tests cho 3 controllers | `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/api/v1/` | ❌ missing — không có `@WebMvcTest` verify 403 cho non-admin role |
| `pre-launch-owasp-rest-hardening-checklist.md` §2.1 | `.claude/rules/` | ✅ shipped — mandate documented |

**Grep commands run:**

```bash
grep -rn "@PreAuthorize" kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/v1/
grep -rn "hasRole" kitehub/kitehub-admin/src/test/java/com/kitehub/admin/
find kitehub/kitehub-admin/src/test/java -name "Admin*ControllerTest.java"
```

## Problem

Audit API Contract Wave 92 Bucket D (2026-05-18) phát hiện 3 admin v1 controllers `/api/v1/admin/{instances,payments,revenue}` trong `kitehub-admin` thiếu **class-level annotation** `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`. Đây là vi phạm trực tiếp:

- **OWASP A01 Broken Access Control** — endpoints admin chỉ dựa trên gateway routing layer + JWT claim, không enforce role check tại controller boundary
- **`pre-launch-owasp-rest-hardening-checklist.md` §2.1** mandate explicit `@PreAuthorize` per admin endpoint
- **`roles/api-contract.md`** mandate role-based gating documented

Đồng thời, 403 forbidden response tests cũng missing — không có `@WebMvcTest` verify rằng request từ non-admin role (ví dụ `TENANT_USER`, `TEACHER`) bị reject với HTTP 403.

## Context

Wave 92 Bucket D ship 3 admin v1 endpoints scaffold + UI consumption nhưng skip security hardening layer. Production exposure risk cao vì:

1. Gateway forwarding logic chỉ check JWT signature, không check role claim cụ thể
2. Nếu attacker có valid JWT (bất kỳ role nào) + biết endpoint URL → có thể truy cập admin data
3. Phase 1 BETA gate yêu cầu zero P0 security finding

## Proposed Fix

### Step 1: Add class-level @PreAuthorize

```java
@RestController
@RequestMapping("/api/v1/admin/instances")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminInstancesController { ... }
```

Apply tương tự cho `AdminPaymentsController` + `AdminRevenueController`.

### Step 2: Add 403 MockMvc tests

Mỗi controller cần test class `Admin{X}ControllerSecurityTest`:

```java
@WebMvcTest(AdminInstancesController.class)
class AdminInstancesControllerSecurityTest {
    @Test
    @WithMockUser(roles = "TENANT_USER")
    void getInstances_nonAdminRole_returns403() { ... }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getInstances_teacherRole_returns403() { ... }
}
```

### Step 3: Update api-contract.md docs

Document role requirement explicit trong 3 admin endpoint sections của `documents/01-business/kitehub/admin/api-contract.md`.

## Acceptance Criteria

- [ ] 3 admin v1 controllers có class-level `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` annotation
- [ ] Mỗi endpoint × ≥2 non-admin role test trả HTTP 403 (≥18 test cases tổng)
- [ ] api-contract.md document role requirement explicit cho 3 endpoint groups
- [ ] Tests run pass trong `mvn test` Wave 94c CI
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist

## Related

- **Audit origin:** [documents/04-quality/audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md](../audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** [GAP-619](GAP-619-wave-92-post-wave-audit-suite.md) (audit suite spawn)
- **Rules:**
  - `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1
  - `.claude/rules/pre-handoff-self-test-completeness.md` §2.4
  - `.claude/rules/contract-first-for-cross-layer.md` §3

## Log

- **2026-05-18** — Initial write-up. Filed từ Wave 92 post-wave audit suite (GAP-619) API Contract audit finding P0. State-check confirmed 3 controllers shipped Wave 92 Bucket D scaffold nhưng thiếu `@PreAuthorize` (grep `@PreAuthorize` trong `kitehub-admin/src/main/java/com/kitehub/admin/api/v1/` returns 0 matches at class level). Phase 1 BETA blocker — security audit gate ≥80 yêu cầu zero P0 finding.
