# Terms of Service (TOS) — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — section structure + TODO markers; Phase 2 content via GAP-154 umbrella)
**Owner:** Legal + PM/CEO
**Reviewer:** PM + CEO + Legal counsel (Phase 2)
**Last-Updated:** 2026-04-29
**Tracking:** GAP-180 (Phase 1, Wave Legal-BRD 2026-04-29) → GAP-154 (Phase 2 content + legal sign-off)
**Legal basis:** VN Civil Code 2015 (contract formation), Luật Giao dịch điện tử 2023, Luật Bảo vệ Quyền lợi Người tiêu dùng 2023
**Acceptance mechanism:** Click-wrap at signup, re-accept on modification
**Jurisdiction:** TAND có thẩm quyền theo địa điểm trụ sở Provider, Vietnam law governing

---

## Mục đích tài liệu

Đây là **bộ khung (skeleton)** cho Terms of Service (TOS) ràng buộc giữa Provider (đơn vị vận hành KiteHub/KiteClass) và Customer (tenant đăng ký dịch vụ). Phase 1 ship cấu trúc 15 sections + cross-link slots + TODO markers cho phần content thực tế. Phase 2 (legal counsel content + sign-off) tracked qua GAP-154 umbrella, blocked-on stakeholder engagement.

**Mục tiêu phase 1:**
- Unblock payment processor onboarding (VNPay/Stripe yêu cầu TOS link công khai)
- Cung cấp khung cho enterprise RFP responses (legal team đối tác cần xem section list)
- Reserve cross-link slots cho 4 sibling skeletons cùng Wave (AUP, Privacy, Retention)
- Document acceptance mechanism + jurisdiction từ trước → tránh rework khi legal counsel engage Phase 2

**KHÔNG phải mục tiêu phase 1:**
- Final legal text (chờ legal counsel)
- Click-wrap UI implementation (separate feature gap)
- TOS versioning/history storage (feature gap, not BRD doc)
- English translation (defer post-launch)

---

## Glossary — Defined Terms

Section này định nghĩa các thuật ngữ key được capitalized trong toàn TOS. Mỗi term sẽ được legal counsel finalize trong Phase 2; placeholder dưới đây là working definition.

- **"Provider"** — đơn vị pháp nhân vận hành nền tảng KiteHub (SaaS quản lý) và KiteClass (multi-tenant education). <!-- Phase 2: legal counsel to fill chính xác tên doanh nghiệp + GPKD + địa chỉ trụ sở — informed gut value, GAP-154 -->
- **"Customer"** (hoặc "Tenant") — chủ thể đăng ký subscription cho Service: trung tâm giáo dục, trường học, gia sư tự do, hoặc tổ chức/cá nhân khác đại diện cho cơ sở giáo dục. Customer chịu trách nhiệm content + lawful use cho instance của họ.
- **"End User"** — người dùng cuối truy cập Service qua tenant của Customer: teacher, student, parent, accountant, admin, hoặc role khác do Customer cấu hình. End User KHÔNG phải đối tác trực tiếp của Provider — quan hệ pháp lý đi qua Customer.
- **"Service"** — toàn bộ phần mềm + hạ tầng + tính năng + AI Branding + storage + support do Provider cung cấp qua KiteHub/KiteClass platform, theo gói subscription đã chọn.
- **"Content"** — bất kỳ data, văn bản, hình ảnh, file đính kèm, AI-generated assets, audit logs, communications, mà Customer hoặc End Users upload/tạo/lưu trên Service.
- **"Confidential Information"** — non-public business information mà mỗi bên disclose cho bên kia trong quá trình thực thi TOS, bao gồm pricing, technical details, customer data, business plans.
- **"Effective Date"** — ngày Customer click-wrap accept TOS lần đầu tại signup, hoặc ngày Customer accept revised TOS sau khi modification được notice.
- **"Term"** — khoảng thời gian TOS có hiệu lực, từ Effective Date cho đến termination per §9.

**Phase 2 TODO:**
- [ ] Legal counsel review từng definition above + bổ sung thuật ngữ mới (ví dụ: "Personal Data", "Affiliate", "Subprocessor" theo PDPL terminology)
- [ ] Cross-check terminology nhất quán với Privacy Policy (privacy-policy.md) và Acceptable Use Policy (acceptable-use-policy.md)
- [ ] Thêm definition cho subscription tiers (FREE/BASIC/PREMIUM/ENTERPRISE) + reference pricing-model.md

---

## 1. Parties + Definitions

