# Best Practices: Tính Năng Trang Web Giới Thiệu Công Khai

**Ngày tạo:** 2026-01-30
**Mục đích:** Phân tích kỹ thuật và khuyến nghị cho tính năng Preview Website
**Trạng thái:** Nghiên cứu Best Practice & Khuyến nghị
**Độ ưu tiên:** 🔴 CRITICAL - Blocking PR 3.4+
**Người đọc:** Product Owner, Tech Lead, Dev Team

---

## MỤC LỤC

1. [Tóm Tắt Điều Hành](#tom-tat-dieu-hanh)
2. [Định Nghĩa Vấn Đề](#dinh-nghia-van-de)
3. [Phân Tích Best Practices Ngành](#phan-tich-best-practices-nganh)
4. [Giải Pháp Khuyến Nghị](#giai-phap-khuyen-nghi)
5. [Đặc Tả Kỹ Thuật](#dac-ta-ky-thuat)
6. [Kế Hoạch Triển Khai](#ke-hoach-trien-khai)
7. [Các Phương Án Thay Thế](#cac-phuong-an-thay-the)
8. [Phụ Lục](#phu-luc)

---

## TÓM TẮT ĐIỀU HÀNH

### "Preview Website" Là Gì?

**Định nghĩa được khuyến nghị:** Trang Web Marketing Công Khai cho mỗi instance KiteClass

### Mục Đích Cốt Lõi

Giúp trung tâm giáo dục thu hút học viên tiềm năng thông qua một website công khai chuyên nghiệp, được tối ưu SEO, giới thiệu khóa học, giảng viên và thương hiệu—toàn bộ tự động tạo từ AI branding assets.

### Tính Năng Chính (MVP)

1. **Trang Đích Công Khai** - Không cần xác thực
2. **Nội Dung AI-Generated** - Sử dụng branding assets từ PART 2
3. **Danh Mục Khóa Học** - Danh sách khóa học công khai với CTA đăng ký
4. **Tối Ưu SEO** - Meta tags, structured data, sitemap
5. **Hỗ Trợ Custom Domain** - Chỉ tier PREMIUM

### Giá Trị Kinh Doanh

| Lợi Ích | Tác Động |
|---------|----------|
| **Thu Hút Học Viên** | Trung tâm có thể marketing online → +30-50% tuyển sinh |
| **Lưu Lượng SEO** | Tìm kiếm tự nhiên → Giảm chi phí thu hút khách hàng |
| **Hình Ảnh Chuyên Nghiệp** | Landing page đẹp → Tăng giá trị cảm nhận |
| **Không Cần Công Sức** | Tự động tạo từ AI branding → Không cần thiết kế/dev |
| **Lợi Thế Cạnh Tranh** | Hầu hết LMS thiếu trang marketing công khai |

### Công Sức Triển Khai

- **Thời gian:** 2 tuần (PR 3.4)
- **Độ phức tạp:** Trung bình (Next.js SSG, public routes)
- **Phụ thuộc:** AI Branding System (PART 2), Feature Detection (PART 1)

---

## ĐỊNH NGHĨA VẤN ĐỀ

### Tình Hình Hiện Tại

Các instance KiteClass là **ứng dụng web nội bộ** yêu cầu xác thực:

```
User truy cập: https://abc-academy.kiteclass.com
→ Chuyển hướng đến /login
→ Phải có tài khoản mới xem được gì
→ ❌ Học viên tiềm năng không thể khám phá khóa học
→ ❌ Không có khả năng hiển thị SEO
→ ❌ Trung tâm chỉ có thể marketing qua truyền miệng
```

### Vấn Đề Kinh Doanh

Trung tâm giáo dục cần:
1. **Thu hút học viên mới** - Giới thiệu khóa học với công chúng
2. **Xây dựng uy tín** - Có mặt trực tuyến chuyên nghiệp
3. **Giảm rào cản** - Cho phép người quan tâm khám phá trước khi cam kết
4. **Tận dụng SEO** - Xếp hạng trên Google cho "khóa học lập trình Hà Nội"

### Vấn Đề Kỹ Thuật

Kiến trúc hiện tại không có public routes:
- Tất cả pages yêu cầu xác thực (`AuthProvider` bao toàn bộ app)
- Không có public course catalog API
- Không có SEO meta tags hoặc structured data
- Không có landing page templates

---

## PHÂN TÍCH BEST PRACTICES NGÀNH

### Phân Tích Đối Thủ Cạnh Tranh

| Nền Tảng | Landing Page Công Khai | Tính Năng | Tech Stack |
|----------|------------------------|-----------|-----------|
| **Teachable** | ✅ Có | Course catalog, instructor bio, reviews | Custom |
| **Thinkific** | ✅ Có | Course listings, pricing, free previews | Ruby/React |
| **Kajabi** | ✅ Có | Full website builder, blog, funnel | Custom |
| **Canvas LMS** | ❌ Không | Tập trung doanh nghiệp, không marketing công khai | Java |
| **Moodle** | ⚠️ Tùy chọn | Dựa trên plugin, cồng kềnh | PHP |
| **Udemy** | ✅ Có | Mô hình marketplace, SEO mạnh | Python/React |

**Kết luận:** Các nền tảng giáo dục hướng người tiêu dùng ĐỀU có landing page công khai. LMS doanh nghiệp (Canvas, Moodle) không có vì bán B2B, không phải B2C.

### Các Mẫu Best Practice

#### 1. Mẫu Public Course Catalog (Teachable, Thinkific)

```
Cấu trúc Landing Page:
├── Hero Section (banner AI-generated + headline)
├── About Section (giới thiệu trung tâm)
├── Course Catalog (lưới khóa học)
│   ├── Course Card (hình, tiêu đề, giá, CTA)
│   └── Course Details Page (giáo trình, giảng viên, đánh giá)
├── Instructor Section (hồ sơ giảng viên)
├── Testimonials/Reviews (nhận xét)
├── Contact/Enrollment CTA (liên hệ/đăng ký)
└── Footer (thương hiệu, links)

Chiến lược Nội dung:
- Công khai: Tiêu đề khóa học, mô tả, giá, lịch trình
- Nội bộ: Nội dung bài học, tài liệu, dữ liệu học viên
- Freemium: 1-2 bài demo mỗi khóa (tùy chọn)

Chiến lược SEO:
- Meta tags: Title, description, OG image mỗi trang
- Structured data: Course schema (schema.org/Course)
- Sitemap: /sitemap.xml với tất cả trang công khai
- Robots.txt: Chỉ cho phép crawl các public routes
```

#### 2. Mẫu Authentication Flow

```
Hành trình Khách:
1. Truy cập landing page → Duyệt khóa học → Không cần đăng nhập
2. Click "Đăng Ký Ngay" → Yêu cầu đăng ký/đăng nhập
3. Hoàn tất thanh toán → Tự động ghi danh → Truy cập nội dung khóa học

Conversion Funnel:
Landing Page → Chi Tiết Khóa → Đăng Ký → Thanh Toán → Dashboard Học Viên
  (công khai)    (công khai)      (auth)    (auth)         (auth)
```

#### 3. Mẫu Kiến Trúc Kỹ Thuật

```typescript
// Cấu trúc Next.js Routes
app/
├── (public)/              // Public routes (không auth)
│   ├── page.tsx          // Landing page
│   ├── courses/
│   │   ├── page.tsx      // Danh mục khóa học
│   │   └── [id]/
│   │       └── page.tsx  // Chi tiết khóa học
│   ├── about/
│   │   └── page.tsx      // Về trung tâm
│   └── contact/
│       └── page.tsx      // Form liên hệ
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

#### 4. Mẫu Content Generation (AI-Driven)

```javascript
// Tự động tạo landing page từ AI branding + dữ liệu instance
const landingPageContent = {
  // Từ AI Branding (PART 2)
  hero: {
    banner: aiAssets.heroBanner,
    headline: aiAssets.textContent.hero_headline,
    subheadline: aiAssets.textContent.hero_subheadline,
    cta: aiAssets.textContent.cta
  },

  // Từ Instance Data
  about: {
    name: instance.name,
    description: instance.description,
    logo: aiAssets.logo
  },

  // Từ Course API
  courses: await fetchPublicCourses(instance.id),

  // Từ Teacher API
  instructors: await fetchPublicInstructors(instance.id)
}
```

### Case Study: Cách Tiếp Cận của Teachable

**Teachable** là đối thủ cạnh tranh gần nhất với tầm nhìn KiteClass.

**Mô Hình Của Họ:**
- Mỗi course creator có: `creator-name.teachable.com`
- Landing page tùy chỉnh hoàn toàn (drag-and-drop builder)
- Public course catalog với preview phong phú
- Tối ưu SEO (xếp hạng tốt trên Google)
- Hỗ trợ custom domain (tính năng trả phí)

**Những Gì Hiệu Quả:**
- ✅ Zero-setup: Tự động tạo site đẹp từ dữ liệu khóa học
- ✅ Tập trung conversion: CTA rõ ràng, tối ưu cho đăng ký
- ✅ Mobile-responsive: 60%+ lưu lượng là mobile
- ✅ Nhanh: Static generation, CDN-cached

**Những Gì Không Hiệu Quả:**
- ❌ Builder phức tạp: Quá nhiều tùy chọn → Khó quyết định
- ❌ Template không nhất quán: Một số creator tạo sites xấu
- ❌ SEO cannibalization: Tất cả trên domain teachable.com

**Bài Học Cho KiteClass:**
1. Giữ đơn giản: Tự động tạo, tối thiểu tùy chỉnh
2. Đảm bảo chất lượng thiết kế: Dùng AI branding, không có lựa chọn "xấu"
3. Custom domains: Bắt buộc cho tier PREMIUM (sở hữu SEO)

---

## GIẢI PHÁP KHUYẾN NGHỊ

### Giải Pháp: Trang Web Marketing Công Khai (Tự Động Tạo)

**Định nghĩa:** Mỗi instance KiteClass có một website công khai tự động tạo từ:
- AI branding assets (hero banner, logos, màu sắc)
- Dữ liệu instance (tên, mô tả, liên hệ)
- Danh mục khóa học (tiêu đề, mô tả, giá)
- Hồ sơ giảng viên (tên, tiểu sử, ảnh)

**Cấu Trúc URL:**
```
https://abc-academy.kiteclass.com          → Landing page (công khai)
https://abc-academy.kiteclass.com/courses  → Danh mục khóa học (công khai)
https://abc-academy.kiteclass.com/courses/101 → Chi tiết khóa (công khai)
https://abc-academy.kiteclass.com/login    → Đăng nhập học viên (auth)
https://abc-academy.kiteclass.com/dashboard → Dashboard học viên (auth)

Tier PREMIUM:
https://abc-academy.com                    → Custom domain (công khai)
```

### Nguyên Tắc Cốt Lõi

1. **Zero Configuration** - Tự động tạo từ dữ liệu có sẵn
2. **SEO First** - Meta tags, structured data, sitemaps
3. **Tối Ưu Conversion** - CTA rõ ràng, tải nhanh
4. **Nhất Quán Thương Hiệu** - Sử dụng AI branding assets
5. **Mobile Responsive** - Thiết kế mobile-first
6. **Bảo Vệ Quyền Riêng Tư** - Chỉ dữ liệu công khai, không có PII học viên

### Phạm Vi: MVP vs Tương Lai

| Tính Năng | MVP (V3) | Tương Lai (V4+) |
|-----------|----------|-----------------|
| Trang đích công khai | ✅ | ✅ |
| Danh mục khóa học | ✅ | ✅ |
| Trang chi tiết khóa học | ✅ | ✅ |
| Nội dung AI-generated | ✅ | ✅ |
| SEO meta tags | ✅ | ✅ |
| Custom domain (PREMIUM) | ✅ | ✅ |
| Hồ sơ giảng viên | ✅ | ✅ |
| Form liên hệ | ✅ | ✅ |
| Đánh giá/nhận xét học viên | ❌ | ✅ |
| Mục Blog/Tin tức | ❌ | ✅ |
| Bài demo (xem trước video) | ❌ | ✅ |
| Page builder (tùy chỉnh) | ❌ | ✅ |
| Multi-page funnels | ❌ | ✅ |
| A/B testing | ❌ | ✅ |

### Đối Tượng Mục Tiêu

**Chính:** Học viên tiềm năng đang duyệt khóa học online

**Phụ:**
- Phụ huynh nghiên cứu trường học cho con
- Google crawlers (SEO)
- Giới thiệu mạng xã hội (Facebook, Zalo shares)

### Authentication Flow

```
Public Routes (Không Auth):
- Landing page: /
- Danh mục khóa học: /courses
- Chi tiết khóa học: /courses/[id]
- Trang Về: /about
- Liên hệ: /contact

Protected Routes (Yêu Cầu Auth):
- Đăng ký: /enroll/[courseId]
- Dashboard học viên: /dashboard
- Nội dung khóa học: /learn/[courseId]
- Cài đặt: /settings

Conversion Trigger:
Khách click "Đăng Ký Ngay" → Chuyển đến /login?redirect=/enroll/[courseId]
```

### Nguồn Nội Dung

| Loại Nội Dung | Nguồn | Công khai? | Ghi chú |
|---------------|-------|------------|---------|
| Hero banner | AI Branding | ✅ | Từ PART 2 |
| Headlines/CTAs | AI Branding | ✅ | Từ PART 2 |
| Logo/màu sắc | AI Branding | ✅ | Từ PART 2 |
| Tên/mô tả trung tâm | Dữ liệu Instance | ✅ | Nhập từ admin |
| Tiêu đề khóa học | Course API | ✅ | Catalog công khai |
| Mô tả khóa học | Course API | ✅ | Catalog công khai |
| Giá khóa học | Course API | ✅ | Công khai |
| Lịch trình khóa học | Course API | ✅ | Công khai |
| Tên/tiểu sử giảng viên | Teacher API | ✅ | Hồ sơ công khai |
| Nội dung bài học | Course API | ❌ | Yêu cầu auth |
| Dữ liệu học viên | Student API | ❌ | Nội bộ |
| Điểm/điểm danh | Analytics API | ❌ | Nội bộ |

### Tech Stack

**Frontend:**
- Next.js 14+ App Router
- Server Components (SSR cho SEO)
- Static Generation (ISR cho hiệu suất)
- Tailwind CSS (styling)

**Backend APIs:**
```
GET /api/public/instance/:id/config        → Instance metadata
GET /api/public/instance/:id/branding      → AI branding assets
GET /api/public/instance/:id/courses       → Danh mục khóa học công khai
GET /api/public/courses/:id                → Chi tiết khóa học
GET /api/public/instance/:id/instructors   → Hồ sơ giảng viên
POST /api/public/contact                   → Gửi form liên hệ
```

**SEO:**
- Next.js Metadata API
- Structured data (JSON-LD)
- Sitemap generation
- robots.txt

**Hiệu Suất:**
- ISR (Incremental Static Regeneration) - Rebuild mỗi 1 giờ
- CDN caching (Cloudflare)
- Tối ưu hình ảnh (next/image)
- Code splitting

---

## ĐẶC TẢ KỸ THUẬT

### Cấu Trúc Trang: Landing Page

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
      {/* Hero Banner AI-Generated */}
      <Image
        src={branding.hero.banner}
        alt={branding.hero.headline}
        fill
        className="object-cover"
        priority
      />

      {/* Lớp phủ */}
      <div className="absolute inset-0 bg-gradient-to-r from-black/60 to-transparent" />

      {/* Nội dung */}
      <div className="relative z-10 container mx-auto h-full flex items-center">
        <div className="max-w-2xl text-white">
          {/* Headline AI-Generated */}
          <h1 className="text-5xl font-bold mb-4">
            {branding.textContent.hero_headline}
          </h1>

          {/* Subheadline AI-Generated */}
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
      {/* Hình khóa học */}
      <CardHeader className="p-0">
        <Image
          src={course.thumbnail || '/placeholder-course.jpg'}
          alt={course.title}
          width={400}
          height={225}
          className="w-full h-[225px] object-cover rounded-t-lg"
        />
      </CardHeader>

      {/* Thông tin khóa học */}
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

        {/* Giá */}
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
  duration: number // tuần
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

### Hiệu Suất: ISR Configuration

```typescript
// app/(public)/page.tsx
export const revalidate = 3600 // Revalidate mỗi 1 giờ

// app/(public)/courses/page.tsx
export const revalidate = 1800 // Revalidate mỗi 30 phút

// app/(public)/courses/[id]/page.tsx
export const revalidate = 3600 // Revalidate mỗi 1 giờ
```

### Mức Độ Tùy Chỉnh (MVP)

**Customer CÓ THỂ Tùy Chỉnh:**
- ✅ Tên, mô tả, thông tin liên hệ trung tâm
- ✅ Tiêu đề, mô tả, giá khóa học
- ✅ Tên, tiểu sử, ảnh giảng viên
- ✅ AI branding assets (qua hệ thống PART 2)
- ✅ Vị trí/màu logo (qua Manual Override)

**Customer KHÔNG THỂ Tùy Chỉnh (MVP):**
- ❌ Layout/cấu trúc trang (templates cố định)
- ❌ Thứ tự section (cố định: Hero → Về → Khóa học → Liên hệ)
- ❌ Custom HTML/CSS
- ❌ Trang bổ sung (blog, tài nguyên)

**Lý do:**
- Duy trì chất lượng thiết kế (tránh sites "xấu")
- Đơn giản hóa triển khai (không cần page builder)
- Thời gian ra thị trường nhanh hơn
- Tương lai: Thêm page builder ở V4 nếu có nhu cầu

### Quan Hệ Với Main Instance

**Chiến Lược Đồng Bộ Dữ Liệu:**

```typescript
// Option A: Real-time (khuyến nghị cho MVP)
Landing page → API call → Main instance
- Ưu điểm: Dữ liệu luôn mới, không có logic sync
- Nhược điểm: Độ trễ nhẹ (~100-200ms)

// Option B: Periodic sync (tối ưu tương lai)
Landing page → Static cache → Rebuild mỗi 1h
- Ưu điểm: Nhanh hơn (0ms), scale tốt hơn
- Nhược điểm: Dữ liệu cũ (delay tối đa 1h)
```

**Khuyến nghị cho MVP: Real-time với ISR**
- Server Components fetch dữ liệu mới
- ISR cache trong 1 giờ
- Best of both worlds: Mới + Nhanh

**Flow Đăng Ký Học Viên:**

```
Khách click "Đăng Ký Ngay" trên course card
  ↓
Chuyển hướng đến: /login?redirect=/enroll/101
  ↓
Khách đăng ký (Zalo OTP hoặc email)
  ↓
Tạo tài khoản → Tự động đăng nhập
  ↓
Chuyển hướng đến: /enroll/101 (authenticated route)
  ↓
Form đăng ký → Thanh toán (nếu khóa học trả phí)
  ↓
Thành công → Chuyển đến /dashboard/courses/101
```

**Đồng Bộ Thông Tin Khóa Học:**

```typescript
// Real-time sync qua API
async function fetchPublicCourses(instanceId: string) {
  // Gọi main instance API
  const response = await fetch(
    `https://api.kiteclass.com/v1/public/instance/${instanceId}/courses`
  )

  // Response được Next.js cache trong 30 phút
  return response.json()
}

// ISR đảm bảo:
// - Visitor đầu: Fetch mới (~200ms)
// - 30 phút tiếp: Cached (0ms)
// - Sau 30 phút: Revalidate ở background
```

---

## KẾ HOẠCH TRIỂN KHAI

### Phase 1: Backend APIs (Tuần 1)

**PR 3.4a: Public APIs**

```
Công việc:
1. Tạo public API endpoints (không cần auth)
   - GET /api/v1/public/instance/:id/config
   - GET /api/v1/public/instance/:id/branding
   - GET /api/v1/public/instance/:id/courses
   - GET /api/v1/public/courses/:id
   - GET /api/v1/public/instance/:id/instructors
   - POST /api/v1/public/contact

2. Thêm PublicCourse DTO (lọc private fields)
   - Bao gồm: title, description, price, schedule, instructor
   - Loại trừ: lesson content, danh sách học viên, điểm

3. Thêm rate limiting (ngăn abuse)
   - 100 requests/phút mỗi IP cho public endpoints

4. Thêm CORS headers (cho phép subdomain access)

5. Tests
   - Unit tests cho public DTOs
   - Integration tests cho public APIs
   - Security tests (đảm bảo không rò private data)

Files:
- backend/src/main/java/com/kiteclass/api/public/
- backend/src/main/java/com/kiteclass/dto/public/
- backend/src/test/java/com/kiteclass/api/public/

Ước tính: 3 ngày
```

### Phase 2: Frontend Public Routes (Tuần 2)

**PR 3.4b: Landing Pages**

```
Công việc:
1. Tạo (public) route group
   app/(public)/
   ├── layout.tsx          // Public layout (không AuthProvider)
   ├── page.tsx            // Landing page
   ├── courses/
   │   ├── page.tsx        // Danh mục khóa học
   │   └── [id]/
   │       └── page.tsx    // Chi tiết khóa học
   ├── about/
   │   └── page.tsx        // Về trung tâm
   └── contact/
       └── page.tsx        // Form liên hệ

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
   - CourseFilters (category, level, giá)
   - Pagination

4. Implement course details page
   - CourseHeader (tiêu đề, giảng viên, giá)
   - CourseSyllabus
   - InstructorBio
   - EnrollmentCTA
   - RelatedCourses

5. Tối ưu SEO
   - Metadata API
   - Structured data (Course schema)
   - Sitemap generation
   - robots.txt

6. Mobile responsive
   - Tailwind breakpoints
   - Thiết kế mobile-first
   - CTA thân thiện với touch

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

Ước tính: 5 ngày
```

### Phase 3: Tích Hợp & Hoàn Thiện (Tuần 2)

```
Công việc:
1. Custom domain routing (tier PREMIUM)
   - Cấu hình Nginx cho custom domains
   - SSL auto-provision (Let's Encrypt)
   - DNS CNAME verification

2. Tối ưu hiệu suất
   - Tối ưu hình ảnh (next/image)
   - Cấu hình ISR
   - CDN caching headers
   - Lazy loading

3. Tích hợp analytics
   - Google Analytics 4
   - Conversion tracking (click đăng ký)
   - Heatmaps (Hotjar)

4. Implement contact form
   - Email notification đến CENTER_OWNER
   - Spam protection (reCAPTCHA)
   - Success/error handling

5. Edge cases
   - Empty state (chưa có khóa học)
   - Unpublished courses (ẩn khỏi công khai)
   - Expired courses (đánh dấu "Đã kết thúc")
   - Private instances (opt-out công khai landing)

6. Documentation
   - Hướng dẫn cho center owners
   - Hướng dẫn SEO best practices
   - Hướng dẫn setup custom domain

Ước tính: 2 ngày
```

### Tổng Timeline

| Phase | Thời gian | Deliverable |
|-------|-----------|-------------|
| Backend APIs | 3 ngày | PR 3.4a merged |
| Frontend Public Routes | 5 ngày | PR 3.4b merged |
| Tích hợp & Hoàn thiện | 2 ngày | PR 3.4c merged |
| **Tổng** | **2 tuần** | **Hoàn thành feature** |

### Phụ Thuộc

**Phải hoàn thành trước khi bắt đầu:**
- ✅ PR 3.2: Core Infrastructure (Feature Detection types)
- ✅ PR 3.3: Providers & Layout
- ✅ AI Branding System APIs (PART 2)

**Có thể phát triển song song:**
- PR 3.5: Admin Dashboard
- PR 3.6: Class Management

---

## CÁC PHƯƠNG ÁN THAY THẾ

### Phương Án 1: Live Demo System (Bị Từ Chối)

**Mô tả:** Một demo instance duy nhất cho tất cả prospects

```
URL: https://demo.kiteclass.com
Nội dung: Khóa học mẫu, học viên fake, dữ liệu demo
Mục đích: Cho thấy tính năng sản phẩm với prospects
```

**Ưu điểm:**
- Triển khai đơn giản (1 instance)
- Trải nghiệm demo có kiểm soát
- Không cần setup cho từng customer

**Nhược điểm:**
- ❌ Không cá nhân hóa (demo chung chung)
- ❌ Không giúp trung tâm marketing bản thân
- ❌ UX gây nhầm lẫn (demo vs instance thật)
- ❌ Không có lợi ích SEO cho customers

**Kết luận:** Bị từ chối. Điều này giúp KiteClass bán platform, nhưng không giúp trung tâm bán khóa học.

### Phương Án 2: Staging/Preview Environment (Bị Từ Chối)

**Mô tả:** Xem trước thay đổi branding trước khi publish

```
URL: https://preview-abc-academy.kiteclass.com
Mục đích: Test branding/settings trước khi apply
Đối tượng: Chỉ CENTER_ADMIN
```

**Ưu điểm:**
- Hữu ích cho QA testing
- Thử nghiệm không rủi ro

**Nhược điểm:**
- ❌ Không phải marketing tool (chỉ nội bộ)
- ❌ Thêm độ phức tạp (quản lý 2 environments)
- ❌ Không giải quyết nhu cầu thu hút học viên

**Kết luận:** Bị từ chối cho MVP. Có thể thêm ở V4 nếu có nhu cầu từ khách hàng.

### Phương Án 3: Full Website Builder (Bị Từ Chối Cho MVP)

**Mô tả:** Drag-and-drop page builder như Kajabi

```
Tính năng:
- Custom page layouts
- Reorder sections
- Thêm custom HTML/CSS
- Multi-page funnels
- A/B testing
```

**Ưu điểm:**
- Linh hoạt tối đa
- Power users thích
- Tính năng cạnh tranh

**Nhược điểm:**
- ❌ 8-12 tuần triển khai
- ❌ UI/UX phức tạp
- ❌ Hầu hết customers sẽ không dùng
- ❌ Rủi ro sites xấu, không nhất quán
- ❌ Gánh nặng bảo trì

**Kết luận:** Defer đến V4. MVP dùng templates cố định (đơn giản hơn, nhanh hơn, chất lượng tốt hơn).

### Phương Án 4: WordPress Integration (Bị Từ Chối)

**Mô tả:** Tạo WordPress site cho mỗi instance

```
Tech: WordPress + WooCommerce
Landing: WordPress site
LMS: KiteClass platform
Sync: Custom plugin
```

**Ưu điểm:**
- Hệ sinh thái phong phú (themes, plugins)
- Quen thuộc với nhiều users
- SEO đã được chứng minh

**Nhược điểm:**
- ❌ Tech stack khác nhau (PHP vs Next.js)
- ❌ Overhead bảo trì (WordPress updates)
- ❌ Rủi ro bảo mật (WordPress vulnerabilities)
- ❌ Độ phức tạp sync (2 hệ thống)
- ❌ Chi phí hosting

**Kết luận:** Bị từ chối. Over-engineering. Next.js SSG đơn giản và tốt hơn.

---

## PHỤ LỤC

### A. SEO Checklist

**On-Page SEO:**
- [x] Title tags (50-60 ký tự)
- [x] Meta descriptions (150-160 ký tự)
- [x] H1 tags (1 mỗi trang)
- [x] H2-H6 hierarchy
- [x] Alt text cho hình ảnh
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
- [x] Tiêu đề unique mỗi trang
- [x] Tối ưu từ khóa
- [x] Nội dung dài (500+ từ)
- [x] LSI keywords
- [x] Call-to-actions

### B. Performance Targets

| Metric | Target | Đo Lường |
|--------|--------|----------|
| **Lighthouse Score** | 90+ | Chrome DevTools |
| **First Contentful Paint** | < 1.5s | WebPageTest |
| **Largest Contentful Paint** | < 2.5s | Core Web Vitals |
| **Time to Interactive** | < 3s | Lighthouse |
| **Tổng Kích Thước Trang** | < 1MB | Network tab |
| **Kích Thước Hình** | < 200KB | next/image |

### C. Accessibility Checklist

**WCAG 2.1 AA Compliance:**
- [x] Semantic HTML
- [x] ARIA labels
- [x] Keyboard navigation
- [x] Focus indicators
- [x] Color contrast (4.5:1)
- [x] Alt text cho hình ảnh
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
  placement: 'course_card' // hoặc 'course_details'
})

gtag('event', 'submit_contact_form', {
  form_location: 'landing_page'
})

gtag('event', 'view_instructor', {
  instructor_id: instructor.id
})
```

### E. Content Guidelines

**Mô Tả Khóa Học (cho SEO):**
```
Độ dài: 150-300 từ
Cấu trúc:
1. Hook (1 câu) - "Học lập trình web từ zero đến hero trong 12 tuần"
2. Benefits (3-4 bullets)
   - Build 5 dự án thực tế
   - 1-on-1 mentorship
   - Hỗ trợ tìm việc
3. Đối tượng - "Phù hợp cho người mới bắt đầu"
4. CTA - "Đăng ký ngay để nhận ưu đãi"

Từ khóa: Bao gồm target keywords tự nhiên (vd: "lập trình web", "khóa học online")
Tone: Truyền cảm hứng, tập trung vào lợi ích, trò chuyện
```

### F. Custom Domain Setup (PREMIUM)

**Cấu Hình DNS:**
```
Customer thêm CNAME record:
  Type: CNAME
  Name: www
  Value: proxy.kiteclass.com
  TTL: 3600

KiteClass backend:
1. Verify DNS propagation
2. Provision SSL certificate (Let's Encrypt)
3. Cấu hình Nginx reverse proxy
4. Update bảng domain_mappings

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

**Chỉ Dữ Liệu Công Khai:**
```typescript
// ✅ An toàn để expose
interface PublicCourse {
  id: string
  title: string
  description: string
  price: number
  instructor: {
    name: string  // Hồ sơ công khai
    bio: string
  }
}

// ❌ Không bao giờ expose
interface PrivateCourse {
  students: Student[]        // PII
  lessons: Lesson[]          // Nội dung
  grades: Grade[]            // Nội bộ
  attendance: Attendance[]   // Nội bộ
}
```

**GDPR Compliance:**
- Chỉ dữ liệu công khai (không có PII không có đồng ý)
- Cookie consent banner
- Link privacy policy
- Contact form: Explicit opt-in cho marketing
- Right to be forgotten (xóa tài khoản → xóa khỏi catalog công khai)

---

## KẾT LUẬN

### Cách Tiếp Cận Được Khuyến Nghị

**Triển khai Trang Web Marketing Công Khai (Tự Động Tạo)** như mô tả trong tài liệu này.

**Lợi Ích Chính:**
1. ✅ Giải quyết vấn đề thực của khách hàng (thu hút học viên)
2. ✅ Lợi thế cạnh tranh (hầu hết LMS thiếu tính năng này)
3. ✅ Triển khai nhanh (2 tuần)
4. ✅ Bảo trì thấp (tự động tạo)
5. ✅ Tối ưu SEO (lưu lượng tự nhiên)
6. ✅ Tận dụng AI branding (PART 2)

**Triển Khai:**
- Bắt đầu: Sau khi PR 3.3 hoàn thành
- Thời gian: 2 tuần (PR 3.4a, 3.4b, 3.4c)
- Team: 1 backend dev + 1 frontend dev

### Các Bước Tiếp Theo

1. **Product Owner Review** - Phê duyệt giải pháp được khuyến nghị
2. **Backend API Design** - Finalize public endpoints
3. **Frontend Mockups** - Thiết kế landing page templates
4. **Bắt Đầu Development** - PR 3.4a (Backend APIs)

---

**Phiên Bản Tài Liệu:** 1.0
**Cập Nhật Lần Cuối:** 2026-01-30
**Tác Giả:** Claude Sonnet 4.5
**Trạng Thái:** Chờ Product Owner Phê Duyệt
