# Acceptance Criteria — P1 Solo Teacher (Gia sư tự do)

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** P1
**Persona name (VN):** Gia sư tự do
**Persona name (EN):** Solo Teacher
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent A (Wave Persona-AC-Template, GAP-151 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real solo teacher representative + Product Owner sign-off (deferred to GAP-152 Round 1 review)
**Tier:** 1 Primary
**Tracking:** GAP-151 Phase 1 → GAP-152 (Round 1 review execution) → GAP-153 (secondary persona AC, separate scope)

---

## 0. Context

### Scale assumption (from `personas-catalog.md`)
- **Users:** 1 teacher (sole operator), 5-50 students, 0 staff
- **Data volume:** 1-5 courses/subjects, ~50-200 lesson sessions per month, ~200-1000 attendance records/month, ~50-200 invoices/receipts per month
- **Usage pattern:** Mobile-first (teacher uses phone 70%+ of time on bus/coffee shop), peak hours weekday evenings (17:00-21:00) + weekends (cuối tuần dạy nhiều). Lessons may be 1-on-1 or small groups (≤5 students). Booking + reschedule churn happens daily.

### Organization archetype
- **Type:** Solo gia sư (freelance/part-time tutor) — có thể là giáo viên chính thức của trường công lập dạy thêm ngoài giờ, hoặc sinh viên đại học làm part-time, hoặc gia sư chuyên nghiệp full-time
- **Hierarchy:** Flat — 1 teacher → N students. Không có admin staff, không có hierarchy. Teacher = owner = operator = billing person.
- **Decision-making:** Teacher tự đăng ký, tự quản lý, tự thu tiền. Có thể giao tiếp trực tiếp với phụ huynh hoặc học sinh trưởng thành. Phụ huynh KHÔNG đăng nhập hệ thống ở scale này — communication qua Zalo/SMS.

### Revenue tier mapping
- **Expected tier:** FREE (5-15 students) → BASIC (15-50 students)
- **Reason:** Solo teacher có ngân sách hạn chế (sub-200K VND/month preferred). FREE tier để evaluate; BASIC tier khi business cần unlock advanced scheduling + larger student cap. Không bao giờ chạm PREMIUM/ENTERPRISE tier ở scale này.

### Real-world reviewer profile
- **Acting role:** "Gia sư tiếng Anh part-time, 30 học sinh tại TPHCM, dạy ngoài giờ + cuối tuần. Có công việc chính (giáo viên trường THCS hoặc nhân viên office). Dùng iPhone hoặc Android tầm trung. Không có laptop riêng cho công việc dạy thêm."
- **Critical concerns:**
  1. **Ease of setup (≤30 min onboarding):** Không có thời gian học công cụ phức tạp; nếu setup quá 30 phút sẽ bỏ
  2. **Mobile-friendly:** Phải dùng được hoàn toàn qua phone (chỉnh schedule, mark attendance, send Zalo)
  3. **Low monthly fee:** Sub-200K VND/month, FREE tier phải useable real-world (không chỉ là demo)
  4. **Simple invoicing:** Cash + bank transfer common; e-invoice không cần thiết ở scale này; chỉ cần receipt PDF gửi qua Zalo
  5. **Zalo/SMS communication:** Email không phải primary channel; phụ huynh + học sinh dùng Zalo

---

## AC Categories (6 standardized)

Each AC has format:
- **AC-CATEGORY-NUM** (3-digit zero-padded ID — e.g. AC-ONBOARD-001)
- **Statement** (1 sentence — what must be verifiable)
- **Test** (concrete scenario — reviewer can simulate)
- **Fail signal** (what reviewer observes if system gaps)
- **Status** (PASS / PARTIAL / FAIL — filled at review time, not at AC creation time)
- **Linked gap** (if FAIL → existing GAP-XXX or NEW gap to file)

---

## 1. Onboarding AC

Initial signup → first usable state ≤30 min target. Mobile-first; no IT help required.

- [ ] **AC-ONBOARD-001:** Teacher có thể hoàn tất signup + tenant provisioning trong ≤10 phút trên mobile (iOS Safari hoặc Android Chrome).
  - **Test:** Mở landing page trên iPhone, nhấn "Sign up", nhập email + phone + tên + chọn role "Solo Teacher" → nhận OTP Zalo/SMS → confirm → vào dashboard.
  - **Fail signal:** Form yêu cầu desktop, OTP không gửi qua Zalo/SMS, signup flow >5 màn hình, hoặc tenant chưa ready khi vào dashboard.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-ONBOARD-002:** Teacher có thể skip phần "Upload logo" và "Choose template" trong wizard branding (vì solo teacher không cần custom branding) — system tự dùng theme mặc định.
  - **Statement:** Wizard branding cho phép "Skip / Use default" ở mọi step, không force hoàn tất AI branding flow.
  - **Test:** Trong onboarding wizard, ở step "Upload logo" và step "Choose template", có nút "Skip" rõ ràng → vào dashboard với theme generic.
  - **Fail signal:** Wizard force hoàn tất AI branding (10+ phút overhead), hoặc skip dẫn đến broken theme.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-ONBOARD-003:** Teacher có thể tạo course đầu tiên + thêm 5 student sample trong ≤15 phút (kể cả thời gian explore UI).
  - **Test:** Sau khi vào dashboard, click "Create course" → nhập tên course → click "Add student" → nhập 5 students manual (tên + phone) → save. Tổng thời gian ≤15 phút.
  - **Fail signal:** UI yêu cầu nhập quá nhiều field bắt buộc cho mỗi student (email + ngày sinh + lớp + parent phone đều required); không có "skip optional" cho phụ huynh nếu học sinh trưởng thành.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-ONBOARD-004:** Onboarding tour highlight ≤5 features quan trọng nhất cho solo teacher (schedule, attendance, gradebook, invoice, communicate) — không phải toàn bộ feature list.
  - **Test:** Tour xuất hiện ở first login, navigate qua ≤5 tooltips, có "Skip tour" mọi lúc.
  - **Fail signal:** Tour 10+ steps, không skip được, hoặc highlight feature K-12 (academic year, semester) không relevant.
  - **Status:**
  - **Linked gap:** Related GAP-053 (academic year — phải hidden/optional cho solo persona)

---

## 2. Daily Operations AC

Recurring workflows after onboarding (schedule lessons, attendance, gradebook, communication, scheduling).

- [ ] **AC-OPS-001:** Teacher có thể schedule 1 lesson session trong ≤5 clicks trên mobile.
  - **Test:** Mở app trên phone → click "+ Lesson" → chọn ngày + giờ + course + students → save. Tổng ≤5 clicks/taps.
  - **Fail signal:** Yêu cầu desktop, ≥8 clicks, hoặc form quá phức tạp (yêu cầu room, building, period number).
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-OPS-002:** Teacher có thể tạo recurring class (weekly Tuesday 19:00-20:30 trong 12 tuần) với 1 form duy nhất, không cần tạo từng buổi.
  - **Test:** Tạo recurring class "English Tuesday-Thursday 19:00-20:30 từ 2026-05-01 đến 2026-08-01" → system tự generate ~24 sessions.
  - **Fail signal:** Phải tạo từng buổi manual; recurring rule chỉ support daily không support weekly multi-day.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-OPS-003:** Teacher có thể mark attendance per session (present/absent/late/excused) cho 5-10 students trong ≤2 phút trên mobile.
  - **Test:** Mở session today, tap status icon cho từng student → save. Tổng ≤2 phút cho 10 students.
  - **Fail signal:** Phải scroll qua dài danh sách, hoặc UI tap-target quá nhỏ trên phone, hoặc save không offline-capable.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-OPS-004:** Teacher có thể nhập điểm cho 1 assignment/test với gradebook simple (grade + optional comment per student) — KHÔNG yêu cầu rubric, weighted grade categories, hay GPA calculation.
  - **Test:** Click "+ Grade" → chọn assessment name + students + nhập grade → save.
  - **Fail signal:** UI force chọn grading scheme phức tạp, force enter rubric criteria, hoặc không có "free-form grade" option.
  - **Status:**
  - **Linked gap:** Related GAP-055 (MOET report card — N/A cho solo, gradebook đơn giản đủ)

- [ ] **AC-OPS-005:** Teacher có thể track student progress qua simple view: "tên student → attendance % + last 5 grades" — không cần dashboard analytics phức tạp.
  - **Test:** Mở student profile → thấy attendance summary (tỉ lệ %) + last 5 grades visible without click.
  - **Fail signal:** Profile chỉ có raw data, không summary; hoặc cần navigate 3+ trang để thấy progress.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-OPS-006:** Teacher có thể reschedule 1 session (đổi ngày/giờ) trong ≤3 clicks và auto-notify students qua Zalo/SMS.
  - **Test:** Open session → "Reschedule" → chọn ngày mới → save → notification tự gửi.
  - **Fail signal:** Phải delete session + tạo lại; hoặc notification không tự gửi (yêu cầu manual copy-paste vào Zalo).
  - **Status:**
  - **Linked gap:** GAP-063 (SMS/Zalo notification integration)

- [ ] **AC-OPS-007:** Teacher có thể cancel 1 session với reason field optional, students được notify ngay.
  - **Test:** Open session → "Cancel" → nhập reason (optional) → save → students được notify.
  - **Fail signal:** Cancel không tự notify; hoặc reason field bị required (block teacher khi đang gấp).
  - **Status:**
  - **Linked gap:** GAP-063

- [ ] **AC-OPS-008:** Teacher có thể quick-add 1 student mid-course (giữa khóa học) mà không cần re-import xlsx hay reset data.
  - **Test:** Trong active course, click "+ Add student" → nhập tên + phone → save → student xuất hiện trong roster ngay.
  - **Fail signal:** Phải quay lại roster setup, hoặc add student làm reset attendance history.
  - **Status:**
  - **Linked gap:** Related GAP-051 (xlsx import — solo persona thường manual entry, xlsx optional ở scale 30-50 students)

---

## 3. Financial / Admin AC

Billing, invoicing, simple income tracking. NO payroll (solo = no employees), NO complex tax (cash/transfer is primary).

- [ ] **AC-FIN-001:** Teacher có thể track tuition theo per-session pricing (ví dụ: 200K/buổi × 8 buổi = 1.6M/tháng) — KHÔNG force monthly subscription pricing model.
  - **Test:** Trong course settings, chọn pricing model "per session" → nhập price/session → system tự calculate monthly total based on attended sessions.
  - **Fail signal:** Chỉ support fixed monthly pricing; không có per-session option; hoặc auto-charge subscription bypass attendance.
  - **Status:**
  - **Linked gap:** Related GAP-185 (billing/VAT — partial relevance: solo cần simple, không cần e-invoice)

- [ ] **AC-FIN-002:** Teacher có thể tạo simple receipt PDF cho 1 payment, gửi qua Zalo/SMS link với 1 click.
  - **Test:** Click "+ Payment" → chọn student + amount + method (cash/transfer) → save → "Send receipt" generates PDF + Zalo share link.
  - **Fail signal:** Receipt yêu cầu e-invoice formatting (mã số thuế, MST), force VAT calculation, hoặc không có "Send via Zalo" button.
  - **Status:**
  - **Linked gap:** Related GAP-185

- [ ] **AC-FIN-003:** Teacher có thể xem monthly income summary (tổng thu / tổng outstanding / tổng chi nếu có) trong 1 trang.
  - **Test:** Mở "Finance" → thấy table: Tháng → Thu (paid) + Outstanding + (optional Expense) → Net.
  - **Fail signal:** Phải export Excel để xem, hoặc summary chỉ hiển thị YTD không có monthly breakdown.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-FIN-004:** Teacher có thể track outstanding payments (học sinh chưa đóng) với reminder visible — nhưng KHÔNG auto-suspend access (vì relationship-based business).
  - **Test:** Mở "Outstanding" tab → thấy list students chưa đóng kèm số tiền + ngày due. Click "Send reminder" → message Zalo.
  - **Fail signal:** Auto-suspend khi outstanding (làm hỏng quan hệ), hoặc reminder phải gửi từng người manual.
  - **Status:**
  - **Linked gap:** GAP-063

- [ ] **AC-FIN-005:** System KHÔNG yêu cầu teacher thiết lập payroll, commission, hay teacher account management (vì solo = chính teacher là owner).
  - **Statement:** Settings/menu KHÔNG hiển thị "Payroll / Teacher commission / Staff" cho FREE/BASIC solo persona — feature gated by tier hoặc role.
  - **Test:** Vào Settings của solo teacher account → không thấy "Payroll", "Teacher commission", "Hire staff" trong menu.
  - **Fail signal:** Menu hiển thị feature không relevant, làm rối UX, force teacher hiểu nghĩa của features không cần thiết.
  - **Status:**
  - **Linked gap:** Related GAP-057 (payroll commission — N/A cho solo persona)

---

## 4. Communication AC (stakeholders)

Notifications, schedule changes, reminders. Zalo/SMS-first; email secondary.

- [ ] **AC-COMM-001:** Teacher có thể send Zalo/SMS notification cho 1 student hoặc nhóm students (cùng class) với template predefined trong ≤3 clicks.
  - **Test:** Open class → "Notify students" → chọn template ("Reminder before class" / "Class cancelled" / "Custom") → preview → send.
  - **Fail signal:** Yêu cầu manual copy phone numbers vào Zalo native app; hoặc chỉ support email.
  - **Status:**
  - **Linked gap:** GAP-063 (SMS/Zalo notification integration)

- [ ] **AC-COMM-002:** System tự gửi reminder Zalo/SMS cho students 1 giờ trước class (configurable on/off per class), không cần teacher action.
  - **Test:** Tạo class với "Auto reminder 1h before" toggle ON → 1 giờ trước thấy log "Reminder sent" + students confirm nhận được message.
  - **Fail signal:** Reminder không tự gửi; hoặc gửi sai timezone (UTC thay vì Asia/Ho_Chi_Minh).
  - **Status:**
  - **Linked gap:** GAP-063

- [ ] **AC-COMM-003:** Khi teacher reschedule hoặc cancel class, students nhận notification ngay qua Zalo/SMS (không phải email-only).
  - **Test:** Cancel class trong app → trong ≤30 giây, students nhận Zalo message với reason + new time (nếu reschedule).
  - **Fail signal:** Notification chỉ gửi email; hoặc delay >5 phút; hoặc message thiếu thông tin time/reason.
  - **Status:**
  - **Linked gap:** GAP-063

- [ ] **AC-COMM-004:** Teacher KHÔNG cần parent portal — communication với parents qua Zalo direct (parent là contact, không phải user của system).
  - **Statement:** Solo persona không có parent login flow; parent chỉ nhận Zalo notification + receipt PDF link.
  - **Test:** Settings → không có "Invite parent to portal" cho FREE/BASIC solo tier (parent portal = feature gate cho P3+).
  - **Fail signal:** System force gửi parent invite email, hoặc parent portal CTAs làm rối UX cho solo teacher.
  - **Status:**
  - **Linked gap:** Out of scope — GAP-052 (parent portal) is N/A cho P1 solo persona

---

## 5. Edge Cases AC

Failure scenarios, mobile-specific edge cases, last-minute cancellations, network issues.

- [ ] **AC-EDGE-001:** Teacher có thể mark "no-show" cho student vắng mặt không báo trước, system track riêng vs "excused absent".
  - **Test:** Trong session attendance, có ≥3 status options: Present / Absent (excused) / Absent (no-show) / Late.
  - **Fail signal:** Chỉ có 2 options Present/Absent, không phân biệt được pattern no-show (cần data này để decide có drop student không).
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-EDGE-002:** Khi student cancel last-minute (<2h trước class), system cho phép teacher quyết định: charge full / charge partial / waive — ghi log để track pattern.
  - **Test:** Open session → mark student "Late cancel <2h" → system prompt "Charge: Full / Partial / Waive" → save với reason.
  - **Fail signal:** Auto-charge bất chấp policy của teacher, hoặc không có way để waive (force teacher chargeback manually).
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-EDGE-003:** Teacher có thể handle payment dispute (student claim đã đóng tiền nhưng chưa thấy ghi nhận) qua audit log: timestamp + amount + method + note.
  - **Test:** Mở payment history của student → thấy full audit log từng transaction kèm method (cash/transfer + screenshot reference) + ai entered.
  - **Fail signal:** Payment history chỉ show amount + date, thiếu evidence (transfer reference, cash receipt #), khó resolve dispute.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-EDGE-004:** App work usable offline cho mark attendance (mobile in basement / poor signal area) → sync khi có internet.
  - **Test:** Bật airplane mode, mở app, mark attendance cho 5 students → tắt airplane mode → data sync up không lost.
  - **Fail signal:** App crash hoặc lost data khi offline; hoặc phải reload toàn bộ khi sync.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-EDGE-005:** Teacher account survive mobile-app-uninstall (tất cả data cloud-side, không local-only) — re-install + login on new phone preserves all courses/students/payments.
  - **Test:** Uninstall app, install lại trên phone khác, login → thấy đầy đủ data.
  - **Fail signal:** Mất data khi uninstall; hoặc data localStorage-only không sync server.
  - **Status:**
  - **Linked gap:** —

---

## 6. Exit / Termination AC

Student finishes course, teacher pauses operations, data export.

- [ ] **AC-EXIT-001:** Khi student hoàn tất course, teacher có thể export student progress record (attendance summary + grades + payment history) thành PDF gửi cho student/parent.
  - **Test:** Open student → "Course completed" → "Export record" → PDF download + Zalo share.
  - **Fail signal:** Phải manual screenshot from multiple screens; hoặc export không có header/branding chuyên nghiệp.
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-EXIT-002:** Teacher có thể "pause" account (tạm dừng dạy 2-3 tháng nghỉ hè / nghỉ thai sản) với data preserved 30 ngày miễn phí, sau đó downgrade về data-export-only mode.
  - **Statement:** Có 3-state lifecycle: Active → Paused (30 days free) → Archived (read-only export, no new data) → Deleted (sau 36 tháng theo PDPL nếu không reactivate).
  - **Test:** Settings → "Pause account" → confirm → account paused, students notified, billing paused. Reactivate trong 30 ngày = full restore.
  - **Fail signal:** Pause = immediate data deletion; hoặc không có state Paused (chỉ có Active vs Cancelled).
  - **Status:**
  - **Linked gap:** —

- [ ] **AC-EXIT-003:** Teacher có thể self-service export TOÀN BỘ data (courses + students + attendance + grades + payments) thành xlsx + PDF zip, không cần liên hệ support.
  - **Test:** Settings → "Export all data" → email link với download zip trong ≤10 phút (background job).
  - **Fail signal:** Phải email support manually; hoặc export thiếu fields (chỉ có students không có attendance).
  - **Status:**
  - **Linked gap:** Related GAP-051 (xlsx tooling — export side)

---

## Scoring

**Total ACs:** 29 (sum across 6 categories: 4 Onboarding + 8 Operations + 5 Financial + 4 Communication + 5 Edge + 3 Exit — slightly above 15-25 target but within the 15-30 envelope from `_TEMPLATE.md` §"How to Use" recommended size; reflects mobile-first + offline edge cases that are critical for solo persona on phone)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Persona fully supported (production-ready for this persona) |
| 60-84% | ⚠️ Persona partially supported (usable but with gaps; defer GA for this persona) |
| 30-59% | 🔴 Persona NOT supported (major gaps; not production-ready) |
| <30% | ❌ Persona NOT viable (fundamental misfit; consider deferring to Tier 2/3 or out-of-scope) |

---

## Gap Linkage Summary

Status filled at GAP-152 review time. This section pre-populated with cross-references discovered during AC drafting:

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-OPS-006 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) | 🔵 OPEN | P1 |
| AC-OPS-007 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) | 🔵 OPEN | P1 |
| AC-OPS-008 | TBD | [GAP-051](../../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) | 🔵 OPEN | P0 (related — quick-add UX vs bulk import) |
| AC-FIN-001 | TBD | [GAP-185](../../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) | 🔵 OPEN | P1 (per-session pricing model + simple receipt) |
| AC-FIN-002 | TBD | [GAP-185](../../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) | 🔵 OPEN | P1 |
| AC-FIN-005 | TBD | [GAP-057](../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) | 🔵 OPEN | N/A — gate feature out for solo |
| AC-COMM-001 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) | 🔵 OPEN | P1 |
| AC-COMM-002 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) | 🔵 OPEN | P1 |
| AC-COMM-003 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) | 🔵 OPEN | P1 |
| AC-COMM-004 | TBD | [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md) | 🔵 OPEN | Out-of-scope cho P1 (parent portal là feature P3+) |
| AC-EXIT-003 | TBD | [GAP-051](../../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) | 🔵 OPEN | P0 (export side of xlsx tooling) |
| AC-ONBOARD-004 | TBD | [GAP-053](../../04-quality/gaps/GAP-053-academic-year-semester-structure.md) | 🔵 OPEN | Related — academic year cần hidden/optional cho solo |

