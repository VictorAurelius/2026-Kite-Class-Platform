# THIẾT KẾ TỔNG THỂ: LEADS & TRIAL LEARNING SYSTEM

**Version**: V4.1
**Last Updated**: 2026-02-26
**Status**: Architecture Design Document
**Author**: KiteClass Platform Team

---

## 📋 MỤC LỤC

1. [Tổng quan](#1-tổng-quan)
2. [Lead vs Student - Database Design](#2-lead-vs-student---database-design)
3. [Trial Learning System](#3-trial-learning-system)
4. [Trial Learning Business Logic](#4-trial-learning-business-logic)
5. [Lead → Student Conversion](#5-lead--student-conversion)
6. [Implementation Roadmap](#6-implementation-roadmap)

---

## 1. TỔNG QUAN

### 1.1. Mục tiêu

Thiết kế hệ thống cho phép:
- **Guest users** xem trial lessons (anonymous)
- **Leads** đăng ký trial account → Track progress
- **Leads** học thử (self-paced) với giới hạn
- **Leads** liên hệ teacher để đăng ký
- **Leads** convert thành Students sau thanh toán

### 1.2. Key Principles

1. ✅ **Separate Domains**: Lead (Marketing) ≠ Student (Academic)
2. ✅ **Progress Continuity**: Trial progress → Student progress (seamless)
3. ✅ **Analytics Intact**: Track conversion funnel
4. ✅ **Scalable**: Self-paced trial (no class capacity limit)
5. ✅ **Flexible**: Teacher controls trial content

---

## 2. LEAD VS STUDENT - DATABASE DESIGN

### 2.1. Decision: Separate Tables ✅

**Approach**: Leads và Students là 2 tables riêng biệt

```
Lead (Marketing Domain)          Student (Academic Domain)
- id                              - id
- email                           - email
- name                            - full_name
- phone                           - phone
- source (TRIAL, LANDING_PAGE)    - student_code
- status (NEW, CONVERTED, LOST)   - date_of_birth
- course_interest_id              - address
- user_id (FK to Gateway)         - user_id (FK to Gateway)
- converted_to_student_id         - emergency_contact
```

### 2.2. Why NOT Single Table?

❌ **Rejected Approach**: Merge vào 1 table `contacts` với `contact_type` field

**Lý do reject**:
- ❌ Too many NULL values (lead-specific + student-specific fields)
- ❌ Performance issues (filter `WHERE contact_type = 'STUDENT'` mọi query)
- ❌ Schema evolution hell (add student field → affect leads table)
- ❌ Different lifecycles (leads: 3-6 months, students: 5-10 years)
- ❌ Security risks (marketing team có thể leak student data)

### 2.3. Benefits of Separate Tables

1. ✅ **Clear Separation**: Marketing vs Academic domains
2. ✅ **Different Lifecycles**: Auto-archive leads sau 90 ngày
3. ✅ **Better Performance**: No type filtering, smaller indexes
4. ✅ **Minimal NULLs**: Clean schema
5. ✅ **Independent Evolution**: Add marketing fields ≠ affect students
6. ✅ **Better Security**: Table-level permissions

---

## 3. TRIAL LEARNING SYSTEM

### 3.1. Authentication & Authorization

#### Problem: Leads Need User Accounts

**Gap Analysis**:
```
Current V4.1:
- Guest: Anonymous access (no auth)
- lesson_progress.user_id: NOT NULL
→ Leads không track progress được!

Expand Services (future):
- Video Streaming Service cần JWT
- Quiz Service cần JWT
- Live Class Service cần JWT
→ Leads không access được!
```

#### Solution: TRIAL_USER Role ✅

**Schema Changes**:
```sql
-- Migration V12: Add Trial User Support

-- 1. Gateway: Add TRIAL_USER role
ALTER TYPE user_role ADD VALUE 'TRIAL_USER';

-- 2. Core: Add user_id FK to leads table
ALTER TABLE leads
ADD COLUMN user_id BIGINT REFERENCES users(id);

CREATE INDEX idx_leads_user_id ON leads(user_id)
WHERE user_id IS NOT NULL;

-- 3. Trial quotas (enforce limits)
CREATE TABLE trial_quotas (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    quota_date DATE NOT NULL,
    lessons_viewed INTEGER DEFAULT 0,
    videos_streamed INTEGER DEFAULT 0,
    quizzes_attempted INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_trial_quotas UNIQUE (user_id, quota_date)
);
```

### 3.2. Trial Registration Workflow

```java
@Service
public class TrialRegistrationService {

    @Transactional
    public TrialRegistrationResponse registerTrial(RegisterTrialRequest request) {
        UUID instanceId = TenantContext.getCurrentTenantId();

        // 1. Check duplicates
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("EMAIL_ALREADY_STUDENT");
        }

        // 2. Create Gateway User with role TRIAL_USER
        User trialUser = gatewayClient.createUser(CreateUserRequest.builder()
            .email(request.getEmail())
            .name(request.getName())
            .role(UserRole.TRIAL_USER)  // ⭐ Trial role
            .instanceId(instanceId)
            .password(generateRandomPassword()) // Auto-generate
            .build());

        // 3. Create Lead
        Lead lead = Lead.builder()
            .instanceId(instanceId)
            .email(request.getEmail())
            .name(request.getName())
            .phone(request.getPhone())
            .source(LeadSource.TRIAL)
            .status(LeadStatus.NEW)
            .courseInterestId(request.getCourseId())
            .userId(trialUser.getId()) // ⭐ Link to Gateway user
            .build();

        lead = leadRepository.save(lead);

        // 4. Issue JWT token
        String jwtToken = jwtService.generateToken(trialUser);

        // 5. Send magic link email (passwordless login)
        emailService.sendTrialWelcomeEmail(
            request.getEmail(),
            generateMagicLink(jwtToken)
        );

        return TrialRegistrationResponse.builder()
            .leadId(lead.getId())
            .userId(trialUser.getId())
            .accessToken(jwtToken)
            .message("Trial access granted. Check your email for login link.")
            .build();
    }
}
```

### 3.3. Permission Matrix

| Feature | GUEST | TRIAL_USER | STUDENT | TEACHER | ADMIN |
|---------|-------|------------|---------|---------|-------|
| Xem trial lessons | ✅ | ✅ | ✅ | ✅ | ✅ |
| Xem paid lessons | ❌ | ❌ | ✅ (enrolled) | ✅ | ✅ |
| Track progress | ❌ | ✅ | ✅ | ✅ | ✅ |
| Stream video | ❌ | ✅ (trial, 480p, 2h expiry) | ✅ (1080p, 24h) | ✅ | ✅ |
| Download video | ❌ | ❌ | ✅ | ✅ | ✅ |
| Làm quiz | ❌ | ✅ (1 attempt) | ✅ (unlimited) | ✅ | ✅ |
| Join live class | ❌ | ✅ (trial sessions) | ✅ (all) | ✅ | ✅ |
| Daily limit | - | 3 lessons/day | Unlimited | Unlimited | Unlimited |

### 3.4. Authorization Implementation

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/trial/register").permitAll()

                // Trial lessons - TRIAL_USER or higher
                .requestMatchers(HttpMethod.GET, "/api/v1/lessons/{id}")
                    .hasAnyRole("TRIAL_USER", "STUDENT", "TEACHER", "ADMIN")

                // Progress tracking - TRIAL_USER or higher
                .requestMatchers("/api/v1/lessons/{id}/progress")
                    .hasAnyRole("TRIAL_USER", "STUDENT", "TEACHER", "ADMIN")

                // Paid content - STUDENT or higher (exclude TRIAL_USER)
                .requestMatchers("/api/v1/courses/{id}/full-content")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                .anyRequest().authenticated()
            )
            .build();
    }
}
```

---

## 4. TRIAL LEARNING BUSINESS LOGIC

### 4.1. Course Structure: Trial Lessons

#### Decision: Course chính với `is_trial` flag ✅

**Schema** (already in V4.1):
```sql
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    is_trial BOOLEAN DEFAULT FALSE NOT NULL,  -- ⭐ Trial flag
    content TEXT,
    video_url VARCHAR(500),
    order_number INTEGER NOT NULL,
    ...
);
```

**Example**:
```
Course: "IELTS Foundation"
  │
  ├─ Module 1: Introduction
  │   ├─ Lesson 1.1: "What is IELTS?" (isTrial: TRUE) ⭐ FREE
  │   ├─ Lesson 1.2: "Test Format" (isTrial: TRUE) ⭐ FREE
  │   └─ Lesson 1.3: "Study Strategies" (isTrial: FALSE) 🔒 PAID
  │
  └─ Module 2-6: Mixed (some trial, mostly paid)
```

**Business Rules**:
- BR-TRIAL-001: Mỗi course có 2-3 trial lessons (10-20% content)
- BR-TRIAL-002: Trial lessons thường ở đầu (Module 1, 2)
- BR-TRIAL-003: Teacher quyết định lesson nào là trial
- BR-TRIAL-004: Trial lessons accessible by Guest, TRIAL_USER, STUDENT

**Why NOT separate trial course?**
- ❌ Content duplication (lessons exist 2 times)
- ❌ Progress loss (lead học trial course → convert → học lại từ đầu)
- ❌ Maintenance hell (update content → sync 2 courses)

### 4.2. Class Enrollment: Self-Paced

#### Decision: Phase 1 - NO class enrollment ✅

**Approach**: Leads học self-paced (on-demand)

```
Lead → Access trial lessons bất kỳ lúc nào
     ↓
     NO class enrollment
     NO teacher interaction
     NO schedule

→ Pure self-study
```

**Why NO class?**
- ✅ Simple (no trial class management)
- ✅ Scalable (unlimited leads)
- ✅ Low barrier (instant access)
- ✅ Self-paced (học theo tốc độ riêng)
- ✅ Zero cost (no teacher time)

**Phase 2 (optional)**: Add trial events

```sql
-- Optional: Trial live events
CREATE TABLE trial_events (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    teacher_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    title VARCHAR(255), -- "Free IELTS Speaking Workshop"
    event_type VARCHAR(50), -- WEBINAR, Q&A, WORKSHOP
    scheduled_at TIMESTAMP NOT NULL,
    max_attendees INTEGER DEFAULT 50,
    meeting_url VARCHAR(500) -- Zoom link
);
```

### 4.3. UI/UX for Leads

#### Course Detail Page (Lead View)

```
┌─────────────────────────────────────────────────────┐
│ 📚 IELTS Foundation                  by Ms. Lan    │
├─────────────────────────────────────────────────────┤
│ 📖 NỘI DUNG KHÓA HỌC                                │
│                                                      │
│ ✅ Module 1: Introduction (3 lessons)               │
│    ├─ ▶️ Lesson 1.1: What is IELTS? (10:30) 🆓  │ ← Play
│    ├─ ▶️ Lesson 1.2: Test Format (15:45) 🆓     │ ← Play
│    └─ 🔒 Lesson 1.3: Study Strategies (20:00)    │ ← Locked
│                                                      │
│ ✅ Module 2: Listening (5 lessons)                  │
│    ├─ ▶️ Lesson 2.1: Preview (12:00) 🆓         │
│    └─ 🔒 4 lessons locked                         │
│                                                      │
│ 🔒 Module 3-6: Advanced (20 lessons) - Premium     │
│                                                      │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│ 💡 Bạn đang xem 3/28 lessons                        │
│ 👉 [Đăng ký học - Unlock 25 lessons - ₫2,500,000] │
└─────────────────────────────────────────────────────┘
```

**Show**:
- ✅ Trial lessons: Play button + duration + 🆓 badge
- ✅ Locked lessons: 🔒 icon + blur + "Unlock to view"
- ✅ Progress bar (if trial user logged in)
- ✅ Teacher bio, reviews
- ✅ Strong CTA

**Hide/Lock**:
- ❌ Paid lesson videos
- ❌ Premium resources
- ❌ Student comments
- ❌ Download buttons

**Throttling**:
- ⚠️ Video watermark: "KiteClass Trial"
- ⚠️ 3 lessons/day limit
- ⚠️ 480p quality (students: 1080p)
- ⚠️ 1x playback speed (students: 0.5x-2x)

### 4.4. Lead Contact Teacher

#### Decision: Phase 1 - Contact Form + Public Info ✅

**Schema** (reuse Marketing module):
```sql
-- contact_messages table (already exists V4.1)
ALTER TABLE contact_messages
ADD COLUMN teacher_id BIGINT REFERENCES teachers(id),
ADD COLUMN course_id BIGINT REFERENCES courses(id);
```

**UI**:
```
┌──────────────────────────────────────────────┐
│ 💬 LIÊN HỆ GIẢNG VIÊN                        │
├──────────────────────────────────────────────┤
│ 👩‍🏫 Ms. Nguyễn Thị Lan                     │
│ 📧 lan.nguyen@example.com                   │
│ 📱 +84 901 234 567                          │
│ 💬 Zalo: 0901234567                         │
│ 🕐 Available: Mon-Fri 9AM-6PM               │
│                                              │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│ Hoặc gửi tin nhắn:                          │
│ Họ tên: [                              ]   │
│ Email:  [                              ]   │
│ Tin nhắn: [                            ]   │
│ [Gửi tin nhắn]                              │
└──────────────────────────────────────────────┘
```

**Workflow**:
1. Lead fill form
2. System email teacher
3. Teacher reply via email/Zalo/phone
4. System track contact sent

**Phase 2 (optional)**: In-app messaging
- Real-time chat (WebSocket)
- When: >500 leads/month hoặc conversion rate cần improve
- Alternative: Integrate 3rd-party (Intercom, Tawk.to)

---

## 5. LEAD → STUDENT CONVERSION

### 5.1. Conversion Approach

#### Decision: UPDATE ROLE + KEEP LEAD RECORD ✅

**Workflow**:
```
Lead (before):
- Gateway: User (role: TRIAL_USER)
- Core: Lead record (user_id FK)

Convert ↓

Student (after):
- Gateway: SAME User (role: STUDENT) ← Update role only
- Core: Student record (user_id FK, copy từ Lead)
- Core: Lead record (status: CONVERTED, keep for analytics)
```

**Why NOT delete Lead?**
- ❌ Lose analytics (conversion funnel, lead source, trial behavior)
- ❌ Cannot rollback
- ❌ GDPR issues (hard to separate marketing vs academic data)

### 5.2. Conversion Service

```java
@Service
public class LeadConversionService {

    @Transactional
    public StudentConversionResponse convertLeadToStudent(
        Long leadId,
        StudentRegistrationRequest request
    ) {
        // 1. Validate
        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new EntityNotFoundException("LEAD_NOT_FOUND"));

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new ValidationException("LEAD_ALREADY_CONVERTED");
        }

        // 2. Create Student (copy from Lead + additional info)
        Student student = Student.builder()
            .instanceId(lead.getInstanceId())
            .userId(lead.getUserId()) // ⭐ SAME user_id
            .email(lead.getEmail())
            .fullName(lead.getName())
            .phone(lead.getPhone())
            .studentCode(generateStudentCode())
            // Additional from registration form
            .dateOfBirth(request.getDateOfBirth())
            .address(request.getAddress())
            .emergencyContact(request.getEmergencyContact())
            .build();

        student = studentRepository.save(student);

        // 3. Update User role: TRIAL_USER → STUDENT
        gatewayClient.updateUserRole(lead.getUserId(), UserRole.STUDENT);

        // 4. Update Lead status (keep for analytics)
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedAt(Instant.now());
        lead.setConvertedToStudentId(student.getId());
        leadRepository.save(lead);

        // 5. Create enrollment
        if (request.getClassId() != null) {
            Enrollment enrollment = Enrollment.builder()
                .classId(request.getClassId())
                .studentId(student.getId())
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build();
            enrollmentRepository.save(enrollment);
        }

        // 6. Progress migration: KHÔNG CẦN!
        // lesson_progress.user_id unchanged → progress preserved

        // 7. Notifications
        emailService.sendStudentWelcomeEmail(student);
        notificationService.notifyTeacherNewStudent(student);

        return StudentConversionResponse.builder()
            .studentId(student.getId())
            .message("Chào mừng bạn đã trở thành học viên!")
            .build();
    }
}
```

### 5.3. Progress Continuity

**KEY INSIGHT**: Không cần migrate progress!

```sql
-- lesson_progress links to user_id
-- Before conversion (TRIAL_USER):
SELECT * FROM lesson_progress WHERE user_id = 123;
-- | id | user_id | lesson_id | completed | progress_percent |
-- | 1  | 123     | 10        | TRUE      | 100              |