Section này xác định các bên ký kết TOS: Provider (KiteClass/KiteHub) một bên, Customer (tenant) bên kia. Quan hệ pháp lý 3-tier (Provider ↔ Customer ↔ End Users) cần được làm rõ vì End Users (teacher, student, parent) truy cập Service qua tenant của Customer chứ không trực tiếp ký TOS với Provider — Customer đại diện cho End Users trong phạm vi instance của họ.

Section cũng reference Glossary ở trên cho các thuật ngữ capitalized. Provider info (tên pháp nhân, GPKD, địa chỉ, email contact) sẽ được điền chính xác trong Phase 2 sau khi business entity được formalize.

**Phase 2 TODO:**
- [ ] Legal counsel điền tên Provider entity chính xác (KiteHub Co., Ltd hoặc tương đương) + GPKD number + registered address
- [ ] Xác nhận Customer eligibility: tổ chức có GPKD, hộ kinh doanh, cá nhân (gia sư tự do) — quy định khác nhau về thuế + invoice requirements
- [ ] Document Provider's authorized signatory + designated contact email cho TOS-related communications
- [ ] Cross-link tới personas-catalog.md cho End User role taxonomy (teacher/student/parent/accountant/admin)

---

## 2. Service Description

Section này mô tả phạm vi Service mà Customer được sử dụng theo subscription tier. Bao gồm: (a) core platform features (KiteHub instance management; KiteClass education business — student/course/class/attendance/grade/payment), (b) AI Branding subsystem (per ai-branding-guidelines), (c) shared infrastructure access, (d) tier-specific features và quotas, (e) support tiers, (f) explicit exclusions (không bao gồm hosting domain riêng của Customer ngoài subdomain Provider cấp, không bao gồm legal advice, không bao gồm data migration từ legacy systems trừ khi mua add-on).

Service Description ràng buộc Provider phải cung cấp đúng phạm vi đã advertise; ràng buộc Customer chỉ được sử dụng trong phạm vi đó (không reverse engineer, không resell — chi tiết tại §5 Acceptable Use).

**Phase 2 TODO:**
- [ ] Liệt kê chi tiết features per tier theo `documents/00-brd/pricing-model.md` (FREE/BASIC/PREMIUM/ENTERPRISE)
- [ ] Liệt kê AI Branding quotas per tier theo `ai-branding-guidelines.md` §4.3 (FREE 3, BASIC 10, PREMIUM 30, ENTERPRISE unlimited)
- [ ] Document explicit exclusions (custom development, on-premise deployment, white-label, data migration services)
- [ ] Reference SLA document riêng (planned — see GAP-189) cho uptime + support response time commitments
- [ ] Document upgrade/downgrade mechanism + prorated billing handling (cross-link với Payment Terms §7 → planned GAP-185)

---

## 3. Customer Obligations

Section này liệt kê các nghĩa vụ Customer phải tuân thủ trong suốt Term:

- **Content responsibility:** Customer chịu trách nhiệm pháp lý cho mọi Content do Customer hoặc End Users upload/tạo. Provider không pre-screen Content trừ AI-generated outputs (per Quality Gate ai-branding-guidelines.md §5).
- **Account security:** Customer bảo mật credentials (admin password, API keys, OAuth tokens), không share cho bên thứ ba ngoài role-based access do Customer phân quyền.
- **Lawful use:** tuân thủ Acceptable Use Policy (AUP — see acceptable-use-policy.md), không vi phạm pháp luật Việt Nam (Luật An ninh mạng, Luật Sở hữu trí tuệ, Luật Bảo vệ Trẻ em, etc.) hoặc luật quốc tế áp dụng.
- **Data accuracy:** đảm bảo data Customer cung cấp khi signup (tên pháp nhân, GPKD, địa chỉ, contact) là chính xác + cập nhật trong vòng 30 ngày khi có thay đổi.
- **Payment timeliness:** thanh toán đúng hạn theo Payment Terms (§7) để tránh suspension.
- **End User governance:** Customer chịu trách nhiệm enforce Acceptable Use cho End Users của tenant mình; Customer là first-line response cho End User violations.
- **Compliance với PDPL:** Customer tự xác định vai trò của mình (Personal Data Controller hay Processor) theo PDPL Decree 13/2023 và tuân thủ obligations tương ứng. Provider hỗ trợ qua Privacy Policy (privacy-policy.md) + DPO designation.

**Phase 2 TODO:**
- [ ] Legal counsel review với scope của vai trò Controller/Processor split theo PDPL — Customer có thể là Controller cho End User data trong khi Provider là Processor; cần data processing agreement (DPA) skeleton (planned — separate document or addendum)
- [ ] Document tax invoice obligations: Customer phải cung cấp đầy đủ thông tin cho Provider phát hành hóa đơn điện tử (tên doanh nghiệp, MST, địa chỉ) per Nghị định 123/2020/NĐ-CP
- [ ] Document export controls + sanctions compliance nếu Customer ngoài VN
- [ ] Document Customer's obligation cho dữ liệu legacy migrate-in (Customer warranties data có quyền sử dụng + không vi phạm copyright của bên thứ ba)

