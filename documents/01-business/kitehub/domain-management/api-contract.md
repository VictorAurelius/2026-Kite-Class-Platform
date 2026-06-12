# Domain Management — API Contract

## POST /api/instances/{id}/domain
**Use case:** UC-DOM-01
**Auth:** Bearer token (Owner — PREMIUM/ENTERPRISE)
**Request:**
```json
{ "customDomain": "school.edu.vn" }
```
**Response 200:**
```json
{
  "customDomain": "school.edu.vn",
  "verifyToken": "kitehub-verify=550e8400-e29b-41d4-a716-446655440000",
  "domainStatus": "PENDING_VERIFY",
  "backupUrl": "https://thptabc.kitehub.me",
  "dnsInstructions": "Add TXT record: @ kitehub-verify=550e8400..."
}
```
**Errors:**
- 403: tier không đủ điều kiện
- 409: domain đã dùng bởi instance khác

---

## POST /api/instances/{id}/domain/verify
**Use case:** UC-DOM-02
**Auth:** Bearer token (Owner)
**Response 200:**
```json
{
  "customDomain": "school.edu.vn",
  "domainStatus": "VERIFIED",
  "verifiedAt": "2026-03-24T10:00:00Z",
  "backupUrl": "https://thptabc.kitehub.me"
}
```
hoặc nếu chưa xác minh được:
```json
{
  "customDomain": "school.edu.vn",
  "domainStatus": "PENDING_VERIFY",
  "message": "DNS TXT record not found yet"
}
```

---

## GET /api/instances/{id}/domain
**Use case:** UC-DOM-03
**Auth:** Bearer token
**Response 200:** DomainVerifyResponse
**Errors:** 404 instance not found

---

## DELETE /api/instances/{id}/domain
**Use case:** UC-DOM-04
**Auth:** Bearer token (Owner)
**Response 204:** No content
**Errors:** 404 instance not found
