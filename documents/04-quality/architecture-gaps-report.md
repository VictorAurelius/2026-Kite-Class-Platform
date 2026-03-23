# Frontend Architecture Gaps & Requirements Report

**Date:** 2026-01-29
**Status:** Analysis Complete - Action Required
**Related To:** Frontend PRs 3.1-3.11 Implementation

---

## Executive Summary

Phân tích system-architecture-v3-final.md đã xác định **4 vấn đề quan trọng** ảnh hưởng đến frontend development:

1. **Pricing Tier UI Customization**: Hệ thống có 3 gói (Basic/Standard/Premium) + 2 add-ons, nhưng KHÔNG có tài liệu về cơ chế feature detection cho frontend
2. **AI Branding System**: Đã được document đầy đủ - tạo 10+ marketing assets tự động từ 1 ảnh upload
3. **Preview Website Feature**: Được đánh dấu là tính năng MỚI của V3 nhưng KHÔNG có bất kỳ mô tả nào
4. **Guest User Support**: Public routes tồn tại cho KiteHub, nhưng KHÔNG rõ KiteClass instances có hỗ trợ guest access không

**Kết luận:** Cần bổ sung thiết kế cho 3 gaps quan trọng trước khi implement frontend PRs.

---

## 1. PRICING TIER UI CUSTOMIZATION

### 1.1. Architecture Findings

**3 Base Pricing Tiers:**
- **BASIC** (500k/tháng): ≤50 học viên, 3 services (User+Gateway, Core, Frontend)
  - ❌ KHÔNG có: Gamification, Parent Portal, Forum, Media

- **STANDARD** (1tr/tháng): ≤200 học viên, 4 services (+ Engagement Service)
  - ✅ CÓ: Gamification, Parent Portal, Forum
  - ❌ KHÔNG có: Media features

- **PREMIUM** (2tr/tháng): Unlimited học viên, 4-5 services
  - ✅ CÓ: TẤT CẢ features + AI Marketing Agent + Priority Support

**2 Add-on Packs:**
- **ENGAGEMENT PACK** (+300k/tháng): Adds Gamification + Parent Portal + Forum
- **MEDIA PACK** (+500-800k/tháng): Adds Video Upload + Live Streaming

### 1.2. Service-Level Feature Differentiation

**Backend Dynamic Provisioning:**
```
BASIC Tier:
├── User+Gateway Service (512MB)
├── Core Service (768MB)
└── Frontend (256MB)
Total: ~1.7GB RAM

STANDARD Tier:
├── User+Gateway Service (512MB)
├── Core Service (768MB)
├── Engagement Service (384MB)  ← NEW
└── Frontend (256MB)
Total: ~2GB RAM

PREMIUM Tier:
├── User+Gateway Service (512MB)
├── Core Service (768MB)
├── Engagement Service (384MB)
├── Media Service (512MB)  ← OPTIONAL
└── Frontend (256MB)
Total: ~2.5GB RAM
```

**Key Insight:** Services được deploy động dựa trên tier → Frontend PHẢI detect services nào available.

### 1.3. Critical Gap: No Feature Detection Mechanism

**Thiếu trong Architecture:**
- ❌ Không có API endpoint để query available features
- ❌ Không có feature flag configuration system
- ❌ Không có UI component-level conditional rendering logic
- ❌ Không có documentation về cách frontend detect tier/services

**Frontend Requirements:**

1. **Feature Detection API Needed:**
```typescript
// Endpoint cần thiết kế:
GET /api/instance/features

Response:
{
  "tier": "STANDARD",
  "services": ["user-gateway", "core", "engagement", "frontend"],
  "features": {
    "gamification": true,
    "parentPortal": true,
    "forum": true,
    "mediaUpload": false,
    "liveStreaming": false,
    "aiMarketing": false
  },
  "limits": {
    "maxStudents": 200,
    "videoStorageGB": 0
  }
}
```

