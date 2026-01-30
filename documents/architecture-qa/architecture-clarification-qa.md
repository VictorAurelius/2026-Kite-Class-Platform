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

## 4.1. Trial System Design

### Q4.1.1: Trial Duration
**Câu hỏi:** Trial bao lâu?

**Landing page hiện tại: "Dùng thử miễn phí 14 ngày"**

**Confirm:**
- [ ] 14 ngày (as stated)
- [ ] 7 ngày
- [ ] 30 ngày
- [ ] Khác: _____ ngày

---

### Q4.1.2: Trial Tier
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
- [ ] Option A: Full PREMIUM
- [ ] Option B: STANDARD tier
- [ ] Option C: Custom với limits
- [ ] Khác: _____________________

**Nếu Option C, specify limits:**
```
Max students: _____
Max courses: _____
Max teachers: _____
Video storage: _____ GB
Gamification: [ ] Có [ ] Không
Parent Portal: [ ] Có [ ] Không
AI Marketing: [ ] Có [ ] Không
```

---

### Q4.1.3: Trial Signup Requirements
**Câu hỏi:** Yêu cầu gì để signup trial?

**Current proposal:**
- Organization name
- Name
- Email
- Phone

**Payment info required?**
- [ ] CÓ - Phải nhập credit card (không charge)
- [ ] KHÔNG - Không cần payment info

**Phone verification?**
- [ ] CÓ - Zalo OTP verification
- [ ] CÓ - SMS OTP
- [ ] KHÔNG - Chỉ cần email verification

**Email verification?**
- [ ] CÓ - Gửi link verify email trước khi activate trial
- [ ] KHÔNG - Activate ngay sau signup

**Additional questions:**
- [ ] Industry type (giáo dục, corporate training, etc.)
- [ ] Company size (nhỏ hơn 50, 50-200, >200 học viên)
- [ ] How did you hear about us?
- [ ] Khác: _____________________

---

### Q4.1.4: Trial Expiration Behavior
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
- [ ] Option B: 3-day grace period
- [ ] Option C: Auto downgrade to FREE
- [ ] Khác: _____________________

**Data retention sau trial:**
- [ ] Keep forever (customer có thể comeback anytime)
- [ ] Keep 30 ngày sau trial expiration
- [ ] Keep 90 ngày
- [ ] Delete ngay (không retention)

---

### Q4.1.5: Trial-to-Paid Conversion
**Câu hỏi:** Conversion flow từ trial sang paid như thế nào?

**In-app conversion prompts:**

**Day 1-10 (early trial):**
- [ ] No prompts (để customer explore)
- [ ] Soft banner: "Bạn còn X ngày trial"
- [ ] Upgrade CTA ở footer

**Day 11-13 (late trial):**
- [ ] Warning banner: "Còn 3 ngày, nâng cấp ngay"
- [ ] Email reminder
- [ ] In-app notification

**Day 14 (last day):**
- [ ] Urgent banner: "Hôm nay là ngày cuối"
- [ ] Email: "Last chance to upgrade"
- [ ] Phone call from sales (high-touch)

**After expiration:**
- [ ] Lock instance + email với upgrade link
- [ ] Allow grace period (see Q4.1.4)

**Conversion incentives:**
- [ ] Discount: "Upgrade hôm nay giảm 20%"
- [ ] Extended trial: "Thêm 7 ngày nếu nâng cấp trong 24h"
- [ ] No incentive (standard pricing)

**Vui lòng chọn strategy và incentives:**
_____________________

---

### Q4.1.6: Multiple Trial Prevention
**Câu hỏi:** Ngăn chặn customer tạo nhiều trial accounts như thế nào?

**Detection methods:**
- [ ] Email address (1 email = 1 trial)
- [ ] Phone number (1 phone = 1 trial)
- [ ] Credit card (nếu require CC)
- [ ] IP address
- [ ] Device fingerprinting
- [ ] Không ngăn chặn (allow multiple trials)

**Enforcement:**
- [ ] Hard block: "Email này đã dùng trial"
- [ ] Soft warning: "Bạn có muốn extend trial thay vì tạo mới?"
- [ ] Allow but notify sales team

---

## 4.2. Guest User Access

### Q4.2.1: Public Course Catalog
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
- [ ] Option A: Full public catalog (SEO-friendly)
- [ ] Option B: Teaser (limited preview)
- [ ] Option C: No public access
- [ ] Khác: _____________________

**Nếu Option A or B:**
**Course details nào public?**
- [ ] Course name
- [ ] Description
- [ ] Price
- [ ] Schedule (start date, duration)
- [ ] Teacher name & bio
- [ ] Syllabus/curriculum
- [ ] Student count
- [ ] Reviews/ratings
- [ ] Khác: _____________________

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

## Critical Blocking Issues (Must Answer Before PR 3.2)

- [ ] Q1.1.1: Feature Detection API endpoint confirmed
- [ ] Q3.1.1: "Preview Website" definition (CRITICAL)
- [ ] Q4.1.2: Trial tier specification
- [ ] Q4.2.1: Public course catalog decision

## High Priority (Needed for PR 3.3-3.4)

- [ ] Q2.1.2: AI branding re-generation policy
- [ ] Q4.1.3: Trial signup requirements
- [ ] Q4.1.4: Trial expiration behavior
- [ ] Q4.2.3: Guest self-registration flow

## Medium Priority (Needed for PR 3.5+)

- [ ] Q1.1.5: Tier upgrade flow
- [ ] Q2.1.3: Manual override capabilities
- [ ] Q2.2.1: AI service provider selection
- [ ] Q4.2.2: Course preview/demo lessons

## Low Priority (Can Defer)

- [ ] Q1.2.2: Analytics tier differentiation
- [ ] Q2.3.1: Multi-language support
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
