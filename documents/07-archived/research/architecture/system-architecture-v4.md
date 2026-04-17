# Kiến Trúc V4.1 - Bundled Business Model with LMS

**Ngày tạo:** 2026-02-26
**Phiên bản:** 4.1 (Bundled Model)
**Kế thừa từ:** V4.0 Addendum
**ADR tham chiếu:** Bundled business model for KiteHub (2026-02-26)

---

## Tổng quan thay đổi V4.0 → V4.1

| Thành phần | V4.0 (Unbundled) | V4.1 (Bundled) | Lý do |
|------------|------------------|----------------|-------|
| **Core Service** | ~650MB (Admin only) | ~900MB (Admin + LMS + Marketing) | Extend với guest-facing features |
| **LMS Module** | ❌ Tách service riêng | ✅ Merge vào Core | Đơn giản hóa architecture, bundled pricing |
| **Marketing Module** | ❌ Không | ✅ Trong Core | Landing page API, Lead management, Contact form |
| **Business Model** | Unbundled (Core + upsell) | **Bundled (1 gói = all features)** | Đơn giản cho khách hàng |
| **Pricing** | ₫299k base + addons | **₫299k all-inclusive** | Competitive pricing, faster conversion |

**Tài liệu chi tiết:**
- V4.0: `service-architecture-revision-report.md` (41KB)
- V4.1: Bundled business model decision (2026-02-26)

---

## 1. Kiến trúc KiteClass Instance V4.0