-- After conversion (STUDENT):
-- SAME records! user_id unchanged
-- | id | user_id | lesson_id | completed | progress_percent |
-- | 1  | 123     | 10        | TRUE      | 100              | ← Unchanged!
```

**Business Logic Update**:
```java
// After conversion: Student can continue from trial progress
if (user.getRole() == UserRole.STUDENT) {
    boolean hasEnrollment = enrollmentRepository.existsActive(
        user.getId(),
        lesson.getCourse().getId()
    );

    if (!hasEnrollment && !lesson.isTrial()) {
        throw new ForbiddenException("ENROLLMENT_REQUIRED");
    }

    // ✅ Can continue from where they left off!
}
```

### 5.4. Payment Integration

```java
@Service
public class PaymentService {

    // Webhook from VNPay/Momo
    @PostMapping("/api/v1/payments/webhook")
    @Transactional
    public void handlePaymentWebhook(@RequestBody PaymentWebhookRequest webhook) {
        Payment payment = paymentRepository.findById(webhook.getPaymentId())...

        if (webhook.getStatus().equals("SUCCESS")) {
            // 1. Update payment
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(Instant.now());
            paymentRepository.save(payment);

            // 2. Auto-convert Lead → Student
            Lead lead = leadRepository.findByUserId(payment.getUserId())...

            StudentConversionResponse conversion = leadConversionService
                .convertLeadToStudent(lead.getId(), studentRequest);

            // 3. Notifications
            emailService.sendEnrollmentConfirmation(conversion.getStudentId());
        }
    }
}
```

### 5.5. Data Mapping

| Lead Field | Student Field | Notes |
|------------|---------------|-------|
| `id` | - | Lead ID giữ nguyên |
| `user_id` | `user_id` | ⭐ SAME |
| `email` | `email` | Copy |
| `name` | `full_name` | Copy |
| `phone` | `phone` | Copy |
| `course_interest_id` | - | Used for enrollment |
| `source` | - | Keep in Lead |
| `status` | - | Update to CONVERTED |
| - | `student_code` | Generate new |
| - | `date_of_birth` | From form |
| - | `address` | From form |

### 5.6. Rollback Strategy

```java
@Service
public class StudentDowngradeService {

    @Transactional
    public void downgradeStudentToTrial(Long studentId, String reason) {
        Student student = studentRepository.findById(studentId)...

        // 1. Check eligible
        if (enrollmentRepository.existsActive(studentId)) {
            throw new ValidationException("CANNOT_DOWNGRADE_WITH_ENROLLMENTS");
        }

        // 2. Soft-delete Student
        student.setDeleted(true);
        studentRepository.save(student);

        // 3. Update User role: STUDENT → TRIAL_USER
        gatewayClient.updateUserRole(student.getUserId(), UserRole.TRIAL_USER);

        // 4. Reactivate Lead
        Lead lead = leadRepository.findByConvertedToStudentId(studentId).get();
        lead.setStatus(LeadStatus.ACTIVE);
        lead.setConvertedAt(null);
        leadRepository.save(lead);

        // Note: lesson_progress giữ nguyên (user_id unchanged)
    }
}
```

---

## 6. IMPLEMENTATION ROADMAP

### Phase 1: MVP (Month 1-3)

**Objective**: Trial learning cơ bản

**Features**:
1. ✅ Trial user registration (TRIAL_USER role)
2. ✅ Self-paced trial lessons (no class)
3. ✅ Progress tracking cho trial users
4. ✅ Trial quotas (3 lessons/day)
5. ✅ Lead UI (trial/locked lessons)
6. ✅ Contact form + public teacher info
7. ✅ Lead → Student conversion
8. ✅ Payment integration (VNPay/Momo webhook)

**Schema Changes**:
```sql
-- Migration V12
ALTER TYPE user_role ADD VALUE 'TRIAL_USER';

