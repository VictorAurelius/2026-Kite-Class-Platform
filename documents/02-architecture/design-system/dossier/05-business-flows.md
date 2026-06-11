# 05 — Business Flows

10 critical user flows. Each row tells Claude Design **what to mock** when designing screens — what the actor sees, what trigger starts the flow, what success/failure looks like.

**Use this when:** writing mock data for a screen. Look up the flow that screen belongs to, then mirror the same actors + data shapes used in the use-cases doc.

**Source:** `documents/01-business/kiteclass/*/use-cases.md`, `documents/01-business/kitehub/*/use-cases.md`. Synthesis where docs are skeleton-only.

---

## Flow 1 — Tenant signup → DEPLOYED instance

| Field | Value |
|-------|-------|
| Actor | P2 Center Owner |
| Trigger | "Dùng thử miễn phí 14 ngày" CTA on KH marketing landing |
| Success state | Instance status `DEPLOYED`, owner accesses tenant subdomain `[slug].kiteclass.app` |
| Duration p95 | 30–90s |
| Used by screens | KH `/register`, KH onboarding wizard, KH `/dashboard` (post-signup), KH `/instances/[id]` (lifecycle UI) |

**Steps:**
1. Owner fills signup form (email, password, phone, business name, slug — auto-suggested from name)
2. KH validates slug uniqueness (live debounce check)
3. POST `/api/v1/tenants` → backend emits `TenantCreatedEvent`
4. KC saga starts provisioning:
   - INITIALIZING — DB schema created, default seed data
   - GENERATING — AI Branding analyzer + planner run; logo/banner/hero generated
   - Quality gate /100 evaluates
   - DEPLOYED — instance reachable
5. KH dashboard polls + SSE updates lifecycle UI
6. On DEPLOYED, redirect to onboarding wizard (greeting, first class setup)

**Failure paths:**
- Slug conflict (409) — inline error, suggest alternative
- AI provider down — falls back to template-only branding
- Provisioning failure — instance marked FAILED, retry button visible
- Email already registered (409) — link to login

**Mock data for designs:**
- Slug: `mathmaster`, `ngoaingu-vietanh`, `truong-eduplus`
- Owner names: `Nguyễn Văn An`, `Trần Thị Hương`, `Lê Minh Tuấn`
- Business names: `Trung tâm Toán Master`, `Trung tâm Anh ngữ Việt-Anh`, `Trường THCS-THPT EduPlus`
- Email pattern: `[name]@[business-slug].vn` or `@gmail.com`

---

## Flow 2 — Bulk import students

| Field | Value |
|-------|-------|
| Actor | P3 Medium Center Admin / P5 K–12 Principal |
| Trigger | Admin clicks "Nhập danh sách từ Excel" on `/students` |
| Success state | Valid rows inserted; error report downloadable for failed rows |
| Duration | <60s for 10k rows |
| Used by screens | KC `/students` (entry), bulk-import modal, error report page |

**Steps:**
1. Admin downloads sample xlsx (or uses own format)
2. Drag-drop or click to upload (max 5 MB, ≤ 10k rows)
3. POST `/bulk-import/preview` parses + validates
4. Preview table shows: rows ✓ + rows × with inline errors
5. Admin reviews errors, optionally re-uploads corrected file
6. POST `/bulk-import/commit` batches insert (500 rows/txn)
7. Progress bar + per-batch status
8. Final report: N inserted, M failed (download error.xlsx)

**Failure paths:**
- File corrupt → `400 INVALID_FILE_FORMAT`
- >10k rows → `400 TOO_MANY_ROWS`
- Duplicate emails (within tenant) → row marked, others continue
- DB constraint violation → row skipped, logged

**Mock data:**
- Sample row: `Nguyễn Văn An | 2010-05-15 | nguyen.an@email.com | 0901234567 | Lớp 10A2 | Toán nâng cao`
- Error message: `Dòng 23: Số điện thoại không hợp lệ (phải bắt đầu bằng 03/05/07/08/09)`

---