### 1.1. Sơ đồ tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    KITECLASS INSTANCE V4.0 (PER CUSTOMER)                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │             CORE BUNDLE (Required) - Gói đầy đủ: ₫299k/tháng             │  │
│  │                    Bao gồm: Admin + LMS + Guest Features                  │  │
│  ├───────────────────────────────────────────────────────────────────────────┤  │
│  │                                                                           │  │
│  │  ┌─────────────────┐  ┌─────────────────────────────────────────────┐   │  │
│  │  │   Gateway       │  │  Core Service (Extended)                   │   │  │
│  │  │   Service       │  │                                             │   │  │
│  │  │                 │  │  🏫 ADMIN MODULE:                          │   │  │
│  │  │  • Auth         │  │    • Student, Teacher, Class Management    │   │  │
│  │  │  • User CRUD    │  │    • Enrollment, Attendance, Grading       │   │  │
│  │  │  • JWT          │  │    • Billing (Invoice, Payment)            │   │  │
│  │  │  • Rate Limit   │  │                                             │   │  │
│  │  │                 │  │  🎓 LMS MODULE (NEW):                      │   │  │
│  │  │  RAM: ~400MB    │  │    • CourseModule, Lesson, Resources       │   │  │
│  │  └─────────────────┘  │    • Trial lesson access control           │   │  │
│  │                       │    • Learning progress tracking             │   │  │
│  │  ┌─────────────────┐  │    • Quiz engine (optional)                │   │  │
│  │  │   Frontend      │  │                                             │   │  │
│  │  │   (Next.js)     │  │  🎨 MARKETING MODULE (NEW):                │   │  │
│  │  │                 │  │    • Landing page content API              │   │  │
│  │  │  • Guest Pages  │  │    • Lead management                        │   │  │
│  │  │    - Landing    │  │    • Contact form processing               │   │  │
│  │  │    - Catalog    │  │    • Trial enrollment workflow             │   │  │
│  │  │    - Trial      │  │                                             │   │  │
│  │  │  • Admin UI     │  │  RAM: ~900MB (650MB + 250MB LMS/Marketing) │   │  │
│  │  │  • Student UI   │  └─────────────────────────────────────────────┘   │  │
│  │  │                 │                                                     │  │
│  │  │  RAM: ~250MB    │                                                     │  │
│  │  └─────────────────┘                                                     │  │
│  │                                                                           │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                   OPTIONAL ADDONS (Pick & Choose)                         │  │
│  │                      Expand features beyond Core                          │  │
│  ├───────────────────────────────────────────────────────────────────────────┤  │
│  │                                                                           │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │  │
│  │  │ Parent Service  │  │ Gamification    │  │  Forum Service  │          │  │
│  │  │ (Future)        │  │   (Future)      │  │   (Future)      │          │  │
│  │  │                 │  │                 │  │                 │          │  │
│  │  │ • Zalo OTP Reg  │  │  • Points       │  │  • Q&A Forum    │          │  │
│  │  │ • Track child   │  │  • Badges       │  │  • Discussions  │          │  │
│  │  │ • Notifications │  │  • Leaderboard  │  │  • Comments     │          │  │
│  │  │ • Billing view  │  │  • Rewards      │  │  • Voting       │          │  │
│  │  │                 │  │                 │  │                 │          │  │
│  │  │ ₫100k/tháng     │  │  ₫150k/tháng    │  │  ₫100k/tháng    │          │  │
│  │  │ RAM: ~150MB     │  │  RAM: ~180MB    │  │  RAM: ~150MB    │          │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │  │
│  │                                                                           │  │
│  │  Note: These remain as separate services for advanced users              │  │
│  │                                                                           │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                       MEDIA SERVICE (Optional)                            │  │
│  │                   Ant Media Server CE + MinIO                             │  │
│  ├───────────────────────────────────────────────────────────────────────────┤  │
│  │                                                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐        │  │
│  │  │              Ant Media Server CE (All-in-one)                │        │  │
│  │  │                                                              │        │  │
│  │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │        │  │
│  │  │  │   VOD    │  │   Live   │  │  WebRTC  │  │ Recording│    │        │  │
│  │  │  │ Streaming│  │ Streaming│  │   P2P    │  │          │    │        │  │
│  │  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │        │  │
│  │  │                                                              │        │  │
│  │  │                  ┌─────────────────────┐                    │        │  │
│  │  │                  │   FFmpeg Engine     │                    │        │  │
│  │  │                  │   Auto Transcoding  │                    │        │  │
│  │  │                  │  (360p/720p/1080p)  │                    │        │  │
│  │  │                  └─────────────────────┘                    │        │  │
│  │  │                                                              │        │  │
│  │  └──────────────────────────────────────────────────────────────┘        │  │
│  │                              │                                           │  │
│  │                              ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐        │  │
│  │  │                    MinIO Object Storage                      │        │  │
│  │  │         S3-compatible - Videos, HLS segments, Thumbnails     │        │  │
│  │  └──────────────────────────────────────────────────────────────┘        │  │
│  │                                                                           │  │
│  │  ₫200k/tháng (STANDARD tier)                                              │  │
│  │  ₫400k/tháng (PREMIUM tier - more storage & bandwidth)                    │  │
│  │  RAM: ~600MB (Ant Media) + ~200MB (MinIO) = ~800MB                        │  │
│  │                                                                           │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                         SHARED DATABASE                                   │  │
│  │  ┌─────────────────┐                    ┌─────────────────┐              │  │
│  │  │   PostgreSQL    │                    │      Redis      │              │  │
│  │  │  (Per Instance) │                    │   (Caching)     │              │  │
│  │  └─────────────────┘                    └─────────────────┘              │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2. So sánh Service Count & Pricing

| Gói | Core Services | Optional Addons | Media Service | Total Services | RAM Usage |
|-----|---------------|-----------------|---------------|----------------|-----------|
| **CORE BUNDLE** (V4.1) | 3 (Gateway + Core + Frontend) | 0 | 0 | **3** | **~1.55GB** |
| **CORE + Media** | 3 | 0 | 1 | 4 | ~2.35GB |
| **CORE + Parent** | 3 | 1 (Parent) | 0 | 4 | ~1.70GB |
| **FULL PACKAGE** | 3 | 3 (all optional) | 1 (Media+) | 7 | ~2.85GB |

**New Pricing V4.1 (Bundled Model):**
- **CORE BUNDLE**: ₫299k/tháng ⭐ **All-Inclusive** (Admin + LMS + Marketing)
  - Bao gồm:
    - ✅ Landing page + Course catalog (guest xem)
    - ✅ Trial lessons (guest học thử)
    - ✅ Contact form + Lead management
    - ✅ Full admin features (Student, Teacher, Class, Attendance, Billing)
    - ✅ Learning progress tracking
