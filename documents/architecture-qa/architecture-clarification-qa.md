# Architecture Clarification Q&A

**Mục đích:** Xác nhận yêu cầu cho 4 vấn đề architecture ảnh hưởng đến frontend implementation
**Deadline:** Trước khi bắt đầu PR 3.2
**Người trả lời:** Product Owner / Tech Lead

---

# PART 1: PRICING TIER UI CUSTOMIZATION

## 1.1. Feature Detection Mechanism

### Q1.1.1: Feature Detection API Endpoint
**Câu hỏi:** Backend sẽ cung cấp endpoint nào để frontend query available features?

**Đề xuất:**
```
GET /api/v1/instance/config
GET /api/v1/instance/features
GET /api/v1/subscription/status
```

**Vui lòng chọn hoặc đề xuất endpoint khác:**
- [ ] `/api/v1/instance/config` (Recommended)
- [ ] `/api/v1/instance/features`
- [ ] Khác: _____________________

**Response format mong muốn:**
```json
{
  "instanceId": "abc-academy-001",
  "tier": "STANDARD",
  "addOns": ["ENGAGEMENT"],
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
  }
}
```

**Response format này có OK không?**
- [ ] OK, implement đúng như vậy
- [ ] Cần điều chỉnh: _____________________

---

### Q1.1.2: Feature Detection Caching
**Câu hỏi:** Feature flags có thay đổi trong runtime không? Frontend có cần poll để update không?

**Scenarios:**
1. Customer nâng cấp từ BASIC → STANDARD trong khi đang dùng
2. Customer thêm MEDIA pack trong khi đang dùng
3. Trial expires → features bị lock

**Vui lòng trả lời:**

**Feature flags có thể thay đổi khi user đang online không?**
- [ ] CÓ - Frontend cần poll hoặc WebSocket để update real-time
- [ ] KHÔNG - Chỉ update khi user login lại

**Nếu CÓ thay đổi runtime:**
**Cơ chế notification nào sẽ dùng?**
- [ ] Frontend poll mỗi 5 phút
- [ ] WebSocket push notification từ backend
- [ ] Server-Sent Events (SSE)
- [ ] Không cần real-time, user sẽ refresh page

**Cache TTL bao lâu?**
- [ ] 1 giờ (Recommended)
- [ ] 24 giờ
- [ ] Khác: _____ giờ

---

### Q1.1.3: Feature Lock Behavior
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
- [ ] Option B: Soft Block (modal with preview)
- [ ] Option C: Hide completely (no menu item)
- [ ] Khác: _____________________

---

### Q1.1.4: Resource Limit Warnings
**Câu hỏi:** Khi nào hiển thị warning về giới hạn tài nguyên?

**Ví dụ:** STANDARD tier có limit 200 học viên

**Warning thresholds:**
- [ ] 80% capacity (160/200 students) → Warning banner
- [ ] 90% capacity (180/200 students) → Warning banner + email
- [ ] 100% capacity (200/200 students) → Block thêm học viên mới + force upgrade

**UI Behavior khi đạt 100% limit:**
- [ ] Disable "Thêm học viên" button
- [ ] Show "Thêm học viên" button nhưng click → upgrade modal
- [ ] Cho phép exceed limit 5% (grace period)
- [ ] Khác: _____________________

**Email notification khi đạt limit?**
- [ ] CÓ - Gửi email tự động cho CENTER_OWNER
- [ ] KHÔNG - Chỉ show UI warning

---

### Q1.1.5: Tier Upgrade Flow
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
- [ ] Khác: _____________________

**Nếu Option A, payment gateway nào?**
- [ ] VNPay
- [ ] MoMo
- [ ] ZaloPay
- [ ] Stripe (international)
- [ ] Tất cả

---

## 1.2. Tier-Specific UI Differences

### Q1.2.1: UI Customization Level
**Câu hỏi:** Ngoài feature availability, có điểm khác biệt UI nào giữa các tier không?

**Ví dụ potential differences:**

| Feature | BASIC | STANDARD | PREMIUM |
|---------|-------|----------|---------|
| Logo branding | ❌ KiteClass logo | ✅ Custom logo | ✅ Custom logo |
| Theme colors | ❌ Default only | ❌ Default only | ✅ Custom colors |
| Remove "Powered by KiteClass" | ❌ | ❌ | ✅ |
| Custom domain | ❌ | ❌ | ✅ |
| Priority support badge | ❌ | ❌ | ✅ |

**Vui lòng xác nhận:**

**BASIC tier có được custom logo không?**
- [ ] CÓ - Tất cả tier đều có custom logo
- [ ] KHÔNG - Chỉ STANDARD và PREMIUM

**BASIC tier có được custom theme colors không?**
- [ ] CÓ - Tất cả tier đều custom được
- [ ] KHÔNG - Chỉ PREMIUM mới custom được
- [ ] KHÔNG - Tất cả tier đều dùng AI-generated branding

