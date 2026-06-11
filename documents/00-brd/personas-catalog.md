# Personas Catalog — KiteHub + KiteClass SaaS

**Trạng thái:** 🟡 DRAFT v1
**Ngày:** 2026-04-14
**Mục đích:** Canonical list of target personas. Review quarterly.

**Principle:** "SaaS này phải tạo sân chơi chung cho TẤT CẢ đối tượng thỏa mãn nhu cầu core của quản lý và học trực tuyến."

---

## Persona Classification

### Tier 1: Primary Target (Launch Focus)
Những persona dự án nhắm đến và phải support đầy đủ tại GA launch.

### Tier 2: Secondary (Post-GA expansion)
Support sau GA, có thể với limited features ban đầu.

### Tier 3: Future / Maybe Out-of-Scope
Review sau khi dự án mature, decide support hay không.

---

## Complete Personas List

### 🎯 Tier 1: Primary

#### P1. Solo Teacher (Gia sư tự do)
- **Scale:** 1 teacher, 5-50 students, 1-5 courses
- **Profile:** Tự do, part-time, có thể là giáo viên chính thức ngoài giờ
- **Revenue:** FREE → BASIC tier
- **Key needs:**
  - Simple scheduling
  - Payment tracking (học phí cá nhân)
  - Basic gradebook
  - Student progress tracking
  - Parent communication
- **Pain points:**
  - Mất công quản lý thủ công
  - Thu tiền khó track
  - Nhiều app khác nhau

#### P2. Small Tutoring Center (Trung tâm nhỏ / Lớp học thêm)
- **Scale:** 1-3 teachers, 20-100 students, 3-10 classes
- **Profile:** Chủ trung tâm tự dạy + thuê 1-2 giáo viên, thường là dạy thêm văn-toán-anh-lý-hóa
- **Revenue:** BASIC → PREMIUM
- **Key needs:**
  - Class management (schedule, roster)
  - Enrollment + payment collection
  - Attendance tracking
  - Basic reporting
  - Parent notifications
- **Pain points:**
  - Không có admin dedicated
  - Owner wear many hats

#### P3. Medium Education Center (Trung tâm quy mô vừa)
- **Scale:** 5-20 teachers, 100-500 students, 10-50 classes
- **Profile:** Organized với dedicated admin, multiple subjects
- **Revenue:** PREMIUM
- **Examples:** Small language centers, STEM clubs
- **Key needs:**
  - Role-based access (admin/teacher/accountant)
  - Multi-course catalog
  - Financial reports (monthly/yearly)
  - Teacher payroll (hours + commission)
  - Marketing website (branding)
- **Pain points:**
  - Role complexity
  - Financial tracking overhead

#### S. Student (Học sinh / Người học) — Cross-tenant canonical persona
- **Scale:** Cross-tenant — same persona spans P1/P2/P3/P5 contexts; ~85% mobile-PWA sessions
- **Age range:** 6-22 tuổi spectrum, 4 age bands (tiểu học / THCS / THPT / vocational+university)
- **Profile:** Digital native, mobile-primary, parent-mediated cho under-18, child-protection constraints (no off-platform DM, no direct payment, parent-reset password)
- **Revenue:** N/A (student is consumer of tenant service, not buyer; parent or tenant-admin pays)
- **Key needs (8 journeys):**
  - Today (home, next-class context)
  - My Classes (enrolled list — 1-15 lớp tùy tenant)
  - Assignment workflow (view, submit, saved-draft, deadline tracking)
  - Grades (self-tracking + GPA + Học lực + parent visibility + GVCN comment for P5)
  - Attendance (read-only, anti-fraud, period-granular for P5)
  - Notifications (parent-kép visualization, throttled, daily-digest option for P3+)
  - Profile (basic info, locked PII for minor, preset avatar)
  - Payment fees (READ-ONLY for K-12 — parent-trigger workflow, AC-FIN-001 cấm "Pay" button)
- **Distinct constraints (NOT inherited from owner):**
  - Child-protection lock for under-18 (parent-mediated payment + parent-reset password)
  - Notification throttling + parent-kép visualization
  - Anti-fraud attendance (teacher marks, student CHỈ view)
  - No off-platform DM với teachers (per child-protection-policy.md §4.2)
