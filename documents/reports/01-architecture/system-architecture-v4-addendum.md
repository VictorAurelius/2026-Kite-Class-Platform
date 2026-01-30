# Kiến Trúc V4.0 Addendum - Service Separation & Media Service

**Ngày tạo:** 2026-01-30
**Phiên bản:** 4.0 Addendum
**Kế thừa từ:** V3.1 Final
**ADR tham chiếu:** service-architecture-revision-report.md

---

## Tổng quan thay đổi V3.1 → V4.0

| Thành phần | V3.1 | V4.0 | Lý do |
|------------|------|------|-------|
| **Engagement Service** | 1 service (Gamification + Parent + Forum) | ❌ Loại bỏ | Tách thành 3 services độc lập |
| **Parent Service** | ❌ Không | ✅ Service riêng (₫100k/tháng) | Business independence, pricing flexibility |
| **Gamification Service** | ❌ Không | ✅ Service riêng (₫150k/tháng) | Independent scaling, fault isolation |
| **Forum Service** | ❌ Không | ✅ Service riêng (₫100k/tháng) | Optional feature, không phải core |
| **Media Service** | ❌ Không | ✅ Ant Media Server CE | Clone open-source, tiết kiệm 5-7 tháng |

**Tài liệu chi tiết:** Xem `service-architecture-revision-report.md` (41KB)

---

## 1. Kiến trúc KiteClass Instance V4.0

### 1.1. Sơ đồ tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    KITECLASS INSTANCE V4.0 (PER CUSTOMER)                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                        CORE SERVICES (Required)                           │  │
│  │                         Gói BASIC: ₫299k/tháng                            │  │
│  ├───────────────────────────────────────────────────────────────────────────┤  │
│  │                                                                           │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │  │
│  │  │   Gateway       │  │  Core Service   │  │   Frontend      │          │  │
│  │  │   Service       │  │                 │  │   (Next.js)     │          │  │
│  │  │                 │  │  • Student      │  │                 │          │  │
│  │  │  • Auth         │  │  • Teacher      │  │  • Landing      │          │  │
│  │  │  • User CRUD    │  │  • Class        │  │  • Dashboard    │          │  │
│  │  │  • JWT          │  │  • Attendance   │  │  • Student UI   │          │  │
│  │  │  • Rate Limit   │  │  • Assignment   │  │  • Teacher UI   │          │  │
│  │  │                 │  │  • Grading      │  │                 │          │  │
│  │  │  RAM: ~400MB    │  │  RAM: ~650MB    │  │  RAM: ~200MB    │          │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │  │
│  │                                                                           │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                      EXPAND SERVICES (Optional)                           │  │
│  │                  Pick & Choose - Unbundled Pricing                        │  │
│  ├───────────────────────────────────────────────────────────────────────────┤  │
│  │                                                                           │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │  │
│  │  │ Parent Service  │  │ Gamification    │  │  Forum Service  │          │  │
│  │  │                 │  │   Service       │  │                 │          │  │
│  │  │ • Zalo OTP Reg  │  │                 │  │  • Q&A Forum    │          │  │
│  │  │ • Track child   │  │  • Points       │  │  • Discussions  │          │  │
│  │  │ • Notifications │  │  • Badges       │  │  • Comments     │          │  │
│  │  │ • Billing view  │  │  • Leaderboard  │  │  • Voting       │          │  │
│  │  │                 │  │  • Rewards      │  │                 │          │  │
│  │  │                 │  │                 │  │                 │          │  │
│  │  │ ₫100k/tháng     │  │  ₫150k/tháng    │  │  ₫100k/tháng    │          │  │
│  │  │ RAM: ~150MB     │  │  RAM: ~180MB    │  │  RAM: ~150MB    │          │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘          │  │
│  │                                                                           │  │
│  │  Bundle: All 3 = ₫300k/tháng (giảm ₫50k)                                 │  │
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

### 1.2. So sánh Service Count

| Gói | Core Services | Expand Services | Media Service | Total | RAM Usage |
|-----|---------------|-----------------|---------------|-------|-----------|
| **BASIC** | 3 services | 0 | 0 | 3 | ~1.25GB |
| **BASIC + Parent** | 3 | 1 (Parent) | 0 | 4 | ~1.40GB |
| **STANDARD** | 3 | 3 (all expand) | 1 (Media) | 7 | ~2.53GB |
| **PREMIUM** | 3 | 3 (all expand) | 1 (Media+) | 7 | ~2.53GB |

**Pricing:**
- BASIC: ₫299k/tháng
- BASIC + Parent: ₫299k + ₫100k = ₫399k/tháng
- BASIC + Gamification: ₫299k + ₫150k = ₫449k/tháng
- STANDARD: ₫299k + ₫300k (expand bundle) + ₫200k (media) = ₫799k/tháng
- PREMIUM: ₫299k + ₫300k + ₫400k (media+) = ₫999k/tháng

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

## 4. Pricing Model V4.0 (Unbundled)

### 4.1. Pricing Tiers

```
┌─────────────────────────────────────────────────────────────────┐
│                      PRICING V4.0 (Unbundled)                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  CORE (Required):                                                │
│  ├─ Gateway + Core + Frontend          ₫299k/tháng              │
│  └─ Features: Student, Teacher, Class, Attendance, Grading       │
│                                                                  │
│  EXPAND (Pick & Choose):                                         │
│  ├─ Parent Service                     ₫100k/tháng              │
│  ├─ Gamification Service               ₫150k/tháng              │
│  ├─ Forum Service                      ₫100k/tháng              │
│  └─ Bundle (all 3)                     ₫300k/tháng (save ₫50k)  │
│                                                                  │
│  MEDIA (Optional):                                               │
│  ├─ STANDARD (100GB storage, 500GB bandwidth)  ₫200k/tháng      │
│  └─ PREMIUM (500GB storage, 2TB bandwidth)     ₫400k/tháng      │
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

### 6.1. Business Benefits

| Metric | V3.1 | V4.0 | Improvement |
|--------|------|------|-------------|
| **Average revenue/customer** | ₫45k | ₫60k | **+33%** |
| **Conversion rate (entry)** | 15% (₫300k barrier) | 30% (₫100k entry) | **+100%** |
| **Customer satisfaction** | Trả cho bundle dù không cần | Trả đúng những gì cần | **Higher** |
| **Upsell rate** | Khó (all-or-nothing) | Dễ (từ 1→3 services) | **Easier** |

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

**V4.0 Architecture Decisions:**
- ✅ **Tách Parent, Gamification, Forum** thành 3 services riêng
- ✅ **Clone Ant Media Server CE** cho Media Service
- ✅ **Unbundled pricing** model

**Expected Outcomes:**
- 📈 **+33% revenue** per customer
- ⚡ **60% RAM saving** for customers chỉ cần 1 expand service
- ⏱️ **5-7 months faster** time to market (Media Service)
- 💰 **₫70-90M cost saving** (development)

**Next Steps:**
1. Stakeholder approval
2. Implementation Phase 1 (Parent Service separation - 1 tháng)
3. Implementation Phase 2 (Media Service setup - 6 tuần)
4. Implementation Phase 3 (Gamification & Forum - 2 tháng)

---

**Document Version:** 4.0 Addendum
**Status:** Ready for Implementation
**Author:** Nguyễn Văn Kiệt
**Date:** 2026-01-30
