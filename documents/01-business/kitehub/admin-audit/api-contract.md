# Admin Audit — API Contract

**Domain:** Admin audit log (GAP-640 — Wave 97 Bucket C 3-layer foundation)
**Source-of-truth controller:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/`
**Last verified:** 2026-05-18 (Wave 97 Bucket C — GAP-640 admin-audit 3-layer docs META P1)

---

## Tổng quan

Domain `admin-audit` không expose endpoint public — đây là **internal logging infrastructure** consumed bởi mọi service KiteHub thông qua `AuditLogService.record()` (write path). Read path chỉ available cho `PLATFORM_ADMIN` qua compliance export + observability dashboard.

3 luồng API chính:

| Luồng | Hướng | Consumer | Note |
|---|---|---|---|
| **Write** | Internal Java method call | Mọi service (auth, beta-access, instance-provisioning, ...) | Synchronous, cùng transaction với business action |
| **Read (UI)** | `GET /api/v1/admin/audit-logs` | PLATFORM_ADMIN dashboard | Paginated, filtered |
| **Read (compliance export)** | `GET /api/v1/admin/audit-logs/export` | Compliance team manual / scheduled | CSV format, ≥1000 rows/page |

---

## Write path — Internal API (Java method call)

### `AuditLogService.record(AuditLogEntry entry)`

**Use case:** UC-ADMIN-AUDIT-001..003 — ghi nhận audit log sau khi hành động admin hoàn thành (success hoặc fail)
**Caller:** Mọi service hoàn thành hành động cần audit (vd `AuthService.adminLogin`, `BetaAccessService.approve`, `InstanceService.delete`)
**Business rule:** BR-ADMIN-AUDIT-001 (immutable), BR-ADMIN-AUDIT-002 (required fields V36), BR-ADMIN-AUDIT-003 (sensitive actions JSONB enrichment V54)

**Method signature:**
```java
public void record(AuditLogEntry entry);
```

**`AuditLogEntry` record (V36 + V54 enriched):**
```java
public record AuditLogEntry(
    UUID adminUserId,           // BR-ADMIN-AUDIT-002 required — null OK nếu auth fail
    String action,              // BR-ADMIN-AUDIT-002 required — enum AuditAction
    String targetEntityType,    // BR-ADMIN-AUDIT-002 required — vd "Instance", "BetaAccessRequest"
    UUID targetEntityId,        // BR-ADMIN-AUDIT-002 required
    String requestIp,           // BR-ADMIN-AUDIT-002 required — IPv4/IPv6 string
    String userAgent,           // BR-ADMIN-AUDIT-002 required
    Object payloadJson,         // BR-ADMIN-AUDIT-002 required — Jackson serializable
    boolean success,            // BR-ADMIN-AUDIT-002 required
    String errorMessage,        // BR-ADMIN-AUDIT-002 nullable khi success=true
    String requestId,           // V54 — correlation ID từ MDC traceId (Wave 92 enrichment)
    String targetResourceType,  // V54 — finer-grained scope (vd "Subscription", "TenantUser")
    UUID targetResourceId,      // V54
    JsonNode beforeState,       // V54 — BR-ADMIN-AUDIT-003 required cho sensitive actions
    JsonNode afterState         // V54 — BR-ADMIN-AUDIT-003 required cho sensitive actions
) { }
```

**Behavior:**
1. Validate required fields (throw `IllegalArgumentException` if missing baseline)
2. Persist row vào `admin_audit_log` table cùng transaction với caller
3. Per `audit-service-isolation.md` — `@Transactional(propagation = REQUIRES_NEW)` để audit failure KHÔNG block caller (audit best-effort, business action không phụ thuộc audit success)
4. Throw exception nếu validation fail (caller decide handle/log/ignore)

**Error semantics:**
- Validation fail (missing required field) → `IllegalArgumentException` → caller wrap log warn + continue
- DB write fail (constraint violation, connection drop) → audit row not persisted → log error + emit metric `audit.write.failure` → caller continues (business action unaffected per REQUIRES_NEW isolation)

**Anti-patterns:**
- ❌ Caller bọc `record()` trong main `@Transactional` → audit failure rollback business action (vi phạm BR-ADMIN-AUDIT-001 isolation principle)
- ❌ Async write (fire-and-forget) → race condition mất audit row khi process crash (vi phạm immutability guarantee)
- ❌ Update existing row → vi phạm BR-ADMIN-AUDIT-001 immutability

---

## Read path — UI endpoint

### GET /api/v1/admin/audit-logs

**Use case:** UC-ADMIN-AUDIT-004 — Admin truy vấn audit log qua dashboard
**Auth:** `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` per `pre-launch-owasp-rest-hardening-checklist.md` §2.1
**Rate limit:** 30 req/min per admin user (gateway-enforced)

**Query parameters:**
| Param | Type | Required | Default | Mô tả |
|---|---|---|---|---|
| `adminUserId` | UUID | No | (all) | Filter by admin user |
| `action` | String | No | (all) | Filter by action type (enum value) |
| `targetEntityType` | String | No | (all) | Filter by entity type |
| `targetEntityId` | UUID | No | (all) | Filter by specific entity |
| `startDate` | ISO date | No | (30 days ago) | Range filter — inclusive |
| `endDate` | ISO date | No | (today) | Range filter — inclusive |
| `success` | boolean | No | (both) | Filter by success status |
| `page` | int | No | `0` | 0-indexed |
| `size` | int | No | `50` | Max 200 per request |

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "adminUserId": "...",
      "adminUserEmail": "admin@kitehub.me",
      "action": "BETA_REQUEST_APPROVE",
      "targetEntityType": "BetaAccessRequest",
      "targetEntityId": "...",
      "requestIp": "203.0.113.7",
      "userAgent": "Mozilla/5.0 ...",
      "payloadJson": {...},
      "success": true,
      "errorMessage": null,
      "requestId": "trace-abc-123",
      "createdAt": "2026-05-18T14:30:00Z"
    }
  ],
  "totalElements": 1234,
  "totalPages": 25,
  "page": 0,
  "size": 50
}
```

