# Security Hardening — API Contract

Sub-PR 4.2 ships **SPI interfaces**, not HTTP endpoints. Consumers call these beans directly.

---

## SPI Beans

All live in `com.kiteclass.core.common.security`; implementations in `com.kiteclass.core.common.security.impl`.

### `SvgSanitizer`

```java
String sanitize(String rawSvg);
```

- `rawSvg` may be null/blank → returns `""`.
- Never returns null. May return `""` if input was entirely unsafe.
- Idempotent: `sanitize(sanitize(x))` equals `sanitize(x)` for all `x`.

### `UrlAllowlistValidator`

```java
boolean isAllowed(String url, String tenantId);
```

- `url` null/blank → `false`.
- `tenantId` null → only `public-api-patterns` path applies.

### `CsrfTokenProvider`

```java
String issue();
boolean verify(String token, String cookie);
```

- `issue()` → opaque 3-part token `nonce.issuedAt.signature` (all base64-url, no padding).
- `verify(null, ·)` / `verify(·, null)` → `false`.
- `verify` checks header↔cookie equality, signature, and TTL.

---

## HTTP Wiring (downstream Sub-PRs)

| Route | Consumer | SPI used |
|-------|----------|----------|
| `POST /api/v1/branding/logo` (wave 3) | `LogoUploadController` | `SvgSanitizer` |
| `POST /api/v1/webhooks/*` (wave 3.4) | `WebhookDispatcher` | `UrlAllowlistValidator` |
| Any mutating endpoint on `/api/v1/**` | Gateway `CsrfTokenFilter` (wave 4.x) | `CsrfTokenProvider` |

These controllers/filters are NOT introduced by Sub-PR 4.2 — only the SPI implementations.

---

## Error Codes (when consumers reject)

| Code | HTTP | Origin | When |
|------|------|--------|------|
| `SEC-SVG-001` | `422` | Upload endpoint | `sanitize()` returned empty for non-empty input |
| `SEC-URL-001` | `403` | Webhook dispatcher | `isAllowed()` returned `false` |
| `SEC-CSRF-001` | `403` | CSRF filter | `verify()` returned `false` |
| `SEC-CSRF-002` | `500` (startup) | Platform | Insecure CSRF secret at boot |

---

## Log

- 2026-04-15 — Contract authored alongside Sub-PR 4.2 SPI impls.
