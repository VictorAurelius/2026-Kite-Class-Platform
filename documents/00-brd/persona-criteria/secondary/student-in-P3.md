# Acceptance Criteria — Student in P3 Medium Education Center

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Student × P3
**Persona name (VN):** Học sinh trong trung tâm quy mô vừa
**Persona name (EN):** Student in Medium Education Center
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent A (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real student/parent representative + Product Owner sign-off (deferred to GAP-152 Round 1 review)
**Tier:** 1 Primary (Tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152 (Round 1 review) → GAP-281 (P1 cells follow-up if needed)
**Tenant context:** Medium Education Center (P3)
**Role:** Student (secondary persona)

---

> **Extends Tier-1** [`S-student.md`](../S-student.md) **with P3 medium-center tenant-context overrides.** This doc preserves P3-specific journey variations (2-5 môn unified UX, multi-teacher gradebook, parent daily-digest option, multi-class enrollment + transfer); for canonical persona AC see Tier-1 doc.

---

## 0. Context

### Persona profile

- **Tenant:** Medium Education Center (P3) — 100-500 students, 5-20 teachers, 10-50 classes, 3-5 admin staff (giám đốc + lễ tân + kế toán + ops)
- **User:** Học sinh THCS (lớp 6-9) hoặc THPT (lớp 10-12) hoặc tiểu học (lớp 3-5) tùy center; some centers also có lớp luyện thi đại học/IELTS
- **Age band:** 9-18 tuổi, parent-mediated cho lớp tiểu học/THCS, semi-independent từ lớp 10 trở lên
- **Device:** Smartphone cá nhân (lớp 8 trở lên thường có), kèm parent device cho lớp dưới
- **Subjects per student:** 2-5 môn cùng lúc (e.g. Anh + Toán + Khoa học), enrollment vào multiple classes/groups song song
- **Multi-class enrollment:** điểm distinguishing với P2 — student có thể ở "Anh nâng cao 9A" + "Toán cơ bản 9C" + "Sciences STEM" cùng học kỳ, mỗi lớp khác giáo viên + khác lịch

### Key journeys (Student perspective)

1. **Onboarding:** Lễ tân enroll → credentials gửi qua Zalo (parent + student) → first login → consent flow nếu <13 tuổi → schedule overview
2. **Daily:** Mobile app → "Today" widget hiển thị tất cả buổi của 3-4 môn → tap môn xem detail → đến lớp → multi-teacher gradebook
3. **Multi-class:** Lịch tuần tích hợp 3-5 lớp → tránh conflict với teacher khác (system flag); parent-coordinator view nếu sibling cùng học trung tâm
4. **Communication:** Zalo notification kép → in-app inbox → parent-CC mọi DM với teacher (child protection)
5. **Edge:** Quên password → parent reset; chuyển từ lớp này sang lớp khác trong cùng môn (e.g. Anh cơ bản → Anh nâng cao); nghỉ ốm → makeup plan multi-class
6. **Exit:** Hoàn thành học kỳ + parent withdraws hoặc graduate khỏi center → data retention per PDPL Art 16 minor

### Critical concerns (real student/parent voice)

1. **Multi-class schedule view** — distinguishing con vs P2: student cần unified view across 3-5 môn, không per-class navigation
2. **Multi-teacher gradebook unified** — student không phải mở 5 nơi để xem điểm
3. **Parent communication coordination** — 1 student × 5 teachers → parent dễ bị spam; cần aggregated daily digest option
4. **Attendance per class** (KHÔNG per period như P5) — mỗi buổi 1 class = 1 attendance record
5. **Anti-fraud attendance** — teacher marks, student CHỈ view (giống P2 nhưng scale lớn hơn → critical hơn)
6. **More formal communication tone** — center quy mô vừa thường có pseudo-professional tone vs informal P2

### Out-of-scope for student-in-P3 (covered elsewhere)

- ❌ Formal report card MOET format (bảng điểm THPT bilingual) → P5 K-12 territory (GAP-055)
- ❌ Period-based attendance (Tiết 1-5/ngày) → P5 only (GAP-060)
- ❌ Conduct grade (hạnh kiểm) → P5 only (GAP-059)
- ❌ Homeroom teacher GVCN → P5 only (GAP-056)
- ❌ Promotion/retention logic (lên lớp) → P5 K-12 (GAP-061)
- ❌ Direct payment by minor → parent-mediated for under 16, optional 16+ (out-of-scope this phase)

---

## AC Categories (6 standardized)

Each AC: **AC-{CATEGORY}-{NUM}** + Statement + Test + Fail signal + Status (blank — filled at GAP-152 review) + Linked gap.

---

## 1. Onboarding AC

Student lần đầu nhận account → first login → multi-class setup → ready để dùng daily.

- [ ] **AC-ONBOARD-001:** Student nhận credentials qua Zalo/SMS (cả parent + student phone nếu student có), first login ≤3 phút từ smartphone
  - **Test:** Lễ tân enroll student vào 3 lớp (Anh nâng cao + Toán cơ bản + Sciences STEM) → bulk Zalo invite gửi tới parent + student (nếu student >13 tuổi có phone) → student tap link → input credentials → land trên dashboard hiển thị multi-class schedule
  - **Fail signal:** Yêu cầu student self-signup (sai tuổi pháp lý cho <13), credentials gửi email mà parent/student không check, dashboard chỉ hiển thị 1 lớp (multi-class chưa render), no Zalo channel
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md), [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-ONBOARD-002:** Student profile linked với parent contact + emergency contact secondary; consent flow active cho student <13 tuổi (parent must approve account creation)
  - **Test:** Lễ tân tạo student "C" lớp 7 (12 tuổi) → wizard force "Parent consent required" → parent receive Zalo with consent link → parent approve → student account activated; student "D" lớp 11 (17 tuổi) → consent skip, parent_contact required nhưng student có thể self-confirm
  - **Fail signal:** Không có consent gate cho <13 tuổi (vi phạm child protection + PDPL); parent contact optional ở mọi tuổi; emergency contact field thiếu; consent record không stored cho audit
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-ONBOARD-003:** Multi-class enrollment hiển thị clear trên onboarding wizard — student xem được "Bạn đã được đăng ký 3 lớp: Anh, Toán, Sciences" với schedule preview
  - **Test:** Sau first login → wizard step 2 hiển thị card list 3 môn × giáo viên × time slot → student tap "Confirm" → land vào weekly schedule unified view
  - **Fail signal:** Wizard chỉ show 1 môn (multi-class scope không recognized); hiển thị unsorted/raw data; không có schedule preview; phải navigate per class để verify enrollment
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md)

---

## 2. Daily Operations AC

Multi-class workflows — view multi-class schedule, attend per-class, multi-teacher gradebook, homework receipt per class.

- [ ] **AC-OPS-001:** Student xem lịch tuần unified across 3-5 môn trên 1 màn hình mobile, ≤2 lần tap từ home; conflict detection nếu enrolled overlapping
  - **Test:** Open app → Home → "Lịch tuần" → see grid 7 ngày × time slots với 3 môn color-coded (Anh blue, Toán red, Sciences green); nếu Toán T2 19h trùng Sciences T2 19h → flag "Xung đột" + suggest contact lễ tân
  - **Fail signal:** Phải navigate per class để xem lịch; không color-code; không conflict detection (student có thể trùng 2 lớp mà không biết); calendar UX desktop trên mobile
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-002:** Student xem attendance history per-class read-only (3-5 records mỗi tuần) — KHÔNG cho phép student tự mark
  - **Test:** Vào "Attendance" tab → filter by class → see 30-day attendance for "Anh nâng cao" (8 buổi), "Toán cơ bản" (8 buổi), "Sciences" (4 buổi); each record marked by teacher với timestamp
  - **Fail signal:** Student có thể tự mark/edit; attendance gộp chung không filter được per-class; không real-time sync với teacher input; missing teacher attribution
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-003:** Multi-teacher gradebook unified view — student xem điểm 3-5 môn từ 3-5 giáo viên trên 1 màn hình; per-subject breakdown available với 1 tap
  - **Test:** "Điểm" tab → "All subjects" view: Anh 8.5 (avg), Toán 7.0, Sciences 9.0; tap "Anh" → breakdown "Kiểm tra 15p: 8.5 (weight 20%) + Mid-term: 8.5 (weight 30%) + ..."; mỗi grade entry có teacher name attribution
  - **Fail signal:** Phải navigate per teacher để xem điểm; không unified avg view; teacher attribution missing; weighted average không tính sẵn; student có quyền edit
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-004:** Student nhận homework feed across all enrolled classes — chronological list, không phân tán per class
  - **Test:** "Bài tập" tab → see chronological feed: "Anh: Read p.45-46 nộp T6" + "Toán: SBT trang 12-15 nộp T4" + "Sciences: Lab report nộp Chủ nhật"; tap entry → detail + mark "Đã làm"
  - **Fail signal:** Per-class navigation only (student forget homework cho class này vì chỉ check class kia); no chronological feed; due date không sort; mark "đã làm" trigger workflow phức tạp
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-005:** Student xem class material library per-class (handouts giáo viên upload) — read-only, KHÔNG yêu cầu upload từ student
  - **Test:** "Anh nâng cao 9A" class detail → "Tài liệu" tab → list 5-10 PDFs/links giáo viên đã share → tap để view inline (không force download); mobile-friendly viewer
  - **Fail signal:** Không có library view; force download large PDFs trên mobile data; viewer broken trên mobile; require upload từ student (out-of-scope cho P3 student)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 3. Financial / Admin AC

P3 student KHÔNG trực tiếp pay (parent-mediated cho minor). Student có thể XEM tình trạng học phí across 3-5 môn để biết.

- [ ] **AC-FIN-001:** Student xem fee status read-only across 3-5 môn — aggregated total + per-class breakdown — không có button "Pay" cho student
  - **Test:** "Học phí" tab → see "Tháng 4: 4M VND total — Anh ✅ Đã đóng 1.5M, Toán ✅ Đã đóng 1.2M, Sciences ⏳ Chưa đóng 1.3M (hạn 5/4)"; KHÔNG có "Pay now" button
  - **Fail signal:** Aggregated view không có (per-class only confusing); fee status không sync real-time với parent's payment record; status hiển thị unknown khi parent đã đóng nhưng kế toán chưa reconcile; có "Pay" button cho minor (vi phạm tuổi pháp lý)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 4. Communication AC

Multi-channel; daily digest option để tránh spam khi student × 5 teachers; parent-CC mandatory cho child protection.

- [ ] **AC-COMM-001:** Student nhận Zalo notification (KÉP — cả student VÀ parent) cho events từ tất cả enrolled classes (3-5 lớp); option daily digest cho parent
  - **Test:** Cả 3 giáo viên (Anh, Toán, Sciences) post events trong 1 ngày → student nhận 3 separate Zalo + parent nhận 1 daily digest "Bé A có 3 cập nhật hôm nay: ..." (tổng hợp, hạn 8h tối); parent có thể switch sang real-time nếu muốn
  - **Fail signal:** Parent bị spam 3 messages tách biệt (UX kém ở scale 3-5 môn); student missed messages; daily digest không có option; delay >15 phút; tone không VN-localized
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-COMM-002:** Student in-app inbox aggregated across 3-5 lớp + filter by class; sender attribution rõ ràng
  - **Test:** Inbox view → all messages chronological; filter dropdown "All / Anh / Toán / Sciences"; mỗi message có "From: Cô A (Anh)" + timestamp; mark read syncs với Zalo read receipt
  - **Fail signal:** Per-class inbox only (lost messages cross-class); no filter; sender attribution missing; sync với Zalo broken
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-COMM-003:** Student-teacher direct messaging BANNED — chỉ qua broadcast hoặc parent-CC'd thread; cross-teacher coordination cũng phải có audit trail
  - **Test:** Student app KHÔNG có 1-on-1 DM với giáo viên; "Gửi câu hỏi" form submit kèm parent_phone CC + class context; tất cả teacher response visible trong cả student + parent inbox
  - **Fail signal:** Có DM trực tiếp giáo viên không có parent visibility (vi phạm child protection); no audit trail; parent không CC'd; cross-teacher coordination không log (security risk khi student × 5 teachers)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

---

## 5. Edge Cases AC

P3-specific scenarios: forgot password (multi-class implications), class transfer in same subject (e.g. Anh cơ bản → Anh nâng cao), missed class với multi-class makeup coordination.

- [ ] **AC-EDGE-001:** Student quên password — parent reset (cho <16 tuổi) hoặc student self-reset với parent notification (16+); reset doesn't disrupt multi-class enrollment
  - **Test:** Student lớp 7 (13 tuổi) quên password → flow "Nhờ ba/mẹ reset" only; student lớp 11 (17 tuổi) → option self-reset qua SMS OTP với parent notification cc; sau reset, student vẫn enrolled 3 lớp gốc, không cần re-link
  - **Fail signal:** <16 tuổi có thể self-reset không parent visibility (vi phạm child protection); reset hủy enrollment; reset disrupt class permissions (không thấy lại tài liệu cũ)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-EDGE-002:** Student chuyển từ "Anh cơ bản" sang "Anh nâng cao" giữa kỳ — attendance + grades + homework history preserved + visible trong new class
  - **Test:** Owner/lễ tân move student từ "Anh cơ bản 9A" sang "Anh nâng cao 9A" giữa tháng → student app: lịch update real-time; gradebook hiển thị "Lớp cũ: Anh cơ bản (đã hoàn thành 50%)" + "Lớp mới: Anh nâng cao (bắt đầu)"; homework history preserved cho reference
  - **Fail signal:** History lost khi transfer; lịch không update; new class permissions không grant ngay; student bị locked out của class material cũ (cần xem lại)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 6. Exit / Termination AC

Student finishes học kỳ + parent withdraws / graduates khỏi center / center closes. Data retention per VN PDPL Art 16 (minor — special protection).

- [ ] **AC-EXIT-001:** Parent withdraws student mid-semester — student account deactivated cho tất cả 3-5 môn cùng lúc; data export available trong 7 ngày trước hard-delete; final invoice settled
  - **Test:** Parent request withdraw "B" giữa kỳ → owner/lễ tân deactivate → student app login lần cuối thấy "Khóa học đã kết thúc cho 3 lớp, dữ liệu sẽ xóa sau ngày 1/6/2026"; parent receive Zalo download archive (attendance + điểm + invoice + homework history) trong 24h; final invoice prorated cho phần còn lại
  - **Fail signal:** Deactivation per-class only (still enrolled vài lớp); no aggregated export; immediate hard-delete (no grace period); no final invoice settlement; parent không nhận confirmation
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md)

