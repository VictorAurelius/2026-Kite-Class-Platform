# Acceptable Use Policy (AUP) — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — section structure + TODO markers; Phase 2 content + T&S team review via GAP-154)
**Owner:** Legal + Trust & Safety
**Reviewer:** T&S team + MOET alignment check + Legal counsel (Phase 2)
**Last-Updated:** 2026-04-29
**Tracking:** GAP-181 (Phase 1, Wave Legal-BRD 2026-04-29) → GAP-154 (Phase 2 content + moderation playbook in 05-guides/)
**Legal basis:** VN Cybersecurity Law 2018 (Law 24/2018/QH14), Criminal Code, MOET content circulars for education platforms
**Cross-cuts:** GAP-018 (technical content moderation), GAP-186 (Child Protection — stricter AUP for minors), GAP-042 (Legal/IP Protection)

---

## 1. Scope + Acceptance

Acceptable Use Policy (AUP) áp dụng cho **TẤT CẢ users** sử dụng nền tảng KiteHub (SaaS quản lý) và KiteClass (multi-tenant education platform), không phân biệt vai trò:

- **Tenant Admin** (chủ trường / education center owner) — chịu trách nhiệm về toàn bộ nội dung và hành vi trong tenant của mình
- **Teacher** (giáo viên) — chịu trách nhiệm cá nhân về nội dung giảng dạy, tài liệu upload, và tương tác với học sinh / phụ huynh
- **Parent** (phụ huynh) — chịu trách nhiệm khi đại diện cho con (đặc biệt minors dưới 16 tuổi)
- **Student** (học sinh) — chịu trách nhiệm cá nhân nếu đủ tuổi (từ 16 tuổi trở lên); nếu dưới 16, parent đồng chịu trách nhiệm
- **Platform Admin** (KiteHub super admin) — bound bởi internal policy + AUP này khi thực hiện moderation actions

Bằng việc tạo tài khoản hoặc sử dụng platform, user xác nhận đã đọc, hiểu, và đồng ý tuân thủ AUP. AUP có thể được cập nhật theo thời gian — material changes sẽ được thông báo qua email + in-app banner ít nhất 30 ngày trước khi effective.

AUP bổ sung cho (không thay thế) Terms of Service (TOS), Privacy Policy, và Data Processing Agreement (DPA). Khi có xung đột, TOS prevails về commercial terms; AUP prevails về content + behavior; Privacy Policy prevails về data handling.

<!-- Phase 2: T&S to refine acceptance flow — click-through vs scroll-through, age-gate cho minors, parent co-acceptance — GAP-154 -->

---

## 2. Prohibited Content

KiteHub/KiteClass nghiêm cấm các loại nội dung sau. Vi phạm sẽ bị remove ngay lập tức + có thể dẫn đến suspension/ban + báo cáo cơ quan chức năng (xem §8).

### 2.1 Illegal content (VN Cybersecurity Law + Criminal Code)

Nội dung vi phạm pháp luật Việt Nam, đặc biệt:
- **Luật An ninh mạng 2018** (Law 24/2018/QH14) Điều 8, 16, 17 — chống Nhà nước, xuyên tạc lịch sử, kích động bạo lực
- **Bộ luật Hình sự 2015 (sửa đổi 2017)** — các tội về truyền bá văn hóa phẩm đồi trụy, tổ chức sử dụng trái phép chất ma túy, đánh bạc
- **Luật Báo chí 2016** — phát tán thông tin chưa được kiểm chứng / thông tin xấu độc
- Nội dung cổ vũ ly khai, chia rẽ dân tộc, tôn giáo

### 2.2 CSAM (Child Sexual Abuse Material) — **ZERO TOLERANCE**

**Tuyệt đối cấm** mọi hình thức nội dung khai thác, lạm dụng tình dục trẻ em (dưới 18 tuổi):
- Tự động ban permanent ngay lập tức (không qua warning/strike system §5)
- Báo cáo bắt buộc cho **Cục An ninh mạng và Phòng chống tội phạm sử dụng công nghệ cao (A05) — Bộ Công an** trong vòng 24 giờ
- Phối hợp với **Cục Trẻ em — Bộ LĐTBXH** và các tổ chức quốc tế (NCMEC, INHOPE) khi cần
- Preserve evidence cho điều tra hình sự (không xóa hoàn toàn cho đến khi cơ quan chức năng cho phép)
- Cross-cut: **GAP-186 Child Protection** — quy định stricter AUP và moderation cho nội dung liên quan minors