2. **Feature Flag Provider Pattern:**
```typescript
// src/providers/FeatureFlagProvider.tsx
interface FeatureFlags {
  gamification: boolean;
  parentPortal: boolean;
  forum: boolean;
  mediaUpload: boolean;
  liveStreaming: boolean;
  aiMarketing: boolean;
}

const FeatureFlagContext = createContext<FeatureFlags | null>(null);

export function useFeatureFlag(feature: keyof FeatureFlags): boolean {
  const flags = useContext(FeatureFlagContext);
  if (!flags) throw new Error('FeatureFlagProvider not found');
  return flags[feature];
}
```

3. **Conditional UI Rendering:**
```typescript
// Example usage in navigation:
function Navigation() {
  const hasGamification = useFeatureFlag('gamification');
  const hasParentPortal = useFeatureFlag('parentPortal');

  return (
    <nav>
      <NavItem href="/classes">Lớp học</NavItem>
      <NavItem href="/students">Học viên</NavItem>
      {hasGamification && <NavItem href="/gamification">Game hóa</NavItem>}
      {hasParentPortal && <NavItem href="/parents">Phụ huynh</NavItem>}
    </nav>
  );
}
```

4. **Upgrade Prompts for Locked Features:**
```typescript
// src/components/UpgradePrompt.tsx
function UpgradePrompt({ feature }: { feature: string }) {
  return (
    <Card>
      <Lock className="w-12 h-12 text-gray-400" />
      <h3>Tính năng {feature} chỉ có trên gói cao hơn</h3>
      <Button onClick={handleUpgrade}>Nâng cấp gói</Button>
    </Card>
  );
}
```

### 1.4. Testing Requirements

**Feature Flag Testing:**
```typescript
// src/__tests__/feature-flags.test.ts
describe('Feature Flag System', () => {
  it('should show gamification menu for STANDARD tier', () => {
    mockFeatureFlags({ tier: 'STANDARD', gamification: true });
    render(<Navigation />);
    expect(screen.getByText('Game hóa')).toBeInTheDocument();
  });

  it('should hide media upload for BASIC tier', () => {
    mockFeatureFlags({ tier: 'BASIC', mediaUpload: false });
    render(<MediaUploadButton />);
    expect(screen.getByText('Nâng cấp gói')).toBeInTheDocument();
  });
});
```

### 1.5. Action Required

**BEFORE PR 3.2:**
- [ ] Thiết kế Feature Detection API endpoint
- [ ] Xác định feature flag data structure
- [ ] Document tier-based UI behavior specifications

**IN PR 3.3 (Providers):**
- [ ] Implement FeatureFlagProvider
- [ ] Add useFeatureFlag hook
- [ ] Create feature flag loading/caching logic

**IN PR 3.4+ (Components):**
- [ ] Conditional navigation rendering
- [ ] Upgrade prompt components
- [ ] Feature-locked states for all tier-specific features

---

## 2. AI BRANDING SYSTEM

### 2.1. Architecture Findings

**FULLY DOCUMENTED** - System đã có trong V2, giữ nguyên V3.

**AI Marketing Agent Module (KiteHub):**
- Input: 1 ảnh upload từ customer
- Output: 10+ marketing assets trong ~5 phút
- Technology: Stable Diffusion XL + GPT-4 + Remove.bg
- Cost: $0.186 per instance

**Generated Assets:**
1. Profile Images (3 variations): Background-removed, circle crop, square crop
2. Hero Banner (1920x600): AI-generated gradient background
3. Section Banners (3 items): About, Courses, Contact sections
4. Logo Variants (3 items): Primary, secondary, icon-only
5. Open Graph Image (1200x630): For social sharing
6. Marketing Copy: Hero headline, sub-headline, CTAs, value props

### 2.2. Frontend Requirements

**1. Asset Display & Management:**
```typescript
// src/types/branding.ts
interface BrandingAssets {
  profileImages: {
    cutout: string;
    circle: string;
    square: string;
  };
  heroBanner: string;
  sectionBanners: {
    about: string;
    courses: string;
    contact: string;
  };
  logos: {
    primary: string;
    secondary: string;
    iconOnly: string;
  };
  ogImage: string;
  marketingCopy: {
    heroHeadline: string;
    subHeadline: string;
    callToAction: string;
    valueProps: string[];
  };
}

interface BrandingSettings {
  organizationName: string;
  industry?: string;
  slogan?: string;
  brandColors?: {
    primary: string;
    secondary: string;
  };
  generatedAssets?: BrandingAssets;
  generatedAt?: Date;
}
```

