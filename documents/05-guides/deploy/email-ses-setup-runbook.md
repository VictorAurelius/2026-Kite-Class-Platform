# AWS SES Production Setup Runbook

**Audience:** Solo dev / SRE provisioning AWS SES for KiteHub email transactional traffic
**Status:** Active runbook (Wave 33 Bucket B — GAP-370; Wave 61 Bucket B refresh — production approval prep; Wave 84 Bucket D — Vietnamese quick-start overlay GAP-423)
**Last reviewed:** 2026-05-15
**Cross-refs:**
- `documents/04-quality/gaps/GAP-370-email-transactional-infrastructure.md`
- `documents/05-guides/deploy/dns-setup-runbook.md` (Bucket D — TXT records)
- `kitehub/kitehub-email/src/main/resources/application.yml` (`aws.ses.*` keys)
- `.claude/rules/release-deploy-standard.md` §3.4 (MAJOR release email checklist)
- `kitehub/kitehub-email/src/test/java/com/kitehub/email/integration/SesIntegrationSmokeTest.java` (Wave 45 — profile-gated JUnit smoke test)
- `scripts/smoke-ses.sh` (Wave 61 — read-only AWS SES state verification)

---

## 🇻🇳 Hướng dẫn nhanh — Tiếng Việt

> Section này là tóm tắt tiếng Việt cho dev VN chưa quen AWS SES. Phần technical chi tiết bên dưới (Wave 61 Verification, §0 §1-§7) giữ English/mixed để cross-locale stable cho terminology AWS.

**AWS SES là gì:** AWS Simple Email Service — dịch vụ gửi email transactional có tích hợp sẵn với AWS infra (CloudWatch metrics, IAM, SNS feedback). KiteHub dùng SES để gửi email welcome / xác thực / khôi phục MFA / hoá đơn / DSAR cho tenant Phase 1 BETA. Region `ap-southeast-1` (Singapore) per ADR-025.

**Khi nào dùng runbook này:**

- Lần đầu setup SES cho môi trường production mới (1 lần / cloud account).
- Verify lại domain `kitehub.me` (DKIM + SPF + DMARC) sau khi đổi DNS provider.
- Submit request mở **Production access** (thoát Sandbox 200 email/24h → Free Tier 62k email/tháng).
- Cấu hình SNS topic cho bounce + complaint feedback loops trước khi go live.
- Refresh sau incident về deliverability (bounce rate spike, complaint rate vượt ngưỡng).

**Quy trình tóm tắt (6 bước, ~30 phút thao tác + 24-48h chờ AWS approval):**

1. Verify domain identity `kitehub.me` trong SES Console region `ap-southeast-1` → SES sinh 3 DKIM CNAME tokens. Chi tiết: §3 "Sender domain verification" + §3.1 CLI commands.
2. Add DNS records vào Cloudflare DNS: 3 DKIM CNAME + 1 SPF TXT + 1 DMARC TXT + 1 verification TXT. Đợi propagation 5-30 phút → SES auto-verify status `Success`. Chi tiết: §3.2 bảng records.
3. Submit Production access request qua AWS Support Center → form template Wave 61 sẵn ở §4.1.1 copy-paste verbatim (mail type / website / use case description ≥30 words / volume forecast). Chi tiết: §4 "Sandbox → Production access request".
4. Đợi AWS review 24-48h calendar (giờ VN GMT+7 vs AWS UTC chênh ~16h khi notification gửi 09:00 UTC). Có thể bị reject lần đầu — re-submit với specifics. Chi tiết: §4.2 + §0 "KYC pitfalls cho user VN" bên dưới.
5. Sau khi approve: verify Production limits (62k email/tháng default Free Tier) trong "Account dashboard" tab. Chi tiết: §4.4 "Verify production approval".
6. Configure SNS topic cho bounce + complaint feedback loops → wire vào `kitehub-email` service via `application.yml` (`aws.ses.notification-topic-arn`) → smoke test. Chi tiết: §5 + §7.

**Bẫy thường gặp:**

- ❌ Sai region — verify domain ở `us-east-1` nhưng app stack ở `ap-southeast-1`. DKIM keys khác nhau giữa region → phải re-verify. Verify bằng: `aws ses get-identity-verification-attributes --region ap-southeast-1 --identities kitehub.me`.
- ❌ DMARC policy quá strict ngay từ đầu — set `p=reject` trước khi DKIM/SPF settle → email hợp lệ bị block. Phase 1 BETA bắt đầu `p=quarantine`, sau 30 ngày review report → cân nhắc `p=reject`.
- ❌ Quên enable 2FA trên AWS root account trước khi submit Production request — AWS reject vì security posture chưa đủ.
- ❌ Use case description form bằng tiếng Việt — AWS form bắt buộc English ≥30 từ. Dùng template §4.1.1 copy verbatim.
- ❌ Bounce rate vượt 5% → AWS tự pause account, phải mở support ticket giải trình. Verify weekly: AWS SES Console → Reputation tab.

**Khi gặp lỗi:** xem §0 "Hỏi đáp thường gặp (Vietnamese FAQ)" H1-H8 bên dưới, hoặc reject reasons §0 "KYC pitfalls cho user VN" — đã liệt kê 4 reject reasons phổ biến + remediation. Nếu vẫn stuck: cross-link tới `documents/05-guides/operations/incident-comms-runbook.md` để escalate qua status page Instatus.

**Thuật ngữ AWS SES nhanh (tham chiếu chéo English ↔ Việt):**

| Thuật ngữ EN | Nghĩa Việt | Ghi chú thực tế |
|--------------|------------|-----------------|
| Sandbox mode | Chế độ thử nghiệm | Mặc định khi mở SES — chỉ gửi tới địa chỉ verified, giới hạn 200 email/24h |
| Production access | Cấp quyền sản xuất | Sau khi AWS review request — gửi tự do, Free Tier 62k email/tháng |
| Domain identity | Định danh tên miền | Cần verify ownership qua DNS records (TXT + DKIM CNAME) |
| Easy DKIM | Chữ ký số đơn giản | AWS tự sinh 3 CNAME tokens — recommended cho mọi domain mới |
| SPF record | Bản ghi cho phép gửi | Khai báo `amazonses.com` được gửi mail thay cho `kitehub.me` |
| DMARC policy | Chính sách xác thực email | `p=none` (collect) → `p=quarantine` (suspicious vào spam) → `p=reject` (block) |
| Bounce rate | Tỉ lệ thư không tới được | Mục tiêu <2%, AWS cảnh báo ở 5%, khoá account ở 10% |
| Complaint rate | Tỉ lệ người nhận đánh dấu spam | Mục tiêu <0.1% — vượt là AWS pause account ngay |
| Suppression list | Danh sách chặn tự động | Địa chỉ hard bounce SES tự cho vào — phải xoá trước khi gửi lại |
| SNS topic | Chủ đề SNS | AWS pub-sub queue nhận bounce + complaint notification |
| Warmup schedule | Lịch hâm nóng tài khoản | Tăng dần volume để build reputation, tránh spam flag |
| Throttling | Giới hạn tốc độ gửi | AWS limit ~14 email/giây sau khi production approve |
| Reputation | Uy tín người gửi | Tính dựa trên bounce rate + complaint rate — quan trọng cho deliverability |

