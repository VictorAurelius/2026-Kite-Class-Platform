# Privacy Policy — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — VN PDPL-mandated section structure; Phase 2 legal counsel + MPS consultation via GAP-154)
**Owner:** Legal + Data Protection Officer — see [`dpo-designation.md`](dpo-designation.md) (acting `@nguyenvankiet`, solo-dev, 2026-05-06; formal counsel review queued GAP-156)
**Reviewer:** Legal counsel với VN PDPL expertise + DPO + MPS A05 (if sensitive data registration required)
**Last-Updated:** 2026-04-29
**Tracking:** GAP-182 (Phase 1, Wave Legal-BRD 2026-04-29) → GAP-154 (Phase 2 content + legal sign-off + EN translation)
**Legal basis:** **Decree 13/2023/NĐ-CP** (Personal Data Protection Law, effective 2026-07-01) — Article 11 (notice mandate), Article 14 (rights response SLA), Article 23 (breach 72h); Cybersecurity Law 2018; Law on Electronic Transactions 2023; **GDPR Articles 13-14 + 15-22** (extraterritorial application)
**Languages:** Vietnamese (canonical, this file). English translation Phase 2.
**Cross-cuts:** GAP-180 (TOS — references Privacy), GAP-184 (retention aligned), GAP-186 (minor data), GAP-190 (breach notification ops)

---

## Tóm tắt (Phase 1 skeleton)

Tài liệu này thiết lập khung cấu trúc Privacy Policy theo mandate của **Nghị định 13/2023/NĐ-CP** về Bảo vệ Dữ liệu Cá nhân (PDPL — hiệu lực 2026-07-01) Điều 11 — yêu cầu thông báo cho chủ thể dữ liệu trước khi xử lý. Phase 1 ship 16 sections cấu trúc + bảng quyền chủ thể + ma trận data category. Phase 2 (GAP-154) — legal counsel review, DPO designation, EN translation, A05 consultation, cookie banner UI.

Tất cả tham chiếu Article (Điều) dưới đây áp dụng cho **Nghị định 13/2023/NĐ-CP** trừ khi ghi rõ luật khác.

---

## 1. Data Controller Identity (Đơn vị Kiểm soát Dữ liệu)

**Mục đích:** Định danh pháp lý của bên thu thập + xử lý dữ liệu cá nhân (PDPL Art 11.1.a — quyền được biết của chủ thể).

- **Tên công ty:** TODO (Phase 2 — legal entity name khi đăng ký kinh doanh hoàn tất)
- **Số đăng ký doanh nghiệp:** TODO (Phase 2 — Mã số doanh nghiệp / GP-ĐKKD)
- **Địa chỉ trụ sở:** TODO (Phase 2)
- **Email liên hệ chính thức:** TODO (Phase 2 — `legal@TODO`)
- **Người đại diện theo pháp luật:** TODO (Phase 2)
- **Vai trò pháp lý:** Bên Kiểm soát Dữ liệu (Data Controller) đối với dữ liệu của tenant admins, end-users (giáo viên, học sinh, phụ huynh, kế toán, staff). Đối với dữ liệu mà tenant đưa lên KiteClass thuộc phạm vi quản lý của tenant đó, KiteClass đóng vai trò Bên Xử lý Dữ liệu (Data Processor) — chi tiết DPA tracked trong GAP-180 TOS.

Chính sách này áp dụng cho mọi sản phẩm thuộc nền tảng — KiteHub (SaaS quản lý instance) và KiteClass (multi-tenant education delivery).

## 2. Data Protection Officer (DPO — Cán bộ Bảo vệ Dữ liệu)

**Mục đích:** Đầu mối liên hệ cho mọi yêu cầu thực thi quyền chủ thể dữ liệu + báo cáo sự cố (PDPL Art 28 — DPO mandatory cho tổ chức xử lý dữ liệu nhạy cảm hoặc dữ liệu trẻ em).

- **DPO designation:** Acting `@nguyenvankiet` (solo-dev, 2026-05-06) — see [`dpo-designation.md`](dpo-designation.md). Formal contracted DPO upon team growth OR crossing 50k subscribers per Phase 2 (GAP-156).
- **Email DPO:** TODO (Phase 2 — `dpo@TODO`)
- **Hotline / form yêu cầu chủ thể:** TODO (Phase 2 — endpoint web form + email kênh dự phòng)
- **Trách nhiệm DPO:** giám sát compliance PDPL, training nhân sự, làm cầu nối với Cục An ninh mạng và Phòng chống tội phạm sử dụng công nghệ cao (A05 — Bộ Công an), xử lý yêu cầu chủ thể dữ liệu trong SLA 20-30 ngày (Art 14).