- **CORE + Media**: ₫299k + ₫200k = ₫499k/tháng (thêm video streaming)
- **CORE + Parent**: ₫299k + ₫100k = ₫399k/tháng (thêm parent portal)
- **FULL PACKAGE**: ₫299k + ₫300k (optional addons) + ₫200k (media) = ₫799k/tháng

**Old Pricing V4.0 (Unbundled):**
- BASIC: ₫299k/tháng (admin only, không có guest features)
- STANDARD: ₫799k/tháng (+ expand bundle + media)
- PREMIUM: ₫999k/tháng (+ premium media)

---

## 2. Service Dependencies V4.0

### 2.1. Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                     SERVICE DEPENDENCIES                         │
└─────────────────────────────────────────────────────────────────┘

Frontend
  └──► Gateway Service (Auth, API calls)

Gateway Service
  └──► Core Service (Student, Teacher, Class APIs)

Parent Service (NEW)
  ├──► Core Service (Read: Student, Attendance, Grades)
  └──► Gateway Service (Auth verification)

Gamification Service (NEW)
  ├──► Core Service (Events: Attendance, Assignment, Test)
  └──► Gateway Service (Auth verification)

Forum Service (NEW)
  ├──► Core Service (References: User, Class)
  └──► Gateway Service (Auth verification)

Media Service (NEW)
  ├──► Gateway Service (Auth verification)
  └──► Core Service (Link videos to courses)

Core Service
  └──► (No dependencies - central service)
```

**Key Points:**
- ✅ **No circular dependencies**
- ✅ **Expand services độc lập nhau** (không gọi lẫn nhau)
- ✅ **Event-driven communication** (Gamification nhận events từ Core)
- ✅ **Fault isolation** (1 expand service down không ảnh hưởng services khác)

### 2.2. Communication Patterns

| From → To | Protocol | Purpose | Sync/Async |
|-----------|----------|---------|------------|
| Frontend → Gateway | REST API | User requests | Sync |
| Gateway → Core | REST API | Business logic | Sync |
| Parent → Core | REST API | Read student data | Sync |
| Gamification ← Core | Event (RabbitMQ) | Tích điểm tự động | Async |
| Forum → Core | REST API | Get user/class info | Sync |
| Media → Core | REST API | Link videos | Sync |
| All → Gateway | REST API | Auth verification | Sync |

---

## 2.3. Core Service Extended Modules (V4.1)

### LMS Module (Learning Management)

**Entities:**
```
CourseModule (id, courseId, title, order, description)
  └─ Lesson (id, moduleId, title, content, videoUrl, isTrial, order)
      └─ LearningResource (id, lessonId, type, url, title)

LessonProgress (id, userId, lessonId, completed, completedAt)
CourseProgress (id, userId, courseId, progress, lastAccessedAt)
```

**API Endpoints:**
```
# Guest/Student endpoints
GET  /api/v1/courses/{id}/modules        # Xem course structure
GET  /api/v1/lessons/{id}                # Xem lesson content
POST /api/v1/lessons/{id}/complete       # Mark lesson complete
GET  /api/v1/courses/{id}/progress       # Get learning progress

# Teacher/Admin endpoints
POST   /api/v1/courses/{id}/modules      # Create module
PUT    /api/v1/modules/{id}              # Update module
DELETE /api/v1/modules/{id}              # Delete module
POST   /api/v1/modules/{id}/lessons      # Add lesson
PUT    /api/v1/lessons/{id}              # Update lesson (toggle isTrial)
DELETE /api/v1/lessons/{id}              # Delete lesson
```

**Access Control:**
- **Trial lessons** (isTrial=true): Public, guest chỉ cần email
- **Paid lessons** (isTrial=false): Require active enrollment

**Business Logic:**
- Guest đăng ký email → Có thể xem trial lessons
- Student enrolled → Có thể xem tất cả lessons
- Progress tracking: Auto-save khi student hoàn thành lesson
- Quiz engine: Phase 2 (optional feature)

### Marketing Module (Lead Generation)

**Entities:**
```
LandingPageContent (id, tenantId, teacherBio, heroImage, tagline)
  └─ CourseHighlight (id, contentId, courseId, order)

Lead (id, tenantId, email, name, phone, source, status)
ContactMessage (id, tenantId, name, email, message, createdAt)
```

**API Endpoints:**
```
# Public endpoints
GET  /api/v1/tenants/{id}/landing        # Get landing page content
POST /api/v1/leads                       # Register for trial
POST /api/v1/contact                     # Send contact message