---

## 4. Provider Obligations

Section này liệt kê các cam kết Provider đối với Customer trong Term:

- **Service availability:** Provider commit uptime % per tier theo Service Level Agreement (planned — see GAP-189). Downtime ngoài planned maintenance được tính credit/refund per SLA terms.
- **Support:** Provider cung cấp support qua kênh + response time tương ứng tier. FREE = community/docs only; BASIC = email business hours; PREMIUM = email + chat business hours; ENTERPRISE = priority + dedicated CSM (per pricing-model.md).
- **Data security:** Provider implement security measures theo industry standards: encryption at rest + in transit, access controls, audit logging, periodic security audits (per `documents/04-quality/audits/security-audit-*.md`). Chi tiết tại Privacy Policy (privacy-policy.md) §13 Security Measures.
- **Data confidentiality:** Provider không sell, rent, hoặc share Customer data cho bên thứ ba ngoài subprocessors necessary cho Service operation (payment gateway, email provider, hosting) — chi tiết tại Privacy Policy §7.
- **Notice of changes:** Provider notice Customer trước modification material đối với TOS, Privacy Policy, hoặc tier features (per §14 Modifications).
- **Data export on termination:** Provider cung cấp data export tools cho Customer trong reasonable timeframe sau termination (per §9 Term + Termination, cross-link Data Retention/Deletion Policy data-retention-deletion-policy.md).
- **Lawful operation:** Provider tuân thủ pháp luật VN (Luật An ninh mạng, PDPL, Luật Quản lý Thuế, Luật Doanh nghiệp) — bao gồm cooperation với cơ quan có thẩm quyền khi nhận yêu cầu hợp pháp.

**Phase 2 TODO:**
- [ ] Cross-link SLA document khi GAP-189 ship — uptime targets (FREE 95% / BASIC 99% / PREMIUM 99.5% / ENTERPRISE 99.9% — TODO actual values per business-rules-review.md 5-attribute) <!-- Phase 2: legal counsel + PM to finalize uptime tiers — informed gut value, GAP-154 -->
- [ ] Document support SLA chi tiết (response time targets) per tier
- [ ] Reference latest security audit report path
- [ ] Document subprocessor list + change notification process (per PDPL Art 11(4) subject right to know about subprocessors)
- [ ] Document Provider's data export format + delivery mechanism (CSV, JSON, API, full DB dump) trong 30 ngày sau termination

---

## 5. Acceptable Use

Acceptable Use Policy (AUP) ràng buộc cách Customer + End Users sử dụng Service. Detailed AUP nằm trong tài liệu riêng — see [acceptable-use-policy.md](acceptable-use-policy.md) (sibling skeleton same-wave, GAP-181).

Tóm tắt high-level:
- **Prohibited content:** illegal content (theo Luật Hình sự VN), CSAM (zero-tolerance), hate speech, adult content, copyrighted material không có quyền, misinformation gây hại.
- **Prohibited conduct:** account sharing ngoài quy định, bot/automation không authorized, scraping, reverse engineering, spam, attacks vào infrastructure.
- **Education-specific prohibitions:** academic fraud, leaked exams, teacher impersonation, predatory behavior toward minors (cross-cuts với Child Protection Policy planned — see GAP-186).
- **Enforcement:** Provider có quyền warn → suspend → terminate theo strike system + appeal flow defined trong AUP.

Vi phạm AUP material là grounds for termination per §9.

**Phase 2 TODO:**
- [ ] Verify acceptable-use-policy.md sibling shipped đúng path (test: `ls documents/00-brd/acceptable-use-policy.md`)
- [ ] Sync AUP enforcement language với §10 Warranties + §11 Limitation of Liability
- [ ] Document precedence rule khi AUP và TOS xung đột (default: TOS controls trừ khi AUP express more specific)

---

## 6. Intellectual Property

Section này xác định ownership + license của IP rights giữa Provider và Customer:

- **Customer Data Ownership:** Customer giữ toàn bộ quyền sở hữu Content do Customer + End Users tạo trong Service (student records, gradebooks, attendance logs, uploaded files, parent communications, custom branding assets). Provider chỉ có license giới hạn để host, process, backup, deliver Service tới Customer + End Users. License terminate khi TOS terminate (trừ phần data Provider phải retain theo legal hold hoặc audit log per Data Retention Policy).
- **Provider IP:** Provider giữ toàn bộ IP rights của KiteHub/KiteClass platform, source code, documentation, design templates (UI templates, image templates per ai-branding-guidelines.md §8), trademarks, AI model weights và prompts internal, và derivative works. Customer KHÔNG có quyền reverse engineer, decompile, hoặc copy.
- **AI-Generated Output Ownership:** Output AI-generated cho Customer instance (banners, hero images, generated copy) — Customer được license sử dụng cho instance của họ; Provider giữ background IP của model + prompts. Cross-link ai-branding-guidelines.md §6 (lifecycle ownership rules).
- **Feedback License:** nếu Customer cung cấp feedback, suggestions, bug reports, feature requests, Provider có irrevocable license sử dụng feedback đó để improve Service mà không owe compensation cho Customer.
- **Trademarks:** Customer không được sử dụng "KiteHub", "KiteClass", logo, hoặc trademark của Provider cho mục đích marketing nếu chưa có written permission. Tương tự, Provider không sử dụng Customer's trademark ngoài purpose xác thực Customer là user.

**Phase 2 TODO:**
- [ ] Legal counsel review IP ownership clauses theo Luật Sở hữu trí tuệ VN 2005 (sửa đổi 2009, 2019, 2022) — đặc biệt §10 Quyền tác giả + §27 Quyền sở hữu công nghiệp
- [ ] Document open-source acknowledgments (third-party libraries used trong KiteHub/KiteClass code — license compatibility check)
- [ ] Document AI training opt-out: Customer có quyền opt-out khỏi việc data của họ được dùng để train AI models (PDPL Art 17 right to object to processing — applicable cho personal data)
- [ ] Document trademark usage guidelines + brand kit cho marketing case-study (with consent)

---

## 7. Payment Terms

Detailed payment terms (subscription pricing, billing cycle, late fee %, refund window, VAT/TCT handling, e-invoice mechanism per Nghị định 123/2020/NĐ-CP) sẽ được document trong Billing Terms riêng (planned — see GAP-185 Phase 2 deferred to next wave).

Tóm tắt high-level cho Phase 1:
- **Subscription tiers:** FREE / BASIC / PREMIUM / ENTERPRISE (per pricing-model.md). FREE không có payment obligation.
- **Billing cycle:** monthly hoặc annual prepay (annual discount per pricing model).
- **Payment methods:** VNPay, MoMo, Zalo Pay, bank transfer (domestic), credit card (qua VNPay/Stripe gateway).
- **VAT:** giá listed có thể chưa bao gồm VAT 10% — Customer có VN GPKD được phát hành hóa đơn điện tử theo yêu cầu.
- **Late payment:** subscription auto-suspend sau X ngày overdue (TODO value per business-logic-review.md 5-attribute review). Suspension không terminate TOS — data preserved trong grace period rồi mới deletion-eligible per Data Retention Policy.
- **Refund policy:** detailed trong Billing Terms (GAP-185); high-level money-back guarantee window TODO (typically 14 hoặc 30 days post first paid period).

**Phase 2 TODO:**
- [ ] Cross-link Billing Terms khi GAP-185 ship
- [ ] Document VAT handling chi tiết per Nghị định 123/2020/NĐ-CP — invoice generation timing, format, Customer's obligation cung cấp MST <!-- Phase 2: tax advisor + legal to fill — informed gut value, GAP-154 -->
- [ ] Document late fee % + grace period days với 5-attribute review per `business-logic-review.md` §2
- [ ] Document refund policy chi tiết + dispute resolution path (cross-link §13 Dispute Resolution)
- [ ] Document price change notice period (typically 30-60 days advance notice cho subscription renewals)

---

## 8. Confidentiality + Data Protection

Section này govern handling Confidential Information (per Glossary) + Personal Data (per PDPL).

