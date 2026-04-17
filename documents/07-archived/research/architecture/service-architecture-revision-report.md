# Báo Cáo Đánh Giá Lại Kiến Trúc Service - KiteClass Platform

**Ngày tạo:** 2026-01-30
**Phiên bản:** 1.1 (V4.1 update) ⭐
**Last Updated:** 2026-02-26
**Tác giả:** Nguyễn Văn Kiệt
**Loại:** Architecture Decision Record (ADR)

> ⭐ **V4.1 Update**: Thêm quyết định bundling LMS + Marketing modules vào Core Service thay vì tạo services riêng. Chi tiết xem `system-architecture-v4.md`.

---

## Executive Summary

Báo cáo này đánh giá 2 thay đổi quan trọng đối với kiến trúc service của KiteClass Platform:

1. **Tách Parent Module thành service độc lập** (hiện tại đang tích hợp trong Engagement Service)
2. **Sử dụng open-source có sẵn cho Media Service** thay vì phát triển từ đầu

**Kết luận:**
- ✅ **Chấp thuận** tách Parent Service (độc lập, flexible pricing)
- ✅ **Chấp thuận** clone/customize open-source cho Media Service (tiết kiệm thời gian, mature technology)

---

## Table of Contents

1. [Luận điểm 1: Tách Parent thành Service riêng](#luận-điểm-1-tách-parent-thành-service-riêng)
2. [Luận điểm 2: Clone Open-source cho Media Service](#luận-điểm-2-clone-open-source-cho-media-service)
3. [Best Practices & Recommendations](#best-practices--recommendations)
4. [Implementation Roadmap](#implementation-roadmap)
5. [Risk Assessment](#risk-assessment)
6. [Storage Integration Decision](#storage-integration-decision)

---

## Storage Integration Decision (Updated 2026-02-27)

### Context

KiteClass Platform cần file storage cho avatars, documents, videos, certificates. Câu hỏi: Dedicated Storage Service hay integrate trực tiếp vào Core Service?

### Design Finalized

**Decision**: Dedicated **Storage Service** (PR 2.10.1) với S3-compatible storage backend.

**Rationale**:
- **Separation of concerns**: File management logic tách biệt với business logic (Student, Course, Attendance)
- **Scalability**: Storage Service có thể scale independently khi file volume tăng
- **Reusability**: Frontend, Gateway, Media Service đều có thể dùng chung Storage APIs
- **Technology choice**: S3 SDK, presigned URLs, quota tracking - không làm phình Core Service

### Architecture

**Object Storage**: MinIO (dev), AWS S3 hoặc CloudFlare R2 (prod)
- **MinIO** (dev): Self-hosted S3-compatible, Testcontainers cho integration tests
- **AWS S3** (prod): Managed service, high availability
- **CloudFlare R2** (prod alternative): Zero egress fees, cheaper for CDN use case

**Upload Method**: Presigned URLs (client uploads directly to S3, bypass backend)
- **Pros**: Lower backend load, faster upload (no proxy)
- **Cons**: Client phải handle S3 errors (network issues, quota exceeded)
- **Why**: Scalability - backend chỉ generate URL, không stream file data

**Metadata Storage**: PostgreSQL (`uploaded_files`, `storage_quotas` tables)
- **uploaded_files**: File metadata (original filename, storage path, size, status, access control)
- **storage_quotas**: Per-tenant quota tracking (Trial: 500MB, Basic: 5GB, Pro: 50GB)

**Multi-tenant Isolation**:
- **Storage path prefix**: `{tenant-id}/{file-type}/{uuid}.{ext}` (e.g., `tenant-123/avatars/abc.png`)
- **Database filter**: Hibernate `@FilterDef` on `instance_id` column
- **S3 bucket policies** (optional): IAM policies prevent cross-tenant access

### Migration Path

**V13 Migration**: `V13__create_file_storage_tables.sql`
- Create `uploaded_files` table (15 columns: id, instance_id, uploaded_by, file_type, storage_path, file_size_bytes, mime_type, status, access_level, etc.)
- Create `storage_quotas` table (7 columns: id, instance_id, quota_bytes, used_bytes, last_calculated_at, etc.)
- Add indexes for performance (instance_id, uploaded_by, file_type, status)

**Backward Compatibility**:
- Keep existing `avatar_url VARCHAR(500)` columns in students/teachers tables
- Add new `avatar_file_id UUID` column (nullable)
- Migrate existing URLs to `uploaded_files` records (background job)
- Phase out `avatar_url` after 2-3 versions

### Quota Enforcement

**Default Tiers**:
- **Trial**: 500 MB (524,288,000 bytes)
- **Basic**: 5 GB (5,368,709,120 bytes)
- **Pro**: 50 GB (53,687,091,200 bytes)
- **Enterprise**: Custom (unlimited)

**Enforcement Point**: Before generating presigned upload URL
- Check current usage: `SELECT used_bytes FROM storage_quotas WHERE instance_id = ?`
- Check if `used_bytes + file_size <= quota_bytes`
- If exceeded → Reject with `403 STORAGE_QUOTA_EXCEEDED`

**Scheduled Job**: Daily quota recalculation
```sql
UPDATE storage_quotas sq
SET used_bytes = (
    SELECT COALESCE(SUM(file_size_bytes), 0)
    FROM uploaded_files uf
    WHERE uf.instance_id = sq.instance_id
      AND uf.status = 'READY'
      AND uf.deleted = FALSE
),
last_calculated_at = NOW();
```

### Access Control

**Three Levels**:
- **PRIVATE**: Only `uploaded_by` user can download (e.g., personal documents)
- **COURSE**: Teacher + enrolled students can download (e.g., course syllabus, lecture videos)
- **PUBLIC**: All authenticated users can download (e.g., marketing images, public teacher profiles)

**Enforcement**: In `FileService.generateDownloadUrl()` method
- Fetch `uploaded_files` record
- Check `access_level`:
  - PRIVATE: Verify `uploaded_by == current_user_id`
  - COURSE: Verify user is teacher OR enrolled student (query enrollments table)
  - PUBLIC: Allow if authenticated
- Generate presigned download URL (24h expiry)

### Related PRs

- **PR 2.10.1**: Storage & File Management Service (Core)
- **PR 3.10**: Profile picture upload (Frontend)
- **PR 3.12**: Guest Pages (teacher photos, hero images)

**Documentation**: See [Storage Service Design](../../03-planning/implementation/storage-service-design.md) for complete API flows, testing guides, and local development setup (MinIO Console).

### Comparison: Storage Service vs Core Integration

| Aspect | Dedicated Storage Service ✅ | Integrated in Core |
|--------|------------------------------|---------------------|
| **Separation of Concerns** | ✅ Clean separation | ❌ Mixed with business logic |
| **Scalability** | ✅ Independent scaling | ❌ Core Service bloat |
| **Reusability** | ✅ Shared APIs (Gateway, Frontend, Media) | ⚠️ Core-specific |
| **Technology Stack** | ✅ S3 SDK isolated | ❌ Core dependencies grow |
| **Testing** | ✅ MinIO Testcontainer isolated | ❌ Core tests slower |
| **Deployment** | ⚠️ One more service | ✅ Simpler deployment |

**Decision**: Dedicated Storage Service wins - benefits outweigh deployment complexity.

---

## Luận điểm 1: Tách Parent thành Service riêng

### 1.1. Tình trạng hiện tại

**Engagement Service (Kiến trúc hiện tại v3.1):**
```
┌──────────────────────────────────────────────────────┐
│         ENGAGEMENT SERVICE (Tùy chọn)                │
├──────────────────────────────────────────────────────┤
│                                                       │
│  ┌───────────────┐  ┌───────────────┐  ┌──────────┐ │
│  │ GAMIFICATION  │  │ PARENT PORTAL │  │  FORUM   │ │
│  │               │  │               │  │          │ │
│  │ • Points      │  │ • Register    │  │ • Q&A    │ │
│  │ • Badges      │  │ • Track child │  │ • Discuss│ │
│  │ • Leaderboard │  │ • Notifications│ │          │ │
│  │ • Rewards     │  │ • Billing     │  │          │ │
│  └───────────────┘  └───────────────┘  └──────────┘ │
│                                                       │
│  Pricing: STANDARD/PREMIUM hoặc +₫300k/tháng        │
└──────────────────────────────────────────────────────┘
```

**Vấn đề:**
- Parent, Gamification, Forum bundled trong 1 service
- Pricing không linh hoạt (all-or-nothing)
- Không thể bán riêng Parent Portal cho khách hàng chỉ cần tính năng này

### 1.2. Phân tích Use Cases & Độc lập

| Module | Use Cases | Phụ thuộc Core? | Độc lập với modules khác? |
|--------|-----------|-----------------|---------------------------|
| **Gamification** | 3 UCs (tích điểm, đổi quà, xếp hạng) | ✅ Yes (events từ Learning, Attendance) | ✅ Yes |
| **Parent Portal** | 3 UCs (đăng ký Zalo OTP, xem info con, nhận thông báo) | ✅ Yes (read Student, Attendance, Grades) | ✅ Yes |
| **Forum** | 2+ UCs (Q&A, discussions) | ⚠️ Partial (references to Class, User) | ✅ Yes |

**Kết luận phân tích:**
- **3 modules hoàn toàn độc lập về logic nghiệp vụ**
- Không có dependency lẫn nhau
- Mỗi module có thể được enable/disable riêng biệt

### 1.3. Lý do NÊN tách Parent Service

#### ✅ Lý do 1: Tính năng bổ trợ, không phải core

**Thực tế thị trường:**
- **Trung tâm nhỏ (BASIC plan)**: Không cần Parent Portal (học viên là người lớn hoặc quản lý trực tiếp)
- **Trung tâm cho trẻ em**: CẦN Parent Portal (phụ huynh muốn theo dõi con)
- **Trường cấp 3**: CẦN Parent Portal (phụ huynh kiểm tra học tập)
- **Khóa học online cho người đi làm**: KHÔNG CẦN Parent Portal

**Tỷ lệ ước tính:**
- ~40% khách hàng cần Parent Portal
- ~60% khách hàng không cần (wasted resources nếu bundle)

#### ✅ Lý do 2: Pricing linh hoạt hơn

**Kiến trúc hiện tại (Bundled):**
```
Engagement Pack = Gamification + Parent + Forum = ₫300k/tháng
```

**Vấn đề:**
- Khách hàng chỉ cần Parent → phải trả ₫300k cho cả bundle
- Khách hàng chỉ cần Gamification → phải trả ₫300k cho cả bundle
- Không thể mix & match

**Kiến trúc mới (Unbundled):**
```
Parent Service     = ₫100k/tháng
Gamification Pack  = ₫150k/tháng
Forum Pack         = ₫100k/tháng
All 3 (Bundle)     = ₫300k/tháng (giảm ₫50k)
```

**Lợi ích:**
- ✅ Khách hàng trả đúng những gì họ cần
- ✅ Tăng conversion rate (entry price thấp hơn)
- ✅ Upsell dễ dàng hơn (từ 1 service sang nhiều services)

#### ✅ Lý do 3: Resource optimization

**RAM usage:**
```
Engagement Service (current) = 384MB (cả 3 modules)
Parent Service alone         = ~150MB (ước tính)
Gamification Service alone   = ~180MB (ước tính)
Forum Service alone          = ~150MB (ước tính)
```

**Nếu khách hàng chỉ cần Parent:**
- Hiện tại: Deploy cả Engagement (384MB) → lãng phí 234MB
- Tách riêng: Deploy Parent (150MB) → tiết kiệm 60%

**Chi phí tiết kiệm:**
- 100 instances chỉ dùng Parent: Tiết kiệm ~23GB RAM
- Tương đương: ~2-3 servers (8GB RAM each) không cần provision

#### ✅ Lý do 4: Independent scaling

**Ví dụ thực tế:**
```
Trung tâm A:
- 500 students
- Parent Portal usage: Cao (phụ huynh check 3-5 lần/ngày)
- Gamification usage: Thấp (1-2 lần/tuần)

Trung tâm B:
- 300 students
- Parent Portal usage: Không có
- Gamification usage: Cao (event-driven, tích điểm real-time)
```

**Với kiến trúc hiện tại:**
- Cả 2 trung tâm phải deploy Engagement Service giống nhau
- Không thể scale riêng module

**Với kiến trúc tách riêng:**
- Trung tâm A: Scale Parent Service (2 replicas), Gamification Service (1 replica)
- Trung tâm B: Không deploy Parent, Gamification Service (3 replicas)

#### ✅ Lý do 5: Fault isolation tốt hơn

**Kịch bản lỗi:**
```
Gamification Service có bug trong logic tích điểm
→ Service crash
```

**Kiến trúc hiện tại:**
- Cả Engagement Service down
- Parent Portal không hoạt động (affected)
- Forum không hoạt động (affected)

**Kiến trúc tách riêng:**
- Chỉ Gamification Service down
- Parent Portal vẫn hoạt động ✅
- Forum vẫn hoạt động ✅

### 1.4. Lý do KHÔNG NÊN tách (Phản biện)

#### ❌ Phản biện 1: Tăng độ phức tạp

**Vấn đề:**
- 1 service → 3 services = 3x complexity
- Deployment, monitoring, logging phức tạp hơn

**Đáp lại:**
- ✅ Kubernetes giúp quản lý multi-services dễ dàng (auto-scale, health check)
- ✅ Complexity có thể accept được vì benefits lớn hơn
- ✅ Monitoring tools (Prometheus, Grafana) handle multi-services tốt

#### ❌ Phản biện 2: Chi phí infrastructure tăng

**Vấn đề:**
- 1 service = 1 deployment
- 3 services = 3 deployments (base overhead: ~50MB x 3 = 150MB)

**Đáp lại:**
- ✅ Optional services → chỉ deploy khi cần
- ✅ Tiết kiệm lớn hơn chi phí tăng (xem section 1.3.3)
- ✅ Base overhead nhỏ so với lợi ích

#### ❌ Phản biện 3: Inter-service communication overhead

**Vấn đề:**
- Nếu services cần gọi nhau → latency tăng

**Đáp lại:**
- ✅ **Kiểm tra dependencies**: 3 modules KHÔNG gọi nhau
  - Parent đọc từ Core (Student, Attendance, Grades)
  - Gamification nhận events từ Core (event-driven)
  - Forum references User/Class từ Core
- ✅ Không có inter-service calls giữa 3 modules
- ✅ Latency không tăng

### 1.5. Đề xuất kiến trúc mới

#### Option A: Tách hoàn toàn 3 services

```
┌────────────────────────────────────────────────────────────────┐
│                  EXPAND SERVICES (Tùy chọn)                     │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────┐                                        │
│  │  PARENT SERVICE    │  ₫100k/tháng                           │
│  │  (Java Spring Boot)│                                        │
│  │                    │                                        │
│  │  • Zalo OTP Reg    │                                        │
│  │  • Track children  │                                        │
│  │  • Notifications   │                                        │
│  │  • Billing view    │                                        │
│  │                    │                                        │
│  │  RAM: ~150MB       │                                        │
│  └────────────────────┘                                        │
│                                                                 │
│  ┌────────────────────┐                                        │
│  │ GAMIFICATION SVC   │  ₫150k/tháng                           │
│  │  (Java Spring Boot)│                                        │
│  │                    │                                        │
│  │  • Points system   │                                        │
│  │  • Badges          │                                        │
│  │  • Leaderboards    │                                        │
│  │  • Reward store    │                                        │
│  │                    │                                        │
│  │  RAM: ~180MB       │                                        │
│  └────────────────────┘                                        │
│                                                                 │
│  ┌────────────────────┐                                        │
│  │  FORUM SERVICE     │  ₫100k/tháng                           │
│  │  (Java Spring Boot)│                                        │
│  │                    │                                        │
│  │  • Q&A Forum       │                                        │
│  │  • Discussions     │                                        │
│  │  • Comments        │                                        │
│  │                    │                                        │
│  │  RAM: ~150MB       │                                        │
│  └────────────────────┘                                        │
│                                                                 │
│  Bundle: All 3 = ₫300k/tháng (save ₫50k)                      │
└────────────────────────────────────────────────────────────────┘
```

**✅ Đánh giá:**
- Pros: Maximum flexibility, independent scaling, fault isolation
- Cons: Deployment complexity tăng (3 services)

#### Option B: Parent độc lập, Gamification + Forum vẫn bundle

```
┌────────────────────────────────────────────────────────────────┐
│                  EXPAND SERVICES (Tùy chọn)                     │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────┐                                        │
│  │  PARENT SERVICE    │  ₫100k/tháng                           │
│  │  (Java Spring Boot)│                                        │
│  │  RAM: ~150MB       │                                        │
│  └────────────────────┘                                        │
│                                                                 │
│  ┌─────────────────────────────────────────┐                  │
│  │  ENGAGEMENT SERVICE                      │  ₫200k/tháng    │
│  │  (Gamification + Forum)                  │                  │
│  │                                          │                  │
│  │  • Points, Badges, Leaderboards, Rewards │                  │
│  │  • Q&A Forum, Discussions                │                  │
│  │                                          │                  │
│  │  RAM: ~330MB                             │                  │
│  └─────────────────────────────────────────┘                  │
│                                                                 │
│  Bundle: Parent + Engagement = ₫250k/tháng (save ₫50k)        │
└────────────────────────────────────────────────────────────────┘
```

**⚠️ Đánh giá:**
- Pros: Giảm complexity (2 services thay vì 3), vẫn tách Parent
- Cons: Gamification & Forum vẫn phải bundle (ít flexible hơn)

### 1.6. Recommendation

**✅ Chọn Option A (Tách hoàn toàn 3 services)**

**Lý do:**
1. **Maximum flexibility**: Khách hàng chọn chính xác những gì họ cần
2. **Better resource utilization**: Tiết kiệm RAM đáng kể
3. **Independent scaling**: Mỗi service scale theo usage riêng
4. **Fault isolation**: Bug trong 1 service không ảnh hưởng services khác
5. **Easier to sell**: Entry price thấp (₫100k cho 1 service thay vì ₫300k cho bundle)

**Trade-offs chấp nhận được:**
- Deployment complexity tăng → Kubernetes giải quyết tốt
- Infrastructure overhead → Tiết kiệm lớn hơn chi phí

---

## Luận điểm 2: Clone Open-source cho Media Service

### 2.1. Tình trạng hiện tại

**Theo media-service-analysis.md:**
- Đã phân tích 6 giải pháp open-source
- 3 options được đề xuất (A: Đơn giản, B: Đầy đủ, C: Scale)

**Câu hỏi cần trả lời:**
- Build từ đầu vs. Clone & customize open-source?
- Giải pháp nào phù hợp cho KiteClass?

### 2.2. So sánh Build from Scratch vs. Clone Open-source

| Tiêu chí | Build from Scratch | Clone Open-source |
|----------|-------------------|-------------------|
| **Time to market** | 3-6 tháng | 1-2 tuần setup |
| **Development cost** | ~₫200-300 triệu | ~₫20-30 triệu |
| **Maintenance effort** | Cao (tự maintain toàn bộ) | Thấp (community support) |
| **Feature completeness** | Chỉ những gì cần | Đầy đủ features (có thể thừa) |
| **Customization** | 100% custom | 70-80% custom |
| **Learning value** | Rất cao | Trung bình |
| **Production readiness** | Cần test kỹ | Đã mature, battle-tested |
| **Bug risk** | Cao (new codebase) | Thấp (đã có users) |
| **Community support** | Không | Có (documentation, forums) |
| **Scalability** | Phụ thuộc kinh nghiệm | Đã được verify ở scale lớn |

### 2.3. Phân tích Requirements KiteClass

**KiteClass Platform cần:**

| Feature | Priority | Complexity | Build effort |
|---------|----------|------------|--------------|
| **Video on Demand (VOD)** | Cao | Trung bình | ~2 tháng |
| **Video Transcoding** | Cao | Cao | ~1.5 tháng |
| **Adaptive Bitrate (HLS/DASH)** | Cao | Cao | ~1 tháng |
| **Live Streaming** | Trung bình | Cao | ~2 tháng |
| **Recording** | Trung bình | Trung bình | ~1 tháng |
| **Storage Integration** | Cao | Thấp | ~2 tuần |

**Tổng ước tính build from scratch:** ~6-8 tháng

**Nếu dùng open-source:**
- Setup Ant Media Server CE: 1-2 ngày
- Customize & integrate với KiteClass: 1-2 tuần
- **Tổng:** ~3-4 tuần

**Tiết kiệm:** ~5-7 tháng = ~70-90 triệu đồng chi phí development

### 2.4. Đánh giá các giải pháp Open-source

#### Option 1: Ant Media Server CE (Recommended cho MVP)

**Repo:** https://github.com/ant-media/Ant-Media-Server (4,000+ stars)

**✅ Ưu điểm:**
- All-in-one solution (VOD + Live + WebRTC)
- Auto transcoding (HLS, DASH)
- Dashboard có sẵn
- REST API đầy đủ
- Documentation tốt
- Active community
- Apache 2.0 license (commercial-friendly)

**❌ Nhược điểm:**
- Community Edition có giới hạn (no clustering)
- Cần server mạnh (8 cores, 16GB RAM recommend)

**💰 Chi phí:**
- Free (Community Edition)
- Server: ~$96-120/tháng (8 vCPU, 16GB RAM)

**🎯 Phù hợp cho:**
- MVP và production (1-1000 concurrent users)
- Đầy đủ features
- Time to market nhanh

#### Option 2: OvenMediaEngine (Alternative cao cấp)

**Repo:** https://github.com/AirenSoft/OvenMediaEngine (3,000+ stars)

**✅ Ưu điểm:**
- Ultra-low latency (sub-second WebRTC)
- High performance (Made in Korea)
- 100% free, no limits
- GPLv2 license

**❌ Nhược điểm:**
- Documentation chủ yếu tiếng Hàn
- Community nhỏ hơn Ant Media
- Không có dashboard built-in

**💰 Chi phí:**
- Free
- Server: ~$96-120/tháng

**🎯 Phù hợp cho:**
- Production scale (>1000 concurrent users)
- Cần ultra-low latency
- Team có kinh nghiệm

#### Option 3: MinIO + FFmpeg + nginx (Simplest)

**Stack:**
- MinIO: Object storage (S3-compatible)
- FFmpeg: Transcoding
- nginx: HLS delivery
- Video.js: Player

**✅ Ưu điểm:**
- Rất nhẹ
- Chi phí thấp (~$48-60/tháng)
- Dễ hiểu, dễ customize
- Phù hợp cho đồ án tốt nghiệp

**❌ Nhược điểm:**
- Chỉ VOD (không có Live)
- Cần tự implement transcoding pipeline
- Không có dashboard

**💰 Chi phí:**
- Free (open-source)
- Server: ~$48-60/tháng (4 vCPU, 8GB RAM)

**🎯 Phù hợp cho:**
- Đồ án tốt nghiệp (thesis demo)
- MVP chỉ cần VOD
- Budget thấp

### 2.5. Recommendation: Multi-phase Approach

#### Phase 1: Đồ án Tốt nghiệp (2-3 tháng)

**Giải pháp:** MinIO + FFmpeg + nginx (Option 3)

**Lý do:**
- ✅ Đủ để demo các tính năng (VOD)
- ✅ Chi phí thấp (~$50/tháng)
- ✅ Dễ setup, maintain
- ✅ Có thể chạy local để test
- ✅ Learning value cao (hiểu rõ transcoding pipeline)

**Deliverables:**
```
✅ Video upload API
✅ FFmpeg transcoding (360p, 720p, 1080p)
✅ HLS streaming
✅ Video.js player integration
✅ Progress tracking
```

#### Phase 2: MVP Production (3-6 tháng sau thesis)

**Giải pháp:** Clone & customize Ant Media Server CE

**Migration path:**
```
1. Keep MinIO storage (đã có videos)
2. Deploy Ant Media Server CE
3. Configure Ant Media để đọc từ MinIO
4. Add Live Streaming feature
5. Add WebRTC support
6. Migrate existing videos (nếu cần)
```

**Lợi ích:**
- ✅ Giữ lại code từ Phase 1 (storage layer)
- ✅ Thêm Live Streaming và WebRTC
- ✅ Dashboard có sẵn (giảm effort)
- ✅ Production-ready

#### Phase 3: Scale (1-2 năm sau)

**Giải pháp:** Đánh giá lại dựa trên usage

**Options:**
- **Nếu < 1000 concurrent users:** Giữ Ant Media Server CE
- **Nếu > 1000 concurrent users:** Migrate sang OvenMediaEngine hoặc Hybrid
- **Nếu budget thoải mái:** Outsource sang Cloudflare Stream / Bunny.net

### 2.6. Customization Strategy

**Customize gì trong open-source?**

#### Must Customize:

1. **Storage Integration**
   ```
   Ant Media Server → MinIO (thay vì local storage)
   - S3-compatible API
   - Custom upload path (per instance)
   ```

2. **Authentication & Authorization**
   ```
   Ant Media Server → KiteClass JWT
   - Verify JWT token từ Gateway
   - Check permissions (TEACHER, STUDENT)
   ```

3. **Webhook Events**
   ```
   Ant Media → KiteClass Core
   - Stream started/ended
   - Recording finished
   - Transcoding complete
   ```

4. **Database Integration**
   ```
   Ant Media metadata → KiteClass PostgreSQL
   - Video metadata
   - Live stream sessions
   - Analytics
   ```

#### Nice to Have:

5. **Custom Dashboard**
   ```
   Replace Ant Media Dashboard → KiteClass Admin UI
   - Consistent branding
   - Integrated với KiteClass features
   ```

6. **CDN Integration**
   ```
   Ant Media → Cloudflare CDN
   - Faster delivery
   - Reduce bandwidth cost
   ```

**Ước tính effort customize:**
- Must Customize: ~2-3 tuần
- Nice to Have: ~1-2 tuần
- **Tổng:** ~3-5 tuần (vẫn nhanh hơn build from scratch ~5 tháng)

### 2.7. License Considerations

| Solution | License | Commercial Use | Modify & Distribute | Attribution Required |
|----------|---------|----------------|---------------------|----------------------|
| **Ant Media CE** | Apache 2.0 | ✅ Yes | ✅ Yes | ⚠️ Yes (in docs) |
| **OvenMediaEngine** | GPLv2 | ⚠️ Yes (if open-source app) | ✅ Yes | ✅ Yes |
| **MinIO** | AGPL v3 | ⚠️ Yes (if API-only) | ✅ Yes | ✅ Yes |
| **FFmpeg** | LGPL/GPL | ✅ Yes | ⚠️ Yes (complex) | ✅ Yes |
| **nginx** | BSD | ✅ Yes | ✅ Yes | ⚠️ Yes |

**✅ Recommendation:**
- Sử dụng **Ant Media Server CE (Apache 2.0)** → No legal risk, commercial-friendly
- Avoid OvenMediaEngine (GPLv2) nếu không muốn open-source toàn bộ app
- MinIO (AGPL) OK nếu chỉ dùng qua API (không modify source)

### 2.8. Kết luận Luận điểm 2

**✅ Chấp nhận: Clone & customize open-source cho Media Service**

**Lý do:**
1. **Time to market**: 3-5 tuần vs. 6-8 tháng (build from scratch)
2. **Cost-effective**: Tiết kiệm ~70-90 triệu đồng development cost
3. **Battle-tested**: Đã được verify bởi thousands of users
4. **Community support**: Documentation, forums, examples
5. **Lower risk**: Ít bugs hơn new codebase
6. **Scalability proven**: Đã handle production traffic lớn

**Chiến lược:**
- **Phase 1 (Thesis):** MinIO + FFmpeg + nginx (simple, learning)
- **Phase 2 (MVP):** Ant Media Server CE (full features)
- **Phase 3 (Scale):** Đánh giá lại (OvenMediaEngine hoặc outsource)

---

## Best Practices & Recommendations

### 3.1. Service Architecture Best Practices

#### Rule 1: Tách service khi có đủ 3 điều kiện

**3 điều kiện:**
1. ✅ **Business independence:** Có thể bán riêng biệt
2. ✅ **Technical independence:** Không phụ thuộc logic lẫn nhau
3. ✅ **Operational value:** Lợi ích (flexibility, scaling) > chi phí (complexity)

**Áp dụng cho Parent:**
- ✅ Business: Có thể bán riêng (₫100k/tháng)
- ✅ Technical: Không gọi Gamification/Forum
- ✅ Operational: Flexibility lớn, tiết kiệm RAM đáng kể

**→ Kết luận: NÊN tách**

#### Rule 2: Không tách service nếu chỉ vì "microservices hype"

**Anti-pattern:**
```
❌ Tách User Service thành:
   - UserProfile Service
   - UserAuth Service
   - UserPreference Service
```

**Lý do SAI:**
- Quá granular
- Không có business value riêng
- Inter-service calls quá nhiều
- Complexity >> benefits

**Đúng:**
```
✅ Giữ User Service là 1 service
   - Bao gồm: Profile, Auth, Preference
   - Bounded context rõ ràng
```

#### Rule 3: Clone open-source khi có sẵn mature solution

**Khi NÊN clone:**
- ✅ Problem đã được giải quyết tốt (mature project)
- ✅ Open-source license phù hợp (Apache 2.0, MIT)
- ✅ Active community
- ✅ Customization cần thiết < 30% codebase

**Khi KHÔNG NÊN clone:**
- ❌ Core business logic (competitive advantage)
- ❌ License không phù hợp (GPL với closed-source app)
- ❌ Customization cần > 50% codebase
- ❌ Learning là mục tiêu chính (thesis riêng về topic đó)

**Áp dụng cho Media Service:**
- ✅ Mature: Ant Media Server đã 5+ years
- ✅ License: Apache 2.0 OK
- ✅ Community: 4,000+ stars, active
- ✅ Customize: ~20% (storage, auth, webhooks)

**→ Kết luận: NÊN clone**

### 3.2. Pricing Strategy Best Practices

#### Unbundled Pricing Model

**Cũ (Bundled):**
```
Engagement Pack = ₫300k/tháng (all-or-nothing)
Conversion rate: ~15% (cao ngưỡng)
```

**Mới (Unbundled):**
```
Parent Service      = ₫100k/tháng  → Entry point thấp
Gamification Pack   = ₫150k/tháng  → Add-on
Forum Pack          = ₫100k/tháng  → Add-on
Bundle (all 3)      = ₫300k/tháng  → Discount ₫50k
```

**Lợi ích:**
- ✅ **Tăng conversion rate:** Entry price ₫100k vs. ₫300k
- ✅ **Upsell dễ hơn:** Từ 1 service → nhiều services
- ✅ **Customer satisfaction:** Trả cho đúng những gì cần

**Dự đoán:**
```
Trước: 15% customers × ₫300k = ₫45k/customer average
Sau:
  - 30% customers × ₫100k (Parent only)     = ₫30k
  - 10% customers × ₫150k (Gamification)    = ₫15k
  - 5% customers × ₫300k (Bundle)           = ₫15k
Total: ₫60k/customer average (+33% revenue)
```

### 3.3. Technical Architecture Decisions

#### Decision 1: Service Granularity

**Framework:**
```
Domain Size:
  - Nano (<5 use cases)    → Too small, bundle with others
  - Micro (5-15 use cases) → Perfect for microservice ✅
  - Medium (15-50 use cases) → OK, consider splitting
  - Large (>50 use cases)  → Should split
```

**Áp dụng:**
```
Parent Module:      3 use cases  → Micro (OK) ✅
Gamification:       3 use cases  → Micro (OK) ✅
Forum:              2 use cases  → Nano (consider bundle with Gamification)
Core (Student, Class): 50+ use cases → Large (already OK)
```

**Recommendation:**
- Parent: Tách ✅
- Gamification: Tách ✅
- Forum: Có thể bundle với Gamification hoặc tách (flexible)

#### Decision 2: Technology Reuse vs. Build

**Decision Matrix:**

|  | Mature OS exists | Core competency | Learning goal | Decision |
|--|------------------|-----------------|---------------|----------|
| **Media Streaming** | ✅ Yes (Ant Media) | ❌ No | ⚠️ Moderate | ✅ Clone & customize |
| **Authentication** | ✅ Yes (Keycloak) | ✅ Yes | ❌ No | ⚠️ Build (custom logic) |
| **Payment** | ✅ Yes (VNPay SDK) | ❌ No | ❌ No | ✅ Use SDK as-is |
| **AI Branding** | ⚠️ Partial (OpenAI) | ✅ Yes | ✅ Yes | ✅ Build (use OpenAI API) |

**Guideline:**
- Clone: Commodity features (streaming, storage, messaging)
- Build: Competitive advantage features (AI branding, multi-tenant logic)

### 3.4. Migration & Rollback Strategy

#### For Parent Service Migration

**Step-by-step:**
```
Week 1: Create Parent Service skeleton
  - Setup Spring Boot project
  - Setup PostgreSQL schema
  - Deploy to staging

Week 2: Migrate code from Engagement Service
  - Copy Parent module code
  - Update imports
  - Update configs

Week 3: Dual-write phase
  - Write to both Engagement & Parent Service
  - Read from Engagement (old)
  - Validate consistency

Week 4: Switch reads to Parent Service
  - Read from Parent (new)
  - Keep writing to both

Week 5: Deprecate Engagement Parent module
  - Stop writing to Engagement
  - Remove code from Engagement Service

Week 6: Monitor & optimize
  - Performance tuning
  - Bug fixes
```

**Rollback plan:**
```
If issues:
  - Week 3-4: Switch reads back to Engagement ✅
  - Week 5: Re-enable writes to Engagement ✅
  - Week 6: Can still rollback but harder
```

#### For Media Service Setup

**Incremental approach:**
```
Month 1 (Thesis): MinIO + FFmpeg + nginx
  - VOD only
  - Local testing
  - Demo-ready

Month 2-3 (Post-thesis): Ant Media Server CE
  - Deploy Ant Media to staging
  - Integrate with KiteClass
  - Parallel testing

Month 4: Production soft launch
  - New instances use Ant Media
  - Old instances keep MinIO + FFmpeg
  - Monitor performance

Month 5-6: Full migration
  - Migrate old instances
  - Deprecate MinIO + FFmpeg setup
```

---

## Implementation Roadmap

### Phase 1: Parent Service Separation (1 tháng)

**Sprint 1 (Week 1-2): Setup & Code Migration**
- [ ] Create `kiteclass-parent` Spring Boot project
- [ ] Setup PostgreSQL schema (parents, student_parent_links, notifications)
- [ ] Migrate code from Engagement Service
- [ ] Write unit tests (target: 80% coverage)
- [ ] Deploy to staging

**Sprint 2 (Week 3-4): Integration & Testing**
- [ ] Setup dual-write (Engagement + Parent)
- [ ] Integration tests with Gateway
- [ ] Load testing (simulate 100 concurrent parents)
- [ ] UAT with pilot customers

**Deliverables:**
- ✅ Parent Service deployed to production
- ✅ API documented (OpenAPI/Swagger)
- ✅ Monitoring dashboard (Grafana)

### Phase 2: Media Service Setup (6 tuần)

**Sprint 1 (Week 1-2): Thesis Demo Version**
- [ ] Setup MinIO on DigitalOcean ($48/month droplet)
- [ ] Implement FFmpeg transcoding script
- [ ] Setup nginx HLS server
- [ ] Video.js player integration
- [ ] Upload API (Spring Boot multipart)

**Sprint 2 (Week 3-4): Production Version**
- [ ] Deploy Ant Media Server CE on AWS ($96/month EC2)
- [ ] Configure Ant Media → MinIO integration
- [ ] Implement authentication (JWT verify)
- [ ] Webhook handlers (stream events → KiteClass DB)
- [ ] Admin dashboard customization

**Sprint 3 (Week 5-6): Testing & Optimization**
- [ ] Load testing (100 concurrent streams)
- [ ] CDN integration (Cloudflare)
- [ ] Monitoring setup (CPU, bandwidth usage)
- [ ] Documentation (API docs, user guide)

**Deliverables:**
- ✅ VOD working (upload → transcode → stream)
- ✅ Live streaming working (RTMP ingest → HLS output)
- ✅ Integrated with KiteClass Core

### Phase 3: Gamification & Forum Services (2 tháng)

**Sprint 1-2: Gamification Service**
- [ ] Create `kiteclass-gamification` project
- [ ] Migrate code from Engagement
- [ ] Event-driven architecture (listen to Core events)
- [ ] Testing & deployment

**Sprint 3-4: Forum Service (Optional)**
- [ ] Create `kiteclass-forum` project
- [ ] Migrate code from Engagement
- [ ] Testing & deployment

**Deliverables:**
- ✅ 3 independent services (Parent, Gamification, Forum)
- ✅ Flexible pricing model
- ✅ Deprecate old Engagement Service

---

## Risk Assessment

### 5.1. Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Ant Media Server CE limits** | Medium | Medium | Plan migration to OvenMediaEngine if needed |
| **Transcoding performance issues** | Low | High | Load test early, provision adequate CPU |
| **Storage costs exceed budget** | Medium | Medium | Implement video retention policy (delete old videos) |
| **Parent Service migration bugs** | Low | Medium | Dual-write phase + thorough testing |
| **Inter-service latency** | Low | Low | Services don't call each other (event-driven) |

### 5.2. Business Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Customers don't want unbundled pricing** | Low | Medium | Offer bundle discount, easy upgrade path |
| **Complexity increases support cost** | Medium | Low | Better documentation, admin training |
| **Market doesn't value Parent Portal** | Low | High | Market research first, pilot with 5-10 customers |

### 5.3. Operational Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Monitoring 3 services vs. 1** | Medium | Low | Centralized logging (ELK stack), Grafana dashboards |
| **Deployment complexity** | Medium | Low | CI/CD automation (GitHub Actions), Kubernetes |
| **On-call burden increases** | Low | Medium | Good alerting, runbooks, auto-recovery |

---

## Conclusion

### Summary of Decisions

| Decision | Status | Rationale |
|----------|--------|-----------|
| **Tách Parent Service** | ✅ **APPROVED** | Business independence, pricing flexibility, resource optimization |
| **Tách Gamification Service** | ✅ **APPROVED** | Same reasons as Parent |
| **Tách Forum Service** | ⚠️ **OPTIONAL** | Small (2 UCs), consider bundle with Gamification |
| **Clone Ant Media Server CE** | ✅ **APPROVED** (Phase 2) | Mature, time-saving, production-ready |
| **Use MinIO + FFmpeg (Thesis)** | ✅ **APPROVED** (Phase 1) | Simple, learning value, demo-ready |

### Expected Benefits

**Business Benefits:**
- 📈 **+33% revenue** (unbundled pricing model)
- 🎯 **Higher conversion rate** (lower entry price ₫100k vs. ₫300k)
- 😊 **Better customer satisfaction** (pay for what you need)

**Technical Benefits:**
- ⚡ **60% RAM saving** (deploy only needed services)
- 🔧 **Independent scaling** (scale per service)
- 🛡️ **Better fault isolation** (1 service down ≠ all down)
- ⏱️ **5-7 months time saving** (clone vs. build Media Service)

**Operational Benefits:**
- 💰 **~₫70-90M cost saving** (Media Service development)
- 📊 **Easier pricing strategy** (flexible packages)
- 🚀 **Faster time to market** (reuse mature open-source)

### Next Steps

1. **Week 1-2:** Approval & planning
   - Get stakeholder buy-in
   - Finalize architecture diagrams
   - Setup GitHub projects

2. **Month 1:** Parent Service separation
   - Sprint planning
   - Development & testing
   - Pilot deployment

3. **Month 2:** Media Service (Thesis version)
   - MinIO + FFmpeg setup
   - Thesis demo ready

4. **Month 3-4:** Media Service (Production version)
   - Ant Media Server integration
   - Full testing & optimization

5. **Month 5-6:** Gamification & Forum services
   - Complete service separation
   - Deprecate old Engagement Service
   - Launch new pricing model

---

## Appendix

### A. Technology Stack Summary

**Parent Service:**
- Java Spring Boot 3.x
- PostgreSQL
- Zalo OA API (OTP)
- WebSocket (real-time notifications)
- RAM: ~150MB

**Gamification Service:**
- Java Spring Boot 3.x
- PostgreSQL
- Event-driven (listen to Core events)
- RAM: ~180MB

**Forum Service:**
- Java Spring Boot 3.x
- PostgreSQL
- Full-text search (PostgreSQL FTS)
- RAM: ~150MB

**Media Service (Phase 1):**
- MinIO (storage)
- FFmpeg (transcoding)
- nginx (HLS delivery)
- Video.js (player)
- Server: 4 vCPU, 8GB RAM

**Media Service (Phase 2):**
- Ant Media Server CE (streaming)
- MinIO (storage)
- nginx (reverse proxy)
- Server: 8 vCPU, 16GB RAM

### B. Cost Comparison

| Scenario | Old Architecture | New Architecture | Savings |
|----------|------------------|------------------|---------|
| **Customer needs Parent only** | ₫300k/month (Engagement bundle) | ₫100k/month (Parent) | **₫200k/month** |
| **100 instances, 40% need Parent** | 40 × 384MB = 15.4GB RAM | 40 × 150MB = 6GB RAM | **9.4GB RAM** |
| **Media Service development** | Build from scratch: ₫90M | Clone Ant Media: ₫20M | **₫70M** |

### C. References

- **Service Architecture:**
  - `documents/reports/02-service-analysis/service-use-cases-v3.md`
  - `documents/reports/01-architecture/system-architecture-v3-final.md`

- **Media Service:**
  - `documents/reports/02-service-analysis/media-service-analysis.md`
  - Ant Media Server: https://github.com/ant-media/Ant-Media-Server
  - OvenMediaEngine: https://github.com/AirenSoft/OvenMediaEngine

- **Microservices Patterns:**
  - Martin Fowler: https://martinfowler.com/microservices/
  - Sam Newman: "Building Microservices" (O'Reilly, 2021)

---

**Document Status:** ✅ Ready for Review
**Next Review Date:** After implementation of Phase 1
**Author:** Nguyễn Văn Kiệt
**Date:** 2026-01-30
