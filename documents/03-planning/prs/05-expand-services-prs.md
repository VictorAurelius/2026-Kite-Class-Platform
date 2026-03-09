# Expand Services - PR Implementation List

**Service:** Expand Services (Optional add-ons)
**Architecture Version:** V4.1+
**Effective Date:** 2026-03-09 (planning phase)
**Repository:** `expand/` (new microservices)
**Total PRs:** 13 PRs (6 Parent + 4 Gamification + 3 Forum)
**Timeline:** 6-8 weeks
**Status:** 📋 **DEFERRED** to Phase 2 (post-KiteHub launch)

---

## Overview

Expand Services are **optional add-on features** that KiteClass instances can purchase separately. These are NOT core platform features, but value-added services for competitive differentiation.

**Business Model:**
- Core Platform: ₫500k/month (includes Gateway + Core + Frontend)
- Parent Service: +₫100k/month (optional)
- Gamification Service: +₫150k/month (optional)
- Forum Service: +₫100k/month (optional)

**Why Separate Services:**
- **Modularity:** Customers pay only for features they need
- **Scalability:** Heavy features (gamification, forum) don't affect core performance
- **Development Flexibility:** Can iterate on add-ons without touching core platform

---

## PHASE 1: PARENT SERVICE (6 PRs)

**Purpose:** Allow parents to track their children's progress, attendance, and grades

**Architecture:**
```
┌─────────────────────────────────────┐
│  Expand - Parent Service            │
├─────────────────────────────────────┤
│                                     │
│  - OTP Login (magic link)           │
│  - View children's progress         │
│  - View attendance reports          │
│  - Receive notifications            │
│  - View invoices/payments           │
│                                     │
└─────────────────────────────────────┘
         │
         ▼ Read-only API calls
┌─────────────────────────────────────┐
│  KiteClass Core Service             │
│  - Students API                     │
│  - Attendance API                   │
│  - Grades API                       │
└─────────────────────────────────────┘
```

---

### ⏳ EXP-1: Parent Service Setup

**Status:** Planned (Phase 2)
**Duration:** 3 days
**Dependencies:** None

**Scope:**
- Spring Boot 3.5.10 project setup
- PostgreSQL schema (parents, parent_student_links, otp_tokens)
- Redis for OTP storage
- RabbitMQ integration
- Health checks + actuator

**Database Schema:**
```sql
CREATE TABLE parents (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY,
    parent_id UUID REFERENCES parents(id),
    student_id UUID NOT NULL,  -- From Core Service
    relationship VARCHAR(50),   -- MOTHER, FATHER, GUARDIAN
    instance_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(parent_id, student_id, instance_id)
);

CREATE TABLE otp_tokens (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Endpoints:**
- POST /api/v1/parents - Create parent account
- POST /api/v1/parents/link-student - Link parent to student
- GET /api/v1/parents/{id}/students - Get linked students

---

### ⏳ EXP-2: OTP Authentication Module

**Status:** Planned (Phase 2)
**Duration:** 2 days
**Dependencies:** EXP-1

**Scope:**
- Generate 6-digit OTP code
- Send OTP via SMS (Twilio) or Email (AWS SES)
- Verify OTP and issue JWT token
- Rate limiting (prevent OTP spam)

**Implementation:**
```java
@Service
public class OtpService {

    public String generateOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Store in Redis (5-minute expiry)
        redisTemplate.opsForValue().set(
            "otp:" + email,
            otp,
            Duration.ofMinutes(5)
        );

        // Send via Email or SMS
        emailService.sendOtp(email, otp);

        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        String storedOtp = redisTemplate.opsForValue().get("otp:" + email);
        return otp.equals(storedOtp);
    }
}
```

**Endpoints:**
- POST /api/v1/auth/send-otp - Send OTP to email/phone
- POST /api/v1/auth/verify-otp - Verify OTP and login

---

### ⏳ EXP-3: Parent Portal Backend APIs

**Status:** Planned (Phase 2)
**Duration:** 4 days
**Dependencies:** EXP-2

**Scope:**
- Get children's progress (grades, assignments)
- View attendance history
- View upcoming classes
- View invoices/payments
- Subscribe to notifications

**Endpoints:**
```java
// Progress
GET /api/v1/parents/{parentId}/children/{studentId}/progress
GET /api/v1/parents/{parentId}/children/{studentId}/grades
GET /api/v1/parents/{parentId}/children/{studentId}/assignments

// Attendance
GET /api/v1/parents/{parentId}/children/{studentId}/attendance
GET /api/v1/parents/{parentId}/children/{studentId}/attendance/summary

// Schedule
GET /api/v1/parents/{parentId}/children/{studentId}/schedule
GET /api/v1/parents/{parentId}/children/{studentId}/upcoming-classes

// Billing
GET /api/v1/parents/{parentId}/invoices
GET /api/v1/parents/{parentId}/payments
```

**Implementation:**
```java
@RestController
@RequestMapping("/api/v1/parents")
public class ParentPortalController {

