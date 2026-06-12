# GAP-038: Developer API Docs + SDK / Client Library

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Developer Experience / Documentation
**Detected:** 2026-04-14 (simulation: Developer × Integration × C8)

## Problem

Developer (internal hoặc tenant's dev) **không có** resources để integrate với AI Branding API:

- ❌ Không public OpenAPI/Swagger docs
- ❌ Không SDK / client library (TS, Java, Python)
- ❌ Không code examples
- ❌ Không postman collection
- ❌ Không integration testing sandbox

Impact: tenant dev team muốn build custom integrations (e.g., sync branding với internal wiki) → phải reverse-engineer.

## Proposed Fix

### 1. OpenAPI Spec (SpringDoc)

Auto-generate from kitehub-branding controllers:
- Endpoint: `/v3/api-docs` (JSON)
- Swagger UI: `/swagger-ui.html`
- Include all branding endpoints với request/response schemas
- Authentication documented

### 2. SDK Libraries

```
@kiteclass/branding-sdk-typescript
@kiteclass/branding-sdk-java
@kiteclass/branding-sdk-python
```

Generated từ OpenAPI spec (openapi-generator).

```typescript
import { BrandingClient } from '@kiteclass/branding-sdk-typescript';

const client = new BrandingClient({
  baseUrl: 'https://api.kitehub.me',
  apiKey: process.env.KITECLASS_API_KEY,
  tenantId: 'abc-123'
});

// Fetch branding package
const pkg = await client.getBrandingPackage();
console.log(pkg.theme.primaryColor);

// Listen for updates
client.onBrandingUpdate(pkg => {
  updateCssVars(pkg.theme);
});
```

### 3. Developer Portal

```
/developers
├── Getting Started
├── Authentication
├── API Reference (auto-gen from OpenAPI)
├── SDK Installation
├── Code Examples
│   ├── Fetch branding (TS)
│   ├── Apply theme (React)
│   ├── Webhook handler (Node.js)
│   └── Sync to external system
├── Webhooks Guide
├── Rate Limits
├── Changelog
└── Support
```

### 4. Postman Collection

Export: `kiteclass-branding.postman_collection.json`
- Pre-configured requests
- Environment variables (tenant, token)
- Example responses

### 5. Sandbox Environment

```
sandbox.kitehub.me
- Test API key provided
- Pre-populated test tenant
- No rate limit
- Reset daily
```

### 6. Versioning Strategy

```
/api/v1/branding/...  (current)
/api/v2/branding/...  (future)
```

- Semantic versioning
- Deprecation notice in response headers
- Changelog maintained

## Acceptance Criteria

- [ ] OpenAPI spec published at `/v3/api-docs`
- [ ] Swagger UI accessible
- [ ] 3 SDK libraries published (NPM + Maven + PyPI)
- [ ] Developer portal (static site)
- [ ] 5+ code examples
- [ ] Postman collection downloadable
- [ ] Sandbox environment setup
- [ ] API versioning strategy documented
- [ ] Integration test: SDK in sample app

## Dependencies

- GAP-010 (package API) — documents this endpoint
- Kitehub-branding must have stable public API

## Log

- 2026-04-14 — Developer DX gap identified
