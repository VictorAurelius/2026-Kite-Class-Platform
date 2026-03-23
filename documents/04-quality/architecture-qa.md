# Architecture Clarification Q&A

**Mục đích:** Xác nhận yêu cầu cho 4 vấn đề architecture ảnh hưởng đến frontend implementation
**Deadline:** Trước khi bắt đầu PR 3.2
**Người trả lời:** Product Owner / Tech Lead

---

# PART 1: PRICING TIER UI CUSTOMIZATION

## 1.1. Feature Detection Mechanism

### Q1.1.1: Feature Detection API Endpoint ✅ ANSWERED
**Câu hỏi:** Backend sẽ cung cấp endpoint nào để frontend query available features?

**Đề xuất:**
```
GET /api/v1/instance/config
GET /api/v1/instance/features
GET /api/v1/subscription/status
```

**Vui lòng chọn hoặc đề xuất endpoint khác:**
- [x] `/api/v1/instance/config` (Recommended)
- [ ] `/api/v1/instance/features`
- [ ] Khác: _____________________

**Answer:** Sử dụng best practice: `/api/v1/instance/config`

**Response format mong muốn:**
```json
{
  "instanceId": "abc-academy-001",
  "tier": "STANDARD",
  "addOns": ["ENGAGEMENT"],
  "services": ["user-gateway", "core", "engagement", "frontend"],
  "features": {
    "classManagement": true,
    "studentManagement": true,
    "attendance": true,
    "billing": true,
    "gamification": true,
    "parentPortal": true,
    "forum": true,
    "videoUpload": false,
    "liveStreaming": false,
    "aiMarketing": false
  },
  "limits": {
    "maxStudents": 200,
    "maxCourses": null,
    "videoStorageGB": 0,
    "maxConcurrentStreams": 0
  },
  "owner": {
    "id": "owner-uuid-123",
    "name": "Nguyễn Văn A",
    "email": "owner@example.com"
  }
}
```

**Response format này có OK không?**
- [x] OK, implement đúng như vậy
- [ ] Cần điều chỉnh: _____________________

**Updated in:** system-architecture-v3-final.md PHẦN 6B.1

---

### Q1.1.2: Feature Detection Caching ✅ ANSWERED
**Câu hỏi:** Feature flags có thay đổi trong runtime không? Frontend có cần poll để update không?

**Scenarios:**
1. Customer nâng cấp từ BASIC → STANDARD trong khi đang dùng
2. Customer thêm MEDIA pack trong khi đang dùng
3. Trial expires → features bị lock

**Vui lòng trả lời:**

**Feature flags có thể thay đổi khi user đang online không?**
- [ ] CÓ - Frontend cần poll hoặc WebSocket để update real-time
- [x] KHÔNG - Chỉ update khi user login lại

**Answer:** User muốn upgrade → Vào KiteHub portal → Thực hiện thay đổi → User login lại instance. Đây là best practice (centralized management).

**Rationale:**
- ✅ Tập trung billing/subscription quản lý tại KiteHub
- ✅ Audit trail cho mọi config changes
- ✅ Security: Instance users không access billing APIs
- ✅ Simplify frontend: No polling, no WebSocket
- ✅ Consistent UX: Features không suddenly appear/disappear

**Nếu CÓ thay đổi runtime:**
**Cơ chế notification nào sẽ dùng?**
- [ ] Frontend poll mỗi 5 phút
- [ ] WebSocket push notification từ backend
- [ ] Server-Sent Events (SSE)
- [x] Không cần real-time, user sẽ refresh page

**Cache TTL bao lâu?**
- [x] 1 giờ (Recommended)
- [ ] 24 giờ
- [ ] Khác: _____ giờ

**Updated in:** system-architecture-v3-final.md PHẦN 6B.1 (Caching Strategy)

---

### Q1.1.3: Feature Lock Behavior ✅ ANSWERED
**Câu hỏi:** Khi user access feature bị lock (ví dụ: BASIC tier click vào Gamification), hành vi nào mong muốn?

**Option A: Hard Block (Recommended)**
```
User click "Game hóa" trong menu
→ Redirect to /upgrade page
→ Hiện pricing comparison
→ CTA: "Nâng cấp lên gói STANDARD"
```

**Option B: Soft Block với Preview**
```
User click "Game hóa" trong menu
→ Show modal với:
  - Preview/screenshot của feature
  - Benefits của feature
  - Pricing info
  - CTA: "Dùng thử 7 ngày" hoặc "Nâng cấp ngay"
```

**Option C: Hide Completely**
```
User không thấy menu "Game hóa"
→ Không biết feature này tồn tại
```

**Vui lòng chọn:**
- [ ] Option A: Hard Block (redirect to /upgrade)
- [x] Option B: Soft Block (modal with preview)
- [ ] Option C: Hide completely (no menu item)
- [ ] Khác: _____________________

**Answer:** Option B - Soft Block với Preview

**Rationale:**
- ✅ Better conversion (show value before upgrade)
- ✅ Educate users about features
- ✅ Friendly UX (not frustrating)
- ✅ Can showcase feature screenshots

**Modal Structure:**
```
🔒 Tính năng X chỉ có trên gói Y
📸 Preview screenshot (600x400px)
✨ Benefits (3-5 bullet points)
💰 Gói Y: [Price]/tháng

[Nâng cấp ngay] [Xem chi tiết] [Đóng]
```

**Updated in:** system-architecture-v3-final.md PHẦN 6B.3

---

### Q1.1.4: Resource Limit Warnings ✅ ANSWERED
**Câu hỏi:** Khi nào hiển thị warning về giới hạn tài nguyên?

**Ví dụ:** STANDARD tier có limit 200 học viên

**Warning thresholds:**
- [x] 80% capacity (160/200 students) → Warning banner
- [x] 90% capacity (180/200 students) → Warning banner + email
- [x] 100% capacity (200/200 students) → Block thêm học viên mới + force upgrade

**Answer:** Sử dụng best practice - 3-tier warning system

**Thresholds:**
| Capacity | UI Display | Action |
|----------|------------|--------|
| < 80% | No warning | Normal operation |
| 80-89% | ⚠️ Yellow banner | "Sắp đạt giới hạn (160/200)" |
| 90-99% | 🟠 Orange alert | "Gần đạt giới hạn (180/200). Nâng cấp ngay." |
| 100% | 🔴 Red block | "Đã đạt giới hạn 200 học viên" + Disable button |

**UI Behavior khi đạt 100% limit:**
- [x] Disable "Thêm học viên" button
- [ ] Show "Thêm học viên" button nhưng click → upgrade modal
- [ ] Cho phép exceed limit 5% (grace period)
- [ ] Khác: _____________________

**Email notification khi đạt limit?**
- [x] CÓ - Gửi email tự động cho CENTER_OWNER (at 90% and 100%)
- [ ] KHÔNG - Chỉ show UI warning

**Updated in:** system-architecture-v3-final.md PHẦN 6B.2 (Resource Limit Warnings)

---

### Q1.1.5: Tier Upgrade Flow ✅ ANSWERED
**Câu hỏi:** Khi user click "Nâng cấp gói", flow như thế nào?

**Option A: Instant Upgrade (Online Payment)**
```
User click "Nâng cấp"
→ Pricing page
→ Select tier + add-ons
→ Payment gateway (VNPay/MoMo)
→ Payment success
→ Backend auto-provision services
→ Features available ngay lập tức
```

**Option B: Request-Based (Offline)**
```
User click "Nâng cấp"
→ Submit upgrade request form
→ Sales team contact
→ Customer transfer tiền
→ Admin manually approve
→ Backend provision services (1-2 ngày)
```

**Option C: Hybrid**
```
Upgrade BASIC → STANDARD: Instant (online payment)
Upgrade to PREMIUM: Sales contact (offline)
Add-ons: Instant (online payment)
```

**Vui lòng chọn:**
- [ ] Option A: Instant upgrade với online payment
- [ ] Option B: Request-based với sales involvement
- [ ] Option C: Hybrid (tier nhỏ instant, tier lớn sales)
- [x] Khác: Role-based redirect (Best Practice)

**Answer:** Tùy theo actor role:

**Case 1: CENTER_OWNER clicks locked feature**
```
Show Soft Block Modal
  ↓
Click "Nâng cấp ngay"
  ↓
Redirect to KiteHub Portal: https://kiteclass.com/portal/upgrade?instance={id}
  ↓
KiteHub: Select tier → Payment → Provision
  ↓
Success → Redirect back to instance
  ↓
User login lại → New features available
```

**Case 2: Non-Owner (ADMIN/TEACHER/STUDENT) clicks locked feature**
```
Show Contact Owner Modal
  ↓
Display: "Liên hệ [Owner Name]"
         📧 owner@example.com
         📱 0123456789
  ↓
Click "Gửi yêu cầu qua email"
  ↓
Backend sends notification to OWNER
  ↓
Show success: "Đã gửi yêu cầu đến Center Owner"
```

**Architecture Principle:**
Mọi thao tác thay đổi cấu hình instance phải thông qua KiteHub (centralized management). Đây là best practice.

**Nếu Option A, payment gateway nào?**
- [x] VNPay (primary for Vietnam)
- [x] MoMo
- [ ] ZaloPay
- [ ] Stripe (international)
- [ ] Tất cả

**Note:** Payment gateway implementation in PR 3.7+ (future)

**Updated in:** system-architecture-v3-final.md PHẦN 6B.2 (Tier Upgrade Flow)

---

## 1.2. Tier-Specific UI Differences

### Q1.2.1: UI Customization Level ✅ ANSWERED
**Câu hỏi:** Ngoài feature availability, có điểm khác biệt UI nào giữa các tier không?

**Ví dụ potential differences:**

| Feature | BASIC | STANDARD | PREMIUM |
|---------|-------|----------|---------|
| Logo branding | ✅ Custom logo | ✅ Custom logo | ✅ Custom logo |
| Theme colors | ✅ Custom colors | ✅ Custom colors | ✅ Custom colors |
| Remove "Powered by KiteClass" | ❌ | ❌ | ❌ |
| Custom domain | ❌ | ❌ | ✅ |
| Priority support badge | ❌ | ❌ | ✅ |

**Vui lòng xác nhận:**

**BASIC tier có được custom logo không?**
- [x] CÓ - Tất cả tier đều có custom logo
- [ ] KHÔNG - Chỉ STANDARD và PREMIUM

**BASIC tier có được custom theme colors không?**
- [x] CÓ - Tất cả tier đều custom được
- [ ] KHÔNG - Chỉ PREMIUM mới custom được
- [ ] KHÔNG - Tất cả tier đều dùng AI-generated branding

**Answer:** Tất cả tier đều có AI-generated branding (logo, colors, banners). Philosophy: "Cung cấp đủ feature cho người giàu" - Equal features, differentiate by scale/support only.

**Có watermark "Powered by KiteClass" không?**
- [ ] CÓ - Hiện trên BASIC và STANDARD, PREMIUM thì remove được
- [x] CÓ - Hiện trên tất cả tier
- [ ] KHÔNG - Không có watermark

**Answer:** Tất cả tier đều có watermark "Powered by KiteClass" ở footer. Purpose: Brand awareness, free marketing.

**PREMIUM có được custom subdomain không?**
- [x] CÓ - Ví dụ: custom-domain.com thay vì abc-academy.kiteclass.com
- [ ] KHÔNG - Tất cả dùng *.kiteclass.com

**Answer:** PREMIUM tier có thể custom domain (e.g., abc-academy.com thay vì abc-academy.kiteclass.com). Implementation: DNS CNAME + SSL auto-provision + Nginx reverse proxy. Effort: 2-3 weeks.

**Rationale:**
- ✅ Equal UX: All customers get beautiful, professional branding
- ✅ Competitive advantage: Even cheapest tier looks premium
- ✅ Lower barriers: Customers don't feel "poor" on basic tier
- ✅ Simpler codebase: Same UI components for all tiers
- ✅ Marketing: Watermark on all tiers → brand awareness

**Updated in:** system-architecture-v3-final.md PHẦN 6C.1

---

### Q1.2.2: Analytics & Reporting Access ✅ ANSWERED
**Câu hỏi:** Analytics features có khác nhau giữa các tier không?

**Đề xuất differentiation:**

| Feature | BASIC | STANDARD | PREMIUM |
|---------|-------|----------|---------|
| Basic reports (điểm danh, học phí) | ✅ | ✅ | ✅ |
| Advanced analytics dashboard | ✅ | ✅ | ✅ |
| Export to Excel | ✅ | ✅ | ✅ |
| Custom reports | ✅ | ✅ | ✅ |
| API access | ✅ | ✅ | ✅ |

**Có implement tier-based analytics không?**
- [ ] CÓ - Implement theo bảng trên
- [ ] CÓ - Nhưng khác: _____________________
- [x] KHÔNG - Tất cả tier có full analytics

**Answer:** Tất cả tier có đầy đủ analytics và reporting features. No differentiation.

**Rationale:**
- ✅ Philosophy: "Cung cấp đủ feature cho người giàu"
- ✅ Better UX: No frustration from missing features
- ✅ Data-driven decisions: All customers can analyze their business
- ✅ Simpler code: No conditional rendering for analytics
- ✅ Differentiation by scale: BASIC (≤50 students) vs PREMIUM (unlimited)

**Tier differentiation is by:**
1. **Scale/Limits:** Max students, storage, concurrent streams
2. **Support:** PREMIUM gets priority support
3. **Infrastructure:** Custom domain (PREMIUM only)

**NOT by features:** All customers get same features, same UX quality

**Updated in:** system-architecture-v3-final.md PHẦN 6C.1 (Equal Features Philosophy)

---

# PART 2: AI BRANDING SYSTEM

## 2.1. AI Branding Workflow

### Q2.1.1: Who Can Upload Branding? ✅ ANSWERED
**Câu hỏi:** Ai có quyền upload ảnh để generate branding?

**Vui lòng chọn:**
- [ ] CENTER_OWNER only
- [x] CENTER_OWNER và CENTER_ADMIN
- [ ] Tất cả roles (TEACHER cũng được)
- [ ] Chỉ KiteHub Admin (customer không tự upload được)

**Answer:** CENTER_OWNER và CENTER_ADMIN có quyền upload branding.

**Approval Workflow:**
```
Step 1: CENTER_ADMIN uploads logo → AI generates assets → Save to DRAFT storage
Step 2: CENTER_ADMIN previews → Manual override if needed
Step 3: CENTER_ADMIN submits for approval
Step 4: CENTER_OWNER reviews draft → Approve or reject
Step 5: If approved → Publish to PRODUCTION storage → Apply to instance
```

**Rationale:**
- ✅ Delegation: OWNER can delegate branding work to ADMIN
- ✅ Quality control: OWNER has final approval before publish
- ✅ Separation: Draft (experimentation) vs Published (production)
- ✅ Security: TEACHER/STUDENT cannot change branding

**Storage Tiers:**
- **Draft:** /kitehub/users/{userId}/branding-drafts/ (30-day TTL)
- **Published:** /instances/{instanceId}/branding/ (permanent, versioned)

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (Approval Workflow)

---

### Q2.1.2: Re-generation Policy ✅ ANSWERED
**Câu hỏi:** Customer có thể generate lại branding bao nhiêu lần?

**Scenarios:**
- Customer không thích kết quả AI generation
- Customer muốn đổi logo mới
- Customer muốn adjust colors

**Policy options:**

**Option A: Unlimited Free**
- Customer generate lại bao nhiêu lần cũng được
- Mỗi lần generate cost $0.186 → Platform chi phí

**Option B: Limited per Month**
- 1 lần free mỗi tháng
- Từ lần thứ 2: charge $5/generation

**Option C: Tier-Based**
- BASIC: 1 lần/tháng
- STANDARD: 3 lần/tháng
- PREMIUM: Unlimited

**Option D: One-Time Only**
- Generate 1 lần duy nhất khi setup instance
- Muốn đổi → contact support

**Vui lòng chọn:**
- [ ] Option A: Unlimited free
- [ ] Option B: 1 free/tháng, $5 cho lần sau
- [ ] Option C: Tier-based limits
- [ ] Option D: One-time only
- [x] Khác: Hybrid - AI + Manual Override (Best Practice)

**Answer:** Hybrid approach - Unlimited AI generation với manual override.

**Implementation:**
```
1. Initial AI Generation: Free, unlimited iterations
   - Upload logo → Generate 10+ assets
   - Don't like? Upload new logo → Re-generate
   - Cost: ~$0.10/generation (acceptable)

2. Manual Override: Free, unlimited edits
   - AI generated headline: "Học viện ABC - Nơi ươm mầm tài năng"
   - Customer edit: "Học viện ABC - Khơi nguồn tri thức"
   - Change colors, adjust text, reposition logo
   - Cost: $0 (no AI call)

3. Hybrid Workflow:
   - Generate with AI → Preview → Manual tweaks → Publish
   - New logo → Re-generate → Keep manual overrides if possible
```

