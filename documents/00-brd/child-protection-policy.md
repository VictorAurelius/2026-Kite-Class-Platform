# Child Protection Policy (K-12 Minors) — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — section structure + safeguarding rules + mandatory reporting matrix; Phase 2 legal counsel + MOLISA consultation + parental consent UI implementation via GAP-154)
**Owner:** Legal + Trust & Safety + DPO
**Reviewer:** Legal counsel (VN child protection law expertise) + DPO + MOLISA consultation (if platform registration required) + Education domain expert (Phase 2)
**Last-Updated:** 2026-04-29
**Tracking:** GAP-186 (Phase 1, Wave Legal-BRD Phase 1.5 2026-04-29) → GAP-154 (Phase 2 content + legal sign-off + parental consent UI + age verification + safeguarding reporting channel + teacher vetting)
**Legal basis:** **Luật Trẻ em 2016** (Law on Children, Law No. 102/2016/QH13) — toàn diện child protection mandate; **Decree 56/2017/NĐ-CP** (Law on Children implementation); **Decree 13/2023/NĐ-CP Article 16** (PDPL minor data — parental consent <16); **MOLISA circulars** on child online safety; Penal Code Art 142-147 (offenses against minors)
**International references:** COPPA (US, if expanding), UK Children's Code, Singapore PDPA child provisions
**Cross-cuts:** [GAP-180 TOS](terms-of-service.md) (minor user section), [GAP-181 AUP](acceptable-use-policy.md) (education-specific prohibitions §4 — predatory behavior, CSAM), [GAP-182 Privacy Policy](privacy-policy.md) (minor section §12), [GAP-184 Retention](data-retention-deletion-policy.md) (sensitive-minor stricter), GAP-052 parent portal (enables parent visibility — safeguarding dependency)
**Strategic priority:** P5 K-12 School persona blocker — without this policy, deploying to any school = legal violation

---

## Mục lục

