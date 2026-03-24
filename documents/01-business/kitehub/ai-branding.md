# AI Branding

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| AIB-01 | FREE/TRIAL daily limit | 3 requests/day | `ai.rate-limit.free-per-day` |
| AIB-02 | BASIC daily limit | 10 requests/day | `ai.rate-limit.basic-per-day` |
| AIB-03 | PREMIUM daily limit | 50 requests/day | `ai.rate-limit.premium-per-day` |
| AIB-04 | ENTERPRISE daily limit | Unlimited (-1) | `ai.rate-limit.enterprise-per-day` |
| AIB-05 | Rate limit reset | Daily (per calendar day via LocalDate) | AIUsageLog.usageDate |
| AIB-06 | Usage tracking | AIUsageLog entity (instanceId, usageDate, requestCount) | ai_usage_log table |
| AIB-07 | Usage increment | Upsert: increment if exists, create if new | recordUsage() |
| AIB-08 | Unlimited marker | -1 means no limit enforced | isRateLimited() check |
| AIB-09 | AI provider | Configurable: ollama (local) or openai (cloud) | `ai.provider` |
| AIB-10 | Template gallery | Pre-built templates, no AI generation needed | TemplateGalleryService |
| AIB-11 | Template categories | education, business, general | category filter |
| AIB-12 | Template apply | Returns themeConfig JSON for instant frontend use | applyTemplate() |
| AIB-13 | Template active filter | Only active=true templates shown | findByActiveTrueOrderByNameAsc() |

## Flow

### AI Rate Limit Check
1. Receive AI branding request with instanceId + tier
2. Call `isRateLimited(instanceId, tier)`
3. Get limit for tier from config (-1 = unlimited)
4. If limit < 0, allow (unlimited)
5. Get current usage: count from AIUsageLog for today
6. If currentUsage >= limit, reject (rate limited)
7. On successful AI request: call `recordUsage(instanceId)`
8. Upsert AIUsageLog: increment requestCount or create new record

### Template Gallery Flow (No AI)
1. User browses templates: GET /api/platform/branding/templates?category=education
2. Returns list of active BrandingTemplate entities
3. User selects template: GET /api/platform/branding/templates/{id}
4. User applies template: POST /api/platform/branding/templates/{id}/apply
   - Header: X-Instance-Id required
   - Returns themeConfig JSON + status "applied"
5. Frontend applies theme config instantly (< 1s response time)

### AI Branding Flow (With AI)
1. User submits branding request (logo/colors/description)
2. Rate limit check against tier
3. If allowed: process via AI provider (ollama or openai)
4. Record usage in AIUsageLog
5. Return generated branding assets
6. Uses RabbitMQ queue for async processing

## Emails

No AI branding-specific emails are sent.

## Config

```yaml
ai:
  provider: ${AI_PROVIDER:openai}  # "ollama" or "openai"
  rate-limit:
    free-per-day: 3
    basic-per-day: 10
    premium-per-day: 50
    enterprise-per-day: -1           # unlimited

  ollama:
    base-url: ${OLLAMA_BASE_URL:http://kite-ollama:11434}
    text-model: ${OLLAMA_TEXT_MODEL:llama3.1:8b}
    vision-model: ${OLLAMA_VISION_MODEL:llava:13b}
    timeout-seconds: ${OLLAMA_TIMEOUT:120}

openai:
  api:
    key: ${OPENAI_API_KEY:sk-mock-key-for-local-testing}
    base-url: https://api.openai.com/v1
  models:
    vision: gpt-4-vision-preview
    dalle: dall-e-3
    text: gpt-4-turbo
  rate-limit:
    requests-per-minute: 10
  timeout:
    seconds: 60

storage:
  s3:
    bucket: ${S3_BUCKET:kitehub-assets}
    region: ${AWS_REGION:ap-southeast-1}
    cdn-domain: ${CDN_DOMAIN:localhost:9100}
    mock-mode: ${S3_MOCK_MODE:true}
```
