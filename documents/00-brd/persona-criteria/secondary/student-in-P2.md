# Acceptance Criteria — Student in P2 Small Tutoring Center

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Student × P2
**Persona name (VN):** Học sinh trong trung tâm nhỏ / lớp học thêm
**Persona name (EN):** Student in Small Tutoring Center
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent A (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real student/parent representative + Product Owner sign-off (deferred to GAP-152 Round 1 review)
**Tier:** 1 Primary (Tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152 (Round 1 review) → GAP-281 (P1 cells follow-up if needed)
**Tenant context:** Small Tutoring Center (P2)
**Role:** Student (secondary persona)

---

## 0. Context

### Persona profile

- **Tenant:** Small Tutoring Center (P2) — 60 students, 2 hired teachers + owner, 5 classes (Toán-Văn-Anh-Lý-Hóa)
- **User:** Học sinh tiểu học (Tier 2/3) hoặc THCS (Tier 4-5) attending extra-class tutoring (học thêm) sau giờ học chính khóa
- **Age band:** 7-15 tuổi (Lớp 2 - Lớp 9), parent-mediated for ALL financial + identity decisions
- **Device:** Smartphone của ba/mẹ (cấp 1-2 không có máy riêng) hoặc smartphone cá nhân (lớp 8-9). Hiếm khi truy cập từ desktop.
- **Subjects per student:** 1-3 môn (e.g. chỉ Toán + Anh, không full 5 môn). Mỗi môn 2-3 buổi/tuần.

### Key journeys (Student perspective)

1. **Onboarding:** Receive credentials từ giáo viên/owner (KHÔNG self-signup) → first login với SMS OTP / parent-shared password → set avatar
2. **Daily:** Mở Zalo notification → tap link xem lịch tuần → xem bài tập cho buổi tới → đến lớp → xem điểm sau buổi
3. **Communication:** Nhận thông báo nghỉ học/đổi lịch qua Zalo (chính); xem bài tập trên app
4. **Edge:** Quên password → ba/mẹ reset; chuyển lớp giữa kỳ → xem lịch sử attendance; nghỉ ốm → xem makeup plan
5. **Exit:** Hoàn thành khóa hoặc ba/mẹ withdraw → student account deactivate; data retained per VN PDPL Art 16 (minor) ≤ 6 tháng

### Critical concerns (real student/parent voice)

1. **Zalo notification kép** — cả student VÀ parent nhận, KHÔNG chỉ một bên (parent ở P2 wants visibility, student wants own copy)
2. **Đơn giản tối đa** — 1 click thấy "hôm nay học gì, mấy giờ, ở đâu". Không multi-step navigation.
3. **Không nhập điểm danh tự** — giáo viên mark, student CHỈ xem (anti-fraud, common abuse pattern at Solo/Small scale)
4. **Bài tập đơn giản** — receipt-only ("HW: làm trang 45 SBT, nộp T6"), không full LMS (không upload file, không quiz online — outside P2 scope)
5. **Parent contact luôn primary** — student không gọi/chat trực tiếp giáo viên ngoài app; tất cả qua parent

### Out-of-scope for student-in-P2 (covered elsewhere)

- ❌ Formal report card (bảng điểm VN format) → P5 K-12 territory (GAP-055)
- ❌ Period-based attendance (Tiết 1-5/ngày) → P5 only (GAP-060)
- ❌ Conduct grade (hạnh kiểm) → P5 only (GAP-059)
- ❌ Homeroom teacher (GVCN) → P5 only (GAP-056)
- ❌ Direct payment by student → parent-mediated only at this age band
- ❌ Multi-tenant student account (student transfer giữa centers) → out-of-scope toàn product

---

## AC Categories (6 standardized)

Each AC: **AC-{CATEGORY}-{NUM}** + Statement + Test + Fail signal + Status (blank — filled at GAP-152 review) + Linked gap.

---

## 1. Onboarding AC

Student lần đầu nhận account → first login → ready để dùng daily.

- [ ] **AC-ONBOARD-001:** Student nhận credentials qua Zalo/SMS (gửi tới parent phone, KHÔNG yêu cầu student self-signup), first login ≤3 phút từ smartphone
  - **Test:** Owner bulk-import 60 students → parent phone nhận Zalo invite chứa username + temporary password + link → tap link → mở mobile login screen → input credentials → land trên student dashboard hiển thị "Hôm nay học gì"
  - **Fail signal:** Yêu cầu student self-signup (sai tuổi pháp lý), credentials gửi qua email mà parent không check, link login break trên mobile, dashboard empty không có CTA
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-051](../../04-quality/gaps/GAP-051-xlsx-import.md), [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-ONBOARD-002:** Student profile linked với parent contact ngay từ creation (parent phone bắt buộc, parent name optional)
  - **Test:** Sau bulk import, mở student profile → field "Parent phone" populated từ xlsx; "Parent name" optional → khi vắng học, system biết notify đúng số phone
  - **Fail signal:** Parent contact không bắt buộc (cho phép tạo student không có guardian — vi phạm child protection), parent phone field không validate VN format, không link với child-protection rules
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-052](../../04-quality/gaps/GAP-052-parent-portal.md), [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-ONBOARD-003:** First-login wizard ≤3 bước, hỗ trợ student dưới 13 tuổi (parent có thể setup hộ trên cùng device)
  - **Test:** Lần đầu login → step 1: confirm tên + DOB; step 2: chọn avatar từ preset library (KHÔNG upload — privacy); step 3: see today's schedule. Toàn bộ ≤2 phút.
  - **Fail signal:** Wizard >5 bước, yêu cầu upload photo (vi phạm child protection), không có preset avatar, parent không thể setup hộ
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

---

## 2. Daily Operations AC

Workflows hằng ngày — xem lịch, đến lớp, nhận bài tập, xem điểm. **Student KHÔNG mark attendance — teacher marks.**

- [ ] **AC-OPS-001:** Student xem lịch tuần (1-3 môn) trên 1 màn hình mobile, ≤2 lần tap từ home
  - **Test:** Open app → Home → tap "Lịch tuần" → see grid 7 ngày × buổi (ví dụ Toán T2-T4-T6 19h, Anh T3-T5 18h) → tap 1 buổi để xem detail (giáo viên, phòng, status)
  - **Fail signal:** Phải tap >2 lần để thấy lịch, lịch không hiển thị multi-class trên 1 view, không có "next class" highlight, calendar UX desktop-style trên mobile
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-002:** Student xem attendance history (giáo viên đã mark) read-only — KHÔNG cho phép student tự mark
  - **Test:** Vào "Attendance" tab → see last 30 days với mỗi buổi: Có mặt / Vắng / Đi muộn (do giáo viên mark) → student CHỈ có quyền view, không có button "Mark present" hay edit
  - **Fail signal:** Student có thể tự mark/edit attendance (anti-fraud violation), không có history view, dữ liệu attendance không real-time với teacher's input
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-003:** Student nhận bài tập (homework receipt) hiển thị mỗi buổi học — đơn giản text/link, KHÔNG full LMS
  - **Test:** Sau buổi học Toán 9A T2 → giáo viên post note "HW: làm trang 45-46 SBT, nộp Thứ 6" → student thấy notification + entry trong "Bài tập" tab; tap để mark "Đã làm" (self-report, optional)
  - **Fail signal:** Không có homework view; yêu cầu upload file hoàn thành (P2 không cần); không có notification khi giáo viên post; mark "đã làm" trigger workflow phức tạp
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

- [ ] **AC-OPS-004:** Student xem điểm môn học (gradebook view) sau khi giáo viên enter — read-only
  - **Test:** Giáo viên vào gradebook nhập "Kiểm tra 15p: 8.5" cho Student A → trong vòng 15 phút, student mở "Điểm" tab → thấy entry mới với scale 10 + weighted average preview
  - **Fail signal:** Student không thấy điểm hoặc thấy raw data không formatted; không có notification mới; weighted average không tính sẵn; student có quyền edit điểm
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 3. Financial / Admin AC

P2 student KHÔNG trực tiếp pay — parent trả. Student có thể XEM tình trạng (parent đã đóng/chưa) để biết.

- [ ] **AC-FIN-001:** Student xem fee status read-only (parent đã đóng tháng X chưa) — không có button "Pay" cho student
  - **Test:** Student mở "Học phí" tab → thấy "Tháng 4: ✅ Đã đóng (1M VND, ngày 5/4)" hoặc "Tháng 5: ⏳ Chưa đóng (hạn 5/5)"; KHÔNG có "Pay now" button cho student
  - **Fail signal:** Student có thể trigger payment (vi phạm tuổi pháp lý ký), không có fee status view, status không sync real-time với parent's payment record
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 4. Communication AC

Zalo/SMS là PRIMARY channel; student có view in-app nhưng push notification quan trọng nhất.

- [ ] **AC-COMM-001:** Student nhận Zalo notification (KÉP — cả student VÀ parent) khi: (a) lịch đổi (b) lớp nghỉ (c) bài tập mới (d) điểm mới
  - **Test:** Owner reschedule "Toán 9A T4 19h" → student's Zalo + parent's Zalo cả 2 nhận message "Bé A: lịch Toán đổi sang T7 9h" trong vòng 5 phút (with proper VN tone — formal cho parent, friendly cho student có thể)
  - **Fail signal:** Chỉ parent nhận (student missed); chỉ student nhận (parent ngạc nhiên); message không localize VN; delay >15 phút; không có channel choice
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-COMM-002:** Student có inbox in-app nhận announcement broadcast từ giáo viên (cùng kênh Zalo) — không cần install thêm app
  - **Test:** Giáo viên broadcast "Nghỉ Tết 28/1-5/2" tới class "Toán 9A" → student mở app inbox → thấy message + timestamp + giáo viên sender; cũng nhận Zalo nếu parent chưa đọc trong 1 giờ
  - **Fail signal:** Inbox không sync với Zalo; phải install app riêng để xem; broadcast không có sender attribution; spam other classes' messages
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md)

- [ ] **AC-COMM-003:** Student-teacher direct messaging BANNED outside platform/parent oversight — chỉ qua broadcast hoặc parent-mediated
  - **Test:** Student app KHÔNG có "DM giáo viên" button; ask question → click "Gửi câu hỏi" → form submit kèm parent_phone CC; giáo viên trả lời visible trong cả student inbox VÀ parent inbox
  - **Fail signal:** Có DM trực tiếp giáo viên không có parent visibility (vi phạm child protection); không có audit trail; parent không CC'd
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

---

## 5. Edge Cases AC

Common student-side disruptions: forgot password, missed class, transfer between classes mid-month.

- [ ] **AC-EDGE-001:** Student quên password — parent là người reset (KHÔNG phải student tự reset qua email) — tránh self-recovery loophole với minor
  - **Test:** Student không login được → app suggest "Nhờ ba/mẹ reset" → parent mở Zalo nhận magic link → click → reset password new → push tới student device → student login lại
  - **Fail signal:** Student có thể reset qua email mà parent không biết (vi phạm parental control); reset flow không gửi parent notification; bypass thông qua security questions không có parent verification
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md)

