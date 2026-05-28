---
audience: dev
date: 2026-05-28
session-theme: Beta-launch persona simulation cho Plan D (Hybrid close-loop 1-3 friend beta ~3 tuần)
audit-type: persona-review
scope: 5 personas × 3 critical flows × MUST-HAVE vs NICE-TO-HAVE matrix
related-incident: documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md (17 bugs trong feature shipped-DONE)
related-retro: documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md (46-feature retro, 50% NONE walk-evidence)
priority: P0 (Phase 2 BETA scope-shaping prerequisite — friend-beta selection + ~3-tuần timeline verify)
related-rule: .claude/rules/feature-ship-runtime-walk-mandate.md v1.0.0
methodology: persona simulation — không walk live stack (audit retro đã cover walk-evidence); pure simulation + FE/BE source code inventory
---

# Beta-launch persona simulation — Plan D realistic scope

## Tóm lược điều hành (TL;DR)

User mất niềm tin về Phase 1 BETA full-flow pass sau khi Wave meta-6 Bucket A walk surface 17 bugs trong feature shipped-DONE + audit retro xác nhận 50% / 46 Wave 80+ features không có walk evidence. Plan D = Hybrid close-loop 1-3 friend beta ~3 tuần thay vì public invite — cần ground bằng persona simulation để xác định MUST-HAVE vs NICE-TO-HAVE features per persona.

**5 personas simulated:** P1 Solo Teacher (chị Linh) / P2 Center Owner (chị Hằng) / P3 Center Manager (anh Tâm) / P4 Parent (chị Mai) / P5 Student (em An).

**Key findings:**

1. **MUST-HAVE matrix tổng 17 features** (≤ phạm vi ~15-20 target) cho close-loop beta thành công với 1-3 friend
2. **Friend-beta recommendation:** P2 Owner-only cohort 1-3 người (KHÔNG mix persona) — chị Hằng class là cohort tối ưu vì surface complexity nhất + critical path Owner→Manager→Teacher chained
3. **Timeline verify:** ~5 ngày bare-minimum prep KHẢ THI **CHỈ NẾU** scope hẹp xuống MUST-HAVE matrix + skip P4/P5 flows + defer 8 NICE-TO-HAVE features
4. **Top drop-off risk hotspots:** Bug #14 (no email send Bucket A) + Bug #17 (no user provisioning) + GAP-704 (JWT tenantId missing post-signup) + course/class CRUD walk-untested + payment-collect path NONE-walk → 5 features sẽ cause beta abandonment trong ngày đầu nếu broken

---

## §1. Personas covered (5)

### 1.1 P1 Solo Teacher — chị Linh

**Demographics:** Nữ, 32 tuổi, dạy thêm tiếng Anh 30 học sinh THCS/THPT tại nhà + cafe. 1 mình tự quản tất cả: lịch dạy, thu phí, chấm điểm, liên hệ phụ huynh. Dùng Zalo + Google Sheet hiện tại. Tech-comfort: trung bình (smartphone heavy user, laptop occasional).

**Pain points hiện tại:**
- Mất tracking học sinh nào đã đóng phí tháng này
- Phụ huynh hỏi điểm con qua Zalo, mỗi lần phải mở Sheet tìm
- Lớp 8 em vắng, không biết ngày nào em vắng để báo phụ huynh
- Tự in hóa đơn Word mỗi tháng cho phụ huynh nào yêu cầu

**Đến KiteHub vì:** "Có ai bảo KiteHub tiện cho giáo viên solo? Tôi muốn bớt việc admin để tập trung dạy".

**Quyết định ở lại sau 1 tuần khi:** ≥2 việc admin được tự động hóa (vd: nhắc đóng phí + điểm danh + hóa đơn tự sinh).

### 1.2 P2 Center Owner — chị Hằng

**Demographics:** Nữ, 38 tuổi, sở hữu "Trung tâm Anh ngữ Sky Education" 50 học sinh + 5 nhân viên (3 giáo viên + 1 quản lý + 1 lễ tân). 8 năm vận hành center. Trước đây dùng Excel + Zalo group. Tech-comfort: cao cho operations, trung bình cho data analytics.

**Pain points hiện tại:**
- Tháng nào cũng cãi nhau với giáo viên về số tiết dạy (tracking thủ công)
- Phụ huynh hỏi "lớp con tôi học phí bao nhiêu" — Excel quá lộn xộn
- Báo cáo cuối tháng (doanh thu/chi phí/lương) mất 2 ngày tổng hợp
- Mỗi giáo viên mới phải training 1 tuần dùng hệ thống Excel hiện tại

**Đến KiteHub vì:** "Tôi cần platform 1 chỗ — giáo viên log lịch, lễ tân thu tiền, tôi xem báo cáo. Excel hết chịu nổi".

**Quyết định ở lại sau 1 tuần khi:** ≥3/5 nhân viên dùng được hệ thống mà không gọi chị Hằng hỏi + báo cáo doanh thu hiển thị đúng. **Đây là persona surface complexity cao nhất** — Owner chain dependency: invite staff → staff onboard → staff log workflow → Owner xem báo cáo.

