---
status: active
audience: ops-internal
last-updated: 2026-05-26
version: v0.9.0-beta (Phase 1 BETA)
wave: beta-prep-1-bucket-A
gaps: [GAP-PDPL-COMPLIANCE-MIN]
---

# Breach Notification SOP — Quy trình xử lý sự cố an ninh dữ liệu cá nhân

> ⚠️ **Phase 1 BETA v1 pending counsel review** — bản đầu để đáp ứng PDPL Art 13 (72h notification mandate). Phase 2 sẽ counsel-review + DPO chính thức.

**Cập nhật lần cuối:** Thứ Ba, 26/05/2026 · Đọc khoảng **8 phút** · Wave beta-prep-1 Bucket A

---

## TL;DR

Khi phát hiện sự cố an ninh dữ liệu cá nhân (data breach), KiteHub PHẢI:

1. **Phát hiện + Triage (0-4 giờ):** xác minh sự cố thật, đánh giá scope
2. **Containment (4-24 giờ):** ngăn chặn lan rộng (rotate credentials, block IP, isolate hệ thống)
3. **Thông báo cơ quan nhà nước (Bộ Công an A05) trong 72 giờ** — bắt buộc theo PDPL Art 13
4. **Thông báo người dùng bị ảnh hưởng trong 72 giờ** qua email + Zalo OA (Phase 1.5+)
5. **Post-mortem trong 7 ngày** + báo cáo cải thiện

**Liên hệ khẩn cấp Phase 1 BETA:** Nguyễn Văn Kiệt (acting DPO) — [support@kitehub.me](mailto:support@kitehub.me)

---

## 1. Định nghĩa sự cố an ninh dữ liệu

Theo PDPL Art 2 + Nghị định 13/2023/NĐ-CP Art 7, sự cố an ninh dữ liệu cá nhân là **một hoặc nhiều trong các trường hợp**:

| Loại sự cố | Ví dụ |
|---|---|
| **Vi phạm tính bảo mật** | Hacker truy cập trái phép database, leak credentials trong commit công khai, AWS S3 bucket bị set public |
| **Vi phạm tính toàn vẹn** | Database bị sửa đổi trái phép, ransomware encrypt dữ liệu |
| **Vi phạm tính khả dụng** | Mất dữ liệu vĩnh viễn do hardware failure không backup, RDS xóa nhầm |
| **Tiết lộ trái phép** | Gửi email sai recipient chứa danh sách học sinh, nhân viên KiteHub leak dữ liệu |
| **Truy cập trái phép** | User KiteHub xem được dữ liệu của tenant khác (cross-tenant leak), admin role bị bypass |

**Threshold thông báo:**
- **Bắt buộc thông báo** nếu sự cố có khả năng ảnh hưởng đến quyền + tự do của chủ thể dữ liệu (PDPL Art 13 §2)
- **Có thể không bắt buộc** nếu dữ liệu đã mã hóa unrecoverable + có biện pháp giảm thiểu hiệu quả (hiếm — phải tham vấn counsel trước khi quyết định)

## 2. Giai đoạn 1 — Phát hiện + Triage (0-4 giờ)

### 2.1 Nguồn phát hiện

- **Alert tự động:** CloudWatch alarm (failed login spike, unusual API access pattern, audit-log gap)
- **Tenant báo cáo:** email [support@kitehub.me](mailto:support@kitehub.me)
- **Internal review:** dev/ops phát hiện trong session daily check
- **External report:** security researcher, cơ quan nhà nước

### 2.2 Triage checklist

Trong vòng **4 giờ** kể từ moment phát hiện:

- [ ] **Xác minh sự cố thật** — không phải false positive
- [ ] **Đánh giá scope:**
  - Bao nhiêu user/tenant bị ảnh hưởng?
  - Loại dữ liệu nào bị compromise? (PII / PII nhạy cảm / non-PII / metadata)
  - Hệ thống nào? (DB / S3 / email / log)
  - Phương thức tấn công nếu có?
- [ ] **Ghi nhận timeline:** thời điểm phát hiện, dấu hiệu đầu tiên, scope
- [ ] **Khởi tạo incident ticket** trong `documents/04-quality/audits/incidents/YYYY-MM-DD-breach-<topic>.md`
- [ ] **Notify on-call:** Phase 1 BETA = Nguyễn Văn Kiệt (solo founder); Phase 1.5+ on-call rotation