### 2.3 Hate speech, harassment, threats

Nội dung kích động thù hận, phân biệt đối xử, đe dọa cá nhân hoặc nhóm dựa trên:
- Dân tộc, tôn giáo, vùng miền
- Giới tính, xu hướng tính dục, identity
- Khuyết tật, tình trạng sức khỏe
- Tình trạng kinh tế - xã hội

Bao gồm doxxing (công khai thông tin cá nhân để hạ nhục), cyberbullying, threats of violence (bao gồm cả threats với teachers / classmates).

### 2.4 Adult / pornographic content

Nội dung khiêu dâm, gợi dục dưới mọi hình thức không phù hợp với platform giáo dục:
- Hình ảnh / video khỏa thân, hoạt động tình dục
- Sexually suggestive content trong avatar, profile, chat, course materials
- Liên hệ inappropriate giữa adult users và minors

Lưu ý: nội dung giáo dục về sức khỏe sinh sản trong khuôn khổ chương trình MoET (Sinh học, Giáo dục Công dân) được phép — phải tuân thủ MOET content guidelines.

### 2.5 Copyright infringement (DMCA-equivalent)

Vi phạm bản quyền — upload, share, distribute nội dung không được phép:
- Sách giáo khoa scan/pirate (đặc biệt SGK MoET có bản quyền)
- Đề thi / lời giải có bản quyền của các trung tâm khác
- Phim, nhạc, phần mềm bị crack
- Materials từ paid courses của competitors

**Process khiếu nại bản quyền (DMCA-equivalent cho VN):**
- Email: `copyright@kitehub.me` <!-- Phase 2: T&S to provision actual mailbox -->
- Required: identification of copyrighted work + URL của infringing content + sworn statement + contact info
- Response SLA: <!-- Phase 2: T&S to set — informed gut, GAP-154 (likely 7-14 ngày theo Luật Sở hữu trí tuệ 2005 sửa đổi 2022) -->
- Counter-notice process: 14 ngày để user contest

### 2.6 Misinformation (đặc biệt health/political theo VN law)

- **Health misinformation** vi phạm **Luật Khám bệnh, Chữa bệnh 2009 + Nghị định 117/2020/NĐ-CP** — quảng cáo thuốc/dịch vụ y tế trái phép, lan truyền tin giả về dịch bệnh, bài thuốc chưa kiểm chứng
- **Political misinformation** vi phạm **Luật An ninh mạng 2018** Điều 8 — xuyên tạc đường lối chính sách, tin giả về cơ quan Nhà nước
- **Educational misinformation** — fake credentials, fake academic claims, false advertising về trường/giáo viên

### 2.7 Prohibited content matrix (skeleton — Phase 2 to finalize)

| Content type | Severity | Detection method | Platform response | Reporting authority | Appeal eligible? |
|--------------|:--------:|------------------|-------------------|---------------------|:----------------:|
| CSAM | 🔴 Critical | Auto (PhotoDNA + ML) + report | Permanent ban + content preserve + report A05/NCMEC <24h | A05, Cục Trẻ em, NCMEC | ❌ No |
| Illegal (Cybersec Law) | 🔴 Critical | Manual review + ML pre-screen | Remove + suspend pending review + báo A05 nếu nghiêm trọng | A05 | ⚠️ Limited |
| Hate speech / threats (severe) | 🔴 Critical | Report + manual review | Remove + suspend 7-30d / permanent ban tùy mức độ | A05 nếu threat hình sự | ✅ Yes |
| Adult / pornographic | 🔴 Critical | Auto (NSFW classifier) + report | Remove + 1st = strike, repeat = ban | — (trừ nếu liên quan minors → §2.2) | ✅ Yes |
| CSAM-adjacent (grooming behavior) | 🔴 Critical | Pattern detection + report | Remove + suspend + investigate + báo cơ quan | A05, Cục Trẻ em | ⚠️ Limited |
| Copyright infringement | 🟠 High | DMCA-style notice | Remove + counter-notice window 14d | — (civil) | ✅ Yes |
| Health misinformation | 🟠 High | Report + manual review | Remove + warn (1st) / strike (2nd) | Bộ Y tế nếu nghiêm trọng | ✅ Yes |
| Political misinformation | 🟠 High | Report + manual review | Remove + báo A05 nếu xuyên tạc | A05 | ⚠️ Limited |
| Spam / phishing | 🟡 Medium | Auto + report | Remove + rate-limit / suspend | — | ✅ Yes |
| Hate speech (mild / borderline) | 🟡 Medium | Report + manual review | Warn → strike → suspend | — | ✅ Yes |
| Other AUP violation | 🟢 Low | Report | Warn → strike | — | ✅ Yes |

