---
audience: dev
---

# GAP-764 — Beta request JSON response HTML entity escape Vietnamese diacritic

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng A2 POST endpoint probe)
**Affects:** `POST /api/v1/auth/request-beta-access` response — Vietnamese data integrity
**Phase:** phase-1-beta

## Problem

POST `/api/v1/auth/request-beta-access` với body `{"orgName":"Trung tâm Anh ngữ Sky Education", ...}` trả về JSON response:

```json
{
  "id": 12,
  "name": "Trần Thị Hồng",
  "orgName": "Trung t&acirc;m Anh ngữ Sky Education",   // ← &acirc; thay vì raw â
  "persona": "P2_CENTER_OWNER",
  "status": "PENDING"
}
```

`â` (U+00E2) bị escape thành HTML entity `&acirc;` trong JSON response body. Other Vietnamese diacritics (`ữ`, `ữ`, `ồ`, `Trần`, `Hồng`, `ngữ`) KHÔNG bị affect — chỉ subset (likely ASCII-extended chars trong HTML entity table cơ bản).

Repro:
```bash
curl -X POST http://localhost:8081/api/v1/auth/request-beta-access \
  -H "Content-Type: application/json" \
  -d '{"name":"Trần Thị Hồng","email":"t@test.vn","phone":"0901234567","orgName":"Trung tâm Anh ngữ Sky Education","persona":"P2_CENTER_OWNER","consentGiven":true,"honeypot":""}'
```

## Root Cause (suspected)

HTML sanitization filter (OWASP HTML Sanitizer / Jackson HtmlEscapingSerializer / Spring HtmlEscapeUtils) áp dụng MIS-applied to JSON response. JSON UTF-8 should NOT escape via HTML entity — should output raw `â` OR `â` unicode escape if needed.

Check:
- `kitehub-subscription` controller serialization config (`ObjectMapper` bean)
- Any `@JsonSerialize(using = HtmlEscapeSerializer.class)` annotation on BetaRequestResponse DTO
- Global Jackson config trong `application.yml` (`jackson.escape-non-ascii: true`?)

## Question for verify

Cần check Postgres row `beta_access_request id=12`:
- Field `org_name`: là `Trung tâm Anh ngữ Sky Education` (raw) hay `Trung t&acirc;m...` (corrupted at write)?

Nếu raw trong DB → bug chỉ ở response serializer (lower severity).
Nếu corrupted trong DB → P0 (write path bị sanitize sai, ảnh hưởng future read + email rendering).

## Proposed Fix

1. Verify DB state (query `beta_access_request` table)
2. Locate sanitization config trong subscription module
3. Fix: configure Jackson to use raw UTF-8 (default) hoặc unicode escape, NOT HTML entity
4. **E2E spec paired** per `e2e-rst-test-layer-boundary.md` §3 — `kitehub-frontend/e2e/beta-request-utf8-roundtrip.spec.ts` assert POST → response body contains raw `â` not `&acirc;`

## Acceptance Criteria

- [ ] DB state verified (raw OR corrupted)
- [ ] Sanitization root cause identified
- [ ] Fix shipped: response trả raw UTF-8 cho mọi VN diacritic
- [ ] E2E spec regression-guard paired same PR

## Related

- Wave 106 RST A2 walkthrough
- Rule: `vn-localization-audit-checklist.md` §1 (VND format + VN sample data trong tenant-facing artifact)
- Rule: `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate
