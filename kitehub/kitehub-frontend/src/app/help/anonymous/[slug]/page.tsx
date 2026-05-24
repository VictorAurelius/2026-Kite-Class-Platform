import Link from 'next/link';
import { notFound } from 'next/navigation';
import type { Metadata } from 'next';
import DOMPurify from 'isomorphic-dompurify';
import {
  getAllManualPagesForPersona,
  getManualPage,
  getManualSlugsForPersona,
} from '@/lib/user-manual';
import { SITE_URL } from '@/lib/site-config';

/**
 * User manual — anonymous persona — single page route.
 *
 * Per `.claude/rules/user-manual-content-standard.md` §3 (discoverability matrix):
 *   - Anonymous persona entry points: top-nav "Hướng dẫn" + footer + Google indexable
 *   - URL pattern: `/help/anonymous/{slug}` (slug = topic from frontmatter)
 *
 * Source: `documents/05-guides/user-manual/anonymous/{slug}.md`
 *
 * 15-item checklist verified per page at content-source level (review-time).
 */

interface PageProps {
  params: Promise<{ slug: string }>;
}

export async function generateStaticParams() {
  return getManualSlugsForPersona('anonymous').map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  const page = await getManualPage('anonymous', slug);
  if (!page) {
    return { title: 'Trang không tồn tại - KiteHub' };
  }
  return {
    title: `${page.title} - KiteHub Hướng dẫn`,
    description: page.title,
    alternates: { canonical: `${SITE_URL}/help/anonymous/${slug}` },
    openGraph: {
      title: page.title,
      url: `${SITE_URL}/help/anonymous/${slug}`,
      type: 'article',
    },
  };
}

export default async function AnonymousManualPage({ params }: PageProps) {
  const { slug } = await params;
  const page = await getManualPage('anonymous', slug);
  if (!page) {
    notFound();
  }
  const allPages = getAllManualPagesForPersona('anonymous');

  return (
    <div className="mx-auto grid max-w-7xl grid-cols-1 gap-8 px-4 py-12 sm:px-6 lg:grid-cols-[16rem_1fr] lg:px-8 lg:py-16">
      {/* TOC sidebar — collapses on mobile per §2 item 14 (≥360px viewport) */}
      <aside
        aria-label="Mục lục Hướng dẫn Anonymous"
        className="hidden lg:block print:hidden"
      >
        <nav className="sticky top-20 border-l border-slate-200 pl-4">
          <p className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-600">
            Anonymous Prospect
          </p>
          <ul className="space-y-2 text-sm">
            {allPages.map((p) => (
              <li key={p.slug}>
                <Link
                  href={`/help/anonymous/${p.slug}`}
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
            <Link href="/help/anonymous" className="hover:text-slate-900">
              ← Quay về tất cả tài liệu
            </Link>
          </div>
        </nav>
      </aside>

      {/* Main content — markdown rendered to HTML server-side */}
      <article className="prose prose-slate max-w-3xl print:max-w-full">
        <nav aria-label="Đường dẫn" className="mb-4 text-xs text-slate-600 print:hidden">
          <Link href="/" className="hover:underline">
            Trang chủ
          </Link>{' '}
          /{' '}
          <Link href="/help/anonymous" className="hover:underline">
            Hướng dẫn
          </Link>{' '}
          / <span className="text-slate-900">{page.title}</span>
        </nav>
        <div
          // WCAG AA: heading hierarchy preserved by source markdown (§2 item 11)
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(page.contentHtml) }}
        />
      </article>
    </div>
  );
}