**Rationale:**
- ✅ Best UX: No generation limits = no frustration
- ✅ Cost-effective: $0.10/generation is negligible (~1-5 generations typical)
- ✅ Flexibility: Manual override for fine-tuning without re-generation
- ✅ Quality: Customer can iterate until satisfied
- ✅ Competitive advantage: Most platforms charge per generation

**Edge Case Prevention:**
- Rate limit: Max 10 generations/hour (prevent abuse/accidents)
- Warning at 5th generation: "Bạn đã generate 5 lần, cân nhắc dùng Manual Edit"

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (Hybrid Re-generation Policy)

---

### Q2.1.3: Manual Override ✅ ANSWERED
**Câu hỏi:** Customer có thể manual edit AI-generated assets không?

**Ví dụ:**
- AI generate headline: "Học viện ABC - Nơi ươm mầm tài năng"
- Customer muốn đổi thành: "Học viện ABC - Nơi khơi nguồn tri thức"

**Manual override options:**

**Text Content (headlines, CTAs):**
- [x] CÓ - Customer tự edit text trong admin panel
- [ ] KHÔNG - Phải dùng text do AI generate

**Logo Position/Size:**
- [x] CÓ - Customer adjust position, scale
- [ ] KHÔNG - Fixed layout

**Colors:**
- [x] CÓ - Customer override primary/secondary colors
- [ ] KHÔNG - Phải dùng colors do AI extract

**Images:**
- [x] CÓ - Customer upload custom hero banner (không dùng AI)
- [ ] KHÔNG - Chỉ dùng AI-generated banners

**Answer:** TẤT CẢ assets đều có thể manual override. 100% flexibility.

**Manual Override UI:**
```typescript
interface BrandingAsset {
  id: string
  type: 'hero' | 'section-banner' | 'logo' | 'og-image' | 'text'

  // AI-generated (original)
  aiGenerated: {
    url?: string          // For images
    text?: string         // For text content
    colors?: ColorScheme
    position?: Layout
  }

  // Manual overrides (optional)
  manualOverride?: {
    url?: string          // Upload custom image
    text?: string         // Edit text
    colors?: ColorScheme  // Change colors
    position?: Layout     // Adjust layout
  }

  // Active (what's actually displayed)
  active: 'ai' | 'manual'
}
```

**Example Workflow:**
```
1. AI generates hero banner with headline: "Học viện ABC - Ươm mầm tài năng"
2. Customer clicks "Edit Text" → Input: "Khơi nguồn tri thức"
3. Preview shows manual version
4. Customer clicks "Apply" → manualOverride.text saved
5. Customer can "Revert to AI" anytime
```

**Rationale:**
- ✅ Best practice: AI as starting point, human as final polish
- ✅ Flexibility: Some customers want full control
- ✅ Quality: Human judgment > AI for nuanced branding
- ✅ Edge cases: AI might generate inappropriate content (rare)
- ✅ Branding consistency: Customer can align with existing brand guidelines

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (Manual Override System)

---

### Q2.1.4: Asset Storage & CDN ✅ ANSWERED
**Câu hỏi:** AI-generated assets sẽ store ở đâu?

**Vui lòng chọn:**
- [ ] AWS S3 + CloudFront CDN
- [x] Cloudflare R2 + CDN
- [ ] Local server storage (không dùng CDN)
- [ ] Khác: _____________________

**Answer:** Cloudflare R2 + CDN (Best Practice)

**2-Tier Storage Architecture:**

**Tier 1: Draft Storage (Experimentation)**
```
Location: /kitehub/users/{userId}/branding-drafts/
Purpose: AI generation iterations, manual edits
Retention: 30 days after last update
Access: CENTER_ADMIN + CENTER_OWNER only
CDN: No (draft content doesn't need CDN)
```

**Tier 2: Published Storage (Production)**
```
Location: /instances/{instanceId}/branding/
Purpose: Live branding assets on instance
Retention: Forever (with versioning)
Access: Public CDN
CDN: Yes (Cloudflare global CDN)
Versioning: v1, v2, v3 (rollback capability)
```

**Asset retention policy:**
- [x] Keep forever (không xóa)
- [ ] Keep 1 năm, sau đó archive
- [ ] Delete khi customer churn

**Answer:** Published assets keep forever với versioning. Draft assets TTL 30 days.

**Rationale:**
- ✅ Rollback: Customer can revert to previous branding version
- ✅ Audit: Track branding history
- ✅ No data loss: Even after churn, assets preserved (potential comeback)

**Quota per instance:**
- [x] No limit
- [ ] 1GB storage
- [ ] 5GB storage
- [ ] Khác: _____ GB

**Answer:** No hard limit. Typical usage: 10-20 assets × 200KB = 2-4MB total.

**Rationale:**
- ✅ Cost-effective: R2 storage is $0.015/GB/month → $0.0001/instance
- ✅ No surprises: Customers won't hit quota limits
- ✅ Simpler UX: No "storage full" errors

**Soft Limit:** 100MB per instance (alert if exceeded, likely indicates issue)

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (2-Tier Storage Architecture)

---

### Q2.1.5: Asset Quality Settings ✅ ANSWERED
**Câu hỏi:** Quality settings cho AI-generated images?

**Hero Banner (1920x600):**
- [ ] High quality (300KB - 500KB, best visual)
- [x] Medium quality (150KB - 250KB, balanced)
- [ ] Low quality (< 100KB, fast load)

**Answer:** Medium quality 85% WebP (200-300KB) with JPEG fallback

**Profile Images (400x400):**
- [ ] High quality (~200KB)
- [x] Medium quality (~100KB)
- [ ] Low quality (~50KB)

**Answer:** Medium quality 90% WebP (50-80KB) with JPEG fallback

**WebP format support:**
- [x] CÓ - Use WebP với fallback to JPEG
- [ ] KHÔNG - Chỉ dùng JPEG/PNG

**Answer:** WebP + JPEG fallback (Best Practice for 2025+)

**Detailed Quality Settings:**

| Asset Type | Dimensions | Format | Quality | Size Range | Use Case |
|------------|------------|--------|---------|------------|----------|
| Hero Banner | 1920×600 | WebP | 85% | 200-300KB | Landing page hero |
| Section Banner | 1200×400 | WebP | 85% | 150-200KB | Course sections |
| Profile Logo | 400×400 | WebP | 90% | 50-80KB | User profile, navbar |
| Favicon | 512×512 | PNG | 100% | 30-50KB | Browser tab icon |
| OG Image | 1200×630 | JPEG | 85% | 150-200KB | Social media preview |

**Format Strategy:**
```html
<picture>
  <source srcset="hero-banner.webp" type="image/webp">
  <source srcset="hero-banner.jpg" type="image/jpeg">
  <img src="hero-banner.jpg" alt="Hero Banner">
</picture>
```

**Browser Support (2025):**
- WebP: 97%+ browsers (Chrome, Firefox, Safari, Edge)
- JPEG fallback: 100% browsers

**Rationale:**
- ✅ Performance: WebP 25-35% smaller than JPEG at same quality
- ✅ Visual quality: 85-90% indistinguishable from 100%
- ✅ Page load: Hero banner loads in <500ms on 4G
- ✅ SEO: Google Page Speed score 90+
- ✅ Future-proof: WebP is industry standard

**Compression Settings:**
```javascript
// Image processing pipeline
sharp(inputBuffer)
  .resize(1920, 600, { fit: 'cover' })
  .webp({ quality: 85, effort: 6 })
  .toFile('hero-banner.webp')

sharp(inputBuffer)
  .resize(1920, 600, { fit: 'cover' })
  .jpeg({ quality: 85, progressive: true })
  .toFile('hero-banner.jpg')
```

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (Asset Quality Standards)

---

## 2.2. AI Service Provider

### Q2.2.1: Image Generation Provider ✅ ANSWERED
**Câu hỏi:** Sử dụng AI provider nào cho image generation?

**Current architecture mentions Stable Diffusion XL, but confirm:**

**Primary provider:**
- [x] Stable Diffusion XL (self-hosted)
- [ ] DALL-E 3 (OpenAI API)
- [ ] Midjourney API
- [ ] Stability AI API (hosted)
- [ ] Khác: _____________________

**Answer:** Stable Diffusion XL 1.0 (self-hosted) - Best Practice

**Fallback provider (nếu primary fail):**
- [ ] CÓ fallback: _____________________
- [x] KHÔNG fallback

**Answer:** No fallback. Stable Diffusion XL is reliable enough. If fails → Retry with exponential backoff.

**Cost consideration:**
- Stable Diffusion XL: ~$0.08/image (self-hosted)
- DALL-E 3: ~$0.04/image (1024x1024)
- Midjourney: ~$0.07/image

**Budget per generation job (10 images):**
- [x] < $0.50 (use cheaper options)
- [ ] $0.50 - $1.00 (balanced)
- [ ] > $1.00 (highest quality)

**Answer:** $0.10 per complete generation (4 banner images × $0.08 = $0.32, rest are free)

**Detailed Cost Breakdown:**
```
AI Generation Pipeline (Total: ~$0.10)

1. Background Removal: U2-Net (self-hosted) = $0.00
2. Color Extraction: Python/PIL = $0.00
3. Text Generation: GPT-4o-mini = $0.002
4. Hero Banner: SDXL = $0.08
5. Section Banner 1: SDXL = $0.08  (skip if budget tight)
6. Section Banner 2: SDXL = $0.08  (skip if budget tight)
7. Section Banner 3: SDXL = $0.08  (skip if budget tight)
8. Logo variants: ImageMagick = $0.00
9. OG Image: Composite = $0.00

Minimal: $0.082 (1 hero banner only)
Standard: $0.10 (hero + 1 section banner)
Full: $0.32 (hero + 3 section banners)
```

**Recommendation:** Standard package ($0.10) with 1 hero + 1 reusable section banner

**Rationale:**
- ✅ Cost-effective: $0.10/customer is negligible
- ✅ Quality: SDXL produces photorealistic, professional images
- ✅ Control: Self-hosted = no API limits, no censorship, no vendor lock-in
- ✅ Privacy: Logo stays on our servers (vs sending to OpenAI/Midjourney)
- ✅ Customization: Fine-tune model for education domain
- ✅ Latency: Local inference = 20-30s vs 60s+ for external APIs

**Hardware Requirements:**
- GPU: NVIDIA A100 (40GB) or 2× RTX 4090 (24GB each)
- Generation time: ~20-30s per image (1920×600)
- Concurrent: 4-8 generations simultaneously

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (AI Provider Stack)

---

### Q2.2.2: Background Removal Service ✅ ANSWERED
**Câu hỏi:** Background removal dùng service nào?

**Options:**
- [ ] Remove.bg API ($0.09/image, highest quality)
- [x] U2-Net (self-hosted, free)
- [ ] Cloudinary Remove Background
- [ ] Khác: _____________________

**Answer:** U2-Net (self-hosted) - Best Practice

**Rationale:**
- ✅ Cost: $0 vs $0.09/image (Remove.bg) = Save $0.09 × ∞ generations
- ✅ Privacy: Logo doesn't leave our infrastructure
- ✅ Quality: U2-Net quality is 95% as good as Remove.bg
- ✅ Latency: Local inference ~5-10s vs 15-30s API round-trip
- ✅ No limits: Unlimited usage, no rate limits

**Quality Comparison:**
```
Remove.bg:  ⭐⭐⭐⭐⭐ 5/5 (best, but expensive)
U2-Net:     ⭐⭐⭐⭐½ 4.5/5 (excellent, free)
rembg:      ⭐⭐⭐⭐ 4/5 (good, free, easier to deploy)
```

**Implementation:**
```python
# U2-Net model (https://github.com/xuebinqin/U-2-Net)
from u2net import U2NET
import torch

model = U2NET(3, 1).cuda()
model.load_state_dict(torch.load('u2net.pth'))

def remove_background(image_path):
    # Load image
    img = Image.open(image_path)

    # Run U2-Net inference (~5-10s)
    mask = model(img)

    # Apply mask
    result = img * mask

    return result  # Transparent PNG
```

**Hardware Requirements:**
- GPU: Any modern GPU (GTX 1080+ or equivalent)
- VRAM: 4GB minimum
- Processing time: 5-10s per image

**Fallback:**
If U2-Net quality insufficient for certain logos (complex backgrounds):
- Manual review flag: "Background removal quality low"
- Admin can manually clean up in Photoshop
- Or use Remove.bg API as fallback ($0.09, rare cases only)

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (Background Removal Pipeline)

---

### Q2.2.3: Text Generation (Marketing Copy) ✅ ANSWERED
**Câu hỏi:** Marketing copy generation dùng LLM nào?

**Options:**
- [ ] GPT-4 (~$0.015/generation, best quality)
- [x] GPT-4o-mini (~$0.002/generation, good quality)
- [ ] Claude 3.5 Sonnet
- [ ] Gemini Pro
- [ ] Self-hosted LLM (Llama, etc.)

**Answer:** GPT-4o-mini (OpenAI) - Best Practice

**Rationale:**
- ✅ Cost: $0.002 vs $0.015 (GPT-4) = 7.5× cheaper
- ✅ Quality: Good enough for marketing headlines (80-90% as good as GPT-4)
- ✅ Speed: ~1-2s response time
- ✅ Multi-language: Excellent Vietnamese support
- ✅ Reliability: OpenAI API 99.9% uptime

**Quality Comparison:**
```
GPT-4:          ⭐⭐⭐⭐⭐ 5/5 (best, but expensive)
GPT-4o-mini:    ⭐⭐⭐⭐ 4/5 (good, 7.5× cheaper)
Claude 3.5:     ⭐⭐⭐⭐⭐ 5/5 (best, but more expensive than GPT-4o-mini)
Llama 3:        ⭐⭐⭐ 3/5 (ok, free, self-hosted complexity)
```

**Prompt Template:**
```javascript
const prompt = `Generate marketing copy for an education center.

Center name: ${centerName}
Logo description: ${logoDescription}
Industry: ${industry}
Target audience: ${targetAudience}

Generate:
1. Hero headline (max 60 chars, inspiring, Vietnamese)
2. Hero subheadline (max 120 chars, benefits-focused)
3. Section 1 headline: "Về chúng tôi" (max 40 chars)
4. Section 1 text (max 200 chars)
5. Section 2 headline: "Khóa học" (max 40 chars)
6. Section 2 text (max 200 chars)
7. CTA text (max 20 chars, action-oriented)

Tone: ${tone}
Language: ${language}

Return JSON format.`
```

**Tone & style:**
- [ ] Professional & formal
- [ ] Friendly & casual
- [x] Inspiring & motivational
- [x] Tùy theo industry type (education vs corporate)

**Answer:** Tone tùy theo industry + có Manual Override

**Tone Presets:**
```javascript
const tonePresets = {
  education: 'Inspiring & motivational - Khơi nguồn học tập',
  corporate: 'Professional & results-driven - Đào tạo hiệu quả',
  kids: 'Friendly & fun - Vui học, chơi mà học',
  language: 'Encouraging & practical - Thành thạo ngoại ngữ',
  coding: 'Modern & tech-forward - Lập trình tương lai'
}
```

**Cost per Generation:**
- Input tokens: ~500 tokens × $0.000150/1K = $0.000075
- Output tokens: ~800 tokens × $0.000600/1K = $0.00048
- **Total: ~$0.002 per generation**

**Fallback:**
If GPT-4o-mini quality insufficient (subjective, rare):
- Admin can manually edit all text (Manual Override)
- Or upgrade to GPT-4 for specific regeneration ($0.015)

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (Text Generation Pipeline)

---

## 2.3. Multi-Language Support

### Q2.3.1: Language for Generated Content ✅ ANSWERED
**Câu hỏi:** AI-generated marketing copy sẽ là ngôn ngữ gì?

**Current assumption: Vietnamese only**

**Confirm:**
- [ ] Chỉ tiếng Việt
- [ ] Tiếng Việt + English
- [x] Multi-language (customer chọn)

**Answer:** Multi-language support - 5 ngôn ngữ chính

**Supported Languages:**
```typescript
type Language = 'vi' | 'en' | 'zh' | 'ja' | 'ko'

const languageLabels = {
  vi: 'Tiếng Việt',
  en: 'English',
  zh: '中文 (Chinese)',
  ja: '日本語 (Japanese)',
  ko: '한국어 (Korean)'
}
```

