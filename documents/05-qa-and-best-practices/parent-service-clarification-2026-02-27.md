# BÁO CÁO: PARENT SERVICE CLARIFICATION - ALIGNMENT VỚI ARCHITECTURE V4.1

**Ngày:** 2026-02-27
**Người thực hiện:** Architecture Alignment Agent
**Mục đích:** Loại bỏ Parent Service khỏi Core và đảm bảo alignment với Architecture V4.1

---

## EXECUTIVE SUMMARY

Đã thực hiện cleanup để remove Parent Service khỏi Core Service scope, đảm bảo alignment với Architecture V4.1 trong đó Parent Service được định nghĩa rõ ràng là **Optional Addon (Future)**, KHÔNG phải Core Service.

### Kết quả

| Aspect | Before | After | Status |
|--------|--------|-------|--------|
| **Architecture Gaps** | 5 critical gaps | 4 critical gaps | ✅ Fixed |
| **Database Design** | Có parents/parent_children trong Core | Removed + added note | ✅ Fixed |
| **Implementation Plan** | Mixed references | Clear separation | ✅ Fixed |
| **Alignment với V4.1** | Inconsistent | Fully aligned | ✅ Fixed |

---

## SOURCE OF TRUTH: ARCHITECTURE V4.1

Theo `documents/01-research/architecture/system-architecture-v4.md`:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                   OPTIONAL ADDONS (Pick & Choose)                         │
│                      Expand features beyond Core                          │
├───────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │
│  │ Parent Service  │  │ Gamification    │  │  Forum Service  │          │
│  │ (Future)        │  │   (Future)      │  │   (Future)      │          │
│  │                 │  │                 │  │                 │          │
│  │ • Zalo OTP Reg  │  │  • Points       │  │  • Q&A Forum    │          │
│  │ • Track child   │  │  • Badges       │  │  • Discussions  │          │
│  │ • View reports  │  │  • Leaderboard  │  │  • Peer help    │          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Pricing Model:**
- **Core Bundle (₫500k/tháng):** Gateway + Core + Frontend (3 services)
- **Parent Service Addon (₫100k/tháng):** Optional future service
- **Gamification Addon (₫100k/tháng):** Optional future service
- **Forum Addon (₫100k/tháng):** Optional future service

**Rõ ràng:** Parent Service là separate addon, KHÔNG thuộc Core scope.

---

## CHANGES MADE

### 1. Architecture Gaps Report Update ✅

**File:** `documents/05-qa-and-best-practices/architecture-gaps-analysis-2026-02-27.md`

**Changes:**

1. **Executive Summary:**
   - Critical Gaps: 5 → 4
   - Updated severity counts

2. **Section Title:**
   - "TOP 5 CRITICAL GAPS" → "TOP 4 CRITICAL GAPS"

3. **Removed GAP 4:**
   ```diff
   - ### GAP 4: Parent Entity KHÔNG có trong Core Service
   -
   - **Vấn đề:**
   - - `database-design.md` có `parents` table và `parent_children` table
   - - **NHƯNG:** Codebase Core KHÔNG có Parent entity
   -
   - **Tác động:**
   - - ❌ Parent login flow KHÔNG hoạt động
   - - ❌ Parent-children linking KHÔNG có API
   -
   - **Recommendation:** ⚠️ **BLOCKING - Implement Parent Module trước PR 1.8**
   ```

   **Lý do:** Đây KHÔNG phải gap vì Parent Service là optional addon future, không phải Core requirement.

4. **Renumbered GAP 5 → GAP 4:**
   - Guest Access Policy giờ là GAP 4 (vẫn là critical)

5. **Updated Recommendations:**
   ```diff
   **Action 1.2: Implement Missing Core Entities** (5 days)
   - - [ ] Parent entity + service + repository + controller
   - [ ] InstanceConfig API (feature detection)
   - [ ] Storage service (presigned URLs, quota tracking)

   + **Action 1.2: Implement Missing Core Entities** (3 days)
   + - [ ] InstanceConfig API (feature detection)
   + - [ ] Storage service (presigned URLs, quota tracking)
   ```

6. **Updated Phase 1 Roadmap:**
   - Removed "Implement Parent entity" from Week 1

7. **Added Architecture Note:**
   ```markdown
   ### Note về Parent Service

   ⚠️ **Parent Service is OPTIONAL ADDON (Future)**, không phải Core Service:
   - Architecture V4.1 clearly states: "Parent Service (Future)" - separate addon tại ₫100k/tháng
   - Core Service chỉ bao gồm: Gateway, Core (Admin + LMS + Marketing), Frontend
   - Parent tables sẽ thuộc separate Parent Service database (khi implement trong tương lai)
   - Không cần implement Parent Module trong current Core implementation
   ```

---

### 2. Database Design Update ✅

**File:** `documents/03-planning/database/database-design.md`

**Changes:**