**Có watermark "Powered by KiteClass" không?**
- [ ] CÓ - Hiện trên BASIC và STANDARD, PREMIUM thì remove được
- [ ] CÓ - Hiện trên tất cả tier
- [ ] KHÔNG - Không có watermark

**PREMIUM có được custom subdomain không?**
- [ ] CÓ - Ví dụ: custom-domain.com thay vì abc-academy.kiteclass.com
- [ ] KHÔNG - Tất cả dùng *.kiteclass.com

---

### Q1.2.2: Analytics & Reporting Access
**Câu hỏi:** Analytics features có khác nhau giữa các tier không?

**Đề xuất differentiation:**

| Feature | BASIC | STANDARD | PREMIUM |
|---------|-------|----------|---------|
| Basic reports (điểm danh, học phí) | ✅ | ✅ | ✅ |
| Advanced analytics dashboard | ❌ | ✅ | ✅ |
| Export to Excel | ❌ | ✅ | ✅ |
| Custom reports | ❌ | ❌ | ✅ |
| API access | ❌ | ❌ | ✅ |

**Có implement tier-based analytics không?**
- [ ] CÓ - Implement theo bảng trên
- [ ] CÓ - Nhưng khác: _____________________
- [ ] KHÔNG - Tất cả tier có full analytics

---

# PART 2: AI BRANDING SYSTEM

## 2.1. AI Branding Workflow

### Q2.1.1: Who Can Upload Branding?
**Câu hỏi:** Ai có quyền upload ảnh để generate branding?

**Vui lòng chọn:**
- [ ] CENTER_OWNER only
- [ ] CENTER_OWNER và CENTER_ADMIN
- [ ] Tất cả roles (TEACHER cũng được)
- [ ] Chỉ KiteHub Admin (customer không tự upload được)

---

### Q2.1.2: Re-generation Policy
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
- [ ] Khác: _____________________

---

### Q2.1.3: Manual Override
**Câu hỏi:** Customer có thể manual edit AI-generated assets không?

**Ví dụ:**
- AI generate headline: "Học viện ABC - Nơi ươm mầm tài năng"
- Customer muốn đổi thành: "Học viện ABC - Nơi khơi nguồn tri thức"

**Manual override options:**

**Text Content (headlines, CTAs):**
- [ ] CÓ - Customer tự edit text trong admin panel
- [ ] KHÔNG - Phải dùng text do AI generate

**Logo Position/Size:**
- [ ] CÓ - Customer adjust position, scale
- [ ] KHÔNG - Fixed layout

**Colors:**
- [ ] CÓ - Customer override primary/secondary colors
- [ ] KHÔNG - Phải dùng colors do AI extract

**Images:**
- [ ] CÓ - Customer upload custom hero banner (không dùng AI)
- [ ] KHÔNG - Chỉ dùng AI-generated banners

---

### Q2.1.4: Asset Storage & CDN
**Câu hỏi:** AI-generated assets sẽ store ở đâu?

**Vui lòng chọn:**
- [ ] AWS S3 + CloudFront CDN
- [ ] Cloudflare R2 + CDN
- [ ] Local server storage (không dùng CDN)
- [ ] Khác: _____________________

**Asset retention policy:**
- [ ] Keep forever (không xóa)
- [ ] Keep 1 năm, sau đó archive
- [ ] Delete khi customer churn

**Quota per instance:**
- [ ] No limit
- [ ] 1GB storage
- [ ] 5GB storage
- [ ] Khác: _____ GB

---

### Q2.1.5: Asset Quality Settings
**Câu hỏi:** Quality settings cho AI-generated images?

**Hero Banner (1920x600):**
- [ ] High quality (300KB - 500KB, best visual)
- [ ] Medium quality (150KB - 250KB, balanced)
- [ ] Low quality (< 100KB, fast load)

**Profile Images (400x400):**
- [ ] High quality (~200KB)
- [ ] Medium quality (~100KB)
- [ ] Low quality (~50KB)

**WebP format support:**
- [ ] CÓ - Use WebP với fallback to JPEG
- [ ] KHÔNG - Chỉ dùng JPEG/PNG

---

## 2.2. AI Service Provider

### Q2.2.1: Image Generation Provider
**Câu hỏi:** Sử dụng AI provider nào cho image generation?

**Current architecture mentions Stable Diffusion XL, but confirm:**

**Primary provider:**
- [ ] Stable Diffusion XL (self-hosted)
- [ ] DALL-E 3 (OpenAI API)
- [ ] Midjourney API
- [ ] Stability AI API (hosted)
- [ ] Khác: _____________________

**Fallback provider (nếu primary fail):**
- [ ] CÓ fallback: _____________________
- [ ] KHÔNG fallback

**Cost consideration:**
- Stable Diffusion XL: ~$0.08/image (self-hosted)
- DALL-E 3: ~$0.04/image (1024x1024)
- Midjourney: ~$0.07/image