- **AC doc:** [`persona-criteria/S-student.md`](persona-criteria/S-student.md) — **21 ACs (6 LEGAL)**, canonical cross-tenant; secondary docs (`student-in-P2.md` / `student-in-P3.md` / `student-in-P5.md`) extend với tenant-context overrides
- **Pain points:**
  - Mobile-only with limited tech literacy (cấp 1-2)
  - Parent-mediated workflow friction (forgot password, payment status visibility)
  - Multi-class spam at scale (P3+ multi-teacher exposure)

#### P5. Public/Private K-12 School (Trường tiểu học/THCS/THPT) ← **USER'S EXAMPLE**
- **Scale:** 50+ teachers, 500-3000 students, 30+ classes, 10-30 staff
- **Profile:** Hierarchical (principal, VPs, department heads, teachers, students, PARENTS, staff)
- **Revenue:** ENTERPRISE
- **Key needs (MANY unique to K-12):**
  - **Bulk import xlsx** (500 students trong 1 ngày)
  - Semester/school-year structure (not just classes)
  - **Parent portal** (critical cho K-12)
  - Grade report card (bảng điểm chính thức VN format)
  - MOE (Bộ GD&ĐT) reporting
  - Multiple subjects per student (not 1 course/class)
  - Homeroom teacher concept (GVCN)
  - Attendance by period (multiple slots per day)
  - Academic year calendar với VN public holidays
  - Student conduct/behavior tracking (hạnh kiểm)
  - Class size 30-50 (current model assumes smaller?)
  - Report cards signed by principal
  - Promotion/retention logic (lên lớp/ở lại lớp)
  - Student transfer between classes (chuyển lớp giữa năm)
  - Communication với phụ huynh (SMS, app)
  - Fee management (khác trung tâm — thường 1 năm học)

### 🎯 Tier 2: Secondary

#### P4. Large Education Chain / Franchise (Chuỗi/Franchise)
- **Scale:** 20+ teachers, 500-5000 students, multi-branch
- **Examples:** Apollo English, Popodoo, Wall Street English VN
- **Key needs:**
  - Multi-brand per tenant (GAP-027)
  - Franchise royalty tracking
  - Cross-branch student transfers
  - Consolidated dashboards (across branches)
  - Brand consistency enforcement
  - Regional pricing

#### P7. Corporate Training Department (Phòng đào tạo doanh nghiệp)
- **Scale:** Internal employees 50-5000
- **Profile:** Company HR/L&D department training employees
- **Key needs:**
  - SCORM/xAPI compliance
  - Employee training records
  - Compliance certifications
  - HR system integration (Workday, SAP, etc.)
  - Course assignment via role/department
  - Learning path / career progression tracking

#### P8. Online-only Course Creator (Nhà sáng tạo khóa học online)
- **Scale:** 1 creator, 100-10000 students
- **Profile:** Udemy/Coursera-style individual instructor
- **Key needs:**
  - Video hosting + streaming
  - Drip content (unlock per day)
  - Marketing funnels
  - Affiliate programs
  - Discussion forums
  - Course reviews/ratings
- **Note:** Overlaps với Solo Teacher but async-focused

### 🎯 Tier 3: Future / Evaluate

#### P6. University / College (Đại học/Cao đẳng)
- **Scale:** Very large, complex curriculum
- **Verdict:** Likely OUT OF SCOPE — specialized LMS existing (Moodle, Blackboard)

#### P9. International/Bilingual School (Trường quốc tế/song ngữ)
- **Scale:** Medium-large
- **Key needs:**
  - Multi-curriculum (IB, Cambridge, VN)
  - Multi-language content
  - International parent communication
  - Higher pricing tolerance

#### P10. Special Education Center (Giáo dục đặc biệt)
- **Scale:** Small, specialized
- **Key needs:**
  - IEP (Individualized Education Program)
  - Specialized assessments
  - Parent involvement critical
  - Slower pace / individualized plans

---

## Secondary Personas (Users within Tenant)

**AC framework:** Each secondary persona × tenant context combination needs its own acceptance criteria doc. See **GAP-153** for Phase 1 scope (8 P0 cells: Student × P2/P3/P5, Parent × P5, Teacher-employee × P3/P5, Admin × P3/P5). Files live in `persona-criteria/secondary/<role>-in-P<N>.md`.

### Within any tenant, users include:

| Role | Persona | Key Actions | AC doc |
|------|---------|-------------|--------|
| **Owner/Director** | Decision maker | Strategic config, billing, top-level reports | = tenant persona (P1/P2) |
| **Admin** | Operations manager | User management, class setup, financial ops | GAP-153: admin-in-P3.md, admin-in-P5.md |
| **Teacher** | Educator | Gradebook, attendance, class management, communication | GAP-153: teacher-employee-in-P3.md, teacher-employee-in-P5.md |
| **Student** | Learner | View schedule, submit work, view fees read-only, view grades | **Tier-1 canonical:** [`S-student.md`](persona-criteria/S-student.md) (Wave 22, GAP-365); tenant-context extensions: GAP-153 [`student-in-P2`](persona-criteria/secondary/student-in-P2.md) / [`student-in-P3`](persona-criteria/secondary/student-in-P3.md) / [`student-in-P5`](persona-criteria/secondary/student-in-P5.md) |
| **Parent** | Guardian (K-12) | View child's progress, pay fees, communicate with teacher | GAP-153: parent-in-P5.md (P0); P2/P3 deferred P1 |
| **Accountant** | Finance | Invoicing, payment collection, financial reports | Phase 3 P2 cells (deferred to GAP-282) |
| **Receptionist** | Front-desk | Enrollment, inquiries, scheduling | Phase 3 P2 cells (deferred to GAP-282) |
| **IT Staff** | Technical | Integration, data import/export, troubleshooting | Phase 3 P2 cells (deferred to GAP-282) |
| **Parent Rep** | Parent committee | Organize events, coordinate với school | Phase 3 P2 cells (deferred to GAP-282) |

**Phase 2 P1 cells (deferred to GAP-281):** student-in-P1 (Solo Teacher tenant), parent-in-P2 + parent-in-P3 (parent engagement at smaller scales), teacher-employee-in-P2 (1-2 hired teachers).

**Wave Secondary-Persona-AC (2026-04-30, 12th wave-pack) SHIPPED 8 P0 cells: GAP-153 → 🟢 DONE.** Total **167 ACs** across 8 secondary persona AC docs (Student × P2/P3/P5 + Parent × P5 + Teacher-employee × P3/P5 + Admin × P3/P5).

---

## Coverage Review Status (Round 1 measured 2026-05-04 via GAP-152 / Wave 17)

### Tier 1 Personas (measured via Wave 17 role-play review)

| Persona | Coverage (Round 1) | ACs scored | New gaps | Top blocker |
|---------|:------------------:|:----------:|:--------:|-------------|
| P1 Solo Teacher | 🔴 **36.2/100** (was 60% est) | 29 (7 PASS / 7 PARTIAL / 15 FAIL) | 10 (GAP-286..295) | Mobile OTP signup + Zalo notification (GAP-063 keystone) |
| P2 Small Center | 🔴 **36.8/100** (was 75% est) | 38 (7 PASS / 14 PARTIAL / 17 FAIL) | 8 (GAP-296..303) | Notification + commission engine (GAP-063 + GAP-057 keystones) |
| P3 Medium Center | ❌ **9.6/100** (was 65% est) | 82 (0 PASS / 16 PARTIAL / 67 FAIL) | 15 (GAP-306..320 — full range) | Commission/payroll + multi-class scheduling + RBAC audit |
| P5 K-12 School | ❌ **8.3/100** (was 30% est) | 134 (7 PASS / 27 PARTIAL / 100 FAIL) | 24 (GAP-321..344) | LEGAL: parent portal (Luật GD Đ.83) + child protection (Luật Trẻ Em Đ.51) — see GAP-321/322/323 |

**Verdict:** All 4 Tier-1 personas NOT ready for GA. Round 1 measurements significantly LOWER than 2026-04-14 estimates — estimates were optimistic.

**Reports:** [`P1`](persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) · [`P2`](persona-reviews/P2-small-center-round-1-2026-05-04.md) · [`P3`](persona-reviews/P3-medium-center-round-1-2026-05-04.md) · [`P5`](persona-reviews/P5-k12-school-round-1-2026-05-04.md)