Trước khi có DPO chính thức (Phase 2), mọi yêu cầu sẽ được chuyển tới `legal@TODO` và xử lý trong cùng khung SLA.

## 3. Data Subject Categories (Nhóm Chủ thể Dữ liệu)

**Mục đích:** Liệt kê các nhóm chủ thể có dữ liệu được xử lý (PDPL Art 11.1.b).

KiteHub + KiteClass xử lý dữ liệu của các nhóm chủ thể sau (cross-link `personas-catalog.md`):

- **Tenant Admin / Owner** — chủ trung tâm/trường, người sở hữu instance KiteClass.
- **Teacher (Giáo viên)** — bao gồm giáo viên fulltime, gia sư bán thời gian, trợ giảng.
- **Student (Học sinh / Người học)** — bao gồm cả người trưởng thành (adult learner) và **trẻ em dưới 16 tuổi** (K-12 — xem Section 12 + GAP-186).
- **Parent / Guardian (Phụ huynh / Người giám hộ)** — đối với học sinh dưới 16 tuổi.
- **Accountant / Cashier (Kế toán / Thu ngân)** — quản lý hoá đơn, học phí.
- **Other Staff (Nhân viên hỗ trợ)** — admin office, marketing, lễ tân.
- **Visitor / Lead** — người chưa đăng ký nhưng có để lại thông tin trên landing pages.

## 4. Data Categories Processed (Loại Dữ liệu Xử lý)

**Mục đích:** Liệt kê đầy đủ category + nhận diện dữ liệu nhạy cảm (PDPL Art 11.1.c, Art 3.4 định nghĩa "dữ liệu cá nhân nhạy cảm").

### 4.1 Identification (Định danh)
Họ tên, ngày sinh, giới tính, số CCCD/CMND/hộ chiếu (chỉ thu thập cho Tenant Owner phục vụ xuất hoá đơn — PDPL Art 11.1.c liên quan tài chính).

### 4.2 Contact (Liên hệ)
Email, số điện thoại, địa chỉ thường trú/tạm trú, địa chỉ cha mẹ (đối với học sinh dưới 16 tuổi).

### 4.3 Educational (Giáo dục)
Điểm số, điểm danh, bài tập, đánh giá hạnh kiểm, tiến độ học tập, lịch học. Với học sinh K-12, đây là dữ liệu của **trẻ em** — áp dụng các bảo vệ bổ sung tại Section 12.

### 4.4 Financial (Tài chính)
Thông tin thanh toán (token cổng thanh toán — KiteHub/KiteClass KHÔNG lưu số thẻ thật), hoá đơn, lịch sử giao dịch, nợ học phí. Theo PDPL Art 3.4 — dữ liệu giao dịch tài chính = **dữ liệu nhạy cảm**.

### 4.5 Technical (Kỹ thuật)
IP address, user-agent, device fingerprint, session id, cookies (xem Section 15), log truy cập, audit trail.

### 4.6 Sensitive (Nhạy cảm — special handling)
- **Sức khoẻ:** lý do vắng học có thể chứa thông tin sức khoẻ (PDPL Art 3.4 — sensitive).
- **Trẻ em dưới 16 tuổi (K-12):** toàn bộ dữ liệu của subject này được coi là nhạy cảm (PDPL Art 20 — bảo vệ dữ liệu trẻ em).
- **Dữ liệu tài chính** (mục 4.4 ở trên).

Việc xử lý dữ liệu nhạy cảm tuân theo nguyên tắc tối thiểu hoá dữ liệu (Art 3) và đăng ký với A05 nếu thuộc diện cần đăng ký theo Art 43-44 (TODO Phase 2 — A05 consultation).

## 5. Processing Purposes (Mục đích Xử lý)

**Mục đích:** Liệt kê purposes hợp pháp + cụ thể (PDPL Art 11.1.d, Art 3 — purpose limitation).