**2. Dynamic Image Loading:**
```typescript
// Components must use CDN URLs from AI-generated assets
function HeroBanner() {
  const { branding } = useBranding();

  return (
    <section
      style={{
        backgroundImage: `url(${branding.generatedAssets?.heroBanner})`
      }}
    >
      <h1>{branding.generatedAssets?.marketingCopy.heroHeadline}</h1>
      <p>{branding.generatedAssets?.marketingCopy.subHeadline}</p>
    </section>
  );
}
```

**3. Fallback Handling:**
```typescript
// Handle loading states and fallbacks
function BrandedImage({ type }: { type: keyof BrandingAssets }) {
  const { branding, isLoading } = useBranding();

  if (isLoading) return <Skeleton className="w-full h-48" />;
  if (!branding.generatedAssets) return <DefaultPlaceholder />;

  return <img src={branding.generatedAssets[type]} alt={type} />;
}
```

**4. Image Upload UI (Admin Dashboard):**
```typescript
// src/app/(authenticated)/admin/branding/page.tsx
function BrandingUploadPage() {
  const [isGenerating, setIsGenerating] = useState(false);

  const handleUpload = async (file: File) => {
    setIsGenerating(true);
    // Call AI Agent API
    const result = await generateBrandingAssets(file);
    // Poll for completion (~5 minutes)
    await pollGenerationStatus(result.jobId);
    setIsGenerating(false);
  };

  return (
    <div>
      <h1>Upload Logo/Image để tạo Branding tự động</h1>
      <ImageUpload onUpload={handleUpload} />
      {isGenerating && (
        <Progress
          value={progress}
          label="Đang tạo 10+ marketing assets..."
        />
      )}
      <AssetPreview assets={branding.generatedAssets} />
    </div>
  );
}
```

### 2.3. Performance Considerations

**Image Optimization:**
- Use Next.js Image component với CDN URLs
- Lazy load section banners
- Preload hero banner for LCP optimization
- WebP format support

**Caching Strategy:**
```typescript
// Cache generated assets in localStorage
const BRANDING_CACHE_KEY = 'kiteclass_branding_assets';

export function useBrandingCache() {
  const getCached = () => {
    const cached = localStorage.getItem(BRANDING_CACHE_KEY);
    if (!cached) return null;
    const { assets, timestamp } = JSON.parse(cached);
    // Cache valid for 7 days
    if (Date.now() - timestamp > 7 * 24 * 60 * 60 * 1000) return null;
    return assets;
  };

  const setCached = (assets: BrandingAssets) => {
    localStorage.setItem(BRANDING_CACHE_KEY, JSON.stringify({
      assets,
      timestamp: Date.now()
    }));
  };

  return { getCached, setCached };
}
```

### 2.4. Security Considerations

**Already Addressed in Architecture:**
- ✅ AI Agent runs in KiteHub (isolated from customer instances)
- ✅ Generated assets stored in CDN (safe HTTPS URLs)
- ✅ No arbitrary CSS injection (only images + pre-defined text)

**Frontend Validation Needed:**
```typescript
// Validate CDN URLs before rendering
function isValidBrandingAssetUrl(url: string): boolean {
  const allowedDomains = [
    'cdn.kiteclass.com',
    'r2.cloudflare.com',
    's3.amazonaws.com'
  ];
  try {
    const urlObj = new URL(url);
    return allowedDomains.some(domain => urlObj.hostname.endsWith(domain));
  } catch {
    return false;
  }
}
```

### 2.5. Action Required

**IN PR 3.2 (Core Infrastructure):**
- [x] Define BrandingAssets and BrandingSettings types (ALREADY in theme-requirements.md)
- [ ] Create branding API client for fetching assets
- [ ] Add asset URL validation utilities

**IN PR 3.3 (Providers):**
- [ ] Implement BrandingProvider with caching
- [ ] Add useBranding hook
- [ ] Handle loading/error states

**IN PR 3.5+ (Admin Dashboard):**
- [ ] Branding upload UI
- [ ] Asset generation progress tracking
- [ ] Asset preview/management interface

---

## 3. PREVIEW WEBSITE FEATURE

### 3.1. Architecture Findings

