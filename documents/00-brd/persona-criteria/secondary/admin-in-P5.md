# Acceptance Criteria — Admin Staff in P5 K-12 School (Văn phòng + Giáo vụ)

**Trạng thái:** 🟡 DRAFT v1
**Persona ID:** Admin × P5
**Persona name (VN):** Nhân viên hành chính trường K-12 (văn phòng + giáo vụ + thư viện + y tế + bảo vệ + IT)
**Persona name (EN):** Admin Staff in K-12 School (Office + Academic Affairs + ancillary)
**Last-Updated:** 2026-04-30
**Reviewer (Phase 1 — author):** Agent D (Wave Secondary-Persona-AC, GAP-153 Phase 1)
**Reviewer (Phase 2 — domain expert):** TBD — Real giáo vụ + văn phòng staff + Hiệu phó + MOET education expert + Product Owner (deferred to GAP-152)
**Tier:** 1 Primary (USER PRIORITY tenant context)
**Tracking:** GAP-153 Phase 1 → GAP-152
**Tenant context:** Public/Private K-12 School (P5)
**Role:** Admin Staff (secondary persona, organized hierarchy)
**Heaviest system users:** Văn phòng (general office) + Giáo vụ (academic affairs)
**Legal compliance:** TT 22/2021, TT 32/2020, TT 107/2017 financial reporting, Bộ luật Lao động 2019 (employment), Luật Trẻ em 2016 (staff vetting oversight)

---

## 0. Context

### Scale assumption (from `personas-catalog.md` §Secondary Personas + sibling `../P5-k12-school.md`)
- **Tenant scale:** 500–3000 học sinh, 50+ giáo viên, **15 admin/staff** organized hierarchy:
  - **Văn phòng (general office):** 3–5 nhân viên — văn thư, hành chính tổng hợp, lưu trữ học bạ, MoET reporting
  - **Giáo vụ (academic affairs):** 2–3 nhân viên — quản lý lịch dạy, exam scheduling, điểm số oversight, học bạ
  - **Thư viện (library):** 1–2 nhân viên — book lending, reading-room scheduling
  - **Y tế (health/nurse):** 1–2 nhân viên — sổ y tế HS, vaccination tracking, child-injury reporting
  - **Bảo vệ (security):** 2–4 nhân viên (làm ca) — student check-in/out, visitor log
  - **IT staff:** 1–2 nhân viên — system admin, bulk import, troubleshooting, integrations với MoET portal
- **Workload pattern:**
  - **Daily peak (văn phòng + bảo vệ):** 06:30–07:30 (HS đến trường, parent drop-off, visitor log) + 16:30–17:30 (HS về)
  - **Weekly peak (giáo vụ):** Thứ Sáu (review điểm tuần, prep cho thi cuối tuần); Thứ Hai (publish lịch tuần)
  - **Monthly peak (văn phòng):** Cuối tháng — báo cáo PCGD (Phổ cập GD) cho Phòng GD&ĐT
  - **Semester peak (giáo vụ + văn phòng):** Cuối HK1 (cuối tháng 12) + cuối HK2 (cuối tháng 5) — chốt điểm + in học bạ + báo cáo MoET TT 22/2021
  - **Annual peak (mọi role):** Tháng 8 (chuẩn bị năm học) — bulk import, parent vetting, staff vetting renewal, schedule generation; Tháng 6 (graduation, transcript export)
- **Heaviest system users:** Văn phòng + Giáo vụ (chiếm ~80% admin time on system)

### Role differentiation (CRITICAL — RBAC + MOET-mandated workflow)
| Role | Scope | MoET-mandated artifacts | Daily core action |
|------|-------|------------------------|-------------------|
| **Văn phòng** (general office) | Học bạ, hồ sơ HS, MoET reports, communications | TT 32/2020 (quản lý hồ sơ), TT 107/2017 (báo cáo tài chính) | Generate báo cáo PCGD, lưu học bạ, xử lý chuyển trường |
| **Giáo vụ** (academic affairs) | Lịch dạy, exam workflow, điểm số validation, học bạ generation | TT 22/2021 (đánh giá HS), TT 32/2018 (chương trình GDPT) | Duyệt lịch tuần, oversee teacher điểm input, finalize học bạ |
| **Thư viện** (library) | Book lending, reading-room booking | — | Track book loans, send overdue reminders |
| **Y tế** (health/nurse) | Sổ y tế HS, vaccination, injury report | Luật Trẻ em 2016 — mandatory child-injury reporting | Update health records, alert parent for sick HS |
| **Bảo vệ** (security) | Student check-in/out log, visitor log | — | Scan student card, log visitors, alert văn phòng for suspicious |
| **IT staff** | System admin, bulk import, MoET portal sync | — | Run bulk import, troubleshoot, sync with cổng giao dịch điện tử BHXH / TCT / MoET |