1. **Education delivery (Cung cấp dịch vụ giáo dục):** quản lý lớp học, điểm danh, giao bài, chấm điểm, tương tác giáo viên – học sinh – phụ huynh.
2. **Billing & subscription (Hoá đơn & gói dịch vụ):** xuất hoá đơn theo Nghị định 123/2020/NĐ-CP, quản lý gói KiteHub, thu học phí KiteClass.
3. **Customer support (Hỗ trợ khách hàng):** xử lý ticket, troubleshoot bug, hướng dẫn sử dụng.
4. **Analytics (Phân tích):** thống kê tình trạng sử dụng, KPI tenant, tổng hợp ẩn danh phục vụ cải tiến sản phẩm.
5. **AI features (Tính năng AI):** AI Branding (logo analysis, banner generation — local Ollama mặc định), trợ lý giáo viên — phải tuân `.claude/rules/ai-branding-guidelines.md`.
6. **Legal compliance (Tuân thủ pháp luật):** lưu hoá đơn 10 năm theo Luật Quản lý Thuế, báo cáo MoET nếu được yêu cầu.
7. **Security & fraud prevention (An ninh & chống gian lận):** phát hiện đăng nhập bất thường, audit log.

## 6. Legal Basis (Căn cứ Pháp lý)

**Mục đích:** Mỗi purpose ở Section 5 phải gắn với 1 căn cứ pháp lý hợp lệ (PDPL Art 11 + Art 17 — sự đồng ý không phải là duy nhất).

| Purpose | Căn cứ pháp lý chính | Ghi chú |
|---------|----------------------|---------|
| Education delivery | Hợp đồng (TOS — GAP-180) | Bắt buộc để cung cấp dịch vụ — không thể opt-out |
| Billing & subscription | Hợp đồng + Nghĩa vụ pháp lý (thuế) | Hoá đơn không opt-out được |
| Customer support | Lợi ích chính đáng | Có thể từ chối kênh không thiết yếu |
| Analytics (aggregated) | Lợi ích chính đáng (Art 17.1.đ) | Đã ẩn danh / tổng hợp |
| AI features | Sự đồng ý rõ ràng (opt-in) | Logo analysis local mặc định; OpenAI quốc tế chỉ khi Enterprise opt-in (xem Section 8) |
| Marketing communications | Sự đồng ý rõ ràng | Có thể rút bất kỳ lúc nào |
| Legal compliance (tax, MoET) | Nghĩa vụ pháp lý | Không opt-out được |
| Security & audit | Lợi ích chính đáng | Cần thiết để vận hành an toàn |

## 7. Data Sharing — Third Parties (Chia sẻ Dữ liệu với Bên Thứ ba)

**Mục đích:** Liệt kê nhóm bên thứ ba có thể tiếp nhận dữ liệu + khẳng định KHÔNG bán dữ liệu (PDPL Art 11.1.e).

Chúng tôi **KHÔNG bán** dữ liệu cá nhân của bất kỳ chủ thể nào. Dữ liệu chỉ được chia sẻ với các nhóm sau, theo nguyên tắc tối thiểu hoá:

- **Cổng thanh toán:** VNPay, MoMo (xử lý token thanh toán — KiteClass không nhìn thấy số thẻ).
- **Truyền thông & OTP:** Zalo OA, nhà cung cấp SMS (TODO Phase 2 chỉ định cụ thể), email transactional (SMTP/SES TODO).
- **Năng suất & văn phòng:** Google Workspace (chỉ khi tenant chủ động kết nối).
- **Hosting & hạ tầng:** AWS / Oracle Cloud (data residency Vietnam ưu tiên — xem Section 8).
- **Cơ quan nhà nước:** A05 (Cục An ninh mạng), Tổng cục Thuế, Bộ Giáo dục & Đào tạo — chỉ khi có yêu cầu pháp lý hợp lệ.

Mọi đối tác xử lý dữ liệu thay cho KiteHub (Data Processor) đều phải ký Data Processing Agreement (DPA) — TODO Phase 2 publish DPA template.

## 8. Cross-Border Transfer (Chuyển Dữ liệu Xuyên Biên giới)

**Mục đích:** Tuân thủ PDPL Art 25-27 + Cybersecurity Law 2018 Art 26 (data localization một số loại dữ liệu).

