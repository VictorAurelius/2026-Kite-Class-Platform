# Refund + Dispute Resolution Policy — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — section structure + eligibility matrix + escalation ladder; Phase 2 legal counsel + payment processor alignment via GAP-154)
**Owner:** Legal + Finance + Support Lead
**Reviewer:** Legal counsel (VN Consumer Protection Law expertise) + Finance + Payment processor liaison (Phase 2)
**Last-Updated:** 2026-04-29
**Tracking:** GAP-183 (Phase 1, Wave Legal-BRD Phase 1.5 2026-04-29) → GAP-154 (Phase 2 content + legal sign-off + payment processor alignment + contract templates)
**Legal basis:** **Law No. 19/2023/QH15** (VN Consumer Protection Law 2023) — Article 14 mandates clear refund terms; Commercial Law 2005 (commercial dispute resolution); VIAC arbitration rules; TAND civil procedure
**Cross-cuts:** [terms-of-service.md](terms-of-service.md) (GAP-180 — dispute clause references this doc), [billing-terms.md](billing-terms.md) (GAP-185 sibling — refund calculation basis), [data-retention-deletion-policy.md](data-retention-deletion-policy.md) (GAP-184 — dispute records retention), GAP-189 customer SLA (planned — service credits eligibility), GAP-108 (planned — payment/invoice config externalization)

---

## Mục đích tài liệu

Đây là **bộ khung (skeleton)** cho chính sách hoàn tiền và giải quyết tranh chấp giữa Provider (đơn vị vận hành KiteHub/KiteClass) và Customer (tenant). Phase 1 ship cấu trúc 8 sections + refund eligibility matrix + dispute escalation ladder + TODO markers cho nội dung pháp lý chi tiết. Phase 2 (legal counsel review + payment processor alignment + contract templates) tracked qua GAP-154 umbrella.

**Mục tiêu phase 1:**
- Unblock payment processor onboarding (VNPay/MoMo/Stripe yêu cầu refund policy công khai để xử lý chargeback)
- Cung cấp khung cho enterprise RFP responses (procurement bên đối tác yêu cầu refund + dispute terms)
- Đặt nền cho support team SOP (hiện trạng ad-hoc decisions)
- Thỏa mãn tiền đề **VN Consumer Protection Law 2023 Art 14** — mọi nhà cung cấp dịch vụ trực tuyến phải công bố điều khoản hoàn tiền rõ ràng

**KHÔNG phải mục tiêu phase 1:**
- Final legal text (chờ legal counsel chuyên về VN Consumer Protection Law)
- Refund workflow implementation (separate feature gap)
- Chargeback response automation (operational tooling deliverable)
- Contract template redesign (Phase 2 legal deliverable)

---

## 1. Refund Eligibility Matrix

Bảng dưới là **placeholder Phase 1**. Mọi giá trị TODO sẽ được Product Owner + legal counsel + Finance xác nhận trong Phase 2 theo `business-logic-review.md` 5-attribute pattern (Source / Rationale / Reviewer / Compliance check / Review cadence). Hiện trạng các giá trị tiers + scenarios là **informed gut Q3 2026** (xem GAP-154).

| Tier | Trial refund | Mid-cycle refund | Feature failure refund | Goodwill refund |
|------|:------------:|:----------------:|:----------------------:|:---------------:|
| **Free** | N/A (không có thanh toán) | N/A | N/A | N/A |
| **Pro** | Pro-rated theo ngày chưa sử dụng | Case-by-case (Support Lead approval) | Full refund nếu SLA breach (xem GAP-189) | Support discretion ≤30% giá trị tháng |
| **Premium** | Pro-rated theo ngày chưa sử dụng | Pro-rated theo phần kỳ chưa sử dụng | Full refund + service credit nếu downtime ≥4h | Up to 100% theo Customer Success approval |
| **Enterprise** | Per contract terms (negotiable) | Per contract terms | Per contract SLA + remediation clause | Per contract — escalate Customer Success Director |

<!-- Phase 2: legal counsel + Finance + Customer Success to confirm — informed gut Q3 2026, GAP-154 -->

**Anti-pattern:** KHÔNG hardcode bất kỳ giá trị refund threshold nào trong code. Việc tính toán hoàn tiền pro-rated phải đi qua config keys (planned GAP-108 externalization) — ví dụ `refund.pro.goodwill-cap-pct`, `refund.premium.sla-breach-threshold-hours`. Mọi phần trăm + ngưỡng phải tham chiếu [billing-terms.md](billing-terms.md) (GAP-185 sibling) làm nguồn duy nhất.