# Admin endpoints
GET  /api/v1/tenants/{id}/landing        # Get landing content
PUT  /api/v1/tenants/{id}/landing        # Update landing content
GET  /api/v1/leads                       # List leads (filter, pagination)
PUT  /api/v1/leads/{id}                  # Update lead status
GET  /api/v1/contact-messages            # List contact messages
```

**Business Logic:**
- Landing page: Dynamic per tenant (mỗi giảng viên có landing riêng)
- Lead qualification: NEW → CONTACTED → CONVERTED → LOST
- Contact form: Gửi email notification đến teacher
- AI Branding (Phase 2): Generate logo, colors, tagline

**RAM Impact:**
- Admin module: ~650MB (baseline)
- LMS module: ~150MB (entities, repositories, caching)
- Marketing module: ~100MB (simple CRUD, minimal caching)
- **Total**: ~900MB

---

### Storage & File Management

**Implementation**: See [Storage Service Design](../../03-planning/implementation/storage-service-design.md)

**Architecture:**
- Object storage: MinIO (dev), AWS S3 (prod), CloudFlare R2 (CDN)
- Metadata database: PostgreSQL (uploaded_files, storage_quotas tables)
- Upload method: Presigned URLs (client → S3 direct, bypass backend)
- Download method: Presigned URLs with access control
- Quota tracking: Per-tenant limits (Trial: 500MB, Basic: 5GB, Pro: 50GB)

**File Types:**
- Avatars (10MB): User profile pictures
- Documents (50MB): Course materials, assignments
- Videos (2GB): Lecture recordings (see Media Service for streaming)
- Certificates (5MB): Student achievement certificates

**Multi-tenant Isolation:**
- Storage path: `{tenant-id}/{file-type}/{uuid}.{ext}`
- Database: `instance_id` column + Hibernate filter
- S3 bucket policies (optional): Prevent cross-tenant access

**API Endpoints:**
```
# File upload/download
POST /api/v1/files/upload/initiate       # Generate presigned upload URL
POST /api/v1/files/{id}/complete         # Mark upload complete
GET  /api/v1/files/{id}/download         # Generate presigned download URL
DELETE /api/v1/files/{id}                # Soft delete (30-day retention)