- **Mặc định:** dữ liệu lưu trữ tại Việt Nam (AWS Singapore / Oracle Cloud TODO confirm Phase 2 — nếu Singapore, đánh giá impact PDPL Art 25 trước GA).
- **AI Branding (Ollama local):** KHÔNG có cross-border transfer — model chạy trên hạ tầng KiteHub.
- **AI Branding (OpenAI quốc tế):** **chỉ khi tenant Enterprise opt-in** với disclaimer rõ ràng (xem `.claude/rules/ai-branding-guidelines.md` §2.4 + §9). Người dùng được thông báo về việc dữ liệu rời khỏi Việt Nam và phải đồng ý rõ ràng.
- **Trẻ em (K-12):** dữ liệu KHÔNG được transfer cross-border bất kể tier subscription.
- **Đánh giá tác động cross-border (Cross-Border Transfer Impact Assessment):** TODO Phase 2 — bắt buộc theo PDPL Art 25 trước khi go-live tính năng có cross-border.

## 9. Retention Period (Thời hạn Lưu trữ)

**Mục đích:** Mỗi loại dữ liệu có thời hạn cụ thể (PDPL Art 11.1.g + Art 16 — không lưu lâu hơn cần thiết).

Bảng dưới là **placeholder Phase 1**. Phase 2 (GAP-184) sẽ chốt giá trị + ký kết với DPO + Legal counsel.

| Data category | Retention (TODO — GAP-184) | Pháp lý chi phối | Ghi chú |
|---------------|----------------------------|------------------|---------|
| Identification | TODO | PDPL Art 16 + Civil Code | Xoá trong 30 ngày sau yêu cầu erasure trừ legal hold |
| Contact | TODO | PDPL Art 16 | |
| Educational | TODO | Luật Giáo dục + PDPL | Có thể cần lưu cho học bạ chính thức |
| Financial / Invoices | 10 năm | Luật Quản lý Thuế 2019 + Nghị định 123/2020 | Bắt buộc — không xoá được |
| Technical (logs) | TODO (kiến nghị 12 tháng) | PDPL + Cybersecurity Law | Audit log security 24 tháng tối thiểu |
| Sensitive (sức khoẻ, K-12) | TODO | PDPL Art 20 | Xoá ngay sau khi học sinh tốt nghiệp + grace period TODO |
| Backups | TODO | — | Backup retention align với policy |

Cross-link: chi tiết policy retention + erasure SOP nằm tại [`../04-quality/gaps/GAP-184-data-retention-deletion-policy.md`](../04-quality/gaps/GAP-184-data-retention-deletion-policy.md) (sibling skeleton — Wave Legal-BRD 2026-04-29 Agent D).

## 10. Data Subject Rights (Quyền của Chủ thể Dữ liệu — PDPL Art 11)

**Mục đích:** Bảng đầy đủ quyền + cách thực thi + SLA phản hồi (PDPL Art 9-15).

| Quyền (PDPL Art) | Mô tả ngắn | Cách thực thi | SLA phản hồi | Ghi chú |
|------------------|------------|---------------|--------------|---------|
| Right to know (Art 9) | Biết dữ liệu được xử lý | Đọc Privacy Policy này + dashboard "Quyền của tôi" | Tức thời (notice mặc định) | — |
| Right to access (Art 10) | Truy cập bản sao dữ liệu | Tự xuất từ dashboard hoặc gửi yêu cầu DPO | 20 ngày (gia hạn tối đa 10 ngày — Art 14) | Định dạng JSON/PDF |
| Right to rectification (Art 11) | Sửa dữ liệu sai | Self-edit profile hoặc gửi DPO | 20 ngày | — |
| Right to erasure (Art 12) | Yêu cầu xoá ("right to be forgotten") | Gửi DPO + xác minh danh tính | 20 ngày | Có ngoại lệ legal-hold (hoá đơn, audit log) |
| Right to restrict processing (Art 13) | Hạn chế xử lý tạm thời | Gửi DPO | 20 ngày | Trong khi xử lý dispute |
| Right to object (Art 14) | Phản đối xử lý dựa trên lợi ích chính đáng | Gửi DPO | 20 ngày | Marketing — opt-out tức thời |
| Right to data portability (Art 15) | Nhận dữ liệu format chuyển được | Self-export (planned — see GAP-188) | TODO | JSON/CSV |
| Right to lodge complaint | Khiếu nại với A05 | Gửi A05 (Cục An ninh mạng và Phòng chống tội phạm sử dụng công nghệ cao — Bộ Công an) | Theo quy định A05 | KiteHub không can thiệp; chỉ cung cấp evidence khi A05 yêu cầu |

## 11. Exercising Rights (Thực thi Quyền) — Channel + SLA