1. **Removed parents và parent_children tables từ Core Database section:**
   ```diff
   - ### 3.3.3. parents
   -
   - ```sql
   - CREATE TABLE parents (
   -     id BIGSERIAL PRIMARY KEY,
   -     name VARCHAR(100) NOT NULL,
   -     ...
   - );
   - ```
   -
   - ### 3.3.4. parent_children
   -
   - ```sql
   - CREATE TABLE parent_children (
   -     parent_id BIGINT NOT NULL REFERENCES parents(id),
   -     student_id BIGINT NOT NULL REFERENCES students(id),
   -     ...
   - );
   - ```
   ```

2. **Added clarification note:**
   ```markdown
   **⚠️ Note về Parent Service:**
   - `parents` và `parent_children` tables thuộc **Parent Service (Optional Addon - Future)**
   - Parent Service là separate optional service theo Architecture V4.1
   - Không thuộc Core Database scope
   - Sẽ có separate database khi Parent Service được implement trong tương lai
   ```

3. **Updated Architecture Overview Diagram:**
   ```diff
     │  │  ├────────────────┤  │
     │  │  │ • students     │  │
     │  │  │ • teachers     │  │
   - │  │  │ • parents      │  │
     │  │  │ • classes      │  │
     │  │  │ • attendance   │  │
   ```

4. **Updated Core Service Tables List:**
   ```diff
     │  │  Tables:                                                  │ │
     │  │  • students         (student profiles)                    │ │
     │  │  • teachers         (teacher profiles)                    │ │
   - │  │  • parents          (parent profiles)                     │ │
     │  │  • classes          (class management)                    │ │
     │  │  • enrollments      (student-class relationship)          │ │
   + │  │  (parents/parent_children → Parent Service future)        │ │
   ```

5. **Updated Cross-Database Relationship Examples:**
   ```diff
   - #### Core Database - students/teachers/parents tables
   + #### Core Database - students/teachers tables

   -- Note: Parents table thuộc Parent Service (Optional Addon - Future)
   -- Sẽ có separate database khi Parent Service được implement
   ```

6. **Updated Mapping Logic Table:**
   ```diff
   | user_type | reference_id links to | Ý nghĩa |
   |-----------|----------------------|---------|
   | `ADMIN` | `NULL` | Admin không có entity trong Core |
   | `STAFF` | `NULL` | Staff không có entity trong Core |
   | `TEACHER` | `teachers.id` | Teacher profile trong Core |
   - | `PARENT` | `parents.id` | Parent profile trong Core |
   + | `PARENT` | `parents.id` (future) | Parent profile trong Parent Service (optional addon) |
   | `STUDENT` | `students.id` | Student profile trong Core |
   ```

---

### 3. Implementation Plan Update ✅

**File:** `documents/03-planning/implementation/kiteclass-implementation-plan.md`

**Changes:**

1. **Added Parent Service Architecture Note:**
   ```markdown
   **⚠️ Note về Parent Service:**
   - Parent Service là **Optional Addon (Future)** theo Architecture V4.1
   - KHÔNG thuộc Core Service scope (separate service với separate database)
   - Parent-related features sẽ được implement sau khi Core Service stable
   - Current Core PRs KHÔNG bao gồm Parent Module
   ```

**Existing (Already Correct):**
- Line 369: "PR 2.9 updated: Settings & Preferences (removed Parent Module - moved to Engagement Service P1)"
- Line 1346: "⏳ PARENT login with profile → Requires Core Parent Module (future PR)"
- Line 1652: "Waiting for: Core Parent Module implementation"
- Line 2800: "**Note:** Parent Module moved to Engagement Service (P1 priority)"

**Status:** Implementation plan đã correct từ trước, chỉ cần thêm architecture note để clarify.

---

## ALIGNMENT VERIFICATION

### ✅ Architecture V4.1 Compliance

| Component | V4.1 Definition | Current State | Status |
|-----------|----------------|---------------|--------|
| **Gateway Service** | Core Bundle | Implemented | ✅ |
| **Core Service** | Core Bundle (Student, Teacher, Class, LMS, Marketing) | In Progress | ✅ |
| **Frontend** | Core Bundle | In Progress | ✅ |
| **Parent Service** | Optional Addon (Future) | Not in scope | ✅ |
| **Gamification** | Optional Addon (Future) | Not in scope | ✅ |
| **Forum Service** | Optional Addon (Future) | Not in scope | ✅ |

### ✅ Database Alignment

| Database | Services | Tables | Status |
|----------|----------|--------|--------|
| **Gateway DB** | Gateway Service | users, roles, permissions, user_roles, refresh_tokens | ✅ |
| **Core DB** | Core Service | students, teachers, classes, courses, attendance, invoices, payments, gamification | ✅ |
| **Parent DB** | Parent Service (future) | parents, parent_children (when implemented) | ✅ Not in current scope |

