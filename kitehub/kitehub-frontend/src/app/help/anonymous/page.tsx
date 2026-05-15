import Link from 'next/link';
import type { Metadata } from 'next';
import { getAllManualPagesForPersona } from '@/lib/user-manual';
import { SITE_URL } from '@/lib/site-config';

/**
 * User manual — anonymous persona — index/landing page.
 *
 * Per `.claude/rules/user-manual-content-standard.md` §3 — persona-specific
 * landing (Anonymous persona). Entry point 1 of 3 (top-nav "Hướng dẫn").
 */

export const metadata: Metadata = {
  title: 'Hướng dẫn KiteHub — Dành cho người mới',
  description:
    'Tài liệu hướng dẫn sử dụng KiteHub dành cho người chưa đăng ký: tổng quan sản phẩm, bảng giá, cách tham gia Beta, điều khoản dịch vụ, FAQ.',
  alternates: { canonical: `${SITE_URL}/help/anonymous` },
};

export default function AnonymousManualIndexPage() {
  const pages = getAllManualPagesForPersona('anonymous');

  return (
    <main className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
      <header className="mb-10">
        <p className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-600">
          Hướng dẫn KiteHub
        </p>
        <h1 className="text-3xl font-bold text-slate-900 sm:text-4xl">
          Dành cho người mới (Anonymous Prospect)
        </h1>
        <p className="mt-3 max-w-2xl text-base text-slate-700">
          Bạn chưa đăng ký KiteHub? Đây là 5 trang tài liệu giúp bạn hiểu sản phẩm, bảng
          giá, cách tham gia Beta miễn phí 6 tháng và các điều khoản pháp lý cần biết
          trước khi đăng ký.
        </p>
      </header>

      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {pages.map((p) => (
          <li key={p.slug}>
            <Link
              href={`/help/anonymous/${p.slug}`}
              className="block rounded-lg border border-slate-200 bg-white p-5 transition hover:border-slate-400 hover:shadow-sm"
            >
              <h2 className="text-lg font-semibold text-slate-900">{p.title}</h2>
              {p.summary ? (
                <p className="mt-2 line-clamp-3 text-sm text-slate-600">{p.summary}</p>
              ) : null}
              <p className="mt-3 text-xs text-slate-500">
                Đọc khoảng {p.effortMinutes} phút · Cập nhật {p.lastUpdated}
              </p>
            </Link>
          </li>
        ))}
      </ul>

      <footer className="mt-12 rounded-lg border border-slate-200 bg-slate-50 p-5 text-sm text-slate-700">
        <p className="font-semibold text-slate-900">Bạn không thấy thông tin cần tìm?</p>
        <ul className="mt-2 space-y-1">
          <li>
            📧 Email:{' '}
            <a className="text-blue-700 hover:underline" href="mailto:support@kitehub.me">
              support@kitehub.me
            </a>
          </li>
          <li>📊 Trạng thái beta: <Link className="text-blue-700 hover:underline" href="/beta-status">/beta-status</Link></li>
        </ul>
      </footer>
    </main>
  );
}