<!-- Phase 2: T&S to refine severity tiers, detection methods, and SLA per row — GAP-154 + GAP-018 (technical detection) -->

---

## 3. Prohibited Conduct

Ngoài nội dung, các **hành vi** sau cũng bị nghiêm cấm trên platform.

### 3.1 Account sharing, credential abuse

- Chia sẻ tài khoản giữa nhiều người (1 tài khoản = 1 user duy nhất)
- Mua bán, cho thuê tài khoản (đặc biệt teacher accounts để "đi dạy hộ")
- Sử dụng tài khoản của người khác mà không được phép
- Multi-account abuse — tạo nhiều tài khoản để bypass rate limit / quota / strike system

### 3.2 Bot traffic, scraping, rate limit bypass

- Tự động hóa requests vượt quá rate limit định sẵn (xem AI Branding §2.5 input cap rules + per-tier rate limits)
- Web scraping nội dung của tenants khác hoặc của platform mà không có authorization
- Bypass rate limit qua proxy rotation, multiple accounts, headless browsers
- Replay attacks, request flooding

### 3.3 Reverse engineering

- Decompile, disassemble platform code (frontend bundles, mobile apps, backend APIs)
- Probe vulnerabilities mà không qua **Responsible Disclosure Program** (xem GAP-042)
- Tạo derivative works từ KiteHub/KiteClass code base
- Exception: **bug bounty / security research** đăng ký trước với `security@kitehub.me` <!-- Phase 2: T&S to set up program -->

### 3.4 Competitive intelligence gathering

- Tạo "fake tenant" để dò pricing, feature set, AI behavior
- Đăng ký trial chỉ để extract knowledge cho competitor product
- Hire teachers/owners trên platform để fetch internal data
- Industrial espionage — đặc biệt với AI Branding templates, knowledge base

### 3.5 Spam, phishing

- Gửi email/notification hàng loạt không liên quan đến giáo dục
- Phishing — giả mạo KiteHub/KiteClass để lừa user (qua email, in-app message, course content)
- Promotional spam (quảng cáo dịch vụ ngoài) trong course materials, chat với students/parents
- Pyramid schemes, MLM recruitment dưới hình thức "khóa học làm giàu"

---

## 4. Education-Specific Prohibitions

Vì KiteClass là platform **giáo dục**, có những hành vi đặc thù bị cấm vượt ngoài AUP chung:

### 4.1 Academic fraud

- **Plagiarism** — học sinh nộp bài copy từ nguồn khác mà không cite
- **Proxy test-taking** — nhờ người khác làm bài thi / quiz / homework
- **Selling answers, leaked exams** — bán/share đề thi + đáp án (đặc biệt đề thi MoET, đề thi đầu vào)
- **Grade manipulation** — teacher/student bribery để thay đổi điểm
- **Fake certificates** — tạo certificate giả mạo từ platform

### 4.2 Selling answers, leaked exams

Kinh doanh đáp án / đề thi rò rỉ là vi phạm nghiêm trọng:
- 1st violation: warning + content removal
- 2nd violation: 30-day suspension
- 3rd violation: permanent ban + báo MoET nếu liên quan đề thi quốc gia

### 4.3 Impersonating teachers

- Tạo tài khoản giả mạo giáo viên có thật (đặc biệt giáo viên nổi tiếng)
- Fake credentials (giả bằng cấp, học vị, chứng chỉ)
- Mạo danh trường học, sở giáo dục
- Hậu quả: permanent ban + có thể báo cơ quan chức năng nếu liên quan lừa đảo (Bộ luật Hình sự Điều 174)

### 4.4 Predatory behavior toward minors

**Đặc biệt nghiêm trọng** — bao gồm grooming, inappropriate contact, exploitation:
- Adult-to-minor private messaging ngoài context giảng dạy
- Yêu cầu personal info (địa chỉ nhà, số điện thoại cá nhân) từ minors
- Inappropriate gifts, financial transactions với students
- Sexual content / suggestion với minors (xem §2.2 — overlap với CSAM zero-tolerance)

**Cross-cut:** GAP-186 (Child Protection — stricter AUP for minors, Phase 2 deferred) sẽ định nghĩa chi tiết:
- Mandatory teacher background check
- Communication restrictions (chỉ qua platform-monitored channels)
- Parent visibility yêu cầu
- Mandatory reporting tới Cục Trẻ em khi phát hiện grooming