- [ ] **AC-EXIT-002:** Sau hard-delete (≤6 tháng post-termination per PDPL Art 16 minor), student account fully purged across all enrolled classes; audit log retained per legal hold (7 năm tax)
  - **Test:** 6 tháng sau deactivation → owner search student name → "Không tìm thấy" trong active list cho tất cả 3-5 lớp; "Inactive archive" view (admin-only) hiển thị anonymized record với student ID + class IDs nhưng không có PII; audit log retained cho tax + dispute window
  - **Fail signal:** PII vẫn visible 6 tháng sau (vi phạm PDPL); audit log không có; partial purge (some classes vẫn có student PII while others purged); không thể prove deletion với regulator request
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md)

---

## Scoring

**Total ACs:** 13 (sum across 6 categories: 3 Onboard + 5 Ops + 1 Fin + 3 Comm + 2 Edge + 2 Exit)

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

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md), [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-ONBOARD-002 | TBD | [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | TBD | TBD |
| AC-ONBOARD-003 | TBD | [GAP-054](../../04-quality/gaps/GAP-054-multi-subject.md) | TBD | TBD |
| AC-COMM-001 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-COMM-002 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-COMM-003 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | TBD | TBD |
| AC-EDGE-001 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | TBD | TBD |
| AC-EXIT-001 | TBD | [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) | TBD | TBD |
| AC-EXIT-002 | TBD | [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) | TBD | TBD |

**New gaps to file** (FAIL ACs without existing gap — go through `audit-to-gap-pipeline.md` Step 2.5 state-check before filing):
- TBD — surfaced at GAP-152 Round 1 review. Likely candidates: multi-class unified schedule mobile UX (AC-OPS-001), multi-teacher unified gradebook (AC-OPS-003), aggregated homework feed cross-class (AC-OPS-004), parent daily-digest comm option (AC-COMM-001), in-class transfer history preservation (AC-EDGE-002).

---

## Cross-References

- **Persona source:** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas" + sibling [`../P3-medium-center.md`](../P3-medium-center.md)
- **Sibling secondary AC:** [`student-in-P2.md`](student-in-P2.md), [`student-in-P5.md`](student-in-P5.md) (Wave Secondary-Persona-AC parallel deliverables)
- **Folder index:** [`README.md`](README.md)
- **Template:** [`../_TEMPLATE.md`](../_TEMPLATE.md)
- **Review skill:** [`../../../../.claude/skills/quality/persona-based-business-review.md`](../../../../.claude/skills/quality/persona-based-business-review.md)
- **AC framework gap:** [GAP-151](../../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md)
- **Secondary AC parent gap:** [GAP-153](../../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md)
- **Review execution gap:** [GAP-152](../../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check
- **Cross-link reference gaps:**
  - [GAP-051](../../../04-quality/gaps/GAP-051-xlsx-import.md) — bulk xlsx import (P3 thường enroll batch đầu kỳ)
  - [GAP-052](../../../04-quality/gaps/GAP-052-parent-portal.md) — parent portal (linked với student profile mandatory at tuổi <16)
  - [GAP-053](../../../04-quality/gaps/GAP-053-academic-year.md) — academic year/semester structure (relevant cho P3 organized centers)
  - [GAP-054](../../../04-quality/gaps/GAP-054-multi-subject.md) — multi-subject per student (CRITICAL cho P3 — student × 3-5 môn)
  - [GAP-058](../../../04-quality/gaps/GAP-058-role-hierarchy.md) — role hierarchy (student là leaf, but multiple teacher relationships)
  - [GAP-063](../../../04-quality/gaps/GAP-063-sms-zalo.md) — Zalo/SMS (PRIMARY channel + daily digest cho parent ở scale 3-5 môn)
  - [GAP-184](../../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) — data retention sensitive-minor (PDPL Art 16, ≤6 tháng post-termination)
  - [GAP-186](../../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) — child protection (parent-mediated reset for <16, parent-CC mọi DM, no upload, audit trail mandatory ở scale 5 teachers)

---

## Notes for Reviewer (Phase 2 — Domain Expert)

This AC set targets **THCS-THPT age band** (lớp 6-12, 11-18 tuổi) đăng ký **2-5 môn cùng lúc** trong trung tâm 100-500 học sinh. Edge for review:
- Học sinh tiểu học (lớp 3-5) trong P3 (some centers do mix age): may cần thêm parent-only mode tương tự P2 patterns — nếu reviewer thấy phổ biến, file follow-up gap mở rộng age handling
- Học sinh luyện thi đại học/IELTS (lớp 12 + post-graduates): semi-adult; có thể skip parent-CC cho 18+; consider AC-COMM-003 conditional clause
- Multi-class scale "spike" lúc đầu kỳ: 250 students × 5 môn × 2-3 buổi/tuần = ~3000 attendance records/week — performance test critical; flag follow-up nếu UX strain

**Out-of-scope confirmed for student-in-P3:**
- Formal MOET report card (P5 only, GAP-055)
- Period-based attendance Tiết 1-5/ngày (P5 only, GAP-060)
- Conduct grade hạnh kiểm (P5 only, GAP-059)
- Homeroom GVCN concept (P5 only, GAP-056)
- Promotion/retention logic (P5 K-12, GAP-061)
- Direct payment by minor (parent-mediated for <16, optional 16+ deferred)

---

## Anti-Patterns to Avoid in Review

| ❌ Don't | ✅ Do |
|---------|------|
| Mark AC PASS vì feature có cho parent persona | Verify student-side UX với multi-class scale 3-5 môn × 3-5 teachers |
| Score "PASS" vì feature work cho 1 môn | Verify scenario at 3+ môn cùng lúc — multi-class is the differentiating constraint |
| Bundle student-in-P3 với student-in-P2 | P3 student có 3-5 môn vs P2's 1-3 môn; multi-teacher gradebook + aggregated comm distinct scope |
| Bundle student-in-P3 với student-in-P5 | P3 không có period-attendance, không có GVCN, không có formal MOET report card; distinct |
| Test với synthetic adult data | Test với VN student names, age bands 11-18, parent-CC mandatory cho minor, Zalo daily-digest pattern |
| Allow direct DM giáo viên trong app | DM = vi phạm child protection — phải parent-CC'd hoặc broadcast-only |
| Skip data retention test | PDPL Art 16 minor 6-month retention CRITICAL — audit MUST verify |

---

## Log

- **2026-04-30** — Initial AC set v1 (13 ACs across 6 categories). Author: Agent A (Wave Secondary-Persona-AC, GAP-153 Phase 1). Calibrated cho học sinh THCS-THPT (11-18 tuổi) đăng ký 2-5 môn cùng lúc trong trung tâm 250-student / 12-teacher / 4-subject baseline. Sources: `personas-catalog.md` §Secondary Personas + sibling `P3-medium-center.md` + informed-gut research on VN multi-subject tutoring center dynamics (Zalo-primary kép channel với daily-digest option ở scale, multi-class unified UX, anti-fraud attendance teacher-only marks, child protection mandates parent-CC cho 5-teacher exposure, PDPL Art 16 minor 6-month retention). Cross-linked to GAP-051/052/053/054/058/063/184/186.
