import type { Metadata } from 'next';

/**
 * Terms of Service page — KiteHub.
 *
 * Phase 1 v1 (Wave 23 Bucket F, GAP-368) — Vietnamese-first, EN deferred to GAP-182 Phase 2.
 * Source content: `documents/00-brd/terms-of-service.md` (15 sections, click-wrap acceptance).
 * Counsel review pending — see GAP-180/154 Phase 2.
 */
export const metadata: Metadata = {
  title: 'Điều khoản dịch vụ | KiteHub',
  description:
    'Điều khoản dịch vụ áp dụng cho KiteHub (SaaS quản lý) và KiteClass (multi-tenant education). Căn cứ Bộ luật Dân sự 2015, Luật Giao dịch điện tử 2023, Luật Bảo vệ Quyền lợi Người tiêu dùng 2023.',
};

export default function TermsOfServicePage() {
  return (
    <div className="container max-w-4xl py-12">
      <article className="space-y-6 text-sm leading-relaxed">
        <aside
          role="note"
          className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
        >
          <p>
            <strong>v1 — đang chờ legal counsel review.</strong> Bản chính thức
            sẽ được cập nhật sau khi luật sư rà soát (theo dõi qua GAP-180 / GAP-154 Phase 2).
          </p>
          <p className="mt-1 text-xs">
            Cập nhật lần cuối: 2026-05-06 · Hiệu lực: 2026-05-06
          </p>
        </aside>

        <header>
          <h1 className="text-3xl font-bold">Điều khoản dịch vụ</h1>
          <p className="mt-2 text-muted-foreground">
            Áp dụng giữa Provider (đơn vị vận hành KiteHub/KiteClass) và Customer
            (tenant đăng ký dịch vụ). Căn cứ pháp lý: Bộ luật Dân sự 2015, Luật
            Giao dịch điện tử 2023, Luật Bảo vệ Quyền lợi Người tiêu dùng 2023.
            Acceptance mechanism: Click-wrap tại signup, re-accept khi
            modification.
          </p>
        </header>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">Glossary — Defined Terms</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>&quot;Provider&quot;</strong> — đơn vị pháp nhân vận hành nền tảng KiteHub (SaaS quản lý) và KiteClass (multi-tenant education). [Phase 2 — TODO legal entity].</li>
            <li><strong>&quot;Customer&quot;</strong> (hoặc &quot;Tenant&quot;) — chủ thể đăng ký subscription cho Service: trung tâm giáo dục, trường học, gia sư tự do.</li>
            <li><strong>&quot;End User&quot;</strong> — người dùng cuối truy cập Service qua tenant của Customer: teacher, student, parent, accountant, admin.</li>
            <li><strong>&quot;Service&quot;</strong> — toàn bộ phần mềm + hạ tầng + tính năng + AI Branding + storage + support do Provider cung cấp.</li>
            <li><strong>&quot;Content&quot;</strong> — bất kỳ data, văn bản, hình ảnh, file đính kèm, AI-generated assets, mà Customer hoặc End Users upload/tạo/lưu trên Service.</li>
            <li><strong>&quot;Confidential Information&quot;</strong> — non-public business information mỗi bên disclose cho bên kia trong quá trình thực thi TOS.</li>
            <li><strong>&quot;Effective Date&quot;</strong> — ngày Customer click-wrap accept TOS lần đầu, hoặc ngày accept revised TOS.</li>
            <li><strong>&quot;Term&quot;</strong> — khoảng thời gian TOS có hiệu lực, từ Effective Date cho đến termination per Mục 9.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">1. Parties + Definitions</h2>
          <p>
            Điều khoản này xác định các bên ký kết: Provider một bên, Customer
            (tenant) bên kia. Quan hệ pháp lý 3-tier (Provider ↔ Customer ↔ End
            Users) — End Users truy cập Service qua tenant của Customer chứ
            không trực tiếp ký TOS với Provider; Customer đại diện cho End Users
            trong phạm vi instance.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">2. Service Description</h2>
          <p>
            Service bao gồm: (a) core platform features (KiteHub instance
            management; KiteClass education business — student/course/class/
            attendance/grade/payment), (b) AI Branding subsystem, (c) shared
            infrastructure access, (d) tier-specific features và quotas (FREE /
            PRO / PREMIUM / ENTERPRISE), (e) support tiers, (f) explicit
            exclusions: không bao gồm hosting domain riêng của Customer ngoài
            subdomain Provider cấp, không bao gồm legal advice, không bao gồm
            data migration từ legacy systems trừ khi mua add-on.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">3. Customer Obligations</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Content responsibility:</strong> Customer chịu trách nhiệm pháp lý cho mọi Content do Customer hoặc End Users upload/tạo.</li>
            <li><strong>Account security:</strong> Customer bảo mật credentials, không share cho bên thứ ba ngoài role-based access.</li>
            <li><strong>Lawful use:</strong> tuân thủ Acceptable Use Policy, không vi phạm pháp luật Việt Nam (Luật An ninh mạng, Sở hữu trí tuệ, Bảo vệ Trẻ em).</li>
            <li><strong>Data accuracy:</strong> đảm bảo data Customer cung cấp chính xác + cập nhật trong vòng 30 ngày khi có thay đổi.</li>
            <li><strong>Payment timeliness:</strong> thanh toán đúng hạn theo Payment Terms (Mục 7).</li>
            <li><strong>End User governance:</strong> Customer chịu trách nhiệm enforce Acceptable Use cho End Users của tenant mình.</li>
            <li><strong>Compliance với PDPL:</strong> Customer tự xác định vai trò Controller hay Processor và tuân thủ obligations tương ứng.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">4. Provider Obligations</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Service availability:</strong> Provider commit uptime % per tier theo SLA.</li>
            <li><strong>Support:</strong> theo tier — FREE community/docs only; PRO email business hours; PREMIUM email + chat; ENTERPRISE priority + dedicated CSM.</li>
            <li><strong>Data security:</strong> encryption at rest + in transit, access controls, audit logging, periodic security audits.</li>
            <li><strong>Data confidentiality:</strong> Provider không sell, rent, hoặc share Customer data ngoài subprocessors necessary.</li>
            <li><strong>Notice of changes:</strong> Provider notice Customer trước modification material đối với TOS, Privacy Policy, hoặc tier features.</li>
            <li><strong>Data export on termination:</strong> Provider cung cấp data export tools trong reasonable timeframe sau termination.</li>
            <li><strong>Lawful operation:</strong> Provider tuân thủ pháp luật VN.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">5. Acceptable Use</h2>
          <p>Tóm tắt high-level (Acceptable Use Policy chi tiết tracked riêng):</p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li><strong>Prohibited content:</strong> illegal content, CSAM (zero-tolerance), hate speech, adult content, copyrighted material không có quyền, misinformation gây hại.</li>
            <li><strong>Prohibited conduct:</strong> account sharing ngoài quy định, bot/automation không authorized, scraping, reverse engineering, spam, attacks.</li>
            <li><strong>Education-specific prohibitions:</strong> academic fraud, leaked exams, teacher impersonation, predatory behavior toward minors.</li>
            <li><strong>Enforcement:</strong> Provider có quyền warn → suspend → terminate theo strike system + appeal flow.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">6. Intellectual Property</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Customer Data Ownership:</strong> Customer giữ toàn bộ quyền sở hữu Content. Provider chỉ có license giới hạn để host, process, backup, deliver Service.</li>
            <li><strong>Provider IP:</strong> Provider giữ toàn bộ IP của KiteHub/KiteClass platform, source code, design templates, trademarks, AI model weights.</li>
            <li><strong>AI-Generated Output Ownership:</strong> Customer được license sử dụng AI output cho instance của họ; Provider giữ background IP của model + prompts.</li>
            <li><strong>Feedback License:</strong> Provider có irrevocable license sử dụng feedback để improve Service.</li>
            <li><strong>Trademarks:</strong> Customer không được sử dụng trademark &quot;KiteHub&quot;, &quot;KiteClass&quot; cho marketing nếu chưa có written permission.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">7. Payment Terms</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Subscription tiers:</strong> FREE / PRO / PREMIUM / ENTERPRISE. FREE không có payment obligation.</li>
            <li><strong>Billing cycle:</strong> monthly hoặc annual prepay (annual discount).</li>
            <li><strong>Payment methods:</strong> VNPay, MoMo, Zalo Pay, bank transfer (domestic), credit card qua VNPay/Stripe gateway.</li>
            <li><strong>VAT:</strong> giá listed có thể chưa bao gồm VAT 10% — Customer có VN GPKD được phát hành hóa đơn điện tử theo Nghị định 123/2020/NĐ-CP.</li>
            <li><strong>Late payment:</strong> subscription auto-suspend sau X ngày overdue. Suspension không terminate TOS — data preserved trong grace period.</li>
            <li><strong>Refund policy:</strong> high-level money-back guarantee window TODO (typically 14-30 days post first paid period).</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">8. Confidentiality + Data Protection</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Mutual confidentiality:</strong> Provider và Customer phải bảo vệ Confidential Information với reasonable care.</li>
            <li><strong>Personal Data:</strong> xử lý theo PDPL Decree 13/2023/NĐ-CP và Privacy Policy — xem <a href="/legal/privacy" className="underline hover:text-primary">Chính sách quyền riêng tư</a>.</li>
            <li><strong>Data Processing Agreement (DPA):</strong> Customer cần DPA addendum (Phase 2 deliverable).</li>
            <li><strong>Subprocessor management:</strong> Provider duy trì list subprocessors công khai; notify Customer trước khi thay đổi.</li>
            <li><strong>Audit rights:</strong> Customer ENTERPRISE có quyền request annual security audit summary.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">9. Term + Termination</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Term:</strong> TOS bắt đầu vào Effective Date và tiếp diễn cho đến khi terminate.</li>
            <li><strong>Customer termination for convenience:</strong> Customer có thể cancel bất kỳ lúc nào qua admin dashboard hoặc email; effective vào cuối billing cycle.</li>
            <li><strong>Provider termination for cause:</strong> Provider có thể suspend/terminate ngay nếu Customer vi phạm material TOS, AUP, hoặc Payment Terms (sau notice + cure period).</li>
            <li><strong>Provider termination for convenience:</strong> Provider có thể terminate với notice 30-90 days advance + pro-rated refund.</li>
            <li><strong>Effect of termination:</strong> Service access suspend; Customer có data export window 30-90 days; sau đó data delete per Data Retention Policy.</li>
            <li><strong>Survival:</strong> sections survive termination: §6 IP, §8 Confidentiality, §10-11 Warranties + Liability, §12 Indemnification, §13 Dispute Resolution, §15 Governing Law.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">10. Warranties + Disclaimers</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Provider warranties:</strong> Service được cung cấp với reasonable care + skill (industry standard SaaS); Provider có quyền pháp lý cung cấp Service; tuân thủ pháp luật VN.</li>
            <li><strong>AS-IS / AS-AVAILABLE disclaimer:</strong> ngoài warranties express, Service AS-IS — Provider DISCLAIM tất cả implied warranties trong phạm vi pháp luật cho phép.</li>
            <li><strong>AI Output disclaimer:</strong> AI-Generated Content produce bởi machine learning — Provider không warrant accuracy, originality, hoặc legal compliance. Customer phải review trước khi publish.</li>
            <li><strong>Third-party services:</strong> Provider không warrant third-party service availability hoặc quality.</li>
            <li><strong>Vietnamese consumer law preservation:</strong> disclaimers KHÔNG limit Customer&apos;s rights theo Luật Bảo vệ Quyền lợi Người tiêu dùng 2023.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">11. Limitation of Liability</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Cap on damages:</strong> total liability của Provider cap tại tổng amount Customer trả Provider trong 12 tháng trước claim (TODO finalize).</li>
            <li><strong>Excluded damages:</strong> mỗi bên loại trừ liability cho indirect, consequential, special, punitive, exemplary damages; lost profits, lost business, lost data (trừ gross negligence).</li>
            <li><strong>Exceptions to cap:</strong> cap KHÔNG apply cho payment obligations, IP indemnity, Confidentiality breach, gross negligence/willful misconduct, unwaivable consumer rights.</li>
            <li><strong>Vietnamese law preservation:</strong> KHÔNG limit liability cho personal injury hoặc property damage do gross negligence — non-waivable per VN Civil Code 2015 §584-587.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">12. Indemnification</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Customer indemnifies Provider:</strong> hold harmless khỏi claims liên quan tới Content do Customer/End Users upload, misuse vi phạm AUP, failure to comply với laws, breach TOS material.</li>
            <li><strong>Provider indemnifies Customer:</strong> hold harmless khỏi claims về Provider&apos;s IP infringement, gross negligence security breach, breach of Confidentiality.</li>
            <li><strong>Procedure:</strong> indemnified party notify indemnifying party promptly; indemnifying party có quyền control defense + settlement.</li>
            <li><strong>AI-generated content:</strong> AI output is Customer&apos;s responsibility post-Approval; Customer indemnify Provider cho claims sau approval.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">13. Dispute Resolution</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Negotiation first:</strong> parties cố gắng negotiate good-faith trong 30 ngày trước khi escalate.</li>
            <li><strong>Mediation (optional):</strong> qua Vietnam International Arbitration Centre (VIAC) hoặc tổ chức mediation khác.</li>
            <li><strong>Default litigation:</strong> tại Tòa án nhân dân (TAND) có thẩm quyền theo địa điểm trụ sở Provider tại Vietnam.</li>
            <li><strong>Optional ENTERPRISE arbitration:</strong> binding arbitration theo VIAC Rules nếu cả 2 bên đồng thuận.</li>
            <li><strong>Class action waiver:</strong> dispute riêng biệt (individual basis) — trong phạm vi pháp luật cho phép.</li>
            <li><strong>Time bar:</strong> claim phải filed trong thời hiệu áp dụng theo VN Civil Code 2015 (2-3 năm cho contract claims).</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">14. Modifications</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Right to modify:</strong> Provider reserve right modify TOS, AUP, Privacy Policy, pricing periodically.</li>
            <li><strong>Material modifications notice:</strong> email + in-app banner ít nhất 30 ngày trước effective date.</li>
            <li><strong>Re-acceptance:</strong> material modifications require Customer click-wrap re-accept khi login. Failure to re-accept trong 30 ngày → terminated per Mục 9.</li>
            <li><strong>Customer rejection:</strong> Customer có quyền cancel trong notice period với pro-rated refund.</li>
            <li><strong>Continued use = acceptance:</strong> sau effective date.</li>
            <li><strong>Pricing changes:</strong> chỉ tăng cho renewal cycle tiếp theo (60-90 days advance notice).</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            15. Entire Agreement + Severability + Governing Law
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Entire Agreement:</strong> TOS + Privacy Policy + AUP + Data Retention Policy + Order Form (ENTERPRISE) constitute entire agreement.</li>
            <li><strong>Severability:</strong> nếu provision invalid, provisions còn lại vẫn có hiệu lực.</li>
            <li><strong>No waiver:</strong> waiver phải in writing.</li>
            <li><strong>Assignment:</strong> Customer KHÔNG được assign mà không có Provider&apos;s written consent (trừ merger/acquisition); Provider có quyền assign cho affiliate, successor.</li>
            <li><strong>Force majeure:</strong> neither party liable cho delay/failure do force majeure.</li>
            <li><strong>Languages:</strong> TOS available in Vietnamese (controlling) và English (translation for convenience). Vietnamese version controls in case of inconsistency.</li>
            <li><strong>Governing law:</strong> Vietnam law. CISG không apply.</li>
            <li><strong>Jurisdiction:</strong> TAND có thẩm quyền tại Vietnam (per Mục 13).</li>
          </ul>
        </section>

        <hr className="my-8" />

        <nav aria-label="Trang pháp lý liên quan">
          <h2 className="text-lg font-semibold mb-2">Trang pháp lý liên quan</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><a href="/legal/cookies" className="underline hover:text-primary">Chính sách Cookie</a></li>
            <li><a href="/legal/privacy" className="underline hover:text-primary">Chính sách quyền riêng tư</a></li>
            <li><a href="/legal/dmca" className="underline hover:text-primary">DMCA Takedown Notice</a></li>
          </ul>
        </nav>
      </article>
    </div>
  );
}
