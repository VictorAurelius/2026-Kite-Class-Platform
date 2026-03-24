# AI Branding — API Contract

## POST /api/platform/branding/ai/analyze-logo
**Use case:** UC-AIB-01
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:**
```json
{
  "logoUrl": "https://example.com/logo.png",
  "organizationName": "Trường ABC"
}
```
**Response 200:**
```json
{
  "primaryColor": "#1a73e8",
  "secondaryColor": "#fbbc04",
  "fontFamily": "Roboto",
  "brandIdentity": "professional, education-focused"
}
```
**Errors:**
- 429: `{ "error": "AI_RATE_LIMIT_EXCEEDED", "dailyLimit": 3, "tier": "FREE" }`

---

## POST /api/platform/branding/ai/generate-image
**Use case:** UC-AIB-02
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:**
```json
{
  "organizationName": "Trường ABC",
  "theme": "modern education",
  "colors": "#1a73e8, #fbbc04"
}
```
**Response 200:**
```json
{ "imageUrl": "https://cdn.example.com/generated/hero-abc.png" }
```
**Errors:** 429 rate limit exceeded

---

## POST /api/platform/branding/ai/generate-text
**Use case:** UC-AIB-03
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:**
```json
{
  "organizationName": "Trường ABC",
  "theme": "modern education",
  "targetAudience": "học sinh THPT"
}
```
**Response 200:**
```json
{ "text": "Nâng tầm học tập với công nghệ hiện đại..." }
```
**Errors:** 429 rate limit exceeded

---

## POST /api/platform/branding/ai/generate-theme
**Use case:** UC-AIB-04
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:** LogoAnalysis object
```json
{
  "primaryColor": "#1a73e8",
  "secondaryColor": "#fbbc04",
  "fontFamily": "Roboto",
  "brandIdentity": "professional"
}
```
**Response 200:** ThemeConfig object
```json
{
  "primary": "#1a73e8",
  "secondary": "#fbbc04",
  "fontFamily": "Roboto",
  "borderRadius": "8px",
  "spacing": "comfortable"
}
```
**Errors:** 429 rate limit exceeded

---

## GET /api/platform/branding/templates
**Use case:** UC-AIB-05
**Auth:** Bearer token
**Request params:** `?category=education` (optional)
**Response 200:**
```json
[
  {
    "id": "uuid",
    "name": "Education Modern",
    "category": "education",
    "active": true,
    "themeConfig": "{ ... }"
  }
]
```

---

## GET /api/platform/branding/templates/{id}
**Auth:** Bearer token
**Response 200:** Single BrandingTemplate
**Errors:** 404 not found

---

## POST /api/platform/branding/templates/{id}/apply
**Use case:** UC-AIB-06
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}` (required)
**Response 200:**
```json
{
  "themeConfig": "{ \"primary\": \"#1a73e8\", ... }",
  "status": "applied"
}
```
**Errors:** 404 template not found