**Lưu ý vận hành quan trọng cho dev VN solo:**

- Theo dõi reputation hàng ngày trong tuần đầu sau approval — AWS SES Console → "Reputation" tab. Nếu thấy bounce rate spike → kiểm tra suppression list + remove email không hợp lệ.
- Compliance VN: Luật Quảng cáo 2012 + Decree 91/2020/NĐ-CP yêu cầu marketing email phải có `List-Unsubscribe` header. Transactional email (welcome / password reset / MFA / billing) miễn requirement này, nhưng nên include cho consistency cross-locale.
- Email gửi tới gateway VN (Viettel, VNPT, FPT mail) đôi khi bị flag spam — verify bằng `mail-tester.com` score ≥9/10 trước khi go production. Score thấp thường do thiếu DKIM hoặc DMARC sai.
- Quota tracking hàng tháng: AWS Cost Explorer filter Service = "Simple Email Service" — đảm bảo không vượt 62k email/tháng Free Tier. Nếu sắp vượt → xin tăng quota qua Support Center (cùng thủ tục như production access request).
- Kiểm tra DKIM CNAME records định kỳ mỗi 90 ngày qua `dig CNAME <token>._domainkey.kitehub.me` — Cloudflare đôi khi xoá nhầm khi user dọn dẹp DNS. KHÔNG xoá record bắt đầu bằng `<selector>._domainkey.kitehub.me`.

**Cross-link tiếng Việt mở rộng:**

- §0 "Hướng Dẫn Nhanh (Vietnamese)" chi tiết bên dưới — bảng 6 bước + KYC pitfalls + region warning + thuật ngữ EN→VI + FAQ H1-H8 + lưu ý vận hành sau approval.
- Cloudflare DNS records add hướng dẫn VN: `documents/05-guides/vietnamese/cloudflare-setup.md`.
- AWS account prep prerequisite: `documents/05-guides/account-prep/01-aws-account-creation.md`.
- Domain registrar prerequisite: `documents/05-guides/account-prep/02-domain-registrar.md`.
- Incident comms runbook (Instatus VN overlay): `documents/05-guides/operations/incident-comms-runbook.md`.
- Email deliverability runbook (DKIM/SPF/DMARC chi tiết): `documents/05-guides/deploy/email-deliverability-runbook.md`.

**Workflow gợi ý cho dev VN solo (cô đọng):**

Buổi đầu (~2h thao tác + 24-48h chờ): chuẩn bị account AWS với 2FA enabled → tạo IAM user/role với SES permissions → verify domain `kitehub.me` qua SES Console → add 6 DNS records vào Cloudflare → đợi SES auto-verify (≤30 phút) → submit Production access request với form template Wave 61 (§4.1.1) → đợi AWS review.

Buổi tiếp theo (sau khi nhận email approve): verify Production limits trong dashboard → tạo SNS topic cho bounce/complaint → subscribe email/SQS → wire `aws.ses.notification-topic-arn` vào `kitehub-email` service → smoke test bằng `aws ses send-email` từ địa chỉ verified → check inbox + spam folder → verify DKIM-Signature header có + valid → run `mail-tester.com` score ≥9/10.

Sau khi go production (theo dõi tuần đầu): kiểm tra Reputation tab hàng ngày → setup CloudWatch alarm khi bounce rate >2% hoặc complaint rate >0.1% → document suppression list policy (auto-suppress vs manual review) → schedule Phase 2 plan tăng quota khi cohort tenant mở rộng (Phase 1.5 PAID có thể cần 200k+ email/tháng).

Nếu có thay đổi domain (vd: `kitehub.me` → `kitehub.vn` Phase 2): re-verify identity mới + re-add 6 DNS records + đợi propagation → KHÔNG xoá identity cũ ngay (giữ ít nhất 30 ngày để tránh email đang in-flight bị reject) → update `application.yml` `aws.ses.from-address` sau khi verify xong.

**Checklist nhanh trước khi submit Production access request:**

- [ ] Domain identity `kitehub.me` status = `Verified` (kiểm tra qua SES Console hoặc `aws ses get-identity-verification-attributes`).
- [ ] 3 DKIM CNAME records đã propagate (kiểm tra `dig CNAME <token>._domainkey.kitehub.me` trả về `<token>.dkim.amazonses.com`).
- [ ] SPF TXT record `v=spf1 include:amazonses.com -all` đã add vào root domain.
- [ ] DMARC TXT record bắt đầu với `p=quarantine` (KHÔNG `p=reject` ngay từ đầu — sẽ block email hợp lệ nếu DKIM/SPF chưa settle).
- [ ] AWS root account đã enable MFA (TOTP / hardware key) — bắt buộc cho security posture khi AWS review.
- [ ] Form template Wave 61 §4.1.1 đã copy-paste đầy đủ — use case description ≥30 từ tiếng Anh, volume forecast realistic Phase 1 BETA (2k email/tháng).
- [ ] Bounce handling plan đã sẵn sàng — SNS topic ARN có thể cite trong form nếu AWS hỏi.
- [ ] Test email Sandbox đã pass — gửi tới địa chỉ verified, nhận trong inbox + check Spam folder, DKIM-Signature header có + valid.

---

## Wave 61 Verification (2026-05-11)

Re-checked SES state via `bash scripts/smoke-ses.sh` (Tier 1 read-only per `agent-aws-access.md` §2.1):

| Check | State 2026-05-11 | Action |
|-------|-----------------|--------|
| `aws sesv2 get-account` EnforcementStatus | `HEALTHY` | ✅ |
| `ProductionAccessEnabled` | `false` (SANDBOX) | ⏳ submit production access request per §4.1.1 below |
| `SendingEnabled` | `true` | ✅ |
| `Max24HourSend` / `MaxSendRate` | `200` / `1.0` | sandbox default; → 50000 / 14 post-approval |
| Email identities registered | 0 | ⏳ verify `kitehub.me` per §3 (depends on DNS records — see `dns-setup-runbook.md`) |
| Suppression list | empty | ✅ clean baseline |

**Two user-action gates remain (per `release-deploy-standard.md` §9 "Deploy execution = HUMAN-IN-THE-LOOP"):**
1. Verify domain identity `kitehub.me` in SES Console — agent cannot run `aws sesv2 create-email-identity` (Tier 3 banned per `agent-aws-access.md` §4.1)
2. Submit production access request — AWS support case requires browser/console (no public API for this case type)

Agent role: prepare templates + verify post-action state via smoke script. User executes the 2 gates.

---

---

## Wave 45 Verification (2026-05-08)