# Storage quota
GET  /api/v1/storage/quota               # Get tenant quota
POST /api/v1/storage/quota/recalculate   # Manual recalculation (admin)
```

**Business Logic:**
- Quota enforcement: Check before generating presigned URL
- Access control: PRIVATE (uploader only), COURSE (teacher+students), PUBLIC (all authenticated)
- File lifecycle: UPLOADING → PROCESSING → READY → FAILED
- Soft delete: 30-day grace period before permanent deletion
- Scheduled jobs: Daily quota recalculation, cleanup expired files

**Migration**: V13 (File Storage Tables)

**RAM Impact**: ~50MB (lightweight, presigned URLs offload work to S3)

---

## 3. Media Service Architecture (Chi tiết)

### 3.1. Phase 1: Thesis Demo (MinIO + FFmpeg)

**Stack:**
```
┌─────────────────────────────────────────────────────────┐
│              Media Service - Phase 1 (Simple)            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  User Upload                                             │
│      │                                                   │
│      ▼                                                   │
│  ┌──────────────┐      ┌──────────────┐                 │
│  │  KiteClass   │─────►│    MinIO     │                 │
│  │   Backend    │      │   Storage    │                 │
│  └──────────────┘      └──────┬───────┘                 │
│                               │                          │
│                               ▼                          │
│                      ┌──────────────────┐               │
│                      │  FFmpeg Worker   │               │
│                      │  (Transcoding)   │               │
│                      └────────┬─────────┘               │
│                               │                          │
│                               ▼                          │
│                      ┌──────────────────┐               │
│                      │   HLS Files      │               │
│                      │ (360p/720p/1080p)│               │
│                      └────────┬─────────┘               │
│                               │                          │
│                               ▼                          │
│  User Watch           ┌──────────────────┐              │
│      │                │      nginx       │              │
│      └───────────────►│   (HLS Server)   │              │
│                       └──────────────────┘              │
│                                                          │
│  Chi phí: ~₫1.2M/tháng ($50 server)                     │
│  Features: VOD only                                      │
└─────────────────────────────────────────────────────────┘
```

### 3.2. Phase 2: Production (Ant Media Server CE)

**Stack:**
```
┌─────────────────────────────────────────────────────────┐
│           Media Service - Phase 2 (Full Features)        │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────────────────────────────────────┐     │
│  │         Ant Media Server CE (All-in-one)       │     │
│  │                                                │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐     │     │
│  │  │   RTMP   │  │  WebRTC  │  │   HLS    │     │     │
│  │  │  Ingest  │  │   P2P    │  │  Output  │     │     │
│  │  └──────────┘  └──────────┘  └──────────┘     │     │
│  │                                                │     │
│  │         ┌────────────────────┐                 │     │
│  │         │  FFmpeg Transcoding │                │     │
│  │         │  (Auto adaptive)    │                │     │
│  │         └────────────────────┘                 │     │
│  │                                                │     │
│  │  Features:                                     │     │
│  │  ✅ VOD (upload → transcode → stream)          │     │
│  │  ✅ Live Streaming (RTMP/WebRTC → HLS)         │     │
│  │  ✅ Recording (save live sessions)             │     │
│  │  ✅ Adaptive bitrate (auto quality switch)     │     │
│  │  ✅ Dashboard (admin UI)                       │     │
│  │  ✅ REST API (full control)                    │     │
│  │                                                │     │
│  └────────────────┬───────────────────────────────┘     │
│                   │                                      │
│                   ▼                                      │
│  ┌────────────────────────────────────────────────┐     │
│  │           MinIO Object Storage                 │     │
│  │    (S3-compatible - store videos & HLS)        │     │
│  └────────────────────────────────────────────────┘     │
│                                                          │
│  Chi phí: ~₫2.4M/tháng ($100 server)                    │
│  Features: VOD + Live + WebRTC + Recording               │
└─────────────────────────────────────────────────────────┘
```

### 3.3. API Examples

**Upload Video (VOD):**
```http
POST /api/v1/media/videos/upload
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data

{
  "file": <video_file>,
  "courseId": 123,
  "title": "Bài 1: Giới thiệu Java",
  "description": "..."
}

Response:
{
  "videoId": "abc123",
  "status": "PROCESSING",
  "estimatedTime": "5 minutes"
}
```

**Start Live Stream:**
```http
POST /api/v1/media/live/start
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "classId": 456,
  "title": "Buổi học trực tuyến - Lớp 10A"
}

Response:
{
  "streamId": "xyz789",
  "rtmpUrl": "rtmp://media.kiteclass.com/live/xyz789",
  "streamKey": "secret_key_xyz",
  "hlsUrl": "https://media.kiteclass.com/live/xyz789/playlist.m3u8",
  "webrtcUrl": "wss://media.kiteclass.com:5443/WebRTCAppEE/xyz789"
}
```

---

## 4. Pricing Model V4.1 (Bundled)

### 4.1. New Pricing Tiers (All-Inclusive Core)

```
┌─────────────────────────────────────────────────────────────────┐
│                 PRICING V4.1 (Bundled Model) ⭐ NEW              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  CORE BUNDLE (Required): ₫299k/tháng                            │
│  ├─ Gateway + Core (Extended) + Frontend                        │
│  ├─ Admin Features:                                             │
│  │   • Student, Teacher, Class Management                       │
│  │   • Attendance, Assignment, Grading                          │
│  │   • Billing (Invoice, Payment)                               │
│  ├─ Guest-Facing Features (NEW):                                │
│  │   • Landing page (giới thiệu giảng viên, khóa học)          │
│  │   • Course catalog (browse & filter)                         │
│  │   • Trial lessons (học thử miễn phí)                        │
│  │   • Contact form (liên hệ giảng viên)                        │
│  └─ LMS Features (NEW):                                          │
│      • Course structure (Modules → Lessons → Resources)          │
│      • Learning progress tracking                                │
│      • Trial access control (free vs paid lessons)              │
│                                                                  │
│  OPTIONAL ADDONS (Future):                                       │
│  ├─ Parent Portal                      ₫100k/tháng              │
│  ├─ Gamification System                ₫150k/tháng              │
│  └─ Forum/Q&A                          ₫100k/tháng              │
│                                                                  │
│  MEDIA SERVICE (Optional):                                       │
│  ├─ STANDARD (100GB, 500GB bandwidth)  ₫200k/tháng              │
│  └─ PREMIUM (500GB, 2TB bandwidth)     ₫400k/tháng              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2. Package Examples