### 1.3 P3 Center Manager — anh Tâm

**Demographics:** Nam, 29 tuổi, Quản lý vận hành tại Sky Education (báo cáo cho chị Hằng). Day-to-day: sắp lịch lớp, điều phối giáo viên, xử lý feedback phụ huynh, thu phí, check công tiết. Tech-comfort: cao (sử dụng nhiều SaaS công cụ — Trello, Slack, Notion).

**Pain points hiện tại:**
- Sáng đi làm phải gọi từng giáo viên xác nhận có dạy hôm nay không
- Phụ huynh nhắn Zalo nhờ chuyển lớp con — phải tay nhập 3 chỗ (sheet lớp cũ, sheet lớp mới, sheet billing)
- Giáo viên báo nghỉ ốm 30 phút trước giờ dạy → cuống lên tìm người thay

**Đến KiteHub vì:** chị Hằng add anh Tâm vào hệ thống với role Manager. Anh Tâm KHÔNG tự đăng ký — đây là invite-driven onboarding.

**Quyết định ở lại sau 1 tuần khi:** workflow điều phối lịch + xử lý request phụ huynh nhanh hơn 50% so với Excel. **Note:** Phase 1 BETA per `documents/01-business/roles/rules.md` — Manager role MERGE vào OWNER (không separate role table cho Phase 1). P3 unlock Wave 80+ Phase 2. **→ P3 NOT in close-loop beta scope.**

### 1.4 P4 Parent — chị Mai

**Demographics:** Nữ, 42 tuổi, mẹ của em An (15 tuổi, lớp 9). Làm việc văn phòng, ít thời gian. Quan tâm: con đi học đầy đủ không, điểm test thế nào, học phí cần đóng. Tech-comfort: thấp-trung bình (Zalo chính; Email chỉ check khi cần).

**Pain points hiện tại:**
- Phải mở Zalo group lớp con để hỏi điểm bài kiểm tra tuần
- Đóng phí qua chuyển khoản, không có hóa đơn rõ ràng (giáo viên gửi tin nhắn "ok đã nhận")
- Không biết con đi học đầy đủ hay nghỉ (con báo có đi nhưng thực tế?)

**Đến KiteHub vì:** trung tâm chị Hằng gửi cho chị Mai link "Tài khoản phụ huynh KiteHub — theo dõi con An". Chị Mai KHÔNG tự đăng ký — đây là tenant-driven provisioning.

**Quyết định ở lại sau 1 tuần khi:** chỉ cần mở Zalo (KHÔNG cần mở app KiteHub) vẫn nhận được điểm danh + điểm + hóa đơn. **Note:** P4 Parent là **second-degree user** — phụ thuộc P2 Owner onboarding + class enrollment + attendance/grade record. Trong close-loop beta 3 tuần, **P4 chỉ nên có 1 sample child enrollment per Owner** để test broadcast notifications, không phải full P4 workflow.

### 1.5 P5 Student — em An

**Demographics:** Nam, 15 tuổi, lớp 9 THCS. Smartphone là life. Tech-comfort: cao cho consumer apps (TikTok, YouTube, game), nhưng KHÔNG quen với productivity tool. Attention span: ngắn.

**Pain points hiện tại:**
- Quên lịch học (mẹ phải nhắc)
- Quên bài tập về nhà
- Không có cách self-track điểm cải thiện theo thời gian

**Đến KiteHub vì:** giáo viên/trung tâm yêu cầu em An đăng nhập KiteHub để check lịch + bài tập. Em An KHÔNG self-motivated dùng — passive user.

**Quyết định dùng sau 1 tuần khi:** mobile-first UX đủ smooth + push notification nhắc lịch + xem điểm trên 1 màn hình duy nhất. **Note:** P5 cũng là second-degree user. Phase 1 BETA per `kiteclass-frontend/src/app/(dashboard)/student/` — student page tồn tại nhưng walk evidence NONE. **→ P5 LIKELY NOT in close-loop beta scope, defer Phase 2+.**

---

## §2. Per-persona simulation — 3 critical flows

### 2.1 P1 Solo Teacher (chị Linh) — 3 flows

#### Flow 1.1 — First-touch onboarding: Signup → Tạo lớp đầu tiên + thêm 5 học sinh
**Steps simulation:**
1. Landing page → click "Đăng ký dùng thử"
2. Beta signup form: email + tên + chọn role "Giáo viên solo"
3. Verify email link → Set password
4. First-time login → Onboarding wizard 4-step (skipable):
   - Tên trung tâm (chị Linh nhập "Lớp tiếng Anh chị Linh")
   - Tạo lớp đầu tiên (vd "Lớp 8 Anh ngữ thứ 2-4-6")
   - Import 5 học sinh (CSV upload OR thêm tay)
   - Xong! Dashboard hiển thị "1 lớp · 5 học sinh"

**Required features:**
- Beta signup flow (GAP-571/611 fixed)
- Email verification (GAP-576 routing fixed; GAP-702/703 email path)
- Onboarding wizard 4-step skipable (GAP-288 P1 onboarding tour)
- Lớp CRUD (course/class entity backend)
- Student bulk import OR manual add (GAP-325 P2 setup — applicable P1 simplified)