**Error codes:**
| HTTP | Code | Khi nào |
|---|---|---|
| `400` | `INVALID_FILTER` | Filter param sai format (vd UUID invalid, date không parse được) |
| `403` | `FORBIDDEN` | User không có ROLE_PLATFORM_ADMIN (per @PreAuthorize) |
| `429` | `RATE_LIMITED` | Vượt 30 req/min |

---

## Read path — Compliance export endpoint

### GET /api/v1/admin/audit-logs/export

**Use case:** UC-ADMIN-AUDIT-005 — Compliance team xuất audit log cho regulator audit hoặc internal review
**Auth:** `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` + `hasAuthority('AUDIT_EXPORT')` (fine-grained — chỉ compliance officer có authority này)
**Rate limit:** 5 req/hour per admin user (export là heavy operation)

**Query parameters:**
| Param | Type | Required | Mô tả |
|---|---|---|---|
| `format` | enum | Yes | `CSV` (only supported v1) |
| `startDate` | ISO date | Yes | Range start (inclusive) |
| `endDate` | ISO date | Yes | Range end (inclusive) — max range 1 year |
| `adminUserId` | UUID | No | Filter optional |
| `action` | String | No | Filter optional |

**Response (200 OK):**
- `Content-Type: text/csv`
- `Content-Disposition: attachment; filename="admin-audit-{startDate}-to-{endDate}.csv"`
- Body: CSV với header row + N data rows (pagination internal — stream up to `kitehub.admin-audit.export-page-size=1000` rows per chunk)

**CSV columns:**
```
id,admin_user_id,admin_user_email,action,target_entity_type,target_entity_id,request_ip,user_agent,payload_json,success,error_message,request_id,target_resource_type,target_resource_id,before_state,after_state,created_at
```

**Error codes:**
| HTTP | Code | Khi nào |
|---|---|---|
| `400` | `INVALID_DATE_RANGE` | endDate < startDate hoặc range > 1 năm |
| `403` | `FORBIDDEN_EXPORT` | Missing `AUDIT_EXPORT` authority |
| `429` | `EXPORT_RATE_LIMITED` | Vượt 5 export/hour |