ALTER TABLE leads
ADD COLUMN user_id BIGINT REFERENCES users(id);

CREATE TABLE trial_quotas (...);

ALTER TABLE contact_messages
ADD COLUMN teacher_id BIGINT,
ADD COLUMN course_id BIGINT;
```

**Development Effort**: 3-4 weeks
- Week 1: Trial registration + authentication
- Week 2: Trial UI + progress tracking
- Week 3: Conversion workflow + payment
- Week 4: Testing + bug fixes

---

### Phase 2: Growth Features (Month 6-12)

**Objective**: Improve conversion rate

**Features**:
1. ⭐ Optional trial live events (webinars)
2. ⭐ In-app messaging (lead-teacher chat)
3. ⭐ Advanced analytics (lead engagement scoring)
4. ⭐ A/B testing (trial lesson combinations)
5. ⭐ Referral program (lead invites friends)
6. ⭐ Automated email campaigns (nurture leads)

**Triggers to implement**:
- ✅ >500 leads/month
- ✅ Conversion rate <10%
- ✅ Teachers request live interaction
- ✅ Budget available

---

## 7. ANALYTICS & KPIs

### 7.1. Key Metrics

**Conversion Funnel**:
```
Landing Page Views
  ↓ (10-15% signup rate)
Trial Signups (Leads created)
  ↓ (50-60% activation rate)