**Refund eligibility nguyên tắc cốt lõi:**
- **Trial refund:** áp dụng nếu Customer hủy trong giai đoạn trial; refund pro-rated cho phần đã trả (nếu có upgrade sớm trong trial)
- **Mid-cycle refund:** theo nguyên tắc thiện chí + Consumer Protection Law — Customer có quyền chấm dứt + nhận hoàn lại phần kỳ chưa sử dụng nếu Provider không vi phạm
- **Feature failure refund:** áp dụng khi tính năng quảng cáo không hoạt động hoặc SLA bị breach (xem [billing-terms.md](billing-terms.md) §SLA + GAP-189 customer SLA, planned)
- **Goodwill refund:** discretionary, để Support team xử lý complaint không thuộc 3 loại trên

---

## 2. Refund Process

Quy trình xử lý yêu cầu hoàn tiền theo bốn bước chính, đáp ứng **VN Consumer Protection Law 2023** thời hạn phản hồi.

### 2.1 Request channel

Customer gửi yêu cầu qua một trong các kênh chính thức:
- **In-app:** form Refund Request trong tenant admin panel (planned UI feature)
- **Email:** `support@<provider-domain>` với subject prefix `[REFUND REQUEST]`
- **Hotline:** số hotline support nội bộ Provider (Phase 2 cung cấp)

<!-- Phase 2: confirm contact channels + SLA escalation matrix — informed gut Q3 2026, GAP-154 -->

### 2.2 Information required

Customer phải cung cấp đầy đủ trong yêu cầu:
- Mã tenant + email người yêu cầu (xác minh quyền)
- Mã invoice / transaction ID liên quan
- Lý do yêu cầu (trial cancellation / mid-cycle / feature failure / goodwill)
- Ngày + chi tiết sự kiện làm cơ sở yêu cầu
- Phương thức nhận refund mong muốn (mặc định: same payment method)

Nếu thông tin không đủ, Support sẽ gửi clarification request trong **2 business days** kể từ khi nhận yêu cầu, không tính vào SLA phản hồi chính.

### 2.3 Response SLA

**Provider phản hồi yêu cầu hoàn tiền trong vòng 5 business days kể từ ngày nhận đủ thông tin** (theo VN Consumer Protection Law 2023 — thông lệ áp dụng SaaS B2B/B2C). Phản hồi bao gồm:
- Quyết định approve / partial-approve / deny + lý do
- Số tiền hoàn lại (nếu approve / partial)
- Phương thức + thời gian dự kiến nhận tiền
- Hướng dẫn dispute escalation nếu Customer không đồng ý (xem §5)

<!-- Phase 2: confirm SLA chính xác per VN Consumer Protection Law + competitive benchmark — informed gut Q3 2026, GAP-154 -->

### 2.4 Refund method + timing

- **Phương thức:** mặc định trả về **same payment method** đã sử dụng để thanh toán (per VN payment gateway requirements + chargeback prevention)
- **Thời gian:** **7-14 business days** từ ngày approve, tùy thuộc vào payment gateway + ngân hàng phát hành thẻ:
  - VNPay: 5-10 business days
  - MoMo: 3-7 business days
  - International cards (Visa/Master): 7-14 business days
- **Bằng chứng:** Provider gửi confirmation email với refund transaction ID; Customer có thể tra cứu trong tenant admin Billing History

<!-- Phase 2: payment processor liaison confirms exact SLA per gateway — informed gut Q3 2026, GAP-154 -->

---

## 3. Non-Refundable Items

Một số khoản thanh toán + dịch vụ KHÔNG đủ điều kiện hoàn tiền (đã sử dụng / không thể đảo ngược). Provider phải công bố rõ list này tại checkout + invoice để tránh tranh chấp.

### 3.1 Used services

- **Classes đã dạy / đã diễn ra:** không hoàn tiền cho session đã hoàn thành
- **Certificates đã phát hành:** không thu hồi + không hoàn phí phát hành
- **Storage usage đã tiêu thụ:** dung lượng đã ghi vào MinIO/object storage (không tính trial period)
- **SMS / Zalo notifications đã gửi:** chi phí gateway đã phát sinh

### 3.2 AI generation already delivered

