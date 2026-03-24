# AI Branding — Use Cases

### UC-AIB-01: Phân tích Logo (AI)
- **Actor:** Owner (đã xác thực)
- **Precondition:** Instance có tier, không vượt daily limit
- **Steps:**
  1. FE: AI Branding page, upload logo URL + org name
  2. System: check rate limit theo tier (AIB-01 đến AIB-04)
  3. System: nếu vượt limit → 429 Too Many Requests
  4. System: gọi AI provider phân tích logo → trả về brand identity
  5. System: record usage (AIB-07)
- **Postcondition:** Logo analysis result returned
- **Errors:**
  - 429: rate limit exceeded → "Daily AI request limit exceeded. Limit: 3/day (FREE tier)"
- **FE Behavior:** Hiển thị remaining requests (e.g., "2 requests remaining today")

### UC-AIB-02: Generate Hero Image (AI)
- **Actor:** Owner
- **Precondition:** Không vượt daily limit
- **Steps:**
  1. FE: nhập org name, theme, colors
  2. System: rate limit check
  3. System: generate hero banner via DALL-E / Ollama vision
  4. System: record usage
  5. System: trả về imageUrl
- **Errors:**
  - 429: rate limit exceeded

### UC-AIB-03: Generate Marketing Text (AI)
- **Actor:** Owner
- **Precondition:** Không vượt daily limit
- **Steps:**
  1. FE: nhập org name, theme, target audience
  2. System: rate limit check
  3. System: generate marketing copy via GPT-4 / Ollama text
  4. System: record usage
  5. System: trả về text
- **Errors:**
  - 429: rate limit exceeded

### UC-AIB-04: Generate Full Theme (AI)
- **Actor:** Owner
- **Precondition:** Đã có LogoAnalysis result
- **Steps:**
  1. FE: submit LogoAnalysis object
  2. System: rate limit check
  3. System: generate ThemeConfig (colors, typography, spacing, layout)
  4. System: record usage
  5. System: trả về ThemeConfig JSON
- **Postcondition:** Complete theme ready for KiteClass frontend

### UC-AIB-05: Browse Template Gallery (Không cần AI)
- **Actor:** Owner (bất kỳ tier)
- **Steps:**
  1. FE: GET /api/platform/branding/templates?category=education
  2. System: trả về active templates filtered by category
  3. User: chọn template
  4. FE: GET /api/platform/branding/templates/{id}

### UC-AIB-06: Apply Template (Instant, Không cần AI)
- **Actor:** Owner
- **Steps:**
  1. FE: POST /api/platform/branding/templates/{id}/apply với X-Instance-Id header
  2. System: find template, return themeConfig JSON
  3. FE: apply theme config ngay lập tức (< 1s)
- **Postcondition:** Instance theme updated
- **Errors:**
  - 404: template not found