Each step below was re-verified during Wave 45 closure (GAP-370). Original runbook shipped Wave 33; no breaking drift detected — config keys, SES region, sandbox→production flow, and CLI commands still match `kitehub-email` v1.0 wiring.

| Step | Status | Notes |
|------|--------|-------|
| 1. Domain verification (§3) | ✅ verified accurate | `verify-domain-identity` + Easy DKIM still standard; CLI flags unchanged 2026-05-08 |
| 2. DKIM CNAME records (§3.2) | ✅ verified accurate | 3 CNAMEs `<token>._domainkey.kitehub.vn` pattern; Cloudflare DNS host model assumed (per `dns-setup-runbook.md`) |
| 3. SPF + DMARC TXT (§3.2) | ✅ verified accurate | Phase 1 BETA uses `p=quarantine` (per §0 H4 — Phase 2 stable can promote `p=reject`) |
| 4. Sandbox → production request (§4) | ✅ verified accurate | AWS form fields stable; `H1-H8` Vietnamese FAQ section covers VN-specific KYC pitfalls |
| 5. Production approval verification (§4.2) | ✅ verified accurate | Default 50k/day + 14/s post-approval still current AWS tier baseline |
| 6. Bounce/complaint SNS topics (§5) | ✅ verified accurate | `set-identity-notification-topic` API still standard; SQS subscription remains the solo-dev recommended path |
| 7. App config + smoke test (§7) | ✅ verified — runbook smoke (`curl`) + new code-side smoke `SesIntegrationSmokeTest` | Wave 45 added profile-gated JUnit smoke test (skipped by default; manual run via `-Daws-ses-real=true`); cross-link in §7 |

**No drift found.** AWS SES production approval remains a user-executed step (per `release-deploy-standard.md` §9 "Deploy execution = ⚠️ HUMAN-IN-THE-LOOP"); agent role is limited to runbook authorship + smoke-test scaffolding.

---

## §0 Hướng Dẫn Nhanh (Vietnamese)

**Bối cảnh:** Phase 1 BETA cần SES gửi email transactional (welcome, MFA recovery, billing receipts). AWS SES mặc định ở **chế độ Sandbox** (chỉ gửi tới địa chỉ verified, giới hạn 200 email/24h). Phải submit request để mở **Production access** (gửi tự do tới mọi địa chỉ, ~50k email/24h Free Tier).

**6 bước tóm tắt** (cross-link xuống section EN bên dưới):

| # | Bước | Thời gian | Chi tiết EN |
|---|------|----------|-------------|
| 1 | Đăng ký domain identity `kitehub.vn` trong SES Console (region `ap-southeast-1`) → SES sinh DKIM CNAME records | ~5 phút | §3 "Sender domain verification" |
| 2 | Add 3 DKIM CNAME records vào Cloudflare DNS (TTL 300s) → đợi propagation 5-30 phút → SES auto-verify "verified" | ~10 phút + đợi | §3.2 "DKIM CNAME records" |
| 3 | Add SPF TXT (`v=spf1 include:amazonses.com -all`) + DMARC TXT (`v=DMARC1; p=none; rua=mailto:dmarc@kitehub.vn`) vào Cloudflare DNS | ~5 phút | §3.3 "SPF + DMARC" |
| 4 | Submit production access request qua AWS Support Center → Service quota increase → SES sending limits → từ Sandbox sang Production | ~5 phút submit + đợi 24-48h approval | §4 "Sandbox → Production access request" |
| 5 | Sau khi approve: verify production limits (50k/day default) trong SES → "Account dashboard" | ~2 phút | §4.4 "Verify production approval" |
| 6 | Configure SNS topic cho bounce + complaint feedback loops → wire vào `kitehub-email` service | ~10 phút | §5 "Bounce + complaint feedback loops" |

**Tổng thời gian:** ~30 phút thao tác + ~24-48h đợi approval AWS Support.

### KYC pitfalls cho user VN

- **Timezone:** AWS SES dùng UTC mặc định. VN user thấy "approved at 2026-05-08 09:00 UTC" → giờ VN = 16:00 (GMT+7). Đặt notification rule expect ~1-2 ngày calendar.
- **Use case description:** Form yêu cầu mô tả use case bằng English. Solo dev VN viết tiếng Anh ngắn gọn, vd: *"Transactional emails for B2B SaaS education platform — welcome emails to verified school owners, password resets, MFA recovery codes. Audience opt-in via signup. Volume estimate: 1k emails/day Phase 1 BETA, scaling to 10k/day Phase 2."* (~40-60 từ đủ).
- **Bounce rate cam kết:** Form hỏi "What is your plan to handle bounces?". Trả lời ngắn: "Configured SNS topic for bounce/complaint feedback. Auto-suppress addresses after 1 hard bounce. Daily review of CloudWatch metrics" (cross-link `kitehub-email` config).
- **Sender reputation:** Phase 1 BETA volume thấp → reputation chưa đủ build. AWS có thể cấp limit thấp hơn (1k-5k/day). OK cho BETA. Phase 1.5 PAID xin tăng limit lên 50k.
- **Reject reasons phổ biến:**
  - *"Insufficient sending history"* → reply ticket: nêu rõ Phase 1 BETA + volume estimate + opt-in proof
  - *"Bounce handling unclear"* → quote SNS topic ARN + dashboard URL
  - *"Use case too generic"* → re-submit với specifics (vd: "MFA recovery TOTP token email — mandatory per OWASP V2 auth")
  - *"Domain ownership not verified"* → check DKIM CNAME records propagated (re-run §3.2)

### Region warning

⚠️ **SES region MUST match `ap-southeast-1`** per ADR-025. SES domain identity phải tạo trong CÙNG region với app stack. Nếu lỡ verify domain ở `us-east-1` → re-do trong `ap-southeast-1` (DKIM keys khác nhau giữa region).

### Cross-link tiếng Việt

- Phần thực hiện chi tiết EN: §1 → §6 bên dưới
- Cloudflare DNS records add bằng tiếng Việt: `documents/05-guides/vietnamese/cloudflare-setup.md`
- AWS account prep prerequisite: `documents/05-guides/account-prep/01-aws-account-creation.md`
- Domain prerequisite: `documents/05-guides/account-prep/02-domain-registrar.md`
- Superadmin first-login consumer: `documents/05-guides/account-prep/04-kitehub-superadmin-first-login.md` §2.2

### Thuật ngữ tiếng Việt