    @Autowired
    private CoreServiceClient coreServiceClient; // Feign client

    @GetMapping("/{parentId}/children/{studentId}/progress")
    public ResponseEntity<ProgressResponse> getChildProgress(
        @PathVariable UUID parentId,
        @PathVariable UUID studentId
    ) {
        // Verify parent has access to this student
        verifyParentStudentLink(parentId, studentId);

        // Call Core Service API
        ProgressResponse progress = coreServiceClient.getStudentProgress(studentId);

        return ResponseEntity.ok(progress);
    }
}
```

---

### ⏳ EXP-4: Parent Portal Frontend (moved from PR 3.13)

**Status:** Planned (Phase 2)
**Duration:** 1 week
**Dependencies:** EXP-3

**Scope:**
- Next.js pages for parent dashboard
- OTP login flow
- View children's progress cards
- Attendance calendar
- Notifications center

**Routes:**
```
/parent/login          - OTP login page
/parent/dashboard      - Overview of all children
/parent/child/[id]     - Specific child details
/parent/child/[id]/progress
/parent/child/[id]/attendance
/parent/child/[id]/schedule
/parent/invoices       - Billing history
```

---

### ⏳ EXP-5: Parent Notification System

**Status:** Planned (Phase 2)
**Duration:** 3 days
**Dependencies:** EXP-3

**Scope:**
- Subscribe to events (low grade, missed class, invoice due)
- Send notifications via Email + SMS + Push
- RabbitMQ consumer for events from Core Service

**Events:**
```java
// Core Service publishes events
@Service
public class AttendanceService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void markAbsent(UUID studentId) {
        // Mark attendance...

        // Publish event
        rabbitTemplate.convertAndSend(
            "student.events",
            "student.absent",
            new StudentAbsentEvent(studentId, LocalDate.now())
        );
    }
}

// Parent Service consumes events
@RabbitListener(queues = "parent.notifications.queue")
public void handleStudentAbsent(StudentAbsentEvent event) {
    // Find parents of this student
    List<Parent> parents = parentRepository.findByStudentId(event.getStudentId());

    // Send notification
    for (Parent parent : parents) {
        notificationService.send(
            parent.getEmail(),
            "Your child was absent today",
            "Subject: Attendance Alert\n\n" +
            "Your child was marked absent on " + event.getDate()
        );
    }
}
```

---

### ⏳ EXP-6: Parent Service Integration Tests

**Status:** Planned (Phase 2)
**Duration:** 2 days
**Dependencies:** EXP-1 to EXP-5

**Scope:**
- OTP flow end-to-end test
- Parent-student linking test
- Progress API integration test
- Notification delivery test
- Multi-tenant isolation test

---

## PHASE 2: GAMIFICATION SERVICE (4 PRs)

**Purpose:** Increase student engagement with points, badges, leaderboards

**Architecture:**
```
┌─────────────────────────────────────┐
│  Expand - Gamification Service      │
├─────────────────────────────────────┤
│                                     │
│  - Points Engine                    │
│  - Badges/Achievements              │
│  - Leaderboards                     │
│  - Challenges/Quests                │
│  - Rewards Marketplace              │
│                                     │
└─────────────────────────────────────┘
         │
         ▼ Listen to student events
┌─────────────────────────────────────┐
│  KiteClass Core Service             │
│  - Assignment submitted             │
│  - Quiz completed                   │
│  - Attendance marked                │
└─────────────────────────────────────┘
```

---

### ⏳ EXP-7: Gamification Engine

**Status:** Planned (Phase 2)
**Duration:** 5 days
**Dependencies:** None

**Scope:**
- Points system (earn points for actions)
- Levels (Bronze, Silver, Gold based on points)
- Badges (achievements for milestones)
- Event-driven architecture (RabbitMQ)

**Database Schema:**
```sql
CREATE TABLE student_points (
    student_id UUID PRIMARY KEY,
    total_points INT DEFAULT 0,
    level VARCHAR(50) DEFAULT 'BRONZE',
    instance_id UUID NOT NULL
);

