# AI Branding — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `ai.rate-limit`, `ai.provider`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| AIB-01 | FREE/TRIAL daily limit | 3 requests/ngày | `ai.rate-limit.free-per-day` |
| AIB-02 | BASIC daily limit | 10 requests/ngày | `ai.rate-limit.basic-per-day` |
| AIB-03 | PREMIUM daily limit | 50 requests/ngày | `ai.rate-limit.premium-per-day` |
| AIB-04 | ENTERPRISE daily limit | Unlimited (-1) | `ai.rate-limit.enterprise-per-day` |
| AIB-05 | Rate limit reset | Daily (per calendar day) | AIUsageLog.usageDate = LocalDate |
| AIB-06 | Usage tracking | AIUsageLog (instanceId, usageDate, requestCount) | ai_usage_log table |
| AIB-07 | Usage increment | Upsert: increment nếu exists, create nếu new | recordUsage() |
| AIB-08 | Unlimited marker | -1 = không giới hạn | isRateLimited() check |
| AIB-09 | AI provider | Configurable: ollama hoặc openai | `ai.provider` |
| AIB-10 | Template gallery | Pre-built templates, không cần AI | TemplateGalleryService |
| AIB-11 | Template categories | education, business, general | category filter |
| AIB-12 | Template apply | Trả về themeConfig JSON | applyTemplate() |
| AIB-13 | Template active filter | Chỉ hiện active=true | findByActiveTrueOrderByNameAsc() |

## Config

```yaml
ai:
  provider: ${AI_PROVIDER:openai}
  rate-limit:
    free-per-day: 3
    basic-per-day: 10
    premium-per-day: 50
    enterprise-per-day: -1

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
```