### Revenue tier mapping
- **Expected tier:** ENTERPRISE only — quy mô + MoET compliance + multi-role hierarchy buộc
- **Reason:** Public school có ngân sách hạn chế nhưng có legal mandate buộc phải dùng (parent portal, học bạ digital); private school có flexibility tài chính nhưng yêu cầu cao về reporting + branding. Cùng tier, khác config.

### Real-world reviewer profile
- **Acting role:** "Trần Thị Hương, giáo vụ trường THCS công lập 800 HS Hà Nội, làm giáo vụ 8 năm, đã dùng VnEdu + Excel — kỳ vọng KiteClass thay thế cả 2 + giảm 50% thời gian báo cáo MoET"
- **Critical concerns:**
  1. **MoET reporting workflow** — báo cáo PCGD + học bạ + báo cáo tài chính đúng format Thông tư; sai 1 trường = bị phòng GD trả về (cost: 2-3 ngày làm lại)
  2. **Staff vetting workflow** — Luật Trẻ em 2016 Đ.25 mandate background check; vi phạm = mất giấy phép trường tư hoặc kỷ luật hiệu trưởng trường công
  3. **Teacher payroll batch** — 50 teachers × MoET payscale + allowances + BHXH/BHYT/TNCN; không thể thiếu hoặc trễ (vi phạm Bộ luật Lao động Đ.97)
  4. **Bulk parent communication** — 800 HS × 1.5 parents = 1200 parents Zalo OA; không thể spam toàn trường khi chỉ cần 1 lớp
  5. **School-year calendar management** — academic year + holiday + exam periods + 35 weeks học chuẩn theo MoET
  6. **Emergency school closure announcement** — 1200 parents trong ≤5 phút khi có bão / dịch bệnh / sự cố CSVC
  7. **Document/transcript request** — graduation transcripts (học bạ chính thức), transfer-out records → trường mới; không thể delay quá 5 ngày làm việc theo TT 32/2020
  8. **Child safety incident triage** — y tế / bảo vệ / GVCN co-ordinate khi có sự cố; legal mandate báo cáo phụ huynh + cơ quan có thẩm quyền theo Luật Trẻ em 2016

---

## 1. Onboarding AC

Initial admin account provisioning per role, RBAC permission verification, MoET role hierarchy compliance.

- [ ] **AC-ONBOARD-001:** Hiệu trưởng tạo 15 admin/staff accounts với 6 roles distinct (văn phòng / giáo vụ / thư viện / y tế / bảo vệ / IT) trong ≤2 giờ
  - **Test:** Hiệu trưởng upload xlsx 15 staff với cột [họ tên, email, phone, role dropdown 6 options, default permissions auto] → bulk create → invite email + first-login MFA setup; mỗi role có default permissions preset (văn phòng thấy hồ sơ HS + reporting; bảo vệ chỉ thấy student check-in app; y tế thấy sổ y tế); Phó CM có thể edit permissions per-staff
  - **Fail signal:** Phải tạo từng account; không có 6 role presets chuẩn K-12; permissions phải config từng module một; bảo vệ thấy được điểm HS (vi phạm RBAC)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (xlsx import), GAP-058 (role hierarchy)

- [ ] **AC-ONBOARD-002:** Bulk staff vetting workflow — upload CCCD + bằng cấp + LLTP cho 15 admin/staff theo Luật Trẻ em 2016 + Decree 56/2017; track audit log
  - **Test:** Văn phòng / HR upload bulk staff records (xlsx + zip files cho mỗi staff: CCCD scan, bằng tốt nghiệp, LLTP số 2 không quá 6 tháng, ảnh 3×4) → hệ thống lưu MinIO encrypted → admin Kite verify từng staff → khi đủ verify-pass staff mới có quyền access HS data → audit log ghi rõ ngày upload + người verify + timestamp; renewal alert ≤30 ngày trước hạn LLTP (LLTP có hiệu lực 6 tháng theo Luật Lý lịch tư pháp 2009)
  - **Fail signal:** Không có vetting workflow → vi phạm Luật Trẻ em 2016 Đ.25 + Decree 56/2017; evidence lưu plaintext; không alert renewal LLTP; staff thấy được HS data trước khi vetting xong
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — staff vetting), GAP-058 (role hierarchy)