**Mục đích:** Quy trình thực hiện quyền cụ thể (PDPL Art 14 — SLA tối đa).

- **Kênh chính:** form web `https://TODO/privacy-request` (Phase 2 — endpoint chưa go-live).
- **Kênh email:** `dpo@TODO` (Phase 2).
- **Xác minh danh tính:** OTP qua email/SĐT đã đăng ký + (đối với yêu cầu erasure full account) ID document review.
- **SLA phản hồi:** **20 ngày làm việc** kể từ khi nhận yêu cầu hợp lệ; gia hạn tối đa **10 ngày bổ sung** với thông báo lý do (PDPL Art 14).
- **Phí:** miễn phí cho yêu cầu hợp lý. Yêu cầu lặp lại / quá đáng có thể chịu phí hành chính (Art 14.4).
- **Từ chối:** từ chối phải kèm lý do bằng văn bản + chỉ dẫn quyền khiếu nại lên A05.

## 12. Minor Data — Trẻ em dưới 16 tuổi

**Mục đích:** Bảo vệ đặc biệt cho dữ liệu trẻ em theo PDPL Art 20 + Civil Code Art 21 (người chưa thành niên).

- **Định nghĩa:** Trẻ em < 16 tuổi (theo Bộ luật Dân sự 2015 Art 21 — người chưa thành niên).
- **Sự đồng ý:** mọi xử lý dữ liệu của trẻ em < 16 tuổi yêu cầu **sự đồng ý của cha mẹ / người giám hộ hợp pháp** (PDPL Art 20.2).
- **Cơ chế parental consent:** TODO Phase 2 — implementation cross-link [`../04-quality/gaps/GAP-186-child-protection.md`](../04-quality/gaps/GAP-186-child-protection.md) (planned — see GAP-186).
- **Hạn chế:** không gửi marketing trực tiếp tới trẻ em; AI features high-risk (free-prompt) không khả dụng cho học sinh K-12.
- **Cross-border:** dữ liệu trẻ em **KHÔNG** transfer ra khỏi Việt Nam (xem Section 8).
- **Khi đủ 16 tuổi:** chuyển giao quyền kiểm soát dữ liệu từ phụ huynh sang học sinh (process TODO Phase 2).

## 13. Security Measures (Biện pháp Bảo mật)

**Mục đích:** Giải thích hạ tầng bảo mật bảo vệ dữ liệu (PDPL Art 27 — security obligation).

- **Mã hoá khi truyền:** TLS 1.3 cho mọi endpoint web/API.
- **Mã hoá khi lưu:** AES-256 cho database + object storage (PostgreSQL TDE / MinIO SSE TODO Phase 2 verify).
- **Phân quyền (RBAC):** vai trò admin / teacher / parent / student / accountant — least privilege per multi-tenant boundary.
- **Audit logs:** mọi truy cập dữ liệu nhạy cảm được ghi lại + lưu tối thiểu 24 tháng (cross-link `.claude/rules/logs-format-standard.md`).
- **Tách biệt tenant (multi-tenancy):** DB-level isolation per `.claude/CLAUDE.md` architecture.
- **Backups:** mã hoá + test restore định kỳ TODO (cross-link wave DR/Backup).
- **Pen-testing & vulnerability scan:** Dependabot + security audit /100 quarterly.
- **Đào tạo nhân sự:** TODO Phase 2 — annual privacy/security training cho mọi staff có quyền truy cập dữ liệu khách hàng.
- **DPIA mitigation summary:** Mitigation controls inventory + per-activity risk assessment cấu trúc theo [`dpia.md`](dpia.md) §3 (5×5 probability × impact matrix + 12-family mitigation controls). Phase 2 full risk assessment per processing activity backfilled at 50k subscriber trigger.

## 14. Breach Notification (Thông báo Sự cố)

**Mục đích:** Xử lý sự cố lộ/mất dữ liệu trong 72h (PDPL Art 23 — mandatory).