---

## Sensitive actions list (BR-ADMIN-AUDIT-003)

Per config key `kitehub.admin-audit.sensitive-actions` default value:

| Action enum | Description | `beforeState`/`afterState` mandatory |
|---|---|---|
| `LOGIN` | Admin login attempt | No (no state change) |
| `IMPERSONATE` | Admin impersonate tenant user | Yes — capture target user snapshot |
| `DATA_EXPORT` | Admin export tenant data (compliance / debug) | Yes — capture export scope JSON |
| `INSTANCE_DELETE` | Admin soft-delete tenant instance | Yes — capture instance state before delete |
| `TENANT_SUSPEND` | Admin suspend tenant access | Yes — capture suspension reason + scope |

Khi action ∈ sensitive list nhưng caller không cung cấp `beforeState` + `afterState` → throw `IllegalArgumentException` per BR-ADMIN-AUDIT-003. Sensitive actions còn lại (vd `BETA_REQUEST_APPROVE`) → JSONB optional nhưng recommended cho forensic.

---

## Impersonation endpoints (GAP-1333)

**Source-of-truth controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/impersonation/ImpersonationController.java` (`@RequestMapping("/api/v1/admin/impersonate")`)
**Auth:** mọi endpoint `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`. Identity admin lấy từ gateway header `X-User-Id` (→ principal UUID) + `X-User-Roles` (→ `ROLE_PLATFORM_ADMIN`). Non-admin → 403.
**Tests:** package `com.kitehub.subscription.impersonation` (controller + service).

Admin "View as tenant" (GAP-040): mint **scoped read-only JWT** TTL 30 giây mang claim `tenant_id` + `impersonated_by`. Mỗi start/end ghi 1 row audit (action-type `IMPERSONATE` ở bảng "Sensitive actions list" phía trên) — audit row insert **cùng transaction** với token mint (persist fail → không trả token).

### POST /api/v1/admin/impersonate/{tenantSlug}

**Use case:** Admin bắt đầu phiên xem-như-tenant.
**Path param:** `tenantSlug` — slug tenant cần xem.
**Request body:** không. IP + User-Agent lấy từ `X-Forwarded-For` (first hop) / `User-Agent` để ghi audit.

**Response 200 OK** — `ImpersonationStartResponse`:
```json
{
  "sessionId": 1024,
  "impersonationToken": "<HS512 JWT, 30s TTL>",
  "tenantId": "uuid",
  "tenantSlug": "truong-abc",
  "expiresAt": "2026-06-14T08:30:30+07:00"
}
```

| Field | Type | Mô tả |
|---|---|---|
| `sessionId` | number | id row audit-log; FE dùng để gọi `/end`. |
| `impersonationToken` | string | JWT scoped read-only, TTL 30s, claim `tenant_id` + `impersonated_by`. |
| `tenantId` | UUID | Tenant đang xem. |
| `tenantSlug` | string | Echo slug. |
| `expiresAt` | ISO-8601 (OffsetDateTime) | Mốc tuyệt đối token + session hết hạn. |

**Error:** 403 (không phải PLATFORM_ADMIN); 400 (tenantSlug không hợp lệ — `IllegalArgumentException` → ProblemDetail RFC 7807).

### POST /api/v1/admin/impersonate/end

**Use case:** Admin chủ động thoát phiên.
**Request body:** không (admin xác định qua principal).

**Response 200 OK** — `ImpersonationEndResponse`:
```json
{ "sessionId": 1024, "endedAt": "2026-06-14T08:25:10+07:00", "endedReason": "MANUAL_EXIT" }
```

| Field | Type | Mô tả |
|---|---|---|
| `sessionId` | number | Row audit được đóng. |
| `endedAt` | ISO-8601 | Thời điểm đóng. |
| `endedReason` | enum | `MANUAL_EXIT` \| `AUTO_TIMEOUT` \| `NEVER` (`ImpersonationAuditEntry.EndedReason`). |

**Error:** 404 nếu không có phiên active của caller; 403 nếu không phải PLATFORM_ADMIN.

### GET /api/v1/admin/impersonate/audit-log

**Use case:** Panel audit admin xem lịch sử impersonation (newest-first).
**Query params:** `page` (default `0`), `size` (default `20`, clamp `[1..100]`).

**Response 200 OK** — `Page<ImpersonationAuditEntryDto>` (Spring Page wrapper). Mỗi entry:
```json
{
  "id": 1024,
  "adminUserId": "uuid",
  "tenantId": "uuid",
  "tenantSlug": "truong-abc",
  "startedAt": "2026-06-14T08:25:00+07:00",
  "endedAt": "2026-06-14T08:25:10+07:00",
  "endedReason": "MANUAL_EXIT",
  "requestIp": "203.0.113.7",
  "userAgent": "Mozilla/5.0 ..."
}
```

**Error:** 403 nếu không phải PLATFORM_ADMIN.

**Cross-ref:** mỗi start/end tạo 1 `IMPERSONATE` audit row (bảng "Sensitive actions list" phía trên — `beforeState`/`afterState` capture snapshot target user).

---

## Cross-layer dependencies

Per `contract-first-for-cross-layer.md` §3 — contract này là source-of-truth cho:

| Layer | Consumer | Reference |
|---|---|---|
| BE Java | `AuditLogService.record()` impl | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/AuditLogService.java` |
| BE Java | `AdminAuditLog` JPA entity (V36 + V54 fields) | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/admin/audit/AdminAuditLog.java` |
| DB | V36 baseline migration | `kitehub/kitehub-admin/src/main/resources/db/migration/V36__create_admin_audit_log.sql` |
| DB | V54 enrichment migration | `kitehub/kitehub-subscription/src/main/resources/db/migration/V54__admin_audit_log_enrichment.sql` |
| Test | Testcontainers IT (V54 JSONB) | `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/audit/AdminAuditLogJsonbPostgresIT.java` (paired GAP-642 Wave 97 Bucket D) |
| Doc | Methodology + compliance | `documents/01-business/kitehub/admin-audit/{rules,use-cases}.md` (sister docs same wave) |

---

## Compliance evidence (PDPL Art 11 + ISO27001 A.12.4)

API contract này là baseline cho:
- **PDPL 2023 Art 11** — Tổ chức phải lưu giữ evidence về việc thực hiện nghĩa vụ bảo vệ dữ liệu cá nhân. Audit log với 7-year retention + immutable mandate satisfies requirement.
- **ISO27001 A.12.4** — Audit logging và monitoring cho security incidents. JSONB `before_state` + `after_state` cho sensitive actions cung cấp forensic depth.

Compliance reader có thể verify implementation thông qua:
1. Query export endpoint với date range cho compliance audit period
2. Cross-reference `success=false` rows với incident tickets (was each fail tracked?)
3. Cross-reference `IMPERSONATE` rows với approval workflow logs (was each impersonation authorized?)

---

## Related

- [rules.md](./rules.md) — BR-ADMIN-AUDIT-001/002/003 business rules
- [use-cases.md](./use-cases.md) — UC-ADMIN-AUDIT-001..005 actor flows
- [`.claude/rules/audit-service-isolation.md`](../../../../.claude/rules/audit-service-isolation.md) — `@Transactional(REQUIRES_NEW)` mandate
- [`.claude/rules/postgres-specific-type-testcontainers.md`](../../../../.claude/rules/postgres-specific-type-testcontainers.md) — V54 JSONB Testcontainers IT mandate
- [`.claude/rules/pre-launch-owasp-rest-hardening-checklist.md`](../../../../.claude/rules/pre-launch-owasp-rest-hardening-checklist.md) §2.1 — @PreAuthorize mandate cho admin endpoints
- [GAP-640](../../../04-quality/gaps/phase-1-beta/GAP-640-admin-audit-domain-3-layer-docs-missing.md) — parent gap META P1
- [GAP-642](../../../04-quality/gaps/phase-1-beta/GAP-642-v54-jsonb-testcontainers-it-missing.md) — V54 JSONB IT (paired Wave 97 Bucket D)