---

## 5. Enforcement

Khi phát hiện vi phạm AUP, platform áp dụng quy trình enforcement sau.

### 5.1 Warning system (3-strike model TODO)

Hệ thống tích lũy strike để track repeat offenders:

- **Strike 1 (Warning)** — email notice + in-app banner + content removed
- **Strike 2 (Soft suspension)** — temporary feature restriction (e.g., không post bài, không message students) <!-- Phase 2: T&S to set duration — informed gut, GAP-154 -->
- **Strike 3 (Hard suspension)** — full account suspension, escalate to Phase 5.2 review
- **Strike reset** — strikes expire sau <!-- Phase 2: T&S to set — informed gut, GAP-154 (likely 12 tháng) --> nếu không có violation mới

Nghiêm trọng (CSAM, illegal content level Critical) — **bypass strike system** → ban ngay lập tức.

### 5.2 Suspension tiers

| Strike # | Suspension type | Duration | Example violations | Appeal eligible? | Auto-restore? |
|:--------:|----------------|---------|---------------------|:----------------:|:-------------:|
| 1 | Warning only | N/A | Mild AUP violation (low-severity content, first-time spam) | ✅ Yes | Auto |
| 2 | Soft suspension | 24 hours | Repeat offense, pattern emerging | ✅ Yes | Auto after duration |
| 2.5 | Soft suspension | 7 days | Hate speech (mild), repeated copyright | ✅ Yes | Auto after duration |
| 3 | Hard suspension | 30 days | Multiple strikes, or single severe violation (e.g., harassment campaign) | ✅ Yes | Manual review required |
| 3+ | Permanent ban | Indefinite | Repeated 30-day suspensions, severe violations | ⚠️ Limited (one-time) | ❌ No |
| Critical | Permanent ban (immediate) | Indefinite | CSAM, illegal content, threats of violence | ❌ No (CSAM); ⚠️ Limited (others) | ❌ No |

<!-- Phase 2: T&S to finalize durations + matrix — informed gut, GAP-154 -->

### 5.3 Content removal process

- **Auto-detection** (CSAM hash match, NSFW classifier high-confidence): immediate remove + admin notify
- **User report** + manual review: triage SLA per §7
- **Self-service appeal**: user có thể request review trước khi permanent
- **Preservation policy** — illegal content được preserve (không xóa hoàn toàn) để phục vụ điều tra (xem §8)

### 5.4 Appeal process (15 days TODO)

User bị suspend có quyền kháng nghị:

| Step | Action | SLA | Decision authority |
|:----:|--------|-----|-------------------|
| 1 | User submits appeal qua `appeals@kitehub.me` hoặc in-app form | Phải submit trong vòng <!-- Phase 2: T&S to set — informed gut, GAP-154 (likely 15 ngày) --> kể từ khi suspension effective | — |
| 2 | T&S Tier 1 review (initial triage) | 3 business days | T&S Tier 1 reviewer |
| 3 | If escalated → T&S Tier 2 review (deeper investigation) | 7 business days | T&S Lead |
| 4 | If unresolved → Final review board (Legal + T&S Head) | 14 business days | Legal Counsel + T&S Head |
| 5 | Decision communicated qua email + in-app | Within 1 business day of decision | — |

**Appeal NOT eligible:** CSAM bans (per §2.2), threats with criminal investigation pending.

**Appeal flow skeleton:**

```
[User suspended] → [Submit appeal form within 15d]
                        ↓
                   [T&S Tier 1 triage — 3 days]
                        ↓
              ┌─────────┴─────────┐
              ↓                   ↓
        [Uphold suspension]    [Reverse]
              ↓                   ↓
       [User can escalate]    [Restore account]
              ↓
       [T&S Tier 2 — 7 days]
              ↓
       [Final decision OR escalate to Legal+T&S Head — 14 days]
              ↓
       [Final binding decision communicated]
```

<!-- Phase 2: T&S to formalize appeal form fields, SLAs, escalation triggers, and external arbitration option (consumer protection) — GAP-154 -->

---

## 6. Reporting (User Violation Reporting)

Platform phải cung cấp công cụ để users báo cáo vi phạm AUP.

### 6.1 Reporting channels

- **In-app report button** — trên mọi piece of content (post, message, course material, profile)
- **Email** — `report@kitehub.me` <!-- Phase 2: T&S to provision actual mailbox -->
- **Emergency hotline** (cho CSAM, threats) — <!-- Phase 2: T&S to set up dedicated channel + 24/7 coverage rotation, GAP-154 -->
- **Anonymous reporting** — supported cho whistleblower scenarios (ví dụ: học sinh báo teacher misconduct)

