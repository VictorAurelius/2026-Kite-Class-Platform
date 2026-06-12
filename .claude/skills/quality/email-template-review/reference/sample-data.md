# Email Template Sample Data

Canonical sample data per email template. Use these JSON blocks to feed Thymeleaf preview (MailHog / Mailtrap / `EmailPreviewController`).

Every template MUST render successfully with both **branded** (full tenant) and **unbranded** (system defaults) fixtures. Missing variables must fail-safe via `?:` defaults.

---

## Common fixtures

### Branded tenant (Trường Mầm Non Hoa Mai)

```json
{
  "branding": {
    "displayName": "Mầm Non Hoa Mai",
    "primaryColor": "#FF6B9D",
    "secondaryColor": "#FFC6D9",
    "accentColor": "#F9A826",
    "logoUrl": "https://cdn.kitehub.me/tenants/hoa-mai/logo.png",
    "contactEmail": "lienhe@hoamai.kitehub.me"
  },
  "tenantContext": {
    "id": "tenant-hoa-mai-01",
    "displayName": "Trường Mầm Non Hoa Mai",
    "locale": "vi-VN"
  },
  "organizationName": "Trường Mầm Non Hoa Mai",
  "legalEntity": {
    "companyName": "Công ty TNHH Giáo dục Hoa Mai",
    "registrationNumber": "0314567890",
    "address": "123 Nguyễn Văn Cừ, Quận 1, TP.HCM, Việt Nam"
  }
}
```

### Unbranded (system defaults — KiteClass parent)

```json
{
  "branding": null,
  "tenantContext": {
    "id": "sys",
    "displayName": "KiteClass",
    "locale": "vi-VN"
  },
  "legalEntity": {
    "companyName": "Công ty TNHH KiteClass",
    "registrationNumber": "0987654321",
    "address": "Tòa nhà Bitexco, 2 Hải Triều, Quận 1, TP.HCM, Việt Nam"
  }
}
```

---

## Per-template fixtures

### `welcome.html`

```json
{
  "organizationName": "Trường Mầm Non Hoa Mai",
  "trialDays": 14,
  "expiryDate": "04/05/2026",
  "loginUrl": "https://hoa-mai.kitehub.me/login",
  "unsubscribeUrl": "https://hoa-mai.kitehub.me/unsubscribe?token=abc123"
}
```

### `email-verification.html`

```json
{
  "verificationUrl": "https://hoa-mai.kitehub.me/verify?token=xyz789",
  "verificationCode": "482917",
  "expiryMinutes": 15,
  "userEmail": "admin@hoamai.vn"
}
```

### `subscription-created.html`

```json
{
  "planName": "Professional",
  "billingCycle": "Annual",
  "amount": 12000000,
  "currency": "VND",
  "nextBillingDate": "20/04/2027",
  "invoiceUrl": "https://hoa-mai.kitehub.me/invoices/INV-2026-0042.pdf",
  "unsubscribeUrl": "https://hoa-mai.kitehub.me/unsubscribe?token=..."
}
```

### `subscription-expired.html`

```json
{
  "planName": "Professional",
  "expiredOn": "20/04/2026",
  "renewUrl": "https://hoa-mai.kitehub.me/billing/renew",
  "gracePeriodDays": 7,
  "dataRetentionUntil": "27/04/2026"
}
```

### `subscription-renewal-reminder.html`

```json
{
  "planName": "Professional",
  "renewalDate": "20/04/2026",
  "amount": 12000000,
  "currency": "VND",
  "daysUntilRenewal": 7,
  "manageUrl": "https://hoa-mai.kitehub.me/billing"
}
```

### `subscription-suspended.html`

```json
{
  "planName": "Professional",
  "suspensionReason": "Thanh toán thất bại",
  "resolveUrl": "https://hoa-mai.kitehub.me/billing/update-payment",
  "suspendedOn": "20/04/2026",
  "supportEmail": "lienhe@hoamai.kitehub.me"
}
```

### `trial-midpoint.html`

```json
{
  "daysRemaining": 7,
  "trialEndDate": "27/04/2026",
  "upgradeUrl": "https://hoa-mai.kitehub.me/billing/upgrade",
  "usageStats": {
    "studentsCreated": 42,
    "classesCreated": 6,
    "teachersInvited": 8
  }
}
```

### `trial-expiration-warning.html`

