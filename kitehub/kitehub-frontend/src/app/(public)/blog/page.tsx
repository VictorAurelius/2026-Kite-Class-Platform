import Link from 'next/link';
import { getAllBlogPosts } from '@/lib/blog';

import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Blog - KiteHub',
  description:
    'Chia sẻ kiến thức quản lý trung tâm giáo dục, công nghệ EdTech, và mẹo vận hành hiệu quả.',
  openGraph: {
    title: 'Blog - KiteHub',
    description:
      'Chia sẻ kiến thức quản lý trung tâm giáo dục, công nghệ EdTech, và mẹo vận hành hiệu quả.',
    url: 'https://kitehub.vn/blog',
  },
};

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export default function BlogListPage() {
  const posts = getAllBlogPosts();

  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6 lg:px-8">
      <div className="mb-12 text-center">
        <h1 className="text-4xl font-bold tracking-tight text-foreground">Blog</h1>
        <p className="mt-4 text-lg text-muted-foreground">
          Chia se kien thuc quan ly trung tam giao duc va cong nghe EdTech
        </p>
      </div>

      {posts.length === 0 ? (
        <p className="text-center text-gray-500 dark:text-gray-400">Chua co bai viet nao.</p>
      ) : (
        <div className="space-y-8">
          {posts.map((post) => (
            <article
              key={post.slug}
              className="rounded-lg border border-border p-6 transition-shadow hover:shadow-md"
            >
              <Link href={`/blog/${post.slug}`} className="group block">
                <h2 className="text-2xl font-semibold text-foreground group-hover:text-primary">
                  {post.title}
                </h2>
                <p className="mt-2 text-muted-foreground">{post.description}</p>
                <div className="mt-4 flex items-center gap-4 text-sm text-muted-foreground">
                  <span>{post.author}</span>
                  <span aria-hidden="true">&middot;</span>
                  <time dateTime={post.date}>{formatDate(post.date)}</time>
                </div>
                {post.tags.length > 0 && (
                  <div className="mt-3 flex flex-wrap gap-2">
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
              </Link>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