- **SLA thông báo nội bộ:** sự cố nghi ngờ phải báo DPO trong **24 giờ** kể từ phát hiện.
- **SLA thông báo cơ quan (A05):** **72 giờ** kể từ thời điểm xác định "data breach" theo định nghĩa PDPL Art 23.
- **SLA thông báo chủ thể dữ liệu:** "không chậm trễ một cách bất hợp lý" — kiến nghị 72h trừ khi cơ quan yêu cầu khác (PDPL Art 23.2).
- **Nội dung thông báo:** mô tả sự cố, dữ liệu bị ảnh hưởng, biện pháp đã thực hiện, hành động chủ thể nên làm, contact DPO.
- **Incident response runbook:** cross-link [`../04-quality/gaps/GAP-190-incident-response-runbook.md`](../04-quality/gaps/GAP-190-incident-response-runbook.md) (planned — see GAP-190).
- **Tập huấn:** quarterly tabletop exercise DPO + tech lead + on-call (TODO Phase 2).

## 15. Cookie Policy (Chính sách Cookie)

**Mục đích:** Tuân thủ PDPL Art 11 + thông lệ quốc tế (GDPR ePrivacy) đối với cookies / tracking.

- **Cookies cần thiết (necessary):** session id, CSRF token, language preference — KHÔNG yêu cầu consent (vì cần để vận hành dịch vụ).
- **Cookies analytics:** TODO Phase 2 — chỉ enable sau khi user **opt-in** trên cookie banner. Mặc định tắt cho người dùng EU/EEA + người chưa thành niên.
- **Cookies third-party tracking:** **TẮT mặc định**. KiteHub không gắn pixel quảng cáo (Facebook, Google Ads) trên app chính.
- **Cookie banner UI:** implementation tracked riêng — cookie banner UI feature gap (xem GAP-182 §"Out of Scope" + planned cookie banner gap).
- **Thời hạn cookie:** session cookies tắt khi đóng browser; persistent cookies tối đa 12 tháng (TODO Phase 2 confirm per type).
- **Quản lý cookie:** user có thể tắt qua trình duyệt + qua Privacy Center của KiteHub (TODO Phase 2).

## 16. Changes to Policy (Thay đổi Chính sách)

**Mục đích:** Quy trình thay đổi + thông báo + re-consent (PDPL Art 11.2 — material change requires re-notice).

- **Phiên bản hiện tại:** Phase 1 SKELETON 2026-04-29.
- **Material changes:** thay đổi categories of data, purposes, third parties chia sẻ, cross-border transfer, retention period — yêu cầu thông báo trước **30 ngày** (TODO Phase 2 confirm — kiến nghị 30d) qua email + in-app notification.
- **Non-material changes (typo, formatting):** publish trực tiếp + ghi changelog cuối doc.
- **Re-consent:** yêu cầu re-consent rõ ràng nếu thay đổi căn cứ pháp lý hoặc thêm xử lý dữ liệu nhạy cảm.
- **Lịch sử phiên bản:** TODO Phase 2 — duy trì changelog + giữ bản cũ để chủ thể dữ liệu tham chiếu.
- **Ngôn ngữ:** Tiếng Việt là bản gốc (canonical). Bản tiếng Anh sẽ publish ở Phase 2 (GAP-154); trong trường hợp khác biệt diễn giải, **bản tiếng Việt prevail**.

---

## Data Category Matrix (Ma trận dữ liệu × mục đích × căn cứ pháp lý × retention)

| Data category | Processing purpose | Legal basis | Retention (TODO Phase 2 — GAP-184) |
|---------------|--------------------|-------------|------------------------------------|
| Identification | Education delivery, Billing | Contract (TOS) | TODO |
| Identification (CCCD) | Billing | Legal obligation (Tax) | 10 năm |
| Contact (email/phone) | Education delivery, Support | Contract | TODO |
| Contact (marketing) | Marketing | Consent | Until withdraw |
| Educational | Education delivery | Contract | TODO (per Education Law) |
| Educational (K-12 minor) | Education delivery | Parental Consent + Contract | TODO (sensitive — see GAP-186) |
| Financial / Invoices | Billing, Tax compliance | Legal obligation | 10 năm (Tax Law) |
| Technical (logs) | Security, Analytics | Legitimate interest | 12-24 tháng (TODO) |
| Sensitive — Health (absence reasons) | Education delivery | Consent / Vital interest | TODO (sensitive — minimize) |
| Sensitive — K-12 minor full set | Education delivery | Parental consent | TODO (delete post-graduation grace) |
| AI Branding inputs (logo) | AI features | Consent | Session only (not persisted long-term) |
| Cookies (necessary) | Service operation | Legitimate interest (no consent) | Session |
| Cookies (analytics) | Analytics | Consent (opt-in) | TODO ≤12 tháng |

---