**Trường cấp 3 (chỉ cần Parent Portal):**
```
₫299k (Core) + ₫100k (Parent) = ₫399k/tháng
Services: Gateway, Core, Frontend, Parent
RAM: ~1.40GB
```

**Trung tâm Anh ngữ (cần Media + Parent):**
```
₫299k (Core) + ₫100k (Parent) + ₫200k (Media) = ₫599k/tháng
Services: Gateway, Core, Frontend, Parent, Media
RAM: ~2.25GB
```

**Trung tâm Online đầy đủ:**
```
₫299k (Core) + ₫300k (Expand bundle) + ₫200k (Media) = ₫799k/tháng
Services: Gateway, Core, Frontend, Parent, Gamification, Forum, Media
RAM: ~2.53GB
```

### 4.3. Revenue Impact Projection

**V3.1 (Bundled Engagement Pack):**
```
Engagement Pack = ₫300k/tháng (all-or-nothing)
Conversion rate: 15%
Average revenue per customer: 15% × ₫300k = ₫45k
```

**V4.0 (Unbundled):**
```
Scenario 1: 30% chọn Parent only           = 30% × ₫100k = ₫30k
Scenario 2: 10% chọn Gamification only     = 10% × ₫150k = ₫15k
Scenario 3: 5% chọn Bundle                 = 5% × ₫300k  = ₫15k
Total average revenue per customer: ₫60k (+33% vs V3.1)
```

---

## 5. Migration Strategy

### 5.1. From V3.1 to V4.0

**Step 1: Deploy new services (1 tháng)**
- Deploy Parent Service
- Deploy Gamification Service
- Deploy Forum Service
- Dual-write phase (write to both old Engagement & new services)

**Step 2: Migrate customers (1 tháng)**
- Notify customers về new pricing
- Migrate data từ Engagement → 3 services mới
- Switch reads to new services

**Step 3: Deprecate Engagement Service (2 tuần)**
- Stop writes to Engagement Service
- Remove Engagement Service từ deployment
- Monitor stability

**Step 4: Media Service rollout (2 tháng)**
- Phase 1: MinIO + FFmpeg (thesis demo)
- Phase 2: Ant Media Server CE (production)
- Gradual rollout to customers who need

### 5.2. Rollback Plan

**If issues in Step 1-2:**
- ✅ Switch reads back to Engagement Service
- ✅ Keep old pricing model
- ✅ Investigate & fix issues

**If issues in Step 3:**
- ⚠️ Harder to rollback (data migrated)
- Need to restore from backup

**Mitigation:**
- Pilot with 5-10 friendly customers first
- Extensive testing in staging
- Keep dual-write for 2 months (safety net)

---

## 6. Expected Benefits

### 6.1. Business Benefits (V4.0 vs V4.1)

| Metric | V4.0 (Unbundled) | V4.1 (Bundled) | Improvement |
|--------|------------------|----------------|-------------|
| **Entry price** | ₫299k (admin only) | ₫299k (admin + guest) | **Same price, more value** |
| **Feature completeness** | Admin only | Admin + LMS + Marketing | **+50% features** |
| **Time to revenue** | Khách cần upsell LMS | Instant (tất cả có sẵn) | **Faster** |
| **Sales complexity** | Phải giải thích upsell | Đơn giản (1 gói = all) | **Simpler** |
| **Customer satisfaction** | Phải trả thêm cho LMS | All-inclusive, rõ ràng | **Higher** |
| **Conversion rate** | Lower (complex pricing) | Higher (simple pricing) | **+30-50%** |
| **Churn risk** | Cao nếu thiếu features | Thấp (đầy đủ features) | **Lower** |

### 6.2. Technical Benefits