**STATUS: UNDEFINED** ⚠️

**What We Know:**
- Marked as NEW feature in V3 (comparison table line 1480)
- Listed as: `Preview Website | ❌ V1 | ❌ V2 | ✅ V3 ⭐ NEW`
- KiteHub-level feature (not KiteClass instance feature)

**What We DON'T Know:**
- ❌ What "Preview Website" actually is
- ❌ How it differs from landing page or customer portal
- ❌ Whether it's a demo/trial system
- ❌ Whether it's a marketing site generator for each center
- ❌ Any technical specifications

### 3.2. Hypotheses & Interpretations

**Possible Interpretation 1: Instance Preview Sites**
- Each KiteClass instance gets a public-facing marketing website
- Prospective students can view course catalog before enrolling
- Generated using AI branding assets
- Example URL: `https://abc-academy.kiteclass.com/preview`

**Possible Interpretation 2: KiteHub Demo System**
- Live interactive demo of KiteClass features
- Sandbox environment for trial users
- Example: Click "Xem demo" → Get temporary demo instance

**Possible Interpretation 3: Marketing Landing Page Generator**
- Tool for center owners to create custom landing pages
- Drag-and-drop page builder
- Uses AI-generated branding assets

### 3.3. Critical Questions to Answer

**BEFORE implementing any preview-related features:**

1. **What is the Preview Website feature?**
   - Is it per-instance or platform-wide?
   - Who is the target audience (prospective students vs center owners)?
   - What content does it display?

2. **Technical Architecture:**
   - Static generation or SSR?
   - Separate subdomain or route?
   - Authentication required or public?

3. **Relation to AI Branding:**
   - Does Preview Website use AI-generated assets?
   - Can users customize preview before publishing?

4. **Scope:**
   - Is this a full CMS/page builder?
   - Or just a templated landing page?

### 3.4. Action Required

**IMMEDIATE (Before PR planning):**
- [ ] **Clarify with stakeholders/product owner:** What is "Preview Website"?
- [ ] Document detailed requirements
- [ ] Add specification to system-architecture-v3-final.md

**CANNOT PROCEED** with frontend implementation until this is defined.

---

## 4. GUEST USER SUPPORT & MARKETING PLATFORM

### 4.1. Architecture Findings

**KiteHub Frontend - Public Routes (DOCUMENTED):**
```
src/app/(public)/
  ├── page.tsx                  # Landing page
  ├── pricing/page.tsx          # Pricing page
  ├── features/page.tsx         # Features showcase
  └── contact/page.tsx          # Contact form
```

**Self-Registration (DOCUMENTED):**
```
src/app/(auth)/
  ├── login/page.tsx
  ├── register/page.tsx         # ✅ Customers can self-register
  └── forgot-password/page.tsx
```

**Parent Self-Registration (DOCUMENTED):**
- Zalo OTP-based registration flow
- QR code / link sharing from center
- Service: `ParentRegistrationService.java`
- Flow: QR → Phone entry → OTP → Account creation → Child linking

### 4.2. Trial/Demo Functionality

**Mentioned but NOT Detailed:**
- Landing page has "[Dùng thử miễn phí 14 ngày]" button
- Landing page has "[Xem demo]" button

**GAPS:**
- ❌ What does 14-day trial include?
- ❌ Trial limitations (features, student count, etc.)?
- ❌ How trial-to-paid conversion works?
- ❌ Is trial a full instance or limited sandbox?
- ❌ What happens when trial expires?

### 4.3. KiteClass Instance - Guest Access

**CRITICAL GAP: Undefined**

**Questions:**
1. **Can guests view course catalog without login?**
   - If YES: Need public course listing pages
   - If NO: All content behind authentication

2. **Can prospective students try courses before enrolling?**
   - If YES: Need course preview/demo lessons
   - If NO: Marketing limited to static content

3. **What about SEO for course discovery?**
   - If centers want Google to index courses: Need public SSR pages
   - If privacy required: Keep everything authenticated

4. **Marketing vs Management Dichotomy:**
   - Architecture says "KiteClass đóng vai trò quảng bá hình ảnh, thương hiệu và thu hút học viên mới"
   - BUT: No documentation on HOW to attract students via the platform
   - Current architecture only shows management features (classes, attendance, grades)

