# 🧪 Testing Landing Pages Locally

Guide để test Landing Pages với seed data đầy đủ.

## 📊 Seed Data Available

Sau khi start local, bạn sẽ có sẵn:

### **Demo School Tenant**
- **Tenant ID:** `11111111-1111-1111-1111-111111111111`
- **Domain:** `demo.kiteclass.com`
- **Status:** ACTIVE

### **Landing Page Data**
- **Hero Title:** "Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả"
- **Tagline:** "Nâng tầm giáo dục, tối ưu quản lý"
- **Primary Color:** #3B82F6 (blue)
- **Secondary Color:** #8B5CF6 (purple)
- **Contact:** support@kiteclass.com, 1900 xxxx

### **Published Courses (8 total)**

#### English Courses (Vietnamese):
1. **Tiếng Anh Giao Tiếp Cơ Bản** (Course ID: 5)
   - Code: ENG-101
   - Price: 3,000,000 VNĐ
   - Duration: 12 weeks (24 sessions)
   - Level: Beginner
   - Max Students: 20
   - **LMS Content:** 3 modules, 9 lessons (2 trial lessons)

2. **Tiếng Anh Thương Mại** (Course ID: 6)
   - Code: ENG-201
   - Price: 4,500,000 VNĐ
   - Duration: 16 weeks (32 sessions)
   - Level: Intermediate
   - Max Students: 15

3. **IELTS 7.0+ Chuyên Sâu** (Course ID: 7)
   - Code: IELTS-ADV
   - Price: 8,000,000 VNĐ
   - Duration: 20 weeks (60 sessions)
   - Level: Advanced
   - Max Students: 10

4. **Tiếng Anh Thiếu Nhi** (Course ID: 8)
   - Code: KID-ENG
   - Price: 2,500,000 VNĐ
   - Duration: 12 weeks (24 sessions)
   - Level: Beginner
   - Max Students: 12

#### Academic Courses (English):
5. **Algebra Fundamentals** (Course ID: 1) - PUBLISHED
6. **English Literature** (Course ID: 2) - PUBLISHED
7. **General Physics** (Course ID: 3) - PUBLISHED
8. **Introduction to Programming** (Course ID: 4) - DRAFT (not visible on public pages)

### **Teachers**
- Jane Doe (English specialist) - teaches all English courses
- John Smith (Math specialist)
- David Chen (Science specialist)
- Sarah Wilson (Assistant)

---

## 🚀 Quick Start Guide

### **1. Start Backend Services**

```bash
# Start all services (PostgreSQL, Redis, Core, Gateway)
docker-compose -f docker-compose.dev.yml up -d

# Wait for health checks (30-60 seconds)
docker-compose -f docker-compose.dev.yml ps

# Check logs if needed
docker-compose -f docker-compose.dev.yml logs -f kiteclass-core
```

**Database auto-migrations:**
- ✅ All tables created automatically
- ✅ Seed data loaded via Flyway migrations
- ✅ V16: Base test data (teachers, students, classes)
- ✅ V19: Landing page data
- ✅ V20: Enhanced course data + LMS modules/lessons

### **2. Start Frontend**

```bash
cd kiteclass/kiteclass-frontend
pnpm dev
```

Frontend will start at: `http://localhost:3000`

---

## 🧪 Test Scenarios

### **Scenario 1: Homepage (AI Branding)**

**URL:** `http://localhost:3000/`

