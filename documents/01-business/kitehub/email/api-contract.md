---
audience: dev
domain: email
layer: api-contract
version: 1.1.0
last-updated: 2026-05-24
related-gaps: [GAP-657, GAP-659, GAP-662]
---

# API Contract — Email Layer

**Scope:** Endpoint surface của `kitehub-email` service. Wave 98 Bucket B1 — 5 critical email types + header policy.

**Base URL:** `http://kitehub-email:8086` (internal); gateway proxy: `/api/platform/emails/**`.

> **Wave beta-readiness-2 Bucket D (GAP-662) — Option B sync:** Doc URL path corrected từ `/api/email/send` (incorrect) → `/api/platform/emails/send` (matches actual `EmailController` `@RequestMapping`). Legacy `/api/platform/emails/*` namespace là Wave 35-ish controller naming experiment; v1 namespace migration (rename → `/api/v1/email/*` per admin Wave 97 pattern) deferred Wave 109+ via GAP-733 follow-up.

---

## POST /api/platform/emails/send

Send transactional email. Provider dispatch theo `email.provider` config (per BR-EMAIL-006).

**Request body:**

```json
{
  "to": "user@example.com",
  "subject": "Chào mừng đến với KiteHub",
  "templateName": "welcome",
  "variables": {
    "recipientName": "Nguyễn Thị Mai",
    "loginUrl": "https://kitehub.me/login"
  },
  "instanceId": 12345,
  "tenantId": "trung-tam-sky"
}
```

**Fields:**

| Field | Type | Required | Notes |
|---|---|---|---|
| `to` | string | ✅ | Email address; validated `@Email` |
| `subject` | string | ✅ | ≤500 chars |
| `templateName` | string | ✅ | One of `EmailType.templateName` (e.g. `welcome`, `beta-invite`, `email-verification`, `password-reset`, `invite-staff`) |
| `variables` | map<string,object> | optional | Thymeleaf template variables — varies per template |
| `htmlBody` | string | optional | Skip template — send raw HTML (rare) |
| `instanceId` | long | optional | Tenant instance ID — triggers branding fetch |
| `tenantId` | string | optional | Header `X-Tenant-Id` value forwarded to branding |

**Response 200:**

```json
{
  "messageId": "010101870e8a04c0-...-000000",
  "status": "SENT",
  "sentAt": "2026-05-18T10:30:00",
  "errorMessage": null
}
```

**Response 200 (mock):**

```json
{
  "messageId": "mock-a3f5d8...-uuid",
  "status": "MOCK",
  "sentAt": "2026-05-18T10:30:00"
}
```

**Response 200 (failed — note status code 200 + status=FAILED, NOT 500):**

```json
{
  "messageId": null,
  "status": "FAILED",
  "sentAt": "2026-05-18T10:30:00",
  "errorMessage": "SES rate limit exceeded"
}
```

**Error 400:** validation fail (missing `to`, invalid email, template not found).

---

## Headers wired automatically by SESEmailService + ResendEmailService

Per BR-EMAIL-002 + BR-EMAIL-003, every outbound email carries:

| Header | Value | Applies to |
|---|---|---|
| `From` | `KiteHub <no-reply@kitehub.me>` | All |
| `Reply-To` | `support@kitehub.me` | All |
| `List-Unsubscribe` | `<mailto:unsubscribe@kitehub.me>, <https://kitehub.me/unsubscribe?token={token}>` | All EXCEPT `password-reset` |
| `List-Unsubscribe-Post` | `List-Unsubscribe=One-Click` | All EXCEPT `password-reset` |
| `Content-Type` | `multipart/alternative; boundary=...` | All 5 critical templates (HTML + text parts) |

**No client-side override** — these headers wired by `SESEmailService.sendEmail(..., textBody)` overload + `ResendEmailService.sendEmail(...)`.

---

## 5 critical template request schemas (variables)

### welcome

