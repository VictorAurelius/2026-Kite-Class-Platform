import Link from 'next/link';
import { notFound } from 'next/navigation';
// SSG-incompatible isomorphic-dompurify removed — content source is filesystem-trusted MDX (Wave beta-readiness-1 fix per build error /default-stylesheet.css missing).
import { getAllBlogSlugs, getBlogPost } from '@/lib/blog';
import { JsonLd } from '@/components/seo/JsonLd';
import { blogPostingSchema, breadcrumbListSchema } from '@/components/seo/schemas';
import { SITE_URL } from '@/lib/site-config';

import type { Metadata } from 'next';

interface BlogPostPageProps {
  params: Promise<{ slug: string }>;
}

export async function generateStaticParams() {
  const slugs = getAllBlogSlugs();
  return slugs.map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: BlogPostPageProps): Promise<Metadata> {
  const { slug } = await params;
  const post = await getBlogPost(slug);

  if (!post) {
    return { title: 'Not Found - KiteHub Blog' };
  }

  return {
    title: `${post.title} - KiteHub Blog`,
    description: post.description,
    alternates: {
      canonical: `${SITE_URL}/blog/${slug}`,
    },
    openGraph: {
      title: post.title,
      description: post.description,
      url: `${SITE_URL}/blog/${slug}`,
      type: 'article',
      publishedTime: post.date,
      authors: [post.author],
      tags: post.tags,
    },
  };
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export default async function BlogPostPage({ params }: BlogPostPageProps) {
  const { slug } = await params;
  const post = await getBlogPost(slug);

  if (!post) {
    notFound();
  }

  return (
    <article className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
      <JsonLd
        json={JSON.stringify(
          blogPostingSchema({
            slug,
            title: post.title,
            description: post.description,
            author: post.author,
            datePublished: post.date,
            tags: post.tags,
          })
        )}
      />
      <JsonLd
        json={JSON.stringify(
          breadcrumbListSchema([
            { name: 'Trang chủ', path: '/' },
            { name: 'Blog', path: '/blog' },
            { name: post.title, path: `/blog/${slug}` },
          ])
        )}
      />
      <Link
        href="/blog"
        className="mb-8 inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
      >
        &larr; Quay lai blog
      </Link>

      <header className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
          {post.title}
        </h1>
        <div className="mt-4 flex items-center gap-4 text-sm text-muted-foreground">
          <span>{post.author}</span>
          <span aria-hidden="true">&middot;</span>
          <time dateTime={post.date}>{formatDate(post.date)}</time>
        </div>
        {post.tags.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-2">
            {post.tags.map((tag) => (
              <span
                key={tag}
                className="rounded-full bg-muted px-3 py-1 text-xs text-muted-foreground"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </header>

      <div
        className="prose prose-lg max-w-none dark:prose-invert prose-headings:font-semibold prose-a:text-blue-600 dark:prose-a:text-blue-400"
        dangerouslySetInnerHTML={{ __html: post.content }}
      />

      <div className="mt-12 border-t border-border pt-8">
        <Link
          href="/blog"
          className="text-primary hover:text-primary/80"
        >
          &larr; Xem tat ca bai viet
        </Link>
      </div>
    </article>
  );
}