### 2.3 Severity classification

| Severity | Tiêu chí | Ví dụ |
|---|---|---|
| **P0 — Critical** | > 100 user PII bị leak HOẶC dữ liệu nhạy cảm leak HOẶC service down > 4h | DB dump leak công khai; cross-tenant data leak ảnh hưởng > 5 tenant |
| **P1 — High** | 10-100 user PII bị leak HOẶC vi phạm bảo mật giới hạn | Audit log tampering; SQL injection chưa exploit |
| **P2 — Medium** | < 10 user PII bị leak HOẶC near-miss có biện pháp giảm thiểu | Email sai recipient, đã recall thành công |
| **P3 — Low** | Vi phạm policy nội bộ không leak dữ liệu thực tế | Log chưa scrub PII đúng cách |

## 3. Giai đoạn 2 — Containment (4-24 giờ)

### 3.1 Hành động ngăn chặn lan rộng

Theo nature của sự cố:

| Sự cố | Hành động containment |
|---|---|
| **Credentials leak** | Rotate ngay AWS Secrets Manager (JWT secret, DB password); revoke compromised tokens; force logout all sessions |
| **Unauthorized access** | Block IP attacker tại CloudFront/WAF; suspend compromised user accounts; audit log review từ giờ X back |
| **Database compromise** | Snapshot RDS ngay (forensic preservation); isolate connection từ EC2 affected; restore từ backup gần nhất nếu cần |
| **S3 public exposure** | Set bucket private ngay; review CloudTrail xem ai access; rotate credentials nếu liên quan |
| **Ransomware** | Isolate affected EC2; preserve evidence; restore từ backup (KHÔNG trả tiền theo policy KiteHub) |
| **Email sai recipient** | Recall email qua Resend API nếu < 1 phút; thông báo recipient yêu cầu xóa; thông báo affected user |

### 3.2 Forensic preservation

- **Snapshot RDS** trước khi remediate (giữ 90 ngày minimum)
- **Export CloudTrail logs** cho khoảng thời gian liên quan
- **Capture audit-log table** (`admin_audit_log` + `login_audit_log`) — đặc biệt là hash chain để verify integrity
- **Document mọi thao tác** trong incident ticket

## 4. Giai đoạn 3 — Thông báo cơ quan nhà nước (≤ 72 giờ)

### 4.1 Cơ quan tiếp nhận

Theo PDPL Art 13 + Nghị định 13/2023/NĐ-CP Art 7:

- **Cục An ninh mạng và phòng, chống tội phạm sử dụng công nghệ cao (A05) — Bộ Công an**
- Hotline: 069.2348560 (tham khảo — cần xác nhận với counsel Phase 2)
- Email: TBD (Phase 2 sẽ xác định)

### 4.2 Nội dung thông báo bắt buộc

Theo PDPL Art 13 §4, thông báo gồm:

1. **Bản chất sự cố** + thời điểm phát hiện
2. **Phạm vi ảnh hưởng:**
   - Số lượng chủ thể dữ liệu bị ảnh hưởng
   - Loại dữ liệu cá nhân bị compromise
3. **Hậu quả có thể xảy ra**
4. **Biện pháp đã thực hiện** để xử lý sự cố
5. **Biện pháp sẽ thực hiện** để giảm thiểu hậu quả
6. **Thông tin liên hệ DPO** (Phase 1 BETA: Nguyễn Văn Kiệt — [support@kitehub.me](mailto:support@kitehub.me))

### 4.3 Template email cơ quan nhà nước