- [ ] **AC-ONBOARD-003:** Văn phòng + giáo vụ first login thấy MoET dashboard với báo cáo deadlines + missing data alerts
  - **Test:** Giáo vụ login → dashboard "MoET Compliance" với widgets: "Báo cáo PCGD due in 5 days", "Học bạ HK1 — 50/800 HS thiếu điểm môn Toán", "Báo cáo tài chính TT 107/2017 due cuối quý"; click widget → drill-down danh sách thiếu / template prefilled cho báo cáo
  - **Fail signal:** Dashboard generic không MoET-aware; không có deadline tracker; missing data không pinpoint HS / lớp / môn cụ thể; template không prefill từ data hệ thống
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (báo cáo MoET official report)

---

## 2. Daily Operations AC

Role-specific workflows — văn phòng (general ops), giáo vụ (academic affairs), IT staff (bulk import + integration), bảo vệ (security), y tế (health).

- [ ] **AC-OPS-001:** Văn phòng generate monthly báo cáo PCGD (Phổ cập Giáo dục) cho Phòng GD&ĐT trong ≤30 phút theo format TT chuẩn
  - **Test:** Văn phòng click "Generate báo cáo PCGD tháng 5" → hệ thống pull data: tổng HS theo độ tuổi 6-14, HS bỏ học, HS chuyển trường, HS mới nhập học → fill template báo cáo PCGD format Sở GD-ĐT + ký số hiệu trưởng (HSM) → export PDF + XML để upload cổng MoET
  - **Fail signal:** Phải tự gõ Excel; không có template TT chuẩn; không match XML format Sở GD-ĐT; phải print và sign tay (mất 2 ngày)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (báo cáo MoET official report)

- [ ] **AC-OPS-002:** Giáo vụ oversee teacher điểm input — dashboard hiển thị 50 GV × 12 môn × 30 lớp coverage status; alert nếu GV nhập điểm trễ ≥7 ngày sau exam
  - **Test:** Giáo vụ mở "Điểm Coverage Dashboard" cuối tuần → grid 50 GV × 12 môn → cell màu xanh (đã đủ điểm), vàng (thiếu 1 cột), đỏ (thiếu nhiều); click cell đỏ → popup "GV Toán B chưa nhập điểm GK lớp 7A (deadline đã quá 5 ngày)" + 1-click "Send reminder" → SMS + email + Zalo tới GV
  - **Fail signal:** Không có coverage dashboard; phải check từng GV manual; không có alert tiering; reminder phải gửi tay
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (báo cáo MoET), GAP-061 (promotion/retention logic — depends on complete grades)

- [ ] **AC-OPS-003:** Giáo vụ finalize học bạ HK1 cho 800 HS — auto-compute ĐTBmHK + suggest hạnh kiểm + Hiệu trưởng ký số → publish trong ≤2 giờ batch
  - **Test:** Giáo vụ click "Generate học bạ HK1" → hệ thống auto-compute per HS theo TT 22/2021 (ĐTBmHK = (TX + GK×2 + CK×3) / 6) + pull hạnh kiểm GVCN-finalized + nhận xét GVCN + nhận xét Phó CM (nếu có) → review queue cho giáo vụ + Hiệu trưởng → hiệu trưởng batch-sign 800 học bạ với HSM token → publish to parent portal + lưu archive 5 năm theo TT 32/2020
  - **Fail signal:** Phải tính từng HS; ký từng học bạ một; không archive theo TT 32/2020; parent không xem được qua portal
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (báo cáo MoET), GAP-061 (promotion/retention logic), GAP-052 (parent portal)

- [ ] **AC-OPS-004:** Văn phòng manage school-year calendar — academic year + 2 semesters + holidays VN + exam periods + 35 teaching weeks theo MoET
  - **Test:** Văn phòng vào "School Year Calendar" → wizard "Setup year 2026-2027" → start 5/9/2026, end 31/5/2027, 2 HK (HK1: 5/9 → 31/12, HK2: 6/1 → 31/5) → import VN holidays preset (Tết, 30/4, 1/5, 2/9, 20/11, 22/12) + exam periods (cuối HK1: 15-25/12, cuối HK2: 15-31/5) → calculate auto 35 teaching weeks; published calendar visible cho all GV/HS/parent qua portal
  - **Fail signal:** Phải tự config từng tuần; không có VN holidays preset; không calculate 35 weeks theo MoET; calendar không publish được cho stakeholders
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-053 (academic year/semester structure)

