import type { Metadata } from 'next';

/**
 * Privacy Policy page — KiteHub.
 *
 * Phase 1 v1 (Wave 23 Bucket F, GAP-368) — Vietnamese-first, EN deferred to GAP-182 Phase 2.
 * Source content: `documents/00-brd/privacy-policy.md` (16 sections per PDPL Decree 13/2023 Art 11).
 * Counsel review pending — see GAP-182/184 Phase 2.
 */
export const metadata: Metadata = {
  title: 'Chính sách quyền riêng tư | KiteHub',
  description:
    'Chính sách quyền riêng tư áp dụng cho dịch vụ KiteHub theo Nghị định 13/2023/NĐ-CP (PDPL) — bảo vệ dữ liệu cá nhân của tenant admin, end-users và khách hàng.',
};

export default function PrivacyPolicyPage() {
  return (
    <div className="container max-w-4xl py-12">
      <article className="space-y-6 text-sm leading-relaxed">
        <aside
          role="note"
          className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
        >
          <p>
            <strong>v1 — đang chờ legal counsel review.</strong> Bản chính thức sẽ
            được cập nhật sau khi luật sư rà soát (theo dõi qua GAP-182/184 Phase 2).
          </p>
          <p className="mt-1 text-xs">
            Cập nhật lần cuối: 2026-05-06 · Hiệu lực: 2026-05-06
          </p>
        </aside>

        <header>
          <h1 className="text-3xl font-bold">Chính sách quyền riêng tư</h1>
          <p className="mt-2 text-muted-foreground">
            Áp dụng cho cả nền tảng KiteHub (SaaS quản lý instance) và KiteClass
            (multi-tenant education delivery). Căn cứ pháp lý: Nghị định
            13/2023/NĐ-CP (PDPL — hiệu lực 2026-07-01) Điều 11, 14, 23; Luật An
            ninh mạng 2018; Luật Giao dịch điện tử 2023.
          </p>
        </header>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            1. Đơn vị Kiểm soát Dữ liệu (Data Controller)
          </h2>
          <p>
            <strong>Mục đích:</strong> Định danh pháp lý của bên thu thập + xử lý
            dữ liệu cá nhân (PDPL Art 11.1.a — quyền được biết của chủ thể).
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>Tên công ty: TODO (Phase 2 — legal entity name khi đăng ký kinh doanh hoàn tất)</li>
            <li>Số đăng ký doanh nghiệp: TODO (Phase 2 — Mã số doanh nghiệp / GP-ĐKKD)</li>
            <li>Địa chỉ trụ sở: TODO (Phase 2)</li>
            <li>Email liên hệ chính thức: TODO (Phase 2 — <code>legal@TODO</code>)</li>
            <li>Người đại diện theo pháp luật: TODO (Phase 2)</li>
          </ul>
          <p className="mt-2">
            <strong>Vai trò pháp lý:</strong> Bên Kiểm soát Dữ liệu (Data
            Controller) đối với dữ liệu của tenant admins, end-users (giáo viên,
            học sinh, phụ huynh, kế toán, staff). Đối với dữ liệu mà tenant đưa
            lên KiteClass thuộc phạm vi quản lý của tenant đó, KiteClass đóng vai
            trò Bên Xử lý Dữ liệu (Data Processor) — chi tiết DPA tracked trong
            GAP-180 TOS.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            2. Data Protection Officer (DPO — Cán bộ Bảo vệ Dữ liệu)
          </h2>
          <p>
            <strong>Mục đích:</strong> Đầu mối liên hệ cho yêu cầu thực thi quyền
            chủ thể dữ liệu + báo cáo sự cố (PDPL Art 28 — DPO mandatory cho tổ
            chức xử lý dữ liệu nhạy cảm hoặc dữ liệu trẻ em).
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>DPO designation: TODO (Phase 2 — bắt buộc do hệ thống xử lý dữ liệu trẻ em K-12 + dữ liệu sức khoẻ trong lý do vắng học)</li>
            <li>Email DPO: TODO (Phase 2 — <code>dpo@TODO</code>)</li>
            <li>Hotline / form yêu cầu chủ thể: TODO (Phase 2 — endpoint web form + email kênh dự phòng)</li>
          </ul>
          <p className="mt-2">
            Trước khi có DPO chính thức (Phase 2), mọi yêu cầu sẽ được chuyển tới{' '}
            <code>legal@TODO</code> và xử lý trong cùng khung SLA 20-30 ngày
            (Art 14).
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            3. Nhóm Chủ thể Dữ liệu (Data Subject Categories)
          </h2>
          <p>
            KiteHub + KiteClass xử lý dữ liệu của các nhóm chủ thể sau (PDPL Art
            11.1.b):
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li><strong>Tenant Admin / Owner</strong> — chủ trung tâm/trường, người sở hữu instance KiteClass.</li>
            <li><strong>Teacher (Giáo viên)</strong> — bao gồm giáo viên fulltime, gia sư bán thời gian, trợ giảng.</li>
            <li><strong>Student (Học sinh / Người học)</strong> — bao gồm cả người trưởng thành (adult learner) và <strong>trẻ em dưới 16 tuổi</strong> (K-12 — xem Mục 12).</li>
            <li><strong>Parent / Guardian (Phụ huynh / Người giám hộ)</strong> — đối với học sinh dưới 16 tuổi.</li>
            <li><strong>Accountant / Cashier (Kế toán / Thu ngân)</strong> — quản lý hoá đơn, học phí.</li>
            <li><strong>Other Staff (Nhân viên hỗ trợ)</strong> — admin office, marketing, lễ tân.</li>
            <li><strong>Visitor / Lead</strong> — người chưa đăng ký nhưng có để lại thông tin trên landing pages.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            4. Loại Dữ liệu Xử lý (Data Categories Processed)
          </h2>

          <h3 className="text-lg font-semibold mt-4 mb-2">4.1 Identification (Định danh)</h3>
          <p>
            Họ tên, ngày sinh, giới tính, số CCCD/CMND/hộ chiếu (chỉ thu thập cho
            Tenant Owner phục vụ xuất hoá đơn — PDPL Art 11.1.c).
          </p>

          <h3 className="text-lg font-semibold mt-4 mb-2">4.2 Contact (Liên hệ)</h3>
          <p>
            Email, số điện thoại, địa chỉ thường trú/tạm trú, địa chỉ cha mẹ (đối
            với học sinh dưới 16 tuổi).
          </p>

          <h3 className="text-lg font-semibold mt-4 mb-2">4.3 Educational (Giáo dục)</h3>
          <p>
            Điểm số, điểm danh, bài tập, đánh giá hạnh kiểm, tiến độ học tập,
            lịch học. Với học sinh K-12, đây là dữ liệu của <strong>trẻ em</strong>{' '}
            — áp dụng các bảo vệ bổ sung tại Mục 12.
          </p>

          <h3 className="text-lg font-semibold mt-4 mb-2">4.4 Financial (Tài chính)</h3>
          <p>
            Thông tin thanh toán (token cổng thanh toán — KiteHub/KiteClass KHÔNG
            lưu số thẻ thật), hoá đơn, lịch sử giao dịch, nợ học phí. Theo PDPL
            Art 3.4 — dữ liệu giao dịch tài chính = <strong>dữ liệu nhạy cảm</strong>.
          </p>

          <h3 className="text-lg font-semibold mt-4 mb-2">4.5 Technical (Kỹ thuật)</h3>
          <p>
            IP address, user-agent, device fingerprint, session id, cookies (xem
            Mục 15), log truy cập, audit trail.
          </p>

          <h3 className="text-lg font-semibold mt-4 mb-2">4.6 Sensitive (Nhạy cảm — special handling)</h3>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Sức khoẻ:</strong> lý do vắng học có thể chứa thông tin sức khoẻ (PDPL Art 3.4).</li>
            <li><strong>Trẻ em dưới 16 tuổi (K-12):</strong> toàn bộ dữ liệu được coi là nhạy cảm (PDPL Art 20).</li>
            <li><strong>Dữ liệu tài chính</strong> (mục 4.4 ở trên).</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            5. Mục đích Xử lý (Processing Purposes)
          </h2>
          <ol className="list-decimal space-y-1 pl-5">
            <li><strong>Education delivery:</strong> quản lý lớp học, điểm danh, giao bài, chấm điểm, tương tác giáo viên – học sinh – phụ huynh.</li>
            <li><strong>Billing &amp; subscription:</strong> xuất hoá đơn theo Nghị định 123/2020/NĐ-CP, quản lý gói KiteHub, thu học phí KiteClass.</li>
            <li><strong>Customer support:</strong> xử lý ticket, troubleshoot bug, hướng dẫn sử dụng.</li>
            <li><strong>Analytics:</strong> thống kê tình trạng sử dụng, KPI tenant, tổng hợp ẩn danh phục vụ cải tiến sản phẩm.</li>
            <li><strong>AI features:</strong> AI Branding (logo analysis, banner generation — local Ollama mặc định), trợ lý giáo viên.</li>
            <li><strong>Legal compliance:</strong> lưu hoá đơn 10 năm theo Luật Quản lý Thuế, báo cáo MoET nếu được yêu cầu.</li>
            <li><strong>Security &amp; fraud prevention:</strong> phát hiện đăng nhập bất thường, audit log.</li>
          </ol>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            6. Căn cứ Pháp lý (Legal Basis)
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b">
                  <th className="py-2 pr-4">Purpose</th>
                  <th className="py-2 pr-4">Căn cứ pháp lý chính</th>
                  <th className="py-2">Ghi chú</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b"><td className="py-2 pr-4">Education delivery</td><td className="py-2 pr-4">Hợp đồng (TOS)</td><td className="py-2">Bắt buộc — không thể opt-out</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Billing &amp; subscription</td><td className="py-2 pr-4">Hợp đồng + Nghĩa vụ pháp lý (thuế)</td><td className="py-2">Hoá đơn không opt-out được</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Customer support</td><td className="py-2 pr-4">Lợi ích chính đáng</td><td className="py-2">Có thể từ chối kênh không thiết yếu</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Analytics (aggregated)</td><td className="py-2 pr-4">Lợi ích chính đáng (Art 17.1.đ)</td><td className="py-2">Đã ẩn danh / tổng hợp</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">AI features</td><td className="py-2 pr-4">Sự đồng ý rõ ràng (opt-in)</td><td className="py-2">Logo analysis local mặc định</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Marketing communications</td><td className="py-2 pr-4">Sự đồng ý rõ ràng</td><td className="py-2">Có thể rút bất kỳ lúc nào</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Legal compliance</td><td className="py-2 pr-4">Nghĩa vụ pháp lý</td><td className="py-2">Không opt-out được</td></tr>
                <tr><td className="py-2 pr-4">Security &amp; audit</td><td className="py-2 pr-4">Lợi ích chính đáng</td><td className="py-2">Cần thiết để vận hành an toàn</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            7. Chia sẻ Dữ liệu với Bên Thứ ba (Data Sharing)
          </h2>
          <p>
            Chúng tôi <strong>KHÔNG bán</strong> dữ liệu cá nhân của bất kỳ chủ
            thể nào (PDPL Art 11.1.e). Dữ liệu chỉ được chia sẻ với các nhóm
            sau, theo nguyên tắc tối thiểu hoá:
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li><strong>Cổng thanh toán:</strong> VNPay, MoMo (xử lý token thanh toán — KiteClass không nhìn thấy số thẻ).</li>
            <li><strong>Truyền thông &amp; OTP:</strong> Zalo OA, nhà cung cấp SMS (TODO Phase 2), email transactional.</li>
            <li><strong>Năng suất &amp; văn phòng:</strong> Google Workspace (chỉ khi tenant chủ động kết nối).</li>
            <li><strong>Hosting &amp; hạ tầng:</strong> AWS / Oracle Cloud (data residency Vietnam ưu tiên — xem Mục 8).</li>
            <li><strong>Cơ quan nhà nước:</strong> A05 (Cục An ninh mạng), Tổng cục Thuế, Bộ Giáo dục &amp; Đào tạo — chỉ khi có yêu cầu pháp lý hợp lệ.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            8. Chuyển Dữ liệu Xuyên Biên giới (Cross-Border Transfer)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Mặc định:</strong> dữ liệu lưu trữ tại Việt Nam.</li>
            <li><strong>AI Branding (Ollama local):</strong> KHÔNG có cross-border transfer.</li>
            <li><strong>AI Branding (OpenAI quốc tế):</strong> chỉ khi tenant Enterprise opt-in với disclaimer rõ ràng.</li>
            <li><strong>Trẻ em (K-12):</strong> dữ liệu KHÔNG được transfer cross-border bất kể tier subscription.</li>
            <li><strong>Cross-Border Transfer Impact Assessment:</strong> TODO Phase 2 — bắt buộc theo PDPL Art 25.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            9. Thời hạn Lưu trữ (Retention Period)
          </h2>
          <p>
            Bảng dưới là <strong>placeholder Phase 1</strong>. Phase 2 (GAP-184)
            sẽ chốt giá trị + ký kết với DPO + Legal counsel.
          </p>
          <div className="mt-2 overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b">
                  <th className="py-2 pr-4">Data category</th>
                  <th className="py-2 pr-4">Retention</th>
                  <th className="py-2">Pháp lý chi phối</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b"><td className="py-2 pr-4">Identification</td><td className="py-2 pr-4">TODO</td><td className="py-2">PDPL Art 16 + Civil Code</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Contact</td><td className="py-2 pr-4">TODO</td><td className="py-2">PDPL Art 16</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Educational</td><td className="py-2 pr-4">TODO</td><td className="py-2">Luật Giáo dục + PDPL</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Financial / Invoices</td><td className="py-2 pr-4">10 năm</td><td className="py-2">Luật Quản lý Thuế 2019 + Nghị định 123/2020</td></tr>
                <tr className="border-b"><td className="py-2 pr-4">Technical (logs)</td><td className="py-2 pr-4">12-24 tháng</td><td className="py-2">PDPL + Cybersecurity Law</td></tr>
                <tr><td className="py-2 pr-4">Sensitive (sức khoẻ, K-12)</td><td className="py-2 pr-4">TODO</td><td className="py-2">PDPL Art 20</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            10. Quyền của Chủ thể Dữ liệu (PDPL Art 9-15)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Quyền được biết</strong> (Art 9): Đọc Privacy Policy này + dashboard &quot;Quyền của tôi&quot;. SLA: tức thời.</li>
            <li><strong>Quyền truy cập</strong> (Art 10): Tự xuất từ dashboard hoặc gửi yêu cầu DPO. SLA: 20 ngày (gia hạn tối đa 10 ngày).</li>
            <li><strong>Quyền chỉnh sửa</strong> (Art 11): Self-edit profile hoặc gửi DPO. SLA: 20 ngày.</li>
            <li><strong>Quyền xoá</strong> (Art 12): Gửi DPO + xác minh danh tính. SLA: 20 ngày. Có ngoại lệ legal-hold (hoá đơn, audit log).</li>
            <li><strong>Quyền hạn chế xử lý</strong> (Art 13): Gửi DPO. SLA: 20 ngày.</li>
            <li><strong>Quyền phản đối</strong> (Art 14): Marketing — opt-out tức thời.</li>
            <li><strong>Quyền chuyển dữ liệu</strong> (Art 15): Self-export (planned — GAP-188).</li>
            <li><strong>Quyền khiếu nại:</strong> Gửi A05 (Cục An ninh mạng).</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            11. Thực thi Quyền (Channel + SLA)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Kênh chính:</strong> form web <code>https://TODO/privacy-request</code> (Phase 2).</li>
            <li><strong>Kênh email:</strong> <code>dpo@TODO</code> (Phase 2).</li>
            <li><strong>Xác minh danh tính:</strong> OTP qua email/SĐT đã đăng ký + (đối với yêu cầu erasure full account) ID document review.</li>
            <li><strong>SLA phản hồi:</strong> 20 ngày làm việc (gia hạn tối đa 10 ngày bổ sung — PDPL Art 14).</li>
            <li><strong>Phí:</strong> miễn phí cho yêu cầu hợp lý.</li>
            <li><strong>Từ chối:</strong> phải kèm lý do bằng văn bản + chỉ dẫn quyền khiếu nại lên A05.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            12. Trẻ em dưới 16 tuổi (Minor Data — PDPL Art 20)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Định nghĩa:</strong> Trẻ em &lt; 16 tuổi (Bộ luật Dân sự 2015 Art 21).</li>
            <li><strong>Sự đồng ý:</strong> mọi xử lý dữ liệu của trẻ em yêu cầu sự đồng ý của cha mẹ / người giám hộ hợp pháp (PDPL Art 20.2).</li>
            <li><strong>Cơ chế parental consent:</strong> TODO Phase 2 (GAP-186).</li>
            <li><strong>Hạn chế:</strong> không gửi marketing trực tiếp tới trẻ em; AI features high-risk không khả dụng cho học sinh K-12.</li>
            <li><strong>Cross-border:</strong> dữ liệu trẻ em KHÔNG transfer ra khỏi Việt Nam.</li>
            <li><strong>Khi đủ 16 tuổi:</strong> chuyển giao quyền kiểm soát từ phụ huynh sang học sinh (TODO Phase 2).</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            13. Biện pháp Bảo mật (Security Measures)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Mã hoá khi truyền:</strong> TLS 1.3 cho mọi endpoint web/API.</li>
            <li><strong>Mã hoá khi lưu:</strong> AES-256 cho database + object storage.</li>
            <li><strong>Phân quyền (RBAC):</strong> least privilege per multi-tenant boundary.</li>
            <li><strong>Audit logs:</strong> truy cập dữ liệu nhạy cảm được ghi lại + lưu tối thiểu 24 tháng.</li>
            <li><strong>Tách biệt tenant:</strong> DB-level isolation.</li>
            <li><strong>Backups:</strong> mã hoá + test restore định kỳ.</li>
            <li><strong>Pen-testing:</strong> Dependabot + security audit /100 quarterly.</li>
            <li><strong>Đào tạo nhân sự:</strong> TODO Phase 2 — annual privacy/security training.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            14. Thông báo Sự cố (Breach Notification — PDPL Art 23)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>SLA thông báo nội bộ:</strong> sự cố nghi ngờ phải báo DPO trong 24 giờ.</li>
            <li><strong>SLA thông báo cơ quan (A05):</strong> 72 giờ kể từ thời điểm xác định &quot;data breach&quot;.</li>
            <li><strong>SLA thông báo chủ thể dữ liệu:</strong> kiến nghị 72h.</li>
            <li><strong>Nội dung thông báo:</strong> mô tả sự cố, dữ liệu bị ảnh hưởng, biện pháp đã thực hiện, contact DPO.</li>
            <li><strong>Tập huấn:</strong> quarterly tabletop exercise (TODO Phase 2).</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            15. Chính sách Cookie (Cookie Policy)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Cookies cần thiết:</strong> session id, CSRF token, language preference — KHÔNG yêu cầu consent.</li>
            <li><strong>Cookies analytics:</strong> chỉ enable sau khi user opt-in trên cookie banner. Mặc định tắt cho người dùng EU/EEA + người chưa thành niên.</li>
            <li><strong>Cookies third-party tracking:</strong> TẮT mặc định. KiteHub không gắn pixel quảng cáo trên app chính.</li>
            <li><strong>Cookie banner UI:</strong> implementation tracked trong GAP-353 (Wave 23 ConsentBanner).</li>
            <li><strong>Thời hạn cookie:</strong> session cookies tắt khi đóng browser; persistent cookies tối đa 12 tháng.</li>
            <li><strong>Quản lý cookie:</strong> chi tiết tại <a href="/legal/cookies" className="underline hover:text-primary">Chính sách Cookie</a>.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">
            16. Thay đổi Chính sách (Changes to Policy)
          </h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Phiên bản hiện tại:</strong> Phase 1 SKELETON 2026-05-06.</li>
            <li><strong>Material changes:</strong> thay đổi categories of data, purposes, third parties, cross-border transfer, retention period — yêu cầu thông báo trước 30 ngày qua email + in-app notification.</li>
            <li><strong>Non-material changes:</strong> publish trực tiếp + ghi changelog cuối doc.</li>
            <li><strong>Re-consent:</strong> yêu cầu re-consent rõ ràng nếu thay đổi căn cứ pháp lý hoặc thêm xử lý dữ liệu nhạy cảm.</li>
            <li><strong>Ngôn ngữ:</strong> Tiếng Việt là bản gốc (canonical). Trong trường hợp khác biệt diễn giải, bản tiếng Việt prevail.</li>
          </ul>
        </section>

        <hr className="my-8" />

        <nav aria-label="Trang pháp lý liên quan">
          <h2 className="text-lg font-semibold mb-2">Trang pháp lý liên quan</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><a href="/legal/terms" className="underline hover:text-primary">Điều khoản dịch vụ</a></li>
            <li><a href="/legal/cookies" className="underline hover:text-primary">Chính sách Cookie</a></li>
            <li><a href="/legal/dmca" className="underline hover:text-primary">DMCA Takedown Notice</a></li>
          </ul>
        </nav>
      </article>
    </div>
  );
}