Trial Activation (view 1+ lesson)
  ↓ (20-30% engagement rate)
Trial Engagement (view 2-3 lessons)
  ↓ (10-15% conversion rate)
Paid Students (converted)
```

**SQL Queries**:
```sql
-- Conversion rate by source
SELECT
    source,
    COUNT(*) as total_leads,
    COUNT(CASE WHEN status = 'CONVERTED' THEN 1 END) as converted,
    COUNT(CASE WHEN status = 'CONVERTED' THEN 1 END) * 100.0 / COUNT(*) as conversion_rate
FROM leads
WHERE created_at >= '2026-01-01'
GROUP BY source;

-- Average time to convert
SELECT
    AVG(EXTRACT(EPOCH FROM (converted_at - created_at)) / 86400) as avg_days
FROM leads
WHERE status = 'CONVERTED';

-- Trial lessons viewed before conversion
SELECT
    l.email,
    COUNT(lp.id) as lessons_viewed,
    l.converted_at - l.created_at as time_to_convert
FROM leads l
JOIN lesson_progress lp ON lp.user_id = l.user_id
WHERE l.status = 'CONVERTED'
GROUP BY l.id;
```

### 7.2. Success Criteria

**Target KPIs** (Phase 1):
- ✅ Trial signup rate: >12%
- ✅ Trial activation rate: >50%
- ✅ Trial engagement rate: >25%
- ✅ Lead → Student conversion: >10%
- ✅ Average time to convert: <7 days

---

## 8. TECHNICAL SPECIFICATIONS

### 8.1. API Endpoints

**Trial Registration**:
```
POST /api/v1/trial/register
Body:
{
  "email": "lead@example.com",
  "name": "Nguyễn Văn A",
  "phone": "0901234567",
  "courseId": 123
}