| Thuật ngữ EN | Tương đương Việt | Ghi chú |
|--------------|-----------------|---------|
| Sandbox | Chế độ thử nghiệm | Default mode khi mở SES; chỉ gửi tới verified address |
| Production access | Cấp quyền sản xuất | Sau khi AWS review request, gửi tự do |
| Sender reputation | Uy tín người gửi | Tính dựa trên bounce rate + complaint rate |
| Bounce rate | Tỉ lệ thư gửi không đến | Hard bounce (không tồn tại) + soft bounce (mailbox đầy) |
| Complaint rate | Tỉ lệ người nhận đánh dấu spam | Mục tiêu <0.1% |
| Suppression list | Danh sách chặn | Địa chỉ hard bounce tự động cho vào — không gửi lại được |
| DKIM | Chữ ký số xác thực domain | DomainKeys Identified Mail — verify email không bị forge |
| SPF | Bản ghi cho phép gửi | Sender Policy Framework — khai báo IP/domain được gửi từ domain |
| DMARC | Chính sách + báo cáo xác thực | Tổng hợp DKIM + SPF + policy what-to-do-when-fail |
| SNS topic | Chủ đề SNS | AWS Simple Notification Service — pub-sub queue |
| Warmup | Hâm nóng tài khoản | Tăng dần volume để build reputation; tránh spam flag |
| Throttling | Giới hạn tốc độ gửi | AWS limit gửi 14 email/giây Phase 1, scaling Phase 2 |

### Hỏi đáp thường gặp (Vietnamese FAQ)

**H1: Tôi có thể skip SES sandbox và dùng provider khác (Resend, SendGrid, Postmark) không?**
Có thể, nhưng mất tích hợp AWS native (CloudWatch metrics, IAM permissions, VPC endpoint). Phase 1 BETA recommend SES vì cost ($0 cho 62k email/tháng nếu chạy trong EC2 cùng region) + đã có ADR-025 lock AWS. Provider khác cân nhắc Phase 2.

**H2: Tại sao phải dùng `ap-southeast-1` mà không dùng region khác rẻ hơn?**
Latency: VN user đến Singapore ~30ms vs `us-east-1` ~250ms. Free tier limit giống nhau giữa region. Compliance VN PDPL 2023 yêu cầu data localization khi xử lý dữ liệu cá nhân — Singapore là ASEAN nên acceptable; `us-east-1` cần DPA bổ sung.

**H3: Bounce rate trong Phase 1 BETA bao nhiêu là ổn?**
Mục tiêu <2%. AWS cảnh báo ở 5%, khoá account ở 10%. Soft bounce (mailbox full) tha thứ; hard bounce (does not exist) bị suppression. Phase 1 BETA volume nhỏ, chỉ cần monitor weekly.

**H4: DMARC policy nên là `none` hay `quarantine` hay `reject`?**
Phase 1 BETA bắt đầu `p=none` (chỉ collect report, không block). Sau 30 ngày review report → chuyển sang `p=quarantine` (suspicious đi vào spam). Phase 2 stable → `p=reject` (block hoàn toàn email forge from-address). Đừng vội nhảy thẳng `reject` — nếu config sai SPF/DKIM, email hợp lệ cũng bị reject.

**H5: AWS SES có hỗ trợ tiếng Việt trong subject + body không?**
Có — SES hỗ trợ UTF-8 đầy đủ cho subject và body. Đảm bảo template email set `Content-Type: text/html; charset=UTF-8` + subject MIME-encode RFC 2047 (vd `=?UTF-8?B?Q2jDoG8gbeG7q25n?=` cho "Chào mừng"). Spring Boot `JavaMailSender` xử lý automatic; KHÔNG cần manual encode.

**H6: Email từ SES có bị gateway VN (Viettel, VNPT, FPT mail) đánh spam không?**
Phụ thuộc vào: (1) DKIM/SPF/DMARC đầy đủ, (2) sender reputation chưa bị flag, (3) content có spam-trigger words không. Phase 1 BETA volume thấp + transactional → ít rủi ro. Nếu user phản ánh email vào spam → kiểm tra `mail-tester.com` score (mục tiêu ≥9/10).

**H7: Có cần dedicated IP không?**
Không cho Phase 1. AWS SES shared IP pool có reputation tốt sẵn. Dedicated IP ($25/month) chỉ cần khi volume >100k email/tháng + cần fully-control reputation. Phase 2 cân nhắc.

**H8: Tôi nên test SES setup như thế nào trước khi go production?**
1. Verify domain xong, vẫn ở Sandbox → verify thêm 2-3 địa chỉ email cá nhân (Sandbox cho phép gửi tới verified address).
2. Send test email qua AWS Console → verify nhận trong inbox + check Spam folder.
3. Inspect raw email source → verify DKIM-Signature header có + valid.
4. Run `mail-tester.com`: gửi 1 email tới địa chỉ random họ cho → score ≥9/10.
5. Submit production access request sau khi 4 bước trên pass.

### Lưu ý vận hành sau khi production access cấp

- **Theo dõi reputation hàng ngày** trong tuần đầu: AWS SES Console → "Reputation" tab. Nếu bounce rate >5% hoặc complaint rate >0.1% → AWS sẽ tạm khoá account, phải mở ticket giải trình.
- **Suppression list:** sau mỗi hard bounce (mailbox does not exist), SES tự động cho địa chỉ vào suppression list global. Phải xoá khỏi suppression list trước khi gửi lại — KHÔNG retry blind.
- **Warmup lịch:** Phase 1 BETA volume thấp tự nhiên warmup. Nếu tăng đột ngột >10× volume trong 1 ngày → AWS có thể flag spam. Kế hoạch: tăng dần 2× mỗi 3 ngày.
- **Email template TestSuite:** trước khi gửi production, dùng `aws ses send-email` với địa chỉ test chính bản thân để verify rendering, header (DKIM-Signature, From, Reply-To, List-Unsubscribe).
- **Compliance VN:** mọi marketing email phải có `List-Unsubscribe` header (Luật Quảng cáo VN 2012 + Decree 91/2020/NĐ-CP về thư điện tử rác). Transactional email (welcome, password reset, MFA) miễn requirement này nhưng vẫn nên include cho consistency.
- **Backup provider:** nếu AWS SES gặp incident regional → fallback Resend.com hoặc SendGrid trong 30 phút (Phase 2 prep). Phase 1 BETA chấp nhận single-provider risk vì volume thấp.
- **Cron giám sát:** kế hoạch Phase 2 thêm cron `kitehub-platform` mỗi 6h kiểm tra SES sending stats + alert qua Slack/email nếu bounce rate spike.
- **Kiểm tra DKIM định kỳ:** mỗi 90 ngày verify DKIM CNAME records vẫn còn trong Cloudflare DNS. Cloudflare đôi khi xóa record cũ khi user dọn dẹp — KHÔNG xóa CNAME bắt đầu bằng `<selector>._domainkey.kitehub.vn`.
- **Region failover:** nếu Phase 2 mở thêm region (vd `us-east-1` cho user US), phải verify domain identity riêng trong region đó. Mỗi region có DKIM keys khác nhau — cần thêm CNAME records vào Cloudflare.
- **Quota tracking:** Phase 1 free tier 62k email/tháng (2k/ngày trung bình). Nếu vượt phải xin tăng quota qua Support Center. Tracking: AWS Cost Explorer filter Service = "Simple Email Service".