**Budget per generation job (10 images):**
- [ ] < $0.50 (use cheaper options)
- [ ] $0.50 - $1.00 (balanced)
- [ ] > $1.00 (highest quality)

---

### Q2.2.2: Background Removal Service
**Câu hỏi:** Background removal dùng service nào?

**Options:**
- [ ] Remove.bg API ($0.09/image, highest quality)
- [ ] U2-Net (self-hosted, free)
- [ ] Cloudinary Remove Background
- [ ] Khác: _____________________

---

### Q2.2.3: Text Generation (Marketing Copy)
**Câu hỏi:** Marketing copy generation dùng LLM nào?

**Options:**
- [ ] GPT-4 (~$0.015/generation, best quality)
- [ ] GPT-3.5-turbo (~$0.002/generation, good quality)
- [ ] Claude 3.5 Sonnet
- [ ] Gemini Pro
- [ ] Self-hosted LLM (Llama, etc.)

**Tone & style:**
- [ ] Professional & formal
- [ ] Friendly & casual
- [ ] Inspiring & motivational
- [ ] Tùy theo industry type (education vs corporate)

---

## 2.3. Multi-Language Support

### Q2.3.1: Language for Generated Content
**Câu hỏi:** AI-generated marketing copy sẽ là ngôn ngữ gì?

**Current assumption: Vietnamese only**

**Confirm:**
- [ ] Chỉ tiếng Việt
- [ ] Tiếng Việt + English
- [ ] Multi-language (customer chọn)

**Nếu multi-language:**
**Customer chọn ngôn ngữ khi nào?**
- [ ] Khi upload ảnh (generate 1 lần cho 1 ngôn ngữ)
- [ ] Sau khi generate (generate lại cho ngôn ngữ khác)
- [ ] Generate multiple languages cùng lúc

---

# PART 3: PREVIEW WEBSITE FEATURE

## 3.1. Feature Definition

### Q3.1.1: What is "Preview Website"?
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
- [ ] Option A: Instance marketing landing page
- [ ] Option B: Live demo system
- [ ] Option C: Staging/preview environment
- [ ] Option D: Marketing site builder
- [ ] Option E: _____________________

---

### Q3.1.2: Target Audience
**Câu hỏi:** Ai sẽ sử dụng "Preview Website" feature?

- [ ] Prospective students (chưa đăng ký học)
- [ ] Prospective customers (chưa mua KiteClass)
- [ ] Existing students (đã đăng ký)
- [ ] CENTER_ADMIN (internal use)
- [ ] Khác: _____________________

---

### Q3.1.3: Authentication Required?
**Câu hỏi:** "Preview Website" có cần authentication không?

- [ ] Public (không cần login)
- [ ] Guest access (tạo temporary account)
- [ ] Requires login
- [ ] Khác: _____________________

---

### Q3.1.4: Content Source
**Câu hỏi:** Content trên "Preview Website" lấy từ đâu?

- [ ] AI-generated assets (from Part 2)
- [ ] Customer manual input
- [ ] Sample/template content
- [ ] Live data from instance
- [ ] Khác: _____________________

---

### Q3.1.5: Technical Stack
**Câu hỏi:** "Preview Website" build bằng công nghệ gì?

- [ ] Next.js static export (same codebase as main frontend)
- [ ] Separate marketing site builder
- [ ] WordPress/CMS integration
- [ ] Custom page builder
- [ ] Khác: _____________________

---

### Q3.1.6: Customization Level
**Câu hỏi:** Customer có customize "Preview Website" được không?

**If yes, what can be customized?**
- [ ] Text content (headlines, descriptions)
- [ ] Images (upload custom images)
- [ ] Layout (reorder sections)
- [ ] Theme colors
- [ ] Domain name
- [ ] SEO meta tags
- [ ] Nothing (fully auto-generated)

---

### Q3.1.7: Relationship with Main Instance
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
- [ ] Redirect to main instance login/register page
- [ ] Inline registration form on Preview Website
- [ ] Contact form (admin follow up manually)
- [ ] No registration capability

**Scenario 2: Course Information**
```
Preview Website hiển thị course catalog
→ Data sync từ main instance hay static content?
```

**Options:**
- [ ] Real-time sync (API call to main instance)
- [ ] Periodic sync (every 1 hour)
- [ ] Manual publish (admin click "Update Preview")
- [ ] Static content (not synced)

---

## 3.2. Implementation Priority

### Q3.2.1: MVP Scope
**Câu hỏi:** "Preview Website" feature có trong MVP scope không?

- [ ] CÓ - Critical feature, must have in V3
- [ ] KHÔNG - Nice to have, có thể defer to V3.5
- [ ] KHÔNG CHẮC - Cần discuss thêm

**Nếu CÓ trong MVP:**
**Which PR should include this?**
- [ ] PR 3.4 (Public Routes & Landing Pages)
- [ ] PR 3.8 (Additional Features)
- [ ] Separate PR after MVP
- [ ] Khác: _____________________

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