**New gaps to file** (FAIL ACs without existing gap — go through `audit-to-gap-pipeline.md` Step 2.5 state-check before filing at GAP-152 review time):

Candidate areas (defer filing until Phase 2 review confirms FAIL status):
- "Per-session pricing model" — nếu khác với GAP-185 scope (subscription-only billing)
- "Recurring class generator (multi-day weekly)" — AC-OPS-002
- "Mobile offline attendance sync" — AC-EDGE-004
- "Account pause/resume lifecycle (30-day free)" — AC-EXIT-002
- "No-show vs excused-absent attendance differentiation" — AC-EDGE-001
- "Student progress export PDF (course completion record)" — AC-EXIT-001
- "Account-level role gating (hide payroll/staff menu cho solo persona)" — AC-FIN-005

**Out-of-scope for P1 (intentional skip — not gaps):**
- GAP-052 Parent portal — parent là contact, không phải user ở scale này
- GAP-053 Academic year/semester — gia sư works per-session, không per-semester
- GAP-054 Multi-subject per student — solo teacher thường 1 subject (English / Math / Music ...)
- GAP-055 Official MOET report card — informal tutoring, no MOET reporting required
- GAP-057 Payroll/commission — solo = no employees

---

## Cross-References

- **Persona source:** [`../personas-catalog.md`](../personas-catalog.md) §P1
- **Review skill:** [`../../../.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
- **Review reports:** [`../persona-reviews/`](../persona-reviews/) (output of GAP-152 quarterly reviews)
- **AC framework gap:** [GAP-151](../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (this template)
- **Review execution gap:** [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check
- **Sibling persona ACs (Wave Persona-AC-Template):** P2 Small Center, P5 K-12 School, Pa Parent (separate Agent files in same wave)

---

## Anti-Patterns Avoided in This Doc

- ✅ Specific ACs (≤30 min onboarding, ≤5 clicks, ≤2 phút attendance)
- ✅ Every AC has Test scenario reproducible by reviewer
- ✅ Every AC has Fail signal observable by reviewer
- ✅ Status BLANK at creation time — filled tại GAP-152 Round 1 review
- ✅ AC count = 29 (slightly above 15-25 target but within 15-30 envelope per `_TEMPLATE.md` §"How to Use"; mobile-first + offline edge cases critical for solo persona)
- ✅ §0 Context populated with scale + archetype + tier + reviewer profile
- ✅ Out-of-scope features explicitly listed (not silently omitted) to prevent reviewer confusion

---

## Log

- **2026-04-30** — Initial AC set v1 derived from `personas-catalog.md` §P1 + real-world solo-tutor workflow analysis. Author: Agent A (Wave Persona-AC-Template, GAP-151 Phase 1). 29 ACs across 6 categories (4+8+5+4+5+3). Cross-links: 6 distinct existing gaps (GAP-051/052/053/055/057/063/185). 7 candidate new gaps surfaced (defer filing to GAP-152 review). 4 explicit out-of-scope items documented.
- **TBD** — Domain expert reviewer sign-off (real solo teacher representative + Product Owner) — deferred to GAP-152 Round 1 review execution.
- **TBD** — GAP-152 Round 1 review completed, score X% — placeholder, filled at review time.
