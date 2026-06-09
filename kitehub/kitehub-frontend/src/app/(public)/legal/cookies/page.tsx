import type { Metadata } from 'next';
import { OpenConsentBannerButton } from '@/components/legal/OpenConsentBannerButton';

/**
 * Cookie Policy page — KiteHub.
 *
 * Phase 1 v1 (Wave 23 Bucket F, GAP-368) — Vietnamese-first, EN deferred to GAP-182 Phase 2.
 * Source content: `documents/00-brd/cookie-policy.md` (8 sections per PDPL Decree 13/2023 Art 11 + 13(3)).
 * Companion to Privacy Policy §15. Counsel review pending — see GAP-182 Phase 2.
 *
 * Wave 86 GAP-585 — §6.1 item 2 wired to OpenConsentBannerButton để close
 * PDPL Decree 13/2023 Art 12 withdraw mechanism gap (audit flagged button
 * documented but not rendered).
 */
export const metadata: Metadata = {
  title: 'Chính sách Cookie | KiteHub',
  description:
    'Chính sách Cookie của KiteHub — phân loại cookie (essential / analytics / marketing), retention period, LocalStorage usage, và quy trình rút lại đồng ý theo Nghị định 13/2023/NĐ-CP.',
};

export default function CookiePolicyPage() {
  return (
    <div className="container max-w-4xl py-12">
      <article className="space-y-6 text-sm leading-relaxed">
        <aside
          role="note"
          className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
        >
          <p>
            <strong>v1 — đang chờ legal counsel review.</strong> Bản chính thức
            sẽ được cập nhật sau khi luật sư rà soát + audit cookie thực tế trên
            production (theo dõi qua GAP-182 Phase 2).
          </p>
          <p className="mt-1 text-xs">
            Cập nhật lần cuối: 2026-05-06 · Hiệu lực: 2026-05-06 · Phiên bản:
            v1
          </p>
        </aside>

        <header>
          <h1 className="text-3xl font-bold">Chính sách Cookie</h1>
          <p className="mt-2 text-muted-foreground">
            Companion document cho{' '}
            <a href="/legal/privacy" className="underline hover:text-primary">
              Chính sách quyền riêng tư §15
            </a>
            . Căn cứ pháp lý: Nghị định 13/2023/NĐ-CP (PDPL — hiệu lực 2026-07-01)
            Điều 11, Điều 13(3); Luật An ninh mạng 2018; thông lệ quốc tế GDPR
            ePrivacy.
          </p>
        </header>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">1. Mục đích + Phạm vi (Purpose + Scope)</h2>
          <p>
            <strong>Mục đích:</strong> Tuân thủ PDPL Decree 13/2023 Art 11
            (right-to-information) + Art 13(3) (purpose-specific consent) đối
            với cookies + LocalStorage. Đảm bảo người dùng:
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>Biết những cookie nào đang chạy + mục đích từng cookie</li>
            <li>Có quyền opt-in / opt-out (trừ cookies cần thiết)</li>
            <li>Có quyền rút lại đồng ý bất kỳ lúc nào với cùng mức độ dễ dàng như khi đồng ý</li>
          </ul>
          <p className="mt-3"><strong>Phạm vi áp dụng:</strong></p>
          <ul className="list-disc space-y-1 pl-5">
            <li>Public marketing surfaces: trang chủ, blog, pricing, catalog, contact, about, legal pages</li>
            <li>Authenticated dashboards: admin / teacher / parent / student / accountant</li>
            <li>Subdomain tenant (KiteClass instances)</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">2. Phân loại Cookie (Cookie Categories)</h2>

          <h3 className="text-lg font-semibold mt-4 mb-2">
            2.1 Essential cookies (Cookies cần thiết — KHÔNG cần consent)
          </h3>
          <p>
            <strong>Căn cứ pháp lý:</strong> Lợi ích chính đáng (PDPL Art
            17.1.đ).
          </p>
          <div className="mt-2 overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b">
                  <th className="py-2 pr-4">Cookie / Storage key</th>
                  <th className="py-2 pr-4">Mục đích</th>
                  <th className="py-2">Retention</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b"><td className="py-2 pr-4"><code>session_id</code></td><td className="py-2 pr-4">Duy trì phiên đăng nhập</td><td className="py-2">Session</td></tr>
                <tr className="border-b"><td className="py-2 pr-4"><code>csrf_token</code></td><td className="py-2 pr-4">Chống tấn công CSRF</td><td className="py-2">Session</td></tr>
                <tr className="border-b"><td className="py-2 pr-4"><code>lang</code></td><td className="py-2 pr-4">Lưu ngôn ngữ ưa thích</td><td className="py-2">12 tháng</td></tr>
                <tr className="border-b"><td className="py-2 pr-4"><code>kite-consent</code> (LocalStorage)</td><td className="py-2 pr-4">Lưu lựa chọn consent của user</td><td className="py-2">12 tháng (re-prompt sau)</td></tr>
                <tr><td className="py-2 pr-4"><code>kite-tenant-id</code></td><td className="py-2 pr-4">Định danh tenant cho subdomain routing</td><td className="py-2">Session</td></tr>
              </tbody>
            </table>
          </div>
          <p className="mt-2">
            User KHÔNG thể tắt essential cookies — nếu chặn qua browser, dịch vụ
            sẽ không hoạt động đúng.
          </p>

          <h3 className="text-lg font-semibold mt-6 mb-2">
            2.2 Analytics cookies (Cookies phân tích — yêu cầu consent opt-in)
          </h3>
          <p>
            <strong>Căn cứ pháp lý:</strong> Sự đồng ý rõ ràng (PDPL Art 11 +
            Art 13(3)).
          </p>
          <p className="mt-2">
            <strong>Phase 1:</strong> KHÔNG có analytics cookies thực tế. Phase
            2 sẽ chọn vendor (self-hosted Plausible / Umami / Matomo ưu tiên
            data-residency-VN) + công bố chi tiết. Mặc định <strong>TẮT</strong>{' '}
            cho người dùng EU/EEA + người chưa thành niên.
          </p>

          <h3 className="text-lg font-semibold mt-6 mb-2">
            2.3 Marketing cookies (Cookies marketing — yêu cầu consent opt-in)
          </h3>
          <p>
            <strong>Phase 1:</strong> KiteHub KHÔNG sử dụng marketing cookies.
            Không gắn pixel quảng cáo (Facebook Pixel, Google Ads, TikTok
            Pixel). Nếu Phase 2 thêm marketing cookies, sẽ:
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>Update tài liệu này</li>
            <li>Re-prompt consent</li>
            <li>Material change notification 30 ngày trước</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">3. LocalStorage Usage</h2>
          <p>
            LocalStorage không phải HTTP cookie nhưng cùng đặt trên thiết bị user
            — PDPL coi tương tự cho mục đích consent.
          </p>
          <div className="mt-2 overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b">
                  <th className="py-2 pr-4">Storage key</th>
                  <th className="py-2 pr-4">Mục đích</th>
                  <th className="py-2 pr-4">Retention</th>
                  <th className="py-2">Category</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b"><td className="py-2 pr-4"><code>kite-consent</code></td><td className="py-2 pr-4">Lưu lựa chọn consent (cấu trúc JSON với version + timestamp)</td><td className="py-2 pr-4">12 tháng</td><td className="py-2">Essential</td></tr>
                <tr className="border-b"><td className="py-2 pr-4"><code>kite-theme</code></td><td className="py-2 pr-4">Lưu lựa chọn theme (light/dark/system)</td><td className="py-2 pr-4">User clear</td><td className="py-2">Essential</td></tr>
                <tr><td className="py-2 pr-4"><code>kite-sidebar-collapsed</code></td><td className="py-2 pr-4">Lưu trạng thái sidebar admin</td><td className="py-2 pr-4">User clear</td><td className="py-2">Essential</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">4. Cookies Bên Thứ ba (Third-Party Cookies)</h2>
          <p>
            <strong>Trạng thái Phase 1:</strong> KiteHub/KiteClass{' '}
            <strong>KHÔNG</strong> sử dụng third-party cookies trên các surface
            công khai.
          </p>
          <p className="mt-2">
            <strong>Lưu ý future-ready:</strong> nếu Phase 2 hoặc giai đoạn sau
            tích hợp:
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li><strong>Cổng thanh toán (VNPay/MoMo):</strong> chỉ load khi user thực sự click &quot;Thanh toán&quot; — đặt cookies riêng theo policy của họ.</li>
            <li><strong>Map embeds (Google Maps):</strong> chỉ load khi user opt-in qua placeholder click-to-load.</li>
            <li><strong>Video embeds (YouTube/Vimeo):</strong> dùng <code>youtube-nocookie.com</code> / privacy-enhanced mode.</li>
            <li><strong>OAuth providers (Google/Microsoft login):</strong> chỉ đặt cookies khi user chủ động chọn social login.</li>
          </ul>
          <p className="mt-2">
            Mọi tích hợp third-party trong tương lai sẽ update tài liệu này +
            trigger re-prompt consent (material change).
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">5. Tần suất Hỏi lại Đồng ý (Re-prompt Cadence)</h2>
          <p>
            Theo <code>BR-PDPL-CONSENT-002</code> trong{' '}
            <code>documents/01-business/kiteclass/marketing/rules.md</code>:
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li><strong>Mặc định:</strong> Re-prompt consent mỗi <strong>12 tháng</strong> (rolling window).</li>
            <li><strong>Material change trigger:</strong> Re-prompt ngay khi:
              <ul className="mt-1 list-disc space-y-1 pl-5">
                <li>Thêm cookie category mới (vd. thêm marketing cookies)</li>
                <li>Thay đổi vendor (vd. swap analytics provider)</li>
                <li>Thay đổi mục đích sử dụng cookies</li>
                <li>Thay đổi retention period material (&gt;20%)</li>
              </ul>
            </li>
            <li><strong>Version-bump trigger:</strong> Khi <code>kite-consent.version</code> field tăng (vd. v1 → v2) → consent stale → re-prompt.</li>
            <li><strong>User-initiated re-review:</strong> User có thể chủ động re-review qua Privacy Center → trigger lại banner.</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">6. Quy trình Rút lại Đồng ý (Revocation Flow)</h2>
          <p>
            PDPL Art 12 (right to erasure) + Art 11.3 (right to withdraw
            consent) — user có thể rút lại với cùng mức độ dễ dàng như khi đồng
            ý.
          </p>

          <h3 className="text-lg font-semibold mt-4 mb-2">6.1 Cách rút lại đồng ý</h3>
          <ol className="list-decimal space-y-2 pl-5">
            <li>
              <strong>Privacy Center (chính thức)</strong> —{' '}
              <code>/account/privacy-settings</code> (TODO Phase 2 endpoint):
              bật/tắt từng category cookie riêng lẻ; thay đổi có hiệu lực ngay;
              tạo audit log entry.
            </li>
            <li>
              <strong>Cookie banner (re-trigger)</strong> — Click nút bên dưới
              để xoá lựa chọn hiện tại và mở lại{' '}
              <code>ConsentBanner</code>; user có thể đổi quyết định bất kỳ lúc
              nào (PDPL Decree 13/2023 Art 12 — withdraw with same ease as
              consent).
              <div className="mt-3">
                <OpenConsentBannerButton />
              </div>
            </li>
            <li>
              <strong>Browser-level</strong> — User có thể xoá cookies +
              LocalStorage qua browser settings: lần truy cập kế sẽ trigger
              banner.
            </li>
          </ol>

          <h3 className="text-lg font-semibold mt-4 mb-2">6.2 Hậu quả khi opt-out</h3>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Analytics:</strong> Không track aggregated stats; không ảnh hưởng đến dịch vụ.</li>
            <li><strong>Marketing:</strong> Không track campaign attribution; không nhận quảng cáo retarget.</li>
            <li><strong>Essential:</strong> KHÔNG thể opt-out — dịch vụ sẽ không hoạt động đúng.</li>
          </ul>

          <h3 className="text-lg font-semibold mt-4 mb-2">6.3 Audit trail</h3>
          <p>
            Mọi consent change (grant + revoke) được log với:{' '}
            <code>userId</code>, <code>timestamp</code>, <code>categories</code>
            , <code>source</code> (banner / privacy-center / browser-clear),{' '}
            <code>version</code>. Audit log retention: 24 tháng.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">7. Tham chiếu chéo (Cross-Reference)</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li>
              <a href="/legal/privacy" className="underline hover:text-primary">
                Chính sách quyền riêng tư §15
              </a>
              {' '}— high-level cookie disclosure
            </li>
            <li>
              <a href="/legal/terms" className="underline hover:text-primary">
                Điều khoản dịch vụ
              </a>
              {' '}§8 Confidentiality + Data Protection
            </li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-3">8. Thay đổi Chính sách Cookie (Changes)</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><strong>Phiên bản hiện tại:</strong> Phase 1 SKELETON v1, last-updated 2026-05-06.</li>
            <li><strong>Material changes (yêu cầu re-consent):</strong>
              <ul className="mt-1 list-disc space-y-1 pl-5">
                <li>Thêm cookie category mới</li>
                <li>Thay đổi vendor (analytics / marketing provider)</li>
                <li>Thay đổi retention period &gt;20%</li>
                <li>Thay đổi mục đích xử lý</li>
                <li>Thêm cross-border transfer</li>
              </ul>
              <strong>Notification:</strong> email + in-app banner + cookie banner re-prompt 30 ngày trước hiệu lực.
            </li>
            <li><strong>Non-material changes (publish trực tiếp):</strong> sửa typo, format, clarification không thay đổi nghĩa, update vendor URL.</li>
            <li><strong>Lịch sử phiên bản:</strong> TODO Phase 2 — duy trì archive <code>/legal/cookies/v1</code>, <code>/legal/cookies/v2</code>.</li>
            <li><strong>Ngôn ngữ:</strong> Tiếng Việt là bản gốc (canonical). Trong trường hợp khác biệt diễn giải, bản tiếng Việt prevail.</li>
          </ul>
        </section>

        <hr className="my-8" />

        <nav aria-label="Trang pháp lý liên quan">
          <h2 className="text-lg font-semibold mb-2">Trang pháp lý liên quan</h2>
          <ul className="list-disc space-y-1 pl-5">
            <li><a href="/legal/privacy" className="underline hover:text-primary">Chính sách quyền riêng tư</a></li>
            <li><a href="/legal/terms" className="underline hover:text-primary">Điều khoản dịch vụ</a></li>
            <li><a href="/legal/dmca" className="underline hover:text-primary">DMCA Takedown Notice</a></li>
          </ul>
        </nav>
      </article>
    </div>
  );
}