```
Kính gửi: Cục An ninh mạng và phòng, chống tội phạm sử dụng công nghệ cao (A05) — Bộ Công an

Tên đơn vị báo cáo: KiteHub (Phase 1 BETA — Nguyễn Văn Kiệt acting DPO)
Email liên hệ: support@kitehub.me
Thời điểm phát hiện sự cố: [Thứ ..., DD/MM/YYYY HH:MM GMT+7]
Thời điểm gửi thông báo: [Thứ ..., DD/MM/YYYY HH:MM GMT+7] (trong vòng 72 giờ)

1. Bản chất sự cố:
[Mô tả chi tiết — vd: "Phát hiện database backup file vô tình được set public trên AWS S3 từ ngày 24/05/2026 đến 26/05/2026, chứa danh sách email của 50 user beta tenant."]

2. Phạm vi ảnh hưởng:
- Số chủ thể dữ liệu: [N user]
- Loại dữ liệu: [email, tên, số điện thoại — KHÔNG bao gồm mật khẩu (đã băm bcrypt)]
- Hệ thống: [AWS S3 bucket kitehub-backup-prod, region ap-southeast-1]

3. Hậu quả có thể xảy ra:
[vd: "User có thể nhận spam email; rủi ro phishing nếu kết hợp với dữ liệu khác."]

4. Biện pháp đã thực hiện:
- [vd: "Set bucket private trong 30 phút sau khi phát hiện."]
- [vd: "Rotate AWS credentials liên quan."]
- [vd: "Review CloudTrail log không phát hiện download trái phép từ public window."]

5. Biện pháp sẽ thực hiện:
- [vd: "Email thông báo user ảnh hưởng trong 72h."]
- [vd: "Triển khai Lambda S3 public block enforcement."]
- [vd: "Post-mortem trong 7 ngày + cải thiện S3 lifecycle policy."]

6. Liên hệ:
- Người chịu trách nhiệm: Nguyễn Văn Kiệt (acting DPO Phase 1 BETA)
- Email: support@kitehub.me
- Trạng thái dịch vụ: https://kitehub.me/beta-status

Trân trọng,
Nguyễn Văn Kiệt
KiteHub Solo Founder
```

## 5. Giai đoạn 4 — Thông báo người dùng bị ảnh hưởng (≤ 72 giờ)

### 5.1 Channels thông báo

- **Email** (primary) — gửi qua Resend production
- **Zalo OA** (secondary, Phase 1.5+) — kích hoạt khi Zalo OA active
- **Banner trong ứng dụng** — hiển thị khi user đăng nhập tiếp theo
- **Status page** `https://kitehub.me/beta-status` — public disclosure

### 5.2 Template email thông báo user

```
Subject: [QUAN TRỌNG] Thông báo sự cố an ninh dữ liệu cá nhân — KiteHub

Kính gửi anh/chị [Tên user — vd "chị Trần Thị Hồng"],

KiteHub xin trân trọng thông báo về sự cố an ninh dữ liệu cá nhân ảnh hưởng đến tài khoản của anh/chị:

📋 Bản chất sự cố:
[Mô tả ngắn gọn, không kỹ thuật — vd: "Vào ngày 24/05/2026, một tệp backup chứa danh sách email beta tenant đã vô tình được lưu công khai trên dịch vụ lưu trữ AWS trong 48 giờ. Chúng tôi đã phát hiện và khắc phục lúc 14:30 ngày 26/05/2026."]

📋 Dữ liệu liên quan đến anh/chị có thể bị ảnh hưởng:
- Email
- Tên đầy đủ
- Số điện thoại (nếu có)
- KHÔNG bao gồm: mật khẩu (đã băm bcrypt không thể recover), dữ liệu học sinh, dữ liệu thanh toán

📋 Hậu quả có thể xảy ra:
- Có thể nhận thêm email spam
- Rủi ro phishing nếu kết hợp dữ liệu khác — vui lòng cảnh giác email lạ yêu cầu nhập mật khẩu

📋 Biện pháp KiteHub đã thực hiện:
- ✅ Khắc phục lỗ hổng trong 30 phút sau khi phát hiện
- ✅ Rotate credentials hệ thống
- ✅ Thông báo Cục An ninh mạng A05 — Bộ Công an
- ✅ Đánh giá lại quy trình bảo mật

📋 Biện pháp anh/chị nên thực hiện:
- ✅ Đổi mật khẩu KiteHub tại Cài đặt → Bảo mật
- ✅ Bật xác thực 2 lớp (sẽ ra mắt Phase 2)
- ✅ Cảnh giác email lạ yêu cầu nhập thông tin nhạy cảm

📋 Quyền của anh/chị theo PDPL Art 11:
- Truy cập dữ liệu cá nhân của mình
- Yêu cầu xóa tài khoản (right to be forgotten)
- Khiếu nại tới Bộ Công an A05 nếu chưa hài lòng

📞 Liên hệ:
- Email: support@kitehub.me
- DPO Phase 1 BETA: Nguyễn Văn Kiệt
- Trạng thái dịch vụ: https://kitehub.me/beta-status

KiteHub xin lỗi vì sự bất tiện này và cam kết minh bạch trong xử lý sự cố. Chúng tôi cảm ơn anh/chị đã đồng hành cùng beta và mong tiếp tục nhận được sự ủng hộ.

Trân trọng,
Nguyễn Văn Kiệt
Founder KiteHub
```