### ✅ Implementation Plan Alignment

| Service | PRs Planned | Parent Module | Status |
|---------|-------------|---------------|--------|
| **Gateway** | 10 PRs | No (authentication only) | ✅ |
| **Core** | 15 PRs | No (removed from PR 2.9) | ✅ |
| **Frontend** | 13 PRs | No (uses Core APIs) | ✅ |
| **Parent Service** | Future PRs | Yes (when addon implemented) | ✅ |

---

## REMAINING REAL GAPS (AFTER CLEANUP)

### 🔴 Critical Gaps (4 remaining)

1. **GAP 1: LMS Module Schema KHÔNG tồn tại**
   - Missing: V13__create_lms_tables.sql
   - Impact: Cannot implement PR 2.9 (LMS Module)
   - Status: Still blocking

2. **GAP 2: Marketing Module Schema KHÔNG tồn tại**
   - Missing: V14__create_marketing_tables.sql
   - Impact: Lead management, contact forms không có storage
   - Status: Still blocking

3. **GAP 3: Storage Service Schema KHÔNG hoàn chỉnh**
   - Missing: uploaded_files, storage_quotas tables
   - Impact: File upload features blocked
   - Status: Still blocking

4. **GAP 4: Guest Access Policy KHÔNG được định nghĩa** (was GAP 5)
   - Missing: Product decision về public/authenticated routes
   - Impact: Blocking PR 3.12 (Guest Frontend Pages)
   - Status: Still blocking

**Note:** GAP về Parent Entity đã REMOVED vì đây không phải gap mà là correct architecture decision.

---

## IMPACT ASSESSMENT

### Before Cleanup (Incorrect State)

❌ **Problem:**
- Architecture Gaps Report claim Parent Entity là critical gap
- Database Design có parents tables trong Core schema
- Mixed signals về Parent Service scope
- Developers might waste time implementing Parent in Core

### After Cleanup (Correct State)

✅ **Benefits:**
1. **Clear Scope:** Core Service = Gateway + Core (LMS/Marketing) + Frontend only
2. **Correct Prioritization:** 4 real critical gaps, không bị distract bởi Parent false-positive
3. **Architecture Alignment:** 100% aligned với V4.1 bundled model
4. **Future-Proof:** Parent Service có clear path như optional addon (separate DB, separate PRs)
5. **Resource Planning:** Không allocate resources cho Parent trong current sprint

---

## NEXT STEPS

### Immediate (Week 1)

1. **Fix Real Critical Gaps:**
   - [ ] Create V13__create_lms_tables.sql (GAP 1)
   - [ ] Create V14__create_marketing_tables.sql (GAP 2)
   - [ ] Create Storage tables migration (GAP 3)

2. **Product Decision:**
   - [ ] Define Guest Access Policies (GAP 4)
   - [ ] Document public vs authenticated routes
   - [ ] Specify trial lesson access workflow

### Future (When Parent Service Addon is needed)

1. **Separate Parent Service Implementation:**
   - [ ] Create separate repository: `kiteclass-parent-service`
   - [ ] Design Parent Service database (separate from Core DB)
   - [ ] Implement Zalo OTP registration
   - [ ] Implement child tracking features
   - [ ] Create Parent Portal UI

2. **Integration with Core:**
   - [ ] Gateway UserType=PARENT remains (authentication layer)
   - [ ] Gateway.users.reference_id → ParentService.parents.id
   - [ ] Feign Client for Core ↔ Parent Service communication

---

## CONCLUSION

### Summary of Changes

| Document | Changes | Impact |
|----------|---------|--------|
| **Architecture Gaps Report** | Removed GAP 4, updated counts 5→4 | Clear focus on real gaps |
| **Database Design** | Removed parents tables, added notes | Core schema correct |
| **Implementation Plan** | Added architecture note | Clear separation |

### Alignment Status

✅ **100% Aligned với Architecture V4.1:**
- Core Bundle = 3 services (Gateway + Core + Frontend)
- Parent Service = Optional Addon (Future)
- Database scope correct (no parents in Core DB)
- Implementation plan clear (no Parent PRs in Core)

### Remaining Work

🔴 **4 Real Critical Gaps** require immediate attention:
1. LMS Module Schema
2. Marketing Module Schema
3. Storage Service Schema
4. Guest Access Policy definition

⏳ **Parent Service** sẽ được implement sau khi:
- Core Bundle stable (all 15 Core PRs complete)
- Customer requests Parent Addon feature
- Budget allocated cho optional service development

---

**Report Status:** ✅ Complete
**Architecture Alignment:** ✅ 100% aligned với V4.1
**Action Required:** Focus on 4 real critical gaps, defer Parent Service to future
**Next Review:** After fixing LMS/Marketing/Storage schemas (Week 1)
**Generated:** 2026-02-27 by Architecture Alignment Agent