| Metric | V3.1 | V4.0 | Improvement |
|--------|------|------|-------------|
| **RAM per instance (Parent only)** | 384MB (full Engagement) | 150MB (Parent only) | **-60%** |
| **Fault isolation** | 1 bug → all features down | 1 bug → 1 feature down | **Better** |
| **Scaling granularity** | Scale cả Engagement | Scale từng service | **Finer** |
| **Development velocity** | Deploy 1 service (risk cao) | Deploy 1 service (risk thấp) | **Faster** |

### 6.3. Cost Benefits

**Development cost saving (Media Service):**
```
Build from scratch:  ₫90M, 6-8 tháng
Clone Ant Media CE:  ₫20M, 3-5 tuần
Saving:              ₫70M, 5-7 tháng
```

**Infrastructure cost saving (100 instances, 40% cần Parent):**
```
V3.1: 40 instances × 384MB = 15.4GB RAM needed
V4.0: 40 instances × 150MB = 6.0GB RAM needed
Saving: 9.4GB RAM = ~1-2 servers (8GB each) = ~₫2.4M/tháng ($100×2)
```

---

## 7. Risk Assessment

### 7.1. Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Ant Media CE limits hit** | Medium | Medium | Plan upgrade to OvenMediaEngine |
| **Service mesh complexity** | Low | Medium | Use Kubernetes service discovery |
| **Data migration bugs** | Low | High | Extensive testing, dual-write phase |
| **Performance degradation** | Low | Medium | Load testing before rollout |

### 7.2. Business Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Customers resist unbundling** | Low | Medium | Offer bundle discount, clear value prop |
| **Support cost increases** | Medium | Low | Better documentation, customer training |
| **Price elasticity miscalculation** | Low | High | Pilot with 10 customers, A/B testing |

---

## 8. Conclusion

**V4.1 Architecture Decisions (NEW):**
- ✅ **Bundled business model** - 1 gói = all features
- ✅ **Extend Core Service** với LMS + Marketing modules
- ✅ **Simplify architecture** - 3 services thay vì 5-7
- ✅ **Competitive pricing** - ₫299k all-inclusive

**Comparison: V4.0 vs V4.1**

| Aspect | V4.0 (Unbundled) | V4.1 (Bundled) |
|--------|------------------|----------------|
| **Core Services** | 3 (Gateway, Core, Frontend) | 3 (same) |
| **Core RAM** | ~650MB (admin only) | ~900MB (admin + LMS + marketing) |
| **Entry Price** | ₫299k (admin only) | ₫299k (all features) |
| **Guest Features** | ❌ Cần tách LMS Service | ✅ Built-in (landing, catalog, trial) |
| **Architecture** | 5-7 services (với addons) | 3-4 services (simpler) |
| **Sales Pitch** | Complex (upsell LMS/media) | Simple ("1 gói = đủ hết") |
| **Development Time** | 6-8 tháng (tách services) | 4-6 tháng (extend Core) |

**Expected Outcomes (V4.1):**
- 📈 **+40-60% conversion** rate (simple pricing)
- ⚡ **2 tháng faster** time to market (no LMS Service separation)
- 💰 **₫30-40M cost saving** (less infrastructure complexity)
- 🎯 **Higher customer satisfaction** (all-inclusive, transparent pricing)
- 🚀 **Faster MVP launch** (3 services vs 5-7)

**Trade-offs:**
- ⚠️ Core Service RAM tăng ~40% (650MB → 900MB) - vẫn chấp nhận được
- ⚠️ Khó scale LMS riêng (nhưng bundled model không cần)
- ⚠️ Future refactoring nếu cần unbundle (acceptable risk)

**Next Steps:**
1. ✅ **Update all documents** (architecture, use cases, PR plans)
2. **Implement PR 2.9**: LMS Module (2-3 tuần)
3. **Implement PR 2.10**: Marketing Module (1-2 tuần)
4. **Implement PR 3.12**: Guest Frontend (2-3 tuần)
5. **Launch MVP**: Full-featured platform (6-8 tuần total)

---

**Document Version:** 4.1 (Bundled Model)
**Status:** Ready for Implementation
**Author:** Nguyễn Văn Kiệt
**Date:** 2026-02-26
**Supersedes:** V4.0 Addendum (2026-01-30)