Response:
{
  "leadId": 456,
  "userId": 789,
  "accessToken": "jwt-token...",
  "message": "Check email for magic link"
}
```

**Conversion**:
```
POST /api/v1/leads/{leadId}/convert
Body:
{
  "dateOfBirth": "2000-01-01",
  "address": "123 Street, City",
  "emergencyContact": "0909999999",
  "classId": 45
}

Response:
{
  "studentId": 999,
  "studentCode": "STU2026001",
  "message": "Welcome!"
}
```

### 8.2. Security Considerations

1. ✅ **Rate Limiting**: 5 trial registrations/IP/day
2. ✅ **Email Verification**: Magic link expires in 24h
3. ✅ **Trial Quotas**: 3 lessons/day, enforced by database
4. ✅ **Payment Security**: Webhook signature verification
5. ✅ **GDPR**: Lead data deletable independently from Student

### 8.3. Performance Optimizations

1. ✅ **Caching**: Trial lesson list (1h TTL)
2. ✅ **CDN**: Trial videos on CDN với signed URLs
3. ✅ **Database Indexes**:
   ```sql
   CREATE INDEX idx_leads_user_id ON leads(user_id);
   CREATE INDEX idx_leads_status ON leads(status);
   CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id);
   ```

---

## 9. APPENDIX

### 9.1. Business Rules Summary

**Lead Management**:
- BR-LEAD-001: Email must be unique per tenant
- BR-LEAD-002: Lead expires after 90 days if not converted
- BR-LEAD-003: Lead can only convert once

**Trial Access**:
- BR-TRIAL-001: Each course has 2-3 trial lessons (10-20%)
- BR-TRIAL-002: Trial users limited to 3 lessons/day
- BR-TRIAL-003: Trial access expires after 7 days
- BR-TRIAL-004: Video quality 480p max for trial

**Conversion**:
- BR-CONV-001: Payment required before conversion
- BR-CONV-002: Progress preserved after conversion
- BR-CONV-003: Lead record kept for analytics

### 9.2. Error Codes

| Code | Message | HTTP Status |
|------|---------|-------------|
| LEAD_NOT_FOUND | Lead not found | 404 |
| LEAD_ALREADY_CONVERTED | Lead already converted | 400 |
| EMAIL_ALREADY_STUDENT | Email exists as student | 400 |
| TRIAL_DAILY_LIMIT_REACHED | Daily trial limit reached | 429 |
| TRIAL_EXPIRED | Trial period expired | 403 |
| PAYMENT_REQUIRED | Payment required | 402 |

### 9.3. References

- [Database Design V4.1](database-design.md)
- [Core Service Implementation](core-service-implementation.md)
- [PR 2.10: Marketing Module](../prs/02-core-prs.md#pr-210-marketing-module)
- [Gateway Implementation](gateway-implementation-plan.md)

---

**Last Updated**: 2026-02-26
**Status**: ✅ Design Complete - Ready for Implementation
