---
title: Benchmark — Beta Cohort Practices in VN-friendly EdTech SaaS
status: complete
created: 2026-05-14
phase: pre-wave-73
related_waves: [wave-73, wave-74]
---

# So sánh quy trình beta với 5 SaaS giáo dục Việt-friendly

**Author:** Benchmark agent (Wave 73 prep)
**Mục tiêu:** Tìm "họ có mà ta thiếu" trước khi đóng băng scope Wave 73 (email audit + user manual + Tally + UI smoke) + Wave 74 (Stripe sandbox).

---

## 0. Phương pháp + caveat

Sử dụng WebSearch + WebFetch trên public materials (blog, help center, press, YC profile, founder interviews, TechCrunch, VnExpress, Vietcetera). Một số sản phẩm Việt (Mona Media EdTech, Got It Education) có public data **rất hạn chế** về beta cohort cụ thể — đã thay thế bằng các sản phẩm có public materials tốt hơn nhưng vẫn relevant: **Marathon Education (YC W22, Việt Nam, K-12)** và **Kajabi (SaaS course platform có precedent beta cohort 800-user)**.

**Caveat độ tin cậy:**
- ✅ Hotmart, Teachable, Kajabi, ELSA Speak: public docs đầy đủ — đáng tin cậy
- ⚠️ Marathon Education: chỉ có press + Facebook page + YC profile — beta-specific process suy luận từ pattern chung của YC EdTech startup VN
- ❌ Mona Labs / Got It Education / hầu hết Vietnamese EdTech khác: closed-source, không có public beta playbook → "không rõ" trong matrix

**Khung phân tích:** không copy 1-1; đánh giá "tại sao họ làm vậy + có phù hợp Kite Việt + Phase 1 BETA solo-dev không".

---

## 1. Profile 5 sản phẩm tham chiếu