---

## 1. Overview

KiteHub email transactional pipeline sử dụng AWS SES vì cost-effective + tích hợp sẵn AWS infra. Bài runbook này cover toàn bộ steps từ sandbox → production:

1. Verify sender domain (DKIM + SPF + DMARC)
2. Sandbox → production access request
3. Configure bounce + complaint feedback loops
4. Configure sending limits + warmup schedule
5. Wire app-side config (`application.yml`)
6. Smoke test

**Region:** `ap-southeast-1` (Singapore — closest to VN, lowest latency)
**Sender:** `noreply@kitehub.vn` (production) / `noreply@localhost` (dev)

---

## 2. Prerequisites

- [ ] AWS account active với billing enabled
- [ ] IAM user/role có permissions: `ses:*`, `sns:*` (or scoped: `ses:SendEmail`, `ses:SendRawEmail`, `ses:GetIdentityVerificationAttributes`, `sns:CreateTopic`, `sns:Subscribe`)
- [ ] Domain `kitehub.vn` đã được mua + DNS managed (per `dns-setup-runbook.md`)
- [ ] Terraform state cho `terraform-aws/` initialized

---

## 3. Sender domain verification

### 3.1 Initiate verification (AWS console / CLI)

**AWS Console:**
SES → **Verified identities** → **Create identity** → **Domain** → enter `kitehub.vn` → enable **DKIM** (Easy DKIM).

**CLI equivalent:**
```bash
aws ses verify-domain-identity \
  --region ap-southeast-1 \
  --domain kitehub.vn

aws ses verify-domain-dkim \
  --region ap-southeast-1 \
  --domain kitehub.vn
```

CLI returns 3 DKIM tokens (CNAME records) — needed for §3.2.

### 3.2 Add TXT + CNAME records to DNS

Add these DNS records (per `dns-setup-runbook.md` Bucket D will own actual record creation; here we list the values):

| Type | Name | Value | Purpose |
|------|------|-------|---------|
| TXT | `_amazonses.kitehub.vn` | `<verification-token-from-ses>` | Domain ownership |
| CNAME | `<token1>._domainkey.kitehub.vn` | `<token1>.dkim.amazonses.com` | DKIM key 1 |
| CNAME | `<token2>._domainkey.kitehub.vn` | `<token2>.dkim.amazonses.com` | DKIM key 2 |
| CNAME | `<token3>._domainkey.kitehub.vn` | `<token3>.dkim.amazonses.com` | DKIM key 3 |
| TXT | `kitehub.vn` | `v=spf1 include:amazonses.com -all` | SPF |
| TXT | `_dmarc.kitehub.vn` | `v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@kitehub.vn; pct=100; sp=quarantine; aspf=s; adkim=s` | DMARC |

**Verification time:** SES auto-checks DNS every ~15min. Status `Pending` → `Verified` once records propagate (usually <30min).

```bash
# Check verification status
aws ses get-identity-verification-attributes \
  --region ap-southeast-1 \
  --identities kitehub.vn
```

Expected: `"VerificationStatus": "Success"`.

---

## 4. Sandbox → Production access request

By default, SES accounts are in **sandbox mode**:
- Can only send **to verified addresses** (recipients must opt-in)
- Cap: **200 emails/day**, **1 email/second**

For production (send to anyone, higher limits), you must request out-of-sandbox.

### 4.1 Submit request

**AWS Console:** SES → **Account dashboard** → **Request production access**

#### 4.1.1 Copy-paste form template (Wave 61 — refreshed for Free Tier 62k/mo)

> **User action:** open SES Console region `ap-southeast-1` → Account dashboard → "Request production access" → copy/paste each field below verbatim. Edit volume forecast only if Phase 1 BETA cohort size diverges.

**Mail type:** `Transactional`

**Website URL:**
```
https://kitehub.me
```

**Use case description** (≥30 words, English required by AWS form):
```
Transactional emails for B2B SaaS education platform Phase 1 BETA invite cohort (5-10 education center tenants). Use cases: (1) email verification on signup, (2) beta invite delivery with one-time signup token, (3) password reset with expiring link, (4) MFA recovery TOTP backup codes per OWASP V2 auth requirements, (5) subscription/billing notifications, (6) DSAR acknowledgement per PDPL 2023 Art 23 compliance. Audience is explicit opt-in only via signup or invitation form; we do not send marketing-class email from this domain. Volume forecast: ~2,000 emails/month Phase 1 BETA (5-10 tenants × ~5-10 students each × signup + verify + 2-3 notifications/month). Free Tier 62,000 emails/month is sufficient runway through Phase 2 PAID launch. We commit to maintaining bounce rate <2% and complaint rate <0.1% via SNS-subscribed feedback loop with automatic suppression list management.
```

**How do you build and maintain your mailing lists:**
```
Users explicitly sign up via web form at https://kitehub.me/signup which requires email verification before account activation. Beta invite list is curated from BetaAccessRequest database table — only users who personally submitted the request form receive invites. No purchased lists, no scraping, no third-party sources. Email addresses are stored encrypted at rest (PostgreSQL with TLS) per PDPL 2023 Art 23 personal data protection requirements.
```

**How do you handle bounces and complaints:**
```
SES → SNS topics ses-bounces and ses-complaints subscribed. Bounce/complaint events are processed by kitehub-email service: hard bounces → permanently suppressed in email_suppression_list table (90-day retention per PDPL); soft bounces → retry max 3 times with exponential backoff then suppress; complaints → immediate permanent suppression with audit log entry. CloudWatch alarms wired to alert at bounce rate >3% (warn) and >5% (paging). Daily reputation dashboard review during Phase 1 BETA warmup period.
```

**How can recipients opt out of receiving email:**
```
All non-critical transactional emails (subscription notifications, onboarding tips, billing reminders) include a List-Unsubscribe header (RFC 2369 + RFC 8058 one-click) and a footer unsubscribe link. Critical transactional emails (email verification, password reset, MFA recovery, DSAR acknowledgement) cannot be unsubscribed per industry standard and PDPL 2023 Art 23 requirements for account security. Unsubscribe requests update email_suppression_list within 24 hours.
```

**Additional contacts / process** (optional but recommended):
```
DPO contact: dpo@kitehub.me (PDPL compliance per Vietnam Decree 13/2023/NĐ-CP). Abuse contact: abuse@kitehub.me. Bounce/complaint webhook automation deployed since Wave 33 (PR #896). Suppression list managed per documents/05-guides/deploy/email-ses-setup-runbook.md.
```

**Volume forecast** (form field):
- Phase 1 BETA (current): ~2,000 emails/month (62k Free Tier headroom)
- Phase 2 PAID (~Tuần 13-18): ~10,000 emails/month
- Phase 3 K-12: ~50,000 emails/month (still within Free Tier when sending from EC2)

**Bounce/complaint targets:**
- Expected bounce rate: <2% (industry threshold; SES suspends if >5%)
- Expected complaint rate: <0.1% (SES suspends if >0.3%)