- [ ] **AC-EDGE-002:** Student nghỉ ốm/báo nghỉ qua app — parent submit, student VIEW status; makeup plan visible nếu owner schedule bù
  - **Test:** Parent mở "Báo nghỉ" → input lý do "Ốm 2 ngày" + dates → submit → student app hiển thị "Vắng có phép T2-T3" + "Buổi makeup: T7 14h (nếu có)"; teacher's attendance pre-marked "Vắng có phép"
  - **Fail signal:** Student có thể tự báo nghỉ (vi phạm parental authority), makeup plan không hiển thị, attendance không pre-mark, parent không nhận confirmation
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** —

---

## 6. Exit / Termination AC

Student finishes course / parent withdraws / center closes. Data retention per VN PDPL Art 16 (minor — special protection).

- [ ] **AC-EXIT-001:** Parent withdraws student mid-term — student account deactivated, app shows "Lớp đã kết thúc"; data export available trong 7 ngày trước hard-delete
  - **Test:** Owner deactivate student "B" sau parent request → student app login lần cuối thấy "Khóa học đã kết thúc, dữ liệu sẽ xóa sau ngày 1/6/2026"; parent receive Zalo với link download archive (attendance + điểm + invoice history) trong 24h
  - **Fail signal:** Student vẫn login như bình thường; không export window cho parent; immediate hard-delete (no grace period vi phạm parent rights); không Zalo notification cho parent
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md)