**Quality bar PASS:** Chị Linh đăng ký + thêm lớp + 5 HS trong <15 phút mà KHÔNG cần Google search "how to" — first-action delivers value tức thì (lớp + HS visible trên dashboard).

**Drop-off risk score: 9/10** — nếu signup/verify-email broken (GAP-576 class) hoặc onboarding wizard crash → chị Linh bỏ trong ngày đầu. **Đây là Top 1 critical flow.**

#### Flow 1.2 — Weekly action: Điểm danh + ghi điểm test cho 1 buổi học
**Steps simulation:**
1. Login → Dashboard → click "Lịch hôm nay"
2. Lớp 8 Anh ngữ 19:00 → click "Điểm danh"
3. Bảng 5 HS hiện ra với toggle Có mặt / Vắng / Đi muộn
4. Chọn → Lưu
5. Sau giờ học → click "Ghi điểm" → nhập điểm test cho 5 HS
6. Click "Gửi điểm cho phụ huynh" → Zalo/email broadcast

**Required features:**
- Attendance entity + UI (`kiteclass-frontend/src/app/(dashboard)/attendance/`)
- Grade entry + UI (`kiteclass-frontend/src/app/(dashboard)/...`)
- Parent broadcast notification (Zalo OA preferred per GAP-660, email fallback)

**Quality bar PASS:** Điểm danh + ghi điểm + broadcast hoàn tất <5 phút sau buổi học. Phụ huynh nhận noti trong 1 phút.

**Drop-off risk score: 8/10** — đây là daily retention loop. Nếu vắng 1 trong 3 features (attendance, grade entry, broadcast) → chị Linh quay về Zalo + Sheet sau ngày 3.

#### Flow 1.3 — Edge case: Phụ huynh hỏi sao tháng này con đóng 800k mà em B chỉ đóng 600k
**Steps simulation:**
1. Phụ huynh nhắn Zalo: "Phí thế nào?"
2. Chị Linh: mở KiteHub → Billing → click HS đó → lịch sử thanh toán
3. Hiện ra: tháng này 8 buổi học × 100k = 800k. Em B nghỉ 2 buổi → 6 buổi × 100k = 600k
4. Chị Linh screenshot gửi phụ huynh

**Required features:**
- Per-student billing history (đơn giản: số buổi × đơn giá)
- Học phí auto-calculate dựa trên attendance
- Invoice generation (PDF hoặc image share-able)

**Quality bar PASS:** Mở app + tìm thông tin <1 phút. KHÔNG cần Google Sheet backup.

**Drop-off risk score: 6/10** — edge case nhưng cao impact: nếu chị Linh không trả lời được phụ huynh tự tin → mất trust vào hệ thống → quay về Excel.

### 2.2 P2 Center Owner (chị Hằng) — 3 flows

#### Flow 2.1 — First-touch onboarding: Signup → Setup trung tâm 5 nhân viên + 3 lớp
**Steps simulation:**
1. Landing → "Đăng ký Trung tâm" → beta signup form
2. Verify email → Set password → Onboarding wizard 7-step (per GAP-588):
   - Thông tin trung tâm (tên, địa chỉ, MST)
   - Branding (logo, màu chủ đạo — skip OK)
   - Tạo gói học phí (đơn giá per buổi/tháng)
   - Mời 5 nhân viên (3 giáo viên + 1 quản lý + 1 lễ tân) via email
   - Tạo lớp đầu tiên
   - Skip / Done
3. Dashboard Owner hiển thị: 1 lớp · 0 HS · 5 lời mời pending

**Required features:**
- Beta signup + email verify
- JWT tenantId claim post-signup (GAP-704 BLOCKER — current state: onboarding 400 sau signup)
- Onboarding wizard 7-step skipable (GAP-588 DONE — but walk evidence PARTIAL)
- Branding wizard skip-default (GAP-287)
- **Staff invitation flow (Wave meta-6 Bucket A — INVALID DONE per shutdown findings 17 bugs)**
  - Bug #14: no email send → staff KHÔNG nhận lời mời
  - Bug #17: no user provisioning on accept → staff không thể đăng nhập
  - **→ HARD BLOCKER cho P2 flow.** Without email + provisioning, P2 Owner KHÔNG thể onboard staff
- Pricing tier configuration

**Quality bar PASS:** Chị Hằng hoàn tất setup + 5 staff nhận email + ≥3/5 staff đăng nhập thành công trong ngày đầu. **Currently FAIL hard** — staff-invite path 100% broken.

**Drop-off risk score: 10/10** — **TOP 1 BLOCKER cho close-loop beta.** Nếu staff không onboard được → chị Hằng KHÔNG ship được team lên KiteHub → quay Excel ngay ngày 1.

#### Flow 2.2 — Weekly action: Xem báo cáo doanh thu tháng + lương giáo viên
**Steps simulation:**
1. Login → Dashboard Owner → KPI cards 3 chỉ số (Doanh thu / Học phí thu / Lương phải trả)
2. Click "Xem chi tiết" → bảng breakdown per lớp / per giáo viên
3. Export Excel (optional)

