# Beta Cohort Onboarding Playbook — 5-tenant manual hand-holding (Phase 1 BETA)

**Audience:** Solo dev / Beta coordinator (Mai role)
**Created:** 2026-05-26 (Wave beta-prep-1 Bucket F2)
**Status:** Phase 1 BETA — `[v1 chờ tư vấn pháp lý]`
**Owner:** @nguyenvankiet
**References:**
- Sister runbook: [`beta-invite-flow.md`](beta-invite-flow.md) — end-to-end flow Bước 1-5
- Sister runbook: [`tenant-init-handoff-runbook.md`](tenant-init-handoff-runbook.md) — admin approve → tenant ready
- Sister runbook: [`tenant-support-channels-runbook.md`](tenant-support-channels-runbook.md) — Zalo OA / email / phone escalation
- Sister runbook: [`support-escalation-runbook.md`](support-escalation-runbook.md) — quyết định kênh + SLA
- Decision: [`ADR-036`](../../02-architecture/adr/ADR-036-multi-branch-spike.md) — multi-branch filter Phase 1
- Rule: [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.3 + §2.4
- Rule: [`vn-localization-audit-checklist.md`](../../../.claude/rules/vn-localization-audit-checklist.md) §2 4 sections

---

## 1. Mục đích

Playbook hướng dẫn coordinator **hand-hold 5 tenant đầu tiên** trong Phase 1 BETA cohort. Khác `beta-invite-flow.md` (luồng kỹ thuật end-to-end), runbook này tập trung **process con người**: chuẩn bị, gọi điện, kèm tay, thu phản hồi.

Tại sao cần hand-hold thủ công 5 tenant đầu (không tự động hoá ngay)?

- **Phát hiện friction sớm:** dev đi cùng owner trong onboarding giúp surface UX bugs / docs gaps trước khi scale.
- **Trust capital:** P2 owner Việt Nam quen với mô hình "có người hỗ trợ riêng", không phải "tự đọc docs".
- **Feedback loop chặt:** 5 tenant × 7 ngày = 35 datapoint cụ thể về Beta scope.
- **Cohort retention:** sister docs `cohort-retention-tracking.md` đo retention; runbook này tạo input data.

Per `release-deploy-standard.md` §3.1 PRE-RELEASE — `[ ] Beta tenant invite mechanism` operational item.

---

## 2. Tiêu chí chọn 5 tenant đầu (shortlist criteria)

Trước khi gửi invite, coordinator shortlist theo bảng:

| # | Tiêu chí | Lý do | Ví dụ pass |
|---|---|---|---|
| 1 | **Persona = P2 Center Owner (chủ trung tâm)** | Phase 1 BETA scope chính | "Em chào chị, em là Hằng — Trung tâm Anh ngữ Sky Education, Q.3 TP.HCM" |
| 2 | **Số chi nhánh = 1** | ADR-036 quyết định Phase 1 chỉ accept single-branch; multi-branch → waitlist | Trung tâm Sky Education chỉ có 1 cơ sở Q.3 |
| 3 | **Quy mô 30-200 học sinh** | Tránh extreme: <30 = không đáng dùng SaaS; >200 = stress Phase 1 capacity | Sky Education ~85 học sinh |
| 4 | **Đã quản lý bằng Excel ≥ 6 tháng** | Có pain-point rõ, sẵn sàng đổi tool | "Em quản lý 5 lớp với Google Sheets, đếm điểm danh thủ công" |
| 5 | **Có 1-3 giáo viên cộng tác** | Đủ để test multi-user nhưng không quá phức tạp | 2 GV part-time + 1 trợ giảng |
| 6 | **Referral từ kênh tin cậy** | Friend-of-friend / Zalo edu group / coordinator gặp trực tiếp | Giới thiệu từ chị Hằng → chị Mai |
| 7 | **Sẵn lòng phản hồi 7 ngày liên tục** | Beta cần feedback, không phải "ship rồi quên" | Hỏi rõ: "Mình có thể gọi/nhắn em mỗi 2 ngày trong tuần đầu được không?" |
| 8 | **Không có yêu cầu compliance phức tạp Phase 1** | K-12 / tổ chức nhà nước defer Phase 3 | Trung tâm tư nhân dạy thêm OK; trường công K-12 defer |

**Pattern khuyến nghị cohort 5 tenant đầu:**

| Slot | Persona | Vai trò trong cohort |
|---|---|---|
| 1 | P2 Owner, 1 GV, 30-50 HS, IELTS | "Small + simple" — baseline |
| 2 | P2 Owner, 2 GV, 60-90 HS, Toán THCS | "Mid + multi-class" — multi-user test |
| 3 | P2 Owner, 1 GV part-time, 40-60 HS, Tin học | "Solo-shaped Owner" — edge case P1/P2 boundary |
| 4 | P2 Owner, 3 GV, 100-150 HS, Tiếng Hàn | "Larger + scheduling" — calendar stress |
| 5 | P2 Owner, 2 GV, 80-120 HS, Tiếng Nhật | "Backup + variety" — language domain diversity |

Coordinator KHÔNG được vượt 5 trong wave đầu. Nếu có 8 candidates → 5 đầu vào cohort, 3 sau vào waitlist + thông báo Phase 1.5.

---

## 3. Quy trình 5 ngày trước khi invite (D-5 → D-1)

### D-5: Shortlist + xác nhận liên hệ

- [ ] Coordinator chốt danh sách 5 candidates qua spreadsheet (Google Sheet riêng cohort)
- [ ] Mỗi candidate có: tên, email, SĐT, Zalo, persona, số HS, số GV, số chi nhánh
- [ ] Verify tiêu chí §2 (8 rows) — đánh dấu fail/pass mỗi tiêu chí
- [ ] Loại bỏ candidates fail ≥ 2 tiêu chí

### D-4 — D-3: First-contact (15 phút Zalo/phone)

- [ ] Nhắn Zalo / gọi điện từng candidate
- [ ] Pitch ngắn (2 phút): "KiteHub đang mở Beta cho 5 trung tâm trước launch. Mình giới thiệu nhanh cho chị xem có hợp không nha"
- [ ] Demo screen-share 5 phút (signup → dashboard → tạo lớp mẫu)
- [ ] Hỏi: "Tuần sau chị sẵn sàng dành 1 tiếng để mình hỗ trợ setup chính thức không?"
- [ ] Ghi nhận đồng ý / từ chối / hẹn lại

### D-2: Backup pool

- [ ] Nếu < 5 đồng ý → mở rộng shortlist từ pool kế tiếp
- [ ] Tuyệt đối **không** giảm tiêu chí §2 để đạt số 5; thà 3-4 chất lượng còn hơn 5 tệ

### D-1: Pre-send check

- [ ] Verify `kitehub.me` apex resolve OK (`curl -I https://kitehub.me`)
- [ ] Verify admin login working (per `pre-handoff-self-test-completeness.md` §2.4)
- [ ] Verify invite email template render OK (trigger 1 test invite tới chính email của coordinator)
- [ ] Verify Zalo OA OR support@kitehub.me phản hồi trong 15 phút
- [ ] Block lịch coordinator 7 ngày kế tiếp (15:00-17:00 daily cho support window)

---

## 4. Day 0 — Send invite (5 tenant simultaneous)

Per [`beta-invite-flow.md`](beta-invite-flow.md) Bước 1-3:

### 4.1 Admin issue invite tokens

- [ ] Login `/admin/beta-requests` với credential admin
- [ ] Approve từng candidate trong PENDING list → triggers email invite (token TTL 24h)
- [ ] **Quan trọng:** approve trong cùng 30 phút (không spread cả ngày) để cohort experience đồng nhất

### 4.2 Theo dõi mailbox 30-60 phút đầu

- [ ] Mở Resend / SES dashboard kiểm `delivered` status mỗi 10 phút
- [ ] Nếu `bounced` → liên hệ qua Zalo backup ngay (offer resend tới email khác)
- [ ] Nếu `pending > 30 phút` → escalate per [`support-escalation-runbook.md`](support-escalation-runbook.md)

### 4.3 Zalo follow-up sau 1 giờ

Nhắn từng tenant qua Zalo (template):

```
Em chào chị {ownerName},
Em vừa gửi link mời truy cập KiteHub Beta qua email {email} — chị check giúp em rồi click vào "Hoàn tất đăng ký" nhé.
Link có hiệu lực 24 giờ. Nếu chị cần em hỗ trợ qua điện thoại / screen-share thì nhắn em ngay.
— Mai (KiteHub team)
```

---

## 5. Day 1 — First-call setup (60 phút mỗi tenant)

Coordinator gọi từng tenant theo lịch đã hẹn. **Mỗi cuộc gọi = 1 tenant** — không gộp nhóm.

### 5.1 Pre-call checklist (5 phút trước cuộc)

- [ ] Mở `/admin/beta-requests/{id}` xem trạng thái signed-up chưa
- [ ] Mở Zalo chat history với tenant — refresh context
- [ ] Mở `tenant-init-handoff-runbook.md` mở sẵn tab Pre-conditions

### 5.2 Cuộc gọi — 60 phút breakdown

| Phút | Hoạt động | Pass criterion |
|---|---|---|
| 0-5 | Chào + hỏi thăm + reaffirm purpose | Tenant relaxed, đồng ý record (nếu muốn lưu) |
| 5-15 | Signup flow live (screen-share 2 chiều) | Tenant nhập password + chọn subdomain; thành công |
| 15-25 | Tạo lớp đầu tiên cùng nhau | 1 lớp được tạo, có ≥ 1 học sinh, ≥ 1 lịch học |
| 25-40 | Hướng dẫn 3 tính năng chính: điểm danh, tính học phí, thông báo phụ huynh | Tenant tự làm 1 lần qua, hiểu UI |
| 40-50 | Hỏi expectation tuần đầu + pain-point cũ | Coordinator ghi note vào cohort sheet |
| 50-55 | Hướng dẫn support channels (Zalo OA + email) | Tenant lưu số + Zalo OA |
| 55-60 | Confirm daily check-in lịch + AC tuần đầu | Tenant đồng ý nhắn báo cáo mỗi tối 20h trong 7 ngày |

### 5.3 Post-call (10 phút)

- [ ] Ghi note vào cohort sheet (3 highlights + 3 friction)
- [ ] File `documents/04-quality/audits/persona-review/2026-MM-DD-cohort-day-1-{tenant-slug}.md` nếu có insight quan trọng
- [ ] Send Zalo follow-up: "Cảm ơn chị {ownerName} đã setup cùng em. Em sẽ check-in lúc 20h tối nay nha"

---

## 6. Day 2-7 — Daily check-in (15 phút/tenant)

### 6.1 Khung giờ cố định 20:00-21:00 mỗi tối

Coordinator nhắn Zalo từng tenant theo template:

```
Em chào chị {ownerName},
Hôm nay chị dùng KiteHub có gì gặp khó khăn không ạ?
Em hỏi nhanh 3 câu cho cohort survey:
  1. Tính năng nào dùng nhiều nhất hôm nay?
  2. Có chỗ nào confusing không?
  3. Có chỗ nào chị muốn KiteHub thêm/sửa không?
— Mai
```

### 6.2 Phản hồi SLA

- < 30 phút → coordinator nhắn lại trong 10 phút
- 30 phút - 2 giờ → 1 giờ
- > 2 giờ → 4 giờ (chấp nhận trong trường hợp tenant bận giờ dạy)
- Im lặng > 24 giờ → escalate per [`support-escalation-runbook.md`](support-escalation-runbook.md) Tier 2 (gọi điện)

### 6.3 Cohort log

Mỗi tối, coordinator update cohort sheet với:

| Tenant | Day | Activity (Y/N) | Friction count | Highlight | Next-step |
|---|---|---|---|---|---|
| Sky Education | Day 2 | Y | 1 (email tích hợp không gửi) | "Em rất thích báo cáo doanh thu" | GAP-XXX |

---

## 7. Day 7 — Feedback collection + cohort retro

### 7.1 Survey ngắn (Google Form, 10 phút)

10 câu hỏi:

1. NPS 0-10: "Chị recommend KiteHub cho bạn bè trong ngành ở mức nào?"
2. Top 3 tính năng dùng nhiều nhất
3. Top 3 friction lớn nhất
4. Có muốn tiếp tục sau Beta?
5. Mức giá chị chấp nhận trả/tháng cho Phase 2 GA?
6. So với Excel/Google Sheets cũ, KiteHub: tốt hơn / tệ hơn / tương đương?
7. Phản hồi support: hài lòng (1-5)?
8. UI: dễ dùng (1-5)?
9. Documentation: đủ không (1-5)?
10. Open: chia sẻ thêm gì cho team KiteHub?

### 7.2 Coordinator retro (60 phút sau khi 5 tenant đã trả survey)

- Tổng hợp NPS → target ≥ 6 cho 4/5 tenant để pass Phase 1 retention gate
- Đếm friction count → top 5 friction → tạo gap files (GAP-XXX) per `audit-to-gap-pipeline.md`
- Quyết định: nâng cohort lên 10 (Phase 1.5 prep) HAY giữ 5 + fix friction trước?

### 7.3 Tenant thank-you

Email + Zalo thank-you tới mỗi tenant. Đề xuất: gift card 200.000đ / 1 tháng miễn phí Phase 2 cho tenant phản hồi đầy đủ.

---

## 8. Failure modes + recovery

| Failure mode | Trigger | Recovery |
|---|---|---|
| **Tenant không signup trong 24h** | Token expired; email vào spam | Resend invite per `beta-invite-flow.md` Bước 3.5; Zalo phone backup |
| **Tenant signup nhưng không login lại Day 1** | Quên password; subdomain sai | Reset password via admin tool; Zalo screen-share |
| **Tenant gặp bug Day 2-7** | Production bug Phase 1 chưa cover | File P0 hotfix gap; thông báo tenant ETA; offer workaround |
| **Tenant churn Day 3-5** | Không thấy giá trị | Coordinator gọi điện 30 phút tìm hiểu lý do; ghi feedback; offer 1 tháng Phase 2 miễn phí nếu willing to retry |
| **AWS account suspended** | (Wave 105 incident GAP-612 pattern) | Pause cohort + thông báo tenant qua Zalo; restore AWS theo `aws-suspension-no-notification-email.md`; resume cohort + giải thích delay |
| **Coordinator burnout** | 5 tenant × 7 ngày × 15 phút/tối = ~9 giờ; cộng setup calls 5 tiếng → ~14 giờ trong tuần | Phase 1 cap 5 tenant cố định; Phase 1.5 thuê thêm 1 support agent trước khi scale lên 10 |

---

## 9. Standards reference

- `pre-handoff-self-test-completeness.md` §2.3 email-driven + §2.4 admin flow per cuộc gọi setup
- `vn-localization-audit-checklist.md` §2 4 sections — Zalo template VN + VND quote + Vietnamese tone
- `dev-readable-doc-language.md` §2 — narrative tiếng Việt
- `release-deploy-standard.md` §3.1 — beta tenant invite mechanism operational checklist
- `output-review-mandate.md` §3 — playbook review standard (this file applies user-manual content discipline + professional-manual narrative)

---

## 10. Log

- **2026-05-26 (v1.0.0):** Playbook created cho Wave beta-prep-1 Bucket F2. Closes GAP-372 follow-up operational gap (code shipped Wave 33; manual process docs missing). Reviewer: @nguyenvankiet (solo-dev). Per `pre-handoff-self-test-completeness.md` §2.4 admin flow + sister `beta-invite-flow.md` end-to-end. Tiêu chí 8-row shortlist + 5-day pre-invite + Day 0-7 cadence + failure modes 6-row.