```json
{
  "daysRemaining": 3,
  "trialEndDate": "23/04/2026",
  "upgradeUrl": "https://hoa-mai.kitehub.me/billing/upgrade",
  "dataExportUrl": "https://hoa-mai.kitehub.me/settings/export"
}
```

### `trial-expired.html`

```json
{
  "trialEndedOn": "20/04/2026",
  "upgradeUrl": "https://hoa-mai.kitehub.me/billing/upgrade",
  "dataExportUrl": "https://hoa-mai.kitehub.me/settings/export",
  "dataRetentionDays": 30,
  "deleteOn": "20/05/2026"
}
```

### `data-retention-warning.html`

```json
{
  "tenantDisplayName": "Trường Mầm Non Hoa Mai",
  "inactiveSince": "20/01/2026",
  "deletionDate": "20/05/2026",
  "daysUntilDeletion": 30,
  "dataExportUrl": "https://hoa-mai.kitehub.me/settings/export",
  "reactivateUrl": "https://hoa-mai.kitehub.me/reactivate"
}
```

### `data-retention-final-warning.html`

```json
{
  "tenantDisplayName": "Trường Mầm Non Hoa Mai",
  "deletionDate": "20/05/2026",
  "daysUntilDeletion": 7,
  "dataExportUrl": "https://hoa-mai.kitehub.me/settings/export"
}
```

### `data-deleted.html`

```json
{
  "tenantDisplayName": "Trường Mầm Non Hoa Mai",
  "deletedOn": "20/05/2026",
  "archiveReference": "ARCHIVE-HOAMAI-2026-05-20"
}
```

### `onboarding-tips.html`

```json
{
  "userName": "Nguyễn Thị Lan",
  "dayOfOnboarding": 3,
  "nextSteps": [
    {"title": "Thêm học sinh", "url": "https://hoa-mai.kitehub.me/students/new"},
    {"title": "Tạo lớp học", "url": "https://hoa-mai.kitehub.me/classes/new"}
  ],
  "helpCenterUrl": "https://kitehub.me/help"
}
```

### `password-reset.html` (kiteclass-gateway)

```json
{
  "userName": "Nguyễn Thị Lan",
  "resetUrl": "https://hoa-mai.kitehub.me/auth/reset?token=reset-abc-123",
  "expiryMinutes": 30,
  "requestIpAddress": "113.161.45.22",
  "requestedAt": "2026-04-20 14:32 ICT"
}
```

### `account-locked.html` (kiteclass-gateway)

```json
{
  "userName": "Nguyễn Thị Lan",
  "lockReason": "Quá nhiều lần đăng nhập sai",
  "lockDurationMinutes": 30,
  "unlockAt": "15:02 ICT, 20/04/2026",
  "supportEmail": "lienhe@hoamai.kitehub.me"
}
```

---

## Edge cases to test

For each template above, also render with these edge-case variants:

| Variant | What changes | Why |
|---------|--------------|-----|
| **VN long name** | `organizationName: "Trung Tâm Ngoại Ngữ Phát Triển Kỹ Năng Toàn Diện Sao Sáng Alpha"` (60+ chars) | Verify no overflow in header/subject |
| **EN locale** | `tenantContext.locale: "en-US"` | Verify `_en.html` loaded or graceful fallback |
| **No logo** | `branding.logoUrl: null` | Verify header still readable, no broken `<img>` |
| **Null branding** | `branding: null` entirely | System defaults kick in |
| **Missing optional** | Omit `usageStats`, `nextSteps` | `th:if` guards prevent broken layout |
| **XSS attempt** | `organizationName: "<script>alert('x')</script>"` | Verify escaped to `&lt;script&gt;...` in output |
| **Diacritics** | `userName: "Đỗ Thị Ánh Nguyệt — Trưởng Phòng"` | UTF-8 preserved through SMTP |

---

## Preview tooling

**Option A: MailHog (recommended for dev)**
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
# Configure spring.mail.host=localhost spring.mail.port=1025 in application-dev.yml
# Visit http://localhost:8025 to inspect sent messages
```

**Option B: Mailtrap (shared team inbox)**
Configure `application-dev.yml` with Mailtrap SMTP credentials. Each dev gets isolated inbox.

**Option C: `EmailPreviewController` (future GAP-173 follow-up)**
`GET /dev/email/preview/{template}?tenant=hoa-mai` returns rendered HTML — no SMTP round-trip. File as follow-up gap.