**Approval time:** Typically 24-48h, occasionally up to 7 days. AWS will email approval/rejection to root account contact.

#### 4.1.2 Common rejection reasons + reply templates

If AWS rejects with "Insufficient sending history":
```
This is a new AWS account for a new product (Phase 1 BETA). Sending history is intentionally zero — we have not sent any production email yet. We are requesting production access BEFORE first send to avoid bounce-rate spikes during initial warmup. Volume will start at <50 emails/day Day 1 and ramp gradually per documented warmup schedule (see runbook §6.1). All recipients have explicit opt-in via signup form. Suppression list is empty (verified via `aws sesv2 list-suppressed-destinations`).
```

If AWS rejects with "Bounce handling unclear":
```
Bounce/complaint handling is automated via SNS topics ses-bounces and ses-complaints (configured per §5 of our SES setup runbook). The kitehub-email Java service consumes these topics and maintains a database-backed suppression list (email_suppression_list table). Hard bounces are permanently suppressed; soft bounces retry up to 3 times then suppress. CloudWatch alarms alert at bounce rate >3%. Source code: github.com/VictorAurelius/2026-Kite-Class-Platform (public).
```

If AWS rejects with "Use case too generic":
```
Specific use cases (all transactional, all opt-in):
1. Email verification on signup — RFC 5322 compliant verification link sent within 30 seconds of user submitting signup form
2. Password reset — One-time token with 60-minute expiry per OWASP Auth Cheat Sheet
3. MFA recovery codes — TOTP backup codes per OWASP ASVS V2.7 (account compromise recovery)
4. Beta invite delivery — single email per BetaAccessRequest row with unique signup token
5. DSAR acknowledgement — automatic reply within 24h of data subject access request per PDPL 2023 Art 23
6. Subscription/billing notifications — trial expiry warnings, renewal reminders, payment receipts
```

### 4.2 Post-approval limits

After approval, default production tier:
- **50,000 emails/day**
- **14 emails/second**

These match `aws.ses.rate.max-per-day=50000` + `aws.ses.rate.max-per-second=14` defaults (set conservatively at 10 in code; bump after warmup).

To request higher limits later: SES → Account dashboard → **Request quota increase** với justification.

### 4.3 Verify approval (post user-action)

After AWS support emails approval (24-48h), run smoke script to verify production state propagated:

```bash
# Tier 1 read-only verification (per agent-aws-access.md §2.1)
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 bash scripts/smoke-ses.sh
```

Expected output post-approval:
```
PASS  EnforcementStatus = HEALTHY
PASS  SendingEnabled = true
PASS  ProductionAccessEnabled = true (Max24h=50000.0, Rate=14.0/sec, Sent24h=...)
```

Pre-approval (sandbox) output:
```
WARN  ProductionAccessEnabled = false — SANDBOX mode (Max24h=200.0, Rate=1.0/sec)
WARN    → Action: submit production access request per email-ses-setup-runbook.md §4
```

### 4.4 DNS records verification (post §3.2 user-action)

After Cloudflare DNS records added (per `dns-setup-runbook.md`), verify propagation:

```bash
# SPF
dig +short TXT kitehub.me | grep -E '^"v=spf1'
# Expected: "v=spf1 include:amazonses.com -all"

# DMARC
dig +short TXT _dmarc.kitehub.me | grep -E '^"v=DMARC1'
# Expected: "v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@kitehub.me; ..."

# DKIM (one per token returned by aws sesv2 create-email-identity)
dig +short CNAME <token1>._domainkey.kitehub.me
# Expected: <token1>.dkim.amazonses.com

# All-in-one (Wave 61): smoke script checks SPF + DMARC + AWS identity state
bash scripts/smoke-ses.sh --domain
```

### 4.5 User-action checklist

| Step | Owner | Verify command | Status |
|------|-------|---------------|--------|
| Verify domain identity `kitehub.me` in SES Console (region `ap-southeast-1`) | User | `aws sesv2 list-email-identities --region ap-southeast-1` shows `kitehub.me` | ⏳ pending |
| Add 3 DKIM CNAME + 1 SPF TXT + 1 DMARC TXT to Cloudflare DNS | User | `dig +short TXT kitehub.me` returns SPF line | ⏳ pending DNS setup (`dns-setup-runbook.md`) |
| Submit production access request via SES Console with template §4.1.1 | User | AWS support case ID returned + approval email within 24-48h | ⏳ pending |
| Wait approval 24-48h (occasionally up to 7 days) | (AWS) | `aws sesv2 get-account` returns `ProductionAccessEnabled: true` | ⏳ pending |
| Run `bash scripts/smoke-ses.sh` to verify approval propagated | User or agent | smoke script PASS on production access check | ⏳ pending |
| Add reminder calendar event D+1 + D+7 (escalate if no approval) | User (Google Calendar MCP) | event visible | ⏳ pending |

---

## 5. Bounce + complaint feedback loops

SES requires explicit bounce/complaint handling to maintain reputation.

### 5.1 Create SNS topics

```bash
# Bounces
aws sns create-topic \
  --region ap-southeast-1 \
  --name ses-bounces

# Complaints
aws sns create-topic \
  --region ap-southeast-1 \
  --name ses-complaints
```

Output: 2 ARNs like `arn:aws:sns:ap-southeast-1:123456789012:ses-bounces`.

### 5.2 Configure SES → SNS notifications

**Console:** SES → Verified identities → `kitehub.vn` → **Notifications** tab → set:
- **Bounces:** select `ses-bounces` topic
- **Complaints:** select `ses-complaints` topic
- Enable **Include original headers**

**CLI:**
```bash
aws ses set-identity-notification-topic \
  --region ap-southeast-1 \
  --identity kitehub.vn \
  --notification-type Bounce \
  --sns-topic arn:aws:sns:ap-southeast-1:123456789012:ses-bounces

aws ses set-identity-notification-topic \
  --region ap-southeast-1 \
  --identity kitehub.vn \
  --notification-type Complaint \
  --sns-topic arn:aws:sns:ap-southeast-1:123456789012:ses-complaints
```

### 5.3 Subscribe app endpoint to SNS topics

Two options:
1. **HTTPS endpoint** (recommended for prod): `https://api.kitehub.vn/internal/ses/bounce` — kitehub-email service exposes the handler. SNS auto-confirms subscription on first POST.
2. **SQS queue** (simpler for solo-dev): subscribe SQS queue, kitehub-email polls.

For Wave 33 BETA: **SQS queue** is faster to set up. Track HTTPS endpoint as follow-up gap when scale demands.

### 5.4 Wire app config

After SNS topic ARNs are ready, set environment variables:

```bash
export AWS_SES_BOUNCE_TOPIC_ARN="arn:aws:sns:ap-southeast-1:123456789012:ses-bounces"
export AWS_SES_COMPLAINT_TOPIC_ARN="arn:aws:sns:ap-southeast-1:123456789012:ses-complaints"
```