### 5.3 Tone matrix theo persona

| Persona | Greeting | Tone |
|---|---|---|
| **P2 Center Owner** | `Em chào chị/anh [Tên],` | Formal-respectful, kèm xin lỗi rõ ràng |
| **P3 Center Manager** | `Em chào chị/anh [Tên],` | Formal, kỹ thuật hơn |
| **P1 Solo Teacher** | `Chào em/bạn [Tên],` | Casual-friendly nhưng nghiêm túc |
| **Parent (P4 Phase 2+)** | `Kính gửi quý phụ huynh,` | Very formal, focus trẻ em |

## 6. Giai đoạn 5 — Post-mortem (≤ 7 ngày)

### 6.1 Post-mortem document

Tạo file `documents/04-quality/audits/incidents/YYYY-MM-DD-breach-<topic>-post-mortem.md` theo template `documents/05-guides/operations/post-mortem-template.md` với sections:

1. **Timeline** — thời gian phát hiện, containment, notification, resolve
2. **Root cause** — nguyên nhân kỹ thuật + process
3. **Impact** — số user/tenant/dữ liệu bị ảnh hưởng
4. **What went well** — phát hiện nhanh, containment hiệu quả
5. **What went wrong** — gaps trong detection, response
6. **Action items** — concrete improvements với owner + deadline
7. **Compliance verification** — PDPL Art 13 timeline met (≤ 72h)

### 6.2 Action items priority

| Loại action | Deadline |
|---|---|
| Patch vulnerability (technical) | 7 ngày |
| Process improvement (runbook update) | 14 ngày |
| Detection enhancement (monitoring/alert) | 30 ngày |
| Training (team education) | 60 ngày |

## 7. Lưu trữ + Reporting

- **Incident log:** lưu trong `documents/04-quality/audits/incidents/` retention 5 năm (per audit log standard)
- **Báo cáo thường niên:** Phase 2 sẽ ship annual privacy report tóm tắt incidents tới cộng đồng
- **Quarterly retro:** mỗi quý review tất cả incidents + identify systemic patterns
- **Communication archive:** giữ bản sao email thông báo cơ quan + user trong 5 năm

## 8. Liên hệ + Escalation

| Tình huống | Liên hệ |
|---|---|
| P0/P1 incident | Nguyễn Văn Kiệt — [support@kitehub.me](mailto:support@kitehub.me) (Phase 1 BETA) |
| Cơ quan nhà nước A05 | Hotline 069.2348560 (tham khảo, cần xác nhận với counsel) |
| User affected reporting back | [support@kitehub.me](mailto:support@kitehub.me) (24h SLA Phase 1 BETA) |
| Counsel review (Phase 2) | TBD — sẽ engage trước Phase 2 |

## 9. Tham chiếu

- [`documents/01-business/legal/privacy-notice.md`](../../01-business/legal/privacy-notice.md) — Thông báo bảo mật
- [`documents/01-business/legal/terms-of-service.md`](../../01-business/legal/terms-of-service.md) — Điều khoản dịch vụ
- [`documents/01-business/legal/data-retention-policy.md`](../../01-business/legal/data-retention-policy.md) — Chính sách lưu trữ
- [`documents/05-guides/operations/incident-response-runbook.md`](incident-response-runbook.md) — General incident runbook
- [`documents/05-guides/operations/post-mortem-template.md`](post-mortem-template.md) — Post-mortem template
- [`documents/05-guides/operations/incident-comms-runbook.md`](incident-comms-runbook.md) — Communication runbook
- **PDPL 2023 Art 13:** thông báo sự cố trong 72 giờ
- **Nghị định 13/2023/NĐ-CP Art 7:** quy trình thông báo
- **Luật An ninh mạng 2018:** nghĩa vụ bảo vệ dữ liệu

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 🐛 Báo lỗi runbook này: [support@kitehub.me?subject=Lỗi runbook breach-notification](mailto:support@kitehub.me?subject=L%E1%BB%97i%20runbook%20breach-notification)

**Phiên bản:** v0.9.0-beta v1 pending counsel review · **Cập nhật:** Thứ Ba, 26/05/2026 · **Wave:** beta-prep-1 Bucket A
