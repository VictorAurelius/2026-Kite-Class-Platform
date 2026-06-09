/**
 * Server-rendered above-fold landing content.
 *
 * Wired into `LandingShell` as the `next/dynamic` loading fallback so that the
 * initial HTML returned by Next.js contains real, semantic content (hero +
 * CTAs + value-prop bullets) instead of a "Đang tải…" spinner.
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
 * Chrome (header nav + footer) is provided by `(public)/layout.tsx` →
 * `PublicLayout` (which renders to the initial HTML since it is a client
 * component). This shell renders ONLY the unique landing content — rendering
 * its own <header>/<footer>/<main> here duplicated the site chrome and nested
 * a second <main>, matching neither `LandingClient` nor the other (public)
 * pages. Keep parity with `LandingClient`: content sections only.
 *
 * GAP-459 — 2026-05-10.
 */

import Link from 'next/link';

export default function LandingShellSSR() {
  return (
    <>
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

      {/*
       * Wave beta-prep-1 Bucket F3 — trust section (unique landing content, NOT
       * part of PublicLayout chrome → kept here).
       * VN-localization audit checklist (§2 row 5 support footer + §4 Zalo culture).
       *
       *  - 1 placeholder testimonial (P2 owner Hằng quote VN)
       *  - Security trust badges (HTTPS + PDPL pending counsel)
       *  - Counsel disclaimer `[v1 chờ tư vấn pháp lý]`
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
    </>
  );
}