- **Mutual confidentiality obligation:** Provider và Customer phải bảo vệ Confidential Information của nhau với reasonable care (industry-standard security). Confidentiality obligation survive termination trong X năm (TODO per legal counsel — typically 3-5 years).
- **Personal Data:** xử lý Personal Data tuân thủ PDPL Decree 13/2023/NĐ-CP và Privacy Policy riêng — see [privacy-policy.md](privacy-policy.md) (sibling skeleton same-wave, GAP-182). Privacy Policy detail: data subject rights, retention, cross-border transfer, breach notification (72h per PDPL).
- **Data Processing Agreement (DPA):** for Customer cần Provider làm Processor cho End User data, parties sẽ ký DPA addendum (Phase 2 deliverable).
- **Subprocessor management:** Provider duy trì list subprocessors công khai (payment gateway VNPay/MoMo, email provider, hosting); notify Customer trước khi thay đổi material per PDPL Art 11(4).
- **Audit rights:** Customer ENTERPRISE tier có quyền request annual security audit summary (không phải full pen-test report — Provider's IP). Smaller tiers reference latest published audit report.

**Phase 2 TODO:**
- [ ] Confidentiality obligation duration finalize (3y / 5y / perpetual cho trade secrets)
- [ ] DPA addendum skeleton (separate doc, GAP-154 umbrella)
- [ ] Subprocessor list công khai (separate page hoặc privacy-policy.md §7)
- [ ] Document End User direct rights vs through-Customer rights per PDPL Art 11
- [ ] Document audit access scope cho ENTERPRISE tier (frequency, scope, NDA requirements)

---

## 9. Term + Termination

Section này xác định khi nào TOS có hiệu lực, khi nào terminate, và hậu quả termination:

- **Term:** TOS bắt đầu vào Effective Date và tiếp diễn cho đến khi terminate per section này.
- **Customer termination for convenience:** Customer có thể cancel subscription bất kỳ lúc nào qua admin dashboard hoặc email request. Cancellation effective vào cuối billing cycle hiện tại (no prorated refund cho phần còn lại trừ khi pricing-model nói khác).
- **Provider termination for cause:** Provider có thể suspend hoặc terminate ngay lập tức nếu Customer vi phạm material TOS, AUP, hoặc Payment Terms (sau notice + cure period nếu applicable). Examples: non-payment > grace period, AUP zero-tolerance violation (CSAM), illegal use, security breach do Customer cause.
- **Provider termination for convenience:** Provider có thể terminate với notice X ngày advance (TODO — typically 30-90 days). Customer được pro-rated refund cho phần subscription paid-in-advance.
- **Effect of termination:**
  - Service access immediately suspend (Customer + End Users).
  - Customer có data export window (TODO — typically 30-90 days post-termination) để download data per §4 Provider Obligations.
  - Sau export window, data delete per Data Retention + Deletion Policy — see [data-retention-deletion-policy.md](data-retention-deletion-policy.md) (sibling skeleton same-wave, GAP-184).
  - Provider có thể giữ aggregated/anonymized analytics + audit logs per legal hold + retention obligations (PDPL, Tax Law, Cybersecurity Law).
- **Survival:** sections survive termination: §6 IP (Provider IP + Feedback license), §8 Confidentiality (X years), §10-11 Warranties + Liability limits, §12 Indemnification (cho events trước termination), §13 Dispute Resolution, §15 Governing Law.

**Phase 2 TODO:**
- [ ] Termination notice period (30 vs 60 vs 90 days) finalize per business-logic-review.md 5-attribute
- [ ] Cure period cho material breach (typically 14-30 days với written notice)
- [ ] Data export window length finalize (cross-link với data-retention-deletion-policy.md tenant offboarding runbook)
- [ ] Document tenant offboarding runbook reference (planned — `documents/05-guides/tenant-offboarding.md` Phase 2)
- [ ] Document data preservation cho legal hold scenarios

---

## 10. Warranties + Disclaimers

Section này xác định warranties Provider đưa ra + disclaimers excluding warranties khác:

- **Provider warranties:**
  - Service được cung cấp với reasonable care + skill (industry standard SaaS).
  - Provider có quyền pháp lý cung cấp Service (không vi phạm IP của bên thứ ba theo Provider's knowledge).
  - Provider sẽ tuân thủ pháp luật VN trong quá trình cung cấp Service.
- **AS-IS / AS-AVAILABLE disclaimer:** ngoài warranties express trên, Service được cung cấp AS-IS và AS-AVAILABLE. Provider DISCLAIM tất cả implied warranties trong phạm vi pháp luật cho phép, bao gồm: merchantability, fitness for particular purpose, non-infringement, accuracy, uninterrupted operation, error-free operation.
- **AI Output disclaimer:** AI-Generated Content (banners, hero, generated text) được produce bởi machine learning models — Provider không warrant accuracy, originality, hoặc legal compliance của AI output. Customer phải review trước khi publish (per Quality Gate ai-branding-guidelines.md §5 + Preview-before-Commit §4.2).
- **Third-party services:** Service tích hợp với third-party providers (VNPay, MoMo, Zalo, Google Workspace, Ollama/OpenAI). Provider không warrant third-party service availability hoặc quality.
- **Vietnamese consumer law preservation:** disclaimers above KHÔNG limit Customer's rights theo Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 đối với sản phẩm/dịch vụ tiêu dùng — non-waivable consumer rights vẫn applicable.

**Phase 2 TODO:**
- [ ] Legal counsel review disclaimers theo Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 — Article 16 limits unfair terms; some disclaimers có thể không enforceable vs consumer Customers
- [ ] Document warranty scope cho ENTERPRISE tier (có thể có additional warranties trong order form / MSA)
- [ ] Cross-link Quality Gate cho AI output (ai-branding-guidelines.md §5)
- [ ] Document third-party service status page references (Provider không control these)

---

## 11. Limitation of Liability

Section này cap mức damages mỗi bên có thể đòi từ bên kia + exclude certain damage categories:

- **Cap on damages:** total liability của Provider đối với Customer (across all claims, all theories) cap tại X (TODO — typically tổng amount Customer trả Provider trong 12 tháng trước claim, hoặc fixed cap như 1000 USD cho FREE tier).
- **Excluded damages:** ngoài phạm vi pháp luật cho phép, mỗi bên loại trừ liability cho: indirect, consequential, special, punitive, exemplary damages; lost profits, lost business, lost data (trừ khi data loss do Provider gross negligence breach §4 Data security obligation).
- **Exceptions to cap:** cap KHÔNG apply cho: (a) Customer's payment obligations (full subscription owed), (b) Customer's IP indemnity (unlimited cho IP claims), (c) breach of Confidentiality, (d) gross negligence hoặc willful misconduct, (e) unwaivable consumer rights theo Luật Bảo vệ Quyền lợi Người tiêu dùng.
- **Vietnamese law preservation:** limitation of liability KHÔNG limit liability cho personal injury hoặc property damage do Provider gross negligence — non-waivable per VN Civil Code 2015 §584-587.

**Phase 2 TODO:**
- [ ] Liability cap amount finalize per tier per business-logic-review.md 5-attribute review <!-- Phase 2: legal counsel + PM to finalize cap formula — informed gut value, GAP-154 -->
- [ ] Cross-check exclusions list vs VN Civil Code unwaivable liabilities
- [ ] Document insurance coverage Provider carries (E&O, cyber liability) cho ENTERPRISE customers wanting to verify
- [ ] Document negligence threshold ("gross" vs ordinary) per VN legal interpretation
- [ ] Document special handling cho data breach liability (regulatory fines pass-through scenarios)

---

## 12. Indemnification

Section này quy định nghĩa vụ bồi thường giữa các bên:

- **Customer indemnifies Provider:** Customer hold harmless Provider khỏi claims của bên thứ ba liên quan tới: (a) Content do Customer/End Users upload (IP infringement, defamation, illegal content), (b) Customer's misuse Service vi phạm AUP, (c) Customer's failure to comply với laws (PDPL violations cho End User data, tax obligations, education sector regulations), (d) Customer's breach TOS material.
- **Provider indemnifies Customer:** Provider hold harmless Customer khỏi claims của bên thứ ba liên quan tới: (a) Provider's IP infringement (Service infringes third-party IP — Provider's knowledge requirement), (b) Provider's gross negligence security breach causing data loss to Customer, (c) Provider's breach of Confidentiality.
- **Procedure:**
  - Indemnified party notify indemnifying party promptly khi nhận claim (failure to notify within reasonable time chỉ giảm indemnity nếu prejudice tới defense).
  - Indemnifying party có quyền control defense + settlement (with indemnified party's reasonable cooperation).
  - Settlement không bind indemnified party nếu admit liability mà không có consent.
- **AI-generated content:** AI output is Customer's responsibility post-Approval (per ai-branding-guidelines.md §4.2 Preview before Commit) — Customer indemnify Provider cho claims sau approval.

**Phase 2 TODO:**
- [ ] Legal counsel review indemnification scope vs VN Civil Code 2015 contract indemnity rules
- [ ] Document carve-outs cho consumer Customers (consumer protection law có thể limit indemnity Customer owes)
- [ ] Document legal fees + costs scope (indemnity covers legal fees + judgments + reasonable costs)
- [ ] Document insurance subrogation coordination (if Customer has insurance covering its indemnity)
- [ ] Document threshold cho "promptly notify" (typically 30 days from receipt of claim)

---

## 13. Dispute Resolution

Section này quy định cách giải quyết tranh chấp giữa Provider và Customer (xem cũng Refund/Dispute Policy planned — see GAP-183 Phase 2 deferred to next wave).

- **Negotiation first:** parties cố gắng negotiate good-faith trong X ngày (TODO — typically 30 days) trước khi escalate. Customer contact qua designated email cho disputes.
- **Mediation (optional):** parties có thể đồng thuận mediation qua Vietnam International Arbitration Centre (VIAC) hoặc tổ chức mediation khác.
- **Arbitration / litigation:** nếu negotiation/mediation thất bại, dispute giải quyết qua:
  - **Default:** litigation tại Tòa án nhân dân (TAND) có thẩm quyền theo địa điểm trụ sở Provider tại Vietnam.
  - **Optional ENTERPRISE:** binding arbitration theo VIAC Rules nếu Customer + Provider đồng thuận trong order form.
- **Class action waiver:** parties đồng ý dispute riêng biệt (individual basis), không class action — trong phạm vi pháp luật cho phép. (Note: VN consumer law có thể limit này cho consumer Customers.)
- **Jurisdiction + venue:** TAND có thẩm quyền theo địa điểm trụ sở Provider; venue tại Vietnam.
- **Governing law:** Vietnam law (per §15 Governing Law).
- **Time bar:** any claim must be filed trong thời hiệu áp dụng theo VN Civil Code 2015 (typically 2-3 năm cho contract claims).

**Phase 2 TODO:**
- [ ] Verify class action waiver enforceability vs Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 cho consumer Customers
- [ ] Cross-link Refund/Dispute Policy khi GAP-183 ship
- [ ] Document escalation contacts (email, phone, mailing address) cho dispute notices
- [ ] Document arbitration cost allocation (typically split unless otherwise ordered)
- [ ] Document interim relief carve-out (parties có thể seek injunctive relief từ courts trong khi arbitration pending — IP, confidentiality breaches)

---

## 14. Modifications

Section này quy định cách Provider có thể modify TOS + Customer's options:

- **Right to modify:** Provider reserve right to modify TOS, AUP, Privacy Policy, pricing periodically để reflect product changes, legal updates, market conditions.
- **Notice mechanism:**
  - Material modifications: notice qua email + in-app banner ít nhất X ngày trước effective date (TODO — typically 30 days).
  - Non-material modifications (typo fixes, clarifications, contact info updates): notice qua updated TOS page.
- **Re-acceptance (click-wrap):** material modifications require Customer click-wrap re-accept khi login lần đầu sau effective date. Failure to re-accept trong Y ngày (TODO — typically 30 days) → Customer cannot continue using Service và may be terminated per §9.
- **Customer rejection of changes:** Customer reject material modification có quyền cancel trong notice period với pro-rated refund cho unused subscription period.
- **Continued use = acceptance:** continued use of Service sau effective date của notified modifications (without express objection) constitutes acceptance.
- **Version archive:** Provider duy trì archive của previous TOS versions công khai để Customer reference (TODO — implementation tracked separately as feature gap).
- **Pricing changes:** giá subscription chỉ được tăng cho renewal cycle tiếp theo (không retroactive). Notice period cho price changes typically dài hơn TOS modifications (60-90 days advance notice).

**Phase 2 TODO:**
- [ ] Notice period cho material modifications finalize (30 vs 60 days)
- [ ] Notice period cho price changes finalize (60 vs 90 days)
- [ ] Re-acceptance grace period finalize
- [ ] Document version archive URL pattern (e.g., `/legal/tos/v1`, `/legal/tos/v2`)
- [ ] Document classification "material" vs "non-material" với examples

---

## 15. Entire Agreement + Severability + Governing Law

Section này contains miscellaneous boilerplate provisions:

- **Entire Agreement:** TOS + Privacy Policy + AUP + Data Retention/Deletion Policy + applicable Order Form (for ENTERPRISE) constitute entire agreement giữa parties về subject matter, supersede prior negotiations + agreements (oral hoặc written).
- **Order of precedence:** trong trường hợp xung đột giữa documents, order of precedence: Order Form (specific to Customer) > Master Service Agreement (if any) > TOS > Privacy Policy > AUP > Other policies. Cho non-ENTERPRISE Customers without Order Form: TOS controls primary, sub-policies apply trong scope của họ.
- **Severability:** nếu provision nào của TOS được declared invalid hoặc unenforceable bởi tòa án có thẩm quyền, provisions còn lại vẫn có hiệu lực. Invalid provision sẽ được modified minimum necessary để valid + enforceable + giữ original intent.
- **No waiver:** failure to enforce provision không waive right to enforce later. Waiver phải in writing để có hiệu lực.
- **Assignment:** Customer KHÔNG được assign TOS cho bên thứ ba mà không có Provider's written consent (trừ khi assign theo merger/acquisition của Customer's business). Provider có quyền assign cho affiliate, successor, hoặc trong context of merger/acquisition with notice tới Customer.
- **Force majeure:** neither party liable cho delay/failure performance do force majeure (acts of God, war, terrorism, government action, pandemic, infrastructure provider outage outside reasonable control). Notice + reasonable mitigation required.
- **Notices:** legal notices to Provider qua designated email + registered mail tới registered address. Notices to Customer qua email on file + admin dashboard banner.
- **Headings:** section headings cho convenience only; không affect interpretation.
- **Languages:** TOS available in Vietnamese (controlling) và English (translation for convenience). In case of inconsistency, Vietnamese version controls.
- **Governing law:** TOS được governed bởi Vietnam law, không apply conflicts-of-laws principles. UN Convention on Contracts for International Sale of Goods (CISG) không apply.
- **Jurisdiction:** as per §13 Dispute Resolution — TAND có thẩm quyền tại Vietnam.

**Phase 2 TODO:**
- [ ] Legal counsel finalize order of precedence — particularly cho ENTERPRISE customers với negotiated MSA
- [ ] Document force majeure triggers + notification mechanism
- [ ] Document Provider's registered address + designated legal notice email
- [ ] Confirm Vietnamese version controls (per VN legal practice for VN-jurisdiction contracts)
- [ ] Confirm CISG exclusion necessary (typically applicable cho cross-border B2B sales — không relevant cho domestic SaaS, but include cho safety)
- [ ] Document successor + affiliate definitions for assignment clauses

---

## Cross-references (sibling skeletons + future docs)

**Same-wave Phase 1 skeletons (created 2026-04-29):**
- [acceptable-use-policy.md](acceptable-use-policy.md) — AUP detail, GAP-181
- [privacy-policy.md](privacy-policy.md) — Privacy Policy detail (PDPL mandate), GAP-182
- [data-retention-deletion-policy.md](data-retention-deletion-policy.md) — Retention + Deletion detail, GAP-184

**Deferred next wave (extend Wave 8 Business Governance):**
- Refund/Dispute Resolution Policy (planned — see GAP-183) — referenced in §13
- Billing Terms + VAT/TCT Compliance (planned — see GAP-185) — referenced in §7
- Child Protection Policy (planned — see GAP-186) — referenced in §5

**Other future docs:**
- Customer SLA (planned — see GAP-189) — referenced in §4
- Tenant Offboarding Runbook (planned — `documents/05-guides/tenant-offboarding.md` Phase 2) — referenced in §9
- Incident Response Runbook (planned — see GAP-190) — referenced via Privacy Policy §14

**Existing referenced docs:**
- [pricing-model.md](pricing-model.md) — tier features + pricing
- [personas-catalog.md](personas-catalog.md) — End User role taxonomy
- [compliance-scope.md](compliance-scope.md) — applicable laws per jurisdiction
- [.claude/rules/ai-branding-guidelines.md](../../.claude/rules/ai-branding-guidelines.md) — AI output rules + Quality Gate + Preview-before-Commit
- [.claude/rules/business-logic-review.md](../../.claude/rules/business-logic-review.md) — 5-attribute review for business values

---

## Phase 2 closure checklist (legal counsel sign-off)

Following items must complete before Status flip 🔵 SKELETON → 🟡 PARTIAL → 🟢 APPROVED:

- [ ] Legal counsel engagement secured (depends on GAP-049 process + GAP-156 sign-off framework)
- [ ] Provider entity formalized (tên doanh nghiệp, GPKD, registered address, designated signatory)
- [ ] All Phase 2 TODO markers resolved with content drafted by legal counsel
- [ ] All TODO placeholder values reviewed per business-logic-review.md 5-attribute (Source, Rationale, Reviewer, Compliance check, Review cadence)
- [ ] Cross-document consistency check vs Privacy Policy, AUP, Retention Policy
- [ ] PDPL Decree 13/2023 compliance verified (Privacy Policy linkage)
- [ ] Consumer Protection Law 2023 compliance verified (warranty + liability + dispute clauses)
- [ ] Click-wrap UI implementation feature gap filed + targeted for shipment
- [ ] TOS versioning/history storage feature gap filed + targeted for shipment
- [ ] Legal review complete với written sign-off from PM + CEO + Legal counsel
- [ ] Status flip per `gap-done-discipline.md` rules — only when all AC verified

---

## Log

- **2026-04-29** — Phase 1 skeleton shipped (15 sections + Glossary + Cross-references + Phase 2 closure checklist) per Wave Legal-BRD Phase 1 (GAP-180). All sections include TODO markers cho legal counsel content fill in Phase 2 (GAP-154 umbrella). Frontmatter follows markdown-header style per personas-catalog.md mimicry. Cross-link slots reserved cho 4 sibling skeletons same-wave (AUP/Privacy/Retention) + 3 deferred next-wave (Refund/Billing/Child Protection) + future docs (SLA/Offboarding/Incident Response). Status will flip 🔵 OPEN → 🟡 PARTIAL on closure PR per `gap-done-discipline.md` §3 PARTIAL exit-ramp (NOT DONE — Phase 2 content blocked-on legal counsel engagement).
