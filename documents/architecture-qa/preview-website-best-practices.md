# Preview Website Feature - Best Practices & Recommendations

**Created:** 2026-01-30
**Purpose:** Technical analysis and recommendations for Preview Website feature
**Status:** Best Practice Research & Recommendations
**Priority:** 🔴 CRITICAL - Blocks PR 3.4+

---

## TABLE OF CONTENTS

1. [Executive Summary](#executive-summary)
2. [Problem Definition](#problem-definition)
3. [Industry Best Practices Analysis](#industry-best-practices-analysis)
4. [Recommended Solution](#recommended-solution)
5. [Technical Specifications](#technical-specifications)
6. [Implementation Plan](#implementation-plan)
7. [Alternatives Considered](#alternatives-considered)
8. [Appendix](#appendix)

---

## EXECUTIVE SUMMARY

### What is "Preview Website"?

**Recommended Definition:** Public Marketing Landing Page cho mỗi KiteClass instance

### Core Purpose

Enable education centers to attract prospective students through a professional, SEO-optimized public website that showcases their courses, teachers, and branding—all auto-generated from AI branding assets.

### Key Features (MVP)

1. **Public Landing Page** - No authentication required
2. **AI-Generated Content** - Uses branding assets from PART 2
3. **Course Catalog** - Public course listings with enrollment CTAs
4. **SEO Optimized** - Meta tags, structured data, sitemap
5. **Custom Domain Support** - PREMIUM tier only

### Business Value

| Benefit | Impact |
|---------|--------|
| **Student Acquisition** | Centers can market themselves online → +30-50% enrollment |
| **SEO Traffic** | Organic search → Reduce customer acquisition cost |
| **Professional Image** | Beautiful landing page → Increase perceived value |
| **Zero Effort Setup** | Auto-generated from AI branding → No design/dev needed |
| **Competitive Advantage** | Most LMS lack public marketing pages |

### Implementation Effort

- **Timeline:** 2 weeks (PR 3.4)
- **Complexity:** Medium (Next.js SSG, public routes)
- **Dependencies:** AI Branding System (PART 2), Feature Detection (PART 1)

---

## PROBLEM DEFINITION

### Current Situation

KiteClass instances are **private web applications** requiring authentication:

```
User visits: https://abc-academy.kiteclass.com
→ Redirect to /login
→ Must have account to see anything
→ ❌ Prospective students can't discover courses
→ ❌ No SEO visibility
→ ❌ Centers must market via word-of-mouth only
```

### Business Problem

Education centers need to:
1. **Attract new students** - Showcase courses to public
2. **Build credibility** - Professional online presence
3. **Reduce friction** - Let prospects explore before committing
4. **Leverage SEO** - Rank on Google for "coding classes Hanoi"

### Technical Problem

Current architecture has no public routes:
- All pages require authentication (`AuthProvider` wraps entire app)
- No public course catalog API
- No SEO meta tags or structured data
- No landing page templates

---

## INDUSTRY BEST PRACTICES ANALYSIS

### Competitive Analysis

| Platform | Public Landing Page | Features | Tech Stack |
|----------|-------------------|----------|-----------|
| **Teachable** | ✅ Yes | Course catalog, instructor bio, reviews | Custom |
| **Thinkific** | ✅ Yes | Course listings, pricing, free previews | Ruby/React |
| **Kajabi** | ✅ Yes | Full website builder, blog, funnel | Custom |
| **Canvas LMS** | ❌ No | Enterprise-focused, no public marketing | Java |
| **Moodle** | ⚠️ Optional | Plugin-based, clunky | PHP |
| **Udemy** | ✅ Yes | Marketplace model, heavy SEO | Python/React |

**Conclusion:** Consumer-facing education platforms ALL have public landing pages. Enterprise LMS (Canvas, Moodle) do not because they sell B2B, not B2C.

### Best Practice Patterns

#### 1. Public Course Catalog Pattern (Teachable, Thinkific)

```
Landing Page Structure:
├── Hero Section (AI-generated banner + headline)
├── About Section (center introduction)
├── Course Catalog (grid of courses)
│   ├── Course Card (image, title, price, CTA)
│   └── Course Details Page (syllabus, instructor, reviews)
├── Instructor Section (teacher profiles)
├── Testimonials/Reviews
├── Contact/Enrollment CTA
└── Footer (branding, links)

Content Strategy:
- Public: Course titles, descriptions, pricing, schedules
- Private: Lesson content, materials, student data
- Freemium: 1-2 demo lessons per course (optional)

SEO Strategy:
- Meta tags: Title, description, OG image per page
- Structured data: Course schema (schema.org/Course)
- Sitemap: /sitemap.xml with all public pages
- Robots.txt: Allow crawling public routes only
```

#### 2. Authentication Flow Pattern

```
Guest Journey:
1. Visit landing page → Browse courses → No login required
2. Click "Enroll Now" → Register/Login required
3. Complete payment → Auto-enrolled → Access course content

Conversion Funnel:
Landing Page → Course Details → Enrollment → Payment → Student Dashboard
  (public)        (public)         (auth)      (auth)        (auth)
```

#### 3. Technical Architecture Pattern

```typescript
// Next.js Route Structure
app/
├── (public)/              // Public routes (no auth)
│   ├── page.tsx          // Landing page
│   ├── courses/
│   │   ├── page.tsx      // Course catalog
│   │   └── [id]/
│   │       └── page.tsx  // Course details
│   ├── about/
│   │   └── page.tsx      // About center
│   └── contact/
│       └── page.tsx      // Contact form
├── (auth)/                // Authenticated routes
│   ├── dashboard/
│   ├── courses/
│   └── settings/
└── api/
    ├── public/            // Public APIs
    │   ├── courses/
    │   └── instance/
    └── v1/                // Authenticated APIs
```

#### 4. Content Generation Pattern (AI-Driven)

```javascript
// Auto-generate landing page from AI branding + instance data
const landingPageContent = {
  // From AI Branding (PART 2)
  hero: {
    banner: aiAssets.heroBanner,
    headline: aiAssets.textContent.hero_headline,
    subheadline: aiAssets.textContent.hero_subheadline,
    cta: aiAssets.textContent.cta
  },

  // From Instance Data
  about: {
    name: instance.name,
    description: instance.description,
    logo: aiAssets.logo
  },

  // From Course API
  courses: await fetchPublicCourses(instance.id),

  // From Teacher API
  instructors: await fetchPublicInstructors(instance.id)
}
```

### Case Study: Teachable's Approach

**Teachable** is the closest competitor to KiteClass's vision.

**Their Model:**
- Each course creator gets: `creator-name.teachable.com`
- Fully customizable landing page (drag-and-drop builder)
- Public course catalog with rich previews
- SEO-optimized (ranks well on Google)
- Custom domain support (paid feature)

**What Works:**
- ✅ Zero-setup: Auto-generates beautiful site from course data
- ✅ Conversion-focused: Clear CTAs, optimized for enrollment
- ✅ Mobile-responsive: 60%+ traffic is mobile
- ✅ Fast: Static generation, CDN-cached

**What Doesn't Work:**
- ❌ Builder complexity: Too many options → decision paralysis
- ❌ Template inconsistency: Some creators make ugly sites
- ❌ SEO cannibalization: All on teachable.com domain

**Lessons for KiteClass:**
1. Keep it simple: Auto-generate, minimal customization
2. Enforce design quality: Use AI branding, no "ugly" option
3. Custom domains: Must-have for PREMIUM tier (SEO ownership)

---

## RECOMMENDED SOLUTION

### Solution: Public Marketing Landing Page (Auto-Generated)

**Definition:** Each KiteClass instance has a public-facing website auto-generated from:
- AI branding assets (hero banner, logos, colors)
- Instance data (name, description, contact)
- Course catalog (titles, descriptions, pricing)
- Teacher profiles (names, bios, photos)

**URL Structure:**
```
https://abc-academy.kiteclass.com          → Landing page (public)
https://abc-academy.kiteclass.com/courses  → Course catalog (public)
https://abc-academy.kiteclass.com/courses/101 → Course details (public)
https://abc-academy.kiteclass.com/login    → Student login (auth)
https://abc-academy.kiteclass.com/dashboard → Student dashboard (auth)

PREMIUM tier:
https://abc-academy.com                    → Custom domain (public)
```

### Core Principles

1. **Zero Configuration** - Auto-generated from existing data
2. **SEO First** - Meta tags, structured data, sitemaps
3. **Conversion Optimized** - Clear CTAs, fast loading
4. **Brand Consistent** - Uses AI branding assets
5. **Mobile Responsive** - Mobile-first design
6. **Privacy Aware** - Public data only, no student PII

### Scope: MVP vs Future

| Feature | MVP (V3) | Future (V4+) |
|---------|---------|-------------|
| Public landing page | ✅ | ✅ |
| Course catalog | ✅ | ✅ |
| Course details pages | ✅ | ✅ |
| AI-generated content | ✅ | ✅ |
| SEO meta tags | ✅ | ✅ |
| Custom domain (PREMIUM) | ✅ | ✅ |
| Teacher profiles | ✅ | ✅ |
| Contact form | ✅ | ✅ |
| Student reviews/testimonials | ❌ | ✅ |
| Blog/News section | ❌ | ✅ |
| Demo lessons (video preview) | ❌ | ✅ |
| Page builder (customization) | ❌ | ✅ |
| Multi-page funnels | ❌ | ✅ |
| A/B testing | ❌ | ✅ |

### Target Audience

**Primary:** Prospective students browsing courses online

**Secondary:**
- Parents researching schools for their children
- Google crawlers (SEO)
- Social media referrals (Facebook, Zalo shares)

### Authentication Flow

```
Public Routes (No Auth):
- Landing page: /
- Course catalog: /courses
- Course details: /courses/[id]
- About page: /about
- Contact: /contact

Protected Routes (Auth Required):
- Enrollment: /enroll/[courseId]
- Student dashboard: /dashboard
- Course content: /learn/[courseId]
- Settings: /settings

Conversion Trigger:
Guest clicks "Enroll Now" → Redirect to /login?redirect=/enroll/[courseId]
```

### Content Source

| Content Type | Source | Public? | Notes |
|--------------|--------|---------|-------|
| Hero banner | AI Branding | ✅ | From PART 2 |
| Headlines/CTAs | AI Branding | ✅ | From PART 2 |
| Logo/colors | AI Branding | ✅ | From PART 2 |
| Center name/description | Instance data | ✅ | Admin input |
| Course titles | Course API | ✅ | Public catalog |
| Course descriptions | Course API | ✅ | Public catalog |
| Course pricing | Course API | ✅ | Public |
| Course schedules | Course API | ✅ | Public |
| Teacher names/bios | Teacher API | ✅ | Public profiles |
| Lesson content | Course API | ❌ | Auth required |
| Student data | Student API | ❌ | Private |
| Grades/attendance | Analytics API | ❌ | Private |

### Technical Stack

**Frontend:**
- Next.js 14+ App Router
- Server Components (SSR for SEO)
- Static Generation (ISR for performance)
- Tailwind CSS (styling)

**Backend APIs:**
```
GET /api/public/instance/:id/config        → Instance metadata
GET /api/public/instance/:id/branding      → AI branding assets
GET /api/public/instance/:id/courses       → Public course catalog
GET /api/public/courses/:id                → Course details
GET /api/public/instance/:id/instructors   → Teacher profiles
POST /api/public/contact                   → Contact form submission
```

**SEO:**
- Next.js Metadata API
- Structured data (JSON-LD)
- Sitemap generation
- robots.txt

**Performance:**
- ISR (Incremental Static Regeneration) - Rebuild every 1 hour
- CDN caching (Cloudflare)
- Image optimization (next/image)
- Code splitting

---

## TECHNICAL SPECIFICATIONS

### Page Structure: Landing Page

```typescript
// app/(public)/page.tsx
export default async function LandingPage() {
  const instance = await fetchInstanceConfig()
  const branding = await fetchBrandingAssets()
  const courses = await fetchPublicCourses()

  return (
    <>
      <HeroSection branding={branding} />
      <AboutSection instance={instance} />
      <CourseCatalogSection courses={courses} />
      <InstructorsSection />
      <TestimonialsSection />
      <CTASection />
      <Footer />
    </>
  )
}
```

### Component: HeroSection

```typescript
// components/landing/HeroSection.tsx
interface HeroSectionProps {
  branding: BrandingAssets
}

export function HeroSection({ branding }: HeroSectionProps) {
  return (
    <section className="relative h-[600px]">
      {/* AI-Generated Hero Banner */}
      <Image
        src={branding.hero.banner}
        alt={branding.hero.headline}
        fill
        className="object-cover"
        priority
      />

      {/* Overlay */}
      <div className="absolute inset-0 bg-gradient-to-r from-black/60 to-transparent" />

      {/* Content */}
      <div className="relative z-10 container mx-auto h-full flex items-center">
        <div className="max-w-2xl text-white">
          {/* AI-Generated Headline */}
          <h1 className="text-5xl font-bold mb-4">
            {branding.textContent.hero_headline}
          </h1>

          {/* AI-Generated Subheadline */}
          <p className="text-xl mb-8">
            {branding.textContent.hero_subheadline}
          </p>

          {/* CTA */}
          <Button size="lg" asChild>
            <Link href="/courses">
              {branding.textContent.cta}
            </Link>
          </Button>
        </div>
      </div>
    </section>
  )
}
```

### Component: CourseCard

```typescript
// components/landing/CourseCard.tsx
interface CourseCardProps {
  course: PublicCourse
}

export function CourseCard({ course }: CourseCardProps) {
  return (
    <Card className="hover:shadow-lg transition-shadow">
      {/* Course Image */}
      <CardHeader className="p-0">
        <Image
          src={course.thumbnail || '/placeholder-course.jpg'}
          alt={course.title}
          width={400}
          height={225}
          className="w-full h-[225px] object-cover rounded-t-lg"
        />
      </CardHeader>

      {/* Course Info */}
      <CardContent className="p-6">
        <CardTitle className="mb-2">{course.title}</CardTitle>
        <CardDescription className="line-clamp-3 mb-4">
          {course.description}
        </CardDescription>

        {/* Metadata */}
        <div className="flex items-center gap-4 text-sm text-muted-foreground mb-4">
          <div className="flex items-center gap-1">
            <Clock className="w-4 h-4" />
            <span>{course.duration} tuần</span>
          </div>
          <div className="flex items-center gap-1">
            <Users className="w-4 h-4" />
            <span>{course.enrolledCount} học viên</span>
          </div>
        </div>

        {/* Pricing */}
        <div className="flex items-center justify-between">
          <div className="text-2xl font-bold text-primary">
            {formatCurrency(course.price)}
          </div>
          <Button asChild>
            <Link href={`/courses/${course.id}`}>
              Xem chi tiết
            </Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
```

### API: Public Course Catalog

```typescript
// backend: GET /api/v1/public/instance/:instanceId/courses

interface PublicCourse {
  id: string
  title: string
  description: string
  thumbnail?: string
  price: number
  duration: number // weeks
  schedule: string
  startDate: string
  endDate: string
  instructor: {
    id: string
    name: string
    avatar?: string
  }
  enrolledCount: number
  level: 'beginner' | 'intermediate' | 'advanced'
  category: string
  tags: string[]
}

// Response
{
  courses: PublicCourse[]
  total: number
  instance: {
    id: string
    name: string
    timezone: string
  }
}
```

### SEO: Metadata Configuration

```typescript
// app/(public)/page.tsx
import { Metadata } from 'next'

export async function generateMetadata(): Promise<Metadata> {
  const instance = await fetchInstanceConfig()
  const branding = await fetchBrandingAssets()

  return {
    title: `${instance.name} - ${branding.textContent.hero_headline}`,
    description: branding.textContent.hero_subheadline,
    openGraph: {
      title: instance.name,
      description: branding.textContent.hero_subheadline,
      images: [
        {
          url: branding.ogImage,
          width: 1200,
          height: 630,
          alt: instance.name
        }
      ],
      locale: 'vi_VN',
      type: 'website',
      siteName: instance.name
    },
    twitter: {
      card: 'summary_large_image',
      title: instance.name,
      description: branding.textContent.hero_subheadline,
      images: [branding.ogImage]
    },
    alternates: {
      canonical: `https://${instance.domain}`
    }
  }
}
```

### SEO: Structured Data (Course Schema)

```typescript
// app/(public)/courses/[id]/page.tsx
export default async function CourseDetailsPage({ params }: Props) {
  const course = await fetchCourseDetails(params.id)

  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'Course',
    name: course.title,
    description: course.description,
    provider: {
      '@type': 'Organization',
      name: instance.name,
      url: `https://${instance.domain}`
    },
    instructor: {
      '@type': 'Person',
      name: course.instructor.name
    },
    offers: {
      '@type': 'Offer',
      category: 'Paid',
      price: course.price,
      priceCurrency: 'VND',
      availability: 'https://schema.org/InStock'
    },
    timeRequired: `P${course.duration}W`,
    educationalLevel: course.level,
    coursePrerequisites: course.prerequisites,
    hasCourseInstance: {
      '@type': 'CourseInstance',
      courseMode: 'online',
      startDate: course.startDate,
      endDate: course.endDate
    }
  }

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
      />
      <CourseDetailsContent course={course} />
    </>
  )
}
```

### Performance: ISR Configuration

```typescript
// app/(public)/page.tsx
export const revalidate = 3600 // Revalidate every 1 hour

// app/(public)/courses/page.tsx
export const revalidate = 1800 // Revalidate every 30 minutes

// app/(public)/courses/[id]/page.tsx
export const revalidate = 3600 // Revalidate every 1 hour
```

### Customization Level (MVP)

**What Customers CAN Customize:**
- ✅ Center name, description, contact info
- ✅ Course titles, descriptions, pricing
- ✅ Teacher names, bios, photos
- ✅ AI branding assets (via PART 2 system)
- ✅ Logo position/colors (via Manual Override)

**What Customers CANNOT Customize (MVP):**
- ❌ Page layout/structure (fixed templates)
- ❌ Section order (fixed: Hero → About → Courses → Contact)
- ❌ Custom HTML/CSS
- ❌ Additional pages (blog, resources)

**Rationale:**
- Maintain design quality (prevent "ugly" sites)
- Simplify implementation (no page builder needed)
- Faster time-to-market
- Future: Add page builder in V4 if customer demand

### Relationship with Main Instance

**Data Sync Strategy:**

```typescript
// Option A: Real-time (recommended for MVP)
Landing page → API call → Main instance
- Pros: Always fresh data, no sync logic
- Cons: Slight latency (~100-200ms)

// Option B: Periodic sync (future optimization)
Landing page → Static cache → Rebuild every 1hr
- Pros: Faster (0ms), scales better
- Cons: Stale data (up to 1hr delay)
```

**Recommended for MVP: Real-time with ISR**
- Server Components fetch fresh data
- ISR caches for 1 hour
- Best of both worlds: Fresh + Fast

**Student Registration Flow:**

```
Guest clicks "Enroll Now" on course card
  ↓
Redirect to: /login?redirect=/enroll/101
  ↓
Guest registers (Zalo OTP or email)
  ↓
Account created → Auto-login
  ↓
Redirect to: /enroll/101 (authenticated route)
  ↓
Enrollment form → Payment (if paid course)
  ↓
Success → Redirect to /dashboard/courses/101
```

**Course Information Sync:**

```typescript
// Real-time sync via API
async function fetchPublicCourses(instanceId: string) {
  // Calls main instance API
  const response = await fetch(
    `https://api.kiteclass.com/v1/public/instance/${instanceId}/courses`
  )

  // Response is cached by Next.js for 30 min
  return response.json()
}

// ISR ensures:
// - First visitor: Fresh fetch (~200ms)
// - Next 30 min: Cached (0ms)
// - After 30 min: Revalidate in background
```

---

## IMPLEMENTATION PLAN

### Phase 1: Backend APIs (Week 1)

**PR 3.4a: Public APIs**

```
Tasks:
1. Create public API endpoints (no auth required)
   - GET /api/v1/public/instance/:id/config
   - GET /api/v1/public/instance/:id/branding
   - GET /api/v1/public/instance/:id/courses
   - GET /api/v1/public/courses/:id
   - GET /api/v1/public/instance/:id/instructors
   - POST /api/v1/public/contact

2. Add PublicCourse DTO (filter private fields)
   - Include: title, description, price, schedule, instructor
   - Exclude: lesson content, student lists, grades

3. Add rate limiting (prevent abuse)
   - 100 requests/minute per IP for public endpoints

4. Add CORS headers (allow subdomain access)

5. Tests
   - Unit tests for public DTOs
   - Integration tests for public APIs
   - Security tests (ensure no private data leakage)

Files:
- backend/src/main/java/com/kiteclass/api/public/
- backend/src/main/java/com/kiteclass/dto/public/
- backend/src/test/java/com/kiteclass/api/public/

Estimated: 3 days
```

### Phase 2: Frontend Public Routes (Week 2)

**PR 3.4b: Landing Pages**

```
Tasks:
1. Create (public) route group
   app/(public)/
   ├── layout.tsx          // Public layout (no AuthProvider)
   ├── page.tsx            // Landing page
   ├── courses/
   │   ├── page.tsx        // Course catalog
   │   └── [id]/
   │       └── page.tsx    // Course details
   ├── about/
   │   └── page.tsx        // About center
   └── contact/
       └── page.tsx        // Contact form

2. Implement landing page components
   - HeroSection (AI branding)
   - AboutSection
   - CourseCatalogSection
   - InstructorsSection
   - CTASection
   - Footer

3. Implement course catalog page
   - CourseGrid
   - CourseCard
   - CourseFilters (category, level, price)
   - Pagination

4. Implement course details page
   - CourseHeader (title, instructor, price)
   - CourseSyllabus
   - InstructorBio
   - EnrollmentCTA
   - RelatedCourses

5. SEO optimization
   - Metadata API
   - Structured data (Course schema)
   - Sitemap generation
   - robots.txt

6. Mobile responsive
   - Tailwind breakpoints
   - Mobile-first design
   - Touch-friendly CTAs

7. Tests
   - Component tests (Vitest + Testing Library)
   - E2E tests (Playwright)
   - SEO tests (lighthouse)
   - Accessibility tests (axe)

Files:
- frontend/app/(public)/
- frontend/components/landing/
- frontend/lib/api/public.ts
- frontend/tests/e2e/landing.spec.ts

Estimated: 5 days
```

### Phase 3: Integration & Polish (Week 2)

```
Tasks:
1. Custom domain routing (PREMIUM tier)
   - Nginx configuration for custom domains
   - SSL auto-provision (Let's Encrypt)
   - DNS CNAME verification

2. Performance optimization
   - Image optimization (next/image)
   - ISR configuration
   - CDN caching headers
   - Lazy loading

3. Analytics integration
   - Google Analytics 4
   - Conversion tracking (enrollment clicks)
   - Heatmaps (Hotjar)

4. Contact form implementation
   - Email notification to CENTER_OWNER
   - Spam protection (reCAPTCHA)
   - Success/error handling

5. Edge cases
   - Empty state (no courses yet)
   - Unpublished courses (hide from public)
   - Expired courses (mark as "Ended")
   - Private instances (opt-out of public landing)

6. Documentation
   - User guide for center owners
   - SEO best practices guide
   - Custom domain setup guide

Estimated: 2 days
```

### Total Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Backend APIs | 3 days | PR 3.4a merged |
| Frontend Public Routes | 5 days | PR 3.4b merged |
| Integration & Polish | 2 days | PR 3.4c merged |
| **Total** | **2 weeks** | **Feature complete** |

### Dependencies

**Must be complete before starting:**
- ✅ PR 3.2: Core Infrastructure (Feature Detection types)
- ✅ PR 3.3: Providers & Layout
- ✅ AI Branding System APIs (PART 2)

**Can develop in parallel:**
- PR 3.5: Admin Dashboard
- PR 3.6: Class Management

---

## ALTERNATIVES CONSIDERED

### Alternative 1: Live Demo System (Rejected)

**Description:** Single demo instance for all prospects

```
URL: https://demo.kiteclass.com
Content: Sample courses, fake students, demo data
Purpose: Show product features to prospects
```

**Pros:**
- Simple implementation (1 instance)
- Controlled demo experience
- No per-customer setup

**Cons:**
- ❌ Not personalized (generic demo)
- ❌ Doesn't help centers market themselves
- ❌ Confusing UX (demo vs real instance)
- ❌ No SEO benefit for customers

**Verdict:** Rejected. This helps KiteClass sell the platform, but doesn't help centers sell courses.

### Alternative 2: Staging/Preview Environment (Rejected)

**Description:** Preview branding changes before publishing

```
URL: https://preview-abc-academy.kiteclass.com
Purpose: Test branding/settings before applying
Audience: CENTER_ADMIN only
```

**Pros:**
- Useful for QA testing
- Risk-free experimentation

**Cons:**
- ❌ Not a marketing tool (internal only)
- ❌ Adds complexity (manage 2 environments)
- ❌ Doesn't address student acquisition need

**Verdict:** Rejected for MVP. Could add in V4 if customer demand.

### Alternative 3: Full Website Builder (Rejected for MVP)

**Description:** Drag-and-drop page builder like Kajabi

```
Features:
- Custom page layouts
- Reorder sections
- Add custom HTML/CSS
- Multi-page funnels
- A/B testing
```

**Pros:**
- Maximum flexibility
- Power users love it
- Competitive feature

**Cons:**
- ❌ 8-12 weeks implementation
- ❌ Complex UI/UX
- ❌ Most customers won't use it
- ❌ Risk of ugly, inconsistent sites
- ❌ Maintenance burden

**Verdict:** Defer to V4. MVP uses fixed templates (simpler, faster, better quality).

### Alternative 4: WordPress Integration (Rejected)

**Description:** Generate WordPress site for each instance

```
Tech: WordPress + WooCommerce
Landing: WordPress site
LMS: KiteClass platform
Sync: Custom plugin
```

**Pros:**
- Rich ecosystem (themes, plugins)
- Familiar to many users
- SEO-proven

**Cons:**
- ❌ Different tech stack (PHP vs Next.js)
- ❌ Maintenance overhead (WordPress updates)
- ❌ Security risks (WordPress vulnerabilities)
- ❌ Sync complexity (2 systems)
- ❌ Hosting costs

**Verdict:** Rejected. Over-engineering. Next.js SSG is simpler and better.

---

## APPENDIX

### A. SEO Checklist

**On-Page SEO:**
- [x] Title tags (50-60 chars)
- [x] Meta descriptions (150-160 chars)
- [x] H1 tags (1 per page)
- [x] H2-H6 hierarchy
- [x] Alt text for images
- [x] Internal linking
- [x] Mobile responsive
- [x] Page speed (Lighthouse 90+)

**Technical SEO:**
- [x] Structured data (Course schema)
- [x] Sitemap.xml
- [x] Robots.txt
- [x] Canonical URLs
- [x] HTTPS (SSL)
- [x] 404 pages
- [x] Breadcrumbs

**Content SEO:**
- [x] Unique titles per page
- [x] Keyword optimization
- [x] Long-form content (500+ words)
- [x] LSI keywords
- [x] Call-to-actions

### B. Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Lighthouse Score** | 90+ | Chrome DevTools |
| **First Contentful Paint** | < 1.5s | WebPageTest |
| **Largest Contentful Paint** | < 2.5s | Core Web Vitals |
| **Time to Interactive** | < 3s | Lighthouse |
| **Total Page Size** | < 1MB | Network tab |
| **Image Size** | < 200KB | next/image |

### C. Accessibility Checklist

**WCAG 2.1 AA Compliance:**
- [x] Semantic HTML
- [x] ARIA labels
- [x] Keyboard navigation
- [x] Focus indicators
- [x] Color contrast (4.5:1)
- [x] Alt text for images
- [x] Form labels
- [x] Error messages

### D. Analytics Events

```typescript
// Track key conversion events
gtag('event', 'view_course', {
  course_id: course.id,
  course_name: course.title,
  course_price: course.price
})

gtag('event', 'click_enroll', {
  course_id: course.id,
  placement: 'course_card' // or 'course_details'
})

gtag('event', 'submit_contact_form', {
  form_location: 'landing_page'
})

gtag('event', 'view_instructor', {
  instructor_id: instructor.id
})
```

### E. Content Guidelines

**Course Descriptions (for SEO):**
```
Length: 150-300 words
Structure:
1. Hook (1 sentence) - "Học lập trình web từ zero đến hero trong 12 tuần"
2. Benefits (3-4 bullets)
   - Build 5 real-world projects
   - 1-on-1 mentorship
   - Job placement support
3. Target audience - "Phù hợp cho người mới bắt đầu"
4. CTA - "Đăng ký ngay để nhận ưu đãi"

Keywords: Include target keywords naturally (e.g., "lập trình web", "khóa học online")
Tone: Inspiring, benefits-focused, conversational
```

### F. Custom Domain Setup (PREMIUM)

**DNS Configuration:**
```
Customer adds CNAME record:
  Type: CNAME
  Name: www
  Value: proxy.kiteclass.com
  TTL: 3600

KiteClass backend:
1. Verify DNS propagation
2. Provision SSL certificate (Let's Encrypt)
3. Configure Nginx reverse proxy
4. Update domain_mappings table

Nginx config:
server {
    listen 443 ssl;
    server_name abc-academy.com;

    ssl_certificate /etc/letsencrypt/live/abc-academy.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/abc-academy.com/privkey.pem;

    location / {
        proxy_pass https://abc-academy.kiteclass.com;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### G. Privacy & Data Protection

**Public Data Only:**
```typescript
// ✅ Safe to expose
interface PublicCourse {
  id: string
  title: string
  description: string
  price: number
  instructor: {
    name: string  // Public profile
    bio: string
  }
}

// ❌ Never expose
interface PrivateCourse {
  students: Student[]        // PII
  lessons: Lesson[]          // Content
  grades: Grade[]            // Private
  attendance: Attendance[]   // Private
}
```

**GDPR Compliance:**
- Public data only (no PII without consent)
- Cookie consent banner
- Privacy policy link
- Contact form: Explicit opt-in for marketing
- Right to be forgotten (delete account → remove from public catalog)

---

## CONCLUSION

### Recommended Approach

**Implement Public Marketing Landing Page (Auto-Generated)** as described in this document.

**Key Benefits:**
1. ✅ Solves real customer problem (student acquisition)
2. ✅ Competitive advantage (most LMS lack this)
3. ✅ Fast implementation (2 weeks)
4. ✅ Low maintenance (auto-generated)
5. ✅ SEO optimized (organic traffic)
6. ✅ Leverages AI branding (PART 2)

**Implementation:**
- Start: After PR 3.3 complete
- Duration: 2 weeks (PR 3.4a, 3.4b, 3.4c)
- Team: 1 backend dev + 1 frontend dev

### Next Steps

1. **Product Owner Review** - Approve recommended solution
2. **Backend API Design** - Finalize public endpoints
3. **Frontend Mockups** - Design landing page templates
4. **Start Development** - PR 3.4a (Backend APIs)

---

**Document Version:** 1.0
**Last Updated:** 2026-01-30
**Author:** Claude Sonnet 4.5
**Status:** Awaiting Product Owner Approval