- [ ] **AC-OPS-005:** IT staff oversee bulk import 800 HS + 1200 parents đầu năm — staging → validation → import → notification trong ≤4 giờ
  - **Test:** IT staff nhận xlsx 800 HS từ tuyển sinh / phòng GD → upload "Bulk Import Staging" → validation report (missing fields, duplicates, parent contact format) → fix issues co-ordinate với văn phòng → click "Import" → batch process với progress bar (~30 min) → kết quả: 800 student accounts + ~1200 parent accounts auto-link → SMS/Zalo gửi parent credentials; failure list export để follow-up
  - **Fail signal:** Staging UI không validate trước import; phải import theo batch nhỏ <100 (timeout); duplicate parent khi sibling cùng trường; credentials gửi plaintext qua email không encrypted
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-051 (bulk import xlsx), GAP-052 (parent portal), GAP-063 (SMS/Zalo)

- [ ] **AC-OPS-006:** Bảo vệ daily student check-in/out log via card scan + auto-notify parent qua Zalo OA when child arrived/left
  - **Test:** Bảo vệ scan student card lúc 07:00 → student status = "đã đến trường"; lúc 16:30 scan lúc về → status = "đã về"; auto-notify Zalo OA tới parent: "Con bạn (Nguyễn Văn A, 7A) đã đến/về trường lúc HH:MM"; nếu HS không scan check-out trong 30 phút sau giờ tan trường → alert văn phòng + GVCN
  - **Fail signal:** Manual check-in (bảo vệ ghi sổ giấy); không tự notify parent; không alert khi HS missing; không integrate với attendance system (GVCN không thấy)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-060 (period-based attendance — extends to school check-in/out), GAP-063 (SMS/Zalo)

- [ ] **AC-OPS-007:** Y tế update sổ y tế HS — health record per HS + vaccination tracker + injury incident log + alert parent khi HS sick at school
  - **Test:** Y tế mở sổ y tế HS Nguyễn Văn A → tab "Vaccination" hiển thị MMR / Sởi / DPT theo schedule MoH → flag missing vaccines; tab "Incident" log "HS bị sốt 38.5°C lúc 09:30, đã cho HS nghỉ phòng y tế" → auto-notify parent + GVCN qua Zalo + SMS; tab "Health History" preserve persistent conditions (asthma, allergy)
  - **Fail signal:** Không có sổ y tế module; vaccination không track theo lịch MoH; injury log không required theo Luật Trẻ em 2016 Đ.27; parent không nhận notify timely
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — health/safety), Related: Luật Trẻ em 2016 Đ.27

---

## 3. Financial AC

Văn phòng (kế toán cụm) role — payroll, fee collection oversight, VAT invoicing oversight, MOET financial reporting.

- [ ] **AC-FIN-001:** Văn phòng / kế toán run teacher payroll batch 50 teachers — fixed salary per MoET payscale + allowances (phụ cấp đứng lớp + phụ cấp thâm niên + chức vụ) + BHXH/BHYT/TNCN
  - **Test:** Văn phòng click "Run payroll" tháng 5 → tính per teacher theo MoET payscale (hệ số lương × mức lương cơ sở 1,800,000 VND 2026) + Σ phụ cấp + (-) BHXH 8% + (-) BHYT 1.5% + (-) TNCN bậc thang → 50 payslips PDF + bank transfer file MT940 ready để upload Vietcombank/BIDV/Agribank; export báo cáo C12-TS BHXH + Mẫu 02/KK-TNCN cho cổng giao dịch BHXH + TCT
  - **Fail signal:** Không có MoET payscale preset (phải nhập tay hệ số lương); không tính phụ cấp đặc thù giáo viên; không xuất file MT940 / C12-TS / Mẫu 02; báo cáo phải làm tay trên Excel rồi upload cổng MoET
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-057 (payroll commission), GAP-062 (payroll bank integration), Related: Bộ luật Lao động 2019, MoET payscale