- [ ] **AC-EXIT-002:** Sau hard-delete (≤6 tháng post-termination per PDPL Art 16 minor), student account fully purged; no trace trong public-facing UI; audit log retained per legal hold (7 năm tax)
  - **Test:** 6 tháng sau deactivation → owner search student name → "Không tìm thấy" trong active list; "Inactive archive" view (admin-only) hiển thị anonymized record với student ID nhưng không có PII (tên, phone, photo); audit log riêng giữ tax record
  - **Fail signal:** PII vẫn visible 6 tháng sau (vi phạm PDPL); audit log không có; không thể prove deletion với regulator request; parent không nhận confirmation deletion
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md)

---

## Scoring

**Total ACs:** 13 (sum across 6 categories: 3 Onboard + 4 Ops + 1 Fin + 3 Comm + 2 Edge + 2 Exit)

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
| AC-ONBOARD-003 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | TBD | TBD |
| AC-COMM-001 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-COMM-002 | TBD | [GAP-063](../../04-quality/gaps/GAP-063-sms-zalo.md) | TBD | TBD |
| AC-COMM-003 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | TBD | TBD |
| AC-EDGE-001 | TBD | [GAP-186](../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) | TBD | TBD |
| AC-EXIT-001 | TBD | [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) | TBD | TBD |
| AC-EXIT-002 | TBD | [GAP-184](../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) | TBD | TBD |

**New gaps to file** (FAIL ACs without existing gap — go through `audit-to-gap-pipeline.md` Step 2.5 state-check before filing):
- TBD — surfaced at GAP-152 Round 1 review. Likely candidates: simplified mobile homework receipt UX (AC-OPS-003), student read-only fee status view (AC-FIN-001), parent-mediated absence reporting flow (AC-EDGE-002).