**Required features:**
- Income dashboard (GAP-293 reference — Wave 100 Bucket B)
- KPI cards với VND format `1.500.000đ`
- Per-class + per-teacher breakdown
- Commission engine (Phase 2 P3 scope — defer)

**Quality bar PASS:** Mở report + hiểu số liệu <2 phút. Số khớp với expectation chị Hằng (10% sai cũng nghi ngờ).

**Drop-off risk score: 7/10** — nếu báo cáo sai số → chị Hằng mất trust system làm payroll → switch back Excel.

#### Flow 2.3 — Edge case: 1 staff (lễ tân) nghỉ việc giữa tháng → phải transfer quyền + revoke access
**Steps simulation:**
1. Login → Settings → Staff management
2. Click lễ tân đó → "Off-boarding"
3. Confirm → role revoked, JWT invalidated, audit log entry
4. Re-assign duty (vd: ai sẽ thu phí thay) — manual ngoài hệ thống

**Required features:**
- Staff management UI (per Wave meta-6 Bucket A — current state walk evidence missing)
- Role revoke + JWT invalidation (per `documents/01-business/kitehub/off-boarding/`)
- Admin audit log (GAP-715 DONE walk evidence PARTIAL — current JSON null binding bug fixed)

**Quality bar PASS:** Off-boarding hoàn tất <3 phút + staff cũ KHÔNG còn login được trong vòng 5 phút.

**Drop-off risk score: 4/10** — edge case low-frequency nhưng high-trust impact (security).

### 2.3 P4 Parent (chị Mai) — 3 flows

#### Flow 4.1 — First-touch: Nhận email invite từ trung tâm + lần đầu xem điểm con
**Steps simulation:**
1. Trung tâm chị Hằng (qua chị Linh/anh Tâm) add em An vào lớp với link tài khoản phụ huynh = chị Mai email
2. Chị Mai nhận email "Trung tâm Sky Education đã tạo tài khoản phụ huynh cho bạn — theo dõi em An lớp 9 Anh ngữ"
3. Click magic link (KHÔNG yêu cầu set password) → Mobile-first dashboard
4. Xem: em An tuần này có 3 buổi học, 2 lần đã đi, điểm test gần nhất 7.5

**Required features:**
- Parent provisioning flow (link via tenant; Wave 2 inline-fetch FE skeleton 159 LOC tồn tại nhưng GAP-345 missed)
- Magic link auth (passwordless cho parent simplicity)
- Mobile-first responsive parent dashboard
- Real-time attendance + grade read API

**Quality bar PASS:** Chị Mai mở email + thấy điểm con trong <30 giây + KHÔNG cần download app.

**Drop-off risk score: 5/10** — second-degree user; impact gián tiếp lên P2 Owner retention (nếu phụ huynh khen Owner: "KiteHub tiện quá!" → P2 stick).

#### Flow 4.2 — Weekly action: Nhận push notification "An vắng buổi học hôm nay"
**Steps simulation:**
1. Giáo viên điểm danh trong KiteHub → em An vắng
2. Trigger broadcast → chị Mai nhận:
   - Zalo OA message (preferred per GAP-660)
   - Email fallback (nếu chưa kết nối Zalo)
3. Click link → app/web hiển thị chi tiết: "An vắng buổi 19:00 hôm nay (15/06/2026)"

**Required features:**
- Attendance trigger → notification fan-out
- Zalo OA integration (GAP-660 — Wave 100, Phase 1.5 candidate; defer Phase 1 BETA)
- Email transactional path (GAP-702/703 — current FIX shipped Wave 105)

**Quality bar PASS:** Notification arrives <5 phút sau khi giáo viên điểm danh. Email format VN-friendly (tone "Kính gửi quý phụ huynh,").

**Drop-off risk score: 3/10** cho close-loop beta (P4 second-degree); nhưng cho production launch sau này: 8/10.

#### Flow 4.3 — Edge case: Chị Mai chuyển khoản học phí nhưng bank chậm → hệ thống chưa update
**Steps simulation:**
1. Chị Mai chuyển khoản 1.500.000đ qua VietQR → app banking báo "Thành công"
2. Trung tâm KHÔNG nhận noti (auto-confirm chưa work) → trạng thái HS vẫn "Còn nợ"
3. Chị Mai nhắn Zalo lễ tân → lễ tân vào KiteHub → manual mark paid
4. (Phase 1.5 ideal: auto-confirm via SePay/Casso webhook — GAP-NEW-payment-processor-init cancel per Wave 93)

**Required features:**
- Payment record entity (Phase 1 manual mark)
- VietQR generation (optional Phase 1)
- Auto-confirm webhook (Phase 1.5+ — defer)

**Quality bar PASS Phase 1:** Manual mark paid hoàn tất <2 phút sau khi lễ tân nhận noti Zalo. Auto-confirm = Phase 1.5+.

**Drop-off risk score: 4/10** — friction nhưng manual workaround OK cho beta.