- [ ] **AC-FIN-002:** Văn phòng oversee fee collection — public school (low fee + scholarship management) hoặc private school (flexible pricing); reconcile mixed payment methods
  - **Test:**
    - **Public school:** Văn phòng mở "Fee Collection" → list HS với học phí + BHYT + bán trú + đồng phục + scholarship discount (nếu thuộc diện hỗ trợ) → bulk generate notification cho 800 HS → reconcile với KBNN (Kho bạc Nhà nước) cho học phí công lập
    - **Private school:** Văn phòng tier-based pricing per HS + sibling discount + mid-year transfer pro-rata → generate VAT e-invoice nếu parent yêu cầu (NĐ 123/2020)
  - **Fail signal:** Không có scholarship tier (vi phạm Nghị định 81/2021/NĐ-CP về miễn giảm học phí); reconcile manual; không support KBNN (public) hoặc VAT e-invoice (private)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT), Related: NĐ 81/2021/NĐ-CP

- [ ] **AC-FIN-003:** Văn phòng generate báo cáo tài chính TT 107/2017 cuối quý — revenue + cost + budget per department + MoET-mandated format
  - **Test:** Văn phòng click "Báo cáo tài chính Q1/2026 (TT 107/2017)" → hệ thống pull: revenue (học phí + KBNN cấp + ngân sách + tài trợ), cost (lương + BHXH/BHYT + CSVC + giáo dục), budget per department (Toán / Văn / Anh / KHTN / KHXH / GDCD / Tin / Công Nghệ / Thể Dục / Âm Nhạc / Mỹ Thuật / Y tế / Thư viện) → export PDF + XML chuẩn TT 107/2017 → ký số hiệu trưởng (HSM) → upload cổng MoET
  - **Fail signal:** Không có template TT 107/2017; budget per department phải làm Excel tay; không support XML format MoET; phải in và bàn giao tay
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-185 (billing/VAT — financial reporting), Related: TT 107/2017

---

## 4. Communication AC

Bulk parent communication, internal staff messaging, escalation queue, MoET communication.

- [ ] **AC-COMM-001:** Văn phòng send bulk parent notification school-year calendar đầu năm — 1200 parents qua Zalo OA + SMS fallback trong ≤5 phút với delivery tracking
  - **Test:** Văn phòng tạo notification "Năm học 2026-2027 khai giảng 5/9/2026, lịch tuần 1 đính kèm" → audience "Tất cả phụ huynh" → preview 1200 parents → send → Zalo OA primary + SMS fallback nếu Zalo fail; delivery dashboard "delivered: 1187/1200, failed Zalo + SMS retry: 13 → manual call list export"
  - **Fail signal:** Không có Zalo OA + SMS fallback dual-channel; không có delivery receipt; failure không export call list cho văn phòng follow-up qua điện thoại
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-002:** Giáo vụ send targeted alert per khối / lớp — "Lớp 7A đổi giờ tiết 3 hôm nay" → chỉ 25 phụ huynh nhận
  - **Test:** Giáo vụ mở "Send Alert" → audience filter "Class = 7A" → preview "25 phụ huynh, 25 HS" → send → 25 parents nhận Zalo + 25 HS nhận in-app notification; system loga audience size trước send (KHÔNG được spam toàn trường)
  - **Fail signal:** Chỉ all-or-nothing; preview không hiển thị size; phụ huynh các lớp khác cũng nhận (false positives); không loga audit
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-COMM-003:** Văn phòng escalation queue parent complaint → Hiệu trưởng + Phó CM với SLA 48h; tự động escalate nếu vượt hạn
  - **Test:** Parent gửi complaint qua portal "GVCN không thông báo con tôi vắng học" → văn phòng triage → category = "Academic" → auto-route Phó CM (SLA 48h start); 47h chưa response → auto-escalate Hiệu trưởng + alert văn phòng; resolution log ghi action + parent response + closure date; parent xem status qua portal real-time
  - **Fail signal:** Email rời rạc → no audit; SLA không track; không tự escalate; parent không thấy status; closure không document → khiếu nại lên Sở GD-ĐT khó defend
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-052 (parent portal — complaint workflow)

---

## 5. Edge Cases AC

Emergency school closure, peak enrollment overload, MoET / Tax audit, child safety incident triage.

- [ ] **AC-EDGE-001:** Emergency school closure announcement — Hiệu trưởng broadcast 1200 parents trong ≤5 phút khi có bão / dịch / sự cố CSVC
  - **Test:** Hiệu trưởng (hoặc Phó HT delegate) trigger "Emergency Broadcast" → template "Trường nghỉ học [tên ngày] do [lý do]; thông báo lại lúc [time]" → preview audience "Tất cả phụ huynh + GV" 1200+50 = 1250 → confirm → multi-channel push Zalo + SMS + email + push notification mobile; delivery receipt within 5 phút; backup: nếu Zalo OA down, fall back to SMS pure (delivery 99% trong 10 phút)
  - **Fail signal:** Không có emergency template; chỉ Zalo (single point of failure); không có push mobile; delivery >15 phút (parent đã đưa con đi học rồi); backup channel không có
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-063 (SMS/Zalo notification)