**Nếu multi-language:**
**Customer chọn ngôn ngữ khi nào?**
- [x] Khi upload ảnh (generate 1 lần cho 1 ngôn ngữ)
- [x] Sau khi generate (generate lại cho ngôn ngữ khác)
- [ ] Generate multiple languages cùng lúc

**Answer:** Generate 1 language at a time. Customer can switch language và re-generate.

**Workflow:**
```
Step 1: Upload logo → Select language (default: vi)
Step 2: AI generates text in Vietnamese
Step 3: Preview → Customer satisfied
Step 4: Customer clicks "Generate English version"
Step 5: AI re-generates text in English (images stay same)
Step 6: Publish multiple language versions
```

**Storage Structure:**
```javascript
interface BrandingAssets {
  images: {
    hero: 'hero-banner.webp',      // Same for all languages
    logo: 'logo-transparent.png'    // Same for all languages
  }

  textContent: {
    vi: {
      hero_headline: 'Học viện ABC - Khơi nguồn tri thức',
      hero_subheadline: 'Phương pháp giảng dạy hiện đại...',
      cta: 'Đăng ký ngay'
    },
    en: {
      hero_headline: 'ABC Academy - Ignite Knowledge',
      hero_subheadline: 'Modern teaching methods...',
      cta: 'Register Now'
    }
  }
}
```

**Cost per Language:**
- Images: $0.08 (generated once, reused for all languages)
- Text: $0.002 per language
- **Total for 5 languages: $0.08 + (5 × $0.002) = $0.09**

**Rationale:**
- ✅ International: Support Vietnamese diaspora teaching Chinese, Japanese, etc.
- ✅ Cost-effective: Text generation is cheap ($0.002)
- ✅ Flexibility: Customer can add languages later
- ✅ SEO: Multi-language landing pages → broader reach
- ✅ Branding consistency: Same visual assets, translated text

**Language Detection:**
Customer can set instance default language:
```typescript
interface InstanceConfig {
  defaultLanguage: Language
  supportedLanguages: Language[]
}
```

Guest users see content in:
1. Their browser language (if supported)
2. Instance default language (fallback)

**Updated in:** system-architecture-v3-final.md PHẦN 6C.3 (Multi-Language Support)

---

# PART 3: PREVIEW WEBSITE FEATURE

## 3.1. Feature Definition

### Q3.1.1: What is "Preview Website"? ✅ ANSWERED
**Câu hỏi:** "Preview Website" feature là gì? (CRITICAL - currently undefined)

**Vui lòng chọn 1 trong các interpretations sau hoặc mô tả chi tiết:**

**Option A: Instance Marketing Landing Page**
```
Mỗi KiteClass instance có public landing page riêng:
- URL: https://abc-academy.kiteclass.com hoặc abc-academy.com
- Content:
  - AI-generated hero banner
  - About the center
  - Course catalog (public view)
  - Contact info
  - "Đăng ký học" CTA
- Mục đích: Thu hút học viên mới (SEO, marketing)
- Audience: Prospective students (chưa đăng ký)
```

**Option B: Live Demo System for Trial**
```
KiteHub có 1 demo instance cho prospect customers:
- URL: https://demo.kiteclass.com
- Prepopulated với sample data (courses, students, etc.)
- Prospect click "Xem demo" → Vào demo instance với read-only access
- Mục đích: Cho customer thấy platform hoạt động thế nào
- Audience: Potential customers (chưa mua)
```

**Option C: Staging/Preview Environment**
```
Customer có thể preview changes trước khi publish:
- Customer update branding → "Preview" trước khi apply
- Customer tạo course → "Preview" trước khi publish
- URL: https://preview-abc-academy.kiteclass.com
- Mục đích: QA/testing environment
- Audience: CENTER_ADMIN
```

**Option D: Marketing Site Builder**
```
Tool để customer tự build landing page:
- Drag-and-drop page builder
- Templates: Hero + Features + Pricing + Contact
- Uses AI-generated assets
- Publish to subdomain
- Mục đích: Marketing automation
- Audience: CENTER_OWNER building their site
```

**Option E: Something Else**
```
(Vui lòng mô tả chi tiết)
```

**Vui lòng chọn và mô tả chi tiết:**
- [x] Option A: Instance marketing landing page
- [ ] Option B: Live demo system
- [ ] Option C: Staging/preview environment
- [ ] Option D: Marketing site builder
- [ ] Option E: _____________________

**Answer:** Option A - Public Marketing Landing Page (Tự động tạo)

**Chi Tiết Giải Pháp:**

Mỗi KiteClass instance có một website marketing công khai, tự động tạo từ:
- **AI branding assets** (hero banner, logo, colors - từ PART 2)
- **Instance data** (tên trung tâm, mô tả, liên hệ)
- **Course catalog** (danh sách khóa học công khai)
- **Teacher profiles** (hồ sơ giảng viên)

**URL Structure:**
```
https://abc-academy.kiteclass.com/          → Landing page (public)
https://abc-academy.kiteclass.com/courses   → Course catalog (public)
https://abc-academy.kiteclass.com/courses/101 → Course details (public)
https://abc-academy.kiteclass.com/login     → Student login (auth)
https://abc-academy.kiteclass.com/dashboard → Student dashboard (auth)

PREMIUM tier:
https://abc-academy.com                     → Custom domain (public)
```

**Giá Trị Kinh Doanh:**
- ✅ +30-50% tuyển sinh qua SEO organic
- ✅ Giảm chi phí thu hút khách hàng
- ✅ Hình ảnh chuyên nghiệp
- ✅ Zero effort (tự động tạo)
- ✅ Lợi thế cạnh tranh (hầu hết LMS không có)

**Lý Do Từ Chối Các Options Khác:**
- ❌ Option B (Demo System): Giúp KiteClass bán platform, không giúp centers bán khóa học
- ❌ Option C (Staging): Chỉ internal QA, không phải marketing tool
- ❌ Option D (Page Builder): Quá phức tạp (8-12 tuần), defer V4

**Updated in:** system-architecture-v3-final.md PHẦN 6D (Preview Website)

---

### Q3.1.2: Target Audience ✅ ANSWERED
**Câu hỏi:** Ai sẽ sử dụng "Preview Website" feature?

- [x] Prospective students (chưa đăng ký học)
- [ ] Prospective customers (chưa mua KiteClass)
- [ ] Existing students (đã đăng ký)
- [ ] CENTER_ADMIN (internal use)
- [ ] Khác: _____________________

**Answer:** Prospective students (học viên tiềm năng chưa đăng ký)

**Target Audience Chi Tiết:**
- **Chính:** Học viên tiềm năng đang duyệt khóa học online
- **Phụ:** Phụ huynh nghiên cứu trường cho con, Google crawlers (SEO), Social media referrals

**Use Case:**
```
1. Student tìm "khóa học lập trình Hà Nội" trên Google
2. Click vào abc-academy.kiteclass.com (SEO organic)
3. Duyệt course catalog (public, không cần login)
4. Xem chi tiết khóa học, giảng viên, giá
5. Click "Đăng Ký Ngay" → Redirect to /login
6. Đăng ký tài khoản → Enroll → Trở thành student
```

---

### Q3.1.3: Authentication Required? ✅ ANSWERED
**Câu hỏi:** "Preview Website" có cần authentication không?

- [x] Public (không cần login)
- [ ] Guest access (tạo temporary account)
- [ ] Requires login
- [ ] Khác: _____________________

**Answer:** Public - Không cần login

**Public Routes (No Auth):**
- `/` - Landing page
- `/courses` - Course catalog
- `/courses/[id]` - Course details
- `/about` - Về trung tâm
- `/contact` - Form liên hệ

**Protected Routes (Auth Required):**
- `/enroll/[courseId]` - Enrollment form
- `/dashboard` - Student dashboard
- `/learn/[courseId]` - Course content
- `/settings` - Settings

**Conversion Flow:**
```
Guest browse public pages → Click "Đăng Ký" → Redirect /login → Register → Enroll
```

---

### Q3.1.4: Content Source ✅ ANSWERED
**Câu hỏi:** Content trên "Preview Website" lấy từ đâu?

- [x] AI-generated assets (from Part 2)
- [x] Customer manual input
- [ ] Sample/template content
- [x] Live data from instance
- [ ] Khác: _____________________

**Answer:** Kết hợp 3 nguồn - AI assets + Instance data + Live course data

**Content Source Mapping:**

| Content Type | Source | Public? |
|--------------|--------|---------|
| Hero banner, logo, colors | AI Branding (PART 2) | ✅ |
| Headlines, CTAs | AI Branding (PART 2) | ✅ |
| Tên/mô tả trung tâm | Instance data (admin input) | ✅ |
| Course titles, descriptions, pricing | Course API (live data) | ✅ |
| Teacher names, bios | Teacher API (live data) | ✅ |
| Lesson content | Course API | ❌ (auth required) |
| Student data | Student API | ❌ (private) |

**Data Sync:** Real-time với ISR (revalidate mỗi 1 giờ)

---

### Q3.1.5: Technical Stack ✅ ANSWERED
**Câu hỏi:** "Preview Website" build bằng công nghệ gì?

- [x] Next.js static export (same codebase as main frontend)
- [ ] Separate marketing site builder
- [ ] WordPress/CMS integration
- [ ] Custom page builder
- [ ] Khác: _____________________

**Answer:** Next.js 14+ App Router (cùng codebase với main frontend)

**Tech Stack Chi Tiết:**

**Frontend:**
- Next.js 14+ App Router
- Server Components (SSR cho SEO)
- ISR (Incremental Static Regeneration - revalidate 1h)
- Tailwind CSS

**Backend APIs:**
```
GET /api/public/instance/:id/config      → Instance metadata
GET /api/public/instance/:id/branding    → AI branding assets
GET /api/public/instance/:id/courses     → Course catalog
GET /api/public/courses/:id              → Course details
GET /api/public/instance/:id/instructors → Teacher profiles
POST /api/public/contact                 → Contact form
```

**SEO:**
- Next.js Metadata API
- Structured data (Course schema - schema.org/Course)
- Sitemap.xml generation
- robots.txt

**Performance:**
- ISR: Rebuild mỗi 1 giờ
- CDN caching (Cloudflare)
- Image optimization (next/image)
- Target: Lighthouse 90+, FCP <1.5s

---

### Q3.1.6: Customization Level ✅ ANSWERED
**Câu hỏi:** Customer có customize "Preview Website" được không?

**If yes, what can be customized?**
- [x] Text content (headlines, descriptions) - via AI branding + manual override
- [x] Images (upload custom images) - via AI branding system
- [ ] Layout (reorder sections) - ❌ Fixed MVP
- [x] Theme colors - via AI branding
- [x] Domain name - PREMIUM tier only
- [ ] SEO meta tags - ❌ Auto-generated
- [ ] Nothing (fully auto-generated)

**Answer:** Limited customization qua AI Branding System (PART 2)

**Customer CÓ THỂ Tùy Chỉnh (MVP):**
- ✅ Tên, mô tả, liên hệ trung tâm (admin input)
- ✅ Course titles, descriptions, pricing (course management)
- ✅ Teacher names, bios, photos (teacher management)
- ✅ AI branding assets (upload logo → re-generate)
- ✅ Logo position, colors (Manual Override từ PART 2)
- ✅ Text content (Manual Override từ PART 2)
- ✅ Custom domain (PREMIUM tier)

**Customer KHÔNG THỂ Tùy Chỉnh (MVP):**
- ❌ Page layout/structure (templates cố định)
- ❌ Section order (fixed: Hero → About → Courses → Contact)
- ❌ Custom HTML/CSS
- ❌ Additional pages (blog, resources)
- ❌ SEO meta tags (auto-generated from branding)

**Rationale:**
- Đơn giản hóa implementation (không cần page builder)
- Maintain design quality (tránh sites "xấu")
- Faster time-to-market (2 tuần vs 8-12 tuần với builder)
- Future V4: Thêm page builder nếu có nhu cầu

---

### Q3.1.7: Relationship with Main Instance ✅ ANSWERED
**Câu hỏi:** "Preview Website" có tích hợp với main KiteClass instance không?

**Example scenarios:**

**Scenario 1: Student Registration**
```
Prospective student visits Preview Website
→ Sees course catalog
→ Clicks "Đăng ký học"
→ ??? (What happens?)
```

**Options:**
- [x] Redirect to main instance login/register page
- [ ] Inline registration form on Preview Website
- [ ] Contact form (admin follow up manually)
- [ ] No registration capability

**Answer:** Redirect to main instance /login page

**Student Registration Flow:**
```
1. Guest clicks "Đăng Ký Ngay" trên course card
2. Redirect to: /login?redirect=/enroll/[courseId]
3. Guest registers (Zalo OTP hoặc email)
4. Tạo tài khoản → Auto-login
5. Redirect to: /enroll/[courseId] (authenticated)
6. Enrollment form → Payment (nếu paid course)
7. Success → Redirect to /dashboard/courses/[courseId]
```

**Scenario 2: Course Information**
```
Preview Website hiển thị course catalog
→ Data sync từ main instance hay static content?
```

**Options:**
- [x] Real-time sync (API call to main instance)
- [x] Periodic sync (every 1 hour) - via ISR
- [ ] Manual publish (admin click "Update Preview")
- [ ] Static content (not synced)

**Answer:** Real-time sync với ISR (Best of both worlds)

**Data Sync Strategy:**

**ISR (Incremental Static Regeneration):**
```typescript
// app/(public)/page.tsx
export const revalidate = 3600 // Revalidate mỗi 1 giờ

// app/(public)/courses/page.tsx
export const revalidate = 1800 // Revalidate mỗi 30 phút
```

**How It Works:**
```
1. First visitor: Server fetch fresh data (~200ms)
2. Next 1 hour: Serve cached static page (0ms)
3. After 1 hour: Background revalidation
4. Updated page ready for next visitor
```

**Benefits:**
- ✅ Always fresh data (revalidate định kỳ)
- ✅ Fast loading (cached static pages)
- ✅ No manual sync (tự động)
- ✅ Scalable (CDN-cached)

**API Calls:**
```typescript
async function fetchPublicCourses(instanceId: string) {
  const response = await fetch(
    `https://api.kiteclass.com/v1/public/instance/${instanceId}/courses`,
    { next: { revalidate: 1800 } } // Cache 30 min
  )
  return response.json()
}
```

---

## 3.2. Implementation Priority

### Q3.2.1: MVP Scope ✅ ANSWERED
**Câu hỏi:** "Preview Website" feature có trong MVP scope không?

- [x] CÓ - Critical feature, must have in V3
- [ ] KHÔNG - Nice to have, có thể defer to V3.5
- [ ] KHÔNG CHẮC - Cần discuss thêm

**Answer:** CÓ - Critical feature cho MVP V3

**Nếu CÓ trong MVP:**
**Which PR should include this?**
- [x] PR 3.4 (Public Routes & Landing Pages)
- [ ] PR 3.8 (Additional Features)
- [ ] Separate PR after MVP
- [ ] Khác: _____________________

**Answer:** PR 3.4 - Chia thành 3 sub-PRs

**Implementation Plan:**

**PR 3.4a: Backend Public APIs (3 ngày)**
- 6 public endpoints (no auth)
- PublicCourse DTO (filter private fields)
- Rate limiting (100 req/min per IP)
- Tests (unit, integration, security)

**PR 3.4b: Frontend Public Routes (5 ngày)**
- (public) route group
- Landing page + Course catalog + Course details
- SEO optimization (metadata, structured data, sitemap)
- Mobile responsive
- Tests (component, E2E, SEO, a11y)

**PR 3.4c: Integration & Polish (2 ngày)**
- Custom domain routing (PREMIUM)
- Performance optimization (ISR, CDN, images)
- Analytics integration (GA4, conversion tracking)
- Contact form + spam protection
- Edge cases (empty states, expired courses)

**Timeline:** 2 tuần total

**Dependencies:**
- ✅ PR 3.2: Core Infrastructure (Feature Detection)
- ✅ PR 3.3: Providers & Layout
- ✅ AI Branding System APIs (PART 2)

**Updated in:** kiteclass-implementation-plan.md (PR 3.4 expanded)

---

# PART 4: GUEST USER & TRIAL SYSTEM

## 🎯 CLARIFICATION: B2B Model - Owner-Centric Trial & Sales

**Key Principles:**
1. **Trial chỉ cho OWNER** - Khi đăng ký gói tạo instance, sau launch xong mới trial expand services/features
2. **Non-owners liên hệ OWNER** - ADMIN/TEACHER/STUDENT muốn trial → Liên hệ OWNER
3. **Guest không auto-enroll** - Guest muốn đăng ký học → Liên hệ OWNER (KiteClass không đảm nhận sales)
4. **OWNER làm sales** - Nghiệp vụ tư vấn, bán khóa học do OWNER đảm nhận
5. **Contact info prominently displayed** - Facebook, Messenger, Zalo để guest liên hệ OWNER

## 4.1. Trial System Design

### Q4.1.1: Trial Duration ✅ ANSWERED
**Câu hỏi:** Trial bao lâu?

**Landing page hiện tại: "Dùng thử miễn phí 14 ngày"**

**Confirm:**
- [x] 14 ngày (as stated)
- [ ] 7 ngày
- [ ] 30 ngày
- [ ] Khác: _____ ngày

**Answer:** 14 ngày trial cho OWNER

**Chi Tiết Trial System:**

**Trial Scope:**
```
Trial ÁP DỤNG CHO:
✅ CENTER_OWNER đăng ký gói tạo instance
✅ Trial expand services/features SAU KHI launch instance
✅ Test PREMIUM features (gamification, media, AI branding, etc.)

Trial KHÔNG ÁP DỤNG CHO:
❌ Guests browsing public website
❌ Students đăng ký học khóa
❌ ADMIN/TEACHER muốn trial features → Phải liên hệ OWNER
```

**Trial Timeline:**
```
Day 0: OWNER đăng ký instance tại KiteHub
  ↓
Launch instance với BASIC tier (default)
  ↓
Day 1-14: Trial expand features
  - OWNER enable/disable expand services
  - Test ENGAGEMENT pack (gamification, forum, parent portal)
  - Test MEDIA pack (video, live streaming)
  - Test PREMIUM features (AI branding, custom domain, etc.)
  ↓
Day 14 23:59:59: Trial expires
  ↓
OWNER chọn gói: BASIC, STANDARD, PREMIUM + add-ons
```

**Non-Owner Access:**
```
ADMIN/TEACHER/STUDENT muốn trial feature:
1. Click locked feature (e.g., "Gamification")
2. Show modal: "Tính năng này cần gói STANDARD"
3. Display OWNER contact info:
   - "Liên hệ [Owner Name]"
   - 📧 owner@example.com
   - 📱 0123456789
   - 💬 Facebook/Messenger link
4. Option: "Gửi yêu cầu" → Email notification đến OWNER
```

---

### Q4.1.2: Trial Tier ✅ ANSWERED
**Câu hỏi:** Trial account tương đương tier nào?

**Option A: Trial = Premium Tier**
```
Customer gets full PREMIUM features trong trial
Mục đích: Show best features để convert
Sau trial: Downgrade to selected tier
```

**Option B: Trial = Standard Tier**
```
Customer gets STANDARD features
Mục đích: Balanced trial experience
Upsell Premium features sau khi convert
```

**Option C: Trial = Custom Tier**
```
Trial có feature set riêng:
- Tất cả features enabled
- Nhưng có limits:
  - Max 20 students
  - Max 3 courses
  - Max 1GB storage
```

**Vui lòng chọn:**
- [x] Option C: Custom với limits (Best Practice)
- [ ] Option A: Full PREMIUM
- [ ] Option B: STANDARD tier
- [ ] Khác: _____________________

**Answer:** Trial = Custom Tier với limits (BASIC + Expand Features)

**Trial Tier Specification:**

**Base Tier: BASIC**
```
Subscription: FREE (trial)
Billing: 0đ/tháng (during trial)
Limits:
- Max students: 50
- Max courses: 10
- Max teachers: 5
- Video storage: 0GB (chưa enable MEDIA pack)
```

**Expand Features Available for Trial:**
```
✅ ENGAGEMENT Pack (+300k/tháng - Trial FREE):
   - Gamification (badges, leaderboards, points)
   - Forum (discussions, Q&A)
   - Parent Portal (progress tracking, messaging)

✅ MEDIA Pack (+500k/tháng - Trial FREE):
   - Video Upload (5GB storage during trial)
   - Live Streaming (1 concurrent stream)
   - Video Analytics

✅ PREMIUM Features (Trial FREE):
   - AI Branding (10 generations during trial)
   - Custom Domain (test only, not publish)
   - Priority Support
```

**Rationale:**
- ✅ OWNER có thể test TẤT CẢ features
- ✅ Limits đủ để explore (50 students, 10 courses)
- ✅ Không overwhelm với unlimited (tránh abuse)
- ✅ Clear upgrade path sau trial

**Sau Trial:**
```
Day 14 23:59:59 → Trial expires

OWNER chọn gói:
Option 1: BASIC (500k/tháng, ≤50 students)
Option 2: STANDARD (1tr/tháng, ≤200 students) + add-ons
Option 3: PREMIUM (2tr/tháng, unlimited)

Expand features disabled nếu không subscribe:
- Gamification → Locked
- Forum → Read-only
- Video Upload → Blocked
- AI Branding → Disabled
```

---

### Q4.1.3: Trial Signup Requirements ✅ ANSWERED
**Câu hỏi:** Yêu cầu gì để signup trial?

**Current proposal:**
- Organization name
- Name
- Email
- Phone

**Payment info required?**
- [ ] CÓ - Phải nhập credit card (không charge)
- [x] KHÔNG - Không cần payment info

**Answer:** Không cần payment info (Reduce friction, tăng conversion)

**Phone verification?**
- [x] CÓ - Zalo OTP verification (Best for Vietnam market)
- [ ] CÓ - SMS OTP
- [ ] KHÔNG - Chỉ cần email verification

**Answer:** Zalo OTP verification (Vietnam market standard)

**Email verification?**
- [x] CÓ - Gửi link verify email trước khi activate trial
- [ ] KHÔNG - Activate ngay sau signup

**Answer:** Email verification bắt buộc (Prevent spam, ensure valid contact)

**Additional questions:**
- [x] Industry type (giáo dục, corporate training, etc.)
- [x] Company size (nhỏ hơn 50, 50-200, >200 học viên)
- [x] How did you hear about us?
- [ ] Khác: _____________________

**Answer:** Thu thập 3 additional questions (Sales intelligence)

**Trial Signup Flow:**

```
Step 1: Landing Page (kiteclass.com)
  → Click "Dùng thử miễn phí 14 ngày"

Step 2: Registration Form
  ┌─────────────────────────────────────────┐
  │  Đăng Ký Trial KiteClass               │
  ├─────────────────────────────────────────┤
  │  Organization Name: [____________]      │
  │  Tên của bạn: [____________]           │
  │  Email: [____________]                  │
  │  Số điện thoại: [____________]         │
  │                                         │
  │  Loại hình: [v] Trung tâm giáo dục    │
  │             [ ] Đào tạo doanh nghiệp   │
  │             [ ] Trường học             │
  │                                         │
  │  Quy mô: [v] < 50 học viên            │
  │          [ ] 50-200 học viên           │
  │          [ ] > 200 học viên            │
  │                                         │
  │  Biết KiteClass qua: [v] Google       │
  │                      [ ] Facebook      │
  │                      [ ] Bạn bè        │
  │                                         │
  │  [Đăng Ký Ngay]                        │
  └─────────────────────────────────────────┘

Step 3: Zalo OTP Verification
  → Gửi OTP qua Zalo đến số điện thoại
  → Nhập mã OTP (6 digits)
  → Verify

Step 4: Email Verification
  → Gửi email với link verify
  → Click link → Activate trial

Step 5: Instance Provisioning
  → KiteHub tạo instance
  → URL: {organization-slug}.kiteclass.com
  → Deploy 3 core services (User, Core, Frontend)
  → Status: TRIAL (14 days)

Step 6: Welcome Email
  → Login credentials
  → Quick start guide
  → Trial timeline (Day 1, 7, 13, 14)
  → Support contact
```

**Required Fields:**
- ✅ Organization name (tên trung tâm)
- ✅ Owner name
- ✅ Email (verify)
- ✅ Phone (Zalo OTP)
- ✅ Industry type (dropdown)
- ✅ Company size (dropdown)
- ✅ Referral source (dropdown)

**NOT Required:**
- ❌ Payment info (credit card)
- ❌ Address (không cần ngay)
- ❌ Tax code (không cần cho trial)

---

### Q4.1.4: Trial Expiration Behavior ✅ ANSWERED
**Câu hỏi:** Khi trial hết hạn, điều gì xảy ra?

**Day 14 23:59:59 → Day 15 00:00:00**

**Option A: Immediate Lock**
```
Trial expires → Instance bị lock ngay
- Customer không login được
- Hiện message: "Trial đã hết, vui lòng nâng cấp"
- Data retained nhưng không access được
```

**Option B: Grace Period**
```
Trial expires → 3 ngày grace period
- Customer vẫn login được nhưng có banner cảnh báo
- Chức năng CRUD bị disable (read-only mode)
- Day 17: Lock hoàn toàn
```

**Option C: Auto Downgrade**
```
Trial expires → Auto downgrade to FREE tier
- Limited features
- Data retained
- Customer có thể tiếp tục dùng (limited)
```

**Vui lòng chọn:**
- [ ] Option A: Lock ngay
- [x] Option B: 3-day grace period (Best Practice)
- [ ] Option C: Auto downgrade to FREE
- [ ] Khác: _____________________

**Answer:** Option B - 3-day grace period với read-only mode

**Trial Expiration Timeline:**

```
Day 1-10: Early trial
  ✅ Full access
  ✅ Soft banner: "Bạn còn X ngày trial"
  ✅ Explore all features

Day 11-13: Late trial
  ⚠️ Warning banner: "Còn 3 ngày trial, nâng cấp ngay"
  ⚠️ Email reminder (Day 11, 13)
  ✅ Full access vẫn còn

Day 14 (Last day):
  🔴 Urgent banner: "HÔM NAY là ngày cuối trial"
  🔴 Email: "Last chance to upgrade"
  🔴 In-app modal khi login
  ✅ Full access vẫn còn

Day 14 23:59:59 → Trial expires

Day 15-17 (Grace Period):
  📖 Read-only mode:
     - Login OK
     - View data OK (students, courses, etc.)
     - CRUD disabled (cannot add/edit/delete)
     - Banner: "Trial đã hết. Còn X ngày grace period"
  🔒 Expand features locked:
     - Gamification → Disabled
     - Forum → Read-only
     - Video Upload → Blocked
  📧 Daily email reminder

Day 18 (Grace period ends):
  🔒 Instance LOCKED
     - Cannot login
     - Show message: "Trial & grace period đã hết"
     - "Nâng cấp ngay" button → KiteHub billing
  💾 Data retained (90 days)
```

**Data retention sau trial:**
- [ ] Keep forever (customer có thể comeback anytime)
- [ ] Keep 30 ngày sau trial expiration
- [x] Keep 90 ngày
- [ ] Delete ngay (không retention)

**Answer:** Keep 90 ngày (Best Practice)

**Data Retention Policy:**
```
Day 18-107 (90 days after lock):
  💾 Data retained on backup storage
  💾 OWNER có thể upgrade → Restore ngay
  💾 No charges during locked period

Day 108:
  ⚠️ Email warning: "Còn 7 ngày data sẽ bị xóa"
  ⚠️ Option: "Archive & download data" button

Day 115:
  🗑️ Permanent deletion
  🗑️ Instance deprovisioned
  ❌ Cannot recover
```

**Rationale:**
- ✅ Grace period: Reduce churn, give time to decide
- ✅ Read-only: OWNER vẫn access data (không mất)
- ✅ 90-day retention: Industry standard (Salesforce, HubSpot)
- ✅ Comeback anytime: OWNER có thể upgrade trong 90 ngày

---

### Q4.1.5: Trial-to-Paid Conversion ✅ ANSWERED
**Câu hỏi:** Conversion flow từ trial sang paid như thế nào?

**In-app conversion prompts:**

**Day 1-10 (early trial):**
- [ ] No prompts (để customer explore)
- [x] Soft banner: "Bạn còn X ngày trial"
- [x] Upgrade CTA ở footer

**Day 11-13 (late trial):**
- [x] Warning banner: "Còn 3 ngày, nâng cấp ngay"
- [x] Email reminder
- [x] In-app notification

**Day 14 (last day):**
- [x] Urgent banner: "Hôm nay là ngày cuối"
- [x] Email: "Last chance to upgrade"
- [ ] Phone call from sales (high-touch) - ❌ Too expensive

**After expiration:**
- [x] Lock instance + email với upgrade link
- [x] Allow grace period (see Q4.1.4)

**Answer:** Multi-touch conversion strategy (banner + email + modal)

**Conversion incentives:**
- [x] Discount: "Upgrade hôm nay giảm 20%"
- [ ] Extended trial: "Thêm 7 ngày nếu nâng cấp trong 24h"
- [ ] No incentive (standard pricing)

**Answer:** Early-bird discount 20% cho upgrade trong 3 ngày đầu

**Conversion Strategy:**

```
Day 1: Welcome email
  → Quick start guide
  → "Bạn còn 14 ngày trial"

Day 3: Feature highlight email
  → "Đã thử AI Branding chưa?"
  → Link to tutorial

Day 7: Mid-trial check-in
  → Email: "Còn 7 ngày trial"
  → Survey: "Trải nghiệm thế nào?"
  → Offer: "Upgrade ngay giảm 20%"

Day 11: Late-trial warning
  → Banner: ⚠️ "Còn 3 ngày trial"
  → Email: "Còn 3 ngày, nâng cấp ngay"
  → In-app modal khi login

Day 13: Urgent reminder
  → Banner: 🔴 "Còn 1 ngày trial"
  → Email: "Last chance!"
  → Push notification (nếu enabled)

Day 14: Final day
  → Banner: 🔴 "HÔM NAY là ngày cuối"
  → Email: "Trial ends tonight!"
  → Modal popup: "Nâng cấp ngay giảm 20%"

Day 15-17: Grace period
  → Read-only mode
  → Banner: "Trial đã hết. Nâng cấp để tiếp tục"
  → Daily email reminder

Day 18+: Locked
  → Cannot login
  → Email: "Instance locked. Upgrade to restore"
```

**Conversion Incentives:**
```
Early-bird discount (Day 1-10):
  → Upgrade trong 10 ngày đầu: Giảm 20% tháng đầu
  → VD: STANDARD 1tr → 800k (tháng đầu)

Standard pricing (Day 11+):
  → No discount
  → Full price
```

**Upgrade Flow:**
```
1. OWNER clicks "Nâng cấp ngay"
2. Redirect to KiteHub billing page
3. Select tier: BASIC, STANDARD, PREMIUM
4. Select add-ons: ENGAGEMENT, MEDIA
5. Payment: VNPay/MoMo
6. Success → Instance activated
7. Email confirmation
```

---

### Q4.1.6: Multiple Trial Prevention ✅ ANSWERED
**Câu hỏi:** Ngăn chặn customer tạo nhiều trial accounts như thế nào?

**Detection methods:**
- [x] Email address (1 email = 1 trial)
- [x] Phone number (1 phone = 1 trial)
- [ ] Credit card (nếu require CC) - N/A (không require CC)
- [ ] IP address - ❌ Too restrictive (shared office IPs)
- [ ] Device fingerprinting - ❌ Complex, privacy concerns
- [ ] Không ngăn chặn (allow multiple trials)

**Answer:** Email + Phone number (2-factor prevention)

**Enforcement:**
- [x] Hard block: "Email này đã dùng trial"
- [ ] Soft warning: "Bạn có muốn extend trial thay vì tạo mới?"
- [x] Allow but notify sales team

**Answer:** Hard block + notify sales (for legitimate cases)

**Multiple Trial Prevention Strategy:**

```java
// Trial eligibility check
@Service
public class TrialEligibilityService {

    public TrialEligibility checkEligibility(String email, String phone) {
        // Check email
        boolean emailUsed = trialRepo.existsByEmail(email);

        // Check phone
        boolean phoneUsed = trialRepo.existsByPhone(phone);

        if (emailUsed || phoneUsed) {
            // Log attempt
            auditLog.warn("Duplicate trial attempt", email, phone);

            // Notify sales team
            salesNotificationService.notifyDuplicateTrial(email, phone);

            return TrialEligibility.builder()
                .eligible(false)
                .reason("Email hoặc số điện thoại đã được sử dụng cho trial")
                .existingTrialDate(getExistingTrialDate(email, phone))
                .build();
        }

        return TrialEligibility.eligible();
    }
}
```

**UI Behavior:**
```
User submits trial signup form
  ↓
Backend checks email + phone
  ↓
If duplicate detected:
  ┌─────────────────────────────────────────┐
  │  ⚠️ Email hoặc SĐT đã dùng trial       │
  ├─────────────────────────────────────────┤
  │  Email hoặc số điện thoại này đã được  │
  │  sử dụng để đăng ký trial trước đó.   │
  │                                         │
  │  Ngày đăng ký: 2026-01-15              │
  │                                         │
  │  Nếu bạn cần hỗ trợ, vui lòng liên hệ: │
  │  📧 support@kiteclass.com              │
  │  📱 1900-xxxx                           │
  │                                         │
  │  [Liên hệ Sales]  [Đóng]               │
  └─────────────────────────────────────────┘

Sales team receives notification:
  → Email: "Duplicate trial attempt"
  → Details: Email, phone, timestamp
  → Action: Contact customer (legit case vs abuse)
```

**Legitimate Cases (Sales Override):**
```
Scenario 1: Company rebrand
  - Trung tâm đổi tên, muốn trial lại
  - Sales team: Manual approve

Scenario 2: Different organization
  - Cùng person, khác organization
  - Sales team: Assess & approve

Scenario 3: Previous trial failed
  - Technical issues trong trial
  - Sales team: Reset trial
```

**Abuse Cases (Block):**
```
Scenario 1: Serial trialer
  - Cùng person, cùng org, trial nhiều lần
  - Action: Hard block

Scenario 2: Competitor research
  - Nhiều trial trong thời gian ngắn
  - Action: Block + investigate
```

---

## 4.2. Guest User Access

### 🎯 CLARIFICATION: Admin-Controlled Public Resources + Owner-Led Sales

**Key Principles:**
1. **Admin quản lý public resources** - Backend service để ADMIN control khóa học/lớp nào public
2. **Guest không auto-enroll** - Guest muốn đăng ký → Liên hệ OWNER (không tự đăng ký)
3. **OWNER làm sales** - Tư vấn, xác nhận, enroll thủ công
4. **Contact info prominent** - Display Facebook, Messenger, Zalo để guest liên hệ OWNER
5. **SEO optimization** - Public catalog cho SEO, nhưng conversion qua OWNER

### Q4.2.1: Public Course Catalog ✅ ANSWERED
**Câu hỏi:** Mỗi KiteClass instance có public course catalog không?

**Scenario:**
```
Guest user (chưa đăng ký) vào https://abc-academy.kiteclass.com
→ Có xem được danh sách khóa học không?
```

**Option A: Full Public Catalog**
```
- Guest xem được tất cả courses
- Course details (description, schedule, price)
- Teacher info
- Testimonials
- Không thấy nội dung bài học (cần login)
```

**Option B: Teaser Only**
```
- Guest chỉ thấy 3-5 courses nổi bật
- Basic info only (title, image)
- Click vào → Require login
```

**Option C: No Public Catalog**
```
- Tất cả course info yêu cầu login
- Landing page chỉ có about, contact, generic info
```

**Vui lòng chọn:**
- [x] Option A: Full public catalog (SEO-friendly) + Admin-controlled
- [ ] Option B: Teaser (limited preview)
- [ ] Option C: No public access
- [ ] Khác: _____________________

**Answer:** Option A - Full public catalog + Admin control visibility

**Admin-Controlled Public Resources:**

```java
// Course entity - Admin controls public visibility
@Entity
public class Course {
    @Id
    private String id;

    private String title;
    private String description;

    // Admin-controlled visibility
    @Column(nullable = false)
    private PublicVisibility publicVisibility = PublicVisibility.PRIVATE;

    public enum PublicVisibility {
        PRIVATE,     // Guest không thấy
        PUBLIC       // Guest thấy được (trong catalog)
    }
}

// Admin UI to control visibility
@RestController
public class CourseAdminController {

    @PatchMapping("/api/v1/admin/courses/{id}/visibility")
    @PreAuthorize("hasRole('CENTER_ADMIN')")
    public ResponseEntity<Void> updateVisibility(
        @PathVariable String id,
        @RequestBody PublicVisibility visibility
    ) {
        courseService.updatePublicVisibility(id, visibility);
        return ResponseEntity.ok().build();
    }
}
```

**Public API - Chỉ trả về courses với visibility=PUBLIC:**

```java
@GetMapping("/api/v1/public/instance/{instanceId}/courses")
public ResponseEntity<List<PublicCourseDTO>> getPublicCourses(
    @PathVariable String instanceId
) {
    List<Course> courses = courseRepo.findByInstanceIdAndPublicVisibility(
        instanceId,
        PublicVisibility.PUBLIC  // Chỉ PUBLIC courses
    );

    return ResponseEntity.ok(toPublicDTO(courses));
}
```

**Nếu Option A or B:**
**Course details nào public?**
- [x] Course name
- [x] Description
- [x] Price
- [x] Schedule (start date, duration)
- [x] Teacher name & bio
- [x] Syllabus/curriculum
- [ ] Student count - ❌ Private
- [ ] Reviews/ratings - ❌ V4 feature (defer)
- [x] **Contact info** (Facebook, Messenger, Zalo) ← **KEY**

**Answer:** Tất cả course details EXCEPT student count, reviews

**Contact Information Display:**

```typescript
// Course Details Page - Contact OWNER section
<CourseDetailsPage>
  <CourseHeader title={course.title} price={course.price} />
  <CourseSyllabus curriculum={course.syllabus} />
  <InstructorBio instructor={course.instructor} />

  {/* KEY: Contact OWNER Section */}
  <ContactOwnerSection>
    <h3>Quan tâm khóa học này?</h3>
    <p>Liên hệ trực tiếp với trung tâm để đăng ký:</p>

    <ContactMethods>
      <ContactButton icon="phone" href={`tel:${owner.phone}`}>
        {owner.phone}
      </ContactButton>

      <ContactButton icon="facebook" href={owner.facebookUrl}>
        Nhắn tin Facebook
      </ContactButton>

      <ContactButton icon="messenger" href={owner.messengerUrl}>
        Chat Messenger
      </ContactButton>

      <ContactButton icon="zalo" href={owner.zaloUrl}>
        Chat Zalo
      </ContactButton>

      <ContactButton icon="email" href={`mailto:${owner.email}`}>
        Gửi Email
      </ContactButton>
    </ContactMethods>

    <OwnerInfo>
      <Avatar src={owner.avatar} />
      <div>
        <p><strong>{owner.name}</strong></p>
        <p>Giám đốc - {instance.name}</p>
      </div>
    </OwnerInfo>
  </ContactOwnerSection>
</CourseDetailsPage>
```

**Rationale:**
- ✅ SEO benefits: Full catalog public → Google index
- ✅ OWNER control: ADMIN chọn courses nào public
- ✅ Lead generation: Guest contact OWNER → OWNER qualify & close
- ✅ Human touch: Personal sales process (trust, customization)
- ✅ No auto-enroll: Prevent fraud, ensure payment
- ✅ Prominent contact: Facebook/Zalo are primary in Vietnam

---

### Q4.2.2: Course Preview/Demo Lessons
**Câu hỏi:** Guest có thể "học thử" course không?

**Option A: Demo Lessons**
```
Mỗi course có 1-2 bài học demo (public)
Guest xem được video, slides, materials
Mục đích: Taste before buying
```

**Option B: No Preview**
```
Guest chỉ thấy course description
Muốn xem content → Phải enroll (paid or trial)
```

**Option C: Limited Access**
```
Guest xem được:
- Video thumbnail/trailer (2 phút)
- Sample materials (PDF first page)
- Quiz preview (no answers)
```

**Vui lòng chọn:**
- [ ] Option A: Full demo lessons
- [ ] Option B: No preview
- [ ] Option C: Limited preview
- [ ] Khác: _____________________

---

### Q4.2.3: Guest Self-Registration
**Câu hỏi:** Guest có thể tự đăng ký làm STUDENT không?

**Current system:** Parent self-registration via Zalo OTP (documented)

**For Students:**

**Option A: Open Registration**
```
Guest click "Đăng ký học"
→ Self-registration form
→ OTP verification
→ Account created with STUDENT role
→ Enroll in courses
```

**Option B: Invitation-Only**
```
Guest không tự register được
Admin/Teacher phải invite (gửi link/QR)
Guest click link → Verify → Account created
```

**Option C: Request-Based**
```
Guest submit registration request
Admin review & approve
Guest nhận email → Activate account
```

**Vui lòng chọn:**
- [ ] Option A: Open self-registration
- [ ] Option B: Invitation-only
- [ ] Option C: Request & approve
- [ ] Khác: _____________________

**Verification method:**
- [ ] Zalo OTP (như Parent)
- [ ] SMS OTP
- [ ] Email verification
- [ ] No verification

**Enrollment process:**
- [ ] Self-enroll in public courses (free or paid)
- [ ] Must contact admin to enroll
- [ ] Add to cart → Payment → Auto enroll

---

### Q4.2.4: Guest Session Tracking
**Câu hỏi:** Track guest behavior để optimize conversion không?

**Analytics to collect:**
- [ ] Pages visited (landing, courses, pricing)
- [ ] Time on site
- [ ] Courses viewed
- [ ] CTA clicks (Đăng ký học, Liên hệ)
- [ ] Form abandonment
- [ ] Referral source

**GDPR/Privacy compliance:**
- [ ] Show cookie consent banner
- [ ] Anonymous tracking (no PII)
- [ ] Opt-in tracking
- [ ] No tracking

**Retention:**
- [ ] Guest session data retained _____ days
- [ ] Delete after guest converts to user
- [ ] Never delete (analytics)

---

### Q4.2.5: Marketing Content for Guests
**Câu hỏi:** Guest có nhận marketing content không?

**Channels:**

**On-site:**
- [ ] Pop-up: "Nhập email để nhận tài liệu học thử"
- [ ] Banner: "Đăng ký nhận tin về khóa học mới"
- [ ] Chatbot: "Có thể giúp gì cho bạn?"

**Off-site (after guest visits):**
- [ ] Email marketing (nếu guest submit email)
- [ ] SMS marketing (nếu guest submit phone)
- [ ] Remarketing ads (Facebook, Google)
- [ ] Zalo marketing messages

**Opt-in required?**
- [ ] CÓ - Explicit opt-in checkbox
- [ ] KHÔNG - Auto opt-in (có opt-out link)

---

### Q4.2.6: Guest-to-Trial Conversion
**Câu hỏi:** Guest có thể activate trial không? (vs paid enrollment)

**Scenario:**
```
Guest vào site, xem courses, muốn thử
→ Click "Học thử miễn phí"
→ ???
```

**Option A: Direct Trial Enrollment**
```
Guest register → Tạo account với TRIAL status
Được học 1-2 courses trong 7-14 ngày
Trial expires → Must pay to continue
```

**Option B: No Guest Trial**
```
Trial chỉ dành cho center owners (business customers)
Guest students phải pay hoặc wait for invitation
```

**Option C: Limited Guest Trial**
```
Guest register → Temporary account (3 days)
Access 1 demo course only
Conversion: Upgrade to paid student
```

**Vui lòng chọn:**
- [ ] Option A: Guest có trial enrollment
- [ ] Option B: No guest trial (trial chỉ cho businesses)
- [ ] Option C: Limited guest trial (3 days)
- [ ] Khác: _____________________

---

## 4.5. V4.1 LMS + Marketing Modules ⭐ NEW

### Q4.5.1: Trial Lesson Access Control
**Câu hỏi:** Guest có thể xem lesson nào mà không cần đăng nhập?

**Proposed Design:**
```
Course Structure:
  Module 1: Introduction (FREE)
    - Lesson 1.1: What is Java? (isTrial = TRUE) ✅ Guest access
    - Lesson 1.2: Installing JDK (isTrial = FALSE) 🔒 Enrollment required
    - Lesson 1.3: First Program (isTrial = FALSE) 🔒 Enrollment required
  Module 2: OOP Concepts
    - Lesson 2.1: Classes & Objects (isTrial = TRUE) ✅ Guest access
    - ...
```

**Business Rules:**
- BR-LMS-001: Guest can view lessons where `isTrial = TRUE` without login
- BR-LMS-002: Student must have active enrollment to access `isTrial = FALSE` lessons
- BR-LMS-003: Lesson progress only tracked for authenticated users

**Vui lòng confirm:**
- [ ] ✅ OK, implement theo thiết kế trên
- [ ] ❌ Thay đổi: _____________________

**Response trang Lesson cho Guest:**
```json
{
  "lessonId": 123,
  "title": "Lesson 1.1: What is Java?",
  "isTrial": true,
  "content": "...", // Full content for trial
  "videoUrl": "https://...",
  "resources": [
    {"type": "PDF", "title": "Slides", "url": "..."}
  ],
  "nextLesson": {
    "id": 124,
    "title": "Lesson 1.2: Installing JDK",
    "isTrial": false,
    "locked": true, // Show lock icon
    "ctaText": "Đăng ký học để tiếp tục"
  }
}
```

**Confirm response format:**
- [ ] ✅ OK
- [ ] ❌ Cần điều chỉnh: _____________________

---

### Q4.5.2: Landing Page Customization Scope
**Câu hỏi:** Landing page của mỗi tenant có thể customize những gì?

**Proposed Customizable Fields:**
```
Hero Section:
  - heroTitle: "Học lập trình cùng Thầy Kiệt"
  - heroSubtitle: "Master Java in 3 months"
  - heroImageUrl: "https://..."
  - logoUrl: "https://..."

About Teacher:
  - teacherBio: "20 years of experience..."
  - teacherPhotoUrl: "https://..."

Branding:
  - primaryColor: "#3B82F6" (blue)
  - secondaryColor: "#10B981" (green)
  - tagline: "Your path to Java mastery"

Course Highlights:
  - Featured courses (manually selected by teacher)
  - Auto-display 3-5 most popular courses
```

**Vui lòng confirm:**
- [ ] ✅ OK, đủ customization
- [ ] ❌ Cần thêm: _____________________

**Landing Page URL Format:**
```
https://{tenant-slug}.kiteclass.vn           (Custom subdomain)
OR
https://kiteclass.vn/{tenant-slug}           (Shared domain)
```

**Prefer which URL format?**
- [ ] Custom subdomain (requires DNS setup)
- [ ] Shared domain with slug (simpler)
- [ ] Both options (configurable)

---

### Q4.5.3: Lead Workflow & Conversion
**Câu hỏi:** Khi guest submit contact form, workflow thế nào?

**Proposed Lead Status Workflow:**
```
NEW → CONTACTED → CONVERTED → LOST

NEW:
  - Guest submit contact form
  - Lead auto-created with status = NEW
  - Email notification sent to teacher

CONTACTED:
  - Teacher marks as "Đã liên hệ"
  - lastContactedAt timestamp recorded

CONVERTED:
  - Guest signs up as student
  - Lead linked to Student record
  - Status = CONVERTED (success metric)

LOST:
  - Lead not interested
  - Mark as LOST to clean up pipeline
```

**Contact Form Data Captured:**
```json
{
  "name": "Nguyen Van A",
  "email": "nguyenvana@gmail.com",
  "phone": "0901234567",
  "message": "Tôi muốn đăng ký học Java",
  "courseInterestId": 123, // Optional: which course interested in
  "source": "CONTACT_FORM" // or "LANDING_PAGE", "TRIAL"
}
```

**Vui lòng confirm:**
- [ ] ✅ Workflow OK
- [ ] ❌ Thay đổi: _____________________

**Email Notification to Teacher:**
- [ ] Immediate (realtime) - Send email ngay khi guest submit
- [ ] Batched (daily digest) - Gom leads trong ngày, send 1 email tổng hợp
- [ ] Configurable by teacher

---

### Q4.5.4: Learning Progress Tracking
**Câu hỏi:** Tiến độ học tập được track như thế nào?

**Proposed Design:**
```
Student completes lesson:
  → LessonProgress record created/updated
  → {
      userId: 456,
      lessonId: 123,
      completed: true,
      completedAt: "2026-02-26T10:00:00Z",
      progressPercent: 100
    }

Course Progress Calculation:
  → completedLessons / totalLessons * 100
  → E.g., 5 completed / 10 total = 50%

Display to Student:
  - Progress bar on course page
  - Checkmarks on completed lessons
  - "Continue Learning" CTA shows next lesson
  - Certificate awarded when 100% complete (future)
```

**Progress Display Options:**
- [ ] Show % only (e.g., "50% complete")
- [ ] Show % + count (e.g., "5/10 lessons completed")
- [ ] Show % + estimated time remaining (e.g., "~2 hours left")

**Vui lòng chọn:**
- [ ] Option 1: % only
- [ ] Option 2: % + count (Recommended)
- [ ] Option 3: % + estimated time
- [ ] All of the above (most info)

---

### Q4.5.5: Guest Pages SEO Strategy
**Câu hỏi:** Landing page cần SEO như thế nào để tăng organic traffic?

**Proposed SEO Features:**
```
Meta Tags (per tenant):
  <title>{heroTitle} | KiteClass</title>
  <meta name="description" content="{heroSubtitle}" />
  <meta name="keywords" content="học lập trình, Java, online course" />

Open Graph (Social Sharing):
  <meta property="og:title" content="{heroTitle}" />
  <meta property="og:description" content="{teacherBio}" />
  <meta property="og:image" content="{heroImageUrl}" />
  <meta property="og:url" content="https://kiteclass.vn/{tenant-slug}" />

Structured Data (Schema.org):
  - Course schema for each course
  - Organization schema for teacher
  - Review schema (if ratings enabled)
```

**Vui lòng confirm:**
- [ ] ✅ Implement full SEO as proposed
- [ ] ❌ Chỉ cần basic meta tags
- [ ] ❌ SEO là low priority (Phase 2)

**Sitemap Generation:**
- [ ] Auto-generate sitemap.xml for each tenant
- [ ] Manual submission to Google Search Console
- [ ] Not needed initially

---

# PART 5: INTEGRATION & DEPENDENCIES

## 5.1. Backend API Readiness

### Q5.1.1: Which APIs Need to Be Implemented First?
**Câu hỏi:** Để implement frontend PRs, cần backend APIs nào ready trước?

**For PR 3.2 (Core Infrastructure):**
- [ ] `GET /api/v1/instance/config` (feature flags)
- [ ] `GET /api/v1/instance/theme` (theme settings)
- [ ] `GET /api/v1/instance/branding` (branding settings)

**For PR 3.3 (Providers):**
- [ ] Same as PR 3.2

**For PR 3.4 (Public Routes):**
- [ ] `GET /api/v1/public/courses` (public course catalog)
- [ ] `POST /api/v1/auth/trial-signup` (trial registration)
- [ ] `POST /api/v1/auth/guest-register` (guest registration)

**For PR 3.5 (Admin Dashboard):**
- [ ] `POST /api/v1/branding/upload` (upload image for AI)
- [ ] `GET /api/v1/branding/status/:jobId` (poll generation status)
- [ ] `POST /api/v1/subscription/upgrade` (tier upgrade)

**Backend team có thể deliver APIs này theo timeline nào?**
- [ ] PR 3.2 APIs ready: _____ (date)
- [ ] PR 3.4 APIs ready: _____ (date)
- [ ] PR 3.5 APIs ready: _____ (date)

---

## 5.2. Third-Party Services

### Q5.2.1: Payment Gateway Integration
**Câu hỏi:** Payment integration priority?

**Services needed:**
- [ ] VNPay (Vietnam payment)
- [ ] MoMo wallet
- [ ] ZaloPay
- [ ] Stripe (international cards)

**Which PR should include payment?**
- [ ] PR 3.7 (Payment Integration)
- [ ] Post-MVP
- [ ] V4

**Testing environment:**
- [ ] Sandbox accounts ready: [ ] Yes [ ] No
- [ ] Test payment flow ready: [ ] Yes [ ] No

---

### Q5.2.2: Email Service
**Câu hỏi:** Email service provider?

**For:**
- Trial expiration warnings
- Welcome emails
- Password reset
- Marketing campaigns

**Provider:**
- [ ] SendGrid
- [ ] AWS SES
- [ ] Mailgun
- [ ] Self-hosted SMTP
- [ ] Khác: _____________________

**Ready to use?**
- [ ] Yes, credentials available
- [ ] No, need to setup

---

### Q5.2.3: SMS/OTP Service
**Câu hỏi:** SMS OTP provider?

**For:**
- Trial signup verification
- Student registration
- Parent registration (currently Zalo OTP)

**Provider:**
- [ ] Zalo OTP (existing)
- [ ] Twilio
- [ ] AWS SNS
- [ ] Vietnam SMS gateways (VietGuys, SMAS, etc.)
- [ ] Khác: _____________________

---

# SUMMARY CHECKLIST

## ✅ ALL PARTS COMPLETED

### PART 1: Pricing Tier UI Customization (18 questions)
- ✅ Feature Detection API designed
- ✅ Tier-based UI patterns defined
- ✅ Feature flag system specified

### PART 2: AI Branding System (15 questions)
- ✅ AI branding workflow defined
- ✅ Asset generation specifications
- ✅ Multi-language support planned
- ✅ Approval workflow (ADMIN → OWNER)

### PART 3: Preview Website Feature (8 questions)
- ✅ Public Marketing Landing Page solution
- ✅ Best practices report (60+ pages, Vietnamese)
- ✅ SEO optimization strategy
- ✅ Technical specifications complete

### PART 4: Guest User & Trial System (16 questions)
- ✅ B2B owner-centric model defined
- ✅ Trial for CENTER_OWNER only (14 days)
- ✅ Guest contact OWNER flow (no self-registration)
- ✅ Public course visibility control
- ✅ Guest analytics for OWNER insights

### PART 5: Integration & Dependencies (3 questions)
- ✅ Backend API readiness strategy (parallel development)
- ✅ VietQR payment system (KiteHub + Instance-level)
- ✅ AWS SES email service
- ✅ Zalo OTP (existing)

---

## 📊 Documentation Status

| Document | Status |
|----------|--------|
| `architecture-clarification-qa.md` | ✅ Complete (All 60+ questions answered) |
| `system-architecture-v3-final.md` | ⏳ Need PHẦN 6F: Payment System (VietQR) |
| `kiteclass-implementation-plan.md` | ⏳ Need PR updates for payment |
| `frontend-code-quality.md` | ✅ Complete (PART 1-15) |
| `preview-website-best-practices.md` | ✅ Complete (Vietnamese) |

---

## 🎯 Next Actions

1. ✅ Update `system-architecture-v3-final.md` - Add PHẦN 6F: Payment System
2. ✅ Update `kiteclass-implementation-plan.md` - Add payment PRs (3.10, update 3.8)
3. ✅ Consider adding PART 16 to `frontend-code-quality.md` for payment UI patterns
4. ✅ Ready to start implementation!
- [ ] Q4.2.5: Marketing content for guests
- [ ] Q5.2.2-5.2.3: Third-party service details

---

**Next Steps:**
1. Product owner review và trả lời các câu hỏi
2. Document answers vào system-architecture-v3-final.md
3. Create detailed specs cho undefined features (Preview Website)
4. Schedule backend API implementation
5. Resume frontend PRs với clear requirements

**Estimated Time to Complete Q&A:** 2-4 hours
**Recommended Format:** Meeting + follow-up document

---

### Q4.2.2: Course Preview/Demo Lessons ✅ ANSWERED

**Answer:** KHÔNG có demo lessons trong MVP (Defer to V4)

**Rationale:**
- Guest muốn "học thử" → Liên hệ OWNER để negotiate
- OWNER có thể offer trial lesson offline (không qua platform)
- Reduce complexity (no demo content management)
- Focus MVP on lead generation, not self-service

---

### Q4.2.3: Guest Self-Registration ✅ ANSWERED

**Answer:** KHÔNG có guest self-registration (Contact OWNER model)

**Guest Journey:**
```
1. Guest browses public course catalog
2. Interested → Click "Liên Hệ Đăng Ký"
3. Contact OWNER via Facebook/Zalo/Phone
4. OWNER tư vấn, confirm, negotiate
5. OWNER manually enrolls student (admin panel)
6. Student receives login credentials
7. Start learning
```

**Rationale:**
- ✅ OWNER control: Qualify leads, prevent fraud
- ✅ Personal touch: Sales conversation, custom packages
- ✅ Payment flexibility: Cash, transfer, installment
- ✅ No auto-enroll complexity: No payment gateway in MVP
- ✅ Vietnam market: Personal relationship important

---

### Q4.2.4: Guest Session Tracking ✅ ANSWERED

**Answer:** CÓ - Track guest behavior cho OWNER insights

**Analytics Events:**
- ✅ Page visits (landing, courses, course details)
- ✅ Time on site
- ✅ Courses viewed
- ✅ Contact clicks (Facebook, Zalo, Phone)
- ✅ Referral source (Google, Facebook, Direct)

**GDPR Compliance:**
- ✅ Cookie consent banner
- ✅ Anonymous tracking (no PII)
- ✅ Privacy policy link

---

### Q4.2.5: Marketing Content for Guests ✅ ANSWERED

**Answer:** OWNER-driven marketing (KiteClass provides tools)

**On-site:**
- ✅ Contact buttons (Facebook, Zalo, Phone)
- ✅ "Liên Hệ Tư Vấn" forms → Email to OWNER
- ❌ Pop-ups (too intrusive)

**Off-site:**
- ❌ KiteClass không làm marketing cho instances
- ✅ OWNER tự chạy Facebook Ads, Google Ads
- ✅ OWNER remarketing riêng

**Opt-in:**
- ✅ Explicit opt-in for contact form

---

### Q4.2.6: Guest-to-Student Conversion ✅ ANSWERED

**Answer:** Manual conversion qua OWNER (không auto)

**Conversion Flow:**
```
Guest → Contact OWNER → Sales conversation → OWNER enrolls manually
```

**No Guest Trial:** Trial chỉ cho business OWNER (test platform), không phải students

---

## 4.3. Summary: B2B Owner-Centric Model

**Trial System:**
- ✅ 14-day trial cho CENTER_OWNER tạo instance
- ✅ Trial expand features (ENGAGEMENT, MEDIA, PREMIUM)
- ✅ Non-owners liên hệ OWNER để request features
- ✅ 3-day grace period sau trial
- ✅ 90-day data retention
- ✅ Email + Phone prevention (duplicate trials)

**Guest Access:**
- ✅ Public course catalog (Admin-controlled visibility)
- ✅ Full course details + Contact OWNER info
- ✅ Contact: Facebook, Messenger, Zalo, Phone, Email
- ❌ No auto-enrollment (OWNER manual process)
- ❌ No demo lessons (MVP)
- ❌ No guest trial (trial only for business owners)

**Philosophy:**
- **B2B first:** Platform serves business owners, not end students
- **Owner-led sales:** OWNER controls lead qualification, pricing, enrollment
- **KiteClass enables:** Provide tools (public catalog, contact info, analytics)
- **Owner executes:** OWNER closes sales, manages students
- **Human touch:** Personal relationships important in Vietnam market

**Updated in:** 
- system-architecture-v3-final.md PHẦN 6E (Guest & Trial System)
- kiteclass-implementation-plan.md (no changes needed - align with Preview Website PR 3.4)

---

# PART 5: INTEGRATION & DEPENDENCIES

## 🎯 CLARIFICATION: Best Practice Approach

**Key Principles:**
1. **Parallel Development**: Frontend & Backend develop simultaneously
2. **API-First Design**: Define contracts early, mock in frontend
3. **VietQR Payment**: Simple, cost-effective, widely adopted in Vietnam
4. **Minimal Third-Party**: Reduce complexity and cost

---

## 5.1. Backend API Readiness

### Q5.1.1: Which APIs Need to Be Implemented First? ✅ ANSWERED

**Approach:** Parallel development với API mocking trong frontend.

**PR 3.2 (Core Infrastructure) - Can start immediately:**
- ✅ `GET /api/v1/instance/config` (feature flags)
- ✅ `GET /api/v1/instance/theme` (theme settings)
- ✅ `GET /api/v1/instance/branding` (branding settings)

**Strategy:**
```typescript
// Frontend mock data during development
// src/lib/mocks/instance-config.ts
export const mockInstanceConfig = {
  tier: 'BASIC',
  features: {
    engagement: true,
    media: false,
    premium: false
  },
  limitations: {
    maxStudents: 50,
    maxCourses: 10
  }
};

// Switch to real API when backend ready
const config = await fetch('/api/v1/instance/config');
```

**Timeline recommendation:**
- ✅ **Week 1-2**: Frontend uses mocks, Backend develops APIs
- ✅ **Week 3**: Integration testing
- ✅ **Week 4**: Deploy together

---

**PR 3.4 (Public Routes) - Backend ready before frontend starts:**
- ✅ `GET /api/v1/public/instance/{instanceId}/config`
- ✅ `GET /api/v1/public/instance/{instanceId}/branding`
- ✅ `GET /api/v1/public/instance/{instanceId}/courses`
- ✅ `GET /api/v1/public/courses/{courseId}`
- ✅ `GET /api/v1/public/instance/{instanceId}/instructors`
- ✅ `POST /api/v1/public/contact`

**Timeline recommendation:**
- ✅ **Backend first** (3 days) → **Frontend** (5 days) → **Integration** (2 days)

---

**PR 3.11 (AI Branding):**
- ✅ `POST /api/v1/branding/upload` (upload logo)
- ✅ `GET /api/v1/branding/status/{jobId}` (poll generation progress)
- ✅ `GET /api/v1/branding/assets/{jobId}` (get generated assets)
- ✅ `POST /api/v1/branding/publish` (publish to instance)

**Timeline recommendation:**
- ✅ Backend can develop in parallel with earlier PRs
- ✅ Integration when PR 3.11 starts (Week 8-9)

---

**Backend team delivery timeline:**
- ✅ **PR 3.2 APIs**: Week 1-2 (parallel with frontend)
- ✅ **PR 3.4 APIs**: Week 5 (before frontend PR 3.4)
- ✅ **PR 3.11 APIs**: Week 8 (parallel with frontend PR 3.11)

---

## 5.2. Third-Party Services

### Q5.2.1: Payment Gateway Integration ✅ ANSWERED

**Decision:** Sử dụng **VietQR** thay vì payment gateways truyền thống.

**Rationale:**
- ✅ Zero transaction fees (không như VNPay 2-3%, MoMo 1.5%)
- ✅ Instant bank transfer (real-time)
- ✅ Widely adopted in Vietnam (95%+ banks support)
- ✅ Simple implementation (generate QR, verify transfer)
- ✅ No merchant account required (use bank account directly)
- ✅ Support ALL Vietnamese banks (Vietcombank, Techcombank, ACB, etc.)

---

#### **Two-Level Payment System:**

**1. KiteHub Payment (Platform-level):**
```
User purchases subscription → KiteHub bank account
- Generate VietQR with pre-filled:
  - Amount: 499,000 VND (BASIC tier)
  - Content: "KITEHUB {orderId} {customerEmail}"
  - Bank: KiteHub business account
```

**2. KiteClass Instance Payment (Instance-level):**
```
Student enrolls in course → CENTER_OWNER bank account
- Generate VietQR with pre-filled:
  - Amount: 2,000,000 VND (course tuition)
  - Content: "KHOAHOC {courseId} {studentName}"
  - Bank: Owner's personal/business account
```

---

#### **VietQR Implementation:**

**Libraries:**
```json
{
  "dependencies": {
    "vietqr": "^1.2.0",           // VietQR API client
    "qrcode": "^1.5.3",            // QR code generation
    "react-qr-code": "^2.0.12"     // React QR component
  }
}
```

**KiteHub Payment QR:**
```typescript
// backend/src/services/payment/VietQRService.java
@Service
public class VietQRService {

    private static final String KITEHUB_BANK = "970415"; // Vietcombank
    private static final String KITEHUB_ACCOUNT = "1234567890";
    private static final String KITEHUB_ACCOUNT_NAME = "CONG TY TNHH KITECLASS";

    /**
     * Generate VietQR for KiteHub subscription payment
     */
    public VietQRResponse generateSubscriptionQR(
        String orderId,
        PricingTier tier,
        String customerEmail
    ) {
        long amount = tier.getPriceVND(); // 499k, 999k, 1499k
        String content = String.format("KITEHUB %s %s", orderId, customerEmail);

        String qrUrl = buildVietQRUrl(
            KITEHUB_BANK,
            KITEHUB_ACCOUNT,
            KITEHUB_ACCOUNT_NAME,
            amount,
            content
        );

        return VietQRResponse.builder()
            .qrDataUrl(qrUrl)
            .bankName("Vietcombank")
            .accountNumber(maskAccountNumber(KITEHUB_ACCOUNT))
            .accountName(KITEHUB_ACCOUNT_NAME)
            .amount(amount)
            .content(content)
            .orderId(orderId)
            .expiresAt(LocalDateTime.now().plusHours(24)) // 24h expiry
            .build();
    }

    /**
     * Build VietQR URL using VietQR API
     * https://vietqr.io/
     */
    private String buildVietQRUrl(
        String bankBin,
        String accountNo,
        String accountName,
        long amount,
        String content
    ) {
        // VietQR API format
        return String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.jpg?amount=%d&addInfo=%s&accountName=%s",
            bankBin,
            accountNo,
            amount,
            URLEncoder.encode(content, StandardCharsets.UTF_8),
            URLEncoder.encode(accountName, StandardCharsets.UTF_8)
        );
    }
}
```

**Instance-Level Payment QR (OWNER configurable):**
```typescript
// backend/src/entities/Instance.java
@Entity
public class Instance {