- **AI-generated banners/hero images:** đã render + giao cho tenant, không hoàn phí AI compute
- **AI Branding regenerations đã sử dụng:** mỗi tenant có quota regenerate per tier (xem [`ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §4.3) — quota đã tiêu KHÔNG hoàn lại
- **AI prompt tokens đã tính:** token đã consume qua provider (Ollama/OpenAI) không hoàn

### 3.3 Custom branding already approved

- **Custom logo / theme đã approve qua quality gate:** đã trigger DEPLOYED state (xem [`ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §6 lifecycle), không hoàn phí setup
- **Domain registration / SSL setup fees:** đã trả cho registrar / cert authority bên thứ 3
- **Migration / onboarding consulting fees:** dịch vụ chuyên gia đã thực hiện

<!-- Phase 2: legal counsel verify list theo VN Consumer Protection Law — informed gut Q3 2026, GAP-154 -->

---

## 4. Service Credits

Dạng đền bù thay thế hoàn tiền (alternative to cash refund) — credit ghi vào tài khoản Customer để dùng cho subscription kế tiếp. Ưu tiên áp dụng cho SLA breach + goodwill cases.

### 4.1 Eligibility

Service credit áp dụng cho:
- **SLA breach** (downtime, performance degradation, feature unavailability) — xem GAP-189 customer SLA (planned)
- **Goodwill** sau complaint không đủ điều kiện cash refund
- **Migration/onboarding compensation** cho enterprise tier khi Provider chậm deliver

### 4.2 Calculation

Linked to GAP-189 customer SLA structure (planned — see GAP-189). Khung dự kiến Phase 1:

| SLA breach severity | Credit % of monthly fee |
|---------------------|:-----------------------:|
| Downtime <99.5% (Premium SLA) | TODO 10% |
| Downtime <99% | TODO 25% |
| Downtime <95% | TODO 50% |
| Critical incident affecting tenant operations | TODO 100% (cap 1 month) |

<!-- Phase 2: confirm credit % theo GAP-189 SLA tiering — informed gut Q3 2026, GAP-154 -->

### 4.3 Validity period

- **Mặc định:** TODO 12 tháng kể từ ngày credit phát hành (informed gut Q3 2026)
- **Sau hết hạn:** credit còn dư expire không hoàn lại tiền
- **Non-transferable:** không chuyển nhượng giữa tenants
- **Application order:** auto-apply cho invoice kế tiếp, từ credit cũ đến mới (FIFO)

<!-- Phase 2: validity period per VN Consumer Protection + tax implications — informed gut Q3 2026, GAP-154 -->

---

## 5. Dispute Resolution Process

Khi Customer không đồng ý với quyết định refund hoặc có khiếu nại khác liên quan đến Service, quy trình giải quyết tranh chấp đi theo **escalation ladder** từ informal đến formal, ưu tiên ADR (Alternative Dispute Resolution) trước khi đến court.

### 5.1 Escalation ladder

| Level | Actor | SLA | Outcome |
|:-----:|-------|:---:|---------|
| **L1** | Support Specialist (informal) | Phản hồi 1 business day | Resolution / escalate |
| **L2** | Support Lead (informal) | Phản hồi 3 business days | Resolution / escalate |
| **L3** | Customer Success Director / Operations Manager (informal) | Phản hồi 5 business days | Final informal decision / escalate to formal |
| **L4** | Formal written complaint (per VN Civil Procedure) | Provider response 30 days | Written response + remediation offer |
| **L5** | Mediation (Trung tâm Hòa giải Thương mại) | Per mediation center rules | Mediation agreement / escalate |
| **L6** | Arbitration (VIAC — Vietnam International Arbitration Centre) | Per VIAC rules | Binding arbitration award (commercial contracts only) |
| **L7** | Court (TAND có thẩm quyền per contract jurisdiction clause) | Per VN Civil Procedure Code | Final judgment |

<!-- Phase 2: SLA + escalation criteria refinement — informed gut Q3 2026, GAP-154 -->

### 5.2 Informal escalation (L1-L3)

Mục tiêu giải quyết ≥80% disputes ở L1-L3. Channel: cùng kênh refund request (in-app form, email, hotline). Mọi correspondence phải lưu trữ làm evidence — xem [data-retention-deletion-policy.md](data-retention-deletion-policy.md) §dispute records retention.

### 5.3 Formal written complaint (L4)

Customer gửi văn bản (email hoặc thư bảo đảm) tới địa chỉ Legal/DPO của Provider. Provider phản hồi bằng văn bản trong **30 days** theo VN Civil Procedure Code. Nếu Provider không phản hồi đúng hạn, Customer có quyền escalate lên L5+ ngay lập tức.

### 5.4 Mediation (L5)

Áp dụng ADR qua **Trung tâm Hòa giải Thương mại** (commercial mediation center). Cả hai bên thỏa thuận chọn mediator + chia chi phí. Mediation agreement nếu đạt là binding nếu được công nhận theo Luật Hòa giải Thương mại.

### 5.5 Arbitration (L6)

**Áp dụng CHỈ cho Commercial Customers** (xem §6) — không áp dụng cho Consumer Customers theo VN Consumer Protection Law (consumers retain right to courts).

- **Cơ quan:** **VIAC — Vietnam International Arbitration Centre** (mặc định) hoặc theo thỏa thuận khác trong contract
- **Quy tắc:** VIAC Rules of Arbitration (current version)
- **Ngôn ngữ:** Tiếng Việt (mặc định) hoặc song ngữ Việt-Anh nếu thỏa thuận
- **Số trọng tài:** 1 sole arbitrator hoặc 3-arbitrator panel tùy giá trị tranh chấp
- **Quyết định trọng tài:** binding + không kháng cáo trừ trường hợp luật định

<!-- Phase 2: VIAC clause exact wording cho enterprise contracts — informed gut Q3 2026, GAP-154 -->

### 5.6 Court (L7)

Final escalation nếu các bước trên thất bại hoặc không áp dụng:
- **Mặc định jurisdiction:** **TAND có thẩm quyền tại địa điểm trụ sở Provider** (HCMC) per [terms-of-service.md](terms-of-service.md) jurisdiction clause
- **Enterprise contracts:** có thể negotiate jurisdiction tại địa điểm trụ sở Customer
- **Consumer disputes:** Consumer có quyền chọn TAND nơi cư trú của mình per Consumer Protection Law

<!-- Phase 2: legal counsel verify jurisdiction clauses across enterprise contract templates — informed gut Q3 2026, GAP-154 -->

---

## 6. Consumer vs Commercial Customers

Phân biệt quan trọng theo VN luật pháp — Consumer được bảo vệ mạnh hơn theo **Consumer Protection Law 2023**, Commercial customers chịu **Commercial Law 2005**. Provider phải nhận diện đúng phân loại từ thời điểm signup.

### 6.1 Consumer Customers

**Định nghĩa:** Cá nhân hoặc hộ gia đình sử dụng Service cho mục đích phi-thương-mại / cá-nhân — bao gồm:
- Solo Teacher (gia sư tự do, không đăng ký kinh doanh)
- Small Center Owner (chủ trung tâm nhỏ chưa thành lập pháp nhân)
- Cá nhân test platform cho mục đích cá nhân

**Quyền lợi đặc biệt theo Consumer Protection Law 2023:**
- **Quyền hủy hợp đồng từ xa** (right of withdrawal) — Phase 2 confirm thời hạn cụ thể, thường 7-14 ngày kể từ signup
- **Quyền nhận thông tin rõ ràng** về điều khoản hoàn tiền (Art 14) — chính sách này phải dễ tiếp cận
- **Cấm điều khoản bất công** (unfair terms) — không thể buộc consumer waive quyền refund hoặc dispute
- **Quyền chọn jurisdiction** — Consumer có quyền chọn TAND nơi cư trú khi kiện
- **Không bị bắt buộc arbitration** — clause arbitration trong consumer contract không binding

### 6.2 Commercial Customers

**Định nghĩa:** Pháp nhân hoặc tổ chức đăng ký kinh doanh sử dụng Service cho mục đích thương mại — bao gồm:
- Incorporated schools (trường tư có giấy phép)
- Education chains (chuỗi trung tâm có pháp nhân)
- Enterprise tier customers
- Mid Center Owner đã đăng ký kinh doanh

**Quyền lợi + nghĩa vụ:**
- **Contract-based:** điều khoản refund + dispute áp dụng theo TOS / enterprise contract đã ký
- **Commercial Law 2005:** áp dụng cho dispute giữa các bên thương mại
- **Arbitration available:** có thể arbitration qua VIAC nếu thỏa thuận trong contract
- **Negotiable terms:** enterprise tier có thể negotiate refund + dispute clauses cụ thể

### 6.3 Classification rule

Tại signup, Customer khai báo loại customer (Consumer / Commercial). Provider verify qua:
- Mã số doanh nghiệp (GPKD) cho Commercial → bắt buộc cho Pro/Premium/Enterprise tier
- Free tier mặc định coi là Consumer trừ khi Customer cung cấp GPKD
- Bug: misclassification phải được fix trong vòng 30 days kể từ khi phát hiện, retrospective apply quyền consumer protection

<!-- Phase 2: legal counsel verify classification logic + KYC requirements — informed gut Q3 2026, GAP-154 -->

---

## 7. Chargeback Handling

Chargeback (hoàn lại qua ngân hàng) xảy ra khi Customer disputes giao dịch trực tiếp với ngân hàng phát hành thẻ thay vì qua Provider. Provider phải có quy trình response để bảo vệ doanh thu hợp pháp + tuân thủ payment processor SLA.

### 7.1 Response procedure

Khi nhận chargeback notification từ payment processor (VNPay/MoMo/Visa/Master):

1. **Acknowledge trong 2 business days** — payment processor SLA thường yêu cầu response window 7-15 days
2. **Pull evidence package:**
   - Invoice + payment confirmation
   - TOS acceptance log (click-wrap timestamp)
   - Service usage logs (login history, feature usage)
   - Communication history với Customer
   - Screenshot of disputed Service delivered
3. **Submit response qua processor portal** với evidence + cover letter giải thích
4. **Track outcome** — won (giao dịch giữ lại) / lost (refund + chargeback fee)
5. **Post-mortem:** nếu lost, phân tích root cause để prevent tái diễn

### 7.2 Evidence collection

Provider phải lưu giữ + dễ truy xuất:
- TOS click-wrap acceptance logs (xem GAP-180 implementation)
- Audit logs của Customer activity (xem [data-retention-deletion-policy.md](data-retention-deletion-policy.md) audit log retention)
- Email correspondence với Customer trong cycle disputed
- Service delivery proof (account active, features accessed)

Retention period cho chargeback evidence: **18 tháng** (per VNPay/MoMo SLA dispute window) — sẽ được aligned với [data-retention-deletion-policy.md](data-retention-deletion-policy.md) trong Phase 2.

### 7.3 Alignment với VNPay/MoMo SLA

- **VNPay:** chargeback response window 7 days, evidence submission qua merchant portal
- **MoMo:** dispute response window 10 days, escalation qua MoMo Business support
- **International cards (Visa/Master qua acquirer):** chargeback window 120 days, response 7-15 days tùy reason code

<!-- Phase 2: payment processor liaison confirms exact SLA + evidence templates — informed gut Q3 2026, GAP-154 -->

### 7.4 Chargeback prevention

- **Clear billing description:** transaction descriptor trên statement phải khớp brand Customer nhận diện
- **Reminder emails** trước billing cycle để Customer nhận biết auto-renew
- **Easy cancellation flow** trong tenant admin để Customer không phải dispute qua bank
- **Refund proactive** cho cases borderline để tránh chargeback fee

---

## 8. Force Majeure

Phân loại service interruptions theo nguyên nhân — Provider fault chịu refund/credit obligation, Force Majeure events miễn trách nhiệm theo VN Civil Code 2015.

### 8.1 Provider fault — refund/credit applicable

Sự cố do Provider chịu trách nhiệm khắc phục + đền bù:
- **Infrastructure failure:** server crash, database corruption, network misconfiguration trong scope Provider quản lý
- **Software bugs:** feature broken do code defect, deployed regression
- **Capacity issues:** quá tải do Provider không scale kịp (không phải traffic spike đột biến của Customer)
- **Vendor lock-in failure:** AI provider downtime mà Provider không có fallback (xem [`ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §10 Circuit Breaker requirement)
- **Security incidents** do Provider negligence (chưa patch known CVE, weak auth config)

→ Áp dụng SLA credit per §4 + cash refund per §1 nếu Customer yêu cầu.

### 8.2 Force Majeure events — exempt

Sự kiện bất khả kháng theo **VN Civil Code 2015 Art 156** — Provider không chịu refund/credit obligation:
- **Natural disasters:** bão, lũ lụt, động đất, hỏa hoạn không phải do Provider
- **Government action:** internet shutdown, regulatory ban, tax/license suspension không cảnh báo
- **War, terrorism, civil unrest:** ảnh hưởng infrastructure or operations
- **Pandemic / public health emergency:** governmental lockdowns affecting service
- **Upstream provider catastrophic failure:** AWS region down, Cloudflare global outage, ISP backbone failure ngoài tầm kiểm soát Provider
- **Cyberattacks ngoài tầm phòng vệ thông thường:** zero-day exploit chưa được vendor patch, nation-state attack

### 8.3 Notice + duration

Khi Force Majeure event xảy ra, Provider phải:
1. **Notice trong 48 giờ** qua status page + email tới Customer affected
2. **Cập nhật progress** ít nhất mỗi 24h trong duration event
3. **Resume notification** khi service phục hồi
4. **Post-mortem report** trong 7 ngày sau resume

Nếu Force Majeure kéo dài >**30 ngày**, Customer có quyền terminate contract + nhận refund cho phần subscription chưa sử dụng (per VN Civil Code Art 156).

<!-- Phase 2: legal counsel verify Force Majeure clause + notice procedure per Civil Code 2015 — informed gut Q3 2026, GAP-154 -->

### 8.4 Borderline cases

Một số sự cố nằm giữa Provider fault và Force Majeure — Provider phải làm rõ:
- **DDoS attack:** Provider fault nếu chưa có DDoS protection cơ bản; Force Majeure nếu attack volume vượt commercially-reasonable defense
- **Third-party AI provider degradation:** Provider fault nếu chưa implement Circuit Breaker fallback (xem [`ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §10); Force Majeure nếu fallback cũng down
- **DNS provider outage:** thường Force Majeure (Cloudflare/Route53), trừ khi do Provider misconfiguration

Quyết định case-by-case bởi Customer Success Director với input từ Legal — Customer có quyền dispute classification qua escalation ladder §5.

---

## Phase 2 — Tracking + ownership

| Item | Owner Phase 2 | Tracking |
|------|---------------|----------|
| Refund eligibility matrix values (TODO %, days, caps) | Product Owner + Finance + Legal | GAP-154 |
| Response SLA + payment timing per gateway | Payment processor liaison + Finance | GAP-154 |
| Service credit calculation theo SLA tiers | Customer Success + Engineering | GAP-189 (customer SLA) + GAP-154 |
| Validity period for service credits | Finance + Legal (tax implications) | GAP-154 |
| Dispute escalation SLA per level | Customer Success + Operations | GAP-154 |
| VIAC arbitration clause exact wording | Legal counsel | GAP-154 |
| Consumer vs Commercial classification logic | Legal counsel + Engineering (KYC) | GAP-154 |
| Chargeback evidence templates per processor | Finance + Engineering | GAP-154 |
| Force Majeure clause final wording | Legal counsel | GAP-154 |
| Contract templates update (enterprise tier) | Legal counsel | GAP-154 |
| Config externalization (refund.*, dispute.*) | Engineering | GAP-108 |
| Refund workflow implementation | Engineering | Separate feature gap |
| Chargeback response automation | Engineering + Finance | Separate feature gap |

**Phase 2 trigger:** Provider engages legal counsel chuyên về VN Consumer Protection Law + payment processor liaison. Phase 2 deliverable phải pass `business-logic-review.md` 5-attribute review (Source / Rationale / Reviewer / Compliance check / Review cadence) cho mọi giá trị business value (% refund, days SLA, credit caps).

---

## Cross-references — sibling skeletons + planned docs

- [terms-of-service.md](terms-of-service.md) — GAP-180 (sibling Wave Legal-BRD Phase 1) — TOS containing dispute resolution clause that references this doc
- [acceptable-use-policy.md](acceptable-use-policy.md) — GAP-181 (sibling Wave Legal-BRD Phase 1) — AUP violations triggering account suspension (different from refund disputes)
- [privacy-policy.md](privacy-policy.md) — GAP-182 (sibling Wave Legal-BRD Phase 1) — privacy disputes có thể overlap khi Customer claim PDPL violation
- [data-retention-deletion-policy.md](data-retention-deletion-policy.md) — GAP-184 (sibling Wave Legal-BRD Phase 1) — dispute records + chargeback evidence retention
- [billing-terms.md](billing-terms.md) — GAP-185 (sibling Wave Legal-BRD Phase 1.5) — billing cycle + invoice + late fee structure (refund calculation cơ sở)
- GAP-189 — Customer SLA / Service Level Agreement (planned — service credits eligibility)
- GAP-108 — Payment/invoice config externalization (planned — refund threshold config keys)
- GAP-154 — Phase 2 umbrella — legal counsel content + sign-off + payment processor alignment + contract templates