- [ ] **AC-EDGE-002:** MoET audit by Sở GD-ĐT — văn phòng generate full data export 5 năm theo TT 32/2020 trong ≤4 giờ
  - **Test:** Sở GD-ĐT yêu cầu audit năm học 2021-2026 → Hiệu trưởng trigger "MoET Audit Export" → văn phòng prep package: học bạ all HS + sổ điểm + báo cáo PCGD + báo cáo tài chính TT 107/2017 + biên bản hội đồng + audit logs (WORM-immutable) → encrypted ZIP download + bàn giao MoET qua cổng giao dịch điện tử; integrity hash để Sở verify; audit log ghi rõ ai trigger, ai download, timestamp
  - **Fail signal:** Export không có audit log; records mutable / deletable; không match TT 32/2020 5-year retention; không có WORM immutability; Sở GD-ĐT reject vì format
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-184 (data retention), Related: TT 32/2020

- [ ] **AC-EDGE-003:** Child safety incident triage — y tế / bảo vệ / GVCN / văn phòng co-ordinate workflow theo Luật Trẻ em 2016 + báo cáo cơ quan có thẩm quyền
  - **Test:** Bảo vệ phát hiện sự cố (ví dụ: HS bị thương ở sân chơi) → trigger "Child Safety Incident" → workflow: (1) y tế first response + log; (2) GVCN notify parent ≤15 phút; (3) văn phòng escalate Hiệu trưởng nếu severity ≥ medium; (4) nếu severity = high (gãy tay, nghi ngờ abuse, etc.) → mandatory report to UBND xã/phường + công an + Sở GD-ĐT trong 24h; (5) audit log full timeline; (6) parent có quyền access báo cáo qua portal
  - **Fail signal:** Workflow ad-hoc qua Zalo group; không có severity classification; không mandatory report khi cần (vi phạm Luật Trẻ em 2016 Đ.27 + Đ.51); audit log không cho parent access (vi phạm minh bạch)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-186 (child protection — incident workflow), Related: Luật Trẻ em 2016 Đ.27 + Đ.51

---

## 6. Exit AC

Admin role change / departure, role transfer to successor, data handover, transcript request handling.

- [ ] **AC-EXIT-001:** Admin staff resignation — handover responsibilities + revoke access ≤24h sau official last day; LLTP renewal alert disabled; audit log preserved
  - **Test:** Văn thư Hương thôi việc 30/6 → Hiệu trưởng trigger "Offboard staff" wizard → (1) reassign work-in-progress (5 báo cáo PCGD draft) cho văn thư khác / Phó HT; (2) export Hương's audit log cho HR file; (3) revoke account access 30/6 23:59; (4) historical records preserved (5 năm theo TT 32/2020); (5) Hương vẫn xuất hiện trong audit log nhưng marked "former staff"; (6) LLTP renewal alert disabled
  - **Fail signal:** Phải reassign manual; access không revoke timely; historical records bị xóa khi delete account; audit log reference Hương biến mất → break compliance trail; LLTP renewal alert vẫn fire (false positive)
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-058 (role hierarchy — offboard), GAP-184 (data retention), GAP-186 (child protection — staff vetting renewal)

- [ ] **AC-EXIT-002:** Document/transcript request handling — HS chuyển trường hoặc tốt nghiệp; văn phòng xuất học bạ chính thức + transfer-out records trong ≤5 ngày làm việc theo TT 32/2020
  - **Test:**
    - **Transfer-out:** Parent yêu cầu chuyển HS sang trường khác → văn phòng mở "Transfer-out Wizard" → input trường nhận → hệ thống generate package: học bạ chính thức (đã ký số HSM hiệu trưởng) + sổ điểm + giấy chuyển trường (giấy giới thiệu) + bản sao y tế → encrypted package gửi trường nhận qua portal liên trường (nếu có) hoặc email; SLA 5 ngày làm việc tracker
    - **Graduation:** Cuối lớp 9 (THCS) hoặc lớp 12 (THPT), hệ thống batch generate học bạ chính thức + bằng tốt nghiệp template MoET → Hiệu trưởng batch-sign HSM → archive 5 năm + parent có quyền download qua portal
  - **Fail signal:** Phải in tay học bạ; ký từng cuốn một (Hiệu trưởng mất 2-3 tuần cho khóa graduation); không có portal liên trường; SLA không track; vi phạm TT 32/2020
  - **Status:** PASS / PARTIAL / FAIL
  - **Linked gap:** GAP-055 (báo cáo MoET — học bạ format), GAP-061 (promotion/retention logic), GAP-184 (data retention)