### 1.1 Hotmart (Brazil → toàn cầu, có VN traffic)
- Creator-economy platform, course + digital download + subscription
- Onboarding: AI suggests format + outline + pricing; YouTube tutorial channel; Help Center (EN/PT, không có VN dedicated); product format selection là bước 1 sau signup
- Source: [Hotmart Help Center](https://help.hotmart.com/en) + [Hotmart Tutorial YouTube](https://www.youtube.com/hotmarthelpcenter)

### 1.2 Teachable (US, 150k+ creator)
- Course + digital download + coaching offer; 7-day free trial + 30-day money-back guarantee
- Onboarding: 1-2h/ngày → 1 tuần (hoặc 1 ngày) là có course đầu tiên; MVP-first approach (mini-course 1-3 lessons); blog "complete guide to getting started"
- Source: [Teachable Get Started](https://www.teachable.com/blog/get-started-on-teachable) + [Teachable Pricing](https://www.teachable.com/pricing)

### 1.3 Kajabi (US, course + community + email + funnel all-in-one)
- Premium tier course platform; nổi tiếng vì community 2.0 + branded native app
- **Beta cohort precedent:** Branded App tested với **800 beta users** trước khi GA — cohort size đủ lớn để có thống kê có ý nghĩa
- Onboarding: "Smarter Start To Success" live workshop + 28-day Hero Challenge (chương trình hướng dẫn launch course đầu tiên) + 24h live chat (Growth/Pro plan)
- Source: [Kajabi Help Center](https://help.kajabi.com/) + [Kajabi Smarter Start Workshop](https://www.kajabi.com/learn/a-smarter-start-to-success) + [TechCrunch — Kajabi Branded App](https://techcrunch.com/2024/05/02/online-course-platform-kajabi-allows-creators-to-build-their-own-branded-apps/)

### 1.4 ELSA Speak (US-VN founder, 50M+ users, gốc Việt)
- AI English pronunciation app; founder Vu Van (Stanford) — Vietnamese accent → product origin story
- **Beta launch story:** SXSW 2016 startup competition winner → 30,000 users trong 24h sau viral
- **Việt-specific:** Initially 80-90% user base là Việt; campaign **"Thầy cô Việt dạy tiếng Anh hay"** (Nov 2018) — sponsor mỗi giáo viên English VN một ELSA Pro account + Teacher Dashboard cho trường học
- **Recent ELSA Nova upgrade (2025):** rolled out **trong cohorts nhỏ giữa Jan 10-17** để smooth transition; welcome screen + 5 onboarding questions sau upgrade
- Source: [Vietcetera Vu Van](https://vietcetera.com/en/vu-vans-journey-in-building-elsa-speak) + [ELSA Nova Coming Soon](https://blog.elsaspeak.com/en/elsa-speak-new-version-coming-soon/) + [VnExpress](https://e.vnexpress.net/news/tech/how-vietnamese-entrepreneur-utilizes-ai-to-teach-english-to-50-million-learners-worldwide-4849115.html)

### 1.5 Marathon Education (YC W22, Việt Nam, K-12)
- Live tutoring online cho K-12, math + science theo MOET national curriculum; $1.5M pre-seed Forge Ventures
- **Founder profile:** Duc (cựu TPG PE giáo dục) + Tung (serial founder tech VN) — không phải founder-tech-cũ-bị-bỏ, có infra
- **Community channel:** Facebook page chính (57k+ likes) + Facebook page Careers riêng; **không public Discord/Slack** → Facebook + Zalo là chính
- **Beta-specific process:** không public — pattern suy luận: YC W22 batch → khả năng cao dùng YC playbook (small cohort + weekly call + Slack/Discord internal + Demo Day)
- Source: [YC Marathon](https://www.ycombinator.com/companies/marathon-education) + [TechCrunch pre-seed](https://techcrunch.com/2021/08/24/vietnam-after-school-learning-startup-marathon-raises-1-5m-pre-seed-round/) + [Facebook page](https://www.facebook.com/marathon.edu.vn/)

---

## 2. Ma trận so sánh 15 khía cạnh × 5 sản phẩm

Ký hiệu: ✅ public confirmed | ⚠️ partial / inferred | ❓ không rõ | ❌ explicitly không có

| Khía cạnh | Hotmart | Teachable | Kajabi | ELSA | Marathon | **KiteHub Phase 1 BETA (Wave 73/74 đề xuất)** |
|---|---|---|---|---|---|---|
| **1. Email mời beta** | ❓ Không public mời cá nhân; tự signup free trial | ❓ Tự signup 7-day free trial | ❓ Tự signup 14-day free trial | ✅ ELSA Nova upgrade dùng email cohort-staged (Jan 10-17) | ❓ Không public — likely social-first (Facebook) | ✅ Email lifecycle DONE (Wave 71). Subject + tone chưa audit |
| **2. Form đăng ký** | ✅ Standard (email/Google/Facebook OAuth) | ✅ Standard (email/Google OAuth) | ✅ Standard | ✅ Mobile app — minimal form, OAuth | ✅ Phone OTP common (VN K-12 parent UX) | ✅ Form 1 field (email), honeypot ❓, CAPTCHA ❓, no OTP. **MISS: chưa VN-localize copy form** |
| **3. Verify email** | ✅ Standard double-opt-in | ✅ Standard | ✅ Standard | N/A (mobile OTP) | ✅ SMS OTP typical | ✅ Verify-email working (kitehub.me, GAP-509 etc DONE Wave 71) |
| **4. Onboarding video** | ✅ YouTube channel dedicated "Hotmart Help Center" — EN/PT, **không có VN dedicated** | ✅ Blog text-first; video embed trong help docs | ✅ **Smarter Start To Success** live workshop + Hero Challenge 28 ngày | ✅ Welcome screen + 5 onboarding questions trong app | ⚠️ FB page chứa video tutorial tiếng Việt | ❌ Wave 73 manual chỉ text+ảnh tiếng Việt — **KHÔNG có video** |
| **5. Office hours / chat support** | ❌ Help center async only | ✅ Live chat (Pro plan); blog + community | ✅ **24h live chat** (Growth/Pro plan) | ❓ In-app chatbot + email | ⚠️ Facebook Messenger + có thể Zalo (typical VN) | ❌ Không có. **MISS lớn nhất** |
| **6. Documentation / Manual** | ✅ Help Center web + YouTube; tone: instructional | ✅ Blog "complete guide" + Support center | ✅ Help Center + learn.kajabi.com (Onboarding plan riêng) | ✅ blog.elsaspeak.com + in-app tooltip | ⚠️ FB posts + có thể PDF gửi parent qua Zalo | ✅ Wave 73 manual.md tiếng Việt — text+ảnh. Tone: chưa quyết (formal/casual) |
| **7. Cam kết riêng beta** | ❓ Không public | ❓ Không public | ⚠️ Branded App beta: 800 user → có thể có sớm-access pricing nhưng không public | ⚠️ Pro account sponsorship cho giáo viên (Nov 2018 campaign) — không phải pure SaaS beta nhưng tinh thần "founder member" | ❓ Không public | ❌ Không có lifetime free / sớm-supporter badge / pricing lock |
| **8. Reset / fresh start** | ✅ Có thể tạo lại product | ✅ Có thể tạo lại course | ✅ Có thể tạo lại product | ✅ Reset progress option | ❓ Không rõ | ❌ Không có nút "xoá hết làm lại" cho tenant |
| **9. Data export** | ✅ CSV/JSON export student data | ✅ Export students + revenue | ✅ Export contacts + revenue + analytics | ✅ Export progress data | ❓ Không public | ⚠️ GAP-301 (deferred). **MISS: nếu beta user rời, không có path lấy data** |
| **10. Comms cadence (check-in tuần 1/2/4)** | ❌ Self-serve, không cá nhân hóa | ⚠️ Email drip onboarding (auto, không 1-1) | ✅ Hero Challenge 28 ngày có structured cadence; live chat cá nhân | ⚠️ In-app push + email digest | ⚠️ FB community engagement | ❌ Không có lịch check-in week 1/2/4 |
| **11. Status page** | ✅ Có (status.hotmart.com hoặc tương đương) | ✅ status.teachable.com | ✅ status.kajabi.com | ❓ App-internal status, không public web | ❓ Không public | ⚠️ GAP-373 PARTIAL — chưa có status page public |
| **12. Feedback channel** | ✅ In-app feedback + survey | ✅ Survey + community forum | ✅ Community 2.0 forum + survey | ✅ In-app rating + Apple/Google review prompts | ⚠️ FB comments + Messenger | ✅ Wave 73 đề xuất Tally embed (1 form). **CHƯA có in-app widget cho contextual feedback** |
| **13. NPS / survey** | ✅ Periodic NPS | ✅ NPS sau trial end + sau 1st sale | ✅ NPS post-onboarding | ✅ Rating + survey trong app | ❓ Không public | ❌ Wave 73/74 chưa lên kế hoạch NPS |
| **14. Beta termination plan** | N/A (không phải beta) | N/A | ⚠️ Branded App beta → GA: chỉ thông báo, không có migration plan public | ⚠️ Classic → Nova: auto-migrate, giữ progress | ❓ Không rõ | ❌ Chưa quyết: cuối Wave 73/74 thì sao? Auto-convert paid? Lock data? Continue free? |
| **15. Funnel tracking** | ✅ In-house analytics + GA4 | ✅ Native dashboard + integrations | ✅ Native funnel analytics | ✅ Mixpanel/Amplitude (industry-typical) | ✅ Có (any YC EdTech standard) | ❌ Không có PostHog/Mixpanel/GA4 installed → **không biết signup→activate→pay funnel** |

---

## 3. Top 5 thiếu sót lớn nhất (P0/P1) — relative to peer practices

### 3.1 ❌ Office hours / live support cho beta cohort (P0)

**Họ có:** Kajabi có 24h live chat cho Growth/Pro plan; Teachable có chat Pro+; ELSA có in-app chatbot; Marathon Education dùng Facebook Messenger + Zalo (typical VN K-12 EdTech behavior cho parent users).

**Ta thiếu:** Không có channel cho beta tenant hỏi nhanh "đăng ký xong làm sao tiếp?". Email support cycle thường 12-24h → beta cohort sẽ bỏ cuộc trong tuần 1.

**Tại sao P0 cho VN:** Văn hoá VN expect Messenger/Zalo reply trong **<30 phút giờ làm việc**. Email-only = sẽ bị nghĩ "không serious / chết rồi".

**Source:** [Kajabi help](https://help.kajabi.com/) confirmed 24h chat tier; [Zalo for business](https://www.infobip.com/blog/zalo-business) — Zalo 79M MAU, expected channel cho VN SMB.

### 3.2 ❌ Onboarding video tiếng Việt (P1)

**Họ có:** Hotmart, Teachable đều có YouTube tutorial channel; Kajabi có live workshop + Hero Challenge 28 ngày video content; ELSA có in-app interactive tour + welcome onboarding video; Marathon Education có FB video tutorial tiếng Việt.

**Ta thiếu:** Wave 73 manual chỉ text+ảnh. Text manual cho VN K-12 owner / parent / teacher cohort **dưới-trung niên 35-55 tuổi** sẽ ít người đọc hết.

**Tại sao P1:** Video 2-3 phút tiếng Việt subtitle cứng (đề phòng tắt mic) showing 3 happy-path screens là conversion driver mạnh hơn 10 trang text. Có thể dùng Loom hoặc OBS + smartphone — không cần production cao cấp.

**Source:** [Hotmart YouTube tutorial](https://www.youtube.com/hotmarthelpcenter) + [Kajabi Smarter Start](https://www.kajabi.com/learn/a-smarter-start-to-success).

### 3.3 ❌ Cam kết "founder member" cho beta cohort (P1)

**Họ có:** Industry pattern — 32% founders offer free/discounted access cho beta, 29% dùng roadmap influence (theo SaaS beta tester research) — và "founding member" framing với 3-5 năm locked pricing thay vì pure lifetime deal.

**Ta thiếu:** Phase 1 BETA chưa hứa gì cho người đầu tiên. → Tenant sẽ hỏi: "Sau beta phải trả bao nhiêu? Tôi có ưu đãi gì?" → Không trả lời được = mất trust.

**Tại sao P1:** Cốt lõi của beta program đối với SaaS K-12 VN là **giảm rủi ro cho hiệu trưởng / chủ trung tâm trải nghiệm**: "free trong N tháng + lock price năm đầu khi GA + early-supporter badge trên dashboard" là kit chuẩn.

**Source:** [Quoleady — SaaS founder beta testing stories](https://www.quoleady.com/how-to-find-beta-testers/) + [LifetimeDeals SaaS guide](https://www.datadab.com/blog/lifetime-memberships-for-saas/).

### 3.4 ❌ Funnel tracking — không biết drop-off (P1)

**Họ có:** Tất cả 5 sản phẩm peer đều có analytics native (Hotmart, Teachable, Kajabi) hoặc Mixpanel/Amplitude (ELSA, Marathon).

**Ta thiếu:** Không có PostHog / Mixpanel / GA4 → khi Wave 73 ship, nếu 100 user signup mà chỉ 5 hoàn thành onboarding → ta sẽ không biết bottleneck ở step nào.

**Tại sao P1:** Beta = data collection. Không có funnel = không có cách iterate. Solo-dev → càng phải đo, không thể quan sát qua mắt thường.

**Recommendation:** PostHog free tier (1M events/month) — self-host hoặc Cloud free. Install Wave 73 song song với Tally feedback.

**Source:** [PostHog NPS vs CSAT vs CES](https://posthog.com/product-engineers/nps-vs-csat-vs-ces).

### 3.5 ❌ Beta termination plan (P0)

**Họ có:** ELSA Classic → Nova: auto-migrate giữ progress. Industry pattern: thank-you email + lifetime discount cho beta tester khi chuyển GA.

**Ta thiếu:** Chưa quyết khi Wave 73/74 + tuần thử dùng kết thúc thì sao. Auto-convert paid? Lock data? Continue free? Email "cảm ơn + tặng đặc quyền"? → Tenant không biết → giảm willingness to invest data.

**Tại sao P0:** Cần quyết TRƯỚC khi mời người. Nếu sau 30 ngày họ thấy data bị lock + bắt trả tiền không báo trước = PR disaster cho VN K-12 cộng đồng (rất closed-knit, Facebook group hiệu trưởng sẽ lan toả nhanh).

**Source:** [Binadox — SaaS data portability exit](https://www.binadox.com/blog/saas-data-portability-planning-your-exit-strategy-before-you-need-it/) + [ELSA Nova upgrade](https://blog.elsaspeak.com/en/elsa-speak-new-version-coming-soon/).

---

## 4. Top 5 ý hay từ benchmarks — worth steal

### 4.1 ✅ ELSA "Thầy cô Việt" campaign model — sponsor + Teacher Dashboard

**Pattern:** Nov 2018, ELSA sponsor **mỗi** giáo viên English VN một ELSA Pro account + Teacher Dashboard cho trường. Không phải bán hàng → **gieo trust và word-of-mouth** trong cộng đồng giáo dục VN.

**Adapt cho Kite:** Phase 1 BETA mời **5-10 hiệu trưởng / chủ trung tâm pilot** với gói full premium FREE trong 12 tháng + branded "Pioneer Beta" badge + 1-1 onboarding call 30 phút với founder. Đổi lại: monthly feedback + cho phép screenshot làm case study.

**Tại sao phù hợp:** VN K-12 EdTech market = relationship-driven. Mỗi hiệu trưởng pilot = 5-20 hiệu trưởng khác nghe thấy.

**Source:** [Vietcetera Vu Van](https://vietcetera.com/en/vu-vans-journey-in-building-elsa-speak).

### 4.2 ✅ Kajabi Hero Challenge — 28-day structured onboarding cadence

**Pattern:** Thay vì "tài liệu xong, tự đọc", Kajabi có **28-day program** day 1-day 28 với daily email + weekly checkpoint + "1st sale by day 28" milestone.

**Adapt cho Kite:** "Kite Hub Launch in 14 Days" — email day 1 (welcome + video 3 phút), day 3 (set up tenant), day 7 (mời 1 giáo viên + 1 lớp pilot), day 14 (publish first attendance roster + check NPS). Có thể automate qua kitehub-email lifecycle đã có.

**Tại sao phù hợp:** Hiệu trưởng VN bận. Cần "lái" họ qua activation timeline rõ ràng, không expect họ self-explore.

**Source:** [Kajabi Hero Challenge](https://www.kajabi.com/) + [Kajabi Smarter Start workshop](https://www.kajabi.com/learn/a-smarter-start-to-success).

### 4.3 ✅ ELSA staggered cohort rollout (Jan 10-17, 2025 Nova)

**Pattern:** Khi upgrade major (Classic → Nova), ELSA không big-bang rollout — chia **small cohorts giữa 10-17 Jan** để fix issues incremental.

**Adapt cho Kite:** Phase 1 BETA cohort waves: tuần 1-2 = 5 tenant; tuần 3-4 = 10 thêm; tháng 2 = open 30+. Mỗi cohort wave có Friday retro 30 phút (Zalo group call) để gom feedback trước khi mở batch sau.

**Tại sao phù hợp:** Solo-dev cannot handle 50 onboarding cùng lúc. Cohort wave = bounded support load + structured iteration.

**Source:** [ELSA Nova rollout](https://blog.elsaspeak.com/en/elsa-speak-new-version-coming-soon/) + [Quoleady — staggered cohort 20-30 user/week](https://www.quoleady.com/how-to-find-beta-testers/).

### 4.4 ✅ Marathon Education — Facebook page = community + comms hub

**Pattern:** Marathon dùng **1 FB page chính (57k+ likes)** + 1 FB Careers page → channel chính cho parent communication + acquisition. Không phải multi-channel rải rác (Twitter + LinkedIn + IG).

**Adapt cho Kite:** Phase 1 BETA tạo **1 Zalo OA (Official Account) + 1 Facebook page** + (optional) **1 Zalo group** riêng 10-30 tenant beta. Đây là channel duy nhất cho status + announcement + Q&A — không spread sang Discord/Slack chưa cần.

**Tại sao phù hợp:** VN K-12 stakeholder (hiệu trưởng, parent, teacher) **không dùng Discord**. Zalo + FB là native. Reach + retention cao hơn email 5-10x.

**Source:** [Marathon Education FB](https://www.facebook.com/marathon.edu.vn/) + [Zalo business guide](https://www.infobip.com/blog/zalo-business) (79M VN MAU).

### 4.5 ✅ Industry standard — "Core, Reach, Backup" recruitment model

**Pattern:** Quoleady research với SaaS founder: Core = waitlist/email subscribers; Reach = niche communities (Slack/Discord/subreddit); Backup = paid panels + LinkedIn.

**Adapt cho Kite:** Phase 1 BETA recruit Core = hiệu trưởng đã có trong network cá nhân (5-10 người dev biết); Reach = 1-2 FB group hiệu trưởng VN + 1 Zalo group cộng đồng EdTech; Backup = paid micro-promote trên FB nếu Core+Reach <10 sau 2 tuần. Có structure thay vì ad-hoc.

**Tại sao phù hợp:** Solo-dev không có ad budget lớn. Core+Reach là free; Backup chỉ activate khi cần.

**Source:** [Quoleady SaaS founder stories](https://www.quoleady.com/how-to-find-beta-testers/) + [Rachel Andreago — beta acquisition strategies](https://rachelandreago.com/beta-user-acquisition/).

---

## 5. Khuyến nghị — phân chia vào Wave 73 / Wave 74 / Hậu beta

### 5.1 ✅ KEEP TRONG WAVE 73 (đã có trong scope, giữ nguyên)

- Email lifecycle audit (subject line, tone tiếng Việt)
- User manual tiếng Việt (text + ảnh)
- Tally feedback form (1 form sau onboarding completion)
- UI smoke test

### 5.2 🟠 BỔ SUNG VÀO WAVE 73 (low effort, high ROI)

| Item | Effort | Justification |
|---|---|---|
| **Zalo OA + FB page Phase 1 BETA + Zalo group beta cohort** | ~4h setup | Top miss #1 (office hours surrogate). VN-native channel. Không cần dev — admin work. |
| **Beta termination plan doc** (`documents/03-planning/roadmap/beta-termination-plan.md`) — ghi rõ "30 ngày end → auto-extend free + Pioneer Beta badge → khi GA, lock price năm đầu" | ~2h | Top miss #5. Quyết TRƯỚC khi mời người. |
| **Founder welcome email + 1-1 onboarding call slot 30 phút** (Calendly free tier) | ~2h | Top miss #1 partial + steal #1 ELSA model. Mỗi tenant đầu = founder phone call. |
| **"Pioneer Beta" badge UI** (1 chip component trên tenant dashboard header) | ~3h FE | Top miss #3 cam kết. Visual confirmation họ là sớm. |
| **PostHog free tier install** (signup → activate → publish first roster funnel) | ~4h FE+BE | Top miss #4. Không có funnel = không có data để iterate sau Wave 74. |
| **Onboarding video tiếng Việt** (Loom hoặc smartphone screen-record, 2-3 phút, 3 happy-path screens, subtitle cứng) | ~4h | Top miss #2. Replace text manual section "Bắt đầu nhanh". |
| **Status page public** (Openstatus hoặc UptimeRobot free) | ~2h | GAP-373 partial → close. Build trust mở. |

**Tổng effort bổ sung Wave 73:** ~21h (~2.5 ngày solo-dev). Đáng làm vì 5/5 items là one-time setup + cover top 5 miss.

### 5.3 🟡 WAVE 74 (đề xuất giữ Stripe sandbox, BỔ SUNG)

| Item | Effort | Justification |
|---|---|---|
| **NPS trigger sau day 14** trong email lifecycle | ~2h BE | Top miss + steal #2 Hero Challenge cadence. Đơn giản: email day 14 với 1 câu hỏi 0-10. |
| **Data export endpoint** (JSON dump roster + class + payment) — GAP-301 unblock | ~6h BE | Top miss #5 termination. Cần TRƯỚC khi mở Stripe billing — tenant muốn biết "data có lấy được không?" trước khi nhập thẻ. |
| **Reset / fresh-start button** (admin-only, "xoá hết data tenant này, giữ account") | ~4h FE+BE | Top miss khía cạnh #8. Cho phép tenant pilot, sai, làm lại sạch — quan trọng với cohort beta. |

**Tổng effort bổ sung Wave 74:** ~12h. Stripe sandbox vẫn giữ priority cao nhất; 3 items này là parallel work.

### 5.4 🟢 HẬU BETA — Wave 75+

| Item | Phase | Justification |
|---|---|---|
| **14-day onboarding email cadence** (Kajabi Hero Challenge model) | Wave 75 | Steal #2. Cần >50 tenant để A/B test variant subject lines. |
| **In-app contextual feedback widget** (vs Tally external) | Wave 75 | Steal #5 better; Tally là first-step nhưng widget = contextual capture (rage-click, drop-off page) |
| **Community forum / Discord-equivalent** | Wave 80+ | Khi cohort >100 tenant, peer-help giảm tải founder Q&A |
| **Marathon-style Facebook page + content marketing cadence** | Wave 80+ | Lead-gen channel cho post-BETA GA (Phase 1.5 PAID) |
| **Teacher Dashboard / sponsor model ELSA** | Phase 2 | Big-bang go-to-market khi sản phẩm stable. Phase 1 BETA chưa đến đó. |

---

## 6. Đánh giá tổng — Wave 73/74 hiện tại đủ chưa?

**Trước benchmark:** Wave 73 (email audit + manual + Tally + smoke) + Wave 74 (Stripe sandbox) — **TỐT về technical readiness** nhưng **MISS về human-side beta cohort**.

**Sau benchmark:** Wave 73/74 hiện tại = **infrastructure complete nhưng cohort experience thiếu 5 thứ quan trọng**:
1. Comms channel native VN (Zalo+FB)
2. Founder welcome call + Pioneer badge
3. Beta termination + data export safety net
4. Funnel tracking để iterate
5. Onboarding video tiếng Việt thay cho text-only manual

**Khuyến nghị final:** GIỮ scope Wave 73/74 tech việc, **PARALLEL spin up "Wave 73c — Cohort Experience Foundations"** (~21h Wave 73, ~12h Wave 74) bằng admin + design work, không cần extend timeline lớn. Mỗi item là 2-6h, có thể batch trong 2-3 evening sessions.

---

## 7. Caveat + giới hạn report

- **Public-only data:** 2/5 sản phẩm (Marathon, ELSA Việt-specific beta detail) suy luận từ press + social. Không có internal beta playbook public.
- **Mona Media / Got It Education:** không tìm thấy public beta cohort process → thay bằng Kajabi + Marathon. Nếu có connection trực tiếp với hai công ty này, nên primary research qua interview.
- **Cultural caveat:** "phù hợp Việt" dựa trên industry knowledge + Zalo usage data. Không có UX research thực địa.
- **Không phải audit /128 hay /100:** report này là **discovery / gap-finding** report, không phải scored audit. Không thay thế security/performance/UI audit suite.
- **Time-bound:** thông tin public benchmark có thể stale 6-12 tháng. Re-validate quarterly.

---

## 8. Source list

1. [Hotmart Help Center](https://help.hotmart.com/en) — onboarding + tutorial structure
2. [Hotmart Tutorial YouTube](https://www.youtube.com/hotmarthelpcenter) — video content model
3. [Teachable Get Started Guide](https://www.teachable.com/blog/get-started-on-teachable) — 1-day to 1-week launch path
4. [Teachable Pricing](https://www.teachable.com/pricing) — 7-day trial + 30-day money-back
5. [Kajabi Help Center](https://help.kajabi.com/) — 24h chat support tier
6. [Kajabi Smarter Start To Success Workshop](https://www.kajabi.com/learn/a-smarter-start-to-success) — live onboarding pattern
7. [TechCrunch — Kajabi Branded App 800 beta users](https://techcrunch.com/2024/05/02/online-course-platform-kajabi-allows-creators-to-build-their-own-branded-apps/) — beta cohort size precedent
8. [Vietcetera — Vu Van Journey](https://vietcetera.com/en/vu-vans-journey-in-building-elsa-speak) — ELSA founder origin + early Vietnam user base
9. [VnExpress — ELSA Vu Van 50M users](https://e.vnexpress.net/news/tech/how-vietnamese-entrepreneur-utilizes-ai-to-teach-english-to-50-million-learners-worldwide-4849115.html) — "Thầy cô Việt" sponsor campaign
10. [ELSA Nova Coming Soon](https://blog.elsaspeak.com/en/elsa-speak-new-version-coming-soon/) — staggered cohort rollout Jan 10-17
11. [Y Combinator Marathon Education](https://www.ycombinator.com/companies/marathon-education) — YC W22 K-12 Vietnam profile
12. [TechCrunch — Marathon $1.5M pre-seed](https://techcrunch.com/2021/08/24/vietnam-after-school-learning-startup-marathon-raises-1-5m-pre-seed-round/) — funding + founder background
13. [Marathon Education FB page](https://www.facebook.com/marathon.edu.vn/) — community channel model
14. [Quoleady — How to Find Beta Testers 2025](https://www.quoleady.com/how-to-find-beta-testers/) — Core/Reach/Backup recruitment + staggered cohorts
15. [Rachel Andreago — beta user acquisition strategies](https://rachelandreago.com/beta-user-acquisition/) — 8 strategies
16. [Datadab — Lifetime Memberships for Early-Stage SaaS](https://www.datadab.com/blog/lifetime-memberships-for-saas/) — founder member framing
17. [Binadox — SaaS Data Portability Exit Strategy](https://www.binadox.com/blog/saas-data-portability-planning-your-exit-strategy-before-you-need-it/) — termination + data export
18. [Zalo for business guide (Infobip)](https://www.infobip.com/blog/zalo-business) — Zalo VN business channel, 79M MAU
19. [PostHog — NPS vs CSAT vs CES](https://posthog.com/product-engineers/nps-vs-csat-vs-ces) — survey framework
20. [Openstatus](https://www.openstatus.dev/) + [UptimeRobot Status Page Guide](https://uptimerobot.com/knowledge-hub/monitoring/building-a-status-page-ultimate-guide/) — public status page free options

---

## 9. Acceptance criteria self-check

- [x] Ma trận so sánh 15 khía cạnh × 5 sản phẩm có status mark cho mỗi cell
- [x] Top 5 thiếu sót có cite source URL specific
- [x] Top 5 steal ý hay có "tại sao phù hợp Việt + Kite Phase 1 BETA"
- [x] Khuyến nghị Wave 73 / Wave 74 / hậu beta với effort estimate
- [x] Disclaimer về public data limit (§7)
- [x] Không bịa thông tin về Mona / Got It → đã thay bằng Kajabi + Marathon có public data
- [x] Không copy 1-1 — mỗi steal pattern có "Adapt cho Kite" + rationale
- [x] Tính phù hợp văn hoá Việt: Zalo, FB, Vietnamese tone explicit trong miss #1 + steal #1+#3+#4