### 6.2 Report form fields

Khi user submit report, hệ thống thu thập:
- Loại vi phạm (dropdown: CSAM, hate speech, copyright, spam, etc.)
- URL / content ID
- Description (free text)
- Evidence (screenshots, links)
- Reporter contact (optional — anonymous OK trừ legal cases)

### 6.3 Report SLA per severity

Xem §7 (Platform Response Time) — SLA gắn với severity tier.

### 6.4 Anti-abuse of reporting

- Báo cáo sai sự thật / hàng loạt (mass false reporting) → bản thân là AUP violation
- Pattern: 5+ false reports trong 30 ngày → reporter bị strike
- Mass-coordinated reporting (brigading) → suspend reporter group + escalate

<!-- Phase 2: T&S to design reporter quality score + reputation system — GAP-154 -->

---

## 7. Platform Response Time (SLA per Severity)

Platform cam kết SLA xử lý báo cáo theo severity:

| Severity | Examples | Acknowledgment SLA | Triage SLA | Resolution SLA | Escalation if missed |
|:--------:|----------|-------------------|------------|----------------|---------------------|
| 🔴 P0 Critical | CSAM, imminent threats of violence, active harassment campaign | <!-- Phase 2: ~1 hour, GAP-154 --> 1h | <!-- 4h --> 4h | <!-- 24h --> 24h | T&S Head + Legal + báo cơ quan chức năng |
| 🔴 P1 High | Illegal content, severe hate speech, copyright takedown | <!-- 4h --> 4h | <!-- 24h --> 24h | <!-- 3 business days --> 3 ngày | T&S Lead |
| 🟠 P2 Medium | Spam, mild harassment, moderate AUP violations | <!-- 24h --> 24h | <!-- 3 business days --> 3 ngày | <!-- 7 business days --> 7 ngày | T&S Tier 2 |
| 🟡 P3 Low | Minor AUP violations, content disputes | <!-- 3 business days --> 3 ngày | <!-- 7 business days --> 7 ngày | <!-- 14 business days --> 14 ngày | T&S Tier 1 supervisor |

<!-- Phase 2: T&S team review + adjustment based on staffing capacity + benchmark vs industry (Discord T&S, YouTube Trust & Safety) — GAP-154 -->

**Coverage hours:**
- P0 Critical: 24/7 on-call rotation
- P1-P3: business hours (8h-22h VN time, 7 days/week)

**Communication:**
- Reporter receives status updates: acknowledged → triaged → resolved
- Subject (reported user) receives notice + evidence + appeal option (per §5.4)

---

## 8. Cooperation with Authorities

Platform cooperates với cơ quan chức năng VN trong khuôn khổ pháp luật.

### 8.1 When we disclose

| Trigger | Authority | Disclosure type | Legal basis |
|---------|-----------|-----------------|-------------|
| Court order / subpoena | Tòa án các cấp | Full disclosure as ordered | Bộ luật Tố tụng Hình sự, Tố tụng Dân sự |
| A05 (Cục An ninh mạng) request — cybersecurity case | A05 — Bộ Công an | Full disclosure if formal request | Luật An ninh mạng 2018 Điều 26 |
| CSAM detection | A05, Cục Trẻ em — Bộ LĐTBXH, NCMEC | Mandatory proactive disclosure within 24h | Luật An ninh mạng + Luật Trẻ em 2016 Điều 54 |
| MoET inquiry — education content compliance | Bộ Giáo dục và Đào tạo | Cooperate per education circulars | MOET regulations |
| Tax / financial investigation | Tổng cục Thuế, Cơ quan điều tra | Disclose financial records as ordered | Luật Quản lý Thuế 2019, Luật Phòng chống rửa tiền 2022 |
| Consumer protection complaint | Cục Cạnh tranh và Bảo vệ Người tiêu dùng | Provide records of dispute | Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 |

### 8.2 How we disclose

- **Formal request required** — chấp nhận written request từ authority có jurisdiction (court order, A05 official letterhead, etc.)
- **Verification step** — verify authenticity của request trước khi disclose (chống social engineering)
- **Minimum scope** — chỉ disclose data scope của request, không "fishing expedition"
- **User notification** — thông báo cho user bị disclose nếu pháp luật cho phép (một số trường hợp confidentiality required)
- **Audit log** — mọi disclosure được log (user ID, authority, scope, date, legal basis) per data retention policy