---

## Scoring

**Total ACs:** 19 (3 Onboarding + 7 Daily Ops + 3 Financial + 3 Communication + 3 Edge Cases + 2 Exit)

| Status | Definition |
|--------|------------|
| **PASS** | Meets AC fully — system handles scenario without manual workaround |
| **PARTIAL** | Partial implementation — works but with friction, edge case missing, or manual step required |
| **FAIL** | Missing entirely — no system support, blocks persona |

**Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100**

| Coverage | Verdict |
|----------|---------|
| ≥85% | ✅ Persona fully supported |
| 60-84% | ⚠️ Persona partially supported |
| 30-59% | 🔴 Persona NOT supported (major gaps) |
| <30% | ❌ Persona NOT viable |

---

## Gap Linkage Summary

ACs filled at GAP-152 review time. Pre-filled with linked existing gaps:

| AC ID | Status | Gap ID | Gap Status | Priority |
|-------|:------:|--------|:----------:|:--------:|
| AC-ONBOARD-001 | TBD | GAP-051, GAP-058 | 🔵 OPEN | P0 |
| AC-ONBOARD-002 | TBD | GAP-186, GAP-058 | 🔵 OPEN | P0 |
| AC-ONBOARD-003 | TBD | GAP-055 | 🔵 OPEN | P0 |
| AC-OPS-001 | TBD | GAP-055 | 🔵 OPEN | P0 |
| AC-OPS-002 | TBD | GAP-055, GAP-061 | 🔵 OPEN | P0 |
| AC-OPS-003 | TBD | GAP-055, GAP-061, GAP-052 | 🔵 OPEN | P0 |
| AC-OPS-004 | TBD | GAP-053 | 🔵 OPEN | P1 |
| AC-OPS-005 | TBD | GAP-051, GAP-052, GAP-063 | 🔵 OPEN | P0 |
| AC-OPS-006 | TBD | GAP-060, GAP-063 | 🔵 OPEN | P0 |
| AC-OPS-007 | TBD | GAP-186 | 🔵 OPEN | P0 |
| AC-FIN-001 | TBD | GAP-057, GAP-062 | 🔵 OPEN | P1 |
| AC-FIN-002 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-FIN-003 | TBD | GAP-185 | 🔵 OPEN | P1 |
| AC-COMM-001 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-002 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-COMM-003 | TBD | GAP-052 | 🔵 OPEN | P0 |
| AC-EDGE-001 | TBD | GAP-063 | 🔵 OPEN | P0 |
| AC-EDGE-002 | TBD | GAP-184 | 🔵 OPEN | P1 |
| AC-EDGE-003 | TBD | GAP-186 | 🔵 OPEN | P0 |
| AC-EXIT-001 | TBD | GAP-058, GAP-184, GAP-186 | 🔵 OPEN | P0 |
| AC-EXIT-002 | TBD | GAP-055, GAP-061, GAP-184 | 🔵 OPEN | P0 |

**Candidate NEW gaps to file at review time** (state-check qua `audit-to-gap-pipeline.md` Step 2.5 trước khi filing):
- MoET báo cáo PCGD generator (AC-OPS-001) — extends GAP-055
- LLTP renewal alert (AC-ONBOARD-002, AC-EXIT-001) — likely no current gap, child-protection-specific
- Điểm coverage dashboard (AC-OPS-002) — likely no current gap, giáo vụ-specific
- Học bạ batch HSM signing (AC-OPS-003) — likely no current gap, infra-heavy
- Student card scan check-in/out + parent notify (AC-OPS-006) — extends GAP-060
- Health/y tế module + vaccination tracker (AC-OPS-007) — likely no current gap, P5-specific
- KBNN reconciliation public school (AC-FIN-002) — likely no current gap
- TT 107/2017 financial report template (AC-FIN-003) — extends GAP-185
- Emergency broadcast template + multi-channel push (AC-EDGE-001) — extends GAP-063
- Child safety incident workflow severity classification (AC-EDGE-003) — extends GAP-186
- Inter-school transfer portal (AC-EXIT-002) — likely no current gap, P5-specific

---

## Cross-References