### 4.4. Proposed Guest User Flows

**Flow 1: Prospective Student Discovery (NEEDS DESIGN):**
```
Guest visits https://abc-academy.kiteclass.com
  ↓
Sees public landing page with:
  - Center branding (AI-generated)
  - Course catalog (public view)
  - Testimonials
  - "Đăng ký học thử" button
  ↓
Guest browses courses → Clicks "Học thử"
  ↓
Registration form (name, phone, email)
  ↓
OTP verification (Zalo)
  ↓
Trial account created → Limited course access
  ↓
Conversion to paid enrollment
```

**Flow 2: Parent Discovery (DOCUMENTED):**
```
Center shares QR code on social media/poster
  ↓
Parent scans QR
  ↓
Lands on registration page (public route)
  ↓
Enters phone number → Zalo OTP
  ↓
Account created → Links to children
  ↓
Access to Parent Portal
```

**Flow 3: Center Owner Trial (NEEDS DESIGN):**
```
Customer visits kiteclass.com
  ↓
Clicks "Dùng thử miễn phí 14 ngày"
  ↓
Registration form
  ↓
Trial instance provisioned automatically
  ↓
Access to limited/full features for 14 days
  ↓
Upgrade prompt → Payment → Full instance
```

### 4.5. Frontend Requirements

**1. Public Landing Page for KiteClass Instances (NEEDS DECISION):**
```typescript
// IF this is required, need these routes:
src/app/(kiteclass-instance)/(public)/
  ├── page.tsx                  # Instance landing page
  ├── courses/page.tsx          # Public course catalog
  ├── courses/[id]/page.tsx     # Course detail (preview)
  ├── about/page.tsx            # About the center
  └── contact/page.tsx          # Contact form
```

**2. Guest User Components:**
```typescript
// src/components/guest/CoursePreview.tsx
function CoursePreview({ course }: { course: Course }) {
  const isGuest = useAuth().user === null;

  return (
    <Card>
      <CourseImage src={course.thumbnail} />
      <CourseTitle>{course.name}</CourseTitle>
      <CourseDescription>{course.description}</CourseDescription>
      {isGuest ? (
        <Button onClick={handleTrialSignup}>
          Đăng ký học thử
        </Button>
      ) : (
        <Button onClick={handleEnroll}>
          Ghi danh
        </Button>
      )}
    </Card>
  );
}
```

**3. Trial Account Management:**
```typescript
// src/types/user.ts
interface User {
  id: string;
  email: string;
  role: Role;
  accountType: 'TRIAL' | 'PAID' | 'FREE';
  trialExpiresAt?: Date;
  limitations?: {
    maxStudents?: number;
    maxCourses?: number;
    featureAccess?: string[];
  };
}

// Trial expiration warning
function TrialExpirationBanner() {
  const { user } = useAuth();

  if (user?.accountType !== 'TRIAL') return null;

  const daysLeft = getDaysUntilExpiration(user.trialExpiresAt);

  return (
    <Alert variant="warning">
      <AlertTitle>Thời gian dùng thử còn {daysLeft} ngày</AlertTitle>
      <Button onClick={handleUpgrade}>Nâng cấp ngay</Button>
    </Alert>
  );
}
```

**4. SEO for Course Discovery:**
```typescript
// src/app/(public)/courses/[id]/page.tsx
export async function generateMetadata({ params }): Promise<Metadata> {
  const course = await getCourse(params.id);

  return {
    title: `${course.name} - ABC Academy`,
    description: course.description,
    openGraph: {
      images: [course.thumbnail],
    },
  };
}
```

### 4.6. Testing Requirements

**Guest User Testing:**
```typescript
// src/__tests__/guest-access.test.ts
describe('Guest Access', () => {
  it('should allow guests to view course catalog', async () => {
    render(<CourseCatalog />);
    expect(await screen.findByText('Khóa học lập trình')).toBeInTheDocument();
  });

  it('should show trial signup for guests', () => {
    render(<CourseDetail courseId="123" />, { user: null });
    expect(screen.getByText('Đăng ký học thử')).toBeInTheDocument();
  });

  it('should show enroll button for authenticated users', () => {
    render(<CourseDetail courseId="123" />, { user: mockStudent });
    expect(screen.getByText('Ghi danh')).toBeInTheDocument();
  });
});
```