---

## Cross-References

- **Persona source:** [`../../personas-catalog.md`](../../personas-catalog.md) §"Secondary Personas" + sibling [`../P2-small-center.md`](../P2-small-center.md)
- **Sibling secondary AC:** [`student-in-P3.md`](student-in-P3.md), [`student-in-P5.md`](student-in-P5.md) (Wave Secondary-Persona-AC parallel deliverables)
- **Folder index:** [`README.md`](README.md)
- **Template:** [`../_TEMPLATE.md`](../_TEMPLATE.md)
- **Review skill:** [`../../../../.claude/skills/quality/persona-based-business-review.md`](../../../../.claude/skills/quality/persona-based-business-review.md)
- **AC framework gap:** [GAP-151](../../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md)
- **Secondary AC parent gap:** [GAP-153](../../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md)
- **Review execution gap:** [GAP-152](../../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Audit-to-gap pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../../.claude/rules/audit-to-gap-pipeline.md) §Step 2.5 state-check
- **Cross-link reference gaps:**
  - [GAP-051](../../../04-quality/gaps/GAP-051-xlsx-import.md) — bulk xlsx import (student credentials distributed via parent contact)
  - [GAP-052](../../../04-quality/gaps/GAP-052-parent-portal.md) — parent portal (linked với student profile mandatory at this age band)
  - [GAP-058](../../../04-quality/gaps/GAP-058-role-hierarchy.md) — role hierarchy (student là leaf node, không có sub-permissions)
  - [GAP-063](../../../04-quality/gaps/GAP-063-sms-zalo.md) — Zalo/SMS notification (PRIMARY channel cho cả student và parent)
  - [GAP-184](../../../04-quality/gaps/GAP-184-data-retention-sensitive-minor.md) — data retention sensitive-minor (PDPL Art 16, ≤6 tháng post-termination)
  - [GAP-186](../../../04-quality/gaps/GAP-186-child-protection-policy-implementation.md) — child protection policy (parent-mediated reset, no direct DM, no upload, parent CC on all comms)

---

## Notes for Reviewer (Phase 2 — Domain Expert)

This AC set targets **lower-mid age band** (lớp 5-9, 10-15 tuổi). Edge for review:
- Học sinh tiểu học (lớp 1-2, 6-8 tuổi): even more parent-mediated; AC-ONBOARD-003 wizard có thể cần "parent-only setup" mode
- Học sinh THPT (lớp 10-12, 16-18 tuổi): sap chuyển sang student-in-P3 hay student-in-P5 vì THPT thường ở center lớn hơn
- Một số P2 centers cho phép student tự pay (lớp 9 trở lên có quỹ lớp riêng) — nếu reviewer thấy pattern này phổ biến, file gap mở rộng AC-FIN

**Out-of-scope confirmed for student-in-P2:**
- Formal report card (P5 territory, GAP-055)
- Period-based attendance multi-tiết/ngày (P5 only, GAP-060)
- Conduct grade hạnh kiểm (P5 only, GAP-059)
- Homeroom GVCN (P5 only, GAP-056)
- Direct payment by minor (parent-mediated only)
- Multi-tenant student transfer giữa centers (out-of-scope toàn product)

---

## Anti-Patterns to Avoid in Review

| ❌ Don't | ✅ Do |
|---------|------|
| Mark AC PASS vì feature có sẵn cho parent persona | Verify student-side UX cụ thể với 10-15 tuổi mobile usage pattern |
| Assume student có email riêng | At lớp 5-9, parent phone là PRIMARY identifier — không assume student email |
| Test với synthetic data người lớn | Test với VN student names, school grade levels (lớp 5/6/7/8/9), parent Zalo flow |
| Score "PASS" nếu DM giáo viên có sẵn | Direct DM = vi phạm child protection — phải parent-CC'd hoặc broadcast-only |
| Bundle student-in-P2 với student-in-P5 | P2 student có 1-3 môn, no GVCN, no period attendance, no formal report card — distinct scope |

---

## Log

- **2026-04-30** — Initial AC set v1 (13 ACs across 6 categories). Author: Agent A (Wave Secondary-Persona-AC, GAP-153 Phase 1). Calibrated cho học sinh lớp 5-9 (10-15 tuổi) trong trung tâm dạy thêm 60-student / 2-teacher / 5-subject baseline. Sources: `personas-catalog.md` §Secondary Personas + sibling `P2-small-center.md` + informed-gut research on VN học-thêm dynamics (Zalo-primary parent-student kép channel, no student self-pay at this age, anti-fraud attendance, child protection mandates parent-mediated workflows, PDPL Art 16 minor 6-month retention). Cross-linked to GAP-051/052/058/063/184/186.