CREATE TABLE point_transactions (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    points INT NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE badges (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url TEXT,
    points_required INT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE student_badges (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    badge_id UUID REFERENCES badges(id),
    earned_at TIMESTAMP DEFAULT NOW()
);
```

**Points Rules:**
```java
@Service
public class PointsEngine {

    public void awardPoints(UUID studentId, PointsReason reason) {
        int points = switch (reason) {
            case ASSIGNMENT_SUBMITTED -> 10;
            case QUIZ_PERFECT_SCORE -> 50;
            case ATTENDANCE_STREAK_7_DAYS -> 100;
            case FIRST_ASSIGNMENT -> 5;
        };

        // Add points
        studentPointsRepository.addPoints(studentId, points);

        // Check level up
        checkLevelUp(studentId);

        // Check badge unlock
        checkBadgeUnlock(studentId);
    }
}
```

---

### ⏳ EXP-8: Leaderboard System

**Status:** Planned (Phase 2)
**Duration:** 3 days
**Dependencies:** EXP-7

**Scope:**
- Class leaderboard (top 10 students in class)
- School-wide leaderboard
- Weekly/Monthly leaderboards
- Redis sorted sets for performance

**Implementation:**
```java
@Service
public class LeaderboardService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void updateLeaderboard(UUID classId, UUID studentId, int points) {
        String key = "leaderboard:class:" + classId;
        redisTemplate.opsForZSet().add(key, studentId.toString(), points);
    }

    public List<LeaderboardEntry> getTopStudents(UUID classId, int limit) {
        String key = "leaderboard:class:" + classId;
        Set<ZSetOperations.TypedTuple<String>> entries = redisTemplate
            .opsForZSet()
            .reverseRangeWithScores(key, 0, limit - 1);

        return entries.stream()
            .map(e -> new LeaderboardEntry(
                UUID.fromString(e.getValue()),
                e.getScore().intValue()
            ))
            .collect(Collectors.toList());
    }
}
```

---

### ⏳ EXP-9: Challenges & Quests

**Status:** Planned (Phase 2)
**Duration:** 4 days
**Dependencies:** EXP-7

**Scope:**
- Time-limited challenges (e.g., "Submit 5 assignments this week")
- Quest chains (multi-step achievements)
- Rewards (bonus points, exclusive badges)

---

### ⏳ EXP-10: Gamification Frontend

**Status:** Planned (Phase 2)
**Duration:** 1 week
**Dependencies:** EXP-7, EXP-8, EXP-9

**Scope:**
- Student profile with points/level/badges
- Leaderboard page
- Challenges dashboard
- Rewards marketplace

---

## PHASE 3: FORUM SERVICE (3 PRs)

**Purpose:** Q&A forum for students to help each other (like Stack Overflow for classes)

---

### ⏳ EXP-11: Q&A Forum Module

**Status:** Planned (Phase 2)
**Duration:** 5 days
**Dependencies:** None

**Scope:**
- Post questions
- Answer questions
- Upvote/downvote
- Mark accepted answer
- Search questions

**Database Schema:**
```sql
CREATE TABLE forum_questions (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    author_id UUID NOT NULL,
    course_id UUID REFERENCES courses(id),
    views INT DEFAULT 0,
    upvotes INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'OPEN',  -- OPEN, ANSWERED, CLOSED
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE forum_answers (
    id UUID PRIMARY KEY,
    question_id UUID REFERENCES forum_questions(id),
    body TEXT NOT NULL,
    author_id UUID NOT NULL,
    upvotes INT DEFAULT 0,
    is_accepted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE forum_votes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    target_type VARCHAR(50),  -- QUESTION, ANSWER
    target_id UUID NOT NULL,
    vote_type VARCHAR(10),    -- UP, DOWN
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, target_type, target_id)
);
```

---

### ⏳ EXP-12: Forum Moderation

**Status:** Planned (Phase 2)
**Duration:** 3 days
**Dependencies:** EXP-11

**Scope:**
- Flag inappropriate content
- Moderator queue
- Ban users
- Edit/delete posts

---

### ⏳ EXP-13: Forum Frontend

**Status:** Planned (Phase 2)
**Duration:** 1 week
**Dependencies:** EXP-11, EXP-12

**Scope:**
- Question list page
- Question detail page
- Ask question form
- Answer form
- Moderation dashboard

---

## Summary

**Total PRs:** 13
- Parent Service: 6 PRs (3 weeks)
- Gamification Service: 4 PRs (3 weeks)
- Forum Service: 3 PRs (2 weeks)

**Total Timeline:** 6-8 weeks (with some parallelization)

**Priority:** LOW (defer to Phase 2 after KiteHub launch)

**Revenue Potential:**
- 100 customers × 50% opt-in × ₫350k/month (avg 2 add-ons) = **₫1.75M/month additional revenue**

---

## Next Steps

1. **Complete KiteHub (Tier 1 priority)**
   - Subscription Service (PR 4.1)
   - Database Provisioning (PR 4.2)
   - Payment Service (PR 4.5)
   - AI Branding (PR 4.8-4.10)

2. **Validate market demand**
   - Survey existing customers
   - Identify which add-on has highest demand
   - Start with highest-demand feature first

3. **Implement Phase 1 (Parent Service)**
   - Most requested feature from parents
   - Easier to build than gamification/forum
   - Clear ROI (reduces teacher communication burden)

4. **Launch & iterate**
   - Beta test with 5-10 schools
   - Gather feedback
   - Improve before wide rollout

---

## Related Documentation

- [KiteHub PRs](./04-kitehub-prs.md)
- [Core Service PRs](./02-core-prs.md)
- [Frontend PRs](./03-frontend-prs.md)
- [Master PR Index](./00-master-pr-index.md)

---

**Last Updated:** 2026-03-09
**Status:** Planning phase, deferred to Phase 2