**Expected:**
- ✅ Hero section với primary color gradient (#3B82F6)
- ✅ Hero title: "Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả"
- ✅ Tagline: "Nâng tầm giáo dục, tối ưu quản lý"
- ✅ Features section (3 cards: LMS, Quản lý Học viên, Thanh toán)
- ✅ Testimonials (3 khách hàng)
- ✅ CTA buttons (Đăng ký ngay, Xem khóa học)

**Verify:**
```bash
# Check API response
curl http://localhost:8080/api/v1/tenants/11111111-1111-1111-1111-111111111111/landing
```

---

### **Scenario 2: Course Catalog**

**URL:** `http://localhost:3000/catalog`

**Expected:**
- ✅ 7 published courses hiển thị (DRAFT course không hiện)
- ✅ Search bar hoạt động
- ✅ Filter by level (Beginner/Intermediate/Advanced)
- ✅ Sort by: Newest, Name, Price Low-High
- ✅ Pagination (nếu > 9 courses)
- ✅ Course cards hiển thị: name, description, price, level

**Test cases:**
```
1. Search "IELTS" → Should show 1 result
2. Filter "Beginner" → Should show 3 courses (ENG-101, KID-ENG, MATH101)
3. Sort "Price High-Low" → IELTS first (8M), Physics second (4M)
```

**Verify API:**
```bash
curl "http://localhost:8080/api/v1/courses?status=PUBLISHED&page=0&size=9"
```

---

### **Scenario 3: Course Detail (WITH LMS PREVIEW)**

**URL:** `http://localhost:3000/catalog/5`

**Expected:**
- ✅ Course header: "Tiếng Anh Giao Tiếp Cơ Bản"
- ✅ Breadcrumb: Trang chủ / Khóa học / Course name
- ✅ Course info cards:
  - Duration: 12 tuần
  - Max students: 20
  - Level: Cơ bản
- ✅ Learning objectives (4 bullet points với checkmarks)
- ✅ **LMS Modules Preview:**
  - Module 1: Giới thiệu bản thân (3 lessons)
    - Lesson 1: Greetings (badge "Học thử")
    - Lesson 2: Self Introduction (badge "Học thử")
    - Lesson 3: Talking about hobbies (🔒 locked)
  - Module 2: Tình huống hàng ngày (3 lessons, all locked)
  - Module 3: Ngữ pháp cơ bản (3 lessons, all locked)
- ✅ Sidebar:
  - Price: 3,000,000 VNĐ
  - CTA: "Đăng ký ngay" button
  - "Liên hệ tư vấn" button
  - Benefits list (6 items với checkmarks)

**Verify APIs:**
```bash
# Course details
curl http://localhost:8080/api/v1/courses/5

# LMS structure
curl http://localhost:8080/api/v1/lms/courses/5/modules
```

**Test other courses:**
```
/catalog/6 → Business English (no LMS preview - should show fallback)
/catalog/7 → IELTS (no LMS preview)
/catalog/999 → Should show 404 page
```

---

### **Scenario 4: Contact Form**

**URL:** `http://localhost:3000/contact`

**Expected:**
- ✅ Contact form với validation
- ✅ Required fields: name, email, message
- ✅ Optional field: phone
- ✅ Submit button
- ✅ Contact info cards (Email, Hotline, Địa chỉ)
- ✅ Form validation errors hiển thị đúng
- ✅ Success state sau khi submit

**Test cases:**
```
1. Submit empty form → Show validation errors
2. Submit invalid email → Show "Email không hợp lệ"
3. Submit valid data → Show success message with checkmark
```

**Verify API:**
```bash
curl -X POST http://localhost:8080/api/v1/marketing/contact \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 11111111-1111-1111-1111-111111111111" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "phone": "0901234567",
    "message": "Tôi muốn tìm hiểu về khóa học IELTS"
  }'
```

---

### **Scenario 5: About Page**

**URL:** `http://localhost:3000/about`

**Expected:**
- ✅ Hero section với badge "Về chúng tôi"
- ✅ Mission & Vision cards (2 cards với icons)
- ✅ Statistics (4 cards):
  - 100+ Trung tâm
  - 10,000+ Học viên
  - 500+ Giáo viên
  - 1,000+ Khóa học
- ✅ Core Values (3 cards):
  - Đáng tin cậy (Shield icon)
  - Hiệu quả (Zap icon)
  - Dễ sử dụng (Globe icon)
- ✅ Key Features (4 cards):
  - Quản lý toàn diện
  - Báo cáo & Phân tích
  - Multi-tenant SaaS
  - LMS & E-Learning
- ✅ Development Timeline (4 milestones)
- ✅ CTA section với gradient background

---

## 🐛 Common Issues & Solutions

### **Issue 1: "Cannot fetch landing page"**

**Cause:** Backend not running hoặc database chưa migrate

**Solution:**
```bash
# Check backend status
docker-compose -f docker-compose.dev.yml ps

# Restart backend
docker-compose -f docker-compose.dev.yml restart kiteclass-core

# Check logs
docker-compose -f docker-compose.dev.yml logs -f kiteclass-core
```

### **Issue 2: "No courses found"**

**Cause:** Seed data chưa load hoặc courses status = DRAFT

**Solution:**
```bash
# Check database migrations
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Check courses
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass -c "SELECT id, code, name, status FROM courses;"
```

### **Issue 3: "Course modules not loading"**

**Cause:** LMS API endpoint missing hoặc migration V20 chưa run

**Solution:**
```bash
# Check if V20 migration ran
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass -c "SELECT version, description, installed_on FROM flyway_schema_history WHERE version = '20';"

# If not found, restart backend to trigger migration
docker-compose -f docker-compose.dev.yml restart kiteclass-core
```

### **Issue 4: CORS errors**

**Cause:** Frontend domain không match backend CORS config

**Solution:**
- Verify `NEXT_PUBLIC_API_URL=http://localhost:8080` trong frontend `.env.local`
- Backend auto-allows `localhost:3000` in dev mode

---

## 📊 Database Verification

```bash
# Connect to PostgreSQL
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass

# Verify seed data
SELECT * FROM landing_pages WHERE instance_id = '11111111-1111-1111-1111-111111111111';
SELECT id, code, name, status, price, level FROM courses WHERE status = 'PUBLISHED';
SELECT id, title FROM lms_modules WHERE course_id = 5;
SELECT id, title, is_trial FROM lms_lessons WHERE module_id = 1;
```

---

## 🎯 Best Practices Summary

**✅ Use Seed Data (not mocks) for:**
- Integration testing
- End-to-end testing
- Local development
- Visual testing
- Bug reproduction

**❌ Use Mocks only for:**
- Component unit tests (React Testing Library)
- Storybook components
- When backend unavailable (rare)

**Why Seed Data is Better:**
- Tests real API contracts
- Catches integration bugs early
- Production-like environment
- One source of truth for team
- No mock-reality mismatch

---

## 📁 Seed Data Files

```
kiteclass/kiteclass-core/src/main/resources/db/migration/
├── V16__seed_test_data.sql          # Base data (teachers, students, classes)
├── V19__seed_default_landing_page.sql # Landing page branding
└── V20__seed_landing_page_courses.sql # Enhanced courses + LMS (NEW)
```

---

## 🔄 Clean Database & Reseed

If you need to reset database:

```bash
# Stop services
docker-compose -f docker-compose.dev.yml down

# Remove volumes (deletes database)
docker volume rm 2026-kite-class-platform_postgres-data

# Start fresh (migrations will auto-run)
docker-compose -f docker-compose.dev.yml up -d

# Wait 60 seconds for migrations to complete
docker-compose -f docker-compose.dev.yml logs -f kiteclass-core
```

---

**Happy Testing!** 🎉

Nếu gặp vấn đề, check logs:
- Backend: `docker-compose -f docker-compose.dev.yml logs -f kiteclass-core`
- Frontend: Terminal running `pnpm dev`
- Database: `docker-compose -f docker-compose.dev.yml logs -f postgres`