    @Embedded
    private BankAccountInfo ownerBankAccount;

    @Embeddable
    public static class BankAccountInfo {
        @Column(nullable = true)
        private String bankCode;        // "970415" (Vietcombank)

        @Column(nullable = true)
        private String accountNumber;   // "9876543210"

        @Column(nullable = true)
        private String accountName;     // "NGUYEN VAN A"

        @Column(nullable = true)
        private String bankName;        // "Vietcombank" (display name)

        // QR template cho instance này
        @Column(nullable = true)
        private String qrTemplate;      // "HOCPHI {courseId} {studentName}"
    }
}

// backend/src/services/payment/InstancePaymentService.java
@Service
public class InstancePaymentService {

    /**
     * Generate QR for course enrollment payment
     * Owner có thể customize bank account
     */
    public VietQRResponse generateCourseEnrollmentQR(
        String instanceId,
        String courseId,
        String studentName,
        long tuitionAmount
    ) {
        Instance instance = instanceRepo.findById(instanceId)
            .orElseThrow(() -> new NotFoundException("Instance not found"));

        BankAccountInfo bankInfo = instance.getOwnerBankAccount();

        if (bankInfo == null || bankInfo.getAccountNumber() == null) {
            throw new PaymentConfigException(
                "Owner chưa cấu hình thông tin chuyển khoản. " +
                "Vui lòng cập nhật trong Settings > Payment."
            );
        }

        // Generate content using owner's template
        String content = formatPaymentContent(
            bankInfo.getQrTemplate(),
            courseId,
            studentName
        );

        String qrUrl = buildVietQRUrl(
            bankInfo.getBankCode(),
            bankInfo.getAccountNumber(),
            bankInfo.getAccountName(),
            tuitionAmount,
            content
        );

        return VietQRResponse.builder()
            .qrDataUrl(qrUrl)
            .bankName(bankInfo.getBankName())
            .accountNumber(maskAccountNumber(bankInfo.getAccountNumber()))
            .accountName(bankInfo.getAccountName())
            .amount(tuitionAmount)
            .content(content)
            .build();
    }

    private String formatPaymentContent(
        String template,
        String courseId,
        String studentName
    ) {
        return template
            .replace("{courseId}", courseId)
            .replace("{studentName}", studentName)
            .replace("{timestamp}", String.valueOf(System.currentTimeMillis() / 1000));
    }
}
```

**Frontend - KiteHub Payment UI:**
```tsx
// frontend/app/(dashboard)/subscription/upgrade/page.tsx
'use client';

import { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Copy, Check } from 'lucide-react';

export default function UpgradePage() {
  const [qrData, setQRData] = useState<VietQRResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const handleUpgrade = async (tier: 'BASIC' | 'STANDARD' | 'PREMIUM') => {
    const response = await fetch('/api/v1/payment/subscription/generate-qr', {
      method: 'POST',
      body: JSON.stringify({ tier })
    });

    const data = await response.json();
    setQRData(data);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="max-w-2xl mx-auto p-6">
      {!qrData ? (
        // Tier selection
        <div className="grid gap-4 md:grid-cols-3">
          <TierCard tier="BASIC" price="499,000" onSelect={handleUpgrade} />
          <TierCard tier="STANDARD" price="999,000" onSelect={handleUpgrade} />
          <TierCard tier="PREMIUM" price="1,499,000" onSelect={handleUpgrade} />
        </div>
      ) : (
        // Payment QR display
        <Card className="p-6">
          <h2 className="text-2xl font-bold mb-4 text-center">
            Quét mã QR để thanh toán
          </h2>

          {/* QR Code */}
          <div className="flex justify-center mb-6">
            <div className="p-4 bg-white rounded-lg shadow-lg">
              <img
                src={qrData.qrDataUrl}
                alt="VietQR"
                className="w-64 h-64"
              />
            </div>
          </div>

          {/* Bank info */}
          <div className="space-y-3 mb-6">
            <div className="flex justify-between items-center p-3 bg-muted rounded">
              <span className="text-sm text-muted-foreground">Ngân hàng:</span>
              <span className="font-semibold">{qrData.bankName}</span>
            </div>

            <div className="flex justify-between items-center p-3 bg-muted rounded">
              <span className="text-sm text-muted-foreground">Số tài khoản:</span>
              <div className="flex items-center gap-2">
                <span className="font-mono">{qrData.accountNumber}</span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => copyToClipboard(qrData.accountNumber)}
                >
                  {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                </Button>
              </div>
            </div>

            <div className="flex justify-between items-center p-3 bg-muted rounded">
              <span className="text-sm text-muted-foreground">Chủ tài khoản:</span>
              <span className="font-semibold">{qrData.accountName}</span>
            </div>

            <div className="flex justify-between items-center p-3 bg-primary/10 rounded">
              <span className="text-sm text-muted-foreground">Số tiền:</span>
              <span className="text-xl font-bold text-primary">
                {qrData.amount.toLocaleString('vi-VN')} đ
              </span>
            </div>

            <div className="flex justify-between items-center p-3 bg-muted rounded">
              <span className="text-sm text-muted-foreground">Nội dung:</span>
              <div className="flex items-center gap-2">
                <span className="font-mono text-sm">{qrData.content}</span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => copyToClipboard(qrData.content)}
                >
                  {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                </Button>
              </div>
            </div>
          </div>

          <Alert>
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              ⚠️ <strong>Quan trọng:</strong> Vui lòng ghi ĐÚNG nội dung chuyển khoản
              để hệ thống tự động xác nhận thanh toán.
            </AlertDescription>
          </Alert>

          {/* Payment verification status */}
          <div className="mt-6 text-center">
            <p className="text-sm text-muted-foreground mb-3">
              Sau khi chuyển khoản, hệ thống sẽ tự động xác nhận trong vòng 1-2 phút.
            </p>
            <Button variant="outline" onClick={() => window.location.reload()}>
              Tôi đã chuyển khoản
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}
```

**Frontend - Instance Payment Settings (OWNER configurable):**
```tsx
// frontend/app/(dashboard)/settings/payment/page.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';

const bankAccountSchema = z.object({
  bankCode: z.string().min(1, 'Vui lòng chọn ngân hàng'),
  accountNumber: z.string().min(8, 'Số tài khoản không hợp lệ'),
  accountName: z.string().min(1, 'Vui lòng nhập tên chủ tài khoản'),
  qrTemplate: z.string().min(1, 'Vui lòng nhập mẫu nội dung chuyển khoản')
});

export default function PaymentSettingsPage() {
  const form = useForm({
    resolver: zodResolver(bankAccountSchema),
    defaultValues: {
      bankCode: '',
      accountNumber: '',
      accountName: '',
      qrTemplate: 'HOCPHI {courseId} {studentName}'
    }
  });

  const [previewQR, setPreviewQR] = useState<string | null>(null);

  const handlePreview = async () => {
    const values = form.getValues();

    // Generate preview QR
    const response = await fetch('/api/v1/instance/payment/preview-qr', {
      method: 'POST',
      body: JSON.stringify({
        ...values,
        amount: 2000000, // Sample amount
        content: values.qrTemplate
          .replace('{courseId}', 'SAMPLE123')
          .replace('{studentName}', 'Nguyen Van A')
      })
    });

    const data = await response.json();
    setPreviewQR(data.qrDataUrl);
  };

  const handleSave = async (data: BankAccountForm) => {
    await fetch('/api/v1/instance/payment/bank-account', {
      method: 'PUT',
      body: JSON.stringify(data)
    });

    toast.success('Đã cập nhật thông tin chuyển khoản');
  };

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Cấu hình thanh toán</h1>

      <Alert className="mb-6">
        <AlertCircle className="h-4 w-4" />
        <AlertTitle>VietQR Payment</AlertTitle>
        <AlertDescription>
          Cấu hình tài khoản ngân hàng để nhận thanh toán từ học viên.
          Hệ thống sẽ tự động tạo mã QR cho mỗi giao dịch.
        </AlertDescription>
      </Alert>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(handleSave)} className="space-y-6">

          {/* Bank selection */}
          <FormField
            control={form.control}
            name="bankCode"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Ngân hàng</FormLabel>
                <Select onValueChange={field.onChange} defaultValue={field.value}>
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn ngân hàng" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="970415">Vietcombank</SelectItem>
                    <SelectItem value="970416">ACB</SelectItem>
                    <SelectItem value="970418">BIDV</SelectItem>
                    <SelectItem value="970422">MB Bank</SelectItem>
                    <SelectItem value="970423">Techcombank</SelectItem>
                    <SelectItem value="970436">Vietinbank</SelectItem>
                    {/* More banks... */}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Account number */}
          <FormField
            control={form.control}
            name="accountNumber"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Số tài khoản</FormLabel>
                <FormControl>
                  <Input
                    placeholder="1234567890"
                    {...field}
                    className="font-mono"
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Account name */}
          <FormField
            control={form.control}
            name="accountName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Tên chủ tài khoản</FormLabel>
                <FormControl>
                  <Input
                    placeholder="NGUYEN VAN A"
                    {...field}
                    className="uppercase"
                  />
                </FormControl>
                <FormDescription>
                  Nhập tên CHÍNH XÁC như trên tài khoản ngân hàng (viết hoa, không dấu)
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* QR content template */}
          <FormField
            control={form.control}
            name="qrTemplate"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Mẫu nội dung chuyển khoản</FormLabel>
                <FormControl>
                  <Input
                    placeholder="HOCPHI {courseId} {studentName}"
                    {...field}
                  />
                </FormControl>
                <FormDescription>
                  Sử dụng biến: {'{courseId}'}, {'{studentName}'}, {'{timestamp}'}
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Preview button */}
          <Button
            type="button"
            variant="outline"
            onClick={handlePreview}
            className="w-full"
          >
            Xem trước mã QR
          </Button>

          {/* QR Preview */}
          {previewQR && (
            <Card className="p-6">
              <h3 className="font-semibold mb-4 text-center">Mã QR mẫu</h3>
              <div className="flex justify-center">
                <img src={previewQR} alt="Preview QR" className="w-48 h-48" />
              </div>
              <p className="text-sm text-muted-foreground text-center mt-4">
                Học viên sẽ thấy mã QR này khi đăng ký khóa học
              </p>
            </Card>
          )}

          {/* Save button */}
          <Button type="submit" className="w-full">
            Lưu cấu hình
          </Button>
        </form>
      </Form>
    </div>
  );
}
```

---

#### **Payment Verification:**

**Manual verification (MVP):**
```java
// backend/src/services/payment/PaymentVerificationService.java
@Service
public class PaymentVerificationService {

    /**
     * OWNER manually confirms payment
     */
    public void manualConfirmPayment(
        String orderId,
        String transactionReference,
        LocalDateTime paidAt
    ) {
        PaymentOrder order = paymentRepo.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));

        order.setStatus(PaymentStatus.PAID);
        order.setTransactionReference(transactionReference);
        order.setPaidAt(paidAt);

        paymentRepo.save(order);

        // Trigger post-payment actions (activate subscription, enroll student, etc.)
        paymentEventPublisher.publish(new PaymentConfirmedEvent(order));
    }
}
```

**Automated verification (V2 - Optional):**
```java
// Use bank API webhooks (if available)
// Vietcombank, Techcombank có Business API
@RestController
@RequestMapping("/api/v1/webhooks/bank")
public class BankWebhookController {

    /**
     * Receive bank transfer notification
     */
    @PostMapping("/transfer-notification")
    public ResponseEntity<?> handleTransferNotification(
        @RequestBody BankTransferNotification notification
    ) {
        // Verify webhook signature
        if (!verifyWebhookSignature(notification)) {
            return ResponseEntity.status(401).build();
        }

        // Match transfer content with order
        String content = notification.getTransferContent();
        PaymentOrder order = paymentRepo.findByContentMatch(content)
            .orElse(null);

        if (order != null && notification.getAmount() == order.getAmount()) {
            order.setStatus(PaymentStatus.PAID);
            order.setTransactionReference(notification.getTransactionId());
            order.setPaidAt(notification.getTransferTime());
            paymentRepo.save(order);

            paymentEventPublisher.publish(new PaymentConfirmedEvent(order));
        }

        return ResponseEntity.ok().build();
    }
}
```

---

#### **Which PR includes payment?**

- ✅ **PR 3.10 (Billing Module)** - KiteHub subscription payment
- ✅ **PR 3.8 (Courses Module)** - Instance enrollment payment (Owner configurable)

---

### Q5.2.2: Email Service ✅ ANSWERED

**Decision:** AWS SES (Simple Email Service)

**Rationale:**
- ✅ Cost-effective: $0.10 per 1,000 emails (first 62,000 free)
- ✅ High deliverability (99%+ inbox rate)
- ✅ Easy integration with Spring Boot
- ✅ Templates support
- ✅ Bounce/complaint handling

**Alternative:** SendGrid (if AWS SES not available)

**Use cases:**
- Trial expiration warnings (Day 11, 13, 14, +1, +2, +3)
- Welcome emails (new user registration)
- Password reset
- Course enrollment confirmation
- Payment confirmation
- Monthly report for OWNER

**Implementation:**
```java
// backend/src/services/email/EmailService.java
@Service
public class EmailService {

    private final AmazonSimpleEmailService sesClient;

    public void sendTrialExpirationWarning(User user, int daysLeft) {
        String subject = String.format(
            "⚠️ Còn %d ngày sử dụng thử KiteClass",
            daysLeft
        );

        String htmlBody = renderTemplate("trial-expiration-warning.html", Map.of(
            "userName", user.getName(),
            "daysLeft", daysLeft,
            "upgradeUrl", "https://kiteclass.com/upgrade"
        ));

        send(user.getEmail(), subject, htmlBody);
    }
}
```

**Setup required:**
- ✅ AWS SES credentials (Access Key, Secret Key)
- ✅ Verify sender domain (kiteclass.com)
- ✅ Email templates (Thymeleaf or HTML)
- ✅ Unsubscribe link (GDPR compliance)

---

### Q5.2.3: SMS/OTP Service ✅ ANSWERED

**Decision:** Keep existing Zalo OTP

**Rationale:**
- ✅ Already implemented (backend has Zalo OTP)
- ✅ Popular in Vietnam
- ✅ Cost-effective
- ✅ Good deliverability

**Use cases:**
- Trial signup verification (prevent fraud)
- Student registration (verify phone)
- Parent registration (verify phone)
- Password reset (secondary method)

**No changes needed** - Continue using existing Zalo OTP service.

**Backup option:** Add Twilio SMS for international numbers (post-MVP)

---

# PART 6: TRIAL LEARNING SYSTEM (V4.1 Phase 2) ⭐ NEW

**Context:** Trial Learning System cho phép potential students (leads) trải nghiệm một số lessons trước khi trả phí. Khác với PART 4 (Owner trial = 14 days platform access), đây là student trial = 3 lessons/day quota.

**Key Principles:**
1. **Trial users = Leads** - Separate from paid students
2. **Passwordless auth** - Magic link via email (faster onboarding)
3. **Daily quota limit** - 3 lessons/day (balance "try before buy" vs "create urgency")
4. **Single course approach** - Mark existing courses with `is_trial` flag (no separate trial courses)
5. **Progress preservation** - Same user_id before/after conversion (no data migration)

---

## 6.1. Trial User Authentication

### Q6.1.1: Why Magic Link vs Traditional Password? ✅ ANSWERED

**Question:** Tại sao dùng magic link (passwordless) thay vì email + password?

**Options:**
- [x] Magic link (passwordless) - Recommended
- [ ] Traditional email + password
- [ ] Social login (Google, Facebook)

**Answer:** Magic link cho trial users

**Rationale:**
1. **Faster onboarding**: Guest nhập email → nhận link → click → bắt đầu học (< 2 phút)
   - Không cần nhớ password, không cần password complexity rules
   - Không có "Forgot password" flow (giảm friction)
2. **Better UX for trial**: Guest chưa commit trả tiền → không muốn create account phức tạp
3. **Mobile-friendly**: Không cần gõ password trên điện thoại (dễ sai)
4. **Security**: One-time token (30 min expiry), không có password để bị leak
5. **Email verification implicit**: Nếu nhận được email → prove ownership

**Implementation:**
- Gateway Service: MagicLinkService generates UUID token, stores in Redis (TTL 30min)
- Email sent với link: `https://kiteclass.com/auth/verify?token={uuid}`
- User clicks → Gateway verifies token → creates User (role=TRIAL_USER) → returns JWT
- Token deleted after use (one-time)

**Post-conversion:** Sau khi trial user converts to paid student, yêu cầu set password (one-time setup)

