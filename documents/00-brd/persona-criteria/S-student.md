# Acceptance Criteria — S. Student (Học sinh / Người học)

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** S
**Persona name (VN):** Học sinh / Người học
**Persona name (EN):** Student / Learner
**Last-Updated:** 2026-05-06
**Reviewer (Phase 1 — author):** Wave 22 Bucket C agent (GAP-365 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real student rep (3 age bands) + Parent rep + GVCN homeroom + Legal counsel (child protection) + Product Owner (deferred to GAP-152 Round 1 — multi-stakeholder)
**Tier:** 1 Primary (canonical persona — sister of P1/P2/P3/P5 tenant personas)
**Tracking:** GAP-365 Phase 1 → GAP-152 (Round 1 review) → secondary/* docs as tenant-context extensions
**Strategic priority:** Mobile-PWA primary user (~85% sessions); cross-cuts ALL tenant types (P1/P2/P3/P5)
**Legal compliance:** PDPL Decree 13/2023 Art 16 (minor data + parental consent), Luật Trẻ em 2016 Đ.51 (mandatory child-safety reporting), Luật Giáo dục 2019 Đ.83 (parental monitoring rights)

---

## 0. Context

### Why this is a Tier-1 doc (not just a sub-persona)

S. Student is **NOT** simply a sub-persona of P1/P2/P3/P5 — they are a Tier-1 persona in their own right because:

1. **Cross-cutting persona:** The same student can be a learner trong P1 (gia sư private), P2 (lớp học thêm), P3 (trung tâm), hoặc P5 (trường K-12). Tenant context shifts UX details (multi-class vs single-class, formal vs informal tone, GVCN vs Owner contact), but **core student journeys remain consistent**: schedule view, assignment workflow, grade self-tracking, attendance check, payment status (READ-ONLY for K-12).

2. **Distinct device + UX constraints:** Mobile-PWA primary (~85% sessions per `kiteclass-student/README.md`); short bursts of 3-5 min between classes; thumb-reach-only navigation; emoji-friendly empty states; informal "bạn" address.

3. **Distinct legal constraints:** Child-protection lock for under-18; parent-mediated payment (cannot execute commitments); parent-kép notification visibility; no off-platform DM với teachers (PDPL Art 16 + Luật Trẻ em 2016).

4. **Distinct unique journeys NOT inherited from owner persona:** Assignment submission (student-only action), grade self-tracking + GPA visualization, attendance check-in (read-only), parent-trigger payment workflow (AC-FIN-001).

This Tier-1 doc serves as the **canonical AC source** — the secondary docs (`student-in-P2.md`, `student-in-P3.md`, `student-in-P5.md`) extend this with tenant-context overrides (multi-class scope, MOET formal artifacts, period-based scheduling). Kit reviews + Track 2 ports cite this doc; if a tenant-context-specific AC is needed, it lives in the matching `secondary/student-in-P*.md`.

### Persona basics

- **Name (VN):** Học sinh / Người học
- **Name (EN):** Student / Learner
- **Age range:** 6-22 tuổi spectrum, 4 age bands:
  - **Tiểu học (lớp 1-5):** 6-10 tuổi — minor, parent-mediated cho hầu hết workflows; PH thay mặt setup; basic touch UX critical
  - **THCS (lớp 6-9):** 11-14 tuổi — minor, parent-mediated; student tự thao tác app cơ bản; PH có quyền giám sát đầy đủ
  - **THPT (lớp 10-12):** 15-18 tuổi — phần lớn vẫn <16 (lớp 10-11 = 15-16t = minor), lớp 12 typically 17-18 transitioning sang adult tự consent
  - **Vocational + university tutoring:** 18-22 tuổi — adult, có thể trực tiếp pay (out-of-scope this phase per personas-catalog Tier-1)
- **Primary device:** Mobile-PWA (~85% sessions, smartphone Android/iOS); desktop ~10% (BTVN dài cuối kỳ); tablet ~5%
- **Session pattern:** Short bursts 3-5 min giữa các tiết / sau tiết / lúc đi ngủ; peak BTVN hours 19h-22h; semester-end peak (xem học bạ + báo điểm)
- **Communication preferences:**
  - **In-app push** = primary (notifications GVCN/teacher/owner)
  - **Zalo** = parent-kép channel (cả student + parent nhận, hoặc parent-only cho cấp 1-2)
  - **Email** = NOT preferred (student không check email native — fallback only cho documents formal)
  - **SMS** = OTP + emergency only (cost-sensitive)
- **Tech literacy:** Digital native (Zalo/Facebook/TikTok native) BUT lacking admin software experience — UX phải simple, minimum jargon, emoji-friendly cho cấp 1-3, semi-formal cho cấp 3
- **Notifications throttled:** Daily digest option (avoid spam khi student × 5 môn × multi-teacher); parent-kép visualization (cả student + parent thấy cùng update để tránh "spin" câu chuyện)
- **Parent visibility (K-12 mandate):** PH có quyền pháp lý xem mọi tương tác student với platform per Luật GD 2019 Đ.83; child protection mandates cho under-16

### Tenant context dependencies (cross-cuts secondary/* docs)

Core journeys are tenant-agnostic, BUT specific behaviors flex per tenant context:

| Tenant context | Override doc | Distinguishing constraints |
|----------------|--------------|----------------------------|
| **Solo Teacher (P1)** | (deferred to Phase 2 P1 cells, GAP-281) | 1 môn, 1 teacher, parent-direct comm |
| **Small Center (P2)** | [`secondary/student-in-P2.md`](secondary/student-in-P2.md) | 1-3 môn, Zalo-primary, parent-only setup cho tiểu học |
| **Medium Center (P3)** | [`secondary/student-in-P3.md`](secondary/student-in-P3.md) | 2-5 môn cùng lúc, multi-class unified UX, parent daily-digest option |
| **K-12 School (P5)** | [`secondary/student-in-P5.md`](secondary/student-in-P5.md) | 12-15 môn, period-based schedule, GVCN primary contact, formal MOET artifacts (học bạ + bằng tốt nghiệp), child protection mandates |

This Tier-1 doc captures the **canonical AC** (what every student needs regardless of tenant); secondary docs ADD tenant-specific constraints WITHOUT duplicating canonical text. AC-FIN-001 wording in particular flows verbatim from `secondary/student-in-P2.md` to maintain calibration consistency.

### 8 Journey areas (Tier-1, mobile-PWA primary)

Per `kiteclass-student/` kit (Round 3 SHIPPED 2026-05-05) + cross-cuts với 4 secondary docs:

1. **Today** — home screen, next-class context, today's schedule, pending tasks, attendance streak
2. **My Classes** — enrolled list (1-15 lớp tùy tenant) với chips filter, search, favorites
3. **Assignment workflow** — view, submit, saved-draft model, deadline tracking, status (chưa/đã nộp/quá hạn)
4. **Grades** — self-tracking + GPA + Học lực (Tốt/Khá/TB/Yếu per Thông tư 22/2021) + parent visibility + GVCN comment
5. **Attendance** — self-view (read-only) + teacher mark + streak hero + period-granular cho P5
6. **Notifications** — throttled (daily digest cho parent), parent-kép visualization, in-app inbox + Zalo cross-promo
7. **Profile** — basic info read-only (DOB/CCCD locked cho minor), avatar preset library, settings (theme/language/notifications), logout
8. **Payment fees** — READ-ONLY for K-12 — payment via parent-trigger workflow (AC-FIN-001 cấm "Pay" button cho student under-18)

### Critical concerns (top 6 — driving AC selection)

1. **Parent-paired account creation** — Student KHÔNG self-signup; account created via bulk import (xlsx) hoặc parent consent grant; first-login UX phải simple (no CCCD/email validation phức tạp)
2. **READ-ONLY fees access** — Cấm "Pay" button cho minor; payment workflow chỉ qua parent-trigger; child-protection lock per AC-FIN-001
3. **Parent-reset password** — Forgot password recovery phải qua parent (cho <16) hoặc GVCN tại trường; KHÔNG email-based reset (PII risk + parent-not-aware)
4. **Notification throttling + parent-kép** — Both student + parent receive critical updates; daily-digest option cho parent ở scale multi-class; tránh "spin" câu chuyện kỷ luật
5. **Anti-fraud attendance** — Student CHỈ view attendance (read-only); teacher marks; cấm self-mark (common abuse pattern at all scales)
6. **No off-platform DM với teachers** — All comm goes through GVCN/owner channel hoặc parent-CC'd thread per child-protection-policy.md §4.2 (preventing grooming risk)

### Out-of-scope for Tier-1 S-student.md (covered in tenant-context docs OR future phases)

- ❌ **Tenant-specific multi-class UX** (P3 5-môn unified gradebook, P5 period-based schedule) → see `secondary/student-in-P3.md` + `secondary/student-in-P5.md`
- ❌ **MOET formal artifacts** (học bạ TT 22/2021 Phụ lục I + bằng tốt nghiệp Phụ lục II) → P5 only, see `secondary/student-in-P5.md`
- ❌ **Period-based attendance** (Tiết 1-10/ngày) → P5 only
- ❌ **Conduct grade hạnh kiểm formal MOET process** → P5 only
- ❌ **GVCN homeroom concept** → P5 only (P1/P2/P3 dùng Owner/Teacher direct)
- ❌ **Adult student direct payment** (vocational + university 18+) → out-of-scope this phase per personas-catalog Tier-1 spectrum
- ❌ **Multi-tenant student transfer giữa centers** → out-of-scope toàn product
- ❌ **Solo Teacher (P1) tenant context** → deferred Phase 2, GAP-281

---

## AC Categories (5 standardized — adapted for cross-tenant student persona)

Standard 6 categories adapted: kept Onboarding / Daily Ops (split into Content + Notif theme groups for clarity) / Financial / Edge Cases. Communication merged into Notifications (NOTIF) since student comm is primarily notification-receiving (not bidirectional like P5 adult personas). Exit/Termination follows tenant context (deferred to secondary/* docs).

Each AC: **AC-{CATEGORY}-{NUM}** + Statement + Test + Fail signal + Status + Linked gap.

---

## 1. Onboarding AC (AC-ONBOARD-*)

Initial account creation → first login → profile bootstrap. Account NEVER self-created — parent-paired or tenant-import.

- [ ] **AC-ONBOARD-001:** Student nhận credentials qua Zalo/SMS (gửi tới parent phone primary, student phone secondary nếu có), first login ≤3 phút từ smartphone
  - **Test:** Owner/lễ tân/IT trường bulk-import students → parent phone nhận Zalo invite chứa username + temporary password + link → tap link → mở mobile login screen → input credentials → land trên student dashboard hiển thị "Hôm nay học gì"
  - **Fail signal:** Yêu cầu student self-signup (sai tuổi pháp lý cho <13), credentials gửi qua email mà parent không check, link login break trên mobile, dashboard empty không có CTA
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md), [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-ONBOARD-002:** Student profile linked với parent contact ngay từ creation — parent phone bắt buộc, parent name optional, emergency contact secondary
  - **Test:** Sau bulk import / parent-grant, mở student profile → field "Parent phone" populated từ xlsx; "Parent name" optional → khi vắng học hoặc cần reset password, system biết notify đúng số phone của parent legal guardian
  - **Fail signal:** Parent contact không bắt buộc (cho phép tạo student không có guardian — vi phạm child protection), parent phone field không validate VN format, không link với child-protection rules
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-ONBOARD-003:** First-login wizard ≤3 bước, hỗ trợ student dưới 13 tuổi (parent có thể setup hộ trên cùng device)
  - **Test:** Lần đầu login → step 1: confirm tên + DOB; step 2: chọn avatar từ preset library (KHÔNG upload — privacy); step 3: see today's schedule. Toàn bộ ≤2 phút.
  - **Fail signal:** Wizard >5 bước, yêu cầu upload photo (vi phạm child protection), không có preset avatar, parent không thể setup hộ
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-ONBOARD-004:** Simplified TOS-for-minor + parental consent gate cho student <13 tuổi (per PDPL Art 16 + Luật Trẻ em 2016)
  - **Test:** Student lần đầu accept TOS → font lớn, ngôn ngữ học sinh hiểu (5 dòng key points), không legalese >1000 từ; nếu student <13 tuổi → wizard force "Parent consent required" → parent receive Zalo with consent link → parent approve → student account activated; consent record audit-logged with timestamp
  - **Fail signal:** TOS dùng legalese không simplified-for-minor (HS không hiểu), không có consent gate cho <13 tuổi (vi phạm child protection + PDPL), consent record không stored (không thể chứng minh khi PDPL audit)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md), [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md)

---

## 2. Content AC (AC-CONTENT-*) — Assignment, Grade, Attendance access patterns

Daily content workflows — view schedule, submit BTVN, view grades read-only, view attendance read-only. Saved-draft model cho assignment submission. Anti-fraud: student CHỈ view, teacher marks.

- [ ] **AC-CONTENT-001:** Student xem lịch tuần (1-15 môn tùy tenant) trên 1 màn hình mobile, ≤2 lần tap từ home; default to "today" view
  - **Test:** Open app → Home → tap "Lịch tuần" → see grid 7 ngày × buổi (P2: 1-3 môn; P3: 2-5 môn color-coded; P5: 5-10 tiết/ngày period-granular) → tap 1 buổi để xem detail (giáo viên, phòng, status); swipe để xem ngày khác; "next class" highlight visible từ home
  - **Fail signal:** Phải tap >2 lần để thấy lịch hôm nay, lịch không hiển thị multi-class trên 1 view (P3/P5), không có "next class" highlight, calendar UX desktop-style trên mobile, swipe lag trên entry-level smartphone
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-053](../../04-quality/gaps/GAP-053-academic-year.md), [GAP-060](../../04-quality/gaps/GAP-060-period-attendance.md) (P5 only)

- [ ] **AC-CONTENT-002:** Student xem attendance history read-only — KHÔNG cho phép student tự mark (anti-fraud invariant cross-tenant)
  - **Test:** Vào "Attendance" tab → see last 30 days với mỗi buổi: Có mặt / Vắng / Đi muộn (do giáo viên mark) → student CHỈ có quyền view, không có button "Mark present" hay edit; aggregation tuần / tháng / kỳ visible; phân biệt vắng có phép vs không phép
  - **Fail signal:** Student có thể tự mark/edit attendance (anti-fraud violation), không có history view, dữ liệu attendance không real-time với teacher's input, không phân biệt 3 trạng thái (just present/absent)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-CONTENT-003:** Student nhận + submit BTVN saved-draft model — view assignment, save draft offline, submit khi có mạng; multi-môn feed chronological
  - **Test:** Sau buổi học → giáo viên post note "HW: làm trang 45-46 SBT, nộp Thứ 6" → student thấy notification + entry trong "Bài tập" tab; tap detail → input answer / upload file / chỉ self-report "Đã làm" tùy scope; offline → save draft locally; online sau → auto-submit; chronological feed across all enrolled classes
  - **Fail signal:** Không có homework view; yêu cầu upload file mọi lúc (P2 không cần); không có notification khi giáo viên post; submit fail không retry / mất data khi offline; per-class navigation only (forget homework cho class này vì chỉ check class kia)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md) (P3+ multi-class context)

- [ ] **AC-CONTENT-004:** Student xem điểm môn học read-only — gradebook view với GPA + Học lực + breakdown Thông tư 22/2021 weighting (TX hệ số 1 + GK hệ số 2 + CK hệ số 3); publishing window respect
  - **Test:** Giáo viên nhập điểm → publishing approval workflow (Tổ trưởng/Phó CM duyệt cho P5; Owner duyệt cho P1/P2/P3) → 24h sau student mở "Điểm" tab → thấy entry mới với scale 10 + weighted average preview + GPA hero + Học lực pill (Giỏi/Khá/TB/Yếu); HS không thể edit; KHÔNG thấy điểm trước approval (anti-leak)
  - **Fail signal:** HS thấy điểm ngay sau giáo viên nhập (vi phạm publishing approval — leak điểm cho HS khác), công thức ĐTBm sai, UI không phân biệt TX/GK/CK (HS confused), không show partial calculation, student có quyền edit điểm
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md), [GAP-055](../../04-quality/gaps/GAP-055-grade-report-card.md) (P5 formal report card extension)

- [ ] **AC-CONTENT-005:** Student xem class material library per-class (handouts giáo viên upload) — read-only, KHÔNG yêu cầu upload từ student
  - **Test:** Class detail → "Tài liệu" tab → list 5-10 PDFs/links giáo viên đã share → tap để view inline (không force download); mobile-friendly viewer; no force-upload from student side
  - **Fail signal:** Không có library view; force download large PDFs trên mobile data; viewer broken trên mobile; require upload từ student (out-of-scope cho student persona — content is teacher-curated)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 3. Financial AC (AC-FIN-*) — READ-ONLY fees access, child-protection lock for under-18

Student NEVER pays directly (parent-mediated for under-18); student VIEWS fee status để biết. **AC-FIN-001 wording is the canonical anchor** — this Tier-1 doc preserves verbatim from `secondary/student-in-P2.md` line 118 to prevent calibration drift across kit reviews + Track 2 ports.

- [ ] **AC-FIN-001:** Student xem fee status read-only (parent đã đóng tháng X chưa) — không có button "Pay" cho student
  - **Test:** Student mở "Học phí" tab → thấy "Tháng 4: ✅ Đã đóng (1M VND, ngày 5/4)" hoặc "Tháng 5: ⏳ Chưa đóng (hạn 5/5)"; KHÔNG có "Pay now" button cho student
  - **Fail signal:** Student có thể trigger payment (vi phạm tuổi pháp lý ký), không có fee status view, status không sync real-time với parent's payment record
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-FIN-002:** Parent-trigger payment workflow — khi student xem khoản nợ, có CTA "Yêu cầu ba/mẹ đóng" thay vì "Pay now"; trigger gửi notification tới parent's app/Zalo
  - **Test:** Student xem "Tháng 5: ⏳ Chưa đóng (hạn 5/5)" → tap khoản nợ → CTA "Yêu cầu ba/mẹ đóng" (NOT "Pay") → confirm → notification sent tới parent qua app + Zalo + email; student app hiển thị chip "Đã gửi yêu cầu — chờ ba/mẹ xác nhận"; sau parent thanh toán → status auto-update + notification trả về student
  - **Fail signal:** Student có nút "Pay" trực tiếp (vi phạm parent-mediated payment + tuổi pháp lý cho under-18), parent-trigger CTA missing, không có status feedback chip cho student sau khi gửi yêu cầu, parent không nhận notification trigger
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), Related: [GAP-363](../../04-quality/gaps/GAP-363-kiteclass-student-polish.md) (kit payments.html Option C rebuild)

- [ ] **AC-FIN-003:** Multi-fee breakdown visibility (K-12 spectrum) — student xem aggregated total + per-fee breakdown (HP, bán trú, đồng phục, BHYT, BHTN, quỹ PH) per tháng / kỳ; KHÔNG thấy credit card / bank info của parent (PII protection minor)
  - **Test:** Student mở "Học phí" tab → list các khoản: "HP tháng 10: 300k ✅", "Bán trú tháng 10: 500k ✅", "Đồng phục năm: 800k ⏳ Còn nợ 200k"; KHÔNG thấy CC/bank/wallet PII của parent; aggregated total + per-fee breakdown hiển thị clear
  - **Fail signal:** Không hiển thị multi-fee breakdown (HS chỉ thấy total → không hiểu cụ thể), thấy CC/bank PII (vi phạm PDPL Art 16 — minor không nên thấy adult PII), không có status sync real-time với parent's payment record
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** Related: pricing-model.md (multi-fee), privacy-policy.md (PII masking minor view), [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md)

---

## 4. Notification AC (AC-NOTIF-*) — Parent-kép visualization, throttling

Multi-channel notification: in-app push primary + Zalo parent-kép + email fallback. Daily-digest option ở scale multi-class. NO off-platform DM với teachers per child-protection-policy.md.

- [ ] **AC-NOTIF-001:** Student nhận notification kép (cả student VÀ parent receive) cho events: lịch đổi, lớp nghỉ, bài tập mới, điểm mới, hạnh kiểm change
  - **Test:** Owner/teacher reschedule "Toán T4 19h" → student's app + Zalo + parent's app + Zalo cả 2 nhận message "Bé A: lịch Toán đổi sang T7 9h" trong vòng 5 phút (tone formal cho parent, friendly cho student); cả 2 channel sync read-receipt
  - **Fail signal:** Chỉ parent nhận (student missed); chỉ student nhận (parent ngạc nhiên); message không localize VN; delay >15 phút; không có channel choice; tone không adapt persona (parent vs student)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-NOTIF-002:** Daily-digest option cho parent ở scale multi-class (P3 3-5 môn, P5 12-15 môn) để tránh spam; student vẫn nhận real-time per-event
  - **Test:** Cả 5 giáo viên (P3 scale) post events trong 1 ngày → student nhận 5 separate Zalo real-time + parent có option (default ON cho P3+, OFF cho P2) nhận 1 daily digest "Bé A có 5 cập nhật hôm nay: ..." tổng hợp lúc 8h tối; parent có thể switch sang real-time nếu muốn
  - **Fail signal:** Parent bị spam 5 messages tách biệt (UX kém ở scale 5 môn); student missed messages; daily digest không có option; tone không VN-localized; digest delivery time không configurable
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-NOTIF-003:** Student in-app inbox aggregated across all enrolled classes + filter by class + sender attribution clear
  - **Test:** Inbox view → all messages chronological; filter dropdown "All / per-môn"; mỗi message có "From: Cô A (môn X)" + timestamp; mark read syncs với Zalo read receipt; persistent (không xóa tay được — audit trail)
  - **Fail signal:** Per-class inbox only (lost messages cross-class); no filter; sender attribution missing; sync với Zalo broken; student có thể delete (mất evidence)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-NOTIF-004:** Student-teacher direct messaging BANNED outside platform/parent oversight — chỉ qua broadcast hoặc parent-CC'd thread (per child-protection-policy.md §4.2)
  - **Test:** Student app KHÔNG có "DM giáo viên" button; ask question → click "Gửi câu hỏi" → form submit kèm parent_phone CC + class context; tất cả teacher response visible trong cả student inbox VÀ parent inbox; cấm GV bộ môn DM riêng student
  - **Fail signal:** Có DM trực tiếp giáo viên không có parent visibility (vi phạm child protection); không có audit trail; parent không CC'd; cross-teacher coordination không log (security risk khi student × multi-teacher exposure)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-NOTIF-005:** Parent-mediated incident notification — khi GVCN/owner log incident về student (kỷ luật, hạnh kiểm, transfer), notification gửi cả student + parent đồng thời; student không thể "hide" thông báo khỏi parent view
  - **Test:** GVCN log incident "HS A đánh bạn 15/10" → workflow: notification gửi parent (qua app PH + Zalo + email) + gửi student (qua app); student thấy "Đã thông báo phụ huynh về sự việc 15/10. Em sẽ trao đổi sớm." → student không có nút "Xóa" / "Đánh dấu spam"; chỉ "Đã đọc"; parent receive đồng thời và thấy timeline đầy đủ
  - **Fail signal:** Student có thể delete/hide notification từ parent view (vi phạm Luật GD Đ.83 — parent has right to know), parent không nhận khi student bị kỷ luật (parent bypass), thông báo không sync timing (student biết trước parent → có thể "spin" câu chuyện)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

---

## 5. Edge Case AC (AC-EDGE-*) — Forgot password parent-reset, account lockout, child-safety

Edge scenarios: forgot password parent-reset, account lockout escalation, child safety incident report, account compromise emergency lockout.

- [ ] **AC-EDGE-001:** Student quên password — parent là người reset (KHÔNG phải student tự reset qua email) — tránh self-recovery loophole với minor
  - **Test:** Student không login được → app suggest "Nhờ ba/mẹ reset" → parent mở Zalo nhận magic link → click → reset password new → push tới student device → student login lại
  - **Fail signal:** Student có thể reset qua email mà parent không biết (vi phạm parental control); reset flow không gửi parent notification; bypass thông qua security questions không có parent verification
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-EDGE-002:** Account lockout escalation — sau 5 failed login attempts, account lockout 15 phút + parent notification; reset chỉ qua parent hoặc GVCN tại trường (P5)
  - **Test:** Student fail login 5 lần → account locked 15 phút → message "Tài khoản tạm khóa. Vui lòng nhờ ba/mẹ reset hoặc thử lại sau 15 phút." → parent nhận push "Tài khoản con bị thử login 5 lần lúc 14:30" để biết → lockout audit log preserved 1 năm
  - **Fail signal:** Không có lockout (brute-force risk), parent không được notified (vi phạm parental control + security awareness), audit log không có (không thể investigate khi account compromise), lockout duration quá ngắn (<5 phút) hoặc quá dài (>1 giờ — block legitimate use)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md), [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md)

- [ ] **AC-EDGE-003:** Account compromise emergency lockout — student hoặc parent có thể trigger emergency lockout khi nghi ngờ session hijack; audit log event preserved 1 năm
  - **Test:** Student bỗng thấy app hiển thị thông báo lạ → "Account Activity" tab thấy login lạ từ IP xx.xx.xx.xx → tap "This wasn't me" → confirm với parent password (parental authority for minor account) → emergency lockout: tất cả sessions force-logout, password auto-reset, notification GVCN/Owner + IT staff; audit log lưu 1 năm cho investigation
  - **Fail signal:** Không có account activity view (student không biết hack), student có thể tự lockout không cần parent (risk false-positive khi student giận dỗi), không có audit log preservation (mất evidence cho công an khi cần)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md), [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md)

- [ ] **AC-EDGE-004:** Child safety incident report — student có channel báo bullying / grooming / abuse qua app; ticket priority CRITICAL, encrypted, only safeguarding officer + Hiệu trưởng/Owner + designated counselor see; mandatory reporting per Luật Trẻ em 2016 Đ.51 nếu có CSAM
  - **Test:** Student mở app → "Báo cáo an toàn" (button visible từ home screen, không bị nested) → form: "Em đang gặp vấn đề gì? [bị bắt nạt / bị đe dọa / bị quấy rối / khác]" → describe sự việc → optional: upload evidence (screenshot/photo) → submit → ticket priority CRITICAL với tag "Child safety" → notification gửi safeguarding officer + Hiệu trưởng/Owner + counselor (KHÔNG gửi GVCN/teacher nếu student chọn "GVCN có thể là người liên quan" để tránh retaliation); evidence preserved encrypted; student nhận confirmation "Tin nhắn đã được nhận và sẽ được xử lý bí mật trong 24h"
  - **Fail signal:** Không có safety report channel (student không có cách an toàn để báo); report đi qua GVCN/teacher (vi phạm trường hợp GVCN/teacher là kẻ vi phạm); không encrypted (PII leak nếu hệ thống bị hack); không có mandatory reporting suggestion cho CSAM (vi phạm Luật Trẻ em 2016 Đ.51 + Decree 56/2017)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md), Related: child-protection-policy.md §4.4 mandatory reporting

- [ ] **AC-EDGE-005:** Student nghỉ ốm/báo nghỉ qua app — parent submit, student VIEW status; makeup plan visible nếu owner/teacher schedule bù
  - **Test:** Parent mở "Báo nghỉ" → input lý do "Ốm 2 ngày" + dates → submit → student app hiển thị "Vắng có phép T2-T3" + "Buổi makeup: T7 14h (nếu có)"; teacher's attendance pre-marked "Vắng có phép"; parent nhận confirmation
  - **Fail signal:** Student có thể tự báo nghỉ (vi phạm parental authority cho minor), makeup plan không hiển thị, attendance không pre-mark, parent không nhận confirmation
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## Scoring

**Total ACs:** 21 (sum across 5 categories: 4 Onboard + 5 Content + 3 Fin + 5 Notif + 5 Edge)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / 21 × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ S. Student fully supported (production-ready for student-facing UX cross-tenant) |
| 60-84% | ⚠️ S. Student partially supported (usable but with friction; defer student-facing GA) |
| 30-59% | 🔴 S. Student NOT supported (major UX gaps; not production-ready) |
| <30% | ❌ S. Student NOT viable (fundamental misfit) |

**Pre-review baseline:** Inherits cross-tenant baseline — student-specific gaps likely in: AC-FIN-002 (parent-trigger payment workflow — Wave 22 GAP-363 addresses), AC-NOTIF-002 (daily-digest option), AC-EDGE-004 (child safety report), AC-EDGE-001 (parent-reset password).

---

## Gap Linkage Summary

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md), [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | 🔵 OPEN | P0 |
| AC-ONBOARD-002 | TBD | [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | 🔵 OPEN | P0 |
| AC-ONBOARD-003 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | 🔵 OPEN | P0 |
| AC-ONBOARD-004 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md), [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) | 🔵 OPEN | **P0 LEGAL** |
| AC-CONTENT-001 | TBD | [GAP-053](../../04-quality/gaps/GAP-053-academic-year.md), [GAP-060](../../04-quality/gaps/GAP-060-period-attendance.md) | 🔵 OPEN | P0 |
| AC-CONTENT-002 | TBD | — | — | P1 |
| AC-CONTENT-003 | TBD | [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md) | 🔵 OPEN | P0 |
| AC-CONTENT-004 | TBD | [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md), [GAP-055](../../04-quality/gaps/GAP-055-grade-report-card.md) | 🔵 OPEN | P0 |
| AC-CONTENT-005 | TBD | — | — | P2 |
| AC-FIN-001 | TBD | — (canonical anchor) | — | **P0 LEGAL** |
| AC-FIN-002 | TBD | [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-363](../../04-quality/gaps/GAP-363-kiteclass-student-polish.md) | 🔵 OPEN | P0 |
| AC-FIN-003 | TBD | [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md) | 🔵 OPEN | P1 |
| AC-NOTIF-001 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | 🔵 OPEN | P0 |
| AC-NOTIF-002 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | 🔵 OPEN | P1 |
| AC-NOTIF-003 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | 🔵 OPEN | P1 |
| AC-NOTIF-004 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | 🔵 OPEN | **P0 LEGAL** |
| AC-NOTIF-005 | TBD | [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-001 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-002 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md), [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) | 🔵 OPEN | P1 |
| AC-EDGE-003 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md), [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) | 🔵 OPEN | P1 |
| AC-EDGE-004 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | 🔵 OPEN | **P0 LEGAL** |
| AC-EDGE-005 | TBD | — | — | P2 |

**Legal-mandate ACs (LEGAL tag):** 6 / 21 — reflect Luật Trẻ em 2016 + PDPL Art 16 + child-protection-policy.md cross-cuts. These cannot ship "PARTIAL" for K-12 student-facing GA.

**New gaps surfaced (likely candidates at GAP-152 Round 1):**
- Parent-trigger payment workflow UX standard (AC-FIN-002) — currently only kit-level mockup in GAP-363 closure; needs cross-tenant standard
- Daily-digest delivery scheduling (AC-NOTIF-002) — config option per tenant
- Child safety report visibility-to-home-screen design (AC-EDGE-004) — UX prominence requirement

---

## Cross-References

- **Persona catalog:** [`../personas-catalog.md`](../personas-catalog.md) §"Tier 1 Primary" + §"Secondary Personas — Student"
- **Tenant-context extension docs (secondary):**
  - [`secondary/student-in-P2.md`](secondary/student-in-P2.md) — Small Tutoring Center context (1-3 môn, Zalo-primary, parent-only setup tiểu học)
  - [`secondary/student-in-P3.md`](secondary/student-in-P3.md) — Medium Education Center context (2-5 môn unified, daily-digest, multi-teacher gradebook)
  - [`secondary/student-in-P5.md`](secondary/student-in-P5.md) — K-12 School context (12-15 môn, period-based, GVCN, formal MOET artifacts, child protection mandates)
- **Sister Tier-1 persona docs:**
  - [`P1-solo-teacher.md`](P1-solo-teacher.md) — Solo Teacher tenant
  - [`P2-small-center.md`](P2-small-center.md) — Small Tutoring Center tenant
  - [`P3-medium-center.md`](P3-medium-center.md) — Medium Education Center tenant
  - [`P5-k12-school.md`](P5-k12-school.md) — K-12 School tenant (USER PRIORITY)
- **Folder index:** [`README.md`](README.md)
- **Template:** [`_TEMPLATE.md`](_TEMPLATE.md)
- **UI realization (kit):** [`../../02-architecture/design-system/ui_kits/kiteclass-student/README.md`](../../02-architecture/design-system/ui_kits/kiteclass-student/README.md) — Round 3 mobile-PWA prototype 116/128 avg
- **Business rules (parent + child protection):**
  - [`../../01-business/kiteclass/parent-portal/rules.md`](../../01-business/kiteclass/parent-portal/rules.md) — parent-kép visualization + consent BR-PARENT-PORTAL-* + parent-trigger payment workflow
  - [`child-protection-policy.md`](child-protection-policy.md) (when filed) — mandatory reporting, no off-platform DM, recording 1-to-1, parental consent
  - [`privacy-policy.md`](privacy-policy.md) (when filed) — PDPL Art 16 minor data + parental consent
- **Review skill:** [`../../../.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
- **Audit pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check
- **Cross-linked gaps (8 total):** GAP-051 (bulk import), GAP-052 (parent portal), GAP-053 (academic year), GAP-054 (multi-subject), GAP-055 (báo cáo MOET P5), GAP-060 (period attendance P5), GAP-063 (SMS/Zalo notification), GAP-184 (data retention sensitive-minor), GAP-186 (child protection — **CRITICAL**), GAP-363 (kit polish — payments Option C)
- **Legal citations:** Luật Trẻ em 2016 (Đ.51 mandatory reporting), PDPL Decree 13/2023 Art 16 (parental consent + minor data), Luật Giáo dục 2019 Đ.83 (parent monitoring rights)

---

## Reviewer Hat (Phase 2 — for GAP-152 Round 1 multi-stakeholder review)

| Reviewer role | Critical responsibility | Sample stakeholder |
|---------------|------------------------|--------------------|
| **Real student rep — 3 age bands** | Validate UX simplicity (cấp 1-2 typing, mobile-first, 3G performance, emoji-friendly) | HS lớp 4 + lớp 7 + lớp 11, mobile-only access |
| **Real parent rep** | Validate parent-mediated payment + parental consent flow + AC-EDGE-001 password recovery + AC-NOTIF-005 incident notification sync | Phụ huynh có 2+ con khác age bands |
| **GVCN/teacher rep** | Validate AC-NOTIF-001..005, AC-CONTENT-002 anti-fraud attendance, AC-EDGE-005 absence report flow | Tổ trưởng GVCN khối 7 + giáo viên trung tâm vừa |
| **Legal counsel (child protection)** | Validate AC-NOTIF-004 (no off-platform DM), AC-EDGE-004 (mandatory reporting), AC-ONBOARD-004 (TOS-minor + consent), AC-FIN-001 (no Pay button minor) | Luật sư trẻ em + Luật Trẻ em 2016 expert |
| **Product Owner (KiteClass)** | Cross-cut với secondary docs — identify shared canonical AC vs tenant-specific overrides | @nguyenvankiet acting PO |

**Review process estimate:** 4-5 days (21 ACs × 5 stakeholders, focused canonical scope; secondary/* docs reviewed separately for tenant-context details).

---

## Anti-Patterns (specific to S. Student persona)

| ❌ Don't | ✅ Do |
|---------|------|
| Treat student like adult user (free-form chat, email reset, direct payment) | Recognize minor-specific constraints: parental consent, no off-platform DM, parent-mediated payment, parent-reset password |
| Use proxy AC from secondary/student-in-P2.md without crediting Tier-1 source | Cite this S-student.md as canonical; secondary docs ADD tenant-context, KHÔNG re-state canonical |
| Skip parent visibility cho student communications | AC-NOTIF-001 + AC-NOTIF-005 mandate parent-kép per Luật GD Đ.83 + child-protection-policy.md |
| Hardcode age-band constraints (e.g. "student >13 luôn được self-reset") | Recognize 4 age bands (tiểu học / THCS / THPT / vocational) với distinct constraints; default to most restrictive (parent-reset) for under-16 |
| Generic TOS cho minor accounts | Simplified TOS-for-minor (AC-ONBOARD-004) per child-protection-policy.md |
| Free-form DM giữa student với teacher | Class-level announcements only, 1-to-1 chỉ qua GVCN/owner với recording option (P5) |
| Email-based password reset cho student <16 | Parental + GVCN/owner-mediated recovery (AC-EDGE-001) per PDPL Art 16 |
| Student có nút "Pay" trong app | Parent-trigger workflow only — student view-only AC-FIN-001 |
| Treat tenant context as overrides on Tier-1 doc | Tier-1 = canonical cross-tenant AC; secondary/* = tenant-specific delta. Both shipped, neither replaces other |

---

## Notes for Reviewer (Phase 2 — Domain Expert)

### Tenant-context routing — when to use which doc

When reviewing a kit / Track 2 port / feature gap, ALWAYS:

1. **Start with this Tier-1 doc** (S-student.md) — canonical AC apply to ALL tenant types
2. **Cross-reference secondary doc** matching the tenant context being reviewed:
   - Solo Teacher tenant → defer to GAP-281 (Phase 2 P1 cells)
   - Small Center tenant → `secondary/student-in-P2.md` (1-3 môn, parent-only tiểu học)
   - Medium Center tenant → `secondary/student-in-P3.md` (2-5 môn unified, multi-teacher)
   - K-12 School tenant → `secondary/student-in-P5.md` (12-15 môn, MOET formal artifacts, GVCN, child protection mandates **CRITICAL**)
3. **Calibration drift check:** Verify AC-FIN-001 wording matches verbatim across this doc + secondary/student-in-P2.md (canonical anchor); if drifted, file gap to re-sync

### Edge for review

- **Vocational + university (lớp 12+ adult):** Adult tự consent, có thể direct pay; deferred this phase per personas-catalog Tier-1 spectrum; reviewer may flag for GAP-282 expansion if pattern phổ biến
- **Multi-tenant student transfer:** Out-of-scope toàn product (e.g. học sinh chuyển từ trung tâm này sang trung tâm khác); each tenant's S. Student account is independent
- **Cross-age-band families:** Khi parent có nhiều con ở age bands khác nhau (cấp 1 + cấp 3), parent app phải handle cả 2 modes (full-mediated cho con nhỏ, view-monitor cho con lớn) — out-of-scope this S-student.md, see `secondary/parent-in-P5.md`

### Out-of-scope confirmed

- Solo Teacher (P1) tenant context (deferred Phase 2 GAP-281)
- Adult student direct payment (vocational + university 18+, deferred GAP-282)
- Multi-tenant student transfer giữa centers (out-of-scope toàn product)
- MOET formal artifacts detail (P5 only, see `secondary/student-in-P5.md`)
- Period-based attendance UX detail (P5 only, see `secondary/student-in-P5.md`)
- Conduct grade hạnh kiểm formal MOET process (P5 only)

---

## How to Use This Doc

1. **Phase 1 (now — 2026-05-06):** Tier-1 AC framework drafted (this file v1, Wave 22 Bucket C agent, GAP-365 Phase 1)
2. **Phase 2 (GAP-152 Round 1 review):** 5 stakeholders fill Status (PASS/PARTIAL/FAIL); new gaps filed for FAIL items not matching existing GAP
3. **Phase 3 (post-review):** ROADMAP updated; coverage % computed; if <60% → block student-facing GA cross-tenant; secondary/* docs reviewed separately for tenant-context delta
4. **Phase 4 (quarterly re-review):** Re-score sau wave fix related gaps; track delta against secondary/* docs coverage; re-check Luật Trẻ em / PDPL amendment, MOET TT 22/2021 grade formula updates

**This doc reviewed every quarter** (per `business-logic-review.md` Quarterly cadence).

---

## Log

- **2026-05-06** — Initial Tier-1 AC set v1 created by Wave 22 Bucket C agent (GAP-365 Phase 1). 21 ACs across 5 categories (Onboarding 4 + Content 5 + Financial 3 + Notification 5 + Edge 5; Communication merged into Notification due to student comm being primarily notification-receiving). Sources: secondary/student-in-P2.md (canonical AC-FIN-001 verbatim anchor) + secondary/student-in-P3.md (multi-class UX) + secondary/student-in-P5.md (USER PRIORITY K-12 + child protection LEGAL ACs) + `kiteclass-student/` Round 3 kit (mobile-PWA primary persona basics) + personas-catalog.md §Secondary Personas. 6/21 ACs marked LEGAL — non-negotiable for student-facing GA. Reframes 3 secondary docs as **tenant-context extensions** (each adds tenant-specific delta, this Tier-1 doc holds canonical cross-tenant AC). 8 GAP cross-links + Luật Trẻ em 2016 + PDPL 2023 + Luật GD 2019 legal citations. Multi-stakeholder Phase 2 review requirement documented (5 reviewer roles).
- **TBD** (2026-Q3 target) — Phase 2 GAP-152 Round 1 review with multi-stakeholder sign-off (3 student reps × age bands + parent rep + GVCN/teacher rep + legal counsel + PO).
