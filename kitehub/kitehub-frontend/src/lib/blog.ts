import fs from 'fs';
import path from 'path';
import matter from 'gray-matter';
import { remark } from 'remark';
import html from 'remark-html';

const BLOG_DIR = path.join(process.cwd(), 'content', 'blog');

export interface BlogPost {
  slug: string;
  title: string;
  description: string;
  date: string;
  author: string;
  tags: string[];
  content: string;
}

export interface BlogPostMeta {
  slug: string;
  title: string;
  description: string;
  date: string;
  author: string;
  tags: string[];
}

/**
 * Get all blog post slugs for static generation.
 */
export function getAllBlogSlugs(): string[] {
  if (!fs.existsSync(BLOG_DIR)) {
    return [];
  }

  return fs
    .readdirSync(BLOG_DIR)
    .filter((file) => file.endsWith('.md'))
    .map((file) => file.replace(/\.md$/, ''));
}

/**
 * Get all blog posts metadata, sorted by date (newest first).
 */
export function getAllBlogPosts(): BlogPostMeta[] {
  const slugs = getAllBlogSlugs();

  const posts = slugs.map((slug) => {
    const filePath = path.join(BLOG_DIR, `${slug}.md`);
    const fileContent = fs.readFileSync(filePath, 'utf-8');
    const { data } = matter(fileContent);

    return {
      slug,
      title: (data.title as string) ?? '',
      description: (data.description as string) ?? '',
      date: (data.date as string) ?? '',
      author: (data.author as string) ?? 'KiteHub Team',
      tags: (data.tags as string[]) ?? [],
    };
  });

  return posts.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
}

/**
 * Get a single blog post by slug, including rendered HTML content.
 */
export async function getBlogPost(slug: string): Promise<BlogPost | null> {
  const filePath = path.join(BLOG_DIR, `${slug}.md`);

  if (!fs.existsSync(filePath)) {
    return null;
  }

  const fileContent = fs.readFileSync(filePath, 'utf-8');
  const { data, content } = matter(fileContent);

  const processedContent = await remark().use(html).process(content);
  const htmlContent = processedContent.toString();

  return {
    slug,
    title: (data.title as string) ?? '',
    description: (data.description as string) ?? '',
    date: (data.date as string) ?? '',
    author: (data.author as string) ?? 'KiteHub Team',
    tags: (data.tags as string[]) ?? [],
    content: htmlContent,
  };
}
