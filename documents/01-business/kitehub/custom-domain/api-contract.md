# Custom Domain — API Contract

**Last verified:** 2026-06-01
**Created:** Wave tenant-domain-1 Bucket D (GAP-812)
**Rules:** [`rules.md`](rules.md)
**Use cases:** [`use-cases.md`](use-cases.md)
**Controller:** `kitehub-subscription/.../controller/DomainController.java`
**Base path:** `/api/instances/{id}/domain`

## Endpoints

### 1. POST /api/instances/{id}/domain — Initiate custom domain setup

**Use case:** UC-DOMAIN-001 (step 3) + UC-DOMAIN-003 (re-verify after FAILED).

**Path params:**
- `id` (UUID) — instance ID

**Request body** (`DomainSetupRequest`):
```json
{
  "customDomain": "lop.skyedu.vn"
}
```

Validation:
- `customDomain`: required, FQDN format (regex `^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\.[a-zA-Z]{2,}$`)

**Response 200** (`DomainVerifyResponse`):
```json
{
  "customDomain": "lop.skyedu.vn",
  "verifyToken": "kitehub-verify=abc12345-...",
  "verifyRecord": "Add TXT record to your DNS: @ kitehub-verify=abc12345-... (or _kitehub-verify.lop.skyedu.vn)",
  "status": "PENDING_VERIFY",
  "verifiedAt": null,
  "backupUrl": "https://skyedu.kitehub.me"
}
```

**Error responses:**
| Status | Reason | Code |
|--------|--------|------|
| 400 | Tier không hỗ trợ (BR-DOMAIN-011) | `INVALID_TIER` |
| 400 | Domain đã được instance khác chiếm (BR-DOMAIN-006) | `DOMAIN_IN_USE` |
| 400 | Domain format invalid | `INVALID_DOMAIN_FORMAT` |
| 404 | Instance không tồn tại | `INSTANCE_NOT_FOUND` |

---

### 2. POST /api/instances/{id}/domain/verify — Trigger DNS verification

**Use case:** UC-DOMAIN-001 (step 8) — Owner click "Verify ngay" sau khi thêm TXT.

**Path params:**
- `id` (UUID) — instance ID

**Request body:** (empty)

**Response 200** (`DomainVerifyResponse`):
```json
{
  "customDomain": "lop.skyedu.vn",
  "verifyToken": "kitehub-verify=abc12345-...",
  "verifyRecord": "Add TXT record to your DNS: @ kitehub-verify=...",
  "status": "VERIFIED",
  "verifiedAt": "2026-06-01T14:23:45",
  "backupUrl": "https://skyedu.kitehub.me"
}
```

**Status transitions (state machine completion — GAP-1024, [ADR-045](../../../02-architecture/adr/ADR-045-custom-domain-verification-state-machine-cert-seam.md)):**
- `PENDING_VERIFY` + TXT match (BR-DOMAIN-005) → `CERT_PROVISIONING` → request cert (`CertProvisioningService`); Phase 1 stub auto-issue đồng bộ → `VERIFIED` + `verifiedAt`. Phase 1.5+ real ACM/Cloudflare trả PENDING → giữ `CERT_PROVISIONING` (poll/webhook flip VERIFIED out-of-band).
- `PENDING_VERIFY` + TXT not match/not found → giữ `PENDING_VERIFY` (chờ tenant fix DNS hoặc `DomainVerificationTimeoutScheduler` flip FAILED sau 48h)
- **Idempotent (GAP-1024):** re-verify `VERIFIED` → no-op HTTP 200 (trả state hiện tại, KHÔNG throw 400); re-verify `CERT_PROVISIONING` → re-poll cert issuance (no DNS re-check)
- Cert provisioning FAILED → `status=FAILED` (re-initiate để regenerate token, BR-DOMAIN-004)

**Error responses:**
| Status | Reason | Code |
|--------|--------|------|
| 400 | Không có verify pending — status `NONE`/`FAILED` hoặc chưa initiate (re-initiate để regenerate token) | `NO_VERIFY_PENDING` |
| 404 | Instance không tồn tại | `INSTANCE_NOT_FOUND` |

---

### 3. DELETE /api/instances/{id}/domain — Remove custom domain

**Use case:** UC-DOMAIN-002.

**Path params:**
- `id` (UUID) — instance ID

**Response 204:** (no content)

**Side effects:**
- `customDomain=null`, `domainVerifyToken=null`, `domainVerifiedAt=null`, `domainStatus=NONE`
- Backup URL `{subdomain}.kitehub.me` vẫn hoạt động (không cần thêm gì)
- (Future v1.1) Background cleanup: revoke ACM cert / delete Cloudflare Custom Hostname

**Error responses:**
| Status | Reason | Code |
|--------|--------|------|
| 404 | Instance không tồn tại | `INSTANCE_NOT_FOUND` |

---

### 4. GET /api/instances/{id}/domain — Get domain status

**Use case:** UC-DOMAIN-001 (UI initial load + polling badge refresh).

**Path params:**
- `id` (UUID) — instance ID

**Response 200** (`DomainVerifyResponse`):
- Same shape như endpoint #1/#2 — phản ánh trạng thái hiện tại
- Nếu chưa setup: `customDomain=null`, `status=NONE`, `verifyToken=null`

**Error responses:**
| Status | Reason | Code |
|--------|--------|------|
| 404 | Instance không tồn tại | `INSTANCE_NOT_FOUND` |

---

## Status enum (`Instance.DomainStatus`)

| Value | Meaning |
|-------|---------|
| `NONE` | Chưa setup custom domain (default) |
| `PENDING_VERIFY` | Đã initiate, chờ tenant thêm TXT record + verify |
| `CERT_PROVISIONING` | TXT verified, SSL cert đang provision (v1.1+) |
| `VERIFIED` | Live — domain trỏ về tenant qua HTTPS |
| `FAILED` | Verify timeout 48h hoặc lookup fail liên tục — tenant cần re-initiate |

## Authentication / Authorization

- Endpoints yêu cầu Bearer JWT của Owner instance đó
- Per-resource authz: `@PreAuthorize` check ownership (BR-DOMAIN không cho admin platform set domain của tenant khác — sister concern với `pre-launch-owasp-rest-hardening-checklist.md` §2.1)

## Rate limiting

- POST initiate / verify: 30 requests/giờ/instance (verify endpoint có thể bị abuse bằng cách spam khi DNS chưa propagate)
- GET status: 60 requests/phút/instance (poll badge OK)

## Related

- **Controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/DomainController.java`
- **Service:** `DomainService.java` + `DnsTxtLookupService.java` (new — JNDI TXT lookup)
- **DTOs:** `DomainSetupRequest`, `DomainVerifyResponse`
- **Config:** `DomainVerificationConfig` (`kitehub.domain.verification.*`)
- **Tests:** `DomainServiceTest`, `DnsTxtLookupServiceTest`
- **Runbook:** [`../../../05-guides/operations/custom-domain-verify-runbook.md`](../../../05-guides/operations/custom-domain-verify-runbook.md)

## Log

- **2026-06-01:** Doc created — Wave tenant-domain-1 Bucket D (GAP-812). 4 endpoints documented + status enum extended with `CERT_PROVISIONING`.