## Flow 3 — Daily attendance (teacher)

| Field | Value |
|-------|-------|
| Actor | Teacher (homeroom or subject), optionally Students (QR scan) |
| Trigger | Class session scheduled — bell rings at 14:00 |
| Success state | Attendance locked, stats recalculated, parents notified via Zalo OA |
| Duration | 2–5 min mark + async recalc |
| Used by screens | KC `/classes/[id]/attendance`, attendance roster (G2), attendance calendar (G8) |

**Steps:**
1. Teacher opens session (auto-loaded from schedule)
2. Roster shown, all default to PRESENT
3. Teacher toggles status per student: P / V / M / L (see `02-vietnamese-ux-musts.md` §3)
4. Optional: late minutes input, excuse note
5. ALT: Students scan QR code → auto-set to PRESENT (or LATE if past start+15min, ABSENT if past +30min)
6. Save → BE validates no duplicates, calculates `attendance_rate`
7. Optimistic UI: row turns green/red as marked
8. After save, calendar view updates
9. Async: parent notification dispatched (Zalo OA)

**Failure paths:**
- No enrolled students (404) — empty state + add-students CTA
- Duplicate record (409) — server resolves to last-write-wins
- QR expired (400) — student sees re-scan prompt

**Mock data:**
- Class: `Lớp 10A2 - Toán nâng cao`
- Date: `Thứ Hai, 15/04/2026 14:00 - 15:30`
- Students: 25 names, mix of P/V/M/L statuses
- Attendance rate: 92%

---

## Flow 4 — Grade entry & finalization

| Field | Value |
|-------|-------|
| Actor | Teacher |
| Trigger | Students submit assignments; teacher ready to grade |
| Success state | Grades finalized + locked; transcripts available; parent can view |
| Duration | Assignment create 1–2 min; grading 5–20 min/class |
| Used by screens | KC `/classes/[id]/grades` (new), gradebook (G3) |

**Steps:**
1. Teacher creates assignment: `title`, `max_score`, `due_date`, `weight_pct`
2. Students submit work before close
3. Teacher grades each: enters score 0-10 (decimal allowed), late penalty auto-applied (10%/day, max 50%)
4. System calculates final weighted grade per student:
   - Default weights: attendance 10%, assignments 30%, midterm 25%, final 35%
   - Tenant-configurable
5. Teacher reviews finals + clicks "Chốt điểm" (Finalize)
6. Confirmation modal: irreversible action
7. Locked → readonly, parent gets Zalo notification

**Failure paths:**
- Weights ≠ 100% (400) — block save with diff banner
- Score > max (400) — inline cell error
- No submissions (404) — empty state for assignment

**Mock data:**
- Assignment: `Bài kiểm tra giữa kỳ 1`, max 10, weight 25%, due `30/04/2026`
- Scores: distribution mostly 6-9, few 4 or 10
- Honor breakdown: 5 Xuất sắc, 12 Giỏi, 7 Khá, 1 Trung bình

---

## Flow 5 — Payment & invoice

| Field | Value |
|-------|-------|
| Actor | P2 Owner / Admin / Student / Parent |
| Trigger | Enrollment created → invoice auto-generated; OR manual top-up |
| Success state | Invoice `PAID`, receipt available, attendance/grade access unlocked |
| Duration | Total ~3–5 min (sync) |
| Used by screens | KC `/billing/[id]/pay`, payment-method-selector (G5), invoice-detail (G6), payment-timeline (G10) |

**Steps:**
1. Enrollment triggers `EnrollmentCreatedEvent` → invoice created (PENDING)
2. Admin optionally adds adjustments (discount %, scholarship VND)
3. Payer (student or parent) views balance + selects method
4. Method options: VNPay / MoMo / ZaloPay / Bank transfer / Cash
5. System validates amount ≥ 100k VND, ≤ balance
6. Redirect to gateway / show QR / show bank info
7. Webhook confirms payment
8. System allocates to invoice, status → PAID
9. Receipt PDF generated, email sent