**Cross-persona keystones (recommended priority bumps):**
- **GAP-063 Zalo/SMS notification** P1 → **P0** (blocks all 4 personas; 8/21 P2 FAILs depend on it)
- **GAP-057 Commission/payroll engine** P1 → **P0** (blocks P2, P3, P5; foundational for VN tutoring/center model)
- **Recurring class generator** (GAP-290) — needed by all 4 personas (RRULE-based scheduling)
- **K-12 Stage 1 LEGAL bundle:** GAP-321 + GAP-322 + GAP-323 — must ship before any K-12 deployment (~6 weeks)

### Tier 2

| Persona | Coverage | Gaps |
|---------|:--------:|------|
| P4 Chain | 🔴 20% | Multi-brand (GAP-027), franchise tracking |
| P7 Corporate | 🔴 10% | SCORM, compliance tracking, HR integration |
| P8 Online Course | 🔴 25% | Video hosting, drip content |

### Tier 3
Out of scope for now. Evaluate post-GA.

---

## Critical Missing Features (Summary)

Derived từ role-play analysis. Each = 1 gap file:

| Gap | Feature | Blocks Persona(s) | Priority |
|-----|---------|-------------------|:--------:|
| GAP-051 | Bulk import xlsx (students + teachers) | P5 (critical), P3, P4 | 🔴 P0 |
| GAP-052 | Parent portal + accounts | P5 (critical), P9 | 🔴 P0 |
| GAP-053 | Academic year + semester structure | P5, P9 | 🔴 P0 |
| GAP-054 | Multi-subject per student (không chỉ 1 course/class) | P5 | 🔴 P0 |
| GAP-055 | Official grade report card (bảng điểm VN format) | P5, P9 | 🟠 P1 |
| GAP-056 | Homeroom teacher (GVCN) concept | P5 | 🟠 P1 |
| GAP-057 | Payroll + teacher commission calculation | P3, P4, P5 | 🟠 P1 |
| GAP-058 | Role hierarchy + org chart | P3, P4, P5 | 🟠 P1 |
| GAP-059 | Student conduct/behavior tracking (hạnh kiểm) | P5 | 🟡 P2 |
| GAP-060 | Period-based attendance (multiple slots/day) | P5 | 🟡 P2 |
| GAP-061 | Promotion/retention logic (lên lớp) | P5 | 🟡 P2 |
| GAP-062 | Teacher payroll integration | P3, P4, P5 | 🟡 P2 |
| GAP-063 | SMS/Zalo notification integration | All | 🟠 P1 |
| GAP-064 | SCORM/xAPI compliance | P7 | 🟡 P2 |

---

## Role-Play Assumption: Thị trường VN

Vietnamese-specific considerations:
- Phụ huynh rất active (đặc biệt K-12)
- Zalo > Facebook cho parent communication
- VNPay, MoMo, ZaloPay payment methods
- Tax: Tổng cục Thuế invoice format
- Education law: MOE regulations, curriculum standards
- Cultural: respect for teachers (Tôn sư trọng đạo)
- Fee payment: thường đóng theo kỳ (1-3 months) or theo năm học

---

## Next Actions

1. ✅ Create this catalog
2. 🆕 Run role-play for each Tier 1 persona (GAP-050 tracks)
3. 🆕 Create gap files for critical missing features (GAP-051..064)
4. 🆕 Prioritize based on persona coverage
5. 🔁 Quarterly review cycle

---

## Log

- 2026-05-06 — Added Tier-1 S. Student canonical persona doc ([`persona-criteria/S-student.md`](persona-criteria/S-student.md)) — cross-tenant canonical AC (21 ACs, 6 LEGAL); existing 3 secondary docs (`student-in-P2/P3/P5.md`) reframed as tenant-context extensions of this Tier-1 doc. Filed by Wave 22 Bucket C closure (GAP-365 → 🟢 DONE). Triggered by Wave 20 Bucket A external review surfacing absence — kit reviews previously used `secondary/student-in-P2.md` as proxy → calibration drift risk. Tier-1 doc now serves as canonical AC source for kit reviews + Track 2 ports.
- 2026-04-20 — Secondary Personas table updated with AC doc pointers. GAP-153 added for secondary persona AC coverage (Student × P2/P3/P5, Parent × P5, Teacher-employee × P3/P5, Admin × P3/P5). Triggered by user question "BRD đã có cho đối tượng học sinh không?" revealing scope gap.
- 2026-04-14 — Initial catalog created. Review triggered by user raising bulk import as critical gap for K-12 schools.