- **Persona source:** [`../../personas-catalog.md`](../../personas-catalog.md) §Secondary Personas — Admin row
- **Sibling tenant AC:** [`../P5-k12-school.md`](../P5-k12-school.md) — tenant-level (Hiệu trưởng-as-tenant); this doc is admin-as-employee
- **Sibling secondary AC:** [`teacher-employee-in-P5.md`](teacher-employee-in-P5.md), [`student-in-P5.md`](student-in-P5.md), [`parent-in-P5.md`](parent-in-P5.md), [`admin-in-P3.md`](admin-in-P3.md) (parallel — Admin in P3)
- **Review skill:** [`../../../../.claude/skills/quality/persona-based-business-review.md`](../../../../.claude/skills/quality/persona-based-business-review.md)
- **AC framework gap:** [GAP-151](../../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (template)
- **Review execution gap:** [GAP-152](../../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md)
- **Secondary persona AC gap:** [GAP-153](../../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md) (this Phase 1)

### Linked feature gaps (cross-link for review traceability)
- [GAP-051](../../../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) — Bulk import (IT staff executes 800 HS + 1200 parents)
- [GAP-052](../../../04-quality/gaps/GAP-052-parent-portal.md) — Parent portal (complaint workflow, document access)
- [GAP-053](../../../04-quality/gaps/GAP-053-academic-year-semester-structure.md) — Academic year/semester (CRITICAL — văn phòng manages)
- [GAP-055](../../../04-quality/gaps/GAP-055-student-grading-report-system.md) — Báo cáo MoET official (CRITICAL — giáo vụ + văn phòng heavy use)
- [GAP-057](../../../04-quality/gaps/GAP-057-payroll-teacher-commission.md) — Payroll commission (50 teachers MoET payscale)
- [GAP-058](../../../04-quality/gaps/GAP-058-role-hierarchy-org-chart.md) — Role hierarchy + RBAC (CRITICAL — 6 admin roles)
- [GAP-060](../../../04-quality/gaps/GAP-060-period-based-attendance.md) — Period-based attendance (extends to bảo vệ check-in/out)
- [GAP-061](../../../04-quality/gaps/GAP-061-promotion-retention-logic.md) — Promotion/retention (giáo vụ admin role)
- [GAP-062](../../../04-quality/gaps/GAP-062-teacher-payroll-bank-integration.md) — Payroll bank MT940
- [GAP-063](../../../04-quality/gaps/GAP-063-sms-zalo-notification-integration.md) — SMS/Zalo notification (CRITICAL — 1200 parents)
- [GAP-180](../../../04-quality/gaps/GAP-180-terms-of-service.md) — TOS
- [GAP-184](../../../04-quality/gaps/GAP-184-data-retention.md) — Data retention (CRITICAL — TT 32/2020 5-year)
- [GAP-185](../../../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md) — Billing/VAT/TCT (TT 107/2017 financial report)
- [GAP-186](../../../04-quality/gaps/GAP-186-child-protection.md) — Child protection (CRITICAL — staff vetting + incident workflow)

---

## Log

- **2026-04-30** — Initial AC set v1 (author: Agent D, Wave Secondary-Persona-AC, GAP-153 Phase 1). 19 ACs across 6 categories covering 6-role admin hierarchy in K-12 School context (văn phòng + giáo vụ + thư viện + y tế + bảo vệ + IT). Highlights: MOET reporting workflow (TT 22/2021, TT 32/2020, TT 107/2017), staff vetting + LLTP renewal (Luật Trẻ em 2016 Đ.25 + Decree 56/2017), teacher payroll batch 50 GV với MoET payscale + BHXH/BHYT/TNCN, học bạ batch HSM signing 800 HS, child safety incident workflow severity classification, emergency school closure 1200-recipient broadcast, MoET audit export 5-year retention WORM-immutable. Legal compliance citations include: TT 22/2021, TT 32/2020, TT 107/2017, TT 32/2018 (chương trình GDPT), Bộ luật Lao động 2019, Luật Trẻ em 2016 Đ.25 + Đ.27 + Đ.51, Decree 56/2017, Luật Lý lịch tư pháp 2009, NĐ 81/2021/NĐ-CP (miễn giảm học phí), NĐ 123/2020/NĐ-CP (e-invoice). 14 cross-links to existing feature gaps + 11 candidate NEW gaps surfaced at review time.
- **TBD** — GAP-152 Round 1 review by domain expert (Real giáo vụ + văn phòng staff + Hiệu phó + MOET education expert) + Product Owner sign-off; status updates fill PASS/PARTIAL/FAIL.