**Reference:** Gateway PR 1.13 - Magic Link Authentication

---

### Q6.1.2: Why TRIAL_USER Role in Gateway vs Core-Only? ✅ ANSWERED

**Question:** Tại sao thêm TRIAL_USER vào Gateway's user_role enum thay vì chỉ track ở Core Service?

**Options:**
- [x] Add TRIAL_USER to Gateway enum - Recommended
- [ ] Keep TRIAL_USER status only in Core (leads table)

**Answer:** TRIAL_USER trong Gateway user_role enum

**Rationale:**
1. **Authentication at Gateway layer**: Gateway handles all authentication (JWT generation)
   - JWT needs role claim for authorization
   - Frontend checks `user.role` from JWT to show/hide features
2. **API Gateway routing**: Gateway needs role-based rules
   - Rate limiting stricter for TRIAL_USER (30 req/min vs 100 req/min for STUDENT)
   - Authorization: TRIAL_USER can access `/api/v1/lessons/*` but not `/api/v1/classes/*`
3. **Consistent with multi-tenant security model**: All role-based authorization at Gateway
4. **Cross-service consistency**: If role only in Core, Gateway can't enforce trial restrictions

**Implementation:**
- Migration V12 (Gateway): `ALTER TYPE user_role ADD VALUE 'TRIAL_USER'`
- JwtTokenProvider includes role in claims: `{..., "role": "TRIAL_USER", ...}`
- SecurityConfig adds TRIAL_USER to authorized roles for trial endpoints

**Alternative rejected:** Keep status only in Core → Gateway can't make auth decisions, breaks responsibility boundary

**Reference:** Database Design Section 5.5 - Gateway Schema Extension

---

## 6.2. Trial Learning Business Logic

### Q6.2.1: Why 3 Lessons/Day Quota Limit? ✅ ANSWERED

**Question:** Tại sao chọn quota_limit = 3 lessons/day?

**Options:**
- [ ] 1 lesson/day (too restrictive?)
- [x] 3 lessons/day (recommended)
- [ ] 5 lessons/day
- [ ] Unlimited lessons for 7 days (time-based trial)

**Answer:** 3 lessons/day quota

**Rationale:**
1. **Balance "try before buy" and "create urgency"**:
   - 3 lessons ≈ 1-2 hours learning → enough to evaluate course quality
   - Not enough to finish entire course → encourages paid conversion
2. **Prevent abuse**: Unlimited access → users might binge-watch all content without paying
3. **Conversion window**:
   - Trial course has ~20 lessons
   - 3 lessons/day → 7-10 days to complete trial content
   - Creates urgency: "Need to finish soon → should upgrade this week"
4. **Psychological**: Number 3 is digestible ("Just 3 lessons today") vs 5 feels like work
5. **Configurable per tenant**: quota_limit column allows future customization (e.g., premium trial = 5 lessons/day)

**Implementation:**
- `trial_quotas` table: `quota_limit INT DEFAULT 3`
- TrialQuotaService checks: `IF lessons_accessed >= quota_limit THEN throw QuotaExceededException`
- Quota resets daily (new record per user per day)

**Conversion funnel**:
```
Day 1: Access 3 lessons (10% progress)
Day 2: Access 3 lessons (20% progress)
Day 3: Access 3 lessons (30% progress) → "Want to finish? Upgrade now!"
Day 4: Quota exceeded → Show upgrade modal
```

**Reference:** Core PR 2.13 - Trial Quota Management

---

### Q6.2.2: Why Separate Leads Table vs Merge with Students? ✅ ANSWERED

**Question:** Tại sao tạo `leads` table riêng thay vì thêm `is_trial` flag vào `students` table?

**Options:**
- [x] Separate `leads` table - Recommended
- [ ] Add `is_trial` flag to `students` table
- [ ] Use Gateway `users` table only (no Core table)

**Answer:** Separate `leads` table

**Rationale:**
1. **Different data lifecycle**:
   - Leads: NEW → CONTACTED → CONVERTED/LOST (sales workflow)
   - Students: ACTIVE → INACTIVE → GRADUATED (education workflow)
   - Mixing them complicates queries (e.g., "count active students" would need `WHERE is_trial = false`)
2. **Different business processes**:
   - Leads: Nurturing campaigns, conversion tracking, source attribution
   - Students: Enrollment, attendance, grades, certificates
3. **Clear domain separation**:
   - Sales/Marketing domain: Leads, conversion funnel, ROI tracking
   - Education domain: Students, courses, learning progress
4. **Analytics needs**:
   - Marketing: "Conversion rate by source (TRIAL_SIGNUP vs LANDING_PAGE)"
   - Marketing: "Average time to convert: 7 days"
   - Would pollute student analytics if merged
5. **Students may come from non-trial sources**:
   - Direct signup (no trial)
   - Offline registration (in-person)
   - Corporate training (bulk enrollment)

**Implementation:**
- `leads` table: id, user_id (FK to Gateway), email, source, status, converted_at
- `students` table: unchanged (id, user_id, enrollment_id, ...)
- After conversion: Lead.status = CONVERTED, create enrollment (no data migration)

**Alternative rejected:** Add `is_trial` to students → mixes domains, complicates business logic

**Reference:** Database Design Section 5.5 - Design Rationale

---

### Q6.2.3: Why Single Course with is_trial Flag vs Separate Trial Course? ✅ ANSWERED

**Question:** Tại sao đánh dấu existing courses với `is_trial = true` thay vì tạo course riêng cho trial users?

**Options:**
- [x] Single course with `is_trial` flag + `lessons.is_trial_accessible` - Recommended
- [ ] Create separate "Trial Course" with duplicated content
- [ ] Create "Trial Course" with subset of lessons (references to main course)

**Answer:** Single course approach (mark with flag)

**Rationale:**
1. **Avoid content duplication**:
   - Separate trial course → need to copy lessons, videos, resources
   - When main course updates → need to update trial course too (sync nightmare)
2. **Easier content management**:
   - Teacher updates main course → trial users automatically see updates
   - Single source of truth
3. **Simpler conversion**:
   - Same course_id before/after conversion
   - Progress tracking: lesson_progress table uses same course_id → no data migration
4. **Consistent analytics**:
   - "Total completions for Course 101" includes both trial and paid users
   - Can filter by user role if needed: `WHERE user.role = 'STUDENT'`
5. **Flexible trial configuration**:
   - Teacher marks first 3 lessons as `is_trial_accessible = true`
   - Easy to change: "Let's make first 5 lessons free" → update flags, no course duplication

**Implementation:**
- `courses` table: `is_trial BOOLEAN DEFAULT FALSE` (typically 1 trial course per tenant)
- `lessons` table: `is_trial_accessible BOOLEAN DEFAULT FALSE` (first 1-3 lessons)
- LessonService checks: `IF user.role = TRIAL_USER AND lesson.is_trial_accessible = false THEN throw AccessDeniedException`

**Alternative rejected:** Separate trial course → duplication, sync issues, complex conversion logic

**Reference:** Database Design Section 5.5 - Design Rationale (Q4)

---

## 6.3. Trial to Student Conversion

### Q6.3.1: Why UPDATE ROLE vs CREATE NEW STUDENT? ✅ ANSWERED

**Question:** Khi trial user converts to paid student, tại sao UPDATE existing user's role thay vì CREATE new student record?

**Options:**
- [x] UPDATE user.role: TRIAL_USER → STUDENT - Recommended
- [ ] CREATE new User + Student, DELETE old Trial user
- [ ] MERGE Trial and Student data (complex migration)

**Answer:** UPDATE role approach

**Rationale:**
1. **Progress preservation automatic**:
   - `lesson_progress` table uses `user_id` (not student_id)
   - Same user_id before/after conversion → all progress automatically visible
   - No data migration needed
2. **Audit trail preserved**:
   - Same User record → `created_at` shows original trial registration date
   - Can track: "User registered on 2026-01-15, converted on 2026-01-22 (7 days trial)"
3. **Simpler implementation**:
   - Core calls Gateway API: `PUT /users/{id}/role` with `{role: "STUDENT"}`
   - Core updates: `leads.status = CONVERTED, leads.converted_at = NOW()`
   - Core creates enrollment (reuses existing user_id)
   - No foreign key updates, no data copying
4. **Lead record kept for analytics**:
   - Lead table: `{user_id: "abc", source: "TRIAL_SIGNUP", status: "CONVERTED", converted_at: "2026-01-22"}`
   - Marketing can track: "Trial signup source = Google Ads → converted → ROI = +$299"

**Implementation:**
```java
// Core PR 2.14 - Lead Conversion
public ConversionResponse convertToStudent(UUID leadId, ConvertLeadRequest request) {
    // 1. Verify payment
    paymentService.verifyPayment(lead.getUserId(), request.getCourseId());

    // 2. Update role in Gateway
    gatewayClient.updateUserRole(lead.getUserId(), "STUDENT");

    // 3. Update lead status
    lead.setStatus(LeadStatus.CONVERTED);
    lead.setConvertedAt(Instant.now());

    // 4. Create enrollment (reuses user_id)
    enrollmentService.createEnrollment(lead.getUserId(), request.getCourseId());

    return new ConversionResponse(lead.getUserId(), "STUDENT", enrollmentId);
}
```

**Progress preservation query**:
```sql
-- After conversion, student sees all previous progress
SELECT lp.lesson_id, lp.progress_percent, lp.completed
FROM lesson_progress lp
WHERE lp.user_id = :userId; -- Same user_id → progress preserved
```

**Alternative rejected:** CREATE new user + DELETE old → loses progress, loses audit trail, complex FK updates

**Reference:** Core PR 2.14 - Lead to Student Conversion

---

### Q6.3.2: Why Self-Paced (No Class Enrollment) for Trial Phase 1? ✅ ANSWERED

**Question:** Tại sao trial users không thể enroll vào classes (chỉ access lessons trực tiếp) trong Phase 1?

**Options:**
- [x] Self-paced learning only (no class enrollment) for Phase 1 - Recommended
- [ ] Allow trial users to join classes immediately
- [ ] Create separate "Trial Classes" với limited features

**Answer:** Self-paced learning for Phase 1, defer class enrollment to Phase 2

**Rationale:**
1. **Simpler onboarding**:
   - No scheduling conflicts (trial user can start learning immediately)
   - No class selection complexity ("Which class to join?")
   - No waiting for class to start (instant access)
2. **Lower barrier to entry**:
   - Guest clicks "Try Free" → access lessons within 5 minutes
   - vs "Join class" → need to check schedule, wait for enrollment approval, etc.
3. **Phase 1 focus: Validate "try before buy" concept**:
   - Prove that quota system works (3 lessons/day)
   - Prove that trial → paid conversion works
   - Add class features later once core flow validated
4. **Avoid class capacity management**:
   - If trial users join classes → consume class seats
   - Teachers might be annoyed: "Half my class is trial users who won't pay"
5. **Simpler testing**:
   - Phase 1: Test quota enforcement, lesson access, conversion flow
   - Phase 2: Test class enrollment, attendance, teacher-student interaction

**Implementation (Phase 1)**:
- EnrollmentService: `IF user.role = TRIAL_USER AND request.classId != null THEN throw TrialUserClassEnrollmentNotAllowedException`
- Frontend: Hide "Enroll in Class" button for trial users
- Show: "Self-paced learning. Upgrade to join live classes!"

**Phase 2 (Future)**:
- Allow trial users to join "trial classes" (separate from paid classes)
- Or: Allow observing classes (read-only, no interaction)
- Or: Allow joining last 30min of class as observer

**Alternative rejected:** Allow full class access → complex capacity management, teacher friction

**Reference:** Database Design Section 5.5 - Business Rules (BR-TRIAL-006)

---

## 6.4. Trial Learning Technical Implementation

### Q6.4.1: Why Magic Link Expires in 30 Minutes? ✅ ANSWERED

**Question:** Tại sao magic link expiry = 30 minutes (không phải 1 hour hoặc 24 hours)?

**Options:**
- [ ] 10 minutes (too short?)
- [x] 30 minutes (recommended)
- [ ] 1 hour
- [ ] 24 hours (link còn dùng được cả ngày)

**Answer:** 30 minutes expiry

**Rationale:**
1. **Security**: Shorter expiry → smaller window for token theft
   - If email account compromised → attacker has limited time
   - Prevents token reuse after long delay
2. **Balance between security and UX**:
   - 30 minutes enough for user to: Check email (5min) → Click link (30 sec)
   - Not too short: User distracted by phone call → comes back 10min later → still works
3. **Industry standard**: Most passwordless systems use 15-30 minutes
   - Slack magic links: 30 minutes
   - Medium magic links: 15 minutes
   - GitHub magic links: 30 minutes
4. **Encourage immediate action**: "Limited time" creates urgency
   - User more likely to complete signup immediately
   - Reduces abandoned signups

**Implementation:**
- Redis TTL: `redisTemplate.opsForValue().set(key, data, Duration.ofMinutes(30))`
- If expired: Show error "Magic link expired. Request a new one."
- User can request new link (no penalty)

**Edge case handling**:
- User requests link → doesn't check email for 1 hour → link expired → Show clear error + "Resend link" button

**Alternative rejected:** 24 hours → too long, security risk, user might forget context

**Reference:** Gateway PR 1.13 - Magic Link Security

---

### Q6.4.2: Why Lead Conversion Requires Payment Verification? ✅ ANSWERED

**Question:** Tại sao Lead→Student conversion yêu cầu verify payment trước khi update role?

**Options:**
- [x] Verify payment BEFORE conversion - Recommended
- [ ] Update role immediately, verify payment async (trust-based)
- [ ] Manual approval by admin (slow)

**Answer:** Verify payment before conversion

**Rationale:**
1. **Prevent fraud**:
   - User could bypass payment and get student access
   - Without verification: POST /leads/{id}/convert → instant STUDENT role → free access
2. **Atomic transaction**:
   - Payment success → Role update → Enrollment created (all or nothing)
   - If payment fails → rollback entire conversion
3. **Clear business logic**:
   ```java
   if (!paymentService.verifyPayment(userId, courseId)) {
       throw new PaymentNotCompletedException("PAYMENT_NOT_FOUND");
   }
   // Only proceed if payment verified
   gatewayClient.updateUserRole(userId, "STUDENT");
   ```
4. **Audit trail**: Conversion record includes `payment_transaction_id`
   - Can track: "User paid ₫299k via VNPay on 2026-01-22 → converted"
   - Accounting reconciliation: "All conversions have matching payments"

**Implementation (Phase 1 - Mock)**:
```java
@Service
@Profile("!prod")
public class MockPaymentService implements PaymentService {
    public PaymentVerificationResponse verifyPayment(UUID userId, Long courseId) {
        // Always return success for testing
        return new PaymentVerificationResponse(UUID.randomUUID(), 299000, "COMPLETED");
    }
}
```

**Implementation (Phase 2 - Real)**:
- Integrate VNPay/Stripe API
- Check payment status: `GET /payments/{transactionId}/status`
- Only convert if status = COMPLETED

**Alternative rejected:** Trust-based (no verification) → fraud risk, accounting issues

**Reference:** Core PR 2.14 - Payment Verification

---

## 6.5. Summary

**Trial Learning System Design Decisions:**

| Decision | Chosen Approach | Rationale |
|----------|----------------|-----------|
| Authentication | Magic link (passwordless) | Faster onboarding, mobile-friendly, security |
| Role location | TRIAL_USER in Gateway enum | Consistent auth model, JWT claims, API routing |
| Quota limit | 3 lessons/day | Balance "try before buy" vs "create urgency" |
| Data model | Separate leads table | Different lifecycle, clear domain separation |
| Course approach | Single course with flags | No duplication, easier management, simpler conversion |
| Conversion strategy | UPDATE role (same user_id) | Progress preserved, audit trail, simpler implementation |
| Trial scope | Self-paced (no classes) Phase 1 | Simpler onboarding, validate core flow first |
| Magic link expiry | 30 minutes | Security vs UX balance, industry standard |
| Payment verification | Required before conversion | Prevent fraud, atomic transaction, audit trail |

**Implementation References:**
- Database: Migration V12 (Gateway enum + Core tables)
- Gateway: PR 1.13 (Magic link auth)
- Core: PR 2.13 (Trial registration), PR 2.14 (Conversion)
- Frontend: PR 3.13 (Trial UI), PR 3.14 (Payment flow)

**Next Steps:**
1. Review and approve all 8 design decisions
2. Implement Gateway PR 1.13 (TRIAL_USER authentication)
3. Implement Core PR 2.13 (Quota management)
4. Implement Core PR 2.14 (Conversion flow)
5. Implement Frontend PR 3.13-3.14 (Trial UI)

---