**Failure paths:**
- Amount < 100k VND (400 MIN_PAYMENT)
- Signature invalid (400) — gateway tampering check
- Payment timeout (15 min QR expiry)
- Gateway 503 — retry button + 24h queue

**Mock data:**
- Invoice: `Số HĐ: KH-2026-04-001`, `Khóa học Toán 10 - Kỳ 1`
- Amount: `1.500.000đ`
- Discount: `100.000đ` (early bird)
- Payment method preference (VN distribution): MoMo 35% / VNPay 30% / Bank 20% / ZaloPay 10% / Cash 5%

---

## Flow 6 — Parent views child's grades & attendance

| Field | Value |
|-------|-------|
| Actor | Pa. Parent |
| Trigger | Admin/teacher invites parent email |
| Success state | Parent ACTIVE account linked to child, can view dashboard |
| Duration | <2 min after token click |
| Used by screens | KC `/parent-invite/[token]`, KC `/parent` dashboard, parent invite (G7) |

**Steps:**
1. Admin enters parent email + selects child(ren) in admin UI
2. Email sent with redemption link (`https://[tenant].kiteclass.app/parent-invite/[token]`)
3. Token expiry: 24h
4. Parent clicks → sees welcome page with child name + center info
5. Parent creates account (email/password) OR signs in if already has account
6. ParentStudentLink created
7. Parent dashboard loads: today's class, attendance %, latest grade, next assignment due, fee balance

**Failure paths:**
- Token expired (400 PARENT_INVITATION_EXPIRED) — request new link
- Email already registered → merge link to existing account
- Network failure → resume from email link

**Mock data:**
- Child: `Lê Minh Tuấn (Lớp 10A2)`
- Attendance: 92% this month
- Latest grade: `Toán: 8.5`
- Fee balance: `Đã đóng đủ học phí kỳ 1`
- Zalo OA toggle: ON

---

## Flow 7 — Trial → upgrade to paid

| Field | Value |
|-------|-------|
| Actor | P2 Owner |
| Trigger | Instance status `TRIAL`, owner clicks "Nâng cấp" |
| Success state | Instance `ACTIVE`, subscription row created, zero downtime, welcome email |
| Duration | 3–5s (typically <5s p95) |
| Used by screens | KH `/billing/upgrade` (39/128 🔴), KH `/billing/payment/[id]` |

**Steps:**
1. Owner sees trial countdown banner (X days left)
2. Click "Nâng cấp ngay" → tier selection (BASIC / PREMIUM / ENTERPRISE)
3. Each tier shows: price, AI quota, regenerate quota, feature list
4. Owner picks tier + payment method
5. System validates `migration_phase=NONE`, sets `INITIATED`
6. Payment gateway captures charge
7. Async worker: TRIAL → ACTIVE, create subscription row
8. AI budget refreshed per tier, branding templates refreshed
9. Welcome screen + new feature unlock animation

**Failure paths:**
- Payment declined (402)
- Migration worker unavailable (503) → retry from PAYMENT_CAPTURED
- Concurrency conflict (409) — refresh page

**Mock data (tier table):**
| Tier | Price/month | AI quota | Regenerate | Storage |
|------|------------|----------|-----------|---------|
| BASIC | `199.000đ` | 1 GB AI calls | 10/ngày | 10 GB |
| PREMIUM | `499.000đ` | 5 GB | 30/ngày | 50 GB |
| ENTERPRISE | `Liên hệ` | unlimited | unlimited | unlimited |

---

## Flow 8 — AI Branding regenerate (rebrand)

| Field | Value |
|-------|-------|
| Actor | Owner (non-Enterprise) or Admin (Enterprise — needs 2nd approval) |
| Trigger | Instance DEPLOYED, owner requests rebrand |
| Success state | Package regenerated, FE cache invalidated, theme + assets updated |
| Duration | Pipeline 10–30s; E2E <60s |
| Used by screens | KH `/branding/wizard` (33/128 🔴), KH `/branding/templates`, KH `/branding/assets`, theme-live-preview (G11) |

**Steps:**

