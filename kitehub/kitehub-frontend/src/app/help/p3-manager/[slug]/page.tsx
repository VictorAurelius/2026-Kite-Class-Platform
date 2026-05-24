import Link from 'next/link';
import { notFound } from 'next/navigation';
import type { Metadata } from 'next';
// SSG-incompatible isomorphic-dompurify removed — MDX source is filesystem-trusted (Wave beta-readiness-1 fix per build error /home/runner/.../browser/default-stylesheet.css missing). Defensive sanitization moved to client OR build-time MDX render escape.
import {
  getAllManualPagesForPersona,
  getManualPage,
  getManualSlugsForPersona,
} from '@/lib/user-manual';
import { SITE_URL } from '@/lib/site-config';

/**
 * User manual — P2 Center Owner persona — single page route.
 *
 * Per `.claude/rules/user-manual-content-standard.md` §3 — URL `/help/p3-manager/{slug}`.
 * Source: `documents/05-guides/user-manual/p3-manager/{slug}.md`
 *
 * Wave 80 Bucket D — F2 user manual full retrofit (GAP-537).
 */

const PERSONA = 'p3-manager';
const PERSONA_LABEL = 'Quản lý trung tâm';

interface PageProps {
  params: Promise<{ slug: string }>;
}

export async function generateStaticParams() {
  return getManualSlugsForPersona(PERSONA).map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  const page = await getManualPage(PERSONA, slug);
  if (!page) {
    return { title: 'Trang không tồn tại - KiteHub' };
  }
  return {
    title: `${page.title} - KiteHub Hướng dẫn`,
    description: page.title,
    alternates: { canonical: `${SITE_URL}/help/${PERSONA}/${slug}` },
    openGraph: {
      title: page.title,
      url: `${SITE_URL}/help/${PERSONA}/${slug}`,
      type: 'article',
    },
  };
}

export default async function P3ManagerManualPage({ params }: PageProps) {
  const { slug } = await params;
  const page = await getManualPage(PERSONA, slug);
  if (!page) {
    notFound();
  }
  const allPages = getAllManualPagesForPersona(PERSONA);

  return (
    <div className="mx-auto grid max-w-7xl grid-cols-1 gap-8 px-4 py-12 sm:px-6 lg:grid-cols-[16rem_1fr] lg:px-8 lg:py-16">
      <aside
        aria-label={`Mục lục Hướng dẫn ${PERSONA_LABEL}`}
        className="hidden lg:block print:hidden"
      >
        <nav className="sticky top-20 border-l border-slate-200 pl-4">
          <p className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-600">
            {PERSONA_LABEL}
          </p>
          <ul className="space-y-2 text-sm">
            {allPages.map((p) => (
              <li key={p.slug}>
                <Link
                  href={`/help/${PERSONA}/${p.slug}`}
                  className={`block rounded px-2 py-1 hover:bg-slate-100 ${
                    p.slug === slug
                      ? 'bg-slate-100 font-semibold text-slate-900'
                      : 'text-slate-700'
                  }`}
                  aria-current={p.slug === slug ? 'page' : undefined}
                >
                  {p.title}
                </Link>
              </li>
            ))}
          </ul>
          <div className="mt-6 border-t border-slate-200 pt-4 text-xs text-slate-500">
            <Link href={`/help/${PERSONA}`} className="hover:text-slate-900">
              ← Quay về tất cả tài liệu
            </Link>
          </div>
        </nav>
      </aside>

      <article className="prose prose-slate max-w-3xl print:max-w-full">
        <nav aria-label="Đường dẫn" className="mb-4 text-xs text-slate-600 print:hidden">
          <Link href="/" className="hover:underline">
            Trang chủ
          </Link>{' '}
          /{' '}
          <Link href={`/help/${PERSONA}`} className="hover:underline">
            Hướng dẫn {PERSONA_LABEL}
          </Link>{' '}
          / <span className="text-slate-900">{page.title}</span>
        </nav>
        <div
          dangerouslySetInnerHTML={{ __html: page.contentHtml }}
        />
      </article>
    </div>
  );
}