### 2.4 P3 Manager (anh Tâm) — DEFER Phase 2 per business rules

Per `documents/01-business/roles/rules.md` — Phase 1 BETA Manager role MERGE vào OWNER. **→ KHÔNG simulate P3 trong close-loop beta scope.** Defer Wave 80+ Phase 2.

### 2.5 P5 Student (em An) — DEFER Phase 2 per scope

Per Wave 80+ retro audit, student page (`kiteclass-frontend/src/app/(dashboard)/student/`) tồn tại nhưng walk evidence NONE. Em An là passive second-degree user — close-loop beta 3 tuần KHÔNG đủ thời gian cover mobile-first UX polish + push notification + smooth grade view. **→ DEFER Phase 2.**

---

## §3. MUST-HAVE matrix (the deliverable)

Aggregated từ 5 personas × 3 flows (P3/P5 defer → effective scope = P1 + P2 + P4-light).

| Persona | Flow | MUST-HAVE features (Phase 1 BETA close-loop) | NICE-TO-HAVE (defer Phase 1.5+ hoặc Phase 3) |
|---|---|---|---|
| **P1 Solo Linh** | F1.1 First-touch onboarding | (1) Beta signup + email verify (GAP-576/702/703/704 fixed) · (2) P1 onboarding tour 4-step skipable (GAP-288) · (3) Class/Course CRUD basic · (4) Student manual add (1 by 1) | Bulk CSV import (defer Phase 1.5 — GAP-325) · Branding wizard non-default (skip default per GAP-287) |
| P1 Solo Linh | F1.2 Weekly action | (5) Attendance UI per buổi (toggle Có/Vắng/Đi muộn) · (6) Grade entry basic per HS per test · (7) Email broadcast điểm cho phụ huynh (Zalo defer) | Zalo OA broadcast (defer Phase 1.5 — GAP-660) · Bulk grade entry (CSV) |
| P1 Solo Linh | F1.3 Edge billing | (8) Per-student billing history (số buổi × đơn giá) · (9) Invoice generation (PDF or image) | Auto-calculate billing từ attendance (manual entry Phase 1) · VAT eInvoice MISA partnership (Phase 1.5 GAP-185) |
| **P2 Owner Hằng** | F2.1 First-touch onboarding | (10) JWT tenantId claim post-signup (GAP-704 — currently FAIL) · (11) Onboarding wizard 7-step skipable (GAP-588 DONE walk evidence PARTIAL) · (12) Staff invitation email path (Bug #14 fix) · (13) Staff user provisioning on accept (Bug #17 fix) · (14) Pricing tier config basic | Multi-class scheduling (P3 scope defer Phase 2) · Commission engine (P3 defer Phase 2) |
| P2 Owner Hằng | F2.2 Weekly action | (15) Income dashboard 3 KPI cards VND format (GAP-293 Wave 100 Bucket B) | Cohort retention D7/D14/D30 (GAP-591 internal admin only) · Per-teacher payroll auto (P3 defer) |
| P2 Owner Hằng | F2.3 Edge off-boarding | (16) Staff management UI + role revoke (per off-boarding rules.md) | Audit log search UI (admin only, defer Phase 2) |
| **P4 Parent Mai (light)** | F4.1 First-touch | (17) Parent provisioning + magic link auth + mobile-first dashboard read | Full parent self-service portal (Phase 2+) |
| P4 Parent Mai (light) | F4.2 Weekly noti | Email broadcast (within MUST-HAVE #7 above) | Zalo OA broadcast (GAP-660 defer Phase 1.5) · Push notification mobile app (Phase 2+) |
| P4 Parent Mai (light) | F4.3 Edge payment | (covered by P1 #8 + P2 #16 staff manual mark paid) | Auto-confirm webhook SePay/Casso (Phase 1.5+) · VietQR generation (Phase 1.5) |

**Total MUST-HAVE = 17 features** trong target ~15-20 range. ✅

### NICE-TO-HAVE deferral list (8 features defer Phase 1.5+ hoặc Phase 3):

1. Bulk CSV student import (GAP-325) → Phase 1.5
2. Zalo OA broadcast (GAP-660) → Phase 1.5
3. Bulk grade entry CSV → Phase 1.5
4. Auto-calculate billing từ attendance → Phase 1.5
5. VAT eInvoice MISA partnership (GAP-185) → Phase 1.5
6. Cohort retention D7/D14/D30 (GAP-591) → defer (internal admin only, không user-facing)
7. Auto-confirm payment webhook → Phase 1.5+
8. VietQR generation → Phase 1.5
9. P3 Manager full role separation → Phase 2
10. P3 Commission engine → Phase 2
11. P3 Multi-class scheduling conflict → Phase 2
12. P5 Student mobile-first UX polish → Phase 2
13. Full parent self-service portal → Phase 2+
14. Push notification mobile app → Phase 2+

---

## §4. Friend-beta selection recommendation

### 4.1 Recommended: P2 Owner-only cohort, 1-3 friends, KHÔNG mix persona

**Why P2 Owner class first:**

1. **Surface complexity cao nhất** — Owner chain dependency (invite staff → staff onboard → staff log workflow → Owner xem báo cáo) bao trùm critical path nhiều bugs nhất (Wave meta-6 Bucket A 17 bugs đều trong P2 Owner workflow)
2. **Force exercise hardest path đầu tiên** — Phase 2 BETA pre-flight check; nếu P2 Owner pass close-loop → mọi persona đơn giản hơn (P1 solo = subset of P2 Owner workflow without staff layer; P4/P5 = second-degree readers)
3. **Single persona = simpler feedback loop** — 3 P2 Owner sẽ surface variance trong scope hẹp; tránh confusion "lỗi này do persona A hay B?"
4. **P2 Owner = primary revenue persona Phase 1.5 paid launch** — friend feedback dồn thẳng vào paid conversion path
5. **3 personas khác (P1 solo + P3 manager + P4 parent + P5 student)** tự nhiên emerge trong P2 Owner workflow: chị Hằng add chị Linh-equivalent (1 giáo viên solo trong center) + add 1 staff manager-like + 1 phụ huynh sample. Đây là **organic multi-persona coverage** mà KHÔNG cần recruit thêm.

**KHÔNG nên:**
- ❌ Mix P1 Solo + P2 Owner cùng cohort 1-3 → 2 personas × 3 = 6 feedback streams, overwhelm cho solo dev triage
- ❌ Recruit P3/P4/P5 trực tiếp trong close-loop → P3 role defer Phase 2; P4/P5 second-degree
- ❌ Cohort >3 friend → solo dev không scale support load; mỗi bug fix cần ~2-4h
- ❌ Public invite trước close-loop → trust-pass risk còn ≥7 recurrence; cần verify từng feature bằng human walk

### 4.2 Friend selection criteria

**3 friends ideal cohort:**

| # | Profile | Variance dimension | Expected feedback signal |
|---|---|---|---|
| 1 | Friend là chủ trung tâm tiếng Anh nhỏ 30-50 HS, 3-5 staff, đã dùng Excel + Zalo nhiều năm | Closest to chị Hằng baseline | Variance LOW — base coverage |
| 2 | Friend là chủ trung tâm dạy thêm Toán/Văn, dạy lẻ home-based mở rộng dần ~50 HS | Variance HIGH (subject + setup type) | Surface scope gaps (vd: nếu UI assume English-only subject naming) |
| 3 | Friend là cô giáo solo 20-30 HS, KHÔNG có staff (P1 phải dùng P2 features partially) | Variance EDGE (P1 trying P2 platform) | Surface UX friction (nếu wizard 7-step quá nặng cho solo) |

**Feedback signal quality projected:**
- Cohort 3 friend × 3 tuần × 1 weekly retro = **9 feedback sessions**
- Ước lượng ~5-15 unique bugs surfaced (60-70% feature class — auth/onboarding/staff-invite/billing/dashboard)
- Critical mass cho Phase 1.5 paid launch prep: cohort 3 → 9 sessions đủ surface trust-pass bugs class

### 4.3 KHÔNG nên scope thêm

- Email signup KHÔNG mời > Phase 1 BETA invite (chờ Wave 100 Bucket C → Phase 1.5 promote)
- KHÔNG launch landing page public — chỉ direct invite link per friend
- KHÔNG yêu cầu friend pay (close-loop = free trial)

---

## §5. Plan D refinement based on persona findings

### 5.1 Flows fixed first để satisfy MUST-HAVE matrix (priority order)

**Block 1 — P0 BLOCKERS cho close-loop launch (must fix trước ngày 1):**

1. **Bug #14 + #17 staff invitation feature** (Wave meta-6 Bucket A REVERT DONE → re-classify PARTIAL)
   - Bug #14: Implement email send path (outbox + kitehub-email consumer + template + SES binding)
   - Bug #17: Implement user provisioning on accept (decide architecture — kiteclass-core OR gateway callback OR kitehub-platform owns)
   - Estimate: ~3-4 ngày engineer
2. **GAP-704 JWT tenantId claim post-signup** (currently P2 Owner onboarding 400)
   - Estimate: ~0.5-1 ngày
3. **GAP-704 sister gaps** (GAP-705/706/711/712 JWT/tenant filter chain)
   - Estimate: ~1 ngày bundled

**Block 2 — Quality bar verify (must walk before invite friend):**

4. RST walk full P2 Owner Flow 2.1 (signup → onboarding → invite staff → staff onboard → Owner add lớp)
5. RST walk P1 Solo Flow 1.1 (signup → onboarding → tạo lớp + 5 HS)
6. RST walk P1 Solo Flow 1.2 (điểm danh + grade entry + email broadcast)
7. RST walk P2 Owner Flow 2.2 (income dashboard 3 KPI cards)
8. RST walk Flow 4.1 P4 parent magic link (sample 1 phụ huynh per Owner)

**Block 3 — Polish before invite:**

9. Per `feature-ship-runtime-walk-mandate.md` v1.0.0 — every MUST-HAVE feature paste walk evidence vào gap closure
10. Bug catalog cleanup từ Wave 80+ retro audit (50% NONE walk — sample 5-10 highest-priority features)

### 5.2 Deferred beyond close-loop (Phase 1.5 hoặc Phase 3+)

Per §3 NICE-TO-HAVE list — 14 features deferred:
- Bulk imports (CSV)
- Zalo OA (GAP-660)
- VAT eInvoice MISA (GAP-185)
- Auto-confirm payment webhook
- P3 Manager separation + commission + scheduling
- P5 Student mobile UX
- Full parent self-service portal

### 5.3 Realistic timeline verify: ~5 ngày bare-minimum prep — **PARTIAL TRUE**

| Phase | Scope | Estimate | Verdict |
|---|---|---|---|
| **Block 1 — P0 BLOCKERS fix** | Bug #14 email + Bug #17 provisioning + GAP-704 cluster | **4-6 ngày** | ❌ KHÔNG fit ~5 ngày bare-minimum nếu Bug #17 architecture decision chưa lock |
| **Block 2 — RST walk verify (5 flows)** | 0.5 ngày × 5 flow = 2.5 ngày | **2-3 ngày** | ✅ Fit |
| **Block 3 — Polish (5-10 features walk evidence)** | Per `feature-ship-runtime-walk-mandate.md` walk + gap closure | **3-5 ngày** | ❌ Compound với Block 1 → tổng ~9-14 ngày |

**Realistic timeline conclusion:**

- ❌ **~5 ngày bare-minimum prep KHÔNG khả thi** nếu yêu cầu close all Block 1 + Block 2 + Block 3 trước invite
- ✅ **~2 tuần (10-14 ngày) prep + 3 tuần beta = ~5 tuần total** là realistic timeline
- ⚠️ **Alternative path "≤5 ngày prep":** scope hẹp xuống CHỈ Block 1 (Bug #14 + #17 + GAP-704) + Block 2 RST walk; defer Block 3 polish vào in-beta hotfix cycles. Acceptable risk vì cohort 3 friend (KHÔNG public)
- 🟡 **Recommended:** lock plan D = **10 ngày prep + 3 tuần beta** thay vì 5 ngày prep — match realistic engineer capacity + giảm in-beta hotfix friction

### 5.4 Per `release-fix-retry-budget.md` §3.5 — investigation-first discipline

Nếu Bug #17 user provisioning architecture quyết định sai → retry cycle sẽ rồi vào trap "patch-without-investigation" (per Wave meta-1+meta-2 GAP-735 precedent). MANDATORY apply rule `release-fix-retry-budget.md` v1.2.0 §3.5: investigation phase BEFORE first fix attempt.

---

## §6. Drop-off risk hotspots (top 5)

Map từ MUST-HAVE matrix × known bug catalog (17 Bucket A bugs + 46 Wave 80+ feature retro).

### Top 1: Staff invitation flow Bug #14 + #17 (P0)
**Persona affected:** P2 Owner (chị Hằng)
**Drop-off risk:** 10/10
**Bug catalog mapping:**
- Wave meta-6 Bucket A walk shutdown #14 (no email send)
- Wave meta-6 Bucket A walk shutdown #17 (no user provisioning)
**Impact:** Chị Hằng KHÔNG thể onboard 5 staff trong ngày 1 → bỏ ngay. **Single-point-of-failure cho cả P2 Owner cohort.**
**Action:** MUST fix trước friend invite. Re-classify Wave meta-6 Bucket A DONE → PARTIAL.

### Top 2: JWT tenantId claim post-signup (P0)
**Persona affected:** P2 Owner (chị Hằng)
**Drop-off risk:** 10/10
**Bug catalog mapping:**
- GAP-704 (Wave 105 DONE — walk evidence PARTIAL)
- GAP-705/706/711/712 sister cluster
**Impact:** Chị Hằng đăng ký xong → 400 lỗi → "Trang web không hoạt động" → reload nhiều lần → bỏ.
**Action:** Walk verify trên production-equivalent stack. Confirm `pre-handoff-self-test-completeness.md` §2.4 (a)→(g) all PASS.

### Top 3: Course/Class CRUD walk-untested (P0 likely)
**Persona affected:** P1 Solo + P2 Owner
**Drop-off risk:** 8-9/10
**Bug catalog mapping:**
- 46-feature retro audit estimate ~50% NONE walk evidence
- Course/class is foundational entity — nếu CRUD broken, attendance + grade + billing đều cascade fail
**Impact:** Chị Linh tạo lớp đầu tiên fail → onboarding wizard hang → bỏ.
**Action:** Pre-flight RST walk lớp CRUD path. Verify FE↔BE contract drift (Bug #7/#12/#15 sister class).

### Top 4: Approval email broadcast path (Phase 1 manual fallback)
**Persona affected:** P1 Solo (chị Linh broadcast điểm) + P4 Parent (chị Mai nhận noti)
**Drop-off risk:** 7/10 (P1) + 5/10 (P4)
**Bug catalog mapping:**
- GAP-702 (approval email NOT firing — Wave 105 fix DONE walk PARTIAL)
- GAP-703 (List-Unsubscribe + multipart/alternative MISSING — Wave 105 fix DONE walk PARTIAL)
**Impact:** Phụ huynh không nhận điểm → trust trust chị Linh + KiteHub đồng thời. Sau ngày 3-5 → bỏ.
**Action:** Walk verify email path live (SES sandbox đủ cho beta). Confirm GAP-702/703 walk evidence solid.

### Top 5: Billing per-student visibility (P1 + P2 + P4)
**Persona affected:** P1 Solo edge case Flow 1.3 + P2 Owner Flow 2.2 income dashboard + P4 Parent payment lookup
**Drop-off risk:** 6/10
**Bug catalog mapping:**
- GAP-293 income dashboard (Wave 100 Bucket B — close-loop status?)
- Wave 80+ retro 50% NONE walk; billing endpoints likely chưa walk
**Impact:** Phụ huynh hỏi học phí, chị Linh không trả lời tự tin → trust giảm; chị Hằng báo cáo doanh thu sai → switch Excel.
**Action:** Verify VND format `1.500.000đ` rendered + per-student breakdown correct + invoice PDF generation.

---

## §7. Recommendations

### 7.1 Immediate (this session OR next)

1. **REVERT Wave meta-6 Bucket A DONE flag** — re-classify PARTIAL per shutdown findings (`feature-ship-runtime-walk-mandate.md` v1.0.0 enforcement)
2. **Lock Plan D timeline:** 10 ngày prep + 3 tuần beta = 5 tuần total (NOT 5 ngày bare-minimum)
3. **File 17 gaps from Wave meta-6 Bucket A shutdown findings** (1 META + 16 individual bugs)
4. **Audit retro suite refresh** — sample additional 5-10 high-priority Wave 80+ features walk evidence (per `post-wave-audit-mandate.md` cadence)
5. **Cohort selection:** identify 1-3 P2 Owner-class friends (cohort criteria §4.2)

### 7.2 Block 1 — P0 BLOCKERS fix (~4-6 ngày)

6. Implement Bug #14 staff invitation email path (outbox + email consumer + SES template)
7. Implement Bug #17 user provisioning on accept (architecture decision investigation per `release-fix-retry-budget.md` §3.5 BEFORE first fix)
8. Verify GAP-704/705/706/711/712 JWT tenantId cluster walk evidence

### 7.3 Block 2 — RST walk verify (~2-3 ngày)

9. RST walk 5 critical flows: P1 F1.1 + F1.2, P2 F2.1 + F2.2, P4 F4.1
10. Per `feature-ship-runtime-walk-mandate.md` paste walk evidence vào gap closure

### 7.4 Block 3 — Polish (in-beta hotfix acceptable)

11. Sample 5-10 Wave 80+ features walk evidence backfill
12. UI feature-flag persona-mismatched routes (GAP-758 follow-up)
13. KC class-lifecycle E2E gate (GAP-759)
14. Address top 5 drop-off risk hotspots in priority order

### 7.5 Friend-beta launch (Week 2-5)

15. Send 1-3 friend invite (P2 Owner-only cohort) Week 3
16. Weekly retro sessions × 3 = 9 feedback rounds
17. Daily hot-fix cycles cho bugs surface
18. Pre-launch Phase 1.5 paid: ≥7/9 retro rounds → fewer than 3 P0 bugs surface 2 rounds liên tiếp

---

## §8. References

- Wave meta-6 Bucket A walk shutdown findings: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md`
- Wave 80+ retro audit: `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md`
- Release Lần 1 Phase 1 plan: `documents/03-planning/roadmap/release-1-plan-2026.md` §3 Phase 1 BETA
- Roles business rules: `documents/01-business/roles/rules.md` (P3 Manager defer Phase 2 mandate)
- FE source inventory: `kitehub/kitehub-frontend/src/app/(customer)/**` (Owner) + `kiteclass/kiteclass-frontend/src/app/(dashboard)/**` (multi-persona)
- META rule mandate: `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0
- Investigation-first rule: `.claude/rules/release-fix-retry-budget.md` v1.2.0 §3.5
- Pre-handoff verify rule: `.claude/rules/pre-handoff-self-test-completeness.md` v1.2.0 §2.4 admin-flow checklist + §3 post-fix re-walk mandate

---

## §9. Audit verdict

**Persona simulation MUST-HAVE matrix shipped, 17 features identified trong target 15-20 range. Friend-beta selection recommendation = P2 Owner-only cohort 1-3 friends. Plan D timeline verify: ~5 ngày bare-minimum prep PARTIAL TRUE (chỉ Block 1+2; Block 3 defer in-beta). Recommended timeline: 10 ngày prep + 3 tuần beta = 5 tuần total.**

**Top 5 drop-off risk hotspots mapped → bug catalog (17 + 46) cho prioritization.**

Per `feature-ship-runtime-walk-mandate.md` v1.0.0 + `release-fix-retry-budget.md` v1.2.0 §3.5 mandates, Plan D close-loop beta đòi hỏi runtime walk evidence cho mọi MUST-HAVE feature + investigation phase trước fix retry. Audit retroactive scope = Phase 2 BETA pre-flight check.