### 8.3 Legal hold trigger

Khi nhận được:
- Court order với preservation requirement
- A05 hình sự investigation notice
- Civil litigation subpoena

Platform kích hoạt **legal hold** — pause data deletion (override default retention period) cho specific data set:
- Indefinite hold cho đến khi case kết thúc + thêm 1 năm
- Audit log mọi data movement during hold
- Cross-cut: **GAP-042 Legal/IP Protection** — quy định chi tiết legal hold workflow

### 8.4 Mandatory proactive disclosure

Một số trường hợp phải báo cáo cơ quan chức năng **không cần request**:
- **CSAM** — báo A05 + Cục Trẻ em + NCMEC trong vòng 24h kể từ phát hiện
- **Active threats of violence** — báo Công an phường/quận có jurisdiction
- **Terrorism / national security** — báo A05 ngay lập tức
- **Trafficking / exploitation** — báo Cục Trẻ em (nếu liên quan minors) + Bộ Công an

### 8.5 Transparency report (Phase 2)

<!-- Phase 2: T&S + Legal to publish quarterly transparency report — số lượng + loại requests, response rate, types of violations actioned. Industry benchmark: Discord, Google, Meta — GAP-154 + GAP-042 -->

---

## 9. Cross-References + Related Gaps

- [GAP-018](../04-quality/gaps/GAP-018.md) — Technical content moderation pipeline (planned — AI detection + reporting UI; feeds AUP enforcement §5.3)
- [GAP-186](../04-quality/gaps/GAP-186-child-protection.md) — Child Protection (Phase 2 deferred — stricter AUP cho minors, expands §2.2 + §4.4)
- [GAP-042](../04-quality/gaps/GAP-042.md) — Legal/IP Protection (planned — DMCA-equivalent process, legal hold workflow, transparency report)
- [GAP-154](../04-quality/gaps/GAP-154.md) — Phase 2 content + T&S team review umbrella (this skeleton's parent)
- Sibling Phase 1 skeletons (Wave Legal-BRD 2026-04-29):
  - [GAP-180 Terms of Service](terms-of-service.md) (planned — see GAP-180)
  - [GAP-182 Privacy Policy](privacy-policy.md) (planned — see GAP-182)
  - [GAP-184 Cookie Policy](cookie-policy.md) (planned — see GAP-184)
- Wave plan: [wave-2026-04-29-legal-brd-phase1.md](../03-planning/waves/wave-2026-04-29-legal-brd-phase1.md)

---

## 10. Phase 2 TODO Summary

Phase 1 ships skeleton + section structure + TODO markers. Phase 2 (tracked in **GAP-154**) requires:

- [ ] **Legal counsel review** — full AUP review by qualified VN lawyer (priority: §2 prohibited content, §5 enforcement penalties, §8 authorities cooperation)
- [ ] **T&S team review** — operational feasibility check (SLA realism, staffing, escalation paths)
- [ ] **MOET alignment check** — verify §2.6 health/political misinformation + §4 education-specific prohibitions align với MoET content circulars cho education platforms
- [ ] **T&S playbook** — derive operational moderation playbook (separate doc in `documents/05-guides/trust-and-safety/`)
- [ ] **TODO placeholders to set** — strike count details, suspension durations, SLA hours, appeal window, transparency report cadence
- [ ] **Form templates** — DMCA notice form, Appeal form, Report form (UI mockups + i18n VN/EN)
- [ ] **Mailbox provisioning** — `copyright@`, `report@`, `appeals@`, `security@` mailboxes + on-call rotation
- [ ] **Content moderation tooling integration** — AUP rules → GAP-018 technical detection mapping
- [ ] **Anti-abuse reporter quality system** — design + implement (per §6.4)
- [ ] **Quarterly transparency report** — first publication target Q3 2026 (per §8.5)

---

## 11. Document Governance

- **Status:** 🔵 SKELETON — content placeholders, structure approved Phase 1
- **Phase 2 owner:** T&S team (TBD) + Legal counsel (TBD)
- **Review cadence (post-Phase 2):** Annual + event-driven (new VN regulation, major content incident, MOET circular update)
- **Material change notice:** 30 ngày in-app banner + email to all users
- **Versioning:** semver — MAJOR for substantive policy change, MINOR for new sections, PATCH for clarifications
- **Archive:** previous versions kept in `documents/07-archived/aup-versions/` per `.claude/rules/docs-folder-structure.md` archive policy
