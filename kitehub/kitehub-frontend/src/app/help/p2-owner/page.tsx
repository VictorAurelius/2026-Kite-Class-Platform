import Link from 'next/link';
import type { Metadata } from 'next';
import { getAllManualPagesForPersona } from '@/lib/user-manual';
import { SITE_URL } from '@/lib/site-config';

/**
 * User manual — P2 Center Owner persona — index/landing page.
 *
 * Per `.claude/rules/user-manual-content-standard.md` §3 — persona-specific
 * landing. Entry points: Header `?` button + Sidebar footer.
 *
 * Wave 80 Bucket D — F2 user manual full retrofit (GAP-537).
 */

const PERSONA = 'p2-owner';
const PERSONA_LABEL = 'Chủ trung tâm';

export const metadata: Metadata = {
  title: `Hướng dẫn KiteHub — ${PERSONA_LABEL}`,
  description:
    'Tài liệu hướng dẫn KiteHub dành cho Chủ trung tâm: bảng giá + thanh toán, mời nhân viên, tuỳ chỉnh logo, cấu hình chung.',
  alternates: { canonical: `${SITE_URL}/help/${PERSONA}` },
};

export default function P2OwnerManualIndexPage() {
  const pages = getAllManualPagesForPersona(PERSONA);

  return (
    <main className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
      <header className="mb-10">
        <p className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-600">
          Hướng dẫn KiteHub
        </p>
        <h1 className="text-3xl font-bold text-slate-900 sm:text-4xl">
          Dành cho {PERSONA_LABEL}
        </h1>
        <p className="mt-3 max-w-2xl text-base text-slate-700">
          Bạn là Chủ trung tâm? 5 trang dưới đây giúp bạn nắm vững các tác vụ chính:
          bảng giá + thanh toán, mời nhân viên, tuỳ chỉnh thương hiệu, cấu hình chung.
        </p>
      </header>

      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {pages.map((p) => (
          <li key={p.slug}>
            <Link
              href={`/help/${PERSONA}/${p.slug}`}
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
          <li>📞 Hotline: 1900-xxxx (giờ hành chính)</li>
          <li>📊 Trạng thái beta: <Link className="text-blue-700 hover:underline" href="/beta-status">/beta-status</Link></li>
        </ul>
      </footer>
    </main>
  );
}