```json
{
  "to": "user@example.com",
  "subject": "Chào mừng đến với KiteHub",
  "templateName": "welcome",
  "variables": {
    "recipientName": "Nguyễn Thị Mai",
    "loginUrl": "https://kitehub.me/login",
    "docsUrl": "https://kitehub.me/help",
    "unsubscribeUrl": "https://kitehub.me/unsubscribe?token=..."
  }
}
```

### beta-invite

```json
{
  "to": "owner@truongmaixinh.vn",
  "subject": "Lời mời tham gia chương trình Beta KiteHub",
  "templateName": "beta-invite",
  "variables": {
    "orgName": "Trung tâm Anh ngữ Sky Education",
    "inviteUrl": "https://kitehub.me/beta/accept?token=...",
    "verificationCode": "382041",
    "expiresAt": "Thứ Tư, 20/05/2026 18:00 (giờ Việt Nam)",
    "unsubscribeUrl": "https://kitehub.me/unsubscribe?token=..."
  }
}
```

### email-verification

```json
{
  "to": "user@example.com",
  "subject": "Mã xác minh email — KiteHub",
  "templateName": "email-verification",
  "variables": {
    "recipientName": "Trần Văn An",
    "verificationCode": "184205",
    "verifyUrl": "https://kitehub.me/verify?token=...",
    "expiresInMinutes": 15
  }
}
```

### password-reset

```json
{
  "to": "user@example.com",
  "subject": "Đặt lại mật khẩu — KiteHub",
  "templateName": "password-reset",
  "variables": {
    "recipientName": "Phạm Thị Hồng",
    "resetUrl": "https://kitehub.me/password-reset?token=...",
    "expiresInMinutes": 30
  }
}
```

### invite-staff

```json
{
  "to": "manager@truongmaixinh.vn",
  "subject": "Lời mời tham gia trung tâm Trung tâm Sky Education",
  "templateName": "invite-staff",
  "variables": {
    "recipientName": "Trần Văn Tâm",
    "ownerName": "Nguyễn Thị Hằng",
    "tenantName": "Trung tâm Sky Education",
    "role": "MANAGER",
    "inviteUrl": "https://kitehub.me/staff/accept?token=...",
    "expiresAt": "7 ngày kể từ thời điểm gửi"
  }
}
```

---

## Tone resolution (Wave 98 simplification)

Per BR-EMAIL-004, callers MAY pass a `tone` variable (one of `FORMAL_AUTHORITY` / `SEMI_FORMAL_PEER` / `INFORMAL_FRIEND` / `FORMAL_SAFE_DEFAULT`) or omit (defaults to `FORMAL_SAFE_DEFAULT`).

**Wave 98:** ALL templates render `FORMAL_SAFE_DEFAULT` regardless of tone variable. Per-tone variant templates defer Wave 99+ per GAP-659 §Step 2.

**Server-side resolution (if `tone` not provided):** `EmailTemplateRenderer` resolves from `variables.recipientRole` via `Tone.fromRole()`.

---

## Error codes

| Code | Status | Cause |
|---|---|---|
| `EMAIL_400_VALIDATION` | 400 | Missing `to`/`subject`/`templateName`; invalid email format |
| `EMAIL_400_UNKNOWN_TEMPLATE` | 400 | `templateName` không match `EmailType` enum |
| `EMAIL_503_PROVIDER_DOWN` | 503 | SES/Resend HTTP error; retry via outbox |

(Note: `EmailController` currently returns 200 + `status=FAILED` envelope for provider failures — see [`EmailController.java`](../../../kitehub/kitehub-email/src/main/java/com/kitehub/email/controller/EmailController.java) for actual mapping; this contract documents target shape, gap GAP-572 covers refactor.)

---

## Related

- **rules.md** (sister) — business rules behind contract
- **GAP-657** — header policy + plain-text fallback (this contract's headers section)
- **GAP-659** — tone register (this contract's Tone resolution section)
- **EmailType.java** — enum canonical for `templateName` field