**Non-Enterprise:**
1. Owner POSTs `/instances/{id}/rebrand` with new params (audience, tone, template)
2. Lifecycle sets REGENERATING, evicts cache
3. AI pipeline re-runs (analyze + plan + execute)
4. Quality gate /100 evaluates (5 checks)
5. If pass: DEPLOYED, `brandingVersion++`, ETag invalidated
6. If fail (<70): falls back to template-only

**Enterprise (approval needed):**
1. Admin 1 requests
2. Admin 2 must approve in 24h window
3. Concurrent version check (409 if stale)
4. Approval → triggers same rebrand flow

**Failure paths:**
- AI provider down → template-only fallback
- Approval denied (REJECTED state)
- Request expired (24h → EXPIRED)
- Quality gate fail → display issues + suggest fix

**Mock data:**
- Audience options: `Trường mầm non`, `Trường THCS`, `Trung tâm tiếng Anh`, `Lớp luyện thi đại học`
- Tone options: `Chuyên nghiệp`, `Thân thiện`, `Năng động`, `Sang trọng`
- Templates: 6 preview cards (each with a different visual language)

---

## Flow 9 — Student enrollment to active

| Field | Value |
|-------|-------|
| Actor | Admin + Student |
| Trigger | Admin enrolls student in class |
| Success state | Enrollment ACTIVE, student in roster, can submit/view |
| Duration | 1–2 min create + payment window |
| Used by screens | KC `/students/new`, KC `/classes/[id]` (enroll modal), invoice flow |

**Steps:**
1. Admin creates student (name, email, phone, DOB)
2. Admin enrolls in class (capacity check)
3. Discount applied if applicable
4. `EnrollmentCreatedEvent` → invoice generated (Flow 5)
5. Student status `PENDING_PAYMENT`
6. After payment, status `ACTIVE`

**Failure paths:**
- Class full (409 CAPACITY_REACHED)
- Already enrolled (409 DUPLICATE_ENROLLMENT)
- Email duplicate (409)
- Capacity 0 (400)

**Mock data:**
- Student: `Phạm Thị Mai`, born `2008-09-15`, `0901234567`
- Class capacity: 30 / 30 (full) or 25 / 30
- Discount: 10% sibling discount

---

## Flow 10 — Institutional sync (K–12 academic year refresh)

| Field | Value |
|-------|-------|
| Actor | P5 K–12 Principal/Admin |
| Trigger | New school year begins (September) |
| Success state | Roster refreshed, classes structured, parent links carried |
| Duration | 5–10 min validation + 1–2 min insert |
| Used by screens | KC `/students` bulk-import (Flow 2), KH `/admin/instances/[id]` settings, academic year config |

**Steps:**
1. Admin exports last year's student list from legacy system
2. Cleans up graduates/transfers, adds new students
3. Uploads xlsx via bulk-import (Flow 2 reused)
4. Academic year structure refreshed:
   - New semester calendar (HK1: Sept-Jan, HK2: Feb-June)
   - New classes created (Lớp 10A1, 10A2, ... Lớp 12A5)
   - Parent links carried forward (keep ParentStudentLink even if student moved class)
5. Teachers reassigned (homeroom + subjects)
6. Ready for enrollment (Flow 9)

**Failure paths:**
- File format mismatch
- >10k rows
- Duplicate phone globally (warning, not blocker — phone shared by sibling)
- No semester setup → partial import OK

**Mock data:**
- Year: `Năm học 2026-2027`
- Class structure: 5 grades × 5 sections = 25 classes
- Student count: 1500
- Imports: 500 new (lớp 10), 1000 returning

---

## What this list does NOT cover

- Internal admin flows (refund, dispute escalation, audit log review) — Round 3
- Marketing flows (newsletter signup, demo booking) — Direction A scope
- API integrations (Zalo OA setup, MoMo merchant onboarding) — backend setup, not UI
- Subscription cancellation / pause / churn flow — Round 3 (post-PMF priority)

When Claude Design needs a flow not listed here, ask the user to confirm scope before designing.