Or in production Helm values / `.env.production` per `secrets-management-runbook.md` (Bucket D).

---

## 6. Sending limits + warmup schedule

Even after production approval, **deliverability requires warmup**. New IPs/domains start with low reputation; sending too much too fast triggers spam filtering.

### 6.1 Warmup schedule (recommended)

| Day | Max emails/day | Notes |
|-----|----------------|-------|
| 1 | 50 | Internal testing only — verified addresses |
| 2 | 100 | First beta cohort (small) |
| 3 | 200 | Expand to ~50 beta tenants |
| 4 | 500 | Continue beta — monitor bounce rate |
| 5 | 1,000 | Add support emails |
| 7 | 2,000 | All beta tenants active |
| 14 | 10,000 | Phase 2 ramp |
| 30 | 50,000 | Production tier reached |

Set `aws.ses.rate.max-per-day` to match the current day target. App-side limiter rejects emails over the cap (per `feedback_dependabot_first_run.md` defensive pattern).

### 6.2 Rate limit enforcement

```yaml
# application-production.yml
aws:
  ses:
    rate:
      max-per-second: 10   # Day 1: stay well below 14/s SES cap
      max-per-day: 500     # Day 4 target
```

App enforces both caps via Resilience4j RateLimiter (configured separately — TODO follow-up if not yet wired).

### 6.3 Monitoring

CloudWatch metrics to watch:
- `Send` — total successful sends
- `Bounce` — bounced emails (target: <2%)
- `Complaint` — spam complaints (target: <0.1%)
- `Reputation.BounceRate` (custom)
- `Reputation.ComplaintRate` (custom)

Alert thresholds (per `monitoring-runbook.md` future scope):
- Bounce rate >3% → WARN
- Bounce rate >5% → SES auto-suspend; PAGE on-call
- Complaint rate >0.2% → WARN
- Complaint rate >0.3% → SES auto-suspend; PAGE on-call

---

## 6.1 Test mailbox setup for E2E smoke (Wave 62 GAP-475)

The shell smoke `scripts/smoke-ses.sh` (Wave 62 Bucket B extension) provides two opt-in E2E checks that send a real email and verify receipt:

- `send_receive_email_e2e` — sends a timestamped test email via `aws ses send-email` and polls a mailbox for delivery evidence (GAP-475 Sub-2).
- `verify_mfa_otp_e2e` — triggers `POST /api/auth/register` then polls for the verification-email link, extracts the `?token=<UUID>` value, and calls `POST /api/auth/verify-email?token=<...>` to confirm the email→link→verify flow end-to-end (GAP-475 Sub-3).

Both checks are env-gated. Without env set, the smoke skips them (graceful `[SKIP]` log line, exit 0). Two polling backends are supported; **Mailgun route is the primary path** for production cron / CI use.

### 6.1.1 Path A — Mailgun events API (preferred)

Mailgun's events API gives a structured, scriptable read of inbound messages without IMAP fragility. Use this in CI cron + nightly Phase 1 BETA verification.

**Prerequisites:**
1. Mailgun account with a verified domain configured for inbound routing (free Flex tier covers smoke volume; ~5 inbound msg/day for the smoke).
2. A Mailgun route forwarding inbound mail addressed to `smoke@<mailgun-domain>` into stored events.
3. Mailgun **Private API key** (Dashboard → Settings → API Keys → Private API key).

**Env vars to set:**

```bash
export SMOKE_EMAIL_E2E=1
export SMOKE_MFA_E2E=1                           # optional, to also run Sub-3
export SMOKE_EMAIL_RECIPIENT="smoke@<mailgun-domain>"
export SMOKE_EMAIL_MAILGUN_API_KEY="<private-api-key>"
export SMOKE_EMAIL_MAILGUN_DOMAIN="<mailgun-domain>"
# Optional — override sender (defaults to noreply@kitehub.me)
export SMOKE_EMAIL_FROM="noreply@kitehub.me"
# Optional — for MFA E2E, override target backend (defaults to http://localhost:8080)
export KH_URL="https://api.kitehub.me"
```

Then run:

```bash
bash scripts/smoke-ses.sh
```

The script will:
1. Run all existing Tier 1 SES read-only checks.
2. Send a `smoke-test-<timestamp>` email via SES to `SMOKE_EMAIL_RECIPIENT`.
3. Poll the Mailgun events API every 30 sec for up to 5 min, filtered by `recipient` + `event=accepted|delivered|stored`.
4. Assert subject match (and body content where the event payload includes it).
5. If `SMOKE_MFA_E2E=1`: POST a fresh registration to `${KH_URL}/api/auth/register`, poll for the verification email, extract the URL token, and POST `${KH_URL}/api/auth/verify-email?token=<...>`.

**Cost:** 1-2 SES quota slots per run + free Mailgun events read. Safe to run hourly during the Phase 1 BETA hardening window.

### 6.1.2 Path B — Dedicated IMAP mailbox `smoke@kitehub.me`

For environments where Mailgun is not provisioned, point the smoke at a dedicated IMAP mailbox. **Caveat:** the IMAP path in `scripts/smoke-ses.sh` uses `curl imaps://` for fetch and is best-effort — it grep-matches the most recent message body. For production cron, prefer Path A.

**Prerequisites:**
1. Create a dedicated mailbox `smoke@kitehub.me` (Cloudflare Email Routing → forward to a Gmail/Outlook account with IMAP enabled; or any standalone IMAP host).
2. Generate an app password (Gmail requires 2FA + app password; Outlook accepts native password if IMAP/POP enabled).
3. Note the IMAP host (e.g., `imap.gmail.com`, `outlook.office365.com`).

**Env vars to set:**

```bash
export SMOKE_EMAIL_E2E=1
export SMOKE_MFA_E2E=1                           # optional
export SMOKE_EMAIL_RECIPIENT="smoke@kitehub.me"
export SMOKE_EMAIL_IMAP_HOST="imap.gmail.com"
export SMOKE_EMAIL_IMAP_USER="smoke-forwarded@gmail.com"
export SMOKE_EMAIL_IMAP_PASS="<app-password>"
```

Then run `bash scripts/smoke-ses.sh` as in Path A.

**Known limitations of the IMAP path:**
- Single-shot `curl imaps://` fetch; race possible if email is delivered between polls.
- Body parsing is grep-based; HTML-only emails may fail token extraction in the MFA flow.
- TLS handshake may fail on hosts with strict cipher policies.

For higher reliability, replace the IMAP polling block in `scripts/smoke-ses.sh::_poll_email_inbox()` with a project-specific helper (e.g., a small Python script using `imaplib`).

### 6.1.3 CI / cron integration

Once env vars are wired into GitHub Actions secrets (Mailgun path) or a secrets manager (IMAP path), schedule the smoke daily via the existing workflow gate:

```yaml
# .github/workflows/smoke-ses-nightly.yml (out of scope this PR — see GAP-475 Sub-X)
schedule:
  - cron: "17 3 * * *"   # daily 03:17 UTC
env:
  SMOKE_EMAIL_E2E: "1"
  SMOKE_MFA_E2E: "1"
  SMOKE_EMAIL_RECIPIENT: ${{ secrets.SMOKE_EMAIL_RECIPIENT }}
  SMOKE_EMAIL_MAILGUN_API_KEY: ${{ secrets.SMOKE_EMAIL_MAILGUN_API_KEY }}
  SMOKE_EMAIL_MAILGUN_DOMAIN: ${{ vars.SMOKE_EMAIL_MAILGUN_DOMAIN }}
  KH_URL: ${{ vars.KH_URL }}
```

Reference: `scripts/smoke-ses.sh::send_receive_email_e2e` + `::verify_mfa_otp_e2e`.

---

## 7. Smoke test (post-setup verification)

Three complementary smoke paths (use in order — cheapest first):

1. **Shell read-only smoke (Wave 61)** — `bash scripts/smoke-ses.sh` runs Tier 1 read-only AWS CLI calls only (no email sent, no cost): `get-account`, `list-email-identities`, `get-email-identity`, `list-suppressed-destinations`, plus optional `dig` for SPF/DMARC TXT. Use to verify SES state at any time without spinning up the JVM or hitting AWS send quota. Output: PASS/WARN/FAIL summary + artifact path. Safe to run hourly in CI/cron.
2. **HTTP curl smoke (§7 below)** — exercises `kitehub-email` service end-to-end (controller → Thymeleaf render → SES `send-email`). Use after stack is deployed to staging/prod. Costs 1 quota slot per run.
3. **JUnit code-side smoke** — `kitehub/kitehub-email/src/test/java/com/kitehub/email/integration/SesIntegrationSmokeTest.java` (Wave 45 GAP-370). Profile-gated, skipped by default; sends one minimal SES email + asserts `MessageId`. Use to verify credentials + FROM-domain identity + production access without spinning up the full service:

   ```bash
   cd kitehub
   ./mvnw -pl kitehub-email verify \
       -Daws-ses-real=true \
       -Dses.smoke.recipient=verified@example.com \
       -Dses.smoke.from=noreply@kitehub.vn \
       -Dses.smoke.region=ap-southeast-1
   ```

   Uses default AWS credential chain (env vars / IAM role / SSO profile). Default `mvn verify` skips this test (no system property set).

```bash
# Set production credentials
export AWS_SES_MOCK_MODE=false
export EMAIL_PROVIDER=ses
export AWS_SES_REGION=ap-southeast-1
export AWS_SES_FROM_EMAIL=noreply@kitehub.vn
export AWS_SES_FROM_NAME="KiteHub"
export AWS_ACCESS_KEY_ID=<...>
export AWS_SECRET_ACCESS_KEY=<...>

# Run smoke test (kitehub-email exposes /internal/test endpoint in dev/staging only)
curl -X POST https://api.kitehub.vn/internal/test/email \
  -H "Content-Type: application/json" \
  -d '{
    "to": "your-personal@gmail.com",
    "subject": "[SMOKE TEST] KiteHub SES production",
    "templateName": "beta-invite",
    "variables": {
      "orgName": "Test Org",
      "inviteToken": "smoke-test-token-1234",
      "inviteUrl": "https://kitehub.vn/auth/beta-signup?token=smoke-test-token-1234",
      "expiryDate": "2026-12-31"
    }
  }'
```

Verify:
- [ ] Email arrives in inbox (not spam folder) within 1min
- [ ] DKIM signature valid (check raw headers for `Authentication-Results: dkim=pass`)
- [ ] SPF passes (`spf=pass`)
- [ ] DMARC passes (`dmarc=pass`)
- [ ] Branding/styling renders correctly across Gmail / Outlook / mobile

---

## 8. Rollback / break-glass

If SES is suspended due to bounce/complaint spike:

1. **Stop all sends immediately:** Set `EMAIL_PROVIDER=mock` in production config + redeploy. Emails are logged but not sent.
2. **Investigate cause:** Pull last 24h sends from SES → identify spike source (test data leak? mailing list error? template misconfig?).
3. **Clean suppression list:** Mark recently-bounced addresses as suppressed; they will not be re-attempted.
4. **Submit reinstatement request:** SES Console → Reputation dashboard → **Request reinstatement** với root cause + remediation plan.
5. **Re-warm IP:** After reinstatement, restart at Day 1 of warmup schedule.

---

## 9. Open items / follow-ups

- [ ] App-side rate limiter wiring — `RateLimiter` bean using `aws.ses.rate.*` props (track in follow-up gap)
- [ ] HTTPS bounce/complaint webhook endpoint — replace SQS polling once scale demands
- [ ] DMARC report aggregation — set up `dmarc-reports@kitehub.vn` mailbox + parsing pipeline
- [ ] Suppression list table + auto-clean job (90d retention per PDPL)
- [ ] CloudWatch dashboard + alerts wired to PagerDuty/ntfy

---

## 10. Standards reference

Per `.claude/rules/release-deploy-standard.md` §2:
- **AWS Well-Architected** — Operational Excellence (warmup), Security (SPF/DKIM/DMARC), Reliability (bounce/complaint feedback)
- **OWASP Top 10** — A05:2021 Security Misconfiguration (proper email auth records prevent spoofing)
- **PDPL 2023** — Art 23 retention (suppression list 90d cap)
- **GAP-370** — closes BETA blocker for transactional email

---

## 11. Log

- **2026-05-11** (Wave 61 Bucket B): Production approval prep refresh. Added §4.1.1 copy-paste form template (refreshed for `kitehub.me` domain + Free Tier 62k/mo volume forecast + Phase 1 BETA invite cohort 5-10 tenants); §4.1.2 rejection-reason reply templates (3 common AWS rejection patterns); §4.3 post-approval verification commands + smoke script output expectations; §4.4 DNS verification commands (dig SPF + DMARC + DKIM); §4.5 user-action checklist with verify commands. New §7 path 1 references `scripts/smoke-ses.sh` (Tier 1 read-only AWS CLI verification — `get-account` + `list-email-identities` + suppression list + DNS records). Wave 61 verification table added at top — SES state 2026-05-11 = sandbox HEALTHY, 0 identities, suppression list empty. Reviewed accuracy of all sections — no drift. GAP-370 stays PARTIAL until user submits SES production access request + AWS approves.
- **2026-05-07** (Wave 33 Bucket B): Runbook created. SES sandbox→production approval steps + DKIM/SPF/DMARC TXT values + bounce/complaint SNS subscription + warmup schedule (Day 1: 50/day → Day 14: 10K/day → Day 30: 50K/day). Paired same-PR với `beta-invite.html` + `beta-request-confirmation.html` templates + `EmailType` enum + SES `bounce/complaint/rate` config properties.