1. [Phạm vi áp dụng (Scope)](#1-phạm-vi-áp-dụng-scope)
2. [Sự đồng ý của Phụ huynh (Parental Consent)](#2-sự-đồng-ý-của-phụ-huynh-parental-consent)
3. [Bảo vệ Dữ liệu Trẻ vị thành niên (Minor Data Protection)](#3-bảo-vệ-dữ-liệu-trẻ-vị-thành-niên-minor-data-protection)
4. [Quy tắc An toàn Trẻ em (Safeguarding Rules)](#4-quy-tắc-an-toàn-trẻ-em-safeguarding-rules)
5. [Tính năng An toàn Nền tảng (Platform Safety Features)](#5-tính-năng-an-toàn-nền-tảng-platform-safety-features)
6. [Bảo vệ qua Đội ngũ Giáo dục (Staff Safeguarding)](#6-bảo-vệ-qua-đội-ngũ-giáo-dục-staff-safeguarding)
7. [Phản ứng Sự cố (Incident Response — child safety specific)](#7-phản-ứng-sự-cố-incident-response--child-safety-specific)
8. [Đào tạo và Nâng cao Nhận thức (Training + Awareness)](#8-đào-tạo-và-nâng-cao-nhận-thức-training--awareness)

---

## 1. Phạm vi áp dụng (Scope)

Chính sách này áp dụng cho mọi **trẻ vị thành niên (minor)** sử dụng nền tảng KiteHub/KiteClass. Theo định nghĩa của **Bộ luật Dân sự 2015 Điều 21** và **Decree 13/2023/NĐ-CP Điều 16** (PDPL — Luật Bảo vệ Dữ liệu Cá nhân), trẻ vị thành niên trong phạm vi tài liệu này được hiểu là **người dưới 16 tuổi**. Một số nội dung an toàn (safeguarding) còn áp dụng rộng hơn đến người dưới 18 tuổi theo **Luật Trẻ em 2016 Điều 1** (định nghĩa trẻ em là người dưới 16 tuổi tại Việt Nam) — nền tảng có thể áp dụng tiêu chuẩn nghiêm hơn (under-18) khi xét đến rủi ro grooming, CSAM, hoặc khi triển khai cho thị trường có định nghĩa minor cao hơn.

Phạm vi áp dụng theo combination persona × tenant type (theo `personas-catalog.md`):

| Persona | Tenant type | Trigger | Áp dụng full policy? |
|---------|-------------|---------|----------------------|
| **Student** | P5 K-12 School | Luôn áp dụng (mặc định <16) | ✅ Đầy đủ |
| **Student** | P3 Tutoring Center | Áp dụng nếu Student <16 (xác minh DOB) | ✅ Đầy đủ |
| **Student** | P1 Adult Skills, P2 Language Center (≥16) | Không áp dụng (adult learner) | ❌ |
| **Parent** | P5 K-12 School / P3 Tutoring (con <16) | Áp dụng — parent là proxy consent + visibility | ✅ Phần consent + visibility |
| **Teacher** | P5/P3 (giảng dạy minors) | Áp dụng phần Safeguarding (§4, §6, §8) | ✅ Phần safeguarding |
| **Owner/Admin** | P5 K-12 | Áp dụng đầy đủ — chịu trách nhiệm tenant-level | ✅ Đầy đủ |

<!-- Phase 2: Persona × tenant matrix sẽ được legal counsel xác nhận; có thể mở rộng under-18 cho safeguarding-only sections — informed gut Q3 2026, GAP-154 -->

**Ưu tiên chiến lược:** P5 K-12 School là phân khúc target. Chính sách này là **điều kiện tiên quyết** để triển khai cho bất kỳ trường K-12 nào — thiếu nó = vi phạm pháp luật Việt Nam (Luật Trẻ em 2016 + PDPL Art 16) và tạo legal exposure nghiêm trọng cho cả nền tảng lẫn tenant.

---

## 2. Sự đồng ý của Phụ huynh (Parental Consent)

Theo **Decree 13/2023/NĐ-CP Điều 16 khoản 1**, mọi hoạt động xử lý dữ liệu cá nhân của trẻ em dưới 16 tuổi BẮT BUỘC phải có sự đồng ý của cha mẹ hoặc người giám hộ hợp pháp. Đồng thời, **Luật Trẻ em 2016 Điều 6** quy định quyền tham gia của trẻ em phải được tôn trọng — nền tảng phải vừa lấy parental consent, vừa cung cấp cho trẻ thông tin phù hợp với độ tuổi.

### 2.1 Khi nào yêu cầu

Parental consent BẮT BUỘC tại các điểm sau:
- **Tạo tài khoản** (account creation) — trước khi minor được kích hoạt access
- **Xử lý dữ liệu cá nhân** (theo PDPL Art 16: thu thập, lưu trữ, xử lý, chia sẻ)
- **Khởi tạo kênh giao tiếp** (giáo viên gửi tin nhắn đầu tiên, mời vào lớp 1-1)
- **Thay đổi mục đích xử lý** so với consent ban đầu
- **Sử dụng tính năng AI** xử lý dữ liệu của minor (chấm điểm tự động, recommendation, học liệu cá nhân hóa)
- **Chia sẻ dữ liệu với bên thứ ba** (kể cả third-party tools tích hợp trong tenant)

### 2.2 Định dạng (Format)

Consent phải **verifiable** — không chấp nhận tick-box đơn giản. Các định dạng được chấp nhận:

| Định dạng | Verifiability | Áp dụng |
|-----------|---------------|---------|
| Chữ ký điện tử (e-signature) qua VNeID | Cao — định danh quốc gia | Khuyến nghị (mặc định) |
| Chữ ký số (digital signature) cá nhân | Cao | Cho phép |
| Văn bản giấy có chữ ký + scan | Trung bình | Cho phép — tenant lưu bản gốc |
| Email xác nhận từ địa chỉ đã verify + OTP SMS | Trung bình | Chấp nhận khi VNeID không khả dụng |
| Tick-box web đơn thuần | ❌ Thấp | KHÔNG chấp nhận cho consent gốc |

<!-- Phase 2: parental consent UI — flow chi tiết VNeID integration + fallback cho phụ huynh không có VNeID — informed gut Q3 2026, GAP-154 -->

### 2.3 Phạm vi (Scope of authorization)

Consent form PHẢI liệt kê rõ ràng (granular consent — không bundle):
- Loại dữ liệu được xử lý (PII cơ bản, học bạ, hình ảnh lớp học, audio/video buổi học, kết quả AI)
- Mục đích xử lý (giảng dạy, đánh giá, báo cáo phụ huynh, marketing → phụ huynh phải opt-in riêng)
- Thời hạn lưu trữ (link tới [Data Retention Policy](data-retention-deletion-policy.md) — mục sensitive-minor 6 tháng max post-termination)
- Bên thứ ba có thể truy cập (nếu có)
- Quyền của phụ huynh (rút consent, yêu cầu xóa, yêu cầu copy)
- Tính năng AI (giáo viên ảo, content moderation tự động) — phụ huynh phải opt-in riêng

### 2.4 Cơ chế rút consent (Withdrawal mechanism)

Theo **PDPL Art 14 + Luật Trẻ em 2016 Điều 33**:
- Phụ huynh có quyền **rút consent bất cứ lúc nào**, không cần lý do
- Rút consent phải được hỗ trợ qua **Parent Portal** (planned — see GAP-052), email tới DPO, hoặc văn bản
- Sau khi rút consent: nền tảng phải **dừng xử lý trong 72 giờ** và chuyển dữ liệu vào trạng thái retention-only theo policy
- Việc rút consent KHÔNG ảnh hưởng tính hợp pháp của xử lý đã thực hiện trước đó
- Tenant + nền tảng phải gửi xác nhận rút consent + báo cáo trạng thái dữ liệu trong vòng 7 ngày làm việc

### 2.5 Quy trình xác minh độ tuổi (Age verification)

Khi tạo tài khoản minor:
1. Phụ huynh khai báo ngày sinh (DOB) của con
2. Hệ thống tính tuổi tại thời điểm đăng ký
3. Nếu <16 tuổi → bắt buộc parental consent flow đầy đủ (§2.2)
4. Nếu ≥16 tuổi nhưng <18 → áp dụng safeguarding rules (§4, §5) nhưng consent có thể là chính minor đó tự ký theo PDPL Art 16 khoản 2
5. Cross-check với CCCD/CMND/giấy khai sinh khi tenant onboard (P5 K-12 — bắt buộc)

```
ASCII Flow — Age Verification + Consent

  [Parent registers child]
            ↓
  [Enter child DOB]
            ↓
  [System calculates age]
            ↓
       ┌────┴────┐
       <16       ≥16 (and <18)
       │             │
       ↓             ↓
  [Full parental    [Safeguarding-only —
   consent flow]    minor self-consent OK
                    per PDPL Art 16(2)]
       │             │
       ↓             ↓
  [VNeID e-sign     [Standard signup +
   OR fallback      DOB recorded]
   per §2.2]
       │
       ↓
  [Granular scope   ← Parent picks data categories,
   selection §2.3]    purposes, AI opt-in
       │
       ↓
  [Confirmation +
   audit log entry]
       │
       ↓
  [Account activated;
   consent versioned + retrievable]
```

<!-- Phase 2: age verification flow + VNeID integration + fallback path khi VNeID không có; cross-check với giấy khai sinh khi tenant onboard — informed gut Q3 2026, GAP-154 -->

---

## 3. Bảo vệ Dữ liệu Trẻ vị thành niên (Minor Data Protection)

Dữ liệu của minor được xếp vào nhóm **dữ liệu cá nhân nhạy cảm** theo **PDPL Điều 2 khoản 4** và phải được bảo vệ theo tiêu chuẩn nghiêm ngặt hơn dữ liệu adult.

### 3.1 Lưu trữ nghiêm ngặt hơn (Stricter retention)

Theo [Data Retention + Deletion Policy](data-retention-deletion-policy.md) mục `sensitive-minor`:
- **Tối đa 6 tháng sau khi tài khoản kết thúc** (vs 36 tháng cho adult)
- Backup chứa minor data phải được encrypt với key tách biệt
- Post-termination grace period rút ngắn từ 30 ngày (adult) xuống **7 ngày** (minor)

### 3.2 Cấm marketing đối với trẻ em

**KHÔNG marketing tới minor dưới mọi hình thức** — theo **Luật Quảng cáo 2012 Điều 7** (cấm quảng cáo với trẻ em không phù hợp) + **Luật Trẻ em 2016 Điều 50** (bảo vệ trẻ em khỏi quảng cáo có hại):
- Không gửi email/push notification có nội dung marketing tới tài khoản minor
- Không hiển thị banner ads trong UI minor
- Không sử dụng minor data cho lookalike audience hoặc retargeting
- Không gửi email khuyến mãi, upsell tier, cross-sell sang sản phẩm khác

### 3.3 Cấm profiling và behavioral advertising

Theo **PDPL Art 16 khoản 3** + **Luật Trẻ em 2016 Điều 33**:
- KHÔNG xây dựng behavioral profile của minor cho mục đích thương mại
- KHÔNG bán hoặc chia sẻ minor data cho mạng quảng cáo
- KHÔNG dùng AI để dự đoán hành vi tiêu dùng / preference thương mại của minor
- AI phục vụ học tập (recommendation bài học, adaptive learning) ĐƯỢC PHÉP với consent rõ ràng — nhưng phải minimize data, không lưu profile vĩnh viễn

### 3.4 Không chia sẻ với bên thứ ba ngoài mục đích giáo dục

Minor data CHỈ được chia sẻ với:
- Tenant (trường/trung tâm) sở hữu lớp học
- Giáo viên trực tiếp giảng dạy minor đó
- Phụ huynh đã consent
- Cơ quan nhà nước có thẩm quyền (theo lệnh hợp pháp — §4.4 mandatory reporting)
- Sub-processor đã ký DPA (data processing agreement) với cam kết minor protection tương đương

KHÔNG chia sẻ với: ad networks, analytics providers chưa pass minor-data assessment, third-party LLM providers chưa có data residency cam kết tại Việt Nam.

### 3.5 Mã hóa và Access Control nâng cao

- Minor PII được encrypt at rest với AES-256 + key rotation hàng quý
- Minor PII trong transit luôn TLS 1.3
- **Audit log mọi truy cập** — mỗi lần admin/teacher/system đọc minor data đều ghi log (who, what, when, why)
- Role-based access control: chỉ teacher của lớp + phụ huynh + admin tenant + DPO platform mới có quyền đọc
- Không lưu minor data trong logs application (PII scrubber bắt buộc — link `logs-format-standard.md`)

### 3.6 Ma trận xử lý dữ liệu (Minor Data Handling Matrix)

| Loại dữ liệu | Quy tắc chuẩn (adult) | Quy tắc minor (stricter) | Legal basis |
|--------------|------------------------|--------------------------|-------------|
| **PII cơ bản** (tên, DOB, ảnh đại diện) | Lưu 36mo, encrypt at rest | Lưu max 6mo post-termination, encrypt + key tách biệt | PDPL Art 16, Luật Trẻ em Đ.33 |
| **Học liệu / bài làm** | Lưu suốt thời gian học + 12mo | Lưu suốt thời gian học + 6mo, không dùng training AI | PDPL Art 16(3) |
| **Audio/video buổi học** | Lưu 90 ngày, opt-in | Lưu max 30 ngày, **bắt buộc parent visibility** + opt-in granular | Luật Trẻ em Đ.33, PDPL Art 16 |
| **AI-generated assessment data** | Lưu kèm bài, có thể dùng training | Lưu kèm bài, **KHÔNG dùng training**, KHÔNG profile | PDPL Art 16(3) |
| **Behavioral analytics** (heatmap, time-on-task) | Aggregated OK cho product analytics | **Cấm cá nhân hóa**; chỉ aggregated tenant-level | Luật QC 2012 Đ.7, Luật Trẻ em Đ.50 |
| **Marketing list** | Opt-in adult | **Tuyệt đối cấm** — không add minor vào list | Luật QC 2012 Đ.7, Luật Trẻ em Đ.50 |
| **Communication metadata** (DM, calls) | Lưu 12mo | **Lưu vĩnh viễn cho safeguarding evidence** (link §7.3) | Luật Trẻ em Đ.25 (bảo vệ trẻ em trên không gian mạng) |
| **Health / disability data** (nếu có) | Sensitive, encrypt + DPA | Sensitive **VÀ** minor — double protection, restricted access | PDPL Art 2(4), Luật Trẻ em Đ.35 |
| **Location data** | Opt-in | **Cấm trừ khi safeguarding emergency** | Luật Trẻ em Đ.6 + PDPL Art 16 |

<!-- Phase 2: data classification matrix sẽ được legal counsel rà soát; có thể bổ sung biometric data row khi có tính năng face-recognition attendance — informed gut Q3 2026, GAP-154 -->

---

## 4. Quy tắc An toàn Trẻ em (Safeguarding Rules)

Theo **Luật Trẻ em 2016 Chương IV (Bảo vệ trẻ em)** Điều 25-27 (bảo vệ trẻ em trên môi trường mạng) + **Decree 56/2017/NĐ-CP Chương III** (trách nhiệm các bên trong môi trường mạng).

### 4.1 Giao tiếp giáo viên-học sinh phải qua nền tảng

**Bắt buộc 100% — không exception**:
- Mọi giao tiếp giữa teacher và student dưới 18 tuổi PHẢI diễn ra trên nền tảng (in-platform messaging, video call, classroom)
- **CẤM** giáo viên DM học sinh qua kênh ngoài (Zalo cá nhân, Facebook, WhatsApp, SMS cá nhân, gọi điện thoại cá nhân)
- Tenant phải ký "Code of Conduct" với teacher xác nhận quy tắc này (§6.3)
- Vi phạm = báo cáo Trust & Safety + có thể terminate teacher account

### 4.2 Lớp 1-1 (one-to-one calls)

Khi giáo viên có buổi 1-1 với minor:
- **Tùy chọn ghi âm/ghi hình** phải khả dụng — phụ huynh chọn opt-in tại consent
- **Parent visibility**: phụ huynh có quyền **join hoặc observe** buổi 1-1 không cần báo trước (parent observer mode)
- Mặc định buổi 1-1 với minor được lên lịch trong **giờ làm việc** (8:00-21:00) — buổi ngoài giờ phải parent + admin tenant approve
- Buổi 1-1 phải có **lý do giáo dục rõ ràng** ghi trong system (tutoring topic, makeup class, …)
- Recording (nếu opt-in) lưu max 30 ngày, encrypt, chỉ admin tenant + phụ huynh + giáo viên đọc

<!-- Phase 2: parent observer mode UX flow + opt-in granular cho recording — informed gut Q3 2026, GAP-154 -->

### 4.3 Quy trình báo cáo hành vi đáng ngờ (Suspicious behavior reporting)

Bất kỳ ai (student, parent, teacher, admin, observer) đều có thể báo cáo:
- Báo cáo nặc danh được hỗ trợ (anonymous report) — qua Trust & Safety hotline
- Trust & Safety triage trong vòng **24 giờ** với mọi báo cáo liên quan minor
- Báo cáo P0 (nghi ngờ grooming, abuse, CSAM) → escalate IMMEDIATELY tới authorities (§7.2)
- Không chấp nhận retaliation — người báo cáo có thiện chí được bảo vệ
- Nền tảng cung cấp checklist nhận diện hành vi đáng ngờ (grooming patterns, isolation tactics, gift-giving, asking for personal info ngoài context giáo dục)

### 4.4 Báo cáo bắt buộc tới cơ quan chức năng (Mandatory reporting)

Theo **Luật Trẻ em 2016 Điều 51 (trách nhiệm phát hiện, tố giác trẻ em bị xâm hại)** + **Penal Code Art 142-147** (các tội xâm hại tình dục trẻ em, mua bán trẻ em, hành hạ trẻ em):

| Loại sự cố | Cơ quan báo cáo | Thời hạn | Căn cứ pháp lý |
|------------|------------------|----------|-----------------|
| **Grooming** (gạ gẫm tình dục online) | Cảnh sát 113 + MOLISA Đường dây 111 | IMMEDIATE (≤24h) | Luật Trẻ em Đ.51 + PC Art 146 |
| **CSAM** (Child Sexual Abuse Material — phát hiện trên platform) | Cảnh sát PC02/A05 + MOLISA + Tổng đài 111 | IMMEDIATE — báo cáo + bảo toàn evidence | PC Art 147 (sản xuất, mua bán văn hóa phẩm khiêu dâm trẻ em) |
| **Bạo hành / lạm dụng thể chất** (qua tin nhắn, video, audio) | MOLISA Đường dây 111 + Cảnh sát địa phương | ≤24h | Luật Trẻ em Đ.51 + PC Art 140 |
| **Tự tử / tự hại** (suicide ideation từ minor) | Tổng đài 111 + thông báo phụ huynh + cảnh sát nếu nguy cấp | IMMEDIATE | Luật Trẻ em Đ.27 |
| **Kẻ trưởng thành xâm nhập tài khoản minor** (impersonation, hijack) | Cảnh sát A05 (cybersecurity) + MOLISA | ≤24h | Luật ANM 2018 + Luật Trẻ em Đ.51 |
| **Mua bán / dụ dỗ trẻ em** | Cảnh sát + MOLISA + Tổng đài 111 | IMMEDIATE | PC Art 151 (mua bán người dưới 16 tuổi) |
| **Bắt nạt qua mạng** (cyberbullying nghiêm trọng) | MOLISA + thông báo tenant + parent | ≤72h | Luật Trẻ em Đ.27, Đ.54 |
| **Vi phạm dữ liệu liên quan minor** (data breach) | Bộ Công an A05 + MOLISA + parent | ≤72h theo PDPL | PDPL Art 23 + Luật Trẻ em Đ.51 |

**Mandatory Reporting Matrix (full list):**

| Incident type | Authority | VN law citation | Severity |
|---------------|-----------|-----------------|:--------:|
| Grooming | Cảnh sát 113, Tổng đài 111 (MOLISA) | Luật Trẻ em 2016 Đ.51, Penal Code Art 146 | P0 |
| CSAM detection | Cảnh sát A05/PC02, Tổng đài 111 | Penal Code Art 147, Luật ANM 2018 | P0 |
| Sexual abuse indicators | Cảnh sát + MOLISA | Penal Code Art 142-145, Luật Trẻ em Đ.51 | P0 |
| Physical abuse indicators | Cảnh sát địa phương + MOLISA | Penal Code Art 140, Luật Trẻ em Đ.51 | P0 |
| Suicide/self-harm threat | Tổng đài 111 + parent + cảnh sát (nếu cấp) | Luật Trẻ em Đ.27 | P0 |
| Account hijack/impersonation (adult posing as minor) | Cảnh sát A05 | Luật ANM 2018 + Luật Trẻ em Đ.51 | P1 |
| Trafficking signals | Cảnh sát + MOLISA | Penal Code Art 151 | P0 |
| Severe cyberbullying | MOLISA + tenant + parent | Luật Trẻ em Đ.27, Đ.54 | P1 |
| Minor data breach | Bộ Công an A05 + MOLISA + parent | PDPL Art 23, Luật Trẻ em Đ.51 | P1 |
| Repeated platform safety violation by adult user | Tenant Trust & Safety + escalate MOLISA nếu pattern | Luật Trẻ em Đ.51 | P2 |

Bộ phận Trust & Safety của nền tảng đóng vai trò **mandatory reporter** — không chờ tenant quyết định, không pre-screen với legal trước khi escalate P0.

<!-- Phase 2: safeguarding hotline integration với Tổng đài 111 + workflow ticket → cảnh sát; dedicated safeguarding incident dashboard cho admin tenant — informed gut Q3 2026, GAP-154 -->

---

## 5. Tính năng An toàn Nền tảng (Platform Safety Features)

Theo **Decree 56/2017/NĐ-CP Điều 33-37** (an toàn trẻ em trên không gian mạng — trách nhiệm doanh nghiệp).

### 5.1 Lọc nội dung nghiêm ngặt hơn cho tài khoản minor

Tài khoản minor phải có content filtering tighter:
- AI moderation thresholds nghiêm hơn 30% so với tài khoản adult (text + image + video)
- Blocklist mở rộng: gambling, alcohol, dating, mature content, self-harm references, hate speech
- Real-time scan của tin nhắn từ outside class context (private DM nếu có) → flagged ngay khi detect signal

### 5.2 Hạn chế thời gian (Time-of-day restrictions)

- Phụ huynh có thể đặt **giờ cấm sử dụng** (parent-set ban window)
- **Mặc định khuyến nghị**: 22:00 – 06:00 không truy cập (theo MOLISA recommendation về digital wellbeing trẻ em)
- Tenant có thể đặt giờ học cố định để chặn truy cập ngoài giờ
- Trong giờ cấm: minor chỉ truy cập nội dung emergency (safeguarding hotline, contact parent)

<!-- Phase 2: time-of-day restriction defaults — cần stakeholder interview với MOLISA + 5 trường K-12 để chốt giờ mặc định — informed gut Q3 2026, GAP-154 -->

### 5.3 Báo cáo thời gian sử dụng cho phụ huynh

- Weekly digest gửi cho phụ huynh: tổng thời gian sử dụng, phân bổ theo lớp/môn, alert nếu vượt threshold
- Daily breakdown khả dụng trong Parent Portal (planned — see GAP-052)
- Tích hợp khuyến nghị nghỉ giữa giờ (mỗi 45 phút) hiển thị cho minor

<!-- Phase 2: weekly digest email template + Parent Portal screen mockup — informed gut Q3 2026, GAP-154 -->

### 5.4 Liên hệ khẩn cấp (Emergency contact quick access)

Mọi trang trong UI minor có nút "Cần giúp đỡ" (visible, accessible):
- Tổng đài 111 (MOLISA — bảo vệ trẻ em) — direct call/chat
- Liên hệ phụ huynh đã đăng ký
- Liên hệ giáo viên chủ nhiệm
- Trust & Safety 24/7 hotline của nền tảng (§7.1)

### 5.5 Thiết kế mặc định an toàn (Privacy by Default)

- Profile minor mặc định private (không discoverable bởi user khác trên platform)
- Tin nhắn từ user ngoài lớp/trường mặc định block (whitelist-only)
- Vị trí (location) mặc định không thu thập
- Camera/microphone mặc định off, chỉ on khi join lớp học có lịch

---

## 6. Bảo vệ qua Đội ngũ Giáo dục (Staff Safeguarding)

**Lưu ý phạm vi:** Phần này quy định **trách nhiệm của tenant** (trường/trung tâm) — nền tảng không trực tiếp tuyển dụng giáo viên. Nền tảng documents requirements + cung cấp công cụ để tenant tuân thủ.

### 6.1 Yêu cầu sàng lọc giáo viên (Teacher vetting requirement)

Tenant P5 K-12 BẮT BUỘC khi onboard giáo viên:
- Xác minh **danh tính** qua CCCD + bằng cấp giáo dục
- Yêu cầu **Lý lịch tư pháp số 2** (LLTP — criminal background check) — không có tiền án về tội xâm hại trẻ em (Penal Code Art 142-147), không có lệnh cấm tiếp xúc trẻ em
- Verify thông tin với cơ quan giáo dục địa phương (nếu áp dụng cho giáo viên trường công)

Nền tảng cung cấp **Teacher Vetting Checklist** trong tenant onboarding flow — tenant tự chịu trách nhiệm thực hiện, nền tảng audit randomly.

<!-- Phase 2: teacher vetting tenant onboarding workflow — UI + checklist + admin dashboard cho compliance status — informed gut Q3 2026, GAP-154 -->

### 6.2 Lưu trữ tài liệu nền tảng (Background check documentation)

- Tenant phải lưu bản scan LLTP + CCCD của mỗi giáo viên dạy minor
- Update LLTP định kỳ **mỗi 2 năm**
- Documentation phải sẵn sàng khi audit (cảnh sát, MOLISA, hoặc nền tảng yêu cầu)
- Encrypted storage tại tenant; nền tảng không lưu bản gốc (data minimization) — chỉ lưu **status flag** (verified/expired/pending) cho dashboard compliance

### 6.3 Code of Conduct ký tại signup

Mỗi giáo viên khi tạo account trên nền tảng phải đọc + tick consent với **Teacher Code of Conduct**:
- Không liên lạc với học sinh ngoài kênh nền tảng (§4.1)
- Không gặp riêng học sinh ngoài context giảng dạy
- Không nhận quà, tiền từ học sinh/phụ huynh ngoài học phí chính thức
- Không lưu dữ liệu học sinh (ảnh, audio, bài làm) trên thiết bị cá nhân
- Báo cáo bất kỳ hành vi đáng ngờ nào tới Trust & Safety
- Tham gia training annual safeguarding (§8.1)

Vi phạm Code of Conduct = ngay lập tức suspended, điều tra, và có thể terminate + báo cáo cơ quan chức năng nếu phát hiện vi phạm Penal Code.

<!-- Phase 2: Teacher Code of Conduct full text + e-signature flow + version history; tích hợp với annual training requirement (§8.1) — informed gut Q3 2026, GAP-154 -->

---

## 7. Phản ứng Sự cố (Incident Response — child safety specific)

Theo **Decree 56/2017/NĐ-CP Điều 36** (xử lý sự cố liên quan trẻ em).

### 7.1 Đường dây 24/7 và priority support

- **Trust & Safety hotline 24/7** dành riêng cho safeguarding — số điện thoại + email + in-platform escalate button
- Tickets gắn nhãn "child-safety" được route ngay tới senior T&S analyst, KHÔNG qua tier 1 thông thường
- **SLA**: P0 (grooming, CSAM, suicide) — phản hồi ≤15 phút; P1 — ≤2 giờ; P2 — ≤24 giờ
- Direct line tới legal counsel của nền tảng cho consultation realtime nếu cần

<!-- Phase 2: hotline phone number + escalation roster + on-call rotation — informed gut Q3 2026, GAP-154 -->

### 7.2 Escalation tới cơ quan chức năng

Theo §4.4 mandatory reporting matrix:
- **Cảnh sát 113** — tình huống nguy cấp tức thời (đang xảy ra)
- **Cảnh sát A05** (cybersecurity) — sự cố data breach, account hijack, online crime
- **Cảnh sát PC02** — tội phạm liên quan trẻ em (xâm hại, mua bán)
- **MOLISA Tổng đài 111** — đường dây quốc gia bảo vệ trẻ em (24/7, miễn phí, đa ngôn ngữ)
- **Bộ Lao động — Thương binh và Xã hội (MOLISA)** — Cục Trẻ em — báo cáo case-level
- **Sở Giáo dục địa phương** — khi sự cố xảy ra trong context trường học chính quy

Trust & Safety lead phải có direct contact của các đầu mối trên + ghi log mọi escalation.

### 7.3 Bảo toàn evidence (Evidence preservation)

Khi nghi ngờ sự cố P0:
- **KHÔNG xóa** dữ liệu liên quan (tin nhắn, recording, log truy cập, metadata) kể cả phụ huynh yêu cầu xóa
- Áp dụng **legal hold** — bypass retention policy để giữ evidence cho điều tra
- Snapshot timestamped, hash để chứng minh integrity
- Chỉ release evidence cho cảnh sát có lệnh hợp pháp + MOLISA + DPO platform — không bên thứ ba khác
- Mọi lần access evidence trong legal hold đều log audit trail

### 7.4 Thông báo cho phụ huynh

Theo severity:

**Severity Classification (Safeguarding Incident Severity × Response × Authority Notification):**

| Severity | Loại sự cố | Response window | Phụ huynh notify | Authority notify |
|:--------:|------------|-----------------|------------------|------------------|
| **P0** | Grooming, CSAM, suicide threat, abuse imminent | ≤15 phút | IMMEDIATE — call + in-app + email | IMMEDIATE — police + MOLISA |
| **P1** | Account hijack, severe bullying, data breach minor PII, ngoài-platform contact phát hiện | ≤2 giờ | ≤4 giờ — call + email | ≤24 giờ — MOLISA + cảnh sát nếu phù hợp |
| **P2** | Suspicious behavior pattern (chưa confirmed), minor policy violation, recurring guideline issues | ≤24 giờ | ≤48 giờ — email + Parent Portal | Không ngay; review pattern → escalate nếu repeat |
| **P3** | Minor UX safety issue (e.g. emergency button không hoạt động đúng), nhưng không có incident thực | ≤7 ngày | Không bắt buộc | Không |

<!-- Phase 2: notification timing per severity — chốt sau khi consult legal counsel + 1 case study với MOLISA — informed gut Q3 2026, GAP-154 -->

### 7.5 Hậu sự cố (Post-incident review)

- Mọi incident P0/P1 phải có **post-incident review** trong 7 ngày
- Output: timeline, root cause, preventive actions, policy update nếu cần
- Annual safeguarding report (anonymized aggregate) chia sẻ với MOLISA + tenant ecosystem để cải thiện chung
- Nếu phát hiện gap trong policy/feature → file gap mới + đưa vào wave roadmap

---

## 8. Đào tạo và Nâng cao Nhận thức (Training + Awareness)

Theo **Luật Trẻ em 2016 Điều 76** (giáo dục, truyền thông về bảo vệ trẻ em).

### 8.1 Đào tạo annual safeguarding cho giáo viên

- Mọi giáo viên dạy minor PHẢI hoàn thành **safeguarding training annual** (≥4 giờ)
- Nội dung: nhận diện grooming, mandatory reporting, code of conduct, cybersafety, mental health awareness, GDPR/PDPL minor data
- Tenant chịu trách nhiệm tracking — nền tảng cung cấp dashboard compliance
- Không hoàn thành training trong 30 ngày sau deadline = **suspended teaching access** đến khi hoàn thành

<!-- Phase 2: annual safeguarding training curriculum — outline 4 modules × 1h, có video + quiz + certification; có thể outsource cho UNICEF VN / Save the Children VN curriculum — informed gut Q3 2026, GAP-154 -->

### 8.2 Tài liệu cho học sinh (age-appropriate)

Học liệu an toàn mạng cho minor, phân theo độ tuổi:

| Cấp học | Tuổi | Nội dung chính |
|---------|------|----------------|
| **Tiểu học** (Primary, lớp 1-5) | 6-11 | Quy tắc cơ bản (không chia sẻ password, hỏi người lớn nếu thấy lạ, "stranger danger" online), video animation, quiz đơn giản |
| **THCS** (Middle school, lớp 6-9) | 11-15 | Cyberbullying, grooming awareness, privacy settings, digital footprint, cách báo cáo hành vi xấu, mental wellbeing online |
| **THPT** (High school, lớp 10-12, ≥16) | 15-18 | Sextortion, deepfake, peer pressure, AI safety, data rights, consent online, healthy relationships, addiction risks |

Nội dung dạng micro-learning (5-10 phút), tích hợp vào onboarding của minor và refresher hàng quý.

### 8.3 Tài nguyên cho phụ huynh

- Hướng dẫn **parental consent flow** (video walkthrough + FAQ)
- Hướng dẫn sử dụng **Parent Portal** (planned — see GAP-052) để quan sát + kiểm soát
- Cảnh báo dấu hiệu trẻ gặp vấn đề online (behavioral changes, sudden secrecy, etc.)
- Tổng hợp số điện thoại khẩn cấp (Tổng đài 111, cảnh sát 113, hotline platform)
- Newsletter định kỳ (opt-in) về xu hướng safety + cập nhật policy

<!-- Phase 2: parent awareness resources — 3 video × 5 phút + FAQ document + Parent Portal user guide — informed gut Q3 2026, GAP-154 -->

---

## 9. Quan hệ với các tài liệu khác (Cross-references)

Tài liệu này nằm trong cluster Legal & Trust framework — phải đọc cùng:

- [Terms of Service](terms-of-service.md) — minor user section định nghĩa user agreement với người dùng <16
- [Acceptable Use Policy](acceptable-use-policy.md) §4 — education-specific prohibitions: predatory behavior, CSAM, grooming bị cấm trên nền tảng
- [Privacy Policy](privacy-policy.md) §12 — minor data section align với chính sách này (PDPL Art 16)
- [Data Retention + Deletion Policy](data-retention-deletion-policy.md) — sensitive-minor category 6 tháng max post-termination
- `personas-catalog.md` — định nghĩa P5 K-12 + P3 Tutoring personas
- GAP-052 (Parent Portal — planned) — UI implementation enable parental visibility + control
- GAP-154 (Phase 2 umbrella) — content + legal sign-off + UI implementation
- GAP-186 (this gap, Phase 1)

---

## 10. Lịch sử cập nhật (Update Log)

| Ngày | Phiên bản | Người thực hiện | Mô tả |
|------|-----------|------------------|-------|
| 2026-04-29 | 0.1 (skeleton Phase 1) | GAP-186 Wave Legal-BRD Phase 1.5 Agent C | Tạo cấu trúc 8 sections + safeguarding rules + mandatory reporting matrix theo Luật Trẻ em 2016 + PDPL Art 16. Phase 2 (legal counsel + MOLISA consultation + parental consent UI + age verification + safeguarding hotline + teacher vetting onboarding) tracked GAP-154. |

<!-- Phase 2: legal counsel sign-off + MOLISA consultation outcome + version 1.0 release — informed gut Q3 2026, GAP-154 -->
