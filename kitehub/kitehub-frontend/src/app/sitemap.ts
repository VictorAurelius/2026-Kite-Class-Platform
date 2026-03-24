import { MetadataRoute } from 'next';
import { getAllBlogPosts } from '@/lib/blog';

export default function sitemap(): MetadataRoute.Sitemap {
  const blogPosts = getAllBlogPosts();

  const blogUrls: MetadataRoute.Sitemap = blogPosts.map((post) => ({
    url: `https://kitehub.vn/blog/${post.slug}`,
    lastModified: new Date(post.date),
    changeFrequency: 'monthly' as const,
    priority: 0.6,
  }));

  return [
    { url: 'https://kitehub.vn', lastModified: new Date(), changeFrequency: 'weekly', priority: 1 },
    { url: 'https://kitehub.vn/pricing', lastModified: new Date(), changeFrequency: 'monthly', priority: 0.8 },
    { url: 'https://kitehub.vn/blog', lastModified: new Date(), changeFrequency: 'weekly', priority: 0.7 },
    ...blogUrls,
    { url: 'https://kitehub.vn/login', lastModified: new Date(), changeFrequency: 'yearly', priority: 0.3 },
    { url: 'https://kitehub.vn/register', lastModified: new Date(), changeFrequency: 'yearly', priority: 0.5 },
  ];
}