## Data Subject Rights Table (Bảng quyền chủ thể dữ liệu — VN PDPL Art 9-15)

| Quyền | Cách thực thi | Response SLA | VN PDPL Article ref |
|-------|---------------|--------------|----------------------|
| Right to know | Privacy Policy + Privacy Center dashboard | Tức thời | Decree 13/2023 Art 9 |
| Right to access | Self-export dashboard hoặc DPO request | 20 ngày (+10 gia hạn) | Decree 13/2023 Art 10 + Art 14 |
| Right to rectification | Self-edit profile hoặc DPO | 20 ngày | Decree 13/2023 Art 11 |
| Right to erasure | DPO request + identity verification | 20 ngày | Decree 13/2023 Art 12 |
| Right to restrict processing | DPO request | 20 ngày | Decree 13/2023 Art 13 |
| Right to object | DPO request (marketing — instant opt-out) | 20 ngày / instant cho marketing | Decree 13/2023 Art 14 |
| Right to data portability | Self-export (planned — see GAP-188) | TODO Phase 2 | Decree 13/2023 Art 15 |
| Right to lodge complaint | A05 (Cục An ninh mạng) | Theo A05 | Decree 13/2023 Art 9.4 + Cybersecurity Law 2018 |

---

## Cross-References (Tham chiếu chéo)

- **Sibling skeletons (Wave Legal-BRD 2026-04-29):**
  - [`./compliance-scope.md`](./compliance-scope.md) — VN legal framework mapping (đã tồn tại).
  - Terms of Service (GAP-180 Phase 1 — Agent A — sibling sẽ tồn tại post-merge): `./terms-of-service.md`.
  - Data Retention & Deletion Policy (GAP-184 Phase 1 — Agent D — sibling sẽ tồn tại post-merge): [`../04-quality/gaps/GAP-184-data-retention-deletion-policy.md`](../04-quality/gaps/GAP-184-data-retention-deletion-policy.md).
- **Planned (future Phase 2 / wave kế tiếp):**
  - GAP-186 Child Protection / Parental Consent (planned — see GAP-186)
  - GAP-188 Data Portability / Self-Export (planned — see GAP-188)
  - GAP-190 Incident Response Runbook (planned — see GAP-190)
- **Rule references:**
  - `.claude/rules/ai-branding-guidelines.md` §2.4 §8 §9 — AI inference + privacy
  - `.claude/rules/logs-format-standard.md` — PII scrubbing in logs
  - `.claude/rules/business-logic-review.md` §2.4 — VN compliance taxonomy
- **BRD docs:**
  - [`./personas-catalog.md`](./personas-catalog.md) — data subject categories cross-link
  - [`./compliance-scope.md`](./compliance-scope.md) — VN legal framework

---

## Phase 2 TODO Tracker (consolidated)

Mục cần hoàn thành ở GAP-154 Phase 2 trước khi GA:

- [ ] Designate Data Controller legal entity name + registration number + address (Section 1)
- [ ] Designate DPO + email + hotline (Section 2)
- [ ] Confirm SMS/email vendors + DPA signed (Section 7)
- [ ] Cross-Border Transfer Impact Assessment (Section 8)
- [ ] Finalize retention values per category — sync with GAP-184 (Section 9)
- [ ] Privacy request endpoint live (`/privacy-request`) (Section 11)
- [ ] Parental consent UI flow (Section 12 — GAP-186)
- [ ] Self-export endpoint (Section 10 portability — GAP-188)
- [ ] Cookie banner UI live + analytics opt-in (Section 15)
- [ ] EN translation parity (header note + ToC)
- [ ] Legal counsel sign-off (VN PDPL expert)
- [ ] A05 consultation if sensitive data registration required
- [ ] Annual privacy/security training program live (Section 13)
- [ ] Quarterly tabletop exercise scheduled (Section 14)
- [ ] Material-change notification mechanism (email + in-app banner) (Section 16)

---

## Log

- **2026-04-29 (Phase 1 SKELETON):** Created during Wave Legal-BRD Phase 1 (Agent C) per GAP-182. 16 sections theo mandate Decree 13/2023/NĐ-CP Art 11. Phase 2 (legal counsel + MPS A05 consultation + EN translation + cookie banner UI) tracked under GAP-154. Cross-links to siblings GAP-180 (TOS), GAP-184 (retention), GAP-186 (minor), GAP-188 (portability), GAP-190 (breach response).