### 4.7. Action Required

**IMMEDIATE (Before PR planning):**
- [ ] **Decide:** Do KiteClass instances have public-facing pages?
- [ ] **Decide:** Can guests view course catalog? Try courses?
- [ ] **Design:** 14-day trial system specifications
- [ ] **Design:** Trial-to-paid conversion flow
- [ ] **Design:** Trial account limitations

**IN PR 3.4+ (Public Routes):**
- [ ] Implement instance landing page (if required)
- [ ] Public course catalog (if required)
- [ ] Trial signup flow

**IN PR 3.6+ (Auth System):**
- [ ] Trial account types
- [ ] Trial expiration handling
- [ ] Limitation enforcement

---

## IMPACT ON IMPLEMENTATION PLAN

### PRs Affected by These Gaps

**PR 3.1 - Project Setup:** ✅ No impact (already complete)

**PR 3.2 - Core Infrastructure:**
- ⚠️ Need Feature Detection types
- ⚠️ Need BrandingAssets types (DONE in theme-requirements)
- ⚠️ Need Trial/Guest user types

**PR 3.3 - Providers & Context:**
- ⚠️ Need FeatureFlagProvider design
- ⚠️ Need BrandingProvider design
- ⚠️ Unclear on AuthProvider guest handling

**PR 3.4 - Public Routes:**
- 🔴 BLOCKED until Preview Website is defined
- 🔴 BLOCKED until Guest Access policies are defined
- 🔴 BLOCKED until Trial system is designed

**PR 3.5+ - Admin Dashboard:**
- ⚠️ Need Branding Upload UI specs
- ⚠️ Need tier-based feature visibility

---

## RECOMMENDATIONS

### Priority 1: IMMEDIATE CLARIFICATION NEEDED ⚠️

**BEFORE continuing frontend PRs:**
1. **Define "Preview Website" feature** with product owner
2. **Design Feature Detection API** with backend team
3. **Decide Guest Access policies** (public course catalog? trial learning?)
4. **Design 14-day Trial system** (limitations, conversion flow)

**Estimated Impact:** 1-2 days of design work, will save weeks of rework

### Priority 2: UPDATE ARCHITECTURE DOCUMENT 📝

**Add missing sections to system-architecture-v3-final.md:**
1. Preview Website feature specification
2. Feature Detection/Feature Flag system
3. Guest User access policies
4. Trial system architecture
5. Tier-based UI differentiation patterns

### Priority 3: UPDATE FRONTEND SKILLS 🎯

**Add to .claude/skills/frontend-code-quality.md:**
1. Feature Flag patterns and testing
2. Branding asset integration patterns
3. Guest user handling best practices
4. Trial account limitations UI patterns

### Priority 4: UPDATE IMPLEMENTATION PLAN 📋

**Adjust PR sequencing:**
- PR 3.2: Add Feature Detection API client + types
- PR 3.3: Add FeatureFlagProvider + BrandingProvider
- PR 3.4: WAIT for Preview Website + Guest Access decisions
- PR 3.5+: Proceed with conditional rendering based on feature flags

---

## CONCLUSION

**2 vấn đề đã clear:**
1. ✅ **AI Branding System**: Fully documented, ready to implement
2. ✅ **Pricing Tiers**: Structure clear, chỉ cần thiết kế Feature Detection API

**2 vấn đề cần clarification URGENT:**
3. 🔴 **Preview Website**: Completely undefined - BLOCKING PR 3.4+
4. 🔴 **Guest User Support**: Partially defined - Need decisions on access policies

**Next Steps:**
1. Present report to stakeholders
2. Schedule architecture clarification meeting
3. Update system-architecture document
4. Resume frontend PRs with clear requirements

---

**Prepared by:** Claude Sonnet 4.5
**For:** KiteClass Platform V3 Implementation
**Related Documents:**
- `/mnt/e/person/2026-Kite-Class-Platform/documents/reports/system-architecture-v3-final.md`
- `/mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md`
- `/tmp/claude/.../scratchpad/theme-requirements.md`
