/**
 * Server-rendered above-fold landing content.
 *
 * Wired into `LandingShell` as the `next/dynamic` loading fallback so that the
 * initial HTML returned by Next.js contains real, semantic content (hero +
 * CTAs + value-prop bullets + nav links) instead of a "Đang tải…" spinner.
 *
 * Why this matters: the dynamic landing client uses `ssr: false` to keep
 * framer-motion (~130 KB gz) out of the first paint per GAP-127 Wave 7-Perf.
 * That trade-off ships a HTML body that bots / headless reviewers without JS
 * see as a near-empty loading state plus a `BAILOUT_TO_CLIENT_SIDE_RENDERING`
 * marker — AWS Activate's review tool flagged this as "site fails to load"
 * and denied our $1k credit application 2026-05-10 (GAP-412 → GAP-459).
 *
 * Replacing the spinner with this static shell keeps the perf budget intact
 * (no framer-motion in the critical path) while giving non-JS clients the
 * same value-prop content a hydrated user would eventually see.
 *
 * GAP-459 — 2026-05-10.
 */

import Link from 'next/link';

export default function LandingShellSSR() {
  return (
    <div className="min-h-screen bg-background">
      <header className="border-b">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <span className="text-lg font-bold">KiteHub</span>
          <ul className="flex items-center gap-4 text-sm">
            <li>
              <Link href="/pricing" className="hover:underline">
                Bảng giá
              </Link>
            </li>
            <li>
              <Link href="/blog" className="hover:underline">
                Blog
              </Link>
            </li>
            <li>
              <Link href="/login" className="hover:underline">
                Đăng nhập
              </Link>
            </li>
            <li>
              <Link
                href="/register"
                className="rounded-md bg-primary px-3 py-1.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
              >
                Đăng ký
              </Link>
            </li>
          </ul>
        </nav>
      </header>

      <main>
        <section className="mx-auto max-w-4xl px-6 py-20 text-center">
          <p className="mb-4 text-sm font-medium text-primary">
            Nền tảng quản lý giáo dục #1 Việt Nam
          </p>
          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl">
            Quản lý trung tâm giáo dục thông minh hơn
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground">
            Dành thời gian cho việc giảng dạy, để KiteHub lo phần còn lại.
            Quản lý học viên, lịch học, thanh toán — tất cả trong một nền tảng.
          </p>

          <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <Link
              href="/register"
              className="rounded-xl bg-primary px-6 py-3.5 text-sm font-semibold text-primary-foreground hover:opacity-90"
            >
              Dùng thử miễn phí 14 ngày
            </Link>
            <Link
              href="/pricing"
              className="rounded-xl border-2 px-6 py-3.5 text-sm font-semibold hover:border-primary hover:text-primary"
            >
              Xem bảng giá
            </Link>
          </div>

          <p className="mt-4 text-sm text-muted-foreground">
            ✓ Không cần thẻ tín dụng &nbsp; ✓ Hủy bất kỳ lúc nào &nbsp; ✓ Hỗ trợ
            tiếng Việt
          </p>

          {/* GAP-609 Wave 91 — alternate entry cho user nhận claim code qua kênh khác */}
          <p className="mt-3 text-sm">
            <Link
              href="/beta-signup/code"
              className="text-primary underline-offset-4 hover:underline"
            >
              Tôi đã có mã invite →
            </Link>
          </p>
        </section>

        <section className="mx-auto max-w-5xl px-6 pb-20">
          <ul className="grid gap-6 sm:grid-cols-3">
            <li className="rounded-lg border bg-card p-6">
              <h2 className="text-lg font-semibold">Tuyển sinh tự động</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                Landing page riêng cho từng trung tâm, form đăng ký liên kết CRM,
                AI tự động tạo thương hiệu phù hợp định vị.
              </p>
            </li>
            <li className="rounded-lg border bg-card p-6">
              <h2 className="text-lg font-semibold">Quản lý lớp + học phí</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                Lịch học, điểm danh, tiến độ học viên, thanh toán VietQR &mdash;
                vận hành trung tâm bằng một nền tảng duy nhất.
              </p>
            </li>
            <li className="rounded-lg border bg-card p-6">
              <h2 className="text-lg font-semibold">Tuân thủ PDPL Việt Nam</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                Dữ liệu lưu trữ tại Việt Nam, công cụ DSAR đầy đủ, sẵn sàng hỗ
                trợ trung tâm tuân thủ Luật Bảo vệ Dữ liệu Cá nhân 2023.
              </p>
            </li>
          </ul>
        </section>
      </main>

      {/*
       * Wave beta-prep-1 Bucket F3 — trust footer enhancement.
       * VN-localization audit checklist (§2 row 5 support footer + §4 Zalo culture).
       *
       * Adds:
       *  - 1 placeholder testimonial (P2 owner Hằng quote VN)
       *  - Security trust badges (HTTPS + PDPL pending counsel)
       *  - Counsel disclaimer `[v1 chờ tư vấn pháp lý]`
       *  - Multi-channel support (email + Zalo OA placeholder per tenant-support-channels-runbook.md §4)
       */}
      <section className="border-t bg-muted/30 py-12">
        <div className="mx-auto max-w-6xl px-6">
          <blockquote className="mx-auto max-w-3xl rounded-lg bg-background p-6 shadow-sm">
            <p className="text-base italic text-foreground">
              &ldquo;Trước đây mình quản lý 5 lớp bằng Google Sheets, đếm điểm danh thủ
              công mất cả buổi tối. Từ khi dùng KiteHub, mình tiết kiệm được khoảng
              4 giờ mỗi tuần và phụ huynh nhận thông báo tự động qua Zalo.&rdquo;
            </p>
            <footer className="mt-4 text-sm text-muted-foreground">
              — Chị Trần Thị Hồng, chủ Trung tâm Anh ngữ Sky Education (Q.3, TP.HCM)
              <br />
              <span className="text-xs italic opacity-70">
                [Testimonial mẫu Phase 1 BETA — sẽ thay thế bằng quote thật khi cohort
                feedback đầy đủ]
              </span>
            </footer>
          </blockquote>

          <div className="mt-8 flex flex-wrap items-center justify-center gap-6 text-sm text-muted-foreground">
            <span className="flex items-center gap-2">
              <span aria-hidden="true">🔒</span>
              <span>HTTPS / TLS 1.3 toàn site</span>
            </span>
            <span className="flex items-center gap-2">
              <span aria-hidden="true">🇻🇳</span>
              <span>Dữ liệu lưu trữ tại Việt Nam (AWS Singapore)</span>
            </span>
            <span className="flex items-center gap-2">
              <span aria-hidden="true">⚖️</span>
              <span>Tuân thủ PDPL 2023 [v1 chờ tư vấn pháp lý hoàn thiện]</span>
            </span>
          </div>
        </div>
      </section>

      <footer className="border-t bg-background">
        <div className="mx-auto max-w-6xl px-6 py-8">
          <div className="grid gap-8 sm:grid-cols-3">
            <div>
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                Hỗ trợ
              </h3>
              <ul className="space-y-2 text-sm">
                <li>
                  <a
                    href="mailto:support@kitehub.me"
                    className="hover:text-primary hover:underline"
                  >
                    Email: support@kitehub.me
                  </a>
                </li>
                <li>
                  <a
                    href="https://zalo.me/kitehub"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="hover:text-primary hover:underline"
                  >
                    Zalo OA: zalo.me/kitehub
                    <span className="ml-1 text-xs italic opacity-70">(chờ kích hoạt)</span>
                  </a>
                </li>
                <li>
                  <Link
                    href="/beta-status"
                    className="hover:text-primary hover:underline"
                  >
                    Trạng thái hệ thống
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                Pháp lý
              </h3>
              <ul className="space-y-2 text-sm">
                <li>
                  <Link href="/legal/terms" className="hover:text-primary hover:underline">
                    Điều khoản sử dụng
                  </Link>
                </li>
                <li>
                  <Link
                    href="/legal/privacy"
                    className="hover:text-primary hover:underline"
                  >
                    Chính sách quyền riêng tư
                  </Link>
                </li>
                <li>
                  <Link
                    href="/legal/data-rights"
                    className="hover:text-primary hover:underline"
                  >
                    Quyền dữ liệu cá nhân (PDPL)
                  </Link>
                </li>
                <li>
                  <Link
                    href="/legal/cookies"
                    className="hover:text-primary hover:underline"
                  >
                    Cookies
                  </Link>
                </li>
              </ul>
            </div>

            <div>
              <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                Tài nguyên
              </h3>
              <ul className="space-y-2 text-sm">
                <li>
                  <Link href="/pricing" className="hover:text-primary hover:underline">
                    Bảng giá
                  </Link>
                </li>
                <li>
                  <Link href="/blog" className="hover:text-primary hover:underline">
                    Blog
                  </Link>
                </li>
                <li>
                  <Link
                    href="/request-beta-access"
                    className="hover:text-primary hover:underline"
                  >
                    Yêu cầu Beta access
                  </Link>
                </li>
              </ul>
            </div>
          </div>

          <div className="mt-8 border-t pt-6 text-center text-xs text-muted-foreground">
            <p>
              &copy; {new Date().getFullYear()} KiteHub. All rights reserved.
            </p>
            <p className="mt-1 italic opacity-70">
              [Phiên bản v1 đang chờ tư vấn pháp lý hoàn thiện. Phase 1 BETA — invite
              only.]
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
